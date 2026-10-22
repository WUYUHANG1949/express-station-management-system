/**
 * 公开接口（无需登录）
 * 对应后端 PublicController：/api/public/**
 *
 * 设计前提：**收件人不需要注册账号**。系统只保留「驿站员工」与「系统管理员」两类
 * 登录角色，收件人作为外部服务对象，通过本站点自助查询取件码。
 *
 * 注意：该模块的请求不需要 token，因此不经过登录态判断；
 * 但依然复用统一的 axios 封装，以便享受统一的错误提示与响应解包。
 */
import request from '@/utils/request'

/**
 * 自助查询取件码（一个入参，后端按格式自动识别）
 * GET /api/public/pickup-query?keyword=...
 *
 * 支持的三种入参：
 *   - 11 位手机号（1[3-9] 开头）：返回该号码名下所有待取件，最多 20 条
 *   - 8 位纯数字：取件码，返回对应的一件
 *   - 字母前缀 + 数字：运单号（如 SF1234567890123），返回对应的一件
 *
 * 返回：仍在驿站（在库待取 / 派送中）的快件列表；
 * 非手机号查询时后端会把收件人手机号打码（139****0001）。
 *
 * @param {string} keyword 手机号 / 取件码 / 运单号
 */
export function queryPickupByKeyword(keyword) {
  return request({
    url: '/public/pickup-query',
    method: 'get',
    params: { keyword }
  })
}
