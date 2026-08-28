package ai.revexa.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** Walks the whole product loop over HTTP: register, import, submit, review, hint, chat. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PracticeWorkflowTest {

    private static final String STATEMENT =
            """
            Two Sum

            You are given an array of integers nums and an integer target. Return the indices of the two
            numbers that add up to target.

            Constraints:
            2 <= nums.length <= 10^4
            """;

    private static final String BRUTE_FORCE =
            """
            def two_sum(nums, target):
                for i in range(len(nums)):
                    for j in range(i + 1, len(nums)):
                        if nums[i] + nums[j] == target:
                            return [i, j]
                return []
            """;

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;

    private String token;

    @BeforeEach
    void signUp() throws Exception {
        String email = "learner-" + java.util.UUID.randomUUID() + "@revexa.ai";
        MvcResult result =
                mvc.perform(
                                post("/api/v1/auth/register")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                json(
                                                        Map.of(
                                                                "email", email,
                                                                "displayName", "Test Learner",
                                                                "password", "supersecret123"))))
                        .andExpect(status().isCreated())
                        .andReturn();
        token = "Bearer " + node(result).path("accessToken").asText();
    }

    @Test
    @DisplayName("rejects unauthenticated access to a protected endpoint")
    void requiresAuthentication() throws Exception {
        mvc.perform(get("/api/v1/problems")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("serves public metadata without a token")
    void servesPublicMeta() throws Exception {
        mvc.perform(get("/api/v1/meta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hintLadder.length()").value(6))
                .andExpect(jsonPath("$.aiProviders").isArray());
    }

    @Test
    @DisplayName("rejects a registration with a weak password")
    void validatesRegistration() throws Exception {
        mvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json(Map.of("email", "x@revexa.ai", "displayName", "X", "password", "short"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"));
    }

    @Test
    @DisplayName("runs the full loop: import a problem, review a submission, then climb the hint ladder")
    void fullLoop() throws Exception {
        // Import through the PracticePlatformProvider abstraction.
        MvcResult imported =
                mvc.perform(
                                post("/api/v1/problems/import")
                                        .header("Authorization", token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(json(Map.of("rawText", STATEMENT, "title", "Two Sum"))))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.providerId").value("manual"))
                        .andReturn();
        String problemId = node(imported).path("problem").path("id").asText();

        // Review a deliberately quadratic submission.
        MvcResult reviewed =
                mvc.perform(
                                post("/api/v1/reviews")
                                        .header("Authorization", token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                json(
                                                        Map.of(
                                                                "problemId", problemId,
                                                                "code", BRUTE_FORCE,
                                                                "language", "python",
                                                                "force", true))))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.result.complexity.time").value("O(n^2)"))
                        .andExpect(jsonPath("$.result.optimization.betterApproachExists").value(true))
                        .andReturn();
        assertThat(node(reviewed).path("result").path("findings")).isNotEmpty();

        // Open the hint ladder and take one rung.
        MvcResult session =
                mvc.perform(
                                post("/api/v1/hints/sessions")
                                        .header("Authorization", token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(json(Map.of("problemId", problemId))))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.currentLevel").value(0))
                        .andReturn();
        String sessionId = node(session).path("id").asText();

        mvc.perform(
                        post("/api/v1/hints/sessions/" + sessionId + "/next")
                                .header("Authorization", token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json(Map.of("revealSolution", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentLevel").value(1))
                .andExpect(jsonPath("$.hints[0].spoiler").value(false));

        // The dashboard now has something to report.
        mvc.perform(get("/api/v1/progress/summary").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headline.reviewsRun").value(1))
                .andExpect(jsonPath("$.headline.problemsAttempted").value(1));
    }

    @Test
    @DisplayName("refuses to jump to the solution rung without an explicit reveal")
    void solutionRungIsGated() throws Exception {
        MvcResult imported =
                mvc.perform(
                                post("/api/v1/problems/import")
                                        .header("Authorization", token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(json(Map.of("rawText", STATEMENT, "title", "Two Sum"))))
                        .andReturn();
        String problemId = node(imported).path("problem").path("id").asText();

        MvcResult session =
                mvc.perform(
                                post("/api/v1/hints/sessions")
                                        .header("Authorization", token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(json(Map.of("problemId", problemId))))
                        .andReturn();
        String sessionId = node(session).path("id").asText();

        // Rungs 1-5 are freely available, one at a time.
        for (int i = 1; i <= 5; i++) {
            mvc.perform(
                            post("/api/v1/hints/sessions/" + sessionId + "/next")
                                    .header("Authorization", token)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(json(Map.of("revealSolution", false))))
                    .andExpect(status().isOk());
        }

        // The sixth is not.
        mvc.perform(
                        post("/api/v1/hints/sessions/" + sessionId + "/next")
                                .header("Authorization", token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json(Map.of("revealSolution", false))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("bad_request"));

        // Unless it is asked for explicitly.
        mvc.perform(
                        post("/api/v1/hints/sessions/" + sessionId + "/next")
                                .header("Authorization", token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json(Map.of("revealSolution", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.solutionRevealed").value(true));
    }

    @Test
    @DisplayName("one user cannot read another user's problem")
    void enforcesOwnership() throws Exception {
        MvcResult imported =
                mvc.perform(
                                post("/api/v1/problems/import")
                                        .header("Authorization", token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(json(Map.of("rawText", STATEMENT, "title", "Private Problem"))))
                        .andReturn();
        String problemId = node(imported).path("problem").path("id").asText();

        MvcResult other =
                mvc.perform(
                                post("/api/v1/auth/register")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                json(
                                                        Map.of(
                                                                "email", "intruder-" + java.util.UUID.randomUUID() + "@revexa.ai",
                                                                "displayName", "Intruder",
                                                                "password", "supersecret123"))))
                        .andReturn();
        String otherToken = "Bearer " + node(other).path("accessToken").asText();

        mvc.perform(get("/api/v1/problems/" + problemId).header("Authorization", otherToken))
                .andExpect(status().isForbidden());
    }

    private String json(Map<String, ?> body) throws Exception {
        return mapper.writeValueAsString(body);
    }

    private JsonNode node(MvcResult result) throws Exception {
        return mapper.readTree(result.getResponse().getContentAsString());
    }
}
