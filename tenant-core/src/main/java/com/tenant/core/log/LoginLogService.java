package com.tenant.core.log;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 登录日志服务
 * 记录登录成功/失败日志，并提供登录异常检测能力
 * <p>异常检测机制：
 * <ul>
 *   <li>同一用户连续登录失败5次，锁定账户15分钟</li>
 *   <li>同一IP连续登录失败10次，封禁IP 30分钟</li>
 * </ul>
 * <p>Redis Key结构：
 * <ul>
 *   <li>用户失败计数：{@code login:fail:user:{username}} → 次数，TTL=15分钟</li>
 *   <li>IP失败计数：{@code login:fail:ip:{ip}} → 次数，TTL=30分钟</li>
 *   <li>用户锁定标记：{@code login:lock:user:{username}} → "1"，TTL=15分钟</li>
 *   <li>IP封禁标记：{@code login:lock:ip:{ip}} → "1"，TTL=30分钟</li>
 * </ul>
 *
 * @author Aze
 */
@Component
public class LoginLogService {

    private static final Logger log = LoggerFactory.getLogger(LoginLogService.class);

    private static final String USER_FAIL_COUNT_KEY = "login:fail:user:";
    private static final String IP_FAIL_COUNT_KEY = "login:fail:ip:";
    private static final String USER_LOCK_KEY = "login:lock:user:";
    private static final String IP_LOCK_KEY = "login:lock:ip:";

    /** 用户连续登录失败上限 */
    private static final int USER_FAIL_THRESHOLD = 5;

    /** IP连续登录失败上限 */
    private static final int IP_FAIL_THRESHOLD = 10;

    /** 用户锁定时间（秒） */
    private static final long USER_LOCK_SECONDS = 900; // 15分钟

    /** IP封禁时间（秒） */
    private static final long IP_LOCK_SECONDS = 1800; // 30分钟

    private final LoginLogMapper loginLogMapper;

    private final StringRedisTemplate redisTemplate;

