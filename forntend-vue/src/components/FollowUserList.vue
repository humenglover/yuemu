<template>
  <div v-if="props.mode === 'list'" class="follow-users-container">
    <div v-if="!props.users || props.users.length === 0" class="empty-following-small">
      <p class="empty-tip">暂无关注</p>
    </div>
    <div v-else class="follow-users-list">
      <div
        v-for="user in props.users"
        :key="user.id"
        class="follow-user-item"
        @click="$emit('item-click', user.id)"
      >
        <img :src="user.userAvatar || getDefaultAvatar(user.userName)" :alt="user.userName" class="follow-user-avatar" />
        <span class="follow-user-name">{{ user.userName }}</span>
      </div>
      <!-- 查看更多按钮 -->
      <div class="follow-user-item view-more-item" @click="$emit('more-click')">
        <div class="view-more-avatar">
          <i class="fas fa-plus"></i>
        </div>
        <span class="follow-user-name">查看更多</span>
      </div>
    </div>
  </div>

  <div v-else-if="props.mode === 'stack'" class="following-filter-content">
    <div class="avatar-stack-v3">
      <div v-for="(user, index) in props.users.slice(0, maxCount)"
           :key="user.id"
           class="stacked-avatar-wrapper-v3"
           :style="{ zIndex: 50 - index }"
           @click="$emit('item-click', user.id)">
        <img :src="user.userAvatar || getDefaultAvatar(user.userName)" class="stacked-avatar-v3" />
        <div class="name-tooltip-v3">{{ user.userName }}</div>
      </div>
      <div v-if="props.users.length > maxCount" class="stacked-more-v3" @click="$emit('more-click')">
        +{{ props.users.length - maxCount }}
      </div>
    </div>
    <div class="following-tip-text">
      <span class="tip-inner-v3">最近关注的 {{ props.users.length }} 位创作者</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { getDefaultAvatar } from '@/utils/userUtils'

interface User {
  id: string | number
  userName: string
  userAvatar?: string
}

const props = withDefaults(defineProps<{
  users?: User[]
  mode?: 'list' | 'stack'
  loading?: boolean
  maxCount?: number
}>(), {
  users: () => [],
  mode: 'list',
  loading: false,
  maxCount: 12
})

defineEmits(['item-click', 'more-click'])
</script>

<style scoped>
/* 共同样式或通过类名区分 */

/* List 模式 (移动端/Banner下) */
.follow-users-container {
  padding: 15px 10px;
  background: #fff;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

.follow-users-list {
  display: flex;
  gap: 15px;
  min-width: max-content;
}

.follow-user-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  width: 60px;
  cursor: pointer;
  transition: transform 0.2s;
}

.follow-user-item:active {
  transform: scale(0.95);
}

.follow-user-avatar {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #f0f2f5;
}

.follow-user-name {
  font-size: 11px;
  color: #666;
  width: 100%;
  text-align: center;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.view-more-item .view-more-avatar {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  border: 2px dashed #dcdfe6;
}

/* Stack 模式 (PC 导航) */
.following-filter-content {
  display: flex;
  align-items: center;
  gap: 8px; /* 减小间距，为文字留出空间 */
  flex-wrap: nowrap;
  overflow: hidden;
  max-width: 100%;
}

.avatar-stack-v3 {
  display: flex;
  align-items: center;
}

.stacked-avatar-wrapper-v3 {
  position: relative;
  margin-right: -15px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
}

.stacked-avatar-wrapper-v3:hover {
  transform: translateY(-8px) scale(1.15);
  z-index: 100 !important;
}

.stacked-avatar-v3 {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  border: 3px solid #fff;
  box-shadow: 0 4px 15px rgba(0,0,0,0.1);
  object-fit: cover;
}

.name-tooltip-v3 {
  position: absolute;
  top: -36px;
  left: 50%;
  transform: translateX(-50%);
  background: #111827;
  color: #fff;
  padding: 5px 10px;
  border-radius: 6px;
  font-size: 11px;
  white-space: nowrap;
  opacity: 0;
  visibility: hidden;
  transition: all 0.2s;
  pointer-events: none;
  z-index: 100;
}

.stacked-avatar-wrapper-v3:hover .name-tooltip-v3 {
  opacity: 1;
  visibility: visible;
  top: -42px;
}

.stacked-more-v3 {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: #f1f5f9;
  border: 3px solid #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  color: #475569;
  font-weight: 700;
  margin-left: 10px;
  box-shadow: 0 4px 10px rgba(0,0,0,0.05);
  cursor: pointer;
}

.following-tip-text {
  font-size: 14px;
  color: #64748b;
  font-weight: 500;
  margin-left: 10px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
  min-width: 0;
}

.follow-users-container::-webkit-scrollbar {
  display: none;
}

.empty-following-small {
  padding: 10px;
  text-align: center;
}

.empty-tip {
  font-size: 12px;
  color: #999;
}

/* 响应式适配移动端堆叠效果 */
@media screen and (max-width: 768px) {
  .following-filter-content {
    gap: 12px;
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
    width: 100%;
  }

  .following-filter-content::-webkit-scrollbar {
    display: none;
  }

  .stacked-avatar-v3, .stacked-more-v3 {
    width: 38px;
    height: 38px;
    border-width: 2px;
  }

  .stacked-avatar-wrapper-v3 {
    margin-right: -12px;
  }

  .following-tip-text {
    font-size: 11px;
    white-space: nowrap !important;
    overflow: hidden !important;
    text-overflow: ellipsis !important;
    flex: 1;
    min-width: 0;
    margin-left: 4px;
    display: block;
    max-width: 120px; /* 物理限制宽度，解决省略号不显示问题 */
  }
}
</style>
