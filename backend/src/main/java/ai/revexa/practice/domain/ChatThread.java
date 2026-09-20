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

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getProblemId() {
        return problemId;
    }

    public void setProblemId(UUID problemId) {
        this.problemId = problemId;
    }

    public UUID getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(UUID submissionId) {
        this.submissionId = submissionId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getMaxSpoilerLevel() {
        return maxSpoilerLevel;
    }

    public void setMaxSpoilerLevel(int maxSpoilerLevel) {
        this.maxSpoilerLevel = maxSpoilerLevel;
    }
}
