package ai.revexa.intelligence.dto;

import java.util.List;

/** Stage 1 output: what the problem actually asks, in plain language. */
public record ProblemUnderstanding(
        String restatement,
        List<String> inputs,
        String output,
        List<String> constraints,
        List<String> easyToMisread,
        List<String> topics,
        String difficultyGuess,
        List<String> clarifyingQuestions) {}
