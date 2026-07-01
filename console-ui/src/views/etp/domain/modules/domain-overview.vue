<template>
  <div class="art-card p-5 mb-5 max-sm:mb-4">
    <div class="mb-5">
      <h4 class="m-0 text-lg font-semibold">域名资源</h4>
      <p class="m-0 mt-1 text-sm text-g-500">
        基础域名池定义平台可用的根域名；已分配域名展示代理隧道当前占用的访问地址。
      </p>
    </div>

    <div class="flex gap-10 max-md:flex-col max-md:gap-5">
      <div
        v-for="item in items"
        :key="item.key"
        class="flex-1 min-w-0 cursor-pointer rounded-lg p-4 transition-colors"
        :class="active === item.key ? 'bg-theme/10 ring-1 ring-theme' : 'hover:bg-g-100'"
        @click="handleSelect(item.key)"
      >
        <div class="mb-1 text-sm text-g-600">{{ item.label }}</div>
        <div class="flex items-center gap-3">
          <ArtCountTo class="text-3xl font-semibold leading-none" :target="item.count" :duration="600" />
          <ArtSvgIcon :icon="item.icon" class="ml-auto text-2xl" :class="item.iconClass" />
        </div>
        <div class="mt-2 text-xs text-g-500">{{ item.hint }}</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { computed } from 'vue'
  import ArtCountTo from '@/components/core/text-effect/art-count-to/index.vue'
  import ArtSvgIcon from '@/components/core/base/art-svg-icon/index.vue'

  defineOptions({ name: 'DomainOverview' })

  export type DomainView = 'pool' | 'allocated'

  const active = defineModel<DomainView>('active', { default: 'pool' })

  const props = defineProps<{
    baseCount: number
    usedCount: number
  }>()

  const items = computed(() => [
    {
      key: 'pool' as DomainView,
      label: '基础域名池',
      hint: '用于子域名自动拼接的根域名',
      icon: 'ri:global-line',
      iconClass: 'text-theme',
      count: props.baseCount
    },
    {
      key: 'allocated' as DomainView,
      label: '已分配域名',
      hint: '已被代理隧道占用的访问域名',
      icon: 'ri:link',
      iconClass: 'text-g-600',
      count: props.usedCount
    }
  ])

  const handleSelect = (view: DomainView) => {
    active.value = view
  }
</script>
