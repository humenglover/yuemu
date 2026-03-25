<template>
  <div id="tagManagePage">
    <!-- PC端展示 -->
    <template v-if="device === DEVICE_TYPE_ENUM.PC">
      <!-- 搜索与添加按钮容器，使其在同一行显示 -->
      <div class="search-and-add-container">
        <a-form layout="inline" :model="searchParams" @finish="doSearch">
          <a-form-item label="标签名称">
            <a-input v-model:value="searchParams.tagName" placeholder="输入标签名称" allow-clear />
          </a-form-item>
          <a-form-item>
            <a-button type="primary" html-type="submit" class="action-button search-button">
              <SearchOutlined />搜索
            </a-button>
          </a-form-item>
        </a-form>
        <div class="add-button-wrapper">
          <a-button type="primary" class="action-button create-button" @click="showAddModal">
            <PlusOutlined />添加标签
          </a-button>
        </div>
      </div>
      <div style="margin-bottom: 16px" />
      <!-- 表格 -->
      <a-table
        :columns="columns"
        :data-source="tagList"
        :pagination="false"
        @change="handleTableChange"
        class="tag-table"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'createTime'">
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
            <a-button
              class="table-button delete-button"
              @click="showDeleteConfirm(record)"
            >
              <DeleteOutlined />删除
            </a-button>
          </template>
        </template>
      </a-table>
      <div class="pagination-container">
        <a-pagination
          v-model:current="searchParams.current"
          :page-size-options="pcPageSizeOptions"
          :total="total"
          show-size-changer
          :page-size="searchParams.pageSize"
          @change="onPageChange"
          @showSizeChange="onShowSizeChange"
        >
          <template #buildOptionText="props">
            <span>{{ props.value }}条/页</span>
          </template>
        </a-pagination>
      </div>
    </template>

    <!-- 移动端展示 -->
    <template v-else>
      <div class="mobile-container">
        <div class="mobile-content">
          <!-- 搜索区域 -->
          <div class="search-section">
            <van-search
              v-model="searchParams.tagName"
              placeholder="搜索标签名称"
              @search="doSearch"
            />
          </div>

          <!-- 添加按钮 -->
          <div class="action-bar">
            <van-button type="primary" block @click="showAddModal" class="add-button">
              <template #icon><PlusOutlined /></template>
              添加标签
            </van-button>
          </div>

          <!-- 标签列表 -->
          <div class="tag-list">
            <van-cell-group v-for="tag in tagList" :key="tag.id" class="tag-group">
              <van-card class="tag-card">
                <template #title>
                  <div class="card-title">{{ tag.tagName }}</div>
                </template>

                <template #desc>
                  <div class="card-info">
                    <div class="info-item">
                      <span class="label">标签ID：</span>
                      <span class="value">{{ tag.id }}</span>
                    </div>
                    <div class="info-item">
                      <span class="label">创建时间：</span>
                      <span class="value">{{
                        dayjs(tag.createTime).format('YYYY-MM-DD HH:mm:ss')
                      }}</span>
                    </div>
                  </div>
                </template>

                <template #footer>
                  <div class="card-footer">
                    <van-button
                      type="danger"
                      size="small"
                      @click="showDeleteConfirm(tag)"
                      class="mobile-button delete-button"
                    >
                      <template #icon><DeleteOutlined /></template>
                      删除
                    </van-button>
                  </div>
                </template>
              </van-card>
            </van-cell-group>
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
                :show-prev-text="false"
                :show-next-text="false"
                :show-page-size="3"
                class="custom-pagination"
                force-ellipses
              >
                <template #prev-text>
                  <van-icon name="arrow-left" />
                </template>
                <template #next-text>
                  <van-icon name="arrow" />
                </template>
              </van-pagination>
              <div class="jump-page">
                <span>跳至</span>
                <van-field v-model="jumpPage" type="number" @keypress.enter="handleJumpPage" />
                <span>页</span>
              </div>
            </div>
          </div>

          <!-- 每页条数选择器 -->
          <van-action-sheet
            v-model:show="showPageSizeSheet"
            :actions="pageSizeOptions"
            cancel-text="取消"
            close-on-click-action
            @select="handlePageSizeChange"
          />
        </div>
      </div>
    </template>

    <!-- 添加标签模态框 -->
    <a-modal v-model:open="addModalVisible" title="添加标签" @ok="handleAdd">
      <a-form ref="addFormRef">
        <a-form-item label="标签名称" name="tagName">
          <a-input v-model:value="addForm.tagName" />
        </a-form-item>
      </a-form>
    </a-modal>

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
        <h3 class="confirm-title">确认删除该标签？</h3>
        <p class="confirm-desc">
          标签名称：{{ selectedTag?.tagName }}<br>
          标签ID：{{ selectedTag?.id }}
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
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import {
  addTagUsingPost,
  deleteTagUsingPost,
  listTagVoByPageUsingPost,
  searchTagUsingPost,
} from '@/api/tagController.ts'
import dayjs from 'dayjs'
import { PlusOutlined, DeleteOutlined, SearchOutlined, ExclamationCircleFilled } from '@ant-design/icons-vue'
import { DEVICE_TYPE_ENUM } from '@/constants/device'
import { getDeviceType } from '@/utils/device'

