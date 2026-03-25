<template>
  <div class="unified-message-card">
    <div class="card-header-floating">
      <div class="header-left">
        <component :is="getIconComponent()" class="header-icon" :class="messageType" />
        <span class="header-title">{{ getTitle() }}</span>
      </div>
      <div class="header-right" v-if="unreadCount > 0">
        <span class="unread-badge">{{ unreadCount }}</span>
      </div>
    </div>

    <div class="card-content">
      <!-- 加载状态 -->
      <div v-if="loading" class="loading-state">
        <a-spin size="large" />
        <p>加载中...</p>
      </div>

      <!-- 所有消息列表，根据isRead字段区分显示 -->
      <transition-group
        v-if="allMessages && allMessages.length > 0"
        name="message-list"
        tag="div"
        class="message-list-grid"
      >
        <div
          v-for="(message, index) in allMessages"
          :key="getMessageKey(message, index)"
          class="message-item"
          :class="{ 'read-item': isMessageRead(message) }"
          @click="props.messageType !== 'system' ? handleMessageClick(message) : undefined"
        >
          <div class="message-content">
            <!-- 添加头像显示 -->
            <a-avatar
              v-if="messageType !== 'system'"
              :src="getUserAvatar(message) || defaultImage"
              :size="36"
              class="user-avatar"
            />

            <div class="message-main">
              <div class="message-header">
                <div class="header-info">
                  <component
                    v-if="messageType === 'system'"
                    :is="getSystemNotificationIcon(message.notifyType)"
                    class="notification-icon"
                    :class="getNotificationIconClass(message.notifyType)"
                  />
                  <span class="user-name">{{ getUserName(message) }}</span>
                  <!-- 添加类型标签 -->
                  <span
                    v-if="messageType === 'system'"
                    class="type-tag"
                    :class="getNotificationTagClass(message.notifyType)"
                  >
                    {{ getNotificationTagText(message.notifyType) }}
                  </span>
                  <!-- 添加描述信息 -->
                  <span
                    v-if="messageType === 'comment'"
                    class="message-desc"
                  >
                    评论了你的
                  </span>
                  <span
                    v-else-if="messageType === 'like'"
                    class="message-desc"
                  >
                    赞了你的
                  </span>
                  <span
                    v-else-if="messageType === 'share'"
                    class="message-desc"
                  >
                    分享了你的
                  </span>
                </div>
                <span class="message-time">{{ formatTime(getMessageTime(message)) }}</span>
              </div>

              <!-- 评论内容和目标信息 -->
              <div class="message-comment" v-if="messageType === 'comment'">
                <!-- 评论内容 -->
                <div class="comment-content">{{ message.content }}</div>
                <!-- 评论目标 -->
                <div
                  class="comment-target"
                  @click="handleTargetClick(message)"
                >
                  <div
                    class="target-card"
                    :class="{
                      'picture-card': message.targetType === 1,
                      'post-card': message.targetType === 2
                    }"
                  >
                    <template v-if="message.targetType === 1 && message.picture">
                      <!-- 图片类型：左图右文 -->
                      <img
                        :src="safeUrl(message.picture.thumbnailUrl) || defaultImage"
                        :alt="message.picture.name || '图片已失效'"
                        @error="handleImageError"
                        class="target-img"
                      >
                      <div class="target-info">
                        <div class="target-type">
                          <PictureOutlined class="type-icon" />
                          <span>图片</span>
                        </div>
                        <div class="target-title">{{ message.picture.name || '图片已失效' }}</div>
                      </div>
                    </template>
                    <template v-else-if="message.targetType === 1">
                      <img :src="defaultImage" alt="图片已失效" class="target-img" />
                      <div class="target-info">
                        <div class="target-type">
                          <PictureOutlined class="type-icon" />
                          <span>图片</span>
                        </div>
                        <div class="target-title">图片已失效</div>
                      </div>
                    </template>
                    <template v-else-if="message.targetType === 2 && message.post">
                      <!-- 帖子类型：纯文字紧凑展示 -->
                      <div class="target-info">
                        <div class="target-type">
                          <FileTextOutlined class="type-icon" />
                          <span>帖子</span>
                        </div>
                        <div class="target-title">{{ message.post.title || '帖子已失效' }}</div>
                        <div class="target-content">{{ message.post.content ? stripHtml(message.post.content) : '' }}</div>
                      </div>
                    </template>
                    <template v-else-if="message.targetType === 2">
                      <div class="target-info">
                        <div class="target-type">
                          <FileTextOutlined class="type-icon" />
                          <span>帖子</span>
                        </div>
                        <div class="target-title">帖子已失效</div>
                      </div>
                    </template>
                  </div>
                </div>
              </div>

              <!-- 系统通知内容 -->
              <div class="system-notification" v-else-if="messageType === 'system'">
                <div class="system-title">{{ message.title }}</div>
                <div class="system-content">{{ message.content }}</div>
              </div>

              <!-- 点赞和分享的目标信息 -->
              <div class="message-target" v-else>
                <div
                  class="target-card"
                  :class="{
                    'picture-card': message.targetType === 1,
                    'post-card': message.targetType === 2
                  }"
                  @click="handleTargetClick(message)"
                >
                  <template v-if="message.targetType === 1 && message.target">
                    <!-- 图片类型：左图右文 -->
                    <img
                      :src="safeUrl(message.target.thumbnailUrl) || defaultImage"
                      :alt="message.target.name || '图片已失效'"
                      @error="handleImageError"
                      class="target-img"
                    >
                    <div class="target-info">
                      <div class="target-type">
                        <PictureOutlined class="type-icon" />
                        <span>图片</span>
                      </div>
                      <div class="target-title">{{ message.target.name || '图片已失效' }}</div>
                    </div>
                  </template>
                  <template v-else-if="message.targetType === 1">
                    <img :src="defaultImage" alt="图片已失效" class="target-img" />
                    <div class="target-info">
                      <div class="target-type">
                        <PictureOutlined class="type-icon" />
                        <span>图片</span>
                      </div>
                      <div class="target-title">图片已失效</div>
                    </div>
                  </template>
                  <template v-else-if="message.targetType === 2 && message.target">
                    <!-- 帖子类型：纯文字紧凑展示 -->
                    <div class="target-info">
                      <div class="target-type">
                        <FileTextOutlined class="type-icon" />
                        <span>帖子</span>
                      </div>
                      <div class="target-title">{{ message.target.title || '帖子已失效' }}</div>
                      <div class="target-content">{{ message.target.content ? stripHtml(message.target.content) : '' }}</div>
                    </div>
                  </template>
                  <template v-else-if="message.targetType === 2">
                    <div class="target-info">
                      <div class="target-type">
                        <FileTextOutlined class="type-icon" />
                        <span>帖子</span>
                      </div>
                      <div class="target-title">帖子已失效</div>
                    </div>
                  </template>
                </div>
              </div>
            </div>

            <div class="message-status">
              <!-- 根据消息是否已读显示不同状态 -->
              <div v-if="!isMessageRead(message)" class="mark-read-btn" @click.stop="markAsRead(message)" title="标记为已读">
                <CheckOutlined />
              </div>
              <div v-else class="read-indicator">
                <CheckCircleOutlined />
              </div>
            </div>
          </div>
        </div>
      </transition-group>

      <!-- 空状态 -->
      <div v-else class="empty-state">
        <component :is="getEmptyIcon()" class="empty-icon" />
        <p>暂无{{ getTitle() }}</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// @ts-nocheck - 忽略类型检查错误，因为这些是开发环境缺少类型声明导致的
