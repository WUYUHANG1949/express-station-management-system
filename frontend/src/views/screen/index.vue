<template>
  <!--
    数据大屏（全屏深色看板）
    设计目标：答辩现场投屏展示，一屏内呈现驿站的实时运营全貌。
    特点：
      - 深空蓝底 + 青绿光效，玻璃拟态面板
      - 30 秒自动轮询刷新，右上角显示实时时钟与刷新倒计时
      - 一屏聚合 6 类指标 + 3 张图表 + 3 个滚动榜单
      - 支持浏览器全屏（按 F 或点按钮）
  -->
  <div class="screen">
    <!-- 背景装饰光效 -->
    <div class="screen__bg screen__bg--1" />
    <div class="screen__bg screen__bg--2" />
    <div class="screen__grid" />

    <!-- ==================== 顶部标题栏 ==================== -->
    <header class="screen__header">
      <div class="screen__header-side">
        <div class="screen__live">
          <span class="es-live-dot" />
          <span>实时监控中</span>
        </div>
      </div>

      <div class="screen__title">
        <h1>快递驿站快件收发管理平台</h1>
        <p>EXPRESS STATION OPERATION DASHBOARD</p>
      </div>

      <div class="screen__header-side screen__header-side--right">
        <el-select
          v-if="isAdmin"
          v-model="stationId"
          size="small"
          placeholder="全部驿站"
          clearable
          class="screen__station-select"
          @change="loadData"
        >
          <el-option v-for="s in stationList" :key="s.id" :label="s.stationName" :value="s.id" />
        </el-select>
        <div class="screen__clock">
          <div class="screen__time">{{ clock.time }}</div>
          <div class="screen__date">{{ clock.date }}</div>
        </div>
        <el-tooltip content="进入全屏（F）" placement="bottom">
          <el-icon class="screen__icon-btn" @click="toggleFullscreen"><FullScreen /></el-icon>
        </el-tooltip>
        <el-tooltip content="返回系统" placement="bottom">
          <el-icon class="screen__icon-btn" @click="goBack"><Back /></el-icon>
        </el-tooltip>
      </div>
    </header>

    <!-- ==================== 主体 ==================== -->
    <main v-loading="loading" class="screen__body" element-loading-background="rgba(4, 16, 31, 0.6)">
      <!-- 第一行：核心指标 -->
      <section class="screen__metrics">
        <div v-for="m in metricCards" :key="m.label" class="screen__metric" :class="`screen__metric--${m.tone}`">
          <div class="screen__metric-icon">
            <el-icon :size="22"><component :is="m.icon" /></el-icon>
          </div>
          <div class="screen__metric-main">
            <div class="screen__metric-value">
              {{ m.value }}
              <span class="screen__metric-unit">{{ m.unit }}</span>
            </div>
            <div class="screen__metric-label">{{ m.label }}</div>
          </div>
        </div>
      </section>

      <!-- 第二行：趋势图（左宽） + 公司分布 -->
      <section class="screen__row screen__row--charts">
        <div class="screen__panel screen__panel--wide">
          <div class="screen__panel-head">
            <span class="screen__panel-title">近 14 日出入库趋势</span>
            <div class="screen__legend">
              <span class="screen__legend-item"><i style="background: #16d3c8" />入库</span>
              <span class="screen__legend-item"><i style="background: #1a6dff" />取件</span>
            </div>
          </div>
          <ChartBox :option="trendOption" :loading="loading" height="100%" :empty="!trendHasData" empty-text="暂无出入库数据" />
        </div>

        <div class="screen__panel">
          <div class="screen__panel-head">
            <span class="screen__panel-title">快递公司分布</span>
          </div>
          <ChartBox :option="companyOption" :loading="loading" height="100%" :empty="!companyHasData" empty-text="暂无数据" />
        </div>
      </section>

      <!-- 第三行：类型分布 + 驿站排行 + 实时动态 -->
      <section class="screen__row screen__row--bottom">
        <div class="screen__panel">
          <div class="screen__panel-head">
            <span class="screen__panel-title">快件类型分布</span>
          </div>
          <ChartBox :option="typeOption" :loading="loading" height="100%" :empty="!typeHasData" empty-text="暂无数据" />
        </div>

        <div class="screen__panel">
          <div class="screen__panel-head">
            <span class="screen__panel-title">驿站业务量排行</span>
          </div>
          <ChartBox
            :option="rankOption"
            :loading="loading"
            height="100%"
            :empty="!rankHasData"
            empty-text="仅系统管理员可查看"
          />
        </div>

        <div class="screen__panel screen__panel--feed">
          <div class="screen__panel-head">
            <span class="screen__panel-title">实时动态</span>
            <div class="screen__tabs">
              <span
                v-for="tab in feedTabs"
                :key="tab.key"
                class="screen__tab"
                :class="{ 'is-active': feedTab === tab.key }"
                @click="feedTab = tab.key"
              >
                {{ tab.label }}
              </span>
            </div>
          </div>
          <div class="screen__feed">
            <transition-group name="feed" tag="div">
              <div v-for="item in currentFeed" :key="item.key" class="screen__feed-item">
                <div class="screen__feed-code">{{ item.code }}</div>
                <div class="screen__feed-main">
                  <div class="screen__feed-waybill">{{ item.waybillNo }}</div>
                  <div class="screen__feed-meta">{{ item.meta }}</div>
                </div>
                <div class="screen__feed-tag" :class="`screen__feed-tag--${item.tone}`">{{ item.tag }}</div>
              </div>
            </transition-group>
            <el-empty
              v-if="!currentFeed.length"
              description="暂无动态"
              :image-size="60"
              class="screen__feed-empty"
            />
          </div>
        </div>
      </section>

      <!-- 底部：今日通知量 + 更新时间 -->
      <footer class="screen__footer">
        <div class="screen__footer-item">
          <el-icon><Bell /></el-icon>
          <span>今日通知发送 <b>{{ data.overview.notifyToday || 0 }}</b> 条</span>
        </div>
        <div class="screen__footer-item">
          <el-icon><Timer /></el-icon>
          <span>数据更新于 {{ updatedAt || '-' }}（每 30 秒自动刷新，{{ countdown }} 秒后刷新）</span>
        </div>
      </footer>
    </main>
  </div>
