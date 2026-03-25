<template>
  <a-modal
    :open="modelValue"
    @update:open="$emit('update:modelValue', $event)"
    title="空间详情"
    :footer="null"
    width="600px"
    class="space-detail-modal"
    @cancel="handleClose"
  >
    <div class="space-detail-content">
      <!-- 空间基本信息 -->
      <div class="space-header">
        <div class="space-owner">
          <a-avatar
            :size="64"
            :src="spaceDetail?.user?.userAvatar"
            class="owner-avatar"
          />
          <div class="owner-info">
            <h3 class="space-name">{{ spaceDetail?.spaceName }}</h3>
            <div class="owner-name">
              创建者：{{ spaceDetail?.user?.userName }}
              <span class="user-role">{{ spaceDetail?.user?.userRole === 'admin' ? '管理员' : '普通用户' }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 空间统计信息 -->
      <div class="space-stats">
        <div class="stat-item">
          <div class="stat-value">{{ formatSize(spaceDetail?.totalSize) }}</div>
          <div class="stat-label">已用空间</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">{{ formatSize(spaceDetail?.maxSize) }}</div>
          <div class="stat-label">总空间</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">{{ spaceDetail?.totalCount }}</div>
          <div class="stat-label">图片数量</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">{{ spaceDetail?.maxCount }}</div>
          <div class="stat-label">最大数量</div>
        </div>
      </div>

      <!-- 空间详细信息 -->
      <div class="space-info">
        <a-descriptions :column="1">
          <a-descriptions-item label="空间ID">
            <div class="id-container">
              <span>{{ spaceDetail?.id }}</span>
              <a-button class="copy-button" type="text" @click="copySpaceId">
                <CopyOutlined />
              </a-button>
            </div>
          </a-descriptions-item>
          <a-descriptions-item label="空间类型">
            <div class="space-type">
              {{ spaceDetail?.spaceType === 0 ? '私有空间' : '团队空间' }}
              <a-tag :color="spaceDetail?.spaceType === 0 ? '#ff8e53' : '#4096ff'">
                {{ spaceDetail?.spaceLevel === 0 ? '普通版' :
                spaceDetail?.spaceLevel === 1 ? '专业版' : '旗舰版' }}
              </a-tag>
            </div>
          </a-descriptions-item>
          <a-descriptions-item label="空间等级">
            Level {{ spaceDetail?.spaceLevel }}
          </a-descriptions-item>
          <a-descriptions-item label="创建时间">
            {{ formatTime(spaceDetail?.createTime) }}
          </a-descriptions-item>
          <a-descriptions-item label="最近更新">
            {{ formatTime(spaceDetail?.updateTime) }}
          </a-descriptions-item>
        </a-descriptions>
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, defineProps, defineEmits } from 'vue'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import { CopyOutlined } from '@ant-design/icons-vue'

const props = defineProps<{
  modelValue: boolean
  spaceDetail: any
}>()

const emit = defineEmits(['update:modelValue'])

const handleClose = () => {
  emit('update:modelValue', false)
}

// 格式化文件大小
const formatSize = (bytes: string | number) => {
  if (!bytes || isNaN(Number(bytes))) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const value = Number(bytes)
  const i = value === 0 ? 0 : Math.floor(Math.log(value) / Math.log(k))
  if (i < 0 || i >= sizes.length) return '0 B'
  return `${(value / Math.pow(k, i)).toFixed(2)} ${sizes[i]}`
}

// 格式化时间
const formatTime = (time: string) => {
  return time ? dayjs(time).format('YYYY-MM-DD HH:mm:ss') : '-'
}

// 格式化权限名称
const formatPermission = (perm: string) => {
  const permMap: Record<string, string> = {
    'spaceUser:manage': '成员管理',
    'picture:view': '查看图片',
    'picture:upload': '上传图片',
    'picture:edit': '编辑图片',
    'picture:delete': '删除图片'
  }
  return permMap[perm] || perm
}

// 复制空间ID
const copySpaceId = () => {
  if (props.spaceDetail?.id) {
    navigator.clipboard.writeText(props.spaceDetail.id.toString())
      .then(() => {
        message.success('已复制空间ID')
      })
      .catch(() => {
        message.error('复制失败，请手动复制')
      })
  }
}
</script>

<style scoped>
.space-detail-modal {
  :deep(.ant-modal-content) {
    border-radius: 16px;
    overflow: hidden;
  }
}

.space-detail-content {
  padding: 20px;
}

.space-header {
  margin-bottom: 24px;
}

.space-owner {
  display: flex;
  align-items: center;
  gap: 16px;
}

.owner-avatar {
  border: 2px solid #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.owner-info {
  flex: 1;
}

.space-name {
  font-size: 20px;
  font-weight: 600;
  margin: 0 0 4px 0;
  color: #1a1a1a;
}

.owner-name {
  font-size: 14px;
  color: #666;
}

.user-role {
  margin-left: 8px;
  font-size: 12px;
  color: #ff8e53;
  background: #fff6f3;
  padding: 2px 8px;
  border-radius: 10px;
}

.space-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
  padding: 16px;
  background: #f8fafc;
  border-radius: 12px;
}

.stat-item {
  text-align: center;
}

.stat-value {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 13px;
  color: #666;
}

.space-info {
  background: white;
  border-radius: 12px;
  padding: 16px;
}

.permission-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.id-container {
  display: flex;
  align-items: center;
  gap: 8px;
}

.copy-button {
  padding: 2px 4px;
  height: auto;
  font-size: 14px;
  color: #94a3b8;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;

  &:hover {
    color: #1a1a1a;
    background: #f1f5f9;
  }

  &:active {
    transform: scale(0.95);
  }
}

.space-type {
  display: flex;
  align-items: center;
  gap: 8px;
}

.space-type :deep(.ant-tag) {
  margin: 0;
  border: none;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .space-detail-modal {
    :deep(.ant-modal-content) {
      margin: 12px;
    }
  }

  .space-detail-content {
    padding: 16px;
  }

  .space-owner {
    gap: 12px;
  }

  .owner-avatar {
    width: 48px;
    height: 48px;
  }

  .space-name {
    font-size: 18px;
  }

  .space-stats {
    padding: 12px;
    gap: 12px;
  }

  .stat-value {
    font-size: 16px;
  }

  .stat-label {
    font-size: 12px;
  }
}
</style>
