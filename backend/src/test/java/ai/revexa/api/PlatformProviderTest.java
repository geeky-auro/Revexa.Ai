package ai.revexa.api;

import static org.assertj.core.api.Assertions.assertThat;

import ai.revexa.catalog.domain.PlatformSource;
import ai.revexa.catalog.provider.PracticePlatformProvider;
import ai.revexa.catalog.provider.PracticePlatformRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class PlatformProviderTest {

    @Autowired private PracticePlatformRegistry registry;

    @Test
    @DisplayName("a LeetCode URL routes to the link provider and yields slug and title")
    void routesLeetCodeUrls() {
        PracticePlatformProvider.ImportRequest request =
                new PracticePlatformProvider.ImportRequest(
                        "Given an array of integers nums...", "https://leetcode.com/problems/two-sum/", null, null, null);

        PracticePlatformProvider provider = registry.resolve(null, request);
        assertThat(provider.id()).isEqualTo("leetcode-link");

        PracticePlatformProvider.ImportedProblem imported = provider.importProblem(request);
        assertThat(imported.slug()).isEqualTo("two-sum");
        assertThat(imported.title()).isEqualTo("Two Sum");
        assertThat(imported.source()).isEqualTo(PlatformSource.LEETCODE);
    }

    @Test
    @DisplayName("a plain paste falls back to manual entry")
    void fallsBackToManual() {
        PracticePlatformProvider.ImportRequest request =
                new PracticePlatformProvider.ImportRequest("Some problem text", null, "My Problem", null, null);
        assertThat(registry.resolve(null, request).id()).isEqualTo("manual");
    }

    @Test
    @DisplayName("an extension payload routes to the browser-extension provider")
    void routesExtensionPayloads() {
        String payload =
                """
                {"title":"Valid Anagram","platform":"LEETCODE","difficulty":"EASY",
                 "statement":"Given two strings s and t, return true if t is an anagram of s.",
                 "constraints":"1 <= s.length <= 5 * 10^4","topics":["String","Hash Table"],
                 "url":"https://leetcode.com/problems/valid-anagram/"}
                """;
        PracticePlatformProvider.ImportRequest request =
                new PracticePlatformProvider.ImportRequest(null, null, null, null, payload);

        PracticePlatformProvider provider = registry.resolve(null, request);
        assertThat(provider.id()).isEqualTo("browser-extension");

        PracticePlatformProvider.ImportedProblem imported = provider.importProblem(request);
        assertThat(imported.title()).isEqualTo("Valid Anagram");
        assertThat(imported.topics()).contains("String", "Hash Table");
        assertThat(imported.source()).isEqualTo(PlatformSource.LEETCODE);
    }

    @Test
    @DisplayName("every provider states its real capabilities, and none claims server-side scraping")
    void capabilitiesAreHonest() {
        assertThat(registry.all()).isNotEmpty();
        registry
                .all()
                .forEach(
                        provider -> {
                            assertThat(provider.capabilities().acceptedInputs()).isNotEmpty();
                            assertThat(provider.capabilities().notes()).isNotBlank();
                            if (provider.capabilities().automaticImport()) {
                                // Anything automatic must run with the user's own authorisation.
                                assertThat(provider.capabilities().requiresUserAuthorization()).isTrue();
                            }
                        });
    }
}
