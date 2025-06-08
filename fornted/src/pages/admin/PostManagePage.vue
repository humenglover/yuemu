<template>
  <div id="postManagePage">
    <!-- PC端展示 -->
    <template v-if="device === DEVICE_TYPE_ENUM.PC">
      <div class="pc-container">
        <!-- 搜索表单和操作按钮 -->
        <div class="search-and-button-container">
          <a-form layout="inline" :model="searchParams" @finish="doSearch">
            <a-form-item label="关键词">
              <a-input
                v-model:value="searchParams.searchText"
                placeholder="从标题和内容搜索"
                allow-clear
                class="compact-input"
              />
            </a-form-item>
            <a-form-item label="分类">
              <a-input
                v-model:value="searchParams.category"
                placeholder="请输入分类"
                allow-clear
                class="compact-input"
              />
            </a-form-item>
            <a-form-item name="status" label="审核状态">
              <a-select
                v-model:value="searchParams.status"
                class="compact-select"
                placeholder="请选择审核状态"
                :options="POST_STATUS_OPTIONS"
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
                <div class="post-title-cell">
                  <span class="title-text">{{ record.title }}</span>
                  <a-tag v-if="record.category" class="category-tag">{{ record.category }}</a-tag>
                </div>
              </template>
              <template v-if="column.dataIndex === 'content'">
                <div class="post-content-cell">
                  <a-button type="link" @click="handleView(record)">
                    查看内容
                  </a-button>
                </div>
              </template>
              <template v-if="column.dataIndex === 'attachments'">
                <div class="attachments-preview">
                  <a-image
                    v-for="(attachment, index) in record.attachments"
                    :key="index"
                    :src="attachment.url"
                    :width="60"
                    class="attachment-thumb"
                  />
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
                  <span>点赞: {{ record.likeCount }}</span>
                  <span>浏览: {{ record.viewCount }}</span>
                  <span>评论: {{ record.commentCount }}</span>
                </div>
              </template>
              <template v-if="column.dataIndex === 'status'">
                <a-tag :color="getStatusColor(record.status)">
                  {{ POST_STATUS_MAP[record.status] }}
                </a-tag>
              </template>
              <template v-if="column.dataIndex === 'createTime'">
                {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}
              </template>
              <template v-if="column.key === 'action'">
                <a-space wrap>
                  <a-button
                    v-if="record.status !== POST_STATUS_ENUM.PASS"
                    type="primary"
                    class="table-button approve-button"
                    @click="handleReview(record, POST_STATUS_ENUM.PASS)"
                  >
                    <CheckOutlined />
                    通过
                  </a-button>
                  <a-button
                    v-if="record.status !== POST_STATUS_ENUM.REJECT"
                    type="primary"
                    danger
                    class="table-button reject-button"
                    @click="showRejectModal(record)"
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
              v-model:current="searchParams.current"
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
            v-model:value="searchParams.searchText"
            placeholder="搜索帖子"
            @search="doSearch"
            class="mobile-search-input"
          />
          <a-select
            v-model:value="searchParams.status"
            class="mobile-status-select"
            placeholder="审核状态"
            :options="POST_STATUS_OPTIONS"
            allow-clear
          />
        </div>

        <!-- 帖子列表 -->
        <div class="mobile-list-wrapper">
          <a-list
            :loading="loading"
            :data-source="dataList"
            class="mobile-post-list"
          >
            <template #renderItem="{ item: post }">
              <a-list-item class="mobile-post-item">
                <div class="mobile-post-content">
                  <div class="mobile-post-header">
                    <span class="mobile-post-title">{{ post.title }}</span>
                    <a-tag :color="getStatusColor(post.status)">
                      {{ POST_STATUS_MAP[post.status] }}
                    </a-tag>
                  </div>

                  <div class="mobile-post-info">
                    <div class="mobile-user-info">
                      <a-avatar :size="20" :src="post.user?.userAvatar" />
                      <span>{{ post.user?.userName }}</span>
                    </div>
                    <span class="mobile-post-time">
                      {{ dayjs(post.createTime).format('YYYY-MM-DD HH:mm') }}
                    </span>
                  </div>

                  <div class="mobile-post-actions">
                    <a-space>
                      <a-button
                        v-if="post.status !== POST_STATUS_ENUM.PASS"
                        type="primary"
                        size="small"
                        @click="handleReview(post, POST_STATUS_ENUM.PASS)"
                      >
                        通过
                      </a-button>
                      <a-button
                        v-if="post.status !== POST_STATUS_ENUM.REJECT"
                        type="primary"
                        danger
                        size="small"
                        @click="showRejectModal(post)"
                      >
                        拒绝
                      </a-button>
                      <a-button
                        type="link"
                        size="small"
                        @click="handleView(post)"
                      >
                        查看
                      </a-button>
                      <a-button
                        type="link"
                        danger
                        size="small"
                        @click="showDeleteConfirm(post)"
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
        <div class="mobile-pagination" v-if="device !== DEVICE_TYPE_ENUM.PC">
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
              v-model:value="searchParams.pageSize"
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
              v-for="option in POST_REJECT_REASON_OPTIONS"
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

    <!-- 内容查看模态框 -->
    <a-modal
      v-model:open="contentModalVisible"
      title="帖子内容"
      width="800px"
      :footer="null"
      class="content-modal"
      :bodyStyle="{ padding: '0' }"
    >
      <div class="content-modal-body">
        <div class="post-header">
          <h2 class="post-title">{{ selectedPost?.title }}</h2>
          <div class="post-meta">
            <span class="category-tag" v-if="selectedPost?.category">{{ selectedPost.category }}</span>
            <a-tag v-if="selectedPost?.status === 0" color="orange">待审核</a-tag>
            <a-tag v-else-if="selectedPost?.status === 2" color="red">已拒绝</a-tag>
          </div>
        </div>
        <markdown-content
          v-if="selectedPost"
          :content="selectedPost.content"
        />
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed,h } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal, Image } from 'ant-design-vue'
import dayjs from 'dayjs'
import {
  SearchOutlined,
  CheckOutlined,
  CloseOutlined,
  DeleteOutlined,
  EyeOutlined,
  ExclamationCircleOutlined,
  DownOutlined,
} from '@ant-design/icons-vue'
import { DEVICE_TYPE_ENUM } from '@/constants/device'
import { getDeviceType } from '@/utils/device'
import { POST_REJECT_REASON_OPTIONS, POST_REJECT_REASON_MAP } from '@/constants/review'
import { listPostByPageUsingPost, deletePostUsingPost, reviewPostUsingPost, getPostByIdUsingGet } from '@/api/postController'
import MarkdownContent from '@/components/MarkdownContent.vue'

