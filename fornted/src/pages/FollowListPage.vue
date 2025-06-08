<template>
  <div class="page-container">


    <!-- 搜索框 -->
    <div class="search-container">
      <!-- 顶部导航 -->
      <div class="nav-container">
        <div class="nav-tabs">
          <div
            class="nav-tab"
            :class="{ active: activeTab === 'follow' }"
            @click="activeTab = 'follow'"
          >
            <span class="tab-text">关注</span>
            <span class="tab-count">{{ followCount }}</span>
          </div>
          <div
            class="nav-tab"
            :class="{ active: activeTab === 'fans' }"
            @click="activeTab = 'fans'"
          >
            <span class="tab-text">粉丝</span>
            <span class="tab-count">{{ fansCount }}</span>
          </div>
        </div>
      </div>
      <div class="search-box">
        <i class="search-icon">🔍</i>
        <input
          type="text"
          :placeholder="activeTab === 'follow' ? '搜索关注' : '搜索粉丝'"
          v-model="searchText"
          @input="handleSearch"
          class="search-input"
        />
        <div class="search-clear" @click="handleClear" v-if="searchText">✕</div>
      </div>
    </div>

    <!-- 用户列表 -->
    <div class="content-container" @scroll="handleScroll">
      <div class="user-list">
        <div v-for="user in userList" :key="user.id" class="user-card">
          <div class="user-info" @click="goToUserSpace(user)">
            <div class="avatar-wrapper">
              <img
                :src="user.userAvatar || getDefaultAvatar(user.userName)"
                class="avatar"
                alt="avatar"
              />
              <div class="avatar-border"></div>
            </div>
            <div class="user-details">
              <div class="username">{{ user.userName }}</div>
              <div class="user-id">
                <span class="id-label">ID:</span>
                <span class="id-value">{{ user.id }}</span>
              </div>
            </div>
          </div>
          <div class="action-area">
            <button
              v-if="activeTab === 'follow'"
              class="follow-btn unfollow"
              @click="handleUnfollow(user)"
            >
              取消关注
            </button>
            <button
              v-else
              class="follow-btn"
              :class="{ following: user.isFollowing }"
              @click="toggleFollow(user)"
            >
              {{ user.isFollowing ? '已关注' : '关注' }}
            </button>
          </div>
        </div>
      </div>

      <!-- 加载状态 -->
      <div class="loading-state" v-if="loading">
        <div class="loading-spinner"></div>
        <span>加载中...</span>
      </div>

      <!-- 空状态 -->
      <div class="empty-state" v-if="!loading && !userList.length">
        <div class="empty-icon">🤔</div>
        <div class="empty-text">{{ activeTab === 'follow' ? '还没有关注任何人' : '还没有粉丝' }}</div>
        <div class="empty-hint">{{ activeTab === 'follow' ? '去发现更多有趣的人吧' : '分享更多内容来吸引粉丝吧' }}</div>
        <button class="explore-btn" @click="goToHome">去首页看看</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { message } from 'ant-design-vue'
import { getFollowOrFanListUsingPost } from '@/api/userFollowsController'
import { addUserFollowsUsingPost } from '@/api/userFollowsController'

const router = useRouter()
const route = useRoute()
const loginUserStore = useLoginUserStore()
const activeTab = ref(route.query.tab?.toString() || 'follow')
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)
const currentPage = ref(1)
const pageSize = 20
const userList = ref<any[]>([])
const followCount = ref(0)
const fansCount = ref(0)
const searchText = ref('')

// 格式化输入，只允许数字
const idFormatter = (value: string) => {
  return value.replace(/[^\d]/g, '')
}

// 格式化计数
const formatCount = (count: number) => {
  if (count >= 10000) {
    return (count / 10000).toFixed(1) + 'w'
  } else if (count >= 1000) {
    return (count / 1000).toFixed(1) + 'k'
  }
  return count
}

