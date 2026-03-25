<template>
  <div class="permission-setting">
    <div class="setting-title">隐私权限</div>
    <div class="permission-grid">
      <div
        class="permission-item"
        @click="togglePermission('allowPrivateChat')"
      >
        <div class="item-icon chat-icon">
          <i class="fas fa-comments"></i>
        </div>
        <div class="item-text">允许私聊</div>
        <div class="toggle-switch" :class="{ active: permissionData.allowPrivateChat === 1 }">
          <div class="toggle-thumb"></div>
        </div>
      </div>

      <div
        class="permission-item"
        @click="togglePermission('allowFollow')"
      >
        <div class="item-icon follow-icon">
          <i class="fas fa-user-plus"></i>
        </div>
        <div class="item-text">允许被关注</div>
        <div class="toggle-switch" :class="{ active: permissionData.allowFollow === 1 }">
          <div class="toggle-thumb"></div>
        </div>
      </div>

      <div
        class="permission-item"
        @click="togglePermission('showFollowList')"
      >
        <div class="item-icon list-icon">
          <i class="fas fa-users"></i>
        </div>
        <div class="item-text">展示关注列表</div>
        <div class="toggle-switch" :class="{ active: permissionData.showFollowList === 1 }">
          <div class="toggle-thumb"></div>
        </div>
      </div>

      <div
        class="permission-item"
        @click="togglePermission('showFansList')"
      >
        <div class="item-icon fans-icon">
          <i class="fas fa-heart"></i>
        </div>
        <div class="item-text">展示粉丝列表</div>
        <div class="toggle-switch" :class="{ active: permissionData.showFansList === 1 }">
          <div class="toggle-thumb"></div>
        </div>
      </div>
    </div>

    <div class="action-group">
      <button class="btn cancel-btn" @click="handleCancel" :disabled="saving">取消</button>
      <button class="btn save-btn" @click="handleSave" :disabled="saving">
        <span v-if="!saving">保存</span>
        <span v-else>
          <span class="loading-spinner"></span>
          保存中
        </span>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, defineProps, defineEmits, onMounted } from 'vue';
import { message } from 'ant-design-vue';
import { updateUserPermissionsUsingPost } from '@/api/userController';

interface Props {
  userId: number | string;
  initialPermissions?: {
    allowPrivateChat?: number;
    allowFollow?: number;
    showFollowList?: number;
    showFansList?: number;
  };
}

const props = withDefaults(defineProps<Props>(), {
  initialPermissions: () => ({
    allowPrivateChat: 1,
    allowFollow: 1,
    showFollowList: 1,
    showFansList: 1
  })
});

const emit = defineEmits<{
  saved: [];
  cancelled: [];
}>();

const permissionData = reactive<{
  allowPrivateChat: number;
  allowFollow: number;
  showFollowList: number;
  showFansList: number;
}>({
  allowPrivateChat: 1,
  allowFollow: 1,
  showFollowList: 1,
  showFansList: 1
});

// 在组件挂载时同步初始权限值
onMounted(() => {
  permissionData.allowPrivateChat = props.initialPermissions.allowPrivateChat ?? 1;
  permissionData.allowFollow = props.initialPermissions.allowFollow ?? 1;
  permissionData.showFollowList = props.initialPermissions.showFollowList ?? 1;
  permissionData.showFansList = props.initialPermissions.showFansList ?? 1;
});

const saving = ref(false);

const togglePermission = (key: 'allowPrivateChat' | 'allowFollow' | 'showFollowList' | 'showFansList') => {
  permissionData[key] = permissionData[key] === 1 ? 0 : 1;
};

const handleSave = async () => {
  if (!props.userId) {
    message.error('用户ID不能为空');
    return;
  }

  saving.value = true;
  try {
    const response = await updateUserPermissionsUsingPost({
      userId: props.userId,
      allowPrivateChat: permissionData.allowPrivateChat,
      allowFollow: permissionData.allowFollow,
      showFollowList: permissionData.showFollowList,
      showFansList: permissionData.showFansList
    });

    if (response.data.code === 0) {
      message.success('权限设置保存成功');
      emit('saved');
    } else {
      message.error(response.data.message || '保存失败');
    }
  } catch (error) {
    console.error('保存失败:', error);
    message.error('保存失败，请重试');
  } finally {
    saving.value = false;
  }
};

const handleCancel = () => {
  emit('cancelled');
};
</script>

<style scoped>
.permission-setting {
  padding: 16px;
  background: var(--card-background);
  max-width: 400px;
  margin: 0 auto;
  border-radius: 8px;
  transition: var(--theme-transition);
}

.setting-title {
  font-size: 17px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 16px;
}

.permission-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 8px;
  margin-bottom: 24px;
}

.permission-item {
  display: flex;
  align-items: center;
  padding: 12px 10px;
  border-radius: 6px;
  background: var(--hover-background);
  cursor: pointer;
  transition: var(--theme-transition);
}

.permission-item:hover {
  background: var(--nav-item-hover);
}

.item-icon {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
}

.chat-icon {
  color: var(--link-color);
}

.follow-icon {
  color: var(--markdown-heading-blue-border);
}

.list-icon {
  color: var(--markdown-heading-green-border);
}

.fans-icon {
  color: var(--markdown-heading-pink-border);
}

.item-text {
  flex: 1;
  font-size: 15px;
  color: var(--text-primary);
}

.toggle-switch {
  width: 40px;
  height: 22px;
  border-radius: 11px;
  background: var(--border-color);
  position: relative;
  transition: background 0.2s ease;
}

.toggle-switch.active {
  background: var(--link-color);
}

.toggle-thumb {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: var(--text-other);
  position: absolute;
  top: 2px;
  left: 2px;
  transition: left 0.2s ease;
  box-shadow: 0 1px 3px var(--shadow-color);
}

.toggle-switch.active .toggle-thumb {
  left: 20px;
}

.action-group {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}

.btn {
  padding: 8px 16px;
  border-radius: 4px;
  border: none;
  font-size: 14px;
  cursor: pointer;
  transition: var(--theme-transition);
}

.cancel-btn {
  background: var(--hover-background);
  color: var(--text-secondary);
}

.cancel-btn:hover {
  background: var(--border-color);
}

.save-btn {
  background: var(--link-color);
  color: var(--text-other);
  display: flex;
  align-items: center;
  gap: 6px;
}

.save-btn:hover:not(:disabled) {
  background: var(--link-hover-color);
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.loading-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid transparent;
  border-top: 2px solid var(--text-other);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

@media (min-width: 375px) {
  .permission-grid {
    grid-template-columns: 1fr;
  }
}
</style>
