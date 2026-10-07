<template>
  <PageContainer title="角色权限" sub-title="维护角色基本信息并为角色分配菜单与按钮权限">
    <el-row :gutter="12">
      <!-- ==================== 左侧：角色列表 ==================== -->
      <el-col :xs="24" :md="10" :lg="9">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="panel-card__header">
              <span class="panel-card__title">角色列表</span>
              <el-button v-perm="'system:role:list'" type="primary" size="small" :icon="Plus" @click="openCreate">
                新增角色
              </el-button>
            </div>
          </template>

          <el-table
            v-loading="loading"
            :data="roleList"
            border
            stripe
            highlight-current-row
            height="470"
            @current-change="handleRoleSelect"
          >
            <el-table-column prop="roleName" label="角色名称" min-width="110" />
            <el-table-column prop="roleCode" label="编码" width="100" align="center" />
            <el-table-column prop="description" label="描述" min-width="140" show-overflow-tooltip />
            <el-table-column label="状态" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="dictType(STATUS_OPTIONS, row.status)" size="small">
                  {{ dictLabel(STATUS_OPTIONS, row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="110" align="center">
              <template #default="{ row }">
                <div class="table-actions">
                  <el-button v-perm="'system:role:list'" link type="primary" size="small" @click.stop="openEdit(row)">
                    编辑
                  </el-button>
                  <el-button v-perm="'system:role:list'" link type="danger" size="small" @click.stop="handleDelete(row)">
                    删除
                  </el-button>
                </div>
              </template>
            </el-table-column>

            <template #empty>
              <el-empty description="暂无角色数据" :image-size="80" />
            </template>
          </el-table>

          <div class="pagination-wrap">
            <el-pagination
              v-model:current-page="roleQuery.pageNum"
              v-model:page-size="roleQuery.pageSize"
              background
              small
              :page-sizes="[10, 20, 50]"
              :total="roleTotal"
              layout="total, sizes, prev, pager, next"
              @size-change="handleRoleSizeChange"
              @current-change="loadRoles"
            />
          </div>
        </el-card>
      </el-col>

      <!-- ==================== 右侧：权限树 ==================== -->
      <el-col :xs="24" :md="14" :lg="15">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="panel-card__header">
              <span class="panel-card__title">
                权限分配
                <el-tag v-if="currentRole" type="primary" effect="dark" class="ml-8">{{ currentRole.roleName }}</el-tag>
              </span>
              <div class="panel-card__actions">
                <el-checkbox v-model="permissionType" true-value="ALL" false-value="MENU" @change="loadPermissionTree">
                  显示按钮权限
                </el-checkbox>
                <el-button size="small" :icon="Refresh" :loading="treeLoading" @click="reloadPermission">刷新权限树</el-button>
                <el-button
                  v-perm="'system:role:assign'"
                  type="primary"
                  size="small"
                  :icon="Check"
                  :loading="assigning"
                  :disabled="!currentRole"
                  @click="handleAssign"
                >
                  保存权限
                </el-button>
              </div>
            </div>
          </template>

          <!-- 未选择角色 -->
          <el-empty
            v-if="!currentRole"
            description="请先选择左侧角色，再为其勾选权限"
            :image-size="100"
          />

          <div v-else v-loading="treeLoading" class="perm-tree-wrap">
            <el-alert
              type="info"
              :closable="false"
              show-icon
              class="mb-12"
              title="勾选说明"
              description="权限树采用父子独立勾选（check-strictly）模式，勾选父节点不会自动勾选子节点，回填与提交完全一致，避免出现权限丢失。"
            />

            <el-tree
              ref="treeRef"
              :data="permTree"
              show-checkbox
              node-key="id"
              check-strictly
              default-expand-all
              :props="{ label: 'permName', children: 'children' }"
              class="perm-tree"
            >
              <template #default="{ data }">
                <span class="perm-node">
                  <span class="perm-node__name">{{ data.permName }}</span>
                  <span class="perm-node__code">{{ data.permCode }}</span>
                  <el-tag
                    size="small"
                    :type="data.permType === 'MENU' ? 'primary' : 'warning'"
                    effect="plain"
                    class="perm-node__tag"
                  >
                    {{ data.permType === 'MENU' ? '菜单' : '按钮' }}
                  </el-tag>
                </span>
              </template>
            </el-tree>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- ==================== 新增 / 编辑角色对话框 ==================== -->
    <el-dialog
      v-model="formVisible"
      :title="isEdit ? '编辑角色' : '新增角色'"
      width="560px"
      :close-on-click-modal="false"
      @closed="resetFormDialog"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="角色编码" prop="roleCode">
          <el-input
            v-model.trim="form.roleCode"
            :disabled="isEdit"
            placeholder="例如 ADMIN / STAFF / USER"
            @input="(val) => (form.roleCode = val.toUpperCase())"
          />
        </el-form-item>
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model.trim="form.roleName" placeholder="例如 驿站员工" />
        </el-form-item>
        <el-form-item label="角色描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="请描述该角色的职责范围"
          />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="1" :max="999" style="width: 100%" />
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
 * 角色权限页（ADMIN）
 * 左侧：角色分页列表 + 新增/编辑/删除
 * 右侧：权限树勾选分配（GET /api/roles/{id}/permissions 回填，PUT 提交）
 * 说明：权限树使用 check-strictly（父子独立勾选），保证「回填的 id 集合」与「提交的 id 集合」完全一致
 */
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Plus, Refresh } from '@element-plus/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import {
  assignRolePermissions,
  createRole,
  deleteRole,
  getPermissionTree,
  getRolePermissions,
  pageRoles,
  updateRole
} from '@/api/role'
import { STATUS_OPTIONS, dictLabel, dictType } from '@/utils/dict'

/** 角色列表分页查询条件 */
const roleQuery = reactive({ pageNum: 1, pageSize: 10 })
/** 角色列表与总数 */
const roleList = ref([])
const roleTotal = ref(0)
const loading = ref(false)

/** 当前选中的角色 */
const currentRole = ref(null)

/** 权限树 */
const treeRef = ref(null)
const permTree = ref([])
const treeLoading = ref(false)
/** 权限树类型：ALL 含按钮权限，MENU 仅菜单 */
const permissionType = ref('ALL')
/** 权限保存中 */
const assigning = ref(false)

/** 加载角色分页列表 */
async function loadRoles() {
  loading.value = true
  try {
    const data = await pageRoles({ pageNum: roleQuery.pageNum, pageSize: roleQuery.pageSize })
    roleList.value = (data && data.list) || []
    roleTotal.value = (data && data.total) || 0
  } catch (e) {
    roleList.value = []
    roleTotal.value = 0
  } finally {
    loading.value = false
  }
}

/** 切换每页条数 */
function handleRoleSizeChange() {
  roleQuery.pageNum = 1
  loadRoles()
}

/** 加载权限树 */
async function loadPermissionTree() {
  treeLoading.value = true
  try {
    const data = await getPermissionTree({ type: permissionType.value })
    permTree.value = Array.isArray(data) ? data : []
    // 重新加载树后需要重新回填当前角色的勾选状态
    if (currentRole.value) {
      await fillRolePermissions(currentRole.value.id)
    }
  } catch (e) {
    permTree.value = []
  } finally {
    treeLoading.value = false
  }
}

/**
 * 回填某角色已分配的权限
 * @param {number} roleId 角色 id
 */
async function fillRolePermissions(roleId) {
  try {
    const ids = await getRolePermissions(roleId)
    const checked = Array.isArray(ids) ? ids : []
    // 等待树渲染完成后再设置勾选，避免节点尚未创建导致设置失效
    await nextTick()
    if (treeRef.value) {
      treeRef.value.setCheckedKeys(checked, false)
    }
  } catch (e) {
    if (treeRef.value) treeRef.value.setCheckedKeys([], false)
  }
}

/**
 * 选中角色：加载该角色的权限
 * @param {Object} row 角色行数据
 */
async function handleRoleSelect(row) {
  if (!row) return
  // 切换角色前提示未保存的修改
  if (currentRole.value && currentRole.value.id !== row.id && treeRef.value) {
    const checkedCount = treeRef.value.getCheckedKeys().length
    if (checkedCount > 0) {
      // 这里不阻塞流程，仅做轻量提示：勾选内容会随角色切换而重新回填
      ElMessage.info('已切换到新角色，权限树将重新加载该角色的权限')
    }
  }
  currentRole.value = row
  await fillRolePermissions(row.id)
}

/** 刷新权限树（重新请求接口 + 回填） */
async function reloadPermission() {
  await loadPermissionTree()
  ElMessage.success('权限树已刷新')
}

/** 保存权限分配 */
async function handleAssign() {
  if (!currentRole.value || !treeRef.value) {
    ElMessage.warning('请先选择要分配权限的角色')
    return
  }
  // check-strictly 模式下，getCheckedKeys 即为用户勾选的完整集合
  const permIds = treeRef.value.getCheckedKeys(false)
  if (!permIds.length) {
    try {
      await ElMessageBox.confirm('当前未勾选任何权限，保存后该角色将没有任何权限，确定继续吗？', '提示', {
        confirmButtonText: '确定保存',
        cancelButtonText: '取消',
        type: 'warning'
      })
    } catch (e) {
      return
    }
  }

  assigning.value = true
  try {
    await assignRolePermissions(currentRole.value.id, permIds)
    ElMessage.success(`已为「${currentRole.value.roleName}」分配 ${permIds.length} 项权限`)
    await fillRolePermissions(currentRole.value.id)
  } catch (e) {
    // 错误提示已由拦截器统一处理
  } finally {
    assigning.value = false
  }
}

/* ------------------------------------------------------------------
 * 新增 / 编辑 / 删除角色
 * ---------------------------------------------------------------- */
const formVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref(null)

/** 角色表单默认值工厂 */
function createFormData() {
  return {
    id: null,
    roleCode: '',
    roleName: '',
    description: '',
    sort: 1,
    status: 1
  }
}

/** 角色表单数据 */
const form = reactive(createFormData())

/** 表单校验规则 */
const formRules = {
  roleCode: [
    { required: true, message: '请输入角色编码', trigger: 'blur' },
    { pattern: /^[A-Z_]{2,20}$/, message: '角色编码为 2-20 位大写字母或下划线', trigger: 'blur' }
  ],
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }]
}

