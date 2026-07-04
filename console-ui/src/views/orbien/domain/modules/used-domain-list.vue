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
  import { getDomainTypeLabel } from '@/enums/orbien/business'
  import { fetchGetUsedDomainListByPage } from '@/api/domain'
  import { ElTag } from 'element-plus'

  defineOptions({ name: 'UsedDomainList' })

  const emit = defineEmits<{ change: [] }>()

  type UsedDomainItem = Api.Domain.UsedDomainDTO

  const getDomainTypeTag = (domainType: number) => getDomainTypeLabel(domainType)

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
        },
        {
          prop: 'domainType',
          label: '域名类型',
          formatter: (row: UsedDomainItem) => {
            const config = getDomainTypeTag(row.domainType)
            return h(ElTag, { type: config.type, size: 'small' }, () => config.text)
          }
        },
        {
          prop: 'proxyName',
          label: '关联代理',
          formatter: (row: UsedDomainItem) => row.proxyName || row.proxyId || ''
        },
        {
          prop: 'rootDomain',
          label: '根域名',
          formatter: (row: UsedDomainItem) => row.rootDomain || ''
        }
      ]
    },
    hooks: {
      onSuccess: () => emit('change')
    }
  })
</script>
