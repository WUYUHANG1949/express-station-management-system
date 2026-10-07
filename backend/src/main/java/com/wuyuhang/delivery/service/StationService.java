package com.wuyuhang.delivery.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wuyuhang.delivery.common.BusinessException;
import com.wuyuhang.delivery.common.PageResult;
import com.wuyuhang.delivery.dto.StationSaveDTO;
import com.wuyuhang.delivery.entity.Station;
import com.wuyuhang.delivery.mapper.ParcelMapper;
import com.wuyuhang.delivery.mapper.StationMapper;
import com.wuyuhang.delivery.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 驿站管理服务。
 *
 * @author 吴宇航
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StationService {

    private final StationMapper stationMapper;
    private final SysUserMapper userMapper;
    private final ParcelMapper parcelMapper;

    /**
     * 查询全部启用驿站（下拉框使用，所有登录用户可读）。
     */
    public List<Station> list() {
        return stationMapper.selectList(Wrappers.<Station>lambdaQuery()
                .eq(Station::getStatus, 1)
                .orderByAsc(Station::getId));
    }

    /**
     * 分页查询驿站，并带出货位数量、在库快件数。
     */
    public PageResult<Station> page(long pageNum, long pageSize, String stationName, Integer status) {
        Page<Station> page = stationMapper.selectPage(new Page<>(pageNum, pageSize),
                Wrappers.<Station>lambdaQuery()
                        .like(StringUtils.hasText(stationName), Station::getStationName, stationName)
                        .eq(status != null, Station::getStatus, status)
                        .orderByAsc(Station::getId));

        // 为当前页的每条记录补充统计值（一次查询取回全部驿站统计，再匹配）
        List<Station> stats = stationMapper.selectStationWithStat();
        page.getRecords().forEach(station -> stats.stream()
                .filter(s -> s.getId().equals(station.getId()))
                .findFirst()
                .ifPresent(s -> {
                    station.setShelfCount(s.getShelfCount());
                    station.setUsedCount(s.getUsedCount());
                }));
        return PageResult.of(page);
    }

    /**
     * 新增驿站。
     */
    public Long create(StationSaveDTO dto) {
        Long count = stationMapper.selectCount(Wrappers.<Station>lambdaQuery()
                .eq(Station::getStationCode, dto.getStationCode()));
        if (count != null && count > 0) {
            throw new BusinessException("该驿站编号已存在");
        }
        Station station = new Station();
        copyProperties(station, dto);
        stationMapper.insert(station);
        log.info("新增驿站 [{}]", dto.getStationName());
        return station.getId();
    }

    /**
     * 编辑驿站。
     */
    public void update(Long id, StationSaveDTO dto) {
        if (stationMapper.selectById(id) == null) {
            throw new BusinessException("驿站不存在");
        }
        Station station = new Station();
        station.setId(id);
        copyProperties(station, dto);
        stationMapper.updateById(station);
    }

    /**
     * 删除驿站。存在关联员工或在库快件时禁止删除。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Station station = stationMapper.selectById(id);
        if (station == null) {
            throw new BusinessException("驿站不存在");
        }
        int userCount = userMapper.countByStationId(id);
        if (userCount > 0) {
            throw new BusinessException("该驿站下仍有 " + userCount + " 名员工，无法删除");
        }
        int parcelCount = stationMapper.countInStoreParcel(id);
        if (parcelCount > 0) {
            throw new BusinessException("该驿站仍有 " + parcelCount + " 件在库快件，无法删除");
        }
        stationMapper.deleteById(id);
        log.info("删除驿站 [{}]", station.getStationName());
    }

    private void copyProperties(Station station, StationSaveDTO dto) {
        station.setStationCode(dto.getStationCode());
        station.setStationName(dto.getStationName());
        station.setAddress(dto.getAddress());
        station.setContactPhone(dto.getContactPhone());
        station.setManagerName(dto.getManagerName());
        station.setBusinessHours(dto.getBusinessHours());
        station.setCapacity(dto.getCapacity());
        station.setStatus(dto.getStatus());
    }
}
