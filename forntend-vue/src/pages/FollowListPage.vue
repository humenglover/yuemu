<template>
  <div class="page-container">
    <div class="nav-container">
      <div class="nav-header">
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
        <button class="graph-btn" @click="openGraphModal">
          <i class="fas fa-project-diagram graph-icon"></i>
          <span class="graph-text">关系图谱</span>
        </button>
      </div>
    </div>

    <div class="search-box">
      <fs-icon name="search" class="search-icon"></fs-icon>
      <a-select
        v-model:value="searchType"
        class="search-type-select"
      >
        <a-select-option value="id">ID</a-select-option>
        <a-select-option value="name">用户名</a-select-option>
        <a-select-option value="account">账号</a-select-option>
        <a-select-option value="profile">简介</a-select-option>
      </a-select>
      <input
        type="text"
        :placeholder="getSearchPlaceholder()"
        v-model="searchText"
        @input="handleSearch"
        class="search-input"
      />
      <fs-icon name="close" class="search-clear" @click="handleClear" v-if="searchText"></fs-icon>
    </div>

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
            </div>
            <div class="user-details">
              <div class="username">{{ user.userName }}</div>
              <div class="user-id">
                <fs-icon name="idcard" class="id-icon"></fs-icon>
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
              <fs-icon name="user-unfollow" class="btn-icon"></fs-icon>
              <span>取关</span>
            </button>
            <button
              v-else
              class="follow-btn"
              :class="{ following: user.isFollowing }"
              @click="toggleFollow(user)"
            >
              <fs-icon v-if="!user.isFollowing" name="user-add" class="btn-icon"></fs-icon>
              <fs-icon v-if="user.isFollowing" name="check" class="btn-icon"></fs-icon>
              <span>{{ user.isFollowing ? '已关注' : '关注' }}</span>
            </button>
          </div>
        </div>
      </div>

      <div class="loading-state" v-if="loading">
        <fs-icon name="loading" class="loading-icon"></fs-icon>
        <span>加载中...</span>
      </div>

      <div class="empty-state" v-if="!loading && !userList.length">
        <fs-icon :name="activeTab === 'follow' ? 'user-follow-empty' : 'user-star-empty'" class="empty-icon"></fs-icon>
        <div class="empty-text">{{ activeTab === 'follow' ? '还没有关注任何人' : '还没有粉丝' }}</div>
        <div class="empty-hint">{{ activeTab === 'follow' ? '去发现更多有趣的人吧' : '分享更多内容来吸引粉丝吧' }}</div>
        <button class="explore-btn" @click="goToHome">
          <fs-icon name="home" class="btn-icon"></fs-icon>
          <span>去首页看看</span>
        </button>
      </div>
    </div>

    <div class="graph-modal-mask" v-if="graphModalVisible" @click="closeGraphModal">
      <div class="graph-modal-content" @click.stop>
        <div class="graph-modal-header">
          <h3>{{ activeTab === 'follow' ? '我的关注关系图谱' : '我的粉丝关系图谱' }}</h3>
          <button class="modal-close-btn" @click="closeGraphModal">
            <i class="fas fa-times close-icon"></i>
          </button>
        </div>
        <div class="graph-modal-body">
          <div id="userGraph" class="graph-container"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, nextTick, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { message } from 'ant-design-vue'
import { getFollowOrFanListUsingPost } from '@/api/userFollowsController'
import { addUserFollowsUsingPost } from '@/api/userFollowsController'
import * as echarts from 'echarts'

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
const searchType = ref('id')
const searchText = ref('')
const searchDebounceTimer = ref<number | null>(null)
const graphModalVisible = ref(false)
let graphChart: echarts.ECharts | null = null

const idFormatter = (value: string) => {
  return value.replace(/[^\d]/g, '')
}

const formatCount = (count: number) => {
  if (count >= 10000) {
    return (count / 10000).toFixed(1) + 'w'
  } else if (count >= 1000) {
    return (count / 1000).toFixed(1) + 'k'
  }
  return count
}

