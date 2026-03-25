<template>
  <div class="post-detail-page">
    <!-- 自动滚动指示器 -->
    <div
      v-if="showAutoScrollIndicator"
      class="auto-scroll-indicator"
      @click="stopPostAutoScroll"
      title="点击停止自动滚动">
      <i class="fas fa-stop"></i>
    </div>
    <div class="content-layer">
      <div class="pc-container" :class="{ 'comments-hidden': !isCommentVisible }">

        <!-- PC Floating Action Bar -->
        <div class="pc-floating-actions" v-if="!isMobile">
          <div class="action-item" :class="{ active: post.isLiked === 1 }" @click="handleLike" title="点赞">
            <i :class="post.isLiked === 1 ? 'fas fa-heart' : 'far fa-heart'"></i>
            <span class="count" v-if="post.likeCount">{{ post.likeCount }}</span>
          </div>
          <div class="action-item" @click="toggleCommentSection" :class="{ active: isCommentVisible }" title="评论">
            <i class="fas fa-comment"></i>
            <span class="count" v-if="post.commentCount">{{ post.commentCount }}</span>
          </div>
          <div class="action-item" :class="{ active: post.isFavorited === 1 }" @click="doFavorite" title="收藏">
            <i :class="post.isFavorited === 1 ? 'fas fa-star' : 'far fa-star'"></i>
            <span class="count" v-if="post.favoriteCount">{{ post.favoriteCount }}</span>
          </div>
          <div class="action-item" :class="{ active: post.isShared === 1 }" @click="handleShare" title="分享">
            <i class="fas fa-share"></i>
          </div>
          <div class="action-item more-action-item" @click="showMoreModal = true" title="更多">
            <i class="fas fa-ellipsis-h"></i>
          </div>
        </div>

        <div class="preview-section">
          <div class="post-card">
            <div class="content-scroll-wrapper" ref="contentScrollWrapper">
              <div class="info-card">
                <div class="card-header">
                  <h1 class="post-title">{{ post.title }}</h1>
                  <div class="post-meta-info">
                    <span class="meta-author" @click="handleUserClick(post.user)">{{ post.user?.userName || '匿名用户' }}</span>
                    <span class="meta-time">{{ formatShortDate(post.createTime) }}</span>

                    <!-- 移动端标签换行修复区 -->
                    <div class="tags-wrapper" v-if="post?.category || (post?.tagList && post.tagList.length > 0)">
                      <span v-if="post?.category" class="meta-category">{{ post?.category }}</span>
                      <span v-for="tag in post?.tagList" :key="tag" class="meta-tag">#{{ tag }}</span>
                    </div>

                    <span v-if="post.user?.id !== loginUserStore.loginUser?.id"
                          class="meta-follow"
                          :class="{ 'is-followed': isFollowed, 'is-loading': followLoading }"
                          @click="!followLoading && handleFollow()">
                      {{ isFollowed ? '已关注' : '关注' }}
                    </span>
                  </div>
                </div>
                <div class="centralized-info">
                  <div class="description-and-time">
                    <div class="info-item description">
                      <div class="value">
                        <Html-content :content="post.content" class="Html-content" />
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="info-section" ref="commentsRef" v-show="isCommentVisible">
          <div class="info-section-content">
            <div class="comment-section">
              <div class="comment-header">
                <h3>评论 ({{ post.commentCount || 0 }})</h3>
                <button class="close-comment-btn" @click="toggleCommentSection" title="收起评论区">
                  <i class="fas fa-times"></i>
                </button>
              </div>
              <div class="comments-list" ref="scrollContainer" @scroll="handleScroll">
                <comment-list :comments="comments" @reply-clicked="handleReplyClick" @update-comments="queryComments" />
                <div v-if="isEndOfData" class="end-message">没有更多评论了~</div>
              </div>
              <div class="comment-input-bar">
                <div class="input-wrapper">
                  <input
                    v-model="commentContent"
                    :placeholder="replyCommentId ? `回复 @${replyTargetUserName}：${replyTargetContent}` : '说点什么~'"
                    class="comment-input-field"
                    @input="handleInputChange"
                    @keydown.enter.prevent="addComment"
                    maxlength="200"
                    @focus="handleInputFocus"
                    @blur="handleInputBlur"
                  >

                  <div class="input-actions">
                    <button class="emoji-btn" @click.stop="toggleEmojiPicker" @mousedown.prevent>😊</button>
                    <button class="send-comment-btn" :disabled="!commentContent.trim()" @click="addComment" @mousedown.prevent>
                      {{ replyCommentId ? '回复' : '发送' }}
                    </button>
                  </div>
                </div>
              </div>
              <div v-if="showEmojiPicker" class="emoji-picker-wrapper" @mousedown.prevent>
                <emoji-picker class="custom-emoji-picker" :i18n="emojiI18n" @select="onEmojiSelect" />
              </div>
            </div>
          </div>
        </div>
      </div>
      <div class="mobile-comments-section" ref="mobileCommentsRef">
        <div class="info-section-content">
          <div class="comment-section mobile-comment-section">
            <div class="comment-header">
              <h3>评论 ({{ post.commentCount || 0 }})</h3>
            </div>
            <div class="comments-list mobile-comments-list" ref="mobileScrollContainer" @scroll="handleScroll">
              <comment-list :comments="comments" @reply-clicked="handleReplyClick" @update-comments="queryComments" />
              <div v-if="isEndOfData" class="end-message">没有更多评论了~</div>
            </div>
          </div>
        </div>
      </div>
      <div class="mobile-comment-input-fixed">
        <div class="comment-input-bar">
          <div class="input-wrapper">

            <input
              v-model="commentContent"
              :placeholder="replyCommentId ? `回复 @${replyTargetUserName}：${replyTargetContent}` : '说点什么~'"
              class="comment-input-field"
              @input="handleInputChange"
              @keydown.enter.prevent="addComment"
              maxlength="200"
              @focus="handleInputFocus"
              @blur="handleInputBlur"
            >
            <!-- 修改：根据焦点状态切换按钮 -->
            <div class="input-actions" v-if="!isInputFocused">
              <button class="action-btn btn-like" :class="{ 'is-liked': post.isLiked === 1 }" @click="handleLike">
                <i :class="post.isLiked === 1 ? 'fas fa-heart' : 'far fa-heart'"></i>
                <span>{{ post.likeCount || 0 }}</span>
              </button>
              <button class="action-btn btn-share" :class="{ 'is-shared': post.isShared === 1 }" @click="handleShare">
                <i class="fas fa-share"></i>
                <span>{{ post.shareCount || 0 }}</span>
              </button>
              <button class="action-btn btn-favorite" :class="{ 'is-favorited': post.isFavorited === 1 }" @click="doFavorite">
                <i :class="post.isFavorited === 1 ? 'fas fa-star' : 'far fa-star'"></i>
                <span>{{ post.favoriteCount || 0 }}</span>
              </button>
              <button class="action-btn btn-more-text" @click="showMoreModal = true">
                <span>更多</span>
              </button>
            </div>
            <div class="input-actions" v-else>
              <button class="emoji-btn" @click.stop="toggleEmojiPicker" @mousedown.prevent>😊</button>
              <button class="send-comment-btn" :disabled="!commentContent.trim()" @click="addComment" @mousedown.prevent>
                {{ replyCommentId ? '回复' : '发送' }}
              </button>
            </div>
          </div>
        </div>
        <div v-if="showEmojiPicker" class="emoji-picker-wrapper mobile-emoji-picker" @mousedown.prevent>
          <emoji-picker class="custom-emoji-picker" :i18n="emojiI18n" @select="onEmojiSelect" />
        </div>
      </div>
    </div>
    <share-modal ref="shareModalRef" :link="shareLink" :imageUrl="shareImage" :title="post.title" :user="post.user" :create-time="post.createTime" @share-success="handleShareSuccess" />
    <div class="delete-modal" v-if="showDeleteModal">
      <div class="delete-modal-mask" @click="showDeleteModal = false"></div>
      <div class="delete-modal-content">
        <div class="delete-modal-icon">
          <i class="fas fa-triangle-exclamation"></i>
        </div>
        <h3 class="delete-modal-title">确认删除帖子？</h3>
        <p class="delete-modal-desc">删除后无法恢复，请谨慎操作</p>
        <div class="delete-modal-actions">
          <button class="delete-modal-btn cancel" @click="showDeleteModal = false">取消</button>
          <button class="delete-modal-btn confirm" @click="confirmDelete">确认删除</button>
        </div>
      </div>
    </div>
    <ReportModal
      ref="reportModalRef"
      :target-type="reportTargetType"
      :target-id="reportTargetId"
    />

    <!-- 权限设置弹窗 -->
    <div v-if="showPermissionSetting" class="permission-setting-modal show">
      <div class="modal-mask" @click="showPermissionSetting = false"></div>
      <div class="modal-content">
        <div class="modal-header">
          <h3>权限设置</h3>
          <button class="close-btn" @click="showPermissionSetting = false">
            <i class="fas fa-times"></i>
          </button>
        </div>
        <div class="modal-body">
          <ContentPermissionSetting
            :post-id="route.params.id"
            :initial-permissions="{
              allowLike: post.allowLike ? 1 : 0,
              allowComment: post.allowComment ? 1 : 0,
              allowCollect: post.allowCollect ? 1 : 0,
              allowShare: post.allowShare ? 1 : 0
            }"
            @saved="() => { showPermissionSetting = false; fetchPostDetail(); }"
            @cancelled="() => showPermissionSetting = false"
          />
        </div>
      </div>
    </div>
  </div>

  <!-- 更多功能弹框 -->
  <div v-if="showMoreModal" class="more-modal">
    <div class="modal-mask" @click="showMoreModal = false"></div>
    <div class="modal-content">
      <div class="modal-header">
        <div class="header-content">
          <div class="author-info">
            <img
              :src="post.user?.userAvatar || getDefaultAvatar(post.user?.userName)"
              :alt="post.user?.userName"
              class="author-avatar"
            >
            <div class="author-text">
              <div class="author-name">{{ post.user?.userName || '匿名用户' }}</div>
              <div class="publish-time">{{ formatShortDate(post.createTime) }}</div>
            </div>
          </div>
          <button class="close-btn" @click="showMoreModal = false">
            <i class="fas fa-times"></i>
          </button>
        </div>
        <div class="post-info">
          <div class="post-details">
            <div class="post-title">{{ post.title || '未命名帖子' }}</div>
            <div class="post-stats">
              <span><i class="fas fa-eye"></i> {{ post.viewCount || 0 }}</span>
              <span><i class="fas fa-heart"></i> {{ post.likeCount || 0 }}</span>
              <span><i class="fas fa-comment"></i> {{ post.commentCount || 0 }}</span>
            </div>
          </div>
        </div>
      </div>
      <div class="modal-body">
        <div class="horizontal-scroll-container">
          <div class="action-buttons">
            <button class="action-btn" @click="handleEdit" v-if="canEdit">
              <i class="fas fa-edit"></i>
              <span>编辑</span>
            </button>
            <button class="action-btn" @click="showDeleteConfirm" v-if="canDelete">
              <i class="fas fa-trash-alt"></i>
              <span>删除</span>
            </button>
            <button class="action-btn" @click="togglePostAutoScroll">
              <i class="fas fa-play" v-if="!isPostAutoScrolling"></i>
              <i class="fas fa-pause" v-else></i>
              <span>{{ isPostAutoScrolling ? '暂停滚动' : '自动滚动' }}</span>
              <span v-if="isPostAutoScrolling" class="scrolling-indicator">●</span>
            </button>
            <button  class="action-btn" @click="openReportModal">
              <i class="fas fa-flag"></i>
              <span>举报</span>
            </button>
            <button v-if="post.user?.id === loginUserStore.loginUser?.id" class="action-btn" @click="showPermissionSetting = true">
              <i class="fas fa-lock"></i>
              <span>权限设置</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, nextTick, onUnmounted, watch, onActivated } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Image } from 'ant-design-vue'
