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
          <SearchOutlined class="search-icon" />
          <span class="search-divider">|</span>
          <span class="search-text">搜索</span>
        </div>
      </a-button>
    </div>

    <!-- 移动端顶部导航 -->
    <div v-if="device !== DEVICE_TYPE_ENUM.PC" class="mobile-nav">

      <van-tabs
        v-model:active="activeTab"
        swipeable
        animated
        style="width: 76%"
        :duration="0.3"
        :swipe-threshold="5"
        title-inactive-color="#8b9eb0"
        title-active-color="#ff8e53"
        :line-width="20"
      >
        <van-tab name="all">
          <template #title>
            <div class="tab-content">
              <span>{{ activeTab === 'all' ? '🗂️' : '' }}全部</span>
            </div>
          </template>
        </van-tab>
        <van-tab name="following">
          <template #title>
            <div class="tab-content">
              <span>{{ activeTab === 'following' ? '❤️' : '' }}关注</span>
            </div>
          </template>
        </van-tab>
        <van-tab name="ranking">
          <template #title>
            <div class="tab-content">
              <span>{{ activeTab === 'ranking' ? '🏆' : '' }}榜单</span>
            </div>
          </template>
        </van-tab>
      </van-tabs>
      <div class="m-filter-section" v-if="activeTab == 'all'" >
        <a-tabs
          v-model:active-key="selectedCategory"
          @change="handleCategoryChange"
          class="category-tabs"
        >
          <a-tab-pane v-for="category in categoryList" :tab="category" :key="category" />
        </a-tabs>
      </div>
      <!-- 榜单子标签 -->
      <div v-if="activeTab === 'ranking'" class="ranking-tabs">
        <van-tabs v-model:active="rankingType" @change="handleRankingTypeChange">
          <van-tab v-for="tab in rankingTabs"
                   :key="tab.value"
                   :title="tab.name"
                   :name="tab.value"
          />
        </van-tabs>
      </div>
    </div>


    <!-- 移动端活动轮播图 -->
    <div v-if="device !== DEVICE_TYPE_ENUM.PC && activeTab === 'all' && carouselActivities.length > 0" class="activity-carousel">
      <div class="carousel-container"
           @touchstart="handleTouchStart"
           @touchmove="handleTouchMove"
           @touchend="handleTouchEnd"
           @touchstart.passive="stopAutoplay"
           @touchend.passive="startAutoplay">
        <div class="carousel-wrapper"
             :style="{ transform: `translateX(${translateX}px)` }">
          <div v-for="activity in carouselActivities"
               :key="activity.id"
               class="carousel-item"
               @click="handleActivityClick(activity.id)">
            <img :src="activity.coverUrl"
                 :alt="activity.title"
                 class="carousel-image"
                 loading="lazy" />
            <div class="carousel-info">
              <h3 class="carousel-title">{{ activity.title }}</h3>
              <div class="carousel-meta">
                <span class="carousel-status"
                      :class="{ 'expired': activity.isExpired === 1 }">
                  {{ activity.isExpired === 1 ? '已结束' : '进行中' }}
                </span>
                <span class="carousel-date">{{ formatTime(activity.expireTime) }}截止</span>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div class="carousel-indicators">
        <span v-for="(_, index) in carouselActivities"
              :key="index"
              class="indicator-dot"
              :class="{ active: index === currentActivityIndex }"
              @click="currentActivityIndex = index"></span>
      </div>
    </div>

    <div v-if="device === DEVICE_TYPE_ENUM.PC">
      <!-- PC端顶部布局 -->
      <div class="pc-top-layout">
        <!-- PC端活动轮播图 -->
        <div class="pc-carousel-section" v-if="carouselActivities.length > 0">
          <div class="pc-carousel">
            <div class="carousel-container"
                 @mousedown="handleMouseDown"
                 @mousemove="handleMouseMove"
                 @mouseup="handleMouseUp"
                 @mouseleave="handleMouseUp">
              <div class="carousel-wrapper"
                   :style="{ transform: `translateX(${pcTranslateX}px)` }">
                <div v-for="activity in carouselActivities"
                     :key="activity.id"
                     class="carousel-item"
                     @click="handleActivityClick(activity.id)">
                  <img :src="activity.coverUrl"
                       :alt="activity.title"
                       class="carousel-image"
                       loading="lazy" />
                  <div class="carousel-info">
                    <h3 class="carousel-title">{{ activity.title }}</h3>
                    <div class="carousel-meta">
                      <span class="carousel-status"
                            :class="{ 'expired': activity.isExpired === 1 }">
                        {{ activity.isExpired === 1 ? '已结束' : '进行中' }}
                      </span>
                      <span class="carousel-date">{{ formatTime(activity.expireTime) }}截止</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <div class="carousel-indicators">
              <span v-for="(_, index) in carouselActivities"
                    :key="index"
                    class="indicator-dot"
                    :class="{ active: index === currentActivityIndex }"
                    @click="currentActivityIndex = index"></span>
            </div>
          </div>
        </div>

        <!-- PC端右侧快捷导航和名言 -->
        <div class="pc-quick-nav">
          <!-- 可滚动的内容区域 -->
          <div class="scrollable-content">
            <!-- 名人名言部分 -->
            <div class="quote-section" @click="handleQuoteClick">
              <h3 class="section-title">
                <span class="title-left">每日一句</span>
                <button class="barrage-btn" @click.stop="goToBarrage">
                  <CommentOutlined />
                  弹幕墙
                </button>
              </h3>
              <div class="quote-content">
                <div class="quote-text-wrapper">
                  <p class="quote-text playful" :class="{ 'truncate': !isQuoteExpanded }">
                    <span v-for="(char, index) in dailyQuote.content" :key="index"
                          :style="{ 'animation-delay': `${index * 0.1}s` }">
                      {{ char }}
                    </span>
                  </p>
                  <p class="quote-author">
                    <span class="typing-text">—— {{ dailyQuote.author }}</span>
                  </p>
                </div>
              </div>
            </div>

            <h3 class="section-title">快捷筛选</h3>
            <!-- 顶部导航 -->
            <div class="nav-tabs">
              <div class="nav-item"
                   :class="{ active: activeTab === 'all' }"
                   @click="activeTab = 'all'">
                <span>
                  <AppstoreOutlined class="nav-icon" />
                  全部
                </span>
              </div>
              <div class="nav-item"
                   :class="{ active: activeTab === 'following' }"
                   @click="activeTab = 'following'">
                <span>
                  <HeartOutlined class="nav-icon" />
                  关注
                </span>
              </div>
              <div class="nav-item"
                   :class="{ active: activeTab === 'ranking' }"
                   @click="activeTab = 'ranking'">
                <span>
                  <TrophyOutlined class="nav-icon" />
                  榜单
                </span>
              </div>
            </div>

            <!-- 分类选项（仅在全部标签下显示） -->
            <div v-if="activeTab === 'all'" class="category-section">
              <div class="category-list">
                <div class="category-item"
                     v-for="category in categoryList"
                     :key="category"
                     :class="{ active: selectedCategory === category }"
                     @click="handleCategoryClick(category)">
                  <span>{{ category }}</span>
                </div>
              </div>
            </div>

            <div v-if="activeTab === 'following'" class="following-section">
              <div class="following-content">
                <div class="following-header">
                  <div class="following-title">
                    <HeartOutlined class="following-icon" />
                    <span>我的关注</span>
                  </div>
                  <div class="current-time">
                    <div class="time-wrapper">
                      <ClockCircleOutlined class="time-icon" />
                      <span class="time-text">{{ currentTime }}</span>
                    </div>
                  </div>
                </div>

                <!-- 装饰性浮动图标 -->
                <div class="decoration-icons">
                  <StarOutlined class="floating-icon" />
                  <HeartOutlined class="floating-icon" />
                  <FireOutlined class="floating-icon" />
                </div>
              </div>
            </div>

            <!-- 榜单选项（仅在榜单标签下显示） -->
            <div v-if="activeTab === 'ranking'" class="ranking-section">
              <div class="ranking-list">
                <div class="ranking-item"
                     v-for="tab in rankingTabs"
                     :key="tab.value"
                     :class="{ active: rankingType === tab.value }"
                     @click="handleRankingTypeChange(tab.value)">
                  <span>{{ tab.name }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- PC端瀑布流组件 -->
      <div class="pc-waterfall-section">
        <WaterfallPictureList
          :dataList="pcDataList"
          :loading="loading"
          :onLoadMore="loadMorePictures"
        />
      </div>
    </div>
    <div v-else>
      <van-pull-refresh style="margin-left: -20px;margin-right: -20px"
                        :style="activeTab === 'following'
                          ? { marginTop: '-62px' }
                          : activeTab === 'all'
                          ? { marginTop: '-92px' }
                          : activeTab === 'ranking'
                          ? { marginTop: '16px' }
                          : {}"
                        v-model="loading"
                        @refresh="onRefresh"
                        :distance="80"
                        :head-height="60">
        <div class="mobile-list-container">
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

          <!-- 内部组件 -->
          <MobilePictureList v-else :dataList="mobileDataList" :loading="loading" />

          <!-- 加载状态提示 -->
          <div v-if="!isEndOfData && !loading && mobileDataList.length > 0&&activeTab === 'all'"  class="loading-more">
            <svg class="loading-camera" viewBox="0 0 100 100">
              <path class="camera-body" d="M25,30H75a8,8,0,0,1,8,8V70a8,8,0,0,1-8,8H25a8,8,0,0,1-8-8V38A8,8,0,0,1,25,30Zm5-10H70a2,2,0,0,1,2,2v4a2,2,0,0,1-2,2H30a2,2,0,0,1-2-2V22A2,2,0,0,1,30,20Z"/>
              <circle class="camera-lens" cx="50" cy="54" r="15"/>
              <circle class="camera-flash" cx="72" cy="42" r="4"/>
            </svg>
          </div>
          <div v-if="isEndOfData && mobileDataList.length > 0" class="no-more-data-tip">没有更多数据了哦~</div>
        </div>
      </van-pull-refresh>
    </div>

    <!-- 添加展开查看弹出层 -->
    <div class="quote-popup" v-if="isQuoteExpanded" @click.self="closeQuotePopup">
      <div class="quote-popup-content">
        <div class="quote-popup-close" @click="closeQuotePopup">×</div>
        <p class="quote-popup-text">{{ dailyQuote.content }}</p>
        <p class="quote-popup-author">—— {{ dailyQuote.author }}</p>
      </div>
    </div>

    <!-- 添加弹框组件 -->
    <DailyQuotePopup
      v-model:visible="showQuotePopup"
      :quote="dailyQuote"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, onUnmounted, onActivated, onDeactivated, watch, nextTick } from 'vue'
