<template>
  <div class="art-card h-105 p-5 mb-5 max-sm:mb-4">
    <div class="art-card-header">
      <div class="title">
        <h4>代理类型分布</h4>
        <p>HTTP、HTTPS、TCP 与 UDP 代理数量占比</p>
      </div>
    </div>
    <div class="flex items-center justify-center h-[calc(100%-56px)]">
      <ArtRingChart
        :data="proxyData"
        :loading="loading"
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

  defineOptions({ name: 'ProxyStats' })

  const loading = ref(false)

  const proxyData = ref([
    { name: 'HTTP', value: 0 },
    { name: 'HTTPS', value: 0 },
    { name: 'TCP', value: 0 },
    { name: 'UDP', value: 0 }
  ])

  const getProxyProtocolStats = async () => {
    loading.value = true
    try {
      const data = await fetchGetProxyProtocolStats()
      if (data) {
        proxyData.value = [
          { name: 'HTTP', value: data.httpCount ?? 0 },
          { name: 'HTTPS', value: data.httpsCount ?? 0 },
          { name: 'TCP', value: data.tcpCount ?? 0 },
          { name: 'UDP', value: data.udpCount ?? 0 }
        ]
      }
    } finally {
      loading.value = false
    }
  }

  onMounted(() => {
    getProxyProtocolStats()
  })
</script>
