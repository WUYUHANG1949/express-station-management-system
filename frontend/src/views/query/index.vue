<template>
  <!--
    收件人自助查询取件码（免登录公开页面）
    风格：独立的对外小工具，深蓝渐变背景 + 青绿光晕，内容垂直居中偏上
    注意：本页不读取登录态，直接调用公开接口 /api/public/pickup-query
  -->
  <div class="pq">
    <!-- 背景光晕装饰 -->
    <span class="pq__glow pq__glow--a" />
    <span class="pq__glow pq__glow--b" />

    <div class="pq__inner">
      <!-- ==================== 品牌区 ==================== -->
      <header class="pq__brand">
        <div class="pq__logo">
          <el-icon :size="24"><Box /></el-icon>
        </div>
        <div class="pq__brand-text">
          <h1 class="pq__title">快件收发管理系统</h1>
          <p class="pq__subtitle">输入手机号，查询您的取件码</p>
        </div>
      </header>

      <!-- ==================== 查询卡片 ==================== -->
      <section class="pq__card">
        <div class="pq__search">
          <el-input
            v-model="phone"
            size="large"
            maxlength="11"
            placeholder="请输入收件人手机号（11 位）"
            clearable
            @input="handlePhoneInput"
            @keyup.enter="handleQuery"
          >
            <template #prefix>
              <el-icon><Iphone /></el-icon>
            </template>
          </el-input>
          <el-button type="primary" size="large" :loading="loading" @click="handleQuery">查询取件码</el-button>
        </div>

        <!-- 加载中：骨架屏 -->
        <el-skeleton v-if="loading" class="pq__skeleton" :rows="3" animated />

        <!-- 查询前：使用说明 -->
        <div v-else-if="!searched" class="pq__intro">
          <el-icon><InfoFilled /></el-icon>
          <p>仅展示仍在驿站待取的快件；如需查看历史记录请联系驿站</p>
        </div>

        <!-- 无结果 -->
        <el-empty
          v-else-if="!list.length"
          description="该手机号下暂无待取快件，请核对号码或联系驿站"
          :image-size="90"
        />

        <!-- 有结果 -->
        <div v-else class="pq__results">
          <div class="pq__results-head">
            <span class="es-live-dot" />
            <span>共查询到 {{ list.length }} 件待取快件，点击卡片可复制取件码</span>
          </div>

          <article
            v-for="(item, index) in list"
            :key="item.id || item.waybillNo || index"
            class="pq-item"
            @click="copyCode(item.pickupCode)"
          >
            <div class="pq-item__code">
              <span class="pq-item__code-label">取件码</span>
              <span class="pq-item__code-value es-gradient-text">{{ item.pickupCode || '-' }}</span>
              <el-icon class="pq-item__copy"><CopyDocument /></el-icon>
            </div>

            <div class="pq-item__rows">
              <div class="pq-item__row">
                <span class="pq-item__k">运单号</span>
                <span class="pq-item__v pq-item__v--mono">{{ item.waybillNo || '-' }}</span>
              </div>
              <div class="pq-item__row">
                <span class="pq-item__k">快递公司</span>
                <span class="pq-item__v">
                  {{ item.expressCompany || '-' }}
                  <span class="text-muted">·</span>
                  {{ item.parcelTypeName || dictLabel(PARCEL_TYPE, item.parcelType) }}
                </span>
              </div>
              <div class="pq-item__row">
                <span class="pq-item__k">所在库位</span>
                <span class="pq-item__v">{{ item.shelfCode || '待分配' }}</span>
              </div>
              <div class="pq-item__row">
                <span class="pq-item__k">驿站</span>
                <span class="pq-item__v">{{ item.stationName || '-' }}</span>
              </div>
              <div class="pq-item__row">
                <span class="pq-item__k">入库时间</span>
                <span class="pq-item__v">{{ formatDateTime(item.inTime) }}</span>
              </div>
            </div>

            <div class="pq-item__foot">
              <el-tag size="small" effect="plain" :type="dictType(PARCEL_STATUS, item.status)">
                {{ item.statusName || PARCEL_STATUS_NAME[item.status] || '在库' }}
              </el-tag>
              <span v-if="overdueFeeOf(item) > 0" class="text-danger text-bold">
                已产生逾期保管费 {{ formatMoney(overdueFeeOf(item), true) }}
              </span>
              <span v-else class="text-success">
                还可在驿站免费保管 {{ formatNumber(item.overdueDays) }} 天
              </span>
            </div>
          </article>
        </div>
      </section>

      <!-- ==================== 底部 ==================== -->
      <footer class="pq__foot">
        <span>本页为收件人自助查询入口，取件时请向工作人员出示取件码</span>
        <router-link to="/login">返回系统登录</router-link>
      </footer>
    </div>
  </div>
