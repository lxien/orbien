<template>
  <ArtTableHeader v-model:columns="columnChecks" :loading="loading" @refresh="refreshData"/>

  <ArtTable
      :loading="loading"
      :data="data"
      :columns="columns"
      :pagination="pagination"
      @pagination:size-change="handleSizeChange"
      @pagination:current-change="handleCurrentChange"
  />
</template>

<script setup lang="ts">
import {h} from 'vue'
import {useRouter} from 'vue-router'
import {ElTag} from 'element-plus'
import ArtButtonTable from '@/components/core/forms/art-button-table/index.vue'
import {useTable} from '@/hooks/core/useTable'
import {getDomainTypeLabel} from '@/enums/orbien/business'
import {fetchGetUsedDomainListByPage} from '@/api/domain'
import {resolveProxyListRoute} from '@/views/orbien/proxy/shared/resolve-proxy-list-route'

defineOptions({name: 'UsedDomainList'})

const emit = defineEmits<{ change: [] }>()
const router = useRouter()

type UsedDomainItem = Api.Domain.UsedDomainDTO

const renderProxyLink = (row: UsedDomainItem) => {
  const label = row.proxyName || row.proxyId || ''
  if (!label) return ''

  const route = resolveProxyListRoute(row.protocol)
  if (!route) return label

  return h(ArtButtonTable, {
    type: 'link',
    text: label,
    onClick: () => router.push(route)
  })
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
    apiFn: fetchGetUsedDomainListByPage,
    apiParams: {
      current: 1,
      size: 20
    },
    columnsFactory: () => [
      {
        prop: 'fullDomain',
        label: '完整域名'
      },
      {
        prop: 'domainType',
        label: '域名类型',
        formatter: (row: UsedDomainItem) => {
          const config = getDomainTypeLabel(row.domainType)
          return h(ElTag, {type: config.type, size: 'small'}, () => config.text)
        }
      },
      {
        prop: 'proxyName',
        label: '关联代理',
        formatter: (row: UsedDomainItem) => renderProxyLink(row)
      },
      {
        prop: 'rootDomain',
        label: '根域名',
        formatter: (row: UsedDomainItem) => row.rootDomain || ''
      }
    ]
  },
  hooks: {
    onSuccess: () => emit('change')
  }
})
</script>
