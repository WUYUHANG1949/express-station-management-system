<template>
  <PageContainer title="取件核销" sub-title="扫描运单条码或输入取件码，回车即查，选中后一键核销">
    <template #extra>
      <el-select
        v-model="stationId"
        placeholder="全部驿站"
        clearable
        style="width: 200px"
        @change="handleStationChange"
      >
        <el-option v-for="item in stationList" :key="item.id" :label="item.stationName" :value="item.id" />
      </el-select>
    </template>

    <!-- ==================== 上半部分：扫描查询区 ==================== -->
    <el-card shadow="never" class="scan-card">
      <div class="scan-card__body">
        <el-icon class="scan-card__icon"><Aim /></el-icon>
        <div class="scan-card__input">
          <el-input
            ref="keywordInputRef"
            v-model.trim="keyword"
            size="large"
            placeholder="扫描运单条码 / 输入 8 位取件码 / 输入 11 位手机号，回车查询"
            clearable
            :prefix-icon="Search"
            @keyup.enter="handleQuery"
            @clear="handleClearResult"
          >
            <template #append>
              <el-button :icon="Search" :loading="querying" @click="handleQuery">查询</el-button>
            </template>
          </el-input>
          <div class="scan-card__tip">
            <el-icon><InfoFilled /></el-icon>
            <span>支持扫码枪直接扫描运单号；也可输入取件码或收件人手机号模糊查询（最多返回 20 条）</span>
          </div>
        </div>
        <el-button size="large" :icon="RefreshLeft" @click="handleResetAll">清空</el-button>
      </div>
    </el-card>

    <!-- ==================== 查询结果卡片列表 ==================== -->
    <el-card shadow="never" class="result-card">
      <template #header>
        <div class="result-card__header">
          <span class="result-card__title">
            匹配结果
            <el-tag v-if="resultList.length" size="small" type="primary" effect="plain" class="ml-8">
              {{ resultList.length }} 条
            </el-tag>
          </span>
          <span v-if="resultList.length" class="text-muted">点击卡片选中要核销的快件</span>
        </div>
      </template>

      <div v-loading="querying" class="result-card__body">
        <!-- 有结果：卡片网格 -->
        <div v-if="resultList.length" class="parcel-grid">
          <div
            v-for="item in resultList"
            :key="item.id"
            class="parcel-card"
            :class="{
              'is-active': selectedParcel && selectedParcel.id === item.id,
              'is-disabled': !isPickable(item.status)
            }"
            @click="selectParcel(item)"
          >
            <div class="parcel-card__head">
              <span class="parcel-card__code">{{ item.pickupCode || '无取件码' }}</span>
              <el-tag :type="dictType(PARCEL_STATUS, item.status)" size="small">
                {{ item.statusName || PARCEL_STATUS_NAME[item.status] || item.status }}
              </el-tag>
            </div>
            <div class="parcel-card__row">
              <span class="parcel-card__label">运单号</span>
              <span class="parcel-card__value">{{ item.waybillNo || '-' }}</span>
            </div>
            <div class="parcel-card__row">
              <span class="parcel-card__label">收件人</span>
              <span class="parcel-card__value">{{ item.receiverName || '-' }} / {{ item.receiverPhone || '-' }}</span>
            </div>
            <div class="parcel-card__row">
              <span class="parcel-card__label">快递公司</span>
              <span class="parcel-card__value">
                {{ item.expressCompany || '-' }} · {{ item.parcelTypeName || dictLabel(PARCEL_TYPE, item.parcelType) }}
              </span>
            </div>
            <div class="parcel-card__row">
              <span class="parcel-card__label">存放货位</span>
              <span class="parcel-card__value">{{ item.shelfCode || '未分配' }}</span>
            </div>
            <div class="parcel-card__row">
              <span class="parcel-card__label">入库时间</span>
              <span class="parcel-card__value">{{ formatDateTime(item.inTime || item.createTime) }}</span>
            </div>
            <div class="parcel-card__row">
              <span class="parcel-card__label">保管情况</span>
              <span class="parcel-card__value">
                已存 {{ formatNumber(item.storageDays) }} 天 / 免费 {{ formatNumber(item.overdueDays) }} 天
                <span v-if="Number(item.overdueFee) > 0" class="text-danger">（逾期费 {{ formatMoney(item.overdueFee, true) }}）</span>
              </span>
            </div>
            <div v-if="!isPickable(item.status)" class="parcel-card__mask">
              <el-tag type="danger" effect="dark">该快件当前状态不可取件</el-tag>
            </div>
          </div>
        </div>

        <!-- 已查询但无结果 -->
        <el-empty
          v-else-if="queried"
          description="未查询到匹配的快件，请核对取件码、运单号或手机号"
          :image-size="100"
        />

        <!-- 尚未查询：引导状态 -->
        <el-empty v-else description="等待扫描…请在上方扫描运单条码或输入取件码后回车" :image-size="100" />
      </div>
    </el-card>

    <!-- ==================== 下半部分：核销表单区 ==================== -->
    <el-card shadow="never" class="pickup-card">
      <template #header>
        <div class="result-card__header">
          <span class="result-card__title">取件核销</span>
          <span v-if="selectedParcel" class="text-muted">
            当前选中：{{ selectedParcel.waybillNo }}（取件码 {{ selectedParcel.pickupCode }}）
          </span>
        </div>
      </template>

      <!-- 未选中任何快件 -->
      <el-empty v-if="!selectedParcel" description="请先在上方扫描或查询并选择一条快件" :image-size="90" />

      <div v-else>
        <!-- 费用试算结果 -->
        <el-alert
          :type="Number(feeInfo.overdueFee) > 0 ? 'warning' : 'success'"
          :closable="false"
          show-icon
          class="mb-12"
        >
          <template #title>
            <span v-if="feeLoading">正在试算应缴保管费…</span>
            <span v-else>
              保管天数 {{ formatNumber(feeInfo.storageDays) }} 天，免费保管 {{ formatNumber(feeInfo.overdueDays) }} 天，
              应缴保管费
              <b :class="Number(feeInfo.overdueFee) > 0 ? 'text-danger' : 'text-success'">
                {{ formatMoney(feeInfo.overdueFee, true) }}
              </b>
            </span>
          </template>
        </el-alert>

        <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" @keyup.enter="handlePickup">
          <el-row :gutter="20">
            <el-col :xs="24" :md="12">
              <el-form-item label="取件码" prop="pickupCode">
                <el-input v-model.trim="form.pickupCode" placeholder="系统已自动带入，请与客户核对" />
              </el-form-item>
              <el-form-item label="实际取件人" prop="receiverName">
                <el-input v-model.trim="form.receiverName" placeholder="请输入实际取件人姓名" />
              </el-form-item>
              <el-form-item label="取件人手机号" prop="receiverPhone">
                <el-input v-model.trim="form.receiverPhone" placeholder="请输入 11 位手机号" maxlength="11" />
              </el-form-item>
            </el-col>

            <el-col :xs="24" :md="12">
              <el-form-item label="取件方式" prop="pickupType">
                <el-select v-model="form.pickupType" style="width: 100%">
                  <el-option v-for="item in PICKUP_TYPE" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </el-form-item>
              <el-form-item label="核验方式" prop="verifyType">
                <el-select v-model="form.verifyType" style="width: 100%">
                  <el-option v-for="item in VERIFY_TYPE" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </el-form-item>
              <el-form-item label="保管费（元）" prop="storageFee">
                <el-input-number v-model="form.storageFee" :min="0" :max="99999" :precision="2" style="width: 100%" />
                <div class="form-tip">系统试算 {{ formatMoney(feeInfo.overdueFee, true) }}，可根据实际情况人工调整</div>
              </el-form-item>
            </el-col>

            <el-col :span="24">
              <el-form-item label="备注" prop="remark">
                <el-input
                  v-model="form.remark"
                  type="textarea"
                  :rows="2"
                  maxlength="200"
                  show-word-limit
                  placeholder="例如：本人持身份证自取 / 家属代取"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item>
            <el-button
              type="primary"
              size="large"
              :icon="Finished"
              :loading="submitting"
              :disabled="!isPickable(selectedParcel.status)"
              @click="handlePickup"
            >
              确认核销
            </el-button>
            <el-button size="large" @click="handleCancelSelect">取消选择</el-button>
            <el-tooltip v-if="!isPickable(selectedParcel.status)" content="仅「在库待取」与「派送中」状态的快件可以核销" placement="top">
              <el-tag type="danger" effect="plain" class="ml-8">当前状态不可取件</el-tag>
            </el-tooltip>
          </el-form-item>
        </el-form>
      </div>
    </el-card>
  </PageContainer>