import { formatDistanceToNow } from 'date-fns'
import { zhCN } from 'date-fns/locale'
import { useRouter } from 'vue-router'
import { Spin as ASpin, Avatar as AAvatar } from 'ant-design-vue'
import { ref, onMounted, computed, onUnmounted, nextTick } from 'vue'
import {
  CommentOutlined,
  LikeOutlined,
  ShareAltOutlined,
  BellOutlined,
  CheckOutlined,
  CheckCircleOutlined,
  FileImageOutlined,
  FileTextOutlined,
  StarOutlined,
  ExclamationCircleOutlined,
  PictureOutlined
} from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { markSingleAsReadUsingPost } from '@/api/messageCenterController'
import defaultImage from '@/assets/default.png'

const router = useRouter()

const props = defineProps<{
  messageType: 'comment' | 'like' | 'share' | 'system'
  allMessages: any[]  // 修改为接收所有消息的数组
  unreadCount: number
  loading?: boolean  // 添加加载状态属性
}>()

// 添加一个计算属性来处理加载状态
const isLoading = computed(() => {
  // 如果父组件传递了loading属性，使用父组件的值
  if (props.loading !== undefined) {
    return props.loading
  }
  // 如果没有传递loading属性，默认根据消息数组是否存在来判断
  return !props.allMessages
})