// 定义搜索参数类型
type SearchParams = {
  current: number
  pageSize: number
  tagName?: string
  sortField?: string
  sortOrder?: string
}

// 表格列配置
const columns = [
  {
    title: '标签ID',
    dataIndex: 'id',
    key: 'id',
  },
  {
    title: '标签名称',
    dataIndex: 'tagName',
    key: 'tagName',
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    key: 'createTime',
  },
  {
    title: '操作',
    key: 'action',
    scopedSlots: { customRender: 'action' },
  },
]

// 标签列表数据
const tagList = ref([])
// 总记录数
const total = ref(0)
// 分页配置及搜索参数整合
const searchParams = reactive<SearchParams>({
  current: 1,
  pageSize: 8,
  sortField: 'createTime',
  sortOrder: 'ascend',
  tagName: '',
})
// PC端分页选项
const pcPageSizeOptions = ['5', '8', '10', '20', '50']
// 添加标签模态框是否可见
const addModalVisible = ref(false)
// 添加标签表单数据
const addForm = reactive({
  tagName: '',
})

// 设备类型
const device = ref<string>('')
onMounted(async () => {
  device.value = await getDeviceType()
})

const showPageSizeSheet = ref(false)
const jumpPage = ref('')

// 移动端分页选项
const pageSizeOptions = [
  { name: '10条/页', value: 10 },
  { name: '20条/页', value: 20 },
  { name: '30条/页', value: 30 },
  { name: '50条/页', value: 50 },
]

// 获取标签列表数据
const getTagList = async () => {
  try {
    const res = await listTagVoByPageUsingPost({
      ...searchParams,
    })
    if (res.data.code === 0 && res.data.data) {
      tagList.value = res.data.data.records
      total.value = res.data.data.total
    } else {
      message.error('获取标签列表失败')
    }
  } catch (error) {
    console.error('获取标签列表出错', error)
    message.error('获取标签列表失败')
  }
}

// 页面加载时获取标签列表
onMounted(() => {
  getTagList()
})

// 处理分页尺寸改变时的逻辑（每页显示条数改变）
const onShowSizeChange = (current: number, pageSize: number) => {
  searchParams.current = 1 // 切换每页条数时，默认回到第一页，可根据需求调整
  searchParams.pageSize = pageSize
  getTagList()
}

// 处理页码改变时的逻辑
const onPageChange = (page: number, pageSize: number) => {
  searchParams.current = page
  searchParams.pageSize = pageSize
  getTagList()
}

// 处理表格页码、页大小改变等操作（包括排序等情况）
const handleTableChange = (paginationParam: any) => {
  searchParams.current = paginationParam.current
  searchParams.pageSize = paginationParam.pageSize
  if (paginationParam.sortField && paginationParam.sortOrder) {
    searchParams.sortField = paginationParam.sortField
    searchParams.sortOrder = paginationParam.sortOrder === 'ascend' ? 'ascend' : 'descend'
  }
  getTagList()
}

// 处理搜索操作，优化了未输入信息时的逻辑
const doSearch = () => {
  // 重置页码
  searchParams.current = 1
  getTagList()
}

// 显示添加标签模态框
const showAddModal = () => {
  addModalVisible.value = true
}

// 处理添加标签操作，确保正确获取输入值并传递给后端
const handleAdd = async () => {
  const tagNameValue = addForm.tagName // 获取输入框的值并去除两端空格
  if (tagNameValue === '') {
    message.error('请输入标签名称')
    return
  }
  try {
    const addParams = {
      tagName: tagNameValue, // 使用处理后的非空值传递给后端
    }
    const res = await addTagUsingPost(addParams)
    if (res.data.code === 0) {
      // message.success('添加标签成功')
      addModalVisible.value = false
      getTagList()
    } else {
      message.error('添加标签失败')
    }
  } catch (error) {
    console.error('添加标签出错', error)
    message.error('添加标签失败')
  }
}

// 删除确认相关的状态
const deleteConfirmVisible = ref(false)
const selectedTag = ref<API.TagVO | null>(null)

// 显示删除确认框
const showDeleteConfirm = (tag: API.TagVO) => {
  selectedTag.value = tag
  deleteConfirmVisible.value = true
}

// 确认删除
const confirmDelete = async () => {
  if (!selectedTag.value?.id) return

  try {
    const res = await deleteTagUsingPost({ id: selectedTag.value.id })
    if (res.data.code === 0) {
      message.success('删除成功')
      deleteConfirmVisible.value = false
      // 刷新数据
      getTagList()
    } else {
      message.error('删除失败：' + res.data.message)
    }
  } catch (error) {
    message.error('删除失败，请稍后重试')
  }
}

