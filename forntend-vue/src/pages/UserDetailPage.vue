<template>
  <div id="usersDetailPage" ref="pageRef">
    <div class="top-nav">
      <div class="top-nav__left">
        <i class="fas fa-arrow-left back-icon" @click="goBack" style="cursor: pointer; font-size: 18px; color: #fff;"></i>
      </div>
      <div class="top-nav__right">
        <i class="fas fa-share-alt share-icon" @click="handleShare" style="cursor: pointer; font-size: 16px; color: #fff; margin-right: 4px;"></i>
      </div>
    </div>

    <div class="page-content">
      <!-- 背景图区域 - 添加右下角按钮 -->
      <div class="bg-banner" :style="{ backgroundImage: `url(${userHomepageBg})` }">
        <!-- 私聊/关注按钮组 - 固定在背景图右下角 -->
        <div class="banner-actions" v-if="showActionButtons">
          <van-button
            round
            size="small"
            type="default"
            @click="startPrivateChat"
            :disabled="!canPrivateChat"
            class="chat-button"
          >
            <i class="fas fa-comment-dots chat-icon"></i>
            私聊
          </van-button>
          <van-button
            round
            size="small"
            :type="isFollowing ? 'default' : 'primary'"
            :loading="followLoading"
            @click="toggleFollow"
            :disabled="!canFollow"
            class="follow-button"
          >
            {{ isFollowing ? '已关注' : '关注' }}
          </van-button>
          <van-button
            round
            size="small"
            type="danger"
            @click="showReportModal"
            class="report-button"
            v-if="loginUserStore.loginUser?.id && loginUserStore.loginUser?.id !== String(userInfo.id)"
          >
            <i class="fas fa-exclamation-triangle report-icon"></i>
            举报
          </van-button>
        </div>
      </div>

      <!-- 用户头像区域 -->
      <div class="profile">
        <div class="profile__avatar">
          <div class="avatar__img">
            <img style="border-radius: 50%; border: 3px solid #fff;" :src="userInfo.userAvatar || getDefaultAvatar(userInfo.userName)" alt="头像" />
          </div>
          <div>
            <div class="profile__name">{{ userInfo.userName }}</div>
            <div class="profile__id">
              悦木号: {{ userInfo.id }}
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
          <div
            class="stats__item"
            @click="goToFollowList('follow')"
            :class="{ 'disabled': !canShowFollowList }"
          >
            <span class="stats__value">{{ followCount || 0 }}</span>
            <span class="stats__label">关注</span>
          </div>
          <div
            class="stats__item"
            @click="goToFollowList('fans')"
            :class="{ 'disabled': !canShowFansList }"
          >
            <span class="stats__value">{{ fansCount || 0 }}</span>
            <span class="stats__label">粉丝</span>
          </div>
        </div>
      </div>

      <!-- 个性签名和标签 -->
      <div class="intro">
        <div class="intro__text" v-if="userInfo.userProfile">
          {{ userInfo.userProfile }}
        </div>
        <div class="intro__text" v-if="userInfo.personalSign">
          {{ userInfo.personalSign }}
        </div>
        <div class="intro__tag">
          <span class="tag__item" v-if="userInfo.region">{{ userInfo.region }}</span>
          <span class="tag__item" v-if="userInfo.gender || userInfo.birthday">{{ formatGenderAndAge(userInfo.gender, userInfo.birthday) }}</span>
          <span class="tag__item" v-for="tag in parseUserTags(userInfo.userTags)" :key="tag">{{ tag }}</span>
        </div>
      </div>

      <!-- 隐藏原来的用户操作按钮区域 -->
      <div class="user-actions" v-if="false">
        <div class="action-buttons">
          <van-button
            round
            size="small"
            type="default"
            @click="startPrivateChat"
            class="chat-button"
          >
            <i class="fas fa-comment-dots chat-icon"></i>
            私聊
          </van-button>
          <van-button
            round
            size="small"
            :type="isFollowing ? 'default' : 'primary'"
            :loading="followLoading"
            @click="toggleFollow"
            class="follow-button"
          >
            {{ isFollowing ? '已关注' : '关注' }}
          </van-button>
        </div>
      </div>

      <!-- 内容切换 -->
      <div class="content-switch">
        <div class="switch-tabs">
          <div
            class="switch-tab"
            :class="{ active: activeTab === 'pictures' }"
            @click="activeTab = 'pictures'"
          >
            <i class="fas fa-image picture-icon"></i>
            <span>图片 {{ pictureTotal > 0 ? `(${pictureTotal})` : '' }}</span>
          </div>
          <div
            class="switch-tab"
            :class="{ active: activeTab === 'posts' }"
            @click="activeTab = 'posts'"
          >
            <i class="fas fa-file-alt post-icon"></i>
            <span>帖子 {{ postTotal > 0 ? `(${postTotal})` : '' }}</span>
          </div>
        </div>
      </div>

      <!-- 内容展示区域 -->
      <div class="content-area" ref="contentAreaRef">
        <div v-show="activeTab === 'pictures'" class="pictures-section">
          <!-- 图片列表 -->
          <div v-if="device === DEVICE_TYPE_ENUM.PC">
            <div class="picture-list-container">
              <div class="picture-list-wrapper">
                <WaterfallPictureList
                  :dataList="pictureList"
                  :loading="loading"
                  :onLoadMore="loadMorePictures"
                />
              </div>
            </div>
          </div>
          <!-- 移动用户发布的图片列表 -->
          <div class="pictures-section" v-if="device!==DEVICE_TYPE_ENUM.PC">
            <div class="picture-list-container">
              <div class="picture-list-wrapper">
                <MobilePictureList
                  :dataList="pictureList"
                  :loading="loading"
                />
                <!-- 加载更多提示 -->
                <div class="load-more-status" v-if="pictureList.length > 0 && loading">
                  <van-loading size="24px">加载中...</van-loading>
                </div>
                <div v-else-if="isLoadingMore" class="load-more-loading">
                  <van-loading size="24px" />
                  <span>加载中...</span>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div v-show="activeTab === 'posts'" class="posts-section">
          <PostList
            :dataList="postList"
            :loading="postLoading"
            :showStatus="false"
          />
          <!-- 加载更多提示 -->
          <div class="load-more-status" v-if="postList.length > 0 && postLoading">
            <van-loading size="24px">加载中...</van-loading>
          </div>
          <div v-else-if="isLoadingMore" class="load-more-loading">
            <van-loading size="24px" />
            <span>加载中...</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 分享模态框 -->
    <ShareModal
      ref="shareModalRef"
      :title="'悦木图库-用户详情'"
      :link="userHomeLink"
      :image-url="getImageUrlForShare()"
      :user="userInfo"
      :create-time="userInfo.createTime"
    />

    <!-- 举报模态框 -->
    <ReportModal
      ref="reportModalRef"
      :open="reportModalVisible"
      :target-id="String(userInfo.id)"
      target-type="4"
      @update:open="handleReportModalChange"
      @success="handleReportSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import dayjs from 'dayjs'
