<template>
  <ElDialog
      v-model="dialogVisible"
      title="为当前代理申请证书"
      width="760px"
      align-center
      destroy-on-close
      append-to-body
      :close-on-click-modal="false"
  >
    <ElSteps :active="currentStep" finish-status="success" align-center class="acme-wizard wizard-steps">
      <ElStep title="选择域名"/>
      <ElStep title="验证方式"/>
      <ElStep title="DNS 验证"/>
    </ElSteps>

    <div class="acme-wizard wizard-body" v-loading="submitting" element-loading-text="正在提交申请，请稍候...">
      <AcmeApplyStepDomainProxy
          v-show="currentStep === 0"
          :visible="dialogVisible"
          :proxy-id="proxyId"
          :proxy-name="proxyName"
          v-model:cert-brand="certBrand"
          v-model:selected-domains="selectedDomains"
          v-model:extra-domain-text="extraDomainText"
          :parsed-extra-domains="parsedExtraDomains"
      />

      <AcmeApplyStepValidation
          v-show="currentStep === 1"
          v-model:validation-mode="validationMode"
          v-model:dns-credential-id="dnsCredentialId"
          :credential-list="credentialList"
          :credential-loading="credentialLoading"
          @add-credential="openDnsDialog"
      />

      <AcmeApplyStepDnsVerify
          v-show="currentStep === 2"
          :validation-mode="validationMode"
          :order-result="orderResult"
          @copy="copyText"
      />
    </div>

    <template #footer>
      <ElButton v-if="currentStep > 0 && currentStep < 2" :disabled="submitting" @click="currentStep--">上一步
      </ElButton>
      <ElButton v-if="currentStep < 1" type="primary" @click="handleNext">下一步</ElButton>
      <ElButton v-else-if="currentStep === 1" type="primary" :loading="submitting" @click="handleSubmit">提交申请
      </ElButton>
      <ElButton
          v-else-if="validationMode === 1 && canManualVerify"
          type="primary"
          :loading="verifying"
          @click="handleVerify"
      >
        开始验证
      </ElButton>
      <ElButton v-else-if="currentStep === 2" :loading="refreshing" @click="handleVerifyDone">刷新状态</ElButton>
    </template>
  </ElDialog>

  <DnsCredentialDialog v-model:visible="dnsDialogVisible" @submit="handleDnsCredentialSaved"/>
</template>

<script setup lang="ts">
import {computed, toRef} from 'vue'
import DnsCredentialDialog from '@/views/orbien/tls/modules/dns-credential-dialog.vue'
import AcmeApplyStepDomainProxy from '@/views/orbien/tls/modules/acme/acme-apply-step-domain-proxy.vue'
import AcmeApplyStepValidation from '@/views/orbien/tls/modules/acme/acme-apply-step-validation.vue'
import AcmeApplyStepDnsVerify from '@/views/orbien/tls/modules/acme/acme-apply-step-dns-verify.vue'
import {useAcmeApplyWizard} from '@/views/orbien/tls/modules/acme/use-acme-apply-wizard'

defineOptions({name: 'PluginAcmeApplyWizard'})

const props = defineProps<{
  visible: boolean
  proxyId: string
  proxyName?: string
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'success'): void
}>()

const dialogVisible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value)
})

const {
  currentStep,
  certBrand,
  selectedDomains,
  extraDomainText,
  parsedExtraDomains,
  validationMode,
  dnsCredentialId,
  credentialList,
  credentialLoading,
  dnsDialogVisible,
  submitting,
  verifying,
  refreshing,
  orderResult,
  canManualVerify,
  openDnsDialog,
  handleDnsCredentialSaved,
  goNextFromDomain,
  handleSubmit,
  refreshOrder,
  handleVerify,
  copyText
} = useAcmeApplyWizard({
  visible: toRef(props, 'visible'),
  onSuccess: () => emit('success')
})

const handleNext = () => {
  goNextFromDomain()
}

const handleVerifyDone = async () => {
  await refreshOrder()
}
</script>

<style lang="scss">
@use '@/views/orbien/tls/modules/acme/acme-apply-shared.scss';
</style>
