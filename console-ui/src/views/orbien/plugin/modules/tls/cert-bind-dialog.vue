<template>
  <ElDialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="760px"
      align-center
      destroy-on-close
      append-to-body
  >
    <div v-loading="loading" class="cert-bind-dialog">
      <section class="bind-section">
        <div class="bind-section__title">绑定域名</div>
        <div class="domain-tags">
          <ElTag v-for="item in domains" :key="item.proxyDomainId" type="info" effect="plain">
            {{ item.fullDomain }}
          </ElTag>
        </div>
        <div v-if="overrideCount" class="bind-hint text-warning">
          {{ overrideCount }} 个域名已有证书，确认后将覆盖
        </div>
      </section>

      <section class="bind-section">
        <div class="bind-section__title">选择证书</div>
        <ElEmpty v-if="!loading && !matchedCerts.length" description="暂无覆盖所选域名的证书"/>
        <ElRadioGroup v-else v-model="selectedCertId" class="cert-picker">
          <label
              v-for="cert in matchedCerts"
              :key="cert.id"
              class="cert-card art-card-sm"
              :class="{ 'is-active': selectedCertId === cert.id }"
          >
            <ElRadio :value="cert.id"/>
            <div class="cert-card__body">
              <div class="cert-card__header">
                <span class="cert-card__san">{{ cert.sanDomains?.join(', ') }}</span>
                <ElTag size="small" :type="cert.source === 2 ? 'primary' : 'warning'" effect="plain">
                  {{ cert.source === 2 ? 'ACME' : '手动' }}
                </ElTag>
              </div>
              <div class="cert-card__meta">
                <span>{{ cert.issuer || '未知颁发者' }}</span>
                <span>到期 {{ formatDate(cert.notAfter) }}</span>
              </div>
              <div v-if="cert.boundDomainCount" class="cert-card__usage">
                已用于 {{ cert.boundDomainCount }} 个域名
              </div>
            </div>
          </label>
        </ElRadioGroup>
      </section>
    </div>

    <template #footer>
      <ElButton @click="dialogVisible = false">取消</ElButton>
      <ElButton type="primary" :disabled="!selectedCertId" :loading="submitting" @click="handleSubmit">
        确认绑定
      </ElButton>
    </template>
  </ElDialog>
</template>

<script setup lang="ts">
import {computed, ref, watch} from 'vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import {fetchBindCert} from '@/api/cert-binding'
import {fetchGetCertListByPage} from '@/api/tls'

defineOptions({name: 'PluginCertBindDialog'})

const props = defineProps<{
  visible: boolean
  domains: Api.CertBinding.ProxyDomainCertItem[]
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'success'): void
}>()

const dialogVisible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value)
})

const loading = ref(false)
const submitting = ref(false)
const allCerts = ref<Api.Tls.CertDTO[]>([])
const selectedCertId = ref('')

const dialogTitle = computed(() =>
    props.domains.length > 1 ? `批量绑定证书（${props.domains.length} 个域名）` : '绑定证书'
)

const overrideCount = computed(
    () => props.domains.filter((item) => item.binding).length
)

const formatDate = (dateStr?: string) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

const isDomainMatchedByCert = (domain: string, cert: Api.Tls.CertDTO) => {
  const sanList = cert.sanDomains || []
  const normalized = domain.trim().toLowerCase()
  return sanList.some((san) => {
    const s = san.trim().toLowerCase()
    if (normalized === s) return true
    if (s.startsWith('*.')) {
      const suffix = s.substring(1)
      return (
          normalized.endsWith(suffix) &&
          normalized.length > suffix.length &&
          !normalized.slice(0, -suffix.length).includes('.')
      )
    }
    return false
  })
}

const matchedCerts = computed(() => {
  if (!props.domains.length) return []
  return allCerts.value.filter((cert) =>
      props.domains.every((domain) => isDomainMatchedByCert(domain.fullDomain, cert))
  )
})

const loadCerts = async () => {
  loading.value = true
  try {
    const page = await fetchGetCertListByPage({current: 1, size: 200})
    allCerts.value = page.records || []
  } finally {
    loading.value = false
  }
}

const resetSelection = () => {
  const currentCertId = props.domains.length === 1 ? props.domains[0].binding?.certId : ''
  const preferred = matchedCerts.value.find((cert) => cert.id === currentCertId)
  selectedCertId.value = preferred?.id || matchedCerts.value[0]?.id || ''
}

watch(
    () => props.visible,
    async (visible) => {
      if (!visible) return
      await loadCerts()
      resetSelection()
    }
)

watch(matchedCerts, () => {
  if (!selectedCertId.value || !matchedCerts.value.some((cert) => cert.id === selectedCertId.value)) {
    selectedCertId.value = matchedCerts.value[0]?.id || ''
  }
})

const handleSubmit = async () => {
  if (!selectedCertId.value || !props.domains.length) return
  if (overrideCount.value > 0) {
    await ElMessageBox.confirm('部分域名已有绑定，继续将覆盖原有证书，是否继续？', '绑定确认', {
      type: 'warning'
    })
  }
  submitting.value = true
  try {
    const result = await fetchBindCert({
      certId: selectedCertId.value,
      proxyDomainIds: props.domains.map((item) => item.proxyDomainId),
      override: true
    })
    if (result.failedCount > 0) {
      ElMessage.warning(`绑定完成：成功 ${result.successCount} 个，失败 ${result.failedCount} 个`)
    } else {
      ElMessage.success(`已成功绑定 ${result.successCount} 个域名`)
    }
    dialogVisible.value = false
    emit('success')
  } finally {
    submitting.value = false
  }
}
</script>

<style lang="scss" scoped>
.cert-bind-dialog {
  min-height: 220px;
}

.bind-section + .bind-section {
  margin-top: 24px;
}

.bind-section__title {
  margin-bottom: 12px;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.domain-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.bind-hint {
  margin-top: 8px;
  font-size: 13px;
}

.text-warning {
  color: var(--el-color-warning);
}

.cert-picker {
  display: grid !important;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  align-content: start;
  width: 100%;
  max-height: 420px;
  overflow-y: auto;
  padding-right: 2px;
}

.cert-card {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  height: 100%;
  min-height: 108px;
  padding: 14px 16px;
  margin: 0;
  box-sizing: border-box;
  cursor: pointer;
  transition: background-color 0.2s, border-color 0.2s;

  &:hover:not(.is-active) {
    background-color: var(--art-hover-color);
  }

  &.is-active {
    border-color: var(--theme-color) !important;
    background-color: color-mix(in srgb, var(--theme-color) 8%, var(--default-box-color));
  }

  :deep(.el-radio) {
    height: auto;
    margin-right: 0;
    flex-shrink: 0;
    padding-top: 2px;
  }

  :deep(.el-radio__label) {
    display: none;
  }
}

.cert-card__body {
  flex: 1;
  min-width: 0;
}

.cert-card__header {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 8px;
}

.cert-card__san {
  flex: 1;
  min-width: 0;
  font-size: 13px;
  font-weight: 600;
  line-height: 1.45;
  color: var(--el-text-color-primary);
  word-break: break-all;
}

.cert-card__meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 12px;
  line-height: 1.4;
  color: var(--el-text-color-secondary);
}

.cert-card__usage {
  margin-top: 6px;
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}
</style>