import { getDefaultAvatar } from '@/utils/userUtils'
import { message } from 'ant-design-vue'
import { createOrUpdatePrivateChatUsingPost } from '@/api/privateChatController'
import { getFollowAndFansCountUsingPost, addUserFollowsUsingPost, findIsFollowUsingPost } from '@/api/userFollowsController'
import { getFollowOrFanPictureUsingPost } from '@/api/userFollowsController'
import WaterfallPictureList from '@/components/WaterfallPictureList.vue'
import { getDeviceType } from '@/utils/device.ts'
import { DEVICE_TYPE_ENUM } from '@/constants/device.ts'
import MobilePictureList from '@/components/MobilePictureList.vue'
import { MessageOutlined, PictureOutlined, FileTextOutlined, ArrowLeftOutlined, ShareAltOutlined, CopyOutlined, WarningOutlined } from '@ant-design/icons-vue'
import { listPostByPageUsingPost } from '@/api/postController'
import PostList from '@/components/PostList.vue'
import { getUserPublicInfoUsingGet } from '@/api/userController'
import ShareModal from '@/components/ShareModal.vue'
import ReportModal from '@/components/ReportModal.vue'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()
const device = ref<string>('')
const activeTab = ref('pictures')
const postList = ref<API.Post[]>([])
const postTotal = ref(0)
const postLoading = ref(false)
const pictureTotal = ref(0)
const postPagination = ref({
  current: 1,
  pageSize: 8
})
const isEndOfData = ref(false)
const isLoadingMore = ref(false)
const copySuccess = ref(false)

