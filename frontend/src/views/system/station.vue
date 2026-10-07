<template>
  <PageContainer title="驿站管理" sub-title="维护驿站基础信息、容量与状态">
    <!-- ==================== 条件查询 ==================== -->
    <el-form :model="query" :inline="true" class="search-bar">
      <el-form-item label="驿站名称">
        <el-input v-model.trim="query.stationName" placeholder="请输入驿站名称" clearable style="width: 200px" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
          <el-option v-for="item in STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
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
        <el-tag type="info" effect="plain">共 {{ total }} 个驿站</el-tag>
      </div>
      <div class="table-toolbar__right">
        <el-button :icon="Refresh" :loading="loading" @click="loadData">刷新</el-button>
        <el-button v-perm="'system:station:edit'" type="primary" :icon="Plus" @click="openCreate">新增驿站</el-button>
      </div>
    </div>

    <!-- ==================== 数据表格 ==================== -->
    <el-table v-loading="loading" :data="list" border stripe height="520">
      <el-table-column prop="id" label="ID" width="70" align="center" />
      <el-table-column prop="stationCode" label="驿站编码" width="110" />
      <el-table-column prop="stationName" label="驿站名称" min-width="170" show-overflow-tooltip />
      <el-table-column prop="address" label="详细地址" min-width="230" show-overflow-tooltip />
      <el-table-column prop="contactPhone" label="联系电话" width="130" />
      <el-table-column prop="managerName" label="负责人" width="100" />
      <el-table-column prop="businessHours" label="营业时间" width="130" />
      <el-table-column prop="capacity" label="容量" width="90" align="right" />
      <el-table-column label="货位使用" width="170">
        <template #default="{ row }">
          <el-progress
            :percentage="usageRate(row)"
            :stroke-width="12"
            :color="usageColor(usageRate(row))"
            :format="() => `${row.usedCount ?? 0} / ${row.shelfCount ?? 0}`"
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
      <el-table-column label="创建时间" width="170">
        <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <div class="table-actions">
            <el-button v-perm="'system:station:edit'" link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button v-perm="'system:station:edit'" link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </div>
        </template>
      </el-table-column>

      <template #empty>
        <el-empty description="暂无驿站数据" :image-size="90" />
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
      :title="isEdit ? '编辑驿站' : '新增驿站'"
      width="640px"
      :close-on-click-modal="false"
      @closed="resetFormDialog"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="驿站编码" prop="stationCode">
              <el-input v-model.trim="form.stationCode" placeholder="例如 ST001" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="驿站名称" prop="stationName">
              <el-input v-model.trim="form.stationName" placeholder="例如 幸福小区快递驿站" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="详细地址" prop="address">
              <el-input v-model.trim="form.address" placeholder="请输入驿站详细地址" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话" prop="contactPhone">
              <el-input v-model.trim="form.contactPhone" placeholder="手机号或座机，例如 025-88880001" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="负责人" prop="managerName">
              <el-input v-model.trim="form.managerName" placeholder="请输入负责人姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="营业时间" prop="businessHours">
              <el-input v-model.trim="form.businessHours" placeholder="例如 08:00-21:00" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="容量（件）" prop="capacity">
              <el-input-number v-model="form.capacity" :min="1" :max="100000" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio v-for="item in STATUS_OPTIONS" :key="item.value" :value="item.value">{{ item.label }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
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
 * 驿站管理页（ADMIN）
 * 接口：分页查询 / 新增 / 编辑 / 删除
 * 权限：system:station:edit（新增、编辑、删除）
 */
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, RefreshLeft, Search } from '@element-plus/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import { createStation, deleteStation, pageStations, updateStation } from '@/api/station'
import { STATUS_OPTIONS, dictLabel, dictType } from '@/utils/dict'
import { formatDateTime } from '@/utils/format'

/** 组件名：供 Layout 的 keep-alive include 使用 */
defineOptions({ name: 'SystemStation' })

/** 查询条件 */
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  stationName: '',
  status: ''
})

/** 列表数据 */
const list = ref([])
const total = ref(0)
const loading = ref(false)

