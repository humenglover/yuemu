<template>
  <div class="comment-list">
    <div v-for="comment in comments"
         :key="comment.commentId"
         class="comment-item">
      <div class="comment-container">
        <div class="avatar-container">
          <img
            :src="comment.commentUser?.userAvatar || getDefaultAvatar(comment.commentUser?.userName)"
            alt="User"
            class="user-avatar"
            @click="handleUserClick(comment.commentUser)"
          />
        </div>

        <div class="comment-content">
          <div class="comment-header">
            <div class="user-info">
              <span class="username">{{ comment.commentUser?.userName }}</span>
              <span v-if="comment.location" class="comment-location">
                <i class="fa fa-map-marker" aria-hidden="true"></i>
                {{ comment.location }}
              </span>
              <span class="comment-time">{{ formatTime(comment.createTime) }}</span>
            </div>
          </div>

          <div class="comment-text">
            {{ comment.content }}
          </div>

          <div class="comment-actions">
            <!-- 点赞（红色） -->
            <div class="action-item like-btn" @click="handleLike(comment)">
              <i class="fa" :class="likedComments.includes(comment.commentId) ? 'fa-heart' : 'fa-heart-o'" aria-hidden="true"></i>
              <span>{{ comment.likeCount || 0 }}</span>
            </div>
            <!-- 点踩（蓝色） -->
            <div class="action-item dislike-btn" @click="handleDislike(comment)">
              <i class="fa" :class="dislikedComments.includes(comment.commentId) ? 'fa-thumbs-down' : 'fa-thumbs-o-down'" aria-hidden="true"></i>
            </div>
            <div class="action-item" @click="handleReply(comment)">
              <i class="fa fa-comment-o" aria-hidden="true"></i>
              <span>回复</span>
            </div>
            <div v-if="canDelete(comment)" class="action-item delete" @click="handleDelete(comment)">
              <i class="fa fa-trash-o" aria-hidden="true"></i>
            </div>
          </div>
        </div>
      </div>
      <div v-if="hasReplies(comment)" class="comment-children">
        <div class="timeline-container">
          <div class="timeline-line"></div>
          <div class="timeline-content">
            <div v-for="(reply, index) in getAllReplies(comment)"
                 :key="reply.commentId"
                 class="comment-item reply-item">
              <div class="comment-container">
                <div class="avatar-container">
                  <img
                    :src="reply.commentUser?.userAvatar || getDefaultAvatar(reply.commentUser?.userName)"
                    alt="User"
                    class="user-avatar"
                    @click="handleUserClick(reply.commentUser)"
                  />
                  <div class="timeline-dot"></div>
                </div>
                <div class="comment-content">
                  <div class="comment-header">
                    <div class="user-info">
                      <span class="username">{{ reply.commentUser?.userName }}</span>
                      <span v-if="reply.location" class="comment-location">
                        <i class="fa fa-map-marker" aria-hidden="true"></i>
                        {{ reply.location }}
                      </span>
                      <span class="comment-time">{{ formatTime(reply.createTime) }}</span>
                    </div>
                  </div>
                  <div class="comment-text">
                    <template v-if="reply.targetUser">
                      <span class="reply-to">@{{ reply.targetUser.userName }}：</span>
                    </template>
                    {{ reply.content }}
                  </div>
                  <div class="comment-actions">
                    <div class="action-item like-btn" @click="handleLike(reply)">
                      <i class="fa" :class="likedComments.includes(reply.commentId) ? 'fa-heart' : 'fa-heart-o'" aria-hidden="true"></i>
                      <span>{{ reply.likeCount || 0 }}</span>
                    </div>
                    <div class="action-item dislike-btn" @click="handleDislike(reply)">
                      <i class="fa" :class="dislikedComments.includes(reply.commentId) ? 'fa-thumbs-down' : 'fa-thumbs-o-down'" aria-hidden="true"></i>
                      <span>{{ reply.dislikeCount || 0 }}</span>
                    </div>
                    <div class="action-item" @click="handleReply(reply)">
                      <span>回复</span>
                    </div>
                    <div v-if="canDelete(reply)" class="action-item delete" @click="handleDelete(reply)">
                      <i class="fa fa-trash-o" aria-hidden="true"></i>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
@import url("https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.7.0/css/font-awesome.min.css");

.comment-list {
  width: 100%;
  box-sizing: border-box;
}

.comment-item {
  margin-bottom: 12px;
  transition: all 0.3s ease;
}

.comment-container {
  display: flex;
  gap: 12px;
  padding: 8px 16px;
  border-radius: 16px;
  transition: background-color 0.2s ease;
}