import HtmlContent from '@/components/HtmlContent.vue'
import ShareModal from '@/components/ShareModal.vue'
import EmojiPicker from '@/components/EmojiPicker.vue'
import CommentList from '@/components/CommentList.vue'
import ReportModal from '@/components/ReportModal.vue'
import ContentPermissionSetting from '@/components/ContentPermissionSetting.vue'
import { getPostByIdUsingGet, deletePostUsingPost } from '@/api/postController'
import { addUserFollowsUsingPost, findIsFollowUsingPost } from '@/api/userFollowsController'
import { addCommentUsingPost, queryCommentUsingPost } from '@/api/commentsController'
import { doLikeUsingPost } from '@/api/likeRecordController'
import { doShareUsingPost } from '@/api/shareRecordController'
import { addFavoriteRecordUsingPost, cancelFavoriteUsingPost } from '@/api/favoriteRecordController'
import { getDefaultAvatar } from '@/utils/userUtils'
import { formatTime, formatShortDate } from '@/utils/dateUtils'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import { throttle } from 'lodash-es'
import { ViewDurationTracker } from '@/utils/viewDurationTracker'
import { getTextCover } from '@/utils/textCoverGenerator'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()
const post = ref<API.Post & {
  isLiked?: number;
  isFavorited?: number;
  isShared?: number;
  likeCount?: string | number;
  favoriteCount?: string | number;
  shareCount?: string | number;
  viewCount?: string | number;
  commentCount?: string | number;
}>({} as API.Post & {
  isLiked?: number;
  isFavorited?: number;
  isShared?: number;
  likeCount?: string | number;
  favoriteCount?: string | number;
  shareCount?: string | number;
  viewCount?: string | number;
  commentCount?: string | number;
})
const commentsRef = ref<HTMLElement | null>(null)
const mobileCommentsRef = ref<HTMLElement | null>(null)
const contentScrollWrapper = ref<HTMLElement | null>(null) // 新增内容容器引用
const isFollowed = ref(false)
const followLoading = ref(false)
const comments = ref<API.Comment[]>([])
// 防抖标识
const isAddingComment = ref(false)

// 浏览时长记录器
const viewDurationTracker = new ViewDurationTracker(2) // 2表示帖子类型

const commentContent = ref('')
const replyCommentId = ref('')
const replyTargetUserName = ref('')
const replyTargetContent = ref('')
const commentloading = ref(false)
const isEndOfData = ref(false)
const showEmojiPicker = ref(false)
const scrollContainer = ref<HTMLElement | null>(null)
const mobileScrollContainer = ref<HTMLElement | null>(null)
const queryRequest = ref<API.CommentsQueryRequest>({
  targetId: 0,
  targetType: 2,
  current: 1,
  pageSize: 15,
})
const shareModalRef = ref()
const reportModalRef = ref()
const reportTargetType = ref<string>('')
const reportTargetId = ref<string>('')
const showPermissionSetting = ref(false)

// 新增：跟踪输入框焦点状态
const isInputFocused = ref(false)
const isMobile = ref(window.innerWidth <= 768)

// 新增：PC端评论区显隐状态
const isCommentVisible = ref(true)
const toggleCommentSection = () => {
  isCommentVisible.value = !isCommentVisible.value
}

const shareLink = computed(() => window.location.origin + '/post/' + post.value?.id)

const handleResize = () => {
  const wasMobile = isMobile.value
  isMobile.value = window.innerWidth <= 768

  // 如果跨越了断点，需要更新滚动监听
  if (wasMobile !== isMobile.value) {
    if (isMobile.value) {
      window.addEventListener('scroll', handleMobileScroll)
      document.body.style.overflow = ''
      document.documentElement.style.overflow = ''
    } else {
      window.removeEventListener('scroll', handleMobileScroll)
      document.body.style.overflow = 'hidden'
      document.documentElement.style.overflow = 'hidden'
    }
  }

  nextTick(() => {
    if (isMobile.value && replyCommentId.value) {
      const inputEl = document.querySelector('.mobile-comment-input-fixed .comment-input-field') as HTMLInputElement
      inputEl?.focus()
    }
  })
}

