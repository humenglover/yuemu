<template>
  <div class="upload-container">
    <div
      class="upload-box"
      :class="{ uploading: isUploading }"
      @click="handleClick"
      @drop.prevent="handleDrop"
      @dragover.prevent
    >
      <div v-if="!previewUrl" class="upload-content">
        <div class="illustration-wrapper">
          <img src="@/assets/illustrations/upload-illustration.png" alt="上传插图" class="upload-illustration" />
        </div>
        <div class="upload-info">
          <p class="primary-text">
            {{ isMobile ? '点击上传精彩' : '点击或拖拽上传图片' }}
          </p>
          <div class="support-tips">
            <span class="tip-tag">JPG</span>
            <span class="tip-tag">PNG</span>
            <span class="tip-tag">GIF</span>
            <span class="tip-tag">WEBP</span>
          </div>
        </div>

        <!-- 上传中的动画遮罩 -->
        <div v-if="isUploading" class="uploading-overlay">
          <div class="spinner"></div>
          <span>正在处理中...</span>
        </div>
      </div>

      <div v-else class="preview-content">
        <img :src="previewUrl" class="preview-image" />
        <div class="preview-mask">
          <div class="change-icon">
            <i class="fa-solid fa-camera-rotate"></i>
          </div>
          <p class="change-text">点击更换一张</p>
        </div>
      </div>

      <input
        type="file"
        ref="fileInput"
        style="display: none"
        accept="image/*"
        @change="handleFileChange"
      />
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted, watch } from 'vue'
import { message } from 'ant-design-vue'
import { uploadPictureUsingPost } from '@/api/pictureController'

// 图片压缩配置
const MAX_ORIGINAL_SIZE = 20 * 1024 * 1024 // 20MB，原始文件最大限制
const MAX_COMPRESSED_SIZE = 5 * 1024 * 1024 // 5MB，压缩后的目标大小
const MAX_WIDTH = 2560 // 最大宽度
const MAX_HEIGHT = 1440 // 最大高度

interface Props {
  picture?: API.PictureVO
  spaceId?: number
  onSuccess?: (newPicture: API.PictureVO, file: File) => void
  onUploadStart?: () => void
  onUploadProgress?: (progress: number) => void
}

const props = defineProps<Props>()
const fileInput = ref<HTMLInputElement | null>(null)
const previewUrl = ref<string>('')
const isUploading = ref(false)

// 检测是否为移动设备
const isMobile = ref(false)

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

onMounted(() => {
  // 检查是否为移动设备
  isMobile.value = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(
    navigator.userAgent,
  )
})

// 检查浏览器是否支持 WebP
const checkWebPSupport = (): Promise<boolean> => {
  return new Promise((resolve) => {
    const webP = new Image()
    webP.onload = function() {
      const result = webP.width > 0 && webP.height > 0
      resolve(result)
    }
    webP.onerror = function() {
      resolve(false)
    }
    webP.src = 'data:image/webp;base64,UklGRhoAAABXRUJQVlA4TA0AAAAvAAAAEAcQERGIiP4HAA=='
  })
}

