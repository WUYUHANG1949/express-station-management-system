<template>
  <PageContainer title="货位地图" sub-title="按库区展示货架占用情况，点击格子查看该库位上的快件">
    <!-- ==================== 顶部指标 ==================== -->
    <div class="map-stats">
      <StatCard
        label="货位总数"
        :value="stats.shelfTotal"
        unit="个"
        icon="Grid"
        preset="blue"
        :sub-text="`覆盖 ${groups.length} 个库区`"
      />
      <StatCard
        label="已占用货位"
        :value="stats.usedShelf"
        unit="个"
        icon="Files"
        preset="teal"
        :sub-text="`空闲 ${stats.emptyShelf} 个`"
      />
      <StatCard
        label="在架快件"
        :value="stats.parcelTotal"
        unit="件"
        icon="Box"
        preset="orange"
        :sub-text="`已登记占用 ${stats.used} 件`"
      />
      <StatCard
        label="整体占用率"
        :value="stats.rate"
        unit="%"
        icon="PieChart"
        preset="purple"
        :sub-text="`已用 ${stats.used} / 容量 ${stats.capacity}`"
      />
    </div>

    <!-- ==================== 控制栏 ==================== -->
    <el-form :inline="true" class="search-bar">
      <el-form-item label="所属驿站">
        <el-select
          v-model="stationId"
          placeholder="全部驿站"
          :clearable="isAdmin"
          :disabled="!isAdmin"
          style="width: 200px"
          @change="loadData"
        >
          <el-option v-for="item in stationList" :key="item.id" :label="item.stationName" :value="item.id" />
        </el-select>
        <span v-if="!isAdmin" class="text-muted ml-8">员工仅可查看本驿站货位</span>
        <span v-else-if="!stationId" class="text-muted ml-8">
          已选「全部驿站」：每格已标出所属驿站，同名库位可据此区分
        </span>
      </el-form-item>
      <el-form-item label="库区">
        <el-radio-group v-model="activeArea">
          <el-radio-button value="ALL">全部</el-radio-button>
          <el-radio-button v-for="key in areaKeys" :key="key" :value="key">{{ areaLabel(key) }}</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item>
        <el-button :icon="Refresh" :loading="loading" @click="loadData">刷新</el-button>
        <el-button
          v-perm="'system:shelf:edit'"
          type="warning"
          plain
          :icon="MagicStick"
          :loading="recalcLoading"
          @click="handleRecalculate"
        >
          重算货位占用
        </el-button>
      </el-form-item>
    </el-form>

    <!-- ==================== 图例 ==================== -->
    <div class="legend">
      <span class="legend__title">图例</span>
      <span v-for="item in SHELF_LEVEL" :key="item.value" class="legend__item">
        <i class="legend__chip" :style="{ background: item.color }" />
        {{ item.label }}
      </span>
      <span class="legend__item">
        <i class="legend__chip legend__chip--off" />
        停用
      </span>
    </div>

    <!-- ==================== 货架网格图 ==================== -->
    <div v-loading="loading" class="map-body">
      <template v-if="visibleGroups.length">
        <section v-for="group in visibleGroups" :key="group.key" class="area-card">
          <header class="area-card__head">
            <h3 class="area-card__title">
              {{ group.title }}
              <span class="area-card__meta">
                （{{ group.shelves.length }} 个库位 · 已用 {{ group.used }} / 容量 {{ group.capacity }}）
              </span>
            </h3>
            <el-tag size="small" effect="plain" :type="group.rate >= 90 ? 'danger' : group.rate >= 70 ? 'warning' : 'success'">
              占用 {{ group.rate }}%
            </el-tag>
          </header>

          <div class="shelf-grid">
            <div
              v-for="shelf in group.shelves"
              :key="shelf.shelfId"
              class="shelf-cell"
              :class="{ 'shelf-cell--off': shelf.status === 0, 'shelf-cell--badge': parcelCount(shelf) > 0 }"
              :style="cellStyle(shelf)"
              :title="cellTitle(shelf)"
              @click="openShelf(shelf)"
            >
              <!-- 顶部：库位编号 + 占用率 -->
              <div class="shelf-cell__top">
                <span class="shelf-cell__code">{{ shelf.shelfCode }}</span>
                <span class="shelf-cell__rate">{{ formatRate(shelf) }}%</span>
              </div>

              <!-- 未筛选驿站时，格子上标出所属驿站，避免不同驿站同名库位难以区分 -->
              <div v-if="!stationId && shelf.stationName" class="shelf-cell__station">
                {{ shelf.stationName }}
              </div>

              <!-- 中部：已用 / 容量 + 细进度条 -->
              <div class="shelf-cell__mid">
                <span class="shelf-cell__count">{{ toNum(shelf.usedCount) }}</span>
                <span class="shelf-cell__capacity">/ {{ toNum(shelf.capacity) }} 件</span>
              </div>
              <div class="shelf-cell__bar">
                <i class="shelf-cell__bar-inner" :style="{ width: `${barWidth(shelf)}%` }" />
              </div>

              <!-- 底部：剩余 -->
              <div class="shelf-cell__foot">剩余 {{ toNum(shelf.freeCount) }} 个位</div>

              <!-- 件数角标 -->
              <span v-if="parcelCount(shelf) > 0" class="shelf-cell__badge">{{ parcelCount(shelf) }}</span>
              <!-- 停用标记 -->
              <span v-if="shelf.status === 0" class="shelf-cell__off-tag">停用</span>
            </div>
          </div>
        </section>
      </template>

      <el-empty
        v-else-if="!loading"
        description="暂无货位数据，请先到「货位管理」维护货位，或切换驿站后重试"
        :image-size="110"
      />
    </div>

    <!-- ==================== 库位详情抽屉 ==================== -->
    <el-drawer v-model="drawerVisible" :title="drawerTitle" size="480px">
      <div v-if="currentShelf" class="shelf-detail">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="所属库区">{{ currentShelf.area || '未分区' }}</el-descriptions-item>
          <el-descriptions-item label="占用程度">
            <el-tag size="small" :type="dictType(SHELF_LEVEL, currentShelf.level)" effect="plain">
              {{ dictLabel(SHELF_LEVEL, currentShelf.level, SHELF_LEVEL_NAME[currentShelf.level] || '-') }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="容量">{{ toNum(currentShelf.capacity) }} 件</el-descriptions-item>
          <el-descriptions-item label="已用">{{ toNum(currentShelf.usedCount) }} 件</el-descriptions-item>
          <el-descriptions-item label="剩余">
            <span :class="{ 'text-danger': toNum(currentShelf.freeCount) <= 0 }">{{ toNum(currentShelf.freeCount) }} 件</span>
          </el-descriptions-item>
          <el-descriptions-item label="占用率">{{ formatRate(currentShelf) }}%</el-descriptions-item>
        </el-descriptions>

        <el-progress
          class="mt-16"
          :percentage="barWidth(currentShelf)"
          :stroke-width="14"
          :color="levelColor(currentShelf.level)"
          :show-text="false"
        />

        <div class="shelf-detail__title">
          库位上的快件（{{ parcelCount(currentShelf) }} 件）
        </div>

        <div v-if="parcelCount(currentShelf)" class="parcel-list">
          <article
            v-for="item in currentShelf.parcels"
            :key="item.id"
            class="parcel-card"
            :class="{ 'parcel-card--overdue': toNum(item.overdueDayCount) > 0 }"
          >
            <header class="parcel-card__head">
              <span class="parcel-card__code">{{ item.pickupCode || '-' }}</span>
              <el-tag v-if="toNum(item.overdueDayCount) > 0" type="danger" size="small" effect="dark">
                逾期 {{ toNum(item.overdueDayCount) }} 天
              </el-tag>
              <el-tag v-else size="small" :type="dictType(PARCEL_STATUS, item.status)" effect="plain">
                {{ item.statusName || PARCEL_STATUS_NAME[item.status] || '在库' }}
              </el-tag>
            </header>

            <div class="parcel-card__row">
              <span class="parcel-card__k">运单号</span>
              <span class="parcel-card__v mono">{{ item.waybillNo || '-' }}</span>
            </div>
            <div class="parcel-card__row">
              <span class="parcel-card__k">收件人</span>
              <span class="parcel-card__v">{{ item.receiverName || '-' }} · {{ item.receiverPhone || '-' }}</span>
            </div>
            <div class="parcel-card__row">
              <span class="parcel-card__k">快递公司</span>
              <span class="parcel-card__v">
                {{ item.expressCompany || '-' }} · {{ item.parcelTypeName || dictLabel(PARCEL_TYPE, item.parcelType) }}
              </span>
            </div>
            <div class="parcel-card__row">
              <span class="parcel-card__k">入库时间</span>
              <span class="parcel-card__v">{{ formatDateTime(item.inTime) }}</span>
            </div>
            <div class="parcel-card__row">
              <span class="parcel-card__k">已保管</span>
              <span class="parcel-card__v">{{ formatNumber(item.storageDays) }} 天</span>
            </div>
            <div v-if="toNum(item.overdueFee) > 0" class="parcel-card__row">
              <span class="parcel-card__k">逾期费</span>
              <span class="parcel-card__v text-danger">{{ formatMoney(item.overdueFee, true) }}</span>
            </div>
          </article>
        </div>

        <el-empty v-else description="该库位当前为空" :image-size="90" />
      </div>
    </el-drawer>
  </PageContainer>
</template>

<script setup>
/**
 * 货位地图（v1.1）
 * 接口：getShelfMap(stationId) 一次取回全部库位与该库位上的快件，页面所有指标均由这一次请求算出
 *       recalculateShelves(stationId) 重算货位占用，修复历史不一致数据
 * 交互：按库区分块 —— CSS Grid 铺库位格子 —— 点击格子打开抽屉看该库位上的快件
 */
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { MagicStick, Refresh } from '@element-plus/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import StatCard from '@/components/StatCard.vue'
import { getShelfMap, recalculateShelves } from '@/api/shelf'
import { listStations } from '@/api/station'
import { useUserStore } from '@/stores/user'
import {
  PARCEL_STATUS,
  PARCEL_STATUS_NAME,
  PARCEL_TYPE,
  SHELF_LEVEL,
  SHELF_LEVEL_NAME,
  dictLabel,
  dictType
} from '@/utils/dict'
import { formatDateTime, formatMoney, formatNumber } from '@/utils/format'

/** 组件名：供 Layout 的 keep-alive include 使用 */
defineOptions({ name: 'ShelfMap' })

const userStore = useUserStore()

/** 是否管理员：员工锁定在本驿站，管理员可查看全部驿站 */
const isAdmin = computed(() => userStore.isAdmin)

/** 未分区库位的分组 key */
const UNKNOWN_AREA = '__UNKNOWN__'

/* ------------------------------------------------------------------
 * 数据加载
 * ---------------------------------------------------------------- */
/** 原始库位数据（后端一次返回，含每个库位上的快件） */
const shelves = ref([])
const loading = ref(false)
const recalcLoading = ref(false)
/** 驿站下拉 */
const stationList = ref([])
/** 当前驿站：员工默认本驿站，管理员默认全部 */
const stationId = ref(userStore.isAdmin ? '' : userStore.stationId || '')
/** 当前选中的库区（ALL 表示全部） */
const activeArea = ref('ALL')

/** 加载货位地图数据 */
async function loadData() {
  loading.value = true
  try {
    const data = await getShelfMap(stationId.value || undefined)
    shelves.value = Array.isArray(data) ? data : []
    // 库区可能因数据变化而消失，重置筛选并同步抽屉里的引用
    if (activeArea.value !== 'ALL' && !areaKeys.value.includes(activeArea.value)) {
      activeArea.value = 'ALL'
    }
    syncCurrentShelf()
  } catch (e) {
    // 错误提示已由响应拦截器统一处理
    shelves.value = []
  } finally {
    loading.value = false
  }
}

/** 加载驿站下拉数据 */
async function loadStations() {
  try {
    const data = await listStations()
    stationList.value = Array.isArray(data) ? data : []
  } catch (e) {
    stationList.value = []
  }
}

/**
 * 重算货位占用（修复因异常中断导致的不一致数据）
 * 后端返回受影响（更新）的货位行数
 */
async function handleRecalculate() {
  try {
    await ElMessageBox.confirm(
      '将按快件表重新计算各货位的占用数量，用于修复异常情况下产生的不一致数据。是否继续？',
      '重算货位占用',
      { confirmButtonText: '开始重算', cancelButtonText: '取消', type: 'warning' }
    )
  } catch (e) {
    return // 用户取消
  }

  recalcLoading.value = true
  try {
    const rows = await recalculateShelves(stationId.value || undefined)
    ElMessage.success(`重算完成，共更新 ${Number(rows) || 0} 个货位占用数量`)
    await loadData()
  } catch (e) {
    // 错误提示已由响应拦截器统一处理
  } finally {
    recalcLoading.value = false
  }
}

/* ------------------------------------------------------------------
 * 顶部指标（全部由 shelves 一次请求算出）
 * ---------------------------------------------------------------- */
const stats = computed(() => {
  let capacity = 0
  let used = 0
  let parcelTotal = 0
  let usedShelf = 0

  shelves.value.forEach((item) => {
    capacity += toNum(item.capacity)
    used += toNum(item.usedCount)
    parcelTotal += parcelCount(item)
    if (toNum(item.usedCount) > 0) usedShelf += 1
  })

  return {
    shelfTotal: shelves.value.length,
    usedShelf,
    emptyShelf: shelves.value.length - usedShelf,
    parcelTotal,
    capacity,
    used,
    rate: capacity ? Number(((used / capacity) * 100).toFixed(1)) : 0
  }
})

/* ------------------------------------------------------------------
 * 按库区分组 + 库区筛选
 * ---------------------------------------------------------------- */
/** 全部库区（按编码自然排序，未分区排最后） */
const groups = computed(() => {
  const map = new Map()
  shelves.value.forEach((item) => {
    const key = item.area ? String(item.area) : UNKNOWN_AREA
    if (!map.has(key)) {
      map.set(key, { key, title: key === UNKNOWN_AREA ? '未分区' : `${key} 区`, shelves: [], used: 0, capacity: 0 })
    }
    const group = map.get(key)
    group.shelves.push(item)
    group.used += toNum(item.usedCount)
    group.capacity += toNum(item.capacity)
  })

  return Array.from(map.values())
    .map((group) => ({
      ...group,
      // 库区内按货位编码排序，保证格子位置稳定
      shelves: group.shelves.slice().sort((a, b) => String(a.shelfCode || '').localeCompare(String(b.shelfCode || ''))),
      rate: group.capacity ? Number(((group.used / group.capacity) * 100).toFixed(1)) : 0
    }))
    .sort((a, b) => {
      if (a.key === UNKNOWN_AREA) return 1
      if (b.key === UNKNOWN_AREA) return -1
      return a.key.localeCompare(b.key)
    })
})

/** 库区筛选可选项 */
const areaKeys = computed(() => groups.value.map((group) => group.key))

/** 当前库区筛选下要渲染的分组 */
const visibleGroups = computed(() => {
  if (activeArea.value === 'ALL') return groups.value
  return groups.value.filter((group) => group.key === activeArea.value)
})

/**
 * 库区按钮文案
 * @param {string} key 分组 key
 */
function areaLabel(key) {
  return key === UNKNOWN_AREA ? '未分区' : key
}

/* ------------------------------------------------------------------
 * 库位格子
 * ---------------------------------------------------------------- */
/** 各占用程度对应的主色与浅色渐变背景 */
const LEVEL_STYLE = {
  EMPTY: { color: '#0fb98f', bg: 'linear-gradient(160deg, rgba(15, 185, 143, 0.16) 0%, rgba(15, 185, 143, 0.05) 100%)' },
  NORMAL: { color: '#1a6dff', bg: 'linear-gradient(160deg, rgba(26, 109, 255, 0.16) 0%, rgba(26, 109, 255, 0.05) 100%)' },
  BUSY: { color: '#f59f00', bg: 'linear-gradient(160deg, rgba(245, 159, 0, 0.18) 0%, rgba(245, 159, 0, 0.05) 100%)' },
  FULL: { color: '#f04438', bg: 'linear-gradient(160deg, rgba(240, 68, 56, 0.18) 0%, rgba(240, 68, 56, 0.05) 100%)' }
}
/** 停用库位的灰底 */
const OFF_STYLE = { color: '#b6c0d0', bg: '#f3f5f9' }

/**
 * 数字兜底
 * @param {*} value
 */
function toNum(value) {
  return formatNumber(value)
}

/** 该库位上的快件数量（以后端返回的 parcels 为准） */
function parcelCount(shelf) {
  return Array.isArray(shelf && shelf.parcels) ? shelf.parcels.length : 0
}

/** 占用率（后端已算好，缺失时前端兜底） */
function rateOf(shelf) {
  const rate = shelf && shelf.rate
  if (rate !== null && rate !== undefined && rate !== '') return Number(rate)
  const capacity = toNum(shelf && shelf.capacity)
  if (!capacity) return 0
  return Number(((toNum(shelf.usedCount) / capacity) * 100).toFixed(1))
}

/** 占用率展示文案（保留 1 位小数，去掉多余的 .0） */
function formatRate(shelf) {
  const rate = rateOf(shelf)
  if (!Number.isFinite(rate)) return '0'
  return Number.isInteger(rate) ? String(rate) : rate.toFixed(1)
}

/** 进度条宽度：限制在 0-100 */
function barWidth(shelf) {
  return Math.min(Math.max(rateOf(shelf), 0), 100)
}

/** 占用程度颜色 */
function levelColor(level) {
  const hit = (SHELF_LEVEL || []).find((item) => item.value === level)
  return hit ? hit.color : '#1a6dff'
}

/** 格子配色：停用优先置灰 */
function cellStyle(shelf) {
  const meta = shelf.status === 0 ? OFF_STYLE : LEVEL_STYLE[shelf.level] || LEVEL_STYLE.EMPTY
  return { '--cell-color': meta.color, background: meta.bg }
}

/** 格子原生 tooltip：数据不一致时给出提示（可用「重算货位占用」修复） */
function cellTitle(shelf) {
  const parts = [
    `${shelf.shelfCode}（${shelf.area ? `${shelf.area} 区` : '未分区'}）`,
    `容量 ${toNum(shelf.capacity)} 件 / 已用 ${toNum(shelf.usedCount)} 件 / 剩余 ${toNum(shelf.freeCount)} 件`,
    `占用率 ${formatRate(shelf)}%（${SHELF_LEVEL_NAME[shelf.level] || '-'}）`
  ]
  // ShelfMapVO 未返回驿站字段，这里从库位上的快件反推，便于「全部驿站」时辨认
  const parcelWithStation = (shelf.parcels || []).find((item) => item.stationName)
  if (parcelWithStation) parts.push(`驿站：${parcelWithStation.stationName}`)
  if (shelf.status === 0) parts.push('该库位已停用')
  if (parcelCount(shelf) !== toNum(shelf.usedCount)) {
    parts.push(`在架快件 ${parcelCount(shelf)} 件与占用登记 ${toNum(shelf.usedCount)} 件不一致，可使用「重算货位占用」修复`)
  }
  parts.push('点击查看该库位上的快件')
  return parts.join('\n')
}

/* ------------------------------------------------------------------
 * 库位详情抽屉
 * ---------------------------------------------------------------- */
const drawerVisible = ref(false)
const currentShelf = ref(null)

/** 当前库位所属驿站名称：优先取后端返回的 shelf.stationName，其次按当前筛选匹配 */
const currentStationName = computed(() => {
  const shelf = currentShelf.value
  if (!shelf) return ''
  if (shelf.stationName) return shelf.stationName
  const fromParcel = (shelf.parcels || []).find((item) => item.stationName)
  if (fromParcel) return fromParcel.stationName
  const hit = stationList.value.find((item) => String(item.id) === String(stationId.value))
  return hit ? hit.stationName : userStore.stationName || ''
})

/** 抽屉标题：库位编号 · 驿站名称 */
const drawerTitle = computed(() => {
  if (!currentShelf.value) return '库位详情'
  return `${currentShelf.value.shelfCode} · ${currentStationName.value || '快递驿站'}`
})

/**
 * 打开库位详情
 * @param {Object} shelf 库位数据
 */
function openShelf(shelf) {
  currentShelf.value = shelf
  drawerVisible.value = true
}

/** 刷新后同步抽屉里的库位引用，库位不存在则关闭抽屉 */
function syncCurrentShelf() {
  if (!currentShelf.value) return
  const hit = shelves.value.find((item) => String(item.shelfId) === String(currentShelf.value.shelfId))
  if (hit) {
    currentShelf.value = hit
  } else {
    currentShelf.value = null
    drawerVisible.value = false
  }
}

onMounted(async () => {
  await loadStations()
  loadData()
})
</script>

<style scoped>
/* ---------------- 顶部指标 ---------------- */
.map-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

@media (max-width: 1100px) {
  .map-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

/* ---------------- 图例 ---------------- */
.legend {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
  padding: 10px 14px;
  margin-bottom: 14px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: var(--es-radius);
  background: linear-gradient(180deg, #fbfdff 0%, #f7faff 100%);
  font-size: 13px;
  color: var(--es-text-2);
}

.legend__title {
  font-weight: 600;
  color: var(--es-text-1);
}

.legend__item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.legend__chip {
  width: 14px;
  height: 14px;
  border-radius: 4px;
  display: inline-block;
}

.legend__chip--off {
  background: #f3f5f9;
  border: 1px dashed #b6c0d0;
}

/* ---------------- 货架网格图 ---------------- */
.map-body {
  min-height: 220px;
}

.area-card {
  padding: 14px 16px 16px;
  margin-bottom: 14px;
  border: 1px solid var(--es-border);
  border-radius: var(--es-radius-lg);
  background: #fff;
  box-shadow: var(--es-shadow-sm);
}

.area-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 1px dashed var(--es-border);
}

.area-card__title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--es-text-1);
}

