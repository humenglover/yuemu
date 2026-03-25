<template>
  <div v-if="visible" class="image-out-painting-overlay">
    <div
      class="image-out-painting-modal"
      :class="{ 'mobile-fullscreen': device === DEVICE_TYPE_ENUM.MOBILE }"
      :style="device === DEVICE_TYPE_ENUM.MOBILE ? {
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        width: '100vw',
        height: '100vh',
        maxWidth: '100vw',
        maxHeight: '100vh',
        borderRadius: 0,
        margin: 0
      } : {}"
    >
      <div class="modal-header">
        <div class="header-title">AI 扩图</div>
        <button class="close-button" @click="closeModal">×</button>
      </div>
      <div class="modal-body">
        <div class="out-painting-container">
          <!-- PC端，左右布局 -->
          <template v-if="device === DEVICE_TYPE_ENUM.PC">
            <div class="image-comparison">
              <div class="image-section original">
                <div class="section-title">原始图片</div>
                <div class="image-wrapper">
                  <img :src="picture?.url" :alt="picture?.name" />
                </div>
              </div>
              <div v-if="resultImageUrl" class="image-section result">
                <div class="section-title">扩图结果</div>
                <div class="image-wrapper">
                  <img :src="resultImageUrl" :alt="picture?.name" />
                </div>
              </div>
            </div>
          </template>

          <!-- 移动端，上下布局 -->
          <template v-else>
            <div class="image-comparison mobile">
              <div class="image-section original">
                <div class="section-title">原始图片</div>
                <div class="image-wrapper">
                  <img :src="picture?.url" :alt="picture?.name" />
                </div>
              </div>
              <div v-if="resultImageUrl" class="image-section result">
                <div class="section-title">扩图结果</div>
                <div class="image-wrapper">
                  <img :src="resultImageUrl" :alt="picture?.name" />
                </div>
              </div>
            </div>
          </template>

          <!-- 操作按钮 -->
          <div class="action-buttons">
            <div class="button-group">
              <button
                class="generate-button"
                :class="{ loading: !!taskId }"
                :disabled="!!taskId"
                @click="createTask"
              >
                <ThunderboltOutlined v-if="!taskId" />
                {{ taskId ? '生成中...' : '生成图片' }}
              </button>
              <button
                v-if="resultImageUrl"
                class="apply-button"
                :class="{ loading: uploadLoading }"
                :disabled="uploadLoading"
                @click="handleUpload"
              >
                <CheckOutlined v-if="!uploadLoading" />
                {{ uploadLoading ? '应用中...' : '应用结果' }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted } from 'vue'
import {
  createPictureOutPaintingTaskUsingPost,
  getPictureOutPaintingTaskUsingGet,
  uploadPictureByUrlUsingPost,
} from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'
import { getDeviceType } from '@/utils/device.ts'
import { DEVICE_TYPE_ENUM } from '@/constants/device.ts'
import { ThunderboltOutlined, CheckOutlined } from '@ant-design/icons-vue'
// 定义用于存储设备类型的响应式变量
const device = ref<string>('')
// 页面加载时获取设备类型并获取数据
onMounted(async () => {
  device.value = await getDeviceType()
})
interface Props {
  picture?: API.PictureVO
  spaceId?: number
  onSuccess?: (newPicture: API.PictureVO) => void
}

const props = defineProps<Props>()

const resultImageUrl = ref<string>('')

// 任务 id
const taskId = ref<string>()

/**
 * 创建任务
 */
const createTask = async () => {
  if (!props.picture?.id) {
    return
  }
  const res = await createPictureOutPaintingTaskUsingPost({
    pictureId: props.picture.id,
    // 根据需要设置扩图参数
    parameters: {
      xScale: 2,
      yScale: 2,
    },
  })
  if (res.data.code === 0 && res.data.data) {
    message.success('创建任务成功，请耐心等待，不要退出界面')
    // console.log(res.data.data.output.taskId)
    taskId.value = res.data.data.output.taskId
    // 开启轮询
    startPolling()
  } else {
    message.error('图片任务失败，' + res.data.message)
  }
}

// 轮询定时器
let pollingTimer: NodeJS.Timeout = null

// 开始轮询
const startPolling = () => {
  if (!taskId.value) {
    return
  }

  pollingTimer = setInterval(async () => {
    try {
      const res = await getPictureOutPaintingTaskUsingGet({
        taskId: taskId.value,
      })
      if (res.data.code === 0 && res.data.data) {
        const taskResult = res.data.data.output
        if (taskResult.taskStatus === 'SUCCEEDED') {
          message.success('扩图任务执行成功')
          resultImageUrl.value = taskResult.outputImageUrl
          // 清理轮询
          clearPolling()
        } else if (taskResult.taskStatus === 'FAILED') {
          message.error('扩图任务执行失败')
          // 清理轮询
          clearPolling()
        }
      }
    } catch (error) {
      console.error('扩图任务轮询失败', error)
      message.error('扩图任务轮询失败，' + error.message)
      // 清理轮询
      clearPolling()
    }
  }, 3000) // 每 3 秒轮询一次
}

