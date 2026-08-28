package ai.revexa.core.ratelimit;

import ai.revexa.core.config.RevexaProperties;
import ai.revexa.core.error.ApiError;
import ai.revexa.core.security.CurrentUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Protects the API with three tiers: anonymous auth traffic (per IP), AI endpoints (expensive, per
 * user) and everything else. Runs after authentication so it can key on the user when there is one.
 */
@Component
@Order(20)
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiter limiter;
    private final RevexaProperties.RateLimit config;
    private final ObjectMapper mapper;

    public RateLimitFilter(RateLimiter limiter, RevexaProperties properties, ObjectMapper mapper) {
        this.limiter = limiter;
        this.config = properties.getRateLimit();
        this.mapper = mapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !config.isEnabled()
                || path.startsWith("/actuator")
                || path.startsWith("/swagger")
                || path.startsWith("/v3/api-docs")
                || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain)
            throws ServletException, IOException {
        Tier tier = tierFor(request);
        String identity = CurrentUser.find().map(u -> "u:" + u.id()).orElseGet(() -> "ip:" + clientIp(request));
        Duration window = config.getWindow();

        RateLimiter.Decision decision = limiter.check(tier.name() + ":" + identity, tier.limit(config), window);
        response.setHeader("X-RateLimit-Limit", String.valueOf(decision.limit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(decision.remaining()));

        if (!decision.allowed()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(decision.retryAfterSeconds()));
            mapper.writeValue(
                    response.getOutputStream(),
                    ApiError.of(
                            "rate_limited",
                            "Too many requests — try again in " + decision.retryAfterSeconds() + "s",
                            request.getRequestURI()));
            return;
        }
        chain.doFilter(request, response);
    }

    private Tier tierFor(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path.contains("/auth/")) {
            return Tier.AUTH;
        }
        if (path.contains("/reviews")
                || path.contains("/hints")
                || path.contains("/chat")
                || path.contains("/comparisons")
                || path.contains("/execution")) {
            return Tier.AI;
        }
        return Tier.DEFAULT;
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private enum Tier {
        AUTH,
        AI,
        DEFAULT;

        int limit(RevexaProperties.RateLimit config) {
            return switch (this) {
                case AUTH -> config.getAuthRequests();
                case AI -> config.getAiRequests();
                case DEFAULT -> config.getDefaultRequests();
            };
        }
    }
}
