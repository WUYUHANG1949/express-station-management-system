<template>
  <PageContainer
    title="逾期催取"
    sub-title="超过免费保管期的快件按逾期天数倒序排列，逾期保管费 2 元/天（超出免费保管期的天数 × 2 元）"
  >
    <!-- ==================== 指标卡片 ==================== -->
    <el-row :gutter="12" class="mb-12">
      <el-col v-for="item in statCards" :key="item.label" :xs="12" :sm="12" :md="6" class="stat-col">
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

    <!-- ==================== 条件筛选 ==================== -->
    <el-form :model="query" :inline="true" class="search-bar">
      <el-form-item label="所属驿站">
        <el-select
          v-model="query.stationId"
          :placeholder="adminMode ? '全部驿站' : '本驿站'"
          :disabled="!adminMode"
          clearable
          style="width: 200px"
          @change="handleSearch"
        >
          <el-option v-for="item in stationList" :key="item.id" :label="item.stationName" :value="item.id" />
        </el-select>
      </el-form-item>

      <el-form-item label="逾期天数">
        <el-radio-group v-model="query.minDays" @change="handleSearch">
          <el-radio-button v-for="item in MIN_DAYS_OPTIONS" :key="item.value" :value="item.value">
            {{ item.label }}
          </el-radio-button>
        </el-radio-group>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" :icon="Search" :loading="loading" @click="handleSearch">查询</el-button>
        <el-button :icon="RefreshLeft" @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- ==================== 工具条 ==================== -->
    <div class="table-toolbar">
      <div class="table-toolbar__left">
        <el-tag type="danger" effect="plain">共 {{ total }} 件逾期</el-tag>
        <el-tag type="info" effect="plain">
          当前口径：{{ stationLabel }} · 逾期 {{ query.minDays }} 天以上
        </el-tag>
        <el-tag v-if="!adminMode" type="warning" effect="plain">员工仅可查看与催取本驿站快件</el-tag>
      </div>
      <div class="table-toolbar__right">
        <el-button :icon="Refresh" :loading="loading" @click="loadAll">刷新</el-button>
        <el-button v-perm="'notify:send'" type="primary" :icon="Bell" :loading="batching" @click="handleBatchNotify">
          批量催取
        </el-button>
      </div>
    </div>

    <!-- ==================== 数据表格 ==================== -->
    <el-table v-loading="loading" :data="list" border stripe height="520">
      <el-table-column prop="waybillNo" label="运单号" width="170" fixed="left" show-overflow-tooltip />
      <el-table-column label="取件码" width="120" align="center">
        <template #default="{ row }">
          <el-tag type="primary" effect="dark" class="pickup-code">{{ row.pickupCode || '-' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="expressCompany" label="快递公司" width="110" />
      <el-table-column label="类型" width="90" align="center">
        <template #default="{ row }">
          {{ row.parcelTypeName || dictLabel(PARCEL_TYPE, row.parcelType) }}
        </template>
      </el-table-column>
      <el-table-column prop="receiverName" label="收件人" width="100">
        <template #default="{ row }">{{ row.receiverName || '-' }}</template>
      </el-table-column>
      <el-table-column prop="receiverPhone" label="手机号" width="130">
        <template #default="{ row }">{{ row.receiverPhone || '-' }}</template>
      </el-table-column>
      <el-table-column prop="stationName" label="所属驿站" width="160" show-overflow-tooltip>
        <template #default="{ row }">{{ row.stationName || '-' }}</template>
      </el-table-column>
      <el-table-column label="货位" width="100" align="center">
        <template #default="{ row }">{{ row.shelfCode || '未分配' }}</template>
      </el-table-column>
      <el-table-column label="入库时间" width="170">
        <template #default="{ row }">{{ formatDateTime(row.inTime) }}</template>
      </el-table-column>
      <el-table-column label="已保管天数" width="110" align="center">
        <template #default="{ row }">{{ formatNumber(row.storageDays) }} 天</template>
      </el-table-column>
      <el-table-column label="逾期天数" width="110" align="center">
        <template #default="{ row }">
          <el-tag type="danger" effect="dark" :class="overdueClass(row.overdueDayCount)">
            {{ formatNumber(row.overdueDayCount) }} 天
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="应缴保管费" width="120" align="right">
        <template #default="{ row }">
          <span class="text-danger text-bold">{{ formatMoney(row.overdueFee, true) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <div class="table-actions">
            <el-tooltip :disabled="!notifiedIds.has(row.id)" content="该快件今日已催取，同一快件当天不会重复发送" placement="top">
              <span>
                <el-button
                  v-perm="'notify:send'"
                  link
                  type="primary"
                  size="small"
                  :loading="notifyingId === row.id"
                  :disabled="notifiedIds.has(row.id)"
                  @click="handleNotify(row)"
                >
                  {{ notifiedIds.has(row.id) ? '今日已催取' : '催取' }}
                </el-button>
              </span>
            </el-tooltip>
            <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
          </div>
        </template>
      </el-table-column>

      <template #empty>
        <el-empty description="太好了，当前没有逾期未取的快件" :image-size="90" />
      </template>
    </el-table>

    <!-- ==================== 分页 ==================== -->
    <div class="pagination-wrap">
      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        background
        :page-sizes="[10, 20, 50]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="loadData"
      />
    </div>

    <!-- ==================== 快件详情抽屉 ==================== -->
    <el-drawer v-model="detailVisible" title="逾期快件详情" size="620px" :destroy-on-close="true">
      <div v-loading="detailLoading" class="detail">
        <el-alert
          type="warning"
          :closable="false"
          show-icon
          class="mb-12"
          :title="`已逾期 ${formatNumber(detail.overdueDayCount)} 天，应缴逾期保管费 ${formatMoney(detail.overdueFee, true)}`"
          description="保管费口径：超出免费保管期的天数 × 2 元/天"
        />

        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="运单号">{{ detail.waybillNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="取件码">
            <el-tag type="primary" effect="dark">{{ detail.pickupCode || '-' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="快件状态">
            <el-tag :type="dictType(PARCEL_STATUS, detail.status)">
              {{ detail.statusName || PARCEL_STATUS_NAME[detail.status] || '-' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="快件类型">
            {{ detail.parcelTypeName || dictLabel(PARCEL_TYPE, detail.parcelType) }}
          </el-descriptions-item>
          <el-descriptions-item label="快递公司">{{ detail.expressCompany || '-' }}</el-descriptions-item>
          <el-descriptions-item label="所属驿站">{{ detail.stationName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="货位编码">{{ detail.shelfCode || '未分配' }}</el-descriptions-item>
          <el-descriptions-item label="重量">{{ formatWeight(detail.weight) }}</el-descriptions-item>
          <el-descriptions-item label="收件人">{{ detail.receiverName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="收件人手机号">{{ detail.receiverPhone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="免费保管天数">{{ formatNumber(detail.overdueDays) }} 天</el-descriptions-item>
          <el-descriptions-item label="已保管天数">{{ formatNumber(detail.storageDays) }} 天</el-descriptions-item>
          <el-descriptions-item label="逾期天数">
            <span class="text-danger text-bold">{{ formatNumber(detail.overdueDayCount) }} 天</span>
          </el-descriptions-item>
          <el-descriptions-item label="应缴保管费">
            <span class="text-danger text-bold">{{ formatMoney(detail.overdueFee, true) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="运费">{{ formatMoney(detail.freight, true) }}</el-descriptions-item>
          <el-descriptions-item label="入库时间">{{ formatDateTime(detail.inTime) }}</el-descriptions-item>
          <el-descriptions-item label="操作员">{{ detail.operatorName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDateTime(detail.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ detail.remark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <!-- 轨迹时间线 -->
        <div class="detail__trace-title">快件轨迹</div>
        <div v-loading="traceLoading" class="detail__trace">
          <el-timeline v-if="traceList.length">
            <el-timeline-item
              v-for="(item, index) in traceList"
              :key="item.id || index"
              :timestamp="formatDateTime(item.operateTime)"
              :type="index === 0 ? 'primary' : ''"
              :hollow="index !== 0"
              placement="top"
            >
              <div class="trace-item">
                <el-tag size="small" effect="plain" class="trace-item__tag">
                  {{ TRACE_TYPE_NAME[item.operateType] || item.operateType || '轨迹' }}
                </el-tag>
                <div class="trace-item__desc">{{ item.operateDesc || '-' }}</div>
                <div class="text-muted">操作人：{{ item.operatorName || '-' }}</div>
              </div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无轨迹记录" :image-size="70" />
        </div>

        <!-- 该快件的通知记录 -->
        <div class="detail__trace-title">催取通知记录</div>
        <div v-loading="notifyLoading" class="detail__trace">
          <el-table v-if="notifyList.length" :data="notifyList" border size="small">
            <el-table-column label="通知类型" width="110" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="dictType(NOTIFY_TYPE, row.notifyType)">
                  {{ row.notifyTypeName || dictLabel(NOTIFY_TYPE, row.notifyType) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="渠道" width="90" align="center">
              <template #default="{ row }">
                {{ row.channelName || dictLabel(NOTIFY_CHANNEL, row.channel) }}
              </template>
            </el-table-column>
            <el-table-column label="结果" width="100" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="dictType(SEND_STATUS, row.sendStatus)">
                  {{ row.sendStatusName || dictLabel(SEND_STATUS, row.sendStatus) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="发送时间" width="170">
              <template #default="{ row }">{{ formatDateTime(row.sendTime) }}</template>
            </el-table-column>
          </el-table>
          <el-empty v-else description="该快件暂无通知记录" :image-size="70" />
        </div>

        <div class="detail__footer">
          <el-button @click="detailVisible = false">关闭</el-button>
          <el-button
            v-perm="'notify:send'"
            type="primary"
            :icon="Bell"
            :disabled="notifiedIds.has(detail.id)"
            @click="handleNotify(detail)"
          >
            {{ notifiedIds.has(detail.id) ? '今日已催取' : '发送催取通知' }}
          </el-button>
        </div>
      </div>
    </el-drawer>
  </PageContainer>
</template>

<script setup>
/**
 * 逾期催取页
 * 功能：逾期件分层筛选（1/3/7/15 天以上）+ 指标概览 + 分页表格 + 单个催取 + 批量催取 + 详情（轨迹与通知历史）
 * 接口：GET /parcels/overdue/page、POST /notifications/batch-overdue、POST /notifications、GET /notifications/parcel/{id}
 * 说明：后端 pageOverdueParcels 仅支持 stationId 与 minDays 两个条件，
 *       因此本页不做「收件人姓名/手机号」筛选（避免给出无效条件误导使用者）。
 * 权限：notify:send 控制催取相关按钮；管理员可切换驿站，员工由后端强制收敛到本驿站。
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Bell, Refresh, RefreshLeft, Search } from '@element-plus/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import StatCard from '@/components/StatCard.vue'
import { getParcel, getParcelTraces, pageOverdueParcels } from '@/api/parcel'
import { batchNotifyOverdue, listNotificationsByParcel, pageNotifications, sendNotification } from '@/api/notify'
import { listStations } from '@/api/station'
import { useUserStore } from '@/stores/user'
import {
  NOTIFY_CHANNEL,
  NOTIFY_TYPE,
  PARCEL_STATUS,
  PARCEL_STATUS_NAME,
  PARCEL_TYPE,
  SEND_STATUS,
  TRACE_TYPE_NAME,
  dictLabel,
  dictType
} from '@/utils/dict'
import { formatDate, formatDateTime, formatMoney, formatNumber, formatWeight } from '@/utils/format'

const userStore = useUserStore()

/** 组件名：供 Layout 的 keep-alive include 使用 */
defineOptions({ name: 'ParcelOverdue' })

/** 逾期天数分段选项 */
const MIN_DAYS_OPTIONS = [
  { value: 1, label: '1 天以上' },
  { value: 3, label: '3 天以上' },
  { value: 7, label: '7 天以上' },
  { value: 15, label: '15 天以上' }
]

/** 是否管理员：管理员可查看全部驿站，员工只能看本驿站（后端强制校验） */
const adminMode = computed(() => userStore.isAdmin)

/** 查询条件（pageNum / pageSize / stationId / minDays 参与接口请求） */
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  stationId: '',
  minDays: 1
})

/** 表格数据与分页总数 */
const list = ref([])
const total = ref(0)
const loading = ref(false)
/** 逾期 3 天以上的总件数（独立请求获取，真实数据） */
const overdue3Total = ref(0)
/** 今日已发送的催取通知条数（真实请求获取，取失败时为 null，展示为「-」） */
const todayNotifyCount = ref(null)
/** 批量催取中 */
const batching = ref(false)
/** 正在单条催取的快件 id */
const notifyingId = ref(null)
/** 本会话内已成功催取的快件 id 集合（同一快件当天不重复发送） */
const notifiedIds = ref(new Set())
/** 驿站下拉数据 */
const stationList = ref([])

/** 驿站口径文案 */
const stationLabel = computed(() => {
  if (!adminMode.value) return userStore.stationName || '本驿站'
  if (!query.stationId) return '全部驿站'
  const hit = stationList.value.find((item) => String(item.id) === String(query.stationId))
  return hit ? hit.stationName : '所选驿站'
})

/** 当前页应缴保管费合计（口径：当前页列表求和） */
const pageFeeTotal = computed(() =>
  (list.value || []).reduce((sum, row) => sum + (Number(row.overdueFee) || 0), 0)
)

/** 顶部指标卡片 */
const statCards = computed(() => [
  {
    label: '逾期总件数',
    value: formatNumber(total.value),
    unit: '件',
    icon: 'AlarmClock',
    preset: 'red',
    subText: `口径：${stationLabel.value} · 逾期 ${query.minDays} 天以上`
  },
  {
    label: '逾期 3 天以上',
    value: formatNumber(overdue3Total.value),
    unit: '件',
    icon: 'Warning',
    preset: 'orange',
    subText: `口径：${stationLabel.value} · 逾期满 3 天`
  },
  {
    label: '应缴保管费合计',
    value: formatMoney(pageFeeTotal.value, true),
    icon: 'Money',
    preset: 'orange',
    subText: '口径：当前页列表逾期保管费求和'
  },
  {
    label: '今日已发催取',
    value: todayNotifyCount.value === null ? '-' : formatNumber(todayNotifyCount.value),
    unit: todayNotifyCount.value === null ? '' : '条',
    icon: 'ChatDotSquare',
    preset: 'teal',
    subText: todayNotifyCount.value === null ? '口径：今日逾期催取通知条数（需 notify:list 权限）' : '口径：今日发送的逾期催取通知条数'
  }
])

/* ------------------------------------------------------------------
 * 列表查询
 * ---------------------------------------------------------------- */

/** 组装请求参数：员工不传 stationId，由后端按登录用户收敛驿站范围 */
function buildParams() {
  const params = { pageNum: query.pageNum, pageSize: query.pageSize, minDays: query.minDays }
  if (query.stationId !== '' && query.stationId !== null && query.stationId !== undefined) {
    params.stationId = query.stationId
  }
  return params
}

/** 加载逾期件分页数据 */
async function loadData() {
  loading.value = true
  try {
    const data = await pageOverdueParcels(buildParams())
    list.value = (data && data.list) || []
    total.value = (data && data.total) || 0
  } catch (e) {
    // 请求失败：清空数据，避免界面停留在旧数据
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

/** 加载「逾期 3 天以上」总件数（独立小请求，只取 total） */
async function loadOverdue3Total() {
  try {
    const params = { pageNum: 1, pageSize: 1, minDays: 3 }
    if (query.stationId !== '' && query.stationId !== null && query.stationId !== undefined) {
      params.stationId = query.stationId
    }
    const data = await pageOverdueParcels(params)
    overdue3Total.value = (data && data.total) || 0
  } catch (e) {
    overdue3Total.value = 0
  }
}

/** 加载今日已发送的催取通知条数（需要 notify:list 权限，无权限时展示「-」） */
async function loadTodayNotifyCount() {
  const today = formatDate(new Date().toISOString().slice(0, 10), '')
  try {
    const params = {
      pageNum: 1,
      pageSize: 1,
      notifyType: 'OVERDUE',
      startTime: today,
      endTime: today
    }
    if (query.stationId !== '' && query.stationId !== null && query.stationId !== undefined) {
      params.stationId = query.stationId
    }
    const data = await pageNotifications(params)
    todayNotifyCount.value = (data && data.total) || 0
  } catch (e) {
    todayNotifyCount.value = null
  }
}

/** 刷新整页数据（列表 + 指标） */
function loadAll() {
  loadData()
  loadOverdue3Total()
  loadTodayNotifyCount()
}

/** 点击查询：回到第一页 */
function handleSearch() {
  query.pageNum = 1
  loadAll()
}

/** 重置查询条件 */
function handleReset() {
  query.stationId = ''
  query.minDays = 1
  query.pageNum = 1
  loadAll()
}

/** 切换每页条数：回到第一页重新查询 */
function handleSizeChange() {
  query.pageNum = 1
  loadData()
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
 * 逾期天数的视觉分级：1-2 天橙色、3-6 天深橙、7 天以上红色加粗
 * @param {number} days 逾期天数
 */
function overdueClass(days) {
  const num = Number(days) || 0
  if (num >= 7) return 'overdue-level-3'
  if (num >= 3) return 'overdue-level-2'
  return 'overdue-level-1'
}

/* ------------------------------------------------------------------
 * 催取通知
 * ---------------------------------------------------------------- */

/** 标记某快件今日已催取 */
function markNotified(id) {
  const next = new Set(notifiedIds.value)
  next.add(id)
  notifiedIds.value = next
}

/**
 * 单条催取：调用 POST /notifications
 * @param {Object} row 快件对象（含 id / parcelId）
 */
async function handleNotify(row) {
  const parcelId = row.id || row.parcelId
  if (!parcelId) {
    ElMessage.warning('缺少快件标识，无法发送催取通知')
    return
  }
  if (notifiedIds.value.has(parcelId)) {
    ElMessage.info('该快件今日已催取，同一快件当天不会重复发送')
    return
  }

  notifyingId.value = parcelId
  try {
    const record = await sendNotification({ parcelId, notifyType: 'OVERDUE', channel: 'SMS' })
    if (record && record.sendStatus === 'FAILED') {
      ElMessage.warning(`催取通知已生成记录，但发送失败：${record.failReason || '接收号码不合法或为空'}`)
    } else {
      markNotified(parcelId)
      ElMessage.success('催取通知已发送')
    }
    // 详情抽屉打开时同步刷新该快件的通知历史与指标
    if (detailVisible.value) loadDetailNotifies(parcelId)
    loadTodayNotifyCount()
  } catch (e) {
    // 错误提示已由响应拦截器统一处理
  } finally {
    notifyingId.value = null
  }
}

/** 批量催取：按当前筛选范围逐条发送，同一快件当天不会重复发送 */
async function handleBatchNotify() {
  try {
    await ElMessageBox.confirm(
      `将给当前筛选范围内逾期 ${query.minDays} 天以上的快件逐条发送催取短信，同一快件当天不会重复发送。是否继续？`,
      '批量催取确认',
      { confirmButtonText: '确定发送', cancelButtonText: '取消', type: 'warning' }
    )
  } catch (e) {
    return // 用户取消
  }

  batching.value = true
  try {
    const params = { minDays: query.minDays }
    if (query.stationId !== '' && query.stationId !== null && query.stationId !== undefined) {
      params.stationId = query.stationId
    }
    const sent = await batchNotifyOverdue(params)
    const count = Number(sent) || 0
    if (count > 0) {
      ElMessage.success(`本次实际发送 ${count} 条催取通知`)
    } else {
      ElMessage.info('本次没有需要发送的催取通知（可能今日均已催取过）')
    }
    // 批量发送后本页数据已可能变化，整体刷新
    notifiedIds.value = new Set()
    loadAll()
  } catch (e) {
    // 错误提示已由响应拦截器统一处理
  } finally {
    batching.value = false
  }
}

/* ------------------------------------------------------------------
 * 详情抽屉
 * ---------------------------------------------------------------- */
const detailVisible = ref(false)
const detailLoading = ref(false)
const traceLoading = ref(false)
const notifyLoading = ref(false)
const detail = ref({})
const traceList = ref([])
const notifyList = ref([])

/**
 * 打开详情抽屉：并行请求快件详情、轨迹与通知记录
 * @param {Object} row 表格行数据
 */
async function openDetail(row) {
  detailVisible.value = true
  detail.value = { ...row }
  traceList.value = []
  notifyList.value = []
  detailLoading.value = true
  traceLoading.value = true

  try {
    const data = await getParcel(row.id)
    if (data) detail.value = data
  } catch (e) {
    // 详情请求失败时保留列表行数据，保证抽屉有内容
  } finally {
    detailLoading.value = false
  }

  try {
    const traces = await getParcelTraces(row.id)
    traceList.value = Array.isArray(traces) ? traces : []
  } catch (e) {
    traceList.value = []
  } finally {
    traceLoading.value = false
  }

  loadDetailNotifies(row.id)
}

/**
 * 加载某快件的通知记录
 * @param {number} parcelId
 */
async function loadDetailNotifies(parcelId) {
  notifyLoading.value = true
  try {
    const data = await listNotificationsByParcel(parcelId)
    notifyList.value = Array.isArray(data) ? data : []
  } catch (e) {
    notifyList.value = []
  } finally {
    notifyLoading.value = false
  }
}

onMounted(() => {
  loadStations()
  loadAll()
})
</script>

<style scoped>
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.stat-col {
  margin-bottom: 12px;
}

.pickup-code {
  font-family: Consolas, Monaco, monospace;
  letter-spacing: 1px;
}

/* 逾期天数视觉分级：1-2 天橙、3-6 天深橙、7 天以上红色加粗 */
.overdue-level-1 {
  font-weight: 600;
  background: #f59f00;
  border-color: #f59f00;
}

.overdue-level-2 {
  font-weight: 700;
  background: #d9480f;
  border-color: #d9480f;
}

.overdue-level-3 {
  font-weight: 700;
  background: #c92a2a;
  border-color: #c92a2a;
  box-shadow: 0 0 0 3px rgba(201, 42, 42, 0.16);
}

/* 详情抽屉 */
.detail__trace-title {
  margin: 20px 0 12px;
  padding-left: 8px;
  border-left: 4px solid var(--es-primary);
  font-weight: 600;
  color: var(--es-text-1);
}

.trace-item__tag {
  margin-bottom: 4px;
}

.trace-item__desc {
  font-size: 13px;
  color: var(--es-text-2);
  margin-bottom: 2px;
}

.detail__footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 24px;
  padding-top: 14px;
  border-top: 1px solid var(--es-border);
}
</style>