.area-card__meta {
  font-size: 12px;
  font-weight: 400;
  color: var(--es-text-3);
}

.shelf-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 12px;
}

.shelf-cell {
  position: relative;
  padding: 10px 12px 10px 14px;
  min-height: 104px;
  border: 1px solid var(--es-border);
  border-left: 4px solid var(--cell-color, var(--es-primary));
  border-radius: var(--es-radius);
  background: #fff;
  cursor: pointer;
  user-select: none;
  transition: transform 0.22s var(--es-ease), box-shadow 0.22s var(--es-ease);
}

.shelf-cell:hover {
  transform: translateY(-3px);
  box-shadow: var(--es-shadow);
}

/* 停用库位：置灰 + 虚线边框 */
.shelf-cell--off {
  border-style: dashed;
  border-left-style: solid;
  color: var(--es-text-3);
}

.shelf-cell__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
}

/* 有件数角标时给占用率留出空间，避免被角标遮住 */
.shelf-cell--badge .shelf-cell__top {
  padding-right: 16px;
}

.shelf-cell__code {
  font-size: 14px;
  font-weight: 700;
  color: var(--es-text-1);
  letter-spacing: 0.4px;
  font-family: 'Consolas', 'Courier New', monospace;
}

.shelf-cell--off .shelf-cell__code {
  color: var(--es-text-3);
}

