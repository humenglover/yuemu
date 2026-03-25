<template>
  <Transition name="fade">
    <div v-if="visible" class="editor-overlay">
      <div class="editor-card">
        <div class="editor-header">
          <h3 class="editor-title">编辑资源</h3>
          <button class="close-btn" @click="closeModal" aria-label="关闭">
            <i class="fas fa-times"></i>
          </button>
        </div>

        <div class="editor-content">
          <VueCropper
            ref="cropperRef"
            :key="cropperKey"
            :img="cropperOption.img"
            :autoCrop="cropperOption.autoCrop"
            :fixedBox="cropperOption.fixedBox"
            :centerBox="cropperOption.centerBox"
            :canMove="cropperOption.canMove"
            :canMoveBox="cropperOption.canMoveBox"
            :original="cropperOption.original"
            :autoCropArea="cropperOption.autoCropArea"
            :info="cropperOption.info"
            :infoTrue="cropperOption.infoTrue"
            :outputSize="cropperOption.outputSize"
            :outputType="cropperOption.outputType"
            @imgLoad="onImgLoad"
            class="cropper-container"
          />
          <!-- 渲染加载动画遮罩 -->
          <div v-show="isImgLoading" class="rendering-overlay">
            <div class="loading-content">
              <div class="spinner-box">
                <div class="spinner-inner"></div>
              </div>
              <span class="loading-text">正在渲染画质...</span>
            </div>
          </div>
        </div>

        <div class="editor-toolbar">
          <div class="toolbar-inner">
            <button class="tool-btn" @click="toggleFixedRatio" title="锁定/解锁比例">
              <i class="fas" :class="cropperOption.fixedBox ? 'fa-lock' : 'fa-unlock'"></i>
              <span class="tool-text">{{ cropperOption.fixedBox ? '1:1' : '自由' }}</span>
            </button>
            <button class="tool-btn" @click="rotateLeft" title="向左旋转">
              <i class="fas fa-rotate-left"></i>
              <span class="tool-text">左转</span>
            </button>
            <button class="tool-btn" @click="rotateRight" title="向右旋转">
              <i class="fas fa-rotate-right"></i>
              <span class="tool-text">右转</span>
            </button>
            <button class="tool-btn" @click="changeScale(0.2)" title="放大">
              <i class="fas fa-search-plus"></i>
              <span class="tool-text">放大</span>
            </button>
            <button class="tool-btn" @click="changeScale(-0.2)" title="缩小">
              <i class="fas fa-search-minus"></i>
              <span class="tool-text">缩小</span>
            </button>
            <button class="tool-btn" @click="resetCrop" title="重置编辑">
              <i class="fas fa-redo"></i>
              <span class="tool-text">重置</span>
            </button>
            <button class="tool-btn" @click="handleRemoveBg" title="AI抠图" :disabled="aiLoading || blurLoading" :class="{ 'btn-loading': aiLoading }">
              <i class="fas fa-magic" :class="{ 'fa-spin': aiLoading }"></i>
              <span class="tool-text">{{ aiLoading ? '处理中' : 'AI抠图' }}</span>
            </button>
            <button class="tool-btn" @click="handleFaceBlur" title="人脸打码" :disabled="blurLoading || aiLoading" :class="{ 'btn-loading': blurLoading }">
              <i class="fas fa-user-secret" :class="{ 'fa-spin': blurLoading }"></i>
              <span class="tool-text">{{ blurLoading ? '处理中' : '人脸打码' }}</span>
            </button>
            <button class="tool-btn" @click="previewImage" title="预览图片">
              <i class="fas fa-eye"></i>
              <span class="tool-text">预览</span>
            </button>
          </div>
        </div>

        <div class="collab-tip" v-if="isTeamSpace && editingUser">
          <span class="pulse-dot"></span>
          {{ editingUser.userName }} 正在编辑
        </div>

        <div class="editor-footer">
          <button class="cancel-btn" @click="closeModal" :disabled="loading">
            取消
          </button>
          <button
            class="confirm-btn"
            :class="{ 'btn-loading': loading }"
            :disabled="loading || !canEdit"
            @click="handleConfirm"
          >
            <i class="fas fa-spinner fa-spin" v-if="loading"></i>
            <span v-if="!loading">完成</span>
            <span v-if="loading">上传中...</span>
          </button>
        </div>
      </div>

      <div v-if="previewVisible" class="preview-overlay" @click="closePreview">
        <div class="preview-container" @click.stop>
          <img :src="previewUrl" alt="预览图片" class="preview-img" />
          <button class="preview-close-btn" @click="closePreview">
            <i class="fas fa-times"></i>
          </button>
        </div>
      </div>

      <!-- AI 抠图结果弹窗 -->
      <div v-if="removeBgVisible" class="preview-overlay" @click="closeRemoveBg">
        <div class="result-container" @click.stop>
          <div class="result-header">
            <h3 class="result-title">AI 抠图结果</h3>
            <button class="preview-close-btn result-close-btn" @click="closeRemoveBg">
              <i class="fas fa-times"></i>
            </button>
          </div>
          <div class="result-img-wrapper">
            <img :src="removeBgUrl" alt="抠图结果" class="result-img" />
          </div>
          <div class="result-footer">
            <button class="result-cancel-btn" @click="closeRemoveBg">取消</button>
            <button class="result-confirm-btn" @click="confirmRemoveBg">
              <i class="fas fa-check"></i> 使用此图
            </button>
          </div>
        </div>
      </div>

      <link rel="stylesheet" href="https://cdn.bootcdn.net/ajax/libs/font-awesome/6.4.0/css/all.min.css" />
    </div>
  </Transition>
