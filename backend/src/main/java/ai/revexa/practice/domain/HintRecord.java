package ai.revexa.practice.domain;

import ai.revexa.core.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "hint_record")
@Getter
@Setter
public class HintRecord extends BaseEntity {

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "level", nullable = false)
    private int level;

    @Column(name = "title", nullable = false, length = 160)
    private String title;

    @Column(name = "payload", nullable = false, columnDefinition = "text")
    private String payload;

    @Column(name = "spoiler", nullable = false)
    private boolean spoiler;
}
