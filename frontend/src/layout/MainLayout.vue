<template>
  <el-container class="main-layout">
    <el-header class="header">
      <div class="header-left">
        <el-icon :size="24"><SetUp /></el-icon>
        <span class="title">注塑车间 · 定位块工装资产登记系统</span>
      </div>
      <div class="header-right">
        <div class="latest-batch-entry" v-if="latestBatch" @click="goToLatestBatch">
          <el-icon class="batch-icon"><DocumentChecked /></el-icon>
          <div class="batch-info">
            <div class="batch-month">{{ latestBatch.batchMonth }} 清点</div>
            <div class="batch-meta">
              <el-tag size="small" :type="batchStatusTagType(latestBatch.status)" effect="dark">
                {{ latestBatch.statusDescription }}
              </el-tag>
              <span class="batch-diff" v-if="latestBatch.pendingDiffCount > 0">
                <el-icon><Warning /></el-icon>
                {{ latestBatch.pendingDiffCount }} 项待处理
              </span>
            </div>
          </div>
        </div>
        <el-tag type="info" size="small">单车间独立数据</el-tag>
      </div>
    </el-header>
    <el-container>
      <el-aside width="200px" class="aside">
        <el-menu
          :default-active="activeMenu"
          router
          class="side-menu"
          background-color="#1d1e1f"
          text-color="#bfcbd9"
          active-text-color="#409eff"
        >
          <el-menu-item index="/assets">
            <el-icon><Box /></el-icon>
            <span>工装台账</span>
          </el-menu-item>
          <el-menu-item index="/board">
            <el-icon><Grid /></el-icon>
            <span>工位看板</span>
          </el-menu-item>
          <el-menu-item index="/floor-plan">
            <el-icon><Position /></el-icon>
            <span>平面看板</span>
          </el-menu-item>
          <el-menu-item index="/transfer">
            <el-icon><Switch /></el-icon>
            <span>移位登记</span>
          </el-menu-item>
          <el-menu-item index="/inventory">
            <el-icon><Checked /></el-icon>
            <span>实物清点</span>
          </el-menu-item>
          <el-menu-item index="/inventory-workbench">
            <el-icon><DocumentChecked /></el-icon>
            <span>清点工作台</span>
          </el-menu-item>
          <el-menu-item index="/image-precheck">
            <el-icon><Picture /></el-icon>
            <span>图片预检</span>
          </el-menu-item>
          <el-menu-item index="/inventory-diff-closure">
            <el-icon><Operation /></el-icon>
            <span>差异闭环</span>
          </el-menu-item>
          <el-menu-item index="/scrap">
            <el-icon><Delete /></el-icon>
            <span>报废归档</span>
          </el-menu-item>
          <el-menu-item index="/trace">
            <el-icon><Clock /></el-icon>
            <span>轨迹履历</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Box, Switch, Checked, Delete, SetUp, Clock, Grid, Position, Operation, DocumentChecked, Picture, Warning } from '@element-plus/icons-vue'
import { getLatestInventoryBatchSummary } from '../api/tooling'

const route = useRoute()
const router = useRouter()
const activeMenu = computed(() => route.path)

const latestBatch = ref(null)

const batchStatusTagType = (status) => {
  const map = {
    DRAFT: 'info',
    FROZEN: 'warning',
    CLOSED: 'success',
  }
  return map[status] || 'info'
}

const fetchLatestBatch = async () => {
  try {
    const res = await getLatestInventoryBatchSummary()
    latestBatch.value = res.data || null
  } catch {
    latestBatch.value = null
  }
}

const goToLatestBatch = () => {
  if (!latestBatch.value) return
  if (latestBatch.value.pendingDiffCount > 0) {
    router.push('/inventory-diff-closure')
  } else {
    router.push({ path: '/inventory-workbench', query: { month: latestBatch.value.batchMonth } })
  }
}

onMounted(() => {
  fetchLatestBatch()
})
</script>

<style scoped>
.main-layout {
  height: 100vh;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  color: #fff;
  padding: 0 20px;
  height: 56px;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}
.title {
  font-size: 17px;
  font-weight: 600;
  letter-spacing: 1px;
}
.aside {
  background-color: #1d1e1f;
}
.side-menu {
  border-right: none;
}
.main-content {
  background: #f0f2f5;
  padding: 20px;
  overflow-y: auto;
}

.latest-batch-entry {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 14px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.latest-batch-entry:hover {
  background: rgba(255, 255, 255, 0.18);
  border-color: rgba(64, 158, 255, 0.6);
}

.batch-icon {
  color: #409eff;
  font-size: 20px;
}

.batch-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.batch-month {
  font-size: 13px;
  font-weight: 600;
  color: #fff;
}

.batch-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.batch-diff {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 11px;
  color: #e6a23c;
}

.batch-diff .el-icon {
  font-size: 12px;
}
</style>
