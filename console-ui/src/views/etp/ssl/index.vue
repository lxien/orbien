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
    <DeployDialog v-model:visible="deployDialogVisible" @submit="handleDeploySubmit" />
  </div>
</template>

<script setup lang="ts">
  import { ref, h } from 'vue'
  import { useTable } from '@/hooks/core/useTable'
  import { ElMessage, ElMessageBox, ElButton } from 'element-plus'
  import SslDialog from './modules/ssl-dialog.vue'
  import DeployDialog from './modules/deploy-dialog.vue'
  import { fetchGetCertListByPage } from '@/api/ssl'
  import ArtButtonTable from '@/components/core/forms/art-button-table/index.vue'

  defineOptions({ name: 'SslManagement' })

  type SslItem = Api.Ssl.CertDTO

  const selectedRows = ref<SslItem[]>([])
  const dialogVisible = ref(false)
  const deployDialogVisible = ref(false)

  const getExpireDays = (item: SslItem) => {
    const now = new Date()
    const notAfter = new Date(item.notAfter)
    if (now > notAfter) {
      return h('span', { style: { color: 'var(--el-color-danger)' } }, '已过期')
    }
    const diffTime = notAfter.getTime() - now.getTime()
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))
    return h('span', { style: { color: 'var(--el-color-primary)' } }, `剩余${diffDays}天`)
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
      apiFn: fetchGetCertListByPage,
      apiParams: {
        current: 1,
        size: 10
      },
      columnsFactory: () => [
        { type: 'selection' },
        {
          prop: 'sanDomains',
          label: '认证域名',
          minWidth: 180,
          formatter: (row: SslItem) => row.sanDomains?.join(', ') || ''
        },
        {
          prop: 'org',
          label: '证书分类',
          minWidth: 120
        },
        {
          prop: 'issuer',
          label: '证书品牌',
          minWidth: 100
        },
        {
          prop: 'notAfter',
          label: '到期时间',
          minWidth: 160,
          formatter: (row: SslItem) => getExpireDays(row)
        },
        {
          prop: 'operation',
          label: '操作',
          width: 220,
          fixed: 'right',
          formatter: (row: SslItem) => {
            const now = new Date()
            const notAfter = new Date(row.notAfter)
            const isExpired = now > notAfter
            const children = []
            if (!isExpired) {
              children.push(
                h(ArtButtonTable, {
                  type: 'text',
                  text: '部署',
                  onClick: () => handleDeploy(row)
                }),
                h(ArtButtonTable, {
                  type: 'text',
                  text: '下载',
                  onClick: () => handleDownload(row)
                })
              )
            }
            children.push(
              h(ArtButtonTable, {
                type: 'delete',
                text: '删除',
                onClick: () => handleDelete(row)
              })
            )
            return h('div', children)
          }
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

  const handleDeploySubmit = () => {
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

  const handleDeploy = (row: SslItem) => {
    deployDialogVisible.value = true
  }

  const handleDownload = (row: SslItem) => {
    console.log('下载证书:', row)
  }

  const handleDelete = async (row: SslItem) => {
    try {
      await ElMessageBox.confirm('确定要删除该证书吗？', '证书删除确认', {
        confirmButtonText: '删除',
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

<style lang="scss" scoped>
  :deep(.el-dialog__body) {
    padding: 0 !important;
  }
</style>
