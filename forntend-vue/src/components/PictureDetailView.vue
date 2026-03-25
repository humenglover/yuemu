<template>
  <Teleport to="body">
    <Transition name="fade">
      <div v-if="visible" class="picture-modal-overlay" @click.self="handleClose">
        <!-- 背景虚化层 -->
        <div
          class="modal-backdrop-blur"
          :style="{ backgroundImage: picture.url ? `url(${picture.url})` : 'none' }"
        ></div>

        <div class="modal-container" :class="{ 'is-mobile': isMobile }">
          <!-- 关闭按钮 -->
          <button class="modal-close-btn" @click="handleClose" title="关闭">
            <i class="fas fa-times"></i>
          </button>

          <div class="modal-content-wrapper" @click.stop>
            <!-- 左侧：图片预览区 -->
            <div class="preview-section" :style="{ backgroundColor: dominantColor + '22' }">
              <div v-if="!imgReady" class="img-skeleton">
                <div class="shimmer"></div>
              </div>
              <div class="image-container" :class="{ 'ready': imgReady }">
                <!-- 底部虚化背景让容器更丰满 -->
                <img
                  v-if="picture.url"
                  :src="picture.url"
                  class="bg-fill-blur"
                  alt=""
                />
                <img
                  v-if="picture.url"
                  :src="picture.url"
                  :alt="picture.name"
                  class="main-image"
                  @load="handleImageLoad"
                  @click="showImagePreview"
                />
              </div>

              <!-- 图片底部工具条 (点赞、收藏快捷方式，可选) -->
              <div class="image-overlay-tools" v-if="!isMobile">
                <div class="tool-item like" :class="{ active: picture.isLiked === 1 }" @click="doLike" title="点赞">
                  <i class="fas fa-heart"></i>
                </div>
                <div class="tool-item favorite" :class="{ active: picture.isFavorited === 1 }" @click="doFavorite" title="收藏">
                  <i class="fas fa-star"></i>
                </div>
              </div>
            </div>

            <!-- 右侧：详情滚动区 -->
            <div class="details-section">
              <div class="details-inner">
                <!-- 用户信息 -->
                <div class="author-card">
                  <div class="author-info" @click="handleUserClick(picture.user)">
                    <img
                      :src="picture.user?.userAvatar || getDefaultAvatar(picture.user?.userName)"
                      class="author-avatar"
                      alt=""
                    />
                    <div class="author-meta">
                      <div class="name-row">
                        <span class="author-name">{{ picture.user?.userName || '匿名用户' }}</span>
                        <!-- 关注按钮移到名字旁边 -->
                        <button
                          v-if="picture.user?.id !== loginUserStore.loginUser?.id"
                          class="follow-btn-inline"
                          :class="{ 'followed': isFollowed }"
                          @click.stop="handleFollow"
                        >
                          {{ isFollowed ? '已关注' : '关注' }}
                        </button>
                      </div>
                      <span class="publish-time">{{ formatTime(picture.createTime) }}</span>
                    </div>
                  </div>
                </div>

                <!-- 标题与描述 -->
                <div class="description-card">
                  <h1 class="pic-title">{{ picture.name || '未命名图片' }}</h1>
                  <div class="pic-intro" v-html="formattedIntroduction"></div>
                  <div class="pic-tags" v-if="picture.tags?.length">
                    <span v-for="tag in picture.tags" :key="tag" class="tag-item">#{{ tag }}</span>
                  </div>
                </div>

                <!-- 统计信息 -->
                <div class="stats-bar">
                  <span class="stat-item"><i class="fas fa-eye"></i> {{ picture.viewCount || 0 }} 浏览</span>
                  <span class="stat-item"><i class="fas fa-clock"></i> 发布于 {{ formatTime(picture.createTime) }}</span>
                </div>

                <div class="divider"></div>

                <!-- 评论区标题 -->
                <div class="comments-header">
                  <h3>共 {{ picture.commentCount || 0 }} 条评论</h3>
                </div>

                <!-- 评论列表 -->
                <div class="comments-scroll-area" ref="scrollContainer" @scroll="handleScroll">
                  <comment-list
                    v-if="comments.length"
                    :comments="comments"
                    @reply-clicked="handleReplyClick"
                    @update-comments="queryComments"
                  />
                  <!-- 空状态提示 -->
                  <div v-if="comments.length === 0 && !commentloading" class="empty-comment-state">
                    <img src="@/assets/illustrations/empty.png" alt="暂无评论" />
                    <p>还没有人发言，快来抢沙发吧~</p>
                  </div>
                  <!-- 已经到底了 -->
                  <div v-if="isEndOfData && comments.length > 0" class="end-of-data">
                    已经到底啦 ~
                  </div>
                  <div v-if="commentloading" class="loading-more">
                    <i class="fas fa-spinner fa-spin"></i> 加载中...
                  </div>
                </div>
              </div>

              <!-- 底部操作栏 (固定在右侧底部) -->
              <div class="action-footer">
                <div class="comment-input-container">
                  <div v-if="replyCommentId" class="reply-hint">
                    <span>回复 @{{ getReplyUserName() }}</span>
                    <i class="fas fa-times" @click="cancelReply"></i>
                  </div>
                  <div class="input-box-wrapper">
                    <input
                      v-model="commentContent"
                      class="comment-input"
                      type="text"
                      :placeholder="picture.allowComment ? '发条友善的评论吧...' : '主创已关闭评论'"
                      :disabled="!picture.allowComment"
                      @keydown.enter="addComment"
                    />
                    <button
                      class="send-btn"
                      :disabled="!commentContent.trim() || isAddingComment"
                      @click="addComment"
                    >
                      发送
                    </button>
                  </div>
                </div>

                <div class="footer-buttons">
                  <div class="footer-btn-item like" :class="{ active: picture.isLiked === 1 }" @click="doLike">
                    <i class="fas fa-heart"></i>
                    <span>{{ picture.likeCount || 0 }}</span>
                  </div>
                  <div class="footer-btn-item favorite" :class="{ active: picture.isFavorited === 1 }" @click="doFavorite">
                    <i class="fas fa-star"></i>
                    <span>{{ picture.favoriteCount || 0 }}</span>
                  </div>
                  <div class="footer-btn-item" :class="{ active: picture.isShared === 1 }" @click="doShare">
                    <i class="fas fa-share-alt"></i>
                    <span>{{ picture.shareCount || 0 }}</span>
                  </div>
                  <div class="footer-btn-item more" @click="handleShowMoreModal">
                    <i class="fas fa-ellipsis-h"></i>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="modal-layer">
          <ShareModal
            ref="shareModalRef"
            :link="shareLink"
            :imageUrl="picture.url"
            :title="picture.name"
            :user="picture.user"
            :createTime="picture.createTime"
          />
        </div>
        <ReportModal ref="reportModalRef" :target-type="'1'" :target-id="String(picture.id)" />

        <!-- 权限设置弹窗 -->
        <div v-if="showPermissionSetting" class="custom-confirm-overlay">
          <div class="permission-setting-content" @click.stop>
            <div class="modal-header">
              <h3>权限设置</h3>
              <button class="modal-close-btn" style="position:relative; width:30px; height:30px; top:0; right:0" @click="showPermissionSetting = false">
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

        <!-- 聊天室弹窗 -->
        <div v-if="showChatModal" class="custom-confirm-overlay">
          <div class="chat-room-content" @click.stop>
            <div class="modal-header">
              <h3>图片聊天室 <span style="font-size:12px; font-weight:normal;">(在线 {{ onlineCount || 0 }} 人)</span></h3>
              <button class="modal-close-btn" style="position:relative; width:30px; height:30px; top:0; right:0" @click="showChatModal = false">
                <i class="fas fa-times"></i>
              </button>
            </div>
            <div class="chat-room-body" style="height: 50vh; overflow-y: auto;">
              <PictureChatRoom
                ref="chatRoomRef"
                :pictureId="props.id"
                @message="handleChatMessage"
                class="modal-chat-room"
              />
            </div>
          </div>
        </div>

        <Teleport to="body">
          <div v-if="showMoreModal" class="custom-dropdown-overlay" @click="showMoreModal = false">
            <div class="custom-dropdown-content" @click.stop>
              <div class="dropdown-item" @click="openChatModal"><i class="fas fa-comments"></i> 聊天室</div>
              <div v-if="picture.user?.id === loginUserStore.loginUser?.id" class="dropdown-item" @click="openPermissionSetting"><i class="fas fa-cog"></i> 权限设置</div>
              <div class="dropdown-item" @click="openReportModal"><i class="fas fa-flag"></i> 举报</div>
              <div v-if="picture.user?.id === loginUserStore.loginUser?.id" class="dropdown-item" @click="doEdit"><i class="fas fa-edit"></i> 编辑资料</div>
              <div v-if="picture.user?.id === loginUserStore.loginUser?.id" class="dropdown-item danger" @click="showDeleteConfirm"><i class="fas fa-trash-alt"></i> 删除图片</div>
              <div v-if="picture.isDownload !== 0" class="dropdown-item" @click="handleDownload"><i class="fas fa-download"></i> 保存图片</div>
            </div>
          </div>
        </Teleport>

        <!-- 删除确认 (原生风格) -->
        <div v-if="deleteConfirmVisible" class="custom-confirm-overlay">
          <div class="confirm-box">
            <h3>提示</h3>
            <p>确认要删除这张图片吗？此操作不可撤销。</p>
            <div class="confirm-actions">
              <button class="btn-cancel" @click="deleteConfirmVisible = false">取消</button>
              <button class="btn-danger" @click="confirmDelete">确认删除</button>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>

  <!-- 图片预览 (PC端保持原样或自定义) -->
  <a-image
    v-if="picture.url"
    :src="picture.url"
    :preview="{
      visible: showPreview,
      onVisibleChange: handlePreviewChange,
    }"
    style="display: none;"
  />
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick, onUnmounted, reactive, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { downloadImage, toHexColor } from '@/utils'
import { getDeviceType } from '@/utils/device'
import { DEVICE_TYPE_ENUM } from '@/constants/device'
import { SPACE_PERMISSION_ENUM } from '@/constants/space'
import ShareModal from '@/components/ShareModal.vue'
import { getDefaultAvatar } from '@/utils/userUtils'
import { deletePictureUsingPost, getPictureVoByIdUsingGet } from '@/api/pictureController'
import { addUserFollowsUsingPost, findIsFollowUsingPost } from '@/api/userFollowsController'
import { message } from 'ant-design-vue'
import CommentList from '@/components/CommentList.vue'
import { addCommentUsingPost, queryCommentUsingPost } from '@/api/commentsController'
import { doLikeUsingPost } from '@/api/likeRecordController'
import { throttle } from 'lodash-es'
import { doShareUsingPost } from '@/api/shareRecordController'
import { addFavoriteRecordUsingPost, cancelFavoriteUsingPost } from '@/api/favoriteRecordController'
import { formatTime } from '@/utils/dateUtils'
import ReportModal from '@/components/ReportModal.vue'
import PictureChatRoom from '@/components/PictureChatRoom.vue'
import ContentPermissionSetting from '@/components/ContentPermissionSetting.vue'
import {
  LoadingOutlined,
} from '@ant-design/icons-vue'

