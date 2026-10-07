import axios from 'axios'
import { ElMessage } from 'element-plus'

/**
 * axios 统一封装
 * 职责：
 * 1. 请求拦截器：注入 Authorization: Bearer <token>（token 存于 localStorage，key 为 es_token）
 * 2. 响应拦截器：统一解包后端响应体 { code, message, data }，直接返回 data
 * 3. 业务异常（code=1001/400/404/500）与鉴权异常（401/403）统一提示
 * 4. 401 时清除本地登录态并跳转登录页
 */

export const TOKEN_KEY = 'es_token'

/** 401 只处理一次，避免并发请求重复弹窗与重复跳转 */
let unauthorizedHandled = false

const service = axios.create({
  baseURL: '/api',
  timeout: 15000
})

/* ------------------------------------------------------------------
 * 请求拦截器
 * ---------------------------------------------------------------- */
service.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem(TOKEN_KEY)
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

/**
 * 清空本地登录信息并跳转登录页（保留原访问地址，便于登录后回跳）
 * 采用 location.replace 而不是 router.push，避免在拦截器中引入 router 循环依赖
 */
function redirectToLogin() {
  if (unauthorizedHandled) return
  unauthorizedHandled = true
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem('es_user')
  setTimeout(() => {
    unauthorizedHandled = false
  }, 1500)
  const { pathname, search, hash } = window.location
  const current = `${pathname}${search}${hash}`
  if (current.startsWith('/login')) return
  const redirect = encodeURIComponent(current)
  window.location.replace(`/login?redirect=${redirect}`)
  // 让单页应用内的 Pinia 状态同步失效（页面已跳转，属于兜底）
  window.setTimeout(() => {
    window.location.reload()
  }, 300)
}

/* ------------------------------------------------------------------
 * 响应拦截器
 * ---------------------------------------------------------------- */
service.interceptors.response.use(
  (response) => {
    // 文件流（导出台账）直接返回原始响应，交由调用方处理
    if (response.config && response.config.responseType === 'blob') {
      return response
    }

    const res = response.data

    // 非标准响应体（例如后端直接返回字符串），原样返回
    if (!res || typeof res !== 'object' || res.code === undefined) {
      return res
    }

    if (res.code === 200) {
      return res.data
    }

    // 401：未登录或 Token 失效
    if (res.code === 401) {
      ElMessage.error(res.message || '登录已过期，请重新登录')
      redirectToLogin()
      return Promise.reject(new Error(res.message || '登录已过期，请重新登录'))
    }

    // 403：已登录但无权限
    if (res.code === 403) {
      ElMessage.error(res.message || '无操作权限，请联系管理员')
      return Promise.reject(new Error(res.message || '无操作权限，请联系管理员'))
    }

    // 其余业务异常（1001 / 400 / 404 / 500）统一弹提示
    ElMessage.error(res.message || '操作失败，请稍后重试')
    return Promise.reject(new Error(res.message || '操作失败，请稍后重试'))
  },
  (error) => {
    // 网络层异常处理
    let message = '网络异常，请检查网络后重试'
    const status = error.response && error.response.status
    const serverMessage = error.response && error.response.data && error.response.data.message

    if (status === 401) {
      message = serverMessage || '登录已过期，请重新登录'
      ElMessage.error(message)
      redirectToLogin()
      return Promise.reject(error)
    }
    if (status === 403) {
      message = serverMessage || '无操作权限，请联系管理员'
    } else if (status === 404) {
      message = serverMessage || '请求的资源不存在'
    } else if (status === 500) {
      message = serverMessage || '服务器内部错误，请联系管理员'
    } else if (error.code === 'ECONNABORTED' || String(error.message).includes('timeout')) {
      message = '请求超时，请稍后重试'
    } else if (serverMessage) {
      message = serverMessage
    }

    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default service
