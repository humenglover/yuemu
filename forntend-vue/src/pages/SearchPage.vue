<template>
  <div class="search-page">
    <!-- 搜索头部 -->
    <div class="search-header">
      <div class="search-container">
        <div class="search-bar">
          <div class="xh-searchbar">
            <input
              v-model="searchText"
              type="text"
              class="xh-search-input"
              placeholder="搜索图片、帖子、空间、用户"
              @input="handleInput"
              @keyup.enter="doSearch()"
              ref="searchInput"
              autofocus
            />
            <div class="xh-search-btn" @click="doSearch()">
              <i class="fas fa-search search-btn-icon"></i>
              <span class="search-btn-text">搜索</span>
            </div>
          </div>
        </div>

        <!-- 搜索类型切换 -->
        <div class="search-type">
          <div class="type-buttons">
            <div
              class="type-button"
              :class="{ active: searchType === 'picture' }"
              @click="() => handleTypeChange('picture')"
            >
              <i class="fas fa-image type-icon"></i>
              <span>图片</span>
              <span class="count" v-if="pictureList.length > 0">{{ pictureList.length }}</span>
            </div>
            <div
              class="type-button"
              :class="{ active: searchType === 'post' }"
              @click="() => handleTypeChange('post')"
            >
              <i class="fas fa-file-alt type-icon"></i>
              <span>帖子</span>
              <span class="count" v-if="postList.length > 0">{{ postList.length }}</span>
            </div>
            <div
              class="type-button"
              :class="{ active: searchType === 'space' }"
              @click="() => handleTypeChange('space')"
            >
              <i class="fas fa-th-large type-icon"></i>
              <span>空间</span>
              <span class="count" v-if="spaceList.length > 0">{{ spaceList.length }}</span>
            </div>
            <div
              class="type-button"
              :class="{ active: searchType === 'user' }"
              @click="() => handleTypeChange('user')"
            >
              <i class="fas fa-user type-icon"></i>
              <span>用户</span>
              <span class="count" v-if="userList.length > 0">{{ userList.length }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 搜索建议区域 -->
    <div v-if="!searchText" class="search-suggestions">
      <div class="suggestions-container">
        <div class="xh-history" v-if="searchHistory.length > 0">
          <div class="xh-title-row">
            <div class="xh-title">历史搜索</div>
            <a-button type="link" @click="clearHistory">清空</a-button>
          </div>
          <div class="history-scroll">
            <div class="history-pill" v-for="item in searchHistory" :key="item" @click="searchByTag(item)">{{ item }}</div>
          </div>
        </div>

        <!-- 猜你想搜 -->
        <div class="guess-searches" v-if="guessSearches.length > 0">
          <div class="xh-title-row">
            <div class="xh-title">猜你想搜</div>
          </div>
          <div class="guess-scroll">
            <div class="guess-pill" v-for="item in guessSearches" :key="typeof item === 'string' ? item : item.keyword" @click="searchByTag(typeof item === 'string' ? item : item.keyword)">
              <span>{{ typeof item === 'string' ? item : item.keyword }}</span>
            </div>
          </div>
        </div>

        <div class="hot-searches">
          <div style="display: flex; justify-content: space-between; align-items: center; margin: 12px;">
            <span  class="xh-title">热门搜索</span>
            <a-button type="link" @click="showHotScoreExplanation" style="padding: 0; height: auto;">热度计算说明</a-button>
          </div>
          <div class="hot-grid">
            <div
              v-for="(item, index) in hotSearches"
              :key="typeof item === 'string' ? item : item.keyword"
              class="hot-item"
              @click="searchByTag(typeof item === 'string' ? item : item.keyword)"
            >
              <span class="hot-rank" :class="{ 'top3': index < 3 }">{{ index + 1 }}</span>
              <span class="hot-text">{{ typeof item === 'string' ? item : item.keyword }}</span>
              <span v-if="typeof item !== 'string'" class="hot-meta">热度: {{ item.score?.toFixed(1) }}</span>
              <i v-if="index < 3" class="fas fa-fire fire-icon"></i>
            </div>
          </div>
        </div>

      </div>
    </div>

    <!-- 搜索结果区域 -->
    <div v-else class="search-results">
      <div v-if="loading" class="loading-container">
        <i class="fas fa-spinner fa-spin loading-icon-large" style="font-size: 24px;"></i>
      </div>
      <template v-else>
        <!-- 图片搜索结果 -->
        <div v-if="searchType === 'picture'" class="picture-results">
          <div class="results-header">
            <h2 class="results-title">找到 {{ pictureList.length }} 张相关图片</h2>
          </div>

          <!-- PC端使用瀑布流组件 -->
          <WaterfallPictureList
            v-if="!isMobile && pictureList.length > 0"
            :dataList="pictureList"
            :loading="loading"
            :streamLayout="'waterfall'"
          />

          <!-- 移动端使用移动端瀑布流组件 -->
          <MobilePictureList
            v-else-if="isMobile && pictureList.length > 0"
            :dataList="pictureList"
            :loading="loading"
            :streamLayout="'waterfall'"
          />

          <div v-else class="xh-empty-wrapper">
            <div class="custom-empty-state">
              <img :src="emptyImage" alt="暂无结果" class="empty-illustration" />
              <p class="empty-text">暂无相关图片，换个词试试吧</p>
            </div>
          </div>
        </div>

        <!-- 帖子搜索结果 -->
        <div v-else-if="searchType === 'post'" class="post-results">
          <h2 class="results-title">找到 {{ postList.length }} 个相关帖子</h2>
          <div class="post-results-list">
            <PostList
              :dataList="postList"
              :loading="loading"
              :showStatus="false"
            />
          </div>
        </div>

        <!-- 空间搜索结果 -->
        <div v-else-if="searchType === 'space'" class="space-results">
          <h2 class="results-title">找到 {{ spaceList.length }} 个相关空间</h2>
          <div class="space-grid">
            <SpaceInfoCard
              v-for="space in spaceList"
              :key="space.id"
              :spaceInfo="space"
              @click="goToSpaceDetail(space)"
            />
          </div>
        </div>

        <!-- 用户搜索结果 -->
        <div v-else class="user-results">
          <h2 class="results-title">找到 {{ userList.length }} 位相关用户</h2>
          <div class="user-grid">
            <div v-for="user in userList" :key="user.id" class="user-card">
              <div class="user-header" @click="handleUserClick(user)">
                <a-avatar :src="user.userAvatar" :size="64" />
                <div class="user-info">
                  <h3 class="user-name">{{ user.userName }}</h3>
                  <p class="user-bio">{{ user.userProfile || '这个人很懒，什么都没写~' }}</p>
                </div>
              </div>
              <div class="user-stats">
                <div class="stat-item">
                  <i class="fas fa-image stat-icon"></i>
                  <span>{{ user.fansCount || 0 }} 位粉丝</span>
                </div>
                <div class="stat-item">
                  <i class="fas fa-users stat-icon"></i>
                  <span>{{ user.followCount || 0 }} 位关注者</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>

    <!-- 分页加载指示器 -->
    <div v-if="searchText" class="pagination-footer">
      <div v-if="loading && searchParams.current > 1" class="loading-more">
        <i class="fas fa-spinner fa-spin"></i>
        <span>正在加载更多...</span>
      </div>
      <div v-if="isEndOfData && (pictureList.length > 0 || postList.length > 0 || spaceList.length > 0 || userList.length > 0)" class="no-more">
        <img :src="emptyImage" style="width: 24px; opacity: 0.5; margin-right: 8px;" />
        <span>没有内容显示了哦 ~</span>
      </div>
    </div>

    <SpaceDetailModal
      v-model="spaceDetailVisible"
      :space-detail="selectedSpace"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, watch, onUnmounted, onActivated, onDeactivated } from 'vue'
