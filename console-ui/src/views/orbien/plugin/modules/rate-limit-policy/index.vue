<template>
  <div class="rate-limit-page" v-loading="loading">
    <section class="setting-section">
      <div class="section-header">
        <h3 class="section-title">带宽单位</h3>
        <p class="section-desc">统一设置下方所有带宽字段的输入与显示单位</p>
      </div>
      <ElSelect v-model="form.bandwidthUnit" placeholder="选择单位" class="unit-select">
        <ElOption v-for="unit in BANDWIDTH_UNIT_OPTIONS" :key="unit" :label="unit" :value="unit" />
      </ElSelect>
    </section>

    <section class="setting-section">
      <div class="section-header">
        <h3 class="section-title">带宽限制</h3>
        <p class="section-desc">分别限制入站与出站流量速率，留空表示不限制</p>
      </div>

      <div class="setting-list">
        <div v-for="field in LIMIT_FIELDS" :key="field.key" class="setting-item">
          <div class="setting-meta">
            <span class="setting-name">{{ field.label }}</span>
            <span class="setting-hint">{{ field.hint }}</span>
          </div>
          <div class="limit-input-wrap">
            <ElInputNumber
              v-model="form[field.key]"
              :placeholder="field.placeholder"
              :controls="false"
              :min="0"
              :precision="0"
              class="limit-input"
            />
            <span class="limit-unit">{{ form.bandwidthUnit }}</span>
          </div>
        </div>
      </div>
    </section>

    <div class="form-actions">
      <ElButton type="primary" :loading="saving" @click="handleSave">保存配置</ElButton>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref, reactive, watch } from 'vue'
  import {
    fetchProxyDetail,
    saveProxyBandwidthConfig,
    type ProxyDetail
  } from '@/api/proxy-plugin'
  import {
    BANDWIDTH_UNIT_OPTIONS,
    BANDWIDTH_UNIT_TO_BPS,
    BandwidthUnit
  } from '@/enums/orbien/business'
  import type { ProxyConfigProtocol } from '../../menus'

  defineOptions({ name: 'RateLimitPolicyPage' })

  type BandwidthUnitOption = (typeof BANDWIDTH_UNIT_OPTIONS)[number]

  const LIMIT_FIELDS = [
    {
      key: 'limitIn',
      label: '入站带宽',
      hint: '限制从客户端到内网服务的入站流量速率',
      placeholder: '不限'
    },
    {
      key: 'limitOut',
      label: '出站带宽',
      hint: '限制从内网服务到客户端的出站流量速率',
      placeholder: '不限'
    }
  ] as const

  type LimitKey = (typeof LIMIT_FIELDS)[number]['key']

  const props = defineProps<{
    proxyId: string
    protocol: ProxyConfigProtocol
  }>()

  const loading = ref(false)
  const saving = ref(false)
  const isInitializing = ref(false)
  const form = reactive<Record<LimitKey, number | undefined> & { bandwidthUnit: BandwidthUnitOption }>({
    limitIn: undefined,
    limitOut: undefined,
    bandwidthUnit: BandwidthUnit.MBPS
  })

  let detailSnapshot: ProxyDetail | null = null

  const resolveDisplayUnit = (limitIn?: number | null, limitOut?: number | null): BandwidthUnitOption => {
    const bpsValues = [limitIn, limitOut].filter((v): v is number => v != null)
    if (bpsValues.length === 0) return BandwidthUnit.MBPS

    const maxBps = Math.max(...bpsValues)
    if (maxBps >= BANDWIDTH_UNIT_TO_BPS[BandwidthUnit.GBPS]) return BandwidthUnit.GBPS
    if (maxBps >= BANDWIDTH_UNIT_TO_BPS[BandwidthUnit.MBPS]) return BandwidthUnit.MBPS
    return BandwidthUnit.KBPS
  }

  const bpsToUnit = (bps: number | null | undefined, unit: BandwidthUnitOption) => {
    if (bps == null) return undefined
    return Math.round(bps / BANDWIDTH_UNIT_TO_BPS[unit])
  }

  const convertBetweenUnits = (
    value: number | undefined,
    from: BandwidthUnitOption,
    to: BandwidthUnitOption
  ) => {
    if (value == null) return undefined
    return Math.round((value * BANDWIDTH_UNIT_TO_BPS[from]) / BANDWIDTH_UNIT_TO_BPS[to])
  }

  const fillForm = (bandwidth: Api.Proxy.BandwidthDTO | null) => {
    isInitializing.value = true
    const unit = resolveDisplayUnit(bandwidth?.limitIn, bandwidth?.limitOut)
    form.bandwidthUnit = unit
    form.limitIn = bpsToUnit(bandwidth?.limitIn, unit)
    form.limitOut = bpsToUnit(bandwidth?.limitOut, unit)
    isInitializing.value = false
  }

  const loadData = async () => {
    loading.value = true
    try {
      const detail = await fetchProxyDetail(props.protocol, props.proxyId)
      detailSnapshot = detail
      fillForm(detail.bandwidth)
    } finally {
      loading.value = false
    }
  }

  watch(
    () => [props.proxyId, props.protocol] as const,
    ([proxyId]) => {
      if (proxyId) loadData()
    },
    { immediate: true }
  )

  watch(
    () => form.bandwidthUnit,
    (newUnit, oldUnit) => {
      if (isInitializing.value || !oldUnit || newUnit === oldUnit) return

      for (const { key } of LIMIT_FIELDS) {
        form[key] = convertBetweenUnits(form[key], oldUnit, newUnit)
      }
    }
  )

  const handleSave = async () => {
    if (!detailSnapshot) return

    saving.value = true
    try {
      await saveProxyBandwidthConfig(props.protocol, detailSnapshot, {
        limitTotal: detailSnapshot.bandwidth?.limitTotal ?? null,
        limitIn: form.limitIn ?? null,
        limitOut: form.limitOut ?? null,
        unit: form.bandwidthUnit
      })
      await loadData()
    } finally {
      saving.value = false
    }
  }
</script>

<style scoped lang="scss">
  .rate-limit-page {
    max-width: 640px;
  }

  .setting-section {
    padding: 16px;
    margin-bottom: 16px;
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 8px;
    background: var(--el-fill-color-blank);
  }

  .section-header {
    margin-bottom: 16px;
  }

  .section-title {
    margin: 0 0 4px;
    font-size: 15px;
    font-weight: 600;
    color: var(--el-text-color-primary);
  }

  .section-desc {
    margin: 0;
    font-size: 13px;
    line-height: 1.5;
    color: var(--el-text-color-secondary);
  }

  .unit-select,
  .limit-input {
    width: 200px;
  }

  .setting-item {
    display: flex;
    flex-direction: column;
    gap: 10px;
    padding: 16px 0;
    border-top: 1px solid var(--el-border-color-lighter);

    &:first-child {
      padding-top: 0;
      border-top: none;
    }

    &:last-child {
      padding-bottom: 0;
    }
  }

  .setting-meta {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  .setting-name {
    font-size: 14px;
    font-weight: 500;
    color: var(--el-text-color-primary);
  }

  .setting-hint {
    font-size: 12px;
    line-height: 1.5;
    color: var(--el-text-color-secondary);
  }

  .limit-input-wrap {
    display: flex;
    align-items: center;
    gap: 8px;
    width: fit-content;
  }

  .limit-unit {
    font-size: 13px;
    color: var(--el-text-color-secondary);
  }

  .form-actions {
    padding-top: 4px;
  }
</style>
