<template>
  <div class="test-page art-full-height">
    <ElCard class="art-table-card">
      <h2>Test Page</h2>
      <p>This is a test page for development purposes.</p>
      <ElSpace wrap>
        <ElButton type="primary" @click="openDialog(ProtocolType.HTTP)">HTTP 配置</ElButton>
        <ElButton type="success" @click="openDialog(ProtocolType.HTTPS)">HTTPS 配置</ElButton>
        <ElButton @click="openDialog(ProtocolType.TCP)">TCP 配置</ElButton>
      </ElSpace>
    </ElCard>

    <TestDialog v-model:visible="dialogVisible" :protocol="currentProtocol" />
  </div>
</template>

<script setup lang="ts">
  import { ref } from 'vue'
  import { ProtocolType } from '@/enums/businessEnum'
  import TestDialog from './modules/test-dialog.vue'
  import type { ProxyConfigProtocol } from './modules/proxy-config'

  defineOptions({ name: 'Test' })

  const dialogVisible = ref(false)
  const currentProtocol = ref<ProxyConfigProtocol>(ProtocolType.HTTP)

  const openDialog = (protocol: ProxyConfigProtocol) => {
    currentProtocol.value = protocol
    dialogVisible.value = true
  }
</script>
