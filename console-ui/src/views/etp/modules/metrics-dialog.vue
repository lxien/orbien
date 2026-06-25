<template>
  <ElDialog v-model="dialogVisible" title="流量统计" width="1050px" align-center>
    <div v-if="loading" class="my-5">
      <ElSkeleton :rows="10" animated />
    </div>
    <div v-else>
      <div class="flex justify-between items-center mb-5">
        <h2 class="text-xl font-semibold m-0 text-g-900">流量统计概览</h2>
        <ElButton type="primary" @click="getData" v-ripple>
          <template #icon>
            <ElIcon><Refresh /></ElIcon>
          </template>
          刷新
        </ElButton>
      </div>
      <div class="grid grid-cols-1 md:grid-cols-5 gap-5 mb-5">
        <ArtStatsCard
          title="连接数"
          :count="ByteUtils.formatNumber(metricsData.activeChannels || 0)"
          description="当前活跃连接"
          icon="ri:share-line"
          iconStyle="bg-cyan-500"
        />
        <ArtStatsCard
          title="上行流量"
          :count="ByteUtils.formatNumber(metricsData.upTotal || 0)"
          :description="ByteUtils.formatBytes(metricsData.upTotal || 0)"
          icon="ri:arrow-up-line"
          iconStyle="bg-green-500"
        />
        <ArtStatsCard
          title="下行流量"
          :count="ByteUtils.formatNumber(metricsData.downTotal || 0)"
          :description="ByteUtils.formatBytes(metricsData.downTotal || 0)"
          icon="ri:arrow-down-line"
          iconStyle="bg-orange-500"
        />
        <ArtStatsCard
          title="上行速率"
          :count="ByteUtils.formatNumber(metricsData.upRate || 0)"
          :description="ByteUtils.formatBytes(metricsData.upRate || 0) + '/s'"
          icon="ri:arrow-up-circle-line"
          iconStyle="bg-purple-500"
        />
        <ArtStatsCard
          title="下行速率"
          :count="ByteUtils.formatNumber(metricsData.downRate || 0)"
          :description="ByteUtils.formatBytes(metricsData.downRate || 0) + '/s'"
          icon="ri:arrow-down-circle-line"
          iconStyle="bg-indigo-500"
        />
      </div>
      <div class="pt-1">
        <div class="art-card-sm p-4 mb-6">
          <div class="flex justify-between items-center mb-4">
            <h3 class="text-lg font-medium m-0 text-g-900">流量趋势</h3>
            <div v-if="showTimeRange" class="flex items-center gap-2">
              <ElSelect
                v-model="timeRange"
                placeholder="选择时间范围"
                size="default"
                style="width: 140px"
              >
                <ElOption label="最近24小时" value="24h" />
                <ElOption label="最近3天" value="3d" />
                <ElOption label="最近7天" value="7d" />
                <ElOption label="最近15天" value="15d" />
                <ElOption label="自定义日期" value="custom" />
              </ElSelect>
              <ElDatePicker
                v-if="timeRange === 'custom'"
                v-model="customDate"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                size="default"
                :disabled-date="disabledDate"
                @calendar-change="handleCalendarChange"
              />
            </div>
          </div>
          <ArtLineChart
            height="300px"
            :data="lineChartData"
            :xAxisData="lineChartXAxis"
            :showAreaColor="true"
            :showAxisLine="false"
            :showLegend="true"
            :yAxisLabelFormatter="yAxisLabelFormatter"
            :tooltipFormatter="tooltipFormatter"
          />
        </div>
        <div class="flex gap-5">
          <div class="art-card-sm p-4 flex-1">
            <h3 class="text-lg font-medium m-0 mb-4 text-g-900">流量对比</h3>
            <ArtRingChart
              :data="[
                { name: '上行流量', value: metricsData.upTotal || 0 },
                { name: '下行流量', value: metricsData.downTotal || 0 }
              ]"
              :colors="['#409EFF', '#67C23A']"
              :showLegend="true"
              :showLabel="true"
              :centerText="`总流量\n${ByteUtils.formatBytes((metricsData.upTotal || 0) + (metricsData.downTotal || 0))}`"
              :radius="['40%', '70%']"
              height="300px"
            />
          </div>
          <div class="art-card-sm p-4 flex-1">
            <h3 class="text-lg font-medium m-0 mb-4 text-g-900">速率对比</h3>
            <ArtHBarChart
              :data="[
                { name: '上行速率', data: [ByteUtils.formatNumber(metricsData.upRate || 0)] },
                { name: '下行速率', data: [ByteUtils.formatNumber(metricsData.downRate || 0)] }
              ]"
              :xAxisData="['速率']"
              :colors="['#409EFF', '#67C23A']"
              :showLegend="true"
              height="300px"
            />
          </div>
        </div>
      </div>
    </div>
    <template #footer>
      <div class="dialog-footer">
        <ElButton @click="dialogVisible = false">关闭</ElButton>
      </div>
    </template>
  </ElDialog>
</template>

