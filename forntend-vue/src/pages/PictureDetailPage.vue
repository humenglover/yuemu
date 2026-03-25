<template>
  <div class="picture-detail" :class="{ 'is-loaded': pageReady }">

    <!-- 背景层 -->
    <template v-if="pictureLoaded && picture.id">
      <div class="background-effects">
        <div
          class="color-blur-layer"
          :style="{
            background: `linear-gradient(135deg, ${dominantColor}, ${adjustColor(dominantColor, 20)}, var(--background))`,
            opacity: 0.4
          }"
        ></div>
        <div
          class="blurred-image-layer"
          :style="{ backgroundImage: picture.url ? `url(${picture.url})` : 'none' }"
        ></div>
      </div>
    </template>

    <!-- 内容层 -->
    <div class="content-layer" :class="{ 'is-loaded': mounted }">
      <!-- 删除状态 -->
      <div v-if="isDeleted" class="deleted-view">
        <div class="deleted-content">
          <i class="fas fa-trash-can"></i>
          <h2>图片已删除</h2>
          <p>该图片可能已被作者删除或管理员删除</p>
          <button class="btn-primary" @click="router.back()">返回上一页</button>
        </div>
      </div>

      <!-- 主要内容 -->
      <template v-else>
        <div class="layout">
          <!-- 左侧预览区 -->
          <div class="preview-section" :class="{ 'is-loaded': imgReady }">
            <!-- 骨架屏：数据未就绪 -->
            <div v-if="!pictureLoaded" class="image-skeleton">
              <a-skeleton-image style="width:100%;height:100%;" />
            </div>
            <!-- 图片容器：数据就绪后显示 -->
            <div v-else class="image-wrapper" :class="{ 'is-loaded': imgReady }">
              <!-- 模糊缩略图占位（原图未加载完时） -->
              <img
                v-if="!imgReady && (picture.thumbnailUrl || picture.url)"
                :src="picture.thumbnailUrl || picture.url"
                class="blur-placeholder"
                alt="Loading placeholder"
              />
              <!-- 原图（加载完成后淡入） -->
              <img
                v-if="picture.url"
                :src="picture.url"
                :alt="picture.name"
                class="preview-image"
                :class="{ 'img-visible': imgReady }"
                @click="showImagePreview"
                @load="handleImageLoad"
              />
            </div>
          </div>

          <!-- 图片预览组件（PC端） -->
          <template v-if="!isMobile">
            <a-image
              :src="picture.url"
              :preview="{
                visible: showPreview,
                onVisibleChange: handlePreviewChange,
                maskClassName: 'preview-mask'
              }"
              alt=""
              style="display: none;"
            />
          </template>

          <!-- 移动端图片预览（保留但适配平板） -->
          <van-image-preview
            v-if="isMobile"
            v-model:show="showPreview"
            :images="[picture.url]"
            :closeable="false"
            :show-index="false"
            :overlay-style="{ backgroundColor: 'rgba(0,0,0,0.9)' }"
            @close="closeImagePreview"
          >
            <template #cover>
              <div class="preview-info">
                <h3>{{ picture.name }}</h3>
                <p>{{ picture.introduction }}</p>
              </div>
            </template>
          </van-image-preview>

          <!-- 右侧信息栏 -->
          <div class="info-section" ref="infoSection" style="display: flex; flex-direction: column;">
            <!-- 骨架屏：数据未就绪 -->
            <div v-if="!pictureLoaded" class="info-skeleton">
              <a-skeleton active :paragraph="{ rows: 4 }" />
            </div>
            <div class="info-section-content" v-else ref="infoSectionContent" style="flex: 1; display: flex; flex-direction: column; overflow: hidden;">
              <!-- 图片信息卡片 -->
              <div class="info-card" style="flex: 1; display: flex; flex-direction: column; overflow: hidden;">
                <div class="card-header">
                  <div class="title-header">
                    <div class="title-avatar-container">
                      <div class="title-avatar" @click="handleUserClick(picture.user)">
                        <img
                          :src="picture.user?.userAvatar || getDefaultAvatar(picture.user?.userName)"
                          :alt="picture.user?.userName"
                          class="title-avatar-img"
                        >
                      </div>
                      <button
                        v-if="picture.user?.id !== loginUserStore.loginUser?.id"
                        class="btn-follow-overlay"
                        :class="{ 'is-followed': isFollowed }"
                        @click="handleFollow"
                      >
                        <i :class="isFollowed ? 'fas fa-minus' : 'fas fa-plus'" />
                      </button>
                    </div>
                    <h4 class="picture-title">{{ picture.name || '未命名' }}</h4>
                  </div>
                </div>
                <div class="centralized-info" style="flex: 1; overflow-y: auto; padding: 8px 0;">
                  <div class="description-and-time">
                    <div class="info-item description">
                      <div class="value">
                        <div
                          class="description-content"
                          :class="{ 'multiline': isMultiline }"
                          v-html="formattedIntroduction"
                        ></div>
                      </div>
                    </div>
                    <div class="info-item time-view">
                      <span class="value">{{ formatTime(picture.createTime) }}</span>
                      <span class="view-count"><i class="fas fa-eye"></i> {{ picture.viewCount || 0 }} 浏览</span>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 评论区 -->
              <div class="comment-section" style="height: 56vh; display: flex; flex-direction: column; flex-shrink: 0;">
                <div class="comment-header">
                  <h3>评论 ({{ picture.commentCount || 0 }})</h3>
                </div>
                <!-- 评论列表 -->
                <div class="comments-list" ref="scrollContainer" @scroll="handleScroll" style="flex: 1; overflow-y: auto; padding: 16px; background: var(--comment-list-background); border-bottom: 1px solid var(--border-color);">
                  <comment-list
                    :comments="comments"
                    @reply-clicked="handleReplyClick"
                    @update-comments="queryComments"
                  />
                  <div v-if="isEndOfData" class="end-message">没有更多评论了~</div>
                </div>
                <!-- 输入框栏 -->
                <div class="comment-input-bar">
                  <div class="input-wrapper">
                    <input
                      v-model="commentContent"
                      :placeholder="picture.allowComment ? (replyCommentId ? `回复 @${getReplyUserName()}: ${replyTargetContent}` : '说点什么...') : '主人关闭了评论功能'"
                      class="comment-input-field"
                      :disabled="!picture.allowComment"
                      @focus="isInputFocused = true"
                      @blur="isInputFocused = false"
                      @input="handleInputChange"
                      @keydown.enter.prevent="addComment"
                    >
                    <!-- 互动按钮（输入框未聚焦且无内容时显示） -->
                    <div class="input-actions" v-if="!isInputFocused && !commentContent.trim()">
                      <button
                        class="action-btn btn-like"
                        :class="{ 'is-liked': picture.isLiked === 1 }"
                        @click="doLike"
                      >
                        <i :class="picture.isLiked === 1 ? 'fas fa-heart' : 'far fa-heart'"></i>
                        <span>{{ picture.likeCount || 0 }}</span>
                      </button>
                      <button
                        class="action-btn btn-favorite"
                        :class="{ 'is-favorited': picture.isFavorited === 1 }"
                        @click="doFavorite"
                      >
                        <i :class="picture.isFavorited === 1 ? 'fas fa-star' : 'far fa-star'"></i>
                        <span>{{ picture.favoriteCount || 0 }}</span>
                      </button>
                      <button
                        class="action-btn btn-share"
                        :class="{ 'is-shared': picture.isShared === 1 }"
                        @click="doShare"
                      >
                        <i class="fas fa-share"></i>
                        <span>{{ picture.shareCount || 0 }}</span>
                      </button>
                      <!-- 更多按钮 -->
                      <button
                        class="action-btn btn-more-text"
                        @click.stop="handleShowMoreModal"
                      >
                        <span>更多</span>
                      </button>
                    </div>
                    <div class="input-actions" v-else>
                      <button
                        class="emoji-btn"
                        @click.stop="toggleEmojiPicker"
                        @mousedown.prevent
                      >
                        😊
                      </button>
                      <button
                        class="send-comment-btn"
                        :disabled="!commentContent.trim() || isAddingComment"
                        @click="addComment"
                      >
                        发送
                      </button>
                    </div>
                  </div>
                </div>
                <!-- 表情包选择器 -->
                <div v-if="showEmojiPicker" class="emoji-picker-wrapper" @mousedown.prevent>
                  <emoji-picker
                    class="custom-emoji-picker"
                    :i18n="emojiI18n"
                    @select="onEmojiSelect"
                  />
                </div>
              </div>

              <!-- 更多功能弹框 -->
              <div v-if="showMoreModal" class="more-modal show">
                <div class="modal-mask" @click.stop="showMoreModal = false"></div>
                <div class="modal-content">
                  <div class="modal-header">
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
                      <button class="close-btn" @click="showMoreModal = false">
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
                  <div class="modal-body">
                    <div class="action-buttons-container">
                      <div class="action-buttons">
                        <button class="action-btn" @click="openReportModal">
                          <i class="fas fa-flag"></i>
                          <span>举报</span>
                        </button>
                        <button v-if="showChatRoom" class="action-btn" @click="openChatModal">
                          <i class="fas fa-comments"></i>
                          <span>聊天室</span>
                        </button>
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
                        <button v-if="picture.isDownload !== 0" class="action-btn" @click="handleDownload">
                          <i class="fas fa-download"></i>
                          <span>下载</span>
                        </button>
                        <button v-else class="action-btn disabled" disabled>
                          <i class="fas fa-ban"></i>
                          <span>下载(不可用)</span>
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 举报模态框 -->
              <ReportModal
                ref="reportModalRef"
                :target-type="reportTargetType"
                :target-id="reportTargetId"
                @success="handleReportSuccess"
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
                      :picture-id="props.id"
                      :initial-permissions="{
                        allowLike: picture.allowLike ? 1 : 0,
                        allowComment: picture.allowComment ? 1 : 0,
                        allowCollect: picture.allowCollect ? 1 : 0,
                        allowShare: picture.allowShare ? 1 : 0
                      }"
                      @saved="() => { showPermissionSetting = false; fetchPictureDetail(); }"
                      @cancelled="() => showPermissionSetting = false"
                    />
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </template>

      <!-- 分享弹框 -->
      <ShareModal
        ref="shareModalRef"
        :link="shareLink"
        :imageUrl="picture.thumbnailUrl || picture.url"
        :title="picture.name"
        :user="picture.user"
        :createTime="picture.createTime"
      />

      <!-- 聊天室弹窗 -->
      <div v-if="showChatModal" class="chat-room-modal" :class="{ 'show': showChatModal }">
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

      <!-- 删除确认弹窗（新增样式） -->
      <div v-if="deleteConfirmVisible" class="delete-modal">
        <div class="delete-modal-mask" @click="deleteConfirmVisible = false"></div>
        <div class="delete-modal-content">
          <div class="delete-modal-icon">
            <i class="fas fa-triangle-exclamation"></i>
          </div>
          <h3 class="delete-modal-title">确认删除图片？</h3>
          <p class="delete-modal-desc">删除后无法恢复，请谨慎操作</p>
          <div class="delete-modal-actions">
            <button class="delete-modal-btn cancel" @click="deleteConfirmVisible = false">取消</button>
            <button class="delete-modal-btn confirm" @click="confirmDelete">确认删除</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick, onUnmounted, reactive, h } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { downloadImage, toHexColor } from '@/utils'