import { SearchOutlined, PictureOutlined, UserOutlined, FileTextOutlined, TeamOutlined, FileImageOutlined, UserAddOutlined, FireOutlined, HistoryOutlined, ClockCircleOutlined, LoadingOutlined, LikeOutlined, MessageOutlined, AppstoreOutlined, TableOutlined, BulbOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { useRouter, useRoute } from 'vue-router'
import { searchAllUsingPost, getHotSearchKeywordsUsingGet } from '@/api/searchController'
import { joinSpaceUsingPost } from '@/api/spaceUserController'
import MobilePictureList from '@/components/MobilePictureList.vue'
import PictureList from '@/components/PictureList.vue'
import { getDeviceType } from '@/utils/device'
import { DEVICE_TYPE_ENUM } from '@/constants/device'
import WaterfallPictureList from '@/components/WaterfallPictureList.vue'
import PostList from '@/components/PostList.vue'
import SpaceInfoCard from '@/components/SpaceInfoCard.vue'
import { debounce } from 'lodash-es'
import SpaceDetailModal from '@/components/SpaceDetailModal.vue'
import { getGuessYouWantToSearchUsingGet } from '@/api/searchController'
import emptyImage from '@/assets/illustrations/empty.png'

const router = useRouter()
const route = useRoute()
const searchInput = ref()
const searchText = ref('')
const searchType = ref('picture')
const loading = ref(false)
const isEndOfData = ref(false)

// 搜索参数
const searchParams = ref({
  current: 1,
  pageSize: 20
})

// 添加类型声明
interface PictureUser {
  userId: number;
  userName: string;
  userAvatar: string;
}

interface PictureVO {
  id: number;
  url: string;
  description: string;
  thumbNum: number;
  commentCount: number;
  isThumb: boolean;
  user: PictureUser;
}

// 修改类型声明
const pictureList = ref<PictureVO[]>([])
const userList = ref<API.UserVO[]>([])
const postList = ref<API.Post[]>([])
const spaceList = ref<API.SpaceVO[]>([])
const spaceDetailVisible = ref(false)
const selectedSpace = ref<API.SpaceVO | null>(null)

// 搜索历史
const searchHistory = ref<string[]>([])
// 获取热门搜索
const hotSearches = ref([])
// 猜你想搜
const guessSearches = ref([])

// 判断是否为移动端
const isMobile = ref(getDeviceType() !== DEVICE_TYPE_ENUM.PC)

// Add view mode state
const viewMode = ref('waterfall')

// 获取热门搜索
const fetchHotSearches = async (type: string) => {
  try {
    const res = await getHotSearchKeywordsUsingGet({
      type,
      size: 12
    })
    if (res.data?.code === 0) {
      hotSearches.value = res.data.data
    }
  } catch (error) {
    console.error('获取热门搜索失败:', error)
  }
}

// 获取猜你想搜数据
const fetchGuessSearches = async (type: string) => {
  try {
    const res = await getGuessYouWantToSearchUsingGet({
      type,
      size: 12
    })
    if (res.data?.code === 0) {
      guessSearches.value = res.data.data
    }
  } catch (error) {
    console.error('获取猜你想搜数据失败:', error)
  }
}

onMounted(() => {
  // 从localStorage加载搜索历史
  const history = localStorage.getItem('searchHistory')
  if (history) {
    searchHistory.value = JSON.parse(history)
  }

  // 根据路由参数设置初始搜索类型
  if (route.query.type) {
    searchType.value = route.query.type as string
  }

  // 自动聚焦搜索框
  nextTick(() => {
    searchInput.value?.focus()
  })

  // 获取初始热门搜索
  fetchHotSearches(searchType.value)
  // 获取猜你想搜数据
  fetchGuessSearches(searchType.value)

  // 绑定滚动事件
  window.addEventListener('scroll', handleScroll)
})

// 确保每次进入页面时都使用瀑布流布局
onActivated(() => {
  // 按照路由参数或初始状态开启滚动监听
  window.addEventListener('scroll', handleScroll)
  // 确保瀑布流布局设置正确
  localStorage.setItem('streamLayout', 'waterfall')
})

onDeactivated(() => {
  window.removeEventListener('scroll', handleScroll)
})

// 组件卸载时取消未执行的防抖函数并移除监听
onUnmounted(() => {
  debouncedSearch.cancel()
  handleScroll.cancel()
  window.removeEventListener('scroll', handleScroll)
})

// 监听路由参数变化
watch(
  () => route.query.type,
  (newType) => {
    if (newType) {
      searchType.value = newType as string
      // 如果有搜索文本，立即执行搜索
      if (searchText.value) {
        doSearch()
      }
    }
  }
)

// 添加防抖搜索函数
const debouncedSearch = debounce(async (text: string) => {
  if (!text.trim()) {
    pictureList.value = []
    userList.value = []
    postList.value = []
    spaceList.value = []
    isEndOfData.value = false
    return
  }

  searchParams.value.current = 1
  isEndOfData.value = false
  loading.value = true

  try {
    const res = await searchAllUsingPost({
      searchText: text,
      type: searchType.value,
      current: searchParams.value.current,
      pageSize: searchParams.value.pageSize
    })

    if (res.data.code === 0) {
      const newData = res.data.data.content || []
      if (searchType.value === 'picture') {
        pictureList.value = newData
      } else if (searchType.value === 'post') {
        postList.value = newData
      } else if (searchType.value === 'space') {
        spaceList.value = newData
      } else {
        userList.value = newData
      }
      // 判断是否还有更多数据
      isEndOfData.value = newData.length < searchParams.value.pageSize
      // 添加到搜索历史
      addToHistory(text)
    } else {
      message.error('搜索失败：' + res.data.message)
    }
  } catch (error) {
    message.error('搜索出错，请稍后重试')
  } finally {
    loading.value = false
  }
}, 500)

// 执行搜索
const doSearch = async (text = searchText.value, isAppend = false) => {
  if (!text.trim()) {
    message.warning('请输入搜索内容')
    return
  }

  if (isAppend) {
    if (loading.value || isEndOfData.value) return
    searchParams.value.current += 1
  } else {
    searchParams.value.current = 1
    isEndOfData.value = false
    loading.value = true
  }

  try {
    const res = await searchAllUsingPost({
      searchText: text,
      type: searchType.value,
      current: searchParams.value.current,
      pageSize: searchParams.value.pageSize
    })

    if (res.data.code === 0) {
      const newData = res.data.data.content || []
      if (searchType.value === 'picture') {
        pictureList.value = isAppend ? [...pictureList.value, ...newData] : newData
      } else if (searchType.value === 'post') {
        postList.value = isAppend ? [...postList.value, ...newData] : newData
      } else if (searchType.value === 'space') {
        spaceList.value = isAppend ? [...spaceList.value, ...newData] : newData
      } else {
        userList.value = isAppend ? [...userList.value, ...newData] : newData
      }

      // 判断是否还有更多数据
      isEndOfData.value = newData.length < searchParams.value.pageSize

      if (!isAppend) {
        // 添加到搜索历史
        addToHistory(text)
      }
    } else {
      message.error('搜索失败：' + res.data.message)
    }
  } catch (error) {
    message.error('搜索出错，请稍后重试')
  } finally {
    if (!isAppend) {
      loading.value = false
    }
  }
}

// 监听滚动实现触底加载
const handleScroll = debounce(() => {
  if (!searchText.value || loading.value || isEndOfData.value) return

  const scrollTop = window.pageYOffset || document.documentElement.scrollTop
  const windowHeight = window.innerHeight
  const scrollHeight = document.documentElement.scrollHeight

  // 距离底部 200px 时触发加载
  if (scrollHeight - scrollTop - windowHeight <= 200) {
    doSearch(searchText.value, true)
  }
}, 200)

// 修改处理输入框内容变化的函数
const handleInput = () => {
  if (!searchText.value) {
    pictureList.value = []
    userList.value = []
    postList.value = []
    spaceList.value = []
    return
  }
  // 触发防抖搜索
  debouncedSearch(searchText.value)
}

// 修改搜索类型切换处理函数
const handleTypeChange = (type: string) => {
  searchType.value = type
  // 清空之前的搜索结果
  pictureList.value = []
  postList.value = []
  userList.value = []
  spaceList.value = []
  // 当输入框为空时，获取对应类型的热门搜索
  if (!searchText.value) {
    fetchHotSearches(type)
    fetchGuessSearches(type)
  } else {
    doSearch()
  }
}

// 添加到搜索历史
const addToHistory = (text: string) => {
  if (!searchHistory.value.includes(text)) {
    searchHistory.value.unshift(text)
    if (searchHistory.value.length > 10) {
      searchHistory.value.pop()
    }
    localStorage.setItem('searchHistory', JSON.stringify(searchHistory.value))
  }
}

// 清空搜索历史
const clearHistory = () => {
  searchHistory.value = []
  localStorage.removeItem('searchHistory')
}

// 显示热度计算说明
const showHotScoreExplanation = () => {
  message.info({
    content: '热度值计算规则：热度值 = 0.7 × 实时搜索次数 + 0.3 × 历史搜索次数，综合反映关键词的即时热度和长期受欢迎程度。',
    duration: 5,
  });
}

// 修改点击标签搜索函数
const searchByTag = (text: string) => {
  searchText.value = text
  // 立即执行搜索，不使用防抖
  doSearch(text)
}

// 处理图片点击
const handlePictureClick = (item: API.PictureVO) => {
  router.push(`/picture/${item.id}`)
}

// 处理用户点击
const handleUserClick = (user) => {
  if (!user) return
  // console.log('用户点击', user)
  router.push({
    path: `/user/${user.id}`,
    query: {
      userName: user.userName,
      userAvatar: user.userAvatar,
      userAccount: user.userAccount,
      userProfile: user.userProfile,
      userRole: user.userRole,
      createTime: user.createTime
    }
  })
}

// 跳转到空间详情页
const goToSpaceDetail = (space: API.SpaceVO) => {
  if (!space || !space.id) return
  router.push(`/space/${space.id}`)
}
</script>

<style scoped>
.search-page {
  min-height: 88vh;
  color: var(--text-primary);
  background-color: var(--background);
}

.search-header {
  z-index: 100;
  margin-top: 32px;
}

.search-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 16px;
}

