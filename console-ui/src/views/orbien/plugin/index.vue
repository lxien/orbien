<template>
  <ElDialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="960px"
      top="15px"
      class="plugin-dialog"
      header-class="plugin-dialog-header"
      body-class="plugin-dialog-body"
      :close-on-click-modal="false"
      :show-close="true"
  >
    <div class="dialog-layout">
      <div class="plugin-dialog-sidebar layout-sidebar">
        <div
            class="menu-left menu-left-open"
            :class="`menu-left-${menuTheme}`"
            :style="{ background: menuBgColor }"
        >
          <ElScrollbar class="h-full">
            <ElMenu
                :key="`${protocol}-${activeMenuKey}`"
                :class="'el-menu-' + menuTheme"
                :default-active="activeMenu"
                :text-color="menuTextColor"
                :background-color="menuBgColor"
                @select="handleMenuSelect"
            >
              <ElMenuItem v-for="item in menuItems" :key="item.key" :index="item.key">
                <div class="menu-icon flex-cc">
                  <ArtSvgIcon :icon="item.icon" :style="{ color: menuIconColor }"/>
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
        <component
            :is="currentPageComponent"
            v-if="currentPageComponent && proxyId"
            :key="`${proxyId}-${activeMenu}`"
            :proxy-id="proxyId"
            :proxy-name="proxyName"
            :protocol="protocol"
        />
        <ElEmpty v-else description="功能开发中"/>
      </div>
    </div>
  </ElDialog>
</template>

<script setup lang="ts">
import {ref, computed, watch, type Component} from 'vue'
import {useSettingStore} from '@/store/modules/setting'
import {storeToRefs} from 'pinia'
import {ProtocolType} from '@/enums/orbien/business'
import ArtSvgIcon from '@/components/core/base/art-svg-icon/index.vue'
import AccessControlPage from './modules/access-control/index.vue'
import BasicAuthPage from './modules/basic-auth/index.vue'
import TlsPage from './modules/tls/index.vue'
import ClusterPage from './modules/cluster/index.vue'
import TransportPage from './modules/transport/index.vue'
import RateLimitPolicyPage from './modules/rate-limit-policy/index.vue'
import HealthCheckPage from './modules/health-check/index.vue'
import HeaderRewritePage from './modules/header-rewrite/index.vue'
import TimeAccessPage from './modules/time-access/index.vue'
import {type ProxyConfigProtocol, getProtocolMenus} from './menus'

defineOptions({name: 'PluginDialog'})

interface Props {
  visible: boolean
  protocol: ProxyConfigProtocol
  proxyId?: string
  proxyName?: string
  initialMenu?: string
}

interface Emits {
  (e: 'update:visible', value: boolean): void
}

const props = withDefaults(defineProps<Props>(), {
  protocol: ProtocolType.TCP,
  proxyId: '',
  proxyName: '',
  initialMenu: ''
})
const emit = defineEmits<Emits>()

const settingStore = useSettingStore()
const {getMenuTheme} = storeToRefs(settingStore)

const dialogVisible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value)
})

const activeMenu = ref('')
const activeMenuKey = ref(0)

const menuTheme = computed(() => getMenuTheme.value.theme)
const menuBgColor = computed(() => getMenuTheme.value.background)
const menuTextColor = computed(() => getMenuTheme.value.textColor)
const menuIconColor = computed(() => getMenuTheme.value.iconColor)

const menuItems = computed(() => getProtocolMenus(props.protocol))

const dialogTitle = computed(() => {
  if (props.proxyName) {
    return `${props.proxyName} - 设置`
  }
  return '代理设置'
})

const pageComponents: Record<string, Component> = {
  access: AccessControlPage,
  time: TimeAccessPage,
  load: ClusterPage,
  trans: TransportPage,
  limit: RateLimitPolicyPage,
  health: HealthCheckPage,
  tls: TlsPage,
  headers: HeaderRewritePage
}

const resetActiveMenu = () => {
  activeMenu.value = menuItems.value[0]?.key ?? ''
}

watch(
    () => props.protocol,
    () => {
      resetActiveMenu()
    },
    {immediate: true}
)

const applyInitialMenu = () => {
  const menuKey = props.initialMenu
  if (menuKey && menuItems.value.some((item) => item.key === menuKey)) {
    activeMenu.value = menuKey
    return
  }
  resetActiveMenu()
}

watch(dialogVisible, (visible) => {
  if (visible) {
    applyInitialMenu()
    activeMenuKey.value++
  }
})

const handleMenuSelect = (key: string) => {
  activeMenu.value = key
}

const currentPageComponent = computed(() => {
  if (activeMenu.value === 'auth') {
    return BasicAuthPage
  }
  return pageComponents[activeMenu.value]
})
</script>

<style lang="scss" scoped>
.dialog-layout {
  display: flex;
  height: 100%;
}

.plugin-dialog-sidebar {
  flex-shrink: 0;
  width: 180px;
  border-right: 1px solid var(--art-card-border);
  display: flex;
  flex-direction: column;

  .menu-left {
    flex: 1;
    min-height: 0;
    display: flex;
    flex-direction: column;
  }

  :deep(.el-scrollbar) {
    flex: 1;
  }

  :deep(.el-scrollbar__wrap) {
    overflow-x: hidden;
  }

  :deep(.el-scrollbar__bar.is-horizontal) {
    display: none;
  }

  :deep(.el-menu:not(.el-menu--collapse)) {
    width: 180px;
    border-right: none;
  }
}

.dialog-content {
  flex: 1;
  min-width: 0;
  padding: 16px;
  overflow-y: auto;
}
</style>

<style lang="scss">
.plugin-dialog.el-dialog {
  --el-dialog-padding-primary: 16px;
  height: calc(100vh - 30px);
  margin: 15px auto 30px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding:  0;
}

.plugin-dialog.el-dialog .el-dialog__header {
  flex-shrink: 0;
}

.plugin-dialog.el-dialog .plugin-dialog-header {
  padding: var(--el-dialog-padding-primary);
  padding-bottom: 0;
}

.plugin-dialog.el-dialog .plugin-dialog-body {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}
</style>