interface Props {
  id: string | number
  visible?: boolean // 控制弹窗显示
  initialData?: API.PictureVO
}

const props = withDefaults(defineProps<Props>(), {
  visible: true,
})

const emit = defineEmits(['close', 'updated'])

// 监听弹窗显示，锁定/解锁 body 滚动
watch(() => props.visible, (newVal) => {
  if (newVal) {
    document.body.style.overflow = 'hidden'
  } else {
    document.body.style.overflow = ''
  }
}, { immediate: true })

onUnmounted(() => {
  document.body.style.overflow = ''
  window.removeEventListener('keydown', handleKeydown)
})

const loginUserStore = useLoginUserStore()
const router = useRouter()

// 基础状态
const isMobile = ref(false)
const imgReady = ref(false)
const picture = ref<API.PictureVO>({} as API.PictureVO)
const comments = ref<API.CommentsVO[]>([])
const commentloading = ref(false)
const isEndOfData = ref(false)
const isFollowed = ref(false)
const showMoreModal = ref(false)
const deleteConfirmVisible = ref(false)
const showPreview = ref(false)
const showChatModal = ref(false)
const showPermissionSetting = ref(false)
const onlineCount = ref(0)
const onlineUsers = ref<any[]>([])

const shareModalRef = ref()
const reportModalRef = ref()
const chatRoomRef = ref()
const scrollContainer = ref<HTMLElement | null>(null)

