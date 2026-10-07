<template>
  <PageContainer title="个人中心" sub-title="维护本人资料、账号安全与最近动态">
    <!-- ==================== 一、个人信息横幅 ==================== -->
    <div class="pf-banner">
      <span class="pf-banner__glow pf-banner__glow--blue" />
      <span class="pf-banner__glow pf-banner__glow--teal" />
      <span class="pf-banner__grid" />

      <!-- 上排：头像 + 身份信息 -->
      <div class="pf-banner__top">
        <div class="pf-banner__identity">
          <div class="pf-avatar" title="点击更换头像" :style="avatarStyle" @click="openAvatarDialog">
            <img v-if="avatarIsImage" :src="avatarValue" alt="用户头像" />
            <span v-else class="pf-avatar__initial">{{ avatarInitial }}</span>
            <span class="pf-avatar__mask">
              <el-icon><CameraFilled /></el-icon>
              <em>更换头像</em>
            </span>
          </div>

          <div class="pf-banner__meta">
            <div class="pf-banner__name-row">
              <h3 class="pf-banner__name">{{ userInfo.realName || userInfo.username || '未命名用户' }}</h3>
              <span class="pf-banner__online">
                <i class="es-live-dot pf-banner__dot" />
                已登录
              </span>
            </div>
            <div class="pf-banner__account">@{{ userInfo.username || '-' }}</div>
            <div class="pf-banner__roles">
              <el-tag
                v-for="role in roleList"
                :key="role"
                size="small"
                effect="dark"
                class="pf-banner__role"
              >
                {{ ROLE_CODE_NAME[role] || role }}
              </el-tag>
              <span v-if="!roleList.length" class="pf-banner__account">未分配角色</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 下排：关键信息 + 刷新按钮（右下角） -->
      <div class="pf-banner__bottom">
        <div class="pf-banner__info">
          <el-tooltip
            v-for="item in bannerInfos"
            :key="item.label"
            :content="item.tip"
            :disabled="!item.tip"
            placement="top"
          >
            <div class="pf-info" :class="{ 'is-action': item.action }" @click="item.action && togglePhoneVisible()">
              <el-icon class="pf-info__icon"><component :is="item.icon" /></el-icon>
              <div class="pf-info__text">
                <div class="pf-info__label">{{ item.label }}</div>
                <div class="pf-info__value">
                  {{ item.value }}
                  <el-icon v-if="item.action" class="pf-info__eye">
                    <component :is="phoneVisible ? 'Hide' : 'View'" />
                  </el-icon>
                </div>
              </div>
            </div>
          </el-tooltip>
        </div>

        <div class="pf-banner__actions">
          <el-button :icon="Refresh" :loading="loadingUser" @click="refreshAll">刷新个人信息</el-button>
        </div>
      </div>
    </div>

    <!-- ==================== 二、数据统计小卡 ==================== -->
    <el-row :gutter="12" class="mt-12">
      <el-col v-for="card in statCards" :key="card.label" :xs="12" :sm="12" :md="6" class="pf-stat-col">
        <StatCard
          :label="card.label"
          :value="card.value"
          :unit="card.unit"
          :icon="card.icon"
          :preset="card.preset"
          :sub-text="card.subText"
        />
      </el-col>
    </el-row>

    <!-- ==================== 三、左右两栏 ==================== -->
    <el-row :gutter="12" class="mt-12">
      <!-- ---------------- 左栏：基本资料 / 修改密码 ---------------- -->
      <el-col :xs="24" :md="14">
        <!-- 基本资料 -->
        <el-card shadow="never" class="pf-card es-top-accent">
          <template #header>
            <div class="pf-card__header">
              <span class="es-title-bar" />
              <span class="pf-card__title">基本资料</span>
              <div class="pf-card__extra">
                <el-button link type="primary" :icon="CameraFilled" @click="openAvatarDialog">更换头像</el-button>
              </div>
            </div>
          </template>

          <el-form ref="profileFormRef" :model="profileForm" :rules="profileRules" label-width="110px">
            <el-form-item label="用户名">
              <el-input :model-value="userInfo.username" disabled />
              <div class="pf-form-tip">用户名用于登录，不可修改</div>
            </el-form-item>
            <el-form-item label="真实姓名" prop="realName">
              <el-input v-model.trim="profileForm.realName" placeholder="请输入真实姓名" clearable />
            </el-form-item>
            <el-form-item label="手机号" prop="phone">
              <el-input v-model.trim="profileForm.phone" placeholder="请输入 11 位手机号" maxlength="11" clearable />
              <div class="pf-form-tip">普通用户仅能查询本人手机号名下的快件，请确保手机号准确</div>
            </el-form-item>
            <el-form-item label="性别" prop="gender">
              <el-radio-group v-model="profileForm.gender">
                <el-radio v-for="item in GENDER_OPTIONS" :key="item.value" :value="item.value">{{ item.label }}</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="头像地址" prop="avatar">
              <el-input v-model.trim="profileForm.avatar" placeholder="可填写图片 URL，留空则显示姓名首字" clearable />
              <div class="pf-form-tip">建议点击「更换头像」选择内置渐变头像，无需外部图床</div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="savingProfile" @click="submitProfile">保存资料</el-button>
              <el-button @click="resetProfileForm">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 修改密码 -->
        <el-card shadow="never" class="pf-card es-top-accent">
          <template #header>
            <div class="pf-card__header">
              <span class="es-title-bar" />
              <span class="pf-card__title">修改密码</span>
            </div>
          </template>

          <el-alert
            type="warning"
            :closable="false"
            show-icon
            class="mb-12"
            title="安全提示"
            description="密码修改成功后需要重新登录，请使用新密码登录系统。"
          />

          <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="110px">
            <el-form-item label="原密码" prop="oldPassword">
              <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入当前登录密码" />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="6-20 位新密码" />
              <div class="pf-form-tip">新密码需 6-20 位，且不能与原密码相同</div>
            </el-form-item>
            <el-form-item label="确认新密码" prop="confirmPassword">
              <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="savingPassword" @click="submitPassword">确认修改</el-button>
              <el-button @click="resetPwdForm">清空</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- ---------------- 右栏：账号安全 / 最近动态 ---------------- -->
      <el-col :xs="24" :md="10">
        <!-- 账号安全 -->
        <el-card shadow="never" class="pf-card es-top-accent">
          <template #header>
            <div class="pf-card__header">
              <span class="es-title-bar" />
              <span class="pf-card__title">账号安全</span>
              <div class="pf-card__extra">
                <el-tag size="small" type="success" effect="plain">令牌有效</el-tag>
              </div>
            </div>
          </template>

          <ul class="pf-security">
            <li v-for="row in securityRows" :key="row.label" class="pf-security__item">
              <div class="pf-security__icon">
                <el-icon><component :is="row.icon" /></el-icon>
              </div>
              <div class="pf-security__body">
                <div class="pf-security__label">{{ row.label }}</div>
                <div class="pf-security__value">{{ row.value }}</div>
                <div v-if="row.desc" class="pf-security__desc">{{ row.desc }}</div>
              </div>
            </li>
          </ul>

          <div class="pf-security__note">
            <el-icon><InfoFilled /></el-icon>
            <span>修改密码后系统会引导重新登录，密码以 BCrypt 哈希形式存储，数据库中不保存明文。</span>
          </div>
        </el-card>

        <!-- 最近动态 -->
        <el-card shadow="never" class="pf-card es-top-accent">
          <template #header>
            <div class="pf-card__header">
              <span class="es-title-bar" />
              <span class="pf-card__title">{{ isStaffLike ? '最近通知动态' : '我的快件动态' }}</span>
              <div class="pf-card__extra">
                <el-button link :icon="Refresh" :loading="loadingActivity" @click="loadActivities">刷新</el-button>
                <el-button v-if="canViewNotify" link type="primary" @click="goNotifyList">查看全部</el-button>
                <el-button v-else-if="canViewParcel" link type="primary" @click="goParcelList">查看全部</el-button>
              </div>
            </div>
          </template>

          <el-skeleton v-if="loadingActivity" :rows="4" animated />
          <el-empty v-else-if="!activities.length" description="暂无动态记录" :image-size="90" />
          <el-timeline v-else class="pf-timeline">
            <el-timeline-item
              v-for="item in activities"
              :key="item.id"
              :timestamp="item.timeText"
              :type="item.nodeType"
              placement="top"
            >
              <div class="pf-timeline__head">
                <el-tag size="small" :type="item.tagType" effect="light">{{ item.title }}</el-tag>
                <span class="pf-timeline__waybill">{{ item.waybillNo }}</span>
                <el-tag size="small" :type="item.resultType" effect="plain">{{ item.result }}</el-tag>
              </div>
              <div v-if="item.desc" class="pf-timeline__desc">{{ item.desc }}</div>
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>
    </el-row>

    <!-- ==================== 四、头像设置对话框 ==================== -->
    <el-dialog v-model="avatarDialogVisible" title="更换头像" width="520px" append-to-body>
      <div class="pf-avatar-dialog">
        <div class="pf-avatar-dialog__preview">
          <div class="pf-avatar pf-avatar--preview" :style="draftStyle">
            <img v-if="draftIsImage" :src="avatarDraft" alt="头像预览" />
            <span v-else class="pf-avatar__initial">{{ avatarInitial }}</span>
          </div>
          <div class="pf-avatar-dialog__intro">
            <div class="text-bold">头像预览</div>
            <div class="text-muted">
              项目暂未提供文件上传接口，可粘贴图片链接，或直接选用内置渐变头像（保存后全站生效）。
            </div>
          </div>
        </div>

        <el-tabs v-model="avatarTab">
          <el-tab-pane label="图片链接" name="url">
            <el-input v-model.trim="avatarDraft" placeholder="https://example.com/avatar.png" clearable />
            <div class="pf-avatar-dialog__tip">留空并保存，即恢复为「姓名首字」默认头像</div>
          </el-tab-pane>
          <el-tab-pane label="内置渐变头像" name="preset">
            <div class="pf-avatar-grid">
              <div
                v-for="item in AVATAR_PRESETS"
                :key="item.id"
                class="pf-avatar-grid__item"
                :class="{ 'is-active': avatarDraft === item.uri }"
                :style="{ background: item.css }"
                :title="item.name"
                @click="avatarDraft = item.uri"
              >
                <span>{{ avatarInitial }}</span>
                <el-icon v-if="avatarDraft === item.uri" class="pf-avatar-grid__check"><Select /></el-icon>
              </div>
            </div>
            <div class="pf-avatar-dialog__tip">共 {{ AVATAR_PRESETS.length }} 款品牌渐变头像，与主题配色一致</div>
          </el-tab-pane>
        </el-tabs>

        <div class="pf-avatar-dialog__value">
          <span class="text-muted">将保存的头像值：</span>
          <code>{{ avatarDraft || '（空，使用姓名首字）' }}</code>
        </div>
      </div>

      <template #footer>
        <el-button @click="avatarDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingAvatar" @click="saveAvatar">保存头像</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup>
