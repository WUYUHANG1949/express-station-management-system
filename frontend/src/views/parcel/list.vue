<template>
  <PageContainer title="快件查询" sub-title="按条件检索快件台账，支持详情、编辑、派送、删除与导出">
    <!-- ==================== 条件筛选 ==================== -->
    <el-form :model="query" :inline="true" class="search-bar">
      <el-form-item label="运单号">
        <el-input v-model.trim="query.waybillNo" placeholder="请输入运单号" clearable style="width: 180px" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="取件码">
        <el-input v-model.trim="query.pickupCode" placeholder="请输入取件码" clearable style="width: 140px" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="收件人">
        <el-input v-model.trim="query.receiverName" placeholder="请输入收件人姓名" clearable style="width: 140px" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="手机号">
        <el-input v-model.trim="query.receiverPhone" placeholder="请输入手机号" clearable style="width: 150px" @keyup.enter="handleSearch" />
      </el-form-item>

      <!-- 更多筛选条件：可折叠 -->
      <template v-if="showMore">
        <el-form-item label="快递公司">
          <el-select v-model="query.expressCompany" placeholder="全部" clearable style="width: 150px">
            <el-option v-for="item in EXPRESS_COMPANIES" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="快件状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="item in PARCEL_STATUS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="快件类型">
          <el-select v-model="query.parcelType" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="item in PARCEL_TYPE" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属驿站">
          <el-select v-model="query.stationId" placeholder="全部" clearable style="width: 180px">
            <el-option v-for="item in stationList" :key="item.id" :label="item.stationName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="入库时间">
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
        <el-tag type="info" effect="plain">共 {{ total }} 条快件记录</el-tag>
        <el-tag v-if="!isStaff" type="warning" effect="plain">普通用户仅可查看本人名下快件</el-tag>
      </div>
      <div class="table-toolbar__right">
        <el-button :icon="Refresh" :loading="loading" @click="loadData">刷新</el-button>
        <el-button v-perm="'parcel:export'" type="success" :icon="Download" :loading="exporting" @click="handleExport">
          导出台账
        </el-button>
      </div>
    </div>

    <!-- ==================== 数据表格 ==================== -->
    <el-table v-loading="loading" :data="list" border stripe height="520">
      <el-table-column prop="waybillNo" label="运单号" width="170" fixed="left" show-overflow-tooltip />
      <el-table-column label="取件码" width="120" align="center">
        <template #default="{ row }">
          <el-tag type="primary" effect="dark" class="pickup-code" @click="copyText(row.pickupCode)">
            {{ row.pickupCode || '-' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="expressCompany" label="快递公司" width="110" />
      <el-table-column label="类型" width="90" align="center">
        <template #default="{ row }">
          {{ row.parcelTypeName || dictLabel(PARCEL_TYPE, row.parcelType) }}
        </template>
      </el-table-column>
      <el-table-column prop="receiverName" label="收件人" width="100" />
      <el-table-column prop="receiverPhone" label="手机号" width="130" />
      <el-table-column prop="stationName" label="所属驿站" width="160" show-overflow-tooltip />
      <el-table-column prop="shelfCode" label="货位" width="100" align="center">
        <template #default="{ row }">{{ row.shelfCode || '未分配' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="dictType(PARCEL_STATUS, row.status)">
            {{ row.statusName || PARCEL_STATUS_NAME[row.status] || row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="入库时间" width="170">
        <template #default="{ row }">{{ formatDateTime(row.inTime || row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="保管天数" width="90" align="center">
        <template #default="{ row }">{{ formatNumber(row.storageDays) }}</template>
      </el-table-column>
      <el-table-column label="逾期费" width="100" align="right">
        <template #default="{ row }">
          <span :class="{ 'text-danger': Number(row.overdueFee) > 0 }">{{ formatMoney(row.overdueFee, true) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="300" fixed="right">
        <template #default="{ row }">
          <div class="table-actions">
            <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
            <el-button v-perm="'parcel:edit'" link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button
              v-perm="'parcel:deliver'"
              link
              type="warning"
              size="small"
              :disabled="row.status !== 'IN_STORE'"
              :title="row.status !== 'IN_STORE' ? '仅「在库待取」状态可派送' : ''"
              @click="handleDeliver(row)"
            >
              派送
            </el-button>
            <el-button v-perm="'parcel:print'" link type="primary" size="small" @click="handlePrintTicket(row)">
              打印小票
            </el-button>
            <el-button v-perm="'parcel:delete'" link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </div>
        </template>
      </el-table-column>

      <template #empty>
        <el-empty description="暂无快件数据，请调整筛选条件后重试" :image-size="90" />
      </template>
    </el-table>

    <!-- ==================== 分页 ==================== -->
    <div class="pagination-wrap">
      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        background
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="loadData"
      />
    </div>

    <!-- ==================== 详情抽屉 ==================== -->
    <el-drawer v-model="detailVisible" title="快件详情" size="620px" :destroy-on-close="true">
      <div v-loading="detailLoading" class="detail">
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
          <el-descriptions-item label="运费">{{ formatMoney(detail.freight, true) }}</el-descriptions-item>
          <el-descriptions-item label="免费保管天数">{{ formatNumber(detail.overdueDays) }} 天</el-descriptions-item>
          <el-descriptions-item label="已保管天数">{{ formatNumber(detail.storageDays) }} 天</el-descriptions-item>
          <el-descriptions-item label="逾期费用">
            <span :class="{ 'text-danger': Number(detail.overdueFee) > 0 }">{{ formatMoney(detail.overdueFee, true) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="入库时间">{{ formatDateTime(detail.inTime) }}</el-descriptions-item>
          <el-descriptions-item label="取件时间">{{ formatDateTime(detail.pickupTime) }}</el-descriptions-item>
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
      </div>
    </el-drawer>

    <!-- ==================== 编辑对话框 ==================== -->
    <el-dialog v-model="editVisible" title="编辑快件" width="620px" :close-on-click-modal="false" @closed="resetEditForm">
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="110px">
        <el-form-item label="运单号">
          <el-input :model-value="editForm.waybillNo" disabled />
        </el-form-item>
        <el-form-item label="收件人姓名" prop="receiverName">
          <el-input v-model.trim="editForm.receiverName" placeholder="请输入收件人姓名" />
        </el-form-item>
        <el-form-item label="收件人手机号" prop="receiverPhone">
          <el-input v-model.trim="editForm.receiverPhone" placeholder="请输入 11 位手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="快件类型" prop="parcelType">
          <el-select v-model="editForm.parcelType" placeholder="请选择快件类型" style="width: 100%">
            <el-option v-for="item in PARCEL_TYPE" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="货位" prop="shelfId">
          <el-select v-model="editForm.shelfId" placeholder="请选择货位" clearable style="width: 100%">
            <el-option
              v-for="item in shelfOptions"
              :key="item.id"
              :label="`${item.shelfCode}（剩余 ${item.freeCount ?? '-'} / 容量 ${item.capacity ?? '-'}）`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="免费保管天数" prop="overdueDays">
          <el-input-number v-model="editForm.overdueDays" :min="0" :max="365" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="editForm.remark" type="textarea" :rows="3" maxlength="200" show-word-limit placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="editSubmitting" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- ==================== 取件小票（仅用于打印，放在屏幕外，不影响布局） ==================== -->
    <div class="ticket-holder">
      <ParcelTicket ref="ticketRef" :parcel="ticketParcel" />
    </div>
  </PageContainer>
</template>

<script setup>
/**
 * 快件查询页
 * 功能：多条件筛选 + 分页表格 + 详情（含轨迹时间线）+ 编辑 + 派送 + 打印取件小票 + 删除 + 导出台账
 * 权限：parcel:edit / parcel:deliver / parcel:print / parcel:delete / parcel:export 通过 v-perm 控制
 */
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, Refresh, RefreshLeft, Search } from '@element-plus/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import ParcelTicket from '@/components/ParcelTicket.vue'
import {
  deleteParcel,
  deliverParcel,
  exportParcels,
  getParcel,
  getParcelTraces,
  pageParcels,
  updateParcel
} from '@/api/parcel'
import { listShelves } from '@/api/shelf'
import { listStations } from '@/api/station'
import { useUserStore } from '@/stores/user'
import {
  EXPRESS_COMPANIES,
  PARCEL_STATUS,
  PARCEL_STATUS_NAME,
  PARCEL_TYPE,
  TRACE_TYPE_NAME,
  dictLabel,
  dictType
} from '@/utils/dict'
import { formatDateTime, formatMoney, formatNumber, formatWeight, splitDateRange } from '@/utils/format'
import { downloadBlob } from '@/utils/download'

const userStore = useUserStore()

/** 组件名：供 Layout 的 keep-alive include 使用（多个 list.vue 同名会互相顶替，故显式命名） */
defineOptions({ name: 'ParcelList' })

/** 是否员工/管理员（用于提示文案） */
const isStaff = computed(() => userStore.isAdmin || (userStore.roles || []).includes('STAFF'))

/** 是否展开更多筛选条件 */
const showMore = ref(false)
/** 日期范围（与 query 中的 startTime/endTime 联动） */
const dateRange = ref([])

/** 查询条件（pageNum / pageSize 参与接口请求） */
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  waybillNo: '',
  pickupCode: '',
  receiverName: '',
  receiverPhone: '',
  expressCompany: '',
  status: '',
  parcelType: '',
  stationId: '',
  startTime: '',
  endTime: ''
})

/** 表格数据与分页总数 */
const list = ref([])
const total = ref(0)
const loading = ref(false)
/** 导出中 */
const exporting = ref(false)
/** 驿站下拉数据 */
const stationList = ref([])

/* ------------------------------------------------------------------
 * 列表查询
 * ---------------------------------------------------------------- */

/**
 * 组装请求参数：过滤空值，避免后端把空字符串当作有效条件
 * @param {boolean} withPage 是否包含分页参数（导出时不需要）
 */
function buildParams(withPage = true) {
  const range = splitDateRange(dateRange.value)
  const params = {
    waybillNo: query.waybillNo,
    pickupCode: query.pickupCode,
    receiverName: query.receiverName,
    receiverPhone: query.receiverPhone,
    expressCompany: query.expressCompany,
    status: query.status,
    parcelType: query.parcelType,
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

/** 加载快件分页数据 */
async function loadData() {
  loading.value = true
  try {
    const data = await pageParcels(buildParams(true))
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

/** 点击查询：回到第一页 */
function handleSearch() {
  query.pageNum = 1
  loadData()
}

/** 重置查询条件 */
function handleReset() {
  query.waybillNo = ''
  query.pickupCode = ''
  query.receiverName = ''
  query.receiverPhone = ''
  query.expressCompany = ''
  query.status = ''
  query.parcelType = ''
  query.stationId = ''
  query.pageNum = 1
  dateRange.value = []
  loadData()
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
 * 复制文本到剪贴板（取件码一键复制，方便告知客户）
 * @param {string} text
 */
async function copyText(text) {
  if (!text) return
  try {
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(String(text))
    } else {
      const input = document.createElement('input')
      input.value = String(text)
      document.body.appendChild(input)
      input.select()
      document.execCommand('copy')
      document.body.removeChild(input)
    }
    ElMessage.success(`已复制：${text}`)
  } catch (e) {
    ElMessage.warning('复制失败，请手动选择复制')
  }
}

/* ------------------------------------------------------------------
 * 详情抽屉
 * ---------------------------------------------------------------- */
const detailVisible = ref(false)
const detailLoading = ref(false)
const traceLoading = ref(false)
const detail = ref({})
const traceList = ref([])

/**
 * 打开详情抽屉：并行请求详情与轨迹
 * @param {Object} row 表格行数据
 */
async function openDetail(row) {
  detailVisible.value = true
  detail.value = { ...row }
  traceList.value = []
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
}

/* ------------------------------------------------------------------
 * 编辑对话框
 * ---------------------------------------------------------------- */
const editVisible = ref(false)
const editSubmitting = ref(false)
const editFormRef = ref(null)
const shelfOptions = ref([])

/** 编辑表单（仅契约 3.8 允许修改的字段） */
const editForm = reactive({
  id: null,
  waybillNo: '',
  receiverName: '',
  receiverPhone: '',
  parcelType: 'NORMAL',
  shelfId: null,
  overdueDays: 3,
  remark: ''
})

/** 编辑表单校验规则 */
const editRules = {
  receiverName: [{ required: true, message: '请输入收件人姓名', trigger: 'blur' }],
  receiverPhone: [
    { required: true, message: '请输入收件人手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  parcelType: [{ required: true, message: '请选择快件类型', trigger: 'change' }]
}

/**
 * 打开编辑对话框
 * @param {Object} row 表格行数据
 */
async function openEdit(row) {
  editForm.id = row.id
  editForm.waybillNo = row.waybillNo
  editForm.receiverName = row.receiverName
  editForm.receiverPhone = row.receiverPhone
  editForm.parcelType = row.parcelType || 'NORMAL'
  editForm.shelfId = row.shelfId || null
  editForm.overdueDays = row.overdueDays ?? 3
  editForm.remark = row.remark || ''
  editVisible.value = true

  // 拉取该快件所属驿站的可选货位
  shelfOptions.value = []
  if (row.stationId) {
    try {
      const data = await listShelves(row.stationId)
      shelfOptions.value = Array.isArray(data) ? data : []
    } catch (e) {
      shelfOptions.value = []
    }
  }
}

/** 关闭后重置编辑表单，避免残留上一次数据 */
function resetEditForm() {
  editFormRef.value?.clearValidate()
  editForm.id = null
  editForm.waybillNo = ''
  editForm.receiverName = ''
  editForm.receiverPhone = ''
  editForm.parcelType = 'NORMAL'
  editForm.shelfId = null
  editForm.overdueDays = 3
  editForm.remark = ''
  shelfOptions.value = []
}

/** 提交编辑 */
async function submitEdit() {
  if (!editFormRef.value) return
  try {
    await editFormRef.value.validate()
  } catch (e) {
    return
  }

  editSubmitting.value = true
  try {
    await updateParcel(editForm.id, {
      receiverName: editForm.receiverName,
      receiverPhone: editForm.receiverPhone,
      parcelType: editForm.parcelType,
      shelfId: editForm.shelfId,
      overdueDays: editForm.overdueDays,
      remark: editForm.remark
    })
    ElMessage.success('快件信息修改成功')
    editVisible.value = false
    loadData()
  } catch (e) {
    // 错误提示已由拦截器处理
  } finally {
    editSubmitting.value = false
  }
}

/* ------------------------------------------------------------------
 * 派送 / 删除 / 导出
 * ---------------------------------------------------------------- */

/**
 * 派送：状态由 IN_STORE 改为 DELIVERING
 * @param {Object} row
 */
async function handleDeliver(row) {
  try {
    await ElMessageBox.confirm(
      `确定将运单号「${row.waybillNo}」的快件标记为派送中吗？`,
      '派送确认',
      { confirmButtonText: '确定派送', cancelButtonText: '取消', type: 'warning' }
    )
  } catch (e) {
    return // 用户取消
  }

  try {
    await deliverParcel(row.id)
    ElMessage.success('已标记为派送中')
    loadData()
  } catch (e) {
    // 错误提示已由拦截器处理
  }
}

/**
 * 删除快件（逻辑删除，同时释放货位）
 * @param {Object} row
 */
async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除运单号「${row.waybillNo}」的快件吗？删除后将释放其占用的货位，且不可恢复。`,
      '删除确认',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'error' }
    )
  } catch (e) {
    return // 用户取消
  }

  try {
    await deleteParcel(row.id)
    ElMessage.success('快件已删除')
    // 若当前页删空则回退一页
    if (list.value.length === 1 && query.pageNum > 1) {
      query.pageNum -= 1
    }
    loadData()
  } catch (e) {
    // 错误提示已由拦截器处理
  }
}

/**
 * 导出台账：按当前筛选条件导出 xlsx
 * 注意：blob 请求的响应不会被响应拦截器解包，需要取原始响应交给 downloadBlob
 */
async function handleExport() {
  exporting.value = true
  try {
    const response = await exportParcels(buildParams(false))
    downloadBlob(response, `快件台账_${new Date().getTime()}.xlsx`)
    ElMessage.success('台账导出成功')
  } catch (e) {
    // 后端返回的可能是 JSON 错误体（blob 包裹），此处统一给出兜底提示
    ElMessage.error('导出失败，请稍后重试')
  } finally {
    exporting.value = false
  }
}

/* ------------------------------------------------------------------
 * 打印取件小票（v1.1 新增）
 * ---------------------------------------------------------------- */
/** 取件小票组件实例 */
const ticketRef = ref(null)
/** 待打印的快件（设为行数据后调用子组件的 print()） */
const ticketParcel = ref(null)

/**
 * 打印某一行的取件小票
 * 先把行数据交给小票组件，等 DOM 更新后再触发打印（打印内容在隐藏 iframe 中生成）
 * @param {Object} row 表格行数据
 */
async function handlePrintTicket(row) {
  ticketParcel.value = { ...row }
  await nextTick()
  ticketRef.value?.print()
}

onMounted(() => {
  loadStations()
  loadData()
})
</script>

<style scoped>
.pickup-code {
  cursor: pointer;
  font-family: Consolas, Monaco, monospace;
  letter-spacing: 1px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
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
  color: var(--es-text-1);
  margin-bottom: 2px;
}

/* 取件小票容器：放在屏幕外，仅作为打印数据源，不参与页面布局 */
.ticket-holder {
  position: fixed;
  top: 0;
  left: -9999px;
  z-index: -1;
  pointer-events: none;
}
</style>