</template>

<script setup>
/**
 * 取件核销页（核心作业页面）
 * 流程：
 *  1. 扫描运单号 / 输入取件码或手机号 → GET /api/parcels/query
 *  2. 结果以卡片列表展示，点击选中一条
 *  3. 选中后调用 GET /api/parcels/{id}/overdue-fee 展示应缴保管费
 *  4. 填写核销表单 → POST /api/parcels/pickup
 *  5. 成功后清空全部状态并重新聚焦搜索框，方便连续作业
 */
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Aim, Finished, InfoFilled, RefreshLeft, Search } from '@element-plus/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import { getOverdueFee, pickupParcel, queryParcels } from '@/api/parcel'
import { listStations } from '@/api/station'
import { useUserStore } from '@/stores/user'
import {
  PARCEL_STATUS,
  PARCEL_STATUS_NAME,
  PARCEL_TYPE,
  PICKUP_TYPE,
  VERIFY_TYPE,
  dictLabel,
  dictType
} from '@/utils/dict'
import { formatDateTime, formatMoney, formatNumber } from '@/utils/format'

const userStore = useUserStore()

/**
 * 可核销的快件状态。
 * 后端 ParcelService.pickup 允许「在库待取」与「派送中」两种状态核销：
 * 派送中的快件由员工送上门、客户当面签收时同样需要走核销流程，
 * 因此前端必须与后端保持同一口径，否则派送中的快件将无法签收。
 */
