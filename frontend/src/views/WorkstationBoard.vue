<template>
  <div class="workstation-board">
    <div class="toolbar">
      <div class="stats-bar">
        <div class="stat-item stat-in-use">
          <i class="dot"></i>
          <span class="stat-label">在用</span>
          <span class="stat-value">{{ totals.inUse }}</span>
        </div>
        <div class="stat-item stat-transferred">
          <i class="dot"></i>
          <span class="stat-label">移位</span>
          <span class="stat-value">{{ totals.transferred }}</span>
        </div>
        <div class="stat-item stat-scrapped">
          <i class="dot"></i>
          <span class="stat-label">报废</span>
          <span class="stat-value">{{ totals.scrapped }}</span>
        </div>
        <div class="stat-item stat-total">
          <span class="stat-label">定位块总数</span>
          <span class="stat-value">{{ totals.total }}</span>
        </div>
      </div>
      <el-button :icon="Refresh" :loading="loading" @click="fetchAssets">刷新</el-button>
    </div>

    <div class="legend">
      <span class="legend-item"><i class="dot dot-in-use"></i>在用</span>
      <span class="legend-item"><i class="dot dot-transferred"></i>移位</span>
      <span class="legend-item"><i class="dot dot-scrapped"></i>报废</span>
      <span class="legend-hint">点击工位卡片可展开该区域定位块清单</span>
    </div>

    <div class="floor-plan" v-loading="loading">
      <div
        v-for="area in floorAreas"
        :key="area.key"
        class="area"
        :class="`area-${area.key}`"
      >
        <div class="area-header">
          <span class="area-name">{{ area.name }}</span>
          <span class="area-count">共 {{ area.total }} 块</span>
        </div>
        <div class="ws-grid">
          <div
            v-for="ws in area.workstations"
            :key="ws.name"
            class="ws-block"
            :class="{ 'is-active': selectedWorkstation === ws.name, 'is-empty': ws.total === 0 }"
            @click="toggleWorkstation(ws.name)"
          >
            <div class="ws-head">
              <span class="ws-name">{{ ws.name }}</span>
              <span class="ws-total">共 {{ ws.total }}</span>
            </div>
            <div class="ws-cbs">
              <div class="cb cb-in-use">
                <span class="cb-num">{{ ws.inUse }}</span>
                <span class="cb-label">在用</span>
              </div>
              <div class="cb cb-transferred">
                <span class="cb-num">{{ ws.transferred }}</span>
                <span class="cb-label">移位</span>
              </div>
              <div class="cb cb-scrapped">
                <span class="cb-num">{{ ws.scrapped }}</span>
                <span class="cb-label">报废</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <transition name="board-expand">
      <div v-if="selectedWorkstation" class="detail-panel">
        <div class="detail-header">
          <div class="detail-title">
            <el-icon :size="18"><Location /></el-icon>
            <span>{{ selectedWorkstation }}</span>
            <el-tag size="small" type="info">定位块清单</el-tag>
            <el-tag size="small">共 {{ selectedAssets.length }} 块</el-tag>
          </div>
          <el-button :icon="Close" text @click="selectedWorkstation = ''">收起</el-button>
        </div>
        <el-row :gutter="16" class="card-grid">
          <el-col v-for="item in selectedAssets" :key="item.id" :xl="4" :lg="6" :md="8" :sm="12">
            <el-card class="locator-card" shadow="hover">
              <div class="card-img-wrapper">
                <img
                  v-if="item.imageUrl"
                  :src="'/api/file/' + item.imageUrl"
                  class="card-img"
                  alt=""
                />
                <div v-else class="card-img-placeholder">
                  <el-icon :size="32"><Picture /></el-icon>
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
                  <span class="info-value">{{ item.productName || '-' }}</span>
                </div>
                <div class="card-info">
                  <span class="info-label">入库日期</span>
                  <span class="info-value">{{ item.entryDate || '-' }}</span>
                </div>
              </div>
              <div class="card-actions">
                <el-button size="small" type="primary" :icon="Clock" @click="viewTrace(item)">轨迹</el-button>
              </div>
            </el-card>
          </el-col>
        </el-row>
        <el-empty v-if="!selectedAssets.length" description="该工位暂无定位块" :image-size="80" />
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Refresh, Location, Picture, Clock, Close } from '@element-plus/icons-vue'
import { listAssets } from '../api/tooling'

const router = useRouter()

const loading = ref(false)
const assets = ref([])
const selectedWorkstation = ref('')

const FLOOR_PLAN = [
  {
    key: 'injection',
    name: '注塑机区',
    workstations: [
      '注塑机01', '注塑机02', '注塑机03', '注塑机04',
      '注塑机05', '注塑机06', '注塑机07', '注塑机08',
    ],
  },
  {
    key: 'mold',
    name: '模具库',
    workstations: ['模具库A区', '模具库B区'],
  },
  {
    key: 'inspection',
    name: '待检区',
    workstations: ['待检区'],
  },
  {
    key: 'repair',
    name: '维修区',
    workstations: ['维修区'],
  },
]

const statusLabel = (status) => {
  const map = { IN_USE: '在用', TRANSFERRED: '已移位', SCRAPPED: '已报废' }
  return map[status] || status || '-'
}

const statusTagType = (status) => {
  const map = { IN_USE: 'success', TRANSFERRED: 'warning', SCRAPPED: 'danger' }
  return map[status] || 'info'
}

