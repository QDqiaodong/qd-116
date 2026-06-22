<template>
  <div class="asset-list">
    <div class="toolbar">
      <div class="search-bar">
        <el-input
          v-model="keyword"
          placeholder="工装编号/产品名"
          clearable
          :prefix-icon="Search"
          @clear="fetchList"
          @keyup.enter="fetchList"
          style="width: 220px"
        />
        <el-select v-model="filterStatus" placeholder="状态筛选" clearable style="width: 140px" @change="fetchList">
          <el-option label="在用" value="IN_USE" />
          <el-option label="已移位" value="TRANSFERRED" />
          <el-option label="已报废" value="SCRAPPED" />
        </el-select>
        <el-select v-model="filterWorkstation" placeholder="工位筛选" clearable style="width: 140px" @change="fetchList">
          <el-option v-for="ws in workstationOptions" :key="ws" :label="ws" :value="ws" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="fetchList">查询</el-button>
      </div>
      <el-button type="success" :icon="Plus" @click="openDialog()">新增工装</el-button>
    </div>

    <div class="stats-bar">
      <div class="stat-item stat-in-use">
        <span class="stat-label">在用</span>
        <span class="stat-value">{{ stats.inUse }}</span>
      </div>
      <div class="stat-item stat-transferred">
        <span class="stat-label">已移位</span>
        <span class="stat-value">{{ stats.transferred }}</span>
      </div>
      <div class="stat-item stat-scrapped">
        <span class="stat-label">已报废</span>
        <span class="stat-value">{{ stats.scrapped }}</span>
      </div>
    </div>

    <el-row :gutter="16" class="card-grid">
      <el-col v-for="item in listWithSpec" :key="item.id" :xl="4" :lg="6" :md="8" :sm="12">
        <el-card class="asset-card" shadow="hover">
          <div class="card-img-wrapper">
            <img
              v-if="item.imageUrl && !isImageBroken(item)"
              :src="'/api/file/' + item.imageUrl"
              class="card-img"
              alt=""
              @error="handleImageError(item)"
            />
            <div v-else class="card-img-placeholder">
              <el-icon :size="40"><Picture /></el-icon>
            </div>
            <el-tag
              class="card-status-tag"
              :type="statusTagType(item.status)"
              size="small"
              effect="dark"
            >{{ statusLabel(item.status) }}</el-tag>
          </div>
          <div class="card-body">
            <div class="card-title">{{ item.toolingCode }}</div>
            <div class="spec-summary">
              <div class="spec-row">
                <span class="spec-label">关键尺寸</span>
                <span v-if="item.spec.keyDim" class="spec-value">{{ item.spec.keyDim }}</span>
                <span v-else class="spec-placeholder">未填写</span>
              </div>
              <div class="spec-row">
                <span class="spec-label">定位面</span>
                <span v-if="item.spec.positioningFace" class="spec-value">{{ item.spec.positioningFace }}</span>
                <span v-else class="spec-placeholder">未填写</span>
              </div>
              <div class="spec-row">
                <span class="spec-label">适配产品</span>
                <span class="spec-value">{{ item.spec.product }}</span>
              </div>
            </div>
            <div class="card-info">
              <span class="info-label">存放工位</span>
              <span class="info-value">{{ item.workstation }}</span>
            </div>
            <div class="card-info">
              <span class="info-label">入库日期</span>
              <span class="info-value">{{ item.entryDate }}</span>
            </div>
          </div>
          <div class="card-actions">
            <el-button size="small" :icon="Edit" @click="openDialog(item)">编辑</el-button>
            <el-button size="small" type="primary" :icon="Clock" @click="viewTrace(item)">轨迹</el-button>
            <el-button size="small" type="warning" :icon="Switch" @click="handleTransfer(item)" :disabled="item.status === 'SCRAPPED'">移位</el-button>
            <el-button size="small" type="danger" :icon="CircleClose" @click="handleScrap(item)" :disabled="item.status === 'SCRAPPED'">报废</el-button>
            <el-button size="small" type="info" :icon="Delete" @click="handleDelete(item)"></el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-empty v-if="!list.length && !loading" description="暂无工装数据" />

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑工装' : '新增工装'"
      width="600px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="工装编号" prop="toolingCode">
          <div style="display:flex;gap:8px;width:100%;align-items:flex-start;">
            <el-input
              v-model="form.toolingCode"
              placeholder="请输入工装编号，格式：TL-YYYY-XXX"
              style="flex:1"
              @blur="handleCodeBlur"
              @input="handleCodeInput"
            />
            <el-button type="primary" plain :icon="MagicStick" @click="generateNextCode">自动生成</el-button>
          </div>
          <div v-if="codeValidation.message" class="code-validation-tip" :class="{ valid: codeValidation.valid, invalid: !codeValidation.valid }">
            <el-icon v-if="!codeValidation.valid" class="tip-icon"><Warning /></el-icon>
            <el-icon v-else class="tip-icon"><CircleCheck /></el-icon>
            <span class="tip-text">{{ codeValidation.message }}</span>
            <el-link
              v-if="codeValidation.suggestedCode && !isEdit"
              type="primary"
              :underline="false"
              style="margin-left:8px;"
              @click="applySuggestedCode"
            >使用推荐编号</el-link>
          </div>
          <div class="code-format-hint">
            <el-icon><InfoFilled /></el-icon>
            <span>定位块编号规则：TL-年份-3位流水号（如 TL-{{ currentYear }}-001）</span>
          </div>
        </el-form-item>
        <el-form-item label="适配产品" prop="productName">
          <el-input v-model="form.productName" placeholder="请输入适配产品" />
        </el-form-item>
        <el-form-item label="存放工位" prop="workstation">
          <el-select v-model="form.workstation" placeholder="请选择工位" style="width: 100%">
            <el-option v-for="ws in workstationOptions" :key="ws" :label="ws" :value="ws" />
          </el-select>
        </el-form-item>
        <el-form-item label="入库日期" prop="entryDate">
          <el-date-picker
            v-model="form.entryDate"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            :disabled-date="disabledEntryDate"
            style="width: 100%"
          />
          <div class="entry-date-hint">
            <el-icon><InfoFilled /></el-icon>
            <span>入库日期需在系统启用日期（{{ systemConfig.entryDateMin || '加载中' }}）至今天之间，超出范围将被拦截</span>
          </div>
        </el-form-item>
        <el-form-item v-if="isEdit" label="状态" prop="status">
          <el-select v-model="form.status" placeholder="请选择状态" style="width: 100%">
            <el-option label="在用" value="IN_USE" />
            <el-option label="已移位" value="TRANSFERRED" />
            <el-option label="已报废" value="SCRAPPED" />
          </el-select>
        </el-form-item>
        <el-form-item label="图片">
          <el-upload
            v-model:file-list="fileList"
            action="#"
            :auto-upload="false"
            list-type="picture-card"
            accept="image/*"
            :limit="1"
            :before-upload="beforeImageUpload"
            :on-exceed="handleImageExceed"
          >
            <el-icon :size="24"><Plus /></el-icon>
          </el-upload>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
        <el-form-item
          v-if="isEdit && (isStatusChanged || isWorkstationChanged)"
          label="状态变更说明"
          prop="statusChangeRemark"
        >
          <el-input
            v-model="form.statusChangeRemark"
            type="textarea"
            :rows="2"
            placeholder="请说明状态/工位变更的原因，必填"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="transferVisible" title="工装移位" width="520px" destroy-on-close>
      <div v-if="isHighRiskTransfer" class="high-risk-warning">
        <el-alert
          type="warning"
          :closable="false"
          show-icon
          title="⚠️ 高风险移位提醒"
        >
          <template #default>
            <div>该移位属于高风险移位（从重点区域流向敏感区域），需先走审批流程，不能直接移位。</div>
            <div style="margin-top:8px;">
              <el-button size="small" type="warning" @click="openHighRiskApprovalDialog">发起审批申请</el-button>
              <el-button size="small" @click="transferVisible = false">取消移位</el-button>
            </div>
          </template>
        </el-alert>
      </div>
      <el-form ref="transferFormRef" :model="transferForm" :rules="transferRules" label-width="100px" :disabled="isHighRiskTransfer">
        <el-form-item label="工装编号">
          <el-input :model-value="currentItem?.toolingCode" disabled />
        </el-form-item>
        <el-form-item label="当前工位">
          <el-input :model-value="currentItem?.workstation" disabled />
        </el-form-item>
        <el-form-item label="目标工位" prop="toWorkstation">
          <el-select v-model="transferForm.toWorkstation" placeholder="请选择目标工位" style="width: 100%" @change="checkHighRiskOnChange">
            <el-option v-for="ws in workstationOptions" :key="ws" :label="ws" :value="ws" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作人" prop="operator">
          <el-input v-model="transferForm.operator" placeholder="请输入操作人" />
        </el-form-item>
        <el-form-item label="状态变更说明" prop="statusChangeRemark">
          <el-input
            v-model="transferForm.statusChangeRemark"
            type="textarea"
            :rows="2"
            placeholder="请说明移位原因，必填"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="transferForm.remark" type="textarea" :rows="2" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="transferVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" :disabled="isHighRiskTransfer" @click="submitTransfer">确认移位</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="scrapVisible" title="工装报废" width="480px" destroy-on-close>
      <el-form ref="scrapFormRef" :model="scrapForm" :rules="scrapRules" label-width="100px">
        <el-form-item label="工装编号">
          <el-input :model-value="currentItem?.toolingCode" disabled />
        </el-form-item>
        <el-form-item label="报废原因" prop="scrapReason">
          <el-input v-model="scrapForm.scrapReason" type="textarea" :rows="3" placeholder="请输入报废原因" />
        </el-form-item>
        <el-form-item label="报废日期" prop="scrapDate">
          <el-date-picker
            v-model="scrapForm.scrapDate"
            type="date"
            placeholder="选择报废日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="操作人" prop="operator">
          <el-input v-model="scrapForm.operator" placeholder="请输入操作人" />
        </el-form-item>
        <el-form-item label="状态变更说明" prop="statusChangeRemark">
          <el-input
            v-model="scrapForm.statusChangeRemark"
            type="textarea"
            :rows="2"
            placeholder="请说明报废原因的详细情况，必填"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="scrapForm.remark" type="textarea" :rows="2" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="scrapVisible = false">取消</el-button>
        <el-button type="danger" :loading="submitting" @click="submitScrap">确认报废</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="highRiskApprovalVisible" title="高风险移位审批申请" width="560px" destroy-on-close>
      <el-alert
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom:16px;"
        title="请完成以下信息提交审批"
      />
      <el-form ref="highRiskApprovalFormRef" :model="highRiskApprovalForm" :rules="highRiskApprovalRules" label-width="100px">
        <el-form-item label="工装编号">
          <el-input :model-value="currentItem?.toolingCode" disabled />
        </el-form-item>
        <el-form-item label="源工位">
          <el-input :model-value="currentItem?.workstation" disabled />
        </el-form-item>
        <el-form-item label="目标工位">
          <el-input :model-value="transferForm.toWorkstation" disabled />
        </el-form-item>
        <el-form-item label="申请人" prop="applicant">
          <el-input v-model="highRiskApprovalForm.applicant" placeholder="请输入申请人姓名" />
        </el-form-item>
        <el-form-item label="申请原因" prop="applyReason">
          <el-input
            v-model="highRiskApprovalForm.applyReason"
            type="textarea"
            :rows="3"
            placeholder="请详细说明高风险移位的原因，必填"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="highRiskApprovalForm.remark"
            type="textarea"
            :rows="2"
            placeholder="请输入补充备注（可选）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="highRiskApprovalVisible = false">取消</el-button>
        <el-button type="warning" :loading="submittingApproval" @click="submitHighRiskApproval">提交审批申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search,
  Plus,
  Edit,
  Delete,
  Switch,
  CircleClose,
  Picture,
  Clock,
  MagicStick,
  Warning,
  CircleCheck,
  InfoFilled,
} from '@element-plus/icons-vue'
import {
  listAssets,
  createAsset,
  updateAsset,
  deleteAsset,
  getStats,
  getSystemConfig,
  uploadFile,
  transferTooling,
  scrapTooling,
  checkDuplicateAsset,
  getNextLocatorBlockCode,
  validateLocatorBlockCode,
  getSpecTemplate,
  listSpecCategories,
  listWorkstationNames,
  checkHighRiskTransfer,
  applyHighRiskTransfer,
  listPendingHighRiskTransferApprovals,
} from '../api/tooling'
import { batchCompressImages, validateImageFile } from '../utils/compress'
import dayjs from 'dayjs'

