<template>
  <ElDialog
    v-model="dialogVisible"
    :title="dialogType === 'add' ? '添加根域名' : '编辑根域名'"
    width="500px"
    align-center
  >
    <div v-if="loading" class="loading-state">
      <ElSkeleton :rows="3" animated />
    </div>
    <ElForm v-else ref="formRef" :model="formData" :rules="rules" label-width="100px" :show-message="false">
      <ElFormItem label="根域名" prop="domain">
        <ElInput
          v-model="formData.domain"
          :disabled="dialogType === 'edit'"
          placeholder="例如 example.com"
        />
      </ElFormItem>
      <ElFormItem label="描述" prop="remark">
        <ElInput
          v-model="formData.remark"
          type="textarea"
          :rows="3"
          placeholder="请输入描述"
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
  import { fetchCreateDomain, fetchUpdateDomain, fetchGetDomainById } from '@/api/domain'

  interface Props {
    visible: boolean
    type: string
    domainId?: number
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

  const formData = reactive({
    id: undefined as number | undefined,
    domain: '',
    remark: ''
  })

  const rules: FormRules = {
    domain: [{ required: true, message: '请输入根域名', trigger: 'blur' }]
  }

  const initFormData = async () => {
    if (props.type === 'add') {
      Object.assign(formData, {
        id: undefined,
        domain: '',
        remark: ''
      })
      return
    }

    if (props.type === 'edit' && props.domainId) {
      loading.value = true
      try {
        const data = await fetchGetDomainById(props.domainId)
        Object.assign(formData, {
          id: data.id,
          domain: data.domain || '',
          remark: data.remark || ''
        })
      } catch (error) {
        console.error('获取域名详情失败:', error)
        ElMessage.error('获取域名详情失败')
      } finally {
        loading.value = false
      }
    }
  }

  watch(
    () => [props.visible, props.type, props.domainId],
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
        if (dialogType.value === 'add') {
          await fetchCreateDomain({
            domain: formData.domain.trim(),
            remark: formData.remark.trim() || undefined
          })
          ElMessage.success('添加成功')
        } else if (formData.id) {
          await fetchUpdateDomain({
            id: formData.id,
            remark: formData.remark.trim() || undefined
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
