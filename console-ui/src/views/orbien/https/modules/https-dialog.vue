<template>
  <ElDialog
    v-model="dialogVisible"
    :title="dialogType === 'add' ? '添加 HTTPS 代理' : '编辑 HTTPS 代理'"
    width="650px"
    align-center
  >
    <ElForm ref="formRef" :model="formData" :rules="rules" label-width="120px" :show-message="false">
      <ElFormItem label="客户端" prop="agentId">
        <ElSelect
          v-model="formData.agentId"
          placeholder="请选择客户端"
          :disabled="dialogType === 'edit'"
          style="width: 250px"
        >
          <ElOption v-for="agent in agents" :key="agent.id" :label="agent.name" :value="agent.id" />
        </ElSelect>
      </ElFormItem>

      <ElFormItem label="代理名称" prop="name">
        <ElInput v-model="formData.name" placeholder="请输入代理名称" clearable />
      </ElFormItem>

      <ElFormItem label="域名类型" prop="domainType">
        <ElRadioGroup v-model="formData.domainType">
          <ElRadio :label="String(DomainType.AUTO)">自动</ElRadio>
          <ElRadio :label="String(DomainType.SUBDOMAIN)">子域名</ElRadio>
          <ElRadio :label="String(DomainType.CUSTOM_DOMAIN)">自定义域名</ElRadio>
        </ElRadioGroup>
      </ElFormItem>

      <ElFormItem v-if="formData.domainType === String(DomainType.SUBDOMAIN)">
        <SubdomainBindingRows
          v-model="formData.subdomainBindings"
          :root-domains="rootDomains"
          :loading="rootDomainLoading"
          :error-indexes="subdomainErrorIndexes"
          @clear-error="resetSubdomainErrors"
        />
      </ElFormItem>

      <ElFormItem
        v-if="formData.domainType === String(DomainType.CUSTOM_DOMAIN)"
        label="自定义域名"
        prop="customDomains"
      >
        <ElInput
          v-model="formData.customDomains"
          type="textarea"
          :rows="3"
          placeholder="请输入完整域名，多个用换行分隔，如 www.example.com"
        />
      </ElFormItem>

      <ElFormItem label="内网服务" prop="localHost">
        <ElRow :gutter="20">
          <ElCol :span="12">
            <ElInput v-model="formData.localHost" placeholder="如127.0.0.1" />
          </ElCol>
          <ElCol :span="12">
            <ElInput v-model.number="formData.localPort" type="number" placeholder="内网端口">
              <template #prepend>
                <el-select v-model="formData.localPort" placeholder="常用端口" style="width: 115px">
                  <el-option label="HTTP - 80" :value="80" />
                  <el-option label="HTTPS - 443" :value="443" />
                  <el-option label="SSH - 22" :value="22" />
                  <el-option label="Redis - 6379" :value="6379" />
                  <el-option label="Tomcat - 8080" :value="8080" />
                  <el-option label="MySQL - 3306" :value="3306" />
                  <el-option label="SQL Server - 1433" :value="1433" />
                  <el-option label="Windows远程桌面 - 3389" :value="3389" />
                </el-select>
              </template>
            </ElInput>
          </ElCol>
        </ElRow>
      </ElFormItem>

      <ElFormItem label="强制HTTPS">
        <ElSwitch v-model="formData.forceHttps" />
      </ElFormItem>

      <ElFormItem label="带宽限制" prop="limitTotal">
        <el-input
          v-model.number="formData.limitTotal"
          placeholder="总带宽"
          type="number"
          :precision="0"
          style="width: 200px"
        >
          <template #append>Mbps</template>
        </el-input>
      </ElFormItem>
    </ElForm>
    <template #footer>
      <div class="dialog-footer">
        <ElButton @click="dialogVisible = false">取消</ElButton>
        <ElButton type="primary" @click="handleSubmit">提交</ElButton>
      </div>
    </template>
  </ElDialog>
</template>

