<!-- 统计卡片 -->
<template>
  <div
    class="art-card w-full min-w-0 px-4 py-4 transition-transform duration-200 hover:-translate-y-0.5"
    :class="boxStyle"
  >
    <div class="flex items-start justify-between gap-3">
      <div class="min-w-0 flex-1">
        <p
          class="m-0 text-sm text-g-600 truncate"
          :style="{ color: textColor }"
          v-if="title"
        >
          {{ title }}
        </p>
        <ArtCountTo
          class="mt-2 block text-2xl font-semibold leading-none truncate"
          v-if="count !== undefined"
          :target="count"
          :duration="2000"
          :decimals="decimals"
          :separator="separator"
          :prefix="prefix"
          :suffix="suffix"
        />
        <p
          class="mt-1.5 mb-0 text-xs text-g-500 truncate"
          :style="{ color: textColor }"
          v-if="description"
        >
          {{ description }}
        </p>
      </div>
      <div
        v-if="icon"
        class="size-10 shrink-0 flex-cc rounded-lg text-lg text-white"
        :class="iconStyle"
      >
        <ArtSvgIcon :icon="icon" />
      </div>
      <div v-else-if="showArrow" class="shrink-0 self-center">
        <ArtSvgIcon icon="ri:arrow-right-s-line" class="text-xl text-g-500" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  defineOptions({ name: 'ArtStatsCard' })

  interface StatsCardProps {
    /** 盒子样式 */
    boxStyle?: string
    /** 图标 */
    icon?: string
    /** 图标样式 */
    iconStyle?: string
    /** 标题 */
    title?: string
    /** 数值 */
    count?: number
    /** 小数位 */
    decimals?: number
    /** 分隔符 */
    separator?: string
    /** 前缀*/
    prefix?: string
    /** 后缀 */
    suffix?: string
    /** 描述 */
    description?: string
    /** 文本颜色 */
    textColor?: string
    /** 是否显示箭头 */
    showArrow?: boolean
  }

  withDefaults(defineProps<StatsCardProps>(), {
    decimals: 0,
    separator: ',',
    prefix: '',
    suffix: ''
  })
</script>
