<template>
  <div id="activityManagePage">
    <!-- PC端展示 -->
    <template v-if="device === DEVICE_TYPE_ENUM.PC">
      <div class="pc-container">
        <!-- 搜索表单和操作按钮 -->
        <div class="search-and-button-container">
          <a-form layout="inline" :model="searchParams" @finish="doSearch">
            <a-form-item label="关键词">
              <a-input
                v-model="searchParams.searchText"
                placeholder="从标题和内容搜索"
                allow-clear
                class="compact-input"
              />
            </a-form-item>
            <a-form-item name="status" label="审核状态">
              <a-select
                v-model="searchParams.status"
                class="compact-select"
                placeholder="请选择审核状态"
                :options="ACTIVITY_STATUS_OPTIONS"
                allow-clear
              />
            </a-form-item>
            <a-form-item>
              <a-button type="primary" html-type="submit" class="action-button search-button">
                <SearchOutlined />
                搜索
              </a-button>
            </a-form-item>
          </a-form>
          <a-button type="primary" class="create-button" @click="handleCreate">
            <PlusOutlined />
            创建活动
          </a-button>
        </div>

        <div class="table-section">
          <a-spin tip="正在加载中..." v-if="loading" class="loading-spin" />
          <!-- 表格 -->
          <a-table
            rowKey="id"
            :columns="columns"
            :data-source="dataList"
            :pagination="false"
            @change="doTableChange"
            class="custom-table"
            :scroll="{ x: 1200 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'title'">
                <div class="activity-title-cell">
                  <span class="title-text">{{ record.title }}</span>
                </div>
              </template>
              <template v-if="column.dataIndex === 'content'">
                <div class="activity-content-cell">
                  <a-button type="link" @click="handleView(record)">
                    查看内容
                  </a-button>
                </div>
              </template>
              <template v-if="column.dataIndex === 'userInfo'">
                <div class="user-info-cell">
                  <a-avatar :size="24" :src="record.user?.userAvatar" />
                  <span>{{ record.user?.userName }}</span>
                </div>
              </template>
              <template v-if="column.dataIndex === 'stats'">
                <div class="stats-cell">
                  <span>浏览: {{ record.viewCount }}</span>
                </div>
              </template>
              <template v-if="column.dataIndex === 'status'">
                <a-tag :color="getStatusColor(record.status)">
                  {{ ACTIVITY_STATUS_MAP[record.status] }}
                </a-tag>
              </template>
              <template v-if="column.dataIndex === 'expireTime'">
                <div class="expire-time-cell">
                  <span>{{ dayjs(record.expireTime).format('YYYY-MM-DD HH:mm') }}</span>
                  <a-tag :color="record.isExpired === 1 ? 'error' : 'success'">
                    {{ record.isExpired === 1 ? '已过期' : '进行中' }}
                  </a-tag>
                </div>
              </template>
              <template v-if="column.dataIndex === 'createTime'">
                {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}
              </template>
              <template v-if="column.key === 'action'">
                <a-space wrap>
                  <a-button
                    v-if="record.status !== ACTIVITY_STATUS_ENUM.PASS"
                    type="primary"
                    class="table-button approve-button"
                    @click="handleReview(record, ACTIVITY_STATUS_ENUM.PASS)"
                  >
                    <CheckOutlined />
                    通过
                  </a-button>
                  <a-button
                    v-if="record.status !== ACTIVITY_STATUS_ENUM.REJECT"
                    type="primary"
                    danger
                    class="table-button reject-button"
                    @click="handleReview(record, ACTIVITY_STATUS_ENUM.REJECT)"
                  >
                    <CloseOutlined />
                    拒绝
                  </a-button>
                  <a-button
                    danger
                    class="table-button delete-button"
                    @click="showDeleteConfirm(record)"
                  >
                    <DeleteOutlined />
                    删除
                  </a-button>
                </a-space>
              </template>
            </template>
          </a-table>

          <!-- PC端分页器 -->
          <div class="pagination-wrapper">
            <a-pagination
              v-model="searchParams.current"
              :total="total"
              :pageSize="searchParams.pageSize"
              :pageSizeOptions="['10', '20', '30', '40']"
              show-size-changer
              show-quick-jumper
              :showTotal="total => `共 ${total} 条`"
              @change="handlePageChange"
              @showSizeChange="handleSizeChange"
            />
          </div>
        </div>
      </div>
    </template>

    <!-- 移动端展示 -->
    <template v-else>
      <div class="mobile-container">


        <!-- 搜索区域 -->
        <div class="mobile-search">
          <a-input-search
            v-model="searchParams.searchText"
            placeholder="搜索活动"
            @search="doSearch"
            class="mobile-search-input"
          />
          <a-select
            v-model="searchParams.status"
            class="mobile-status-select"
            placeholder="审核状态"
            :options="ACTIVITY_STATUS_OPTIONS"
            allow-clear
          />
        </div>

        <!-- 顶部操作区 -->
        <div class="mobile-header">
          <a-button type="primary" class="mobile-create-button" @click="handleCreate">
            <PlusOutlined />
            创建活动
          </a-button>
        </div>

        <!-- 活动列表 -->
        <div class="mobile-list-wrapper">
          <a-list
            :loading="loading"
            :data-source="dataList"
            class="mobile-activity-list"
          >
            <template #renderItem="{ item: activity }">
              <a-list-item class="mobile-activity-item">
                <div class="mobile-activity-content">
                  <div class="mobile-activity-header">
                    <span class="mobile-activity-title">{{ activity.title }}</span>
                    <a-tag :color="getStatusColor(activity.status)">
                      {{ ACTIVITY_STATUS_MAP[activity.status] }}
                    </a-tag>
                  </div>

                  <div class="mobile-activity-info">
                    <div class="mobile-user-info">
                      <a-avatar :size="20" :src="activity.user?.userAvatar" />
                      <span>{{ activity.user?.userName }}</span>
                    </div>
                    <span class="mobile-activity-time">
                      {{ dayjs(activity.createTime).format('YYYY-MM-DD HH:mm') }}
                    </span>
                  </div>

                  <div class="mobile-activity-expire">
                    <span>截止时间：{{ dayjs(activity.expireTime).format('YYYY-MM-DD HH:mm') }}</span>
                    <a-tag :color="activity.isExpired === 1 ? 'error' : 'success'">
                      {{ activity.isExpired === 1 ? '已过期' : '进行中' }}
                    </a-tag>
                  </div>

                  <div class="mobile-activity-actions">
                    <a-space>
                      <a-button
                        v-if="activity.status !== ACTIVITY_STATUS_ENUM.PASS"
                        type="primary"
                        size="small"
                        @click="handleReview(activity, ACTIVITY_STATUS_ENUM.PASS)"
                      >
                        通过
                      </a-button>
                      <a-button
                        v-if="activity.status !== ACTIVITY_STATUS_ENUM.REJECT"
                        type="primary"
                        danger
                        size="small"
                        @click="handleReview(activity, ACTIVITY_STATUS_ENUM.REJECT)"
                      >
                        拒绝
                      </a-button>
                      <a-button
                        type="link"
                        size="small"
                        @click="handleView(activity)"
                      >
                        查看
                      </a-button>
                      <a-button
                        type="link"
                        danger
                        size="small"
                        @click="showDeleteConfirm(activity)"
                      >
                        删除
                      </a-button>
                    </a-space>
                  </div>
                </div>
              </a-list-item>
            </template>
          </a-list>
        </div>

        <!-- 移动端分页 -->
        <div class="mobile-pagination">
          <div class="page-info">
            <span>第 {{ searchParams.current }} 页</span>
            <span class="separator">/</span>
            <span>共 {{ Math.ceil(total / searchParams.pageSize) }} 页</span>
          </div>
          <div class="page-actions">
            <a-button
              :disabled="searchParams.current === 1"
              @click="handlePrevPage"
              class="page-button"
            >
              上一页
            </a-button>
            <a-select
              v-model="searchParams.pageSize"
              style="width: 110px"
              @change="handlePageSizeChange"
            >
              <a-select-option :value="10">10 条/页</a-select-option>
              <a-select-option :value="20">20 条/页</a-select-option>
              <a-select-option :value="30">30 条/页</a-select-option>
              <a-select-option :value="40">40 条/页</a-select-option>
            </a-select>
            <a-button
              :disabled="searchParams.current >= Math.ceil(total / searchParams.pageSize)"
              @click="handleNextPage"
              class="page-button"
            >
              下一页
            </a-button>
          </div>
        </div>
      </div>
    </template>

    <!-- 拒绝原因模态框 -->
    <a-modal
      v-model:open="rejectModalVisible"
      title="拒绝原因"
      @ok="handleRejectConfirm"
      :confirmLoading="confirmLoading"
      class="reject-modal"
      okText="确认拒绝"
      cancelText="取消"
    >
      <div class="reject-form">
        <div class="reason-select">
          <div class="form-label">快捷原因：</div>
          <a-select
            v-model:value="selectedReason"
            placeholder="请选择拒绝原因"
            style="width: 100%"
            @change="handleReasonSelect"
          >
            <a-select-option
              v-for="option in ACTIVITY_REJECT_REASON_OPTIONS"
              :key="option.value"
              :value="option.value"
            >
              {{ option.label }}
            </a-select-option>
          </a-select>
        </div>
        <div class="reason-input">
          <div class="form-label">详细说明：</div>
          <a-textarea
            v-model:value="rejectMessage"
            placeholder="请输入拒绝原因"
            :rows="4"
            :maxLength="200"
            show-count
          />
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, h } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import dayjs from 'dayjs'
import {
  SearchOutlined,
  CheckOutlined,
  CloseOutlined,
  DeleteOutlined,
  ExclamationCircleOutlined,
  PlusOutlined,
} from '@ant-design/icons-vue'
import { DEVICE_TYPE_ENUM } from '@/constants/device'
import { getDeviceType } from '@/utils/device'
import { ACTIVITY_REJECT_REASON_OPTIONS, ACTIVITY_REJECT_REASON_MAP } from '@/constants/review'
import { listActivityByPageUsingPost as listActivitiesUsingPost, reviewActivityUsingPost, deleteActivityUsingPost } from '@/api/activityController'

