<template>
  <div class="mobile-picture-detail">

    <!-- 优化的背景层 -->
    <template v-if="pictureLoaded && picture.id">
      <div class="background-effects">
        <div
          class="color-blur-layer"
          :style="{
            background: `linear-gradient(135deg, ${dominantColor}, ${adjustColor(dominantColor, 20)}, #1a1f2e)`,
            opacity: 0.4
          }"
        ></div>
        <div
          class="blurred-image-layer"
          :style="{ backgroundImage: picture.url ? `url(${picture.url})` : 'none' }"
        ></div>
      </div>
    </template>

    <!-- 主要内容 -->
    <template v-if="!isDeleted">
      <!-- 图片展示区 (占满屏幕宽度) -->
      <div class="picture-container">
        <!-- 骨架屏：数据未就绪 -->
        <div v-if="!pictureLoaded" class="mobile-image-skeleton"></div>

        <template v-else>
          <!-- 模糊缩略图占位 -->
          <img
            :class="{ 'hidden-placeholder': imgReady }"
            :src="picture.thumbnailUrl || picture.url"
            class="full-width-image blur-placeholder"
            alt="Loading placeholder"
          />
          <!-- 原图（加载完成后淡入） -->
          <img
            v-if="picture.url"
            :src="picture.url"
            :alt="picture.name"
            class="full-width-image preview-image"
            :class="{ 'img-visible': imgReady }"
            @click="showImagePreview"
            @load="handleImageLoad"
          />
        </template>

        <!-- 顶部导航栏 (浮动在图片上方) -->
        <div class="top-nav">
          <div class="left-nav">
            <button class="back-btn" @click="router.back()">
              <i class="fas fa-chevron-left"></i>
            </button>
            <div class="author-info">
              <img
                :src="picture.user?.userAvatar || getDefaultAvatar(picture.user?.userName)"
                :alt="picture.user?.userName"
                class="author-avatar"
                @click="handleUserClick(picture.user)"
              >
              <div class="author-text">
                <div class="author-name" @click="handleUserClick(picture.user)">
                  {{ picture.user?.userName || '匿名用户' }}
                </div>
              </div>
            </div>
          </div>
          <button
            v-if="picture.user?.id !== loginUserStore.loginUser?.id"
            class="follow-btn"
            :class="{ 'is-followed': isFollowed }"
            @click="handleFollow"
          >
            <i :class="isFollowed ? 'fas fa-check' : 'fas fa-plus'"></i>
            <span>{{ isFollowed ? '已关注' : '关注' }}</span>
          </button>
        </div>

        <div class="right-action-bar">
          <button
            class="action-btn like-btn"
            :class="{ 'is-liked': picture.isLiked === 1, 'permission-disabled': !picture.allowLike && picture.user?.id !== loginUserStore.loginUser?.id }"
            :disabled="!picture.allowLike && picture.user?.id !== loginUserStore.loginUser?.id"
            @click="doLike"
          >
            <i :class="picture.isLiked === 1 ? 'fas fa-heart' : 'far fa-heart'"></i>
            <span class="count">{{ picture.likeCount || 0 }}</span>
          </button>
          <button class="action-btn comment-btn" @click="handleCommentClick">
            <i class="far fa-comment"></i>
            <span class="count">{{ picture.commentCount || 0 }}</span>
          </button>
          <button
            class="action-btn favorite-btn"
            :class="{ 'is-favorited': picture.isFavorited === 1, 'permission-disabled': !picture.allowCollect && picture.user?.id !== loginUserStore.loginUser?.id }"
            :disabled="!picture.allowCollect && picture.user?.id !== loginUserStore.loginUser?.id"
            @click="doFavorite"
          >
            <i :class="picture.isFavorited === 1 ? 'fas fa-star' : 'far fa-star'"></i>
            <span class="count">{{ picture.favoriteCount || 0 }}</span>
          </button>
          <button class="action-btn share-btn" :class="{ 'is-shared': picture.isShared === 1, 'permission-disabled': !picture.allowShare && picture.user?.id !== loginUserStore.loginUser?.id }" :disabled="!picture.allowShare && picture.user?.id !== loginUserStore.loginUser?.id" @click="doShare">
            <i class="fas fa-share-alt"></i>
            <span class="count">{{ picture.shareCount || 0 }}</span>
          </button>
          <button class="action-btn info-btn" @click="showMoreDrawer = true">
            <i class="fas fa-ellipsis-h"></i>
          </button>
        </div>

        <!-- 图片信息浮层 (浮动在图片下方) -->
        <div class="picture-info-float">
          <div class="picture-title">{{ picture.name || '未命名' }}</div>
          <div ref="pictureDescRef" class="picture-desc" :class="{ 'collapsed': !descExpanded }" v-html="formattedIntroduction"></div>
          <button v-if="shouldShowExpandButton" class="toggle-desc-btn" @click="descExpanded = !descExpanded">
            {{ descExpanded ? '收起' : '展开' }}
          </button>
          <div class="picture-stats">
            <span><i class="fas fa-eye mr-1"></i>{{ picture.viewCount || 0 }}次浏览</span>
            <!--            <span><i class="fas fa-map-marker mr-1"></i>广东</span>-->
          </div>
        </div>

      </div>

      <!-- 移动端图片预览 -->
      <van-image-preview
        v-if="isMobile"
        v-model:show="showPreview"
        :images="[picture.url]"
        :closeable="false"
        :show-index="false"
        :overlay-style="{ backgroundColor: 'rgba(0,0,0,0.9)' }"
        @close="closeImagePreview"
      >
      </van-image-preview>

      <!-- 信息弹窗 -->
      <div v-if="showInfoModal" class="info-modal">
        <div class="modal-mask" @click="showInfoModal = false"></div>
        <div class="info-modal-content modal-enter">
          <div class="modal-title">作品信息</div>
          <div class="modal-content">
            <div v-if="picture.tags && picture.tags.length" class="info-item">
              <span class="label">标签：</span>
              <span class="value">
                <span v-for="tag in picture.tags" :key="tag" class="tag-item">#{{ tag }}</span>
              </span>
            </div>
            <div v-if="picture.picColor" class="info-item">
              <span class="label">主色调：</span>
              <div class="color-value">
                <div class="color-dot" :style="{ backgroundColor: toHexColor(picture.picColor) }"></div>
                <span class="color-hex">{{ toHexColor(picture.picColor) }}</span>
              </div>
            </div>
            <div class="info-item">
              <span class="label">尺寸：</span>
              <span class="value">{{ picture.picWidth || '-' }} × {{ picture.picHeight || '-' }}</span>
            </div>
            <div class="info-item">
              <span class="label">大小：</span>
              <span class="value">{{ formatSize(picture.picSize) }}</span>
            </div>
            <div class="info-item">
              <span class="label">发布时间：</span>
              <span class="value">{{ formatTime(picture.createTime) }}</span>
            </div>
          </div>
          <button class="close-modal-btn" @click="showInfoModal = false">关闭</button>
        </div>
      </div>

      <!-- 更多功能抽屉 -->
      <div v-if="showMoreDrawer" class="more-drawer">
        <div class="drawer-mask" @click="showMoreDrawer = false"></div>
        <div class="drawer-content">
          <div class="drawer-header">
            <div class="header-content">
              <div class="author-info">
                <img
                  :src="picture.user?.userAvatar || getDefaultAvatar(picture.user?.userName)"
                  :alt="picture.user?.userName"
                  class="author-avatar"
                >
                <div class="author-text">
                  <div class="author-name">{{ picture.user?.userName || '匿名用户' }}</div>
                  <div class="publish-time">{{ formatTime(picture.createTime) }}</div>
                </div>
              </div>
              <button class="close-btn" @click="showMoreDrawer = false">
                <i class="fas fa-times"></i>
              </button>
            </div>
            <div class="picture-info">
              <img :src="picture.url" :alt="picture.name" class="picture-thumb">
              <div class="picture-details">
                <div class="picture-title">{{ picture.name || '未命名' }}</div>
                <div class="picture-stats">
                  <span><i class="fas fa-eye"></i> {{ picture.viewCount || 0 }}</span>
                  <span><i class="fas fa-heart"></i> {{ picture.likeCount || 0 }}</span>
                  <span><i class="fas fa-comment"></i> {{ picture.commentCount || 0 }}</span>
                </div>
              </div>
            </div>
            <!-- 简化的作品信息 -->
            <div class="picture-details-info">
              <div v-if="picture.category" class="info-item">
                <span class="label">分类：</span>
                <span class="value">{{ picture.category }}</span>
              </div>
              <div v-if="picture.tags && picture.tags.length" class="info-item">
                <span class="label">标签：</span>
                <span class="value">
                  <span v-for="tag in picture.tags" :key="tag" class="tag-item">#{{ tag }}</span>
                </span>
              </div>
              <div v-if="picture.picColor" class="info-item">
                <span class="label">主色调：</span>
                <div class="color-value">
                  <div class="color-dot" :style="{ backgroundColor: toHexColor(picture.picColor) }"></div>
                  <span class="color-hex">{{ toHexColor(picture.picColor) }}</span>
                </div>
              </div>
            </div>
          </div>
          <div class="drawer-body">
            <div class="action-buttons-container">
              <div class="action-buttons">
                <button v-if="picture.user?.id === loginUserStore.loginUser?.id" class="action-btn" @click="doEdit">
                  <i class="fas fa-edit"></i>
                  <span>编辑</span>
                </button>
                <button v-if="picture.user?.id === loginUserStore.loginUser?.id" class="action-btn" @click="showDeleteConfirm">
                  <i class="fas fa-trash-alt"></i>
                  <span>删除</span>
                </button>
                <button v-if="picture.user?.id === loginUserStore.loginUser?.id" class="action-btn" @click="showPermissionSetting = true">
                  <i class="fas fa-lock"></i>
                  <span>权限设置</span>
                </button>
                <button class="action-btn" @click="handleDownload">
                  <i class="fas fa-download"></i>
                  <span>下载</span>
                </button>
                <button v-if="showChatRoom" class="action-btn" @click="openChatModal">
                  <i class="fas fa-comments"></i>
                  <span>聊天室</span>
                </button>
                <button class="action-btn" @click="openReportModal">
                  <i class="fas fa-flag"></i>
                  <span>举报</span>
                </button>

              </div>
            </div>
          </div>
        </div>
      </div>
      <!-- 评论弹窗 -->
      <div v-if="visible" class="comment-modal">
        <div class="modal-mask" @click="closeModal"></div>
        <div class="comment-modal-content">
          <div class="comment-header">
            <div class="comment-count">共{{ picture.commentCount || 0 }}条评论</div>
            <!--            <div class="comment-tabs">-->
            <!--              <button class="tab-btn active">最新</button>-->
            <!--            </div>-->
            <button class="close-comment-btn" @click="closeModal">
              <i class="fas fa-times"></i>
            </button>
          </div>
          <div class="comments-container">
            <div class="comments-list" ref="scrollContainer" @scroll="handleScroll">
              <div v-if="commentloading && queryRequest.current > 1" class="loading-comments">
                <div class="loading-spinner">
                  <i class="fas fa-spinner fa-spin"></i>
                  <span>加载更多评论中...</span>
                </div>
              </div>
              <div v-if="commentloading && queryRequest.current === 1" class="loading-comments">
                <div class="loading-spinner">
                  <i class="fas fa-spinner fa-spin"></i>
                  <span>加载中...</span>
                </div>
              </div>
              <comment-list
                :comments="comments"
                @reply-clicked="handleReplyClick"
                @update-comments="queryComments"
              />
              <div v-if="isEndOfData && !commentloading" class="end-message">没有更多评论了~</div>
              <div v-if="commentloading && queryRequest.current > 1" class="loading-more-indicator">
                <div class="loading-spinner">
                  <i class="fas fa-spinner fa-spin"></i>
                  <span>加载中...</span>
                </div>
              </div>
            </div>
          </div>
          <div class="comment-input">
            <div v-if="showEmojiPicker" class="emoji-picker-wrapper">
              <emoji-picker
                class="custom-emoji-picker"
                :i18n="emojiI18n"
                @select="onEmojiSelect"
              />
            </div>
            <div class="comment-input-bar">
              <div class="input-wrapper">
                <div class="user-avatar-input">
                  <img
                    :src="loginUserStore.loginUser?.userAvatar || getDefaultAvatar(loginUserStore.loginUser?.userName)"
                    :alt="loginUserStore.loginUser?.userName"
                    class="input-user-avatar"
                    @click="handleUserClick(loginUserStore.loginUser)"
                  >
                </div>
                <input
                  v-model="commentContent"
                  :placeholder="picture.allowComment ? (replyCommentId ? `回复 @${replyComment?.commentUser?.userName || '用户'}：${replyTargetContent}` : '说点什么...') : '主人关闭了评论功能'"
                  class="comment-input-field"
                  :disabled="!picture.allowComment"
                  @input="handleInputChange"
                  @keydown.enter.prevent="addComment"
                >
                <!-- 输入框为空时的按钮组（这里不包含点赞、分享、更多按钮） -->
                <div class="input-actions" v-if="!commentContent.trim()">
                  <button
                    class="emoji-btn"
                    :disabled="!picture.allowComment"
                    @click="toggleEmojiPicker"
                  >
                    😊
                  </button>
                </div>
                <div class="input-actions" v-else>
                  <button
                    class="emoji-btn"
                    :disabled="!picture.allowComment"
                    @click="toggleEmojiPicker"
                  >
                    😊
                  </button>
                  <button
                    class="send-comment-btn"
                    :disabled="!commentContent.trim() || !picture.allowComment"
                    @click="addComment"
                  >
                    发送
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- 删除状态/空状态 -->
    <div v-if="isDeleted" class="deleted-view">
      <div class="deleted-content">
        <div class="custom-empty-state">
          <img :src="emptyImage" alt="加载失败" class="empty-illustration" />
          <h2 class="error-title">该作品已消失</h2>
          <p class="error-desc">可能已被作者删除或管理员下架，换个有趣的去看看吧</p>
          <a-button type="primary" class="back-btn" @click="router.back()">返回上一页</a-button>
        </div>
      </div>
    </div>

    <!-- 其他弹窗/组件 (保留原有逻辑) -->
    <ShareModal
      ref="shareModalRef"
      :link="shareLink"
      :imageUrl="picture.thumbnailUrl || picture.url"
      :title="picture.name"
      :user="picture.user"
      :createTime="picture.createTime"
    />
    <ReportModal
      ref="reportModalRef"
      :target-type="reportTargetType"
      :target-id="reportTargetId"
    />
    <div v-if="showChatModal" class="chat-room-modal">
      <div class="chat-room-content" :class="{ 'mobile': isMobile }">
        <div class="chat-room-header">
          <div class="chat-room-title">
            <h3>图片聊天室</h3>
            <div class="online-count">在线 {{ onlineCount || 0 }} 人</div>
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
    <div v-if="deleteConfirmVisible" class="delete-modal">
      <div class="modal-content">
        <i class="fas fa-ellipsis-h"></i>
        <h3>确认删除图片？</h3>
        <p>删除后无法恢复，请谨慎操作</p>
        <div class="modal-actions">
          <button class="btn-cancel" @click="deleteConfirmVisible = false">取消</button>
          <button class="btn-confirm" @click="confirmDelete">确认删除</button>
        </div>
      </div>
    </div>
    <!-- 聊天室弹窗 -->
    <div v-if="showChatModal" class="chat-room-modal">
      <div class="chat-room-content" :class="{ 'mobile': isMobile }">
        <div class="chat-room-header">
          <div class="chat-room-title">
            <h3>图片聊天室</h3>
            <div class="online-count">在线 {{ onlineCount || 0 }} 人</div>
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

    <!-- 权限设置弹窗 -->
    <div v-if="showPermissionSetting" class="permission-setting-modal">
      <div class="modal-mask" @click="showPermissionSetting = false"></div>
      <div class="permission-setting-content">
        <div class="modal-header">
          <h3>权限设置</h3>
          <button class="close-btn" @click="showPermissionSetting = false">
            <i class="fas fa-times"></i>
          </button>
        </div>
        <ContentPermissionSetting
          :pictureId="props.id"
          :initialPermissions="{
            allowLike: picture.allowLike ? 1 : 0,
            allowComment: picture.allowComment ? 1 : 0,
            allowCollect: picture.allowCollect ? 1 : 0,
            allowShare: picture.allowShare ? 1 : 0
          }"
          @permissions-updated="handlePermissionsUpdated"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick, onUnmounted, reactive, h } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { downloadImage, formatSize, toHexColor } from '@/utils'
