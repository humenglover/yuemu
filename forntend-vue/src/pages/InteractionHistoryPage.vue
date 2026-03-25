<template>
  <div class="interaction-history" :class="{ 'pc-interaction-history': !isMobile }">
    <!-- 背景动画 -->
    <div class="background-animation" v-if="!isMobile">
      <div class="gradient-bg" style="background: linear-gradient(45deg, rgba(255, 142, 83, 0.03), rgba(255, 170, 128, 0.05))"></div>
      <div class="wave wave1"></div>
      <div class="wave wave2"></div>
    </div>

    <!-- 装饰元素 -->
    <div class="decorative-elements">
      <div class="floating-circle circle-1" style="background: rgba(255, 142, 83, 0.1)"></div>
      <div class="floating-circle circle-2" style="background: rgba(255, 170, 128, 0.08)"></div>
      <div class="floating-circle circle-3" style="background: rgba(255, 198, 173, 0.06)"></div>
    </div>

    <!-- 页面标题 -->
    <div class="history-header">
      <div class="header-left">
        <div class="title-wrapper">
          <h2>互动历史</h2>
          <div class="divider"></div>
          <div class="total-count">
            共 <span class="highlight">{{ pagination.total || 0 }}</span> 条记录
          </div>
        </div>
      </div>
      <div class="header-right">
        <a-button class="back-btn" @click="router.back()">
          <template #icon><ArrowLeftOutlined /></template>
          返回
        </a-button>
      </div>
    </div>

    <!-- 类型选择 -->
    <div class="type-selector">
      <div class="type-filter">
        <a-radio-group v-model:value="activeType" size="middle" button-style="solid">
          <a-radio-button value="received">收到的互动</a-radio-button>
          <a-radio-button value="sent">发出的互动</a-radio-button>
        </a-radio-group>
      </div>

      <div class="stats-cards">
        <div
          class="stat-card"
          :class="{ active: interactionType === 'comments' }"
          @click="handleTypeChange('comments')"
        >
          <div class="stat-icon comments">
            <CommentOutlined />
          </div>
          <div class="stat-info">
            <div class="stat-label">评论历史</div>
          </div>
        </div>

        <div
          class="stat-card"
          :class="{ active: interactionType === 'likes' }"
          @click="handleTypeChange('likes')"
        >
          <div class="stat-icon likes">
            <LikeOutlined />
          </div>
          <div class="stat-info">
            <div class="stat-label">点赞历史</div>
          </div>
        </div>

        <div
          class="stat-card"
          :class="{ active: interactionType === 'shares' }"
          @click="handleTypeChange('shares')"
        >
          <div class="stat-icon shares">
            <ShareAltOutlined />
          </div>
          <div class="stat-info">
            <div class="stat-label">分享历史</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 历史记录列表 -->
    <div class="history-list">
      <div class="list-header">
        <div class="header-content">
          <div class="title-section">
            <span class="list-title">{{ getTypeName() }}记录</span>
            <span class="list-count">共 {{ pagination.total || 0 }} 条</span>
          </div>
          <div class="filter-tags">
            <a-tag
              class="filter-tag picture-tag"
              :class="{ active: targetFilter === 'picture' }"
              @click="handleTargetFilter('picture')"
            >
              <PictureOutlined />图片
            </a-tag>
            <a-tag
              class="filter-tag post-tag"
              :class="{ active: targetFilter === 'post' }"
              @click="handleTargetFilter('post')"
            >
              <FileTextOutlined />帖子
            </a-tag>
          </div>
        </div>
      </div>
      <div class="list-content">
        <div v-if="loading" class="loading-state">
          <a-spin size="large" />
        </div>
        <template v-else>
          <div v-if="historyData.length === 0" class="empty-state">
            <a-empty :description="'暂无' + (activeType === 'received' ? '收到' : '发出') + '的' + getTypeName()" />
          </div>
          <template v-else>
            <UnreadCommentList
              v-if="interactionType === 'comments'"
              :comments="historyData"
              :is-received="activeType === 'received'"
            />
            <UnreadLikeList
              v-else-if="interactionType === 'likes'"
              :likes="historyData"
              :is-received="activeType === 'received'"
            />
            <UnreadShareList
              v-else-if="interactionType === 'shares'"
              :shares="historyData"
              :is-received="activeType === 'received'"
            />
          </template>
        </template>
        <!-- 分页器 -->
        <template v-if="historyData.length > 0">
          <!-- PC端分页器 -->
          <div class="pagination-wrapper" v-if="!isMobile">
            <a-pagination
              v-model:current="pagination.current"
              v-model:pageSize="pagination.pageSize"
              :total="pagination.total"
              :show-total="pagination.showTotal"
              :show-size-changer="pagination.showSizeChanger"
              :show-quick-jumper="pagination.showQuickJumper"
              :page-size-options="pagination.pageSizeOptions"
              @change="(page, pageSize) => handlePageChange({ current: page, pageSize })"
            />
          </div>
          <!-- 移动端分页器 -->
          <div class="mobile-pagination" v-else>
            <div class="pagination-info">
              <span>共 {{ pagination.total }} 条</span>
              <a-select
                v-model:value="pagination.pageSize"
                :options="pagination.pageSizeOptions.map(size => ({ value: Number(size), label: `${size}条/页` }))"
                @change="handlePageSizeSelect"
                class="page-size-select"
                size="small"
              />
            </div>
            <div class="pagination-wrapper">
              <a-pagination
                v-model:current="pagination.current"
                :total="pagination.total"
                :page-size="pagination.pageSize"
                :show-less-items="true"
                @change="(page) => handlePageChange({ current: page, pageSize: pagination.pageSize })"
              />
              <div class="jump-page">
                <span>跳至</span>
                <a-input-number
                  v-model:value="jumpPage"
                  :min="1"
                  :max="Math.ceil(pagination.total / pagination.pageSize)"
                  @pressEnter="handleJumpPage"
                  size="small"
                />
                <span>页</span>
              </div>
            </div>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Grid } from 'ant-design-vue'
