package ai.revexa.practice.domain;

import ai.revexa.core.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "chat_message")
@Getter
@Setter
public class ChatMessage extends BaseEntity {

    @Column(name = "thread_id", nullable = false)
    private UUID threadId;

    @Column(name = "role", nullable = false, length = 16)
    private String role;

    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "follow_ups", columnDefinition = "text")
    private String followUps;

    @Column(name = "spoiler_level", nullable = false)
    private int spoilerLevel = 1;

    @Column(name = "revealed_solution", nullable = false)
    private boolean revealedSolution = false;
}
