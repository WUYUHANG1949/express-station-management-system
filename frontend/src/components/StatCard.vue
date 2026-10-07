<template>
  <!--
    统计指标卡片
    展示单个业务指标：图标 + 标签 + 数值（+ 单位 / 副标题）
  -->
  <el-card class="stat-card" shadow="hover" :body-style="{ padding: '16px' }">
    <div class="stat-card__body">
      <div class="stat-card__icon" :style="{ backgroundColor: bgColor, color: color }">
        <el-icon :size="22"><component :is="icon" /></el-icon>
      </div>
      <div class="stat-card__content">
        <div class="stat-card__label">{{ label }}</div>
        <div class="stat-card__value" :style="{ color: color }">
          {{ displayValue }}
          <span v-if="unit" class="stat-card__unit">{{ unit }}</span>
        </div>
        <div v-if="subText" class="stat-card__sub">{{ subText }}</div>
      </div>
    </div>
  </el-card>
</template>

<script setup>
/**
 * 统计卡片组件
 * 用于首页概览与数据统计页的指标展示
 */
import { computed } from 'vue'

const props = defineProps({
  /** 指标名称 */
  label: { type: String, required: true },
  /** 指标数值 */
  value: { type: [Number, String], default: 0 },
  /** 数值单位，例如「件」 */
  unit: { type: String, default: '' },
  /** 图标组件名（Element Plus 图标名） */
  icon: { type: String, default: 'DataLine' },
  /** 主题色 */
  color: { type: String, default: '#2563eb' },
  /** 图标底色（默认取主题色的浅色版本） */
  bgColor: { type: String, default: '' },
  /** 副标题，例如「已用 15 / 共 320」 */
  subText: { type: String, default: '' }
})

/** 数值兜底：null/undefined/空串统一显示为 0 */
const displayValue = computed(() => {
  if (props.value === null || props.value === undefined || props.value === '') return 0
  return props.value
})
</script>

<style scoped>
.stat-card {
  border-radius: 8px;
  border: none;
}

.stat-card__body {
  display: flex;
  align-items: center;
  gap: 12px;
}

.stat-card__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 46px;
  height: 46px;
  border-radius: 10px;
  flex-shrink: 0;
  background-color: #ecf5ff;
}

.stat-card__content {
  flex: 1;
  min-width: 0;
}

.stat-card__label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.stat-card__unit {
  font-size: 12px;
  font-weight: 400;
  margin-left: 2px;
  color: #909399;
}

.stat-card__sub {
  margin-top: 2px;
  font-size: 12px;
  color: #a8abb2;
}
</style>
