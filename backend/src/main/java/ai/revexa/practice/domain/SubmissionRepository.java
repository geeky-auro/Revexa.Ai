package ai.revexa.practice.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

    Optional<Submission> findByIdAndUserId(UUID id, UUID userId);

    Page<Submission> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<Submission> findByUserIdAndProblemIdOrderByCreatedAtDesc(UUID userId, UUID problemId);

    long countByUserId(UUID userId);
}