const PICKABLE_STATUS = ['IN_STORE', 'DELIVERING']

/**
 * 判断某状态的快件当前是否可以核销
 * @param {string} status 快件状态编码
 * @returns {boolean} 可核销返回 true
 */
function isPickable(status) {
  return PICKABLE_STATUS.includes(status)
}

/** 搜索框引用（用于自动聚焦） */
const keywordInputRef = ref(null)
/** 核销表单引用 */
const formRef = ref(null)

/** 搜索关键字 */
const keyword = ref('')
/** 是否已经执行过查询（用于区分「等待扫描」与「无结果」） */
const queried = ref(false)
/** 驿站筛选（默认当前用户所属驿站；管理员可切换） */
const stationId = ref(userStore.stationId || undefined)
/** 驿站下拉数据 */
const stationList = ref([])

/** 查询结果与选中项 */
const resultList = ref([])
const selectedParcel = ref(null)

/** loading 状态 */
const querying = ref(false)
const feeLoading = ref(false)
const submitting = ref(false)

/** 费用试算结果 */
const feeInfo = reactive({
  storageDays: 0,
  overdueDays: 0,
  overdueFee: 0
})

/** 核销表单 */
const form = reactive({
  pickupCode: '',
  receiverName: '',
  receiverPhone: '',
  pickupType: 'SELF',
  verifyType: 'CODE',
  storageFee: 0,
  remark: ''
})

/** 手机号校验 */
function validatePhone(rule, value, callback) {
  if (!value) {
    callback(new Error('请输入取件人手机号'))
  } else if (!/^1[3-9]\d{9}$/.test(value)) {
    callback(new Error('手机号格式不正确'))
  } else {
    callback()
  }
}

