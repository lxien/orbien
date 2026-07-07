<template>
  <ElDialog
      v-model="dialogVisible"
      :title="formData.id ? '编辑 DNS 密钥' : '添加 DNS 密钥'"
      width="580px"
      align-center
      destroy-on-close
  >
    <ElForm
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-width="140px"
        autocomplete="off"
        :validate-on-rule-change="false"
        :show-message="false"
        class="dns-credential-form"
    >
      <div class="autofill-trap" aria-hidden="true">
        <input type="text" tabindex="-1" autocomplete="username"/>
        <input type="password" tabindex="-1" autocomplete="current-password"/>
      </div>

      <ElFormItem label="名称" prop="name">
        <ElInput
            v-model="formData.name"
            placeholder="如：生产环境-阿里云"
            maxlength="64"
            autocomplete="off"
            name="dns-credential-label"
        />
      </ElFormItem>
      <ElFormItem label="DNS 厂商" prop="provider">
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


      <ElAlert
          v-if="formData.id"
          type="info"
          :closable="false"
          show-icon
          title="编辑时需重新填写密钥信息"
          class="edit-tip"
      />

      <template v-if="currentSchema">
        <ElFormItem
            v-for="field in currentSchema.fields"
            :key="field.key"
            :prop="`config.${field.key}`"
        >
          <template #label>
            <span class="field-label">{{ field.label }}</span>
          </template>
          <ElInput
              v-model="formData.config[field.key]"
              :type="field.secret ? 'password' : 'text'"
              :placeholder="field.required ? '必填' : '选填'"
              :show-password="field.secret"
              :autocomplete="resolveFieldAutocomplete(field)"
              :name="`dns-credential-${formData.provider}-${field.key}`"
              readonly
              @focus="unlockFieldReadonly"
          />
        </ElFormItem>
      </template>
    </ElForm>
    <template #footer>
      <ElButton :disabled="submitting" @click="dialogVisible = false">取消</ElButton>
      <ElButton type="primary" :loading="submitting" @click="handleSubmit">保存并测试</ElButton>
    </template>
  </ElDialog>
</template>

<script setup lang="ts">
import {computed, nextTick, reactive, ref, watch} from 'vue'
import type {FormInstance, FormRules} from 'element-plus'
import {ElMessage} from 'element-plus'
import {fetchDnsProviderSchemas, fetchSaveDnsCredential} from '@/api/dns-credential'

defineOptions({name: 'DnsCredentialDialog'})

interface Props {
  visible: boolean
  record?: Api.DnsCredential.CredentialDTO | null
}

interface Emits {
  (e: 'update:visible', value: boolean): void

  (e: 'submit', saved?: Api.DnsCredential.CredentialDTO): void
}

const props = withDefaults(defineProps<Props>(), {record: null})
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

const resolveFieldAutocomplete = (field: Api.DnsCredential.ProviderField) =>
    field.secret ? 'new-password' : 'off'

const unlockFieldReadonly = (event: FocusEvent) => {
  const target = event.target as HTMLInputElement | null
  target?.removeAttribute('readonly')
}

const requiredRule = {required: true, message: ' ', trigger: 'blur' as const}

const rules = computed<FormRules>(() => {
  const dynamicRules: FormRules = {
    name: [requiredRule],
    provider: [{required: true, message: ' ', trigger: 'change' as const}]
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

const buildSubmitPayload = (): Api.DnsCredential.SaveParams => {
  const config: Record<string, string> = {}
  currentSchema.value?.fields.forEach((field) => {
    const value = formData.config[field.key]?.trim()
    if (value) {
      config[field.key] = value
    }
  })
  return {
    id: formData.id,
    name: formData.name.trim(),
    provider: formData.provider,
    config
  }
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
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    const saved = await fetchSaveDnsCredential(buildSubmitPayload())
    ElMessage.success('保存成功')
    dialogVisible.value = false
    emit('submit', saved)
  } finally {
    submitting.value = false
  }
}
</script>

<style lang="scss" scoped>
.dns-credential-form {
  position: relative;

  .autofill-trap {
    position: absolute;
    width: 0;
    height: 0;
    overflow: hidden;
    pointer-events: none;
    opacity: 0;
  }

  :deep(.el-form-item) {
    align-items: flex-start;
    margin-bottom: 18px;
  }

  :deep(.el-form-item__label) {
    display: inline-flex;
    align-items: flex-start;
    justify-content: flex-end;
    height: auto !important;
    min-height: var(--el-component-custom-height);
    padding-top: 7px;
    line-height: 1.4 !important;
    white-space: nowrap;
  }

  :deep(.el-form-item.is-required:not(.is-no-asterisk) > .el-form-item__label::before) {
    margin-top: 2px;
    margin-right: 4px;
  }

  :deep(.el-form-item__content) {
    min-width: 0;
  }

  :deep(.el-form-item__error) {
    display: none;
  }

  .field-label {
    display: inline-block;
    max-width: 100%;
    overflow: hidden;
    text-overflow: ellipsis;
    vertical-align: top;
  }
}

.edit-tip {
  margin-bottom: 16px;
}
</style>
