<template>
  <!--
    通用 ECharts 图表容器
    统一处理：实例创建/销毁、窗口 resize 自适应、keep-alive 激活后 resize、空数据兜底
  -->
  <div class="chart-box" :style="{ height: height }">
    <div v-if="empty" class="chart-box__empty">
      <el-empty :description="emptyText" :image-size="80" />
    </div>
    <div v-else ref="chartRef" class="chart-box__canvas" v-loading="loading" />
  </div>
</template>

<script setup>
/**
 * 图表组件：传入 ECharts option 即可渲染
 * 采用 echarts 按需引入（只注册本项目用到的折线图、饼图、柱状图及相关组件），
 * 相比全量引入可显著减小打包体积。
 */
import { onActivated, onBeforeUnmount, onMounted, ref, shallowRef, watch, nextTick } from 'vue'
import * as echarts from 'echarts/core'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import {
  GridComponent,
  LegendComponent,
  TitleComponent,
  TooltipComponent
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

// 注册本项目使用到的图表类型与组件
echarts.use([
  LineChart,
  PieChart,
  BarChart,
  GridComponent,
  TooltipComponent,
  LegendComponent,
  TitleComponent,
  CanvasRenderer
])

const props = defineProps({
  /** ECharts 配置项 */
  option: { type: Object, default: () => ({}) },
  /** 图表高度，例如 '320px' */
  height: { type: String, default: '320px' },
  /** 加载状态 */
  loading: { type: Boolean, default: false },
  /** 是否为空数据（为 true 时展示空状态而不渲染图表） */
  empty: { type: Boolean, default: false },
  /** 空数据文案 */
  emptyText: { type: String, default: '暂无数据' }
})

/** 图表容器 DOM 引用 */
const chartRef = ref(null)
/** ECharts 实例（用 shallowRef 避免被 Vue 深度代理） */
const chart = shallowRef(null)
/** 窗口 resize 监听函数引用，便于卸载时移除 */
let resizeHandler = null

/** 初始化图表实例 */
function initChart() {
  if (!chartRef.value) return
  if (!chart.value) {
    chart.value = echarts.init(chartRef.value)
  }
  if (props.option && Object.keys(props.option).length) {
    chart.value.setOption(props.option, true)
  }
}

/** 销毁图表实例 */
function disposeChart() {
  if (chart.value) {
    chart.value.dispose()
    chart.value = null
  }
}

/** 图表尺寸自适应 */
function resizeChart() {
  if (chart.value) {
    chart.value.resize()
  }
}

/** option 变化时刷新图表 */
watch(
  () => props.option,
  async () => {
    if (props.empty) return
    await nextTick()
    initChart()
  },
  { deep: true }
)

/** empty 由 true 变 false 时需要重新初始化（DOM 之前未渲染） */
watch(
  () => props.empty,
  async (val) => {
    if (!val) {
      await nextTick()
      initChart()
      resizeChart()
    } else {
      disposeChart()
    }
  }
)

onMounted(() => {
  if (!props.empty) initChart()
  resizeHandler = () => resizeChart()
  window.addEventListener('resize', resizeHandler)
})

/** keep-alive 缓存页面被重新激活时，容器尺寸可能变化，需 resize */
onActivated(() => {
  nextTick(() => resizeChart())
})

onBeforeUnmount(() => {
  if (resizeHandler) {
    window.removeEventListener('resize', resizeHandler)
    resizeHandler = null
  }
  disposeChart()
})

defineExpose({ resizeChart, initChart })
</script>

<style scoped>
.chart-box {
  width: 100%;
  position: relative;
}

.chart-box__canvas {
  width: 100%;
  height: 100%;
}

.chart-box__empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}
</style>
