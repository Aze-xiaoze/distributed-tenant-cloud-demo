package com.tenant.system.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tenant.core.tenant.TenantValidator;
import com.tenant.system.entity.SysUserEntity;
import com.tenant.system.entity.TenantEntity;
import com.tenant.system.mapper.SysUserMapper;
import com.tenant.system.mapper.TenantMapper;
import com.tenant.system.service.EmailService;
import com.tenant.system.service.NotificationService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * XXL-Job 任务处理器
 * <p>提供租户过期自动处理、通知清理等分布式调度任务
 * <p>任务名在 XXL-Job 调度中心配置，此处通过 @XxlJob 注解绑定
 *
 * @author Aze
 */
@Slf4j
@Component
public class TenantXxlJobHandlerJob {

    @Autowired
    private TenantMapper tenantMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private TenantValidator tenantValidator;

    /**
     * 租户过期检查与自动处理
     * <p>扫描所有活跃租户，检查到期时间：
     * <ul>
     *   <li>到期前30/7/1天：发送预警通知（站内信 + 邮件）</li>
     *   <li>已过期：自动停用租户 + 清除缓存 + 通知租户管理员</li>
     * </ul>
     */
    @XxlJob("tenantExpiryCheckHandler")
    public void tenantExpiryCheckHandler() {
        XxlJobHelper.log("租户过期检查任务开始执行");

        try {
            LambdaQueryWrapper<TenantEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(TenantEntity::getStatus, 1);
            List<TenantEntity> tenantEntities = tenantMapper.selectList(wrapper);

            LocalDate today = LocalDate.now();
            int warningCount = 0;
            int disabledCount = 0;

            for (TenantEntity tenantEntity : tenantEntities) {
                if (tenantEntity.getExpireTime() == null) {
                    continue;
                }

                LocalDate expireDate = tenantEntity.getExpireTime().toLocalDate();
                long daysLeft = ChronoUnit.DAYS.between(today, expireDate);

                if (daysLeft == 30 || daysLeft == 7 || daysLeft == 1) {
                    sendExpiryWarning(tenantEntity, daysLeft);
                    warningCount++;
                } else if (daysLeft <= 0) {
                    disableTenant(tenantEntity);
                    disabledCount++;
                }
            }

            String result = "检查完成: 扫描%d个租户, 预警%d个, 停用%d个"
                    .formatted(tenantEntities.size(), warningCount, disabledCount);
            XxlJobHelper.handleSuccess(result);
            XxlJobHelper.log(result);

        } catch (Exception e) {
            XxlJobHelper.handleFail("任务执行异常: " + e.getMessage());
            log.error("租户过期检查任务异常", e);
        }
    }

    /**
     * 过期通知清理任务
     * <p>清理7天前已读的通知，释放数据库空间
     */
    @XxlJob("notificationCleanupHandler")
    public void notificationCleanupHandler() {
        XxlJobHelper.log("通知清理任务开始执行");

        try {
            int count = notificationService.cleanExpiredNotifications();
            String result = "清理完成: 删除%d条过期通知".formatted(count);
            XxlJobHelper.handleSuccess(result);
            XxlJobHelper.log(result);
        } catch (Exception e) {
            XxlJobHelper.handleFail("任务执行异常: " + e.getMessage());
            log.error("通知清理任务异常", e);
        }
    }