import {
  getFollowPictureUsingPost,
  getTop100PictureUsingGet,
  listPictureTagCategoryUsingGet,
  listPictureVoByPageUsingPost,
  getFeaturePictureUsingPost
} from '@/api/pictureController'
import { message } from 'ant-design-vue'
import PictureList from '@/components/PictureList.vue'
import { getDeviceType } from '@/utils/device'
import { DEVICE_TYPE_ENUM } from '@/constants/device'
import { useRoute, useRouter } from 'vue-router'
import { SearchOutlined, PlusOutlined, UploadOutlined, CalendarOutlined, TrophyOutlined, HeartOutlined, ClockCircleOutlined, StarOutlined, FireOutlined, AppstoreOutlined } from '@ant-design/icons-vue'
import { debounce } from 'lodash-es'
import MobilePictureList from '@/components/MobilePictureList.vue'
import WaterfallPictureList from '@/components/WaterfallPictureList.vue'
import { listCarouselActivitiesUsingPost } from '@/api/activityController'
import { formatTime } from '@/utils/time'
import { FAMOUS_QUOTES } from '@/constants/quotes'
import DailyQuotePopup from '@/components/DailyQuotePopup.vue'
import { CommentOutlined } from '@ant-design/icons-vue'

const router = useRouter()
const route = useRoute()
const isEndOfData = ref(false)
// 定义用于存储设备类型的响应式变量
const device = ref<string>('')
// 页面加载时获取设备类型并获取数据
onMounted(async () => {
  device.value = await getDeviceType()
  // 不论是PC还是移动端都需要获取轮播图数据
  await fetchCarouselActivities()
  if (device.value !== DEVICE_TYPE_ENUM.PC) {
    // 只有移动端需要初始化容器宽度
    containerWidth.value = document.querySelector('.carousel-container')?.clientWidth || 0

    // 添加触摸事件监听器
    document.addEventListener('touchstart', handleGlobalTouchStart)
    document.addEventListener('touchend', handleGlobalTouchEnd)
  }
  // 启动自动播放
  if (carouselActivities.value.length > 0) {
    startAutoplay()
  }

  // 添加时间更新定时器
  const timer = setInterval(updateTime, 1000)

  // 在组件卸载时清除定时器
  onUnmounted(() => {
    clearInterval(timer)
  })
})

// 定义PC端数据
const pcDataList = ref<API.PictureVO[]>([])
const total = ref(0)
const loading = ref(true)
const pcIsEndOfData = ref(false) // 新增 PC 端是否加载完所有数据的标记

// 定义移动端数据
const mobileDataList = ref<API.PictureVO[]>([])

// 搜索条件（PC端和移动端共用部分）
const searchParams = reactive<API.PictureQueryRequest>({
  current: 1,
  pageSize: 20, // 增加每页加载数量

})

// 新增用于移动端分页的页码变量
const page = ref(1)

// 获取标签和分类选项（PC端和移动端共用）
const getTagCategoryOptions = async () => {
    const res = await listPictureTagCategoryUsingGet()
    if (res.data.code === 0 && res.data.data) {
      tagList.value = res.data.data.tagList ?? []
      // PC端和移动端都添加"最新"分类
      if (device.value === DEVICE_TYPE_ENUM.PC) {
        categoryList.value = ['最新', ...(res.data.data?.categoryList || [])]
      } else {
        categoryList.value = ['精选', '最新', ...(res.data.data?.categoryList || [])]
      }
    } else {
      // message.error('获取标签分类列表失败，' + res.data.message)
    }
  }

;(() => {
  // console.log(mobileDataList.value.length)
  if (mobileDataList.value.length === 0 || pcDataList.value.length === 0) {
    // console.log('空数组刷新数据')
    loading.value = true
    // 更彻底地重置搜索参数（移动端和PC端共用部分）
    searchParams.searchText = ''
    searchParams.current = 1
    searchParams.pageSize = 25
    page.value = 1 // 确保移动端分页页码重置为初始值
    // 重新获取标签和分类选项以及数据
    getTagCategoryOptions()
      .then(() => {
        return fetchMobileData()
      })
      .then(() => {
        return fetchPcData()
      })
      .then(() => {
        // console.log(mobileDataList.value.length)
        loading.value = false
      })
      .catch((error) => {
        // console.error('数据获取过程出现错误:', error)
        loading.value = false
      })
  }
})()

