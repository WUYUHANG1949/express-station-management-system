package com.wuyuhang.delivery.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 统一分页响应结构。
 *
 * @param <T> 列表元素类型
 * @author 吴宇航
 */
@Data
@Schema(description = "分页结果")
public class PageResult<T> implements Serializable {

    @Schema(description = "总记录数")
    private Long total;

    @Schema(description = "总页数")
    private Long pages;

    @Schema(description = "当前页码")
    private Long pageNum;

    @Schema(description = "每页条数")
    private Long pageSize;

    @Schema(description = "当前页数据")
    private List<T> list;

    public PageResult() {
        this.total = 0L;
        this.pages = 0L;
        this.pageNum = 1L;
        this.pageSize = 10L;
        this.list = new ArrayList<>();
    }

    public PageResult(Long total, Long pages, Long pageNum, Long pageSize, List<T> list) {
        this.total = total;
        this.pages = pages;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.list = list == null ? new ArrayList<>() : list;
    }

    /**
     * 由 MyBatis-Plus 的分页对象直接转换。
     */
    public static <T> PageResult<T> of(IPage<T> page) {
        return new PageResult<>(page.getTotal(), page.getPages(), page.getCurrent(), page.getSize(), page.getRecords());
    }

    /**
     * 由 MyBatis-Plus 的分页对象转换，并把实体逐条映射为 VO。
     *
     * @param page      分页结果
     * @param converter 实体 -> VO 的转换函数
     */
    public static <E, T> PageResult<T> of(IPage<E> page, Function<E, T> converter) {
        List<T> voList = page.getRecords().stream().map(converter).toList();
        return new PageResult<>(page.getTotal(), page.getPages(), page.getCurrent(), page.getSize(), voList);
    }

    /**
     * 空分页结果。
     */
    public static <T> PageResult<T> empty(Long pageNum, Long pageSize) {
        return new PageResult<>(0L, 0L, pageNum, pageSize, new ArrayList<>());
    }
}
