<template>
  <div id="spaceUserManagePage" class="page-container">
    <template v-if="device === DEVICE_TYPE_ENUM.PC">
      <div class="header-section">
        <h2 class="page-title">空间成员管理</h2>
        <div class="status-tabs">
          <div class="custom-tabs">
            <div
              class="custom-tab"
              :class="{ 'active': activeStatus === '1' }"
              @click="activeStatus = '1'; handleStatusChange('1')"
            >
              <span class="tab-label">
                已通过
                <span class="custom-badge" :class="{ active: activeStatus === '1' }">{{ statusCounts[1] }}</span>
              </span>
            </div>
            <div
              class="custom-tab"
              :class="{ 'active': activeStatus === '0' }"
              @click="activeStatus = '0'; handleStatusChange('0')"
            >
              <span class="tab-label">
                待审核
                <span class="custom-badge" :class="{ active: activeStatus === '0' }">{{ statusCounts[0] }}</span>
              </span>
            </div>
            <div
              class="custom-tab"
              :class="{ 'active': activeStatus === '2' }"
              @click="activeStatus = '2'; handleStatusChange('2')"
            >
              <span class="tab-label">
                已拒绝
                <span class="custom-badge" :class="{ active: activeStatus === '2' }">{{ statusCounts[2] }}</span>
              </span>
            </div>
          </div>
        </div>
      </div>

      <div class="search-form">
        <form class="inline-form" @submit.prevent="handleSubmit">
          <div class="form-item search-form-item">
            <label class="form-label">用户 ID</label>
            <div class="input-wrapper">
              <input
                v-model="formData.userId"
                placeholder="请输入用户 ID"
                class="search-input"
              />
              <button
                v-if="formData.userId"
                type="button"
                @click="formData.userId = ''"
                class="clear-btn"
              >
                ×
              </button>
            </div>
          </div>
          <div class="form-item">
            <button type="submit" class="primary-btn add-member-btn">
              <UserAddOutlined /> 添加成员
            </button>
          </div>
        </form>
      </div>

      <div class="member-table">
        <div class="table-header">
          <div class="table-cell">用户</div>
          <div class="table-cell">角色</div>
          <div class="table-cell">创建时间</div>
          <div class="table-cell">操作</div>
        </div>
        <div class="table-body">
          <div
            v-for="record in dataList"
            :key="record.id"
            class="table-row"
          >
            <div class="table-cell">
              <div class="user-info">
                <img
                  :src="record.user?.userAvatar"
                  class="clickable-avatar"
                  @click="goToUserPage(record.user?.id)"
                  alt="用户头像"
                />
                {{ record.user?.userName }}
              </div>
            </div>
            <div class="table-cell">
              <select
                v-model="record.spaceRole"
                @change="(e) => editSpaceRole(e.target.value, record)"
                :disabled="record.user?.id === loginUserStore.loginUser?.id || activeStatus !== '1'"
                class="role-select"
              >
                <option
                  v-for="option in SPACE_ROLE_OPTIONS"
                  :key="option.value"
                  :value="option.value"
                >
                  {{ option.label }}
                </option>
              </select>
            </div>
            <div class="table-cell">
              {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}
            </div>
            <div class="table-cell">
              <div class="action-buttons">
                <template v-if="activeStatus !== '1' && record.user?.id !== loginUserStore.loginUser?.id">
                  <button
                    @click="handleAudit(record, 1)"
                    class="success-btn"
                  >
                    通过
                  </button>
                </template>
                <template v-if="activeStatus !== '2' && record.user?.id !== loginUserStore.loginUser?.id">
                  <button
                    @click="handleAudit(record, 2)"
                    class="danger-btn"
                  >
                    拒绝
                  </button>
                </template>
                <button
                  @click="toggleRecommended(record)"
                  :class="record.isRecommended === 1 ? 'recommended-btn' : 'unrecommended-btn'"
                  :disabled="activeStatus !== '1'"
                >
                  {{ record.isRecommended === 1 ? '取消推荐' : '设为推荐' }}
                </button>
                <button
                  @click="showDeleteConfirm(record)"
                  :disabled="record.user?.id === loginUserStore.loginUser?.id"
                  class="delete-btn"
                >
                  删除
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <template v-else>
      <div class="mobile-tabs">
        <div
          class="mobile-tab"
          :class="{ 'active': activeStatus === '1' }"
          @click="activeStatus = '1'; handleStatusChange('1')"
        >
          <span>已通过</span>
          <span class="mobile-badge" :class="{ active: activeStatus === '1' }">{{ statusCounts[1] }}</span>
        </div>
        <div
          class="mobile-tab"
          :class="{ 'active': activeStatus === '0' }"
          @click="activeStatus = '0'; handleStatusChange('0')"
        >
          <span>待审核</span>
          <span class="mobile-badge" :class="{ active: activeStatus === '0' }">{{ statusCounts[0] }}</span>
        </div>
        <div
          class="mobile-tab"
          :class="{ 'active': activeStatus === '2' }"
          @click="activeStatus = '2'; handleStatusChange('2')"
        >
          <span>已拒绝</span>
          <span class="mobile-badge" :class="{ active: activeStatus === '2' }">{{ statusCounts[2] }}</span>
        </div>
      </div>

      <div class="mobile-search-container">
        <div class="mobile-search">
          <div class="mobile-search-input-wrapper">
            <input
              v-model="formData.userId"
              placeholder="请输入用户 ID"
              class="mobile-search-input"
              @keyup.enter="handleSubmit"
            />
            <button
              v-if="formData.userId"
              type="button"
              @click="formData.userId = ''"
              class="mobile-clear-btn"
            >
              ×
            </button>
          </div>
          <button @click="handleSubmit" class="mobile-add-btn">
            添加
          </button>
        </div>
      </div>

      <div class="mobile-member-list">
        <div
          v-for="member in dataList"
          :key="member.id"
          class="member-card"
        >
          <div class="member-info">
            <img
              :src="member.user?.userAvatar"
              class="clickable-avatar"
              @click="goToUserPage(member.user?.id)"
              alt="用户头像"
            />
            <span class="username">{{ member.user?.userName }}</span>
          </div>
          <div class="member-meta">
            <span class="join-time">
              加入时间：{{ dayjs(member.createTime).format('YYYY-MM-DD') }}
            </span>
            <div class="member-actions">
              <button
                @click="openRoleModal(member)"
                :disabled="member.user?.id === loginUserStore.loginUser?.id || activeStatus !== '1'"
                class="mobile-role-btn"
              >
                {{ getRoleLabel(member.spaceRole) }}
              </button>
              <div class="action-buttons">
                <template v-if="activeStatus !== '1' && member.user?.id !== loginUserStore.loginUser?.id">
                  <button
                    @click="handleAudit(member, 1)"
                    class="mobile-success-btn"
                  >
                    通过
                  </button>
                </template>
                <template v-if="activeStatus !== '2' && member.user?.id !== loginUserStore.loginUser?.id">
                  <button
                    @click="handleAudit(member, 2)"
                    class="mobile-danger-btn"
                  >
                    拒绝
                  </button>
                </template>
                <button
                  @click="toggleRecommended(member)"
                  :class="member.isRecommended === 1 ? 'mobile-recommended-btn' : 'mobile-unrecommended-btn'"
                  :disabled="activeStatus !== '1'"
                >
                  {{ member.isRecommended === 1 ? '取消推荐' : '设为推荐' }}
                </button>
                <button
                  @click="showDeleteConfirm(member)"
                  :disabled="member.user?.id === loginUserStore.loginUser?.id"
                  class="mobile-delete-btn"
                >
                  删除
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <div
      v-if="deleteConfirmVisible"
      class="confirm-modal-overlay"
      @click="deleteConfirmVisible = false"
    >
      <div
        class="confirm-modal"
        @click.stop
      >
        <div class="delete-confirm-content">
          <div class="warning-icon">
            <ExclamationCircleFilled />
          </div>
          <h3 class="confirm-title">确认移除该成员？</h3>
          <p class="confirm-desc">
            用户名称：{{ selectedMember?.user?.userName || '未设置' }}<br>
            用户账号：{{ selectedMember?.user?.userAccount }}
          </p>
          <div class="confirm-actions">
            <button class="cancel-btn" @click="deleteConfirmVisible = false">取消</button>
            <button class="confirm-delete-btn" @click="confirmDelete">
              确认移除
            </button>
          </div>
        </div>
      </div>
    </div>

    <div
      v-if="roleModalVisible"
      class="role-modal-overlay"
      @click="closeRoleModal"
    >
      <div
        class="role-modal"
        @click.stop
      >
        <div class="role-modal-header">
          <h3 class="role-modal-title">修改成员角色</h3>
          <button class="role-modal-close" @click="closeRoleModal">×</button>
        </div>
        <div class="role-modal-body">
          <div class="role-list">
            <div
              v-for="option in SPACE_ROLE_OPTIONS"
              :key="option.value"
              class="role-item"
              :class="{ active: currentMember?.spaceRole === option.value }"
              @click="selectRole(option.value)"
            >
              {{ option.label }}
            </div>
          </div>
        </div>
        <div class="role-modal-footer">
          <button class="role-modal-cancel" @click="closeRoleModal">取消</button>
          <button class="role-modal-confirm" @click="confirmRoleChange">确认修改</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { SPACE_ROLE_OPTIONS } from '../../constants/space.ts'
