<template>
  <ElDialog
    v-model="dialogVisible"
    :title="dialogType === 'add' ? '添加 TCP 代理' : '编辑 TCP 代理'"
    width="650px"
    align-center
  >
    <ElForm
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-width="120px"
      :show-message="false"
    >
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

      <ElFormItem label="内网服务" prop="localHost">
        <ElRow :gutter="20">
          <ElCol :span="12">
            <ElInput v-model="formData.localHost" placeholder="如127.0.0.1" />
          </ElCol>
          <ElCol :span="12">
            <ElInput v-model.number="formData.localPort" type="number" placeholder="内网端口">
              <template #prepend>
                <el-select v-model="formData.localPort" placeholder="常用端口" style="width: 125px">
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
      <ElFormItem label="远程端口" prop="remotePort">
        <div class="remote-port-field">
          <ElInput
            v-model="remotePortInput"
            type="number"
            placeholder="不填自动生成"
            class="remote-port-input"
            @input="onRemotePortInput"
          />
          <ElButton
            v-for="port in suggestedPorts"
            :key="port"
            size="small"
            :type="selectedSuggestPort === port ? 'primary' : 'default'"
            plain
            @click="selectSuggestedPort(port)"
          >
            {{ port }}
          </ElButton>
          <span v-if="!suggestLoading && !suggestedPorts.length" class="port-suggestions-empty">
            暂无
          </span>
          <ElButton link type="primary" :loading="suggestLoading" @click="loadSuggestedPorts">
            换一批
          </ElButton>
        </div>
      </ElFormItem>
      <ElFormItem label="带宽限制" prop="limitTotal">
        <el-input
          v-model.number="formData.limitTotal"
          placeholder="总带宽"
          :controls="false"
          :min="1"
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
  import { fetchCreateTcpProxy, fetchUpdateTcpProxy, fetchGetTcpProxyById } from '@/api/proxy'
  import { fetchSuggestAvailablePorts } from '@/api/port-pool'
  import { PortPoolType } from '@/enums/orbien/business'

  defineOptions({ name: 'TcpDialog' })

  interface FormDataState {
    agentId: string
    name: string
    localHost: string
    localPort: number | undefined
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
  const suggestedPorts = ref<number[]>([])
  const selectedSuggestPort = ref<number | null>(null)
  const suggestLoading = ref(false)
  const remotePortInput = ref('')
  const SUGGEST_PORT_COUNT = 4

  watch(
    () => props.visible,
    (newVal) => {
      dialogVisible.value = newVal
    }
  )

  const DEFAULT_FORM_DATA: FormDataState = {
    agentId: '',
    name: '',
    localHost: '127.0.0.1',
    localPort: undefined,
    limitTotal: 1
  }
  const formData = reactive<FormDataState>({ ...DEFAULT_FORM_DATA })

  const rules: FormRules = {
    agentId: [{ required: true, message: '请选择客户端', trigger: 'change' }],
    name: [{ required: true, message: '请输入代理名称', trigger: 'blur' }],
    remotePort: [
      {
        validator: (_rule, _value, callback) => {
          if (!remotePortInput.value) {
            callback()
            return
          }
          const numValue = parseInt(remotePortInput.value, 10)
          if (isNaN(numValue)) {
            callback(new Error('远程端口必须是数字'))
          } else if (numValue < 1 || numValue > 65535) {
            callback(new Error('远程端口必须在 1-65535 之间'))
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

  const parseRemotePort = (): number | undefined => {
    if (!remotePortInput.value) {
      return undefined
    }
    const num = parseInt(remotePortInput.value, 10)
    return isNaN(num) ? undefined : num
  }

  const fetchAgents = async () => {
    try {
      const agentsList = await fetchGetAgentListAll()
      agents.value = agentsList || []
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

  const resetSuggestState = () => {
    suggestedPorts.value = []
    selectedSuggestPort.value = null
  }

  const resetFormData = () => {
    Object.assign(formData, { ...DEFAULT_FORM_DATA })
    remotePortInput.value = ''
    resetSuggestState()
  }

  const loadSuggestedPorts = async () => {
    suggestLoading.value = true
    selectedSuggestPort.value = null
    try {
      suggestedPorts.value = await fetchSuggestAvailablePorts(PortPoolType.TCP, SUGGEST_PORT_COUNT)
    } catch (error) {
      console.error('获取可用端口失败:', error)
      suggestedPorts.value = []
    } finally {
      suggestLoading.value = false
    }
  }

  const onRemotePortInput = () => {
    selectedSuggestPort.value = null
  }

  const selectSuggestedPort = (port: number) => {
    remotePortInput.value = String(port)
    selectedSuggestPort.value = port
    formRef.value?.clearValidate('remotePort')
  }

  const initFormData = async () => {
    const isEdit = props.type === 'edit' && props.proxyData && props.proxyData.id

    if (isEdit) {
      try {
        const proxyDetail = await fetchGetTcpProxyById(props.proxyData!.id!)
        Object.assign(formData, {
          ...DEFAULT_FORM_DATA,
          agentId: proxyDetail.agentId || '',
          name: proxyDetail.name || '',
          localHost: proxyDetail.localHost || '127.0.0.1',
          localPort: proxyDetail.localPort,
          limitTotal: proxyDetail.limitTotal ?? 1
        })
        const displayPort = proxyDetail.remotePort ?? proxyDetail.listenPort
        remotePortInput.value = displayPort != null ? String(displayPort) : ''
      } catch (error) {
        console.error('获取代理详情失败:', error)
        ElMessage.error('获取代理详情失败，请稍后重试')
        const row = props.proxyData
        Object.assign(formData, {
          ...DEFAULT_FORM_DATA,
          agentId: row?.agentId || '',
          name: row?.name || ''
        })
        remotePortInput.value = ''
      }
    } else {
      resetFormData()
    }
    await loadSuggestedPorts()
  }

  watch(
    () => [props.visible, props.type, props.proxyData],
    async ([visible]) => {
      if (visible) {
        if (props.type === 'add') {
          resetFormData()
        } else {
          resetSuggestState()
        }
        formRef.value?.clearValidate()
        await fetchAgents()
        await initFormData()
        applyDefaultAgentIfNeeded()
      }
    },
    { immediate: true }
  )

  watch(dialogVisible, (newVal) => {
    emit('update:visible', newVal)
    if (!newVal) {
      resetFormData()
      formRef.value?.clearValidate()
    }
  })

  const handleSubmit = async () => {
    if (!formRef.value) return

    await formRef.value.validate(async (valid) => {
      if (valid) {
        try {
          const remotePort = parseRemotePort()
          const commonData: Omit<Api.Proxy.TcpProxyUpdateParam, 'id'> = {
            name: formData.name,
            localHost: formData.localHost,
            localPort: formData.localPort!,
            limitTotal: formData.limitTotal,
            ...(remotePort != null ? { remotePort } : {})
          }

          if (dialogType.value === 'add') {
            await fetchCreateTcpProxy({
              agentId: formData.agentId,
              ...commonData
            })
          } else {
            await fetchUpdateTcpProxy({
              id: props.proxyData?.id || '',
              ...commonData
            })
          }

          dialogVisible.value = false
          emit('submit')
          resetFormData()
          formRef.value?.clearValidate()
        } catch (error) {
          console.error('提交失败:', error)
        }
      }
    })
  }
</script>

<style scoped lang="scss">
  .remote-port-field {
    display: flex;
    flex-wrap: nowrap;
    gap: 8px;
    align-items: center;
    width: 100%;
  }

  .remote-port-input {
    width: 146px;
    flex-shrink: 0;
  }

  .port-suggestions-empty {
    font-size: 12px;
    color: var(--el-text-color-secondary);
    white-space: nowrap;
  }
</style>
