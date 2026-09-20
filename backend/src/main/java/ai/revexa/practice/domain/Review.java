package ai.revexa.practice.domain;

import ai.revexa.core.domain.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * A completed AI review.
 *
 * <p>The full structured result is kept verbatim in {@code payload} so the UI can re-render an old
 * review exactly as it was produced, while the scalar fields are promoted to columns because the
 * progress dashboard aggregates over them.
 */
@Entity
@Table(name = "review")
@Getter
@Setter
public class Review extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "problem_id", nullable = false)
    private UUID problemId;

    @Column(name = "submission_id", nullable = false)
    private UUID submissionId;

    @Column(name = "payload", nullable = false, columnDefinition = "text")
    private String payload;

    @Column(name = "verdict", nullable = false, length = 24)
    private String verdict;

    @Column(name = "score", nullable = false)
    private int score;

    @Column(name = "time_complexity", nullable = false, length = 40)
    private String timeComplexity;

    @Column(name = "space_complexity", nullable = false, length = 40)
    private String spaceComplexity;

    @Column(name = "optimal_time", nullable = false, length = 40)
    private String optimalTime;

    @Column(name = "optimal_space", nullable = false, length = 40)
    private String optimalSpace;

    @Column(name = "time_optimal", nullable = false)
    private boolean timeOptimal;

    @Column(name = "language", nullable = false, length = 32)
    private String language;

    @Column(name = "approach_name", nullable = false, length = 120)
    private String approachName;

    @Column(name = "provider", nullable = false, length = 40)
    private String provider;

    @Column(name = "model", nullable = false, length = 80)
    private String model;

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

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getVerdict() {
        return verdict;
    }

    public void setVerdict(String verdict) {
        this.verdict = verdict;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getTimeComplexity() {
        return timeComplexity;
    }

    public void setTimeComplexity(String timeComplexity) {
        this.timeComplexity = timeComplexity;
    }

    public String getSpaceComplexity() {
        return spaceComplexity;
    }

    public void setSpaceComplexity(String spaceComplexity) {
        this.spaceComplexity = spaceComplexity;
    }

    public String getOptimalTime() {
        return optimalTime;
    }

    public void setOptimalTime(String optimalTime) {
        this.optimalTime = optimalTime;
    }

    public String getOptimalSpace() {
        return optimalSpace;
    }

    public void setOptimalSpace(String optimalSpace) {
        this.optimalSpace = optimalSpace;
    }

    public boolean isTimeOptimal() {
        return timeOptimal;
    }

    public void setTimeOptimal(boolean timeOptimal) {
        this.timeOptimal = timeOptimal;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getApproachName() {
        return approachName;
    }

    public void setApproachName(String approachName) {
        this.approachName = approachName;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<String> getTopics() {
        return topics;
    }

    public void setTopics(List<String> topics) {
        this.topics = topics;
    }

    public List<String> getIssueTitles() {
        return issueTitles;
    }

    public void setIssueTitles(List<String> issueTitles) {
        this.issueTitles = issueTitles;
    }

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "review_topic", joinColumns = @JoinColumn(name = "review_id"))
    @Column(name = "topic", nullable = false, length = 80)
    private List<String> topics = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "review_issue", joinColumns = @JoinColumn(name = "review_id"))
    @Column(name = "issue", nullable = false, length = 160)
    private List<String> issueTitles = new ArrayList<>();
}
