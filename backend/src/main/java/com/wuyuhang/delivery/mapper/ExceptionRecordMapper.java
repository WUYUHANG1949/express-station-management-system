package com.wuyuhang.delivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.wuyuhang.delivery.dto.query.ExceptionQuery;
import com.wuyuhang.delivery.entity.ExceptionRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 异常件 Mapper。
 *
 * @author 吴宇航
 */
@Mapper
public interface ExceptionRecordMapper extends BaseMapper<ExceptionRecord> {

    /**
     * 条件组合查询 + 分页（关联驿站、收件人信息）。
     */
    IPage<ExceptionRecord> selectExceptionPage(IPage<ExceptionRecord> page, @Param("q") ExceptionQuery query);

    /**
     * 统计某快件是否已存在"未处理完"的异常记录，避免重复登记。
     */
    int countUnhandledByParcelId(@Param("parcelId") Long parcelId);
}
