<template>
  <!-- 登录页：左侧品牌展示 + 右侧登录卡片（v1.1 换肤，功能与逻辑保持不变） -->
  <div class="login">
    <!-- ==================== 左半边：品牌展示区 ==================== -->
    <div class="login__brand">
      <span class="login__glow login__glow--teal" />
      <span class="login__glow login__glow--blue" />
      <span class="login__grid" />

      <div class="login__brand-inner">
        <div class="login__logo">
          <span class="login__logo-mark">
            <el-icon :size="22"><Van /></el-icon>
          </span>
          <span class="login__logo-text">Express Station</span>
        </div>

        <h1 class="login__title">快件收发管理系统</h1>
        <p class="login__subtitle">面向快递驿站的一体化快件收发管理平台</p>

        <!-- 系统特性介绍 -->
        <ul class="login__features">
          <li v-for="(item, index) in features" :key="item.title">
            <span class="login__feature-index">{{ String(index + 1).padStart(2, '0') }}</span>
            <el-icon class="login__feature-icon"><component :is="item.icon" /></el-icon>
            <div class="login__feature-text">
              <strong>{{ item.title }}</strong>
              <span>{{ item.desc }}</span>
            </div>
          </li>
        </ul>

        <div class="login__brand-footer">
          <span class="login__brand-dot" />
          <span>毕业设计作品</span>
          <el-divider direction="vertical" />
          <span>Vue 3 + Element Plus · Spring Boot 3 + MyBatis-Plus</span>
        </div>
      </div>
    </div>

    <!-- ==================== 右半边：登录表单 ==================== -->
    <div class="login__panel">
      <el-card class="login__card" shadow="always">
        <div class="login__card-header">
          <span class="login__badge">EXPRESS STATION SYSTEM</span>
          <h2>欢迎登录</h2>
          <p>请输入账号信息以进入管理后台</p>
        </div>

        <!-- 登录表单：支持回车提交 -->
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          size="large"
          @keyup.enter="handleLogin"
        >
          <el-form-item label="用户名" prop="username">
            <el-input v-model.trim="form.username" placeholder="请输入用户名" clearable :prefix-icon="User" />
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              show-password
              :prefix-icon="Lock"
            />
          </el-form-item>

          <!-- 忘记密码：两步弹窗（账号名 + 手机号双要素校验 → 验证码重置） -->
          <div class="login__assist">
            <el-link type="primary" :underline="false" @click="forgotVisible = true">
              忘记密码？
            </el-link>
          </div>

          <el-form-item>
            <el-button
              type="primary"
              class="login__submit"
              :loading="loading"
              :disabled="loading"
              @click="handleLogin"
            >
              {{ loading ? '登录中…' : '登 录' }}
            </el-button>
          </el-form-item>
        </el-form>

        <!-- 演示账号：点击一键填充（只列管理员与员工，收件人走免登录查询） -->
        <div class="login__demo">
          <div class="login__demo-title">
            <el-icon><InfoFilled /></el-icon>
            <span>演示账号（点击自动填充）</span>
          </div>
          <div class="login__demo-list">
            <div
              v-for="account in demoAccounts"
              :key="account.username"
              class="login__demo-item"
              @click="fillAccount(account)"
            >
              <span class="login__demo-role">{{ account.role }}</span>
              <span class="login__demo-account">{{ account.username }} / {{ account.password }}</span>
              <span class="login__demo-fill">
                填充
                <el-icon><Right /></el-icon>
              </span>
            </div>
          </div>
        </div>

        <!--
          收件人自助查询入口。
          说明：收件人不需要注册账号（自助注册已下线），因此这里不给"立即注册"链接，
          只提供免登录查询入口；系统内账号由管理员在「用户管理」中创建。
        -->
        <div class="login__card-footer">
          <span class="text-muted">收件人查件？</span>
          <el-link type="primary" :underline="false" @click="goQuery">免登录自助查询 →</el-link>
        </div>
      </el-card>

      <p class="login__copyright">© 2026 快件收发管理系统 · 毕业设计作品</p>
      <p class="login__stack">Vue 3 + Element Plus · Spring Boot 3 + MyBatis-Plus</p>
    </div>

    <!--
      找回密码弹窗（两步）
      设计取舍：提供自助找回，但必须"账号名 + 该账号绑定的手机号"双要素匹配才发码，
      避免知道用户名就能重置他人密码。验证码 5 分钟有效、一次性使用、错误 5 次作废、
      同号 60 秒内只能申请一次。
      发送通道由后端 sms.provider 决定（mock / mail / aliyun）：
      默认 mock 会把验证码回传到 demoCode，前端自动填入，便于没有短信网关时完整演示；
      切到 mail 后验证码真实投递到配置的邮箱，此时 demoCode 为 null，需要去邮箱取。
    -->
    <el-dialog
      v-model="forgotVisible"
      title="找回密码"
      width="480px"
      class="forgot-dialog"
      append-to-body
      @closed="resetForgotState"
    >
      <el-steps :active="forgotStep" simple class="forgot__steps">
        <el-step title="验证身份" />
        <el-step title="重置密码" />
      </el-steps>

      <!-- 第一步：账号名 + 手机号 -->
      <el-form
        v-if="forgotStep === 0"
        ref="forgotFormRef"
        :model="forgotForm"
        :rules="forgotRules"
        label-position="top"
        @keyup.enter="handleSendCode"
      >
        <el-form-item label="账号名" prop="username">
          <el-input v-model.trim="forgotForm.username" placeholder="请输入要找回的账号名" clearable />
        </el-form-item>
        <el-form-item label="该账号绑定的手机号" prop="phone">
          <el-input v-model.trim="forgotForm.phone" placeholder="请输入 11 位手机号" maxlength="11" clearable />
        </el-form-item>
        <p class="forgot__hint">
          需同时匹配账号名与手机号才会发送验证码。手机号已变更或账号被禁用，请联系管理员处理。
        </p>
      </el-form>

      <!-- 第二步：验证码 + 新密码 -->
      <el-form
        v-else
        ref="forgotFormRef"
        :model="forgotForm"
        :rules="forgotRules"
        label-position="top"
        @keyup.enter="handleResetPassword"
      >
        <el-alert
          v-if="forgotInfo.message"
          :title="forgotInfo.message"
          type="success"
          :closable="false"
          show-icon
          class="forgot__alert"
        />
        <el-alert
          v-if="forgotInfo.demoCode"
          :title="`演示模式（未接入短信网关）：验证码 ${forgotInfo.demoCode} 已自动填入`"
          type="warning"
          :closable="false"
          show-icon
          class="forgot__alert"
        />
        <el-form-item label="验证码" prop="code">
          <el-input v-model.trim="forgotForm.code" placeholder="6 位数字验证码" maxlength="6" clearable />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="forgotForm.newPassword" type="password" placeholder="6~20 位" show-password />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="forgotForm.confirmPassword" type="password" placeholder="再次输入新密码" show-password />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="forgotVisible = false">取消</el-button>
        <el-button
          v-if="forgotStep === 0"
          type="primary"
          :loading="forgotSending"
          @click="handleSendCode"
        >
          获取验证码
        </el-button>
        <template v-else>
          <el-button @click="forgotStep = 0">上一步</el-button>
          <el-button type="primary" :loading="forgotResetting" @click="handleResetPassword">
            重置密码
          </el-button>
        </template>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * 登录页
 * 功能：表单校验、回车提交、加载状态、演示账号一键填充、登录后按 redirect 参数回跳
 * v1.1：仅升级视觉（深蓝渐变品牌区 + 白色圆角登录卡 + 胶囊式演示账号），逻辑未改动
 */
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { InfoFilled, Lock, Right, User, Van } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { sendForgotCode, resetForgotPassword } from '@/api/auth'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