// 移动端分页处理方法
const onMobilePageChange = (page: number) => {
  searchParams.current = page
  getTagList()
}

const handlePageSizeChange = (action: { value: number }) => {
  searchParams.current = 1
  searchParams.pageSize = action.value
  getTagList()
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
  getTagList()
  jumpPage.value = ''
}
</script>

<style scoped>
@media screen and (min-width: 768px) {
  #tagManagePage {
    background: var(--header-background);
    color: var(--text-primary)!important;
    width: 100vw;
    margin: 0 auto;
    padding: 24px;
  }
}

/* 可以在这里添加页面的自定义样式 */
.search-and-add-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.add-button-wrapper {
  margin-left: 10px;
}

.custom-button {
  margin-left: 0;
}
.mz-antd-pagination {
  text-align: right;
  margin-top: 20px;
}

/* 移动端样式 */
.mobile-container {
  min-height: 100vh;
  padding-bottom: 50px;

}

.mobile-content {
  padding: 12px;
}

.action-bar {
  margin: 12px;
}

.add-button {
  background: linear-gradient(135deg, #40c9ff 0%, #1890ff 100%);
  border: none;
  border-radius: 8px;
  height: 36px;
  font-size: 14px;
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.2);
}

.tag-list {
  padding: 0;
}

.tag-group {
  margin-bottom: 8px;
}

.tag-card {
  background: #fff;
  width: 100%;
}

.card-title {
  font-size: 16px;
  font-weight: 500;
  color: #323233;
  margin-bottom: 8px;
}

.card-info {
  font-size: 14px;
  color: #666;
}

.info-item {
  margin-bottom: 8px;
}

.info-item .label {
  color: #999;
  margin-right: 8px;
}

.info-item .value {
  color: #323233;
}

.card-footer {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}

/* 分页样式 */
.mobile-pagination {
  margin-top: 16px;
  padding: 12px;
  background: white;
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
  background: white;
  cursor: pointer;
}

.pagination-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
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
  color: white;
  border: none;
}

/* 搜索按钮 */
.search-button {
  background: linear-gradient(135deg, #36cfc9 0%, #13c2c2 100%);
  box-shadow: 0 4px 12px rgba(19, 194, 194, 0.2);
}

.search-button:hover {
  background: linear-gradient(135deg, #40d9d4 0%, #36cfc9 100%);
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(19, 194, 194, 0.3);
}

/* 创建按钮 */
.create-button {
  background: linear-gradient(135deg, #40c9ff 0%, #1890ff 100%);
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.2);
}

.create-button:hover {
  background: linear-gradient(135deg, #69d4ff 0%, #40a9ff 100%);
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(24, 144, 255, 0.3);
}

/* 表格按钮样式 */
.table-button {
  height: 28px;
  border-radius: 6px;
  padding: 0 12px;
  font-size: 13px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: none;
  transition: all 0.3s ease;
  color: white;
}

.table-button .anticon {
  font-size: 14px;
}

/* 删除按钮 */
.delete-button {
  background: linear-gradient(135deg, #ff7875 0%, #ff4d4f 100%);
  box-shadow: 0 2px 8px rgba(255, 77, 79, 0.2);
}

.delete-button:hover {
  background: linear-gradient(135deg, #ff9c9a 0%, #ff7875 100%);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(255, 77, 79, 0.3);
}

/* 移动端按钮样式 */
.mobile-button {
  border: none;
  border-radius: 6px;
  font-size: 13px;
  height: 28px;
  padding: 0 12px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

/* 分页器样式优化 */
:deep(.custom-pagination) {
  .van-pagination__item {
    min-width: 28px;
    height: 28px;
    line-height: 28px;
    border-radius: 16px;
    font-size: 14px;
    border: 1px solid #ebedf0;
    margin: 0 2px;
  }

  .van-pagination__item--active {
    background: #1989fa;
    color: white;
    border-color: #1989fa;
  }

  .van-pagination__prev,
  .van-pagination__next {
    background: #f7f8fa;
    border: 1px solid #ebedf0;
    font-weight: bold;
    min-width: 28px !important;
    height: 28px !important;
    line-height: 28px !important;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .van-icon {
    font-size: 14px;
    color: #666;
  }
}
.search-section {
  position: sticky;
  top: 4px; /* 调整搜索栏的粘性定位位置 */
  z-index: 100;
  background: #f7f8fa;
  /* 抵消父元素的内边距 */
  margin: 0 -12px 12px;
}

/* 按钮激活状态 */
.action-button:active,
.table-button:active,
:deep(.van-button:active) {
  transform: translateY(0);
  opacity: 0.9;
}

/* PC端表格和分页容器样式 */
.tag-table {
  margin-bottom: 16px;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
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

/* 移动端适配 */
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