import { getDeviceType } from '@/utils/device'
import { DEVICE_TYPE_ENUM } from '@/constants/device'
import { SPACE_PERMISSION_ENUM } from '@/constants/space'
import ShareModal from '@/components/ShareModal.vue'
import PictureChatRoom from '@/components/PictureChatRoom.vue'
import { getDefaultAvatar } from '@/utils/userUtils'
import 'vant/lib/image-preview/style'
import { deletePictureUsingPost, getPictureVoByIdUsingGet } from '@/api/pictureController'
import { addUserFollowsUsingPost, findIsFollowUsingPost } from '@/api/userFollowsController'
import { message } from 'ant-design-vue'
import CommentList from '@/components/CommentList.vue'
import { addCommentUsingPost, queryCommentUsingPost } from '@/api/commentsController'
import EmojiPicker from '@/components/EmojiPicker.vue'
import { doLikeUsingPost } from '@/api/likeRecordController.ts'
import { throttle } from 'lodash-es'
import { doShareUsingPost } from '@/api/shareRecordController'
import { addFavoriteRecordUsingPost, cancelFavoriteUsingPost } from '@/api/favoriteRecordController'
import { formatTime } from '@/utils/dateUtils.ts'
import { ViewDurationTracker } from '@/utils/viewDurationTracker'
import ReportModal from '@/components/ReportModal.vue'
import ContentPermissionSetting from '@/components/ContentPermissionSetting.vue'