// 统一处理 Body 滚动锁定
const setBodyScroll = (lock: boolean) => {
  // PC 端始终保持全局锁定，除非是模态框控制
  if (!isMobile.value) {
    document.body.style.overflow = 'hidden'
    document.documentElement.style.overflow = 'hidden'
    return
  }

  // 移动端根据传入参数锁定
  if (lock) {
    document.body.style.overflow = 'hidden'
  } else {
    document.body.style.overflow = ''
  }
}

const openReportModal = () => {
  if (reportModalRef.value) {
    // 设置举报目标类型和ID
    reportTargetType.value = '2'  // 帖子类型
    reportTargetId.value = post.value.id.toString()

    // 传递参数并打开模态框
    reportModalRef.value.openModal(reportTargetType.value, reportTargetId.value)
  }
  // 点击后关闭弹框
  showMoreModal.value = false
  setBodyScroll(false)
}

// 帖子内容自动滚动相关变量
const isPostAutoScrolling = ref(false)
let autoScrollInterval: number | null = null
const autoScrollSpeed = ref(3)
const autoScrollPauseOnComment = ref(true)

// 自动滚动指示器相关变量
const showAutoScrollIndicator = ref(false)

// 切换帖子内容自动滚动
const togglePostAutoScroll = () => {
  if (isPostAutoScrolling.value) {
    // 停止自动滚动
    stopPostAutoScroll()
  } else {
    // 重置滚动位置到顶部
    resetScrollPosition()
    // 开始自动滚动
    startPostAutoScroll()
  }
  // 点击后关闭弹框
  showMoreModal.value = false
  setBodyScroll(false)
}

// 重置滚动位置到顶部
const resetScrollPosition = () => {
  const isMobile = window.innerWidth <= 768

  if (isMobile) {
    // 移动端重置页面滚动位置
    window.scrollTo({
      top: 0,
      behavior: 'instant'
    })
  } else if (contentScrollWrapper.value) {
    // PC端重置内容容器滚动位置
    contentScrollWrapper.value.scrollTop = 0
  }
}

// 开始自动滚动帖子内容
const startPostAutoScroll = () => {
  // 清除已有定时器，避免重复创建
  if (autoScrollInterval) {
    clearInterval(autoScrollInterval)
  }

  isPostAutoScrolling.value = true
  showAutoScrollIndicator.value = true // 显示自动滚动指示器

  if (isMobile.value) {
    autoScrollInterval = setInterval(() => {
      // 检查是否到达评论区
      const mobileCommentsEl = mobileCommentsRef.value
      if (mobileCommentsEl && autoScrollPauseOnComment.value) {
        const rect = mobileCommentsEl.getBoundingClientRect()
        // 如果评论区进入可视区域，暂停滚动
        if (rect.top <= window.innerHeight * 0.8) {
          stopPostAutoScroll()
          message.info('已滚动至评论区，自动滚动已暂停')
          return
        }
      }

      // 计算当前滚动位置
      const currentScrollTop = window.scrollY
      const windowHeight = window.innerHeight
      const documentHeight = Math.max(
        document.body.scrollHeight,
        document.body.offsetHeight,
        document.documentElement.clientHeight,
        document.documentElement.scrollHeight,
        document.documentElement.offsetHeight
      )

      // 放宽停止条件（剩余300px才停止）
      if (currentScrollTop + windowHeight >= documentHeight - 300) {
        stopPostAutoScroll()
        message.info('已滚动至底部，自动滚动已停止')
        return
      }

      // 使用更平滑的滚动
      window.scrollBy({
        top: autoScrollSpeed.value,
        left: 0,
        behavior: 'smooth'
      })
    }, 200) // 增加滚动间隔到200ms，体验更好
  } else {
    // PC端逻辑
    if (contentScrollWrapper.value) {
      contentScrollWrapper.value.classList.add('auto-scrolling')

      autoScrollInterval = setInterval(() => {
        // 检查是否到达容器底部（放宽阈值到100px）
        if (contentScrollWrapper.value) {
          const { scrollTop, clientHeight, scrollHeight } = contentScrollWrapper.value

          if (scrollTop + clientHeight >= scrollHeight - 100) {
            stopPostAutoScroll()
            message.info('已滚动至底部，自动滚动已停止')
            return
          }

          // 平滑滚动
          contentScrollWrapper.value.scrollTop += autoScrollSpeed.value
        }
      }, 200)
    }
  }
}

// 停止自动滚动帖子内容
const stopPostAutoScroll = () => {
  if (autoScrollInterval) {
    clearInterval(autoScrollInterval)
    autoScrollInterval = null
  }
  isPostAutoScrolling.value = false
  showAutoScrollIndicator.value = false // 隐藏自动滚动指示器

  // 移除自动滚动CSS类
  if (contentScrollWrapper.value) {
    contentScrollWrapper.value.classList.remove('auto-scrolling')
  }
}

// 继续滚动（用于评论区暂停后继续）
const resumeAutoScroll = () => {
  if (!isPostAutoScrolling.value) {
    startPostAutoScroll()
  }
}

const shareImage = computed(() => post.value?.coverUrl || '')
const showMoreActions = ref(false)
const showDeleteModal = ref(false)
const showMoreModal = ref(false)

// 控制弹窗出现时的背景滚动
watch(showMoreModal, (newVal) => {
  setBodyScroll(newVal)
});


const loginUserId = computed(() => loginUserStore.loginUser?.id)
const canEdit = computed(() => post.value.userId === loginUserId.value)
const canDelete = computed(() => canEdit.value)

// 新增：处理输入框聚焦
const handleInputFocus = () => {
  isInputFocused.value = true
  // 聚焦时如果有回复状态，保持不变；没有则清空可能的空内容提示
  if (!replyCommentId.value) {
    showEmojiPicker.value = false
  }
}

// 新增：处理输入框失焦
const handleInputBlur = (e: FocusEvent) => {
  // 检查是否点击的是表情选择器中的元素，如果是则不失去焦点
  const relatedTarget = e.relatedTarget as HTMLElement;
  if (relatedTarget && (
    relatedTarget.closest('.emoji-picker-wrapper') ||
    relatedTarget.closest('.emoji-mart')
  )) {
    return;
  }

  isInputFocused.value = false
  showEmojiPicker.value = false // 失焦时隐藏表情包面板

  // 如果输入框为空，取消回复状态
  if (!commentContent.value.trim()) {
    cancelReply()
  }
}

const fetchPostDetail = async () => {
  const id = route.params.id as string
  if (!id) {
    router.push('/forum')
    return
  }
  try {
    const res = await getPostByIdUsingGet({ id })
    if (res.data?.data) {
      post.value = res.data.data
      queryRequest.value.targetId = post.value.id
      // 核心修复：显式重置为第一页，确保数据从头开始加载
      queryRequest.value.current = 1
      isEndOfData.value = false
      await queryComments()
      // 如果移动端内容不足一屏，自动触发补充加载
      if (isMobile.value) {
        await checkAndLoadMoreComments()
      }
      await checkIsFollowed()
    }
  } catch (error: unknown) {
    message.error((error as Error)?.message || '获取帖子详情失败')
    router.push('/forum')
  }
}

const checkIsFollowed = async () => {
  if (!loginUserStore.loginUser?.id || !post.value?.user?.id) return
  try {
    const res = await findIsFollowUsingPost({
      followerId: loginUserStore.loginUser.id,
      followingId: post.value.user.id
    })
    isFollowed.value = res.data?.data || false
  } catch (error) {
    console.error('检查关注状态失败:', error)
  }
}

const handleFollow = async () => {
  if (!loginUserStore.loginUser?.id) {
    message.warning('请先登录')
    return
  }
  followLoading.value = true
  try {
    const res = await addUserFollowsUsingPost({
      followerId: loginUserStore.loginUser.id,
      followingId: post.value.user.id,
      followStatus: isFollowed.value ? 0 : 1
    })
    if (res.data.code === 0) {
      isFollowed.value = !isFollowed.value
      message.success(isFollowed.value ? '关注成功' : '取消关注成功')
    } else {
      message.error('操作失败')
    }
  } catch (error: unknown) {
    message.error((error as Error)?.message || '操作失败，请稍后重试')
  } finally {
    followLoading.value = false
  }
}

