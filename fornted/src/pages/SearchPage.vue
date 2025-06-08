<template>
  <div class="search-page">
    <!-- 搜索头部 -->
    <div class="search-header">
      <div class="search-container">
        <div class="search-bar">
          <a-input-search
            v-model:value="searchText"
            placeholder="搜索优质图片、创意视频、设计素材..."
            enter-button="搜索"
            @search="doSearch"
            @input="handleInput"
            ref="searchInput"
            class="modern-search"
            size="large"
            autofocus
          >
            <template #prefix>
              <SearchOutlined class="search-icon" />
            </template>
          </a-input-search>
        </div>

        <!-- 搜索类型切换 -->
        <div class="search-type">
          <div class="type-buttons">
            <div
              class="type-button"
              :class="{ active: searchType === 'picture' }"
              @click="() => handleTypeChange('picture')"
            >
              <PictureOutlined class="icon" />
              <span>图片</span>
              <span class="count" v-if="searchText">{{ pictureList.length }}</span>
            </div>
            <div
              class="type-button"
              :class="{ active: searchType === 'post' }"
              @click="() => handleTypeChange('post')"
            >
              <FileTextOutlined class="icon" />
              <span>帖子</span>
              <span class="count" v-if="searchText">{{ postList.length }}</span>
            </div>
            <div
              class="type-button"
              :class="{ active: searchType === 'space' }"
              @click="() => handleTypeChange('space')"
            >
              <TeamOutlined class="icon" />
              <span>空间</span>
              <span class="count" v-if="searchText">{{ spaceList.length }}</span>
            </div>
            <div
              class="type-button"
              :class="{ active: searchType === 'user' }"
              @click="() => handleTypeChange('user')"
            >
              <UserOutlined class="icon" />
              <span>用户</span>
              <span class="count" v-if="searchText">{{ userList.length }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 搜索建议区域 -->
    <div v-if="!searchText" class="search-suggestions">
      <div class="suggestions-container">
        <!-- 热门搜索 -->
        <div class="hot-searches">
          <h3 class="section-title">
            <FireOutlined class="title-icon" />
            热门搜索
          </h3>
          <div class="hot-grid">
            <div
              v-for="(item, index) in hotSearches"
              :key="item"
              class="hot-item"
              @click="searchByTag(item)"
            >
              <span class="hot-rank" :class="{ 'top3': index < 3 }">{{ index + 1 }}</span>
              <span class="hot-text">{{ item }}</span>
              <FireOutlined v-if="index < 3" class="fire-icon" />
            </div>
          </div>
        </div>

        <!-- 搜索历史 -->
        <div class="search-history" v-if="searchHistory.length > 0">
          <div class="history-header">
            <h3 class="section-title">
              <HistoryOutlined class="title-icon" />
              搜索历史
            </h3>
            <a @click="clearHistory" class="clear-btn">清空历史</a>
          </div>
          <div class="history-tags">
            <div
              v-for="item in searchHistory"
              :key="item"
              class="history-tag"
              @click="searchByTag(item)"
            >
              <ClockCircleOutlined class="tag-icon" />
              <span>{{ item }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 搜索结果区域 -->
    <div v-else class="search-results">
      <div v-if="loading" class="loading-container">
        <a-spin size="large">
          <template #indicator>
            <LoadingOutlined style="font-size: 24px" spin />
          </template>
        </a-spin>
      </div>
      <template v-else>
        <!-- 图片搜索结果 -->
        <div v-if="searchType === 'picture'" class="picture-results">
          <div class="results-header">
            <h2 class="results-title">找到 {{ pictureList.length }} 张相关图片</h2>
            <div class="view-options">
              <a-radio-group v-model:value="viewMode" button-style="solid">
                <a-radio-button value="waterfall">
                  <AppstoreOutlined />
                </a-radio-button>
                <a-radio-button value="grid">
                  <TableOutlined />
                </a-radio-button>
              </a-radio-group>
            </div>
          </div>

          <WaterfallPictureList
            v-if="!isMobile && viewMode === 'waterfall' && pictureList.length > 0"
            :dataList="pictureList"
            :loading="loading"
          />
          <div
            v-else-if="pictureList.length > 0"
            class="picture-grid"
            :class="{ 'mobile-grid': isMobile }"
          >
            <div
              v-for="item in pictureList"
              :key="item.id"
              class="grid-item"
              @click="handlePictureClick(item)"
            >
              <div class="image-wrapper">
                <img :src="item.url" :alt="item.description" />
                <div class="image-overlay">
                  <div class="overlay-content">
                    <div class="user-info">
                      <a-avatar :src="item.user?.userAvatar" :size="32" />
                      <span>{{ item.user?.userName }}</span>
                    </div>
                    <div class="image-stats">
                      <span><LikeOutlined /> {{ item.thumbNum }}</span>
                      <span><MessageOutlined /> {{ item.commentCount }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <a-empty v-else description="暂无搜索结果" />
        </div>

        <!-- 帖子搜索结果 -->
        <div v-else-if="searchType === 'post'" class="post-results">
          <h2 class="results-title">找到 {{ postList.length }} 个相关帖子</h2>
          <div class="post-grid">
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
            <div v-for="space in spaceList" :key="space.id" class="space-card">
              <div class="space-header">
                <h3 class="space-name">{{ space.spaceName }}</h3>
                <a-tag class="space-type">团队空间</a-tag>
              </div>
              <div class="space-owner" @click="handleUserClick(space.user)">
                <a-avatar :src="space.user?.userAvatar" :size="40" />
                <div class="owner-info">
                  <span class="owner-name">{{ space.user?.userName }}</span>
                  <span class="owner-title">创建者</span>
                </div>
              </div>
              <div class="space-stats">
                <div class="stat-item">
                  <FileImageOutlined />
                  <span>{{ space.totalCount }} 张图片</span>
                </div>
                <div class="stat-item">
                  <TeamOutlined />
                  <span>{{ space.memberCount || 0 }} 位成员</span>
                </div>
              </div>
              <div class="space-actions">
                <a-button type="primary" @click="handleJoinSpace(space)">
                  <UserAddOutlined />
                  申请加入
                </a-button>
                <a-button @click="showSpaceDetail(space)">查看详情</a-button>
              </div>
            </div>
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
                  <PictureOutlined />
                  <span>{{ user.pictureCount || 0 }} 张图片</span>
                </div>
                <div class="stat-item">
                  <TeamOutlined />
                  <span>{{ user.followCount || 0 }} 位关注者</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>

    <SpaceDetailModal
      v-model="spaceDetailVisible"
      :space-detail="selectedSpace"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, watch, onUnmounted } from 'vue'
import { SearchOutlined, PictureOutlined, UserOutlined, FileTextOutlined, TeamOutlined, FileImageOutlined, UserAddOutlined, FireOutlined, HistoryOutlined, ClockCircleOutlined, LoadingOutlined, LikeOutlined, MessageOutlined, AppstoreOutlined, TableOutlined } from '@ant-design/icons-vue'
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
import { debounce } from 'lodash-es'
import SpaceDetailModal from '@/components/SpaceDetailModal.vue'

const router = useRouter()
const route = useRoute()
const searchInput = ref()
const searchText = ref('')
const searchType = ref('picture')
const loading = ref(false)

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

// 判断是否为移动端
const isMobile = ref(getDeviceType() !== DEVICE_TYPE_ENUM.PC)

// Add view mode state
const viewMode = ref('waterfall')

// 获取热门搜索
const fetchHotSearches = async (type: string) => {
  try {
    const res = await getHotSearchKeywordsUsingGet({
      type,
      size: 9
    })
    if (res.data?.code === 0) {
      hotSearches.value = res.data.data
    }
  } catch (error) {
    console.error('获取热门搜索失败:', error)
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
    return
  }

  loading.value = true

  try {
    const res = await searchAllUsingPost({
      searchText: text,
      type: searchType.value,
      current: 1,
      pageSize: 20
    })

    if (res.data.code === 0) {
      if (searchType.value === 'picture') {
        pictureList.value = res.data.data.content || []
      } else if (searchType.value === 'post') {
        postList.value = res.data.data.content || []
      } else if (searchType.value === 'space') {
        spaceList.value = res.data.data.content || []
      } else {
        userList.value = res.data.data.content || []
      }
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
}, 500)  // 设置 0.5 秒的延迟

// 执行搜索
const doSearch = async (text = searchText.value) => {
  if (!text.trim()) {
    message.warning('请输入搜索内容')
    return
  }

  loading.value = true

  try {
    const res = await searchAllUsingPost({
      searchText: text,
      type: searchType.value,
      current: 1,
      pageSize: 20
    })

    if (res.data.code === 0) {
      if (searchType.value === 'picture') {
        pictureList.value = res.data.data.content || []
      } else if (searchType.value === 'post') {
        postList.value = res.data.data.content || []
      } else if (searchType.value === 'space') {
        spaceList.value = res.data.data.content || []
      } else {
        userList.value = res.data.data.content || []
      }
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
}

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

// 显示空间详情
const showSpaceDetail = (space: API.SpaceVO) => {
  selectedSpace.value = space
  spaceDetailVisible.value = true
}

// 申请加入空间
const handleJoinSpace = async (space: API.SpaceVO) => {
  if (!space || !space.id) return

  try {
    const res = await joinSpaceUsingPost({
      spaceId: space.id
    })

    if (res.data?.code === 0) {
      message.success('申请已提交，请等待管理员审核')
    } else {
      message.error(res.data?.message || '申请失败')
    }
  } catch (error) {
    message.error('申请失败，请稍后重试')
  }
}

// 组件卸载时取消未执行的防抖函数
onUnmounted(() => {
  debouncedSearch.cancel()
})
</script>

<style scoped>
.search-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #f6f8fc 0%, #f0f4f8 100%);
  position: relative;
  overflow: hidden;
  padding-top: 0;
  margin: -24px;
}

.search-page::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 400px;
  background: linear-gradient(135deg,
  rgba(64, 169, 255, 0.1) 0%,
  rgba(24, 144, 255, 0.05) 100%
  );
  pointer-events: none;
}

.search-page::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 300px;
  background: linear-gradient(135deg,
  rgba(255, 122, 69, 0.05) 0%,
  rgba(255, 169, 64, 0.08) 100%
  );
  pointer-events: none;
}

.search-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  padding: 24px 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);
}