<script setup lang="ts">
  import { ref, watch, computed } from 'vue'
  import ArtHBarChart from '@/components/core/charts/art-h-bar-chart/index.vue'
  import ArtRingChart from '@/components/core/charts/art-ring-chart/index.vue'
  import ArtLineChart from '@/components/core/charts/art-line-chart/index.vue'
  import ArtStatsCard from '@/components/core/cards/art-stats-card/index.vue'
  import { fetchGetProxyMetrics } from '@/api/metrics'
  import { ByteUtils } from '@/utils/format/byteFormatter'
  import { ElButton, ElIcon } from 'element-plus'
  import { Refresh } from '@element-plus/icons-vue'

  interface Props {
    visible: boolean
    proxyId: string
    showTimeRange?: boolean
  }

  interface Emits {
    (e: 'update:visible', value: boolean): void
  }

  const props = defineProps<Props>()
  const emit = defineEmits<Emits>()

  const dialogVisible = computed({
    get: () => props.visible,
    set: (value) => emit('update:visible', value)
  })

  const DEFAULT_TIME_RANGE = '24h'

  const loading = ref(false)
  const timeRange = ref(DEFAULT_TIME_RANGE)
  const customDate = ref<string | [string, string] | ''>('')
  const startDateAnchor = ref<Date | null>(null)

  const createEmptyMetricsData = (): Api.Metrics.TrafficChartVO => ({
    up: { xAxis: [], yAxis: [] },
    down: { xAxis: [], yAxis: [] },
    upTotal: 0,
    downTotal: 0,
    upRate: 0,
    downRate: 0,
    activeChannels: 0
  })

  const createEmptyLineChartData = () => [
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
  ]

  const metricsData = ref<Api.Metrics.TrafficChartVO>(createEmptyMetricsData())
  const lineChartData = ref(createEmptyLineChartData())
  const lineChartXAxis = ref<string[]>([])
  const unitDivisor = ref(1)
  const unitLabel = ref('B')

  /** 重置 timeRange 时不触发 timeRange 的 watch，避免重复请求 */
  let suppressRangeWatch = false

  const clearDisplayState = () => {
    metricsData.value = createEmptyMetricsData()
    lineChartData.value = createEmptyLineChartData()
    lineChartXAxis.value = []
    unitDivisor.value = 1
    unitLabel.value = 'B'
  }

  const resetDialogState = () => {
    suppressRangeWatch = true
    timeRange.value = DEFAULT_TIME_RANGE
    customDate.value = ''
    startDateAnchor.value = null
    clearDisplayState()
    suppressRangeWatch = false
  }

  const yAxisLabelFormatter = (value: number): string => {
    if (value <= 0) return '0'
    return Math.round(value / unitDivisor.value).toString()
  }

  const tooltipFormatter = (params: any[]): string => {
    if (!params || params.length === 0) return ''
    let html = `时间：${params[0].name}<br/>`
    params.forEach((item: any) => {
      html += `${item.marker} ${item.seriesName}: ${ByteUtils.formatBytes(item.value)}<br/>`
    })
    return html
  }

  const handleCalendarChange = (dates: Date[]) => {
    if (dates && dates.length > 0) {
      startDateAnchor.value = dates[0] || null
    } else {
      startDateAnchor.value = null
    }
  }

  const disabledDate = (time: Date): boolean => {
    const today = new Date()
    today.setHours(0, 0, 0, 0)

    if (time.getTime() > today.getTime()) {
      return true
    }

    if (startDateAnchor.value) {
      const maxEndDate = new Date(startDateAnchor.value)
      maxEndDate.setDate(maxEndDate.getDate() + 30)
      if (time.getTime() > maxEndDate.getTime()) {
        return true
      }
    }

    return false
  }

  const getData = async () => {
    if (!props.proxyId) return

    clearDisplayState()
    loading.value = true
    try {
      const requestParams: {
        proxyId: string
        queryType: string
        startDate?: string
        endDate?: string
      } = {
        proxyId: props.proxyId,
        queryType: timeRange.value
      }

      if (timeRange.value === 'custom' && customDate.value && customDate.value.length === 2) {
        requestParams.startDate = customDate.value[0]
        requestParams.endDate = customDate.value[1]
      }

      const response = (await fetchGetProxyMetrics(requestParams)) as Api.Metrics.TrafficChartVO
      if (response) {
        metricsData.value = response

        lineChartData.value[0].data = response.down?.yAxis || []
        lineChartData.value[1].data = response.up?.yAxis || []
        const rawXAxis = response.down?.xAxis || []
        // 后端返回 timeUnit='hour' 时为小时粒度，需加 :00
        lineChartXAxis.value =
          response.timeUnit === 'hour' ? rawXAxis.map((h: string) => `${h}:00`) : rawXAxis

        const allValues = [...(response.down?.yAxis || []), ...(response.up?.yAxis || [])]
        const dataMax = allValues.length > 0 ? Math.max(...allValues, 0) : 0
        const unitInfo = ByteUtils.getUnitInfo(dataMax)
        unitDivisor.value = unitInfo.divisor
        unitLabel.value = unitInfo.unit
      }
    } finally {
      loading.value = false
    }
  }

  watch(
    () => [props.visible, props.proxyId] as const,
    ([visible, proxyId], previous) => {
      if (!visible) {
        if (previous?.[0]) {
          resetDialogState()
        }
        return
      }
      const wasVisible = previous?.[0] ?? false
      const prevProxyId = previous?.[1] ?? ''
      if (!wasVisible || proxyId !== prevProxyId) {
        resetDialogState()
        getData()
      }
    },
    { immediate: true }
  )

  watch(timeRange, () => {
    if (suppressRangeWatch || !props.visible) return
    if (timeRange.value !== 'custom') {
      getData()
    }
  })

  watch(customDate, () => {
    if (
      props.visible &&
      timeRange.value === 'custom' &&
      customDate.value &&
      customDate.value.length === 2
    ) {
      getData()
    }
  })
</script>
