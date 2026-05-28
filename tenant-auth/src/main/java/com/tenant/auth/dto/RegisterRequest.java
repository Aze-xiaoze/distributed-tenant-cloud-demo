package com.tenant.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

/**
 * 注册请求DTO
 * 封装用户注册时提交的信息，用户名和密码为必填项，其他为可选项
 * <p>使用示例：{@code POST /auth/register} 接口接收此对象作为请求体
 *
 * @author Aze
 */
public class RegisterRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户名
     * <p>仅允许字母、数字、下划线，防止注入攻击
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度应在3-50之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    private String username;

    /**
     * 密码
     * <p>基础格式校验：8-128位，复杂度校验由PasswordValidator完成
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 128, message = "密码长度应在8-128之间")
    private String password;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 邮箱（可选）
     */
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 手机号（可选，中国大陆11位手机号）
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
