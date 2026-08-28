package ai.revexa.intelligence.pipeline;

import ai.revexa.intelligence.llm.LlmMessage;
import java.util.List;

/**
 * Everything any pipeline stage might need, in one immutable value.
 *
 * <p>Hosted providers see this rendered into a prompt; the offline engine reads the fields directly.
 */
public record StageContext(
        String problemTitle,
        String problemStatement,
        String constraintsText,
        List<String> topics,
        String difficulty,
        String code,
        String language,
        int hintLevel,
        String question,
        List<LlmMessage> history,
        boolean allowSpoilers,
        String mentorMode) {

    public static Builder builder() {
        return new Builder();
    }

    public boolean hasCode() {
        return code != null && !code.isBlank();
    }

    public String safeStatement() {
        return problemStatement == null ? "" : problemStatement;
    }

    public static final class Builder {
        private String problemTitle = "";
        private String problemStatement = "";
        private String constraintsText = "";
        private List<String> topics = List.of();
        private String difficulty = "";
        private String code = "";
        private String language = "";
        private int hintLevel = 1;
        private String question = "";
        private List<LlmMessage> history = List.of();
        private boolean allowSpoilers = false;
        private String mentorMode = "guided";

        public Builder problemTitle(String v) {
            this.problemTitle = v;
            return this;
        }

        public Builder problemStatement(String v) {
            this.problemStatement = v;
            return this;
        }

        public Builder constraintsText(String v) {
            this.constraintsText = v;
            return this;
        }

        public Builder topics(List<String> v) {
            this.topics = v == null ? List.of() : v;
            return this;
        }

        public Builder difficulty(String v) {
            this.difficulty = v;
            return this;
        }

        public Builder code(String v) {
            this.code = v;
            return this;
        }

        public Builder language(String v) {
            this.language = v;
            return this;
        }

        public Builder hintLevel(int v) {
            this.hintLevel = v;
            return this;
        }

        public Builder question(String v) {
            this.question = v;
            return this;
        }

        public Builder history(List<LlmMessage> v) {
            this.history = v == null ? List.of() : v;
            return this;
        }

        public Builder allowSpoilers(boolean v) {
            this.allowSpoilers = v;
            return this;
        }

        public Builder mentorMode(String v) {
            this.mentorMode = v == null ? "guided" : v;
            return this;
        }

        public StageContext build() {
            return new StageContext(
                    problemTitle,
                    problemStatement,
                    constraintsText,
                    topics,
                    difficulty,
                    code,
                    language,
                    hintLevel,
                    question,
                    history,
                    allowSpoilers,
                    mentorMode);
        }
    }
}