const emit = defineEmits(['refresh'])

// 获取标题
const getTitle = () => {
  switch (props.messageType) {
    case 'comment': return '评论'
    case 'like': return '点赞'
    case 'share': return '分享'
    case 'system': return '系统通知'
    default: return '消息'
  }
}

// 获取图标组件
const getIconComponent = () => {
  switch (props.messageType) {
    case 'comment': return CommentOutlined
    case 'like': return LikeOutlined
    case 'share': return ShareAltOutlined
    case 'system': return BellOutlined
    default: return BellOutlined
  }
}

// 获取系统通知的图标组件
const getSystemNotificationIcon = (notifyType: string) => {
  switch (notifyType) {
    case 'PICTURE_APPROVED':
      return FileImageOutlined
    case 'PICTURE_REJECTED':
      return ExclamationCircleOutlined
    case 'POST_APPROVED':
      return FileTextOutlined
    case 'POST_REJECTED':
      return ExclamationCircleOutlined
    case 'PICTURE_FEATURED':
      return StarOutlined
    case 'POST_FEATURED':
      return StarOutlined
    default:
      return BellOutlined
  }
}

// 获取系统通知图标的CSS类
const getNotificationIconClass = (notifyType: string) => {
  switch (notifyType) {
    case 'PICTURE_APPROVED':
      return 'picture-approved'
    case 'PICTURE_REJECTED':
      return 'picture-rejected'
    case 'POST_APPROVED':
      return 'post-approved'
    case 'POST_REJECTED':
      return 'post-rejected'
    case 'PICTURE_FEATURED':
      return 'picture-featured'
    case 'POST_FEATURED':
      return 'post-featured'
    default:
      return 'default'
  }
}

// 获取系统通知标签文本
const getNotificationTagText = (notifyType: string) => {
  switch (notifyType) {
    case 'PICTURE_APPROVED':
      return '图片通过'
    case 'PICTURE_REJECTED':
      return '图片未通过'
    case 'POST_APPROVED':
      return '帖子通过'
    case 'POST_REJECTED':
      return '帖子未通过'
    case 'PICTURE_FEATURED':
      return '图片精选'
    case 'POST_FEATURED':
      return '帖子精选'
    default:
      return '系统通知'
  }
}

// 获取系统通知标签的CSS类
const getNotificationTagClass = (notifyType: string) => {
  switch (notifyType) {
    case 'PICTURE_APPROVED':
      return 'tag-picture-approved'
    case 'PICTURE_REJECTED':
      return 'tag-picture-rejected'
    case 'POST_APPROVED':
      return 'tag-post-approved'
    case 'POST_REJECTED':
      return 'tag-post-rejected'
    case 'PICTURE_FEATURED':
      return 'tag-picture-featured'
    case 'POST_FEATURED':
      return 'tag-post-featured'
    default:
      return 'tag-default'
  }
}

// 获取空状态图标
const getEmptyIcon = () => {
  switch (props.messageType) {
    case 'comment': return CommentOutlined
    case 'like': return LikeOutlined
    case 'share': return ShareAltOutlined
    case 'system': return BellOutlined
    default: return BellOutlined
  }
}

// 获取消息唯一键
const getMessageKey = (msg: any, index: number) => {
  return msg?.id ?? msg?.commentId ?? msg?.likeId ?? msg?.shareId ?? index
}

// 格式化时间
const formatTime = (time: string) => {
  try {
    return time ? formatDistanceToNow(new Date(time), { addSuffix: true, locale: zhCN }) : '-'
  } catch (error) {
    console.error('时间格式化错误:', error)
    return '-'
  }
}

