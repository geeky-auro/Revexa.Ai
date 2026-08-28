package ai.revexa.practice.service;

import ai.revexa.catalog.api.ProblemService;
import ai.revexa.catalog.domain.Problem;
import ai.revexa.core.error.ApiException;
import ai.revexa.core.json.Json;
import ai.revexa.intelligence.dto.GeneratedHint;
import ai.revexa.intelligence.dto.HintLevel;
import ai.revexa.intelligence.pipeline.ReviewPipeline;
import ai.revexa.intelligence.pipeline.StageContext;
import ai.revexa.practice.domain.HintRecord;
import ai.revexa.practice.domain.HintRepository;
import ai.revexa.practice.domain.HintSession;
import ai.revexa.practice.domain.HintSessionRepository;
import ai.revexa.practice.domain.Submission;
import ai.revexa.practice.dto.PracticeDtos;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns the progressive hint ladder.
 *
 * <p>Two rules are enforced here rather than in the UI, because they are the product: a session
 * advances exactly one level per request, and the final rung ({@link HintLevel#SOLUTION}) is only
 * reachable when the caller explicitly asks to reveal it.
 */
@Service
public class HintService {

    private final HintSessionRepository sessions;
    private final HintRepository hints;
    private final ProblemService problems;
    private final SubmissionService submissions;
    private final ReviewPipeline pipeline;
    private final StageContextFactory contexts;
    private final Json json;

    public HintService(
            HintSessionRepository sessions,
            HintRepository hints,
            ProblemService problems,
            SubmissionService submissions,
            ReviewPipeline pipeline,
            StageContextFactory contexts,
            Json json) {
        this.sessions = sessions;
        this.hints = hints;
        this.problems = problems;
        this.submissions = submissions;
        this.pipeline = pipeline;
        this.contexts = contexts;
        this.json = json;
    }

    @Transactional
    public PracticeDtos.HintSessionView start(PracticeDtos.StartHintSessionRequest request, UUID userId) {
        Problem problem = problems.require(request.problemId(), userId);
        HintSession session =
                sessions
                        .findByUserIdAndProblemId(userId, problem.getId())
                        .orElseGet(
                                () -> {
                                    HintSession created = new HintSession();
                                    created.setUserId(userId);
                                    created.setProblemId(problem.getId());
                                    created.setSubmissionId(request.submissionId());
                                    return sessions.save(created);
                                });
        if (request.submissionId() != null && session.getSubmissionId() == null) {
            session.setSubmissionId(request.submissionId());
        }
        return view(session);
    }

    @Transactional(readOnly = true)
    public PracticeDtos.HintSessionView get(UUID sessionId, UUID userId) {
        return view(require(sessionId, userId));
    }

    @Transactional
    public PracticeDtos.HintSessionView next(UUID sessionId, PracticeDtos.NextHintRequest request, UUID userId) {
        HintSession session = require(sessionId, userId);
        Problem problem = problems.require(session.getProblemId(), userId);

        int nextLevel = session.getCurrentLevel() + 1;
        if (nextLevel > HintLevel.SOLUTION.order()) {
            throw ApiException.badRequest("You have already reached the final rung of the hint ladder.");
        }
        if (nextLevel == HintLevel.SOLUTION.order() && !request.revealSolution()) {
            throw ApiException.badRequest(
                    "The full solution is the last rung and is never shown by default. Re-send with revealSolution=true "
                            + "if you are sure — the pseudocode rung usually gets you there on your own.");
        }

        String code = request.code();
        String language = request.language();
        if ((code == null || code.isBlank()) && session.getSubmissionId() != null) {
            Submission submission = submissions.require(session.getSubmissionId(), userId);
            code = submission.getCode();
            language = submission.getLanguage();
        }

        StageContext context =
                contexts
                        .from(problem)
                        .code(code)
                        .language(language)
                        .hintLevel(nextLevel)
                        .allowSpoilers(request.revealSolution())
                        .build();

        GeneratedHint hint = pipeline.hint(context);

        HintRecord record = hints.findBySessionIdAndLevel(session.getId(), nextLevel).orElseGet(HintRecord::new);
        record.setSessionId(session.getId());
        record.setLevel(nextLevel);
        record.setTitle(hint.title());
        record.setPayload(json.write(hint));
        record.setSpoiler(hint.spoiler());
        hints.save(record);

        session.setCurrentLevel(nextLevel);
        if (nextLevel == HintLevel.SOLUTION.order()) {
            session.setSolutionRevealed(true);
        }
        sessions.save(session);
        return view(session);
    }

    @Transactional(readOnly = true)
    public HintSession require(UUID sessionId, UUID userId) {
        return sessions
                .findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> ApiException.notFound("Hint session"));
    }

    private PracticeDtos.HintSessionView view(HintSession session) {
        List<GeneratedHint> revealed =
                hints.findBySessionIdOrderByLevelAsc(session.getId()).stream()
                        .map(record -> json.read(record.getPayload(), GeneratedHint.class))
                        .toList();

        PracticeDtos.LadderStep[] ladder =
                java.util.Arrays.stream(HintLevel.values())
                        .map(
                                level ->
                                        new PracticeDtos.LadderStep(
                                                level.order(),
                                                level.name(),
                                                level.title(),
                                                level.intent(),
                                                level.order() <= session.getCurrentLevel(),
                                                level.isSpoiler()))
                        .toArray(PracticeDtos.LadderStep[]::new);

        return new PracticeDtos.HintSessionView(
                session.getId(),
                session.getProblemId(),
                session.getCurrentLevel(),
                HintLevel.SOLUTION.order(),
                session.isSolutionRevealed(),
                revealed,
                ladder);
    }
}
