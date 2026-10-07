/**
 * 文件下载工具
 * 用于快件台账导出（后端返回 xlsx 文件流）
 */

/**
 * 解析后端 Content-Disposition 中的文件名
 * @param {string} disposition 响应头内容
 * @param {string} fallback 兜底文件名
 * @returns {string}
 */
function parseFileName(disposition, fallback) {
  if (!disposition) return fallback
  // 优先取 filename*=UTF-8''xxx 形式
  const utf8Match = /filename\*=UTF-8''([^;]+)/i.exec(disposition)
  if (utf8Match && utf8Match[1]) {
    try {
      return decodeURIComponent(utf8Match[1])
    } catch (e) {
      return utf8Match[1]
    }
  }
  const match = /filename="?([^";]+)"?/i.exec(disposition)
  if (match && match[1]) {
    try {
      return decodeURIComponent(match[1])
    } catch (e) {
      return match[1]
    }
  }
  return fallback
}

/**
 * 将后端返回的 blob 响应下载为本地文件
 * @param {Object} response axios 原始响应（responseType: 'blob'）
 * @param {string} fallbackName 后端未返回文件名时使用的兜底名
 */
export function downloadBlob(response, fallbackName = 'download.xlsx') {
  const blobData = response.data instanceof Blob ? response.data : new Blob([response.data])
  const disposition =
    (response.headers && (response.headers['content-disposition'] || response.headers['Content-Disposition'])) || ''
  const fileName = parseFileName(disposition, fallbackName)
  const url = window.URL.createObjectURL(blobData)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}

/**
 * 对象转 URL query 字符串（过滤空值），用于 window.open 下载兜底方案
 * @param {Object} params
 * @returns {string}
 */
export function toQueryString(params = {}) {
  const parts = []
  Object.keys(params).forEach((key) => {
    const value = params[key]
    if (value === null || value === undefined || value === '') return
    parts.push(`${encodeURIComponent(key)}=${encodeURIComponent(value)}`)
  })
  return parts.join('&')
}