// 获取用户名
const getUserName = (msg: any) => {
  if (props.messageType === 'comment') {
    return msg.commentUser?.userName || '未知用户'
  } else if (props.messageType === 'like') {
    return msg.user?.userName || '未知用户'
  } else if (props.messageType === 'share') {
    return msg.user?.userName || '未知用户'
  } else if (props.messageType === 'system') {
    return '系统通知'
  }
  return '未知用户'
}

// 获取用户头像
const getUserAvatar = (msg: any) => {
  if (props.messageType === 'comment') {
    return msg.commentUser?.userAvatar
  } else if (props.messageType === 'like') {
    return msg.user?.userAvatar
  } else if (props.messageType === 'share') {
    return msg.user?.userAvatar
  }
  return ''
}

// 获取消息时间
const getMessageTime = (msg: any) => {
  switch (props.messageType) {
    case 'comment': return msg.createTime
    case 'like': return msg.lastLikeTime
    case 'share': return msg.shareTime
    case 'system': return msg.createTime
    default: return ''
  }
}

// 获取目标名称
const getTargetName = (msg: any) => {
  // 对于评论消息，目标对象在 picture 或 post 字段中
  if (props.messageType === 'comment') {
    if (msg.targetType === 1 && msg.picture) {
      return msg.picture.name || '图片'
    } else if (msg.targetType === 2 && msg.post) {
      return msg.post.title || '帖子'
    }
  } else {
    // 对于点赞和分享消息，目标对象在 target 字段中
    if (msg.targetType === 1 && msg.target) {
      return msg.target.name || '图片'
    } else if (msg.targetType === 2 && msg.target) {
      return msg.target.title || '帖子'
    }
  }
  return '未知目标'
}

// 判断消息是否已读
const isMessageRead = (msg: any) => {
  // 系统通知使用readStatus字段，其他类型使用isRead字段
  if (props.messageType === 'system') {
    return msg.readStatus === 1
  }
  return msg.isRead === 1
}

// 标记为已读（内部使用）
const markAsReadInternal = async (msg: any) => {
  try {
    // 对于系统通知，使用不同的ID字段
    let id;
    if (props.messageType === 'system') {
      id = msg.id;
    } else {
      id = msg.id || msg.commentId || msg.likeId || msg.shareId;
    }

    if (!id) {
      console.error('消息ID不存在')
      return false
    }

    const res = await markSingleAsReadUsingPost({
      type: props.messageType,
      id
    })

    if (res.data?.code === 0) {
      // 更新消息的已读状态
      if (props.messageType === 'system') {
        msg.readStatus = 1
      } else {
        msg.isRead = 1
      }
      return true
    } else {
      console.error(res.data?.message || '操作失败')
      return false
    }
  } catch (error) {
    console.error('标记已读失败:', error)
    return false
  }
}

// 处理消息点击
const handleMessageClick = async (msg: any) => {
  try {
    // 对于系统通知，不进行跳转
    if (props.messageType === 'system') {
      return
    }

    // 对于评论消息，点击消息主体应该跳转到目标页面，而不是标记为已读
    if (props.messageType === 'comment') {
      // 直接跳转到目标页面
      // 注意：评论消息的数据结构中，目标对象在 picture 或 post 字段中
      if (msg.targetType === 1 && msg.picture?.id) {
        // 跳转到图片详情页
        router.push(`/picture-redirect/${msg.picture.id}`)
      } else if (msg.targetType === 2 && msg.post?.id) {
        // 跳转到帖子详情页
        router.push(`/post/${msg.post.id}`)
      }
      return
    }

    // 对于点赞和分享消息，点击消息主体应该标记为已读并跳转到目标页面
    // 标记消息为已读
    const success = await markAsReadInternal(msg)
    if (success) {
      // 通知父组件刷新数据
      emit('refresh')
    }

    // 根据targetType跳转到相应页面
    if (msg.targetType === 1 && msg.target?.id) {
      router.push(`/picture-redirect/${msg.target.id}`)
    } else if (msg.targetType === 2 && msg.target?.id) {
      router.push(`/post/${msg.target.id}`)
    }
  } catch (error) {
    console.error('消息点击处理错误:', error)
  }
}

