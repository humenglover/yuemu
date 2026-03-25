<template>
  <div id="audioManagePage">
    <!-- PC端展示 -->
    <template v-if="device === DEVICE_TYPE_ENUM.PC">
      <div class="pc-container">
        <!-- 搜索表单 -->
        <div class="search-and-button-container">
          <a-form layout="inline" :model="searchParams" @finish="doSearch">
            <a-form-item label="文件名">
              <a-input
                v-model:value="searchParams.fileName"
                placeholder="输入文件名"
                allow-clear
              />
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
              @click="batchDeleteSelectedAudios"
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
                <template v-if="column.dataIndex === 'fileName'">
                  <a-tooltip :title="record.fileName">
                    <div class="ellipsis-text">
                      {{ record.fileName }}
                    </div>
                  </a-tooltip>
                </template>
                <template v-if="column.dataIndex === 'createTime'">
                  <div class="ellipsis-text">
                    {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}
                  </div>
                </template>
                <template v-else-if="column.dataIndex === 'audio'">
                  <AudioBubble :url="record.fileUrl" :isSelf="false" />
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
              v-model="searchParams.fileName"
              placeholder="搜索文件名"
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
              @click="batchDeleteSelectedAudios"
            >批量删除</van-button>
          </div>

          <!-- 音频列表 -->
          <div class="audio-list">
            <van-checkbox-group v-model="state.selectedRowKeys">
              <van-cell-group inset v-for="audio in dataList" :key="audio.id">
                <van-card class="audio-card">
                  <template #title>
                    <div class="audio-info">
                      <div class="audio-name">{{ audio.fileName }}</div>
                      <div class="audio-meta">
                        <div class="meta-item">
                          <span class="meta-label">音频ID：</span>
                          <span class="meta-value">{{ audio.id }}</span>
                        </div>
                        <div class="meta-item">
                          <span class="meta-label">用户ID：</span>
                          <span class="meta-value">{{ audio.userId }}</span>
                        </div>
                        <div class="meta-item">
                          <span class="meta-label">文件大小：</span>
                          <span class="meta-value">{{ formatFileSize(audio.fileSize) }}</span>
                        </div>
                        <div class="meta-item">
                          <span class="meta-label">时长：</span>
                          <span class="meta-value">{{ formatDuration(audio.duration) }}</span>
                        </div>
                        <div class="meta-item">
                          <span class="meta-label">创建时间：</span>
                          <span class="meta-value">{{ dayjs(audio.createTime).format('YYYY-MM-DD HH:mm:ss') }}</span>
                        </div>
                      </div>
                      <!-- 音频播放器 -->
                      <div class="audio-player-container">
                        <AudioBubble :url="audio.fileUrl" :isSelf="false" />
                      </div>
                    </div>
                  </template>

                  <template #footer>
                    <div class="card-footer">
                      <van-checkbox
                        :name="audio.id"
                        class="card-checkbox"
                      />
                      <div class="card-actions">
                        <van-button
                          size="small"
                          type="danger"
                          @click="showDeleteConfirm(audio)"
                          class="action-btn delete-btn"
                        >删除</van-button>
                      </div>
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
            <div class="pagination-controls">
              <van-button
                :disabled="searchParams.current <= 1"
                @click="onMobilePageChange(searchParams.current - 1)"
                size="small"
              >上一页</van-button>
              <span class="page-info">{{ searchParams.current }} / {{ Math.ceil(total / searchParams.pageSize) }}</span>
              <van-button
                :disabled="searchParams.current >= Math.ceil(total / searchParams.pageSize)"
                @click="onMobilePageChange(searchParams.current + 1)"
                size="small"
              >下一页</van-button>
            </div>
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
              <van-button size="small" type="primary" @click="handleJumpPage">确定</van-button>
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
        <h3 class="confirm-title">确认删除该音频？</h3>
        <p class="confirm-desc">
          文件名：{{ selectedAudio?.fileName || '无名称' }}<br>
          创建时间：{{ selectedAudio ? dayjs(selectedAudio.createTime).format('YYYY-MM-DD HH:mm:ss') : '未知' }}
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
import {
  listAudioByPageAdminUsingGet,
  deleteAudioUsingDelete,
  batchDeleteAudioUsingDelete,
} from '@/api/audioFileController'
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
import AudioBubble from '@/components/AudioBubble.vue'

