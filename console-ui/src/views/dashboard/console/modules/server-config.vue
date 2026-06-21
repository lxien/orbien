<template>
  <div class="app-config">
    <div class="art-card app-config-card">
      <div class="header">
        <div>
          <div class="title">服务器配置</div>
          <div class="subtitle">ETP 服务器运行时配置</div>
        </div>
      </div>

      <ElSkeleton v-if="loading" :rows="6" animated />

      <div v-else class="config-grid">
        <div v-for="item in configItems" :key="item.label" class="config-item art-card">
          <div class="config-icon" :class="item.iconBgClass">
            <ArtSvgIcon :icon="item.icon" class="icon" />
          </div>

          <div class="config-content">
            <div class="config-value">
              {{ item.value }}
            </div>

            <div class="config-label">
              {{ item.label }}
            </div>
          </div>
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
      icon: 'ri:router-line',
      iconBgClass: 'icon-bg-orange'
    },
    {
      label: '基础域名',
      value: configInfo.value?.baseDomain || '-',
      icon: 'ri:earth-line',
      iconBgClass: 'icon-bg-purple'
    },
    {
      label: '端口范围',
      value: `${configInfo.value?.portStart || '-'} ~ ${configInfo.value?.portEnd || '-'}`,
      icon: 'ri:settings-3-line',
      iconBgClass: 'icon-bg-blue'
    }
  ])

  onMounted(() => {
    getData()
  })
</script>

<style scoped lang="scss">
  .app-config {
    margin-bottom: 20px;
  }

  .app-config-card {
    padding: 24px;
  }

  .header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 24px;

    .title {
      font-size: 18px;
      font-weight: 700;
      color: var(--title-color);
    }

    .subtitle {
      margin-top: 4px;
      font-size: 13px;
      color: var(--el-text-color-secondary);
    }
  }

  .config-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
  }

  .config-item {
    display: flex;
    gap: 16px;
    align-items: center;
    padding: 18px;
    transition: all 0.2s ease;

    &:hover {
      transform: translateY(-2px);
    }
  }

  .config-icon {
    width: 48px;
    height: 48px;
    border-radius: 14px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
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

  .icon-bg-purple {
    background: #f0e7ff;
  }

  .icon {
    font-size: 24px;
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

  .icon-bg-purple .icon {
    color: #7c3aed;
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
    .app-config-card {
      padding: 16px;
    }

    .config-grid {
      grid-template-columns: 1fr;
    }
  }
</style>
