<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { getAccountList } from '@/api/account';
import { getGoodsList, getGoodsDetail, updateAutoDeliveryStatus } from '@/api/goods';
import {
  getAutoDeliveryConfig,
  saveOrUpdateAutoDeliveryConfig,
  type AutoDeliveryConfig,
  type SaveAutoDeliveryConfigReq,
  type GetAutoDeliveryConfigReq
} from '@/api/auto-delivery-config';
import { showSuccess, showError, showInfo } from '@/utils';
import type { Account } from '@/types';
import type { GoodsItemWithConfig } from '@/api/goods';
import GoodsDetailDialog from '../goods/components/GoodsDetailDialog.vue';

const loading = ref(false);
const saving = ref(false);
const accounts = ref<Account[]>([]);
const selectedAccountId = ref<number | null>(null);
const goodsList = ref<GoodsItemWithConfig[]>([]);
const selectedGoods = ref<GoodsItemWithConfig | null>(null);
const currentConfig = ref<AutoDeliveryConfig | null>(null);

// 商品详情对话框
const detailDialogVisible = ref(false);
const selectedGoodsId = ref<string>('');

// 表单数据
const configForm = ref({
  type: 1,
  autoDeliveryContent: ''
});

// 格式化时间
const formatTime = (time: string) => {
  if (!time) return '-';
  // 将ISO时间格式转换为 YYYY-MM-DD HH:mm:ss
  return time.replace('T', ' ').substring(0, 19);
};

// 加载账号列表
const loadAccounts = async () => {
  try {
    const response = await getAccountList();
    if (response.code === 0 || response.code === 200) {
      accounts.value = response.data?.accounts || [];
      // 默认选择第一个账号
      if (accounts.value.length > 0 && !selectedAccountId.value) {
        selectedAccountId.value = accounts.value[0]?.id || null;
        loadGoods();
      }
    }
  } catch (error: any) {
    console.error('加载账号列表失败:', error);
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
    const params = {
      xianyuAccountId: selectedAccountId.value,
      pageNum: 1,
      pageSize: 100 // 获取所有商品
    };

    const response = await getGoodsList(params);
    if (response.code === 0 || response.code === 200) {
      goodsList.value = response.data?.itemsWithConfig || [];
      // 默认选择第一个商品
      if (goodsList.value.length > 0 && !selectedGoods.value) {
        if (goodsList.value.length > 0) {
          selectGoods(goodsList.value[0]!);
        }
      }
    } else {
      throw new Error(response.msg || '获取商品列表失败');
    }
  } catch (error: any) {
    console.error('加载商品列表失败:', error);
    goodsList.value = [];
  } finally {
    loading.value = false;
  }
};

// 账号变更
const handleAccountChange = () => {
  selectedGoods.value = null;
  currentConfig.value = null;
  loadGoods();
};

// 选择商品
const selectGoods = async (goods: GoodsItemWithConfig) => {
  selectedGoods.value = goods;
  await loadConfig();
};

// 加载配置
const loadConfig = async () => {
  if (!selectedGoods.value || !selectedAccountId.value) return;

  try {
    const req: GetAutoDeliveryConfigReq = {
      xianyuAccountId: selectedAccountId.value,
      xyGoodsId: selectedGoods.value.item.xyGoodId
    };

    const response = await getAutoDeliveryConfig(req);
    if (response.code === 0 || response.code === 200) {
      currentConfig.value = response.data || null;
      if (response.data) {
        configForm.value.type = response.data.type;
        configForm.value.autoDeliveryContent = response.data.autoDeliveryContent || '';
      } else {
        // 重置表单
        configForm.value.type = 1;
        configForm.value.autoDeliveryContent = '';
      }
    } else {
      throw new Error(response.msg || '获取配置失败');
    }
  } catch (error: any) {
    console.error('加载配置失败:', error);
    currentConfig.value = null;
  }
};

