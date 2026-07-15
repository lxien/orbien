<template>
  <div class="time-access-page">
    <div class="mb-6">
      <h3 class="text-lg font-semibold mb-4">基本配置</h3>
      <div class="flex flex-col gap-4">
        <div class="flex items-center gap-3">
          <span class="w-20 font-medium">启用状态：</span>
          <ElSwitch v-model="formData.enabled" @change="handleConfigChange" />
        </div>
        <div class="flex items-center gap-3">
          <span class="w-20 font-medium">控制模式：</span>
          <ElRadioGroup v-model="formData.mode" @change="handleConfigChange">
            <ElRadio :label="AccessControl.ALLOW">允许（仅窗口内可访问）</ElRadio>
            <ElRadio :label="AccessControl.DENY">禁止（仅窗口内不可访问）</ElRadio>
          </ElRadioGroup>
        </div>
        <div class="flex items-center gap-3">
          <span class="w-20 font-medium">时区：</span>
          <ElSelect
            v-model="formData.timezone"
            filterable
            allow-create
            default-first-option
            style="width: 280px"
            @change="handleConfigChange"
          >
            <ElOption v-for="tz in timezoneOptions" :key="tz" :label="tz" :value="tz" />
          </ElSelect>
        </div>
      </div>
    </div>

    <div class="mb-6">
      <h3 class="text-lg font-semibold mb-4">周期限制</h3>
      <div class="flex flex-wrap items-center gap-3">
        <ElCheckbox
          :model-value="isAllDaysSelected"
          :indeterminate="isDaysIndeterminate"
          @change="handleSelectAllDays"
        >
          全选
        </ElCheckbox>
        <ElCheckboxGroup v-model="formData.days" @change="handleConfigChange">
          <ElCheckbox v-for="day in weekDays" :key="day.value" :label="day.value">
            {{ day.label }}
          </ElCheckbox>
        </ElCheckboxGroup>
      </div>
    </div>

    <div class="mb-4">
      <div class="flex items-center gap-3 mb-4">
        <h3 class="text-lg font-semibold">时间限制</h3>
        <ElSwitch v-model="formData.timeEnabled" @change="handleConfigChange" />
      </div>
      <div
        class="border border-gray-200 rounded p-4"
        :class="{ 'opacity-50 pointer-events-none': !formData.timeEnabled }"
      >
        <ElTable :data="formData.windows" style="width: 100%" border>
          <ElTableColumn label="开始时间" min-width="180">
            <template #default="scope">
              <ElTimePicker
                v-if="editingWindowId === scope.row.id"
                v-model="scope.row.start"
                size="small"
                value-format="HH:mm:ss"
                placeholder="选择开始时间"
                style="width: 100%"
              />
              <span v-else>{{ scope.row.start }}</span>
            </template>
          </ElTableColumn>
          <ElTableColumn label="结束时间" min-width="180">
            <template #default="scope">
              <ElTimePicker
                v-if="editingWindowId === scope.row.id"
                v-model="scope.row.end"
                size="small"
                value-format="HH:mm:ss"
                placeholder="选择结束时间"
                style="width: 100%"
              />
              <span v-else>{{ scope.row.end }}</span>
            </template>
          </ElTableColumn>
          <ElTableColumn label="操作" width="180" fixed="right">
            <template #default="scope">
              <ElSpace size="small">
                <ElButton
                  v-if="editingWindowId === scope.row.id"
                  type="primary"
                  size="small"
                  @click="handleSaveWindow(scope.row)"
                >
                  保存
                </ElButton>
                <ElButton v-else type="link" size="small" @click="handleEditWindow(scope.row)">
                  编辑
                </ElButton>
                <ElButton type="link" size="small" @click="handleDeleteWindow(scope.row)">
                  <template #icon>
                    <Delete />
                  </template>
                  删除
                </ElButton>
              </ElSpace>
            </template>
          </ElTableColumn>
        </ElTable>
        <ElButton type="primary" size="small" class="mt-3" @click="addWindow">
          <template #icon>
            <Plus />
          </template>
          新增时段
        </ElButton>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref, reactive, watch, computed } from 'vue'
  import { ElMessage, ElMessageBox } from 'element-plus'
  import { Plus, Delete } from '@element-plus/icons-vue'
  import {
    fetchGetTimeAccess,
    fetchUpdateTimeAccess,
    fetchAddTimeAccessWindow,
    fetchUpdateTimeAccessWindow,
    fetchDeleteTimeAccessWindow
  } from '@/api/time-access'
  import { AccessControl } from '@/enums/orbien/business'

  defineOptions({ name: 'TimeAccessPage' })

  const props = defineProps<{
    proxyId: string
  }>()

  const weekDays = [
    { value: 1, label: '周一' },
    { value: 2, label: '周二' },
    { value: 3, label: '周三' },
    { value: 4, label: '周四' },
    { value: 5, label: '周五' },
    { value: 6, label: '周六' },
    { value: 7, label: '周日' }
  ]

  const timezoneOptions = [
    'Asia/Shanghai',
    'Asia/Hong_Kong',
    'Asia/Tokyo',
    'UTC',
    'America/New_York',
    'Europe/London'
  ]

  const formData = reactive({
    enabled: false,
    mode: AccessControl.ALLOW as number,
    timeEnabled: true,
    timezone: 'Asia/Shanghai',
    days: [] as number[],
    windows: [] as Api.TimeAccess.WindowDTO[]
  })

  const editingWindowId = ref<number | null>(null)

  const isAllDaysSelected = computed(() => formData.days.length === 7)
  const isDaysIndeterminate = computed(() => formData.days.length > 0 && formData.days.length < 7)

  const resetForm = () => {
    formData.enabled = false
    formData.mode = AccessControl.ALLOW
    formData.timeEnabled = true
    formData.timezone = 'Asia/Shanghai'
    formData.days = []
    formData.windows = []
    editingWindowId.value = null
  }

  const fetchData = async () => {
    const response = await fetchGetTimeAccess(props.proxyId)
    if (!response) return
    formData.enabled = !!response.enabled
    formData.mode = response.mode ?? AccessControl.ALLOW
    formData.timeEnabled = response.timeEnabled !== false
    formData.timezone = response.timezone || 'Asia/Shanghai'
    formData.days = [...(response.days || [])]
    formData.windows = (response.windows || []).map((w) => ({ ...w }))
  }

  watch(
    () => props.proxyId,
    async (id) => {
      if (!id) return
      resetForm()
      await fetchData()
    },
    { immediate: true }
  )

  const handleConfigChange = async () => {
    await fetchUpdateTimeAccess({
      proxyId: props.proxyId,
      enabled: formData.enabled,
      mode: formData.mode,
      timeEnabled: formData.timeEnabled,
      timezone: formData.timezone,
      days: [...formData.days]
    })
  }

  const handleSelectAllDays = async (checked: boolean | string | number) => {
    formData.days = checked ? weekDays.map((d) => d.value) : []
    await handleConfigChange()
  }

  const addWindow = () => {
    formData.windows.push({
      id: 0,
      start: '',
      end: ''
    })
    editingWindowId.value = 0
  }

  const handleEditWindow = (row: Api.TimeAccess.WindowDTO) => {
    editingWindowId.value = row.id ?? 0
  }

  const handleSaveWindow = async (row: Api.TimeAccess.WindowDTO) => {
    if (!row.start || !row.end) {
      ElMessage.error('请选择开始与结束时间')
      return
    }
    if (row.id && row.id > 0) {
      await fetchUpdateTimeAccessWindow({
        id: row.id,
        proxyId: props.proxyId,
        start: row.start,
        end: row.end
      })
    } else {
      await fetchAddTimeAccessWindow({
        proxyId: props.proxyId,
        start: row.start,
        end: row.end
      })
    }
    editingWindowId.value = null
    await fetchData()
  }

  const handleDeleteWindow = async (row: Api.TimeAccess.WindowDTO) => {
    await ElMessageBox.confirm('确定删除该时段吗？', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    if (row.id && row.id > 0) {
      await fetchDeleteTimeAccessWindow(row.id)
      await fetchData()
      return
    }
    const index = formData.windows.indexOf(row)
    if (index > -1) {
      formData.windows.splice(index, 1)
    }
    editingWindowId.value = null
  }
</script>