// 状态定义
const route = useRoute()
const device = ref<string>('')
const pictureLoaded = ref(false)    // 图片元数据就绪
const imgReady = ref(false)         // 原图加载完成
const pageReady = ref(false)        // 页面整体就绪
const mounted = ref(false)
const isDeleted = computed(() => pictureLoaded.value && (!picture.value || !picture.value.id))
// isFavorited 状态由后端返回，不需要单独定义
// const isFavorited = ref(false)
const infoSection = ref<HTMLElement | null>(null)
const infoSectionContent = ref<HTMLElement | null>(null)
const isMobile = ref(false)
const showMoreModal = ref(false)

// 处理显示更多弹窗
const handleShowMoreModal = () => {
  console.log('点击了更多按钮')
  showMoreModal.value = true
}
const showPermissionSetting = ref(false)
const reportModalRef = ref()
// 举报相关
const reportTargetType = ref('')
const reportTargetId = ref('')

// 浏览时长记录器
const viewDurationTracker = new ViewDurationTracker(1) // 1表示图片类型

// 初始化
onMounted(async () => {
  device.value = await getDeviceType()
  // 修改：平板也视为非移动端，保持左右布局
  isMobile.value = device.value === DEVICE_TYPE_ENUM.MOBILE && window.innerWidth < 768
  mounted.value = true

  // 1. 先拿图片元数据，骨架屏尽快切换为真实内容
  await fetchPictureDetail()
  pageReady.value = true

  // 2. 并发执行其余次要请求，总耗时 = max(T) 而非串行之和
  await Promise.all([
    checkIsFollowed(),
    updateLatestData(),
    queryComments()
  ])

  if (loginUserStore.loginUser) {
    nextTick(() => { showChatModal.value = false })
  }

  document.addEventListener('click', handleClickOutside)
  nextTick(() => checkDescriptionLines())
  window.addEventListener('resize', checkDescriptionLines)

  // 初始化浏览时长记录
  setTimeout(() => {
    if (picture.value.id && loginUserStore.loginUser?.id) {
      viewDurationTracker.init(picture.value.id, loginUserStore.loginUser.id)
    }
  }, 100)
})


// 销毁时清理
onUnmounted(() => {
  if (chatRoomRef.value) chatRoomRef.value.disconnect()
  document.removeEventListener('click', handleClickOutside)
  document.body.style.overflow = ''
  window.removeEventListener('resize', checkDescriptionLines)

  // 清理浏览时长记录
  viewDurationTracker.cleanup();
})

// 权限检查
const canEdit = computed(() => (picture.value.permissionList ?? []).includes(SPACE_PERMISSION_ENUM.PICTURE_EDIT))
const canDelete = computed(() => (picture.value.permissionList ?? []).includes(SPACE_PERMISSION_ENUM.PICTURE_DELETE))
const showShareButton = computed(() => true)
const showChatRoom = computed(() => showShareButton.value && !picture.value?.spaceId)

// Props定义
interface Props {
  id: string | number
}
const props = defineProps<Props>()
const picture = ref<API.PictureVO>({} as API.PictureVO)
const initialPictureUrl = ref<string>('')
const loginUserStore = useLoginUserStore()
const router = useRouter()

// 从路由获取图片数据
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

// 获取图片详情
const fetchPictureDetail = async () => {
  try {
    const routePictureData = getRoutePictureData()
    if (routePictureData) {
      picture.value = routePictureData
      initialPictureUrl.value = routePictureData.url
      onlineCount.value = routePictureData.chatCount || 0
      pictureLoaded.value = true // 数据已就绪，关闭骨架屏，开始显示模糊图
      return
    }

    const res = await getPictureVoByIdUsingGet({ id: props.id })
    if (res.data.code === 0 && res.data.data) {
      picture.value = res.data.data
      initialPictureUrl.value = res.data.data.url
      onlineCount.value = res.data.data.chatCount || 0
      pictureLoaded.value = true // 数据已就绪，关闭骨架屏
    } else {
      pictureLoaded.value = true // 即使失败也关闭骨架屏，后续走报错逻辑
    }
  } catch (e: any) {
    pictureLoaded.value = true
  }
}

// 更新最新数据
const updateLatestData = async () => {
  try {
    const res = await getPictureVoByIdUsingGet({ id: props.id })
    if (res.data.code === 0 && res.data.data) {
      const latestData = res.data.data
      const imageUrl = picture.value.url
      Object.keys(latestData).forEach(key => {
        if (key !== 'url' && key in picture.value) {
          (picture.value as any)[key] = latestData[key as keyof API.PictureVO]
        }
      })
      picture.value.url = imageUrl
      onlineCount.value = latestData.chatCount || 0
      nextTick(() => checkDescriptionLines())
    }
  } catch (e: any) {
    console.error('获取最新数据失败:', e)
  }
}

// 检查描述行数
const checkDescriptionLines = () => {
  const descriptionElements = document.querySelectorAll('.description-content')
  descriptionElements.forEach(element => {
    element.classList.remove('multiline')
    const computedStyle = window.getComputedStyle(element)
    const lineHeight = parseFloat(computedStyle.lineHeight)
    const height = element.offsetHeight
    const paddingTop = parseFloat(computedStyle.paddingTop)
    const paddingBottom = parseFloat(computedStyle.paddingBottom)
    const contentHeight = height - paddingTop - paddingBottom
    if (contentHeight > lineHeight * 1.5) {
      element.classList.add('multiline')
    }
  })
}


