package ai.revexa.practice.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRepository extends JpaRepository<ChatThread, UUID> {

    Optional<ChatThread> findByIdAndUserId(UUID id, UUID userId);

    List<ChatThread> findByUserIdOrderByUpdatedAtDesc(UUID userId);

    List<ChatThread> findByUserIdAndProblemIdOrderByUpdatedAtDesc(UUID userId, UUID problemId);
}
