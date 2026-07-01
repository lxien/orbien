<template>
  <div class="ssl-page">
    <ElCard>
      <template #header>
        <div class="card-header">
          <span>SSL证书</span>
          <ElButton type="primary" size="small">上传证书</ElButton>
        </div>
      </template>
      <ElTable :data="tableData" stripe>
        <ElTableColumn prop="domain" label="域名" min-width="200" />
        <ElTableColumn prop="issuer" label="颁发者" min-width="150" />
        <ElTableColumn prop="expireDate" label="到期日期" width="150" />
        <ElTableColumn prop="status" label="状态" width="100">
          <template #default="{ row }">
            <ElTag :type="row.status === '有效' ? 'success' : 'danger'" size="small">
              {{ row.status }}
            </ElTag>
          </template>
        </ElTableColumn>
        <ElTableColumn label="操作" width="150" fixed="right">
          <template #default>
            <ElButton type="primary" link size="small">查看</ElButton>
            <ElButton type="danger" link size="small">删除</ElButton>
          </template>
        </ElTableColumn>
      </ElTable>
    </ElCard>
  </div>
</template>

<script setup lang="ts">
  import { ref } from 'vue'

  defineOptions({ name: 'SslPage' })

  const tableData = ref([
    { domain: 'example.com', issuer: 'Let\'s Encrypt', expireDate: '2025-01-01', status: '有效' },
    { domain: 'test.com', issuer: 'DigiCert', expireDate: '2024-06-15', status: '有效' },
    { domain: 'demo.cn', issuer: 'Symantec', expireDate: '2024-03-20', status: '即将过期' }
  ])
</script>

<style lang="scss" scoped>
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
</style>