<template>
  <div class="art-card h-105 p-5 mb-5 max-sm:mb-4">
    <div class="art-card-header">
      <div class="title">
        <h4>代理类型分布</h4>
        <p>HTTP、HTTPS 与 TCP 代理数量占比</p>
      </div>
    </div>
    <div class="flex items-center justify-center h-[calc(100%-56px)]">
      <ArtRingChart
        :data="proxyData"
        :radius="['0%', '70%']"
        :showLegend="true"
        legendPosition="bottom"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref, onMounted } from 'vue'
  import ArtRingChart from '@/components/core/charts/art-ring-chart/index.vue'
  import { fetchGetProxyProtocolStats } from '@/api/monitor'

  const proxyData = ref([
    {
      name: 'HTTP 代理',
      value: 0
    },
    {
      name: 'HTTPS 代理',
      value: 0
    },
    {
      name: 'TCP 代理',
      value: 0
    }
  ])

  /**
   * 获取代理协议统计数据
   */
  const getProxyProtocolStats = async () => {
    const data = await fetchGetProxyProtocolStats()
    if (data) {
      proxyData.value = [
        {
          name: 'HTTP 代理',
          value: data.httpCount
        },
        {
          name: 'HTTPS 代理',
          value: data.httpsCount
        },
        {
          name: 'TCP 代理',
          value: data.tcpCount
        }
      ]
    }
  }

  onMounted(() => {
    getProxyProtocolStats()
  })
</script>
