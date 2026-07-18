<template>
  <div class="settings-page">
    <ElCard class="art-table-card settings-table-card">
      <ElTabs v-model="activeTab" type="card" class="settings-tabs" @tab-change="onTabChange">
        <ElTabPane label="三方登录" name="oauth">
          <div class="tab-panel">
            <OAuthPanel ref="oauthPanelRef" />
          </div>
        </ElTabPane>
      </ElTabs>
    </ElCard>
  </div>
</template>

<script setup lang="ts">
  import { onMounted, ref, watch } from 'vue'
  import { useRoute, useRouter } from 'vue-router'
  import OAuthPanel from './modules/oauth-panel.vue'

  defineOptions({ name: 'SystemSettings' })

  const route = useRoute()
  const router = useRouter()
  const activeTab = ref('oauth')
  const oauthPanelRef = ref<InstanceType<typeof OAuthPanel>>()

  const syncTabFromRoute = () => {
    const tab = (route.query.tab as string) || 'oauth'
    activeTab.value = tab === 'oauth' ? 'oauth' : 'oauth'
  }

  const onTabChange = (name: string | number) => {
    router.replace({ path: '/settings', query: { ...route.query, tab: String(name) } })
  }

  onMounted(() => {
    syncTabFromRoute()
  })

  watch(
    () => route.query.tab,
    () => syncTabFromRoute()
  )
</script>

<style lang="scss" scoped>
  .settings-table-card {
    flex: none;

    :deep(.el-card__body) {
      height: auto;
      overflow: visible;
    }
  }

  .settings-tabs {
    :deep(.el-tabs__header) {
      margin-bottom: 0;
    }
  }

  .tab-panel {
    padding: 20px 4px 24px;
    box-sizing: border-box;
  }
</style>