const handleLike = async () => {
  try {
    const res = await doLikeUsingPost({
      targetId: post.value.id,
      targetType: 2,
      isLiked: post.value.isLiked !== 1
    })
    if (res.data.code === 0) {
      post.value.isLiked = post.value.isLiked === 1 ? 0 : 1
      post.value.likeCount = (Number(post.value.likeCount || 0) + (post.value.isLiked === 1 ? 1 : -1)).toString()
    }
  } catch (error: unknown) {
    message.error((error as Error)?.message || '点赞失败')
  }
}

const handleShare = async () => {
  if (!loginUserStore.loginUser?.id) {
    message.warning('请先登录')
    return
  }
  if (post.value.isShared === 1) {
    try {
      await doShareUsingPost({
        targetId: post.value.id,
        targetType: 2,
        isShared: false
      })
      post.value.isShared = 0
      post.value.shareCount = String(Number(post.value.shareCount || 0) - 1)
      message.success('取消分享成功')
    } catch (error: unknown) {
      message.error((error as Error)?.message || '取消分享失败')
    }
    return
  }
  try {
    const res = await doShareUsingPost({
      targetId: post.value.id,
      targetType: 2,
      isShared: true
    })
    if (res.data.code === 0) {
      post.value.isShared = 1
      post.value.shareCount = String(Number(post.value.shareCount || 0) + 1)

      // 检查是否有封面图，如果没有则生成基于标题的封面
      let shareImageUrl = post.value.coverUrl || '';
      if (!shareImageUrl && post.value.title) {
        shareImageUrl = await getTextCover(post.value.title, 300, 400);
      }

      // 更新分享模态框的图片URL
      shareModalRef.value?.openModal(shareImageUrl);
    }
  } catch (error: unknown) {
    message.error((error as Error)?.message || '分享失败')
  }
}

const handleShareSuccess = () => {
  post.value.shareCount = String(Number(post.value.shareCount || 0) + 1)
  post.value.isShared = 1
  message.success('分享成功')
}

const queryComments = async () => {
  try {
    if (queryRequest.value.current > 1) commentloading.value = true
    const res = await queryCommentUsingPost(queryRequest.value)
    if (res.data?.data) {
      const newComments = res.data.data.records.map(comment => ({
        ...comment,
        commentId: comment.commentId?.toString(),
        parentCommentId: comment.parentCommentId?.toString(),
      }))
      if (queryRequest.value.current === 1) {
        comments.value = newComments
      } else {
        comments.value = comments.value.concat(newComments)
      }
      isEndOfData.value = newComments.length < queryRequest.value.pageSize
    } else {
      isEndOfData.value = true
    }
  } catch (error: unknown) {
    message.error((error as Error)?.message || '获取评论失败')
  } finally {
    commentloading.value = false
  }
}

const loadMoreComments = async () => {
  // 防止重复加载
  if (commentloading.value || isEndOfData.value) return

  // 设置加载状态
  commentloading.value = true
  queryRequest.value.current += 1

  try {
    await queryComments()
  } catch (error) {
    console.error('加载更多评论失败:', error)
  } finally {
    // 确保在所有情况下都重置加载状态
    commentloading.value = false
  }
}

const handleScroll = throttle(() => {
  // 移动端由handleMobileScroll专门处理，这里只处理PC端
  if (isMobile.value) return

  const container = scrollContainer.value

  // 验证容器是否存在且未在加载中
  if (!container || commentloading.value) return

  const { scrollTop, clientHeight, scrollHeight } = container

  // PC端使用较小的阈值
  const threshold = 50

  // 检查是否接近底部且还有数据可加载
  if ((scrollTop + clientHeight >= scrollHeight - threshold) && !isEndOfData.value) {
    loadMoreComments()
  }
}, 300)

const addComment = async () => {
  // 防抖处理：如果正在提交评论，则直接返回
  if (isAddingComment.value) {
    return
  }

  if (!commentContent.value.trim()) {
    message.warning('评论内容不能为空')
    return
  }
  if (!loginUserStore.loginUser?.id) {
    message.warning('请先登录')
    return
  }

  // 设置防抖标识为 true，防止重复提交
  isAddingComment.value = true

  try {
    const res = await addCommentUsingPost({
      targetId: post.value.id,
      targetType: 2,
      content: commentContent.value.trim(),
      parentCommentId: replyCommentId.value || '0'
    })
    if (res.data.code === 0) {
      commentContent.value = ''
      replyCommentId.value = ''
      replyTargetUserName.value = ''
      queryRequest.value.current = 1
      await queryComments()
      post.value.commentCount = String(Number(post.value.commentCount || 0) + 1)
      message.success('评论成功')
      showEmojiPicker.value = false
      // 滚动到最新评论位置
      nextTick(() => {
        scrollToLatestComment()
      })
    }
  } catch (error: unknown) {
    message.error((error as Error)?.message || '评论失败')
  } finally {
    // 无论成功或失败，都要重置防抖标识
    isAddingComment.value = false
  }
}

const handleReplyClick = (commentId: string) => {
  replyCommentId.value = commentId
  const targetComment = comments.value.find(c => c.commentId === commentId)
  replyTargetUserName.value = targetComment?.commentUser?.userName || ''

  // 截取被回复评论内容的前50个字符作为预览
  let contentPreview = targetComment?.content || ''
  if (contentPreview.length > 50) {
    contentPreview = contentPreview.substring(0, 50) + '...'
  }
  replyTargetContent.value = contentPreview

  nextTick(() => {
    // 优先聚焦移动端输入框，如果不存在则聚焦PC端输入框
    const mobileInput = document.querySelector('.mobile-comment-input-fixed .comment-input-field') as HTMLInputElement
    const pcInput = document.querySelector('.info-section .comment-input-field') as HTMLInputElement

    if (mobileInput) {
      mobileInput.focus()
    } else if (pcInput) {
      pcInput.focus()
    }
  })
}

const cancelReply = () => {
  replyCommentId.value = ''
  replyTargetUserName.value = ''
  replyTargetContent.value = ''
}

// 修改：移除原有根据内容切换的逻辑
const handleInputChange = () => {
  // 仅保留输入内容变化时的基础处理
}

const toggleEmojiPicker = () => {
  showEmojiPicker.value = !showEmojiPicker.value
  if (showEmojiPicker.value) {
    nextTick(() => {
      const inputEl = document.querySelector('.mobile-comment-input-fixed .comment-input-field') as HTMLInputElement ||
        document.querySelector('.info-section .comment-input-field') as HTMLInputElement
      if (inputEl) {
        inputEl.focus()
      }
    })
  }
}

const onEmojiSelect = (emoji: string) => {
  commentContent.value += emoji

  // 选择表情后保持输入框聚焦并设置光标位置
  nextTick(() => {
    const inputEl = document.querySelector('.mobile-comment-input-fixed .comment-input-field') as HTMLInputElement ||
      document.querySelector('.info-section .comment-input-field') as HTMLInputElement
    if (inputEl) {
      inputEl.focus()
      // 将光标移到文本末尾
      inputEl.setSelectionRange(inputEl.value.length, inputEl.value.length)
    }
  })
}

const emojiI18n = {
  search: '搜索表情',
  categories: {
    recent: '最近使用',
    smileys: '表情',
    people: '人物',
    nature: '自然',
    foods: '食物',
    activity: '活动',
    places: '地点',
    objects: '物品',
    symbols: '符号',
    flags: '旗帜'
  }
}

const handleClickOutside = (e: MouseEvent) => {
  const target = e.target as HTMLElement
  // 检查点击的目标是否在表情选择器内部，包括表情按钮、表情面板和表情本身
  if (!target.closest('.emoji-btn') && !target.closest('.emoji-picker-wrapper') && !target.closest('.emoji-mart')) {
    showEmojiPicker.value = false
  }
  if (!target.closest('.action-btn.btn-more-text') && !target.closest('.more-popover')) {
    showMoreActions.value = false
  }
  // 点击空白处取消回复状态，但排除回复按钮的点击
  if (replyCommentId.value && !target.closest('.comment-input-field') && !target.closest('.comment-input-bar') && !target.closest('.action-item')) {
    cancelReply()
  }
}