/**
 * 个人中心（v1.1 视觉与功能升级）
 * 功能：
 *  - 个人信息横幅：头像（内置渐变头像 / 图片链接）、真实姓名、账号、角色、手机号脱敏、性别、所属驿站等
 *  - 数据统计小卡：角色数、权限数、待催取逾期件（员工/管理员）或我的快件（普通用户）、账号状态
 *  - 基本资料：PUT /api/users/profile
 *  - 修改密码：PUT /api/users/self/password（成功后清理登录态并跳转登录页）
 *  - 账号安全：展示令牌类型 / 有效期 / 本次登录时间 / 密码加密方式（纯展示，未新增后端接口）
 *  - 最近动态：员工与管理员取通知记录 GET /api/notifications/page，普通用户取本人快件 GET /api/parcels/page
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { CameraFilled, Refresh } from '@element-plus/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import StatCard from '@/components/StatCard.vue'
import { updateProfile, updateSelfPassword } from '@/api/user'
import { getOverdueCount, pageParcels } from '@/api/parcel'
import { pageNotifications } from '@/api/notify'
import { useUserStore } from '@/stores/user'
import {
  GENDER_NAME,
  GENDER_OPTIONS,
  NOTIFY_CHANNEL_NAME,
  NOTIFY_TYPE,
  NOTIFY_TYPE_NAME,
  PARCEL_STATUS,
  PARCEL_STATUS_NAME,
  ROLE_CODE_NAME,
  SEND_STATUS_NAME,
  dictType
} from '@/utils/dict'
import { formatDateTime, maskPhone } from '@/utils/format'

const router = useRouter()
const userStore = useUserStore()

/** 资料刷新 loading */
const loadingUser = ref(false)
/** 手机号是否明文展示 */
const phoneVisible = ref(false)
/** 本次进入个人中心的时间（前端记录，非后端字段） */
const sessionLoginTime = ref(new Date())

