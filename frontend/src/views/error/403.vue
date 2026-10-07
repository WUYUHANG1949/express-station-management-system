<template>
  <!-- 403 无权限页 -->
  <div class="error-page">
    <div class="error-page__code">403</div>
    <h2 class="error-page__title">抱歉，您没有访问该页面的权限</h2>
    <p class="error-page__desc">该功能需要相应的权限标识，请联系系统管理员为您分配角色权限。</p>
    <div class="error-page__actions">
      <el-button type="primary" @click="goHome">返回首页</el-button>
      <el-button @click="goBack">返回上一页</el-button>
    </div>
  </div>
</template>

<script setup>
/** 403 无权限页面 */
import { useRouter } from 'vue-router'

const router = useRouter()

/** 回到首页概览 */
function goHome() {
  router.replace('/dashboard')
}

/** 返回上一页；无历史记录时回首页 */
function goBack() {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.replace('/dashboard')
  }
}
</script>

<style scoped>
.error-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 70vh;
  padding: 24px;
  text-align: center;
}

.error-page__code {
  font-size: 96px;
  font-weight: 800;
  line-height: 1;
  background: linear-gradient(135deg, #f59e0b, #f97316);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.error-page__title {
  margin: 18px 0 8px;
  font-size: 20px;
  color: #1f2d3d;
}

.error-page__desc {
  margin: 0 0 24px;
  font-size: 14px;
  color: #909399;
}

.error-page__actions {
  display: flex;
  gap: 12px;
}
</style>
