<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue';
import { ElMessageBox } from 'element-plus';
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
  
  // 显示确认对话框
  try {
    await ElMessageBox.confirm(
      '断开连接后将无法接收消息和执行自动化流程，确定要断开连接吗？',
      '确认断开连接',
      {
        confirmButtonText: '确定断开',
        cancelButtonText: '取消',
        type: 'warning',
      }
    );
  } catch {
    // 用户取消操作
    return;
  }
  
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
          <!-- 连接状态大卡片 - 包含所有依赖信息 -->
          <div v-if="connectionStatus" class="connection-main-card">
            <!-- 主标题区域 -->
            <div class="main-card-header">
              <div class="header-left">
                <div class="icon-wrapper-large" :class="connectionStatus.connected ? 'icon-success' : 'icon-danger'">
                  <span class="icon-large">{{ connectionStatus.connected ? '✓' : '✕' }}</span>
                </div>
                <div class="header-info">
                  <h2 class="main-title">连接状态</h2>
                  <p class="main-subtitle">账号 ID: {{ connectionStatus.xianyuAccountId }} · {{ connectionStatus.status }}</p>
                  <p class="main-note" :class="connectionStatus.connected ? 'note-success' : 'note-danger'">
                    {{ connectionStatus.connected ? '已连接到闲鱼服务器' : '当前未连接到闲鱼服务器，无法监听消息以及执行自动化流程' }}
                  </p>
                </div>
              </div>
              <div class="header-right">
                <el-tag
                  :type="connectionStatus.connected ? 'success' : 'danger'"
                  size="large"
                  effect="dark"
                  round
                  class="status-tag-large"
                >
                  {{ connectionStatus.connected ? '● 已连接' : '● 未连接' }}
                </el-tag>
              </div>
            </div>

            <!-- 详细信息区域 -->
            <div class="details-grid">
              <!-- Cookie 详情 -->
              <div class="detail-section cookie-section">
                <div class="section-header">
                  <div class="section-icon">🍪</div>
                  <div class="section-title-group">
                    <h3 class="section-title">Cookie 凭证</h3>
                    <p class="section-note">用于识别账号，如果过期无法使用任何功能</p>
                  </div>
                  <el-tag 
                    :type="getCookieStatusType(connectionStatus.cookieStatus)" 
                    size="small"
                    round
                  >
                    {{ getCookieStatusText(connectionStatus.cookieStatus) }}
                  </el-tag>
                </div>
                <div class="section-body">
                  <div class="info-box">
                    <div class="info-box-label">Cookie 内容</div>
                    <div class="info-box-value cookie-value">
                      {{ connectionStatus.cookieText || '未获取到Cookie' }}
                    </div>
                    <div class="info-box-meta" v-if="connectionStatus.cookieText">
                      长度: {{ connectionStatus.cookieText.length }} 字符
                    </div>
                  </div>
                  <div class="section-actions">
                    <el-button
                      type="warning"
                      size="small"
                      @click="handleRefreshCookie"
                    >
                      📱 扫码刷新
                    </el-button>
                    <el-button
                      type="primary"
                      size="small"
                      @click="handleManualUpdateCookie"
                      class="manual-update-btn"
                    >
                      ✏️ 手动更新
                    </el-button>
                  </div>
                </div>
              </div>

              <!-- Token 详情 -->
              <div class="detail-section token-section">
                <div class="section-header">
                  <div class="section-icon">🔑</div>
                  <div class="section-title-group">
                    <h3 class="section-title">WebSocket Token</h3>
                    <p class="section-note">这个是收取消息的凭证，如果异常，可能是账号被锁人机验证，需要隔段时间再试一试</p>
                  </div>
                  <el-tag 
                    :type="getTokenStatusType(connectionStatus.tokenExpireTime)" 
                    size="small"
                    round
                  >
                    {{ getTokenStatusText(connectionStatus.tokenExpireTime) }}
                  </el-tag>
                </div>
                <div class="section-body">
                  <div class="info-box">
                    <div class="info-box-label">⏰ 过期时间</div>
                    <div class="info-box-value time-value">
                      {{ formatTimestamp(connectionStatus.tokenExpireTime) }}
                    </div>
                  </div>
                  <div class="info-box">
                    <div class="info-box-label">Token 内容</div>
                    <div class="info-box-value token-value">
                      {{ connectionStatus.websocketToken || '未获取到Token' }}
                    </div>
                    <div class="info-box-meta" v-if="connectionStatus.websocketToken">
                      长度: {{ connectionStatus.websocketToken.length }} 字符
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 操作区域 -->
            <div class="main-actions">
              <div class="action-wrapper">
                <el-button
                  v-if="connectionStatus.connected"
                  type="danger"
                  size="default"
                  @click="handleStopConnection"
                  class="main-action-btn"
                >
                  ⏸ 断开连接
                </el-button>
                <el-button
                  v-else
                  type="success"
                  size="default"
                  @click="handleStartConnection"
                  class="main-action-btn start-connection-btn"
                >
                  ▶ 启动连接
                </el-button>
                <div class="action-tip">
                  ⚠️ 请勿频繁启用连接和断开连接，否则容易触发滑动窗口人机校验，导致账号暂时不可用
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

