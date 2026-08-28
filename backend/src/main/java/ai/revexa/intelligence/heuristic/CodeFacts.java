package ai.revexa.intelligence.heuristic;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Everything the static analyser could establish about a submission. This is the single input to
 * complexity estimation, finding detection and pattern matching.
 */
public record CodeFacts(
        String language,
        int lines,
        int maxLoopDepth,
        int loopCount,
        boolean recursive,
        int selfCallSites,
        boolean memoized,
        boolean sorts,
        boolean binarySearch,
        boolean nestedScanOverSameCollection,
        Set<String> structures,
        Set<String> signals,
        List<String> functionNames,
        Map<String, Integer> signalLines,
        boolean guardsEmptyInput,
        boolean hasMidpointOverflowRisk,
        boolean hasBaseCase,
        boolean allocatesAuxiliaryArray,
        boolean allocatesMatrix,
        boolean buildsStringByConcatInLoop,
        String cleanedCode) {

    public boolean has(String signal) {
        return signals.contains(signal) || structures.contains(signal);
    }
}
