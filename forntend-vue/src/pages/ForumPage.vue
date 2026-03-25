<template>
  <div id="forumPage">
    <div class="page-container" @scroll="handleScroll" ref="pcContainer">
      <div class="content-wrapper">
        <div class="mobile-header" v-if="device === DEVICE_TYPE_ENUM.MOBILE">
          <div class="mobile-nav-bar">
            <div class="tab-container">
              <div
                class="mobile-tab-item"
                :class="{ active: activeTab === 'all' }"
                @click="handleTabChange('all')"
              >
                推荐
              </div>
              <div
                class="mobile-tab-item"
                :class="{ active: activeTab === 'following' }"
                @click="handleTabChange('following')"
              >
                关注
              </div>
            </div>
            <div class="mobile-search mobile-search-fixed" @click="handleSearchClick">
              <button class="search-button">
                <div class="search-content">
                  <i class="fas fa-search search-icon"></i>
                  <span class="search-divider">|</span>
                  <span class="search-text">搜索</span>
                </div>
              </button>
            </div>
          </div>
          <div v-if="activeTab === 'all'" class="mobile-category-nav">
            <div class="mobile-category-bar-wrapper">
              <div class="mobile-category-bar" ref="mobileCategoryBar">
                <div
                  class="mobile-category-item"
                  v-for="category in categories"
                  :key="category"
                  :class="{ active: selectedCategory === category }"
                  @click="handleMobileCategoryChange(category)"
                >
                  {{ category }}
                </div>
              </div>
              <div class="category-expand-btn" @click="toggleCategoryDropdown">
                <i class="fas fa-chevron-down" :class="{ rotated: showCategoryDropdown }"></i>
              </div>
            </div>

            <!-- 宫格展开弹窗 -->
            <transition name="fade">
              <div class="category-dropdown-overlay" v-show="showCategoryDropdown" @click="showCategoryDropdown = false"></div>
            </transition>
            <transition name="slide-down">
              <div class="category-dropdown-panel" v-show="showCategoryDropdown">
                <div class="dropdown-header">
                  <span class="dropdown-title">全部分类</span>
                  <div class="close-btn-wrapper" @click="showCategoryDropdown = false">
                    <i class="fas fa-times"></i>
                  </div>
                </div>
                <div class="dropdown-grid">
                  <div
                    class="dropdown-category-item"
                    v-for="category in categories"
                    :key="category"
                    :class="{ active: selectedCategory === category }"
                    @click="handleMobileCategoryChange(category)"
                  >
                    {{ category }}
                  </div>
                </div>
              </div>
            </transition>
          </div>
          <div v-if="activeTab === 'following'" class="mobile-follow-users-container">
            <div v-if="followUsersLoading" class="follow-users-loading">
              <i class="fas fa-spinner fa-spin loading-icon-small"></i>
              <span class="loading-text">加载关注列表中...</span>
            </div>
            <div v-else-if="followUsers.length === 0" class="follow-users-empty">
              <p class="empty-text">还没有关注任何用户</p>
            </div>
            <div v-else class="follow-users-container">
              <div class="follow-users-list">
                <div
                  v-for="user in followUsers"
                  :key="user.id"
                  class="follow-user-item-horizontal"
                  @click="$router.push(`/user/${user.id}`)"
                >
                  <img :src="user.userAvatar || getDefaultAvatar(user.userName)" :alt="user.userName" class="follow-user-avatar" />
                  <span class="follow-user-name">{{ user.userName }}</span>
                </div>
                <div class="follow-user-item-horizontal view-more-item" @click="$router.push('/follow-list')">
                  <div class="view-more-avatar">
                    <i class="fas fa-plus"></i>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="nav-category-wrapper" v-else>
          <div class="hero-banner-section">
            <div class="banner-container">
              <img
                :src="currentSeasonBanner"
                class="hero-banner-img"
                :class="{ loaded: bannerLoaded }"
                @load="bannerLoaded = true"
              />
              <div class="banner-overlay"></div>
            </div>
            <div class="hero-content">
              <div class="glass-nav-container">
                <div class="nav-upper">
                  <div class="tab-pill-group">
                    <div
                      class="tab-pill"
                      :class="{ active: activeTab === 'all' }"
                      @click="handleTabChange('all')"
                    >
                      发现
                    </div>
                    <div
                      class="tab-pill"
                      :class="{ active: activeTab === 'following' }"
                      @click="handleTabChange('following')"
                    >
                      关注
                    </div>
                  </div>
                </div>

                <div class="nav-lower">
                  <!-- PC 发现页分类：精选 + 更多 -->
                  <div v-if="activeTab === 'all'" class="pc-category-dashboard">
                    <div class="category-grid-inline">
                      <div
                        class="category-item-pill"
                        :class="{ active: selectedCategory === 'all' }"
                        @click="handleCategoryChange('all')"
                      >
                        推荐
                      </div>
                      <div
                        v-for="category in topCategories"
                        :key="category"
                        class="category-item-pill"
                        :class="{ active: selectedCategory === category }"
                        @click="handleCategoryChange(category)"
                      >
                        {{ category }}
                      </div>

                      <!-- 更多分类 Popover -->
                      <a-popover
                        v-if="remainingCategories.length > 0"
                        v-model:visible="moreCategoriesVisible"
                        placement="bottomRight"
                        trigger="click"
                        overlayClassName="apple-category-popover"
                      >
                        <template #content>
                          <div class="more-category-grid">
                            <div
                              v-for="category in remainingCategories"
                              :key="category"
                              class="more-category-item"
                              :class="{ active: selectedCategory === category }"
                              @click="handleCategoryChange(category); moreCategoriesVisible = false"
                            >
                              {{ category }}
                            </div>
                          </div>
                        </template>
                        <div class="category-item-pill more-trigger">
                          <AppstoreOutlined />
                          <span>更多</span>
                        </div>
                      </a-popover>
                    </div>
                  </div>

                  <div v-if="activeTab === 'following'" class="pc-follow-wrapper">
                    <div v-if="followUsersLoading" class="follow-users-loading">
                      <i class="fas fa-spinner fa-spin"></i>
                    </div>
                    <div v-else-if="followUsers.length === 0" class="follow-users-empty-mini">
                      暂无关注
                    </div>
                    <div v-else class="pc-follow-list">
                      <div
                        v-for="user in followUsers"
                        :key="user.id"
                        class="pc-follow-user"
                        @click="$router.push(`/user/${user.id}`)"
                        :title="user.userName"
                      >
                        <img :src="user.userAvatar || getDefaultAvatar(user.userName)" class="mini-avatar" />
                      </div>
                      <div class="pc-follow-user add-follow" @click="$router.push('/follow-list')">
                        <i class="fas fa-plus mini-icon"></i>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="content-main">
          <div v-if="activeTab === 'following' && posts.length === 0 && !loading" class="empty-following">
            <div class="custom-empty-state">
              <img :src="emptyImage" alt="暂无内容" class="empty-illustration" />
              <p class="empty-desc">暂无关注内容</p>
              <p class="empty-subdesc">关注感兴趣的创作者，获取第一手帖子更新</p>
              <a-button type="primary" class="go-discover-btn" @click="handleTabChange('all')">去发现</a-button>
            </div>
          </div>
          <div class="posts-container">
            <div class="custom-pull-refresh"
                 :style="{ transform: `translateY(${pullDownDistance}px)`, transition: pullDownDistance ? 'none' : 'transform 0.3s' }"
                 @touchstart="handleTouchStart"
                 @touchmove.passive="handleTouchMove"
                 @touchend="handleTouchEnd"
                 @touchcancel="handleTouchEnd">
              <div class="refresh-indicator" :class="{ 'refreshing': isRefreshing, 'pulled': pullDownDistance >= refreshThreshold }"
                   :style="{ opacity: Math.min(pullDownDistance / refreshThreshold, 1) }">
                <div class="refresh-icon">
                  <img :src="emptyImage" class="refresh-image" :class="{ 'spinning': isRefreshing }"
                       :style="{ transform: isRefreshing ? '' : `scale(${Math.min(1, pullDownDistance / refreshThreshold)}) rotate(${(pullDownDistance / refreshThreshold) * 360}deg)` }" />
                </div>
                <span class="refresh-text">{{ isRefreshing ? '刷新中...' : pullDownDistance >= refreshThreshold ? '释放刷新' : '下拉刷新' }}</span>
              </div>
              <post-list
                class="post-list-container"
                :loading="isInitialLoading"
                :data-list="posts"
              />
            </div>
          </div>
          <div v-if="isLoadingMore && posts.length > 0" class="loading-more">
            <i class="fas fa-spinner fa-spin loading-icon"></i>
            <span>加载中...</span>
          </div>
          <div v-if="!hasMore && posts.length > 0" class="no-more-data">
            没有更多数据了~
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, onUnmounted, onActivated, onDeactivated, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { listPostByPageUsingPost, getFollowPostsUsingPost, listPostTagCategoryUsingGet } from '@/api/postController'
import { getFollowOrFanListUsingPost } from '@/api/userFollowsController'
import PostList from '@/components/PostList.vue'
import { POST_STATUS_ENUM } from '@/constants/post'
import { message } from 'ant-design-vue'
import { throttle, debounce } from 'lodash-es'
import { getDefaultAvatar } from '@/utils/userUtils'
import { DEVICE_TYPE_ENUM } from '@/constants/device'
import { getDeviceType } from '@/utils/device'
import { AppstoreOutlined, DownOutlined } from '@ant-design/icons-vue'