import {
  addSpaceUserUsingPost,
  deleteSpaceUserUsingPost,
  editSpaceUserUsingPost,
  listSpaceUserUsingPost,
  auditSpaceUserUsingPost,
  setRecommendedMemberUsingPost,
} from '@/api/spaceUserController.ts'
import dayjs from 'dayjs'
import { UserAddOutlined, ExclamationCircleFilled } from '@ant-design/icons-vue'
import { getDeviceType } from '@/utils/device.ts'
import { DEVICE_TYPE_ENUM } from '@/constants/device.ts'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { useRouter } from 'vue-router'

const router = useRouter()
const device = ref<string>('')

onMounted(async () => {
  device.value = await getDeviceType()
})

interface Props {
  id: string
}

const props = defineProps<Props>()

const dataList = ref<API.SpaceUserVO[]>([])
const statusCounts = ref({
  0: 0,
  1: 0,
  2: 0
})

const fetchStatusCounts = async () => {
  const spaceId = props.id
  if (!spaceId) return

  try {
    const promises = [0, 1, 2].map(status =>
      listSpaceUserUsingPost({
        spaceId: spaceId.toString(),
        status
      })
    )

    const results = await Promise.all(promises)
    results.forEach((res, index) => {
      if (res.data?.code === 0) {
        statusCounts.value[index] = res.data.data?.length || 0
      }
    })
  } catch (error) {
    console.error('获取成员数量失败:', error)
  }
}

