package ai.revexa.practice.domain;

import ai.revexa.core.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** A snapshot of code a learner submitted for review. Immutable once written. */
@Entity
@Table(name = "submission")
@Getter
@Setter
public class Submission extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "problem_id", nullable = false)
    private UUID problemId;

    @Column(name = "language", nullable = false, length = 32)
    private String language;

    @Column(name = "code", nullable = false, columnDefinition = "text")
    private String code;

    @Column(name = "notes", length = 1000)
    private String notes;
}