// 滚动加载相关
const contentAreaRef = ref<HTMLElement | null>(null)
let scrollHandler: any = null

// 分享模态框引用
const shareModalRef = ref<any>(null)

// 举报模态框引用
const reportModalRef = ref<any>(null)
const reportModalVisible = ref(false)

// 修改滚动相关的引用
const pageRef = ref<HTMLElement | null>(null)

// 修改滚动检测逻辑
const checkScrollBottom = () => {
  const scrollTop = Math.max(
    window.pageYOffset,
    document.documentElement.scrollTop,
    document.body.scrollTop
  )
  const windowHeight = window.innerHeight
  const documentHeight = Math.max(
    document.documentElement.scrollHeight,
    document.body.scrollHeight
  )
  const threshold = 100

  if (documentHeight - (scrollTop + windowHeight) < threshold) {
    if (activeTab.value === 'pictures' && !loading.value && !isEndOfData.value) {
      handleAutoLoad()
    } else if (activeTab.value === 'posts' && !postLoading.value) {
      handleAutoLoadPosts()
    }
  }
}

// 自动加载更多图片
const handleAutoLoad = async () => {
  if (loading.value || isEndOfData.value || isLoadingMore.value) return

  isLoadingMore.value = true

  const nextPage = currentPage.value + 1
  const maxPage = Math.ceil(total.value / pageSize)

  if (nextPage <= maxPage) {
    currentPage.value = nextPage
    await loadPictureData()
  }

  isLoadingMore.value = false
}

// 自动加载更多帖子
const handleAutoLoadPosts = async () => {
  if (postLoading.value || isLoadingMore.value) return

  isLoadingMore.value = true

  const nextPage = postPagination.value.current + 1
  const maxPage = Math.ceil(postTotal.value / postPagination.value.pageSize)

  if (nextPage <= maxPage) {
    postPagination.value.current = nextPage
    await loadPostData()
  }

  isLoadingMore.value = false
}

// 返回上一页
const goBack = () => {
  router.go(-1)
}

// 分享处理
const handleShare = () => {
  if (!loginUserStore.loginUser?.id) {
    message.warning('请先登录')
    return
  }
  shareModalRef.value?.openModal()
}

