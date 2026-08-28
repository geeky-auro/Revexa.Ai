package ai.revexa.practice.domain;

import ai.revexa.core.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "chat_thread")
@Getter
@Setter
public class ChatThread extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "problem_id", nullable = false)
    private UUID problemId;

    @Column(name = "submission_id")
    private UUID submissionId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /** Highest hint level reached in this conversation, so the UI can badge spoiler exposure. */
    @Column(name = "max_spoiler_level", nullable = false)
    private int maxSpoilerLevel = 1;
}
