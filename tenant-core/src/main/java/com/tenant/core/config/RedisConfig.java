package com.tenant.core.config;

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
 * <p><b>兼容性说明</b>：Spring Data Redis 3.x移除了 {@code Jackson2JsonRedisSerializer.setObjectMapper()} 方法，
 * 必须使用 {@link GenericJackson2JsonRedisSerializer} 替代
 *
 * @author Aze
 */
@Configuration
public class RedisConfig {

    /**
     * 配置RedisTemplate
     * 使用GenericJackson2Json作为值序列化器（自动包含类型信息），String作为键序列化器
     *
     * @param connectionFactory Redis连接工厂
     * @return RedisTemplate 实例
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 键使用String序列化
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);

        // 值使用GenericJackson2Json序列化（自动包含@class类型信息，无需手动配置ObjectMapper）
        GenericJackson2JsonRedisSerializer jsonRedisSerializer = new GenericJackson2JsonRedisSerializer();
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
