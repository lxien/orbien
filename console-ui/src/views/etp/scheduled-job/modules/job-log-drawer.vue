<template>
  <ElDrawer
    v-model="drawerVisible"
    :title="`执行日志 - ${jobName || ''}`"
    size="760px"
    destroy-on-close
  >
    <ArtTableHeader :loading="loading" @refresh="refreshData">
      <template #left>
        <ElButton @click="handleBatchDelete" :disabled="selectedRows.length === 0" v-ripple>
          批量删除
        </ElButton>
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
  </ElDrawer>
</template>

<script setup lang="ts">
  import { computed, h, ref, watch } from 'vue'
  import { ElMessage, ElMessageBox, ElTag } from 'element-plus'
  import { useTable } from '@/hooks/core/useTable'
  import ArtButtonTable from '@/components/core/forms/art-button-table/index.vue'
  import { fetchDeleteScheduledJobLogs, fetchScheduledJobLogs } from '@/api/scheduled-job'
  import { resolveBinaryOutcomeTagType } from '@/utils/ui/status-tag'

  defineOptions({ name: 'JobLogDrawer' })

  interface Props {
    visible: boolean
    jobCode?: string | null
    jobName?: string
  }

  interface Emits {
    (e: 'update:visible', value: boolean): void
  }

  const props = defineProps<Props>()
  const emit = defineEmits<Emits>()

  const selectedRows = ref<Api.ScheduledJob.JobLogDTO[]>([])

  const drawerVisible = computed({
    get: () => props.visible,
    set: (value) => emit('update:visible', value)
  })

  const handleSelectionChange = (selection: Api.ScheduledJob.JobLogDTO[]) => {
    selectedRows.value = selection
  }

  const deleteLogs = async (rows: Api.ScheduledJob.JobLogDTO[]) => {
    if (!props.jobCode || rows.length === 0) {
      return
    }
    const ids = rows.map((row) => row.id)
    await fetchDeleteScheduledJobLogs(props.jobCode, ids)
    ElMessage.success('删除成功')
    selectedRows.value = []
    refreshData()
  }

  const handleDelete = async (row: Api.ScheduledJob.JobLogDTO) => {
    try {
      await ElMessageBox.confirm('确定删除该条执行日志吗？', '删除确认', {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      })
      await deleteLogs([row])
    } catch (error) {
      if (error === 'cancel') return
    }
  }

  const handleBatchDelete = async () => {
    if (selectedRows.value.length === 0) {
      ElMessage.warning('请选择要删除的日志')
      return
    }
    try {
      await ElMessageBox.confirm(
        `确定删除选中的 ${selectedRows.value.length} 条执行日志吗？`,
        '批量删除确认',
        {
          confirmButtonText: '删除',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )
      await deleteLogs(selectedRows.value)
    } catch (error) {
      if (error === 'cancel') return
    }
  }

  const {
    columns,
    data,
    loading,
    pagination,
    handleSizeChange,
    handleCurrentChange,
    refreshData,
    resetSearchParams
  } = useTable<Api.ScheduledJob.JobLogDTO>({
    core: {
      apiFn: (params) => {
        if (!props.jobCode) {
          return Promise.resolve({
            records: [],
            total: 0,
            current: params.current ?? 1,
            size: params.size ?? 10
          })
        }
        return fetchScheduledJobLogs(props.jobCode, params)
      },
      apiParams: {
        current: 1,
        size: 10
      },
      immediate: false,
      columnsFactory: () => [
        { type: 'selection' },
        {
          prop: 'startedAt',
          label: '开始时间',
          width: 170
        },
        {
          prop: 'finishedAt',
          label: '结束时间',
          width: 170
        },
        {
          prop: 'triggerTypeLabel',
          label: '触发方式',
          width: 90
        },
        {
          prop: 'statusLabel',
          label: '状态',
          width: 80,
          formatter: (row: Api.ScheduledJob.JobLogDTO) =>
            h(
              ElTag,
              { size: 'small', type: resolveBinaryOutcomeTagType(row.status) },
              () => row.statusLabel
            )
        },
        {
          prop: 'affectedCount',
          label: '影响数量',
          width: 90
        },

        {
          prop: 'message',
          label: '说明',
          minWidth: 140,
          showOverflowTooltip: true
        },
        {
          prop: 'operation',
          label: '操作',
          width: 80,
          fixed: 'right',
          formatter: (row: Api.ScheduledJob.JobLogDTO) =>
            h(ArtButtonTable, {
              type: 'link',
              text: '删除',
              onClick: () => handleDelete(row)
            })
        }
      ]
    }
  })

  watch(
    () => [props.visible, props.jobCode],
    ([visible, jobCode]) => {
      if (visible && jobCode) {
        selectedRows.value = []
        resetSearchParams()
        refreshData()
      }
    }
  )
</script>

<style lang="scss" scoped>
  :deep(.art-table) {
    padding: 0;
  }
</style>
