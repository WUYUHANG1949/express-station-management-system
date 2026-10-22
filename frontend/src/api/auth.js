/**
 * 认证模块接口（契约第 2 节）
 *
 * 说明：自助注册（POST /api/auth/register）已下线 —— 收件人不需要账号，
 * 改用免登录自助查询；系统内账号由管理员在「用户管理」中创建。
 * 因此本模块不再导出 register。
 */
import request from '@/utils/request'

/** 登录：POST /api/auth/login → { token, tokenType, expiresIn, userInfo } */
export function login(data) {
  return request({
    url: '/auth/login',
    method: 'post',
    data
  })
}

/** 退出登录：POST /api/auth/logout */
export function logout() {
  return request({
    url: '/auth/logout',
    method: 'post'
  })
}

/** 获取当前登录用户信息：GET /api/auth/me */
export function getMe() {
  return request({
    url: '/auth/me',
    method: 'get'
  })
}

/** 获取当前用户菜单树：GET /api/auth/menus */
export function getMenus() {
  return request({
    url: '/auth/menus',
    method: 'get'
  })
}
