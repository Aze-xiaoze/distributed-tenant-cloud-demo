package com.tenant.core.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis配置类
 * 配置RedisTemplate的序列化策略，支持对象级别的缓存读写
 * <p>序列化策略：
 * <ul>
 *   <li>Key — {@link StringRedisSerializer}：键使用String序列化，保证可读性</li>
 *   <li>Value — {@link GenericJackson2JsonRedisSerializer}：值使用JSON序列化，自动包含@class类型信息，
 *       反序列化时无需手动指定类型</li>
 * </ul>
 * <p><b>修复</b>：复用 JacksonConfig 中的 ObjectMapper，确保 Redis 缓存中的时间序列化格式（yyyy-MM-dd HH:mm:ss）
 * 与 API 响应保持一致，避免缓存与接口时间格式不统一
 *
 * @author Aze
 */
@Configuration
public class RedisConfig {

    /**
     * 配置RedisTemplate
     * 使用GenericJackson2Json作为值序列化器（自动包含类型信息），String作为键序列化器
     * <p>复用 JacksonConfig 的 ObjectMapper，确保时间格式化与 API 响应一致
     *
     * @param connectionFactory Redis连接工厂
     * @param objectMapper      已配置的 ObjectMapper（复用 JacksonConfig 中的时间格式配置）
     * @return RedisTemplate 实例
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 键使用String序列化
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);

        // 复制 ObjectMapper 并启用类型信息（RedisTemplate 也需要存储类型信息以便反序列化）
        ObjectMapper cacheObjectMapper = objectMapper.copy();
        cacheObjectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        // 值使用GenericJackson2Json序列化（复用 JacksonConfig 的 ObjectMapper，确保时间格式一致）
        GenericJackson2JsonRedisSerializer jsonRedisSerializer = new GenericJackson2JsonRedisSerializer(cacheObjectMapper);
        template.setValueSerializer(jsonRedisSerializer);
        template.setHashValueSerializer(jsonRedisSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * 配置StringRedisTemplate
     *
     * @param connectionFactory Redis连接工厂
     * @return StringRedisTemplate 实例
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}
