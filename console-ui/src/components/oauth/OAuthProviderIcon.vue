<script setup lang="ts">
  import githubIcon from '@imgs/svg/github.svg'
  import giteeIcon from '@imgs/svg/gitee.svg'
  import googleIcon from '@imgs/svg/google.svg'
  import wechatIcon from '@imgs/svg/wechat.svg'
  import qqIcon from '@imgs/svg/qq.svg'
  import qiWechatIcon from '@imgs/svg/qi_wechat.svg'

  defineOptions({ name: 'OAuthProviderIcon' })

  const props = withDefaults(
    defineProps<{
      provider: string
      size?: number | string
      alt?: string
    }>(),
    {
      size: 22
    }
  )

  const ICON_MAP: Record<string, string> = {
    GITHUB: githubIcon,
    GITEE: giteeIcon,
    GOOGLE: googleIcon,
    WECHAT: wechatIcon,
    QQ: qqIcon,
    QI_WECHAT: qiWechatIcon,
    QIWECHAT: qiWechatIcon
  }

  const providerKey = computed(() => (props.provider || '').trim().toUpperCase())
  const src = computed(() => ICON_MAP[providerKey.value] || '')
  const label = computed(() => props.alt || providerKey.value || 'OAuth')
  const sizeValue = computed(() =>
    typeof props.size === 'number' ? `${props.size}px` : props.size
  )
</script>

<template>
  <img
    v-if="src"
    :src="src"
    :alt="label"
    class="oauth-provider-icon"
    :class="`is-${providerKey.toLowerCase()}`"
    :style="{ width: sizeValue, height: sizeValue }"
    draggable="false"
  />
  <span
    v-else
    class="oauth-provider-icon oauth-provider-icon--fallback"
    :style="{ width: sizeValue, height: sizeValue }"
  >
    {{ label.slice(0, 1) }}
  </span>
</template>

<style scoped lang="scss">
  .oauth-provider-icon {
    display: block;
    flex-shrink: 0;
    object-fit: contain;
    user-select: none;
  }

  .oauth-provider-icon--fallback {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    background: var(--el-fill-color);
    color: var(--el-text-color-secondary);
    font-size: 12px;
    font-weight: 600;
  }
</style>

<style lang="scss">
  /* GitHub 图标为纯黑，暗色模式下反色保证可见 */
  html.dark .oauth-provider-icon.is-github,
  [data-theme='dark'] .oauth-provider-icon.is-github {
    filter: invert(1);
  }
</style>