// PC端获取数据函数
const fetchPcData = async () => {
  loading.value = true
  try {
    const params = {
      ...searchParams,
      tags: [] as string[],
    }
    if (selectedCategory.value !== 'all') {
      params.category = selectedCategory.value
    }
    selectedTagList.value.forEach((useTag, index) => {
      if (useTag) {
        params.tags.push(tagList.value[index])
      }
    })
    const res = await listPictureVoByPageUsingPost(params)
    if (res?.data?.code === 0 && res.data.data) {
      const data = res.data.data
      if ('records' in data) {
        pcDataList.value = data.records ?? []
        total.value = data.total ?? 0
        pcIsEndOfData.value = data.records.length < (params.pageSize || 20)
      } else {
        pcDataList.value = data as API.PictureVO[]
        total.value = data.length
        pcIsEndOfData.value = data.length < (params.pageSize || 20)
      }
    } else {
      // message.error('获取数据失败，' + res.data.message)
    }
  } catch (error) {
    // console.error('获取数据失败:', error)
    // message.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

// 移动端获取数据函数
// 移动端顶部导航激活标签
const activeTab = ref('all')

// 监听标签切换
watch(activeTab, async (newTab) => {
  // 先将当前内容淡出
  const container = document.querySelector('.mobile-list-container')
  if (container) {
    container.style.opacity = '0'
    container.style.transform = 'translateY(20px)'
  }

  // 等待淡出动画完成
  await new Promise(resolve => setTimeout(resolve, 300))

  // 重置数据和状态
  mobileDataList.value = []
  page.value = 1
  searchParams.current = 1
  isEndOfData.value = false
  loading.value = true

  try {
    let res
    if (newTab === 'all') {
      res = await fetchMobileData()
    } else if (newTab === 'following') {
      res = await fetchFollowData()
    } else if (newTab === 'ranking') {
      res = await fetchRankingData()
    }

    // 等待数据加载完成后，淡入新内容
    if (container) {
      setTimeout(() => {
        container.style.opacity = '1'
        container.style.transform = 'translateY(0)'
      }, 50)
    }
  } catch (error) {
    // 错误处理
  } finally {
    loading.value = false
  }
}, { immediate: true })

const fetchMobileData = async () => {
  loading.value = true
  try {
    const res = await listPictureVoByPageUsingPost(searchParams)
    if (res.data.code === 0 && res.data.data) {
      const newData = res.data.data.records ?? []
      // 为每个图片添加加载状态标记
      const processedData = newData.map((item) => ({
        ...item,
        loaded: false,
      }))
      mobileDataList.value = [...mobileDataList.value, ...processedData]
    }
  } catch (error) {
    // console.error('获取数据失败:', error)
  } finally {
    loading.value = false
  }
}

// PC端分页参数改变时触发
const onPageChange = (page: number, pageSize: number) => {
  if (device.value === DEVICE_TYPE_ENUM.PC) {
    searchParams.current = page
    searchParams.pageSize = pageSize
    fetchPcData()
  }
}
const fetchData = async () => {
  loading.value = true
  // 转换搜索参数
  const params = {
    ...searchParams,
    tags: [] as string[],
  }
  if (selectedCategory.value !== 'all') {
    params.category = selectedCategory.value
  }
  // [true, false, false] => ['java']
  selectedTagList.value.forEach((useTag, index) => {
    if (useTag) {
      params.tags.push(tagList.value[index])
    }
  })
  const res = await listPictureVoByPageUsingPost(params)
  if (res.data.code === 0 && res.data.data) {
    mobileDataList.value = res.data.data.records ?? []
    pcDataList.value = res.data.data.records ?? []
    total.value = res.data.data.total ?? 0
  } else {
    // message.error('获取数据失败，' + res.data.message)
  }
  loading.value = false
}
// 搜索操作触发（PC端和移动端共用）// 搜索
const doSearch = () => {
  // 重置数据
  resetPcData()
  // 重新加载数据
  fetchPcData()
}


const scroll = debounce(() => {
  const isLoading = ref(true)
  const throttledFetchData = debounce(async () => {
    if (isLoading.value) {
      page.value = page.value + 1
      searchParams.current = page.value
      let res = null
      if (activeTab.value === 'all') {
        if (selectedCategory.value === '精选') {
          // 精选分类使用精选 API
          res = await getFeaturePictureUsingPost({
            current: page.value,
            pageSize: 20
          })
        } else if (selectedCategory.value === '最新') {
          // 最新分类使用带排序的普通 API
          res = await listPictureVoByPageUsingPost({
            ...searchParams,
            sortField: 'createTime',
            sortOrder: 'desc'
          })
        } else {
          // 其他分类使用普通 API
          res = await listPictureVoByPageUsingPost(searchParams)
        }
      }
      if(activeTab.value==='following'){
        res = await getFollowPictureUsingPost(searchParams)
      }
      if (res.data.code === 0 && res.data.data) {
        const newData = res.data.data.records ?? []
        mobileDataList.value = [...mobileDataList.value, ...newData]
        isEndOfData.value = newData.length === 0
        isLoading.value = true
      } else {
        // message.error('获取数据失败，' + res.data.message)
        isLoading.value = false
      }
    }
  }, 1000) // 设置节流时间间隔为4000毫秒，即每4000毫秒内最多触发一次请求，可根据实际情况调整

  window.onscroll = () => {
    const bottomOfWindow =
      document.documentElement.offsetHeight -
      document.documentElement.scrollTop -
      window.innerHeight <=
      100
    if (bottomOfWindow) {
      throttledFetchData()
    }
  }
})

// 添加滚动位置记忆变量
const scrollPosition = ref(0)

// 修改现有的 onDeactivated 钩子
onDeactivated(() => {
  // 保存滚动位置
  scrollPosition.value = window.pageYOffset || document.documentElement.scrollTop
  if (device.value !== DEVICE_TYPE_ENUM.PC) {
    window.onscroll = null
  }
})

// 修改现有的 onActivated 钩子
onActivated(() => {
  if (device.value !== DEVICE_TYPE_ENUM.PC) {
    // 先恢复滚动位置
    nextTick(() => {
      window.scrollTo({
        top: scrollPosition.value,
        behavior: 'instant'
      })
    })
    // 然后重新绑定滚动监听
    scroll()
  }
})

// 页面加载时设置滚动分页监听（移动端）
onMounted(() => {
  if (device.value !== DEVICE_TYPE_ENUM.PC) {
    scroll()
  }
})

// 组件卸载时移除滚动监听
onUnmounted(() => {
  if (device.value !== DEVICE_TYPE_ENUM.PC) {
    window.onscroll = null
  }

  if (device.value !== DEVICE_TYPE_ENUM.PC) {
    document.removeEventListener('touchstart', handleGlobalTouchStart)
    document.removeEventListener('touchend', handleGlobalTouchEnd)
  }
})

// 标签和分类列表
const categoryList = ref<string[]>([])
const selectedCategory = ref<string>('all')
const tagList = ref<string[]>([])
const selectedTagList = ref<boolean[]>([])

// 下拉刷新处理函数
const onRefresh = async () => {
  loading.value = true;
  try {
    // 重置搜索参数
    searchParams.searchText = '';
    searchParams.current = 1;
    searchParams.pageSize = 25;
    searchParams.category = '';
    page.value = 1;

    let res;
    switch (activeTab.value) {
      case 'all':
        // 重置到"全部"标签
        activeTab.value = 'all';
        selectedCategory.value = '';
        categoryList.value = [];
        // 重新获取标签和分类选项以及数据
        await getTagCategoryOptions();
        res = await listPictureVoByPageUsingPost(searchParams);
        break;
      case 'following':
        res = await getFollowPictureUsingPost(searchParams);
        break;
      case 'ranking':
        res = await getTop100PictureUsingGet({ id: rankingType.value });
        break;
    }

    if (res && res.data.code === 0) {
      if (activeTab.value === 'ranking') {
        mobileDataList.value = res.data.data || [];
      } else {
        mobileDataList.value = res.data.data?.records || [];
      }
      isEndOfData.value = false;
      message.success('刷新成功');
    } else if (res) {
      // message.error('刷新失败，' + res.data.message);
    }
  } catch (error) {
    // console.error('刷新数据出错:', error);
    // message.error('刷新失败，请稍后重试');
  } finally {
    loading.value = false;
  }
};

// 添加搜索框固定状态
const isSearchFixed = ref(false)

// 监听路由参数变化
watch(
  () => route.query.refresh,
  async (newVal) => {
    if (newVal === 'true') {
      // 重置数据
      page.value = 1
      searchParams.current = 1
      isEndOfData.value = false
      mobileDataList.value = []
      pcDataList.value = []

      // 重新获取数据
      if (device.value === DEVICE_TYPE_ENUM.PC) {
        await fetchPcData()
      } else {
        await fetchMobileData()
      }
    }
  },
)


// 榜单相关变量
const rankingType = ref(3) // 默认周榜
const rankingTabs = [
  { name: '日榜', value: 1 },
  { name: '周榜', value: 2 },
  { name: '月榜', value: 3 },
  { name: '总榜', value: 4 }
]

// 获取榜单数据
const fetchRankingData = async () => {
  try {
    const res = await getTop100PictureUsingGet({
      id: rankingType.value
    })
    if (res.data.code === 0) {
      // 直接替换数据而不是追加
      mobileDataList.value = res.data.data || []
      loading.value = false
      isEndOfData.value = true
    } else {
      // message.error('获取榜单数据失败：' + res.data.message)
    }
  } catch (error) {
    // console.error('获取榜单数据出错:', error)
    // message.error('获取榜单数据失败，请稍后重试')
    throw error // 向上抛出错误以便统一处理
  }
}

// 修改榜单类型切换的处理函数
const handleRankingTypeChange = async (type: number) => {
  // 保存新的榜单类型
  rankingType.value = type
  loading.value = true

  try {
    if (device.value === DEVICE_TYPE_ENUM.PC) {
      // PC端处理
      pcDataList.value = []
      pcIsEndOfData.value = false
      const res = await getTop100PictureUsingGet({ id: type })
      if (res.data?.code === 0) {
        pcDataList.value = res.data.data || []
      }
    } else {
      // 移动端处理
      mobileDataList.value = []
      await fetchRankingData()
    }
  } catch (error) {
    // console.error('切换榜单类型失败:', error)
    // message.error('获取榜单数据失败，请重试')
  } finally {
    loading.value = false
  }
}

//获取关注数据
const fetchFollowData = async () => {
  try {
    const res = await getFollowPictureUsingPost(searchParams)
    if (res.data.code === 0 && res.data.data) {
      mobileDataList.value = res.data.data.records ?? []
      isEndOfData.value = res.data.data.records.length === 0
    } else {
      // message.error('获取数据失败，' + res.data.message)
    }
  }catch (error) {
    // console.error('获取关注数据出错:', error)
    throw error // 向上抛出错误以便统一处理
  }
}
// 修改分类标签切换处理函数
const handleCategoryChange = async (category: string) => {
  if (activeTab.value === 'all') {
    loading.value = true
    mobileDataList.value = []
    page.value = 1
    searchParams.current = 1
    isEndOfData.value = false

    if (category === '精选') {
      // 调用精选接口，使用正确的参数格式
      const res = await getFeaturePictureUsingPost({
        current: page.value,
        pageSize: 20
      })
      if (res?.data?.code === 0) {
        if (device.value === DEVICE_TYPE_ENUM.PC) {
          pcDataList.value = res.data.data.records || []
          total.value = res.data.data.total
          pcIsEndOfData.value = (res.data.data.records || []).length < searchParams.pageSize
        } else {
          mobileDataList.value = res.data.data.records || []
          isEndOfData.value = (res.data.data.records || []).length < searchParams.pageSize
        }
      }
    } else if (category === '最新') {  // 修改这里，使用中文"最新"而不是'latest'
      // 获取最新内容
      const res = await listPictureVoByPageUsingPost({
        ...searchParams,
        sortField: 'createTime',
        sortOrder: 'desc'
      })
      if (res?.data?.code === 0 && res.data.data) {  // 添加状态码检查
        if (device.value === DEVICE_TYPE_ENUM.PC) {
          pcDataList.value = res.data.data.records || []
          total.value = res.data.data.total
          pcIsEndOfData.value = res.data.data.current >= res.data.data.pages
        } else {
          mobileDataList.value = res.data.data.records || []
          isEndOfData.value = res.data.data.current >= res.data.data.pages
        }
      } else {
        message.error('获取最新内容失败：' + (res?.data?.message || '未知错误'))
      }
    } else {
      // 调用普通分类接口
      searchParams.category = category === 'all' ? undefined : category
      try {
        await fetchMobileData()
      } catch (error) {
        // console.error('切换分类失败:', error)
        // message.error('切换分类失败，请稍后重试')
      }
    }
    loading.value = false
  }
}

// 加载更多图片
const loadMorePictures = async (nextPage: number) => {
  if (pcIsEndOfData.value) return false
  if (loading.value) return false

  loading.value = true
  try {
    let res;
    // 根据当前标签页加载不同的数据
    switch (activeTab.value) {
      case 'following':
        res = await getFollowPictureUsingPost({
          ...searchParams,
          current: nextPage,
          pageSize: 20
        })
        break
      case 'ranking':
        // 榜单数据不需要分页加载
        return false
      case 'all':
      default:
        res = await listPictureVoByPageUsingPost({
          ...searchParams,
          current: nextPage,
          pageSize: 20
        })
        break
    }

    if (res.data?.code === 0 && res.data.data) {
      // 处理不同类型的响应数据
      let newData = []
      if (activeTab.value === 'ranking') {
        newData = res.data.data || []
      } else {
        newData = res.data.data.records || []
      }

      if (newData.length > 0) {
        await nextTick(() => {
          pcDataList.value = [...pcDataList.value, ...newData]
          // 只有在全部标签页时才更新总数
          if (activeTab.value === 'all') {
            total.value = res.data.data.total ?? 0
          }
          pcIsEndOfData.value = newData.length < 20
        })
        return true
      } else {
        pcIsEndOfData.value = true
      }
    }
    return false
  } catch (error) {
    console.error('加载更多图片失败:', error)
    return false
  } finally {
    loading.value = false
  }
}

// 重置 PC 端数据的方法
const resetPcData = () => {
  pcDataList.value = []
  searchParams.current = 1
  pcIsEndOfData.value = false
}

const isSearchTransitioning = ref(false)
// 处理搜索点击
const handleSearchClick = () => {
  isSearchTransitioning.value = true

  // 获取搜索框元素
  const searchBar = document.querySelector('.search-bar') as HTMLElement
  const searchWrapper = document.querySelector('.search-wrapper') as HTMLElement

  if (searchBar && searchWrapper) {
    // 第一阶段：搜索框缩小并上移
    searchBar.style.transition = 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)'
    searchBar.style.transform = 'scale(0.9) translateY(-20px)'
    searchBar.style.opacity = '0.8'

    // 第二阶段：背景区域收缩
    searchWrapper.style.transition = 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)'
    searchWrapper.style.transform = 'scaleY(0.8)'
    searchWrapper.style.opacity = '0.6'
  }

  // 第三阶段：完成动画并跳转
  setTimeout(() => {
    if (searchBar && searchWrapper) {
      searchBar.style.transform = 'scale(0.8) translateY(-40px)'
      searchBar.style.opacity = '0'
      searchWrapper.style.transform = 'scaleY(0)'
      searchWrapper.style.opacity = '0'
    }

    // 导航跳转
    router.push('/search')
  }, 300)
}


