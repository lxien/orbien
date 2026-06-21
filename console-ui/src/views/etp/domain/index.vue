<template>
  <div class="domain-page art-full-height">
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
    </ElCard>
  </div>
</template>

<script setup lang="ts">
  import { useTable } from '@/hooks/core/useTable'
  import { fetchGetDomainListByPage } from '@/api/domain'

  defineOptions({ name: 'DomainManagement' })

  type DomainItem = Api.Domain.DomainDTO

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
      apiFn: fetchGetDomainListByPage,
      apiParams: {
        current: 1,
        size: 20
      },
      columnsFactory: () => [
        {
          prop: 'domain',
          label: '域名'
        },
        {
          prop: 'createdAt',
          label: '创建时间'
        },
        {
          prop: 'updatedAt',
          label: '更新时间'
        }
      ]
    }
  })
</script>

<style lang="scss" scoped></style>