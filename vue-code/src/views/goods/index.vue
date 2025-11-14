<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { getAccountList } from '@/api/account';
import { getGoodsList, refreshGoods, getGoodsDetail, updateAutoDeliveryStatus, updateAutoReplyStatus } from '@/api/goods';
import { showSuccess, showError, showInfo } from '@/utils';
import type { Account } from '@/types';
import type { GoodsItemWithConfig } from '@/api/goods';
import GoodsDetailDialog from './components/GoodsDetailDialog.vue';

const loading = ref(false);
const refreshing = ref(false);
const accounts = ref<Account[]>([]);
const selectedAccountId = ref<number | null>(null);
const statusFilter = ref<string>('');
const goodsList = ref<GoodsItemWithConfig[]>([]);
const currentPage = ref(1);
const pageSize = ref(20);
const total = ref(0);

// 商品详情对话框
const detailDialogVisible = ref(false);
const selectedGoodsId = ref<string>('');

// 加载账号列表
const loadAccounts = async () => {
  try {
    const response = await getAccountList();
    if (response.code === 0 || response.code === 200) {
      accounts.value = response.data?.accounts || [];
      // 默认选择第一个账号
      if (accounts.value.length > 0 && !selectedAccountId.value) {
        selectedAccountId.value = accounts.value[0].id;
        loadGoods();
      }
    }
  } catch (error: any) {
    showError('加载账号列表失败: ' + error.message);
  }
};

// 加载商品列表
const loadGoods = async () => {
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

    if (statusFilter.value !== '') {
      params.status = parseInt(statusFilter.value);
    }

    const response = await getGoodsList(params);
    if (response.code === 0 || response.code === 200) {
      goodsList.value = response.data?.itemsWithConfig || [];
      total.value = response.data?.totalCount || 0;
    } else {
      throw new Error(response.msg || '获取商品列表失败');
    }
  } catch (error: any) {
    showError('加载商品列表失败: ' + error.message);
    goodsList.value = [];
  } finally {
    loading.value = false;
  }
};

// 刷新商品数据
const handleRefresh = async () => {
  if (!selectedAccountId.value) {
    showInfo('请先选择账号');
    return;
  }

  refreshing.value = true;
  try {
    const response = await refreshGoods(selectedAccountId.value);
    if (response.code === 0 || response.code === 200) {
      showSuccess('商品数据刷新成功');
      await loadGoods();
    } else {
      throw new Error(response.msg || '刷新商品数据失败');
    }
  } catch (error: any) {
    showError('刷新商品数据失败: ' + error.message);
  } finally {
    refreshing.value = false;
  }
};

// 账号变更
const handleAccountChange = () => {
  currentPage.value = 1;
  loadGoods();
};

// 状态筛选
const handleStatusFilter = () => {
  currentPage.value = 1;
  loadGoods();
};

// 分页变更
const handlePageChange = (page: number) => {
  currentPage.value = page;
  loadGoods();
};

// 查看详情
const handleViewDetail = (xyGoodId: string) => {
  selectedGoodsId.value = xyGoodId;
  detailDialogVisible.value = true;
};

// 切换自动发货
const handleToggleAutoDelivery = async (item: GoodsItemWithConfig, value: boolean) => {
  if (!selectedAccountId.value) return;

  try {
    const response = await updateAutoDeliveryStatus({
      xianyuAccountId: selectedAccountId.value,
      xyGoodsId: item.item.xyGoodId,
      xianyuAutoDeliveryOn: value ? 1 : 0
    });

    if (response.code === 0 || response.code === 200) {
      showSuccess(`自动发货${value ? '开启' : '关闭'}成功`);
      item.xianyuAutoDeliveryOn = value ? 1 : 0;
    } else {
      throw new Error(response.msg || '操作失败');
    }
  } catch (error: any) {
    showError('操作失败: ' + error.message);
    // 恢复开关状态
    item.xianyuAutoDeliveryOn = value ? 0 : 1;
  }
};