// 状态枚举
const POST_STATUS_ENUM = {
  REVIEWING: 0,
  PASS: 1,
  REJECT: 2,
}

// 状态映射
const POST_STATUS_MAP = {
  [POST_STATUS_ENUM.REVIEWING]: '待审核',
  [POST_STATUS_ENUM.PASS]: '已通过',
  [POST_STATUS_ENUM.REJECT]: '已拒绝',
}

// 状态选项
const POST_STATUS_OPTIONS = [
  { label: '待审核', value: POST_STATUS_ENUM.REVIEWING },
  { label: '已通过', value: POST_STATUS_ENUM.PASS },
  { label: '已拒绝', value: POST_STATUS_ENUM.REJECT },
]

// 表格列定义
const columns = [
  { title: '标题', dataIndex: 'title', width: 200 },
  { title: '内容', dataIndex: 'content', width: 200 },
  { title: '附件', dataIndex: 'attachments', width: 150 },
  { title: '作者', dataIndex: 'userInfo', width: 120 },
  { title: '数据', dataIndex: 'stats', width: 150 },
  { title: '状态', dataIndex: 'status', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', width: 160 },
  { title: '操作', key: 'action', fixed: 'right', width: 200 },
]

const router = useRouter()
const device = ref(getDeviceType())
const loading = ref(false)
const dataList = ref<API.Post[]>([])
const searchParams = ref({
  searchText: '',
  category: '',
  status: undefined,
  isPublic :false,
  current: 1,
  pageSize: 10,
})
const total = ref(0)

// 获取状态颜色
const getStatusColor = (status: number) => {
  switch (status) {
    case POST_STATUS_ENUM.REVIEWING:
      return 'warning'
    case POST_STATUS_ENUM.PASS:
      return 'success'
    case POST_STATUS_ENUM.REJECT:
      return 'error'
    default:
      return 'default'
  }
}

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const res = await listPostByPageUsingPost({
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

// 查看帖子
const handleView = (record: API.Post) => {
  router.push(`/post/${record.id}`)
}

// 删除确认
const showDeleteConfirm = (record: API.Post) => {
  Modal.confirm({
    class: 'delete-confirm-modal',
    icon: null,
    content: h('div', { class: 'delete-confirm-content' }, [
      h('div', { class: 'warning-icon' }, [
        h(ExclamationCircleOutlined, { style: { color: '#ff4d4f' } })
      ]),
      h('div', { class: 'confirm-title' }, '确定要删除这个帖子吗？'),
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
        await deletePostUsingPost({ id: record.id })
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
const currentPost = ref<API.Post>()

// 显示拒绝模态框
const showRejectModal = (post: API.Post) => {
  currentPost.value = post
  rejectMessage.value = ''
  selectedReason.value = ''
  rejectModalVisible.value = true
}

// 处理拒绝原因选择
const handleReasonSelect = (value: string) => {
  selectedReason.value = value
  rejectMessage.value = POST_REJECT_REASON_MAP[value] || ''
}

const handleReview = async (post: API.Post, status: number) => {
  try {
    await reviewPostUsingPost({
      id: post.id,
      status,
    })
    message.success('操作成功')
    loadData()
  } catch (error: any) {
    message.error('操作失败：' + error.message)
  }
}

const handleRejectConfirm = async () => {
  if (!currentPost.value) return

  confirmLoading.value = true
  try {
    await reviewPostUsingPost({
      id: currentPost.value.id,
      status: POST_STATUS_ENUM.REJECT,
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

// 内容查看相关
const contentModalVisible = ref(false)
const selectedPost = ref<API.Post>()

const showContentModal = async (post: API.Post) => {
  try {
    const res = await getPostByIdUsingGet({ id: post.id })
    if (res.data?.data) {
      selectedPost.value = res.data.data
      contentModalVisible.value = true
    }
  } catch (error: any) {
    message.error('获取帖子详情失败：' + error.message)
  }
}

// 移动端分页配置
const mobilePagination = computed(() => ({
  ...searchParams.value,
  size: 'small',
  showSizeChanger: false,
  showQuickJumper: false,
}))

// 分页相关数据
const pageSizeOptions = ['10', '20', '30', '40']

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

onMounted(() => {
  loadData()
})
</script>

<style scoped>
#postManagePage {
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
.post-title-cell {
  display: flex;
  align-items: center;
  gap: 8px;

  .title-text {
    font-weight: 500;
  }

  .category-tag {
    font-size: 12px;
  }
}

.post-content-cell {
  color: #666;
  font-size: 13px;
}

.attachments-preview {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;

  .attachment-thumb {
    border-radius: 4px;
    overflow: hidden;
  }
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

  &.view-button {
    background: #3b82f6;
    border-color: #3b82f6;

    &:hover {
      background: #2563eb;
      border-color: #2563eb;
    }
  }
}

/* 表格分页样式 */
:deep(.ant-table-pagination) {
  margin: 24px 0 !important;
  padding: 0 24px;
  display: flex;
  justify-content: center;

  .ant-pagination-item {
    border-radius: 8px;
    transition: all 0.3s ease;

    &:hover, &-active {
      border-color: #1890ff;
      a {
        color: #1890ff;
      }
    }
  }

  .ant-pagination-prev,
  .ant-pagination-next {
    button {
      border-radius: 8px;
    }
  }
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  #postManagePage {
    padding: 0;
    margin: -16px -16px -80px -16px;
  }

  .pc-container {
    padding: 16px;
  }

  .search-and-button-container {
    padding: 16px;
    margin-bottom: 16px;
  }

  .table-section {
    padding: 16px;
    margin: 0 16px;
  }

  .compact-input,
  .compact-select {
    width: 100%;
  }
}

/* 移动端样式 */
.mobile-container {
  padding: 16px 0;
  padding-bottom: 80px;
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

.mobile-post-list {
  :deep(.ant-list-pagination) {
    margin: 24px 0;
    text-align: center;

    .ant-pagination-item {
      border-radius: 16px;
      min-width: 32px;
      height: 32px;
      line-height: 32px;

      &-active {
        background: #1890ff;
        border-color: #1890ff;
        a {
          color: white;
        }
      }
    }

    .ant-pagination-prev,
    .ant-pagination-next {
      button {
        border-radius: 16px;
        min-width: 32px;
        height: 32px;
        line-height: 32px;
      }
    }
  }
}

.mobile-post-item {
  background: white;
  border-radius: 0;
  margin-bottom: 12px;
  padding: 16px;
  width: 100%;

  :deep(.ant-list-item-main) {
    width: 100%;
  }

  &:first-child {
    border-top-left-radius: 8px;
    border-top-right-radius: 8px;
  }

  &:last-child {
    border-bottom-left-radius: 8px;
    border-bottom-right-radius: 8px;
    margin-bottom: 0;
  }
}

.mobile-post-content {
  width: 100%;
}

.mobile-post-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.mobile-post-title {
  font-size: 16px;
  font-weight: 500;
  flex: 1;
  margin-right: 12px;
}

.mobile-post-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-size: 13px;
  color: #666;
  width: 100%;
}

.mobile-user-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.mobile-post-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
  width: 100%;

  :deep(.ant-space) {
    flex-wrap: wrap;
    gap: 8px !important;
  }
}

/* 内容模态框样式 */
.content-modal {
  :deep(.ant-modal-content) {
    max-height: 80vh;
    overflow-y: auto;
    border-radius: 12px;
  }

  :deep(.ant-modal-header) {
    border-bottom: none;
    padding: 16px 24px;
  }

  :deep(.markdown-content) {
    font-size: 16px;
    line-height: 1.8;
    color: #374151;
    white-space: pre-wrap;
    word-break: break-word;
    padding: 16px;
    overflow: hidden;

    .image-container {
      margin: 16px auto;
      max-width: 800px;
      width: 100%;
      text-align: center;
      background: #f8fafc;
      border-radius: 8px;
      padding: 8px;
    }

    .content-image {
      max-width: 100%;
      height: auto;
      display: inline-block;
      border-radius: 4px;
      cursor: pointer;
      transition: transform 0.3s ease;
      vertical-align: middle;

      &:hover {
        transform: scale(1.02);
      }
    }

    :deep(p) {
      margin: 16px 0;
    }
  }
}

.content-modal-body {
  padding: 0 24px 24px;
}

.post-header {
  margin-bottom: 16px;

  .post-title {
    font-size: 20px;
    font-weight: 600;
    color: #1a1a1a;
    margin-bottom: 12px;
  }

  .post-meta {
    display: flex;
    gap: 8px;
    align-items: center;
  }
}

.category-tag {
  font-size: 12px;
  color: #666;
  background: #f5f5f5;
  padding: 2px 8px;
  border-radius: 4px;
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .content-modal {
    :deep(.ant-modal-content) {
      border-radius: 8px;
    }

    :deep(.ant-modal-header) {
      padding: 12px 16px;
    }
  }

  .content-modal-body {
    padding: 0 16px 16px;
  }

  .post-header {
    margin-bottom: 12px;

    .post-title {
      font-size: 18px;
      margin-bottom: 8px;
    }

  }

  .delete-confirm-content {
    padding: 24px 16px;
  }

  .warning-icon {
    font-size: 40px;
  }

  .confirm-title {
    font-size: 16px;
  }

  .confirm-desc {
    font-size: 13px;
  }

  .confirm-actions {
    gap: 8px;
  }

  .cancel-button,
  .confirm-button {
    min-width: 90px;
    height: 36px;
    font-size: 13px;
  }
}

/* 删除确认弹框样式 */
:deep(.delete-confirm-modal) {
  .ant-modal-content {
    padding: 0;
    border-radius: 16px;
    overflow: hidden;
  }

  .ant-modal-body {
    padding: 0;
  }
}

.delete-confirm-content {
  padding: 32px 24px;
  text-align: center;
}

.warning-icon {
  font-size: 48px;
  margin-bottom: 16px;

  .anticon {
    animation: pulse 2s infinite;
  }
}

.confirm-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 12px;
}

.confirm-desc {
  font-size: 14px;
  color: #64748b;
  margin-bottom: 24px;
  line-height: 1.6;
}

.confirm-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.cancel-button {
  min-width: 100px;
  height: 38px;
  border-radius: 19px;
  border: 1px solid #e2e8f0;
  color: #64748b;
  font-size: 14px;
  transition: all 0.3s ease;

  &:hover {
    color: #1a1a1a;
    border-color: #94a3b8;
    background: #f8fafc;
  }
}

.confirm-button {
  min-width: 100px;
  height: 38px;
  border-radius: 19px;
  background: #ff6b6b;
  border: none;
  color: white;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;

  &:hover {
    background: #ff5252;
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(255, 107, 107, 0.2);
  }

  &:active {
    transform: translateY(1px);
  }
}

@keyframes pulse {
  0% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.1);
    opacity: 0.8;
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
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

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .reject-modal {
    :deep(.ant-modal-content) {
      border-radius: 8px;
    }

    :deep(.ant-modal-header) {
      padding: 12px 16px;

      .ant-modal-title {
        font-size: 15px;
      }
    }

    :deep(.ant-modal-body) {
      padding: 16px;
    }

    :deep(.ant-modal-footer) {
      padding: 8px 16px;
    }
  }

  .reject-form {
    .form-label {
      font-size: 13px;
    }
  }
}

/* 分页器样式 */
.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
  padding: 16px;
}

/* 移动端分页样式 */
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
</style>
