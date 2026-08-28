package ai.revexa.catalog.provider;

import ai.revexa.catalog.domain.PlatformSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * LeetCode support, done the legitimate way.
 *
 * <p>LeetCode publishes no public problem API, and scraping it would breach their terms and be
 * fragile besides. So this provider takes what the user can legitimately give us — the problem URL
 * and the text they can see on their own screen — derives the slug and canonical title from the URL,
 * and parses the pasted body. The browser-extension provider covers the same ground with one click
 * once the user installs it.
 */
@Component
public class LeetCodeLinkProvider implements PracticePlatformProvider {

    private static final Pattern SLUG =
            Pattern.compile("(?i)leetcode\\.com/problems/([a-z0-9-]+)");

    private final ProblemTextParser parser;

    public LeetCodeLinkProvider(ProblemTextParser parser) {
        this.parser = parser;
    }

    @Override
    public String id() {
        return "leetcode-link";
    }

    @Override
    public String displayName() {
        return "LeetCode (link + paste)";
    }

    @Override
    public PlatformSource source() {
        return PlatformSource.LEETCODE;
    }

    @Override
    public Capabilities capabilities() {
        return new Capabilities(
                false,
                false,
                false,
                List.of("LeetCode problem URL", "Problem text pasted from the page"),
                "LeetCode has no public problem API and scraping it is against their terms, so Revexa reads the "
                        + "slug from the URL you paste and parses the statement you copied. Install the browser "
                        + "extension to make this one click instead of two.");
    }

    @Override
    public boolean supports(ImportRequest request) {
        return SLUG.matcher(request.safeUrl()).find();
    }

    @Override
    public ImportedProblem importProblem(ImportRequest request) {
        Matcher matcher = SLUG.matcher(request.safeUrl());
        String slug = matcher.find() ? matcher.group(1) : null;
        String titleFromSlug = slug == null ? null : titleCase(slug);

        String title = request.title() != null && !request.title().isBlank() ? request.title() : titleFromSlug;
        ProblemTextParser.Parsed parsed = parser.parse(request.rawText(), title, request.difficulty());

        List<String> warnings = new ArrayList<>();
        if (request.safeText().isBlank()) {
            warnings.add(
                    "Only the URL was supplied, so the statement is empty. Paste the problem text (or use the browser "
                            + "extension) to unlock review, hints and complexity analysis.");
        }
        if (parsed.constraints().isBlank() && !request.safeText().isBlank()) {
            warnings.add("No constraints block was detected — copy it in so complexity targets can be checked against it.");
        }

        return new ImportedProblem(
                parsed.title(),
                slug == null ? parser.slugify(parsed.title()) : slug,
                parsed.statement(),
                parsed.constraints(),
                parsed.examplesJson(),
                parsed.difficulty(),
                parsed.topics(),
                PlatformSource.LEETCODE,
                slug,
                request.url(),
                warnings);
    }

    private String titleCase(String slug) {
        return Arrays.stream(slug.split("-"))
                .map(word -> word.isEmpty() ? word : Character.toUpperCase(word.charAt(0)) + word.substring(1))
                .reduce((a, b) -> a + " " + b)
                .orElse(slug)
                .replace(" Ii", " II")
                .replace(" Iii", " III")
                .replace(" Iv", " IV");
    }

    static String normalise(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
