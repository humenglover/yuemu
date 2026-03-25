<template>
  <div id="homePage">
    <!-- 移动端搜索区域 -->
    <div
      v-if="device !== DEVICE_TYPE_ENUM.PC"
      class="mobile-search mobile-search-fixed"
      :class="{ 'mobile-search-transitioning': isSearchTransitioning }"
      @click="handleSearchClick"
    >
      <a-button class="search-button">
        <div class="search-content">
          <i class="fas fa-search search-icon"></i>
          <span class="search-divider">|</span>
          <span class="search-text">搜索</span>
        </div>
      </a-button>
    </div>

    <!-- 移动端顶部导航 -->
    <div v-if="device !== DEVICE_TYPE_ENUM.PC" class="mobile-nav">
      <van-tabs
        v-model:active="activeTab"
        style="width: 76%"
        :duration="0.3"
        title-inactive-color="#8b9eb0"
        title-active-color="#2563eb"
        :line-width="20"
      >
        <van-tab name="all">
          <template #title>
            <div class="tab-content">
              <span>全部</span>
            </div>
          </template>
        </van-tab>
        <van-tab name="following">
          <template #title>
            <div class="tab-content">
              <span>关注</span>
            </div>
          </template>
        </van-tab>
        <van-tab name="ranking">
          <template #title>
            <div class="tab-content">
              <span>榜单</span>
            </div>
          </template>
        </van-tab>
      </van-tabs>
      <div class="m-filter-section" v-if="activeTab == 'all'" >
        <div class="mobile-category-nav">
          <div class="mobile-category-bar-wrapper">
            <div class="mobile-category-bar">
              <div
                class="mobile-category-item"
                v-for="category in categoryList"
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
        </div>
      </div>
      <div v-if="activeTab === 'ranking'" class="m-ranking-tabs">
        <div v-for="tab in (rankingTabs as any[])"
             :key="tab.value"
             class="m-ranking-tab-item"
             :class="{ active: rankingType === tab.value }"
             @click="handleRankingTypeChange(tab.value)">
          {{ tab.name }}
        </div>
      </div>
    </div>

    <!-- 移动端顶部避空占位 (防止 Fixed 导航遮挡) -->
    <div v-if="device !== DEVICE_TYPE_ENUM.PC && (activeTab !== 'all' || (carouselActivities && carouselActivities.length === 0))"
         :style="{ height: activeTab == 'following' ? '74px' : activeTab == 'ranking' ? '92px' : 0, width: '100%' }"></div>

    <!-- 关注用户头像叠层 (移动端内置 - 移出下拉刷新以固化位置) -->
    <div v-if="device !== DEVICE_TYPE_ENUM.PC && activeTab === 'following'" class="m-follow-users-inline">
      <FollowUserList
        :users="followUsers"
        :loading="followUsersLoading"
        mode="stack"
        :maxCount="6"
        @item-click="id => $router.push(`/user/${id}`)"
        @more-click="$router.push('/follow-list')"
      />
    </div>

    <div v-if="device !== DEVICE_TYPE_ENUM.PC && activeTab === 'all' " class="activity-carousel">
      <van-swipe class="carousel-container" :autoplay="4000" @change="currentActivityIndex = $event" :height="200">
        <van-swipe-item v-for="activity in (carouselActivities as any[])"
                        :key="(activity as any).id"
                        class="carousel-item"
                        @click="handleActivityClick((activity as any).id)">
          <div class="carousel-item-content">
            <img :src="(activity as any).coverUrl"
                 :alt="(activity as any).title"
                 class="carousel-image"
                 loading="lazy" />
            <div class="carousel-info">
              <h3 class="carousel-title">{{ (activity as any).title }}</h3>
              <div class="carousel-meta">
                <span class="carousel-status"
                      :class="{ 'expired': (activity as any).isExpired === 1 }">
                  {{ (activity as any).isExpired === 1 ? '已结束' : '进行中' }}
                </span>
                <span class="carousel-date">{{ formatTime((activity as any).expireTime) }}截止</span>
              </div>
            </div>
          </div>
        </van-swipe-item>
      </van-swipe>
    </div>

    <div v-if="device === DEVICE_TYPE_ENUM.PC">
      <!-- PC端顶部布局：全宽轮播图 -->
      <div class="pc-top-layout-full">
        <div class="pc-carousel-section-full">
          <CustomPcCarousel
            :activities="carouselActivities"
            :autoplay="true"
            :autoplay-interval="4000"
            @activity-click="handleActivityClick"
            @image-error="handleImageError"
          />
        </div>
      </div>

      <!-- V3 新增：全宽层级化过滤中心 -->
      <div class="pc-filter-center">
        <!-- 第一层：主切客 与 刷新控制 -->
        <div class="main-tabs-toolbar-v3">
          <div class="main-tabs-row">
            <div class="main-tab-item" :class="{ active: activeTab === 'all' }" @click="activeTab = 'all'">全部作品</div>
            <div class="main-tab-item" :class="{ active: activeTab === 'following' }" @click="activeTab = 'following'">我的关注</div>
            <div class="main-tab-item" :class="{ active: activeTab === 'ranking' }" @click="activeTab = 'ranking'">风云榜单</div>
          </div>
          <!-- 优雅纯净版发光“能量藤蔓”独立组件 -->
          <GlowingVine />

          <div class="pc-refresh-control-v3">
            <a-button type="text" class="pc-refresh-btn" @click="onRefresh" :loading="loading">
              <template #icon><ReloadOutlined :class="{ 'spin-active': loading }" /></template>
              换一换
            </a-button>
          </div>
        </div>

        <!-- 第二层：次级过滤器 -->
        <div class="sub-filter-row">
          <!-- 全部状态下的分类云 -->
          <div v-if="activeTab === 'all'" class="pc-category-bar-v3">
            <div class="category-wrapper-v3" :class="{ 'is-expanded': isCategoryExpanded }">
              <div class="category-pill-v3" v-for="category in displayedCategoryList" :key="category" :class="{ active: selectedCategory === category }" @click="() => handleCategoryClick(category)">{{ category }}</div>
              <div v-if="categoryList.length > 28" class="category-pill-v3 more-btn" @click="isCategoryExpanded = !isCategoryExpanded">
                {{ isCategoryExpanded ? '收起' : '更多分类' }}
                <i class="fas" :class="isCategoryExpanded ? 'fa-chevron-up' : 'fa-chevron-down'" style="margin-left: 6px;"></i>
              </div>
            </div>
          </div>

          <div v-if="activeTab === 'ranking'" class="pc-ranking-bar-v3">
            <div class="ranking-wrapper-v3">
              <div v-for="tab in (rankingTabs as any[])"
                   :key="tab.value"
                   class="ranking-item-v3"
                   :class="{ active: rankingType === tab.value }"
                   @click="handleRankingTypeChange(tab.value)">{{ (tab as any).name }}</div>
            </div>
          </div>

          <!-- 关注状态下的二级过滤器：组件封装版 -->
          <div v-if="activeTab === 'following'" class="pc-following-bar-v3">
            <FollowUserList
              :users="followUsers"
              mode="stack"
              :maxCount="12"
              @item-click="id => $router.push(`/user/${id}`)"
              @more-click="$router.push('/follow-list')"
            />
          </div>
        </div>
      </div>

      <!-- PC端瀑布流组件（复用移动端数据） -->
      <div class="pc-waterfall-section">
        <WaterfallPictureList
          :dataList="mobileDataList"
          :loading="loading"
        />
        <!-- 加载更多提示 -->
        <div v-if="!isEndOfData && !loading && mobileDataList.length > 0" class="pc-loading-more">
          <svg class="loading-camera" viewBox="0 0 100 100">
            <path class="camera-body" d="M25,30H75a8,8,0,0,1,8,8V70a8,8,0,0,1-8,8H25a8,8,0,0,1-8-8V38A8,8,0,0,1,25,30Zm5-10H70a2,2,0,0,1,2,2v4a2,2,0,0,1-2,2H30a2,2,0,0,1-2-2V22A2,2,0,0,1,30,20Z"/>
            <circle class="camera-lens" cx="50" cy="54" r="15"/>
            <circle class="camera-flash" cx="72" cy="42" r="4"/>
          </svg>
        </div>
        <!-- 没有更多数据 -->
        <div v-if="isEndOfData && mobileDataList.length > 0" class="pc-no-more-data">
          <img src="../assets/add.png" style="width: 36px;" alt="logo" class="logo-image" />没有更多数据了哦~
        </div>
      </div>
    </div>
    <div v-else>
      <div class="custom-pull-refresh"
           :style="[
             { transform: `translateY(${refreshDistance}px)`, transition: refreshDistance ? 'none' : 'transform 0.3s' }
           ]"
           @touchstart="handleTouchStart"
           @touchmove="handleTouchMove"
           @touchend="handleTouchEnd"
           @touchcancel="handleTouchEnd">

        <!-- 刷新指示器 -->
        <div class="refresh-indicator" :class="{ 'refreshing': isRefreshing, 'pulled': refreshDistance >= refreshThreshold }"
             :style="{ opacity: Math.min(refreshDistance / refreshThreshold, 1) }">
          <div class="refresh-icon">
            <i v-if="isRefreshing" class="fas fa-spinner fa-spin loading-icon"></i>
            <div v-else class="pull-arrow" :style="{ transform: `rotate(${Math.min(180, (refreshDistance / refreshThreshold) * 180)}deg)` }">
              ↓
            </div>
          </div>
          <span class="refresh-text">{{ isRefreshing ? '刷新中...' : refreshDistance >= refreshThreshold ? '释放刷新' : '下拉刷新' }}</span>
        </div>

        <div class="mobile-list-container" style="margin-top: 0">
          <!-- 修改关注页面空状态 -->
          <div v-if="activeTab === 'following' && mobileDataList.length === 0 && !loading" class="empty-following">
            <van-empty
              class="custom-empty"
              image="search"
              description="暂无关注内容"
            >
              <template #description>
                <p class="empty-desc">关注感兴趣的创作者，获取第一手图片更新</p>
              </template>
              <template #default>
                <a-button type="primary" class="discover-btn" @click="activeTab = 'all'">
                  去发现
                </a-button>
              </template>
            </van-empty>
          </div>

          <BigPictureList style="margin-top: 4px;" :dataList="mobileDataList" :loading="loading" />

          <!-- 加载更多提示 -->
          <div v-if="!isEndOfData && !loading && mobileDataList.length > 0" class="pc-loading-more">
            <svg class="loading-camera" viewBox="0 0 100 100">
              <path class="camera-body" d="M25,30H75a8,8,0,0,1,8,8V70a8,8,0,0,1-8,8H25a8,8,0,0,1-8-8V38A8,8,0,0,1,25,30Zm5-10H70a2,2,0,0,1,2,2v4a2,2,0,0,1-2,2H30a2,2,0,0,1-2-2V22A2,2,0,0,1,30,20Z"/>
              <circle class="camera-lens" cx="50" cy="54" r="15"/>
              <circle class="camera-flash" cx="72" cy="42" r="4"/>
            </svg>
          </div>
          <div v-if="isEndOfData && mobileDataList.length > 0" class="no-more-data-tip"><img src="../assets/add.png" style="width: 36px;" alt="logo" class="logo-image" />没有更多数据了哦~</div>
        </div>
      </div>
    </div>

    <!-- 移动端分类展开弹窗 (移至最外层以解决 z-index 遮挡问题) -->
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
            v-for="category in categoryList"
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
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, onUnmounted, onActivated, onDeactivated, watch, nextTick, computed } from 'vue'
import {
  getFollowPictureUsingPost,
  getTop100PictureUsingGet,
  listPictureTagCategoryUsingGet,
  listPictureVoByPageUsingPost,
  getFeaturePictureUsingPost
} from '@/api/pictureController'
import { message } from 'ant-design-vue'
import { getDeviceType } from '@/utils/device'
import { DEVICE_TYPE_ENUM } from '@/constants/device'
import { useRoute, useRouter } from 'vue-router'
import { SearchOutlined, PlusOutlined, UploadOutlined, CalendarOutlined, TrophyOutlined, HeartOutlined, ClockCircleOutlined, StarOutlined, FireOutlined, AppstoreOutlined, LinkOutlined, CameraOutlined, UserOutlined, HomeOutlined, HistoryOutlined, RedditOutlined, BranchesOutlined, BorderOuterOutlined, ContactsOutlined, ApartmentOutlined, SmileOutlined } from '@ant-design/icons-vue'
import { debounce } from 'lodash-es'
import BigPictureList from '@/components/BigPictureList.vue'
import WaterfallPictureList from '@/components/WaterfallPictureList.vue'
import { listCarouselActivitiesUsingPost } from '@/api/activityController'
import { formatTime } from '@/utils/time'
import { CommentOutlined } from '@ant-design/icons-vue'
import { getFollowOrFanListUsingPost } from '@/api/userFollowsController'
import { getDefaultAvatar } from '@/utils/userUtils.ts'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import CustomPcCarousel from '@/components/CustomPcCarousel.vue'
import FollowUserList from '@/components/FollowUserList.vue'
import GlowingVine from '@/components/GlowingVine.vue'

