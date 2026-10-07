/**
 * 快件模块接口（契约第 3 节）
 */
import request from '@/utils/request'

/** 收件登记（入库）：POST /api/parcels/in-store，成功后返回含 pickupCode 的快件对象 */
export function inStore(data) {
  return request({
    url: '/parcels/in-store',
    method: 'post',
    data
  })
}

/**
 * 按取件码 / 运单号 / 手机号模糊查询（取件核销页使用）
 * GET /api/parcels/query?keyword=&stationId=
 */
export function queryParcels(params) {
  return request({
    url: '/parcels/query',
    method: 'get',
    params
  })
}

/** 取件核销（出库）：POST /api/parcels/pickup */
export function pickupParcel(data) {
  return request({
    url: '/parcels/pickup',
    method: 'post',
    data
  })
}

/** 快件分页条件查询：GET /api/parcels/page */
export function pageParcels(params) {
  return request({
    url: '/parcels/page',
    method: 'get',
    params
  })
}

/** 快件详情：GET /api/parcels/{id} */
export function getParcel(id) {
  return request({
    url: `/parcels/${id}`,
    method: 'get'
  })
}

/** 快件轨迹：GET /api/parcels/{id}/traces */
export function getParcelTraces(id) {
  return request({
    url: `/parcels/${id}/traces`,
    method: 'get'
  })
}

/** 编辑快件：PUT /api/parcels/{id}（仅 receiverName/receiverPhone/parcelType/shelfId/overdueDays/remark） */
export function updateParcel(id, data) {
  return request({
    url: `/parcels/${id}`,
    method: 'put',
    data
  })
}

/** 删除快件（逻辑删除并释放货位）：DELETE /api/parcels/{id} */
export function deleteParcel(id) {
  return request({
    url: `/parcels/${id}`,
    method: 'delete'
  })
}

/** 派送：PUT /api/parcels/{id}/deliver，状态 IN_STORE → DELIVERING */
export function deliverParcel(id) {
  return request({
    url: `/parcels/${id}/deliver`,
    method: 'put'
  })
}

/** 逾期费用试算：GET /api/parcels/{id}/overdue-fee → { storageDays, overdueDays, overdueFee } */
export function getOverdueFee(id) {
  return request({
    url: `/parcels/${id}/overdue-fee`,
    method: 'get'
  })
}

/**
 * 导出台账（返回 xlsx 文件流）
 * 使用 blob 响应类型，由调用方触发浏览器下载
 * GET /api/parcels/export?<与分页查询相同的筛选参数>
 */
export function exportParcels(params) {
  return request({
    url: '/parcels/export',
    method: 'get',
    params,
    responseType: 'blob'
  })
}
