<template>
  <PageContainer title="数据统计" sub-title="多维度的业务数据可视化分析">
    <template #extra>
      <el-select
        v-model="stationId"
        placeholder="全部驿站"
        clearable
        style="width: 200px"
        @change="handleStationChange"
      >
        <el-option v-for="item in stationList" :key="item.id" :label="item.stationName" :value="item.id" />
      </el-select>
      <el-radio-group v-model="days" @change="loadTrend">
        <el-radio-button :value="7">近 7 日</el-radio-button>
        <el-radio-button :value="15">近 15 日</el-radio-button>
        <el-radio-button :value="30">近 30 日</el-radio-button>
      </el-radio-group>
      <el-button type="primary" plain :icon="Refresh" :loading="loadingAny" @click="loadAll">刷新</el-button>
    </template>

    <!-- ==================== 概览指标 ==================== -->
    <el-row :gutter="12" class="mb-12">
      <el-col v-for="item in statCards" :key="item.label" :xs="12" :sm="8" :md="6" class="stat-col">
        <StatCard
          :label="item.label"
          :value="item.value"
          :unit="item.unit"
          :icon="item.icon"
          :color="item.color"
          :bg-color="item.bgColor"
          :sub-text="item.subText"
        />
      </el-col>
    </el-row>

    <!-- ==================== 趋势折线图 ==================== -->
    <el-card shadow="never" class="chart-card">
      <template #header>
        <div class="chart-card__header">
          <span class="chart-card__title">近 {{ days }} 日出入库趋势</span>
          <el-tag size="small" type="info" effect="plain">{{ stationLabel }}</el-tag>
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

    <!-- ==================== 两个饼图 ==================== -->
    <el-row :gutter="12">
      <el-col :xs="24" :md="12">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="chart-card__header">
              <span class="chart-card__title">快递公司分布</span>
              <el-tag size="small" type="info" effect="plain">按快件量</el-tag>
            </div>
          </template>
          <ChartBox
            :option="companyOption"
            :loading="loadingCompany"
            :empty="companyEmpty"
            empty-text="暂无快递公司数据"
            height="340px"
          />
        </el-card>
      </el-col>

      <el-col :xs="24" :md="12">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="chart-card__header">
              <span class="chart-card__title">快件类型分布</span>
              <el-tag size="small" type="info" effect="plain">按快件量</el-tag>
            </div>
          </template>
          <ChartBox
            :option="parcelTypeOption"
            :loading="loadingParcelType"
            :empty="parcelTypeEmpty"
            empty-text="暂无快件类型数据"
            height="340px"
          />
        </el-card>
      </el-col>
    </el-row>

    <!-- ==================== 驿站业务量排行（仅 ADMIN） ==================== -->
    <el-card shadow="never" class="chart-card">
      <template #header>
        <div class="chart-card__header">
          <span class="chart-card__title">驿站业务量排行</span>
          <el-tag v-if="isAdmin" size="small" type="warning" effect="plain">仅系统管理员可见</el-tag>
        </div>
      </template>

      <!-- 非管理员：无权限提示 -->
      <el-empty v-if="!isAdmin" description="仅系统管理员可查看驿站业务量排行" :image-size="90" />
      <ChartBox
        v-else
        :option="rankOption"
        :loading="loadingRank"
        :empty="rankEmpty"
        empty-text="暂无驿站业务量数据"
        height="380px"
      />
    </el-card>
  </PageContainer>
</template>

<script setup>
/**
 * 数据统计页
 * 接口：
 *  - GET /api/stats/overview?stationId=
 *  - GET /api/stats/trend?days=7&stationId=
 *  - GET /api/stats/company?stationId=
 *  - GET /api/stats/parcel-type?stationId=
 *  - GET /api/stats/station-rank（仅 ADMIN）
 */
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import StatCard from '@/components/StatCard.vue'
import ChartBox from '@/components/ChartBox.vue'
import { getCompanyStats, getOverview, getParcelTypeStats, getStationRank, getTrend } from '@/api/stats'
import { listStations } from '@/api/station'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'
import { formatNumber, formatPercent } from '@/utils/format'

const appStore = useAppStore()
const userStore = useUserStore()

/** 是否为系统管理员（决定是否展示驿站业务量排行） */
const isAdmin = computed(() => userStore.isAdmin)

