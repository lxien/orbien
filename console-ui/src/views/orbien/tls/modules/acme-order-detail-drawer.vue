<template>
  <ElDrawer v-model="drawerVisible" title="申请详情" size="560px" destroy-on-close>
    <div v-loading="loading">
      <ElDescriptions :column="1" border>
        <ElDescriptionsItem label="订单号">{{ detail?.orderNo }}</ElDescriptionsItem>
        <ElDescriptionsItem label="状态">
          <ElTag size="small" :type="resolveAcmeOrderStatusTagType(detail?.status)">{{
              detail?.statusLabel
            }}
          </ElTag>
        </ElDescriptionsItem>
        <ElDescriptionsItem label="域名">{{ detail?.domains?.join(', ') }}</ElDescriptionsItem>
        <ElDescriptionsItem label="验证方式">
          {{ detail?.validationMode === 2 ? '云 DNS 自动' : '手动 DNS' }}
        </ElDescriptionsItem>
        <ElDescriptionsItem v-if="detail?.validationMode === 2" label="续期">
          支持自动续期
        </ElDescriptionsItem>
        <ElDescriptionsItem v-if="detail?.certId" label="证书 ID">{{
            detail.certId
          }}
        </ElDescriptionsItem>
        <ElDescriptionsItem v-if="detail?.errorMessage" label="错误信息">
          <span class="text-danger">{{ detail.errorMessage }}</span>
        </ElDescriptionsItem>
        <ElDescriptionsItem label="创建时间">{{ detail?.createdAt }}</ElDescriptionsItem>
      </ElDescriptions>

      <div v-if="detail?.challenges?.length" class="challenge-section">
        <div class="section-title">DNS TXT 记录</div>
        <div v-for="item in detail.challenges" :key="item.id" class="challenge-card">
          <div class="challenge-domain">{{ item.domain }}</div>
          <div v-if="item.dnsZone" class="zone-tip">DNS 区域：{{ item.dnsZone }}</div>
          <div class="challenge-row">
            <span class="label">主机记录</span>
            <ElInput :model-value="item.hostRecord || item.recordName" readonly>
              <template #append>
                <ElButton @click="copyText(item.hostRecord || item.recordName)">复制</ElButton>
              </template>
            </ElInput>
          </div>
          <div class="challenge-row">
            <span class="label">记录值</span>
            <ElInput :model-value="item.recordValue" readonly>
              <template #append>
                <ElButton @click="copyText(item.recordValue)">复制</ElButton>
              </template>
            </ElInput>
          </div>
          <ElTag size="small" :type="resolveAcmeChallengeStatusTagType(item.status)">
            {{ item.statusLabel || '待验证' }}
          </ElTag>
        </div>
      </div>

      <div v-if="showActions" class="drawer-actions">
        <ElButton v-if="canVerify" type="primary" :loading="actionLoading" @click="handleVerify">
          开始验证
        </ElButton>
        <ElButton v-if="canRetry" :loading="actionLoading" @click="handleRetry">重试</ElButton>
        <ElButton v-if="canCancel" :loading="actionLoading" @click="handleCancel"
        >取消申请
        </ElButton
        >
      </div>
    </div>
  </ElDrawer>
</template>

<script setup lang="ts">
import {computed, ref, watch} from 'vue'
import {ElMessage} from 'element-plus'
import {
  fetchAcmeOrderDetail,
  fetchCancelAcmeOrder,
  fetchRetryAcmeOrder,
  fetchVerifyAcmeOrder
} from '@/api/acme-order'
import {resolveAcmeChallengeStatusTagType, resolveAcmeOrderStatusTagType} from '@/utils/ui/status-tag'

defineOptions({name: 'AcmeOrderDetailDrawer'})

interface Props {
  visible: boolean
  orderId?: number | null
}

interface Emits {
  (e: 'update:visible', value: boolean): void

  (e: 'changed'): void
}

const props = withDefaults(defineProps<Props>(), {orderId: null})
const emit = defineEmits<Emits>()

const drawerVisible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value)
})

const loading = ref(false)
const actionLoading = ref(false)
const detail = ref<Api.AcmeOrder.OrderDTO | null>(null)

const terminalStatuses = [5, 6, 7]
const canVerify = computed(
    () => detail.value && [1, 2].includes(detail.value.status) && detail.value.validationMode === 1
)
const canRetry = computed(() => detail.value?.status === 6)
const canCancel = computed(() => detail.value && !terminalStatuses.includes(detail.value.status))
const showActions = computed(() => canVerify.value || canRetry.value || canCancel.value)

const loadDetail = async () => {
  if (!props.orderId) return
  loading.value = true
  try {
    detail.value = await fetchAcmeOrderDetail(props.orderId)
  } finally {
    loading.value = false
  }
}

watch(
    () => [props.visible, props.orderId],
    ([visible]) => {
      if (visible) loadDetail()
    }
)

const copyText = async (text: string) => {
  await navigator.clipboard.writeText(text)
  ElMessage.success('已复制')
}

const handleVerify = async () => {
  if (!props.orderId) return
  actionLoading.value = true
  try {
    await fetchVerifyAcmeOrder(props.orderId)
    ElMessage.success('已开始验证，请稍后刷新查看结果')
    emit('changed')
    await loadDetail()
  } finally {
    actionLoading.value = false
  }
}

const handleRetry = async () => {
  if (!props.orderId) return
  actionLoading.value = true
  try {
    await fetchRetryAcmeOrder(props.orderId)
    ElMessage.success('已重新提交验证')
    emit('changed')
    await loadDetail()
  } finally {
    actionLoading.value = false
  }
}

const handleCancel = async () => {
  if (!props.orderId) return
  actionLoading.value = true
  try {
    await fetchCancelAcmeOrder(props.orderId)
    ElMessage.success('已取消')
    emit('changed')
    await loadDetail()
  } finally {
    actionLoading.value = false
  }
}
</script>

<style lang="scss" scoped>
.challenge-section {
  margin-top: 20px;
}

.section-title {
  margin-bottom: 12px;
  font-weight: 600;
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

.zone-tip {
  margin-bottom: 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.challenge-row {
  margin-bottom: 8px;

  .label {
    display: block;
    margin-bottom: 4px;
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }
}

.drawer-actions {
  display: flex;
  gap: 8px;
  margin-top: 20px;
}

.text-danger {
  color: var(--el-color-danger);
}
</style>