const router = useRouter()
const route = useRoute()

const workstationOptions = ref([])

const currentYear = new Date().getFullYear()

const codeValidation = reactive({
  valid: false,
  formatValid: false,
  exists: false,
  message: '',
  suggestedCode: '',
})

let codeValidateTimer = null

const statusLabel = (status) => {
  const map = { IN_USE: '在用', TRANSFERRED: '已移位', SCRAPPED: '已报废' }
  return map[status] || status
}

const statusTagType = (status) => {
  const map = { IN_USE: 'success', TRANSFERRED: 'warning', SCRAPPED: 'danger' }
  return map[status] || 'info'
}

const loading = ref(false)
const list = ref([])
const keyword = ref('')
const filterStatus = ref('')
const filterWorkstation = ref('')
const stats = reactive({ inUse: 0, transferred: 0, scrapped: 0 })
const systemConfig = reactive({ entryDateMin: '', today: '' })
const brokenImageIds = reactive(new Set())

const handleImageError = (item) => {
  brokenImageIds.add(item.id)
}

const isImageBroken = (item) => {
  return brokenImageIds.has(item.id)
}

const fetchList = async () => {
  loading.value = true
  try {
    const params = {}
    if (keyword.value) params.keyword = keyword.value
    if (filterStatus.value) params.status = filterStatus.value
    if (filterWorkstation.value) params.workstation = filterWorkstation.value
    const res = await listAssets(params)
    list.value = res.data || []
    brokenImageIds.clear()
  } catch (e) {
    ElMessage.error(e?.message || '获取列表失败')
  } finally {
    loading.value = false
  }
}

