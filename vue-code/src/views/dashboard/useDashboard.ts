import { ref, reactive } from 'vue'
import { getAccountList } from '@/api/account'
import { getGoodsList } from '@/api/goods'
import type { Goods } from '@/types'

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
      if (accountRes.code === 0 && accountRes.data) {
        stats.accountCount = accountRes.data.length
      }

      // 加载商品统计
      const goodsRes = await getGoodsList({})
      if (goodsRes.code === 0 && goodsRes.data) {
        stats.goodsCount = goodsRes.data.length
        stats.onlineGoodsCount = goodsRes.data.filter((g: Goods) => g.status === 0).length
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
