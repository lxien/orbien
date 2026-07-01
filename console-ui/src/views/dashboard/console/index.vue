<!-- 工作台页面 -->
<template>
  <div>
    <ServerStatus />
    <SystemOverview />
    <ElRow :gutter="20">
      <ElCol :sm="24" :md="24" :lg="24">
        <TrafficStats />
      </ElCol>
    </ElRow>
    <ElRow :gutter="20" class="console-stats-row">
      <ElCol :sm="24" :md="12" :lg="12" class="console-stats-col">
        <ProxyStats />
      </ElCol>
      <ElCol :sm="24" :md="12" :lg="12" class="console-stats-col">
        <ServerConfig />
      </ElCol>
    </ElRow>
  </div>
</template>

<script setup lang="ts">
  import ServerStatus from './modules/server-status.vue'
  import SystemOverview from './modules/system-overview.vue'
  import ProxyStats from './modules/proxy-stats.vue'
  import TrafficStats from './modules/traffic-stats.vue'
  import ServerConfig from './modules/server-config.vue'
  defineOptions({ name: 'Console' })
</script>

<style scoped lang="scss">
  .console-stats-row {
    align-items: stretch;
  }

  .console-stats-col {
    display: flex;
    flex-direction: column;
  }
</style>