    /**
     * 租户配额检查任务
     * <p>扫描所有活跃租户，检查用户数是否超过配额
     */
    @XxlJob("tenantQuotaCheckHandler")
    public void tenantQuotaCheckHandler() {
        XxlJobHelper.log("租户配额检查任务开始执行");

        try {
            LambdaQueryWrapper<TenantEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(TenantEntity::getStatus, 1);
            List<TenantEntity> tenantEntities = tenantMapper.selectList(wrapper);

            int overQuotaCount = 0;

            for (TenantEntity tenantEntity : tenantEntities) {
                String tenantCode = tenantEntity.getTenantCode();
                if (tenantEntity.getMaxUsers() == null || tenantEntity.getMaxUsers() <= 0) {
                    continue;
                }

                // 查询当前用户数
                LambdaQueryWrapper<SysUserEntity> userWrapper = new LambdaQueryWrapper<>();
                userWrapper.eq(SysUserEntity::getTenantId, tenantCode)
                        .eq(SysUserEntity::getStatus, 1);
                long currentCount = sysUserMapper.selectCount(userWrapper);

                if (currentCount >= tenantEntity.getMaxUsers()) {
                    // 发送配额预警
                    List<Long> userIds = sysUserMapper.selectList(
                                    new LambdaQueryWrapper<SysUserEntity>()
                                            .eq(SysUserEntity::getTenantId, tenantCode)
                                            .eq(SysUserEntity::getStatus, 1))
                            .stream().map(SysUserEntity::getId).toList();

                    if (!userIds.isEmpty()) {
                        notificationService.sendTenantWarning(tenantCode,
                                "租户用户配额预警",
                                "您的租户用户数已达上限（" + currentCount + "/" + tenantEntity.getMaxUsers() + "），无法再添加新用户。请联系管理员升级配额。",
                                userIds);
                    }
                    overQuotaCount++;
                }
            }

            String result = "检查完成: 扫描%d个租户, %d个超配额".formatted(tenantEntities.size(), overQuotaCount);
            XxlJobHelper.handleSuccess(result);
            XxlJobHelper.log(result);

        } catch (Exception e) {
            XxlJobHelper.handleFail("任务执行异常: " + e.getMessage());
            log.error("租户配额检查任务异常", e);
        }
    }

    // ======================== 私有方法 ========================

    private void sendExpiryWarning(TenantEntity tenantEntity, long daysLeft) {
        String tenantCode = tenantEntity.getTenantCode();
        String level = daysLeft <= 1 ? "【最后通知】" : daysLeft <= 7 ? "【紧急】" : "";
        String message = level + "您的租户将于" + daysLeft + "天后到期，请及时续费以避免服务中断。";

        LambdaQueryWrapper<SysUserEntity> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(SysUserEntity::getTenantId, tenantCode).eq(SysUserEntity::getStatus, 1);
        List<SysUserEntity> users = sysUserMapper.selectList(userWrapper);
        List<Long> userIds = users.stream().map(SysUserEntity::getId).toList();

        if (!userIds.isEmpty()) {
            notificationService.sendTenantWarning(tenantCode, "租户到期预警（剩余" + daysLeft + "天）", message, userIds);
        }

        for (SysUserEntity user : users) {
            if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                String expireDate = tenantEntity.getExpireTime() != null
                        ? tenantEntity.getExpireTime().toLocalDate().toString() : "未知";
                emailService.sendTenantExpiryWarning(user.getEmail(), tenantCode, daysLeft, expireDate);
            }
        }
    }

    private void disableTenant(TenantEntity tenantEntity) {
        tenantEntity.setStatus(0);
        tenantMapper.updateById(tenantEntity);

        // 清除租户缓存
        tenantValidator.clearExpiredCache(tenantEntity.getTenantCode());

        String tenantCode = tenantEntity.getTenantCode();

        LambdaQueryWrapper<SysUserEntity> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(SysUserEntity::getTenantId, tenantCode);
        List<SysUserEntity> users = sysUserMapper.selectList(userWrapper);
        List<Long> userIds = users.stream().map(SysUserEntity::getId).toList();

        if (!userIds.isEmpty()) {
            notificationService.sendTenantWarning(tenantCode,
                    "租户已过期停用",
                    "您的租户已到期并被自动停用。如需恢复服务，请联系管理员续费。",
                    userIds);
        }

        log.warn("XXL-Job: 租户已到期停用 tenantId={}", tenantCode);
    }
}
