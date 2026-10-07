/**
 * 全局格式化工具
 * 统一项目中金额、日期、数字、手机号等展示格式：
 * - 日期时间：YYYY-MM-DD HH:mm:ss
 * - 日期：YYYY-MM-DD
 * - 金额：保留 2 位小数并加 ¥ 前缀
 */

/**
 * 兼容后端可能返回的多种时间格式，统一转为 Date 对象
 * 支持：Date 对象、时间戳、"2026-10-07 17:39:47"、"2026-10-07T17:39:47"、"2026-10-07"
 * @param {*} value
 * @returns {Date|null}
 */
export function toDate(value) {
  if (value === null || value === undefined || value === '') return null
  if (value instanceof Date) return isNaN(value.getTime()) ? null : value
  if (typeof value === 'number') {
    const d = new Date(value)
    return isNaN(d.getTime()) ? null : d
  }
  const text = String(value).trim()
  if (!text) return null
  // 兼容 iOS/Safari 对 "YYYY-MM-DD HH:mm:ss" 解析异常的问题
  const normalized = text.replace(/-/g, '/').replace('T', ' ').replace(/\.\d+Z?$/, '')
  const d = new Date(normalized)
  if (isNaN(d.getTime())) return null
  return d
}

/** 补零 */
function pad(n) {
  return String(n).padStart(2, '0')
}

/**
 * 格式化为 YYYY-MM-DD HH:mm:ss（项目统一日期时间格式）
 * @param {*} value
 * @param {string} fallback 空值兜底，默认 '-'
 */
export function formatDateTime(value, fallback = '-') {
  const d = toDate(value)
  if (!d) return fallback
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(
    d.getMinutes()
  )}:${pad(d.getSeconds())}`
}

/**
 * 格式化为 YYYY-MM-DD
 */
export function formatDate(value, fallback = '-') {
  const d = toDate(value)
  if (!d) return fallback
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

/**
 * 格式化为 MM-DD（图表 X 轴使用）
 */
export function formatMonthDay(value, fallback = '') {
  const d = toDate(value)
  if (!d) return fallback
  return `${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

/**
 * 金额格式化：保留 2 位小数
 * @param {number|string} value
 * @param {boolean} withSymbol 是否加 ¥ 前缀
 */
export function formatMoney(value, withSymbol = false) {
  const num = Number(value)
  const text = isNaN(num) ? '0.00' : num.toFixed(2)
  return withSymbol ? `¥${text}` : text
}

/**
 * 数值格式化：空值显示 0
 */
export function formatNumber(value, fallback = 0) {
  const num = Number(value)
  return isNaN(num) ? fallback : num
}

/**
 * 百分比格式化，如 4.7 -> "4.7%"
 */
export function formatPercent(value) {
  const num = Number(value)
  return `${(isNaN(num) ? 0 : num).toFixed(1)}%`
}

/**
 * 手机号脱敏展示：139****0001
 */
export function maskPhone(phone) {
  const text = String(phone || '')
  if (text.length !== 11) return text || '-'
  return `${text.slice(0, 3)}****${text.slice(7)}`
}

/**
 * 重量格式化，追加 kg
 */
export function formatWeight(value) {
  const num = Number(value)
  if (isNaN(num)) return '-'
  return `${num.toFixed(2)} kg`
}

/**
 * 将 el-date-picker 的 daterange 数组拆分为 startTime / endTime
 * 后端约定日期参数为 "YYYY-MM-DD"
 * @param {Array} range 形如 ['2026-10-01','2026-10-07']
 * @returns {{startTime: string, endTime: string}}
 */
export function splitDateRange(range) {
  if (!Array.isArray(range) || range.length !== 2) return { startTime: '', endTime: '' }
  return { startTime: formatDate(range[0], ''), endTime: formatDate(range[1], '') }
}