const fetchData = async () => {
  const spaceId = props.id
  if (!spaceId) {
    return
  }
  const res = await listSpaceUserUsingPost({
    spaceId: spaceId.toString(),
    status: Number(activeStatus.value),
  })
  if (res.data.code === 0 && res.data.data) {
    dataList.value = res.data.data ?? []
  } else {
    message.error('获取数据失败，' + res.data.message)
  }
}

onMounted(() => {
  fetchData()
  fetchStatusCounts()
})

const formData = reactive<API.SpaceUserAddRequest>({})

const handleSubmit = async () => {
  const spaceId = props.id
  if (!spaceId) {
    return
  }
  const res = await addSpaceUserUsingPost({
    spaceId,
    ...formData,
  })
  if (res.data.code === 0) {
    message.success('添加成功')
    fetchData()
    fetchStatusCounts()
    formData.userId = ''
  } else {
    message.error('添加失败，' + res.data.message)
  }
}

const editSpaceRole = async (value, record) => {
  const res = await editSpaceUserUsingPost({
    id: record.id,
    spaceRole: value,
  })
  if (res.data.code === 0) {
    message.success('修改成功')
  } else {
    message.error('修改失败，' + res.data.message)
  }
}

const deleteConfirmVisible = ref(false)
const selectedMember = ref<API.SpaceUserVO | null>(null)

const showDeleteConfirm = (member: API.SpaceUserVO) => {
  selectedMember.value = member
  deleteConfirmVisible.value = true
}

const confirmDelete = async () => {
  if (!selectedMember.value?.id) return

  try {
    const res = await deleteSpaceUserUsingPost({ id: selectedMember.value.id })
    if (res.data.code === 0) {
      message.success('移除成功')
      deleteConfirmVisible.value = false
      fetchData()
      fetchStatusCounts()
    } else {
      message.error('移除失败：' + res.data.message)
    }
  } catch (error) {
    message.error('移除失败，请稍后重试')
  }
}

