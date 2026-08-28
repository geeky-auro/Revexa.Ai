package ai.revexa.practice.dto;

import ai.revexa.intelligence.dto.ChatReply;
import ai.revexa.intelligence.dto.CodeReviewResult;
import ai.revexa.intelligence.dto.GeneratedHint;
import ai.revexa.intelligence.dto.SolutionComparison;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class PracticeDtos {

    private PracticeDtos() {}

    // ----------------------------------------------------------- submissions

    public record CreateSubmissionRequest(
            @NotNull UUID problemId,
            @NotBlank @Size(max = 60_000) String code,
            @NotBlank @Size(max = 32) String language,
            @Size(max = 1000) String notes) {}

    public record SubmissionView(
            UUID id,
            UUID problemId,
            String problemTitle,
            String language,
            String code,
            String notes,
            Instant createdAt,
            UUID latestReviewId) {}

    // --------------------------------------------------------------- reviews

    public record CreateReviewRequest(
            UUID submissionId,
            UUID problemId,
            @Size(max = 60_000) String code,
            @Size(max = 32) String language,
            boolean force) {}

    public record ReviewView(
            UUID id,
            UUID submissionId,
            UUID problemId,
            String problemTitle,
            String language,
            CodeReviewResult result,
            Instant createdAt) {}

    public record ReviewSummary(
            UUID id,
            UUID problemId,
            String problemTitle,
            String verdict,
            int score,
            String timeComplexity,
            String optimalTime,
            boolean timeOptimal,
            String approachName,
            String language,
            List<String> topics,
            Instant createdAt) {}

    // ----------------------------------------------------------------- hints

    public record StartHintSessionRequest(@NotNull UUID problemId, UUID submissionId) {}

    public record HintSessionView(
            UUID id,
            UUID problemId,
            int currentLevel,
            int maxLevel,
            boolean solutionRevealed,
            List<GeneratedHint> hints,
            LadderStep[] ladder) {}

    public record LadderStep(int level, String name, String title, String intent, boolean unlocked, boolean spoiler) {}

    /** {@code revealSolution} must be sent explicitly to unlock the final rung. */
    public record NextHintRequest(boolean revealSolution, @Size(max = 60_000) String code, String language) {}

    // ------------------------------------------------------------------ chat

    public record StartThreadRequest(@NotNull UUID problemId, UUID submissionId, @Size(max = 200) String title) {}

    public record SendMessageRequest(
            @NotBlank @Size(max = 4000) String message,
            boolean revealSolution,
            @Size(max = 60_000) String code,
            String language) {}

    public record ChatMessageView(
            UUID id, String role, String content, List<String> followUpQuestions, int spoilerLevel, boolean revealedSolution, Instant createdAt) {}

    public record ChatThreadView(
            UUID id,
            UUID problemId,
            String problemTitle,
            String title,
            int maxSpoilerLevel,
            List<ChatMessageView> messages,
            Instant createdAt) {}

    public record ChatTurn(ChatMessageView userMessage, ChatMessageView reply, ChatReply raw) {}

    // ------------------------------------------------------------ comparison

    public record ComparisonRequest(
            @NotNull UUID problemId,
            UUID submissionId,
            @Size(max = 60_000) String code,
            @Size(max = 32) String language,
            boolean revealPseudocode) {}

    public record ComparisonView(UUID problemId, String problemTitle, SolutionComparison comparison) {}

    // ------------------------------------------------------------- bookmarks

    public record CreateBookmarkRequest(
            @NotBlank @Size(max = 24) String kind,
            UUID problemId,
            UUID referenceId,
            @NotBlank @Size(max = 200) String title,
            @NotBlank @Size(max = 20_000) String content,
            @Size(max = 500) String note) {}

    public record BookmarkView(
            UUID id,
            String kind,
            UUID problemId,
            String problemTitle,
            UUID referenceId,
            String title,
            String content,
            String note,
            Instant createdAt) {}
}
