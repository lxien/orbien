<template>
  <div class="domain-page art-full-height">
    <DomainOverview
      v-model:active="activeView"
      class="shrink-0"
      :base-count="summary.baseCount"
      :used-count="summary.usedCount"
    />

    <ElCard class="art-table-card">
      <DomainList v-if="activeView === 'pool'" @change="loadSummary" />
      <UsedDomainList v-else @change="loadSummary" />
    </ElCard>
  </div>
</template>

<script setup lang="ts">
  import { ref, reactive, onMounted } from 'vue'
  import DomainOverview, { type DomainView } from './modules/domain-overview.vue'
  import DomainList from './modules/domain-list.vue'
  import UsedDomainList from './modules/used-domain-list.vue'
  import { fetchGetDomainListByPage, fetchGetUsedDomainListByPage } from '@/api/domain'

  defineOptions({ name: 'DomainManagement' })

  const activeView = ref<DomainView>('pool')

  const summary = reactive({
    baseCount: 0,
    usedCount: 0
  })

  const loadSummary = async () => {
    try {
      const [baseRes, usedRes] = await Promise.all([
        fetchGetDomainListByPage({ current: 1, size: 1 }),
        fetchGetUsedDomainListByPage({ current: 1, size: 1 })
      ])
      summary.baseCount = baseRes.total ?? 0
      summary.usedCount = usedRes.total ?? 0
    } catch {
      // 概览统计失败时不阻断列表展示
    }
  }

  onMounted(() => {
    loadSummary()
  })
</script>