/** 组装查询参数（过滤空值） */
function buildParams() {
  const params = { ...query }
  Object.keys(params).forEach((key) => {
    if (params[key] === '' || params[key] === null || params[key] === undefined) delete params[key]
  })
  return params
}

/** 加载驿站分页数据 */
async function loadData() {
  loading.value = true
  try {
    const data = await pageStations(buildParams())
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
  query.stationName = ''
  query.status = ''
  query.pageNum = 1
  loadData()
}

/** 切换每页条数 */
function handleSizeChange() {
  query.pageNum = 1
  loadData()
}

/**
 * 计算货位占用率
 * @param {Object} row 驿站行数据
 * @returns {number} 0-100
 */
function usageRate(row) {
  const shelfCount = Number(row.shelfCount) || 0
  const usedCount = Number(row.usedCount) || 0
  if (!shelfCount) return 0
  const rate = (usedCount / shelfCount) * 100
  return Math.min(Math.max(Number(rate.toFixed(1)), 0), 100)
}

/** 占用率颜色：越高越警示 */
function usageColor(rate) {
  if (rate >= 90) return '#f56c6c'
  if (rate >= 70) return '#e6a23c'
  return '#67c23a'
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
    stationCode: '',
    stationName: '',
    address: '',
    contactPhone: '',
    managerName: '',
    businessHours: '08:00-21:00',
    capacity: 500,
    status: 1
  }
}

/** 驿站表单数据 */
const form = reactive(createFormData())

/** 联系电话校验：兼容手机号与座机（区号-号码） */
function validateContactPhone(rule, value, callback) {
  if (!value) {
    callback(new Error('请输入联系电话'))
  } else if (!/^1[3-9]\d{9}$/.test(value) && !/^0\d{2,3}-?\d{7,8}$/.test(value)) {
    callback(new Error('请输入正确的手机号或座机号（例如 025-88880001）'))
  } else {
    callback()
  }
}

/** 表单校验规则 */
const formRules = {
  stationCode: [{ required: true, message: '请输入驿站编码', trigger: 'blur' }],
  stationName: [{ required: true, message: '请输入驿站名称', trigger: 'blur' }],
  address: [{ required: true, message: '请输入详细地址', trigger: 'blur' }],
  contactPhone: [{ required: true, validator: validateContactPhone, trigger: 'blur' }],
  capacity: [{ required: true, message: '请输入容量', trigger: 'blur' }]
}

/** 打开新增对话框 */
function openCreate() {
  isEdit.value = false
  Object.assign(form, createFormData())
  formVisible.value = true
}

/**
 * 打开编辑对话框
 * @param {Object} row 驿站行数据
 */
function openEdit(row) {
  isEdit.value = true
  Object.assign(form, createFormData())
  form.id = row.id
  form.stationCode = row.stationCode
  form.stationName = row.stationName
  form.address = row.address
  form.contactPhone = row.contactPhone
  form.managerName = row.managerName || ''
  form.businessHours = row.businessHours || ''
  form.capacity = row.capacity ?? 500
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
      stationCode: form.stationCode,
      stationName: form.stationName,
      address: form.address,
      contactPhone: form.contactPhone,
      managerName: form.managerName,
      businessHours: form.businessHours,
      capacity: form.capacity,
      status: form.status
    }
    if (isEdit.value) {
      await updateStation(form.id, payload)
      ElMessage.success('驿站信息修改成功')
    } else {
      await createStation(payload)
      ElMessage.success('驿站新增成功')
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
 * 删除驿站
 * @param {Object} row 驿站行数据
 */
async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除驿站「${row.stationName}」吗？若该驿站下仍存在快件或用户，后端将拒绝删除。`,
      '删除确认',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'error' }
    )
  } catch (e) {
    return // 用户取消
  }

  try {
    await deleteStation(row.id)
    ElMessage.success('驿站已删除')
    if (list.value.length === 1 && query.pageNum > 1) {
      query.pageNum -= 1
    }
    loadData()
  } catch (e) {
    // 错误提示已由拦截器统一处理
  }
}

onMounted(() => {
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
