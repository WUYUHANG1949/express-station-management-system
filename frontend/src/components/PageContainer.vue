<template>
  <!--
    页面容器组件
    统一页面标题、说明文字与白色卡片内边距，保证全站视觉一致
  -->
  <div class="page-container">
    <div v-if="title" class="page-container__header">
      <div class="page-container__title-wrap">
        <span class="page-container__bar" />
        <h2 class="page-container__title">{{ title }}</h2>
        <el-tag v-if="tag" size="small" type="info" effect="plain">{{ tag }}</el-tag>
        <span v-if="subTitle" class="page-container__subtitle">{{ subTitle }}</span>
      </div>
      <div class="page-container__extra">
        <slot name="extra" />
      </div>
    </div>
    <div class="page-container__body">
      <slot />
    </div>
  </div>
</template>

<script setup>
/** 页面容器：标题 + 右上角操作区 + 内容插槽 */
defineProps({
  /** 页面标题 */
  title: { type: String, default: '' },
  /** 标题右侧的补充说明 */
  subTitle: { type: String, default: '' },
  /** 标题右侧的小标签 */
  tag: { type: String, default: '' }
})
</script>

<style scoped>
.page-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.page-container__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.page-container__title-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}

.page-container__bar {
  display: inline-block;
  width: 4px;
  height: 18px;
  border-radius: 2px;
  background: linear-gradient(180deg, #2563eb, #60a5fa);
}

.page-container__title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #1f2d3d;
}

.page-container__subtitle {
  font-size: 13px;
  color: #909399;
}

.page-container__extra {
  display: flex;
  align-items: center;
  gap: 8px;
}

.page-container__body {
  background-color: #fff;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 1px 6px rgba(0, 21, 41, 0.06);
}
</style>
