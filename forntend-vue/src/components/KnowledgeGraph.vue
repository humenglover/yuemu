<template>
  <div class="knowledge-graph-container">
    <div class="relation-filter">
      <div class="title">关系类型筛选：</div>
      <button
        v-for="filter in filters"
        :key="filter.type"
        :class="['filter-btn', { active: currentFilter === filter.type }]"
        @click="changeFilter(filter.type)"
      >
        {{ filter.label }}
      </button>
    </div>
    <div ref="graphContainer" class="graph-container"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue';
import * as echarts from 'echarts';
import type { ComposeOption } from 'echarts/core';
import type { GraphSeriesOption } from 'echarts/charts';
import type { TooltipComponentOption, LegendComponentOption } from 'echarts/components';

interface GraphNode {
  id: number;
  name: string;
  label: string;
  category: number;
  symbolSize: number;
  tableDesc: string;
  categoryName?: string;
}

interface GraphLink {
  source: number;
  target: number;
  name: string;
  type: string;
  desc: string;
}

interface GraphCategory {
  name: string;
  itemStyle: {
    color: string;
  };
}

interface Filter {
  type: string;
  label: string;
}

type ECOption = ComposeOption<GraphSeriesOption | TooltipComponentOption | LegendComponentOption>;

const graphContainer = ref<HTMLDivElement | null>(null);
let myChart: echarts.ECharts | null = null;
const currentFilter = ref('core');

const filters: Filter[] = [
  { type: 'core', label: '核心业务关系' },
  { type: 'interact', label: '互动行为关系' },
  { type: 'all', label: '显示所有关系' }
];

