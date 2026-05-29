package com.tenant.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统字典实体类
 * 映射数据库 sys_dict 表，用于系统字典数据管理
 * <p>共享表，在 {@link com.tenant.core.config.MybatisPlusConfig} 中被标记为忽略租户过滤
 * <p>逻辑删除：{@link TableLogic} 标记 deleted 字段
 *
 * @author Aze
 */
@Data
@TableName("sys_dict")
public class DictEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 字典类型
     */
    @NotBlank(message = "字典类型不能为空")
    private String dictType;

    /**
     * 字典标签
     */
    @NotBlank(message = "字典标签不能为空")
    private String dictLabel;

    /**
     * 字典值
     */
    @NotBlank(message = "字典值不能为空")
    private String dictValue;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 状态（1-启用，0-禁用）
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