const getSearchPlaceholder = () => {
  const type = searchType.value
  const target = activeTab.value === 'follow' ? '关注' : '粉丝'
  switch (type) {
    case 'id':
      return `输入${target}的ID搜索`
    case 'name':
      return `输入${target}的用户名搜索`
    case 'account':
      return `输入${target}的账号搜索`
    case 'profile':
      return `输入${target}的简介关键词搜索`
    default:
      return '搜索'
  }
}

watch(searchType, () => {
  if (searchText.value) {
    handleSearch()
  }
})

const loadMore = async () => {
  if (refreshing.value) {
    userList.value = []
    refreshing.value = false
  }

  if (loading.value) return
  loading.value = true

  try {
    const params: any = {
      current: currentPage.value,
      pageSize: pageSize,
      searchType: activeTab.value === 'follow' ? 0 : 1
    }

    if (activeTab.value === 'follow') {
      params.followerId = loginUserStore.loginUser.id
      if (searchText.value && searchText.value.trim()) {
        switch (searchType.value) {
          case 'id':
            params.followingId = searchText.value.trim()
            break
          case 'name':
            params.userNameKeyword = searchText.value.trim()
            break
          case 'account':
            params.userAccountKeyword = searchText.value.trim()
            break
          case 'profile':
            params.userProfileKeyword = searchText.value.trim()
            break
        }
      }
    } else {
      params.followingId = loginUserStore.loginUser.id
      if (searchText.value && searchText.value.trim()) {
        switch (searchType.value) {
          case 'id':
            params.followerId = searchText.value.trim()
            break
          case 'name':
            params.userNameKeyword = searchText.value.trim()
            break
          case 'account':
            params.userAccountKeyword = searchText.value.trim()
            break
          case 'profile':
            params.userProfileKeyword = searchText.value.trim()
            break
        }
      }
    }

    const res = await getFollowOrFanListUsingPost(params)

    if (res.data.code === 0) {
      const newUsers = res.data.data.records || []
      const total = res.data.data.total || 0

      if (currentPage.value === 1) {
        if (activeTab.value === 'follow') {
          followCount.value = total
        } else {
          fansCount.value = total
        }
      }

      userList.value.push(...newUsers)

      if (newUsers.length === 0 || userList.value.length >= total) {
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

const onRefresh = async () => {
  finished.value = false
  currentPage.value = 1
  await loadMore()
}

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
      if (graphModalVisible.value) {
        initUserGraph()
      }
    }
  } catch (error) {
    console.error('操作失败:', error)
    message.error('操作失败，请稍后重试')
  } finally {
    user.loading = false
  }
}

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

const getDefaultAvatar = (userName: string) => {
  const defaultName = userName || 'Guest'
  return `https://api.dicebear.com/7.x/adventurer/svg?seed=${encodeURIComponent(defaultName)}&backgroundColor=ebf5ff,e3f2fd,bbdefb`
}

watch(() => route.query.tab, (newTab) => {
  if (newTab) {
    activeTab.value = newTab.toString()
  }
})

watch(activeTab, () => {
  searchText.value = ''
  searchType.value = 'id'
  currentPage.value = 1
  userList.value = []
  finished.value = false
  loading.value = false
  refreshing.value = false

  nextTick(() => {
    const container = document.querySelector('.content-container')
    if (container) {
      container.scrollTop = 0
    }
  })

  loadMore()
})

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
      currentPage.value = 1
      userList.value = []
      loadMore()
      if (graphModalVisible.value) {
        initUserGraph()
      }
    }
  } catch (error) {
    console.error('操作失败:', error)
    message.error('操作失败，请稍后重试')
  }
}

const goToHome = () => {
  router.push('/')
}

