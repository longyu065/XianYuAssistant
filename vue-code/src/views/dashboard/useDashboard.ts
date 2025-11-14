import { ref, reactive } from 'vue'
import { getAccountList } from '@/api/account'
import { getGoodsList } from '@/api/goods'
import type { Account } from '@/types'
import type { GoodsListResponse } from '@/api/goods'

export function useDashboard() {
  const loading = ref(false)
  
  const stats = reactive({
    accountCount: 0,
    goodsCount: 0,
    onlineGoodsCount: 0
  })

  const loadStatistics = async () => {
    loading.value = true
    try {
      // 加载账号统计
      const accountRes = await getAccountList()
      if (accountRes.code === 0 || accountRes.code === 200) {
        stats.accountCount = accountRes.data?.accounts?.length || 0
      }

      // 加载商品统计
      const goodsRes = await getGoodsList({ xianyuAccountId: 0 })
      if (goodsRes.code === 0 || goodsRes.code === 200) {
        const goodsData = goodsRes.data || { itemsWithConfig: [], totalCount: 0 }
        stats.goodsCount = goodsData.totalCount || 0
        stats.onlineGoodsCount = goodsData.itemsWithConfig?.filter((g: any) => g.item?.status === 0).length || 0
      }
    } catch (error) {
      console.error('加载统计数据失败:', error)
    } finally {
      loading.value = false
    }
  }

  return {
    loading,
    stats,
    loadStatistics
  }
}