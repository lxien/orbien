<template>
  <div class="domain-page">
    <ElCard>
      <template #header>
        <div class="card-header">
          <span>域名管理</span>
          <ElButton type="primary" size="small">新增域名</ElButton>
        </div>
      </template>
      <ElTable :data="tableData" stripe>
        <ElTableColumn prop="domain" label="域名" min-width="200" />
        <ElTableColumn prop="type" label="类型" width="100" />
        <ElTableColumn prop="status" label="状态" width="100">
          <template #default="{ row }">
            <ElTag :type="row.status === '启用' ? 'success' : 'info'" size="small">
              {{ row.status }}
            </ElTag>
          </template>
        </ElTableColumn>
        <ElTableColumn prop="createTime" label="创建时间" width="180" />
        <ElTableColumn label="操作" width="150" fixed="right">
          <template #default>
            <ElButton type="primary" link size="small">编辑</ElButton>
            <ElButton type="danger" link size="small">删除</ElButton>
          </template>
        </ElTableColumn>
      </ElTable>
    </ElCard>
  </div>
</template>

<script setup lang="ts">
  import { ref } from 'vue'

  defineOptions({ name: 'DomainPage' })

  const tableData = ref([
    { domain: 'example.com', type: '子域名', status: '启用', createTime: '2024-01-01 10:00:00' },
    { domain: 'test.com', type: '自定义', status: '启用', createTime: '2024-01-02 11:00:00' },
    { domain: 'demo.cn', type: '子域名', status: '禁用', createTime: '2024-01-03 12:00:00' }
  ])
</script>

<style lang="scss" scoped>
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
</style>