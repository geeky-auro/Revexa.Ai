package ai.revexa.practice.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HintRepository extends JpaRepository<HintRecord, UUID> {

    List<HintRecord> findBySessionIdOrderByLevelAsc(UUID sessionId);

    Optional<HintRecord> findBySessionIdAndLevel(UUID sessionId, int level);
}
