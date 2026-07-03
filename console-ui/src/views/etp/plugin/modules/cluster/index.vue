<template>
  <div class="cluster-page" v-loading="loading">
    <div class="mb-6">
      <h3 class="text-lg font-semibold mb-4">负载均衡</h3>
      <div class="flex items-center gap-3">
        <span class="w-20 font-medium shrink-0">策略</span>
        <ElSelect v-model="loadBalanceStrategy" placeholder="请选择策略" style="width: 240px">
          <ElOption
            v-for="item in LOAD_BALANCE_OPTIONS"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </ElSelect>
      </div>
    </div>

    <div>
      <div class="flex items-center justify-between mb-4">
        <h3 class="text-lg font-semibold">服务列表</h3>
        <ElButton type="primary" size="small" @click="addTarget">
          <template #icon>
            <Plus />
          </template>
          添加服务
        </ElButton>
      </div>

      <div class="border border-gray-200 rounded p-4">
        <ElEmpty v-if="targets.length === 0" description="暂无服务，请添加" :image-size="72">
          <ElButton type="primary" size="small" @click="addTarget">添加服务</ElButton>
        </ElEmpty>

        <ElTable v-else :data="targets" border style="width: 100%">
          <ElTableColumn prop="name" label="服务名称" min-width="140">
            <template #default="{ row }">
              <ElInput v-model="row.name" size="small" placeholder="可选" clearable />
            </template>
          </ElTableColumn>
          <ElTableColumn prop="host" label="主机" min-width="160">
            <template #default="{ row }">
              <ElInput v-model="row.host" size="small" placeholder="内网地址" clearable />
            </template>
          </ElTableColumn>
          <ElTableColumn prop="port" label="端口" width="110">
            <template #default="{ row }">
              <ElInput v-model.number="row.port" size="small" type="number" placeholder="1-65535" />
            </template>
          </ElTableColumn>
          <ElTableColumn prop="weight" label="权重" width="100">
            <template #default="{ row }">
              <ElInput
                v-model.number="row.weight"
                size="small"
                type="number"
                placeholder="权重"
                :disabled="loadBalanceStrategy !== LoadBalanceType.WEIGHT"
              />
            </template>
          </ElTableColumn>
          <ElTableColumn label="操作" width="80" align="center" fixed="right">
            <template #default="{ $index }">
              <ElButton link size="small" @click="removeTarget($index)">
                <template #icon>
                  <Delete />
                </template>
              </ElButton>
            </template>
          </ElTableColumn>
        </ElTable>
      </div>
    </div>

    <div class="mt-5">
      <ElButton type="primary" :loading="saving" @click="handleSave">保存配置</ElButton>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref, watch } from 'vue'
  import { ElMessage } from 'element-plus'
  import { Plus, Delete } from '@element-plus/icons-vue'
  import { fetchProxyDetail, saveProxyClusterConfig } from '@/api/proxy-plugin'
  import type { ProxyConfigProtocol } from '../../menus'
  import { LOAD_BALANCE_OPTIONS, LoadBalanceType } from '@/enums/etp/business'

  defineOptions({ name: 'ClusterPage' })

  interface TargetRow {
    host: string
    port: number | string
    weight: number
    name: string
  }

  const props = defineProps<{
    proxyId: string
    protocol: ProxyConfigProtocol
  }>()

  const loading = ref(false)
  const saving = ref(false)
  const loadBalanceStrategy = ref<LoadBalanceType>(LoadBalanceType.ROUND_ROBIN)
  const targets = ref<TargetRow[]>([])
  let detailSnapshot: Awaited<ReturnType<typeof fetchProxyDetail>> | null = null

  const toTargetRow = (target: Api.Proxy.TargetDTO): TargetRow => ({
    host: target.host || '',
    port: target.port || '',
    weight: target.weight || 1,
    name: target.name || ''
  })

  const normalizeWeights = () => {
    if (loadBalanceStrategy.value === LoadBalanceType.WEIGHT) return
    targets.value.forEach((row) => {
      row.weight = 1
    })
  }

  const loadData = async () => {
    loading.value = true
    try {
      const detail = await fetchProxyDetail(props.protocol, props.proxyId)
      detailSnapshot = detail
      loadBalanceStrategy.value =
        (detail.loadBalance?.strategy as LoadBalanceType | undefined) ?? LoadBalanceType.ROUND_ROBIN
      targets.value = detail.targets?.map(toTargetRow) || []
      normalizeWeights()
    } finally {
      loading.value = false
    }
  }

  watch(loadBalanceStrategy, normalizeWeights)

  watch(
    () => [props.proxyId, props.protocol] as const,
    ([proxyId]) => {
      if (proxyId) loadData()
    },
    { immediate: true }
  )

  const addTarget = () => {
    targets.value.push({ host: '127.0.0.1', port: '', weight: 1, name: '' })
  }

  const removeTarget = (index: number) => {
    targets.value.splice(index, 1)
  }

  const validateTargets = () => {
    if (targets.value.length === 0) {
      ElMessage.warning('请至少添加一个服务')
      return false
    }

    for (let i = 0; i < targets.value.length; i++) {
      const { host, port } = targets.value[i]
      if (!host?.trim()) {
        ElMessage.warning(`第 ${i + 1} 行：请输入主机地址`)
        return false
      }
      const portNum = Number(port)
      if (!portNum || portNum < 1 || portNum > 65535) {
        ElMessage.warning(`第 ${i + 1} 行：请输入有效端口（1-65535）`)
        return false
      }
    }

    return true
  }

  const handleSave = async () => {
    if (!detailSnapshot || !validateTargets()) return

    saving.value = true
    try {
      await saveProxyClusterConfig(
        props.protocol,
        detailSnapshot,
        targets.value.map((row) => ({
          host: row.host.trim(),
          port: Number(row.port),
          weight: row.weight || 1,
          name: row.name?.trim() || row.host.trim()
        })),
        { strategy: loadBalanceStrategy.value }
      )
      await loadData()
    } finally {
      saving.value = false
    }
  }
</script>