// 压缩图片函数
const compressImage = async (file: File): Promise<File> => {
  // 检查 WebP 支持
  const supportsWebP = await checkWebPSupport()
  const targetFormat = supportsWebP ? 'image/webp' : 'image/jpeg'
  const targetQuality = targetFormat === 'image/webp' ? 0.85 : 0.92 // WebP 可以用稍低的质量因为其压缩效率更高

  return new Promise((resolve, reject) => {
    // 如果文件已经是 WebP 且小于目标大小，直接返回
    if (file.type === 'image/webp' && file.size <= MAX_COMPRESSED_SIZE) {
      resolve(file)
      return
    }

    const reader = new FileReader()
    reader.readAsDataURL(file)
    reader.onload = (e) => {
      const img = new Image()
      img.src = e.target?.result as string

      img.onload = () => {
        const canvas = document.createElement('canvas')
        let width = img.width
        let height = img.height

        // 计算初始缩放比例
        const initialScale = Math.min(1, Math.sqrt(MAX_COMPRESSED_SIZE / file.size))
        const aspectRatio = width / height

        // 应用初始缩放
        width = Math.round(width * initialScale)
        height = Math.round(height * initialScale)

        // 确保不超过最大尺寸
        if (width > MAX_WIDTH || height > MAX_HEIGHT) {
          if (aspectRatio > 1) {
            width = Math.min(width, MAX_WIDTH)
            height = Math.round(width / aspectRatio)
          } else {
            height = Math.min(height, MAX_HEIGHT)
            width = Math.round(height * aspectRatio)
          }
        }

        canvas.width = width
        canvas.height = height

        const ctx = canvas.getContext('2d', { alpha: targetFormat === 'image/webp' })
        if (!ctx) {
          reject(new Error('无法创建canvas上下文'))
          return
        }

        // 优化渲染质量
        ctx.imageSmoothingEnabled = true
        ctx.imageSmoothingQuality = 'high'

        // 如果是 JPEG，设置白色背景
        if (targetFormat === 'image/jpeg') {
          ctx.fillStyle = '#FFFFFF'
          ctx.fillRect(0, 0, width, height)
        }

        ctx.drawImage(img, 0, 0, width, height)

        // 递归压缩函数，使用二分查找快速找到合适的压缩率
        const compressWithQuality = (minQuality: number, maxQuality: number, attempt = 0): Promise<Blob> => {
          return new Promise((resolveBlob, rejectBlob) => {
            // 最多尝试8次，或者当质量区间足够小时停止
            if (attempt > 8 || maxQuality - minQuality < 0.01) {
              canvas.toBlob(
                (blob) => {
                  if (!blob) {
                    rejectBlob(new Error('图片压缩失败'))
                    return
                  }
                  resolveBlob(blob)
                },
                targetFormat,
                minQuality
              )
              return
            }

            const currentQuality = (minQuality + maxQuality) / 2

            canvas.toBlob(
              (blob) => {
                if (!blob) {
                  rejectBlob(new Error('图片压缩失败'))
                  return
                }

                if (Math.abs(blob.size - MAX_COMPRESSED_SIZE) < MAX_COMPRESSED_SIZE * 0.05) {
                  // 如果大小在目标大小的5%误差范围内，接受这个结果
                  resolveBlob(blob)
                } else if (blob.size > MAX_COMPRESSED_SIZE) {
                  // 如果太大，尝试更低的质量
                  compressWithQuality(minQuality, currentQuality, attempt + 1)
                    .then(resolveBlob)
                    .catch(rejectBlob)
                } else {
                  // 如果太小，尝试更高的质量
                  compressWithQuality(currentQuality, maxQuality, attempt + 1)
                    .then(resolveBlob)
                    .catch(rejectBlob)
                }
              },
              targetFormat,
              currentQuality
            )
          })
        }

        // 开始压缩，质量范围从0.5到目标质量
        compressWithQuality(0.5, targetQuality)
          .then((finalBlob) => {
            // 始终使用.webp扩展名
            const extension = targetFormat === 'image/webp' ? '.webp' : '.jpg'
            const newFileName = file.name.replace(/\.[^/.]+$/, '') + extension
            const compressedFile = new File([finalBlob], newFileName, {
              type: targetFormat,
              lastModified: Date.now(),
            })

            // 输出压缩信息
            const compressionRatio = ((1 - compressedFile.size / file.size) * 100).toFixed(1)
            console.log(`图片压缩完成:
              原始大小: ${(file.size / 1024 / 1024).toFixed(2)}MB
              压缩后大小: ${(compressedFile.size / 1024 / 1024).toFixed(2)}MB
              压缩率: ${compressionRatio}%
              分辨率: ${width}x${height}
              原始格式: ${file.type}
              转换格式: ${targetFormat}
              WebP支持: ${supportsWebP ? '是' : '否'}`)

            resolve(compressedFile)
          })
          .catch((error) => {
            reject(error)
          })
      }

      img.onerror = () => reject(new Error('图片加载失败'))
    }

    reader.onerror = () => reject(new Error('文件读取失败'))
  })
}

// 点击上传区域时触发文件选择
const handleClick = () => {
  if (isUploading.value) {
    message.warning('正在上传中，请稍候...')
    return
  }
  fileInput.value?.click()
}

// 处理文件拖放
const handleDrop = async (e: DragEvent) => {
  if (isMobile.value || isUploading.value) return

  e.preventDefault()
  const files = e.dataTransfer?.files
  if (files && files.length > 0) {
    try {
      const file = files[0]
      // 创建预览URL
      previewUrl.value = URL.createObjectURL(file)
      // 压缩并上传
      await handleUpload(file)
    } catch (error) {
      console.error('处理文件失败:', error)
      message.error('处理文件失败，请重试')
    }
  }
}