const graphData: { nodes: GraphNode[]; links: GraphLink[]; categories: GraphCategory[] } = {
  nodes: [
    { id: 1, name: 'user', label: 'user\n用户表', category: 0, symbolSize: 90, tableDesc: '系统核心表，所有业务的基础，关联所有操作和数据' },
    { id: 2, name: 'space', label: 'space\n空间表', category: 0, symbolSize: 85, tableDesc: '资源归属核心表，管理用户图片/音频等资源的存储维度' },
    { id: 4, name: 'picture', label: 'picture\n图片表', category: 0, symbolSize: 85, tableDesc: '核心资源表，系统主要内容载体，关联所有互动行为' },
    { id: 5, name: 'post', label: 'post\n帖子表', category: 0, symbolSize: 85, tableDesc: '核心内容表，图文载体，关联所有互动行为' },
    { id: 3, name: 'space_user', label: 'space_user\n空间用户关联', category: 1, symbolSize: 60, tableDesc: '用户-空间多对多关联，管理空间成员权限' },
    { id: 6, name: 'post_attachment', label: 'post_attachment\n帖子附件', category: 1, symbolSize: 60, tableDesc: '帖子-图片/文件关联，存储帖子附件信息' },
    { id: 7, name: 'audio_file', label: 'audio_file\n音频表', category: 1, symbolSize: 70, tableDesc: '音频资源表，归属空间，关联用户' },
    { id: 8, name: 'activity', label: 'activity\n活动表', category: 1, symbolSize: 70, tableDesc: '活动发布表，归属空间，关联用户' },
    { id: 9, name: 'like_record', label: 'like_record\n点赞表', category: 2, symbolSize: 75, tableDesc: '通用点赞表，直接关联用户、图片、帖子、空间' },
    { id: 10, name: 'comments', label: 'comments\n评论表', category: 2, symbolSize: 75, tableDesc: '评论表，直接关联用户、图片、帖子' },
    { id: 11, name: 'favorite_record', label: 'favorite_record\n收藏表', category: 2, symbolSize: 75, tableDesc: '通用收藏表，直接关联用户、图片、帖子、空间' },
    { id: 12, name: 'view_record', label: 'view_record\n浏览表', category: 2, symbolSize: 70, tableDesc: '通用浏览表，直接关联用户、图片、帖子、空间、用户' },
    { id: 13, name: 'share_record', label: 'share_record\n分享表', category: 2, symbolSize: 70, tableDesc: '通用分享表，直接关联用户、图片、帖子' },
    { id: 14, name: 'userfollows', label: 'userfollows\n关注表', category: 3, symbolSize: 55, tableDesc: '用户关注关系表' },
    { id: 15, name: 'private_chat', label: 'private_chat\n私聊表', category: 3, symbolSize: 55, tableDesc: '用户私聊表' },
    { id: 16, name: 'chat_message', label: 'chat_message\n聊天消息', category: 3, symbolSize: 50, tableDesc: '聊天消息表' },
    { id: 17, name: 'love_board', label: 'love_board\n恋爱板', category: 3, symbolSize: 55, tableDesc: '恋爱画板模块核心表' },
    { id: 27, name: 't_system_notify', label: 't_system_notify\n系统通知', category: 3, symbolSize: 55, tableDesc: '系统通知表' },
    { id: 29, name: 'report', label: 'report\n举报表', category: 3, symbolSize: 55, tableDesc: '通用举报表' },
    { id: 32, name: 'category', label: 'category\n分类表', category: 3, symbolSize: 50, tableDesc: '资源分类表' },
    { id: 33, name: 'tag', label: 'tag\n标签表', category: 3, symbolSize: 50, tableDesc: '资源标签表' },
    { id: 34, name: 'aichat', label: 'aichat\nAI聊天表', category: 3, symbolSize: 55, tableDesc: 'AI聊天消息表，存储用户与AI的对话记录' },
    { id: 35, name: 'app_version', label: 'app_version\nAPP版本表', category: 3, symbolSize: 55, tableDesc: 'APP版本管理表，记录版本号、更新内容等' },
    { id: 36, name: 'bug_report', label: 'bug_report\nBUG报告表', category: 3, symbolSize: 55, tableDesc: '用户提交的BUG报告记录表' },
    { id: 37, name: 'friend_link', label: 'friend_link\n友情链接表', category: 3, symbolSize: 55, tableDesc: '网站友情链接管理表' },
    { id: 38, name: 'game_2048_record', label: 'game_2048_record\n2048游戏记录', category: 3, symbolSize: 50, tableDesc: '2048游戏玩家得分记录表' },
    { id: 39, name: 'hot_search', label: 'hot_search\n热门搜索表', category: 3, symbolSize: 55, tableDesc: '热门搜索关键词记录表' },
    { id: 40, name: 'love_board_music_album', label: 'love_board_music_album\n恋爱板音乐专栏', category: 3, symbolSize: 50, tableDesc: '恋爱板关联的音乐专栏表' },
    { id: 41, name: 'message', label: 'message\n留言板表', category: 3, symbolSize: 50, tableDesc: '通用留言板表' },
    { id: 42, name: 'message_board', label: 'message_board\n祝福板表', category: 3, symbolSize: 50, tableDesc: '祝福板留言记录表' },
    { id: 43, name: 'rag_session_message', label: 'rag_session_message\n智能客服消息', category: 3, symbolSize: 50, tableDesc: '智能客服会话消息记录表' },
    { id: 44, name: 'rag_user_session', label: 'rag_user_session\n智能客服会话', category: 3, symbolSize: 50, tableDesc: '智能客服用户会话表' },
    { id: 45, name: 'reminder', label: 'reminder\n提醒事项表', category: 3, symbolSize: 55, tableDesc: '用户提醒事项管理表' },
    { id: 46, name: 'snake_game_record', label: 'snake_game_record\n贪吃蛇游戏记录', category: 3, symbolSize: 50, tableDesc: '贪吃蛇游戏玩家得分记录表' },
    { id: 47, name: 'user_search_record', label: 'user_search_record\n用户搜索记录', category: 3, symbolSize: 55, tableDesc: '用户搜索关键词记录表' },
    { id: 48, name: 'user_sign_in_record', label: 'user_sign_in_record\n用户签到记录', category: 3, symbolSize: 55, tableDesc: '用户每日签到记录表' },
    { id: 49, name: 'user_system_notify_read', label: 'user_system_notify_read\n通知阅读状态', category: 3, symbolSize: 50, tableDesc: '用户系统通知阅读状态表' },
    { id: 50, name: 'time_album', label: 'time_album\n时光相册表', category: 3, symbolSize: 50, tableDesc: '恋爱板关联的时光相册表' },
    { id: 51, name: 'wei_yan', label: 'wei_yan\n微言表', category: 3, symbolSize: 55, tableDesc: '恋爱板微言发布表' }
  ],
  links: [
    { source: 1, target: 2, name: '创建', type: 'core', desc: '用户创建空间 user.id → space.userId' },
    { source: 2, target: 1, name: '归属', type: 'core', desc: '空间归属用户 space.userId → user.id' },
    { source: 1, target: 3, name: '加入', type: 'core', desc: '用户加入空间 user.id → space_user.userId' },
    { source: 2, target: 3, name: '关联', type: 'core', desc: '空间关联用户 space.id → space_user.spaceId' },
    { source: 1, target: 4, name: '发布', type: 'core', desc: '用户发布图片 user.id → picture.userId' },
    { source: 4, target: 1, name: '归属', type: 'core', desc: '图片归属用户 picture.userId → user.id' },
    { source: 2, target: 4, name: '归属', type: 'core', desc: '图片归属空间 space.id → picture.spaceId' },
    { source: 4, target: 2, name: '关联', type: 'core', desc: '图片关联空间 picture.spaceId → space.id' },
    { source: 1, target: 5, name: '发布', type: 'core', desc: '用户发布帖子 user.id → post.userId' },
    { source: 5, target: 1, name: '归属', type: 'core', desc: '帖子归属用户 post.userId → user.id' },
    { source: 5, target: 6, name: '包含', type: 'core', desc: '帖子包含附件 post.id → post_attachment.postId' },
    { source: 6, target: 5, name: '关联', type: 'core', desc: '附件关联帖子 post_attachment.postId → post.id' },
    { source: 4, target: 32, name: '关联', type: 'core', desc: '图片关联分类 picture.category → category.name' },
    { source: 4, target: 33, name: '关联', type: 'core', desc: '图片关联标签 picture.tags → tag.name' },
    { source: 5, target: 32, name: '关联', type: 'core', desc: '帖子关联分类 post.category → category.name' },
    { source: 5, target: 33, name: '关联', type: 'core', desc: '帖子关联标签 post.tags → tag.name' },
    { source: 1, target: 7, name: '上传', type: 'core', desc: '用户上传音频 user.id → audio_file.userId' },
    { source: 7, target: 1, name: '归属', type: 'core', desc: '音频归属用户 audio_file.userId → user.id' },
    { source: 2, target: 7, name: '归属', type: 'core', desc: '音频归属空间 space.id → audio_file.spaceId' },
    { source: 1, target: 8, name: '发布', type: 'core', desc: '用户发布活动 user.id → activity.userId' },
    { source: 8, target: 1, name: '归属', type: 'core', desc: '活动归属用户 activity.userId → user.id' },
    { source: 2, target: 8, name: '归属', type: 'core', desc: '活动归属空间 space.id → activity.spaceId' },
    { source: 1, target: 9, name: '点赞', type: 'interact', desc: '用户给资源点赞 user.id → like_record.userId' },
    { source: 9, target: 1, name: '归属', type: 'interact', desc: '点赞记录归属用户 like_record.userId → user.id' },
    { source: 9, target: 4, name: '点赞', type: 'interact', desc: '点赞针对图片 like_record.targetId → picture.id (type=1)' },
    { source: 4, target: 9, name: '统计', type: 'interact', desc: '图片点赞数 picture.likeCount ← like_record 统计' },
    { source: 9, target: 5, name: '点赞', type: 'interact', desc: '点赞针对帖子 like_record.targetId → post.id (type=2)' },
    { source: 5, target: 9, name: '统计', type: 'interact', desc: '帖子点赞数 post.likeCount ← like_record 统计' },
    { source: 9, target: 2, name: '点赞', type: 'interact', desc: '点赞针对空间 like_record.targetId → space.id (type=3)' },
    { source: 9, target: 8, name: '点赞', type: 'interact', desc: '点赞针对活动 like_record.targetId → activity.id (type=4)' },
    { source: 9, target: 7, name: '点赞', type: 'interact', desc: '点赞针对音频 like_record.targetId → audio_file.id (type=5)' },
    { source: 9, target: 17, name: '点赞', type: 'interact', desc: '点赞针对恋爱板 like_record.targetId → love_board.id (type=6)' },
    { source: 1, target: 10, name: '评论', type: 'interact', desc: '用户发布评论 user.id → comments.userId' },
    { source: 10, target: 1, name: '归属', type: 'interact', desc: '评论归属用户 comments.userId → user.id' },
    { source: 10, target: 4, name: '评论', type: 'interact', desc: '评论针对图片 comments.targetId → picture.id (type=1)' },
    { source: 4, target: 10, name: '包含', type: 'interact', desc: '图片包含评论 picture.id → comments.targetId' },
    { source: 10, target: 5, name: '评论', type: 'interact', desc: '评论针对帖子 comments.targetId → post.id (type=2)' },
    { source: 5, target: 10, name: '包含', type: 'interact', desc: '帖子包含评论 post.id → comments.targetId' },
    { source: 1, target: 11, name: '收藏', type: 'interact', desc: '用户收藏资源 user.id → favorite_record.userId' },
    { source: 11, target: 1, name: '归属', type: 'interact', desc: '收藏记录归属用户 favorite_record.userId → user.id' },
    { source: 11, target: 4, name: '收藏', type: 'interact', desc: '收藏针对图片 favorite_record.targetId → picture.id (type=1)' },
    { source: 4, target: 11, name: '统计', type: 'interact', desc: '图片收藏数 picture.favoriteCount ← favorite_record 统计' },
    { source: 11, target: 5, name: '收藏', type: 'interact', desc: '收藏针对帖子 favorite_record.targetId → post.id (type=2)' },
    { source: 5, target: 11, name: '统计', type: 'interact', desc: '帖子收藏数 post.favoriteCount ← favorite_record 统计' },
    { source: 11, target: 2, name: '收藏', type: 'interact', desc: '收藏针对空间 favorite_record.targetId → space.id (type=3)' },
    { source: 1, target: 12, name: '浏览', type: 'interact', desc: '用户浏览资源 user.id → view_record.userId' },
    { source: 12, target: 1, name: '归属', type: 'interact', desc: '浏览记录归属用户 view_record.userId → user.id' },
    { source: 12, target: 4, name: '浏览', type: 'interact', desc: '浏览针对图片 view_record.targetId → picture.id (type=1)' },
    { source: 4, target: 12, name: '统计', type: 'interact', desc: '图片浏览量 picture.viewCount ← view_record 统计' },
    { source: 12, target: 5, name: '浏览', type: 'interact', desc: '浏览针对帖子 view_record.targetId → post.id (type=2)' },
    { source: 5, target: 12, name: '统计', type: 'interact', desc: '帖子浏览量 post.viewCount ← view_record 统计' },
    { source: 12, target: 2, name: '浏览', type: 'interact', desc: '浏览针对空间 view_record.targetId → space.id (type=3)' },
    { source: 12, target: 1, name: '浏览', type: 'interact', desc: '浏览针对用户 view_record.targetId → user.id (type=4)' },
    { source: 12, target: 8, name: '浏览', type: 'interact', desc: '浏览针对活动 view_record.targetId → activity.id (type=5)' },
    { source: 12, target: 7, name: '播放', type: 'interact', desc: '播放音频 view_record.targetId → audio_file.id (type=6)' },
    { source: 7, target: 12, name: '统计', type: 'interact', desc: '音频播放量 audio_file.viewCount ← view_record 统计' },
    { source: 1, target: 13, name: '分享', type: 'interact', desc: '用户分享资源 user.id → share_record.userId' },
    { source: 13, target: 1, name: '归属', type: 'interact', desc: '分享记录归属用户 share_record.userId → user.id' },
    { source: 13, target: 4, name: '分享', type: 'interact', desc: '分享针对图片 share_record.targetId → picture.id (type=1)' },
    { source: 4, target: 13, name: '统计', type: 'interact', desc: '图片分享数 picture.shareCount ← share_record 统计' },
    { source: 13, target: 5, name: '分享', type: 'interact', desc: '分享针对帖子 share_record.targetId → post.id (type=2)' },
    { source: 5, target: 13, name: '统计', type: 'interact', desc: '帖子分享数 post.shareCount ← share_record 统计' },
    { source: 13, target: 8, name: '分享', type: 'interact', desc: '分享针对活动 share_record.targetId → activity.id (type=3)' },
    { source: 1, target: 14, name: '关注', type: 'core', desc: '用户关注他人 user.id → userfollows.followerId' },
    { source: 14, target: 1, name: '归属', type: 'core', desc: '关注记录归属用户 userfollows.followerId → user.id' },
    { source: 1, target: 15, name: '创建', type: 'core', desc: '用户创建私聊 user.id → private_chat.userId' },
    { source: 15, target: 1, name: '归属', type: 'core', desc: '私聊归属用户 private_chat.userId → user.id' },
    { source: 15, target: 16, name: '包含', type: 'core', desc: '私聊包含消息 private_chat.id → chat_message.privateChatId' },
    { source: 1, target: 16, name: '发送', type: 'core', desc: '用户发送消息 user.id → chat_message.senderId' },
    { source: 4, target: 16, name: '关联', type: 'core', desc: '消息关联图片 picture.id → chat_message.pictureId' },
    { source: 2, target: 16, name: '关联', type: 'core', desc: '消息关联空间 space.id → chat_message.spaceId' },
    { source: 1, target: 17, name: '创建', type: 'core', desc: '用户创建恋爱板 user.id → love_board.userId' },
    { source: 17, target: 1, name: '归属', type: 'core', desc: '恋爱板归属用户 love_board.userId → user.id' },
    { source: 17, target: 40, name: '包含', type: 'core', desc: '恋爱板包含音乐专栏 love_board.id → love_board_music_album.loveBoardId' },
    { source: 1, target: 40, name: '创建', type: 'core', desc: '用户创建音乐专栏 user.id → love_board_music_album.userId' },
    { source: 17, target: 50, name: '包含', type: 'core', desc: '恋爱板包含时光相册 love_board.id → time_album.loveBoardId' },
    { source: 1, target: 50, name: '创建', type: 'core', desc: '用户创建时光相册 user.id → time_album.userId' },
    { source: 1, target: 51, name: '发布', type: 'core', desc: '用户发布微言 user.id → wei_yan.userId' },
    { source: 17, target: 51, name: '包含', type: 'core', desc: '恋爱板包含微言 love_board.id → wei_yan.loveBoardId' },
    { source: 1, target: 27, name: '接收', type: 'core', desc: '用户接收系统通知 user.id → t_system_notify.receiverId' },
    { source: 27, target: 49, name: '关联', type: 'core', desc: '通知关联阅读状态 t_system_notify.id → user_system_notify_read.systemNotifyId' },
    { source: 1, target: 49, name: '归属', type: 'core', desc: '阅读状态归属用户 user.id → user_system_notify_read.userId' },
    { source: 1, target: 29, name: '发起', type: 'core', desc: '用户发起举报 user.id → report.userId' },
    { source: 4, target: 29, name: '关联', type: 'core', desc: '举报针对图片 report.targetId → picture.id (type=1)' },
    { source: 5, target: 29, name: '关联', type: 'core', desc: '举报针对帖子 report.targetId → post.id (type=2)' },
    { source: 10, target: 29, name: '关联', type: 'core', desc: '举报针对评论 report.targetId → comments.commentId (type=3)' },
    { source: 1, target: 29, name: '关联', type: 'core', desc: '举报针对用户 report.targetId → user.id (type=4)' },
    { source: 1, target: 34, name: '对话', type: 'core', desc: '用户与AI对话 user.id → aichat.userId' },
    { source: 1, target: 44, name: '创建', type: 'core', desc: '用户创建智能客服会话 user.id → rag_user_session.userId' },
    { source: 44, target: 43, name: '包含', type: 'core', desc: '会话包含消息 rag_user_session.id → rag_session_message.sessionId' },
    { source: 1, target: 43, name: '发送', type: 'core', desc: '用户发送智能客服消息 user.id → rag_session_message.userId' },
    { source: 1, target: 36, name: '提交', type: 'core', desc: '用户提交BUG报告 user.id → bug_report.userId' },
    { source: 1, target: 37, name: '申请', type: 'core', desc: '用户申请友情链接 user.id → friend_link.userId' },
    { source: 1, target: 38, name: '游玩', type: 'core', desc: '用户玩2048游戏 user.id → game_2048_record.userId' },
    { source: 1, target: 46, name: '游玩', type: 'core', desc: '用户玩贪吃蛇游戏 user.id → snake_game_record.userId' },
    { source: 1, target: 45, name: '创建', type: 'core', desc: '用户创建提醒事项 user.id → reminder.userId' },
    { source: 1, target: 47, name: '搜索', type: 'core', desc: '用户搜索内容 user.id → user_search_record.userId' },
    { source: 1, target: 48, name: '签到', type: 'core', desc: '用户签到 user.id → user_sign_in_record.userId' },
    { source: 1, target: 41, name: '留言', type: 'core', desc: '用户留言 user.id → message.ip (间接关联)' },
    { source: 1, target: 42, name: '留言', type: 'core', desc: '用户祝福板留言 user.id → message_board.userId' },
    { source: 1, target: 42, name: '拥有', type: 'core', desc: '用户拥有祝福板 user.id → message_board.ownerId' },
    { source: 39, target: 47, name: '统计', type: 'core', desc: '热门搜索来自用户搜索 hot_search.count ← user_search_record 统计' }
  ],
  categories: [
    { name: '核心四表', itemStyle: { color: '#2563eb' } },
    { name: '核心关联表', itemStyle: { color: '#dc2626' } },
    { name: '互动行为表', itemStyle: { color: '#059669' } },
    { name: '其他业务表', itemStyle: { color: '#64748b' } }
  ]
};

