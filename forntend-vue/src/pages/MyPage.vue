<template>
  <div id="MyPage">
    <div class="top-nav">
      <div class="top-nav__left">
        <i class="fas fa-qrcode" @click="handleScanClick" ></i>
      </div>
      <div class="top-nav__right">
        <i class="fas fa-search" @click="handleMyPostsClick"></i>
        <span v-if="loginUserStore.loginUser.userRole === 'admin'" @click="showAdminModal" >
          <i class="fas fa-sliders-h"></i>
        </span>
        <i class="fas fa-cog" @click="handleSettingClick" ></i>
      </div>
    </div>

    <div
      class="bg-banner"
      :class="{ 'loaded': bgLoaded }"
      :style="{ backgroundImage: bgLoaded ? `url(${userHomepageBg})` : 'none' }"
    >
    </div>

    <!-- 移出背景内部，并确保始终可见 -->
    <div class="bg-actions" v-if="loginUserStore.loginUser.id">
      <div class="bg-action-item" @click="handleMessageCenter">
        <i class="fas fa-bell bg-action-icon"></i>
        <span class="bg-action-text">消息</span>
        <div v-if="unreadCount > 0" class="bg-action-badge">{{ unreadCount }}</div>
      </div>
      <div class="bg-action-item" @click="handleMyTeamsClick">
        <i class="fas fa-users bg-action-icon"></i>
        <span class="bg-action-text">团队</span>
      </div>
    </div>

    <div class="profile">
      <div class="profile__avatar">
        <div class="avatar__img" @click="handleSettingClick">
          <img style="border-radius: 50%; border: 3px solid #fff;" :src="loginUserStore.loginUser.userAvatar || getDefaultAvatar(loginUserStore.loginUser.userName)" alt="头像" />
          <div class="avatar__plus">
            <i class="fas fa-cog" style="font-size: 14px;"></i>
          </div>
        </div>
        <div>
          <div class="profile__name">{{ loginUserStore.loginUser.userName || '未登录' }}</div>
          <div class="profile__id">
            悦木号: {{ loginUserStore.loginUser.id || '未登录' }}
            <i
              class="fas fa-copy copy-icon"
              @click="copyUserId"
              :class="{ 'copy-success': copySuccess }"
              title="点击复制悦木号"
            ></i>
          </div>
        </div>
      </div>
      <div class="profile__stats">
        <div class="stats__item" @click="handleFollowClick">
          <span class="stats__value">{{ followAndFans.followCount || 0 }}</span>
          <span class="stats__label">关注</span>
        </div>
        <div class="stats__item" @click="handleFansClick">
          <span class="stats__value">{{ followAndFans.fansCount || 0 }}</span>
          <span class="stats__label">粉丝</span>
        </div>
      </div>
    </div>

    <div class="intro">
      <div class="intro__text" v-if="loginUserStore.loginUser.userProfile">
        {{ loginUserStore.loginUser.userProfile }}
      </div>
      <div class="intro__text" v-if="loginUserStore.loginUser.personalSign">
        {{ loginUserStore.loginUser.personalSign }}
      </div>
      <div class="intro__tag">
        <span class="tag__item" v-if="loginUserStore.loginUser.region">{{ loginUserStore.loginUser.region }}</span>
        <span class="tag__item" v-if="loginUserStore.loginUser.gender || loginUserStore.loginUser.birthday">{{ formatGenderAndAge(loginUserStore.loginUser.gender, loginUserStore.loginUser.birthday) }}</span>
        <span class="tag__item" v-for="tag in parseUserTags(loginUserStore.loginUser.userTags)" :key="tag">{{ tag }}</span>
      </div>
    </div>

    <div class="unlogin-button-container" v-if="!loginUserStore.loginUser.id">
      <a-button class="custom-button login-button" href="/user/login">
        <span class="button-content">
          <i class="fas fa-sign-in-alt button-icon login-icon"></i>
          <span class="button-text">点击登录</span>
        </span>
      </a-button>
    </div>

    <div class="content-tabs" v-if="loginUserStore.loginUser.id">
      <div class="content-tabs__item" :class="{ active: activeTab === 'picture' }" @click="switchTab('picture')">
        <span>图片</span>
        <i v-if="loading && activeTab === 'picture'" class="fas fa-spinner fa-spin tab-loading-icon"></i>
        <!-- 大图/小图切换图标，只在图片Tab时显示 -->
        <button
          v-if="activeTab === 'picture'"
          class="view-mode-toggle-btn"
          :title="pictureViewMode === 'big' ? '切换为网格模式' : '切换为大图模式'"
          @click.stop="togglePictureViewMode"
        >
          <i :class="pictureViewMode === 'big' ? 'fas fa-th' : 'fas fa-image'"></i>
        </button>
      </div>
      <div class="content-tabs__item" :class="{ active: activeTab === 'post' }" @click="switchTab('post')">
        <span>帖子</span>
        <i v-if="loading && activeTab === 'post'" class="fas fa-spinner fa-spin tab-loading-icon"></i>
      </div>
      <div class="content-tabs__item" :class="{ active: activeTab === 'favorites' }" @click="switchTab('favorites')">
        <span>收藏</span>
        <i v-if="loading && activeTab === 'favorites'" class="fas fa-spinner fa-spin tab-loading-icon"></i>
      </div>

      <div class="content-tabs__item mobile-interaction-tab"
           :class="{ active: ['likes', 'comments', 'shares'].includes(activeTab) }"
           @click="toggleInteractionDropdown"
           v-if="device === DEVICE_TYPE_ENUM.MOBILE">
        <span>互动</span>
        <i v-if="tabLoading[activeTab] && isInteractionTab(activeTab)" class="fas fa-spinner fa-spin tab-loading-icon"></i>
        <i class="fas fa-chevron-down dropdown-icon"
           :class="{ 'rotated': showInteractionDropdown }"></i>

        <div class="interaction-dropdown" v-if="showInteractionDropdown">
          <div class="dropdown-item"
               :class="{ active: activeTab === 'likes' }"
               @click.stop="switchTab('likes')">
            <i class="fas fa-heart"></i>
            <span>点赞</span>
            <i v-if="tabLoading['likes']" class="fas fa-spinner fa-spin tab-loading-icon-small"></i>
          </div>
          <div class="dropdown-item"
               :class="{ active: activeTab === 'comments' }"
               @click.stop="switchTab('comments')">
            <i class="fas fa-comment"></i>
            <span>评论</span>
            <i v-if="tabLoading['comments']" class="fas fa-spinner fa-spin tab-loading-icon-small"></i>
          </div>
          <div class="dropdown-item"
               :class="{ active: activeTab === 'shares' }"
               @click.stop="switchTab('shares')">
            <i class="fas fa-share"></i>
            <span>分享</span>
            <i v-if="tabLoading['shares']" class="fas fa-spinner fa-spin tab-loading-icon-small"></i>
          </div>
        </div>
      </div>

      <div class="content-tabs__item"
           :class="{ active: activeTab === 'likes' }"
           @click="switchTab('likes')"
           v-if="device !== DEVICE_TYPE_ENUM.MOBILE">
        <span>点赞</span>
        <i v-if="tabLoading['likes']" class="fas fa-spinner fa-spin tab-loading-icon"></i>
      </div>
      <div class="content-tabs__item"
           :class="{ active: activeTab === 'shares' }"
           @click="switchTab('shares')"
           v-if="device !== DEVICE_TYPE_ENUM.MOBILE">
        <span>分享</span>
        <i v-if="tabLoading['shares']" class="fas fa-spinner fa-spin tab-loading-icon"></i>
      </div>
      <div class="content-tabs__item"
           :class="{ active: activeTab === 'comments' }"
           @click="switchTab('comments')"
           v-if="device !== DEVICE_TYPE_ENUM.MOBILE">
        <span>评论</span>
        <i v-if="tabLoading['comments']" class="fas fa-spinner fa-spin tab-loading-icon"></i>
      </div>
    </div>

    <div class="works" v-if="loginUserStore.loginUser.id">
      <div class="refresh-indicator" :class="{ 'refreshing': isRefreshing, 'pulled': pullDownDistance > 0 }"
           :style="{ transform: `translateY(${pullDownDistance}px)`, opacity: Math.min(pullDownDistance / 60, 1) }">
        <div class="refresh-content">
          <i v-if="isRefreshing" class="fas fa-spinner fa-spin loading-spinner"></i>
          <div v-else class="pull-arrow" :style="{ transform: `rotate(${Math.min(180, (pullDownDistance / 60) * 180)}deg)` }">
            ↓
          </div>
          <span class="refresh-text">{{ isRefreshing ? '刷新中...' : pullDownDistance >= 60 ? '释放刷新' : '下拉刷新' }}</span>
        </div>
      </div>

      <div v-if="loading && contentData.length === 0 && !isInteractionTab(activeTab)" class="loading-state">
        <div class="loader"></div>
      </div>
      <div v-else-if="loadError" class="error-state">
        <div class="custom-empty-state">
          <img :src="emptyImage" alt="加载失败" class="empty-illustration" />
          <p class="empty-desc">加载失败，请稍后重试</p>
        </div>
      </div>
      <!-- 图片 Tab 的快捷操作按钮，无论有无数据都始终显示 -->
      <div v-if="activeTab === 'picture' && !loading" class="picture-actions">
        <div class="two-button-container">
          <a-button
            type="text"
            @click="handleMySpaceClick"
            class="draft-btn-small space-btn"
          >
            <i class="fas fa-user button-icon"></i>
            私有图库
          </a-button>
          <a-button
            type="text"
            @click="showDraftPictures"
            class="draft-btn-small draft-box-btn"
          >
            <i class="fas fa-file-image button-icon"></i>
            图片草稿
          </a-button>
        </div>
      </div>

      <!-- 帖子 Tab 的快捷操作按钮，无论有无数据都始终显示 -->
      <div v-if="activeTab === 'post' && !loading" class="post-actions">
        <div class="two-button-container">
          <a-button
            type="text"
            @click="showPostDraftModal"
            class="draft-btn-small draft-box-btn"
          >
            <i class="fas fa-sticky-note button-icon"></i>
            帖子草稿
          </a-button>
        </div>
      </div>

      <!-- 空状态提示：无数据、无加载中时显示 -->
      <div v-if="!loading && !loadError && contentData.length === 0 && !tabLoading[activeTab]" class="empty-state">
        <div class="custom-empty-state">
          <img :src="emptyImage" :alt="getEmptyDescription()" class="empty-illustration" />
          <p class="empty-desc">{{ getEmptyDescription() }}</p>
        </div>
      </div>

      <template v-if="!loadError && contentData.length > 0">
        <div v-if="activeTab === 'picture'">
          <!-- 大图模式 -->
          <BigPictureList
            v-if="pictureViewMode === 'big'"
            :dataList="contentData"
            :loading="loading"
            :showOp="true"
            :onReload="fetchContentData"
            :isMyPosts="true"
            style="margin-top: 6px"
          />
          <!-- 小图网格模式（原组件） -->
          <MobilePictureList
            v-else
            :dataList="contentData"
            :loading="loading"
            :showOp="true"
            :onReload="fetchContentData"
            :isMyPosts="true"
          />
        </div>
        <div v-else-if="activeTab === 'post'">
          <PostList
            :dataList="contentData"
            :loading="loading"
            :showOp="true"
            :onReload="fetchContentData"
            :isMyPosts="true"
            :showStatus="true"
          />
        </div>
        <div v-else-if="activeTab === 'favorites'">
          <UnreadFavoriteList
            :favorites="contentData"
            :is-received="false"
          />
        </div>
        <UnreadLikeList
          v-else-if="activeTab === 'likes'"
          :likes="contentData"
          :is-received="false"
        />
        <UnreadShareList
          v-else-if="activeTab === 'shares'"
          :shares="contentData"
          :is-received="false"
        />
        <UnreadCommentList
          v-else-if="activeTab === 'comments'"
          :comments="contentData"
          :is-received="false"
        />
      </template>


      <div v-if="isLoadingMore" class="load-more-loading">
        <i class="fas fa-spinner fa-spin loading-icon-small"></i>
        <span>加载中...</span>
      </div>
      <div v-else-if="!hasMore() && contentData.length > 0" class="no-more-data">
        <span>没有更多数据了</span>
      </div>
    </div>

    <ShareModal
      ref="shareModalRef"
      :title="`${loginUserStore.loginUser.userName || '我'}在悦木图库，扫码关注一下吧`"
      :link="userHomeLink"
      :image-url="getImageUrlForShare()"
      :user="loginUserStore.loginUser"
      :create-time="loginUserStore.loginUser.createTime"
    />

    <div v-if="draftModalVisible" class="custom-draft-modal">
      <div class="modal-overlay" @click="closeDraftModal"></div>
      <div class="modal-container">
        <div class="modal-header">
          <h3>草稿图片管理</h3>
          <div class="close-btn" @click="closeDraftModal">×</div>
        </div>
        <div class="modal-body">
          <MobilePictureList
            :dataList="draftPictures"
            :loading="draftLoading"
            :showOp="true"
            :onReload="fetchDraftPictures"
            :isMyPosts="true"
          />
        </div>
      </div>
    </div>

    <div v-if="postDraftModalVisible" class="custom-draft-modal">
      <div class="modal-overlay" @click="closePostDraftModal"></div>
      <div class="modal-container">
        <div class="modal-header">
          <h3>帖子草稿箱</h3>
          <div class="close-btn" @click="closePostDraftModal">×</div>
        </div>
        <div class="modal-body">
          <PostDraftList />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { DEVICE_TYPE_ENUM } from '@/constants/device'
