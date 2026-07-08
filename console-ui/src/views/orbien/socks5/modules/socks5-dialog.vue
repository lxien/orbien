<template>
  <ElDialog
      v-model="dialogVisible"
      :title="dialogType === 'add' ? '添加 SOCKS5 代理' : '编辑 SOCKS5 代理'"
      width="720px"
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
          <ElOption v-for="agent in agents" :key="agent.id" :label="agent.name" :value="agent.id"/>
        </ElSelect>
      </ElFormItem>

      <ElFormItem label="代理名称" prop="name">
        <ElInput v-model="formData.name" placeholder="请输入代理名称" clearable/>
      </ElFormItem>

      <ElFormItem label="远程端口" prop="remotePort">
        <div class="remote-port-field">
          <ElInput
              v-model="remotePortInput"
              type="number"
              placeholder="不填自动生成"
              class="remote-port-input"
              @input="selectedSuggestPort = null"
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
          <ElButton link type="primary" :loading="suggestLoading" @click="loadSuggestedPorts()">
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

      <ElFormItem label="启用认证">
        <div class="auth-switch-row">
          <ElSwitch v-model="formData.authEnabled"/>
          <span class="auth-switch-tip">强烈建议开启，防止未经授权的用户访问您的内网</span>
        </div>
      </ElFormItem>

      <template v-if="formData.authEnabled">
        <ElFormItem label="认证用户" required>
          <div class="auth-users-panel">
            <ElTable :data="authUsers" border size="small" empty-text="请至少添加一个认证用户">
              <ElTableColumn prop="username" label="用户名" min-width="160">
                <template #default="{ row }">
                  <ElInput v-model="row.username" size="small" placeholder="用户名" clearable/>
                </template>
              </ElTableColumn>
              <ElTableColumn prop="password" label="密码" min-width="160">
                <template #default="{ row }">
                  <ElInput
                      v-model="row.password"
                      size="small"
                      :placeholder="row.id ? '留空则不修改' : '请输入密码'"
                      type="password"
                      show-password
                      clearable
                  />
                </template>
              </ElTableColumn>
              <ElTableColumn label="操作" width="80" fixed="right">
                <template #default="{ $index }">
                  <ElButton
                      link
                      type="primary"
                      size="small"
                      :disabled="authUsers.length <= 1"
                      @click="authUsers.splice($index, 1)"
                  >
                    删除
                  </ElButton>
                </template>
              </ElTableColumn>
            </ElTable>
            <ElButton type="primary" plain size="small" class="add-user-btn" @click="authUsers.push(emptyAuthUser())">
              添加用户
            </ElButton>
          </div>
        </ElFormItem>
      </template>
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
import {ref, reactive, watch, computed} from 'vue'
import {ElMessage} from 'element-plus'
import type {FormInstance, FormRules} from 'element-plus'
import {DialogType} from '@/types'
import {fetchGetAgentListAll} from '@/api/agent'
import {fetchCreateSocks5Proxy, fetchUpdateSocks5Proxy, fetchGetSocks5ProxyById} from '@/api/proxy'
import {fetchSuggestAvailablePorts} from '@/api/port-pool'
import {PortPoolType} from '@/enums/orbien/business'

defineOptions({name: 'Socks5Dialog'})

type AuthUserForm = Api.Proxy.Socks5AuthUserParam & { id?: number }

interface FormDataState {
  agentId: string
  name: string
  limitTotal: number
  authEnabled: boolean
}

