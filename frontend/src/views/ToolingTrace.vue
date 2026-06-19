<template>
  <div class="tooling-trace">
    <div class="page-header">
      <div class="search-bar">
        <el-input
          v-model="searchCode"
          placeholder="请输入工装编号"
          clearable
          :prefix-icon="Search"
          @clear="handleClear"
          @keyup.enter="handleSearch"
          style="width: 280px"
        />
        <el-button type="primary" :icon="Search" @click="handleSearch" :loading="loading">
          查询轨迹
        </el-button>
      </div>
    </div>

    <el-empty v-if="!trace && !loading" description="请输入工装编号查询履历" />

    <div v-if="trace" class="trace-content">
      <el-card class="summary-card" shadow="never">
        <div class="summary">
          <div class="summary-main">
            <div class="code-row">
              <span class="code-label">工装编号</span>
              <span class="code-value">{{ trace.toolingCode }}</span>
              <el-tag
                :type="statusTagType(trace.currentStatus)"
                effect="dark"
                size="default"
              >{{ trace.currentStatusLabel }}</el-tag>
            </div>
            <div class="info-row">
              <span class="info-item">
                <span class="info-label">适配产品</span>
                <span class="info-value">{{ trace.productName }}</span>
              </span>
              <span class="info-item">
                <span class="info-label">当前工位</span>
                <span class="info-value">{{ trace.currentWorkstation || '-' }}</span>
              </span>
            </div>
          </div>
        </div>
      </el-card>

      <div class="timeline-wrapper">
        <div class="timeline-title">
          <el-icon :size="18"><Clock /></el-icon>
          <span>履历时间轴</span>
          <span class="event-count">共 {{ trace.events.length }} 条记录</span>
        </div>
        <el-timeline v-if="trace.events.length" class="trace-timeline">
          <el-timeline-item
            v-for="(event, idx) in trace.events"
            :key="idx"
            :timestamp="formatTime(event.eventTime)"
            :type="eventTypeColor(event.eventType)"
            :icon="eventTypeIcon(event.eventType)"
            placement="top"
          >
            <el-card class="event-card" shadow="hover">
              <div class="event-header">
                <el-tag :type="eventTypeColor(event.eventType)" effect="light" size="small">
                  {{ event.eventLabel }}
                </el-tag>
                <span class="event-time">{{ formatTime(event.eventTime) }}</span>
              </div>
              <div class="event-detail">
                <p class="detail-text">{{ event.detail }}</p>
                <div class="event-meta">
                  <span v-if="event.workstation" class="meta-item">
                    <el-icon><Location /></el-icon>
                    工位：{{ event.workstation }}
                  </span>
                  <span v-if="event.operator" class="meta-item">
                    <el-icon><User /></el-icon>
                    操作人：{{ event.operator }}
                  </span>
                </div>
                <div v-if="event.remark" class="event-remark">
                  <span class="remark-label">备注：</span>
                  <span class="remark-text">{{ event.remark }}</span>
                </div>
              </div>
            </el-card>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else description="暂无履历记录" :image-size="80" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, Clock, Location, User, Box, Switch, Delete, Warning } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { getToolingTrace } from '../api/tooling'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const searchCode = ref('')
const trace = ref(null)

const statusTagType = (status) => {
  const map = { IN_USE: 'success', TRANSFERRED: 'warning', SCRAPPED: 'danger' }
  return map[status] || 'info'
}

const eventTypeColor = (type) => {
  const map = { CREATE: 'success', TRANSFER: 'warning', INVENTORY: 'primary', SCRAP: 'danger' }
  return map[type] || 'info'
}

const eventTypeIcon = (type) => {
  const map = { CREATE: Box, TRANSFER: Switch, INVENTORY: Warning, SCRAP: Delete }
  return map[type] || Box
}

const formatTime = (t) => {
  if (!t) return '-'
  return dayjs(t).format('YYYY-MM-DD HH:mm:ss')
}

const handleSearch = async () => {
  const code = searchCode.value.trim()
  if (!code) {
    ElMessage.warning('请输入工装编号')
    return
  }
  loading.value = true
  try {
    const res = await getToolingTrace(code)
    trace.value = res.data
    router.replace({ query: { code } })
  } catch (e) {
    trace.value = null
    ElMessage.error(e?.response?.data?.message || '未找到该工装的履历信息')
  } finally {
    loading.value = false
  }
}

const handleClear = () => {
  trace.value = null
  router.replace({ query: {} })
}

onMounted(() => {
  const code = route.query.code
  if (code) {
    searchCode.value = code
    handleSearch()
  }
})
</script>

<style scoped>
.tooling-trace {
  min-height: 100%;
}

.page-header {
  margin-bottom: 16px;
}

.search-bar {
  display: flex;
  align-items: center;
  gap: 10px;
}

.trace-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.summary-card :deep(.el-card__body) {
  padding: 20px 24px;
}

.summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.summary-main {
  flex: 1;
}

.code-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.code-label {
  font-size: 14px;
  color: #909399;
}

.code-value {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
  letter-spacing: 1px;
}

.info-row {
  display: flex;
  align-items: center;
  gap: 36px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
}

.info-label {
  color: #909399;
}

.info-value {
  color: #303133;
  font-weight: 500;
}

.timeline-wrapper {
  background: #fff;
  border-radius: 8px;
  padding: 20px 24px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.timeline-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.event-count {
  margin-left: auto;
  font-size: 13px;
  font-weight: 400;
  color: #909399;
}

.trace-timeline {
  padding-top: 8px;
}

.event-card {
  border-radius: 6px;
}

.event-card :deep(.el-card__body) {
  padding: 14px 16px;
}

.event-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.event-time {
  font-size: 12px;
  color: #909399;
}

.detail-text {
  margin: 0 0 10px 0;
  font-size: 14px;
  color: #303133;
  line-height: 1.6;
}

.event-meta {
  display: flex;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #606266;
}

.event-remark {
  display: flex;
  align-items: flex-start;
  gap: 4px;
  font-size: 13px;
  padding-top: 8px;
  border-top: 1px dashed #f0f0f0;
  margin-top: 8px;
}

.remark-label {
  color: #909399;
  flex-shrink: 0;
}

.remark-text {
  color: #606266;
  line-height: 1.5;
}
</style>
