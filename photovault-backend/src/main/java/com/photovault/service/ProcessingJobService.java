package com.photovault.service;

import com.photovault.dto.BulkJobStatusDTO;
import com.photovault.dto.ProcessingJobDTO;
import com.photovault.entity.Media;
import com.photovault.entity.ProcessingJob;
import com.photovault.entity.UploadSession;
import com.photovault.exception.ResourceNotFoundException;
import com.photovault.messaging.MediaEventProducer;
import com.photovault.repository.ProcessingJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessingJobService {

    private final ProcessingJobRepository jobRepository;
    private final MediaEventProducer mediaEventProducer;

    // -------------------------------------------------------------------------
    // Job creation
    // -------------------------------------------------------------------------

    @Transactional
    public ProcessingJob createJob(Media media, UploadSession session) {
        ProcessingJob.JobType type = resolveJobType(media.getMimeType());
        if (type == null) {
            log.debug("No job type for mimeType {}, skipping job creation", media.getMimeType());
            return null;
        }

        ProcessingJob job = ProcessingJob.builder()
            .media(media)
            .session(session)
            .photographerId(media.getPhotographer().getId())
            .albumId(media.getAlbum().getId())
            .jobType(type)
            .status(ProcessingJob.JobStatus.QUEUED)
            .build();

        job = jobRepository.save(job);
        log.info("Created processing job {} ({}) for media {}", job.getId(), type, media.getId());
        return job;
    }

    // -------------------------------------------------------------------------
    // Status transitions (called from MediaEventConsumer)
    // -------------------------------------------------------------------------

    @Transactional
    public void markProcessing(UUID jobId, String topic, int partition, long offset) {
        ProcessingJob job = findOrThrow(jobId);
        job.setStatus(ProcessingJob.JobStatus.PROCESSING);
        job.setStartedAt(Instant.now());
        job.setAttemptCount(job.getAttemptCount() + 1);
        job.setKafkaTopic(topic);
        job.setKafkaPartition(partition);
        job.setKafkaOffset(offset);
        jobRepository.save(job);
    }

    @Transactional
    public void markCompleted(UUID jobId) {
        ProcessingJob job = findOrThrow(jobId);
        job.setStatus(ProcessingJob.JobStatus.COMPLETED);
        job.setCompletedAt(Instant.now());
        job.setErrorMessage(null);
        jobRepository.save(job);
        log.info("Job {} completed", jobId);
    }

    @Transactional
    public void markFailed(UUID jobId, String errorMessage) {
        ProcessingJob job = findOrThrow(jobId);

        if (job.canRetry()) {
            job.setStatus(ProcessingJob.JobStatus.RETRYING);
            log.warn("Job {} failed (attempt {}/{}), will retry: {}",
                jobId, job.getAttemptCount(), job.getMaxAttempts(), errorMessage);
        } else {
            job.setStatus(ProcessingJob.JobStatus.FAILED);
            log.error("Job {} permanently failed after {} attempts: {}",
                jobId, job.getAttemptCount(), errorMessage);
        }

        job.setErrorMessage(errorMessage);
        jobRepository.save(job);
    }

    // -------------------------------------------------------------------------
    // Retry
    // -------------------------------------------------------------------------

    @Transactional
    public ProcessingJob retryJob(UUID jobId) {
        ProcessingJob job = findOrThrow(jobId);

        if (job.getStatus() != ProcessingJob.JobStatus.FAILED
                && job.getStatus() != ProcessingJob.JobStatus.RETRYING) {
            throw new IllegalStateException("Job " + jobId + " is not in a retryable state");
        }
        if (job.getAttemptCount() >= job.getMaxAttempts()) {
            throw new IllegalStateException("Job " + jobId + " has exceeded max attempts");
        }

        job.setStatus(ProcessingJob.JobStatus.QUEUED);
        job.setErrorMessage(null);
        job = jobRepository.save(job);

        // Re-enqueue to Kafka
        Media media = job.getMedia();
        UUID sessionId = job.getSession() != null ? job.getSession().getId() : null;
        mediaEventProducer.sendProcessingEvent(media, job.getId(), sessionId);

        log.info("Retrying job {} for media {}", jobId, media.getId());
        return job;
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public ProcessingJobDTO getJob(UUID jobId) {
        return toDTO(findOrThrow(jobId));
    }

    @Transactional(readOnly = true)
    public BulkJobStatusDTO getBulkStatus(UUID sessionId) {
        List<ProcessingJob> jobs = jobRepository.findBySessionIdOrdered(sessionId);

        int queued      = 0, processing = 0, completed = 0, failed = 0, retrying = 0;
        Instant earliest = null, latest = null;

        for (ProcessingJob j : jobs) {
            switch (j.getStatus()) {
                case QUEUED     -> queued++;
                case PROCESSING -> processing++;
                case COMPLETED  -> completed++;
                case FAILED     -> failed++;
                case RETRYING   -> retrying++;
            }
            if (j.getStartedAt() != null && (earliest == null || j.getStartedAt().isBefore(earliest))) {
                earliest = j.getStartedAt();
            }
            if (j.getCompletedAt() != null && (latest == null || j.getCompletedAt().isAfter(latest))) {
                latest = j.getCompletedAt();
            }
        }

        int total    = jobs.size();
        int terminal = completed + failed;
        boolean done = total > 0 && (terminal == total || (failed + retrying == total - completed && retrying == 0));

        double progress = total == 0 ? 0.0 : (double) completed / total * 100.0;

        return BulkJobStatusDTO.builder()
            .sessionId(sessionId)
            .albumId(jobs.isEmpty() ? null : jobs.get(0).getAlbumId())
            .total(total)
            .queued(queued)
            .processing(processing)
            .completed(completed)
            .failed(failed)
            .retrying(retrying)
            .progressPercent(Math.round(progress * 10.0) / 10.0)
            .done(done)
            .startedAt(earliest)
            .completedAt(done ? latest : null)
            .jobs(jobs.stream().map(this::toDTO).toList())
            .build();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private ProcessingJob findOrThrow(UUID jobId) {
        return jobRepository.findById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("ProcessingJob", "id", jobId));
    }

    private static ProcessingJob.JobType resolveJobType(String mimeType) {
        if (mimeType == null) return null;
        if (mimeType.startsWith("image/")) return ProcessingJob.JobType.IMAGE;
        if (mimeType.startsWith("video/")) return ProcessingJob.JobType.VIDEO;
        return null;
    }

    public ProcessingJobDTO toDTO(ProcessingJob j) {
        Long durationMs = null;
        if (j.getStartedAt() != null && j.getCompletedAt() != null) {
            durationMs = j.getCompletedAt().toEpochMilli() - j.getStartedAt().toEpochMilli();
        }

        return ProcessingJobDTO.builder()
            .id(j.getId())
            .mediaId(j.getMedia().getId())
            .sessionId(j.getSession() != null ? j.getSession().getId() : null)
            .albumId(j.getAlbumId())
            .filename(j.getMedia().getOriginalFilename())
            .mimeType(j.getMedia().getMimeType())
            .jobType(j.getJobType().name())
            .status(j.getStatus().name())
            .attemptCount(j.getAttemptCount())
            .maxAttempts(j.getMaxAttempts())
            .queuedAt(j.getQueuedAt())
            .startedAt(j.getStartedAt())
            .completedAt(j.getCompletedAt())
            .errorMessage(j.getErrorMessage())
            .durationMs(durationMs)
            .build();
    }
}