// 监听路由变化，重置过渡状态
// 监听路由变化，重置过渡状态
watch(
  () => route.path,
  () => {
    isSearchTransitioning.value = false
  }
)

// 监听 activeTab 变化
watch(activeTab, async (newTab) => {
  // 重置数据
  if (device.value === DEVICE_TYPE_ENUM.PC) {
    pcDataList.value = []
    pcIsEndOfData.value = false
    loading.value = true

    try {
      if (newTab === 'all') {
        // 获取全部数据
        const res = await listPictureVoByPageUsingPost(searchParams)
        if (res.data?.code === 0 && res.data.data) {
          pcDataList.value = res.data.data.records || []
          total.value = res.data.data.total ?? 0
        }
      } else if (newTab === 'following') {
        // 获取关注数据
        const res = await getFollowPictureUsingPost(searchParams)
        if (res.data?.code === 0 && res.data.data) {
          pcDataList.value = res.data.data.records || []
          total.value = res.data.data.total ?? 0
        }
      } else if (newTab === 'ranking') {
        // 获取榜单数据
        const res = await getTop100PictureUsingGet({ id: rankingType.value })
        if (res.data?.code === 0) {
          pcDataList.value = res.data.data || []
          total.value = res.data.data.length ?? 0
          pcIsEndOfData.value = true // 榜单数据不需要分页
        }
      }
    } catch (error) {
      // console.error('切换标签页失败:', error)
      // message.error('获取数据失败，请重试')
    } finally {
      loading.value = false
    }
  }
})

// 活动轮播图数据
const carouselActivities = ref([])
const currentActivityIndex = ref(0)
const autoplayInterval = ref(null)

// 获取轮播图活动数据
const fetchCarouselActivities = async () => {
  try {
    const res = await listCarouselActivitiesUsingPost({
      pageSize: 20,
      current: 1,
    })
    if (res.data?.code === 0 && res.data.data) {
      carouselActivities.value = res.data.data.records || []
      if (carouselActivities.value.length > 0) {
        startAutoplay()
      }
    }
  } catch (error) {
    console.error('获取轮播活动失败:', error)
  }
}

// 修改自动播放相关逻辑
const startAutoplay = () => {
  if (autoplayInterval.value) {
    clearInterval(autoplayInterval.value)
  }
  autoplayInterval.value = setInterval(() => {
    if (carouselActivities.value.length > 0) {
      currentActivityIndex.value = (currentActivityIndex.value + 1) % carouselActivities.value.length
    }
  }, 3000)
}

// 停止自动播放
const stopAutoplay = () => {
  if (autoplayInterval.value) {
    clearInterval(autoplayInterval.value)
    autoplayInterval.value = null
  }
}

// 轮播图触摸相关变量
const touchStartX = ref(0)
const touchStartY = ref(0)
const touchEndX = ref(0)
const touchEndY = ref(0)
const isSwipeLocked = ref(false)
const translateX = ref(0)
const containerWidth = ref(0)

// 添加触摸开始事件处理
const handleGlobalTouchStart = (e: TouchEvent) => {
  // 只在主页路由下启用左右滑动
  if (route.path !== '/' && route.path !== '/home') {
    return
  }
  touchStartX.value = e.touches[0].clientX
  touchStartY.value = e.touches[0].clientY
}

// 添加触摸结束事件处理
const handleGlobalTouchEnd = (e: TouchEvent) => {
  // 只在主页路由下启用左右滑动
  if (route.path !== '/' && route.path !== '/home') {
    return
  }

  if (isSwipeLocked.value) return

  touchEndX.value = e.changedTouches[0].clientX
  touchEndY.value = e.changedTouches[0].clientY

  const deltaX = touchEndX.value - touchStartX.value
  const deltaY = touchEndY.value - touchStartY.value

  // 如果水平滑动距离大于垂直滑动距离，且滑动距离超过50像素
  if (Math.abs(deltaX) > Math.abs(deltaY) && Math.abs(deltaX) > 50) {
    // 防止连续滑动
    isSwipeLocked.value = true
    setTimeout(() => {
      isSwipeLocked.value = false
    }, 500)

    const tabs = ['all', 'following', 'ranking']
    const currentIndex = tabs.indexOf(activeTab.value)

    if (deltaX > 0 && currentIndex > 0) {
      // 右滑，切换到前一个标签
      activeTab.value = tabs[currentIndex - 1]
    } else if (deltaX < 0 && currentIndex < tabs.length - 1) {
      // 左滑，切换到后一个标签
      activeTab.value = tabs[currentIndex + 1]
    }
  }
}

// 处理触摸开始
const handleTouchStart = (e: TouchEvent) => {
  touchStartX.value = e.touches[0].clientX
  containerWidth.value = document.querySelector('.carousel-container')?.clientWidth || 0
}

// 处理触摸移动
const handleTouchMove = (e: TouchEvent) => {
  if (touchStartX.value === 0) return

  const currentX = e.touches[0].clientX
  const diff = currentX - touchStartX.value

  // 计算当前位置，考虑边界情况
  const maxTranslate = -(carouselActivities.value.length - 1) * containerWidth.value
  let newTranslate = -(currentActivityIndex.value * containerWidth.value) + diff

  // 减小阻尼效果系数，让滑动更轻松
  if (newTranslate > 0) {
    newTranslate = newTranslate * 0.6
  } else if (newTranslate < maxTranslate) {
    const overDrag = newTranslate - maxTranslate
    newTranslate = maxTranslate + overDrag * 0.6
  }

  translateX.value = newTranslate
}

// 处理触摸结束
const handleTouchEnd = (e: TouchEvent) => {
  if (touchStartX.value === 0) return

  const currentX = e.changedTouches[0].clientX
  const diff = currentX - touchStartX.value

  // 降低滑动阈值，让切换更容易触发
  if (Math.abs(diff) > containerWidth.value * 0.15) {
    if (diff > 0 && currentActivityIndex.value > 0) {
      // 向右滑，显示上一张
      currentActivityIndex.value--
    } else if (diff < 0 && currentActivityIndex.value < carouselActivities.value.length - 1) {
      // 向左滑，显示下一张
      currentActivityIndex.value++
    }
  }

  // 重置触摸起始位置
  touchStartX.value = 0
  // 重置位置到当前活动项，使用更快的动画速度
  translateX.value = -(currentActivityIndex.value * containerWidth.value)
}

// 修改活动点击处理方法
const handleActivityClick = (id: string) => {
  router.push(`/activity/detail/${id}`)
}

// 修改 watch 以更新 translateX
watch(currentActivityIndex, (newIndex) => {
  if (containerWidth.value === 0) {
    containerWidth.value = document.querySelector('.carousel-container')?.clientWidth || 0
  }
  translateX.value = -(newIndex * containerWidth.value)
})

// 分类点击处理
const handleCategoryClick = async (category: string) => {
  selectedCategory.value = category
  loading.value = true
  searchParams.current = 1

  try {
    if (category === '最新') {  // 修改这里，使用中文"最新"而不是'latest'
      // 获取最新内容
      const res = await listPictureVoByPageUsingPost({
        ...searchParams,
        sortField: 'createTime',
        sortOrder: 'desc'
      })
      if (res?.data?.code === 0 && res.data.data) {  // 添加状态码检查
        if (device.value === DEVICE_TYPE_ENUM.PC) {
          pcDataList.value = res.data.data.records || []
          total.value = res.data.data.total
          pcIsEndOfData.value = res.data.data.current >= res.data.data.pages
        } else {
          mobileDataList.value = res.data.data.records || []
          isEndOfData.value = res.data.data.current >= res.data.data.pages
        }
      } else {
        message.error('获取最新内容失败：' + (res?.data?.message || '未知错误'))
      }
    } else {
      const res = await listPictureVoByPageUsingPost({
        ...searchParams,
        category: category === 'all' ? undefined : category
      })
      if (res?.data?.code === 0 && res.data.data) {  // 添加状态码检查
        if (device.value === DEVICE_TYPE_ENUM.PC) {
          pcDataList.value = res.data.data.records || []
          total.value = res.data.data.total
          pcIsEndOfData.value = res.data.data.current >= res.data.data.pages
        } else {
          mobileDataList.value = res.data.data.records || []
          isEndOfData.value = res.data.data.current >= res.data.data.pages
        }
      } else {
        message.error('获取内容失败：' + (res?.data?.message || '未知错误'))
      }
    }
  } catch (error) {
    console.error('获取数据失败:', error)
    message.error('获取数据失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const timeCount = ref(0);
if (FAMOUS_QUOTES.length > 0) {
  timeCount.value = Math.floor(Math.random() * FAMOUS_QUOTES.length);
}

// 每日一句数据
const dailyQuote = ref({
  content: FAMOUS_QUOTES[timeCount.value].content,
  author: FAMOUS_QUOTES[timeCount.value].author,
  english: FAMOUS_QUOTES[timeCount.value].english,
  imageUrl: 'https://yuemu-picture-1328106169.cos.ap-chongqing.myqcloud.com/public/1910505702887313409/2025-04-13_zK4t9VyYPvzKfLOk.webp',
  updateTime: new Date().toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
})
const isQuoteExpanded = ref(false)

const showQuotePopup = ref(false)

const handleQuoteClick = () => {
  showQuotePopup.value = true
}

// 关闭名言展开框
const closeQuotePopup = () => {
  isQuoteExpanded.value = false
}

// PC端轮播图相关变量
const mouseStartX = ref(0)
const pcTranslateX = ref(0)
const pcContainerWidth = ref(0)

// 处理鼠标按下
const handleMouseDown = (e: MouseEvent) => {
  mouseStartX.value = e.clientX
  pcContainerWidth.value = document.querySelector('.pc-carousel .carousel-container')?.clientWidth || 0
}

// 处理鼠标移动
const handleMouseMove = (e: MouseEvent) => {
  if (mouseStartX.value === 0) return

  const currentX = e.clientX
  const diff = currentX - mouseStartX.value

  // 计算当前位置，考虑边界情况
  const maxTranslate = -(carouselActivities.value.length - 1) * pcContainerWidth.value
  let newTranslate = -(currentActivityIndex.value * pcContainerWidth.value) + diff

  // 添加阻尼效果
  if (newTranslate > 0) {
    newTranslate = newTranslate * 0.3
  } else if (newTranslate < maxTranslate) {
    const overDrag = newTranslate - maxTranslate
    newTranslate = maxTranslate + overDrag * 0.3
  }

  pcTranslateX.value = newTranslate
}

// 处理鼠标松开
const handleMouseUp = (e: MouseEvent) => {
  if (mouseStartX.value === 0) return

  const currentX = e.clientX
  const diff = currentX - mouseStartX.value

  // 判断滑动方向和距离是否足够切换图片
  if (Math.abs(diff) > pcContainerWidth.value * 0.3) {
    if (diff > 0 && currentActivityIndex.value > 0) {
      // 向右滑，显示上一张
      currentActivityIndex.value--
    } else if (diff < 0 && currentActivityIndex.value < carouselActivities.value.length - 1) {
      // 向左滑，显示下一张
      currentActivityIndex.value++
    }
  }

  // 重置鼠标起始位置
  mouseStartX.value = 0
  // 重置位置到当前活动项
  pcTranslateX.value = -(currentActivityIndex.value * pcContainerWidth.value)
}

// 修改 watch 以更新 pcTranslateX
watch(currentActivityIndex, (newIndex) => {
  if (pcContainerWidth.value === 0) {
    pcContainerWidth.value = document.querySelector('.pc-carousel .carousel-container')?.clientWidth || 0
  }
  pcTranslateX.value = -(newIndex * pcContainerWidth.value)
})

// 添加跳转到弹幕墙的方法
const goToBarrage = () => {
  router.push('/barrage')
}

// 添加当前时间的响应式变量和更新函数
const currentTime = ref(new Date().toLocaleTimeString())
const updateTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  })
}