// 处理收藏操作
const doFavorite = async () => {
  if (!checkPermissionAndShowMessage(picture.value.allowCollect, '主人关闭了收藏功能')) {
    return
  }

  if (!loginUserStore.loginUser?.id) {
    message.warning('请先登录')
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

// 编辑图片
const doEdit = () => {
  router.push({
    path: '/add_picture',
    query: { id: picture.value.id, spaceId: picture.value.spaceId }
  })
}

// 删除确认
const deleteConfirmVisible = ref(false)
const showDeleteConfirm = () => {
  deleteConfirmVisible.value = true
  showMoreModal.value = false
}

// 确认删除
const confirmDelete = async () => {
  try {
    const res = await deletePictureUsingPost({ id: picture.value?.id })
    if (res.data.code === 0) {
      message.success('删除成功')
      deleteConfirmVisible.value = false
      showMoreModal.value = false
      router.back()
    } else {
      message.error('删除失败：' + res.data.message)
    }
  } catch (error: any) {
    message.error('删除失败：' + error.message)
  }
}

// 下载图片
const handleDownload = () => {
  if (picture.value.isDownload === 0) {
    message.warning({
      content: '抱歉，该图片暂不支持下载',
      icon: h('i', { class: 'fas fa-lock', style: `color: var(--nav-item-active-text); margin-right: 8px;` }),
      class: 'custom-message'
    })
    return
  }

  downloadImage(picture.value.url, picture.value.name || '图片')

  message.success({
    content: '开始下载...',
    icon: h('i', { class: 'fas fa-download', style: `color: var(--link-color); margin-right: 8px;` })
  })
}

// 分享相关
const shareModalRef = ref()
const shareLink = computed(() => picture.value?.id ? `${window.location.origin}/picture/${picture.value.id}` : '')
const doShare = async () => {
  if (!checkPermissionAndShowMessage(picture.value.allowShare, '主人关闭了分享功能')) {
    return
  }

  if (!loginUserStore.loginUser?.id) {
    message.warning('请先登录')
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

// 关注相关
const isFollowed = ref(false)
const checkIsFollowed = async () => {
  if (!loginUserStore.loginUser?.id || !picture.value?.user?.id) return
  try {
    const res = await findIsFollowUsingPost({
      followerId: loginUserStore.loginUser.id,
      followingId: picture.value.user.id
    })
    if (res.data?.data) isFollowed.value = res.data.data
  } catch (error) {
    console.error('检查关注状态失败', error)
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
    if (res.data?.code === 0) isFollowed.value = !isFollowed.value
  } catch (error) {
    message.error('操作失败')
  }
}

// 聊天室相关
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
    showMoreModal.value = false
  } else {
    message.warning('请先登录')
  }
}

// 用户点击跳转
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

// 数字格式化
const formatNumber = (num: number) => {
  if (!num) return '0'
  if (num >= 10000) return (num / 10000).toFixed(1) + 'w'
  if (num >= 1000) return (num / 1000).toFixed(1) + 'k'
  return num.toString()
}

// 图片加载完成 → 触发原图淡入
const handleImageLoad = () => {
  imgReady.value = true
  pictureLoaded.value = true
}

// 评论相关
const comments = ref<API.Comment[]>([])
const commentContent = ref('')
const isInputFocused = ref(false)
const replyCommentId = ref('')
const replyTargetContent = ref('')
const commentloading = ref(false)
const showEmojiPicker = ref(false)
const isEndOfData = ref(false)
const queryRequest = reactive<API.CommentsQueryRequest>({
  targetId: props.id,
  targetType: 1,
  current: 1,
  pageSize: 15
})

// 查询评论
const queryComments = async () => {
  try {
    if (queryRequest.current > 1) commentloading.value = true
    const res = await queryCommentUsingPost(queryRequest)
    if (res.data?.data != null) {
      const newComments = res.data.data.records.map(comment => ({
        ...comment,
        commentId: comment.commentId?.toString(),
        parentCommentId: comment.parentCommentId?.toString()
      }))
      if (queryRequest.current === 1) {
        comments.value = newComments
      } else {
        comments.value = [...comments.value, ...newComments]
      }
      isEndOfData.value = newComments.length < queryRequest.pageSize
    } else {
      if (queryRequest.current === 1) comments.value = []
      isEndOfData.value = true
    }
  } catch (error) {
    console.error('查询评论异常', error)
    message.error('获取评论失败')
  } finally {
    commentloading.value = false
  }
}

// 添加评论
const isAddingComment = ref(false)

const addComment = async () => {
  if (!checkPermissionAndShowMessage(picture.value.allowComment, '主人关闭了评论功能')) {
    return
  }

  // 防抖处理：如果正在提交评论，则直接返回
  if (isAddingComment.value) {
    return
  }

  if (!commentContent.value.trim()) {
    message.warning('评论内容不能为空')
    return
  }

  // 设置防抖标识为 true，防止重复提交
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
        if (scrollContainer.value) scrollContainer.value.scrollTop = 0
      })
    }
  } catch (error) {
    message.error('评论失败')
  } finally {
    // 无论成功或失败，都要重置防抖标识
    isAddingComment.value = false
  }
}

// 输入框变化
const handleInputChange = () => {
  if (commentContent.value.trim().length === 0) showEmojiPicker.value = false
}

// 表情包选择
const toggleEmojiPicker = () => {
  console.log('Emoji button clicked');
  showEmojiPicker.value = !showEmojiPicker.value
}
const onEmojiSelect = (emoji: string) => {
  commentContent.value += emoji

  // 选择表情后保持输入框聚焦并设置光标位置
  nextTick(() => {
    const inputEl = document.querySelector('.comment-input-field') as HTMLInputElement
    if (inputEl) {
      inputEl.focus()
      // 将光标移到文本末尾
      inputEl.setSelectionRange(inputEl.value.length, inputEl.value.length)
    }
  })
}
const handleClickOutside = (event: MouseEvent) => {
  const target = event.target as HTMLElement
  // 检查点击的目标是否在表情选择器内部，包括表情按钮、表情面板和表情本身
  if (!target.closest('.emoji-btn') && !target.closest('.emoji-picker-wrapper') && !target.closest('.emoji-mart')) {
    showEmojiPicker.value = false
  }
  // 点击空白处取消回复状态，但排除回复按钮的点击
  if (replyCommentId.value && !target.closest('.comment-input-field') && !target.closest('.comment-input-bar') && !target.closest('.action-item')) {
    cancelReply()
  }
}

// 回复评论
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
    inputEl?.focus()
    inputEl?.scrollIntoView({ behavior: 'smooth', block: 'end' })
  })
}
const cancelReply = () => {
  replyCommentId.value = ''
  replyTargetContent.value = ''
}

