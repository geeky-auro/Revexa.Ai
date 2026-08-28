package ai.revexa.practice.service;

import ai.revexa.catalog.api.ProblemService;
import ai.revexa.catalog.domain.Problem;
import ai.revexa.core.error.ApiException;
import ai.revexa.core.json.Json;
import ai.revexa.intelligence.dto.ChatReply;
import ai.revexa.intelligence.llm.LlmMessage;
import ai.revexa.intelligence.pipeline.ReviewPipeline;
import ai.revexa.intelligence.pipeline.StageContext;
import ai.revexa.practice.domain.ChatMessage;
import ai.revexa.practice.domain.ChatMessageRepository;
import ai.revexa.practice.domain.ChatRepository;
import ai.revexa.practice.domain.ChatThread;
import ai.revexa.practice.domain.Submission;
import ai.revexa.practice.dto.PracticeDtos;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The conversational mentor.
 *
 * <p>The thread carries the problem context so the assistant never has to be re-told what is being
 * solved, and the spoiler gate is re-evaluated on every turn: an explicit reveal on one message does
 * not unlock the rest of the conversation.
 */
@Service
public class ChatService {

    /**
     * The only free-text phrases that unlock a full reveal.
     *
     * <p>Deliberately narrow. "Give me the solution" and friends are treated as a first ask: the
     * mentor answers with the key observation and tells the learner the exact phrase to use if they
     * still want everything. That two-step gate is what stops the product becoming an answer button —
     * the UI's explicit reveal control (the {@code revealSolution} flag) is the other way through.
     */
    private static final List<String> EXPLICIT_REVEAL =
            List.of("reveal the solution", "reveal the full solution", "reveal the answer");

    private final ChatRepository threads;
    private final ChatMessageRepository messages;
    private final ProblemService problems;
    private final SubmissionService submissions;
    private final ReviewPipeline pipeline;
    private final StageContextFactory contexts;
    private final Json json;

    public ChatService(
            ChatRepository threads,
            ChatMessageRepository messages,
            ProblemService problems,
            SubmissionService submissions,
            ReviewPipeline pipeline,
            StageContextFactory contexts,
            Json json) {
        this.threads = threads;
        this.messages = messages;
        this.problems = problems;
        this.submissions = submissions;
        this.pipeline = pipeline;
        this.contexts = contexts;
        this.json = json;
    }

    @Transactional
    public PracticeDtos.ChatThreadView start(PracticeDtos.StartThreadRequest request, UUID userId) {
        Problem problem = problems.require(request.problemId(), userId);
        ChatThread thread = new ChatThread();
        thread.setUserId(userId);
        thread.setProblemId(problem.getId());
        thread.setSubmissionId(request.submissionId());
        thread.setTitle(
                request.title() == null || request.title().isBlank() ? problem.getTitle() : request.title().strip());
        return view(threads.save(thread), problem.getTitle());
    }

    @Transactional(readOnly = true)
    public PracticeDtos.ChatThreadView get(UUID threadId, UUID userId) {
        ChatThread thread = require(threadId, userId);
        return view(thread, problems.titleOf(thread.getProblemId()));
    }

    @Transactional(readOnly = true)
    public List<PracticeDtos.ChatThreadView> list(UUID userId, UUID problemId) {
        List<ChatThread> found =
                problemId == null
                        ? threads.findByUserIdOrderByUpdatedAtDesc(userId)
                        : threads.findByUserIdAndProblemIdOrderByUpdatedAtDesc(userId, problemId);
        return found.stream().map(thread -> view(thread, problems.titleOf(thread.getProblemId()))).toList();
    }

    @Transactional
    public PracticeDtos.ChatTurn send(UUID threadId, PracticeDtos.SendMessageRequest request, UUID userId) {
        ChatThread thread = require(threadId, userId);
        Problem problem = problems.require(thread.getProblemId(), userId);

        String code = request.code();
        String language = request.language();
        if ((code == null || code.isBlank()) && thread.getSubmissionId() != null) {
            Submission submission = submissions.require(thread.getSubmissionId(), userId);
            code = submission.getCode();
            language = submission.getLanguage();
        }

        // The reveal gate is per turn: it must be asked for in this message, not inherited.
        boolean explicit = request.revealSolution() || mentionsExplicitReveal(request.message());

        List<ChatMessage> history = messages.findByThreadIdOrderByCreatedAtAsc(thread.getId());
        List<LlmMessage> llmHistory =
                history.stream()
                        .map(m -> new LlmMessage("user".equals(m.getRole()) ? "user" : "assistant", m.getContent()))
                        .toList();

        StageContext context =
                contexts
                        .from(problem)
                        .code(code)
                        .language(language)
                        .question(request.message())
                        .history(llmHistory)
                        .allowSpoilers(explicit)
                        .build();

        ChatReply reply = pipeline.chat(context);

        ChatMessage userMessage = new ChatMessage();
        userMessage.setThreadId(thread.getId());
        userMessage.setRole("user");
        userMessage.setContent(request.message());
        userMessage.setSpoilerLevel(0);
        messages.save(userMessage);

        ChatMessage assistantMessage = new ChatMessage();
        assistantMessage.setThreadId(thread.getId());
        assistantMessage.setRole("assistant");
        assistantMessage.setContent(reply.message());
        assistantMessage.setFollowUps(json.write(reply.followUpQuestions()));
        assistantMessage.setSpoilerLevel(reply.spoilerLevel());
        assistantMessage.setRevealedSolution(reply.revealedSolution());
        messages.save(assistantMessage);

        thread.setMaxSpoilerLevel(Math.max(thread.getMaxSpoilerLevel(), reply.spoilerLevel()));
        threads.save(thread);

        return new PracticeDtos.ChatTurn(toView(userMessage), toView(assistantMessage), reply);
    }

    @Transactional(readOnly = true)
    public ChatThread require(UUID threadId, UUID userId) {
        return threads.findByIdAndUserId(threadId, userId).orElseThrow(() -> ApiException.notFound("Chat thread"));
    }

    private boolean mentionsExplicitReveal(String message) {
        String lower = message == null ? "" : message.toLowerCase(Locale.ROOT);
        return EXPLICIT_REVEAL.stream().anyMatch(lower::contains);
    }

    private PracticeDtos.ChatThreadView view(ChatThread thread, String problemTitle) {
        List<PracticeDtos.ChatMessageView> views =
                messages.findByThreadIdOrderByCreatedAtAsc(thread.getId()).stream().map(this::toView).toList();
        return new PracticeDtos.ChatThreadView(
                thread.getId(),
                thread.getProblemId(),
                problemTitle,
                thread.getTitle(),
                thread.getMaxSpoilerLevel(),
                views,
                thread.getCreatedAt());
    }

    private PracticeDtos.ChatMessageView toView(ChatMessage message) {
        List<String> followUps =
                message.getFollowUps() == null || message.getFollowUps().isBlank()
                        ? List.of()
                        : json.read(message.getFollowUps(), new TypeReference<List<String>>() {});
        return new PracticeDtos.ChatMessageView(
                message.getId(),
                message.getRole(),
                message.getContent(),
                followUps,
                message.getSpoilerLevel(),
                message.isRevealedSolution(),
                message.getCreatedAt());
    }
}