/** 用户信息（优先取 store 中的缓存，加载后更新） */
const userInfo = computed(() => userStore.userInfo || {})
/** 角色编码集合 */
const roleList = computed(() => userStore.roles || [])
/** 角色中文名拼接，用于统计卡副标题 */
const roleText = computed(() => roleList.value.map((r) => ROLE_CODE_NAME[r] || r).join('、') || '未分配角色')
/** 员工或管理员：可见通知记录与逾期数据 */
const isStaffLike = computed(() => userStore.isAdmin || roleList.value.includes('STAFF'))
/** 是否具备通知记录查看权限 */
const canViewNotify = computed(() => isStaffLike.value && userStore.hasPerm('notify:list'))
/** 是否具备快件查询权限 */
const canViewParcel = computed(() => userStore.hasPerm('parcel:list'))

/* ==================================================================
 * 一、个人信息横幅
 * ================================================================ */

/** 内置渐变头像（存 SVG data-uri，长度控制在 238 字符以内，兼容 avatar VARCHAR(255)） */
function buildAvatarUri(from, to) {
  const color = (hex) => `%23${String(hex).replace('#', '')}`
  return (
    "data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg'>" +
    `<linearGradient id='a'><stop offset='0' stop-color='${color(from)}'/>` +
    `<stop offset='1' stop-color='${color(to)}'/></linearGradient>` +
    "<rect width='100%' height='100%' fill='url(%23a)'/></svg>"
  )
}

