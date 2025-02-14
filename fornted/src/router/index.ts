import { createRouter, createWebHistory } from 'vue-router'
import SpaceUserManagePage from '@/pages/admin/SpaceUserManagePage.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('@/pages/HomePage.vue'),
      meta: {
        keepAlive: true,
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
        keepAlive: false,
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
        keepAlive: false,
        needLogin: true
      }
    },
    {
      path: '/chat-list',
      name: '聊天列表',
      component: () => import('@/pages/ChatListPage.vue'),
      meta: {
        keepAlive: false,
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