graphData.nodes.forEach((node: GraphNode) => {
  node.categoryName = graphData.categories[node.category].name;
});

const filterLinks = (filterType: string) => {
  switch (filterType) {
    case 'core':
      return graphData.links.filter(link => link.type === 'core');
    case 'interact':
      return graphData.links.filter(link => link.type === 'interact');
    case 'all':
      return graphData.links;
    default:
      return graphData.links.filter(link => link.type === 'core');
  }
};

const getEchartsOption = (filterType: string) => {
  return {
    title: {
      text: '项目业务知识图谱',
      left: 'center',
      textStyle: { fontSize: 18, color: '#1e293b', fontWeight: 600 }
    },
    tooltip: {
      trigger: 'item',
      backgroundColor: '#ffffff',
      borderColor: '#e2e8f0',
      borderWidth: 1,
      padding: 12,
      textStyle: { color: '#1e293b', fontSize: 14 },
      formatter: (params: any) => {
        if (params.dataType === 'node') {
          return `<div style="min-width: 250px;">
                      <div style="color: #2563eb; font-weight: 600; margin-bottom: 4px;">【${(params.data as GraphNode).categoryName}】</div>
                      <div><span style="color: #64748b;">表名：</span>${(params.data as GraphNode).name}（${(params.data as GraphNode).label.split('\n')[1]}）</div>
                      <div style="margin-top: 4px;"><span style="color: #64748b;">说明：</span>${(params.data as GraphNode).tableDesc}</div>
                  </div>`;
        } else if (params.dataType === 'edge') {
          const sourceNode = graphData.nodes.find((node: GraphNode) => node.id === (params.data as GraphLink).source);
          const targetNode = graphData.nodes.find((node: GraphNode) => node.id === (params.data as GraphLink).target);
          return `<div style="min-width: 200px;">
                      <div><span style="color: #64748b;">关系类型：</span>${params.data.type === 'core' ? '核心业务关系' : '互动行为关系'}</div>
                      <div style="margin: 4px 0;"><span style="color: #64748b;">关联：</span>${sourceNode?.name} → ${targetNode?.name}</div>
                      <div><span style="color: #64748b;">关系：</span>${params.data.name}</div>
                      <div style="margin-top: 4px;"><span style="color: #64748b;">说明：</span>${params.data.desc}</div>
                  </div>`;
        }
        return '';
      }
    },
    legend: {
      orient: 'vertical',
      top: 'bottom',
      left: 'right',
      data: graphData.categories.map((cate: any) => cate.name),
      textStyle: { fontSize: 13, color: '#475569' },
      itemWidth: 14,
      itemHeight: 14
    },
    series: [
      {
        type: 'graph',
        layout: 'force',
        data: graphData.nodes,
        links: filterLinks(filterType),
        categories: graphData.categories,
        roam: true,
        label: {
          show: true,
          position: 'inside',
          fontSize: 12,
          color: '#ffffff',
          fontWeight: 500
        },
        force: {
          repulsion: 220,
          edgeLength: 110,
          gravity: 0.1,
          layoutAnimation: true
        },
        lineStyle: {
          color: 'source',
          curveness: 0.1,
          width: 1.2
        },
        emphasis: {
          focus: 'adjacency',
          lineStyle: { width: 2.5 }
        }
      }
    ]
  };
};

