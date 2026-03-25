<template>
  <div id="likeManagePage">
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
            <a-form-item label="目标ID">
              <a-input v-model:value="searchParams.targetId" placeholder="输入目标ID" allow-clear />
            </a-form-item>
            <a-form-item label="目标类型">
              <a-select v-model:value="searchParams.targetType" placeholder="选择目标类型" allow-clear>
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
              @click="batchDeleteSelectedLikes"
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
                <template v-if="column.dataIndex === 'isLiked'">
                  <a-tag :color="record.isLiked ? 'red' : 'default'">
                    {{ record.isLiked ? '已点赞' : '已取消' }}
                  </a-tag>
                </template>
                <template v-if="column.dataIndex === 'firstLikeTime'">
                  {{ dayjs(record.firstLikeTime).format('YYYY-MM-DD HH:mm:ss') }}
                </template>
                <template v-else-if="column.key === 'action'">
                  <div class="action-buttons">
                    <a-button
                      type="primary"
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
              @click="batchDeleteSelectedLikes"
            >批量删除</van-button>
          </div>

          <!-- 点赞列表 -->
          <div class="like-list">
            <van-checkbox-group v-model="state.selectedRowKeys">
              <van-cell-group inset v-for="like in dataList" :key="like.id">
                <van-card
                  :title="'目标ID: ' + like.targetId"
                  :desc="'用户ID: ' + like.userId"
                  class="like-card"
                >
                  <template #tags>
                    <van-tag
                      :type="like.targetType === 1 ? 'primary' : 'success'"
                      round
                      style="margin-right: 4px"
                    >
                      {{ like.targetType === 1 ? '图片' : '帖子' }}
                    </van-tag>
                    <van-tag
                      :type="like.isLiked ? 'danger' : 'default'"
                      round
                    >
                      {{ like.isLiked ? '已点赞' : '已取消' }}
                    </van-tag>
                  </template>

                  <template #footer>
                    <div class="card-footer">
                      <van-checkbox :name="like.id" class="card-checkbox" />
                      <van-button
                        size="mini"
                        type="danger"
                        @click="showDeleteConfirm(like)"
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
        <h3 class="confirm-title">确认删除该点赞记录？</h3>
        <p class="confirm-desc">
          用户ID：{{ selectedLike?.userId }}<br>
          目标ID：{{ selectedLike?.targetId }}<br>
          目标类型：{{ selectedLike?.targetType === 1 ? '图片' : '帖子' }}
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
  listLikesByPageUsingPost,
  batchOperationLikesUsingPost,
  updateLikeUsingPost,
} from '@/api/likeRecordController'
import { Form, message } from 'ant-design-vue'
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
    title: '用户ID',
    dataIndex: 'userId',
    width: 100,
  },
  {
    title: '目标ID',
    dataIndex: 'targetId',
    width: 100,
  },
  {
    title: '目标类型',
    dataIndex: 'targetType',
    width: 100,
  },
  {
    title: '点赞状态',
    dataIndex: 'isLiked',
    width: 100,
  },
  {
    title: '创建时间',
    dataIndex: 'firstLikeTime',
    width: 180,
  },
  {
    title: '操作',
    key: 'action',
    width: 120,
  },
]

// 定义数据
const dataList = ref<API.LikeRecord[]>([])
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
const searchParams = reactive<API.LikeAdminRequest>({
  current: 1,
  pageSize: 8,
  sortField: 'firstLikeTime',
  sortOrder: 'descend'
})
const sortOrder = ref<'ascend' | 'descend'>('ascend')

