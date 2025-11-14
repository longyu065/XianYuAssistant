<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import { getAccountList } from '@/api/account';
import { getConnectionStatus, startConnection, stopConnection } from '@/api/websocket';
import { showSuccess, showError, showInfo } from '@/utils';
import type { Account } from '@/types';

interface ConnectionStatus {
  xianyuAccountId: number;
  connected: boolean;
  status: string;
}

const loading = ref(false);
const accounts = ref<Account[]>([]);
const selectedAccountId = ref<number | null>(null);
const connectionStatus = ref<ConnectionStatus | null>(null);
const statusLoading = ref(false);
const logs = ref<Array<{ time: string; message: string; isError?: boolean }>>([]);
let statusInterval: number | null = null;

// 加载账号列表
const loadAccounts = async () => {
  loading.value = true;
  try {
    const response = await getAccountList();
    if (response.code === 0 || response.code === 200) {
      accounts.value = response.data?.accounts || [];
    } else {
      throw new Error(response.msg || '获取账号列表失败');
    }
  } catch (error: any) {
    showError('加载账号列表失败: ' + error.message);
    accounts.value = [];
  } finally {
    loading.value = false;
  }
};

// 选择账号
const selectAccount = (accountId: number) => {
  selectedAccountId.value = accountId;
  loadConnectionStatus(accountId);
  
  // 启动定时刷新
  if (statusInterval) {
    clearInterval(statusInterval);
  }
  statusInterval = window.setInterval(() => {
    if (selectedAccountId.value) {
      loadConnectionStatus(selectedAccountId.value, true);
    }
  }, 5000);
};

// 加载连接状态
const loadConnectionStatus = async (accountId: number, silent = false) => {
  if (!silent) {
    statusLoading.value = true;
  }
  try {
    const response = await getConnectionStatus(accountId);
    if (response.code === 0 || response.code === 200) {
      connectionStatus.value = response.data;
      if (!silent) {
        addLog('状态已更新');
      }
    } else {
      throw new Error(response.msg || '获取连接状态失败');
    }
  } catch (error: any) {
    if (!silent) {
      showError('加载连接状态失败: ' + error.message);
      addLog('加载状态失败: ' + error.message, true);
    }
  } finally {
    statusLoading.value = false;
  }
};

// 启动连接
const handleStartConnection = async () => {
  if (!selectedAccountId.value) return;
  
  statusLoading.value = true;
  addLog('正在启动连接...');
  try {
    const response = await startConnection(selectedAccountId.value);
    if (response.code === 0 || response.code === 200) {
      showSuccess('连接启动成功');
      addLog('连接启动成功');
      await loadConnectionStatus(selectedAccountId.value);
    } else {
      throw new Error(response.msg || '启动连接失败');
    }
  } catch (error: any) {
    showError('启动连接失败: ' + error.message);
    addLog('启动连接失败: ' + error.message, true);
  } finally {
    statusLoading.value = false;
  }
};

// 停止连接
const handleStopConnection = async () => {
  if (!selectedAccountId.value) return;
  
  statusLoading.value = true;
  addLog('正在断开连接...');
  try {
    const response = await stopConnection(selectedAccountId.value);
    if (response.code === 0 || response.code === 200) {
      showSuccess('连接已断开');
      addLog('连接已断开');
      await loadConnectionStatus(selectedAccountId.value);
    } else {
      throw new Error(response.msg || '断开连接失败');
    }
  } catch (error: any) {
    showError('断开连接失败: ' + error.message);
    addLog('断开连接失败: ' + error.message, true);
  } finally {
    statusLoading.value = false;
  }
};

// 刷新状态
const handleRefresh = () => {
  if (selectedAccountId.value) {
    loadConnectionStatus(selectedAccountId.value);
    showInfo('状态已刷新');
  }
};

// 添加日志
const addLog = (message: string, isError = false) => {
  const now = new Date();
  const time = now.toLocaleTimeString();
  logs.value.push({ time, message, isError });
  
  // 限制日志数量
  if (logs.value.length > 50) {
    logs.value.shift();
  }
};

// 获取账号显示名称
const getAccountName = (account: Account) => {
  return account.accountNote || account.unb || '未命名账号';
};

// 获取账号头像字符
const getAccountAvatar = (account: Account) => {
  const name = getAccountName(account);
  return name.charAt(0);
};

onMounted(() => {
  loadAccounts();
});

onUnmounted(() => {
  if (statusInterval) {
    clearInterval(statusInterval);
  }
});
</script>

