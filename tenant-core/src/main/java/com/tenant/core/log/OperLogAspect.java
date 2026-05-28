package com.tenant.core.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tenant.core.tenant.TenantContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 操作日志AOP切面
 * 拦截标注 {@link OperLog} 注解的Controller方法，自动记录操作日志
 * <p>记录内容：操作人、租户ID、请求URL、IP、请求参数、响应结果、执行耗时、异常信息
 * <p>日志异步写入数据库，不影响业务接口响应时间
 * <p>敏感参数过滤：自动排除password、oldPassword、newPassword等字段
 *
 * @author Aze
 */
@Aspect
@Component
public class OperLogAspect {

    private static final Logger log = LoggerFactory.getLogger(OperLogAspect.class);

    /**
     * 敏感参数名列表（这些字段的值将被替换为******）
     */
    private static final String[] SENSITIVE_PARAMS = {
            "password", "oldPassword", "newPassword", "confirmPassword",
            "secret", "token", "accessToken", "refreshToken"
    };

    /**
     * 请求参数最大长度（超过截断，防止TEXT字段溢出）
     */
    private static final int MAX_PARAM_LENGTH = 4096;

    /**
     * 响应结果最大长度
     */
    private static final int MAX_RESULT_LENGTH = 4096;

    private final OperLogMapper operLogMapper;

    private final ObjectMapper objectMapper;

    public OperLogAspect(OperLogMapper operLogMapper, ObjectMapper objectMapper) {
        this.operLogMapper = operLogMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 环绕通知：拦截@OperLog注解的方法
     */
    @Around("@annotation(operLog)")
    public Object around(ProceedingJoinPoint point, OperLog operLog) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 构建日志实体
        OperLogEntity logEntity = new OperLogEntity();
        logEntity.setTitle(operLog.title());
        logEntity.setOperationType(operLog.operationType().getCode());
        logEntity.setMethod(point.getSignature().getDeclaringTypeName() + "." + point.getSignature().getName());
        logEntity.setCreateTime(LocalDateTime.now());

        // 填充请求信息
        fillRequestInfo(logEntity);

        // 填充操作人和租户ID
        fillOperatorInfo(logEntity);

        // 记录请求参数
        if (operLog.saveRequestParams()) {
            logEntity.setRequestParams(buildRequestParams(point, operLog.excludeSensitiveParams()));
        }

        Object result = null;
        try {
            // 执行目标方法
            result = point.proceed();
            logEntity.setStatus(1); // 成功

            // 记录响应结果
            if (operLog.saveResponseResult() && result != null) {
                String resultStr = objectMapper.writeValueAsString(result);
                logEntity.setResponseResult(truncate(resultStr, MAX_RESULT_LENGTH));
            }
        } catch (Throwable e) {
            logEntity.setStatus(0); // 失败
            logEntity.setErrorMsg(truncate(e.getMessage(), MAX_RESULT_LENGTH));
            throw e;
        } finally {
            logEntity.setExecutionTime(System.currentTimeMillis() - startTime);
            // 异步写入日志（不阻塞业务线程）
            saveLogAsync(logEntity);
        }

        return result;
    }

    /**
     * 填充请求信息（URL、IP、请求方式）
     */
    private void fillRequestInfo(OperLogEntity logEntity) {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            logEntity.setUrl(request.getRequestURI());
            logEntity.setRequestMethod(request.getMethod());
            logEntity.setIp(getClientIp(request));
        }
    }

    /**
     * 填充操作人和租户ID
     * <p>优先从Security上下文获取，其次从请求头获取（网关透传）
     */
    private void fillOperatorInfo(OperLogEntity logEntity) {
        // 从Security上下文获取操作人
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            logEntity.setOperator(authentication.getName());
        }

        // 从TenantContextHolder获取租户ID
        String tenantId = TenantContextHolder.getCurrentTenantId();
        if (tenantId != null) {
            logEntity.setTenantId(tenantId);
        }

        // 如果Security上下文没有操作人，尝试从请求头获取
        if (logEntity.getOperator() == null) {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String user = request.getHeader("X-User-Id");
                if (user == null) {
                    user = request.getHeader("X-User-Name");
                }
                if (user != null) {
                    logEntity.setOperator(user);
                }
                if (logEntity.getTenantId() == null) {
                    String tid = request.getHeader("X-Tenant-ID");
                    if (tid != null) {
                        logEntity.setTenantId(tid);
                    }
                }
            }
        }
    }

    /**
     * 构建请求参数JSON字符串
     * <p>排除敏感参数值，排除不可序列化的参数类型
     */
    private String buildRequestParams(ProceedingJoinPoint point, boolean excludeSensitive) {
        try {
            MethodSignature signature = (MethodSignature) point.getSignature();
            String[] paramNames = signature.getParameterNames();
            Object[] args = point.getArgs();

            Map<String, Object> params = new HashMap<>();
            for (int i = 0; i < paramNames.length; i++) {
                Object arg = args[i];
                // 跳过不可序列化的参数类型
                if (arg instanceof HttpServletRequest || arg instanceof HttpServletResponse
                        || arg instanceof HttpSession || arg instanceof MultipartFile) {
                    continue;
                }
                if (excludeSensitive && isSensitiveParam(paramNames[i])) {
                    params.put(paramNames[i], "******");
                } else {
                    params.put(paramNames[i], arg);
                }
            }

            String json = objectMapper.writeValueAsString(params);
            return truncate(json, MAX_PARAM_LENGTH);
        } catch (Exception e) {
            log.warn("序列化请求参数失败: {}", e.getMessage());
            return "{}";
        }
    }

    /**
     * 判断参数名是否为敏感参数
     */
    private boolean isSensitiveParam(String paramName) {
        if (paramName == null) {
            return false;
        }
        String lower = paramName.toLowerCase();
        for (String sensitive : SENSITIVE_PARAMS) {
            if (lower.contains(sensitive.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取客户端真实IP（支持代理透传）
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // X-Forwarded-For可能包含多个IP，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 字符串截断
     */
    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...(truncated)";
    }

    /**
     * 异步保存日志
     * <p>使用独立线程保存，日志写入失败不影响业务流程
     */
    private void saveLogAsync(OperLogEntity logEntity) {
        try {
            operLogMapper.insert(logEntity);
        } catch (Exception e) {
            log.error("操作日志保存失败：title={}, operator={}, error={}",
                    logEntity.getTitle(), logEntity.getOperator(), e.getMessage());
        }
    }
}