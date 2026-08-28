package ai.revexa.intelligence.dto;

/**
 * The mentoring ladder. Every rung is a deliberate step away from "here is the answer" — a session
 * always starts at {@link #CLARIFY} and can only advance one rung at a time, and {@link #SOLUTION}
 * is reachable only when the learner explicitly asks for it.
 */
public enum HintLevel {
    CLARIFY(1, "Clarify the problem", "Restate the task and pin down inputs, outputs and constraints."),
    OBSERVE(2, "Find the observation", "Surface the structure in the data that the solution leans on."),
    CONCEPT(3, "Conceptual hint", "Name the idea or invariant without naming the algorithm."),
    DIRECTION(4, "Algorithmic direction", "Point at the family of algorithms that fits."),
    PSEUDOCODE(5, "Pseudocode", "Give the shape of the implementation, not the code."),
    SOLUTION(6, "Full solution", "Complete, working implementation with commentary.");

    private final int order;
    private final String title;
    private final String intent;

    HintLevel(int order, String title, String intent) {
        this.order = order;
        this.title = title;
        this.intent = intent;
    }

    public int order() {
        return order;
    }

    public String title() {
        return title;
    }

    public String intent() {
        return intent;
    }

    public boolean isSpoiler() {
        return this == PSEUDOCODE || this == SOLUTION;
    }

    public HintLevel next() {
        return this == SOLUTION ? SOLUTION : values()[ordinal() + 1];
    }

    public static HintLevel ofOrder(int order) {
        for (HintLevel level : values()) {
            if (level.order == order) {
                return level;
            }
        }
        return CLARIFY;
    }
}
