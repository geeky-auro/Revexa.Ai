package ai.revexa.catalog.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProblemRepository extends JpaRepository<Problem, UUID> {

    Optional<Problem> findBySlugAndOwnerId(String slug, UUID ownerId);

    Optional<Problem> findBySlugAndCuratedTrue(String slug);

    /** A user sees the curated library plus anything they imported themselves. */
    @Query(
            """
            select p from Problem p
            where (p.curated = true or p.ownerId = :ownerId)
              and (:search is null or lower(p.title) like lower(concat('%', :search, '%')))
              and (:difficulty is null or p.difficulty = :difficulty)
            """)
    Page<Problem> findVisible(
            @Param("ownerId") UUID ownerId,
            @Param("search") String search,
            @Param("difficulty") Difficulty difficulty,
            Pageable pageable);

    long countByCuratedTrue();
}
