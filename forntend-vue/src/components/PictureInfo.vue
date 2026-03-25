<template>
  <div class="picture-info">
    <!-- 作者信息 -->
    <div class="info-item">
      <div class="info-label">作者</div>
      <div class="info-content">
        <a-space>
          <a-avatar :size="28" :src="picture.user?.userAvatar" />
          <span class="author-name">{{ picture.user?.userName }}</span>
        </a-space>
      </div>
    </div>

    <!-- 基本信息 -->
    <div class="info-item">
      <div class="info-label">名称</div>
      <div class="info-content">{{ picture.name ?? '未命名' }}</div>
    </div>

    <div class="info-item">
      <div class="info-label">简介</div>
      <div class="info-content">{{ picture.introduction ?? '-' }}</div>
    </div>

    <div class="info-item" v-if="device === DEVICE_TYPE_ENUM.PC">
      <div class="info-label">分类</div>
      <div class="info-content">{{ picture.category ?? '默认' }}</div>
    </div>

    <div class="info-item" v-if="device === DEVICE_TYPE_ENUM.PC">
      <div class="info-label" >标签</div>
      <div class="info-content">
        <a-tag v-for="tag in picture.tags" :key="tag">{{ tag }}</a-tag>
      </div>
    </div>

    <!-- 其他信息 -->
    <div class="info-item">
      <div class="info-label">宽高比</div>
      <div class="info-content">{{ picture.picScale ?? '-' }}</div>
    </div>

    <div class="info-item">
      <div class="info-label">大小</div>
      <div class="info-content">{{ formatSize(picture.picSize) }}</div>
    </div>

    <div class="info-item">
      <div class="info-label">主色调</div>
      <div class="info-content">
        <div
          v-if="picture.picColor"
          class="color-block"
          :style="{ backgroundColor: toHexColor(picture.picColor) }"
        />
        <div v-else>-</div>
      </div>
    </div>

    <!-- 操作按钮 -->
    <div class="action-buttons">
      <a-space>
        <a-button type="primary" @click="$emit('download')" class="action-btn">
          <DownloadOutlined />
        </a-button>
        <a-button
          type="primary"
          ghost
          @click="$emit('share')"
          v-if="showShareButton"
          class="action-btn"
        >
          <ShareAltOutlined />
        </a-button>
        <a-button @click="$emit('edit')" v-if="canEdit" class="action-btn">
          <EditOutlined />
        </a-button>
        <a-button danger @click="$emit('delete')" v-if="canEdit" class="action-btn">
          <DeleteOutlined />
        </a-button>
      </a-space>
    </div>
  </div>
</template>

<style scoped>
.picture-info {
  padding: 12px 16px;
}

.info-item {
  display: flex;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.info-label {
  width: 60px;
  font-size: 13px;
  color: #666;
  flex-shrink: 0;
}

.info-content {
  flex: 1;
  font-size: 13px;
  color: #333;
  padding-left: 8px;
}

.author-name {
  font-size: 13px;
  font-weight: 500;
}

.color-block {
  width: 66px;
  height: 24px;
  border-radius: 4px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.action-buttons {
  padding: 12px 0 4px;
  display: flex;
  justify-content: flex-start;
}

.action-btn {
  width: 40px;
  height: 40px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
}

:deep(.ant-tag) {
  margin: 2px 4px 2px 0;
  font-size: 12px;
}
</style>

<script setup lang="ts">
import {
  DownloadOutlined,
  ShareAltOutlined,
  EditOutlined,
  DeleteOutlined,
} from '@ant-design/icons-vue'
import { onMounted, reactive, ref, onUnmounted } from 'vue'
import { formatSize, toHexColor } from '@/utils'
import { getDeviceType } from '@/utils/device.ts'
import { DEVICE_TYPE_ENUM } from '@/constants/device.ts'
const device = ref<string>('')
// 页面加载时获取设备类型并获取数据
onMounted(async () => {
  device.value = await getDeviceType()
})

defineProps<{
  picture: API.PictureVO
  showShareButton: boolean
  canEdit: boolean
}>()

defineEmits(['download', 'share', 'edit', 'delete'])
</script>