// 在组件挂载时启动时间更新
onMounted(() => {
  // 原有的 onMounted 逻辑
  device.value = getDeviceType()
  fetchCarouselActivities()
  if (device.value !== DEVICE_TYPE_ENUM.PC) {
    containerWidth.value = document.querySelector('.carousel-container')?.clientWidth || 0
  }
  if (carouselActivities.value.length > 0) {
    startAutoplay()
  }

  // 添加时间更新定时器
  const timer = setInterval(updateTime, 1000)

  // 在组件卸载时清除定时器
  onUnmounted(() => {
    clearInterval(timer)
  })
})
</script>

<style scoped>
#homePage {
  margin-bottom: 16px;
  margin-top: -24px;
  background: #ffffff;
  position: relative;
}

/* PC端搜索框样式 */
.search-wrapper {
  padding: 8px 0;
  padding-bottom: 24px;
  background: linear-gradient(180deg, #fff 0%, #f8fafc 100%);
  border-bottom: 1px solid #f1f5f9;
  margin-top: -8px;
  will-change: transform;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.search-bar {
  max-width: 480px;
  margin: 0 auto;
  padding: 0 16px;
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
  background: #ff8e53;
  border-color: #ff8e53;
  box-shadow: none;
}

:deep(.ant-input-search .ant-btn:hover) {
  background: #ff7a33;
  border-color: #ff7a33;
}

.search-icon {
  color: #114da1;
  font-size: 16px;
}

/* 分类和标签区域 */
.filter-section {
  padding: 6px 6px 6px;
  background: white;
  border-bottom: 1px solid #f1f5f9;
}

.m-filter-section{
  padding: 6px 0 6px 24px;
  background: white;
  border-bottom: 1px solid #f1f5f9;
}

/* 分类标签样式 */
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

  &:hover {
    color: #ff8e53;
  }
}

:deep(.category-tabs .ant-tabs-tab-active) {
  background: rgba(255, 142, 83, 0.1);
  border-radius: 16px;
  color: #ff8e53 !important;
  font-weight: 500;
}

:deep(.category-tabs .ant-tabs-tab-btn) {
  color: inherit;
}

:deep(.category-tabs .ant-tabs-tab-active .ant-tabs-tab-btn) {
  color: #ff8e53 !important;
}

:deep(.category-tabs .ant-tabs-ink-bar) {
  display: none;
}

/* 标签样式 */
.tag-bar {
  padding: 4px 0;
}

.custom-tag {
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 13px;
  background: #f1f5f9;
  border: none;
  color: #64748b;
  transition: all 0.3s ease;
}

.custom-tag:hover {
  background: #e2e8f0;
  color: #3b82f6;
}

:deep(.ant-tag-checkable-checked) {
  background: #3b82f6 !important;
  color: white !important;
}

/* 移动端搜索框样式 */
.mobile-search {
  padding-top: 0;
  background: white;
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
  top: 8px !important;
  right: 48px !important;
  width: 96px !important;
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
    0 2px 4px rgba(255, 142, 83, 0.05) !important;
  backdrop-filter: blur(8px) !important;
  border: 1px solid rgba(255, 142, 83, 0.1) !important;
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
  width: 96px !important;
  transform: scale(1) !important;
  height: 32px;
  line-height: 32px;
  box-shadow:
    0 6px 20px rgba(0, 0, 0, 0.08),
    0 2px 8px rgba(255, 142, 83, 0.08) !important;
  border: 1px solid rgba(255, 142, 83, 0.15) !important;
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
    0 2px 8px rgba(255, 142, 83, 0.12),
    0 1px 4px rgba(0, 0, 0, 0.04) !important;
}

/* 占位符样式 */
.mobile-search :deep(.ant-input::placeholder) {
  color: #94a3b8 !important;
  font-size: 13px !important;
}

/* 新增加载更多样式 */
.loading-more {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px 0;
}

.loading-spinner {
  width: 36px;
  height: 36px;
  animation: spinner-rotate 2s linear infinite;
}

.path {
  stroke: #ff8e53;
  stroke-linecap: round;
  animation: spinner-dash 1.5s ease-in-out infinite;
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
}

/* PC端内容区域 */
.pc-content {
  margin-top: 16px;
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

/* 移动端内容区域 */
.mobile-content {
  margin-top: 12px;
}

/* 移动端分类和标签样式 */
.mobile-categories,
.mobile-tags {
  padding: 12px 16px;

  .category-title,
  .tag-title {
    font-size: 14px;
    color: #1a1a1a;
    margin-bottom: 12px;
    font-weight: 500;
  }

  .category-list,
  .tag-list {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  .category-item,
  .tag-item {
    font-size: 13px;
    padding: 6px 12px;
    border-radius: 16px;
    background: #f8fafc;
    color: #64748b;
    transition: all 0.3s ease;
  }
}

/* 分页器样式美化 */
.pagination-wrapper {
  margin-top: 32px;
  padding-bottom: 12px;
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
  .ant-pagination-item {
    border-radius: 8px;
    transition: all 0.3s ease;

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

  .ant-select-selector {
    border-radius: 8px !important;
    transition: all 0.3s ease;

    &:hover {
      border-color: #ff8e53 !important;
    }
  }

  .ant-select-focused .ant-select-selector {
    border-color: #ff8e53 !important;
    box-shadow: 0 0 0 2px rgba(255, 142, 83, 0.1) !important;
  }

  .ant-pagination-options-quick-jumper {
    input {
      border-radius: 8px;
      transition: all 0.3s ease;

      &:hover,
      &:focus {
        border-color: #ff8e53;
      }

      &:focus {
        box-shadow: 0 0 0 2px rgba(255, 142, 83, 0.1);
      }
    }
  }
}

.mobile-list-container{
  margin-top: 90px;

}
/* 移动端顶部导航样式 */
.mobile-nav {
  position: fixed !important;
  top: 10px !important;
  left: 12px !important;
  width: 100%;
  height: 64px !important;
  padding-top: 16px;
  transform: translateY(0) !important;
  opacity: 1 !important;
  margin: -16px -16px 0;
  padding-bottom: 10px;
  background: white;
  z-index: 2;
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
  color: #ff8e53;
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
  padding: 8px 16px;
  background: white;
  border-bottom: 1px solid #f1f5f9;
}

.ranking-tabs :deep(.van-tabs__wrap) {
  height: 40px;
}

.ranking-tabs :deep(.van-tabs__nav) {
  background: #f8fafc;
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
  color: #ff8e53;
  font-weight: 500;
  font-size: 11px;
  background: linear-gradient(135deg, #fff6f3 0%, #fff 100%);
  box-shadow:
    0 2px 8px rgba(255, 142, 83, 0.1),
    0 1px 4px rgba(255, 142, 83, 0.05);
}

/* 添加点击效果 */
.ranking-tabs :deep(.van-tab)::before {
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

.ranking-tabs :deep(.van-tab:active)::before {
  opacity: 0.1;
}

/* 隐藏底部线条 */
.ranking-tabs :deep(.van-tabs__line) {
  display: none;
}

/* 添加渐变背景效果 */
.ranking-tabs :deep(.van-tab--active) {
  background: linear-gradient(135deg, #fff6f3 0%, #fff 100%);
  box-shadow:
    0 2px 8px rgba(255, 142, 83, 0.1),
    0 1px 4px rgba(255, 142, 83, 0.05);
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
  background: linear-gradient(135deg, #ff8e53 0%, #ff6b6b 100%);
  border: none;
  color: white;
  font-weight: 500;
  box-shadow: 0 4px 12px rgba(255, 107, 107, 0.2);
  transition: all 0.3s ease;
}

.discover-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(255, 107, 107, 0.3);
}

.discover-btn:active {
  transform: translateY(1px);
}

/* PC端搜索框过渡动画 */
.search-wrapper {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.search-transitioning {
  transform: translateY(20px);
  opacity: 0;
}

.search-bar {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.search-bar-transitioning {
  transform: scale(1.05);
  opacity: 0;
}

/* 移动端搜索框过渡动画 */
.mobile-search {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.mobile-search-transitioning {
  transform: scale(1.1) translateY(-10px);
  opacity: 0;
}

/* 搜索框样式优化 */
.search-bar {
  cursor: pointer;
  transform-origin: center center;

  &:hover {
    transform: translateY(-1px);
  }

  &:active {
    transform: scale(0.98);
  }
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
      background: rgba(255, 255, 255, 0.98) !important;
      box-shadow:
        0 2px 8px rgba(255, 142, 83, 0.12),
        0 1px 4px rgba(0, 0, 0, 0.04) !important;
    }
  }
}

/* 搜索按钮样式 */
.search-button {
  border: none;
  background: rgba(255, 142, 83, 0.1);
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

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: linear-gradient(45deg, rgba(255, 142, 83, 0.1), rgba(255, 107, 107, 0.2));
    opacity: 0;
    transition: opacity 0.3s ease;
  }

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 2px 8px rgba(255, 142, 83, 0.2);

    .search-icon {
      transform: rotate(-5deg) scale(1.1);
    }

    .search-text {
      transform: translateX(2px);
    }
  }

  &:active {
    transform: translateY(0);
    box-shadow: 0 1px 4px rgba(255, 142, 83, 0.1);
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
  color: #ff8e53;
  font-size: 16px;
  opacity: 0.8;
  transition: all 0.3s ease;
}

.search-divider {
  color: rgba(255, 142, 83, 0.3);
  font-size: 14px;
  transform: scale(0.9);
  margin: 0 -1px;
}

.search-text {
  color: #ff8e53;
  font-size: 13px;
  opacity: 0.8;
  transition: all 0.3s ease;
  font-weight: 500;
}

/* 标签样式优化 */
.mobile-nav :deep(.van-tabs__wrap) {
  padding: 0 16px;

  .van-tabs__nav {
    background: #f8fafc;
    border-radius: 20px;
    height: 24px;
  }

  .van-tab {
    flex: 1;
    min-width: 80px;
    height: 40px;
    line-height: 40px;
    font-size: 14px;
    color: #64748b;
    border-radius: 16px;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    position: relative;
    padding: 0;

    &--active {
      color: #ff8e53;
      font-weight: 500;
      background: linear-gradient(135deg, #fff6f3 0%, #fff 100%);
      box-shadow:
        0 2px 8px rgba(255, 142, 83, 0.1),
        0 1px 4px rgba(255, 142, 83, 0.05);
      backdrop-filter: blur(4px);
    }

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
  }

  .van-tabs__line {
    display: none;
  }
}

.pc-category-item,
.pc-ranking-item {
  padding: 2px 12px;
  font-size: 13px;
  color: #64748b;
  white-space: nowrap;
  cursor: pointer;
  transition: all 0.3s ease;
  border-radius: 16px;
  background: rgba(24, 144, 255, 0.05);
  margin: 0 4px;

  &:hover {
    color: #ff8e53;
    background: rgba(24, 144, 255, 0.08);
  }

  &.active {
    color: #ff8e53;
    font-weight: 500;
    background: linear-gradient(135deg, rgba(199, 164, 129, 0.12), rgba(207, 131, 54, 0.15));
    box-shadow:
      0 2px 8px rgba(192, 153, 100, 0.1),
      0 1px 4px rgba(24, 144, 255, 0.05);
  }
}

/* 分类导航样式 */
.pc-category-nav,
.pc-ranking-nav {
  height: 36px;
  background: #fff;
  display: flex;
  align-items: center;
  padding: 0 12px 1px;
  overflow-x: auto;
  border-bottom: 1px solid #f0f0f0;
  z-index: 99;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: none;
  margin-bottom: 16px;
  border-radius: 12px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);

  &::-webkit-scrollbar {
    display: none;
  }
}

.pc-content {
  background: white;
  border-radius: 12px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  padding: 24px;
}

.pc-nav {
  background: white;
  border-radius: 12px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  margin-bottom: 16px;

  :deep(.ant-tabs-nav) {
    margin: 0;
    padding: 0 24px;
  }

  :deep(.ant-tabs-tab) {
    padding: 16px 24px;
    font-size: 16px;
    transition: all 0.3s;
    margin: 0;

    &:hover {
      color: #ff8e53;
    }

    &.ant-tabs-tab-active {
      .ant-tabs-tab-btn {
        color: #ff8e53;
        font-weight: 500;
      }
    }
  }

  :deep(.ant-tabs-ink-bar) {
    background: #ff8e53;
  }
}

/* 移动端活动轮播图样式 */
.activity-carousel {
  padding-top: 98px;
  margin: -8px -22px 8px !important;
  background: #fff;
  position: relative;
}

.activity-carousel .carousel-container {
  height: 240px;
  border-radius: 16px;
  overflow: hidden;
  position: relative;
}

.activity-carousel .carousel-wrapper {
  display: flex;
  height: 100%;
  transition: transform 0.2s cubic-bezier(0.33, 1, 0.68, 1);
  will-change: transform;
}

.activity-carousel .carousel-item {
  flex: 0 0 100%;
  position: relative;
  width: 100%;
  height: 100%;
}

.activity-carousel .carousel-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.activity-carousel .carousel-info {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 12px 16px;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.7), transparent);
  color: #fff;
}

.activity-carousel .carousel-title {
  font-size: 16px;
  font-weight: 500;
  margin: 0 0 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.activity-carousel .carousel-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}

.activity-carousel .carousel-status {
  padding: 2px 6px;
  border-radius: 10px;
  background: rgba(255, 142, 83, 0.9);
  font-weight: 500;
}

.activity-carousel .carousel-status.expired {
  background: rgba(255, 77, 79, 0.9);
}

.activity-carousel .carousel-date {
  opacity: 0.9;
}

.activity-carousel .carousel-indicators {
  position: absolute;
  bottom: 12px;
  left: 0;
  right: 0;
  display: flex;
  justify-content: center;
  gap: 8px;
  z-index: 1;
}

.activity-carousel .indicator-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  transition: all 0.3s ease;
}

.activity-carousel .indicator-dot.active {
  width: 18px;
  border-radius: 3px;
  background: #ff8e53;
}

/* PC端顶部布局样式 */
.pc-top-layout {
  display: flex;
  gap: 24px;
  margin: 24px 24px;
  width: calc(100% - 48px);
  overflow: visible;  /* 确保内容可以溢出 */
  height: 480px;  /* 固定高度 */
}

/* PC端轮播图区域样式 */
.pc-carousel-section {
  width: calc(60% - 12px);
  height: 100%;
  border-radius: 16px;
  overflow: hidden;
  background: #fff;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  display: flex;
  flex-direction: column;
}

/* PC端轮播图样式 */
.pc-carousel {
  width: 100%;
  height: 100%;
  border-radius: 16px;
  overflow: hidden;
  background: #fff;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
  position: relative;
}

.pc-carousel .carousel-container {
  width: 100%;
  height: 100%;
  position: relative;
  overflow: hidden;
}

.pc-carousel .carousel-wrapper {
  display: flex;
  height: 100%;
  width: 100%;
  transition: transform 0.3s ease-in-out;
}

.pc-carousel .carousel-item {
  flex: 0 0 100%;
  width: 100%;
  height: 100%;
  position: relative;
  cursor: pointer;
  overflow: hidden;
}

.pc-carousel .carousel-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.pc-carousel .carousel-item:hover .carousel-image {
  transform: scale(1.05);
}

.pc-carousel .carousel-info {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 24px;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.8), transparent);
  color: #fff;
}

.pc-carousel .carousel-title {
  font-size: 20px;
  font-weight: 500;
  margin: 0 0 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pc-carousel .carousel-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
}

.pc-carousel .carousel-status {
  padding: 4px 12px;
  border-radius: 16px;
  background: rgba(255, 142, 83, 0.9);
  font-weight: 500;
  backdrop-filter: blur(4px);
}

.pc-carousel .carousel-status.expired {
  background: rgba(255, 77, 79, 0.9);
}

.pc-carousel .carousel-indicators {
  position: absolute;
  bottom: 20px;
  left: 0;
  right: 0;
  display: flex;
  justify-content: center;
  gap: 8px;
  z-index: 1;
}

.pc-carousel .indicator-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  transition: all 0.3s ease;
}

.pc-carousel .indicator-dot.active {
  width: 18px;
  border-radius: 3px;
  background: #ff8e53;
}

/* 响应式调整 */
@media screen and (max-width: 1440px) {
  .pc-top-layout {
    height: 420px;
  }
  .pc-carousel .carousel-title {
    font-size: 18px;
  }
  .pc-carousel .carousel-info {
    padding: 20px;
  }
}

@media screen and (max-width: 1200px) {
  .pc-top-layout {
    height: 360px;
  }
  .pc-carousel .carousel-title {
    font-size: 16px;
  }
  .pc-carousel .carousel-info {
    padding: 16px;
  }
  .pc-carousel .carousel-meta {
    gap: 8px;
    font-size: 12px;
  }
}

/* PC端右侧快捷导航和名言区域 */
.pc-quick-nav {
  width: calc(40% - 12px);
  background: linear-gradient(-45deg,
  rgba(124, 92, 219, 0.15) 0%,
  rgba(157, 123, 234, 0.2) 25%,
  rgba(124, 92, 219, 0.15) 50%,
  rgba(157, 123, 234, 0.2) 75%,
  rgba(124, 92, 219, 0.15) 100%);
  background-size: 400% 400%;
  animation: gradientBG 15s ease infinite;
  border-radius: 16px;
  padding: 24px 24px 4px;
  box-shadow:
    0 4px 20px rgba(124, 92, 219, 0.15),
    0 2px 8px rgba(124, 92, 219, 0.1),
    inset 0 2px 4px rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  position: relative;
  height: 480px;  /* 固定高度与轮播图一致 */
  display: flex;
  flex-direction: column;
  overflow: hidden; /* 默认隐藏滚动条 */
}

/* 内容容器 */
.pc-quick-nav .nav-section {
  flex: none;
  margin-bottom: 12px;
}

.pc-quick-nav .quote-section {
  flex: none;
  margin-bottom: 20px;
}

/* 分类和榜单区域容器 */
.pc-quick-nav .category-section,
.pc-quick-nav .ranking-section {
  flex: none;
}

/* 当内容超出时显示滚动条的容器 */
.pc-quick-nav .scrollable-content {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  margin: 0 -24px;
  padding: 0 24px;
  /* 自定义滚动条样式 */
  scrollbar-width: thin;
  scrollbar-color: rgba(124, 92, 219, 0.3) rgba(124, 92, 219, 0.1);
}

.pc-quick-nav .scrollable-content::-webkit-scrollbar {
  width: 4px;
}

.pc-quick-nav .scrollable-content::-webkit-scrollbar-track {
  background: rgba(124, 92, 219, 0.1);
  border-radius: 2px;
}

.pc-quick-nav .scrollable-content::-webkit-scrollbar-thumb {
  background: rgba(124, 92, 219, 0.3);
  border-radius: 2px;
}

.pc-quick-nav .scrollable-content::-webkit-scrollbar-thumb:hover {
  background: rgba(124, 92, 219, 0.5);
}

/* 自定义滚动条样式 - Webkit browsers */
.pc-quick-nav::-webkit-scrollbar {
  width: 6px;
}

.pc-quick-nav::-webkit-scrollbar-track {
  background: rgba(124, 92, 219, 0.1);
  border-radius: 3px;
}

.pc-quick-nav::-webkit-scrollbar-thumb {
  background: rgba(124, 92, 219, 0.3);
  border-radius: 3px;
  transition: background 0.3s ease;
}

.pc-quick-nav::-webkit-scrollbar-thumb:hover {
  background: rgba(124, 92, 219, 0.5);
}

/* 响应式调整 */
@media screen and (max-width: 1440px) {
  .pc-quick-nav {
    height: 420px;
  }
}

@media screen and (max-width: 1200px) {
  .pc-quick-nav {
    height: 360px;
  }
}

/* 移除之前的滚动条样式 */
.pc-quick-nav::-webkit-scrollbar {
  display: none;
}

/* 调整内部布局以适应固定高度 */
.nav-section {
  flex-shrink: 0;
  margin-bottom: 12px;
}

.quote-section {
  flex-shrink: 0;
}

.category-section,
.ranking-section {
  flex-shrink: 0;
}

/* 优化内部间距 */
.section-title {
  margin-bottom: 16px;
}

.nav-tabs {
  margin-bottom: 16px;
}

/* 调整分类列表布局 */
.category-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: -4px;
}

