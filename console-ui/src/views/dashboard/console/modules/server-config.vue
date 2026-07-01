<template>
  <div class="server-config art-card h-105 p-5 mb-5 max-sm:mb-4">
    <div class="config-header">
      <h4>服务器配置</h4>
    </div>

    <ElSkeleton v-if="loading" :rows="4" animated class="config-body" />

    <div v-else class="config-body">
      <div v-for="item in configItems" :key="item.label" class="config-item">
        <div class="config-icon" :class="item.iconBgClass">
          <ArtSvgIcon :icon="item.icon" class="icon" />
        </div>
        <div class="config-content">
          <div class="config-value">{{ item.value }}</div>
          <div class="config-label">{{ item.label }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { computed, onMounted, ref } from 'vue'
  import { ElSkeleton } from 'element-plus'
  import ArtSvgIcon from '@/components/core/base/art-svg-icon/index.vue'
  import { fetchGetAppConfig } from '@/api/app'

  defineOptions({ name: 'AppConfig' })

  const loading = ref(false)
  const configInfo = ref<Api.App.AppConfigInfoDTO | null>(null)

  const getData = async () => {
    loading.value = true
    try {
      configInfo.value = await fetchGetAppConfig()
    } finally {
      loading.value = false
    }
  }

  const configItems = computed(() => [
    {
      label: '服务器地址',
      value: configInfo.value?.serverAddr || '-',
      icon: 'ri:global-line',
      iconBgClass: 'icon-bg-blue'
    },
    {
      label: '服务器端口',
      value: configInfo.value?.serverPort || '-',
      icon: 'ri:server-line',
      iconBgClass: 'icon-bg-green'
    },
    {
      label: 'HTTP 代理',
      value: configInfo.value?.httpProxyPort || '-',
      icon: 'ri:router-line',
      iconBgClass: 'icon-bg-orange'
    },
    {
      label: 'HTTPS 代理',
      value: configInfo.value?.httpsProxyPort || '-',
      icon: 'ri:shield-keyhole-line',
      iconBgClass: 'icon-bg-teal'
    }
  ])

  onMounted(() => {
    getData()
  })
</script>

<style scoped lang="scss">
  .server-config {
    display: flex;
    flex-direction: column;
    min-height: 0;
  }

  .config-header {
    flex-shrink: 0;
    margin-bottom: 16px;

    h4 {
      margin: 0;
      font-size: 18px;
      font-weight: 500;
      color: var(--art-gray-900);
    }
  }

  .config-body {
    display: grid;
    flex: 1;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    grid-template-rows: repeat(2, minmax(0, 1fr));
    gap: 14px;
    min-height: 0;
  }

  .config-item {
    display: flex;
    gap: 14px;
    align-items: center;
    min-width: 0;
    min-height: 0;
    padding: 14px 16px;
    background: var(--art-gray-100);
    border-radius: 10px;
  }

  .config-icon {
    display: flex;
    flex-shrink: 0;
    align-items: center;
    justify-content: center;
    width: 44px;
    height: 44px;
    border-radius: 10px;
  }

  .icon-bg-blue {
    background: var(--el-color-primary-light-9);
  }

  .icon-bg-green {
    background: var(--el-color-success-light-9);
  }

  .icon-bg-orange {
    background: var(--el-color-warning-light-9);
  }

  .icon-bg-teal {
    background: var(--el-color-info-light-9);
  }

  .icon {
    font-size: 20px;
  }

  .icon-bg-blue .icon {
    color: var(--el-color-primary);
  }

  .icon-bg-green .icon {
    color: var(--el-color-success);
  }

  .icon-bg-orange .icon {
    color: var(--el-color-warning);
  }

  .icon-bg-teal .icon {
    color: var(--el-color-info);
  }

  .config-content {
    min-width: 0;
  }

  .config-value {
    overflow: hidden;
    font-size: 16px;
    font-weight: 700;
    color: var(--title-color);
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .config-label {
    margin-top: 4px;
    font-size: 13px;
    color: var(--el-text-color-secondary);
  }

  @media (max-width: 768px) {
    .config-body {
      flex: none;
      grid-template-columns: 1fr;
      grid-template-rows: repeat(4, auto);
    }
  }
</style>
