<template>
  <!-- 应用根组件：仅承载路由出口 -->
  <router-view />
</template>

<script setup>
/**
 * 根组件
 * 全局错误捕获：避免未捕获异常导致白屏时不给出任何提示
 */
import { onErrorCaptured } from 'vue'
import { ElMessage } from 'element-plus'

onErrorCaptured((err) => {
  console.error('[全局异常]', err)
  ElMessage.error('页面出现异常，请刷新后重试')
  return false
})
</script>
