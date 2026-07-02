<template>
  <ElDialog v-model="dialogVisible" title="客户端详情" width="60%" align-center>
    <div v-if="localClientData" class="mt-5">
      <ElDescriptions :column="2" border>
        <ElDescriptionsItem label="客户端名称">{{ localClientData.name }}</ElDescriptionsItem>
        <ElDescriptionsItem label="访问令牌">{{ localClientData.token }}</ElDescriptionsItem>
        <ElDescriptionsItem label="状态">
          <ElTag :type="getStatusType(localClientData.isOnline)">
            {{ getStatusText(localClientData.isOnline) }}
          </ElTag>
        </ElDescriptionsItem>
        <ElDescriptionsItem label="类型">{{
          getAgentTypeLabel(localClientData.agentType)
        }}</ElDescriptionsItem>
        <ElDescriptionsItem label="操作系统">{{ localClientData.os }}</ElDescriptionsItem>
        <ElDescriptionsItem label="架构">{{ localClientData.arch }}</ElDescriptionsItem>
        <ElDescriptionsItem label="版本">{{ localClientData.version }}</ElDescriptionsItem>
        <ElDescriptionsItem label="最后在线时间">{{
          formatDate(localClientData.lastActiveTime)
        }}</ElDescriptionsItem>
        <ElDescriptionsItem label="创建时间">{{
          formatDate(localClientData.createdAt)
        }}</ElDescriptionsItem>
        <ElDescriptionsItem label="更新时间">{{
          formatDate(localClientData.updatedAt)
        }}</ElDescriptionsItem>
      </ElDescriptions>
    </div>
    <div v-else class="mt-5">
      <ElSkeleton :rows="10" animated />
    </div>
    <template #footer>
      <div class="dialog-footer">
        <ElButton @click="dialogVisible = false">关闭</ElButton>
      </div>
    </template>
  </ElDialog>
</template>

<script setup lang="ts">
  import { ref, computed, watch, nextTick } from 'vue'
  import { ElTag, ElDescriptions, ElSkeleton, ElButton, ElMessage } from 'element-plus'
  import { fetchGetAgentById } from '@/api/agent'
  import { getAgentTypeLabel } from '@/enums/etp/business'

  interface Props {
    visible: boolean
    clientData?: Api.Agent.AgentDTO
  }

  interface Emits {
    (e: 'update:visible', value: boolean): void
  }

  const props = defineProps<Props>()
  const emit = defineEmits<Emits>()

  // 对话框显示控制
  const dialogVisible = computed({
    get: () => props.visible,
    set: (value) => emit('update:visible', value)
  })

  // 客户端数据
  const localClientData = ref<Api.Agent.AgentDTO | null>(null)
  const loading = ref(false)

  /**
   * 获取客户端详情
   */
  const fetchClientDetail = async () => {
    if (!props.clientData?.id) return

    loading.value = true
    try {
      const data = await fetchGetAgentById(props.clientData.id)
      localClientData.value = data
    } catch (error) {
      console.error('获取客户端详情失败:', error)
      ElMessage.error('获取客户端详情失败')
    } finally {
      loading.value = false
    }
  }

  /**
   * 获取状态类型
   */
  const getStatusType = (isOnline: boolean) => {
    return isOnline ? 'primary' : 'info'
  }

  /**
   * 获取状态文本
   */
  const getStatusText = (isOnline: boolean) => {
    return isOnline ? '在线' : '离线'
  }

  /**
   * 格式化日期
   */
  const formatDate = (dateString: string) => {
    if (!dateString) return ''
    const date = new Date(dateString)
    return date.toLocaleString()
  }

  /**
   * 监听对话框状态变化
   */
  watch(
    () => props.visible,
    (visible) => {
      if (visible) {
        nextTick(() => {
          fetchClientDetail()
        })
      }
    }
  )

  /**
   * 监听客户端数据变化
   */
  watch(
    () => props.clientData,
    () => {
      if (props.visible) {
        fetchClientDetail()
      }
    }
  )
</script>