// 分享链接计算
const userHomeLink = computed(() => {
  const user = userInfo.value || {}
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
  if (userInfo.value?.homepageBg) {
    return userInfo.value.homepageBg;
  }
  // 其次使用用户头像
  if (userInfo.value?.userAvatar) {
    return userInfo.value.userAvatar;
  }
  // 最后使用默认头像
  return getDefaultAvatar(userInfo.value.userName);
}

// 页面加载时获取设备类型并获取数据
onMounted(async () => {
  device.value = await getDeviceType()
  // console.log(route.params)
  await Promise.all([
    getUserPublicInfo(),
    getFollowAndFansCount(),
    checkIsFollowing(),
    loadPictureData(),
    loadPostData()
  ])
  // 延迟设置滚动监听，确保内容已渲染
  nextTick(() => {
    setupScrollListener()
  })
})

// 组件卸载时清理
onUnmounted(() => {
  removeScrollListener()
})

// 修改标签切换处理
watch(activeTab, () => {
  // 切换标签时滚动到顶部
  window.scrollTo({ top: 0, behavior: 'smooth' })
})

// 初始化用户信息
const userInfo = ref<API.UserPublicVO>({
  id: String(route.params.id),
  userName: route.query.userName as string,
  userAvatar: route.query.userAvatar as string,
  userAccount: route.query.userAccount as string,
  userProfile: route.query.userProfile as string,
  userRole: route.query.userRole as string,
  createTime: route.query.createTime as string,
  birthday: route.query.birthday as string,
  gender: route.query.gender as string,
  homepageBg: route.query.homepageBg as string,
  personalSign: route.query.personalSign as string,
  region: route.query.region as string,
  userTags: route.query.userTags as string,
  allowPrivateChat: 1,
  allowFollow: 1,
  showFollowList: 1,
  showFansList: 1
})

// 计算用户背景图
const userHomepageBg = computed(() => {
  if (userInfo.value.homepageBg) {
    return userInfo.value.homepageBg;
  }
  // 默认背景图
  return 'https://images.unsplash.com/photo-1501854140801-50d01698950b?ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D&auto=format&fit=crop&w=1200&q=80';
})

// 格式化性别和年龄
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

// 解析用户标签
const parseUserTags = (tagsStr: string | undefined) => {
  if (!tagsStr) return [];
  return tagsStr.split(/[;,，；]/).filter(tag => tag.trim() !== '');
};

console.log(route.params)
const followCount = ref(0)
const fansCount = ref(0)
const isFollowing = ref(false)
const followLoading = ref(false)

// 格式化日期
const formatDate = (date: string) => {
  return dayjs(date).format('YYYY-MM-DD')
}

// 格式化角色
const formatRole = (role: string) => {
  const roleMap: Record<string, string> = {
    user: '普通用户',
    admin: '管理员',
    ban: '封禁用户'
  }
  return roleMap[role] || role
}

// 权限相关字段
const canPrivateChat = computed(() => {
  if (!userInfo.value.allowPrivateChat) {
    return false
  }
  return loginUserStore.loginUser?.id && loginUserStore.loginUser?.id !== userInfo.value.id
})

const canFollow = computed(() => {
  if (!userInfo.value.allowFollow) {
    return false
  }
  return loginUserStore.loginUser?.id && loginUserStore.loginUser?.id !== userInfo.value.id
})

const canShowFollowList = computed(() => {
  if (!userInfo.value.showFollowList) {
    return false
  }
  return true
})

const canShowFansList = computed(() => {
  if (!userInfo.value.showFansList) {
    return false
  }
  return true
})

// 是否显示关注按钮
const showActionButtons = computed(() => {
  return loginUserStore.loginUser?.id && loginUserStore.loginUser?.id !== userInfo.value.id
})

// 获取关注和粉丝数
const getFollowAndFansCount = async () => {
  try {
    const userId = userInfo.value.id
    const res = await getFollowAndFansCountUsingPost({ id: userId })
    if (res.data.code === 0) {
      const { followCount: fc, fansCount: fs } = res.data.data
      followCount.value = fc || 0
      fansCount.value = fs || 0
    }
  } catch (error) {
    console.error('获取关注粉丝数失败:', error)
    message.error('获取关注粉丝数失败')
  }
}

// 检查是否已关注
const checkIsFollowing = async () => {
  try {
    const res = await findIsFollowUsingPost({
      followerId: String(loginUserStore.loginUser?.id),
      followingId: String(userInfo.value.id)
    })
    if (res.data?.code === 0) {
      isFollowing.value = res.data.data
    }
  } catch (error) {
    console.error('检查关注状态失败:', error)
  }
}

// 关注/取消关注
const toggleFollow = async () => {
  // 检查关注权限
  if (!userInfo.value.allowFollow) {
    message.warning('对方设置了隐私权限，不允许关注')
    return
  }

  if (!loginUserStore.loginUser?.id) {
    message.warning('请先登录')
    return
  }

  if (followLoading.value) return
  followLoading.value = true

  try {
    const res = await addUserFollowsUsingPost({
      followerId: String(loginUserStore.loginUser.id),
      followingId: String(userInfo.value.id),
      followingName: userInfo.value.userName,
      followerName: loginUserStore.loginUser.userName,
      followStatus: isFollowing.value ? 0 : 1
    })

    if (res.data?.code === 0) {
      isFollowing.value = !isFollowing.value
      message.success(isFollowing.value ? '关注成功' : '已取消关注')
      await getFollowAndFansCount()
    }
  } catch (error) {
    console.error('操作失败:', error)
    message.error('操作失败，请稍后重试')
  } finally {
    followLoading.value = false
  }
}

// 跳转到关注/粉丝列表
const goToFollowList = (type: 'follow' | 'fans') => {
  // 检查权限
  if (type === 'follow' && !userInfo.value.showFollowList) {
    message.warning('对方设置了隐私权限，不展示关注列表')
    return
  }

  if (type === 'fans' && !userInfo.value.showFansList) {
    message.warning('对方设置了隐私权限，不展示粉丝列表')
    return
  }

  // 检查是否登录
  if (!loginUserStore.loginUser?.id) {
    message.warning('请先登录')
    return
  }

  router.push({
    path: '/follow-list',
    query: {
      type,
      userId: userInfo.value.id,
      userName: userInfo.value.userName,
      userAvatar: userInfo.value.userAvatar
    }
  })
}

// 图片列表相关
const pictureList = ref<API.PictureVO[]>([])
const loading = ref(false)
const currentPage = ref(1)
const total = ref(0)
const pageSize = 12

// 加载图片数据
const loadPictureData = async () => {
  try {
    // 区分首次加载和加载更多
    if (currentPage.value === 1) {
      loading.value = true
    }
    const res = await getFollowOrFanPictureUsingPost({
      userId: String(userInfo.value.id),
      current: currentPage.value,
      pageSize: pageSize,
      sortField: 'createTime',
      sortOrder: 'descend'
    })

    if (res.data?.code === 0) {
      const newRecords = res.data.data.records || []
      if (currentPage.value === 1) {
        pictureList.value = newRecords
      } else {
        pictureList.value = [...pictureList.value, ...newRecords]
      }
      total.value = res.data.data.total || 0
      pictureTotal.value = res.data.data.total || 0
      isEndOfData.value = !newRecords.length || newRecords.length < pageSize
    }
  } catch (error) {
    console.error('获取图片列表失败:', error)
    message.error('获取图片列表失败')
  } finally {
    loading.value = false
    // 确保在finally块中也重置isLoadingMore
    if (!loading.value && !isEndOfData.value && currentPage.value > 1) {
      isLoadingMore.value = false
    }
  }
}

// 加载更多图片
const loadMorePictures = async (nextPage: number) => {
  if (isEndOfData.value || isLoadingMore.value) return false

  try {
    isLoadingMore.value = true
    currentPage.value = nextPage
    const res = await getFollowOrFanPictureUsingPost({
      userId: String(userInfo.value.id),
      current: nextPage,
      pageSize: pageSize,
      sortField: 'createTime',
      sortOrder: 'descend'
    })

    if (res.data?.code === 0) {
      const newData = res.data.data.records || []
      if (newData.length > 0) {
        pictureList.value = [...pictureList.value, ...newData]
        total.value = res.data.data.total || 0
        return true
      } else {
        isEndOfData.value = true
      }
    }
    return false
  } catch (error) {
    console.error('加载更多图片失败:', error)
    return false
  } finally {
    isLoadingMore.value = false
  }
}

// 修改加载帖子数据的函数
const loadPostData = async () => {
  try {
    // 区分首次加载和加载更多
    if (postPagination.value.current === 1) {
      postLoading.value = true
    }
    const res = await listPostByPageUsingPost({
      userId: userInfo.value.id,
      current: postPagination.value.current,
      pageSize: postPagination.value.pageSize,
      status: 1
    })

    if (res.data?.code === 0) {
      const newRecords = res.data.data.records || []
      if (postPagination.value.current === 1) {
        postList.value = newRecords
      } else {
        postList.value = [...postList.value, ...newRecords]
      }
      postTotal.value = res.data.data.total || 0
    }
  } catch (error) {
    console.error('获取帖子列表失败:', error)
    message.error('获取帖子列表失败')
  } finally {
    postLoading.value = false
    // 确保在finally块中也重置isLoadingMore
    if (!postLoading.value && postPagination.value.current > 1) {
      isLoadingMore.value = false
    }
  }
}

// 获取用户公开信息
const getUserPublicInfo = async () => {
  try {
    const res = await getUserPublicInfoUsingGet({
      userId: String(route.params.id)
    })
    if (res.data?.code === 0 && res.data.data) {
      // 更新用户信息
      Object.assign(userInfo.value, res.data.data)
    }
  } catch (error) {
    console.error('获取用户公开信息失败:', error)
    message.error('获取用户信息失败')
  }
}

// 监听路由参数变化，重新获取数据
watch(() => route.params.id, async (newId) => {
  if (newId) {
    currentPage.value = 1
    isEndOfData.value = false
    pictureList.value = []
    await Promise.all([
      getUserPublicInfo(),
      getFollowAndFansCount(),
      checkIsFollowing(),
      loadPictureData(),
      loadPostData()
    ])
  }
})

// 添加私聊处理函数
const startPrivateChat = async () => {
  // 检查私聊权限
  if (!userInfo.value.allowPrivateChat) {
    message.warning('对方设置了隐私权限，不允许私聊')
    return
  }

  // 检查是否登录
  if (!loginUserStore.loginUser?.id) {
    message.warning('请先登录')
    return
  }

  // 检查是否是自己
  if (userInfo.value.id === loginUserStore.loginUser?.id) {
    message.warning('不能私聊自己')
    return
  }

  try {
    // 先创建或更新私聊记录
    const res = await createOrUpdatePrivateChatUsingPost({
      targetUserId: userInfo.value.id,
      lastMessage: '发起聊天'
    })

    if (res.data?.code === 0 && res.data.data) {
      const chat = res.data.data
      // 跳转到聊天页面
      router.push({
        path: `/chat/${userInfo.value.id}`,
        query: {
          privateChatId: chat.id,
          userName: userInfo.value.userName,
          userAvatar: userInfo.value.userAvatar,
          userAccount: userInfo.value.userAccount,
          createTime: userInfo.value.createTime,
          isSender: 'true'  // 从用户详情页发起的私聊，当前用户一定是发送者
        }
      })
    } else {
      message.error('创建私聊失败')
    }
  } catch (error) {
    console.error('创建私聊失败:', error)
    message.error('创建私聊失败，请稍后重试')
  }
}

// 修改分页处理函数，使用 window.scrollTo
const handlePageChange = (page: number) => {
  currentPage.value = page
  window.scrollTo({ top: 0, behavior: 'smooth' })
  loadPictureData()
}

const handlePostPageChange = (page: number) => {
  postPagination.value.current = page
  window.scrollTo({ top: 0, behavior: 'smooth' })
  loadPostData()
}

// 优化滚动监听设置
const setupScrollListener = () => {
  // 确保只添加一次监听器
  removeScrollListener()
  scrollHandler = () => {
    requestAnimationFrame(checkScrollBottom)
  }
  window.addEventListener('scroll', scrollHandler, { passive: true })
}

// 修改移除滚动监听
const removeScrollListener = () => {
  if (scrollHandler) {
    window.removeEventListener('scroll', scrollHandler)
  }
}

// 复制悦木号
const copyUserId = async () => {
  try {
    await navigator.clipboard.writeText(userInfo.value.id)
    copySuccess.value = true
    message.success('悦木号复制成功')

    // 2秒后重置状态
    setTimeout(() => {
      copySuccess.value = false
    }, 2000)
  } catch (err) {
    console.error('复制失败:', err)
    message.error('复制失败，请手动复制')

    // 降级方案：使用旧的复制API
    try {
      const textArea = document.createElement('textarea')
      textArea.value = userInfo.value.id
      document.body.appendChild(textArea)
      textArea.select()
      document.execCommand('copy')
      document.body.removeChild(textArea)
      copySuccess.value = true
      message.success('悦木号复制成功')

      setTimeout(() => {
        copySuccess.value = false
      }, 2000)
    } catch (fallbackErr) {
      console.error('降级复制也失败:', fallbackErr)
    }
  }
}

// 显示举报模态框
const showReportModal = () => {
  if (!loginUserStore.loginUser?.id) {
    message.warning('请先登录')
    return
  }

  if (userInfo.value.id === loginUserStore.loginUser?.id) {
    message.warning('不能举报自己')
    return
  }

  reportModalVisible.value = true
}

// 处理举报模态框状态变化
const handleReportModalChange = (val: boolean) => {
  reportModalVisible.value = val
}

// 处理举报成功回调
const handleReportSuccess = () => {
  message.success('举报提交成功，我们会尽快处理')
  reportModalVisible.value = false
}
</script>

<style scoped>
#usersDetailPage {
  background: var(--header-background);
  color: var(--text-primary);
  margin-bottom: 44px;
}

