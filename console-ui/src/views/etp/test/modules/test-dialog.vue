<template>
  <ElDialog
    v-model="dialogVisible"
    :title="dialogTitle"
    width="960px"
    class="test-dialog"
    header-class="test-dialog-header"
    body-class="test-dialog-body"
    :show-close="true"
  >
    <div class="dialog-layout">
      <div class="test-dialog-sidebar layout-sidebar">
        <div
          class="menu-left menu-left-open"
          :class="`menu-left-${menuTheme}`"
          :style="{ background: menuBgColor }"
        >
          <ElScrollbar class="h-full">
            <ElMenu
              :key="protocol"
              :class="'el-menu-' + menuTheme"
              :default-active="activeMenu"
              :text-color="menuTextColor"
              :background-color="menuBgColor"
              @select="handleMenuSelect"
            >
              <ElMenuItem v-for="item in menuItems" :key="item.key" :index="item.key">
                <div class="menu-icon flex-cc">
                  <ArtSvgIcon :icon="item.icon" :style="{ color: menuIconColor }" />
                </div>
                <template #title>
                  <span class="menu-name">{{ item.label }}</span>
                </template>
              </ElMenuItem>
            </ElMenu>
          </ElScrollbar>
        </div>
      </div>

      <div class="dialog-content">
        <component :is="currentPageComponent" v-if="currentPageComponent" />
        <ElEmpty v-else description="功能开发中" />
      </div>
    </div>
  </ElDialog>
</template>

<script setup lang="ts">
  import { ref, computed, watch } from 'vue'
  import { useSettingStore } from '@/store/modules/setting'
  import { storeToRefs } from 'pinia'
  import { ProtocolType } from '@/enums/businessEnum'
  import ArtSvgIcon from '@/components/core/base/art-svg-icon/index.vue'
  import DomainPage from './domain-page.vue'
  import SslPage from './ssl-page.vue'
  import {
    type ProxyConfigProtocol,
    getProtocolMenus,
    getProtocolTitle
  } from './proxy-config'

  defineOptions({ name: 'TestDialog' })

  interface Props {
    visible: boolean
    protocol: ProxyConfigProtocol
  }

  interface Emits {
    (e: 'update:visible', value: boolean): void
  }

  const props = withDefaults(defineProps<Props>(), {
    protocol: ProtocolType.HTTP
  })
  const emit = defineEmits<Emits>()

  const settingStore = useSettingStore()
  const { getMenuTheme } = storeToRefs(settingStore)

  const dialogVisible = computed({
    get: () => props.visible,
    set: (value) => emit('update:visible', value)
  })

  const activeMenu = ref('')

  const menuTheme = computed(() => getMenuTheme.value.theme)
  const menuBgColor = computed(() => getMenuTheme.value.background)
  const menuTextColor = computed(() => getMenuTheme.value.textColor)
  const menuIconColor = computed(() => getMenuTheme.value.iconColor)

  const menuItems = computed(() => getProtocolMenus(props.protocol))
  const dialogTitle = computed(() => getProtocolTitle(props.protocol))

  const pageComponents: Record<string, any> = {
    ssl: SslPage,
    access: DomainPage
  }

  const resetActiveMenu = () => {
    activeMenu.value = menuItems.value[0]?.key ?? ''
  }

  watch(
    () => props.protocol,
    () => {
      resetActiveMenu()
    },
    { immediate: true }
  )

  watch(dialogVisible, (visible) => {
    if (visible) {
      resetActiveMenu()
    }
  })

  const handleMenuSelect = (key: string) => {
    activeMenu.value = key
  }

  const currentPageComponent = computed(() => pageComponents[activeMenu.value])
</script>

<style lang="scss" scoped>
  .dialog-layout {
    display: flex;
    height: 70vh;
  }

  .dialog-content {
    flex: 1;
    min-width: 0;
    padding: 16px;
    overflow-y: auto;
  }
</style>

<style lang="scss">
  .test-dialog.el-dialog {
    --el-dialog-padding-primary: 16px;
  }

  .test-dialog.el-dialog .test-dialog-header {
    padding: var(--el-dialog-padding-primary);
    padding-bottom: 0;
  }

  .test-dialog.el-dialog .test-dialog-body {
    padding: 0 !important;
  }

  .test-dialog-sidebar.layout-sidebar {
    flex-shrink: 0;
    height: 100%;
    width: 180px;

    .menu-left {
      height: 100%;
      width: 180px;
    }

    .el-menu:not(.el-menu--collapse) {
      width: 180px;
    }
  }
</style>
