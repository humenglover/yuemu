<template>
  <!-- 外层全屏容器 -->
  <div class="box admin-layout-wrapper">
    <div class="admin-layout-container">
      <!-- 移动端顶部导航（紧凑版） -->
      <div class="mobile-header">
        <span class="logo-text">悦目管理</span>
        <a-button @click="toggleMobileMenu" type="text" size="small">
          <template #icon>
            <MenuUnfoldOutlined v-if="!isMobileMenuOpen" />
            <MenuFoldOutlined v-else />
          </template>
        </a-button>
      </div>

      <!-- 移动端遮罩层 -->
      <div
        class="mobile-mask"
        v-if="isMobileMenuOpen"
        @click="toggleMobileMenu"
      ></div>

      <!-- 左侧侧边栏 -->
      <div
        class="admin-sider"
        :class="{
          'sider-collapsed': isCollapsed && !isMobile,
          'mobile-sider-active': isMobileMenuOpen
        }"
      >
        <!-- 侧边栏头部 -->
        <div class="sider-header">
          <h2 class="sider-title" v-show="!isCollapsed || isMobileMenuOpen">悦目管理后台</h2>
          <a-button
            @click="toggleCollapsed"
            type="text"
            v-show="!isMobile"
          >
            <template #icon>
              <MenuFoldOutlined v-if="!isCollapsed" />
              <MenuUnfoldOutlined v-else />
            </template>
            <span v-show="!isCollapsed">{{ isCollapsed ? '展开' : '收起' }}</span>
          </a-button>
        </div>

        <!-- 菜单列表 -->
        <a-menu
          mode="inline"
          v-model:selectedKeys="selectedKey"
          class="admin-menu"
          :inline-collapsed="isCollapsed && !isMobileMenuOpen"
          :style="{ fontSize: isMobile ? '13px' : '14px' }"
        >
          <!-- 数据概览 -->
          <a-menu-item key="dashboard" @click="handleMenuClick('dashboard')" class="menu-item-compact">
            <template #icon><DashboardOutlined /></template>
            <span v-show="!isCollapsed || isMobileMenuOpen">数据概览</span>
          </a-menu-item>

          <!-- 知识图谱 -->
          <a-menu-item key="knowledgeGraph" @click="handleMenuClick('knowledgeGraph')" class="menu-item-compact">
            <template #icon><AppstoreOutlined /></template>
            <span v-show="!isCollapsed || isMobileMenuOpen">知识图谱</span>
          </a-menu-item>

          <!-- 用户管理 -->
          <a-sub-menu key="user" class="submenu-compact" v-if="hasSubMenu('user')">
            <template #icon><UserOutlined /></template>
            <template #title>用户管理</template>
            <a-menu-item key="userManage" @click="handleMenuClick('userManage')" class="menu-item-compact">
              <template #icon><UserOutlined /></template>
              用户列表
            </a-menu-item>
          </a-sub-menu>

          <!-- 内容管理 -->
          <a-sub-menu key="content" class="submenu-compact" v-if="hasSubMenu('content')">
            <template #icon><FileTextOutlined /></template>
            <template #title>内容管理</template>
            <a-menu-item key="pictureManage" @click="handleMenuClick('pictureManage')" class="menu-item-compact">
              <template #icon><PictureOutlined /></template>
              图片管理
            </a-menu-item>
            <a-menu-item key="postManage" @click="handleMenuClick('postManage')" class="menu-item-compact">
              <template #icon><FileTextOutlined /></template>
              帖子管理
            </a-menu-item>
            <a-menu-item key="audioManage" @click="handleMenuClick('audioManage')" class="menu-item-compact">
              <template #icon><SoundOutlined /></template>
              音频管理
            </a-menu-item>
            <a-menu-item key="commentManage" @click="handleMenuClick('commentManage')" class="menu-item-compact">
              <template #icon><CommentOutlined /></template>
              评论管理
            </a-menu-item>
            <a-menu-item key="messageManage" @click="handleMenuClick('messageManage')" class="menu-item-compact">
              <template #icon><MessageOutlined /></template>
              留言管理
            </a-menu-item>
            <a-menu-item key="knowledgeFileManage" @click="handleMenuClick('knowledgeFileManage')" class="menu-item-compact">
              <template #icon><FileTextOutlined /></template>
              知识库文件管理
            </a-menu-item>
            <a-menu-item key="reportManage" @click="handleMenuClick('reportManage')" class="menu-item-compact">
              <template #icon><ExclamationCircleOutlined /></template>
              举报管理
            </a-menu-item>
          </a-sub-menu>

          <!-- 空间管理 -->
          <a-sub-menu key="space" class="submenu-compact" v-if="hasSubMenu('space')">
            <template #icon><AppstoreOutlined /></template>
            <template #title>空间管理</template>
            <a-menu-item key="spaceManage" @click="handleMenuClick('spaceManage')" class="menu-item-compact">
              <template #icon><AppstoreOutlined /></template>
              用户空间
            </a-menu-item>
            <a-menu-item key="loveBoardManage" @click="handleMenuClick('loveBoardManage')" class="menu-item-compact">
              <template #icon><HeartOutlined /></template>
              恋爱板管理
            </a-menu-item>
          </a-sub-menu>

          <!-- 系统管理 -->
          <a-sub-menu key="system" class="submenu-compact" v-if="hasSubMenu('system')">
            <template #icon><SettingOutlined /></template>
            <template #title>系统管理</template>
            <a-menu-item key="tagManage" @click="handleMenuClick('tagManage')" class="menu-item-compact">
              <template #icon><TagsOutlined /></template>
              标签管理
            </a-menu-item>
            <a-menu-item key="categoryManage" @click="handleMenuClick('categoryManage')" class="menu-item-compact">
              <template #icon><FolderOutlined /></template>
              分类管理
            </a-menu-item>
            <a-menu-item key="appManage" @click="handleMenuClick('appManage')" class="menu-item-compact">
              <template #icon><AppstoreOutlined /></template>
              APP管理
            </a-menu-item>
            <a-menu-item key="activityManage" @click="handleMenuClick('activityManage')" class="menu-item-compact">
              <template #icon><CalendarOutlined /></template>
              活动管理
            </a-menu-item>
            <a-menu-item key="friendLinkManage" @click="handleMenuClick('friendLinkManage')" class="menu-item-compact">
              <template #icon><LinkOutlined /></template>
              友链管理
            </a-menu-item>
            <a-menu-item key="systemNotifyManage" @click="handleMenuClick('systemNotifyManage')" class="menu-item-compact">
              <template #icon><BellOutlined /></template>
              通知管理
            </a-menu-item>
            <a-menu-item key="redisMonitor" @click="handleMenuClick('redisMonitor')" class="menu-item-compact">
              <template #icon><DatabaseOutlined /></template>
              Redis监控
            </a-menu-item>
          </a-sub-menu>

          <!-- 互动管理 -->
          <a-sub-menu key="interaction" class="submenu-compact" v-if="hasSubMenu('interaction')">
            <template #icon><MessageOutlined /></template>
            <template #title>互动管理</template>
            <a-menu-item key="chatManage" @click="handleMenuClick('chatManage')" class="menu-item-compact">
              <template #icon><MessageOutlined /></template>
              聊天管理
            </a-menu-item>
            <a-menu-item key="sessionManage" @click="handleMenuClick('sessionManage')" class="menu-item-compact">
              <template #icon><MessageOutlined /></template>
              会话管理
            </a-menu-item>
            <a-menu-item key="likeManage" @click="handleMenuClick('likeManage')" class="menu-item-compact">
              <template #icon><LikeOutlined /></template>
              点赞管理
            </a-menu-item>
            <a-menu-item key="shareManage" @click="handleMenuClick('shareManage')" class="menu-item-compact">
              <template #icon><ShareAltOutlined /></template>
              分享管理
            </a-menu-item>
          </a-sub-menu>

          <!-- AI管理 -->
          <a-sub-menu key="ai" class="submenu-compact" v-if="hasSubMenu('ai')">
            <template #icon><RobotOutlined /></template>
            <template #title>AI管理</template>
            <a-menu-item key="aiChatManage" @click="handleMenuClick('aiChatManage')" class="menu-item-compact">
              <template #icon><RobotOutlined /></template>
              AI对话
            </a-menu-item>
            <a-menu-item key="deepseek" @click="handleAIManageClick" class="menu-item-compact">
              <template #icon><RobotOutlined /></template>
              DeepSeek
            </a-menu-item>
          </a-sub-menu>

          <!-- 博客管理 -->
          <a-menu-item key="blog" @click="handleBlogManageClick" class="menu-item-compact">
            <template #icon><BookOutlined /></template>
            <span v-show="!isCollapsed || isMobileMenuOpen">博客管理</span>
          </a-menu-item>
        </a-menu>
      </div>

      <!-- 右侧内容区域 -->
      <div class="admin-content">
        <!-- 内容头部（单行布局：标题+按钮） -->
        <div class="content-header content-header-single-row">
          <h1 class="content-title">{{ pageTitle }}</h1>
          <a-button type="primary" @click="refreshData" size="small" class="refresh-btn">
            <template #icon><ReloadOutlined /></template>
            <span v-show="!isMobile">刷新数据</span>
            <span v-show="isMobile">刷新</span>
          </a-button>
        </div>

        <!-- 数据统计卡片（仅数据概览页显示） -->
        <div class="stats-container" v-if="selectedKey[0] === 'dashboard'">
          <div class="stats-grid" :class="{ 'stats-grid-mobile': isMobile }">
            <!-- 为卡片添加点击事件和路由跳转 -->
            <a-card
              class="stats-card"
              v-for="(item, index) in statsList"
              :key="index"
              :class="{
              'stats-card-mobile': isMobile,
              'clickable-card': item.route
            }"
              @click="handleStatsCardClick(item.route)"
              hoverable
            >
              <template #title>
                <component :is="item.icon" class="card-icon" /> {{ item.title }}
              </template>
              <div class="stats-content">
                <span class="stats-number">{{ dashboardStats[item.key] || '0' }}</span>
                <!-- 显示跳转提示 -->
                <span v-if="item.route" class="card-link-icon">
                  <ArrowRightOutlined />
                </span>
              </div>
            </a-card>
          </div>

          <!-- 图表展示区域 - 优化为紧凑布局 -->
          <div class="charts-container">
            <div class="chart-row compact-chart-row">
              <div class="chart-item">
                <h3>用户活跃度雷达图</h3>
                <div id="radarChart" class="chart compact-chart"></div>
              </div>
              <div class="chart-item">
                <h3>本月图片分类饼图</h3>
                <div id="pieChart" class="chart compact-chart"></div>
              </div>
              <div class="chart-item">
                <h3>帖子发布堆叠柱状图</h3>
                <div id="stackedBarChart" class="chart compact-chart"></div>
              </div>
            </div>
            <div class="chart-row compact-chart-row">
              <div class="chart-item full-width">
                <h3>新增空间趋势面积图</h3>
                <div id="areaChart" class="chart compact-chart"></div>
              </div>
            </div>
          </div>
        </div>

        <!-- 知识图谱组件（直接嵌入显示，不跳转页面） -->
        <div class="knowledge-graph-container" v-else-if="selectedKey[0] === 'knowledgeGraph'">
          <div class="knowledge-graph-content">
            <KnowledgeGraph />
          </div>
        </div>

        <!-- 其他页面占位 -->
        <div class="page-placeholder" v-else>
          <div class="placeholder-content">
            <component :is="menuComponentMap[selectedKey[0]] || 'div'">
              <h2>【{{ pageTitle }}】页面开发中...</h2>
              <p>点击左侧菜单切换不同管理模块</p>
            </component>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import {
  TeamOutlined,
  PictureOutlined,
  EyeOutlined,
  AppstoreOutlined,
  FileTextOutlined,
  LinkOutlined,
  UserOutlined,
  TagsOutlined,
  FolderOutlined,
  CalendarOutlined,
  CommentOutlined,
  SoundOutlined,
  RobotOutlined,
  MessageOutlined,
  HeartOutlined,
  LikeOutlined,
  ShareAltOutlined,
  DatabaseOutlined,
  BellOutlined,
  DashboardOutlined,
  SettingOutlined,
  BookOutlined,
  ReloadOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  ExclamationCircleOutlined,
  ArrowRightOutlined // 新增箭头图标
} from '@ant-design/icons-vue'
import { getDashboardStatsUsingGet, getChartStatsUsingGet } from '@/api/dashboardController'
import * as echarts from 'echarts'
import KnowledgeGraph from '@/components/KnowledgeGraph.vue'