<template>
  <div class="connection-page">
    <div class="page-header">
      <h1 class="page-title">连接管理</h1>
    </div>

    <div class="connection-container">
      <!-- 左侧账号列表 -->
      <el-card class="account-panel">
        <template #header>
          <div class="panel-header">
            <span class="panel-title">闲鱼账号</span>
          </div>
        </template>
        
        <div v-loading="loading" class="account-list">
          <div
            v-for="account in accounts"
            :key="account.id"
            class="account-item"
            :class="{ active: selectedAccountId === account.id }"
            @click="selectAccount(account.id)"
          >
            <div class="account-avatar">{{ getAccountAvatar(account) }}</div>
            <div class="account-info">
              <div class="account-name">{{ getAccountName(account) }}</div>
              <div class="account-id">ID: {{ account.id }}</div>
            </div>
          </div>
          
          <el-empty
            v-if="!loading && accounts.length === 0"
            description="暂无账号数据"
            :image-size="80"
          />
        </div>
      </el-card>

      <!-- 右侧连接状态 -->
      <el-card class="status-panel">
        <template #header>
          <div class="panel-header">
            <span class="panel-title">连接状态</span>
          </div>
        </template>
        
        <div v-if="!selectedAccountId" class="empty-state">
          <el-empty description="请选择一个账号查看连接状态" :image-size="100">
            <template #image>
              <div class="empty-icon">🔗</div>
            </template>
          </el-empty>
        </div>

        <div v-else v-loading="statusLoading" class="status-content">
          <!-- 连接状态卡片 -->
          <div v-if="connectionStatus" class="status-card">
            <div class="status-header">
              <h3 class="status-title">连接信息</h3>
              <el-tag
                :type="connectionStatus.connected ? 'success' : 'danger'"
                size="large"
                effect="dark"
              >
                {{ connectionStatus.connected ? '已连接' : '未连接' }}
              </el-tag>
            </div>
            
            <div class="status-details">
              <div class="detail-item">
                <span class="detail-label">账号ID</span>
                <span class="detail-value">{{ connectionStatus.xianyuAccountId }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">连接状态</span>
                <span class="detail-value">{{ connectionStatus.status }}</span>
              </div>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="action-buttons">
            <el-button
              v-if="connectionStatus?.connected"
              type="danger"
              size="large"
              @click="handleStopConnection"
            >
              断开连接
            </el-button>
            <el-button
              v-else
              type="primary"
              size="large"
              @click="handleStartConnection"
            >
              启动连接
            </el-button>
            <el-button size="large" @click="handleRefresh">
              刷新状态
            </el-button>
          </div>

          <!-- 操作日志 -->
          <div class="logs-section">
            <div class="logs-header">操作日志</div>
            <div class="logs-container">
              <div
                v-for="(log, index) in logs"
                :key="index"
                class="log-entry"
                :class="{ 'log-error': log.isError }"
              >
                <span class="log-time">[{{ log.time }}]</span>
                <span class="log-message">{{ log.message }}</span>
              </div>
              <div v-if="logs.length === 0" class="log-empty">
                暂无日志记录
              </div>
            </div>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.connection-page {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.page-header {
  margin-bottom: 20px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.connection-container {
  flex: 1;
  display: flex;
  gap: 20px;
  min-height: 0;
}

.account-panel {
  flex: 1;
  min-width: 300px;
  display: flex;
  flex-direction: column;
}

.status-panel {
  flex: 2;
  min-width: 400px;
  display: flex;
  flex-direction: column;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.panel-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.account-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow-y: auto;
  max-height: calc(100vh - 280px);
}

.account-item {
  display: flex;
  align-items: center;
  padding: 12px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  gap: 12px;
}

.account-item:hover {
  background: #fafafa;
}

.account-item.active {
  background: #1a1a1a;
  color: white;
}

.account-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #1a1a1a;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 16px;
  margin-right: 0;
  flex-shrink: 0;
}

.account-item.active .account-avatar {
  background: white;
  color: #1a1a1a;
}

.account-info {
  flex: 1;
  min-width: 0;
}

.account-name {
  font-weight: 500;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.account-id {
  font-size: 12px;
  opacity: 0.7;
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 400px;
}

.empty-icon {
  font-size: 80px;
}

.status-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.status-card {
  background: #fafafa;
  border-radius: 8px;
  padding: 20px;
  border: 1px solid #e8e8e8;
}

.status-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.status-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0;
}

.status-details {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.detail-item {
  display: flex;
  flex-direction: column;
}

.detail-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 4px;
}

.detail-value {
  font-size: 16px;
  font-weight: 500;
  color: #303133;
}

.action-buttons {
  display: flex;
  gap: 12px;
}

.logs-section {
  margin-top: 10px;
}

.logs-header {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}

.logs-container {
  background: #2c3e50;
  color: #ecf0f1;
  border-radius: 8px;
  padding: 16px;
  font-family: 'Courier New', Consolas, monospace;
  font-size: 13px;
  max-height: 300px;
  overflow-y: auto;
}

.log-entry {
  margin-bottom: 8px;
  line-height: 1.6;
}

.log-entry:last-child {
  margin-bottom: 0;
}

.log-time {
  color: #95a5a6;
  margin-right: 8px;
}

.log-message {
  color: #ecf0f1;
}

.log-entry.log-error .log-message {
  color: #e74c3c;
}

.log-empty {
  text-align: center;
  color: #95a5a6;
  padding: 20px;
}

/* 滚动条样式 */
.account-list::-webkit-scrollbar,
.logs-container::-webkit-scrollbar {
  width: 6px;
}

.account-list::-webkit-scrollbar-thumb,
.logs-container::-webkit-scrollbar-thumb {
  background: #dcdfe6;
  border-radius: 3px;
}

.logs-container::-webkit-scrollbar-thumb {
  background: #34495e;
}

/* 响应式 */
@media (max-width: 768px) {
  .connection-container {
    flex-direction: column;
  }
  
  .account-panel,
  .status-panel {
    min-width: auto;
  }
  
  .status-details {
    grid-template-columns: 1fr;
  }
}
</style>