// 关注用户列表相关状态
const followUsers = ref<any[]>([])
const followUsersLoading = ref(false)
const router = useRouter()
const route = useRoute()
const isEndOfData = ref(false)
const loginUserStore = useLoginUserStore()
const showCategoryDropdown = ref(false)

// 各大 Tab 的状态持久化缓存 (全标签秒开核心)
const tabStates = reactive<Record<string, { list: any[], page: number, isEndOfData: boolean }>>({
  'all': { list: [], page: 1, isEndOfData: false },
  'following': { list: [], page: 1, isEndOfData: false },
  'ranking': { list: [], page: 1, isEndOfData: false }
})

// 设备类型
const device = ref<string>('')

// 标签和分类
const categoryList = ref<string[]>([])
const selectedCategory = ref<string>('最新')
const tagList = ref<string[]>([])
const selectedTagList = ref<boolean[]>([])

// 搜索条件（统一复用）
const searchParams = reactive<API.PictureQueryRequest>({
  current: 1,
  pageSize: 36,
})

// 分页（统一复用移动端分页）
const page = ref(1)

// 统一数据列表（移动端和PC端共用）
const loading = ref(true)
const mobileDataList = ref<API.PictureVO[]>([])
// 添加加载进度状态
const loadingProgress = ref(0)
// 进度条定时器引用（核心新增）
const progressIntervalRef = ref<NodeJS.Timeout | null>(null)

// 榜单
const rankingType = ref<number>(3)
interface RankingTab {
  name: string
  value: number
}
const rankingTabs: RankingTab[] = [
  { name: '日榜', value: 1 },
  { name: '周榜', value: 2 },
  { name: '月榜', value: 3 },
  { name: '总榜', value: 4 }
]

// 统一标签（移动端和PC端共用）
const activeTab = ref('all')

// 轮播图
const carouselActivities = ref<API.Activity[]>([])
const currentActivityIndex = ref(0)
const autoplayInterval = ref(null)
const translateX = ref(0)
const containerWidth = ref(0)


// 分类展开收起逻辑
const isCategoryExpanded = ref(false)
const displayedCategoryList = computed(() => {
  if (isCategoryExpanded.value) return categoryList.value
  return categoryList.value.slice(0, 28)
})

// 搜索过渡
const isSearchTransitioning = ref(false)

// 下拉刷新（仅移动端生效，PC端忽略）
const startY = ref(0)
const currentY = ref(0)
const refreshDistance = ref(0)
const isRefreshing = ref(false)
const maxPullDistance = 100
const refreshThreshold = 80

// 滚动位置
const scrollPosition = ref(0)

// 添加滚动方向判断
const lastScrollTop = ref(0)
const isScrollingDown = ref(true)

// 初始化标志
const isInitialized = ref(false)

// 添加刷新标识（在其他 ref 声明附近）
const isRefreshingFlag = ref(false)
// 添加可见图片跟踪
const visiblePictures = ref<Set<string>>(new Set())

// 分类图标映射
const categoryIconMap = {
  '推荐': 'fas fa-th-large',
  '关注': 'fas fa-heart',
  '榜单': 'fas fa-trophy',
  '风光': 'fas fa-mountain',
  '人文': 'fas fa-user-friends',
  '建筑': 'fas fa-building',
  '纪实': 'fas fa-history',
  '动物': 'fas fa-paw',
  '植物': 'fas fa-leaf',
  '抽象': 'fas fa-cube',
  '人像': 'fas fa-user',
  '城市': 'fas fa-city',
  '漫画感': 'fas fa-theater-masks',
  '风景': 'fas fa-tree',
  '美食': 'fas fa-utensils',
  '时尚': 'fas fa-tshirt',
  '运动': 'fas fa-running',
  '科技': 'fas fa-microchip',
  '旅行': 'fas fa-plane',
  '艺术': 'fas fa-palette',
  '生活': 'fas fa-home',
  '创意': 'fas fa-lightbulb',
  '自然': 'fas fa-feather',
  '星空': 'fas fa-star',
  '海洋': 'fas fa-water',
  '沙漠': 'fas fa-wind'
}

// 获取分类图标CSS类名
const getCategoryIconClass = (category) => {
  // "关注"和"榜单"不显示图标
  if (category === '关注' || category === '榜单') {
    return ''
  }
  return categoryIconMap[category] || 'fas fa-tag'
}

// 核心新增：启动加载进度条
const startLoadingProgress = () => {
  // 先清除已有定时器，避免叠加
  if (progressIntervalRef.value) {
    clearInterval(progressIntervalRef.value)
  }
  loadingProgress.value = 0 // 重置进度
  let progress = 0

  progressIntervalRef.value = setInterval(() => {
    if (progress < 80) {
      progress += 10
      loadingProgress.value = progress
    } else {
      clearInterval(progressIntervalRef.value!)
      progressIntervalRef.value = null
    }
  }, 80)
}

// 核心新增：终止加载进度条（接口返回时调用）
const stopLoadingProgress = () => {
  if (progressIntervalRef.value) {
    clearInterval(progressIntervalRef.value)
    progressIntervalRef.value = null
  }
  loadingProgress.value = 100 // 直接置为100%，表示加载完成
}

// 获取轮播图活动数据
const fetchCarouselActivities = async () => {
  try {
    const res = await listCarouselActivitiesUsingPost({
      pageSize: 36,
      current: 1,
    })
    if (res.data?.code === 0 && res.data.data) {
      carouselActivities.value = res.data.data.records || []
    }
  } catch (error) {
    console.error('获取轮播活动失败:', error)
  }
}