// 切换自动回复
const handleToggleAutoReply = async (item: GoodsItemWithConfig, value: boolean) => {
  if (!selectedAccountId.value) return;

  try {
    const response = await updateAutoReplyStatus({
      xianyuAccountId: selectedAccountId.value,
      xyGoodsId: item.item.xyGoodId,
      xianyuAutoReplyOn: value ? 1 : 0
    });

    if (response.code === 0 || response.code === 200) {
      showSuccess(`自动回复${value ? '开启' : '关闭'}成功`);
      item.xianyuAutoReplyOn = value ? 1 : 0;
    } else {
      throw new Error(response.msg || '操作失败');
    }
  } catch (error: any) {
    showError('操作失败: ' + error.message);
    // 恢复开关状态
    item.xianyuAutoReplyOn = value ? 0 : 1;
  }
};

// 获取状态标签类型
const getStatusType = (status: number) => {
  const statusMap: Record<number, string> = {
    0: 'success',
    1: 'info',
    2: 'warning'
  };
  return statusMap[status] || 'info';
};

// 获取状态文本
const getStatusText = (status: number) => {
  const statusMap: Record<number, string> = {
    0: '在售',
    1: '已下架',
    2: '已售出'
  };
  return statusMap[status] || '未知';
};

// 格式化价格
const formatPrice = (price: string) => {
  return price ? `¥${price}` : '-';
};

onMounted(() => {
  loadAccounts();
});
</script>

<template>
  <div class="goods-page">
    <div class="page-header">
      <h1 class="page-title">商品管理</h1>
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
        
        <el-select
          v-model="statusFilter"
          placeholder="全部状态"
          style="width: 150px"
          clearable
          @change="handleStatusFilter"
        >
          <el-option label="在售商品" value="0" />
          <el-option label="已下架" value="1" />
          <el-option label="已售出" value="2" />
        </el-select>
        
        <el-button @click="loadGoods">刷新列表</el-button>
        <el-button type="primary" :loading="refreshing" @click="handleRefresh">
          同步闲鱼商品
        </el-button>
      </div>
    </div>

    <el-card class="goods-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">商品列表</span>
          <span class="card-subtitle">共 {{ total }} 件商品</span>
        </div>
      </template>

      <el-table
        v-loading="loading"
        :data="goodsList"
        stripe
        style="width: 100%"
      >
        <el-table-column type="index" label="序号" width="60" align="center" />
        
        <el-table-column prop="item.xyGoodId" label="商品ID" width="150">
          <template #default="{ row }">
            <div class="goods-id">{{ row.item.xyGoodId }}</div>
          </template>
        </el-table-column>
        
        <el-table-column label="商品图片" width="100">
          <template #default="{ row }">
            <el-image
              :src="row.item.coverPic"
              fit="cover"
              class="goods-image"
              :preview-src-list="[row.item.coverPic]"
            />
          </template>
        </el-table-column>
        
        <el-table-column prop="item.title" label="商品标题" min-width="200" show-overflow-tooltip />
        
        <el-table-column label="价格" width="120" align="right">
          <template #default="{ row }">
            <span class="goods-price">{{ formatPrice(row.item.soldPrice) }}</span>
          </template>
        </el-table-column>
        
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.item.status)" size="small">
              {{ getStatusText(row.item.status) }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column label="自动发货" width="100" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.xianyuAutoDeliveryOn === 1"
              @change="(val: boolean) => handleToggleAutoDelivery(row, val)"
            />
          </template>
        </el-table-column>
        
        <el-table-column label="自动回复" width="100" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.xianyuAutoReplyOn === 1"
              @change="(val: boolean) => handleToggleAutoReply(row, val)"
            />
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              link
              size="small"
              @click="handleViewDetail(row.item.xyGoodId)"
            >
              查看详情
            </el-button>
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

    <!-- 商品详情对话框 -->
    <GoodsDetailDialog
      v-model="detailDialogVisible"
      :goods-id="selectedGoodsId"
      :account-id="selectedAccountId"
      @refresh="loadGoods"
    />
  </div>
</template>

<style scoped>
.goods-page {
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

.goods-card {
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

.goods-id {
  font-family: 'Courier New', Consolas, monospace;
  font-size: 12px;
  color: #606266;
  word-break: break-all;
}

.goods-image {
  width: 60px;
  height: 60px;
  border-radius: 4px;
}

.goods-price {
  font-size: 16px;
  font-weight: 600;
  color: #f56c6c;
}

.pagination-container {
  display: flex;
  justify-content: center;
  padding: 20px 0;
  margin-top: 20px;
  border-top: 1px solid #ebeef5;
}
</style>
