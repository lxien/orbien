<template>
  <div class="art-card h-105 p-5 mb-5 max-sm:mb-4">
    <div class="art-card-header">
      <div class="title">
        <h4>代理类型分布</h4>
        <p>HTTP、HTTPS、TCP、UDP、SOCKS5 与 FILE 代理数量占比</p>
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
import {ref, onMounted} from 'vue'
import ArtRingChart from '@/components/core/charts/art-ring-chart/index.vue'
import {fetchGetProxyProtocolStats} from '@/api/monitor'
import {ProtocolType, getProtocolLabel} from '@/enums/orbien/business'

defineOptions({name: 'ProxyStats'})

const loading = ref(false)

const buildProxyData = (counts: Partial<Api.Monitor.ProxyProtocolCountDTO> = {}) => [
  {name: getProtocolLabel(ProtocolType.HTTP), value: counts.httpCount ?? 0},
  {name: getProtocolLabel(ProtocolType.HTTPS), value: counts.httpsCount ?? 0},
  {name: getProtocolLabel(ProtocolType.TCP), value: counts.tcpCount ?? 0},
  {name: getProtocolLabel(ProtocolType.UDP), value: counts.udpCount ?? 0},
  {name: getProtocolLabel(ProtocolType.SOCKS5), value: counts.socks5Count ?? 0},
  {name: getProtocolLabel(ProtocolType.FILE), value: counts.fileCount ?? 0}
]

const proxyData = ref(buildProxyData())

const getProxyProtocolStats = async () => {
  loading.value = true
  try {
    const data = await fetchGetProxyProtocolStats()
    if (data) {
      proxyData.value = buildProxyData(data)
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  getProxyProtocolStats()
})
</script>
