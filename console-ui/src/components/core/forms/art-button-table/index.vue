<!-- 表格按钮 -->
<template>
  <div
    :class="[
      isLinkVariant ? linkBaseClass : buttonBaseClass,
      buttonClass,
      { 'opacity-50 cursor-not-allowed': disabled }
    ]"
    :style="inlineStyle"
    @click="handleClick"
  >
    <ArtSvgIcon v-if="!text && iconContent" :icon="iconContent" />
    <span v-else>{{ text }}</span>
  </div>
</template>

<script setup lang="ts">
  defineOptions({ name: 'ArtButtonTable' })

  type ButtonType = 'add' | 'edit' | 'delete' | 'more' | 'view' | 'text' | 'primary' | 'link'

  interface Props {
    /** 按钮类型 */
    type?: ButtonType
    /** 展示变体：link 为无背景文字链样式 */
    variant?: 'default' | 'link'
    /** 按钮图标 */
    icon?: string
    /** 按钮样式类 */
    iconClass?: string
    /** icon 颜色 */
    iconColor?: string
    /** 按钮背景色 */
    buttonBgColor?: string
    /** 按钮文本 */
    text?: string
    /** 是否禁用 */
    disabled?: boolean
  }

  const props = withDefaults(defineProps<Props>(), {
    variant: 'default'
  })

  const emit = defineEmits<{
    (e: 'click'): void
  }>()

  const buttonBaseClass =
    'inline-flex items-center justify-center min-w-8 h-8 px-2.5 mr-2.5 text-sm c-p rounded-md align-middle'

  const linkBaseClass =
    'inline-flex items-center mr-3 text-sm c-p align-middle bg-transparent'

  const defaultButtons: Record<
    ButtonType,
    { icon?: string; class: string; variant?: 'default' | 'link' }
  > = {
    add: { icon: 'ri:add-fill', class: 'bg-theme/12 text-theme' },
    edit: { icon: 'ri:pencil-line', class: 'bg-secondary/12 text-secondary' },
    delete: { icon: 'ri:delete-bin-5-line', class: 'bg-error/12 text-error' },
    view: { icon: 'ri:eye-line', class: 'bg-info/12 text-info' },
    more: { icon: 'ri:more-2-fill', class: '' },
    text: { class: 'bg-gray-100 text-gray-800' },
    primary: { class: 'bg-theme/12 text-theme' },
    link: { class: 'text-theme', variant: 'link' }
  }

  const isLinkVariant = computed(
    () => props.variant === 'link' || props.type === 'link'
  )

  const iconContent = computed(() => {
    return props.icon || (props.type ? defaultButtons[props.type]?.icon : '') || ''
  })

  const buttonClass = computed(() => {
    return props.iconClass || (props.type ? defaultButtons[props.type]?.class : '') || ''
  })

  const inlineStyle = computed(() => {
    if (isLinkVariant.value) return undefined
    return {
      backgroundColor: props.buttonBgColor,
      color: props.iconColor
    }
  })

  const handleClick = () => {
    if (!props.disabled) {
      emit('click')
    }
  }
</script>
