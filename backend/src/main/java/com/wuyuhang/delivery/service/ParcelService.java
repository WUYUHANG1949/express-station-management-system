package com.wuyuhang.delivery.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wuyuhang.delivery.common.BusinessException;
import com.wuyuhang.delivery.common.PageResult;
import com.wuyuhang.delivery.common.enums.OperateType;
import com.wuyuhang.delivery.common.enums.NotifyDict;
import com.wuyuhang.delivery.common.enums.ParcelStatus;
import com.wuyuhang.delivery.common.enums.ParcelType;
import com.wuyuhang.delivery.common.enums.PickupDict;
import com.wuyuhang.delivery.common.util.BizNoGenerator;
import com.wuyuhang.delivery.common.util.ExpressCompanyUtil;
import com.wuyuhang.delivery.dto.ParcelInStoreDTO;
import com.wuyuhang.delivery.dto.ParcelPickupDTO;
import com.wuyuhang.delivery.dto.ParcelUpdateDTO;
import com.wuyuhang.delivery.dto.query.ParcelQuery;
import com.wuyuhang.delivery.entity.Parcel;
import com.wuyuhang.delivery.entity.ParcelTrace;
import com.wuyuhang.delivery.entity.PickupRecord;
import com.wuyuhang.delivery.entity.Shelf;
import com.wuyuhang.delivery.entity.SysUser;
import com.wuyuhang.delivery.mapper.ParcelMapper;
import com.wuyuhang.delivery.mapper.ParcelTraceMapper;
import com.wuyuhang.delivery.mapper.PickupRecordMapper;
import com.wuyuhang.delivery.mapper.ShelfMapper;
import com.wuyuhang.delivery.mapper.SysUserMapper;
import com.wuyuhang.delivery.security.LoginUser;
import com.wuyuhang.delivery.security.UserContext;
import com.wuyuhang.delivery.vo.OverdueFeeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 快件业务服务 —— 系统的核心。
 * <p>
 * 其中 {@link #inStore} 与 {@link #pickup} 是本系统最关键的两个方法：
 * 它们同时修改「快件状态」「货位占用数量」「轨迹」「取件记录」四类数据，
 * 因此必须加上 {@code @Transactional}。以取件核销为例，如果释放货位这一步失败，
 * 整个事务回滚，快件仍然是"在库"状态，避免出现「货位显示空闲但快件还在架上」
 * 或者反过来的库存不一致问题。
 *
 * @author 吴宇航
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParcelService {

    /** 逾期保管费单价：元 / 天 */
    private static final BigDecimal OVERDUE_FEE_PER_DAY = new BigDecimal("2.00");

    /** 默认免费保管天数 */
    private static final int DEFAULT_FREE_DAYS = 3;

    /** 生成取件码时的最大重试次数，避免极端情况下死循环 */
    private static final int MAX_PICKUP_CODE_RETRY = 20;

    private final ParcelMapper parcelMapper;
    private final ParcelTraceMapper traceMapper;
    private final PickupRecordMapper pickupRecordMapper;
    private final ShelfMapper shelfMapper;
    private final SysUserMapper userMapper;
    private final ExcelExportService excelExportService;
    private final NotifyService notifyService;

    // ==================================================================
    //  一、收件登记（入库）
    // ==================================================================

    /**
     * 收件登记：生成取件码、分配货位、写入快件与轨迹。
     *
     * @param dto 登记参数
     * @return 新建的快件（含取件码）
     */
    @Transactional(rollbackFor = Exception.class)
    public Parcel inStore(ParcelInStoreDTO dto) {
        String waybillNo = dto.getWaybillNo().trim().toUpperCase();

        // 1. 按快递公司的编码规则校验运单号格式
        if (!ExpressCompanyUtil.isValid(dto.getExpressCompany(), waybillNo)) {
            throw new BusinessException(ExpressCompanyUtil.ruleHint(dto.getExpressCompany()));
        }

        // 2. 运单号唯一性校验
        Long exists = parcelMapper.selectCount(
                Wrappers.<Parcel>lambdaQuery().eq(Parcel::getWaybillNo, waybillNo));
        if (exists != null && exists > 0) {
            throw new BusinessException("该运单号已登记，请勿重复入库");
        }

        // 3. 确定驿站：优先取前端传入，其次取当前员工所属驿站
        LoginUser loginUser = UserContext.require();
        Long stationId = dto.getStationId() != null ? dto.getStationId() : loginUser.getStationId();
        if (stationId == null) {
            throw new BusinessException("请先选择入库驿站");
        }

        // 4. 分配货位：指定则校验，未指定则自动挑选剩余容量最大的库位
        Long shelfId = resolveShelfId(stationId, dto.getShelfId());

        // 5. 占用货位（带容量条件的 UPDATE，返回 0 说明已满）
        if (shelfId != null && shelfMapper.occupy(shelfId) == 0) {
            throw new BusinessException("该货位已满，请选择其他货位");
        }

        // 6. 生成不重复的取件码
        String pickupCode = generateUniquePickupCode();

        // 7. 组装并保存快件
        Parcel parcel = new Parcel();
        parcel.setWaybillNo(waybillNo);
        parcel.setStationId(stationId);
        parcel.setShelfId(shelfId);
        parcel.setExpressCompany(dto.getExpressCompany());
        parcel.setParcelType(StringUtils.hasText(dto.getParcelType()) ? dto.getParcelType() : "NORMAL");
        parcel.setReceiverName(dto.getReceiverName());
        parcel.setReceiverPhone(dto.getReceiverPhone());
        parcel.setPickupCode(pickupCode);
        parcel.setWeight(dto.getWeight());
        parcel.setFreight(dto.getFreight() == null ? BigDecimal.ZERO : dto.getFreight());
        parcel.setStatus(ParcelStatus.IN_STORE.name());
        parcel.setInTime(LocalDateTime.now());
        parcel.setOverdueDays(dto.getOverdueDays() == null ? DEFAULT_FREE_DAYS : dto.getOverdueDays());
        parcel.setStorageFee(BigDecimal.ZERO);
        parcel.setOperatorId(loginUser.getUserId());
        parcel.setRemark(dto.getRemark());
        parcelMapper.insert(parcel);

        // 8. 写入轨迹，做到操作留痕
        String shelfCode = shelfId == null ? "待分配" : String.valueOf(getShelfCode(shelfId));
        saveTrace(parcel, OperateType.IN_STORE,
                "快件入库登记成功，取件码 " + pickupCode + "，存放库位 " + shelfCode);

        log.info("快件入库：运单号 {}，取件码 {}，驿站 {}，货位 {}",
                waybillNo, pickupCode, stationId, shelfCode);

        // 到件后自动给收件人发送取件通知（内部已捕获异常，通知失败不会影响入库）
        notifyService.autoSend(parcel, NotifyDict.NotifyType.IN_STORE, NotifyDict.Channel.SMS);

        Parcel saved = parcelMapper.selectParcelDetail(parcel.getId());
        fillDisplayFields(saved);
        return saved;
    }

    // ==================================================================
    //  二、取件核销（出库）
    // ==================================================================

    /**
     * 取件核销：校验取件码、更新快件状态、释放货位、写入取件记录与轨迹。
     * <p>
     * 全流程在一个事务内完成，保证"快件状态"与"货位占用"始终一致。
     */
    @Transactional(rollbackFor = Exception.class)
    public PickupRecord pickup(ParcelPickupDTO dto) {
        LoginUser loginUser = UserContext.require();

        Parcel parcel = parcelMapper.selectOne(
                Wrappers.<Parcel>lambdaQuery().eq(Parcel::getWaybillNo, dto.getWaybillNo().trim().toUpperCase()));
        if (parcel == null) {
            throw new BusinessException("未找到该运单号对应的快件");
        }

        // 1. 取件码必须与运单号匹配，防止拿错件
        if (!parcel.getPickupCode().equals(dto.getPickupCode().trim())) {
            throw new BusinessException("取件码不正确，请核对");
        }

        // 2. 状态校验：只有在库或派送中的快件才能核销
        if (!ParcelStatus.IN_STORE.name().equals(parcel.getStatus())
                && !ParcelStatus.DELIVERING.name().equals(parcel.getStatus())) {
            throw new BusinessException("该快件当前状态为「"
                    + ParcelStatus.labelOf(parcel.getStatus()) + "」，不可取件");
        }

        // 3. 计算应收保管费（前端传入则以前端为准，否则按规则自动计算）
        BigDecimal storageFee = dto.getStorageFee() != null
                ? dto.getStorageFee()
                : calculateFee(parcel.getInTime(), parcel.getOverdueDays()).getOverdueFee();

        // 取件方式与核验方式必须落在约定的枚举集合内，避免写入脏数据
        String pickupType = StringUtils.hasText(dto.getPickupType()) ? dto.getPickupType() : "SELF";
        String verifyType = StringUtils.hasText(dto.getVerifyType()) ? dto.getVerifyType() : "CODE";
        if (!PickupDict.PickupType.isValid(pickupType)) {
            throw new BusinessException("非法的取件方式：" + pickupType);
        }
        if (!PickupDict.VerifyType.isValid(verifyType)) {
            throw new BusinessException("非法的核验方式：" + verifyType);
        }

        LocalDateTime now = LocalDateTime.now();

        // 4. 更新快件状态为已取件
        Parcel update = new Parcel();
        update.setId(parcel.getId());
        update.setStatus(ParcelStatus.PICKED_UP.name());
        update.setPickupTime(now);
        update.setStorageFee(storageFee);
        parcelMapper.updateById(update);

        // 5. 释放货位占用 —— 这是"库存不一致"问题的关键修复点，与上一步同事务
        if (parcel.getShelfId() != null) {
            shelfMapper.release(parcel.getShelfId());
        }

        // 6. 写入取件记录（业务凭证）
        PickupRecord record = new PickupRecord();
        record.setParcelId(parcel.getId());
        record.setWaybillNo(parcel.getWaybillNo());
        record.setPickupCode(parcel.getPickupCode());
        record.setReceiverName(dto.getReceiverName());
        record.setReceiverPhone(StringUtils.hasText(dto.getReceiverPhone())
                ? dto.getReceiverPhone() : parcel.getReceiverPhone());
        record.setPickupType(pickupType);
        record.setVerifyType(verifyType);
        record.setStorageFee(storageFee);
        record.setStationId(parcel.getStationId());
        record.setOperatorId(loginUser.getUserId());
        record.setOperatorName(loginUser.getRealName());
        record.setPickupTime(now);
        record.setRemark(dto.getRemark());
        pickupRecordMapper.insert(record);

        // 7. 写入轨迹
        parcel.setStatus(ParcelStatus.PICKED_UP.name());
        parcel.setPickupTime(now);
        saveTrace(parcel, OperateType.PICKUP,
                "取件核销完成，" + (storageFee.compareTo(BigDecimal.ZERO) > 0
                        ? "收取逾期保管费 " + storageFee.toPlainString() + " 元" : "无需缴纳保管费"));

        // 8. 自动发送取件确认通知（通知失败不影响核销）
        notifyService.autoSend(parcel, NotifyDict.NotifyType.PICKUP_DONE, NotifyDict.Channel.APP);

        log.info("取件核销：运单号 {}，取件码 {}，操作员 {}，保管费 {}",
                parcel.getWaybillNo(), parcel.getPickupCode(), loginUser.getRealName(), storageFee);
        return record;
    }

    // ==================================================================
    //  三、查询
    // ==================================================================

    /**
     * 分页条件查询，并按角色做数据权限过滤：
     * <ul>
     *   <li>系统管理员：可查看全部驿站数据；</li>
     *   <li>驿站员工：只能查看本驿站数据；</li>
     *   <li>普通用户：只能查看本人手机号名下的快件。</li>
     * </ul>
     */
    public PageResult<Parcel> page(long pageNum, long pageSize, ParcelQuery query) {
        applyDataScope(query);
        Page<Parcel> page = new Page<>(pageNum, pageSize);
        var result = parcelMapper.selectParcelPage(page, query);
        result.getRecords().forEach(this::fillDisplayFields);
        return PageResult.of(result);
    }

    /**
     * 取件核销页的模糊查询：支持取件码、运单号、手机号、姓名。
     */
    public List<Parcel> queryByKeyword(String keyword, Long stationId) {
        if (!StringUtils.hasText(keyword)) {
            throw new BusinessException("请输入取件码、运单号或手机号");
        }
        LoginUser loginUser = UserContext.require();
        Long scopeStationId = stationId != null ? stationId : loginUser.getStationId();
        String onlyPhone = isStaffOrAdmin() ? null : currentUserPhone();

        List<Parcel> list = parcelMapper.selectByKeyword(keyword.trim(), scopeStationId, onlyPhone, 20);
        list.forEach(this::fillDisplayFields);
        return list;
    }

    /**
     * 查询快件详情。
     */
    public Parcel detail(Long id) {
        Parcel parcel = parcelMapper.selectParcelDetail(id);
        if (parcel == null) {
            throw new BusinessException("快件不存在或已被删除");
        }
        fillDisplayFields(parcel);
        return parcel;
    }

    /**
     * 查询快件轨迹。
     */
    public List<ParcelTrace> traces(Long parcelId) {
        return traceMapper.selectByParcelId(parcelId);
    }

    /**
     * 逾期保管费试算。
     */
    public OverdueFeeVO overdueFee(Long id) {
        Parcel parcel = parcelMapper.selectById(id);
        if (parcel == null) {
            throw new BusinessException("快件不存在");
        }
        return calculateFee(parcel.getInTime(), parcel.getOverdueDays());
    }

    /**
     * 导出用的查询（不分页），同样受数据权限约束。
     */
    public byte[] exportLedger(ParcelQuery query) {
        applyDataScope(query);
        List<Parcel> parcels = parcelMapper.selectParcelList(query);
        parcels.forEach(this::fillDisplayFields);
        return excelExportService.exportParcelLedger(parcels, query);
    }

    /**
     * 导出文件名。
     */
    public String exportFileName() {
        return excelExportService.buildFileName();
    }

    /**
     * 分页查询逾期未取快件。
     * <p>
     * 「逾期」= 已保管天数超过免费保管天数，且超出部分达到 minDays 天，
     * 便于按「逾期 1 天以上 / 3 天以上 / 7 天以上」分层催取。
     *
     * @param stationId 驿站ID，管理员可指定；员工与普通用户由后端强制收敛
     * @param minDays   至少逾期天数，默认 1
     */
    public PageResult<Parcel> overduePage(long pageNum, long pageSize, Long stationId, Integer minDays) {
        LoginUser loginUser = UserContext.require();
        Long scopeStationId = stationId;
        String limitPhone = null;
        if (loginUser.isAdmin()) {
            // 管理员可查看任意驿站，stationId 为空表示全部
        } else if (loginUser.hasRole("STAFF")) {
            scopeStationId = loginUser.getStationId();
        } else {
            // 普通用户只能看到本人名下逾期未取的快件
            scopeStationId = null;
            limitPhone = currentUserPhone();
        }
        int days = (minDays == null || minDays < 1) ? 1 : minDays;

        var result = parcelMapper.selectOverduePage(new Page<>(pageNum, pageSize), scopeStationId, days, limitPhone);
        result.getRecords().forEach(this::fillDisplayFields);
        return PageResult.of(result);
    }

    /**
     * 查询逾期未取快件数量，供首页与侧边栏角标使用。
     * <p>
     * 数据权限与分页接口保持一致，避免"角标显示的数字比列表里能看到的还多"：
     * 管理员可看全部或指定驿站；员工固定本驿站；普通用户只统计本人手机号名下的逾期件。
     */
    public int countOverdue(Long stationId) {
        LoginUser loginUser = UserContext.require();
        if (loginUser.isAdmin()) {
            return parcelMapper.countOverdue(stationId, null);
        }
        if (loginUser.hasRole("STAFF")) {
            return parcelMapper.countOverdue(loginUser.getStationId(), null);
        }
        // 普通用户：stationId 必须置空，否则会统计到全部驿站的逾期件
        return parcelMapper.countOverdue(null, currentUserPhone());
    }

    // ==================================================================
    //  四、其他业务操作
    // ==================================================================

    /**
     * 编辑快件信息。修改货位时会同步调整新旧货位的占用数量。
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ParcelUpdateDTO dto) {
        Parcel exist = parcelMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException("快件不存在");
        }
        if (ParcelStatus.PICKED_UP.name().equals(exist.getStatus())) {
            throw new BusinessException("已取件的快件不允许再修改");
        }

        Parcel update = new Parcel();
        update.setId(id);
        update.setReceiverName(dto.getReceiverName());
        update.setReceiverPhone(dto.getReceiverPhone());
        update.setParcelType(dto.getParcelType());
        update.setOverdueDays(dto.getOverdueDays());
        update.setRemark(dto.getRemark());

        // 货位变更：释放旧货位、占用新货位，两步都在同一事务内
        if (dto.getShelfId() != null && !dto.getShelfId().equals(exist.getShelfId())) {
            Shelf target = shelfMapper.selectById(dto.getShelfId());
            if (target == null || !target.getStationId().equals(exist.getStationId())) {
                throw new BusinessException("目标货位不属于该快件所在驿站");
            }
            if (shelfMapper.occupy(dto.getShelfId()) == 0) {
                throw new BusinessException("该货位已满，请选择其他货位");
            }
            if (exist.getShelfId() != null) {
                shelfMapper.release(exist.getShelfId());
            }
            update.setShelfId(dto.getShelfId());
            saveTrace(exist, OperateType.TRANSFER,
                    "快件货位由 " + exist.getShelfCode() + " 调整为 " + target.getShelfCode());
        }

        parcelMapper.updateById(update);
        saveTrace(exist, OperateType.EDIT, "快件信息被修改");
    }

    /**
     * 删除快件（逻辑删除），同时释放占用的货位。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Parcel exist = parcelMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException("快件不存在");
        }
        // 仅"快件仍在货架上"的状态需要释放货位（在库、派送中、异常件）；
        // 已取件、已退回的快件货位早已释放，重复释放会导致占用数偏小
        if (exist.getShelfId() != null && isShelfOccupied(exist.getStatus())) {
            shelfMapper.release(exist.getShelfId());
        }
        parcelMapper.deleteById(id);
        log.info("删除快件：运单号 {}", exist.getWaybillNo());
    }

    /**
     * 派送出库：在库 -> 派送中。
     */
    @Transactional(rollbackFor = Exception.class)
    public void deliver(Long id) {
        Parcel exist = parcelMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException("快件不存在");
        }
        if (!ParcelStatus.IN_STORE.name().equals(exist.getStatus())) {
            throw new BusinessException("只有在库快件才能发起派送");
        }
        Parcel update = new Parcel();
        update.setId(id);
        update.setStatus(ParcelStatus.DELIVERING.name());
        parcelMapper.updateById(update);
        saveTrace(exist, OperateType.DELIVER, "员工发起派送，快件出库派送中");
    }

    // ==================================================================
    //  五、私有方法
    // ==================================================================

    /**
     * 判断某个状态下快件是否仍占用货位。
     * <p>
     * 统一口径：在库待取、派送中、异常件这几种状态下快件实体仍在驿站货架上，
     * 需要占用库位；已取件与已退回则不再占用。本方法被入库、取件、删除、
     * 异常处理以及货位重算等多处复用，避免各处口径不一致。
     */
    public static boolean isShelfOccupied(String status) {
        return ParcelStatus.IN_STORE.name().equals(status)
                || ParcelStatus.DELIVERING.name().equals(status)
                || ParcelStatus.EXCEPTION.name().equals(status);
    }

    /**
     * 确定要使用的货位：指定货位时做归属与容量校验，未指定时自动分配。
     */
    private Long resolveShelfId(Long stationId, Long shelfId) {
        if (shelfId != null) {
            Shelf shelf = shelfMapper.selectById(shelfId);
            if (shelf == null) {
                throw new BusinessException("所选货位不存在");
            }
            if (!shelf.getStationId().equals(stationId)) {
                throw new BusinessException("所选货位不属于当前驿站");
            }
            if (shelf.getStatus() != null && shelf.getStatus() == 0) {
                throw new BusinessException("所选货位已停用，请重新选择");
            }
            return shelfId;
        }
        // 自动分配：挑选剩余容量最大的可用货位，尽量把快件摊开存放
        Shelf best = shelfMapper.selectBestAvailable(stationId);
        return best == null ? null : best.getId();
    }

    /**
     * 生成一个当前未被使用的取件码。
     */
    private String generateUniquePickupCode() {
        for (int i = 0; i < MAX_PICKUP_CODE_RETRY; i++) {
            String code = BizNoGenerator.generatePickupCode();
            Long count = parcelMapper.selectCount(Wrappers.<Parcel>lambdaQuery()
                    .eq(Parcel::getPickupCode, code)
                    .in(Parcel::getStatus, ParcelStatus.IN_STORE.name(), ParcelStatus.DELIVERING.name()));
            if (count == null || count == 0) {
                return code;
            }
        }
        throw new BusinessException("取件码生成失败，请重试");
    }

    /**
     * 写入一条快件轨迹。供异常件服务等其它业务复用。
     */
    public void saveTrace(Parcel parcel, OperateType operateType, String description) {
        LoginUser loginUser = UserContext.get();
        ParcelTrace trace = new ParcelTrace();
        trace.setParcelId(parcel.getId());
        trace.setWaybillNo(parcel.getWaybillNo());
        trace.setOperateType(operateType.name());
        trace.setOperateDesc(description);
        trace.setStationId(parcel.getStationId());
        if (loginUser != null) {
            trace.setOperatorId(loginUser.getUserId());
            trace.setOperatorName(loginUser.getRealName());
        }
        trace.setOperateTime(LocalDateTime.now());
        traceMapper.insert(trace);
    }

    /**
     * 计算逾期保管费。
     * <p>
     * 规则：已保管天数超过免费保管天数后，超出部分按 2 元/天计费。
     */
    private OverdueFeeVO calculateFee(LocalDateTime inTime, Integer freeDays) {
        int free = freeDays == null ? DEFAULT_FREE_DAYS : freeDays;
        if (inTime == null) {
            return new OverdueFeeVO(0, free, 0, BigDecimal.ZERO);
        }
        int storageDays = (int) ChronoUnit.DAYS.between(inTime.toLocalDate(), LocalDate.now());
        int overdueDays = Math.max(0, storageDays - free);
        BigDecimal fee = OVERDUE_FEE_PER_DAY.multiply(BigDecimal.valueOf(overdueDays));
        return new OverdueFeeVO(storageDays, free, overdueDays, fee);
    }

    /**
     * 填充前端展示用的派生字段（状态中文名、类型中文名、保管天数、逾期天数、逾期费）。
     * <p>
     * 其它服务（货位地图、数据大屏等）也需要同一套派生字段，因此这里对外开放。
     */
    public void fillDisplayFields(Parcel parcel) {
        if (parcel == null) {
            return;
        }
        parcel.setStatusName(ParcelStatus.labelOf(parcel.getStatus()));
        parcel.setParcelTypeName(ParcelType.labelOf(parcel.getParcelType()));
        OverdueFeeVO fee = calculateFee(parcel.getInTime(), parcel.getOverdueDays());
        parcel.setStorageDays(fee.getStorageDays());
        parcel.setOverdueDayCount(fee.getOverdueDays());
        parcel.setOverdueFee(
                ParcelStatus.PICKED_UP.name().equals(parcel.getStatus()) && parcel.getStorageFee() != null
                        ? parcel.getStorageFee() : fee.getOverdueFee());
    }

    /**
     * 数据权限过滤：把当前用户的可见范围写入查询条件，前端无法绕过。
     */
    private void applyDataScope(ParcelQuery query) {
        LoginUser loginUser = UserContext.require();
        if (loginUser.isAdmin()) {
            return;
        }
        if (loginUser.hasRole("STAFF")) {
            // 员工只能看到本驿站的数据
            if (loginUser.getStationId() != null && query.getStationId() == null) {
                query.setStationId(loginUser.getStationId());
            }
            return;
        }
        // 普通用户：只能看本人手机号名下的快件
        query.setLimitPhone(currentUserPhone());
    }

    /**
     * 当前用户是否属于员工或管理员。
     */
    private boolean isStaffOrAdmin() {
        LoginUser loginUser = UserContext.require();
        return loginUser.isAdmin() || loginUser.hasRole("STAFF");
    }

    /**
     * 查询当前登录用户的手机号。
     */
    private String currentUserPhone() {
        SysUser user = userMapper.selectById(UserContext.userId());
        return user == null ? null : user.getPhone();
    }

    /**
     * 查询货位编号，仅用于拼装轨迹描述。
     */
    private String getShelfCode(Long shelfId) {
        Shelf shelf = shelfMapper.selectById(shelfId);
        return shelf == null ? "待分配" : shelf.getShelfCode();
    }
}
