<!-- 仪表盘 -->
<template>
  <div ref="chartRef" :style="{ height: props.height }" v-loading="props.loading"> </div>
</template>

<script setup lang="ts">
  import { useChartOps, useChartComponent } from '@/hooks/core/useChart'
  import type { EChartsOption } from '@/plugins/echarts'

  defineOptions({ name: 'ArtGaugeChart' })

  interface GaugeChartProps {
    // 基础配置
    height?: string
    loading?: boolean
    isEmpty?: boolean
    colors?: string[]

    // 数据配置
    value?: number
    name?: string
    min?: number
    max?: number

    // 交互配置
    showTooltip?: boolean
  }

  const props = withDefaults(defineProps<GaugeChartProps>(), {
    // 基础配置
    height: useChartOps().chartHeight,
    loading: false,
    isEmpty: false,
    colors: () => useChartOps().colors,

    // 数据配置
    value: 0,
    name: 'Score',
    min: 0,
    max: 100,

    // 交互配置
    showTooltip: true
  })

  // 使用新的图表组件抽象
  const { chartRef, getTooltipStyle } = useChartComponent({
    props,
    checkEmpty: () => {
      return props.value === undefined || props.value === null
    },
    watchSources: [() => props.value, () => props.name, () => props.min, () => props.max],
    generateOptions: (): EChartsOption => {
      const options: EChartsOption = {
        tooltip: props.showTooltip
          ? getTooltipStyle('item', {
              formatter: '{a} <br/>{b} : {c}%'
            })
          : undefined,
        series: [
          {
            name: props.name,
            type: 'gauge',
            min: props.min,
            max: props.max,
            splitNumber: 8,
            axisLine: {
              lineStyle: {
                width: 6,
                color: [
                  [0.3, '#67C23A'],
                  [0.7, '#E6A23C'],
                  [1, '#F56C6C']
                ]
              }
            },
            pointer: {
              show: true,
              length: '80%',
              itemStyle: {
                color: 'auto'
              }
            },
            axisTick: {
              show: true,
              length: 8,
              lineStyle: {
                color: 'auto',
                width: 1
              }
            },
            splitLine: {
              show: true,
              length: 12,
              lineStyle: {
                color: 'auto',
                width: 2
              }
            },
            axisLabel: {
              show: true,
              color: '#464646',
              fontSize: 12
            },
            title: {
              show: true,
              offsetCenter: [0, '-10%'],
              fontSize: 14
            },
            detail: {
              show: true,
              offsetCenter: [0, '10%'],
              valueAnimation: true,
              formatter: '{value}%',
              color: 'auto'
            },
            data: [
              {
                value: props.value,
                name: props.name
              }
            ]
          }
        ]
      }

      return options
    }
  })
</script>
