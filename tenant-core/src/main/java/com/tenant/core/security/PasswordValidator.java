package com.tenant.core.security;

import lombok.Getter;

import java.util.regex.Pattern;

/**
 * 密码强度校验工具
 * 提供密码复杂度校验功能，确保用户密码满足安全策略要求
 * <p>密码策略（符合等保2.0要求）：
 * <ul>
 *   <li>最小长度8位</li>
 *   <li>必须包含大写字母</li>
 *   <li>必须包含小写字母</li>
 *   <li>必须包含数字</li>
 *   <li>必须包含特殊字符（!@#$%^&*等）</li>
 *   <li>禁止包含用户名（不区分大小写）</li>
 *   <li>禁止常见弱密码（123456、password等）</li>
 * </ul>
 *
 * @author Aze
 */
public class PasswordValidator {

    /**
     * 最小密码长度
     */
    private static final int MIN_LENGTH = 8;

    /**
     * 最大密码长度（防止DoS攻击，BCrypt有1024字节限制）
     */
    private static final int MAX_LENGTH = 128;

    /**
     * 大写字母正则
     */
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile(".*[A-Z].*");

    /**
     * 小写字母正则
     */
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile(".*[a-z].*");

    /**
     * 数字正则
     */
    private static final Pattern DIGIT_PATTERN = Pattern.compile(".*\\d.*");

    /**
     * 特殊字符正则
     */
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?`~].*");

    /**
     * 常见弱密码列表
     */
    private static final String[] WEAK_PASSWORDS = {
            "123456", "password", "12345678", "qwerty", "123456789",
            "1234567890", "abc123", "111111", "123123", "admin",
            "letmein", "welcome", "monkey", "password1", "qwerty123",
            "admin123", "root123", "123456789a", "Passw0rd", "p@ssw0rd"
    };

    /**
     * 校验密码强度，返回校验结果
     * <p>不抛异常，通过返回对象让调用方决定如何处理
     *
     * @param password 密码明文
     * @param username 用户名（用于检查密码是否包含用户名）
     * @return 校验结果
     */
    public static PasswordValidationResult validate(String password, String username) {
        PasswordValidationResult result = new PasswordValidationResult();

        if (password == null || password.isEmpty()) {
            result.addError("密码不能为空");
            return result;
        }

        if (password.length() < MIN_LENGTH) {
            result.addError("密码长度不能少于" + MIN_LENGTH + "位");
        }

        if (password.length() > MAX_LENGTH) {
            result.addError("密码长度不能超过" + MAX_LENGTH + "位");
        }

        if (!UPPERCASE_PATTERN.matcher(password).matches()) {
            result.addError("密码必须包含至少一个大写字母");
        }

        if (!LOWERCASE_PATTERN.matcher(password).matches()) {
            result.addError("密码必须包含至少一个小写字母");
        }

        if (!DIGIT_PATTERN.matcher(password).matches()) {
            result.addError("密码必须包含至少一个数字");
        }

        if (!SPECIAL_CHAR_PATTERN.matcher(password).matches()) {
            result.addError("密码必须包含至少一个特殊字符（如!@#$%^&*等）");
        }

        // 检查密码是否包含用户名
        if (username != null && !username.isEmpty()
                && password.toLowerCase().contains(username.toLowerCase())) {
            result.addError("密码不能包含用户名");
        }

        // 检查常见弱密码
        String lowerPassword = password.toLowerCase();
        for (String weak : WEAK_PASSWORDS) {
            if (lowerPassword.equals(weak) || lowerPassword.contains(weak)) {
                result.addError("密码过于简单，不能使用常见弱密码");
                break;
            }
        }

        return result;
    }

    /**
     * 密码校验结果
     */
    public static class PasswordValidationResult {

        @Getter
        private boolean valid = true;

        private final StringBuilder errors = new StringBuilder();

        public void addError(String error) {
            this.valid = false;
            if (!errors.isEmpty()) {
                errors.append("; ");
            }
            errors.append(error);
        }

        public String getErrors() {
            return errors.toString();
        }
    }
}