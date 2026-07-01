<template>
  <ElDialog
    v-model="dialogVisible"
    :title="dialogType === 'add' ? '新增 HTTPS 代理' : '编辑 HTTPS 代理'"
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
          <ElRadio label="0">自动</ElRadio>
          <ElRadio label="1">子域名</ElRadio>
          <ElRadio label="2">自定义域名</ElRadio>
        </ElRadioGroup>
      </ElFormItem>

      <ElFormItem v-show="formData.domainType !== '0'" label="域名" prop="domains">
        <ElInput
          v-model="formData.domains"
          type="textarea"
          :rows="3"
          placeholder="请输入域名，多个域名用换行分隔"
        />
      </ElFormItem>

      <ElFormItem label="强制HTTPS">
        <ElSwitch v-model="formData.forceHttps" />
      </ElFormItem>

      <ElFormItem label="内网服务" prop="localIp">
        <ElRow :gutter="20">
          <ElCol :span="12">
            <ElInput v-model="formData.localIp" placeholder="如127.0.0.1" />
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

      <ElFormItem label="带宽限制" prop="limitTotal">
        <el-input
          v-model="formData.limitTotal"
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

  defineOptions({ name: 'HttpsDialog' })

  const MBPS = 1_000_000

  interface TargetSnapshot {
    host: string
    port: number | string
    weight: number
    name: string
  }

  interface FormDataState {
    agentId: string
    name: string
    status: string
    domainType: string
    domains: string
    localIp: string
    localPort: number | string
    limitTotal: number | undefined
    forceHttps: boolean
  }

  interface Props {
    visible: boolean
    type: DialogType
    proxyData?: Partial<{
      id: string
      agentId: string
      name: string
      status: number
      domainType: number
      domains: string[]
      targets: TargetSnapshot[]
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

  const clusterSnapshot = ref<{ targets: TargetSnapshot[]; loadBalanceStrategy: string }>({
    targets: [],
    loadBalanceStrategy: '1'
  })
  const transportSnapshot = ref<Api.Proxy.TransportSaveParam>({
    encrypt: false,
    tunnelType: 0
  })
  const bandwidthSnapshot = ref<{ limitIn: number | null; limitOut: number | null }>({
    limitIn: null,
    limitOut: null
  })

  const DEFAULT_FORM_DATA: FormDataState = {
    agentId: '',
    name: '',
    status: '1',
    domainType: '0',
    domains: '',
    localIp: '127.0.0.1',
    localPort: '',
    limitTotal: undefined,
    forceHttps: false
  }

  const formData = reactive<FormDataState>({ ...DEFAULT_FORM_DATA })

  const rules: FormRules = {
    agentId: [{ required: true, message: '请选择客户端', trigger: 'change' }],
    name: [{ required: true, message: '请输入代理名称', trigger: 'blur' }],
    domainType: [{ required: true, message: '请选择域名类型', trigger: 'change' }],
    domains: [
      {
        validator: (_rule, value: string, callback) => {
          if (formData.domainType !== '0' && (!value || !value.trim())) {
            callback(new Error('请输入域名'))
          } else {
            callback()
          }
        },
        trigger: 'blur'
      }
    ],
    localIp: [{ required: true, message: '请输入主机', trigger: 'blur' }],
    localPort: [
      { required: true, message: '请输入端口', trigger: 'blur' },
      { type: 'number', message: '端口必须是数字', trigger: 'blur' },
      { min: 1, max: 65535, message: '端口必须在 1-65535 之间', trigger: 'blur' }
    ]
  }

  const toMbps = (bps?: number | null) => (bps != null ? Math.round(bps / MBPS) : undefined)

  const mapTarget = (target: Api.Proxy.TargetDTO): TargetSnapshot => ({
    host: target.host || '',
    port: target.port || '',
    weight: target.weight || 1,
    name: target.name || ''
  })

  const resetSnapshots = () => {
    clusterSnapshot.value = { targets: [], loadBalanceStrategy: '1' }
    transportSnapshot.value = { encrypt: false, tunnelType: 0 }
    bandwidthSnapshot.value = { limitIn: null, limitOut: null }
  }

  const applyDetail = (detail: Api.Proxy.HttpsProxyDetailDTO) => {
    const targets = detail.targets?.map(mapTarget) || []
    clusterSnapshot.value = {
      targets,
      loadBalanceStrategy: detail.loadBalance?.strategy?.toString() || '1'
    }
    transportSnapshot.value = {
      encrypt: detail.transport?.encrypt ?? false,
      tunnelType: detail.transport?.tunnelType ?? 0
    }
    bandwidthSnapshot.value = {
      limitIn: detail.bandwidth?.limitIn ?? null,
      limitOut: detail.bandwidth?.limitOut ?? null
    }
    Object.assign(formData, {
      ...DEFAULT_FORM_DATA,
      agentId: detail.agentId || '',
      name: detail.name || '',
      status: detail.status?.toString() || '1',
      domainType: detail.domainType?.toString() || '0',
      domains: (detail.domains || []).join('\n'),
      localIp: targets[0]?.host || '127.0.0.1',
      localPort: targets[0]?.port || '',
      limitTotal: toMbps(detail.bandwidth?.limitTotal),
      forceHttps: detail.forceHttps ?? false
    })
  }

  const fetchAgents = async () => {
    try {
      agents.value = (await fetchGetAgentListAll()) || []
    } catch (error) {
      console.error('获取客户端列表失败:', error)
      ElMessage.error('获取客户端列表失败')
    }
  }

  const resetFormData = () => {
    Object.assign(formData, { ...DEFAULT_FORM_DATA })
    resetSnapshots()
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
      const row = props.proxyData
      const targets = row?.targets?.map((t) => mapTarget(t as Api.Proxy.TargetDTO)) || []
      clusterSnapshot.value = { targets, loadBalanceStrategy: '1' }
      Object.assign(formData, {
        ...DEFAULT_FORM_DATA,
        agentId: row?.agentId || '',
        name: row?.name || '',
        status: row?.status?.toString() || '1',
        domainType: row?.domainType?.toString() || '0',
        domains: row?.domains?.join('\n') || '',
        localIp: targets[0]?.host || '127.0.0.1',
        localPort: targets[0]?.port || ''
      })
    }
  }

  watch(
    () => [props.visible, props.type, props.proxyData] as const,
    async ([visible]) => {
      if (visible) {
        resetFormData()
        formRef.value?.clearValidate()
        await fetchAgents()
        await initFormData()
      }
    },
    { immediate: true }
  )

  watch(dialogVisible, (visible) => {
    emit('update:visible', visible)
    if (!visible) {
      resetFormData()
      formRef.value?.clearValidate()
    }
  })

  const buildTargets = () => {
    const { targets } = clusterSnapshot.value
    if (targets.length >= 2) {
      return targets.map((t) => ({
        host: t.host,
        port: Number(t.port),
        weight: t.weight,
        name: t.name
      }))
    }
    return [
      {
        name: formData.name,
        host: formData.localIp,
        port: Number(formData.localPort),
        weight: 1
      }
    ]
  }

  const handleSubmit = async () => {
    if (!formRef.value) return

    await formRef.value.validate(async (valid) => {
      if (!valid) return

      try {
        const domains =
          formData.domainType === '0'
            ? []
            : formData.domains
                .split('\n')
                .map((v) => v.trim())
                .filter(Boolean)

        const clusterTargets = clusterSnapshot.value.targets
        const isCluster = clusterTargets.length >= 2

        const commonData = {
          name: formData.name,
          status: parseInt(formData.status),
          domainType: parseInt(formData.domainType),
          domains,
          forceHttps: formData.forceHttps,
          deploymentMode: isCluster ? 0 : 1,
          targets: buildTargets(),
          bandwidth: {
            limitTotal: formData.limitTotal != null ? Number(formData.limitTotal) * MBPS : null,
            limitIn: bandwidthSnapshot.value.limitIn,
            limitOut: bandwidthSnapshot.value.limitOut,
            unit: 'Mbps'
          },
          loadBalance: isCluster
            ? { strategy: parseInt(clusterSnapshot.value.loadBalanceStrategy) }
            : null,
          transport: transportSnapshot.value
        }

        if (dialogType.value === 'add') {
          await fetchCreateHttpsProxy({ agentId: formData.agentId, ...commonData })
        } else {
          await fetchUpdateHttpsProxy({ id: props.proxyData!.id!, ...commonData })
        }

        dialogVisible.value = false
        emit('submit')
        resetFormData()
        formRef.value?.clearValidate()
      } catch (error) {
        console.error('提交失败:', error)
      }
    })
  }
</script>

<style scoped></style>
