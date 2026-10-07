<template>
  <!--
    通用数据表格组件
    统一封装：loading、空数据占位、斑马纹、边框、自适应高度、分页
    用法：<DataTable :data="list" :loading="loading" :total="total" v-model:page-num="query.pageNum" v-model:page-size="query.pageSize" @change="loadData">...</DataTable>
  -->
  <div class="data-table">
    <el-table
      v-loading="loading"
      :data="data"
      border
      stripe
      highlight-current-row
      :height="height"
      :max-height="maxHeight"
      :size="size"
      @selection-change="(rows) => emit('selection-change', rows)"
    >
      <!-- 默认插槽放置各列 el-table-column -->
      <slot />
      <!-- 空数据兜底 -->
      <template #empty>
        <el-empty :description="emptyText" :image-size="90" />
      </template>
    </el-table>

    <!-- 分页：支持切换每页条数 -->
    <div v-if="showPagination" class="data-table__pagination">
      <el-pagination
        background
        :current-page="pageNum"
        :page-size="pageSize"
        :page-sizes="pageSizes"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="onSizeChange"
        @current-change="onCurrentChange"
      />
    </div>
  </div>
</template>

<script setup>
/**
 * 通用表格组件
 * 通过插槽承载列定义，组件本身只负责表格容器、空数据与分页的统一处理
 */
const props = defineProps({
  /** 表格数据 */
  data: { type: Array, default: () => [] },
  /** 加载状态 */
  loading: { type: Boolean, default: false },
  /** 总条数 */
  total: { type: Number, default: 0 },
  /** 当前页码（配合 v-model:page-num 使用） */
  pageNum: { type: Number, default: 1 },
  /** 每页条数（配合 v-model:page-size 使用） */
  pageSize: { type: Number, default: 10 },
  /** 每页条数可选项 */
  pageSizes: { type: Array, default: () => [10, 20, 50, 100] },
  /** 是否显示分页 */
  showPagination: { type: Boolean, default: true },
  /** 空数据文案 */
  emptyText: { type: String, default: '暂无数据' },
  /** 表格高度 */
  height: { type: [String, Number], default: undefined },
  /** 表格最大高度 */
  maxHeight: { type: [String, Number], default: undefined },
  /** 尺寸 */
  size: { type: String, default: 'default' }
})

const emit = defineEmits(['update:pageNum', 'update:pageSize', 'change', 'selection-change'])

/** 页码变化：同步 v-model 并触发查询 */
function onPageNumChange(val) {
  emit('update:pageNum', val)
  emit('change', { pageNum: val, pageSize: props.pageSize })
}

/** 每页条数变化（Element Plus 2.x 使用 size-change 事件） */
function onSizeChange(size) {
  emit('update:pageSize', size)
  // 切换每页条数后回到第一页，避免出现空页
  emit('update:pageNum', 1)
  emit('change', { pageNum: 1, pageSize: size })
}

/** 页码变化（Element Plus 2.x 使用 current-change 事件） */
function onCurrentChange(page) {
  emit('update:pageNum', page)
  emit('change', { pageNum: page, pageSize: props.pageSize })
}

defineExpose({ onSizeChange, onCurrentChange })
</script>

<style scoped>
.data-table {
  width: 100%;
}

.data-table__pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