// 获取标签分类选项
const getTagCategoryOptions = async () => {
  const res = await listPictureTagCategoryUsingGet()
  if (res.data.code === 0 && res.data.data) {
    tagList.value = res.data.data.tagList ?? []
    categoryList.value = ['推荐', '最新', '精选', ...(res.data.data?.categoryList || [])]
  }
}

// 获取关注用户列表
const fetchFollowUsers = async () => {
  // 如果已有数据且不是正在刷新，则不重新加载
  if (followUsers.value.length > 0 && !isRefreshingFlag.value) return
  if (followUsersLoading.value) return
  followUsersLoading.value = true
  try {
    const res = await getFollowOrFanListUsingPost({
      current: 1,
      pageSize: 36,
      searchType: 0,
      followerId: loginUserStore.loginUser?.id
    })
    if (res.data?.code === 0 && res.data.data?.records) {
      followUsers.value = res.data.data.records
    }
  } catch (error) {
    console.error('获取关注用户列表失败:', error)
  } finally {
    followUsersLoading.value = false
  }
}

// 统一数据加载函数（移动端和PC端共用）
const fetchData = async () => {
  // 只有在第一页时才显示loading状态
  const isFirstPage = page.value === 1;
  if (isFirstPage) {
    startLoadingProgress() // 启动进度条
  }

  try {
    let res = null
    if (activeTab.value === 'all') {
      if (selectedCategory.value === '精选') {
        // 精选接口统一加载20张
        res = await getFeaturePictureUsingPost({
          current: page.value,
          pageSize: 20
        })
      } else if (selectedCategory.value === '最新') {
        // 最新接口统一加载36张
        res = await listPictureVoByPageUsingPost({
          ...searchParams,
          pageSize: 36,
          sortField: 'createTime',
          sortOrder: 'desc'
        })
      } else {
        // 普通分类接口统一加载36张
        res = await listPictureVoByPageUsingPost({
          ...searchParams,
          pageSize: 36,
          category: (selectedCategory.value === 'all' || selectedCategory.value === '推荐') ? undefined : selectedCategory.value
        })
      }
    } else if (activeTab.value === 'following') {
      // 关注接口统一加载36张
      res = await getFollowPictureUsingPost({
        ...searchParams,
        current: page.value,
        pageSize: 36
      })
    } else if (activeTab.value === 'ranking') {
      res = await getTop100PictureUsingGet({ id: rankingType.value })
      isEndOfData.value = true // 榜单数据一次性加载，无分页
    }

    // 接口返回成功，立即终止进度条
    stopLoadingProgress()

    if (res?.data.code === 0) {
      const newData = activeTab.value === 'ranking'
        ? res.data.data || []
        : res.data.data?.records || []

      // 处理图片加载状态
      const processedData = newData.map((item) => ({
        ...item,
        loaded: false,
        imageLoaded: false,
        avatarLoaded: false
      }))

      // 分页加载：追加数据；切换标签/分类：替换数据
      // 修改这里：当下拉刷新时也替换数据而不是追加
      if (page.value > 1 && activeTab.value !== 'ranking' && !isRefreshingFlag.value) {
        mobileDataList.value = [...mobileDataList.value, ...processedData]
      } else {
        mobileDataList.value = processedData
      }

      // 判断是否到底（榜单无分页，已在上面设置isEndOfData）
      if (activeTab.value !== 'ranking') {
        // 根据不同接口判断是否到底
        const pageSize = selectedCategory.value === '精选' ? 20 : 36
        isEndOfData.value = newData.length < pageSize
      }
    }
  } catch (error) {
    // 接口失败也终止进度条
    stopLoadingProgress()
    console.error('数据加载失败:', error)
  }
}

// 统一触底加载更多（移动端和PC端共用）
const handleScrollLoad = debounce(async () => {
  if (loading.value || isEndOfData.value || activeTab.value === 'ranking') return

  // 只有向下滚动才触发加载
  if (!isScrollingDown.value) return

  page.value += 1
  searchParams.current = page.value
  // 触底加载时不显示全局loading状态
  const originalLoading = loading.value
  loading.value = false
  await fetchData()
  // 恢复原来的loading状态
  loading.value = originalLoading
}, 1000)

// 绑定滚动事件（区分设备，但逻辑统一）
const bindScrollEvent = () => {
  let ticking = false;

  const updateScrollPosition = () => {
    // 使用requestAnimationFrame优化滚动事件计算
    const currentScrollTop = window.pageYOffset || document.documentElement.scrollTop;
    isScrollingDown.value = currentScrollTop > lastScrollTop.value;
    lastScrollTop.value = currentScrollTop;

    // 滚动时自动关闭分类弹窗
    if (showCategoryDropdown.value) {
      showCategoryDropdown.value = false;
    }

    // 提前200px触发加载更多
    const bottomOfWindow = document.documentElement.offsetHeight - document.documentElement.scrollTop - window.innerHeight <= 200;
    if (bottomOfWindow && isScrollingDown.value) {
      handleScrollLoad();
    }

    ticking = false;
  };

  const onScroll = () => {
    if (!ticking) {
      requestAnimationFrame(updateScrollPosition);
      ticking = true;
    }
  };

  window.onscroll = debounce(onScroll, 200);
}

// 自动播放轮播图
const startAutoplay = () => {
  if (autoplayInterval.value) {
    clearInterval(autoplayInterval.value)
  }
  autoplayInterval.value = setInterval(() => {
    if (carouselActivities.value.length > 0) {
      const currentItem = document.querySelector(`.carousel-item:nth-child(${currentActivityIndex.value + 1})`)
      currentItem?.classList.remove('active')
      currentActivityIndex.value = (currentActivityIndex.value + 1) % carouselActivities.value.length
      translateX.value = -(currentActivityIndex.value * containerWidth.value)
      setTimeout(() => {
        const newItem = document.querySelector(`.carousel-item:nth-child(${currentActivityIndex.value + 1})`)
        newItem?.classList.add('active')
      }, 50)
    }
  }, 4000)
}

// 统一初始化函数
const initializeData = async () => {
  if (isInitialized.value) return
  isInitialized.value = true

  // 初始化时启动进度条
  startLoadingProgress()

  // 1. 并行获取公共基础数据
  const baseDataPromise = Promise.all([
    fetchCarouselActivities(),
    getTagCategoryOptions()
  ])

  // 2. 并行加载首屏数据
  const screenDataPromise = fetchData()

  // 3. 如果是关注标签，加载关注用户列表
  const followUsersPromise = activeTab.value === 'following' ? fetchFollowUsers() : Promise.resolve()

  try {
    // 并行执行所有初始化请求
    await Promise.all([baseDataPromise, screenDataPromise, followUsersPromise])
    // 初始化完成终止进度条
    stopLoadingProgress()
  } catch (error) {
    // 初始化失败也终止进度条
    stopLoadingProgress()
    console.error('初始化失败:', error)
    message.error('初始化失败，请刷新页面重试')
  } finally {
    loading.value = false
  }
}

// 生命周期钩子
onMounted(async () => {

  // 必须先获取设备类型
  device.value = await getDeviceType()

  // 执行统一初始化
  await initializeData()

  // 启动轮播
  if (carouselActivities.value.length > 0) {
    startAutoplay()
  }

  // 为第一个轮播项添加active类
  setTimeout(() => {
    document.querySelector('.carousel-item:first-child')?.classList.add('active')
  }, 50)

  // 绑定滚动加载事件（移动端和PC端都绑定，逻辑统一）
  bindScrollEvent()

  // 时间更新定时器
  const timer = setInterval(updateTime, 1000)
  onUnmounted(() => clearInterval(timer))

  // 添加 Intersection Observer 来实现图片懒加载
  if ('IntersectionObserver' in window) {
    const observer = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          const pictureId = entry.target.getAttribute('data-pic-id');
          if (pictureId) {
            visiblePictures.value.add(pictureId);
          }
        }
      });
    }, {
      rootMargin: '200px' // 提前200px加载图片
    });

    // 观察所有图片元素
    const observeImages = () => {
      document.querySelectorAll('[data-pic-id]').forEach(el => {
        observer.observe(el);
      });
    };

    // 初始观察
    observeImages();

    // 在数据更新后重新观察
    watch(mobileDataList, () => {
      nextTick(() => {
        observeImages();
      });
    });
  }
})

onActivated(() => {
  if (device.value !== DEVICE_TYPE_ENUM.PC) {
    nextTick(() => {
      window.scrollTo({ top: scrollPosition.value, behavior: 'instant' })
    })
  }
  // 重新绑定滚动事件
  bindScrollEvent()
})

onDeactivated(() => {
  scrollPosition.value = window.pageYOffset || document.documentElement.scrollTop
  // 解绑滚动事件
  window.onscroll = null
})

onUnmounted(() => {
  if (autoplayInterval.value) {
    clearInterval(autoplayInterval.value)
  }
  window.onscroll = null
  // 清除进度条定时器，防止内存泄漏
  if (progressIntervalRef.value) {
    clearInterval(progressIntervalRef.value)
  }
})

