package ai.revexa.practice.domain;

import ai.revexa.core.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * A saved explanation. The content is snapshotted rather than referenced, so a bookmark survives even
 * if the underlying review is re-run and the wording changes.
 */
@Entity
@Table(name = "bookmark")
@Getter
@Setter
public class Bookmark extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "kind", nullable = false, length = 24)
    private String kind;

    @Column(name = "problem_id")
    private UUID problemId;

    @Column(name = "reference_id")
    private UUID referenceId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "note", length = 500)
    private String note;
}