const router = useRouter()

// 响应式数据
const isCollapsed = ref(false)
const isMobileMenuOpen = ref(false)
const isMobile = ref(false)
const selectedKey = ref(['dashboard'])
const dashboardStats = ref<API.DashboardVO>({
  newUsers: 0,
  newPictures: 0,
  newPosts: 0,
  newSpaces: 0,
  newActivities: 0,
  newLoveBoards: 0,
  newFriendLinks: 0,
  newMessages: 0,
  totalViews: 0,
  newAudioFiles: 0,
  newReports: 0,
  newChatMessages: 0
})

// 图表数据
const chartData = ref<API.ChartVO>({
  radarChartData: null,
  pieChartData: null,
  stackedBarChartData: null
} as API.ChartVO)

// 统计卡片配置（新增路由配置）
const statsList = ref([
  {
    title: isMobile.value ? '用户' : '今日新增用户',
    key: 'newUsers',
    icon: TeamOutlined,
    route: '/admin/userManage' // 配置对应的路由
  },
  {
    title: isMobile.value ? '图片' : '今日新增图片',
    key: 'newPictures',
    icon: PictureOutlined,
    route: '/admin/pictureManage'
  },
  {
    title: isMobile.value ? '访问量' : '今日访问量',
    key: 'totalViews',
    icon: EyeOutlined,
    route: '' // 无对应路由，不跳转
  },
  {
    title: isMobile.value ? '空间' : '今日新增空间',
    key: 'newSpaces',
    icon: AppstoreOutlined,
    route: '/admin/spaceManage'
  },
  {
    title: isMobile.value ? '帖子' : '今日新增帖子',
    key: 'newPosts',
    icon: FileTextOutlined,
    route: '/admin/postManage'
  },
  {
    title: isMobile.value ? '友链' : '今日新增友链',
    key: 'newFriendLinks',
    icon: LinkOutlined,
    route: '/admin/friendLinkManage'
  },
  {
    title: isMobile.value ? '空间' : '今日新增恋爱空间',
    key: 'newLoveBoards',
    icon: HeartOutlined,
    route: '/admin/loveBoardManage'
  },
  {
    title: isMobile.value ? '留言' : '今日新增留言',
    key: 'newMessages',
    icon: MessageOutlined,
    route: '/admin/messageManage'
  },
  {
    title: isMobile.value ? '活动' : '今日新增活动',
    key: 'newActivities',
    icon: CalendarOutlined,
    route: '/admin/activityManage'
  },
  {
    title: isMobile.value ? '音频' : '今日新增音频',
    key: 'newAudioFiles',
    icon: SoundOutlined,
    route: '/admin/audioManage'
  },
  {
    title: isMobile.value ? '举报' : '今日新增举报',
    key: 'newReports',
    icon: ExclamationCircleOutlined,
    route: '/admin/reportManage'
  },
  {
    title: isMobile.value ? '会话' : '今日新增会话',
    key: 'newChatMessages',
    icon: MessageOutlined,
    route: '/admin/sessionManage'
  }
])

