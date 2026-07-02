<template>
  <ElDialog
    v-model="dialogVisible"
    :title="dialogType === 'add' ? '添加 TCP 代理' : '编辑 TCP 代理'"
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
      <ElFormItem label="远程端口" prop="remotePort">
        <ElInput
          v-model.number="formData.remotePort"
          type="number"
          placeholder="不填自动分配"
          style="width: 200px"
        />
      </ElFormItem>
      <ElFormItem label="带宽限制" prop="limitTotal">
        <el-input
          v-model="formData.limitTotal"
          placeholder="总带宽"
          :controls="false"
          :min="0"
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
  import { ProxyStatus } from '@/enums/etp/business'

  defineOptions({ name: 'TcpDialog' })

  interface FormDataState {
    agentId: string
    name: string
    status: string
    remotePort: string | number
    localIp: string
    localPort: number
    limitTotal: number | undefined
  }

  interface Props {
    visible: boolean
    type: DialogType
    proxyData?: Partial<{
      id: string
      agentId: string
      name: string
      status: number
      remotePort: number
      targets: { host: string; port: number }[]
      bandwidth: {
        limitTotal: number | null
      }
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

  watch(
    () => props.visible,
    (newVal) => {
      dialogVisible.value = newVal
    }
  )

  const DEFAULT_FORM_DATA: FormDataState = {
    agentId: '',
    name: '',
    remotePort: '',
    localIp: '127.0.0.1',
    localPort: '',
    limitTotal: 1
  }
  const formData = reactive<FormDataState>({ ...DEFAULT_FORM_DATA })

  const rules: FormRules = {
    agentId: [{ required: true, message: '请选择客户端', trigger: 'change' }],
    name: [{ required: true, message: '请输入代理名称', trigger: 'blur' }],
    status: [{ required: true, message: '请选择状态', trigger: 'change' }],
    remotePort: [
      {
        validator: (rule: any, value: any, callback: any) => {
          if (!value) {
            callback()
            return
          }
          const numValue = parseInt(value)
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
    localIp: [{ required: true, message: '请输入主机', trigger: 'blur' }],
    localPort: [
      { required: true, message: '请输入端口', trigger: 'blur' },
      { type: 'number', message: '端口必须是数字', trigger: 'blur' },
      { min: 1, max: 65535, message: '端口必须在 1-65535 之间', trigger: 'blur' }
    ]
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

  const resetFormData = () => {
    Object.assign(formData, { ...DEFAULT_FORM_DATA })
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
          status: proxyDetail.status?.toString() || String(ProxyStatus.OPEN),
          remotePort: proxyDetail.listenPort || undefined,
          localIp: proxyDetail.targets?.[0]?.host || '127.0.0.1',
          localPort: proxyDetail.targets?.[0]?.port || '',
          limitTotal:
            proxyDetail.bandwidth?.limitTotal != null
              ? Math.round(proxyDetail.bandwidth.limitTotal / 1000000)
              : undefined
        })
      } catch (error) {
        console.error('获取代理详情失败:', error)
        ElMessage.error('获取代理详情失败，请稍后重试')
        const row = props.proxyData
        Object.assign(formData, {
          ...DEFAULT_FORM_DATA,
          agentId: row ? row.agentId || '' : '',
          name: row ? row.name || '' : '',
          status: row ? row.status?.toString() || String(ProxyStatus.OPEN) : String(ProxyStatus.OPEN),
          remotePort: row ? row.remotePort || 0 : 0,
          localIp: row?.targets?.[0]?.host || '127.0.0.1',
          localPort: row?.targets?.[0]?.port || '',
          limitTotal:
            row?.bandwidth?.limitTotal != null
              ? Math.round(row.bandwidth.limitTotal / 1000000)
              : undefined
        })
      }
    } else {
      resetFormData()
    }
  }

  watch(
    () => [props.visible, props.type, props.proxyData],
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

  watch(dialogVisible, (newVal) => {
    emit('update:visible', newVal)
    if (!newVal) {
      resetFormData()
      formRef.value?.clearValidate()
    }
  })

  const handleSubmit = async () => {
    if (!formRef.value) return

    const remotePortNum = parseInt(formData.remotePort as any)
    formData.remotePort = remotePortNum

    await formRef.value.validate(async (valid) => {
      if (valid) {
        try {
          const commonData = {
            name: formData.name,
            status: parseInt(formData.status),
            remotePort: Number(formData.remotePort),
            deploymentMode: 1,
            targets: [
              {
                name: formData.name,
                host: formData.localIp,
                port: parseInt(formData.localPort as any) || 0,
                weight: 1
              }
            ],
            bandwidth: {
              limitTotal: formData.limitTotal != null ? formData.limitTotal * 1000000 : null,
              unit: 'Mbps'
            },
            loadBalance: null,
            transport: {
              tunnelType: 1,
              encrypt: false
            }
          }

          if (dialogType.value === 'add') {
            const requestData = {
              agentId: formData.agentId,
              ...commonData
            }
            await fetchCreateTcpProxy(requestData)
          } else {
            const requestData = {
              id: props.proxyData?.id || '',
              ...commonData
            }
            await fetchUpdateTcpProxy(requestData)
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

<style scoped></style>
