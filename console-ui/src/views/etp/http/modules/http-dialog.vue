<template>
  <ElDialog
    v-model="dialogVisible"
    :title="dialogType === 'add' ? '新增 HTTP 代理' : '编辑 HTTP 代理'"
    width="900px"
    align-center
  >
    <ElForm ref="formRef" :model="formData" :rules="rules" label-width="140px" :show-message="false">
      <ElFormItem label="客户端" prop="agentId">
        <ElSelect
          v-model="formData.agentId"
          placeholder="请选择客户端"
          :disabled="dialogType === 'edit'"
          style="width: 200px"
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

      <ElFormItem label="部署模式" prop="deployMode">
        <ElRadioGroup v-model="formData.deployMode">
          <ElRadio label="single">单机</ElRadio>
          <ElRadio label="cluster">集群</ElRadio>
        </ElRadioGroup>
      </ElFormItem>

      <ElFormItem v-show="formData.deployMode === 'single'" label="目标服务" :required="true">
        <ElRow :gutter="20">
          <ElCol :span="12">
            <ElInput v-model="formData.singleHost" placeholder="请输入内网地址" />
          </ElCol>
          <ElCol :span="12">
            <ElInput
              v-model.number="formData.singlePort"
              type="number"
              placeholder="请输入内网端口"
            />
          </ElCol>
        </ElRow>
      </ElFormItem>

      <ElFormItem
        v-show="formData.deployMode === 'cluster'"
        label="负载均衡策略"
        prop="loadBalanceStrategy"
        :required="formData.deployMode === 'cluster'"
      >
        <ElSelect
          v-model="formData.loadBalanceStrategy"
          placeholder="请选择负载均衡策略"
          style="width: 220px"
        >
          <ElOption label="轮询 (roundrobin)" value="1" />
          <ElOption label="权重 (weight)" value="2" />
          <ElOption label="随机 (random)" value="3" />
          <ElOption label="最少连接 (leastconn)" value="4" />
        </ElSelect>
      </ElFormItem>

      <ElFormItem v-show="formData.deployMode === 'cluster'" label="服务列表" prop="targets">
        <div class="targets-table">
          <ElTable :data="formData.targets" style="width: 100%">
            <ElTableColumn prop="name" label="服务名称" min-width="150">
              <template #default="scope">
                <ElInput
                  v-model="scope.row.name"
                  placeholder="请输入服务名称"
                  style="width: 100%"
                  clearable
                />
              </template>
            </ElTableColumn>
            <ElTableColumn prop="host" label="主机" min-width="180">
              <template #default="scope">
                <ElInput
                  v-model="scope.row.host"
                  placeholder="请输入主机"
                  style="width: 100%"
                  clearable
                />
              </template>
            </ElTableColumn>
            <ElTableColumn prop="port" label="服务端口" width="100">
              <template #default="scope">
                <ElInput
                  v-model.number="scope.row.port"
                  type="number"
                  placeholder="内网端口"
                  style="width: 100%"
                />
              </template>
            </ElTableColumn>
            <ElTableColumn prop="weight" label="权重" width="100">
              <template #default="scope">
                <ElInput
                  v-model.number="scope.row.weight"
                  type="number"
                  placeholder="负载均衡权重"
                  style="width: 100%"
                />
              </template>
            </ElTableColumn>
            <ElTableColumn label="操作" width="80" align="center">
              <template #default="scope">
                <ElButton type="danger" size="small" @click="removeTarget(scope.$index)"
                  >删除</ElButton
                >
              </template>
            </ElTableColumn>
          </ElTable>
          <ElButton type="primary" size="small" @click="addTarget" style="margin-top: 10px"
            >添加服务</ElButton
          >
        </div>
      </ElFormItem>

      <ElFormItem label="带宽限流">
        <ElRow :gutter="20">
          <ElCol :span="6">
            <ElFormItem label="总带宽" prop="limitTotal" label-width="70px">
              <ElInputNumber
                v-model="formData.limitTotal"
                placeholder="带宽总和"
                :controls="false"
                :min="0"
                :precision="0"
                style="width: 100%"
              />
            </ElFormItem>
          </ElCol>
          <ElCol :span="6">
            <ElFormItem label="入站带宽" prop="limitIn" label-width="70px">
              <ElInputNumber
                v-model="formData.limitIn"
                placeholder="入站带宽"
                :controls="false"
                :min="0"
                :precision="0"
                style="width: 100%"
              />
            </ElFormItem>
          </ElCol>
          <ElCol :span="6">
            <ElFormItem label="出站带宽" prop="limitOut" label-width="70px">
              <ElInputNumber
                v-model="formData.limitOut"
                placeholder="出站带宽"
                :controls="false"
                :min="0"
                :precision="0"
                style="width: 100%"
              />
            </ElFormItem>
          </ElCol>
          <ElCol :span="6">
            <ElFormItem label="单位" prop="bandwidthUnit" label-width="50px">
              <ElSelect v-model="formData.bandwidthUnit" placeholder="单位" style="width: 100%">
                <ElOption label="bps" value="bps" />
                <ElOption label="Kbps" value="Kbps" />
                <ElOption label="Mbps" value="Mbps" />
                <ElOption label="Gbps" value="Gbps" />
              </ElSelect>
            </ElFormItem>
          </ElCol>
        </ElRow>
      </ElFormItem>

      <ElFormItem label="开启状态" prop="status">
        <ElRadioGroup v-model="formData.status">
          <ElRadio label="1">开启</ElRadio>
          <ElRadio label="0">关闭</ElRadio>
        </ElRadioGroup>
      </ElFormItem>

      <ElCollapse accordion>
        <ElCollapseItem name="1">
          <template #title>
            <span>高级配置</span>
          </template>
          <ElFormItem label="TLS加密" prop="encrypt">
            <ElSwitch v-model="formData.encrypt" />
          </ElFormItem>

          <ElFormItem label="传输隧道" prop="tunnelType">
            <ElRadioGroup v-model="formData.tunnelType">
              <ElRadio label="0">共享隧道</ElRadio>
              <ElRadio label="1">独立隧道</ElRadio>
            </ElRadioGroup>
          </ElFormItem>
        </ElCollapseItem>
      </ElCollapse>
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
  import { ref, reactive, watch, nextTick, computed } from 'vue'
  import { ElMessage } from 'element-plus'
  import type { FormInstance, FormRules } from 'element-plus'
  import { DialogType } from '@/types'
  import { fetchGetAgentListAll } from '@/api/agent'
  import {
    fetchCreateHttpProxy,
    fetchUpdateHttpProxy,
    fetchGetHttpProxyById
  } from '@/api/proxy'

  defineOptions({ name: 'HttpDialog' })

  interface Target {
    id?: string
    host: string
    port: number | string
    weight: number
    name: string
  }

  type BandwidthUnit = 'bps' | 'Kbps' | 'Mbps' | 'Gbps'

  interface FormDataState {
    agentId: string
    name: string
    status: string
    domainType: string
    domains: string
    encrypt: boolean
    tunnelType: string
    deployMode: string
    singleHost: string
    singlePort: string | number
    targets: Target[]
    limitTotal: number | undefined
    limitIn: number | undefined
    limitOut: number | undefined
    bandwidthUnit: BandwidthUnit
    loadBalanceStrategy: string
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
      encrypt: boolean
      tunnelType: number
      targets: Target[]
      bandwidth: {
        limitTotal?: number
        limitIn?: number
        limitOut?: number
        uploadLimit?: number
        downloadLimit?: number
      }
      loadBalanceStrategy: number
      transport: {
        encrypt: boolean
        tunnelType: number
      }
      deploymentMode: number
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
  const agents = ref([])

  watch(
    () => props.visible,
    (newVal) => {
      dialogVisible.value = newVal
    }
  )

  const DEFAULT_FORM_DATA: FormDataState = {
    agentId: '',
    name: '',
    status: '1',
    domainType: '0',
    domains: '',
    encrypt: false,
    tunnelType: '0',
    deployMode: 'single',
    singleHost: '127.0.0.1',
    singlePort: '',
    targets: [] as Target[],
    limitTotal: undefined as number | undefined,
    limitIn: undefined as number | undefined,
    limitOut: undefined as number | undefined,
    bandwidthUnit: 'Mbps',
    loadBalanceStrategy: '1'
  }

  const formData = reactive<FormDataState>({ ...DEFAULT_FORM_DATA })
  const isInitializingBandwidth = ref(false)

  const UNIT_FACTORS: Record<BandwidthUnit, number> = {
    bps: 1,
    Kbps: 1000,
    Mbps: 1000 * 1000,
    Gbps: 1000 * 1000 * 1000
  }

  const rules: FormRules = {
    agentId: [{ required: true, message: '请选择客户端', trigger: 'change' }],
    name: [{ required: true, message: '请输入代理名称', trigger: 'blur' }],
    status: [{ required: true, message: '请选择状态', trigger: 'change' }],
    domainType: [{ required: true, message: '请选择域名类型', trigger: 'change' }],
    domains: [
      {
        validator: (rule: any, value: string, callback: any) => {
          if (formData.domainType !== '0' && (!value || !value.trim())) {
            callback(new Error('请输入域名'))
          } else {
            callback()
          }
        },
        trigger: 'blur'
      }
    ],
    encrypt: [{ required: true, message: '请选择是否开启TLS加密', trigger: 'change' }],
    tunnelType: [{ required: true, message: '请选择隧道类型', trigger: 'change' }],
    deployMode: [{ required: true, message: '请选择部署模式', trigger: 'change' }],
    singleHost: [
      { required: true, message: '请输入主机', trigger: 'blur' },
      { required: formData.deployMode === 'single', message: '请输入主机', trigger: 'blur' }
    ],
    singlePort: [
      { required: true, message: '请输入端口', trigger: 'blur' },
      { required: formData.deployMode === 'single', message: '请输入端口', trigger: 'blur' },
      { type: 'number', message: '端口必须是数字', trigger: 'blur' },
      { min: 1, max: 65535, message: '端口必须在 1-65535 之间', trigger: 'blur' }
    ],
    targets: [
      {
        validator: (rule: any, value: Target[], callback: any) => {
          if (formData.deployMode === 'cluster' && value.length === 0) {
            callback(new Error('请至少添加一个服务'))
          } else {
            callback()
          }
        },
        trigger: 'change'
      }
    ],
    loadBalanceStrategy: [
      {
        validator: (rule: any, value: string, callback: any) => {
          if (formData.deployMode === 'cluster' && formData.targets.length >= 2 && !value) {
            callback(new Error('服务列表数量在2个及以上时，必须选择负载均衡策略'))
          } else {
            callback()
          }
        },
        trigger: 'change'
      }
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
    Object.assign(formData, { ...DEFAULT_FORM_DATA, targets: [] })
  }

  const findBestCommonUnit = (
    limitTotalBps: number | undefined,
    limitInBps: number | undefined,
    limitOutBps: number | undefined
  ): string => {
    const values = [limitTotalBps, limitInBps, limitOutBps].filter((v): v is number => v != null)
    if (values.length === 0) return 'Mbps'

    const maxValue = Math.max(...values)
    if (maxValue >= 1000 * 1000 * 1000) return 'Gbps'
    if (maxValue >= 1000 * 1000) return 'Mbps'
    if (maxValue >= 1000) return 'Kbps'
    return 'bps'
  }

  const convertBandwidthToUnit = (
    bps: number | undefined,
    targetUnit: string
  ): number | undefined => {
    if (bps == null) return undefined
    return Math.round(bps / UNIT_FACTORS[targetUnit as BandwidthUnit])
  }

  const convertBandwidth = (
    value: number | undefined,
    oldUnit: string,
    newUnit: string
  ): number | undefined => {
    if (value == null) return undefined
    const result =
      (value * UNIT_FACTORS[oldUnit as BandwidthUnit]) / UNIT_FACTORS[newUnit as BandwidthUnit]
    return Math.round(result)
  }

  const getDisplayBandwidthFromBps = (
    limitTotalBps: number | undefined,
    limitInBps: number | undefined,
    limitOutBps: number | undefined
  ) => {
    const targetUnit = findBestCommonUnit(limitTotalBps, limitInBps, limitOutBps)
    return {
      bandwidthUnit: targetUnit,
      limitTotal: convertBandwidthToUnit(limitTotalBps, targetUnit),
      limitIn: convertBandwidthToUnit(limitInBps, targetUnit),
      limitOut: convertBandwidthToUnit(limitOutBps, targetUnit)
    }
  }

  const mapTarget = (target: any): Target => ({
    id: target?.id || '',
    host: target?.host || target?.targetHost || '',
    port: target?.port || target?.targetPort || '',
    weight: target?.weight || 1,
    name: target?.name || ''
  })

  const applyBandwidthDisplayData = async (bandwidthData: {
    limitTotal: number | undefined
    limitIn: number | undefined
    limitOut: number | undefined
    bandwidthUnit: string
  }) => {
    isInitializingBandwidth.value = true
    try {
      formData.limitTotal = bandwidthData.limitTotal
      formData.limitIn = bandwidthData.limitIn
      formData.limitOut = bandwidthData.limitOut
      formData.bandwidthUnit = bandwidthData.bandwidthUnit as BandwidthUnit
      await nextTick()
    } finally {
      isInitializingBandwidth.value = false
    }
  }

  const addTarget = () => {
    formData.targets.push({ host: '127.0.0.1', port: '', weight: 1, name: '' })
  }

  const removeTarget = (index: number) => {
    formData.targets.splice(index, 1)
  }

  const getRawBandwidth = (bandwidth: any) => ({
    limitTotal: bandwidth?.limitTotal ?? bandwidth?.uploadLimit,
    limitIn: bandwidth?.limitIn ?? bandwidth?.downloadLimit,
    limitOut: bandwidth?.limitOut
  })

  const initFormData = async () => {
    const isEdit = props.type === 'edit' && props.proxyData && props.proxyData.id

    if (isEdit) {
      try {
        const proxyDetail = await fetchGetHttpProxyById(props.proxyData!.id!)
        const rawBandwidth = getRawBandwidth(proxyDetail.bandwidth)
        const bandwidthDisplayData = getDisplayBandwidthFromBps(
          rawBandwidth.limitTotal,
          rawBandwidth.limitIn,
          rawBandwidth.limitOut
        )
        const detailTargets = proxyDetail.targets?.map(mapTarget) || []
        Object.assign(formData, {
          ...DEFAULT_FORM_DATA,
          agentId: proxyDetail.agentId || '',
          name: proxyDetail.name || '',
          status: proxyDetail.status?.toString() || '1',
          domainType: proxyDetail.domainType?.toString() || '0',
          domains: (proxyDetail.domains || []).join('\n'),
          encrypt: proxyDetail.transport?.encrypt || false,
          tunnelType: proxyDetail.transport?.tunnelType?.toString() || '1',
          deployMode: proxyDetail.deploymentMode === 1 ? 'single' : 'cluster',
          singleHost: detailTargets.length > 0 ? detailTargets[0].host || '127.0.0.1' : '127.0.0.1',
          singlePort: detailTargets.length > 0 ? detailTargets[0].port || '' : '',
          targets: detailTargets,
          loadBalanceStrategy: proxyDetail.loadBalance?.strategy?.toString() || '1'
        })
        await applyBandwidthDisplayData(bandwidthDisplayData)
      } catch (error) {
        console.error('获取代理详情失败:', error)
        ElMessage.error('获取代理详情失败，请稍后重试')
        const row = props.proxyData
        const rawBandwidth = getRawBandwidth(row?.bandwidth)
        const bandwidthDisplayData = getDisplayBandwidthFromBps(
          rawBandwidth.limitTotal,
          rawBandwidth.limitIn,
          rawBandwidth.limitOut
        )
        Object.assign(formData, {
          ...DEFAULT_FORM_DATA,
          agentId: row ? row.agentId || '' : '',
          name: row ? row.name || '' : '',
          status: row ? row.status?.toString() || '1' : '1',
          domainType: row ? row.domainType?.toString() || '0' : '0',
          domains: row?.domains ? row.domains.join('\n') : '',
          encrypt: row ? row.encrypt || false : false,
          tunnelType: row ? row.tunnelType?.toString() || '1' : '1',
          deployMode: 'single',
          targets: row?.targets ? row.targets.map(mapTarget) : [],
          loadBalanceStrategy: '1'
        })
        await applyBandwidthDisplayData(bandwidthDisplayData)
      }
    } else {
      resetFormData()
    }

    if (formData.targets.length === 0) {
      addTarget()
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
        nextTick(() => {
          formRef.value?.clearValidate()
        })
      }
    },
    { immediate: true }
  )

  watch(
    () => formData.targets.length,
    () => {
      formRef.value?.validateField('loadBalanceStrategy')
    }
  )

  watch(
    () => formData.deployMode,
    () => {
      formRef.value?.clearValidate()
      formRef.value?.validateField('singleHost')
      formRef.value?.validateField('singlePort')
      formRef.value?.validateField('targets')
      formRef.value?.validateField('loadBalanceStrategy')
    }
  )

  watch(
    () => formData.bandwidthUnit,
    (newUnit, oldUnit) => {
      if (isInitializingBandwidth.value) return
      if (!oldUnit || !newUnit || oldUnit === newUnit) return

      if (formData.limitTotal != null) {
        formData.limitTotal = convertBandwidth(formData.limitTotal, oldUnit, newUnit)
      }
      if (formData.limitIn != null) {
        formData.limitIn = convertBandwidth(formData.limitIn, oldUnit, newUnit)
      }
      if (formData.limitOut != null) {
        formData.limitOut = convertBandwidth(formData.limitOut, oldUnit, newUnit)
      }
    }
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
          const domainsArray =
            formData.domainType === '0'
              ? []
              : formData.domains.split('\n').map((v) => v.trim()).filter(Boolean)

          const targets =
            formData.deployMode === 'single'
              ? [
                  {
                    name: formData.name,
                    host: formData.singleHost,
                    port: formData.singlePort,
                    weight: 1
                  }
                ]
              : formData.targets

          const commonData = {
            name: formData.name,
            status: parseInt(formData.status),
            domainType: parseInt(formData.domainType),
            domains: domainsArray,
            deploymentMode: formData.deployMode === 'single' ? 1 : 0,
            targets: targets,
            bandwidth: {
              // 后端会基于 unit 转换到 bps，这里应传用户输入值，避免重复换算
              limitTotal: formData.limitTotal ?? null,
              limitIn: formData.limitIn ?? null,
              limitOut: formData.limitOut ?? null,
              unit: formData.bandwidthUnit
            },
            loadBalance:
              formData.deployMode === 'cluster'
                ? {
                    strategy: parseInt(formData.loadBalanceStrategy)
                  }
                : undefined,
            transport: {
              tunnelType: parseInt(formData.tunnelType),
              encrypt: formData.encrypt
            }
          }

          if (dialogType.value === 'add') {
            const requestData = {
              agentId: formData.agentId,
              ...commonData
            }
            await fetchCreateHttpProxy(requestData)
          } else {
            const requestData = {
              id: props.proxyData?.id,
              ...commonData
            }
            await fetchUpdateHttpProxy(requestData)
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

<style scoped>
  .targets-table {
    border: 1px solid #e4e7ed;
    border-radius: 4px;
    padding: 10px;
  }

  :deep(.el-collapse) {
    border: none;
  }

  :deep(.el-collapse-item__header) {
    border-bottom: none;
  }

  :deep(.el-collapse-item__wrap) {
    border-top: none;
  }
</style>
