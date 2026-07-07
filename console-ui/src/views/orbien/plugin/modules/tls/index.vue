<template>
  <div class="tls-page">
    <div class="tls-page-content">
      <div v-if="matrix" class="tls-summary">
        <span>域名证书：{{ matrix.boundCount }}/{{ matrix.totalDomains }} 已配置</span>
        <ElTag v-if="matrix.warningCount > 0" type="danger" size="small">
          {{ matrix.warningCount }} 个异常
        </ElTag>
      </div>

      <ElTabs v-model="activeName" type="card">
        <ElTabPane label="域名证书" name="domain-cert">
          <div class="tab-content">
            <div class="toolbar">
              <ElButton type="primary" @click="openUploadDialog">上传并绑定</ElButton>
              <ElButton @click="handleDisableAll" :disabled="!matrix?.boundCount">全部禁用</ElButton>
            </div>

            <ArtTable
              row-key="proxyDomainId"
              :show-table-header="false"
              :loading="matrixLoading"
              :data="matrix?.domains || []"
              :columns="domainColumns"
            />
          </div>
        </ElTabPane>

        <ElTabPane label="证书夹" name="cert-folder">
          <div class="cert-folder-content">
            <ArtTable
              row-key="id"
              :show-table-header="false"
              :loading="certLoading"
              :data="certTableData"
              :columns="certColumns"
              :pagination="certPagination"
              @pagination:size-change="handleCertSizeChange"
              @pagination:current-change="handleCertCurrentChange"
            />
          </div>
        </ElTabPane>
      </ElTabs>
    </div>

    <ElDialog v-model="uploadDialogVisible" title="上传并绑定证书" width="720px" align-center>
      <div class="upload-form">
        <div class="form-item">
          <div class="form-label">绑定域名</div>
          <ElSelect v-model="uploadForm.proxyDomainIds" multiple placeholder="选择要绑定的域名" style="width: 100%">
            <ElOption
              v-for="item in matrix?.domains || []"
              :key="item.proxyDomainId"
              :label="item.fullDomain"
              :value="item.proxyDomainId"
            />
          </ElSelect>
        </div>
        <div class="form-item">
          <div class="form-label">私钥(KEY)</div>
          <ElInput v-model="uploadForm.keyContent" type="textarea" :rows="8" resize="none" />
        </div>
        <div class="form-item">
          <div class="form-label">证书(PEM格式)</div>
          <ElInput v-model="uploadForm.certContent" type="textarea" :rows="8" resize="none" />
        </div>
      </div>
      <template #footer>
        <ElButton @click="uploadDialogVisible = false">取消</ElButton>
        <ElButton type="primary" :loading="uploadSubmitting" @click="handleUploadAndBind">保存并绑定</ElButton>
      </template>
    </ElDialog>

    <ElDialog v-model="bindDialogVisible" title="绑定证书" width="520px" align-center>
      <div v-if="currentBindDomain">
        为域名 <strong>{{ currentBindDomain.fullDomain }}</strong> 选择证书：
      </div>
      <ElSelect v-model="selectedCertId" placeholder="请选择证书" style="width: 100%; margin-top: 16px">
        <ElOption
          v-for="cert in matchedCerts"
          :key="cert.id"
          :label="`${cert.sanDomains?.join(', ')} (${formatDate(cert.notAfter)})`"
          :value="cert.id"
        />
      </ElSelect>
      <template #footer>
        <ElButton @click="bindDialogVisible = false">取消</ElButton>
        <ElButton type="primary" :disabled="!selectedCertId" @click="confirmBindCert">确定</ElButton>
      </template>
    </ElDialog>
  </div>
</template>

