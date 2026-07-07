<template>
  <div class="acme-order-panel">
    <ArtTableHeader v-model:columns="columnChecks" :loading="loading" @refresh="refreshData">
      <template #left>
        <ElSpace wrap>
          <ElButton type="primary" @click="emit('apply')" v-ripple>免费申请</ElButton>
          <ElButton @click="handleBatchDelete" v-ripple :disabled="selectedRows.length === 0">
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

    <AcmeOrderDetailDrawer
        v-model:visible="detailVisible"
        :order-id="currentOrderId"
        @changed="refreshData"
    />
  </div>
</template>

<script setup lang="ts">
import {h, ref} from 'vue'
import {ElMessage, ElMessageBox, ElTag} from 'element-plus'
import {useTable} from '@/hooks/core/useTable'
import ArtButtonTable from '@/components/core/forms/art-button-table/index.vue'
import AcmeOrderDetailDrawer from './acme-order-detail-drawer.vue'
import {
  fetchAcmeOrderPage,
  fetchCancelAcmeOrder,
  fetchDeleteAcmeOrders,
  fetchRetryAcmeOrder,
  fetchVerifyAcmeOrder
} from '@/api/acme-order'
import {resolveAcmeOrderStatusTagType} from '@/utils/ui/status-tag'

defineOptions({name: 'AcmeOrderPanel'})

interface Emits {
  (e: 'apply'): void
}

const emit = defineEmits<Emits>()

const detailVisible = ref(false)
const currentOrderId = ref<number | null>(null)
const selectedRows = ref<Api.AcmeOrder.OrderDTO[]>([])

const openDetail = (row: Api.AcmeOrder.OrderDTO) => {
  currentOrderId.value = row.id
  detailVisible.value = true
}

const handleSelectionChange = (selection: Api.AcmeOrder.OrderDTO[]) => {
  selectedRows.value = selection
}

const handleVerify = async (row: Api.AcmeOrder.OrderDTO) => {
  await fetchVerifyAcmeOrder(row.id)
  ElMessage.success('已开始验证')
  refreshData()
}

const handleRetry = async (row: Api.AcmeOrder.OrderDTO) => {
  await fetchRetryAcmeOrder(row.id)
  ElMessage.success('已重新提交')
  refreshData()
}

const handleCancel = async (row: Api.AcmeOrder.OrderDTO) => {
  await fetchCancelAcmeOrder(row.id)
  ElMessage.success('已取消')
  refreshData()
}

const handleDelete = async (row: Api.AcmeOrder.OrderDTO) => {
  try {
    await ElMessageBox.confirm(`确定删除申请记录「${row.orderNo}」吗？`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await fetchDeleteAcmeOrders([row.id])
    ElMessage.success('删除成功')
    if (currentOrderId.value === row.id) {
      detailVisible.value = false
      currentOrderId.value = null
    }
    refreshData()
  } catch (error) {
    if (error === 'cancel') return
  }
}

const handleBatchDelete = async () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请选择要删除的申请记录')
    return
  }
  try {
    await ElMessageBox.confirm(
        `确定删除选中的 ${selectedRows.value.length} 条申请记录吗？`,
        '批量删除确认',
        {
          confirmButtonText: '删除',
          cancelButtonText: '取消',
          type: 'warning'
        }
    )
    const ids = selectedRows.value.map((row) => row.id)
    await fetchDeleteAcmeOrders(ids)
    ElMessage.success('删除成功')
    selectedRows.value = []
    detailVisible.value = false
    currentOrderId.value = null
    refreshData()
  } catch (error) {
    if (error === 'cancel') return
  }
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
    apiFn: fetchAcmeOrderPage,
    apiParams: {current: 1, size: 10},
    columnsFactory: () => [
      {type: 'selection'},
      {prop: 'orderNo', label: '订单号', minWidth: 160},
      {
        prop: 'domains',
        label: '域名',
        minWidth: 180,
        formatter: (row: Api.AcmeOrder.OrderDTO) => row.domains?.join(', ') || ''
      },
      {
        prop: 'validationMode',
        label: '验证方式',
        width: 120,
        formatter: (row: Api.AcmeOrder.OrderDTO) => (row.validationMode === 2 ? '云DNS' : '手动')
      },
      {
        prop: 'status',
        label: '状态',
        width: 110,
        formatter: (row: Api.AcmeOrder.OrderDTO) =>
            h(
                ElTag,
                {type: resolveAcmeOrderStatusTagType(row.status), size: 'small'},
                () => row.statusLabel
            )
      },
      {prop: 'createdAt', label: '创建时间', width: 170},
      {
        prop: 'operation',
        label: '操作',
        width: 240,
        fixed: 'right',
        formatter: (row: Api.AcmeOrder.OrderDTO) => {
          const actions = [
            h(ArtButtonTable, {
              type: 'link',
              text: '详情',
              onClick: () => openDetail(row)
            })
          ]
          if ([1, 2].includes(row.status) && row.validationMode === 1) {
            actions.push(
                h(ArtButtonTable, {
                  type: 'link',
                  text: '验证',
                  onClick: () => handleVerify(row)
                })
            )
          }
          if (row.status === 6) {
            actions.push(
                h(ArtButtonTable, {
                  type: 'link',
                  text: '重试',
                  onClick: () => handleRetry(row)
                })
            )
          }
          if (![5, 6, 7].includes(row.status)) {
            actions.push(
                h(ArtButtonTable, {
                  type: 'link',
                  text: '取消',
                  onClick: () => handleCancel(row)
                })
            )
          }
          actions.push(
              h(ArtButtonTable, {
                type: 'link',
                text: '删除',
                onClick: () => handleDelete(row)
              })
          )
          return h('div', actions)
        }
      }
    ]
  }
})

defineExpose({refreshData})
</script>
