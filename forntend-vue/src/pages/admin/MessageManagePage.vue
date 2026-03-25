<template>
  <div id="messageManagePage">
    <!-- PC端展示 -->
    <template v-if="device === DEVICE_TYPE_ENUM.PC">
      <div class="pc-container">
        <!-- 搜索表单 -->
        <div class="search-and-button-container">
          <a-form layout="inline" :model="searchParams" @finish="doSearch">
            <a-form-item label="IP">
              <a-input
                v-model:value="searchParams.ip"
                placeholder="输入IP地址"
                allow-clear
              />
            </a-form-item>
            <a-form-item label="内容">
              <a-input v-model:value="searchParams.content" placeholder="输入留言内容" allow-clear />
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
              @click="batchDeleteSelectedMessages"
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
                <template v-if="column.dataIndex === 'content'">
                  <a-tooltip :title="record.content">
                    <div class="ellipsis-text">
                      {{ record.content }}
                    </div>
                  </a-tooltip>
                </template>
                <template v-else-if="column.dataIndex === 'createTime'">
                  <div
                    :style="{
                      maxWidth: '250px',
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                      whiteSpace: 'nowrap',
                    }"
                  >
                    {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}
                  </div>
                </template>
                <template v-else-if="column.key === 'action'">
                  <div class="action-buttons">
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
              v-model="searchParams.content"
              placeholder="搜索留言内容"
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
              @click="batchDeleteSelectedMessages"
            >批量删除</van-button>
          </div>

          <!-- 留言列表 -->
          <div class="message-list">
            <van-checkbox-group v-model="state.selectedRowKeys">
              <van-cell-group inset v-for="message in dataList" :key="message.id">
                <van-card
                  :desc="message.content"
                  class="message-card"
                >
                  <template #tags>
                    <van-tag type="warning" round size="small" style="padding: 0 8px">
                      IP: {{ message.ip }}
                    </van-tag>
                  </template>

                  <template #num>
                    <span class="create-time">
                      {{ dayjs(message.createTime).format('YYYY-MM-DD HH:mm') }}
                    </span>
                  </template>

                  <template #footer>
                    <div class="card-footer">
                      <van-checkbox
                        :name="message.id"
                        class="card-checkbox"
                      />
                      <van-button
                        size="mini"
                        type="danger"
                        @click="showDeleteConfirm(message)"
                        class="delete-button"
                      >删除</van-button>
                    </div>
                  </template>
                </van-card>
              </van-cell-group>
            </van-checkbox-group>
          </div>

          <!-- 移动端分页 -->
          <div class="mobile-pagination">
            <div class="pagination-info">
              <span>共 {{ total }} 条</span>
              <div class="page-size-selector" @click="showPageSizeSheet = true">
                <span>{{ searchParams.pageSize }}条/页</span>
                <van-icon name="arrow-down" />
              </div>
            </div>
            <div class="pagination-wrapper">
              <van-pagination
                v-model="searchParams.current"
                :total-items="total"
                :items-per-page="searchParams.pageSize"
                @change="onMobilePageChange"
                prev-text="<"
                next-text=">"
                :show-page-size="3"
                class="custom-pagination"
                force-ellipses
              />
              <div class="jump-page">
                <span>跳至</span>
                <van-field
                  v-model="jumpPage"
                  type="number"
                  :placeholder="searchParams.current.toString()"
                  input-align="center"
                  @keyup.enter="handleJumpPage"
                />
                <span>页</span>
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
        <h3 class="confirm-title">确认删除该留言？</h3>
        <p class="confirm-desc">
          留言内容：{{ selectedMessage?.content || '无内容' }}<br>
          IP地址：{{ selectedMessage?.ip || '未知' }}
        </p>
        <div class="confirm-actions">
          <a-button class="cancel-button" @click="deleteConfirmVisible = false">取消</a-button>
          <a-button class="confirm-button" danger @click="confirmDelete">
            确认删除
          </a-button>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
import { onMounted, reactive, ref, computed, watch } from 'vue'
import {
  listAdminMessagesByPageUsingPost,
  batchAdminOperationMessagesUsingPost,
  deleteMessageUsingPost,
} from '@/api/messageController'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { getDeviceType } from '@/utils/device'
import { DEVICE_TYPE_ENUM } from '@/constants/device'
import {
  DeleteOutlined,
  SortAscendingOutlined,
  SortDescendingOutlined,
  SearchOutlined,
  ExclamationCircleFilled,
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
    width: 80,
  },
  {
    title: 'IP地址',
    dataIndex: 'ip',
    width: 120,
  },
  {
    title: '留言内容',
    dataIndex: 'content',
    width: 300,
    ellipsis: true,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 180,
  },
  {
    title: '操作',
    key: 'action',
    width: 120,
  },
]

// 定义数据
const dataList = ref<API.Message[]>([])
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

