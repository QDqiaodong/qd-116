<template>
  <div class="floor-plan-board">
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
      <span class="legend-hint">点击工位色块可展开该区域定位块清单</span>
    </div>

    <div class="floor-container" v-loading="loading">
      <div class="floor-plan">
        <div class="zone zone-injection">
          <div class="zone-header">
            <span class="zone-title">注塑机区</span>
            <span class="zone-count">共 {{ injectionTotal }} 块</span>
          </div>
          <div class="injection-grid">
            <div
              v-for="ws in injectionMachines"
              :key="ws.name"
              class="machine-cell"
              :class="{ 'is-active': selectedWorkstation === ws.name, 'is-empty': ws.total === 0 }"
              @click="toggleWorkstation(ws.name)"
            >
              <div class="machine-label">{{ ws.name }}</div>
              <div class="machine-stats">
                <div class="stat-bar">
                  <div
                    class="bar-segment bar-in-use"
                    :style="{ width: getBarWidth(ws.inUse, ws.total) }"
                  ></div>
                  <div
                    class="bar-segment bar-transferred"
                    :style="{ width: getBarWidth(ws.transferred, ws.total) }"
                  ></div>
                  <div
                    class="bar-segment bar-scrapped"
                    :style="{ width: getBarWidth(ws.scrapped, ws.total) }"
                  ></div>
                </div>
                <div class="stat-numbers">
                  <span class="num num-in-use">{{ ws.inUse }}</span>
                  <span class="num num-transferred">{{ ws.transferred }}</span>
                  <span class="num num-scrapped">{{ ws.scrapped }}</span>
                </div>
              </div>
              <div class="machine-total">共 {{ ws.total }} 块</div>
            </div>
          </div>
        </div>

        <div class="zone-row">
          <div class="zone zone-mold">
            <div class="zone-header">
              <span class="zone-title">模具库</span>
              <span class="zone-count">共 {{ moldTotal }} 块</span>
            </div>
            <div class="zone-content">
              <div
                v-for="ws in moldAreas"
                :key="ws.name"
                class="zone-block"
                :class="{ 'is-active': selectedWorkstation === ws.name, 'is-empty': ws.total === 0 }"
                @click="toggleWorkstation(ws.name)"
              >
                <div class="block-label">{{ ws.name }}</div>
                <div class="block-color-bar">
                  <div
                    class="color-segment color-in-use"
                    :style="{ width: getBarWidth(ws.inUse, ws.total) }"
                  ></div>
                  <div
                    class="color-segment color-transferred"
                    :style="{ width: getBarWidth(ws.transferred, ws.total) }"
                  ></div>
                  <div
                    class="color-segment color-scrapped"
                    :style="{ width: getBarWidth(ws.scrapped, ws.total) }"
                  ></div>
                </div>
                <div class="block-stats">
                  <span class="stat-pill pill-in-use">在用 {{ ws.inUse }}</span>
                  <span class="stat-pill pill-transferred">移位 {{ ws.transferred }}</span>
                  <span class="stat-pill pill-scrapped">报废 {{ ws.scrapped }}</span>
                </div>
              </div>
            </div>
          </div>

          <div class="zone zone-inspection">
            <div class="zone-header">
              <span class="zone-title">待检区</span>
              <span class="zone-count">共 {{ inspectionTotal }} 块</span>
            </div>
            <div class="zone-content">
              <div
                v-for="ws in inspectionAreas"
                :key="ws.name"
                class="zone-block block-large"
                :class="{ 'is-active': selectedWorkstation === ws.name, 'is-empty': ws.total === 0 }"
                @click="toggleWorkstation(ws.name)"
              >
                <div class="block-label">{{ ws.name }}</div>
                <div class="block-color-bar">
                  <div
                    class="color-segment color-in-use"
                    :style="{ width: getBarWidth(ws.inUse, ws.total) }"
                  ></div>
                  <div
                    class="color-segment color-transferred"
                    :style="{ width: getBarWidth(ws.transferred, ws.total) }"
                  ></div>
                  <div
                    class="color-segment color-scrapped"
                    :style="{ width: getBarWidth(ws.scrapped, ws.total) }"
                  ></div>
                </div>
                <div class="block-stats">
                  <span class="stat-pill pill-in-use">在用 {{ ws.inUse }}</span>
                  <span class="stat-pill pill-transferred">移位 {{ ws.transferred }}</span>
                  <span class="stat-pill pill-scrapped">报废 {{ ws.scrapped }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="zone zone-repair">
          <div class="zone-header">
            <span class="zone-title">维修区</span>
            <span class="zone-count">共 {{ repairTotal }} 块</span>
          </div>
          <div class="zone-content">
            <div
              v-for="ws in repairAreas"
              :key="ws.name"
              class="zone-block block-wide"
              :class="{ 'is-active': selectedWorkstation === ws.name, 'is-empty': ws.total === 0 }"
              @click="toggleWorkstation(ws.name)"
            >
              <div class="block-label">{{ ws.name }}</div>
              <div class="block-color-bar">
                <div
                  class="color-segment color-in-use"
                  :style="{ width: getBarWidth(ws.inUse, ws.total) }"
                ></div>
                <div
                  class="color-segment color-transferred"
                  :style="{ width: getBarWidth(ws.transferred, ws.total) }"
                ></div>
                <div
                  class="color-segment color-scrapped"
                  :style="{ width: getBarWidth(ws.scrapped, ws.total) }"
                ></div>
              </div>
              <div class="block-stats">
                <span class="stat-pill pill-in-use">在用 {{ ws.inUse }}</span>
                <span class="stat-pill pill-transferred">移位 {{ ws.transferred }}</span>
                <span class="stat-pill pill-scrapped">报废 {{ ws.scrapped }}</span>
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
          <el-col v-for="item in selectedAssets" :key="item.id" :xl="3" :lg="4" :md="6" :sm="8" :xs="12">
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

const INJECTION_MACHINES = [
  '注塑机01', '注塑机02', '注塑机03', '注塑机04',
  '注塑机05', '注塑机06', '注塑机07', '注塑机08',
]

const MOLD_AREAS = ['模具库A区', '模具库B区']
const INSPECTION_AREAS = ['待检区']
const REPAIR_AREAS = ['维修区']

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

const getWsStats = (name) => {
  return wsStats.value[name] || { inUse: 0, transferred: 0, scrapped: 0, total: 0 }
}

const injectionMachines = computed(() =>
  INJECTION_MACHINES.map((name) => ({ name, ...getWsStats(name) }))
)

const moldAreas = computed(() =>
  MOLD_AREAS.map((name) => ({ name, ...getWsStats(name) }))
)

const inspectionAreas = computed(() =>
  INSPECTION_AREAS.map((name) => ({ name, ...getWsStats(name) }))
)

const repairAreas = computed(() =>
  REPAIR_AREAS.map((name) => ({ name, ...getWsStats(name) }))
)

const injectionTotal = computed(() =>
  injectionMachines.value.reduce((sum, ws) => sum + ws.total, 0)
)

const moldTotal = computed(() =>
  moldAreas.value.reduce((sum, ws) => sum + ws.total, 0)
)

const inspectionTotal = computed(() =>
  inspectionAreas.value.reduce((sum, ws) => sum + ws.total, 0)
)

const repairTotal = computed(() =>
  repairAreas.value.reduce((sum, ws) => sum + ws.total, 0)
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

const getBarWidth = (value, total) => {
  if (total === 0) return '0%'
  return `${(value / total) * 100}%`
}

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
.floor-plan-board {
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

.floor-container {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  padding: 24px;
  margin-bottom: 16px;
  border: 1px solid #f0f0f0;
}

.floor-plan {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.zone {
  background: #fafafa;
  border-radius: 10px;
  padding: 16px;
  border: 1px solid #ebeef5;
}

.zone-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
  padding-bottom: 10px;
  border-bottom: 2px solid #e4e7ed;
}

.zone-title {
  font-size: 16px;
  font-weight: 700;
  color: #303133;
  position: relative;
  padding-left: 12px;
}

.zone-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 18px;
  background: linear-gradient(180deg, #409eff 0%, #66b1ff 100%);
  border-radius: 2px;
}

.zone-injection .zone-title::before {
  background: linear-gradient(180deg, #409eff 0%, #66b1ff 100%);
}

.zone-mold .zone-title::before {
  background: linear-gradient(180deg, #67c23a 0%, #85ce61 100%);
}

.zone-inspection .zone-title::before {
  background: linear-gradient(180deg, #e6a23c 0%, #ebb563 100%);
}

.zone-repair .zone-title::before {
  background: linear-gradient(180deg, #f56c6c 0%, #f78989 100%);
}

.zone-count {
  font-size: 13px;
  color: #909399;
  font-weight: 500;
}

.zone-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.injection-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.machine-cell {
  background: #fff;
  border: 2px solid #dcdfe6;
  border-radius: 8px;
  padding: 14px 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  text-align: center;
  position: relative;
  overflow: hidden;
}

.machine-cell::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #67c23a 0%, #e6a23c 50%, #f56c6c 100%);
  opacity: 0.3;
  transition: opacity 0.3s ease;
}

.machine-cell:hover {
  border-color: #409eff;
  box-shadow: 0 4px 16px rgba(64, 158, 255, 0.2);
  transform: translateY(-2px);
}

.machine-cell:hover::before {
  opacity: 0.6;
}

.machine-cell.is-active {
  border-color: #409eff;
  background: #ecf5ff;
  box-shadow: 0 4px 16px rgba(64, 158, 255, 0.25);
}

.machine-cell.is-active::before {
  opacity: 0.8;
  background: linear-gradient(90deg, #67c23a 0%, #409eff 50%, #f56c6c 100%);
}

.machine-cell.is-empty {
  opacity: 0.5;
}

.machine-label {
  font-size: 14px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 10px;
}

.machine-stats {
  margin-bottom: 8px;
}

.stat-bar {
  height: 8px;
  background: #f0f0f0;
  border-radius: 4px;
  overflow: hidden;
  display: flex;
  margin-bottom: 6px;
}

.bar-segment {
  height: 100%;
  transition: width 0.5s ease;
}

.bar-in-use {
  background: #67c23a;
}

.bar-transferred {
  background: #e6a23c;
}

.bar-scrapped {
  background: #f56c6c;
}

.stat-numbers {
  display: flex;
  justify-content: space-around;
  gap: 4px;
}

.stat-numbers .num {
  font-size: 12px;
  font-weight: 600;
}

.num-in-use {
  color: #67c23a;
}

.num-transferred {
  color: #e6a23c;
}

.num-scrapped {
  color: #f56c6c;
}

.machine-total {
  font-size: 12px;
  color: #909399;
  padding-top: 6px;
  border-top: 1px dashed #ebeef5;
}

.zone-content {
  display: flex;
  gap: 12px;
}

.zone-block {
  flex: 1;
  background: #fff;
  border: 2px solid #dcdfe6;
  border-radius: 8px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.zone-block:hover {
  border-color: #409eff;
  box-shadow: 0 4px 16px rgba(64, 158, 255, 0.2);
  transform: translateY(-2px);
}

.zone-block.is-active {
  border-color: #409eff;
  background: #ecf5ff;
  box-shadow: 0 4px 16px rgba(64, 158, 255, 0.25);
}

.zone-block.is-empty {
  opacity: 0.5;
}

.block-label {
  font-size: 15px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 12px;
}

.block-color-bar {
  height: 12px;
  background: #f0f0f0;
  border-radius: 6px;
  overflow: hidden;
  display: flex;
  margin-bottom: 12px;
}

.color-segment {
  height: 100%;
  transition: width 0.5s ease;
}

.color-in-use {
  background: #67c23a;
}

.color-transferred {
  background: #e6a23c;
}

.color-scrapped {
  background: #f56c6c;
}

.block-stats {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.stat-pill {
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

.pill-in-use {
  background: #f0f9eb;
  color: #67c23a;
}

.pill-transferred {
  background: #fdf6ec;
  color: #e6a23c;
}

.pill-scrapped {
  background: #fef0f0;
  color: #f56c6c;
}

.block-large {
  min-height: 100px;
}

.block-wide {
  min-height: 90px;
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
  height: 120px;
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
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-info {
  display: flex;
  align-items: center;
  font-size: 12px;
  line-height: 1.8;
}

.info-label {
  color: #909399;
  min-width: 56px;
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

@media (max-width: 1200px) {
  .injection-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}

@media (max-width: 900px) {
  .zone-row {
    grid-template-columns: 1fr;
  }

  .injection-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 600px) {
  .injection-grid {
    grid-template-columns: 1fr;
  }

  .zone-content {
    flex-direction: column;
  }
}
</style>
