<template>
  <div class="transport-page" v-loading="loading">
    <section class="setting-section">
      <div class="summary-bar">
        <span class="summary-label">当前生效</span>
        <span class="summary-value">{{ summaryText }}</span>
      </div>
    </section>

    <section class="setting-section">
      <div class="protocol-grid">
        <button
            v-for="item in protocolOptions"
            :key="item.value"
            type="button"
            class="protocol-card"
            :class="{
            'protocol-card--active': form.dataProtocol === item.value,
            'protocol-card--disabled': !item.available
          }"
            :disabled="!item.available"
            @click="selectProtocol(item.value)"
        >
          <span class="protocol-card__name">{{ item.label }}</span>
          <span class="protocol-card__port">端口 {{ item.port }}</span>
          <span class="protocol-card__desc">{{ item.desc }}</span>
          <span v-if="!item.available" class="protocol-card__badge">未启用</span>
        </button>
      </div>
    </section>

    <section class="setting-section">
      <div class="setting-list">
        <div v-if="showTunnelMode" class="setting-item">
          <ElRadioGroup v-model="form.tunnelType">
            <ElRadio :label="String(TunnelType.MULTIPLEX)">多路复用</ElRadio>
            <ElRadio :label="String(TunnelType.DIRECT)">独立连接</ElRadio>
          </ElRadioGroup>
        </div>

        <div class="setting-item">
          <span class="setting-name">隧道加密</span>
          <ElSwitch v-model="form.encrypt" :disabled="!encryptEditable"/>
        </div>

        <div class="setting-item">
          <div class="setting-row">
            <span class="setting-name">数据压缩</span>
            <ElSwitch v-model="form.compress"/>
          </div>

          <Transition name="compress-expand">
            <div v-if="form.compress" class="compress-panel">
              <div class="algorithm-grid" role="radiogroup" aria-label="压缩算法">
                <button
                    v-for="item in algorithmOptions"
                    :key="item.value"
                    type="button"
                    class="algorithm-card"
                    :class="{ 'algorithm-card--active': form.compressAlgorithm === item.value }"
                    role="radio"
                    :aria-checked="form.compressAlgorithm === item.value"
                    @click="form.compressAlgorithm = item.value"
                >
                  <span class="algorithm-card__name">{{ item.label }}</span>
                  <span class="algorithm-card__tag">{{ item.tag }}</span>
                </button>
              </div>
            </div>
          </Transition>
        </div>
      </div>
    </section>

    <div>
      <ElButton type="primary" :loading="saving" @click="handleSave">保存配置</ElButton>
    </div>
  </div>
</template>

<script setup lang="ts">
import {ref, reactive, watch, computed} from 'vue'
import {ElMessage} from 'element-plus'
import {fetchProxyTransport, saveProxyTransport} from '@/api/proxy-transport'
import type {ProxyConfigProtocol} from '../../menus'
import {
  TunnelType,
  ProtocolType,
  TransportProtocol,
  getTunnelTypeLabel,
  getTransportProtocolLabel
} from '@/enums/orbien/business'

defineOptions({name: 'TransportPage'})

const props = defineProps<{
  proxyId: string
  protocol: ProxyConfigProtocol
}>()

const loading = ref(false)
const saving = ref(false)
const detail = ref<Api.Proxy.ProxyTransportDetailDTO | null>(null)

const form = reactive({
  dataProtocol: TransportProtocol.TCP,
  encrypt: true,
  tunnelType: String(TunnelType.MULTIPLEX),
  compress: false,
  compressAlgorithm: 'snappy'
})

const algorithmOptions = [
  {value: 'snappy', label: 'Snappy', tag: '均衡'},
  {value: 'lz4', label: 'LZ4', tag: '速度'},
  {value: 'zstd', label: 'Zstd', tag: '压缩率'}
]

const isUdpProtocol = computed(() => props.protocol === ProtocolType.UDP)
const protocolConstraints = computed(() => detail.value?.protocolConstraints)
const globalTlsEnabled = computed(() => detail.value?.encryptConstraints?.globalTlsEnabled ?? false)

const protocolOptions = computed(() => {
  const c = protocolConstraints.value
  const available = new Set(c?.availableProtocols ?? [TransportProtocol.TCP])
  return [
    {
      value: TransportProtocol.TCP,
      label: 'TCP',
      port: c?.tcpPort ?? 9527,
      desc: '默认选项，兼容性最好',
      available: available.has(TransportProtocol.TCP)
    },
    {
      value: TransportProtocol.WEBSOCKET,
      label: 'WebSocket',
      port: c?.websocketPort ?? 9528,
      desc: '穿透防火墙友好，强制 TLS',
      available: available.has(TransportProtocol.WEBSOCKET)
    },
    {
      value: TransportProtocol.QUIC,
      label: 'QUIC',
      port: c?.quicPort ?? 9529,
      desc: '低延迟，强制 TLS，多路复用',
      available: available.has(TransportProtocol.QUIC)
    }
  ]
})

const requiresTlsProtocol = computed(
    () =>
        form.dataProtocol === TransportProtocol.WEBSOCKET ||
        form.dataProtocol === TransportProtocol.QUIC
)

const multiplexOnly = computed(
    () => isUdpProtocol.value || form.dataProtocol === TransportProtocol.QUIC
)

const showTunnelMode = computed(() => !multiplexOnly.value)