const loginUserStore = useLoginUserStore()

const activeStatus = ref('1')
const handleStatusChange = (status: string) => {
  activeStatus.value = status
  fetchData()
}

const handleAudit = async (record: API.SpaceUserVO, status: number) => {
  try {
    const res = await auditSpaceUserUsingPost({
      spaceId: props.id.toString(),
      userId: record.userId,
      status,
    })
    if (res.data?.code === 0) {
      message.success(status === 1 ? '已通过申请' : '已拒绝申请')
      fetchData()
      fetchStatusCounts()
    }
  } catch (error) {
    message.error('操作失败')
  }
}

const toggleRecommended = async (record: API.SpaceUserVO) => {
  try {
    const currentRecommended = record.isRecommended === 1 ? 1 : 0;
    const newRecommended = currentRecommended === 1 ? 0 : 1;

    const res = await setRecommendedMemberUsingPost({
      spaceId: props.id,
      userId: record.userId,
      isRecommended: Number(newRecommended),
    })
    if (res.data?.code === 0) {
      message.success(currentRecommended === 1 ? '已取消推荐' : '已设为推荐')
      record.isRecommended = Number(newRecommended);
    } else {
      message.error('操作失败：' + res.data.message)
    }
  } catch (error: any) {
    if (error.message && error.message.includes('推荐成员数量已达上限')) {
      message.error('推荐成员数量已达上限（10个），无法添加更多推荐成员');
    } else {
      message.error('操作失败');
    }
  }
}

const goToUserPage = (userId?: number) => {
  if (userId) {
    router.push(`/user/${userId}`)
  }
}

const roleModalVisible = ref(false)
const currentMember = ref<API.SpaceUserVO | null>(null)
const selectedRole = ref('')

const openRoleModal = (member: API.SpaceUserVO) => {
  currentMember.value = member
  selectedRole.value = member.spaceRole
  roleModalVisible.value = true
}

const closeRoleModal = () => {
  roleModalVisible.value = false
  currentMember.value = null
  selectedRole.value = ''
}

const selectRole = (value: string) => {
  selectedRole.value = value
}

const confirmRoleChange = async () => {
  if (!currentMember.value || !selectedRole.value) return

  await editSpaceRole(selectedRole.value, currentMember.value)
  currentMember.value.spaceRole = selectedRole.value
  roleModalVisible.value = false
}

const getRoleLabel = (value: string) => {
  const option = SPACE_ROLE_OPTIONS.find(item => item.value === value)
  return option ? option.label : '未知角色'
}
</script>

<style scoped>
.page-container {
  padding: 24px;
  background: var(--background);
  min-height: 100vh;
  margin: 0 auto;
  max-width: 1400px;
  transition: var(--theme-transition);
}

.header-section {
  margin-bottom: 28px;
}

.page-title {
  color: var(--post-title-color);
  font-size: 28px;
  margin: 0 0 20px 0;
  font-weight: 700;
  letter-spacing: -0.5px;
}

.status-tabs {
  margin-bottom: 24px;
}

.custom-tabs {
  display: flex;
  background: var(--card-background);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-color);
}

.custom-tab {
  flex: 1;
  padding: 14px 20px;
  text-align: center;
  color: var(--text-secondary);
  font-size: 16px;
  cursor: pointer;
  transition: var(--theme-transition);
  border-bottom: 3px solid transparent;
}

.custom-tab:hover {
  color: var(--link-color);
  background: var(--hover-background);
}

.custom-tab.active {
  color: var(--link-color);
  font-weight: 600;
  border-bottom: 3px solid var(--link-color);
  background: var(--card-background);
}

.tab-label {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.custom-badge {
  min-width: 22px;
  height: 22px;
  line-height: 22px;
  border-radius: var(--radius-full);
  background: var(--hover-background);
  color: var(--text-primary);
  font-size: 12px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 6px;
}

.custom-badge.active {
  background: var(--link-color);
  color: var(--text-other);
}

.search-form {
  margin-bottom: 28px;
  padding: 24px;
  background: var(--card-background);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow);
  border: 1px solid var(--border-color);
}

.inline-form {
  display: flex;
  align-items: center;
  gap: 20px;
}

