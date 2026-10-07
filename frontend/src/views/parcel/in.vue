<template>
  <PageContainer title="收件登记" sub-title="录入到件信息，系统自动生成 8 位取件码并分配货位">
    <template #extra>
      <el-tag type="info" effect="plain">带 <span class="text-danger">*</span> 为必填项</el-tag>
    </template>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" class="in-form">
      <el-row :gutter="20">
        <!-- ---------------- 第一列 ---------------- -->
        <el-col :xs="24" :md="12">
          <el-divider content-position="left">快件信息</el-divider>

          <el-form-item label="运单号" prop="waybillNo">
            <el-input
              ref="waybillInputRef"
              v-model.trim="form.waybillNo"
              placeholder="扫描或输入运单号，需与所选快递公司格式匹配"
              clearable
            />
          </el-form-item>

          <el-form-item label="所属驿站" prop="stationId">
            <el-select v-model="form.stationId" placeholder="请选择驿站" style="width: 100%" @change="handleStationChange">
              <el-option v-for="item in stationList" :key="item.id" :label="item.stationName" :value="item.id" />
            </el-select>
          </el-form-item>

          <el-form-item label="快递公司" prop="expressCompany">
            <el-select v-model="form.expressCompany" placeholder="请选择快递公司" style="width: 100%">
              <el-option v-for="item in EXPRESS_COMPANIES" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>

          <el-form-item label="快件类型" prop="parcelType">
            <el-select v-model="form.parcelType" placeholder="请选择快件类型" style="width: 100%">
              <el-option v-for="item in PARCEL_TYPE" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>

          <el-form-item label="货位" prop="shelfId">
            <el-select
              v-model="form.shelfId"
              placeholder="留空由系统自动分配剩余容量最大的货位"
              clearable
              :loading="shelfLoading"
              style="width: 100%"
            >
              <el-option
                v-for="item in shelfList"
                :key="item.id"
                :label="`${item.shelfCode}（剩余 ${item.freeCount ?? '-'} / 容量 ${item.capacity ?? '-'}）`"
                :value="item.id"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="免费保管天数" prop="overdueDays">
            <el-input-number v-model="form.overdueDays" :min="0" :max="365" style="width: 100%" />
            <div class="form-tip">超过该天数后取件将产生逾期保管费</div>
          </el-form-item>
        </el-col>

        <!-- ---------------- 第二列 ---------------- -->
        <el-col :xs="24" :md="12">
          <el-divider content-position="left">收件人与费用</el-divider>

          <el-form-item label="收件人姓名" prop="receiverName">
            <el-input v-model.trim="form.receiverName" placeholder="请输入收件人姓名" clearable />
          </el-form-item>

          <el-form-item label="收件人手机号" prop="receiverPhone">
            <el-input v-model.trim="form.receiverPhone" placeholder="请输入 11 位手机号" maxlength="11" clearable />
          </el-form-item>

          <el-form-item label="重量（kg）" prop="weight">
            <el-input-number v-model="form.weight" :min="0" :max="1000" :precision="2" :step="0.1" style="width: 100%" />
          </el-form-item>

          <el-form-item label="运费（元）" prop="freight">
            <el-input-number v-model="form.freight" :min="0" :max="99999" :precision="2" style="width: 100%" />
          </el-form-item>

          <el-form-item label="备注" prop="remark">
            <el-input
              v-model="form.remark"
              type="textarea"
              :rows="4"
              maxlength="200"
              show-word-limit
              placeholder="例如：客户要求短信通知 / 易碎品轻拿轻放"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <!-- ---------------- 操作按钮 ---------------- -->
      <el-form-item>
        <el-button type="primary" size="large" :icon="Finished" :loading="submitting" @click="handleSubmit">
          {{ submitting ? '提交中…' : '确认入库' }}
        </el-button>
        <el-button size="large" :icon="RefreshLeft" @click="handleReset">重置表单</el-button>
        <el-button size="large" text type="primary" @click="goPickup">前往取件核销 →</el-button>
      </el-form-item>
    </el-form>

    <el-alert
      class="mt-12"
      type="info"
      :closable="false"
      show-icon
      title="操作提示"
      description="提交成功后系统会弹出大字取件码，请念给客户或抄写在快件外包装上；运单号重复或格式不正确时后端会给出中文提示。"
    />
  </PageContainer>