.page-content {
  max-width: 1400px!important;
  margin: 0 auto;
}


:deep(.van-pagination__item) {
  margin: 0;
  border-radius: 8px;
  min-width: 36px;
  transition: all 0.3s ease;

  &:hover {
    color: #2563eb; /* 主色：蓝色 */
    background: #eff6ff; /* 浅蓝背景 */
  }
}

:deep(.van-pagination__item--active) {
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.2); /* 蓝色阴影 */

  &:hover {
    color: white;
    background: #2563eb; /* 主色：蓝色 */
  }
}

:deep(.van-pagination__prev),
:deep(.van-pagination__next) {
  background: var(--header-background);
  color: var(--text-primary);
  border: 1px solid #e2e8f0;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-family: monospace;

  &:hover {
    border-color: #2563eb; /* 主色：蓝色 */
    color: #2563eb; /* 主色：蓝色 */
  }

  &:active {
    background: #eff6ff; /* 浅蓝背景 */
  }
}

.user-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 8px;
  backdrop-filter: blur(8px);
  border: 1px solid rgba(226, 232, 240, 0.6);
}


.action-buttons {
  display: flex;
  gap: 8px;
  align-items: center;
}

.chat-button {
  display: flex;
  align-items: center;
  height: 32px;
  padding: 0 16px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;

  &:hover {
    color: #2563eb; /* 主色：蓝色 */
    border-color: #2563eb; /* 主色：蓝色 */
    background: #eff6ff; /* 浅蓝背景 */
  }
}

