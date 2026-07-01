<template>
  <ArtTableHeader v-model:columns="columnChecks" :loading="loading" @refresh="refreshData">
    <template #left>
      <ElSpace wrap>
        <ElButton type="primary" @click="handleAddDomain" v-ripple>添加根域名</ElButton>
        <ElButton @click="handleBatchDelete" :disabled="selectedRows.length === 0" v-ripple>
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
</template>

<script setup lang="ts">
  import { ref, h } from 'vue'
  import ArtButtonTable from '@/components/core/forms/art-button-table/index.vue'
  import { useTable } from '@/hooks/core/useTable'
  import { fetchGetDomainListByPage } from '@/api/domain'

  defineOptions({ name: 'DomainList' })

  const emit = defineEmits<{ change: [] }>()

  type DomainItem = Api.Domain.DomainDTO

  const selectedRows = ref<DomainItem[]>([])

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
        { type: 'selection' },
        {
          prop: 'domain',
          label: '根域名',
          minWidth: 180
        },
        {
          prop: 'createdAt',
          label: '创建时间'
        },
        {
          prop: 'updatedAt',
          label: '更新时间'
        },
        {
          prop: 'operation',
          label: '操作',
          width: 130,
          fixed: 'right',
          formatter: (row: DomainItem) =>
            h('div', [
              h(ArtButtonTable, {
                type: 'delete',
                onClick: () => deleteDomain(row)
              })
            ])
        }
      ]
    },
    hooks: {
      onSuccess: () => emit('change')
    }
  })

  const handleSelectionChange = (selection: DomainItem[]): void => {
    selectedRows.value = selection
  }

  const deleteDomain = (row: DomainItem): void => {
    console.log('删除域名:', row)
  }

  const handleAddDomain = (): void => {
    console.log('添加域名')
  }

  const handleBatchDelete = (): void => {
    console.log('批量删除:', selectedRows.value)
  }
</script>
