import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('@/pages/HomePage.vue'),
      meta: {
        keepAlive: true,
        name: 'HomePage'
      },
    },
    {
      path: '/home',
      name: 'myhome',
      component: () => import('@/pages/HomePage.vue'),
      meta: {
        keepAlive: true,
      },
    },
    {
      path: '/user/login',
      name: '用户登录',
      component: () => import('@/pages/user/UserLoginPage.vue'),
      meta: {
        keepAlive: false,
      },
    },
    {
      path: '/user/register',
      name: '用户注册',
      component: () => import('@/pages/user/UserRegisterPage.vue'),
      meta: {
        keepAlive: false,
      },
    },
    {
      path: '/user/setting',
      name: '用户设置',
      component: () => import('@/views/SettingView.vue'),
      meta: {
        keepAlive: false,
      },
    },
    {
      path: '/add_picture',
      name: '创建图片',
      component: () => import('@/pages/AddPicturePage.vue'),
      meta: {
        keepAlive: false,
      },
    },
    {
      path: '/admin/userManage',
      name: '用户管理',
      component: () => import('@/pages/admin/UserManagePage.vue'),
      meta: {
        keepAlive: false,
      },
    },
    {
      path: '/admin/pictureManage',
      name: '图片管理',
      component: () => import('@/pages/admin/PictureManagePage.vue'),
      meta: {
        keepAlive: false,
      },
    },
    {
      path: '/admin/spaceManage',
      name: '空间管理',
      component: () => import('@/pages/admin/SpaceManagePage.vue'),
      meta: {
        keepAlive: false,
      },
    },
    {
      path: '/add_space',
      name: '创建空间',
      component: () => import('@/pages/AddSpacePage.vue'),
      meta: {
        keepAlive: false,
      },
    },
    {
      path: '/my_space',
      name: '我的空间',
      component: () => import('@/pages/MySpacePage.vue'),
      meta: {
        keepAlive: false,
      },
    },
    {
      path: '/space/:id',
      name: '空间详情',
      component: () => import('@/pages/SpaceDetailPage.vue'),
      props: true,
      meta: {
        keepAlive: true,
      },
    },
    {
      path: '/spaceUserManage/:id',
      name: '空间成员管理',
      component: () => import('@/pages/admin/SpaceUserManagePage.vue'),
      props: true,
    },
    {
      path: '/my_ports',
      name: '我的发布',
      component: () => import('@/pages/MyPostsPage.vue'),
      props: true,
      meta: {
        keepAlive: false,
      },
    },
    {
      path: '/my_teams',
      name: '我的团队',
      component: () => import('@/pages/MyTeamsPage.vue'),
      meta: {
        title: '我的团队',
        needLogin: true,
      },
    },
    {
      path: '/my',
      name: '我的',
      component: () => import('@/pages/MyPage.vue'),
      meta: {
        keepAlive: false,
      },
    },
    {
      path: '/space_analyze',
      name: '空间分析',
      component: () => import('@/pages/SpaceAnalyzePage.vue'),
      meta: {
        keepAlive: false,
      },
    },
    {
      path: '/search_picture',
      name: '图片搜索',
      component: () => import('@/pages/SearchPicturePage.vue'),
      meta: {
        keepAlive: false,
      },
    },
    {
      path: '/admin/tagManage',
      name: '标签管理',
      component: () => import('@/pages/admin/TagManagePage.vue'),
      meta: {
        keepAlive: false,
      },
    },
    {
      path: '/admin/categoryManage',
      name: '分类管理',
      component: () => import('@/pages/admin/CategoryManagePage.vue'),
      meta: {
        keepAlive: false,
      },
    },
    {
      path: '/picture/:id',
      name: '图片详情',
      component: () => import('@/pages/PictureDetailPage.vue'),
      props: true,
      meta: {
        keepAlive: false,
      },
    },
    {
      path: '/add_picture/batch',
      name: '批量创建图片',
      component: () => import('@/pages/AddPictureBatchPage.vue'),
      meta: {
        keepAlive: false,
      },
    },
    {
      path: '/follow-list',
      name: '关注列表',
      component: () => import('@/pages/FollowListPage.vue'),
      meta: {
        keepAlive: false,
      },
    },
    {
      path: '/user/:id',
      name: '用户详情',
      component: () => import('@/pages/UserDetailPage.vue'),
      props: true,
      meta: {
        keepAlive: false,
      },
    },
    {
      path: '/search',
      name: '搜索',
      component: () => import('@/pages/SearchPage.vue'),
      meta: {
        title: '搜索'
      }
    },
    {
      path: '/space_chat/:id',
      name: '团队聊天室',
      component: () => import('@/pages/SpaceChatPage.vue')
    },
    {
      path: '/chat/:userId?',
      name: '私聊',
      component: () => import('@/pages/ChatPage.vue'),
      props: true,
      meta: {
        keepAlive: false,
        needLogin: true
      }
    },
    {
      path: '/forum',
      name: '论坛',
      component: () => import('@/pages/ForumPage.vue'),
      meta: {
        keepAlive: true,
        name: 'ForumPage'
      },
    },
    {
      path: '/chat-list',
      name: '聊天列表',
      component: () => import('@/pages/ChatListPage.vue'),
      meta: {
        needLogin: true
      }
    },
    {
      path: '/post/:id',
      name: 'postDetail',
      component: () => import('@/pages/PostDetailPage.vue'),
      meta: {
        keepAlive: false,
        needLogin: true
      }
    },
    {
      path: '/post/edit/:id?',
      name: 'postEdit',
      component: () => import('@/pages/PostEditPage.vue'),
      meta: {
        keepAlive: false,
        needLogin: true
      }
    },
    {
      path: '/admin/postManage',
      name: 'postManage',
      component: ()=>import('@/pages/admin/PostManagePage.vue'),
      meta: {
        requireAuth: true,
        requireAdmin: true,
      }
    },
    {
      path: '/admin/activityManage',
      name: '活动管理',
      component: () => import('@/pages/admin/ActivityManagePage.vue'),
      meta: {
        requireAuth: true,
        requireAdmin: true,
      }
    },
    {
      path: '/activity/edit/:id?',
      name: '编辑活动',
      component: () => import('@/pages/ActivityEditPage.vue'),
      meta: {
        keepAlive: false,
        needLogin: true,
        requireAdmin: true
      }
    },
    {
      path: '/activity/detail/:id',
      name: '活动详情',
      component: () => import('@/pages/ActivityDetailPage.vue'),
      meta: {
        keepAlive: false
      }
    },
    {
      path: '/message-center',
      name: 'messageCenter',
      component: () => import('@/pages/MessageCenterPage.vue'),
      meta: {
        keepAlive: false,
        needLogin: true
      }
    },
    {
      path: '/message/history',
      name: 'InteractionHistory',
      component: () => import('@/pages/InteractionHistoryPage.vue'),
      meta: {
        keepAlive: false,
        needLogin: true
      }
    },
    {
      path: '/user/reset-password',
      name: '重置密码',
      component: () => import('@/pages/user/UserResetPasswordPage.vue'),
      meta: {
        keepAlive: false,
      },
    },
    {
      path: '/chat/ai',
      name: 'AIChat',
      component: () => import('@/pages/AIChatPage.vue'),
      meta: {
        keepAlive: false,
        needLogin: true
      }
    },
    {
      path: '/admin/manage',
      name: '管理模块',
      component: () => import('@/pages/AdminManagePage.vue'),
      meta: {
        keepAlive: false,
        needLogin: true,
        requireAdmin: true
      }
    },
    {
      path: '/reminder',
      name: '记事本',
      component: () => import('@/pages/ReminderPage.vue'),
      meta: {
        keepAlive: false,
        needLogin: true,
        requireAdmin: true
      }
    },
    {
      path: '/admin/appManage',
      name: 'APP管理',
      component: () => import('@/pages/admin/AppManagePage.vue'),
      meta: {
        keepAlive: false,
        needLogin: true,
        requireAdmin: true
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
            needLogin: true
          }
        },
        {
          path: '2048',
          component: () => import('@/pages/games/Game2048Page.vue'),
          meta: {
            keepAlive: false,
            needLogin: true
          }
        },
        {
          path: 'queens',
          component: () => import('@/pages/games/QueensGamePage.vue'),
          meta: {
            keepAlive: false,
            needLogin: true
          }
        },
        {
          path: 'tetris',
          component: () => import('@/pages/games/TetrisPage.vue'),
          meta: {
            keepAlive: false,
            needLogin: true
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
        }
      ]
    },
    {
      path: '/games',
      name: '小游戏',
      component: () => import('@/pages/GamesPage.vue'),
      meta: {
        keepAlive: false,
        needLogin: true
      }
    },
    {
      path: '/games/2048',
      name: '2048',
      component: () => import('@/pages/games/Game2048Page.vue'),
      meta: {
        keepAlive: false,
        needLogin: true
      }
    },
    {
      path: '/games/queens',
      name: 'Queens',
      component: () => import('@/pages/games/QueensGamePage.vue'),
      meta: {
        keepAlive: false,
        needLogin: true
      }
    },
    {
      path: '/games/tetris',
      name: '俄罗斯方块',
      component: () => import('@/pages/games/TetrisPage.vue'),
      meta: {
        keepAlive: false,
        needLogin: true
      }
    },
    {
      path: '/tools',
      name: 'tools',
      component: () => import('@/pages/ToolsPage.vue'),
      meta: {
        title: '实用工具',
        requiresAuth: true
      }
    },
    {
      path: '/tools/calculator',
      name: 'calculator',
      component: () => import('@/pages/tools/CalculatorPage.vue'),
      meta: {
        title: '计算器',
        requiresAuth: true
      }
    },
    {
      path: '/tools/timer',
      name: 'timer',
      component: () => import('@/pages/tools/TimerPage.vue'),
      meta: {
        title: '计时器',
        requiresAuth: true
      }
    },
    {
      path: '/tools/food-wheel',
      name: 'foodWheel',
      component: () => import('@/pages/tools/FoodWheelPage.vue'),
      meta: {
        title: '今天吃什么',
        requiresAuth: true
      }
    },
    {
      path: '/tools/sticky-wall',
      name: 'stickyWall',
      component: () => import('@/pages/tools/StickyWallPage.vue'),
      meta: {
        title: '便签墙',
        requiresAuth: true
      }
    },
    {
      path: '/tools/pomodoro',
      name: 'pomodoro',
      component: () => import('@/pages/tools/PomodoroPage.vue'),
      meta: {
        title: '番茄钟',
        requiresAuth: true
      }
    },
    {
      path: '/tools/random',
      name: 'random',
      component: () => import('@/pages/tools/RandomPage.vue'),
      meta: {
        title: '随机数生成器',
        requiresAuth: true
      }
    },
    {
      path: '/tools/base-converter',
      name: 'baseConverter',
      component: () => import('@/pages/tools/BaseConverterPage.vue'),
      meta: {
        title: '进制转换器',
        requiresAuth: true
      }
    },
    {
      path: '/tools/color-picker',
      name: 'colorPicker',
      component: () => import('@/pages/tools/ColorPickerPage.vue'),
      meta: {
        title: '颜色选择器',
        requiresAuth: true
      }
    },
    // {
    //   path: '/tools/video-compressor',
    //   name: 'videoCompressor',
    //   component: () => import('@/pages/tools/VideoCompressorPage.vue'),
    //   meta: {
    //     title: '视频压缩',
    //     requiresAuth: true
    //   }
    // },
    {
      path: '/games/brick',
      name: 'brick',
      component: () => import('@/pages/games/BrickGamePage.vue'),
      meta: {
        keepAlive: false,
        needLogin: true,
        title: '打砖块'
      }
    },
    {
      path: '/games/dino',
      name: 'dinoGame',
      component: () => import('@/pages/games/DinoGamePage.vue'),
      meta: {
        keepAlive: false,
        needLogin: true,
        title: '恐龙快跑'
      }
    },
    {
      path: '/games/memory-card',
      name: 'memoryCard',
      component: () => import('@/pages/games/MemoryCardPage.vue'),
      meta: {
        keepAlive: false,
        needLogin: true,
        title: '记忆翻牌'
      }
    },
    {
      path: '/games/minesweeper',
      name: 'minesweeper',
      component: () => import('@/pages/games/MinesweeperPage.vue'),
      meta: {
        keepAlive: false,
        needLogin: true,
        title: '扫雷'
      }
    },
    {
      path: '/barrage',
      name: 'barrage',
      component: () => import('@/pages/BarragePage.vue'),
      meta: {
        title: '弹幕墙',
        requireAuth: false
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
  ],
})

// 用于存储上一个路由信息的变量，初始化为 null，类型为 RouteLocationNormalizedLoaded | null
let prevRoute: any | null

// 全局前置导航守卫，在每次路由跳转前记录上一个路由
router.beforeEach((to, from, next): void => {
  prevRoute = from
  // 路由切换时清理全局滚动监听器
  window.onscroll = null
  // 移除所有滚动事件监听器
  const oldScrollListeners = window.listeners?.scroll || []
  oldScrollListeners.forEach(listener => {
    window.removeEventListener('scroll', listener)
  })
  next()
})

export default router
export { prevRoute }
