package com.tenant.system.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tenant.system.entity.SysUser;
import com.tenant.system.entity.Tenant;
import com.tenant.system.mapper.SysUserMapper;
import com.tenant.system.mapper.TenantMapper;
import com.tenant.system.service.EmailService;
import com.tenant.system.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 租户到期预警定时任务
 * <p>每天凌晨2点检查即将到期的租户，发送站内信和邮件预警
 * <p>预警规则：
 * <ul>
 *   <li>到期前30天：提醒续费</li>
 *   <li>到期前7天：紧急预警</li>
 *   <li>到期前1天：最后通知</li>
 *   <li>已到期：自动停用租户</li>
 * </ul>
 *
 * @author Aze
 */
@Slf4j
@Component
public class TenantExpiryWarningJob {

    @Autowired
    private TenantMapper tenantMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private EmailService emailService;

    /**
     * 每天凌晨2点执行租户到期预警检查
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void checkTenantExpiry() {
        log.info("===== 租户到期预警检查开始 =====");

        try {
            // 查询所有活跃租户
            LambdaQueryWrapper<Tenant> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Tenant::getStatus, 1); // 活跃租户
            List<Tenant> tenants = tenantMapper.selectList(wrapper);

            LocalDate today = LocalDate.now();

            for (Tenant tenant : tenants) {
                if (tenant.getExpireTime() == null) {
                    continue;
                }

                LocalDate expireDate = tenant.getExpireTime().toLocalDate();
                long daysLeft = ChronoUnit.DAYS.between(today, expireDate);

                // 到期前30天预警
                if (daysLeft == 30) {
                    sendWarning(tenant, daysLeft, "您的租户将于30天后到期，请及时续费以避免服务中断。");
                }
                // 到期前7天紧急预警
                else if (daysLeft == 7) {
                    sendWarning(tenant, daysLeft, "【紧急】您的租户将于7天后到期！请立即续费，否则服务将被暂停。");
                }
                // 到期前1天最后通知
                else if (daysLeft == 1) {
                    sendWarning(tenant, daysLeft, "【最后通知】您的租户将于明天到期！到期后服务将立即暂停。");
                }
                // 已到期，自动停用
                else if (daysLeft <= 0) {
                    disableExpiredTenant(tenant);
                }
            }

            // 清理过期通知
            notificationService.cleanExpiredNotifications();

        } catch (Exception e) {
            log.error("租户到期预警检查异常", e);
        }

        log.info("===== 租户到期预警检查完成 =====");
    }

    /**
     * 发送到期预警通知（站内信 + 邮件）
     *
     * @param tenant   租户信息
     * @param daysLeft 剩余天数
     * @param message  预警消息
     */
    private void sendWarning(Tenant tenant, long daysLeft, String message) {
        String tenantCode = tenant.getTenantCode();
        String title = "租户到期预警（剩余" + daysLeft + "天）";

        // 查询该租户下所有活跃用户
        LambdaQueryWrapper<SysUser> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(SysUser::getTenantId, tenantCode)
                .eq(SysUser::getStatus, 1);
        List<SysUser> users = sysUserMapper.selectList(userWrapper);

        List<Long> userIds = users.stream().map(SysUser::getId).toList();

        // 发送站内信
        if (!userIds.isEmpty()) {
            notificationService.sendTenantWarning(tenantCode, title, message, userIds);
        }

        // 发送邮件给有邮箱的用户
        for (SysUser user : users) {
            if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                String expireDate = tenant.getExpireTime() != null
                        ? tenant.getExpireTime().toLocalDate().toString()
                        : "未知";
                emailService.sendTenantExpiryWarning(user.getEmail(), tenantCode, daysLeft, expireDate);
            }
        }

        log.info("发送租户预警: tenantId={}, daysLeft={}, userCount={}", tenantCode, daysLeft, users.size());
    }

    /**
     * 停用已到期租户
     *
     * @param tenant 租户信息
     */
    private void disableExpiredTenant(Tenant tenant) {
        tenant.setStatus(0); // 0-禁用
        tenantMapper.updateById(tenant);

        String tenantCode = tenant.getTenantCode();

        // 通知租户管理员
        LambdaQueryWrapper<SysUser> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(SysUser::getTenantId, tenantCode)
                .eq(SysUser::getStatus, 1);
        List<SysUser> users = sysUserMapper.selectList(userWrapper);
        List<Long> userIds = users.stream().map(SysUser::getId).toList();

        if (!userIds.isEmpty()) {
            notificationService.sendTenantWarning(tenantCode,
                    "租户已过期停用",
                    "您的租户已到期并被自动停用。如需恢复服务，请联系管理员续费。",
                    userIds);
        }

        log.warn("租户已到期停用: tenantId={}, tenantCode={}", tenant.getId(), tenantCode);
    }
}