import springBanner from '@/assets/season/spring.png'
import summerBanner from '@/assets/season/summer.png'
import autumnBanner from '@/assets/season/autumn.png'
import winterBanner from '@/assets/season/winter.png'
import emptyImage from '@/assets/illustrations/empty.png'

const router = useRouter()
const loginUserStore = useLoginUserStore()

const device = ref<string>('')
const activeTab = ref('all')
const selectedCategory = ref('最新')
const categories = ref<string[]>(['最新', '精选', '摄影', '修图', '构图', '光影', '器材', '手机', '单反', '微距', '布光', '风光', '调色', '滤镜', '抠图', '合成', '胶片'])
const posts = ref<Post[]>([])
const followUsers = ref<any[]>([])

// PC 端分类处理
const topCategories = computed(() => categories.value.slice(0, 8))
const remainingCategories = computed(() => categories.value.slice(8))
const showMoreCategories = ref(false)
const moreCategoriesVisible = ref(false)

const isInitialLoading = ref(false)
const isLoadingMore = ref(false)
const loading = computed(() => isInitialLoading.value || isLoadingMore.value)
const followUsersLoading = ref(false)
const hasMore = ref(true)
const isLoadingLocked = ref(false)

const currentPage = ref(1)
const pageSize = ref(10)
const isDesktop = computed(() => device.value === DEVICE_TYPE_ENUM.PC)