/** 6-8 款内置渐变头像：CSS 用于本页渲染，data-uri 用于落库与顶栏展示 */
const AVATAR_PRESETS = [
  { id: 'brand', name: '品牌蓝绿', from: '#1a6dff', to: '#16d3c8' },
  { id: 'navy', name: '深海蓝', from: '#0b2450', to: '#1a6dff' },
  { id: 'teal', name: '青绿', from: '#0fb98f', to: '#22d3ee' },
  { id: 'violet', name: '紫罗兰', from: '#7c4dff', to: '#22d3ee' },
  { id: 'sunset', name: '落日橙红', from: '#f59f00', to: '#f04438' },
  { id: 'rose', name: '玫红', from: '#f04438', to: '#7c4dff' },
  { id: 'ink', name: '墨青', from: '#103061', to: '#0fb98f' },
  { id: 'bluepurple', name: '蓝紫', from: '#1a6dff', to: '#7c4dff' }
].map((item) => ({
  ...item,
  css: `linear-gradient(135deg, ${item.from} 0%, ${item.to} 100%)`,
  uri: buildAvatarUri(item.from, item.to)
}))

/** 根据头像值匹配内置头像 */
function findPreset(value) {
  return AVATAR_PRESETS.find((item) => item.uri === value) || null
}

/** 当前头像值 */
const avatarValue = computed(() => userInfo.value.avatar || '')
/** 姓名首字（无头像时的兜底展示） */
const avatarInitial = computed(() => String(userStore.realName || userStore.username || 'U').slice(0, 1))
/** 当前头像是否为图片地址（非内置渐变头像） */
const avatarIsImage = computed(() => !!avatarValue.value && !findPreset(avatarValue.value))
/** 横幅头像样式：内置头像用 CSS 渐变，图片地址由 img 承载 */
const avatarStyle = computed(() => {
  const hit = findPreset(avatarValue.value)
  if (hit) return { background: hit.css }
  if (avatarIsImage.value) return { background: '#ffffff' }
  return { background: 'var(--es-brand-gradient)' }
})

/** 横幅关键信息行（后端未返回的字段不展示，避免编造数据） */
const bannerInfos = computed(() => {
  const info = userInfo.value
  const items = [
    {
      label: '手机号',
      value: info.phone ? (phoneVisible.value ? info.phone : maskPhone(info.phone)) : '未绑定',
      icon: 'Iphone',
      action: !!info.phone,
      tip: info.phone ? (phoneVisible.value ? '点击隐藏手机号' : '点击查看完整手机号') : '当前账号未绑定手机号'
    },
    { label: '性别', value: GENDER_NAME[info.gender] || '未设置', icon: 'User', tip: '' },
    { label: '所属驿站', value: info.stationName || '未绑定驿站', icon: 'OfficeBuilding', tip: '' },
    { label: '用户 ID', value: info.id ? `#${info.id}` : '-', icon: 'Postcard', tip: '' }
  ]
  // lastLoginTime / createTime 目前不在 /auth/me 返回字段中，有值才展示
  if (info.lastLoginTime) {
    items.push({ label: '最后登录时间', value: formatDateTime(info.lastLoginTime), icon: 'Clock', tip: '' })
  }
  if (info.createTime) {
    items.push({ label: '账号创建时间', value: formatDateTime(info.createTime), icon: 'Calendar', tip: '' })
  }
  return items
})

/** 切换手机号明文/脱敏 */
function togglePhoneVisible() {
  phoneVisible.value = !phoneVisible.value
}

/* ==================================================================
 * 二、数据统计小卡
 * ================================================================ */
/** 待催取逾期件数量 */
const overdueCount = ref(0)
/** 普通用户本人快件总数 */
const parcelTotal = ref(0)