.follow-button {
  height: 32px;
  padding: 0 16px;
}


.banner-actions {
  position: absolute;
  bottom: 20px;
  right: 20px;
  display: flex;
  gap: 8px;
  z-index: 10;
}

@media screen and (max-width: 768px) {

  .action-buttons {
    gap: 6px;
  }

  .chat-button,
  .follow-button {
    height: 30px;
    padding: 0 12px;
    font-size: 13px;
  }

  /* 移动端按钮位置调整 */
  .banner-actions {
    bottom: 15px;
    right: 15px;
  }
}

.pictures-section, .posts-section {
  background: var(--header-background);
  color: var(--text-primary);
  border-radius: 12px;
  margin-bottom: 24px;
  width: 100%;
  position: relative;
}

.picture-list-container {
  width: 100%;
}

.picture-list-wrapper {
  position: relative;
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .picture-list-container {
    margin: 0;
  }

  .picture-list-wrapper {
    padding: 0;
  }

  .empty-state,
  .pagination-wrapper {
    margin-left: 12px;
    margin-right: 12px;
  }

  :deep(.van-pagination__item) {
    min-width: 32px;
    height: 32px;
  }

  :deep(.van-pagination__prev),
  :deep(.van-pagination__next) {
    width: 32px;
    height: 32px;
    font-size: 16px;
  }
}

