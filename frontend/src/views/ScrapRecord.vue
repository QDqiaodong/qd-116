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
      <el-table-column label="报废原因" min-width="200">
        <template #default="{ row }">
          <div class="reason-tags">
            <el-tag
              v-for="(tag, idx) in parseReasonTags(row.scrapReason)"
              :key="idx"
              :type="tag.type"
              :effect="tag.effect"
              size="small"
              class="reason-tag"
            >{{ tag.label }}</el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="scrapDate" label="报废日期" min-width="120" />
      <el-table-column prop="operator" label="操作人" min-width="100" />
      <el-table-column prop="statusChangeRemark" label="状态变更说明" min-width="180">
        <template #default="{ row }">
          <el-tooltip
            v-if="row.statusChangeRemark && row.statusChangeRemark.length > 15"
            :content="row.statusChangeRemark"
            placement="top"
            popper-class="remark-tooltip"
          >
            <span class="remark-text">{{ row.statusChangeRemark.slice(0, 15) }}...</span>
          </el-tooltip>
          <span v-else class="remark-text">{{ row.statusChangeRemark || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="备注" min-width="160">
        <template #default="{ row }">
          <el-tooltip
            v-if="row.remark && row.remark.length > 15"
            :content="row.remark"
            placement="top"
            popper-class="remark-tooltip"
          >
            <span class="remark-text">{{ row.remark.slice(0, 15) }}...</span>
          </el-tooltip>
          <span v-else class="remark-text">{{ row.remark || '-' }}</span>
        </template>
      </el-table-column>
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
          <el-input v-model="scrapForm.scrapReason" type="textarea" :rows="3" placeholder="请输入报废原因，如：磨损、断裂、维修失败等" />
          <div class="reason-suggestions">
            <el-tag
              v-for="reason in commonReasons"
              :key="reason"
              size="small"
              class="suggestion-tag"
              :type="getReasonTagType(reason)"
              @click="appendReason(reason)"
            >{{ reason }}</el-tag>
          </div>
        </el-form-item>
        <el-form-item label="操作人" prop="operator">
          <el-input v-model="scrapForm.operator" placeholder="请输入操作人" />
        </el-form-item>
        <el-form-item label="状态变更说明" prop="statusChangeRemark">
          <el-input v-model="scrapForm.statusChangeRemark" type="textarea" :rows="2" placeholder="请说明报废详细情况，必填" />
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
        <el-table-column label="报废原因" min-width="200">
          <template #default="{ row }">
            <div class="reason-tags">
              <el-tag
                v-for="(tag, idx) in parseReasonTags(row.scrapReason)"
                :key="idx"
                :type="tag.type"
                :effect="tag.effect"
                size="small"
                class="reason-tag"
              >{{ tag.label }}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="scrapDate" label="报废日期" min-width="120" />
        <el-table-column prop="operator" label="操作人" min-width="100" />
        <el-table-column prop="statusChangeRemark" label="状态变更说明" min-width="180">
          <template #default="{ row }">
            <el-tooltip
              v-if="row.statusChangeRemark && row.statusChangeRemark.length > 15"
              :content="row.statusChangeRemark"
              placement="top"
              popper-class="remark-tooltip"
            >
              <span class="remark-text">{{ row.statusChangeRemark.slice(0, 15) }}...</span>
            </el-tooltip>
            <span v-else class="remark-text">{{ row.statusChangeRemark || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="备注" min-width="160">
          <template #default="{ row }">
            <el-tooltip
              v-if="row.remark && row.remark.length > 15"
              :content="row.remark"
              placement="top"
              popper-class="remark-tooltip"
            >
              <span class="remark-text">{{ row.remark.slice(0, 15) }}...</span>
            </el-tooltip>
            <span v-else class="remark-text">{{ row.remark || '-' }}</span>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!historyData.length && !historyLoading" description="暂无报废记录" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import { listScraps, getScrapHistory, scrapTooling } from '../api/tooling'

const commonReasons = ['磨损', '断裂', '维修失败', '老化', '变形', '锈蚀', '其他']

const reasonTypeMap = {
  '磨损': { type: 'warning', effect: 'light' },
  '断裂': { type: 'danger', effect: 'dark' },
  '维修失败': { type: 'info', effect: 'dark' },
  '老化': { type: 'success', effect: 'light' },
  '变形': { type: 'warning', effect: 'dark' },
  '锈蚀': { type: 'danger', effect: 'light' },
  '其他': { type: 'info', effect: 'light' },
}

const getReasonTagType = (reason) => {
  for (const key of Object.keys(reasonTypeMap)) {
    if (reason.includes(key)) {
      return reasonTypeMap[key].type
    }
  }
  return 'info'
}

const parseReasonTags = (reason) => {
  if (!reason) return []
  const keywords = Object.keys(reasonTypeMap)
  const foundTags = []
  let remaining = reason

  for (const kw of keywords) {
    if (remaining.includes(kw)) {
      foundTags.push({ label: kw, ...reasonTypeMap[kw] })
      remaining = remaining.replace(new RegExp(kw, 'g'), '')
    }
  }

  remaining = remaining.replace(/[，,、；;\s]+/g, ' ').trim()
  if (remaining) {
    const extras = remaining.split(/\s+/).filter(Boolean)
    extras.forEach((e) => {
      if (!foundTags.some((t) => t.label === e)) {
        foundTags.push({ label: e, type: 'info', effect: 'plain' })
      }
    })
  }

  if (foundTags.length === 0) {
    foundTags.push({ label: reason, type: 'info', effect: 'plain' })
  }

  return foundTags
}

const appendReason = (reason) => {
  if (!scrapForm.scrapReason) {
    scrapForm.scrapReason = reason
  } else if (!scrapForm.scrapReason.includes(reason)) {
    scrapForm.scrapReason = scrapForm.scrapReason + '、' + reason
  }
}

const loading = ref(false)
const tableData = ref([])
const keyword = ref('')

const fetchList = async () => {
  loading.value = true
  try {
    const res = await listScraps()
    tableData.value = res.data || []
  } catch (e) {
    ElMessage.error(e?.message || '获取报废记录失败')
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
  statusChangeRemark: '',
  remark: '',
})

const scrapRules = {
  toolingCode: [{ required: true, message: '请输入工装编号', trigger: 'blur' }],
  scrapDate: [{ required: true, message: '请选择报废日期', trigger: 'change' }],
  scrapReason: [{ required: true, message: '请输入报废原因', trigger: 'blur' }],
  operator: [{ required: true, message: '请输入操作人', trigger: 'blur' }],
  statusChangeRemark: [{ required: true, message: '请填写状态变更说明', trigger: 'blur' }],
}

const openScrapDialog = () => {
  Object.assign(scrapForm, {
    toolingCode: '',
    scrapDate: '',
    scrapReason: '',
    operator: '',
    statusChangeRemark: '',
    remark: '',
  })
  scrapDialogVisible.value = true
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
      toolingCode: scrapForm.toolingCode,
      scrapDate: scrapForm.scrapDate,
      scrapReason: scrapForm.scrapReason,
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
    ElMessage.success('报废登记成功')
    scrapDialogVisible.value = false
    fetchList()
  } catch (e) {
    ElMessage.error(e?.message || '报废登记失败')
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
  } catch (e) {
    ElMessage.error(e?.message || '获取报废历史失败')
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

.reason-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.reason-tag {
  margin-right: 0;
}

.remark-text {
  color: #606266;
  font-size: 13px;
}

.reason-suggestions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 6px;
}

.suggestion-tag {
  cursor: pointer;
  transition: transform 0.15s ease;
}

.suggestion-tag:hover {
  transform: translateY(-1px);
}
</style>

<style>
.remark-tooltip {
  max-width: 400px;
  word-break: break-all;
  white-space: pre-wrap;
}
</style>
