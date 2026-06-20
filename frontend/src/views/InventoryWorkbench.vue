<template>
  <div class="inventory-workbench">
    <div class="toolbar">
      <div class="toolbar-left">
        <span class="field-label">清点月份</span>
        <el-date-picker
          v-model="activeMonth"
          type="month"
          placeholder="选择月份"
          value-format="YYYY-MM"
          :clearable="false"
          style="width: 150px"
          @change="onMonthChange"
        />
        <span class="field-label">清点人</span>
        <el-input v-model="checker" placeholder="请输入清点人" style="width: 160px" />
        <el-button :icon="Refresh" :loading="loading" @click="refreshAll">刷新</el-button>
      </div>
      <div class="toolbar-right">
        <el-button type="warning" plain :icon="Operation" @click="goToClosure" v-if="summary.pending > 0">
          处理差异 ({{ summary.pending }})
        </el-button>
      </div>
    </div>

    <div class="summary-row">
      <div class="sum-card sum-book">
        <div class="sum-label">本月账面数</div>
        <div class="sum-value">{{ summary.book }}</div>
        <div class="sum-desc">在档定位块（不含报废）</div>
      </div>
      <div class="sum-card sum-actual">
        <div class="sum-label">实盘数</div>
        <div class="sum-value">{{ summary.actual }}</div>
        <div class="sum-desc">账面 − 缺失 + 盘盈</div>
      </div>
      <div
        class="sum-card sum-diff"
        :class="{
          'diff-positive': summary.diff > 0,
          'diff-negative': summary.diff < 0,
        }"
      >
        <div class="sum-label">差异数</div>
        <div class="sum-value">{{ summary.diff > 0 ? '+' : '' }}{{ summary.diff }}</div>
        <div class="sum-desc">实盘 − 账面</div>
      </div>
      <div class="sum-card sum-progress">
        <div class="sum-label">已盘点</div>
        <div class="sum-value">{{ summary.checked }} / {{ summary.book }}</div>
        <div class="sum-desc">{{ progressPercent }}%</div>
      </div>
      <div class="sum-card sum-pending" v-if="summary.pending > 0">
        <div class="sum-label">待处理差异</div>
        <div class="sum-value">{{ summary.pending }}</div>
        <div class="sum-desc">
          缺{{ summary.missing }} · 不符{{ summary.misplaced }} · 盈{{ summary.extra }}
        </div>
      </div>
    </div>

    <el-card class="ws-panel" shadow="never" v-loading="loading">
      <template #header>
        <div class="panel-header">
          <span class="panel-title">工位清点工作台</span>
          <span class="panel-sub" v-if="activeMonth">
            月份：{{ activeMonth }} · 按工位分组逐项标记「正常 / 缺失 / 工位不符」
          </span>
        </div>
      </template>

      <el-alert type="info" :closable="false" show-icon class="tip-alert">
        <template #title>
          逐项核对每个在档定位块：实物在位且工位一致点击「正常」；实物缺失点击「缺失」；实物存在但工位不符点击「工位不符」并登记实际工位。汇总数据实时更新。
        </template>
      </el-alert>

      <el-empty v-if="!assets.length" description="暂无在档定位块，无法清点" :image-size="80" />

      <el-collapse v-else v-model="activeWorkstations">
        <el-collapse-item
          v-for="g in workstationGroups"
          :key="g.workstation"
          :name="g.workstation"
        >
          <template #title>
            <div class="ws-header">
              <span class="ws-name">
                <el-icon><Location /></el-icon>
                {{ g.workstation || '未分配工位' }}
              </span>
              <div class="ws-stats">
                <span class="ws">
                  <i>账面</i><b>{{ g.book }}</b>
                </span>
                <span class="ws">
                  <i>实盘</i><b>{{ g.actual }}</b>
                </span>
                <span class="ws" :class="diffClass(g.diff)">
                  <i>差异</i><b>{{ g.diff > 0 ? '+' : '' }}{{ g.diff }}</b>
                </span>
                <span class="ws ws-muted">
                  <i>已盘点</i><b>{{ g.checked }}/{{ g.book }}</b>
                </span>
                <span class="ws ws-pending" v-if="g.pending > 0">
                  <i>待处理</i><b>{{ g.pending }}</b>
                </span>
              </div>
            </div>
          </template>

          <el-table :data="g.assets" size="small" border>
            <el-table-column prop="toolingCode" label="定位块编号" min-width="150">
              <template #default="{ row }">
                <span class="code-cell">{{ row.toolingCode }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="productName" label="适配产品" min-width="120" />
            <el-table-column prop="entryDate" label="入库日期" min-width="110" />
            <el-table-column label="状态" min-width="90">
              <template #default="{ row }">
                <el-tag size="small" :type="assetStatusTagType(row.status)">{{ assetStatusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="实际发现工位" min-width="140">
              <template #default="{ row }">
                <span v-if="row.diffType === 'MISPLACED' && row.actualFoundWorkstation" class="ws-diff">
                  {{ row.actualFoundWorkstation }}
                </span>
                <span v-else class="ws-empty">-</span>
              </template>
            </el-table-column>
            <el-table-column label="清点结果" min-width="120">
              <template #default="{ row }">
                <el-tag v-if="row.diffType === 'MATCH'" type="success" size="small">正常</el-tag>
                <el-tag v-else-if="row.diffType === 'MISSING'" type="danger" size="small">
                  缺失
                  <el-tag v-if="row.handleStatus === 'PENDING'" type="danger" size="small" effect="plain" style="margin-left:4px">待处理</el-tag>
                  <el-tag v-else-if="row.handleStatus === 'PROCESSED'" type="success" size="small" effect="plain" style="margin-left:4px">已闭环</el-tag>
                </el-tag>
                <el-tag v-else-if="row.diffType === 'MISPLACED'" type="warning" size="small">
                  工位不符
                  <el-tag v-if="row.handleStatus === 'PENDING'" type="warning" size="small" effect="plain" style="margin-left:4px">待处理</el-tag>
                  <el-tag v-else-if="row.handleStatus === 'PROCESSED'" type="success" size="small" effect="plain" style="margin-left:4px">已闭环</el-tag>
                </el-tag>
                <span v-else class="not-checked">未盘点</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" min-width="300" fixed="right">
              <template #default="{ row }">
                <el-button size="small" type="success" plain :icon="CircleCheck" @click="doMarkNormal(row)" v-if="canMark(row)">
                  正常
                </el-button>
                <el-button size="small" type="danger" plain :icon="CircleClose" @click="doMarkMissing(row)" v-if="canMark(row)">
                  缺失
                </el-button>
                <el-button size="small" type="warning" plain :icon="Warning" @click="openMismatchDialog(row)" v-if="canMark(row)">
                  工位不符
                </el-button>
                <el-button size="small" type="primary" link v-if="row.handleStatus === 'PENDING'" @click="goToClosure">
                  去处理
                </el-button>
                <span v-if="row.handleStatus === 'PROCESSED'" class="closed-text">
                  已闭环：{{ handleTypeLabel(row.handleType) }}
                </span>
              </template>
            </el-table-column>
          </el-table>

          <div v-if="g.extraDiffs.length" class="extra-list">
            <span class="extra-title">盘盈项（台账无记录）：</span>
            <el-tag
              v-for="d in g.extraDiffs"
              :key="d.id || d.toolingCode"
              type="warning"
              size="small"
              class="extra-tag"
            >
              {{ d.toolingCode }}
              <span v-if="d.actualFoundWorkstation" class="extra-ws">@ {{ d.actualFoundWorkstation }}</span>
              <el-tag v-if="d.handleStatus === 'PENDING'" size="small" type="warning" effect="plain" style="margin-left:4px">待处理</el-tag>
            </el-tag>
          </div>
        </el-collapse-item>
      </el-collapse>
    </el-card>

    <el-dialog
      v-model="mismatchDialogVisible"
      :title="`标记工位不符 - ${mismatchRow?.toolingCode || ''}`"
      width="480px"
      destroy-on-close
    >
      <el-form ref="mismatchFormRef" :model="mismatchForm" :rules="mismatchRules" label-width="110px">
        <el-form-item label="登记工位">
          <el-input :model-value="mismatchRow?.workstation" disabled />
        </el-form-item>
        <el-form-item label="实际发现工位" prop="actualFoundWorkstation">
          <el-input v-model="mismatchForm.actualFoundWorkstation" placeholder="请输入实际发现的工位" />
        </el-form-item>
        <el-form-item label="清点人" prop="checker">
          <el-input v-model="mismatchForm.checker" placeholder="请输入清点人姓名" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="mismatchForm.remark" type="textarea" :rows="2" placeholder="请输入备注（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="mismatchDialogVisible = false">取消</el-button>
        <el-button type="warning" :loading="markLoading" @click="confirmMarkMismatch">确认标记工位不符</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Refresh,
  Location,
  CircleCheck,
  CircleClose,
  Warning,
  Operation,
} from '@element-plus/icons-vue'
import {
  listAssets,
  listToolingDiffsByMonth,
  markMatch,
  markMissing,
  markMisplaced,
  getLatestCheck,
} from '../api/tooling'

const router = useRouter()

const loading = ref(false)
const markLoading = ref(false)
const assets = ref([])
const diffs = ref([])
const activeMonth = ref('')
const checker = ref('')

const WS_ORDER = [
  '注塑机01', '注塑机02', '注塑机03', '注塑机04',
  '注塑机05', '注塑机06', '注塑机07', '注塑机08',
  '模具库A区', '模具库B区', '待检区', '维修区',
]

const wsIndexOf = (ws) => {
  const idx = WS_ORDER.indexOf(ws)
  return idx === -1 ? Number.MAX_SAFE_INTEGER : idx
}

const assetStatusLabel = (status) => {
  const map = { IN_USE: '在用', TRANSFERRED: '已移位', SCRAPPED: '已报废' }
  return map[status] || status || '-'
}

const assetStatusTagType = (status) => {
  const map = { IN_USE: 'success', TRANSFERRED: 'warning', SCRAPPED: 'danger' }
  return map[status] || 'info'
}

const handleTypeLabel = (type) => {
  const map = {
    RECOVERED: '确认找回',
    CORRECTED_WORKSTATION: '修正工位',
    SCRAPPED: '转报废',
    REGISTERED: '补录登记',
  }
  return map[type] || type || '-'
}

const diffClass = (diff) => {
  if (diff > 0) return 'diff-positive'
  if (diff < 0) return 'diff-negative'
  return 'diff-zero'
}

const activeAssets = computed(() => assets.value.filter((a) => a.status !== 'SCRAPPED'))

const diffByCode = computed(() => {
  const map = {}
  diffs.value.forEach((d) => {
    if (d.toolingCode) map[d.toolingCode] = d
  })
  return map
})

const workstationGroups = computed(() => {
  const groups = {}
  const ensure = (ws) => {
    if (!groups[ws]) {
      groups[ws] = {
        workstation: ws,
        assets: [],
        extraDiffs: [],
        book: 0,
        missing: 0,
        checked: 0,
        pending: 0,
        actual: 0,
        diff: 0,
      }
    }
    return groups[ws]
  }

  activeAssets.value.forEach((a) => {
    const ws = a.workstation || '未分配工位'
    const g = ensure(ws)
    const d = diffByCode.value[a.toolingCode]
    g.assets.push({
      ...a,
      diffType: d ? d.diffType : null,
      handleStatus: d ? d.handleStatus : null,
      handleType: d ? d.handleType : null,
      actualFoundWorkstation: d ? d.actualFoundWorkstation : null,
      diffId: d ? d.id : null,
    })
    g.book++
    if (d) {
      g.checked++
      if (d.diffType === 'MISSING') g.missing++
      if (d.handleStatus === 'PENDING') g.pending++
    }
  })

  const activeCodes = new Set(activeAssets.value.map((a) => a.toolingCode))
  diffs.value.forEach((d) => {
    if (d.bookExists === false && d.actualExists === true && !activeCodes.has(d.toolingCode)) {
      const ws = d.actualFoundWorkstation || d.workstation || '未分配工位'
      ensure(ws).extraDiffs.push(d)
    }
  })

  Object.values(groups).forEach((g) => {
    g.actual = g.book - g.missing + g.extraDiffs.length
    g.diff = g.actual - g.book
  })

  return Object.values(groups).sort((a, b) => {
    const ia = wsIndexOf(a.workstation)
    const ib = wsIndexOf(b.workstation)
    if (ia !== ib) return ia - ib
    return (a.workstation || '').localeCompare(b.workstation || '')
  })
})

const activeWorkstations = ref([])

const summary = computed(() => {
  let book = 0
  let missing = 0
  let checked = 0
  let extra = 0
  let pending = 0

  const activeCodes = new Set(activeAssets.value.map((a) => a.toolingCode))
  book = activeAssets.value.length

  diffs.value.forEach((d) => {
    if (activeCodes.has(d.toolingCode)) {
      checked++
      if (d.diffType === 'MISSING') missing++
      if (d.handleStatus === 'PENDING') pending++
    } else if (d.bookExists === false && d.actualExists === true) {
      extra++
      if (d.handleStatus === 'PENDING') pending++
    }
  })

  const actual = book - missing + extra
  const diff = actual - book
  const misplaced = diffs.value.filter(
    (d) => d.diffType === 'MISPLACED' && d.handleStatus === 'PENDING'
  ).length

  return { book, actual, diff, checked, missing, misplaced, extra, pending }
})

const progressPercent = computed(() => {
  if (!summary.value.book) return 0
  return Math.round((summary.value.checked / summary.value.book) * 100)
})

const canMark = (row) => {
  if (row.status === 'SCRAPPED') return false
  if (row.handleStatus === 'PROCESSED') return false
  return true
}

const ensureChecker = () => {
  if (!checker.value || !checker.value.trim()) {
    ElMessage.warning('请先在顶部填写清点人')
    return false
  }
  return true
}

const fetchAssets = async () => {
  try {
    const res = await listAssets({})
    assets.value = res.data || []
  } catch {
    assets.value = []
  }
}

const fetchDiffs = async () => {
  if (!activeMonth.value) {
    diffs.value = []
    return
  }
  try {
    const res = await listToolingDiffsByMonth(activeMonth.value)
    diffs.value = res.data || []
  } catch {
    diffs.value = []
  }
}

const fetchLatest = async () => {
  try {
    const res = await getLatestCheck()
    const latest = res.data
    if (latest && latest.checkMonth) {
      if (!activeMonth.value) {
        activeMonth.value = latest.checkMonth
      }
      if (!checker.value && latest.checker) {
        checker.value = latest.checker
      }
    }
  } catch {
    /* ignore */
  }
}

const initDefaultMonth = () => {
  if (!activeMonth.value) {
    const now = new Date()
    const m = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
    activeMonth.value = m
  }
}

const refreshAll = async () => {
  loading.value = true
  try {
    await fetchAssets()
    await fetchDiffs()
  } finally {
    loading.value = false
  }
}

const onMonthChange = () => {
  fetchDiffs()
}

const goToClosure = () => {
  router.push('/inventory-diff-closure')
}

const doMarkNormal = async (row) => {
  if (!ensureChecker()) return
  try {
    await ElMessageBox.confirm(
      `确认将定位块「${row.toolingCode}」标记为正常（实物在位且工位一致）？`,
      '标记正常',
      { type: 'success' }
    )
  } catch {
    return
  }
  try {
    await markMatch({
      checkMonth: activeMonth.value,
      toolingCode: row.toolingCode,
      checker: checker.value,
      remark: '清点确认正常',
    })
    ElMessage.success('已标记正常')
    fetchDiffs()
  } catch (e) {
    ElMessage.error('标记失败：' + (e?.message || ''))
  }
}

const doMarkMissing = async (row) => {
  if (!ensureChecker()) return
  try {
    await ElMessageBox.confirm(
      `确认将定位块「${row.toolingCode}」标记为缺失？\n标记后将生成待处理项，可在差异闭环页面进行「找回 / 修正工位 / 转报废」处理。`,
      '标记缺失',
      { type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await markMissing({
      checkMonth: activeMonth.value,
      toolingCode: row.toolingCode,
      checker: checker.value,
      remark: '清点标记缺失',
    })
    ElMessage.success('已标记缺失，已生成待处理项')
    fetchDiffs()
  } catch (e) {
    ElMessage.error('标记失败：' + (e?.message || ''))
  }
}

const mismatchDialogVisible = ref(false)
const mismatchFormRef = ref(null)
const mismatchRow = ref(null)
const mismatchForm = reactive({
  actualFoundWorkstation: '',
  checker: '',
  remark: '',
})
const mismatchRules = {
  actualFoundWorkstation: [{ required: true, message: '请输入实际发现工位', trigger: 'blur' }],
  checker: [{ required: true, message: '请输入清点人', trigger: 'blur' }],
}

const openMismatchDialog = (row) => {
  if (!ensureChecker()) return
  mismatchRow.value = row
  Object.assign(mismatchForm, {
    actualFoundWorkstation: '',
    checker: checker.value,
    remark: '',
  })
  mismatchDialogVisible.value = true
}

const confirmMarkMismatch = async () => {
  const valid = await mismatchFormRef.value.validate().catch(() => false)
  if (!valid) return
  markLoading.value = true
  try {
    await markMisplaced({
      checkMonth: activeMonth.value,
      toolingCode: mismatchRow.value.toolingCode,
      actualFoundWorkstation: mismatchForm.actualFoundWorkstation,
      checker: mismatchForm.checker,
      remark: mismatchForm.remark || '清点标记工位不符',
    })
    ElMessage.success('已标记工位不符，已生成待处理项')
    mismatchDialogVisible.value = false
    checker.value = mismatchForm.checker
    fetchDiffs()
  } catch (e) {
    ElMessage.error('标记失败：' + (e?.message || ''))
  } finally {
    markLoading.value = false
  }
}

onMounted(async () => {
  initDefaultMonth()
  await fetchLatest()
  initDefaultMonth()
  await refreshAll()
  activeWorkstations.value = workstationGroups.value.map((g) => g.workstation)
})
</script>

<style scoped>
.inventory-workbench {
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

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.toolbar-right {
  display: flex;
  gap: 8px;
}

.field-label {
  font-size: 13px;
  color: #606266;
  white-space: nowrap;
}

.summary-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.sum-card {
  background: #fff;
  border-radius: 10px;
  padding: 16px 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  position: relative;
  overflow: hidden;
}

.sum-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: #909399;
}

.sum-book::before {
  background: #409eff;
}

.sum-actual::before {
  background: #67c23a;
}

.sum-diff::before {
  background: #909399;
}

.sum-progress::before {
  background: #9254de;
}

.sum-pending::before {
  background: #f56c6c;
}

.sum-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 6px;
}

.sum-value {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.2;
  color: #303133;
}

.sum-book .sum-value {
  color: #409eff;
}

.sum-actual .sum-value {
  color: #67c23a;
}

.sum-diff.diff-positive .sum-value {
  color: #e6a23c;
}

.sum-diff.diff-negative .sum-value {
  color: #f56c6c;
}

.sum-progress .sum-value {
  color: #9254de;
  font-size: 24px;
}

.sum-pending .sum-value {
  color: #f56c6c;
}

.sum-desc {
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 4px;
}

.ws-panel {
  margin-bottom: 20px;
}

.ws-panel :deep(.el-card__header) {
  padding: 12px 16px;
}

.panel-header {
  display: flex;
  align-items: baseline;
  gap: 12px;
  flex-wrap: wrap;
}

.panel-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.panel-sub {
  font-size: 12px;
  color: #909399;
}

.tip-alert {
  margin-bottom: 16px;
}

.ws-panel :deep(.el-collapse-item__header) {
  height: auto;
  min-height: 48px;
  padding: 6px 0;
  align-items: center;
}

.ws-header {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
  width: 100%;
  padding-right: 8px;
}

.ws-name {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  min-width: 130px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.ws-name .el-icon {
  color: #409eff;
}

.ws-stats {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

.ws {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #606266;
}

.ws i {
  font-style: normal;
  color: #909399;
  font-size: 12px;
}

.ws b {
  font-weight: 700;
  color: #303133;
}

.ws.diff-positive b {
  color: #e6a23c;
}

.ws.diff-negative b {
  color: #f56c6c;
}

.ws.diff-zero b {
  color: #909399;
}

.ws-muted b {
  color: #909399;
  font-weight: 600;
}

.ws-pending b {
  color: #f56c6c;
  font-weight: 700;
}

.code-cell {
  font-family: 'Menlo', 'Consolas', monospace;
  font-weight: 600;
  color: #1f2d3d;
}

.ws-diff {
  color: #e6a23c;
  font-weight: 600;
}

.ws-empty {
  color: #c0c4cc;
}

.not-checked {
  color: #c0c4cc;
  font-size: 12px;
}

.closed-text {
  color: #67c23a;
  font-size: 12px;
}

.extra-list {
  margin-top: 10px;
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.extra-title {
  font-size: 12px;
  color: #909399;
}

.extra-tag {
  margin: 0;
}

.extra-ws {
  color: #b88230;
  margin-left: 2px;
}
</style>
