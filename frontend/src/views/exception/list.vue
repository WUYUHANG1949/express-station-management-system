<template>
  <PageContainer title="异常件管理" sub-title="登记快件异常并跟踪处理进度">
    <!-- ==================== 条件查询 ==================== -->
    <el-form :model="query" :inline="true" class="search-bar">
      <el-form-item label="运单号">
        <el-input v-model.trim="query.waybillNo" placeholder="请输入运单号" clearable style="width: 180px" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="异常类型">
        <el-select v-model="query.exceptionType" placeholder="全部" clearable style="width: 150px">
          <el-option v-for="item in EXCEPTION_TYPE" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="处理状态">
        <el-select v-model="query.handleStatus" placeholder="全部" clearable style="width: 140px">
          <el-option v-for="item in HANDLE_STATUS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="所属驿站">
        <el-select v-model="query.stationId" placeholder="全部" clearable style="width: 180px">
          <el-option v-for="item in stationList" :key="item.id" :label="item.stationName" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" :loading="loading" @click="handleSearch">查询</el-button>
        <el-button :icon="RefreshLeft" @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- ==================== 工具条 ==================== -->
    <div class="table-toolbar">
      <div class="table-toolbar__left">
        <el-tag type="info" effect="plain">共 {{ total }} 条异常记录</el-tag>
        <el-tag type="danger" effect="plain">登记异常后快件状态将自动置为「异常件」</el-tag>
      </div>
      <div class="table-toolbar__right">
        <el-button :icon="Refresh" :loading="loading" @click="loadData">刷新</el-button>
        <el-button v-perm="'exception:add'" type="primary" :icon="Warning" @click="openCreate">登记异常</el-button>
      </div>
    </div>

    <!-- ==================== 数据表格 ==================== -->
    <el-table v-loading="loading" :data="list" border stripe height="520">
      <el-table-column prop="waybillNo" label="运单号" width="170" fixed="left" show-overflow-tooltip />
      <el-table-column label="异常类型" width="120" align="center">
        <template #default="{ row }">
          <el-tag :type="dictType(EXCEPTION_TYPE, row.exceptionType)">
            {{ row.exceptionTypeName || EXCEPTION_TYPE_NAME[row.exceptionType] || row.exceptionType }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="异常描述" min-width="200" show-overflow-tooltip />
      <el-table-column label="处理状态" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="dictType(HANDLE_STATUS, row.handleStatus)">
            {{ row.handleStatusName || HANDLE_STATUS_NAME[row.handleStatus] || row.handleStatus }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="handlerName" label="处理人" width="100">
        <template #default="{ row }">{{ row.handlerName || '-' }}</template>
      </el-table-column>
      <el-table-column prop="handleResult" label="处理结果" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">{{ row.handleResult || '-' }}</template>
      </el-table-column>
      <el-table-column prop="stationName" label="所属驿站" width="160" show-overflow-tooltip />
      <el-table-column label="登记时间" width="170">
        <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="处理时间" width="170">
        <template #default="{ row }">{{ formatDateTime(row.handleTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="110" fixed="right">
        <template #default="{ row }">
          <div class="table-actions">
            <el-tooltip
              :disabled="row.handleStatus !== 'RESOLVED'"
              content="该异常已解决，无需再次处理"
              placement="top"
            >
              <span>
                <el-button
                  v-perm="'exception:handle'"
                  link
                  type="primary"
                  size="small"
                  :disabled="row.handleStatus === 'RESOLVED'"
                  @click="openHandle(row)"
                >
                  处理异常
                </el-button>
              </span>
            </el-tooltip>
          </div>
        </template>
      </el-table-column>

      <template #empty>
        <el-empty description="暂无异常件记录" :image-size="90" />
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

    <!-- ==================== 登记异常对话框 ==================== -->
    <el-dialog v-model="createVisible" title="登记异常" width="760px" :close-on-click-modal="false" @closed="resetCreateDialog">
      <!-- 第一步：按运单号搜索快件 -->
      <el-divider content-position="left">第一步：搜索并选择快件</el-divider>
      <div class="parcel-search">
        <el-input
          v-model.trim="parcelKeyword"
          placeholder="输入运单号 / 取件码 / 手机号后回车查询"
          clearable
          :prefix-icon="Search"
          style="max-width: 420px"
          @keyup.enter="searchParcel"
        >
          <template #append>
            <el-button :icon="Search" :loading="parcelSearching" @click="searchParcel">查询</el-button>
          </template>
        </el-input>
        <span class="text-muted ml-8">支持按运单号、取件码或收件人手机号模糊匹配</span>
      </div>

      <el-table
        v-loading="parcelSearching"
        :data="parcelOptions"
        border
        highlight-current-row
        max-height="220"
        class="mt-12"
        @current-change="handleParcelSelect"
      >
        <el-table-column prop="waybillNo" label="运单号" min-width="170" show-overflow-tooltip />
        <el-table-column prop="pickupCode" label="取件码" width="110" align="center" />
        <el-table-column prop="receiverName" label="收件人" width="100" />
        <el-table-column prop="receiverPhone" label="手机号" width="130" />
        <el-table-column prop="expressCompany" label="快递公司" width="110" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="dictType(PARCEL_STATUS, row.status)" size="small">
              {{ row.statusName || PARCEL_STATUS_NAME[row.status] || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="请输入运单号后查询快件" :image-size="70" />
        </template>
      </el-table>

      <div v-if="selectedParcel" class="selected-parcel mt-8">
        <el-icon><CircleCheckFilled /></el-icon>
        <span>已选择快件：{{ selectedParcel.waybillNo }}（取件码 {{ selectedParcel.pickupCode || '-' }}）</span>
      </div>

      <!-- 第二步：填写异常信息 -->
      <el-divider content-position="left">第二步：填写异常信息</el-divider>
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="100px">
        <el-form-item label="异常类型" prop="exceptionType">
          <el-select v-model="createForm.exceptionType" placeholder="请选择异常类型" style="width: 100%">
            <el-option v-for="item in EXCEPTION_TYPE" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="异常描述" prop="description">
          <el-input
            v-model="createForm.description"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="请描述异常情况，例如：到件外包装破损，内件疑似受损"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="createSubmitting" @click="submitCreate">确认登记</el-button>
      </template>
    </el-dialog>

    <!-- ==================== 处理异常对话框 ==================== -->
    <el-dialog v-model="handleVisible" title="处理异常" width="640px" :close-on-click-modal="false" @closed="resetHandleDialog">
      <el-descriptions v-if="currentRow" :column="1" border size="small" class="mb-12">
        <el-descriptions-item label="运单号">{{ currentRow.waybillNo }}</el-descriptions-item>
        <el-descriptions-item label="异常类型">
          <el-tag :type="dictType(EXCEPTION_TYPE, currentRow.exceptionType)" size="small">
            {{ currentRow.exceptionTypeName || EXCEPTION_TYPE_NAME[currentRow.exceptionType] }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="异常描述">{{ currentRow.description || '-' }}</el-descriptions-item>
        <el-descriptions-item label="登记时间">{{ formatDateTime(currentRow.createTime) }}</el-descriptions-item>
      </el-descriptions>

      <el-form ref="handleFormRef" :model="handleForm" :rules="handleRules" label-width="110px">
        <el-form-item label="处理状态" prop="handleStatus">
          <el-select v-model="handleForm.handleStatus" placeholder="请选择处理状态" style="width: 100%">
            <el-option v-for="item in HANDLE_STATUS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理结果" prop="handleResult">
          <el-input
            v-model="handleForm.handleResult"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="请填写处理过程与结果，例如：已协商理赔，快件退回发件网点"
          />
        </el-form-item>
        <el-form-item label="同步快件状态" prop="parcelStatus">
          <el-select v-model="handleForm.parcelStatus" placeholder="不修改可留空" clearable style="width: 100%">
            <el-option
              v-for="item in parcelStatusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
          <div class="form-tip">选择后将在处理异常的同时更新该快件的状态</div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="handleVisible = false">取消</el-button>
        <el-button type="primary" :loading="handleSubmitting" @click="submitHandle">确认处理</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup>
/**
 * 异常件管理页
 * 功能：条件查询 + 分页表格 + 登记异常（先按运单号搜索快件再选择）+ 处理异常
 * 权限：exception:add / exception:handle
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { CircleCheckFilled, Refresh, RefreshLeft, Search, Warning } from '@element-plus/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import { createException, handleException, pageExceptions } from '@/api/exception'
import { queryParcels } from '@/api/parcel'
import { listStations } from '@/api/station'
import { useUserStore } from '@/stores/user'
import {
  EXCEPTION_TYPE,
  EXCEPTION_TYPE_NAME,
  HANDLE_STATUS,
  HANDLE_STATUS_NAME,
  PARCEL_STATUS,
  PARCEL_STATUS_NAME,
  dictType
} from '@/utils/dict'
import { formatDateTime } from '@/utils/format'

const userStore = useUserStore()

/** 组件名：供 Layout 的 keep-alive include 使用（多个 list.vue 同名会互相顶替，故显式命名） */
defineOptions({ name: 'ExceptionList' })

/** 查询条件 */
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  waybillNo: '',
  exceptionType: '',
  handleStatus: '',
  stationId: ''
})

/** 列表数据 */
const list = ref([])
const total = ref(0)
const loading = ref(false)
/** 驿站下拉 */
const stationList = ref([])

/** 组装查询参数（过滤空值） */
function buildParams() {
  const params = { ...query }
  Object.keys(params).forEach((key) => {
    if (params[key] === '' || params[key] === null || params[key] === undefined) delete params[key]
  })
  return params
}

/** 加载异常件分页数据 */
async function loadData() {
  loading.value = true
  try {
    const data = await pageExceptions(buildParams())
    list.value = (data && data.list) || []
    total.value = (data && data.total) || 0
  } catch (e) {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

/** 查询 */
function handleSearch() {
  query.pageNum = 1
  loadData()
}

/** 重置筛选条件 */
function handleReset() {
  query.waybillNo = ''
  query.exceptionType = ''
  query.handleStatus = ''
  query.stationId = ''
  query.pageNum = 1
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
 * 登记异常
 * ---------------------------------------------------------------- */
const createVisible = ref(false)
const createSubmitting = ref(false)
const createFormRef = ref(null)

/** 快件搜索关键字与结果 */
const parcelKeyword = ref('')
const parcelSearching = ref(false)
const parcelOptions = ref([])
const selectedParcel = ref(null)

/** 登记异常表单 */
const createForm = reactive({
  exceptionType: '',
  description: ''
})

/** 校验规则 */
const createRules = {
  exceptionType: [{ required: true, message: '请选择异常类型', trigger: 'change' }],
  description: [
    { required: true, message: '请填写异常描述', trigger: 'blur' },
    { min: 4, message: '异常描述至少 4 个字，便于后续追溯', trigger: 'blur' }
  ]
}

/** 打开登记异常对话框 */
function openCreate() {
  parcelKeyword.value = ''
  parcelOptions.value = []
  selectedParcel.value = null
  createForm.exceptionType = ''
  createForm.description = ''
  createVisible.value = true
}

/** 对话框关闭后重置 */
function resetCreateDialog() {
  createFormRef.value?.clearValidate()
  parcelKeyword.value = ''
  parcelOptions.value = []
  selectedParcel.value = null
  createForm.exceptionType = ''
  createForm.description = ''
}

/** 按关键字搜索快件（供登记异常时选择） */
async function searchParcel() {
  if (!parcelKeyword.value) {
    ElMessage.warning('请先输入运单号 / 取件码 / 手机号')
    return
  }
  parcelSearching.value = true
  selectedParcel.value = null
  parcelOptions.value = []
  try {
    const params = { keyword: parcelKeyword.value }
    if (userStore.stationId) params.stationId = userStore.stationId
    const data = await queryParcels(params)
    parcelOptions.value = Array.isArray(data) ? data : []
    if (!parcelOptions.value.length) {
      ElMessage.warning('未查询到匹配的快件，请核对运单号')
    } else if (parcelOptions.value.length === 1) {
      // 只有一条时自动选中
      selectedParcel.value = parcelOptions.value[0]
    }
  } catch (e) {
    parcelOptions.value = []
  } finally {
    parcelSearching.value = false
  }
}

/** 表格当前行变化：记录选中的快件 */
function handleParcelSelect(row) {
  if (row) selectedParcel.value = row
}

/** 提交登记异常 */
async function submitCreate() {
  if (!selectedParcel.value) {
    ElMessage.warning('请先搜索并选择需要登记异常的快件')
    return
  }
  if (!createFormRef.value) return
  try {
    await createFormRef.value.validate()
  } catch (e) {
    ElMessage.warning('请先完善标红的必填项')
    return
  }

  createSubmitting.value = true
  try {
    await createException({
      parcelId: selectedParcel.value.id,
      exceptionType: createForm.exceptionType,
      description: createForm.description
    })
    ElMessage.success('异常登记成功，快件状态已置为「异常件」')
    createVisible.value = false
    query.pageNum = 1
    loadData()
  } catch (e) {
    // 错误提示已由拦截器统一处理
  } finally {
    createSubmitting.value = false
  }
}

/* ------------------------------------------------------------------
 * 处理异常
 * ---------------------------------------------------------------- */
const handleVisible = ref(false)
const handleSubmitting = ref(false)
const handleFormRef = ref(null)
/** 当前处理的异常记录 */
const currentRow = ref(null)

/** 处理异常表单 */
const handleForm = reactive({
  handleStatus: 'HANDLING',
  handleResult: '',
  parcelStatus: ''
})

/** 可同步的快件状态（排除当前状态） */
const parcelStatusOptions = computed(() =>
  PARCEL_STATUS.filter((item) => !currentRow.value || item.value !== currentRow.value.parcelStatus)
)

/** 校验规则 */
const handleRules = {
  handleStatus: [{ required: true, message: '请选择处理状态', trigger: 'change' }],
  handleResult: [
    { required: true, message: '请填写处理结果', trigger: 'blur' },
    { min: 4, message: '处理结果至少 4 个字', trigger: 'blur' }
  ]
}

/**
 * 打开处理异常对话框
 * @param {Object} row 异常记录
 */
function openHandle(row) {
  currentRow.value = row
  handleForm.handleStatus = row.handleStatus === 'PENDING' ? 'HANDLING' : row.handleStatus
  handleForm.handleResult = row.handleResult || ''
  handleForm.parcelStatus = ''
  handleVisible.value = true
}

/** 对话框关闭后重置 */
function resetHandleDialog() {
  handleFormRef.value?.clearValidate()
  handleForm.handleStatus = 'HANDLING'
  handleForm.handleResult = ''
  handleForm.parcelStatus = ''
  currentRow.value = null
}

/** 提交处理异常 */
async function submitHandle() {
  if (!handleFormRef.value || !currentRow.value) return
  try {
    await handleFormRef.value.validate()
  } catch (e) {
    ElMessage.warning('请先完善标红的必填项')
    return
  }

  handleSubmitting.value = true
  try {
    const payload = {
      handleStatus: handleForm.handleStatus,
      handleResult: handleForm.handleResult
    }
    // parcelStatus 为可选字段：留空时不传，后端不会修改快件状态
    if (handleForm.parcelStatus) {
      payload.parcelStatus = handleForm.parcelStatus
    }

    await handleException(currentRow.value.id, payload)
    ElMessage.success('异常处理完成')
    handleVisible.value = false
    loadData()
  } catch (e) {
    // 错误提示已由拦截器统一处理
  } finally {
    handleSubmitting.value = false
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

.parcel-search {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.selected-parcel {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  border-radius: 6px;
  background: #f0f9eb;
  color: #529b2e;
  font-size: 13px;
}

.form-tip {
  width: 100%;
  font-size: 12px;
  color: var(--es-text-3);
}
</style>
