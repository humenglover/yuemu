<template>
  <div id="categoryManagePage">
    <!-- PC端展示 -->
    <template v-if="device === DEVICE_TYPE_ENUM.PC">
      <!-- 搜索表单与添加按钮容器，使其在同一行显示 -->
      <div class="search-and-add-container">
        <!-- 类型切换 -->
        <div class="type-switch">
          <a-radio-group v-model:value="searchParams.type" @change="handleTypeChange">
            <a-radio-button :value="0">图片分类</a-radio-button>
            <a-radio-button :value="1">帖子分类</a-radio-button>
            <a-radio-button :value="2">音频分类</a-radio-button>
          </a-radio-group>
        </div>
        <a-form layout="inline" :model="searchParams" @finish="doSearch">
          <a-form-item label="分类名称">
            <a-input
              v-model:value="searchParams.categoryName"
              placeholder="输入分类名称"
              allow-clear
            />
          </a-form-item>
          <a-form-item>
            <a-button type="primary" html-type="submit" class="action-button search-button">
              <SearchOutlined />搜索
            </a-button>
          </a-form-item>
        </a-form>
        <div class="add-button-wrapper">
          <a-button type="primary" class="action-button create-button" @click="showAddModal">
            <PlusOutlined />添加分类
          </a-button>
        </div>
      </div>
      <!-- 表格 -->
      <a-table
        :columns="columns"
        :data-source="categoryList"
        :pagination="false"
        @change="handleTableChange"
        class="category-table"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'type'">
            <a-tag :color="getTypeColor(record.type)">
              {{ getTypeText(record.type) }}
            </a-tag>
          </template>
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
            <a-space>
              <a-button
                class="table-button delete-button"
                @click="showDeleteConfirm(record)"
              >
                <DeleteOutlined />删除
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
      <!-- 分页组件 -->
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
          <!-- 移动端类型切换 -->
          <div class="mobile-type-switch">
            <van-tabs v-model:active="searchParams.type" @change="handleTypeChange">
              <van-tab :name="0" title="图片分类" />
              <van-tab :name="1" title="帖子分类" />
              <van-tab :name="2" title="音频分类" />
            </van-tabs>
          </div>

          <!-- 搜索区域 -->
          <div class="search-section">
            <van-search
              v-model="searchParams.categoryName"
              placeholder="搜索分类名称"
              @search="doSearch"
            />
          </div>

          <!-- 添加按钮 -->
          <div class="action-bar">
            <van-button type="primary" block @click="showAddModal" class="add-button">
              <template #icon><PlusOutlined /></template>
              添加分类
            </van-button>
          </div>

          <!-- 分类列表 -->
          <div class="category-list">
            <van-cell-group
              v-for="category in categoryList"
              :key="category.id"
              class="category-group"
            >
              <van-card class="category-card">
                <template #title>
                  <div class="card-title">{{ category.categoryName }}</div>
                </template>

                <template #desc>
                  <div class="card-info">
                    <div class="info-item">
                      <span class="label">分类ID：</span>
                      <span class="value">{{ category.id }}</span>
                    </div>
                    <div class="info-item">
                      <span class="label">创建时间：</span>
                      <span class="value">{{
                          dayjs(category.createTime).format('YYYY-MM-DD HH:mm:ss')
                        }}</span>
                    </div>
                  </div>
                </template>

                <template #footer>
                  <div class="card-footer">
                    <van-button
                      type="danger"
                      size="small"
                      @click="showDeleteConfirm(category)"
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

    <!-- 添加分类模态框 -->
    <a-modal
      v-model:open="addModalVisible"
      title="添加分类"
      @ok="handleAdd"
      @cancel="addModalVisible = false"
    >
      <a-form :model="addForm" ref="addFormRef">
        <a-form-item label="分类类型" required>
          <a-radio-group v-model:value="addForm.type">
            <a-radio :value="0">图片分类</a-radio>
            <a-radio :value="1">帖子分类</a-radio>
            <a-radio :value="2">音频分类</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="分类名称" name="categoryName">
          <a-input v-model:value="addForm.categoryName" />
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
        <h3 class="confirm-title">确认删除该分类？</h3>
        <p class="confirm-desc">
          分类名称：{{ selectedCategory?.categoryName }}<br>
          分类ID：{{ selectedCategory?.id }}
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
import { ref, reactive, onMounted, watch } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, DeleteOutlined, SearchOutlined, ExclamationCircleFilled } from '@ant-design/icons-vue'
import { DEVICE_TYPE_ENUM } from '@/constants/device'
import { getDeviceType } from '@/utils/device'
import {
  addCategoryUsingPost,
  deleteCategoryUsingPost,
  listCategoryVoUsingPost,
  findCategoryUsingPost,
} from '@/api/categoryController.ts'
import dayjs from 'dayjs'