// 获取数据
const fetchData = async () => {
  if (device.value === DEVICE_TYPE_ENUM.PC) {
    loading.value = true
  }

  try {
    const res = await listLikesByPageUsingPost({
      ...searchParams,
      sortOrder: sortOrder.value,
    })
    if (res.data?.code === 0) {
      if (device.value !== DEVICE_TYPE_ENUM.PC && searchParams.current > 1) {
        dataList.value = [...dataList.value, ...res.data.data.records]
      } else {
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

    const res = await listLikesByPageUsingPost({
      ...searchParams,
      sortField: 'firstLikeTime',
      sortOrder: sortOrder.value,
    })

    if (res.data?.code === 0) {
      dataList.value = res.data.data.records
      total.value = parseInt(res.data.data.total)
    } else {
      message.error('获取点赞列表失败，请稍后重试')
    }
  } catch (error) {
    console.error('获取点赞列表异常', error)
    message.error('获取点赞列表失败，请稍后重试')
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
const selectedLike = ref<API.LikeRecord | null>(null)

// 显示删除确认框
const showDeleteConfirm = (like: API.LikeRecord) => {
  selectedLike.value = like
  deleteConfirmVisible.value = true
}

// 确认删除
const confirmDelete = async () => {
  if (!selectedLike.value?.id) return

  try {
    const res = await batchOperationLikesUsingPost({
      ids: [selectedLike.value.id],
      operation: 'physical'
    })
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

// 切换排序顺序
const toggleSortOrder = () => {
  sortOrder.value = sortOrder.value === 'ascend' ? 'descend' : 'ascend'
  searchParams.sortOrder = sortOrder.value
  fetchData()
}

// 表格行选择状态
const state = reactive({
  selectedRowKeys: [] as number[],
  loading: false,
})
const onSelectChange = (selectedRowKeys: number[]) => {
  state.selectedRowKeys = selectedRowKeys
}
// 是否有选中的行
const hasSelected = computed(() => state.selectedRowKeys.length > 0)

// 批量删除选中的点赞记录
const batchDeleteSelectedLikes = async () => {
  if (state.selectedRowKeys.length === 0) {
    message.warning('请先选择要删除的记录')
    return
  }
  try {
    const res = await batchOperationLikesUsingPost({
      ids: state.selectedRowKeys,
      operation: 'physical'
    })
    if (res.data.code === 0) {
      message.success('批量删除成功')
      state.selectedRowKeys = []
      fetchData()
    } else {
      message.error('批量删除失败')
    }
  } catch (error) {
    message.error('批量删除出现异常，请稍后再试')
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
</script>

<style scoped>
#likeManagePage {
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

.action-bar > * {
  order: 1;
}

.action-bar .sort-button {
  order: 2;
  height: 32px;
  padding: 0 12px;
}

.like-card {
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

:deep(.van-card) {
  background: var(--header-background);
  color: var(--text-primary);
  padding: 10px 0;
}

:deep(.van-card__title) {
  font-size: 15px;
  font-weight: 500;
  margin-bottom: 4px;
}

:deep(.van-card__desc) {
  margin-top: 4px;
  color: #666;
  font-size: 13px;
  line-height: 1.4;
}

:deep(.van-tag) {
  margin-top: 6px;
  font-size: 11px;
}

:deep(.van-card__content) {
  padding-left: 8px;
}

:deep(.van-cell-group--inset) {
  margin: 0 12px 12px;
}

:deep(.van-cell-group--inset:last-child) {
  margin-bottom: 0;
}

.sort-button {
  height: 32px;
  padding: 0 12px;
}

:deep(.van-list) {
  min-height: 100px;
}

/* PC端按钮样式 */
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

/* PC端样式 */
.pc-container {
  background: var(--header-background);
  color: var(--text-primary);
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.06);
}

.search-and-button-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

/* 按钮统一样式 */
.action-button {
  height: 32px;
  border-radius: 8px;
  padding: 0 16px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition: all 0.3s ease;
  font-weight: 500;
}

.primary-button {
  background: linear-gradient(135deg, #ff8e53 0%, #ff6b6b 100%);
  border: none;
  box-shadow: 0 4px 12px rgba(255, 107, 107, 0.2);
}

.primary-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(255, 107, 107, 0.3);
}

.primary-button:active {
  transform: translateY(1px);
}

.sort-button {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  transition: all 0.3s ease;
}

.sort-button:hover {
  color: #ff8e53;
  border-color: #ff8e53;
  background: #fff6f3;
}

.delete-button {
  height: 28px;
  border-radius: 8px;
  padding: 0 12px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  font-weight: 500;
}

/* 表格样式 */
.table-section {
  background: var(--header-background);
  color: var(--text-primary);
  border-radius: 8px;
}

:deep(.custom-table) {
  .ant-table-thead > tr > th {
    background: #fafafa;
    font-weight: 500;
    color: #1f2937;
  }

  .ant-table-tbody > tr:hover > td {
    background: #fff6f3;
  }
}

/* 分页样式 */
.mz-antd-pagination {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

/* 搜索表单样式 */
:deep(.ant-form-inline) {
  .ant-form-item {
    margin-right: 16px;
  }

  .ant-input {
    border-radius: 8px;
  }
}

/* 移动端分页样式优化 */
.mobile-pagination {
  margin-top: 16px;
  padding: 12px;
  background: var(--header-background);
  color: var(--text-primary);
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.search-section {
  position: sticky;
  top: -12px;
  z-index: 100;
  background: var(--header-background);
  color: var(--text-primary);
  margin: 0 -12px 12px;
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

.page-size-selector .van-icon {
  font-size: 12px;
  color: #999;
}

.pagination-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

/* 分页器样式 */
:deep(.van-pagination) {
  width: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 4px;
}

:deep(.van-pagination__item) {
  min-width: 28px;
  height: 28px;
  line-height: 28px;
  border-radius: 16px;
  font-size: 14px;
  border: 1px solid #ebedf0;
  margin: 0 2px;
}

:deep(.van-pagination__item--active) {
  background: #1989fa;
  color: white;
  border-color: #1989fa;
}

:deep(.van-pagination__prev),
:deep(.van-pagination__next) {
  background: var(--header-background);
  color: var(--text-primary);
  border: 1px solid #ebedf0;
  font-weight: bold;
  min-width: 28px !important;
  height: 28px !important;
  line-height: 28px !important;
}

/* 跳转页码样式 */
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

/* 隐藏输入框的上下箭头 */
.jump-page :deep(.van-field__control::-webkit-inner-spin-button),
.jump-page :deep(.van-field__control::-webkit-outer-spin-button) {
  -webkit-appearance: none;
  margin: 0;
}

.jump-page :deep(.van-field__control) {
  -moz-appearance: textfield;
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

  .cancel-button,
  .confirm-button {
    min-width: 90px;
    height: 36px;
    font-size: 13px;
  }
}
</style>
