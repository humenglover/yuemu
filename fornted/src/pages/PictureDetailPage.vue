<template>
  <div class="picture-detail" :class="{ 'is-loaded': mounted }">
    <!-- 欢迎层 -->
    <div class="welcome-layer" v-if="!mounted">
      <div class="welcome-content">
        <div class="emoji-row">ʕ •ᴥ•ʔ</div>
        <div class="emoji-row">(｡♥‿♥｡)</div>
      </div>
    </div>

    <!-- 背景层,只在图片加载完成后显示 -->
    <template v-if="pictureLoaded && picture.id">
      <div :style="{
        backgroundImage: picture.url ? `url(${picture.url})` : 'none',
        backgroundPosition: 'center',
        backgroundSize: 'cover',
        backgroundRepeat: 'no-repeat',
        filter: 'blur(120px) brightness(0.6) saturate(120%)',
        transform: 'scale(1.5)',
        opacity: '0.85',
        position: 'absolute',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        zIndex: 1,
        transition: 'opacity 0.6s ease'
      }"></div>

      <div :style="{
        backgroundImage: picture.url ? `url(${picture.url})` : 'none',
        backgroundPosition: 'center',
        backgroundSize: 'cover',
        backgroundRepeat: 'no-repeat',
        filter: 'blur(60px) brightness(0.8) saturate(110%)',
        transform: 'scale(1.5)',
        opacity: '0.6',
        position: 'absolute',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        zIndex: 2,
        transition: 'opacity 0.8s ease'
      }"></div>

      <div :style="{
        backgroundImage: picture.url ? `url(${picture.url})` : 'none',
        backgroundPosition: 'center',
        backgroundSize: 'cover',
        backgroundRepeat: 'no-repeat',
        filter: 'blur(30px) brightness(1) saturate(100%)',
        transform: 'scale(1.5)',
        opacity: '0.4',
        position: 'absolute',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        zIndex: 3,
        transition: 'opacity 1s ease'
      }"></div>
    </template>

    <!-- 内容层 -->
    <div class="content-layer" :style="{
      position: 'relative',
      zIndex: 5,
      height: '100%',
      opacity: mounted ? '1' : '0',
      transition: 'opacity 0.6s ease'
    }">
      <!-- Font Awesome -->
      <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/5.15.4/css/all.min.css">

      <!-- 删除状态 -->
      <div v-if="isDeleted" class="deleted-view">
        <div class="deleted-content">
          <i class="icon-trash"></i>
          <h2>图片已删除</h2>
          <p>该图片可能已被作者删除或管理员删除</p>
          <button class="btn-primary" @click="router.back()">返回上一页</button>
        </div>
      </div>

      <!-- 主要内容 -->
      <template v-else>
        <div class="layout">
          <!-- 左侧预览区 -->
          <div class="preview-section" :class="{ 'is-loaded': pictureLoaded }">
            <div class="image-wrapper" :class="{ 'is-loaded': pictureLoaded }">
              <img
                v-if="pictureLoaded"
                :src="picture.url"
                :alt="picture.name"
                class="preview-image"
                @click="handleImageClick"
                @load="handleImageLoad"
              />
              <div v-else class="loading-state">
                <svg class="loader" viewBox="0 0 128 128" xmlns="http://www.w3.org/2000/svg">
                  <defs>
                    <linearGradient id="rainbow" x1="0%" y1="0%" x2="100%" y2="100%">
                      <stop offset="0%" stop-color="#FF6B6B">
                        <animate attributeName="stop-color"
                                 values="#FF6B6B; #4FACFE; #43E97B; #F6D365; #FF6B6B"
                                 dur="4s" repeatCount="indefinite"/>
                      </stop>
                      <stop offset="100%" stop-color="#4FACFE">
                        <animate attributeName="stop-color"
                                 values="#4FACFE; #43E97B; #F6D365; #FF6B6B; #4FACFE"
                                 dur="4s" repeatCount="indefinite"/>
                      </stop>
                    </linearGradient>
                    <filter id="glow">
                      <feGaussianBlur stdDeviation="2" result="coloredBlur"/>
                      <feMerge>
                        <feMergeNode in="coloredBlur"/>
                        <feMergeNode in="SourceGraphic"/>
                      </feMerge>
                    </filter>
                  </defs>
                  <g class="loader-group">
                    <path class="loader-camera" d="M86,36H42c-8.284,0-15,6.716-15,15v26c0,8.284,6.716,15,15,15h44c8.284,0,15-6.716,15-15V51 C101,42.716,94.284,36,86,36z M64,84c-9.941,0-18-8.059-18-18s8.059-18,18-18s18,8.059,18,18S73.941,84,64,84z"/>
                    <circle class="loader-lens" cx="64" cy="66" r="12"/>
                    <path class="loader-flash" d="M86,36h-8l-4-8H54l-4,8H27c0,0,15,0.021,15,15"/>
                  </g>
                </svg>
              </div>
            </div>
          </div>

          <!-- 全屏预览组件 -->
          <div
            class="fullscreen-viewer"
            :class="{ 'active': isFullscreen }"
            :style="{
              background: picture.picColor ? `rgba(${hexToRgb(toHexColor(picture.picColor))[0]}, ${hexToRgb(toHexColor(picture.picColor))[1]}, ${hexToRgb(toHexColor(picture.picColor))[2]}, 0.95)` : 'rgba(0, 0, 0, 0.95)'
            }"
            @click="closeFullscreen"
          >
            <div class="image-container">
              <img
                :src="picture.url"
                :alt="picture.name"
                @click.stop
                @touchstart.prevent="handleTouchStartAndDoubleTap"
                @touchmove.prevent="handleImageTouchMove"
                @touchend.prevent="handleImageTouchEnd"
                :style="{
                  transform: `scale(${scale}) translateX(${translateX}px)`,
                  transition: isTransitioning ? 'transform 0.3s ease' : 'none',
                  'touch-action': 'pan-x',
                  'user-select': 'none',
                  '-webkit-user-select': 'none'
                }"
              />
            </div>
            <button class="close-button" @click="closeFullscreen">
              <i class="fas fa-times"></i>
            </button>
          </div>

          <!-- 右侧信息栏 -->
          <div class="info-section"
               ref="infoSection"
               @touchstart="handleTouchStart"
               @touchmove="handleTouchMove"
               @touchend="handleTouchEnd"
               :class="{ 'is-expanded': isInfoExpanded }">
            <div class="info-section-content" ref="infoSectionContent">
              <!-- 作者信息 -->
              <div class="author-info">
                <div class="author-avatar"  style="cursor: pointer;">
                  <img @click="handleUserClick(picture.user)" :src="picture.user?.userAvatar || getDefaultAvatar(picture.user?.userName)" :alt="picture.user?.userName">
                </div>
                <div class="author-details">
                  <h3 class="author-name"  style="cursor: pointer;">{{ picture.user?.userName }}</h3>
                  <button
                    v-if="picture.user?.id !== loginUserStore.loginUser?.id"
                    class="btn-follow"
                    :class="{ 'is-followed': isFollowed }"
                    @click="handleFollow"
                  >
                    {{ isFollowed ? '已关注' : '关注' }}
                  </button>
                </div>
              </div>

              <!-- 聊天室入口 -->
              <div v-if="showChatRoom" class="chat-section">
                <button class="btn-chat" @click="openChatModal">
                  <i class="fas fa-comments"></i>
                  <span class="chat-text">聊天室</span>
                  <div class="online-info">
                    <a-avatar-group
                      :maxCount="3"
                      size="small"
                      class="online-avatars"
                    >
                      <a-tooltip
                        v-for="user in onlineUsers"
                        :key="user.id"
                        :title="user.userName"
                      >
                        <a-avatar
                          :size="24"
                          :src="user.userAvatar || getDefaultAvatar(user.userName)"
                        />
                      </a-tooltip>
                    </a-avatar-group>
                  </div>
                </button>
              </div>

              <!-- 互动按钮 -->
              <div class="interaction-buttons">
                <button
                  class="btn-action like"
                  :class="{ 'is-liked': picture.isLiked === 1 }"
                  @click="doLike"
                >
                  <i class="fas fa-heart"></i>
                  <span>{{ formatNumber(picture.likeCount || 0) }}</span>
                </button>
                <button
                  class="btn-action comment"
                  @click="handleCommentClick"
                >
                  <i class="fas fa-comment"></i>
                  <span>{{ formatNumber(picture.commentCount || 0) }}</span>
                </button>
                <button
                  class="btn-action share"
                  :class="{ 'is-shared': picture.isShared === 1 }"
                  @click="doShare"
                >
                  <i class="fas fa-share-alt"></i>
                  <span>{{ formatNumber(picture.shareCount || 0) }}</span>
                </button>
                <button
                  class="btn-action download"
                  :disabled="picture.isDownload === 0"
                  @click="handleDownload"
                >
                  <i class="fas fa-download"></i>
                </button>
              </div>

              <!-- 图片信息 -->
              <div class="picture-info">
                <div class="info-group">
                  <label>名称</label>
                  <div>{{ picture.name || '未命名' }}</div>
                </div>
                <div class="info-group">
                  <label>简介</label>
                  <div>{{ picture.introduction || '-' }}</div>
                </div>
                <div v-if="canEdit" class="info-group">
                  <label>分类</label>
                  <div>{{ picture.category || '默认' }}</div>
                </div>
                <div v-if="canEdit" class="info-group">
                  <label>标签</label>
                  <div class="tags">
                    <span v-for="tag in picture.tags" :key="tag" class="tag">
                      {{ tag }}
                    </span>
                  </div>
                </div>
                <div class="info-group">
                  <label>尺寸</label>
                  <div class="specs">
                    <span>{{ picture.picWidth || '-' }} × {{ picture.picHeight || '-' }}</span>
                    <span class="divider">|</span>
                    <span>{{ formatSize(picture.picSize) }}</span>
                  </div>
                </div>
                <div class="info-group">
                  <label>发布时间</label>
                  <div>{{ formatTime(picture.createTime) }}</div>
                </div>
                <div class="info-group">
                  <label>主色调</label>
                  <div class="color-preview" v-if="picture.picColor">
                    <div class="color-box" :style="{ backgroundColor: toHexColor(picture.picColor) }"></div>
                    <span>{{ toHexColor(picture.picColor) }}</span>
                  </div>
                </div>
              </div>

              <!-- 管理按钮 -->
              <div v-if="canEdit || canDelete" class="management-buttons">
                <button
                  v-if="canEdit"
                  class="btn-action edit"
                  @click="doEdit"
                >
                  <i class="fas fa-edit"></i>
                  <span>编辑</span>
                </button>
                <button
                  v-if="canDelete"
                  class="btn-action delete"
                  @click="showDeleteConfirm"
                >
                  <i class="fas fa-trash-alt"></i>
                  <span>删除</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </template>

      <!-- 分享弹框 -->
      <div v-if="showShareModal" class="share-modal">
        <div class="share-content" :class="{ 'mobile': isMobile }">
          <div class="share-header">
            <h3>分享图片</h3>
            <button class="btn-close" @click="showShareModal = false">
              <i class="fas fa-times"></i>
            </button>
          </div>
          <div class="share-body">
            <div class="preview-box">
              <img :src="picture.thumbnailUrl || picture.url" :alt="picture.name" />
            </div>
            <div class="share-info">
              <div class="link-box">
                <input ref="linkInput" type="text" readonly :value="shareLink" />
                <button class="btn-copy" @click="copyLink">
                  <i class="fas fa-copy"></i>
                  <span>复制链接</span>
                </button>
              </div>
              <div class="share-options">
                <button class="btn-share-option" @click="shareToWeChat">
                  <i class="fab fa-weixin"></i>
                  <span>微信</span>
                </button>
                <button class="btn-share-option" @click="shareToWeibo">
                  <i class="fab fa-weibo"></i>
                  <span>微博</span>
                </button>
                <button class="btn-share-option" @click="shareToQQ">
                  <i class="fab fa-qq"></i>
                  <span>QQ</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 聊天室弹窗 -->
      <div v-if="showChatModal" class="chat-room-modal">
        <div class="chat-room-content" :class="{ 'mobile': device !== DEVICE_TYPE_ENUM.PC }">
          <div class="chat-room-header">
            <div class="chat-room-title">
              <h3>聊天室</h3>
              <div class="online-info">
                <a-popover
                  placement="bottomRight"
                  trigger="hover"
                  :overlayClassName="'online-users-popover'"
                >
                  <template #content>
                    <div class="online-users-list">
                      <div class="section-title">在线用户 ({{ onlineCount }})</div>
                      <div v-for="user in onlineUsers" :key="user.id" class="online-user-item">
                        <a-avatar :src="user.userAvatar || getDefaultAvatar(user.userName)" size="small" />
                        <span class="online-user-name">{{ user.userName }}</span>
                        <span class="online-status active"></span>
                      </div>
                    </div>
                  </template>
                  <a-avatar-group
                    :maxCount="5"
                    size="small"
                    class="online-avatars"
                  >
                    <a-tooltip
                      v-for="user in onlineUsers"
                      :key="user.id"
                      :title="user.userName"
                    >
                      <a-avatar :src="user.userAvatar || getDefaultAvatar(user.userName)" />
                    </a-tooltip>
                  </a-avatar-group>
                </a-popover>
                <span class="online-count">({{ onlineCount }}人在线)</span>
              </div>
            </div>
            <button class="btn-close" @click="showChatModal = false">
              <i class="fas fa-times"></i>
            </button>
          </div>
          <div class="chat-room-body">
            <PictureChatRoom
              ref="chatRoomRef"
              :pictureId="props.id"
              @message="handleChatMessage"
              class="modal-chat-room"
            />
          </div>
        </div>
      </div>

      <!-- 删除确认弹窗 -->
      <div v-if="deleteConfirmVisible" class="modal delete-modal">
        <div class="modal-content">
          <i class="icon-warning"></i>
          <h3>确认删除该图片？</h3>
          <p>删除后将无法恢复，是否继续？</p>
          <div class="modal-actions">
            <button class="btn-secondary" @click="deleteConfirmVisible = false">取消</button>
            <button class="btn-danger" @click="confirmDelete">确认删除</button>
          </div>
        </div>
      </div>

      <!-- 评论弹框 -->
      <div v-if="visible" class="comment-drawer">
        <div class="drawer-content">
          <div class="drawer-header">
            <h2>评论 ({{ picture.commentCount || 0 }})</h2>
            <button class="close-btn" @click="closeModal">×</button>
          </div>

          <div class="comments-list" ref="scrollContainer" @scroll="handleScroll">
            <div v-if="commentloading" class="loading-state">加载中...</div>
            <template v-else>
              <comment-list
                :comments="comments"
                @reply-clicked="handleReplyClick"
                @update-comments="queryComments"
              />
              <div v-if="isEndOfData" class="end-message">没有更多评论了~</div>
            </template>
          </div>

          <div class="comment-input">
            <div v-if="replyCommentId" class="reply-bar">
              <span>回复评论</span>
              <button @click="cancelReply">取消回复</button>
            </div>
            <div class="input-box">
              <button class="emoji-btn" @click="toggleEmojiPicker">😊</button>
              <textarea
                v-model="commentContent"
                placeholder="写下你的评论..."
                @keydown.enter.prevent="addComment"
              ></textarea>
              <button
                class="send-btn"
                :disabled="!commentContent.trim()"
                @click="addComment"
              >
                发送
              </button>
            </div>
            <div v-if="showEmojiPicker" class="emoji-picker-wrapper">
              <emoji-picker
                class="custom-emoji-picker"
                :i18n="emojiI18n"
                @select="onEmojiSelect"
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick, onUnmounted, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { downloadImage, formatSize, toHexColor } from '@/utils'
import { getDeviceType } from '@/utils/device'
import { DEVICE_TYPE_ENUM } from '@/constants/device'
import { SPACE_PERMISSION_ENUM } from '@/constants/space'
import { prevRoute } from '@/router'
import ShareModal from '@/components/ShareModal.vue'
import PictureChatRoom from '@/components/PictureChatRoom.vue'
import { getDefaultAvatar } from '@/utils/userUtils'
import {
  deletePictureUsingPost,
  getPictureVoByIdUsingGet
} from '@/api/pictureController'
import {
  addUserFollowsUsingPost,
  findIsFollowUsingPost
} from '@/api/userFollowsController'
import { message } from 'ant-design-vue'
import CommentList from '@/components/CommentList.vue'
import { addCommentUsingPost, queryCommentUsingPost } from '@/api/commentsController'
import EmojiPicker from '@/components/EmojiPicker.vue'
import { doLikeUsingPost } from '@/api/likeRecordController.ts'
import { throttle } from 'lodash'
import { doShareUsingPost } from '@/api/shareRecordController'
import { formatTime } from '@/utils/dateUtils.ts'

