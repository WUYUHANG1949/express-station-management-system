/**
 * 用户管理模块接口（契约 7.1）
 * 说明：/users/profile 与 /users/self/password 为「本人」操作，登录即可调用
 */
import request from '@/utils/request'

/** 用户分页查询：GET /api/users/page（ADMIN） */
export function pageUsers(params) {
  return request({
    url: '/users/page',
    method: 'get',
    params
  })
}

/**
 * 账号统计：GET /api/users/stats（ADMIN）
 * 返回 { total, adminCount, staffCount, userCount, enabledCount, disabledCount, noStationCount }
 * 一条 SQL 取回全部计数，供页面顶部统计卡片使用
 */
export function getUserStats() {
  return request({
    url: '/users/stats',
    method: 'get'
  })
}

/** 新增用户：POST /api/users，可指定 roleIds、stationId */
export function createUser(data) {
  return request({
    url: '/users',
    method: 'post',
    data
  })
}

/** 编辑用户：PUT /api/users/{id} */
export function updateUser(id, data) {
  return request({
    url: `/users/${id}`,
    method: 'put',
    data
  })
}

/** 删除用户（逻辑删除）：DELETE /api/users/{id} */
export function deleteUser(id) {
  return request({
    url: `/users/${id}`,
    method: 'delete'
  })
}

/** 启用/禁用用户：PUT /api/users/{id}/status?status=0|1 */
export function updateUserStatus(id, status) {
  return request({
    url: `/users/${id}/status`,
    method: 'put',
    params: { status }
  })
}

/** 重置指定用户密码：PUT /api/users/{id}/password?password=123456 */
export function resetUserPassword(id, password) {
  return request({
    url: `/users/${id}/password`,
    method: 'put',
    params: { password }
  })
}

/** 修改本人资料：PUT /api/users/profile */
export function updateProfile(data) {
  return request({
    url: '/users/profile',
    method: 'put',
    data
  })
}

/** 修改本人密码：PUT /api/users/self/password，body { oldPassword, newPassword } */
export function updateSelfPassword(data) {
  return request({
    url: '/users/self/password',
    method: 'put',
    data
  })
}
