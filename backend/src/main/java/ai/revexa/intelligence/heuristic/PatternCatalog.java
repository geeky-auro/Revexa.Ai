package ai.revexa.intelligence.heuristic;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Loads the algorithmic knowledge base and scores it against a problem statement and the facts
 * extracted from a submission.
 *
 * <p>Keeping this as data rather than code means new patterns can be added by editing one JSON file,
 * and the same catalogue backs hints, comparisons and recommendations.
 */
@Component
public class PatternCatalog {

    private static final Logger log = LoggerFactory.getLogger(PatternCatalog.class);

    private final ObjectMapper mapper;
    private List<AlgorithmPattern> patterns = List.of();
    private AlgorithmPattern fallback;

    public PatternCatalog(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Catalog(List<AlgorithmPattern> patterns, AlgorithmPattern fallback) {}

    @PostConstruct
    void load() {
        try (InputStream in = new ClassPathResource("knowledge/patterns.json").getInputStream()) {
            Catalog catalog = mapper.readValue(in, Catalog.class);
            this.patterns = catalog.patterns() == null ? List.of() : catalog.patterns();
            this.fallback = catalog.fallback();
            log.info("Loaded {} algorithmic patterns into the knowledge base", patterns.size());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to load knowledge/patterns.json", e);
        }
    }

    public List<AlgorithmPattern> all() {
        return patterns;
    }

    public AlgorithmPattern fallback() {
        return fallback;
    }

    public Optional<AlgorithmPattern> byId(String id) {
        return patterns.stream().filter(p -> p.id().equals(id)).findFirst();
    }

    /**
     * Scores every pattern against the problem text (strong signal) and the submitted code (weaker,
     * corroborating signal), and returns them best-first.
     */
    public List<PatternMatch> rank(String problemText, List<String> topics, CodeFacts facts) {
        String haystack = (problemText == null ? "" : problemText).toLowerCase(Locale.ROOT);
        List<String> normalizedTopics =
                topics == null ? List.of() : topics.stream().map(t -> t.toLowerCase(Locale.ROOT)).toList();

        List<PatternMatch> matches = new ArrayList<>();
        for (AlgorithmPattern pattern : patterns) {
            int score = 0;
            for (String keyword : pattern.problemKeywords()) {
                if (haystack.contains(keyword.toLowerCase(Locale.ROOT))) {
                    score += keyword.contains(" ") ? 6 : 3;
                }
            }
            for (String topic : pattern.topics()) {
                if (normalizedTopics.contains(topic.toLowerCase(Locale.ROOT))) {
                    score += 5;
                }
            }
            if (facts != null) {
                for (String signal : pattern.codeSignals()) {
                    if (facts.has(signal)) {
                        score += 2;
                    }
                }
            }
            if (score > 0) {
                matches.add(new PatternMatch(pattern, score, score >= 8));
            }
        }
        matches.sort(Comparator.comparingInt(PatternMatch::score).reversed());
        return matches;
    }

    public PatternMatch best(String problemText, List<String> topics, CodeFacts facts) {
        List<PatternMatch> ranked = rank(problemText, topics, facts);
        return ranked.isEmpty() ? new PatternMatch(fallback, 0, false) : ranked.get(0);
    }
}
