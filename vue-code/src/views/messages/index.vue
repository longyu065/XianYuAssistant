<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { getAccountList } from '@/api/account';
import { getMessageList } from '@/api/message';
import { getGoodsList } from '@/api/goods';
import { showError, showInfo } from '@/utils';
import type { Account } from '@/types';
import type { ChatMessage } from '@/api/message';
import type { GoodsItemWithConfig } from '@/api/goods';

const loading = ref(false);
const accounts = ref<Account[]>([]);
const selectedAccountId = ref<number | null>(null);
const goodsIdFilter = ref<string>('');
const messageList = ref<ChatMessage[]>([]);
const currentPage = ref(1);
const pageSize = ref(20);
const total = ref(0);
const filterCurrentAccount = ref(false); // 过滤当前账号消息开关

// 商品列表相关
const goodsList = ref<GoodsItemWithConfig[]>([]);
const goodsCurrentPage = ref(1);
const goodsTotal = ref(0);
const goodsLoading = ref(false);
const goodsListRef = ref<HTMLElement | null>(null);

// 获取当前选中账号的UNB
const getCurrentAccountUnb = computed(() => {
  if (!selectedAccountId.value) return '';
  const account = accounts.value.find(acc => acc.id === selectedAccountId.value);
  return account ? account.unb : '';
});

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
        loadGoodsList();
      }
    }
  } catch (error: any) {
    console.error('加载账号列表失败:', error);
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
      pageSize: pageSize.value,
      filterCurrentAccount: filterCurrentAccount.value // 添加过滤参数
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
    console.error('加载消息列表失败:', error);
    messageList.value = [];
  } finally {
    loading.value = false;
  }
};

// 加载商品列表
const loadGoodsList = async () => {
  if (!selectedAccountId.value) {
    return;
  }

  goodsLoading.value = true;
  try {
    const params: any = {
      xianyuAccountId: selectedAccountId.value,
      pageNum: goodsCurrentPage.value,
      pageSize: 10
    };

    const response = await getGoodsList(params);
    if (response.code === 0 || response.code === 200) {
      // 如果是第一页，则替换列表，否则追加到列表末尾
      if (goodsCurrentPage.value === 1) {
        goodsList.value = response.data?.itemsWithConfig || [];
      } else {
        goodsList.value.push(...(response.data?.itemsWithConfig || []));
      }
      goodsTotal.value = response.data?.totalCount || 0;
    } else {
      throw new Error(response.msg || '获取商品列表失败');
    }
  } catch (error: any) {
    console.error('加载商品列表失败:', error);
    goodsList.value = [];
  } finally {
    goodsLoading.value = false;
  }
};

// 处理商品列表滚动事件
const handleGoodsScroll = () => {
  if (!goodsListRef.value) return;

  const { scrollTop, scrollHeight, clientHeight } = goodsListRef.value;
  // 当滚动到底部时加载更多
  if (scrollTop + clientHeight >= scrollHeight - 10) {
    if (goodsList.value.length < goodsTotal.value) {
      goodsCurrentPage.value++;
      loadGoodsList();
    }
  }
};

// 监听滚动事件
const addScrollListener = () => {
  if (goodsListRef.value) {
    goodsListRef.value.addEventListener('scroll', handleGoodsScroll);
  }
};

// 移除滚动监听
const removeScrollListener = () => {
  if (goodsListRef.value) {
    goodsListRef.value.removeEventListener('scroll', handleGoodsScroll);
  }
};

// 账号变更
const handleAccountChange = () => {
  currentPage.value = 1;
  goodsCurrentPage.value = 1;
  loadMessages();
  loadGoodsList();
};

// 选择商品进行筛选
const selectGoods = (goodsId: string) => {
  goodsIdFilter.value = goodsId;
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
  // contentType=1 是用户消息，其他都是系统消息
  if (contentType === 1) {
    return '用户消息';
  }
  return `系统消息(${contentType})`;
};

// 获取消息类型标签类型
const getContentTypeTag = (contentType: number) => {
  // contentType=1 是用户消息（绿色），其他都是系统消息（橙色）
  if (contentType === 1) {
    return 'success';
  }
  return 'warning';
};

