<template>
  <ElDialog
      v-model="dialogVisible"
      :title="dialogType === 'add' ? '添加文件共享' : '编辑文件共享'"
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

      <ElFormItem label="共享名称" prop="name">
        <ElInput v-model="formData.name" placeholder="请输入共享名称" clearable/>
      </ElFormItem>

      <ElFormItem label="根目录" prop="rootPath">
        <ElInput v-model="formData.rootPath" placeholder="如 /data/share" clearable/>
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
            placeholder="请输入完整域名，多个用换行分隔，如 files.example.com"
        />
      </ElFormItem>

      <ElFormItem label="启用认证">
        <div class="auth-switch-row">
          <ElSwitch v-model="formData.authEnabled"/>
          <span class="auth-switch-tip">开启后访问文件共享需要用户名和密码</span>
        </div>
      </ElFormItem>

      <template v-if="formData.authEnabled">
        <ElFormItem label="认证用户" required>
          <div class="auth-users-panel">
            <ElTable :data="authUsers" border size="small" empty-text="请至少添加一个认证用户">
              <ElTableColumn prop="username" label="用户名" min-width="140">
                <template #default="{ row }">
                  <ElInput v-model="row.username" size="small" placeholder="用户名" clearable/>
                </template>
              </ElTableColumn>
              <ElTableColumn prop="password" label="密码" min-width="140">
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
              <ElTableColumn prop="permission" label="权限" width="120">
                <template #default="{ row }">
                  <ElSelect v-model="row.permission" size="small" style="width: 100%">
                    <ElOption
                        v-for="option in permissionOptions"
                        :key="option.value"
                        :label="option.label"
                        :value="option.value"
                    />
                  </ElSelect>
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

      <ElFormItem label="上传限制" prop="maxUploadSizeMb">
        <ElInput
            v-model.number="formData.maxUploadSizeMb"
            type="number"
            :min="1"
            placeholder="单文件最大上传大小"
            style="width: 200px"
        >
          <template #append>MB</template>
        </ElInput>
      </ElFormItem>

      <ElFormItem label="操作权限">
        <ElSpace wrap>
          <ElCheckbox v-model="formData.allowUpload">允许上传</ElCheckbox>
          <ElCheckbox v-model="formData.allowDelete">允许删除</ElCheckbox>
          <ElCheckbox v-model="formData.allowMkdir">允许创建目录</ElCheckbox>
        </ElSpace>
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
import {ref, reactive, watch, computed} from 'vue'
import {ElMessage} from 'element-plus'
import type {FormInstance, FormRules} from 'element-plus'
import {DialogType} from '@/types'
import {fetchGetAgentListAll} from '@/api/agent'
import {fetchCreateFileShare, fetchUpdateFileShare, fetchGetFileShareById} from '@/api/file-share'
import {DomainType} from '@/enums/orbien/business'
import {
  useRootDomainOptions,
  validateSubdomainBindings,
  buildSubdomainBindingsPayload
} from '@/views/orbien/proxy/shared/use-root-domain-options'
import SubdomainBindingRows from '@/views/orbien/proxy/shared/subdomain-binding-rows.vue'

defineOptions({name: 'FileShareDialog'})

const BYTES_PER_MB = 1024 * 1024
const DEFAULT_MAX_UPLOAD_MB = 500

type AuthUserForm = Api.FileShare.FileShareAuthUserParam & { id?: number }

interface FormDataState {
  agentId: string
  name: string
  rootPath: string
  domainType: string
  subdomainBindings: Api.Proxy.SubdomainBindingParam[]
  customDomains: string
  authEnabled: boolean
  maxUploadSizeMb: number
  allowUpload: boolean
  allowDelete: boolean
  allowMkdir: boolean
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

const permissionOptions = [
  {label: '只读', value: 'read'},
  {label: '读写', value: 'read_write'}
] as const

const dialogVisible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value)
})

const dialogType = computed(() => props.type)
const formRef = ref<FormInstance>()
const agents = ref<Api.Agent.AgentDTO[]>([])
const authUsers = ref<AuthUserForm[]>([])
const subdomainErrorIndexes = ref<number[]>([])

const {
  rootDomains,
  rootDomainLoading,
  loadRootDomains,
  createDefaultSubdomainBinding,
  normalizeSubdomainBindings
} = useRootDomainOptions()

let openSession = 0

const createDefaultFormData = (): FormDataState => ({
  agentId: '',
  name: '',
  rootPath: '',
  domainType: String(DomainType.AUTO),
  subdomainBindings: [createDefaultSubdomainBinding()],
  customDomains: '',
  authEnabled: false,
  maxUploadSizeMb: DEFAULT_MAX_UPLOAD_MB,
  allowUpload: true,
  allowDelete: true,
  allowMkdir: true
})

const formData = reactive<FormDataState>(createDefaultFormData())

const isStaleSession = (session: number) => session !== openSession || !props.visible

const emptyAuthUser = (): AuthUserForm => ({username: '', password: '', permission: 'read'})

