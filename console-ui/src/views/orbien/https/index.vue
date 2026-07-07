<template>
  <div class="https-page art-full-height">
    <ElCard class="art-table-card">
      <!-- 表格头部 -->
      <ArtTableHeader v-model:columns="columnChecks" :loading="loading" @refresh="refreshData">
        <template #left>
          <ElSpace wrap>
            <ElButton type="primary" @click="showDialog('add')" v-ripple>添加</ElButton>
            <ElButton
                @click="handleBatchDelete"
                v-ripple
                :disabled="selectedRows.length === 0"
            >批量删除
            </ElButton
            >
          </ElSpace>
        </template>
      </ArtTableHeader>

      <!-- 表格 -->
      <ArtTable
          :loading="loading"
          :data="data"
          :columns="columns"
          :pagination="pagination"
          @selection-change="handleSelectionChange"
          @pagination:size-change="handleSizeChange"
          @pagination:current-change="handleCurrentChange"
      >
      </ArtTable>

      <!-- HTTPS 代理弹窗 -->
      <HttpsDialog
          v-model:visible="dialogVisible"
          :type="dialogType"
          :proxy-data="currentProxyData"
          @submit="handleDialogSubmit"
          @open-cluster-config="handleOpenClusterConfig"
      />

      <!-- 扩展设置弹窗 -->
      <PluginDialog
          v-model:visible="pluginDialogVisible"
          :protocol="ProtocolType.HTTPS"
          :proxy-id="currentPluginProxyId"
          :proxy-name="currentPluginProxyName"
          :initial-menu="pluginInitialMenu"
      />

      <!-- 流量统计弹窗 -->
      <MetricsDialog
          v-model:visible="metricsDialogVisible"
          :proxy-id="currentMetricsProxyId"
          :show-time-range="true"
          @close="handleMetricsClose"
      />

      <InspectorDrawer
          v-model:visible="inspectorDrawerVisible"
          :proxy-id="currentInspectorProxyId"
          :proxy-name="currentInspectorProxyName"
      />
    </ElCard>
  </div>
</template>

<script setup lang="ts">
import {ref, h, nextTick} from 'vue'
import ArtButtonTable from '@/components/core/forms/art-button-table/index.vue'
import {useTable} from '@/hooks/core/useTable'
import {fetchGetHttpsProxyList, fetchBatchDeleteProxy} from '@/api/proxy'
import HttpsDialog from './modules/https-dialog.vue'
import PluginDialog from '../plugin/index.vue'
import MetricsDialog from '../common/modules/metrics-dialog/index.vue'
import InspectorDrawer from '../common/modules/inspector-drawer/index.vue'
import {renderTargetTags} from '../common/render-target-tag'
import {renderTransportProtocolTag} from '../common/render-transport-protocol-tag'
import {useProxyStatusToggle} from '../common/use-proxy-status-toggle'
import {ElTag, ElSwitch, ElMessage, ElMessageBox, ElSpace} from 'element-plus'
import {DialogType} from '@/types'
import {ProtocolType, ProxyStatus} from '@/enums/orbien/business'

defineOptions({name: 'HttpsPenetration'})

type HttpsProxyItem = Api.Proxy.HttpsProxyListDTO

// 选中行
const selectedRows = ref<HttpsProxyItem[]>([])

// 弹窗相关
const dialogType = ref<DialogType>('add')
const dialogVisible = ref(false)
const currentProxyData = ref<Partial<HttpsProxyItem>>({})

// 扩展设置弹窗相关
const pluginDialogVisible = ref(false)
const currentPluginProxyId = ref('')
const currentPluginProxyName = ref('')
const pluginInitialMenu = ref('')

// 流量统计弹窗相关
const metricsDialogVisible = ref(false)
const currentMetricsProxyId = ref('')

const inspectorDrawerVisible = ref(false)
const currentInspectorProxyId = ref('')
const currentInspectorProxyName = ref('')

const {isToggling, handleStatusChange} = useProxyStatusToggle()

