package ai.revexa.core.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Every environment-tunable knob in one typed place, bound from {@code revexa.*}.
 */
@ConfigurationProperties(prefix = "revexa")
public class RevexaProperties {

    private final Security security = new Security();
    private final Ai ai = new Ai();
    private final RateLimit rateLimit = new RateLimit();
    private final Sandbox sandbox = new Sandbox();
    private final Cache cache = new Cache();
    private List<String> corsAllowedOrigins = List.of("http://localhost:3000");
    private boolean seedDemoData = true;

    public Security getSecurity() {
        return security;
    }

    public Ai getAi() {
        return ai;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public Sandbox getSandbox() {
        return sandbox;
    }

    public Cache getCache() {
        return cache;
    }

    public List<String> getCorsAllowedOrigins() {
        return corsAllowedOrigins;
    }

    public void setCorsAllowedOrigins(List<String> corsAllowedOrigins) {
        this.corsAllowedOrigins = corsAllowedOrigins;
    }

    public boolean isSeedDemoData() {
        return seedDemoData;
    }

    public void setSeedDemoData(boolean seedDemoData) {
        this.seedDemoData = seedDemoData;
    }

    public static class Security {
        /** HS256 signing secret. Must be >= 32 bytes; override in every real environment. */
        private String jwtSecret = "revexa-local-development-secret-change-me-please-0123456789";
        private Duration accessTokenTtl = Duration.ofHours(2);
        private Duration refreshTokenTtl = Duration.ofDays(30);
        private String issuer = "revexa.ai";

        public String getJwtSecret() {
            return jwtSecret;
        }

        public void setJwtSecret(String jwtSecret) {
            this.jwtSecret = jwtSecret;
        }

        public Duration getAccessTokenTtl() {
            return accessTokenTtl;
        }

        public void setAccessTokenTtl(Duration accessTokenTtl) {
            this.accessTokenTtl = accessTokenTtl;
        }

        public Duration getRefreshTokenTtl() {
            return refreshTokenTtl;
        }

        public void setRefreshTokenTtl(Duration refreshTokenTtl) {
            this.refreshTokenTtl = refreshTokenTtl;
        }

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }
    }

    public static class Ai {
        /** Active provider id: {@code heuristic}, {@code anthropic} or {@code openai}. */
        private String provider = "heuristic";
        /** Fall back to the offline heuristic engine when a remote provider errors out. */
        private boolean fallbackToHeuristic = true;
        private Duration timeout = Duration.ofSeconds(60);
        private int maxOutputTokens = 4096;
        private double temperature = 0.2;
        private final Anthropic anthropic = new Anthropic();
        private final OpenAi openai = new OpenAi();

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public boolean isFallbackToHeuristic() {
            return fallbackToHeuristic;
        }

        public void setFallbackToHeuristic(boolean fallbackToHeuristic) {
            this.fallbackToHeuristic = fallbackToHeuristic;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }

        public int getMaxOutputTokens() {
            return maxOutputTokens;
        }

        public void setMaxOutputTokens(int maxOutputTokens) {
            this.maxOutputTokens = maxOutputTokens;
        }

        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }

        public Anthropic getAnthropic() {
            return anthropic;
        }

        public OpenAi getOpenai() {
            return openai;
        }
    }

    public static class Anthropic {
        private String apiKey = "";
        private String baseUrl = "https://api.anthropic.com";
        private String model = "claude-sonnet-4-5";
        private String version = "2023-06-01";

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }
    }

    public static class OpenAi {
        private String apiKey = "";
        private String baseUrl = "https://api.openai.com";
        private String model = "gpt-4o-mini";

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }
    }

    public static class RateLimit {
        private boolean enabled = true;
        /** Requests per window for authenticated, non-AI endpoints. */
        private int defaultRequests = 240;
        /** Requests per window for the (expensive) AI endpoints. */
        private int aiRequests = 30;
        /** Requests per window for anonymous auth endpoints, keyed by client IP. */
        private int authRequests = 20;
        private Duration window = Duration.ofMinutes(1);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getDefaultRequests() {
            return defaultRequests;
        }

        public void setDefaultRequests(int defaultRequests) {
            this.defaultRequests = defaultRequests;
        }

        public int getAiRequests() {
            return aiRequests;
        }

        public void setAiRequests(int aiRequests) {
            this.aiRequests = aiRequests;
        }

        public int getAuthRequests() {
            return authRequests;
        }

        public void setAuthRequests(int authRequests) {
            this.authRequests = authRequests;
        }

        public Duration getWindow() {
            return window;
        }

        public void setWindow(Duration window) {
            this.window = window;
        }
    }

    public static class Cache {
        /** {@code memory} (Caffeine, zero infra) or {@code redis} (shared, multi-instance safe). */
        private String mode = "memory";
        private Duration ttl = Duration.ofHours(6);

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public Duration getTtl() {
            return ttl;
        }

        public void setTtl(Duration ttl) {
            this.ttl = ttl;
        }
    }

    public static class Sandbox {
        /** {@code disabled} or {@code simulated}. A real runner (Judge0/Firecracker) plugs in here. */
        private String provider = "simulated";
        private Duration timeout = Duration.ofSeconds(10);

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }
    }
}
