package com.wuyuhang.delivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.wuyuhang.delivery.dto.query.ShipOrderQuery;
import com.wuyuhang.delivery.entity.ShipOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 寄件单 Mapper。
 *
 * @author 吴宇航
 */
@Mapper
public interface ShipOrderMapper extends BaseMapper<ShipOrder> {

    /**
     * 条件组合查询 + 分页（关联驿站名称与受理员工姓名）。
     */
    IPage<ShipOrder> selectShipOrderPage(IPage<ShipOrder> page, @Param("q") ShipOrderQuery query);

    /**
     * 查询寄件单详情。
     */
    ShipOrder selectShipOrderDetail(@Param("id") Long id);
}
