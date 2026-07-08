<template>
  <div class="tls-page art-full-height">
    <ElCard class="art-table-card tls-table-card">
      <ElTabs v-model="activeTab" class="tls-tabs" type="card">
        <ElTabPane label="证书列表" name="certs">
          <div class="tab-panel-content">
            <ArtTableHeader v-model:columns="columnChecks" :loading="loading" @refresh="refreshData">
              <template #left>
                <ElSpace wrap>
                  <ElButton type="primary" @click="wizardVisible = true" v-ripple>免费申请</ElButton>
                  <ElButton v-ripple @click="handleAdd">上传证书</ElButton>
                  <ElButton @click="handleBatchDelete" v-ripple :disabled="selectedRows.length === 0">
                    批量删除
                  </ElButton>
                </ElSpace>
              </template>
            </ArtTableHeader>

            <ArtTable
                :loading="loading"
                :data="data"
                :columns="columns"
                :pagination="pagination"
                @selection-change="handleSelectionChange"
                @pagination:size-change="handleSizeChange"
                @pagination:current-change="handleCurrentChange"
            />
          </div>
        </ElTabPane>

        <ElTabPane label="申请记录" name="orders">
          <div class="tab-panel-content">
            <AcmeOrderPanel ref="orderPanelRef" @apply="wizardVisible = true"/>
          </div>
        </ElTabPane>

        <ElTabPane label="DNS 密钥" name="dns">
          <div class="tab-panel-content">
            <DnsCredentialPanel ref="dnsPanelRef"/>
          </div>
        </ElTabPane>
      </ElTabs>
    </ElCard>

    <TlsDialog v-model:visible="dialogVisible" @submit="handleUploadSubmit"/>
    <BindDialog v-model:visible="bindDialogVisible" :cert-id="currentCertId" @submit="handleBindSubmit"/>
    <AcmeApplyWizard
        v-model:visible="wizardVisible"
        @success="handleApplySuccess"
    />
  </div>
</template>

<script setup lang="ts">
import {ref, h} from 'vue'
import {useTable} from '@/hooks/core/useTable'
import {ElMessage, ElMessageBox, ElTag, ElSwitch} from 'element-plus'
import TlsDialog from './modules/tls-dialog.vue'
import BindDialog from './modules/bind-dialog.vue'
import AcmeOrderPanel from './modules/acme-order-panel.vue'
import DnsCredentialPanel from './modules/dns-credential-panel.vue'
import AcmeApplyWizard from './modules/acme-apply-wizard.vue'
import {fetchGetCertListByPage, fetchDownloadCert, fetchDeleteCert, fetchUpdateCertAutoRenew} from '@/api/tls'
import ArtButtonTable from '@/components/core/forms/art-button-table/index.vue'
import {downloadBlob} from '@/utils/download'

defineOptions({name: 'TlsManagement'})

type TlsItem = Api.Tls.CertDTO

const activeTab = ref('certs')
const selectedRows = ref<TlsItem[]>([])
const dialogVisible = ref(false)
const bindDialogVisible = ref(false)
const wizardVisible = ref(false)
const currentCertId = ref<string | null>(null)
const orderPanelRef = ref<InstanceType<typeof AcmeOrderPanel>>()
const dnsPanelRef = ref<InstanceType<typeof DnsCredentialPanel>>()
const renewingCertId = ref<string | null>(null)

const sourceLabel = (source?: number) => {
  if (source === 2) {
    return h(ElTag, {type: 'primary', size: 'small'}, () => 'ACME')
  }
  return h(ElTag, {type: 'warning', size: 'small'}, () => '手动')
}

const getExpireDays = (item: TlsItem) => {
  const now = new Date()
  const notAfter = new Date(item.notAfter)
  if (now > notAfter) {
    return h('span', {style: {color: 'var(--el-color-danger)'}}, '已过期')
  }
  const diffTime = notAfter.getTime() - now.getTime()
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))
  return h('span', {style: {color: 'var(--el-color-primary)'}}, `剩余${diffDays}天`)
}

