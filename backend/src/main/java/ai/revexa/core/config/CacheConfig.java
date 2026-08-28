package ai.revexa.core.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

/**
 * AI calls are slow and metered, so identical (problem, code, stage) requests are cached. Swap
 * {@code revexa.cache.mode} to {@code redis} to share that cache across instances.
 */
@Configuration
public class CacheConfig {

    public static final String REVIEW_CACHE = "ai-review";
    public static final String COMPARISON_CACHE = "ai-comparison";
    public static final String RECOMMENDATION_CACHE = "recommendations";

    @Bean
    @ConditionalOnProperty(name = "revexa.cache.mode", havingValue = "redis")
    CacheManager redisCacheManager(RedisConnectionFactory factory, RevexaProperties properties) {
        RedisCacheConfiguration config =
                RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(properties.getCache().getTtl())
                        .disableCachingNullValues()
                        .serializeValuesWith(
                                RedisSerializationContext.SerializationPair.fromSerializer(
                                        new GenericJackson2JsonRedisSerializer()));
        return RedisCacheManager.builder(factory).cacheDefaults(config).build();
    }

    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    CacheManager caffeineCacheManager(RevexaProperties properties) {
        CaffeineCacheManager manager =
                new CaffeineCacheManager(REVIEW_CACHE, COMPARISON_CACHE, RECOMMENDATION_CACHE);
        Duration ttl = properties.getCache().getTtl();
        manager.setCaffeine(Caffeine.newBuilder().expireAfterWrite(ttl).maximumSize(5_000));
        return manager;
    }
}
