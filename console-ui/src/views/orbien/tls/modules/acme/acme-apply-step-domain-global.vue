<template>
  <div class="acme-wizard domain-step">
    <section class="section-block">
      <div class="section-title">证书品牌</div>
      <ElRadioGroup :model-value="certBrand" @update:model-value="emit('update:certBrand', $event)">
        <ElRadio v-for="item in certBrandOptions" :key="item.value" :value="item.value">
          {{ item.label }}
        </ElRadio>
      </ElRadioGroup>
    </section>

    <section class="section-block">
      <div class="section-title">HTTPS 隧道</div>
      <ElSelect
          :model-value="selectedProxyId"
          filterable
          clearable
          placeholder="选择要申请证书的 HTTPS 代理"
          style="width: 100%"
          :loading="proxyLoading"
          @update:model-value="handleProxyChange"
      >
        <ElOption
            v-for="item in proxyOptions"
            :key="item.proxyId"
            :label="formatProxyLabel(item)"
            :value="item.proxyId"
        >
          <div class="proxy-option">
            <span class="proxy-option__name">{{ item.name }}</span>
            <span class="proxy-option__meta">{{ item.agentName }} · {{ item.domainCount }} 个域名</span>
          </div>
        </ElOption>
      </ElSelect>

      <div v-if="selectedProxy" class="proxy-summary">
        <ElTag size="small" :type="selectedProxy.status === ProxyStatus.OPEN ? 'success' : 'info'">
          {{ selectedProxy.status === ProxyStatus.OPEN ? '已启用' : '已停用' }}
        </ElTag>
        <span v-if="selectedProxy.domainPreview.length" class="proxy-summary__domains">
          {{ selectedProxy.domainPreview.join('、') }}
          <template v-if="selectedProxy.domainCount > selectedProxy.domainPreview.length">
            等 {{ selectedProxy.domainCount }} 个
          </template>
        </span>
      </div>
    </section>

    <section v-if="selectedProxyId" class="section-block">
      <div class="section-title">选择域名</div>
      <AcmeDomainSelectTable
          v-model="selectedDomainsModel"
          :loading="domainLoading"
          :domain-options="domainOptions"
      />
    </section>

    <AcmeApplyDomainSummary
        :selected-domains="selectedDomains"
        :extra-domains="parsedExtraDomains"
        :extra-domain-text="extraDomainText"
        @update:extra-domain-text="emit('update:extraDomainText', $event)"
    />
  </div>
</template>

<script setup lang="ts">
import {computed, ref, watch} from 'vue'
import {fetchAcmeHttpsProxyDomains, fetchAcmeHttpsProxyOptions} from '@/api/acme-order'
import {ProxyStatus} from '@/enums/orbien/business'
import {certBrandOptions} from './types'
import type {CertBrand} from './types'
import AcmeDomainSelectTable from './acme-domain-select-table.vue'
import AcmeApplyDomainSummary from './acme-apply-domain-summary.vue'

defineOptions({name: 'AcmeApplyStepDomainGlobal'})

const props = defineProps<{
  visible: boolean
  certBrand: CertBrand
  selectedDomains: Api.AcmeOrder.HttpsProxyDomainOption[]
  extraDomainText: string
  parsedExtraDomains: string[]
}>()

const emit = defineEmits<{
  (e: 'update:certBrand', value: CertBrand): void
  (e: 'update:selectedDomains', value: Api.AcmeOrder.HttpsProxyDomainOption[]): void
  (e: 'update:extraDomainText', value: string): void
}>()

const selectedProxyId = ref<string>()
const proxyOptions = ref<Api.AcmeOrder.HttpsProxyOption[]>([])
const domainOptions = ref<Api.AcmeOrder.HttpsProxyDomainOption[]>([])
const proxyLoading = ref(false)
const domainLoading = ref(false)

const selectedProxy = computed(() =>
    proxyOptions.value.find((item) => item.proxyId === selectedProxyId.value)
)

const selectedDomainsModel = computed({
  get: () => props.selectedDomains,
  set: (value) => emit('update:selectedDomains', value)
})

const formatProxyLabel = (item: Api.AcmeOrder.HttpsProxyOption) =>
    `${item.name}（${item.agentName} · ${item.domainCount} 个域名）`

const resolveDefaultProxy = (options: Api.AcmeOrder.HttpsProxyOption[]) => {
  if (!options.length) return undefined
  return (
      options.find((item) => item.status === ProxyStatus.OPEN && item.domainCount > 0) ??
      options.find((item) => item.domainCount > 0) ??
      options[0]
  )
}

const loadProxyDomains = async (proxyId: string) => {
  domainLoading.value = true
  try {
    domainOptions.value = await fetchAcmeHttpsProxyDomains(proxyId)
    emit('update:selectedDomains', [])
  } finally {
    domainLoading.value = false
  }
}

const loadProxyOptions = async () => {
  proxyLoading.value = true
  try {
    proxyOptions.value = await fetchAcmeHttpsProxyOptions()
    const defaultProxy = resolveDefaultProxy(proxyOptions.value)
    if (defaultProxy) {
      selectedProxyId.value = defaultProxy.proxyId
      await loadProxyDomains(defaultProxy.proxyId)
    }
  } finally {
    proxyLoading.value = false
  }
}

const handleProxyChange = async (proxyId?: string) => {
  selectedProxyId.value = proxyId
  emit('update:selectedDomains', [])
  domainOptions.value = []
  if (!proxyId) return
  await loadProxyDomains(proxyId)
}

const reset = () => {
  selectedProxyId.value = undefined
  proxyOptions.value = []
  domainOptions.value = []
}

watch(
    () => props.visible,
    (visible) => {
      if (visible) {
        reset()
        void loadProxyOptions()
      }
    },
    {immediate: true}
)

defineExpose({validate: () => !!selectedProxyId.value})
</script>

<style lang="scss">
@use './acme-apply-shared.scss';
</style>