// 获取被回复用户的用户名
const getReplyUserName = () => {
  // 查找被回复的评论对象
  const findComment = (comments: API.Comment[]) => {
    for (const comment of comments) {
      if (comment.commentId === replyCommentId.value) {
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
  return foundComment?.commentUser?.userName || '用户'
}

// 表情包国际化
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

// 加载更多评论
const handleScroll = throttle(() => {
  const container = scrollContainer.value
  if (!container || commentloading.value || isEndOfData.value) return
  const { scrollTop, clientHeight, scrollHeight } = container
  if (scrollTop + clientHeight >= scrollHeight - 100) {
    const oldScrollHeight = scrollHeight
    const oldScrollTop = scrollTop
    loadMoreComments(oldScrollHeight, oldScrollTop)
  }
}, 200)
const loadMoreComments = async (oldScrollHeight: number, oldScrollTop: number) => {
  if (commentloading.value || isEndOfData.value) return
  commentloading.value = true
  queryRequest.current++
  await queryComments()
  const container = scrollContainer.value
  if (container) {
    setTimeout(() => {
      container.scrollTop = oldScrollTop
    }, 0)
  }
  commentloading.value = false
}
const scrollContainer = ref<HTMLElement | null>(null)

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

// 点赞操作
const doLike = async () => {
  if (!checkPermissionAndShowMessage(picture.value.allowLike, '主人关闭了点赞功能')) {
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
        picture.value.likeCount = String(Number(picture.value.likeCount || 0) + 1)
        picture.value.isLiked = 1
      } else {
        picture.value.likeCount = String(Number(picture.value.likeCount || 0) - 1)
        picture.value.isLiked = 0
      }
    }
  } catch (error) {
    message.error('操作异常')
  }
}

// 图片预览
const showPreview = ref(false)
const handlePreviewChange = (visible: boolean) => {
  showPreview.value = visible
  document.body.style.overflow = visible ? 'hidden' : ''
}
const showImagePreview = () => {
  if (isMobile.value) showPreview.value = true
  else handlePreviewChange(true)
}
const closeImagePreview = () => {
  if (isMobile.value) showPreview.value = false
  else handlePreviewChange(false)
}

// 主色调计算
const dominantColor = computed(() => picture.value?.picColor ? toHexColor(picture.value.picColor) : '#4a5568')
function adjustColor(hex: string, percent: number) {
  hex = hex.replace('#', '').padStart(6, '0')
  const r = parseInt(hex.substring(0, 2), 16)
  const g = parseInt(hex.substring(2, 4), 16)
  const b = parseInt(hex.substring(4, 6), 16)
  const adjustValue = (value: number) => Math.min(255, Math.max(0, Math.round(value + (value * (percent / 100)))))
  const rr = adjustValue(r).toString(16).padStart(2, '0')
  const gg = adjustValue(g).toString(16).padStart(2, '0')
  const bb = adjustValue(b).toString(16).padStart(2, '0')
  return `#${rr}${gg}${bb}`
}

// 介绍格式化
const formattedIntroduction = computed(() => {
  const intro = picture.value.introduction || '暂无介绍'
  return intro.split('\n').map(paragraph => `<p>${paragraph}</p>`).join('')
})
const isMultiline = computed(() => {
  const intro = picture.value.introduction || ''
  return intro.includes('\n') || intro.length > 50
})

// 聚焦评论输入框
const focusCommentInput = () => {
  nextTick(() => {
    const inputEl = document.querySelector('.comment-input-field') as HTMLInputElement
    inputEl?.focus()
  })
}

// 打开举报模态框
const openReportModal = () => {
  if (reportModalRef.value) {
    // 设置举报目标类型和ID
    reportTargetType.value = '1'  // 图片类型
    reportTargetId.value = picture.value.id.toString()

    // 传递参数并打开模态框
    reportModalRef.value.openModal()
  }
  showMoreModal.value = false
}

// 举报成功回调
const handleReportSuccess = () => {
  console.log('举报提交成功')
}
</script>

<style scoped>
.picture-detail {
  position: fixed;
  inset: 0;
  height: 100vh;
  overflow: hidden;
  color: var(--text-primary);
  font-family: var(--font-family-base);
  z-index: 100;
  background: var(--background);
}

.welcome-layer {
  position: fixed;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--card-background) 0%, var(--background) 100%);
  z-index: 1000;
  transition: opacity 0.3s ease;
}
.welcome-content {
  text-align: center;
}
.emoji-row {
  font-size: 32px;
  margin: 20px;
  opacity: 0;
  animation: emojiAppear 0.25s ease-out forwards;
}
.emoji-row:nth-child(1) { animation-delay: 0.05s; }
.emoji-row:nth-child(2) { animation-delay: 0.15s; }
@keyframes emojiAppear {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

.background-effects {
  position: absolute;
  inset: 0;
  z-index: 1;
  overflow: hidden;
}
.color-blur-layer {
  position: absolute;
  inset: 0;
  opacity: 0.4;
  filter: blur(100px);
  transition: opacity 0.5s ease, background 0.6s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}
.blurred-image-layer {
  position: absolute;
  inset: 0;
  background-position: center;
  background-size: cover;
  background-repeat: no-repeat;
  filter: blur(22px) brightness(0.85) saturate(1.05) contrast(1.1);
  opacity: 0;
  transform: scale(1.1);
  transition: opacity 0.75s cubic-bezier(0.25, 0.46, 0.45, 0.94), filter 0.5s ease, transform 0.6s ease;
  background-blend-mode: soft-light;
}
.picture-detail.is-loaded .blurred-image-layer {
  opacity: 0.55;
}

.content-layer {
  position: relative;
  z-index: 2;
  height: 100%;
  opacity: 0;
  transition: opacity 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}
.content-layer.is-loaded {
  opacity: 1;
}

.layout {
  display: flex;
  height: 100%;
  gap: 0;
  flex-wrap: nowrap;
}

.preview-section {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  min-width: 300px;
  opacity: 0;
  transition: opacity 0.4s ease 0.1s;
}
.preview-section.is-loaded {
  opacity: 1;
}
.image-wrapper {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}
.preview-image {
  max-width: 88%;
  max-height: 88vh;
  object-fit: contain;
  border-radius: 18px;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.25), 0 2px 8px rgba(0, 0, 0, 0.12);
  cursor: zoom-in;
  opacity: 0;
  transform: scale(0.97) translateY(4px);
  transition:
    transform 0.5s cubic-bezier(0.34, 1.56, 0.64, 1),
    box-shadow 0.3s ease,
    opacity 0.5s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}
.image-wrapper.is-loaded .preview-image {
  opacity: 1;
  transform: scale(1) translateY(0);
}
.preview-image:hover {
  transform: scale(1.015);
  box-shadow: 0 16px 60px rgba(0, 0, 0, 0.3), 0 4px 16px rgba(0, 0, 0, 0.12);
}

.progressive-image-wrapper {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}
.blur-placeholder {
  max-width: 90%;
  max-height: 90vh;
  object-fit: contain;
  filter: blur(10px);
  transform: scale(1.02);
  opacity: 0;
  transition: opacity 0.3s ease, filter 0.4s ease;
}
.image-wrapper.is-loading .blur-placeholder {
  opacity: 1;
}

.loading-state {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 1;
  transition: opacity 0.2s ease;
}
.image-wrapper.is-loaded .loading-state {
  opacity: 0;
  pointer-events: none;
}
.modern-loader {
  display: flex;
  gap: 12px;
}
.loader-circle {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: var(--link-color);
  animation: loaderPulse 1.4s infinite ease-in-out both;
}
.loader-circle:nth-child(1) { animation-delay: -0.32s; }
.loader-circle:nth-child(2) { animation-delay: -0.16s; }
@keyframes loaderPulse {
  0%, 80%, 100% { transform: scale(0); opacity: 0.5; }
  40% { transform: scale(1); opacity: 1; }
}