// 菜单标题映射
const menuTitleMap = ref({
  dashboard: '数据概览',
  userManage: '用户列表',
  pictureManage: '图片管理',
  postManage: '帖子管理',
  audioManage: '音频管理',
  commentManage: '评论管理',
  messageManage: '留言管理',
  spaceManage: '用户空间',
  loveBoardManage: '恋爱板管理',
  tagManage: '标签管理',
  categoryManage: '分类管理',
  appManage: 'APP管理',
  activityManage: '活动管理',
  friendLinkManage: '友链管理',
  systemNotifyManage: '通知管理',
  redisMonitor: 'Redis监控',
  chatManage: '聊天管理',
  sessionManage: '会话管理',
  likeManage: '点赞管理',
  shareManage: '分享管理',
  aiChatManage: 'AI对话',
  deepseek: 'DeepSeek',
  blog: '博客管理',
  reportManage: '举报管理'
})

// 菜单组件映射
const menuComponentMap = ref({
  userManage: 'UserManage',
  pictureManage: 'PictureManage',
  postManage: 'PostManage',
  sessionManage: 'SessionManage',
  reportManage: 'ReportManage'
})

// 计算属性：当前页面标题
const pageTitle = computed(() => {
  return menuTitleMap.value[selectedKey.value[0]] || '数据概览'
})

// 方法：判断是否有子菜单（修复空白弹框）
const hasSubMenu = (key: string) => {
  const subMenuMap = {
    user: true,
    content: true,
    space: true,
    system: true,
    interaction: true,
    ai: true
  }
  return subMenuMap[key as keyof typeof subMenuMap]
}