const fetchStats = async () => {
  try {
    const res = await getStats()
    const d = res.data || {}
    const sc = d.statusCount || {}
    stats.inUse = sc.IN_USE || 0
    stats.transferred = sc.TRANSFERRED || 0
    stats.scrapped = sc.SCRAPPED || 0
  } catch {
    /* ignore */
  }
}

const fetchSystemConfig = async () => {
  try {
    const res = await getSystemConfig()
    const d = res.data || {}
    systemConfig.entryDateMin = d.entryDateMin || ''
    systemConfig.today = d.today || ''
  } catch {
    /* ignore */
  }
}

const disabledEntryDate = (date) => {
  if (!date) return false
  const d = dayjs(date)
  if (d.isAfter(dayjs(), 'day')) return true
  if (systemConfig.entryDateMin && d.isBefore(dayjs(systemConfig.entryDateMin), 'day')) return true
  return false
}

const validateEntryDate = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请选择入库日期'))
    return
  }
  const d = dayjs(value)
  if (d.isAfter(dayjs(), 'day')) {
    callback(new Error('入库日期不能晚于今天，请重新选择'))
    return
  }
  if (systemConfig.entryDateMin && d.isBefore(dayjs(systemConfig.entryDateMin), 'day')) {
    callback(new Error(`入库日期不能早于系统启用日期（${systemConfig.entryDateMin}），请重新选择`))
    return
  }
  callback()
}