.info-section {
  width: 460px;
  background: var(--card-background);
  opacity: 0;
  border-left: 1px solid var(--border-color);
  border-top-left-radius: 24px;
  border-bottom-left-radius: 24px;
  padding: 20px 20px 0 20px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
  flex-shrink: 0;
  min-width: 400px;
  transform: translateX(24px);
  transition:
    opacity 0.5s cubic-bezier(0.25, 0.46, 0.45, 0.94) 0.15s,
    transform 0.5s cubic-bezier(0.34, 1.12, 0.64, 1) 0.15s;
  /* 超细滚动条 */
  scrollbar-width: thin;
  scrollbar-color: transparent transparent;
}
.info-section:hover {
  scrollbar-color: var(--border-color) transparent;
}
.content-layer.is-loaded .info-section {
  opacity: 1;
  transform: translateX(0);
}
.info-section-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
}

.author-info-card {
  background: var(--hover-background);
  border-radius: 12px;
  padding: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  transition: all 0.15s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  opacity: 0;
  transform: translateY(10px);
  animation: cardAppear 0.25s ease forwards 0.3s;
}
@keyframes cardAppear {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
.author-info-card:hover {
  background: var(--comment-item-hover);
  transform: translateY(-2px);
}
.author-header {
  display: flex;
  align-items: center;
  gap: 12px;
}
.author-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.15s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  position: relative;
}


.author-avatar:hover {
  transform: scale(1.05);
}
.author-details {
  flex: 1;
}
.author-name {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: color 0.15s ease;
  color: var(--text-primary);
}
.author-name:hover {
  color: var(--link-color);
}
.btn-follow {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
  background: var(--link-color);
  color: var(--text-other);
  border: none;
  cursor: pointer;
  transition: all 0.15s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  min-width: 80px;
  justify-content: center;
}
.btn-follow.is-followed {
  background: var(--border-color);
  color: var(--text-secondary);
}
.btn-follow.is-followed:hover {
  background: var(--hover-background);
}
.btn-follow:active {
  transform: translateY(-1px);
}

.info-card {
  background: var(--hover-background);
  border-radius: 16px;
  overflow: hidden;
  opacity: 0;
  transform: translateY(12px);
  animation: cardAppear 0.35s cubic-bezier(0.34, 1.2, 0.64, 1) forwards 0.3s;
  border: 1px solid var(--border-color);
}
.card-header {
  padding: 14px 16px;
  border-bottom: 1px solid var(--border-color);
}
.card-header h4 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.title-header {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  min-height: 48px;
}

.title-avatar-container {
  position: relative;
  display: inline-block;
  flex-shrink: 0;
}

.title-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
  transition:
    transform 0.25s cubic-bezier(0.34, 1.56, 0.64, 1),
    box-shadow 0.25s ease;
  position: relative;
  box-shadow: 0 0 0 2px transparent;
}

.title-avatar:hover {
  transform: scale(1.08);
  box-shadow: 0 0 0 3px var(--link-color);
}

.title-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.btn-follow-overlay {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #141414;
  color: white;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
  transform: translate(20%, 20%);
}

.btn-follow-overlay.is-followed {
  background: #ec4141;
}

.btn-follow-overlay.is-followed:hover {
  background: #ff2d55;
}

.btn-follow-overlay:active {
  transform: translate(20%, 20%) scale(0.9);
}

.btn-follow-overlay i {
  font-size: 10px;
  margin: 0;
  padding: 0;
}

.centralized-info {
  padding: 16px;
}
.description-and-time {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.description-content {
  font-size: 14px;
  line-height: 1.6;
  color: var(--text-primary);
  opacity: 0;
  animation: fadeIn 0.4s ease forwards 0.4s;
}
@keyframes fadeIn {
  to { opacity: 1; }
}
.time-view {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: var(--text-secondary);
  opacity: 0;
  animation: fadeIn 0.4s ease forwards 0.45s;
}
.view-count {
  display: flex;
  align-items: center;
  gap: 4px;
}

.comment-section {
  background: var(--hover-background);
  border-radius: 16px;
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  opacity: 0;
  transform: translateY(12px);
  animation: cardAppear 0.35s cubic-bezier(0.34, 1.2, 0.64, 1) forwards 0.4s;
  border: 1px solid var(--border-color);
  overflow: hidden;
}
.comment-header {
  padding: 14px 16px;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;
}
.comment-header h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  letter-spacing: 0.01em;
}
.comments-list {
  flex: 1;
  overflow-y: auto;
  border-bottom: 1px solid var(--border-color);
  /* 超细滚动条 */
  scrollbar-width: thin;
  scrollbar-color: transparent transparent;
}
.comments-list:hover {
  scrollbar-color: var(--border-color) transparent;
}

.comment-input-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px 12px;
  background: var(--card-background);
  border-top: 1px solid var(--border-color);
  flex-shrink: 0;
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
  transition: transform 0.1s ease;
  opacity: 0;
  animation: fadeIn 0.4s ease forwards 0.5s;
}
.input-user-avatar:hover {
  transform: scale(1.05);
}
.comment-input-field {
  flex: 1;
  padding: 8px 14px;
  background: var(--comment-input-background);
  border: 1.5px solid transparent;
  border-radius: 20px;
  font-size: 13.5px;
  outline: none;
  transition:
    background 0.2s ease,
    border-color 0.2s ease,
    box-shadow 0.2s ease;
  color: var(--comment-input-text);
  min-width: 0;
  opacity: 0;
  animation: fadeIn 0.4s ease forwards 0.55s;
}
.comment-input-field:disabled {
  cursor: not-allowed;
  opacity: 0.5;
  background: var(--hover-background);
}
.comment-input-field::placeholder {
  color: var(--comment-input-placeholder);
  font-size: 13px;
}
.comment-input-field:focus {
  background: var(--hover-background);
  border-color: var(--link-color);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--link-color) 15%, transparent);
}

.emoji-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  border-radius: 50%;
  font-size: 17px;
  cursor: pointer;
  transition: transform 0.2s cubic-bezier(0.34, 1.56, 0.64, 1), background 0.15s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: var(--text-primary);
  opacity: 1;
}
.emoji-btn:hover {
  background: var(--hover-background);
  transform: scale(1.15);
}

.input-actions {
  display: flex;
  gap: 4px;
  align-items: center;
  opacity: 1;
  transition: none;
}
.action-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  background: transparent;
  border: none;
  cursor: pointer;
  transition:
    color 0.2s ease,
    transform 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
  color: var(--interaction-button-color);
  padding: 4px 8px;
  border-radius: 8px;
}
.action-btn:active {
  transform: scale(0.88) !important;
}
.action-btn.btn-like:hover,
.action-btn.btn-like.is-liked {
  color: var(--like-button-active-color);
  transform: scale(1.08);
}
.action-btn.btn-like.is-liked i {
  animation: heartBeat 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}