const handleSearch = () => {
  if (searchDebounceTimer.value) {
    clearTimeout(searchDebounceTimer.value)
  }

  searchDebounceTimer.value = setTimeout(() => {
    if (searchType.value === 'id') {
      if (searchText.value && !/^\d+$/.test(searchText.value)) {
        message.error('请输入有效的用户ID')
        return
      }
      if (searchText.value && (searchText.value.length > 19 || searchText.value.length < 1)) {
        message.error('请输入有效的用户ID')
        return
      }
    }

    currentPage.value = 1
    userList.value = []
    finished.value = false
    loadMore()
  }, 500)
}

const handleScroll = (event: Event) => {
  const target = event.target as HTMLElement
  const { scrollTop, scrollHeight, clientHeight } = target

  if (scrollHeight - scrollTop - clientHeight < 100 && !loading.value && !finished.value) {
    loadMore()
  }
}

const handleClear = () => {
  searchText.value = ''
  handleSearch()
}

const openGraphModal = () => {
  graphModalVisible.value = true
  document.body.style.overflow = 'hidden'
  nextTick(() => {
    initUserGraph()
  })
}

const closeGraphModal = () => {
  graphModalVisible.value = false
  document.body.style.overflow = ''
}

const initUserGraph = () => {
  const container = document.getElementById('userGraph')
  if (!container) return

  if (graphChart) {
    graphChart.dispose()
  }

  graphChart = echarts.init(container)

  const currentUser = loginUserStore.loginUser
  const nodes = [
    {
      id: currentUser.id,
      name: currentUser.userName,
      label: `${currentUser.userName}\n(我)`,
      category: 0,
      symbolSize: 80,
      avatar: currentUser.userAvatar || getDefaultAvatar(currentUser.userName)
    }
  ]

  const links = []

  userList.value.forEach((user, index) => {
    nodes.push({
      id: user.id,
      name: user.userName,
      label: user.userName,
      category: activeTab.value === 'follow' ? 1 : 2,
      symbolSize: 60,
      avatar: user.userAvatar || getDefaultAvatar(user.userName)
    })

    links.push({
      source: currentUser.id,
      target: user.id,
      name: activeTab.value === 'follow' ? '关注' : '粉丝',
      desc: activeTab.value === 'follow' ? `我关注了${user.userName}` : `${user.userName}关注了我`
    })
  })

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: (params: any) => {
        if (params.dataType === 'node' && params.data) {
          return `<div style="padding: 8px; min-width: 200px;">
                    <div><img src="${params.data.avatar}" style="width:40px;height:40px;border-radius:50%;margin-bottom:8px;" /></div>
                    <div><b>用户名：</b>${params.data.name}</div>
                    <div><b>用户ID：</b>${params.data.id}</div>
                    <div><b>关系：</b>${params.data.category === 0 ? '我' : activeTab.value === 'follow' ? '我的关注' : '我的粉丝'}</div>
                  </div>`
        } else if (params.dataType === 'edge' && params.data) {
          return `<div style="padding: 8px; min-width: 150px;">
                    <div><b>关系：</b>${params.data.name}</div>
                    <div><b>说明：</b>${params.data.desc}</div>
                  </div>`
        }
        return ''
      }
    },
    legend: {
      orient: 'vertical',
      top: 'bottom',
      left: 'right',
      data: ['我', '关注', '粉丝'],
      textStyle: { fontSize: 12 }
    },
    series: [
      {
        type: 'graph',
        layout: 'force',
        data: nodes,
        links: links,
        categories: [
          { name: '我', itemStyle: { color: '#409EFF' } },
          { name: '关注', itemStyle: { color: '#67C23A' } },
          { name: '粉丝', itemStyle: { color: '#F56C6C' } }
        ],
        roam: true,
        label: {
          show: true,
          position: 'inside',
          fontSize: 12,
          color: '#fff',
          fontWeight: 500
        },
        force: {
          repulsion: 300,
          edgeLength: 150,
          gravity: 0.1,
          layoutAnimation: true
        },
        lineStyle: {
          color: 'source',
          curveness: 0.1,
          width: 2
        },
        emphasis: {
          focus: 'adjacency',
          lineStyle: { width: 4 }
        },
        itemStyle: {
          borderRadius: 50
        },
        symbol: (params: any) => {
          if (!params || !params.data) {
            return {
              type: 'circle',
              width: 60,
              height: 60
            }
          }
          return {
            type: 'image',
            image: params.data.avatar,
            width: params.data.symbolSize,
            height: params.data.symbolSize
          }
        }
      }
    ]
  }

  graphChart.setOption(option)

  const resizeGraph = () => {
    if (graphChart) {
      graphChart.resize()
    }
  }

  window.addEventListener('resize', resizeGraph)
}