    public LoginLogService(LoginLogMapper loginLogMapper, StringRedisTemplate redisTemplate) {
        this.loginLogMapper = loginLogMapper;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 记录登录成功日志
     *
     * @param username 用户名
     * @param tenantId 租户ID
     * @param message  登录消息
     */
    public void logLoginSuccess(String username, String tenantId, String message) {
        // 登录成功时清除失败计数
        clearFailCount(username);

        LoginLogEntity entity = new LoginLogEntity();
        entity.setUsername(username);
        entity.setTenantId(tenantId);
        entity.setLoginStatus(1);
        entity.setLoginMessage(message);
        entity.setLoginTime(LocalDateTime.now());
        fillRequestInfo(entity);

        saveLog(entity);
    }

    /**
     * 记录登录失败日志
     * <p>同时更新失败计数，达到阈值时触发锁定
     *
     * @param username 用户名
     * @param tenantId 租户ID
     * @param message  失败原因
     */
    public void logLoginFailure(String username, String tenantId, String message) {
        LoginLogEntity entity = new LoginLogEntity();
        entity.setUsername(username);
        entity.setTenantId(tenantId);
        entity.setLoginStatus(0);
        entity.setLoginMessage(message);
        entity.setLoginTime(LocalDateTime.now());
        fillRequestInfo(entity);

        saveLog(entity);

        // 更新失败计数
        incrementUserFailCount(username);
        String ip = getClientIp();
        if (ip != null) {
            incrementIpFailCount(ip);
        }
    }

    /**
     * 检查用户是否被锁定（连续登录失败过多）
     *
     * @param username 用户名
     * @return true=已锁定，false=未锁定
     */
    public boolean isUserLocked(String username) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(USER_LOCK_KEY + username));
    }

    /**
     * 检查IP是否被封禁
     *
     * @param ip IP地址
     * @return true=已封禁，false=未封禁
     */
    public boolean isIpBanned(String ip) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(IP_LOCK_KEY + ip));
    }

    /**
     * 获取用户连续登录失败次数
     */
    public int getUserFailCount(String username) {
        String count = redisTemplate.opsForValue().get(USER_FAIL_COUNT_KEY + username);
        return count != null ? Integer.parseInt(count) : 0;
    }

    /**
     * 解锁用户（管理员手动解锁）
     */
    public void unlockUser(String username) {
        redisTemplate.delete(USER_FAIL_COUNT_KEY + username);
        redisTemplate.delete(USER_LOCK_KEY + username);
        log.info("用户已解锁：{}", username);
    }

    // ===== 私有方法 =====

    private void fillRequestInfo(LoginLogEntity entity) {
        String ip = getClientIp();
        if (ip != null) {
            entity.setIp(ip);
        }

        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String userAgent = request.getHeader("User-Agent");
            if (userAgent != null) {
                entity.setBrowser(parseBrowser(userAgent));
                entity.setOs(parseOs(userAgent));
            }
        }
    }

    private void incrementUserFailCount(String username) {
        String key = USER_FAIL_COUNT_KEY + username;
        String countStr = redisTemplate.opsForValue().get(key);
        int count = countStr != null ? Integer.parseInt(countStr) + 1 : 1;

        redisTemplate.opsForValue().set(key, String.valueOf(count), USER_LOCK_SECONDS, TimeUnit.SECONDS);

        if (count >= USER_FAIL_THRESHOLD) {
            redisTemplate.opsForValue().set(USER_LOCK_KEY + username, "1", USER_LOCK_SECONDS, TimeUnit.SECONDS);
            log.warn("用户登录失败次数超过阈值，已锁定：username={}, count={}", username, count);
        }
    }

    private void incrementIpFailCount(String ip) {
        String key = IP_FAIL_COUNT_KEY + ip;
        String countStr = redisTemplate.opsForValue().get(key);
        int count = countStr != null ? Integer.parseInt(countStr) + 1 : 1;

        redisTemplate.opsForValue().set(key, String.valueOf(count), IP_LOCK_SECONDS, TimeUnit.SECONDS);

        if (count >= IP_FAIL_THRESHOLD) {
            redisTemplate.opsForValue().set(IP_LOCK_KEY + ip, "1", IP_LOCK_SECONDS, TimeUnit.SECONDS);
            log.warn("IP登录失败次数超过阈值，已封禁：ip={}, count={}", ip, count);
        }
    }

    private void clearFailCount(String username) {
        redisTemplate.delete(USER_FAIL_COUNT_KEY + username);
        // 不清除用户锁定标记（锁定期间即使成功登录也不应解除，需等待锁定过期）
    }

    private String getClientIp() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 简单解析浏览器类型
     */
    private String parseBrowser(String userAgent) {
        if (userAgent.contains("Chrome") && !userAgent.contains("Edg")) {
            return "Chrome";
        } else if (userAgent.contains("Firefox")) {
            return "Firefox";
        } else if (userAgent.contains("Safari") && !userAgent.contains("Chrome")) {
            return "Safari";
        } else if (userAgent.contains("Edg")) {
            return "Edge";
        } else if (userAgent.contains("MSIE") || userAgent.contains("Trident")) {
            return "IE";
        }
        return "Unknown";
    }

    /**
     * 简单解析操作系统
     */
    private String parseOs(String userAgent) {
        if (userAgent.contains("Windows")) {
            return "Windows";
        } else if (userAgent.contains("Mac OS")) {
            return "Mac";
        } else if (userAgent.contains("Linux")) {
            return "Linux";
        } else if (userAgent.contains("Android")) {
            return "Android";
        } else if (userAgent.contains("iPhone") || userAgent.contains("iPad")) {
            return "iOS";
        }
        return "Unknown";
    }

    private void saveLog(LoginLogEntity entity) {
        try {
            loginLogMapper.insert(entity);
        } catch (Exception e) {
            log.error("登录日志保存失败：username={}, error={}", entity.getUsername(), e.getMessage());
        }
    }
}