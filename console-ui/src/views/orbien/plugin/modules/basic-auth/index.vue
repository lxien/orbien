<template>
  <div class="basic-auth-page">
    <div class="mb-6">
      <h3 class="text-lg font-semibold mb-4">基本配置</h3>
      <div class="flex flex-col gap-4">
        <div class="flex items-center gap-3">
          <span class="w-20 font-medium">启用状态：</span>
          <ElSwitch v-model="formData.enabled" @change="handleEnableChange" />
        </div>
      </div>
    </div>

    <div>
      <h3 class="text-lg font-semibold mb-4">用户列表</h3>
      <div class="border border-gray-200 rounded p-4">
        <ElTable :data="formData.users" style="width: 100%" border>
          <ElTableColumn prop="username" label="用户名" width="200">
            <template #default="scope">
              <ElInput
                v-if="editingUserId === scope.row.id"
                v-model="scope.row.username"
                size="small"
                placeholder="请输入用户名"
                style="width: 100%"
              />
              <span v-else>{{ scope.row.username }}</span>
            </template>
          </ElTableColumn>
          <ElTableColumn prop="password" label="密码" width="200">
            <template #default="scope">
              <ElInput
                v-if="editingUserId === scope.row.id"
                v-model="scope.row.password"
                size="small"
                placeholder="请输入密码"
                type="password"
                style="width: 100%"
              />
              <span v-else>{{ '••••••••' }}</span>
            </template>
          </ElTableColumn>
          <ElTableColumn label="操作" width="200" fixed="right">
            <template #default="scope">
              <ElSpace size="small">
                <ElButton
                  v-if="editingUserId === scope.row.id"
                  type="primary"
                  size="small"
                  @click="handleSaveUser(scope.row)"
                >
                  保存
                </ElButton>
                <ElButton v-else type="link" size="small" @click="handleEditUser(scope.row)">
                  编辑
                </ElButton>
                <ElButton type="link" size="small" @click="handleDeleteUser(scope.row.id)">
                  <template #icon>
                    <Delete />
                  </template>
                  删除
                </ElButton>
              </ElSpace>
            </template>
          </ElTableColumn>
        </ElTable>
        <ElButton type="primary" size="small" @click="addUser" class="mt-3">
          <template #icon>
            <Plus />
          </template>
          新增用户
        </ElButton>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref, reactive, watch } from 'vue'
  import { ElMessage, ElMessageBox } from 'element-plus'
  import { Plus, Delete } from '@element-plus/icons-vue'
  import {
    fetchGetBasicAuth,
    fetchUpdateBasicAuth,
    fetchAddBasicAuthUser,
    fetchUpdateBasicAuthUser,
    fetchDeleteBasicAuthUser
  } from '@/api/basic-auth'

  defineOptions({ name: 'BasicAuthPage' })

  const props = defineProps<{
    proxyId: string
  }>()

  const formData = reactive({
    enabled: false,
    users: [] as Array<{
      id: number
      proxyId: string
      username: string
      password: string
    }>
  })

  const editingUserId = ref<number | null>(null)
  const editingUserBackup = ref<any>(null)

  const resetFormData = () => {
    formData.enabled = false
    formData.users = []
    editingUserId.value = null
    editingUserBackup.value = null
  }

  const fetchBasicAuthData = async () => {
    const response = await fetchGetBasicAuth(props.proxyId)
    if (response) {
      formData.enabled = response.enabled || false
      formData.users = response.users || []
    }
  }

  watch(
    () => props.proxyId,
    async (proxyId) => {
      if (!proxyId) return
      resetFormData()
      await fetchBasicAuthData()
    },
    { immediate: true }
  )

  const handleEnableChange = async () => {
    await fetchUpdateBasicAuth({
      proxyId: props.proxyId,
      enabled: formData.enabled
    })
  }

  const addUser = () => {
    formData.users.push({
      id: 0,
      proxyId: props.proxyId,
      username: '',
      password: ''
    })
    const newUser = formData.users[formData.users.length - 1]
    handleEditUser(newUser)
  }

  const handleEditUser = (user: any) => {
    editingUserBackup.value = { ...user }
    editingUserId.value = user.id
  }

  const handleSaveUser = async (user: any) => {
    if (!user.username) {
      ElMessage.error('请输入用户名')
      return
    }
    if (!user.password) {
      ElMessage.error('请输入密码')
      return
    }

    if (user.id > 0) {
      await fetchUpdateBasicAuthUser({
        id: user.id,
        proxyId: props.proxyId,
        username: user.username,
        password: user.password
      })
    } else {
      await fetchAddBasicAuthUser({
        proxyId: props.proxyId,
        username: user.username,
        password: user.password
      })
    }
    editingUserId.value = null
    await fetchBasicAuthData()
    editingUserBackup.value = null
  }

  const handleDeleteUser = async (id: number) => {
    await ElMessageBox.confirm('确定要删除此用户吗？', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    if (id > 0) {
      await fetchDeleteBasicAuthUser(id)
      await fetchBasicAuthData()
    } else {
      const index = formData.users.findIndex((user) => user.id === id)
      if (index > -1) {
        formData.users.splice(index, 1)
        editingUserId.value = null
      }
    }
  }
</script>
