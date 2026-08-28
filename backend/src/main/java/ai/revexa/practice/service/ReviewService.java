package ai.revexa.practice.service;

import ai.revexa.catalog.api.ProblemService;
import ai.revexa.catalog.domain.Problem;
import ai.revexa.core.error.ApiException;
import ai.revexa.core.json.Json;
import ai.revexa.intelligence.dto.CodeReviewResult;
import ai.revexa.intelligence.dto.Finding;
import ai.revexa.intelligence.pipeline.ReviewPipeline;
import ai.revexa.intelligence.pipeline.StageContext;
import ai.revexa.practice.domain.Review;
import ai.revexa.practice.domain.ReviewRepository;
import ai.revexa.practice.domain.Submission;
import ai.revexa.practice.dto.PracticeDtos;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Runs the review pipeline for a submission and persists the structured result. */
@Service
public class ReviewService {

    private final ReviewRepository repository;
    private final SubmissionService submissionService;
    private final ProblemService problemService;
    private final ReviewPipeline pipeline;
    private final StageContextFactory contexts;
    private final Json json;

    public ReviewService(
            ReviewRepository repository,
            SubmissionService submissionService,
            ProblemService problemService,
            ReviewPipeline pipeline,
            StageContextFactory contexts,
            Json json) {
        this.repository = repository;
        this.submissionService = submissionService;
        this.problemService = problemService;
        this.pipeline = pipeline;
        this.contexts = contexts;
        this.json = json;
    }

    @Transactional
    public PracticeDtos.ReviewView review(PracticeDtos.CreateReviewRequest request, UUID userId) {
        Submission submission;
        Problem problem;

        if (request.submissionId() != null) {
            submission = submissionService.require(request.submissionId(), userId);
            problem = problemService.require(submission.getProblemId(), userId);
        } else {
            if (request.problemId() == null || request.code() == null || request.code().isBlank()) {
                throw ApiException.badRequest("Provide either a submissionId, or a problemId together with code.");
            }
            problem = problemService.require(request.problemId(), userId);
            PracticeDtos.SubmissionView created =
                    submissionService.create(
                            new PracticeDtos.CreateSubmissionRequest(
                                    problem.getId(),
                                    request.code(),
                                    request.language() == null ? "python" : request.language(),
                                    null),
                            userId);
            submission = submissionService.require(created.id(), userId);
        }

        if (!request.force()) {
            var existing = repository.findFirstBySubmissionIdOrderByCreatedAtDesc(submission.getId());
            if (existing.isPresent()) {
                return toView(existing.get(), problem.getTitle(), submission.getLanguage());
            }
        }

        StageContext context = contexts.forCode(problem, submission.getCode(), submission.getLanguage());
        CodeReviewResult result = pipeline.review(context);

        Review review = new Review();
        review.setUserId(userId);
        review.setProblemId(problem.getId());
        review.setSubmissionId(submission.getId());
        review.setPayload(json.write(result));
        review.setVerdict(result.verdict());
        review.setScore(result.score());
        review.setTimeComplexity(result.complexity().time());
        review.setSpaceComplexity(result.complexity().space());
        review.setOptimalTime(result.complexity().optimalTime());
        review.setOptimalSpace(result.complexity().optimalSpace());
        review.setTimeOptimal(result.complexity().timeOptimal());
        review.setLanguage(submission.getLanguage());
        review.setApproachName(truncate(result.approach().name(), 120));
        review.setProvider(result.provider());
        review.setModel(result.model());
        review.setTopics(new ArrayList<>(result.topics() == null ? List.of() : result.topics()));
        review.setIssueTitles(
                result.findings() == null
                        ? new ArrayList<>()
                        : new ArrayList<>(result.findings().stream().map(Finding::title).map(t -> truncate(t, 160)).toList()));

        return toView(repository.save(review), problem.getTitle(), submission.getLanguage());
    }

    @Transactional(readOnly = true)
    public PracticeDtos.ReviewView get(UUID id, UUID userId) {
        Review review = repository.findByIdAndUserId(id, userId).orElseThrow(() -> ApiException.notFound("Review"));
        Problem problem = problemService.require(review.getProblemId(), userId);
        return toView(review, problem.getTitle(), review.getLanguage());
    }

    @Transactional(readOnly = true)
    public Page<PracticeDtos.ReviewSummary> history(UUID userId, Pageable pageable) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId, pageable).map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public List<PracticeDtos.ReviewSummary> forProblem(UUID problemId, UUID userId) {
        return repository.findByUserIdAndProblemIdOrderByCreatedAtDesc(userId, problemId).stream()
                .map(this::toSummary)
                .toList();
    }

    private PracticeDtos.ReviewView toView(Review review, String problemTitle, String language) {
        return new PracticeDtos.ReviewView(
                review.getId(),
                review.getSubmissionId(),
                review.getProblemId(),
                problemTitle,
                language,
                json.read(review.getPayload(), CodeReviewResult.class),
                review.getCreatedAt());
    }

    private PracticeDtos.ReviewSummary toSummary(Review review) {
        return new PracticeDtos.ReviewSummary(
                review.getId(),
                review.getProblemId(),
                problemService.titleOf(review.getProblemId()),
                review.getVerdict(),
                review.getScore(),
                review.getTimeComplexity(),
                review.getOptimalTime(),
                review.isTimeOptimal(),
                review.getApproachName(),
                review.getLanguage(),
                review.getTopics(),
                review.getCreatedAt());
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max - 1) + "…";
    }
}
