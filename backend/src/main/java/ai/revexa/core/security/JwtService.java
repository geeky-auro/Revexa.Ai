package ai.revexa.core.security;

import ai.revexa.core.config.RevexaProperties;
import ai.revexa.core.error.ApiException;
import ai.revexa.identity.domain.UserAccount;
import ai.revexa.identity.domain.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/** Issues and verifies the stateless HS256 tokens used by every client surface. */
@Service
public class JwtService {

    private static final String CLAIM_TYPE = "typ";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final RevexaProperties.Security config;
    private final SecretKey key;

    public JwtService(RevexaProperties properties) {
        this.config = properties.getSecurity();
        byte[] secret = config.getJwtSecret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException(
                    "revexa.security.jwt-secret must be at least 32 bytes for HS256");
        }
        this.key = Keys.hmacShaKeyFor(secret);
    }

    public TokenPair issue(UserAccount user) {
        String access =
                build(
                        user.getId(),
                        TYPE_ACCESS,
                        config.getAccessTokenTtl(),
                        Map.of(
                                "email", user.getEmail(),
                                "name", user.getDisplayName(),
                                "role", user.getRole().name()));
        String refresh = build(user.getId(), TYPE_REFRESH, config.getRefreshTokenTtl(), Map.of());
        return new TokenPair(access, refresh, config.getAccessTokenTtl().toSeconds());
    }

    public AuthenticatedUser verifyAccessToken(String token) {
        Claims claims = parse(token);
        if (!TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw ApiException.unauthorized("Not an access token");
        }
        return new AuthenticatedUser(
                UUID.fromString(claims.getSubject()),
                claims.get("email", String.class),
                claims.get("name", String.class),
                UserRole.valueOf(claims.getOrDefault("role", "USER").toString()));
    }

    public UUID verifyRefreshToken(String token) {
        Claims claims = parse(token);
        if (!TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw ApiException.unauthorized("Not a refresh token");
        }
        return UUID.fromString(claims.getSubject());
    }

    private String build(UUID subject, String type, Duration ttl, Map<String, Object> claims) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(subject.toString())
                .issuer(config.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .claims(claims)
                .claim(CLAIM_TYPE, type)
                .signWith(key)
                .compact();
    }

    private Claims parse(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(config.getIssuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw ApiException.unauthorized("Token is invalid or has expired");
        }
    }
}
