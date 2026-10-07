<template>
  <PageContainer title="个人中心" sub-title="查看并维护本人资料与登录密码">
    <el-row :gutter="12">
      <!-- ==================== 左侧：个人信息卡片 ==================== -->
      <el-col :xs="24" :md="8">
        <el-card shadow="never" class="profile-card">
          <div class="profile-card__avatar">
            <el-avatar :size="76" :src="userInfo.avatar || ''">
              {{ (userInfo.realName || userInfo.username || 'U').slice(0, 1) }}
            </el-avatar>
            <div class="profile-card__name">{{ userInfo.realName || userInfo.username || '-' }}</div>
            <div class="profile-card__username">@{{ userInfo.username || '-' }}</div>
            <div class="profile-card__roles">
              <el-tag v-for="role in userInfo.roles || []" :key="role" type="warning" effect="dark" size="small">
                {{ ROLE_CODE_NAME[role] || role }}
              </el-tag>
            </div>
          </div>

          <el-divider />

          <el-descriptions :column="1" size="small">
            <el-descriptions-item label="手机号">{{ userInfo.phone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="性别">{{ GENDER_NAME[userInfo.gender] || '未设置' }}</el-descriptions-item>
            <el-descriptions-item label="所属驿站">{{ userInfo.stationName || '未绑定' }}</el-descriptions-item>
            <el-descriptions-item label="最后登录">{{ formatDateTime(userInfo.lastLoginTime) }}</el-descriptions-item>
          </el-descriptions>

          <el-button class="mt-12 w-100" :icon="Refresh" :loading="loadingUser" @click="loadUserInfo(true)">
            刷新个人信息
          </el-button>
        </el-card>
      </el-col>

      <!-- ==================== 右侧：资料 / 密码 ==================== -->
      <el-col :xs="24" :md="16">
        <el-card shadow="never" class="profile-card">
          <el-tabs v-model="activeTab">
            <!-- ---------------- 基本资料 ---------------- -->
            <el-tab-pane label="基本资料" name="profile">
              <el-form ref="profileFormRef" :model="profileForm" :rules="profileRules" label-width="110px" class="tab-form">
                <el-form-item label="用户名">
                  <el-input :model-value="userInfo.username" disabled />
                  <div class="form-tip">用户名用于登录，不可修改</div>
                </el-form-item>
                <el-form-item label="真实姓名" prop="realName">
                  <el-input v-model.trim="profileForm.realName" placeholder="请输入真实姓名" />
                </el-form-item>
                <el-form-item label="手机号" prop="phone">
                  <el-input v-model.trim="profileForm.phone" placeholder="请输入 11 位手机号" maxlength="11" />
                  <div class="form-tip">普通用户仅能查询本人手机号名下的快件，请确保手机号准确</div>
                </el-form-item>
                <el-form-item label="性别" prop="gender">
                  <el-radio-group v-model="profileForm.gender">
                    <el-radio v-for="item in GENDER_OPTIONS" :key="item.value" :value="item.value">{{ item.label }}</el-radio>
                  </el-radio-group>
                </el-form-item>
                <el-form-item label="头像地址" prop="avatar">
                  <el-input v-model.trim="profileForm.avatar" placeholder="可填写图片 URL，留空则显示姓名首字" clearable />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="savingProfile" @click="submitProfile">保存资料</el-button>
                  <el-button @click="resetProfileForm">重置</el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>

            <!-- ---------------- 修改密码 ---------------- -->
            <el-tab-pane label="修改密码" name="password">
              <el-alert
                type="warning"
                :closable="false"
                show-icon
                class="mb-12"
                title="安全提示"
                description="密码修改成功后需要重新登录，请使用新密码登录系统。"
              />

              <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="110px" class="tab-form">
                <el-form-item label="原密码" prop="oldPassword">
                  <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入当前登录密码" />
                </el-form-item>
                <el-form-item label="新密码" prop="newPassword">
                  <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="6-20 位新密码" />
                </el-form-item>
                <el-form-item label="确认新密码" prop="confirmPassword">
                  <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="savingPassword" @click="submitPassword">确认修改</el-button>
                  <el-button @click="resetPwdForm">清空</el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>
    </el-row>
  </PageContainer>
</template>

<script setup>
/**
 * 个人中心
 * 功能：
 *  - 展示本人资料（用户名、姓名、手机号、性别、所属驿站、角色、最后登录时间）
 *  - 修改本人资料 PUT /api/users/profile
 *  - 修改本人密码 PUT /api/users/self/password（成功后强制重新登录）
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import { updateProfile, updateSelfPassword } from '@/api/user'
import { useUserStore } from '@/stores/user'
import { GENDER_NAME, GENDER_OPTIONS, ROLE_CODE_NAME } from '@/utils/dict'
import { formatDateTime } from '@/utils/format'

const router = useRouter()
const userStore = useUserStore()

/** 当前页签 */
const activeTab = ref('profile')
/** 资料刷新 loading */
const loadingUser = ref(false)

/** 用户信息（优先取 store 中的缓存，加载后更新） */
const userInfo = computed(() => userStore.userInfo || {})

/* ------------------------------------------------------------------
 * 基本资料
 * ---------------------------------------------------------------- */
const profileFormRef = ref(null)
const savingProfile = ref(false)

/** 资料表单 */
const profileForm = reactive({
  realName: '',
  phone: '',
  gender: 1,
  avatar: ''
})

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

/** 资料校验规则 */
const profileRules = {
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' },
    { min: 2, max: 20, message: '姓名长度为 2-20 位', trigger: 'blur' }
  ],
  phone: [{ required: true, validator: validatePhone, trigger: 'blur' }]
}

/** 用当前用户信息回填资料表单 */
function fillProfileForm() {
  const info = userStore.userInfo || {}
  profileForm.realName = info.realName || ''
  profileForm.phone = info.phone || ''
  profileForm.gender = info.gender ?? 1
  profileForm.avatar = info.avatar || ''
  profileFormRef.value?.clearValidate()
}

/** 重置资料表单为当前用户信息 */
function resetProfileForm() {
  fillProfileForm()
  ElMessage.info('已恢复为当前资料')
}

/**
 * 加载用户信息
 * @param {boolean} force 是否强制重新请求
 */
async function loadUserInfo(force = false) {
  loadingUser.value = true
  try {
    await userStore.fetchUserInfo(force)
    fillProfileForm()
  } catch (e) {
    // 请求失败时保留本地缓存数据
    fillProfileForm()
  } finally {
    loadingUser.value = false
  }
}

/** 提交资料修改 */
async function submitProfile() {
  if (!profileFormRef.value) return
  try {
    await profileFormRef.value.validate()
  } catch (e) {
    ElMessage.warning('请先完善标红的必填项')
    return
  }

  savingProfile.value = true
  try {
    await updateProfile({
      realName: profileForm.realName,
      phone: profileForm.phone,
      gender: profileForm.gender,
      avatar: profileForm.avatar || null
    })
    ElMessage.success('资料修改成功')
    // 刷新 store 中的用户信息，顶部展示同步更新
    await loadUserInfo(true)
  } catch (e) {
    // 错误提示已由拦截器统一处理
  } finally {
    savingProfile.value = false
  }
}

/* ------------------------------------------------------------------
 * 修改密码
 * ---------------------------------------------------------------- */
const pwdFormRef = ref(null)
const savingPassword = ref(false)

/** 密码表单 */
const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

/**
 * 确认新密码校验器
 */
function validateConfirm(rule, value, callback) {
  if (!value) {
    callback(new Error('请再次输入新密码'))
  } else if (value !== pwdForm.newPassword) {
    callback(new Error('两次输入的新密码不一致'))
  } else {
    callback()
  }
}

/** 与旧密码不同的校验器 */
function validateNewPassword(rule, value, callback) {
  if (!value) {
    callback(new Error('请输入新密码'))
  } else if (value.length < 6 || value.length > 20) {
    callback(new Error('新密码长度为 6-20 位'))
  } else if (value === pwdForm.oldPassword) {
    callback(new Error('新密码不能与原密码相同'))
  } else {
    callback()
  }
}

/** 密码校验规则 */
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [{ required: true, validator: validateNewPassword, trigger: 'blur' }],
  confirmPassword: [{ required: true, validator: validateConfirm, trigger: 'blur' }]
}