// 状态枚举
const ACTIVITY_STATUS_ENUM = {
  REVIEWING: 0,
  PASS: 1,
  REJECT: 2,
}

// 状态映射
const ACTIVITY_STATUS_MAP = {
  [ACTIVITY_STATUS_ENUM.REVIEWING]: '待审核',
  [ACTIVITY_STATUS_ENUM.PASS]: '已通过',
  [ACTIVITY_STATUS_ENUM.REJECT]: '已拒绝',
}

// 状态选项
const ACTIVITY_STATUS_OPTIONS = [
  { label: '待审核', value: ACTIVITY_STATUS_ENUM.REVIEWING },
  { label: '已通过', value: ACTIVITY_STATUS_ENUM.PASS },
  { label: '已拒绝', value: ACTIVITY_STATUS_ENUM.REJECT },
]

// 表格列定义
const columns = [
  { title: '标题', dataIndex: 'title', width: 200 },
  { title: '内容', dataIndex: 'content', width: 120 },
  { title: '作者', dataIndex: 'userInfo', width: 120 },
  { title: '数据', dataIndex: 'stats', width: 100 },
  { title: '状态', dataIndex: 'status', width: 100 },
  { title: '过期时间', dataIndex: 'expireTime', width: 180 },
  { title: '创建时间', dataIndex: 'createTime', width: 160 },
  { title: '操作', key: 'action', fixed: 'right', width: 200 },
]

