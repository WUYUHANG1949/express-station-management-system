package com.wuyuhang.delivery.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wuyuhang.delivery.common.BusinessException;
import com.wuyuhang.delivery.common.PageResult;
import com.wuyuhang.delivery.common.enums.ExceptionType;
import com.wuyuhang.delivery.common.enums.HandleStatus;
import com.wuyuhang.delivery.common.enums.OperateType;
import com.wuyuhang.delivery.common.enums.ParcelStatus;
import com.wuyuhang.delivery.dto.ExceptionCreateDTO;
import com.wuyuhang.delivery.dto.ExceptionHandleDTO;
import com.wuyuhang.delivery.dto.query.ExceptionQuery;
import com.wuyuhang.delivery.entity.ExceptionRecord;
import com.wuyuhang.delivery.entity.Parcel;
import com.wuyuhang.delivery.mapper.ExceptionRecordMapper;
import com.wuyuhang.delivery.mapper.ParcelMapper;
import com.wuyuhang.delivery.mapper.ShelfMapper;
import com.wuyuhang.delivery.security.LoginUser;
import com.wuyuhang.delivery.security.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 异常件处理服务。
 * <p>
 * 破损、丢失、地址错误、拒收、长期未取等异常件不走正常流程，
 * 需要单独登记、单独跟进，处理完成后可同步更新快件状态。
 *
 * @author 吴宇航
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExceptionService {

    private final ExceptionRecordMapper exceptionMapper;
    private final ParcelMapper parcelMapper;
    private final ShelfMapper shelfMapper;
    private final ParcelService parcelService;

    /**
     * 分页条件查询。
     */
    public PageResult<ExceptionRecord> page(long pageNum, long pageSize, ExceptionQuery query) {
        LoginUser loginUser = UserContext.require();
        if (!loginUser.isAdmin() && loginUser.getStationId() != null && query.getStationId() == null) {
            query.setStationId(loginUser.getStationId());
        }
        var result = exceptionMapper.selectExceptionPage(new Page<>(pageNum, pageSize), query);
        result.getRecords().forEach(this::fillDisplayFields);
        return PageResult.of(result);
    }

    /**
     * 登记异常件：写入异常记录，并把快件状态改为「异常件」。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long create(ExceptionCreateDTO dto) {
        if (!ExceptionType.isValid(dto.getExceptionType())) {
            throw new BusinessException("非法的异常类型：" + dto.getExceptionType());
        }

        // 按 ID 或运单号定位快件
        Parcel parcel = null;
        if (dto.getParcelId() != null) {
            parcel = parcelMapper.selectById(dto.getParcelId());
        } else if (StringUtils.hasText(dto.getWaybillNo())) {
            parcel = parcelMapper.selectOne(Wrappers.<Parcel>lambdaQuery()
                    .eq(Parcel::getWaybillNo, dto.getWaybillNo().trim().toUpperCase()));
        }
        if (parcel == null) {
            throw new BusinessException("未找到对应的快件，请核对运单号");
        }

        // 同一件快件不允许重复登记未处理完的异常
        if (exceptionMapper.countUnhandledByParcelId(parcel.getId()) > 0) {
            throw new BusinessException("该快件已有未处理完的异常记录，请先处理");
        }

        LoginUser loginUser = UserContext.require();
        ExceptionRecord record = new ExceptionRecord();
        record.setParcelId(parcel.getId());
        record.setWaybillNo(parcel.getWaybillNo());
        record.setExceptionType(dto.getExceptionType());
        record.setDescription(dto.getDescription());
        record.setHandleStatus(HandleStatus.PENDING.name());
        record.setStationId(parcel.getStationId());
        record.setCreateTime(LocalDateTime.now());
        exceptionMapper.insert(record);

        // 同步把快件状态置为异常，避免异常件还显示"在库待取"而误导取件
        Parcel update = new Parcel();
        update.setId(parcel.getId());
        update.setStatus(ParcelStatus.EXCEPTION.name());
        parcelMapper.updateById(update);

        parcelService.saveTrace(parcel, OperateType.EXCEPTION,
                "登记异常件：" + ExceptionType.labelOf(dto.getExceptionType())
                        + (StringUtils.hasText(dto.getDescription()) ? "，" + dto.getDescription() : ""));

        log.info("登记异常件：运单号 {}，类型 {}", parcel.getWaybillNo(), dto.getExceptionType());
        return record.getId();
    }

    /**
     * 处理异常件：更新处理状态与结果，必要时同步快件状态。
     */
    @Transactional(rollbackFor = Exception.class)
    public void handle(Long id, ExceptionHandleDTO dto) {
        ExceptionRecord record = exceptionMapper.selectById(id);
        if (record == null) {
            throw new BusinessException("异常记录不存在");
        }
        if (!HandleStatus.isValid(dto.getHandleStatus())) {
            throw new BusinessException("非法的处理状态：" + dto.getHandleStatus());
        }
        if (HandleStatus.RESOLVED.name().equals(record.getHandleStatus())) {
            throw new BusinessException("该异常已处理完毕，无需重复处理");
        }

        LoginUser loginUser = UserContext.require();
        ExceptionRecord update = new ExceptionRecord();
        update.setId(id);
        update.setHandleStatus(dto.getHandleStatus());
        update.setHandleResult(dto.getHandleResult());
        update.setHandlerId(loginUser.getUserId());
        update.setHandlerName(loginUser.getRealName());
        if (HandleStatus.RESOLVED.name().equals(dto.getHandleStatus())) {
            update.setHandleTime(LocalDateTime.now());
        }
        exceptionMapper.updateById(update);

        // 需要同步快件状态时一并更新（例如退回、恢复在库）
        Parcel parcel = parcelMapper.selectById(record.getParcelId());
        if (parcel != null && StringUtils.hasText(dto.getParcelStatus())) {
            if (!ParcelStatus.isValid(dto.getParcelStatus())) {
                throw new BusinessException("非法的快件状态：" + dto.getParcelStatus());
            }
            Parcel parcelUpdate = new Parcel();
            parcelUpdate.setId(parcel.getId());
            parcelUpdate.setStatus(dto.getParcelStatus());
            parcelMapper.updateById(parcelUpdate);

            // 异常件转为「已退回」等不再占用货位的状态时，快件实际已离开驿站，
            // 必须释放其占用的货位，否则 shelf.used_count 会一直虚高，造成货位库存不一致
            if (parcel.getShelfId() != null
                    && ParcelService.isShelfOccupied(parcel.getStatus())
                    && !ParcelService.isShelfOccupied(dto.getParcelStatus())) {
                shelfMapper.release(parcel.getShelfId());
                // 置空 shelf_id：MyBatis-Plus 默认忽略 null 字段，需要用 UpdateWrapper 显式 set
                parcelMapper.update(null, Wrappers.<Parcel>lambdaUpdate()
                        .eq(Parcel::getId, parcel.getId())
                        .set(Parcel::getShelfId, null));
            }

            OperateType operateType = ParcelStatus.RETURNED.name().equals(dto.getParcelStatus())
                    ? OperateType.RETURN : OperateType.EXCEPTION_HANDLE;
            parcelService.saveTrace(parcel, operateType,
                    "异常件处理：" + dto.getHandleResult()
                            + "，快件状态同步为「" + ParcelStatus.labelOf(dto.getParcelStatus()) + "」");
        } else if (parcel != null) {
            parcelService.saveTrace(parcel, OperateType.EXCEPTION_HANDLE,
                    "异常件处理：" + dto.getHandleResult());
        }

        log.info("异常记录 {} 处理为 {}", id, dto.getHandleStatus());
    }

    /**
     * 填充中文名称。
     */
    private void fillDisplayFields(ExceptionRecord record) {
        if (record == null) {
            return;
        }
        record.setExceptionTypeName(ExceptionType.labelOf(record.getExceptionType()));
        record.setHandleStatusName(HandleStatus.labelOf(record.getHandleStatus()));
    }
}
