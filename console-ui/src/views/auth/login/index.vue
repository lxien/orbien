<!-- 登录页面 -->
<template>
  <div class="flex w-full h-screen">
    <LoginLeftView />

    <div class="relative flex-1">
      <AuthTopBar />

      <div class="auth-right-wrap">
        <div class="form">
          <h3 class="title">{{ $t('login.title') }}</h3>
          <p class="sub-title">{{ $t('login.subTitle') }}</p>
          <ElForm
            ref="formRef"
            :model="formData"
            :rules="rules"
            :key="formKey"
            @keyup.enter="handleSubmit"
            style="margin-top: 25px"
          >
            <ElFormItem prop="account">
              <ElSelect v-model="formData.account" @change="setupAccount">
                <ElOption
                  v-for="account in accounts"
                  :key="account.key"
                  :label="account.label"
                  :value="account.key"
                >
                  <span>{{ account.label }}</span>
                </ElOption>
              </ElSelect>
            </ElFormItem>
            <ElFormItem prop="username">
              <ElInput
                class="custom-height"
                :placeholder="$t('login.placeholder.username')"
                v-model.trim="formData.username"
              />
            </ElFormItem>
            <ElFormItem prop="password">
              <ElInput
                class="custom-height"
                :placeholder="$t('login.placeholder.password')"
                v-model.trim="formData.password"
                type="password"
                autocomplete="off"
                show-password
              />
            </ElFormItem>

            <!-- 推拽验证 -->
            <div class="relative pb-5 mt-6">
              <div
                class="relative z-[2] overflow-hidden select-none rounded-lg border border-transparent tad-300"
                :class="{ '!border-[#FF4E4F]': !isPassing && isClickPass }"
              >
                <ArtDragVerify
                  ref="dragVerify"
                  v-model:value="isPassing"
                  :text="$t('login.sliderText')"
                  textColor="var(--art-gray-700)"
                  :successText="$t('login.sliderSuccessText')"
                  progressBarBg="var(--main-color)"
                  :background="isDark ? '#26272F' : '#F1F1F4'"
                  handlerBg="var(--default-box-color)"
                />
              </div>
              <p
                class="absolute top-0 z-[1] px-px mt-2 text-xs text-[#f56c6c] tad-300"
                :class="{ 'translate-y-10': !isPassing && isClickPass }"
              >
                {{ $t('login.placeholder.slider') }}
              </p>
            </div>

            <div class="flex-cb mt-2 text-sm">
              <ElCheckbox v-model="formData.rememberPassword">{{
                $t('login.rememberPwd')
              }}</ElCheckbox>
<!--              <RouterLink class="text-theme" :to="{ name: 'ForgetPassword' }">{{
                $t('login.forgetPwd')
              }}</RouterLink>-->
            </div>

            <div style="margin-top: 30px">
              <ElButton
                class="w-full custom-height"
                type="primary"
                @click="handleSubmit"
                :loading="loading"
                v-ripple
              >
                {{ $t('login.btnText') }}
              </ElButton>
            </div>

            <div v-if="oauthProviders.length" class="oauth-login-section">
              <div class="oauth-divider">
                <span>{{ $t('login.oauth.otherWays') }}</span>
              </div>
              <div class="oauth-buttons">
                <button
                  v-for="item in oauthProviders"
                  :key="item.provider"
                  type="button"
                  class="oauth-icon-btn"
                  :title="$t('login.oauth.loginWith', { name: item.displayName })"
                  :aria-label="$t('login.oauth.loginWith', { name: item.displayName })"
                  @click="handleOAuthLogin(item.provider)"
                >
                  <OAuthProviderIcon :provider="item.provider" :size="22" :alt="item.displayName" />
                </button>
              </div>
            </div>