</template>

<script setup>
/**
 * 收件人自助查询取件码（v1.1，免登录）
 * 接口：queryPickupByPhone(phone) → 仍在库待取的快件（后端最多返回 20 条）
 * 交互：输入手机号 → 回车或点查询 → 结果卡片列出取件码 → 点击卡片复制取件码
 * 说明：本页不涉及登录态，不引入 useUserStore，避免未登录访客触发鉴权逻辑
 */
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Box, CopyDocument, InfoFilled, Iphone } from '@element-plus/icons-vue'
import { queryPickupByPhone } from '@/api/public'
import { PARCEL_STATUS, PARCEL_STATUS_NAME, PARCEL_TYPE, dictLabel, dictType } from '@/utils/dict'
import { formatDateTime, formatMoney, formatNumber } from '@/utils/format'

/** 组件名：供 keep-alive / 路由使用 */
defineOptions({ name: 'PickupQuery' })

/** 手机号（只保留数字，最多 11 位） */
const phone = ref('')
/** 查询结果 */
const list = ref([])
/** 是否正在查询 */
const loading = ref(false)
/** 是否已经查询过（用于区分「查询前说明」与「无结果」） */
const searched = ref(false)

/**
 * 输入框过滤：只允许数字，最多 11 位
 * @param {string} value 输入框当前值
 */
function handlePhoneInput(value) {
  const digits = String(value || '').replace(/\D/g, '').slice(0, 11)
  if (digits !== value) phone.value = digits
}

/** 逾期保管费（> 0 表示已逾期） */
function overdueFeeOf(item) {
  return Number(item && item.overdueFee) || 0
}

/** 执行查询 */
async function handleQuery() {
  const value = String(phone.value || '').trim()
  if (!/^1[3-9]\d{9}$/.test(value)) {
    ElMessage.warning('请输入正确的 11 位手机号')
    return
  }

  loading.value = true
  try {
    const data = await queryPickupByPhone(value)
    list.value = Array.isArray(data) ? data : []
    searched.value = true
  } catch (e) {
    // 业务错误（手机号格式等）已由响应拦截器弹提示，这里只做状态兜底
    list.value = []
    searched.value = false
  } finally {
    loading.value = false
  }
}

/**
 * 复制取件码（secure context 用 clipboard API，否则退回 execCommand）
 * @param {string} code 取件码
 */
async function copyCode(code) {
  if (!code) return
  try {
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(String(code))
    } else {
      const input = document.createElement('input')
      input.value = String(code)
      document.body.appendChild(input)
      input.select()
      document.execCommand('copy')
      document.body.removeChild(input)
    }
    ElMessage.success('取件码已复制')
  } catch (e) {
    ElMessage.warning('复制失败，请手动长按选择取件码')
  }
}
</script>

<style scoped>
.pq {
  position: relative;
  min-height: 100vh;
  padding: 46px 16px 32px;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  background: var(--es-sidebar-gradient);
  overflow: hidden;
}

/* ---------------- 背景光晕 ---------------- */
.pq__glow {
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
  filter: blur(6px);
}

.pq__glow--a {
  width: 460px;
  height: 460px;
  top: -160px;
  left: -120px;
  background: radial-gradient(circle, rgba(22, 211, 200, 0.34) 0%, rgba(22, 211, 200, 0) 68%);
}

.pq__glow--b {
  width: 520px;
  height: 520px;
  bottom: -200px;
  right: -160px;
  background: radial-gradient(circle, rgba(26, 109, 255, 0.32) 0%, rgba(26, 109, 255, 0) 68%);
}

.pq__inner {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 640px;
}

/* ---------------- 品牌区 ---------------- */
.pq__brand {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 22px;
  color: #fff;
}

