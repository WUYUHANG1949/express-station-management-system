<template>
  <!-- 404 页面不存在 -->
  <div class="error-page">
    <div class="error-page__code">404</div>
    <h2 class="error-page__title">页面走丢了</h2>
    <p class="error-page__desc">您访问的页面不存在或已被移除，请检查地址是否正确。</p>
    <div class="error-page__actions">
      <el-button type="primary" @click="goHome">返回首页</el-button>
      <el-button @click="goLogin">重新登录</el-button>
    </div>
  </div>
</template>

<script setup>
/** 404 页面 */
import { useRouter } from 'vue-router'

const router = useRouter()

/** 回到首页概览 */
function goHome() {
  router.replace('/dashboard')
}

/** 回到登录页 */
function goLogin() {
  router.replace('/login')
}
</script>

<style scoped>
.error-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 80vh;
  padding: 24px;
  text-align: center;
}

.error-page__code {
  font-size: 110px;
  font-weight: 800;
  line-height: 1;
  background: var(--es-brand-gradient);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.error-page__title {
  margin: 18px 0 8px;
  font-size: 20px;
  color: var(--es-text-1);
}

.error-page__desc {
  margin: 0 0 24px;
  font-size: 14px;
  color: var(--es-text-3);
}

.error-page__actions {
  display: flex;
  gap: 12px;
}
</style>
