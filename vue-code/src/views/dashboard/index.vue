<script setup lang="ts">
import { useDashboard } from './useDashboard'

const { loading, stats, loadStatistics } = useDashboard()

// 组件挂载时加载数据
loadStatistics()
</script>

<template>
  <div class="dashboard-page" v-loading="loading">
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon account-icon">👤</div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.accountCount }}</div>
              <div class="stat-label">闲鱼账号</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon goods-icon">📦</div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.goodsCount }}</div>
              <div class="stat-label">商品总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon online-icon">🔥</div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.onlineGoodsCount }}</div>
              <div class="stat-label">在售商品</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="welcome-card" shadow="never">
      <h2>欢迎使用闲鱼自动化管理系统</h2>
      <p>基于 Vue3 + TypeScript 的现代化管理系统</p>
      <el-divider />
      <el-row :gutter="20">
        <el-col :span="12">
          <h3>快速开始</h3>
          <ul>
            <li>1. 在"扫码登录"页面添加闲鱼账号</li>
            <li>2. 在"连接管理"页面启动 WebSocket 连接</li>
            <li>3. 在"商品管理"页面配置自动化功能</li>
          </ul>
        </el-col>
        <el-col :span="12">
          <h3>功能特性</h3>
          <ul>
            <li>✅ 多账号管理</li>
            <li>✅ 自动发货</li>
            <li>✅ 自动回复</li>
            <li>✅ 消息管理</li>
          </ul>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<style scoped src="./dashboard.css"></style>