/** 打开新增角色对话框 */
function openCreate() {
  isEdit.value = false
  Object.assign(form, createFormData())
  formVisible.value = true
}

/**
 * 打开编辑角色对话框
 * @param {Object} row 角色行数据
 */
function openEdit(row) {
  isEdit.value = true
  Object.assign(form, createFormData())
  form.id = row.id
  form.roleCode = row.roleCode
  form.roleName = row.roleName
  form.description = row.description || ''
  form.sort = row.sort ?? 1
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
      roleCode: form.roleCode,
      roleName: form.roleName,
      description: form.description,
      sort: form.sort,
      status: form.status
    }
    if (isEdit.value) {
      await updateRole(form.id, payload)
      ElMessage.success('角色修改成功')
      // 同步刷新左侧选中项的展示
      if (currentRole.value && currentRole.value.id === form.id) {
        currentRole.value = { ...currentRole.value, ...payload }
      }
    } else {
      await createRole(payload)
      ElMessage.success('角色新增成功')
    }
    formVisible.value = false
    loadRoles()
  } catch (e) {
    // 错误提示已由拦截器统一处理
  } finally {
    submitting.value = false
  }
}

/**
 * 删除角色
 * @param {Object} row 角色行数据
 */
async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除角色「${row.roleName}」吗？删除后该角色下的用户将失去对应权限。`, '删除确认', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'error'
    })
  } catch (e) {
    return // 用户取消
  }

  try {
    await deleteRole(row.id)
    ElMessage.success('角色已删除')
    if (currentRole.value && currentRole.value.id === row.id) {
      currentRole.value = null
      treeRef.value?.setCheckedKeys([], false)
    }
    if (roleList.value.length === 1 && roleQuery.pageNum > 1) {
      roleQuery.pageNum -= 1
    }
    loadRoles()
  } catch (e) {
    // 错误提示已由拦截器统一处理（例如该角色下仍存在关联用户）
  }
}

onMounted(async () => {
  await loadRoles()
  await loadPermissionTree()
})
</script>

<style scoped>
.panel-card {
  border-radius: 8px;
  border: none;
  margin-bottom: 12px;
}

.panel-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
}

.panel-card__title {
  display: flex;
  align-items: center;
  font-size: 15px;
  font-weight: 600;
  color: var(--es-text-1);
}

.panel-card__actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.perm-tree-wrap {
  min-height: 420px;
  max-height: 560px;
  overflow-y: auto;
}

.perm-tree {
  background: transparent;
}

.perm-node {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.perm-node__name {
  color: var(--es-text-1);
}

.perm-node__code {
  font-family: Consolas, Monaco, monospace;
  font-size: 12px;
  color: #a8abb2;
}

.perm-node__tag {
  transform: scale(0.9);
}
</style>
