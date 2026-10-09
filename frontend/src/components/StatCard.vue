<template>
  <!--
    统计指标卡片
    结构：纯色图标块 + 指标名 + 数值（等宽数字）+ 副标题 + 可选环比标签
    视觉取向：白色卡片 + 细边框 + 纯色图标，不放柔光/光晕等装饰，
    保持企业内部系统的干净观感
  -->
  <div class="sc" :class="`sc--${preset}`">
    <div class="sc__body">
      <div class="sc__icon">
        <el-icon :size="20"><component :is="icon" /></el-icon>
      </div>
      <div class="sc__content">
        <div class="sc__label">
          {{ label }}
          <span v-if="trend" class="sc__trend" :class="trendClass">{{ trend }}</span>
        </div>
        <div class="sc__value">
          {{ displayValue }}
          <span v-if="unit" class="sc__unit">{{ unit }}</span>
        </div>
        <div v-if="subText" class="sc__sub">{{ subText }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * 统计卡片组件
 * 用于首页概览、数据统计页与数据大屏的指标展示
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
  /**
   * 配色预设：blue 主色 / teal 青绿 / orange 警示 / red 危险 / purple 紫 / gray 中性
   * 也可以直接传十六进制色值（此时按自定义色渲染）
   */
  preset: { type: String, default: 'blue' },
  /** 环比文案，例如 '+12.5%'；为空则不显示 */
  trend: { type: String, default: '' },
  /** 环比方向：up 上升 / down 下降 / flat 持平 */
  trendType: { type: String, default: 'flat' },
  /** 副标题，例如「已用 15 / 共 320」 */
  subText: { type: String, default: '' }
})

/** 数值兜底：null/undefined/空串统一显示为 0 */
const displayValue = computed(() => {
  if (props.value === null || props.value === undefined || props.value === '') return 0
  return props.value
})

/** 环比标签的配色类 */
const trendClass = computed(() => `sc__trend--${props.trendType}`)
</script>

<style scoped>
.sc {
  position: relative;
  padding: 16px 18px;
  border-radius: var(--es-radius);
  background: #fff;
  border: 1px solid var(--es-border);
  transition: border-color 0.22s var(--es-ease), box-shadow 0.22s var(--es-ease);
}

.sc:hover {
  border-color: #cfd8e3;
  box-shadow: var(--es-shadow-sm);
}

.sc__body {
  position: relative;
  display: flex;
  align-items: center;
  gap: 14px;
}

/* 图标块：纯色底 + 轻微圆角，克制的色彩点缀 */
.sc__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 11px;
  flex-shrink: 0;
  color: #fff;
  background: var(--sc-solid);
}

.sc__content {
  flex: 1;
  min-width: 0;
}

.sc__label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--es-text-3);
  margin-bottom: 3px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sc__trend {
  padding: 1px 6px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 600;
  flex-shrink: 0;
}

.sc__trend--up {
  color: #0c9472;
  background: rgba(15, 185, 143, 0.14);
}

.sc__trend--down {
  color: #c9372c;
  background: rgba(240, 68, 56, 0.13);
}

.sc__trend--flat {
  color: var(--es-text-3);
  background: rgba(138, 151, 171, 0.14);
}

.sc__value {
  font-size: 27px;
  font-weight: 700;
  line-height: 1.25;
  color: var(--es-text-1);
  font-variant-numeric: tabular-nums;
  font-family: 'DIN Alternate', 'Bahnschrift', 'Helvetica Neue', sans-serif;
  letter-spacing: -0.5px;
}

.sc__unit {
  font-size: 12px;
  font-weight: 400;
  margin-left: 3px;
  color: var(--es-text-3);
  letter-spacing: 0;
}

.sc__sub {
  margin-top: 3px;
  font-size: 12px;
  color: var(--es-text-3);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ---------------- 配色预设（纯色，不使用渐变与光晕） ---------------- */
.sc--blue {
  color: #1a6dff;
  --sc-solid: #1a6dff;
}

.sc--teal {
  color: #0fb98f;
  --sc-solid: #0fb98f;
}

.sc--orange {
  color: #f59f00;
  --sc-solid: #f59f00;
}

.sc--red {
  color: #f04438;
  --sc-solid: #f04438;
}

.sc--purple {
  color: #7c4dff;
  --sc-solid: #7c4dff;
}

.sc--gray {
  color: #64748b;
  --sc-solid: #64748b;
}
</style>
