<template>
  <ArtTableHeader v-model:columns="columnChecks" :loading="loading" @refresh="refreshData" />

  <ArtTable
    :loading="loading"
    :data="data"
    :columns="columns"
    :pagination="pagination"
    @pagination:size-change="handleSizeChange"
    @pagination:current-change="handleCurrentChange"
  />
</template>

<script setup lang="ts">
  import { h } from 'vue'
  import { useTable } from '@/hooks/core/useTable'
  import { fetchGetUsedDomainListByPage } from '@/api/domain'
  import { ElTag } from 'element-plus'

  defineOptions({ name: 'UsedDomainList' })

  const emit = defineEmits<{ change: [] }>()

  type UsedDomainItem = Api.Domain.UsedDomainDTO

  const domainTypeMap: Record<number, { type: 'primary' | 'success' | 'warning'; text: string }> =
    {
      0: { type: 'primary', text: '自动' },
      1: { type: 'success', text: '子域名' },
      2: { type: 'warning', text: '自定义' }
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
      apiFn: fetchGetUsedDomainListByPage,
      apiParams: {
        current: 1,
        size: 20
      },
      columnsFactory: () => [
        {
          prop: 'fullDomain',
          label: '完整域名',
          minWidth: 200
        },
        {
          prop: 'domainType',
          label: '域名类型',
          width: 110,
          formatter: (row: UsedDomainItem) => {
            const config = domainTypeMap[row.domainType] ?? { type: 'primary' as const, text: '未知' }
            return h(ElTag, { type: config.type, size: 'small' }, () => config.text)
          }
        },
        {
          prop: 'proxyName',
          label: '关联代理',
          minWidth: 140,
          formatter: (row: UsedDomainItem) => row.proxyName || row.proxyId || '-'
        },
        {
          prop: 'rootDomain',
          label: '根域名',
          minWidth: 160,
          formatter: (row: UsedDomainItem) => row.rootDomain || '-'
        }
      ]
    },
    hooks: {
      onSuccess: () => emit('change')
    }
  })
</script>
