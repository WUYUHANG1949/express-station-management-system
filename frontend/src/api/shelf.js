/**
 * 货位模块接口（契约 7.5）
 */
import request from '@/utils/request'

/** 该驿站全部货位：GET /api/shelves/list?stationId= */
export function listShelves(stationId) {
  return request({
    url: '/shelves/list',
    method: 'get',
    params: { stationId }
  })
}

/** 仅剩余容量 > 0 的可用货位：GET /api/shelves/available?stationId= */
export function listAvailableShelves(stationId) {
  return request({
    url: '/shelves/available',
    method: 'get',
    params: { stationId }
  })
}

/** 货位分页：GET /api/shelves/page */
export function pageShelves(params) {
  return request({
    url: '/shelves/page',
    method: 'get',
    params
  })
}

/** 新增货位：POST /api/shelves */
export function createShelf(data) {
  return request({
    url: '/shelves',
    method: 'post',
    data
  })
}

/** 编辑货位：PUT /api/shelves/{id} */
export function updateShelf(id, data) {
  return request({
    url: `/shelves/${id}`,
    method: 'put',
    data
  })
}

/** 删除货位：DELETE /api/shelves/{id}（有在库快件时后端禁止删除） */
export function deleteShelf(id) {
  return request({
    url: `/shelves/${id}`,
    method: 'delete'
  })
}