</template>

<script lang="ts" setup>
import { ref, computed, watchEffect, onUnmounted, nextTick, reactive } from 'vue'
import { uploadPictureUsingPost } from '@/api/pictureController'
import { removeBackgroundUsingPost, faceBlurUsingPost } from '@/api/pythonApiController'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import PictureEditWebSocket from '@/utils/pictureEditWebSocket'
import { SPACE_TYPE_ENUM } from '@/constants/space'
import { PICTURE_EDIT_MESSAGE_TYPE_ENUM } from '@/constants/picture'
import 'vue-cropper/dist/index.css'
import { VueCropper } from 'vue-cropper'

interface Props {
  imageUrl?: string
  picture?: API.PictureVO
  spaceId?: number
  space?: API.SpaceVO
  onSuccess?: (newPicture: API.PictureVO) => void
}

const props = defineProps<Props>()
const visible = ref(false)
const loading = ref(false)
const isImgLoading = ref(false)
const cropperKey = ref(0) // 新增：稳定渲染的关键 Key
const cropperRef = ref<InstanceType<typeof VueCropper>>()
const previewVisible = ref(false)
const previewUrl = ref('')
const aiLoading = ref(false)
const blurLoading = ref(false)
const removeBgVisible = ref(false)
const removeBgUrl = ref('')
const removeBgBlob = ref<Blob>()
// 跟踪当前最新图片URL（编辑成功后实时更新，避免重新打开还是旧图）
const currentImageUrl = ref(props.imageUrl || '')

const cropperOption = reactive({
  img: currentImageUrl.value,
  autoCrop: true,
  fixedBox: false,
  centerBox: true,
  canMove: true,
  canMoveBox: true,
  original: false,
  autoCropArea: 1,  // 默认裁剪框覆盖全图（100%）
  info: true,
  infoTrue: true,
  outputSize: 1,
  outputType: 'png' as const
})

const openModal = () => {
  visible.value = true
  isImgLoading.value = true
  nextTick(() => {
    const target = currentImageUrl.value || props.imageUrl || ''
    if (cropperOption.img !== target) {
      cropperOption.img = target
      cropperKey.value++ // 只有图片源变了才触发强制重绘，阻断中间态更新导致的循环
    }
  })
}

