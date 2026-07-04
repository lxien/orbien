<template>
  <div class="port-pool-page art-full-height">
    <ElCard class="art-table-card">
      <ArtTableHeader v-model:columns="columnChecks" :loading="loading" @refresh="refreshData">
        <template #left>
          <ElSpace wrap>
            <ElButton type="primary" @click="showDialog('add')" v-ripple>添加端口</ElButton>
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
      />

      <PortPoolDialog
        v-model:visible="dialogVisible"
        :type="dialogType"
        :port-pool-id="currentPortPoolId"
        @submit="handleDialogSubmit"
      />
    </ElCard>
  </div>
</template>

<script setup lang="ts">
  import { ref, h, nextTick } from 'vue'
  import ArtButtonTable from '@/components/core/forms/art-button-table/index.vue'
  import { useTable } from '@/hooks/core/useTable'
  import { ElTag, ElMessage, ElMessageBox, ElSpace } from 'element-plus'
  import {
    fetchGetPortPoolListByPage,
    fetchDeleteBatchPortPools
  } from '@/api/port-pool'
  import PortPoolDialog from './modules/port-pool-dialog.vue'
  import { DialogType } from '@/types'
  import { getPortPoolTypeLabel } from '@/enums/orbien/business'

  defineOptions({ name: 'PortPool' })

  type PortPoolItem = Api.PortPool.PortPoolDTO

  const selectedRows = ref<PortPoolItem[]>([])
  const dialogType = ref<DialogType>('add')
  const dialogVisible = ref(false)
  const currentPortPoolId = ref<number | undefined>()

  const formatPort = (row: PortPoolItem) => {
    return row.endPort ? `${row.startPort}-${row.endPort}` : `${row.startPort}`
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
      apiFn: fetchGetPortPoolListByPage,
      apiParams: {
        current: 1,
        size: 20
      },
      columnsFactory: () => [
        { type: 'selection' },
        {
          prop: 'startPort',
          label: '端口',
          formatter: (row: PortPoolItem) => formatPort(row)
        },
        {
          prop: 'type',
          label: '协议',
          formatter: (row: PortPoolItem) => {
            const config = getPortPoolTypeLabel(row.type)
            return h(ElTag, { type: config.type, size: 'small' }, () => config.text)
          }
        },
        {
          prop: 'remark',
          label: '备注',
          formatter: (row: PortPoolItem) => row.remark || ''
        },
        {
          prop: 'createdAt',
          label: '创建时间'
        },
        {
          prop: 'operation',
          label: '操作',
          width: 150,
          fixed: 'right',
          formatter: (row: PortPoolItem) =>
            h('div', [
              h(ArtButtonTable, {
                type: 'link',
                text: '编辑',
                onClick: () => showDialog('edit', row)
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

  const handleSelectionChange = (selection: PortPoolItem[]): void => {
    selectedRows.value = selection
  }

  const deletePortPools = async (rows: PortPoolItem[], title: string, message: string) => {
    await ElMessageBox.confirm(message, title, {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await fetchDeleteBatchPortPools(rows.map((row) => row.id))
    ElMessage.success('删除成功')
    refreshData()
  }

  const handleDelete = (row: PortPoolItem): void => {
    deletePortPools([row], '删除端口', `确定要删除端口「${formatPort(row)}」吗？`).catch(() => {})
  }

  const handleBatchDelete = (): void => {
    if (selectedRows.value.length === 0) {
      ElMessage.warning('请选择要删除的端口')
      return
    }
    deletePortPools(
      selectedRows.value,
      '批量删除',
      `确定要删除选中的 ${selectedRows.value.length} 个端口配置吗？`
    ).catch(() => {})
  }

  const showDialog = (type: DialogType, row?: PortPoolItem): void => {
    dialogType.value = type
    currentPortPoolId.value = row?.id
    nextTick(() => {
      dialogVisible.value = true
    })
  }

  const handleDialogSubmit = () => {
    refreshData()
  }
</script>

<style lang="scss" scoped>
  .port-pool-page {
    width: 100%;
  }
</style>
