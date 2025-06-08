<template>
  <div class="post-detail-page">
    <div class="post-content-wrapper">
      <!-- 主内容区 -->
      <div class="post-main">
        <div class="post-header">
          <h1 class="title">{{ post.title }}</h1>
          <div class="status-tags">
            <span v-if="post.status === 0" class="tag pending">待审核</span>
            <span v-else-if="post.status === 2" class="tag rejected">已拒绝</span>
          </div>
          <!-- 移动端作者信息 -->
          <div class="author-card">
            <div class="author-info" @click="handleUserClick(post.user)">
              <div class="author-avatar">
                <img class="avatar" :src="post.user?.userAvatar || getDefaultAvatar(post.user?.userName)" :alt="post.user?.userName">
              </div>
              <div class="author-details">
                <div class="author-name-follow">
                  <span class="name">{{ post.user?.userName }}</span>
                  <button v-if="post.user?.id !== loginUserId" class="btn-follow" :class="{ 'is-followed': isFollowed }" @click.stop="handleFollow"  >
                    {{ isFollowed ? '已关注' : '关注' }}
                  </button>
                </div>
                <span class="time">{{ formatTime(post.createTime) }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="post-content">
          <markdown-content :content="post.content" />
        </div>
      </div>

      <!-- 侧边栏 -->
      <div class="post-sidebar">
        <div class="interaction-bar">
          <div class="action-item like" :class="{ active: post.isLiked === 1 }" @click="handleLike">
            <span class="emoji">{{ post.isLiked === 1 ? '❤️' : '🤍' }}</span>
            <span>{{ post.likeCount || 0 }}</span>
          </div>
          <div class="action-item">
            <span class="emoji">👁️</span>
            <span>{{ post.viewCount || 0 }}</span>
          </div>
          <div class="action-item" @click="handleCommentClick">
            <span class="emoji">💬</span>
            <span>{{ post.commentCount || 0 }}</span>
          </div>
          <div class="action-item share" :class="{ active: post.isShared === 1 }" @click="handleShare">
            <span class="emoji">{{ post.isShared === 1 ? '✨' : '🔗' }}</span>
            <span>{{ post.shareCount || 0 }}</span>
          </div>
        </div>

        <div v-if="canEdit || canDelete" class="admin-actions">
          <button v-if="canEdit" class="edit-btn" @click="handleEdit" title="编辑">✏️</button>
          <button v-if="canDelete" class="delete-btn" @click="showDeleteConfirm" title="删除">❌️</button>
        </div>
      </div>
    </div>

    <!-- 评论弹框样式 -->
    <div v-if="visible" class="comment-drawer">
      <div class="drawer-content">
        <div class="drawer-header">
          <h2>评论 ({{ post.commentCount || 0 }})</h2>
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
            <button @click="cancelReply">取消</button>
          </div>
          <div class="input-box">
            <button class="emoji-btn" @click.stop="toggleEmojiPicker">😊</button>
            <textarea
              v-model="commentContent"
              :placeholder="replyCommentId ? '写下你的回复...' : '写下你的评论...'"
              maxlength="200"
              @keydown.enter.prevent="addComment"
            ></textarea>
            <button
              class="send-btn"
              :disabled="!commentContent.trim()"
              @click="addComment"
            >
              {{ replyCommentId ? '回复' : '发送' }}
            </button>
          </div>
          <div v-if="showEmojiPicker" class="emoji-picker-wrapper" @click.stop>
            <emoji-picker
              @select="onEmojiSelect"
              :i18n="emojiI18n"
              class="custom-emoji-picker"
            />
          </div>
        </div>
      </div>
    </div>

    <share-modal
      ref="shareModalRef"
      :link="shareLink"
      :imageUrl="shareImage"
      :title="post.title"
      @share-success="handleShareSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, reactive, nextTick, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { Image } from 'ant-design-vue'
import { LikeOutlined, EyeOutlined, CommentOutlined, EditOutlined, DeleteOutlined, ShareAltOutlined, SmileOutlined, MessageOutlined, ArrowRightOutlined, CloseCircleOutlined } from '@ant-design/icons-vue'
import MarkdownContent from '@/components/MarkdownContent.vue'
import { getPostByIdUsingGet, deletePostUsingPost } from '@/api/postController'
import { addUserFollowsUsingPost, findIsFollowUsingPost } from '@/api/userFollowsController'
import { getDefaultAvatar } from '@/utils/userUtils'
import { formatTime } from '@/utils/dateUtils'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import ShareModal from '@/components/ShareModal.vue'
import CommentList from '@/components/CommentList.vue'
import { addCommentUsingPost, queryCommentUsingPost } from '@/api/commentsController'
import { doLikeUsingPost } from '@/api/likeRecordController'
import { doShareUsingPost } from '@/api/shareRecordController'
import EmojiPicker from '@/components/EmojiPicker.vue'
import '@lottiefiles/lottie-player'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()
const mounted = ref(false)
const post = ref<API.Post>({} as API.Post)
const followLoading = ref(false)
const isFollowed = ref(false)
const isEndOfData = ref(false)
// 添加全局预览方法
window.$previewImage = (options: any) => {
  const { src } = options
  Image.preview({
    src,
    maskClosable: true,
    icons: {
      close: true,
    }
  })
}

// 权限判断
const loginUserId = computed(() => loginUserStore.loginUser?.id)
const canEdit = computed(() => {
  return post.value.userId === loginUserId.value ||
    loginUserStore.loginUser?.userRole === 'admin'
})
const canDelete = computed(() => canEdit.value)

// 检查是否已关注
const checkIsFollowed = async () => {
  if (!loginUserStore.loginUser?.id || !post.value?.user?.id) {
    return
  }
  try {
    const res = await findIsFollowUsingPost({
      followerId: loginUserStore.loginUser.id,
      followingId: post.value.user.id
    })
    if (res.data?.data) {
      isFollowed.value = res.data.data
    }
  } catch (error) {
    console.error('检查关注状态失败:', error)
  }
}

// 获取帖子详情
const fetchPostDetail = async () => {
  const id = route.params.id
  if (!id || typeof id !== 'string') {
    // message.error('无效的帖子ID')
    router.push('/forum')
    return
  }

  try {
    const res = await getPostByIdUsingGet({ id })
    if (res.data?.data) {
      post.value = res.data.data
      mounted.value = true
      await checkIsFollowed()
    }
  } catch (error: any) {
    // message.error('获取帖子详情失败：' + error.message)
    router.push('/forum')
  }
}

// 修改点赞处理函数
const handleLike = async () => {
  try {
    const requestBody: API.LikeRequest = {
      targetId: post.value.id,
      targetType: 2, // 2 表示帖子类型
      isLiked: post.value.isLiked !== 1
    }

    const res = await doLikeUsingPost(requestBody)
    if (res.data.code === 0) {
      // 更新前端数据
      if (requestBody.isLiked) {
        post.value.likeCount = String(Number(post.value.likeCount || 0) + 1)
        post.value.isLiked = 1
      } else {
        post.value.likeCount = String(Number(post.value.likeCount || 0) - 1)
        post.value.isLiked = 0
      }
    }
  } catch (error) {
    // console.error('点赞失败:', error)
    // message.error('点赞失败')
  }
}

// 关注作者
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

    if (res.data?.code === 0) {
      isFollowed.value = !isFollowed.value
      // message.success(isFollowed.value ? '关注成功' : '取消关注成功')
    } else {
      // message.error('操作失败')
    }
  } catch (error) {
    // message.error('操作失败，请稍后重试')
  } finally {
    followLoading.value = false
  }
}