const closeModal = () => {
  visible.value = false
  if (websocket) websocket.disconnect()
  editingUser.value = undefined
  loading.value = false
  resetCrop()
}

const toggleFixedRatio = () => {
  cropperOption.fixedBox = !cropperOption.fixedBox
  nextTick(() => {
    cropperRef.value?.refresh()
  })
}

const resetCrop = () => {
  // 重置时使用最新图片（而非原始 imageUrl），保持编辑历史
  const target = currentImageUrl.value || props.imageUrl
  if (target) {
    cropperOption.img = target
    cropperOption.fixedBox = false
    cropperOption.autoCropArea = 1
  }
}

const previewImage = () => {
  if (!cropperRef.value) return
  cropperRef.value.getCropBlob((blob: Blob) => {
    if (!blob) {
      message.error('预览失败')
      return
    }
    previewUrl.value = URL.createObjectURL(blob)
    previewVisible.value = true
  }, 'image/png', 1.0)
}

const closePreview = () => {
  previewVisible.value = false
  URL.revokeObjectURL(previewUrl.value)
  previewUrl.value = ''
}

const handleConfirm = async () => {
  if (!cropperRef.value || !props.imageUrl) return
  loading.value = true
  try {
    await new Promise<void>((resolve, reject) => {
      cropperRef.value?.getCropBlob((blob: Blob) => {
        if (!blob) {
          reject(new Error('图片处理失败'))
          return
        }
        const fileName = (props.picture?.name || '资源') + '.png'
        const file = new File([blob], fileName, { type: 'image/png' })
        handleUpload({ file }).then(resolve).catch(reject)
      }, 'image/png', 1.0)
    })
  } catch (error: any) {
    message.error('图片处理失败：' + (error.message || '未知错误'))
    loading.value = false
  }
}

const handleRemoveBg = async () => {
  if (!cropperRef.value || !props.imageUrl) return
  if (aiLoading.value || blurLoading.value) return

  aiLoading.value = true
  try {
    const blob = await new Promise<Blob>((resolve, reject) => {
      cropperRef.value?.getCropBlob((blob: Blob) => {
        if (!blob) reject(new Error('图片获取失败'))
        else resolve(blob)
      }, 'image/png', 1.0)
    })

    const fileName = (props.picture?.name || 'temp') + '.png'
    const file = new File([blob], fileName, { type: 'image/png' })

    message.loading({ content: 'AI 抠图处理中...', key: 'aiRemoveBg' })

    // 调用AI抠图接口，返回blob
    const res = await removeBackgroundUsingPost({}, file, { responseType: 'blob' })

    // axios 配置可能会将 response 封装，这里兼容直接返回 Blob 和包裹在 data 中的情况
    const responseBlob = res instanceof Blob ? res : (res as any).data instanceof Blob ? (res as any).data : new Blob([res as any], { type: 'image/png' })

    if (responseBlob instanceof Blob) {
      if (responseBlob.type === 'application/json') {
        const text = await responseBlob.text()
        const json = JSON.parse(text)
        throw new Error(json.message || '抠图失败')
      }

      // 准备预览，此时不触发主区域加载动画
      removeBgBlob.value = responseBlob
      removeBgUrl.value = URL.createObjectURL(responseBlob)
      removeBgVisible.value = true
      message.success({ content: 'AI 抠图成功', key: 'aiRemoveBg' })
    } else {
      throw new Error('API 返回格式错误')
    }
  } catch (error: any) {
    message.error({ content: 'AI抠图失败：' + (error.message || '未知错误'), key: 'aiRemoveBg' })
  } finally {
    aiLoading.value = false
  }
}

