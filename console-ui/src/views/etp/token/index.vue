<template>
  <div class="token-page art-full-height">
    <ElCard class="art-table-card">
      <!-- 表格头部 -->
      <ArtTableHeader v-model:columns="columnChecks" :loading="loading" @refresh="refreshData">
        <template #left>
          <ElSpace wrap>
            <ElButton type="primary" @click="showDialog('add')" v-ripple>新增访问令牌</ElButton>
            <ElButton @click="handleBatchDelete" :disabled="selectedRows.length === 0" v-ripple
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

      <!-- 访问令牌弹窗 -->
      <TokenDialog
        v-model:visible="dialogVisible"
        :type="dialogType"
        :token-id="currentTokenId"
        @submit="handleDialogSubmit"
        @create-success="handleCreateSuccess"
      />
      
      <!-- Token创建成功弹窗 -->
      <TokenSuccess
        v-model:visible="tokenSuccessVisible"
        :token="createdToken"
        @close="handleTokenSuccessClose"
      />
    </ElCard>
  </div>
</template>

<script setup lang="ts">
  import { ref, h, nextTick } from 'vue'
  import ArtButtonTable from '@/components/core/forms/art-button-table/index.vue'
  import { useTable } from '@/hooks/core/useTable'
  import { fetchGetTokenList, fetchDeleteToken, fetchDeleteBatchTokens } from '@/api/token'
  import TokenDialog from './modules/token-dialog.vue'
import TokenSuccess from './modules/token-success.vue'
import { ElMessageBox, ElMessage } from 'element-plus'
  import { DialogType } from '@/types'

  defineOptions({ name: 'TokenManagement' })

  // 选中行
  const selectedRows = ref<Api.AccessToken.AccessTokenDTO[]>([])

  // 弹窗相关
  const dialogType = ref<DialogType>('add')
  const dialogVisible = ref(false)
  const currentTokenId = ref<number | undefined>()
  
  // Token成功弹窗
  const tokenSuccessVisible = ref(false)
  const createdToken = ref('')

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
      apiFn: fetchGetTokenList,
      apiParams: {
        current: 1,
        size: 20
      },
      paginationKey: {
        current: 'current',
        size: 'size'
      },
      columnsFactory: () => [
        { type: 'selection' },
        {
          prop: 'name',
          label: '令牌名称',
          minWidth: 50
        },
        {
          prop: 'token',
          label: '令牌',
          minWidth: 200
        },
        {
          prop: 'remark',
          label: '描述',
          minWidth: 150
        },
        {
          prop: 'operation',
          label: '操作',
          width: 130,
          fixed: 'right',
          formatter: (row: Api.AccessToken.AccessTokenDTO) =>
            h('div', [
              h(ArtButtonTable, {
                type: 'link',
                text: '编辑',
                onClick: () => showDialog('edit', row)
              }),
              h(ArtButtonTable, {
                type: 'link',
                text: '删除',
                onClick: () => deleteToken(row)
              })
            ])
        }
      ]
    }
  })

  /**
   * 删除令牌
   */
  const deleteToken = (row: Api.AccessToken.AccessTokenDTO): void => {
    ElMessageBox.confirm(`确定要删除该访问令牌吗？`, '删除令牌', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'error'
    }).then(async () => {
      await fetchDeleteToken(row.id)
      ElMessage.success('删除成功')
      refreshData()
    })
  }

  /**
   * 批量删除令牌
   */
  const handleBatchDelete = (): void => {
    if (selectedRows.value.length === 0) return

    ElMessageBox.confirm(
      `确定要删除选中的 ${selectedRows.value.length} 个访问令牌吗？`,
      '批量删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'error'
      }
    ).then(async () => {
      const ids = selectedRows.value.map((row) => row.id)
      await fetchDeleteBatchTokens(ids)
      ElMessage.success('批量删除成功')
      refreshData()
    })
  }

  /**
   * 处理表格行选择变化
   */
  const handleSelectionChange = (selection: Api.AccessToken.AccessTokenDTO[]): void => {
    selectedRows.value = selection
  }

  /**
   * 显示对话框
   */
  const showDialog = (type: DialogType, row?: Api.AccessToken.AccessTokenDTO): void => {
    dialogType.value = type
    currentTokenId.value = row?.id
    nextTick(() => {
      dialogVisible.value = true
    })
  }

  /**
   * 处理对话框提交（编辑/更新）
   */
  const handleDialogSubmit = () => {
    refreshData()
  }

  /**
   * 处理创建成功
   */
  const handleCreateSuccess = (token: string) => {
    createdToken.value = token
    tokenSuccessVisible.value = true
  }

  /**
   * 处理成功弹窗关闭
   */
  const handleTokenSuccessClose = () => {
    createdToken.value = ''
    refreshData()
  }
</script>

<style lang="scss" scoped></style>
