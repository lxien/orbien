<template>
  <ElDialog
    v-model="dialogVisible"
    title="申请免费 TLS 证书"
    width="720px"
    align-center
    destroy-on-close
    :close-on-click-modal="false"
  >
    <ElSteps :active="currentStep" finish-status="success" align-center class="wizard-steps">
      <ElStep title="填写域名" />
      <ElStep title="验证方式" />
      <ElStep title="DNS 验证" />
    </ElSteps>

    <div class="wizard-body">
      <div v-show="currentStep === 0">
        <ElAlert
          type="info"
          :closable="false"
          show-icon
          title="支持单域名与通配符（如 *.example.com），每行一个域名"
          class="mb-4"
        />
        <ElInput
          v-model="domainText"
          type="textarea"
          :rows="8"
          placeholder="example.com&#10;*.example.com"
        />
      </div>

      <div v-show="currentStep === 1">
        <ElForm label-width="120px">
          <ElFormItem label="验证方式">
            <ElRadioGroup v-model="validationMode">
              <ElRadio :value="1">手动添加 DNS TXT</ElRadio>
              <ElRadio :value="2">云 DNS 自动解析</ElRadio>
            </ElRadioGroup>
          </ElFormItem>
          <ElFormItem v-if="validationMode === 2" label="DNS 密钥">
            <ElSelect v-model="dnsCredentialId" placeholder="请选择 DNS 密钥" style="width: 100%">
              <ElOption
                v-for="item in credentialList"
                :key="item.id"
                :label="`${item.name}（${item.providerLabel}）`"
                :value="item.id"
              />
            </ElSelect>
            <div v-if="!credentialList.length" class="form-tip">
              暂无可用密钥，请先在「DNS 密钥」页签中添加
            </div>
          </ElFormItem>
        </ElForm>
      </div>

      <div v-show="currentStep === 2">
        <ElAlert
          v-if="validationMode === 1"
          type="warning"
          :closable="false"
          show-icon
          title="请在 DNS 服务商处添加以下 TXT 记录，生效后点击「开始验证」"
          class="mb-4"
        />
        <ElAlert
          v-else
          type="success"
          :closable="false"
          show-icon
          title="系统已自动添加 TXT 记录，正在等待 DNS 生效并验证"
          class="mb-4"
        />

        <div v-if="orderResult?.challenges?.length">
          <div v-for="item in orderResult.challenges" :key="item.id" class="challenge-card">
            <div class="challenge-domain">{{ item.domain }}</div>
            <div v-if="item.dnsZone" class="field-row zone-tip">DNS 区域：{{ item.dnsZone }}</div>
            <div class="field-row">
              <span>主机记录</span>
              <code>{{ item.hostRecord || item.recordName }}</code>
              <ElButton link type="primary" @click="copyText(item.hostRecord || item.recordName)">复制</ElButton>
            </div>
            <div class="field-row">
              <span>记录类型</span>
              <code>TXT</code>
            </div>
            <div class="field-row">
              <span>记录值</span>
              <code class="record-value">{{ item.recordValue }}</code>
              <ElButton link type="primary" @click="copyText(item.recordValue)">复制</ElButton>
            </div>
          </div>
        </div>

        <div v-if="orderResult" class="order-status">
          当前状态：<ElTag size="small" :type="resolveAcmeOrderStatusTagType(orderResult.status)">{{ orderResult.statusLabel }}</ElTag>
        </div>
        <div v-if="orderResult?.errorMessage" class="error-text">{{ orderResult.errorMessage }}</div>
      </div>
    </div>

    <template #footer>
      <ElButton v-if="currentStep > 0 && currentStep < 2" @click="currentStep--">上一步</ElButton>
      <ElButton @click="dialogVisible = false">关闭</ElButton>
      <ElButton v-if="currentStep < 1" type="primary" @click="handleNext">下一步</ElButton>
      <ElButton
        v-else-if="currentStep === 1"
        type="primary"
        :loading="submitting"
        @click="handleSubmit"
      >
        提交申请
      </ElButton>
      <ElButton
        v-else-if="validationMode === 1 && canManualVerify"
        type="primary"
        :loading="verifying"
        @click="handleVerify"
      >
        开始验证
      </ElButton>
      <ElButton v-else-if="currentStep === 2" :loading="refreshing" @click="refreshOrder">刷新状态</ElButton>
    </template>
  </ElDialog>
</template>