const getDeviceTypeAsync = async () => await getDeviceType()

const isRefreshing = ref(false)
const pullDownDistance = ref(0)
const startY = ref(0)
const isTouching = ref(false)
const refreshThreshold = 80
const isAtTop = ref(true)
const scrollPosition = ref(0)

const showCategoryDropdown = ref(false)

const bannerImages = { spring: springBanner, summer: summerBanner, autumn: autumnBanner, winter: winterBanner }
const getCurrentSeason = () => {
  const month = new Date().getMonth()+1
  return month >=3 && month <=5 ? 'spring' : month >=6 && month <=8 ? 'summer' : month >=9 && month <=11 ? 'autumn' : 'winter'
}
const currentSeason = ref(getCurrentSeason())
const currentSeasonBanner = ref(bannerImages[currentSeason.value])
const bannerLoaded = ref(false)

const fetchCategories = async () => {
  try {
    const res = await listPostTagCategoryUsingGet()
    if (res.data?.code === 0 && res.data.data) {
      categories.value = ['最新', '精选', ...(res.data.data.categoryList || [])]
    }
  } catch (error) {
    console.error('获取分类失败:', error)
    message.error('获取分类列表失败')
  }
}

const fetchDiscoveryPosts = async (reset = false) => {
  if (isLoadingLocked.value) return
  isLoadingLocked.value = true

  if (reset) {
    currentPage.value = 1
    posts.value = []
    hasMore.value = true
    isInitialLoading.value = true
  } else {
    isLoadingMore.value = true
  }

  try {
    const res = await listPostByPageUsingPost({
      sortField: 'createTime',
      sortOrder: 'desc',
      current: currentPage.value,
      pageSize: pageSize.value,
      status: POST_STATUS_ENUM.PASS,
      category: (selectedCategory.value === 'all' || selectedCategory.value === '最新' || selectedCategory.value === '推荐') ? undefined : selectedCategory.value
    })

    if (res.data?.data?.records) {
      const newPosts = res.data.data.records
      posts.value = reset ? newPosts : [...posts.value, ...newPosts.filter(n => !posts.value.some(o => o.id === n.id))]
      hasMore.value = newPosts.length === pageSize.value
    } else {
      hasMore.value = false
    }
  } catch (error) {
    console.error('获取帖子失败:', error)
    message.error('获取帖子列表失败')
    hasMore.value = false
  } finally {
    isInitialLoading.value = false
    isLoadingMore.value = false
    isLoadingLocked.value = false
  }
}

