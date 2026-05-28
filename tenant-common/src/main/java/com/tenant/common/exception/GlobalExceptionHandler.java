package com.tenant.common.exception;

import com.tenant.common.vo.Result;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 统一拦截系统中抛出的各类异常，转换为标准格式的错误响应（{@link Result}）
 * <p>异常优先级（从具体到宽泛）：ConstraintViolationException → MethodArgumentNotValidException → IllegalArgumentException → BusinessException → RuntimeException → Exception
 * <p><b>重要</b>：@ExceptionHandler 按声明顺序匹配，必须将最具体的异常放在最前面，宽泛的异常放在最后兜底
 * <p>配合{@link BusinessException}使用，业务异常返回400，系统异常返回500
 *
 * @author Aze
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理参数约束校验异常（@RequestParam / @PathVariable 上的校验注解触发）
     * <p>当 Controller 类标注 {@code @Validated} 且方法参数使用 {@code @NotBlank}、{@code @Size} 等注解时，
     * 校验失败会抛出此异常（区别于 {@link MethodArgumentNotValidException} 的 @RequestBody 校验）
     *
     * @param e 约束校验异常
     * @return 错误响应结果
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Object> handleConstraintViolationException(ConstraintViolationException e) {
        String errors = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        logger.warn("参数约束校验失败: {}", errors);
        return Result.fail("参数校验失败: " + errors);
    }

    /**
     * 处理Hibernate Validator参数校验异常（@Valid触发的校验）
     * <p>将所有字段校验错误聚合为一条友好的错误消息
     *
     * @param e 校验异常对象
     * @return 错误响应结果，包含所有字段校验错误
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Object> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String errors = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        logger.warn("参数校验失败: {}", errors);
        return Result.fail("参数校验失败: " + errors);
    }

    /**
     * 处理参数校验异常
     *
     * @param e 参数校验异常对象
     * @return 错误响应结果
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Object> handleIllegalArgumentException(IllegalArgumentException e) {
        logger.error("参数校验异常: {}", e.getMessage());
        return Result.fail("参数错误: " + e.getMessage());
    }

    /**
     * 处理业务异常
     *
     * @param e 业务异常对象
     * @return 错误响应结果
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Object> handleBusinessException(BusinessException e) {
        logger.error("业务异常: {}", e.getMessage());
        return Result.fail(e.getMessage());
    }

    /**
     * 处理运行时异常
     *
     * @param e 运行时异常对象
     * @return 错误响应结果
     */
    @ExceptionHandler(RuntimeException.class)
    public Result<Object> handleRuntimeException(RuntimeException e) {
        logger.error("运行时异常", e);
        return Result.error(e.getMessage());
    }

    /**
     * 处理系统内部异常（最宽泛，兜底处理）
     *
     * @param e 异常对象
     * @return 错误响应结果
     */
    @ExceptionHandler(Exception.class)
    public Result<Object> handleException(Exception e) {
        logger.error("系统内部异常", e);
        return Result.error("系统内部错误，请联系管理员");
    }
}