.form-item {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 0;
}

.form-label {
  color: var(--text-primary);
  font-weight: 600;
  font-size: 16px;
  min-width: 70px;
}

.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  flex: 1;
}

.search-input {
  width: 100%;
  height: 48px;
  padding: 0 40px 0 16px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-full);
  background: var(--hover-background);
  color: var(--text-primary);
  font-size: 16px;
  outline: none;
  transition: var(--theme-transition);
}

.search-input:focus {
  border-color: var(--link-color);
  background: var(--card-background);
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.1);
}

.search-input::placeholder {
  color: var(--text-secondary);
}

.clear-btn {
  position: absolute;
  right: 12px;
  background: none;
  border: none;
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 20px;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
}

.clear-btn:hover {
  color: var(--text-primary);
  background: var(--hover-background);
}

.primary-btn {
  height: 48px;
  padding: 0 32px;
  background: var(--link-color);
  border: none;
  border-radius: var(--radius-full);
  color: var(--text-other);
  font-weight: 600;
  font-size: 16px;
  cursor: pointer;
  transition: var(--theme-transition);
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.primary-btn:hover {
  background: var(--link-hover-color);
  transform: translateY(-1px);
  box-shadow: var(--shadow);
}

.primary-btn:active {
  transform: translateY(0);
}

.add-member-btn {
  white-space: nowrap;
}

.member-table {
  background: var(--card-background);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow);
  border: 1px solid var(--border-color);
}

.table-header {
  display: flex;
  background: var(--hover-background);
  color: var(--text-primary);
  font-weight: 600;
  border-bottom: 1px solid var(--border-color);
}

.table-header .table-cell {
  padding: 16px 20px;
  flex: 1;
  text-align: left;
  font-size: 16px;
}

.table-row {
  display: flex;
  border-bottom: 1px solid var(--border-color);
  transition: var(--theme-transition);
}

.table-row:hover {
  background: var(--hover-background);
  border-radius: var(--radius);
  margin: 0 8px;
}

.table-row:last-child {
  border-bottom: none;
}

.table-cell {
  padding: 20px;
  flex: 1;
  text-align: left;
  vertical-align: middle;
  font-size: 15px;
  color: var(--text-primary);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.clickable-avatar {
  cursor: pointer;
  transition: var(--theme-transition);
  width: 40px;
  height: 40px;
  border-radius: var(--radius-full);
  object-fit: cover;
  border: 2px solid var(--border-color);
}

.clickable-avatar:hover {
  transform: scale(1.05);
  box-shadow: var(--shadow-sm);
}

.role-select {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius);
  background: var(--card-background);
  color: var(--text-primary);
  font-size: 15px;
  outline: none;
  cursor: pointer;
  transition: var(--theme-transition);
}

.role-select:hover:not(:disabled) {
  border-color: var(--link-color);
}

.role-select:focus:not(:disabled) {
  border-color: var(--link-color);
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.1);
}

.role-select:disabled {
  background: var(--hover-background);
  cursor: not-allowed;
  opacity: 0.7;
  color: var(--text-secondary);
}

