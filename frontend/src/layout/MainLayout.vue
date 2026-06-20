<template>
  <el-container class="main-layout">
    <el-header class="header">
      <div class="header-left">
        <el-icon :size="24"><SetUp /></el-icon>
        <span class="title">注塑车间 · 定位块工装资产登记系统</span>
      </div>
      <div class="header-right">
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
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { Box, Switch, Checked, Delete, SetUp, Clock, Grid, Position, Operation } from '@element-plus/icons-vue'

const route = useRoute()
const activeMenu = computed(() => route.path)
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
</style>