const router = useRouter()
const device = ref(getDeviceType())
const loading = ref(false)
const dataList = ref<API.Activity[]>([])
const searchParams = ref({
  searchText: '',
  status: undefined,
  isPublic: false,
  current: 1,
  pageSize: 10,
})
const total = ref(0)

// 获取状态颜色
const getStatusColor = (status: number) => {
  switch (status) {
    case ACTIVITY_STATUS_ENUM.REVIEWING:
      return 'warning'
    case ACTIVITY_STATUS_ENUM.PASS:
      return 'success'
    case ACTIVITY_STATUS_ENUM.REJECT:
      return 'error'
    default:
      return 'default'
  }
}

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const res = await listActivitiesUsingPost({
      ...searchParams.value,
      current: searchParams.value.current,
      pageSize: searchParams.value.pageSize,
    })
    if (res.data?.data) {
      dataList.value = res.data.data.records
      total.value = res.data.data.total
    }
  } catch (error: any) {
    message.error('获取数据失败：' + error.message)
  } finally {
    loading.value = false
  }
}

// 搜索
const doSearch = () => {
  searchParams.value.current = 1
  loadData()
}

// 表格变化
const doTableChange = (pag: any) => {
  searchParams.value.current = pag.current
  searchParams.value.pageSize = pag.pageSize
  loadData()
}

