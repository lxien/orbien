<template>
  <ElDialog
    v-model="dialogVisible"
    :title="dialogType === 'add' ? '添加端口' : '编辑端口'"
    width="500px"
    align-center
  >
    <div v-if="loading" class="loading-state">
      <ElSkeleton :rows="4" animated />
    </div>
    <ElForm v-else ref="formRef" :model="formData" :rules="rules" label-width="100px" :show-message="false">
      <ElFormItem label="应用类型">
        <ElSelect
          v-model="quickFillId"
          placeholder="请选择应用类型"
          class="w-full"
          @change="handleAppTypeChange"
        >
          <ElOption label="自定义" :value="CUSTOM_PRESET_ID" />
          <ElOptionGroup v-for="group in presetGroups" :key="group" :label="group">
            <ElOption
              v-for="preset in presetsByGroup[group]"
              :key="preset.id"
              :label="preset.label"
              :value="preset.id"
            />
          </ElOptionGroup>
        </ElSelect>
      </ElFormItem>

      <ElFormItem label="协议" prop="type">
        <ElSelect
          v-model="formData.type"
          placeholder="请选择协议"
          class="w-full"
          :disabled="!isCustomMode"
        >
          <ElOption
            v-for="item in PORT_POOL_TYPE_OPTIONS"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </ElSelect>
      </ElFormItem>
      <ElFormItem label="端口" prop="port">
        <ElInput
          v-model="formData.port"
          :disabled="!isCustomMode"
          placeholder="单个端口如 8000，范围端口如 8000-9000"
        />
      </ElFormItem>
      <ElFormItem label="备注" prop="remark">
        <ElInput
          v-model="formData.remark"
          type="textarea"
          :rows="3"
          placeholder="请输入备注"
          maxlength="500"
          show-word-limit
        />
      </ElFormItem>
    </ElForm>
    <template #footer>
      <div class="dialog-footer">
        <ElButton @click="dialogVisible = false">取消</ElButton>
        <ElButton type="primary" @click="handleSubmit" :loading="submitting">提交</ElButton>
      </div>
    </template>
  </ElDialog>
</template>

<script setup lang="ts">
  import { ref, reactive, computed, watch, nextTick } from 'vue'
  import type { FormInstance, FormRules } from 'element-plus'
  import { ElMessage } from 'element-plus'
  import {
    fetchCreatePortPool,
    fetchUpdatePortPool,
    fetchGetPortPoolById
  } from '@/api/port-pool'
  import { PORT_POOL_TYPE_OPTIONS, PortPoolType } from '@/enums/orbien/business'
  import {
    CUSTOM_PRESET_ID,
    PORT_POOL_PRESETS,
    PORT_POOL_PRESET_GROUPS,
    type PortPoolPreset
  } from './port-pool-presets'

  interface Props {
    visible: boolean
    type: string
    portPoolId?: number
  }

  interface Emits {
    (e: 'update:visible', value: boolean): void
    (e: 'submit'): void
  }

  const props = defineProps<Props>()
  const emit = defineEmits<Emits>()

  const dialogVisible = computed({
    get: () => props.visible,
    set: (value) => emit('update:visible', value)
  })

  const dialogType = computed(() => props.type)
  const loading = ref(false)
  const submitting = ref(false)
  const formRef = ref<FormInstance>()
  const quickFillId = ref(CUSTOM_PRESET_ID)

  const isCustomMode = computed(() => quickFillId.value === CUSTOM_PRESET_ID)

  const presetGroups = PORT_POOL_PRESET_GROUPS
  const presetsByGroup = computed(() => {
    return presetGroups.reduce<Record<string, PortPoolPreset[]>>((acc, group) => {
      acc[group] = PORT_POOL_PRESETS.filter((item) => item.group === group)
      return acc
    }, {})
  })

  const formData = reactive({
    id: undefined as number | undefined,
    type: PortPoolType.TCP,
    port: '',
    remark: ''
  })

  const validationError = () => new Error(' ')

  const validatePort = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
    if (!isCustomMode.value) {
      callback()
      return
    }
    if (!value?.trim()) {
      callback(validationError())
      return
    }
    const trimmed = value.trim()
    const singlePattern = /^\d+$/
    const rangePattern = /^(\d+)-(\d+)$/

    const validatePortNumber = (port: number) => port >= 1 && port <= 65535

    if (singlePattern.test(trimmed)) {
      const port = Number(trimmed)
      if (!validatePortNumber(port)) {
        callback(validationError())
        return
      }
      callback()
      return
    }

    if (rangePattern.test(trimmed)) {
      const match = trimmed.match(rangePattern)
      if (!match) {
        callback(validationError())
        return
      }
      const start = Number(match[1])
      const end = Number(match[2])
      if (!validatePortNumber(start) || !validatePortNumber(end)) {
        callback(validationError())
        return
      }
      if (end <= start) {
        callback(validationError())
        return
      }
      callback()
      return
    }

    callback(validationError())
  }

  const rules: FormRules = {
    type: [{ required: true, message: ' ', trigger: 'change' }],
    port: [{ required: true, validator: validatePort, trigger: 'blur' }]
  }

  const formatPortDisplay = (startPort: number, endPort?: number) => {
    return endPort ? `${startPort}-${endPort}` : `${startPort}`
  }

  const applyPreset = (preset: PortPoolPreset) => {
    formData.type = preset.type
    formData.port = preset.port
    formData.remark = preset.remark
    nextTick(() => {
      formRef.value?.clearValidate(['type', 'port'])
    })
  }

  const handleAppTypeChange = (presetId: string) => {
    if (presetId === CUSTOM_PRESET_ID) {
      nextTick(() => {
        formRef.value?.clearValidate(['type', 'port'])
      })
      return
    }

    const preset = PORT_POOL_PRESETS.find((item) => item.id === presetId)
    if (preset) {
      applyPreset(preset)
    }
  }

  const initFormData = async () => {
    quickFillId.value = CUSTOM_PRESET_ID

    if (props.type === 'add') {
      Object.assign(formData, {
        id: undefined,
        type: PortPoolType.TCP,
        port: '',
        remark: ''
      })
      return
    }

    if (props.type === 'edit' && props.portPoolId) {
      loading.value = true
      try {
        const data = await fetchGetPortPoolById(props.portPoolId)
        Object.assign(formData, {
          id: data.id,
          type: data.type,
          port: formatPortDisplay(data.startPort, data.endPort),
          remark: data.remark || ''
        })
      } catch (error) {
        console.error('获取端口配置失败:', error)
        ElMessage.error('获取端口配置失败')
      } finally {
        loading.value = false
      }
    }
  }

  watch(
    () => [props.visible, props.type, props.portPoolId],
    ([visible]) => {
      if (visible) {
        initFormData()
        nextTick(() => {
          formRef.value?.clearValidate()
        })
      }
    },
    { immediate: true }
  )

  const handleSubmit = async () => {
    if (!formRef.value) return

    await formRef.value.validate(async (valid) => {
      if (!valid) return

      submitting.value = true
      try {
        const payload = {
          port: formData.port.trim(),
          type: formData.type,
          remark: formData.remark.trim() || undefined
        }
        if (dialogType.value === 'add') {
          await fetchCreatePortPool(payload)
          ElMessage.success('添加成功')
        } else if (formData.id) {
          await fetchUpdatePortPool({
            id: formData.id,
            ...payload
          })
          ElMessage.success('更新成功')
        }
        dialogVisible.value = false
        emit('submit')
      } catch (error) {
        console.error('操作失败:', error)
      } finally {
        submitting.value = false
      }
    })
  }
</script>
