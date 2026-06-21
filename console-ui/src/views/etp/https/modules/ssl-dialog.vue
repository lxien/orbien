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
            <div class="ssl-info">
              <div class="info-column">
                <div class="info-item">
                  <span class="info-label">证书分类</span>
                  <span class="info-value">自签证书</span>
                </div>
                <div class="info-item">
                  <span class="info-label">认证域名</span>
                  <span class="info-value"></span>
                </div>
              </div>
              <div class="info-column">
                <div class="info-item">
                  <span class="info-label">证书品牌</span>
                  <span class="info-value">localhost</span>
                </div>
                <div class="info-item">
                  <span class="info-label">到期时间</span>
                  <span class="info-value">2027-06-20</span>
                </div>
              </div>
            </div>
            <div class="form-wrapper">
              <div class="form-item">
                <div class="form-label">密钥(KEY)</div>
                <ElInput v-model="certData.keyContent" type="textarea" resize="none" />
              </div>

              <div class="form-item">
                <div class="form-label">证书(PEM格式)</div>
                <ElInput v-model="certData.certContent" type="textarea" resize="none" />
              </div>
            </div>
            <div class="form-actions">
              <ElSpace v-if="sslStatus !== 1">
                <ElButton type="primary">保存并启用证书</ElButton>
                <ElButton>下载证书</ElButton>
              </ElSpace>
              <ElSpace v-else>
                <ElButton type="primary">保存</ElButton>
                <ElButton>下载证书</ElButton>
                <ElButton>关闭SSL</ElButton>
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
  import { fetchGetCertListByPage } from '@/api/ssl'

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
          minWidth: 150
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
          width: 150,
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
    (newVal) => {
      dialogVisible.value = newVal
    },
    { immediate: true }
  )
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

  const handleCertDelete = async (row: Api.Ssl.CertDTO) => {
    try {
      await ElMessageBox.confirm('确定要删除该证书吗？', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      ElMessage.success('删除成功')
    } catch (error) {
      if (error !== 'cancel') {
        console.error('删除失败:', error)
      }
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
