import { defineStore } from 'pinia'
import { login as loginApi, logout as logoutApi, getMe, getMenus } from '@/api/auth'
import { TOKEN_KEY } from '@/utils/request'

/** 用户信息本地缓存 key（仅做刷新时首屏兜底，最终以 /auth/me 为准） */
const USER_KEY = 'es_user'

/**
 * 读取本地缓存的用户信息
 * @returns {Object|null}
 */
function readCacheUser() {
  try {
    const raw = localStorage.getItem(USER_KEY)
    return raw ? JSON.parse(raw) : null
  } catch (e) {
    return null
  }
}

/**
 * 用户状态仓库
 * 负责：token 持久化、用户信息、权限集合、菜单树；提供 hasPerm 权限判断
 */
export const useUserStore = defineStore('user', {
  state: () => {
    const cached = readCacheUser()
    return {
      /** 登录令牌，持久化到 localStorage（key: es_token） */
      token: localStorage.getItem(TOKEN_KEY) || '',
      /** 用户信息（id/username/realName/phone/avatar/stationId/stationName/roles/permissions） */
      userInfo: cached || null,
      /** 权限标识集合，加速 hasPerm 判断 */
      permissions: cached && Array.isArray(cached.permissions) ? cached.permissions : [],
      /** 角色编码集合 */
      roles: cached && Array.isArray(cached.roles) ? cached.roles : [],
      /** 后端返回的菜单树 */
      menus: [],
      /** 是否已从后端拉取过用户信息（路由守卫据此避免重复请求） */
      loaded: false
    }
  },

  getters: {
    /** 是否已登录 */
    isLogin: (state) => !!state.token,
    /** 当前用户名 */
    username: (state) => (state.userInfo ? state.userInfo.username : ''),
    /** 当前用户真实姓名 */
    realName: (state) => (state.userInfo ? state.userInfo.realName || state.userInfo.username : ''),
    /** 当前用户所属驿站 id */
    stationId: (state) => (state.userInfo ? state.userInfo.stationId : null),
    /** 当前用户所属驿站名称 */
    stationName: (state) => (state.userInfo ? state.userInfo.stationName : ''),
    /** 是否系统管理员 */
    isAdmin: (state) => (state.roles || []).includes('ADMIN'),
    /** 头像地址 */
    avatar: (state) => (state.userInfo ? state.userInfo.avatar : '')
  },

  actions: {
    /**
     * 登录
     * @param {{username: string, password: string}} form
     * @returns {Promise<Object>} 后端返回的 { token, userInfo, ... }
     */
    async login(form) {
      const data = await loginApi(form)
      const token = data && data.token ? data.token : ''
      this.token = token
      localStorage.setItem(TOKEN_KEY, token)
      if (data && data.userInfo) {
        this.setUserInfo(data.userInfo)
      }
      this.loaded = true
      return data
    },

    /**
     * 写入用户信息与权限集合，并同步到 localStorage
     * @param {Object} info
     */
    setUserInfo(info) {
      this.userInfo = info || null
      this.permissions = info && Array.isArray(info.permissions) ? info.permissions : []
      this.roles = info && Array.isArray(info.roles) ? info.roles : []
      try {
        localStorage.setItem(USER_KEY, JSON.stringify(info || null))
      } catch (e) {
        // 忽略本地存储写入异常（例如隐私模式）
      }
    },

    /**
     * 拉取当前登录用户信息（/auth/me），用于刷新页面后恢复登录态
     * @param {boolean} force 是否强制重新请求
     */
    async fetchUserInfo(force = false) {
      if (this.loaded && !force) return this.userInfo
      const info = await getMe()
      this.setUserInfo(info)
      this.loaded = true
      return info
    },

    /**
     * 拉取当前用户菜单树（/auth/menus），供左侧菜单动态渲染
     * @param {boolean} force 是否强制重新请求
     * @returns {Promise<Array>}
     */
    async fetchMenus(force = false) {
      if (this.menus.length && !force) return this.menus
      const list = await getMenus()
      this.menus = Array.isArray(list) ? list : []
      return this.menus
    },

    /**
     * 判断是否拥有某个权限标识（契约 1.4 的 perm_code）
     * ADMIN 角色默认拥有全部权限，避免后端权限数据异常导致管理员被拦
     * @param {string|Array<string>} perm 单个权限标识或数组（数组为「任一满足」）
     * @returns {boolean}
     */
    hasPerm(perm) {
      if (!perm) return true
      if (this.isAdmin) return true
      const perms = this.permissions || []
      if (Array.isArray(perm)) {
        if (!perm.length) return true
        return perm.some((p) => perms.includes(p))
      }
      return perms.includes(perm)
    },

    /** 退出登录：调用后端接口并清空本地状态（接口失败也要清空，保证能退出） */
    async logout() {
      try {
        if (this.token) await logoutApi()
      } catch (e) {
        // 忽略退出接口异常，本地状态必须清理
      } finally {
        this.resetState()
      }
    },

    /** 清空登录态 */
    resetState() {
      this.token = ''
      this.userInfo = null
      this.permissions = []
      this.roles = []
      this.menus = []
      this.loaded = false
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
    }
  }
})