// 监听activeTab变化（统一处理标签切换）
watch(activeTab, async (newTab, oldTab) => {
  // 如果是下拉刷新触发的变更，直接返回避免重复加载
  if (isRefreshingFlag.value) return

  // 1. 保存当前状态到旧 Tab 缓存
  if (oldTab && tabStates[oldTab]) {
    tabStates[oldTab].list = [...mobileDataList.value]
    tabStates[oldTab].page = page.value
    tabStates[oldTab].isEndOfData = isEndOfData.value
  }

  // 2. 尝试从缓存恢复新状态
  if (tabStates[newTab] && tabStates[newTab].list.length > 0) {
    mobileDataList.value = tabStates[newTab].list
    page.value = tabStates[newTab].page
    searchParams.current = tabStates[newTab].page
    isEndOfData.value = tabStates[newTab].isEndOfData
    // 如果是关注标签，静默确保作者列表已加载
    if (newTab === 'following') {
      fetchFollowUsers()
    }
    return // 直接从快照恢复，不发起网络加载
  }

  // 3. 第一次进入该 Tab，执行初始化加载
  page.value = 1
  searchParams.current = 1
  isEndOfData.value = false
  mobileDataList.value = []

  loading.value = true
  startLoadingProgress()

  if (device.value !== DEVICE_TYPE_ENUM.PC) {
    const container = document.querySelector('.mobile-list-container') as HTMLElement
    if (container) {
      container.style.opacity = '0'
      container.style.transform = 'translateY(20px)'
    }
  }

  try {
    await fetchData();
    if (newTab === 'following') {
      fetchFollowUsers()
    }

    if (device.value !== DEVICE_TYPE_ENUM.PC) {
      const container = document.querySelector('.mobile-list-container') as HTMLElement
      if (container) {
        setTimeout(() => {
          container.style.opacity = '1'
          container.style.transform = 'translateY(0)'
        }, 50)
      }
    }
  } catch (error) {
    stopLoadingProgress()
    console.error('切换标签失败:', error)
  } finally {
    setTimeout(() => {
      loading.value = false;
    }, 300);
  }
})

// 修改 selectedCategory 监听器
// 监听分类变化（统一处理）
watch(selectedCategory, async (newCategory) => {
  // 如果是下拉刷新触发的变更，直接返回避免重复加载
  if (isRefreshingFlag.value) return

  // 重置分页
  page.value = 1
  searchParams.current = 1
  isEndOfData.value = false
  // 清空数据列表，确保显示加载状态
  mobileDataList.value = []

  // 立即显示loading状态和启动进度条
  loading.value = true
  startLoadingProgress()

  try {
    await fetchData()
  } catch (error) {
    // 兜底终止进度条
    stopLoadingProgress()
    console.error('切换分类失败:', error)
  } finally {
    // 延迟关闭loading状态，确保用户能看到完成状态
    setTimeout(() => {
      loading.value = false;
    }, 300);
  }
})

// 其他监听器
watch(
  () => route.query.refresh,
  async (newVal) => {
    if (newVal === 'true') {
      page.value = 1
      searchParams.current = 1
      isEndOfData.value = false
      mobileDataList.value = []

      // 启动进度条
      startLoadingProgress()
      try {
        await fetchData()
      } finally {
        // 终止进度条
        stopLoadingProgress()
      }
    }
  },
)

watch(currentActivityIndex, (newIndex) => {
  if (containerWidth.value === 0) {
    containerWidth.value = document.querySelector('.carousel-container')?.clientWidth || 0
  }
  translateX.value = -(newIndex * containerWidth.value)
})

watch(() => route.path, () => {
  isSearchTransitioning.value = false
})

// 监听榜单类型变化（统一处理）
watch(rankingType, async (newType) => {
  // 确保只在榜单标签页内执行
  if (activeTab.value !== 'ranking') return

  // 立即显示loading状态和启动进度条
  loading.value = true
  startLoadingProgress()

  try {
    await fetchData()
  } catch (error) {
    // 兜底终止进度条
    stopLoadingProgress()
    console.error('切换榜单类型失败:', error)
  } finally {
    // 延迟关闭loading状态，确保用户能看到完成状态
    setTimeout(() => {
      loading.value = false;
    }, 300);
  }
})

// 事件处理函数（统一逻辑）
const handleCategoryClick = (category: string) => {
  if (selectedCategory.value === category) return
  selectedCategory.value = category
  window.scrollTo({ top: 0, behavior: 'auto' })
}

const handleMobileCategoryChange = (category: string) => {
  if (selectedCategory.value === category) {
    showCategoryDropdown.value = false;
    return;
  }
  selectedCategory.value = category;
  showCategoryDropdown.value = false;

  // 置顶逻辑：将选中的分类移到数组首位
  const index = categoryList.value.indexOf(category);
  if (index > -1) {
    const item = categoryList.value.splice(index, 1)[0];
    categoryList.value.unshift(item);
  }

  // 触发业务逻辑加载
  page.value = 1;
  searchParams.current = 1;
  fetchData();
  // 平滑滚动回顶部
  window.scrollTo({ top: 0, behavior: 'smooth' });
};

const toggleCategoryDropdown = () => {
  showCategoryDropdown.value = !showCategoryDropdown.value;
};

const handleSearchClick = () => {
  isSearchTransitioning.value = true
  const searchBar = document.querySelector('.search-bar') as HTMLElement
  const searchWrapper = document.querySelector('.search-wrapper') as HTMLElement

  if (searchBar && searchWrapper) {
    searchBar.style.transition = 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)'
    searchBar.style.transform = 'scale(0.9) translateY(-20px)'
    searchBar.style.opacity = '0.8'
    searchWrapper.style.transition = 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)'
    searchWrapper.style.transform = 'scaleY(0.8)'
    searchWrapper.style.opacity = '0.6'
  }

  setTimeout(() => {
    if (searchBar && searchWrapper) {
      searchBar.style.transform = 'scale(0.8) translateY(-40px)'
      searchBar.style.opacity = '0'
      searchWrapper.style.transform = 'scaleY(0)'
      searchWrapper.style.opacity = '0'
    }
    router.push('/search')
  }, 300)
}

const handleRankingTypeChange = (type: number) => {
  rankingType.value = type
}

const handleActivityClick = (id: string) => {
  router.push(`/activity/detail/${id}`)
}

const handleImageError = (e) => {
  e.target.src = 'https://static.yuemutuku.com/public/1910505702887313409/2025-04-13_zK4t9VyYPvzKfLOk.webp'
}


// 统一刷新函数
// 下拉刷新处理函数
const onRefresh = async () => {
  // 设置刷新标识
  isRefreshingFlag.value = true
  loading.value = true;
  // 启动进度条
  startLoadingProgress()

  try {
    // 重置搜索参数
    searchParams.searchText = '';
    searchParams.current = 1;
    searchParams.pageSize = 36;
    page.value = 1;

    // 清空列表数据
    mobileDataList.value = [];

    // 调用 fetchData 获取当前分类的数据，不改变任何状态
    await fetchData();
  } catch (error) {
    // 终止进度条
    stopLoadingProgress()
    console.error('刷新数据出错:', error);
    message.error('刷新失败，请稍后重试');
  } finally {
    loading.value = false;
    // 重置刷新标识
    isRefreshingFlag.value = false;
  }
}

// 下拉刷新相关（仅移动端生效）
const handleTouchStart = (e: TouchEvent) => {
  if (device.value === DEVICE_TYPE_ENUM.PC) return
  startY.value = e.touches[0].clientY
  currentY.value = e.touches[0].clientY
}

const handleTouchMove = (e: TouchEvent) => {
  if (device.value === DEVICE_TYPE_ENUM.PC || isRefreshing.value) return
  const scrollTop = document.documentElement.scrollTop || document.body.scrollTop
  if (scrollTop > 0) return
  currentY.value = e.touches[0].clientY
  const distance = currentY.value - startY.value
  if (distance > 0) {
    e.preventDefault()
    refreshDistance.value = Math.min(distance * 0.5, maxPullDistance)
  }
}

const handleTouchEnd = async () => {
  if (device.value === DEVICE_TYPE_ENUM.PC) return
  if (refreshDistance.value >= refreshThreshold && !isRefreshing.value) {
    isRefreshing.value = true
    await onRefresh()
    isRefreshing.value = false
  }
  refreshDistance.value = 0
}

// 时间更新
const updateTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  })
}

// 时间显示
const currentTime = ref(new Date().toLocaleTimeString())
</script>

<style scoped>
#homePage {
  color: var(--text-primary);
  position: relative;
  max-width: 1400px;
  margin: 0 2px;
}

@media screen and (min-width: 768px) {
  #homePage {
    margin: auto;
    margin-top: -16px;
  }
}