// 方法：切换侧边栏折叠状态
const toggleCollapsed = () => {
  isCollapsed.value = !isCollapsed.value
}

// 方法：切换移动端菜单
const toggleMobileMenu = () => {
  isMobileMenuOpen.value = !isMobileMenuOpen.value
  document.body.style.overflow = isMobileMenuOpen.value ? 'hidden' : 'auto'
}

// 方法：菜单点击事件
const handleMenuClick = (route: string) => {
  selectedKey.value = [route]
  if (isMobile.value) {
    isMobileMenuOpen.value = false
    document.body.style.overflow = 'auto'
  }
  if (route !== 'dashboard' && route !== 'deepseek' && route !== 'blog' && route !== 'knowledgeGraph') {
    router.push(`/admin/${route}`)
  }
}

// 新增：统计卡片点击事件处理
const handleStatsCardClick = (route: string) => {
  if (!route) return // 如果没有配置路由，不执行跳转

  // 移动端关闭菜单
  if (isMobile.value) {
    isMobileMenuOpen.value = false
    document.body.style.overflow = 'auto'
  }

  // 跳转到对应路由
  router.push(route)

  // 更新选中的菜单项
  const routeKey = route.replace('/admin/', '')
  if (menuTitleMap.value[routeKey]) {
    selectedKey.value = [routeKey]
  }
}

// 方法：AI管理跳转
const handleAIManageClick = () => {
  selectedKey.value = ['deepseek']
  window?.open('https://platform.deepseek.com/top_up', '_blank')
}

// 方法：博客管理跳转
const handleBlogManageClick = () => {
  selectedKey.value = ['blog']
  window?.open('http://yuemustory.fun/wp-admin', '_blank')
}

// 方法：刷新数据
const refreshData = () => {
  fetchDashboardStats()
  fetchChartStats() // 刷新时同时更新图表数据
}

// 方法：获取数据看板统计
const fetchDashboardStats = async () => {
  try {
    const response = await getDashboardStatsUsingGet()
    if (response.data) {
      dashboardStats.value = response.data.data
    }
  } catch (error) {
    console.error('获取数据看板统计失败:', error)
  }
}

// 方法：获取图表统计
const fetchChartStats = async () => {
  try {
    const response = await getChartStatsUsingGet()
    if (response.data) {
      chartData.value = response.data.data
      // 获取数据后渲染图表
      await nextTick()
      renderCharts()
    }
  } catch (error) {
    console.error('获取图表统计失败:', error)
  }
}

// 监听窗口大小变化
const handleResize = () => {
  const mobileBreakpoint = 768
  isMobile.value = window.innerWidth < mobileBreakpoint
  if (isMobile.value) {
    isCollapsed.value = true
    isMobileMenuOpen.value = false
  } else {
    isMobileMenuOpen.value = false
    document.body.style.overflow = 'auto'
  }
  // 同步统计卡片标题
  statsList.value = [
    {
      title: isMobile.value ? '用户' : '今日新增用户',
      key: 'newUsers',
      icon: TeamOutlined,
      route: '/admin/userManage'
    },
    {
      title: isMobile.value ? '图片' : '今日新增图片',
      key: 'newPictures',
      icon: PictureOutlined,
      route: '/admin/pictureManage'
    },
    {
      title: isMobile.value ? '访问量' : '今日访问量',
      key: 'totalViews',
      icon: EyeOutlined,
      route: ''
    },
    {
      title: isMobile.value ? '空间' : '今日新增空间',
      key: 'newSpaces',
      icon: AppstoreOutlined,
      route: '/admin/spaceManage'
    },
    {
      title: isMobile.value ? '帖子' : '今日新增帖子',
      key: 'newPosts',
      icon: FileTextOutlined,
      route: '/admin/postManage'
    },
    {
      title: isMobile.value ? '友链' : '今日新增友链',
      key: 'newFriendLinks',
      icon: LinkOutlined,
      route: '/admin/friendLinkManage'
    },
    {
      title: isMobile.value ? '恋爱空间' : '今日新增恋爱空间',
      key: 'newLoveBoards',
      icon: HeartOutlined,
      route: '/admin/loveBoardManage'
    },
    {
      title: isMobile.value ? '留言' : '今日新增留言',
      key: 'newMessages',
      icon: MessageOutlined,
      route: '/admin/messageManage'
    },
    {
      title: isMobile.value ? '活动' : '今日新增活动',
      key: 'newActivities',
      icon: CalendarOutlined,
      route: '/admin/activityManage'
    },
    {
      title: isMobile.value ? '音频' : '今日新增音频',
      key: 'newAudioFiles',
      icon: SoundOutlined,
      route: '/admin/audioManage'
    },
    {
      title: isMobile.value ? '举报' : '今日新增举报',
      key: 'newReports',
      icon: ExclamationCircleOutlined,
      route: '/admin/reportManage'
    },
    {
      title: isMobile.value ? '会话' : '今日新增会话',
      key: 'newChatMessages',
      icon: MessageOutlined,
      route: '/admin/sessionManage'
    }
  ]

  // 窗口变化时重新渲染图表
  renderCharts()
}

// 图表渲染函数（统一管理）
const renderCharts = () => {
  renderRadarChart()
  renderPieChart()
  renderStackedBarChart()
  renderAreaChart()
}

