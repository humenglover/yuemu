<template>
  <div id="globalSider">
    <!-- 已登录状态 -->
    <div class="sider-wrapper" v-if="loginUserStore.loginUser.id">
      <nav class="sider-nav">
        <div class="menu-list">
          <!-- Public Gallery -->
          <router-link to="/" class="menu-item" :class="{ active: route.path === '/' }">
            <i class="menu-icon">🖼️</i>
            <span>公共图库</span>
          </router-link>

          <!-- My Space -->
          <router-link to="/my_space" class="menu-item" :class="{ active: route.path === '/my_space' }">
            <i class="menu-icon">👤</i>
            <span>我的空间</span>
          </router-link>

          <!-- My Posts -->
          <router-link to="/my_ports" class="menu-item" :class="{ active: route.path === '/my_ports' }">
            <i class="menu-icon">📤</i>
            <span>我的发布</span>
          </router-link>

          <!-- Tools -->
          <router-link to="/tools" class="menu-item" :class="{ active: route.path === '/tools' }">
            <i class="menu-icon">🛠️</i>
            <span>实用工具</span>
          </router-link>

          <!-- Games -->
          <router-link to="/games" class="menu-item" :class="{ active: route.path === '/games' }">
            <i class="menu-icon">🎮</i>
            <span>休闲游戏</span>
          </router-link>

          <!-- Team Spaces Section -->
          <div class="team-section">
            <div class="team-header" @click="toggleTeamList">
              <span class="team-title">我的团队</span>
              <div class="team-actions">
                <button class="add-team-btn" @click.stop="handleAddTeam">
                  <i class="add-icon">+</i>
                </button>
                <i class="expand-icon" :class="{ expanded: isTeamExpanded }">▼</i>
              </div>
            </div>

            <transition name="fade">
              <div class="team-list" :class="{ expanded: isTeamExpanded }">
                <router-link
                  v-for="team in teamSpaceList"
                  :key="team.spaceId"
                  :to="`/space/${team.spaceId}`"
                  class="menu-item team-item"
                  :class="{ active: route.path === `/space/${team.spaceId}` }"
                >
                  <span>{{ team.space?.userId === userId ? `${team.space?.spaceName} (我的)` : team.space?.spaceName }}</span>
                </router-link>
              </div>
            </transition>
          </div>
        </div>
      </nav>
    </div>

    <!-- 未登录状态 -->
    <div class="sider-wrapper" v-else>
      <div class="login-prompt">
        <div class="login-icon">👋</div>
        <h3>欢迎来到悦木</h3>
        <p>登录后可以体验更多功能</p>
        <router-link to="/user/login" class="login-button">
          立即登录
        </router-link>
        <router-link to="/user/register" class="register-link">
          还没有账号？立即注册
        </router-link>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { SPACE_TYPE_ENUM } from '@/constants/space'
import { listMyTeamSpaceUsingPost } from '@/api/spaceUserController'

const loginUserStore = useLoginUserStore()
const router = useRouter()
const route = useRoute()
const userId = computed(() => loginUserStore.loginUser.id)
const isTeamExpanded = ref(false)

const teamSpaceList = ref<API.SpaceUserVO[]>([])

const handleAddTeam = (e: Event) => {
  e.stopPropagation()
  router.push('/add_space?type=' + SPACE_TYPE_ENUM.TEAM)
}

// 获取团队列表数据
const fetchTeamList = async () => {
  if (loginUserStore.loginUser.id) {
    try {
      const res = await listMyTeamSpaceUsingPost({})
      if (res.data.code === 0) {
        teamSpaceList.value = res.data.data ?? []
      }
    } catch (error) {
      console.error('获取团队空间列表失败:', error)
    }
  }
}

const toggleTeamList = async (e: Event) => {
  e.stopPropagation()
  isTeamExpanded.value = !isTeamExpanded.value

  // 展开时获取最新的团队列表数据
  if (isTeamExpanded.value) {
    await fetchTeamList()
  }
}

onMounted(async () => {
  // 组件挂载时获取团队列表
  await fetchTeamList()
})
</script>

<style scoped>
#globalSider {
  height: 100%;
  background: linear-gradient(180deg,
  rgba(255, 255, 255, 0.95) 0%,
  rgba(255, 255, 255, 0.85) 100%
  );
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-right: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow:
    4px 0 25px rgba(31, 41, 55, 0.04),
    0 0 10px rgba(0, 0, 0, 0.02) inset;
  position: relative;
  overflow: hidden;

}

#globalSider::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 150px;
  background: linear-gradient(180deg,
  rgba(255, 142, 83, 0.08) 0%,
  rgba(255, 142, 83, 0) 100%
  );
  pointer-events: none;
}

.sider-wrapper {
  height: 100%;
  width: 180px;
  position: relative;
  z-index: 1;
}