/** 核销表单校验规则 */
const rules = {
  pickupCode: [{ required: true, message: '请输入或核对取件码', trigger: 'blur' }],
  receiverName: [{ required: true, message: '请输入实际取件人姓名', trigger: 'blur' }],
  receiverPhone: [{ required: true, validator: validatePhone, trigger: 'blur' }],
  pickupType: [{ required: true, message: '请选择取件方式', trigger: 'change' }],
  verifyType: [{ required: true, message: '请选择核验方式', trigger: 'change' }]
}

/** 加载驿站下拉 */
async function loadStations() {
  try {
    const data = await listStations()
    stationList.value = Array.isArray(data) ? data : []
  } catch (e) {
    stationList.value = []
  }
}

/** 聚焦搜索框，便于扫码枪连续作业 */
function focusKeyword() {
  nextTick(() => {
    keywordInputRef.value?.focus()
  })
}

/** 驿站切换：清空当前查询结果，避免跨驿站误核销 */
function handleStationChange() {
  handleClearResult()
}

/**
 * 执行查询
 */
async function handleQuery() {
  const value = keyword.value
  if (!value) {
    ElMessage.warning('请先扫描运单条码或输入取件码 / 手机号')
    focusKeyword()
    return
  }

  querying.value = true
  queried.value = true
  // 每次重新查询都清空上一次的选中与表单
  selectedParcel.value = null
  resetFee()
  resultList.value = []

  try {
    const params = { keyword: value }
    if (stationId.value) params.stationId = stationId.value
    const data = await queryParcels(params)
    resultList.value = Array.isArray(data) ? data : []

    if (!resultList.value.length) {
      ElMessage.warning('未查询到匹配的快件，请核对取件码或运单号')
      return
    }

    // 只有一条且状态可核销时自动选中，减少一次点击
    const onlyOne = resultList.value.length === 1 ? resultList.value[0] : null
    if (onlyOne && isPickable(onlyOne.status)) {
      await selectParcel(onlyOne)
    } else {
      ElMessage.success(`查询到 ${resultList.value.length} 条匹配记录，请选择要核销的快件`)
    }
  } catch (e) {
    resultList.value = []
  } finally {
    querying.value = false
  }
}

/** 重置费用试算结果 */
function resetFee() {
  feeInfo.storageDays = 0
  feeInfo.overdueDays = 0
  feeInfo.overdueFee = 0
}

/**
 * 选中一条快件：带入表单并试算逾期保管费
 * @param {Object} item 快件对象
 */
async function selectParcel(item) {
  selectedParcel.value = item
  form.pickupCode = item.pickupCode || ''
  form.receiverName = item.receiverName || ''
  form.receiverPhone = item.receiverPhone || ''
  form.pickupType = 'SELF'
  form.verifyType = 'CODE'
  form.storageFee = 0
  form.remark = ''
  formRef.value?.clearValidate()

  resetFee()
  feeLoading.value = true
  try {
    const data = await getOverdueFee(item.id)
    feeInfo.storageDays = Number(data?.storageDays) || 0
    feeInfo.overdueDays = Number(data?.overdueDays) || 0
    feeInfo.overdueFee = Number(data?.overdueFee) || 0
    // 默认把试算金额带入保管费输入框
    form.storageFee = feeInfo.overdueFee
  } catch (e) {
    // 试算失败兜底：使用快件自身字段，金额记 0
    feeInfo.storageDays = Number(item.storageDays) || 0
    feeInfo.overdueDays = Number(item.overdueDays) || 0
    feeInfo.overdueFee = Number(item.overdueFee) || 0
    form.storageFee = feeInfo.overdueFee
    ElMessage.warning('应缴保管费试算失败，请人工核对后填写')
  } finally {
    feeLoading.value = false
  }

  if (!isPickable(item.status)) {
    ElMessage.warning('该快件当前状态不可取件')
  }
}

/** 取消选择：回到待扫描状态 */
function handleCancelSelect() {
  selectedParcel.value = null
  resetFee()
  form.pickupCode = ''
  form.receiverName = ''
  form.receiverPhone = ''
  form.pickupType = 'SELF'
  form.verifyType = 'CODE'
  form.storageFee = 0
  form.remark = ''
  formRef.value?.clearValidate()
  focusKeyword()
}

