package com.tenant.common.vo;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 统一分页响应包装类
 * <p>用于将 MyBatis-Plus 的 {@link IPage} 转换为前端友好的分页响应格式，
 * 隐藏底层分页细节，统一 API 响应结构
 * <p>使用示例：
 * <pre>{@code
 * // Controller 中
 * IPage<SysUser> page = userService.page(query);
 * return ResultVO.success(PageVO.of(page));
 * }</pre>
 * <p>返回 JSON 结构：
 * <pre>{@code
 * {
 *   "code": 200,
 *   "message": "成功",
 *   "data": {
 *     "records": [...],
 *     "total": 100,
 *     "pages": 10,
 *     "current": 1,
 *     "size": 10
 *   }
 * }
 * }</pre>
 *
 * @param <T> 记录类型
 * @author Aze
 */
@Data
public class PageVO<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 数据列表
     */
    private List<T> records;

    /**
     * 总记录数
     */
    private long total;

    /**
     * 总页数
     */
    private long pages;

    /**
     * 当前页码
     */
    private long current;

    /**
     * 每页条数
     */
    private long size;

    /**
     * 空构造函数
     */
    public PageVO() {
    }

    /**
     * 从 IPage 构造分页响应
     *
     * @param page MyBatis-Plus 分页对象
     * @param <T>  记录类型
     * @return 分页响应对象
     */
    public static <T> PageVO<T> of(IPage<T> page) {
        PageVO<T> pageVO = new PageVO<>();
        pageVO.setRecords(page.getRecords());
        pageVO.setTotal(page.getTotal());
        pageVO.setPages(page.getPages());
        pageVO.setCurrent(page.getCurrent());
        pageVO.setSize(page.getSize());
        return pageVO;
    }

    /**
     * 手动构造分页响应（适用于非 MyBatis-Plus 数据源）
     *
     * @param records 数据列表
     * @param total   总记录数
     * @param current 当前页码
     * @param size    每页条数
     * @param <T>     记录类型
     * @return 分页响应对象
     */
    public static <T> PageVO<T> of(List<T> records, long total, long current, long size) {
        PageVO<T> pageVO = new PageVO<>();
        pageVO.setRecords(records);
        pageVO.setTotal(total);
        long pages = total / size + (total % size == 0 ? 0 : 1);
        pageVO.setPages(pages);
        pageVO.setCurrent(current);
        pageVO.setSize(size);
        return pageVO;
    }
}