const route = useRoute() // 获取当前路由实例
// 定义用于存储设备类型的响应式变量
const device = ref<string>('')
// 新增一个响应式变量用于标记图片是否加载完成，初始化为false
const pictureLoaded = ref(false)
const mounted = ref(false)
const isDeleted = computed(() => {
  // 只有当图片数据加载完成且确实被删除时才显示删除状态
  return pictureLoaded.value && (!picture.value || !picture.value.id)
})
const showAnimation = ref(false)

// 添加信息区域展开状态
const isInfoExpanded = ref(false);
const infoSection = ref<HTMLElement | null>(null);
const infoSectionContent = ref<HTMLElement | null>(null);
let touchStartY = 0;
let touchMoveY = 0;
let initialHeight = 0;
let lastScrollTop = 0;

// 处理触摸开始
const handleTouchStart = (e: TouchEvent) => {
  if (!infoSection.value) return;

  touchStartY = e.touches[0].clientY;
  touchMoveY = touchStartY;
  initialHeight = infoSection.value.clientHeight;

  // 记录当前滚动位置
  if (infoSectionContent.value) {
    lastScrollTop = infoSectionContent.value.scrollTop;
  }
};

// 处理触摸移动
const handleTouchMove = (e: TouchEvent) => {
  if (!infoSection.value || !infoSectionContent.value) return;

  const currentY = e.touches[0].clientY;
  const deltaY = touchStartY - currentY;
  touchMoveY = currentY;

  // 获取内容区域的滚动状态
  const { scrollTop, scrollHeight, clientHeight } = infoSectionContent.value;
  const isAtTop = scrollTop <= 0;
  const isAtBottom = Math.abs(scrollHeight - scrollTop - clientHeight) < 1;

  // 只有在以下情况下阻止默认滚动：
  // 1. 内容未展开时的上下滑动
  // 2. 内容已展开，在顶部向下滑动
  // 3. 内容已展开，在底部向上滑动
  if (
    (!isInfoExpanded.value) ||
    (isInfoExpanded.value && isAtTop && deltaY < 0) ||
    (isInfoExpanded.value && isAtBottom && deltaY > 0)
  ) {
    e.preventDefault();

    // 计算新的高度
    let newHeight;
    if (deltaY > 0) { // 上滑展开
      newHeight = Math.min(initialHeight + Math.abs(deltaY), window.innerHeight * 0.8);
    } else { // 下滑收起
      newHeight = Math.max(initialHeight - Math.abs(deltaY), window.innerHeight * 0.35);
    }

    // 更新高度
    infoSection.value.style.transition = 'none';
    infoSection.value.style.maxHeight = `${newHeight}px`;
  }
};

// 处理触摸结束
const handleTouchEnd = () => {
  if (!infoSection.value || !infoSectionContent.value) return;

  const { scrollTop } = infoSectionContent.value;
  const isAtTop = scrollTop <= 0;

  // 如果内容已展开但不在顶部，不处理折叠
  if (isInfoExpanded.value && !isAtTop) {
    return;
  }

  infoSection.value.style.transition = 'max-height 0.3s ease';

  // 计算滑动速度和方向
  const swipeDirection = touchStartY - touchMoveY;
  const swipeThreshold = 50;

  // 根据滑动方向和距离决定是否展开
  if (Math.abs(swipeDirection) > swipeThreshold) {
    isInfoExpanded.value = swipeDirection > 0;
  } else {
    // 如果滑动距离不够，根据当前高度决定状态
    const currentHeight = infoSection.value.clientHeight;
    const threshold = window.innerHeight * 0.5;
    isInfoExpanded.value = currentHeight > threshold;
  }

  // 设置最终高度
  const finalHeight = isInfoExpanded.value ? '80%' : '35%';
  infoSection.value.style.maxHeight = finalHeight;
};

// 移除点击处理函数
// const handleInfoSectionClick = () => { ... }; // 删除这个函数

// 全屏状态
const isFullscreen = ref(false)
const isMobile = ref(false)

// 处理图片点击
const handleImageClick = () => {
  isFullscreen.value = true
}

// 处理图片触摸开始
const handleImageTouchStart = (e: TouchEvent) => {
  e.preventDefault()
  const touches = e.touches

  if (touches.length === 2) {
    // 双指触摸 - 准备缩放
    initialScale = scale.value
    const dx = touches[1].clientX - touches[0].clientX
    const dy = touches[1].clientY - touches[0].clientY
    initialDistance = Math.sqrt(dx * dx + dy * dy)
  } else if (touches.length === 1) {
    // 单指触摸 - 准备移动
    lastTouchX = touches[0].clientX
  }
  isTransitioning.value = false
}