const specMapByCategory = ref({})

const SPEC_FIELD_ALIASES = {
  keyDim: ['关键尺寸', 'keyDimension', 'key_dimension', '关键尺寸(mm)', '尺寸'],
  positioningFace: ['定位面', 'positioningFace', '定位面信息', '定位'],
  product: ['适配产品', 'productName', '适配产品型号', '产品'],
}

const pickSpecField = (tmpl, aliases) => {
  if (!tmpl) return ''
  for (const k of aliases) {
    const v = tmpl[k]
    if (v !== undefined && v !== null && String(v).trim() !== '') return String(v)
  }
  return ''
}

const matchSpecCategory = (productName) => {
  if (!productName) return null
  const map = specMapByCategory.value
  if (map[productName]) return productName
  let best = null
  Object.keys(map).forEach((cat) => {
    if (productName.startsWith(cat) && cat.length > (best ? best.length : 0)) best = cat
  })
  return best
}

const buildSpec = (item) => {
  const cat = matchSpecCategory(item.productName)
  const tmpl = cat ? specMapByCategory.value[cat] : null
  return {
    keyDim: pickSpecField(tmpl, SPEC_FIELD_ALIASES.keyDim),
    positioningFace: pickSpecField(tmpl, SPEC_FIELD_ALIASES.positioningFace),
    product: pickSpecField(tmpl, SPEC_FIELD_ALIASES.product) || item.productName || '',
  }
}

const listWithSpec = computed(() =>
  list.value.map((item) => ({ ...item, spec: buildSpec(item) }))
)

const fieldsToMap = (fields) => {
  if (!fields || !Array.isArray(fields)) return {}
  const map = {}
  fields.forEach((f) => {
    if (f && f.fieldName) {
      const val = f.defaultValue || f.label || ''
      map[f.fieldName] = val
      if (f.label && f.label !== f.fieldName) {
        map[f.label] = val
      }
    }
  })
  return map
}

const fetchSpecTemplates = async () => {
  try {
    const catRes = await listSpecCategories()
    const cats = catRes.data || []
    const map = {}
    await Promise.all(
      cats.map(async (cat) => {
        const t = await getSpecTemplate(cat)
        map[cat] = fieldsToMap(t.data)
      })
    )
    specMapByCategory.value = map
  } catch {
    /* ignore */
  }
}

const fetchWorkstationOptions = async () => {
  try {
    const res = await listWorkstationNames()
    workstationOptions.value = res.data || []
  } catch {
    /* ignore */
  }
}

const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const submitting = ref(false)
const fileList = ref([])
const editId = ref(null)
const originalStatus = ref('')
const originalWorkstation = ref('')

const form = reactive({
  toolingCode: '',
  productName: '',
  workstation: '',
  entryDate: '',
  status: 'IN_USE',
  imageUrl: '',
  remark: '',
  statusChangeRemark: '',
})

const isStatusChanged = computed(() => {
  return isEdit.value && originalStatus.value !== form.status
})
const isWorkstationChanged = computed(() => {
  return isEdit.value && originalWorkstation.value !== form.workstation
})

const validateStatusChangeRemark = (rule, value, callback) => {
  if (isEdit.value && (isStatusChanged.value || isWorkstationChanged.value)) {
    if (!value || !value.trim()) {
      callback(new Error('状态或工位发生变化时，请填写状态变更说明'))
    } else {
      callback()
    }
  } else {
    callback()
  }
}

const rules = {
  toolingCode: [{ required: true, message: '请输入工装编号', trigger: 'blur' }],
  productName: [{ required: true, message: '请输入适配产品', trigger: 'blur' }],
  workstation: [{ required: true, message: '请选择存放工位', trigger: 'change' }],
  entryDate: [{ required: true, validator: validateEntryDate, trigger: 'change' }],
  statusChangeRemark: [{ validator: validateStatusChangeRemark, trigger: 'blur' }],
}

