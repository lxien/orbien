<template>
  <ElDialog
    v-model="dialogVisible"
    title="SSL 配置"
    top="2%"
    width="58%"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    @close="handleClose"
  >
    <div class="ssl-dialog-content">
      <ElTabs v-model="activeName" type="card">
        <template #add-icon>
          <ElSwitch v-model="forceHttps" />
        </template>
        <ElTabPane name="current-cert">
          <template #label>
            <span>
              当前证书-[
              <span
                :style="{
                  color: sslStatus === 1 ? 'var(--el-color-primary)' : 'var(--el-color-danger)'
                }"
              >
                {{ sslStatus === 1 ? '已部署SSL' : '未部署SSL' }}
              </span>
              ]
            </span>
          </template>
          <div class="tab-content">
            <div v-if="sslDeployInfo" class="ssl-info">
              <div class="info-column">
                <div class="info-item">
                  <span class="info-label">证书分类</span>
                  <span class="info-value">{{ sslDeployInfo.org }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">认证域名</span>
                  <span class="info-value">{{ sslDeployInfo.sanDomains?.join(', ') || '' }}</span>
                </div>
              </div>
              <div class="info-column">
                <div class="info-item">
                  <span class="info-label">证书品牌</span>
                  <span class="info-value">{{ sslDeployInfo.issuer }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">到期时间</span>
                  <span class="info-value">{{ formatDate(sslDeployInfo.notAfter) }}</span>
                </div>
              </div>
            </div>
            <div v-else class="ssl-info ssl-info--empty">
              <span class="ssl-empty-text">暂未配置SSL证书</span>
            </div>
            <div class="form-wrapper">
              <div class="form-item">
                <div class="form-label">私钥(KEY)</div>
                <ElInput v-model="certData.keyContent" type="textarea" resize="none" />
              </div>

              <div class="form-item">
                <div class="form-label">证书(PEM格式)</div>
                <ElInput v-model="certData.certContent" type="textarea" resize="none" />
              </div>
            </div>
            <div class="form-actions">
              <ElSpace v-if="sslStatus !== 1">
                <ElButton type="primary" @click="handleSaveAndDeploy">保存并部署证书</ElButton>
                <ElButton v-if="sslDeployInfo" @click="handleDownloadCurrentCert"
                  >下载证书</ElButton
                >
              </ElSpace>
              <ElSpace v-else>
                <ElButton type="primary" @click="handleSaveAndDeploy">保存</ElButton>
                <ElButton @click="handleDownloadCurrentCert">下载证书</ElButton>
                <ElButton @click="handleCloseSsl">关闭SSL</ElButton>
              </ElSpace>
            </div>
          </div>
        </ElTabPane>
        <ElTabPane label="证书夹" name="cert-folder">
          <div class="cert-folder-content">
            <ArtTable
              rowKey="id"
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
    <DeployDialog v-model:visible="deployDialogVisible" />
  </ElDialog>
</template>

<script setup lang="ts">
  import { ref, reactive, watch, h } from 'vue'
  import { useTable } from '@/hooks/core/useTable'
  import { ElMessage, ElMessageBox } from 'element-plus'
  import ArtTable from '@/components/core/tables/art-table/index.vue'
  import ArtButtonTable from '@/components/core/forms/art-button-table/index.vue'
  import DeployDialog from './deploy-dialog.vue'
  import {
    fetchGetCertListByPage,
    fetchDownloadCert,
    fetchDeleteCert,
    fetchSaveAndDeployCert
  } from '@/api/ssl'
  import { downloadBlob } from '@/utils/download'
  import { fetchGetSslDeployInfo, fetchCloseSsl } from '@/api/deploy'

  defineOptions({ name: 'SslDialog' })

  const props = defineProps({
    visible: { type: Boolean, default: false },
    proxyId: { type: String, required: true }
  })

  const emit = defineEmits(['update:visible', 'close'])

  const dialogVisible = ref(false)
  const activeName = ref('current-cert')
  const sslStatus = ref(0)
  const forceHttps = ref(false)
  const certData = reactive({
    keyContent: '',
    certContent: ''
  })
  const deployDialogVisible = ref(false)
  const sslDeployInfo = ref<Api.Deploy.SslDeployInfoDTO | null>(null)

  const formatDate = (dateStr: string) => {
    const date = new Date(dateStr)
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
  }

  const {
    columns: certColumns,
    data: certTableData,
    loading: certLoading,
    pagination: certPagination,
    handleSizeChange: handleCertSizeChange,
    handleCurrentChange: handleCertCurrentChange
  } = useTable({
    core: {
      apiFn: fetchGetCertListByPage,
      apiParams: {
        current: 1,
        size: 10
      },
      columnsFactory: () => [
        {
          prop: 'sanDomains',
          label: '认证域名',
          minWidth: 150,
          formatter: (row: Api.Ssl.CertDTO) => row.sanDomains?.join(', ') || ''
        },
        {
          prop: 'issuer',
          label: '证书分类',
          minWidth: 100
        },
        {
          prop: 'issuer0',
          label: '品牌',
          minWidth: 100
        },
        {
          prop: 'notAfter',
          label: '到期时间',
          minWidth: 180,
          formatter: (row: Api.Ssl.CertDTO) => formatDate(row.notAfter)
        },
        {
          prop: 'operation',
          label: '操作',
          width: 180,
          formatter: (row: Api.Ssl.CertDTO) =>
            h('div', [
              h(ArtButtonTable, {
                type: 'text',
                text: '部署',
                onClick: () => handleCertDeploy(row)
              }),
              h(ArtButtonTable, {
                type: 'delete',
                text: '删除',
                onClick: () => handleCertDelete(row)
              })
            ])
        }
      ]
    }
  })

  watch(
    () => props.visible,
    async (newVal) => {
      dialogVisible.value = newVal
      if (newVal && props.proxyId) {
        await loadSslDeployInfo()
      }
    },
    { immediate: true }
  )

  const loadSslDeployInfo = async () => {
    try {
      const data = await fetchGetSslDeployInfo(props.proxyId)
      if (data) {
        sslDeployInfo.value = data
        sslStatus.value = data.enabled ? 1 : 0
        certData.keyContent = data.keyPem || ''
        certData.certContent = data.fullChainPem || ''
      } else {
        sslDeployInfo.value = null
        sslStatus.value = 0
        certData.keyContent = ''
        certData.certContent = ''
      }
    } catch (error) {
      sslDeployInfo.value = null
      sslStatus.value = 0
      certData.keyContent = ''
      certData.certContent = ''
    }
  }
  watch(dialogVisible, (newVal) => {
    emit('update:visible', newVal)
  })

  const handleClose = () => {
    dialogVisible.value = false
    emit('close')
  }

  const handleCertDeploy = (row: Api.Ssl.CertDTO) => {
    deployDialogVisible.value = true
  }

  const handleSaveAndDeploy = async () => {
    if (!certData.keyContent.trim()) {
      ElMessage.warning('请输入私钥(KEY)')
      return
    }
    if (!certData.certContent.trim()) {
      ElMessage.warning('请输入证书(PEM格式)')
      return
    }
    await fetchSaveAndDeployCert({
      proxyId: props.proxyId,
      key: certData.keyContent.trim(),
      fullChain: certData.certContent.trim()
    })
    ElMessage.success('证书已保存')
    await loadSslDeployInfo()
  }

  const handleCertDelete = async (row: Api.Ssl.CertDTO) => {
    try {
      await ElMessageBox.confirm('确定要删除该证书吗？', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      await fetchDeleteCert([row.id!])
      ElMessage.success('删除成功')
    } catch (error) {
      if (error === 'cancel') {
        return
      }
    }
  }

  const handleCloseSsl = async () => {
    try {
      await ElMessageBox.confirm('确定要关闭SSL证书吗？', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      await fetchCloseSsl(props.proxyId)
      ElMessage.success('SSL已关闭')
      sslStatus.value = 0
    } catch (error) {
      if (error === 'cancel') {
        return
      }
    }
  }

  const handleDownloadCurrentCert = async () => {
    const certId = sslDeployInfo.value?.certId
    if (!certId) {
      ElMessage.warning('当前没有可下载的证书')
      return
    }

    try {
      const blob = await fetchDownloadCert(certId)
      const fileName = `${sslDeployInfo.value?.sanDomains?.join('_') || 'cert'}.zip`
      downloadBlob(blob, fileName)
    } catch (error: any) {
      console.error('下载失败:', error)
      ElMessage.error(error?.message || '下载失败')
    }
  }
</script>

<style scoped>
  .ssl-dialog-content {
    min-height: calc(100vh - 180px);
  }

  .tab-content {
    display: flex;
    flex-direction: column;
    padding: 0 15px;
    height: 100%;
    box-sizing: border-box;
    gap: 16px;
  }

  .cert-folder-content {
    flex: 1;
    min-height: 300px;
  }

  .ssl-info {
    background-color: var(--art-gray-200);
    padding: 20px;
    border-radius: 4px;
    display: flex;
    gap: 48px;
    flex-shrink: 0;
  }

  .ssl-info--empty {
    justify-content: center;
    align-items: center;
    min-height: 80px;
  }

  .ssl-empty-text {
    color: var(--el-text-color-secondary);
    font-size: 14px;
  }

  .info-column {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 14px;
  }

  .info-item {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  .info-label {
    font-size: 14px;
    width: 70px;
    flex-shrink: 0;
    font-weight: 700;
  }

  .info-value {
    font-size: 14px;
    font-weight: 400;
  }

  .info-link {
    font-size: 13px;
    cursor: pointer;
    color: var(--el-color-primary);
  }

  .form-wrapper {
    display: flex;
    gap: 30px;
    height: 360px;
    flex-shrink: 0;
  }

  .form-actions {
    display: flex;
    justify-content: flex-start;
    padding: 8px 0;
    flex-shrink: 0;
  }

  .form-item {
    flex: 1;
    display: flex;
    flex-direction: column;
    min-height: 0;
  }

  .form-label {
    font-size: 14px;
    font-weight: 500;
    margin-bottom: 8px;
    flex-shrink: 0;
  }

  :deep(.el-textarea) {
    flex: 1;
  }

  :deep(.el-textarea__inner) {
    height: 100% !important;
  }
</style>