import { getDeviceType } from '@/utils/device'
import { DEVICE_TYPE_ENUM } from '@/constants/device'
import { SPACE_PERMISSION_ENUM } from '@/constants/space'
import ShareModal from '@/components/ShareModal.vue'
import PictureChatRoom from '@/components/PictureChatRoom.vue'
import { getDefaultAvatar } from '@/utils/userUtils'
import { ImagePreview } from 'vant'
import 'vant/lib/image-preview/style'
import {
  deletePictureUsingPost,
  getPictureVoByIdUsingGet
} from '@/api/pictureController'
import {
  addUserFollowsUsingPost,
  findIsFollowUsingPost
} from '@/api/userFollowsController'
import { message} from 'ant-design-vue'
import CommentList from '@/components/CommentList.vue'
import { addCommentUsingPost, queryCommentUsingPost } from '@/api/commentsController'
import EmojiPicker from '@/components/EmojiPicker.vue'
import { doLikeUsingPost } from '@/api/likeRecordController.ts'
import { throttle } from 'lodash-es'
import { doShareUsingPost } from '@/api/shareRecordController'
import { addFavoriteRecordUsingPost, cancelFavoriteUsingPost } from '@/api/favoriteRecordController'
import { formatTime } from '@/utils/dateUtils.ts'
import { ViewDurationTracker } from '@/utils/viewDurationTracker'
import ContentPermissionSetting from '@/components/ContentPermissionSetting.vue'
import { LockOutlined } from '@ant-design/icons-vue'
import emptyImage from '@/assets/illustrations/empty.png'

