<template>
  <div id="MyPostsPage">
    <!-- 切换按钮 -->
    <div class="content-switch">
      <div class="switch-tabs">
        <div
          class="switch-tab"
          :class="{ active: contentType === 'picture' }"
          @click="contentType = 'picture'"
        >
          <PictureOutlined />
          <span>我的图片</span>
        </div>
        <div
          class="switch-tab"
          :class="{ active: contentType === 'post' }"
          @click="contentType = 'post'"
        >
          <FileTextOutlined />
          <span>我的帖子</span>
        </div>
      </div>
    </div>

    <!-- 搜索区域 -->
    <template v-if="device !== DEVICE_TYPE_ENUM.PC">
      <div class="mobile-search-container">
        <!-- 搜索区域 -->
        <div class="search-section">
          <a-input
            v-model:value="searchParams.searchText"
            :placeholder="contentType === 'picture' ? '搜索图片' : '搜索帖子'"
            allow-clear
            class="mobile-search-input"
            @pressEnter="doSearch"
          >
            <template #suffix>
              <SearchOutlined class="search-icon" @click="doSearch" />
            </template>
          </a-input>
          <a-select
            v-model:value="searchParams.reviewStatus"
            placeholder="审核状态"
            :options="contentType === 'picture' ? PIC_REVIEW_STATUS_OPTIONS : POST_STATUS_OPTIONS"
            allow-clear
            class="mobile-status-select"
            @change="doSearch"
          />
          <a-tooltip :title="`共发布 ${total} ${contentType === 'picture' ? '张图片' : '篇帖子'}`">
            <a-progress
              type="circle"
              :size="30"
              :percent="100"
              class="progress"
            />
          </a-tooltip>
        </div>
      </div>
    </template>

    <!-- PC端搜索表单 -->
    <template v-else>
      <div class="search-and-button-container">
        <a-form layout="inline" :model="searchParams" @finish="doSearch">
          <!-- PC端搜索选项 -->
          <a-form-item label="关键词">
            <a-input
              v-model:value="searchParams.searchText"
              :placeholder="contentType === 'picture' ? '从名称和简介搜索' : '从标题和内容搜索'"
              allow-clear
              class="compact-input"
            />
          </a-form-item>
          <a-form-item label="类型">
            <a-input
              v-model:value="searchParams.category"
              placeholder="请输入类型"
              allow-clear
              class="compact-input"
            />
          </a-form-item>
          <a-form-item label="标签">
            <a-input
              v-model:value="searchParams.tags"
              placeholder="请输入标签"
              allow-clear
              class="compact-input"
            />
          </a-form-item>
          <!-- PC端审核状态选择 -->
          <a-form-item v-if="device === DEVICE_TYPE_ENUM.PC" name="reviewStatus" label="审核状态">
            <a-select
              v-model:value="searchParams.reviewStatus"
              class="compact-select"
              placeholder="请选择审核状态"
              :options="contentType === 'picture' ? PIC_REVIEW_STATUS_OPTIONS : POST_STATUS_OPTIONS"
              allow-clear
            />
          </a-form-item>
          <!-- PC端搜索按钮 -->
          <a-form-item v-if="device === DEVICE_TYPE_ENUM.PC">
            <a-button type="primary" html-type="submit" class="action-button search-button">
              <SearchOutlined />
              搜索
            </a-button>
          </a-form-item>
        </a-form>
      </div>
    </template>

    <!-- 内容列表 -->
    <div v-if="device === DEVICE_TYPE_ENUM.PC">
      <template v-if="contentType === 'picture'">
        <PictureList
          :dataList="dataList"
          :loading="loading"
          :showOp="true"
          :onReload="fetchData"
          :isMyPosts="true"
        />
      </template>
      <template v-else>
        <PostList
          :dataList="dataList"
          :loading="loading"
          :showStatus="true"
        />
      </template>
      <!-- PC端分页 -->
      <div class="pagination-wrapper">
        <a-pagination
          v-model:current="searchParams.current"
          v-model:pageSize="searchParams.pageSize"
          :page-size-options="['6', '12', '18', '24', '30']"
          :total="total"
          :show-total="(total) => `共 ${total} 条`"
          @change="onPageChange"
          show-size-changer
          show-quick-jumper
          class="custom-pagination"
        >
          <template #buildOptionText="props">
            <span>{{ props.value }}{{ contentType === 'picture' ? '张' : '篇' }}/页</span>
          </template>
        </a-pagination>
      </div>
    </div>
    <div v-else>
      <van-pull-refresh
        v-model="loading"
        class="pull-refresh-container"
        @refresh="onRefresh"
        :distance="80"
        :head-height="60"
      >
        <template v-if="contentType === 'picture'">
          <MobilePictureList
            :dataList="dataList"
            :loading="loading"
            :showOp="true"
            :onReload="fetchData"
            :isMyPosts="true"
          />
        </template>
        <template v-else>
          <PostList
            :dataList="dataList"
            :loading="loading"
            :showStatus="true"
          />
        </template>
      </van-pull-refresh>
      <!-- 移动端分页 -->
      <div class="mobile-pagination">
        <div class="page-info">
          <span>第 {{ searchParams.current }} 页</span>
          <span class="separator">/</span>
          <span>共 {{ Math.ceil(total / searchParams.pageSize) }} 页</span>
        </div>
        <div class="page-actions">
          <a-button
            class="page-button prev"
            :disabled="searchParams.current === 1"
            @click="() => onPageChange(searchParams.current - 1, searchParams.pageSize)"
          >
            上一页
          </a-button>
          <a-button
            class="page-button next"
            :disabled="searchParams.current >= Math.ceil(total / searchParams.pageSize)"
            @click="() => onPageChange(searchParams.current + 1, searchParams.pageSize)"
          >
            下一页
          </a-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { listPictureVoByPageUsingPost } from '@/api/pictureController'
