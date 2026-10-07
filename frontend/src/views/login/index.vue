<template>
  <!-- 登录页：左侧品牌展示 + 右侧登录卡片 -->
  <div class="login">
    <!-- ==================== 左半边：品牌展示区 ==================== -->
    <div class="login__brand">
      <div class="login__brand-inner">
        <div class="login__logo">
          <img src="/favicon.svg" alt="系统标识" />
          <span>Express Station</span>
        </div>

        <h1 class="login__title">快件收发管理系统</h1>
        <p class="login__subtitle">面向快递驿站的一体化快件收发管理平台</p>

        <!-- 系统特性介绍 -->
        <ul class="login__features">
          <li v-for="item in features" :key="item.title">
            <el-icon class="login__feature-icon"><component :is="item.icon" /></el-icon>
            <div class="login__feature-text">
              <strong>{{ item.title }}</strong>
              <span>{{ item.desc }}</span>
            </div>
          </li>
        </ul>

        <div class="login__brand-footer">
          <span>毕业设计作品</span>
          <el-divider direction="vertical" />
          <span>Vue 3 + Vite 5 + Element Plus</span>
        </div>
      </div>
    </div>

    <!-- ==================== 右半边：登录表单 ==================== -->
    <div class="login__panel">
      <el-card class="login__card" shadow="always">
        <div class="login__card-header">
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

        <!-- 演示账号：点击一键填充 -->
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
              <span class="login__demo-account">{{ account.username }} / {{ account.password }}</span>
              <span class="login__demo-role">{{ account.role }}</span>
            </div>
          </div>
        </div>

        <div class="login__card-footer">
          <span class="text-muted">还没有账号？</span>
          <el-link type="primary" :underline="false" @click="goRegister">立即注册</el-link>
        </div>
      </el-card>

      <p class="login__copyright">© 2026 快件收发管理系统 · 毕业设计作品</p>
    </div>
  </div>
</template>

<script setup>
/**
 * 登录页
 * 功能：表单校验、回车提交、加载状态、演示账号一键填充、登录后按 redirect 参数回跳
 */
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { InfoFilled, Lock, User } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

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
  { icon: 'Box', title: '入库登记', desc: '录入到件信息，系统自动生成 8 位取件码并分配货位' },
  { icon: 'Finished', title: '取件核销', desc: '支持扫码枪连续作业，自动试算逾期保管费' },
  { icon: 'Van', title: '寄件受理', desc: '寄件单登记、状态流转与运单号回填一站式管理' },
  { icon: 'DataAnalysis', title: '数据统计', desc: '出入库趋势、快递公司分布与驿站业务量排行' }
]

/** 演示账号列表（与后端初始化数据保持一致） */
const demoAccounts = [
  { username: 'admin', password: '123456', role: '系统管理员' },
  { username: 'staff01', password: '123456', role: '驿站员工' },
  { username: 'user01', password: '123456', role: '普通用户' }
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

/** 跳转注册页 */
function goRegister() {
  router.push('/register')
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
 * 页面挂载：若从注册页跳转而来则回填用户名；否则默认填充演示管理员账号，便于快速体验
 */
onMounted(() => {
  const username = route.query.username
  if (username) {
    form.username = String(username)
    return
  }
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

/* ---------------- 左侧品牌区 ---------------- */
.login__brand {
  flex: 1.15;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  color: #fff;
  background: linear-gradient(135deg, #1d4ed8 0%, #2563eb 45%, #0ea5e9 100%);
  position: relative;
  overflow: hidden;
}

/* 背景装饰圆 */
.login__brand::before,
.login__brand::after {
  content: '';
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
}

.login__brand::before {
  width: 380px;
  height: 380px;
  top: -120px;
  right: -120px;
}

.login__brand::after {
  width: 260px;
  height: 260px;
  bottom: -90px;
  left: -70px;
}

.login__brand-inner {
  position: relative;
  z-index: 1;
  max-width: 520px;
}

.login__logo {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 28px;
  font-size: 15px;
  letter-spacing: 2px;
  text-transform: uppercase;
  opacity: 0.9;
}

.login__logo img {
  width: 34px;
  height: 34px;
}

.login__title {
  margin: 0 0 10px;
  font-size: 36px;
  font-weight: 700;
  letter-spacing: 2px;
}

.login__subtitle {
  margin: 0 0 32px;
  font-size: 15px;
  opacity: 0.86;
}

.login__features {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 16px;
}

.login__features li {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.12);
}

.login__feature-icon {
  font-size: 22px;
  margin-top: 2px;
}

.login__feature-text {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.login__feature-text strong {
  font-size: 15px;
}

.login__feature-text span {
  font-size: 13px;
  opacity: 0.85;
}

.login__brand-footer {
  margin-top: 34px;
  font-size: 13px;
  opacity: 0.8;
}

/* ---------------- 右侧登录区 ---------------- */
.login__panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: #f5f7fa;
  overflow-y: auto;
}

.login__card {
  width: 100%;
  max-width: 420px;
  border-radius: 12px;
}

.login__card-header {
  margin-bottom: 18px;
  text-align: center;
}

.login__card-header h2 {
  margin: 0 0 6px;
  font-size: 22px;
  color: #1f2d3d;
}

.login__card-header p {
  margin: 0;
  font-size: 13px;
  color: #909399;
}

.login__submit {
  width: 100%;
  letter-spacing: 4px;
  font-weight: 600;
}

/* 演示账号 */
.login__demo {
  margin-top: 4px;
  padding: 12px;
  border-radius: 8px;
  background: #f5f7fa;
  border: 1px dashed #dcdfe6;
}

.login__demo-title {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 10px;
  font-size: 13px;
  color: #606266;
}

.login__demo-list {
  display: grid;
  gap: 8px;
}

.login__demo-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 7px 10px;
  border-radius: 6px;
  background: #fff;
  border: 1px solid #ebeef5;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.login__demo-item:hover {
  border-color: #2563eb;
  background: #ecf5ff;
  transform: translateX(2px);
}

.login__demo-account {
  font-family: Consolas, Monaco, monospace;
  color: #303133;
}

.login__demo-role {
  color: #909399;
  font-size: 12px;
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
  color: #a8abb2;
}

/* ---------------- 响应式：窄屏隐藏品牌区 ---------------- */
@media (max-width: 900px) {
  .login__brand {
    display: none;
  }

  .login__panel {
    flex: 1;
  }
}
</style>
