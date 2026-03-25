<template>
  <div id="shareManagePage">
    <!-- PC端展示 -->
    <template v-if="device === DEVICE_TYPE_ENUM.PC">
      <div class="pc-container">
        <!-- 搜索表单 -->
        <div class="search-and-button-container">
          <a-form layout="inline" :model="searchParams" @finish="doSearch">
            <a-form-item label="用户ID">
              <a-input
                v-model:value="searchParams.userId"
                placeholder="输入用户ID"
                allow-clear
              />
            </a-form-item>
            <a-form-item label="内容ID">
              <a-input
                v-model:value="searchParams.targetId"
                placeholder="输入内容ID"
                allow-clear
              />
            </a-form-item>
            <a-form-item label="内容类型">
              <a-select v-model:value="searchParams.targetType" placeholder="选择内容类型" style="width: 120px">
                <a-select-option :value="1">图片</a-select-option>
                <a-select-option :value="2">帖子</a-select-option>
              </a-select>
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
              @click="batchDeleteSelectedShares"
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
                <template v-if="column.dataIndex === 'targetType'">
                  <a-tag :color="record.targetType === 1 ? 'blue' : 'green'">
                    {{ record.targetType === 1 ? '图片' : '帖子' }}
                  </a-tag>
                </template>
                <template v-if="column.dataIndex === 'isShared'">
                  <a-tag :color="record.isShared ? 'success' : 'error'">
                    {{ record.isShared ? '已分享' : '已取消' }}
                  </a-tag>
                </template>
                <template v-if="column.dataIndex === 'isRead'">
                  <a-tag :color="record.isRead ? 'success' : 'warning'">
                    {{ record.isRead ? '已读' : '未读' }}
                  </a-tag>
                </template>
                <template v-if="column.dataIndex === 'shareTime'">
                  {{ dayjs(record.shareTime).format('YYYY-MM-DD HH:mm:ss') }}
                </template>
                <template v-else-if="column.key === 'action'">
                  <div class="action-buttons">
                    <a-button
                      type="link"
                      class="table-button view-button"
                      @click="viewContent(record)"
                    >
                      <EyeOutlined />
                      查看内容
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
              v-model="searchParams.userId"
              placeholder="搜索用户ID"
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
              @click="batchDeleteSelectedShares"
            >批量删除</van-button>
          </div>

          <!-- 分享列表 -->
          <div class="share-list">
            <van-checkbox-group v-model="state.selectedRowKeys">
              <van-cell-group inset v-for="share in dataList" :key="share.id">
                <van-card
                  :title="'分享ID: ' + share.id"
                  :desc="'用户ID: ' + share.userId"
                  class="share-card"
                >
                  <template #tags>
                    <van-tag
                      :type="share.targetType === 1 ? 'primary' : 'success'"
                      round
                      size="small"
                      style="margin-right: 4px"
                    >
                      {{ share.targetType === 1 ? '图片' : '帖子' }}
                    </van-tag>
                    <van-tag
                      :type="share.isShared ? 'success' : 'danger'"
                      round
                      size="small"
                      style="margin-right: 4px"
                    >
                      {{ share.isShared ? '已分享' : '已取消' }}
                    </van-tag>
                    <van-tag
                      :type="share.isRead ? 'success' : 'warning'"
                      round
                      size="small"
                    >
                      {{ share.isRead ? '已读' : '未读' }}
                    </van-tag>
                  </template>

                  <template #price>
                    <span class="target-text">内容ID: {{ share.targetId }}</span>
                  </template>

                  <template #num>
                    <span class="share-time">
                      {{ dayjs(share.shareTime).format('YYYY-MM-DD HH:mm') }}
                    </span>
                  </template>

                  <template #footer>
                    <div class="card-footer">
                      <van-checkbox :name="share.id" class="card-checkbox" />
                      <van-button
                        size="mini"
                        type="primary"
                        plain
                        @click="viewContent(share)"
                        class="view-button"
                      >
                        查看内容
                      </van-button>
                      <van-button
                        size="mini"
                        type="danger"
                        @click="showDeleteConfirm(share)"
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
        <h3 class="confirm-title">确认删除该分享记录？</h3>
        <p class="confirm-desc">
          分享ID：{{ selectedShare?.id }}<br>
          用户ID：{{ selectedShare?.userId }}<br>
          内容ID：{{ selectedShare?.targetId }}
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
import { onMounted, reactive, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
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
import { message } from 'ant-design-vue'
import {
  listSharesByPageUsingPost,
  batchOperationSharesUsingPost,
} from '@/api/shareRecordController'

const router = useRouter()
const device = ref<string>('')

// 页面加载时获取设备类型
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
    title: '用户ID',
    dataIndex: 'userId',
    width: 100,
  },
  {
    title: '内容ID',
    dataIndex: 'targetId',
    width: 100,
  },
  {
    title: '内容类型',
    dataIndex: 'targetType',
    width: 100,
  },
  {
    title: '分享状态',
    dataIndex: 'isShared',
    width: 100,
  },
  {
    title: '阅读状态',
    dataIndex: 'isRead',
    width: 100,
  },
  {
    title: '分享时间',
    dataIndex: 'shareTime',
    width: 180,
  },
  {
    title: '操作',
    key: 'action',
    fixed: 'right',
    width: 200,
  },
]

