<template>
  <PageContainer title="用户管理" sub-title="维护系统账号、角色与所属驿站">
    <!-- ==================== 条件查询 ==================== -->
    <el-form :model="query" :inline="true" class="search-bar">
      <el-form-item label="用户名">
        <el-input v-model.trim="query.username" placeholder="请输入用户名" clearable style="width: 150px" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="真实姓名">
        <el-input v-model.trim="query.realName" placeholder="请输入真实姓名" clearable style="width: 140px" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="手机号">
        <el-input v-model.trim="query.phone" placeholder="请输入手机号" clearable style="width: 150px" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
          <el-option v-for="item in STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="角色">
        <el-select v-model="query.roleCode" placeholder="全部" clearable style="width: 150px">
          <el-option v-for="item in roleList" :key="item.id" :label="item.roleName" :value="item.roleCode" />
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
        <el-tag type="info" effect="plain">共 {{ total }} 个账号</el-tag>
      </div>
      <div class="table-toolbar__right">
        <el-button :icon="Refresh" :loading="loading" @click="loadData">刷新</el-button>
        <el-button v-perm="'system:user:add'" type="primary" :icon="Plus" @click="openCreate">新增用户</el-button>
      </div>
    </div>

    <!-- ==================== 数据表格 ==================== -->
    <el-table v-loading="loading" :data="list" border stripe height="520">
      <el-table-column prop="id" label="ID" width="70" align="center" />
      <el-table-column prop="username" label="用户名" width="130" fixed="left" />
      <el-table-column prop="realName" label="真实姓名" width="110" />
      <el-table-column prop="phone" label="手机号" width="130" />
      <el-table-column label="性别" width="80" align="center">
        <template #default="{ row }">{{ GENDER_NAME[row.gender] || dictLabel(GENDER_OPTIONS, row.gender) }}</template>
      </el-table-column>
      <el-table-column label="所属驿站" width="170" show-overflow-tooltip>
        <template #default="{ row }">{{ row.stationName || '未绑定' }}</template>
      </el-table-column>
      <el-table-column label="角色" min-width="150">
        <template #default="{ row }">
          <template v-if="row.roleNames && row.roleNames.length">
            <el-tag v-for="name in row.roleNames" :key="name" size="small" type="warning" effect="plain" class="mr-4">
              {{ name }}
            </el-tag>
          </template>
          <span v-else class="text-muted">未分配</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-switch
            :model-value="row.status"
            :active-value="1"
            :inactive-value="0"
            :loading="statusLoadingId === row.id"
            @change="(val) => handleStatusChange(row, val)"
          />
        </template>
      </el-table-column>
      <el-table-column label="最后登录时间" width="170">
        <template #default="{ row }">{{ formatDateTime(row.lastLoginTime) }}</template>
      </el-table-column>
      <el-table-column label="创建时间" width="170">
        <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="190" fixed="right">
        <template #default="{ row }">
          <div class="table-actions">
            <el-button v-perm="'system:user:edit'" link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button v-perm="'system:user:reset'" link type="warning" size="small" @click="handleResetPassword(row)">
              重置密码
            </el-button>
            <el-button v-perm="'system:user:delete'" link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </div>
        </template>
      </el-table-column>

      <template #empty>
        <el-empty description="暂无用户数据" :image-size="90" />
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
      :title="isEdit ? '编辑用户' : '新增用户'"
      width="640px"
      :close-on-click-modal="false"
      @closed="resetFormDialog"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="110px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model.trim="form.username" :disabled="isEdit" placeholder="3-20 位字母、数字或下划线" />
        </el-form-item>
        <el-form-item v-if="!isEdit" label="初始密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="6-20 位密码" />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model.trim="form.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model.trim="form.phone" placeholder="请输入 11 位手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="form.gender">
            <el-radio v-for="item in GENDER_OPTIONS" :key="item.value" :value="item.value">{{ item.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="所属驿站" prop="stationId">
          <el-select v-model="form.stationId" placeholder="不绑定驿站可留空" clearable style="width: 100%">
            <el-option v-for="item in stationList" :key="item.id" :label="item.stationName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色" prop="roleIds">
          <el-select v-model="form.roleIds" multiple placeholder="请选择角色（可多选）" style="width: 100%">
            <el-option v-for="item in roleList" :key="item.id" :label="item.roleName" :value="item.id" />
          </el-select>
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
 * 用户管理页（ADMIN）
 * 接口：分页查询 / 新增 / 编辑 / 删除 / 启用停用 / 重置密码
 * 权限：system:user:add / system:user:edit / system:user:delete / system:user:reset
 */
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, RefreshLeft, Search } from '@element-plus/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import {
  createUser,
  deleteUser,
  pageUsers,
  resetUserPassword,
  updateUser,
  updateUserStatus
} from '@/api/user'
import { listRoles } from '@/api/role'
import { listStations } from '@/api/station'
import { GENDER_NAME, GENDER_OPTIONS, STATUS_OPTIONS, dictLabel } from '@/utils/dict'
import { formatDateTime } from '@/utils/format'

/** 组件名：供 Layout 的 keep-alive include 使用 */
defineOptions({ name: 'SystemUser' })

/** 查询条件 */
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  username: '',
  realName: '',
  phone: '',
  status: '',
  roleCode: '',
  stationId: ''
})

/** 列表数据 */
const list = ref([])
const total = ref(0)
const loading = ref(false)
/** 状态切换 loading 的行 id */
const statusLoadingId = ref(null)