interface Props {
  visible: boolean
  type: DialogType
  proxyData?: Partial<{ id: string; agentId: string; name: string }>
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
const authUsers = ref<AuthUserForm[]>([])

let openSession = 0

const DEFAULT_FORM: FormDataState = {
  agentId: '',
  name: '',
  limitTotal: 1,
  authEnabled: false
}

const formData = reactive<FormDataState>({...DEFAULT_FORM})

const isStaleSession = (session: number) => session !== openSession || !props.visible

const emptyAuthUser = (): AuthUserForm => ({username: '', password: ''})

const rules = computed<FormRules>(() => ({
  agentId: [{required: true, message: '请选择客户端', trigger: 'change'}],
  name: [{required: true, message: '请输入代理名称', trigger: 'blur'}],
  remotePort: [{
    validator: (_rule, _value, callback) => {
      if (!remotePortInput.value) return callback()
      const port = parseInt(remotePortInput.value, 10)
      if (isNaN(port) || port < 1 || port > 65535) {
        callback(new Error('远程端口必须在 1-65535 之间'))
      } else {
        callback()
      }
    },
    trigger: 'blur'
  }],
  limitTotal: [
    {required: true, message: '请输入带宽限制', trigger: 'blur'},
    {type: 'number', min: 1, message: '带宽必须大于 0', trigger: 'blur'}
  ]
}))

const resetForm = () => {
  Object.assign(formData, {...DEFAULT_FORM})
  remotePortInput.value = ''
  authUsers.value = []
  suggestedPorts.value = []
  selectedSuggestPort.value = null
}

const ensureAuthUsers = () => {
  if (formData.authEnabled && authUsers.value.length === 0) {
    authUsers.value = [emptyAuthUser()]
  }
}

const validateAuthUsers = (): boolean => {
  if (!formData.authEnabled) return true

  const users = authUsers.value.filter((user) => user.username?.trim())
  if (users.length === 0) {
    ElMessage.warning('启用认证时请至少添加一个用户')
    return false
  }

  for (const user of users) {
    const needPassword = !user.id
    if (needPassword && !user.password?.trim()) {
      ElMessage.warning('请填写用户名和密码')
      return false
    }
  }
  return true
}

const buildAuthPayload = (): Api.Proxy.Socks5AuthUserParam[] => {
  if (!formData.authEnabled) return []
  return authUsers.value
      .filter((user) => user.username?.trim())
      .map((user) => ({
        ...(user.id ? {id: user.id} : {}),
        username: user.username.trim(),
        ...(user.password?.trim() ? {password: user.password.trim()} : {})
      }))
}

const parseRemotePort = (): number | undefined => {
  if (!remotePortInput.value) return undefined
  const port = parseInt(remotePortInput.value, 10)
  return isNaN(port) ? undefined : port
}

const selectSuggestedPort = (port: number) => {
  remotePortInput.value = String(port)
  selectedSuggestPort.value = port
  formRef.value?.clearValidate('remotePort')
}

const loadSuggestedPorts = async (session = openSession) => {
  suggestLoading.value = true
  selectedSuggestPort.value = null
  try {
    const ports = await fetchSuggestAvailablePorts(PortPoolType.TCP, 4)
    if (!isStaleSession(session)) {
      suggestedPorts.value = ports
    }
  } catch (error) {
    console.error('获取可用端口失败:', error)
    if (!isStaleSession(session)) {
      suggestedPorts.value = []
    }
  } finally {
    if (!isStaleSession(session)) {
      suggestLoading.value = false
    }
  }
}

const loadEditForm = async (session: number, proxyId: string) => {
  try {
    const detail = await fetchGetSocks5ProxyById(proxyId)
    if (isStaleSession(session) || props.type !== 'edit' || props.proxyData?.id !== proxyId) return

    Object.assign(formData, {
      agentId: detail.agentId || '',
      name: detail.name || '',
      limitTotal: detail.limitTotal ?? 1,
      authEnabled: detail.authEnabled ?? false
    })
    const displayPort = detail.remotePort ?? detail.listenPort
    remotePortInput.value = displayPort != null ? String(displayPort) : ''
    authUsers.value = (detail.authUsers || []).map((user) => ({
      id: user.id,
      username: user.username,
      password: ''
    }))
    ensureAuthUsers()
  } catch (error) {
    console.error('获取代理详情失败:', error)
    if (!isStaleSession(session)) {
      ElMessage.error('获取代理详情失败，请稍后重试')
    }
  }
}

const openDialog = async () => {
  const session = ++openSession
  resetForm()
  formRef.value?.clearValidate()

  try {
    agents.value = await fetchGetAgentListAll() || []
  } catch (error) {
    console.error('获取客户端列表失败:', error)
    ElMessage.error('获取客户端列表失败')
  }
  if (isStaleSession(session)) return

  if (props.type === 'edit' && props.proxyData?.id) {
    await loadEditForm(session, props.proxyData.id)
  } else if (!formData.agentId && agents.value.length > 0) {
    formData.agentId = agents.value[0].id
  }

  if (!isStaleSession(session)) {
    await loadSuggestedPorts(session)
  }
}

watch(() => formData.authEnabled, ensureAuthUsers)

watch(
    () => [props.visible, props.type, props.proxyData?.id] as const,
    ([visible]) => {
      if (visible) {
        openDialog()
      } else {
        openSession++
        resetForm()
        formRef.value?.clearValidate()
      }
    }
)

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid || !validateAuthUsers()) return

    try {
      const remotePort = parseRemotePort()
      const payload: Omit<Api.Proxy.Socks5ProxyUpdateParam, 'id'> = {
        name: formData.name,
        limitTotal: formData.limitTotal,
        authEnabled: formData.authEnabled,
        authUsers: buildAuthPayload(),
        ...(remotePort != null ? {remotePort} : {})
      }

      if (dialogType.value === 'add') {
        await fetchCreateSocks5Proxy({agentId: formData.agentId, ...payload})
      } else {
        await fetchUpdateSocks5Proxy({id: props.proxyData?.id || '', ...payload})
      }

      dialogVisible.value = false
      emit('submit')
    } catch (error) {
      console.error('提交失败:', error)
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

.auth-switch-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.auth-switch-tip {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.auth-users-panel {
  width: 100%;
}

.add-user-btn {
  margin-top: 12px;
}
</style>
