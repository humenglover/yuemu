<template>
  <div class="unread-system-notify-list">
    <template v-if="safeNotifies && safeNotifies.length > 0">
      <div v-for="(notify, i) in safeNotifies" :key="notify?.id ?? i" class="notify-item">
        <!-- 核心：横向布局（图标+主体内容） -->
        <div class="notify-content-wrap">
          <!-- 左侧：图标（固定尺寸）- 全部改为非实心版本 -->
          <div class="notify-icon" :class="getNotifyTypeClass(notify.notifyType)">
            <i class="far fa-bell" v-if="notify.notifyType === 'ADMIN_ANNOUNCE'"></i>
            <i class="far fa-star" v-else-if="notify.notifyType === 'POST_SELECTED'"></i>
            <i class="far fa-trash-alt" v-else-if="notify.notifyType === 'POST_DELETED'"></i>
            <i class="far fa-edit" v-else-if="notify.notifyType === 'POST_UPDATED'"></i>
            <i class="far fa-user" v-else-if="notify.notifyType === 'ACCOUNT_CHANGED'"></i>
            <i class="far fa-exclamation-circle" v-else-if="notify.notifyType === 'SYSTEM_ALERT'"></i>
            <i class="far fa-bell" v-else></i>
          </div>

          <!-- 右侧：所有信息紧凑排列 -->
          <div class="notify-main">
            <!-- 通知信息（一行展示） -->
            <div class="notify-header">
              <span class="notify-type">{{ getNotifyTypeName(notify.notifyType) }}</span>
              <span class="notify-time">{{ formatTime(notify.createTime) }}</span>
            </div>

            <!-- 通知标题 -->
            <div class="notify-title">{{ notify.title || '无标题' }}</div>

            <!-- 通知内容（精简行数） -->
            <div class="notify-content">{{ notify.content || '' }}</div>
          </div>

          <!-- 标记已读按钮 - 非实心版本 -->
          <div class="mark-read-btn" @click="markAsRead(notify.id)">
            <i class="far fa-check"></i>
          </div>
        </div>
      </div>
    </template>
    <div v-else class="empty-state">
      <i class="far fa-bell empty-icon"></i>
      <p>暂无未读系统通知</p>
    </div>
  </div>
</template>
<script setup lang="ts">
import { formatDistanceToNow } from 'date-fns'
import { zhCN } from 'date-fns/locale'
import {
  BellOutlined,
  StarOutlined,
  DeleteOutlined,
  EditOutlined,
  UserOutlined,
  ExclamationCircleOutlined,
  CheckOutlined
} from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { computed } from 'vue'
import { markAsReadUsingPut } from '@/api/systemNotifyController'

const props = defineProps<{
  notifies: any[]
}>()

const emit = defineEmits(['refresh'])

const safeNotifies = computed(() => (props.notifies || []).filter((item) => !!item))

// 格式化时间（改为相对时间，更紧凑）
const formatTime = (time: string) => {
  try {
    return time ? formatDistanceToNow(new Date(time), { addSuffix: true, locale: zhCN }) : '-'
  } catch (error) {
    console.error('时间格式化错误:', error)
    return '-'
  }
}

// 获取通知类型对应的CSS类名
const getNotifyTypeClass = (type: string) => {
  const typeClassMap: Record<string, string> = {
    'ADMIN_ANNOUNCE': 'admin-announce',
    'POST_SELECTED': 'post-selected',
    'POST_DELETED': 'post-deleted',
    'POST_UPDATED': 'post-updated',
    'ACCOUNT_CHANGED': 'account-changed',
    'SYSTEM_ALERT': 'system-alert'
  }
  return typeClassMap[type] || 'default'
}

// 获取通知类型名称
const getNotifyTypeName = (type: string) => {
  const typeNameMap: Record<string, string> = {
    'ADMIN_ANNOUNCE': '管理员公告',
    'POST_SELECTED': '帖子精选',
    'POST_DELETED': '帖子删除',
    'POST_UPDATED': '帖子修改',
    'ACCOUNT_CHANGED': '账号变更',
    'SYSTEM_ALERT': '系统告警'
  }
  return typeNameMap[type] || '系统通知'
}

