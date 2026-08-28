package ai.revexa.catalog.provider;

import ai.revexa.catalog.domain.PlatformSource;
import java.util.List;
import org.springframework.stereotype.Component;

/** The always-available path: the user pastes a statement and we parse what we can out of it. */
@Component
public class ManualEntryProvider implements PracticePlatformProvider {

    private final ProblemTextParser parser;

    public ManualEntryProvider(ProblemTextParser parser) {
        this.parser = parser;
    }

    @Override
    public String id() {
        return "manual";
    }

    @Override
    public String displayName() {
        return "Manual entry";
    }

    @Override
    public PlatformSource source() {
        return PlatformSource.MANUAL;
    }

    @Override
    public Capabilities capabilities() {
        return new Capabilities(
                false,
                false,
                false,
                List.of("Pasted problem statement", "Optional title and difficulty"),
                "Works with any platform, any language, and nothing to authorise. The parser recovers the "
                        + "constraints block, worked examples and likely topics from the text you paste.");
    }

    @Override
    public boolean supports(ImportRequest request) {
        return !request.safeText().isBlank();
    }

    @Override
    public ImportedProblem importProblem(ImportRequest request) {
        ProblemTextParser.Parsed parsed = parser.parse(request.rawText(), request.title(), request.difficulty());
        List<String> warnings =
                parsed.constraints().isBlank()
                        ? List.of("No constraints block was found — complexity targets will be inferred from the problem shape instead.")
                        : List.of();
        return new ImportedProblem(
                parsed.title(),
                parser.slugify(parsed.title()),
                parsed.statement(),
                parsed.constraints(),
                parsed.examplesJson(),
                parsed.difficulty(),
                parsed.topics(),
                PlatformSource.MANUAL,
                null,
                request.url(),
                warnings);
    }
}
