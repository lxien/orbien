<template>
  <ArtTableHeader v-model:columns="columnChecks" :loading="loading" @refresh="refreshData">
    <template #left>
      <ElSpace wrap>
        <ElButton type="primary" @click="showDialog('add')" v-ripple>添加根域名</ElButton>
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

  <DomainDialog
    v-model:visible="dialogVisible"
    :type="dialogType"
    :domain-id="currentDomainId"
    @submit="handleDialogSubmit"
  />
</template>

<script setup lang="ts">
  import { ref, h, nextTick } from 'vue'
  import { ElMessage, ElMessageBox } from 'element-plus'
  import ArtButtonTable from '@/components/core/forms/art-button-table/index.vue'
  import { useTable } from '@/hooks/core/useTable'
  import { fetchGetDomainListByPage, fetchDeleteBatchDomains } from '@/api/domain'
  import DomainDialog from './domain-dialog.vue'
  import { DialogType } from '@/types'

  defineOptions({ name: 'DomainList' })

  const emit = defineEmits<{ change: [] }>()

  type DomainItem = Api.Domain.DomainDTO

  const selectedRows = ref<DomainItem[]>([])
  const dialogType = ref<DialogType>('add')
  const dialogVisible = ref(false)
  const currentDomainId = ref<number | undefined>()

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
          prop: 'remark',
          label: '描述',
          minWidth: 160,
          formatter: (row: DomainItem) => row.remark || ''
        },
        {
          prop: 'createdAt',
          label: '创建时间',
          minWidth: 170
        },
        {
          prop: 'updatedAt',
          label: '更新时间',
          minWidth: 170
        },
        {
          prop: 'operation',
          label: '操作',
          width: 150,
          fixed: 'right',
          formatter: (row: DomainItem) =>
            h('div', [
              h(ArtButtonTable, {
                type: 'link',
                text: '编辑',
                onClick: () => showDialog('edit', row)
              }),
              h(ArtButtonTable, {
                type: 'link',
                text: '删除',
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

  const deleteDomains = async (rows: DomainItem[], title: string, message: string) => {
    await ElMessageBox.confirm(message, title, {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'error'
    })
    await fetchDeleteBatchDomains(rows.map((row) => row.id))
    ElMessage.success('删除成功')
    refreshData()
  }

  const deleteDomain = (row: DomainItem): void => {
    deleteDomains([row], '删除根域名', `确定要删除根域名「${row.domain}」吗？`).catch(() => {})
  }

  const handleBatchDelete = (): void => {
    if (selectedRows.value.length === 0) return
    deleteDomains(
      selectedRows.value,
      '批量删除',
      `确定要删除选中的 ${selectedRows.value.length} 个根域名吗？`
    ).catch(() => {})
  }

  const showDialog = (type: DialogType, row?: DomainItem): void => {
    dialogType.value = type
    currentDomainId.value = row?.id
    nextTick(() => {
      dialogVisible.value = true
    })
  }

  const handleDialogSubmit = () => {
    refreshData()
  }
</script>
