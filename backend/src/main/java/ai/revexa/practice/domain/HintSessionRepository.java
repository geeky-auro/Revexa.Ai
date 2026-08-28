package ai.revexa.practice.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HintSessionRepository extends JpaRepository<HintSession, UUID> {

    Optional<HintSession> findByIdAndUserId(UUID id, UUID userId);

    Optional<HintSession> findByUserIdAndProblemId(UUID userId, UUID problemId);

    List<HintSession> findByUserId(UUID userId);
}