/** 下拉数据源 */
const roleList = ref([])
const stationList = ref([])

/** 组装查询参数（过滤空值） */
function buildParams() {
  const params = { ...query }
  Object.keys(params).forEach((key) => {
    if (params[key] === '' || params[key] === null || params[key] === undefined) delete params[key]
  })
  return params
}

/** 加载用户分页数据 */
async function loadData() {
  loading.value = true
  try {
    const data = await pageUsers(buildParams())
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

/** 重置筛选 */
function handleReset() {
  query.username = ''
  query.realName = ''
  query.phone = ''
  query.status = ''
  query.roleCode = ''
  query.stationId = ''
  query.pageNum = 1
  loadData()
}

/** 切换每页条数 */
function handleSizeChange() {
  query.pageNum = 1
  loadData()
}

/** 加载角色与驿站下拉（任一失败不影响列表展示） */
async function loadOptions() {
  try {
    const roles = await listRoles()
    roleList.value = Array.isArray(roles) ? roles : []
  } catch (e) {
    roleList.value = []
  }
  try {
    const stations = await listStations()
    stationList.value = Array.isArray(stations) ? stations : []
  } catch (e) {
    stationList.value = []
  }
}

/* ------------------------------------------------------------------
 * 启用 / 停用
 * ---------------------------------------------------------------- */

/**
 * 切换用户启用状态
 * @param {Object} row 用户行数据
 * @param {number} val 目标状态 1 启用 / 0 停用
 */
async function handleStatusChange(row, val) {
  const actionText = val === 1 ? '启用' : '停用'
  try {
    await ElMessageBox.confirm(`确定要${actionText}账号「${row.username}」吗？`, `${actionText}确认`, {
      confirmButtonText: `确定${actionText}`,
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (e) {
    // 用户取消：界面上的 switch 使用 model-value 绑定，未修改 row.status，无需回滚
    return
  }

  statusLoadingId.value = row.id
  try {
    await updateUserStatus(row.id, val)
    row.status = val
    ElMessage.success(`账号已${actionText}`)
  } catch (e) {
    // 失败时界面状态未变更，无需回滚
  } finally {
    statusLoadingId.value = null
  }
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
    username: '',
    password: '',
    realName: '',
    phone: '',
    gender: 1,
    stationId: null,
    roleIds: [],
    status: 1
  }
}

/** 用户表单数据 */
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
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度为 3-20 位', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]+$/, message: '用户名只能包含字母、数字和下划线', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入初始密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为 6-20 位', trigger: 'blur' }
  ],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  phone: [{ required: true, validator: validatePhone, trigger: 'blur' }],
  roleIds: [
    {
      required: true,
      validator: (rule, value, callback) => {
        if (!value || !value.length) callback(new Error('请至少选择一个角色'))
        else callback()
      },
      trigger: 'change'
    }
  ]
}

/** 打开新增对话框 */
function openCreate() {
  isEdit.value = false
  Object.assign(form, createFormData())
  formVisible.value = true
}

/**
 * 打开编辑对话框
 * @param {Object} row 用户行数据
 */
function openEdit(row) {
  isEdit.value = true
  Object.assign(form, createFormData())
  form.id = row.id
  form.username = row.username
  form.realName = row.realName
  form.phone = row.phone
  form.gender = row.gender ?? 1
  form.stationId = row.stationId ?? null
  form.roleIds = Array.isArray(row.roleIds) ? [...row.roleIds] : []
  form.status = row.status ?? 1
  formVisible.value = true
}

/** 对话框关闭后重置 */
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
      username: form.username,
      realName: form.realName,
      phone: form.phone,
      gender: form.gender,
      stationId: form.stationId,
      roleIds: form.roleIds,
      status: form.status
    }

    if (isEdit.value) {
      await updateUser(form.id, payload)
      ElMessage.success('用户信息修改成功')
    } else {
      payload.password = form.password
      await createUser(payload)
      ElMessage.success('用户新增成功')
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
 * 重置密码 / 删除
 * ---------------------------------------------------------------- */

/**
 * 重置指定用户密码
 * @param {Object} row 用户行数据
 */
async function handleResetPassword(row) {
  try {
    const { value } = await ElMessageBox.prompt(`为用户「${row.username}」设置新密码`, '重置密码', {
      confirmButtonText: '确定重置',
      cancelButtonText: '取消',
      inputType: 'password',
      inputPlaceholder: '请输入 6-20 位新密码',
      inputValidator: (val) => {
        if (!val) return '请输入新密码'
        if (val.length < 6 || val.length > 20) return '密码长度为 6-20 位'
        return true
      }
    })
    await resetUserPassword(row.id, value)
    ElMessage.success('密码重置成功，请告知用户使用新密码登录')
  } catch (e) {
    // 用户取消或接口异常（接口异常已由拦截器提示）
  }
}

/**
 * 删除用户（逻辑删除）
 * @param {Object} row 用户行数据
 */
async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除账号「${row.username}」吗？删除后不可恢复。`, '删除确认', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'error'
    })
  } catch (e) {
    return // 用户取消
  }

  try {
    await deleteUser(row.id)
    ElMessage.success('用户已删除')
    if (list.value.length === 1 && query.pageNum > 1) {
      query.pageNum -= 1
    }
    loadData()
  } catch (e) {
    // 错误提示已由拦截器统一处理
  }
}

onMounted(async () => {
  await loadOptions()
  loadData()
})
</script>

<style scoped>
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.mr-4 {
  margin-right: 4px;
}
</style>