// 雷达图渲染
const renderRadarChart = () => {
  if (!chartData.value.radarChartData) return;

  const chartDom = document.getElementById('radarChart');
  if (!chartDom) return;

  // 先销毁旧实例
  const oldChart = echarts.getInstanceByDom(chartDom)
  if (oldChart) oldChart.dispose()

  const myChart = echarts.init(chartDom);
  const option = {
    backgroundColor: 'transparent',
    title: {
      text: '用户活跃度雷达图',
      show: false
    },
    tooltip: {},
    radar: {
      indicator: chartData.value.radarChartData.indicator?.map(item => ({ name: item })) || [],
      axisName: {
        color: 'var(--text-primary)',
        fontSize: 10 // 缩小轴名称字体
      },
      splitLine: {
        lineStyle: {
          color: 'var(--border-color)'
        }
      },
      splitArea: {
        show: false
      },
      axisLine: {
        lineStyle: {
          color: 'var(--border-color)'
        }
      }
    },
    series: [{
      name: '用户统计',
      type: 'radar',
      data: chartData.value.radarChartData.data || [],
      itemStyle: {
        color: '#176bf2',
        borderColor: '#176bf2'
      },
      areaStyle: {
        opacity: 0.2
      }
    }]
  };
  myChart.setOption(option);
  // 监听窗口变化自动调整
  window.addEventListener('resize', () => myChart.resize())
};

// 饼图渲染
const renderPieChart = () => {
  if (!chartData.value.pieChartData) return;

  const chartDom = document.getElementById('pieChart');
  if (!chartDom) return;

  // 先销毁旧实例
  const oldChart = echarts.getInstanceByDom(chartDom)
  if (oldChart) oldChart.dispose()

  const myChart = echarts.init(chartDom);
  const option = {
    backgroundColor: 'transparent',
    title: {
      text: '本月图片分类饼图',
      show: false
    },
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)',
      textStyle: {
        fontSize: 10 // 缩小提示框字体
      }
    },
    legend: {
      orient: 'vertical',
      left: 'left',
      textStyle: {
        color: 'var(--text-primary)',
        fontSize: 10 // 缩小图例字体
      }
    },
    series: [{
      name: '图片分类',
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['50%', '50%'],
      data: chartData.value.pieChartData.labels?.map((label, index) => ({
        name: label,
        value: chartData.value.pieChartData.values?.[index] || 0
      })) || [],
      emphasis: {
        itemStyle: {
          shadowBlur: 5, // 减小阴影
          shadowOffsetX: 0,
          shadowColor: 'rgba(0, 0, 0, 0.3)'
        }
      },
      itemStyle: {
        color: function(params) {
          const colors = ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452', '#9a60b4', '#ea7ccc'];
          return colors[params.dataIndex % colors.length];
        }
      },
      label: {
        fontSize: 9 // 缩小标签字体
      }
    }]
  };
  myChart.setOption(option);
  // 监听窗口变化自动调整
  window.addEventListener('resize', () => myChart.resize())
};

// 堆叠柱状图渲染（添加日期标注）
const renderStackedBarChart = () => {
  if (!chartData.value.stackedBarChartData) return;

  const chartDom = document.getElementById('stackedBarChart');
  if (!chartDom) return;

  // 先销毁旧实例
  const oldChart = echarts.getInstanceByDom(chartDom)
  if (oldChart) oldChart.dispose()

  const myChart = echarts.init(chartDom);
  const option = {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      },
      textStyle: {
        fontSize: 10 // 缩小提示框字体
      }
    },
    legend: {
      textStyle: {
        color: 'var(--text-primary)',
        fontSize: 10 // 缩小图例字体
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%', // 增加底部间距以显示日期标注
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: chartData.value.stackedBarChartData.xaxisData || [],
      axisLabel: {
        color: 'var(--text-primary)',
        fontSize: 10,
        interval: 0, // 强制显示所有标签
        rotate: 45, // 旋转标签防止重叠
        formatter: function(value) {
          // 添加日期标注说明
          return `${value}\n`;
        }
      },
      axisLine: {
        lineStyle: {
          color: 'var(--border-color)'
        }
      },
      // 添加额外的日期标注
      axisPointer: {
        label: {
          formatter: function(params) {
            return `日期: ${params.value}`;
          }
        }
      }
    },
    yAxis: {
      type: 'value',
      nameTextStyle: {
        fontSize: 10,
        color: 'var(--text-primary)'
      },
      axisLabel: {
        color: 'var(--text-primary)',
        fontSize: 10
      },
      axisLine: {
        lineStyle: {
          color: 'var(--border-color)'
        }
      }
    },
    series: chartData.value.stackedBarChartData.series?.map((item, index) => ({
      name: item.name || '',
      type: 'bar',
      stack: 'total',
      label: {
        show: true,
        color: 'var(--text-primary)',
        fontSize: 9
      },
      emphasis: {
        focus: 'series'
      },
      data: item.data || [],
      itemStyle: {
        color: function(params) {
          const colors = ['#5470c6', '#91cc75', '#fac858'];
          return colors[params.seriesIndex % colors.length];
        }
      }
    })) || []
  };
  myChart.setOption(option);
  // 监听窗口变化自动调整
  window.addEventListener('resize', () => myChart.resize())
};

// 面积图渲染
const renderAreaChart = () => {
  if (!chartData.value.areaChartData) return;

  const chartDom = document.getElementById('areaChart');
  if (!chartDom) return;

  // 先销毁旧实例
  const oldChart = echarts.getInstanceByDom(chartDom)
  if (oldChart) oldChart.dispose()

  const myChart = echarts.init(chartDom);
  const option = {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross'
      },
      textStyle: {
        fontSize: 10
      }
    },
    legend: {
      textStyle: {
        color: 'var(--text-primary)',
        fontSize: 10
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: chartData.value.areaChartData.xaxisData || [],
      axisLabel: {
        color: 'var(--text-primary)',
        fontSize: 10,
        interval: 0,
        rotate: 45
      },
      axisLine: {
        lineStyle: {
          color: 'var(--border-color)'
        }
      }
    },
    yAxis: {
      type: 'value',
      nameTextStyle: {
        fontSize: 10,
        color: 'var(--text-primary)'
      },
      axisLabel: {
        color: 'var(--text-primary)',
        fontSize: 10
      },
      axisLine: {
        lineStyle: {
          color: 'var(--border-color)'
        }
      }
    },
    series: chartData.value.areaChartData.series?.map((item, index) => ({
      name: item.name || '',
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      areaStyle: {
        opacity: 0.4
      },
      emphasis: {
        focus: 'series'
      },
      data: item.data || [],
      itemStyle: {
        color: function(params) {
          const colors = ['#5470c6'];
          return colors[params.seriesIndex % colors.length];
        }
      }
    })) || []
  };
  myChart.setOption(option);
  // 监听窗口变化自动调整
  window.addEventListener('resize', () => myChart.resize())
};

