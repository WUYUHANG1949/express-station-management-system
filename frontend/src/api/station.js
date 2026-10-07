/**
 * 驿站模块接口（契约 7.4）
 */
import request from '@/utils/request'

/** 全部启用驿站：GET /api/stations/list（所有登录用户可读，用于下拉框） */
export function listStations() {
  return request({
    url: '/stations/list',
    method: 'get'
  })
}

/** 驿站分页：GET /api/stations/page */
export function pageStations(params) {
  return request({
    url: '/stations/page',
    method: 'get',
    params
  })
}

/** 新增驿站：POST /api/stations */
export function createStation(data) {
  return request({
    url: '/stations',
    method: 'post',
    data
  })
}

/** 编辑驿站：PUT /api/stations/{id} */
export function updateStation(id, data) {
  return request({
    url: `/stations/${id}`,
    method: 'put',
    data
  })
}

/** 删除驿站：DELETE /api/stations/{id} */
export function deleteStation(id) {
  return request({
    url: `/stations/${id}`,
    method: 'delete'
  })
}
