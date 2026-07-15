<template>
  <div class="file-share-page art-full-height">
    <ElCard class="art-table-card">
      <ArtTableHeader v-model:columns="columnChecks" :loading="loading" @refresh="refreshData">
        <template #left>
          <ElSpace wrap>
            <ElButton type="primary" @click="showDialog('add')" v-ripple>添加</ElButton>
            <ElButton
                @click="handleBatchDelete"
                v-ripple
                :disabled="selectedRows.length === 0"
            >批量删除
            </ElButton>
          </ElSpace>
        </template>
      </ArtTableHeader>

      <ArtTable
          :loading="loading"
          :data="data"
          :columns="columns"
          :pagination="pagination"
          @selection-change="handleSelectionChange"
          @pagination:size-change="handleSizeChange"
          @pagination:current-change="handleCurrentChange"
      />

      <FileShareDialog
          v-model:visible="dialogVisible"
          :type="dialogType"
          :proxy-data="currentProxyData"
          @submit="handleDialogSubmit"
      />

      <PluginDialog
          v-model:visible="pluginDialogVisible"
          :protocol="ProtocolType.FILE"
          :proxy-id="currentPluginProxyId"
          :proxy-name="currentPluginProxyName"
          :initial-menu="pluginInitialMenu"
      />

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
import {ref, h, nextTick, onMounted} from 'vue'
import ArtButtonTable from '@/components/core/forms/art-button-table/index.vue'
import {useTable} from '@/hooks/core/useTable'
import {fetchGetFileShareList} from '@/api/file-share'
import {fetchBatchDeleteProxy} from '@/api/proxy'
import {fetchGetAgentListAll} from '@/api/agent'
import FileShareDialog from './modules/file-share-dialog.vue'
import PluginDialog from '../plugin/index.vue'
import MetricsDialog from '../metrics/metrics-dialog/index.vue'
import InspectorDrawer from '../inspector/inspector-drawer/index.vue'
import {renderTransportProtocolTag} from '../proxy/shared/render-transport-protocol-tag'
import {renderTrafficRate} from '../proxy/shared/render-traffic-rate'
import {renderTlsCertSummaryTag} from '../https/render-tls-cert-summary'
import {useProxyStatusToggle} from '../proxy/shared/use-proxy-status-toggle'
import {ElTag, ElSwitch, ElMessage, ElMessageBox, ElSpace} from 'element-plus'
import {DialogType} from '@/types'
import {ProtocolType, ProxyStatus} from '@/enums/orbien/business'

defineOptions({name: 'FileSharePenetration'})

type FileShareItem = Api.FileShare.FileShareListDTO

const selectedRows = ref<FileShareItem[]>([])
const dialogType = ref<DialogType>('add')
const dialogVisible = ref(false)
const currentProxyData = ref<Partial<FileShareItem>>({})
const agentNameMap = ref<Record<string, string>>({})

const pluginDialogVisible = ref(false)
const currentPluginProxyId = ref('')
const currentPluginProxyName = ref('')
const pluginInitialMenu = ref('')

const metricsDialogVisible = ref(false)
const currentMetricsProxyId = ref('')

const inspectorDrawerVisible = ref(false)
const currentInspectorProxyId = ref('')
const currentInspectorProxyName = ref('')

const {isToggling, handleStatusChange} = useProxyStatusToggle()

const resolveAgentName = (agentId: string) => agentNameMap.value[agentId] || agentId

const loadAgentNameMap = async () => {
  try {
    const agents = await fetchGetAgentListAll() || []
    agentNameMap.value = Object.fromEntries(agents.map((agent) => [agent.id, agent.name]))
  } catch (error) {
    console.error('获取客户端列表失败:', error)
  }
}