// 加载数据
const loadMore = async () => {
  if (refreshing.value) {
    userList.value = []
    refreshing.value = false
  }

  if (loading.value) return
  loading.value = true

  try {
    const res = await getFollowOrFanListUsingPost({
      current: currentPage.value,
      pageSize: pageSize,
      followerId: activeTab.value === 'follow'
        ? loginUserStore.loginUser.id
        : (searchText.value ? searchText.value : undefined),
      followingId: activeTab.value === 'fans'
        ? loginUserStore.loginUser.id
        : (searchText.value ? searchText.value : undefined),
      searchType: activeTab.value === 'follow' ? 0 : 1
    })

    if (res.data.code === 0) {
      const newUsers = res.data.data.records || []
      if (currentPage.value === 1) {
        if (activeTab.value === 'follow') {
          followCount.value = res.data.data.total || 0
        } else {
          fansCount.value = res.data.data.total || 0
        }
      }
      userList.value.push(...newUsers)

      if (newUsers.length < pageSize) {
        finished.value = true
      } else {
        currentPage.value++
      }
    }
  } catch (error) {
    console.error('加载失败:', error)
    message.error('加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 下拉刷新
const onRefresh = async () => {
  finished.value = false
  currentPage.value = 1
  await loadMore()
}

// 切换关注状态
const toggleFollow = async (user: any) => {
  if (!loginUserStore.loginUser?.id) {
    message.warning('请先登录')
    return
  }

  user.loading = true
  try {
    const res = await addUserFollowsUsingPost({
      followerId: loginUserStore.loginUser.id,
      followingId: user.id,
      followingName: user.userName,
      followerName: loginUserStore.loginUser.userName,
      followStatus: user.isFollowing ? 0 : 1
    })

    if (res.data?.code === 0) {
      user.isFollowing = !user.isFollowing
      message.success(user.isFollowing ? '关注成功' : '已取消关注')
    }
  } catch (error) {
    console.error('操作失败:', error)
    message.error('操作失败，请稍后重试')
  } finally {
    user.loading = false
  }
}

// 跳转到用户空间
const goToUserSpace = (user: API.UserVO) => {
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

// 获取默认头像
const getDefaultAvatar = (userName: string) => {
  const defaultName = userName || 'Guest'
  return `https://api.dicebear.com/7.x/adventurer/svg?seed=${encodeURIComponent(defaultName)}&backgroundColor=ffd5dc,ffdfbf,ffd5dc`
}

// 监听路由参数变化
watch(() => route.query.tab, (newTab) => {
  if (newTab) {
    activeTab.value = newTab.toString()
  }
})

// 监听标签页切换
watch(activeTab, () => {
  // 清空搜索框
  searchText.value = ''
  // 重置列表状态
  currentPage.value = 1
  userList.value = []
  finished.value = false
  // 重新加载数据
  loadMore()
})

// 取消关注
const handleUnfollow = async (user: any) => {
  if (!loginUserStore.loginUser?.id) {
    message.warning('请先登录')
    return
  }

  try {
    const res = await addUserFollowsUsingPost({
      followerId: loginUserStore.loginUser.id,
      followingId: user.id,
      followingName: user.userName,
      followerName: loginUserStore.loginUser.userName,
      followStatus: 0
    })

    if (res.data?.code === 0) {
      message.success('已取消关注')
      // 重新加载数据
      currentPage.value = 1
      userList.value = []
      loadMore()
    }
  } catch (error) {
    console.error('操作失败:', error)
    message.error('操作失败，请稍后重试')
  }
}

// 跳转到首页
const goToHome = () => {
  router.push('/')
}

// 处理搜索
const handleSearch = () => {
  if (searchText.value && !/^\d+$/.test(searchText.value)) {
    message.error('请输入有效的用户ID')
    return
  }
  // 检查ID长度
  if (searchText.value && (searchText.value.length > 19 || searchText.value.length < 1)) {
    message.error('请输入有效的用户ID')
    return
  }
  currentPage.value = 1
  userList.value = []
  finished.value = false
  loadMore()
}

// 处理清除搜索
const handleClear = () => {
  searchText.value = ''
  handleSearch()
}

onMounted(() => {
  if (!loginUserStore.loginUser?.id) {
    message.warning('请先登录')
    router.replace('/user/login')
    return
  }
  loadMore()
})
</script>

<style scoped>
.page-container {
  min-height: 92vh;
  background: linear-gradient(135deg, #f0f7ff 0%, #fff5f5 100%);
  position: relative;
  margin: -28px!important;
  padding: 0;
  width: 100vw;
  overflow-x: hidden;
}

.nav-container {
  position: sticky;
  top: 0;

  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  padding: 16px 0;
  width: 100%;
  margin: 0;
}

.nav-tabs {
  display: flex;
  justify-content: center;
  gap: 32px;
  max-width: 800px;
  margin: 0 auto;
  padding: 0 20px;

}

.nav-tab {
  position: relative;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  border-radius: 12px;
}

.nav-tab.active {
  background: linear-gradient(135deg, #ff6b6b10 0%, #ffd93d10 100%);
}

.nav-tab.active::after {
  content: '';
  position: absolute;
  bottom: -17px;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  height: 3px;
  background: linear-gradient(to right, #ff6b6b, #ffd93d);
  border-radius: 3px;
}

.tab-text {
  font-size: 17px;
  font-weight: 600;
  color: #2d3436;
  transition: all 0.3s ease;
}

.nav-tab.active .tab-text {
  background: linear-gradient(135deg, #ff6b6b, #ffd93d);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.tab-count {
  font-size: 14px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 12px;
  background: linear-gradient(135deg, #ff6b6b20, #ffd93d20);
  color: #ff6b6b;
}

.search-container {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
  position: relative;

}

.search-box {
  display: flex;
  align-items: center;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 16px;
  padding: 12px 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.8);
  transition: all 0.3s ease;
  backdrop-filter: blur(10px);
  margin-top: 24px;
}

.search-box:focus-within {
  box-shadow: 0 4px 25px rgba(255, 107, 107, 0.1);
  border-color: rgba(255, 107, 107, 0.2);
}

.search-icon {
  font-size: 18px;
  margin-right: 12px;
  opacity: 0.5;
}

.search-input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 15px;
  color: #2d3436;
  background: transparent;
}

.search-input::placeholder {
  color: #b2bec3;
}

.search-clear {
  cursor: pointer;
  padding: 4px;
  color: #b2bec3;
  transition: all 0.3s ease;
}

.search-clear:hover {
  color: #ff6b6b;
}

.content-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 0 20px 20px;
  position: relative;

}

.user-list {
  display: flex;
  flex-direction: column;
}

.user-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
  border: 1px solid rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  margin-bottom: 16px;
}

.user-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.08);
  background: rgba(255, 255, 255, 0.95);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 16px;
  cursor: pointer;
}

.avatar-wrapper {
  position: relative;
  width: 56px;
  height: 56px;
  border-radius: 16px;
  overflow: hidden;
}

.avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 16px;
  transition: all 0.3s ease;
}

.avatar-border {
  position: absolute;
  inset: -2px;
  border-radius: 18px;
  background: linear-gradient(135deg, #ff6b6b, #ffd93d);
  opacity: 0;
  transition: all 0.3s ease;

}

.user-info:hover .avatar-border {
  opacity: 1;
  inset: -3px;
}

.user-details {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.username {
  font-size: 16px;
  font-weight: 600;
  color: #2d3436;
  line-height: 1.4;
}

.user-id {
  display: flex;
  align-items: center;
  gap: 6px;
}

.id-label {
  font-size: 13px;
  color: #b2bec3;
}

.id-value {
  font-size: 13px;
  color: #636e72;
  font-weight: 500;
}

.follow-btn {
  padding: 8px 24px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  border: none;
  background: linear-gradient(135deg, #ff6b6b, #ffd93d);
  color: white;
  box-shadow: 0 4px 15px rgba(255, 107, 107, 0.2);
}

.follow-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(255, 107, 107, 0.3);
}

.follow-btn.following {
  background: #f5f6fa;
  color: #636e72;
  box-shadow: none;
}

.follow-btn.unfollow {
  background: rgba(245, 246, 250, 0.95);
  color: #636e72;
  box-shadow: none;
  border: 1px solid rgba(0, 0, 0, 0.08);
  backdrop-filter: blur(10px);
}

.follow-btn.unfollow:hover {
  background: #fff1f1;
  color: #ff6b6b;
  border-color: #ffd8d8;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 40px 0;
  color: #636e72;
}

.loading-spinner {
  width: 30px;
  height: 30px;
  border: 3px solid #ff6b6b20;
  border-top-color: #ff6b6b;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 20px;
  text-align: center;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 24px;
  box-shadow: 0 4px 30px rgba(0, 0, 0, 0.05);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.8);
  margin-top: 40px;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 20px;
}

.empty-text {
  font-size: 18px;
  font-weight: 600;
  color: #2d3436;
  margin-bottom: 8px;
}

.empty-hint {
  font-size: 15px;
  color: #636e72;
  margin-bottom: 24px;
}

.explore-btn {
  padding: 12px 32px;
  border-radius: 16px;
  font-size: 15px;
  font-weight: 600;
  background: linear-gradient(135deg, #ff6b6b, #ffd93d);
  color: white;
  border: none;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 20px rgba(255, 107, 107, 0.2);
}

.explore-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 30px rgba(255, 107, 107, 0.3);
}

@media screen and (max-width: 768px) {
  .page-container {
    padding: 0;
  }

  .nav-container {
    padding: 12px 0;
  }

  .search-container {
    padding: 16px;
  }

  .content-container {
    padding: 0 16px 16px;
  }

  .user-card {
    padding: 16px;
    margin-bottom: 12px;
    border-radius: 16px;
  }

  .empty-state {
    margin: 20px 16px;
    padding: 40px 16px;
    border-radius: 20px;
  }

  .nav-tabs {
    gap: 20px;
    padding: 0 16px;
  }

  .nav-tab {
    padding: 6px 12px;
  }

  .tab-text {
    font-size: 16px;
  }

  .tab-count {
    font-size: 13px;
    padding: 3px 8px;
  }

  .search-box {
    padding: 10px 16px;
  }

  .avatar-wrapper {
    width: 48px;
    height: 48px;
  }

  .username {
    font-size: 15px;
  }

  .follow-btn {
    padding: 6px 20px;
    font-size: 13px;
  }

  .empty-text {
    font-size: 16px;
  }

  .empty-hint {
    font-size: 14px;
  }

  .explore-btn {
    padding: 10px 28px;
    font-size: 14px;
  }
}
</style>