.sider-nav {
  height: 100%;
  padding: 16px 0;
  overflow-y: auto;
}

.menu-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 0 8px;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  border-radius: 12px;
  color: #64748b;
  text-decoration: none;
  transition: all 0.3s ease;
  font-weight: 500;
  background: transparent;
  position: relative;
  overflow: hidden;
}

.menu-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(120deg,
  rgba(255, 255, 255, 0) 0%,
  rgba(255, 255, 255, 0.3) 50%,
  rgba(255, 255, 255, 0) 100%
  );
  transform: translateX(-100%);
  transition: transform 0.6s ease;
}

.menu-item:hover::before {
  transform: translateX(100%);
}

.menu-item:hover {
  background: rgba(255, 255, 255, 0.5);
  color: #333;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.03);
}

.menu-item.active {
  color: #ff8e53;
  background: rgba(255, 142, 83, 0.08);
  box-shadow: 0 4px 12px rgba(255, 142, 83, 0.06);
}

.menu-icon {
  font-size: 18px;
  width: 22px;
  height: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.menu-item:hover .menu-icon {
  transform: scale(1.1);
}

.team-section {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  position: relative;
}

.team-section::before {
  content: '';
  position: absolute;
  top: -1px;
  left: 15%;
  right: 15%;
  height: 1px;
  background: linear-gradient(90deg,
  rgba(255, 142, 83, 0) 0%,
  rgba(255, 142, 83, 0.2) 50%,
  rgba(255, 142, 83, 0) 100%
  );
}

.team-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px 8px;
  cursor: pointer;
  user-select: none;
  transition: all 0.3s ease;
}

.team-header:hover {
  color: #ff8e53;
}

.team-header:hover .expand-icon {
  color: #ff8e53;
  transform: translateY(-1px);
}

.team-title {
  font-weight: 500;
  color: #64748b;
  font-size: 14px;
}

.team-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.add-team-btn {
  width: 22px;
  height: 22px;
  border-radius: 6px;
  border: none;
  background: transparent;
  color: #4f46e5;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.add-team-btn:hover {
  background: rgba(79, 70, 229, 0.1);
  transform: scale(1.1);
}

.add-icon {
  font-size: 18px;
  font-weight: 300;
}

.expand-icon {
  font-size: 12px;
  color: #64748b;
  transition: transform 0.3s ease;
}

.expand-icon.expanded {
  transform: rotate(180deg);
}

.team-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  height: 0;
  opacity: 0;
  overflow: hidden;
  transition: all 0.3s ease;
}

.team-list.expanded {
  height: auto;
  opacity: 1;
  margin-top: 8px;
}

.team-item {
  padding-left: 32px;
  font-size: 14px;
  opacity: 0;
  transform: translateY(-10px);
  transition: all 0.3s ease;
}

.team-list.expanded .team-item {
  opacity: 1;
  transform: translateY(0);
}

@media screen and (max-width: 992px) {
  .sider-wrapper {
    width: 160px;
  }

  .menu-item {
    padding: 8px 12px;
  }
}

/* Scrollbar Styling */
.sider-nav::-webkit-scrollbar {
  width: 4px;
}

.sider-nav::-webkit-scrollbar-track {
  background: transparent;
}

.sider-nav::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.1);
  border-radius: 4px;
}

.sider-nav::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.2);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 未登录状态样式 */
.login-prompt {
  padding: 24px;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.login-icon {
  font-size: 48px;
  margin-bottom: 8px;
  animation: wave 2s infinite;
}

.login-prompt h3 {
  font-size: 18px;
  color: #1a1a1a;
  margin: 0;
}

.login-prompt p {
  font-size: 14px;
  color: #64748b;
  margin: 0;
}

.login-button {
  display: inline-block;
  padding: 12px 32px;
  background: linear-gradient(135deg, #ff8e53, #ff6b6b);
  color: white;
  text-decoration: none;
  border-radius: 24px;
  font-weight: 500;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(255, 107, 107, 0.2);
  margin-top: 8px;
}

.login-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(255, 107, 107, 0.3);
}

.register-link {
  color: #64748b;
  text-decoration: none;
  font-size: 13px;
  transition: color 0.3s ease;
  margin-top: 8px;
}

.register-link:hover {
  color: #ff8e53;
}

@keyframes wave {
  0%, 100% {
    transform: rotate(0deg);
  }
  25% {
    transform: rotate(-10deg);
  }
  75% {
    transform: rotate(10deg);
  }
}

@media screen and (max-width: 992px) {
  .login-prompt {
    padding: 16px;
  }

  .login-icon {
    font-size: 40px;
  }

  .login-prompt h3 {
    font-size: 16px;
  }

  .login-prompt p {
    font-size: 13px;
  }

  .login-button {
    padding: 10px 24px;
    font-size: 14px;
  }
}
</style>
