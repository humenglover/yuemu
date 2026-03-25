import { createRouter, createWebHistory } from 'vue-router'
import type { RouteLocationNormalized, RouteLocationNormalizedLoaded } from 'vue-router'
import { shouldSaveScrollPosition } from '@/constants/route'

const DEFAULT_PAGE_TITLE = '悦木图库 - 发现、分享、创造美好瞬间'
let prevRoute: RouteLocationNormalizedLoaded | null = null

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'Home',
      component: () => import('@/pages/HomePage.vue'),
      meta: {
        keepAlive: true,
        title: '首页'
      },
    },
    {
      path: '/home',
      name: 'MyHome',
      component: () => import('@/pages/HomePage.vue'),
      meta: {
        keepAlive: true,
        title: '首页'
      },
    },
    {
      path: '/admin/knowledgeFileManage',
      name: 'AdminKnowledgeFileManage',
      component: () => import('@/pages/admin/KnowledgeFileManagePage.vue'),
      meta: {
        keepAlive: false,
        needLogin: true,
        requireAdmin: true,
        title: '知识库文件管理'
      }
    },
    {
      path: '/user/login',
      name: 'UserLogin',
      component: () => import('@/pages/user/UserLoginPage.vue'),
      meta: {
        keepAlive: false,
        title: '用户登录'
      },
    },
    {
      path: '/user/register',
      name: 'UserRegister',
      component: () => import('@/pages/user/UserRegisterPage.vue'),
      meta: {
        keepAlive: false,
        title: '用户注册'
      },
    },
    {
      path: '/user/setting',
      name: 'UserSetting',
      component: () => import('@/views/SettingView.vue'),
      meta: {
        keepAlive: false,
        title: '用户设置'
      },
    },
    {
      path: '/add_picture',
      name: 'AddPicture',
      component: () => import('@/pages/AddPicturePage.vue'),
      meta: {
        keepAlive: false,
        title: '创建图片'
      },
    },
    {
      path: '/admin/userManage',
      name: 'AdminUserManage',
      component: () => import('@/pages/admin/UserManagePage.vue'),
      meta: {
        keepAlive: false,
        title: '用户管理'
      },
    },
    {
      path: '/admin/pictureManage',
      name: 'AdminPictureManage',
      component: () => import('@/pages/admin/PictureManagePage.vue'),
      meta: {
        keepAlive: false,
        title: '图片管理'
      },
    },
    {
      path: '/admin/spaceManage',
      name: 'AdminSpaceManage',
      component: () => import('@/pages/admin/SpaceManagePage.vue'),
      meta: {
        keepAlive: false,
        title: '空间管理'
      },
    },
    {
      path: '/add_space',
      name: 'AddSpace',
      component: () => import('@/pages/AddSpacePage.vue'),
      meta: {
        keepAlive: false,
        title: '创建空间'
      },
    },
    {
      path: '/my_space',
      name: 'MySpace',
      component: () => import('@/pages/MySpacePage.vue'),
      meta: {
        keepAlive: false,
        title: '我的空间'
      },
    },
    {
      path: '/space/:id',
      name: 'SpaceDetail',
      component: () => import('@/pages/SpaceDetailPage.vue'),
      props: true,
      meta: {
        keepAlive: false,
        title: '空间详情'
      },
    },
    {
      path: '/spaceUserManage/:id',
      name: 'SpaceUserManage',
      component: () => import('@/pages/admin/SpaceUserManagePage.vue'),
      props: true,
      meta: {
        keepAlive: false,
        title: '空间成员管理'
      }
    },
    {
      path: '/my_ports',
      name: 'MyPosts',
      component: () => import('@/pages/MyPostsPage.vue'),
      props: true,
      meta: {
        keepAlive: false,
        title: '我的发布'
      },
    },
    {
      path: '/admin/systemNotifyManage',
      name: 'AdminSystemNotifyManage',
      component: () => import('@/pages/admin/SystemNotifyManagePage.vue'),
      meta: {
        keepAlive: false,
        needLogin: true,
        requireAdmin: true,
        title: '系统通知管理'
      }
    },
    {
      path: '/user/report-center',
      name: 'UserReportCenter',
      component: () => import('@/pages/user/UserReportCenter.vue'),
      meta: {
        keepAlive: false,
        needLogin: true,
        title: '举报中心'
      },
    },
    {
      path: '/my_teams',
      name: 'MyTeams',
      component: () => import('@/pages/MyTeamsPage.vue'),
      meta: {
        needLogin: true,
        title: '我的团队'
      },
    },
    {
      path: '/my',
      name: 'MyPage',
      component: () => import('@/pages/MyPage.vue'),
      meta: {
        keepAlive: true,
        title: '我的'
      },
      props: (route) => ({
        messageCenterUnreadCount: 0
      })
    },
    {
      path: '/space_analyze',
      name: 'SpaceAnalyze',
      component: () => import('@/pages/SpaceAnalyzePage.vue'),
      meta: {
        keepAlive: false,
        title: '空间分析'
      },
    },
    {
      path: '/search_picture',
      name: 'SearchPicture',
      component: () => import('@/pages/SearchPicturePage.vue'),
      meta: {
        keepAlive: false,
        title: '图片搜索'
      },
    },
    {
      path: '/admin/tagManage',
      name: 'AdminTagManage',
      component: () => import('@/pages/admin/TagManagePage.vue'),
      meta: {
        keepAlive: false,
        title: '标签管理'
      },
    },
    {
      path: '/admin/categoryManage',
      name: 'AdminCategoryManage',
      component: () => import('@/pages/admin/CategoryManagePage.vue'),
      meta: {
        keepAlive: false,
        title: '分类管理'
      },
    },
    {
      path: '/picture/:id',
      name: 'PictureDetail',
      component: () => import('@/pages/PictureDetailPage.vue'),
      props: true,
      meta: {
        keepAlive: false,
        title: '图片详情'
      }
    },
    {
      path: '/mobile/picture/:id',
      name: 'MobilePictureDetail',
      component: () => import('@/pages/MobilePictureDetailPage.vue'),
      props: true,
      meta: {
        keepAlive: false,
        title: '移动端图片详情'
      },
    },
    {
      path: '/picture-redirect/:id',
      name: 'PictureRedirect',
      component: () => import('@/pages/PictureRedirectPage.vue'),
      props: true,
      meta: {
        title: '图片详情重定向'
      },
    },
    {
      path: '/add_picture/batch',
      name: 'AddPictureBatch',
      component: () => import('@/pages/AddPictureBatchPage.vue'),
      meta: {
        keepAlive: false,
        title: '批量创建图片'
      },
    },
    {
      path: '/admin/reportManage',
      name: 'AdminReportManage',
      component: () => import('@/pages/admin/ReportManagePage.vue'),
      meta: {
        keepAlive: false,
        needLogin: true,
        requireAdmin: true,
        title: '举报管理'
      }
    },
    {
      path: '/follow-list',
      name: 'FollowList',
      component: () => import('@/pages/FollowListPage.vue'),
      meta: {
        keepAlive: false,
        title: '关注列表'
      },
    },
    {
      path: '/user/:id',
      name: 'UserDetail',
      component: () => import('@/pages/UserDetailPage.vue'),
      props: true,
      meta: {
        keepAlive: false,
        title: '用户详情'
      },
    },
    {
      path: '/friend-links',
      name: 'FriendLinks',
      component: () => import('@/pages/FriendLinks.vue'),
      meta: {
        keepAlive: true,
        title: '友情链接'
      },
    },
    {
      path: '/search',
      name: 'Search',
      component: () => import('@/pages/SearchPage.vue'),
      meta: {
        title: '搜索'
      }
    },
    {
      path: '/space_chat/:id',
      name: 'SpaceChat',
      component: () => import('@/pages/SpaceChatPage.vue'),
      meta: {
        title: '团队聊天室'
      }
    },
    {
      path: '/chat/:userId?',
      name: 'PrivateChat',
      component: () => import('@/pages/ChatPage.vue'),
      props: true,
      meta: {
        keepAlive: false,
        needLogin: true,
        title: '私聊'
      }
    },
    {
      path: '/forum',
      name: 'Forum',
      component: () => import('@/pages/ForumPage.vue'),
      meta: {
        keepAlive: true,
        title: '论坛'
      },
    },
    {
      path: '/chat-list',
      name: 'ChatList',
      component: () => import('@/pages/ChatListPage.vue'),
      meta: {
        needLogin: true,
        keepAlive: false,
        title: '聊天列表'
      }
    },
    {
      path: '/post/:id',
      name: 'PostDetail',
      component: () => import('@/pages/PostDetailPage.vue'),
      meta: {
        keepAlive: false,
        needLogin: true,
        title: '帖子详情'
      }
    },
    {
      path: '/post/edit/:id?',
      name: 'PostEdit',
      component: () => import('@/pages/PostEditPage.vue'),
      meta: {
        keepAlive: false,
        needLogin: true,
        title: '编辑帖子'
      }
    },
    {
      path: '/admin/postManage',
      name: 'AdminPostManage',
      component: ()=>import('@/pages/admin/PostManagePage.vue'),
      meta: {
        requireAuth: true,
        requireAdmin: true,
        title: '帖子管理'
      }
    },
    {
      path: '/admin/activityManage',
      name: 'AdminActivityManage',
      component: () => import('@/pages/admin/ActivityManagePage.vue'),
      meta: {
        requireAuth: true,
        requireAdmin: true,
        title: '活动管理'
      }
    },
    {
      path: '/space/:spaceId/activityManage',
      name: 'SpaceActivityManage',
      component: () => import('@/pages/SpaceActivityManagePage.vue'),
      props: true,
      meta: {
        requireAuth: true,
        needLogin: true,
        title: '空间活动管理'
      }
    },
    {
      path: '/activity/edit/:id?',
      name: 'ActivityEdit',
      component: () => import('@/pages/ActivityEditPage.vue'),
      meta: {
        keepAlive: false,
        needLogin: true,
        requireAdmin: true,
        title: '编辑活动'
      }
    },
    {
      path: '/activity/detail/:id',
      name: 'ActivityDetail',
      component: () => import('@/pages/ActivityDetailPage.vue'),
      meta: {
        keepAlive: false,
        title: '活动详情'
      }
    },
    {
      path: '/message-center',
      name: 'MessageCenter',
      component: () => import('@/pages/MessageCenterPage.vue'),
      meta: {
        keepAlive: false,
        needLogin: true,
        title: '消息中心'
      }
    },
    {
      path: '/message/history',
      name: 'InteractionHistory',
      component: () => import('@/pages/InteractionHistoryPage.vue'),
      meta: {
        keepAlive: false,
        needLogin: true,
        title: '互动历史'
      }
    },
    {
      path: '/user/reset-password',
      name: 'UserResetPassword',
      component: () => import('@/pages/user/UserResetPasswordPage.vue'),
      meta: {
        keepAlive: false,
        title: '重置密码'
      },
    },
    {
      path: '/chat/ai',
      name: 'AIChat',
      component: () => import('@/pages/AIChatPage.vue'),
      meta: {
        keepAlive: false,
        needLogin: true,
        title: 'AI聊天'
      }
    },
    {
      path: '/admin/manage',
      name: 'AdminManage',
      component: () => import('@/pages/AdminManagePage.vue'),
      meta: {
        keepAlive: false,
        needLogin: true,
        requireAdmin: true,
        title: '管理模块'
      }
    },
    {
      path: '/reminder',
      name: 'Reminder',
      component: () => import('@/pages/ReminderPage.vue'),
      meta: {
        keepAlive: false,
        needLogin: true,
        requireAdmin: true,
        title: '记事本'
      }
    },
    {
      path: '/admin/likeManage',
      name: 'AdminLikeManage',
      component: () => import('@/pages/admin/LikeManagePage.vue'),
      meta: {
        keepAlive: false,
        needLogin: true,
        requireAdmin: true,
        title: '点赞管理'
      }
    },
    {
      path: '/admin/shareManage',
      name: 'AdminShareManage',
      component: () => import('@/pages/admin/ShareManagePage.vue'),
      meta: {
        keepAlive: false,
        title: '分享管理'
      },
    },
    {
      path: '/admin/appManage',
      name: 'AdminAppManage',
      component: () => import('@/pages/admin/AppManagePage.vue'),
      meta: {
        keepAlive: false,
        needLogin: true,
        requireAdmin: true,
        title: 'APP管理'
      }
    },
    {
      path: '/admin/friendLinkManage',
      name: 'AdminFriendLinkManage',
      component: () => import('@/pages/admin/FriendLinkManagePage.vue'),
      meta: {
        keepAlive: false,
        title: '友链管理'
      }
    },
    {
      path: '/admin/audioManage',
      name: 'AdminAudioManage',
      component: () => import('@/pages/admin/AudioManagePage.vue'),
      meta: {
        keepAlive: false,
        needLogin: true,
        requireAdmin: true,
        title: '音频管理'
      }
    },
    {
      path: '/admin/chatManage',
      name: 'AdminChatManage',
      component: () => import('@/pages/admin/ChatManagePage.vue'),
      meta: {
        keepAlive: false,
        needLogin: true,
        requireAdmin: true,
        title: '聊天管理'
      }
    },
    {
      path: '/admin/loveBoardManage',
      name: 'AdminLoveBoardManage',
      component: () => import('@/pages/admin/LoveBoardManagePage.vue'),
      meta: {
        keepAlive: false,
        needLogin: true,
        requireAdmin: true,
        title: '恋爱画板管理'
      }
    },
    {
      path: '/admin/messageManage',
      name: 'AdminMessageManage',
      component: () => import('@/pages/admin/MessageManagePage.vue'),
      meta: {
        keepAlive: false,
        needLogin: true,
        requireAdmin: true,
        title: '留言管理'
      }
    },
    {
      path: '/admin/redisMonitor',
      name: 'AdminRedisMonitor',
      component: () => import('@/pages/admin/RedisCacheMonitorPage.vue'),
      meta: {
        keepAlive: false,
        needLogin: true,
        requireAdmin: true,
        title: 'Redis监控'
      }
    },
    {
      path: '/admin/aiChatManage',
      name: 'AdminAIChatManage',
      component: () => import('@/pages/admin/AiChatManagePage.vue'),
      meta: {
        keepAlive: false,
        needLogin: true,
        requireAdmin: true,
        title: 'AI聊天管理'
      }
    },
    {
      path: '/admin/sessionManage',
      name: 'AdminSessionManage',
      component: () => import('@/pages/admin/SessionManagePage.vue'),
      meta: {
        keepAlive: false,
        needLogin: true,
        requireAdmin: true,
        title: '会话管理'
      }
    },
    {
      path: '/admin/commentManage',
      name: 'AdminCommentManage',
      component: () => import('@/pages/admin/CommentManagePage.vue'),
      meta: {
        keepAlive: false,
        needLogin: true,
        requireAdmin: true,
        title: '评论管理'
      }
    },
    {
      path: '/games',
      children: [
        {
          path: 'snake',
          component: () => import('@/pages/games/SnakeGamePage.vue'),
          meta: {
            keepAlive: false,
            needLogin: true,
            title: '贪吃蛇'
          }
        },
        {
          path: '2048',
          component: () => import('@/pages/games/Game2048Page.vue'),
          meta: {
            keepAlive: false,
            needLogin: true,
            title: '2048'
          }
        },
        {
          path: 'queens',
          component: () => import('@/pages/games/QueensGamePage.vue'),
          meta: {
            keepAlive: false,
            needLogin: true,
            title: '八皇后'
          }
        },
        {
          path: 'tetris',
          component: () => import('@/pages/games/TetrisPage.vue'),
          meta: {
            keepAlive: false,
            needLogin: true,
            title: '俄罗斯方块'
          }
        },
        {
          path: 'sliding-puzzle',
          component: () => import('@/pages/games/SlidingPuzzlePage.vue'),
          meta: {
            title: '滑动拼图',
            keepAlive: false,
            needLogin: true
          }
        },
        {
          path: 'link-link',
          component: () => import('@/pages/games/LinkLinkPage.vue'),
          meta: {
            title: '连连看',
            keepAlive: false,
            needLogin: true
          }
        },
        {
          path: 'brick',
          component: () => import('@/pages/games/BrickGamePage.vue'),
          meta: {
            keepAlive: false,
            needLogin: true,
            title: '打砖块'
          }
        },
        {
          path: 'dino',
          component: () => import('@/pages/games/DinoGamePage.vue'),
          meta: {
            keepAlive: false,
            needLogin: true,
            title: '恐龙快跑'
          }
        },
        {
          path: 'memory-card',
          component: () => import('@/pages/games/MemoryCardPage.vue'),
          meta: {
            keepAlive: false,
            needLogin: true,
            title: '记忆翻牌'
          }
        },
        {
          path: 'minesweeper',
          component: () => import('@/pages/games/MinesweeperPage.vue'),
          meta: {
            keepAlive: false,
            needLogin: true,
            title: '扫雷'
          }
        }
      ]
    },
    {
      path: '/games',
      name: 'Games',
      component: () => import('@/pages/GamesPage.vue'),
      meta: {
        keepAlive: false,
        needLogin: true,
        title: '小游戏'
      }
    },
    {
      path: '/tools',
      name: 'Tools',
      component: () => import('@/pages/ToolsPage.vue'),
      meta: {
        title: '实用工具',
        requiresAuth: true
      }
    },
    {
      path: '/tools/calculator',
      name: 'Calculator',
      component: () => import('@/pages/tools/CalculatorPage.vue'),
      meta: {
        title: '计算器',
        requiresAuth: true
      }
    },
    {
      path: '/tools/timer',
      name: 'Timer',
      component: () => import('@/pages/tools/TimerPage.vue'),
      meta: {
        title: '计时器',
        requiresAuth: true
      }
    },
    {
      path: '/tools/food-wheel',
      name: 'FoodWheel',
      component: () => import('@/pages/tools/FoodWheelPage.vue'),
      meta: {
        title: '今天吃什么',
        requiresAuth: true
      }
    },
    {
      path: '/tools/sticky-wall',
      name: 'StickyWall',
      component: () => import('@/pages/tools/StickyWallPage.vue'),
      meta: {
        title: '便签墙',
        requiresAuth: true
      }
    },
    {
      path: '/tools/pomodoro',
      name: 'Pomodoro',
      component: () => import('@/pages/tools/PomodoroPage.vue'),
      meta: {
        title: '番茄钟',
        requiresAuth: true
      }
    },
    {
      path: '/tools/random',
      name: 'Random',
      component: () => import('@/pages/tools/RandomPage.vue'),
      meta: {
        title: '随机数生成器',
        requiresAuth: true
      }
    },
    {
      path: '/tools/base-converter',
      name: 'BaseConverter',
      component: () => import('@/pages/tools/BaseConverterPage.vue'),
      meta: {
        title: '进制转换器',
        requiresAuth: true
      }
    },
    {
      path: '/pc-chat',
      name: 'PCChat',
      component: () => import('@/pages/PCChatPage.vue'),
      meta: {
        needLogin: true,
        title: 'PC聊天'
      }
    },
    {
      path: '/chat-redirect',
      name: 'ChatRedirect',
      component: () => import('@/pages/ChatRedirectPage.vue'),
      meta: {
        needLogin: true,
        title: '聊天中转站'
      }
    },
    {
      path: '/tools/color-picker',
      name: 'ColorPicker',
      component: () => import('@/pages/tools/ColorPickerPage.vue'),
      meta: {
        title: '颜色选择器',
        requiresAuth: true
      }
    },
    {
      path: '/tools/video-compressor',
      name: 'VideoCompressor',
      component: () => import('@/pages/tools/VideoCompressorPage.vue'),
      meta: {
        title: '视频压缩',
        requiresAuth: true
      }
    },
    {
      path: '/tools/bankers-algorithm',
      name: 'BankersAlgorithm',
      component: () => import('@/pages/tools/BankersAlgorithmPage.vue'),
      meta: {
        title: '银行家算法演示',
        requiresAuth: true
      }
    },
    {
      path: '/barrage',
      name: 'Barrage',
      component: () => import('@/pages/BarragePage.vue'),
      meta: {
        title: '弹幕墙',
        requireAuth: false
      }
    },
    {
      path: '/browse-history',
      name: 'BrowseHistory',
      component: () => import('@/pages/BrowseHistoryPage.vue'),
      meta: {
        title: '浏览历史',
        keepAlive: false,
        needLogin: true
      }
    },
    {
      path: '/loveboard',
      name: 'LoveBoard',
      component: () => import('@/views/LoveBoardView.vue'),
      meta: {
        title: '恋爱画板',
        requireAuth: false,
        keepAlive: false,
      },
    },
    {
      path: '/loveboard/:id',
      name: 'LoveBoardShare',
      component: () => import('@/views/LoveBoardView.vue'),
      meta: {
        title: '恋爱画板',
        requireAuth: false,
        keepAlive: false,
      },
    },
    {
      path: '/loveboard/list',
      name: 'LoveBoardList',
      component: () => import('@/views/LoveBoardListView.vue'),
      meta: {
        title: '恋爱画板广场',
        requireAuth: false,
        keepAlive: false,
      },
    },
    {
      path: '/time-album/:id',
      name: 'TimeAlbumDetail',
      component: () => import('@/views/TimeAlbumDetailView.vue'),
      meta: {
        title: '相册详情',
        requireAuth: false,
        keepAlive: false,
      },
    },
    {
      path: '/music-album/:id',
      name: 'MusicAlbumDetail',
      component: () => import('@/views/MusicAlbumDetailView.vue'),
      meta: {
        title: '音乐专辑详情'
      }
    },
    {
      path: '/contact',
      name: 'Contact',
      component: () => import('@/pages/ContactPage.vue'),
      meta: {
        title: '联系我',
        keepAlive: false
      }
    },
    {
      path: '/discovery',
      name: 'Discovery',
      component: () => import('@/pages/DiscoveryPage.vue'),
      meta: {
        title: '发现',
        keepAlive: false
      }
    }
  ],
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    }
    if (shouldSaveScrollPosition(to.path)) {
      const savedScrollPosition = sessionStorage.getItem(`scroll_${to.path}`)
      if (savedScrollPosition) {
        const position = parseInt(savedScrollPosition)
        if (!isNaN(position)) {
          return { top: position }
        }
      }
    }
    return { top: 0 }
  }
})

router.beforeEach((to, from, next) => {
  // 异步保存滚动位置，避免阻塞路由切换
  if (shouldSaveScrollPosition(from.path)) {
    Promise.resolve().then(() => {
      const scrollPosition = window.pageYOffset || document.documentElement.scrollTop
      sessionStorage.setItem(`scroll_${from.path}`, scrollPosition.toString())
    })
  }
  prevRoute = from
  next()
})

let lastTitle = DEFAULT_PAGE_TITLE

router.afterEach((to) => {
  const newTitle = to.meta?.title ? `${String(to.meta.title)} - 悦木图库` : DEFAULT_PAGE_TITLE

  // 只有当标题真正改变时才更新DOM，减少不必要的DOM操作
  if (newTitle !== lastTitle) {
    document.title = newTitle
    lastTitle = newTitle
  }
})

export { prevRoute }
export default router
