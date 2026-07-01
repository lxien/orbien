<template>
  <div class="port-pool-page art-full-height">
    <ElCard class="art-table-card">
      <!-- 表格头部 -->
      <ArtTableHeader v-model:columns="columnChecks" :loading="loading" @refresh="refreshData">
        <template #left>
          <ElSpace wrap>
            <ElButton type="primary" @click="handleAdd" v-ripple>添加端口</ElButton>
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
      />
    </ElCard>
  </div>
</template>

<script setup lang="ts">
  import { ref, h } from 'vue'
  import ArtButtonTable from '@/components/core/forms/art-button-table/index.vue'
  import { useTable } from '@/hooks/core/useTable'
  import { ElTag, ElMessage, ElMessageBox, ElSpace } from 'element-plus'

  defineOptions({ name: 'PortPool' })

  // 选中行
  const selectedRows = ref<PortPoolItem[]>([])

  // 端口池类型映射
  const portPoolTypeMap = {
    1: { type: 'primary' as const, text: 'TCP' },
    2: { type: 'success' as const, text: 'UDP' }
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
      // 暂不请求后端，保留结构
      apiFn: async () => ({ list: [], total: 0 }),
      apiParams: {
        current: 1,
        size: 20
      },
      columnsFactory: () => [
        { type: 'selection' },
        {
          prop: 'id',
          label: 'ID',
          width: 80
        },
        {
          prop: 'portStart',
          label: '端口',
          formatter: (row: PortPoolItem) => {
            return row.portEnd ? `${row.portStart} - ${row.portEnd}` : `${row.portStart}`
          }
        },
        {
          prop: 'type',
          label: '协议',
          formatter: (row: PortPoolItem) => {
            const config = portPoolTypeMap[row.type as keyof typeof portPoolTypeMap] || {
              type: 'info' as const,
              text: '未知'
            }
            return h(ElTag, { type: config.type }, () => config.text)
          }
        },
        {
          prop: 'remark',
          label: '备注',
          showOverflowTooltip: true
        },
        {
          prop: 'operation',
          label: '操作',
          width: 180,
          fixed: 'right',
          formatter: (row: PortPoolItem) =>
            h('div', [
              h(ArtButtonTable, {
                type: 'text',
                text: '编辑',
                onClick: () => handleEdit(row)
              }),
              h(ArtButtonTable, {
                type: 'delete',
                text: '删除',
                onClick: () => handleDelete(row)
              })
            ])
        }
      ]
    }
  })

  /**
   * 端口池数据项
   */
  interface PortPoolItem {
    id: number
    portStart: number
    portEnd?: number
    type: number
    remark?: string
    createdAt: string
    updatedAt: string
  }

  /**
   * 表格选择变化
   */
  const handleSelectionChange = (selection: PortPoolItem[]): void => {
    selectedRows.value = selection
  }

  /**
   * 新增端口池
   */
  const handleAdd = (): void => {
    console.log('新增端口池')
  }

  /**
   * 编辑端口池
   */
  const handleEdit = (row: PortPoolItem) => {
    console.log('编辑端口池:', row)
  }

  /**
   * 批量删除端口池
   */
  const handleBatchDelete = async (): Promise<void> => {
    if (selectedRows.value.length === 0) {
      ElMessage.warning('请选择要删除的端口池')
      return
    }

    try {
      await ElMessageBox.confirm('确定要删除选中的端口池吗？', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })

      ElMessage.success('删除成功')
      refreshData()
    } catch (error) {
      if (error !== 'cancel') {
        console.error('删除失败:', error)
      }
    }
  }

  /**
   * 删除端口池
   */
  const handleDelete = (row: PortPoolItem) => {
    console.log('删除端口池:', row)
  }
</script>

<style lang="scss" scoped>
  .port-pool-page {
    width: 100%;
  }
</style>