import { listMyPostsUsingPost } from '@/api/postController'
import PictureList from '@/components/PictureList.vue'
import PostList from '@/components/PostList.vue'
import MobilePictureList from '@/components/MobilePictureList.vue'
import { getDeviceType } from '@/utils/device'
import { DEVICE_TYPE_ENUM } from '@/constants/device'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { SearchOutlined, PictureOutlined, FileTextOutlined } from '@ant-design/icons-vue'
import { PIC_REVIEW_STATUS_OPTIONS } from '@/constants/picture'
import { POST_STATUS_OPTIONS } from '@/constants/post'

const loginUserStore = useLoginUserStore()
const device = ref(DEVICE_TYPE_ENUM.PC)
const contentType = ref('picture') // 'picture' | 'post'

// 数据列表
const dataList = ref<any[]>([])
const total = ref<number>(0)
const loading = ref<boolean>(false)

// 搜索参数
const searchParams = reactive({
  current: 1,
  pageSize: 12,
  sortField: 'createTime',
  sortOrder: 'descend',
  userId: loginUserStore.loginUser?.id,
})

// 获取数据
const fetchData = async () => {
  loading.value = true
  try {
    if (contentType.value === 'picture') {
      const pictureParams = { ...searchParams }
      const res = await listPictureVoByPageUsingPost(pictureParams)
      if (res.data.code === 0 && res.data.data) {
        dataList.value = res.data.data.records ?? []
        total.value = res.data.data.total ?? 0
      } else {
        message.error('获取数据失败，' + res.data.message)
      }
    } else {
      const postParams = { ...searchParams }
      const res = await listMyPostsUsingPost(postParams)
      if (res.data.code === 0 && res.data.data) {
        dataList.value = res.data.data.records ?? []
        total.value = res.data.data.total ?? 0
      } else {
        message.error('获取数据失败，' + res.data.message)
      }
    }
  } catch (e: any) {
    message.error('获取数据失败，' + e.message)
  }
  loading.value = false
}

// 分页变化
const onPageChange = (page: number, pageSize: number) => {
  searchParams.current = page
  searchParams.pageSize = pageSize
  fetchData()
}

// 搜索
const onSearch = (params: API.PictureQueryRequest) => {
  searchParams.current = 1
  Object.assign(searchParams, params)
  fetchData()
}

// 下拉刷新
const isRefreshing = ref(false)
const onRefresh = async () => {
  if (isRefreshing.value) return
  isRefreshing.value = true
  try {
    // 重置所有搜索参数
    Object.assign(searchParams, {
      current: 1,
      pageSize: 12,
      sortField: 'createTime',
      sortOrder: 'descend',
      userId: loginUserStore.loginUser?.id,
      searchText: undefined,
      category: undefined,
      tags: undefined,
      reviewStatus: undefined
    })
    await fetchData()
    // message.success('刷新成功')
  } catch (e: any) {
    message.error('刷新失败：' + e.message)
  } finally {
    isRefreshing.value = false
  }
}

// 监听内容类型变化
watch(contentType, () => {
  searchParams.current = 1
  searchParams.pageSize = 12
  searchParams.searchText = ''
  searchParams.reviewStatus = undefined
  searchParams.category = undefined
  searchParams.tags = undefined
  fetchData()
})

// 页面加载时获取设备类型和数据
onMounted(async () => {
  device.value = await getDeviceType()
  fetchData()
})

// 添加搜索方法
const doSearch = () => {
  searchParams.current = 1
  fetchData()
}
</script>

<style scoped>
#MyPostsPage {
  margin-bottom: 16px;
  margin-left: -20px !important;
  margin-right: -20px !important;
  margin-top: -20px!important;
}

