import * as echarts from 'echarts'
import { onUnmounted } from 'vue'

/**
 * 初始化ECharts图表
 * @param {string} domId - 图表容器ID
 * @param {Object} options - ECharts配置项
 * @returns {echarts.ECharts} 图表实例
 */
export function useECharts(domId, options = {}) {
  const chartDom = document.getElementById(domId)
  if (!chartDom) throw new Error(`未找到ID为${domId}的图表容器`)
  
  // 初始化图表（避免重复实例化）
  let chartInstance = echarts.getInstanceByDom(chartDom)
  if (!chartInstance) {
    chartInstance = echarts.init(chartDom)
  }

  // 设置图表配置
  chartInstance.setOption(options)

  // 窗口 resize 时自适应
  const handleResize = () => chartInstance.resize()
  window.addEventListener('resize', handleResize)

  // 组件卸载时清理
  onUnmounted(() => {
    window.removeEventListener('resize', handleResize)
    chartInstance.dispose()
  })

  return {
    chartInstance,
    // 更新图表配置
    updateOptions: (newOptions) => chartInstance.setOption(newOptions)
  }
}