/** 筛选条件：驿站与统计天数 */
const stationId = ref(appStore.filterStationId || undefined)
const days = ref(7)

/** 驿站下拉数据 */
const stationList = ref([])

/** 各数据块 */
const overview = ref(null)
const trend = ref({ dates: [], inCounts: [], pickupCounts: [] })
const companyList = ref([])
const parcelTypeList = ref([])
const rankList = ref([])

/** loading 状态 */
const loadingOverview = ref(false)
const loadingTrend = ref(false)
const loadingCompany = ref(false)
const loadingParcelType = ref(false)
const loadingRank = ref(false)

/** 任一区块加载中（用于刷新按钮的 loading） */
const loadingAny = computed(
  () => loadingOverview.value || loadingTrend.value || loadingCompany.value || loadingParcelType.value || loadingRank.value
)

/** 当前驿站名称（用于图表右上角标注） */
const stationLabel = computed(() => {
  if (!stationId.value) return '全部驿站'
  const hit = stationList.value.find((item) => item.id === stationId.value)
  return hit ? hit.stationName : '指定驿站'
})

/** 查询参数：未选择驿站时不传 stationId */
const stationParam = computed(() => (stationId.value ? { stationId: stationId.value } : {}))

/** 概览指标卡片 */
const statCards = computed(() => {
  const o = overview.value || {}
  const shelf = o.shelfUsage || {}
  return [
    { label: '今日入库', value: formatNumber(o.todayInCount), unit: '件', icon: 'Download', color: '#2563eb', bgColor: '#ecf5ff', subText: '今日登记到件' },
    { label: '今日取件', value: formatNumber(o.todayPickupCount), unit: '件', icon: 'Finished', color: '#67c23a', bgColor: '#f0f9eb', subText: '今日取件核销' },
    { label: '今日寄件', value: formatNumber(o.todayShipCount), unit: '件', icon: 'Van', color: '#0ea5e9', bgColor: '#e8f7ff', subText: '今日受理寄件' },
    { label: '在库快件', value: formatNumber(o.inStoreCount), unit: '件', icon: 'Box', color: '#409eff', bgColor: '#ecf5ff', subText: '当前在库待取' },
    { label: '逾期件', value: formatNumber(o.overdueCount), unit: '件', icon: 'AlarmClock', color: '#e6a23c', bgColor: '#fdf6ec', subText: '超过免费保管期' },
    { label: '异常件', value: formatNumber(o.exceptionCount), unit: '件', icon: 'Warning', color: '#f56c6c', bgColor: '#fef0f0', subText: '待处理与处理中' },
    {
      label: '货位使用率',
      value: formatPercent(shelf.rate),
      unit: '',
      icon: 'Grid',
      color: '#7c3aed',
      bgColor: '#f3eeff',
      subText: `已用 ${formatNumber(shelf.used)} / 共 ${formatNumber(shelf.total)}`
    },
    { label: '累计快件', value: formatNumber(o.totalParcelCount), unit: '件', icon: 'DataLine', color: '#909399', bgColor: '#f4f4f5', subText: '历史累计登记量' }
  ]
})

/** 趋势图空数据判断 */
const trendEmpty = computed(() => !trend.value?.dates?.length)
/** 公司分布空数据判断 */
const companyEmpty = computed(() => !companyList.value.length)
/** 类型分布空数据判断 */
const parcelTypeEmpty = computed(() => !parcelTypeList.value.length)
/** 驿站排行空数据判断 */
const rankEmpty = computed(() => !rankList.value.length)

/** 趋势折线图配置 */
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
        symbolSize: 6,
        data: t.inCounts || [],
        itemStyle: { color: '#2563eb' },
        lineStyle: { width: 3 },
        areaStyle: { color: 'rgba(37,99,235,0.18)' }
      },
      {
        name: '取件量',
        type: 'line',
        smooth: true,
        symbolSize: 6,
        data: t.pickupCounts || [],
        itemStyle: { color: '#67c23a' },
        lineStyle: { width: 3 },
        areaStyle: { color: 'rgba(103,194,58,0.18)' }
      }
    ]
  }
})

/**
 * 生成饼图配置的工厂函数（公司分布与类型分布共用）
 * @param {Array} data [{ name, value }]
 * @param {string} seriesName 系列名称
 */