// 清理轮询
const clearPolling = () => {
  if (pollingTimer) {
    clearInterval(pollingTimer)
    pollingTimer = null
    taskId.value = null
  }
}

// 是否正在上传
const uploadLoading = ref(false)

/**
 * 上传图片
 * @param file
 */
const handleUpload = async () => {
  uploadLoading.value = true
  try {
    const params: API.PictureUploadRequest = {
      fileUrl: resultImageUrl.value,
      spaceId: props.spaceId,
    }
    if (props.picture) {
      params.id = props.picture.id
    }
    const res = await uploadPictureByUrlUsingPost(params)
    if (res.data.code === 0 && res.data.data) {
      message.success('图片上传成功')
      // 将上传成功的图片信息传递给父组件
      props.onSuccess?.(res.data.data)
      // 关闭弹窗
      closeModal()
    } else {
      message.error('图片上传失败，' + res.data.message)
    }
  } catch (error) {
    console.error('图片上传失败', error)
    message.error('图片上传失败，' + error.message)
  }
  uploadLoading.value = false
}

// 是否可见
const visible = ref(false)

// 打开弹窗
const openModal = () => {
  visible.value = true
}

// 关闭弹窗
const closeModal = () => {
  visible.value = false
}

// 暴露函数给父组件
defineExpose({
  openModal,
})
</script>

<style scoped>
.image-out-painting-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  backdrop-filter: blur(4px);
}

.image-out-painting-modal {
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  width: 800px;
  max-width: 90vw;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);

  &.mobile-fullscreen {
    width: 100vw;
    height: 100vh;
    max-width: 100vw;
    max-height: 100vh;
    border-radius: 0;
    margin: 0;
  }
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  border-bottom: 1px solid #f0f0f0;
}

.header-title {
  font-size: 18px;
  font-weight: 600;
  color: #1D2129;
}

.close-button {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #86909C;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.2s;
}

.close-button:hover {
  background-color: #f2f3f5;
  color: #4e5969;
}

.modal-body {
  padding: 24px;
  overflow-y: auto;
  flex: 1;
}

.out-painting-container {
  text-align: center;
}

.image-comparison {
  display: flex;
  gap: 24px;
  margin-bottom: 24px;
  min-height: 400px;

  &.mobile {
    flex-direction: column;
    gap: 16px;
    min-height: auto;
  }
}

.image-section {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.section-title {
  font-size: 15px;
  font-weight: 500;
  color: #64748b;
  margin-bottom: 12px;
}

.image-wrapper {
  background: #f8fafc;
  border-radius: 12px;
  padding: 16px;
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.05);
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;

  img {
    max-width: 100%;
    max-height: 400px;
    border-radius: 8px;
    display: block;
    margin: 0 auto;
    object-fit: contain;
  }
}

.action-buttons {
  margin-top: 24px;
}

.button-group {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.generate-button, .apply-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 36px;
  padding: 0 20px;
  border-radius: 18px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  border: none;
}

.generate-button {
  background: transparent;
  color: #2563eb;
  border: 1px solid #2563eb;
}

.generate-button:hover:not(:disabled) {
  background: rgba(37, 99, 235, 0.1);
  color: #1d4ed8;
  border-color: #1d4ed8;
}

.apply-button {
  background: linear-gradient(135deg, #2563eb 0%, #3b82f6 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.2);
}

.apply-button:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(37, 99, 235, 0.3);
}

.apply-button:active:not(:disabled) {
  transform: translateY(1px);
}

.generate-button:disabled,
.apply-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 响应式调整 */
@media screen and (max-width: 768px) {
  .image-out-painting-modal {
    width: 100vw;
    height: 100vh;
    max-width: 100vw;
    max-height: 100vh;
    border-radius: 0;
    margin: 0;
  }

  .image-out-painting-modal.mobile-fullscreen {
    width: 100vw;
    height: 100vh;
    max-width: 100vw;
    max-height: 100vh;
    border-radius: 0;
    margin: 0;
  }

  .modal-header {
    padding: 12px 16px;
  }

  .modal-body {
    padding: 16px;
  }

  .image-wrapper {
    padding: 12px;

    img {
      max-height: 300px;
    }
  }

  .section-title {
    font-size: 14px;
    margin-bottom: 8px;
  }

  .action-buttons {
    margin-top: 16px;
  }

  .button-group {
    flex-direction: column;
    gap: 8px;
  }

  .generate-button,
  .apply-button {
    height: 32px;
    padding: 0 16px;
    font-size: 13px;
    width: 100%;
  }
}
</style>
