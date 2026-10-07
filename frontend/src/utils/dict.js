/**
 * 全局枚举字典与中文名映射
 * 与《前后端接口契约》v1.0 严格对应，任何枚举调整必须同步本文件与契约文档。
 * 每个字典项结构：{ value: 枚举值, label: 中文名, type: Element Plus Tag 主题色 }
 */

/* ------------------------------------------------------------------
 * 快件状态（契约 3.4）
 * ---------------------------------------------------------------- */
export const PARCEL_STATUS = [
  { value: 'IN_STORE', label: '在库待取', type: 'primary' },
  { value: 'PICKED_UP', label: '已取件', type: 'success' },
  { value: 'DELIVERING', label: '派送中', type: 'warning' },
  { value: 'EXCEPTION', label: '异常件', type: 'danger' },
  { value: 'RETURNED', label: '已退回', type: 'info' }
]
/** 快件状态 -> 中文名，用于后端未返回 statusName 时的兜底 */
export const PARCEL_STATUS_NAME = {
  IN_STORE: '在库待取',
  PICKED_UP: '已取件',
  DELIVERING: '派送中',
  EXCEPTION: '异常件',
  RETURNED: '已退回'
}

/* ------------------------------------------------------------------
 * 快件类型（契约 3.4）
 * ---------------------------------------------------------------- */
export const PARCEL_TYPE = [
  { value: 'NORMAL', label: '普通件', type: 'primary' },
  { value: 'SMALL', label: '小件', type: 'success' },
  { value: 'LARGE', label: '大件', type: 'warning' },
  { value: 'FRAGILE', label: '易碎品', type: 'danger' },
  { value: 'DOCUMENT', label: '文件', type: 'info' },
  { value: 'COLD', label: '生鲜', type: 'success' }
]
export const PARCEL_TYPE_NAME = {
  NORMAL: '普通件',
  SMALL: '小件',
  LARGE: '大件',
  FRAGILE: '易碎品',
  DOCUMENT: '文件',
  COLD: '生鲜'
}

/* ------------------------------------------------------------------
 * 快递公司（收件登记 / 寄件登记下拉选项）
 * ---------------------------------------------------------------- */
export const EXPRESS_COMPANIES = [
  '顺丰速运',
  '京东物流',
  '中通快递',
  '圆通速递',
  '申通快递',
  '韵达快递',
  '邮政EMS',
  '极兔速递'
]

/* ------------------------------------------------------------------
 * 寄件单状态（契约 4.2）
 * ---------------------------------------------------------------- */
export const SHIP_STATUS = [
  { value: 'PENDING', label: '待揽收', type: 'warning' },
  { value: 'ACCEPTED', label: '已揽收', type: 'primary' },
  { value: 'SHIPPED', label: '已发出', type: 'success' },
  { value: 'CANCELLED', label: '已取消', type: 'info' }
]
export const SHIP_STATUS_NAME = {
  PENDING: '待揽收',
  ACCEPTED: '已揽收',
  SHIPPED: '已发出',
  CANCELLED: '已取消'
}

/* ------------------------------------------------------------------
 * 异常类型 / 处理状态（契约 5）
 * ---------------------------------------------------------------- */
export const EXCEPTION_TYPE = [
  { value: 'DAMAGED', label: '破损', type: 'danger' },
  { value: 'LOST', label: '丢失', type: 'danger' },
  { value: 'ADDRESS_ERROR', label: '地址错误', type: 'warning' },
  { value: 'REFUSED', label: '拒收', type: 'info' },
  { value: 'TIMEOUT', label: '长期未取', type: 'warning' },
  { value: 'OTHER', label: '其他', type: 'info' }
]
export const EXCEPTION_TYPE_NAME = {
  DAMAGED: '破损',
  LOST: '丢失',
  ADDRESS_ERROR: '地址错误',
  REFUSED: '拒收',
  TIMEOUT: '长期未取',
  OTHER: '其他'
}

export const HANDLE_STATUS = [
  { value: 'PENDING', label: '待处理', type: 'danger' },
  { value: 'HANDLING', label: '处理中', type: 'warning' },
  { value: 'RESOLVED', label: '已解决', type: 'success' }
]
export const HANDLE_STATUS_NAME = {
  PENDING: '待处理',
  HANDLING: '处理中',
  RESOLVED: '已解决'
}

/* ------------------------------------------------------------------
 * 取件方式 / 核验方式（契约 3.3）
 * 注意：送货上门的枚举值是 DELIVERY（与后端 PickupDict.PickupType 一致），
 * 早期版本误写为 DOOR，会导致按字典渲染时匹配不到后端返回值，已修正。
 * ---------------------------------------------------------------- */
export const PICKUP_TYPE = [
  { value: 'SELF', label: '本人自取', type: 'primary' },
  { value: 'AGENT', label: '他人代取', type: 'warning' },
  { value: 'DELIVERY', label: '送货上门', type: 'success' }
]
export const PICKUP_TYPE_NAME = { SELF: '本人自取', AGENT: '他人代取', DELIVERY: '送货上门' }