.action-buttons {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.success-btn {
  background: var(--markdown-heading-green-border);
  border: none;
  border-radius: var(--radius-full);
  color: var(--text-other);
  padding: 8px 16px;
  cursor: pointer;
  transition: var(--theme-transition);
  font-size: 14px;
  font-weight: 500;
}

.success-btn:hover {
  background: #16a34a;
  transform: translateY(-1px);
  box-shadow: var(--shadow-sm);
}

.success-btn:disabled {
  background: var(--border-color);
  cursor: not-allowed;
  opacity: 0.7;
}

.danger-btn {
  background: var(--comment-delete-hover-color);
  border: none;
  border-radius: var(--radius-full);
  color: var(--text-other);
  padding: 8px 16px;
  cursor: pointer;
  transition: var(--theme-transition);
  font-size: 14px;
  font-weight: 500;
}

.danger-btn:hover {
  background: #e53935;
  transform: translateY(-1px);
  box-shadow: var(--shadow-sm);
}

.danger-btn:disabled {
  background: var(--border-color);
  cursor: not-allowed;
  opacity: 0.7;
}

.delete-btn {
  color: var(--comment-delete-hover-color);
  border: 1px solid var(--comment-delete-hover-color);
  background: transparent;
  padding: 8px 16px;
  cursor: pointer;
  border-radius: var(--radius-full);
  transition: var(--theme-transition);
  font-size: 14px;
  font-weight: 500;
}

.delete-btn:hover:not(:disabled) {
  background: rgba(255, 77, 79, 0.1);
}

.delete-btn:disabled {
  color: var(--text-secondary);
  border-color: var(--border-color);
  cursor: not-allowed;
  background: transparent;
}

.recommended-btn {
  color: var(--markdown-heading-green-border);
  border: 1px solid var(--markdown-heading-green-border);
  background: transparent;
  padding: 8px 16px;
  cursor: pointer;
  border-radius: var(--radius-full);
  transition: var(--theme-transition);
  font-size: 14px;
  font-weight: 500;
}

.recommended-btn:hover:not(:disabled) {
  background: rgba(34, 197, 94, 0.1);
}

.recommended-btn:disabled {
  color: var(--text-secondary);
  border-color: var(--border-color);
  cursor: not-allowed;
  background: transparent;
}

.unrecommended-btn {
  color: var(--link-color);
  border: 1px solid var(--link-color);
  background: transparent;
  padding: 8px 16px;
  cursor: pointer;
  border-radius: var(--radius-full);
  transition: var(--theme-transition);
  font-size: 14px;
  font-weight: 500;
}

.unrecommended-btn:hover:not(:disabled) {
  background: rgba(59, 130, 246, 0.1);
}

.unrecommended-btn:disabled {
  color: var(--text-secondary);
  border-color: var(--border-color);
  cursor: not-allowed;
  background: transparent;
}

.mobile-tabs {
  display: flex;
  background: var(--card-background);
  border-radius: var(--radius-lg);
  overflow: hidden;
  margin-bottom: 16px;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-color);
}

.mobile-tab {
  flex: 1;
  padding: 12px 16px;
  text-align: center;
  color: var(--text-secondary);
  font-size: 15px;
  cursor: pointer;
  transition: var(--theme-transition);
  border-bottom: 2px solid transparent;
}

.mobile-tab:hover {
  color: var(--link-color);
  background: var(--hover-background);
}

.mobile-tab.active {
  color: var(--link-color);
  font-weight: 600;
  border-bottom: 2px solid var(--link-color);
}

.mobile-badge {
  background: var(--hover-background);
  color: var(--text-primary);
  border-radius: var(--radius-full);
  font-size: 11px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 5px;
  min-width: 18px;
  height: 18px;
  margin-left: 4px;
}

.mobile-badge.active {
  background: var(--link-color);
  color: var(--text-other);
}

.mobile-search-container {
  margin: 16px 0;
}

.mobile-search {
  display: flex;
  align-items: center;
  background: var(--card-background);
  border-radius: var(--radius-full);
  padding: 4px;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-color);
}

.mobile-search-input-wrapper {
  flex: 1;
  display: flex;
  align-items: center;
  position: relative;
}

.mobile-search-input {
  flex: 1;
  border: none;
  background: transparent;
  padding: 12px 36px 12px 16px;
  color: var(--text-primary);
  font-size: 15px;
  outline: none;
}

.mobile-search-input::placeholder {
  color: var(--text-secondary);
}

.mobile-clear-btn {
  position: absolute;
  right: 8px;
  background: none;
  border: none;
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 18px;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
}

.mobile-clear-btn:hover {
  color: var(--text-primary);
  background: var(--hover-background);
}

.mobile-add-btn {
  background: var(--link-color);
  border: none;
  border-radius: var(--radius-full);
  padding: 8px 20px;
  color: var(--text-other);
  cursor: pointer;
  font-size: 15px;
  font-weight: 600;
  margin-left: 4px;
  transition: var(--theme-transition);
}

.mobile-add-btn:active {
  background: var(--link-hover-color);
}

.mobile-member-list {
  padding: 0;
}

.member-card {
  margin-bottom: 12px;
  border-radius: var(--radius-lg);
  background: var(--card-background);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
  padding: 16px;
  border: 1px solid var(--border-color);
  transition: var(--theme-transition);
}

.member-card:hover {
  box-shadow: var(--shadow);
  transform: translateY(-2px);
}

