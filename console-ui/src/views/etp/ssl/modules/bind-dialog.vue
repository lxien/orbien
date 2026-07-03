<template>
  <ElDialog v-model="dialogVisible" title="绑定证书到域名" width="720px" align-center>
    <div v-loading="loading">
      <ElAlert
        v-if="certSanText"
        type="info"
        :closable="false"
        show-icon
        class="mb-4"
        :title="`证书 SAN: ${certSanText}`"
      />
      <ElTable
        ref="tableRef"
        row-key="proxyDomainId"
        :data="domainList"
        max-height="420"
        @selection-change="handleSelectionChange"
      >
        <ElTableColumn type="selection" width="48" :selectable="isRowSelectable" />
        <ElTableColumn prop="fullDomain" label="域名" min-width="180" />
        <ElTableColumn prop="proxyName" label="所属代理" min-width="120" />
        <ElTableColumn label="匹配状态" width="110">
          <template #default="{ row }">
            <ElTag :type="row.matched ? 'success' : 'danger'" size="small">
              {{ row.matched ? '匹配' : '不匹配' }}
            </ElTag>
          </template>
        </ElTableColumn>
        <ElTableColumn label="当前证书" min-width="120">
          <template #default="{ row }">
            <span v-if="row.hasBinding" class="text-warning">已绑定</span>
            <span v-else class="text-secondary">未绑定</span>
          </template>
        </ElTableColumn>
      </ElTable>
      <div v-if="selectedRows.length" class="selection-tip">
        已选择 {{ selectedRows.length }} 个域名
        <span v-if="overrideCount">（{{ overrideCount }} 个将覆盖已有绑定）</span>
      </div>
    </div>

    <template #footer>
      <ElButton @click="handleCancel">取消</ElButton>
      <ElButton type="primary" :disabled="selectedRows.length === 0" :loading="submitting" @click="handleSubmit">
        确认绑定
      </ElButton>
    </template>
  </ElDialog>
</template>

<script setup lang="ts">
  import { ref, computed, watch } from 'vue'
  import { ElMessage, ElMessageBox } from 'element-plus'
  import { fetchListBindableDomains, fetchBindCert } from '@/api/cert-binding'
  import { fetchGetCertListByPage } from '@/api/ssl'

  defineOptions({ name: 'BindDialog' })

  interface Props {
    visible: boolean
    certId: string | null
  }

  interface Emits {
    (e: 'update:visible', value: boolean): void
    (e: 'submit'): void
  }

  const props = withDefaults(defineProps<Props>(), { certId: null })
  const emit = defineEmits<Emits>()

  const dialogVisible = computed({
    get: () => props.visible,
    set: (value) => emit('update:visible', value)
  })

  const loading = ref(false)
  const submitting = ref(false)
  const domainList = ref<Api.CertBinding.PreviewItem[]>([])
  const selectedRows = ref<Api.CertBinding.PreviewItem[]>([])
  const certSanText = ref('')

  const overrideCount = computed(
    () => selectedRows.value.filter((row) => row.hasBinding).length
  )

  const isRowSelectable = (row: Api.CertBinding.PreviewItem) => row.matched

  const loadData = async () => {
    if (!props.certId) return
    loading.value = true
    try {
      const [domains, certPage] = await Promise.all([
        fetchListBindableDomains(props.certId),
        fetchGetCertListByPage({ current: 1, size: 100 })
      ])
      domainList.value = domains || []
      const cert = certPage.records?.find((item) => item.id === props.certId)
      certSanText.value = cert?.sanDomains?.join(', ') || ''
      selectedRows.value = []
    } finally {
      loading.value = false
    }
  }

  watch(dialogVisible, (visible) => {
    if (visible) {
      loadData()
    }
  })

  const handleSelectionChange = (rows: Api.CertBinding.PreviewItem[]) => {
    selectedRows.value = rows
  }

  const handleCancel = () => {
    dialogVisible.value = false
  }

  const handleSubmit = async () => {
    if (!props.certId || selectedRows.value.length === 0) return

    const override = overrideCount.value > 0
    if (override) {
      await ElMessageBox.confirm('部分域名已有绑定，继续将覆盖原有证书，是否继续？', '绑定确认', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
    }

    submitting.value = true
    try {
      const result = await fetchBindCert({
        certId: props.certId,
        proxyDomainIds: selectedRows.value.map((row) => row.proxyDomainId),
        override: true
      })
      if (result.failedCount > 0) {
        ElMessage.warning(`绑定完成：成功 ${result.successCount} 个，失败 ${result.failedCount} 个`)
      } else {
        ElMessage.success(`已成功绑定 ${result.successCount} 个域名`)
      }
      dialogVisible.value = false
      emit('submit')
    } finally {
      submitting.value = false
    }
  }
</script>

<style scoped>
  .mb-4 {
    margin-bottom: 16px;
  }

  .selection-tip {
    margin-top: 12px;
    font-size: 13px;
    color: var(--el-text-color-secondary);
  }

  .text-warning {
    color: var(--el-color-warning);
  }

  .text-secondary {
    color: var(--el-text-color-secondary);
  }
</style>