.search-bar {
  margin-bottom: 12px;
}

.modern-search {
  :deep(.ant-input-wrapper) {
    border-radius: 999px;
    background-color: var(--card-background);
    border: 1px solid var(--border-color);
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    backdrop-filter: blur(10px);
    -webkit-backdrop-filter: blur(10px);

    &:focus-within {
      background-color: var(--background);
      box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.15);
      border-color: #2563eb;
    }
  }

  :deep(.ant-input) {
    height: 44px;
    font-size: 15px;
    background-color: transparent;
    border: none;
    padding-left: 20px;
    padding-right: 20px;
    color: var(--text-primary);

    &::placeholder {
      color: var(--text-secondary);
      opacity: 0.6;
    }
  }

  :deep(.ant-input-search-button) {
    background-color: #2563eb;
    border-radius: 999px;
    width: 44px;
    height: 44px;
    padding: 0;
    border: none;
    transition: all 0.3s;

    &:hover {
      background-color: #3b82f6;
      transform: scale(1.05);
    }
  }
}

.search-type {
  margin-top: 32px;
}

.type-buttons {
  display: flex;
  width: 100%;
  padding-bottom: 8px;
}

.type-button {
  flex: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px 14px;
  border-radius: 999px;
  font-size: 14px;
  white-space: nowrap;
  cursor: pointer;
  background-color: var(--card-background);
  color: var(--text-secondary);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid var(--border-color);

  &:hover {
    background-color: var(--hover-background);
    color: var(--text-primary);
  }

  &.active {
    background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
    color: white;
    border-color: transparent;
    box-shadow: 0 4px 12px rgba(37, 99, 235, 0.2);
  }

  .count {
    font-size: 12px;
    background-color: rgba(255, 255, 255, 0.2);
    padding: 1px 6px;
    border-radius: 999px;
  }
}

