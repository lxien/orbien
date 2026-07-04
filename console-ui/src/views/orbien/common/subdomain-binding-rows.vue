<template>
  <div class="subdomain-bindings">
    <div v-for="(row, index) in rows" :key="row.rowKey" class="subdomain-row">
      <ElInput
        :model-value="row.prefix"
        placeholder="前缀"
        clearable
        :validate-event="false"
        :class="['subdomain-row__prefix', { 'subdomain-row__prefix--error': isPrefixError(index) }]"
        @update:model-value="(value) => updateRow(index, { prefix: value })"
      />
      <ElSelect
        :model-value="row.rootDomainId"
        placeholder="根域名"
        :loading="loading"
        filterable
        :validate-event="false"
        class="subdomain-row__root"
        @update:model-value="(value) => updateRow(index, { rootDomainId: normalizeRootDomainId(value) })"
      >
        <ElOption v-for="item in rootDomains" :key="item.id" :label="item.domain" :value="item.id" />
      </ElSelect>
      <div class="subdomain-row__actions">
        <ElButton v-if="index > 0" link @click="removeRow(index)">删除</ElButton>
        <ElButton v-if="index === rows.length - 1" link type="primary" @click="addRow">添加</ElButton>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { computed, ref, watch } from 'vue'
  import {
    normalizeRootDomainId
  } from '@/views/orbien/common/use-root-domain-options'

  defineOptions({ name: 'SubdomainBindingRows' })

  interface SubdomainBindingRow extends Api.Proxy.SubdomainBindingParam {
    rowKey: number
  }

  interface Props {
    modelValue: Api.Proxy.SubdomainBindingParam[]
    rootDomains: Api.Domain.DomainDTO[]
    loading?: boolean
    errorIndexes?: number[]
  }

  interface Emits {
    (e: 'update:modelValue', value: Api.Proxy.SubdomainBindingParam[]): void
    (e: 'clear-error'): void
  }

  const props = withDefaults(defineProps<Props>(), {
    loading: false,
    errorIndexes: () => []
  })
  const emit = defineEmits<Emits>()

  let rowKeySeed = 0
  const rowKeys = ref<number[]>([])

  const nextRowKey = () => ++rowKeySeed

  const ensureRowKeys = (length: number) => {
    if (rowKeys.value.length === length) {
      return
    }
    if (rowKeys.value.length < length) {
      while (rowKeys.value.length < length) {
        rowKeys.value.push(nextRowKey())
      }
      return
    }
    rowKeys.value = rowKeys.value.slice(0, length)
  }

  const resetRowKeys = (length: number) => {
    rowKeys.value = Array.from({ length }, () => nextRowKey())
  }

  const toInternalRows = (bindings: Api.Proxy.SubdomainBindingParam[]): SubdomainBindingRow[] =>
    bindings.map((row, index) => ({
      rowKey: rowKeys.value[index] ?? nextRowKey(),
      rootDomainId: normalizeRootDomainId(row.rootDomainId),
      prefix: row.prefix ?? ''
    }))

  watch(
    () => props.modelValue.length,
    (length, previousLength) => {
      if (previousLength == null) {
        resetRowKeys(length)
        return
      }
      if (length > previousLength) {
        ensureRowKeys(length)
        return
      }
      if (length < previousLength) {
        rowKeys.value = rowKeys.value.slice(0, length)
      }
    },
    { immediate: true }
  )

  const rows = computed<SubdomainBindingRow[]>(() => toInternalRows(props.modelValue))

  const errorIndexSet = computed(() => new Set(props.errorIndexes))

  const isPrefixError = (index: number) => errorIndexSet.value.has(index)

  const emitRows = (nextRows: Api.Proxy.SubdomainBindingParam[]) => {
    emit('update:modelValue', nextRows)
  }

  const updateRow = (index: number, patch: Partial<Api.Proxy.SubdomainBindingParam>) => {
    emit('clear-error')
    const nextRows = props.modelValue.map((row, rowIndex) =>
      rowIndex === index
        ? {
            ...row,
            ...patch,
            rootDomainId:
              patch.rootDomainId !== undefined
                ? normalizeRootDomainId(patch.rootDomainId)
                : normalizeRootDomainId(row.rootDomainId),
            prefix: patch.prefix !== undefined ? patch.prefix : row.prefix
          }
        : { ...row }
    )
    emitRows(nextRows)
  }

  const createRow = (): Api.Proxy.SubdomainBindingParam => ({
    rootDomainId: normalizeRootDomainId(props.rootDomains[0]?.id),
    prefix: ''
  })

  const addRow = () => {
    emit('clear-error')
    ensureRowKeys(props.modelValue.length + 1)
    rowKeys.value.push(nextRowKey())
    emitRows([...props.modelValue, createRow()])
  }

  const removeRow = (index: number) => {
    if (index === 0) {
      return
    }
    emit('clear-error')
    rowKeys.value.splice(index, 1)
    emitRows(props.modelValue.filter((_, rowIndex) => rowIndex !== index))
  }
</script>

<style scoped lang="scss">
  .subdomain-bindings {
    width: 100%;
  }

  .subdomain-row {
    display: flex;
    gap: 8px;
    align-items: center;
    margin-bottom: 8px;

    &:last-child {
      margin-bottom: 0;
    }
  }

  .subdomain-row__prefix {
    width: 120px;
    flex-shrink: 0;

    &--error :deep(.el-input__wrapper) {
      box-shadow: 0 0 0 1px var(--el-color-danger) inset;
    }

    &--error :deep(.el-input__wrapper:hover),
    &--error :deep(.el-input__wrapper.is-focus) {
      box-shadow: 0 0 0 1px var(--el-color-danger) inset;
    }
  }

  .subdomain-row__root {
    width: 180px;
    flex-shrink: 0;
  }

  .subdomain-row__actions {
    display: flex;
    gap: 4px;
    flex-shrink: 0;
    margin-left: 4px;

    :deep(.el-button) {
      padding: 0 4px;
      height: auto;
    }
  }
</style>