const wsStats = computed(() => {
  const map = {}
  assets.value.forEach((a) => {
    const ws = a.workstation
    if (!ws) return
    if (!map[ws]) map[ws] = { inUse: 0, transferred: 0, scrapped: 0, total: 0 }
    if (a.status === 'IN_USE') map[ws].inUse++
    else if (a.status === 'TRANSFERRED') map[ws].transferred++
    else if (a.status === 'SCRAPPED') map[ws].scrapped++
    map[ws].total++
  })
  return map
})

const floorAreas = computed(() =>
  FLOOR_PLAN.map((area) => {
    const workstations = area.workstations.map((ws) => {
      const s = wsStats.value[ws] || { inUse: 0, transferred: 0, scrapped: 0, total: 0 }
      return { name: ws, ...s }
    })
    const total = workstations.reduce((sum, ws) => sum + ws.total, 0)
    return { ...area, workstations, total }
  })
)

const totals = computed(() => {
  const t = { inUse: 0, transferred: 0, scrapped: 0, total: 0 }
  assets.value.forEach((a) => {
    if (a.status === 'IN_USE') t.inUse++
    else if (a.status === 'TRANSFERRED') t.transferred++
    else if (a.status === 'SCRAPPED') t.scrapped++
    t.total++
  })
  return t
})

const selectedAssets = computed(() => {
  if (!selectedWorkstation.value) return []
  return assets.value.filter((a) => a.workstation === selectedWorkstation.value)
})

const toggleWorkstation = (ws) => {
  selectedWorkstation.value = selectedWorkstation.value === ws ? '' : ws
}

const viewTrace = (item) => {
  router.push({ path: '/trace', query: { code: item.toolingCode } })
}

const fetchAssets = async () => {
  loading.value = true
  try {
    const res = await listAssets({})
    assets.value = res.data || []
  } catch {
    ElMessage.error('获取工位分布失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchAssets()
})
</script>

<style scoped>
.workstation-board {
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

.stats-bar {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 22px;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.stat-item .dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  display: inline-block;
}

.stat-in-use .dot {
  background: #67c23a;
}

.stat-transferred .dot {
  background: #e6a23c;
}

.stat-scrapped .dot {
  background: #f56c6c;
}

.stat-total .dot {
  background: #409eff;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
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

.legend {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 16px;
  padding: 10px 16px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  flex-wrap: wrap;
}

.legend-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #606266;
}

.legend-item .dot {
  width: 12px;
  height: 12px;
  border-radius: 3px;
  display: inline-block;
}

.dot-in-use {
  background: #67c23a;
}

.dot-transferred {
  background: #e6a23c;
}

.dot-scrapped {
  background: #f56c6c;
}

.legend-hint {
  margin-left: auto;
  font-size: 12px;
  color: #909399;
}

.floor-plan {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  grid-template-areas:
    "injection injection"
    "mold inspection"
    "repair repair";
  gap: 16px;
  margin-bottom: 16px;
}

.area {
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  padding: 16px;
  border: 1px solid #f0f0f0;
}

.area-injection {
  grid-area: injection;
}

.area-mold {
  grid-area: mold;
}

.area-inspection {
  grid-area: inspection;
}

.area-repair {
  grid-area: repair;
}

.area-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
  padding-bottom: 10px;
  border-bottom: 1px solid #f5f5f5;
}

.area-name {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  position: relative;
  padding-left: 10px;
}

.area-name::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 14px;
  background: #409eff;
  border-radius: 2px;
}

.area-count {
  font-size: 12px;
  color: #909399;
}

.ws-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 12px;
}

.ws-block {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 12px;
  cursor: pointer;
  transition: all 0.25s ease;
  background: #fff;
  user-select: none;
}

.ws-block:hover {
  border-color: #409eff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
  transform: translateY(-2px);
}

.ws-block.is-active {
  border-color: #409eff;
  background: #ecf5ff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.2);
}

.ws-block.is-empty {
  opacity: 0.65;
}

.ws-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.ws-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.ws-total {
  font-size: 12px;
  color: #909399;
}

.ws-cbs {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 6px;
}

.cb {
  border-radius: 6px;
  padding: 8px 4px;
  text-align: center;
  color: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  min-height: 48px;
  justify-content: center;
}

.cb-in-use {
  background: #67c23a;
}

.cb-transferred {
  background: #e6a23c;
}

.cb-scrapped {
  background: #f56c6c;
}

.cb-num {
  font-size: 18px;
  font-weight: 700;
  line-height: 1;
}

.cb-label {
  font-size: 11px;
  opacity: 0.9;
}

.detail-panel {
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  padding: 18px 20px;
  border: 1px solid #f0f0f0;
}

.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f5f5f5;
}

.detail-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.card-grid {
  margin-top: 0;
}

.locator-card {
  margin-bottom: 16px;
  border-radius: 8px;
  overflow: hidden;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}

.locator-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.locator-card :deep(.el-card__body) {
  padding: 0;
}

.card-img-wrapper {
  position: relative;
  width: 100%;
  height: 150px;
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
  padding: 12px 14px 8px;
}

.card-title {
  font-size: 15px;
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
}

.card-actions .el-button {
  flex: 1;
}

.board-expand-enter-active,
.board-expand-leave-active {
  transition: all 0.3s ease;
  overflow: hidden;
}

.board-expand-enter-from,
.board-expand-leave-to {
  opacity: 0;
  transform: translateY(-12px);
  max-height: 0;
}

.board-expand-enter-to,
.board-expand-leave-from {
  opacity: 1;
  max-height: 2000px;
}
</style>
