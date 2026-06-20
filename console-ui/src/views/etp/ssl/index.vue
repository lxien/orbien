<template>
  <div class="ssl-page art-full-height">
    <ElCard class="art-table-card">
      <!-- 表格头部 -->
      <ArtTableHeader v-model:columns="columnChecks" :loading="loading" @refresh="refreshData">
        <template #left>
          <ElSpace wrap>
            <ElButton type="primary" @click="handleAdd" v-ripple>上传证书</ElButton>
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
      >
      </ArtTable>
    </ElCard>

    <SslDialog v-model:visible="dialogVisible" @submit="handleUploadSubmit" />
  </div>
</template>

<script setup lang="ts">
  import { ref } from 'vue'
  import { useTable } from '@/hooks/core/useTable'
  import { ElMessage, ElMessageBox } from 'element-plus'
  import SslDialog from './modules/ssl-dialog.vue'

  defineOptions({ name: 'SslManagement' })

  type SslItem = any

  // 选中行
  const selectedRows = ref<SslItem[]>([])

  // 对话框可见性
  const dialogVisible = ref(false)

  const {
    columns,
    columnChecks,
    data,
    loading,
    pagination,
    getData,
    handleSizeChange,
    handleCurrentChange,
    refreshData
  } = useTable({
    core: {
      apiFn: async () => ({ data: [], total: 0 }),
      apiParams: {
        current: 1,
        size: 10
      },
      columnsFactory: () => [
        { type: 'selection' },
        { type: 'index', width: 60, label: '序号' },
        {
          prop: 'name',
          label: '证书名称',
          minWidth: 120
        },
        {
          prop: 'domain',
          label: '域名',
          minWidth: 150
        },
        {
          prop: 'expireTime',
          label: '过期时间',
          minWidth: 150
        },
        {
          prop: 'status',
          label: '状态',
          width: 80
        },
        {
          prop: 'operation',
          label: '操作',
          width: 200,
          fixed: 'right'
        }
      ]
    }
  })

  const handleSelectionChange = (selection: SslItem[]): void => {
    selectedRows.value = selection
  }

  const handleAdd = () => {
    dialogVisible.value = true
  }

  const handleUploadSubmit = () => {
    refreshData()
  }

  const handleBatchDelete = async () => {
    if (selectedRows.value.length === 0) {
      ElMessage.warning('请选择要删除的证书')
      return
    }

    try {
      await ElMessageBox.confirm('确定要删除选中的证书吗？', '警告', {
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
</script>

<style lang="scss" scoped></style>