.search-suggestions {
  max-width: 1200px;
  margin: 0 auto;
}

@media (max-width: 768px) {
  .search-suggestions {
    padding: 16px 0;
  }
}

@media (min-width: 769px) {
  .search-suggestions {
    padding: 16px 0;
  }
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 0 0 16px 0;
  display: flex;
  align-items: center;
  padding: 0 16px;
}

.hot-searches {
  margin-bottom: 30px;
}

.hot-grid {
  display: grid;
  gap: 6px;
  padding: 0 16px;
}

@media (min-width: 1200px) {
  .hot-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}

@media (min-width: 769px) and (max-width: 1199px) {
  .hot-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (min-width: 576px) and (max-width: 768px) {
  .hot-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 575px) {
  .hot-grid {
    grid-template-columns: 1fr;
  }
}

.space-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 24px;
  padding: 16px 0;
}

@media (max-width: 768px) {
  .space-grid {
    grid-template-columns: 1fr;
    gap: 16px;
    padding: 8px 0;
  }
}

.hot-item {
  display: flex;
  align-items: center;
  padding: 14px 16px;
  background-color: var(--card-background);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid var(--border-color);

  &:hover {
    background-color: var(--hover-background);
    transform: translateY(-2px);
    box-shadow: 0 4px 12px var(--shadow-color);
  }

  .hot-rank {
    width: 24px;
    height: 24px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    margin-right: 12px;
    font-size: 14px;
    font-weight: 600;
    color: var(--text-secondary);
    background-color: var(--hover-background);

    &.top3 {
      color: white;
      background: linear-gradient(135deg, #ff6b6b 0%, #ee5253 100%);
    }
  }

  .hot-text {
    flex: 1;
    font-size: 15px;
    color: var(--text-primary);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .hot-meta {
    font-size: 12px;
    color: #999;
    margin-left: 8px;
  }

  .fire-icon {
    color: #2563eb;
    margin-left: 8px;
    font-size: 16px;
  }
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding: 0 16px;
}

.pagination-footer {
  padding: 40px 0 60px;
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
}

.loading-more {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--text-secondary);
  font-size: 15px;

  i {
    font-size: 20px;
    color: #2563eb;
  }
}

.no-more {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-secondary);
  font-size: 14px;
  opacity: 0.8;
}