const route = useRoute()
const device = ref<string>('')
const pictureLoaded = ref(false)
const mounted = ref(false)
const imgReady = ref(false)
const pageReady = ref(false)
const isDeleted = computed(() => {
  return pictureLoaded.value && (!picture.value || !picture.value.id)
})

const isMobile = ref(false)
const showInfoModal = ref(false) // 新增：控制信息弹窗显示
const showMoreDrawer = ref(false) // 控制更多功能抽屉显示
const showPermissionSetting = ref(false) // 控制权限设置弹窗显示
const descExpanded = ref(false) // 新增：控制描述展开/收起
const pictureDescRef = ref(null) // 添加引用以检测文本是否溢出

// 浏览时长记录器
const viewDurationTracker = new ViewDurationTracker(1) // 1表示图片类型

const isDescLong = computed(() => {
  const intro = picture.value.introduction || ''
  return intro.length > 100 || intro.split('\n').length > 2
})

// 检测文本是否视觉上被截断
const isTextTruncated = ref(false)

// 检查文本是否被截断的方法
const checkTextTruncation = () => {
  nextTick(() => {
    if (pictureDescRef.value && picture.value.introduction) {
      const element = pictureDescRef.value
      // 检查文本是否溢出（被截断）
      isTextTruncated.value = element.scrollHeight > element.clientHeight || element.scrollWidth > element.clientWidth
    }
  })
}

// 结合原始判断和视觉截断检测
const shouldShowExpandButton = computed(() => {
  return isDescLong.value || isTextTruncated.value
})

onMounted(async () => {
  device.value = await getDeviceType()
  isMobile.value = device.value === DEVICE_TYPE_ENUM.MOBILE
  mounted.value = true

  await fetchPictureDetail()
  pageReady.value = true

  Promise.all([
    checkIsFollowed(),
    updateLatestData()
  ])

  if (loginUserStore.loginUser) {
    nextTick(() => {
      showChatModal.value = false
    })
  }

  document.addEventListener('click', handleClickOutside)
  window.addEventListener('resize', checkDescriptionLines)

  // 检查文本截断状态
  nextTick(() => {
    checkTextTruncation()
  })

  // 初始化浏览时长记录
  setTimeout(() => {
    if (picture.value.id && loginUserStore.loginUser?.id) {
      viewDurationTracker.init(picture.value.id, loginUserStore.loginUser.id);
    }
  }, 100) // 延迟执行，确保picture数据已加载
})

