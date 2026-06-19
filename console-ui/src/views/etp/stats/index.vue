<template>
  <div class="stats-page art-full-height">
    <ElCard class="art-table-card">
      <ArtTableHeader v-model:columns="columnChecks" :loading="loading" @refresh="refreshData">
      </ArtTableHeader>

      <ArtTable
        :loading="loading"
        :data="data"
        :columns="columns"
        :pagination="pagination"
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
  import { fetchGetMetricsList } from '@/api/metrics'
  import MetricsDialog from '../modules/metrics-dialog.vue'
  import { ByteUtils } from '@/utils/format/byteFormatter'
  import { ElTag } from 'element-plus'

  defineOptions({ name: 'Stats' })

  const metricsDialogVisible = ref(false)
  const currentMetricsProxyId = ref('')

  const getProtocolText = (protocol?: number) => {
    if (protocol === 2) return 'HTTP'
    if (protocol === 1) return 'TCP'
    return '-'
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
        { type: 'index', width: 60, label: '序号' },
        {
          prop: 'agentName',
          label: '客户端名称',
          minWidth: 100,
          formatter: (row: Api.Metrics.TrafficCountDTO) => row.agentName || '-'
        },
        {
          prop: 'proxyName',
          label: '代理名称',
          minWidth: 100,
          formatter: (row: Api.Metrics.TrafficCountDTO) => row.proxyName || '-'
        },
        {
          prop: 'protocol',
          label: '协议',
          width: 90,
          formatter: (row: Api.Metrics.TrafficCountDTO) => {
            const text = getProtocolText(row.protocol)
            if (text === '-') return text
            const type = row.protocol === 2 ? 'warning' : 'primary'
            return h(ElTag, { type, size: 'small' }, () => text)
          }
        },
        {
          prop: 'writeBytes',
          label: '上行流量',
          width: 120,
          formatter: (row: Api.Metrics.TrafficCountDTO) =>
            ByteUtils.formatBytes(row.writeBytes || 0)
        },
        {
          prop: 'readBytes',
          label: '下行流量',
          width: 120,
          formatter: (row: Api.Metrics.TrafficCountDTO) => ByteUtils.formatBytes(row.readBytes || 0)
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
          formatter: (row: Api.Metrics.TrafficCountDTO) =>
            ByteUtils.formatBytes(row.totalBytes || 0)
        },
        {
          prop: 'operation',
          label: '操作',
          width: 150,
          fixed: 'right',
          formatter: (row: Api.Metrics.TrafficCountDTO) =>
            h('div', [
              h(ArtButtonTable, {
                type: 'text',
                text: '数据',
                onClick: () => handleViewMetrics(row)
              })
            ])
        }
      ]
    }
  })

  const handleViewMetrics = (row: Api.Metrics.TrafficCountDTO) => {
    currentMetricsProxyId.value = row.proxyId
    metricsDialogVisible.value = true
  }

  const handleMetricsClose = () => {
    currentMetricsProxyId.value = ''
  }
</script>

<style scss scoped></style>