.search-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 16px;
}

.search-bar {
  max-width: 600px;
  margin: 0 auto 16px;
}

.modern-search {
  :deep(.ant-input-wrapper) {
    border-radius: 12px;
    overflow: hidden;
    box-shadow: 0 4px 20px rgba(24, 144, 255, 0.1);
    transition: all 0.3s ease;

    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 8px 30px rgba(24, 144, 255, 0.15);
    }
  }

  :deep(.ant-input) {
    height: 48px;
    font-size: 16px;
    padding: 4px 16px;
    border: 1px solid rgba(24, 144, 255, 0.1);
    background: rgba(255, 255, 255, 0.9);

    &:hover, &:focus {
      border-color: #1890ff;
      box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.1);
      background: #ffffff;
    }
  }

  :deep(.ant-input-search-button) {
    height: 48px;
    padding: 0 28px;
    font-size: 16px;
    background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
    border: none;
    transition: all 0.3s ease;

    &:hover {
      background: linear-gradient(135deg, #40a9ff 0%, #1890ff 100%);
      transform: translateX(1px);
    }
  }
}

.search-type {
  margin-top: 20px;
}

.type-buttons {
  display: flex;
  justify-content: center;
  gap: 16px;
  flex-wrap: wrap;
}

.type-button {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.3s;
  color: #666;
  font-size: 15px;
  position: relative;
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(0, 0, 0, 0.04);

  &:hover {
    color: #1890ff;
    background: rgba(255, 255, 255, 0.95);
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(24, 144, 255, 0.1);
  }

  &.active {
    color: #1890ff;
    font-weight: 500;
    background: #ffffff;
    box-shadow: 0 4px 12px rgba(24, 144, 255, 0.1);

    &::after {
      content: '';
      position: absolute;
      bottom: -8px;
      left: 0;
      width: 100%;
      height: 2px;
      background: linear-gradient(90deg, #1890ff, #40a9ff);
      border-radius: 2px;
    }
  }

  .count {
    font-size: 12px;
    background: rgba(24, 144, 255, 0.1);
    padding: 2px 8px;
    border-radius: 10px;
    color: #1890ff;
  }
}

.suggestions-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 16px;
}