import { getDeviceType } from '@/utils/device'
import {
  UserOutlined,
  LoginOutlined,
  RightOutlined,
  SettingOutlined,
  LogoutOutlined,
  LinkOutlined,
  AppstoreOutlined,
  PictureOutlined,
  TagsOutlined,
  FolderOutlined,
  ControlOutlined,
  TeamOutlined,
  CopyOutlined,
  FileTextOutlined,
  BellOutlined,
  MailOutlined,
  CalendarOutlined,
  AndroidOutlined,
  PlayCircleOutlined,
  CommentOutlined,
  SearchOutlined,
  QrcodeOutlined,
  ReloadOutlined,
  FileImageOutlined,
} from '@ant-design/icons-vue'
import { useRouter, onBeforeRouteLeave } from 'vue-router'
import { userLogoutUsingPost } from '@/api/userController'
import { message } from 'ant-design-vue'
import { h, ref, onMounted, onUnmounted, computed, onActivated, onDeactivated, watch } from 'vue'
import { Modal } from 'ant-design-vue'
import { getFollowAndFansCountUsingPost } from '@/api/userFollowsController'
import '@lottiefiles/lottie-player'
import { useParallax } from '@vueuse/core'
import { Badge } from 'ant-design-vue'
import SeasonalBanner from '@/banner/SeasonalBanner.vue'
import WeatherWidget from '@/components/WeatherWidget.vue'
import ShareModal from '@/components/ShareModal.vue'
import { useMessageStore } from '@/stores/useMessageStore'
import PictureList from '@/components/PictureList.vue'
import PostList from '@/components/PostList.vue'
import UnreadLikeList from '@/components/UnreadLikeList.vue'
import UnreadShareList from '@/components/UnreadShareList.vue'
import UnreadCommentList from '@/components/UnreadCommentList.vue'
import UnreadFavoriteList from '@/components/UnreadFavoriteList.vue'
import { listPictureVoByPageUsingPost } from '@/api/pictureController'
import { listMyPostsUsingPost } from '@/api/postController'
import { getMyLikeHistoryUsingPost } from '@/api/likeRecordController'
import { getMyFavoriteHistoryUsingPost } from '@/api/favoriteRecordController'
import { getMyShareHistoryUsingPost } from '@/api/shareRecordController'
import { getMyCommentHistoryUsingPost } from '@/api/commentsController'
import { listDraftPicturesUsingGet } from '@/api/pictureController'
import BigPictureList from '@/components/BigPictureList.vue'
import PostDraftList from '@/components/PostDraftList.vue'
import emptyImage from '@/assets/illustrations/empty.png'