const {
  columns,
  columnChecks,
  data,
  loading,
  pagination,
  handleSizeChange,
  handleCurrentChange,
  refreshData
} = useTable({
  core: {
    apiFn: fetchGetFileShareList,
    apiParams: {
      current: 1,
      size: 10
    },
    columnsFactory: () => [
      {type: 'selection'},
      {
        prop: 'name',
        label: '共享名称',
        minWidth: 60
      },

      {
        prop: 'rootPath',
        label: '根目录',
        minWidth: 120
      },
      {
        prop: 'accessUrls',
        label: '访问地址',
        formatter: (row: FileShareItem) => {
          const urls = row.accessUrls?.length ? row.accessUrls : (row.domains || [])
          if (!urls.length) {
            return ''
          }
          return h(ElSpace, {direction: 'horizontal', size: 4, wrap: true}, () =>
              urls.map((url) => {
                const href = url.startsWith('http') ? url : `https://${url}`
                const label = row.domains?.find((domain) => href.includes(domain)) || url.replace(/^https?:\/\//, '')
                return h(
                    ElTag,
                    {
                      type: 'primary',
                      size: 'small',
                      style: 'cursor: pointer;',
                      onClick: () => window.open(href, '_blank')
                    },
                    () => label
                )
              })
          )
        }
      },
      {
        prop: 'tlsCertSummary',
        label: 'TLS 证书',
        formatter: (row: FileShareItem) =>
            renderTlsCertSummaryTag(row.tlsCertSummary, () => handleOpenTlsConfig(row))
      },
      {
        prop: 'transportProtocol',
        label: '传输协议',
        formatter: (row: FileShareItem) => renderTransportProtocolTag(row.transportProtocol)
      },
      {
        prop: 'traffic',
        label: '流量',
        width: 130,
        formatter: (row: FileShareItem) =>
          renderTrafficRate(row.traffic, () => handleMetrics(row))
      },
      {
        prop: 'authEnabled',
        label: '认证状态',
        formatter: (row: FileShareItem) =>
            h(ElTag, {
              type: row.authEnabled ? 'primary' : 'info',
              size: 'small'
            }, () => row.authEnabled ? '已启用' : '未启用')
      },
      {
        prop: 'status',
        label: '状态',
        width: 80,
        formatter: (row: FileShareItem) =>
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
        width: 150,
        fixed: 'right',
        formatter: (row: FileShareItem) =>
            h('div', [
              h(ArtButtonTable, {
                type: 'link',
                text: '设置',
                onClick: () => handleSettings(row)
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

const handleSelectionChange = (selection: FileShareItem[]): void => {
  selectedRows.value = selection
}

const showDialog = (type: DialogType, row?: FileShareItem): void => {
  dialogType.value = type
  currentProxyData.value = type === 'add' ? {} : (row || {})
  nextTick(() => {
    dialogVisible.value = true
  })
}

const handleSettings = (proxy: FileShareItem) => {
  pluginInitialMenu.value = ''
  currentPluginProxyId.value = proxy.id
  currentPluginProxyName.value = proxy.name
  pluginDialogVisible.value = true
}

const handleOpenTlsConfig = (proxy: FileShareItem) => {
  pluginInitialMenu.value = 'tls'
  currentPluginProxyId.value = proxy.id
  currentPluginProxyName.value = proxy.name
  pluginDialogVisible.value = true
}

const handleMetrics = (proxy: FileShareItem) => {
  currentMetricsProxyId.value = proxy.id
  metricsDialogVisible.value = true
}

const handleMetricsClose = () => {
  currentMetricsProxyId.value = ''
}

const handleInspector = (proxy: FileShareItem) => {
  currentInspectorProxyId.value = proxy.id
  currentInspectorProxyName.value = proxy.name
  inspectorDrawerVisible.value = true
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

const handleBatchDelete = async () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请选择要删除的文件共享')
    return
  }

  try {
    await ElMessageBox.confirm('确定要删除选中的文件共享吗？', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const ids = selectedRows.value.map((item) => item.id)
    await fetchBatchDeleteProxy({ids, protocol: ProtocolType.FILE})
    refreshData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

const handleSingleDelete = async (proxy: FileShareItem) => {
  try {
    await ElMessageBox.confirm(`确定要删除文件共享「${proxy.name}」吗？`, '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await fetchBatchDeleteProxy({ids: [proxy.id], protocol: ProtocolType.FILE})
    refreshData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

onMounted(() => {
  loadAgentNameMap()
})
</script>

<style lang="scss" scoped>
:deep(.orbien-root-path-cell) {
  display: inline-block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
  color: var(--el-text-color-regular);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace;
  font-size: 12px;
}
</style>
