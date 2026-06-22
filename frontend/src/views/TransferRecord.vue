<template>
  <div class="transfer-record">
    <div class="toolbar">
      <div class="search-bar">
        <el-input
          v-model="keyword"
          placeholder="工装编号搜索"
          clearable
          :prefix-icon="Search"
          @clear="fetchList"
          @keyup.enter="fetchList"
          style="width: 220px"
        />
        <el-button type="primary" :icon="Search" @click="fetchList">查询</el-button>
      </div>
      <el-button type="success" :icon="Plus" @click="openTransferDialog">新增移位登记</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" border stripe style="width: 100%">
      <el-table-column prop="toolingCode" label="工装编号" min-width="120" />
      <el-table-column prop="fromWorkstation" label="原工位" min-width="100" />
      <el-table-column prop="toWorkstation" label="新工位" min-width="100" />
      <el-table-column prop="transferTime" label="移位时间" min-width="160" />
      <el-table-column prop="operator" label="操作人" min-width="100" />
      <el-table-column prop="statusChangeRemark" label="状态变更说明" min-width="180" show-overflow-tooltip />
      <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" link @click="viewHistory(row.toolingCode)">查看历史</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="transferDialogVisible" title="移位登记" width="520px" destroy-on-close>
      <el-form ref="transferFormRef" :model="transferForm" :rules="transferRules" label-width="100px">
        <el-form-item label="工装编号" prop="toolingCode">
          <el-input v-model="transferForm.toolingCode" placeholder="请输入工装编号" />
        </el-form-item>
        <el-form-item label="原工位" prop="fromWorkstation">
          <el-select v-model="transferForm.fromWorkstation" placeholder="请选择原工位" style="width: 100%">
            <el-option v-for="ws in workstationOptions" :key="ws" :label="ws" :value="ws" />
          </el-select>
        </el-form-item>
        <el-form-item label="新工位" prop="toWorkstation">
          <el-select v-model="transferForm.toWorkstation" placeholder="请选择新工位" style="width: 100%">
            <el-option v-for="ws in workstationOptions" :key="ws" :label="ws" :value="ws" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作人" prop="operator">
          <el-input v-model="transferForm.operator" placeholder="请输入操作人" />
        </el-form-item>
        <el-form-item label="状态变更说明" prop="statusChangeRemark">
          <el-input v-model="transferForm.statusChangeRemark" type="textarea" :rows="2" placeholder="请说明移位原因，必填" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="transferForm.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="transferDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitTransfer">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="historyDialogVisible" title="移位历史" width="760px" destroy-on-close>
      <div v-loading="historyLoading" class="history-content">
        <div v-if="stayData.length" class="stay-path-section">
          <div class="section-title">
            <el-icon :size="16"><Position /></el-icon>
            <span>工位路径</span>
          </div>
          <div class="path-flow">
            <div
              v-for="(stay, idx) in stayData"
              :key="stay.sequence"
              class="path-node"
              :class="{ 'is-current': idx === stayData.length - 1 }"
            >
              <div class="node-badge">{{ stay.sequence }}</div>
              <div class="node-name">{{ stay.workstation }}</div>
              <div class="node-days">{{ stay.stayDays }}天</div>
              <div v-if="idx < stayData.length - 1" class="node-arrow">
                <el-icon><Right /></el-icon>
              </div>
            </div>
          </div>
        </div>

        <div v-if="stayData.length" class="stay-list-section">
          <div class="section-title">
            <el-icon :size="16"><Clock /></el-icon>
            <span>停留明细</span>
            <span class="stay-count">共 {{ stayData.length }} 段停留</span>
          </div>
          <div class="stay-list">
            <div
              v-for="(stay, idx) in stayData"
              :key="stay.sequence"
              class="stay-item"
              :class="{ 'is-current': idx === stayData.length - 1 }"
            >
              <div class="stay-left">
                <div class="stay-seq">
                  <span class="seq-badge">{{ stay.sequence }}</span>
                </div>
                <div class="stay-timeline">
                  <div class="timeline-dot"></div>
                  <div v-if="idx < stayData.length - 1" class="timeline-line"></div>
                </div>
              </div>
              <div class="stay-body">
                <div class="stay-header">
                  <span class="stay-workstation">{{ stay.workstation }}</span>
                  <el-tag v-if="idx === stayData.length - 1" type="success" size="small">当前位置</el-tag>
                </div>
                <div class="stay-info">
                  <span class="info-item">
                    <el-icon><Calendar /></el-icon>
                    开始：{{ formatDateTime(stay.startTime) }}
                  </span>
                  <span class="info-item">
                    <el-icon><Calendar /></el-icon>
                    结束：{{ formatDateTime(stay.endTime) }}
                  </span>
                </div>
                <div class="stay-meta">
                  <span class="meta-tag">
                    <el-icon><Timer /></el-icon>
                    停留 {{ stay.stayDays }} 天
                  </span>
                  <span v-if="stay.lastOperator" class="meta-tag">
                    <el-icon><User /></el-icon>
                    最近操作：{{ stay.lastOperator }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <el-empty v-if="!stayData.length && !historyLoading" description="暂无移位记录" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Plus, Clock, User, Calendar, Timer, Position, Right } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { listTransfers, getTransferHistory, transferTooling, getWorkstationStays } from '../api/tooling'

const workstationOptions = [
  '注塑机01', '注塑机02', '注塑机03', '注塑机04',
  '注塑机05', '注塑机06', '注塑机07', '注塑机08',
  '模具库A区', '模具库B区', '待检区', '维修区',
]

const loading = ref(false)
const tableData = ref([])
const keyword = ref('')