// 定义数据
const dataList = ref<API.ShareRecord[]>([])
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
const searchParams = reactive<API.ShareAdminRequest>({
  current: 1,
  pageSize: 8,
  sortField: 'shareTime',
  sortOrder: 'descend',
})

const sortOrder = ref<'ascend' | 'descend'>('descend')

// 获取数据
const fetchData = async () => {
  loading.value = true
  try {
    const res = await listSharesByPageUsingPost(searchParams)
    if (res.data?.code === 0) {
      dataList.value = res.data.data.records
      total.value = res.data.data.total
    } else {
      message.error('获取分享列表失败')
    }
  } catch (error) {
    console.error('获取分享列表失败:', error)
    message.error('获取分享列表失败')
  } finally {
    loading.value = false
  }
}

// 处理分页尺寸改变
const onShowSizeChange = (current: number, pageSize: number) => {
  searchParams.current = 1
  searchParams.pageSize = pageSize
  fetchData()
}

// 处理页码改变
const onPageChange = (page: number, pageSize: number) => {
  searchParams.current = page
  searchParams.pageSize = pageSize
  fetchData()
}

// 表格变化处理
const doTableChange = (pagination: any, filters: any, sorter: any) => {
  searchParams.current = pagination.current
  searchParams.pageSize = pagination.pageSize
  if (sorter.field && sorter.order) {
    searchParams.sortField = sorter.field
    searchParams.sortOrder = sorter.order
  }
  fetchData()
}

// 搜索数据
const doSearch = () => {
  searchParams.current = 1
  fetchData()
}

// 切换排序
const toggleSortOrder = () => {
  sortOrder.value = sortOrder.value === 'ascend' ? 'descend' : 'ascend'
  searchParams.sortOrder = sortOrder.value
  fetchData()
}

// 删除确认相关
const deleteConfirmVisible = ref(false)
const selectedShare = ref<API.ShareRecord | null>(null)

// 显示删除确认框
const showDeleteConfirm = (share: API.ShareRecord) => {
  selectedShare.value = share
  deleteConfirmVisible.value = true
}

// 确认删除
const confirmDelete = async () => {
  if (!selectedShare.value?.id) return

  try {
    const res = await batchOperationSharesUsingPost({
      ids: [selectedShare.value.id],
      operation: 'delete'
    })
    if (res.data?.code === 0) {
      message.success('删除成功')
      deleteConfirmVisible.value = false
      fetchData()
    } else {
      message.error('删除失败：' + res.data?.message)
    }
  } catch (error) {
    message.error('删除失败，请稍后重试')
  }
}

// 查看内容
const viewContent = (record: API.ShareRecord) => {
  const path = record.targetType === 1 ? `/picture/${record.targetId}` : `/post/${record.targetId}`
  router.push(path)
}

// 批量选择相关
const state = reactive({
  selectedRowKeys: [] as number[],
})

const onSelectChange = (selectedRowKeys: number[]) => {
  state.selectedRowKeys = selectedRowKeys
}

const hasSelected = computed(() => state.selectedRowKeys.length > 0)

// 批量删除
const batchDeleteSelectedShares = async () => {
  if (state.selectedRowKeys.length === 0) {
    message.warning('请先选择要删除的记录')
    return
  }

  try {
    const res = await batchOperationSharesUsingPost({
      ids: state.selectedRowKeys,
      operation: 'delete'
    })
    if (res.data?.code === 0) {
      message.success('批量删除成功')
      state.selectedRowKeys = []
      fetchData()
    } else {
      message.error('批量删除失败')
    }
  } catch (error) {
    message.error('批量删除失败，请稍后重试')
  }
}

const rowSelection = computed(() => {
  return {
    selectedRowKeys: state.selectedRowKeys,
    onChange: onSelectChange
  }
})

// 移动端分页处理
const onMobilePageChange = (page: number) => {
  searchParams.current = page
  fetchData()
}

// 处理每页条数改变
const handlePageSizeChange = (action: { value: number }) => {
  searchParams.current = 1
  searchParams.pageSize = action.value
  fetchData()
}

// 处理页码跳转
const handleJumpPage = () => {
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
  fetchData()
  jumpPage.value = ''
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
/* 复用 UserManagePage.vue 的样式并做必要调整 */
#shareManagePage {
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
  margin-bottom: 20px;
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

.share-card {
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

.target-text {
  font-size: 12px;
  color: #666;
  display: inline-block;
  margin-top: 4px;
}

.share-time {
  font-size: 12px;
  color: #999;
}

/* PC端样式 */
.pc-container {
  background: var(--header-background);
  color: var(--text-primary);
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.06);
}

/* 删除确认弹框样式 */
.delete-confirm-modal {
  :deep(.ant-modal-content) {
    padding: 0;
    border-radius: 16px;
    overflow: hidden;
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

/* 响应式布局 */
@media screen and (max-width: 768px) {
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
}

/* 移动端分页样式 */
.mobile-pagination {
  margin-top: 16px;
  padding: 12px;
  background: var(--header-background);
  color: var(--text-primary);
  border-radius: 8px;
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

/* 按钮样式 */
.action-button {
  height: 32px;
  border-radius: 6px;
  padding: 0 16px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.view-button {
  color: #1890ff;
  border-color: #1890ff;
}

.delete-button {
  color: #ff4d4f;
  border-color: #ff4d4f;
}
</style> 