const dominantColor = computed(() => picture.value?.picColor ? toHexColor(picture.value.picColor) : '#f0f0f0')

const shareLink = computed(() => {
  if (!picture.value?.id) return ''
  return `${window.location.origin}/picture/${picture.value.id}`
})

const showImagePreview = () => {
  showPreview.value = true
}

const handlePreviewChange = (visible: boolean) => {
  showPreview.value = visible
}

// 评论分页
const queryRequest = reactive<API.CommentsQueryRequest>({
  targetId: String(props.id),
  targetType: 1,
  current: 1,
  pageSize: 20
})

// 初始化
onMounted(async () => {
  const device = await getDeviceType()
  isMobile.value = device === DEVICE_TYPE_ENUM.MOBILE || window.innerWidth < 768

  await fetchPictureDetail()
  queryComments()
  checkIsFollowed()

  window.addEventListener('keydown', handleKeydown)
})


const handleKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Escape') handleClose()
}

const fetchPictureDetail = async () => {
  try {
    if (props.initialData) {
      picture.value = props.initialData
      onlineCount.value = props.initialData.chatCount || 0
    }
    const res = await getPictureVoByIdUsingGet({ id: props.id })
    if (res.data.code === 0 && res.data.data) {
      picture.value = res.data.data
      onlineCount.value = res.data.data.chatCount || 0
    }
  } catch (e) {}
}