const fetchFollowPosts = async (reset = false) => {
  if (isLoadingLocked.value) return
  isLoadingLocked.value = true

  if (reset) {
    currentPage.value = 1
    posts.value = []
    hasMore.value = true
    isInitialLoading.value = true
  } else {
    isLoadingMore.value = true
  }

  try {
    const res = await getFollowPostsUsingPost({
      current: currentPage.value,
      pageSize: pageSize.value,
      sortField: 'createTime',
      sortOrder: 'desc'
    })

    if (res.data?.data?.records) {
      const newPosts = res.data.data.records
      posts.value = reset ? newPosts : [...posts.value, ...newPosts.filter(n => !posts.value.some(o => o.id === n.id))]
      hasMore.value = newPosts.length === pageSize.value
    } else {
      hasMore.value = false
    }
  } catch (error) {
    console.error('获取关注帖子失败:', error)
    message.error('获取关注帖子失败')
    hasMore.value = false
  } finally {
    isInitialLoading.value = false
    isLoadingMore.value = false
    isLoadingLocked.value = false
  }
}

const fetchFollowUsers = async () => {
  if (followUsersLoading.value) return
  followUsersLoading.value = true

  try {
    const res = await getFollowOrFanListUsingPost({
      current: 1,
      pageSize: 20,
      searchType: 0,
      followerId: loginUserStore.loginUser?.id
    })

    if (res.data?.code === 0 && res.data.data?.records) {
      followUsers.value = res.data.data.records
    }
  } catch (error) {
    console.error('获取关注用户失败:', error)
  } finally {
    followUsersLoading.value = false
  }
}

const loadMore = async () => {
  if (isLoadingLocked.value || isInitialLoading.value || isRefreshing.value || isLoadingMore.value || !hasMore.value) return

  currentPage.value++
  activeTab.value === 'all' ? await fetchDiscoveryPosts(false) : await fetchFollowPosts(false)
}

const handleRefresh = async () => {
  if (isRefreshing.value) return

  isRefreshing.value = true
  pullDownDistance.value = refreshThreshold

  try {
    if (activeTab.value === 'all') {
      await fetchDiscoveryPosts(true)
    } else {
      await fetchFollowPosts(true)
      await fetchFollowUsers()
    }
  } catch (error) {
    message.error('刷新失败，请稍后重试')
  } finally {
    isRefreshing.value = false
    pullDownDistance.value = 0

    setTimeout(() => {
      const headerEl = document.querySelector('.mobile-header') || document.querySelector('.nav-category-wrapper')
      if (headerEl) {
        headerEl.style.display = 'none'
        void headerEl.offsetWidth
        headerEl.style.display = ''
      }

      window.dispatchEvent(new Event('resize'))
      window.dispatchEvent(new Event('scroll'))
    }, 100)
  }
}

const handleScroll = throttle(() => {
  const scrollTop = window.pageYOffset || document.documentElement.scrollTop
  isAtTop.value = scrollTop <= 0

  const clientHeight = window.innerHeight
  const scrollHeight = document.documentElement.scrollHeight

  if ((scrollTop + clientHeight) >= (scrollHeight - 100)) {
    loadMore()
  }

  // 增加滚动时自动关闭分类弹窗（移动端体验优化）
  if (showCategoryDropdown.value && scrollTop > 10) {
    showCategoryDropdown.value = false;
  }

  scrollPosition.value = scrollTop
}, 200)

const handleTouchStart = (e: TouchEvent) => {
  if (isDesktop.value) return

  if (isAtTop.value) {
    startY.value = e.touches[0].clientY
    isTouching.value = true
    pullDownDistance.value = 0
  }
}

const handleTouchMove = (e: TouchEvent) => {
  if (!isTouching.value || isDesktop.value || isRefreshing.value) return

  const distance = e.touches[0].clientY - startY.value
  if (distance > 0) {
    pullDownDistance.value = Math.min(distance / 2, 120)
    if (e.cancelable) e.preventDefault()
  } else {
    pullDownDistance.value = 0
  }
}

const handleTouchEnd = () => {
  if (!isTouching.value || isDesktop.value) return

  isTouching.value = false
  if (pullDownDistance.value >= refreshThreshold) {
    handleRefresh()
  } else {
    pullDownDistance.value = 0
  }
}

const handleTabChange = (key: string) => {
  if (activeTab.value === key) return

  activeTab.value = key
  currentPage.value = 1
  posts.value = []
  hasMore.value = true

  window.scrollTo({ top: 0, behavior: 'auto' })
  key === 'following' ? (fetchFollowPosts(true), fetchFollowUsers()) : fetchDiscoveryPosts(true)
}

const handleCategoryChange = (category: string) => {
  if (selectedCategory.value === category) return

  selectedCategory.value = category
  fetchDiscoveryPosts(true)
  window.scrollTo({ top: 0, behavior: 'auto' })
}

const handleMobileCategoryChange = (category: string) => {
  if (selectedCategory.value === category) {
    showCategoryDropdown.value = false;
    return;
  }
  selectedCategory.value = category;
  showCategoryDropdown.value = false;

  // 置顶逻辑
  const index = categories.value.indexOf(category);
  if (index > -1) {
    const item = categories.value.splice(index, 1)[0];
    categories.value.unshift(item);
  }

  fetchDiscoveryPosts(true);
  window.scrollTo({ top: 0, behavior: 'smooth' });
};