@keyframes heartBeat {
  0% { transform: scale(1); }
  30% { transform: scale(1.35); }
  60% { transform: scale(0.92); }
  100% { transform: scale(1); }
}
.action-btn.btn-favorite:hover,
.action-btn.btn-favorite.is-favorited {
  color: #FFD700;
  transform: scale(1.08);
}
.action-btn.btn-share:hover,
.action-btn.btn-share.is-shared {
  color: var(--link-color);
  transform: scale(1.08);
}
.action-btn.btn-more-text {
  background: var(--link-color);
  color: var(--text-other);
  padding: 5px 12px;
  border-radius: 20px;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
  border: none;
  transition:
    background 0.2s ease,
    transform 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
  font-weight: 500;
}
.action-btn.btn-more-text:hover {
  background: var(--link-hover-color);
  color: var(--text-other);
  transform: scale(1.08);
}
.action-btn i {
  font-size: 16px;
  transition: transform 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.action-btn:hover i {
  transform: scale(1.1);
}
.action-btn span {
  font-size: 12px;
  font-weight: 500;
}

.send-comment-btn {
  padding: 7px 18px;
  border: none;
  border-radius: 20px;
  background-color: var(--link-color);
  color: var(--text-other);
  font-size: 13.5px;
  font-weight: 500;
  cursor: pointer;
  white-space: nowrap;
  opacity: 1;
  transition:
    background 0.2s ease,
    transform 0.25s cubic-bezier(0.34, 1.56, 0.64, 1),
    box-shadow 0.2s ease;
  letter-spacing: 0.02em;
}
.send-comment-btn:disabled {
  background-color: var(--border-color);
  color: var(--text-secondary);
  cursor: not-allowed;
  transform: none;
}
.send-comment-btn:not(:disabled):hover {
  background-color: var(--link-hover-color);
  transform: scale(1.06);
  box-shadow: 0 4px 12px color-mix(in srgb, var(--link-color) 35%, transparent);
}
.send-comment-btn:not(:disabled):active {
  transform: scale(0.94);
}

.input-wrapper {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 4px;
  position: relative;
  min-width: 0;
}

.emoji-picker-wrapper {
  position: absolute;
  bottom: 60px;
  right: 20px;
  z-index: 10;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 4px 12px var(--shadow-color);
  background: var(--emoji-picker-background);
  border: 1px solid var(--emoji-picker-border);
  opacity: 1;
  transform: translateY(0) scale(1);
  transition: all 0.15s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}
.emoji-picker-wrapper.show {
  opacity: 1;
  transform: translateY(0) scale(1);
  display: block;
}

.more-modal {
  position: fixed;
  inset: 0;
  z-index: 9998;
  opacity: 0;
  transition: opacity 0.15s ease;
  pointer-events: none;
}
.more-modal.show {
  opacity: 1;
  pointer-events: auto;
}
.modal-mask {
  position: absolute;
  inset: 0;
  background: var(--comment-drawer-backdrop);
  opacity: 0;
  transition: opacity 0.15s ease;
}
.more-modal.show .modal-mask {
  opacity: 1;
}
.modal-content {
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
.more-modal.show .modal-content {
  opacity: 1;
  transform: translate(-50%, -50%) scale(1);
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
  transition: all 0.1s ease;
  color: var(--text-primary);
}
.close-btn:hover {
  background: var(--hover-background);
  transform: scale(1.1);
}
.picture-info {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}
.picture-thumb {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 8px;
  transition: transform 0.15s ease;
}
.picture-thumb:hover {
  transform: scale(1.05);
}
.picture-details {
  flex: 1;
}
.picture-title {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 8px;
  color: var(--text-primary);
}
.picture-stats span {
  font-size: 12px;
  color: var(--text-secondary);
  margin-right: 12px;
  transition: color 0.1s ease;
}
.picture-stats span:hover {
  color: var(--link-color);
}
.picture-details-info {
  background: var(--hover-background);
  border-radius: 8px;
  padding: 12px;
}
.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 14px;
  color: var(--text-primary);
  opacity: 0;
  animation: fadeIn 0.15s ease forwards;
}
.info-item:nth-child(1) { animation-delay: 0.1s; }
.info-item:nth-child(2) { animation-delay: 0.15s; }
.info-item:nth-child(3) { animation-delay: 0.2s; }
.info-item:last-child {
  margin-bottom: 0;
}
.label {
  color: var(--text-secondary);
  min-width: 50px;
}
.value {
  color: var(--text-primary);
  flex: 1;
}
.tag-item {
  display: inline-block;
  margin-right: 4px;
  color: var(--link-color);
  transition: all 0.1s ease;
}
.tag-item:hover {
  color: var(--link-hover-color);
  transform: scale(1.05);
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
  border: 1px solid var(--text-other);
  box-shadow: 0 2px 4px var(--shadow-color);
  transition: transform 0.15s ease;
}
.color-dot:hover {
  transform: scale(1.2);
}
.modal-body {
  padding: 20px;
}
.action-buttons {
  display: flex;
  justify-content: flex-start;
  gap: 16px;
  overflow-x: auto;
  -ms-overflow-style: auto;
  padding-bottom: 8px;
}

.action-buttons::-webkit-scrollbar {
  display: block;
  height: 6px;
}

.action-buttons::-webkit-scrollbar-track {
  background: var(--hover-background);
  border-radius: 3px;
}

.action-buttons::-webkit-scrollbar-thumb {
  background: var(--border-color);
  border-radius: 3px;
}

.action-buttons::-webkit-scrollbar-thumb:hover {
  background: var(--text-secondary);
}

.action-buttons-container {
  width: 100%;
  overflow-x: auto;
  padding: 0 10px 10px 10px;
  -ms-overflow-style: none;
}

.action-buttons-container::-webkit-scrollbar {
  display: none;
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
  transition: all 0.15s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  flex: 0 0 auto;
  color: var(--text-primary);
  opacity: 0;
  animation: fadeIn 0.15s ease forwards;
  width: 76px;
}
.modal-body .action-btn:nth-child(1) { animation-delay: 0.1s; }
.modal-body .action-btn:nth-child(2) { animation-delay: 0.15s; }
.modal-body .action-btn:nth-child(3) { animation-delay: 0.2s; }
.modal-body .action-btn:nth-child(4) { animation-delay: 0.25s; }
.modal-body .action-btn:nth-child(5) { animation-delay: 0.3s; }
.modal-body .action-btn:hover:not(.disabled) {
  background: var(--comment-item-hover);
  transform: translateY(-2px) scale(1.05);
}
.modal-body .action-btn.disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}
.modal-body .action-btn i {
  font-size: 24px;
  margin-bottom: 4px;
  transition: transform 0.15s ease;
}
.modal-body .action-btn:hover:not(.disabled) i {
  transform: scale(1.2);
}
.modal-body .action-btn span {
  font-size: 12px;
}