</template>

<script setup>
/**
 * 数据大屏
 * 数据来源：GET /api/stats/screen?stationId= （一次请求聚合全部指标，见 ScreenVO）
 */
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Back, Bell, FullScreen, Timer } from '@element-plus/icons-vue'
import ChartBox from '@/components/ChartBox.vue'
import { getScreenData } from '@/api/stats'
import { listStations } from '@/api/station'
import { useUserStore } from '@/stores/user'

defineOptions({ name: 'DataScreen' })

const router = useRouter()
const userStore = useUserStore()

/** 是否系统管理员（决定能否切换驿站、能否看排行） */
const isAdmin = computed(() => userStore.isAdmin)

/** 当前筛选驿站：管理员默认全部，员工固定本驿站 */
const stationId = ref(isAdmin.value ? undefined : userStore.stationId)
const stationList = ref([])

/** 大屏数据（结构见后端 ScreenVO） */
const data = ref({
  overview: {},
  trend: { dates: [], inCounts: [], pickupCounts: [] },
  company: [],
  parcelType: [],
  stationRank: [],
  recentInStore: [],
  recentPickup: [],
  topOverdue: [],
  notifyToday: 0
})

const loading = ref(false)
const updatedAt = ref('')

/* ------------------------------------------------------------------
 * 实时时钟 + 自动刷新倒计时
 * ---------------------------------------------------------------- */