/** 表单实例引用 */
const formRef = ref(null)
/** 登录请求加载状态 */
const loading = ref(false)

/** 登录表单数据 */
const form = reactive({
  username: '',
  password: ''
})

/** 表单校验规则 */
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度为 3-20 位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为 6-20 位', trigger: 'blur' }
  ]
}

/** 左侧品牌区展示的系统特性（图标均为 Element Plus 已注册的图标名） */
const features = [
  { icon: 'Box', title: '收件登记', desc: '录入到件信息，系统自动生成 8 位取件码并分配货位' },
  { icon: 'Finished', title: '取件核销', desc: '支持取件码与手机号核验，自动试算逾期保管费' },
  { icon: 'Van', title: '寄件受理', desc: '寄件登记、状态流转与运单号回填一站式管理' },
  { icon: 'DataAnalysis', title: '数据统计', desc: '出入库趋势、快递公司分布与驿站业务量排行' }
]

/**
 * 演示账号列表（与后端初始化数据保持一致）
 *
 * 说明：**不列普通用户**。收件人不需要登录，查件走免登录自助查询（/query），
 * 在登录页摆一个"普通用户"账号会与这一设计自相矛盾。
 * 数据初始化中仍保留 user01 / user02 两个普通用户账号（管理员可在「用户管理」中看到），
 * 用于需要演示三级数据权限的场合，只是不再作为登录页的推荐入口。
 */