// 监听滚动事件，检测评论区是否出现
const handlePageScroll = throttle(() => {
  if (!isPostAutoScrolling.value) return

  if (isMobile.value) {
    const mobileCommentsEl = mobileCommentsRef.value
    if (mobileCommentsEl && autoScrollPauseOnComment.value) {
      const rect = mobileCommentsEl.getBoundingClientRect()
      // 当评论区进入80%可视区域时暂停
      if (rect.top <= window.innerHeight * 0.8) {
        stopPostAutoScroll()
        message.info('已到达评论区，自动滚动已暂停')
      }
    }
  }
}, 100)

// 专门针对移动端的滚动处理，移动端是整个页面滚动触底加载
const handleMobileScroll = throttle(() => {
  if (!isMobile.value) return; // 只在移动端执行

  // 移动端使用页面级滚动计算
  const scrollTop = window.pageYOffset || document.documentElement.scrollTop || document.body.scrollTop
  const clientHeight = window.innerHeight
  const scrollHeight = Math.max(
    document.body.scrollHeight,
    document.body.offsetHeight,
    document.documentElement.clientHeight,
    document.documentElement.scrollHeight,
    document.documentElement.offsetHeight
  )

  if (commentloading.value || isEndOfData.value) return

  // 移动端使用更大的阈值，防止滚动事件频繁触发
  const threshold = 150

  // 检查是否接近底部且还有数据可加载
  if ((scrollTop + clientHeight >= scrollHeight - threshold)) {
    loadMoreComments()
  }
}, 300)

// 确保在组件卸载时移除事件监听器
onMounted(() => {
  fetchPostDetail()
  window.addEventListener('click', handleClickOutside)
  window.addEventListener('resize', handleResize)
  window.addEventListener('scroll', handlePageScroll)

  if (isMobile.value) {
    window.addEventListener('scroll', handleMobileScroll);
  } else {
    // PC 端强行锁定全局滚动
    document.body.style.overflow = 'hidden'
    document.documentElement.style.overflow = 'hidden'
  }

  watch(comments, () => {
    nextTick(() => {
      // 只有在 PC 端且存在 scrollContainer 时才需要特殊触发（其实通常不需要，依靠 CSS 即可）
      if (!isMobile.value && scrollContainer.value) {
        // handleScroll() // 移除主动触发，减少干扰
      }
    })
  })

  // 初始化浏览时长记录
  setTimeout(() => {
    if (post.value.id && loginUserStore.loginUser?.id) {
      viewDurationTracker.init(post.value.id, loginUserStore.loginUser.id);
    }
  }, 100) // 延迟执行，确保post数据已加载
})

// 处理路由变化
watch(() => route.params.id, (newId) => {
  if (newId) {
    fetchPostDetail()
  }
})

// 处理 keep-alive 重新激活
onActivated(async () => {
  await fetchPostDetail()
  await checkAndLoadMoreComments()
})

// 检查是否需要加载更多评论（针对移动端内容不足一屏的情况）
const checkAndLoadMoreComments = async () => {
  // 仅在移动端执行
  if (!isMobile.value || isEndOfData.value || commentloading.value) return

  // 等待渲染完成
  await nextTick()
  await new Promise(resolve => setTimeout(resolve, 300))

  const clientHeight = window.innerHeight
  const scrollHeight = document.documentElement.scrollHeight

  // 如果内容高度小于屏幕高度+阈值，则继续加载
  if (scrollHeight <= clientHeight + 300) {
    console.log('[PostDetail] Mobile room detected (' + scrollHeight + ' / ' + clientHeight + '), loading more...')
    if (!commentloading.value && !isEndOfData.value) {
      await loadMoreComments()
      // 等待加载完成后再次尝试填充
      checkAndLoadMoreComments()
    }
  }
}

onUnmounted(() => {
  window.removeEventListener('click', handleClickOutside)
  window.removeEventListener('resize', handleResize)
  window.removeEventListener('scroll', handlePageScroll) // 移除页面滚动监听

  // 移除移动端页面滚动监听器
  if (window.innerWidth <= 768) {
    window.removeEventListener('scroll', handleMobileScroll);
  }

  // 清理自动滚动定时器
  if (autoScrollInterval) {
    clearInterval(autoScrollInterval)
    autoScrollInterval = null
  }

  // 移除自动滚动CSS类
  if (contentScrollWrapper.value) {
    contentScrollWrapper.value.classList.remove('auto-scrolling')
  }

  // 清理浏览时长记录
  viewDurationTracker.cleanup();
})

const scrollToComments = () => {
  if (window.innerWidth <= 768) {
    mobileCommentsRef.value?.scrollIntoView({ behavior: 'smooth' })
  } else {
    commentsRef.value?.scrollIntoView({ behavior: 'smooth' })
  }
}

const scrollToLatestComment = () => {
  // Use nextTick to ensure DOM updates before scrolling
  nextTick(() => {
    // For PC, scroll to the top of the comment section
    if (scrollContainer.value) {
      const commentHeader = scrollContainer.value.parentElement?.querySelector('.comment-header')
      if (commentHeader) {
        const headerTop = (commentHeader as HTMLElement).offsetTop
        scrollContainer.value.scrollTo({
          top: headerTop,
          behavior: 'smooth'
        })
      } else {
        scrollContainer.value.scrollTo({
          top: 0,
          behavior: 'smooth'
        })
      }
    }

    // For mobile, scroll to the top of the comment section
    if (mobileScrollContainer.value) {
      const mobileCommentHeader = mobileScrollContainer.value.parentElement?.querySelector('.comment-header')
      if (mobileCommentHeader) {
        const mobileHeaderTop = (mobileCommentHeader as HTMLElement).offsetTop
        mobileScrollContainer.value.scrollTo({
          top: mobileHeaderTop,
          behavior: 'smooth'
        })
      } else {
        mobileScrollContainer.value.scrollTo({
          top: 0,
          behavior: 'smooth'
        })
      }
    }

    if (isMobile.value) {
      nextTick(() => {
        mobileCommentsRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
      })
    }
  })
}

const previewImage = (src: string) => {
  Image.preview({ src, maskClosable: true })
}