// 编辑帖子
const handleEdit = () => {
  router.push({
    path: `/post/edit/${post.value.id}`,
    query: {
      post: JSON.stringify({
        id: post.value.id,
        title: post.value.title,
        content: post.value.content,
        category: post.value.category,
        attachments: post.value.attachments
      })
    }
  })
}

// 删除帖子
const showDeleteConfirm = () => {
  Modal.confirm({
    title: '确认删除',
    content: '删除后无法恢复，确定要删除这篇帖子吗？',
    okText: '确认',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      try {
        const res = await deletePostUsingPost({ id: post.value.id })
        if (res.data?.code === 0) {
          // message.success('删除成功')
          router.push('/forum')
        } else {
          // message.error('删除失败')
        }
      } catch (error: any) {
        // message.error('删除失败：' + error.message)
      }
    },
  })
}

// 添加评论相关状态
const visible = ref(false)
const comments = ref<API.Comment[]>([])
const commentContent = ref('')
const replyCommentId = ref('')
const commentloading = ref(false)
const showEmojiPicker = ref(false)


// 查询评论请求对象
const queryRequest = reactive<API.CommentsQueryRequest>({
  targetId: 0,
  targetType: 2, // 2 表示帖子类型
  current: 1,
  pageSize: 15,
})

// 查询评论
const queryComments = async () => {
  try {
    commentloading.value = true
    const res = await queryCommentUsingPost(queryRequest)
    if (res.data.data != null) {
      comments.value = res.data.data.records.map(comment => ({
        ...comment,
        commentId: comment.commentId?.toString(),
        parentCommentId: comment.parentCommentId?.toString(),
      }))
      isEndOfData.value = comments.value.length < queryRequest.pageSize
    } else {
      comments.value = []
      isEndOfData.value = true
    }
  } catch (error) {
    // console.error('查询评论异常', error)
    // message.error('获取评论失败')
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
      targetId: post.value.id,
      targetType: 2,
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
      post.value.commentCount = String(Number(post.value.commentCount || 0) + 1)
    }
  } catch (error) {
    // console.error('评论失败:', error)
    // message.error('评论失败')
  }
}