<script setup lang="ts">
  import { computed, ref, watch } from 'vue'
  import { ElMessage } from 'element-plus'
  import { fetchDnsCredentialList } from '@/api/dns-credential'
  import {
    fetchAcmeOrderDetail,
    fetchCreateAcmeOrder,
    fetchVerifyAcmeOrder
  } from '@/api/acme-order'
  import { resolveAcmeOrderStatusTagType } from '@/utils/ui/status-tag'

  defineOptions({ name: 'AcmeApplyWizard' })

  interface Props {
    visible: boolean
  }

  interface Emits {
    (e: 'update:visible', value: boolean): void
    (e: 'success'): void
  }

  const props = defineProps<Props>()
  const emit = defineEmits<Emits>()

  const dialogVisible = computed({
    get: () => props.visible,
    set: (value) => emit('update:visible', value)
  })

  const currentStep = ref(0)
  const domainText = ref('')
  const validationMode = ref(1)
  const dnsCredentialId = ref<number>()
  const credentialList = ref<Api.DnsCredential.CredentialDTO[]>([])
  const submitting = ref(false)
  const verifying = ref(false)
  const refreshing = ref(false)
  const orderResult = ref<Api.AcmeOrder.OrderDTO | null>(null)

  const canManualVerify = computed(
    () => orderResult.value && [1, 2, 6].includes(orderResult.value.status)
  )

  const parseDomains = () =>
    domainText.value
      .split(/[\n,;]+/)
      .map((item) => item.trim())
      .filter(Boolean)

  const loadCredentials = async () => {
    credentialList.value = await fetchDnsCredentialList()
  }

  const resetWizard = () => {
    currentStep.value = 0
    domainText.value = ''
    validationMode.value = 1
    dnsCredentialId.value = undefined
    orderResult.value = null
  }

  watch(
    () => props.visible,
    (visible) => {
      if (visible) {
        resetWizard()
        loadCredentials()
      }
    }
  )

  const handleNext = () => {
    const domains = parseDomains()
    if (!domains.length) {
      ElMessage.warning('请至少填写一个域名')
      return
    }
    currentStep.value++
  }

  const handleSubmit = async () => {
    const domains = parseDomains()
    if (!domains.length) {
      ElMessage.warning('请至少填写一个域名')
      return
    }
    if (validationMode.value === 2 && !dnsCredentialId.value) {
      ElMessage.warning('请选择 DNS 密钥')
      return
    }
    submitting.value = true
    try {
      orderResult.value = await fetchCreateAcmeOrder({
        domains,
        validationMode: validationMode.value,
        dnsCredentialId: dnsCredentialId.value
      })
      currentStep.value = 2
      if (orderResult.value.status === 5) {
        ElMessage.success('证书申请成功')
        emit('success')
      } else if (validationMode.value === 2) {
        ElMessage.info('已提交，系统正在自动验证')
      }
    } finally {
      submitting.value = false
    }
  }

  const refreshOrder = async () => {
    if (!orderResult.value?.id) return
    refreshing.value = true
    try {
      orderResult.value = await fetchAcmeOrderDetail(orderResult.value.id)
      if (orderResult.value.status === 5) {
        ElMessage.success('证书申请成功')
        emit('success')
      }
    } finally {
      refreshing.value = false
    }
  }

  const handleVerify = async () => {
    if (!orderResult.value?.id) return
    verifying.value = true
    try {
      await fetchVerifyAcmeOrder(orderResult.value.id)
      ElMessage.success('已开始验证，请稍后刷新状态')
      await refreshOrder()
    } finally {
      verifying.value = false
    }
  }

  const copyText = async (text: string) => {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制')
  }
</script>

<style lang="scss" scoped>
  .wizard-steps {
    margin-bottom: 24px;
  }

  .wizard-body {
    min-height: 280px;
  }

  .form-tip {
    margin-top: 6px;
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }

  .challenge-card {
    padding: 12px;
    margin-bottom: 12px;
    background: var(--el-fill-color-light);
    border-radius: 8px;
  }

  .challenge-domain {
    margin-bottom: 8px;
    font-weight: 500;
  }

  .field-row {
    display: flex;
    gap: 8px;
    align-items: center;
    margin-bottom: 6px;
    font-size: 13px;

    span {
      flex-shrink: 0;
      width: 64px;
      color: var(--el-text-color-secondary);
    }

    code {
      flex: 1;
      word-break: break-all;
    }

    .record-value {
      font-size: 12px;
    }
  }

  .zone-tip {
    margin-bottom: 8px;
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }

  .order-status {
    margin-top: 16px;
  }

  .error-text {
    margin-top: 8px;
    color: var(--el-color-danger);
  }
</style>