const queryComments = async () => {
  if (commentloading.value) return
  commentloading.value = true
  try {
    const res = await queryCommentUsingPost(queryRequest)
    if (res.data?.code === 0 && res.data.data) {
      const records = res.data.data.records || []
      if (queryRequest.current === 1) {
        comments.value = records
      } else {
        comments.value = [...comments.value, ...records]
      }
      isEndOfData.value = records.length < queryRequest.pageSize
    }
  } finally {
    commentloading.value = false
  }
}

const handleScroll = throttle((e: Event) => {
  const el = e.target as HTMLElement
  if (el.scrollHeight - el.scrollTop - el.clientHeight < 50) {
    if (!isEndOfData.value && !commentloading.value) {
      queryRequest.current++
      queryComments()
    }
  }
}, 200)

// 逻辑处理
const handleClose = () => {
  emit('close')
}

const handleImageLoad = () => {
  imgReady.value = true
}

const handleUserClick = (user: any) => {
  if (!user?.id) return
  router.push(`/user/${user.id}`)
}

const checkIsFollowed = async () => {
  if (!loginUserStore.loginUser?.id || !picture.value?.user?.id) return
  try {
    const res = await findIsFollowUsingPost({
      followerId: loginUserStore.loginUser.id,
      followingId: picture.value.user.id
    })
    isFollowed.value = !!res.data?.data
  } catch (e) {}
}

const handleFollow = async () => {
  if (!loginUserStore.loginUser?.id) return message.warning('请登录后再操作')
  try {
    const res = await addUserFollowsUsingPost({
      followerId: loginUserStore.loginUser.id,
      followingId: picture.value.user.id,
      followStatus: isFollowed.value ? 0 : 1
    })
    if (res.data.code === 0) {
      isFollowed.value = !isFollowed.value
      message.success(isFollowed.value ? '已关注' : '互相关注已取消')
    }
  } catch (e) {}
}