// 处理图片触摸移动
const handleImageTouchMove = (e: TouchEvent) => {
  e.preventDefault()
  const touches = e.touches

  // 获取图片和容器元素
  const imageElement = e.target as HTMLImageElement
  const container = document.querySelector('.image-container') as HTMLElement

  if (!imageElement || !container) return

  if (touches.length === 2) {
    // 双指缩放
    const dx = touches[1].clientX - touches[0].clientX
    const dy = touches[1].clientY - touches[0].clientY
    const distance = Math.sqrt(dx * dx + dy * dy)

    requestAnimationFrame(() => {
      // 计算新的缩放值，使用更平滑的缩放系数
      const scaleFactor = distance / initialDistance
      let newScale = initialScale * (1 + (scaleFactor - 1) * 0.8)

      // 限制缩放范围在 1-3 倍之间，使用缓动函数
      const minScale = 1
      const maxScale = 3
      newScale = minScale + (Math.min(Math.max(newScale - minScale, 0), maxScale - minScale))

      scale.value = newScale

      // 计算容器的宽度（即屏幕宽度）
      const containerWidth = container.clientWidth
      // 限制在屏幕宽度范围内，使用缓动
      const maxTranslateX = containerWidth / 2
      const currentTranslateX = translateX.value
      const targetTranslateX = Math.min(Math.max(currentTranslateX, -maxTranslateX), maxTranslateX)
      translateX.value = currentTranslateX + (targetTranslateX - currentTranslateX) * 0.3
    })
  } else if (touches.length === 1 && scale.value > 1) {
    // 单指平移，使用 requestAnimationFrame 优化
    const currentX = touches[0].clientX
    const deltaX = currentX - lastTouchX

    requestAnimationFrame(() => {
      // 计算容器的宽度（即屏幕宽度）
      const containerWidth = container.clientWidth
      // 计算新的位置，添加平滑过渡
      const newTranslateX = translateX.value + deltaX * 0.8

      // 限制在屏幕宽度范围内
      const maxTranslateX = containerWidth / 2
      translateX.value = Math.min(Math.max(newTranslateX, -maxTranslateX), maxTranslateX)
    })

    // 更新最后的触摸位置
    lastTouchX = currentX
  }
}

// 处理图片触摸结束
const handleImageTouchEnd = () => {
  // 使用 requestAnimationFrame 优化过渡动画
  requestAnimationFrame(() => {
    const container = document.querySelector('.image-container') as HTMLElement

    if (container && scale.value > 1) {
      const containerWidth = container.clientWidth
      const maxTranslateX = containerWidth / 2

      isTransitioning.value = true

      // 使用缓动函数平滑过渡到边界位置
      if (translateX.value < -maxTranslateX) {
        translateX.value = -maxTranslateX
      } else if (translateX.value > maxTranslateX) {
        translateX.value = maxTranslateX
      }

      // 300ms 后关闭过渡动画
      setTimeout(() => {
        isTransitioning.value = false
      }, 300)
    }

    // 如果缩放比例接近 1，平滑过渡到初始状态
    if (scale.value < 1.1) {
      isTransitioning.value = true
      const duration = 300
      const startScale = scale.value
      const startTranslateX = translateX.value
      const startTime = performance.now()

      const animate = (currentTime: number) => {
        const elapsed = currentTime - startTime
        const progress = Math.min(elapsed / duration, 1)
        const easeProgress = 1 - Math.pow(1 - progress, 3)

        scale.value = startScale + (1 - startScale) * easeProgress
        translateX.value = startTranslateX * (1 - easeProgress)

        if (progress < 1) {
          requestAnimationFrame(animate)
        } else {
          isTransitioning.value = false
        }
      }

      requestAnimationFrame(animate)
    }
  })
}

// 添加双击重置功能
let lastTapTime = 0
const handleImageDoubleTap = (e: TouchEvent) => {
  const currentTime = new Date().getTime()
  const tapLength = currentTime - lastTapTime

  if (tapLength < 300 && tapLength > 0) {
    e.preventDefault()
    isTransitioning.value = true

    requestAnimationFrame(() => {
      const targetScale = scale.value > 1 ? 1 : 2
      const duration = 300
      const startScale = scale.value
      const startTranslateX = translateX.value

      // 使用 easeOutQuart 缓动函数
      const animate = (currentTime: number) => {
        const elapsed = currentTime - startTime
        const progress = Math.min(elapsed / duration, 1)
        const easeProgress = 1 - Math.pow(1 - progress, 4)

        scale.value = startScale + (targetScale - startScale) * easeProgress

        // 如果是缩小，重置位置到中心
        if (targetScale === 1) {
          translateX.value = startTranslateX * (1 - easeProgress)
        }

        if (progress < 1) {
          requestAnimationFrame(animate)
        } else {
          isTransitioning.value = false
        }
      }

      const startTime = performance.now()
      requestAnimationFrame(animate)
    })
  }
  lastTapTime = currentTime
}

// 修改关闭全屏函数
const closeFullscreen = () => {
  isFullscreen.value = false
  scale.value = 1
  translateX.value = 0
}

// 页面加载时获取设备类型并获取数据
onMounted(async () => {
  device.value = await getDeviceType()

  // 先显示欢迎动画
  setTimeout(() => {
    mounted.value = true
  }, 300)

  // 获取图片详情
  await fetchPictureDetail()
  await checkIsFollowed()

  // 自动触发聊天室按钮点击，但不显示
  if (loginUserStore.loginUser) {
    nextTick(() => {
      showChatModal.value = false
    })
  }

  isMobile.value = device.value === DEVICE_TYPE_ENUM.MOBILE
})

// 通用权限检查函数
function createPermissionChecker(permission: string) {
  return computed(() => {
    return (picture.value.permissionList ?? []).includes(permission)
  })
}

// 定义权限检查
const canEdit = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_EDIT)
const canDelete = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_DELETE)

//是否显示分享按钮
const showShareButton = computed(() => {
  // 移除之前的限制，让分享按钮始终显示
  return true
})

// 判断是否显示聊天室
const showChatRoom = computed(() => {
  // 只有在以下条件都满足时才显示聊天室：
  // 1. 图片可以分享
  // 2. 图片不是来自个人空间
  return showShareButton.value && !picture.value?.spaceId
})

interface Props {
  id: string | number
}

const props = defineProps<Props>()
const picture = ref<API.PictureVO>({} as API.PictureVO)

const loginUserStore = useLoginUserStore()


// 获取图片详情
const fetchPictureDetail = async () => {
  try {
    const res = await getPictureVoByIdUsingGet({
      id: props.id,
    })
    if (res.data.code === 0 && res.data.data) {
      picture.value = res.data.data
      onlineCount.value = res.data.data.chatCount || 0

      // 预加载图片
      if (res.data.data.url) {
        const img = new Image()
        img.src = res.data.data.url
        img.onload = () => {
          pictureLoaded.value = true
          mounted.value = true
        }
        img.onerror = () => {
          pictureLoaded.value = true
          mounted.value = true
        }
      } else {
        pictureLoaded.value = true
        mounted.value = true
      }
    } else {
      // 如果请求成功但没有数据，说明图片确实被删除了
      pictureLoaded.value = true
      mounted.value = true
    }
  } catch (e: any) {
    // 发生错误时也要设置加载完成状态
    pictureLoaded.value = true
    mounted.value = true
  }
}

const router = useRouter()

// 编辑
const doEdit = () => {
  router.push({
    path: '/add_picture',
    query: {
      id: picture.value.id,
      spaceId: picture.value.spaceId,
    },
  })
}

// 删除相关的状态
const deleteConfirmVisible = ref(false)

// 显示删除确认弹窗
const showDeleteConfirm = () => {
  deleteConfirmVisible.value = true
}

// 确认删除
const confirmDelete = async () => {
  try {
    const res = await deletePictureUsingPost({
      id: picture.value?.id
    })
    if (res.data.code === 0) {
      message.success('删除成功')
      deleteConfirmVisible.value = false
      router.back()
    } else {
      message.error('删除失败：' + res.data.message)
    }
  } catch (error: any) {
    message.error('删除失败：' + error.message)
  }
}

// 修改下载处理函数
const handleDownload = () => {
  if (picture.value.isDownload === 0) {
    message.warning({
      content: '抱歉，该图片暂不支持下载，请尊重作者的设置',
      icon: h('i', { class: 'fas fa-lock', style: 'color: #faad14; margin-right: 8px;' }),
      class: 'custom-message'
    });
    return;
  }

  // 创建一个临时的a标签来触发下载
  const link = document.createElement('a');
  link.href = picture.value.url;
  link.download = picture.value.name || '未命名图片';
  link.target = '_blank';

  // 添加到文档中
  document.body.appendChild(link);

  // 触发点击
  link.click();

  // 移除临时元素
  document.body.removeChild(link);

  message.success({
    content: '开始下载...',
    icon: h('i', { class: 'fas fa-download', style: 'color: #52c41a; margin-right: 8px;' })
  });
}

// ----- 分享操作 ----
const shareModalRef = ref()
const showShareModal = ref(false)
// 分享链接
const shareLink = computed(() => {
  if (!picture.value?.id) return ''
  // 获取当前页面的完整URL
  const baseUrl = window.location.origin
  return `${baseUrl}/picture/${picture.value.id}`
})
// 分享图片
const shareImage = ref('')

// 分享
const doShare = async () => {
  if (!loginUserStore.loginUser?.id) {
    message.warning('请先登录')
    return
  }

  // 如果已经分享过,则执行取消分享
  if (picture.value.isShared === 1) {
    try {
      const requestBody: API.ShareRequest = {
        targetId: picture.value.id,
        targetType: 1, // 1 表示图片类型
        isShared: false
      }
      const res = await doShareUsingPost(requestBody)
      if (res.data.code === 0) {
        picture.value.shareCount = String(Number(picture.value.shareCount || 0) - 1)
        picture.value.isShared = 0
      }
    } catch (error) {
      // console.error('取消分享失败:', error)
    }
    return
  }

  // 未分享过,显示分享模态框并调用分享接口
  try {
    const requestBody: API.ShareRequest = {
      targetId: picture.value.id,
      targetType: 1,
      isShared: true
    }
    const res = await doShareUsingPost(requestBody)
    if (res.data.code === 0) {
      picture.value.shareCount = String(Number(picture.value.shareCount || 0) + 1)
      picture.value.isShared = 1
      // 成功后显示分享模态框
      showShareModal.value = true
    }
  } catch (error) {
    // console.error('分享失败:', error)
  }
}