const rules = computed<FormRules>(() => ({
  agentId: [{required: true, message: '请选择客户端', trigger: 'change'}],
  name: [{required: true, message: '请输入共享名称', trigger: 'blur'}],
  rootPath: [{required: true, message: '请输入根目录', trigger: 'blur'}],
  domainType: [{required: true, message: '请选择域名类型', trigger: 'change'}],
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
  maxUploadSizeMb: [
    {required: true, message: '请输入上传限制', trigger: 'blur'},
    {type: 'number', min: 1, message: '上传限制必须大于 0', trigger: 'blur'}
  ]
}))

const resetSubdomainErrors = () => {
  subdomainErrorIndexes.value = []
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

const resetForm = () => {
  Object.assign(formData, createDefaultFormData())
  authUsers.value = []
  subdomainErrorIndexes.value = []
  refreshSubdomainBindings()
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

const buildAuthPayload = (): Api.FileShare.FileShareAuthUserParam[] => {
  if (!formData.authEnabled) return []
  return authUsers.value
      .filter((user) => user.username?.trim())
      .map((user) => ({
        ...(user.id ? {id: user.id} : {}),
        username: user.username.trim(),
        permission: user.permission || 'read',
        ...(user.password?.trim() ? {password: user.password.trim()} : {})
      }))
}

const buildDomainPayload = () => {
  const domainType = parseInt(formData.domainType, 10)
  if (domainType === DomainType.SUBDOMAIN) {
    return {
      subdomainBindings: buildSubdomainBindingsPayload(formData.subdomainBindings, rootDomains.value)
    }
  }
  if (domainType === DomainType.CUSTOM_DOMAIN) {
    return {customDomains: parseLines(formData.customDomains)}
  }
  return {}
}

const bytesToMb = (bytes?: number): number => {
  if (bytes == null || bytes <= 0) {
    return DEFAULT_MAX_UPLOAD_MB
  }
  return Math.round(bytes / BYTES_PER_MB)
}

const mbToBytes = (mb: number): number => Math.round(mb * BYTES_PER_MB)

const applyDetail = (detail: Api.FileShare.FileShareDetailDTO) => {
  Object.assign(formData, {
    ...createDefaultFormData(),
    agentId: detail.agentId || '',
    name: detail.name || '',
    rootPath: detail.rootPath || '',
    domainType: detail.domainType?.toString() || String(DomainType.AUTO),
    subdomainBindings: normalizeSubdomainBindings(detail.subdomainBindings),
    customDomains: (detail.customDomains || []).join('\n'),
    authEnabled: detail.authEnabled ?? false,
    maxUploadSizeMb: bytesToMb(detail.maxUploadSize),
    allowUpload: detail.allowUpload ?? true,
    allowDelete: detail.allowDelete ?? true,
    allowMkdir: detail.allowMkdir ?? true
  })
  authUsers.value = (detail.authUsers || []).map((user) => ({
    id: user.id,
    username: user.username,
    password: '',
    permission: user.permission || 'read'
  }))
  ensureAuthUsers()
  refreshSubdomainBindings()
}

const loadEditForm = async (session: number, proxyId: string) => {
  try {
    const detail = await fetchGetFileShareById(proxyId)
    if (isStaleSession(session) || props.type !== 'edit' || props.proxyData?.id !== proxyId) return
    applyDetail(detail)
  } catch (error) {
    console.error('获取文件共享详情失败:', error)
    if (!isStaleSession(session)) {
      ElMessage.error('获取文件共享详情失败，请稍后重试')
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

  try {
    await loadRootDomains()
  } catch (error) {
    console.error('获取根域名列表失败:', error)
  }
  if (isStaleSession(session)) return

  if (props.type === 'edit' && props.proxyData?.id) {
    await loadEditForm(session, props.proxyData.id)
  } else if (!formData.agentId && agents.value.length > 0) {
    formData.agentId = agents.value[0].id
  }

  refreshSubdomainBindings()
}

watch(() => formData.authEnabled, ensureAuthUsers)

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
          ? validateSubdomainBindings(formData.subdomainBindings, rootDomains.value)
          : {valid: true, errorIndexes: [] as number[]}

  if (!subdomainResult.valid) {
    subdomainErrorIndexes.value = subdomainResult.errorIndexes
    if (subdomainResult.message) {
      ElMessage.warning(subdomainResult.message)
    }
  }

  if (!formValid || !subdomainResult.valid || !validateAuthUsers()) return

  try {
    const payload: Omit<Api.FileShare.FileShareUpdateParam, 'id'> = {
      name: formData.name,
      domainType: parseInt(formData.domainType, 10),
      rootPath: formData.rootPath.trim(),
      limitTotal: 1,
      authEnabled: formData.authEnabled,
      authUsers: buildAuthPayload(),
      maxUploadSize: mbToBytes(formData.maxUploadSizeMb),
      allowUpload: formData.allowUpload,
      allowDelete: formData.allowDelete,
      allowMkdir: formData.allowMkdir,
      ...buildDomainPayload()
    }

    if (dialogType.value === 'add') {
      await fetchCreateFileShare({agentId: formData.agentId, ...payload})
    } else {
      await fetchUpdateFileShare({id: props.proxyData?.id || '', ...payload})
    }

    dialogVisible.value = false
    emit('submit')
  } catch (error) {
    console.error('提交失败:', error)
  }
}
</script>

<style scoped lang="scss">
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
