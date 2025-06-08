<template>
  <div class="admin-manage-page">
    <!-- 添加背景动画 -->
    <div class="background-animation">
      <div class="gradient-bg"></div>
    </div>

    <div class="page-header">
      <div class="header-content">
        <h1>管理模块</h1>
        <p class="sub-title">系统管理员控制面板</p>
      </div>
    </div>

    <div class="admin-grid">
      <a-button class="admin-button" @click="() => handleAdminClick('userManage')">
        <UserOutlined class="admin-icon" />
        <div class="button-text">
          <span class="title">用户管理</span>
          <span class="desc">管理系统用户</span>
        </div>
      </a-button>

      <a-button class="admin-button" @click="() => handleAdminClick('pictureManage')">
        <PictureOutlined class="admin-icon" />
        <div class="button-text">
          <span class="title">图片管理</span>
          <span class="desc">管理系统图片</span>
        </div>
      </a-button>

      <a-button class="admin-button" @click="() => handleAdminClick('postManage')">
        <FileTextOutlined class="admin-icon" />
        <div class="button-text">
          <span class="title">帖子管理</span>
          <span class="desc">管理系统帖子</span>
        </div>
      </a-button>

      <a-button class="admin-button" @click="() => handleAdminClick('spaceManage')">
        <AppstoreOutlined class="admin-icon" />
        <div class="button-text">
          <span class="title">空间管理</span>
          <span class="desc">管理用户空间</span>
        </div>
      </a-button>

      <a-button class="admin-button" @click="() => handleAdminClick('tagManage')">
        <TagsOutlined class="admin-icon" />
        <div class="button-text">
          <span class="title">标签管理</span>
          <span class="desc">管理系统标签</span>
        </div>
      </a-button>

      <a-button class="admin-button" @click="() => handleAdminClick('categoryManage')">
        <FolderOutlined class="admin-icon" />
        <div class="button-text">
          <span class="title">分类管理</span>
          <span class="desc">管理系统分类</span>
        </div>
      </a-button>

      <a-button class="admin-button" @click="() => handleAdminClick('appManage')">
        <AppstoreOutlined class="admin-icon" />
        <div class="button-text">
          <span class="title">APP管理</span>
          <span class="desc">管理APP版本</span>
        </div>
      </a-button>

      <a-button class="admin-button" @click="() => handleAdminClick('activityManage')">
        <CalendarOutlined class="admin-icon" />
        <div class="button-text">
          <span class="title">活动管理</span>
          <span class="desc">管理系统活动</span>
        </div>
      </a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import {
  UserOutlined,
  PictureOutlined,
  FileTextOutlined,
  AppstoreOutlined,
  TagsOutlined,
  FolderOutlined,
  CalendarOutlined
} from '@ant-design/icons-vue'

const router = useRouter()

const handleAdminClick = (route: string) => {
  router.push(`/admin/${route}`)
}
</script>

<style scoped>
.admin-manage-page {
  min-height: 88vh;
  background: #f8fafc;
  position: relative;
  overflow: hidden;
  margin-left: -20px !important;
  margin-right: -20px !important;
  margin-top: -40px;
}

.background-animation {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;

  pointer-events: none;
}

.gradient-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(114, 46, 209, 0.05), rgba(79, 70, 229, 0.1));
}

.page-header {
  position: relative;
  padding: 40px 20px;
  background: linear-gradient(135deg, rgba(114, 46, 209, 0.1), rgba(79, 70, 229, 0.15));
  margin-bottom: 24px;
}

.header-content {
  max-width: 1200px;
  margin: 0 auto;
  text-align: center;
}

.header-content h1 {
  margin: 0;
  font-size: 32px;
  color: #1a1a1a;
  font-weight: 600;
}

.sub-title {
  margin: 8px 0 0;
  color: #666;
  font-size: 16px;
}

.admin-grid {
  position: relative;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 4px;
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.admin-button {
  height: 120px;
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 16px;
  border-radius: 16px;
  border: 1px solid rgba(114, 46, 209, 0.1);
  background: white;
  transition: all 0.3s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 24px rgba(114, 46, 209, 0.15);
    border-color: #722ed1;
  }
}

.admin-icon {
  font-size: 32px;
  color: #722ed1;
  transition: all 0.3s ease;
}

.button-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
  text-align: left;
}

.button-text .title {
  font-size: 16px;
  font-weight: 500;
  color: #1a1a1a;
}

.button-text .desc {
  font-size: 14px;
  color: #666;
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .page-header {
    padding: 24px 16px;
  }

  .header-content h1 {
    font-size: 24px;
  }

  .sub-title {
    font-size: 14px;
  }

  .admin-grid {
    grid-template-columns: 1fr;
    gap: 4px;
    padding: 0 16px;
  }

  .admin-button {
    height: auto;
    padding: 12px;
  }

  .admin-icon {
    font-size: 24px;
  }

  .button-text .title {
    font-size: 15px;
  }

  .button-text .desc {
    font-size: 13px;
  }
}
</style>