// 修改分享处理函数
const handleShare = async () => {
  // 如果已经分享过,则执行取消分享
  if (post.value.isShared === 1) {
    try {
      const requestBody: API.ShareRequest = {
        targetId: post.value.id,
        targetType: 2, // 2 表示帖子类型
        isShared: false
      }
      const res = await doShareUsingPost(requestBody)
      if (res.data.code === 0) {
        post.value.shareCount = String(Number(post.value.shareCount || 0) - 1)
        post.value.isShared = 0
        // message.success('取消分享成功')
      }
    } catch (error) {
      // console.error('取消分享失败:', error)
      // message.error('取消分享失败')
    }
    return
  }

  // 未分享过,显示分享模态框并调用分享接口
  try {
    const requestBody: API.ShareRequest = {
      targetId: post.value.id,
      targetType: 2,
      isShared: true
    }
    const res = await doShareUsingPost(requestBody)
    if (res.data.code === 0) {
      post.value.shareCount = String(Number(post.value.shareCount || 0) + 1)
      post.value.isShared = 1
      // 成功后再显示分享模态框
      shareModalRef.value?.openModal()
    }
  } catch (error) {
    // console.error('分享失败:', error)
    // message.error('分享失败')
  }
}

// 处理分享成功
const handleShareSuccess = async () => {
  try {
    const requestBody: API.ShareRequest = {
      targetId: post.value.id,
      targetType: 2, // 2 表示帖子类型
      isShared: true
    }
    const res = await doShareUsingPost(requestBody)
    if (res.data.code === 0) {
      post.value.shareCount = String(Number(post.value.shareCount || 0) + 1)
      post.value.isShared = 1
      // message.success('分享成功')
    }
  } catch (error) {
    // console.error('分享失败:', error)
    // message.error('分享失败')
  }
}

// 分享相关
const shareModalRef = ref()
const shareLink = computed(() => window.location.origin + '/post/' + post.value?.id)
const shareImage = computed(() => {
  // 如果有附件图片就用第一张，否则用默认图片
  return post.value?.attachments?.[0]?.url || '/default-share-image.png'
})