// 处理收藏操作
const doFavorite = async () => {
  if (!loginUserStore.loginUser?.id) {
    message.warning('请先登录')
    return
  }

  // 检查收藏权限
  if (!checkPermissionAndShowMessage(picture.value.allowCollect !== 0, '主人关闭了收藏功能')) {
    return
  }

  if (picture.value.isFavorited === 1) {
    // 取消收藏
    try {
      const res = await cancelFavoriteUsingPost({
        userId: loginUserStore.loginUser.id,
        targetId: picture.value.id,
        targetType: 1
      })
      if (res.data.code === 0) {
        // 成功取消收藏，前端模拟状态改变和数量减少
        picture.value.isFavorited = 0
        picture.value.favoriteCount = String(Number(picture.value.favoriteCount || 0) - 1)
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
        targetId: picture.value.id,
        targetType: 1,
        targetUserId: picture.value.user.id,
        isFavorite: true
      })
      if (res.data.code === 0) {
        // 成功收藏，前端模拟状态改变和数量增加
        picture.value.isFavorited = 1
        picture.value.favoriteCount = String(Number(picture.value.favoriteCount || 0) + 1)
        message.success('收藏成功')
      } else {
        message.error('收藏失败：' + res.data.message)
      }
    } catch (error) {
      message.error('收藏失败')
    }
  }
}

onUnmounted(() => {
  if (chatRoomRef.value) {
    chatRoomRef.value.disconnect()
  }
  document.removeEventListener('click', handleClickOutside)
  document.body.style.overflow = ''
  window.removeEventListener('resize', checkDescriptionLines)

  // 清理浏览时长记录
  viewDurationTracker.cleanup();
})

function createPermissionChecker(permission: string) {
  return computed(() => {
    return (picture.value.permissionList ?? []).includes(permission)
  })
}

const canEdit = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_EDIT)
const canDelete = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_DELETE)
const showShareButton = computed(() => true)

const showChatRoom = computed(() => {
  return showShareButton.value && !picture.value?.spaceId
})

interface Props {
  id: string | number
}

const props = defineProps<Props>()
const picture = ref<API.PictureVO>({} as API.PictureVO)
const initialPictureUrl = ref<string>('')
const loginUserStore = useLoginUserStore()
const router = useRouter()

const getRoutePictureData = () => {
  try {
    if (typeof window !== 'undefined' && window.history?.state?.pictureData) {
      return window.history.state.pictureData
    }
    return null
  } catch (e) {
    return null
  }
}

const fetchPictureDetail = async () => {
  try {
    const routePictureData = getRoutePictureData()
    if (routePictureData) {
      picture.value = routePictureData
      initialPictureUrl.value = routePictureData.url
      onlineCount.value = routePictureData.chatCount || 0
      pictureLoaded.value = true
      nextTick(() => checkTextTruncation())
      return
    }

    const res = await getPictureVoByIdUsingGet({ id: props.id })
    if (res.data.code === 0 && res.data.data) {
      picture.value = res.data.data
      initialPictureUrl.value = res.data.data.url
      onlineCount.value = res.data.data.chatCount || 0
      pictureLoaded.value = true
      nextTick(() => checkTextTruncation())
    } else {
      pictureLoaded.value = true
      nextTick(() => checkTextTruncation())
    }
  } catch (e: any) {
    pictureLoaded.value = true
    nextTick(() => checkTextTruncation())
  }
}

const updateLatestData = async () => {
  try {
    const res = await getPictureVoByIdUsingGet({ id: props.id })
    if (res.data.code === 0 && res.data.data) {
      const latestData = res.data.data
      const imageUrl = picture.value.url
      updateDataWithAnimation(latestData)
      picture.value.url = imageUrl
      onlineCount.value = latestData.chatCount || 0

      nextTick(() => {
        checkDescriptionLines()
        checkTextTruncation()
      })
    }
  } catch (e: any) {
    console.error('获取最新数据失败:', e)
  }
}

const updateDataWithAnimation = (latestData: API.PictureVO) => {
  Object.keys(latestData).forEach(key => {
    if (key !== 'url') {
      picture.value[key] = latestData[key]
    }
  })
  onlineCount.value = latestData.chatCount || 0
}

const checkDescriptionLines = () => {}

const doEdit = () => {
  router.push({
    path: '/add_picture',
    query: {
      id: picture.value.id,
      spaceId: picture.value.spaceId,
    },
  })
}

const deleteConfirmVisible = ref(false)
const showDeleteConfirm = () => {
  deleteConfirmVisible.value = true
}

