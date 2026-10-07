<template>
  <!-- 注册页：居中卡片，注册成功默认分配 USER（普通用户）角色 -->
  <div class="register">
    <el-card class="register__card" shadow="always">
      <div class="register__header">
        <img class="register__logo" src="/favicon.svg" alt="系统标识" />
        <h2>注册账号</h2>
        <p>注册后默认为「普通用户」角色，可查询本人名下快件</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" @keyup.enter="handleRegister">
        <el-form-item label="用户名" prop="username">
          <el-input v-model.trim="form.username" placeholder="3-20 位字母、数字或下划线" clearable />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="6-20 位密码" show-password />
        </el-form-item>

        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="请再次输入密码" show-password />
        </el-form-item>

        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model.trim="form.realName" placeholder="请输入真实姓名" clearable />
        </el-form-item>

        <el-form-item label="手机号" prop="phone">
          <el-input v-model.trim="form.phone" placeholder="请输入 11 位手机号" maxlength="11" clearable />
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
  </div>
</template>

<script setup>
/**
 * 注册页
 * 接口：POST /api/auth/register
 * body：{ username, password, confirmPassword, realName, phone }
 */
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
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
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  min-height: 100vh;
  padding: 24px;
  background: linear-gradient(135deg, #1d4ed8 0%, #2563eb 45%, #0ea5e9 100%);
}

.register__card {
  width: 100%;
  max-width: 520px;
  border-radius: 12px;
}

.register__header {
  margin-bottom: 20px;
  text-align: center;
}

.register__logo {
  width: 44px;
  height: 44px;
}

.register__header h2 {
  margin: 10px 0 6px;
  font-size: 22px;
  color: #1f2d3d;
}

.register__header p {
  margin: 0;
  font-size: 13px;
  color: #909399;
}

.register__submit {
  width: 100%;
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
  margin-top: 18px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.85);
}
</style>
