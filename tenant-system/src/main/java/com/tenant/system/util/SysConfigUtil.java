package com.tenant.system.util;

import com.tenant.system.service.SysConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 * 系统配置缓存工具类
 * <p>提供带缓存的功能查询系统配置值，避免每次请求都走数据库
 * <p>缓存策略：
 * <ul>
 *   <li>缓存名称：{@code config}</li>
 *   <li>缓存键：配置键名（{@code config:{configKey}}）</li>
 *   <li>过期时间：跟随 CacheConfig 默认 30 分钟</li>
 *   <li>空值不缓存（{@code disableCachingNullValues}）</li>
 * </ul>
 * <p>使用示例：
 * <pre>
 *     String value = sysConfigUtil.getConfigValue("sys.default.password");
 * </pre>
 *
 * @author Aze
 */
@Slf4j
@Component
public class SysConfigUtil {

    private static final String CACHE_NAME = "config";

    private final SysConfigService sysConfigService;

    public SysConfigUtil(SysConfigService sysConfigService) {
        this.sysConfigService = sysConfigService;
    }

    /**
     * 根据配置键获取配置值（带缓存）
     * <p>首次查询后缓存结果，后续直接从 Redis 读取
     *
     * @param configKey 配置键
     * @return 配置值，不存在返回 null
     */
    @Cacheable(value = CACHE_NAME, key = "#configKey", unless = "#result == null")
    public String getConfigValue(String configKey) {
        log.debug("缓存未命中，从数据库查询配置: key={}", configKey);
        return sysConfigService.getConfigValue(configKey);
    }

    /**
     * 根据配置键获取配置值，不存在时返回默认值（带缓存）
     *
     * @param configKey    配置键
     * @param defaultValue 默认值
     * @return 配置值或默认值
     */
    @Cacheable(value = CACHE_NAME, key = "#configKey", unless = "#result == null")
    public String getConfigValueOrDefault(String configKey, String defaultValue) {
        log.debug("缓存未命中，从数据库查询配置: key={}", configKey);
        String value = sysConfigService.getConfigValue(configKey);
        return value != null ? value : defaultValue;
    }

    /**
     * 刷新指定配置的缓存
     * <p>配置更新后调用此方法，确保下次查询获取最新值
     *
     * @param configKey 配置键
     */
    @CacheEvict(value = CACHE_NAME, key = "#configKey")
    public void refreshConfig(String configKey) {
        log.info("刷新配置缓存: key={}", configKey);
    }

    /**
     * 清除所有配置缓存
     */
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void refreshAllConfigs() {
        log.info("刷新所有配置缓存");
    }
}