// 处理分享成功
const handleShareSuccess = async () => {
  try {
    const requestBody: API.ShareRequest = {
      targetId: picture.value.id,
      targetType: 1, // 1 表示图片类型
      isShared: true
    }
    const res = await doShareUsingPost(requestBody)
    if (res.data.code === 0) {
      picture.value.shareCount = String(Number(picture.value.shareCount || 0) + 1)
      picture.value.isShared = 1
    }
  } catch (error) {
    // console.error('分享失败:', error)
  }
}

// 计算属性添加空值检查
const pageTitle = computed(() => {
  return `${picture.value?.name || '加载中'} - 图片详情`
})

const isFollowed = ref(false)
const followLoading = ref(false)

// 检查是否已关注
const checkIsFollowed = async () => {
  if (!loginUserStore.loginUser?.id || !picture.value?.user?.id) {
    return
  }
  try {
    const res = await findIsFollowUsingPost({
      followerId: loginUserStore.loginUser.id,
      followingId: picture.value.user.id
    })
    if (res.data?.data) {
      isFollowed.value = res.data.data
    }
  } catch (error) {
    // console.error('检查关注状态失败:', error)
  }
}

// 处理关注/取消关注
const handleFollow = async () => {
  if (!loginUserStore.loginUser?.id) {
    message.warning('请先登录')
    return
  }

  followLoading.value = true
  try {
    const res = await addUserFollowsUsingPost({
      followerId: loginUserStore.loginUser.id,
      followingId: picture.value.user.id,
      followStatus: isFollowed.value ? 0 : 1
    })

    if (res.data?.code === 0) {
      isFollowed.value = !isFollowed.value
    } else {
      // message.error('操作失败')
    }
  } catch (error) {
    // console.error('关注操作失败:', error)
    // message.error('操作失败，请稍后重试')
  } finally {
    followLoading.value = false
  }
}

// 修改聊天相关的状态
const showChatModal = ref(false)
const onlineUsers = ref<any[]>([])
const onlineCount = ref(picture.value?.chatCount || 0)  // 使用 chatCount 作为初始值
const chatRoomRef = ref()

// 处理聊天消息
const handleChatMessage = (msg: any) => {
  if (msg.type === 'onlineUsers') {
    onlineCount.value = msg.onlineCount
    onlineUsers.value = msg.onlineUsers
  }
}

// 打开聊天室弹框
const openChatModal = () => {
  if (loginUserStore.loginUser) {
    showChatModal.value = true
  } else {
    message.warning('请先登录')
  }
}

// 处理用户点击
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

// 在页面卸载时断开连接
onUnmounted(() => {
  if (chatRoomRef.value) {
    chatRoomRef.value.disconnect()
  }
  document.body.style.overflow = ''
})

// 格式化数字为k,w
const formatNumber = (num: number) => {
  if (!num) return '0'
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + 'w'
  }
  if (num >= 1000) {
    return (num / 1000).toFixed(1) + 'k'
  }
  return num.toString()
}

// 添加背景色计算
const backgroundGradient = computed(() => {
  if (!picture.value?.picColor) {
    return {
      start: '#1a1a1a',
      end: '#0a0a0a'
    }
  }

  try {
    const color = toHexColor(picture.value.picColor)
    // 调整渐变色的亮度范围
    const lightenColor = adjustColor(color, 10)  // 减小变亮程度
    const darkenColor = adjustColor(color, -30)  // 增加变暗程度

    return {
      start: lightenColor,
      end: darkenColor
    }
  } catch (error) {
    console.error('Error generating background gradient:', error)
    return {
      start: '#1a1a1a',
      end: '#0a0a0a'
    }
  }
})

// 优化颜色调整函数
function adjustColor(hex: string, percent: number) {
  // 移除#号并确保是6位颜色值
  hex = hex.replace('#', '').padStart(6, '0')

  // 转换为RGB
  const r = parseInt(hex.substring(0, 2), 16)
  const g = parseInt(hex.substring(2, 4), 16)
  const b = parseInt(hex.substring(4, 6), 16)

  // 调整亮度
  const adjustValue = (value: number) => {
    const adjusted = value + (value * (percent / 100))
    return Math.min(255, Math.max(0, Math.round(adjusted)))
  }

  // 转回HEX
  const rr = adjustValue(r).toString(16).padStart(2, '0')
  const gg = adjustValue(g).toString(16).padStart(2, '0')
  const bb = adjustValue(b).toString(16).padStart(2, '0')

  return `#${rr}${gg}${bb}`
}

// 分享功能相关
const linkInput = ref<HTMLInputElement | null>(null)

const copyLink = () => {
  if (linkInput.value) {
    linkInput.value.select()
    document.execCommand('copy')
    message.success('链接已复制')
  }
}

const shareToWeChat = () => {
  // 实现微信分享逻辑
  message.info('请使用微信扫描二维码分享')
}

const shareToWeibo = () => {
  const url = encodeURIComponent(shareLink.value)
  const title = encodeURIComponent(`分享图片：${picture.value.name}`)
  window.open(`http://service.weibo.com/share/share.php?url=${url}&title=${title}`)
}

const shareToQQ = () => {
  const url = encodeURIComponent(shareLink.value)
  const title = encodeURIComponent(`分享图片：${picture.value.name}`)
  window.open(`http://connect.qq.com/widget/shareqq/index.html?url=${url}&title=${title}`)
}

// 处理图片加载完成
const handleImageLoad = () => {
  pictureLoaded.value = true
}

// 辅助函数：将十六进制颜色转换为RGB
function hexToRgb(hex: string) {
  // 移除#号并确保是6位颜色值
  hex = hex.replace('#', '').padStart(6, '0')

  // 转换为RGB
  const r = parseInt(hex.substring(0, 2), 16)
  const g = parseInt(hex.substring(2, 4), 16)
  const b = parseInt(hex.substring(4, 6), 16)

  return [r, g, b]
}

const backgroundStyle = computed(() => ({
  backgroundImage: picture.value?.url ? `url(${picture.value.url})` : 'none',
  backgroundPosition: 'center',
  backgroundSize: 'cover',
  backgroundRepeat: 'no-repeat',
  // 添加透明模糊效果
  backdropFilter: 'blur(5px) brightness(0.1)', // 模糊半径 5px，亮度降低到 80%
  webkitBackdropFilter: 'blur(5px) brightness(0.8)' // 兼容 Safari 浏览器
}))

// 添加评论相关状态
const visible = ref(false)
const comments = ref<API.Comment[]>([])
const commentContent = ref('')
const replyCommentId = ref('')
const commentloading = ref(false)
const showEmojiPicker = ref(false)
const isEndOfData = ref(false)

// 查询评论请求对象
const queryRequest = reactive<API.CommentsQueryRequest>({
  targetId: props.id,
  targetType: 1, // 1 表示图片类型
  current: 1,
  pageSize: 15,
})

// 查询评论
const queryComments = async () => {
  try {
    commentloading.value = true
    queryRequest.targetId = props.id // 确保每次查询都使用最新的图片ID

    const res = await queryCommentUsingPost(queryRequest)
    if (res.data.data != null) {
      const newComments = res.data.data.records.map(comment => ({
        ...comment,
        commentId: comment.commentId?.toString(),
        parentCommentId: comment.parentCommentId?.toString(),
      }))

      if (queryRequest.current === 1) {
        comments.value = newComments
      } else {
        // 使用数组扩展运算符添加新评论，保持响应式
        comments.value = [...comments.value, ...newComments]
      }

      isEndOfData.value = newComments.length < queryRequest.pageSize
    } else {
      if (queryRequest.current === 1) {
        comments.value = []
      }
      isEndOfData.value = true
    }
  } catch (error) {
    // console.error('查询评论异常', error)
  } finally {
    commentloading.value = false
  }
}

// 添加评论
const addComment = async () => {
  if (!commentContent.value.trim()) {
    message.warning('评论内容不能为空')
    return
  }

  try {
    const requestBody: API.CommentsAddRequest = {
      targetId: picture.value.id,
      targetType: 1, // 1 表示图片类型
      content: commentContent.value.trim(),
      parentCommentId: replyCommentId.value || '0'
    }

    const res = await addCommentUsingPost(requestBody)
    if (res.data.code === 0) {
      // message.success('评论成功')
      commentContent.value = ''
      replyCommentId.value = ''
      // 刷新评论列表
      queryRequest.current = 1
      await queryComments()
      // 更新评论数
      picture.value.commentCount = String(Number(picture.value.commentCount || 0) + 1)
    }
  } catch (error) {
    // console.error('评论失败:', error)
    // message.error('评论失败')
  }
}

// 添加表情相关代码
const toggleEmojiPicker = () => {
  showEmojiPicker.value = !showEmojiPicker.value
}

const onEmojiSelect = (emoji: string) => {
  commentContent.value += emoji
  showEmojiPicker.value = false
}

// 点击其他区域关闭表情选择器
const handleClickOutside = (event: MouseEvent) => {
  const target = event.target as HTMLElement
  if (!target.closest('.emoji-picker') && !target.closest('.emoji-btn')) {
    showEmojiPicker.value = false
  }
}

// 添加评论相关方法
const handleReplyClick = (commentId: string) => {
  replyCommentId.value = commentId
  nextTick(() => {
    const inputEl = document.querySelector('.comment-input') as HTMLInputElement
    if (inputEl) {
      inputEl.focus()
      inputEl.scrollIntoView({ behavior: 'smooth', block: 'end' })
    }
  })
}

const cancelReply = () => {
  replyCommentId.value = ''
}

// 添加表情选择器国际化配置
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

// 关闭弹窗
const closeModal = () => {
  visible.value = false
  commentContent.value = ''
  replyCommentId.value = ''
}

// 修改滚动处理函数，添加节流
const handleScroll = throttle(() => {
  const container = scrollContainer.value
  if (!container) return

  const { scrollTop, clientHeight, scrollHeight } = container
  // 提前 100px 触发加载更多
  if (scrollTop + clientHeight >= scrollHeight - 100 && !commentloading.value && !isEndOfData.value) {
    // 保存当前滚动位置
    const oldScrollHeight = scrollHeight
    const oldScrollTop = scrollTop

    // 加载更多评论
    loadMoreComments(oldScrollHeight, oldScrollTop)
  }
}, 200) // 200ms 的节流时间

// 加载更多评论的函数
const loadMoreComments = async (oldScrollHeight: number, oldScrollTop: number) => {
  if (commentloading.value || isEndOfData.value) return

  queryRequest.current++
  await queryComments()

  // 等待 DOM 更新
  await nextTick()

  // 恢复滚动位置
  const container = scrollContainer.value
  if (container) {
    const newScrollHeight = container.scrollHeight
    const heightDiff = newScrollHeight - oldScrollHeight
    container.scrollTop = oldScrollTop + heightDiff
  }
}

