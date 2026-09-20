package ai.revexa.practice.domain;

import ai.revexa.core.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Tracks how far up the hint ladder a learner has climbed on one problem.
 *
 * <p>The level only ever advances by one, and only on an explicit request, which is what stops the
 * product from degenerating into an answer button.
 */
@Entity
@Table(name = "hint_session")
@Getter
@Setter
public class HintSession extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "problem_id", nullable = false)
    private UUID problemId;

    @Column(name = "submission_id")
    private UUID submissionId;

    @Column(name = "current_level", nullable = false)
    private int currentLevel = 0;

    @Column(name = "solution_revealed", nullable = false)
    private boolean solutionRevealed = false;

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

    public int getCurrentLevel() {
        return currentLevel;
    }

    public void setCurrentLevel(int currentLevel) {
        this.currentLevel = currentLevel;
    }

    public boolean isSolutionRevealed() {
        return solutionRevealed;
    }

    public void setSolutionRevealed(boolean solutionRevealed) {
        this.solutionRevealed = solutionRevealed;
    }
}