const toggleCategoryDropdown = () => {
  showCategoryDropdown.value = !showCategoryDropdown.value;
};

// 废弃 PC 弹窗逻辑
// const toggleCategoryDropdown = () => {
//   showCategoryDropdown.value = !showCategoryDropdown.value
// }

const handleSearchClick = () => {
  router.push({ path: '/search', query: { type: 'post' } })
}

const saveScrollPosition = () => {
  scrollPosition.value = window.pageYOffset || document.documentElement.scrollTop
}

const restoreScrollPosition = () => {
  window.scrollTo({ top: scrollPosition.value, behavior: 'auto' })
}

const handleResize = debounce(() => {}, 200)

onMounted(async () => {
  currentSeason.value = getCurrentSeason()
  currentSeasonBanner.value = bannerImages[currentSeason.value]

  device.value = await getDeviceType()
  await fetchCategories()
  await fetchDiscoveryPosts(true)

  window.addEventListener('scroll', handleScroll)
  window.addEventListener('resize', handleResize)
  const container = document.getElementById('forumPage')
  if (container) {
    container.addEventListener('touchstart', handleTouchStart)
    container.addEventListener('touchmove', handleTouchMove)
    container.addEventListener('touchend', handleTouchEnd)
  }
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
  window.removeEventListener('resize', handleResize)
  const container = document.getElementById('forumPage')
  if (container) {
    container.removeEventListener('touchstart', handleTouchStart)
    container.removeEventListener('touchmove', handleTouchMove)
    container.removeEventListener('touchend', handleTouchEnd)
  }
})

onActivated(async () => {
  device.value = await getDeviceTypeAsync()
  restoreScrollPosition()
  const container = document.getElementById('forumPage')
  if (container) {
    container.addEventListener('touchstart', handleTouchStart)
    container.addEventListener('touchmove', handleTouchMove)
    container.addEventListener('touchend', handleTouchEnd)
  }
  window.addEventListener('scroll', handleScroll)
})

onDeactivated(() => {
  saveScrollPosition()
  const container = document.getElementById('forumPage')
  if (container) {
    container.removeEventListener('touchstart', handleTouchStart)
    container.removeEventListener('touchmove', handleTouchMove)
    container.removeEventListener('touchend', handleTouchEnd)
  }
  window.removeEventListener('scroll', handleScroll)
})

interface Post {
  id: number; title: string; content: string; userId: number; category: string;
  createTime: string; updateTime: string; status: number; [key: string]: any
}
</script>

<style scoped>
#forumPage {
  min-height: 100vh;
  padding: 0;
  overflow-x: hidden;
  max-width: 1400px;
  margin: 0 auto;
}

.page-container {
  width: 100%;
  min-height: 100vh;
  overflow: auto;
  padding: 12px 0;
}

.content-wrapper {
  margin: 0 auto;
  width: 100%;
  padding: 0;
}

.mobile-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 999;
  color: var(--text-other);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  background: var(--ios-bg-blur);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
}

@media screen and (max-width: 768px) {
  .content-main {
    padding-top: 102px;
  }
}

.mobile-nav-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 48px;
  padding: 0 16px;
}

.tab-container {
  display: flex;
  flex: 1;
  justify-content: center;
  gap: 40px;
}

.mobile-tab-item {
  font-size: 17px;
  font-weight: 500;
  color: #666;
  cursor: pointer;
  position: relative;
  padding: 0 8px;
  height: 100%;
  display: flex;
  align-items: center;
  transition: color 0.2s ease;
}

.mobile-tab-item.active {
  color: #0066B3;
}

.mobile-search {
  padding-top: 0;
  background: var(--header-background);
  color: var(--text-primary);
  position: relative;
  z-index: 0;
  transition: all 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.mobile-search-fixed {
  z-index: 4;
  position: relative !important;
  width: 96px !important;
  height: 32px !important;
  padding: 0 4px !important;
  display: flex !important;
  align-items: center !important;
  justify-content: flex-end !important;
  background: transparent !important;
  transform: translateY(0) !important;
  opacity: 1 !important;
}

.search-button {
  border: none;
  background: rgba(37, 99, 235, 0.1);
  border-radius: 16px;
  width: 100%;
  height: 32px;
  transition: all 0.3s ease;
  padding: 0 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  position: relative;
  overflow: hidden;
}

.search-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.2);
}

.search-button:hover .search-icon {
  transform: rotate(-5deg) scale(1.1);
}