// 监听chartData变化并重新渲染图表
watch(chartData, () => {
  nextTick(() => {
    renderCharts();
  });
}, { deep: true });

// 定时刷新数据
let refreshInterval: number | undefined

// 组件挂载
onMounted(() => {
  fetchDashboardStats()
  fetchChartStats()
  refreshInterval = setInterval(() => {
    fetchDashboardStats()
    fetchChartStats()
  }, 60000)
  window.addEventListener('resize', handleResize)
  handleResize()

  // 监听路由变化
  watch(
    () => router.currentRoute.value.path,
    (path) => {
      const route = path.replace('/admin/', '') || 'dashboard'
      if (menuTitleMap.value[route]) {
        selectedKey.value = [route]
        // 切换到数据概览时重新渲染图表
        if (route === 'dashboard') {
          nextTick(() => renderCharts())
        }
      }
    },
    { immediate: true }
  )
})

// 组件卸载
onUnmounted(() => {
  // 清除定时器
  if (refreshInterval) clearInterval(refreshInterval)
  window.removeEventListener('resize', handleResize)
  document.body.style.overflow = 'auto'

  // 销毁所有图表实例
  const chartIds = ['radarChart', 'pieChart', 'stackedBarChart', 'areaChart']
  chartIds.forEach(id => {
    const dom = document.getElementById(id)
    if (dom) {
      const chart = echarts.getInstanceByDom(dom)
      if (chart) chart.dispose()
    }
  })
})
</script>

<style scoped>
/* 全屏样式 */
.box {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: var(--background);
  z-index: 100;
  overflow: auto;
}

.admin-layout-wrapper {
  width: 100%;
  height: 100%;
}

/* 全局布局 */
.admin-layout-container {
  display: flex;
  width: 100%;
  height: 100%;
  background: var(--hover-background);
  color: var(--text-primary);
  position: relative;
  overflow: hidden;
}

/* 移动端头部（紧凑版） */
.mobile-header {
  display: none;
  align-items: center;
  justify-content: space-between;
  padding: 0 12px;
  height: 48px;
  background: var(--card-background);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1010;
}
.mobile-header .logo-text {
  font-size: 16px;
  font-weight: 600;
  color: #176bf2;
}

/* 移动端遮罩层 */
.mobile-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 1000;
  backdrop-filter: blur(2px);
}

/* 左侧侧边栏 */
.admin-sider {
  width: 220px;
  background: var(--card-background);
  box-shadow: 1px 0 2px rgba(0, 0, 0, 0.05);
  transition: all 0.3s cubic-bezier(0.645, 0.045, 0.355, 1);
  z-index: 1005;
  height: 100%;
  overflow-y: auto;
  overflow-x: hidden;
  position: fixed;
  left: 0;
  top: 0;
}
.sider-collapsed {
  width: 64px;
}
.mobile-sider-active {
  transform: translateX(0) !important;
  width: 240px;
}

/* 侧边栏头部 */
.sider-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-bottom: 1px solid var(--border-color);
  height: 56px;
}
.sider-title {
  margin: 0;
  font-size: 16px;
  color: #176bf2;
  font-weight: 600;
  white-space: nowrap;
}

/* 菜单列表（紧凑样式） */
.admin-menu {
  border-right: none;
  padding-top: 4px;
  height: calc(100% - 56px);
  overflow-y: auto;
}
.menu-item-compact {
  height: 40px !important;
  line-height: 40px !important;
  margin: 0 6px 2px !important;
  border-radius: 6px !important;
}
.submenu-compact :deep(.ant-menu-submenu-title) {
  height: 40px !important;
  line-height: 40px !important;
  margin: 0 6px 2px !important;
  border-radius: 6px !important;
}
.admin-menu :deep(.ant-menu-item-selected) {
  background: var(--hover-background) !important;
  color: #176bf2 !important;
}
.admin-menu :deep(.ant-menu-submenu-arrow) {
  color: var(--text-secondary);
  transform: scale(0.8);
}
/* 修复空白弹框：移除无内容子菜单 */
.admin-menu :deep(.ant-menu-submenu-empty) {
  display: none;
}

/* 右侧内容区域 */
.admin-content {
  flex: 1;
  margin-left: 220px;
  padding: 16px !important; /* 减小内边距，更紧凑 */
  transition: all 0.3s cubic-bezier(0.645, 0.045, 0.355, 1);
  height: 100%;
  overflow-y: auto;
  padding-top: 16px !important;
}
.sider-collapsed + .admin-content {
  margin-left: 64px;
}

/* 内容头部（单行布局） */
.content-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px !important; /* 减小底部间距 */
  padding-bottom: 8px !important; /* 减小底部内边距 */
  border-bottom: 1px solid var(--border-color);
}
.content-header-single-row {
  flex-direction: row !important;
  gap: 12px;
}
.content-title {
  margin: 0;
  font-size: 18px !important; /* 减小标题字体 */
  font-weight: 600;
  color: var(--text-primary);
}
.refresh-btn {
  white-space: nowrap;
  background-color: #176bf2 !important;
  border-color: #176bf2 !important;
}
.refresh-btn:hover {
  background-color: #0f57d1 !important;
  border-color: #0f57d1 !important;
}