const handleFaceBlur = async () => {
  if (!cropperRef.value || !props.imageUrl) return
  if (aiLoading.value || blurLoading.value) return

  blurLoading.value = true
  try {
    const blob = await new Promise<Blob>((resolve, reject) => {
      cropperRef.value?.getCropBlob((blob: Blob) => {
        if (!blob) reject(new Error('图片获取失败'))
        else resolve(blob)
      }, 'image/png', 1.0)
    })

    const fileName = (props.picture?.name || 'temp') + '.png'
    const file = new File([blob], fileName, { type: 'image/png' })

    message.loading({ content: 'AI 人脸打码中...', key: 'faceBlur' })

    const res = await faceBlurUsingPost({}, file, { responseType: 'blob' })

    const responseBlob = res instanceof Blob
      ? res
      : (res as any).data instanceof Blob
        ? (res as any).data
        : new Blob([res as any], { type: 'image/png' })

    if (responseBlob instanceof Blob) {
      if (responseBlob.type === 'application/json') {
        const text = await responseBlob.text()
        const json = JSON.parse(text)
        throw new Error(json.message || '打码失败')
      }

      // 性能优化：释放旧的内存引用
      if (cropperOption.img.startsWith('blob:')) {
        URL.revokeObjectURL(cropperOption.img)
      }

      // 启动渲染动画
      isImgLoading.value = true

      // 直接把打码后的图片替换到当前裁剪框中，实现"无缝编辑"
      const faceBlurUrl = URL.createObjectURL(responseBlob)
      cropperOption.img = faceBlurUrl
      currentImageUrl.value = faceBlurUrl
      cropperKey.value++

      nextTick(() => {
        if (cropperRef.value) {
          cropperRef.value.clearCrop()
          cropperOption.autoCropArea = 1
          cropperRef.value.startCrop()
        }
      })
      message.success({ content: '人脸打码成功', key: 'faceBlur' })
    } else {
      throw new Error('API 返回格式错误')
    }
  } catch (error: any) {
    message.error({ content: '人脸打码失败：' + (error.message || '未知错误'), key: 'faceBlur' })
  } finally {
    blurLoading.value = false
  }
}

const closeRemoveBg = () => {
  removeBgVisible.value = false
  if (removeBgUrl.value) {
    URL.revokeObjectURL(removeBgUrl.value)
    removeBgUrl.value = ''
  }
  removeBgBlob.value = undefined
}

const confirmRemoveBg = () => {
  if (!removeBgBlob.value) return

  // 启动主区域渲染动画
  isImgLoading.value = true

  // 将抠出的图片直接写入裁剪组件展示给用户进一步操作
  const newUrl = URL.createObjectURL(removeBgBlob.value)

  // 稳态更新
  cropperOption.img = newUrl
  currentImageUrl.value = newUrl
  cropperKey.value++ // 仅在此必要时刻触发重构

  removeBgVisible.value = false

  nextTick(() => {
    if (cropperRef.value) {
      cropperRef.value.clearCrop()
      cropperOption.autoCropArea = 1
      cropperRef.value.startCrop() // 修正：API 为 startCrop()
    }
  })
}

const handleUpload = async ({ file }: { file: File }) => {
  try {
    const params: API.PictureUploadRequest = props.picture ? { id: props.picture.id } : {}
    params.spaceId = props.spaceId
    const res = await uploadPictureUsingPost(params, {}, file)
    if (res.data?.data) {
      // 更新本地记录的最新图片 URL，保证下次打开编辑器时展示的是最新编辑后版本
      const newUrl = res.data.data.url || res.data.data.thumbnailUrl
      if (newUrl) {
        currentImageUrl.value = newUrl
      }
      props.onSuccess?.(res.data.data)
      closeModal()
      message.success('资源编辑成功')
    } else {
      message.error('资源上传失败：' + (res.data?.message || '未知错误'))
    }
  } catch (error: any) {
    message.error('资源上传失败：' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

const loginUserStore = useLoginUserStore()
const loginUser = loginUserStore.loginUser
const editingUser = ref<API.UserVO>()
const isTeamSpace = computed(() => props.space?.spaceType === SPACE_TYPE_ENUM.TEAM)
const canEnterEdit = computed(() => !editingUser.value)
const canExitEdit = computed(() => editingUser.value?.id === loginUser.id)
const canEdit = computed(() => !isTeamSpace.value || editingUser.value?.id === loginUser.id)

let websocket: PictureEditWebSocket | null = null
const initWebsocket = () => {
  const pictureId = props.picture?.id
  if (!pictureId || !visible.value) return
  if (websocket) websocket.disconnect()
  websocket = new PictureEditWebSocket(pictureId)
  websocket.connect()
  websocket.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.INFO, (msg) => message.info(msg.message))
  websocket.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.ERROR, (msg) => message.error(msg.message))
  websocket.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.ENTER_EDIT, (msg) => {
    message.info(msg.message)
    editingUser.value = msg.user
  })
  websocket.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.EXIT_EDIT, (msg) => {
    message.info(msg.message)
    editingUser.value = undefined
  })
}

