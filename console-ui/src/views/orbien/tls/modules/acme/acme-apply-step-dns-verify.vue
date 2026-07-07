<template>
  <div class="acme-wizard dns-verify-step">
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
          <ElButton link type="primary" @click="emit('copy', item.hostRecord || item.recordName)">复制</ElButton>
        </div>
        <div class="field-row">
          <span>记录类型</span>
          <code>TXT</code>
        </div>
        <div class="field-row">
          <span>记录值</span>
          <code class="record-value">{{ item.recordValue }}</code>
          <ElButton link type="primary" @click="emit('copy', item.recordValue)">复制</ElButton>
        </div>
      </div>
    </div>

    <div v-if="orderResult" class="order-status">
      当前状态：
      <ElTag size="small" :type="resolveAcmeOrderStatusTagType(orderResult.status)">
        {{ orderResult.statusLabel }}
      </ElTag>
    </div>
    <div v-if="orderResult?.errorMessage" class="error-text">{{ orderResult.errorMessage }}</div>
  </div>
</template>

<script setup lang="ts">
import {resolveAcmeOrderStatusTagType} from '@/utils/ui/status-tag'

defineOptions({name: 'AcmeApplyStepDnsVerify'})

defineProps<{
  validationMode: number
  orderResult: Api.AcmeOrder.OrderDTO | null
}>()

const emit = defineEmits<{
  (e: 'copy', text: string): void
}>()
</script>

<style lang="scss">
@use './acme-apply-shared.scss';
</style>
