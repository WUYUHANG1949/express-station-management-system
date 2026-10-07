/**
 * 认证模块接口（契约第 2 节）
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

/** 注册：POST /api/auth/register（无需登录，默认分配 USER 角色） */
export function register(data) {
  return request({
    url: '/auth/register',
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