:deep(.ant-input-search) {
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

:deep(.ant-input-search .ant-input) {
  margin: auto;
  height: 28px;
  font-size: 14px;
  padding: 0 12px;
}

:deep(.ant-input-search .ant-input-group-addon:last-child) {
  inset-inline-start: 0;
  padding: 0;
  border: 0;
}

:deep(.ant-input-search .ant-btn) {
  height: 36px;
  font-size: 14px;
  background: #2563eb;
  border-color: #2563eb;
  box-shadow: none;
}

:deep(.ant-input-search .ant-btn:hover) {
  background: #3b82f6;
  border-color: #3b82f6;
}

.search-icon {
  color: #2563eb;
  font-size: 16px;
}


.m-filter-section{
  padding-left: 6px;
  padding-top: 1px;
  padding-right: 6px;
  color: var(--text-primary);
  background: var(--post-background);
}

.pc-empty .empty-text {
  font-size: 16px;
  margin-bottom: 8px;
}

.pc-empty .empty-desc {
  font-size: 14px;
  opacity: 0.8;
}

.pc-loading-more {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px 0;
  margin-bottom: 32px;
}

.pc-no-more-data {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px 0;
  color: #8b9eb0;
  font-size: 14px;
  gap: 8px;
}

/* 分类标签样式 */
.category-tabs {
  display: flex;
  flex-wrap: nowrap;
  overflow-x: auto;
  gap: 8px;
  padding: 8px 0;
  padding-top: 12px;
  -webkit-overflow-scrolling: touch;
  /* 美化滚动条 */
  scrollbar-width: thin;
  scrollbar-color: rgba(37, 99, 235, 0.3) transparent;
}

/* 美化WebKit浏览器的滚动条 */
.category-tabs::-webkit-scrollbar {
  height: 3px;
}

.category-tabs::-webkit-scrollbar-track {
  background: transparent;
  border-radius: 3px;
}

.category-tabs::-webkit-scrollbar-thumb {
  background: rgba(37, 99, 235, 0.3);
  border-radius: 3px;
  transition: background 0.3s ease;
}

.category-tabs::-webkit-scrollbar-thumb:hover {
  background: rgba(37, 99, 235, 0.5);
}

.category-tab-item {
  padding: 4px 12px;
  font-size: 12px;
  color: #64748b;
  transition: all 0.3s ease;
  border-radius: 12px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
  flex: 0 0 auto; /* 确保项目不会缩小 */
  white-space: nowrap; /* 防止文字换行 */
}

.category-tab-item.active {
  background: rgba(37, 99, 235, 0.1);
  border-radius: 16px;
  color: var(--text-primary);
  font-weight: 500;
}

.category-tab-item i {
  font-size: 10px;
}

:deep(.category-tabs .ant-tabs-nav) {
  margin: 0 0 8px 0;
}

:deep(.category-tabs .ant-tabs-tab) {
  padding: 4px 12px;
  margin: 0 4px;
  font-size: 12px;
  color: #64748b;
  transition: all 0.3s ease;
  border-radius: 12px;
  /* 移除所有边框 */
  border: none !important;
  /* 去除默认边框相关样式 */
  border-width: 0 !important;
}

/* 额外：清除tabs默认的底部边框（如果需要） */
:deep(.category-tabs .ant-tabs-nav::before) {
  border-bottom: none !important;
}

:deep(.category-tabs .ant-tabs-tab-active) {
  background: rgba(37, 99, 235, 0.1);
  border-radius: 16px;
  color: var(--text-primary);
  font-weight: 500;
}

:deep(.category-tabs .ant-tabs-tab-btn) {
  color: inherit;
}

:deep(.category-tabs .ant-tabs-tab-active .ant-tabs-tab-btn) {
  color: var(--text-primary);
}

:deep(.category-tabs .ant-tabs-ink-bar) {
  display: none;
}


:deep(.ant-tag-checkable-checked) {
  background: rgba(37, 99, 235, 0.1);
  color: var(--text-primary);
}

/* 移动端搜索框样式 */
.mobile-search {
  padding-top: 0;
  background: var(--header-background);
  color: var(--text-primary);
  margin-top: -12px;
  margin-right: -40px;
  position: relative;
  z-index: 0;
  transition: all 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.mobile-search .search-bar {
  width: 100%;
}

.mobile-search :deep(.ant-btn-icon-only){
  width: 78px;
}

/* 固定状态的搜索框样式 */
.mobile-search-fixed {
  z-index: 4;
  position: fixed !important;
  top: 10px !important;
  right: 48px !important;
  height: 64px !important;
  padding: 0 4px !important;
  padding-right: 12px !important;
  display: flex !important;
  align-items: center !important;
  justify-content: flex-end !important;
  background: transparent !important;
  transform: translateY(0) !important;
  opacity: 1 !important;
}
.mobile-search-fixed :deep(.ant-btn-icon-only){
  width: 28px;
}
/* 搜索框过渡动画 */
.mobile-search {
  transform: translateX(0) scale(1);
  opacity: 1;
  transition: all 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.mobile-search.mobile-search-fixed {
  transform: translateX(calc(50vw - 50%)) scale(0.9);
}


/* 搜索框样式 */
.mobile-search :deep(.ant-input-search) {
  border: none !important;
  background: rgba(255, 255, 255, 0.95) !important;
  border-radius: 32px !important;
  box-shadow:
    0 4px 16px rgba(0, 0, 0, 0.06),
    0 2px 4px rgba(37, 99, 235, 0.05) !important;
  backdrop-filter: blur(8px) !important;
  border: 1px solid rgba(37, 99, 235, 0.1) !important;
  transition: all 0.2s cubic-bezier(0.34, 1.56, 0.64, 1) !important;
  width: 100% !important;

  /* 移除输入框聚焦时的蓝色边框 */
  .ant-input-wrapper {
    .ant-input:focus {
      box-shadow: none !important;
      outline: none !important;
    }

    .ant-input-group-addon {
      border: none !important;
      background: transparent !important;
    }
  }
}

/* 固定状态时的搜索框样式 */
.mobile-search-fixed :deep(.ant-input-search) {
  transform: scale(1) !important;
  height: 32px;
  line-height: 32px;
  box-shadow:
    0 6px 20px rgba(0, 0, 0, 0.08),
    0 2px 8px rgba(37, 99, 235, 0.08) !important;
  border: 1px solid rgba(37, 99, 235, 0.15) !important;
  /* 修改搜索图标颜色 */
  .anticon-search {
    color: #fff !important;
    font-size: 20px !important;
    opacity: 0.9 !important;
  }
}

.mobile-search :deep(.ant-input) {
  height: 32px !important;
  font-size: 13px !important;
  padding: 0 12px !important;
  background: transparent !important;
  border: none !important;
  text-align: center !important;
  color: #1a1a1a !important;
  transition: all 0.2s cubic-bezier(0.34, 1.56, 0.64, 1) !important;
}

/* 添加动画效果 */
.mobile-search :deep(.ant-input-search) {
  transition: all 0.2s cubic-bezier(0.34, 1.56, 0.64, 1) !important;
}

.mobile-search :deep(.ant-input-search:active) {
  transform: scale(0.95) !important;
  background: rgba(255, 255, 255, 0.98) !important;
  box-shadow:
    0 2px 8px rgba(37, 99, 235, 0.12),
    0 1px 4px rgba(0, 0, 0, 0.04) !important;
}

/* 占位符样式 */
.mobile-search :deep(.ant-input::placeholder) {
  color: #94a3b8 !important;
  font-size: 13px !important;
}

@keyframes spinner-rotate {
  100% {
    transform: rotate(360deg);
  }
}

@keyframes spinner-dash {
  0% {
    stroke-dasharray: 1, 150;
    stroke-dashoffset: 0;
  }
  50% {
    stroke-dasharray: 90, 150;
    stroke-dashoffset: -35;
  }
  100% {
    stroke-dasharray: 90, 150;
    stroke-dashoffset: -124;
  }
}

/* 修改无更多数据提示样式 */
.no-more-data-tip {
  margin: auto;
  text-align: center;
  padding: 16px;
  color: #c4947e;
  font-size: 14px;
  opacity: 0.8;
  margin-bottom: 64px;
}


/* 移除静态间距，改为动态注入或占位符控制 */
.mobile-list-container {
  /* margin-top: 90px; */
}
/* PC 端过滤中心二级导航增强 */
.pc-ranking-bar-v3 {
  margin-top: 12px;
}
.main-tabs-toolbar-v3 {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 4px 0;
}
.ranking-wrapper-v3 {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: nowrap;
}
.pc-refresh-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 38px;
  padding: 0 16px;
  background: #f8fafc;
  border-radius: 10px;
  color: #64748b;
  font-size: 13px;
  font-weight: 500;
  transition: all 0.3s ease;
  border: 1px solid #e2e8f0;
  cursor: pointer;
}
.pc-refresh-btn:hover {
  background: #f1f5f9;
  color: #1e293b;
  border-color: #cbd5e1;
}
.spin-active {
  animation: reload-spin 0.8s linear infinite;
}
@keyframes reload-spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
.ranking-item-v3 {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 38px;
  padding: 0 20px;
  border-radius: 10px;
  cursor: pointer;
  background: #f8fafc;
  color: #64748b;
  font-size: 13px;
  font-weight: 500;
  transition: all 0.3s ease;
  border: 1px solid transparent;
  box-sizing: border-box;
}
.ranking-item-v3:hover {
  background: #f1f5f9;
  color: #2563eb;
}
.ranking-item-v3.active {
  background: #000 !important;
  color: #fff !important;
  font-weight: bold;
  border-color: #000 !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}
/* 移动端作者列表行内容器 */
.m-follow-users-inline {
  padding: 0;
  background: rgba(255, 255, 255, 0.4);
  border-radius: 12px;
  margin: -8px 12px 8px;
  margin-bottom: 0;
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  overflow: hidden;
}

/* 移动端自定义榜单子页签 */
.m-ranking-tabs {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px ;
  padding-bottom: 2px;
  overflow-x: auto;
  -ms-overflow-style: none;
  scrollbar-width: none;
  background: var(--background) !important;
}
.m-ranking-tabs::-webkit-scrollbar {
  display: none;
}

.m-ranking-tab-item {
  flex: none;
  padding: 6px 18px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
  color: #64748b;
  background: #f1f5f9;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid transparent;
}

.m-ranking-tab-item.active {
  color: #ffffff;
  background: #2563eb;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.25);
  transform: translateY(-1px);
}

/* 移动端顶部导航样式 */
.mobile-nav {
  position: fixed !important;
  top: 10px !important;
  width: 100%;
  height: 64px !important;
  padding-top: 16px;
  transform: translateY(0) !important;
  opacity: 1 !important;
  margin: -16px  0;
  padding-bottom: 10px;
  background: var(--background);
  color: var(--text-primary);
  z-index: 2;
}

.mobile-nav :deep(.van-tabs__nav) {
  background-color: transparent !important;
}

.mobile-nav :deep(.van-tab) {
  background-color: transparent !important;
}

.mobile-nav :deep(.van-tabs__wrap) {
  padding: 0 16px;
}

.mobile-nav :deep(.van-tab) {
  flex: none;
  min-width: 72px;
  font-size: 18px;
  color: #64748b;
  position: relative;
  transition: all 0.3s ease;
}

.mobile-nav :deep(.van-tab--active) {
  color: #2563eb;
  font-weight: 500;
  font-size: 18px;
  transform: scale(1.05);
}

.tab-content {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  transition: all 0.3s ease;
}

.tab-content span {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 16px;
}

/* 优化滑动切换动画 */
.mobile-nav :deep(.van-tabs__content) {
  transition: transform 0.3s cubic-bezier(0.645, 0.045, 0.355, 1);
}

.mobile-nav :deep(.van-tabs__track) {
  transition: transform 0.3s cubic-bezier(0.645, 0.045, 0.355, 1);
}

/* 榜单标签样式 */
.ranking-tabs {
  background: var(--background);
  color: var(--text-primary);
  border-bottom: 1px solid var(--border-color);
}

.ranking-tabs :deep(.van-tabs__wrap) {
  height: 40px;
}

.ranking-tabs :deep(.van-tabs__nav) {
  background: var(--background);
  border-radius: 20px;
  height: 40px;
  margin-bottom: 20px;
}

.ranking-tabs :deep(.van-tab) {
  flex: 1;
  min-width: 80px;
  height: 32px;
  line-height: 32px;
  font-size: 11px;
  color: #64748b;
  border-radius: 16px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  padding: 0;
}

.ranking-tabs :deep(.van-tab--active) {
  color: #2563eb;
  font-weight: 500;
  font-size: 11px;
  background: linear-gradient(135deg, #eff6ff 0%, #fff 100%);
  box-shadow:
    0 2px 8px rgba(37, 99, 235, 0.1),
    0 1px 4px rgba(37, 99, 235, 0.05);
}



.ranking-tabs :deep(.van-tab:active)::before {
  opacity: 0.1;
}

/* 隐藏底部线条 */
.ranking-tabs :deep(.van-tabs__line) {
  display: none;
}

/* 添加渐变背景效果 */
.ranking-tabs :deep(.van-tab--active) {
  background: linear-gradient(135deg, #eff6ff 0%, #fff 100%);
  box-shadow:
    0 2px 8px rgba(37, 99, 235, 0.1),
    0 1px 4px rgba(37, 99, 235, 0.05);
}

/* 添加过渡动画 */
.ranking-tabs :deep(.van-tabs__nav) {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.ranking-tabs :deep(.van-tab) {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.empty-following {
  padding: 40px 20px;
  text-align: center;
}

.custom-empty {
  padding: 32px 0;
}

.empty-desc {
  margin: 8px 0 16px;
  color: #94a3b8;
  font-size: 14px;
}

.discover-btn {
  width: 120px;
  height: 36px;
  border-radius: 18px;
  background: linear-gradient(135deg, #2563eb 0%, #3b82f6 100%);
  border: none;
  background: var(--header-background);
  color: var(--text-primary);
  font-weight: 500;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.2);
  transition: all 0.3s ease;
}

.discover-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(37, 99, 235, 0.3);
}

.discover-btn:active {
  transform: translateY(1px);
}



/* 移动端搜索框过渡动画 */
.mobile-search {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.mobile-search-transitioning {
  transform: scale(1.1) translateY(-10px);
  opacity: 0;
}

:deep(.ant-input-search) {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  }

  &:active {
    transform: scale(0.98);
  }
}

/* 移动端搜索框样式优化 */
.mobile-search {
  &:active {
    transform: scale(0.95);
  }

  :deep(.ant-input-search) {
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

    &:active {
      transform: scale(0.95);
      background: var(--header-background);
      color: var(--text-primary);
      box-shadow:
        0 2px 8px rgba(37, 99, 235, 0.12),
        0 1px 4px rgba(0, 0, 0, 0.04) !important;
    }
  }
}

/* 搜索按钮样式 */
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


  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 2px 8px rgba(37, 99, 235, 0.2);

    .search-icon {
      transform: rotate(-5deg) scale(1.1);
    }

    .search-text {
      transform: translateX(2px);
    }
  }

  &:active {
    transform: translateY(0);
    box-shadow: 0 1px 4px rgba(37, 99, 235, 0.1);
  }
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


/* 移动端活动轮播图样式 */
.activity-carousel {
  margin-top: 99px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border-radius: 12px;
  overflow: hidden;
}

.carousel-container {
  width: 100%;
  position: relative;
  overflow: hidden;
}

.carousel-item {
  width: 100%;
  height: 100%;
  position: relative;
  cursor: pointer;
}

.carousel-item-content {
  width: 100%;
  height: 100%;
  position: relative;
}

.carousel-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.carousel-info {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 16px;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.8), transparent);
  color: white;
}

.carousel-title {
  margin: 0 0 8px;
  font-size: 18px;
  font-weight: 600;
  line-height: 1.4;
}

.carousel-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
}

.carousel-status {
  padding: 4px 8px;
  border-radius: 4px;
  background: #4caf50;
  font-weight: 500;
}

.carousel-status.expired {
  background: #9e9e9e;
}

.carousel-date {
  opacity: 0.9;
}

:deep(.van-swipe__indicator) {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.5);
  opacity: 1;
}

:deep(.van-swipe__indicator--active) {
  background: white;
  transform: scale(1.2);
}

/* PC端全宽顶部布局 (V4) */
.pc-top-layout-full {
  max-width: 1400px;
  margin: 0 auto;
  margin-top: 20px;
  margin-bottom: 30px;
}

.pc-carousel-section-full {
  width: 100%;
  height: 420px; /* 扁平化比例，更显高级 */
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.05);
}