const encryptEditable = computed(() => {
  if (requiresTlsProtocol.value) return false
  if (form.dataProtocol === TransportProtocol.TCP) {
    return globalTlsEnabled.value
  }
  return true
})

const compressSummary = computed(() => {
  if (!form.compress) return '无压缩'
  const algo = algorithmOptions.find(item => item.value === form.compressAlgorithm)
  return `${algo?.label ?? form.compressAlgorithm} 压缩`
})

const summaryText = computed(() => {
  const protocol = getTransportProtocolLabel(
      detail.value?.effectiveDataProtocol ?? form.dataProtocol
  )
  const tunnel = getTunnelTypeLabel(
      multiplexOnly.value ? TunnelType.MULTIPLEX : Number(form.tunnelType)
  )
  const enc = (requiresTlsProtocol.value ? true : form.encrypt) ? 'TLS 加密' : '明文传输'
  return `${protocol} · ${tunnel} · ${enc} · ${compressSummary.value}`
})

const applyDependentFields = () => {
  if (requiresTlsProtocol.value) {
    form.encrypt = true
  } else if (form.dataProtocol === TransportProtocol.TCP && !globalTlsEnabled.value) {
    form.encrypt = false
  }
  if (multiplexOnly.value) {
    form.tunnelType = String(TunnelType.MULTIPLEX)
  }
}

const selectProtocol = (value: number) => {
  form.dataProtocol = value
  applyDependentFields()
}

const applyDetail = (data: Api.Proxy.ProxyTransportDetailDTO) => {
  detail.value = data
  form.dataProtocol = data.effectiveDataProtocol ?? data.dataProtocol ?? TransportProtocol.TCP
  form.encrypt = data.encrypt
  form.compress = data.compress ?? false
  form.compressAlgorithm = data.compressAlgorithm ?? 'snappy'
  const effective = data.effectiveTunnelType ?? data.tunnelType ?? TunnelType.MULTIPLEX
  form.tunnelType = String(effective)
  applyDependentFields()
}

const loadData = async () => {
  loading.value = true
  try {
    const data = await fetchProxyTransport(props.proxyId)
    applyDetail(data)
  } finally {
    loading.value = false
  }
}

const handleSave = async () => {
  saving.value = true
  try {
    await saveProxyTransport(props.proxyId, {
      dataProtocol: form.dataProtocol,
      encrypt: form.encrypt,
      tunnelType: Number(form.tunnelType),
      compress: form.compress,
      compressAlgorithm: form.compress ? form.compressAlgorithm : undefined
    })
    ElMessage.success('传输配置已保存')
    await loadData()
  } finally {
    saving.value = false
  }
}

watch(
    () => [props.proxyId, props.protocol] as const,
    ([proxyId]) => {
      if (proxyId) loadData()
    },
    {immediate: true}
)
</script>

<style lang="scss" scoped>
.transport-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.setting-section {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  padding: 16px;
}

.summary-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  background: var(--el-fill-color-light);
  border-radius: 6px;
}

.summary-label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  flex-shrink: 0;
}

.summary-value {
  font-size: 13px;
  font-weight: 500;
}

.protocol-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.protocol-card {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  padding: 12px;
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  background: var(--el-bg-color);
  cursor: pointer;
  text-align: left;
  transition: border-color 0.15s, box-shadow 0.15s;

  &:hover:not(:disabled) {
    border-color: var(--el-color-primary-light-5);
  }

  &--active {
    border-color: var(--el-color-primary);
    box-shadow: 0 0 0 1px var(--el-color-primary-light-7);
    background: var(--el-color-primary-light-9);
  }

  &--disabled {
    opacity: 0.55;
    cursor: not-allowed;
  }
}

.protocol-card__name {
  font-weight: 600;
  font-size: 14px;
}

.protocol-card__port {
  font-size: 11px;
  color: var(--el-text-color-secondary);
}

.protocol-card__desc {
  font-size: 12px;
  color: var(--el-text-color-regular);
  line-height: 1.4;
}

.protocol-card__badge {
  position: absolute;
  top: 8px;
  right: 8px;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 4px;
  background: var(--el-fill-color);
  color: var(--el-text-color-secondary);
}

.setting-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.setting-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.setting-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.compress-panel {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-top: 4px;
}

.algorithm-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.algorithm-card {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
  padding: 10px 12px;
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  background: var(--el-bg-color);
  cursor: pointer;
  text-align: left;
  transition: border-color 0.15s, box-shadow 0.15s, background 0.15s;

  &:hover {
    border-color: var(--el-color-primary-light-5);
  }

  &--active {
    border-color: var(--el-color-primary);
    box-shadow: 0 0 0 1px var(--el-color-primary-light-7);
    background: var(--el-color-primary-light-9);
  }
}

.algorithm-card__name {
  font-size: 13px;
  font-weight: 600;
  line-height: 1.3;
}

.algorithm-card__tag {
  font-size: 11px;
  color: var(--el-text-color-secondary);
  line-height: 1.3;
}

.setting-hint {
  margin: 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.4;
}

.setting-name {
  font-weight: 500;
}

.compress-expand-enter-active,
.compress-expand-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}

.compress-expand-enter-from,
.compress-expand-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

@media (max-width: 720px) {
  .protocol-grid,
  .algorithm-grid {
    grid-template-columns: 1fr;
  }
}
</style>
