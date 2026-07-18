<template>
  <div class="oauth-callback-page">
    <ElResult
      :icon="errorMessage ? 'error' : 'info'"
      :title="errorMessage ? '登录失败' : '正在完成登录…'"
      :sub-title="errorMessage || '请稍候'"
    >
      <template v-if="errorMessage" #extra>
        <ElButton type="primary" @click="goLogin">返回登录</ElButton>
      </template>
    </ElResult>
  </div>
</template>

<script setup lang="ts">
  import { onMounted, ref } from 'vue'
  import { useRoute, useRouter } from 'vue-router'
  import { ElMessage } from 'element-plus'
  import { fetchOAuthToken } from '@/api/oauth'
  import { useUserStore } from '@/store/modules/user'

  defineOptions({ name: 'OAuthCallback' })

  const route = useRoute()
  const router = useRouter()
  const userStore = useUserStore()
  const errorMessage = ref('')

  const goLogin = () => {
    router.replace({ name: 'Login' })
  }

  onMounted(async () => {
    const ticket = route.query.ticket as string
    if (!ticket) {
      errorMessage.value = '登录失败，请重试'
      return
    }
    try {
      const { token, refreshToken } = await fetchOAuthToken(ticket)
      if (!token) {
        errorMessage.value = '登录失败，请重试'
        return
      }
      userStore.setToken(token, refreshToken)
      userStore.setLoginStatus(true)
      ElMessage.success('登录成功')
      router.replace('/')
    } catch {
      errorMessage.value = '登录失败，请重试'
    }
  })
</script>

<style scoped>
  .oauth-callback-page {
    display: flex;
    align-items: center;
    justify-content: center;
    min-height: 100vh;
  }
</style>
