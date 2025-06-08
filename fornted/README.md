# 悦木图库前端项目

一个基于 Vue 3 + TypeScript 开发的现代化图片分享社区前端项目。支持图片上传、分享、社交互动等功能。
![img.png](public/img.png)
![img_1.png](public/img_1.png)
![img_2.png](public/img_2.png)
![img_3.png](public/img_3.png)
![img_4.png](public/img_4.png)
![img_5.png](public/img_5.png)

将 vite.config.ts 文件中的your server url替换为实际服务器地址
```angular2html
proxy: {
'/api': {
target: isProd ? 'your server url' : 'http://localhost:8123',
changeOrigin: true,
ws: true
}
        },
```
将.env.production文件中的VITE_WS_URL替换为实际WS服务器地址

## 技术栈

- **核心框架**: Vue 3
- **开发语言**: TypeScript
- **状态管理**: Pinia
- **UI 框架**:
  - Ant Design Vue (PC端)
  - Vant (移动端)
- **路由**: Vue Router
- **HTTP 请求**: Axios
- **构建工具**: Vite
- **代码规范**: ESLint + Prettier
- **CSS 预处理器**: SCSS/LESS
- **AI 能力**:
  - DeepSeek AI
  - 阿里通义绘画

## 主要功能

- 用户系统
  - 登录/注册
  - 邮箱验证
  - 个人信息管理
  - 用户关注
- 图片管理
  - 图片上传
  - 图片编辑
  - 图片分类
  - 图片搜索
- 空间系统
  - 空间创建
  - 成员管理
  - 空间分析
- 社交功能
  - 评论互动
  - 点赞分享
  - 私信聊天
- 内容发现
  - 个性化推荐
  - 热门排行
  - 关注动态
- 消息通知
  - 互动提醒
  - 系统通知
  - 未读消息
- 记事本功能
  - 日程提醒
  - 待办事项
  - 重要标记
  - 日期筛选
  - 完成状态追踪
- 休闲游戏
  - 贪吃蛇
    - 三种游戏模式
    - 全局排行榜
    - 分数记录
    - 难度递增
  - 更多游戏开发中...
- AI 绘画
  - DeepSeek AI 绘画
    - 文本生成图像
    - 多风格选择
    - 参数调节
  - 阿里通义绘画
    - 中文优化
    - 风格迁移
    - 图像编辑

## 项目结构
![img.png](public/img_6.png)

## 开发环境搭建

1. 安装依赖 npm install
2. 启动开发服务器 npm run dev
3. 构建生产版本 npm run build