</template>

<script setup>
/**
 * 收件登记（入库）页
 * 接口：POST /api/parcels/in-store
 * 成功后用 ElMessageBox 弹出大字取件码，便于员工直接告知客户
 */
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Finished, RefreshLeft } from '@element-plus/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import { inStore } from '@/api/parcel'
import { listAvailableShelves } from '@/api/shelf'
import { listStations } from '@/api/station'
import { useUserStore } from '@/stores/user'
import { EXPRESS_COMPANIES, PARCEL_TYPE } from '@/utils/dict'

const router = useRouter()
const userStore = useUserStore()

/** 表单实例与运单号输入框引用 */
const formRef = ref(null)
const waybillInputRef = ref(null)

/** 提交中状态 */
const submitting = ref(false)
/** 货位加载状态 */
const shelfLoading = ref(false)

/** 驿站下拉数据 */
const stationList = ref([])
/** 当前驿站可用货位 */
const shelfList = ref([])

/** 表单默认值工厂：保证重置与初始状态一致 */
function createForm() {
  return {
    waybillNo: '',
    stationId: userStore.stationId || null,
    expressCompany: '',
    parcelType: 'NORMAL',
    receiverName: '',
    receiverPhone: '',
    weight: 1,
    freight: 0,
    shelfId: null,
    overdueDays: 3,
    remark: ''
  }
}

/** 收件登记表单数据 */
const form = reactive(createForm())

/** 手机号自定义校验 */
function validatePhone(rule, value, callback) {
  if (!value) {
    callback(new Error('请输入收件人手机号'))
  } else if (!/^1[3-9]\d{9}$/.test(value)) {
    callback(new Error('手机号格式不正确'))
  } else {
    callback()
  }
}

/** 表单校验规则 */
const rules = {
  waybillNo: [
    { required: true, message: '请输入运单号', trigger: 'blur' },
    { min: 6, max: 32, message: '运单号长度应为 6-32 位', trigger: 'blur' }
  ],
  stationId: [{ required: true, message: '请选择所属驿站', trigger: 'change' }],
  expressCompany: [{ required: true, message: '请选择快递公司', trigger: 'change' }],
  parcelType: [{ required: true, message: '请选择快件类型', trigger: 'change' }],
  receiverName: [{ required: true, message: '请输入收件人姓名', trigger: 'blur' }],
  receiverPhone: [{ required: true, validator: validatePhone, trigger: 'blur' }]
}

/** 加载驿站下拉（默认选中当前用户所属驿站，否则取第一个） */
async function loadStations() {
  try {
    const data = await listStations()
    stationList.value = Array.isArray(data) ? data : []
    if (!form.stationId && stationList.value.length) {
      form.stationId = stationList.value[0].id
    }
    await loadShelves()
  } catch (e) {
    stationList.value = []
  }
}

/** 加载当前驿站可用货位（仅剩余容量 > 0） */
async function loadShelves() {
  shelfList.value = []
  if (!form.stationId) return
  shelfLoading.value = true
  try {
    const data = await listAvailableShelves(form.stationId)
    shelfList.value = Array.isArray(data) ? data : []
  } catch (e) {
    shelfList.value = []
  } finally {
    shelfLoading.value = false
  }
}

/** 切换驿站：重新加载货位并清空已选货位 */
function handleStationChange() {
  form.shelfId = null
  loadShelves()
}

/**
 * 构建大字取件码提示的 HTML
 * @param {Object} parcel 后端返回的新建快件对象
 * @returns {string} HTML 字符串
 */
