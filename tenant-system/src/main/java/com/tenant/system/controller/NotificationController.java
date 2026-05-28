package com.tenant.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tenant.common.vo.Result;
import com.tenant.core.tenant.TenantContextHolder;
import com.tenant.system.entity.Notification;
import com.tenant.system.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 通知管理控制器
 * <p>提供站内信查询、标记已读、批量已读、未读数统计等接口
 * <p>接口前缀：{@code /notification}（经网关 StripPrefix=1 后映射为 {@code /system/notification/**}）
 *
 * @author Aze
 */
@RestController
@RequestMapping("/notification")
@Tag(name = "通知管理", description = "站内信通知查询、已读标记等接口")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    /**
     * 查询当前用户的通知列表（分页）
     *
     * @param current 当前页
     * @param size    每页大小
     * @param isRead  已读状态（null全部，0未读，1已读）
     * @return 分页结果
     */
    @GetMapping("/page")
    @Operation(summary = "查询通知列表", description = "分页查询当前用户的通知，可按已读状态筛选")
    public Result<Page<Notification>> page(
            @Parameter(description = "当前页") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Long size,
            @Parameter(description = "已读状态") @RequestParam(required = false) Integer isRead) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未获取到当前用户信息");
        }
        Page<Notification> result = notificationService.getUserNotifications(userId, current, size, isRead);
        return Result.success(result);
    }

    /**
     * 获取未读通知数量
     *
     * @return 未读数量
     */
    @GetMapping("/unread-count")
    @Operation(summary = "未读通知数量", description = "获取当前用户的未读通知数量")
    public Result<Long> unreadCount() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未获取到当前用户信息");
        }
        long count = notificationService.getUnreadCount(userId);
        return Result.success(count);
    }

    /**
     * 标记单条通知为已读
     *
     * @param notificationId 通知ID
     * @return 操作结果
     */
    @PutMapping("/{notificationId}/read")
    @Operation(summary = "标记已读", description = "标记指定通知为已读")
    public Result<String> markAsRead(
            @Parameter(description = "通知ID") @PathVariable Long notificationId) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未获取到当前用户信息");
        }
        notificationService.markAsRead(userId, notificationId);
        return Result.success("标记成功");
    }

    /**
     * 批量标记所有通知为已读
     *
     * @return 操作结果
     */
    @PutMapping("/read-all")
    @Operation(summary = "全部标记已读", description = "将当前用户的所有未读通知标记为已读")
    public Result<String> markAllAsRead() {
        Long userId = getCurrentUserId();
        String tenantId = TenantContextHolder.getCurrentTenantId();
        if (userId == null) {
            return Result.error(401, "未获取到当前用户信息");
        }
        int count = notificationService.markAllAsRead(userId, tenantId);
        return Result.success("已标记 " + count + " 条通知为已读");
    }

    /**
     * 删除通知
     *
     * @param notificationId 通知ID
     * @return 操作结果
     */
    @DeleteMapping("/{notificationId}")
    @Operation(summary = "删除通知", description = "删除指定通知")
    public Result<String> delete(
            @Parameter(description = "通知ID") @PathVariable Long notificationId) {
        notificationService.deleteNotification(notificationId);
        return Result.success("删除成功");
    }

    /**
     * 获取当前登录用户ID（从 SecurityContext 中提取）
     *
     * @return 用户ID，未认证返回null
     */
    private Long getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() != null) {
                Object principal = authentication.getPrincipal();
                // 假设 principal 是用户ID（Long类型），或从details中获取
                if (principal instanceof Long) {
                    return (Long) principal;
                }
                if (principal instanceof Integer) {
                    return ((Integer) principal).longValue();
                }
                // 尝试从name解析（JWT中可能存储的是username，需要额外查询）
                // 这里使用简化的方式，实际项目中应从JWT Claims中获取userId
                String name = authentication.getName();
                if (name != null && name.matches("\\d+")) {
                    return Long.parseLong(name);
                }
            }
        } catch (Exception e) {
            // 忽略解析异常
        }
        return null;
    }
}