// 点赞/收藏
const doLike = async () => {
  if (!loginUserStore.loginUser?.id) return message.warning('请登录后再操作')
  const newStatus = picture.value.isLiked === 1 ? false : true
  try {
    const res = await doLikeUsingPost({
      targetId: props.id,
      targetType: 1,
      isLiked: newStatus
    })
    if (res.data.code === 0) {
      picture.value.isLiked = newStatus ? 1 : 0
      picture.value.likeCount = String(Number(picture.value.likeCount || 0) + (newStatus ? 1 : -1))
    }
  } catch (e) {}
}

const doFavorite = async () => {
  if (!loginUserStore.loginUser?.id) return message.warning('请登录后再操作')
  const isFavorited = picture.value.isFavorited === 1
  try {
    let res;
    if (isFavorited) {
      res = await cancelFavoriteUsingPost({
        userId: loginUserStore.loginUser.id,
        targetId: props.id,
        targetType: 1
      })
    } else {
      res = await addFavoriteRecordUsingPost({
        userId: loginUserStore.loginUser.id,
        targetId: props.id,
        targetType: 1,
        targetUserId: picture.value.user?.id,
        isFavorite: true
      })
    }
    if (res.data.code === 0) {
      picture.value.isFavorited = isFavorited ? 0 : 1
      picture.value.favoriteCount = String(Number(picture.value.favoriteCount || 0) + (isFavorited ? -1 : 1))
      message.success(isFavorited ? '取消收藏' : '收藏成功')
    }
  } catch (e) {}
}

// 评论
const commentContent = ref('')
const isAddingComment = ref(false)
const replyCommentId = ref('')
const handleReplyClick = (cid: string) => {
  replyCommentId.value = cid
  nextTick(() => {
    const inputEl = document.querySelector('.comment-input') as HTMLElement
    inputEl?.focus()
  })
}
const cancelReply = () => {
  replyCommentId.value = ''
}
const getReplyUserName = () => {
  return comments.value.find(c => String(c.commentId) === String(replyCommentId.value))?.commentUser?.userName || ''
}
const addComment = async () => {
  if (!commentContent.value.trim() || isAddingComment.value) return
  isAddingComment.value = true
  try {
    const res = await addCommentUsingPost({
      targetId: props.id,
      targetType: 1,
      content: commentContent.value.trim(),
      parentCommentId: replyCommentId.value || '0'
    })
    if (res.data.code === 0) {
      commentContent.value = ''
      replyCommentId.value = ''
      queryRequest.current = 1
      queryComments()
      picture.value.commentCount = String(Number(picture.value.commentCount || 0) + 1)
      message.success('评论成功')
    }
  } catch (e) {
    message.error('评论失败')
  } finally {
    isAddingComment.value = false
  }
}