// 查看活动
const handleView = (record: API.Activity) => {
  router.push(`/activity/detail/${record.id}`)
}

// 删除确认
const showDeleteConfirm = (record: API.Activity) => {
  Modal.confirm({
    class: 'delete-confirm-modal',
    icon: null,
    content: h('div', { class: 'delete-confirm-content' }, [
      h('div', { class: 'warning-icon' }, [
        h(ExclamationCircleOutlined, { style: { color: '#ff4d4f' } })
      ]),
      h('div', { class: 'confirm-title' }, '确定要删除这个活动吗？'),
      h('div', { class: 'confirm-desc' }, '删除后将无法恢复，请谨慎操作'),
    ]),
    okText: '删除',
    okButtonProps: {
      class: 'confirm-button'
    },
    cancelText: '取消',
    cancelButtonProps: {
      class: 'cancel-button'
    },
    async onOk() {
      try {
        await deleteActivityUsingPost({ id: record.id })
        message.success('删除成功')
        loadData()
      } catch (error: any) {
        message.error('删除失败：' + error.message)
      }
    },
  })
}

// 审核相关
const rejectModalVisible = ref(false)
const rejectMessage = ref('')
const selectedReason = ref('')
const confirmLoading = ref(false)
const currentActivity = ref<API.Activity>()

// 显示拒绝模态框
const showRejectModal = (activity: API.Activity) => {
  currentActivity.value = activity
  rejectMessage.value = ''
  selectedReason.value = ''
  rejectModalVisible.value = true
}

// 处理拒绝原因选择
const handleReasonSelect = (value: string) => {
  selectedReason.value = value
  rejectMessage.value = ACTIVITY_REJECT_REASON_MAP[value] || ''
}

// 处理审核
const handleReview = async (activity: API.Activity, status: number) => {
  // 如果是通过操作，显示确认弹框
  if (status === ACTIVITY_STATUS_ENUM.PASS) {
    Modal.confirm({
      title: '确认通过',
      content: h('div', { class: 'review-confirm-content' }, [
        h('div', { class: 'confirm-title' }, '确定要通过这个活动吗？'),
        h('div', { class: 'confirm-desc' }, '通过后活动将对所有用户可见'),
      ]),
      okText: '确认通过',
      cancelText: '取消',
      async onOk() {
        try {
          await reviewActivityUsingPost({
            activityId: activity.id,
            status,
          })
          message.success('操作成功')
          loadData()
        } catch (error: any) {
          message.error('操作失败：' + error.message)
        }
      },
    })
    return
  }

  // 如果是拒绝操作，显示拒绝原因弹框
  if (status === ACTIVITY_STATUS_ENUM.REJECT) {
    showRejectModal(activity)
    return
  }

  // 其他状态的处理
  try {
    await reviewActivityUsingPost({
      activityId: activity.id,
      status,
    })
    message.success('操作成功')
    loadData()
  } catch (error: any) {
    message.error('操作失败：' + error.message)
  }
}

const handleRejectConfirm = async () => {
  if (!currentActivity.value) return

  confirmLoading.value = true
  try {
    await reviewActivityUsingPost({
      activityId: currentActivity.value.id,
      status: ACTIVITY_STATUS_ENUM.REJECT,
      message: rejectMessage.value,
    })
    message.success('操作成功')
    rejectModalVisible.value = false
    rejectMessage.value = ''
    loadData()
  } catch (error: any) {
    message.error('操作失败：' + error.message)
  } finally {
    confirmLoading.value = false
  }
}

