package com.tenant.core.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Spring Cache + Redis 缓存配置
 * <p>统一缓存管理：
 * <ul>
 *   <li>默认缓存过期时间：30分钟</li>
 *   <li>Key 使用 String 序列化（可读性好）</li>
 *   <li>Value 使用 JSON 序列化（跨语言兼容）</li>
 *   <li>自动包含类型信息（反序列化时无需指定类型）</li>
 * </ul>
 * <p>使用方式：在方法上标注 {@code @Cacheable(value = "cacheName", key = "#id")}
 * <p>缓存命名规范：
 * <ul>
 *   <li>字典缓存：{@code dict:{dictType}}</li>
 *   <li>配置缓存：{@code config:{configKey}}</li>
 *   <li>用户缓存：{@code user:{username}}</li>
 * </ul>
 *
 * @author Aze
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 默认缓存过期时间
     */
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(30);

    /**
     * 配置 RedisCacheManager
     * <p>使用 GenericJackson2JsonRedisSerializer 作为值序列化器，
     * 自动在 JSON 中写入 {@code @class} 类型信息，反序列化时自动还原类型
     *
     * @param connectionFactory Redis 连接工厂
     * @param objectMapper      已配置的 ObjectMapper（复用 JacksonConfig 中的时间格式配置）
     * @return RedisCacheManager 实例
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        // 复制 ObjectMapper 并启用类型信息（缓存需要存储类型信息以便反序列化）
        ObjectMapper cacheObjectMapper = objectMapper.copy();
        cacheObjectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(cacheObjectMapper);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                // Key 序列化：String
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                // Value 序列化：JSON（带类型信息）
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(jsonSerializer))
                // 禁用缓存 null 值
                .disableCachingNullValues()
                // 默认过期时间
                .entryTtl(DEFAULT_TTL);

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .transactionAware()
                .build();
    }
}