/** 统计卡（仅展示真实可取到的数据） */
const statCards = computed(() => {
  const cards = [
    {
      label: '我的角色',
      value: roleList.value.length,
      unit: '个',
      icon: 'UserFilled',
      preset: 'blue',
      subText: roleText.value
    },
    {
      label: '我的权限',
      value: (userStore.permissions || []).length,
      unit: '项',
      icon: 'Key',
      preset: 'purple',
      subText: '后端为本账号分配的权限标识数'
    }
  ]
  if (isStaffLike.value) {
    cards.push({
      label: '待催取逾期件',
      value: overdueCount.value,
      unit: '件',
      icon: 'AlarmClock',
      preset: overdueCount.value > 0 ? 'orange' : 'teal',
      subText: userStore.isAdmin ? '全站范围（管理员视角）' : '仅统计本驿站'
    })
  } else {
    cards.push({
      label: '我的快件',
      value: parcelTotal.value,
      unit: '件',
      icon: 'Box',
      preset: 'teal',
      subText: '手机号名下快件总数'
    })
  }
  cards.push({
    label: '账号状态',
    value: '正常',
    icon: 'CircleCheck',
    preset: 'teal',
    subText: '当前登录令牌校验通过，账号可用'
  })
  return cards
})

/** 拉取逾期件数量（员工本驿站 / 管理员全局，由后端按角色自动限定范围） */
async function loadOverdueCount() {
  if (!isStaffLike.value || !canViewParcel.value) return
  try {
    overdueCount.value = Number(await getOverdueCount()) || 0
  } catch (e) {
    overdueCount.value = 0
  }
}

/* ==================================================================
 * 三、基本资料
 * ================================================================ */
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

