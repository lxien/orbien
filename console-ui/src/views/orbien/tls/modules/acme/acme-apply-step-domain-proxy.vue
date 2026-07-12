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
      <div class="section-title">当前代理</div>
      <div class="proxy-context art-card-sm">
        <span class="proxy-context__name">{{ proxyName || '当前代理' }}</span>
        <ElTag size="small" type="info">{{ domainOptions.length }} 个域名</ElTag>
      </div>
    </section>

    <section class="section-block">
      <div class="section-title">选择域名</div>
      <ElEmpty v-if="!domainLoading && !domainOptions.length" description="当前代理暂无可用域名"/>
      <AcmeDomainSelectTable
          v-else
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
import {fetchAcmeHttpsProxyDomains} from '@/api/acme-order'
import {certBrandOptions} from './types'
import type {CertBrand} from './types'
import AcmeDomainSelectTable from './acme-domain-select-table.vue'
import AcmeApplyDomainSummary from './acme-apply-domain-summary.vue'

defineOptions({name: 'AcmeApplyStepDomainProxy'})

const props = defineProps<{
  visible: boolean
  proxyId: string
  proxyName?: string
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

const domainOptions = ref<Api.AcmeOrder.HttpsProxyDomainOption[]>([])
const domainLoading = ref(false)

const selectedDomainsModel = computed({
  get: () => props.selectedDomains,
  set: (value) => emit('update:selectedDomains', value)
})

const loadDomains = async (proxyId: string) => {
  if (!proxyId) {
    domainOptions.value = []
    emit('update:selectedDomains', [])
    return
  }
  domainLoading.value = true
  try {
    domainOptions.value = await fetchAcmeHttpsProxyDomains(proxyId)
    emit('update:selectedDomains', [])
  } finally {
    domainLoading.value = false
  }
}

watch(
    () => [props.visible, props.proxyId] as const,
    ([visible, proxyId]) => {
      if (visible && proxyId) {
        void loadDomains(proxyId)
      }
    },
    {immediate: true}
)
</script>

<style lang="scss">
@use './acme-apply-shared.scss';
</style>