// 添加 scrollContainer ref
const scrollContainer = ref<HTMLElement | null>(null)

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
  if (picture.value.id) {
    queryRequest.targetId = picture.value.id
    queryComments()
  }
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})

// 修改评论按钮点击处理
const handleCommentClick = () => {
  visible.value = true
  queryComments()
}

// 点赞功能
const doLike = async () => {
  try {
    const requestBody: API.LikeRequest = {
      targetId: props.id,
      targetType: 1, // 1 表示图片类型
      isLiked: picture.value.isLiked !== 1
    }

    const res = await doLikeUsingPost(requestBody)
    if (res.data.code === 0) {
      // 更新前端数据
      if (requestBody.isLiked) {
        picture.value.likeCount++
        picture.value.isLiked = 1
      } else {
        picture.value.likeCount--
        picture.value.isLiked = 0
      }
    }
  } catch (error) {
    message.error('操作异常')
  }
}

// 打开评论
const openComments = () => {
  visible.value = true
  queryComments()
}

const scale = ref(1)
const translateX = ref(0)
const translateY = ref(0)
const isTransitioning = ref(false)

// 触摸相关变量
let initialScale = 1
let initialDistance = 0
let lastTouchX = 0
let lastTouchY = 0
let startTouchX = 0
let startTouchY = 0

// 合并触摸开始和双击处理函数
const handleTouchStartAndDoubleTap = (e: TouchEvent) => {
  handleImageTouchStart(e)
  handleImageDoubleTap(e)
}
</script>

<style scoped>
.picture-detail {
  height: calc(100vh - 132px);
  margin: -20px;
  position: relative;
  border-radius: 20px;
  opacity: 1;
  transform: translateY(0);
  transition: transform 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  color: #fff;
  overflow: hidden;
  background: linear-gradient(
    135deg,
    rgba(28, 31, 44, 0.95) 0%,
    rgba(45, 55, 72, 0.95) 50%,
    rgba(74, 85, 104, 0.95) 100%
  );
}

/* 添加可爱的颜文字背景 */
.picture-detail::before {
  content: '(｡♥‿♥｡) ♪(´▽｀) (◕‿◕✿) (｡◕‿◕｡) (●´∀｀●) (◠‿◠✿) ʕ•ᴥ•ʔ (◕‿◕✿)';
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 32px;
  padding: 48px;
  font-size: 24px;
  color: rgba(255, 255, 255, 0.05);
  white-space: pre-wrap;
  line-height: 2;
  text-align: center;
  transform: rotate(-5deg);
  pointer-events: none;
  animation: floatEmoji 20s linear infinite;
  z-index: 1;
}

@keyframes floatEmoji {
  0% {
    transform: rotate(-5deg) translateY(0);
  }
  50% {
    transform: rotate(-2deg) translateY(-10px);
  }
  100% {
    transform: rotate(-5deg) translateY(0);
  }
}

/* Add ambient light effect */
.picture-detail::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(
    to bottom,
    rgba(255, 255, 255, 0.02) 0%,
    rgba(255, 255, 255, 0) 100%
  );
  z-index: 2;
  opacity: 0.5;
}

.picture-detail.is-loaded {
  opacity: 1;
  transform: translateY(0);
}

/* Improve background layer transitions */
.background-layer {
  position: absolute;
  inset: 0;
  opacity: 0;
  transition: opacity 0.3s ease;
  z-index: 2;
}

.background-layer.is-loaded {
  opacity: 1;
}

/* Ensure content is visible on dark background */
.content-layer {
  position: relative;
  z-index: 3;
  height: 100%;
  opacity: 0;
  transition: opacity 0.2s ease;
  background: linear-gradient(
    to bottom,
    rgba(0, 0, 0, 0.2) 0%,
    rgba(0, 0, 0, 0.4) 100%
  );
}

.content-layer.is-loaded {
  opacity: 1;
}

/* 预览区域动画 */
.preview-section {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  height: 100%;
  overflow: hidden;
  position: relative;
}

.is-loaded .preview-section {
  opacity: 1;
  transform: scale(1);
}

/* 信息栏动画 */
.info-section {
  position: relative;
  width: 100%;
  background: rgba(0, 0, 0, 0.75);
  backdrop-filter: blur(20px);
  border-radius: 20px 20px 0 0;
  padding: 16px;
  margin-bottom: 0;
  max-height: 35%;
  overflow: hidden;
  transition: max-height 0.3s ease;
  z-index: 10;
}

.is-loaded .info-section {
  opacity: 1;
  transform: translateX(0);
}

.info-section-content {
  height: 100%;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  padding-right: 8px;
}

.info-section.is-expanded {
  max-height: 80%;
}

.info-section-content::-webkit-scrollbar {
  width: 4px;
}

.info-section-content::-webkit-scrollbar-track {
  background: transparent;
}

.info-section-content::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 2px;
}

/* 作者信息动画 */
.author-info {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 32px;
  opacity: 0;
  transform: translateY(20px);
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}

.is-loaded .author-info {
  opacity: 1;
  transform: translateY(0);
}

/* 图片信息动画 */
.picture-info {
  margin-bottom: 32px;
  opacity: 0;
  transform: translateY(20px);
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}

.is-loaded .picture-info {
  opacity: 1;
  transform: translateY(0);
}

/* 操作按钮动画 */
.actions {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin: 16px 0;
  padding: 0;
  opacity: 0;
  transform: translateY(20px);
  transition: all 0.6s cubic-bezier(0.16, 1, 0.3, 1) 0.4s;
}

.is-loaded .actions {
  opacity: 1;
  transform: translateY(0);
}

/* 聊天室入口动画 */
.chat-section {
  margin: -16px 0 24px;
  padding: 0 4px;
  opacity: 0;
  transform: translateY(20px);
  transition: all 0.6s cubic-bezier(0.16, 1, 0.3, 1) 0.5s;
}

.is-loaded .chat-section {
  opacity: 1;
  transform: translateY(0);
}

/* 背景渐变动画 */
.picture-detail::before {
  content: '';
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.2);
  opacity: 0;
  transition: opacity 1s cubic-bezier(0.16, 1, 0.3, 1);
  z-index: -1;
}

.is-loaded .picture-detail::before {
  opacity: 0.95;
}

/* 移动端适配动画 */
@media (max-width: 1024px) {
  .info-section {
    transform: translateY(30px);
    transition: all 0.6s cubic-bezier(0.16, 1, 0.3, 1), max-height 0.3s ease;
  }

  .is-loaded .info-section {
    transform: translateY(0);
  }

  .preview-section {
    transform: scale(0.95) translateY(-20px);
  }

  .is-loaded .preview-section {
    transform: scale(1) translateY(0);
  }
}

/* 添加图片加载动画 */
.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  width: 100%;
  height: 100%;
}

.loader {
  width: 120px;
  height: 120px;
  filter: url(#glow);
}

.loader-group {
  transform-origin: center;
  animation: float 3s ease-in-out infinite;
}

.loader-camera {
  fill: none;
  stroke: url(#rainbow);
  stroke-width: 4;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-dasharray: 300;
  stroke-dashoffset: 300;
  animation: drawCamera 3s ease-in-out infinite;
}

.loader-lens {
  fill: none;
  stroke: url(#rainbow);
  stroke-width: 4;
  opacity: 0;
  animation: showLens 3s ease-in-out infinite;
}

.loader-flash {
  fill: none;
  stroke: url(#rainbow);
  stroke-width: 4;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-dasharray: 50;
  stroke-dashoffset: 50;
  animation: drawFlash 3s ease-in-out infinite;
}

@keyframes drawCamera {
  0% {
    stroke-dashoffset: 300;
  }
  30% {
    stroke-dashoffset: 0;
  }
  100% {
    stroke-dashoffset: 0;
  }
}

@keyframes showLens {
  0% {
    opacity: 0;
    transform: scale(0);
  }
  30% {
    opacity: 0;
    transform: scale(0);
  }
  40% {
    opacity: 1;
    transform: scale(1.2);
  }
  50% {
    transform: scale(1);
  }
  100% {
    opacity: 1;
    transform: scale(1);
  }
}

@keyframes drawFlash {
  0% {
    stroke-dashoffset: 50;
  }
  30% {
    stroke-dashoffset: 50;
  }
  50% {
    stroke-dashoffset: 0;
  }
  100% {
    stroke-dashoffset: 0;
  }
}

@keyframes float {
  0% {
    transform: translateY(0) rotate(0);
  }
  50% {
    transform: translateY(-10px) rotate(2deg);
  }
  100% {
    transform: translateY(0) rotate(0);
  }
}

/* 移除旧的加载动画样式 */
.loader-circle, .loader-orbit {
  display: none;
}

.loading-state p {
  display: none;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translate(-50%, -40%);
  }
  to {
    opacity: 1;
    transform: translate(-50%, -50%);
  }
}

/* 预览区域样式 */
.preview-section {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  height: 100%;
  overflow: hidden;
  position: relative;
}

.preview-image {
  max-width: 100%;
  max-height: calc(100vh - 156px);
  object-fit: contain;
  border-radius: 12px;
  opacity: 0;
  transform: scale(0.98);
  transition: all 0.8s cubic-bezier(0.16, 1, 0.3, 1),
  transform 0.4s cubic-bezier(0.16, 1, 0.3, 1);
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.15);
  cursor: zoom-in;
}

.image-wrapper.is-loaded .preview-image {
  opacity: 1;
  transform: scale(1);
}

/* 移动端适配 */
@media (max-width: 1024px) {
  .preview-section {
    padding: 0;
  }

  .preview-image {
    cursor: zoom-in;
  }
}

.layout {
  display: flex;
  height: 100%;
  position: relative;
  z-index: 1;
}

/* 预览区域样式 */
.preview-section {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  height: 100%;
  overflow: hidden;
}

.image-wrapper {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.preview-image {
  max-width: 100%;
  max-height: calc(100vh - 156px);
  object-fit: contain;
  border-radius: 12px;
  opacity: 0;
  transform: scale(0.98);
  transition: all 0.8s cubic-bezier(0.16, 1, 0.3, 1),
  transform 0.4s cubic-bezier(0.16, 1, 0.3, 1);
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.15);
  cursor: zoom-in;
}

.image-wrapper.is-loaded .preview-image {
  opacity: 1;
  transform: scale(1);
}

/* 信息栏样式 */
.info-section {
  width: 360px;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(20px);
  padding: 24px;
  height: 100%;
  overflow-y: auto;
  color: #fff;
}

.author-info {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 32px;
}

.author-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  overflow: hidden;
}

.author-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.author-details {
  flex: 1;
}

.author-name {
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 8px;
  color: #fff;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
  transition: color 0.3s ease;
}

.author-name:hover {
  color: #ff8e53;
}

.btn-follow {
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
  background: #2563eb;
  color: #fff;
  border: none;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-follow.is-followed {
  background: rgba(255, 255, 255, 0.1);
}

.btn-follow:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3);
}