const demoAccounts = [
  { username: 'admin', password: '123456', role: '系统管理员' },
  { username: 'staff01', password: '123456', role: '驿站员工' }
]

/**
 * 点击演示账号：一键填充用户名与密码
 * @param {Object} account 演示账号对象
 */
function fillAccount(account) {
  form.username = account.username
  form.password = account.password
  ElMessage.success(`已填充「${account.role}」账号，点击登录即可`)
}

/** 跳转免登录自助查询页（收件人不用登录） */
function goQuery() {
  router.push('/query')
}

/** 「忘记密码」弹窗是否可见 */
const forgotVisible = ref(false)

/* ==================== 找回密码 ==================== */

/** 当前步骤：0 验证身份 / 1 重置密码 */
const forgotStep = ref(0)
const forgotFormRef = ref(null)
const forgotSending = ref(false)
const forgotResetting = ref(false)

/** 第一步返回的信息（提示文案、打码手机号、mock 演示验证码） */
const forgotInfo = reactive({
  message: '',
  maskedPhone: '',
  provider: '',
  demoCode: ''
})

const forgotForm = reactive({
  username: '',
  phone: '',
  code: '',
  newPassword: '',
  confirmPassword: ''
})

/** 确认密码一致性校验 */
function validateConfirm(rule, value, callback) {
  if (!value) {
    callback(new Error('请再次输入新密码'))
  } else if (value !== forgotForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const forgotRules = {
  username: [{ required: true, message: '请输入账号名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { pattern: /^\d{6}$/, message: '验证码为 6 位数字', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度需在 6~20 位之间', trigger: 'blur' }
  ],
  confirmPassword: [{ required: true, validator: validateConfirm, trigger: 'blur' }]
}

/** 打开弹窗时带出已填的账号名，减少重复输入 */
watch(forgotVisible, (visible) => {
  if (visible) {
    forgotForm.username = form.username || ''
  }
})

/** 关闭后清空状态，避免下次打开残留上一次的验证码 */
function resetForgotState() {
  forgotStep.value = 0
  forgotInfo.message = ''
  forgotInfo.maskedPhone = ''
  forgotInfo.provider = ''
  forgotInfo.demoCode = ''
  forgotForm.code = ''
  forgotForm.newPassword = ''
  forgotForm.confirmPassword = ''
  forgotForm.phone = ''
}

/** 第一步：申请验证码 */
async function handleSendCode() {
  if (!forgotFormRef.value) return
  try {
    await forgotFormRef.value.validateField(['username', 'phone'])
  } catch (e) {
    return
  }
  forgotSending.value = true
  try {
    const data = await sendForgotCode({
      username: forgotForm.username,
      phone: forgotForm.phone
    })
    forgotInfo.message = data.message || '验证码已发送'
    forgotInfo.maskedPhone = data.maskedPhone || ''
    forgotInfo.provider = data.provider || ''
    forgotInfo.demoCode = data.demoCode || ''
    // 演示模式下自动填入验证码，免去"去日志里找码"的麻烦
    if (data.demoCode) {
      forgotForm.code = data.demoCode
    }
    forgotStep.value = 1
    ElMessage.success('验证码已发送')
  } catch (e) {
    // 错误提示已由 request 拦截器统一弹出，这里无需重复提示
  } finally {
    forgotSending.value = false
  }
}

/** 第二步：校验验证码并重置密码 */
async function handleResetPassword() {
  if (!forgotFormRef.value) return
  try {
    await forgotFormRef.value.validate()
  } catch (e) {
    return
  }
  forgotResetting.value = true
  try {
    await resetForgotPassword({
      username: forgotForm.username,
      phone: forgotForm.phone,
      code: forgotForm.code,
      newPassword: forgotForm.newPassword
    })
    ElMessage.success('密码已重置，请用新密码登录')
    // 把账号名带回登录表单，密码清空让用户自己输
    form.username = forgotForm.username
    form.password = ''
    forgotVisible.value = false
  } catch (e) {
    // 验证码错误等提示由拦截器弹出
  } finally {
    forgotResetting.value = false
  }
}

/**
 * 执行登录
 * 校验通过后调用 userStore.login（内部已保存 token 到 localStorage 的 es_token）
 */
async function handleLogin() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch (e) {
    return // 校验不通过，不发起请求
  }

  loading.value = true
  try {
    await userStore.login({ username: form.username, password: form.password })
    ElMessage.success('登录成功，欢迎回来')
    // 支持登录后回跳原访问页面
    const redirect = route.query.redirect
    const target = redirect ? decodeURIComponent(String(redirect)) : '/dashboard'
    router.replace(target)
  } catch (e) {
    // 错误提示已由 axios 响应拦截器统一处理
  } finally {
    loading.value = false
  }
}

/**
 * 页面挂载：默认填充演示管理员账号，便于快速体验。
 * （自助注册已下线，不再有从注册页带回用户名的场景）
 */
onMounted(() => {
  form.username = 'admin'
  form.password = '123456'
})
</script>

<style scoped>
.login {
  display: flex;
  width: 100%;
  height: 100vh;
  overflow: hidden;
}

/* ==================== 左侧品牌区 ==================== */
.login__brand {
  position: relative;
  flex: 1.15;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 44px;
  overflow: hidden;
  color: #fff;
  background: var(--es-sidebar-gradient);
}

/* 青绿 / 品牌蓝光晕 */
.login__glow {
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
}

.login__glow--teal {
  top: -140px;
  right: -120px;
  width: 460px;
  height: 460px;
  background: radial-gradient(circle, rgba(22, 211, 200, 0.42), transparent 68%);
}

.login__glow--blue {
  bottom: -180px;
  left: -140px;
  width: 520px;
  height: 520px;
  background: radial-gradient(circle, rgba(26, 109, 255, 0.42), transparent 66%);
}

/* 网格纹理 */
.login__grid {
  position: absolute;
  inset: 0;
  pointer-events: none;
  opacity: 0.3;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.07) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.07) 1px, transparent 1px);
  background-size: 42px 42px;
  mask-image: radial-gradient(circle at 30% 30%, rgba(0, 0, 0, 0.9), transparent 72%);
  -webkit-mask-image: radial-gradient(circle at 30% 30%, rgba(0, 0, 0, 0.9), transparent 72%);
}

