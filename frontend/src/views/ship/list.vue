<template>
  <PageContainer title="寄件管理" sub-title="寄件单登记、状态流转与运单号回填">
    <!-- ==================== 条件查询 ==================== -->
    <el-form :model="query" :inline="true" class="search-bar">
      <el-form-item label="寄件单号">
        <el-input v-model.trim="query.orderNo" placeholder="请输入寄件单号" clearable style="width: 180px" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="寄件人">
        <el-input v-model.trim="query.senderName" placeholder="寄件人姓名" clearable style="width: 130px" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="寄件人手机">
        <el-input v-model.trim="query.senderPhone" placeholder="手机号" clearable style="width: 140px" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="收件人">
        <el-input v-model.trim="query.receiverName" placeholder="收件人姓名" clearable style="width: 130px" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="收件人手机">
        <el-input v-model.trim="query.receiverPhone" placeholder="手机号" clearable style="width: 140px" @keyup.enter="handleSearch" />
      </el-form-item>

      <template v-if="showMore">
        <el-form-item label="寄件状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="item in SHIP_STATUS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属驿站">
          <el-select v-model="query.stationId" placeholder="全部" clearable style="width: 180px">
            <el-option v-for="item in stationList" :key="item.id" :label="item.stationName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="创建时间">
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
        <el-tag type="info" effect="plain">共 {{ total }} 条寄件单</el-tag>
      </div>
      <div class="table-toolbar__right">
        <el-button :icon="Refresh" :loading="loading" @click="loadData">刷新</el-button>
        <el-button v-perm="'ship:add'" type="primary" :icon="Plus" @click="openCreate">新建寄件单</el-button>
      </div>
    </div>

    <!-- ==================== 数据表格 ==================== -->
    <el-table v-loading="loading" :data="list" border stripe height="520">
      <el-table-column prop="orderNo" label="寄件单号" width="180" fixed="left" show-overflow-tooltip />
      <el-table-column label="运单号" width="160" show-overflow-tooltip>
        <template #default="{ row }">{{ row.waybillNo || '-' }}</template>
      </el-table-column>
      <el-table-column prop="expressCompany" label="快递公司" width="110" />
      <el-table-column label="寄件人" width="150">
        <template #default="{ row }">
          <div>{{ row.senderName || '-' }}</div>
          <div class="text-muted">{{ row.senderPhone || '-' }}</div>
        </template>
      </el-table-column>
      <el-table-column label="收件人" width="150">
        <template #default="{ row }">
          <div>{{ row.receiverName || '-' }}</div>
          <div class="text-muted">{{ row.receiverPhone || '-' }}</div>
        </template>
      </el-table-column>
      <el-table-column prop="receiverAddress" label="收件地址" min-width="200" show-overflow-tooltip />
      <el-table-column label="类型" width="90" align="center">
        <template #default="{ row }">{{ row.parcelTypeName || dictLabel(PARCEL_TYPE, row.parcelType) }}</template>
      </el-table-column>
      <el-table-column label="重量" width="100" align="right">
        <template #default="{ row }">{{ formatWeight(row.weight) }}</template>
      </el-table-column>
      <el-table-column label="运费" width="100" align="right">
        <template #default="{ row }">{{ formatMoney(row.freight, true) }}</template>
      </el-table-column>
      <el-table-column label="保价" width="100" align="right">
        <template #default="{ row }">{{ formatMoney(row.insuredValue, true) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="dictType(SHIP_STATUS, row.status)">
            {{ row.statusName || SHIP_STATUS_NAME[row.status] || row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="operatorName" label="操作员" width="100" />
      <el-table-column label="创建时间" width="170">
        <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="210" fixed="right">
        <template #default="{ row }">
          <div class="table-actions">
            <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
            <el-tooltip :disabled="row.status === 'PENDING'" content="仅「待揽收」状态的寄件单可编辑" placement="top">
              <span>
                <el-button
                  v-perm="'ship:edit'"
                  link
                  type="primary"
                  size="small"
                  :disabled="row.status !== 'PENDING'"
                  @click="openEdit(row)"
                >
                  编辑
                </el-button>
              </span>
            </el-tooltip>
            <el-button v-perm="'ship:status'" link type="warning" size="small" @click="openStatus(row)">更新状态</el-button>
            <el-button v-perm="'ship:delete'" link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </div>
        </template>
      </el-table-column>

      <template #empty>
        <el-empty description="暂无寄件单数据，请调整筛选条件后重试" :image-size="90" />
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

    <!-- ==================== 新建 / 编辑对话框 ==================== -->
    <el-dialog
      v-model="formVisible"
      :title="isEdit ? '编辑寄件单' : '新建寄件单'"
      width="760px"
      :close-on-click-modal="false"
      @closed="resetFormDialog"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="所属驿站" prop="stationId">
              <el-select v-model="form.stationId" placeholder="请选择驿站" style="width: 100%" :disabled="isEdit">
                <el-option v-for="item in stationList" :key="item.id" :label="item.stationName" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="快递公司" prop="expressCompany">
              <el-select v-model="form.expressCompany" placeholder="请选择快递公司" style="width: 100%">
                <el-option v-for="item in EXPRESS_COMPANIES" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="寄件人姓名" prop="senderName">
              <el-input v-model.trim="form.senderName" placeholder="请输入寄件人姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="寄件人手机号" prop="senderPhone">
              <el-input v-model.trim="form.senderPhone" placeholder="请输入 11 位手机号" maxlength="11" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="寄件人地址" prop="senderAddress">
              <el-input v-model.trim="form.senderAddress" placeholder="请输入寄件人地址" />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="收件人姓名" prop="receiverName">
              <el-input v-model.trim="form.receiverName" placeholder="请输入收件人姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="收件人手机号" prop="receiverPhone">
              <el-input v-model.trim="form.receiverPhone" placeholder="请输入 11 位手机号" maxlength="11" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="收件人地址" prop="receiverAddress">
              <el-input v-model.trim="form.receiverAddress" placeholder="请输入收件人详细地址" />
            </el-form-item>
          </el-col>

          <el-col :span="8">
            <el-form-item label="快件类型" prop="parcelType">
              <el-select v-model="form.parcelType" style="width: 100%">
                <el-option v-for="item in PARCEL_TYPE" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="重量（kg）" prop="weight">
              <el-input-number v-model="form.weight" :min="0" :max="1000" :precision="2" :step="0.1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="运费（元）" prop="freight">
              <el-input-number v-model="form.freight" :min="0" :max="99999" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="保价（元）" prop="insuredValue">
              <el-input-number v-model="form.insuredValue" :min="0" :max="999999" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">
          {{ isEdit ? '保存修改' : '确认登记' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- ==================== 更新状态对话框 ==================== -->
    <el-dialog v-model="statusVisible" title="更新寄件状态" width="520px" :close-on-click-modal="false" @closed="resetStatusDialog">
      <el-descriptions v-if="currentRow" :column="1" border size="small" class="mb-12">
        <el-descriptions-item label="寄件单号">{{ currentRow.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="寄件人 / 收件人">
          {{ currentRow.senderName }} → {{ currentRow.receiverName }}
        </el-descriptions-item>
        <el-descriptions-item label="当前状态">
          <el-tag :type="dictType(SHIP_STATUS, currentRow.status)">
            {{ currentRow.statusName || SHIP_STATUS_NAME[currentRow.status] }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>

      <el-form ref="statusFormRef" :model="statusForm" :rules="statusRules" label-width="100px">
        <el-form-item label="目标任务状态" prop="status">
          <el-select v-model="statusForm.status" placeholder="请选择状态" style="width: 100%">
            <el-option v-for="item in targetStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="运单号" prop="waybillNo">
          <el-input v-model.trim="statusForm.waybillNo" placeholder="选择「已揽收 / 已发出」时请回填快递运单号" clearable />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="statusVisible = false">取消</el-button>
        <el-button type="primary" :loading="statusSubmitting" @click="submitStatus">确认更新</el-button>
      </template>
    </el-dialog>

    <!-- ==================== 详情对话框 ==================== -->
    <el-dialog v-model="detailVisible" title="寄件单详情" width="720px" :destroy-on-close="true">
      <el-descriptions v-loading="detailLoading" :column="2" border size="small">
        <el-descriptions-item label="寄件单号">{{ detail.orderNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="运单号">{{ detail.waybillNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="dictType(SHIP_STATUS, detail.status)">
            {{ detail.statusName || SHIP_STATUS_NAME[detail.status] || '-' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="快递公司">{{ detail.expressCompany || '-' }}</el-descriptions-item>
        <el-descriptions-item label="所属驿站">{{ detail.stationName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="快件类型">
          {{ detail.parcelTypeName || dictLabel(PARCEL_TYPE, detail.parcelType) }}
        </el-descriptions-item>
        <el-descriptions-item label="寄件人">{{ detail.senderName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="寄件人手机号">{{ detail.senderPhone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="寄件人地址" :span="2">{{ detail.senderAddress || '-' }}</el-descriptions-item>
        <el-descriptions-item label="收件人">{{ detail.receiverName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="收件人手机号">{{ detail.receiverPhone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="收件人地址" :span="2">{{ detail.receiverAddress || '-' }}</el-descriptions-item>
        <el-descriptions-item label="重量">{{ formatWeight(detail.weight) }}</el-descriptions-item>
        <el-descriptions-item label="运费">{{ formatMoney(detail.freight, true) }}</el-descriptions-item>
        <el-descriptions-item label="保价金额">{{ formatMoney(detail.insuredValue, true) }}</el-descriptions-item>
        <el-descriptions-item label="操作员">{{ detail.operatorName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="2">{{ formatDateTime(detail.createTime) }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button type="primary" @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup>
/**
 * 寄件管理页
 * 功能：条件查询 + 分页表格 + 新建/编辑寄件单 + 更新状态（可回填运单号）+ 详情 + 删除
 * 权限：ship:add / ship:edit / ship:status / ship:delete
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, RefreshLeft, Search } from '@element-plus/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import {
  createShipOrder,
  deleteShipOrder,
  getShipOrder,
  pageShipOrders,
  updateShipOrder,
  updateShipOrderStatus
} from '@/api/ship'
import { listStations } from '@/api/station'
import { useUserStore } from '@/stores/user'
import {
  EXPRESS_COMPANIES,
  PARCEL_TYPE,
  SHIP_STATUS,
  SHIP_STATUS_NAME,
  dictLabel,
  dictType
} from '@/utils/dict'
import { formatDateTime, formatMoney, formatWeight, splitDateRange } from '@/utils/format'

const userStore = useUserStore()

/** 组件名：供 Layout 的 keep-alive include 使用（多个 list.vue 同名会互相顶替，故显式命名） */
defineOptions({ name: 'ShipList' })

/** 展开更多筛选 */
const showMore = ref(false)
/** 创建时间范围 */
const dateRange = ref([])
/** 驿站下拉 */
const stationList = ref([])

/** 查询条件 */
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  orderNo: '',
  senderName: '',
  senderPhone: '',
  receiverName: '',
  receiverPhone: '',
  status: '',
  stationId: ''
})

/** 列表数据 */
const list = ref([])
const total = ref(0)
const loading = ref(false)

/* ------------------------------------------------------------------
 * 列表查询
 * ---------------------------------------------------------------- */

/** 组装查询参数，过滤空值 */
function buildParams() {
  const range = splitDateRange(dateRange.value)
  const params = {
    pageNum: query.pageNum,
    pageSize: query.pageSize,
    orderNo: query.orderNo,
    senderName: query.senderName,
    senderPhone: query.senderPhone,
    receiverName: query.receiverName,
    receiverPhone: query.receiverPhone,
    status: query.status,
    stationId: query.stationId,
    startTime: range.startTime,
    endTime: range.endTime
  }
  Object.keys(params).forEach((key) => {
    if (params[key] === '' || params[key] === null || params[key] === undefined) delete params[key]
  })
  return params
}

/** 加载寄件单分页数据 */
async function loadData() {
  loading.value = true
  try {
    const data = await pageShipOrders(buildParams())
    list.value = (data && data.list) || []
    total.value = (data && data.total) || 0
  } catch (e) {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

/** 查询：回到第一页 */
function handleSearch() {
  query.pageNum = 1
  loadData()
}

/** 重置筛选条件 */
function handleReset() {
  query.orderNo = ''
  query.senderName = ''
  query.senderPhone = ''
  query.receiverName = ''
  query.receiverPhone = ''
  query.status = ''
  query.stationId = ''
  query.pageNum = 1
  dateRange.value = []
  loadData()
}

/** 切换每页条数 */
function handleSizeChange() {
  query.pageNum = 1
  loadData()
}

/** 加载驿站下拉 */
async function loadStations() {
  try {
    const data = await listStations()
    stationList.value = Array.isArray(data) ? data : []
  } catch (e) {
    stationList.value = []
  }
}

/* ------------------------------------------------------------------
 * 新建 / 编辑
 * ---------------------------------------------------------------- */
const formVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref(null)

/** 表单默认值工厂 */
function createFormData() {
  return {
    id: null,
    stationId: userStore.stationId || null,
    expressCompany: '',
    senderName: '',
    senderPhone: '',
    senderAddress: '',
    receiverName: '',
    receiverPhone: '',
    receiverAddress: '',
    parcelType: 'NORMAL',
    weight: 1,
    freight: 0,
    insuredValue: 0
  }
}

/** 寄件单表单数据 */
const form = reactive(createFormData())

/** 手机号校验器 */
function validatePhone(rule, value, callback) {
  if (!value) {
    callback(new Error('请输入手机号'))
  } else if (!/^1[3-9]\d{9}$/.test(value)) {
    callback(new Error('手机号格式不正确'))
  } else {
    callback()
  }
}

/** 表单校验规则 */
const formRules = {
  stationId: [{ required: true, message: '请选择所属驿站', trigger: 'change' }],
  expressCompany: [{ required: true, message: '请选择快递公司', trigger: 'change' }],
  senderName: [{ required: true, message: '请输入寄件人姓名', trigger: 'blur' }],
  senderPhone: [{ required: true, validator: validatePhone, trigger: 'blur' }],
  senderAddress: [{ required: true, message: '请输入寄件人地址', trigger: 'blur' }],
  receiverName: [{ required: true, message: '请输入收件人姓名', trigger: 'blur' }],
  receiverPhone: [{ required: true, validator: validatePhone, trigger: 'blur' }],
  receiverAddress: [{ required: true, message: '请输入收件人地址', trigger: 'blur' }],
  parcelType: [{ required: true, message: '请选择快件类型', trigger: 'change' }]
}

/** 打开新建对话框 */
function openCreate() {
  isEdit.value = false
  Object.assign(form, createFormData())
  form.stationId = userStore.stationId || (stationList.value[0] ? stationList.value[0].id : null)
  formVisible.value = true
}

/** 打开编辑对话框（仅 PENDING 可用，调用方已做禁用） */
function openEdit(row) {
  isEdit.value = true
  Object.assign(form, createFormData())
  form.id = row.id
  form.stationId = row.stationId
  form.expressCompany = row.expressCompany
  form.senderName = row.senderName
  form.senderPhone = row.senderPhone
  form.senderAddress = row.senderAddress
  form.receiverName = row.receiverName
  form.receiverPhone = row.receiverPhone
  form.receiverAddress = row.receiverAddress
  form.parcelType = row.parcelType || 'NORMAL'
  form.weight = row.weight ?? 1
  form.freight = row.freight ?? 0
  form.insuredValue = row.insuredValue ?? 0
  formVisible.value = true
}

/** 对话框关闭后重置表单 */
function resetFormDialog() {
  formRef.value?.clearValidate()
  Object.assign(form, createFormData())
}

/** 提交新建 / 编辑 */
async function submitForm() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch (e) {
    ElMessage.warning('请先完善标红的必填项')
    return
  }

  submitting.value = true
  try {
    const payload = {
      stationId: form.stationId,
      expressCompany: form.expressCompany,
      senderName: form.senderName,
      senderPhone: form.senderPhone,
      senderAddress: form.senderAddress,
      receiverName: form.receiverName,
      receiverPhone: form.receiverPhone,
      receiverAddress: form.receiverAddress,
      parcelType: form.parcelType,
      weight: form.weight,
      freight: form.freight,
      insuredValue: form.insuredValue
    }

    if (isEdit.value) {
      await updateShipOrder(form.id, payload)
      ElMessage.success('寄件单修改成功')
      formVisible.value = false
    } else {
      const created = await createShipOrder(payload)
      formVisible.value = false
      // 大字展示系统生成的寄件单号，便于记录
      const orderNo = (created && created.orderNo) || ''
      if (orderNo) {
        await ElMessageBox.alert(
          `<div style="text-align:center;">
             <div style="font-size:13px;color:#909399;margin-bottom:6px;">系统生成的寄件单号</div>
             <div style="font-size:28px;font-weight:700;letter-spacing:2px;color:#2563eb;font-family:Consolas,Monaco,monospace;">
               ${orderNo}
             </div>
             <div style="margin-top:10px;font-size:13px;color:#909399;">请记录该单号，后续可凭此查询寄件进度</div>
           </div>`,
          '寄件单登记成功',
          { dangerouslyUseHTMLString: true, confirmButtonText: '知道了' }
        )
      } else {
        ElMessage.success('寄件单登记成功')
      }
    }
    loadData()
  } catch (e) {
    // 错误提示已由拦截器统一处理
  } finally {
    submitting.value = false
  }
}

/* ------------------------------------------------------------------
 * 更新状态
 * ---------------------------------------------------------------- */
const statusVisible = ref(false)
const statusSubmitting = ref(false)
const statusFormRef = ref(null)
/** 当前操作的寄件单 */
const currentRow = ref(null)

/** 状态表单 */
const statusForm = reactive({
  status: '',
  waybillNo: ''
})

/** 可选择的目标状态（排除初始状态 PENDING） */
const targetStatusOptions = computed(() => SHIP_STATUS.filter((item) => item.value !== 'PENDING'))

/**
 * 运单号校验：目标状态为已揽收 / 已发出时建议填写运单号
 */
function validateWaybill(rule, value, callback) {
  if ((statusForm.status === 'ACCEPTED' || statusForm.status === 'SHIPPED') && !value) {
    callback(new Error('状态为「已揽收 / 已发出」时请填写快递运单号'))
  } else {
    callback()
  }
}

/** 状态表单校验规则 */
const statusRules = {
  status: [{ required: true, message: '请选择目标任务状态', trigger: 'change' }],
  waybillNo: [{ validator: validateWaybill, trigger: 'blur' }]
}

/** 打开更新状态对话框 */
function openStatus(row) {
  currentRow.value = row
  statusForm.status = ''
  statusForm.waybillNo = row.waybillNo || ''
  statusVisible.value = true
}

/** 状态对话框关闭后重置 */
function resetStatusDialog() {
  statusFormRef.value?.clearValidate()
  statusForm.status = ''
  statusForm.waybillNo = ''
  currentRow.value = null
}

/** 提交状态更新 */
async function submitStatus() {
  if (!statusFormRef.value || !currentRow.value) return
  try {
    await statusFormRef.value.validate()
  } catch (e) {
    return
  }

  statusSubmitting.value = true
  try {
    await updateShipOrderStatus(currentRow.value.id, {
      status: statusForm.status,
      waybillNo: statusForm.waybillNo
    })
    ElMessage.success('寄件状态更新成功')
    statusVisible.value = false
    loadData()
  } catch (e) {
    // 错误提示已由拦截器统一处理
  } finally {
    statusSubmitting.value = false
  }
}

/* ------------------------------------------------------------------
 * 详情
 * ---------------------------------------------------------------- */
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref({})

/** 打开详情对话框 */
async function openDetail(row) {
  detailVisible.value = true
  detail.value = { ...row }
  detailLoading.value = true
  try {
    const data = await getShipOrder(row.id)
    if (data) detail.value = data
  } catch (e) {
    // 详情失败时保留列表行数据
  } finally {
    detailLoading.value = false
  }
}

/* ------------------------------------------------------------------
 * 删除
 * ---------------------------------------------------------------- */

/**
 * 删除寄件单（逻辑删除）
 * @param {Object} row
 */
async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除寄件单「${row.orderNo}」吗？删除后不可恢复。`, '删除确认', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (e) {
    return // 用户取消
  }

  try {
    await deleteShipOrder(row.id)
    ElMessage.success('寄件单已删除')
    if (list.value.length === 1 && query.pageNum > 1) {
      query.pageNum -= 1
    }
    loadData()
  } catch (e) {
    // 错误提示已由拦截器统一处理
  }
}

onMounted(() => {
  loadStations()
  loadData()
})
</script>

<style scoped>
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
