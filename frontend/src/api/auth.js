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

/**
 * 找回密码第一步：申请验证码
 * POST /api/auth/forgot-password/code   body { username, phone }
 *
 * 免登录接口。返回 { message, maskedPhone, expireMinutes, provider, demoCode }
 * 其中 demoCode 只在后端 sms.provider=mock 时返回，用于演示环境自动填充。
 */
export function sendForgotCode(data) {
  return request({
    url: '/auth/forgot-password/code',
    method: 'post',
    data
  })
}

/**
 * 找回密码第二步：校验验证码并重置密码
 * POST /api/auth/forgot-password/reset  body { username, phone, code, newPassword }
 */
export function resetForgotPassword(data) {
  return request({
    url: '/auth/forgot-password/reset',
    method: 'post',
    data
  })
}