.search-button:hover .search-text {
  transform: translateX(2px);
}

.search-button:active {
  transform: translateY(0);
  box-shadow: 0 1px 4px rgba(37, 99, 235, 0.1);
}

.search-content {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  position: relative;
  z-index: 1;
}

.search-icon {
  color: #2563eb;
  font-size: 16px;
  opacity: 0.8;
  transition: all 0.3s ease;
}

.search-divider {
  color: rgba(37, 99, 235, 0.3);
  font-size: 14px;
  transform: scale(0.9);
  margin: 0 -1px;
}

.search-text {
  color: #2563eb;
  font-size: 13px;
  opacity: 0.8;
  transition: all 0.3s ease;
  font-weight: 500;
}

.mobile-category-nav {
  position: relative;
  width: 100%;
}

.mobile-category-bar-wrapper {
  padding: 8px 0 10px 0;
  background: transparent;
  width: 100%;
  display: flex;
  align-items: center;
}

.mobile-category-bar {
  flex: 1;
  display: flex;
  overflow-x: auto;
  padding: 0 16px;
  gap: 8px;
  scrollbar-width: none;
  -ms-overflow-style: none;
  /* 增加遮罩渐变提示可以滚动 */
  mask-image: linear-gradient(to right, black 85%, transparent 100%);
  -webkit-mask-image: linear-gradient(to right, black 85%, transparent 100%);
}

.mobile-category-bar::-webkit-scrollbar {
  display: none;
}

.mobile-category-item {
  flex-shrink: 0;
  padding: 6px 16px;
  border-radius: 18px;
  font-size: 14px;
  color: #666;
  background: rgba(0, 0, 0, 0.04);
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  white-space: nowrap;
  border: 1px solid transparent;
}

.mobile-category-item.active {
  background: #0066B3;
  color: white;
  font-weight: 500;
  box-shadow: 0 4px 10px rgba(0, 102, 179, 0.25);
  transform: scale(1.05);
}

.mobile-category-item:active {
  transform: scale(0.95);
}

.category-expand-btn {
  width: 44px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--ios-bg-blur);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  z-index: 10;
  margin-right: 4px;
  border-radius: 8px;
  color: #888;
  box-shadow: -10px 0 15px -5px var(--background);
}

.category-expand-btn .fas {
  transition: transform 0.3s ease;
  font-size: 14px;
}

.category-expand-btn .rotated {
  transform: rotate(180deg);
}

.category-dropdown-overlay {
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  right: 0;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(4px);
  z-index: 1000;
}

.category-dropdown-panel {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  background: var(--background);
  border-radius: 0 0 32px 32px;
  z-index: 2000; /* 提升层级，确保遮盖所有头部 */
  padding: 20px 20px 32px 20px;
  padding-top: max(20px, env(safe-area-inset-top)); /* 适配刘海屏 */
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
}

.dropdown-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 0 8px;
}

.dropdown-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: 0.5px;
}

.close-btn-wrapper {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.05);
  border-radius: 50%;
  color: #999;
  font-size: 14px;
}

.dropdown-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  max-height: 60vh;
  overflow-y: auto;
}

.dropdown-category-item {
  padding: 8px 0;
  text-align: center;
  background: rgba(0, 0, 0, 0.04);
  border-radius: 12px;
  font-size: 13px;
  color: #666;
  border: 1px solid transparent;
  transition: all 0.2s;
}

.dropdown-category-item.active {
  background: #f0f7ff;
  color: #0066B3;
  border-color: #0066B3;
  font-weight: 500;
}

/* 过渡动画 */
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

.slide-down-enter-active, .slide-down-leave-active { transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1); }
.slide-down-enter-from, .slide-down-leave-to { transform: translateY(-100%); opacity: 0.5; }



.follow-users-loading, .follow-users-empty {
  padding: 0;
  text-align: center;
  color: #999;
  font-size: 13px;
}

.follow-users-container {
  width: 100%;
  overflow-x: auto;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.follow-users-container::-webkit-scrollbar {
  display: none;
}

.follow-users-list {
  display: flex;
  gap: 12px;
}

.follow-user-item-horizontal {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #ffffff;
  padding: 5px 15px 5px 5px;
  border-radius: 40px;
  border: 1px solid #eee;
  flex-shrink: 0;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.04);
  transition: transform 0.2s ease;
}

.follow-user-item-horizontal:active {
  transform: scale(0.95);
  background: #f9f9f9;
}

.follow-user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid #fff;
}

.follow-user-name {
  font-size: 13px;
  color: #333;
  font-weight: 500;
  max-width: 90px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.view-more-item {
  padding: 5px;
  width: 46px;
  justify-content: center;
}

.view-more-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #f0f2f5;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #888;
}

