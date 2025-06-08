<template>
  <div>
    <a-comment v-for="(comment, index) in comments" :key="index" style="margin-bottom: 5px">
      <template #actions>
        <span key="comment-basic-like">
          <a-tooltip>
            <template v-if="likedComments.includes(comment.commentId)">
              <LikeFilled @click="like(comment, true)" />
            </template>
            <template v-else>
              <LikeOutlined @click="like(comment, true)" />
            </template>
          </a-tooltip>
          <span style="padding-left: 8px; cursor: auto">
            {{ comment.likeCount }}
          </span>
        </span>
        <span key="comment-basic-dislike">
          <a-tooltip>
            <template v-if="dislikedComments.includes(comment.commentId)">
              <DislikeFilled @click="dislike(comment, false)" />
            </template>
            <template v-else>
              <DislikeOutlined @click="dislike(comment, false)" />
            </template>
          </a-tooltip>
          <span style="padding-left: 8px; cursor: auto">
            {{ comment.dislikeCount }}
          </span>
        </span>
        <span
          key="comment-basic-delete"
          style="font-size: 12px"
          v-if="comment.commentUser?.id === loginUserStore.loginUser?.id"
          @click="(e) => doDelete(comment, e)"
        ><DeleteOutlined
        /></span>
        <span
          key="comment-nested-reply-to"
          style="font-size: 12px"
          @click.stop="handleReplyClick(comment.commentId)"
        >
          回复
        </span>
        <span
          v-if="comment.children && comment.children.length > 0"
          :key="`expand-${comment.commentId}`"
          @click="toggleExpand(comment)"
        >{{ comment.isExpanded ? '折叠' : '展开' }}</span
        >
      </template>
      <template #author>
        <a>{{ comment.commentUser?.userName }}</a>
      </template>
      <template #avatar>
        <a-avatar
          :src="comment.commentUser?.userAvatar || getDefaultAvatar(comment.commentUser?.userName)"
          alt="User"
          class="user-avatar"
          @click="handleUserClick(comment.commentUser)"
        />
      </template>
      <template #content>
        <p>
          {{ comment.content }}
        </p>
      </template>
      <template #datetime>
        <a-tooltip>
          <span key="comment-nested-reply-to" style="font-size: 12px">{{
              formatTime(comment.createTime)
            }}</span>
        </a-tooltip>
      </template>
      <CommentList
        class="comment-list-wrapper"
        v-if="comment.children && comment.children.length > 0 && comment.isExpanded"
        :comments="comment.children"
        @reply-clicked="handleReplyClick"
      />
    </a-comment>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, defineProps, defineEmits } from 'vue'
import {
  LikeFilled,
  LikeOutlined,
  DislikeFilled,
  DislikeOutlined,
  DeleteOutlined,
  RightOutlined,
} from '@ant-design/icons-vue'
import moment from 'moment'
import { deleteCommentUsingPost, likeCommentUsingPost } from '@/api/commentsController.ts'
import { message, Modal } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import { useRouter } from 'vue-router'
import { getDefaultAvatar } from '@/utils/userUtils'

const router = useRouter()

const loginUserStore = useLoginUserStore()

interface Comment {
  current?: number
  pageSize?: number
  sortField?: string
  sortOrder?: string
  commentId?: string
  pictureId?: number
  content?: string
  createTime?: string
  parentCommentId?: string
  likeCount?: string
  dislikeCount?: string
  children?: Comment[]
  commentUser?: {
    id?: number
    userAccount?: string
    userAvatar?: string
    userName?: string
  }
  isExpanded?: boolean
}

const props = defineProps<{
  comments: Comment[]
}>()

const emit = defineEmits(['update-comments'])

const likedComments = ref<string[]>([])
const dislikedComments = ref<string[]>([])

const formatTime = (time: string | undefined): string => {
  if (!time) return ''
  const commentTime = new Date(time)
  const now = new Date()
  const diff = now.getTime() - commentTime.getTime()
  const hours = Math.floor(diff / (1000 * 60 * 60))
  if (hours < 12) {
    if (hours === 0) {
      const minutes = Math.floor(diff / (1000 * 60))
      if (minutes === 0) {
        return '刚刚'
      }
      return `${minutes} 分钟前`
    }
    return `${hours} 小时前`
  }
  return moment(commentTime).format('YYYY-MM-DD')
}

const like = async (comment: Comment) => {
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

    await likeCommentUsingPost(requestBody)

    comment.likeCount = (parseInt(comment.likeCount) + likeCountDelta).toString()
    comment.dislikeCount = (parseInt(comment.dislikeCount) + dislikeCountDelta).toString()
  } catch (error) {
    console.error('点赞操作失败', error)
  }
}

const dislike = async (comment: Comment) => {
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

    await likeCommentUsingPost(requestBody)

    comment.likeCount = (parseInt(comment.likeCount) + likeCountDelta).toString()
    comment.dislikeCount = (parseInt(comment.dislikeCount) + dislikeCountDelta).toString()
  } catch (error) {
    console.error('点踩操作失败', error)
  }
}

const doDelete = async (comment: Comment, e: Event) => {
  e.stopPropagation()
  const requestBody: API.CommentsDeleteRequest = {
    commentId: comment.commentId,
  }
  try {
    Modal.confirm({
      title: '确认删除',
      content: '确定要删除该评论吗？此操作不可撤销哦！',
      okText: '确定',
      cancelText: '取消',
      onOk: async () => {
        const res = await deleteCommentUsingPost(requestBody)
        if (res.data) {
          emit('update-comments')
        } else {
          message.error('评论删除失败：服务器未返回成功信息')
        }
      },
      onCancel: () => {
        message.info('已取消删除操作')
      },
    })
  } catch (error) {
    console.error('删除评论操作失败', error)
    message.error('删除评论操作失败，请稍后重试')
  }
}

const toggleExpand = (comment: Comment) => {
  comment.isExpanded = !comment.isExpanded
}

const handleReplyClick = (commentId: string) => {
  // console.log('CommentList - 回复被点击，评论 ID:', commentId)
  emit('reply-clicked', commentId.toString())
  // console.log('CommentList - 回复被点击，评论 ID:', commentId.toString())
}

// 处理用户头像点击
const handleUserClick = (user: any) => {
  if (user?.id) {
    router.push(`/user/${user.id}`)
  }
}
</script>

<style scoped>
.ant-comment :deep(.ant-comment-inner) {
  padding: 1px 0;
}

.ant-comment :deep(.ant-comment-actions) {
  margin-top: 4px;
  margin-bottom: 8px;
}

:deep(.comment-list-wrapper) {
  padding-left: -100px !important;
}

.user-avatar {
  cursor: pointer;
  transition: transform 0.2s ease;
}

.user-avatar:hover {
  transform: scale(1.1);
}
</style>