// 标记为已读
const markAsRead = async (id?: number) => {
  if (!id) return

  try {
    const res = await markAsReadUsingPut({ id })
    if (res.data?.code === 0) {
      message.success('已标记为已读')
      // 通知父组件刷新数据
      emit('refresh')
    } else {
      message.error(res.data?.message || '操作失败')
    }
  } catch (error) {
    console.error('标记已读失败:', error)
    message.error('操作失败')
  }
}
</script>

<style scoped>
.unread-system-notify-list {
  background: var(--header-background);
  color: var(--text-primary);
  border-radius: 12px;
  padding: 12px;
  width: 100%;
  box-sizing: border-box; /* 确保内边距不影响宽度 */
}

.notify-item {
  padding: 12px;
  border-bottom: 1px solid #f0f0f0;
  transition: all 0.2s ease;
  width: 100%;
  box-sizing: border-box;

  &:last-child {
    border-bottom: none;
  }

  &:hover {
    background: #f8fafc;
    border-radius: 8px;
  }
}

/* 核心：横向布局，压缩垂直高度 */
.notify-content-wrap {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  width: 100%;
  box-sizing: border-box;
}

.notify-icon {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  color: #fff;

  &.admin-announce {
    background: linear-gradient(135deg, #3b82f6, #60a5fa);
  }

  &.post-selected {
    background: linear-gradient(135deg, #f59e0b, #fbbf24);
  }

  &.post-deleted {
    background: linear-gradient(135deg, #ef4444, #f87171);
  }

  &.post-updated {
    background: linear-gradient(135deg, #8b5cf6, #a78bfa);
  }

  &.account-changed {
    background: linear-gradient(135deg, #10b981, #34d399);
  }

  &.system-alert {
    background: linear-gradient(135deg, #ec4899, #f472b6);
  }

  &.default {
    background: linear-gradient(135deg, #64748b, #94a3b8);
  }
}

.notify-main {
  flex: 1;
  min-width: 0;
}

/* 通知信息：一行紧凑排列，支持换行 */
.notify-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
  font-size: 13px;
  flex-wrap: wrap; /* 空间不足时自动换行 */
}

.notify-type {
  font-weight: 500;
  color: #1a1a1a;
  background: #f1f5f9;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 11px;
  white-space: nowrap;
}

.notify-time {
  font-size: 11px;
  color: #999;
  margin-left: auto;
  white-space: nowrap;
}

/* 通知标题：支持自动换行 */
.notify-title {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2; /* 最多显示2行 */
  -webkit-box-orient: vertical;
  word-break: break-word; /* 强制换行 */
}

/* 通知内容：精简样式，支持自动换行 */
.notify-content {
  font-size: 13px;
  color: #1a1a1a;
  line-height: 1.4;
  margin-bottom: 6px;
  padding: 0 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  word-break: break-word; /* 强制换行 */
}

/* 标记已读按钮 */
.mark-read-btn {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #64748b;
  cursor: pointer;
  flex-shrink: 0;
  transition: all 0.2s ease;

  &:hover {
    background: #3b82f6;
    color: #fff;
  }
}

/* 空状态：精简尺寸 */
.empty-state {
  text-align: center;
  padding: 32px 0;
  color: #666;
  width: 100%;
  box-sizing: border-box;
}

.empty-icon {
  font-size: 40px;
  color: #3b82f6;
  margin-bottom: 12px;
  opacity: 0.5;
}

.empty-state p {
  font-size: 13px;
  color: #666;
  margin: 0;
}

/* 移动端适配：进一步压缩并优化换行 */
@media screen and (max-width: 768px) {
  .notify-item {
    padding: 10px;
  }

  .notify-content {
    -webkit-line-clamp: 2; /* 恢复2行，保证可读性 */
    word-break: break-word;
  }

  .notify-title {
    font-size: 13px;
    -webkit-line-clamp: 2;
    word-break: break-word;
  }

  /* 移动端头部自适应换行 */
  .notify-header {
    flex-wrap: wrap;
  }

  .notify-time {
    margin-left: 0;
    margin-top: 2px;
    width: 100%;
    text-align: right;
  }
}
</style>
