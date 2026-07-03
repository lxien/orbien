<template>
  <div class="dns-credential-panel">
    <ArtTableHeader :loading="loading" @refresh="loadData">
      <template #left>
        <ElButton type="primary" @click="handleAdd" v-ripple>添加密钥</ElButton>
      </template>
    </ArtTableHeader>

    <ArtTable :loading="loading" :data="data" :columns="columns" :show-pagination="false" />

    <DnsCredentialDialog
      v-model:visible="dialogVisible"
      :record="currentRecord"
      @submit="loadData"
    />
  </div>
</template>

<script setup lang="ts">
  import { h, onMounted, ref } from 'vue'
  import { ElMessage, ElMessageBox, ElTag } from 'element-plus'
  import ArtButtonTable from '@/components/core/forms/art-button-table/index.vue'
  import DnsCredentialDialog from './dns-credential-dialog.vue'
  import {
    fetchDeleteDnsCredential,
    fetchDnsCredentialList,
    fetchTestDnsCredential
  } from '@/api/dns-credential'
  import { resolveDnsCredentialStatusTagType } from '@/utils/ui/status-tag'

  defineOptions({ name: 'DnsCredentialPanel' })

  const loading = ref(false)
  const data = ref<Api.DnsCredential.CredentialDTO[]>([])
  const dialogVisible = ref(false)
  const currentRecord = ref<Api.DnsCredential.CredentialDTO | null>(null)

  const statusTag = (status: number) => {
    const type = resolveDnsCredentialStatusTagType(status)
    const label = status === 1 ? '正常' : status === 2 ? '无效' : '未测试'
    return h(ElTag, { type, size: 'small' }, () => label)
  }

  const columns = [
    { prop: 'name', label: '名称', minWidth: 140 },
    { prop: 'providerLabel', label: '厂商', width: 120 },
    { prop: 'accountHint', label: '账号标识', minWidth: 160 },
    {
      prop: 'status',
      label: '状态',
      width: 90,
      formatter: (row: Api.DnsCredential.CredentialDTO) => statusTag(row.status)
    },
    { prop: 'lastTestAt', label: '最近测试', width: 170 },
    { prop: 'lastTestMessage', label: '测试信息', minWidth: 140 },
    {
      prop: 'operation',
      label: '操作',
      width: 180,
      fixed: 'right' as const,
      formatter: (row: Api.DnsCredential.CredentialDTO) =>
        h('div', [
          h(ArtButtonTable, {
            type: 'link',
            text: '测试',
            onClick: () => handleTest(row)
          }),
          h(ArtButtonTable, {
            type: 'link',
            text: '编辑',
            onClick: () => handleEdit(row)
          }),
          h(ArtButtonTable, {
            type: 'link',
            text: '删除',
            onClick: () => handleDelete(row)
          })
        ])
    }
  ]

  const loadData = async () => {
    loading.value = true
    try {
      data.value = await fetchDnsCredentialList()
    } finally {
      loading.value = false
    }
  }

  const handleAdd = () => {
    currentRecord.value = null
    dialogVisible.value = true
  }

  const handleEdit = (row: Api.DnsCredential.CredentialDTO) => {
    currentRecord.value = row
    dialogVisible.value = true
  }

  const handleTest = async (row: Api.DnsCredential.CredentialDTO) => {
    try {
      await fetchTestDnsCredential(row.id)
      ElMessage.success('连接测试成功')
      loadData()
    } catch {
      loadData()
    }
  }

  const handleDelete = async (row: Api.DnsCredential.CredentialDTO) => {
    try {
      await ElMessageBox.confirm(`确定删除密钥「${row.name}」吗？`, '删除确认', { type: 'warning' })
      await fetchDeleteDnsCredential(row.id)
      ElMessage.success('删除成功')
      loadData()
    } catch (error) {
      if (error === 'cancel') return
    }
  }

  onMounted(loadData)

  defineExpose({ loadData })
</script>
