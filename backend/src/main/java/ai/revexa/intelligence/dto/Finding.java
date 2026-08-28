package ai.revexa.intelligence.dto;

/** A single correctness, edge-case or quality observation about the submitted code. */
public record Finding(
        String type, String severity, String title, String detail, String suggestion, Integer line) {

    public static final String TYPE_CORRECTNESS = "CORRECTNESS";
    public static final String TYPE_EDGE_CASE = "EDGE_CASE";
    public static final String TYPE_PERFORMANCE = "PERFORMANCE";
    public static final String TYPE_READABILITY = "READABILITY";

    public static final String SEVERITY_CRITICAL = "CRITICAL";
    public static final String SEVERITY_HIGH = "HIGH";
    public static final String SEVERITY_MEDIUM = "MEDIUM";
    public static final String SEVERITY_LOW = "LOW";
}
