package com.wuyuhang.delivery.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wuyuhang.delivery.common.BusinessException;
import com.wuyuhang.delivery.common.PageResult;
import com.wuyuhang.delivery.dto.ShelfSaveDTO;
import com.wuyuhang.delivery.entity.Parcel;
import com.wuyuhang.delivery.entity.Shelf;
import com.wuyuhang.delivery.mapper.ParcelMapper;
import com.wuyuhang.delivery.mapper.ShelfMapper;
import com.wuyuhang.delivery.security.LoginUser;
import com.wuyuhang.delivery.security.UserContext;
import com.wuyuhang.delivery.vo.ShelfMapVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 货位管理服务。
 *
 * @author 吴宇航
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShelfService {

    private final ShelfMapper shelfMapper;
    private final ParcelMapper parcelMapper;
    private final ParcelService parcelService;

    /**
     * 查询某驿站的货位列表（含剩余容量）。
     */
    public List<Shelf> list(Long stationId) {
        List<Shelf> shelves = shelfMapper.selectShelfWithStation(stationId);
        shelves.forEach(this::fillFreeCount);
        return shelves;
    }

    /**
     * 查询仍有剩余容量的货位，用于收件登记时的下拉选择。
     */
    public List<Shelf> available(Long stationId) {
        if (stationId == null) {
            throw new BusinessException("请先选择驿站");
        }
        List<Shelf> shelves = shelfMapper.selectList(Wrappers.<Shelf>lambdaQuery()
                .eq(Shelf::getStationId, stationId)
                .eq(Shelf::getStatus, 1)
                .apply("used_count < capacity")
                .orderByAsc(Shelf::getShelfCode));
        shelves.forEach(this::fillFreeCount);
        return shelves;
    }

    /**
     * 分页查询货位。
     */
    public PageResult<Shelf> page(long pageNum, long pageSize, Long stationId, String shelfCode, String area) {
        Page<Shelf> page = shelfMapper.selectPage(new Page<>(pageNum, pageSize),
                Wrappers.<Shelf>lambdaQuery()
                        .eq(stationId != null, Shelf::getStationId, stationId)
                        .like(StringUtils.hasText(shelfCode), Shelf::getShelfCode, shelfCode)
                        .eq(StringUtils.hasText(area), Shelf::getArea, area)
                        .orderByAsc(Shelf::getStationId)
                        .orderByAsc(Shelf::getShelfCode));
        page.getRecords().forEach(this::fillFreeCount);
        return PageResult.of(page);
    }

    /**
     * 新增货位。
     */
    public Long create(ShelfSaveDTO dto) {
        Long count = shelfMapper.selectCount(Wrappers.<Shelf>lambdaQuery()
                .eq(Shelf::getStationId, dto.getStationId())
                .eq(Shelf::getShelfCode, dto.getShelfCode()));
        if (count != null && count > 0) {
            throw new BusinessException("该驿站下已存在相同的库位编号");
        }
        Shelf shelf = new Shelf();
        shelf.setStationId(dto.getStationId());
        shelf.setShelfCode(dto.getShelfCode());
        shelf.setArea(dto.getArea());
        shelf.setCapacity(dto.getCapacity());
        shelf.setUsedCount(0);
        shelf.setStatus(dto.getStatus());
        shelfMapper.insert(shelf);
        return shelf.getId();
    }

    /**
     * 编辑货位。容量不允许小于当前已占用数量。
     */
    public void update(Long id, ShelfSaveDTO dto) {
        Shelf exist = shelfMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException("货位不存在");
        }
        if (dto.getCapacity() != null && exist.getUsedCount() != null
                && dto.getCapacity() < exist.getUsedCount()) {
            throw new BusinessException("库位容量不能小于当前已占用的 " + exist.getUsedCount() + " 件");
        }
        Shelf shelf = new Shelf();
        shelf.setId(id);
        shelf.setStationId(dto.getStationId());
        shelf.setShelfCode(dto.getShelfCode());
        shelf.setArea(dto.getArea());
        shelf.setCapacity(dto.getCapacity());
        shelf.setStatus(dto.getStatus());
        shelfMapper.updateById(shelf);
    }

    /**
     * 删除货位。有在库快件时禁止删除，必须先转移快件。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Shelf shelf = shelfMapper.selectById(id);
        if (shelf == null) {
            throw new BusinessException("货位不存在");
        }
        if (shelf.getUsedCount() != null && shelf.getUsedCount() > 0) {
            throw new BusinessException("该货位仍存放有快件，请先转移后再删除");
        }
        parcelMapper.clearShelf(id);
        shelfMapper.deleteById(id);
        log.info("删除货位 [{}]", shelf.getShelfCode());
    }

    /**
     * 货位地图：按库位返回占用情况与库位上的快件清单，供前端画货架网格图。
     * <p>
     * 实现上用「两次查询 + 内存分组」代替 N+1 次查询：
     * 先取该驿站全部库位，再一次性取回所有仍占用货位的快件，按 shelfId 分组挂载。
     *
     * @param stationId 驿站ID
     */
    public List<ShelfMapVO> shelfMap(Long stationId) {
        Long scopeStationId = resolveStationScope(stationId);
        List<Shelf> shelves = shelfMapper.selectShelfWithStation(scopeStationId);
        List<Parcel> parcels = parcelMapper.selectOnShelfParcels(scopeStationId);

        // 按库位分组，key 为 shelfId
        Map<Long, List<Parcel>> grouped = parcels.stream()
                .filter(p -> p.getShelfId() != null)
                .peek(parcelService::fillDisplayFields)
                .collect(Collectors.groupingBy(Parcel::getShelfId));

        return shelves.stream().map(shelf -> {
            ShelfMapVO vo = new ShelfMapVO(shelf.getId(), shelf.getStationId(), shelf.getStationName(),
                    shelf.getShelfCode(), shelf.getArea(),
                    shelf.getCapacity(), shelf.getUsedCount(), shelf.getStatus());
            vo.setParcels(grouped.getOrDefault(shelf.getId(), new ArrayList<>()));
            return vo;
        }).toList();
    }

    /**
     * 按快件表重算库位占用数量，用于修复因异常中断导致的不一致数据。
     * <p>
     * 非管理员只能重算本人所属驿站，防止越站修改他站数据。
     */
    @Transactional(rollbackFor = Exception.class)
    public int recalculate(Long stationId) {
        Long scopeStationId = resolveStationScope(stationId);
        int rows = shelfMapper.recalcUsedCount(scopeStationId);
        log.info("重算货位占用数量完成，影响 {} 条记录", rows);
        return rows;
    }

    /**
     * 解析驿站数据权限范围。
     * <p>
     * 管理员可以查看/操作任意驿站（stationId 为空表示全部）；
     * 驿站员工一律被强制收敛到本人所属驿站，
     * 这样即使前端伪造 stationId 也无法读取或改动其它驿站的数据。
     *
     * @param stationId 前端传入的驿站ID
     * @return 实际生效的驿站ID
     */
    private Long resolveStationScope(Long stationId) {
        LoginUser loginUser = UserContext.require();
        if (loginUser.isAdmin()) {
            return stationId;
        }
        return loginUser.getStationId();
    }

    private void fillFreeCount(Shelf shelf) {
        int capacity = shelf.getCapacity() == null ? 0 : shelf.getCapacity();
        int used = shelf.getUsedCount() == null ? 0 : shelf.getUsedCount();
        shelf.setFreeCount(Math.max(capacity - used, 0));
    }
}