const REFRESH_SECONDS = 30
const clock = reactive({ time: '--:--:--', date: '' })
const countdown = ref(REFRESH_SECONDS)
let clockTimer = null
let refreshTimer = null

function tickClock() {
  const now = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  clock.time = `${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`
  const week = ['日', '一', '二', '三', '四', '五', '六'][now.getDay()]
  clock.date = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} 星期${week}`
}

function tickCountdown() {
  countdown.value -= 1
  if (countdown.value <= 0) {
    countdown.value = REFRESH_SECONDS
    loadData()
  }
}

/* ------------------------------------------------------------------
 * 数据加载
 * ---------------------------------------------------------------- */
async function loadData() {
  loading.value = true
  try {
    const res = await getScreenData(stationId.value)
    data.value = {
      ...data.value,
      ...(res || {}),
      overview: (res && res.overview) || {}
    }
    const now = new Date()
    const pad = (n) => String(n).padStart(2, '0')
    updatedAt.value = `${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`
    countdown.value = REFRESH_SECONDS
  } catch (e) {
    // 错误提示由 axios 拦截器统一处理，这里只保证大屏不崩
  } finally {
    loading.value = false
  }
}

async function loadStations() {
  if (!isAdmin.value) return
  try {
    stationList.value = (await listStations()) || []
  } catch (e) {
    stationList.value = []
  }
}

/* ------------------------------------------------------------------
 * 指标卡
 * ---------------------------------------------------------------- */
const metricCards = computed(() => {
  const o = data.value.overview || {}
  const rate = o.shelfUsage && o.shelfUsage.rate !== undefined ? o.shelfUsage.rate : 0
  return [
    { label: '今日入库', value: o.todayInCount || 0, unit: '件', icon: 'Download', tone: 'teal' },
    { label: '今日取件', value: o.todayPickupCount || 0, unit: '件', icon: 'Finished', tone: 'blue' },
    { label: '当前在库', value: o.inStoreCount || 0, unit: '件', icon: 'Box', tone: 'blue' },
    { label: '派送中', value: o.deliveringCount || 0, unit: '件', icon: 'Van', tone: 'purple' },
    { label: '逾期未取', value: o.overdueCount || 0, unit: '件', icon: 'AlarmClock', tone: 'orange' },
    { label: '异常件', value: o.exceptionCount || 0, unit: '件', icon: 'Warning', tone: 'red' },
    { label: '货位使用率', value: rate, unit: '%', icon: 'Grid', tone: 'teal' },
    { label: '累计快件', value: o.totalParcelCount || 0, unit: '件', icon: 'DataLine', tone: 'gray' }
  ]
})

/* ------------------------------------------------------------------
 * 图表配置（深色主题）
 * ---------------------------------------------------------------- */
const AXIS_COLOR = 'rgba(255, 255, 255, 0.55)'
const SPLIT_COLOR = 'rgba(255, 255, 255, 0.08)'
const CHART_COLORS = ['#16d3c8', '#1a6dff', '#7c4dff', '#f59f00', '#f04438', '#0fb98f', '#22d3ee', '#94a3b8']

const trendHasData = computed(() => {
  const t = data.value.trend || {}
  return (t.inCounts || []).some((v) => Number(v) > 0) || (t.pickupCounts || []).some((v) => Number(v) > 0)
})

const trendOption = computed(() => {
  const t = data.value.trend || { dates: [], inCounts: [], pickupCounts: [] }
  return {
    grid: { top: 24, right: 20, bottom: 24, left: 44 },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(7, 26, 58, 0.92)',
      borderColor: 'rgba(22, 211, 200, 0.4)',
      textStyle: { color: '#fff' }
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: t.dates || [],
      axisLine: { lineStyle: { color: SPLIT_COLOR } },
      axisLabel: { color: AXIS_COLOR, fontSize: 11 },
      axisTick: { show: false }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      axisLine: { show: false },
      axisLabel: { color: AXIS_COLOR, fontSize: 11 },
      splitLine: { lineStyle: { color: SPLIT_COLOR } }
    },
    series: [
      {
        name: '入库',
        type: 'line',
        smooth: true,
        symbolSize: 6,
        data: t.inCounts || [],
        itemStyle: { color: '#16d3c8' },
        lineStyle: { width: 3, shadowColor: 'rgba(22,211,200,0.5)', shadowBlur: 12 },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(22,211,200,0.42)' },
              { offset: 1, color: 'rgba(22,211,200,0.02)' }
            ]
          }
        }
      },
      {
        name: '取件',
        type: 'line',
        smooth: true,
        symbolSize: 6,
        data: t.pickupCounts || [],
        itemStyle: { color: '#1a6dff' },
        lineStyle: { width: 3, shadowColor: 'rgba(26,109,255,0.5)', shadowBlur: 12 },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(26,109,255,0.38)' },
              { offset: 1, color: 'rgba(26,109,255,0.02)' }
            ]
          }
        }
      }
    ]
  }
})

const companyHasData = computed(() => (data.value.company || []).length > 0)

const companyOption = computed(() => ({
  color: CHART_COLORS,
  tooltip: {
    trigger: 'item',
    backgroundColor: 'rgba(7, 26, 58, 0.92)',
    borderColor: 'rgba(22, 211, 200, 0.4)',
    textStyle: { color: '#fff' },
    formatter: '{b}<br/>{c} 件（{d}%）'
  },
  legend: {
    type: 'scroll',
    orient: 'vertical',
    right: 8,
    top: 'center',
    itemWidth: 9,
    itemHeight: 9,
    textStyle: { color: AXIS_COLOR, fontSize: 11 }
  },
  series: [
    {
      type: 'pie',
      radius: ['46%', '70%'],
      center: ['34%', '50%'],
      avoidLabelOverlap: true,
      itemStyle: { borderColor: '#061426', borderWidth: 2 },
      label: { show: false },
      labelLine: { show: false },
      data: (data.value.company || []).map((i) => ({ name: i.name, value: i.value }))
    }
  ]
}))

const typeHasData = computed(() => (data.value.parcelType || []).length > 0)

const typeOption = computed(() => ({
  color: CHART_COLORS,
  tooltip: {
    trigger: 'axis',
    axisPointer: { type: 'shadow' },
    backgroundColor: 'rgba(7, 26, 58, 0.92)',
    borderColor: 'rgba(22, 211, 200, 0.4)',
    textStyle: { color: '#fff' }
  },
  grid: { top: 16, right: 26, bottom: 16, left: 64 },
  xAxis: {
    type: 'value',
    minInterval: 1,
    axisLine: { show: false },
    axisLabel: { color: AXIS_COLOR, fontSize: 11 },
    splitLine: { lineStyle: { color: SPLIT_COLOR } }
  },
  yAxis: {
    type: 'category',
    data: (data.value.parcelType || []).map((i) => i.name),
    axisLine: { lineStyle: { color: SPLIT_COLOR } },
    axisLabel: { color: AXIS_COLOR, fontSize: 11 },
    axisTick: { show: false }
  },
  series: [
    {
      type: 'bar',
      barWidth: 13,
      itemStyle: {
        borderRadius: [0, 6, 6, 0],
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 1,
          y2: 0,
          colorStops: [
            { offset: 0, color: '#1a6dff' },
            { offset: 1, color: '#16d3c8' }
          ]
        }
      },
      label: { show: true, position: 'right', color: '#fff', fontSize: 11 },
      data: (data.value.parcelType || []).map((i) => i.value)
    }
  ]
}))

const rankHasData = computed(() => (data.value.stationRank || []).length > 0)

const rankOption = computed(() => {
  const list = data.value.stationRank || []
  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      backgroundColor: 'rgba(7, 26, 58, 0.92)',
      borderColor: 'rgba(22, 211, 200, 0.4)',
      textStyle: { color: '#fff' }
    },
    legend: {
      top: 0,
      right: 0,
      itemWidth: 9,
      itemHeight: 9,
      textStyle: { color: AXIS_COLOR, fontSize: 11 },
      data: ['收件量', '取件量']
    },
    grid: { top: 28, right: 16, bottom: 8, left: 8, containLabel: true },
    xAxis: {
      type: 'value',
      minInterval: 1,
      axisLine: { show: false },
      axisLabel: { color: AXIS_COLOR, fontSize: 11 },
      splitLine: { lineStyle: { color: SPLIT_COLOR } }
    },
    yAxis: {
      type: 'category',
      data: list.map((i) => i.stationName),
      axisLine: { lineStyle: { color: SPLIT_COLOR } },
      axisLabel: { color: AXIS_COLOR, fontSize: 11 },
      axisTick: { show: false }
    },
    series: [
      {
        name: '收件量',
        type: 'bar',
        barWidth: 11,
        itemStyle: { borderRadius: [0, 5, 5, 0], color: '#16d3c8' },
        data: list.map((i) => i.parcelCount)
      },
      {
        name: '取件量',
        type: 'bar',
        barWidth: 11,
        itemStyle: { borderRadius: [0, 5, 5, 0], color: '#7c4dff' },
        data: list.map((i) => i.pickupCount)
      }
    ]
  }
})

/* ------------------------------------------------------------------
 * 实时动态榜单
 * ---------------------------------------------------------------- */
const feedTab = ref('in')
const feedTabs = [
  { key: 'in', label: '最近入库' },
  { key: 'pickup', label: '最近取件' },
  { key: 'overdue', label: '逾期最久' }
]

/** 统一成 { key, code, waybillNo, meta, tag, tone } 结构，便于同一套模板渲染 */
function toFeedItem(parcel, kind) {
  const base = {
    key: `${kind}-${parcel.id}`,
    code: parcel.pickupCode || '------',
    waybillNo: parcel.waybillNo,
    meta: '',
    tag: '',
    tone: 'blue'
  }
  if (kind === 'in') {
    return {
      ...base,
      meta: `${parcel.expressCompany || ''} · ${parcel.receiverName || ''} · 货位 ${parcel.shelfCode || '未分配'}`,
      tag: parcel.statusName || '在库',
      tone: parcel.status === 'EXCEPTION' ? 'red' : 'teal'
    }
  }
  if (kind === 'pickup') {
    return {
      ...base,
      meta: `${parcel.receiverName || ''} · ${parcel.pickupTime ? String(parcel.pickupTime).slice(5, 16) : ''}`,
      tag: '已取件',
      tone: 'blue'
    }
  }
  return {
    ...base,
    meta: `${parcel.receiverName || ''} · 已保管 ${parcel.storageDays || 0} 天`,
    tag: `逾期 ${parcel.overdueDayCount || 0} 天`,
    tone: 'red'
  }
}

const currentFeed = computed(() => {
  const d = data.value
  if (feedTab.value === 'pickup') return (d.recentPickup || []).map((p) => toFeedItem(p, 'pickup'))
  if (feedTab.value === 'overdue') return (d.topOverdue || []).map((p) => toFeedItem(p, 'overdue'))
  return (d.recentInStore || []).map((p) => toFeedItem(p, 'in'))
})

/* ------------------------------------------------------------------
 * 交互
 * ---------------------------------------------------------------- */
function toggleFullscreen() {
  const el = document.documentElement
  if (!document.fullscreenElement) {
    el.requestFullscreen().catch(() => ElMessage.warning('当前浏览器不允许全屏'))
  } else {
    document.exitFullscreen()
  }
}

function goBack() {
  router.push('/dashboard')
}

/** F 键快捷全屏 */
function onKeydown(e) {
  if (e.key === 'f' || e.key === 'F') {
    if (e.target && ['INPUT', 'TEXTAREA'].includes(e.target.tagName)) return
    toggleFullscreen()
  }
}

watch(feedTab, () => {
  // 切换榜单时重置滚动位置（transition-group 会自动处理动画）
})

onMounted(async () => {
  tickClock()
  await loadStations()
  await loadData()
  clockTimer = window.setInterval(tickClock, 1000)
  refreshTimer = window.setInterval(tickCountdown, 1000)
  window.addEventListener('keydown', onKeydown)
})

onBeforeUnmount(() => {
  if (clockTimer) window.clearInterval(clockTimer)
  if (refreshTimer) window.clearInterval(refreshTimer)
  window.removeEventListener('keydown', onKeydown)
})
</script>

<style scoped>
/* ================= 整体容器 ================= */
.screen {
  position: relative;
  display: flex;
  flex-direction: column;
  height: 100vh;
  padding: 14px 18px 10px;
  overflow: hidden;
  color: #e6f1ff;
  background: radial-gradient(circle at 50% -10%, #0d2b52 0%, #061426 45%, #030b16 100%);
  font-family: 'Helvetica Neue', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

/* 背景光效 */
.screen__bg {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  pointer-events: none;
}

.screen__bg--1 {
  top: -140px;
  left: -80px;
  width: 460px;
  height: 460px;
  background: rgba(22, 211, 200, 0.16);
}

.screen__bg--2 {
  bottom: -180px;
  right: -100px;
  width: 520px;
  height: 520px;
  background: rgba(26, 109, 255, 0.18);
}

/* 网格纹理 */
.screen__grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.028) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.028) 1px, transparent 1px);
  background-size: 44px 44px;
  pointer-events: none;
}

/* ================= 顶部标题栏 ================= */
.screen__header {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(22, 211, 200, 0.18);
  flex-shrink: 0;
}

.screen__header-side {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.screen__header-side--right {
  justify-content: flex-end;
}

.screen__live {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: rgba(230, 241, 255, 0.7);
}

.screen__title {
  text-align: center;
  flex-shrink: 0;
}

.screen__title h1 {
  margin: 0;
  font-size: 26px;
  font-weight: 700;
  letter-spacing: 3px;
  background: linear-gradient(135deg, #ffffff 0%, #16d3c8 100%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  text-shadow: 0 0 40px rgba(22, 211, 200, 0.3);
}

.screen__title p {
  margin: 3px 0 0;
  font-size: 10px;
  letter-spacing: 2.4px;
  color: rgba(22, 211, 200, 0.6);
}

.screen__station-select {
  width: 170px;
}

.screen__station-select :deep(.el-select__wrapper) {
  background: rgba(255, 255, 255, 0.07);
  box-shadow: 0 0 0 1px rgba(22, 211, 200, 0.28) inset;
}

.screen__station-select :deep(.el-select__placeholder),
.screen__station-select :deep(.el-select__selected-item) {
  color: rgba(230, 241, 255, 0.85);
}

.screen__clock {
  text-align: right;
  line-height: 1.2;
}

.screen__time {
  font-size: 20px;
  font-weight: 700;
  font-family: 'DIN Alternate', 'Bahnschrift', monospace;
  color: #16d3c8;
  letter-spacing: 1px;
}

.screen__date {
  font-size: 11px;
  color: rgba(230, 241, 255, 0.55);
}

.screen__icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  font-size: 17px;
  color: rgba(230, 241, 255, 0.75);
  cursor: pointer;
  background: rgba(255, 255, 255, 0.06);
  transition: all 0.22s cubic-bezier(0.22, 0.61, 0.36, 1);
}

.screen__icon-btn:hover {
  color: #16d3c8;
  background: rgba(22, 211, 200, 0.16);
}

/* ================= 主体布局 ================= */
.screen__body {
  position: relative;
  z-index: 2;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-top: 12px;
  min-height: 0;
  overflow: hidden;
}

/* 核心指标行 */
.screen__metrics {
  display: grid;
  grid-template-columns: repeat(8, minmax(0, 1fr));
  gap: 12px;
  flex-shrink: 0;
}

.screen__metric {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border-radius: 12px;
  background: linear-gradient(160deg, rgba(255, 255, 255, 0.09) 0%, rgba(255, 255, 255, 0.03) 100%);
  border: 1px solid rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(8px);
  transition: transform 0.26s cubic-bezier(0.22, 0.61, 0.36, 1), border-color 0.26s;
}

.screen__metric:hover {
  transform: translateY(-3px);
  border-color: rgba(22, 211, 200, 0.45);
}

.screen__metric-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 11px;
  flex-shrink: 0;
  color: #fff;
}

.screen__metric--teal .screen__metric-icon {
  background: linear-gradient(135deg, #0fb98f, #16d3c8);
  box-shadow: 0 0 18px rgba(22, 211, 200, 0.4);
}

.screen__metric--blue .screen__metric-icon {
  background: linear-gradient(135deg, #1557cc, #1a6dff);
  box-shadow: 0 0 18px rgba(26, 109, 255, 0.4);
}

.screen__metric--purple .screen__metric-icon {
  background: linear-gradient(135deg, #5b32d6, #7c4dff);
  box-shadow: 0 0 18px rgba(124, 77, 255, 0.4);
}

.screen__metric--orange .screen__metric-icon {
  background: linear-gradient(135deg, #d98200, #f59f00);
  box-shadow: 0 0 18px rgba(245, 159, 0, 0.4);
}

.screen__metric--red .screen__metric-icon {
  background: linear-gradient(135deg, #c9372c, #f04438);
  box-shadow: 0 0 18px rgba(240, 68, 56, 0.4);
}

.screen__metric--gray .screen__metric-icon {
  background: linear-gradient(135deg, #4b5a70, #64748b);
  box-shadow: 0 0 18px rgba(100, 116, 139, 0.35);
}

.screen__metric-main {
  min-width: 0;
}

.screen__metric-value {
  font-size: 23px;
  font-weight: 700;
  line-height: 1.15;
  font-family: 'DIN Alternate', 'Bahnschrift', monospace;
  color: #fff;
  font-variant-numeric: tabular-nums;
}

.screen__metric-unit {
  font-size: 11px;
  font-weight: 400;
  margin-left: 2px;
  color: rgba(230, 241, 255, 0.5);
}

.screen__metric-label {
  font-size: 11px;
  color: rgba(230, 241, 255, 0.55);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 面板通用 */
.screen__row {
  display: grid;
  gap: 12px;
  min-height: 0;
}

.screen__row--charts {
  grid-template-columns: 2.1fr 1fr;
  flex: 1.15;
}

.screen__row--bottom {
  grid-template-columns: 1fr 1fr 1.3fr;
  flex: 1;
}

.screen__panel {
  position: relative;
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding: 12px 14px;
  border-radius: 13px;
  background: linear-gradient(165deg, rgba(255, 255, 255, 0.075) 0%, rgba(255, 255, 255, 0.022) 100%);
  border: 1px solid rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(8px);
  overflow: hidden;
}

/* 面板顶部品牌色描边 */
.screen__panel::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, #16d3c8, #1a6dff 55%, transparent);
}

.screen__panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 8px;
  flex-shrink: 0;
}

.screen__panel-title {
  position: relative;
  padding-left: 11px;
  font-size: 13px;
  font-weight: 600;
  color: #e6f1ff;
  letter-spacing: 0.6px;
}

.screen__panel-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 13px;
  border-radius: 2px;
  background: linear-gradient(180deg, #16d3c8, #1a6dff);
}

.screen__legend {
  display: flex;
  gap: 12px;
  font-size: 11px;
  color: rgba(230, 241, 255, 0.6);
}

.screen__legend-item {
  display: flex;
  align-items: center;
  gap: 5px;
}

.screen__legend-item i {
  width: 9px;
  height: 3px;
  border-radius: 2px;
}

/* 图表容器占满面板剩余高度 */
.screen__panel :deep(.chart-box) {
  flex: 1;
  min-height: 0;
}

/* ================= 实时动态 ================= */
.screen__tabs {
  display: flex;
  gap: 4px;
}

.screen__tab {
  padding: 2px 9px;
  border-radius: 20px;
  font-size: 11px;
  color: rgba(230, 241, 255, 0.6);
  cursor: pointer;
  background: rgba(255, 255, 255, 0.06);
  transition: all 0.22s cubic-bezier(0.22, 0.61, 0.36, 1);
}

.screen__tab:hover {
  color: #fff;
}

.screen__tab.is-active {
  color: #04101f;
  font-weight: 600;
  background: linear-gradient(135deg, #16d3c8, #1a6dff);
}

.screen__feed {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding-right: 2px;
}

.screen__feed-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 7px 9px;
  margin-bottom: 6px;
  border-radius: 9px;
  background: rgba(255, 255, 255, 0.045);
  border-left: 2px solid rgba(22, 211, 200, 0.5);
  transition: background 0.22s;
}

.screen__feed-item:hover {
  background: rgba(22, 211, 200, 0.12);
}

.screen__feed-code {
  flex-shrink: 0;
  min-width: 62px;
  font-family: 'DIN Alternate', 'Bahnschrift', monospace;
  font-size: 14px;
  font-weight: 700;
  color: #16d3c8;
  letter-spacing: 0.5px;
}

.screen__feed-main {
  flex: 1;
  min-width: 0;
}

.screen__feed-waybill {
  font-size: 12px;
  color: #e6f1ff;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.screen__feed-meta {
  font-size: 11px;
  color: rgba(230, 241, 255, 0.5);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.screen__feed-tag {
  flex-shrink: 0;
  padding: 1px 8px;
  border-radius: 20px;
  font-size: 10px;
  font-weight: 600;
}

.screen__feed-tag--teal {
  color: #0b3b32;
  background: rgba(22, 211, 200, 0.85);
}

.screen__feed-tag--blue {
  color: #041a3a;
  background: rgba(90, 155, 255, 0.85);
}

.screen__feed-tag--red {
  color: #3d0a06;
  background: rgba(255, 122, 112, 0.9);
}

.screen__feed-empty {
  padding: 20px 0;
}

.screen__feed-empty :deep(.el-empty__description p) {
  color: rgba(230, 241, 255, 0.45);
  font-size: 12px;
}

/* 动态列表进入动画 */
.feed-enter-active {
  transition: all 0.4s cubic-bezier(0.22, 0.61, 0.36, 1);
}

.feed-enter-from {
  opacity: 0;
  transform: translateX(-14px);
}

/* ================= 底部 ================= */
.screen__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-top: 8px;
  border-top: 1px solid rgba(22, 211, 200, 0.14);
  font-size: 11px;
  color: rgba(230, 241, 255, 0.5);
  flex-shrink: 0;
}

.screen__footer-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.screen__footer-item b {
  color: #16d3c8;
  font-size: 13px;
  font-family: 'DIN Alternate', 'Bahnschrift', monospace;
}

/* ================= 自适应 ================= */
@media (max-width: 1600px) {
  .screen__metrics {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

  .screen__title h1 {
    font-size: 21px;
    letter-spacing: 2px;
  }
}

@media (max-width: 1200px) {
  .screen__row--charts,
  .screen__row--bottom {
    grid-template-columns: 1fr;
  }

  .screen__body {
    overflow-y: auto;
  }
}

/* 滚动条适配深色底 */
.screen__feed::-webkit-scrollbar-thumb {
  background-color: rgba(22, 211, 200, 0.3);
}
</style>