const handleGraphResize = () => {
  if (graphModalVisible.value && graphChart) {
    graphChart.resize()
  }
}

onMounted(() => {
  if (!loginUserStore.loginUser?.id) {
    message.warning('请先登录')
    router.replace('/user/login')
    return
  }
  loadMore()
  window.addEventListener('resize', handleGraphResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleGraphResize)
  if (graphChart) {
    graphChart.dispose()
    graphChart = null
  }
  if (graphModalVisible.value) {
    document.body.style.overflow = ''
  }
})
</script>

<style scoped>
.page-container {
  background: var(--background);
  max-width: 800px;
  margin: 0 auto;
  color: var(--text-primary);
  width: 100%;
  overflow-x: hidden;
}

.nav-container {
  position: sticky;
  top: 0;
  padding: 8px 0;
  background: var(--background);
}

.nav-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 15px;
  width: 100%;
}

.nav-tabs {
  display: flex;
  justify-content: center;
  gap: 60px;
  max-width: 100%;
  margin: 0;
}

.nav-tab {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 0;
  cursor: pointer;
  position: relative;
  font-size: 16px;
}

.nav-tab.active .tab-text {
  color: var(--text-primary);
  font-weight: 700;
}

.nav-tab.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 24px;
  height: 2px;
  background: var(--text-primary);
  border-radius: 1px;
}

.tab-icon {
  font-size: 18px;
  color: var(--text-secondary);
}

.nav-tab.active .tab-icon {
  color: var(--text-primary);
}

.tab-text {
  font-weight: 400;
  color: var(--text-secondary);
  transition: all 0.2s ease;
}

.tab-count {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
  background: var(--hover-background);
  padding: 2px 8px;
  border-radius: 10px;
  margin-left: 4px;
}

.graph-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  border: 1px solid var(--border-color);
  background: var(--hover-background);
  color: var(--text-primary);
  transition: all 0.2s;
  white-space: nowrap;
}

.graph-btn:hover {
  background: var(--card-background);
  border-color: var(--text-primary);
}

.graph-icon {
  font-size: 16px;
}

.search-box {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--hover-background);
  border-radius: 20px;
  padding: 8px 16px;
  margin: 8px auto;
  max-width: 90%;
}

.search-icon {
  font-size: 16px;
  color: var(--text-secondary);
}

.search-input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 14px;
  color: var(--text-primary);
  background: transparent;
  padding: 4px 0;
}

.search-input::placeholder {
  color: var(--text-secondary);
}

.search-clear {
  cursor: pointer;
  font-size: 14px;
  color: var(--text-secondary);
  padding: 4px;
  transition: color 0.2s;
}

.search-clear:hover {
  color: var(--text-primary);
}

.search-type-select {
  min-width: 80px;
}

.search-type-select :deep(.ant-select-selector) {
  border: none !important;
  background: transparent !important;
  box-shadow: none !important;
}

.content-container {
  max-width: 100%;
  padding: 0 15px 20px;
  overflow-y: auto;
  margin-bottom: 48px;
}

.user-list {
  display: flex;
  flex-direction: column;
}

.user-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 10px;
  background: var(--card-background);
  border-bottom: 1px solid var(--hover-background);
  transition: background 0.2s;
}

