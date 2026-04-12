package com.photovault.repository;

import com.photovault.entity.ProcessingJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProcessingJobRepository extends JpaRepository<ProcessingJob, UUID> {

    List<ProcessingJob> findBySessionId(UUID sessionId);

    List<ProcessingJob> findByAlbumIdOrderByQueuedAtDesc(UUID albumId);

    Optional<ProcessingJob> findByMediaId(UUID mediaId);

    @Query("""
        SELECT j FROM ProcessingJob j
        WHERE j.session.id = :sessionId
        ORDER BY j.queuedAt ASC
        """)
    List<ProcessingJob> findBySessionIdOrdered(@Param("sessionId") UUID sessionId);

    @Query("""
        SELECT COUNT(j) FROM ProcessingJob j
        WHERE j.session.id = :sessionId AND j.status = :status
        """)
    long countBySessionIdAndStatus(
        @Param("sessionId") UUID sessionId,
        @Param("status") ProcessingJob.JobStatus status
    );

    @Query("""
        SELECT j FROM ProcessingJob j
        WHERE j.status IN ('FAILED', 'RETRYING')
          AND j.attemptCount < j.maxAttempts
        ORDER BY j.queuedAt ASC
        """)
    List<ProcessingJob> findRetryableJobs();
}
