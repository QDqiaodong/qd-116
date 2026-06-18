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
        <el-form-item label="备注" prop="remark">
          <el-input v-model="transferForm.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="transferDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitTransfer">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="historyDialogVisible" title="移位历史" width="700px" destroy-on-close>
      <el-table :data="historyData" v-loading="historyLoading" border stripe style="width: 100%">
        <el-table-column prop="fromWorkstation" label="原工位" min-width="100" />
        <el-table-column prop="toWorkstation" label="新工位" min-width="100" />
        <el-table-column prop="transferTime" label="移位时间" min-width="160" />
        <el-table-column prop="operator" label="操作人" min-width="100" />
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
      </el-table>
      <el-empty v-if="!historyData.length && !historyLoading" description="暂无移位记录" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import { listTransfers, getTransferHistory, transferTooling } from '../api/tooling'

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
  } catch {
    ElMessage.error('获取移位记录失败')
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
}

const openTransferDialog = () => {
  Object.assign(transferForm, {
    toolingCode: '',
    fromWorkstation: '',
    toWorkstation: '',
    operator: '',
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
      remark: transferForm.remark,
    })
    ElMessage.success('移位登记成功')
    transferDialogVisible.value = false
    fetchList()
  } catch {
    ElMessage.error('移位登记失败')
  } finally {
    submitting.value = false
  }
}

const historyDialogVisible = ref(false)
const historyLoading = ref(false)
const historyData = ref([])

const viewHistory = async (toolingCode) => {
  historyDialogVisible.value = true
  historyLoading.value = true
  try {
    const res = await getTransferHistory(toolingCode)
    historyData.value = res.data || []
  } catch {
    ElMessage.error('获取移位历史失败')
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
</style>