.clear-btn {
  font-size: 14px;
  color: #999;
  cursor: pointer;

  &:hover {
    color: #2563eb;
  }
}

.history-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  padding: 0 16px;
}

.history-tag {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background-color: var(--card-background);
  border-radius: 999px;
  font-size: 15px;
  color: var(--text-primary);
  cursor: pointer;
  border: 1px solid var(--border-color);
  transition: all 0.3s ease;

  &:hover {
    background-color: #f9f9f9;
  }

  .tag-icon {
    font-size: 14px;
    color: #999;
  }
}

.search-results {
  max-width: 1400px;
  margin: 0 auto;
}

.results-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.results-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.view-options {
  :deep(.ant-radio-group) {
    background: #f5f5f5;
    border-radius: 999px;
    padding: 4px;
  }

  :deep(.ant-radio-button) {
    border: none !important;
    border-radius: 999px !important;
    background: transparent;
    color: #666;

    &:before {
      display: none;
    }

    &.ant-radio-button-checked {
      background: #2563eb;
      color: white;
    }
  }

  :deep(.ant-radio-group-solid ) {
    color: #fff;
    background: #2563eb;
    border-color: #2563eb;
  }

  :deep(.ant-radio-group-solid ) {
    color: #fff;
    background: #3b82f6;
    border-color: #3b82f6;
  }
}

