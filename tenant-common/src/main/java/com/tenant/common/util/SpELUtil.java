package com.tenant.common.util;

import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SpEL 表达式解析工具类
 * <p>用于将 {@link com.tenant.common.annotation.DistributedLock} 和 {@link com.tenant.common.annotation.Idempotent}
 * 注解中的 SpEL 表达式解析为实际的 Redis Key
 * <p>支持的表达式示例：
 * <ul>
 *   <li>{@code #userId} — 方法参数引用</li>
 *   <li>{@code #request.orderId} — 参数字段引用</li>
 *   <li>{@code 'order:' + #userId} — 混合字符串和参数</li>
 * </ul>
 * <p><b>安全设计</b>：使用 {@link SimpleEvaluationContext} 替代 StandardEvaluationContext，
 * 仅支持属性访问和简单运算，<b>禁止</b> T() 类型引用和任意方法调用，防止 SpEL 注入攻击
 * <p>表达式解析器缓存：同一表达式字符串只创建一次 {@link Expression} 对象，避免重复解析开销
 *
 * @author Aze
 */
public class SpELUtil {

    private static final ExpressionParser PARSER = new SpelExpressionParser();

    /**
     * Expression 对象缓存（SpEL 表达式编译开销较大，缓存可提升性能）
     */
    private static final Map<String, Expression> EXPRESSION_CACHE = new ConcurrentHashMap<>();

    /**
     * 解析 SpEL 表达式，将方法参数注入上下文后求值
     *
     * @param spelExpression SpEL 表达式字符串
     * @param method         目标方法
     * @param args           方法参数值数组
     * @return 表达式求值结果（字符串）
     */
    public static String parse(String spelExpression, Method method, Object[] args) {
        if (spelExpression == null || spelExpression.isEmpty()) {
            return spelExpression;
        }

        // 非SpEL表达式（不含#或.的纯字符串），直接返回
        if (!spelExpression.contains("#") && !spelExpression.contains(".")) {
            return spelExpression;
        }

        // 使用 SimpleEvaluationContext（安全版本：仅支持属性访问和简单运算，禁止 T() 类型引用和任意方法调用）
        SimpleEvaluationContext context = SimpleEvaluationContext.forReadOnlyDataBinding().build();

        // 将方法参数名和值注入 SpEL 上下文
        // Spring Boot 3 默认保留参数名（-parameters 编译选项），可直接通过 Method 获取
        for (int i = 0; i < method.getParameterCount(); i++) {
            String paramName = method.getParameters()[i].getName();
            context.setVariable(paramName, args[i]);
        }

        // 从缓存获取或编译 Expression
        Expression expression = EXPRESSION_CACHE.computeIfAbsent(spelExpression, PARSER::parseExpression);
        Object value = expression.getValue(context);

        return value != null ? value.toString() : "";
    }
}