// 处理目标点击
const handleTargetClick = async (msg: any) => {
  try {
    // 对于系统通知，不进行跳转
    if (props.messageType === 'system') {
      return
    }

    // 标记消息为已读
    const success = await markAsReadInternal(msg)
    if (success) {
      // 通知父组件刷新数据
      emit('refresh')
    }

    // 对于所有类型，根据targetType跳转到相应页面
    // 注意：评论消息的数据结构中，目标对象在 picture 或 post 字段中
    if (props.messageType === 'comment') {
      if (msg.targetType === 1 && msg.picture?.id) {
        router.push(`/picture-redirect/${msg.picture.id}`)
      } else if (msg.targetType === 2 && msg.post?.id) {
        router.push(`/post/${msg.post.id}`)
      }
    } else {
      // 点赞和分享消息使用 target 字段
      if (msg.targetType === 1 && msg.target?.id) {
        router.push(`/picture-redirect/${msg.target.id}`)
      } else if (msg.targetType === 2 && msg.target?.id) {
        router.push(`/post/${msg.target.id}`)
      }
    }
  } catch (error) {
    console.error('目标点击处理错误:', error)
  }
}

// 标记为已读
const markAsRead = async (msg: any) => {
  try {
    const success = await markAsReadInternal(msg)
    if (success) {
      message.success('已标记为已读')
      // 通知父组件刷新数据
      emit('refresh')
    } else {
      message.error('操作失败')
    }
  } catch (error) {
    console.error('标记已读失败:', error)
    message.error('操作失败')
  }
}

// 处理图片加载错误
const handleImageError = (e: Event) => {
  const img = e.target as HTMLImageElement
  img.src = defaultImage
  img.classList.add('error')
}