/** 横幅刷新：同时刷新用户信息、统计卡与最近动态 */
async function refreshAll() {
  await loadUserInfo(true)
  loadOverdueCount()
  loadActivities()
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

/* ==================================================================
 * 四、头像设置（无文件上传接口：图片链接 + 内置渐变头像）
 * ================================================================ */
const avatarDialogVisible = ref(false)
const avatarTab = ref('preset')
const avatarDraft = ref('')
const savingAvatar = ref(false)

/** 预览区是否为图片地址 */
const draftIsImage = computed(() => !!avatarDraft.value && !findPreset(avatarDraft.value))
/** 预览区样式 */
const draftStyle = computed(() => {
  const hit = findPreset(avatarDraft.value)
  if (hit) return { background: hit.css }
  if (draftIsImage.value) return { background: '#ffffff' }
  return { background: 'var(--es-brand-gradient)' }
})

/** 打开头像设置对话框 */
function openAvatarDialog() {
  const current = avatarValue.value
  avatarDraft.value = current
  // 未设置头像或已是内置头像时，默认停在「内置渐变头像」页签
  avatarTab.value = !current || findPreset(current) ? 'preset' : 'url'
  avatarDialogVisible.value = true
}

/** 保存头像：空字符串表示恢复为姓名首字默认样式 */
async function saveAvatar() {
  const value = (avatarDraft.value || '').trim()
  if (value && !findPreset(value)) {
    if (!/^(https?:)?\/\//i.test(value) && !value.startsWith('data:image/')) {
      ElMessage.warning('请填写以 http(s):// 开头的图片地址，或选择内置渐变头像')
      return
    }
    if (value.length > 255) {
      ElMessage.warning('头像地址过长（后端字段上限 255 字符），请更换更短的图片链接')
      return
    }
  }

  savingAvatar.value = true
  try {
    // 后端 updateById 只更新非 null 字段，这里单独保存头像不会影响其它资料
    await updateProfile({ avatar: value })
    ElMessage.success(value ? '头像已更新' : '已恢复为默认头像')
    avatarDialogVisible.value = false
    await loadUserInfo(true)
  } catch (e) {
    // 错误提示已由拦截器统一处理
  } finally {
    savingAvatar.value = false
  }
}

/* ==================================================================
 * 五、修改密码
 * ================================================================ */
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

/* ==================================================================
 * 六、账号安全（纯展示：与后端 jwt.expiration=86400、BCryptPasswordEncoder(10) 一致）
 * ================================================================ */
const securityRows = computed(() => [
  {
    label: '登录令牌类型',
    value: 'JWT（Bearer）',
    icon: 'Key',
    desc: '请求头 Authorization: Bearer <token>'
  },
  {
    label: '令牌有效期',
    value: '24 小时（86400 秒）',
    icon: 'Timer',
    desc: '过期后需重新登录获取新令牌'
  },
  {
    label: '本次登录时间',
    value: formatDateTime(sessionLoginTime.value),
    icon: 'Clock',
    desc: '前端记录的本次进入系统时间'
  },
  {
    label: '密码加密方式',
    value: 'BCrypt 单向哈希（强度 10）',
    icon: 'Lock',
    desc: '数据库中不保存明文密码，校验时比对哈希值'
  },
  {
    label: '当前账号角色',
    value: roleText.value,
    icon: 'UserFilled',
    desc: `共 ${roleList.value.length} 个角色、${(userStore.permissions || []).length} 项权限`
  }
])

/* ==================================================================
 * 七、最近动态（员工/管理员：通知记录；普通用户：我的快件）
 * ================================================================ */
const loadingActivity = ref(false)
const notifyList = ref([])
const parcelList = ref([])

/** 归一化后的时间轴数据 */
const activities = computed(() => {
  if (isStaffLike.value) {
    return notifyList.value.map((item) => ({
      id: `notify-${item.id}`,
      timeText: formatDateTime(item.sendTime),
      title: item.notifyTypeName || NOTIFY_TYPE_NAME[item.notifyType] || '通知记录',
      tagType: dictType(NOTIFY_TYPE, item.notifyType),
      waybillNo: item.waybillNo || '-',
      result: item.sendStatusName || SEND_STATUS_NAME[item.sendStatus] || '未知结果',
      resultType: item.sendStatus === 'FAILED' ? 'danger' : 'success',
      nodeType: item.sendStatus === 'FAILED' ? 'danger' : 'success',
      desc: [
        item.channelName || NOTIFY_CHANNEL_NAME[item.channel] || '',
        item.receiverName ? `收件人 ${item.receiverName}` : '',
        item.pickupCode ? `取件码 ${item.pickupCode}` : '',
        item.failReason || ''
      ]
        .filter(Boolean)
        .join(' · ')
    }))
  }
  return parcelList.value.map((item) => ({
    id: `parcel-${item.id}`,
    timeText: formatDateTime(item.inTime || item.createTime),
    title: item.statusName || PARCEL_STATUS_NAME[item.status] || '快件记录',
    tagType: dictType(PARCEL_STATUS, item.status),
    waybillNo: item.waybillNo || '-',
    result: item.pickupCode ? `取件码 ${item.pickupCode}` : '待分配',
    resultType: item.status === 'IN_STORE' ? 'primary' : 'info',
    nodeType: item.status === 'IN_STORE' ? 'primary' : 'success',
    desc: [
      item.expressCompany || '',
      item.parcelTypeName || '',
      item.shelfCode ? `货位 ${item.shelfCode}` : '',
      Number(item.overdueDayCount) > 0 ? `已逾期 ${item.overdueDayCount} 天` : ''
    ]
      .filter(Boolean)
      .join(' · ')
  }))
})

/** 加载最近动态 */
async function loadActivities() {
  if (!canViewNotify.value && !canViewParcel.value) return
  loadingActivity.value = true
  try {
    if (canViewNotify.value) {
      const data = await pageNotifications({ pageNum: 1, pageSize: 8 })
      notifyList.value = (data && data.list) || []
    } else {
      const data = await pageParcels({ pageNum: 1, pageSize: 5 })
      parcelList.value = (data && data.list) || []
      parcelTotal.value = (data && data.total) || 0
    }
  } catch (e) {
    // 失败静默，界面走 el-empty 兜底
  } finally {
    loadingActivity.value = false
  }
}

/** 跳转通知记录页 */
function goNotifyList() {
  router.push('/notify/list')
}

/** 跳转快件列表页 */
function goParcelList() {
  router.push('/parcel/list')
}

onMounted(() => {
  sessionLoginTime.value = new Date()
  fillProfileForm()
  loadUserInfo(true)
  loadOverdueCount()
  loadActivities()
})
</script>

<style scoped>
/* ==================== 一、个人信息横幅 ==================== */
.pf-banner {
  position: relative;
  overflow: hidden;
  padding: 20px 22px;
  border-radius: var(--es-radius-lg);
  background: var(--es-sidebar-gradient);
  box-shadow: var(--es-shadow);
  color: #fff;
}

/* 柔和光晕装饰 */
.pf-banner__glow {
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
  filter: blur(2px);
}

.pf-banner__glow--blue {
  top: -110px;
  right: -60px;
  width: 320px;
  height: 320px;
  background: radial-gradient(circle, rgba(26, 109, 255, 0.5), transparent 68%);
}

.pf-banner__glow--teal {
  bottom: -140px;
  left: -80px;
  width: 340px;
  height: 340px;
  background: radial-gradient(circle, rgba(22, 211, 200, 0.34), transparent 66%);
}

/* 网格纹理 */
.pf-banner__grid {
  position: absolute;
  inset: 0;
  pointer-events: none;
  opacity: 0.22;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.08) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.08) 1px, transparent 1px);
  background-size: 34px 34px;
  mask-image: linear-gradient(120deg, rgba(0, 0, 0, 0.85), transparent 70%);
  -webkit-mask-image: linear-gradient(120deg, rgba(0, 0, 0, 0.85), transparent 70%);
}

