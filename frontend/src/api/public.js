/**
 * 公开接口（v1.1 新增，无需登录）
 * 对应后端 PublicController：/api/public/**
 *
 * 注意：该模块的请求不需要 token，因此不经过登录态判断；
 * 但依然复用统一的 axios 封装，以便享受统一的错误提示与响应解包。
 */
import request from '@/utils/request'

/**
 * 按手机号自助查询取件码
 * GET /api/public/pickup-query?phone=13900000001
 * 只返回仍在驿站（在库待取 / 派送中）的快件，最多 20 条
 */
export function queryPickupByPhone(phone) {
  return request({
    url: '/public/pickup-query',
    method: 'get',
    params: { phone }
  })
}