.member-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.username {
  font-size: 17px;
  font-weight: 600;
  color: var(--text-primary);
}

.member-meta {
  margin-top: 12px;
}

.join-time {
  color: var(--text-secondary);
  font-size: 14px;
  display: block;
  margin-bottom: 12px;
}

.member-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.mobile-role-btn {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-full);
  background: var(--hover-background);
  color: var(--text-primary);
  font-size: 14px;
  outline: none;
  cursor: pointer;
  transition: var(--theme-transition);
  min-width: 100px;
  text-align: center;
}

.mobile-role-btn:hover:not(:disabled) {
  border-color: var(--link-color);
  background: var(--card-background);
}

.mobile-role-btn:disabled {
  background: var(--border-color);
  cursor: not-allowed;
  opacity: 0.7;
}

.mobile-success-btn {
  background: var(--markdown-heading-green-border);
  border: none;
  border-radius: var(--radius-full);
  color: var(--text-other);
  padding: 8px 14px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  transition: var(--theme-transition);
}

.mobile-success-btn:hover {
  background: #16a34a;
}

.mobile-success-btn:disabled {
  background: var(--border-color);
  cursor: not-allowed;
  opacity: 0.7;
}

.mobile-danger-btn {
  background: var(--comment-delete-hover-color);
  border: none;
  border-radius: var(--radius-full);
  color: var(--text-other);
  padding: 8px 14px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  transition: var(--theme-transition);
}

.mobile-danger-btn:hover {
  background: #e53935;
}

.mobile-danger-btn:disabled {
  background: var(--border-color);
  cursor: not-allowed;
  opacity: 0.7;
}

.mobile-delete-btn {
  color: var(--comment-delete-hover-color);
  border: 1px solid var(--comment-delete-hover-color);
  background: transparent;
  padding: 8px 14px;
  cursor: pointer;
  border-radius: var(--radius-full);
  font-size: 13px;
  font-weight: 500;
  transition: var(--theme-transition);
}

.mobile-delete-btn:hover:not(:disabled) {
  background: rgba(255, 77, 79, 0.1);
}

.mobile-delete-btn:disabled {
  color: var(--text-secondary);
  border-color: var(--border-color);
  cursor: not-allowed;
}

.mobile-recommended-btn {
  color: var(--markdown-heading-green-border);
  border: 1px solid var(--markdown-heading-green-border);
  background: transparent;
  padding: 8px 14px;
  cursor: pointer;
  border-radius: var(--radius-full);
  font-size: 13px;
  font-weight: 500;
  transition: var(--theme-transition);
}

.mobile-recommended-btn:hover:not(:disabled) {
  background: rgba(34, 197, 94, 0.1);
}

.mobile-recommended-btn:disabled {
  color: var(--text-secondary);
  border-color: var(--border-color);
  cursor: not-allowed;
}

.mobile-unrecommended-btn {
  color: var(--link-color);
  border: 1px solid var(--link-color);
  background: transparent;
  padding: 8px 14px;
  cursor: pointer;
  border-radius: var(--radius-full);
  font-size: 13px;
  font-weight: 500;
  transition: var(--theme-transition);
}

.mobile-unrecommended-btn:hover:not(:disabled) {
  background: rgba(59, 130, 246, 0.1);
}

.mobile-unrecommended-btn:disabled {
  color: var(--text-secondary);
  border-color: var(--border-color);
  cursor: not-allowed;
}

.confirm-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: var(--comment-drawer-backdrop);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(4px);
}

.confirm-modal {
  background: var(--card-background);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-lg);
  width: 90%;
  max-width: 400px;
  padding: 0;
  overflow: hidden;
  border: 1px solid var(--border-color);
}

.delete-confirm-content {
  padding: 32px 24px;
  text-align: center;
}

.warning-icon {
  font-size: 56px;
  color: var(--link-color);
  margin-bottom: 20px;
}

.warning-icon :deep(.anticon) {
  animation: pulse 2s infinite;
}

.confirm-title {
  font-size: 20px;
  color: var(--text-primary);
  margin-bottom: 16px;
  font-weight: 700;
}

.confirm-desc {
  font-size: 15px;
  color: var(--text-secondary);
  line-height: 1.6;
  margin-bottom: 28px;
}