// 定义用于存储设备类型的响应式变量
const device = ref<string>('')
// 页面加载时获取设备类型并获取数据
onMounted(async () => {
  device.value = await getDeviceType()
})

// 表格列定义
const columns = [
  {
    title: '文件名',
    dataIndex: 'fileName',
    width: 200,
    ellipsis: true,
  },
  {
    title: '音频ID',
    dataIndex: 'id',
    width: 100,
  },
  {
    title: '用户ID',
    dataIndex: 'userId',
    width: 100,
  },
  {
    title: '文件大小',
    dataIndex: 'fileSize',
    width: 100,
    customRender: ({ text }) => formatFileSize(text),
  },
  {
    title: '时长',
    dataIndex: 'duration',
    width: 100,
    customRender: ({ text }) => formatDuration(text),
  },
  {
    title: '音频',
    dataIndex: 'audio',
    width: 200,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 200,
  },
  {
    title: '操作',
    key: 'action',
    width: 200,
  },
]

// 定义数据
const dataList = ref<API.AudioFile[]>([])
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
] as const

// 搜索条件
const searchParams = reactive({
  current: 1,
  pageSize: 8,
  sortField: 'createTime',
  sortOrder: 'descend',
  fileName: '',
})

const sortOrder = computed(() => searchParams.sortOrder)

