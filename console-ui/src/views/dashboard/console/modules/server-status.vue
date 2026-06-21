<!--
  -    Copyright 2026 xiaoniucode
  -
  -    Licensed under the Apache License, Version 2.0 (the "License");
  -    you may not use this file except in compliance with the License.
  -    You may obtain a copy of the License at
  -
  -        http://www.apache.org/licenses/LICENSE-2.0
  -
  -    Unless required by applicable law or agreed to in writing, software
  -    distributed under the License is distributed on an "AS IS" BASIS,
  -    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  -    See the License for the specific language governing permissions and
  -    limitations under the License.
  -->

<template>
  <ElRow :gutter="20" class="flex">
    <ElCol v-for="(item, index) in statusData" :key="index" :sm="12" :md="6" :lg="6">
      <div class="art-card relative h-[168px] px-5 py-5 mb-5 max-sm:mb-4 overflow-hidden">
        <div class="ring-chart absolute top-2 right-3">
          <ArtRingChart
            :data="item.ringData"
            :radius="['75%', '88%']"
            :centerText="item.percentage + '%'"
            :colors="[item.color, '#E8E8E8']"
            :showTooltip="false"
            :showLabel="false"
            :borderRadius="0"
            :roundCap="true"
            height="128px"
          />
        </div>

        <div class="absolute left-5 bottom-5">
          <div class="text-sm mb-1" style="color: #303133; font-weight: 900">{{ item.label }}</div>
          <div class="font-bold mb-1" style="font-size: 17px">
            <span :style="{ color: item.label === 'CPU' ? '#303133' : item.color }">{{
              item.usedValue || item.value
            }}</span>
            <span v-if="item.totalValue" style="color: #303133"> / {{ item.totalValue }}</span>
          </div>
          <div class="text-xs text-g-500">{{ item.desc }}</div>
        </div>
      </div>
    </ElCol>
  </ElRow>
</template>

<script setup lang="ts">
  import { ref, computed, onMounted } from 'vue'
  import { ElRow, ElCol } from 'element-plus'
  import ArtRingChart from '@/components/core/charts/art-ring-chart/index.vue'
  import { fetchGetServerInfo } from '@/api/monitor'

  defineOptions({ name: 'ServerStatusCards' })

  const serverInfo = ref<Api.Monitor.ServerInfo | null>(null)

  const statusData = computed(() => {
    if (!serverInfo.value) {
      return [
        {
          label: 'CPU',
          value: '--',
          percentage: 0,
          desc: '--',
          color: '#67C23A',
          ringData: [
            { value: 0, name: 'used' },
            { value: 100, name: 'total' }
          ]
        },
        {
          label: 'JVM内存',
          value: '--',
          percentage: 0,
          desc: '--',
          color: '#67C23A',
          ringData: [
            { value: 0, name: 'used' },
            { value: 100, name: 'total' }
          ]
        },
        {
          label: '直接内存',
          value: '--',
          percentage: 0,
          desc: '--',
          color: '#67C23A',
          ringData: [
            { value: 0, name: 'used' },
            { value: 100, name: 'total' }
          ]
        },
        {
          label: '物理内存',
          value: '--',
          percentage: 0,
          desc: '--',
          color: '#67C23A',
          ringData: [
            { value: 0, name: 'used' },
            { value: 100, name: 'total' }
          ]
        }
      ]
    }

    const cpuUsage = Number(serverInfo.value.cpu?.usage || 0)
    const memUsage = Number(serverInfo.value.osMem?.usage || 0)
    const jvmUsage = Number(serverInfo.value.jvmMem?.usage || 0)
    const directMemUsage = Number(serverInfo.value.directMem?.usage || 0)

    const getColor = (usage: number) => {
      if (usage < 70) return '#20a53a'
      if (usage < 90) return '#f59e0b'
      return '#ef4444'
    }

    return [
      {
        label: 'CPU',
        value: `${serverInfo.value.cpu?.total || 0}核心`,
        percentage: cpuUsage,
        desc: '',
        color: getColor(cpuUsage),
        ringData: [
          { value: cpuUsage, name: 'used' },
          { value: 100 - cpuUsage, name: 'total' }
        ]
      },
      {
        label: 'JVM内存',
        value: '',
        usedValue: serverInfo.value.jvmMem?.used || '0MB',
        totalValue: serverInfo.value.jvmMem?.total || '0MB',
        percentage: jvmUsage,
        desc: '',
        color: getColor(jvmUsage),
        ringData: [
          { value: jvmUsage, name: 'used' },
          { value: 100 - jvmUsage, name: 'total' }
        ]
      },
      {
        label: '直接内存',
        value: '',
        usedValue: serverInfo.value.directMem?.used || '0MB',
        totalValue: serverInfo.value.directMem?.total || '0MB',
        percentage: directMemUsage,
        desc: '',
        color: getColor(directMemUsage),
        ringData: [
          { value: directMemUsage, name: 'used' },
          { value: 100 - directMemUsage, name: 'total' }
        ]
      },
      {
        label: '物理内存',
        value: '',
        usedValue: serverInfo.value.osMem?.used || '0MB',
        totalValue: serverInfo.value.osMem?.total || '0MB',
        percentage: memUsage,
        desc: '',
        color: getColor(memUsage),
        ringData: [
          { value: memUsage, name: 'used' },
          { value: 100 - memUsage, name: 'total' }
        ]
      }
    ]
  })

  const getData = async () => {
    serverInfo.value = (await fetchGetServerInfo()) as Api.Monitor.ServerInfo
  }

  onMounted(() => {
    getData()
  })
</script>

<style scoped>
  .ring-chart {
    width: 128px;
    height: 128px;
  }

  .art-card {
    border-radius: 12px;
    background: var(--el-bg-color);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  }
</style>