import { message } from 'ant-design-vue'
import { ArrowLeftOutlined, CommentOutlined, LikeOutlined, ShareAltOutlined, DownOutlined, FileTextOutlined, PictureOutlined } from '@ant-design/icons-vue'
import UnreadCommentList from '@/components/UnreadCommentList.vue'
import UnreadLikeList from '@/components/UnreadLikeList.vue'
import UnreadShareList from '@/components/UnreadShareList.vue'
import {
  getCommentedHistoryUsingPost,
  getMyCommentHistoryUsingPost
} from '@/api/commentsController'
import {
  getLikeHistoryUsingPost,
  getMyLikeHistoryUsingPost
} from '@/api/likeRecordController'
import {
  getShareHistoryUsingPost,
  getMyShareHistoryUsingPost
} from '@/api/shareRecordController'
import type { TablePaginationConfig } from 'ant-design-vue'

const router = useRouter()
const { useBreakpoint } = Grid
const screens = useBreakpoint()
const isMobile = computed(() => !screens.md)

const activeType = ref('received')
const interactionType = ref('comments')
const historyData = ref([])
const loading = ref(false)
const pagination = ref({
  current: 1,
  pageSize: 6,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`,
  pageSizeOptions: ['6', '10', '20', '50']
})

// 移动端分页相关
const jumpPage = ref<number>()
const showPageSizeSheet = ref(false)

// 目标类型筛选
const targetFilter = ref('')

// 转换数据结构
const transformData = (data: any[], type: string, isReceived: boolean) => {
  if (!data) return []
  const defaultUser = {
    id: '',
    userAccount: '',
    userName: '',
    userAvatar: '',
    userProfile: '',
    userRole: '',
    createTime: ''
  }
  return data.map(item => {
    if (type === 'comments') {
      // 处理评论历史
      return {
        id: item.commentId,
        content: item.content,
        createTime: item.createTime,
        commentUser: item.commentUser || defaultUser,
        targetType: item.targetType,
        picture: item.picture || {},
        post: item.post || {}
      }
    } else if (type === 'likes') {
      // 处理点赞历史
      return {
        id: item.id,
        lastLikeTime: item.lastLikeTime,
        user: item.user || defaultUser,
        targetType: item.targetType,
        target: item.target || {}
      }
    } else {
      // 处理分享历史
      return {
        id: item.id,
        shareTime: item.shareTime || item.createTime,
        user: item.user || defaultUser,
        targetType: item.targetType,
        target: item.target || {}
      }
    }
  }).filter(item =>
    // 过滤掉无效数据
    item.id &&
    (type === 'comments' ? item.content : true) &&
    (type === 'comments' ? (item.picture || item.post) : item.target)
  )
}

// 获取历史数据
const fetchHistoryData = async () => {
  console.log('开始获取历史数据')
  loading.value = true
  try {
    let res
    const params = {
      current: pagination.value.current,
      pageSize: pagination.value.pageSize,
      sortField: 'createTime',
      sortOrder: 'descend',
      targetType: targetFilter.value === 'picture' ? 1 : targetFilter.value === 'post' ? 2 : undefined
    }
    console.log('请求参数:', params)
    const isReceived = activeType.value === 'received'
    console.log('是否为收到的互动:', isReceived)

    // 添加错误处理和数据验证
    try {
      if (isReceived) {
        if (interactionType.value === 'comments') {
          console.log('获取收到的评论')
          res = await getCommentedHistoryUsingPost(params)
        } else if (interactionType.value === 'likes') {
          console.log('获取收到的点赞')
          res = await getLikeHistoryUsingPost(params)
        } else {
          console.log('获取收到的分享')
          res = await getShareHistoryUsingPost(params)
        }
      } else {
        if (interactionType.value === 'comments') {
          console.log('获取发出的评论')
          res = await getMyCommentHistoryUsingPost(params)
        } else if (interactionType.value === 'likes') {
          console.log('获取发出的点赞')
          res = await getMyLikeHistoryUsingPost(params)
        } else {
          console.log('获取发出的分享')
          res = await getMyShareHistoryUsingPost(params)
        }
      }

      console.log('API返回数据:', res)

      if (!res || !res.data) {
        throw new Error('API返回数据格式错误')
      }

      if (res.data?.code === 0) {
        const transformedData = transformData(
          res.data.data?.records || [],
          interactionType.value,
          isReceived
        )
        console.log('转换后的数据:', transformedData)
        historyData.value = transformedData
        pagination.value.total = parseInt(res.data.data?.total || '0')
        console.log('更新后的分页信息:', pagination.value)
      }
    } catch (error) {
      console.error('API调用或数据处理错误:', error)
      historyData.value = []
      pagination.value.total = 0
    }
  } catch (error) {
    console.error('获取历史记录失败，详细错误:', error)
    message.error('获取历史记录失败')
  } finally {
    loading.value = false
    console.log('加载完成，当前数据:', historyData.value)
  }
}

// 监听类型变化
watch([activeType, interactionType], () => {
  console.log('筛选条件变化:', { activeType: activeType.value, interactionType: interactionType.value, targetFilter: targetFilter.value })
  pagination.value.current = 1
  fetchHistoryData()
})

onMounted(() => {
  console.log('组件挂载，开始获取数据')
  fetchHistoryData()
})

// 获取类型名称
const getTypeName = () => {
  switch (interactionType.value) {
    case 'comments':
      return '评论';
    case 'likes':
      return '点赞';
    case 'shares':
      return '分享';
    default:
      return '互动';
  }
}

// 处理页码改变
const handlePageChange = (pag: TablePaginationConfig) => {
  pagination.value.current = pag.current || 1
  pagination.value.pageSize = pag.pageSize || 10
  fetchHistoryData()
}

// 处理每页条数改变
const handlePageSizeChange = (current: number, size: number) => {
  pagination.value.current = 1
  pagination.value.pageSize = size
  showPageSizeSheet.value = false
  fetchHistoryData()
}

// 处理类型切换
const handleTypeChange = (type: string) => {
  interactionType.value = type
  pagination.value.current = 1
  targetFilter.value = ''
  fetchHistoryData()
}

// 处理页码跳转
const handleJumpPage = () => {
  if (!jumpPage.value) return

  const maxPage = Math.ceil(pagination.value.total / pagination.value.pageSize)
  if (jumpPage.value < 1 || jumpPage.value > maxPage) {
    message.warning(`请输入1-${maxPage}之间的页码`)
    return
  }

  pagination.value.current = jumpPage.value
  fetchHistoryData()
  jumpPage.value = undefined
}

// 处理每页条数选择
const handlePageSizeSelect = (size: number) => {
  pagination.value.current = 1
  pagination.value.pageSize = size
  showPageSizeSheet.value = false
  fetchHistoryData()
}

// 处理目标类型筛选
const handleTargetFilter = (type: string) => {
  targetFilter.value = targetFilter.value === type ? '' : type
  pagination.value.current = 1
  fetchHistoryData()
}
</script>

<style scoped>
.interaction-history {
  background: var(--header-background);
  color: var(--text-primary);
  margin: -24px;
  min-height: 100vh;
  position: relative;
  overflow: hidden;
  padding-bottom: 80px; /* 为底部信息栏留出空间 */
}

.pc-interaction-history {
  max-width: 1200px;
  margin: 0 auto;
}

.history-header {
  background: var(--header-background);
  color: var(--text-primary);
  padding: 20px 24px;
  border-radius: 12px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  margin-bottom: 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title-wrapper {
  display: flex;
  align-items: center;
  gap: 16px;
}

.divider {
  width: 1px;
  height: 20px;
  background: #e8e8e8;
  margin: 0 4px;
}

.header-left {
  display: flex;
  align-items: center;
}

.header-left h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: #1a1a1a;
}

.total-count {
  color: #666;
  font-size: 14px;
}

.highlight {
  color: #ff8e53;
  font-weight: 500;
  margin: 0 4px;
}

.back-btn {
  height: 32px;
  border-radius: 16px;
  padding: 0 16px;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: all 0.3s ease;
}

.back-btn:hover {
  color: #ff8e53;
  border-color: #ff8e53;
}

.type-selector {
  background: var(--header-background);
  color: var(--text-primary);
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  margin-bottom: 24px;
}

.type-filter {
  margin-bottom: 20px;
  display: flex;
  justify-content: center;
}

.type-filter :deep(.ant-radio-group) {
  display: flex;
  width: 100%;
  max-width: 400px;
}

.type-filter :deep(.ant-radio-button-wrapper) {
  flex: 1;
  text-align: center;
  height: 40px;
  line-height: 38px;
  font-size: 15px;
  transition: all 0.3s ease;
}

.type-filter :deep(.ant-radio-button-wrapper-checked) {
  background: #ff8e53;
  border-color: #ff8e53;
  color: white;
}

.type-filter :deep(.ant-radio-button-wrapper-checked::before) {
  background-color: #ff8e53;
}

.type-filter :deep(.ant-radio-button-wrapper:hover) {
  color: #ff8e53;
}

.type-filter :deep(.ant-radio-button-wrapper-checked:hover) {
  color: white;
  border-color: #ff8e53;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.stat-card {
  background: var(--header-background);
  color: var(--text-primary);
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 2px solid transparent;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.stat-card.active {
  border-color: #ff8e53;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.stat-icon.comments {
  background: #e6f7ff;
  color: #1890ff;
}

.stat-icon.likes {
  background: #fff2e8;
  color: #ff4d4f;
}

.stat-icon.shares {
  background: #f6ffed;
  color: #52c41a;
}

.stat-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.stat-label {
  font-size: 16px;
  font-weight: 500;
  color: #1a1a1a;
  margin-bottom: 0;
}

.stat-tags {
  display: flex;
  gap: 6px;
}

.filter-tag {
  cursor: pointer;
  padding: 2px 8px;
  font-size: 12px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  gap: 4px;
  transition: all 0.3s ease;
  margin: 0;
  border: 1px solid #e8e8e8;
  background: var(--header-background);
  color: var(--text-primary);
}

.filter-tag:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
}

.filter-tag.active {
  color: white;
  border: none;
}

.picture-tag.active {
  background: linear-gradient(135deg, #36cfc9 0%, #13c2c2 100%);
}

.post-tag.active {
  background: linear-gradient(135deg, #85a5ff 0%, #597ef7 100%);
}

.filter-tag .anticon {
  font-size: 12px;
}

.list-header {
  padding: 20px 24px;
  border-bottom: 1px solid #f0f0f0;
  background: var(--header-background);
  color: var(--text-primary);;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.list-title {
  font-size: 16px;
  font-weight: 500;
  color: #1a1a1a;
}

.list-count {
  font-size: 14px;
  color: #666;
}

.filter-tags {
  display: flex;
  gap: 8px;
}

.list-content {
  padding: 0;
  min-height: 200px; /* 确保内容区域有最小高度 */
}

.pagination-wrapper {
  margin-top: 0;
  padding: 16px 24px;
  background: var(--header-background);
  color: var(--text-primary);;
  border-top: 1px solid #f0f0f0;
  display: flex;
  justify-content: flex-end;
  position: sticky;
  bottom: 0;
  z-index: 1;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.05);
}

/* 分页器样式优化 */
.pagination-wrapper :deep(.ant-pagination) {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.pagination-wrapper :deep(.ant-pagination-item),
.pagination-wrapper :deep(.ant-pagination-prev),
.pagination-wrapper :deep(.ant-pagination-next) {
  border-radius: 8px;
  margin: 0;
  transition: all 0.3s ease;

  &:hover {
    border-color: #ff8e53;
    color: #ff8e53;
  }
}

.pagination-wrapper :deep(.ant-pagination-item-active) {
  background: linear-gradient(135deg, #ffaa80 0%, #ff8e53 100%);
  border: none;
  box-shadow: 0 2px 8px rgba(255, 142, 83, 0.2);

  a {
    color: white !important;
  }

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(255, 142, 83, 0.3);
  }
}

.pagination-wrapper :deep(.ant-select-selector) {
  border-radius: 8px !important;
  transition: all 0.3s ease;

  &:hover {
    border-color: #ff8e53 !important;
  }
}

.pagination-wrapper :deep(.ant-select-focused .ant-select-selector) {
  border-color: #ff8e53 !important;
  box-shadow: 0 0 0 2px rgba(255, 142, 83, 0.1) !important;
}

.pagination-wrapper :deep(.ant-pagination-options) {
  margin-left: 8px;
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .interaction-history {
    margin: 0;
    padding: 0;
    background: var(--header-background);
  color: var(--text-primary);;
    padding-bottom: 0; /* 移动端不需要底部padding */
  }

  .history-header {
    flex-direction: row;
    align-items: center;
    gap: 12px;
    margin-bottom: 16px;
    padding: 16px;
    background: var(--header-background);
  color: var(--text-primary);
    border-bottom: 1px solid #f0f0f0;
    border-radius: 0;
    box-shadow: none;
  }

  .header-left {
    flex-direction: row;
    gap: 12px;
    align-items: center;
  }

  .title-wrapper {
    gap: 12px;
  }

  .header-left h2 {
    margin: 0;
    font-size: 18px;
  }

  .total-count {
    font-size: 13px;
  }

  .type-selector {
    padding: 16px;
    margin-bottom: 12px;
    border-radius: 0;
    box-shadow: none;
  }

  .stats-cards {
    grid-template-columns: repeat(3, 1fr);
    gap: 8px;
  }

  .stat-card {
    padding: 12px;
    flex-direction: column;
    text-align: center;
    gap: 8px;
  }

  .stat-icon {
    width: 40px;
    height: 40px;
    font-size: 18px;
  }

  .stat-label {
    font-size: 12px;
  }

  .history-list {
    padding: 0;
    min-height: calc(100vh - 180px);
    border-radius: 0;
    box-shadow: none;
  }

  .list-header {
    padding: 12px 16px;
    background: var(--header-background);
  color: var(--text-primary);
    border-bottom: 1px solid #f0f0f0;
  }

  .header-content {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .list-title {
    font-size: 15px;
  }

  .list-count {
    font-size: 13px;
  }

  .filter-tags {
    width: 100%;
    justify-content: flex-start;
    gap: 8px;
  }

  .filter-tag {
    flex: 1;
    justify-content: center;
    padding: 4px 0;
  }

  .pagination-wrapper {
    padding: 12px 16px;
    overflow-x: auto;
    justify-content: center;
    background: var(--header-background);
  color: var(--text-primary);
    border-top: 1px solid #f0f0f0;
    white-space: nowrap;

    :deep(.ant-pagination) {
      display: flex;
      justify-content: center;
      align-items: center;
      gap: 4px;
    }

    :deep(.ant-pagination-item),
    :deep(.ant-pagination-prev),
    :deep(.ant-pagination-next) {
      min-width: 32px;
      height: 32px;
      line-height: 30px;
      margin: 0;
    }

    :deep(.ant-pagination-options) {
      margin-left: 4px;
    }

    :deep(.ant-select-selector) {
      height: 32px !important;
      line-height: 30px !important;
    }

    :deep(.ant-select-selection-item) {
      line-height: 30px !important;
    }
  }

  .back-btn {
    height: 28px;
    font-size: 12px;
    padding: 0 12px;
  }

  /* 移动端隐藏装饰元素和背景动画 */
  .background-animation,
  .decorative-elements {
    display: none;
  }

  .loading-state {
    padding: 32px 0;
    min-height: 200px;
  }

  .empty-state {
    padding: 32px 16px;
    min-height: 200px;
  }

  .stat-tags {
    justify-content: center;
    margin-top: 4px;
  }

  .filter-tag {
    padding: 1px 6px;
    font-size: 11px;
  }

  .filter-tag .anticon {
    font-size: 11px;
  }
}

/* 针对特小屏幕的额外优化 */
@media screen and (max-width: 360px) {
  .header-left h2 {
    font-size: 16px;
  }

  .total-count {
    font-size: 11px;
  }

  .type-filter :deep(.ant-radio-button-wrapper) {
    font-size: 12px;
    padding: 0 6px;
  }

  .stat-card {
    padding: 10px;
  }

  .stat-icon {
    width: 32px;
    height: 32px;
    font-size: 16px;
  }

  .stat-label {
    font-size: 13px;
  }

  .pagination-wrapper {
    padding: 8px 12px;

    :deep(.ant-pagination-item),
    :deep(.ant-pagination-prev),
    :deep(.ant-pagination-next) {
      min-width: 28px;
      height: 28px;
      line-height: 26px;
    }

    :deep(.ant-select-selector) {
      height: 28px !important;
      line-height: 26px !important;
    }

    :deep(.ant-select-selection-item) {
      line-height: 26px !important;
      font-size: 12px;
    }

    :deep(.ant-pagination-options-quick-jumper) {
      font-size: 12px;

      input {
        height: 28px;
        padding: 0 6px;
      }
    }
  }

  .filter-tag {
    padding: 0 4px;
    font-size: 10px;
  }

  .filter-tag .anticon {
    font-size: 10px;
  }
}

/* 背景动画 */
.background-animation {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
  pointer-events: none;
  overflow: hidden;
}

.gradient-bg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}

.wave {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 100px;
  opacity: 0.1;
  background: linear-gradient(to bottom, transparent, rgba(255, 142, 83, 0.2));
  border-radius: 50%;
}

.wave1 {
  animation: wave 20s linear infinite;
  z-index: 1;
  opacity: 0.3;
  animation-delay: 0s;
  bottom: 0;
}

.wave2 {
  animation: wave 15s linear infinite;
  z-index: 2;
  opacity: 0.2;
  animation-delay: -5s;
  bottom: 10px;
}

@keyframes wave {
  0% {
    transform: translateX(-50%) scaleY(1);
  }
  50% {
    transform: translateX(0%) scaleY(0.8);
  }
  100% {
    transform: translateX(50%) scaleY(1);
  }
}

/* 装饰元素 */
.decorative-elements {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
}

.floating-circle {
  position: absolute;
  border-radius: 50%;
  animation: float 6s ease-in-out infinite;
}

.circle-1 {
  width: 300px;
  height: 300px;
  top: 10%;
  left: 5%;
  animation-delay: 0s;
}

.circle-2 {
  width: 200px;
  height: 200px;
  top: 60%;
  right: 10%;
  animation-delay: -2s;
}

.circle-3 {
  width: 150px;
  height: 150px;
  top: 30%;
  right: 20%;
  animation-delay: -4s;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-20px); }
}

.history-list {
  background: var(--header-background);
  color: var(--text-primary);
  border-radius: 12px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  min-height: calc(100vh - 280px); /* 减少最小高度，避免内容过长 */
}

.loading-state {
  padding: 32px 0;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 300px; /* 确保有足够的高度来展示加载状态 */
}

.empty-state {
  padding: 32px 16px;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 300px;
}

/* 移动端分页样式 */
.mobile-pagination {
  margin-top: 16px;
  padding: 16px;
  background: var(--header-background);
  color: var(--text-primary);
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.page-size-select {
  width: 100px;
}

.pagination-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  color: #666;
  font-size: 13px;
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

.jump-page :deep(.ant-input-number) {
  width: 48px;

  .ant-input-number-input {
    height: 28px;
    padding: 0 4px;
    text-align: center;
  }
}

/* 移动端分页器样式优化 */
.mobile-pagination :deep(.ant-pagination) {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 4px;
}

.mobile-pagination :deep(.ant-pagination-item),
.mobile-pagination :deep(.ant-pagination-prev),
.mobile-pagination :deep(.ant-pagination-next) {
  min-width: 32px;
  height: 32px;
  line-height: 30px;
  margin: 0;
  border-radius: 16px;

  &.ant-pagination-item-active {
    background: linear-gradient(135deg, #ffaa80 0%, #ff8e53 100%);
    border: none;

    a {
      color: white !important;
    }
  }
}

/* 针对特小屏幕的额外优化 */
@media screen and (max-width: 360px) {
  .mobile-pagination {
    padding: 12px;
  }

  .page-size-select {
    width: 90px;
  }

  .pagination-info {
    font-size: 12px;
  }

  .mobile-pagination :deep(.ant-pagination-item),
  .mobile-pagination :deep(.ant-pagination-prev),
  .mobile-pagination :deep(.ant-pagination-next) {
    min-width: 28px;
    height: 28px;
    line-height: 26px;
  }

  .jump-page {
    font-size: 12px;

    :deep(.ant-input-number-input) {
      height: 24px;
    }
  }
}
</style>
