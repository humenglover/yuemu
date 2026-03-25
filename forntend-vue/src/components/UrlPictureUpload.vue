<template>
  <div class="url-upload-container">
    <div v-if="previewUrl" class="preview-area">
      <img :src="previewUrl" class="preview-image" />
      <div class="preview-mask" @click="clearPreview">
        <div class="change-icon">
          <i class="fa-solid fa-link-slash"></i>
        </div>
        <p class="change-text">点击更换链接</p>
      </div>
    </div>

    <div v-else class="url-input-card">
      <div class="illustration-wrapper">
        <img src="@/assets/illustrations/url-upload-illustration.png" alt="链接上传插图" class="url-illustration" />
      </div>

      <div class="input-section">
        <p class="primary-text">通过图片链接发布</p>
        <div class="url-input-group">
          <div class="input-wrapper">
            <i class="fa-solid fa-link prefix-icon"></i>
            <input
              v-model="url"
              placeholder="请粘贴或输入图片 URL 地址..."
              class="modern-input"
              :disabled="loading"
              @keyup.enter="handlePreview"
            />
          </div>
          <button
            class="submit-button"
            :class="{ loading: loading }"
            :disabled="!url || loading"
            @click="handlePreview"
          >
            <i v-if="loading" class="fa-solid fa-spinner fa-spin"></i>
            <span v-else>立即抓取</span>
          </button>
        </div>
        <p class="tip-text">支持 http/https 协议的公开图片地址</p>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { uploadPictureByUrlUsingPost } from '@/api/pictureController'

interface Props {
  picture?: API.PictureVO
  spaceId?: number
  onSuccess?: (newPicture: API.PictureVO) => void
  onUploadStart?: () => void
  onUploadProgress?: (progress: number) => void
}

const props = defineProps<Props>()
const url = ref<string>('')
const loading = ref<boolean>(false)
const previewUrl = ref<string>('')

// 监听 picture 属性的变化
watch(
  () => props.picture,
  (newPicture) => {
    if (newPicture?.url) {
      previewUrl.value = newPicture.url
    }
  },
  { immediate: true },
)

/**
 * 上传图片
 */
const handleSubmit = async () => {
  loading.value = true
  props.onUploadStart?.()
  try {
    const params: API.PictureUploadRequest = { fileUrl: url.value }
    if (props.spaceId) params.spaceId = props.spaceId
    if (props.picture?.id) params.id = props.picture.id

    const res = await uploadPictureByUrlUsingPost(params)
    if (res.data.code === 0 && res.data.data) {
      props.onSuccess?.(res.data.data)
      message.success('同步成功')
    } else {
      message.error(res.data.message || '抓取失败')
      previewUrl.value = ''
    }
  } catch (error: any) {
    console.error('图片抓取失败', error)
    message.error('由于跨域或链接无效，抓取失败')
    previewUrl.value = ''
  } finally {
    loading.value = false
  }
}

// 预览图片并自动触发同步
const handlePreview = async () => {
  if (!url.value) return
  if (!url.value.match(/^https?:\/\/.+/)) {
    message.warning('请输入有效的 http/https 链接')
    return
  }

  try {
    loading.value = true
    previewUrl.value = url.value
    await handleSubmit()
  } catch (error) {
    loading.value = false
  }
}

// 清除预览
const clearPreview = () => {
  previewUrl.value = ''
  url.value = ''
}
</script>

<style scoped>
.url-upload-container {
  padding: 0;
  height: 100%;
}

.url-input-card {
  background: var(--card-background);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 60px 40px;
  width: 100%;
  min-height: 440px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 32px;
  transition: var(--theme-transition);
}

.url-input-card:hover {
  border-color: #3b82f6;
  background: var(--hover-background);
}

.illustration-wrapper {
  width: 140px;
  height: 140px;
  transition: transform 0.3s ease;
}

.url-input-card:hover .illustration-wrapper {
  transform: scale(1.02);
}

.url-illustration {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.input-section {
  width: 100%;
  max-width: 600px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
}

.primary-text {
  font-size: 18px;
  font-weight: 500;
  color: var(--text-primary);
  margin: 0;
}

.url-input-group {
  width: 100%;
  display: flex;
  gap: 8px;
}

.input-wrapper {
  flex: 1;
  position: relative;
  display: flex;
  align-items: center;
}

.prefix-icon {
  position: absolute;
  left: 12px;
  color: var(--text-secondary);
  font-size: 14px;
}

.modern-input {
  width: 100%;
  height: 44px;
  padding: 0 12px 0 36px;
  border-radius: 8px;
  border: 1px solid var(--border-color);
  background: var(--card-background);
  font-size: 14px;
  outline: none;
  transition: all 0.2s ease;
  color: var(--text-primary);
}

.modern-input:focus {
  border-color: #3b82f6;
}

.submit-button {
  height: 44px;
  padding: 0 24px;
  border-radius: 8px;
  background: #3b82f6;
  color: white;
  border: none;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s ease;
  font-size: 14px;
}

.submit-button:hover:not(:disabled) {
  background: #2563eb;
}

.submit-button:disabled {
  background: var(--hover-background);
  color: var(--text-secondary);
  cursor: not-allowed;
}

.tip-text {
  font-size: 12px;
  color: var(--text-secondary);
}

/* 预览区域样式 */
.preview-area {
  width: 100%;
  height: 440px;
  position: relative;
  overflow: hidden;
  border-radius: 12px;
  background: var(--background);
}

.preview-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.preview-mask {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: all 0.3s ease;
  color: white;
  cursor: pointer;
  gap: 8px;
}

.preview-area:hover .preview-mask {
  opacity: 1;
}

.change-icon {
  font-size: 24px;
  margin-bottom: 4px;
}

.change-text {
  font-size: 14px;
  font-weight: 400;
}

@media screen and (max-width: 768px) {
  .url-input-card {
    padding: 32px 20px;
    min-height: 300px;
    gap: 24px;
  }
}
</style>
