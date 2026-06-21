<template>
  <div class="inventory-check">
    <el-card class="batch-panel" shadow="never" v-if="latestBatch">
      <template #header>
        <div class="batch-panel-header">
          <div class="batch-title">
            <span class="batch-label">清点批次</span>
            <span class="batch-name">{{ latestBatch.batchName || latestBatch.batchMonth }}</span>
            <el-tag size="small" :type="batchStatusTagType(latestBatch.status)" effect="dark">
              {{ batchStatusLabel(latestBatch.status) }}
            </el-tag>
          </div>
          <div class="batch-info">
            <span class="bi-item"><i>月份</i><b>{{ latestBatch.batchMonth }}</b></span>
            <span class="bi-item"><i>账面数</i><b>{{ latestBatch.totalBookCount }}</b></span>
            <span class="bi-item"><i>报废排除</i><b>{{ latestBatch.scrappedExcludedCount }}</b></span>
            <span class="bi-item" v-if="latestBatch.freezeTime"><i>冻结时间</i><b>{{ latestBatch.freezeTime?.slice(0,16) }}</b></span>
            <span class="bi-item" v-if="latestBatch.creator"><i>创建人</i><b>{{ latestBatch.creator }}</b></span>
          </div>
          <div class="batch-actions">
            <el-button size="small" type="warning" plain :icon="SwitchFilled"
              @click="doFreezeBatch(latestBatch)"
              v-if="latestBatch.status === 'DRAFT'">
              冻结账面
            </el-button>
            <el-button size="small" type="info" plain :icon="Refresh"
              @click="doUnfreezeBatch(latestBatch)"
              v-if="latestBatch.status === 'FROZEN'">
              解冻
            </el-button>
            <el-button size="small" type="success" plain :icon="Lock"
              @click="doCloseBatch(latestBatch)"
              v-if="latestBatch.status === 'FROZEN'">
              关闭批次
            </el-button>
          </div>
        </div>
      </template>
      <div class="batch-tip" v-if="latestBatch.status === 'DRAFT'">
        <el-alert type="info" :closable="false" show-icon
          title="当前批次为草稿状态，请确认账面数据无误后点击「冻结账面」生成稳定的盘点基准快照。">
        </el-alert>
      </div>
      <div class="batch-tip" v-else-if="latestBatch.status === 'FROZEN'">
        <el-alert type="success" :closable="false" show-icon
          :title="`批次已冻结，账面快照共 ${batchSnapshots.length} 条工装记录，所有差异对比将以此快照为基准。`">
        </el-alert>
      </div>
      <div class="batch-tip" v-else-if="latestBatch.status === 'CLOSED'">
        <el-alert type="success" :closable="false" show-icon
          :title="`批次已关闭，由 ${latestBatch.closer} 于 ${latestBatch.closeTime?.slice(0,16)} 归档。`">
        </el-alert>
      </div>
    </el-card>

    <el-card class="capacity-panel" shadow="never" v-if="workstationCapacities.length">
      <template #header>
        <div class="capacity-panel-header">
          <span class="capacity-title">工位容量配置</span>
          <span class="capacity-sub">共配置 {{ workstationCapacities.length }} 个工位，建档或移位时将自动校验容量</span>
        </div>
      </template>
      <div class="capacity-list">
        <div class="capacity-item" v-for="c in workstationCapacities" :key="c.id"
          @click="doCheckCapacity(c.workstation)" :title="点击查看实时容量">
          <span class="cap-ws">{{ c.workstation }}</span>
          <el-tag size="small" type="info">{{ c.region }}</el-tag>
          <span class="cap-value">上限 <b>{{ c.maxCapacity }}</b> 件</span>
        </div>
      </div>
    </el-card>

    <div class="toolbar">
      <div class="latest-info" v-if="summaryStats">
        <div class="info-card">
          <span class="info-label">最近清点月份</span>
          <span class="info-value">{{ latestCheck?.checkMonth || '-' }}</span>
        </div>
        <div class="info-card">
          <span class="info-label">账面总数</span>
          <span class="info-value stat-book">{{ summaryStats.totalBook }}</span>
        </div>
        <div class="info-card">
          <span class="info-label">实际确认数</span>
          <span class="info-value stat-actual">{{ summaryStats.totalActualConfirmed }}</span>
        </div>
        <div class="info-card">
          <span class="info-label">缺失数</span>
          <span class="info-value stat-missing">{{ summaryStats.totalMissing }}</span>
        </div>
        <div class="info-card">
          <span class="info-label">错位数</span>
          <span class="info-value stat-misplaced">{{ summaryStats.totalMisplaced }}</span>
        </div>
        <div class="info-card">
          <span class="info-label">已报废排除</span>
          <span class="info-value stat-scrapped">{{ summaryStats.totalScrappedExcluded }}</span>
        </div>
        <div class="info-card pending-card" @click="goToClosure" v-if="summaryStats.totalPending > 0">
          <span class="info-label">待处理差异</span>
          <span class="info-value pending-value">
            {{ summaryStats.totalPending }}
            <el-tag size="small" type="danger" class="mini-tag" v-if="summaryStats.totalMissing">缺{{ summaryStats.totalMissing }}</el-tag>
            <el-tag size="small" type="warning" class="mini-tag" v-if="summaryStats.totalMisplaced">错{{ summaryStats.totalMisplaced }}</el-tag>
          </span>
        </div>
      </div>
      <div v-else class="latest-info">
        <div class="info-card">
          <span class="info-label">暂无清点记录</span>
        </div>
      </div>
      <div class="toolbar-right">
        <el-button type="primary" plain :icon="CircleCheck" @click="goToClosure" v-if="summaryStats && summaryStats.totalPending > 0">
          处理差异 ({{ summaryStats.totalPending }})
        </el-button>
        <el-button type="warning" :icon="SwitchFilled" @click="openBatchDialog">创建批次</el-button>
        <el-button type="success" :icon="Plus" @click="openDialog">新增清点</el-button>
      </div>
    </div>

    <el-card class="region-panel" shadow="never">
      <template #header>
        <div class="region-panel-header">
          <span class="region-panel-title">分区盘点</span>
          <span class="region-panel-sub" v-if="latestCheck">
            最近清点月份：{{ latestCheck.checkMonth }}（实盘/差异取自该月逐项盘点记录）
          </span>
          <span class="region-panel-sub" v-else>暂无清点记录，账面数取自工装台账，实盘默认等于账面</span>
          <span class="region-panel-sub tip" v-if="latestCheck">
            提示：点击每行操作按钮逐项标记「一致 / 缺失 / 错位」，标记缺失或错位后将自动生成待处理项
          </span>
        </div>
      </template>
      <el-empty
        v-if="!assets.length"
        description="暂无工装数据，无法分区盘点"
        :image-size="80"
      />
      <el-collapse v-else v-model="activeRegions">
        <el-collapse-item
          v-for="g in regionGroups"
          :key="g.region"
          :name="g.region"
        >
          <template #title>
            <div class="region-header">
              <span class="region-name">{{ g.region }}</span>
              <div class="region-stats">
                <span class="rs">
                  <i>账面</i><b>{{ regionStatMap[g.region]?.bookCount ?? g.book }}</b>
                </span>
                <span class="rs">
                  <i>实确</i><b>{{ regionStatMap[g.region]?.actualConfirmedCount ?? g.actual }}</b>
                </span>
                <span class="rs rs-missing" v-if="(regionStatMap[g.region]?.missingCount ?? g.missing) > 0">
                  <i>缺失</i><b>{{ regionStatMap[g.region]?.missingCount ?? g.missing }}</b>
                </span>
                <span class="rs rs-misplaced" v-if="(regionStatMap[g.region]?.misplacedCount ?? 0) > 0">
                  <i>错位</i><b>{{ regionStatMap[g.region]?.misplacedCount ?? 0 }}</b>
                </span>
                <span class="rs rs-scrapped" v-if="(regionStatMap[g.region]?.scrappedExcludedCount ?? 0) > 0">
                  <i>报废排除</i><b>{{ regionStatMap[g.region]?.scrappedExcludedCount ?? 0 }}</b>
                </span>
                <span class="rs rs-extra" v-if="(regionStatMap[g.region]?.extraCount ?? g.extraDiffs.length) > 0">
                  <i>盘盈</i><b>{{ regionStatMap[g.region]?.extraCount ?? g.extraDiffs.length }}</b>
                </span>
                <span class="rs rs-muted">
                  <i>已盘点</i><b>{{ regionStatMap[g.region]?.checkedCount ?? g.checked }}/{{ regionStatMap[g.region]?.bookCount ?? g.book }}</b>
                </span>
                <span class="rs rs-pending" v-if="(regionStatMap[g.region]?.pendingCount ?? g.pending) > 0">
                  <i>待处理</i><b>{{ regionStatMap[g.region]?.pendingCount ?? g.pending }}</b>
                </span>
              </div>
            </div>
          </template>
          <el-table :data="g.assets" size="small" border>
            <el-table-column prop="toolingCode" label="工装编号" min-width="150" />
            <el-table-column prop="productName" label="适配产品" min-width="120" />
            <el-table-column prop="workstation" label="登记工位" min-width="120" />
            <el-table-column label="实际发现工位" min-width="140">
              <template #default="{ row }">
                <span v-if="row.diffType === 'MISPLACED' && row.actualFoundWorkstation" style="color: #e6a23c">{{ row.actualFoundWorkstation }}</span>
                <span v-else style="color: #c0c4cc">-</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" min-width="90">
              <template #default="{ row }">
                <el-tag size="small" :type="assetStatusTagType(row.status)">{{ assetStatusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="盘点结果" min-width="110">
              <template #default="{ row }">
                <el-tag v-if="row.diffType === 'MISSING'" type="danger" size="small">
                  盘亏
                  <el-tag v-if="row.handleStatus === 'PENDING'" type="danger" size="small" effect="plain" style="margin-left:4px">待处理</el-tag>
                  <el-tag v-else-if="row.handleStatus === 'PROCESSED'" type="success" size="small" effect="plain" style="margin-left:4px">已闭环</el-tag>
                </el-tag>
                <el-tag v-else-if="row.diffType === 'MISPLACED'" type="warning" size="small">
                  错位
                  <el-tag v-if="row.handleStatus === 'PENDING'" type="warning" size="small" effect="plain" style="margin-left:4px">待处理</el-tag>
                  <el-tag v-else-if="row.handleStatus === 'PROCESSED'" type="success" size="small" effect="plain" style="margin-left:4px">已闭环</el-tag>
                </el-tag>
                <el-tag v-else-if="row.diffType === 'EXTRA'" type="warning" size="small">
                  盘盈
                  <el-tag v-if="row.handleStatus === 'PENDING'" type="warning" size="small" effect="plain" style="margin-left:4px">待处理</el-tag>
                </el-tag>
                <el-tag v-else-if="row.diffType === 'MATCH'" type="success" size="small">一致</el-tag>
                <span v-else class="not-checked">未盘点</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" min-width="320" fixed="right">
              <template #default="{ row }">
                <el-button size="small" type="success" plain @click="doMarkMatch(row)" v-if="canMark(row)">
                  一致
                </el-button>
                <el-button size="small" type="danger" plain @click="doMarkMissing(row)" v-if="canMark(row)">
                  缺失
                </el-button>
                <el-button size="small" type="warning" plain @click="openMisplacedDialog(row)" v-if="canMark(row)">
                  错位
                </el-button>
                <el-button size="small" type="primary" link v-if="row.handleStatus === 'PENDING'" @click="goToClosure">
                  处理
                </el-button>
                <span v-if="row.handleStatus === 'PROCESSED'" style="color: #67c23a; font-size: 12px">已闭环：{{ handleTypeLabel(row.handleType) }}</span>
              </template>
            </el-table-column>
          </el-table>
          <div v-if="g.extraDiffs.length" class="extra-list">
            <span class="extra-title">盘盈项（台账无记录）：</span>
            <el-tag
              v-for="d in g.extraDiffs"
              :key="d.toolingCode"
              type="warning"
              size="small"
              class="extra-tag"
            >
              {{ d.toolingCode }}
              <el-tag v-if="d.handleStatus === 'PENDING'" size="small" type="warning" effect="plain" style="margin-left:4px">待处理</el-tag>
            </el-tag>
          </div>
        </el-collapse-item>
      </el-collapse>
    </el-card>

    <div class="section-title">清点记录</div>
    <el-table :data="tableData" v-loading="loading" border stripe style="width: 100%">
      <el-table-column prop="checkMonth" label="清点月份" min-width="120" />
      <el-table-column prop="totalBook" label="账面总数" min-width="100" />
      <el-table-column prop="totalActual" label="实物总数" min-width="100" />
      <el-table-column prop="missingCount" label="缺失数" min-width="90" />
      <el-table-column prop="misplacedCount" label="错位数" min-width="90" />
      <el-table-column prop="scrappedExcludedCount" label="已报废排除" min-width="110" />
      <el-table-column label="差异" min-width="120">
        <template #default="{ row }">
          <span :class="diffClass(row.difference)">{{ row.difference > 0 ? '+' : '' }}{{ row.difference }}</span>
        </template>
      </el-table-column>
      <el-table-column label="差异状态" min-width="100">
        <template #default="{ row }">
          <el-tag v-if="row.difference === 0" type="success">无差异</el-tag>
          <el-tag v-else-if="row.difference < 0" type="danger">盘亏</el-tag>
          <el-tag v-else type="warning">盘盈</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="checker" label="清点人" min-width="100" />
      <el-table-column prop="checkTime" label="清点时间" min-width="160" />
      <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
    </el-table>

    <el-empty v-if="!tableData.length && !loading" description="暂无清点记录" />

    <el-dialog v-model="dialogVisible" title="清点登记" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="清点月份" prop="checkMonth">
          <el-date-picker
            v-model="form.checkMonth"
            type="month"
            placeholder="选择月份"
            value-format="YYYY-MM"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="账面总数" prop="bookCount">
          <el-input-number v-model="form.bookCount" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="实物总数" prop="actualCount">
          <el-input-number v-model="form.actualCount" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="缺失数">
          <el-input-number v-model="form.missingCount" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="错位数">
          <el-input-number v-model="form.misplacedCount" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="已报废排除数">
          <el-input-number v-model="form.scrappedExcludedCount" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="差异">
          <el-input :model-value="differenceDisplay" disabled />
        </el-form-item>
        <el-form-item label="清点人" prop="checker">
          <el-input v-model="form.checker" placeholder="请输入清点人" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="misplacedDialogVisible" :title="`标记错位 - ${misplacedRow?.toolingCode}`" width="480px" destroy-on-close>
      <el-form ref="misplacedFormRef" :model="misplacedForm" :rules="misplacedRules" label-width="110px">
        <el-form-item label="登记工位">
          <el-input :model-value="misplacedRow?.workstation" disabled />
        </el-form-item>
        <el-form-item label="实际发现工位" prop="actualFoundWorkstation">
          <el-input v-model="misplacedForm.actualFoundWorkstation" placeholder="请输入实际发现的工位" />
        </el-form-item>
        <el-form-item label="清点人" prop="checker">
          <el-input v-model="misplacedForm.checker" placeholder="请输入清点人姓名" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="misplacedForm.remark" type="textarea" :rows="2" placeholder="请输入备注（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="misplacedDialogVisible = false">取消</el-button>
        <el-button type="warning" :loading="markLoading" @click="confirmMarkMisplaced">确认标记错位</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="batchDialogVisible" title="创建清点批次" width="480px" destroy-on-close>
      <el-form ref="batchFormRef" :model="batchForm" :rules="batchRules" label-width="100px">
        <el-form-item label="清点月份" prop="batchMonth">
          <el-date-picker
            v-model="batchForm.batchMonth"
            type="month"
            placeholder="选择月份"
            value-format="YYYY-MM"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="批次名称">
          <el-input v-model="batchForm.batchName" placeholder="留空则使用默认名称（月份+月度盘点）" />
        </el-form-item>
        <el-form-item label="创建人" prop="creator">
          <el-input v-model="batchForm.creator" placeholder="请输入创建人姓名" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="batchForm.remark" type="textarea" :rows="2" placeholder="请输入备注（可选）" />
        </el-form-item>
      </el-form>
      <div class="batch-tip" style="padding:0 20px 10px">
        <el-alert type="info" :closable="false" size="small" show-icon
          title="创建后为草稿状态，需手动点击「冻结账面」生成工装工位快照作为盘点基准。">
        </el-alert>
      </div>
      <template #footer>
        <el-button @click="batchDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="batchSubmitting" @click="handleBatchSubmit">创建批次</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, CircleCheck, SwitchFilled, Lock, Refresh } from '@element-plus/icons-vue'
import {
  listChecks,
  createCheck,
  getLatestCheck,
  listAssets,
  listToolingDiffsByMonth,
  markMissing,
  markMisplaced,
  markMatch,
  countPendingDiffs,
  getInventorySummaryStats,
  createInventoryBatch,
  freezeInventoryBatch,
  unfreezeInventoryBatch,
  closeInventoryBatch,
  listInventoryBatches,
  getLatestInventoryBatch,
  listInventoryBatchSnapshotsByMonth,
  listWorkstationCapacities,
  checkWorkstationCapacity,
} from '../api/tooling'

const router = useRouter()
const loading = ref(false)
const tableData = ref([])
const latestCheck = ref(null)
const assets = ref([])
const diffs = ref([])
const summaryStats = ref(null)
const pendingCounts = reactive({ total: 0, missing: 0, misplaced: 0, extra: 0 })
const activeRegions = ref(['注塑机区', '模具库', '待检区', '其他'])
const markLoading = ref(false)

const batches = ref([])
const latestBatch = ref(null)
const batchSnapshots = ref([])
const batchDialogVisible = ref(false)
const batchSubmitting = ref(false)
const workstationCapacities = ref([])
const capacityDialogVisible = ref(false)

const REGION_ORDER = ['注塑机区', '模具库', '待检区', '其他']

const batchForm = reactive({
  batchMonth: '',
  batchName: '',
  creator: '',
  remark: '',
})

const batchRules = {
  batchMonth: [{ required: true, message: '请选择清点月份', trigger: 'change' }],
  creator: [{ required: true, message: '请输入创建人', trigger: 'blur' }],
}

const regionOf = (ws) => {
  if (!ws) return '其他'
  if (ws.startsWith('注塑机')) return '注塑机区'
  if (ws.startsWith('模具库')) return '模具库'
  if (ws === '待检区' || ws.startsWith('待检')) return '待检区'
  return '其他'
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
  const map = { RECOVERED: '确认找回', CORRECTED_WORKSTATION: '修正工位', SCRAPPED: '转报废', REGISTERED: '补录登记' }
  return map[type] || type || '-'
}

const batchStatusLabel = (status) => {
  const map = { DRAFT: '草稿', FROZEN: '已冻结', CLOSED: '已关闭' }
  return map[status] || status || '-'
}

const batchStatusTagType = (status) => {
  const map = { DRAFT: 'info', FROZEN: 'warning', CLOSED: 'success' }
  return map[status] || 'info'
}

const regionStatMap = computed(() => {
  const map = {}
  if (summaryStats.value && summaryStats.value.regions) {
    summaryStats.value.regions.forEach((r) => {
      map[r.region] = r
    })
  }
  return map
})

const regionGroups = computed(() => {
  const activeAssets = assets.value.filter((a) => a.status !== 'SCRAPPED')
  const diffByCode = {}
  diffs.value.forEach((d) => {
    diffByCode[d.toolingCode] = d
  })
  const groups = {}
  const ensure = (r) => {
    if (!groups[r]) {
      groups[r] = { region: r, assets: [], book: 0, missing: 0, misplaced: 0, checked: 0, extraDiffs: [], actual: 0, diff: 0, pending: 0 }
    }
    return groups[r]
  }
  REGION_ORDER.forEach(ensure)
  activeAssets.forEach((a) => {
    const g = ensure(regionOf(a.workstation))
    const d = diffByCode[a.toolingCode]
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
      if (d.diffType === 'MISPLACED') g.misplaced++
      if (d.handleStatus === 'PENDING') g.pending++
    }
  })
  const activeCodes = new Set(activeAssets.map((a) => a.toolingCode))
  diffs.value.forEach((d) => {
    if (d.bookExists === false && d.actualExists === true && !activeCodes.has(d.toolingCode)) {
      ensure(regionOf(d.workstation || d.actualFoundWorkstation)).extraDiffs.push(d)
    }
  })
  Object.values(groups).forEach((g) => {
    g.actual = g.book - g.missing + g.extraDiffs.length
    g.diff = g.actual - g.book
  })
  return REGION_ORDER
    .map((r) => groups[r])
    .filter((g) => g.region !== '其他' || g.assets.length || g.extraDiffs.length)
})

const canMark = (row) => {
  if (!latestCheck.value) return false
  if (row.status === 'SCRAPPED') return false
  if (row.handleStatus === 'PROCESSED') return false
  return true
}

const fetchAssets = async () => {
  try {
    const res = await listAssets({})
    assets.value = res.data || []
  } catch {
    /* ignore */
  }
}

const fetchDiffs = async () => {
  if (!latestCheck.value || !latestCheck.value.checkMonth) {
    diffs.value = []
    return
  }
  try {
    const res = await listToolingDiffsByMonth(latestCheck.value.checkMonth)
    diffs.value = res.data || []
  } catch {
    diffs.value = []
  }
}

const fetchPendingCounts = async () => {
  try {
    const res = await countPendingDiffs()
    const data = res.data || {}
    pendingCounts.total = Number(data.total) || 0
    pendingCounts.missing = Number(data.missing) || 0
    pendingCounts.misplaced = Number(data.misplaced) || 0
    pendingCounts.extra = Number(data.extra) || 0
  } catch {
    /* ignore */
  }
}

const fetchSummaryStats = async () => {
  try {
    const month = latestCheck.value?.checkMonth || null
    const res = await getInventorySummaryStats(month)
    summaryStats.value = res.data || null
  } catch {
    summaryStats.value = null
  }
}

const fetchBatches = async () => {
  try {
    const res = await listInventoryBatches()
    batches.value = res.data || []
  } catch {
    batches.value = []
  }
}

const fetchLatestBatch = async () => {
  try {
    const res = await getLatestInventoryBatch()
    latestBatch.value = res.data || null
  } catch {
    latestBatch.value = null
  }
}

const fetchBatchSnapshots = async () => {
  if (!latestBatch.value || !latestBatch.value.batchMonth || latestBatch.value.status === 'DRAFT') {
    batchSnapshots.value = []
    return
  }
  try {
    const res = await listInventoryBatchSnapshotsByMonth(latestBatch.value.batchMonth)
    batchSnapshots.value = res.data || []
  } catch {
    batchSnapshots.value = []
  }
}

const fetchWorkstationCapacities = async () => {
  try {
    const res = await listWorkstationCapacities({})
    workstationCapacities.value = res.data || []
  } catch {
    workstationCapacities.value = []
  }
}

const openBatchDialog = () => {
  const now = new Date()
  const defaultMonth = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
  Object.assign(batchForm, {
    batchMonth: defaultMonth,
    batchName: '',
    creator: '',
    remark: '',
  })
  batchDialogVisible.value = true
}

const batchFormRef = ref(null)

const handleBatchSubmit = async () => {
  const valid = await batchFormRef.value?.validate().catch(() => false)
  if (!valid) return
  batchSubmitting.value = true
  try {
    await createInventoryBatch({
      batchMonth: batchForm.batchMonth,
      batchName: batchForm.batchName || undefined,
      creator: batchForm.creator,
      remark: batchForm.remark || undefined,
    })
    ElMessage.success('清点批次创建成功')
    batchDialogVisible.value = false
    await fetchBatches()
    await fetchLatestBatch()
  } catch (e) {
    ElMessage.error(e?.message || '创建批次失败')
  } finally {
    batchSubmitting.value = false
  }
}

const doFreezeBatch = async (batch) => {
  try {
    await ElMessageBox.confirm(
      `确认冻结批次「${batch.batchName || batch.batchMonth}」？\n冻结后将快照当前所有工装的账面工位与状态，作为后续差异比对的稳定基准。冻结后可解冻重新生成快照。`,
      '冻结批次确认',
      { type: 'warning' }
    )
  } catch {
    return
  }
  try {
    const operator = latestCheck.value?.checker || 'admin'
    await freezeInventoryBatch(batch.id, { freezer: operator })
    ElMessage.success('批次冻结成功，账面快照已生成')
    await fetchBatches()
    await fetchLatestBatch()
    await fetchBatchSnapshots()
  } catch (e) {
    ElMessage.error('冻结失败：' + (e?.message || ''))
  }
}

const doUnfreezeBatch = async (batch) => {
  try {
    await ElMessageBox.confirm(
      `确认解冻批次「${batch.batchName || batch.batchMonth}」？\n解冻后将清除已生成的账面快照，可重新冻结生成新的快照。`,
      '解冻批次确认',
      { type: 'warning' }
    )
  } catch {
    return
  }
  try {
    const operator = latestCheck.value?.checker || 'admin'
    await unfreezeInventoryBatch(batch.id, { operator })
    ElMessage.success('批次已解冻，快照已清除')
    await fetchBatches()
    await fetchLatestBatch()
    batchSnapshots.value = []
  } catch (e) {
    ElMessage.error('解冻失败：' + (e?.message || ''))
  }
}

const doCloseBatch = async (batch) => {
  try {
    await ElMessageBox.confirm(
      `确认关闭批次「${batch.batchName || batch.batchMonth}」？\n关闭后批次不可再修改，差异处理结果将永久归档。`,
      '关闭批次确认',
      { type: 'warning' }
    )
  } catch {
    return
  }
  try {
    const operator = latestCheck.value?.checker || 'admin'
    await closeInventoryBatch(batch.id, { closer: operator })
    ElMessage.success('批次已关闭')
    await fetchBatches()
    await fetchLatestBatch()
  } catch (e) {
    ElMessage.error('关闭失败：' + (e?.message || ''))
  }
}

const doCheckCapacity = async (workstation) => {
  if (!workstation) return
  try {
    const res = await checkWorkstationCapacity(workstation)
    const data = res.data || {}
    ElMessage({
      message: data.message || '容量查询完成',
      type: data.sufficient ? 'success' : 'warning',
      duration: 4000,
    })
  } catch (e) {
    ElMessage.error('容量查询失败：' + (e?.message || ''))
  }
}

const fetchList = async () => {
  loading.value = true
  try {
    const res = await listChecks()
    tableData.value = res.data || []
  } catch (e) {
    ElMessage.error(e?.message || '获取清点记录失败')
  } finally {
    loading.value = false
  }
}

const fetchLatest = async () => {
  try {
    const res = await getLatestCheck()
    latestCheck.value = res.data || null
  } catch {
    /* ignore */
  }
}

const diffClass = (diff) => {
  if (diff > 0) return 'diff-positive'
  if (diff < 0) return 'diff-negative'
  return 'diff-zero'
}

const goToClosure = () => {
  router.push('/inventory-diff-closure')
}

const doMarkMatch = async (row) => {
  if (!latestCheck.value) return
  try {
    await ElMessageBox.confirm(`确认将工装「${row.toolingCode}」标记为一致？`, '标记确认', { type: 'success' })
  } catch {
    return
  }
  try {
    await markMatch({
      checkMonth: latestCheck.value.checkMonth,
      toolingCode: row.toolingCode,
      checker: latestCheck.value.checker || 'admin',
      remark: '清点确认一致',
    })
    ElMessage.success('已标记一致')
    fetchDiffs()
    fetchPendingCounts()
    fetchSummaryStats()
  } catch (e) {
    ElMessage.error('标记失败：' + (e?.message || ''))
  }
}

const doMarkMissing = async (row) => {
  if (!latestCheck.value) return
  try {
    await ElMessageBox.confirm(
      `确认将工装「${row.toolingCode}」标记为缺失？\n标记后将生成待处理项，可在差异处理页面进行「找回 / 修正工位 / 转报废」操作。`,
      '标记缺失确认',
      { type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await markMissing({
      checkMonth: latestCheck.value.checkMonth,
      toolingCode: row.toolingCode,
      checker: latestCheck.value.checker || 'admin',
      remark: '清点标记缺失',
    })
    ElMessage.success('已标记缺失，已生成待处理项')
    fetchDiffs()
    fetchPendingCounts()
    fetchSummaryStats()
  } catch (e) {
    ElMessage.error('标记失败：' + (e?.message || ''))
  }
}

const misplacedDialogVisible = ref(false)
const misplacedFormRef = ref(null)
const misplacedRow = ref(null)
const misplacedForm = reactive({
  actualFoundWorkstation: '',
  checker: '',
  remark: '',
})
const misplacedRules = {
  actualFoundWorkstation: [{ required: true, message: '请输入实际发现工位', trigger: 'blur' }],
  checker: [{ required: true, message: '请输入清点人', trigger: 'blur' }],
}

const openMisplacedDialog = (row) => {
  if (!latestCheck.value) return
  misplacedRow.value = row
  Object.assign(misplacedForm, {
    actualFoundWorkstation: '',
    checker: latestCheck.value.checker || '',
    remark: '',
  })
  misplacedDialogVisible.value = true
}

const confirmMarkMisplaced = async () => {
  const valid = await misplacedFormRef.value.validate().catch(() => false)
  if (!valid) return
  markLoading.value = true
  try {
    await markMisplaced({
      checkMonth: latestCheck.value.checkMonth,
      toolingCode: misplacedRow.value.toolingCode,
      actualFoundWorkstation: misplacedForm.actualFoundWorkstation,
      checker: misplacedForm.checker,
      remark: misplacedForm.remark || '清点标记错位',
    })
    ElMessage.success('已标记错位，已生成待处理项')
    misplacedDialogVisible.value = false
    fetchDiffs()
    fetchPendingCounts()
    fetchSummaryStats()
  } catch (e) {
    ElMessage.error('标记失败：' + (e?.message || ''))
  } finally {
    markLoading.value = false
  }
}

const dialogVisible = ref(false)
const formRef = ref(null)
const submitting = ref(false)

const form = reactive({
  checkMonth: '',
  bookCount: 0,
  actualCount: 0,
  missingCount: 0,
  misplacedCount: 0,
  scrappedExcludedCount: 0,
  checker: '',
  remark: '',
})

const rules = {
  checkMonth: [{ required: true, message: '请选择清点月份', trigger: 'change' }],
  bookCount: [{ required: true, message: '请输入账面总数', trigger: 'blur' }],
  actualCount: [{ required: true, message: '请输入实物总数', trigger: 'blur' }],
  checker: [{ required: true, message: '请输入清点人', trigger: 'blur' }],
}

const differenceDisplay = computed(() => {
  const diff = form.actualCount - form.bookCount
  return diff > 0 ? `+${diff}` : `${diff}`
})

const openDialog = () => {
  Object.assign(form, {
    checkMonth: '',
    bookCount: 0,
    actualCount: 0,
    missingCount: 0,
    misplacedCount: 0,
    scrappedExcludedCount: 0,
    checker: '',
    remark: '',
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await createCheck({
      checkMonth: form.checkMonth,
      totalBook: form.bookCount,
      totalActual: form.actualCount,
      missingCount: form.missingCount,
      misplacedCount: form.misplacedCount,
      scrappedExcludedCount: form.scrappedExcludedCount,
      checker: form.checker,
      remark: form.remark,
    })
    ElMessage.success('清点登记成功')
    dialogVisible.value = false
    fetchList()
    fetchAssets()
    await fetchLatest()
    fetchDiffs()
    fetchPendingCounts()
    fetchSummaryStats()
  } catch (e) {
    ElMessage.error(e?.message || '清点登记失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  fetchList()
  fetchAssets()
  fetchLatest().then(() => {
    fetchDiffs()
    fetchSummaryStats()
  })
  fetchPendingCounts()
  fetchBatches()
  fetchLatestBatch().then(() => {
    fetchBatchSnapshots()
  })
  fetchWorkstationCapacities()
})
</script>

<style scoped>
.inventory-check {
  min-height: 100%;
}

.batch-panel {
  margin-bottom: 12px;
  border: 1px solid #ebeef5;
  background: linear-gradient(135deg, #f5f7fa 0%, #ffffff 100%);
}

.batch-panel :deep(.el-card__header) {
  padding: 12px 20px;
  border-bottom: 1px solid #ebeef5;
}

.batch-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  flex-wrap: wrap;
}

.batch-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.batch-label {
  font-size: 14px;
  color: #909399;
  font-weight: 500;
}

.batch-name {
  font-size: 17px;
  font-weight: 700;
  color: #303133;
}

.batch-info {
  display: flex;
  align-items: center;
  gap: 18px;
  flex-wrap: wrap;
}

.bi-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
}

.bi-item i {
  font-style: normal;
  color: #909399;
  font-size: 12px;
}

.bi-item b {
  font-weight: 600;
  color: #303133;
}

.batch-actions {
  display: flex;
  gap: 8px;
}

.batch-tip {
  margin-top: 8px;
}

.capacity-panel {
  margin-bottom: 12px;
  border: 1px solid #ebeef5;
}

.capacity-panel :deep(.el-card__header) {
  padding: 10px 20px;
}

.capacity-panel-header {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.capacity-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.capacity-sub {
  font-size: 12px;
  color: #909399;
}

.capacity-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.capacity-item {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  background: #f5f7fa;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.capacity-item:hover {
  background: #ecf5ff;
  border-color: #409eff;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.15);
}

.cap-ws {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

.cap-value {
  font-size: 12px;
  color: #606266;
}

.cap-value b {
  color: #409eff;
  font-weight: 700;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}

.toolbar-right {
  display: flex;
  gap: 8px;
}

.latest-info {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.info-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px 24px;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  min-width: 100px;
}

.info-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 4px;
}

.info-value {
  font-size: 20px;
  font-weight: 700;
  color: #303133;
}

.pending-card {
  cursor: pointer;
  border: 1px solid #fef0f0;
  background: linear-gradient(135deg, #fff 0%, #fef0f0 100%);
  transition: all 0.2s;
}

.pending-card:hover {
  box-shadow: 0 2px 12px rgba(245, 108, 108, 0.25);
  transform: translateY(-1px);
}

.pending-value {
  color: #f56c6c;
  display: flex;
  align-items: center;
  gap: 4px;
}

.mini-tag {
  font-size: 10px;
  height: 16px;
  line-height: 14px;
}

.diff-positive .info-value {
  color: #e6a23c;
}

.diff-negative .info-value {
  color: #f56c6c;
}

.diff-positive {
  color: #e6a23c;
  font-weight: 600;
}

.diff-negative {
  color: #f56c6c;
  font-weight: 600;
}

.diff-zero {
  color: #909399;
  font-weight: 600;
}

.stat-book {
  color: #409eff;
}

.stat-actual {
  color: #67c23a;
}

.stat-missing {
  color: #f56c6c;
}

.stat-misplaced {
  color: #e6a23c;
}

.stat-scrapped {
  color: #909399;
}

.rs-missing b {
  color: #f56c6c;
}

.rs-misplaced b {
  color: #e6a23c;
}

.rs-scrapped b {
  color: #909399;
  font-weight: 600;
}

.rs-extra b {
  color: #909399;
  font-weight: 600;
}

.region-panel {
  margin-bottom: 20px;
}

.region-panel :deep(.el-card__header) {
  padding: 12px 16px;
}

.region-panel-header {
  display: flex;
  align-items: baseline;
  gap: 12px;
  flex-wrap: wrap;
}

.region-panel-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.region-panel-sub {
  font-size: 12px;
  color: #909399;
}

.region-panel-sub.tip {
  color: #e6a23c;
}

.region-panel :deep(.el-collapse-item__header) {
  height: auto;
  min-height: 48px;
  padding: 6px 0;
  align-items: center;
}

.region-header {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
  width: 100%;
  padding-right: 8px;
}

.region-name {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  min-width: 72px;
}

.region-stats {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

.rs {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #606266;
}

.rs i {
  font-style: normal;
  color: #909399;
  font-size: 12px;
}

.rs b {
  font-weight: 700;
  color: #303133;
}

.rs.diff-positive b {
  color: #e6a23c;
}

.rs.diff-negative b {
  color: #f56c6c;
}

.rs.diff-zero b {
  color: #909399;
}

.rs-muted b {
  color: #909399;
  font-weight: 600;
}

.rs-pending b {
  color: #f56c6c;
  font-weight: 700;
}

.not-checked {
  color: #c0c4cc;
  font-size: 12px;
}

.extra-list {
  margin-top: 8px;
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

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin: 4px 0 12px;
  padding-left: 8px;
  border-left: 3px solid #409eff;
}
</style>
