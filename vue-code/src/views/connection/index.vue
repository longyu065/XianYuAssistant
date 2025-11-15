<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue';
import { getAccountList } from '@/api/account';
import { getConnectionStatus, startConnection, stopConnection } from '@/api/websocket';
import { showSuccess, showError, showInfo } from '@/utils';
import type { Account, WebSocketStatus } from '@/types';
import RefreshCookieDialog from './components/RefreshCookieDialog.vue';
import ManualUpdateCookieDialog from './components/ManualUpdateCookieDialog.vue';

interface ConnectionStatus {
  xianyuAccountId: number;
  connected: boolean;
  status: string;
  cookieStatus?: number;      // Cookie状态 1:有效 2:过期 3:失效
  cookieText?: string;        // Cookie值
  websocketToken?: string;    // WebSocket Token
  tokenExpireTime?: number;   // Token过期时间戳（毫秒）
}

const loading = ref(false);
const accounts = ref<Account[]>([]);
const selectedAccountId = ref<number | null>(null);
const connectionStatus = ref<ConnectionStatus | null>(null);
const statusLoading = ref(false);
const logs = ref<Array<{ time: string; message: string; isError?: boolean }>>([]);
let statusInterval: number | null = null;

// 扫码刷新Cookie对话框
const showRefreshCookieDialog = ref(false);
// 手动更新Cookie对话框
const showManualUpdateCookieDialog = ref(false);

// 当前选中的账号信息
const currentAccount = computed(() => {
  return accounts.value.find(acc => acc.id === selectedAccountId.value);
});

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
    console.error('加载账号列表失败:', error);
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
      connectionStatus.value = response.data as ConnectionStatus;
      if (!silent) {
        addLog('状态已更新');
      }
    } else {
      throw new Error(response.msg || '获取连接状态失败');
    }
  } catch (error: any) {
    if (!silent) {
      console.error('加载连接状态失败:', error);
      addLog('加载状态失败: ' + error.message, true);
    }
  } finally{
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
    console.error('启动连接失败:', error);
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
    console.error('断开连接失败:', error);
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

// 获取Cookie状态文本
const getCookieStatusText = (status?: number) => {
  if (status === undefined || status === null) return '未知';
  const statusMap: Record<number, string> = {
    1: '有效',
    2: '过期',
    3: '失效'
  };
  return statusMap[status] || '未知';
};

// 获取Cookie状态标签类型
const getCookieStatusType = (status?: number) => {
  if (status === undefined || status === null) return 'info';
  const typeMap: Record<number, string> = {
    1: 'success',
    2: 'warning',
    3: 'danger'
  };
  return typeMap[status] || 'info';
};

// 格式化时间戳
const formatTimestamp = (timestamp?: number) => {
  if (!timestamp) return '未设置';
  const date = new Date(timestamp);
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  });
};

// 判断Token是否过期
const isTokenExpired = (timestamp?: number) => {
  if (!timestamp) return false;
  return Date.now() > timestamp;
};

// 获取Token状态文本
const getTokenStatusText = (timestamp?: number) => {
  if (!timestamp) return '未设置';
  return isTokenExpired(timestamp) ? '已过期' : '有效';
};

// 获取Token状态类型
const getTokenStatusType = (timestamp?: number) => {
  if (!timestamp) return 'info';
  return isTokenExpired(timestamp) ? 'danger' : 'success';
};

// 打开扫码刷新Cookie对话框
const handleRefreshCookie = () => {
  showRefreshCookieDialog.value = true;
};

// 打开手动更新Cookie对话框
const handleManualUpdateCookie = () => {
  showManualUpdateCookieDialog.value = true;
};

// Cookie刷新成功回调
const handleRefreshCookieSuccess = async () => {
  addLog('Cookie已刷新');
  if (selectedAccountId.value) {
    await loadConnectionStatus(selectedAccountId.value);
  }
};

// Cookie手动更新成功回调
const handleManualUpdateCookieSuccess = async () => {
  addLog('Cookie已手动更新');
  if (selectedAccountId.value) {
    await loadConnectionStatus(selectedAccountId.value);
  }
};

