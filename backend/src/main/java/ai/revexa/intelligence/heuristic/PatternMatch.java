package ai.revexa.intelligence.heuristic;

/** A pattern plus how strongly it matched, so callers can decide how confidently to speak. */
public record PatternMatch(AlgorithmPattern pattern, int score, boolean confident) {}