.comment-container:hover {
  background-color: var(--comment-hover-background);
}

.avatar-container {
  flex-shrink: 0;
  position: relative;
}

.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  cursor: pointer;
  transition: transform 0.2s ease;
  border: 1px solid var(--comment-avatar-border);
  position: relative;
  z-index: 2;
}

.user-avatar:hover {
  transform: scale(1.05);
}

.comment-content {
  flex: 1;
  min-width: 0;
}

.comment-header {
  margin-bottom: 4px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.username {
  font-weight: 600;
  font-size: 14px;
  color: var(--comment-username-color);
}

.comment-location {
  font-size: 12px;
  color: var(--comment-time-color);
  display: flex;
  align-items: center;
  gap: 2px;
}

.comment-location .fa {
  font-size: 10px;
  color: var(--comment-reply-username-color);
  display: inline-block !important;
}

.comment-time {
  color: var(--comment-time-color);
  font-size: 12px;
}

.comment-text {
  margin: 4px 0 8px;
  color: var(--comment-text-color);
  line-height: 1.4;
  word-break: break-word;
  font-size: 14px;
}

.reply-to {
  color: var(--comment-reply-username-color);
  font-size: 14px;
  opacity: 0.8;
}

.comment-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.action-item {
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  color: var(--comment-action-color);
  font-size: 12px;
  transition: color 0.2s ease;
}

.action-item .fa {
  font-family: 'FontAwesome' !important;
  display: inline-block !important;
  font-size: 14px;
  color: inherit;
}

/* 点赞按钮样式（红色） */
.like-btn {
  color: var(--comment-action-color);
}
.like-btn .fa-heart {
  color: #ff2442 !important; /* 点赞选中红色 */
}
.like-btn:hover {
  color: #ff2442;
}

/* 点踩按钮样式（蓝色） */
.dislike-btn {
  color: var(--comment-action-color);
}
.dislike-btn .fa-thumbs-down {
  color: #165dff !important; /* 点踩选中蓝色 */
}
.dislike-btn:hover {
  color: #165dff;
}

.action-item:hover {
  color: var(--comment-action-hover-color);
}

.action-item.delete:hover {
  color: var(--comment-delete-hover-color);
}

.action-item:hover .fa {
  transform: scale(1.1);
  transition: transform 0.2s ease;
}

.comment-children {
  margin-top: 8px;
  margin-left: 24px;
}

.timeline-container {
  display: flex;
  position: relative;
}

.timeline-line {
  width: 1px;
  background-color: var(--comment-reply-username-color);
  opacity: 0.24;
  position: absolute;
  left: 17px;
  top: 0;
  bottom: 0;
  z-index: 1;
}

.timeline-content {
  flex: 1;
  padding-left: 20px;
}

.reply-item {
  position: relative;
}

.timeline-dot {
  position: absolute;
  left: -20px;
  top: 18px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: var(--comment-reply-username-color);
  opacity: 0.4;
  z-index: 3;
  border: 2px solid var(--comment-item-background);
}

@media (max-width: 768px) {
  .comment-list {
    padding: 8px 0;
  }

  .comment-container {
    padding: 6px 12px;
  }

  .user-avatar {
    width: 32px;
    height: 32px;
  }

  .comment-actions {
    gap: 12px;
  }

  .action-item {
    font-size: 11px;
  }

  .comment-children {
    margin-left: 20px;
  }

  .timeline-line {
    left: 15px;
  }

  .timeline-dot {
    width: 6px;
    height: 6px;
    left: -18px;
    top: 16px;
  }
}
</style>

<script lang="ts" setup>
import { ref } from 'vue'
import { deleteCommentUsingPost, likeCommentUsingPost } from '@/api/commentsController.ts'
import { message, Modal } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import { useRouter } from 'vue-router'
import { getDefaultAvatar } from '@/utils/userUtils'
import moment from 'moment'

const router = useRouter()
const loginUserStore = useLoginUserStore()

const likedComments = ref<string[]>([])
const dislikedComments = ref<string[]>([])

interface CommentUser {
  id?: string
  userAccount?: string
  userAvatar?: string
  userName?: string
}

interface Comment {
  commentId?: string
  userId?: string
  targetId?: string
  targetType?: number
  targetUserId?: string
  content?: string
  parentId?: string | null
  likeCount?: string
  dislikeCount?: string
  createTime?: string
  commentUser?: CommentUser
  targetUser?: CommentUser
  children?: Comment[]
  isExpanded?: boolean
  isRead?: number
  picture?: string | null
  post?: string | null
  location?: string | null
}

const props = defineProps<{
  comments: Comment[]
}>()

const emit = defineEmits<{
  (e: 'reply-clicked', commentId: string): void
  (e: 'update-comments'): void
}>()

const hasReplies = (comment: Comment) => {
  return comment.children && comment.children.length > 0
}

const getAllReplies = (comment: Comment) => {
  const allReplies: Comment[] = []
  const collectReplies = (replies: Comment[] | undefined, parentComment?: Comment) => {
    if (!replies) return
    replies.forEach(reply => {
      if (parentComment) {
        reply.targetUser = parentComment.commentUser
      }
      allReplies.push(reply)
      if (reply.children) {
        collectReplies(reply.children, reply)
      }
    })
  }
  collectReplies(comment.children, comment)
  return allReplies
}

const handleLike = async (comment: Comment) => {
  try {
    if (likedComments.value.includes(comment.commentId)) {
      return
    }
    const likeCountDelta = 1
    let dislikeCountDelta = 0
    likedComments.value.push(comment.commentId)
    if (dislikedComments.value.includes(comment.commentId)) {
      dislikedComments.value = dislikedComments.value.filter((id) => id !== comment.commentId)
      dislikeCountDelta = -1
    }
    const requestBody = {
      commentId: comment.commentId,
      likeCount: likeCountDelta,
      dislikeCount: dislikeCountDelta,
    }
    const res = await likeCommentUsingPost(requestBody)
    if (res.data.code === 0 ) {
      comment.likeCount = String(Number(comment.likeCount || 0) + likeCountDelta)
      comment.dislikeCount = String(Number(comment.dislikeCount || 0) + dislikeCountDelta)
    } else {
      likedComments.value = likedComments.value.filter(id => id !== comment.commentId)
      if (dislikeCountDelta !== 0) {
        dislikedComments.value.push(comment.commentId)
      }
      message.error(res.data.message || '操作失败')
    }
  } catch (error) {
    let dislikeCountDelta = 0
    likedComments.value = likedComments.value.filter(id => id !== comment.commentId)
    if (dislikeCountDelta !== 0) {
      dislikedComments.value.push(comment.commentId)
    }
    message.error('操作失败')
  }
}

const handleDislike = async (comment: Comment) => {
  try {
    if (dislikedComments.value.includes(comment.commentId)) {
      return
    }
    let likeCountDelta = 0
    const dislikeCountDelta = 1
    dislikedComments.value.push(comment.commentId)
    if (likedComments.value.includes(comment.commentId)) {
      likedComments.value = likedComments.value.filter((id) => id !== comment.commentId)
      likeCountDelta = -1
    }
    const requestBody = {
      commentId: comment.commentId,
      likeCount: likeCountDelta,
      dislikeCount: dislikeCountDelta,
    }
    const res = await likeCommentUsingPost(requestBody)
    if (res.data.code === 0) {
      comment.likeCount = String(Number(comment.likeCount || 0) + likeCountDelta)
      comment.dislikeCount = String(Number(comment.dislikeCount || 0) + dislikeCountDelta)
    } else {
      dislikedComments.value = dislikedComments.value.filter(id => id !== comment.commentId)
      if (likeCountDelta !== 0) {
        likedComments.value.push(comment.commentId)
      }
      message.error(res.data.message || '操作失败')
    }
  } catch (error) {
    let likeCountDelta = 0
    dislikedComments.value = dislikedComments.value.filter(id => id !== comment.commentId)
    if (likeCountDelta !== 0) {
      likedComments.value.push(comment.commentId)
    }
    message.error('操作失败')
  }
}

const handleDelete = async (comment: Comment) => {
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除该评论吗？此操作不可撤销哦！',
    okText: '确定',
    cancelText: '取消',
    zIndex: 2000,
    getContainer: () => document.body,
    maskStyle: { zIndex: 1999 },
    async onOk() {
      try {
        const res = await deleteCommentUsingPost({ commentId: comment.commentId })
        if (res.data.code === 0) {
          emit('update-comments')
        }
      } catch (error) {
        message.error('删除失败')
      }
    }
  })
}

const canDelete = (comment: Comment) => {
  return comment.commentUser?.id === loginUserStore.loginUser?.id
}

const handleReply = (comment: Comment) => {
  emit('reply-clicked', comment.commentId!)
}

const handleUserClick = (user: CommentUser | undefined) => {
  if (user?.id) {
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
}

const formatTime = (time: string | undefined) => {
  if (!time) return ''
  return moment(time).format('YYYY-MM-DD')
}
</script>
