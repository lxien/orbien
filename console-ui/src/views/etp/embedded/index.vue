<template>
  <div class="embedded-page art-full-height">
    <ElCard class="art-table-card">
      <ArtTableHeader v-model:columns="columnChecks" :loading="loading" @refresh="refreshData">
        <template #left>
          <ElSpace wrap>
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



      <MetricsDialog
        v-model:visible="metricsDialogVisible"
        :proxy-id="currentMetricsProxyId"
        @close="handleMetricsClose"
      />
    </ElCard>
  </div>
</template>

<script setup lang="ts">
  import { ref, h } from 'vue'
  import ArtButtonTable from '@/components/core/forms/art-button-table/index.vue'
  import { useTable } from '@/hooks/core/useTable'
  import { fetchGetEmbeddedList, fetchBatchDeleteEmbedded } from '@/api/embedded'

  import MetricsDialog from '../modules/metrics-dialog.vue'
  import { ElCard, ElTag, ElMessage, ElMessageBox, ElSpace, ElButton } from 'element-plus'

  defineOptions({ name: 'EmbeddedManagement' })

  const selectedRows = ref<Api.Embedded.TunnelListDTO[]>([])



  const metricsDialogVisible = ref(false)
  const currentMetricsProxyId = ref('')

  const getStatusConfig = (status: number) => {
    return status === 1
      ? { type: 'success' as const, text: '开启' }
      : { type: 'info' as const, text: '关闭' }
  }

  const getProtocolText = (protocol: number) => {
    return protocol === 2 ? 'HTTP' : 'TCP'
  }

  const isHttpProtocol = (
    tunnel: Api.Embedded.HttpTunnelListDTO | Api.Embedded.TcpTunnelListDTO
  ): tunnel is Api.Embedded.HttpTunnelListDTO => {
    return tunnel.protocol === 2
  }

  const {
    columns,
    columnChecks,
    data,
    loading,
    pagination,
    handleSizeChange,
    handleCurrentChange,
    refreshData,
    refreshRemove
  } = useTable({
    core: {
      apiFn: fetchGetEmbeddedList,
      apiParams: {
        page: 1,
        size: 10
      },
      paginationKey: {
        current: 'page',
        size: 'size'
      },
      columnsFactory: () => [
        { type: 'selection' },
        {
          prop: 'tunnel.agentId',
          label: '会话标识',
          minWidth: 140,
          formatter: (row: Api.Embedded.TunnelListDTO) => row.tunnel?.agentId || ''
        },
        {
          prop: 'tunnel.name',
          label: '服务名',
          minWidth: 120,
          formatter: (row: Api.Embedded.TunnelListDTO) => row.tunnel?.name || ''
        },
        {
          prop: 'tunnel.protocol',
          label: '协议',
          width: 80,
          formatter: (row: Api.Embedded.TunnelListDTO) => {
            const protocol = row.tunnel?.protocol || 0
            const text = getProtocolText(protocol)
            const type = protocol === 2 ? 'warning' : 'primary'
            return h(ElTag, { type }, () => text)
          }
        },
        {
          prop: 'remoteAddress',
          label: '远程地址',
          minWidth: 150,
          formatter: (row: Api.Embedded.TunnelListDTO) => {
            const tunnel = row.tunnel
            if (!tunnel) return ''

            if (isHttpProtocol(tunnel)) {
              return h(ElSpace, { direction: 'horizontal', size: 4, wrap: true }, () =>
                tunnel.domains.map((domain) => {
                  const fullAddress =
                    row.httpProxyPort && row.httpProxyPort !== 80
                      ? `${domain}:${row.httpProxyPort}`
                      : domain
                  return h(
                    ElTag,
                    {
                      type: 'warning',
                      style: 'cursor: pointer;',
                      onClick: () => window.open(`http://${fullAddress}`, '_blank')
                    },
                    () => domain
                  )
                })
              )
            } else {
              return h(ElTag, { type: 'primary' }, () => `:${tunnel.listenPort}`)
            }
          }
        },
        {
          prop: 'tunnel.targets',
          label: '内网服务',
          minWidth: 150,
          formatter: (row: Api.Embedded.TunnelListDTO) => {
            const targets = row.tunnel?.targets
            if (!targets || targets.length === 0) {
              return ''
            }
            return h(ElSpace, { direction: 'horizontal', size: 4, wrap: true }, () =>
              targets.map((target) => {
                const text = `${target.host}:${target.port}`
                return h(ElTag, { type: 'primary' }, () => text)
              })
            )
          }
        },
        {
          prop: 'tunnel.status',
          label: '状态',
          width: 80,
          formatter: (row: Api.Embedded.TunnelListDTO) => {
            const config = getStatusConfig(row.tunnel?.status || 0)
            return h(ElTag, { type: config.type }, () => config.text)
          }
        },
        {
          prop: 'operation',
          label: '操作',
          width: 220,
          fixed: 'right',
          formatter: (row: Api.Embedded.TunnelListDTO) =>
            h('div', [
              h(ArtButtonTable, {
                type: 'text',
                text: '流量统计',
                onClick: () => handleMetrics(row)
              }),
              h(ArtButtonTable, {
                type: 'delete',
                onClick: () => handleSingleDelete(row)
              })
            ])
        }
      ]
    },
    transform: {
      dataTransformer: (records) => {
        if (!Array.isArray(records)) {
          console.warn('数据转换器: 期望数组类型，实际收到:', typeof records)
          return []
        }
        return records
      }
    }
  })

  const handleMetrics = (row: Api.Embedded.TunnelListDTO) => {
    currentMetricsProxyId.value = row.tunnel?.proxyId || ''
    metricsDialogVisible.value = true
  }

  const handleMetricsClose = () => {
    currentMetricsProxyId.value = ''
  }

  const handleSelectionChange = (selection: Api.Embedded.TunnelListDTO[]): void => {
    selectedRows.value = selection
  }

  const handleBatchDelete = async () => {
    if (selectedRows.value.length === 0) {
      ElMessage.warning('请选择要删除的隧道')
      return
    }

    try {
      await ElMessageBox.confirm(
        `确定要删除选中的 ${selectedRows.value.length} 条隧道吗？`,
        '警告',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )

      const agentIds = selectedRows.value.map((item) => item.tunnel?.agentId || '').filter(Boolean)
      await fetchBatchDeleteEmbedded({ agentIds })
      ElMessage.success('删除成功')
      refreshRemove()
    } catch (error) {
      if (error !== 'cancel') {
        console.error('删除失败:', error)
      }
    }
  }

  const handleSingleDelete = async (row: Api.Embedded.TunnelListDTO) => {
    try {
      await ElMessageBox.confirm(`确定要删除隧道「${row.tunnel?.name || ''}」吗？`, '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })

      await fetchBatchDeleteEmbedded({ agentIds: [row.tunnel?.agentId || ''] })
      ElMessage.success('删除成功')
      refreshRemove()
    } catch (error) {
      if (error !== 'cancel') {
        console.error('删除失败:', error)
      }
    }
  }
</script>

<style lang="scss" scoped></style>
