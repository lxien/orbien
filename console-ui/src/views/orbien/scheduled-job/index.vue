<template>
  <div class="scheduled-job-page art-full-height">
    <ElCard class="art-table-card">
      <ArtTableHeader v-model:columns="columnChecks" :loading="loading" @refresh="refreshData" />

      <ArtTable
        :loading="loading"
        :data="data"
        :columns="columns"
        :show-pagination="false"
      />
    </ElCard>

    <JobConfigDrawer
      v-model:visible="configVisible"
      :job-code="currentJobCode"
      @saved="refreshData"
    />
    <JobLogDrawer
      v-model:visible="logVisible"
      :job-code="currentJobCode"
      :job-name="currentJobName"
    />
  </div>
</template>

<script setup lang="ts">
  import { h, ref } from 'vue'
  import { ElMessage, ElMessageBox, ElSwitch, ElTag } from 'element-plus'
  import { useTable } from '@/hooks/core/useTable'
  import ArtButtonTable from '@/components/core/forms/art-button-table/index.vue'
  import {
    fetchRunScheduledJob,
    fetchScheduledJobList,
    fetchUpdateScheduledJobEnabled
  } from '@/api/scheduled-job'
  import { describeCronExpression } from '@/utils/cron-schedule'
  import { resolveBinaryOutcomeTagType } from '@/utils/ui/status-tag'
  import JobConfigDrawer from './modules/job-config-drawer.vue'
  import JobLogDrawer from './modules/job-log-drawer.vue'

  defineOptions({ name: 'ScheduledJobManagement' })

  const ACME_RENEW_JOB_CODE = 'ACME_RENEW'

  const configVisible = ref(false)
  const logVisible = ref(false)
  const currentJobCode = ref<string | null>(null)
  const currentJobName = ref('')
  const togglingCode = ref<string | null>(null)

  const openConfig = (row: Api.ScheduledJob.JobDTO) => {
    currentJobCode.value = row.jobCode
    configVisible.value = true
  }

  const openLogs = (row: Api.ScheduledJob.JobDTO) => {
    currentJobCode.value = row.jobCode
    currentJobName.value = row.jobName
    logVisible.value = true
  }

  const handleEnabledChange = async (row: Api.ScheduledJob.JobDTO, enabled: boolean) => {
    if (!enabled && row.jobCode === ACME_RENEW_JOB_CODE) {
      try {
        await ElMessageBox.confirm(
          '停用后，已开启自动续签的证书将暂停自动续签，重新启用可恢复续签。',
          `停用「${row.jobName}」`,
          {
            confirmButtonText: '停用',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )
      } catch {
        return
      }
    }

    togglingCode.value = row.jobCode
    try {
      await fetchUpdateScheduledJobEnabled(row.jobCode, enabled)
      row.enabled = enabled
      if (!enabled) {
        row.nextRunAt = undefined
      }
      ElMessage.success(enabled ? '任务已启用' : '任务已停用')
      refreshData()
    } catch {
      row.enabled = !enabled
    } finally {
      togglingCode.value = null
    }
  }

  const handleRun = async (row: Api.ScheduledJob.JobDTO) => {
    try {
      await ElMessageBox.confirm(`确定立即执行任务「${row.jobName}」吗？`, '手动执行', {
        confirmButtonText: '执行',
        cancelButtonText: '取消',
        type: 'info'
      })
      await fetchRunScheduledJob(row.jobCode)
      ElMessage.success('任务已触发，请稍后查看执行日志')
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
    refreshData
  } = useTable<Api.ScheduledJob.JobDTO>({
    core: {
      apiFn: fetchScheduledJobList,
      columnsFactory: () => [
        {
          prop: 'jobName',
          label: '任务名称',
          minWidth: 160
        },
        {
          prop: 'enabled',
          label: '状态',
          width: 130,
          formatter: (row: Api.ScheduledJob.JobDTO) =>
            h('div', { class: 'status-cell' }, [
              h(
                'span',
                {
                  class: ['status-text', row.enabled ? 'is-active' : '']
                },
                row.enabled ? '正常' : '停用'
              ),
              h(ElSwitch, {
                modelValue: row.enabled,
                size: 'small',
                loading: togglingCode.value === row.jobCode,
                'onUpdate:modelValue': (enabled: string | number | boolean) =>
                  handleEnabledChange(row, Boolean(enabled))
              })
            ])
        },
        {
          prop: 'cronExpression',
          label: '执行周期',
          minWidth: 200,
          showOverflowTooltip: true,
          formatter: (row: Api.ScheduledJob.JobDTO) => describeCronExpression(row.cronExpression)
        },
        {
          prop: 'lastRunAt',
          label: '上次执行',
          width: 170
        },
        {
          prop: 'lastRunStatusLabel',
          label: '执行结果',
          width: 100,
          formatter: (row: Api.ScheduledJob.JobDTO) => {
            if (!row.lastRunStatusLabel) {
              return ''
            }
            return h(
              ElTag,
              { type: resolveBinaryOutcomeTagType(row.lastRunStatus), size: 'small' },
              () => row.lastRunStatusLabel
            )
          }
        },
        {
          prop: 'nextRunAt',
          label: '下次执行',
          width: 170,
          formatter: (row: Api.ScheduledJob.JobDTO) => (row.enabled ? row.nextRunAt || '' : '')
        },
        {
          prop: 'operation',
          label: '操作',
          width: 150,
          fixed: 'right',
          formatter: (row: Api.ScheduledJob.JobDTO) =>
            h('div', [
              h(ArtButtonTable, {
                type: 'link',
                text: '配置',
                onClick: () => openConfig(row)
              }),
              h(ArtButtonTable, {
                type: 'link',
                text: '执行',
                onClick: () => handleRun(row)
              }),
              h(ArtButtonTable, {
                type: 'link',
                text: '日志',
                onClick: () => openLogs(row)
              })
            ])
        }
      ]
    }
  })
</script>

<style lang="scss" scoped>
  :deep(.status-cell) {
    display: inline-flex;
    gap: 10px;
    align-items: center;
  }

  :deep(.status-text) {
    min-width: 28px;
    font-size: 13px;
    color: var(--el-text-color-secondary);

    &.is-active {
      color: var(--el-text-color-primary);
    }
  }
</style>