const loginUserStore = useLoginUserStore()
const router = useRouter()
defineOptions({ name: 'MyPage' })
const adminModalOpen = ref(false)
const device = ref<string>('')
const topBarSolid = ref(false)
const shareModalRef = ref<any>(null)
const draftModalVisible = ref(false)
const postDraftModalVisible = ref(false)


// 控制弹窗打开时禁止背景滚动
const disableBodyScroll = (shouldDisable: boolean) => {
  if (shouldDisable) {
    document.body.style.overflow = 'hidden'
    document.documentElement.style.overflow = 'hidden'
  } else {
    document.body.style.overflow = ''
    document.documentElement.style.overflow = ''
  }
}

const draftPictures = ref<any[]>([])
const draftLoading = ref(false)
const copySuccess = ref(false)

// 监听路由变化，离开页面时关闭所有弹框
onBeforeRouteLeave((to, from) => {
  // 关闭草稿箱弹框
  closeDraftModal()
  closePostDraftModal()

  // 关闭移动端互动下拉菜单
  showInteractionDropdown.value = false
})

const userHomeLink = computed(() => {
  const user = loginUserStore.loginUser || {}
  const href = router.resolve({
    path: `/user/${user.id}`,
    query: {
      userName: user.userName,
      userAvatar: user.userAvatar,
      userAccount: user.userAccount,
      userProfile: user.userProfile,
      userRole: user.userRole,
      createTime: user.createTime
    }
  }).href
  return `${window.location.origin}${href}`
})

const getImageUrlForShare = () => {
  // 优先使用背景图
  if (loginUserStore.loginUser?.homepageBg) {
    return loginUserStore.loginUser.homepageBg;
  }
  // 其次使用用户头像
  if (loginUserStore.loginUser?.userAvatar) {
    return loginUserStore.loginUser.userAvatar;
  }
  // 最后使用默认头像
  return getDefaultAvatar(loginUserStore.loginUser.userName);
}

const followAndFans = ref({
  followCount: 0,
  fansCount: 0
})

const messageStore = useMessageStore()
const props = defineProps({
  messageCenterUnreadCount: {
    type: Number,
    default: 0
  }
})
const unreadCount = computed(() => props.messageCenterUnreadCount)

const activeTab = ref('picture')

// 图片展示模式：'list' 小图网格 | 'big' 大图沉浸
const PICTURE_VIEW_MODE_KEY = 'myPagePictureViewMode'
const pictureViewMode = ref<'list' | 'big'>(
  (localStorage.getItem(PICTURE_VIEW_MODE_KEY) as 'list' | 'big') || 'big' // 默认值改为 big
)
const togglePictureViewMode = () => {
  pictureViewMode.value = pictureViewMode.value === 'list' ? 'big' : 'list'
  localStorage.setItem(PICTURE_VIEW_MODE_KEY, pictureViewMode.value)
}
const contentData = ref<any[]>([])
const loading = ref(false)
const pagination = ref({
  current: 1,
  pageSize: 12,
  total: 0
})
const loadError = ref(false)
const showInteractionDropdown = ref(false)

const tabLoading = ref<Record<string, boolean>>({
  likes: false,
  shares: false,
  comments: false
})

const isInteractionTab = (tab: string) => {
  return ['likes', 'comments', 'shares'].includes(tab)
}

const toggleInteractionDropdown = () => {
  if (device.value === DEVICE_TYPE_ENUM.MOBILE) {
    showInteractionDropdown.value = !showInteractionDropdown.value
  }
}

const handleClickOutside = (event: Event) => {
  const target = event.target as HTMLElement
  const dropdown = target.closest('.mobile-interaction-tab')
  if (!dropdown) {
    showInteractionDropdown.value = false
  }
}

