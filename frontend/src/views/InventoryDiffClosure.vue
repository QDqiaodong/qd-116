<template>
  <div class="diff-closure">
    <div class="stats-row">
      <div
        class="stat-card"
        :class="{ active: activeFilter === '' }"
        @click="setFilter('')"
      >
        <div class="stat-label">全部待处理</div>
        <div class="stat-value total">{{ counts.total || 0 }}</div>
      </div>
      <div
        class="stat-card stat-missing"
        :class="{ active: activeFilter === 'MISSING' }"
        @click="setFilter('MISSING')"
      >
        <div class="stat-label">盘亏（缺失）</div>
        <div class="stat-value missing">{{ counts.missing || 0 }}</div>
        <div class="stat-desc">账面有、实物不在位</div>
      </div>
      <div
        class="stat-card stat-misplaced"
        :class="{ active: activeFilter === 'MISPLACED' }"
        @click="setFilter('MISPLACED')"
      >
        <div class="stat-label">错位</div>
        <div class="stat-value misplaced">{{ counts.misplaced || 0 }}</div>
        <div class="stat-desc">位置与登记不符</div>
      </div>
      <div
        class="stat-card stat-extra"
        :class="{ active: activeFilter === 'EXTRA' }"
        @click="setFilter('EXTRA')"
      >
        <div class="stat-label">盘盈</div>
        <div class="stat-value extra">{{ counts.extra || 0 }}</div>
        <div class="stat-desc">台账无、实物存在</div>
      </div>
    </div>

    <el-card class="table-card" shadow="never">
      <template #header>
        <div class="card-header">
          <div class="card-title">
            <span>{{ activeTab === 'pending' ? '待处理差异列表' : '已闭环处理记录' }}</span>
            <el-tag v-if="activeTab === 'pending'" size="small" type="danger" effect="plain" style="margin-left: 8px">
              {{ activeFilterLabel }}
            </el-tag>
          </div>
          <div class="card-actions">
            <el-tabs v-model="activeTab" class="mini-tabs" @tab-change="onTabChange">
              <el-tab-pane label="待处理" name="pending" />
              <el-tab-pane label="已闭环" name="processed" />
            </el-tabs>
            <el-button :icon="Refresh" size="small" @click="refresh">刷新</el-button>
          </div>
        </div>
      </template>

      <el-alert
        v-if="activeTab === 'pending'"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 16px"
      >
        <template #title>
          <b>处理说明：</b>
          对于缺失项可操作「确认找回」或「修正工位」或「转报废」；对于错位项可操作「修正工位」或「转报废」；所有处理操作将同步更新资产状态并记录流水。
        </template>
      </el-alert>

      <el-table
        :data="tableData"
        v-loading="loading"
        border
        stripe
        style="width: 100%"
        size="small"
        :row-class-name="rowClassName"
      >
        <el-table-column prop="checkMonth" label="清点月份" min-width="100" />
        <el-table-column prop="toolingCode" label="工装编号" min-width="150">
          <template #default="{ row }">
            <span class="code-cell">{{ row.toolingCode }}</span>
          </template>
        </el-table-column>
        <el-table-column label="差异类型" min-width="100">
          <template #default="{ row }">
            <el-tag v-if="row.diffType === 'MISSING'" type="danger" size="small">盘亏</el-tag>
            <el-tag v-else-if="row.diffType === 'MISPLACED'" type="warning" size="small">错位</el-tag>
            <el-tag v-else-if="row.diffType === 'EXTRA'" type="warning" size="small">盘盈</el-tag>
            <el-tag v-else-if="row.diffType === 'MATCH'" type="success" size="small">一致</el-tag>
            <span v-else>{{ row.diffType || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="expectedWorkstation" label="登记工位" min-width="120">
          <template #default="{ row }">
            {{ row.expectedWorkstation || row.workstation || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="实际发现工位" min-width="120">
          <template #default="{ row }">
            <span v-if="row.actualFoundWorkstation" :class="row.diffType === 'MISPLACED' ? 'ws-diff' : ''">
              {{ row.actualFoundWorkstation }}
            </span>
            <span v-else class="ws-empty">-</span>
          </template>
        </el-table-column>
        <el-table-column label="修正后工位" min-width="120" v-if="activeTab === 'processed'">
          <template #default="{ row }">
            {{ row.correctedWorkstation || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="checker" label="清点人" min-width="100" />
        <el-table-column prop="checkTime" label="标记时间" min-width="160" />
        <el-table-column label="闭环方式" min-width="110" v-if="activeTab === 'processed'">
          <template #default="{ row }">
            <el-tag v-if="row.handleType === 'RECOVERED'" type="success" size="small">确认找回</el-tag>
            <el-tag v-else-if="row.handleType === 'CORRECTED_WORKSTATION'" type="primary" size="small">修正工位</el-tag>
            <el-tag v-else-if="row.handleType === 'SCRAPPED'" type="danger" size="small">转报废</el-tag>
            <el-tag v-else-if="row.handleType === 'REGISTERED'" type="warning" size="small">补录登记</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="闭环信息" min-width="200" v-if="activeTab === 'processed'">
          <template #default="{ row }">
            <div class="closed-info">
              <div><b>处理人：</b>{{ row.handler || '-' }}</div>
              <div><b>处理时间：</b>{{ row.handleTime || '-' }}</div>
              <div v-if="row.handleRemark"><b>备注：</b>{{ row.handleRemark }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="340" fixed="right" v-if="activeTab === 'pending'">
          <template #default="{ row }">
            <el-dropdown
              trigger="click"
              @command="(cmd) => handleAction(cmd, row)"
              v-if="row.diffType === 'MISSING'"
            >
              <el-button size="small" type="primary">
                处理 <el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="recover">
                    <el-icon><CircleCheck /></el-icon>确认找回
                  </el-dropdown-item>
                  <el-dropdown-item command="correct">
                    <el-icon><Location /></el-icon>修正工位
                  </el-dropdown-item>
                  <el-dropdown-item command="scrap">
                    <el-icon><Delete /></el-icon>转报废
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-button
              size="small"
              type="primary"
              @click="handleAction('correct', row)"
              v-else-if="row.diffType === 'MISPLACED'"
            >
              修正工位
            </el-button>
            <el-button
              size="small"
              type="danger"
              @click="handleAction('scrap', row)"
              v-else-if="row.diffType === 'MISPLACED'"
              style="margin-left: 6px"
            >
              转报废
            </el-button>
            <el-tag v-else-if="row.diffType === 'EXTRA'" type="warning" size="small">
              需在台账补录后关闭
            </el-tag>
            <el-button
              size="small"
              type="danger"
              plain
              @click="handleAction('scrap', row)"
              v-else-if="row.diffType === 'MISPLACED'"
            >
              转报废
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!tableData.length && !loading" :description="activeTab === 'pending' ? '暂无待处理差异 🎉' : '暂无已闭环记录'" />
    </el-card>

    <el-dialog
      v-model="recoverDialogVisible"
      title="确认找回"
      width="520px"
      destroy-on-close
    >
      <el-alert type="success" :closable="false" show-icon style="margin-bottom: 16px">
        工装「{{ actionRow?.toolingCode }}」已找回，处理后该差异闭环，资产状态保持不变。
      </el-alert>
      <el-form ref="recoverFormRef" :model="recoverForm" :rules="formRules" label-width="90px">
        <el-form-item label="处理人" prop="handler">
          <el-input v-model="recoverForm.handler" placeholder="请输入处理人姓名" />
        </el-form-item>
        <el-form-item label="处理备注" prop="handleRemark">
          <el-input v-model="recoverForm.handleRemark" type="textarea" :rows="3" placeholder="请输入找回说明（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="recoverDialogVisible = false">取消</el-button>
        <el-button type="success" :loading="actionLoading" @click="confirmRecover">确认找回并闭环</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="correctDialogVisible"
      title="修正工位"
      width="540px"
      destroy-on-close
    >
      <el-alert type="primary" :closable="false" show-icon style="margin-bottom: 16px">
        为工装「{{ actionRow?.toolingCode }}」修正工位，处理后将自动创建移位记录并更新资产工位。
      </el-alert>
      <el-form ref="correctFormRef" :model="correctForm" :rules="correctRules" label-width="100px">
        <el-form-item label="原登记工位">
          <el-input :model-value="actionRow?.expectedWorkstation || actionRow?.workstation || '-'" disabled />
        </el-form-item>
        <el-form-item label="实际发现工位" v-if="actionRow?.actualFoundWorkstation">
          <el-input :model-value="actionRow?.actualFoundWorkstation" disabled />
        </el-form-item>
        <el-form-item label="修正为工位" prop="correctedWorkstation">
          <el-input v-model="correctForm.correctedWorkstation" placeholder="请输入正确的工位，如：注塑机03" />
        </el-form-item>
        <el-form-item label="处理人" prop="handler">
          <el-input v-model="correctForm.handler" placeholder="请输入处理人姓名" />
        </el-form-item>
        <el-form-item label="处理备注" prop="handleRemark">
          <el-input v-model="correctForm.handleRemark" type="textarea" :rows="2" placeholder="请输入修正原因（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="correctDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="actionLoading" @click="confirmCorrect">确认修正工位</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="scrapDialogVisible"
      title="转报废"
      width="540px"
      destroy-on-close
    >
      <el-alert type="error" :closable="false" show-icon style="margin-bottom: 16px">
        工装「{{ actionRow?.toolingCode }}」将被标记为报废，处理后将自动创建报废记录并更新资产状态为"已报废"。该操作不可逆！
      </el-alert>
      <el-form ref="scrapFormRef" :model="scrapForm" :rules="scrapRules" label-width="100px">
        <el-form-item label="报废原因" prop="scrapReason">
          <el-input v-model="scrapForm.scrapReason" type="textarea" :rows="2" placeholder="请输入报废原因" />
        </el-form-item>
        <el-form-item label="报废日期" prop="scrapDate">
          <el-date-picker
            v-model="scrapForm.scrapDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择报废日期，默认今天"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="处理人" prop="handler">
          <el-input v-model="scrapForm.handler" placeholder="请输入处理人姓名" />
        </el-form-item>
        <el-form-item label="处理备注" prop="handleRemark">
          <el-input v-model="scrapForm.handleRemark" type="textarea" :rows="2" placeholder="请输入补充说明（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="scrapDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="actionLoading" @click="confirmScrap">确认报废并闭环</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, ArrowDown, CircleCheck, Location, Delete } from '@element-plus/icons-vue'
import {
  listPendingDiffs,
  countPendingDiffs,
  listToolingDiffsByMonth,
  handleRecover,
  handleCorrectWorkstation,
  handleScrap,
  getLatestCheck,
} from '../api/tooling'
import dayjs from 'dayjs'

const loading = ref(false)
const actionLoading = ref(false)
const tableData = ref([])
const activeTab = ref('pending')
const activeFilter = ref('')
const counts = reactive({ total: 0, missing: 0, misplaced: 0, extra: 0 })
const latestCheckMonth = ref('')

const activeFilterLabel = computed(() => {
  const map = { '': '全部', MISSING: '盘亏', MISPLACED: '错位', EXTRA: '盘盈' }
  return map[activeFilter.value] || '全部'
})

const rowClassName = ({ row }) => {
  if (row.diffType === 'MISSING') return 'row-missing'
  if (row.diffType === 'MISPLACED') return 'row-misplaced'
  if (row.diffType === 'EXTRA') return 'row-extra'
  return ''
}

const setFilter = (filter) => {
  activeFilter.value = filter
  fetchPendingList()
}

const onTabChange = () => {
  refresh()
}

const refresh = () => {
  fetchCounts()
  fetchData()
}

const fetchCounts = async () => {
  try {
    const res = await countPendingDiffs()
    const data = res.data || {}
    counts.total = Number(data.total) || 0
    counts.missing = Number(data.missing) || 0
    counts.misplaced = Number(data.misplaced) || 0
    counts.extra = Number(data.extra) || 0
  } catch {
    /* ignore */
  }
}

const fetchData = async () => {
  if (activeTab.value === 'pending') {
    fetchPendingList()
  } else {
    fetchProcessedList()
  }
}

const fetchPendingList = async () => {
  loading.value = true
  try {
    const res = await listPendingDiffs(activeFilter.value || undefined)
    tableData.value = res.data || []
  } catch (e) {
    ElMessage.error(e?.message || '获取待处理差异失败')
  } finally {
    loading.value = false
  }
}

const fetchProcessedList = async () => {
  loading.value = true
  try {
    if (!latestCheckMonth.value) {
      const res = await getLatestCheck()
      if (res.data) latestCheckMonth.value = res.data.checkMonth
    }
    if (latestCheckMonth.value) {
      const res = await listToolingDiffsByMonth(latestCheckMonth.value)
      tableData.value = (res.data || []).filter((d) => d.handleStatus === 'PROCESSED')
    } else {
      tableData.value = []
    }
  } catch (e) {
    ElMessage.error(e?.message || '获取已闭环记录失败')
  } finally {
    loading.value = false
  }
}

const actionRow = ref(null)

const recoverDialogVisible = ref(false)
const recoverFormRef = ref(null)
const recoverForm = reactive({ handler: '', handleRemark: '' })

const correctDialogVisible = ref(false)
const correctFormRef = ref(null)
const correctForm = reactive({ correctedWorkstation: '', handler: '', handleRemark: '' })
const correctRules = {
  correctedWorkstation: [{ required: true, message: '请输入修正后的工位', trigger: 'blur' }],
  handler: [{ required: true, message: '请输入处理人', trigger: 'blur' }],
}

const scrapDialogVisible = ref(false)
const scrapFormRef = ref(null)
const scrapForm = reactive({ scrapReason: '', scrapDate: '', handler: '', handleRemark: '' })
const scrapRules = {
  scrapReason: [{ required: true, message: '请输入报废原因', trigger: 'blur' }],
  handler: [{ required: true, message: '请输入处理人', trigger: 'blur' }],
}

const formRules = {
  handler: [{ required: true, message: '请输入处理人', trigger: 'blur' }],
}

const handleAction = async (cmd, row) => {
  actionRow.value = row
  if (cmd === 'recover') {
    recoverForm.handler = ''
    recoverForm.handleRemark = ''
    recoverDialogVisible.value = true
  } else if (cmd === 'correct') {
    correctForm.correctedWorkstation = row.actualFoundWorkstation || ''
    correctForm.handler = ''
    correctForm.handleRemark = ''
    correctDialogVisible.value = true
  } else if (cmd === 'scrap') {
    scrapForm.scrapReason = ''
    scrapForm.scrapDate = dayjs().format('YYYY-MM-DD')
    scrapForm.handler = ''
    scrapForm.handleRemark = ''
    scrapDialogVisible.value = true
  }
}

const confirmRecover = async () => {
  const valid = await recoverFormRef.value.validate().catch(() => false)
  if (!valid) return
  actionLoading.value = true
  try {
    await handleRecover(actionRow.value.id, {
      handler: recoverForm.handler,
      handleRemark: recoverForm.handleRemark,
    })
    ElMessage.success('已确认找回，差异闭环')
    recoverDialogVisible.value = false
    refresh()
  } catch (e) {
    ElMessage.error('操作失败：' + (e?.message || ''))
  } finally {
    actionLoading.value = false
  }
}

const confirmCorrect = async () => {
  const valid = await correctFormRef.value.validate().catch(() => false)
  if (!valid) return
  try {
    await ElMessageBox.confirm(
      `确认将工装「${actionRow.value.toolingCode}」工位修正为「${correctForm.correctedWorkstation}」？\n系统将自动创建移位记录。`,
      '修正工位确认',
      { type: 'warning' }
    )
  } catch {
    return
  }
  actionLoading.value = true
  try {
    await handleCorrectWorkstation(actionRow.value.id, {
      correctedWorkstation: correctForm.correctedWorkstation,
      handler: correctForm.handler,
      handleRemark: correctForm.handleRemark,
    })
    ElMessage.success('工位已修正，差异闭环，移位记录已创建')
    correctDialogVisible.value = false
    refresh()
  } catch (e) {
    ElMessage.error('操作失败：' + (e?.message || ''))
  } finally {
    actionLoading.value = false
  }
}

const confirmScrap = async () => {
  const valid = await scrapFormRef.value.validate().catch(() => false)
  if (!valid) return
  try {
    await ElMessageBox.confirm(
      `确认将工装「${actionRow.value.toolingCode}」做报废处理？\n此操作将同时更新资产状态为"已报废"，且不可逆！`,
      '报废确认',
      { type: 'error', confirmButtonText: '确认报废', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  actionLoading.value = true
  try {
    await handleScrap(actionRow.value.id, {
      scrapReason: scrapForm.scrapReason,
      scrapDate: scrapForm.scrapDate,
      handler: scrapForm.handler,
      handleRemark: scrapForm.handleRemark,
    })
    ElMessage.success('已转报废，差异闭环，报废记录已创建')
    scrapDialogVisible.value = false
    refresh()
  } catch (e) {
    ElMessage.error('操作失败：' + (e?.message || ''))
  } finally {
    actionLoading.value = false
  }
}

onMounted(() => {
  refresh()
})
</script>

<style scoped>
.diff-closure {
  min-height: 100%;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.stat-card {
  background: #fff;
  border-radius: 10px;
  padding: 18px 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  cursor: pointer;
  transition: all 0.2s;
  border: 2px solid transparent;
  position: relative;
  overflow: hidden;
}

.stat-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: #909399;
}

.stat-card.active {
  border-color: #409eff;
  box-shadow: 0 2px 12px rgba(64, 158, 255, 0.2);
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 6px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.2;
}

.stat-value.total {
  color: #303133;
}

.stat-value.missing {
  color: #f56c6c;
}

.stat-value.misplaced {
  color: #e6a23c;
}

.stat-value.extra {
  color: #67c23a;
}

.stat-desc {
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 4px;
}

.stat-card.stat-missing::before {
  background: #f56c6c;
}

.stat-card.stat-misplaced::before {
  background: #e6a23c;
}

.stat-card.stat-extra::before {
  background: #67c23a;
}

.table-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  display: flex;
  align-items: center;
}

.card-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.mini-tabs :deep(.el-tabs__header) {
  margin: 0;
}

.mini-tabs :deep(.el-tabs__item) {
  height: 32px;
  line-height: 32px;
  font-size: 13px;
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

.closed-info {
  font-size: 12px;
  color: #606266;
  line-height: 1.7;
}

.closed-info div {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:deep(.row-missing) {
  background-color: #fef0f0;
}

:deep(.row-missing:hover > td) {
  background-color: #fde2e2 !important;
}

:deep(.row-misplaced) {
  background-color: #fdf6ec;
}

:deep(.row-misplaced:hover > td) {
  background-color: #faecd8 !important;
}

:deep(.row-extra) {
  background-color: #f0f9eb;
}

:deep(.row-extra:hover > td) {
  background-color: #e1f3d8 !important;
}
</style>