// 安全URL处理
const safeUrl = (url?: string) => {
  if (!url) return ''
  return url.replace(/`/g, '').trim()
}

// 去除HTML标签
const stripHtml = (html: string) => {
  if (!html) return ''
  return html.replace(/<[^>]+>/g, '').slice(0, 100) + '...'
}
</script>

<style scoped>
.unified-message-card {
  background: transparent;
  width: 100%;
  margin-bottom: 32px;
}

.card-header-floating {
  padding: 16px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid var(--border-color);
  background: var(--card-background);
  border-top-left-radius: 12px;
  border-top-right-radius: 12px;
}

.unread-badge {
  background: #ff4d4f;
  color: white;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 600;
  box-shadow: 0 2px 4px rgba(255, 77, 79, 0.3);
}

.card-content {
  padding: 0;
}

.loading-state {
  text-align: center;
  padding: 40px 20px;
  color: #999;
}

.loading-state p {
  margin-top: 12px;
  font-size: 14px;
}

.message-list-grid {
  display: flex;
  flex-direction: column;
  gap: 0;
  width: 100%;

  @media screen and (min-width: 769px) {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(440px, 1fr));
    gap: 20px;
    padding: 24px;
    box-sizing: border-box;
  }
}

.message-item {
  padding: 16px;
  background: var(--card-background);
  border-bottom: 1px solid var(--border-color);
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;

  @media screen and (min-width: 769px) {
    border-radius: 12px;
    border: 1px solid var(--border-color);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 16px rgba(0, 0, 0, 0.08);
      border-color: #4299e1;
    }
  }

  &.read-item {
    opacity: 0.85;
    background: var(--hover-background);
    @media screen and (min-width: 769px) {
      border-color: transparent;
      box-shadow: none;
    }
  }

  /* 未读提示点 */
  &:not(.read-item)::before {
    content: '';
    position: absolute;
    left: 8px;
    top: 50%;
    transform: translateY(-50%);
    width: 6px;
    height: 6px;
    background: #4299e1;
    border-radius: 50%;
    box-shadow: 0 0 8px rgba(66, 153, 225, 0.6);
  }
}


.message-content {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
}

.user-avatar {
  flex-shrink: 0;
}

.message-main {
  flex: 1;
  min-width: 0;
}

.message-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
}

.header-info {
  display: flex;
  align-items: center;
  gap: 6px;
}

.notification-icon {
  font-size: 16px;
}

.notification-icon.picture-approved {
  color: #52c41a;
}

.notification-icon.picture-rejected {
  color: #ff4d4f;
}

.notification-icon.post-approved {
  color: #1890ff;
}

.notification-icon.post-rejected {
  color: #ff4d4f;
}

.notification-icon.picture-featured {
  color: #faad14;
}

.notification-icon.post-featured {
  color: #faad14;
}

/* 系统通知类型标签样式 */
.type-tag {
  font-size: 12px;
  padding: 2px 6px;
  border-radius: 4px;
  font-weight: 500;
}

.type-tag.tag-picture-approved {
  background-color: #f6ffed;
  color: #52c41a;
  border: 1px solid #b7eb8f;
}

.type-tag.tag-picture-rejected {
  background-color: #fff2f0;
  color: #ff4d4f;
  border: 1px solid #ffccc7;
}

.type-tag.tag-post-approved {
  background-color: #e6f7ff;
  color: #1890ff;
  border: 1px solid #91d5ff;
}

.type-tag.tag-post-rejected {
  background-color: #fff2f0;
  color: #ff4d4f;
  border: 1px solid #ffccc7;
}

.type-tag.tag-picture-featured {
  background-color: #fffbe6;
  color: #faad14;
  border: 1px solid #ffe58f;
}

.type-tag.tag-post-featured {
  background-color: #fffbe6;
  color: #faad14;
  border: 1px solid #ffe58f;
}

.user-name {
  font-weight: 600;
  color: var(--text-primary);
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.message-desc {
  color: #4299e1;
  font-size: 12px;
}

.message-time {
  font-size: 12px;
  color: #999;
  white-space: nowrap;
}

/* 评论内容样式 */
.message-comment {
  margin-bottom: 8px;
}

.comment-content {
  font-size: 14px;
  color: var(--text-primary);
  line-height: 1.5;
  margin-bottom: 8px;
  padding: 10px;
  background: var(--hover-background);
  border-radius: 8px;
}

/* 评论目标样式 */
.comment-target {
  border-radius: 8px;
  padding: 8px;
  border: 1px solid #f0f0f0;
  cursor: pointer;
}

.comment-target:hover {
  background: #f1f5f9;
  border-color: #e2e8f0;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.04);
}

.message-body {
  font-size: 13px;
  color: #333;
  line-height: 1.4;
  margin-bottom: 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  cursor: pointer; /* 确保评论内容可点击 */
}

/* 系统通知内容样式 - 完整显示，不使用省略号 */
.system-notification {
  margin-bottom: 8px;
}

.system-title {
  font-weight: 500;
  margin-bottom: 4px;
  color: #333;
}

.system-content {
  color: #666;
  line-height: 1.5;
  white-space: pre-wrap; /* 保留空白符和换行符 */
  word-wrap: break-word; /* 允许长单词换行 */
  word-break: break-word; /* 在适当的地方换行 */
  overflow-wrap: break-word; /* 确保长内容能换行 */
}

.message-target {

  border-radius: 6px;
  padding: 8px;
}

.target-card {
  display: flex;
  flex-direction: column;
}

/* 图片类型：左图右文 */
.picture-card {
  flex-direction: row;
  align-items: center;
  gap: 10px;
}

.target-img {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 6px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  flex-shrink: 0;
}

.target-img.error {
  opacity: 0.5;
  filter: grayscale(100%);
}

.target-info {
  flex: 1;
  min-width: 0;
}

.target-type {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #4299e1;
  font-size: 11px;
  margin-bottom: 4px;
}

.type-icon {
  font-size: 12px;
}

.target-title {
  font-weight: 500;
  color: #1a1a1a;
  font-size: 13px;
  margin-bottom: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.target-content {
  font-size: 11px;
  color: #666;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.message-status {
  display: flex;
  align-items: flex-start;
}

.mark-read-btn {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #e6f4ff;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #1677ff;
  cursor: pointer;
  flex-shrink: 0;
  transition: all 0.2s ease;
}

.mark-read-btn:hover {
  background: #1677ff;
  color: white;
}

.read-indicator {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #00b96b;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.empty-state {
  text-align: center;
  padding: 40px 20px;
  color: #999;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.3;
}

.empty-state p {
  margin: 0;
  font-size: 14px;
}

/* 滚动条样式 */
.message-list::-webkit-scrollbar {
  width: 6px;
}

.message-list::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.message-list::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.message-list::-webkit-scrollbar-thumb:hover {
  background: #a1a1a1;
}
</style>