.chat-room-modal {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: var(--comment-drawer-backdrop);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.15s ease;
  pointer-events: none;
}
.chat-room-modal.show {
  opacity: 1;
  pointer-events: auto;
}
.chat-room-content {
  background: var(--chat-background);
  width: 90%;
  max-width: 600px;
  max-height: 80vh;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 20px var(--shadow-color);
  border: 1px solid var(--chat-header-border);
  transform: scale(0.95);
  transition: transform 0.2s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  opacity: 0;
}
.chat-room-modal.show .chat-room-content {
  transform: scale(1);
  opacity: 1;
}
.chat-room-header {
  padding: 16px;
  border-bottom: 1px solid var(--chat-header-border);
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: var(--chat-header-background);
}
.chat-room-title h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--chat-header-text);
}
.online-count {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 4px;
}
.btn-close {
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
  color: var(--chat-header-text);
}
.btn-close:hover {
  background: var(--hover-background);
  transform: scale(1.1);
}
.chat-room-body {
  height: calc(80vh - 60px);
  overflow-y: auto;
  background: var(--chat-background);
}

.delete-modal {
  position: fixed;
  inset: 0;
  z-index: 1001;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.15s ease;
  pointer-events: none;
}
.delete-modal.show {
  opacity: 1;
  pointer-events: auto;
}
.delete-modal-mask {
  position: absolute;
  inset: 0;
  background: var(--comment-drawer-backdrop);
  opacity: 0;
  transition: opacity 0.15s ease;
}
.delete-modal.show .delete-modal-mask {
  opacity: 1;
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
  transform: scale(0.95);
  opacity: 0;
  transition: all 0.2s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}
.delete-modal.show .delete-modal-content {
  transform: scale(1);
  opacity: 1;
}
.delete-modal-icon {
  font-size: 40px;
  color: var(--nav-item-active-text);
  margin-bottom: 16px;
  opacity: 0;
  animation: fadeIn 0.25s ease forwards 0.1s;
}
.delete-modal-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 8px;
  color: var(--text-primary);
  opacity: 0;
  animation: fadeIn 0.25s ease forwards 0.15s;
}
.delete-modal-desc {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 24px;
  opacity: 0;
  animation: fadeIn 0.25s ease forwards 0.2s;
}
.delete-modal-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  opacity: 0;
  animation: fadeIn 0.25s ease forwards 0.25s;
}
.delete-modal-btn {
  padding: 8px 24px;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.1s ease;
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
  transform: scale(1.05);
}
.delete-modal-btn.confirm:hover {
  background: var(--like-button-hover-color);
  transform: scale(1.05);
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
  padding: 20px;
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

::-webkit-scrollbar {
  width: 3px;
}
::-webkit-scrollbar-track {
  background: transparent;
}
::-webkit-scrollbar-thumb {
  background: transparent;
  border-radius: 4px;
  transition: background 0.2s ease;
}
:hover::-webkit-scrollbar-thumb {
  background: var(--border-color);
}
::-webkit-scrollbar-thumb:hover {
  background: var(--text-secondary) !important;
}

@keyframes actionAnimation {
  0% { transform: scale(1); }
  40% { transform: scale(1.3); }
  70% { transform: scale(0.9); }
  100% { transform: scale(1); }
}
.action-btn.is-liked i {
  animation: heartBeat 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.action-btn.is-shared i {
  animation: actionAnimation 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.deleted-view {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  background: var(--background);
  opacity: 0;
  animation: fadeIn 0.4s ease forwards 0.15s;
}
.deleted-content {
  text-align: center;
  padding: 40px;
  background: var(--card-background);
  border-radius: 16px;
  box-shadow: 0 4px 20px var(--shadow-color);
  border: 1px solid var(--border-color);
  transform: scale(0.95);
  animation: scaleIn 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94) forwards 0.25s;
  opacity: 0;
}
@keyframes scaleIn {
  to {
    opacity: 1;
    transform: scale(1);
  }
}
.deleted-content i {
  font-size: 48px;
  color: var(--like-button-active-color);
  margin-bottom: 20px;
  transform: scale(0);
  animation: popIn 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94) forwards 0.35s;
}
@keyframes popIn {
  to { transform: scale(1); }
}
.deleted-content h2 {
  font-size: 24px;
  color: var(--text-primary);
  margin-bottom: 12px;
  opacity: 0;
  animation: fadeIn 0.4s ease forwards 0.4s;
}
.deleted-content p {
  font-size: 16px;
  color: var(--text-secondary);
  margin-bottom: 30px;
  opacity: 0;
  animation: fadeIn 0.4s ease forwards 0.45s;
}
.btn-primary {
  padding: 10px 24px;
  background: var(--link-color);
  color: var(--text-other);
  border: none;
  border-radius: 8px;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.1s ease;
  opacity: 0;
  animation: fadeIn 0.4s ease forwards 0.5s;
}
.btn-primary:hover {
  background: var(--link-hover-color);
  transform: scale(1.05);
}

.end-message {
  text-align: center;
  padding: 16px;
  color: var(--text-secondary);
  font-size: 14px;
  opacity: 0;
  animation: fadeIn 0.4s ease forwards 0.25s;
}

.multiline {
  line-height: 1.8;
  transition: all 0.15s ease;
}

.progressive-image-wrapper {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}
.blur-placeholder {
  max-width: 90%;
  max-height: 90vh;
  object-fit: contain;
  filter: blur(10px);
  transform: scale(1.02);
  opacity: 1; /* 默认显示模糊占位 */
  transition: opacity 0.5s ease-out, filter 0.5s ease;
  position: absolute;
}
.image-wrapper.is-loaded .blur-placeholder {
  opacity: 0;
  pointer-events: none;
}
.preview-image {
  opacity: 0; /* 默认隐藏原图 */
  transition: opacity 0.4s ease-in;
}
.preview-image.img-visible {
  opacity: 1; /* 加载完毕淡入 */
}

/* 骨架屏占位样式 */
.image-skeleton {
  width: 100%;
  height: 60vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--component-bg, #f5f5f5);
  border-radius: 12px;
}
.info-skeleton {
  padding: 24px;
}

/* 骨架屏占位样式 */
.picture-detail {
  min-height: 100vh;
}
</style>
