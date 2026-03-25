<template>
  <div id="MyTeamsPage">
    <!-- 头部区域 - 现代控制面板风格 -->
    <header class="dashboard-header">
      <div class="header-content">
        <div class="header-left">
          <h1 class="header-title">团队协作</h1>
          <p class="header-subtitle">管理你的团队空间，开启高效协作之旅</p>
        </div>
        <div class="header-right">
          <div class="stats-group">
            <div class="stat-item">
              <span class="stat-value">{{ teamSpaceList.length }}</span>
              <span class="stat-label">团队</span>
            </div>
            <div class="stat-divider"></div>
            <a-button type="primary" size="large" @click="handleAddTeam" class="create-btn">
              <template #icon><PlusOutlined /></template>
              创建团队
            </a-button>
          </div>
        </div>
      </div>
    </header>

    <main class="page-content">
      <!-- 列表容器 -->
      <div v-if="loading" class="loading-wrap">
        <a-spin size="large" />
      </div>

      <div v-else-if="teamSpaceList.length === 0" class="empty-state-wrap">
        <div class="empty-card">
          <div class="empty-illustration">
            <div class="circle-group">
              <div class="circle c1"></div>
              <div class="circle c2"></div>
              <div class="circle c3"></div>
            </div>
            <i class="fas fa-users-viewfinder empty-icon-main"></i>
          </div>
          <h3 class="empty-title">探索你的第一个团队</h3>
          <p class="empty-desc">在这里你可以创建团队，邀请成员共同管理、处理及分享图片资源</p>
          <a-button type="primary" size="large" @click="handleAddTeam" class="empty-btn">
            立即创建团队
          </a-button>
        </div>
      </div>

      <div v-else class="team-grid-container">
        <div class="grid-header">
          <div class="grid-title">
            <i class="fas fa-th-large"></i>
            我的团队空间
          </div>
        </div>
        <div class="team-list-grid">
          <TeamSpaceCard
            v-for="teamSpace in teamSpaceList"
            :key="teamSpace.id"
            :space="teamSpace"
            class="premium-team-card"
          />
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { PlusOutlined } from '@ant-design/icons-vue'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { SPACE_TYPE_ENUM } from '@/constants/space'
import { listMyTeamSpaceUsingPost } from '@/api/spaceUserController'
import TeamSpaceCard from '@/components/TeamSpaceCard.vue'

const router = useRouter()
const loginUserStore = useLoginUserStore()
const teamSpaceList = ref<API.SpaceVO[]>([])
const loading = ref(false)

onMounted(async () => {
  await fetchTeamList()
})

const fetchTeamList = async () => {
  loading.value = true
  try {
    const res = await listMyTeamSpaceUsingPost({})
    if (res.data.code === 0) {
      teamSpaceList.value = res.data.data ?? []
    }
  } catch (error) {
    console.error('获取团队列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleAddTeam = () => {
  router.push('/add_space?type=' + SPACE_TYPE_ENUM.TEAM)
}
</script>

<style scoped>
#MyTeamsPage {
  min-height: 100vh;
  background-color: var(--background);
  color: var(--text-primary);
  display: flex;
  flex-direction: column;
}

/* 头部样式 - 极简白风格 */
.dashboard-header {
  padding: 32px 24px 16px;
  background: var(--background);
  border-bottom: 1px solid var(--border-color);
  margin-bottom: 24px;
}

.header-content {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 24px;
}

.header-title {
  font-size: 28px;
  font-weight: 700;
  margin: 0 0 4px;
  color: var(--text-primary);
  letter-spacing: -0.5px;
}

.header-subtitle {
  font-size: 15px;
  color: var(--text-secondary);
  margin: 0;
}

.stats-group {
  display: flex;
  align-items: center;
  background: var(--fill-color);
  padding: 6px 16px;
  border-radius: 8px;
  border: 1px solid var(--border-color);
}

.stat-item {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--nav-item-active-text);
}

.stat-label {
  font-size: 13px;
  color: var(--text-secondary);
}

.stat-divider {
  width: 1px;
  height: 24px;
  background: var(--border-color);
  margin: 0 20px;
}

.create-btn {
  border-radius: 8px;
  height: 40px;
  padding: 0 20px;
  font-weight: 500;
  border: none;
  transition: all 0.2s ease;
}

.create-btn:hover {
  opacity: 0.9;
}

/* 页面内容区 */
.page-content {
  flex: 1;
  max-width: 1248px;
  margin: 0 auto;
  padding: 0 4px 6px;
  width: 100%;
}

.loading-wrap {
  display: flex;
  justify-content: center;
  padding: 100px 0;
}

/* 空状态 - 艺术化卡片 */
.empty-state-wrap {
  display: flex;
  justify-content: center;
  padding-top: 40px;
}

.empty-card {
  max-width: 480px;
  width: 100%;
  background: var(--background);
  border: 1px dashed var(--border-color);
  border-radius: 16px;
  padding: 60px 40px;
  text-align: center;
  position: relative;
}

.empty-icon-main {
  font-size: 40px;
  color: var(--text-secondary);
  margin-bottom: 24px;
}

.empty-title {
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 12px;
}

.empty-desc {
  font-size: 15px;
  color: var(--text-secondary);
  line-height: 1.6;
  margin-bottom: 32px;
}

.empty-btn {
  width: 100%;
  height: 52px;
  border-radius: 12px;
  font-weight: 600;
}

/* 团队网格列表 */
.team-grid-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.grid-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 4px;
}

.grid-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
}

.grid-title i {
  color: var(--nav-item-active-text);
}

.team-list-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 4px;
}

.premium-team-card {
  transition: all 0.4s cubic-bezier(0.23, 1, 0.32, 1);
}

/* 响应式适配 */
@media (max-width: 768px) {
  .header-content {
    flex-direction: column;
    align-items: flex-start;
    padding: 0 8px;
  }

  .header-right {
    width: 100%;
  }

  .stats-group {
    width: 100%;
    justify-content: space-between;
  }

  .header-title {
    font-size: 26px;
  }

  .team-list-grid {
    grid-template-columns: 1fr;
  }

  .empty-card {
    padding: 40px 24px;
    margin: 0 16px;
  }
}
</style>