function buildPieOption(data, seriesName) {
  return {
    tooltip: { trigger: 'item', formatter: '{b}：{c} 件（{d}%）' },
    legend: { orient: 'vertical', right: 6, top: 'center', itemWidth: 10, itemHeight: 10, textStyle: { fontSize: 12 } },
    series: [
      {
        name: seriesName,
        type: 'pie',
        radius: ['42%', '68%'],
        center: ['36%', '52%'],
        itemStyle: { borderColor: '#fff', borderWidth: 2 },
        label: { formatter: '{b}\n{d}%', fontSize: 12, color: '#606266' },
        labelLine: { length: 8, length2: 8 },
        data: data || []
      }
    ]
  }
}

/** 快递公司分布饼图配置 */
const companyOption = computed(() => buildPieOption(companyList.value, '快递公司'))
/** 快件类型分布饼图配置 */
const parcelTypeOption = computed(() => buildPieOption(parcelTypeList.value, '快件类型'))

/** 驿站业务量排行柱状图配置：收件量 + 取件量双系列 */
const rankOption = computed(() => {
  const names = rankList.value.map((item) => item.stationName)
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { data: ['收件量', '取件量'], right: 10, top: 0 },
    grid: { left: 12, right: 18, top: 44, bottom: 8, containLabel: true },
    xAxis: {
      type: 'category',
      data: names,
      axisLabel: { color: '#909399', interval: 0, rotate: names.length > 6 ? 20 : 0, fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: { lineStyle: { type: 'dashed', color: '#ebeef5' } },
      axisLabel: { color: '#909399' }
    },
    series: [
      {
        name: '收件量',
        type: 'bar',
        barMaxWidth: 28,
        itemStyle: { color: '#2563eb', borderRadius: [4, 4, 0, 0] },
        data: rankList.value.map((item) => item.parcelCount || 0)
      },
      {
        name: '取件量',
        type: 'bar',
        barMaxWidth: 28,
        itemStyle: { color: '#67c23a', borderRadius: [4, 4, 0, 0] },
        data: rankList.value.map((item) => item.pickupCount || 0)
      }
    ]
  }
})

/** 加载概览指标 */
async function loadOverview() {
  loadingOverview.value = true
  try {
    overview.value = (await getOverview(stationParam.value)) || {}
  } catch (e) {
    overview.value = overview.value || {}
  } finally {
    loadingOverview.value = false
  }
}

/** 加载趋势数据 */
async function loadTrend() {
  loadingTrend.value = true
  try {
    const data = await getTrend({ days: days.value, ...stationParam.value })
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
    const data = await getCompanyStats(stationParam.value)
    companyList.value = Array.isArray(data) ? data : []
  } catch (e) {
    companyList.value = []
  } finally {
    loadingCompany.value = false
  }
}

/** 加载快件类型分布 */
async function loadParcelType() {
  loadingParcelType.value = true
  try {
    const data = await getParcelTypeStats(stationParam.value)
    parcelTypeList.value = Array.isArray(data) ? data : []
  } catch (e) {
    parcelTypeList.value = []
  } finally {
    loadingParcelType.value = false
  }
}

/** 加载驿站业务量排行（仅 ADMIN 可调用） */
async function loadRank() {
  if (!isAdmin.value) {
    rankList.value = []
    return
  }
  loadingRank.value = true
  try {
    const data = await getStationRank()
    rankList.value = Array.isArray(data) ? data : []
  } catch (e) {
    rankList.value = []
  } finally {
    loadingRank.value = false
  }
}

/** 加载驿站下拉数据（失败不影响主流程） */
async function loadStations() {
  try {
    const data = await listStations()
    stationList.value = Array.isArray(data) ? data : []
  } catch (e) {
    stationList.value = []
  }
}

/** 驿站筛选变化：同步到全局 store 并重新加载全部统计 */
function handleStationChange(value) {
  appStore.setFilterStation(value === '' || value === undefined ? null : value)
  loadAll()
}

/** 并行加载所有统计数据 */
async function loadAll() {
  await Promise.all([loadOverview(), loadTrend(), loadCompany(), loadParcelType(), loadRank()])
}

onMounted(async () => {
  await loadStations()
  await loadAll()
  if (!stationList.value.length) {
    ElMessage.warning('未获取到驿站列表，驿站筛选不可用')
  }
})
</script>

<style scoped>
.stat-col {
  margin-bottom: 12px;
}

.chart-card {
  border-radius: 8px;
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
  color: #1f2d3d;
}
</style>
