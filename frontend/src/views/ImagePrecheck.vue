<template>
  <div class="image-precheck">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">
          <el-icon :size="22"><Picture /></el-icon>
          工装图片批量预检
        </h2>
        <span class="page-desc">导入多张定位块照片，自动压缩并预检，标记超限图片，可快速进入建档</span>
      </div>
      <div class="header-actions">
        <el-button :icon="Delete" @click="clearAll" :disabled="!imageList.length">清空全部</el-button>
      </div>
    </div>

    <el-upload
      class="upload-area"
      drag
      multiple
      :auto-upload="false"
      :show-file-list="false"
      accept="image/*"
      :on-change="handleFileChange"
    >
      <el-icon class="upload-icon"><UploadFilled /></el-icon>
      <div class="el-upload__text">
        将图片拖到此处，或<em>点击选择</em>
      </div>
      <template #tip>
        <div class="upload-tip">
          <el-icon><InfoFilled /></el-icon>
          支持 JPG/PNG/GIF/WEBP/BMP 格式，单张最大 10MB，支持多选
        </div>
      </template>
    </el-upload>

    <div v-if="imageList.length" class="result-area">
      <div class="result-stats">
        <div class="stat-item">
          <span class="stat-label">已导入</span>
          <span class="stat-value stat-total">{{ imageList.length }}</span>
          <span class="stat-unit">张</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">压缩前总大小</span>
          <span class="stat-value">{{ formatSize(totalOriginalSize) }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">压缩后总大小</span>
          <span class="stat-value stat-compressed">{{ formatSize(totalCompressedSize) }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">节省空间</span>
          <span class="stat-value stat-saved">{{ compressionRate }}%</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">超限图片</span>
          <span class="stat-value stat-warning">{{ overLimitCount }}</span>
          <span class="stat-unit">张</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">正常图片</span>
          <span class="stat-value stat-ok">{{ okCount }}</span>
          <span class="stat-unit">张</span>
        </div>
      </div>

      <el-table
        :data="imageList"
        border
        stripe
        class="result-table"
      >
        <el-table-column type="index" label="#" width="50" align="center" />
        <el-table-column label="预览图" width="120" align="center">
          <template #default="{ row }">
            <div class="preview-cell">
              <el-image
                :src="row.compressedUrl"
                :preview-src-list="[row.compressedUrl]"
                fit="cover"
                class="preview-img"
              />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="文件名" min-width="180">
          <template #default="{ row }">
            <div class="file-name-cell">
              <span :title="row.fileName">{{ row.fileName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="原始大小" width="110" align="center">
          <template #default="{ row }">
            <span class="size-text">{{ formatSize(row.originalSize) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="压缩后大小" width="120" align="center">
          <template #default="{ row }">
            <span class="size-text size-compressed">{{ formatSize(row.compressedSize) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="原始尺寸" width="120" align="center">
          <template #default="{ row }">
            <span v-if="row.originalWidth && row.originalHeight">
              {{ row.originalWidth }} × {{ row.originalHeight }}
            </span>
            <span v-else class="dim-placeholder">-</span>
          </template>
        </el-table-column>
        <el-table-column label="压缩后尺寸" width="130" align="center">
          <template #default="{ row }">
            <span v-if="row.compressedWidth && row.compressedHeight" class="dim-compressed">
              {{ row.compressedWidth }} × {{ row.compressedHeight }}
            </span>
            <span v-else class="dim-placeholder">-</span>
          </template>
        </el-table-column>
        <el-table-column label="压缩率" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.compressionRate !== undefined" :type="row.compressionRate >= 50 ? 'success' : (row.compressionRate >= 20 ? 'warning' : 'info')" size="small">
              {{ row.compressionRate }}%
            </el-tag>
            <span v-else class="dim-placeholder">-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.overLimit" type="danger" size="small" effect="dark">
              <el-icon style="margin-right: 2px"><Warning /></el-icon>
              超限
            </el-tag>
            <el-tag v-else type="success" size="small" effect="dark">
              <el-icon style="margin-right: 2px"><CircleCheck /></el-icon>
              正常
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="超限详情" min-width="160">
          <template #default="{ row }">
            <div v-if="row.overLimitReasons && row.overLimitReasons.length" class="reasons">
              <el-tag
                v-for="(reason, idx) in row.overLimitReasons" :key="idx" type="danger" size="small" effect="plain" style="margin-right: 4px; margin-bottom: 4px;">
                {{ reason }}
              </el-tag>
            </div>
            <span v-else class="ok-text">
              <el-icon><CircleCheck /></el-icon>
              符合要求
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" align="center" fixed="right">
          <template #default="{ row, $index }">
            <el-button type="primary" size="small" :icon="Edit" @click="goToRegister(row)">建档</el-button>
            <el-button type="info" size="small" :icon="ZoomIn" @click="previewImage(row)">查看</el-button>
            <el-button type="danger" size="small" :icon="Delete" @click="removeItem($index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-empty v-else class="empty-state" description="暂无图片，请点击上方区域导入图片">
      <el-icon :size="60"><Picture /></el-icon>
    </el-empty>

    <el-dialog v-model="previewVisible" title="图片详情" width="720px">
      <div v-if="currentPreview" class="preview-dialog">
        <div class="preview-compare">
          <div class="compare-col">
            <div class="compare-title">原图</div>
            <el-image :src="currentPreview.originalUrl" fit="contain" class="compare-img" />
            <div class="compare-info">
              <div class="compare-row">
                <span class="compare-label">大小</span>
                <span class="compare-value">{{ formatSize(currentPreview.originalSize) }}</span>
              </div>
              <div class="compare-row">
                <span class="compare-label">尺寸</span>
                <span class="compare-value">{{ currentPreview.originalWidth }} × {{ currentPreview.originalHeight }}</span>
              </div>
            </div>
          </div>
          <div class="compare-divider">
            <el-icon :size="24"><Switch /></el-icon>
          </div>
          <div class="compare-col">
            <div class="compare-title">压缩后</div>
            <el-image :src="currentPreview.compressedUrl" fit="contain" class="compare-img" />
            <div class="compare-info">
              <div class="compare-row">
                <span class="compare-label">大小</span>
                <span class="compare-value">{{ formatSize(currentPreview.compressedSize) }}</span>
              </div>
              <div class="compare-row">
                <span class="compare-label">尺寸</span>
                <span class="compare-value">{{ currentPreview.compressedWidth }} × {{ currentPreview.compressedHeight }}</span>
              </div>
              <div class="compare-row">
                <span class="compare-label">压缩率</span>
                <span class="compare-value compare-highlight">{{ currentPreview.compressionRate }}%</span>
              </div>
            </div>
          </div>
        </div>
        <div v-if="currentPreview.overLimit" class="preview-warnings">
          <div class="warning-title">
            <el-icon type="warning"><Warning /></el-icon>
            超限提示
          </div>
          <ul class="warning-list">
            <li v-for="(r, i) in currentPreview.overLimitReasons" :key="i">{{ r }}</li>
          </ul>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  UploadFilled, Picture, Delete, InfoFilled, Warning, CircleCheck, Edit, ZoomIn, Switch } from '@element-plus/icons-vue'
import { compressImage, validateImageFile } from '../utils/compress'
import { uploadFile } from '../api/tooling'

const router = useRouter()

const MAX_COMPRESSED_SIZE_LIMIT = 512 * 1024
const MAX_DIMENSION_LIMIT = 800

const imageList = ref([])
const previewVisible = ref(false)
const currentPreview = ref(null)

const totalOriginalSize = computed(() => imageList.value.reduce((sum, item) => sum + (item.originalSize || 0), 0))
const totalCompressedSize = computed(() => imageList.value.reduce((sum, item) => sum + (item.compressedSize || 0), 0))
const compressionRate = computed(() => {
  if (totalOriginalSize.value === 0) return 0
  return Math.round(((totalOriginalSize.value - totalCompressedSize.value) / totalOriginalSize.value) * 100)
})
const overLimitCount = computed(() => imageList.value.filter((item) => item.overLimit).length)
const okCount = computed(() => imageList.value.filter((item) => !item.overLimit).length)

const formatSize = (bytes) => {
  if (bytes === undefined || bytes === null || bytes === 0) return '0 B'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(2) + ' MB'
}

const getImageDimensions = (file) => {
  return new Promise((resolve) => {
    const img = new Image()
    img.onload = () => {
      resolve({ width: img.width, height: img.height })
    }
    img.onerror = () => {
      resolve({ width: 0, height: 0 })
    }
    img.src = URL.createObjectURL(file)
  })
}

const checkOverLimit = (item) => {
  const reasons = []
  if (item.compressedSize > MAX_COMPRESSED_SIZE_LIMIT) {
    reasons.push('压缩后超过 512KB')
  }
  if (item.compressedWidth > MAX_DIMENSION_LIMIT || item.compressedHeight > MAX_DIMENSION_LIMIT) {
    reasons.push('尺寸超过 ' + MAX_DIMENSION_LIMIT + 'px')
  }
  item.overLimit = reasons.length > 0
  item.overLimitReasons = reasons
}

const handleFileChange = async (uploadFile) => {
  const rawFile = uploadFile.raw || uploadFile
  if (!rawFile) return

  const validation = validateImageFile(rawFile)
  if (!validation.valid) {
    ElMessage.error(rawFile.name + ': ' + validation.message)
    return
  }

  const exists = imageList.value.some((item) =>
    item.fileName === rawFile.name && item.originalSize === rawFile.size
  )
  if (exists) {
    ElMessage.warning(rawFile.name + ' 已存在，跳过重复导入')
    return
  }

  const item = reactive({
    id: Date.now() + Math.random(),
    fileName: rawFile.name,
    originalFile: rawFile,
    originalSize: rawFile.size,
    originalUrl: URL.createObjectURL(rawFile),
    originalWidth: 0,
    originalHeight: 0,
    compressedFile: null,
    compressedSize: 0,
    compressedUrl: '',
    compressedWidth: 0,
    compressedHeight: 0,
    compressionRate: 0,
    overLimit: false,
    overLimitReasons: [],
  })

  imageList.value.push(item)

  ;(async () => {
    try {
      const originalDims = await getImageDimensions(rawFile)
      item.originalWidth = originalDims.width
      item.originalHeight = originalDims.height

      const compressed = await compressImage(rawFile)
      item.compressedFile = compressed
      item.compressedSize = compressed.size
      item.compressedUrl = URL.createObjectURL(compressed)

      const compressedDims = await getImageDimensions(compressed)
      item.compressedWidth = compressedDims.width
      item.compressedHeight = compressedDims.height

      if (item.originalSize > 0) {
        item.compressionRate = Math.round(
          ((item.originalSize - item.compressedSize) / item.originalSize) * 100
        )
      }

      checkOverLimit(item)
    } catch (err) {
      console.error('处理图片失败:', err)
      ElMessage.error(rawFile.name + ': 处理失败')
    }
  })();
}

const removeItem = (index) => {
  const item = imageList.value[index]
  if (item.originalUrl) URL.revokeObjectURL(item.originalUrl)
  if (item.compressedUrl) URL.revokeObjectURL(item.compressedUrl)
  imageList.value.splice(index, 1)
}

const clearAll = async () => {
  try {
    await ElMessageBox.confirm('确认清空所有图片？', '清空确认', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消',
    })
    imageList.value.forEach((item) => {
      if (item.originalUrl) URL.revokeObjectURL(item.originalUrl)
      if (item.compressedUrl) URL.revokeObjectURL(item.compressedUrl)
    })
    imageList.value = []
  } catch {
    /* cancelled */
  }
}

const previewImage = (row) => {
  currentPreview.value = row
  previewVisible.value = true
}

const goToRegister = async (row) => {
  router.push({
    path: '/assets',
    query: {
      precheckImage: row.compressedUrl,
      precheckFileName: row.fileName,
    },
  })
}
</script>

<style scoped>
.image-precheck {
  min-height: 100%;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.page-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.page-title .el-icon {
  color: #409eff;
}

.page-desc {
  font-size: 13px;
  color: #909399;
}

.upload-area {
  margin-bottom: 24px;
}

.upload-icon {
  font-size: 67px;
  color: #c0c4cc;
  margin: 20px 0 16px;
}

.upload-tip {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #909399;
  font-size: 12px;
}

.result-area {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.result-stats {
  display: flex;
  gap: 32px;
  padding: 16px 20px;
  background: linear-gradient(135deg, #f5f7fa 0%, #eef2f8 100%);
  border-radius: 8px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.stat-item {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.stat-label {
  font-size: 13px;
  color: #606266;
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
}

.stat-total {
  color: #409eff;
}

.stat-compressed {
  color: #67c23a;
}

.stat-saved {
  color: #e6a23c;
}

.stat-warning {
  color: #f56c6c;
}

.stat-ok {
  color: #67c23a;
}

.stat-unit {
  font-size: 13px;
  color: #909399;
}

.result-table {
  width: 100%;
}

.preview-cell {
  display: flex;
  justify-content: center;
}

.preview-img {
  width: 80px;
  height: 80px;
  border-radius: 4px;
  border: 1px solid #ebeef5;
}

.file-name-cell {
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  color: #606266;
}

.size-text {
  font-size: 13px;
  color: #606266;
}

.size-compressed {
  color: #67c23a;
  font-weight: 600;
}

.dim-placeholder {
  color: #c0c4cc;
  font-size: 13px;
}

.dim-compressed {
  color: #409eff;
}

.ok-text {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #67c23a;
  font-size: 13px;
}

.empty-state {
  padding: 80px 0;
}

.empty-state :deep(.el-empty__description) {
  font-size: 14px;
  color: #909399;
}

.preview-dialog {
  padding: 10px 0;
}

.preview-compare {
  display: flex;
  align-items: stretch;
  gap: 16px;
}

.compare-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #f7f9fc;
  border-radius: 8px;
  padding: 16px;
}

.compare-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
  text-align: center;
}

.compare-img {
  width: 100%;
  height: 240px;
  background: #fff;
  border-radius: 6px;
  margin-bottom: 12px;
  border: 1px solid #ebeef5;
}

.compare-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.compare-row {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
}

.compare-label {
  color: #909399;
}

.compare-value {
  color: #303133;
  font-weight: 500;
}

.compare-highlight {
  color: #67c23a;
  font-weight: 600;
}

.compare-divider {
  display: flex;
  align-items: center;
  color: #c0c4cc;
}

.preview-warnings {
  margin-top: 20px;
  padding: 16px;
  background: #fef0f0;
  border: 1px solid #fbc4c4;
  border-radius: 8px;
}

.warning-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  color: #f56c6c;
  margin-bottom: 10px;
}

.warning-list {
  margin: 0;
  padding-left: 20px;
  color: #f56c6c;
  font-size: 13px;
  line-height: 1.8;
}
</style>
