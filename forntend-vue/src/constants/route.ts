/**
 * 不需要显示PC端底部信息的路由路径
 */
export const HIDE_PC_FOOTER_ROUTES = [
  '/pc-chat',           // PC端聊天界面
  '/picture/:id',       // 图片详情页
  '/forum',             // 论坛
  '/barrage',
  '/admin',             // 所有管理员界面
  '/admin/:path*',      // 所有管理员子路由
  '/space/*',
  '/post/:path*',
  '/message-center',
  '/user/:path*',
  '/space/:path*/activityManage',
  '/netty-test',
  '/chat/:path*',
  '/my_ports',
  '/search',
  '/games/:path*',
  '/tools/:path*',
  '/reminder',
  '/space/:path*',
  '/my',
  '/activity/detail/:path*',
  '/activity/:path*',
  '/follow-list',
  '/discovery',
  '/my_teams',
  '/add_picture',
] as const;

/**
 * 检查当前路由是否需要隐藏PC端底部信息
 * @param path 当前路由路径
 */
export const shouldHidePCFooter = (path: string): boolean => {
  return HIDE_PC_FOOTER_ROUTES.some(route => {
    // 处理动态路由参数
    const routePattern = new RegExp('^' + route.replace(/:[^/]+/g, '[^/]+') + '$')
    return routePattern.test(path)
  })
}

/**
 * 不需要显示移动端底部信息的路由路径
 */
export const HIDE_MOBILE_FOOTER_ROUTES = [
  '/post/:id',          // 帖子详情页
  '/picture/:id',       // 图片详情页
  '/mobile/picture/:id',       // 图片详情页
  '/pc-chat',           // PC端聊天界面
  '/barrage',
  '/admin',             // 所有管理员界面
  '/admin/:path*',      // 所有管理员子路由
  '/space_chat/:path*',

  '/message-center',
  '/user/',
  '/netty-test',
  '/chat/:path*',
  '/space/:path*',
  '/search',
  '/games/:path*',
  '/my_ports',
  '/add_picture',
  '/tools/:path*'
] as const

/**
 * 检查当前路由是否需要隐藏移动端底部信息
 * @param path 当前路由路径
 */
export const shouldHideMobileFooter = (path: string): boolean => {
  return HIDE_MOBILE_FOOTER_ROUTES.some(route => {
    // 处理动态路由参数
    const routePattern = new RegExp('^' + route.replace(/:[^/]+/g, '[^/]+') + '$')
    return routePattern.test(path)
  })
}

/**
 * 不需要显示PC端顶部信息的路由路径
 */
export const HIDE_PC_HEADER_ROUTES = [
  '/picture/:id',       // 图片详情页
  '/barrage',
] as const

/**
 * 检查当前路由是否需要隐藏PC端顶部信息
 * @param path 当前路由路径
 */
export const shouldHidePCHeader = (path: string): boolean => {
  return HIDE_PC_HEADER_ROUTES.some(route => {
    // 处理动态路由参数
    const routePattern = new RegExp('^' + route.replace(/:[^/]+/g, '[^/]+') + '$')
    return routePattern.test(path)
  })
}

/**
 * 不需要显示返回上一页按钮的路由路径
 */
export const HIDE_BACK_BUTTON_ROUTES = [
  '/',                  // 主页
  '/home',              // 主页
  '/mobile/picture/:id', // 移动端图片详情页
] as const

/**
 * 检查当前路由是否需要隐藏返回上一页按钮
 * @param path 当前路由路径
 */
export const shouldHideBackButton = (path: string): boolean => {
  return HIDE_BACK_BUTTON_ROUTES.some(route => {
    // 处理动态路由参数
    const routePattern = new RegExp('^' + route.replace(/:[^/]+/g, '[^/]+') + '$')
    return routePattern.test(path)
  })
}

/**
 * 需要保存滚动位置的路由路径
 */
export const SAVE_SCROLL_POSITION_ROUTES = [
  '/',                  // 主页
  '/home',                  // 主页
  '/forum',            // 论坛页面
  '/my',               // 我的页面

] as const

/**
 * 检查当前路由是否需要保存滚动位置
 * @param path 当前路由路径
 */
export const shouldSaveScrollPosition = (path: string): boolean => {
  return SAVE_SCROLL_POSITION_ROUTES.some(route => {
    // 处理动态路由参数
    const routePattern = new RegExp('^' + route.replace(/:[^/]+/g, '[^/]+') + '$')
    return routePattern.test(path)
  })
}