.dropdown-category-item.active {
  background: #0066B3;
  color: white;
}

/* PC 端 Apple 风格极简顶部 */
.hero-banner-section {
  position: relative;
  width: 100%;
  height: 120px; /* 调整高度为 120px */
  margin-bottom: 24px;
  overflow: hidden;
  border-radius: 20px;
  background: #fdfdfd;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06);
}

.banner-container {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
}

.hero-banner-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0;
  transition: opacity 1s ease, transform 1s ease-out;
}

.hero-banner-img.loaded {
  opacity: 1;
}

.banner-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(180deg, rgba(0,0,0,0) 0%, rgba(0,0,0,0.2) 100%);
  z-index: 2;
}

.hero-content {
  position: relative;
  z-index: 3;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 32px;
}

.glass-nav-container {
  background: rgba(255, 255, 255, 0.55);
  backdrop-filter: blur(32px) saturate(180%);
  -webkit-backdrop-filter: blur(32px) saturate(180%);
  border: 1px solid rgba(255, 255, 255, 0.4);
  border-radius: 28px;
  padding: 10px 20px;
  display: flex;
  align-items: center;
  gap: 20px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
  max-width: 95%;
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}

.nav-upper {
  flex-shrink: 0;
  border-right: 1px solid rgba(0, 0, 0, 0.08);
  padding-right: 20px;
}

.tab-pill-group {
  display: flex;
  background: rgba(0, 0, 0, 0.04);
  padding: 4px;
  border-radius: 14px;
  gap: 2px;
}

.tab-pill {
  padding: 6px 18px;
  border-radius: 10px;
  color: #666;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s ease;
}

.tab-pill:hover {
  color: #000;
}

.tab-pill.active {
  background: #fff;
  color: #000;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.nav-lower {
  flex: 1;
  overflow: hidden;
}

.pc-category-dashboard {
  width: 100%;
}

.category-grid-inline {
  display: flex;
  align-items: center;
  gap: 8px;
  overflow: hidden;
}

.category-item-pill {
  flex-shrink: 0;
  padding: 6px 14px;
  border-radius: 12px;
  color: #444;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.25s ease;
  user-select: none;
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(0, 0, 0, 0.02);
}

.category-item-pill:hover {
  background: rgba(255, 255, 255, 0.8);
  transform: translateY(-1px);
}

.category-item-pill.active {
  background: #000;
  color: #fff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.more-trigger {
  display: flex;
  align-items: center;
  gap: 4px;
  background: rgba(0, 0, 0, 0.03);
  color: #666;
}

.more-trigger:hover {
  background: rgba(0, 0, 0, 0.06);
}

/* Apple 风格弹出面板 */
:global(.apple-category-popover .ant-popover-inner) {
  padding: 16px;
  border-radius: 20px !important;
  background: rgba(255, 255, 255, 0.85) !important;
  backdrop-filter: blur(40px) saturate(200%) !important;
  -webkit-backdrop-filter: blur(40px) saturate(200%) !important;
  border: 1px solid rgba(255, 255, 255, 0.4) !important;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.12) !important;
}

:global(.apple-category-popover .ant-popover-arrow) {
  display: none !important;
}

.more-category-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  min-width: 240px;
  max-height: 300px; /* 设置最大高度 */
  overflow-y: auto; /* 内容过多时上下滚动 */
  padding-right: 4px; /* 为滚动条留出一点空间 */
}

.more-category-item {
  padding: 10px 14px;
  border-radius: 12px;
  color: #555;
  font-size: 13px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s ease;
  background: rgba(0, 0, 0, 0.02);
}

.more-category-item:hover {
  background: rgba(0, 0, 0, 0.05);
  color: #000;
}

.more-category-item.active {
  background: #000;
  color: #fff;
}

.pc-follow-list {
  display: flex;
  align-items: center;
  gap: 12px;
}

