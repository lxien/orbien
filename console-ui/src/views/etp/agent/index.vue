<template>
  <div class="client-page art-full-height">
    <ElCard class="art-table-card">
      <!-- 表格头部 -->
      <ArtTableHeader v-model:columns="columnChecks" :loading="loading" @refresh="refreshData">
        <template #left>
          <ElSpace wrap>
            <ElButton @click="handleBatchDelete" :disabled="selectedRows.length === 0" v-ripple>
              批量删除
            </ElButton>
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

      <!-- 客户端详情弹窗 -->
      <AgentDialog v-model:visible="detailDialogVisible" :client-data="selectedClient" />
    </ElCard>
  </div>
</template>

<script setup lang="ts">
  import { ref, h, nextTick } from 'vue'
  import ArtButtonTable from '@/components/core/forms/art-button-table/index.vue'
  import { useTable } from '@/hooks/core/useTable'
  import { fetchGetAgentListByPage, fetchKickoutAgent, fetchDeleteBatchAgents } from '@/api/agent'
  import AgentDialog from './modules/agent-dialog.vue'
  import { ElTag, ElMessageBox, ElMessage } from 'element-plus'

  defineOptions({ name: 'ClientManagement' })

  type ClientItem = Api.Agent.AgentDTO

  const selectedRows = ref<ClientItem[]>([])

  // 详情弹窗状态
  const detailDialogVisible = ref(false)

  // 选中的客户端
  const selectedClient = ref<ClientItem | null>(null)

  /**
   * 获取客户端状态标签配置
   */
  const getClientStatusConfig = (isOnline: boolean) => {
    return isOnline
      ? { type: 'primary' as const, text: '在线' }
      : { type: 'info' as const, text: '离线' }
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
      apiFn: fetchGetAgentListByPage,
      apiParams: {
        current: 1,
        size: 20
      },
      columnsFactory: () => [
        { type: 'selection' },
        {
          prop: 'id',
          label: '客户端ID',
          width: 180
        },
        {
          prop: 'name',
          label: '名称'
        },
        {
          prop: 'os',
          label: '操作系统'
        },
        {
          prop: 'arch',
          label: '系统架构'
        },
        {
          prop: 'version',
          label: '版本'
        },
        {
          prop: 'isOnline',
          label: '状态',
          formatter: (row: ClientItem) => {
            const statusConfig = getClientStatusConfig(row.isOnline)
            return h(ElTag, { type: statusConfig.type, size: 'small' }, () => statusConfig.text)
          }
        },
        {
          prop: 'operation',
          label: '操作',
          width: 220,
          fixed: 'right',
          formatter: (row: ClientItem) =>
            h('div', [
              h(ArtButtonTable, {
                type: 'link',
                text: '详情',
                onClick: () => showClientDetail(row)
              }),
              h(ArtButtonTable, {
                type: 'link',
                text: '强退',
                onClick: () => kickoutClient(row),
                disabled: !row.isOnline
              }),
              h(ArtButtonTable, {
                type: 'link',
                text: '删除',
                onClick: () => deleteClient(row)
              })
            ])
        }
      ]
    }
  })

  const handleSelectionChange = (selection: ClientItem[]): void => {
    selectedRows.value = selection
  }

  const deleteClients = async (rows: ClientItem[], title: string, message: string) => {
    await ElMessageBox.confirm(message, title, {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'error'
    })
    await fetchDeleteBatchAgents(rows.map((row) => row.id))
    ElMessage.success('删除成功')
    refreshData()
  }

  const deleteClient = (row: ClientItem): void => {
    deleteClients([row], '删除客户端', `确定要删除客户端「${row.name}」吗？`).catch(() => {})
  }

  const handleBatchDelete = (): void => {
    if (selectedRows.value.length === 0) return
    deleteClients(
      selectedRows.value,
      '批量删除',
      `确定要删除选中的 ${selectedRows.value.length} 个客户端吗？`
    ).catch(() => {})
  }

  /**
   * 剔除在线客户端
   */
  const kickoutClient = (row: ClientItem): void => {
    ElMessageBox.confirm(`确定要剔除该在线客户端吗？`, '剔除客户端', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(async () => {
      await fetchKickoutAgent(row.id)
      ElMessage.success('剔除成功')
      refreshData()
    })
  }

  /**
   * 显示客户端详情
   */
  const showClientDetail = (client: ClientItem) => {
    selectedClient.value = client
    nextTick(() => {
      detailDialogVisible.value = true
    })
  }
</script>

<style lang="scss" scoped></style>
