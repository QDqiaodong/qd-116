<template>
  <div class="scrap-record">
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
      <el-button type="success" :icon="Plus" @click="openScrapDialog">新增报废登记</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" border stripe style="width: 100%">
      <el-table-column prop="toolingCode" label="工装编号" min-width="120" />
      <el-table-column prop="scrapReason" label="报废原因" min-width="160" show-overflow-tooltip />
      <el-table-column prop="scrapDate" label="报废日期" min-width="120" />
      <el-table-column prop="operator" label="操作人" min-width="100" />
      <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" link @click="viewHistory(row.toolingCode)">查看历史</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="scrapDialogVisible" title="报废登记" width="520px" destroy-on-close>
      <el-form ref="scrapFormRef" :model="scrapForm" :rules="scrapRules" label-width="100px">
        <el-form-item label="工装编号" prop="toolingCode">
          <el-input v-model="scrapForm.toolingCode" placeholder="请输入工装编号" />
        </el-form-item>
        <el-form-item label="报废日期" prop="scrapDate">
          <el-date-picker v-model="scrapForm.scrapDate" type="date" placeholder="请选择报废日期" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="报废原因" prop="scrapReason">
          <el-input v-model="scrapForm.scrapReason" type="textarea" :rows="3" placeholder="请输入报废原因" />
        </el-form-item>
        <el-form-item label="操作人" prop="operator">
          <el-input v-model="scrapForm.operator" placeholder="请输入操作人" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="scrapForm.remark" type="textarea" :rows="2" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="scrapDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitScrap">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="historyDialogVisible" title="报废历史" width="700px" destroy-on-close>
      <el-table :data="historyData" v-loading="historyLoading" border stripe style="width: 100%">
        <el-table-column prop="toolingCode" label="工装编号" min-width="120" />
        <el-table-column prop="scrapReason" label="报废原因" min-width="160" show-overflow-tooltip />
        <el-table-column prop="scrapDate" label="报废日期" min-width="120" />
        <el-table-column prop="operator" label="操作人" min-width="100" />
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
      </el-table>
      <el-empty v-if="!historyData.length && !historyLoading" description="暂无报废记录" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import { listScraps, getScrapHistory, scrapTooling } from '../api/tooling'

const loading = ref(false)
const tableData = ref([])
const keyword = ref('')

const fetchList = async () => {
  loading.value = true
  try {
    const res = await listScraps()
    tableData.value = res.data || []
  } catch {
    ElMessage.error('获取报废记录失败')
  } finally {
    loading.value = false
  }
}

const scrapDialogVisible = ref(false)
const scrapFormRef = ref(null)
const submitting = ref(false)
const scrapForm = reactive({
  toolingCode: '',
  scrapDate: '',
  scrapReason: '',
  operator: '',
  remark: '',
})

const scrapRules = {
  toolingCode: [{ required: true, message: '请输入工装编号', trigger: 'blur' }],
  scrapDate: [{ required: true, message: '请选择报废日期', trigger: 'change' }],
  scrapReason: [{ required: true, message: '请输入报废原因', trigger: 'blur' }],
  operator: [{ required: true, message: '请输入操作人', trigger: 'blur' }],
}

const openScrapDialog = () => {
  Object.assign(scrapForm, {
    toolingCode: '',
    scrapDate: '',
    scrapReason: '',
    operator: '',
    remark: '',
  })
  scrapDialogVisible.value = true
}

const submitScrap = async () => {
  const valid = await scrapFormRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await scrapTooling({
      toolingCode: scrapForm.toolingCode,
      scrapDate: scrapForm.scrapDate,
      scrapReason: scrapForm.scrapReason,
      operator: scrapForm.operator,
      remark: scrapForm.remark,
    })
    ElMessage.success('报废登记成功')
    scrapDialogVisible.value = false
    fetchList()
  } catch {
    ElMessage.error('报废登记失败')
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
    const res = await getScrapHistory(toolingCode)
    historyData.value = res.data || []
  } catch {
    ElMessage.error('获取报废历史失败')
  } finally {
    historyLoading.value = false
  }
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.scrap-record {
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
