<template>
  <div class="art-card p-5 mb-5 max-sm:mb-4">
    <div class="art-card-header">
      <div class="title">
        <h4>概览</h4>
      </div>
    </div>
    <div class="stats-container">
      <div v-for="(item, index) in dataList" :key="index" class="stat-item">
        <div class="text-sm text-g-600 mb-1">{{ item.des }}</div>
        <div class="value-wrapper">
          <ArtCountTo
            class="value"
            :target="item.num"
            :duration="800"
          />
          <div class="icon-bg" :class="item.iconBgClass">
            <ArtSvgIcon :icon="item.icon" class="stat-icon" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, onMounted } from 'vue'
import { fetchGetDashboardSummary } from '@/api/monitor'
import ArtSvgIcon from '@/components/core/base/art-svg-icon/index.vue'
import ArtCountTo from '@/components/core/text-effect/art-count-to/index.vue'

interface CardDataItem {
  des: string
  icon: string
  num: number
  iconBgClass: string
}

const dataList = reactive<CardDataItem[]>([
  {
    des: '客户端总数',
    icon: 'ri:computer-line',
    num: 0,
    iconBgClass: 'icon-bg-blue'
  },
  {
    des: '在线客户端数',
    icon: 'ri:user-star-line',
    num: 0,
    iconBgClass: 'icon-bg-green'
  },
  {
    des: '隧道总数',
    icon: 'ri:server-line',
    num: 0,
    iconBgClass: 'icon-bg-orange'
  },
  {
    des: '已启动隧道数',
    icon: 'ri:play-circle-line',
    num: 0,
    iconBgClass: 'icon-bg-purple'
  }
])

const getDashboardSummary = async () => {
  const data = await fetchGetDashboardSummary()
  if (data) {
    dataList[0].num = data.totalAgents || 0
    dataList[1].num = data.onlineAgents || 0
    dataList[2].num = data.totalProxies || 0
    dataList[3].num = data.startedProxies || 0
  }
}

onMounted(() => {
  getDashboardSummary()
})
</script>

<style scoped>
.art-card-header {
  margin-bottom: 20px;
}

.title h4 {
  font-size: 18px;
  font-weight: 600;
  color: var(--title-color);
  margin: 0;
}

.stats-container {
  display: flex;
  gap: 40px;
}

.stat-item {
  flex: 1;
  min-width: 0;
}

.value-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
}

.value {
  font-size: 36px;
  font-weight: 600;
  color: var(--title-color);
  line-height: 1;
}

.icon-bg {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-left: auto;
}

.icon-bg-blue {
  background: var(--el-color-primary-light-9);
}

.icon-bg-green {
  background: var(--el-color-success-light-9);
}

.icon-bg-orange {
  background: var(--el-color-warning-light-9);
}

.icon-bg-purple {
  background: #f0e7ff;
}

.stat-icon {
  font-size: 24px;
}

.icon-bg-blue .stat-icon {
  color: var(--el-color-primary);
}

.icon-bg-green .stat-icon {
  color: var(--el-color-success);
}

.icon-bg-orange .stat-icon {
  color: var(--el-color-warning);
}

.icon-bg-purple .stat-icon {
  color: #7c3aed;
}

@media (max-width: 1200px) {
  .stats-container {
    gap: 24px;
  }
}

@media (max-width: 768px) {
  .stats-container {
    flex-direction: column;
    gap: 28px;
  }
  .value {
    font-size: 32px;
  }
}
</style>
