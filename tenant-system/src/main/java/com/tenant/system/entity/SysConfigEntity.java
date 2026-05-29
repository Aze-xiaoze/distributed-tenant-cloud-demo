package com.tenant.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统配置实体类
 * 映射数据库 sys_config 表，用于系统参数配置管理
 * <p>共享表，在 {@link com.tenant.core.config.MybatisPlusConfig} 中被标记为忽略租户过滤
 *
 * @author Aze
 */
@Data
@TableName("sys_config")
public class SysConfigEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 配置键
     */
    @NotBlank(message = "配置键不能为空")
    private String configKey;

    /**
     * 配置值
     */
    @NotBlank(message = "配置值不能为空")
    private String configValue;

    /**
     * 配置描述
     */
    private String description;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