// 获取数据
const fetchData = async () => {
  if (device.value === DEVICE_TYPE_ENUM.PC) {
    loading.value = true
  }

  try {
    const res = await listAudioByPageAdminUsingGet(searchParams)
    if (res.data?.code === 0) {
      dataList.value = res.data.data.records
      total.value = res.data.data.total
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
const doTableChange = () => {
  fetchData()
}

// 搜索数据
const doSearch = async () => {
  searchParams.current = 1
  await fetchData()
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
const selectedAudio = ref<API.AudioFile | null>(null)

// 显示删除确认框
const showDeleteConfirm = (audio: API.AudioFile) => {
  selectedAudio.value = audio
  deleteConfirmVisible.value = true
}

// 确认删除
const confirmDelete = async () => {
  if (!selectedAudio.value?.id) return

  try {
    const res = await deleteAudioUsingDelete({ id: selectedAudio.value.id })
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

// 切换排序顺序
const toggleSortOrder = () => {
  searchParams.sortOrder = searchParams.sortOrder === 'ascend' ? 'descend' : 'ascend'
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

// 计算属性，判断是否有选中的行
const hasSelected = computed(() => state.selectedRowKeys.length > 0)

// 批量删除选中音频
const batchDeleteSelectedAudios = async () => {
  if (state.selectedRowKeys.length === 0) {
    message.warning('请先选择要删除的音频')
    return
  }

  try {
    const res = await batchDeleteAudioUsingDelete(state.selectedRowKeys)
    if (res.data?.code === 0) {
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
  if (page < 1 || page > Math.ceil(total.value / searchParams.pageSize)) {
    return
  }
  searchParams.current = page
  await fetchData()
}

// 处理每页条数改变
const handlePageSizeChange = async (action: { value: number }) => {
  searchParams.current = 1
  searchParams.pageSize = action.value
  showPageSizeSheet.value = false // 关闭选择器
  await fetchData()
}

// 处理页码跳转
const handleJumpPage = async () => {
  const page = parseInt(jumpPage.value)
  if (isNaN(page)) {
    return
  }

  const maxPage = Math.ceil(total.value / searchParams.pageSize)
  if (page < 1 || page > maxPage) {
    message.warning(`Please enter a page number between 1 and ${maxPage}`)
    return
  }

  searchParams.current = page
  await fetchData()
  jumpPage.value = ''
}

// 格式化文件大小
const formatFileSize = (size: number) => {
  if (!size) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let index = 0
  while (size >= 1024 && index < units.length - 1) {
    size /= 1024
    index++
  }
  return `${size.toFixed(2)} ${units[index]}`
}

// 格式化时长
const formatDuration = (duration: number) => {
  if (!duration) return '0:00'
  const minutes = Math.floor(duration / 60)
  const seconds = duration % 60
  return `${minutes}:${seconds.toString().padStart(2, '0')}`
}
</script>

<style scoped>
#audioManagePage {
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

.audio-card {
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

/* PC端样式 */
.pc-container {
  background: var(--header-background);
  color: var(--text-primary);
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.06);
}

.table-section {
  background: var(--header-background);
  color: var(--text-primary);
  border-radius: 8px;
}

/* 删除确认弹框样式 */
.delete-confirm-modal :deep(.ant-modal-content) {
  padding: 0;
  border-radius: 16px;
  overflow: hidden;
}

.delete-confirm-modal :deep(.ant-modal-body) {
  padding: 0;
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

/* 响应式样式 */
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

/* 移动端音频卡片样式 */
.audio-card {
  background: var(--header-background);
  border-radius: 12px;
  margin-bottom: 12px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.audio-info {
  width: 100%;
}

.audio-name {
  font-size: 15px;
  color: var(--text-primary);
  line-height: 1.5;
  margin-bottom: 12px;
  word-break: break-all;
}

.audio-meta {
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 13px;
}

.meta-item {
  display: flex;
  align-items: center;
  color: var(--text-secondary);
}

.meta-label {
  min-width: 70px;
  color: var(--text-secondary);
}

.meta-value {
  color: var(--text-primary);
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--border-color);
}

.card-actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  padding: 0 12px;
  border-radius: 4px;
}

.delete-btn {
  color: #fff;
  background: #ef4444;
  border: none;
}

/* 移动端分页样式优化 */
.mobile-pagination {
  background: var(--header-background);
  padding: 16px;
  border-radius: 12px;
  margin-top: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.pagination-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  color: var(--text-secondary);
}

.page-size-selector {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  cursor: pointer;
}

.pagination-controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.page-info {
  color: var(--text-primary);
  font-size: 14px;
}

.jump-page {
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: center;
  color: var(--text-secondary);
}

.jump-page :deep(.van-field) {
  width: 60px;
  margin: 0 4px;
}

.jump-page :deep(.van-field__control) {
  height: 28px;
  padding: 0 8px;
  text-align: center;
  background: var(--header-background);
  color: var(--text-primary);
}

/* 适配深色模式 */
:deep(.van-card) {
  background: var(--header-background);
  color: var(--text-primary);
}

:deep(.van-cell-group) {
  background: var(--header-background);
}

:deep(.van-button) {
  background: var(--header-background);
  border-color: var(--border-color);
  color: var(--text-primary);
}

/* 文本省略样式 */
.ellipsis-text {
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
}

/* 移动端按钮样式 */
:deep(.van-button--danger) {
  color: #fff !important;
  background: #ef4444 !important;
  border: none !important;
}

/* 移动端分页按钮样式 */
.pagination-controls .van-button {
  min-width: 80px;
  height: 32px;
  border-radius: 6px;
  font-size: 14px;
}

.pagination-controls .van-button:not(:disabled) {
  color: var(--text-primary) !important;
  background: var(--header-background) !important;
  border: 1px solid var(--border-color) !important;
}

.pagination-controls .van-button:disabled {
  opacity: 0.5;
  background: var(--header-background) !important;
  border: 1px solid var(--border-color) !important;
}

/* 跳转页面按钮样式 */
.jump-page .van-button {
  height: 28px;
  padding: 0 12px;
  border-radius: 4px;
  font-size: 13px;
}

/* 操作按钮样式 */
.action-bar .van-button {
  height: 32px;
  padding: 0 16px;
  border-radius: 6px;
  font-size: 14px;
}

/* 卡片操作按钮样式 */
.card-actions .action-btn {
  height: 28px;
  padding: 0 12px;
  border-radius: 4px;
  font-size: 13px;
}

.card-actions .delete-btn {
  color: #fff !important;
  background: #ef4444 !important;
  border: none !important;
}

/* 修复深色模式按钮样式 */
:deep(.van-button--default) {
  background: var(--header-background);
  border-color: var(--border-color);
  color: var(--text-primary);
}

:deep(.van-button--danger) {
  background: #ef4444;
  border-color: #ef4444;
  color: #fff;
}

/* 音频播放器容器样式 */
.audio-player-container {
  margin-top: 12px;
  margin-bottom: 8px;
}
</style>