.section-title {
  font-size: 18px;
  color: #333;
  margin-bottom: 24px;
  display: flex;
  align-items: center;
  gap: 8px;

  .title-icon {
    color: #1890ff;
  }
}

.hot-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 12px;
  margin-bottom: 32px;
}

.hot-item {
  display: flex;
  align-items: center;
  padding: 16px;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s;
  border: 1px solid rgba(0, 0, 0, 0.04);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02);

  &:hover {
    background: #ffffff;
    transform: translateY(-2px);
    box-shadow: 0 8px 24px rgba(24, 144, 255, 0.1);
  }

  .hot-rank {
    width: 28px;
    height: 28px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #f0f5ff;
    border-radius: 8px;
    margin-right: 12px;
    font-size: 14px;
    color: #1890ff;
    font-weight: 500;

    &.top3 {
      background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
      color: #fff;
    }
  }

  .hot-text {
    flex: 1;
    font-size: 14px;
    color: #333;
  }

  .fire-icon {
    color: #ff4d4f;
    font-size: 16px;
  }
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;

  .clear-btn {
    color: #1890ff;
    font-size: 14px;
    cursor: pointer;

    &:hover {
      color: #40a9ff;
    }
  }
}

.history-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.history-tag {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  font-size: 14px;
  color: #666;
  border: 1px solid rgba(0, 0, 0, 0.04);

  &:hover {
    background: #ffffff;
    color: #1890ff;
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(24, 144, 255, 0.1);
  }

  .tag-icon {
    font-size: 14px;
    color: #1890ff;
  }
}

