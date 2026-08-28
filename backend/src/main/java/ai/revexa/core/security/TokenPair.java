package ai.revexa.core.security;

public record TokenPair(String accessToken, String refreshToken, long expiresInSeconds) {}