const TABS = ['picture', 'post', 'likes', 'favorites', 'shares', 'comments'] as const
type Tab = typeof TABS[number]

const createDefaultState = () => ({
  pagination: { current: 1, pageSize: 12, total: 0 },
  content: [] as any[],
  loading: false,
  isLoadingMore: false,
  loadError: false
})

const tabStore = ref<Record<string, {
  pagination: { current: number; pageSize: number; total: number },
  content: any[],
  loading: boolean,
  isLoadingMore: boolean,
  loadError: boolean
}>>({
  picture: createDefaultState(),
  post: createDefaultState(),
  likes: createDefaultState(),
  favorites: createDefaultState(),
  shares: createDefaultState(),
  comments: createDefaultState()
})

import springBanner from '@/assets/season/mobile/1.png'
import summerBanner from '@/assets/season/mobile/2.png'
import autumnBanner from '@/assets/season/mobile/3.png'
import winterBanner from '@/assets/season/mobile/4.png'
const bannerImages = {
  spring: springBanner,
  summer: summerBanner,
  autumn: autumnBanner,
  winter: winterBanner
}
const getCurrentSeason = () => {
  const now = new Date()
  const month = now.getMonth() + 1
  if (month >= 3 && month <= 5) return 'spring'
  if (month >= 6 && month <= 8) return 'summer'
  if (month >= 9 && month <= 11) return 'autumn'
  return 'winter'
}
const currentSeason = ref(getCurrentSeason())
const currentSeasonBanner = ref(bannerImages[currentSeason.value])

const userHomepageBg = computed(() => {
  if (loginUserStore.loginUser?.homepageBg) {
    return loginUserStore.loginUser.homepageBg;
  }
  return currentSeasonBanner.value;
});

const bgLoaded = ref(false)

// 监控背景图加载
const preloadBg = (url: string) => {
  if (!url) return
  bgLoaded.value = false
  const img = new Image()
  img.src = url
  img.onload = () => {
    bgLoaded.value = true
  }
}

watch(() => userHomepageBg.value, (newVal) => {
  preloadBg(newVal)
}, { immediate: true })

const formatBirthday = (birthday: string | undefined) => {
  if (!birthday) return '';
  try {
    const date = new Date(birthday);
    return date.toLocaleDateString('zh-CN', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  } catch (error) {
    console.error('格式化生日失败:', error);
    return birthday;
  }
};

const formatGenderAndAge = (gender: string | undefined, birthday: string | undefined) => {
  if (!birthday) {
    return gender || '';
  }
  try {
    const birthDate = new Date(birthday);
    const today = new Date();
    let age = today.getFullYear() - birthDate.getFullYear();
    const monthDiff = today.getMonth() - birthDate.getMonth();
    if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birthDate.getDate())) {
      age--;
    }
    return `${gender || ''} ${age}岁`;
  } catch (error) {
    console.error('计算年龄失败:', error);
    return gender || '';
  }
};

const parseUserTags = (tagsStr: string | undefined) => {
  if (!tagsStr) return [];
  return tagsStr.split(/[;,，；]/).filter(tag => tag.trim() !== '');
};

const isLoadingMore = ref(false)
const hasMore = () => {
  return pagination.value.current * pagination.value.pageSize < pagination.value.total
}

const barragePosition = ref({
  x: window.innerWidth - 80,
  y: window.innerHeight - 64,
})
const touchStartPos = ref({ x: 0, y: 0 })
const isDragging = ref(false)
const pullDownDistance = ref(0)
const isRefreshing = ref(false)
const scrollTimer = ref<number | null>(null)

const applyTabState = (tab: Tab) => {
  const s = tabStore.value[tab]
  pagination.value = { ...s.pagination }
  contentData.value = [...s.content]
  loading.value = s.loading
  isLoadingMore.value = s.isLoadingMore
  loadError.value = s.loadError
}

const handleRefresh = async () => {
  if (isRefreshing.value) return
  isRefreshing.value = true
  try {
    tabStore.value[activeTab.value].content = []
    tabStore.value[activeTab.value].pagination = { current: 1, pageSize: 12, total: 0 }
    tabStore.value[activeTab.value].loading = true
    tabStore.value[activeTab.value].loadError = false
    applyTabState(activeTab.value)
    await fetchContentData()
  } catch (error) {
    message.error('刷新失败')
  } finally {
    isRefreshing.value = false
  }
}

const getFollowAndFansCount = async () => {
  if (!loginUserStore.loginUser.id) return
  try {
    const res = await getFollowAndFansCountUsingPost({
      id: loginUserStore.loginUser.id
    })
    if (res.data.code === 0) {
      followAndFans.value = res.data.data || { followCount: 0, fansCount: 0 }
    }
  } catch (error) {
    console.error('获取关注粉丝数失败:', error)
  }
}

const handleScroll = () => {
  topBarSolid.value = window.scrollY > 20
  if (window.innerWidth <= 768) {
    const pet = document.querySelector('.page-pet')
    if (pet) {
      const scrolled = window.scrollY + window.innerHeight
      const threshold = document.documentElement.scrollHeight - 60
      if (scrolled > threshold) {
        pet.classList.add('hide')
      } else {
        pet.classList.remove('hide')
      }
    }
  }
  if (scrollTimer.value !== null) return
  scrollTimer.value = window.setTimeout(() => {
    const scrollHeight = document.documentElement.scrollHeight
    const scrollTop = document.documentElement.scrollTop || document.body.scrollTop
    const clientHeight = document.documentElement.clientHeight
    if (scrollHeight - scrollTop - clientHeight < 30 && !loading.value && !isLoadingMore.value && hasMore()) {
      loadMore()
    }
    if (scrollTimer.value !== null) {
      clearTimeout(scrollTimer.value)
      scrollTimer.value = null
    }
  }, 150)
}

