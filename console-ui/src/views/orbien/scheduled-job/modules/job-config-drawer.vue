<template>
  <ElDrawer
    v-model="drawerVisible"
    :title="`配置 - ${job?.jobName || ''}`"
    size="560px"
    destroy-on-close
  >
    <div v-loading="loading" class="drawer-body">
      <ElForm v-if="form" ref="formRef" :model="form" label-position="top" class="job-config-form">
        <section v-if="job?.description" class="form-section">
          <div class="section-label">任务说明</div>
          <div class="desc-block">{{ job.description }}</div>
        </section>

        <section class="form-section">
          <div class="section-label">执行周期</div>
          <CronSchedulePicker v-model="form.cronExpression" />
        </section>

        <section v-if="job?.paramSchema?.length" class="form-section">
          <div class="section-label">任务参数</div>
          <div class="param-list">
            <div v-for="field in job.paramSchema" :key="field.key" class="param-item">
              <div class="param-label">{{ field.label }}</div>
              <ElInputNumber
                v-if="field.type === 'number'"
                v-model="form.params[field.key] as number"
                :min="field.min"
                :max="field.max"
                controls-position="right"
                class="param-number"
              />
              <div v-else-if="field.type === 'boolean'" class="param-switch">
                <ElSwitch v-model="form.params[field.key] as boolean" size="small" />
                <span class="switch-text">
                  {{ form.params[field.key] ? '开启' : '关闭' }}
                </span>
              </div>
              <ElInput v-else v-model="form.params[field.key] as string" class="param-input" />
              <div v-if="field.description" class="field-tip">{{ field.description }}</div>
            </div>
          </div>
        </section>
      </ElForm>

      <div class="drawer-footer">
        <ElButton @click="drawerVisible = false">取消</ElButton>
        <ElButton type="primary" :loading="submitting" @click="handleSubmit">保存</ElButton>
      </div>
    </div>
  </ElDrawer>
</template>

<script setup lang="ts">
  import { computed, ref, watch } from 'vue'
  import { ElMessage } from 'element-plus'
  import { fetchScheduledJobDetail, fetchUpdateScheduledJob } from '@/api/scheduled-job'
  import CronSchedulePicker from './cron-schedule-picker.vue'

  defineOptions({ name: 'JobConfigDrawer' })

  interface Props {
    visible: boolean
    jobCode?: string | null
  }

  interface Emits {
    (e: 'update:visible', value: boolean): void
    (e: 'saved'): void
  }

  const props = defineProps<Props>()
  const emit = defineEmits<Emits>()

  const loading = ref(false)
  const submitting = ref(false)
  const job = ref<Api.ScheduledJob.JobDTO | null>(null)
  const form = ref<Api.ScheduledJob.JobUpdateParam | null>(null)

  const drawerVisible = computed({
    get: () => props.visible,
    set: (value) => emit('update:visible', value)
  })

  const loadDetail = async () => {
    if (!props.jobCode) {
      return
    }
    loading.value = true
    try {
      const detail = await fetchScheduledJobDetail(props.jobCode)
      job.value = detail
      form.value = {
        cronExpression: detail.cronExpression,
        params: { ...detail.params }
      }
    } finally {
      loading.value = false
    }
  }

  const handleSubmit = async () => {
    if (!props.jobCode || !form.value) {
      return
    }
    submitting.value = true
    try {
      await fetchUpdateScheduledJob(props.jobCode, form.value)
      ElMessage.success('保存成功')
      emit('saved')
      drawerVisible.value = false
    } finally {
      submitting.value = false
    }
  }

  watch(
    () => [props.visible, props.jobCode],
    ([visible]) => {
      if (visible) {
        loadDetail()
      }
    }
  )
</script>

<style lang="scss" scoped>
  .drawer-body {
    min-height: 100%;
    padding-bottom: 72px;
  }

  .job-config-form {
    padding: 4px 0;
  }

  .form-section + .form-section {
    margin-top: 24px;
    padding-top: 24px;
    border-top: 1px solid var(--el-border-color-lighter);
  }

  .section-label {
    margin-bottom: 12px;
    font-size: 14px;
    font-weight: 500;
    color: var(--el-text-color-primary);
  }

  .desc-block {
    padding: 12px 14px;
    font-size: 13px;
    line-height: 1.6;
    color: var(--el-text-color-secondary);
    background: var(--el-fill-color-light);
    border-radius: var(--el-border-radius-base);
  }

  .param-list {
    display: flex;
    flex-direction: column;
    gap: 20px;
  }

  .param-label {
    margin-bottom: 8px;
    font-size: 13px;
    color: var(--el-text-color-regular);
  }

  .param-number {
    width: 160px;
  }

  .param-input {
    max-width: 320px;
  }

  .param-switch {
    display: inline-flex;
    gap: 10px;
    align-items: center;
  }

  .switch-text {
    font-size: 13px;
    color: var(--el-text-color-secondary);
  }

  .field-tip {
    margin-top: 6px;
    font-size: 12px;
    line-height: 1.5;
    color: var(--el-text-color-placeholder);
  }

  .drawer-footer {
    position: absolute;
    right: 0;
    bottom: 0;
    left: 0;
    display: flex;
    justify-content: flex-end;
    gap: 12px;
    padding: 16px 20px;
    background: var(--el-bg-color);
    border-top: 1px solid var(--el-border-color-lighter);
  }
</style>