.pc-carousel-section-full :deep(.modern-carousel),
.pc-carousel-section-full :deep(.carousel-swipe),
.pc-carousel-section-full :deep(.carousel-slide),
.pc-carousel-section-full :deep(.slide-card) {
  height: 100%;
  border-radius: 16px;
}

/* 全宽层级过滤中心 V3 */
.pc-filter-center {
  max-width: 1400px;
  margin: 0 auto;
  margin-bottom: 16px;
}

/* 顶部标签和控制按钮包裹层，移交底部描边和布局权给外部控制层 */
.main-tabs-toolbar-v3 {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--border-color);
  margin-bottom: 24px;
}

.main-tabs-row {
  display: flex;
  gap: 32px;
}

.pc-refresh-control-v3 {
  flex-shrink: 0;
}

.pc-refresh-btn {
  color: var(--text-secondary);
  font-weight: 500;
  border-radius: 8px;
  transition: all 0.3s ease;
  padding: 6px 16px;
  background: var(--hover-background);
}

.pc-refresh-btn:hover {
  color: #10b981;
  background: rgba(16, 185, 129, 0.1);
  transform: translateY(-1px);
}

.spin-active {
  animation: a-spin 1s linear infinite;
}



.main-tab-item {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-secondary);
  cursor: pointer;
  position: relative;
  padding: 8px 0;
  transition: var(--theme-transition);
}

.main-tab-item:hover {
  color: var(--text-primary);
}

.main-tab-item.active {
  color: var(--text-primary);
}

.main-tab-item.active::after {
  content: '';
  position: absolute;
  bottom: -5px;
  left: 0;
  right: 0;
  height: 4px;
  background: var(--text-primary);
  border-radius: 2px;
}

.sub-filter-row {
  min-height: 60px; /* 保持基本高度，防止切换时抖动 */
}

/* 分类云 V3 */
.pc-category-bar-v3 {
  animation: fadeIn 0.3s ease;
}

.category-wrapper-v3 {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  min-height: 110px; /* 固化两行高度 */
}

.category-pill-v3 {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 38px;
  padding: 0 22px;
  background: var(--hover-background);
  border-radius: 12px;
  font-size: 14px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: var(--theme-transition);
  font-weight: 600;
  border: 1px solid var(--border-color);
  box-sizing: border-box;
}

.category-pill-v3:hover {
  background: var(--border-color);
  transform: translateY(-2px);
}

.category-pill-v3.active {
  background: var(--text-primary);
  color: var(--background);
  border-color: var(--text-primary);
}