// 更多
const handleShowMoreModal = () => {
  showMoreModal.value = true
}
const openReportModal = () => {
  reportModalRef.value?.openModal()
  showMoreModal.value = false
}
const openChatModal = () => {
  if (loginUserStore.loginUser) {
    showChatModal.value = true
    showMoreModal.value = false
  } else {
    message.warning('请先登录')
  }
}
const handleChatMessage = (msg: any) => {
  if (msg.type === 'onlineUsers') {
    onlineCount.value = msg.onlineCount
    onlineUsers.value = msg.onlineUsers
  }
}
const openPermissionSetting = () => {
  showPermissionSetting.value = true
  showMoreModal.value = false
}
const handlePermissionsUpdated = (newPermissions: any) => {
  picture.value.allowLike = newPermissions.allowLike
  picture.value.allowComment = newPermissions.allowComment
  picture.value.allowCollect = newPermissions.allowCollect
  picture.value.allowShare = newPermissions.allowShare
  showPermissionSetting.value = false
}
const handleDownload = () => {
  downloadImage(picture.value.url, picture.value.name)
  showMoreModal.value = false
}
const doEdit = () => {
  showMoreModal.value = false
  router.push({
    path: '/add_picture',
    query: {
      id: picture.value.id,
      spaceId: picture.value.spaceId
    }
  }).then(() => {
    handleClose()
  })
}
const showDeleteConfirm = () => {
  deleteConfirmVisible.value = true
}
const confirmDelete = async () => {
  const res = await deletePictureUsingPost({ id: picture.value.id })
  if (res.data.code === 0) {
    message.success('删除成功')
    handleClose()
  }
}
const doShare = async () => {
  if (!loginUserStore.loginUser?.id) {
    message.warning('请登录后再操作')
    return
  }

  // 检查分享权限 (如果有 allowShare 字段)
  if (picture.value.allowShare === 0) {
    message.warning('该内容已被原作者关闭分享')
    return
  }

  // 如果已经分享过，切换逻辑 (参考 MobilePictureDetailPage)
  if (picture.value.isShared === 1) {
    try {
      const res = await doShareUsingPost({
        targetId: picture.value.id,
        targetType: 1,
        isShared: false
      })
      if (res.data.code === 0) {
        picture.value.shareCount = (Number(picture.value.shareCount) || 0) - 1
        picture.value.isShared = 0
        message.success('已取消分享记录')
      }
    } catch (e) {
      console.error('取消分享失败:', e)
    }
    return
  }

  try {
    const res = await doShareUsingPost({
      targetId: picture.value.id,
      targetType: 1, // 图片类型
      isShared: true
    })
    if (res.data?.code === 0) {
      picture.value.isShared = 1
      picture.value.shareCount = (Number(picture.value.shareCount) || 0) + 1
      shareModalRef.value?.openModal()
    }
  } catch (e) {
    // 即使后端记录失败，我们也尝试打开弹窗（提高体验）
    shareModalRef.value?.openModal()
  }
}


const formattedIntroduction = computed(() => {
  return (picture.value.introduction || '暂无介绍').replace(/\n/g, '<br/>')
})
</script>

<style scoped>
/* 蒙层动画 */
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}

/* 弹出层样式 */
.picture-modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 10000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.35); /* 稍微深一点的背景 */
}

/* 动态虚化背景 */
.modal-backdrop-blur {
  position: absolute;
  inset: 0;
  background-size: cover;
  background-position: center;
  filter: blur(60px) brightness(0.7);
  opacity: 0.8;
  z-index: -1;
}

.modal-container {
  width: 94vw;
  height: 94vh;
  max-width: 1300px;
  background: var(--card-background);
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 100px 100px -20px rgba(0,0,0,0.5);
  display: flex;
  position: relative;
  z-index: 10001;
}

.modal-container.is-mobile {
  width: 100vw;
  height: 100vh;
  border-radius: 0;
  flex-direction: column;
}

.modal-close-btn {
  position: absolute;
  top: 20px;
  right: 20px;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 10003;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  font-size: 18px;
}
.modal-close-btn:hover {
  background: rgba(0, 0, 0, 0.6);
  transform: rotate(90deg) scale(1.1);
}

.modal-content-wrapper {
  display: flex;
  width: 100%;
  height: 100%;
}