// 定义搜索参数类型，与标签管理页面的搜索参数类型结构保持一致
type SearchParams = {
  current: number
  pageSize: number
  categoryName?: string
  sortField?: string
  sortOrder?: string
  type: number
}

// 表格列配置
const columns = [
  {
    title: 'ID',
    dataIndex: 'id',
    key: 'id',
  },
  {
    title: '分类名称',
    dataIndex: 'categoryName',
    key: 'categoryName',
  },
  {
    title: '分类类型',
    dataIndex: 'type',
    key: 'type',
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

// 搜索表单数据
const searchParams = reactive<SearchParams>({
  current: 1,
  pageSize: 10,
  categoryName: '',
  sortField: 'createTime',
  sortOrder: 'ascend',
  type: 0,
})

// 分类列表数据
const categoryList = ref([])
// 总记录数
const total = ref(0)
// 分页配置，这里保持和标签管理页面类似的结构，可根据实际需求调整初始值等
const pagination = ref({})
// PC端分页选项
const pcPageSizeOptions = ['5', '8', '10', '20', '50']
// 设备类型
const device = ref<string>('')
onMounted(async () => {
  device.value = await getDeviceType()
})

// 添加分类模态框是否可见
const addModalVisible = ref(false)
// 添加分类表单数据
const addForm = reactive({
  categoryName: '',
  type: 0,
})

// 获取分类列表数据
const getCategoryList = async () => {
  try {
    const res = await listCategoryVoUsingPost({
      ...searchParams,
    })
    if (res.data.code === 0 && res.data.data) {
      categoryList.value = res.data.data.records
      total.value = res.data.data.total
    } else {
      message.error('获取分类列表失败')
    }
  } catch (error) {
    console.error('获取分类列表出错', error)
    message.error('获取分类列表失败')
  }
}

// 页面加载时获取分类列表
onMounted(() => {
  getCategoryList()
})

// 处理分页尺寸改变时的逻辑（每页显示条数改变）
const onShowSizeChange = (current: number, pageSize: number) => {
  searchParams.current = 1 // 切换每页条数时，默认回到第一页，可根据需求调整
  searchParams.pageSize = pageSize
  getCategoryList()
}

// 处理页码改变时的逻辑
const onPageChange = (page: number, pageSize: number) => {
  searchParams.current = page
  searchParams.pageSize = pageSize
  getCategoryList()
}

// 处理表格页码、页大小改变等操作（包括排序等情况）
const handleTableChange = (paginationParam: any) => {
  searchParams.current = paginationParam.current
  searchParams.pageSize = paginationParam.pageSize
  if (paginationParam.sortField && paginationParam.sortOrder) {
    searchParams.sortField = paginationParam.sortField
    searchParams.sortOrder = paginationParam.sortOrder === 'ascend' ? 'ascend' : 'descend'
  }
  getCategoryList()
}

// 处理搜索操作，优化了未输入信息时的逻辑
const doSearch = () => {
  // 重置页码
  searchParams.current = 1
  getCategoryList()
}

// 显示添加分类模态框
const showAddModal = () => {
  addModalVisible.value = true
}

// 处理添加分类操作
const handleAdd = async () => {
  const categoryNameValue = addForm.categoryName.trim()
  if (categoryNameValue === '') {
    message.error('请输入分类名称')
    return
  }
  try {
    const addParams = {
      categoryName: categoryNameValue,
      type: addForm.type,
    }
    const res = await addCategoryUsingPost(addParams)
    if (res.data.code === 0) {
      // message.success('添加分类成功')
      addModalVisible.value = false
      getCategoryList()
    } else {
      message.error('添加分类失败')
    }
  } catch (error) {
    console.error('添加分类出错', error)
    message.error('添加分类失败')
  }
}

// 删除确认相关的状态
const deleteConfirmVisible = ref(false)
const selectedCategory = ref<API.CategoryVO | null>(null)

// 显示删除确认框
const showDeleteConfirm = (category: API.CategoryVO) => {
  selectedCategory.value = category
  deleteConfirmVisible.value = true
}

// 确认删除
const confirmDelete = async () => {
  if (!selectedCategory.value?.id) return

  try {
    const res = await deleteCategoryUsingPost({ categoryId: selectedCategory.value.id })
    if (res.data.code === 0) {
      message.success('删除成功')
      deleteConfirmVisible.value = false
      // 刷新数据
      getCategoryList()
    } else {
      message.error('删除失败：' + res.data.message)
    }
  } catch (error) {
    message.error('删除失败，请稍后重试')
  }
}

const showPageSizeSheet = ref(false)
const jumpPage = ref('')

// 移动端分页选项
const pageSizeOptions = [
  { name: '10条/页', value: 10 },
  { name: '20条/页', value: 20 },
  { name: '30条/页', value: 30 },
  { name: '50条/页', value: 50 },
]

// 移动端分页处理方法
const onMobilePageChange = (page: number) => {
  searchParams.current = page
  getCategoryList()
}

const handlePageSizeChange = (action: { value: number }) => {
  searchParams.current = 1
  searchParams.pageSize = action.value
  getCategoryList()
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
  getCategoryList()
  jumpPage.value = ''
}

// 处理类型切换
const handleTypeChange = () => {
  searchParams.current = 1
  getCategoryList()
}

const getTypeColor = (type: number) => {
  switch (type) {
    case 0:
      return 'blue'
    case 1:
      return 'green'
    case 2:
      return 'purple'
    default:
      return 'default'
  }
}

const getTypeText = (type: number) => {
  switch (type) {
    case 0:
      return '图片分类'
    case 1:
      return '帖子分类'
    case 2:
      return '音频分类'
    default:
      return '未知类型'
  }
}
</script>

<style scoped>
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

/* PC端表格和分页容器样式 */
.category-table {
  margin-bottom: 16px;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
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
  background: linear-gradient(135deg, #fca5a5 0%, #ef4444 100%);
  box-shadow: 0 2px 8px rgba(239, 68, 68, 0.15);
}

.delete-button:active {
  background: linear-gradient(135deg, #f87171 0%, #dc2626 100%);
  transform: translateY(1px);
  box-shadow: 0 2px 6px rgba(239, 68, 68, 0.1);
}

/* 删除按钮图标样式 */
.delete-button :deep(.anticon) {
  font-size: 14px;
  transition: transform 0.3s ease;
}

.delete-button:active :deep(.anticon) {
  transform: rotate(-15deg);
}

/* 移动端样式 */
.mobile-container {
  background: #f7f8fa;
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
  background: linear-gradient(135deg, #36cfc9 0%, #06b6d4 100%);
  border: none;
  border-radius: 8px;
  height: 36px;
  font-size: 14px;
  box-shadow: 0 4px 12px rgba(6, 182, 212, 0.2);
  transition: all 0.3s ease;
}

.add-button:active {
  background: linear-gradient(135deg, #40d9d4, #0891b2);
  transform: translateY(1px);
  box-shadow: 0 2px 8px rgba(6, 182, 212, 0.15);
}

/* 按钮图标样式 */
.add-button :deep(.anticon) {
  font-size: 16px;
  margin-right: 4px;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% {
    opacity: 0.8;
    transform: scale(1);
  }
  50% {
    opacity: 1;
    transform: scale(1.1);
  }
  100% {
    opacity: 0.8;
    transform: scale(1);
  }
}

.category-list {
  padding: 0;
}

.category-group {
  margin-bottom: 8px;
}

.category-card {
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
.search-section {
  position: sticky;
  top: 0; /* 调整搜索栏的粘性定位位置 */
  z-index: 100;
  background: #f7f8fa;
  /* 抵消父元素的内边距 */
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

/* 按钮激活状态 */
.action-button:active,
.table-button:active,
:deep(.van-button:active) {
  transform: translateY(0);
  opacity: 0.9;
}

.mobile-button {
  border: none;
  border-radius: 6px;
  font-size: 13px;
  height: 28px;
  padding: 0 12px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  transition: all 0.3s ease;
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

/* 类型切换样式 */
.type-switch {
  margin-bottom: 16px;
}

.mobile-type-switch {
  margin-bottom: 16px;
  background: white;
  border-radius: 8px;
  overflow: hidden;
}

/* 移动端标签页样式 */
:deep(.van-tabs) {
  --van-tabs-line-height: 36px;
  --van-tabs-bottom-bar-color: #ff6b6b;
}

:deep(.van-tab) {
  font-size: 14px;
  color: #64748b;
}

:deep(.van-tab--active) {
  color: #ff6b6b;
  font-weight: 500;
}
</style>
