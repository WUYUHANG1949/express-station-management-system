package com.wuyuhang.delivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wuyuhang.delivery.entity.PickupRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 取件记录 Mapper。
 *
 * @author 吴宇航
 */
@Mapper
public interface PickupRecordMapper extends BaseMapper<PickupRecord> {
}