const changeFilter = (filterType: string) => {
  currentFilter.value = filterType;
  if (myChart) {
    myChart.setOption(getEchartsOption(filterType));
  }
};

const initChart = () => {
  if (!graphContainer.value) return;
  myChart = echarts.init(graphContainer.value);
  myChart.setOption(getEchartsOption(currentFilter.value));
};

const handleResize = () => {
  if (myChart) {
    myChart.resize();
  }
};

onMounted(() => {
  nextTick(() => {
    initChart();
  });
  window.addEventListener('resize', handleResize);
});

onUnmounted(() => {
  if (myChart) {
    myChart.dispose();
  }
  window.removeEventListener('resize', handleResize);
});
</script>

<style scoped>
.knowledge-graph-container {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: var(--background);
  padding: 16px;
  box-sizing: border-box;
  transition: var(--theme-transition);
}

.relation-filter {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  background: var(--card-background);
  border-radius: 6px;
  margin-bottom: 16px;
  flex-wrap: wrap;
  border: 1px solid var(--border-color);
  transition: var(--theme-transition);
}

.relation-filter .title {
  font-size: 14px;
  color: var(--text-primary);
  font-weight: 600;
  white-space: nowrap;
}

.filter-btn {
  padding: 8px 20px;
  border: 1px solid var(--border-color);
  border-radius: 4px;
  background: var(--card-background);
  color: var(--text-primary);
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s ease;
  font-weight: 500;
}

.filter-btn:hover:not(.active) {
  border-color: var(--link-color);
  color: var(--link-color);
  background-color: var(--hover-background);
}

.filter-btn.active {
  background: var(--link-color);
  color: var(--text-other);
  border-color: var(--link-color);
}

.graph-container {
  flex: 1;
  width: 100%;
  border-radius: 6px;
  background-color: var(--card-background);
  border: 1px solid var(--border-color);
  min-height: 600px;
  transition: var(--theme-transition);
}
</style>
