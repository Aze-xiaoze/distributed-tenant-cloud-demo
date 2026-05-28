package com.tenant.core.log;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志实体
 * 对应数据库表 sys_oper_log
 * <p>通过 {@link com.tenant.core.log.OperLog} 注解 + AOP切面自动记录
 *
 * @author Aze
 */
@Data
@TableName("sys_oper_log")
public class OperLogEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作模块 */
    private String title;

    /** 操作类型（1-新增，2-修改，3-删除，4-查询，5-导出，6-导入） */
    private Integer operationType;

    /** 方法名称 */
    private String method;

    /** 请求方式 */
    private String requestMethod;

    /** 操作人 */
    private String operator;

    /** 租户ID */
    private String tenantId;

    /** 请求URL */
    private String url;

    /** 操作IP */
    private String ip;

    /** 请求参数 */
    private String requestParams;

    /** 返回结果 */
    private String responseResult;

    /** 操作状态（1-成功，0-失败） */
    private Integer status;

    /** 错误消息 */
    private String errorMsg;

    /** 执行耗时（毫秒） */
    private Long executionTime;

    /** 创建时间 */
    private LocalDateTime createTime;
}