<template>
  <div class="tcp-page art-full-height">
    <ElCard class="art-table-card" shadow="never">
      <ArtTableHeader v-model:columns="columnChecks" :loading="loading" @refresh="refreshData">
        <template #left>
          <ElSpace wrap>
            <ElButton type="primary" @click="showDialog('add')" v-ripple>添加</ElButton>
            <ElButton :disabled="selectedRows.length === 0" @click="handleBatchDelete" v-ripple>
              批量删除
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

      <TcpDialog
        v-model:visible="dialogVisible"
        :type="dialogType"
        :proxy-data="currentProxyData"
        @submit="handleDialogSubmit"
        @open-cluster-config="handleOpenClusterConfig"
      />

      <PluginDialog
        v-model:visible="pluginDialogVisible"
        :protocol="ProtocolType.TCP"
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
    </ElCard>
  </div>
</template>

<script setup lang="ts">
  import { ref, h, nextTick } from 'vue'
  import ArtButtonTable from '@/components/core/forms/art-button-table/index.vue'
  import { useTable } from '@/hooks/core/useTable'
  import { fetchGetTcpProxyList, fetchBatchDeleteProxy } from '@/api/proxy'
  import TcpDialog from './modules/tcp-dialog.vue'
  import PluginDialog from '../plugin/index.vue'
  import MetricsDialog from '../metrics/metrics-dialog/index.vue'
  import { renderTargetTags } from '../proxy/shared/render-target-tag'
  import { renderTransportProtocolTag } from '../proxy/shared/render-transport-protocol-tag'
  import { renderTrafficRate } from '../proxy/shared/render-traffic-rate'
  import { useProxyStatusToggle } from '../proxy/shared/use-proxy-status-toggle'
  import { ElSwitch, ElMessage, ElMessageBox, ElSpace } from 'element-plus'
  import { DialogType } from '@/types'
  import { ProtocolType, ProxyStatus } from '@/enums/orbien/business'

  defineOptions({ name: 'TcpPenetration' })

  type TcpProxyItem = Api.Proxy.TcpProxyListDTO

  const selectedRows = ref<TcpProxyItem[]>([])

  const dialogType = ref<DialogType>('add')
  const dialogVisible = ref(false)
  const currentProxyData = ref<Partial<TcpProxyItem>>({})

  const pluginDialogVisible = ref(false)
  const currentPluginProxyId = ref('')
  const currentPluginProxyName = ref('')
  const pluginInitialMenu = ref('')

  const metricsDialogVisible = ref(false)
  const currentMetricsProxyId = ref('')

  const { isToggling, handleStatusChange } = useProxyStatusToggle()

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
      apiFn: fetchGetTcpProxyList,
      apiParams: {
        current: 1,
        size: 10
      },
      columnsFactory: () => [
        { type: 'selection', width: 48 },
        {
          prop: 'name',
          label: '代理名称',
          minWidth: 140,
          showOverflowTooltip: true
        },
        {
          prop: 'listenPort',
          label: '远程端口',
          width: 100,
          formatter: (row: TcpProxyItem) =>
            h('span', { class: 'tcp-port' }, row.listenPort != null ? String(row.listenPort) : '—')
        },
        {
          prop: 'targets',
          label: '内网服务',
          minWidth: 180,
          formatter: (row: TcpProxyItem) => renderTargetTags(row.targets)
        },
        {
          prop: 'transportProtocol',
          label: '传输',
          width: 110,
          formatter: (row: TcpProxyItem) => renderTransportProtocolTag(row.transportProtocol)
        },
        {
          prop: 'traffic',
          label: '流量',
          width: 130,
          formatter: (row: TcpProxyItem) =>
            renderTrafficRate(row.traffic, () => handleMetrics(row))
        },
        {
          prop: 'status',
          label: '状态',
          width: 72,
          formatter: (row: TcpProxyItem) =>
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
          formatter: (row: TcpProxyItem) =>
            h('div', { class: 'tcp-ops' }, [
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

  const handleSelectionChange = (selection: TcpProxyItem[]): void => {
    selectedRows.value = selection
  }

  const showDialog = (type: DialogType, row?: TcpProxyItem): void => {
    dialogType.value = type
    currentProxyData.value = row || {}
    nextTick(() => {
      dialogVisible.value = true
    })
  }

  const handleDialogSubmit = () => {
    dialogVisible.value = false
    currentProxyData.value = {}
    refreshData()
  }

  const deleteProxies = async (ids: string[], tip: string) => {
    await ElMessageBox.confirm(tip, '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await fetchBatchDeleteProxy({ ids, protocol: ProtocolType.TCP })
    ElMessage.success('删除成功')
    refreshData()
  }

  const handleBatchDelete = async () => {
    if (selectedRows.value.length === 0) {
      ElMessage.warning('请选择要删除的代理')
      return
    }
    try {
      const ids = selectedRows.value.map((item) => item.id)
      await deleteProxies(ids, `确定删除选中的 ${ids.length} 个代理吗？`)
    } catch (error) {
      if (error !== 'cancel') {
        console.error('批量删除失败:', error)
      }
    }
  }

  const handleSingleDelete = async (proxy: TcpProxyItem) => {
    try {
      await deleteProxies([proxy.id], `确定删除代理「${proxy.name}」吗？`)
    } catch (error) {
      if (error !== 'cancel') {
        console.error('删除失败:', error)
      }
    }
  }

  const handleSettings = (proxy: TcpProxyItem) => {
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

  const handleMetrics = (proxy: TcpProxyItem) => {
    currentMetricsProxyId.value = proxy.id
    metricsDialogVisible.value = true
  }

  const handleMetricsClose = () => {
    currentMetricsProxyId.value = ''
  }
</script>

<style lang="scss" scoped>
  .tcp-page {
    :deep(.tcp-port) {
      font-variant-numeric: tabular-nums;
      font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
      font-size: 13px;
      color: var(--el-text-color-regular);
    }

    :deep(.tcp-ops) {
      display: inline-flex;
      align-items: center;
    }
  }
</style>
