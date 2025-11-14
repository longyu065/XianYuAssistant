<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { getAccountList } from '@/api/account';
import { getMessageList } from '@/api/message';
import { showError, showInfo } from '@/utils';
import type { Account } from '@/types';
import type { ChatMessage } from '@/api/message';

const loading = ref(false);
const accounts = ref<Account[]>([]);
const selectedAccountId = ref<number | null>(null);
const goodsIdFilter = ref<string>('');
const messageList = ref<ChatMessage[]>([]);
const currentPage = ref(1);
const pageSize = ref(20);
const total = ref(0);

// 加载账号列表
const loadAccounts = async () => {
  try {
    const response = await getAccountList();
    if (response.code === 0 || response.code === 200) {
      accounts.value = response.data?.accounts || [];
      // 默认选择第一个账号
      if (accounts.value.length > 0 && !selectedAccountId.value) {
        selectedAccountId.value = accounts.value[0].id;
        loadMessages();
      }
    }
  } catch (error: any) {
    showError('加载账号列表失败: ' + error.message);
  }
};

// 加载消息列表
const loadMessages = async () => {
  if (!selectedAccountId.value) {
    showInfo('请先选择账号');
    return;
  }

  loading.value = true;
  try {
    const params: any = {
      xianyuAccountId: selectedAccountId.value,
      pageNum: currentPage.value,
      pageSize: pageSize.value
    };

    if (goodsIdFilter.value) {
      params.xyGoodsId = goodsIdFilter.value;
    }

    const response = await getMessageList(params);
    if (response.code === 0 || response.code === 200) {
      messageList.value = response.data?.list || [];
      total.value = response.data?.totalCount || 0;
    } else {
      throw new Error(response.msg || '获取消息列表失败');
    }
  } catch (error: any) {
    showError('加载消息列表失败: ' + error.message);
    messageList.value = [];
  } finally {
    loading.value = false;
  }
};

// 账号变更
const handleAccountChange = () => {
  currentPage.value = 1;
  loadMessages();
};

// 商品ID筛选
const handleGoodsFilter = () => {
  currentPage.value = 1;
  loadMessages();
};

// 清除筛选
const handleClearFilter = () => {
  goodsIdFilter.value = '';
  currentPage.value = 1;
  loadMessages();
};

// 分页变更
const handlePageChange = (page: number) => {
  currentPage.value = page;
  loadMessages();
};

// 获取消息类型文本
const getContentTypeText = (contentType: number) => {
  const typeMap: Record<number, string> = {
    1: '用户消息',
    2: '图片',
    32: '已付款待发货'
  };
  return typeMap[contentType] || `其他(${contentType})`;
};

// 获取消息类型标签类型
const getContentTypeTag = (contentType: number) => {
  const tagMap: Record<number, string> = {
    1: '',
    2: 'success',
    32: 'warning'
  };
  return tagMap[contentType] || 'info';
};

// 格式化消息时间
const formatMessageTime = (timestamp: number) => {
  if (!timestamp) return '-';

  const date = new Date(timestamp);
  const now = new Date();
  const diff = now.getTime() - date.getTime();

  // 小于1分钟
  if (diff < 60000) {
    return '刚刚';
  }

  // 小于1小时
  if (diff < 3600000) {
    return `${Math.floor(diff / 60000)}分钟前`;
  }

  // 小于24小时
  if (diff < 86400000) {
    return `${Math.floor(diff / 3600000)}小时前`;
  }

  // 超过24小时，显示具体日期时间
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
};

onMounted(() => {
  loadAccounts();
});
</script>

<template>
  <div class="messages-page">
    <div class="page-header">
      <h1 class="page-title">消息管理</h1>
      <div class="header-actions">
        <el-select
          v-model="selectedAccountId"
          placeholder="选择账号"
          style="width: 200px"
          @change="handleAccountChange"
        >
          <el-option
            v-for="account in accounts"
            :key="account.id"
            :label="account.accountNote || account.unb"
            :value="account.id"
          />
        </el-select>
        
        <el-input
          v-model="goodsIdFilter"
          placeholder="输入商品ID筛选"
          style="width: 200px"
          clearable
          @clear="handleClearFilter"
        >
          <template #append>
            <el-button @click="handleGoodsFilter">筛选</el-button>
          </template>
        </el-input>
        
        <el-button @click="loadMessages">刷新消息</el-button>
      </div>
    </div>

    <el-card class="messages-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">消息列表</span>
          <span class="card-subtitle">共 {{ total }} 条消息</span>
        </div>
      </template>

      <el-table
        v-loading="loading"
        :data="messageList"
        stripe
        style="width: 100%"
        max-height="600"
      >
        <el-table-column type="index" label="序号" width="60" align="center" />
        
        <el-table-column prop="id" label="消息ID" width="100">
          <template #default="{ row }">
            <div class="message-id">{{ row.id }}</div>
          </template>
        </el-table-column>
        
        <el-table-column label="消息类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getContentTypeTag(row.contentType)" size="small">
              {{ getContentTypeText(row.contentType) }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="senderUserName" label="发送者" width="120" show-overflow-tooltip />
        
        <el-table-column prop="msgContent" label="消息内容" min-width="200" show-overflow-tooltip />
        
        <el-table-column prop="xyGoodsId" label="商品ID" width="120">
          <template #default="{ row }">
            <div class="goods-id">{{ row.xyGoodsId || '-' }}</div>
          </template>
        </el-table-column>
        
        <el-table-column label="时间" width="150">
          <template #default="{ row }">
            <div class="message-time">{{ formatMessageTime(row.messageTime) }}</div>
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.reminderUrl"
              type="primary"
              link
              size="small"
              @click="() => window.open(row.reminderUrl, '_blank')"
            >
              查看链接
            </el-button>
            <span v-else class="no-action">-</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next, jumper"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.messages-page {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.messages-card {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.card-subtitle {
  font-size: 14px;
  color: #909399;
}

.message-id {
  font-family: 'Courier New', Consolas, monospace;
  font-size: 12px;
  color: #606266;
}

.goods-id {
  font-family: 'Courier New', Consolas, monospace;
  font-size: 12px;
  color: #409eff;
}

.message-time {
  font-size: 12px;
  color: #909399;
}

.no-action {
  color: #c0c4cc;
}

.pagination-container {
  display: flex;
  justify-content: center;
  padding: 20px 0;
  margin-top: 20px;
  border-top: 1px solid #ebeef5;
}
</style>