onMounted(async () => {
  window.addEventListener('click', handleClickOutside)
  device.value = await getDeviceType()
  getFollowAndFansCount()
  currentSeason.value = getCurrentSeason()
  currentSeasonBanner.value = bannerImages[currentSeason.value]
  if (loginUserStore.loginUser.id) {
    fetchContentData()
  }
  window.addEventListener('scroll', handleScroll)
  window.addEventListener('resize', () => {
    barragePosition.value = {
      x: Math.min(barragePosition.value.x, window.innerWidth - 60),
      y: Math.min(barragePosition.value.y, window.innerHeight - 80),
    }
    const newDeviceType = getDeviceType()
    if (device.value !== newDeviceType) {
      device.value = newDeviceType
      if (device.value === DEVICE_TYPE_ENUM.MOBILE) {
        window.addEventListener('touchstart', handleTouchStart, { passive: false })
        window.addEventListener('touchmove', handleTouchMove, { passive: false })
        window.addEventListener('touchend', handleTouchEnd)
      } else {
        window.removeEventListener('touchstart', handleTouchStart)
        window.removeEventListener('touchmove', handleTouchMove)
        window.removeEventListener('touchend', handleTouchEnd)
      }
    }
  })
  if (device.value === DEVICE_TYPE_ENUM.MOBILE) {
    window.addEventListener('touchstart', handleTouchStart, { passive: false })
    window.addEventListener('touchmove', handleTouchMove, { passive: false })
    window.addEventListener('touchend', handleTouchEnd)
  }
})

const handleMySpaceClick = () => {
  router.push('/my_space')
}

const handleSettingClick = () => {
  router.push('/user/setting')
}

const handleLogoutClick = async () => {
  Modal.confirm({
    title: null,
    icon: null,
    width: 320,
    footer: null,
    wrapClassName: 'logout-modal',
    centered: true,
    maskClosable: true,
    bodyStyle: {
      padding: 0,
      margin: '0 auto'
    },
    content: h('div', {
      class: 'logout-modal-content'
    }, [
      h('div', {
        class: 'warning-icon'
      }, [
        h(LogoutOutlined)
      ]),
      h('h3', {
        class: 'modal-title'
      }, '确认退出登录？'),
      h('p', {
        class: 'modal-desc'
      }, '退出后需要重新登录才能继续使用'),
      h('div', {
        class: 'modal-actions'
      }, [
        h('button', {
          class: 'cancel-button',
          onClick: () => {
            Modal.destroyAll();
          }
        }, '取消'),
        h('button', {
          class: 'confirm-button',
          onClick: async () => {
            try {
              const res = await userLogoutUsingPost()
              if (res.data.code === 0) {
                Modal.destroyAll();
                loginUserStore.setLoginUser({
                  userName: '未登录',
                })
                message.success('退出登录成功')
                await router.push('/user/login')
              } else {
                message.error('退出登录失败，' + res.data.message)
              }
            } catch (error) {
              message.error('退出登录失败，请稍后重试')
            }
          }
        }, '确认退出')
      ])
    ])
  })
}

const handleScanClick = () => {
  if (!loginUserStore.loginUser?.id) {
    message.warning('请先登录')
    return
  }
  shareModalRef.value?.openModal()
}

const handleClick = () => {
  window.location.href = 'http://my.lumenglover.com/contact'
}

const showAdminModal = () => {
  router.push('/admin/manage')
}

const handleAdminClick = (route: string) => {
  adminModalOpen.value = false
  router.push(`/admin/${route}`)
}

const handleMyPostsClick = () => {
  router.push('/my_ports')
}

const handleSearchClick = () => {
  router.push('/search')
}

const handleMyTeamsClick = () => {
  router.push('/my_teams')
}

const handleFollowClick = () => {
  router.push({ path: '/follow-list', query: { tab: 'follow' } })
}

const handleFansClick = () => {
  router.push({ path: '/follow-list', query: { tab: 'fans' } })
}

const handleAvatarClick = () => {
  if (device.value !== DEVICE_TYPE_ENUM.PC && loginUserStore.loginUser.id) {
    router.push('/user/setting')
  }
}

const getDefaultAvatar = (userName: string) => {
  const defaultName = userName || 'Guest'
  return `https://api.dicebear.com/7.x/adventurer/svg?seed=${encodeURIComponent(defaultName)}&backgroundColor=ffd5dc,ffdfbf,ffd5dc`
}

const copyUserId = () => {
  if (loginUserStore.loginUser.id) {
    navigator.clipboard.writeText(loginUserStore.loginUser.id.toString())
      .then(() => {
        message.success('已复制用户ID')
        copySuccess.value = true
        setTimeout(() => {
          copySuccess.value = false
        }, 2000)
      })
      .catch(() => {
        message.error('复制失败，请手动复制')
      })
  }
}

onUnmounted(() => {
  window.removeEventListener('click', handleClickOutside)
  window.removeEventListener('scroll', handleScroll)
  window.removeEventListener('touchstart', handleTouchStart)
  window.removeEventListener('touchmove', handleTouchMove)
  window.removeEventListener('touchend', handleTouchEnd)
  if (scrollTimer.value !== null) {
    clearTimeout(scrollTimer.value)
    scrollTimer.value = null
  }
})

onActivated(() => {
  window.addEventListener('scroll', handleScroll)
  if (device.value === DEVICE_TYPE_ENUM.MOBILE) {
    window.addEventListener('touchstart', handleTouchStart, { passive: false })
    window.addEventListener('touchmove', handleTouchMove, { passive: false })
    window.addEventListener('touchend', handleTouchEnd)
  }
})

onDeactivated(() => {
  window.removeEventListener('scroll', handleScroll)
  window.removeEventListener('touchstart', handleTouchStart)
  window.removeEventListener('touchmove', handleTouchMove)
  window.removeEventListener('touchend', handleTouchEnd)
  if (scrollTimer.value !== null) {
    clearTimeout(scrollTimer.value)
    scrollTimer.value = null
  }
})

const target = ref(null)
const { tilt } = useParallax(target)

const handleMessageCenter = () => {
  router.push('/message-center')
}

const handleEmailSetup = () => {
  router.push('/user/setting')
}

const handleToolsClick = () => {
  router.push('/tools')
}

const downloadApp = () => {
  window.open('/api/app/download', '_blank')
}

const handleGamesClick = () => {
  router.push('/games')
}

const getTabName = (tab: string) => {
  const tabNames = {
    picture: '图片',
    post: '帖子',
    likes: '点赞',
    favorites: '收藏',
    shares: '分享',
    comments: '评论'
  }
  return tabNames[tab as keyof typeof tabNames] || tab
}

const switchTab = async (tab: string) => {
  if (activeTab.value === tab) {
    showInteractionDropdown.value = false
    return
  }
  if (isInteractionTab(tab)) {
    tabLoading.value[tab] = true
  } else {
    loading.value = true
  }
  activeTab.value = tab as Tab
  showInteractionDropdown.value = false
  if (scrollTimer.value !== null) {
    clearTimeout(scrollTimer.value)
    scrollTimer.value = null
  }
  isLoadingMore.value = false
  loadError.value = false
  const s = tabStore.value[activeTab.value]
  if (s.content.length > 0 || s.pagination.total > 0) {
    applyTabState(activeTab.value)
    if (isInteractionTab(tab)) {
      tabLoading.value[tab] = false
    } else {
      loading.value = false
    }
  } else {
    pagination.value = { current: 1, pageSize: pagination.value.pageSize, total: 0 }
    contentData.value = []
    tabStore.value[activeTab.value].pagination = { ...pagination.value }
    tabStore.value[activeTab.value].content = []
    tabStore.value[activeTab.value].loading = true
    tabStore.value[activeTab.value].isLoadingMore = false
    tabStore.value[activeTab.value].loadError = false
    try {
      await fetchContentData()
    } catch (error) {
      message.error(`加载${getTabName(tab)}失败`)
    } finally {
      if (isInteractionTab(tab)) {
        tabLoading.value[tab] = false
      } else {
        loading.value = false
      }
    }
  }
}

