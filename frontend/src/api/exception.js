/**
 * 异常件模块接口（契约第 5 节）
 */
import request from '@/utils/request'

/** 异常件分页查询：GET /api/exceptions/page */
export function pageExceptions(params) {
  return request({
    url: '/exceptions/page',
    method: 'get',
    params
  })
}

/** 登记异常：POST /api/exceptions，同时把快件状态置为 EXCEPTION */
export function createException(data) {
  return request({
    url: '/exceptions',
    method: 'post',
    data
  })
}

/**
 * 处理异常：PUT /api/exceptions/{id}/handle
 * body: { handleStatus, handleResult, parcelStatus? }
 */
export function handleException(id, data) {
  return request({
    url: `/exceptions/${id}/handle`,
    method: 'put',
    data
  })
}
