package com.photovault.controller;

import com.photovault.dto.BulkJobStatusDTO;
import com.photovault.dto.ProcessingJobDTO;
import com.photovault.repository.ProcessingJobRepository;
import com.photovault.service.ProcessingJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
@Slf4j
public class JobController {

    private final ProcessingJobService processingJobService;
    private final ProcessingJobRepository processingJobRepository;

    /**
     * GET /api/v1/jobs/{jobId}
     * Returns status of a single processing job.
     */
    @GetMapping("/{jobId}")
    @PreAuthorize("hasRole('PHOTOGRAPHER')")
    public ResponseEntity<ProcessingJobDTO> getJob(@PathVariable UUID jobId) {
        return ResponseEntity.ok(processingJobService.getJob(jobId));
    }

    /**
     * GET /api/v1/jobs/sessions/{sessionId}
     * Returns aggregate status of all jobs in a bulk upload session.
     */
    @GetMapping("/sessions/{sessionId}")
    @PreAuthorize("hasRole('PHOTOGRAPHER')")
    public ResponseEntity<BulkJobStatusDTO> getBulkStatus(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(processingJobService.getBulkStatus(sessionId));
    }

    /**
     * GET /api/v1/jobs/albums/{albumId}
     * Returns all jobs for an album (most recent first, capped at 200).
     * Used by the admin UI to show the processing queue after individual uploads.
     */
    @GetMapping("/albums/{albumId}")
    @PreAuthorize("hasRole('PHOTOGRAPHER')")
    public ResponseEntity<List<ProcessingJobDTO>> getAlbumJobs(@PathVariable UUID albumId) {
        List<ProcessingJobDTO> jobs = processingJobRepository
            .findByAlbumIdOrderByQueuedAtDesc(albumId)
            .stream()
            .limit(200)
            .map(processingJobService::toDTO)
            .toList();
        return ResponseEntity.ok(jobs);
    }

    /**
     * POST /api/v1/jobs/{jobId}/retry
     * Manually re-queues a FAILED job.
     */
    @PostMapping("/{jobId}/retry")
    @PreAuthorize("hasRole('PHOTOGRAPHER')")
    public ResponseEntity<ProcessingJobDTO> retryJob(@PathVariable UUID jobId) {
        return ResponseEntity.ok(processingJobService.toDTO(processingJobService.retryJob(jobId)));
    }

    /**
     * GET /api/v1/jobs/sessions/{sessionId}/stream
     * Server-Sent Events stream that pushes BulkJobStatusDTO every 2 seconds
     * until all jobs are terminal, then closes.
     *
     * The admin UI connects here after initiating a bulk upload.
     */
    @GetMapping(value = "/sessions/{sessionId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasRole('PHOTOGRAPHER')")
    public SseEmitter streamBulkProgress(@PathVariable UUID sessionId) {

        // 5-minute timeout — bulk uploads rarely take longer; client can reconnect if needed
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                while (true) {
                    BulkJobStatusDTO status = processingJobService.getBulkStatus(sessionId);

                    emitter.send(SseEmitter.event()
                        .name("progress")
                        .data(status, MediaType.APPLICATION_JSON));

                    if (status.isDone()) {
                        emitter.send(SseEmitter.event().name("done").data("complete"));
                        emitter.complete();
                        break;
                    }

                    Thread.sleep(2000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                emitter.completeWithError(e);
            } catch (IOException e) {
                // Client disconnected
                log.debug("SSE client disconnected for session {}", sessionId);
                emitter.completeWithError(e);
            } catch (Exception e) {
                log.error("SSE error for session {}", sessionId, e);
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }
}
