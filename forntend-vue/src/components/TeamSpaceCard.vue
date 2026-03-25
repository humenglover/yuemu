<template>
  <div class="team-space-card" @click="handleTeamClick(space)">
    <!-- 空间封面 -->
    <div class="card-cover" :class="{ 'cover-loaded': coverLoaded }">
      <img
        v-if="space.spaceCover"
        :src="space.spaceCover"
        alt="空间封面"
        class="cover-img"
        loading="lazy"
        @load="coverLoaded = true"
        @error="handleCoverError"
      />
      <img
        v-else
        :src="defaultCover"
        alt="默认封面"
        class="cover-img"
        @load="coverLoaded = true"
      />
      <div class="cover-overlay"></div>
      <div class="space-type-badge private" v-if="space.spaceType === 0">私有空间</div>
    </div>

    <!-- 空间信息 -->
    <div class="card-info">
      <h3 class="space-title">{{ space.spaceName }}</h3>
      <p class="space-desc" v-if="space.spaceDesc">{{ space.spaceDesc }}</p>
      <p class="space-desc empty-desc" v-else>暂无空间简介</p>
    </div>

    <!-- 底部互动栏: 头像堆叠 + 最新活动 -->
    <div class="card-footer">
      <div class="members-group" v-if="recommendedMembers.length > 0">
        <div class="avatar-stack">
          <img
            v-for="(member, index) in recommendedMembers.slice(0, 3)"
            :key="member.userId"
            :src="member.user?.userAvatar || defaultAvatar"
            class="mini-avatar"
            :style="{ zIndex: 3 - index }"
            @click.stop="handleUserClick(member.user)"
          />
          <span class="more-members-count" v-if="recommendedMembers.length > 3">
            +{{ recommendedMembers.length - 3 }}
          </span>
        </div>
      </div>
      <div class="no-members" v-else>暂无成员</div>

      <div
        class="latest-activity-pill"
        v-if="activities.length > 0"
        @click.stop="handleActivityClick(activities[0])"
      >
        <i class="fas fa-calendar-alt"></i>
        <span class="activity-text">{{ activities[0].title }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import defaultCover from '@/assets/images/default_space_cover.png'
import defaultAvatar from '@/assets/default.png'

const props = defineProps<{
  space: API.SpaceVO
}>()

const router = useRouter()
const coverLoaded = ref(false)

const handleCoverError = () => {
  coverLoaded.value = true
}

const handleActivityClick = (activity: API.Activity) => {
  if (activity.id) {
    router.push(`/activity/detail/${activity.id}`)
  }
}

const handleTeamClick = (space: API.SpaceVO) => {
  if (space.id) {
    router.push(`/space/${space.id}`)
  }
}

const handleUserClick = (user: API.User) => {
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

// 统一成员与活动属性
const recommendedMembers = (props.space.recommendedUsers || []) as API.SpaceUserVO[]
const activities = (props.space.activities || []) as API.Activity[]
</script>

<style scoped>
.team-space-card {
  width: 100%;
  background: var(--card-background);
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  border: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  cursor: pointer;
  position: relative;
}

.team-space-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.08);
  border-color: var(--primary-color);
}

/* Cover Styling */
.card-cover {
  width: 100%;
  padding-top: 50%; /* 2:1 ratio for extremely modern and thin look */
  position: relative;
  background: var(--hover-background);
  overflow: hidden;
}

.cover-img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s ease;
}

.team-space-card:hover .cover-img {
  transform: scale(1.05);
}

.cover-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(to top, rgba(0,0,0,0.3) 0%, transparent 60%);
  pointer-events: none;
}

.space-type-badge {
  position: absolute;
  top: 10px;
  right: 10px;
  background: rgba(24, 144, 255, 0.85);
  color: #fff;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
  backdrop-filter: blur(4px);
  z-index: 2;
}

.space-type-badge.private {
  background: rgba(245, 108, 108, 0.85);
}

/* Info Styling */
.card-info {
  padding: 16px 16px 12px;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.space-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 6px 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.space-desc {
  font-size: 12px;
  color: var(--text-secondary);
  margin: 0;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.empty-desc {
  font-style: italic;
  opacity: 0.6;
}

/* Footer Styling */
.card-footer {
  padding: 0 16px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.members-group {
  display: flex;
  align-items: center;
}

.avatar-stack {
  display: flex;
  align-items: center;
}

.mini-avatar {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid var(--card-background);
  margin-left: -8px;
  background: var(--background);
  transition: transform 0.2s;
  cursor: pointer;
}

.mini-avatar:first-child {
  margin-left: 0;
}

.mini-avatar:hover {
  transform: translateY(-2px);
  z-index: 10 !important;
}

.more-members-count {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: var(--hover-background);
  border: 2px solid var(--card-background);
  margin-left: -8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  color: var(--text-secondary);
  font-weight: 600;
  z-index: 0;
}

.no-members {
  font-size: 12px;
  color: var(--text-secondary);
  opacity: 0.7;
}

/* Pill styling for latest activity */
.latest-activity-pill {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 5px 10px;
  background: var(--hover-background);
  border-radius: 100px;
  max-width: 140px;
  transition: all 0.2s ease;
  cursor: pointer;
}

.latest-activity-pill:hover {
  background: rgba(24, 144, 255, 0.1);
}

.latest-activity-pill i {
  font-size: 12px;
  color: var(--primary-color);
  flex-shrink: 0;
}

.activity-text {
  font-size: 11px;
  color: var(--text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-weight: 500;
  transition: color 0.2s ease;
}

.latest-activity-pill:hover .activity-text {
  color: var(--primary-color);
}
</style>