const handleTouchStart = (e: TouchEvent) => {
  if (isRefreshing.value) return
  touchStartPos.value = { x: e.touches[0].clientX, y: e.touches[0].clientY }
}

const handleTouchMove = (e: TouchEvent) => {
  if (isRefreshing.value) return
  const touch = e.touches[0]
  const deltaY = touch.clientY - touchStartPos.value.y
  if (window.scrollY <= 0 && deltaY > 0) {
    e.preventDefault()
    pullDownDistance.value = Math.min(deltaY * 0.4, 100)
  }
}

const handleTouchEnd = async () => {
  if (isRefreshing.value) return
  if (pullDownDistance.value >= 60) {
    isRefreshing.value = true
    pullDownDistance.value = 0
    try {
      tabStore.value[activeTab.value].content = []
      tabStore.value[activeTab.value].pagination = { current: 1, pageSize: 12, total: 0 }
      tabStore.value[activeTab.value].loading = true
      tabStore.value[activeTab.value].loadError = false
      applyTabState(activeTab.value)
      await fetchContentData()
    } catch (error) {
      message.error('刷新失败')
    } finally {
      isRefreshing.value = false
    }
  } else {
    pullDownDistance.value = 0
  }
}

const loadMore = async () => {
  if (isLoadingMore.value || !hasMore()) return
  isLoadingMore.value = true
  tabStore.value[activeTab.value].isLoadingMore = true
  try {
    pagination.value.current += 1
    await fetchContentData(true)
  } catch (error: any) {
    pagination.value.current -= 1
    message.error('加载更多失败：' + error.message)
  } finally {
    isLoadingMore.value = false
    tabStore.value[activeTab.value].isLoadingMore = false
  }
}

const onPageChange = (page: number, pageSize: number) => {
  pagination.value.current = page
  pagination.value.pageSize = pageSize
  contentData.value = []
  tabStore.value[activeTab.value].pagination = { ...pagination.value }
  tabStore.value[activeTab.value].content = []
  tabStore.value[activeTab.value].loading = true
  tabStore.value[activeTab.value].loadError = false
  fetchContentData()
}

const fetchContentData = async (isLoadMore = false) => {
  if (!loginUserStore.loginUser.id) return
  if (!isLoadMore) {
    if (isInteractionTab(activeTab.value)) {
      tabLoading.value[activeTab.value] = true
    } else {
      loading.value = true
    }
  }
  try {
    let res
    const params = {
      current: pagination.value.current,
      pageSize: pagination.value.pageSize,
      sortField: 'createTime',
      sortOrder: 'descend',
      userId: loginUserStore.loginUser.id
    }
    switch (activeTab.value) {
      case 'picture':
        res = await listPictureVoByPageUsingPost(params)
        break
      case 'post':
        res = await listMyPostsUsingPost(params)
        break
      case 'likes':
        res = await getMyLikeHistoryUsingPost(params)
        break
      case 'favorites':
        res = await getMyFavoriteHistoryUsingPost(params)
        break
      case 'shares':
        res = await getMyShareHistoryUsingPost(params)
        break
      case 'comments':
        res = await getMyCommentHistoryUsingPost(params)
        break
    }
    if (res?.data?.code === 0 && res.data.data) {
      const newData = Array.isArray(res.data.data.records) ? res.data.data.records : []
      if (isLoadMore) {
        contentData.value = [...contentData.value, ...newData]
      } else {
        contentData.value = newData
      }
      pagination.value.total = Number(res.data.data.total || 0)
      loadError.value = false
      tabStore.value[activeTab.value].content = [...contentData.value]
      tabStore.value[activeTab.value].pagination = { ...pagination.value }
      tabStore.value[activeTab.value].loading = false
      tabStore.value[activeTab.value].loadError = false
    }
  } catch (error: any) {
    message.error('获取数据失败：' + error.message)
    if (!isLoadMore) {
      contentData.value = []
      pagination.value.total = 0
    }
    loadError.value = true
    tabStore.value[activeTab.value].content = [...contentData.value]
    tabStore.value[activeTab.value].pagination = { ...pagination.value }
    tabStore.value[activeTab.value].loading = false
    tabStore.value[activeTab.value].loadError = true
  } finally {
    if (!isLoadMore) {
      if (isInteractionTab(activeTab.value)) {
        tabLoading.value[activeTab.value] = false
      } else {
        loading.value = false
      }
    }
  }
}

const getEmptyDescription = () => {
  const descriptions = {
    picture: '暂无图片内容',
    post: '暂无帖子内容',
    likes: '暂无点赞记录',
    favorites: '暂无收藏记录',
    shares: '暂无分享记录',
    comments: '暂无评论记录'
  }
  return descriptions[activeTab.value as keyof typeof descriptions]
}

const goToTreeHole = () => {
  router.push('/barrage')
}

const goToFriendLink = () => {
  router.push('/friend-links')
}

const fetchDraftPictures = async () => {
  if (!loginUserStore.loginUser.id) return
  draftLoading.value = true
  try {
    const res = await listDraftPicturesUsingGet()
    if (res?.data?.code === 0) {
      draftPictures.value = Array.isArray(res.data.data) ? res.data.data : []
    }
  } catch (error: any) {
    message.error('获取草稿图片失败：' + error.message)
  } finally {
    draftLoading.value = false
  }
}

const showDraftPictures = async () => {
  await fetchDraftPictures()
  draftModalVisible.value = true
  // 禁用背景滚动
  disableBodyScroll(true)
}

const closeDraftModal = () => {
  draftModalVisible.value = false
  // 恢复滚动
  disableBodyScroll(false)
}

const showPostDraftModal = () => {
  postDraftModalVisible.value = true
  // 禁用背景滚动
  disableBodyScroll(true)
}

const closePostDraftModal = () => {
  postDraftModalVisible.value = false
  // 恢复滚动
  disableBodyScroll(false)
}

