<template>
  <div class="socks5-page art-full-height">
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

      <!-- SOCKS5 代理弹窗 -->
      <Socks5Dialog
          v-model:visible="dialogVisible"
          :type="dialogType"
          :proxy-data="currentProxyData"
          @submit="handleDialogSubmit"
      />

      <!-- 扩展设置弹窗 -->
      <PluginDialog
          v-model:visible="pluginDialogVisible"
          :protocol="ProtocolType.SOCKS5"
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
    </ElCard>
  </div>
</template>

<script setup lang="ts">
import {ref, h, nextTick} from 'vue'
import ArtButtonTable from '@/components/core/forms/art-button-table/index.vue'
import {useTable} from '@/hooks/core/useTable'
import {fetchGetSocks5ProxyList, fetchBatchDeleteProxy} from '@/api/proxy'
import Socks5Dialog from './modules/socks5-dialog.vue'
import PluginDialog from '../plugin/index.vue'
import MetricsDialog from '../metrics/metrics-dialog/index.vue'
import {renderTransportProtocolTag} from '../proxy/shared/render-transport-protocol-tag'
import {useProxyStatusToggle} from '../proxy/shared/use-proxy-status-toggle'
import {ElTag, ElSwitch, ElMessage, ElMessageBox, ElSpace} from 'element-plus'
import {DialogType} from '@/types'
import {ProtocolType, ProxyStatus} from '@/enums/orbien/business'

defineOptions({name: 'Socks5Penetration'})

type Socks5ProxyItem = Api.Proxy.Socks5ProxyListDTO

// 选中行
const selectedRows = ref<Socks5ProxyItem[]>([])

// 弹窗相关
const dialogType = ref<DialogType>('add')
const dialogVisible = ref(false)
const currentProxyData = ref<Partial<Socks5ProxyItem>>({})

// 扩展设置弹窗相关
const pluginDialogVisible = ref(false)
const currentPluginProxyId = ref('')
const currentPluginProxyName = ref('')
const pluginInitialMenu = ref('')

// 流量统计弹窗相关
const metricsDialogVisible = ref(false)
const currentMetricsProxyId = ref('')

const {isToggling, handleStatusChange} = useProxyStatusToggle()

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
    apiFn: fetchGetSocks5ProxyList,
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
        prop: 'listenPort',
        label: '远程端口',
      },
      {
        prop: 'authEnabled',
        label: '认证状态',
        width: 100,
        formatter: (row: Socks5ProxyItem) =>
            h(ElTag, {
              type: row.authEnabled ? 'primary' : 'info',
              size: 'small'
            }, () => row.authEnabled ? '已启用' : '未启用')
      },
      {
        prop: 'authUserCount',
        label: '认证用户',
        width: 90,
        formatter: (row: Socks5ProxyItem) => row.authUserCount ?? 0
      },
      {
        prop: 'transportProtocol',
        label: '传输协议',
        formatter: (row: Socks5ProxyItem) => renderTransportProtocolTag(row.transportProtocol)
      },
      {
        prop: 'status',
        label: '状态',
        width: 80,
        formatter: (row: Socks5ProxyItem) =>
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
        width: 190,
        fixed: 'right',
        formatter: (row: Socks5ProxyItem) =>
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

const handleSelectionChange = (selection: Socks5ProxyItem[]): void => {
  selectedRows.value = selection
  console.log('选中行数据:', selectedRows.value)
}

const showDialog = (type: DialogType, row?: Socks5ProxyItem): void => {
  dialogType.value = type
  currentProxyData.value = type === 'add' ? {} : (row || {})
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
    await fetchBatchDeleteProxy({ids, protocol: ProtocolType.SOCKS5})
    refreshData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

const handleSingleDelete = async (proxy: Socks5ProxyItem) => {
  try {
    await ElMessageBox.confirm(`确定要删除代理「${proxy.name}」吗？`, '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await fetchBatchDeleteProxy({ids: [proxy.id], protocol: ProtocolType.SOCKS5})
    refreshData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

const handleSettings = (proxy: Socks5ProxyItem) => {
  pluginInitialMenu.value = ''
  currentPluginProxyId.value = proxy.id
  currentPluginProxyName.value = proxy.name
  pluginDialogVisible.value = true
}

const handleMetrics = (proxy: Socks5ProxyItem) => {
  currentMetricsProxyId.value = proxy.id
  metricsDialogVisible.value = true
}

const handleMetricsClose = () => {
  currentMetricsProxyId.value = ''
}
</script>