const handleUserClick = (user) => {
  if (!user) return
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

const handleEdit = () => {
  router.push({
    path: `/post/edit/${post.value.id}`,
    query: {
      post: JSON.stringify({
        id: post.value.id,
        title: post.value.title,
        content: post.value.content,
        category: post.value.category,
        coverUrl: post.value.coverUrl,
        tags: post.value.tagList || [],
      })
    }
  })
  showMoreActions.value = false
  showMoreModal.value = false  // 关闭更多弹窗
  setBodyScroll(false)
}

const showDeleteConfirm = () => {
  showMoreActions.value = false
  showMoreModal.value = false  // 关闭更多弹窗
  showDeleteModal.value = true
  setBodyScroll(false)
}

const confirmDelete = async () => {
  try {
    const res = await deletePostUsingPost({ id: post.value.id })
    if (res.data.code === 0) {
      message.success('删除成功')
      router.push('/forum')
    } else {
      message.error('删除失败')
    }
  } catch (error) {
    message.error('删除失败')
  } finally {
    showDeleteModal.value = false
    setBodyScroll(false)
  }
}

const toggleMoreActions = (e: Event) => {
  e.stopPropagation();
  showMoreActions.value = !showMoreActions.value
}


// 处理收藏操作
const doFavorite = async () => {
  if (!loginUserStore.loginUser?.id) {
    message.warning('请先登录')
    return
  }

  if (post.value.isFavorited === 1) {
    // 取消收藏
    try {
      const res = await cancelFavoriteUsingPost({
        userId: loginUserStore.loginUser.id,
        targetId: post.value.id,
        targetType: 2
      })
      if (res.data.code === 0) {
        post.value.isFavorited = 0
        post.value.favoriteCount = String(Number(post.value.favoriteCount || 0) - 1)
        message.success('已取消收藏')
      } else {
        message.error('取消收藏失败：' + res.data.message)
      }
    } catch (error) {
      message.error('取消收藏失败')
    }
  } else {
    // 添加收藏
    try {
      const res = await addFavoriteRecordUsingPost({
        userId: loginUserStore.loginUser.id,
        targetId: post.value.id,
        targetType: 2,
        targetUserId: post.value.user.id,
        isFavorite: true
      })
      if (res.data.code === 0) {
        post.value.isFavorited = 1
        post.value.favoriteCount = String(Number(post.value.favoriteCount || 0) + 1)
        message.success('收藏成功')
      } else {
        message.error('收藏失败：' + res.data.message)
      }
    } catch (error) {
      message.error('收藏失败')
    }
  }
}
</script>

<style lang="scss" scoped>
.post-detail-page {
  position: absolute;
  inset: 0;
  overflow: hidden;
  transition: var(--theme-transition);
  color: var(--text-primary);
  background: var(--background);
  --header-height: 0px; // Reset as we use absolute positioning in the layout content
  --footer-height: 0px;
  --content-height: 100%;

  @media screen and (max-width: 768px) {
    position: relative;
    inset: auto;
    height: auto;
    overflow: visible;
    --header-height: 0px;
    --footer-height: 50px;
    --content-height: auto;
  }
}

/* 自动滚动指示器样式 */
.auto-scroll-indicator {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 9999;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #ff4d4f;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.3);
  animation: indicator-pulse 1.5s infinite;
  transition: all 0.3s ease;
  border: 2px solid var(--card-background, white);
}

.auto-scroll-indicator:hover {
  transform: scale(1.1);
  background: #f5222d;
}

@keyframes indicator-pulse {
  0% { box-shadow: 0 0 0 0 rgba(255, 87, 87, 0.7); }
  70% { box-shadow: 0 0 0 10px rgba(255, 87, 87, 0); }
  100% { box-shadow: 0 0 0 0 rgba(255, 87, 87, 0); }
}

/* 分类和标签样式 */
.post-category-tags {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.category-tag {
  background: rgba(0, 0, 0, 0.05);
  color: #666;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
  display: inline-block;
  border: 1px solid rgba(0, 0, 0, 0.1);
}

.tag-item {
  background: rgba(0, 0, 0, 0.03);
  color: #666;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
  display: inline-block;
  border: 1px solid rgba(0, 0, 0, 0.1);
}

.mobile-comment-input-fixed {
  display: none;
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 999;
  background: var(--card-background);
  border-top: 1px solid var(--border-color);

  padding: 3.6px;
  @media (max-width: 768px) {
    display: block;
  }
  .comment-input-bar {
    border-radius: 0;
    padding: 8px 0;
    border-top: none;
  }
}

.mobile-emoji-picker {
  bottom: 70px !important;
  left: 0 !important;
  right: 0 !important;
  width: 100%;
  top: auto !important;
}

.color-blur-layer {
  position: absolute;
  inset: 0;
  opacity: 0.4;
  filter: blur(100px);
  transition: opacity 1s ease;
}

.content-layer {
  position: relative;
  z-index: 2;
  overflow: hidden;
  box-sizing: border-box;
  height: 100%;
  @media (max-width: 768px) {
    height: auto;
    overflow: visible;
  }
}

.pc-container {
  max-width: 1400px;
  margin: 0 auto;
  display: flex;
  gap: 0;
  height: 100%;
  overflow: hidden;
  transition: all 0.3s ease;

  &.comments-hidden {
    .preview-section {
      max-width: 1000px;
      margin: 0 auto;
    }
  }

  @media (max-width: 768px) {
    display: block;
    margin: 0 auto;
    padding: 0;
    height: auto;
    overflow: visible;
  }
}

.pc-floating-actions {
  order: 3;
  width: 60px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding-top: 20px;
  margin-left: 24px;
  margin-right: 20px;

  .action-item {
    width: 48px;
    height: 48px;
    border-radius: 50%;
    background: var(--card-background);
    box-shadow: 0 2px 10px var(--shadow-color);
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    color: var(--text-secondary);
    cursor: pointer;
    transition: all 0.3s ease;

    i {
      font-size: 20px;
    }

    .count {
      font-size: 11px;
      margin-top: 2px;
      font-weight: 600;
      line-height: 1;
    }

    &:hover {
      color: var(--link-color);
      transform: translateY(-2px);
      box-shadow: 0 4px 12px var(--shadow-color);
    }

    &.active {
      color: var(--link-color);
      i.fa-heart {
        color: var(--like-button-active-color);
      }
      i.fa-star {
        color: #FFD700;
      }
    }
  }

  .more-action-item {
    margin-top: 12px;
  }
}

.preview-section {
  flex: 1;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 20px 0;
  height: 100%;
  overflow: hidden;
  transition: max-width 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  max-width: calc(100% - 500px);

  @media (max-width: 768px) {
    height: auto;
    overflow: visible;
    max-width: 100%;
  }
}

.post-card {
  width: 100%;
  background: var(--card-background);
  border-radius: 16px;
  box-shadow: 0 2px 12px var(--shadow-color);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  gap: 1px;
  height: 100%;
  max-height: 100%;
  overflow: hidden;
  @media (max-width: 768px) {
    height: auto;
    max-height: none;
    overflow: visible;
  }
}

/* Removed author-info-card to use content-centric layout */

.content-scroll-wrapper {
  flex: 1;
  overflow-y: auto;
  &::-webkit-scrollbar {
    width: 6px;
  }
  &::-webkit-scrollbar-track {
    background: var(--hover-background);
  }
  &::-webkit-scrollbar-thumb {
    background: var(--border-color);
    border-radius: 3px;
  }
}

.info-card {
  background: var(--post-content-background);
  padding: 0;
  border-radius: 0;
  .card-header {
    background: var(--author-card-background);
    padding: 24px 24px 16px 24px;
    border-bottom: 1px solid var(--border-color);

    .post-title {
      margin: 0 0 16px 0;
      font-size: 26px;
      font-weight: 600;
      line-height: 1.4;
      color: var(--post-title-color);
      letter-spacing: 0.5px;
    }

    .post-meta-info {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 12px;
      font-size: 14px;
      color: var(--text-secondary);

      .meta-author {
        color: var(--link-color);
        cursor: pointer;
        font-weight: 500;
        transition: opacity 0.2s;
        &:hover {
          opacity: 0.8;
        }
      }
      .meta-time {
        color: var(--text-secondary);
        opacity: 0.8;
      }

      /* TAGS CONTAINER FOR OVERFLOW FIX */
      .tags-wrapper {
        display: flex;
        flex-wrap: nowrap;
        gap: 8px;
        overflow-x: auto;
        -ms-overflow-style: none; /* IE and Edge */
        scrollbar-width: none; /* Firefox */
        align-items: center;
        max-width: 100%;
        &::-webkit-scrollbar {
          display: none;
        }
      }

      .meta-category, .meta-tag {
        color: var(--text-secondary);
        background: rgba(0, 0, 0, 0.04);
        padding: 4px 10px;
        border-radius: 12px;
        font-size: 13px;
        white-space: nowrap;
        flex-shrink: 0;
      }

      .meta-follow {
        margin-left: auto;
        color: var(--link-color);
        cursor: pointer;
        text-wrap: nowrap;
        font-size: 13px;
        font-weight: 500;
        transition: all 0.3s;
        padding: 4px 14px;
        border-radius: 14px;
        background: rgba(24, 144, 255, 0.08);
        border: 1px solid rgba(24, 144, 255, 0.2);

        &:hover {
          background: rgba(24, 144, 255, 0.15);
        }
        &.is-followed {
          color: var(--text-secondary);
          background: rgba(0, 0, 0, 0.04);
          border-color: rgba(0, 0, 0, 0.08);
          &:hover {
            background: rgba(0, 0, 0, 0.08);
          }
        }
        &.is-loading {
          opacity: 0.5;
          cursor: not-allowed;
        }
      }

      @media screen and (max-width: 768px) {
        gap: 8px;

        .tags-wrapper {
          flex: 1 1 100%;
          order: 3;
          margin-top: 4px;
        }

        .meta-category, .meta-tag {
          padding: 2px 8px;
          font-size: 12px;
        }

        .meta-follow {
          order: 2;
          padding: 3px 10px;
          font-size: 12px;
        }
      }
    }
  }
}
.centralized-info {
  padding: 4px;
  .description-and-time {
    .info-item.description .value {
      .Html-content {
        p {
          margin-bottom: 1em;
          line-height: 1.6;
          font-size: 16px;
          color: var(--post-text-color);
        }
        ul, ol {
          margin: 1em 0;
          padding-left: 2em;
          color: var(--post-text-color);
        }
        li {
          margin: 0.5em 0;
          color: var(--post-text-color);
        }
        strong {
          font-weight: 600;
          color: var(--post-text-color);
        }
        pre, code {
          background: var(--code-background);
          color: var(--Html-code-text);
        }
        blockquote {
          background: var(--blockquote-background);
          border-left: 4px solid var(--blockquote-border);
          color: var(--post-text-color);
          padding: 10px 15px;
          margin: 1em 0;
        }
        a {
          color: var(--link-color);
          &:hover {
            color: var(--link-hover-color);
          }
        }
        table {
          width: 100%;
          border-collapse: collapse;
          margin: 1em 0;
          border: 1px solid var(--table-border);
          th, td {
            padding: 8px 12px;
            border: 1px solid var(--table-border);
            color: var(--post-text-color);
          }
          th {
            background: var(--table-header-background);
          }
          tr:nth-child(even) {
            background: var(--table-row-even-background);
          }
        }
      }
    }
  }
}

.post-actions-card {
  background: var(--hover-background);
  padding: 12px 16px;
  display: flex;
  gap: 24px;
  position: relative;
  .action-item {
    display: flex;
    align-items: center;
    gap: 4px;
    cursor: pointer;
    color: var(--text-secondary);
    transition: color 0.2s;
    &:hover {
      color: var(--link-color);
    }
    i {
      font-size: 18px;
    }
    .action-count {
      font-size: 14px;
    }
    &.more-action {
      margin-left: auto;
    }
    .is-liked {
      color: var(--like-button-active-color);
      animation: actionAnimation 0.4s cubic-bezier(0.4, 0, 0.2, 1);
    }
  }
  .more-popover {
    position: absolute;
    right: 0;
    top: 100%;
    margin-top: 4px;
    background: var(--card-background);
    border-radius: 8px;
    box-shadow: 0 2px 12px var(--shadow-color);
    padding: 8px;
    z-index: 100;
    width: 120px;
    .popover-btn {
      width: 100%;
      padding: 8px 12px;
      border: none;
      background: transparent;
      text-align: left;
      border-radius: 4px;
      cursor: pointer;
      font-size: 14px;
      display: flex;
      align-items: center;
      gap: 4px;
      color: var(--text-primary);
      &:hover {
        background-color: var(--hover-background);
      }
      &.danger {
        color: var(--like-button-active-color);
        &:hover {
          background-color: var(--comment-item-hover);
        }
      }
    }
  }

  .more-actions-wrapper {
    position: relative;
    display: inline-block;
  }

  .more-actions-wrapper {
    position: relative;
    display: inline-block;
  }
}

/* 更多功能弹框 */
.more-modal {
  position: fixed;
  inset: 0;
  z-index: 100;
}

.modal-mask {
  position: absolute;
  inset: 0;
  background: var(--comment-drawer-backdrop);
}

.modal-content {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: var(--card-background);
  border-radius: 16px;
  max-width: 500px;
  width: 90%;
  min-width: 300px;
  z-index: 101;
  display: flex;
  flex-direction: column;
  box-shadow: 0 8px 32px var(--shadow-color);
  border: 1px solid var(--border-color);
}

.modal-header {
  padding: 20px;
  border-bottom: 1px solid var(--border-color);
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.author-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.author-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}

.author-text {
  display: flex;
  flex-direction: column;
}

.author-name {
  font-size: 16px;
  font-weight: 500;
  color: var(--text-primary);
}

.publish-time {
  font-size: 12px;
  color: var(--text-secondary);
}

.close-btn {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: background 0.2s;
  color: var(--text-primary);
}

.close-btn:hover {
  background: var(--hover-background);
}

.post-info {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.post-details {
  flex: 1;
}

.post-title {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 8px;
  color: var(--text-primary);
}

.post-stats span {
  font-size: 12px;
  color: var(--text-secondary);
  margin-right: 12px;
}

.modal-body {
  padding: 20px;
}

.action-buttons {
  display: flex;
  justify-content: space-around;
  gap: 16px;
}

.modal-body .action-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 12px 8px;
  background: var(--hover-background);
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  flex: 0 0 auto;  /* 不伸缩，按内容自适应 */
  color: var(--text-primary);
  min-width: 60px;  /* 设置最小宽度 */
}

.modal-body .action-btn:hover:not(.disabled) {
  background: var(--comment-item-hover);
  transform: translateY(-2px);
}

.modal-body .action-btn i {
  font-size: 24px;
  margin-bottom: 4px;
}

.modal-body .action-btn span {
  font-size: 12px;
}

/* 滚动状态指示器 */
.scrolling-indicator {
  font-size: 8px;
  color: var(--link-color);
  animation: blink 1s infinite;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

.horizontal-scroll-container {
  overflow-x: auto;
  overflow-y: hidden;
  padding: 10px 0;
  scrollbar-width: thin;
  scrollbar-color: var(--border-color) transparent;
  -ms-overflow-style: none;
  &::-webkit-scrollbar {
    height: 6px;
    background: transparent;
  }
  &::-webkit-scrollbar-thumb {
    background-color: var(--border-color);
    border-radius: 3px;
    &:hover {
      background-color: var(--link-color);
    }
  }
}

/* 防止弹窗出现时背景滚动 */
.more-modal {
  position: fixed;
  inset: 0;
  z-index: 100;
  &.active {
    overflow: hidden;
  }
}

.modal-body .action-buttons {
  display: flex;
  gap: 16px;
  padding: 0 10px;
  min-width: fit-content;
  flex-wrap: nowrap; /* 防止按钮换行 */
}

.info-section {
  width: 500px;
  background: transparent;
  display: flex;
  flex-direction: column;
  height: 100%;
  position: sticky;
  top: 0;
  padding: 20px 12px;
  box-sizing: border-box;
  @media (max-width: 768px) {
    display: none;
  }
}

.info-section-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--card-background);
  border-radius: 16px;
  box-shadow: 0 2px 12px var(--shadow-color);
  overflow: hidden;
}

.mobile-comments-section {
  display: none;
  background: var(--card-background);
  border-radius: 16px 16px 0 0;
  margin: 4px;
  position: relative;
  @media (max-width: 768px) {
    display: block;
  }
}

.mobile-comment-section {
  display: flex;
  flex-direction: column;
}

.mobile-comments-list {
  overflow-y: auto;
  padding-bottom: 10px;
}

.comment-section {
  background: var(--comment-background);
  border-radius: 16px 16px 0 0;
  overflow: hidden;
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.comment-header {
  padding: 16px;
  border-bottom: 1px solid var(--comment-header-border);
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: var(--comment-header-background);
  border-radius: 16px 16px 0 0;
  h3 {
    margin: 0;
    font-size: 16px;
    font-weight: 600;
    color: var(--comment-header-text);
  }
  .close-comment-btn {
    background: none;
    border: none;
    color: var(--text-secondary);
    font-size: 18px;
    cursor: pointer;
    padding: 4px;
    border-radius: 4px;
    transition: all 0.2s ease;
    display: flex;
    align-items: center;
    justify-content: center;
    &:hover {
      background: var(--hover-background);
      color: var(--text-primary);
    }
  }
}

.comments-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: var(--comment-list-background);
  border-bottom: 1px solid var(--border-color);
  &::-webkit-scrollbar {
    width: 6px;
  }
  &::-webkit-scrollbar-track {
    background: var(--hover-background);
  }
  &::-webkit-scrollbar-thumb {
    background: var(--border-color);
    border-radius: 3px;
  }
  .end-message {
    text-align: center;
    padding: 16px;
    color: var(--text-secondary);
    font-size: 14px;
  }
}

.comment-input-bar {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 12px 16px;
  background: var(--comment-input-background);
  border-top: 1px solid var(--comment-input-border);
  border-radius: 0 0 16px 16px;
  .comment-avatar {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    object-fit: cover;
    flex-shrink: 0;
    border: 1px solid var(--comment-avatar-border);
  }
  .input-wrapper {
    flex: 1;
    display: flex;
    align-items: center;
    gap: 4px;
    position: relative;
  }

  .action-btn.btn-more-text {
    position: relative;
  }
  .reply-bar {
    position: absolute;
    top: -36px;
    left: 0;
    right: 0;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 8px 12px;
    background-color: var(--hover-background);
    border-radius: 8px;
    font-size: 14px;
    z-index: 10;
    color: var(--text-primary);
  }
  .comment-input-field {
    flex: 1;
    padding: 8px 16px;
    background: var(--comment-input-background);
    border: 1px solid var(--comment-input-border);
    border-radius: 20px;
    font-size: 14px;
    outline: none;
    transition: background 0.3s ease;
    color: var(--comment-input-text);
    min-height: auto;
    resize: none;
    min-width: 0;
  }
  .comment-input-field::placeholder {
    color: var(--comment-input-placeholder);
  }
  .comment-input-field:focus {
    background: var(--hover-background);
  }
  .emoji-btn {
    width: 32px;
    height: 32px;
    border: none;
    background: var(--comment-input-background);
    border-radius: 50%;
    font-size: 16px;
    cursor: pointer;
    transition: all 0.3s ease;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    color: var(--text-primary);
  }
  .emoji-btn:hover {
    background: var(--hover-background);
  }
  .input-actions {
    display: flex;
    gap: 4px;
    align-items: center;
  }
  .action-btn {
    display: flex;
    align-items: center;
    gap: 4px;
    background: transparent;
    border: none;
    cursor: pointer;
    transition: color 0.3s ease;
    color: var(--interaction-button-color);
    padding: 4px 8px;
  }
  .action-btn.btn-like:hover,
  .action-btn.btn-like.is-liked {
    color: var(--like-button-active-color);
  }
  .action-btn.btn-share:hover,
  .action-btn.btn-share.is-shared {
    color: var(--link-color);
  }
  .action-btn.btn-favorite:hover,
  .action-btn.btn-favorite.is-favorited {
    color: #FFD700;
  }
  .action-btn.btn-more:hover {
    color: var(--link-color);
  }

  .action-btn.btn-more-text {
    background: var(--link-color);
    color: var(--text-other);
    padding: 4px 8px;
    border-radius: 20px;
    font-size: 12px;
    display: flex;
    align-items: center;
    gap: 4px;
    border: none;
    transition: all 0.3s ease;
    &:hover {
      background: var(--link-hover-color);
      color: var(--text-other);
    }
  }
  .action-btn {
    gap: 3.2px;
  }
  .action-btn i {
    font-size: 16px;
  }
  .action-btn span {
    font-size: 12px;
  }
  .send-comment-btn {
    padding: 8px 20px;
    border: none;
    border-radius: 20px;
    background-color: var(--link-color);
    color: var(--text-other);
    font-size: 14px;
    cursor: pointer;
    white-space: nowrap;
    &:disabled {
      background-color: var(--border-color);
      color: var(--text-secondary);
      cursor: not-allowed;
    }
  }
}

.emoji-picker-wrapper {
  position: absolute;
  bottom: 60px;
  right: 0;
  z-index: 10;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 4px 12px var(--emoji-picker-shadow);
  background: var(--emoji-picker-background);
  border: 1px solid var(--emoji-picker-border);
}

.delete-modal {
  position: fixed;
  inset: 0;
  z-index: 1001;
  display: flex;
  align-items: center;
  justify-content: center;
}

.delete-modal-mask {
  position: absolute;
  inset: 0;
  background: var(--comment-drawer-backdrop);
}

.delete-modal-content {
  background: var(--card-background);
  padding: 24px;
  border-radius: 12px;
  width: 90%;
  max-width: 400px;
  text-align: center;
  box-shadow: 0 4px 20px var(--shadow-color);
  position: relative;
  z-index: 1;
  border: 1px solid var(--border-color);
}

.delete-modal-icon {
  font-size: 40px;
  color: var(--like-button-active-color);
  margin-bottom: 16px;
}

.delete-modal-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 8px;
  color: var(--text-primary);
}