.pf-banner__top,
.pf-banner__bottom {
  position: relative;
  z-index: 1;
}

.pf-banner__identity {
  display: flex;
  align-items: center;
  gap: 18px;
}

/* 头像 */
.pf-avatar {
  position: relative;
  flex-shrink: 0;
  width: 68px;
  height: 68px;
  border-radius: 18px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border: 2px solid rgba(255, 255, 255, 0.28);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.28);
  transition: transform 0.26s var(--es-ease), box-shadow 0.26s var(--es-ease);
}

.pf-avatar:hover {
  transform: translateY(-2px) scale(1.02);
  box-shadow: 0 12px 26px rgba(0, 0, 0, 0.34);
}

.pf-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.pf-avatar__initial {
  font-size: 28px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 1px;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.25);
}

.pf-avatar__mask {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  opacity: 0;
  color: #fff;
  font-size: 11px;
  font-style: normal;
  background: rgba(7, 26, 58, 0.62);
  transition: opacity 0.26s var(--es-ease);
}

.pf-avatar__mask em {
  font-style: normal;
}

.pf-avatar:hover .pf-avatar__mask {
  opacity: 1;
}

.pf-banner__meta {
  min-width: 0;
}

.pf-banner__name-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.pf-banner__name {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 0.5px;
  color: #fff;
}

.pf-banner__online {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 2px 10px;
  border-radius: 20px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.92);
  background: rgba(22, 211, 200, 0.16);
  border: 1px solid rgba(22, 211, 200, 0.34);
}

.pf-banner__dot {
  display: inline-block;
}

.pf-banner__account {
  margin-top: 4px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.68);
  letter-spacing: 0.4px;
}

.pf-banner__roles {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  flex-wrap: wrap;
}

.pf-banner__role {
  border: none !important;
  background: var(--es-brand-gradient) !important;
  color: #fff !important;
}

/* 关键信息行 */
.pf-banner__bottom {
  display: flex;
  align-items: flex-end;
  gap: 16px;
  flex-wrap: wrap;
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.14);
}

.pf-banner__info {
  flex: 1;
  min-width: 260px;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(148px, 1fr));
  gap: 14px 18px;
}

.pf-info {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-radius: var(--es-radius);
  background: rgba(255, 255, 255, 0.07);
  border: 1px solid rgba(255, 255, 255, 0.1);
  transition: background 0.24s var(--es-ease), border-color 0.24s var(--es-ease);
}

.pf-info.is-action {
  cursor: pointer;
}

.pf-info.is-action:hover {
  background: rgba(255, 255, 255, 0.14);
  border-color: rgba(22, 211, 200, 0.42);
}

.pf-info__icon {
  font-size: 16px;
  padding: 7px;
  border-radius: 9px;
  color: #fff;
  background: rgba(255, 255, 255, 0.14);
  flex-shrink: 0;
}

.pf-info__text {
  min-width: 0;
}

.pf-info__label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

.pf-info__value {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-top: 2px;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  word-break: break-all;
}

.pf-info__eye {
  font-size: 13px;
  color: var(--es-cyan-400);
}

.pf-banner__actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 深色横幅上的按钮：玻璃质感 */
.pf-banner__actions :deep(.el-button) {
  --el-button-bg-color: rgba(255, 255, 255, 0.14);
  --el-button-border-color: rgba(255, 255, 255, 0.3);
  --el-button-text-color: #ffffff;
  --el-button-hover-bg-color: rgba(255, 255, 255, 0.26);
  --el-button-hover-border-color: rgba(255, 255, 255, 0.5);
  --el-button-hover-text-color: #ffffff;
  --el-button-active-bg-color: rgba(255, 255, 255, 0.2);
  --el-button-active-border-color: rgba(255, 255, 255, 0.5);
  --el-button-active-text-color: #ffffff;
}

/* ==================== 二、统计卡 ==================== */
.pf-stat-col {
  margin-bottom: 12px;
}

/* ==================== 三、内容卡片 ==================== */
.pf-card {
  margin-bottom: 12px;
}

.pf-card__header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pf-card__title {
  font-size: 15px;
  font-weight: 600;
  color: var(--es-text-1);
}

