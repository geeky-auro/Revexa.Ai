package ai.revexa.practice.api;

import ai.revexa.core.security.CurrentUser;
import ai.revexa.core.web.PageResponse;
import ai.revexa.practice.dto.PracticeDtos;
import ai.revexa.practice.service.BookmarkService;
import ai.revexa.practice.service.ComparisonService;
import ai.revexa.practice.service.ReviewService;
import ai.revexa.practice.service.SubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Submissions, reviews, comparisons and bookmarks — the core practice loop. */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Practice")
public class PracticeController {

    private final SubmissionService submissions;
    private final ReviewService reviews;
    private final ComparisonService comparisons;
    private final BookmarkService bookmarks;

    public PracticeController(
            SubmissionService submissions,
            ReviewService reviews,
            ComparisonService comparisons,
            BookmarkService bookmarks) {
        this.submissions = submissions;
        this.reviews = reviews;
        this.comparisons = comparisons;
        this.bookmarks = bookmarks;
    }

    // ----------------------------------------------------------- submissions

    @PostMapping("/submissions")
    @Operation(summary = "Store a code submission")
    public ResponseEntity<PracticeDtos.SubmissionView> submit(
            @Valid @RequestBody PracticeDtos.CreateSubmissionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(submissions.create(request, CurrentUser.requireId()));
    }

    @GetMapping("/submissions")
    @Operation(summary = "Your submissions, newest first")
    public PageResponse<PracticeDtos.SubmissionView> listSubmissions(
            @RequestParam(required = false) UUID problemId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = CurrentUser.requireId();
        if (problemId != null) {
            List<PracticeDtos.SubmissionView> forProblem = submissions.forProblem(problemId, userId);
            return new PageResponse<>(forProblem, 0, forProblem.size(), forProblem.size(), 1, false);
        }
        return PageResponse.of(
                submissions.list(
                        userId,
                        PageRequest.of(Math.max(0, page), Math.min(100, size), Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    @GetMapping("/submissions/{id}")
    @Operation(summary = "Fetch one submission")
    public PracticeDtos.SubmissionView getSubmission(@PathVariable UUID id) {
        return submissions.get(id, CurrentUser.requireId());
    }

    // --------------------------------------------------------------- reviews

    @PostMapping("/reviews")
    @Operation(summary = "Run the AI review pipeline over a submission")
    public ResponseEntity<PracticeDtos.ReviewView> review(
            @Valid @RequestBody PracticeDtos.CreateReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reviews.review(request, CurrentUser.requireId()));
    }

    @GetMapping("/reviews/{id}")
    @Operation(summary = "Fetch a stored review, exactly as it was produced")
    public PracticeDtos.ReviewView getReview(@PathVariable UUID id) {
        return reviews.get(id, CurrentUser.requireId());
    }

    @GetMapping("/reviews")
    @Operation(summary = "Your review history")
    public PageResponse<PracticeDtos.ReviewSummary> reviewHistory(
            @RequestParam(required = false) UUID problemId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = CurrentUser.requireId();
        if (problemId != null) {
            List<PracticeDtos.ReviewSummary> forProblem = reviews.forProblem(problemId, userId);
            return new PageResponse<>(forProblem, 0, forProblem.size(), forProblem.size(), 1, false);
        }
        return PageResponse.of(
                reviews.history(
                        userId,
                        PageRequest.of(Math.max(0, page), Math.min(100, size), Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    // ------------------------------------------------------------ comparison

    @PostMapping("/comparisons")
    @Operation(summary = "Compare your approach against the optimal one and the alternatives")
    public PracticeDtos.ComparisonView compare(@Valid @RequestBody PracticeDtos.ComparisonRequest request) {
        return comparisons.compare(request, CurrentUser.requireId());
    }

    // ------------------------------------------------------------- bookmarks

    @PostMapping("/bookmarks")
    @Operation(summary = "Save an explanation for later")
    public ResponseEntity<PracticeDtos.BookmarkView> bookmark(
            @Valid @RequestBody PracticeDtos.CreateBookmarkRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookmarks.create(request, CurrentUser.requireId()));
    }

    @GetMapping("/bookmarks")
    @Operation(summary = "Your saved explanations")
    public List<PracticeDtos.BookmarkView> bookmarks() {
        return bookmarks.list(CurrentUser.requireId());
    }

    @DeleteMapping("/bookmarks/{id}")
    @Operation(summary = "Remove a bookmark")
    public ResponseEntity<Void> deleteBookmark(@PathVariable UUID id) {
        bookmarks.delete(id, CurrentUser.requireId());
        return ResponseEntity.noContent().build();
    }
}