/** 清空密码表单 */
function resetPwdForm() {
  pwdForm.oldPassword = ''
  pwdForm.newPassword = ''
  pwdForm.confirmPassword = ''
  pwdFormRef.value?.clearValidate()
}

/** 提交密码修改：成功后清理登录态并跳转登录页 */
async function submitPassword() {
  if (!pwdFormRef.value) return
  try {
    await pwdFormRef.value.validate()
  } catch (e) {
    ElMessage.warning('请先完善标红的必填项')
    return
  }

  savingPassword.value = true
  try {
    await updateSelfPassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword
    })
    ElMessage.success('密码修改成功，请使用新密码重新登录')
    resetPwdForm()
    userStore.resetState()
    router.replace('/login')
  } catch (e) {
    // 错误提示已由拦截器统一处理（例如原密码错误）
  } finally {
    savingPassword.value = false
  }
}

onMounted(() => {
  fillProfileForm()
  loadUserInfo(true)
})
</script>

<style scoped>
.profile-card {
  border-radius: 8px;
  border: none;
  margin-bottom: 12px;
}

.profile-card__avatar {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px 0;
}

.profile-card__name {
  margin-top: 10px;
  font-size: 18px;
  font-weight: 600;
  color: #1f2d3d;
}

.profile-card__username {
  margin-top: 2px;
  font-size: 13px;
  color: #909399;
}

.profile-card__roles {
  display: flex;
  gap: 6px;
  margin-top: 8px;
  flex-wrap: wrap;
  justify-content: center;
}

.tab-form {
  max-width: 520px;
  padding-top: 8px;
}

.form-tip {
  width: 100%;
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
}
</style>