// 判断是否为用户发送的消息
const isUserMessage = (row: ChatMessage) => {
  // 如果senderUserId不等于当前账号的UNB，则标记为用户发送的消息
  return row.senderUserId !== getCurrentAccountUnb.value;
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
  // 等待DOM渲染完成后添加滚动监听
  setTimeout(() => {
    addScrollListener();
  }, 0);
});

onUnmounted(() => {
  removeScrollListener();
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
        
        <el-button @click="loadMessages">刷新消息</el-button>
        
        <el-switch
          v-model="filterCurrentAccount"
          active-text="隐藏当前账号消息"
          inactive-text="显示全部消息"
          @change="loadMessages"
        />
      </div>
    </div>

    <div class="content-container">
      <!-- 左侧商品列表 -->
      <div class="goods-filter-panel">
        <div class="panel-header">
          <span class="panel-title">商品列表</span>
          <el-button 
            link 
            type="primary" 
            @click="loadGoodsList"
            :loading="goodsLoading"
            size="small"
          >
            刷新
          </el-button>
        </div>
        <div 
          ref="goodsListRef" 
          class="goods-list-container"
        >
          <div 
            v-for="goods in goodsList" 
            :key="goods.item.id"
            class="goods-item"
            :class="{ active: goodsIdFilter === goods.item.xyGoodId }"
            @click="selectGoods(goods.item.xyGoodId)"
          >
            <div class="goods-cover">
              <img 
                :src="goods.item.coverPic" 
                :alt="goods.item.title"
                class="cover-img"
              >
            </div>
            <div class="goods-info">
              <div class="goods-title">{{ goods.item.title }}</div>
              <div class="goods-id">#{{ goods.item.xyGoodId }}</div>
            </div>
          </div>
          
          <!-- 加载更多提示 -->
          <div v-if="goodsLoading && goodsCurrentPage > 1" class="loading-more">
            加载中...
          </div>
        </div>
      </div>

      <!-- 右侧消息列表 -->
      <div class="messages-container">
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
            max-height="calc(100vh - 300px)"
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
            
            <el-table-column prop="msgContent" label="消息内容" min-width="200" show-overflow-tooltip>
              <template #default="{ row }">
                <div 
                  class="message-content" 
                  :class="{ 'user-message': isUserMessage(row) }"
                >
                  {{ row.msgContent }}
                </div>
              </template>
            </el-table-column>
            
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
    </div>
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

.content-container {
  display: flex;
  flex: 1;
  gap: 20px;
}

.goods-filter-panel {
  width: 300px;
  display: flex;
  flex-direction: column;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background-color: #fff;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #ebeef5;
}

.panel-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.goods-list-container {
  flex: 1;
  overflow-y: auto;
  max-height: calc(100vh - 200px);
}

.goods-item {
  display: flex;
  padding: 12px 16px;
  border-bottom: 1px solid #f5f7fa;
  cursor: pointer;
  transition: background-color 0.2s;
}

.goods-item:hover {
  background-color: #f5f7fa;
}

.goods-item.active {
  background-color: #ecf5ff;
  border-left: 3px solid #409eff;
}

.goods-cover {
  width: 50px;
  height: 50px;
  border-radius: 4px;
  overflow: hidden;
  margin-right: 12px;
  flex-shrink: 0;
}

.cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.goods-info {
  flex: 1;
  min-width: 0;
}

.goods-title {
  font-size: 14px;
  color: #303133;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.goods-id {
  font-size: 12px;
  color: #909399;
  font-family: 'Courier New', Consolas, monospace;
}

.loading-more {
  text-align: center;
  padding: 12px;
  color: #909399;
  font-size: 14px;
}

.messages-container {
  flex: 1;
  display: flex;
  flex-direction: column;
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

.message-content {
  padding: 4px;
}

.message-content.user-message {
  border: 2px solid #67c23a;
  border-radius: 4px;
}

.pagination-container {
  display: flex;
  justify-content: center;
  padding: 10px 0;
  margin-top: 10px;
  border-top: 1px solid #ebeef5;
}
</style>