// 分页处理方法
const handlePageChange = (page: number, pageSize: number) => {
  searchParams.value.current = page
  searchParams.value.pageSize = pageSize
  loadData()
}

const handleSizeChange = (current: number, size: number) => {
  searchParams.value.current = 1
  searchParams.value.pageSize = size
  loadData()
}

const handlePrevPage = () => {
  if (searchParams.value.current > 1) {
    searchParams.value.current--
    loadData()
  }
}

const handleNextPage = () => {
  if (searchParams.value.current < Math.ceil(total.value / searchParams.value.pageSize)) {
    searchParams.value.current++
    loadData()
  }
}

const handlePageSizeChange = (value: number) => {
  searchParams.value.current = 1
  searchParams.value.pageSize = value
  loadData()
}

// 创建活动
const handleCreate = () => {
  router.push('/activity/edit')
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
#activityManagePage {
  padding: 24px;
  min-height: calc(100vh - 64px - 80px);
  background: #f8fafc;
  margin: -24px;
}

.pc-container {
  max-width: 100%;
  margin: 0 auto;
  padding: 24px;
  padding-bottom: 80px;
}

/* 搜索区域样式 */
.search-and-button-container {
  background: white;
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  margin-bottom: 24px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 16px;
}

.compact-input {
  width: 200px;
}

.compact-select {
  width: 160px;
}

/* 表格区域样式 */
.table-section {
  background: white;
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  position: relative;
}

.loading-spin {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 1;
}

/* 表格内容样式 */
.activity-title-cell {
  display: flex;
  align-items: center;
  gap: 8px;

  .title-text {
    font-weight: 500;
  }
}

.activity-content-cell {
  color: #666;
  font-size: 13px;
}

.user-info-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.stats-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 13px;
  color: #666;
}

.expire-time-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

/* 操作按钮样式 */
.table-button {
  display: flex;
  align-items: center;
  gap: 4px;
  border-radius: 6px;
  padding: 4px 12px;
  height: 32px;
  transition: all 0.3s ease;

  &.approve-button {
    background: #10b981;
    border-color: #10b981;

    &:hover {
      background: #059669;
      border-color: #059669;
    }
  }

  &.reject-button:hover {
    background: #dc2626;
    border-color: #dc2626;
  }
}

/* 移动端样式 */
.mobile-container {
  padding: 16px 0;
  margin: -24px;
}

.mobile-search {
  margin-bottom: 16px;
  display: flex;
  gap: 8px;
  padding: 0 16px;
  max-width: 600px;
  margin: 0 auto;
}

.mobile-search-input {
  flex: 1;
}

.mobile-status-select {
  width: 120px;
}

.mobile-list-wrapper {
  max-width: 600px;
  margin: 0 auto;
  padding: 0 16px;
}

.mobile-activity-item {
  background: white;
  border-radius: 8px;
  margin-bottom: 12px;
  padding: 16px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.mobile-activity-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.mobile-activity-title {
  font-size: 16px;
  font-weight: 500;
  flex: 1;
  margin-right: 12px;
}

.mobile-activity-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-size: 13px;
  color: #666;
}

.mobile-activity-expire {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-size: 13px;
  color: #666;
}

.mobile-activity-actions {
  display: flex;
  justify-content: flex-end;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

/* 分页器样式 */
.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
  padding: 16px;
}

.mobile-pagination {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: white;
  margin-top: 16px;
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.06);
}

.page-info {
  font-size: 14px;
  color: #666;
  display: flex;
  align-items: center;
  gap: 4px;
}

.separator {
  color: #ddd;
}

.page-actions {
  display: flex;
  gap: 8px;
}

.page-button {
  min-width: 80px;
}

/* 拒绝模态框样式 */
.reject-modal {
  :deep(.ant-modal-content) {
    border-radius: 12px;
  }

  :deep(.ant-modal-header) {
    border-bottom: 1px solid #f0f0f0;
    padding: 16px 24px;

    .ant-modal-title {
      font-size: 16px;
      font-weight: 600;
    }
  }

  :deep(.ant-modal-body) {
    padding: 24px;
  }

  :deep(.ant-modal-footer) {
    border-top: 1px solid #f0f0f0;
    padding: 12px 24px;
  }
}