/* 数据统计卡片 - 紧凑版 */
.stats-container {
  margin-top: 4px;
}
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)) !important; /* 减小卡片最小宽度 */
  gap: 8px !important; /* 减小卡片间距 */
  margin-bottom: 12px !important; /* 减小底部间距 */
}
.stats-grid-mobile {
  grid-template-columns: 1fr;
  gap: 8px;
}
.stats-card {
  border-radius: 6px !important; /* 减小圆角 */
  transition: all 0.3s ease;
  overflow: hidden;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03) !important; /* 减小阴影 */
  border: 1px solid var(--border-color);
  height: 90px !important; /* 固定卡片高度 */
  cursor: default; /* 默认鼠标样式 */
}
/* 可点击卡片样式 */
.clickable-card {
  cursor: pointer;
}
.clickable-card:hover {
  box-shadow: 0 2px 8px rgba(23, 107, 242, 0.15) !important;
  border-color: #176bf2 !important;
  transform: translateY(-1px);
}

.stats-card :deep(.ant-card-head) {
  background: transparent;
  border-bottom: 1px solid var(--border-color);
  padding: 6px 12px !important; /* 减小头部内边距 */
  min-height: 30px !important; /* 减小头部最小高度 */
}
.stats-card :deep(.ant-card-body) {
  padding: 6px 12px !important; /* 减小内容区内边距 */
}
.stats-card :deep(.ant-card-head-title) {
  color: var(--text-primary);
  font-size: 12px !important; /* 减小标题字体 */
  font-weight: 500;
  display: flex;
  align-items: center;
  padding: 0 !important;
}
.card-icon {
  margin-right: 6px !important; /* 减小图标间距 */
  font-size: 14px !important; /* 减小图标大小 */
  color: #176bf2;
}
.stats-content {
  padding: 6px 0 !important; /* 减小内容区内边距 */
  text-align: center;
  position: relative;
}
.stats-number {
  font-size: 20px !important; /* 减小数字字体 */
  font-weight: 600;
  color: #176bf2;
}
/* 跳转箭头图标样式 */
.card-link-icon {
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  color: #176bf2;
  font-size: 12px;
  opacity: 0.7;
  transition: all 0.2s ease;
}
.clickable-card:hover .card-link-icon {
  opacity: 1;
  transform: translateY(-50%) translateX(2px);
}

/* 页面占位 */
.page-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  height: calc(100% - 100px);
  background: var(--card-background);
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  border: 1px solid var(--border-color);
}
.placeholder-content {
  text-align: center;
  color: var(--text-secondary);
  padding: 16px;
}
.placeholder-content h2 {
  font-size: 16px;
  margin-bottom: 8px;
  color: var(--text-primary);
}

/* 响应式适配 - 平板 */
@media screen and (max-width: 992px) {
  .stats-grid {
    grid-template-columns: repeat(3, 1fr) !important; /* 平板显示3列 */
  }
  .content-title {
    font-size: 16px !important;
  }
  /* 平板端图表改为两行 */
  .compact-chart-row {
    flex-wrap: wrap;
  }
  .compact-chart-row .chart-item {
    flex: 0 0 calc(50% - 10px) !important;
  }
}

/* 响应式适配 - 移动端 */
@media screen and (max-width: 768px) {
  .mobile-header {
    display: flex;
  }
  .admin-sider {
    transform: translateX(-100%);
    top: 48px;
    height: calc(100% - 48px);
    z-index: 1015;
    box-shadow: 2px 0 8px rgba(0, 0, 0, 0.1);
  }
  .admin-content {
    margin-left: 0 !important;
    padding: 12px !important;
    padding-top: 60px !important;
    width: 100%;
  }
  .stats-number {
    font-size: 16px !important;
  }
  .content-title {
    font-size: 16px !important;
  }
  .stats-card :deep(.ant-card-head-title) {
    font-size: 13px !important;
  }
  /* 移动端图表布局调整 */
  .compact-chart-row {
    flex-direction: column !important;
    gap: 10px !important;
  }
  .compact-chart-row .chart-item {
    flex: 100% !important;
    margin-bottom: 10px !important;
  }
  .compact-chart {
    height: 200px !important;
  }
}

/* 小屏移动端适配 */
@media screen and (max-width: 360px) {
  .sider-title {
    font-size: 14px;
  }
  .stats-card :deep(.ant-card-head-title) {
    font-size: 12px !important;
  }
  .compact-chart {
    height: 180px !important;
  }
}

/* 滚动条优化 */
.admin-sider::-webkit-scrollbar,
.admin-menu::-webkit-scrollbar,
.admin-content::-webkit-scrollbar {
  width: 3px;
}
.admin-sider::-webkit-scrollbar-track,
.admin-menu::-webkit-scrollbar-track,
.admin-content::-webkit-scrollbar-track {
  background: var(--border-color);
  border-radius: 2px;
}
.admin-sider::-webkit-scrollbar-thumb,
.admin-menu::-webkit-scrollbar-thumb,
.admin-content::-webkit-scrollbar-thumb {
  background: var(--text-secondary);
  border-radius: 2px;
}

/* 图表容器 - 紧凑版 */
.charts-container {
  margin-top: 12px !important; /* 减小顶部间距 */
  padding: 12px !important; /* 减小内边距 */
  background: var(--card-background);
  border-radius: 6px !important; /* 减小圆角 */
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03) !important; /* 减小阴影 */
  border: 1px solid var(--border-color);
}

/* 紧凑图表行 - PC端三列展示 */
.compact-chart-row {
  display: flex;
  gap: 10px !important; /* 减小图表间距 */
  margin-bottom: 0 !important; /* 移除底部间距 */
  flex-wrap: nowrap; /* PC端强制不换行 */
}