/* 图片信息样式 */
.picture-info {
  margin-bottom: 32px;
}

.info-group {
  margin-bottom: 24px;
}

.info-group label {
  display: block;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
  margin-bottom: 8px;
  font-weight: 500;
  letter-spacing: 0.5px;
  text-transform: uppercase;
}

.info-group div {
  color: #fff;
  font-weight: 400;
  line-height: 1.6;
  letter-spacing: 0.3px;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag {
  padding: 6px 12px;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 16px;
  font-size: 13px;
  color: #fff;
  font-weight: 500;
  transition: all 0.3s ease;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.tag:hover {
  background: rgba(255, 255, 255, 0.2);
  transform: translateY(-1px);
  border-color: rgba(255, 255, 255, 0.2);
}

.specs {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #fff;
  letter-spacing: 0.3px;
}

.specs span {
  transition: color 0.3s ease;
}

.specs span:hover {
  color: #ff8e53;
}

.divider {
  color: rgba(255, 255, 255, 0.5);
}

.color-preview {
  display: flex;
  align-items: center;
  gap: 12px;
}

.color-box {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: 2px solid rgba(255, 255, 255, 0.1);
}

/* 操作按钮样式 */
.actions {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin: 16px 0;
  padding: 0;
}

.btn-action {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 8px;
  background: transparent;
  border: none;
  color: #fff;
  transition: all 0.3s ease;
  cursor: pointer;
  border-radius: 8px;
  position: relative;
  overflow: hidden;
}

.btn-action i {
  font-size: 20px;
  transition: transform 0.3s ease;
  color: #fff;
  z-index: 1;
}

.btn-action span {
  font-size: 13px;
  color: #fff;
  z-index: 1;
}

.btn-action::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  transition: all 0.3s ease;
  opacity: 0.9;
}

/* 点赞按钮 */
.btn-action.like::before {
  background: linear-gradient(135deg, #ff6b6b, #ff8e53);
}

.btn-action.like.is-liked::before {
  background: linear-gradient(135deg, #ff4b4b, #ff6b6b);
}

/* 评论按钮 */
.btn-action.comment::before {
  background: linear-gradient(135deg, #4facfe, #00f2fe);
}

/* 分享按钮 */
.btn-action.share::before {
  background: linear-gradient(135deg, #60c3d5, #45b1e8);
}

.btn-action.share.is-shared::before {
  background: linear-gradient(135deg, #45b1e8, #3498db);
}

/* 下载按钮 */
.btn-action.download::before {
  background: linear-gradient(135deg, #43e97b, #38f9d7);
}

/* 编辑按钮 */
.btn-action.edit::before {
  background: linear-gradient(135deg, #fa709a, #fee140);
}

/* 删除按钮 */
.btn-action.delete::before {
  background: linear-gradient(135deg, #ff4b4b, #ff6b6b);
}

.btn-action:hover:not(:disabled) {
  transform: translateY(-2px);
}

.btn-action:hover:not(:disabled)::before {
  opacity: 1;
}

.btn-action:hover:not(:disabled) i {
  transform: scale(1.1);
}

.btn-action:active {
  transform: scale(0.95);
}

.btn-action:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-action:disabled::before {
  opacity: 0.5;
}

@keyframes actionAnimation {
  0% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.2);
  }
  100% {
    transform: scale(1);
  }
}

.btn-action.like.is-liked i,
.btn-action.share.is-shared i {
  animation: actionAnimation 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .actions {
    gap: 8px;
    margin: 12px 0;
  }

  .btn-action {
    padding: 6px;
  }

  .btn-action i {
    font-size: 18px;
  }

  .btn-action span {
    font-size: 12px;
  }
}

@media screen and (max-width: 375px) {
  .actions {
    gap: 4px;
    margin: 8px 0;
  }

  .btn-action {
    padding: 4px;
  }

  .btn-action i {
    font-size: 16px;
  }

  .btn-action span {
    font-size: 11px;
  }
}

/* 聊天室入口样式 */
.chat-section {
  margin: -16px 0 24px;
  padding: 0 4px;
}

.btn-chat {
  width: 100%;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 12px;
  color: #fff;
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 14px;
}

.btn-chat:hover {
  background: rgba(255, 255, 255, 0.15);
  border-color: rgba(255, 255, 255, 0.25);
  transform: translateY(-1px);
}

.btn-chat i {
  font-size: 18px;
  color: #ff8e53;
}

.chat-text {
  font-weight: 500;
  flex-shrink: 0;
}

.online-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
}

.online-avatars {
  display: flex;
  align-items: center;
}

.online-count {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
  white-space: nowrap;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .chat-section {
    margin: -8px 0 16px;
  }

  .btn-chat {
    padding: 10px 12px;
    font-size: 13px;
  }

  .btn-chat i {
    font-size: 16px;
  }

  .online-count {
    font-size: 12px;
  }
}

/* 聊天室弹窗样式 */
.chat-room-modal {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.75);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 99999;
}

.chat-room-content {
  background: linear-gradient(135deg, #1a1f35, #2d3748);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 20px;
  width: 800px;
  max-width: 90%;
  height: 600px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
  animation: slideUp 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}

.chat-room-header {
  padding: 16px 24px;
  background: linear-gradient(to right, rgba(49, 130, 206, 0.1), rgba(66, 153, 225, 0.05));
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.chat-room-title {
  display: flex;
  align-items: center;
  gap: 16px;
}

.chat-room-title h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  background: linear-gradient(120deg, #63b3ed, #4299e1);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.chat-room-body {
  flex: 1;
  overflow: hidden;
}

.modal-chat-room {
  height: 100%;
}

.online-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.online-users-list {
  min-width: 200px;
  padding: 12px;
}

.online-user-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.online-user-item:hover {
  background: rgba(0, 0, 0, 0.02);
}

.online-user-name {
  flex: 1;
  font-size: 14px;
}

.online-status.active {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #10b981;
}

.section-title {
  padding: 8px;
  color: #666;
  font-size: 13px;
  font-weight: 500;
  border-left: 3px solid #ff8e53;
  margin: 8px 0;
  background: rgba(255, 142, 83, 0.05);
}

.btn-close {
  background: rgba(255, 255, 255, 0.1);
  border: none;
  font-size: 18px;
  color: #fff;
  cursor: pointer;
  padding: 8px;
  border-radius: 12px;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
}

.btn-close:hover {
  background: rgba(255, 255, 255, 0.2);
  transform: scale(1.1);
}

/* 移动端适配 */
.chat-room-content.mobile {
  width: 100%;
  height: 100%;
  max-width: 100%;
  border-radius: 0;
}

@media (max-width: 768px) {
  .chat-room-header {
    padding: 12px 16px;
  }

  .chat-room-title h3 {
    font-size: 18px;
  }

  .online-info {
    gap: 4px;
  }

  .btn-close {
    width: 32px;
    height: 32px;
    font-size: 16px;
  }
}

/* 安全区域适配 */
@supports (padding: env(safe-area-inset-bottom)) {
  .chat-room-content.mobile {
    padding-bottom: env(safe-area-inset-bottom);
  }
}

/* 分享弹框样式 */
.share-modal {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.75);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 99999;
  margin: 0;
  padding: 0;
}

.share-content {
  background: linear-gradient(135deg, #1a1f35, #2d3748);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 20px;
  width: 480px;
  max-width: 90%;
  max-height: 80vh;
  overflow: hidden;
  color: #fff;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5),
  0 0 30px rgba(66, 153, 225, 0.15);
  animation: slideUp 0.4s cubic-bezier(0.16, 1, 0.3, 1);
  margin: auto;
  position: relative;
}

.share-header {
  padding: 20px 24px;
  background: linear-gradient(to right, rgba(49, 130, 206, 0.1), rgba(66, 153, 225, 0.05));
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.share-header h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  background: linear-gradient(120deg, #63b3ed, #4299e1);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.btn-close {
  background: rgba(255, 255, 255, 0.1);
  border: none;
  font-size: 18px;
  color: #fff;
  cursor: pointer;
  padding: 8px;
  border-radius: 12px;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
}

.btn-close:hover {
  background: rgba(255, 255, 255, 0.2);
  transform: rotate(90deg);
}

.share-body {
  padding: 24px;
}

.preview-box {
  width: 100%;
  height: 240px;
  border-radius: 16px;
  overflow: hidden;
  margin-bottom: 24px;
  background: rgba(0, 0, 0, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.1);
  position: relative;
}

.preview-box img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  transition: transform 0.3s ease;
}

.preview-box:hover img {
  transform: scale(1.02);
}

.share-info {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.link-box {
  display: flex;
  gap: 12px;
  background: rgba(0, 0, 0, 0.2);
  border-radius: 12px;
  padding: 4px;
}

.link-box input {
  flex: 1;
  padding: 12px 16px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  font-size: 14px;
  color: #fff;
  background: rgba(255, 255, 255, 0.05);
  transition: all 0.3s ease;
}

.link-box input:focus {
  outline: none;
  border-color: rgba(66, 153, 225, 0.5);
  background: rgba(255, 255, 255, 0.1);
}

.btn-copy {
  padding: 0 20px;
  border: none;
  border-radius: 8px;
  background: linear-gradient(135deg, #4299e1, #3182ce);
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.3s ease;
}

.btn-copy:hover {
  background: linear-gradient(135deg, #3182ce, #2b6cb0);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(66, 153, 225, 0.3);
}

.share-options {
  display: flex;
  gap: 16px;
  justify-content: center;
  margin-top: 8px;
}

.btn-share-option {
  padding: 16px;
  border: none;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.05);
  color: #fff;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  transition: all 0.3s ease;
  min-width: 100px;
  position: relative;
  overflow: hidden;
}

.btn-share-option::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(66, 153, 225, 0.1), rgba(49, 130, 206, 0.1));
  opacity: 0;
  transition: opacity 0.3s ease;
}

.btn-share-option:hover::before {
  opacity: 1;
}

.btn-share-option i {
  font-size: 28px;
  background: linear-gradient(120deg, #63b3ed, #4299e1);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  transition: transform 0.3s ease;
}

.btn-share-option:hover i {
  transform: scale(1.1);
}

.btn-share-option span {
  font-size: 14px;
  font-weight: 500;
}

/* 动画效果 */
@keyframes fadeIn {
  from {
    opacity: 0;
    backdrop-filter: blur(0);
  }
  to {
    opacity: 1;
    backdrop-filter: blur(8px);
  }
}

@keyframes slideUp {
  from {
    transform: translateY(30px) scale(0.95);
    opacity: 0;
  }
  to {
    transform: translateY(0) scale(1);
    opacity: 1;
  }
}

/* 移动端适配 */
.share-content.mobile {
  width: 100%;
  max-width: 100%;
  height: 100%;
  max-height: 100%;
  border-radius: 0;
  display: flex;
  flex-direction: column;
}

.share-content.mobile .share-body {
  flex: 1;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  padding: 20px;
}

.share-content.mobile .preview-box {
  height: 200px;
  margin-bottom: 20px;
}

.share-content.mobile .share-options {
  position: sticky;
  bottom: 0;
  padding: 20px;
  background: linear-gradient(to bottom, transparent, #1a1f35);
  backdrop-filter: blur(8px);
}

@media (max-width: 640px) {
  .share-content {
    background: linear-gradient(165deg, #1a1f35, #2d3748);
  }

  .btn-share-option {
    padding: 12px;
    min-width: 80px;
    border-radius: 12px;
  }

  .btn-share-option i {
    font-size: 24px;
  }

  .btn-share-option span {
    font-size: 12px;
  }

  .link-box {
    flex-direction: column;
    gap: 8px;
  }

  .btn-copy {
    width: 100%;
    justify-content: center;
    padding: 12px;
  }

  .share-header {
    padding: 16px 20px;
  }

  .share-header h3 {
    font-size: 18px;
  }
}

/* 删除确认弹窗样式 */
.delete-modal {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.75);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 99999;
}

.delete-modal .modal-content {
  background: linear-gradient(135deg, #1a1f35, #2d3748);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 20px;
  padding: 32px;
  width: 400px;
  max-width: 90%;
  text-align: center;
  color: #fff;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
  animation: slideUp 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}

.delete-modal .icon-warning {
  font-size: 48px;
  color: #f56565;
  margin-bottom: 16px;
  display: block;
}

.delete-modal h3 {
  font-size: 24px;
  font-weight: 600;
  margin: 0 0 12px;
}

.delete-modal p {
  color: rgba(255, 255, 255, 0.8);
  margin: 0 0 24px;
  font-size: 16px;
}

.delete-modal .modal-actions {
  display: flex;
  gap: 16px;
  justify-content: center;
}

.delete-modal .btn-secondary {
  padding: 12px 24px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.delete-modal .btn-secondary:hover {
  background: rgba(255, 255, 255, 0.2);
}

.delete-modal .btn-danger {
  padding: 12px 24px;
  border: none;
  border-radius: 8px;
  background: linear-gradient(135deg, #f56565, #e53e3e);
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.delete-modal .btn-danger:hover {
  background: linear-gradient(135deg, #e53e3e, #c53030);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(229, 62, 62, 0.3);
}

@media (max-width: 640px) {
  .delete-modal .modal-content {
    padding: 24px;
  }

  .delete-modal h3 {
    font-size: 20px;
  }

  .delete-modal p {
    font-size: 14px;
  }

  .delete-modal .btn-secondary,
  .delete-modal .btn-danger {
    padding: 10px 20px;
  }
}

/* 移动端样式调整 */
@media (max-width: 1024px) {
  .picture-detail {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    height: 100%;
    padding: 0;
    border-radius: 0;
    margin: 0;
    z-index: 10;
  }

  .layout {
    height: 100%;
    display: flex;
    flex-direction: column;
    position: relative;
    z-index: 1;
  }

  .preview-section {
    flex: 1;
    min-height: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    overflow: hidden;
    padding: 0;
  }

  .info-section {
    position: relative;
    width: 100%;
    background: rgba(0, 0, 0, 0.5);
    backdrop-filter: blur(15px);
    border-radius: 20px 20px 0 0;
    padding: 0;
    margin-bottom: 0;
    max-height: 35%;
    overflow: hidden;
    transition: max-height 0.3s ease, background 0.3s ease;
  }

  .info-section.is-expanded {
    max-height: 80%;
    background: linear-gradient(
      to bottom,
      rgba(0, 0, 0, 0.45) 0%,
      rgba(0, 0, 0, 0.35) 100%
    );
    backdrop-filter: blur(8px);
  }

  .info-section-content {
    height: 100%;
    overflow-y: auto;
    -webkit-overflow-scrolling: touch;
    padding: 16px;
  }

  .info-section-content::-webkit-scrollbar {
    width: 4px;
  }

  .info-section-content::-webkit-scrollbar-track {
    background: transparent;
  }

  .info-section-content::-webkit-scrollbar-thumb {
    background: rgba(255, 255, 255, 0.2);
    border-radius: 2px;
  }

  /* 添加滑动手柄样式 */
  .info-section::before {
    content: '';
    position: absolute;
    top: 8px;
    left: 50%;
    transform: translateX(-50%);
    width: 40px;
    height: 4px;
    background: rgba(255, 255, 255, 0.3);
    border-radius: 2px;
    z-index: 1;
  }
}

/* 平板设备特殊处理 */
@media (min-width: 768px) and (max-width: 1024px) {
  .layout {
    flex-direction: row;
  }

  .preview-section {
    flex: 2;
  }

  .info-section {
    flex: 1;
    max-width: 400px;
    height: 100%;
    max-height: 100%;
    border-radius: 0;
    margin: 0;
  }

  .info-section::before {
    display: none;
  }

  .info-section.is-expanded {
    max-height: 100%;
  }
}

/* 安全区域适配 */
@supports (padding-bottom: env(safe-area-inset-bottom)) {
  @media (max-width: 1024px) {
    .info-section {
      padding-bottom: calc(env(safe-area-inset-bottom) + 12px);
    }
  }
}

/* 深色模式优化 */
@media (prefers-color-scheme: dark) {
  .info-section {
    background: rgba(0, 0, 0, 0.85);
  }

  .btn-action {
    background: rgba(255, 255, 255, 0.15);
  }
}

/* 全屏预览样式 */
.fullscreen-viewer {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(0, 0, 0, 0.95);
  z-index: 99999;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  backdrop-filter: blur(30px) saturate(180%);
}

.fullscreen-viewer.active {
  opacity: 1;
  pointer-events: auto;
}

.fullscreen-viewer .image-container {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  touch-action: none;
}

.fullscreen-viewer img {
  max-width: 90%;
  max-height: 90vh;
  object-fit: contain;
  transform-origin: center center;
  will-change: transform;
  user-select: none;
  -webkit-user-select: none;
  touch-action: pan-x;
  position: relative;
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.fullscreen-viewer img:not(.is-transitioning) {
  transition: none;
}

@media (max-width: 768px) {
  .fullscreen-viewer .image-container {
    padding: 16px;
  }

  .fullscreen-viewer img {
    max-width: 85%;
    max-height: 85vh;
  }
}

.fullscreen-viewer .close-button {
  position: fixed;
  top: 20px;
  right: 20px;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  border: none;
  color: white;
  font-size: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 4;
  transition: all 0.3s ease;
  backdrop-filter: blur(10px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.fullscreen-viewer .close-button:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: scale(1.1);
}

.fullscreen-viewer .close-button:active {
  transform: scale(0.95);
}

@media (max-width: 768px) {
  .fullscreen-viewer .close-button {
    top: auto;
    bottom: 40px;
    right: 50%;
    transform: translateX(50%);
    width: 48px;
    height: 48px;
    font-size: 22px;
    background: rgba(0, 0, 0, 0.5);
    border: 1px solid rgba(255, 255, 255, 0.2);
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.2);
  }

  .fullscreen-viewer .close-button:active {
    transform: translateX(50%) scale(0.95);
  }
}

.background-layer {
  position: absolute;
  inset: 0;
  opacity: 0;
  transition: opacity 0.3s ease;
  z-index: 2;
}

.background-layer.is-loaded {
  opacity: 1;
}

/* 添加额外的玻璃态效果层 */
.overlay-layer {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(
    180deg,
    rgba(255, 255, 255, 0.03) 0%,
    rgba(255, 255, 255, 0.01) 100%
  );
  backdrop-filter: blur(8px) brightness(1.15);
  -webkit-backdrop-filter: blur(8px) brightness(1.15);
  z-index: 2;
  mix-blend-mode: overlay;
  opacity: 0.2;
}

/* 确保内容层在背景之上 */
.content-layer {
  position: relative;
  z-index: 3;
  height: 100%;
}

/* 评论弹框样式 */
.comment-drawer {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.75);
  backdrop-filter: blur(8px);
  z-index: 99999;
  display: flex;
  justify-content: center;
  align-items: center;
  margin: 0;
  padding: 0;
}

.drawer-content {
  position: relative;
  z-index: 10000;
  width: 90%;
  max-width: 600px;
  height: 80vh;
  max-height: 80vh;
  background: linear-gradient(135deg, #1a1f35, #2d3748);
  border-radius: 20px;
  display: flex;
  flex-direction: column;
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
  animation: slideUp 0.4s cubic-bezier(0.16, 1, 0.3, 1);
  color: #fff;
  margin: auto;
  overflow: hidden;
}

/* 评论样式覆盖 */
:deep(.ant-comment-content-author-name > *) {
  color: #fff !important;
}

:deep(.ant-comment-content-author-name) {
  color: #fff !important;
}

:deep(.ant-comment) {
  background: transparent;
  color: #fff !important;
}

:deep(.ant-comment-content-author) {
  margin-bottom: 8px;
}

:deep(.ant-comment-content-author-time) {
  color: rgba(255, 255, 255, 0.6) !important;
}

:deep(.ant-comment-content-detail) {
  color: rgba(255, 255, 255, 0.9) !important;
}

:deep(.ant-comment-actions) {
  margin-top: 8px;
}

:deep(.ant-comment-actions > li > span) {
  color: rgba(255, 255, 255, 0.6) !important;
  transition: color 0.3s ease;
}

:deep(.ant-comment-actions > li > span:hover) {
  color: #ff8e53 !important;
}

:deep(.ant-comment-nested) {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  margin-top: 8px;
  padding: 12px;
}

.drawer-header {
  padding: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.05);
}

.drawer-header h2 {
  font-size: 20px;
  font-weight: 600;
  color: #fff;
  margin: 0;
  background: linear-gradient(120deg, #63b3ed, #4299e1);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.close-btn {
  background: rgba(255, 255, 255, 0.1);
  border: none;
  color: #fff;
  font-size: 24px;
  cursor: pointer;
  border-radius: 50%;
  width: 36px;
  height: 36px;
  display: flex;
  justify-content: center;
  transition: all 0.3s ease;
}

.close-btn:hover {
  background: rgba(255, 255, 255, 0.2);
  transform: rotate(90deg);
}

.comments-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  color: #fff;
  -webkit-overflow-scrolling: touch;
}

.comments-list::-webkit-scrollbar {
  width: 4px;
}

.comments-list::-webkit-scrollbar-track {
  background: transparent;
}

.comments-list::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 2px;
}

.comment-input {
  padding: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(0, 0, 0, 0.2);
  backdrop-filter: blur(10px);
  border-radius: 0 0 20px 20px;
  position: relative;
}

.input-box {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 16px;
  padding: 8px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  transition: all 0.3s ease;
}

.input-box:focus-within {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.2);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.input-box textarea {
  flex: 1;
  background: none;
  border: none;
  color: #fff;
  font-size: 14px;
  line-height: 1.5;
  padding: 8px 4px;
  resize: none;
  min-height: 36px;
  max-height: 120px;
  transition: all 0.3s ease;
}

.input-box textarea:focus {
  outline: none;
}

.input-box textarea::placeholder {
  color: rgba(255, 255, 255, 0.4);
}

.emoji-btn {
  padding: 8px;
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  opacity: 0.8;
  transition: all 0.3s ease;
  border-radius: 50%;
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.emoji-btn:hover {
  opacity: 1;
  background: rgba(255, 255, 255, 0.1);
  transform: scale(1.1);
}

.send-btn {
  padding: 8px 20px;
  border: none;
  border-radius: 12px;
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.send-btn:not(:disabled):hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3);
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  background: rgba(255, 255, 255, 0.1);
}

.reply-bar {
  margin-bottom: 12px;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
}

.reply-bar button {
  background: none;
  border: none;
  color: #ff8e53;
  font-size: 14px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  transition: all 0.3s ease;
}

.reply-bar button:hover {
  background: rgba(255, 142, 83, 0.1);
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .input-box {
    border-radius: 12px;
    padding: 6px;
  }

  .input-box textarea {
    font-size: 14px;
    padding: 6px 2px;
  }

  .emoji-btn {
    padding: 6px;
    font-size: 18px;
  }

  .send-btn {
    padding: 6px 16px;
    font-size: 13px;
    height: 32px;
  }
}

.emoji-picker-wrapper {
  position: absolute;
  bottom: calc(100% + 8px);
  left: 20px;
  z-index: 10;
  animation: fadeInUp 0.3s ease;
}

.custom-emoji-picker {
  width: 320px;
  max-height: 400px;
  border-radius: 12px;
  overflow: hidden;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.emoji-btn {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  padding: 8px;
  opacity: 0.8;
  transition: all 0.3s ease;
  border-radius: 50%;
  position: relative;
  z-index: 1;
}

.emoji-btn:hover {
  opacity: 1;
  background: rgba(255, 255, 255, 0.1);
}

/* 动画效果 */
@keyframes fadeIn {
  from {
    opacity: 0;
    backdrop-filter: blur(0);
  }
  to {
    opacity: 1;
    backdrop-filter: blur(8px);
  }
}

@keyframes slideUp {
  from {
    transform: translateY(30px) scale(0.95);
    opacity: 0;
  }
  to {
    transform: translateY(0) scale(1);
    opacity: 1;
  }
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .drawer-content {
    width: 100%;
    height: 100vh;
    max-height: 100vh;
    border-radius: 0;
    margin: 0;
  }

  .comments-list {
    flex: 1;
    overflow-y: auto;
    -webkit-overflow-scrolling: touch;
  }

  .comment-input {
    border-radius: 0;
    padding: 16px;
    padding-bottom: calc(16px + env(safe-area-inset-bottom));
    background: rgba(0, 0, 0, 0.2);
    backdrop-filter: blur(10px);
  }

  .drawer-header {
    padding: calc(16px + env(safe-area-inset-top)) 16px 16px;
  }
}

.loading-state {
  text-align: center;
  padding: 20px;
  color: rgba(255, 255, 255, 0.6);
}

.end-message {
  text-align: center;
  padding: 20px;
  color: rgba(255, 255, 255, 0.6);
  font-size: 14px;
}

/* 移动端底部操作栏样式 */
.mobile-action-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  justify-content: space-around;
  align-items: center;
  padding: 12px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-top: 1px solid rgba(0, 0, 0, 0.05);
  z-index: 100;

  .action-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
    padding: 8px 16px;
    border-radius: 20px;
    transition: all 0.3s ease;

    &:active {
      transform: scale(0.95);
    }

    span {
      font-size: 12px;
      color: #64748b;
    }

    .action-icon {
      transition: transform 0.3s ease;
    }

    &:hover .action-icon {
      transform: scale(1.1);
    }
  }
}

/* 修改评论抽屉样式以适应移动端底部操作栏 */
.comments-drawer {
  :deep(.ant-drawer-content-wrapper) {
    margin-bottom: var(--mobile-action-bar-height, 60px);
  }
}

/* 修改评论输入框样式 */
.comment-input-wrapper {
  background: white;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  padding: 12px;
  margin-bottom: var(--mobile-action-bar-height, 60px);

  .input-area {
    display: flex;
    align-items: center;
    gap: 8px;
    background: #f8fafc;
    border-radius: 20px;
    padding: 8px 12px;

    .emoji-trigger {
      padding: 4px;
      color: #94a3b8;
      cursor: pointer;
      transition: all 0.3s ease;

      &:hover {
        color: #ff8e53;
      }
    }

    .comment-input {
      flex: 1;
      border: none;
      background: transparent;
      padding: 8px;

      &:focus {
        outline: none;
      }
    }

    .send-button {
      min-width: 60px;
      height: 32px;
      border-radius: 16px;
      background: linear-gradient(135deg, #ff8e53 0%, #ff6b6b 100%);
      border: none;
      color: white;
      font-size: 14px;

      &:disabled {
        opacity: 0.5;
        background: #e2e8f0;
      }
    }
  }
}

/* 添加点赞按钮样式 */
.btn-action.like {
  background: linear-gradient(135deg, #ff6b6b, #ff8e53);
  position: relative;
  overflow: hidden;
}

.btn-action.like.is-liked {
  background: linear-gradient(135deg, #ff4b4b, #ff6b6b);
}

.btn-action.like.is-liked i {
  color: #fff;
  transform: scale(1.1);
}

.btn-action.like:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(255, 107, 107, 0.3);
}

.btn-action.like i {
  color: #fff;
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.btn-action.like:active {
  transform: scale(0.95);
}

/* 添加点击动画效果 */
@keyframes likeAnimation {
  0% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.2);
  }
  100% {
    transform: scale(1);
  }
}

.btn-action.like.is-liked i {
  animation: likeAnimation 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 互动按钮样式 */
.interaction-buttons {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin: 16px 0 24px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

/* 管理按钮样式 */
.management-buttons {
  display: flex;
  gap: 12px;
  margin-top: 24px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.management-buttons .btn-action {
  flex: 1;
  height: 40px;
}

.management-buttons .btn-action.edit::before {
  background: linear-gradient(135deg, #60a5fa, #3b82f6);
}

.management-buttons .btn-action.delete::before {
  background: linear-gradient(135deg, #ef4444, #dc2626);
}

@media (max-width: 768px) {
  .interaction-buttons {
    margin: 12px 0 20px;
    padding: 8px;
    gap: 8px;
  }

  .management-buttons {
    margin-top: 20px;
    padding: 12px;
    gap: 8px;
  }

  .management-buttons .btn-action {
    height: 36px;
  }
}

/* PC 端样式 */
@media (min-width: 1025px) {



  .preview-section {
    flex: 1;
    display: flex;
    align-items: center;

    height: 100%;
    overflow: hidden;
  }

  .info-section {
    max-width: 400px;
    height: 100%;
    max-height: 100% !important;
    background: rgba(0, 0, 0, 0.75);
    backdrop-filter: blur(20px);
    border-radius: 24px;
    overflow: hidden;
    display: flex;
    flex-direction: column;
    border-left: 1px solid rgba(255, 255, 255, 0.1);
  }

}

.deleted-view {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg,
  rgba(28, 31, 44, 0.95) 0%,
  rgba(45, 55, 72, 0.95) 50%,
  rgba(74, 85, 104, 0.95) 100%
  );
  backdrop-filter: blur(10px);
  z-index: 10;
}

.deleted-content {
  text-align: center;
  padding: 40px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
  backdrop-filter: blur(8px);
  max-width: 400px;
  width: 90%;
  animation: fadeInUp 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}

.deleted-content .icon-trash {
  font-size: 48px;
  color: #ef4444;
  margin-bottom: 24px;
  display: block;
  animation: fadeIn 0.6s cubic-bezier(0.16, 1, 0.3, 1);
}

.deleted-content h2 {
  font-size: 24px;
  font-weight: 600;
  color: #fff;
  margin: 0 0 12px;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.deleted-content p {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.8);
  margin: 0 0 32px;
  line-height: 1.6;
}

.deleted-content .btn-primary {
  padding: 12px 32px;
  border: none;
  border-radius: 12px;
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  color: #fff;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.2);
}

.deleted-content .btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(37, 99, 235, 0.3);
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
}

.deleted-content .btn-primary:active {
  transform: translateY(0);
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.2);
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: scale(0.9);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

/* 添加欢迎层样式 */
.welcome-layer {
  position: fixed;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg,
  rgba(28, 31, 44, 0.98) 0%,
  rgba(45, 55, 72, 0.98) 50%,
  rgba(74, 85, 104, 0.98) 100%
  );
  z-index: 4;
  opacity: 1;
  transition: opacity 0.6s ease;
  pointer-events: none;
}

.is-loaded .welcome-layer {
  opacity: 0;
}

.welcome-content {
  text-align: center;
  animation: welcomeFadeIn 0.8s ease-out forwards;
}

.emoji-row {
  font-size: 28px;
  color: #fff;
  margin: 20px;
  opacity: 0;
  transform: translateY(20px);
  animation: emojiAppear 0.5s ease-out forwards;
}

.emoji-row:nth-child(1) { animation-delay: 0.2s; }
.emoji-row:nth-child(2) { animation-delay: 0.4s; }
.emoji-row:nth-child(3) {
  animation-delay: 0.6s;
  font-size: 24px;
  color: rgba(255, 255, 255, 0.9);
}
.emoji-row:nth-child(4) { animation-delay: 0.8s; }

@keyframes welcomeFadeIn {
  from {
    opacity: 0;
    transform: scale(0.35);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

@keyframes emojiAppear {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>

