<template>
  <div class="traffic-card art-card h-105 p-5 mb-5 max-sm:mb-4">
    <!-- header -->
    <div class="art-card-header">
      <div class="title">
        <h4>流量监控</h4>
        <p>最近 24 小时上下行流量趋势</p>
      </div>
    </div>

    <!-- metrics -->
    <div class="metrics-grid">
      <div v-for="item in metrics" :key="item.label" class="metric-item">
        <div class="metric-dot" :style="{ background: item.color }" />

        <div class="metric-label">
          {{ item.label }}
        </div>

        <div class="metric-value">
          {{ item.value }}
        </div>
      </div>
    </div>

    <ArtLineChart
      height="calc(100% - 140px)"
      :data="data"
      :xAxisData="xAxisData"
      :showAreaColor="true"
      :showAxisLine="false"
      :showLegend="true"
      :yAxisLabelFormatter="yAxisLabelFormatter"
      :tooltipFormatter="tooltipFormatter"
    />
  </div>
</template>

<script setup lang="ts">
  import { ref, computed, onMounted } from 'vue'
  import { fetchGet24hMetrics } from '@/api/metrics'
  import { ByteUtils } from '@/utils/format/byteFormatter'

  const data = ref([
    {
      name: '下行流量',
      data: [] as number[],
      showAreaColor: true
    },
    {
      name: '上行流量',
      data: [] as number[],
      showAreaColor: true
    }
  ])

  const xAxisData = ref<string[]>([])
  const upTotal = ref(0)
  const downTotal = ref(0)
  const upRate = ref(0)
  const downRate = ref(0)

  // 统一单位（由数据最大值决定）
  const unitDivisor = ref(1)
  const unitLabel = ref('B')

  const metrics = computed(() => {
    return [
      {
        label: '上行速率',
        value: ByteUtils.formatBytes(upRate.value) + '/s',
        color: '#22c55e'
      },
      {
        label: '下行速率',
        value: ByteUtils.formatBytes(downRate.value) + '/s',
        color: '#f59e0b'
      },
      {
        label: '上行流量',
        value: ByteUtils.formatBytes(upTotal.value),
        color: '#3b82f6'
      },
      {
        label: '下行流量',
        value: ByteUtils.formatBytes(downTotal.value),
        color: '#8b5cf6'
      }
    ]
  })

  // Y轴标签（仅数值）
  const yAxisLabelFormatter = (value: number): string => {
    if (value <= 0) return '0'
    return parseFloat((value / unitDivisor.value).toFixed(2)).toString()
  }

  // Tooltip（带单位）
  const tooltipFormatter = (params: any[]): string => {
    if (!params || params.length === 0) return ''
    let html = `时间：${params[0].name}<br/>`
    params.forEach((item: any) => {
      html += `${item.marker} ${item.seriesName}: ${ByteUtils.formatBytes(item.value)}<br/>`
    })
    return html
  }

  const initData = async () => {
    const result = (await fetchGet24hMetrics()) as Api.Metrics.TrafficChartVO

    const downYAxis = result?.down?.yAxis || []
    const upYAxis = result?.up?.yAxis || []
    const allValues = [...downYAxis, ...upYAxis]

    const dataMax = allValues.length > 0 ? Math.max(...allValues, 0) : 0
    const unitInfo = ByteUtils.getUnitInfo(dataMax)
    unitDivisor.value = unitInfo.divisor
    unitLabel.value = unitInfo.unit

    data.value[0].data = downYAxis
    data.value[1].data = upYAxis
    const rawXAxis = result?.down?.xAxis || []
    // 后端返回 timeUnit='hour' 时为小时粒度，需加 :00
    xAxisData.value = result?.timeUnit === 'hour'
      ? rawXAxis.map((h: string) => `${h}:00`)
      : rawXAxis
    upTotal.value = result?.upTotal ?? 0
    downTotal.value = result?.downTotal ?? 0
    upRate.value = result?.upRate ?? 0
    downRate.value = result?.downRate ?? 0
  }

  onMounted(() => {
    initData()
  })
</script>

<style scoped lang="scss">
  .traffic-card {
    display: flex;
    flex-direction: column;
  }

  .metrics-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 14px;
    margin-bottom: 20px;
  }

  .metric-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 16px 18px;
    background: var(--el-fill-color-light);
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 14px;
    transition: all 0.2s ease;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 24px rgb(0 0 0 / 6%);
      border-color: var(--el-color-primary-light-5);
    }
  }

  .metric-dot {
    position: relative;
    width: 10px;
    height: 10px;
    border-radius: 50%;
    flex-shrink: 0;

    &::before {
      position: absolute;
      inset: 0;
      border-radius: 50%;
      background: inherit;
      content: '';
      animation: pulse 2s infinite;
    }
  }
  @keyframes pulse {
    0% {
      transform: scale(1);
      opacity: 0.8;
    }

    70% {
      transform: scale(2.4);
      opacity: 0;
    }

    100% {
      transform: scale(2.4);
      opacity: 0;
    }
  }

  .metric-label {
    color: var(--el-text-color-regular);
    font-size: 14px;
    white-space: nowrap;
  }

  .metric-value {
    margin-left: auto;
    font-size: 20px;
    font-weight: 700;
    color: var(--el-text-color-primary);
    white-space: nowrap;
  }

  @media (max-width: 1200px) {
    .metrics-grid {
      grid-template-columns: repeat(2, 1fr);
    }
  }

  @media (max-width: 768px) {
    .metrics-grid {
      grid-template-columns: 1fr;
    }

    .metric-value {
      font-size: 18px;
    }
  }
</style>