.chart-item {
  flex: 1;
  min-width: 0;
}

.chart-item h3 {
  margin: 0 0 8px 0 !important; /* 减小标题底部间距 */
  font-size: 14px !important; /* 减小标题字体 */
  font-weight: 600;
  color: var(--text-primary);
}

/* 紧凑图表样式 */
.compact-chart {
  width: 100%;
  height: 220px !important; /* 减小图表高度 */
  background: var(--background);
  border-radius: 4px;
}

/* 移除full-width类的特殊样式 */
.chart-item.full-width {
  flex: 100%;
}

/* ========== 管理界面暗色主题适配 ========== */
.dark-theme {
  /* 基础布局适配 */
  .admin-layout-container {
    background: var(--card-background);
  }

  /* 移动端头部适配 */
  .mobile-header {
    background: var(--card-background);
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
  }

  .mobile-header .logo-text {
    color: #40a9ff;
  }

  /* 侧边栏适配 */
  .admin-sider {
    background: var(--card-background);
    box-shadow: 1px 0 2px rgba(0, 0, 0, 0.1);
  }

  .sider-header {
    border-bottom-color: var(--border-color);
  }

  .sider-title {
    color: #40a9ff;
  }

  /* 菜单样式适配 */
  .admin-menu {
    background: transparent;
  }

  .admin-menu :deep(.ant-menu-item),
  .admin-menu :deep(.ant-menu-submenu-title) {
    color: var(--text-primary) !important;
  }

  .admin-menu :deep(.ant-menu-item:hover),
  .admin-menu :deep(.ant-menu-submenu-title:hover) {
    background: var(--hover-background) !important;
  }

  .admin-menu :deep(.ant-menu-item-selected) {
    background: var(--hover-background) !important;
    color: #40a9ff !important;
  }

  .admin-menu :deep(.ant-menu-submenu-arrow) {
    color: var(--text-secondary);
  }

  /* 内容区域适配 */
  .admin-content {
    background: var(--background);
  }

  .content-header {
    border-bottom-color: var(--border-color);
  }

  .content-title {
    color: var(--text-primary);
  }

  /* 统计卡片适配 */
  .stats-card {
    background: var(--card-background);
    border-color: var(--border-color);
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1) !important;
  }

  .stats-card :deep(.ant-card-head) {
    border-bottom-color: var(--border-color);
  }

  .stats-card :deep(.ant-card-head-title) {
    color: var(--text-primary);
  }

  .card-icon {
    color: #40a9ff;
  }

  .stats-number {
    color: #40a9ff;
  }

  .card-link-icon {
    color: #40a9ff;
  }

  .clickable-card:hover {
    box-shadow: 0 2px 8px rgba(64, 169, 255, 0.2) !important;
    border-color: #40a9ff !important;
  }

  /* 页面占位适配 */
  .page-placeholder {
    background: var(--card-background);
    border-color: var(--border-color);
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  }

  .placeholder-content h2 {
    color: var(--text-primary);
  }

  .placeholder-content {
    color: var(--text-secondary);
  }

  /* 图表容器适配 */
  .charts-container {
    background: var(--card-background);
    border-color: var(--border-color);
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1) !important;
  }

  .chart-item h3 {
    color: var(--text-primary);
  }

  .compact-chart {
    background: var(--background);
  }

  /* 按钮样式适配 */
  .refresh-btn {
    background-color: #40a9ff !important;
    border-color: #40a9ff !important;
  }

  .refresh-btn:hover {
    background-color: #1890ff !important;
    border-color: #1890ff !important;
  }

  /* 滚动条适配 */
  .admin-sider::-webkit-scrollbar-track,
  .admin-menu::-webkit-scrollbar-track,
  .admin-content::-webkit-scrollbar-track {
    background: var(--border-color);
  }

  .admin-sider::-webkit-scrollbar-thumb,
  .admin-menu::-webkit-scrollbar-thumb,
  .admin-content::-webkit-scrollbar-thumb {
    background: var(--text-secondary);
  }
}

/* ========== Ant Design 组件全局暗色适配 ========== */
.dark-theme {
  /* 按钮组件 */
  :deep(.ant-btn) {
    color: var(--text-primary);
    background: var(--card-background);
    border-color: var(--border-color);
  }

  :deep(.ant-btn:hover) {
    background: var(--hover-background);
    border-color: #40a9ff;
    color: #40a9ff;
  }

  :deep(.ant-btn-primary) {
    background: #40a9ff !important;
    border-color: #40a9ff !important;
    color: #fff !important;
  }

  :deep(.ant-btn-primary:hover) {
    background: #1890ff !important;
    border-color: #1890ff !important;
  }

  /* 卡片组件 */
  :deep(.ant-card) {
    background: var(--card-background);
    border-color: var(--border-color);
  }

  :deep(.ant-card-head) {
    border-bottom-color: var(--border-color);
  }

  /* 菜单组件 */
  :deep(.ant-menu) {
    background: transparent;
    color: var(--text-primary);
  }

  :deep(.ant-menu-inline) {
    border-right-color: var(--border-color);
  }

  :deep(.ant-menu-submenu-popup) {
    background: var(--card-background) !important;
    border-color: var(--border-color) !important;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2) !important;
  }

  /* 图标颜色适配 */
  :deep(.anticon) {
    color: var(--text-primary);
  }
}

.knowledge-graph-container {
  background: var(--card-background);
  border-radius: 8px;
  margin-top: 16px;
}

.knowledge-graph-content {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
}


/* ========== 响应式适配补充 ========== */
@media screen and (max-width: 768px) {
  .dark-theme .admin-sider {
    box-shadow: 2px 0 8px rgba(0, 0, 0, 0.2);
  }
}
</style>