const confirmDelete = async () => {
  try {
    const res = await deletePictureUsingPost({ id: picture.value?.id })
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

const handleDownload = () => {
  if (picture.value.isDownload === 0) {
    message.warning({
      content: '抱歉，该图片暂不支持下载',
      icon: h(LockOutlined, { style: 'color: #faad14; margin-right: 8px;' }),
      class: 'custom-message'
    })
    return
  }

  downloadImage(picture.value.url, picture.value.name || '图片')

  message.success({
    content: '开始下载...',
    icon: h('i', { class: 'fas fa-download', style: 'color: #52c41a; margin-right: 8px;' })
  })
}

const shareModalRef = ref()
const reportModalRef = ref()
const reportTargetType = ref<string>('')
const reportTargetId = ref<string>('')

const shareLink = computed(() => {
  if (!picture.value?.id) return ''
  return `${window.location.origin}/picture/${picture.value.id}`
})

const openReportModal = () => {
  if (reportModalRef.value) {
    // 设置举报目标类型和ID
    reportTargetType.value = '1'  // 图片类型
    reportTargetId.value = picture.value.id.toString()

    // 传递参数并打开模态框
    reportModalRef.value.openModal(reportTargetType.value, reportTargetId.value)
  }
}

const doShare = async () => {
  if (!loginUserStore.loginUser?.id) {
    message.warning('请先登录')
    return
  }

  // 检查分享权限
  if (!checkPermissionAndShowMessage(picture.value.allowShare !== 0, '主人关闭了分享功能')) {
    return
  }

  if (picture.value.isShared === 1) {
    try {
      const res = await doShareUsingPost({
        targetId: picture.value.id,
        targetType: 1,
        isShared: false
      })
      if (res.data.code === 0) {
        picture.value.shareCount = String(Number(picture.value.shareCount || 0) - 1)
        picture.value.isShared = 0
      }
    } catch (error) {
      console.error('取消分享失败:', error)
    }
    return
  }

  try {
    const res = await doShareUsingPost({
      targetId: picture.value.id,
      targetType: 1,
      isShared: true
    })
    if (res.data.code === 0) {
      picture.value.shareCount = String(Number(picture.value.shareCount || 0) + 1)
      picture.value.isShared = 1
      shareModalRef.value?.openModal()
    }
  } catch (error) {
    console.error('分享失败:', error)
  }
}

const isFollowed = ref(false)
const checkIsFollowed = async () => {
  if (!loginUserStore.loginUser?.id || !picture.value?.user?.id) return
  try {
    const res = await findIsFollowUsingPost({
      followerId: loginUserStore.loginUser.id,
      followingId: picture.value.user.id
    })
    if (res.data?.data) {
      isFollowed.value = res.data.data
    }
  } catch (error) {
    console.error('检查关注状态失败:', error)
  }
}

const handleFollow = async () => {
  if (!loginUserStore.loginUser?.id) {
    message.warning('请先登录')
    return
  }

  try {
    const res = await addUserFollowsUsingPost({
      followerId: loginUserStore.loginUser.id,
      followingId: picture.value.user.id,
      followStatus: isFollowed.value ? 0 : 1
    })

    if (res.data?.code === 0) {
      isFollowed.value = !isFollowed.value
    }
  } catch (error) {
    message.error('操作失败')
  }
}

const showChatModal = ref(false)
const onlineUsers = ref<any[]>([])
const onlineCount = ref(picture.value?.chatCount || 0)
const chatRoomRef = ref()

const handleChatMessage = (msg: any) => {
  if (msg.type === 'onlineUsers') {
    onlineCount.value = msg.onlineCount
    onlineUsers.value = msg.onlineUsers
  }
}

const openChatModal = () => {
  if (loginUserStore.loginUser) {
    showChatModal.value = true
    showMoreDrawer.value = false
  } else {
    message.warning('请先登录')
  }
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

const handleImageLoad = () => {
  pictureLoaded.value = true
  imgReady.value = true
}

const visible = ref(false)
const comments = ref<API.Comment[]>([])
const commentContent = ref('')
const replyCommentId = ref('')
const replyComment = ref(null) // 添加这行来保存被回复的评论对象
const replyTargetContent = ref('')
const commentloading = ref(false)
const showEmojiPicker = ref(false)
const isEndOfData = ref(false)

const queryRequest = reactive<API.CommentsQueryRequest>({
  targetId: props.id,
  targetType: 1,
  current: 1,
  pageSize: 15,
})

const queryComments = async () => {
  try {
    commentloading.value = true

    const res = await queryCommentUsingPost(queryRequest)
    if (res.data?.data != null) {
      const newComments = res.data.data.records.map(comment => ({
        ...comment,
        commentId: comment.commentId?.toString(),
        parentCommentId: comment.parentCommentId?.toString(),
      }))

      if (queryRequest.current === 1) {
        comments.value = newComments
      } else {
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
    console.error('查询评论异常', error)
    message.error('获取评论失败')
  } finally {
    commentloading.value = false
  }
}

const addComment = async () => {
  // 检查评论权限
  if (!checkPermissionAndShowMessage(picture.value.allowComment !== 0, '主人关闭了评论功能')) {
    return
  }

  if (!commentContent.value.trim()) {
    message.warning('评论内容不能为空')
    return
  }

  // 防抖检查
  if (isAddingComment.value) {
    message.warning('请勿重复提交')
    return
  }

  isAddingComment.value = true

  try {
    const res = await addCommentUsingPost({
      targetId: picture.value.id,
      targetType: 1,
      content: commentContent.value.trim(),
      parentCommentId: replyCommentId.value || '0'
    })

    if (res.data.code === 0) {
      commentContent.value = ''
      replyCommentId.value = ''
      queryRequest.current = 1
      await queryComments()
      picture.value.commentCount = String(Number(picture.value.commentCount || 0) + 1)

      nextTick(() => {
        window.scrollTo({ top: 0, behavior: 'smooth' })
        if (scrollContainer.value) {
          scrollContainer.value.scrollTop = 0
        }
      })
    }
  } catch (error) {
    message.error('评论失败')
  } finally {
    // 重置防抖标识
    isAddingComment.value = false
  }
}

const toggleEmojiPicker = () => {
  showEmojiPicker.value = !showEmojiPicker.value
}

const handleInputChange = () => {
  if (commentContent.value.trim().length === 0) showEmojiPicker.value = false
}

const onEmojiSelect = (emoji: string) => {
  commentContent.value += emoji
  showEmojiPicker.value = false
}

const handleClickOutside = (event: MouseEvent) => {
  const target = event.target as HTMLElement
  if (!target.closest('.emoji-picker') && !target.closest('.emoji-btn')) {
    showEmojiPicker.value = false
  }
  // 点击空白处取消回复状态，但排除回复按钮的点击
  if (replyCommentId.value && !target.closest('.comment-input-field') && !target.closest('.comment-input-bar') && !target.closest('.action-item')) {
    cancelReply()
  }
}

const handleReplyClick = (commentId: string) => {
  replyCommentId.value = commentId
  // 查找被回复的评论对象
  const findComment = (comments: API.Comment[]) => {
    for (const comment of comments) {
      if (comment.commentId === commentId) {
        return comment
      }
      if (comment.children && comment.children.length > 0) {
        const found = findComment(comment.children)
        if (found) return found
      }
    }
    return null
  }

  const foundComment = findComment(comments.value)
  replyComment.value = foundComment

  // 设置被回复评论的用户名和内容预览
  const targetComment = foundComment
  const replyTargetUserName = targetComment?.commentUser?.userName || ''

  // 截取被回复评论内容的前50个字符作为预览
  let contentPreview = targetComment?.content || ''
  if (contentPreview.length > 50) {
    contentPreview = contentPreview.substring(0, 50) + '...'
  }
  replyTargetContent.value = contentPreview

  nextTick(() => {
    const inputEl = document.querySelector('.comment-input-field') as HTMLInputElement
    if (inputEl) {
      inputEl.focus()
      inputEl.scrollIntoView({ behavior: 'smooth', block: 'end' })
    }
  })
}

const cancelReply = () => {
  replyCommentId.value = ''
  replyComment.value = null // 清空被回复的评论对象
  replyTargetContent.value = ''
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

const closeModal = () => {
  visible.value = false
  commentContent.value = ''
  replyCommentId.value = ''
}

const handleScroll = throttle(() => {
  const container = scrollContainer.value
  if (!container || commentloading.value || isEndOfData.value) return

  const { scrollTop, clientHeight, scrollHeight } = container
  // 使用更灵活的距离判断，增加触底敏感度
  if (scrollTop + clientHeight >= scrollHeight - 50) {
    loadMoreComments()
  }
}, 150)

const loadMoreComments = async () => {
  if (commentloading.value || isEndOfData.value) return

  commentloading.value = true
  queryRequest.current++

  try {
    await queryComments()
  } catch (error) {
    console.error('加载更多评论失败:', error)
    message.error('加载更多评论失败')
    queryRequest.current-- // 回滚页码
  } finally {
    commentloading.value = false
  }
}

// 防抖标识
const isAddingComment = ref(false)

const scrollContainer = ref<HTMLElement | null>(null)

onMounted(() => {
  if (picture.value.id) {
    queryRequest.targetId = picture.value.id
    queryComments()
  }
})

const handleCommentClick = () => {
  visible.value = true
  nextTick(() => {
    // 确保评论弹窗打开后聚焦到输入框
    const textarea = document.querySelector('.comment-input textarea')
    if (textarea) {
      textarea.focus()
    }
    // 如果评论还没有加载过，则加载评论数据
    if (comments.value.length === 0 && !commentloading.value) {
      // 立即设置加载状态，然后异步加载数据
      commentloading.value = true
      queryComments()
    }
  })
}

const doLike = async () => {
  // 检查点赞权限
  if (!checkPermissionAndShowMessage(picture.value.allowLike !== 0, '主人关闭了点赞功能')) {
    return
  }

  try {
    const res = await doLikeUsingPost({
      targetId: props.id,
      targetType: 1,
      isLiked: picture.value.isLiked !== 1
    })

    if (res.data.code === 0) {
      if (picture.value.isLiked !== 1) {
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
const showPreview = ref(false)
const handlePreviewChange = (visible: boolean) => {
  showPreview.value = visible
  document.body.style.overflow = visible ? 'hidden' : ''
}

// 权限检查函数
const checkPermissionAndShowMessage = (permission: boolean, messageText: string) => {
  if (!permission) {
    message.warning({
      content: messageText,
      icon: h('i', { class: 'fas fa-lock', style: `color: var(--nav-item-active-text); margin-right: 8px;` }),
      class: 'custom-message'
    })
    return false
  }
  return true
}

// 处理权限更新
const handlePermissionsUpdated = (updatedPermissions: any) => {
  // 更新本地图片数据
  Object.assign(picture.value, updatedPermissions);
  // 关闭权限设置弹窗
  showPermissionSetting.value = false;
  message.success('权限设置更新成功');
}

const showImagePreview = () => {
  if (isMobile.value) {
    showPreview.value = true
  } else {
    handlePreviewChange(true)
  }
}

const closeImagePreview = () => {
  if (isMobile.value) {
    showPreview.value = false
  } else {
    handlePreviewChange(false)
  }
}

const dominantColor = computed(() => {
  return picture.value?.picColor ? toHexColor(picture.value.picColor) : '#4a5568'
})

function adjustColor(hex: string, percent: number) {
  hex = hex.replace('#', '').padStart(6, '0')
  const r = parseInt(hex.substring(0, 2), 16)
  const g = parseInt(hex.substring(2, 4), 16)
  const b = parseInt(hex.substring(4, 6), 16)

  const adjustValue = (value: number) => {
    const adjusted = value + (value * (percent / 100))
    return Math.min(255, Math.max(0, Math.round(adjusted)))
  }

  const rr = adjustValue(r).toString(16).padStart(2, '0')
  const gg = adjustValue(g).toString(16).padStart(2, '0')
  const bb = adjustValue(b).toString(16).padStart(2, '0')

  return `#${rr}${gg}${bb}`
}

const formattedIntroduction = computed(() => {
  const intro = picture.value.introduction || '暂无介绍'
  if (!intro) return '暂无介绍'
  const paragraphs = intro.split('\n')
  return paragraphs.map(paragraph => `<p>${paragraph}</p>`).join('')
})

const isMultiline = computed(() => {
  const intro = picture.value.introduction || ''
  return intro.includes('\n') || intro.length > 50
})
</script>

<style scoped>

/* 背景效果层 */
.background-effects {
  position: absolute;
  inset: 0;
  overflow: hidden;
}

.color-blur-layer {
  position: absolute;
  inset: 0;
  opacity: 0.4;
  filter: blur(100px);
  transition: opacity 1s ease;
}

.blurred-image-layer {
  position: absolute;
  inset: 0;
  background-position: center;
  background-size: cover;
  background-repeat: no-repeat;
  filter: blur(22px) brightness(0.85) saturate(0.95) contrast(1.1);
  opacity: 0.55;
  transform: scale(1.1);
  transition: opacity 1.2s ease, filter 0.8s ease;
  background-blend-mode: soft-light;
}

.mobile-picture-detail {
  position: fixed;
  inset: 0;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  color: #fff;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  background: #000;
}

/* 欢迎层 */
.welcome-layer {
  position: fixed;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--header-background);
  z-index: 1000;
}

.welcome-content {
  text-align: center;
}

.emoji-row {
  font-size: 32px;
  margin: 20px;
  opacity: 0;
  animation: emojiAppear 0.5s ease-out forwards;
}

.emoji-row:nth-child(1) { animation-delay: 0.1s; }
.emoji-row:nth-child(2) { animation-delay: 0.3s; }

@keyframes emojiAppear {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

/* 图片容器 - 占满屏幕宽度 */
.picture-container {
  width: 100%;
  height: 100%;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.full-width-image {
  width: 100%;
  height: auto;
  object-fit: cover;
  display: block;
  max-height: 100%;
  max-width: 100%;
}

/* 骨架图和模糊占位 */
.mobile-image-skeleton {
  width: 100%;
  height: 100%;
  background: var(--component-bg, #222);
  animation: skeleton-blink 1.5s ease-in-out infinite;
}

@keyframes skeleton-blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}

.blur-placeholder {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: contain; /* 改成 contain 与原图保持一致显示，防止裁剪裁切导致拉伸难看 */
  filter: blur(15px);
  transform: scale(1.02);
  opacity: 1;
  transition: opacity 0.5s ease-out;
  pointer-events: none;
  z-index: 1;
}

.blur-placeholder.hidden-placeholder {
  opacity: 0;
}

.preview-image {
  opacity: 0;
  transition: opacity 0.4s ease-in;
  position: relative;
  z-index: 2;
}

.preview-image.img-visible {
  opacity: 1;
}

/* 顶部导航栏 - 浮动在图片上方 */
.top-nav {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 64px;

  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  z-index: 10;
}

.left-nav {
  display: flex;
  align-items: center;
  gap: 12px;
}

.back-btn {
  background: transparent;
  border: none;
  color: #fff;
  font-size: 24px;
  cursor: pointer;
}

.author-info {
  display: flex;
  align-items: center;
  gap: 8px;
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
  cursor: pointer;
}

.author-title {
  font-size: 12px;

}

.follow-btn {
  background: #2563eb;
  color: #fff;
  border: none;
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s;
}

.follow-btn.is-followed {
  background: #4b5563;
}

/* 右侧功能按钮栏 */
.right-action-bar {
  position: absolute;
  right: 16px;
  bottom: 100px;
  display: flex;
  flex-direction: column;
  z-index: 10;
}

.action-btn {
  border: none;
  color: #ffffff;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  font-size: 14px;
  width: 56px;
  height: 56px;
  justify-content: center;
  border-radius: 50%;
}

.action-btn i {
  font-size: 28px;
}

.action-btn .count {
  font-size: 14px;
  color: #ffffff;
}

.like-btn.is-liked {
  color: #ef4444;
}

.like-btn.is-liked .count {
  color: #ef4444;
}

.like-btn.is-liked i {
  color: #ef4444;
  animation: likeAnimation 0.6s ease;
}

/* 收藏按钮样式 */
.favorite-btn.is-favorited {
  color: #FFD700;
}

.favorite-btn.is-favorited .count {
  color: #FFD700;
}

.favorite-btn.is-favorited i {
  color: #FFD700;
  animation: favoriteAnimation 0.6s ease;
}

@keyframes favoriteAnimation {
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

/* 图片信息浮层 */
.picture-info-float {
  position: absolute;
  left: 0;
  right: 80px; /* 为右侧按钮留出空间 */
  bottom: 20px;
  padding: 8px;
  padding-right: 4px;
  z-index: 10;
  background: rgba(0, 0, 0, 0); /* 很淡的浅蓝色背景 */
  border-radius: 12px;
  backdrop-filter: blur(0.6px);
}

.picture-title {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 4px;
  color: #ffffff;
}

.picture-desc {
  font-size: 14px;
  line-height: 1.4;
}

.picture-desc.collapsed {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.toggle-desc-btn {
  background: transparent;
  border: none;
  color: #3b82f6;
  font-size: 14px;
  margin-top: 4px;
  cursor: pointer;
}

.picture-stats {
  display: flex;
  gap: 16px;
  margin-top: 8px;
  font-size: 12px;
}

.picture-stats i {
  font-size: 12px;
}

/* 底部操作栏 */
.bottom-action-bar {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 64px;
  background: rgba(0,0,0,0.8);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  z-index: 10;
}

.bottom-btn {
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 14px;
  cursor: pointer;
  border: none;
  transition: background 0.2s;
}

.original-btn {
  background: #4b5563;
  color: #fff;
}

.filter-btn {
  background: #2563eb;
  color: #fff;
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-btn .dot {
  width: 8px;
  height: 8px;
  background: #ef4444;
  border-radius: 50%;
}

/* 信息弹窗 */
.info-modal {
  position: fixed;
  inset: 0;
  z-index: 100;
}

.modal-mask {
  position: absolute;
  inset: 0;
  background: rgba(0,0,0,0.8);
}

.info-modal-content {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: #000;
  border: 1px solid #374151;
  border-radius: 8px;
  padding: 20px;
  width: 80%;
  max-width: 400px;
  z-index: 101;
  animation: modalFadeIn 0.3s ease-out forwards;
}

@keyframes modalFadeIn {
  from { opacity: 0; transform: translate(-50%, -45%); }
  to { opacity: 1; transform: translate(-50%, -50%); }
}

.modal-title {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 16px;
}

.modal-content {
  font-size: 14px;

  display: flex;
  flex-direction: column;
  gap: 8px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.info-item .label {
  color: #d1d5db;
}

.info-item .value {
  flex: 1;
}

.tag-item {
  display: inline-block;
  margin-right: 4px;
  color: #f97316;
}

.color-value {
  display: flex;
  align-items: center;
  gap: 8px;
}

.color-dot {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 1px solid #fff;
}

.color-hex {
  font-family: monospace;
}

.close-modal-btn {
  width: 100%;
  background: #4b5563;

  border: none;
  padding: 8px 0;
  border-radius: 8px;
  font-size: 14px;
  margin-top: 20px;
  cursor: pointer;
  transition: background 0.2s;
}

.close-modal-btn:hover {
  background: #374151;
  color: #fff;
}

/* 更多功能抽屉 */
.more-drawer {
  position: fixed;
  inset: 0;
  z-index: 100;
}

.drawer-mask {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.3);
}

.drawer-content {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  border-top: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 16px 16px 0 0;
  max-height: 80vh;
  min-height: 300px;
  z-index: 101;
  display: flex;
  flex-direction: column;
}

.drawer-header {
  padding: 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
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
  gap: 8px;
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
  color: #ffffff;
}

.publish-time {
  font-size: 12px;

}

.close-btn {
  background: transparent;
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
}

.close-btn:hover {
  background: rgba(255, 255, 255, 0.1);
}

.picture-info {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.picture-thumb {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 8px;
}

.picture-details {
  flex: 1;
}

.picture-title {
  font-size: 16px;
  font-weight: 500;
  color: #ffffff;
  margin-bottom: 8px;
}

.picture-stats {
  display: flex;
  gap: 16px;
}

.picture-stats span {
  font-size: 12px;

  display: flex;
  align-items: center;
  gap: 4px;
}

/* 作品详细信息 */
.picture-details-info {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 12px;
  padding: 12px;
  margin-bottom: 16px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.info-item:last-child {
  margin-bottom: 0;
}

.label {

  font-size: 14px;
  min-width: 50px;
}

.value {
  color: #ffffff;
  font-size: 14px;
  flex: 1;
}

.tag-item {
  display: inline-block;
  margin-right: 4px;
  color: #f97316;
}

.color-value {
  display: flex;
  align-items: center;
  gap: 8px;
}

.color-dot {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 1px solid #fff;
}

.color-hex {
  font-family: monospace;
}

.drawer-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.action-buttons-container {
  overflow-x: auto;
  padding: 8px 0;
  -webkit-overflow-scrolling: touch;
}

.action-buttons {
  display: flex;
  flex-wrap: nowrap;
  gap: 16px;
  padding: 0 16px;
  min-width: max-content;
}

.action-btn {
  flex: 0 0 auto;
  white-space: nowrap;
}

.action-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 12px 8px;
  background: transparent;
  border: none;
  color: #ffffff;
  font-size: 14px;
  cursor: pointer;
  border-radius: 8px;
  transition: background 0.2s;
  flex: 1;
}

.action-btn:disabled,
.action-btn.permission-disabled {
  opacity: 0.5;
  cursor: not-allowed;
  background: transparent;
}

.action-btn i {
  font-size: 28px;
}
/* 评论弹窗 */
.comment-modal {
  position: fixed;
  inset: 0;
  z-index: 100;
}

.comment-modal-content {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: var(--comment-drawer-background);
  border-top: 1px solid var(--border-color);
  border-radius: 16px 16px 0 0;
  height: 76vh;
  z-index: 101;
  display: flex;
  flex-direction: column;
  opacity: 0.98;
}

.comment-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-bottom: 1px solid var(--border-color);
}

.comment-count {
  font-size: 16px;
  font-weight: 500;
  color: var(--comment-header-text);
}

.comment-tabs {
  display: flex;
  gap: 8px;
}

.tab-btn {
  padding: 4px 12px;
  border-radius: 16px;
  font-size: 14px;
  border: none;
  cursor: pointer;
  color: var(--comment-action-color);
  background: var(--comment-input-background);
}

.tab-btn.active {
  background: var(--primary-color);
  color: var(--text-other);
}

.close-comment-btn {
  background: transparent;
  border: none;
  color: var(--comment-action-color);
  font-size: 20px;
  cursor: pointer;
}

.end-message {
  text-align: center;
  padding: 16px;
  color: #9ca3af;
  font-size: 14px;
}

.comments-container {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.comments-list {
  flex: 1;
  overflow-y: auto;
}

.comment-input {
  padding: 4px;
  background: var(--comment-input-background);
  flex-shrink: 0;
}

.reply-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  font-size: 14px;
  color: #9ca3af;

}

.reply-info button {
  background: none;
  border: none;
  color: #3b82f6;
  cursor: pointer;
  font-size: 14px;
}

.input-box {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid #4b5563;
  border-radius: 20px;
  padding: 8px 12px;
}

.input-box textarea {
  flex: 1;
  background: transparent;
  border: none;
  color: var(--comment-action-color);
  font-size: 14px;
  resize: none;
  min-height: 40px;
  max-height: 100px;
  outline: none;
  padding: 6px 0;
  margin: 0;
}

.input-box textarea::placeholder {

}

.emoji-btn, .image-btn {
  background: transparent;
  border: none;
  color: #9ca3af;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  padding: 8px;
  margin: auto;
  border-radius: 50%;
  width: 36px;
  height: 36px;
  flex-shrink: 0;
}

.emoji-btn:hover, .image-btn:hover {
  color: var(--comment-action-color);
  background: rgba(255, 255, 255, 0.1);
}

.send-btn {
  padding: 0 16px;
  height: 36px;
  border: none;
  border-radius: 8px;
  background: #1890ff;
  color: #f0f0f0;
  cursor: pointer;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  font-weight: 500;
  margin: auto;
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.send-btn:not(:disabled):hover {
  background: #40a9ff;
  color: #fff;
}

.send-btn:active:not(:disabled) {
  transform: scale(0.95);
}

.reply-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  font-size: 14px;

}

.reply-bar button {
  background: none;
  border: none;
  color: #3b82f6;
  cursor: pointer;
  font-size: 14px;
}

/* 分享按钮高亮样式 */
.share-btn.is-shared {
  color: #a78bfa;
}

.share-btn.is-shared .count {
  color: #a78bfa;
}

.share-btn.is-shared i {
  color: #a78bfa;
  animation: shareAnimation 0.6s ease;
}

/* 评论输入框栏 */
.comment-input-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: var(--card-background);
}

.user-avatar-input {
  display: flex;
  align-items: center;
  justify-content: center;
}

.input-user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
  cursor: pointer;
  transition: transform 0.2s ease;
}

.input-user-avatar:hover {
  transform: scale(1.05);
}

.comment-input-field {
  flex: 1;
  padding: 8px 16px;
  background: var(--comment-input-background);
  border: none;
  border-radius: 20px;
  font-size: 14px;
  outline: none;
  transition: background 0.3s ease;
  color: var(--comment-input-text);
  min-width: 0;
}

.comment-input-field::placeholder {
  color: var(--comment-input-placeholder);
}

.comment-input-field:focus {
  background: var(--hover-background);
}

.comment-input-field:disabled {
  background: var(--disabled-background, #f5f5f5);
  color: var(--disabled-text-color, #ccc);
  cursor: not-allowed;
  opacity: 0.6;
}

/* 表情包按钮 */
.emoji-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 50%;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #ffffff;
}

.emoji-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}

/* 互动按钮 */
.input-actions {
  display: flex;
  gap: 4px;
  align-items: center;
}

/* 评论输入框相关的按钮 */
.input-actions .action-btn {
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

.input-actions .action-btn i {
  font-size: 16px;
}

.input-actions .action-btn span {
  font-size: 12px;
}

/* 点赞高亮为红色 */
.action-btn.btn-like:hover,
.action-btn.btn-like.is-liked {
  color: var(--like-button-active-color);
}

/* 分享高亮为蓝色 */
.action-btn.btn-share:hover,
.action-btn.btn-share.is-shared {
  color: var(--link-color);
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



/* 发送按钮 */
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

/* 输入框容器 */
.input-wrapper {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 4px;
  position: relative;
  min-width: 0;
}

/* 评论加载动画 */
.loading-comments {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
}

.loading-more-indicator {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 12px;
  color: #9ca3af;
  font-size: 14px;
}

.loading-spinner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: #9ca3af;
  font-size: 14px;
}

.loading-spinner i {
  font-size: 20px;
}

/* 聊天室弹窗样式 */
.chat-room-modal {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.7);
}

.chat-room-content {
  background: var(--card-background);
  border-radius: 16px;
  width: 90%;
  max-width: 500px;
  height: 80vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
}

.chat-room-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-bottom: 1px solid var(--border-color);
  background: var(--header-background);
}

.chat-room-title {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-primary);
}

.chat-room-title h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
}

.online-count {
  font-size: 12px;
  color: var(--text-secondary);
  margin-left: 8px;
}

.btn-close {
  background: transparent;
  border: none;
  padding: 8px;
  cursor: pointer;
  border-radius: 4px;
  transition: background 0.2s;
  color: var(--text-primary);
}

.btn-close:hover {
  background: var(--hover-background);
}

.chat-room-body {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.modal-chat-room {
  width: 100%;
  height: 100%;
}

/* 优化输入框样式 - 已迁移到新的评论输入框样式 */

/* 响应式设计 */
@media screen and (max-width: 768px) {
  .comment-input-bar {
    padding: 8px 12px;
  }
  .input-actions {
    gap: 8px;
  }
}

@keyframes shareAnimation {
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

/* 删除确认模态框样式 */
.delete-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

/* 权限设置弹窗样式 */
.permission-setting-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 1001;
}

.permission-setting-modal .modal-mask {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
}

.permission-setting-content {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: var(--card-background);
  border-radius: 12px;
  padding: 8px;
  width: 90%;
  max-width: 500px;
  z-index: 1002;
}

.permission-setting-modal .modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #374151;
}

.permission-setting-modal .modal-header h3 {
  color: var(--text-secondary);
  margin: 0;
  font-size: 18px;
  font-weight: 500;
}

.permission-setting-modal .close-btn {
  background: transparent;
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
  color: var(--text-secondary);
}

.permission-setting-modal .close-btn:hover {
  background: #374151;
}

.delete-modal .modal-content {
  background: var(--card-background);
  border-radius: 12px;
  padding: 24px;
  width: 300px;
  text-align: center;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

.delete-modal i {
  font-size: 48px;
  color: #faad14;
  margin-bottom: 16px;
}

.delete-modal h3 {
  color: #ffffff;
  margin: 0 0 8px 0;
  font-size: 18px;
  font-weight: 500;
}

.delete-modal p {
  color: #bfbfbf;
  margin: 0 0 24px 0;
  font-size: 14px;
}

.modal-actions {
  display: flex;
  gap: 12px;
}

.btn-cancel, .btn-confirm {
  flex: 1;
  padding: 12px;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-cancel {
  background: #333333;
  color: #ffffff;
}

.btn-cancel:hover {
  background: #444444;
}

.btn-confirm {
  background: #ff4d4f;
  color: #ffffff;
}

.btn-confirm:hover {
  background: #ff7875;
}

/* 空状态样式 */
.deleted-view {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 80vh;
  width: 100%;
  padding: 0 24px;
  background: var(--background);
  z-index: 10;
  position: relative;

  .custom-empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 24px;
    animation: emptyFadeIn 0.8s ease-out;
    text-align: center;
  }
}

.empty-illustration {
  width: 100%;
  max-width: 320px;
  height: auto;
  opacity: 0.85;
  filter: drop-shadow(0 20px 40px rgba(0,0,0,0.08));
}

.error-title {
  font-size: 26px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
}

.error-desc {
  font-size: 15px;
  color: var(--text-secondary);
  margin: 0;
  max-width: 400px;
  line-height: 1.6;
}

@keyframes emptyFadeIn {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