/* 左侧图片区 */
.preview-section {
  flex: 7;
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.image-container {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  opacity: 0;
  transform: scale(0.98);
  transition: all 0.5s cubic-bezier(0.16, 1, 0.3, 1);
}
.image-container.ready {
  opacity: 1;
  transform: scale(1);
}

.main-image {
  max-width: 90%;
  max-height: 90%;
  object-fit: contain;
  box-shadow: 0 20px 40px rgba(0,0,0,0.3);
  border-radius: 4px;
  cursor: zoom-in;
  z-index: 2;
}

.bg-fill-blur {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  filter: blur(30px) opacity(0.3);
  z-index: 1;
}

.image-overlay-tools {
  position: absolute;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 16px;
  padding: 10px 20px;
  background: rgba(255,255,255, 0.1);
  backdrop-filter: blur(20px);
  border-radius: 30px;
  border: 1px solid rgba(255,255,255, 0.1);
  z-index: 10;
}
.tool-item {
  color: #fff;
  font-size: 20px;
  cursor: pointer;
  transition: transform 0.2s;
}
.tool-item:hover { transform: scale(1.2); }
.tool-item.active.like { color: #ff4d4f; } /* 点赞红色 */
.tool-item.active.favorite { color: #ffca28; } /* 收藏黄色 */

/* 右侧详情区 */

/* 右侧详情区 */
.details-section {
  flex: 3;
  background: var(--card-background);
  display: flex;
  flex-direction: column;
  position: relative;
  border-left: 1px solid var(--border-color);
}

.details-inner {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  padding-bottom: 200px; /* 为底部固定栏留空 */
}

/* 隐藏滚动条 */
.details-inner::-webkit-scrollbar { width: 4px; }
.details-inner::-webkit-scrollbar-thumb { background: #eee; border-radius: 2px; }

.author-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}
.author-info {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}
.author-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid #eee;
}
.author-name {
  display: block;
  font-size: 15px;
  font-weight: 600;
  color: var(--post-title-color);
}
.publish-time {
  display: block;
  font-size: 12px;
  color: var(--text-secondary);
}

.follow-btn-inline {
  padding: 4px 14px;
  border-radius: 15px;
  background: #3b82f6; /* 关注蓝色 */
  color: #fff;
  border: none;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  margin-left: 8px;
  transition: all 0.2s;
}
.follow-btn-inline.followed {
  background: #f3f3f3;
  color: #666;
}
.follow-btn-inline:hover {
  opacity: 0.9;
  transform: scale(1.02);
}

.name-row {
  display: flex;
  align-items: center;
}

.description-card {
  margin-bottom: 24px;
}
.pic-title {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 12px;
  color: var(--post-title-color);
}
.pic-intro {
  font-size: 15px;
  line-height: 1.6;
  color: var(--post-text-color);
  margin-bottom: 16px;
}
.tag-item {
  color: var(--link-color);
  font-size: 14px;
  margin-right: 8px;
  cursor: pointer;
}

.stats-bar {
  display: flex;
  gap: 16px;
  color: var(--text-secondary);
  font-size: 13px;
  margin-bottom: 16px;
}

.divider {
  height: 1px;
  background: var(--border-color);
  margin: 20px 0;
}

.comments-header h3 {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 16px;
}

.no-comments {
  text-align: center;
  padding: 40px 0;
  color: #ccc;
  font-size: 14px;
}

/* 底部操作固定栏 */
.action-footer {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: var(--card-background); /* 取消透明度 */
  border-top: 1px solid var(--border-color);
  padding: 16px 20px;
  z-index: 100;
}

.reply-hint {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 12px;
  color: #3b82f6; /* 回复提示蓝色 */
  background: rgba(59, 130, 246, 0.05);
  padding: 4px 8px;
  border-radius: 4px;
}

.input-box-wrapper {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}
.comment-input {
  flex: 1;
  background: var(--hover-background);
  color: var(--text-primary);
  border: none;
  padding: 10px 16px;
  border-radius: 22px;
  font-size: 14px;
  outline: none;
}
.send-btn {
  background: #3b82f6;
  border: none;
  color: #fff;
  padding: 0 16px;
  border-radius: 18px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}
.send-btn:hover:not(:disabled) {
  background: #2563eb;
  transform: translateY(-1px);
}
.send-btn:disabled {
  background: #e5e7eb;
  color: #9ca3af;
  cursor: not-allowed;
}

.footer-buttons {
  display: flex;
  justify-content: space-around;
  align-items: center;
}
.footer-btn-item {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: color 0.2s;
}
.footer-btn-item i { font-size: 20px; }
.footer-btn-item span { font-size: 13px; font-weight: 500; }
.footer-btn-item.active.like { color: #ff4d4f; } /* 点赞红色 */
.footer-btn-item.active.favorite { color: #ffca28; } /* 收藏黄色 */
.footer-btn-item.active i.fa-share-alt { color: #10b981; } /* 分享绿色 */
.footer-btn-item.more { color: var(--text-secondary); }

/* 下拉菜单 */
.custom-dropdown-overlay {
  position: fixed;
  inset: 0;
  z-index: 10010;
}
.custom-dropdown-content {
  position: absolute;
  right: 20px;
  bottom: 80px;
  background: var(--card-background);
  backdrop-filter: blur(20px);
  border-radius: 16px;
  box-shadow: 0 20px 50px var(--shadow-color);
  border: 1px solid var(--border-color);
  overflow: hidden;
  width: 180px;
  padding: 8px;
  transform-origin: bottom right;
  animation: dropdownIn 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes dropdownIn {
  from { opacity: 0; transform: scale(0.9) translateY(10px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}

.dropdown-item {
  padding: 12px 14px;
  font-size: 14px;
  color: var(--post-text-color);
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 12px;
  border-radius: 10px;
  transition: all 0.2s;
}
.dropdown-item:hover {
  background: var(--hover-background);
  color: var(--link-color);
}
.dropdown-item i {
  width: 16px;
  text-align: center;
  font-size: 15px;
}
.dropdown-item.danger { color: #ef4444; }
.dropdown-item.danger:hover { background: rgba(239, 68, 68, 0.1); }

.end-of-data, .loading-more {
  text-align: center;
  padding: 20px 0;
  color: var(--text-secondary);
  font-size: 13px;
}

/* 空状态提示 */
.empty-comment-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
  opacity: 0.8;
}
.empty-comment-state img {
  width: 120px;
  margin-bottom: 16px;
  filter: drop-shadow(0 4px 6px rgba(0,0,0,0.05));
}
.empty-comment-state p {
  font-size: 14px;
  color: var(--text-secondary);
}
/* 确认框 */
.custom-confirm-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.6);
  z-index: 10020;
  display: flex;
  align-items: center;
  justify-content: center;
}
.confirm-box {
  background: var(--card-background);
  border-radius: 16px;
  padding: 24px;
  width: 300px;
  text-align: center;
}
.confirm-box h3 { font-size: 17px; margin-bottom: 12px; color: var(--post-title-color); }
.confirm-box p { font-size: 14px; color: var(--post-text-color); margin-bottom: 24px; line-height: 1.5; }
.confirm-actions { display: flex; gap: 12px; }
.btn-cancel { flex: 1; padding: 10px; border-radius: 20px; border: 1px solid var(--border-color); background: var(--card-background); color: var(--text-primary); cursor: pointer; }
.btn-danger { flex: 1; padding: 10px; border-radius: 20px; border: none; background: #fe2c55; color: #fff; cursor: pointer; }

/* 确保内部弹窗不会被外部蒙层盖住 */
.modal-layer {
  position: absolute;
  z-index: 10050; /* 高于图片模态框本身 */
}

.permission-setting-content, .chat-room-content {
  background: var(--card-background);
  color: var(--text-primary);
  border-radius: 16px;
  width: 90vw;
  max-width: 500px;
  padding: 20px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
}
.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.modal-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--post-title-color);
}

/* 移动端特殊处理 */
@media (max-width: 768px) {
  .modal-content-wrapper { flex-direction: column; overflow-y: auto; }
  .preview-section { flex: none; height: auto; min-height: 300px; padding: 20px 0; }
  .details-section { flex: none; border-left: none; }
  .action-footer { position: sticky; bottom: 0; }
  .details-inner { overflow: visible; padding-bottom: 20px; }
}

/* 全局覆盖图片预览组件的层级 */
:global(.ant-image-preview-wrap) {
  z-index: 100000 !important;
}
:global(.ant-image-preview-mask) {
  z-index: 100000 !important;
}

/* 覆盖举报弹窗层级，确保在详情弹窗之上 */
:global(.report-mask) {
  z-index: 100001 !important;
}
</style>
