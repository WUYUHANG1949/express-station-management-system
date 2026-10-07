<template>
  <PageContainer title="首页概览" sub-title="今日运营数据与近期趋势一屏掌握">
    <template #extra>
      <span class="text-muted">数据更新时间：{{ updateTime }}</span>
      <el-button type="primary" plain :icon="Refresh" :loading="loading" @click="loadAll">刷新数据</el-button>
    </template>

    <!-- ==================== 指标卡片区（带骨架屏） ==================== -->
    <el-skeleton v-if="loading && !overview" :rows="3" animated class="mb-12" />

    <el-row :gutter="12" class="mb-12">
      <el-col v-for="item in statCards" :key="item.label" :xs="12" :sm="8" :md="6" :lg="6" :xl="3" class="stat-col">
        <StatCard
          :label="item.label"
          :value="item.value"
          :unit="item.unit"
          :icon="item.icon"
          :preset="item.preset"
          :sub-text="item.subText"
        />
      </el-col>
    </el-row>

    <!-- 货位使用率单独一行（含进度条，信息量更大） -->
    <el-card shadow="never" class="mb-12 shelf-card">
      <div class="shelf-card__inner">
        <div class="shelf-card__left">
          <el-icon class="shelf-card__icon"><Grid /></el-icon>
          <div>
            <div class="text-muted">货位使用率</div>
            <div class="shelf-card__value">{{ formatPercent(shelfUsage.rate) }}</div>
          </div>
        </div>
        <div class="shelf-card__right">
          <el-progress
            :percentage="progressPercent"
            :stroke-width="16"
            :color="progressColor"
            :format="() => `${shelfUsage.used} / ${shelfUsage.total}`"
          />
          <div class="text-muted mt-8">
            已用货位 {{ formatNumber(shelfUsage.used) }} 个，总容量 {{ formatNumber(shelfUsage.total) }} 个，
            剩余 {{ Math.max(formatNumber(shelfUsage.total) - formatNumber(shelfUsage.used), 0) }} 个
          </div>
        </div>
      </div>
    </el-card>

    <!-- ==================== 图表区 ==================== -->
    <el-row :gutter="12">
      <!-- 近 7 日出入库趋势 -->
      <el-col :xs="24" :md="14">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="chart-card__header">
              <span class="chart-card__title">近 7 日出入库趋势</span>
              <el-tag size="small" type="info" effect="plain">入库量 / 取件量</el-tag>
            </div>
          </template>
          <ChartBox
            :option="trendOption"
            :loading="loadingTrend"
            :empty="trendEmpty"
            empty-text="暂无趋势数据"
            height="330px"
          />
        </el-card>
      </el-col>

      <!-- 快递公司分布 -->
      <el-col :xs="24" :md="10">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="chart-card__header">
              <span class="chart-card__title">快递公司分布</span>
              <el-tag size="small" type="info" effect="plain">按快件量统计</el-tag>
            </div>
          </template>
          <ChartBox
            :option="companyOption"
            :loading="loadingCompany"
            :empty="companyEmpty"
            empty-text="暂无快递公司数据"
            height="330px"
          />
        </el-card>
      </el-col>
    </el-row>
  </PageContainer>
</template>

<script setup>
/**
 * 首页概览
 * 接口：
 *  - GET /api/stats/overview   概览指标
 *  - GET /api/stats/trend      近 7 日出入库趋势
 *  - GET /api/stats/company    快递公司分布
 * 所有请求失败均有兜底，页面不会白屏
 */