.search-results {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 16px;
}

.results-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.results-title {
  font-size: 20px;
  color: #333;
  margin: 0;
}

.picture-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
  margin-top: 16px;
}

.grid-item {
  position: relative;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;

  &:hover .image-overlay {
    opacity: 1;
  }

  .image-wrapper {
    position: relative;
    padding-top: 75%;

    img {
      position: absolute;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }
}

.image-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  opacity: 0;
  transition: all 0.3s;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding: 16px;
  color: #fff;
}

.overlay-content {
  .user-info {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 8px;
  }

  .image-stats {
    display: flex;
    gap: 16px;
    font-size: 14px;

    span {
      display: flex;
      align-items: center;
      gap: 4px;
    }
  }
}

.space-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.space-card {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  transition: all 0.3s;
  border: 1px solid rgba(0, 0, 0, 0.04);
  backdrop-filter: blur(10px);

  &:hover {
    transform: translateY(-4px);
    background: #ffffff;
    box-shadow: 0 8px 30px rgba(24, 144, 255, 0.12);
  }
}

.space-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;

  .space-name {
    font-size: 18px;
    color: #333;
    margin: 0;
  }

  .space-type {
    background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%);
    color: #1890ff;
    border: none;
    font-weight: 500;
  }
}

.space-owner {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: rgba(240, 245, 255, 0.6);
  backdrop-filter: blur(4px);
  border-radius: 8px;
  cursor: pointer;
  margin-bottom: 20px;

  .owner-info {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  .owner-name {
    font-size: 15px;
    color: #333;
  }

  .owner-title {
    font-size: 12px;
    color: #666;
  }
}

.space-stats {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;

  .stat-item {
    flex: 1;
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 12px;
    background: rgba(240, 245, 255, 0.6);
    backdrop-filter: blur(4px);
    border-radius: 6px;
    font-size: 14px;
    color: #666;
  }
}

.space-actions {
  display: flex;
  gap: 12px;

  .ant-btn {
    flex: 1;
    height: 36px;
  }
}

.user-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.user-card {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  transition: all 0.3s;
  border: 1px solid rgba(0, 0, 0, 0.04);
  backdrop-filter: blur(10px);

  &:hover {
    transform: translateY(-4px);
    background: #ffffff;
    box-shadow: 0 8px 30px rgba(24, 144, 255, 0.12);
  }
}

.user-header {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
  cursor: pointer;

  .user-info {
    flex: 1;
  }

  .user-name {
    font-size: 18px;
    color: #333;
    margin: 0 0 8px;
  }

  .user-bio {
    font-size: 14px;
    color: #666;
    margin: 0;
    line-height: 1.5;
  }
}

.user-stats {
  display: flex;
  gap: 12px;

  .stat-item {
    flex: 1;
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 12px;
    background: rgba(240, 245, 255, 0.6);
    backdrop-filter: blur(4px);
    border-radius: 6px;
    font-size: 14px;
    color: #666;
  }
}

.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 200px;
}

@media screen and (max-width: 768px) {
  .search-header {
    padding: 12px 0;
  }

  .search-container {
    padding: 0 12px;
  }

  .search-bar {
    margin-bottom: 12px;
  }

  .type-buttons {
    gap: 8px;
    padding: 0 4px;
  }

  .type-button {
    padding: 6px 12px;
    font-size: 14px;
  }

  .suggestions-container,
  .search-results {
    padding: 16px 12px;
  }

  .hot-grid {
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
    gap: 8px;
    margin-bottom: 24px;
  }

  .picture-grid.mobile-grid {
    grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
    gap: 8px;
  }

  .space-grid,
  .user-grid {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .space-card,
  .user-card {
    padding: 16px;
  }

  .space-owner,
  .user-header {
    flex-direction: column;
    align-items: center;
    text-align: center;
    gap: 8px;
  }

  .space-stats,
  .user-stats {
    flex-direction: column;
    gap: 8px;
  }

  .stat-item {
    padding: 8px;
  }

  .space-actions {
    flex-direction: column;
    gap: 8px;
  }
}

@media screen and (min-width: 769px) and (max-width: 1024px) {
  .search-container,
  .suggestions-container,
  .search-results {
    max-width: 900px;
  }

  .hot-grid {
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  }

  .picture-grid {
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  }

  .space-grid,
  .user-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