/* 切换按钮样式 */
.content-switch {
  padding: 8px 16px;
  background: var(--header-background);
  color: var(--text-primary);
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
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  user-select: none;

  &:hover:not(.active) {
    color: #2563eb; /* 主色：蓝色 */
    background: rgba(37, 99, 235, 0.05); /* 浅蓝透明背景 */
  }

  &.active {
    color: #2563eb; /* 主色：蓝色 */
    font-weight: 500;
    background: linear-gradient(135deg, #eff6ff 0%, #fff 100%); /* 浅蓝渐变 */

  }

  :deep(.anticon) {
    font-size: 16px;
  }
}

/* 添加滚动加载相关样式 */
.content-area {
  width: 100%;
  position: relative;
  margin-bottom: 64px;
}

.load-more-status {
  text-align: center;
  padding: 16px 0;
  color: #64748b;
  font-size: 14px;
  background: var(--header-background);
  color: var(--text-primary);
  margin-top: 10px;
  border-radius: 8px;
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

/* 顶部导航栏样式 */
.top-nav {
  position: relative;
  height: 44px;
  max-width: 1400px !important;
  padding: 0 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #fff;
  z-index: 100;
  width: 100%;
  box-sizing: border-box;
  margin: 0 auto;
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
  margin: 0 4px;
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

.top-nav__right span {
  font-size: 14px;
  background: rgba(0,0,0,0.3);
  padding: 2px 8px;
  border-radius: 10px;
}

.bg-banner {
  margin-top: -44px!important;
  height: 250px;
  background-size: cover;
  background-position: center;
  position: relative;
  width: 100%;
  box-sizing: border-box;
}

@media (min-width: 768px) {
  .bg-banner {
    height: 420px;
  }
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

.stats__item.disabled {
  opacity: 0.5;
  cursor: not-allowed;
  pointer-events: none;
}

/* 添加按钮禁用样式 */
:deep(.van-button--disabled) {
  opacity: 0.5;
  cursor: not-allowed;
}

.intro {
  padding: 0 16px;
  margin-bottom: 12px;
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

/* 确保移动端图片列表容器样式正确 */
.pictures-section {
  min-height: 100%;
}

.picture-list-container {
  padding-bottom: 20px;
}
</style>
