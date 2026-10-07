/**
 * 寄件模块接口（契约第 4 节）
 */
import request from '@/utils/request'

/** 寄件分页查询：GET /api/ship-orders/page */
export function pageShipOrders(params) {
  return request({
    url: '/ship-orders/page',
    method: 'get',
    params
  })
}

/** 寄件单详情：GET /api/ship-orders/{id} */
export function getShipOrder(id) {
  return request({
    url: `/ship-orders/${id}`,
    method: 'get'
  })
}

/** 寄件登记：POST /api/ship-orders，后端自动生成 orderNo，初始状态 PENDING */
export function createShipOrder(data) {
  return request({
    url: '/ship-orders',
    method: 'post',
    data
  })
}

/** 编辑寄件单：PUT /api/ship-orders/{id}（仅 PENDING 状态可改） */
export function updateShipOrder(id, data) {
  return request({
    url: `/ship-orders/${id}`,
    method: 'put',
    data
  })
}

/**
 * 更新寄件状态并回填运单号
 * PUT /api/ship-orders/{id}/status?status=ACCEPTED&waybillNo=SF123
 */
export function updateShipOrderStatus(id, params) {
  return request({
    url: `/ship-orders/${id}/status`,
    method: 'put',
    params
  })
}

/** 删除寄件单（逻辑删除）：DELETE /api/ship-orders/{id} */
export function deleteShipOrder(id) {
  return request({
    url: `/ship-orders/${id}`,
    method: 'delete'
  })
}
