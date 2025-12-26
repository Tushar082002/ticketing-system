package org.example.ticketingapplication.Config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * RedisConfig - Enterprise-grade Redis Configuration
 *
 * Configures Redis connection, caching, and serialization for the application.
 *
 * Features:
 * - Redis with Lettuce driver (non-blocking, high performance)
 * - Connection pooling with configurable pool size
 * - JSON serialization for values (GenericJackson2JsonRedisSerializer)
 * - String key serialization
 * - Auto-reconnect on connection loss
 * - Read-through cache pattern support
 *
 * Configuration:
 * - Host: localhost (default) or ${REDIS_HOST} env variable
 * - Port: 6379 (default) or ${REDIS_PORT} env variable
 * - Password: Empty (default) or ${REDIS_PASSWORD} env variable
 * - Pool size: max-active=8, max-idle=8, min-idle=2 (in application.properties)
 * - Timeout: 60000ms
 *
 * @author Enterprise Backend Team
 * @version 1.0
 */
@Configuration
@EnableCaching
public class RedisConfig {

    /**
     * Redis host from environment variable or default to localhost
     */
//    @Value("${spring.data.redis.host:localhost}")
//    private String redisHost;
//
//    /**
//     * Redis port from environment variable or default to 6379
//     */
//    @Value("${spring.data.redis.port:6379}")
//    private int redisPort;
//
//    /**
//     * Redis password from environment variable (empty by default)
//     */
//    @Value("${spring.data.redis.password:}")
//    private String redisPassword;
//
//    /**
//     * Redis connection timeout in milliseconds
//     */
//    @Value("${spring.data.redis.timeout:60000}")
//    private long timeout;

    /**
     * Configure Redis Standalone Connection Factory with Lettuce.
     *
     * Features:
     * - Connection pooling using Lettuce
     * - Auto-reconnect on connection loss
     * - Command timeout configuration
     * - Client resources pooling
     * - Pool settings from application.properties
     *
     * @param clientResources Lettuce client resources bean
     * @return RedisConnectionFactory configured for production use
     */
//    @Bean
//    public RedisConnectionFactory redisConnectionFactory(ClientResources clientResources) {
//        // Configure Redis standalone connection
//        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
//        redisConfig.setHostName(redisHost);
//        redisConfig.setPort(redisPort);
//
//        // Add password if provided
//        if (redisPassword != null && !redisPassword.isEmpty()) {
//            redisConfig.setPassword(redisPassword);
//        }
//
//        // Configure Lettuce client options
//        ClientOptions clientOptions = ClientOptions.builder()
//                .autoReconnect(true) // Auto-reconnect on connection loss
//                .build();
//
//        // Configure Lettuce connection pooling
//        LettucePoolingClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
//                .commandTimeout(Duration.ofMillis(timeout))
//                .clientResources(clientResources)
//                .clientOptions(clientOptions)
//                .build();
//
//        return new LettuceConnectionFactory(redisConfig, clientConfig);
//    }

    /**
     * Configure Lettuce Client Resources.
     *
     * Provides thread pooling and resource management for Lettuce.
     * Thread-safe and can be shared across multiple connections.
     *
     * @return ClientResources for Lettuce connections
     */
    @Bean
    public ClientResources clientResources() {
        return DefaultClientResources.create();
    }

    /**
     * Configure JSON serializer for Redis values.
     *
     * Uses GenericJackson2JsonRedisSerializer for flexible JSON serialization.
     * Supports polymorphic types and complex objects.
     *
     * @return RedisSerializer for JSON serialization
     */
    @Bean
    public RedisSerializer<Object> jsonRedisSerializer() {
        return RedisSerializer.json();
    }

    /**
     * Configure RedisTemplate<String, Object>.
     *
     * Key Features:
     * - String key serializer (readable cache keys)
     * - JSON value serializer (stores complex objects)
     * - Connection pooling via Lettuce
     * - Thread-safe operations
     *
     * Cache Key Format: "ticket:{id}"
     * Serialization: JSON for values, UTF-8 String for keys
     *
     * Usage in Service Layer:
     * - Get: redisTemplate.opsForValue().get(key)
     * - Set: redisTemplate.opsForValue().set(key, value, TTL, TimeUnit)
     * - Delete: redisTemplate.delete(key)
     * - Keys: redisTemplate.keys(pattern)
     *
     * @param connectionFactory the Redis connection factory
     * @param jsonRedisSerializer the JSON serializer for values
     * @return RedisTemplate configured for production use
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory,
                                                       RedisSerializer<Object> jsonRedisSerializer) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Set String serializer for keys (readable cache keys)
        template.setKeySerializer(StringRedisSerializer.UTF_8);
        template.setHashKeySerializer(StringRedisSerializer.UTF_8);

        // Set JSON serializer for values (complex object storage)
        template.setValueSerializer(jsonRedisSerializer);
        template.setHashValueSerializer(jsonRedisSerializer);

        // Initialize the template
        template.afterPropertiesSet();

        return template;
    }
}