.pf-card__extra {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 4px;
}

.pf-form-tip {
  width: 100%;
  font-size: 12px;
  color: var(--es-text-3);
  line-height: 1.6;
}

/* 账号安全 */
.pf-security {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 12px;
}

.pf-security__item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 14px;
  border-radius: var(--es-radius);
  background: linear-gradient(135deg, rgba(26, 109, 255, 0.06) 0%, rgba(22, 211, 200, 0.08) 100%);
  border: 1px solid var(--es-border);
  transition: box-shadow 0.24s var(--es-ease), transform 0.24s var(--es-ease);
}

.pf-security__item:hover {
  box-shadow: var(--es-shadow-sm);
  transform: translateX(2px);
}

.pf-security__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  flex-shrink: 0;
  border-radius: 10px;
  color: #fff;
  font-size: 16px;
  background: var(--es-brand-gradient);
  box-shadow: var(--es-shadow-brand);
}

.pf-security__body {
  min-width: 0;
}

.pf-security__label {
  font-size: 12px;
  color: var(--es-text-3);
}

.pf-security__value {
  margin-top: 2px;
  font-size: 14px;
  font-weight: 600;
  color: var(--es-text-1);
  word-break: break-all;
}

.pf-security__desc {
  margin-top: 3px;
  font-size: 12px;
  color: var(--es-text-2);
  line-height: 1.6;
}

.pf-security__note {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-top: 12px;
  padding: 10px 12px;
  border-radius: var(--es-radius);
  font-size: 12px;
  line-height: 1.6;
  color: var(--es-text-2);
  background: rgba(26, 109, 255, 0.06);
}

.pf-security__note .el-icon {
  margin-top: 2px;
  color: var(--es-primary);
  flex-shrink: 0;
}

/* 时间轴 */
.pf-timeline {
  padding-left: 2px;
  margin-bottom: -10px;
}

.pf-timeline__head {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.pf-timeline__waybill {
  font-family: Consolas, Monaco, monospace;
  font-size: 13px;
  color: var(--es-text-1);
  font-weight: 600;
}

.pf-timeline__desc {
  margin-top: 5px;
  font-size: 12px;
  color: var(--es-text-3);
  line-height: 1.6;
}

/* ==================== 四、头像对话框 ==================== */
.pf-avatar-dialog__preview {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px;
  border-radius: var(--es-radius);
  background: var(--es-brand-gradient-soft);
  border: 1px solid var(--es-border);
}

.pf-avatar--preview {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  border: 2px solid #fff;
  cursor: default;
}

.pf-avatar--preview:hover {
  transform: none;
}

.pf-avatar-dialog__intro {
  min-width: 0;
  font-size: 12px;
  line-height: 1.7;
}

.pf-avatar-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(66px, 1fr));
  gap: 12px;
}

.pf-avatar-grid__item {
  position: relative;
  height: 66px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 22px;
  font-weight: 700;
  cursor: pointer;
  border: 2px solid transparent;
  box-shadow: var(--es-shadow-sm);
  transition: transform 0.22s var(--es-ease), box-shadow 0.22s var(--es-ease);
}

.pf-avatar-grid__item:hover {
  transform: translateY(-2px);
  box-shadow: var(--es-shadow);
}

.pf-avatar-grid__item.is-active {
  border-color: var(--es-primary);
  box-shadow: 0 0 0 3px rgba(26, 109, 255, 0.18);
}

.pf-avatar-grid__check {
  position: absolute;
  right: 4px;
  bottom: 4px;
  font-size: 13px;
  padding: 2px;
  border-radius: 50%;
  color: #fff;
  background: rgba(7, 26, 58, 0.55);
}

.pf-avatar-dialog__tip {
  margin-top: 8px;
  font-size: 12px;
  color: var(--es-text-3);
  line-height: 1.6;
}

.pf-avatar-dialog__value {
  margin-top: 6px;
  font-size: 12px;
  word-break: break-all;
}

.pf-avatar-dialog__value code {
  display: inline-block;
  margin-top: 4px;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 11px;
  color: var(--es-text-2);
  background: rgba(9, 30, 66, 0.06);
}

/* ==================== 响应式 ==================== */
@media (max-width: 768px) {
  .pf-banner {
    padding: 16px;
  }

  .pf-banner__identity {
    gap: 14px;
  }

  .pf-avatar {
    width: 58px;
    height: 58px;
    border-radius: 15px;
  }

  .pf-avatar__initial {
    font-size: 24px;
  }

  .pf-banner__name {
    font-size: 18px;
  }

  .pf-banner__actions {
    width: 100%;
  }
}
</style>