.picture-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 12px;
}

@media (max-width: 600px) {
  .picture-grid.mobile-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 10px;
  }

  .xh-searchbar {
    max-width: 100%!important;
  }

  .search-results {
    margin: 4px 8px;
  }
}

.grid-item {
  position: relative;
  aspect-ratio: 3/4;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  background-color: #f5f5f5;
}

.image-wrapper {
  width: 100%;
  height: 100%;
  position: relative;
}

.image-wrapper img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.image-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 8px;
  background: linear-gradient(transparent, rgba(0, 0, 0, 0.6));
  color: white;
}

.overlay-content {
  .user-info {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 8px;

    :deep(.ant-avatar) {
      width: 24px;
      height: 24px;
      line-height: 24px;
    }

    span {
      font-size: 13px;
      font-weight: 500;
    }
  }
}

.image-stats {
  display: flex;
  gap: 12px;
  font-size: 13px;

  span {
    display: flex;
    align-items: center;
    gap: 4px;
  }
}

.space-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.space-item {
  background-color: white;
  border-radius: 8px;
  padding: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  display: flex;
  flex-direction: column;
}

.space-item .space-info-card {
  width: 100%;
  box-shadow: none;
  margin: 0;
}

.space-actions {
  display: flex;
  gap: 8px;
  justify-content: center;
}

.space-actions .ant-btn {
  width: 100%;
  margin: 0;
}

:deep(.ant-btn) {
  height: 32px;
  font-size: 14px;
  border-radius: 4px;
}

:deep(.ant-btn-primary) {
  background-color: #2563eb;
  border-color: #2563eb;

  &:hover, &:focus {
    background-color: #3b82f6;
    border-color: #3b82f6;
  }
}