.pc-follow-user {
  cursor: pointer;
  transition: transform 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

.mini-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 2px solid white;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.add-follow {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1.5px dashed #ccc;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
}

.add-follow:hover {
  border-color: #000;
  color: #000;
}

/* 移动端样式兼容 */
@media screen and (max-width: 768px) {
  .page-container { padding: 0; }
  .content-main { padding-top: 102px; }
}

.posts-container {
  margin: 0 2px;
}

.post-list-container {
  min-height: calc(100vh - 300px);
}

.custom-pull-refresh {
  position: relative;
  width: 100%;
}

.refresh-indicator {
  position: absolute;
  left: 0;
  right: 0;
  top: -60px;
  height: 60px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.refresh-icon {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.refresh-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
  transition: transform 0.1s linear;
}

.refresh-image.spinning {
  animation: refreshSpin 1s linear infinite;
}

@keyframes refreshSpin {
  from { transform: rotate(0deg) scale(1); }
  50% { transform: rotate(180deg) scale(1.1); }
  to { transform: rotate(360deg) scale(1); }
}

.refresh-text {
  font-size: 11px;
  color: #999;
  font-weight: 500;
}

.empty-following {
  padding: 100px 0;
  text-align: center;
}

.custom-empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  animation: emptyFadeIn 0.8s ease-out;

  .empty-illustration {
    width: 160px;
    height: auto;
    opacity: 0.8;
    margin-bottom: 8px;
  }

  .empty-desc {
    font-size: 16px;
    color: var(--text-primary);
    margin-bottom: 4px;
    font-weight: 500;
  }

  .empty-subdesc {
    font-size: 13px;
    color: var(--text-secondary);
    margin-bottom: 16px;
  }
}

.go-discover-btn {
  border-radius: 20px;
  padding: 0 24px;
}

@keyframes emptyFadeIn {
  from { opacity: 0; transform: translateY(15px); }
  to { opacity: 1; transform: translateY(0); }
}

.loading-more, .no-more-data {
  padding: 40px 0;
  text-align: center;
  color: #999;
  font-size: 14px;
}

/* 极致隐藏滚动条 */
* {
  scrollbar-width: none;
  -ms-overflow-style: none;
}

*::-webkit-scrollbar {
  display: none;
}

.mobile-follow-users-container{
  width: 100%;
  height: 50px;
}

/* =========================================
   Video Dark Mode 增强 (Cinematic Enhancements)
   ========================================= */
:global(.dark-theme) #forumPage {
  --background: #0a0a0a;
  --card-background: #141414;
  --text-primary: #f0f0f0;
  --text-secondary: #94a3b8;
  --border-color: rgba(255, 255, 255, 0.06);
  --header-background: rgba(10, 10, 10, 0.8);
  --nav-item-active-text: #3b82f6;
  background-color: var(--background);
}

:global(.dark-theme) .mobile-header {
  background: rgba(10, 10, 10, 0.8) !important;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.4);
}

:global(.dark-theme) .mobile-tab-item {
  color: #64748b;
}

:global(.dark-theme) .mobile-tab-item.active {
  color: #3b82f6;
  text-shadow: 0 0 10px rgba(59, 130, 246, 0.4);
}

:global(.dark-theme) .search-button {
  background: rgba(255, 255, 255, 0.05);
}

:global(.dark-theme) .search-icon,
:global(.dark-theme) .search-text {
  color: #3b82f6;
}

:global(.dark-theme) .hero-banner-section {
  background: #0f172a;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.5);
}

:global(.dark-theme) .banner-overlay {
  background: linear-gradient(180deg, rgba(0,0,0,0.2) 0%, rgba(10,10,10,1) 100%);
}

:global(.dark-theme) .glass-nav-container {
  background: rgba(30, 30, 30, 0.6);
  border-color: rgba(255, 255, 255, 0.08);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.6);
}

:global(.dark-theme) .tab-pill-group {
  background: rgba(255, 255, 255, 0.05);
}

:global(.dark-theme) .tab-pill {
  color: #94a3b8;
}

:global(.dark-theme) .tab-pill.active {
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
}

:global(.dark-theme) .category-item-pill {
  background: rgba(255, 255, 255, 0.03);
  border-color: rgba(255, 255, 255, 0.05);
  color: #cbd5e1;
}

:global(.dark-theme) .category-item-pill:hover {
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
}

:global(.dark-theme) .category-item-pill.active {
  background: #3b82f6;
  color: #fff;
  box-shadow: 0 0 15px rgba(59, 130, 246, 0.3);
}

:global(.dark-theme) .category-dropdown-panel {
  background: #1e1e1e;
}

:global(.dark-theme) .dropdown-category-item {
  background: rgba(255, 255, 255, 0.05);
}

:global(.dark-theme) .dropdown-category-item.active {
  background: rgba(59, 130, 246, 0.2);
  border-color: #3b82f6;
  color: #3b82f6;
}

:global(.dark-theme) .category-expand-btn {
  background: rgba(30, 30, 30, 0.8);
  box-shadow: -10px 0 15px -5px #0a0a0a;
}

:global(.dark-theme) .follow-user-item-horizontal {
  background: #1e1e1e;
  border-color: rgba(255, 255, 255, 0.05);
}

:global(.dark-theme) .follow-user-name {
  color: #e2e8f0;
}

:global(.dark-theme) .view-more-avatar {
  background: #262626;
  color: #94a3b8;
}
</style>