<script setup lang="ts">
  import { ref, reactive, watch, h, computed } from 'vue'
  import { useTable } from '@/hooks/core/useTable'
  import { ElMessage, ElMessageBox, ElTag } from 'element-plus'
  import ArtTable from '@/components/core/tables/art-table/index.vue'
  import ArtButtonTable from '@/components/core/forms/art-button-table/index.vue'
  import { fetchGetCertListByPage, fetchDeleteCert, fetchSaveAndDeployCert } from '@/api/tls'
  import {
    fetchGetProxyCertMatrix,
    fetchBindCert,
    fetchDisableBinding,
    fetchEnableBinding,
    fetchUnbindBinding,
    fetchRedeployBinding,
    fetchDisableAllBindingsByProxy
  } from '@/api/cert-binding'
  import { resolveTlsBindStatusTagType } from '@/utils/ui/status-tag'

  defineOptions({ name: 'TlsPage' })

  const props = defineProps<{ proxyId: string }>()

  const activeName = ref('domain-cert')
  const matrixLoading = ref(false)
  const matrix = ref<Api.CertBinding.ProxyCertMatrix | null>(null)

  const uploadDialogVisible = ref(false)
  const uploadSubmitting = ref(false)
  const uploadForm = reactive({
    proxyDomainIds: [] as number[],
    keyContent: '',
    certContent: ''
  })

  const bindDialogVisible = ref(false)
  const selectedCertId = ref('')
  const currentBindDomain = ref<Api.CertBinding.ProxyDomainCertItem | null>(null)
  const allCerts = ref<Api.Tls.CertDTO[]>([])

  const matchedCerts = computed(() => {
    if (!currentBindDomain.value) return []
    const domain = currentBindDomain.value.fullDomain
    return allCerts.value.filter((cert) => isDomainMatchedByCert(domain, cert))
  })

  const formatDate = (dateStr: string) => {
    const date = new Date(dateStr)
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
  }

  const bindStatusTag = (status?: number) => {
    const map: Record<number, string> = {
      1: '正常',
      2: '已禁用',
      3: 'SAN不匹配',
      4: '已过期',
      5: '部署失败'
    }
    if (!status) {
      return h(ElTag, { type: 'info', size: 'small' }, () => '未配置')
    }
    const text = map[status] || '未知'
    return h(ElTag, { type: resolveTlsBindStatusTagType(status), size: 'small' }, () => text)
  }

  const isDomainMatchedByCert = (domain: string, cert: Api.Tls.CertDTO) => {
    const sanList = cert.sanDomains || []
    const normalized = domain.trim().toLowerCase()
    return sanList.some((san) => {
      const s = san.trim().toLowerCase()
      if (normalized === s) return true
      if (s.startsWith('*.')) {
        const suffix = s.substring(1)
        return normalized.endsWith(suffix) && normalized.length > suffix.length && !normalized.slice(0, -suffix.length).includes('.')
      }
      return false
    })
  }

  const domainColumns = computed(() => [
    {
      prop: 'fullDomain',
      label: '域名',
      minWidth: 160
    },
    {
      prop: 'binding',
      label: '证书 SAN',
      minWidth: 160,
      formatter: (row: Api.CertBinding.ProxyDomainCertItem) =>
        row.binding?.certSanDomains?.join(', ') || '-'
    },
    {
      prop: 'notAfter',
      label: '到期时间',
      width: 120,
      formatter: (row: Api.CertBinding.ProxyDomainCertItem) =>
        row.binding?.notAfter ? formatDate(row.binding.notAfter) : '-'
    },
    {
      prop: 'status',
      label: '状态',
      width: 100,
      formatter: (row: Api.CertBinding.ProxyDomainCertItem) =>
        bindStatusTag(row.binding?.bindStatus)
    },
    {
      prop: 'operation',
      label: '操作',
      width: 220,
      formatter: (row: Api.CertBinding.ProxyDomainCertItem) => {
        const binding = row.binding
        if (!binding) {
          return h(ArtButtonTable, {
            type: 'link',
            text: '绑定证书',
            onClick: () => openBindDialog(row)
          })
        }
        const actions = []
        if (binding.bindStatus === 2) {
          actions.push(
            h(ArtButtonTable, {
              type: 'link',
              text: '启用',
              onClick: () => handleEnable(binding.bindingId)
            })
          )
        } else if (binding.bindStatus === 5) {
          actions.push(
            h(ArtButtonTable, {
              type: 'link',
              text: '重试',
              onClick: () => handleRedeploy(binding.bindingId)
            })
          )
        } else if (binding.bindStatus === 1) {
          actions.push(
            h(ArtButtonTable, {
              type: 'link',
              text: '换证',
              onClick: () => openBindDialog(row)
            }),
            h(ArtButtonTable, {
              type: 'link',
              text: '禁用',
              onClick: () => handleDisable(binding.bindingId)
            })
          )
        } else {
          actions.push(
            h(ArtButtonTable, {
              type: 'link',
              text: '换证',
              onClick: () => openBindDialog(row)
            })
          )
        }
        actions.push(
          h(ArtButtonTable, {
            type: 'link',
            text: '解绑',
            onClick: () => handleUnbind(binding.bindingId)
          })
        )
        return h('div', actions)
      }
    }
  ])

  const {
    columns: certColumns,
    data: certTableData,
    loading: certLoading,
    pagination: certPagination,
    handleSizeChange: handleCertSizeChange,
    handleCurrentChange: handleCertCurrentChange,
    getData: reloadCertList
  } = useTable({
    core: {
      apiFn: fetchGetCertListByPage,
      apiParams: { current: 1, size: 10 },
      columnsFactory: () => [
        {
          prop: 'sanDomains',
          label: '认证域名',
          minWidth: 150,
          formatter: (row: Api.Tls.CertDTO) => row.sanDomains?.join(', ') || ''
        },
        {
          prop: 'issuer',
          label: '品牌',
          minWidth: 100
        },
        {
          prop: 'notAfter',
          label: '到期时间',
          minWidth: 120,
          formatter: (row: Api.Tls.CertDTO) => formatDate(row.notAfter)
        },
        {
          prop: 'operation',
          label: '操作',
          width: 160,
          formatter: (row: Api.Tls.CertDTO) =>
            h('div', [
              h(ArtButtonTable, {
                type: 'link',
                text: '绑定到本代理',
                onClick: () => handleBindCertToProxy(row)
              }),
              h(ArtButtonTable, {
                type: 'link',
                text: '删除',
                onClick: () => handleCertDelete(row)
              })
            ])
        }
      ]
    }
  })

  const loadMatrix = async () => {
    if (!props.proxyId) return
    matrixLoading.value = true
    try {
      matrix.value = await fetchGetProxyCertMatrix(props.proxyId)
    } finally {
      matrixLoading.value = false
    }
  }

  const loadAllCerts = async () => {
    const page = await fetchGetCertListByPage({ current: 1, size: 200 })
    allCerts.value = page.records || []
  }

  watch(
    () => props.proxyId,
    async () => {
      await Promise.all([loadMatrix(), loadAllCerts(), reloadCertList()])
    },
    { immediate: true }
  )

  const openUploadDialog = () => {
    uploadForm.proxyDomainIds = (matrix.value?.domains || [])
      .filter((item) => !item.binding)
      .map((item) => item.proxyDomainId)
    uploadForm.keyContent = ''
    uploadForm.certContent = ''
    uploadDialogVisible.value = true
  }

  const handleUploadAndBind = async () => {
    if (!uploadForm.keyContent.trim() || !uploadForm.certContent.trim()) {
      ElMessage.warning('请输入完整的私钥和证书')
      return
    }
    if (uploadForm.proxyDomainIds.length === 0) {
      ElMessage.warning('请选择至少一个绑定域名')
      return
    }
    uploadSubmitting.value = true
    try {
      const result = await fetchSaveAndDeployCert({
        proxyId: props.proxyId,
        key: uploadForm.keyContent.trim(),
        fullChain: uploadForm.certContent.trim(),
        proxyDomainIds: uploadForm.proxyDomainIds
      })
      if (result.failedCount > 0) {
        ElMessage.warning(`绑定完成：成功 ${result.successCount} 个，失败 ${result.failedCount} 个`)
      } else {
        ElMessage.success('证书已保存并绑定')
      }
      uploadDialogVisible.value = false
      await Promise.all([loadMatrix(), reloadCertList()])
      activeName.value = 'domain-cert'
    } finally {
      uploadSubmitting.value = false
    }
  }

  const openBindDialog = async (row: Api.CertBinding.ProxyDomainCertItem) => {
    currentBindDomain.value = row
    selectedCertId.value = row.binding?.certId || ''
    await loadAllCerts()
    bindDialogVisible.value = true
  }

  const confirmBindCert = async () => {
    if (!currentBindDomain.value || !selectedCertId.value) return
    await fetchBindCert({
      certId: selectedCertId.value,
      proxyDomainIds: [currentBindDomain.value.proxyDomainId],
      override: true
    })
    ElMessage.success('绑定成功')
    bindDialogVisible.value = false
    await loadMatrix()
  }

  const handleBindCertToProxy = async (cert: Api.Tls.CertDTO) => {
    const matchedDomainIds = (matrix.value?.domains || [])
      .filter((item) => isDomainMatchedByCert(item.fullDomain, cert))
      .map((item) => item.proxyDomainId)
    if (matchedDomainIds.length === 0) {
      ElMessage.warning('该证书 SAN 与本代理域名不匹配')
      return
    }
    await ElMessageBox.confirm(`将绑定到 ${matchedDomainIds.length} 个匹配域名，是否继续？`, '绑定确认', {
      type: 'warning'
    })
    const result = await fetchBindCert({
      certId: cert.id,
      proxyDomainIds: matchedDomainIds,
      override: true
    })
    if (result.failedCount > 0) {
      ElMessage.warning(`绑定完成：成功 ${result.successCount} 个，失败 ${result.failedCount} 个`)
    } else {
      ElMessage.success('绑定成功')
    }
    await loadMatrix()
    activeName.value = 'domain-cert'
  }

  const handleDisable = async (bindingId: number) => {
    await fetchDisableBinding(bindingId)
    ElMessage.success('已禁用')
    await loadMatrix()
  }

  const handleEnable = async (bindingId: number) => {
    await fetchEnableBinding(bindingId)
    ElMessage.success('已启用')
    await loadMatrix()
  }

  const handleUnbind = async (bindingId: number) => {
    await ElMessageBox.confirm('确定解绑该域名的证书？', '解绑确认', { type: 'warning' })
    await fetchUnbindBinding(bindingId)
    ElMessage.success('已解绑')
    await loadMatrix()
  }

  const handleRedeploy = async (bindingId: number) => {
    await fetchRedeployBinding(bindingId)
    ElMessage.success('已重新部署')
    await loadMatrix()
  }

  const handleDisableAll = async () => {
    await ElMessageBox.confirm('确定禁用本代理下所有域名的 TLS 证书？', '警告', { type: 'warning' })
    await fetchDisableAllBindingsByProxy(props.proxyId)
    ElMessage.success('已全部禁用')
    await loadMatrix()
  }

  const handleCertDelete = async (row: Api.Tls.CertDTO) => {
    await ElMessageBox.confirm('确定要删除该证书？', '警告', { type: 'warning' })
    await fetchDeleteCert([row.id])
    ElMessage.success('删除成功')
    await reloadCertList()
  }
</script>

<style scoped>
  .tls-page {
    height: 100%;
  }

  .tls-page-content {
    min-height: 100%;
    padding: 0 15px;
  }

  .tls-summary {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 12px;
    font-size: 14px;
  }

  .tab-content {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .toolbar {
    display: flex;
    gap: 8px;
  }

  .cert-folder-content {
    min-height: 300px;
  }

  .upload-form {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .form-label {
    font-size: 14px;
    font-weight: 500;
    margin-bottom: 8px;
  }
</style>
