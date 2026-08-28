package ai.revexa.catalog.domain;

import ai.revexa.core.domain.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "problem")
public class Problem extends BaseEntity {

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "slug", nullable = false, length = 300)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 32)
    private PlatformSource source = PlatformSource.MANUAL;

    @Column(name = "external_id", length = 120)
    private String externalId;

    @Column(name = "url", length = 500)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false, length = 16)
    private Difficulty difficulty = Difficulty.UNKNOWN;

    @Column(name = "statement", nullable = false, columnDefinition = "text")
    private String statement;

    @Column(name = "constraints_text", columnDefinition = "text")
    private String constraintsText;

    @Column(name = "examples", columnDefinition = "text")
    private String examples;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "problem_topic", joinColumns = @JoinColumn(name = "problem_id"))
    @Column(name = "topic", nullable = false, length = 80)
    private List<String> topics = new ArrayList<>();

    /** {@code null} means the problem is part of the shared library rather than a user's own import. */
    @Column(name = "owner_id")
    private UUID ownerId;

    @Column(name = "curated", nullable = false)
    private boolean curated = false;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public PlatformSource getSource() {
        return source;
    }

    public void setSource(PlatformSource source) {
        this.source = source;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public String getStatement() {
        return statement;
    }

    public void setStatement(String statement) {
        this.statement = statement;
    }

    public String getConstraintsText() {
        return constraintsText;
    }

    public void setConstraintsText(String constraintsText) {
        this.constraintsText = constraintsText;
    }

    public String getExamples() {
        return examples;
    }

    public void setExamples(String examples) {
        this.examples = examples;
    }

    public List<String> getTopics() {
        return topics;
    }

    public void setTopics(List<String> topics) {
        this.topics = topics;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public boolean isCurated() {
        return curated;
    }

    public void setCurated(boolean curated) {
        this.curated = curated;
    }
}
