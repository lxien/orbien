<template>
  <ElDialog
    v-model="dialogVisible"
    :title="formData.id ? '编辑 DNS 密钥' : '添加 DNS 密钥'"
    width="560px"
    align-center
    destroy-on-close
  >
    <ElForm
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-width="120px"
      validate-on-rule-change="false"
      class="dns-credential-form"
    >
      <ElFormItem label="名称" prop="name" :show-message="false">
        <ElInput v-model="formData.name" placeholder="如：生产环境-阿里云" maxlength="64" />
      </ElFormItem>
      <ElFormItem label="DNS 厂商" prop="provider" :show-message="false">
        <ElSelect
          v-model="formData.provider"
          placeholder="请选择厂商"
          style="width: 100%"
          :disabled="!!formData.id"
          @change="handleProviderChange"
        >
          <ElOption
            v-for="item in providerSchemas"
            :key="item.provider"
            :label="item.label"
            :value="item.provider"
          />
        </ElSelect>
      </ElFormItem>
      <template v-if="currentSchema">
        <ElFormItem
          v-for="field in currentSchema.fields"
          :key="field.key"
          :label="field.label"
          :prop="`config.${field.key}`"
          :show-message="false"
        >
          <ElInput
            v-model="formData.config[field.key]"
            :type="field.secret ? 'password' : 'text'"
            :placeholder="field.required ? '必填' : '选填'"
            show-password
          />
        </ElFormItem>
      </template>
      <ElAlert
        v-if="formData.id"
        type="info"
        :closable="false"
        show-icon
        title="编辑时需重新填写密钥信息，保存后将重新测试连接"
        class="mb-2"
      />
    </ElForm>
    <template #footer>
      <ElButton @click="dialogVisible = false">取消</ElButton>
      <ElButton type="primary" :loading="submitting" @click="handleSubmit">保存并测试</ElButton>
    </template>
  </ElDialog>
</template>

<script setup lang="ts">
  import { computed, nextTick, reactive, ref, watch } from 'vue'
  import type { FormInstance, FormRules } from 'element-plus'
  import { ElMessage } from 'element-plus'
  import { fetchDnsProviderSchemas, fetchSaveDnsCredential } from '@/api/dns-credential'

  defineOptions({ name: 'DnsCredentialDialog' })

  interface Props {
    visible: boolean
    record?: Api.DnsCredential.CredentialDTO | null
  }

  interface Emits {
    (e: 'update:visible', value: boolean): void
    (e: 'submit'): void
  }

  const props = withDefaults(defineProps<Props>(), { record: null })
  const emit = defineEmits<Emits>()

  const dialogVisible = computed({
    get: () => props.visible,
    set: (value) => emit('update:visible', value)
  })

  const formRef = ref<FormInstance>()
  const submitting = ref(false)
  const providerSchemas = ref<Api.DnsCredential.ProviderSchema[]>([])

  const formData = reactive<Api.DnsCredential.SaveParams>({
    id: undefined,
    name: '',
    provider: 1,
    config: {}
  })

  const currentSchema = computed(() =>
    providerSchemas.value.find((item) => item.provider === formData.provider)
  )

  const requiredRule = { required: true, message: ' ', trigger: 'blur' as const }

  const rules = computed<FormRules>(() => {
    const dynamicRules: FormRules = {
      name: [requiredRule],
      provider: [{ required: true, message: ' ', trigger: 'change' as const }]
    }
    currentSchema.value?.fields.forEach((field) => {
      if (field.required) {
        dynamicRules[`config.${field.key}`] = [requiredRule]
      }
    })
    return dynamicRules
  })

  const resetConfig = () => {
    formData.config = {}
    currentSchema.value?.fields.forEach((field) => {
      formData.config[field.key] = ''
    })
  }

  const clearFormValidate = async () => {
    await nextTick()
    formRef.value?.clearValidate()
  }

  const handleProviderChange = async () => {
    resetConfig()
    await clearFormValidate()
  }

  const loadSchemas = async () => {
    providerSchemas.value = await fetchDnsProviderSchemas()
    if (!formData.provider && providerSchemas.value.length) {
      formData.provider = providerSchemas.value[0].provider
    }
    resetConfig()
  }

  watch(
    () => props.visible,
    async (visible) => {
      if (!visible) return
      await loadSchemas()
      if (props.record) {
        formData.id = props.record.id
        formData.name = props.record.name
        formData.provider = props.record.provider
        resetConfig()
      } else {
        formData.id = undefined
        formData.name = ''
        formData.provider = providerSchemas.value[0]?.provider || 1
        resetConfig()
      }
      await clearFormValidate()
    }
  )

  const handleSubmit = async () => {
    try {
      await formRef.value?.validate()
    } catch {
      return
    }
    submitting.value = true
    try {
      await fetchSaveDnsCredential({ ...formData, config: { ...formData.config } })
      ElMessage.success('保存成功')
      dialogVisible.value = false
      emit('submit')
    } finally {
      submitting.value = false
    }
  }
</script>

<style lang="scss" scoped>
  .dns-credential-form {
    :deep(.el-form-item__error) {
      display: none;
    }
  }
</style>