.delete-modal-desc {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 24px;
}

.delete-modal-actions {
  display: flex;
  gap: 4px;
  justify-content: center;
}

.delete-modal-btn {
  padding: 8px 24px;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s ease;
}

.delete-modal-btn.cancel {
  background: var(--hover-background);
  color: var(--text-primary);
}

.delete-modal-btn.confirm {
  background: var(--like-button-active-color);
  color: var(--text-other);
}

.delete-modal-btn.cancel:hover {
  background: var(--comment-item-hover);
}

.delete-modal-btn.confirm:hover {
  background: var(--like-button-hover-color);
}

@keyframes actionAnimation {
  0% { transform: scale(1); }
  50% { transform: scale(1.2); }
  100% { transform: scale(1); }
}

@media (max-width: 768px) {
  .info-card .centralized-info .description-and-time .post-images .post-image {
    width: calc(50% - 4px);
    height: 100px;
  }

  .preview-section {
    padding:  4px ;
  }
  .post-detail-page {
    padding-bottom: 70px;
  }
  .comments-list-container {
    max-height: 400px;
  }
  .comment-input-bar {
    padding: 8px 12px;
    flex-wrap: wrap;
  }
  .comment-input-field {
    flex: 1;
    min-width: 120px;
    max-width: calc(100% - 100px);
  }
  .input-wrapper {
    flex: 1;
    min-width: 0;
  }
  .input-actions {
    gap: 4px;
  }
  .action-btn i {
    font-size: 14px;
  }
  .action-btn span {
    font-size: 11px;
  }
  .action-btn {
    padding: 4px 6px;
  }
  .action-btn.btn-more-text {
    padding: 4px 6px;
    font-size: 11px;
  }
  .send-comment-btn {
    padding: 6px 12px;
    font-size: 12px;
  }
  .emoji-picker-wrapper {
    bottom: 50px;
    right: 10px;
  }
  .content-layer {
    padding-top: 0;
    padding-bottom: 0;
  }
  .content-scroll-wrapper {
    max-height: none;
    overflow-y: auto;
  }

  /* 当正在进行自动滚动时，确保可以滚动 */
  .content-scroll-wrapper.auto-scrolling {
    overflow-y: auto !important;
  }
  /* .author-info-card sticky property removed */
}