.login__brand-inner {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 520px;
}

.login__logo {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 26px;
}

.login__logo-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 13px;
  color: #fff;
  background: var(--es-brand-gradient);
  box-shadow: var(--es-shadow-brand);
}

.login__logo-text {
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 2.4px;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.86);
}

.login__title {
  margin: 0 0 10px;
  font-size: 38px;
  font-weight: 700;
  letter-spacing: 2px;
  line-height: 1.2;
  color: #fff;
}

.login__subtitle {
  margin: 0 0 30px;
  font-size: 15px;
  line-height: 1.7;
  color: rgba(255, 255, 255, 0.74);
}

.login__features {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 12px;
}

.login__features li {
  position: relative;
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 13px 15px;
  border-radius: var(--es-radius);
  background: rgba(255, 255, 255, 0.07);
  border: 1px solid rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(6px);
  transition: background 0.26s var(--es-ease), transform 0.26s var(--es-ease),
    border-color 0.26s var(--es-ease);
}

.login__features li:hover {
  background: rgba(255, 255, 255, 0.12);
  border-color: rgba(22, 211, 200, 0.42);
  transform: translateX(4px);
}

.login__feature-index {
  font-family: 'DIN Alternate', 'Bahnschrift', Consolas, monospace;
  font-size: 13px;
  font-weight: 700;
  line-height: 1.6;
  color: var(--es-cyan-400);
  opacity: 0.9;
}

.login__feature-icon {
  font-size: 20px;
  margin-top: 2px;
  color: #fff;
  flex-shrink: 0;
}