watchEffect(() => {
  if (isTeamSpace.value && visible.value) initWebsocket()
})

const onImgLoad = (status: string) => {
  if (status === 'success') {
    // 阻断循环：仅关闭动画，不在此处触发 refresh() 导致递归
    isImgLoading.value = false
  } else {
    isImgLoading.value = false
    message.error('图片加载失败')
  }
}

const rotateLeft = () => cropperRef.value?.rotateLeft()
const rotateRight = () => cropperRef.value?.rotateRight()
const changeScale = (num: number) => cropperRef.value?.changeScale(num)
const enterEdit = () => websocket?.sendEnterEdit()
const exitEdit = () => websocket?.sendExitEdit()

onUnmounted(() => {
  if (websocket) websocket.disconnect()
  editingUser.value = undefined
  closePreview()
  closeRemoveBg()
})

defineExpose({ openModal })
</script>

<style lang="scss" scoped>
.editor-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: var(--comment-drawer-backdrop);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
  backdrop-filter: blur(4px);
  transition: var(--theme-transition);
}

.editor-card {
  width: 90%;
  max-width: 900px;
  background: var(--card-background);
  border-radius: 12px;
  box-shadow: 0 4px 20px var(--shadow-color);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  transition: var(--theme-transition);
}

.editor-header {
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: var(--theme-transition);

  .editor-title {
    font-size: 16px;
    font-weight: 600;
    color: var(--text-primary);
    margin: 0;
  }

  .close-btn {
    width: 28px;
    height: 28px;
    border-radius: 50%;
    background: transparent;
    border: none;
    color: var(--text-secondary);
    display: flex;
    justify-content: center;
    align-items: center;
    cursor: pointer;
    transition: background 0.2s, var(--theme-transition);

    &:hover {
      background: var(--hover-background);
    }

    i {
      font-size: 14px;
    }
  }
}