// 保存配置
const saveConfig = async () => {
  if (!selectedGoods.value || !selectedAccountId.value) {
    showInfo('请先选择商品');
    return;
  }

  if (!configForm.value.autoDeliveryContent.trim()) {
    showInfo('请输入自动发货内容');
    return;
  }

  saving.value = true;
  try {
    const req: SaveAutoDeliveryConfigReq = {
      xianyuAccountId: selectedAccountId.value,
      xianyuGoodsId: selectedGoods.value.item.id,
      xyGoodsId: selectedGoods.value.item.xyGoodId,
      type: configForm.value.type,
      autoDeliveryContent: configForm.value.autoDeliveryContent.trim()
    };

    const response = await saveOrUpdateAutoDeliveryConfig(req);
    if (response.code === 0 || response.code === 200) {
      showSuccess('保存配置成功');
      currentConfig.value = response.data || null;
    } else {
      throw new Error(response.msg || '保存配置失败');
    }
  } catch (error: any) {
    console.error('保存配置失败:', error);
  } finally {
    saving.value = false;
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

// 查看商品详情
const viewGoodsDetail = () => {
  if (!selectedGoods.value || !selectedAccountId.value) {
    showInfo('请先选择商品');
    return;
  }

  selectedGoodsId.value = selectedGoods.value.item.xyGoodId;
  detailDialogVisible.value = true;
};

// 切换自动发货状态
const toggleAutoDelivery = async (value: boolean) => {
  if (!selectedGoods.value || !selectedAccountId.value) {
    showInfo('请先选择商品');
    return;
  }

  try {
    const response = await updateAutoDeliveryStatus({
      xianyuAccountId: selectedAccountId.value,
      xyGoodsId: selectedGoods.value.item.xyGoodId,
      xianyuAutoDeliveryOn: value ? 1 : 0
    });

    if (response.code === 0 || response.code === 200) {
      showSuccess(`自动发货${value ? '开启' : '关闭'}成功`);
      // 更新本地状态
      if (selectedGoods.value) {
        selectedGoods.value.xianyuAutoDeliveryOn = value ? 1 : 0;
      }
      // 同时更新商品列表中的状态
      const goodsItem = goodsList.value.find(item => item.item.xyGoodId === selectedGoods.value?.item.xyGoodId);
      if (goodsItem) {
        goodsItem.xianyuAutoDeliveryOn = value ? 1 : 0;
      }
    } else {
      throw new Error(response.msg || '操作失败');
    }
  } catch (error: any) {
    console.error('操作失败:', error);
    // 恢复开关状态
    if (selectedGoods.value) {
      selectedGoods.value.xianyuAutoDeliveryOn = value ? 0 : 1;
    }
  }
};

onMounted(() => {
  loadAccounts();
});
</script>

<template>
  <div class="auto-delivery-page">
    <div class="page-header">
      <h1 class="page-title">自动发货配置</h1>
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
      </div>
    </div>

    <div class="content-container">
      <!-- 左侧商品列表 -->
      <div class="goods-panel">
        <el-card class="goods-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">商品列表</span>
              <span class="card-subtitle">共 {{ goodsList.length }} 件商品</span>
            </div>
          </template>

          <div class="goods-list" v-loading="loading">
            <div
              v-for="goods in goodsList"
              :key="goods.item.xyGoodId"
              :id="`goods-item-${goods.item.id}-${Math.random().toString(36).substr(2, 9)}`"
              class="goods-item"
              :class="{ active: selectedGoods?.item.xyGoodId === goods.item.xyGoodId }"
              @click="selectGoods(goods)"
            >
              <el-image
                :src="goods.item.coverPic"
                fit="cover"
                class="goods-image"
              />
              <div class="goods-info">
                <div class="goods-title">{{ goods.item.title }}</div>
                <div class="goods-meta">
                  <span class="goods-price">{{ formatPrice(goods.item.soldPrice) }}</span>
                  <el-tag :type="getStatusType(goods.item.status)" size="small">
                    {{ getStatusText(goods.item.status) }}
                  </el-tag>
                </div>
              </div>
            </div>

            <div v-if="goodsList.length === 0 && !loading" class="empty-goods">
              <el-empty description="暂无商品" />
            </div>
          </div>
        </el-card>
      </div>

      <!-- 右侧配置面板 -->
      <div class="config-panel">
        <el-card class="config-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">自动发货配置</span>
              <el-button
                v-if="selectedGoods"
                type="primary"
                size="small"
                @click="viewGoodsDetail"
              >
                查看商品详情
              </el-button>
              <span class="card-subtitle" v-else>
                请选择商品
              </span>
            </div>
          </template>

          <div class="config-form" v-if="selectedGoods">
            <el-form :model="configForm" label-width="100px">
              <el-form-item label="自动发货">
                <el-switch
                  v-model="selectedGoods.xianyuAutoDeliveryOn"
                  :active-value="1"
                  :inactive-value="0"
                  @change="toggleAutoDelivery"
                />
                <span class="switch-label">
                  {{ selectedGoods.xianyuAutoDeliveryOn === 1 ? '已开启' : '已关闭' }}
                </span>
              </el-form-item>

              <el-form-item label="发货类型">
                <el-radio-group v-model="configForm.type">
                  <el-radio :value="1">文本内容</el-radio>
                  <el-radio :value="2">自定义</el-radio>
                </el-radio-group>
              </el-form-item>

              <el-form-item label="发货内容">
                <el-input
                  v-model="configForm.autoDeliveryContent"
                  type="textarea"
                  :rows="8"
                  placeholder="请输入自动发货内容，买家下单后将自动发送此内容"
                  maxlength="1000"
                  show-word-limit
                />
              </el-form-item>

              <el-form-item>
                <div class="save-config-container">
                  <el-button type="primary" :loading="saving" @click="saveConfig">
                    保存配置
                  </el-button>
                  <span v-if="currentConfig" class="last-update-time">
                    上次更新: {{ formatTime(currentConfig.updateTime) }}
                  </span>
                </div>
              </el-form-item>
            </el-form>
          </div>

          <div v-else class="empty-config">
            <el-empty description="请选择左侧商品进行配置" />
          </div>
        </el-card>
      </div>
    </div>

    <!-- 商品详情对话框 -->
    <GoodsDetailDialog
      v-model="detailDialogVisible"
      :goods-id="selectedGoodsId"
      :account-id="selectedAccountId"
    />
  </div>
</template>

<style scoped>
.auto-delivery-page {
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

.header-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.content-container {
  flex: 1;
  display: flex;
  gap: 15px;
  min-height: 0;
}

.goods-panel {
  flex: 1;
  min-width: 0;
  max-width: 400px;
}

.config-panel {
  flex: 2;
  min-width: 0;
}

.goods-card,
.config-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 17px;
  font-weight: 600;
  color: #303133;
}

.card-subtitle {
  font-size: 13px;
  color: #909399;
}

.goods-list {
  flex: 1;
  overflow-y: auto;
}

.goods-item {
  display: flex;
  align-items: center;
  padding: 10px;
  border: 1px solid #ebeef5;
  border-radius: 3px;
  margin-bottom: 6px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.goods-item:hover {
  background-color: #f5f7fa;
  border-color: #c0c4cc;
}

.goods-item.active {
  background-color: #ecf5ff;
  border-color: #409eff;
}

.goods-image {
  width: 50px;
  height: 50px;
  border-radius: 3px;
  margin-right: 10px;
  flex-shrink: 0;
}

.goods-info {
  flex: 1;
  min-width: 0;
}

.goods-title {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.goods-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.goods-price {
  font-size: 15px;
  font-weight: 600;
  color: #f56c6c;
}

.empty-goods,
.empty-config {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 200px;
}

.config-form {
  padding: 15px 0;
}

.config-form .el-form-item:first-child {
  margin-bottom: 20px;
}

.config-time {
  color: #909399;
  font-size: 13px;
}

.switch-label {
  margin-left: 10px;
  font-size: 14px;
  color: #606266;
}

.save-config-container {
  display: flex;
  align-items: center;
  gap: 15px;
}

.last-update-time {
  font-size: 12px;
  color: #909399;
}
</style>