<script setup lang="ts">
  import { ref, reactive, watch, computed } from 'vue'
  import { ElMessage } from 'element-plus'
  import type { FormInstance, FormRules } from 'element-plus'
  import { DialogType } from '@/types'
  import { fetchGetAgentListAll } from '@/api/agent'
  import { fetchCreateHttpsProxy, fetchUpdateHttpsProxy, fetchGetHttpsProxyById } from '@/api/proxy'
  import { DomainType } from '@/enums/orbien/business'
  import { useRootDomainOptions, validateSubdomainBindings, buildSubdomainBindingsPayload } from '@/views/orbien/common/use-root-domain-options'
  import SubdomainBindingRows from '@/views/orbien/common/subdomain-binding-rows.vue'

  defineOptions({ name: 'HttpsDialog' })

  interface FormDataState {
    agentId: string
    name: string
    domainType: string
    subdomainBindings: Api.Proxy.SubdomainBindingParam[]
    customDomains: string
    localHost: string
    localPort: number | undefined
    forceHttps: boolean
    limitTotal: number
  }

  interface Props {
    visible: boolean
    type: DialogType
    proxyData?: Partial<{
      id: string
      agentId: string
      name: string
    }>
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
  const formRef = ref<FormInstance>()
  const agents = ref<Api.Agent.AgentDTO[]>([])
  const {
    rootDomains,
    rootDomainLoading,
    loadRootDomains,
    createDefaultSubdomainBinding,
    normalizeSubdomainBindings
  } = useRootDomainOptions()

  const createDefaultFormData = (): FormDataState => ({
    agentId: '',
    name: '',
    domainType: String(DomainType.AUTO),
    subdomainBindings: [createDefaultSubdomainBinding()],
    customDomains: '',
    localHost: '127.0.0.1',
    localPort: undefined,
    forceHttps: true,
    limitTotal: 1
  })

  const formData = reactive<FormDataState>(createDefaultFormData())
  const subdomainErrorIndexes = ref<number[]>([])

  const resetSubdomainErrors = () => {
    subdomainErrorIndexes.value = []
  }

  const rules: FormRules = {
    agentId: [{ required: true, message: '请选择客户端', trigger: 'change' }],
    name: [{ required: true, message: '请输入代理名称', trigger: 'blur' }],
    domainType: [{ required: true, message: '请选择域名类型', trigger: 'change' }],
    customDomains: [
      {
        validator: (_rule, value: string, callback) => {
          if (formData.domainType !== String(DomainType.CUSTOM_DOMAIN)) {
            callback()
            return
          }
          if (!value?.trim()) {
            callback(new Error('请输入自定义域名'))
          } else {
            callback()
          }
        },
        trigger: 'blur'
      }
    ],
    localHost: [{ required: true, message: '请输入主机', trigger: 'blur' }],
    localPort: [
      { required: true, message: '请输入端口', trigger: 'blur' },
      { type: 'number', message: '端口必须是数字', trigger: 'blur' },
      { min: 1, max: 65535, message: '端口必须在 1-65535 之间', trigger: 'blur' }
    ],
    limitTotal: [
      { required: true, message: '请输入带宽限制', trigger: 'blur' },
      { type: 'number', min: 1, message: '带宽必须大于 0', trigger: 'blur' }
    ]
  }

  const parseLines = (value: string): string[] =>
    value
      .split('\n')
      .map((item) => item.trim())
      .filter(Boolean)

  const refreshSubdomainBindings = () => {
    if (formData.domainType !== String(DomainType.SUBDOMAIN)) {
      return
    }
    formData.subdomainBindings = normalizeSubdomainBindings(formData.subdomainBindings)
  }

  const applyDetail = (detail: Api.Proxy.HttpsProxyDetailDTO) => {
    Object.assign(formData, {
      ...createDefaultFormData(),
      agentId: detail.agentId || '',
      name: detail.name || '',
      domainType: detail.domainType?.toString() || String(DomainType.AUTO),
      subdomainBindings: normalizeSubdomainBindings(detail.subdomainBindings),
      customDomains: (detail.customDomains || []).join('\n'),
      localHost: detail.localHost || '127.0.0.1',
      localPort: detail.localPort,
      forceHttps: detail.forceHttps ?? false,
      limitTotal: detail.limitTotal ?? 1
    })
    refreshSubdomainBindings()
  }

  const fetchAgents = async () => {
    try {
      agents.value = (await fetchGetAgentListAll()) || []
    } catch (error) {
      console.error('获取客户端列表失败:', error)
      ElMessage.error('获取客户端列表失败')
    }
  }

  const applyDefaultAgentIfNeeded = () => {
    if (props.type === 'edit' || formData.agentId || agents.value.length === 0) {
      return
    }
    formData.agentId = agents.value[0].id
  }

  const resetFormData = () => {
    Object.assign(formData, createDefaultFormData())
    refreshSubdomainBindings()
  }

  const initFormData = async () => {
    const isEdit = props.type === 'edit' && props.proxyData?.id

    if (!isEdit) {
      resetFormData()
      return
    }

    try {
      applyDetail(await fetchGetHttpsProxyById(props.proxyData!.id!))
    } catch (error) {
      console.error('获取代理详情失败:', error)
      ElMessage.error('获取代理详情失败，请稍后重试')
      Object.assign(formData, {
        ...createDefaultFormData(),
        agentId: props.proxyData?.agentId || '',
        name: props.proxyData?.name || ''
      })
    }
  }

  watch(
    () => [props.visible, props.type, props.proxyData] as const,
    async ([visible]) => {
      if (visible) {
        if (props.type === 'add') {
          resetFormData()
        }
        formRef.value?.clearValidate()
        resetSubdomainErrors()
        await Promise.all([fetchAgents(), loadRootDomains()])
        await initFormData()
        applyDefaultAgentIfNeeded()
        refreshSubdomainBindings()
      }
    },
    { immediate: true }
  )

  watch(
    () => formData.domainType,
    (domainType) => {
      resetSubdomainErrors()
      if (domainType === String(DomainType.SUBDOMAIN)) {
        if (formData.subdomainBindings.length === 0) {
          formData.subdomainBindings = [createDefaultSubdomainBinding()]
        }
        refreshSubdomainBindings()
      }
    }
  )

  watch(rootDomains, () => {
    refreshSubdomainBindings()
  })

  watch(dialogVisible, (visible) => {
    emit('update:visible', visible)
    if (!visible) {
      resetFormData()
      resetSubdomainErrors()
      formRef.value?.clearValidate()
    }
  })

  const buildDomainPayload = () => {
    const domainType = parseInt(formData.domainType, 10)
    if (domainType === DomainType.SUBDOMAIN) {
      return {
        subdomainBindings: buildSubdomainBindingsPayload(formData.subdomainBindings)
      }
    }
    if (domainType === DomainType.CUSTOM_DOMAIN) {
      return { customDomains: parseLines(formData.customDomains) }
    }
    return {}
  }

  const handleSubmit = async () => {
    if (!formRef.value) return

    resetSubdomainErrors()

    let formValid = false
    try {
      await formRef.value.validate()
      formValid = true
    } catch {
      formValid = false
    }

    const subdomainResult =
      formData.domainType === String(DomainType.SUBDOMAIN)
        ? validateSubdomainBindings(formData.subdomainBindings)
        : { valid: true, errorIndexes: [] as number[] }

    if (!subdomainResult.valid) {
      subdomainErrorIndexes.value = subdomainResult.errorIndexes
      if (subdomainResult.message) {
        ElMessage.warning(subdomainResult.message)
      }
    }

    if (!formValid || !subdomainResult.valid) return

    try {
      const commonData: Omit<Api.Proxy.HttpsProxyUpdateParam, 'id'> = {
        name: formData.name,
        domainType: parseInt(formData.domainType, 10),
        localHost: formData.localHost,
        localPort: formData.localPort!,
        forceHttps: formData.forceHttps,
        limitTotal: formData.limitTotal,
        ...buildDomainPayload()
      }

      if (dialogType.value === 'add') {
        await fetchCreateHttpsProxy({
          agentId: formData.agentId,
          ...commonData
        })
      } else {
        await fetchUpdateHttpsProxy({
          id: props.proxyData!.id!,
          ...commonData
        })
      }

      dialogVisible.value = false
      emit('submit')
      resetFormData()
      resetSubdomainErrors()
      formRef.value?.clearValidate()
    } catch (error) {
      console.error('提交失败:', error)
    }
  }
</script>