<!--            <div class="mt-5 text-sm text-gray-600">
              <span>{{ $t('login.noAccount') }}</span>
              <RouterLink class="text-theme" :to="{ name: 'Register' }">{{
                $t('login.register')
              }}</RouterLink>
            </div>-->
          </ElForm>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import AppConfig from '@/config'
  import { useUserStore } from '@/store/modules/user'
  import { useI18n } from 'vue-i18n'
  import { HttpError } from '@/utils/http/error'
  import { fetchLogin } from '@/api/auth'
  import { fetchPublicOAuthProviders, redirectOAuthAuthorize } from '@/api/oauth'
  import OAuthProviderIcon from '@/components/oauth/OAuthProviderIcon.vue'
  import { ElMessage, ElNotification, type FormInstance, type FormRules } from 'element-plus'
  import { useSettingStore } from '@/store/modules/setting'

  defineOptions({ name: 'Login' })

  const settingStore = useSettingStore()
  const { isDark } = storeToRefs(settingStore)
  const { t, locale } = useI18n()
  const formKey = ref(0)
  const oauthProviders = ref<Api.OAuth.PublicProvider[]>([])

  // 监听语言切换，重置表单
  watch(locale, () => {
    formKey.value++
  })

  type AccountKey = 'super' | 'admin' | 'user'

  export interface Account {
    key: AccountKey
    label: string
    userName: string
    password: string
    roles: string[]
  }

  const accounts = computed<Account[]>(() => [
    {
      key: 'super',
      label: t('login.roles.super'),
      userName: '',
      password: '',
      roles: ['R_SUPER']
    }
  ])

  const dragVerify = ref()

  const userStore = useUserStore()
  const router = useRouter()
  const route = useRoute()
  const isPassing = ref(false)
  const isClickPass = ref(false)

  const systemName = AppConfig.systemInfo.name
  const formRef = ref<FormInstance>()

  const formData = reactive({
    account: '',
    username: '',
    password: '',
    rememberPassword: true
  })

  const rules = computed<FormRules>(() => ({
    username: [{ required: true, message: t('login.placeholder.username'), trigger: 'blur' }],
    password: [{ required: true, message: t('login.placeholder.password'), trigger: 'blur' }]
  }))

  const loading = ref(false)

  onMounted(() => {
    setupAccount('super')
    loadOAuthProviders()
    showOAuthErrorIfNeeded()
  })

  const loadOAuthProviders = async () => {
    try {
      oauthProviders.value = (await fetchPublicOAuthProviders()) || []
    } catch {
      oauthProviders.value = []
    }
  }

  const showOAuthErrorIfNeeded = () => {
    const error = route.query.oauthError as string
    if (!error) return
    const messages: Record<string, string> = {
      user_not_found: t('login.oauth.errors.userNotFound'),
      denied: t('login.oauth.errors.denied'),
      invalid_state: t('login.oauth.errors.invalidState'),
      failed: t('login.oauth.errors.failed')
    }
    ElMessage.error(messages[error] || t('login.oauth.errors.failed'))
    router.replace({ name: 'Login' })
  }

  const handleOAuthLogin = (provider: string) => {
    redirectOAuthAuthorize(provider)
  }

  // 设置账号
  const setupAccount = (key: AccountKey) => {
    const selectedAccount = accounts.value.find((account: Account) => account.key === key)
    formData.account = key
    formData.username = selectedAccount?.userName ?? ''
    formData.password = selectedAccount?.password ?? ''
  }

  // 登录
  const handleSubmit = async () => {
    if (!formRef.value) return

    try {
      // 表单验证
      const valid = await formRef.value.validate()
      if (!valid) return

      // 拖拽验证
      if (!isPassing.value) {
        isClickPass.value = true
        return
      }

      loading.value = true

      // 登录请求
      const { username, password } = formData

      const { token, refreshToken } = await fetchLogin({
        userName: username,
        password
      })

      // 验证token
      if (!token) {
        throw new Error('Login failed - no token received')
      }

      userStore.setToken(token, refreshToken)
      userStore.setLoginStatus(true)

      showLoginSuccessNotice()

      // 获取 redirect 参数，如果存在则跳转到指定页面，否则跳转到首页
      const redirect = route.query.redirect as string
      router.push(redirect || '/')
    } catch (error) {
      if (error instanceof HttpError) {
        // console.log(error.code)
      } else {
        // ElMessage.error('登录失败，请稍后重试')
        console.error('[Login] Unexpected error:', error)
      }
    } finally {
      loading.value = false
      resetDragVerify()
    }
  }

  // 重置拖拽验证
  const resetDragVerify = () => {
    dragVerify.value.reset()
  }

  // 登录成功提示
  const showLoginSuccessNotice = () => {
    setTimeout(() => {
      ElNotification({
        title: t('login.success.title'),
        type: 'success',
        duration: 2500,
        zIndex: 10000,
        message: `${t('login.success.message')}, ${systemName}!`
      })
    }, 1000)
  }
</script>

<style scoped>
  @import './style.css';

  .oauth-login-section {
    margin-top: 28px;
  }

  .oauth-divider {
    display: flex;
    align-items: center;
    margin-bottom: 18px;
    color: var(--art-gray-500);
    font-size: 12px;
  }

  .oauth-divider::before,
  .oauth-divider::after {
    content: '';
    flex: 1;
    height: 1px;
    background: var(--art-gray-300);
  }

  .oauth-divider span {
    padding: 0 12px;
    white-space: nowrap;
  }

  .oauth-buttons {
    display: flex;
    align-items: center;
    justify-content: center;
    flex-wrap: wrap;
    gap: 16px;
  }

  .oauth-icon-btn {
    width: 44px;
    height: 44px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    padding: 0;
    border: 1px solid var(--art-gray-300);
    border-radius: 50%;
    background: var(--default-box-color);
    cursor: pointer;
    transition:
      border-color 0.2s ease,
      background-color 0.2s ease,
      transform 0.15s ease,
      box-shadow 0.2s ease;

    &:hover {
      border-color: var(--el-color-primary-light-5);
      background: var(--el-fill-color-light);
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
      transform: translateY(-1px);
    }

    &:active {
      transform: translateY(0);
    }

    &:focus-visible {
      outline: 2px solid var(--el-color-primary-light-5);
      outline-offset: 2px;
    }
  }
</style>

<style lang="scss" scoped>
  :deep(.el-select__wrapper) {
    height: 40px !important;
  }
</style>