.confirm-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.cancel-btn {
  padding: 12px 24px;
  border: 1px solid var(--border-color);
  background: var(--card-background);
  color: var(--text-primary);
  border-radius: var(--radius-full);
  font-weight: 600;
  font-size: 16px;
  cursor: pointer;
  transition: var(--theme-transition);
  flex: 1;
}

.cancel-btn:hover {
  background: var(--hover-background);
  border-color: var(--text-secondary);
}

.confirm-delete-btn {
  padding: 12px 24px;
  background: var(--comment-delete-hover-color);
  border: none;
  border-radius: var(--radius-full);
  color: var(--text-other);
  font-weight: 600;
  font-size: 16px;
  cursor: pointer;
  transition: var(--theme-transition);
  flex: 1;
}

.confirm-delete-btn:hover {
  background: #e53935;
}

.role-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: var(--comment-drawer-backdrop);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1001;
  backdrop-filter: blur(4px);
}

.role-modal {
  background: var(--card-background);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-lg);
  width: 90%;
  max-width: 350px;
  padding: 0;
  overflow: hidden;
  border: 1px solid var(--border-color);
}

.role-modal-header {
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.role-modal-title {
  font-size: 18px;
  color: var(--text-primary);
  font-weight: 600;
  margin: 0;
}

.role-modal-close {
  background: none;
  border: none;
  font-size: 20px;
  color: var(--text-secondary);
  cursor: pointer;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: var(--theme-transition);
}

.role-modal-close:hover {
  background: var(--hover-background);
  color: var(--text-primary);
}

.role-modal-body {
  padding: 20px;
}

.role-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.role-item {
  padding: 12px 16px;
  border-radius: var(--radius);
  color: var(--text-primary);
  font-size: 16px;
  cursor: pointer;
  transition: var(--theme-transition);
  text-align: center;
}

.role-item:hover {
  background: var(--hover-background);
}

.role-item.active {
  background: var(--link-color);
  color: var(--text-other);
  font-weight: 600;
}

.role-modal-footer {
  padding: 16px 20px;
  border-top: 1px solid var(--border-color);
  display: flex;
  gap: 12px;
}

.role-modal-cancel {
  flex: 1;
  padding: 10px;
  border: 1px solid var(--border-color);
  background: var(--card-background);
  color: var(--text-primary);
  border-radius: var(--radius-full);
  font-weight: 600;
  font-size: 15px;
  cursor: pointer;
  transition: var(--theme-transition);
}

.role-modal-cancel:hover {
  background: var(--hover-background);
  border-color: var(--text-secondary);
}

.role-modal-confirm {
  flex: 1;
  padding: 10px;
  background: var(--link-color);
  border: none;
  border-radius: var(--radius-full);
  color: var(--text-other);
  font-weight: 600;
  font-size: 15px;
  cursor: pointer;
  transition: var(--theme-transition);
}

.role-modal-confirm:hover {
  background: var(--link-hover-color);
}

@keyframes pulse {
  0% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.05); opacity: 0.8; }
  100% { transform: scale(1); opacity: 1; }
}

@media screen and (max-width: 768px) {
  .page-container {
    padding: 16px;
    max-width: 100%;
  }

  .page-title {
    font-size: 24px;
    margin-bottom: 16px;
  }

  .inline-form {
    flex-direction: column;
    gap: 16px;
    align-items: stretch;
  }

  .form-item {
    width: 100%;
  }

  .search-input {
    width: 100%;
  }

  .primary-btn {
    width: 100%;
    justify-content: center;
  }

  .table-header .table-cell,
  .table-cell {
    padding: 12px 16px;
  }

  .action-buttons {
    justify-content: flex-start;
  }

  .delete-confirm-content {
    padding: 24px 16px;
  }

  .warning-icon {
    font-size: 48px;
  }

  .confirm-title {
    font-size: 18px;
  }

  .cancel-btn, .confirm-delete-btn {
    padding: 10px 16px;
    font-size: 15px;
  }
}

@media screen and (max-width: 480px) {
  .table-cell {
    padding: 12px 8px;
    font-size: 14px;
  }

  .action-buttons {
    gap: 6px;
  }

  .success-btn, .danger-btn, .delete-btn, .recommended-btn, .unrecommended-btn {
    padding: 6px 10px;
    font-size: 13px;
  }
}
</style>