/* 搜索区域样式 */
.search-and-button-container {
  margin-bottom: 16px;
  padding: 16px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

:deep(.ant-form) {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: center;

  .ant-form-item {
    margin: 0;
  }

  .ant-form-item-label {
    padding-right: 6px;
    font-size: 13px;
    color: #666;
  }
}

/* 紧凑型输入框样式 */
.compact-input {
  width: 140px !important;
  height: 32px;
  border-radius: 6px;
}

/* 紧凑型下拉框样式 */
.compact-select {
  width: 120px !important;
}

/* 搜索按钮样式 */
.search-button {
  background: linear-gradient(135deg, #ff8e53 0%, #ff6b6b 100%);
  border: none;
  box-shadow: 0 2px 8px rgba(255, 107, 107, 0.2);
  transition: all 0.3s ease;
}

.search-button:hover {
  background: linear-gradient(135deg, #ffa06d 0%, #ff8585 100%);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(255, 107, 107, 0.3);
}

.search-button:active {
  background: linear-gradient(135deg, #ff7a39 0%, #ff5151 100%);
  transform: translateY(0);
}

/* 响应式调整 */
@media screen and (max-width: 768px) {
  .search-and-button-container {
    padding: 12px;
    border-radius: 0;
    margin: 0 -12px 16px;
  }

  :deep(.ant-form) {
    gap: 8px;
  }

  .compact-input,
  .compact-select {
    width: 110px !important;
  }
}

/* 移动端分页器样式 */
.mobile-pagination {
  margin-top: 20px;
  padding: 16px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.page-info {
  text-align: center;
  margin-bottom: 12px;
  color: #666;
  font-size: 14px;
}

.separator {
  margin: 0 8px;
  color: #999;
}

.page-actions {
  display: flex;
  gap: 12px;
}

.page-button {
  flex: 1;
  height: 36px;
  border-radius: 8px;
  font-size: 14px;
  border: none;
  transition: all 0.3s ease;
}

.page-button.prev {
  background: linear-gradient(135deg, #ff8e53 0%, #ff6b6b 100%);
  color: white;
}

.page-button.next {
  background: linear-gradient(135deg, #ff8e53 0%, #ff6b6b 100%);
  color: white;
}

.page-button:not(:disabled):hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(255, 107, 107, 0.3);
}

.page-button:disabled {
  background: #f5f5f5;
  color: #999;
}

/* 响应式调整 */
@media screen and (max-width: 768px) {
  #MyPostsPage {
    padding: 0;
    margin: 0 auto;
  }

  .search-form {
    gap: 8px;
  }

  .custom-input {
    min-width: 140px;
  }

  .mobile-pagination {
    margin: 16px 0 0;
    border-radius: 0;
  }
}

/* 移动端搜索栏样式 */
.mobile-search-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
  padding: 16px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

/* 标题区域样式 */
.header-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #f1f5f9;
  background-color: #fff6f3;
  margin: -16px -16px 12px -16px;
  padding: 16px;
  height: 20px;
  border-radius: 8px 8px 0 0;
}

.title {
  font-size: 18px;
  font-weight: 500;
  color: #345750;
}

.progress {
  margin-right: 4px;
}

/* 搜索区域样式 */
.search-section {
  display: flex;
  gap: 8px;
}

.mobile-search-input {
  flex: 2;

  :deep(.ant-input) {
    border-radius: 6px;
    background: #f8fafc;
    border: 1px solid #e2e8f0;
    height: 36px;
    font-size: 14px;
    padding: 0 12px;

    &:hover {
      border-color: #ff8e53;
      background: #fff6f3;
    }

    &:focus {
      border-color: #ff8e53;
      box-shadow: 0 0 0 2px rgba(255, 142, 83, 0.1);
      background: white;
    }
  }

  :deep(.ant-input-suffix) {
    cursor: pointer;
  }
}

.search-icon {
  color: #94a3b8;
  font-size: 16px;
  padding: 4px;
  transition: all 0.3s;

  &:hover {
    color: #ff8e53;
  }
}

.mobile-status-select {
  flex: 1;
  min-width: 100px;

  :deep(.ant-select-selector) {
    height: 36px !important;
    line-height: 36px !important;
    border-radius: 6px !important;
    background: #f8fafc !important;
    border: 1px solid #e2e8f0 !important;

    .ant-select-selection-item {
      line-height: 34px !important;
      font-size: 14px;
    }
  }

  :deep(.ant-select-selection-placeholder) {
    line-height: 34px !important;
    font-size: 14px;
  }

  &:hover {
    :deep(.ant-select-selector) {
      border-color: #ff8e53 !important;
      background: #fff6f3 !important;
    }
  }

  &:focus {
    :deep(.ant-select-selector) {
      border-color: #ff8e53 !important;
      box-shadow: 0 0 0 2px rgba(255, 142, 83, 0.1) !important;
      background: white !important;
    }
  }
}

/* 响应式调整 */
@media screen and (max-width: 768px) {
  .mobile-search-container {
    margin: 0 0 16px;
    border-radius: 0;
  }

  #MyPostsPage {
    padding: 0;
    margin: 0 auto;
  }

  .header-section {
    border-radius: 0;
  }

  /* 确保下拉刷新和图片列表容器宽度一致 */
  .pull-refresh-container {
    margin: 0;
    background: #f8fafc;
  }
}

/* 分页器样式美化 */
.pagination-wrapper {
  margin-top: 8px;
  padding: 16px 24px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  display: flex;
  justify-content: flex-end;
}

:deep(.custom-pagination) {
  .ant-pagination-total-text {
    color: #64748b;
    margin-right: 12px;
  }

  .ant-pagination-prev,
  .ant-pagination-next,
  .ant-pagination-item,
  .ant-pagination-jump-prev,
  .ant-pagination-jump-next {
    border-radius: 8px;
    transition: all 0.3s ease;
    margin-right: 8px;

    &:hover {
      border-color: #ff8e53;
      a {
        color: #ff8e53;
      }
    }
  }

  .ant-pagination-item-active {
    background: linear-gradient(135deg, #ff8e53 0%, #ff6b6b 100%);
    border: none;

    a {
      color: white !important;
    }

    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba(255, 107, 107, 0.2);
    }
  }

  .ant-pagination-options {
    .ant-select {
      .ant-select-selector {
        border-radius: 8px !important;
        transition: all 0.3s ease;
        height: 32px !important;
        padding: 0 11px !important;

        .ant-select-selection-item {
          line-height: 30px !important;
          color: #64748b;
        }

        &:hover {
          border-color: #ff8e53 !important;
          background: #fff6f3;
        }
      }

      &.ant-select-focused .ant-select-selector {
        border-color: #ff8e53 !important;
        box-shadow: 0 0 0 2px rgba(255, 142, 83, 0.1) !important;
      }
    }

    .ant-pagination-options-quick-jumper {
      color: #64748b;
      margin-left: 16px;

      input {
        border-radius: 8px;
        transition: all 0.3s ease;
        height: 32px;
        width: 50px;
        text-align: center;
        margin: 0 8px;

        &:hover {
          border-color: #ff8e53;
          background: #fff6f3;
        }

        &:focus {
          border-color: #ff8e53;
          box-shadow: 0 0 0 2px rgba(255, 142, 83, 0.1);
        }
      }
    }
  }
}

/* 下拉选择器样式 */
:deep(.ant-select-dropdown) {
  border-radius: 8px;
  overflow: hidden;
  padding: 4px;

  .ant-select-item {
    transition: all 0.3s ease;
    padding: 8px 12px;
    border-radius: 6px;
    margin: 2px 0;

    &:hover {
      background: #fff6f3;
      color: #ff8e53;
    }

    &-option-selected {
      background: #fff6f3 !important;
      color: #ff8e53 !important;
      font-weight: 500;
    }

    &-option-active {
      background: #fff6f3 !important;
      color: #ff8e53 !important;
    }
  }
}

/* 响应式调整 */
@media screen and (max-width: 768px) {
  .pagination-wrapper {
    margin: 8px -12px 0;
    padding: 12px;
    border-radius: 0;

    :deep(.ant-pagination-options-quick-jumper) {
      display: none;
    }
  }
}

/* 下拉刷新容器样式 */
.pull-refresh-container {
  margin: 0;
}

/* 切换按钮样式 */
.content-switch {
  padding: 8px 16px;
  background: white;
  border-bottom: 1px solid #f1f5f9;
}

.switch-tabs {
  display: flex;
  background: #f8fafc;
  padding: 4px;
  border-radius: 20px;
  height: 40px;
  max-width: 400px;
  margin: 0 auto;
}

.switch-tab {
  flex: 1;
  height: 32px;
  line-height: 32px;
  font-size: 14px;
  color: #64748b;
  border-radius: 16px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  user-select: none;

  &:hover:not(.active) {
    color: #ff8e53;
    background: rgba(255, 142, 83, 0.05);
  }

  &.active {
    color: #ff8e53;
    font-weight: 500;
    background: white;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  }

  /* 添加点击效果 */
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    border-radius: 16px;
    background: currentColor;
    opacity: 0;
    transition: opacity 0.3s;
  }

  &:active::before {
    opacity: 0.1;
  }

  /* 添加渐变背景效果 */
  &.active {
    background: linear-gradient(135deg, #fff6f3 0%, #fff 100%);
    box-shadow:
      0 2px 8px rgba(255, 142, 83, 0.1),
      0 1px 4px rgba(255, 142, 83, 0.05);
  }

  :deep(.anticon) {
    font-size: 16px;
  }
}
</style>