export const VERIFY_TYPE = [
  { value: 'CODE', label: '取件码核验', type: 'primary' },
  { value: 'PHONE', label: '手机号核验', type: 'success' },
  { value: 'ID_CARD', label: '身份证核验', type: 'warning' }
]
export const VERIFY_TYPE_NAME = { CODE: '取件码核验', PHONE: '手机号核验', ID_CARD: '身份证核验' }

/* ------------------------------------------------------------------
 * 快件轨迹操作类型 -> 中文（契约 3.7）
 * ---------------------------------------------------------------- */
export const TRACE_TYPE_NAME = {
  IN_STORE: '入库登记',
  PICKUP: '取件出库',
  DELIVER: '派送',
  EXCEPTION: '异常登记',
  HANDLE: '异常处理',
  EDIT: '信息修改',
  RETURN: '退回',
  DELETE: '删除'
}

/* ------------------------------------------------------------------
 * 用户状态 / 通用启用停用
 * ---------------------------------------------------------------- */
export const STATUS_OPTIONS = [
  { value: 1, label: '启用', type: 'success' },
  { value: 0, label: '停用', type: 'info' }
]

export const GENDER_OPTIONS = [
  { value: 1, label: '男' },
  { value: 0, label: '女' },
  { value: 2, label: '未知' }
]
export const GENDER_NAME = { 1: '男', 0: '女', 2: '未知' }

/* ------------------------------------------------------------------
 * 角色编码 -> 中文（契约 1.4）
 * ---------------------------------------------------------------- */
export const ROLE_CODE_NAME = {
  ADMIN: '系统管理员',
  STAFF: '驿站员工',
  USER: '普通用户'
}

/* ------------------------------------------------------------------
 * v1.1 新增字典
 * ---------------------------------------------------------------- */

/** 通知类型（后端 NotifyDict.NotifyType） */
export const NOTIFY_TYPE = [
  { value: 'IN_STORE', label: '到件通知', type: 'primary' },
  { value: 'OVERDUE', label: '逾期催取', type: 'warning' },
  { value: 'PICKUP_DONE', label: '取件确认', type: 'success' },
  { value: 'EXCEPTION', label: '异常通知', type: 'danger' }
]
export const NOTIFY_TYPE_NAME = {
  IN_STORE: '到件通知',
  OVERDUE: '逾期催取',
  PICKUP_DONE: '取件确认',
  EXCEPTION: '异常通知'
}

/** 通知渠道（后端 NotifyDict.Channel） */
export const NOTIFY_CHANNEL = [
  { value: 'SMS', label: '短信', type: 'primary' },
  { value: 'APP', label: '站内通知', type: 'success' },
  { value: 'PHONE', label: '电话', type: 'warning' }
]
export const NOTIFY_CHANNEL_NAME = { SMS: '短信', APP: '站内通知', PHONE: '电话' }

/** 通知发送结果（后端 NotifyDict.SendStatus） */
export const SEND_STATUS = [
  { value: 'SUCCESS', label: '发送成功', type: 'success' },
  { value: 'FAILED', label: '发送失败', type: 'danger' }
]
export const SEND_STATUS_NAME = { SUCCESS: '发送成功', FAILED: '发送失败' }

/**
 * 货位占用程度（后端 ShelfMapVO.level）
 * 用于货位地图格子上色与图例说明
 */
export const SHELF_LEVEL = [
  { value: 'EMPTY', label: '空闲', type: 'info', color: '#0fb98f' },
  { value: 'NORMAL', label: '正常', type: 'primary', color: '#1a6dff' },
  { value: 'BUSY', label: '较满', type: 'warning', color: '#f59f00' },
  { value: 'FULL', label: '已满', type: 'danger', color: '#f04438' }
]
export const SHELF_LEVEL_NAME = { EMPTY: '空闲', NORMAL: '正常', BUSY: '较满', FULL: '已满' }

/* ------------------------------------------------------------------
 * 通用工具函数
 * ---------------------------------------------------------------- */

/**
 * 根据枚举值与字典数组取中文名
 * @param {Array} dict 字典数组，如 PARCEL_STATUS
 * @param {string|number} value 枚举值
 * @param {string} fallback 未匹配时的兜底文案
 * @returns {string}
 */
export function dictLabel(dict, value, fallback = '-') {
  if (value === null || value === undefined || value === '') return fallback
  const hit = (dict || []).find((item) => String(item.value) === String(value))
  return hit ? hit.label : fallback
}

/**
 * 根据枚举值与字典数组取 Element Plus Tag 的 type
 * @param {Array} dict 字典数组
 * @param {string|number} value 枚举值
 * @returns {string} 默认 info
 */
export function dictType(dict, value) {
  const hit = (dict || []).find((item) => String(item.value) === String(value))
  return hit && hit.type ? hit.type : 'info'
}

/**
 * 把枚举值数组转换为中文名字符串，用于 detail 展示
 * @param {Array} dict
 * @param {Array} values
 * @returns {string}
 */
export function dictLabels(dict, values) {
  if (!Array.isArray(values)) return '-'
  return values.map((v) => dictLabel(dict, v)).join('、') || '-'
}
