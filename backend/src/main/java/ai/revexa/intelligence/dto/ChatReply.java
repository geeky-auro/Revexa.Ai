package ai.revexa.intelligence.dto;

import java.util.List;

/**
 * A mentor turn. {@code spoilerLevel} mirrors {@link HintLevel#order()} so the UI can badge how far
 * the conversation has gone, and {@code revealedSolution} records an explicit, user-requested reveal.
 */
public record ChatReply(
        String message,
        List<String> followUpQuestions,
        int spoilerLevel,
        boolean revealedSolution,
        String provider,
        String model) {}
