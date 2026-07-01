<template>
  <div class="access-control-page">
    <div class="mb-6">
      <h3 class="text-lg font-semibold mb-4">基本配置</h3>
      <div class="flex flex-col gap-4">
        <div class="flex items-center gap-3">
          <span class="w-20 font-medium">启用状态：</span>
          <ElSwitch v-model="formData.enabled" @change="handleEnableChange" />
        </div>
        <div class="flex items-center gap-3">
          <span class="w-20 font-medium">控制模式：</span>
          <ElRadioGroup v-model="formData.mode" @change="handleModeChange">
            <ElRadio :label="1">白名单（只允许指定IP访问）</ElRadio>
            <ElRadio :label="0">黑名单（禁止指定IP访问）</ElRadio>
          </ElRadioGroup>
        </div>
      </div>
    </div>

    <div>
      <h3 class="text-lg font-semibold mb-4">访问规则</h3>
      <div class="border border-gray-200 rounded p-4">
        <ElTable :data="formData.rules" style="width: 100%" border>
          <ElTableColumn prop="cidr" label="IP地址段 (例如：192.168.1.0/24)" width="300">
            <template #default="scope">
              <ElInput
                v-if="editingRuleId === scope.row.id"
                v-model="scope.row.cidr"
                placeholder="请输入IP地址段，例如：192.168.1.0/24"
                style="width: 100%"
              />
              <span v-else>{{ scope.row.cidr }}</span>
            </template>
          </ElTableColumn>
          <ElTableColumn prop="ruleType" label="规则类型" width="150">
            <template #default="scope">
              <ElRadioGroup v-if="editingRuleId === scope.row.id" v-model="scope.row.ruleType">
                <ElRadio :label="1">放行</ElRadio>
                <ElRadio :label="0">禁止</ElRadio>
              </ElRadioGroup>
              <ElTag v-else :type="scope.row.ruleType === 1 ? 'success' : 'danger'">
                {{ scope.row.ruleType === 1 ? '放行' : '禁止' }}
              </ElTag>
            </template>
          </ElTableColumn>
          <ElTableColumn label="操作" width="240" fixed="right">
            <template #default="scope">
              <ElSpace size="small">
                <ElButton
                  v-if="editingRuleId === scope.row.id"
                  type="primary"
                  size="small"
                  @click="handleSaveRule(scope.row)"
                >
                  保存
                </ElButton>
                <ElButton v-else type="link" size="small" @click="handleEditRule(scope.row)">
                  编辑
                </ElButton>
                <ElButton type="link" size="small" @click="handleDeleteRule(scope.row.id)">
                  <template #icon>
                    <Delete />
                  </template>
                  删除
                </ElButton>
              </ElSpace>
            </template>
          </ElTableColumn>
        </ElTable>
        <ElButton type="primary" size="small" @click="addRule" class="mt-3">
          <template #icon>
            <Plus />
          </template>
          新增规则
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
    fetchGetAccessControl,
    fetchUpdateAccessControl,
    fetchAddAccessControlRule,
    fetchUpdateAccessControlRule,
    fetchDeleteAccessControlRule
  } from '@/api/access-control'

  defineOptions({ name: 'AccessControlPage' })

  const props = defineProps<{
    proxyId: string
  }>()

  const formData = reactive({
    enabled: false,
    mode: 1,
    rules: [] as Array<{
      id: number
      proxyId: string
      cidr: string
      ruleType: number
    }>
  })

  const editingRuleId = ref<number | null>(null)
  const editingRuleBackup = ref<any>(null)

  const resetFormData = () => {
    formData.enabled = false
    formData.mode = 1
    formData.rules = []
    editingRuleId.value = null
    editingRuleBackup.value = null
  }

  const fetchAccessControlData = async () => {
    const response = await fetchGetAccessControl(props.proxyId)
    if (response) {
      formData.enabled = response.enabled || false
      formData.mode = response.mode !== undefined ? response.mode : 1
      formData.rules = response.rules || []
    }
  }

  watch(
    () => props.proxyId,
    async (proxyId) => {
      if (!proxyId) return
      resetFormData()
      await fetchAccessControlData()
    },
    { immediate: true }
  )

  const handleEnableChange = async () => {
    await updateAccessControlConfig()
  }

  const handleModeChange = async () => {
    await updateAccessControlConfig()
  }

  const updateAccessControlConfig = async () => {
    await fetchUpdateAccessControl({
      proxyId: props.proxyId,
      enabled: formData.enabled,
      mode: formData.mode
    })
  }

  const addRule = () => {
    formData.rules.push({
      id: 0,
      proxyId: props.proxyId,
      cidr: '',
      ruleType: 1
    })
    const newRule = formData.rules[formData.rules.length - 1]
    handleEditRule(newRule)
  }

  const handleEditRule = (rule: any) => {
    editingRuleBackup.value = { ...rule }
    editingRuleId.value = rule.id
  }

  const handleSaveRule = async (rule: any) => {
    if (!rule.cidr) {
      ElMessage.error('请输入IP地址段')
      return
    }

    if (rule.id > 0) {
      await fetchUpdateAccessControlRule({
        id: rule.id,
        cidr: rule.cidr,
        ruleType: rule.ruleType
      })
    } else {
      await fetchAddAccessControlRule({
        proxyId: props.proxyId,
        cidr: rule.cidr,
        ruleType: rule.ruleType
      })
    }
    editingRuleId.value = null
    await fetchAccessControlData()
    editingRuleBackup.value = null
  }

  const handleDeleteRule = async (id: number) => {
    await ElMessageBox.confirm('确定要删除此规则吗？', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    if (id > 0) {
      await fetchDeleteAccessControlRule(id)
      await fetchAccessControlData()
    } else {
      const index = formData.rules.findIndex((rule) => rule.id === id)
      if (index > -1) {
        formData.rules.splice(index, 1)
        editingRuleId.value = null
      }
    }
  }
</script>