defineExpose({
  handleMySpaceClick,
  handleSettingClick,
  handleClick,
  handleLogoutClick,
  handleMyPostsClick,
  showAdminModal,
  handleAdminClick,
  handleMyTeamsClick,
  handleToolsClick,
  handleGamesClick,
  goToTreeHole,
  goToFriendLink,
  showDraftPictures,
  fetchDraftPictures,
})
</script>
<style scoped>

@import url("https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.7.0/css/font-awesome.min.css");
#MyPage {
  position: relative;
  padding-bottom: 50px;
  max-width: 1400px;
  margin: 0 auto;
  width: 100%;
  box-sizing: border-box;
}
.top-nav {
  position: relative;
  height: 44px;
  padding: 0 4px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #fff;
  z-index: 1;
  width: 100%;
  box-sizing: border-box;
}

.top-nav__left i,
.top-nav__right i {
  background: rgba(0,0,0,0.4);
  border-radius: 50%;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 20px rgba(0,0,0,0.15);
  transition: all 0.3s ease;
  margin: 0 6px;
}

.top-nav__left i:hover,
.top-nav__right i:hover {
  background: rgba(0,0,0,0.5);
  transform: scale(1.1);
}

.top-nav__center {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.top-nav__title {
  font-size: 16px;
  font-weight: 600;
  color: #fff;
}
.top-nav__left {
  display: flex;
  align-items: center;
  font-size: 18px;
}

.top-nav__right {
  display: flex;
  align-items: center;
}

.bg-banner {
  margin-top: -44px!important;
  height: 250px;
  background-size: cover;
  background-position: center;
  position: relative;
  width: 100%;
  box-sizing: border-box;
  background-color: var(--hover-background);
  opacity: 0;
  filter: blur(10px);
  transition: opacity 0.8s ease-out, filter 1s ease-out;
}

.bg-banner.loaded {
  opacity: 1;
  filter: blur(0);
}

@media (min-width: 768px) {
  .bg-banner {
    height: 420px;
  }
}

.bg-actions {
  position: absolute;
  top: 200px; /* 对应移动端高度减去按钮预留 */
  right: 6px;
  display: flex;
  gap: 12px;
  z-index: 10;
}

@media (min-width: 768px) {
  .bg-actions {
    top: 370px; /* 对应 PC 端高度减去按钮预留 */
  }
}

.bg-action-item {
  display: flex;
  align-items: center;
  gap: 6px;
  background: rgba(0,0,0,0.5);
  backdrop-filter: blur(8px);
  padding: 6px 12px;
  border-radius: 20px;
  color: #fff;
  cursor: pointer;
  font-size: 14px;
  position: relative;
}

.bg-action-icon {
  font-size: 16px;
}

.bg-action-text {
  font-size: 12px;
}

.bg-action-badge {
  position: absolute;
  top: -4px;
  right: -4px;
  background-color: #ff4d4f;
  color: #fff;
  border-radius: 10px;
  padding: 1px 6px;
  font-size: 10px;
  min-width: 16px;
  height: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.profile {
  position: relative;
  padding: 0 8px;
  margin-top: -50px;
  width: 100%;
  box-sizing: border-box;
}

@media (min-width: 768px) {
  .profile {
    margin-top: -64px;
  }
}
.profile__avatar {
  display: flex;
  align-items: flex-end;
  gap: 12px;
}
.avatar__img {
  width: 100px;
  height: 100px;
  overflow: hidden;
  position: relative;
  z-index: 2;
}

@media (min-width: 768px) {
  .avatar__img {
    width: 124px;
    height: 124px;
  }
}
.avatar__img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.avatar__plus {
  position: absolute;
  right: 3px;
  bottom: 3px;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background-color: #27C346;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  border: 2px solid #fff;
}

@media (min-width: 768px) {
  .avatar__plus {
    width: 36px;
    height: 36px;
    right: -4px;
    bottom: -4px;
    font-size: 20px;
  }
}
.profile__name {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 4px;
}
.profile__id {
  font-size: 12px;
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  gap: 4px;
}

.copy-icon {
  cursor: pointer;
  font-size: 12px;
  color: var(--text-secondary);
  transition: all 0.3s ease;
  padding: 2px;
  border-radius: 4px;

  &:hover {
    color: #2563eb;
    background-color: rgba(37, 99, 235, 0.1);
  }
}

.copy-success {
  color: #10b981 !important;
  animation: copySuccess 0.6s ease;
}

@keyframes copySuccess {
  0% { transform: scale(1); }
  50% { transform: scale(1.2); }
  100% { transform: scale(1); }
}

.avatar__plus {
  position: absolute;
  right: 3px;
  bottom: 3px;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background-color: #27C346;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  border: 2px solid #fff;
  transition: all 0.3s ease;
  cursor: pointer;

  &:hover {
    background-color: #1da53a;
    transform: scale(1.1);
  }
}
.profile__edit {
  position: absolute;
  top: 12px;
  right: 16px;
  width: 80px;
  height: 32px;
  border: 1px solid var(--border-color);
  border-radius: 16px;
  background-color: var(--bg-color);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  color: var(--text-primary);
}
.profile__stats {
  display: flex;
  gap: 24px;
  margin: 16px 0;
}
.stats__item {
  text-align: center;
  cursor: pointer;
}
.stats__value {
  font-size: 18px;
  font-weight: 700;
  display: block;
  color: var(--text-primary);
}
.stats__label {
  font-size: 12px;
  color: var(--text-secondary);
}
.intro {
  padding: 0 16px;
  margin-bottom: 4px;
  width: 100%;
  box-sizing: border-box;
  max-height: 120px;
  overflow-y: auto;
}

.intro::-webkit-scrollbar {
  width: 4px;
}

.intro::-webkit-scrollbar-track {
  background: var(--fill-color);
  border-radius: 2px;
}

.intro::-webkit-scrollbar-thumb {
  background: var(--border-color);
  border-radius: 2px;
}

.intro::-webkit-scrollbar-thumb:hover {
  background: var(--text-secondary);
}
.intro__text {
  font-size: 14px;
  line-height: 1.5;
  margin-bottom: 8px;
  color: var(--text-primary);
  word-wrap: break-word;
  white-space: pre-wrap;
  overflow-wrap: break-word;
  word-break: break-word;
  max-width: 100%;
}
.intro__tag {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}
.tag__item {
  padding: 2px 8px;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  font-size: 12px;
  color: var(--text-secondary);
}
.unlogin-button-container {
  padding: 20px 16px;
  display: flex;
  justify-content: center;
}
.custom-button {
  width: 100%;
  height: 44px;
  border-radius: 22px;
  background-color: #1890ff;
  border: none;
  color: #fff;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.button-content {
  display: flex;
  align-items: center;
  gap: 8px;
}
.button-icon {
  font-size: 18px;
}
.content-tabs {
  display: flex;
  justify-content: space-around;
  height: 40px;
  align-items: center;
  border-bottom: 1px solid var(--border-color);
  background-color: var(--bg-color);
  width: 100%;
  box-sizing: border-box;
}
.content-tabs__item {
  font-size: 14px;
  color: var(--text-secondary);
  position: relative;
  cursor: pointer;
  padding: 0 8px;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}
.content-tabs__item.active {
  color: #1890ff;
  font-weight: 500;
}
.content-tabs__item.active::after {
  content: "";
  position: absolute;
  bottom: -1px;
  left: 0;
  width: 100%;
  height: 2px;
  background-color: #1890ff;
}

/* 新增：标签加载图标样式 */
.tab-loading-icon {
  font-size: 12px;
  color: #1890ff;
  margin-left: 4px;
}

.tab-loading-icon-small {
  font-size: 10px;
  color: #1890ff;
  margin-left: auto;
}

/* 切换按钮样式 - 极致简约版 */
.view-mode-toggle-btn {
  background: transparent;
  border: none;
  padding: 4px;
  margin-left: 6px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-secondary);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  font-size: 16px;
  border-radius: 50%;
}

.view-mode-toggle-btn:hover {
  background: var(--hover-background);
  color: #1890ff;
  transform: scale(1.1);
}

.view-mode-toggle-btn i {
  font-size: 14px;
}

.loading-text {
  margin-top: 16px;
  font-size: 14px;
  color: #666;
}

.works {
  position: relative;
  gap: 2px;
  padding: 2px;
  background-color: var(--fill-color);
  width: 100%;
  box-sizing: border-box;
  overflow-y: auto;
}

.picture-actions {
  padding: 0 4px;
  margin-top: 6px;
  background-color: var(--bg-color);
  width: 100%;
  box-sizing: border-box;
}

.two-button-container {

  display: flex;
  gap: 8px;
  width: 100%;
}

.draft-btn {
  flex: 1;
  height: 44px !important;
  border: 1px solid var(--border-color) !important;
  border-radius: 6px !important;
  color: var(--text-primary);
  font-size: 14px !important;
  font-weight: 500 !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  gap: 6px !important;
  transition: all 0.2s ease !important;
}

.draft-btn-small {
  flex: 1;
  height: 36px !important;
  border: 1px solid #bfbfbf !important;
  border-radius: 6px !important;
  color: var(--text-primary);
  font-size: 12px !important;
  font-weight: 500 !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  gap: 4px !important;
  transition: all 0.2s ease !important;
}

.draft-btn.space-btn {
  background-color: var(--fill-color);
  border-color: #1890ff !important;
  color: #1890ff;
}

.draft-btn.draft-box-btn {
  background-color: var(--fill-color);
  border-color: var(--border-color) !important;
  color: var(--text-secondary);
}

.draft-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0,0,0,0.15);
}
.works__item {
  width: calc(33.333% - 1.333px);
  aspect-ratio: 1/1;
  overflow: hidden;
}
.works__item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.post-item {
  padding: 16px;
  border-bottom: 1px solid var(--border-color);
}
.post-item h3 {
  font-size: 16px;
  color: var(--text-primary);
  margin-bottom: 8px;
}
.post-item p {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.5;
}
.loading-state {
  min-height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  box-sizing: border-box;
}
.loader {
  width: 48px;
  height: 48px;
  border: 5px solid var(--fill-color);
  border-bottom-color: #1890ff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}
@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(280deg); }
}
.error-state, .empty-state {
  padding: 60px 24px;
  text-align: center;
  width: 100%;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.custom-empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  animation: emptyFadeIn 0.8s ease-out;

  .empty-illustration {
    width: 200px;
    height: auto;
    opacity: 0.8;
  }

  .empty-desc {
    font-size: 16px;
    color: var(--text-primary);
    margin-top: 12px;
    font-weight: 500;
  }
}

