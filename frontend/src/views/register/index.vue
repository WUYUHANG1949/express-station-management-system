<template>
  <!-- 注册页：深蓝渐变底 + 居中白色卡片，注册成功默认分配 USER（普通用户）角色（v1.1 换肤，逻辑不变） -->
  <div class="register">
    <span class="register__glow register__glow--teal" />
    <span class="register__glow register__glow--blue" />
    <span class="register__grid" />

    <div class="register__inner">
      <!-- 品牌行：与登录页风格统一 -->
      <div class="register__brand">
        <span class="register__logo-mark">
          <el-icon :size="22"><Van /></el-icon>
        </span>
        <div class="register__brand-text">
          <strong>快件收发管理系统</strong>
          <span>Express Station Management System</span>
        </div>
      </div>

      <el-card class="register__card" shadow="always">
        <div class="register__header">
          <span class="register__badge">NEW ACCOUNT</span>
          <h2>注册账号</h2>
          <p>注册后默认为「普通用户」角色，可查询本人名下快件</p>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" @keyup.enter="handleRegister">
          <el-form-item label="用户名" prop="username">
            <el-input
              v-model.trim="form.username"
              placeholder="3-20 位字母、数字或下划线"
              clearable
              :prefix-icon="User"
            />
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="6-20 位密码"
              show-password
              :prefix-icon="Lock"
            />
          </el-form-item>

          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input
              v-model="form.confirmPassword"
              type="password"
              placeholder="请再次输入密码"
              show-password
              :prefix-icon="Lock"
            />
          </el-form-item>

          <el-form-item label="真实姓名" prop="realName">
            <el-input
              v-model.trim="form.realName"
              placeholder="请输入真实姓名"
              clearable
              :prefix-icon="UserFilled"
            />
          </el-form-item>

          <el-form-item label="手机号" prop="phone">
            <el-input
              v-model.trim="form.phone"
              placeholder="请输入 11 位手机号"
              maxlength="11"
              clearable
              :prefix-icon="Iphone"
            />
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              class="register__submit"
              :loading="loading"
              :disabled="loading"
              @click="handleRegister"
            >
              {{ loading ? '提交中…' : '注 册' }}
            </el-button>
          </el-form-item>
        </el-form>

        <div class="register__footer">
          <span class="text-muted">已有账号？</span>
          <el-link type="primary" :underline="false" @click="goLogin">返回登录</el-link>
        </div>
      </el-card>

      <p class="register__copyright">© 2026 快件收发管理系统 · 毕业设计作品</p>
      <p class="register__stack">Vue 3 + Element Plus · Spring Boot 3 + MyBatis-Plus</p>
    </div>
  </div>
</template>

<script setup>
/**
 * 注册页
 * 接口：POST /api/auth/register
 * body：{ username, password, confirmPassword, realName, phone }
 * v1.1：仅升级视觉（深蓝渐变背景 + 品牌行 + 白色圆角卡片），校验与提交逻辑未改动
 */
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Iphone, Lock, User, UserFilled, Van } from '@element-plus/icons-vue'
import { register } from '@/api/auth'

const router = useRouter()

/** 表单实例引用 */
const formRef = ref(null)
/** 提交加载状态 */
const loading = ref(false)

/** 注册表单数据 */
const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  realName: '',
  phone: ''
})

/**
 * 确认密码自定义校验器
 * @param {Object} rule 校验规则
 * @param {string} value 当前输入值
 * @param {Function} callback 回调函数
 */
function validateConfirmPassword(rule, value, callback) {
  if (!value) {
    callback(new Error('请再次输入密码'))
  } else if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

/**
 * 手机号自定义校验器：11 位中国大陆手机号
 */
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
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度为 3-20 位', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]+$/, message: '用户名只能包含字母、数字和下划线', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为 6-20 位', trigger: 'blur' }
  ],
  confirmPassword: [{ required: true, validator: validateConfirmPassword, trigger: 'blur' }],
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' },
    { min: 2, max: 20, message: '姓名长度为 2-20 位', trigger: 'blur' }
  ],
  phone: [{ required: true, validator: validatePhone, trigger: 'blur' }]
}

/** 返回登录页 */
function goLogin() {
  router.replace('/login')
}

/** 提交注册 */
async function handleRegister() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch (e) {
    return // 校验不通过，不发起请求
  }

  loading.value = true
  try {
    await register({
      username: form.username,
      password: form.password,
      confirmPassword: form.confirmPassword,
      realName: form.realName,
      phone: form.phone
    })
    ElMessage.success('注册成功，请使用新账号登录')
    // 带上用户名便于登录页回填
    router.replace({ path: '/login', query: { username: form.username } })
  } catch (e) {
    // 错误提示已由拦截器统一处理（例如「该用户名已被注册」）
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  min-height: 100vh;
  padding: 32px 24px;
  overflow: hidden;
  background: var(--es-sidebar-gradient);
}

/* 光晕装饰 */
.register__glow {
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
}

.register__glow--teal {
  top: -160px;
  right: -140px;
  width: 460px;
  height: 460px;
  background: radial-gradient(circle, rgba(22, 211, 200, 0.4), transparent 68%);
}

.register__glow--blue {
  bottom: -190px;
  left: -150px;
  width: 520px;
  height: 520px;
  background: radial-gradient(circle, rgba(26, 109, 255, 0.42), transparent 66%);
}

/* 网格纹理 */
.register__grid {
  position: absolute;
  inset: 0;
  pointer-events: none;
  opacity: 0.26;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.07) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.07) 1px, transparent 1px);
  background-size: 42px 42px;
  mask-image: radial-gradient(circle at 50% 20%, rgba(0, 0, 0, 0.9), transparent 72%);
  -webkit-mask-image: radial-gradient(circle at 50% 20%, rgba(0, 0, 0, 0.9), transparent 72%);
}

.register__inner {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 540px;
}

/* 品牌行 */
.register__brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  color: #fff;
}

.register__logo-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  flex-shrink: 0;
  border-radius: 13px;
  color: #fff;
  background: var(--es-brand-gradient);
  box-shadow: var(--es-shadow-brand);
}

.register__brand-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.register__brand-text strong {
  font-size: 17px;
  font-weight: 700;
  letter-spacing: 1px;
}

.register__brand-text span {
  font-size: 11px;
  letter-spacing: 1.6px;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.62);
}

.register__card {
  width: 100%;
  border-radius: var(--es-radius-lg) !important;
  box-shadow: var(--es-shadow-lg) !important;
}

.register__header {
  margin-bottom: 20px;
  text-align: center;
}

.register__badge {
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

.register__header h2 {
  margin: 0 0 6px;
  font-size: 23px;
  font-weight: 700;
  letter-spacing: 1px;
  color: var(--es-text-1);
}

.register__header p {
  margin: 0;
  font-size: 13px;
  color: var(--es-text-3);
}

.register__submit {
  width: 100%;
  height: 44px;
  letter-spacing: 4px;
  font-weight: 600;
}

.register__footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: 13px;
}

.register__copyright {
  margin: 18px 0 0;
  font-size: 12px;
  text-align: center;
  color: rgba(255, 255, 255, 0.78);
}

.register__stack {
  margin: 4px 0 0;
  font-size: 12px;
  text-align: center;
  letter-spacing: 0.3px;
  color: rgba(255, 255, 255, 0.58);
}

@media (max-width: 560px) {
  .register {
    padding: 20px 14px;
  }

  .register__brand-text span {
    display: none;
  }
}
</style>