const openDialog = (item, precheckImageUrl, precheckFileName) => {
  isEdit.value = !!item
  resetCodeValidation()
  if (item) {
    editId.value = item.id
    Object.assign(form, {
      toolingCode: item.toolingCode,
      productName: item.productName,
      workstation: item.workstation,
      entryDate: item.entryDate,
      status: item.status,
      imageUrl: item.imageUrl || '',
      remark: item.remark || '',
      statusChangeRemark: '',
    })
    codeValidation.valid = true
    codeValidation.formatValid = true
    codeValidation.exists = false
    codeValidation.message = '编号校验通过'
    originalStatus.value = item.status || ''
    originalWorkstation.value = item.workstation || ''
    fileList.value = item.imageUrl
      ? [{ name: 'current', url: '/api/file/' + item.imageUrl, status: 'success' }]
      : []
  } else {
    editId.value = null
    Object.assign(form, {
      toolingCode: '',
      productName: '',
      workstation: '',
      entryDate: dayjs().format('YYYY-MM-DD'),
      status: 'IN_USE',
      imageUrl: '',
      remark: '',
      statusChangeRemark: '',
    })
    originalStatus.value = ''
    originalWorkstation.value = ''
    if (precheckImageUrl) {
      if (precheckImageUrl.startsWith('blob:')) {
        fileList.value = [{ name: precheckFileName || 'precheck-image', url: precheckImageUrl, status: 'ready', needsUpload: true }]
      } else {
        fileList.value = [{ name: precheckFileName || 'precheck-image', url: '/api/file/' + precheckImageUrl, status: 'success', uploaded: true, serverUrl: precheckImageUrl }]
      }
    } else {
      fileList.value = []
    }
  }
  dialogVisible.value = true
}

const resetCodeValidation = () => {
  codeValidation.valid = false
  codeValidation.formatValid = false
  codeValidation.exists = false
  codeValidation.message = ''
  codeValidation.suggestedCode = ''
}

const generateNextCode = async () => {
  try {
    const res = await getNextLocatorBlockCode()
    if (res.data) {
      form.toolingCode = res.data
      await validateCode(res.data)
    }
  } catch (e) {
    ElMessage.error(e?.message || '生成编号失败')
  }
}

const applySuggestedCode = async () => {
  if (codeValidation.suggestedCode) {
    form.toolingCode = codeValidation.suggestedCode
    await validateCode(codeValidation.suggestedCode)
  }
}

const validateCode = async (code) => {
  if (!code || !code.trim()) {
    resetCodeValidation()
    return
  }
  try {
    const excludeId = isEdit.value ? editId.value : null
    const res = await validateLocatorBlockCode(code.trim(), excludeId)
    const data = res.data || {}
    codeValidation.valid = data.valid || false
    codeValidation.formatValid = data.formatValid || false
    codeValidation.exists = data.exists || false
    codeValidation.message = data.message || ''
    codeValidation.suggestedCode = data.suggestedCode || ''
  } catch {
    resetCodeValidation()
  }
}

const handleCodeInput = () => {
  if (codeValidateTimer) {
    clearTimeout(codeValidateTimer)
    codeValidateTimer = null
  }
  if (!form.toolingCode || !form.toolingCode.trim()) {
    resetCodeValidation()
    return
  }
  codeValidateTimer = setTimeout(() => {
    validateCode(form.toolingCode)
  }, 500)
}

const handleCodeBlur = () => {
  if (form.toolingCode && form.toolingCode.trim()) {
    validateCode(form.toolingCode)
  }
}

const beforeImageUpload = (file) => {
  const validation = validateImageFile(file)
  if (!validation.valid) {
    ElMessage.error(validation.message)
    return false
  }
  return true
}

const handleImageExceed = () => {
  ElMessage.warning('最多只能上传 1 张图片')
}

const urlToFile = async (url, fileName) => {
  const response = await fetch(url)
  const blob = await response.blob()
  return new File([blob], fileName || 'image.jpg', { type: blob.type })
}

const validateImagePresent = () => {
  if (!fileList.value || fileList.value.length === 0) {
    throw new Error('请上传工装图片')
  }
  const first = fileList.value[0]
  if (first.url && first.url.startsWith('blob:')) {
    return true
  }
  if (first.raw) {
    return true
  }
  if (first.uploaded || first.serverUrl) {
    return true
  }
  if (first.url && first.url.includes('/api/file/')) {
    return true
  }
  throw new Error('图片无效，请重新上传')
}

const prepareImage = async () => {
  validateImagePresent()
  const newFiles = fileList.value.filter((f) => f.raw && f.status !== 'success')
  if (newFiles.length) {
    const rawFiles = newFiles.map((f) => f.raw)
    for (const f of rawFiles) {
      const validation = validateImageFile(f)
      if (!validation.valid) {
        throw new Error(validation.message)
      }
    }
    const compressed = await batchCompressImages(rawFiles)
    const uploadResult = await uploadFile(compressed[0])
    form.imageUrl = uploadResult.data
  } else {
    const existingFile = fileList.value.find((f) => (f.status === 'success' || f.uploaded) && f.url)
    if (existingFile && existingFile.url) {
      if (existingFile.serverUrl) {
        form.imageUrl = existingFile.serverUrl
      } else if (existingFile.url.startsWith('blob:')) {
        const fileFromBlob = await urlToFile(existingFile.url, existingFile.name || 'precheck-image.jpg')
        const validation = validateImageFile(fileFromBlob)
        if (!validation.valid) {
          throw new Error(validation.message)
        }
        const compressed = await batchCompressImages([fileFromBlob])
        const uploadResult = await uploadFile(compressed[0])
        form.imageUrl = uploadResult.data
      } else {
        form.imageUrl = existingFile.url.replace('/api/file/', '')
      }
    } else {
      const needsUploadFile = fileList.value.find((f) => f.needsUpload && f.url)
      if (needsUploadFile && needsUploadFile.url) {
        if (needsUploadFile.url.startsWith('blob:')) {
          const fileFromBlob = await urlToFile(needsUploadFile.url, needsUploadFile.name || 'precheck-image.jpg')
          const validation = validateImageFile(fileFromBlob)
          if (!validation.valid) {
            throw new Error(validation.message)
          }
          const compressed = await batchCompressImages([fileFromBlob])
          const uploadResult = await uploadFile(compressed[0])
          form.imageUrl = uploadResult.data
        }
      }
    }
  }
  if (!form.imageUrl) {
    throw new Error('图片上传失败，请重新上传')
  }
}