/** 清空查询结果（保留搜索框内容由调用方决定） */
function handleClearResult() {
  resultList.value = []
  selectedParcel.value = null
  queried.value = false
  resetFee()
}

/** 清空全部：搜索框 + 结果 + 表单 */
function handleResetAll() {
  keyword.value = ''
  handleClearResult()
  formRef.value?.clearValidate()
  focusKeyword()
}

/**
 * 确认核销
 * 成功后清空搜索框与表单，回到待扫描状态，方便连续作业
 */
async function handlePickup() {
  if (!selectedParcel.value) {
    ElMessage.warning('请先选择要核销的快件')
    return
  }
  if (!isPickable(selectedParcel.value.status)) {
    ElMessage.error('该快件当前状态不可取件')
    return
  }
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch (e) {
    ElMessage.warning('请先完善标红的必填项')
    return
  }

  submitting.value = true
  const pickedWaybill = selectedParcel.value.waybillNo
  try {
    await pickupParcel({
      waybillNo: selectedParcel.value.waybillNo,
      pickupCode: form.pickupCode,
      receiverName: form.receiverName,
      receiverPhone: form.receiverPhone,
      pickupType: form.pickupType,
      verifyType: form.verifyType,
      storageFee: form.storageFee,
      remark: form.remark
    })

    ElMessage.success(`运单号 ${pickedWaybill} 取件核销成功，货位已释放`)
    // 回到待扫描状态
    keyword.value = ''
    handleClearResult()
    formRef.value?.clearValidate()
    focusKeyword()
  } catch (e) {
    // 错误提示已由拦截器统一处理（例如「取件码不正确，请核对」）
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  await loadStations()
  // 自动聚焦搜索框，便于扫码枪直接扫入
  focusKeyword()
})
</script>

<style scoped>
/* ---------------- 扫描区 ---------------- */
.scan-card {
  border-radius: 8px;
  border: none;
  border-top: 3px solid #2563eb;
  margin-bottom: 12px;
}

.scan-card__body {
  display: flex;
  align-items: center;
  gap: 16px;
}

.scan-card__icon {
  font-size: 34px;
  color: #2563eb;
  flex-shrink: 0;
}

.scan-card__input {
  flex: 1;
  min-width: 0;
}

.scan-card__tip {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
}

/* ---------------- 结果区 ---------------- */
.result-card,
.pickup-card {
  border-radius: 8px;
  border: none;
  margin-bottom: 12px;
}

.result-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.result-card__title {
  font-size: 15px;
  font-weight: 600;
  color: #1f2d3d;
}

.result-card__body {
  min-height: 140px;
}

.parcel-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 12px;
  max-height: 420px;
  overflow-y: auto;
  padding: 2px;
}

.parcel-card {
  position: relative;
  padding: 12px 14px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  transition: all 0.2s;
}

.parcel-card:hover {
  border-color: #2563eb;
  box-shadow: 0 2px 10px rgba(37, 99, 235, 0.15);
  transform: translateY(-1px);
}

.parcel-card.is-active {
  border-color: #2563eb;
  background: #ecf5ff;
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.25);
}

.parcel-card.is-disabled {
  opacity: 0.72;
}

.parcel-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 8px;
  margin-bottom: 8px;
  border-bottom: 1px dashed #ebeef5;
}

.parcel-card__code {
  font-family: Consolas, Monaco, monospace;
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 3px;
  color: #2563eb;
}

.parcel-card__row {
  display: flex;
  gap: 8px;
  font-size: 13px;
  line-height: 1.9;
}

.parcel-card__label {
  width: 64px;
  flex-shrink: 0;
  color: #909399;
}

.parcel-card__value {
  flex: 1;
  color: #303133;
  word-break: break-all;
}

.parcel-card__mask {
  position: absolute;
  right: 10px;
  bottom: 8px;
}

/* ---------------- 核销表单 ---------------- */
.form-tip {
  width: 100%;
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
}

@media (max-width: 768px) {
  .scan-card__body {
    flex-wrap: wrap;
  }

  .scan-card__icon {
    display: none;
  }
}
</style>