.category-item {
  margin: 4px;
}

/* 调整榜单列表布局 */
.ranking-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: -4px;
}

.ranking-item {
  margin: 4px;
}

@keyframes gradientBG {
  0% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0% 50%;
  }
}

/* 添加装饰性背景元素 */
.pc-quick-nav::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle at center,
  rgba(124, 92, 219, 0.1) 0%,
  rgba(124, 92, 219, 0.05) 30%,
  transparent 70%);
  animation: rotate 20s linear infinite;
  z-index: 0;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

/* 添加滚动条样式 */
.pc-quick-nav::-webkit-scrollbar {
  width: 6px;
}

.pc-quick-nav::-webkit-scrollbar-track {
  background: rgba(124, 92, 219, 0.05);
  border-radius: 3px;
}

.pc-quick-nav::-webkit-scrollbar-thumb {
  background: rgba(124, 92, 219, 0.2);
  border-radius: 3px;
}

.pc-quick-nav::-webkit-scrollbar-thumb:hover {
  background: rgba(124, 92, 219, 0.3);
}

/* 关注区域样式 */
.following-section {
  margin-bottom: 24px;
  padding: 20px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 12px;
  border: 1px solid rgba(124, 92, 219, 0.2);
  position: relative;
  overflow: hidden;
}

