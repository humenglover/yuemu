<template>
  <div class="permission-setting">
    <div class="setting-title">互动权限</div>
    <div class="permission-grid">
      <div
        class="permission-item"
        @click="togglePermission('allowLike')"
      >
        <div class="item-icon like-icon">
          <svg viewBox="0 0 24 24" fill="currentColor">
            <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
          </svg>
        </div>
        <div class="item-text">允许点赞</div>
        <div class="toggle-switch" :class="{ active: permissionData.allowLike === 1 }">
          <div class="toggle-thumb"></div>
        </div>
      </div>

      <div
        class="permission-item"
        @click="togglePermission('allowComment')"
      >
        <div class="item-icon comment-icon">
          <svg viewBox="0 0 24 24" fill="currentColor">
            <path d="M21 9V7c0-1.1-.9-2-2-2H5c-1.1 0-2 .9-2 2v2c-1.21.91-2 2.37-2 4 0 2.76 2.24 5 5 5h14c1.65 0 3-1.35 3-3 0-1.63-.79-3.09-2-4zM5 9V7h14v2H5zm14 5H5c-1.66 0-3-1.34-3-3s1.34-3 3-3v6c0 1.66 1.34 3 3 3h14v-6z"/>
          </svg>
        </div>
        <div class="item-text">允许评论</div>
        <div class="toggle-switch" :class="{ active: permissionData.allowComment === 1 }">
          <div class="toggle-thumb"></div>
        </div>
      </div>

      <div
        class="permission-item"
        @click="togglePermission('allowCollect')"
      >
        <div class="item-icon collect-icon">
          <svg viewBox="0 0 24 24" fill="currentColor">
            <path d="M17 3H7c-1.1 0-1.99.9-1.99 2L5 21l7-3 7 3V5c0-1.1-.9-2-2-2z"/>
          </svg>
        </div>
        <div class="item-text">允许收藏</div>
        <div class="toggle-switch" :class="{ active: permissionData.allowCollect === 1 }">
          <div class="toggle-thumb"></div>
        </div>
      </div>

      <div
        class="permission-item"
        @click="togglePermission('allowShare')"
      >
        <div class="item-icon share-icon">
          <svg viewBox="0 0 24 24" fill="currentColor">
            <path d="M18 16.08c-.76 0-1.44.3-1.96.77L8.91 12.7c.05-.23.09-.46.09-.7s-.04-.47-.09-.7l7.05-4.11c.54.5 1.25.81 2.04.81 1.66 0 3-1.34 3-3s-1.34-3-3-3-3 1.34-3 3c0 .24.04.47.09.7L8.04 9.81C7.5 9.31 6.79 9 6 9c-1.66 0-3 1.34-3 3s1.34 3 3 3c.79 0 1.5-.31 2.04-.81l7.12 4.16c-.05.21-.08.43-.08.65 0 1.61 1.31 2.92 2.92 2.92 1.61 0 2.92-1.31 2.92-2.92s-1.31-2.92-2.92-2.92z"/>
          </svg>
        </div>
        <div class="item-text">允许分享</div>
        <div class="toggle-switch" :class="{ active: permissionData.allowShare === 1 }">
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
import { ref, reactive, defineProps, defineEmits } from 'vue';
import { message } from 'ant-design-vue';
import { setPicturePermissionUsingPost } from '@/api/pictureController';

interface Props {
  pictureId: number | string;
  initialPermissions?: {
    allowLike?: number;
    allowComment?: number;
    allowCollect?: number;
    allowShare?: number;
  };
}

const props = withDefaults(defineProps<Props>(), {
  initialPermissions: () => ({
    allowLike: 1,
    allowComment: 1,
    allowCollect: 1,
    allowShare: 1
  })
});

const emit = defineEmits<{
  saved: [];
  cancelled: [];
}>();

const permissionData = reactive<{
  allowLike: number;
  allowComment: number;
  allowCollect: number;
  allowShare: number;
}>({
  allowLike: props.initialPermissions.allowLike ?? 1,
  allowComment: props.initialPermissions.allowComment ?? 1,
  allowCollect: props.initialPermissions.allowCollect ?? 1,
  allowShare: props.initialPermissions.allowShare ?? 1
});

const saving = ref(false);

const togglePermission = (key: 'allowLike' | 'allowComment' | 'allowCollect' | 'allowShare') => {
  permissionData[key] = permissionData[key] === 1 ? 0 : 1;
};

const handleSave = async () => {
  if (!props.pictureId) {
    message.error('图片ID不能为空');
    return;
  }
  saving.value = true;
  try {
    const response = await setPicturePermissionUsingPost({
      pictureId: props.pictureId,
      ...permissionData
    });
    if (response.data.code === 0) {
      message.success('保存成功');
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

.like-icon {
  color: var(--like-button-active-color);
}

.comment-icon {
  color: var(--link-color);
}

.collect-icon {
  color: var(--markdown-heading-yellow-border);
}

.share-icon {
  color: var(--ai-avatar-background);
}

.item-icon svg {
  width: 20px;
  height: 20px;
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