.pq__logo {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 14px;
  color: #fff;
  background: var(--es-brand-gradient);
  box-shadow: 0 8px 20px rgba(22, 211, 200, 0.34);
  flex-shrink: 0;
}

.pq__title {
  margin: 0;
  font-size: 21px;
  font-weight: 700;
  letter-spacing: 1px;
}

.pq__subtitle {
  margin: 4px 0 0;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.78);
}

/* ---------------- 查询卡片 ---------------- */
.pq__card {
  padding: 22px;
  border-radius: var(--es-radius-lg);
  background: rgba(255, 255, 255, 0.98);
  border: 1px solid rgba(255, 255, 255, 0.28);
  box-shadow: 0 20px 44px rgba(4, 16, 38, 0.34);
  backdrop-filter: blur(10px);
}

.pq__search {
  display: flex;
  gap: 10px;
}

.pq__skeleton {
  margin-top: 22px;
}

.pq__intro {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-top: 18px;
  padding: 12px 14px;
  border-radius: var(--es-radius);
  border: 1px dashed var(--es-border);
  background: linear-gradient(135deg, rgba(26, 109, 255, 0.06) 0%, rgba(22, 211, 200, 0.06) 100%);
  color: var(--es-text-2);
  font-size: 13px;
}

.pq__intro p {
  margin: 0;
  line-height: 1.7;
}

/* ---------------- 结果 ---------------- */
.pq__results {
  margin-top: 18px;
}

.pq__results-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-size: 13px;
  color: var(--es-text-2);
}

.pq-item {
  padding: 14px 16px;
  margin-bottom: 12px;
  border: 1px solid var(--es-border);
  border-radius: var(--es-radius);
  background: linear-gradient(180deg, #fbfdff 0%, #ffffff 100%);
  cursor: pointer;
  transition: transform 0.22s var(--es-ease), box-shadow 0.22s var(--es-ease), border-color 0.22s var(--es-ease);
}

.pq-item:last-child {
  margin-bottom: 0;
}

.pq-item:hover {
  transform: translateY(-2px);
  border-color: rgba(26, 109, 255, 0.4);
  box-shadow: var(--es-shadow);
}

.pq-item__code {
  display: flex;
  align-items: baseline;
  gap: 12px;
  padding-bottom: 12px;
  border-bottom: 1px dashed var(--es-border);
}

.pq-item__code-label {
  font-size: 12px;
  color: var(--es-text-3);
  letter-spacing: 2px;
}

.pq-item__code-value {
  flex: 1;
  font-size: 30px;
  letter-spacing: 3px;
  line-height: 1.2;
  font-family: 'Consolas', 'Courier New', monospace;
  word-break: break-all;
}

.pq-item__copy {
  color: var(--es-text-3);
  font-size: 16px;
}

/* 兜底：极少数不支持 background-clip:text 的浏览器下，渐变文字会变成透明，
   此时退回品牌主色，保证取件码始终可见（取件码是本页最关键的信息） */
@supports not ((-webkit-background-clip: text) or (background-clip: text)) {
  .pq-item__code-value {
    color: var(--es-primary) !important;
  }
}

.pq-item__rows {
  margin-top: 10px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 4px 16px;
}

.pq-item__row {
  display: flex;
  gap: 8px;
  font-size: 13px;
  line-height: 1.8;
}

.pq-item__k {
  flex: 0 0 60px;
  color: var(--es-text-3);
}

.pq-item__v {
  flex: 1;
  min-width: 0;
  color: var(--es-text-1);
  word-break: break-all;
}

.pq-item__v--mono {
  font-family: 'Consolas', 'Courier New', monospace;
}

.pq-item__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px dashed var(--es-border);
  font-size: 13px;
}

/* ---------------- 底部 ---------------- */
.pq__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 18px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.66);
}

.pq__foot a {
  color: #7fe9dd;
}

.pq__foot a:hover {
  color: #fff;
}

/* ---------------- 窄屏适配 ---------------- */
@media (max-width: 560px) {
  .pq {
    padding-top: 30px;
  }

  .pq__card {
    padding: 16px;
  }

  .pq__search {
    flex-direction: column;
  }

  .pq-item__rows {
    grid-template-columns: minmax(0, 1fr);
  }

  .pq-item__code-value {
    font-size: 26px;
  }
}
</style>
