<template>
  <ElTable
      ref="tableRef"
      v-loading="loading"
      :data="domainOptions"
      row-key="proxyDomainId"
      max-height="260"
      border
      @selection-change="handleSelectionChange"
  >
    <ElTableColumn type="selection" width="48" :selectable="isDomainSelectable"/>
    <ElTableColumn prop="fullDomain" label="域名" show-overflow-tooltip/>
    <ElTableColumn prop="domainTypeLabel" label="类型">
      <template #default="{ row }">
        <ElTag
            v-if="row.domainType !== undefined"
            size="small"
            :type="resolveDomainTypeTag(row.domainType).type"
        >
          {{ row.domainTypeLabel || resolveDomainTypeTag(row.domainType).text }}
        </ElTag>
      </template>
    </ElTableColumn>
    <ElTableColumn label="证书状态">
      <template #default="{ row }">
        <span v-if="!row.selectable" class="text-muted">{{ row.unselectableReason }}</span>
        <span v-else-if="row.bound" class="text-warning">
          已绑定{{ row.boundCertIssuer ? `（${row.boundCertIssuer}）` : '' }}
        </span>
        <span v-else class="text-success">未绑定</span>
      </template>
    </ElTableColumn>
  </ElTable>
</template>

<script setup lang="ts">
import {nextTick, ref, watch} from 'vue'
import type {TableInstance} from 'element-plus'
import {getDomainTypeLabel} from '@/enums/orbien/business'

defineOptions({name: 'AcmeDomainSelectTable'})

const props = defineProps<{
  loading: boolean
  domainOptions: Api.AcmeOrder.HttpsProxyDomainOption[]
  modelValue: Api.AcmeOrder.HttpsProxyDomainOption[]
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: Api.AcmeOrder.HttpsProxyDomainOption[]): void
}>()

const tableRef = ref<TableInstance>()

const resolveDomainTypeTag = (domainType: number) => getDomainTypeLabel(domainType)

const isDomainSelectable = (row: Api.AcmeOrder.HttpsProxyDomainOption) => row.selectable

const handleSelectionChange = (rows: Api.AcmeOrder.HttpsProxyDomainOption[]) => {
  emit('update:modelValue', rows)
}

const clearSelection = async () => {
  await nextTick()
  tableRef.value?.clearSelection()
}

watch(
    () => props.domainOptions,
    () => {
      void clearSelection()
    }
)

defineExpose({clearSelection})
</script>

<style lang="scss">
@use './acme-apply-shared.scss';
</style>