onMounted(async () => {
  await loadAccounts();
  // 默认选择第一个账号
  if (accounts.value.length > 0) {
    selectAccount(accounts.value[0].id);
  }
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
            <el-button
              v-if="selectedAccountId"
              size="small"
              :icon="'Refresh'"
              @click="handleRefresh"
              circle
            />
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
          <!-- 连接状态卡片 - 三列布局 -->
          <div v-if="connectionStatus" class="status-cards-grid">
            <!-- 连接信息卡片 -->
            <div class="info-card">
              <div class="card-header">
                <span class="card-title">连接信息</span>
                <el-tag
                  :type="connectionStatus.connected ? 'success' : 'danger'"
                  size="small"
                  effect="dark"
                >
                  {{ connectionStatus.connected ? '已连接' : '未连接' }}
                </el-tag>
              </div>
              <div class="card-content">
                <div class="info-item">
                  <span class="info-label">账号ID</span>
                  <span class="info-value">{{ connectionStatus.xianyuAccountId }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">状态</span>
                  <span class="info-value">{{ connectionStatus.status }}</span>
                </div>
                <div class="card-actions">
                  <el-button
                    v-if="connectionStatus.connected"
                    type="danger"
                    size="default"
                    @click="handleStopConnection"
                    style="width: 100%"
                  >
                    断开连接
                  </el-button>
                  <el-button
                    v-else
                    type="primary"
                    size="default"
                    @click="handleStartConnection"
                    style="width: 100%"
                  >
                    启动连接
                  </el-button>
                </div>
              </div>
            </div>

            <!-- Cookie信息卡片 -->
            <div class="info-card">
              <div class="card-header">
                <span class="card-title">Cookie信息</span>
                <el-tag :type="getCookieStatusType(connectionStatus.cookieStatus)" size="small">
                  {{ getCookieStatusText(connectionStatus.cookieStatus) }}
                </el-tag>
              </div>
              <div class="card-content">
                <div class="info-item info-item-full">
                  <span class="info-label">Cookie值</span>
                  <el-input
                    :model-value="connectionStatus.cookieText || '未获取到Cookie'"
                    type="textarea"
                    :rows="2"
                    readonly
                    class="info-textarea"
                  />
                </div>
                <div class="card-actions">
                  <el-button
                    type="warning"
                    size="default"
                    @click="handleRefreshCookie"
                    class="action-btn"
                  >
                    扫码刷新
                  </el-button>
                  <el-button
                    type="primary"
                    size="default"
                    plain
                    @click="handleManualUpdateCookie"
                    class="action-btn"
                  >
                    手动更新
                  </el-button>
                </div>
              </div>
            </div>

            <!-- WebSocket Token卡片 -->
            <div class="info-card">
              <div class="card-header">
                <span class="card-title">WebSocket Token</span>
                <el-tag :type="getTokenStatusType(connectionStatus.tokenExpireTime)" size="small">
                  {{ getTokenStatusText(connectionStatus.tokenExpireTime) }}
                </el-tag>
              </div>
              <div class="card-content">
                <div class="info-item">
                  <span class="info-label">过期时间</span>
                  <span class="info-value info-value-small">{{ formatTimestamp(connectionStatus.tokenExpireTime) }}</span>
                </div>
                <div class="info-item info-item-full">
                  <span class="info-label">Token值</span>
                  <el-input
                    :model-value="connectionStatus.websocketToken || '未获取到Token'"
                    type="textarea"
                    :rows="2"
                    readonly
                    class="info-textarea"
                  />
                </div>
              </div>
            </div>
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

    <!-- 扫码刷新Cookie对话框 -->
    <RefreshCookieDialog
      v-if="currentAccount"
      v-model="showRefreshCookieDialog"
      :account-id="currentAccount.id"
      :current-unb="currentAccount.unb"
      @success="handleRefreshCookieSuccess"
    />

    <!-- 手动更新Cookie对话框 -->
    <ManualUpdateCookieDialog
      v-if="currentAccount && connectionStatus"
      v-model="showManualUpdateCookieDialog"
      :account-id="currentAccount.id"
      :current-cookie="connectionStatus.cookieText || ''"
      @success="handleManualUpdateCookieSuccess"
    />
  </div>
</template>

<style scoped>
.connection-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 15px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.page-title {
  font-size: 22px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.connection-container {
  flex: 1;
  display: flex;
  gap: 15px;
  min-height: 0;
}

.account-panel,
.status-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.account-panel {
  flex: 1;
  min-width: 0;
  max-width: 400px;
}

.status-panel {
  flex: 2;
  min-width: 0;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.panel-title {
  font-size: 17px;
  font-weight: 600;
  color: #303133;
}

.account-list {
  flex: 1;
  overflow-y: auto;
}

.account-item {
  display: flex;
  align-items: center;
  padding: 10px;
  border: 1px solid #ebeef5;
  border-radius: 3px;
  margin-bottom: 6px;
  cursor: pointer;
  transition: all 0.3s ease;
  gap: 12px;
}

.account-item:hover {
  background-color: #f5f7fa;
  border-color: #c0c4cc;
}

.account-item.active {
  background-color: #ecf5ff;
  border-color: #409eff;
}

.account-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #409eff;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 16px;
  margin-right: 0;
  flex-shrink: 0;
}

.account-info {
  flex: 1;
  min-width: 0;
}

.account-name {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.account-id {
  font-size: 12px;
  color: #909399;
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

/* 三列卡片网格布局 */
.status-cards-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.info-card {
  background: #ffffff;
  border-radius: 8px;
  padding: 16px;
  border: 1px solid #e4e7ed;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
  gap: 8px;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  white-space: nowrap;
}

.card-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
  flex: 1;
}

.card-actions {
  margin-top: auto;
  padding-top: 8px;
  display: flex;
  gap: 8px;
}

.action-btn {
  flex: 1;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.info-item-full {
  flex: 1;
}

.info-label {
  font-size: 12px;
  color: #909399;
  font-weight: 500;
}

.info-value {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  word-break: break-all;
}

.info-value-small {
  font-size: 12px;
  line-height: 1.4;
}

.info-textarea :deep(.el-textarea__inner) {
  font-family: 'Courier New', Consolas, monospace;
  font-size: 11px;
  line-height: 1.4;
  background: #f8f9fa;
  border-color: #dcdfe6;
  resize: none;
  padding: 8px;
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

/* 响应式布局 */
@media (max-width: 1400px) {
  .status-cards-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .info-card:last-child {
    grid-column: 1 / -1;
  }
}

@media (max-width: 1024px) {
  .status-cards-grid {
    grid-template-columns: 1fr;
  }
  
  .info-card:last-child {
    grid-column: auto;
  }
}

@media (max-width: 768px) {
  .connection-container {
    flex-direction: column;
  }
  
  .account-panel {
    max-width: none;
  }
  
  .account-panel,
  .status-panel {
    min-width: auto;
  }
  
  .status-cards-grid {
    grid-template-columns: 1fr;
  }
}
</style>
