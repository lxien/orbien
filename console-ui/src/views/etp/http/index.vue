<template>
  <div class="http-page art-full-height">
    <ElCard class="art-table-card">
      <!-- 表格头部 -->
      <ArtTableHeader v-model:columns="columnChecks" :loading="loading" @refresh="refreshData">
        <template #left>
          <ElSpace wrap>
            <ElButton type="primary" @click="showDialog('add')" v-ripple>新增</ElButton>
            <ElButton
              type="danger"
              @click="handleBatchDelete"
              v-ripple
              :disabled="selectedRows.length === 0"
              >批量删除</ElButton
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

      <!-- HTTP 代理弹窗 -->
      <HttpDialog
        v-model:visible="dialogVisible"
        :type="dialogType"
        :proxy-data="currentProxyData"
        @submit="handleDialogSubmit"
      />

      <!-- 访问控制弹窗 -->
      <AccessControlDialog
        v-model:visible="accessControlDialogVisible"
        :proxy-id="currentAccessControlProxyId"
        @close="handleAccessControlClose"
      />

      <!-- Basic Auth 弹窗 -->
      <BasicAuthDialog
        v-model:visible="basicAuthDialogVisible"
        :proxy-id="currentBasicAuthProxyId"
        @close="handleBasicAuthClose"
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
  import { ref, h, nextTick } from 'vue'
  import ArtButtonTable from '@/components/core/forms/art-button-table/index.vue'
  import { useTable } from '@/hooks/core/useTable'
  import { fetchGetHttpProxyList, fetchBatchDeleteProxy } from '@/api/proxy'
  import HttpDialog from './modules/http-dialog.vue'
  import AccessControlDialog from '../modules/access-control-dialog.vue'
  import BasicAuthDialog from '../modules/basic-auth-dialog.vue'
  import MetricsDialog from '../modules/metrics-dialog.vue'
  import { ElTag, ElMessage, ElMessageBox, ElSpace } from 'element-plus'
  import { DialogType } from '@/types'
  import { ProtocolType } from '@/enums/businessEnum'

  defineOptions({ name: 'HttpPenetration' })

  type HttpProxyItem = Api.Proxy.HttpProxyListDTO

  // 选中行
  const selectedRows = ref<HttpProxyItem[]>([])

  // 弹窗相关
  const dialogType = ref<DialogType>('add')
  const dialogVisible = ref(false)
  const currentProxyData = ref<Partial<HttpProxyItem>>({})

  // 访问控制弹窗相关
  const accessControlDialogVisible = ref(false)
  const currentAccessControlProxyId = ref('')

  // Basic Auth 弹窗相关
  const basicAuthDialogVisible = ref(false)
  const currentBasicAuthProxyId = ref('')

  // 流量统计弹窗相关
  const metricsDialogVisible = ref(false)
  const currentMetricsProxyId = ref('')

  const getProxyStatusConfig = (status: number) => {
    return status === 1
      ? { type: 'success' as const, text: '开启' }
      : { type: 'info' as const, text: '关闭' }
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
      apiFn: fetchGetHttpProxyList,
      apiParams: {
        current: 1,
        size: 10
      },
      columnsFactory: () => [
        { type: 'selection' },
        {
          prop: 'name',
          label: '代理名称',
          minWidth: 80
        },
        {
          prop: 'domains',
          label: '远程地址',
          minWidth: 150,
          formatter: (row: HttpProxyItem) => {
            if (!row.domains || row.domains.length === 0) {
              return ''
            }
            return h(ElSpace, { direction: 'horizontal', size: 4, wrap: true }, () =>
              row.domains.map((domain) => {
                const fullDomain =
                  row.httpProxyPort && row.httpProxyPort !== 80
                    ? `${domain}:${row.httpProxyPort}`
                    : domain
                return h(
                  ElTag,
                  {
                    type: 'warning',
                    style: 'cursor: pointer;',
                    onClick: () => window.open(`http://${fullDomain}`, '_blank')
                  },
                  () => domain
                )
              })
            )
          }
        },
        {
          prop: 'targets',
          label: '目标服务',
          minWidth: 150,
          formatter: (row: HttpProxyItem) => {
            if (!row.targets || row.targets.length === 0) {
              return ''
            }
            return h(ElSpace, { direction: 'horizontal', size: 4, wrap: true }, () =>
              row.targets.map((target) => {
                const text = `${target.host}:${target.port}`
                return h(ElTag, { type: 'primary' }, () => text)
              })
            )
          }
        },
        {
          prop: 'status',
          label: '状态',
          width: 80,
          formatter: (row: HttpProxyItem) => {
            const statusConfig = getProxyStatusConfig(row.status)
            return h(ElTag, { type: statusConfig.type }, () => statusConfig.text)
          }
        },
        {
          prop: 'operation',
          label: '操作',
          width: 360,
          fixed: 'right',
          formatter: (row: HttpProxyItem) =>
            h('div', [
              h(ArtButtonTable, {
                type: 'text',
                text: 'IP访问控制',
                onClick: () => handleIpControl(row)
              }),
              h(ArtButtonTable, {
                type: 'text',
                text: '鉴权认证',
                onClick: () => handleBasicAuth(row)
              }),
              h(ArtButtonTable, {
                type: 'text',
                text: '统计',
                onClick: () => handleMetrics(row)
              }),
              h(ArtButtonTable, {
                type: 'edit',
                onClick: () => showDialog('edit', row)
              }),
              h(ArtButtonTable, {
                type: 'delete',
                onClick: () => handleSingleDelete(row)
              })
            ])
        }
      ]
    }
  })

  const handleSelectionChange = (selection: HttpProxyItem[]): void => {
    selectedRows.value = selection
    console.log('选中行数据:', selectedRows.value)
  }

  const showDialog = (type: DialogType, row?: HttpProxyItem): void => {
    console.log('打开弹窗:', { type, row })
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

  const handleIpControl = (proxy: HttpProxyItem) => {
    currentAccessControlProxyId.value = proxy.id
    accessControlDialogVisible.value = true
  }

  const handleAccessControlClose = () => {
    currentAccessControlProxyId.value = ''
  }

  const handleBasicAuth = (proxy: HttpProxyItem) => {
    currentBasicAuthProxyId.value = proxy.id
    basicAuthDialogVisible.value = true
  }

  const handleBasicAuthClose = () => {
    currentBasicAuthProxyId.value = ''
  }

  const handleMetrics = (proxy: HttpProxyItem) => {
    currentMetricsProxyId.value = proxy.id
    metricsDialogVisible.value = true
  }

  const handleMetricsClose = () => {
    currentMetricsProxyId.value = ''
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
      await fetchBatchDeleteProxy({ ids, protocol: ProtocolType.HTTP })
      refreshData()
    } catch (error) {
      if (error !== 'cancel') {
        console.error('删除失败:', error)
      }
    }
  }

  const handleSingleDelete = async (proxy: HttpProxyItem) => {
    try {
      await ElMessageBox.confirm(`确定要删除代理「${proxy.name}」吗？`, '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })

      await fetchBatchDeleteProxy({ ids: [proxy.id], protocol: ProtocolType.HTTP })
      refreshData()
    } catch (error) {
      if (error !== 'cancel') {
        console.error('删除失败:', error)
      }
    }
  }
</script>

<style lang="scss" scoped></style>
