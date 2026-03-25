<template>
  <div id="sessionManagePage">
    <!-- PC端展示 -->
    <template v-if="device === DEVICE_TYPE_ENUM.PC">
      <div class="pc-container">
        <!-- 搜索表单 -->
        <div class="search-and-button-container">
          <a-form layout="inline" :model="searchParams" @finish="doSearch">
            <a-form-item label="会话名称">
              <a-input
                v-model:value="searchParams.sessionName"
                placeholder="输入会话名称"
                allow-clear
              />
            </a-form-item>
            <a-form-item label="用户ID">
              <a-input v-model:value="searchParams.userId" placeholder="输入用户ID" allow-clear />
            </a-form-item>
            <a-form-item>
              <a-button type="primary" html-type="submit" class="action-button">
                <SearchOutlined />
                搜索
              </a-button>
            </a-form-item>
          </a-form>
          <div style="display: flex; align-items: center">
            <a-button type="dashed" class="sort-button" @click="toggleSortOrder">
              <span class="button-content">
                <SortAscendingOutlined v-if="sortOrder === 'ascend'" />
                <SortDescendingOutlined v-else />
                <span class="button-text">{{ sortOrder === 'ascend' ? '升序' : '降序' }}</span>
              </span>
            </a-button>
            <a-button
              style="margin-left: 20px"
              type="primary"
              danger
              :disabled="!hasSelected"
              @click="batchDeleteSelectedSessions"
              class="action-button"
            >
              <DeleteOutlined />批量删除
            </a-button>
          </div>
        </div>

        <div class="table-section">
          <a-spin tip="正在加载中..." :spinning="loading">
            <a-table
              :row-selection="rowSelection"
              rowKey="id"
              :columns="columns"
              :data-source="dataList"
              :pagination="false"
              @change="doTableChange"
              class="custom-table"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'sessionName'">
                  <a-tooltip :title="record.sessionName">
                    <div class="ellipsis-text">
                      {{ record.sessionName || '未命名会话' }}
                    </div>
                  </a-tooltip>
                </template>
                <template v-else-if="column.dataIndex === 'userId'">
                  <div
                    :style="{
                      maxWidth: '100px',
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                      whiteSpace: 'nowrap',
                    }"
                  >
                    {{ record.userId }}
                  </div>
                </template>
                <template v-else-if="column.dataIndex === 'createTime'">
                  <div
                    :style="{
                      maxWidth: '200px',
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                      whiteSpace: 'nowrap',
                    }"
                  >
                    {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}
                  </div>
                </template>
                <template v-else-if="column.dataIndex === 'updateTime'">
                  <div
                    :style="{
                      maxWidth: '200px',
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                      whiteSpace: 'nowrap',
                    }"
                  >
                    {{ dayjs(record.updateTime).format('YYYY-MM-DD HH:mm:ss') }}
                  </div>
                </template>
                <template v-else-if="column.key === 'action'">
                  <div class="action-buttons">
                    <!-- 查看详情按钮 -->
                    <a-button
                      type="link"
                      class="table-button view-button"
                      @click="viewSessionMessages(record.id)"
                    >
                      <EyeOutlined />
                      查看消息
                    </a-button>
                    <a-button
                      danger
                      class="delete-button"
                      @click="showDeleteConfirm(record)"
                    >
                      <DeleteOutlined />
                      删除
                    </a-button>
                  </div>
                </template>
              </template>
            </a-table>
          </a-spin>
        </div>

        <!-- 分页组件 -->
        <div class="mz-antd-pagination">
          <a-pagination
            v-model:current="searchParams.current"
            :page-size-options="pcPageSizeOptions"
            :total="total"
            :show-total="(total) => `共 ${total} 条`"
            show-size-changer
            :page-size="searchParams.pageSize"
            @change="onPageChange"
            @showSizeChange="onShowSizeChange"
          >
          </a-pagination>
        </div>
      </div>
    </template>

    <!-- 移动端展示 -->
    <template v-else>
      <div class="mobile-container">
        <div class="mobile-content">
          <!-- 搜索区域 -->
          <div class="search-section">
            <van-search
              v-model="searchParams.sessionName"
              placeholder="搜索会话名称"
              @search="doSearch"
            />
          </div>

          <!-- 操作按钮区 -->
          <div class="action-bar">
            <a-button type="dashed" class="sort-button" @click="toggleSortOrder">
              <span class="button-content">
                <span class="button-text">{{ sortOrder === 'ascend' ? '升序' : '降序' }}</span>
              </span>
            </a-button>
            <van-button
              type="danger"
              size="small"
              icon="delete-o"
              :disabled="!hasSelected"
              @click="batchDeleteSelectedSessions"
            >批量删除</van-button>
          </div>

          <!-- 会话列表 -->
          <div class="session-list">
            <van-checkbox-group v-model="state.selectedRowKeys">
              <div v-for="session in dataList" :key="session.id" class="session-card-wrapper">
                <div class="session-card-top">
                  <van-checkbox :name="session.id" class="card-checkbox" />
                  <div class="session-info">
                    <div class="session-header">
                      <span class="session-title">{{ session.sessionName || '未命名会话' }}</span>
                      <span class="update-time">{{ dayjs(session.updateTime).format('MM-DD HH:mm') }}</span>
                    </div>
                    <div class="session-tags">
                      <van-tag type="primary" plain round class="custom-tag">用户ID: {{ session.userId }}</van-tag>
                      <van-tag type="warning" plain round class="custom-tag">会话ID: {{ session.id }}</van-tag>
                    </div>
                    <div class="session-meta">
                      创建于: {{ dayjs(session.createTime).format('YYYY-MM-DD HH:mm') }}
                    </div>
                  </div>
                </div>
                <div class="session-card-footer">
                  <a-button
                    type="link"
                    size="small"
                    @click="viewSessionMessages(session.id)"
                    class="footer-btn view-btn"
                  >
                    <EyeOutlined /> 查看消息
                  </a-button>
                  <a-button
                    danger
                    type="link"
                    size="small"
                    @click="showDeleteConfirm(session)"
                    class="footer-btn delete-btn"
                  >
                    <DeleteOutlined /> 删除
                  </a-button>
                </div>
              </div>
            </van-checkbox-group>
          </div>

          <!-- 移动端分页 -->
          <div class="mobile-pagination">
            <div class="pagination-info">
              <span class="total-text">共 {{ total }} 条</span>
              <div class="page-size-selector" @click="showPageSizeSheet = true">
                <span>{{ searchParams.pageSize }} 条/页</span>
                <van-icon name="arrow-down" class="arrow-icon" />
              </div>
            </div>
            <div class="pagination-controls">
              <van-pagination
                v-model="searchParams.current"
                :total-items="total"
                :items-per-page="searchParams.pageSize"
                @change="onMobilePageChange"
                class="modern-pagination"
                :show-page-size="3"
                force-ellipses
              >
                <template #prev-text><van-icon name="arrow-left" /></template>
                <template #next-text><van-icon name="arrow" /></template>
              </van-pagination>
              <div class="page-jump">
                <van-field
                  v-model="jumpPage"
                  type="digit"
                  placeholder="页码"
                  @keyup.enter="handleJumpPage"
                  class="jump-input"
                />
                <a-button size="small" @click="handleJumpPage" class="jump-btn">跳转</a-button>
              </div>
            </div>
          </div>

          <!-- 每页条数选择器抽屉 -->
          <van-action-sheet
            v-model:show="showPageSizeSheet"
            :actions="mobilePageSizeOptions"
            cancel-text="取消"
            close-on-click-action
            @select="handlePageSizeChange"
          />
        </div>
      </div>
    </template>

    <!-- 删除确认弹框 -->
    <a-modal
      v-model:open="deleteConfirmVisible"
      :title="null"
      :footer="null"
      :width="400"
      class="delete-confirm-modal"
    >
      <div class="delete-confirm-content">
        <div class="warning-icon">
          <ExclamationCircleFilled style="color: #ff6b6b;" />
        </div>
        <h3 class="confirm-title">确认删除该会话？</h3>
        <p class="confirm-desc">
          会话名称：{{ selectedSession?.sessionName || '未命名会话' }}<br>
          会话ID：{{ selectedSession?.id }}
        </p>
        <div class="confirm-actions">
          <a-button class="cancel-button" @click="deleteConfirmVisible = false">取消</a-button>
          <a-button class="confirm-button" danger @click="confirmDelete">
            确认删除
          </a-button>
        </div>
      </div>
    </a-modal>

    <!-- 会话消息详情弹框 -->
    <a-modal
      v-model:open="messageModalVisible"
      title="会话消息详情"
      :width="800"
      :footer="null"
      @cancel="closeMessageModal"
    >
      <div class="message-list-container">
        <a-spin :spinning="loadingMessages">
          <div v-for="message in messageList" :key="message.id" class="message-item">
            <div :class="['message-content', message.messageType === 1 ? 'user-message' : 'ai-message']">
              <strong>{{ message.messageType === 1 ? '用户' : 'AI' }}:</strong>
              <div class="message-text">{{ message.content }}</div>
              <div class="message-time">{{ dayjs(message.createTime).format('YYYY-MM-DD HH:mm:ss') }}</div>
            </div>
          </div>
        </a-spin>
      </div>
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
import { onMounted, reactive, ref, computed } from 'vue'
import {
  listSessionsByPageUsingPost,
  deleteSessionByIdUsingPost,
  deleteBatchSessionsUsingPost,
} from '@/api/sessionController.ts'
import {
  listMessagesBySessionIdUsingGet
} from '@/api/messageQaController.ts'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { getDeviceType } from '@/utils/device.ts'
import { DEVICE_TYPE_ENUM } from '@/constants/device.ts'
import {
  DeleteOutlined,
  SortAscendingOutlined,
  SortDescendingOutlined,
  SearchOutlined,
  ExclamationCircleFilled,
  EyeOutlined,
} from '@ant-design/icons-vue'

