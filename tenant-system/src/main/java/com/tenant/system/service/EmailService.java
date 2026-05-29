package com.tenant.system.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

/**
 * 邮件通知服务
 * <p>封装 Spring Mail，提供简单邮件和 HTML 邮件发送能力
 * <p>异步发送，不阻塞业务线程
 * <p>邮件配置通过 {@code spring.mail.*} 属性控制，未配置时自动降级为日志输出
 *
 * @author Aze
 */
@Slf4j
@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    /**
     * 是否启用邮件发送（有 mailSender 且配置了发件人地址时启用）
     */
    private boolean isMailEnabled() {
        return mailSender == null || fromEmail == null || fromEmail.isEmpty();
    }

    /**
     * 发送简单文本邮件（异步）
     *
     * @param to      收件人
     * @param subject 主题
     * @param content 内容
     */
    @Async
    public void sendSimpleEmail(String to, String subject, String content) {
        if (isMailEnabled()) {
            log.info("邮件未配置，降级为日志: to={}, subject={}", to, subject);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);
            mailSender.send(message);
            log.info("邮件发送成功: to={}, subject={}", to, subject);
        } catch (Exception e) {
            log.error("邮件发送失败: to={}, subject={}, error={}", to, subject, e.getMessage());
        }
    }

    /**
     * 发送 HTML 邮件（异步）
     *
     * @param to      收件人
     * @param subject 主题
     * @param html    HTML内容
     */
    @Async
    public void sendHtmlEmail(String to, String subject, String html) {
        if (isMailEnabled()) {
            log.info("邮件未配置，降级为日志: to={}, subject={}", to, subject);
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(mimeMessage);
            log.info("HTML邮件发送成功: to={}, subject={}", to, subject);
        } catch (Exception e) {
            log.error("HTML邮件发送失败: to={}, subject={}, error={}", to, subject, e.getMessage());
        }
    }

    /**
     * 发送租户到期预警邮件
     *
     * @param toEmail  收件人邮箱
     * @param tenantId 租户ID
     * @param daysLeft 剩余天数
     * @param expireDate 到期日期
     */
    @Async
    public void sendTenantExpiryWarning(String toEmail, String tenantId, long daysLeft, String expireDate) {
        String subject = "【租户预警】您的租户即将到期";
        String html = """
            <div style="padding: 20px; font-family: 'Microsoft YaHei', sans-serif;">
                <h2 style="color: #e6a23c;">租户到期预警通知</h2>
                <p>尊敬的用户：</p>
                <p>您的租户 <strong>%s</strong> 将于 <strong>%s</strong> 到期，剩余 <strong style="color: red;">%d</strong> 天。</p>
                <p>为避免服务中断，请及时联系管理员续费。</p>
                <hr style="margin: 20px 0; border: none; border-top: 1px solid #eee;">
                <p style="color: #999; font-size: 12px;">此邮件由系统自动发送，请勿直接回复。</p>
            </div>
            """.formatted(tenantId, expireDate, daysLeft);

        sendHtmlEmail(toEmail, subject, html);
    }
}