.category-pill-v3.more-btn {
  background: var(--background);
  border: 1px dashed var(--border-color);
  color: var(--text-secondary);
}

/* 榜单切换 V3 */
.pc-ranking-bar-v3 {
  display: flex;
  gap: 12px;
  animation: fadeIn 0.3s ease;
}

.ranking-item-v3 {
  padding: 10px 24px;
  background: var(--card-background);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-secondary);
  cursor: pointer;
  transition: var(--theme-transition);
}

.ranking-item-v3.active {
  background: var(--nav-item-active);
  color: var(--nav-item-active-text);
  border-color: var(--nav-item-active-text);
  box-shadow: 0 4px 12px var(--shadow-color);
}

/* 关注提示 V3 */
.following-tip-v3 {
  font-size: 14px;
  color: #94a3b8;
  background: #f8fafc;
  border-radius: 12px;
  display: inline-block;
  animation: fadeIn 0.3s ease;
}

/* 关注状态下的二级过滤器基础容器 */
.pc-following-bar-v3 {
  animation: fadeIn 0.3s ease;
  padding: 10px 0;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(5px); }
  to { opacity: 1; transform: translateY(0); }
}

/* 确保最后一个元素没有底部边距 */
.scrollable-content > *:last-child {
  margin-bottom: 0;
}

/* 自定义滚动条样式 */
.scrollable-content::-webkit-scrollbar {
  width: 4px;
}

.scrollable-content::-webkit-scrollbar-track {
  background: rgba(37, 99, 235, 0.1);
  border-radius: 2px;
}

.scrollable-content::-webkit-scrollbar-thumb {
  background: rgba(37, 99, 235, 0.3);
  border-radius: 2px;
  transition: background 0.3s ease;
}

.scrollable-content::-webkit-scrollbar-thumb:hover {
  background: rgba(37, 99, 235, 0.5);
}

/* 移动端专用动画及样式由下方继续保留 */

/* 作者列表相关样式已移除，统一由 FollowUserList 组件控制 */



/* 悬浮图标辅助样式保留 */

.floating-icon:nth-child(1) {
  top: 20%;
  left: 10%;
  animation-delay: 0s;
}

.floating-icon:nth-child(2) {
  top: 60%;
  right: 15%;
  animation-delay: -2s;
}

.floating-icon:nth-child(3) {
  bottom: 20%;
  left: 20%;
  animation-delay: -4s;
}


@keyframes float {
  0%, 100% {
    transform: translateY(0) rotate(0deg);
  }
  50% {
    transform: translateY(-20px) rotate(10deg);
  }
}

.nav-tabs {
  display: flex;
  gap: 4px;
  padding: 4px;
  background: rgba(241, 245, 249, 0.7);
  border-radius: 14px;
  position: relative;
  margin-bottom: 4px;
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.03);
  height: 48px;
  border: 1px solid rgba(226, 232, 240, 0.5);
}

.nav-item {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  color: #64748b;
  cursor: pointer;
  border-radius: 11px;
  transition:
    color 0.3s ease,
    background 0.3s ease,
    transform 0.35s cubic-bezier(0.34, 1.56, 0.64, 1),
    box-shadow 0.3s ease;
  position: relative;
  z-index: 1;
  font-weight: 600;
  letter-spacing: 0.3px;
  user-select: none;

  span {
    display: flex;
    align-items: center;
    gap: 6px;
    z-index: 2;
    transition: transform 0.35s cubic-bezier(0.34, 1.56, 0.64, 1);
  }

  &:hover {
    color: #2563eb;
    background: rgba(255, 255, 255, 0.6);
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(37, 99, 235, 0.15);
  }

  &.active {
    color: #fff;
    background: linear-gradient(135deg, #60a5fa, #2563eb);
    box-shadow:
      0 4px 16px rgba(37, 99, 235, 0.35),
      inset 0 1px 0 rgba(255,255,255,0.3);
    transform: translateY(-2px) scale(1.04);
  }

  &:active {
    transform: scale(0.93);
  }
}
/* 移动端适配 */
@media screen and (max-width: 768px) {
  /* 移除作者列表相关覆盖样式 */
}

.category-section {
  padding: 8px;
  background: rgba(248, 250, 252, 0.8);
  border-radius: 12px;
  border: 1px solid rgba(241, 245, 249, 0.8);
  position: relative;
  z-index: 1;
  overflow-y: hidden;  /* 强制禁用垂直滚动条 */
  overflow-x: hidden;  /* 防止内容溢出 */
}

.category-list {
  display: flex;
  flex-wrap: wrap;  /* 允许换行 */
  gap: 10px;
  position: relative;
  z-index: 2;
}

.category-item {
  padding: 8px 18px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.85);
  color: #64748b;
  font-size: 13px;
  cursor: pointer;
  transition:
    color 0.3s ease,
    background 0.3s ease,
    transform 0.35s cubic-bezier(0.34, 1.56, 0.64, 1),
    box-shadow 0.3s ease,
    border-color 0.3s ease;
  border: 1px solid rgba(226, 232, 240, 0.7);
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 6px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.03);
  white-space: nowrap;
  flex-shrink: 0;
  /* stagger 动画 */
  animation: categoryFadeIn 0.4s ease both;

  &:hover {
    color: #2563eb;
    background: #fff;
    border-color: rgba(37, 99, 235, 0.5);
    transform: translateY(-3px) scale(1.04);
    box-shadow:
      0 8px 20px rgba(37, 99, 235, 0.15),
      0 2px 6px rgba(37, 99, 235, 0.08);
  }

  &.active {
    color: #fff;
    background: linear-gradient(135deg, #60a5fa, #2563eb);
    border-color: transparent;
    box-shadow:
      0 6px 20px rgba(37, 99, 235, 0.35),
      inset 0 1px 0 rgba(255, 255, 255, 0.3);
    transform: translateY(-2px) scale(1.06);
  }

  &:active {
    transform: scale(0.93);
  }
}

/* 每个 category-item 按顺序延迟进场 */
.category-item:nth-child(1)  { animation-delay: 0.0s; }
.category-item:nth-child(2)  { animation-delay: 0.04s; }
.category-item:nth-child(3)  { animation-delay: 0.08s; }
.category-item:nth-child(4)  { animation-delay: 0.12s; }
.category-item:nth-child(5)  { animation-delay: 0.16s; }
.category-item:nth-child(6)  { animation-delay: 0.20s; }
.category-item:nth-child(7)  { animation-delay: 0.24s; }
.category-item:nth-child(8)  { animation-delay: 0.28s; }
.category-item:nth-child(n+9){ animation-delay: 0.32s; }
@keyframes categoryFadeIn {
  from { opacity: 0; transform: translateY(6px) scale(0.95); }
  to   { opacity: 1; transform: translateY(0)   scale(1); }
}

.ranking-item {
  flex: 1;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.85);
  color: #64748b;
  font-size: 14px;
  cursor: pointer;
  transition:
    color 0.3s ease,
    background 0.3s ease,
    transform 0.35s cubic-bezier(0.34, 1.56, 0.64, 1),
    box-shadow 0.3s ease;
  border: 1px solid rgba(226, 232, 240, 0.7);
  user-select: none;
  font-weight: 600;
  animation: categoryFadeIn 0.4s ease both;

  &:nth-child(1) { animation-delay: 0.06s; }
  &:nth-child(2) { animation-delay: 0.12s; }
  &:nth-child(3) { animation-delay: 0.18s; }
  &:nth-child(4) { animation-delay: 0.24s; }

  &:hover {
    color: #2563eb;
    background: #fff;
    border-color: rgba(37, 99, 235, 0.5);
    transform: translateY(-3px) scale(1.04);
    box-shadow: 0 8px 20px rgba(37, 99, 235, 0.15);
  }

  &.active {
    color: #fff;
    background: linear-gradient(135deg, #60a5fa, #2563eb);
    border-color: transparent;
    box-shadow:
      0 6px 20px rgba(37, 99, 235, 0.35),
      inset 0 1px 0 rgba(255, 255, 255, 0.3);
    transform: translateY(-2px) scale(1.06);
  }

  &:active {
    transform: scale(0.93);
  }
}

/* PC端分类列表样式优化 */
.category-section {
  width: 100%;
  overflow: hidden;

  .category-list {
    display: flex;
    flex-wrap: nowrap;
    align-items: center;
    overflow-x: auto;
    padding-top: 12px;
    padding-bottom: 4px;
    margin: auto;
    -webkit-overflow-scrolling: touch;
    scrollbar-width: thin;
    white-space: nowrap;
    gap: 8px;
  }

  /* 谷歌/Edge 滚动条美化 */
  .category-list::-webkit-scrollbar {
    height: 2px;
  }

  .category-list::-webkit-scrollbar-track {
    background: transparent;
    border-radius: 3px;
  }

  .category-list::-webkit-scrollbar-thumb {
    background: rgba(37, 99, 235, 0.3);
    border-radius: 3px;
    transition: background 0.3s ease;
  }

  .category-list::-webkit-scrollbar-thumb:hover {
    background: rgba(37, 99, 235, 0.5);
  }

  /* 火狐滚动条兼容 */
  .category-list {
    scrollbar-width: thin;
    scrollbar-color: rgba(37, 99, 235, 0.3) transparent;
  }
}