/* 权限设置弹窗样式 */
.permission-setting-modal {
  position: fixed;
  inset: 0;
  z-index: 9999;
  opacity: 0;
  transition: opacity 0.15s ease;
  pointer-events: none;
}
.permission-setting-modal.show {
  opacity: 1;
  pointer-events: auto;
}
.permission-setting-modal .modal-mask {
  position: absolute;
  inset: 0;
  background: var(--comment-drawer-backdrop);
  opacity: 0;
  transition: opacity 0.15s ease;
}
.permission-setting-modal.show .modal-mask {
  opacity: 1;
}
.permission-setting-modal .modal-content {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -60%) scale(0.95);
  background: var(--card-background);
  border-radius: 16px;
  max-width: 500px;
  width: 90%;
  min-width: 300px;
  z-index: 101;
  display: flex;
  flex-direction: column;
  box-shadow: 0 8px 32px var(--shadow-color);
  border: 1px solid var(--border-color);
  opacity: 0;
  transition: all 0.2s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}
.permission-setting-modal.show .modal-content {
  opacity: 1;
  transform: translate(-50%, -50%) scale(1);
}
.permission-setting-modal .modal-header {
  padding: 20px;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.permission-setting-modal .modal-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}
.permission-setting-modal .modal-body {
  padding: 4px;
}
.permission-setting-modal .close-btn {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.1s ease;
  color: var(--text-primary);
}
.permission-setting-modal .close-btn:hover {
  background: var(--hover-background);
  transform: scale(1.1);
}
</style>