.editor-content {
  height: 500px;
  padding: 20px;
  background: var(--background);
  display: flex;
  justify-content: center;
  align-items: center;
  transition: var(--theme-transition);
  position: relative; // 必须相对定位

  .cropper-container {
    width: 100%;
    height: 100% !important;
    border-radius: 8px;
    overflow: hidden;
    background: var(--background);
    border: 1px solid var(--border-color);
  }

  /* 渲染遮罩样式 */
  .rendering-overlay {
    position: absolute;
    top: 20px;
    left: 20px;
    right: 20px;
    bottom: 20px;
    background: rgba(var(--background-rgb, 255, 255, 255), 0.85);
    backdrop-filter: blur(8px);
    z-index: 10;
    display: flex;
    justify-content: center;
    align-items: center;
    border-radius: 8px;

    .loading-content {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 16px;

      .spinner-box {
        width: 48px;
        height: 48px;
        border: 3px solid var(--border-color);
        border-radius: 50%;
        position: relative;

        .spinner-inner {
          width: 100%;
          height: 100%;
          border: 3px solid transparent;
          border-top-color: var(--link-color);
          border-radius: 50%;
          animation: spin 0.8s linear infinite;
        }
      }

      .loading-text {
        font-size: 14px;
        color: var(--text-secondary);
        font-weight: 500;
        letter-spacing: 1px;
      }
    }
  }
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.editor-toolbar {
  padding: 12px 20px;
  border-bottom: 1px solid var(--border-color);
  background: var(--card-background);
  transition: var(--theme-transition);
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;

  /* 隐藏滚动条 */
  &::-webkit-scrollbar {
    display: none;
  }
  -ms-overflow-style: none; /* IE and Edge */
  scrollbar-width: none; /* Firefox */

  .toolbar-inner {
    display: flex;
    justify-content: space-around;
    align-items: center;
    min-width: 500px; /* 保证这几个按钮不会挤压得太小 */
  }

  .tool-btn {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    width: 60px;
    background: transparent;
    border: none;
    color: var(--text-primary);
    cursor: pointer;
    flex-shrink: 0;
    transition: all 0.2s, var(--theme-transition);

    &.btn-loading {
      opacity: 0.7;
      cursor: not-allowed;
    }

    &:hover:not(.btn-loading) {
      color: var(--link-color);
      transform: translateY(-2px);
    }

    i {
      font-size: 20px;
      margin-bottom: 4px;
    }

    .tool-text {
      font-size: 12px;
      color: var(--text-secondary);
    }
  }
}

.collab-tip {
  padding: 8px 16px;
  background: var(--hover-background);
  color: var(--text-secondary);
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: var(--theme-transition);

  .pulse-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: var(--like-button-active-color);
    animation: pulse 1.5s infinite;
  }
}

.editor-footer {
  padding: 16px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: var(--theme-transition);

  .cancel-btn {
    width: 48%;
    height: 44px;
    background: var(--hover-background);
    color: var(--text-primary);
    border: none;
    border-radius: 8px;
    font-size: 16px;
    font-weight: 500;
    cursor: pointer;
    transition: background 0.2s, var(--theme-transition);

    &:hover {
      background: var(--border-color);
    }

    &:disabled {
      opacity: 0.7;
      cursor: not-allowed;
    }
  }

  .confirm-btn {
    width: 48%;
    height: 44px;
    background: var(--link-color);
    color: var(--text-other);
    border: none;
    border-radius: 8px;
    font-size: 16px;
    font-weight: 500;
    cursor: pointer;
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 8px;
    transition: background 0.2s, box-shadow 0.2s, var(--theme-transition);

    &:hover:not(:disabled) {
      background: var(--link-hover-color);
      box-shadow: 0 4px 12px rgba(59, 130, 246, 0.2);
    }

    &:disabled {
      background: var(--link-color);
      opacity: 0.8;
      cursor: not-allowed;
    }

    &.btn-loading {
      opacity: 0.9;
    }

    i {
      font-size: 16px;
    }
  }
}

:deep(.vue-cropper) {
  .cropper-view-box {
    outline: 2px solid var(--link-color);
    box-shadow: 0 0 0 9999px rgba(0, 0, 0, 0.3);
    border-radius: 8px;
    transition: var(--theme-transition);
  }

  .cropper-point {
    width: 14px;
    height: 14px;
    background: var(--text-other);
    border: 2px solid var(--link-color);
    border-radius: 50%;
    opacity: 1;
    box-shadow: 0 2px 4px var(--shadow-color);
    transition: var(--theme-transition);
  }

  .cropper-line {
    background: var(--link-color);
    opacity: 0.5;
    transition: var(--theme-transition);
  }

  .crop-info {
    background: var(--card-background);
    color: var(--text-primary);
    padding: 6px 12px;
    border-radius: 16px;
    font-size: 12px;
    box-shadow: 0 2px 4px var(--shadow-color);
    transition: var(--theme-transition);
  }
}

.preview-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(0, 0, 0, 0.8);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 10000;
  transition: var(--theme-transition);
}

