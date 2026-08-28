package ai.revexa.practice.service;

import ai.revexa.catalog.api.ProblemService;
import ai.revexa.catalog.domain.Problem;
import ai.revexa.core.error.ApiException;
import ai.revexa.practice.domain.Review;
import ai.revexa.practice.domain.ReviewRepository;
import ai.revexa.practice.domain.Submission;
import ai.revexa.practice.domain.SubmissionRepository;
import ai.revexa.practice.dto.PracticeDtos;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubmissionService {

    private static final List<String> SUPPORTED_LANGUAGES =
            List.of("python", "java", "cpp", "c", "javascript", "typescript", "go", "rust", "kotlin", "csharp", "ruby", "swift");

    private final SubmissionRepository submissions;
    private final ReviewRepository reviews;
    private final ProblemService problems;

    public SubmissionService(
            SubmissionRepository submissions, ReviewRepository reviews, ProblemService problems) {
        this.submissions = submissions;
        this.reviews = reviews;
        this.problems = problems;
    }

    public static List<String> supportedLanguages() {
        return SUPPORTED_LANGUAGES;
    }

    @Transactional
    public PracticeDtos.SubmissionView create(PracticeDtos.CreateSubmissionRequest request, UUID userId) {
        Problem problem = problems.require(request.problemId(), userId);
        String language = request.language().toLowerCase();
        if (!SUPPORTED_LANGUAGES.contains(language)) {
            throw ApiException.badRequest(
                    "Unsupported language '" + request.language() + "'. Supported: " + String.join(", ", SUPPORTED_LANGUAGES));
        }

        Submission submission = new Submission();
        submission.setUserId(userId);
        submission.setProblemId(problem.getId());
        submission.setLanguage(language);
        submission.setCode(request.code());
        submission.setNotes(request.notes());
        return toView(submissions.save(submission), problem.getTitle());
    }

    @Transactional(readOnly = true)
    public Submission require(UUID id, UUID userId) {
        return submissions
                .findByIdAndUserId(id, userId)
                .orElseThrow(() -> ApiException.notFound("Submission"));
    }

    @Transactional(readOnly = true)
    public PracticeDtos.SubmissionView get(UUID id, UUID userId) {
        Submission submission = require(id, userId);
        Problem problem = problems.require(submission.getProblemId(), userId);
        return toView(submission, problem.getTitle());
    }

    @Transactional(readOnly = true)
    public Page<PracticeDtos.SubmissionView> list(UUID userId, Pageable pageable) {
        return submissions
                .findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(
                        submission -> {
                            Problem problem = problems.require(submission.getProblemId(), userId);
                            return toView(submission, problem.getTitle());
                        });
    }

    @Transactional(readOnly = true)
    public List<PracticeDtos.SubmissionView> forProblem(UUID problemId, UUID userId) {
        Problem problem = problems.require(problemId, userId);
        return submissions.findByUserIdAndProblemIdOrderByCreatedAtDesc(userId, problemId).stream()
                .map(submission -> toView(submission, problem.getTitle()))
                .toList();
    }

    private PracticeDtos.SubmissionView toView(Submission submission, String problemTitle) {
        UUID latestReview =
                reviews.findFirstBySubmissionIdOrderByCreatedAtDesc(submission.getId())
                        .map(Review::getId)
                        .orElse(null);
        return new PracticeDtos.SubmissionView(
                submission.getId(),
                submission.getProblemId(),
                problemTitle,
                submission.getLanguage(),
                submission.getCode(),
                submission.getNotes(),
                submission.getCreatedAt(),
                latestReview);
    }
}
