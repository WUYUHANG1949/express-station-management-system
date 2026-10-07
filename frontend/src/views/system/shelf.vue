<template>
  <PageContainer title="货位管理" sub-title="维护驿站货位编码、区域与容量">
    <!-- ==================== 条件查询 ==================== -->
    <el-form :model="query" :inline="true" class="search-bar">
      <el-form-item label="所属驿站">
        <el-select v-model="query.stationId" placeholder="全部驿站" clearable style="width: 200px">
          <el-option v-for="item in stationList" :key="item.id" :label="item.stationName" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="货位编码">
        <el-input v-model.trim="query.shelfCode" placeholder="例如 A-01-01" clearable style="width: 160px" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="区域">
        <el-input v-model.trim="query.area" placeholder="例如 A" clearable style="width: 120px" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" :loading="loading" @click="handleSearch">查询</el-button>
        <el-button :icon="RefreshLeft" @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- ==================== 工具条 ==================== -->
    <div class="table-toolbar">
      <div class="table-toolbar__left">
        <el-tag type="info" effect="plain">共 {{ total }} 个货位</el-tag>
      </div>
      <div class="table-toolbar__right">
        <el-button :icon="Refresh" :loading="loading" @click="loadData">刷新</el-button>
        <el-button v-perm="'system:shelf:edit'" type="primary" :icon="Plus" @click="openCreate">新增货位</el-button>
      </div>
    </div>

    <!-- ==================== 数据表格 ==================== -->
    <el-table v-loading="loading" :data="list" border stripe height="520">
      <el-table-column prop="id" label="ID" width="70" align="center" />
      <el-table-column prop="stationName" label="所属驿站" min-width="170" show-overflow-tooltip />
      <el-table-column prop="shelfCode" label="货位编码" width="130" fixed="left" />
      <el-table-column prop="area" label="区域" width="90" align="center" />
      <el-table-column prop="capacity" label="容量" width="90" align="right" />
      <el-table-column prop="usedCount" label="已用" width="90" align="right" />
      <el-table-column label="剩余" width="90" align="right">
        <template #default="{ row }">
          <span :class="{ 'text-danger': Number(row.freeCount) <= 0 }">{{ row.freeCount ?? 0 }}</span>
        </template>
      </el-table-column>
      <el-table-column label="占用率" width="170">
        <template #default="{ row }">
          <el-progress
            :percentage="usageRate(row)"
            :stroke-width="12"
            :color="usageColor(usageRate(row))"
            :format="(p) => `${p}%`"
          />
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="dictType(STATUS_OPTIONS, row.status)">
            {{ dictLabel(STATUS_OPTIONS, row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <div class="table-actions">
            <el-button v-perm="'system:shelf:edit'" link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button v-perm="'system:shelf:edit'" link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </div>
        </template>
      </el-table-column>

      <template #empty>
        <el-empty description="暂无货位数据" :image-size="90" />
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

    <!-- ==================== 新增 / 编辑对话框 ==================== -->
    <el-dialog
      v-model="formVisible"
      :title="isEdit ? '编辑货位' : '新增货位'"
      width="560px"
      :close-on-click-modal="false"
      @closed="resetFormDialog"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="110px">
        <el-form-item label="所属驿站" prop="stationId">
          <el-select v-model="form.stationId" placeholder="请选择驿站" style="width: 100%">
            <el-option v-for="item in stationList" :key="item.id" :label="item.stationName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="货位编码" prop="shelfCode">
          <el-input v-model.trim="form.shelfCode" placeholder="例如 A-01-01" />
        </el-form-item>
        <el-form-item label="区域" prop="area">
          <el-input v-model.trim="form.area" placeholder="例如 A（同一区域的货位归为一组）" />
        </el-form-item>
        <el-form-item label="容量（件）" prop="capacity">
          <el-input-number v-model="form.capacity" :min="1" :max="10000" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio v-for="item in STATUS_OPTIONS" :key="item.value" :value="item.value">{{ item.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">
          {{ isEdit ? '保存修改' : '确认新增' }}
        </el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup>
/**
 * 货位管理页（ADMIN）
 * 接口：分页查询 / 新增 / 编辑 / 删除（有在库快件时后端禁止删除）
 * 权限：system:shelf:edit
 */
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, RefreshLeft, Search } from '@element-plus/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import { createShelf, deleteShelf, pageShelves, updateShelf } from '@/api/shelf'
import { listStations } from '@/api/station'
import { useUserStore } from '@/stores/user'
import { STATUS_OPTIONS, dictLabel, dictType } from '@/utils/dict'

/** 组件名：供 Layout 的 keep-alive include 使用 */
defineOptions({ name: 'SystemShelf' })

const userStore = useUserStore()

/** 查询条件：默认筛选当前用户所属驿站 */
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  stationId: userStore.stationId || '',
  shelfCode: '',
  area: ''
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

/** 加载货位分页数据 */
async function loadData() {
  loading.value = true
  try {
    const data = await pageShelves(buildParams())
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
  query.stationId = userStore.stationId || ''
  query.shelfCode = ''
  query.area = ''
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

/**
 * 计算货位占用率
 * @param {Object} row 货位行数据
 * @returns {number} 0-100
 */
function usageRate(row) {
  const capacity = Number(row.capacity) || 0
  const usedCount = Number(row.usedCount) || 0
  if (!capacity) return 0
  const rate = (usedCount / capacity) * 100
  return Math.min(Math.max(Number(rate.toFixed(1)), 0), 100)
}

/** 占用率颜色 */
function usageColor(rate) {
  if (rate >= 90) return '#f04438'
  if (rate >= 70) return '#f59f00'
  return '#0fb98f'
}

/* ------------------------------------------------------------------
 * 新增 / 编辑
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
    shelfCode: '',
    area: '',
    capacity: 40,
    status: 1
  }
}

/** 货位表单数据 */
const form = reactive(createFormData())

/** 表单校验规则 */
const formRules = {
  stationId: [{ required: true, message: '请选择所属驿站', trigger: 'change' }],
  shelfCode: [
    { required: true, message: '请输入货位编码', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9-]{2,20}$/, message: '货位编码为 2-20 位字母、数字或短横线', trigger: 'blur' }
  ],
  area: [{ required: true, message: '请输入区域', trigger: 'blur' }],
  capacity: [{ required: true, message: '请输入容量', trigger: 'blur' }]
}

/** 打开新增对话框 */
function openCreate() {
  isEdit.value = false
  Object.assign(form, createFormData())
  form.stationId = userStore.stationId || (stationList.value[0] ? stationList.value[0].id : null)
  formVisible.value = true
}

/**
 * 打开编辑对话框
 * @param {Object} row 货位行数据
 */
function openEdit(row) {
  isEdit.value = true
  Object.assign(form, createFormData())
  form.id = row.id
  form.stationId = row.stationId
  form.shelfCode = row.shelfCode
  form.area = row.area
  form.capacity = row.capacity ?? 40
  form.status = row.status ?? 1
  formVisible.value = true
}

/** 关闭后重置表单 */
function resetFormDialog() {
  formRef.value?.clearValidate()
  Object.assign(form, createFormData())
}

/** 提交新增 / 编辑 */
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
      shelfCode: form.shelfCode,
      area: form.area,
      capacity: form.capacity,
      status: form.status
    }
    if (isEdit.value) {
      await updateShelf(form.id, payload)
      ElMessage.success('货位信息修改成功')
    } else {
      await createShelf(payload)
      ElMessage.success('货位新增成功')
    }
    formVisible.value = false
    loadData()
  } catch (e) {
    // 错误提示已由拦截器统一处理
  } finally {
    submitting.value = false
  }
}

/* ------------------------------------------------------------------
 * 删除
 * ---------------------------------------------------------------- */

/**
 * 删除货位（有在库快件时后端会拒绝）
 * @param {Object} row 货位行数据
 */
async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除货位「${row.shelfCode}」吗？若该货位上仍有在库快件，后端将拒绝删除。`,
      '删除确认',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'error' }
    )
  } catch (e) {
    return // 用户取消
  }

  try {
    await deleteShelf(row.id)
    ElMessage.success('货位已删除')
    if (list.value.length === 1 && query.pageNum > 1) {
      query.pageNum -= 1
    }
    loadData()
  } catch (e) {
    // 错误提示已由拦截器统一处理
  }
}

onMounted(async () => {
  await loadStations()
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
