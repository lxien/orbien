<template>
  <div class="stats-page art-full-height">
    <ElCard class="art-table-card">
      <ArtTableHeader v-model:columns="columnChecks" :loading="loading" @refresh="refreshData">
        <template #left>
          <ElSpace wrap>
            <ElButton
              @click="handleBatchDelete"
              v-ripple
              :disabled="selectedRows.length === 0"
            >
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
      >
      </ArtTable>

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
  import { ref, h } from 'vue'
  import ArtButtonTable from '@/components/core/forms/art-button-table/index.vue'
  import ArtTable from '@/components/core/tables/art-table/index.vue'
  import ArtTableHeader from '@/components/core/tables/art-table-header/index.vue'
  import { useTable } from '@/hooks/core/useTable'
  import { fetchGetMetricsList, fetchBatchDeleteMetrics } from '@/api/metrics'
  import MetricsDialog from '../metrics/metrics-dialog/index.vue'
  import { ByteUtils } from '@/utils/format/byteFormatter'
  import { ElTag, ElMessage, ElMessageBox, ElSpace } from 'element-plus'
  import { getProtocolLabel, ProtocolType } from '@/enums/orbien/business'

  defineOptions({ name: 'Stats' })

  type StatsItem = Api.Metrics.TrafficCountDTO

  const metricsDialogVisible = ref(false)
  const currentMetricsProxyId = ref('')
  const selectedRows = ref<StatsItem[]>([])

  const getProtocolText = (protocol?: number) => getProtocolLabel(protocol)

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
      apiFn: fetchGetMetricsList,
      apiParams: {
        current: 1,
        size: 10
      },
      paginationKey: {
        current: 'current',
        size: 'size'
      },
      columnsFactory: () => [
        { type: 'selection' },
        { type: 'index', width: 60, label: '序号' },
        {
          prop: 'agentName',
          label: '客户端名称',
          minWidth: 100,
          formatter: (row: StatsItem) => row.agentName || ''
        },
        {
          prop: 'proxyName',
          label: '代理名称',
          minWidth: 100,
          formatter: (row: StatsItem) => row.proxyName || ''
        },
        {
          prop: 'protocol',
          label: '协议',
          width: 90,
          formatter: (row: StatsItem) => {
            const text = getProtocolText(row.protocol)
            if (!text) return ''
            const type = row.protocol === ProtocolType.HTTP ? 'info' : 'primary'
            return h(ElTag, { type, size: 'small' }, () => text)
          }
        },
        {
          prop: 'writeBytes',
          label: '上行流量',
          width: 120,
          formatter: (row: StatsItem) => ByteUtils.formatBytes(row.writeBytes || 0)
        },
        {
          prop: 'readBytes',
          label: '下行流量',
          width: 120,
          formatter: (row: StatsItem) => ByteUtils.formatBytes(row.readBytes || 0)
        },
        {
          prop: 'writeMessages',
          label: '上行消息数',
          width: 120
        },
        {
          prop: 'readMessages',
          label: '下行消息数',
          width: 120
        },
        {
          prop: 'totalBytes',
          label: '总流量',
          width: 150,
          formatter: (row: StatsItem) => ByteUtils.formatBytes(row.totalBytes || 0)
        },
        {
          prop: 'operation',
          label: '操作',
          width: 150,
          fixed: 'right',
          formatter: (row: StatsItem) =>
            h('div', [
              h(ArtButtonTable, {
                type: 'link',
                text: '数据',
                onClick: () => handleViewMetrics(row)
              }),
              h(ArtButtonTable, {
                type: 'link',
                text: '删除',
                onClick: () => handleDelete(row)
              })
            ])
        }
      ]
    }
  })

  const handleSelectionChange = (selection: StatsItem[]): void => {
    selectedRows.value = selection
  }

  const deleteMetrics = async (rows: StatsItem[], title: string, message: string) => {
    await ElMessageBox.confirm(message, title, {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await fetchBatchDeleteMetrics({ ids: rows.map((row) => row.proxyId) })
    refreshData()
  }

  const handleDelete = (row: StatsItem): void => {
    const name = row.proxyName || row.proxyId
    deleteMetrics([row], '删除统计', `确定要删除代理「${name}」的流量统计数据吗？`).catch(
      () => {}
    )
  }

  const handleBatchDelete = (): void => {
    if (selectedRows.value.length === 0) {
      ElMessage.warning('请选择要删除的统计数据')
      return
    }
    deleteMetrics(
      selectedRows.value,
      '批量删除',
      `确定要删除选中的 ${selectedRows.value.length} 条代理流量统计数据吗？`
    ).catch(() => {})
  }

  const handleViewMetrics = (row: StatsItem) => {
    currentMetricsProxyId.value = row.proxyId
    metricsDialogVisible.value = true
  }

  const handleMetricsClose = () => {
    currentMetricsProxyId.value = ''
  }
</script>

<style scss scoped></style>