.login__feature-text {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.login__feature-text strong {
  font-size: 15px;
  letter-spacing: 0.4px;
}

.login__feature-text span {
  font-size: 13px;
  line-height: 1.6;
  color: rgba(255, 255, 255, 0.7);
}

.login__brand-footer {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 30px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.66);
}

.login__brand-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--es-teal-400);
  box-shadow: 0 0 0 4px rgba(22, 211, 200, 0.2);
}

/* ==================== 右侧登录区 ==================== */
.login__panel {
  position: relative;
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 28px 24px;
  overflow-y: auto;
  background-color: var(--es-bg-page);
  background-image:
    radial-gradient(760px 420px at 100% 0%, rgba(26, 109, 255, 0.08), transparent 60%),
    radial-gradient(620px 380px at 0% 100%, rgba(22, 211, 200, 0.1), transparent 58%);
}

.login__card {
  width: 100%;
  max-width: 430px;
  border-radius: var(--es-radius-lg) !important;
  box-shadow: var(--es-shadow-lg) !important;
}

.login__card-header {
  margin-bottom: 20px;
  text-align: center;
}

.login__badge {
  display: inline-block;
  padding: 3px 10px;
  margin-bottom: 10px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 1.4px;
  color: var(--es-primary);
  background: var(--es-brand-gradient-soft);
  border: 1px solid rgba(26, 109, 255, 0.18);
}

.login__card-header h2 {
  margin: 0 0 6px;
  font-size: 23px;
  font-weight: 700;
  letter-spacing: 1px;
  color: var(--es-text-1);
}

.login__card-header p {
  margin: 0;
  font-size: 13px;
  color: var(--es-text-3);
}

.login__submit {
  width: 100%;
  height: 44px;
  letter-spacing: 4px;
  font-weight: 600;
}

/* 演示账号：胶囊标签 */
.login__demo {
  margin-top: 6px;
  padding: 12px;
  border-radius: var(--es-radius);
  background: linear-gradient(180deg, #fbfdff 0%, #f5f9ff 100%);
  border: 1px dashed var(--es-border);
}

.login__demo-title {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 10px;
  font-size: 13px;
  color: var(--es-text-2);
}

.login__demo-list {
  display: grid;
  gap: 8px;
}

.login__demo-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-radius: 999px;
  background: #fff;
  border: 1px solid var(--es-border);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.24s var(--es-ease);
}

.login__demo-item:hover {
  border-color: var(--es-primary);
  background: linear-gradient(135deg, rgba(26, 109, 255, 0.08) 0%, rgba(22, 211, 200, 0.1) 100%);
  transform: translateX(3px);
  box-shadow: var(--es-shadow-sm);
}

.login__demo-role {
  flex-shrink: 0;
  padding: 2px 9px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  color: var(--es-primary);
  background: rgba(26, 109, 255, 0.1);
}

.login__demo-account {
  flex: 1;
  min-width: 0;
  font-family: Consolas, Monaco, monospace;
  font-size: 12.5px;
  color: var(--es-text-2);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.login__demo-fill {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  flex-shrink: 0;
  font-size: 12px;
  color: var(--es-text-3);
  opacity: 0;
  transition: opacity 0.24s var(--es-ease), color 0.24s var(--es-ease);
}

.login__demo-item:hover .login__demo-fill {
  opacity: 1;
  color: var(--es-teal-500);
}

.login__card-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-top: 16px;
  font-size: 13px;
}

.login__copyright {
  margin-top: 18px;
  font-size: 12px;
  color: var(--es-text-3);
}

.login__stack {
  margin: 4px 0 0;
  font-size: 12px;
  letter-spacing: 0.3px;
  color: var(--es-text-3);
  opacity: 0.85;
}

/* ==================== 忘记密码 ==================== */
.login__assist {
  display: flex;
  justify-content: flex-end;
  margin: -6px 0 14px;
  font-size: 13px;
}

.forgot__steps {
  margin-bottom: 18px;
}

.forgot__alert {
  margin-bottom: 14px;
}

.forgot__hint {
  margin: -4px 0 0;
  font-size: 12.5px;
  line-height: 1.7;
  color: var(--es-text-3);
}

/* ==================== 响应式：窄屏隐藏品牌区 ==================== */
@media (max-width: 900px) {
  .login__brand {
    display: none;
  }

  .login__panel {
    flex: 1;
  }
}
</style>