.user-card:hover {
  background: var(--hover-background);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  flex: 1;
}

.avatar-wrapper {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  overflow: hidden;
  border: 1px solid var(--border-color);
}

.avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}

.user-details {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.username {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.user-id {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--text-secondary);
}

.id-icon {
  font-size: 10px;
}

.action-area {
  flex-shrink: 0;
}

.follow-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 18px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  border: none;
  background: var(--text-primary);
  color: var(--text-other);
  transition: all 0.2s;
}

.follow-btn.following {
  background: var(--hover-background);
  color: var(--text-secondary);
}

.follow-btn.unfollow {
  background: var(--hover-background);
  color: var(--text-secondary);
  border: 1px solid var(--border-color);
}

.follow-btn:hover {
  opacity: 0.9;
}

.btn-icon {
  font-size: 12px;
}

.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 20px 0;
  color: var(--text-secondary);
  font-size: 14px;
}

.loading-icon {
  font-size: 16px;
  animation: rotate 1s linear infinite;
}

@keyframes rotate {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 20px;
  text-align: center;
  background: var(--card-background);
  margin-top: 20px;
}

.empty-icon {
  font-size: 48px;
  color: var(--border-color);
  margin-bottom: 16px;
}

.empty-text {
  font-size: 17px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.empty-hint {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 24px;
}

.explore-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 28px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
  background: var(--text-primary);
  color: var(--text-other);
  border: none;
  cursor: pointer;
  transition: all 0.2s;
}

.explore-btn:hover {
  background: var(--hover-background);
  color: var(--text-primary);
}

.graph-modal-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  overflow: hidden;
}

.graph-modal-content {
  background: var(--card-background);
  border-radius: 12px;
  width: 90%;
  max-width: 1000px;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  overflow: hidden;
}

.graph-modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-color);
  flex-shrink: 0;
}

.graph-modal-header h3 {
  margin: 0;
  font-size: 18px;
  color: var(--text-primary);
  font-weight: 600;
}

.modal-close-btn {
  background: transparent;
  border: none;
  padding: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: background 0.2s;
}

.modal-close-btn:hover {
  background: var(--hover-background);
}

.close-icon {
  font-size: 20px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: color 0.2s;
}

.close-icon:hover {
  color: var(--text-primary);
}

.graph-modal-body {
  flex: 1;
  padding: 20px;
  overflow: hidden;
}

.graph-container {
  width: 100%;
  height: 600px;
  border-radius: 8px;
}

@media screen and (max-width: 768px) {
  .nav-header {
    flex-direction: row;
    gap: 10px;
    align-items: center;
  }

  .nav-tabs {
    gap: 20px;
    flex: 1;
    justify-content: center;
  }

  .tab-text {
    font-size: 15px;
  }

  .graph-btn {
    padding: 5px 10px;
    font-size: 13px;
  }

  .graph-text {
    display: none;
  }

  .graph-icon {
    font-size: 18px;
  }

  .search-box {
    max-width: calc(100% - 20px);
    padding: 6px 12px;
  }

  .user-card {
    padding: 12px 8px;
  }

  .avatar-wrapper {
    width: 40px;
    height: 40px;
  }

  .username {
    font-size: 14px;
  }

  .follow-btn {
    padding: 5px 14px;
    font-size: 13px;
  }

  .empty-state {
    padding: 40px 16px;
  }

  .empty-icon {
    font-size: 40px;
  }

  .empty-text {
    font-size: 16px;
  }

  .empty-hint {
    font-size: 13px;
  }

  .explore-btn {
    padding: 7px 24px;
    font-size: 13px;
  }

  .graph-modal-content {
    width: 95%;
    max-height: 80vh;
  }

  .graph-container {
    height: 400px;
  }
}

@media screen and (min-width: 769px) {
  .graph-text {
    display: inline-block;
    width: 4em;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}
</style>
