<template>
  <ElDialog v-model="dialogVisible" title="部署证书" width="60%" align-center>
    <div>
      <ArtTable
        ref="tableRef"
        rowKey="id"
        :loading="loading"
        :data="data"
        :columns="columns"
        :pagination="pagination"
        highlight-current-row
        @row-click="handleRowClick"
        @pagination:size-change="handleSizeChange"
        @pagination:current-change="handlePaginationChange"
      />
    </div>

    <template #footer>
      <div class="dialog-footer">
        <ElButton @click="handleCancel">取消</ElButton>
        <ElButton type="primary" @click="handleSubmit" :disabled="!selectedRow">
          部署
        </ElButton>
      </div>
    </template>
  </ElDialog>
</template>

<script setup lang="ts">
  import { ref, computed, watch, h } from 'vue'
  import { useTable } from '@/hooks/core/useTable'
  import { ElMessage, ElTag, ElSpace, ElMessageBox } from 'element-plus'
  import ArtTable from '@/components/core/tables/art-table/index.vue'
  import { fetchGetHttpsProxyList } from '@/api/proxy'
  import { fetchDeployCert } from '@/api/deploy'

  defineOptions({ name: 'DeployDialog' })

  interface Props {
    visible: boolean
    certId: number | null
  }

  interface Emits {
    (e: 'update:visible', value: boolean): void
    (e: 'submit'): void
  }

  const props = withDefaults(defineProps<Props>(), {
    certId: null
  })
  const emit = defineEmits<Emits>()

  const dialogVisible = computed({
    get: () => props.visible,
    set: (value) => emit('update:visible', value)
  })
  const tableRef = ref<InstanceType<typeof ArtTable> | null>(null)
  const selectedRow = ref<Api.Proxy.HttpsProxyListDTO | null>(null)

  watch(dialogVisible, async (newVal) => {
    if (newVal) {
      selectedRow.value = null
      await getData()
    }
  })

  const handleRowClick = (row: Api.Proxy.HttpsProxyListDTO) => {
    selectedRow.value = row
    const elTable = tableRef.value?.elTableRef
    if (elTable) {
      elTable.setCurrentRow(row)
    }
  }

  const getRemoteAddr = (row: Api.Proxy.HttpsProxyListDTO) => {
    if (!row.domains || row.domains.length === 0) {
      return ''
    }
    return h(ElSpace, { direction: 'horizontal', size: 4, wrap: true }, () =>
      row.domains.map((domain) => {
        return h(
          ElTag,
          {
            type: 'primary'
          },
          () => domain
        )
      })
    )
  }

  const {
    columns,
    data,
    loading,
    pagination,
    handleSizeChange,
    handleCurrentChange: paginationCurrentChange,
    getData
  } = useTable({
    core: {
      apiFn: fetchGetHttpsProxyList,
      apiParams: {
        current: 1,
        size: 10
      },
      immediate: false,
      columnsFactory: () => [
        { type: 'index', label: '序号', width: 60 },
        {
          prop: 'name',
          label: '隧道名称',
          minWidth: 150
        },
        {
          prop: 'domains',
          label: '域名',
          minWidth: 200,
          formatter: (row: Api.Proxy.HttpsProxyListDTO) => getRemoteAddr(row)
        }
      ]
    }
  })

  const handlePaginationChange = (val: number) => {
    selectedRow.value = null
    paginationCurrentChange(val)
  }

  const handleCancel = () => {
    dialogVisible.value = false
    emit('update:visible', false)
  }

  const handleSubmit = async () => {
    if (!props.certId) {
      ElMessage.warning('请选择证书')
      return
    }
    if (!selectedRow.value) {
      ElMessage.warning('请选择代理')
      return
    }

    await ElMessageBox.confirm('确认部署该证书？', '部署确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const result = await fetchDeployCert({
      certId: String(props.certId),
      proxyIds: [selectedRow.value.id]
    })

    ElMessage.success(`证书部署成功`)
    dialogVisible.value = false
    emit('submit')
  }
</script>

<style scoped>
</style>
