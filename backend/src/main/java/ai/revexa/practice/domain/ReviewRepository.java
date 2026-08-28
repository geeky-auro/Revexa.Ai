package ai.revexa.practice.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    Optional<Review> findByIdAndUserId(UUID id, UUID userId);

    Optional<Review> findFirstBySubmissionIdOrderByCreatedAtDesc(UUID submissionId);

    Page<Review> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<Review> findByUserIdOrderByCreatedAtAsc(UUID userId);

    List<Review> findByUserIdAndProblemIdOrderByCreatedAtDesc(UUID userId, UUID problemId);

    long countByUserId(UUID userId);

    long countByUserIdAndTimeOptimalTrue(UUID userId);

    long countByUserIdAndCreatedAtAfter(UUID userId, Instant after);
}