// 添加宠物动画相关代码
const PETS = [
  {
    name: 'dog',
    url: 'https://assets5.lottiefiles.com/packages/lf20_syqnfe7c.json'
  },
  {
    name: 'cat',
    url: 'https://assets2.lottiefiles.com/packages/lf20_bkqn2x.json'
  },
  // ... 其他宠物
]

const currentPet = ref(PETS[Math.floor(Math.random() * PETS.length)])
const petAnimation = ref(null)

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

// 添加滚动处理函数
const handleScroll = () => {
  // 实现滚动加载更多的逻辑
  const container = scrollContainer.value
  if (!container) return

  const { scrollTop, clientHeight, scrollHeight } = container
  if (scrollTop + clientHeight >= scrollHeight - 50 && !commentloading.value && !isEndOfData.value) {
    // 加载更多评论
    loadMoreComments()
  }
}

// 加载更多评论的函数
const loadMoreComments = async () => {
  if (commentloading.value || isEndOfData.value) return

  queryRequest.current++
  await queryComments()
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

// 添加 scrollContainer ref
const scrollContainer = ref(null)

onMounted(() => {
  fetchPostDetail().then(() => {
    if (post.value.id) {
      queryRequest.targetId = post.value.id
      queryComments()
    }
  })
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})

// 修改评论按钮点击处理
const handleCommentClick = () => {
  visible.value = true
  queryComments()
}
</script>

<style scoped>
.post-detail-page {
  min-height: calc(100vh - 132px);
  margin: -24px;
  position: relative;
  border-radius: 20px;
  opacity: 1;
  background: linear-gradient(135deg, #1a1f35 0%, #212b3f 100%);
  color: #fff;
  overflow: hidden;
  padding: 20px;
  display: flex;
  justify-content: center;
}

.post-content-wrapper {
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 2rem;
  position: relative;
  height: 100%;
  margin-bottom: 48px;
}

/* 主内容区样式 */
.post-main {
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  overflow: hidden;
  transition: transform 0.3s ease;
  width: 100%;
  max-width: 100%;
}

.post-header {
  padding: 1rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.title {
  font-size: 32px;
  font-weight: 600;
  line-height: 1.4;
  margin-bottom: 1rem;
  color: #fff;
}

.post-content {
  padding: 1rem;
  font-size: 16px;
  line-height: 1.8;
  color: rgba(255, 255, 255, 0.9);
  word-wrap: break-word;
  word-break: break-all;
  white-space: pre-wrap;
  overflow-wrap: break-word;
  max-width: 100%;
}

/* 状态标签 */
.status-tags {
  display: flex;
  gap: 12px;
  margin-top: 16px;
}

.tag {
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 14px;
  background: rgba(255, 255, 255, 0.1);
}

.tag.pending {
  background: rgba(255, 171, 0, 0.15);
  color: #ffab00;
}

.tag.rejected {
  background: rgba(255, 86, 86, 0.15);
  color: #ff5656;
}

/* 侧边栏样式 */
.post-sidebar {
  position: fixed;
  right: 24px;
  top: 100px;
  height: fit-content;
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 0;
  width: 48px;
  z-index: 100;
}

/* 作者卡片 */
.author-card {
  margin-bottom: 12px;
  padding: 16px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(10px);
}

.author-info {
  display: flex;
  align-items: center;
  gap: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.author-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
}

.avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.author-info:hover .avatar {
  transform: scale(1.05);
}

.author-details {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.author-name-follow {
  display: flex;
  align-items: center;
  gap: 12px;
}

.name {
  font-size: 16px;
  font-weight: 600;
  color: #fff;
  margin: 0;
}

.time {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
}

.btn-follow {
  padding: 4px 12px;
  border-radius: 16px;
  font-size: 13px;
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

/* 移动端适配 */
@media (max-width: 768px) {
  .author-card {
    padding: 12px;
    margin-bottom: 4px;
  }

  .author-avatar {
    width: 40px;
    height: 40px;
  }

  .name {
    font-size: 15px;
  }

  .time {
    font-size: 13px;
  }

  .btn-follow {
    padding: 3px 10px;
    font-size: 12px;
  }
}

/* 交互栏 */
.interaction-bar {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.action-item {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.05);
  cursor: pointer;
  transition: all 0.3s ease;
  width: 100%;
  max-width: 160px;
  margin: 0 auto;
}

.action-item:hover {
  background: rgba(255, 255, 255, 0.1);
  transform: translateY(-2px);
}

.action-item .emoji {
  font-size: 20px;
  line-height: 1;
  transition: all 0.3s ease;
}

.action-item:hover .emoji {
  transform: scale(1.2);
}

/* 点赞按钮样式 */
.action-item.like img {
  filter: brightness(0) invert(1);
}

.action-item.like.active img {
  filter: invert(36%) sepia(95%) saturate(7117%) hue-rotate(353deg) brightness(98%) contrast(135%);
}

/* 分享按钮样式 */
.action-item.share img {
  filter: brightness(0) invert(1);
}

.action-item.share.active img {
  filter: invert(72%) sepia(10%) saturate(5162%) hue-rotate(346deg) brightness(103%) contrast(101%);
}

/* 移动端样式 */
@media screen and (max-width: 768px) {
  .post-detail-page {
    margin: -28px;
    padding: 0;
    border-radius: 0;
    min-height: 100vh;
    display: block;
    transition: none;
    transform: none;
    opacity: 1;
  }

  .post-content-wrapper {
    grid-template-columns: 1fr;
    gap: 16px;
    padding: 0;
    padding-bottom: 132px;
  }

  .post-main {
    border-radius: 0;
    backdrop-filter: none;
    background: rgba(255, 255, 255, 0.03);
    transform: none !important;
    box-shadow: none;
    border: none;
  }

  .post-sidebar {
    position: fixed !important;
    bottom: 0 !important;
    left: 0 !important;
    right: 0 !important;
    top: auto !important;
    width: 100% !important;
    height: auto !important;
    background: rgba(26, 31, 53, 0.95);
    backdrop-filter: none;
    padding: 12px 16px;
    border-radius: 20px 20px 0 0;
    z-index: 1000;
    margin: 0;
  }

  .interaction-bar {
    display: flex !important;
    flex-direction: row !important;
    justify-content: space-around !important;
    background: rgba(255, 255, 255, 0.05);
    border-radius: 16px;
    border: 1px solid rgba(255, 255, 255, 0.1);
    padding: 12px;
    width: 100%;
    margin: 0;
  }

  .action-item {
    display: flex !important;
    flex-direction: column !important;
    align-items: center !important;
    gap: 8px;
    padding: 8px;
    background: none;
    cursor: pointer;
    width: auto !important;
    margin: 0 !important;
  }

  .action-item img {
    width: 20px;
    height: 20px;
  }

  .action-item span {
    font-size: 12px;
  }

  .admin-actions {
    display: flex;
    gap: 12px;
  }

  .admin-actions button {
    flex: 1;
    padding: 10px;
    border-radius: 12px;
    background: rgba(255, 255, 255, 0.05);
    border: 1px solid rgba(255, 255, 255, 0.1);
    color: #fff;
  }

  .author-info {
    padding: 12px;
    gap: 12px;
  }

  .avatar {
    width: 48px;
    height: 48px;
  }

  .name {
    font-size: 15px;
  }

  .time {
    font-size: 12px;
  }
}

/* 大屏幕优化 */
@media screen and (min-width: 1400px) {
  .post-sidebar {
    right: calc((100vw - 1200px) / 2 - 80px);
  }
}

/* PC端样式 */
@media screen and (min-width: 769px) {
  .interaction-bar {
    display: flex;
    flex-direction: column;
    gap: 16px;
    padding: 0;
    background: none;
    border: none;
  }

  .action-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
    padding: 0;
    background: none;
    cursor: pointer;
    transition: all 0.3s ease;
    width: auto;
  }

  .action-item:hover {
    transform: translateY(-2px);
  }

  .action-item img {
    width: 20px;
    height: 20px;
    transition: all 0.3s ease;
  }

  .action-item span {
    font-size: 12px;
    color: rgba(255, 255, 255, 0.9);
  }

  .admin-actions {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .admin-actions button {
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 8px;
    background: none;
    border: none;
    color: rgba(255, 255, 255, 0.9);
    cursor: pointer;
    transition: all 0.3s ease;
    font-size: 20px;
  }

  .admin-actions .edit-btn:hover {
    color: #34d399;
    transform: translateY(-2px);
  }

  .admin-actions .delete-btn:hover {
    color: #ef4444;
    transform: translateY(-2px);
  }
}

/* 适配底部安全区域 */
@supports (padding-bottom: env(safe-area-inset-bottom)) {
  @media screen and (max-width: 768px) {
    .post-sidebar {
      padding-bottom: calc(12px + env(safe-area-inset-bottom)) !important;
    }
  }
}

/* 评论弹框样式 */
.comment-drawer {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.75);
  backdrop-filter: blur(8px);
  z-index: 1000;
  display: flex;
  justify-content: center;
  align-items: center;
  animation: fadeIn 0.3s ease;
}

.drawer-content {
  position: relative;
  z-index: 1001;
  width: 90%;
  max-width: 600px;
  height: 80vh;
  background: linear-gradient(135deg, #1a1f35, #2d3748);
  border-radius: 20px;
  display: flex;
  flex-direction: column;
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
  animation: slideUp 0.4s cubic-bezier(0.16, 1, 0.3, 1);
  color: #fff;
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
  padding: 8px;
  border-radius: 50%;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
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
  padding: 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(0, 0, 0, 0.2);
  border-radius: 0 0 20px 20px;
  position: relative;
}

.reply-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  margin-bottom: 12px;
}

.reply-bar span {
  color: rgba(255, 255, 255, 0.8);
  font-size: 14px;
}

.reply-bar button {
  background: none;
  border: none;
  color: #ff8e53;
  font-size: 14px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: all 0.3s ease;
}

.reply-bar button:hover {
  background: rgba(255, 142, 83, 0.1);
}

.input-box {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 20px;
  padding: 8px 16px;
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

textarea {
  flex: 1;
  height: 40px;
  padding: 8px 12px;
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: rgba(255, 255, 255, 0.05);
  color: #fff;
  resize: none;
  font-size: 14px;
  transition: all 0.3s ease;
}

textarea:focus {
  outline: none;
  border-color: #ff8e53;
  background: rgba(255, 255, 255, 0.1);
}

textarea::placeholder {
  color: rgba(255, 255, 255, 0.5);
}

.send-btn {
  padding: 8px 20px;
  border-radius: 20px;
  border: none;
  background: linear-gradient(135deg, #ff8e53, #ff6b6b);
  color: #fff;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  background: rgba(255, 255, 255, 0.1);
}

.send-btn:not(:disabled):hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(255, 142, 83, 0.3);
}

/* 加载状态样式 */
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

/* 表情选择器样式 */
.emoji-picker-wrapper {
  position: absolute;
  bottom: 100%;
  left: 16px;
  margin-bottom: 8px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.2);
  z-index: 1050;
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

.input-box {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 20px;
  padding: 8px 16px;
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
    height: 100%;
    border-radius: 0;
  }

  .comment-input {
    border-radius: 0;
    padding-bottom: calc(20px + env(safe-area-inset-bottom));
  }

  .interaction-bar {
    padding: 12px;
  }

  .action-item {
    padding: 6px 12px;
  }

  .action-item img {
    width: 20px;
    height: 20px;
  }

  .action-item span {
    font-size: 12px;
  }
}

/* 确保图片也不会超出容器 */
:deep(.post-content img) {
  max-width: 100%;
  height: auto;
  object-fit: contain;
}

/* 确保代码块也能自动换行 */
:deep(.post-content pre),
:deep(.post-content code) {
  white-space: pre-wrap;
  word-wrap: break-word;
  max-width: 100%;
  overflow-x: auto;
}
</style>