function buildPickupCodeHtml(parcel) {
  const code = parcel.pickupCode || '--------'
  return `
    <div style="text-align:center;">
      <div style="font-size:13px;color:var(--es-text-3);margin-bottom:6px;">取件码（请告知客户）</div>
      <div style="font-size:40px;font-weight:700;letter-spacing:8px;color:var(--es-primary);font-family:Consolas,Monaco,monospace;line-height:1.3;">
        ${code}
      </div>
      <div style="margin-top:14px;padding-top:12px;border-top:1px dashed #dcdfe6;text-align:left;font-size:13px;color:#606266;line-height:1.9;">
        <div>运单号：<b>${parcel.waybillNo || '-'}</b></div>
        <div>收件人：<b>${parcel.receiverName || '-'}</b>（${parcel.receiverPhone || '-'}）</div>
        <div>快递公司：<b>${parcel.expressCompany || '-'}</b></div>
        <div>存放货位：<b>${parcel.shelfCode || '系统未分配'}</b></div>
        <div>所属驿站：<b>${parcel.stationName || '-'}</b></div>
        <div>免费保管：<b>${parcel.overdueDays ?? 0} 天</b>，超期将产生保管费</div>
      </div>
    </div>
  `
}

/**
 * 提交入库登记
 */
async function handleSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch (e) {
    ElMessage.warning('请先完善标红的必填项')
    return
  }

  submitting.value = true
  try {
    const payload = {
      waybillNo: form.waybillNo,
      stationId: form.stationId,
      expressCompany: form.expressCompany,
      parcelType: form.parcelType,
      receiverName: form.receiverName,
      receiverPhone: form.receiverPhone,
      weight: form.weight,
      freight: form.freight,
      overdueDays: form.overdueDays,
      remark: form.remark
    }
    // 货位可为空：留空由后端自动分配
    if (form.shelfId) {
      payload.shelfId = form.shelfId
    }

    const parcel = await inStore(payload)

    // 弹出大字取件码
    await ElMessageBox.alert(buildPickupCodeHtml(parcel || {}), '入库成功', {
      dangerouslyUseHTMLString: true,
      confirmButtonText: '知道了',
      showClose: false,
      customClass: 'pickup-code-dialog'
    })

    // 连续录入：清空业务字段，保留驿站/快递公司/保管天数等默认值
    const keptStationId = form.stationId
    const kept = {
      stationId: keptStationId,
      expressCompany: form.expressCompany,
      parcelType: form.parcelType,
      overdueDays: form.overdueDays,
      weight: 1,
      freight: 0
    }
    Object.assign(form, createForm(), kept)
    form.stationId = keptStationId
    formRef.value.clearValidate()
    await loadShelves()
    // 运单号输入框重新聚焦，方便扫码枪连续录入
    waybillInputRef.value?.focus()
  } catch (e) {
    // 错误提示已由拦截器统一处理（例如「该运单号已登记，请勿重复入库」）
  } finally {
    submitting.value = false
  }
}

/** 重置表单 */
function handleReset() {
  Object.assign(form, createForm(), { stationId: form.stationId || userStore.stationId || null })
  formRef.value?.clearValidate()
  form.shelfId = null
  loadShelves()
  waybillInputRef.value?.focus()
}

/** 前往取件核销页 */
function goPickup() {
  router.push('/parcel/pickup')
}

onMounted(async () => {
  await loadStations()
  waybillInputRef.value?.focus()
})
</script>

<style scoped>
.in-form {
  max-width: 1200px;
}

.form-tip {
  width: 100%;
  font-size: 12px;
  color: var(--es-text-3);
  line-height: 1.6;
  margin-top: 2px;
}

:deep(.el-divider__text) {
  font-weight: 600;
  color: var(--es-text-1);
}
</style>

<style>
/* 大字取件码弹窗：全局样式，因 ElMessageBox 挂载在 body 上 */
.pickup-code-dialog {
  width: 460px !important;
  border-radius: 10px;
}

.pickup-code-dialog .el-message-box__header {
  padding-bottom: 6px;
}

.pickup-code-dialog .el-message-box__title {
  font-size: 17px;
  font-weight: 700;
}

.pickup-code-dialog .el-message-box__content {
  padding-top: 6px;
}
</style>
