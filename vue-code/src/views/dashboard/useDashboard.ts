import { ref, reactive } from 'vue'
import { getDashboardStats } from '@/api/dashboard'

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
      const res = await getDashboardStats()
      if (res.code === 0 || res.code === 200) {
        stats.accountCount = res.data.accountCount
        stats.goodsCount = res.data.itemCount
        stats.onlineGoodsCount = res.data.sellingItemCount
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