const buildDuplicateHtml = (data) => {
  const assets = data.similarAssets || []
  let html = '<div style="color:#e6a23c;font-weight:600;margin-bottom:10px;">⚠️ 检测到疑似重复记录</div>'
  html += '<div style="margin-bottom:12px;">以下工装与当前录入信息高度相似，请确认是否继续录入：</div>'
  html += '<table style="width:100%;border-collapse:collapse;font-size:13px;">'
  html += '<thead><tr style="background:#fafafa;">'
  html += '<th style="border:1px solid #ebeef5;padding:6px 8px;text-align:left;">编号</th>'
  html += '<th style="border:1px solid #ebeef5;padding:6px 8px;text-align:left;">适配产品</th>'
  html += '<th style="border:1px solid #ebeef5;padding:6px 8px;text-align:left;">工位</th>'
  html += '<th style="border:1px solid #ebeef5;padding:6px 8px;text-align:left;">入库日期</th>'
  html += '</tr></thead><tbody>'
  assets.forEach((a) => {
    html += `<tr>
      <td style="border:1px solid #ebeef5;padding:6px 8px;">${a.toolingCode || '-'}</td>
      <td style="border:1px solid #ebeef5;padding:6px 8px;">${a.productName || '-'}</td>
      <td style="border:1px solid #ebeef5;padding:6px 8px;">${a.workstation || '-'}</td>
      <td style="border:1px solid #ebeef5;padding:6px 8px;">${a.entryDate || '-'}</td>
    </tr>`
  })
  html += '</tbody></table>'
  html += '<div style="margin-top:12px;color:#909399;font-size:12px;">（判定条件：适配产品 + 存放工位 相同，且入库日期相差 ±7 天以内）</div>'
  return html
}

const doCreateAsset = async (forceCreate = false) => {
  const res = await createAsset({ ...form }, forceCreate)
  if (res.code === 409) {
    const similarAssets = res.data?.similarAssets || []
    try {
      await ElMessageBox({
        title: '疑似重复提醒',
        dangerouslyUseHTMLString: true,
        message: buildDuplicateHtml({ similarAssets }),
        confirmButtonText: '确认继续录入',
        cancelButtonText: '取消',
        type: 'warning',
      })
      return await doCreateAsset(true)
    } catch {
      throw new Error('cancelled')
    }
  }
  return res
}