/* 连接状态主卡片 */
.connection-main-card {
  background: linear-gradient(135deg, #ffffff 0%, #f8f9fa 100%);
  border-radius: 12px;
  border: 2px solid #409eff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.12);
  overflow: hidden;
}

/* 主标题区域 */
.main-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: linear-gradient(135deg, #ecf5ff 0%, #ffffff 100%);
  border-bottom: 2px solid #d9ecff;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.icon-wrapper-large {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
}

.icon-success {
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
}

.icon-danger {
  background: linear-gradient(135deg, #f56c6c 0%, #f78989 100%);
}

.icon-large {
  font-size: 28px;
  font-weight: bold;
  color: white;
}

.header-info {
  flex: 1;
}

.main-title {
  font-size: 17px;
  font-weight: 700;
  color: #303133;
  margin: 0 0 4px 0;
  letter-spacing: 0.3px;
}

.main-subtitle {
  font-size: 12px;
  color: #909399;
  margin: 0 0 3px 0;
  font-weight: 500;
}

.main-note {
  font-size: 11px;
  margin: 0;
  font-weight: 500;
  padding: 4px 8px;
  border-radius: 4px;
  display: inline-block;
  margin-top: 4px;
}

.note-danger {
  color: #f56c6c;
  background: #fef0f0;
  border: 1px solid #fde2e2;
}

.note-success {
  color: #67c23a;
  background: #f0f9ff;
  border: 1px solid #c6f6d5;
}

.header-right {
  display: flex;
  align-items: center;
}

.status-tag-large {
  font-size: 14px;
  padding: 8px 16px;
  font-weight: 600;
}

/* 详细信息网格 */
.details-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  padding: 16px 20px;
}

.detail-section {
  background: white;
  border-radius: 10px;
  border: 2px solid #e4e7ed;
  padding: 14px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
}

.detail-section:hover {
  box-shadow: 0 3px 10px rgba(0, 0, 0, 0.08);
}

.cookie-section {
  border-color: #e6a23c;
}

.token-section {
  border-color: #67c23a;
}

.section-header {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f5f7fa;
}

.section-icon {
  font-size: 24px;
  flex-shrink: 0;
  line-height: 1;
}

.section-title-group {
  flex: 1;
  min-width: 0;
}

.section-title {
  font-size: 14px;
  font-weight: 700;
  color: #303133;
  margin: 0 0 4px 0;
}

.section-note {
  font-size: 11px;
  color: #909399;
  margin: 0;
  line-height: 1.4;
}

.section-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.info-box {
  background: #f8f9fa;
  padding: 10px;
  border-radius: 6px;
  border: 1px solid #e4e7ed;
}

.info-box-label {
  font-size: 10px;
  color: #909399;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 6px;
}

.info-box-value {
  font-family: 'Courier New', Consolas, monospace;
  font-size: 10px;
  color: #606266;
  line-height: 1.5;
  word-break: break-all;
  background: white;
  padding: 8px;
  border-radius: 4px;
  border: 1px solid #dcdfe6;
  max-height: 80px;
  overflow-y: auto;
}

.cookie-value,
.token-value {
  font-size: 10px;
}

.time-value {
  font-size: 11px;
  font-weight: 600;
  color: #303133;
}

.info-box-meta {
  font-size: 10px;
  color: #909399;
  margin-top: 4px;
  text-align: right;
}

.section-actions {
  display: flex;
  gap: 6px;
  margin-top: 2px;
}

.section-actions .el-button {
  flex: 1;
}

.manual-update-btn {
  color: white !important;
}

/* 主操作区域 */
.main-actions {
  padding: 14px 20px;
  background: linear-gradient(135deg, #f5f7fa 0%, #ffffff 100%);
  border-top: 1px solid #e4e7ed;
  display: flex;
  justify-content: center;
}

.action-wrapper {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.main-action-btn {
  width: 50%;
  height: 40px;
  font-size: 14px;
  font-weight: 600;
}

.action-tip {
  font-size: 11px;
  color: #909399;
  text-align: center;
  line-height: 1.5;
  max-width: 80%;
}

.start-connection-btn {
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%) !important;
  border-color: #67c23a !important;
  box-shadow: 0 2px 8px rgba(103, 194, 58, 0.3) !important;
}

.start-connection-btn:hover {
  background: linear-gradient(135deg, #85ce61 0%, #95d475 100%) !important;
  box-shadow: 0 4px 12px rgba(103, 194, 58, 0.4) !important;
  transform: translateY(-1px);
}

.logs-section {
  margin-top: 16px;
}

.logs-header {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 10px;
}

.logs-container {
  background: #2c3e50;
  color: #ecf0f1;
  border-radius: 8px;
  padding: 12px;
  font-family: 'Courier New', Consolas, monospace;
  font-size: 12px;
  max-height: 200px;
  overflow-y: auto;
}

.log-entry {
  margin-bottom: 6px;
  line-height: 1.5;
}

.log-entry:last-child {
  margin-bottom: 0;
}

.log-time {
  color: #95a5a6;
  margin-right: 6px;
  font-size: 11px;
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
  padding: 16px;
  font-size: 12px;
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
@media (max-width: 1200px) {
  .details-grid {
    grid-template-columns: 1fr;
  }
  
  .dependency-flow {
    flex-wrap: wrap;
  }
  
  .flow-arrow {
    display: none;
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
  
  .main-card-header {
    flex-direction: column;
    gap: 16px;
    align-items: flex-start;
  }
  
  .header-right {
    width: 100%;
    justify-content: flex-end;
  }
  
  .dependency-flow {
    padding: 20px;
  }
  
  .flow-content {
    padding: 12px 16px;
  }
  
  .details-grid {
    padding: 20px;
  }
}
</style>