const fetchList = async () => {
  loading.value = true
  try {
    const res = await listTransfers()
    tableData.value = res.data || []
  } catch (e) {
    ElMessage.error(e?.message || '获取移位记录失败')
  } finally {
    loading.value = false
  }
}

const transferDialogVisible = ref(false)
const transferFormRef = ref(null)
const submitting = ref(false)
const transferForm = reactive({
  toolingCode: '',
  fromWorkstation: '',
  toWorkstation: '',
  operator: '',
  statusChangeRemark: '',
  remark: '',
})

const validateToWorkstation = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请选择新工位'))
  } else if (value === transferForm.fromWorkstation) {
    callback(new Error('新工位不能与原工位相同'))
  } else {
    callback()
  }
}

const transferRules = {
  toolingCode: [{ required: true, message: '请输入工装编号', trigger: 'blur' }],
  fromWorkstation: [{ required: true, message: '请选择原工位', trigger: 'change' }],
  toWorkstation: [{ required: true, validator: validateToWorkstation, trigger: 'change' }],
  operator: [{ required: true, message: '请输入操作人', trigger: 'blur' }],
  statusChangeRemark: [{ required: true, message: '请填写状态变更说明', trigger: 'blur' }],
}

const openTransferDialog = () => {
  Object.assign(transferForm, {
    toolingCode: '',
    fromWorkstation: '',
    toWorkstation: '',
    operator: '',
    statusChangeRemark: '',
    remark: '',
  })
  transferDialogVisible.value = true
}

const submitTransfer = async () => {
  const valid = await transferFormRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await transferTooling({
      toolingCode: transferForm.toolingCode,
      fromWorkstation: transferForm.fromWorkstation,
      toWorkstation: transferForm.toWorkstation,
      operator: transferForm.operator,
      statusChangeRemark: transferForm.statusChangeRemark,
      remark: transferForm.remark,
    })
    ElMessage.success('移位登记成功')
    transferDialogVisible.value = false
    fetchList()
  } catch (e) {
    ElMessage.error(e?.message || '移位登记失败')
  } finally {
    submitting.value = false
  }
}

const historyDialogVisible = ref(false)
const historyLoading = ref(false)
const historyData = ref([])
const stayData = ref([])

const formatDateTime = (t) => {
  if (!t) return '-'
  return dayjs(t).format('YYYY-MM-DD HH:mm')
}

const viewHistory = async (toolingCode) => {
  historyDialogVisible.value = true
  historyLoading.value = true
  try {
    const [transferRes, staysRes] = await Promise.all([
      getTransferHistory(toolingCode),
      getWorkstationStays(toolingCode),
    ])
    historyData.value = transferRes.data || []
    stayData.value = staysRes.data || []
  } catch (e) {
    ElMessage.error(e?.message || '获取移位历史失败')
  } finally {
    historyLoading.value = false
  }
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.transfer-record {
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

.history-content {
  min-height: 200px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid #f0f0f0;
}

.stay-count {
  margin-left: auto;
  font-size: 12px;
  font-weight: 400;
  color: #909399;
}

.stay-path-section {
  margin-bottom: 20px;
  padding: 16px;
  background: #f7f9fc;
  border-radius: 8px;
}

.path-flow {
  display: flex;
  align-items: stretch;
  gap: 0;
  overflow-x: auto;
  padding: 4px 0;
}

.path-node {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
  min-width: 80px;
  position: relative;
  padding: 10px 8px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
  transition: all 0.2s;
}

.path-node:hover {
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.15);
}

.path-node.is-current {
  background: #f0f9eb;
  border-color: #67c23a;
}

.node-badge {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #409eff;
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}

.path-node.is-current .node-badge {
  background: #67c23a;
}

.node-name {
  font-size: 12px;
  font-weight: 600;
  color: #303133;
  text-align: center;
  word-break: break-all;
  max-width: 80px;
}

.node-days {
  font-size: 11px;
  color: #909399;
}

.path-node.is-current .node-days {
  color: #67c23a;
  font-weight: 500;
}

.node-arrow {
  position: absolute;
  right: -10px;
  top: 50%;
  transform: translateY(-50%);
  color: #c0c4cc;
  z-index: 1;
  font-size: 14px;
}

.stay-list-section {
  margin-bottom: 8px;
}

.stay-list {
  display: flex;
  flex-direction: column;
}

.stay-item {
  display: flex;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px dashed #f0f0f0;
}

.stay-item:last-child {
  border-bottom: none;
}

.stay-item.is-current .stay-body {
  background: #f0f9eb;
}

.stay-left {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex-shrink: 0;
  width: 36px;
}

.stay-seq {
  margin-bottom: 4px;
}

.seq-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #409eff;
  color: #fff;
  font-size: 12px;
  font-weight: 600;
}

.stay-item.is-current .seq-badge {
  background: #67c23a;
}

.stay-timeline {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
}

.timeline-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #c0c4cc;
  flex-shrink: 0;
}

.stay-item.is-current .timeline-dot {
  background: #67c23a;
}

.timeline-line {
  width: 2px;
  flex: 1;
  background: #ebeef5;
  margin-top: 4px;
}

.stay-body {
  flex: 1;
  padding: 8px 12px;
  border-radius: 6px;
  transition: background 0.2s;
}

.stay-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.stay-workstation {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.stay-info {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 8px;
}

.stay-info .info-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #606266;
}

.stay-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.meta-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  background: #ecf5ff;
  color: #409eff;
  font-size: 12px;
  border-radius: 4px;
}

.stay-item.is-current .meta-tag {
  background: #e1f3d8;
  color: #67c23a;
}
</style>