// 定义用于存储设备类型的响应式变量
const device = ref<string>('')

// 页面加载时获取设备类型并获取数据
onMounted(async () => {
  device.value = await getDeviceType()
})

// 表格列定义
const columns = [
  {
    title: 'ID',
    dataIndex: 'id',
    width: 100,
  },
  {
    title: '会话名称',
    dataIndex: 'sessionName',
    width: 150,
  },
  {
    title: '用户ID',
    dataIndex: 'userId',
    width: 100,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 180,
  },
  {
    title: '更新时间',
    dataIndex: 'updateTime',
    width: 180,
  },
  {
    title: '操作',
    key: 'action',
  },
]

// 定义数据
const dataList = ref<API.RagSessionVO[]>([])
const total = ref(0)
const loading = ref(false)
const showPageSizeSheet = ref(false)
const jumpPage = ref('')

// PC端分页选项
const pcPageSizeOptions = ['5', '8', '10', '20', '50']

// 移动端分页选项
const mobilePageSizeOptions = [
  { name: '10条/页', value: 10 },
  { name: '20条/页', value: 20 },
  { name: '30条/页', value: 30 },
  { name: '50条/页', value: 50 },
]

// 搜索条件
const searchParams = reactive<API.SessionQueryRequest>({
  current: 1,
  pageSize: 8,
  sortField: 'createTime',
})

