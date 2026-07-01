<template>
  <div class="transport-page" v-loading="loading">
    <div class="border border-gray-200 rounded p-4">
      <div class="flex flex-col gap-5">
        <div class="flex items-center gap-3">
          <span class="w-24 font-medium shrink-0">TLS 加密</span>
          <ElSwitch v-model="form.encrypt" />
          <span class="text-sm text-gray-500">客户端到内网服务之间启用 TLS</span>
        </div>
        <div class="flex items-center gap-3">
          <span class="w-24 font-medium shrink-0">传输隧道</span>
          <ElRadioGroup v-model="form.tunnelType">
            <ElRadio label="0">共享隧道</ElRadio>
            <ElRadio label="1">独立隧道</ElRadio>
          </ElRadioGroup>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref, reactive, watch } from 'vue'
  import { fetchProxyDetail } from '@/api/proxy-plugin'
  import type { ProxyConfigProtocol } from '../../menus'

  defineOptions({ name: 'TransportPage' })

  const props = defineProps<{
    proxyId: string
    protocol: ProxyConfigProtocol
  }>()

  const loading = ref(false)
  const form = reactive({
    encrypt: false,
    tunnelType: '0'
  })

  const loadData = async () => {
    loading.value = true
    try {
      const detail = await fetchProxyDetail(props.protocol, props.proxyId)
      form.encrypt = detail.transport?.encrypt ?? false
      form.tunnelType = detail.transport?.tunnelType?.toString() ?? '0'
    } finally {
      loading.value = false
    }
  }

  watch(
    () => [props.proxyId, props.protocol] as const,
    ([proxyId]) => {
      if (proxyId) loadData()
    },
    { immediate: true }
  )
</script>