/* 添加弹出层样式 */
.quote-popup {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(4px);
  animation: fadeIn 0.2s ease-out;
}

.quote-popup-content {
  background: #fff;
  padding: 24px;
  border-radius: 16px;
  max-width: 90%;
  width: 480px;
  position: relative;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  animation: slideUp 0.3s ease-out;
  background: linear-gradient(135deg, #fff6f3 0%, #fff 100%);
  border: 1px solid #ffe4d6;
}

.quote-popup-close {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 24px;
  height: 24px;
  border-radius: 12px;
  background: rgba(37, 99, 235, 0.1);
  color: #2563eb;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 18px;
  transition: all 0.3s ease;

  &:hover {
    background: rgba(37, 99, 235, 0.2);
    transform: rotate(90deg);
  }
}

.quote-popup-text {
  font-size: 18px;
  color: #4a5568;
  line-height: 1.8;
  margin-bottom: 16px;
  font-style: italic;
  position: relative;
  padding-left: 28px;

  &::before {
    content: '"';
    position: absolute;
    top: 0;
    left: 0;
    font-size: 48px;
    font-family: serif;
    color: #2563eb;
    opacity: 0.2;
    line-height: 1;
  }
}

.quote-popup-author {
  font-size: 16px;
  color: #718096;
  text-align: right;
  font-weight: 500;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes slideUp {
  from {
    transform: translateY(20px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

/* 添加动态文字效果 */
.quote-text.playful span {
  display: inline-block;
  animation: float 1s ease-in-out infinite;
  animation-play-state: paused;
  transition: color 0.3s ease;
}

.quote-text-wrapper:hover .playful span {
  animation-play-state: running;
}

.quote-text.playful span:hover {
  color: #2563eb;
  transform: translateY(-5px) rotate(10deg) scale(1.1);
}

@keyframes float {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-5px);
  }
}

@keyframes typing {
  from { width: 0 }
  to { width: 100% }
}

@keyframes blink-caret {
  from, to { border-color: transparent }
  50% { border-color: #2563eb }
}
.nav-item span {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 优化标签切换动画 */
.mobile-nav :deep(.van-tabs__content) {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.mobile-nav :deep(.van-tabs__track) {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 添加内容切换动画 */
.mobile-list-container {
  margin-top: 94px;

  transform: translateY(0);
  transition: opacity 0.3s ease, transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 优化移动端导航栏动画 */
.mobile-nav :deep(.van-tab) {
  position: relative;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.mobile-nav :deep(.van-tab--active) {
  transform: scale(1.05);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.mobile-nav :deep(.van-tabs__line) {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 优化分类标签切换动画 */
.m-filter-section :deep(.ant-tabs-ink-bar) {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
}

.m-filter-section :deep(.ant-tabs-tab) {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
}

.m-filter-section :deep(.ant-tabs-tab-active) {
  transform: scale(1.05);
  color: #000 !important;
}

@keyframes spinner-rotate {
  100% {
    transform: rotate(360deg);
  }
}

@keyframes spinner-dash {
  0% {
    stroke-dasharray: 1, 150;
    stroke-dashoffset: 0;
  }
  50% {
    stroke-dasharray: 90, 150;
    stroke-dashoffset: -35;
  }
  100% {
    stroke-dasharray: 90, 150;
    stroke-dashoffset: -124;
  }
}

/* 加载动画关键帧 */
@keyframes slide {
  0%, 100% { bottom: -35px }
  25%, 75% { bottom: -2px }
  20%, 80% { bottom: 2px }
}

@keyframes rotate {
  0% { transform: rotate(-15deg) }
  25%, 75% { transform: rotate(0deg) }
  100% { transform: rotate(25deg) }
}

/* 极速淡入动画 */
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

/* 标题淡入动画 */
@keyframes titleFade {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

/* 加载器显示动画 */
@keyframes loaderShow {
  from { opacity: 0; transform: scale(0.9); }
  to { opacity: 1; transform: scale(1); }
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}


.follow-users-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px 0;
  flex-direction: column;
  gap: 8px;
}

.loading-text {
  font-size: 14px;
  color: #666;
}

.follow-users-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 10px 20px;
  text-align: center;
}

.empty-text {
  font-size: 16px;
  color: #333;
  margin-bottom: 8px;
  font-weight: 500;
}

.empty-desc {
  font-size: 14px;
  color: #999;
  margin: 0;
}

.loading-camera {
  width: 40px;
  height: 40px;
  animation: camera-bounce 1s ease-in-out infinite;
}

.camera-body {
  fill: none;
  stroke: #2563eb;
  stroke-width: 4;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-dasharray: 200;
  stroke-dashoffset: 200;
  animation: camera-draw 3s ease-in-out infinite;
}

.camera-lens {
  fill: none;
  stroke: #2563eb;
  stroke-width: 4;
  stroke-dasharray: 100;
  stroke-dashoffset: 100;
  animation: camera-draw 3s ease-in-out infinite 0.5s;
}

.camera-flash {
  fill: #2563eb;
  opacity: 0;
  animation: flash-blink 3s ease-in-out infinite;
}

@keyframes camera-draw {
  0% {
    stroke-dashoffset: 200;
  }
  30% {
    stroke-dashoffset: 0;
  }
  80% {
    stroke-dashoffset: 0;
  }
  100% {
    stroke-dashoffset: -200;
  }
}

@keyframes flash-blink {
  0%, 20% {
    opacity: 0;
  }
  25%, 35% {
    opacity: 1;
  }
  40%, 100% {
    opacity: 0;
  }
}

@keyframes camera-bounce {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-3px);
  }
}

.custom-pull-refresh {
  position: relative;
  width: 100%;
  will-change: transform;
  touch-action: pan-y;
}

.refresh-indicator {
  position: absolute;
  left: 0;
  right: 0;
  top: -50px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #666;
  font-size: 14px;

  .refresh-icon {
    margin-right: 8px;
  }

  .pull-arrow {
    transition: transform 0.3s;
  }

}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.mobile-list-container {
  min-height: 100vh;
}

/* 移动端高性能分类导航组件样式 */
.mobile-category-nav {
  position: relative;
  background: var(--background);
  z-index: 100;
  margin-bottom: 8px;
}

.mobile-category-bar-wrapper {
  display: flex;
  align-items: center;
  position: relative;
  height: 48px; /* 增加高度确保容错 */
  background: var(--background);
  padding-right: 48px;
  overflow: hidden;
  margin-top: -8px; /* 移除向上偏移带来的空隙，紧贴导航 */
}

.mobile-category-bar {
  display: flex;
  overflow-x: auto;
  white-space: nowrap;
  padding: 0 12px;
  gap: 12px;
  flex: 1;
  scrollbar-width: none; /* 隐藏滚动条 */
  mask-image: linear-gradient(to right, black 85%, transparent 100%);
  -webkit-mask-image: linear-gradient(to right, black 85%, transparent 100%);
}

.mobile-category-bar::-webkit-scrollbar {
  display: none;
}

.mobile-category-item {
  padding: 6px 14px;
  font-size: 14px;
  color: var(--text-secondary);
  background: rgba(0, 0, 0, 0.03);
  border-radius: 18px;
  transition: all 0.2s cubic-bezier(0.18, 0.89, 0.32, 1.28);
  flex-shrink: 0;
}

.mobile-category-item.active {
  background: #2563eb;
  color: #fff;
  font-weight: 600;
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.2);
}

.category-expand-btn {
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  width: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--background);
  z-index: 10;
  color: var(--text-secondary);
  font-size: 14px;
  /* 强力左侧渐变，模拟切断效果 */
  box-shadow: -15px 0 20px -8px var(--background);
}

.category-expand-btn i {
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.category-expand-btn i.rotated {
  transform: rotate(180deg);
}

/* 宫格下拉弹窗沉浸式样式 */
.category-dropdown-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  z-index: 1500;
}

.category-dropdown-panel {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  background: var(--background);
  border-radius: 0 0 32px 32px;
  z-index: 2000;
  padding: 20px 20px 32px 20px;
  padding-top: max(20px, env(safe-area-inset-top));
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
  padding-bottom: 10px;
}

.dropdown-category-item {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 38px;
  font-size: 13px;
  background: rgba(0, 0, 0, 0.03);
  border-radius: 10px;
  color: var(--text-secondary);
  transition: all 0.2s;
  text-align: center;
  padding: 0 4px;
}

.dropdown-category-item.active {
  background: #2563eb;
  color: #fff;
  font-weight: 600;
}

/* 过渡动画 */
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

.slide-down-enter-active, .slide-down-leave-active { transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1); }
.slide-down-enter-from, .slide-down-leave-to { transform: translateY(-100%); opacity: 0.5; }

/* 暗色模式兼容 */
:global(.dark-theme) .mobile-category-item,
:global(.dark-theme) .dropdown-category-item {
  background: rgba(255, 255, 255, 0.08);
}

:global(.dark-theme) .category-expand-btn {
  background: rgba(30, 30, 30, 0.85);
  box-shadow: -4px 0 15px rgba(0, 0, 0, 0.2);
}

:global(.dark-theme) .category-dropdown-panel {
  background: #1e1e1e;
}

:global(.dark-theme) .close-btn-wrapper {
  background: rgba(255, 255, 255, 0.1);
}
</style>

