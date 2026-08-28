package ai.revexa.intelligence;

import static org.assertj.core.api.Assertions.assertThat;

import ai.revexa.core.json.Json;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JsonExtractionTest {

    @Test
    @DisplayName("recovers JSON from a fenced block")
    void stripsFences() {
        String raw = "```json\n{\"time\": \"O(n)\"}\n```";
        assertThat(Json.extractJson(raw)).contains("{\"time\": \"O(n)\"}");
    }

    @Test
    @DisplayName("recovers JSON wrapped in prose")
    void ignoresSurroundingProse() {
        String raw = "Here is the analysis you asked for:\n{\"verdict\": \"OPTIMAL\"}\nHope that helps!";
        assertThat(Json.extractJson(raw)).contains("{\"verdict\": \"OPTIMAL\"}");
    }

    @Test
    @DisplayName("does not stop at a brace inside a string literal")
    void handlesBracesInsideStrings() {
        String raw = "{\"pseudocode\": \"if (x) { return 1; }\", \"level\": 5}";
        assertThat(Json.extractJson(raw)).contains(raw);
    }

    @Test
    @DisplayName("handles an escaped quote inside a string literal")
    void handlesEscapedQuotes() {
        String raw = "{\"note\": \"he said \\\"use a map\\\" and left\"}";
        assertThat(Json.extractJson(raw)).contains(raw);
    }

    @Test
    @DisplayName("returns empty when there is no JSON at all")
    void returnsEmptyWithoutJson() {
        assertThat(Json.extractJson("I could not analyse that.")).isEmpty();
        assertThat(Json.extractJson("")).isEmpty();
        assertThat(Json.extractJson(null)).isEmpty();
    }
}
