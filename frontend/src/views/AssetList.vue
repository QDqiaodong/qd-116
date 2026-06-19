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
      <el-col v-for="item in list" :key="item.id" :xl="4" :lg="6" :md="8" :sm="12">
        <el-card class="asset-card" shadow="hover">
          <div class="card-img-wrapper">
            <img
              v-if="item.imageUrl"
              :src="'/api/file/' + item.imageUrl"
              class="card-img"
              alt=""
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
            <div class="card-info">
              <span class="info-label">适配产品</span>
              <span class="info-value">{{ item.productName }}</span>
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
            <el-button size="small" type="warning" :icon="Switch" @click="handleTransfer(item)" :disabled="item.status !== 'IN_USE'">移位</el-button>
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
          <el-input v-model="form.toolingCode" placeholder="请输入工装编号" />
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
            value-format="yyyy-MM-dd"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="图片">
          <el-upload
            v-model:file-list="fileList"
            action="#"
            :auto-upload="false"
            list-type="picture-card"
            accept="image/*"
            :limit="1"
          >
            <el-icon :size="24"><Plus /></el-icon>
          </el-upload>
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

    <el-dialog v-model="transferVisible" title="工装移位" width="480px" destroy-on-close>
      <el-form ref="transferFormRef" :model="transferForm" :rules="transferRules" label-width="100px">
        <el-form-item label="工装编号">
          <el-input :model-value="currentItem?.toolingCode" disabled />
        </el-form-item>
        <el-form-item label="当前工位">
          <el-input :model-value="currentItem?.workstation" disabled />
        </el-form-item>
        <el-form-item label="目标工位" prop="toWorkstation">
          <el-select v-model="transferForm.toWorkstation" placeholder="请选择目标工位" style="width: 100%">
            <el-option v-for="ws in workstationOptions" :key="ws" :label="ws" :value="ws" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作人" prop="operator">
          <el-input v-model="transferForm.operator" placeholder="请输入操作人" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="transferForm.remark" type="textarea" :rows="2" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="transferVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitTransfer">确认移位</el-button>
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
            value-format="yyyy-MM-dd"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="操作人" prop="operator">
          <el-input v-model="scrapForm.operator" placeholder="请输入操作人" />
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
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
} from '@element-plus/icons-vue'
import {
  listAssets,
  createAsset,
  updateAsset,
  deleteAsset,
  getStats,
  uploadFile,
  transferTooling,
  scrapTooling,
  checkDuplicateAsset,
} from '../api/tooling'
import { batchCompressImages } from '../utils/compress'

const router = useRouter()

const workstationOptions = [
  '注塑机01', '注塑机02', '注塑机03', '注塑机04',
  '注塑机05', '注塑机06', '注塑机07', '注塑机08',
  '模具库A区', '模具库B区', '待检区', '维修区',
]

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

const fetchList = async () => {
  loading.value = true
  try {
    const params = {}
    if (keyword.value) params.keyword = keyword.value
    if (filterStatus.value) params.status = filterStatus.value
    if (filterWorkstation.value) params.workstation = filterWorkstation.value
    const res = await listAssets(params)
    list.value = res.data || []
  } catch {
    ElMessage.error('获取列表失败')
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

const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const submitting = ref(false)
const fileList = ref([])
const editId = ref(null)

const form = reactive({
  toolingCode: '',
  productName: '',
  workstation: '',
  entryDate: '',
  status: 'IN_USE',
  imageUrl: '',
  remark: '',
})

const rules = {
  toolingCode: [{ required: true, message: '请输入工装编号', trigger: 'blur' }],
  productName: [{ required: true, message: '请输入适配产品', trigger: 'blur' }],
  workstation: [{ required: true, message: '请选择存放工位', trigger: 'change' }],
  entryDate: [{ required: true, message: '请选择入库日期', trigger: 'change' }],
}

const openDialog = (item) => {
  isEdit.value = !!item
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
    })
    fileList.value = item.imageUrl
      ? [{ name: 'current', url: '/api/file/' + item.imageUrl, status: 'success' }]
      : []
  } else {
    editId.value = null
    Object.assign(form, {
      toolingCode: '',
      productName: '',
      workstation: '',
      entryDate: '',
      status: 'IN_USE',
      imageUrl: '',
      remark: '',
    })
    fileList.value = []
  }
  dialogVisible.value = true
}

const prepareImage = async () => {
  const newFiles = fileList.value.filter((f) => f.raw && f.status !== 'success')
  if (newFiles.length) {
    const rawFiles = newFiles.map((f) => f.raw)
    const compressed = await batchCompressImages(rawFiles)
    const uploadResult = await uploadFile(compressed[0])
    form.imageUrl = uploadResult.data
  } else {
    const existingFile = fileList.value.find((f) => f.status === 'success' && f.url)
    if (existingFile && existingFile.url) {
      form.imageUrl = existingFile.url.replace('/api/file/', '')
    }
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
    try {
      await ElMessageBox({
        title: '疑似重复提醒',
        dangerouslyUseHTMLString: true,
        message: buildDuplicateHtml({ similarAssets: [] }) + '<div style="margin-top:10px;">' + (res.message || '存在疑似重复记录') + '</div>',
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

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await prepareImage()

    if (isEdit.value) {
      await updateAsset(editId.value, { ...form })
      ElMessage.success('更新成功')
    } else {
      const checkRes = await checkDuplicateAsset({ ...form })
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
            confirmButtonText: '确认继续录入',
            cancelButtonText: '取消',
            type: 'warning',
          })
        } catch {
          submitting.value = false
          return
        }
        await doCreateAsset(true)
      } else {
        await doCreateAsset(false)
      }
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchList()
    fetchStats()
  } catch (err) {
    if (err && err.message === 'cancelled') return
    ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
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
const currentItem = ref(null)
const transferFormRef = ref(null)
const transferForm = reactive({
  toWorkstation: '',
  operator: '',
  remark: '',
})
const transferRules = {
  toWorkstation: [{ required: true, message: '请选择目标工位', trigger: 'change' }],
  operator: [{ required: true, message: '请输入操作人', trigger: 'blur' }],
}

const handleTransfer = (item) => {
  currentItem.value = item
  transferForm.toWorkstation = ''
  transferForm.operator = ''
  transferForm.remark = ''
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
      remark: transferForm.remark,
    })
    ElMessage.success('移位成功')
    transferVisible.value = false
    fetchList()
    fetchStats()
  } catch {
    ElMessage.error('移位失败')
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
  remark: '',
})
const scrapRules = {
  scrapReason: [{ required: true, message: '请输入报废原因', trigger: 'blur' }],
  scrapDate: [{ required: true, message: '请选择报废日期', trigger: 'change' }],
  operator: [{ required: true, message: '请输入操作人', trigger: 'blur' }],
}

const handleScrap = (item) => {
  currentItem.value = item
  scrapForm.scrapReason = ''
  scrapForm.scrapDate = new Date().toISOString().split('T')[0]
  scrapForm.operator = ''
  scrapForm.remark = ''
  scrapVisible.value = true
}

const submitScrap = async () => {
  const valid = await scrapFormRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await scrapTooling({
      toolingCode: currentItem.value.toolingCode,
      scrapReason: scrapForm.scrapReason,
      scrapDate: scrapForm.scrapDate,
      operator: scrapForm.operator,
      remark: scrapForm.remark,
    })
    ElMessage.success('报废成功')
    scrapVisible.value = false
    fetchList()
    fetchStats()
  } catch {
    ElMessage.error('报废失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  fetchList()
  fetchStats()
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
</style>