.space-card {
  background-color: white;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.space-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.space-name {
  font-size: 16px;
  font-weight: 600;
  margin: 0;
}

.space-type {
  background-color: #f5f5f5;
  color: #666;
  border: none;
  border-radius: 999px;
  padding: 2px 8px;
  font-size: 12px;
}

.space-owner {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  cursor: pointer;
}

.owner-info {
  flex: 1;
}

.owner-name {
  font-weight: 500;
  display: block;
}

.owner-title {
  font-size: 12px;
  color: #999;
}

.space-stats {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
  color: #666;
  font-size: 14px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.space-actions {
  display: flex;
  gap: 8px;
}

:deep(.ant-btn) {
  height: 32px;
  font-size: 14px;
  border-radius: 4px;
}

:deep(.ant-btn-primary) {
  background-color: #2563eb;
  border-color: #2563eb;

  &:hover, &:focus {
    background-color: #3b82f6;
    border-color: #3b82f6;
  }
}

.user-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 16px;
}

.user-card {
  background-color: var(--card-background);
  border-radius: 12px;
  padding: 16px 0;
  border: 1px solid var(--border-color);
  transition: all 0.3s ease;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 8px 24px var(--shadow-color);
  }
}

.user-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 16px;
  cursor: pointer;
}

.user-info {
  flex: 1;
}

.user-name {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 4px 0;
}

.user-bio {
  font-size: 14px;
  color: #666;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.user-stats {
  display: flex;
  gap: 16px;
  color: #666;
  font-size: 14px;
  margin: 0 12px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 300px;
}

.post-results-list {
  margin-top: 16px;
}

.xh-searchbar {
  display: flex;
  gap: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  margin: auto;
  max-width: 80%;
  align-items: center;
  background: var(--card-background);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-radius: 999px;
  padding: 6px;
  border: 1px solid var(--border-color);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.xh-searchbar:focus-within {
  box-shadow: 0 8px 30px rgba(37, 99, 235, 0.15);
  border-color: #2563eb;
  background: var(--background);
}

.xh-search-input {
  flex: 1;
  border: none;
  background: transparent;
  outline: none;
  height: 36px;
  padding: 0 12px;
  font-size: 14px;
}

.xh-search-input::placeholder {
  color: var(--text-secondary);
  opacity: 0.6;
}

.xh-search-btn {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: #fff;
  border-radius: 999px;
  height: 36px;
  padding: 0 16px;
  line-height: 36px;
  cursor: pointer;
}

.xh-search-btn:active {
  opacity: .9;
}

.xh-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.xh-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 12px 16px 8px;
}

.history-scroll {
  display: grid;
  grid-auto-flow: column;
  grid-template-rows: repeat(2, auto);
  gap: 8px;
  overflow-x: auto;
  padding: 0 16px 6px;
}

.history-scroll::-webkit-scrollbar {
  height: 6px;
}

.history-scroll::-webkit-scrollbar-thumb {
  background: #e5e5e5;
  border-radius: 999px;
}

.history-pill {
  padding: 8px 16px;
  border-radius: 999px;
  white-space: nowrap;
  color: var(--text-secondary);
  background: var(--card-background);
  border: 1px solid var(--border-color);
  font-size: 13px;
  transition: all 0.3s ease;
}

.history-pill:hover {
  border-color: #2563eb;
  color: #2563eb;
}

.guess-scroll {
  display: grid;
  grid-auto-flow: column;
  grid-template-rows: repeat(2, auto);
  gap: 8px;
  overflow-x: auto;
  padding: 0 16px 6px;
  margin-bottom: 12px;
}

.guess-scroll::-webkit-scrollbar {
  height: 6px;
}

.guess-scroll::-webkit-scrollbar-thumb {
  background: #e5e5e5;
  border-radius: 999px;
}

.guess-pill {
  display: flex;
  align-items: center;
  padding: 8px 16px;
  border-radius: 999px;
  white-space: nowrap;
  color: var(--text-secondary);
  background: var(--card-background);
  border: 1px solid var(--border-color);
  font-size: 13px;
  transition: all 0.3s ease;
}

.guess-pill:hover {
  background: var(--hover-background);
  color: #2563eb;
  border-color: #2563eb;
}

.guess-pill .bulb-icon {
  margin-right: 4px;
  color: #2563eb;
}

/* 统一空状态样式 */
.xh-empty-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 60px 0;
  width: 100%;
}

.custom-empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  animation: emptyFadeIn 0.8s ease-out;
  text-align: center;
}

.empty-illustration {
  width: 240px;
  height: auto;
  opacity: 0.8;
  filter: drop-shadow(0 10px 20px rgba(0,0,0,0.05));
}

.empty-text {
  font-size: 15px;
  color: var(--text-secondary);
  margin-top: 8px;
}

@keyframes emptyFadeIn {
  from { opacity: 0; transform: translateY(15px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