.following-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.following-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.following-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  display: flex;
  align-items: center;
  gap: 8px;
}

.following-icon {
  font-size: 20px;
  color: #7c5cdb;
  animation: pulse 2s ease-in-out infinite;
}

.current-time {
  display: flex;
  align-items: center;
  background: linear-gradient(135deg, rgba(124, 92, 219, 0.1), rgba(157, 123, 234, 0.15));
  padding: 8px 16px;
  border-radius: 12px;
  box-shadow:
    0 2px 8px rgba(124, 92, 219, 0.1),
    inset 0 1px 2px rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(8px);
  transition: all 0.3s ease;
}

.time-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
}

.time-icon {
  font-size: 18px;
  color: #7c5cdb;
  animation: clockRotate 8s linear infinite;
}

.time-text {
  font-family: 'Monaco', monospace;
  font-size: 16px;
  font-weight: 500;
  color: #7c5cdb;
  letter-spacing: 0.5px;
  position: relative;
  padding: 2px 4px;
}

.time-text::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 2px;
  background: linear-gradient(90deg, #7c5cdb, #9d7bea);
  transform: scaleX(0);
  transform-origin: left;
  transition: transform 0.3s ease;
}

.current-time:hover {
  transform: translateY(-2px);
  box-shadow:
    0 4px 12px rgba(124, 92, 219, 0.15),
    inset 0 1px 2px rgba(255, 255, 255, 0.5);
}

.current-time:hover .time-text::after {
  transform: scaleX(1);
}

@keyframes clockRotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.time-icon {
  font-size: 16px;
  animation: timeRotate 2s linear infinite;
}

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.1);
  }
}

@keyframes timeRotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

/* 装饰性图标动画 */
.decoration-icons {
  position: absolute;
  width: 100%;
  height: 100%;
  top: 0;
  left: 0;
  pointer-events: none;
  overflow: hidden;
}

.floating-icon {
  position: absolute;
  font-size: 16px;
  color: rgba(124, 92, 219, 0.3);
  animation: float 6s ease-in-out infinite;
}

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

.nav-section {
  margin-bottom: 12px;
  padding: 12px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.03);
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 8px;

  &::before {
    content: '';
    width: 4px;
    height: 18px;
    background: linear-gradient(to bottom, #9d7bea, #7c5cdb);
    border-radius: 2px;
  }
}

.nav-tabs {
  display: flex;
  gap: 2px;
  padding: 4px;
  background: #f8fafc;
  border-radius: 12px;
  position: relative;
  margin-bottom: 24px;
  box-shadow: inset 0 2px 4px rgba(148, 163, 184, 0.05);
}

