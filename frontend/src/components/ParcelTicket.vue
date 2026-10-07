<template>
  <!--
    取件小票（v1.1）
    用途：真实驿站取件时打印一张小票交给客户，客户凭上面的取件码取件。
    说明：
    - 组件本身只负责渲染「小票样式」的 DOM（宽 300px、白底黑字、等宽字体、虚线分隔），
      是否显示、放在哪里由调用方决定（列表页把它放在屏幕外，仅用于打印）。
    - 打印通过 print() 方法触发，内部把 .ticket 的 HTML 取出写入一个隐藏 iframe，
      只打印小票内容，避免把侧边栏 / 表格一并打印出来。
  -->
  <div class="parcel-ticket">
    <div ref="ticketRef" class="ticket">
      <!-- 抬头：系统名 + 驿站名 -->
      <div class="ticket__head">
        <div class="ticket__system">快件收发管理系统</div>
        <div class="ticket__station">{{ stationTitle }}</div>
      </div>

      <div class="ticket__dash" />

      <!-- 取件码：整张小票最重要的信息，超大字号居中 -->
      <div class="ticket__code-label">取 件 码</div>
      <div class="ticket__code">{{ pickupCode }}</div>

      <div class="ticket__dash" />

      <!-- 明细 -->
      <div class="ticket__row">
        <span class="ticket__k">运单号</span>
        <span class="ticket__v">{{ text(parcel && parcel.waybillNo) }}</span>
      </div>
      <div class="ticket__row">
        <span class="ticket__k">快递公司</span>
        <span class="ticket__v">{{ text(parcel && parcel.expressCompany) }}</span>
      </div>
      <div class="ticket__row">
        <span class="ticket__k">收件人</span>
        <span class="ticket__v">{{ text(parcel && parcel.receiverName) }}</span>
      </div>
      <div class="ticket__row">
        <span class="ticket__k">手机号</span>
        <span class="ticket__v">{{ text(parcel && parcel.receiverPhone) }}</span>
      </div>
      <div class="ticket__row">
        <span class="ticket__k">货位</span>
        <span class="ticket__v">{{ text(parcel && parcel.shelfCode, '未分配') }}</span>
      </div>
      <div class="ticket__row">
        <span class="ticket__k">入库时间</span>
        <span class="ticket__v">{{ formatDateTime(parcel && parcel.inTime) }}</span>
      </div>
      <div class="ticket__row">
        <span class="ticket__k">免费保管</span>
        <span class="ticket__v">{{ formatNumber(parcel && parcel.overdueDays) }} 天</span>
      </div>
      <div v-if="overdueFee > 0" class="ticket__row ticket__row--warn">
        <span class="ticket__k">逾期保管费</span>
        <span class="ticket__v">{{ formatMoney(overdueFee, true) }}</span>
      </div>

      <div class="ticket__dash" />

      <!-- 取件提示 -->
      <div class="ticket__tip">请凭取件码到驿站取件</div>
      <div class="ticket__tip ticket__tip--sub">取件时请出示取件码，并与工作人员核对手机号</div>

      <div class="ticket__dash" />

      <div class="ticket__foot">
        <span data-print-time>{{ printedAt }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * 取件小票组件
 * props：
 *   parcel  快件对象（可为 null，字段缺失时用 '-' 占位）
 *   station 驿站信息对象或驿站名称字符串（可选，缺省时回退到 parcel.stationName）
 * 暴露：print() —— 打印当前小票
 */
import { computed, ref } from 'vue'
import { formatDateTime, formatMoney, formatNumber } from '@/utils/format'

const props = defineProps({
  /** 快件数据（列表行 / 详情对象均可） */
  parcel: { type: Object, default: null },
  /** 驿站信息：对象（取 stationName/name）或直接传名称字符串 */
  station: { type: [Object, String], default: '' }
})

/** 小票根节点（打印时取它的 outerHTML，不含外层包裹容器） */
const ticketRef = ref(null)
/** 打印时间：页面打开时先给一个值，真正打印前会替换为打印那一刻的时间 */
const printedAt = ref(formatDateTime(new Date()))

/** 取件码：后端未返回时用 '-' 兜底 */
const pickupCode = computed(() => {
  const code = props.parcel && props.parcel.pickupCode
  return code ? String(code) : '------'
})

/** 驿站名称：优先用 station 入参，其次用快件自带字段 */
const stationTitle = computed(() => {
  const station = props.station
  if (typeof station === 'string' && station) return station
  if (station && typeof station === 'object') {
    const name = station.stationName || station.name
    if (name) return name
  }
  const fromParcel = props.parcel && props.parcel.stationName
  return fromParcel || '快递驿站'
})

/** 逾期保管费：> 0 时才在小票上出现一行 */
const overdueFee = computed(() => Number(props.parcel && props.parcel.overdueFee) || 0)

/** 空值占位 */
function text(value, fallback = '-') {
  if (value === null || value === undefined || value === '') return fallback
  return value
}

/**
 * 打印用的小票样式
 * 小票会被写入隐藏 iframe，iframe 内没有站点的 scoped 样式，
 * 因此这里单独维护一份纯 CSS 字符串（类名与模板一致）。
 */
const PRINT_CSS = `
  @page { margin: 6mm; }
  * { box-sizing: border-box; }
  html, body { margin: 0; padding: 0; background: #fff; }
  body {
    color: #000;
    font-family: 'Consolas', 'Courier New', 'Microsoft YaHei', monospace;
    -webkit-font-smoothing: antialiased;
  }
  .ticket { width: 300px; padding: 12px 14px 10px; border: 1px solid #000; }
  .ticket__head { text-align: center; }
  .ticket__system { font-size: 13px; font-weight: 700; letter-spacing: 1px; }
  .ticket__station { margin-top: 3px; font-size: 12px; }
  .ticket__dash {
    height: 0;
    margin: 8px 0;
    border-top: 1px dashed #000;
  }
  .ticket__code-label { text-align: center; font-size: 12px; letter-spacing: 4px; }
  .ticket__code {
    margin: 4px 0 2px;
    text-align: center;
    font-size: 34px;
    font-weight: 700;
    letter-spacing: 4px;
    font-family: 'Consolas', 'Courier New', monospace;
    word-break: break-all;
  }
  .ticket__row {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 8px;
    font-size: 12px;
    line-height: 1.7;
  }
  .ticket__k { flex: 0 0 auto; color: #333; }
  .ticket__v { flex: 1 1 auto; text-align: right; word-break: break-all; }
  .ticket__row--warn { font-weight: 700; }
  .ticket__tip { text-align: center; font-size: 12px; line-height: 1.7; }
  .ticket__tip--sub { font-size: 11px; color: #333; }
  .ticket__foot { text-align: center; font-size: 11px; color: #333; }
`

/**
 * 打印小票
 * 实现：把 .ticket 的 HTML 克隆后写入一个隐藏 iframe（附带内联样式），
 * 由 iframe 自己窗口打印，打印结束（afterprint）后移除 iframe。
 * 这样不会打印整个页面（侧边栏、表格、按钮都不会出现）。
 */
function print() {
  if (!ticketRef.value) return

  // 克隆小票 DOM，并把打印时间替换为「此刻」
  const clone = ticketRef.value.cloneNode(true)
  const timeEl = clone.querySelector('[data-print-time]')
  if (timeEl) timeEl.textContent = formatDateTime(new Date())

  const html = `<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="utf-8" />
<title>取件小票</title>
<style>${PRINT_CSS}</style>
</head>
<body>${clone.outerHTML}</body>
</html>`

  // 隐藏 iframe：不占位、不可见，打印完成后移除
  const iframe = document.createElement('iframe')
  iframe.setAttribute('aria-hidden', 'true')
  iframe.setAttribute('title', '取件小票打印')
  iframe.style.position = 'fixed'
  iframe.style.right = '0'
  iframe.style.bottom = '0'
  iframe.style.width = '0'
  iframe.style.height = '0'
  iframe.style.border = '0'
  iframe.style.visibility = 'hidden'
  document.body.appendChild(iframe)

  let removed = false
  /** 移除 iframe（幂等，避免重复触发） */
  const cleanup = () => {
    if (removed) return
    removed = true
    window.setTimeout(() => {
      if (iframe.parentNode) iframe.parentNode.removeChild(iframe)
    }, 300)
  }

  const win = iframe.contentWindow
  if (!win) {
    cleanup()
    return
  }
  const doc = win.document
  doc.open()
  doc.write(html)
  doc.close()

  // 部分浏览器（Safari 等）print() 立即返回，因此以 afterprint 为主，
  // 再用一个较长的兜底定时器保证 iframe 最终一定被回收。
  win.onafterprint = cleanup
  window.setTimeout(cleanup, 30000)

  // doc.write + close 后 DOM 已解析完成，留一帧给浏览器完成排版再唤起打印
  window.setTimeout(() => {
    try {
      win.focus()
      win.print()
    } catch (e) {
      // 打印被拦截 / 浏览器不支持时静默失败，只保证 iframe 被回收
    }
    // Chrome / Edge 的 print() 会阻塞到打印对话框关闭，此时可直接回收
    cleanup()
  }, 60)
}

defineExpose({ print, ticketRef })
</script>

<style scoped>
/* 屏幕上只是为了「有个预览」；列表页会把它放到屏幕外，打印样式见 PRINT_CSS */
.parcel-ticket {
  display: inline-block;
}

.ticket {
  width: 300px;
  padding: 12px 14px 10px;
  background: #fff;
  color: #000;
  border: 1px solid #000;
  font-family: 'Consolas', 'Courier New', 'Microsoft YaHei', monospace;
}

.ticket__head {
  text-align: center;
}

.ticket__system {
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 1px;
}

.ticket__station {
  margin-top: 3px;
  font-size: 12px;
}

.ticket__dash {
  height: 0;
  margin: 8px 0;
  border-top: 1px dashed #000;
}

.ticket__code-label {
  text-align: center;
  font-size: 12px;
  letter-spacing: 4px;
}

.ticket__code {
  margin: 4px 0 2px;
  text-align: center;
  font-size: 34px;
  font-weight: 700;
  letter-spacing: 4px;
  word-break: break-all;
}

.ticket__row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
  font-size: 12px;
  line-height: 1.7;
}

.ticket__k {
  flex: 0 0 auto;
  color: #333;
}

.ticket__v {
  flex: 1 1 auto;
  text-align: right;
  word-break: break-all;
}

.ticket__row--warn {
  font-weight: 700;
}

.ticket__tip {
  text-align: center;
  font-size: 12px;
  line-height: 1.7;
}

.ticket__tip--sub {
  font-size: 11px;
  color: #333;
}

.ticket__foot {
  text-align: center;
  font-size: 11px;
  color: #333;
}
</style>