// 搜索条件，将sortOrder提取出来，方便后续操作
const searchParams = reactive<API.MessageAdminRequest>({
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
    const res = await listAdminMessagesByPageUsingPost({
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
    }
  } catch (error) {
    console.error('获取数据失败:', error)
    message.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

// 处理分页尺寸改变时的逻辑（每页显示条数改变）
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
const doTableChange = (pagination: any) => {
  searchParams.current = pagination.current
  searchParams.pageSize = pagination.pageSize
  if (pagination.sortField && pagination.sortOrder) {
    searchParams.sortField = pagination.sortField
    searchParams.sortOrder = pagination.sortOrder === 'ascend' ? 'ascend' : 'descend'
  }
  fetchData()
}

// 搜索数据
const doSearch = async () => {
  try {
    if (device.value === DEVICE_TYPE_ENUM.PC) {
      loading.value = true
    }

    const res = await listAdminMessagesByPageUsingPost({
      ...searchParams,
      sortField: 'createTime',
      sortOrder: sortOrder.value,
    })

    if (res.data?.code === 0) {
      dataList.value = res.data.data.records
      total.value = parseInt(res.data.data.total)
    } else {
      message.error('获取留言列表失败，请稍后重试')
    }
  } catch (error) {
    console.error('获取留言列表异常', error)
    message.error('获取留言列表失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 监听搜索参数变化
watch(
  () => searchParams,
  async () => {
    try {
      await doSearch()
    } catch (error) {
      console.error('监听搜索参数变化时出错：', error)
    }
  },
  { deep: true },
)

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
const selectedMessage = ref<API.Message | null>(null)

// 显示删除确认框
const showDeleteConfirm = (message: API.Message) => {
  selectedMessage.value = message
  deleteConfirmVisible.value = true
}

// 确认删除
const confirmDelete = async () => {
  if (!selectedMessage.value?.id) return

  try {
    const res = await deleteMessageUsingPost({ id: selectedMessage.value.id })
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

// 计算属性，判断是否有选中的行，用于控制批量删除按钮是否可用
const hasSelected = computed(() => state.selectedRowKeys.length > 0)

// 处理批量删除选中留言的方法
const batchDeleteSelectedMessages = async () => {
  if (state.selectedRowKeys.length === 0) {
    message.warning('请先选择要删除的留言')
    return
  }

  try {
    const res = await batchAdminOperationMessagesUsingPost({
      ids: state.selectedRowKeys,
      operation: 'delete'
    })
    if (res.data.code === 0) {
      message.success('批量删除留言成功')
      state.selectedRowKeys = []
      fetchData()
    } else {
      message.error('批量删除留言失败')
    }
  } catch (error) {
    message.error('批量删除留言出现异常，请稍后再试')
  }
}

const rowSelection = computed(() => {
  return {
    selectedRowKeys: state.selectedRowKeys,
    onChange: (selectedRowKeys: number[]) => {
      state.selectedRowKeys = selectedRowKeys
    }
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
</script>

<style scoped>
/* 复用UserManagePage.vue的样式，但修改一些特定的类名 */
#messageManagePage {
  background: var(--header-background);
  color: var(--text-primary)!important;
}

.mz-antd-pagination {
  text-align: right;
  margin-top: 20px;
}

.button-content {
  display: flex;
  align-items: center;
}

.search-and-button-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.loading-spin {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}

/* 移动端样式 */
.mobile-container {
  background: var(--header-background);
  color: var(--text-primary);
  min-height: 100vh;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  overflow: hidden;
  padding-bottom: 50px;
}

.mobile-content {
  height: 100%;
  overflow-y: auto;
  padding: 12px;
  -webkit-overflow-scrolling: touch;
  padding-bottom: 80px;
}

.action-bar {
  padding: 0 12px 12px;
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: flex-end;
}

.sort-button {
  height: 32px;
  padding: 0 12px;
}

.message-card {
  background: var(--header-background);
  color: var(--text-primary);
  border-radius: 8px;
  overflow: hidden;
  padding: 12px;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 4px;
}

.create-time {
  font-size: 12px;
  color: #999;
}

.card-checkbox {
  transform: scale(0.9);
  margin-right: auto;
}

.delete-button {
  padding: 0 8px;
  height: 24px;
  line-height: 22px;
  font-size: 12px;
}

/* PC端样式 */
.pc-container {
  background: var(--header-background);
  color: var(--text-primary);
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.06);
}

.action-button {
  height: 32px;
  border-radius: 6px;
  padding: 0 16px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition: all 0.3s;
}

.primary-button {
  background: #1890ff;
  border: none;
  box-shadow: 0 2px 0 rgba(24, 144, 255, 0.1);
}

.primary-button:hover {
  background: #40a9ff;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.2);
}

.primary-button:active {
  background: #096dd9;
  transform: translateY(0);
}

.delete-button {
  height: 28px;
  border-radius: 6px;
  padding: 0 12px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
}

.sort-button {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.sort-button .anticon {
  font-size: 14px;
}

.ellipsis-text {
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
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
}

.cancel-button:hover {
  color: #1a1a1a;
  border-color: #94a3b8;
  background: #f8fafc;
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
}

.confirm-button:hover {
  background: #ff5252;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(255, 107, 107, 0.2);
}

.confirm-button:active {
  transform: translateY(1px);
}

/* 移动端分页样式 */
.mobile-pagination {
  margin-top: 16px;
  padding: 12px;
  background: var(--header-background);
  color: var(--text-primary);
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.pagination-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  color: #666;
  font-size: 13px;
}

.page-size-selector {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  border: 1px solid #ebedf0;
  border-radius: 4px;
  background: var(--header-background);
  color: var(--text-primary);
  cursor: pointer;
}

.pagination-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.jump-page {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #666;
}

.jump-page :deep(.van-field) {
  width: 48px;
  padding: 0;
}

.jump-page :deep(.van-field__control) {
  height: 28px;
  padding: 0 4px;
  text-align: center;
  border: 1px solid #ebedf0;
  border-radius: 4px;
  font-size: 13px;
}

/* 响应式布局 */
@media screen and (max-width: 768px) {
  .pc-container {
    display: none;
  }

  .mobile-container {
    display: block;
  }
}

@media screen and (min-width: 769px) {
  .pc-container {
    display: block;
  }

  .mobile-container {
    display: none;
  }
}
</style>