.shelf-cell__rate {
  font-size: 11px;
  color: var(--es-text-3);
  font-variant-numeric: tabular-nums;
}

/* 「全部驿站」时显示的所属驿站标签，用于区分不同驿站的同名库位 */
.shelf-cell__station {
  margin-top: 1px;
  font-size: 10px;
  color: var(--es-text-3);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.shelf-cell__mid {
  display: flex;
  align-items: baseline;
  gap: 4px;
  margin: 8px 0 6px;
}

.shelf-cell__count {
  font-size: 19px;
  font-weight: 700;
  line-height: 1.1;
  color: var(--cell-color, var(--es-text-1));
  font-family: 'DIN Alternate', 'Bahnschrift', 'Helvetica Neue', sans-serif;
  letter-spacing: -0.5px;
}

.shelf-cell__capacity {
  font-size: 12px;
  color: var(--es-text-3);
}

.shelf-cell__bar {
  height: 5px;
  border-radius: 3px;
  background: rgba(9, 30, 66, 0.07);
  overflow: hidden;
}

.shelf-cell__bar-inner {
  display: block;
  height: 100%;
  border-radius: 3px;
  background: var(--cell-color, var(--es-primary));
  transition: width 0.4s var(--es-ease);
}

.shelf-cell__foot {
  margin-top: 7px;
  font-size: 12px;
  color: var(--es-text-3);
}

.shelf-cell__badge {
  position: absolute;
  top: -7px;
  right: -7px;
  min-width: 22px;
  height: 22px;
  padding: 0 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 11px;
  border: 2px solid #fff;
  background: var(--cell-color, var(--es-primary));
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  box-shadow: 0 2px 6px rgba(9, 30, 66, 0.24);
}

.shelf-cell__off-tag {
  position: absolute;
  right: 8px;
  bottom: 8px;
  font-size: 11px;
  color: var(--es-text-3);
}

/* ---------------- 库位详情抽屉 ---------------- */
.shelf-detail__title {
  margin: 18px 0 10px;
  padding-left: 8px;
  border-left: 4px solid var(--es-primary);
  font-size: 14px;
  font-weight: 600;
  color: var(--es-text-1);
}

.parcel-list {
  display: grid;
  gap: 10px;
}

.parcel-card {
  padding: 10px 12px;
  border: 1px solid var(--es-border);
  border-radius: var(--es-radius);
  background: linear-gradient(180deg, #fbfdff 0%, #ffffff 100%);
}

/* 逾期件：红色描边 */
.parcel-card--overdue {
  border-color: var(--el-color-danger);
  box-shadow: 0 0 0 1px rgba(240, 68, 56, 0.16);
}

.parcel-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 6px;
}

.parcel-card__code {
  font-size: 17px;
  font-weight: 700;
  letter-spacing: 1px;
  color: var(--es-primary);
  font-family: 'Consolas', 'Courier New', monospace;
}

.parcel-card__row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 13px;
  line-height: 1.75;
}

.parcel-card__k {
  flex: 0 0 62px;
  color: var(--es-text-3);
}

.parcel-card__v {
  flex: 1;
  min-width: 0;
  color: var(--es-text-1);
  word-break: break-all;
}

.mono {
  font-family: 'Consolas', 'Courier New', monospace;
}
</style>