const doUpdateAsset = async (forceUpdate = false) => {
  const remarkArg = (isStatusChanged.value || isWorkstationChanged.value)
    ? form.statusChangeRemark
    : undefined
  const res = await updateAsset(editId.value, { ...form }, forceUpdate, remarkArg)
  if (res.code === 409) {
    const similarAssets = res.data?.similarAssets || []
    try {
      await ElMessageBox({
        title: '疑似重复提醒',
        dangerouslyUseHTMLString: true,
        message: buildDuplicateHtml({ similarAssets }),
        confirmButtonText: '确认继续保存',
        cancelButtonText: '取消',
        type: 'warning',
      })
      return await doUpdateAsset(true)
    } catch {
      throw new Error('cancelled')
    }
  }
  return res
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await prepareImage()

    const checkDataParam = isEdit.value ? { ...form, id: editId.value } : { ...form }
    const checkRes = await checkDuplicateAsset(checkDataParam)
    const checkData = checkRes.data || {}

    if (checkData.codeDuplicate) {
      ElMessage.error('工装编号已存在，请更换编号')
      submitting.value = false
      return
    }

    if (checkData.duplicate && checkData.similarAssets && checkData.similarAssets.length > 0) {
      try {
        await ElMessageBox({
          title: '疑似重复提醒',
          dangerouslyUseHTMLString: true,
          message: buildDuplicateHtml(checkData),
          confirmButtonText: isEdit.value ? '确认继续保存' : '确认继续录入',
          cancelButtonText: '取消',
          type: 'warning',
        })
      } catch {
        submitting.value = false
        return
      }
      if (isEdit.value) {
        await doUpdateAsset(true)
      } else {
        await doCreateAsset(true)
      }
    } else {
      if (isEdit.value) {
        await doUpdateAsset(false)
      } else {
        await doCreateAsset(false)
      }
    }
    ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
    dialogVisible.value = false
    fetchList()
    fetchStats()
  } catch (err) {
    if (err && err.message === 'cancelled') return
    ElMessage.error(err?.message || (isEdit.value ? '更新失败' : '创建失败'))
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (item) => {
  try {
    await ElMessageBox.confirm(`确认删除工装 ${item.toolingCode}？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消',
    })
    await deleteAsset(item.id)
    ElMessage.success('删除成功')
    fetchList()
    fetchStats()
  } catch {
    /* cancelled or failed */
  }
}

const viewTrace = (item) => {
  router.push({ path: '/trace', query: { code: item.toolingCode } })
}

const transferVisible = ref(false)
const isHighRiskTransfer = ref(false)
const currentItem = ref(null)
const transferFormRef = ref(null)
const transferForm = reactive({
  toWorkstation: '',
  operator: '',
  statusChangeRemark: '',
  remark: '',
})
const transferRules = {
  toWorkstation: [{ required: true, message: '请选择目标工位', trigger: 'change' }],
  operator: [{ required: true, message: '请输入操作人', trigger: 'blur' }],
  statusChangeRemark: [{ required: true, message: '请填写状态变更说明', trigger: 'blur' }],
}

const highRiskApprovalVisible = ref(false)
const highRiskApprovalFormRef = ref(null)
const submittingApproval = ref(false)
const highRiskApprovalForm = reactive({
  applicant: '',
  applyReason: '',
  remark: '',
})
const highRiskApprovalRules = {
  applicant: [{ required: true, message: '请输入申请人', trigger: 'blur' }],
  applyReason: [{ required: true, message: '请填写申请原因', trigger: 'blur' }],
}

const checkHighRiskOnChange = async () => {
  if (!currentItem.value?.workstation || !transferForm.toWorkstation) {
    isHighRiskTransfer.value = false
    return
  }
  try {
    const res = await checkHighRiskTransfer(currentItem.value.workstation, transferForm.toWorkstation)
    isHighRiskTransfer.value = !!res.data
  } catch {
    isHighRiskTransfer.value = false
  }
}

const openHighRiskApprovalDialog = () => {
  highRiskApprovalForm.applicant = ''
  highRiskApprovalForm.applyReason = ''
  highRiskApprovalForm.remark = ''
  highRiskApprovalVisible.value = true
}

const submitHighRiskApproval = async () => {
  const valid = await highRiskApprovalFormRef.value?.validate().catch(() => false)
  if (!valid) return
  submittingApproval.value = true
  try {
    await applyHighRiskTransfer({
      toolingCode: currentItem.value.toolingCode,
      fromWorkstation: currentItem.value.workstation,
      toWorkstation: transferForm.toWorkstation,
      applicant: highRiskApprovalForm.applicant,
      applyReason: highRiskApprovalForm.applyReason,
      remark: highRiskApprovalForm.remark,
    })
    ElMessage.success('高风险移位审批申请已提交，请等待审批通过后再执行移位')
    highRiskApprovalVisible.value = false
    transferVisible.value = false
  } catch (e) {
    ElMessage.error(e?.message || '提交审批申请失败')
  } finally {
    submittingApproval.value = false
  }
}

const handleTransfer = (item) => {
  currentItem.value = item
  transferForm.toWorkstation = ''
  transferForm.operator = ''
  transferForm.statusChangeRemark = ''
  transferForm.remark = ''
  isHighRiskTransfer.value = false
  transferVisible.value = true
}

const submitTransfer = async () => {
  const valid = await transferFormRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await transferTooling({
      toolingCode: currentItem.value.toolingCode,
      fromWorkstation: currentItem.value.workstation,
      toWorkstation: transferForm.toWorkstation,
      operator: transferForm.operator,
      statusChangeRemark: transferForm.statusChangeRemark,
      remark: transferForm.remark,
    })
    ElMessage.success('移位成功')
    transferVisible.value = false
    fetchList()
    fetchStats()
  } catch (e) {
    ElMessage.error(e?.message || '移位失败')
  } finally {
    submitting.value = false
  }
}

const scrapVisible = ref(false)
const scrapFormRef = ref(null)
const scrapForm = reactive({
  scrapReason: '',
  scrapDate: '',
  operator: '',
  statusChangeRemark: '',
  remark: '',
})
const scrapRules = {
  scrapReason: [{ required: true, message: '请输入报废原因', trigger: 'blur' }],
  scrapDate: [{ required: true, message: '请选择报废日期', trigger: 'change' }],
  operator: [{ required: true, message: '请输入操作人', trigger: 'blur' }],
  statusChangeRemark: [{ required: true, message: '请填写状态变更说明', trigger: 'blur' }],
}

const handleScrap = (item) => {
  currentItem.value = item
  scrapForm.scrapReason = ''
  scrapForm.scrapDate = dayjs().format('YYYY-MM-DD')
  scrapForm.operator = ''
  scrapForm.statusChangeRemark = ''
  scrapForm.remark = ''
  scrapVisible.value = true
}

const buildDuplicateScrapMessage = (data) => {
  if (!data) return '该工装已报废，不允许重复提交。'
  let html = '<div style="color:#e6a23c;font-weight:600;margin-bottom:10px;">⚠️ 该工装已存在报废记录</div>'
  html += '<div style="margin-bottom:10px;">不允许重复提交，已有报废信息如下：</div>'
  html += '<table style="width:100%;border-collapse:collapse;font-size:13px;">'
  html += '<tr><td style="padding:4px 8px;color:#909399;width:80px;">工装编号</td><td style="padding:4px 8px;">' + (data.toolingCode || '-') + '</td></tr>'
  html += '<tr><td style="padding:4px 8px;color:#909399;">报废日期</td><td style="padding:4px 8px;">' + (data.scrapDate || '-') + '</td></tr>'
  html += '<tr><td style="padding:4px 8px;color:#909399;">报废原因</td><td style="padding:4px 8px;">' + (data.scrapReason || '-') + '</td></tr>'
  html += '<tr><td style="padding:4px 8px;color:#909399;">操作人</td><td style="padding:4px 8px;">' + (data.operator || '-') + '</td></tr>'
  if (data.remark) {
    html += '<tr><td style="padding:4px 8px;color:#909399;">备注</td><td style="padding:4px 8px;">' + data.remark + '</td></tr>'
  }
  html += '</table>'
  return html
}

const submitScrap = async () => {
  const valid = await scrapFormRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const res = await scrapTooling({
      toolingCode: currentItem.value.toolingCode,
      scrapReason: scrapForm.scrapReason,
      scrapDate: scrapForm.scrapDate,
      operator: scrapForm.operator,
      statusChangeRemark: scrapForm.statusChangeRemark,
      remark: scrapForm.remark,
    })
    if (res && res.code === 409) {
      await ElMessageBox({
        title: '重复报废提醒',
        dangerouslyUseHTMLString: true,
        message: buildDuplicateScrapMessage(res.data),
        confirmButtonText: '我知道了',
        showCancelButton: false,
        type: 'warning',
      })
      submitting.value = false
      return
    }
    ElMessage.success('报废成功')
    scrapVisible.value = false
    fetchList()
    fetchStats()
  } catch (e) {
    ElMessage.error(e?.message || '报废失败')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  await fetchSystemConfig()
  await fetchWorkstationOptions()
  fetchList()
  fetchStats()
  fetchSpecTemplates()
  const precheckImage = route.query.precheckImage
  const precheckFileName = route.query.precheckFileName
  if (precheckImage) {
    await generateNextCode()
    openDialog(null, precheckImage, precheckFileName)
    router.replace({ query: {} })
  }
})
</script>

<style scoped>
.asset-list {
  min-height: 100%;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}

.search-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.stats-bar {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.stat-label {
  font-size: 14px;
  color: #666;
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
}

.stat-in-use .stat-value {
  color: #67c23a;
}

.stat-transferred .stat-value {
  color: #e6a23c;
}

.stat-scrapped .stat-value {
  color: #f56c6c;
}

.card-grid {
  margin-top: 0;
}

.asset-card {
  margin-bottom: 16px;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
  border-radius: 8px;
  overflow: hidden;
}

.asset-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.asset-card :deep(.el-card__body) {
  padding: 0;
}

.card-img-wrapper {
  position: relative;
  width: 100%;
  height: 160px;
  background: #f5f7fa;
  overflow: hidden;
}

.card-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.card-img-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
  background: #f5f7fa;
}

.card-status-tag {
  position: absolute;
  top: 8px;
  right: 8px;
}

.card-body {
  padding: 12px 16px 8px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-info {
  display: flex;
  align-items: center;
  font-size: 13px;
  line-height: 1.8;
}

.info-label {
  color: #909399;
  min-width: 64px;
}

.info-value {
  color: #606266;
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.spec-summary {
  background: #f7f9fc;
  border: 1px solid #eef2f8;
  border-radius: 6px;
  padding: 6px 10px;
  margin-bottom: 8px;
}

.spec-row {
  display: flex;
  align-items: center;
  font-size: 12px;
  line-height: 1.9;
}

.spec-label {
  color: #909399;
  min-width: 56px;
  flex-shrink: 0;
}

.spec-value {
  color: #303133;
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.spec-placeholder {
  color: #c0c4cc;
  font-size: 12px;
}

.card-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 12px 12px;
  border-top: 1px solid #f0f0f0;
  flex-wrap: wrap;
}

.card-actions .el-button {
  flex: 1;
  min-width: 0;
}

.code-validation-tip {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.5;
}

.code-validation-tip.invalid {
  color: #f56c6c;
}

.code-validation-tip.valid {
  color: #67c23a;
}

.tip-icon {
  flex-shrink: 0;
  margin-top: 1px;
  font-size: 14px;
}

.tip-text {
  flex: 1;
}

.code-format-hint {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
}

.code-format-hint .el-icon {
  font-size: 14px;
  color: #409eff;
}

.entry-date-hint {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
}

.entry-date-hint .el-icon {
  font-size: 14px;
  color: #409eff;
}

.high-risk-warning {
  margin-bottom: 16px;
}
</style>