const sortOrder = ref<'ascend' | 'descend'>('ascend')

// 获取数据
const fetchData = async () => {
  if (device.value === DEVICE_TYPE_ENUM.PC) {
    loading.value = true
  }

  try {
    const res = await listSessionsByPageUsingPost({
      ...searchParams,
      sortOrder: sortOrder.value,
    })
    if (res.data?.code === 0) {
      // 如果是移动端加载更多，追加数据
      if (device.value !== DEVICE_TYPE_ENUM.PC && searchParams.current > 1) {
        dataList.value = [...dataList.value, ...res.data.data.records]
      } else {
        // PC端或移动端首次加载/刷新，直接替换数据
        dataList.value = res.data.data.records
      }
      total.value = parseInt(res.data.data.total)
      console.log('获取到的数据:', dataList.value) // 添加日志查看数据
    }
  } catch (error) {
    console.error('获取数据失败:', error)
    message.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

// 处理分页尺寸改变时的逻辑（每页条数改变）
const onShowSizeChange = (current: number, pageSize: number) => {
  searchParams.current = 1 // 切换每页条数时，默认回到第一页，可根据需求调整
  searchParams.pageSize = pageSize
  fetchData()
}

// 处理页码改变时的逻辑
const onPageChange = (page: number, pageSize: number) => {
  searchParams.current = page
  searchParams.pageSize = pageSize
  fetchData()
}

// 表格变化之后，重新获取数据（处理排序等变化情况）
const doTableChange = (page: any) => {
  searchParams.current = page.current
  searchParams.pageSize = page.pageSize
  if (page.sortField && page.sortOrder) {
    searchParams.sortField = page.sortField
    searchParams.sortOrder = page.sortOrder === 'ascend' ? 'ascend' : 'descend'
  }
  fetchData()
}

// 搜索数据
const doSearch = async () => {
  try {
    if (device.value === DEVICE_TYPE_ENUM.PC) {
      loading.value = true
    }

    const res = await listSessionsByPageUsingPost({
      ...searchParams,
      sortField: 'createTime',
      sortOrder: sortOrder.value,
    })

    if (res.data?.code === 0) {
      dataList.value = res.data.data.records
      total.value = parseInt(res.data.data.total)
      console.log('搜索结果:', dataList.value) // 添加日志查看数据
    } else {
      message.error('获取会话列表失败，请稍后重试')
    }
  } catch (error) {
    console.error('获取会话列表异常', error)
    message.error('获取会话列表失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 组件挂载时获取数据
onMounted(async () => {
  try {
    await doSearch()
  } catch (error) {
    console.error('初始化加载数据失败：', error)
  }
})

// 删除确认相关的状态
const deleteConfirmVisible = ref(false)
const selectedSession = ref<API.RagSessionVO | null>(null)

// 显示删除确认框
const showDeleteConfirm = (session: API.RagSessionVO) => {
  selectedSession.value = session
  deleteConfirmVisible.value = true
}

// 确认删除
const confirmDelete = async () => {
  if (!selectedSession.value?.id) return

  try {
    const res = await deleteSessionByIdUsingPost({ id: selectedSession.value.id })
    if (res.data.code === 0) {
      message.success('删除成功')
      deleteConfirmVisible.value = false
      // 刷新数据
      fetchData()
    } else {
      message.error('删除失败：' + res.data.message)
    }
  } catch (error) {
    message.error('删除失败，请稍后重试')
  }
}

// 新增：处理排序按钮点击事件，切换排序顺序并刷新数据
const toggleSortOrder = () => {
  sortOrder.value = sortOrder.value === 'ascend' ? 'descend' : 'ascend'
  searchParams.sortOrder = sortOrder.value
  fetchData()
}

// 用于存储表格行选择状态的响应式对象
const state = reactive({
  selectedRowKeys: [] as number[],
  loading: false,
})

const onSelectChange = (selectedRowKeys: number[]) => {
  state.selectedRowKeys = selectedRowKeys
}

// 计算属性，判断是否有选中的行，用于控制批量删除按钮是否可用
const hasSelected = computed(() => state.selectedRowKeys.length > 0)

// 新增：处理批量删除选中会话的方法
const batchDeleteSelectedSessions = async () => {
  if (state.selectedRowKeys.length === 0) {
    message.warning('请先选择要删除的会话')
    return
  }
  const selectedSessionIds = state.selectedRowKeys
  try {
    const deleteRequests = selectedSessionIds.map(id => ({ id }))
    const res = await deleteBatchSessionsUsingPost(deleteRequests)
    if (res.data.code === 0) {
      message.success('批量删除会话成功')
      state.selectedRowKeys = []
      fetchData()
    } else {
      message.error('批量删除会话失败')
    }
  } catch (error) {
    message.error('批量删除会话出现异常，请稍后再试')
  }
}

const rowSelection = computed(() => {
  return {
    selectedRowKeys: state.selectedRowKeys,
    onChange: onSelectChange,
  }
})

// 移动端分页处理方法
const onMobilePageChange = async (page: number) => {
  searchParams.current = page
  await doSearch()
}

// 处理每页条数改变
const handlePageSizeChange = async (action: { value: number }) => {
  searchParams.current = 1
  searchParams.pageSize = action.value
  await doSearch()
}

// 处理页码跳转
const handleJumpPage = async () => {
  const page = parseInt(jumpPage.value)
  if (isNaN(page)) {
    return
  }

  const maxPage = Math.ceil(total.value / searchParams.pageSize)
  if (page < 1 || page > maxPage) {
    message.warning(`请输入1-${maxPage}之间的页码`)
    return
  }

  searchParams.current = page
  await doSearch()
  jumpPage.value = ''
}

// 会话消息详情相关
const messageModalVisible = ref(false)
const messageList = ref<API.RagMessageVO[]>([])
const loadingMessages = ref(false)

// 查看会话消息
const viewSessionMessages = async (sessionId: number) => {
  loadingMessages.value = true
  try {
    const res = await listMessagesBySessionIdUsingGet({ sessionId })
    if (res.data?.code === 0) {
      messageList.value = res.data.data
      messageModalVisible.value = true
    } else {
      message.error('获取会话消息失败')
    }
  } catch (error) {
    console.error('获取会话消息失败:', error)
    message.error('获取会话消息失败')
  } finally {
    loadingMessages.value = false
  }
}

// 关闭消息弹框
const closeMessageModal = () => {
  messageModalVisible.value = false
  messageList.value = []
}
</script>

<style scoped>
#sessionManagePage{
  background: var(--header-background);
  color: var(--text-primary)!important;
}

@media screen and (min-width: 768px) {
  #sessionManagePage {
    background: var(--header-background);
    color: var(--text-primary)!important;
    width: 100vw;
    margin: 0 auto;
    padding: 24px;
  }
}
.mz-antd-pagination {
  text-align: right;
  margin-top: 20px;
}
/* 按钮内容部分样式（图标和文字） */
.button-content {
  display: flex;
  align-items: center;
}
.search-and-button-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
/* 模态框内表单样式调整 */
.a-modal form {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.loading-spin {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}

.mobile-container {
  background: var(--header-background, #f5f7fa);
  color: var(--text-primary);
  min-height: 100vh;
  padding-bottom: constant(safe-area-inset-bottom);
  padding-bottom: env(safe-area-inset-bottom);
}

.mobile-content {
  padding: 16px 12px;
}

.search-section {
  margin-bottom: 16px;
  :deep(.van-search) {
    padding: 0;
    background: transparent;
  }
  :deep(.van-search__content) {
    background: rgba(255, 255, 255, 0.05);
    border-radius: 10px;
    border: 1px solid rgba(255, 255, 255, 0.1);
  }
}

.action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  gap: 12px;
}

.action-bar-right {
  display: flex;
  gap: 8px;
}

.sort-button-mobile {
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: var(--text-primary);
}

.batch-delete-btn {
  border-radius: 8px;
  height: 32px;
}

.session-card-wrapper {
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  margin-bottom: 16px;
  overflow: hidden;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.session-card-top {
  display: flex;
  padding: 16px;
  gap: 12px;
}

.card-checkbox {
  align-self: flex-start;
  margin-top: 4px;
}

.session-info {
  flex: 1;
  min-width: 0;
}

.session-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 8px;
}

.session-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.update-time {
  font-size: 12px;
  color: var(--text-secondary, #999);
  flex-shrink: 0;
  margin-left: 8px;
}

.session-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 8px;
}

.custom-tag {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
}

.session-meta {
  font-size: 12px;
  color: var(--text-secondary, #8c8c8c);
}

.session-card-footer {
  display: flex;
  justify-content: flex-end;
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.02);
  border-top: 1px solid rgba(255, 255, 255, 0.05);
  gap: 8px;
}

.footer-btn {
  height: auto;
  padding: 4px 8px;
  font-size: 13px;
}

/* 移动端分页优化 */
.mobile-pagination {
  margin-top: 24px;
  padding-bottom: 32px;
}

.pagination-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  color: var(--text-secondary);
  font-size: 14px;
}

.page-size-selector {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 6px;
}

.pagination-controls {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.modern-pagination {
  :deep(.van-pagination__item) {
    background: rgba(255, 255, 255, 0.05);
    border: none;
    color: var(--text-primary);
    border-radius: 8px;
    margin: 0 2px;
  }
  :deep(.van-pagination__item--active) {
    background: var(--primary-color, #1890ff);
    color: #fff;
  }
}

.page-jump {
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: center;
}

.jump-input {
  width: 80px;
  padding: 4px 12px;
  background: rgba(255, 255, 255, 0.05) !important;
  border-radius: 6px;
  :deep(.van-field__control) {
    color: var(--text-primary);
    text-align: center;
  }
}

.jump-btn {
  border-radius: 6px;
}

/* 消息详情样式 */
.message-list-container {
  max-height: 500px;
  overflow-y: auto;
}

.message-item {
  margin-bottom: 15px;
}

.message-content {
  padding: 10px;
  border-radius: 8px;
  margin-bottom: 5px;
}

.user-message {
  background-color: #e3f2fd;
  border-left: 4px solid #2196f3;
}

.ai-message {
  background-color: #f5f5f5;
  border-left: 4px solid #4caf50;
}

.message-text {
  margin: 8px 0;
  word-wrap: break-word;
  white-space: pre-wrap;
}

.message-time {
  font-size: 12px;
  color: #999;
  text-align: right;
}

/* 删除确认弹框样式 */
.delete-confirm-content {
  text-align: center;
  padding: 20px;
}

.warning-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.confirm-title {
  font-size: 18px;
  margin-bottom: 12px;
}

.confirm-desc {
  margin-bottom: 20px;
  color: #666;
}

.confirm-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
}

.cancel-button {
  margin-right: 8px;
}

.confirm-button {
  margin-left: 8px;
}

/* 表格样式 */
.action-buttons {
  display: flex;
  gap: 8px;
}

.table-button {
  padding: 0 8px;
  height: 28px;
  font-size: 12px;
}

.view-button {
  color: #1890ff;
}

.delete-button {
  color: #ff4d4f;
}

.ellipsis-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