// 处理文件选择变化
const handleFileChange = async (e: Event) => {
  const input = e.target as HTMLInputElement
  const files = input.files
  if (files && files.length > 0) {
    try {
      const file = files[0]
      // 创建预览URL
      previewUrl.value = URL.createObjectURL(file)
      // 压缩并上传
      await handleUpload(file)
    } catch (error) {
      console.error('处理文件失败:', error)
      message.error('处理文件失败，请重试')
    }
  }
}

// 处理文件上传
const handleUpload = async (file: File) => {
  // 检查文件类型
  if (!file.type.startsWith('image/')) {
    message.error('请选择图片文件')
    return
  }

  // 检查文件大小
  if (file.size > MAX_ORIGINAL_SIZE) {
    message.error('图片大小不能超过20MB')
    return
  }

  isUploading.value = true
  props.onUploadStart?.()

  try {
    // 压缩并转换图片
    const compressedFile = await compressImage(file)

    // 如果压缩后仍然超过5MB，提示用户
    if (compressedFile.size > MAX_COMPRESSED_SIZE) {
      message.warning('图片过大，可能会影响加载速度')
    }

    const formData = new FormData()
    formData.append('file', compressedFile)
    if (props.spaceId) {
      formData.append('spaceId', props.spaceId.toString())
    }
    if (props.picture?.id) {
      formData.append('id', props.picture.id.toString())
    }

    const res = await uploadPictureUsingPost(
      {
        spaceId: props.spaceId,
        id: props.picture?.id,
      },
      {},
      compressedFile,
      {
        onUploadProgress: (progressEvent) => {
          const progress = Math.round(
            (progressEvent.loaded * 100) / (progressEvent.total ?? 100)
          )
          props.onUploadProgress?.(progress)
        },
      }
    )

    if (res.data.code === 0 && res.data.data) {
      props.onSuccess?.(res.data.data, compressedFile)
    } else {
      message.error('图片上传失败：' + (res.data?.message || '请检查图片格式和大小'))
    }
  } catch (error: any) {
    console.error('图片上传异常：', error)
    message.error('上传失败：' + (error.response?.data?.message || '请稍后重试'))
  } finally {
    isUploading.value = false
    // 清空文件输入框，允许重复上传同一文件
    if (fileInput.value) {
      fileInput.value.value = ''
    }
  }
}
</script>

<style scoped>
.upload-container {
  padding: 0;
  height: 100%;
}

.upload-box {
  background: var(--card-background);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 60px 40px;
  text-align: center;
  cursor: pointer;
  transition: var(--theme-transition);
  width: 100%;
  min-height: 440px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.upload-box:hover {
  border-color: #3b82f6;
  background: var(--hover-background);
}

.upload-box.uploading {
  pointer-events: none;
}

.upload-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 32px;
  width: 100%;
}

.illustration-wrapper {
  width: 160px;
  height: 160px;
  transition: transform 0.3s ease;
}

.upload-box:hover .illustration-wrapper {
  transform: scale(1.02);
}

.upload-illustration {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.upload-info {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.primary-text {
  font-size: 18px;
  font-weight: 500;
  color: var(--text-primary);
  margin: 0;
}

.support-tips {
  display: flex;
  justify-content: center;
  gap: 12px;
}

.tip-tag {
  color: var(--text-secondary);
  font-size: 12px;
  letter-spacing: 0.05em;
  padding: 2px 8px;
  border: 1px solid var(--border-color);
  border-radius: 4px;
}

/* 上传进度遮罩 */
.uploading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: var(--card-background);
  opacity: 0.95;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  z-index: 20;
  color: #3b82f6;
  gap: 12px;
}

.spinner {
  width: 32px;
  height: 32px;
  border: 2px solid var(--border-color);
  border-top-color: #3b82f6;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 预览态样式 */
.preview-content {
  width: 100%;
  height: 100%;
  position: relative;
  overflow: hidden;
  border-radius: 8px;
}

.preview-image {
  width: 100%;
  height: 100%;
  display: block;
  max-height: 500px;
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
  gap: 8px;
}

.upload-box:hover .preview-mask {
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

/* 响应式调整 */
@media screen and (max-width: 768px) {
  .upload-box {
    padding: 32px 20px;
    min-height: 300px;
  }

  .illustration-wrapper {
    width: 120px;
    height: 120px;
  }

  .primary-text {
    font-size: 16px;
  }
}
</style>
