<template>
  <div class="inventory-check">
    <div class="toolbar">
      <div class="latest-info" v-if="latestCheck">
        <div class="info-card">
          <span class="info-label">最近清点月份</span>
          <span class="info-value">{{ latestCheck.checkMonth }}</span>
        </div>
        <div class="info-card">
          <span class="info-label">账面数量</span>
          <span class="info-value">{{ latestCheck.totalBook }}</span>
        </div>
        <div class="info-card">
          <span class="info-label">实物数量</span>
          <span class="info-value">{{ latestCheck.totalActual }}</span>
        </div>
        <div class="info-card" :class="{ 'diff-positive': latestCheck.difference > 0, 'diff-negative': latestCheck.difference < 0 }">
          <span class="info-label">差异</span>
          <span class="info-value">{{ latestCheck.difference > 0 ? '+' : '' }}{{ latestCheck.difference }}</span>
        </div>
      </div>
      <div v-else class="latest-info">
        <div class="info-card">
          <span class="info-label">暂无清点记录</span>
        </div>
      </div>
      <el-button type="success" :icon="Plus" @click="openDialog">新增清点</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" border stripe style="width: 100%">
      <el-table-column prop="checkMonth" label="清点月份" min-width="120" />
      <el-table-column prop="totalBook" label="账面数量" min-width="100" />
      <el-table-column prop="totalActual" label="实物数量" min-width="100" />
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
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="清点月份" prop="checkMonth">
          <el-date-picker
            v-model="form.checkMonth"
            type="month"
            placeholder="选择月份"
            value-format="YYYY-MM"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="账面数量" prop="bookCount">
          <el-input-number v-model="form.bookCount" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="实物数量" prop="actualCount">
          <el-input-number v-model="form.actualCount" :min="0" style="width: 100%" />
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
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { listChecks, createCheck, getLatestCheck } from '../api/tooling'

const loading = ref(false)
const tableData = ref([])
const latestCheck = ref(null)

const fetchList = async () => {
  loading.value = true
  try {
    const res = await listChecks()
    tableData.value = res.data || []
  } catch {
    ElMessage.error('获取清点记录失败')
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

const dialogVisible = ref(false)
const formRef = ref(null)
const submitting = ref(false)

const form = reactive({
  checkMonth: '',
  bookCount: 0,
  actualCount: 0,
  checker: '',
  remark: '',
})

const rules = {
  checkMonth: [{ required: true, message: '请选择清点月份', trigger: 'change' }],
  bookCount: [{ required: true, message: '请输入账面数量', trigger: 'blur' }],
  actualCount: [{ required: true, message: '请输入实物数量', trigger: 'blur' }],
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
      checker: form.checker,
      remark: form.remark,
    })
    ElMessage.success('清点登记成功')
    dialogVisible.value = false
    fetchList()
    fetchLatest()
  } catch {
    ElMessage.error('清点登记失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  fetchList()
  fetchLatest()
})
</script>

<style scoped>
.inventory-check {
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
</style>