.preview-container {
  position: relative;
  max-width: 90vw;
  max-height: 90vh;
  border-radius: 8px;
  overflow: hidden;
}

.preview-img {
  max-width: 100%;
  max-height: 90vh;
  object-fit: contain;
}

.preview-close-btn {
  position: absolute;
  top: 16px;
  right: 16px;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.5);
  border: none;
  color: white;
  display: flex;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  transition: background 0.2s;

  &:hover {
    background: rgba(0, 0, 0, 0.8);
  }

  i {
    font-size: 18px;
  }
}

.result-container {
  position: relative;
  width: 90vw;
  max-width: 500px;
  background: var(--card-background);
  border-radius: 12px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  transition: var(--theme-transition);
}

.result-header {
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  justify-content: space-between;
  align-items: center;

  .result-title {
    font-size: 16px;
    font-weight: 600;
    color: var(--text-primary);
    margin: 0;
  }

  .result-close-btn {
    position: static;
    border-radius: 50%;
    background: transparent;
    color: var(--text-secondary);

    &:hover {
      background: var(--hover-background);
    }
  }
}

.result-img-wrapper {
  padding: 20px;
  display: flex;
  justify-content: center;
  align-items: center;
  background: var(--background);
}

.result-img {
  max-width: 100%;
  max-height: 50vh;
  object-fit: contain;
  /* 透明度网格背景 */
  background: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20"><rect width="10" height="10" fill="%23ccc"/><rect x="10" width="10" height="10" fill="%23fff"/><rect y="10" width="10" height="10" fill="%23fff"/><rect x="10" y="10" width="10" height="10" fill="%23ccc"/></svg>');
}

.result-footer {
  padding: 16px 20px;
  border-top: 1px solid var(--border-color);
  display: flex;
  justify-content: flex-end;
  gap: 12px;

  .result-cancel-btn, .result-confirm-btn {
    padding: 8px 16px;
    border-radius: 6px;
    font-size: 14px;
    cursor: pointer;
    font-weight: 500;
    transition: all 0.2s;
  }

  .result-cancel-btn {
    background: transparent;
    border: 1px solid var(--border-color);
    color: var(--text-primary);

    &:hover {
      background: var(--hover-background);
    }
  }

  .result-confirm-btn {
    background: var(--link-color);
    border: none;
    color: var(--text-other);
    display: flex;
    align-items: center;
    gap: 6px;

    &:hover {
      background: var(--link-hover-color);
    }
  }
}

@keyframes pulse {
  0% { transform: scale(0.9); opacity: 0.7; }
  50% { transform: scale(1); opacity: 1; }
  100% { transform: scale(0.9); opacity: 0.7; }
}

@media (max-width: 768px) {
  .editor-overlay {
    padding: 0;
    backdrop-filter: none;
  }

  .editor-card {
    width: 100%;
    height: 100vh;
    max-height: 100vh;
    max-width: none;
    border-radius: 0;
  }

  .editor-content {
    flex: 1;
    height: auto;
    padding: 10px;
  }

  .editor-toolbar {
    padding: 10px 8px;

    .toolbar-inner {
      min-width: 580px;
    }

    .tool-btn {
      width: 60px;
      min-width: 60px;

      i {
        font-size: 18px;
      }

      .tool-text {
        font-size: 11px;
        white-space: nowrap;
      }
    }
  }

  .editor-footer {
    padding: 12px 16px;

    .cancel-btn,
    .confirm-btn {
      height: 44px;
      font-size: 16px;
    }
  }

  /* 移动端缩放动画调整 */
  .fade-enter-from,
  .fade-leave-to {
    .editor-card {
      transform: translateY(100%);
      opacity: 1;
    }
  }
}

/* 过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: all 0.3s ease;

  .editor-card {
    transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  }
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;

  .editor-card {
    opacity: 0;
    transform: scale(0.9) translateY(20px);
  }
}
</style>