const {
  columns,
  columnChecks,
  data,
  loading,
  pagination,
  getData,
  handleSizeChange,
  handleCurrentChange,
  refreshData
} = useTable({
  core: {
    apiFn: fetchGetHttpsProxyList,
    apiParams: {
      current: 1,
      size: 10
    },
    columnsFactory: () => [
      {type: 'selection'},
      {
        prop: 'name',
        label: '代理名称',
        minWidth: 50
      },
      {
        prop: 'domains',
        label: '外网地址',
        formatter: (row: HttpsProxyItem) => {
          if (!row.domains || row.domains.length === 0) {
            return ''
          }
          return h(ElSpace, {direction: 'horizontal', size: 4, wrap: true}, () =>
              row.domains.map((domain) => {
                const fullDomain =
                    row.httpsProxyPort && row.httpsProxyPort !== 443
                        ? `${domain}:${row.httpsProxyPort}`
                        : domain
                return h(
                    ElTag,
                    {
                      type: 'primary',
                      size: 'small',
                      style: 'cursor: pointer;',
                      onClick: () => window.open(`https://${fullDomain}`, '_blank')
                    },
                    () => domain
                )
              })
          )
        }
      },
      {
        prop: 'targets',
        label: '内网服务',
        formatter: (row: HttpsProxyItem) => renderTargetTags(row.targets)
      },
      {
        prop: 'transportProtocol',
        label: '传输协议',
        formatter: (row: HttpsProxyItem) => renderTransportProtocolTag(row.transportProtocol)
      },
      {
        prop: 'status',
        label: '状态',
        width: 80,
        formatter: (row: HttpsProxyItem) =>
            h(ElSwitch, {
              modelValue: row.status === ProxyStatus.OPEN,
              size: 'small',
              loading: isToggling(row.id),
              'onUpdate:modelValue': (enabled: boolean) => handleStatusChange(row, enabled)
            })
      },
      {
        prop: 'operation',
        label: '操作',
        width: 230,
        fixed: 'right',
        formatter: (row: HttpsProxyItem) =>
            h('div', [
              h(ArtButtonTable, {
                type: 'link',
                text: '设置',
                onClick: () => handleSettings(row)
              }),
              h(ArtButtonTable, {
                type: 'link',
                text: '统计',
                onClick: () => handleMetrics(row)
              }),
              h(ArtButtonTable, {
                type: 'link',
                text: '流量',
                onClick: () => handleInspector(row)
              }),
              h(ArtButtonTable, {
                type: 'link',
                text: '编辑',
                onClick: () => showDialog('edit', row)
              }),
              h(ArtButtonTable, {
                type: 'link',
                text: '删除',
                onClick: () => handleSingleDelete(row)
              })
            ])
      }
    ]
  }
})

const handleSelectionChange = (selection: HttpsProxyItem[]): void => {
  selectedRows.value = selection
  console.log('选中行数据:', selectedRows.value)
}

const showDialog = (type: DialogType, row?: HttpsProxyItem): void => {
  console.log('打开弹窗:', {type, row})
  dialogType.value = type
  currentProxyData.value = row || {}

  nextTick(() => {
    dialogVisible.value = true
  })
}

const handleDialogSubmit = async () => {
  try {
    dialogVisible.value = false
    currentProxyData.value = {}
    refreshData()
  } catch (error) {
    console.error('提交失败:', error)
  }
}

const handleSettings = (proxy: HttpsProxyItem) => {
  pluginInitialMenu.value = ''
  currentPluginProxyId.value = proxy.id
  currentPluginProxyName.value = proxy.name
  pluginDialogVisible.value = true
}

const handleOpenClusterConfig = (payload: { id: string; name: string }) => {
  dialogVisible.value = false
  currentProxyData.value = {}
  pluginInitialMenu.value = 'load'
  currentPluginProxyId.value = payload.id
  currentPluginProxyName.value = payload.name
  pluginDialogVisible.value = true
}

const handleMetrics = (proxy: HttpsProxyItem) => {
  currentMetricsProxyId.value = proxy.id
  metricsDialogVisible.value = true
}

const handleMetricsClose = () => {
  currentMetricsProxyId.value = ''
}

const handleInspector = (proxy: HttpsProxyItem) => {
  currentInspectorProxyId.value = proxy.id
  currentInspectorProxyName.value = proxy.name
  inspectorDrawerVisible.value = true
}

const handleBatchDelete = async () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请选择要删除的代理')
    return
  }

  try {
    await ElMessageBox.confirm('确定要删除选中的代理吗？', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const ids = selectedRows.value.map((item) => item.id)
    await fetchBatchDeleteProxy({ids, protocol: ProtocolType.HTTPS})
    refreshData()
  } catch (error) {
    if (error === 'cancel') {
      return
    }
  }
}

const handleSingleDelete = async (proxy: HttpsProxyItem) => {
  try {
    await ElMessageBox.confirm(`确定要删除代理「${proxy.name}」吗？`, '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await fetchBatchDeleteProxy({ids: [proxy.id], protocol: ProtocolType.HTTPS})
    refreshData()
  } catch (error) {
    if (error === 'cancel') {
      return
    }
  }
}
</script>

<style lang="scss" scoped>
:deep(.el-dialog__body) {
  padding: 0 !important;
}
</style>