import { computed, onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import StatCard from '@/components/StatCard.vue'
import ChartBox from '@/components/ChartBox.vue'
import { getCompanyStats, getOverview, getTrend } from '@/api/stats'
import { formatDateTime, formatNumber, formatPercent } from '@/utils/format'

/** 组件名：供 Layout 的 keep-alive include 使用 */
defineOptions({ name: 'Dashboard' })

/** 概览指标 */
const overview = ref(null)
/** 趋势数据 */
const trend = ref({ dates: [], inCounts: [], pickupCounts: [] })
/** 快递公司分布数据 */
const companyList = ref([])

/** 各区块 loading */
const loading = ref(false)
const loadingTrend = ref(false)
const loadingCompany = ref(false)
/** 数据更新时间 */
const updateTime = ref('-')

/** 货位使用率对象（兜底空对象，避免模板取值报错） */
const shelfUsage = computed(() => overview.value?.shelfUsage || { total: 0, used: 0, rate: 0 })

/** 进度条百分比（限制在 0-100） */
const progressPercent = computed(() => {
  const rate = Number(shelfUsage.value.rate)
  if (isNaN(rate)) return 0
  return Math.min(Math.max(Number(rate.toFixed(1)), 0), 100)
})

/** 使用率越高颜色越警示 */
const progressColor = computed(() => {
  if (progressPercent.value >= 90) return '#f04438'
  if (progressPercent.value >= 70) return '#f59f00'
  return '#1a6dff'
})

/** 趋势图是否为空数据 */
const trendEmpty = computed(() => {
  const t = trend.value || {}
  return !t.dates || t.dates.length === 0
})

/** 公司分布是否为空数据 */
const companyEmpty = computed(() => !companyList.value || companyList.value.length === 0)

/**
 * 指标卡片配置
 * 数值为空时统一显示 0
 */
const statCards = computed(() => {
  const o = overview.value || {}
  return [
    {
      label: '今日入库',
      value: formatNumber(o.todayInCount),
      unit: '件',
      icon: 'Download',
      preset: 'teal',
      subText: '今日新登记到件'
    },
    {
      label: '今日取件',
      value: formatNumber(o.todayPickupCount),
      unit: '件',
      icon: 'Finished',
      preset: 'blue',
      subText: '今日完成核销'
    },
    {
      label: '今日寄件',
      value: formatNumber(o.todayShipCount),
      unit: '件',
      icon: 'Van',
      preset: 'purple',
      subText: '今日受理寄件单'
    },
    {
      label: '在库快件',
      value: formatNumber(o.inStoreCount),
      unit: '件',
      icon: 'Box',
      preset: 'blue',
      subText: '当前在库待取'
    },
    {
      label: '逾期件',
      value: formatNumber(o.overdueCount),
      unit: '件',
      icon: 'AlarmClock',
      preset: 'orange',
      subText: '超过免费保管天数'
    },
    {
      label: '异常件',
      value: formatNumber(o.exceptionCount),
      unit: '件',
      icon: 'Warning',
      preset: 'red',
      subText: '待处理与处理中'
    },
    {
      label: '派送中',
      value: formatNumber(o.deliveringCount),
      unit: '件',
      icon: 'Promotion',
      preset: 'purple',
      subText: '已派送未签收'
    },
    {
      label: '累计快件',
      value: formatNumber(o.totalParcelCount),
      unit: '件',
      icon: 'DataLine',
      preset: 'gray',
      subText: '历史累计登记总量'
    }
  ]
})

/**
 * 趋势折线图配置
 * 两条线：入库量、取件量，带渐变面积
 */
const trendOption = computed(() => {
  const t = trend.value || { dates: [], inCounts: [], pickupCounts: [] }
  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['入库量', '取件量'], right: 10, top: 0 },
    grid: { left: 12, right: 18, top: 44, bottom: 8, containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: t.dates || [],
      axisLine: { lineStyle: { color: '#dcdfe6' } },
      axisLabel: { color: '#909399' }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: { lineStyle: { type: 'dashed', color: '#ebeef5' } },
      axisLabel: { color: '#909399' }
    },
    series: [
      {
        name: '入库量',
        type: 'line',
        smooth: true,
        symbolSize: 7,
        data: t.inCounts || [],
        itemStyle: { color: '#16d3c8' },
        lineStyle: { width: 3 },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(22,211,200,0.35)' },
              { offset: 1, color: 'rgba(22,211,200,0.02)' }
            ]
          }
        }
      },
      {
        name: '取件量',
        type: 'line',
        smooth: true,
        symbolSize: 7,
        data: t.pickupCounts || [],
        itemStyle: { color: '#1a6dff' },
        lineStyle: { width: 3 },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(26,109,255,0.32)' },
              { offset: 1, color: 'rgba(26,109,255,0.02)' }
            ]
          }
        }
      }
    ]
  }
})

/** 快递公司分布环形图配置 */
const companyOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}：{c} 件（{d}%）' },
  legend: { orient: 'vertical', right: 6, top: 'center', itemWidth: 10, itemHeight: 10, textStyle: { fontSize: 12 } },
  series: [
    {
      name: '快递公司',
      type: 'pie',
      radius: ['45%', '70%'],
      center: ['38%', '52%'],
      avoidLabelOverlap: true,
      itemStyle: { borderColor: '#fff', borderWidth: 2 },
      label: { formatter: '{b}\n{d}%', fontSize: 12, color: '#606266' },
      labelLine: { length: 8, length2: 8 },
      data: companyList.value || []
    }
  ]
}))

/** 加载概览指标 */
async function loadOverview() {
  loading.value = true
  try {
    const data = await getOverview()
    overview.value = data || {}
  } catch (e) {
    overview.value = overview.value || {}
  } finally {
    loading.value = false
    updateTime.value = formatDateTime(new Date())
  }
}

/** 加载近 7 日趋势 */
async function loadTrend() {
  loadingTrend.value = true
  try {
    const data = await getTrend({ days: 7 })
    trend.value = {
      dates: (data && data.dates) || [],
      inCounts: (data && data.inCounts) || [],
      pickupCounts: (data && data.pickupCounts) || []
    }
  } catch (e) {
    trend.value = { dates: [], inCounts: [], pickupCounts: [] }
  } finally {
    loadingTrend.value = false
  }
}

/** 加载快递公司分布 */
async function loadCompany() {
  loadingCompany.value = true
  try {
    const data = await getCompanyStats()
    companyList.value = Array.isArray(data) ? data : []
  } catch (e) {
    companyList.value = []
  } finally {
    loadingCompany.value = false
  }
}

/** 并行加载全部数据 */
async function loadAll() {
  await Promise.all([loadOverview(), loadTrend(), loadCompany()])
}

onMounted(() => {
  loadAll()
})
</script>

<style scoped>
.stat-col {
  margin-bottom: 12px;
}

/* 货位使用率卡片 */
.shelf-card {
  border-radius: 8px;
  border: none;
}

.shelf-card__inner {
  display: flex;
  align-items: center;
  gap: 24px;
  flex-wrap: wrap;
}

.shelf-card__left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 200px;
}

.shelf-card__icon {
  font-size: 30px;
  color: var(--es-primary);
}

.shelf-card__value {
  font-size: 24px;
  font-weight: 700;
  color: var(--es-text-1);
}

.shelf-card__right {
  flex: 1;
  min-width: 260px;
}

/* 图表卡片 */
.chart-card {
  border-radius: var(--es-radius-lg);
  border: none;
  margin-bottom: 12px;
}

.chart-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.chart-card__title {
  font-size: 15px;
  font-weight: 600;
  color: var(--es-text-1);
}
</style>
