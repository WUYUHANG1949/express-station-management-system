package com.wuyuhang.delivery.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wuyuhang.delivery.common.BusinessException;
import com.wuyuhang.delivery.common.PageResult;
import com.wuyuhang.delivery.common.enums.ParcelType;
import com.wuyuhang.delivery.common.enums.ShipStatus;
import com.wuyuhang.delivery.common.util.BizNoGenerator;
import com.wuyuhang.delivery.dto.ShipOrderDTO;
import com.wuyuhang.delivery.dto.query.ShipOrderQuery;
import com.wuyuhang.delivery.entity.ShipOrder;
import com.wuyuhang.delivery.mapper.ShipOrderMapper;
import com.wuyuhang.delivery.security.LoginUser;
import com.wuyuhang.delivery.security.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

/**
 * 寄件业务服务。
 * <p>
 * 收件是"外部快件进入驿站"，寄件是"用户从驿站把快件发出去"，
 * 两者的数据结构与业务规则差别较大，因此分成两张表、两个服务。
 *
 * @author 吴宇航
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShipOrderService {

    private final ShipOrderMapper shipOrderMapper;

    /**
     * 分页条件查询。
     */
    public PageResult<ShipOrder> page(long pageNum, long pageSize, ShipOrderQuery query) {
        // 员工只能查看本驿站的寄件单
        LoginUser loginUser = UserContext.require();
        if (!loginUser.isAdmin() && loginUser.getStationId() != null && query.getStationId() == null) {
            query.setStationId(loginUser.getStationId());
        }
        var result = shipOrderMapper.selectShipOrderPage(new Page<>(pageNum, pageSize), query);
        result.getRecords().forEach(this::fillDisplayFields);
        return PageResult.of(result);
    }

    /**
     * 寄件单详情。
     */
    public ShipOrder detail(Long id) {
        ShipOrder order = shipOrderMapper.selectShipOrderDetail(id);
        if (order == null) {
            throw new BusinessException("寄件单不存在或已被删除");
        }
        fillDisplayFields(order);
        return order;
    }

    /**
     * 寄件登记。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long create(ShipOrderDTO dto) {
        LoginUser loginUser = UserContext.require();

        ShipOrder order = new ShipOrder();
        order.setOrderNo(BizNoGenerator.generateShipOrderNo());
        order.setStationId(dto.getStationId());
        order.setExpressCompany(dto.getExpressCompany());
        order.setSenderName(dto.getSenderName());
        order.setSenderPhone(dto.getSenderPhone());
        order.setSenderAddress(dto.getSenderAddress());
        order.setReceiverName(dto.getReceiverName());
        order.setReceiverPhone(dto.getReceiverPhone());
        order.setReceiverAddress(dto.getReceiverAddress());
        order.setParcelType(StringUtils.hasText(dto.getParcelType()) ? dto.getParcelType() : "NORMAL");
        order.setWeight(dto.getWeight());
        order.setFreight(dto.getFreight());
        order.setInsuredValue(dto.getInsuredValue() == null ? BigDecimal.ZERO : dto.getInsuredValue());
        order.setStatus(ShipStatus.PENDING.name());
        order.setOperatorId(loginUser.getUserId());
        shipOrderMapper.insert(order);

        log.info("寄件登记成功：单号 {}，寄件人 {}，操作员 {}",
                order.getOrderNo(), dto.getSenderName(), loginUser.getRealName());
        return order.getId();
    }

    /**
     * 编辑寄件单。只有"待揽收"状态才允许修改。
     */
    public void update(Long id, ShipOrderDTO dto) {
        ShipOrder exist = shipOrderMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException("寄件单不存在");
        }
        if (!ShipStatus.PENDING.name().equals(exist.getStatus())) {
            throw new BusinessException("只有「待揽收」的寄件单才能修改");
        }
        ShipOrder order = new ShipOrder();
        order.setId(id);
        order.setStationId(dto.getStationId());
        order.setExpressCompany(dto.getExpressCompany());
        order.setSenderName(dto.getSenderName());
        order.setSenderPhone(dto.getSenderPhone());
        order.setSenderAddress(dto.getSenderAddress());
        order.setReceiverName(dto.getReceiverName());
        order.setReceiverPhone(dto.getReceiverPhone());
        order.setReceiverAddress(dto.getReceiverAddress());
        order.setParcelType(dto.getParcelType());
        order.setWeight(dto.getWeight());
        order.setFreight(dto.getFreight());
        order.setInsuredValue(dto.getInsuredValue());
        shipOrderMapper.updateById(order);
    }

    /**
     * 更新寄件单状态，可同时回填快递公司给的运单号。
     */
    public void updateStatus(Long id, String status, String waybillNo) {
        ShipOrder exist = shipOrderMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException("寄件单不存在");
        }
        if (!ShipStatus.isValid(status)) {
            throw new BusinessException("非法的状态值：" + status);
        }
        // 已取消的寄件单不允许再流转
        if (ShipStatus.CANCELLED.name().equals(exist.getStatus())) {
            throw new BusinessException("已取消的寄件单不能变更状态");
        }
        ShipOrder order = new ShipOrder();
        order.setId(id);
        order.setStatus(status);
        if (StringUtils.hasText(waybillNo)) {
            order.setWaybillNo(waybillNo.trim().toUpperCase());
        }
        shipOrderMapper.updateById(order);
        log.info("寄件单 {} 状态更新为 {}", exist.getOrderNo(), status);
    }

    /**
     * 删除寄件单（逻辑删除）。已发出的不允许删除。
     */
    public void delete(Long id) {
        ShipOrder exist = shipOrderMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException("寄件单不存在");
        }
        if (ShipStatus.SHIPPED.name().equals(exist.getStatus())) {
            throw new BusinessException("已发出的寄件单不允许删除");
        }
        shipOrderMapper.deleteById(id);
    }

    /**
     * 填充状态与类型的中文名称。
     */
    private void fillDisplayFields(ShipOrder order) {
        if (order == null) {
            return;
        }
        order.setStatusName(ShipStatus.labelOf(order.getStatus()));
        order.setParcelTypeName(ParcelType.labelOf(order.getParcelType()));
    }

    /**
     * 统计今日寄件单数量，供其它模块使用。
     */
    public long countToday(Long stationId) {
        return shipOrderMapper.selectCount(Wrappers.<ShipOrder>lambdaQuery()
                .eq(stationId != null, ShipOrder::getStationId, stationId)
                .apply("DATE(create_time) = CURDATE()"));
    }
}