@keyframes emptyFadeIn {
  from { opacity: 0; transform: translateY(15px); }
  to { opacity: 1; transform: translateY(0); }
}
.load-more-loading {
  padding: 16px;
  text-align: center;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 14px;
  color: var(--text-secondary);
  width: 100%;
  box-sizing: border-box;
}
.no-more-data {
  padding: 16px;
  text-align: center;
  font-size: 14px;
  color: var(--text-secondary);
  width: 100%;
  box-sizing: border-box;
}

/* 下拉刷新指示器 */
.refresh-indicator {
  position: absolute;
  top: -60px;
  left: 0;
  right: 0;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.2s ease;
}

.refresh-content {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: var(--text-secondary);
  font-size: 14px;
}

.pull-arrow {
  transition: transform 0.2s ease;
  font-size: 18px;
}

.loading-spinner {
  width: 18px;
  height: 18px;
  border: 2px solid var(--border-color);
  border-top-color: #1890ff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

.refresh-text {
  font-size: 12px;
  color: var(--text-secondary);
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.refresh-indicator.refreshing .loading-spinner {
  display: block;
}

.refresh-indicator.refreshing .pull-arrow {
  display: none;
}

.refresh-indicator.pulled .loading-spinner {
  display: none;
}

.refresh-indicator.pulled .pull-arrow {
  display: block;
}
.custom-draft-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 1000;
}
.modal-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0,0,0,0.5);
}
.modal-container {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 90%;
  max-width: 400px;
  background-color: #fff;
  border-radius: 12px;
  overflow: hidden;
}
.modal-header {
  padding: 16px;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.modal-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}
.close-btn {
  font-size: 20px;
  color: var(--text-secondary);
  cursor: pointer;
}
.modal-body {
  padding: 2.4px;
  max-height: 60vh;
  overflow-y: auto;
}

/* 移动端互动标签下拉菜单 */
.mobile-interaction-tab {
  position: relative;
}

.interaction-dropdown {
  position: absolute;
  top: 100%;
  right: -12px;
  width: 88px;
  background: var(--background);
  border: 1px solid var(--border-color);
  border-radius: 6px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  z-index: 100;
  margin-top: 4px;
  overflow: hidden;
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  cursor: pointer;
  transition: background-color 0.2s ease;
  font-size: 14px;
  justify-content: space-between;
}

.dropdown-item:hover {
  background-color: var(--hover-background);
}

.dropdown-item.active {
  background-color: #1890ff;
  color: white;
}

.dropdown-icon {
  transition: transform 0.3s ease;
  font-size: 12px;
  margin-left: 4px;
}

.dropdown-icon.rotated {
  transform: rotate(180deg);
}

.post-actions{
  padding: 4px ;
}
</style>