.nav-item {
  flex: 1;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  color: #64748b;
  cursor: pointer;
  border-radius: 10px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
  user-select: none;
  font-weight: 500;
  letter-spacing: 0.3px;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: linear-gradient(135deg, #9d7bea, #7c5cdb);
    opacity: 0;
    transition: opacity 0.3s ease;
    z-index: 0;
  }

  span {
    position: relative;
    z-index: 1;
    display: flex;
    align-items: center;
    gap: 6px;
  }

  &:hover {
    color: #7c5cdb;
    transform: translateY(-1px);
    background: rgba(124, 92, 219, 0.04);
  }

  &.active {
    color: #fff;
    font-weight: 600;
    transform: translateY(0);

    &::before {
      opacity: 1;
    }

    &::after {
      content: '';
      position: absolute;
      bottom: -8px;
      left: 50%;
      transform: translateX(-50%);
      width: 16px;
      height: 2px;
      background: linear-gradient(90deg, #9d7bea, #7c5cdb);
      border-radius: 4px;
    }
  }

  &:active {
    transform: scale(0.98);
  }
}

.category-section {
  margin-bottom: 24px;
  padding: 20px;
  background: rgba(248, 250, 252, 0.8);
  border-radius: 12px;
  border: 1px solid rgba(241, 245, 249, 0.8);
  position: relative;
  z-index: 1;
  overflow: hidden;  /* 防止内容溢出 */
}

.category-list {
  display: flex;
  flex-wrap: wrap;  /* 允许换行 */
  gap: 10px;
  position: relative;
  z-index: 2;
}

.category-item {
  padding: 8px 20px;
  border-radius: 20px;
  background: #fff;
  color: #64748b;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid #e2e8f0;
  user-select: none;
  font-weight: 500;
  letter-spacing: 0.2px;
  position: relative; /* 添加定位 */
  z-index: 3; /* 确保可以点击 */

  &:hover {
    color: #7c5cdb;
    border-color: #7c5cdb;
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(124, 92, 219, 0.08);
  }

  &.active {
    color: #fff;
    background: linear-gradient(135deg, #9d7bea, #7c5cdb);
    border-color: transparent;
    box-shadow: 0 4px 12px rgba(124, 92, 219, 0.15);
  }

  &:active {
    transform: scale(0.98);
  }
}

.ranking-section {
  margin-bottom: 24px;
  padding: 20px;
  background: rgba(248, 250, 252, 0.8);
  border-radius: 12px;
  border: 1px solid rgba(241, 245, 249, 0.8);
  position: relative;
  z-index: 1;
  overflow: hidden;  /* 防止内容溢出 */
}

.ranking-list {
  display: flex;
  flex-wrap: wrap;  /* 允许换行 */
  gap: 12px;
  position: relative;
  z-index: 2;
}

.ranking-item {
  flex: 1;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 20px;
  background: #fff;
  color: #64748b;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid #e2e8f0;
  user-select: none;
  white-space: nowrap;
  font-weight: 500;
  letter-spacing: 0.2px;
  position: relative; /* 添加定位 */
  z-index: 3; /* 确保可以点击 */

  &:hover {
    color: #7c5cdb;
    border-color: #7c5cdb;
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(124, 92, 219, 0.08);
  }

  &.active {
    color: #fff;
    background: linear-gradient(135deg, #9d7bea, #7c5cdb);
    border-color: transparent;
    box-shadow: 0 4px 12px rgba(124, 92, 219, 0.15);
  }

  &:active {
    transform: scale(0.98);
  }
}

@media screen and (max-width: 1440px) {
  .nav-section {
    padding: 20px;
  }

  .nav-item {
    height: 40px;
    font-size: 14px;
  }

  .category-section,
  .ranking-section {
    padding: 16px;
  }

  .category-item,
  .ranking-item {
    padding: 6px 16px;
    height: 36px;
  }
}

@media screen and (max-width: 1200px) {
  .nav-section {
    padding: 16px;
  }

  .nav-tabs {
    gap: 8px;
  }

  .nav-item {
    height: 36px;
  }

  .category-section,
  .ranking-section {
    padding: 12px;
  }

  .category-item,
  .ranking-item {
    height: 32px;
    padding: 4px 14px;
  }
}

.quote-section {
  padding: 14px;
  border-radius: 12px;
  background: linear-gradient(135deg, #f8f6ff 0%, #fff 100%);
  border: 1px solid #e8e0ff;
  position: relative;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(124, 92, 219, 0.08);
  }

  &::before {
    content: '"';
    position: absolute;
    top: 12px;
    left: 16px;
    font-size: 64px;
    font-family: serif;
    color: #7c5cdb;
    opacity: 0.1;
    line-height: 1;
  }

  .quote-content {
    position: relative;
    z-index: 1;
  }

  .quote-text {
    font-size: 15px;
    color: #4a5568;
    line-height: 1.6;
    margin-bottom: 12px;
    font-style: italic;
    padding-left: 24px;

    &.truncate {
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
  }

  .quote-author {
    font-size: 14px;
    color: #718096;
    text-align: right;
    font-weight: 500;
    overflow: hidden;
  }

  .typing-text {
    display: inline-block;
    width: 0;
    white-space: nowrap;
    overflow: hidden;
    animation: typing 3s steps(40) infinite;
  }

  @keyframes typing {
    0% {
      width: 0;
    }
    50% {
      width: 100%;
    }
    75% {
      width: 100%;
    }
    100% {
      width: 0;
    }
  }

  .barrage-btn {
    padding: 6px 12px;
    border-radius: 16px;
    background: linear-gradient(135deg, #9d7bea, #7c5cdb);
    color: white;
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.3s ease;
    border: none;
    display: flex;
    align-items: center;
    gap: 4px;

    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba(124, 92, 219, 0.2);
    }

    &:active {
      transform: translateY(1px);
    }

    .anticon {
      font-size: 16px;
    }
  }
}

@media screen and (max-width: 1440px) {
  .pc-quick-nav {
    padding: 20px;

    .section-title {
      font-size: 16px;
      margin-bottom: 16px;
    }

    .nav-tabs :deep(.ant-tabs-tab) {
      padding: 6px 20px;

      .ant-tabs-tab-btn {
        font-size: 14px;
      }
    }

    .category-section,
    .ranking-section {
      padding: 16px;
    }

    .quote-section {
      padding: 20px;
    }
  }
}

@media screen and (max-width: 1200px) {
  .pc-quick-nav {
    padding: 16px;

    .nav-tabs :deep(.ant-tabs-tab) {
      padding: 6px 16px;
    }

    .category-section,
    .ranking-section {
      padding: 12px;
    }

    .quote-section {
      padding: 16px;
    }
  }
}

/* PC端分类列表样式优化 */
.category-section {
  margin-bottom: 24px;
  width: 100%;
  overflow: hidden;

  .category-list {
    display: flex;
    flex-wrap: nowrap;
    align-items: center;
    overflow-x: auto;
    padding-bottom: 8px;
    margin: 0 -4px;
    -webkit-overflow-scrolling: touch;
    scrollbar-width: thin;
    white-space: nowrap;
    gap: 8px;
    padding: 4px;

    /* 自定义滚动条样式 */
    &::-webkit-scrollbar {
      height: 6px;
    }

    &::-webkit-scrollbar-track {
      background: #f1f5f9;
      border-radius: 3px;
    }

    &::-webkit-scrollbar-thumb {
      background: #cbd5e1;
      border-radius: 3px;

      &:hover {
        background: #94a3b8;
      }
    }
  }

  .category-item {
    flex: 0 0 auto;
    padding: 8px 16px;
    margin: 0 4px;
    font-size: 14px;
    color: #64748b;
    background: #fff;
    border: 1px solid #e2e8f0;
    border-radius: 20px;
    cursor: pointer;
    transition: all 0.3s ease;
    white-space: nowrap;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-width: fit-content;

    &:hover {
      color: #7c5cdb;
      border-color: #7c5cdb;
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba(124, 92, 219, 0.08);
    }

    &.active {
      color: #fff;
      background: linear-gradient(135deg, #9d7bea, #7c5cdb);
      border-color: transparent;
      box-shadow: 0 4px 12px rgba(124, 92, 219, 0.15);
    }

    &:active {
      transform: scale(0.98);
    }

    .featured-icon {
      margin-right: 4px;
      font-size: 16px;
    }
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
  background: rgba(255, 142, 83, 0.1);
  color: #ff8e53;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 18px;
  transition: all 0.3s ease;

  &:hover {
    background: rgba(255, 142, 83, 0.2);
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
    color: #7c5cdb;
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

/* PC端轮播图交互样式 */
.pc-carousel .carousel-container {
  cursor: grab;
  user-select: none;
}

.pc-carousel .carousel-container:active {
  cursor: grabbing;
}

.pc-carousel .carousel-wrapper {
  will-change: transform;
  transition: transform 0.3s ease-out;
}

.pc-carousel .carousel-item {
  position: relative;
  cursor: pointer;
  transition: transform 0.3s ease;

  &:hover {
    .carousel-image {
      transform: scale(1.02);
    }

    .carousel-info {
      background: linear-gradient(to top, rgba(0, 0, 0, 0.85), transparent);
    }
  }
}

.pc-carousel .carousel-image {
  transition: transform 0.3s ease;
}

.pc-carousel .carousel-info {
  transition: background 0.3s ease;
  pointer-events: none;
}

/* 优化轮播图动画效果 */
.activity-carousel .carousel-wrapper {
  display: flex;
  height: 100%;
  transition: transform 0.2s cubic-bezier(0.33, 1, 0.68, 1);
  will-change: transform;
}

.activity-carousel .carousel-container {
  touch-action: pan-x;
  user-select: none;
  -webkit-user-select: none;
  -webkit-touch-callout: none;
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
  color: #ff8e53;
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

.typing {
  display: inline-block;
  overflow: hidden;
  border-right: 2px solid #ff8e53;
  white-space: nowrap;
  animation: typing 3.5s steps(40, end), blink-caret 0.75s step-end infinite;
  margin: 0 auto;
}

@keyframes typing {
  from { width: 0 }
  to { width: 100% }
}

@keyframes blink-caret {
  from, to { border-color: transparent }
  50% { border-color: #ff8e53 }
}

.quote-text-wrapper {
  position: relative;
  padding: 20px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.8), rgba(255, 255, 255, 0.95));
  border-radius: 12px;
  box-shadow:
    0 4px 15px rgba(255, 142, 83, 0.1),
    0 1px 5px rgba(255, 142, 83, 0.05);
  transition: all 0.3s ease;
}

.quote-text-wrapper:hover {
  transform: translateY(-2px);
  box-shadow:
    0 8px 25px rgba(255, 142, 83, 0.15),
    0 2px 10px rgba(255, 142, 83, 0.1);
}

.quote-text {
  font-size: 16px;
  line-height: 1.8;
  color: #4a5568;
  margin-bottom: 16px;
  font-weight: 500;
  letter-spacing: 0.5px;
}

.quote-author {
  font-size: 14px;
  color: #718096;
  text-align: right;
  font-style: italic;
  margin-top: 12px;
}

/* 优化弹幕按钮样式 */
.barrage-btn {
  padding: 8px 16px;
  border-radius: 20px;
  background: linear-gradient(135deg, #9d7bea, #7c5cdb);
  color: white;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  border: none;
  display: flex;
  align-items: center;
  gap: 6px;
  box-shadow:
    0 4px 12px rgba(124, 92, 219, 0.2),
    0 2px 4px rgba(124, 92, 219, 0.1);
}

.barrage-btn:hover {
  transform: translateY(-2px);
  box-shadow:
    0 6px 16px rgba(124, 92, 219, 0.25),
    0 2px 6px rgba(124, 92, 219, 0.15);
}

.barrage-btn:active {
  transform: translateY(1px);
  box-shadow:
    0 2px 8px rgba(124, 92, 219, 0.2),
    0 1px 3px rgba(124, 92, 219, 0.1);
}

.barrage-btn .anticon {
  font-size: 16px;
  transition: transform 0.3s ease;
}

.barrage-btn:hover .anticon {
  transform: scale(1.1) rotate(-10deg);
}

/* 添加新的样式 */
.nav-icon {
  margin-right: 4px;
  font-size: 16px;
  transition: all 0.3s ease;
}

.nav-item.active .nav-icon {
  color: #fff;
  transform: scale(1.1);
}

.nav-item:hover .nav-icon {
  transform: rotate(-10deg);
}

.nav-item span {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 添加全局滑动过渡动画 */
.van-tabs__content {
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.van-tabs__track {
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
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
  margin-top: 90px;
  opacity: 1;
  transform: translateY(0);
  transition: opacity 0.3s ease, transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.mobile-list-container.fade-enter-active,
.mobile-list-container.fade-leave-active {
  transition: opacity 0.3s ease, transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.mobile-list-container.fade-enter-from,
.mobile-list-container.fade-leave-to {
  opacity: 0;
  transform: translateY(20px);
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
}

/* 添加内容过渡效果 */
.van-pull-refresh {
  transition: margin-top 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 新的加载动画样式 */
.loading-more {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px 0;
}

.loading-spinner {
  width: 36px;
  height: 36px;
  animation: spinner-rotate 2s linear infinite;
}

.path {
  stroke: #ff8e53;
  stroke-linecap: round;
  animation: spinner-dash 1.5s ease-in-out infinite;
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

/* 删除旧的加载动画样式 */
.loading-container,
.loading-svg,
.loading-circle,
.loading-text {
  display: none;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 新的相机加载动画样式 */
.loading-more {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px 0;
}

.loading-camera {
  width: 40px;
  height: 40px;
  animation: camera-bounce 1s ease-in-out infinite;
}

.camera-body {
  fill: none;
  stroke: #ff8e53;
  stroke-width: 4;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-dasharray: 200;
  stroke-dashoffset: 200;
  animation: camera-draw 3s ease-in-out infinite;
}

.camera-lens {
  fill: none;
  stroke: #ff8e53;
  stroke-width: 4;
  stroke-dasharray: 100;
  stroke-dashoffset: 100;
  animation: camera-draw 3s ease-in-out infinite 0.5s;
}

.camera-flash {
  fill: #ff8e53;
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

</style>