const {
  columns,
  columnChecks,
  data,
  loading,
  pagination,
  handleSizeChange,
  handleCurrentChange,
  refreshData
} = useTable({
  core: {
    apiFn: fetchGetCertListByPage,
    apiParams: {
      current: 1,
      size: 10
    },
    columnsFactory: () => [
      {type: 'selection'},
      {
        prop: 'sanDomains',
        label: '认证域名',
        minWidth: 120,
        formatter: (row: TlsItem) => row.sanDomains?.join(', ') || ''
      },
      {
        prop: 'source',
        label: '来源',
        width: 90,
        formatter: (row: TlsItem) => sourceLabel(row.source)
      },
      {
        prop: 'org',
        label: '证书分类',
        minWidth: 100
      },
      {
        prop: 'issuer',
        label: '证书品牌',
        minWidth: 120
      },
      {
        prop: 'boundDomainCount',
        label: '使用域名数',
        width: 100,
        formatter: (row: TlsItem) => row.boundDomainCount ?? 0
      },
      {
        prop: 'notAfter',
        label: '到期时间',
        minWidth: 110,
        formatter: (row: TlsItem) => getExpireDays(row)
      },
      {
        prop: 'autoRenew',
        label: '自动续签',
        width: 100,
        formatter: (row: TlsItem) => {
          if (row.source !== 2) {
            return ''
          }
          const now = new Date()
          const notAfter = new Date(row.notAfter)
          if (now > notAfter) {
            return ''
          }
          return h(ElSwitch, {
            modelValue: row.autoRenew ?? false,
            size: 'small',
            loading: renewingCertId.value === row.id,
            onChange: (value: string | number | boolean) => handleAutoRenewChange(row, Boolean(value))
          })
        }
      },
      {
        prop: 'operation',
        label: '操作',
        width: 150,
        fixed: 'right',
        formatter: (row: TlsItem) => {
          const now = new Date()
          const notAfter = new Date(row.notAfter)
          const isExpired = now > notAfter
          const children = []
          if (!isExpired) {
            children.push(
                h(ArtButtonTable, {
                  type: 'link',
                  text: '绑定',
                  onClick: () => handleBind(row)
                }),
                h(ArtButtonTable, {
                  type: 'link',
                  text: '下载',
                  onClick: () => handleDownload(row)
                })
            )
          }
          children.push(
              h(ArtButtonTable, {
                type: 'link',
                text: '删除',
                onClick: () => handleDelete(row)
              })
          )
          return h('div', children)
        }
      }
    ]
  }
})

const handleSelectionChange = (selection: TlsItem[]): void => {
  selectedRows.value = selection
}

const handleAdd = () => {
  dialogVisible.value = true
}

const handleUploadSubmit = () => {
  refreshData()
}

const handleBindSubmit = () => {
  refreshData()
}

const handleApplySuccess = () => {
  activeTab.value = 'orders'
  orderPanelRef.value?.refreshData()
  refreshData()
}

const handleBatchDelete = async () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请选择要删除的证书')
    return
  }

  try {
    await ElMessageBox.confirm('确定要删除选中的证书吗？', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const ids = selectedRows.value.map((row) => row.id)
    await fetchDeleteCert(ids)
    ElMessage.success('删除成功')
    refreshData()
  } catch (error) {
    if (error === 'cancel') return
  }
}

const handleBind = (row: TlsItem) => {
  currentCertId.value = row.id || null
  bindDialogVisible.value = true
}

const handleAutoRenewChange = async (row: TlsItem, autoRenew: boolean) => {
  if (!row.id) return
  renewingCertId.value = row.id
  try {
    const result = await fetchUpdateCertAutoRenew(row.id, autoRenew)
    row.autoRenew = autoRenew
    if (autoRenew && result.acmeRenewJobAutoEnabled) {
      ElMessage.success('已开启自动续签，并已自动启用 ACME 续签计划任务')
    } else {
      ElMessage.success(autoRenew ? '已开启自动续签' : '已关闭自动续签')
    }
  } catch {
    row.autoRenew = !autoRenew
  } finally {
    renewingCertId.value = null
  }
}

const handleDownload = async (row: TlsItem) => {
  try {
    const blob = await fetchDownloadCert(row.id)
    const fileName = `${row.sanDomains?.join('_') || 'cert'}.zip`
    downloadBlob(blob, fileName)
  } catch (error: any) {
    ElMessage.error(error?.message || '下载失败')
  }
}

const handleDelete = async (row: TlsItem) => {
  try {
    await ElMessageBox.confirm('确定要删除该证书吗？', '证书删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await fetchDeleteCert([row.id])
    ElMessage.success('删除成功')
    refreshData()
  } catch (error) {
    if (error === 'cancel') return
  }
}
</script>

<style lang="scss" scoped>
.tls-table-card {
  :deep(.el-card__body) {
    display: flex;
    flex: 1;
    flex-direction: column;
    min-height: 0;
    overflow: hidden;
  }
}

.tls-tabs {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;

  :deep(.el-tabs__header) {
    flex-shrink: 0;
    margin-bottom: 16px;
  }

  :deep(.el-tabs__content) {
    flex: 1;
    min-height: 0;
  }

  :deep(.el-tab-pane) {
    height: 100%;
  }
}

.tab-panel-content {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;

  :deep(.acme-order-panel),
  :deep(.dns-credential-panel) {
    display: flex;
    flex: 1;
    flex-direction: column;
    min-height: 0;
  }

  :deep(.art-table) {
    flex: 1;
    min-height: 0;
  }
}

:deep(.el-dialog__body) {
  padding: 0 !important;
}
</style>
