package ai.revexa.intelligence.dto;

import java.util.List;

/** One rung of the hint ladder. */
public record GeneratedHint(
        int level,
        String levelName,
        String intent,
        String title,
        String content,
        List<String> socraticQuestions,
        boolean spoiler,
        boolean lastLevel) {}