.reject-form {
  .form-label {
    font-size: 14px;
    color: #1a1a1a;
    margin-bottom: 8px;
  }

  .reason-select {
    margin-bottom: 16px;

    :deep(.ant-select) {
      width: 100%;
    }
  }

  .reason-input {
    :deep(.ant-input) {
      resize: none;
      border-radius: 8px;

      &:hover, &:focus {
        border-color: #1890ff;
      }
    }

    :deep(.ant-input-data-count) {
      font-size: 12px;
      color: #94a3b8;
      margin-top: 4px;
    }
  }
}

/* 审核确认弹框样式 */
:deep(.review-confirm-content) {
  text-align: center;
  padding: 16px 0;

  .confirm-title {
    font-size: 16px;
    font-weight: 500;
    color: #1a1a1a;
    margin-bottom: 8px;
  }

  .confirm-desc {
    font-size: 14px;
    color: #666;
  }
}

/* 响应式调整 */
@media screen and (max-width: 768px) {
  #activityManagePage {
    padding: 0;
    margin: -24px;
    background: #f8fafc;
  }

  .mobile-container {
    padding: 16px 0;
    padding-bottom: 80px;
  }

  .mobile-search {
    margin: 0 16px 16px;
    display: flex;
    gap: 8px;
  }

  .mobile-list-wrapper {
    margin: 0 16px;
  }

  .mobile-activity-item {
    background: white;
    border-radius: 12px;
    margin-bottom: 12px;
    padding: 16px;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  }

  .mobile-activity-header {
    margin-bottom: 12px;
  }

  .mobile-activity-title {
    font-size: 16px;
    font-weight: 500;
    margin-right: 12px;
    color: #1a1a1a;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  .mobile-activity-info {
    margin-bottom: 12px;
  }

  .mobile-user-info {
    display: flex;
    align-items: center;
    gap: 8px;

    .ant-avatar {
      flex-shrink: 0;
    }

    span {
      font-size: 14px;
      color: #666;
      max-width: 120px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  .mobile-activity-time {
    font-size: 13px;
    color: #999;
  }

  .mobile-activity-expire {
    font-size: 13px;
    color: #666;
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;

    .ant-tag {
      margin: 0;
    }
  }

  .mobile-activity-actions {
    padding-top: 12px;
    border-top: 1px solid #f0f0f0;

    .ant-space {
      width: 100%;
      justify-content: flex-end;
    }

    .ant-btn {
      padding: 4px 12px;
      height: 32px;
      border-radius: 6px;

      &.ant-btn-link {
        padding: 4px 8px;
      }
    }
  }

  .mobile-pagination {
    margin: 16px;
    padding: 16px;
    border-radius: 12px;
  }

  /* 拒绝模态框移动端优化 */
  .reject-modal {
    margin: 0 16px;

    :deep(.ant-modal-content) {
      padding: 16px;
    }

    :deep(.ant-modal-header) {
      padding: 0 0 16px;
    }

    :deep(.ant-modal-body) {
      padding: 16px 0;
    }

    :deep(.ant-modal-footer) {
      padding: 16px 0 0;
      margin-top: 0;
    }
  }

  .reject-form {
    .form-label {
      margin-bottom: 8px;
    }

    .reason-select {
      margin-bottom: 16px;
    }

    .reason-input {
      :deep(.ant-input) {
        border-radius: 8px;
      }
    }
  }
}

.create-button {
  background: #10b981;
  border-color: #10b981;
  border-radius: 8px;
  height: 40px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.3s ease;

  &:hover {
    background: #059669;
    border-color: #059669;
    transform: translateY(-1px);
  }

  &:active {
    transform: translateY(1px);
  }
}


.mobile-create-button {
  background: #10b981;
  border-color: #10b981;
  border-radius: 20px;
  margin: auto;
  height: 36px;
  padding: 0 16px;
  margin-bottom: 8px;
  margin-top: -8px;
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;

  &:hover {
    background: #059669;
    border-color: #059669;
  }
}
</style>
