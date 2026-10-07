<template>
  <PageContainer
    title="通知记录"
    sub-title="到件通知、逾期催取与取件确认的发送历史，可用于核对是否已通知客户"
  >
    <!-- ==================== 指标卡片 ==================== -->
    <el-row :gutter="12" class="mb-12">
      <el-col v-for="item in statCards" :key="item.label" :xs="12" :sm="8" class="stat-col">
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

    <!-- ==================== 模拟发送说明（答辩演示用） ==================== -->
    <el-alert
      type="info"
      :closable="false"
      show-icon
      class="mb-12"
      title="短信发送在本系统中以写入通知记录的方式模拟（真实项目对接阿里云/腾讯云短信网关），失败记录可用于演示重发流程"
      description="当前模拟规则：接收号码为空或不合法时记为「发送失败」，并写入失败原因；其余情况记为「发送成功」。"
    />

    <!-- ==================== 条件筛选 ==================== -->
    <el-form :model="query" :inline="true" class="search-bar">
      <el-form-item label="运单号">
        <el-input v-model.trim="query.waybillNo" placeholder="请输入运单号" clearable style="width: 180px" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="接收手机号">
        <el-input v-model.trim="query.receiverPhone" placeholder="请输入手机号" clearable style="width: 150px" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="通知类型">
        <el-select v-model="query.notifyType" placeholder="全部" clearable style="width: 140px">
          <el-option v-for="item in NOTIFY_TYPE" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>

      <!-- 更多筛选条件：可折叠 -->
      <template v-if="showMore">
        <el-form-item label="通知渠道">
          <el-select v-model="query.channel" placeholder="全部" clearable style="width: 130px">
            <el-option v-for="item in NOTIFY_CHANNEL" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="发送结果">
          <el-select v-model="query.sendStatus" placeholder="全部" clearable style="width: 130px">
            <el-option v-for="item in SEND_STATUS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属驿站">
          <el-select
            v-model="query.stationId"
            :placeholder="adminMode ? '全部驿站' : '本驿站'"
            :disabled="!adminMode"
            clearable
            style="width: 180px"
          >
            <el-option v-for="item in stationList" :key="item.id" :label="item.stationName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="发送时间">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 250px"
          />
        </el-form-item>
      </template>

      <el-form-item>
        <el-button type="primary" :icon="Search" :loading="loading" @click="handleSearch">查询</el-button>
        <el-button :icon="RefreshLeft" @click="handleReset">重置</el-button>
        <el-button link type="primary" @click="showMore = !showMore">
          {{ showMore ? '收起筛选' : '展开筛选' }}
          <el-icon><component :is="showMore ? 'ArrowUp' : 'ArrowDown'" /></el-icon>
        </el-button>
      </el-form-item>
    </el-form>

    <!-- ==================== 工具条 ==================== -->
    <div class="table-toolbar">
      <div class="table-toolbar__left">
        <el-tag type="info" effect="plain">共 {{ total }} 条通知记录</el-tag>
        <el-tag type="info" effect="plain">当前口径：{{ stationLabel }}</el-tag>
        <el-tag v-if="!adminMode" type="warning" effect="plain">员工仅可查看本驿站通知记录</el-tag>
      </div>
      <div class="table-toolbar__right">
        <el-button :icon="Refresh" :loading="loading" @click="loadAll">刷新</el-button>
      </div>
    </div>

    <!-- ==================== 数据表格 ==================== -->
    <el-table v-loading="loading" :data="list" border stripe height="520">
      <el-table-column prop="waybillNo" label="运单号" width="170" fixed="left" show-overflow-tooltip>
        <template #default="{ row }">{{ row.waybillNo || '-' }}</template>
      </el-table-column>
      <el-table-column label="取件码" width="120" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.pickupCode" type="primary" effect="dark" class="pickup-code">{{ row.pickupCode }}</el-tag>
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>
      <el-table-column label="通知类型" width="120" align="center">
        <template #default="{ row }">
          <el-tag :type="dictType(NOTIFY_TYPE, row.notifyType)">
            {{ row.notifyTypeName || dictLabel(NOTIFY_TYPE, row.notifyType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="渠道" width="100" align="center">
        <template #default="{ row }">
          {{ row.channelName || dictLabel(NOTIFY_CHANNEL, row.channel) }}
        </template>
      </el-table-column>
      <el-table-column label="接收人" width="150">
        <template #default="{ row }">
          <div>{{ row.receiverName || '-' }}</div>
          <div class="text-muted cell-sub">{{ row.receiverPhone || '-' }}</div>
        </template>
      </el-table-column>
      <el-table-column prop="content" label="通知内容" min-width="260">
        <template #default="{ row }">
          <el-tooltip
            :disabled="!row.content"
            :content="row.content"
            placement="top"
            effect="dark"
            :show-after="200"
          >
            <span class="cell-content">{{ row.content || '-' }}</span>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column label="发送结果" width="110" align="center">
        <template #default="{ row }">
          <el-tooltip
            :disabled="row.sendStatus !== 'FAILED' || !row.failReason"
            :content="row.failReason || ''"
            placement="top"
          >
            <el-tag :type="dictType(SEND_STATUS, row.sendStatus)">
              {{ row.sendStatusName || dictLabel(SEND_STATUS, row.sendStatus) }}
            </el-tag>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column prop="stationName" label="所属驿站" width="160" show-overflow-tooltip>
        <template #default="{ row }">{{ row.stationName || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作人" width="110">
        <template #default="{ row }">{{ row.operatorName || '系统自动' }}</template>
      </el-table-column>
      <el-table-column label="发送时间" width="170">
        <template #default="{ row }">{{ formatDateTime(row.sendTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <div class="table-actions">
            <el-button link type="primary" size="small" @click="openDetail(row)">查看详情</el-button>
            <el-button
              v-perm="'notify:send'"
              link
              type="warning"
              size="small"
              :loading="resendingId === row.id && row.sendStatus === 'FAILED'"
              :disabled="row.sendStatus !== 'FAILED'"
              @click="handleResend(row)"
            >
              重发
            </el-button>
          </div>
        </template>
      </el-table-column>

      <template #empty>
        <el-empty description="暂无通知记录，请调整筛选条件后重试" :image-size="90" />
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

    <!-- ==================== 通知详情对话框 ==================== -->
    <el-dialog v-model="detailVisible" title="通知详情" width="720px" :close-on-click-modal="false">
      <el-descriptions v-if="detail.id" :column="2" border size="small">
        <el-descriptions-item label="运单号">{{ detail.waybillNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="取件码">
          <el-tag v-if="detail.pickupCode" type="primary" effect="dark">{{ detail.pickupCode }}</el-tag>
          <span v-else class="text-muted">-</span>
        </el-descriptions-item>
        <el-descriptions-item label="通知类型">
          <el-tag :type="dictType(NOTIFY_TYPE, detail.notifyType)">
            {{ detail.notifyTypeName || dictLabel(NOTIFY_TYPE, detail.notifyType) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="通知渠道">
          {{ detail.channelName || dictLabel(NOTIFY_CHANNEL, detail.channel) }}
        </el-descriptions-item>
        <el-descriptions-item label="接收人">{{ detail.receiverName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="接收手机号">{{ detail.receiverPhone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="发送结果">
          <el-tag :type="dictType(SEND_STATUS, detail.sendStatus)">
            {{ detail.sendStatusName || dictLabel(SEND_STATUS, detail.sendStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="所属驿站">{{ detail.stationName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ detail.operatorName || '系统自动' }}</el-descriptions-item>
        <el-descriptions-item label="发送时间">{{ formatDateTime(detail.sendTime) }}</el-descriptions-item>
        <el-descriptions-item label="失败原因" :span="2">
          <span :class="{ 'text-danger': !!detail.failReason }">{{ detail.failReason || '无' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="通知内容" :span="2">
          <div class="detail-content">{{ detail.content || '-' }}</div>
        </el-descriptions-item>
      </el-descriptions>

      <!-- 关联快件信息 -->
      <div class="detail__section-title">关联快件信息</div>
      <div v-loading="parcelLoading">
        <el-descriptions v-if="parcel.id" :column="2" border size="small">
          <el-descriptions-item label="快件状态">
            <el-tag :type="dictType(PARCEL_STATUS, parcel.status)">
              {{ parcel.statusName || PARCEL_STATUS_NAME[parcel.status] || '-' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="快件类型">
            {{ parcel.parcelTypeName || dictLabel(PARCEL_TYPE, parcel.parcelType) }}
          </el-descriptions-item>
          <el-descriptions-item label="快递公司">{{ parcel.expressCompany || '-' }}</el-descriptions-item>
          <el-descriptions-item label="货位编码">{{ parcel.shelfCode || '未分配' }}</el-descriptions-item>
          <el-descriptions-item label="入库时间">{{ formatDateTime(parcel.inTime) }}</el-descriptions-item>
          <el-descriptions-item label="取件时间">{{ formatDateTime(parcel.pickupTime) }}</el-descriptions-item>
          <el-descriptions-item label="免费保管天数">{{ formatNumber(parcel.overdueDays) }} 天</el-descriptions-item>
          <el-descriptions-item label="已保管天数">{{ formatNumber(parcel.storageDays) }} 天</el-descriptions-item>
          <el-descriptions-item label="逾期天数">
            <span class="text-danger text-bold">{{ formatNumber(parcel.overdueDayCount) }} 天</span>
          </el-descriptions-item>
          <el-descriptions-item label="应缴保管费">
            <span class="text-danger text-bold">{{ formatMoney(parcel.overdueFee, true) }}</span>
          </el-descriptions-item>
        </el-descriptions>
        <el-empty v-else-if="!parcelLoading" description="未查询到关联快件（可能已被删除）" :image-size="70" />
      </div>

      <!-- 该快件的全部通知历史 -->
      <div class="detail__section-title">该快件的通知历史</div>
      <div v-loading="historyLoading" class="detail__history">
        <el-table v-if="historyList.length" :data="historyList" border size="small" max-height="220">
          <el-table-column label="类型" width="110" align="center">
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
          <el-table-column prop="content" label="内容" min-width="200" show-overflow-tooltip />
        </el-table>
        <el-empty v-else description="该快件暂无其它通知记录" :image-size="70" />
      </div>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button
          v-if="detail.sendStatus === 'FAILED'"
          v-perm="'notify:send'"
          type="primary"
          :loading="resendingId === detail.id"
          @click="handleResend(detail)"
        >
          重发本条通知
        </el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup>
/**
 * 通知记录页
 * 功能：多条件筛选 + 分页表格 + 指标概览（今日发送 / 发送失败 / 通知总数）+ 详情（含关联快件与通知历史）+ 失败重发
 * 接口：GET /notifications/page、GET /notifications/parcel/{id}、POST /notifications、GET /parcels/{id}
 * 说明：短信发送由后端以「写入通知记录」的方式模拟（见 NotifyService#dispatch），
 *       对接真实网关时只需替换该方法的实现，本页无需改动。
 * 权限：notify:send 控制重发按钮；管理员可切换驿站，员工由后端强制收敛到本驿站。
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, RefreshLeft, Search } from '@element-plus/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import StatCard from '@/components/StatCard.vue'
import { getParcel } from '@/api/parcel'
import { listNotificationsByParcel, pageNotifications, sendNotification } from '@/api/notify'
import { listStations } from '@/api/station'
import { useUserStore } from '@/stores/user'
import {
  NOTIFY_CHANNEL,
  NOTIFY_TYPE,
  PARCEL_STATUS,
  PARCEL_STATUS_NAME,
  PARCEL_TYPE,
  SEND_STATUS,
  dictLabel,
  dictType
} from '@/utils/dict'
import { formatDate, formatDateTime, formatMoney, formatNumber, splitDateRange } from '@/utils/format'

const userStore = useUserStore()

/** 组件名：供 Layout 的 keep-alive include 使用 */
defineOptions({ name: 'NotifyList' })

/** 是否管理员：管理员可查看全部驿站，员工只能看本驿站（后端强制校验） */
const adminMode = computed(() => userStore.isAdmin)

/** 是否展开更多筛选条件 */
const showMore = ref(true)
/** 日期范围（与 query 中的 startTime/endTime 联动） */
const dateRange = ref([])

/** 查询条件（pageNum / pageSize 参与接口请求） */
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  waybillNo: '',
  receiverPhone: '',
  notifyType: '',
  channel: '',
  sendStatus: '',
  stationId: ''
})

/** 表格数据与分页总数 */
const list = ref([])
const total = ref(0)
const loading = ref(false)
/** 指标：今日发送 / 发送失败 / 通知总数（均为真实请求取回） */
const todayCount = ref(0)
const failedCount = ref(0)
const allCount = ref(0)
/** 正在重发的通知记录 id */
const resendingId = ref(null)
/** 驿站下拉数据 */
const stationList = ref([])

/** 驿站口径文案 */
const stationLabel = computed(() => {
  if (!adminMode.value) return userStore.stationName || '本驿站'
  if (!query.stationId) return '全部驿站'
  const hit = stationList.value.find((item) => String(item.id) === String(query.stationId))
  return hit ? hit.stationName : '所选驿站'
})

/** 顶部指标卡片 */
const statCards = computed(() => [
  {
    label: '今日发送',
    value: formatNumber(todayCount.value),
    unit: '条',
    icon: 'Promotion',
    preset: 'blue',
    subText: `口径：${stationLabel.value} · 今日发送时间`
  },
  {
    label: '发送失败',
    value: formatNumber(failedCount.value),
    unit: '条',
    icon: 'WarningFilled',
    preset: 'red',
    subText: `口径：${stationLabel.value} · 全部时间发送失败`
  },
  {
    label: '通知总数',
    value: formatNumber(allCount.value),
    unit: '条',
    icon: 'ChatDotSquare',
    preset: 'teal',
    subText: `口径：${stationLabel.value} · 全部时间通知记录`
  }
])

/* ------------------------------------------------------------------
 * 列表查询
 * ---------------------------------------------------------------- */

/**
 * 组装请求参数：过滤空值，避免后端把空字符串当作有效条件
 * @param {boolean} withPage 是否包含分页参数
 */
function buildParams(withPage = true) {
  const range = splitDateRange(dateRange.value)
  const params = {
    waybillNo: query.waybillNo,
    receiverPhone: query.receiverPhone,
    notifyType: query.notifyType,
    channel: query.channel,
    sendStatus: query.sendStatus,
    stationId: query.stationId,
    startTime: range.startTime,
    endTime: range.endTime
  }
  Object.keys(params).forEach((key) => {
    if (params[key] === '' || params[key] === null || params[key] === undefined) delete params[key]
  })
  if (withPage) {
    params.pageNum = query.pageNum
    params.pageSize = query.pageSize
  }
  return params
}

/** 加载通知记录分页数据 */
async function loadData() {
  loading.value = true
  try {
    const data = await pageNotifications(buildParams(true))
    list.value = (data && data.list) || []
    total.value = (data && data.total) || 0
  } catch (e) {
    // 请求失败：清空数据但保留分页信息，避免界面停留在旧数据
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

/**
 * 加载顶部指标：今日发送 / 发送失败 / 通知总数
 * 三个数字都通过 pageNotifications 只取 total 得到，与列表口径保持一致
 */
async function loadStats() {
  const today = formatDate(new Date().toISOString().slice(0, 10), '')
  const stationParams =
    query.stationId !== '' && query.stationId !== null && query.stationId !== undefined
      ? { stationId: query.stationId }
      : {}

  const tasks = [
    pageNotifications({ ...stationParams, pageNum: 1, pageSize: 1, startTime: today, endTime: today }),
    pageNotifications({ ...stationParams, pageNum: 1, pageSize: 1, sendStatus: 'FAILED' }),
    pageNotifications({ ...stationParams, pageNum: 1, pageSize: 1 })
  ]

  const results = await Promise.allSettled(tasks)
  const [todayRes, failedRes, allRes] = results
  todayCount.value = todayRes.status === 'fulfilled' ? (todayRes.value && todayRes.value.total) || 0 : 0
  failedCount.value = failedRes.status === 'fulfilled' ? (failedRes.value && failedRes.value.total) || 0 : 0
  allCount.value = allRes.status === 'fulfilled' ? (allRes.value && allRes.value.total) || 0 : 0
}

/** 刷新整页数据（列表 + 指标） */
function loadAll() {
  loadData()
  loadStats()
}

/** 点击查询：回到第一页 */
function handleSearch() {
  query.pageNum = 1
  loadData()
  loadStats()
}

/** 重置查询条件 */
function handleReset() {
  query.waybillNo = ''
  query.receiverPhone = ''
  query.notifyType = ''
  query.channel = ''
  query.sendStatus = ''
  query.stationId = ''
  query.pageNum = 1
  dateRange.value = []
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

/* ------------------------------------------------------------------
 * 详情对话框
 * ---------------------------------------------------------------- */
const detailVisible = ref(false)
const detail = ref({})
/** 关联快件信息 */
const parcel = ref({})
const parcelLoading = ref(false)
/** 该快件的全部通知历史 */
const historyList = ref([])
const historyLoading = ref(false)

/**
 * 打开通知详情：并行加载关联快件与该快件的通知历史
 * @param {Object} row 通知记录
 */
function openDetail(row) {
  detail.value = { ...row }
  parcel.value = {}
  historyList.value = []
  detailVisible.value = true
  loadParcel(row.parcelId)
  loadHistory(row.parcelId)
}

/**
 * 加载关联快件信息
 * @param {number} parcelId 快件ID
 */
async function loadParcel(parcelId) {
  if (!parcelId) {
    parcel.value = {}
    return
  }
  parcelLoading.value = true
  try {
    const data = await getParcel(parcelId)
    parcel.value = data || {}
  } catch (e) {
    // 快件可能已被删除：保持空对象，界面显示空状态
    parcel.value = {}
  } finally {
    parcelLoading.value = false
  }
}

/**
 * 加载某快件的全部通知记录
 * @param {number} parcelId 快件ID
 */
async function loadHistory(parcelId) {
  if (!parcelId) {
    historyList.value = []
    return
  }
  historyLoading.value = true
  try {
    const data = await listNotificationsByParcel(parcelId)
    historyList.value = Array.isArray(data) ? data : []
  } catch (e) {
    historyList.value = []
  } finally {
    historyLoading.value = false
  }
}

/* ------------------------------------------------------------------
 * 失败重发
 * ---------------------------------------------------------------- */

/**
 * 重发通知：沿用原通知的类型、渠道与内容重新调用发送接口
 * @param {Object} row 通知记录
 */
async function handleResend(row) {
  if (!row || !row.parcelId) {
    ElMessage.warning('该记录缺少关联快件，无法重发')
    return
  }
  if (row.sendStatus !== 'FAILED') {
    ElMessage.info('仅「发送失败」的通知需要重发')
    return
  }

  resendingId.value = row.id
  try {
    const record = await sendNotification({
      parcelId: row.parcelId,
      notifyType: row.notifyType,
      channel: row.channel,
      content: row.content
    })
    if (record && record.sendStatus === 'FAILED') {
      ElMessage.warning(`重发失败：${record.failReason || '接收号码不合法或为空'}`)
    } else {
      ElMessage.success('通知已重新发送')
      // 详情打开时同步刷新该快件的通知历史
      if (detailVisible.value) loadHistory(row.parcelId)
      loadAll()
    }
  } catch (e) {
    // 错误提示已由响应拦截器统一处理
  } finally {
    resendingId.value = null
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

/* 表格内的次要信息（手机号等） */
.cell-sub {
  font-size: 12px;
}

/* 通知内容限宽并截断，完整内容用 tooltip 展示 */
.cell-content {
  display: block;
  max-width: 360px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 详情对话框 */
.detail__section-title {
  margin: 20px 0 12px;
  padding-left: 8px;
  border-left: 4px solid var(--es-primary);
  font-weight: 600;
  color: var(--es-text-1);
}

.detail-content {
  line-height: 1.7;
  color: var(--es-text-2);
  word-break: break-all;
}
</style>
