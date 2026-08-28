package ai.revexa.catalog.domain;

/** Where a problem came from. New platforms are added here and in a {@code PracticePlatformProvider}. */
public enum PlatformSource {
    MANUAL,
    LEETCODE,
    CODEFORCES,
    HACKERRANK,
    ATCODER,
    CUSTOM
}
