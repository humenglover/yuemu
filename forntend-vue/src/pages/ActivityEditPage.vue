<template>
  <div class="box">
    <div class="bg-mask"></div>
    <div class="activity-edit-container">
      <header class="edit-header">
        <button class="back-btn" @click="handleCancel">
          <i class="fas fa-arrow-left"></i>
        </button>
        <h1 class="edit-title">{{ isEdit ? '编辑活动' : '创建活动' }}</h1>
        <button class="tip-btn" @click="showTipModal = true">
          <i class="fas fa-question-circle"></i>
        </button>
      </header>

      <main class="edit-form">
        <div class="form-item">
          <label class="form-label">活动标题</label>
          <div class="input-wrap">
            <input
              v-model="formState.title"
              class="form-input"
              placeholder="请输入活动标题"
              maxlength="100"
            >
            <span class="char-count">{{ formState.title.length }}/100</span>
          </div>
        </div>

        <div class="form-item">
          <label class="form-label">封面图</label>
          <div class="cover-upload">
            <div
              class="upload-area"
              @click="!uploading && triggerCoverUpload()"
              :class="{ loading: uploading }"
            >
              <!-- 上传中遮罩 -->
              <div v-if="uploading" class="upload-mask">
                <i class="fas fa-spinner fa-spin"></i>
                <span>上传中...</span>
              </div>

              <div v-if="formState.coverUrl" class="cover-preview">
                <img :src="formState.coverUrl" alt="封面图" class="cover-img">
                <div class="cover-mask">
                  <i class="fas fa-camera"></i>
                </div>
              </div>
              <div v-else-if="!uploading" class="upload-placeholder">
                <i class="fas fa-plus"></i>
              </div>
            </div>
            <input
              type="file"
              ref="coverFileInput"
              accept="image/*"
              @change="handleCoverUpload"
              class="file-input"
            >
          </div>
        </div>

        <div class="form-item">
          <label class="form-label">过期日期</label>
          <input
            v-model="formState.expireTimeText"
            class="form-input date-input"
            placeholder="请选择活动过期日期"
            @click="showDatePicker = true"
            readonly
          >
        </div>

        <div class="form-item editor-item">
          <label class="form-label">活动内容</label>
          <div class="editor-wrapper">
            <div class="editor-toolbar">
              <Toolbar
                :editor="editorRef"
                :defaultConfig="toolbarConfig"
                :mode="mode"
              />
            </div>
            <div class="editor-content">
              <Editor
                v-model="formState.content"
                :defaultConfig="editorConfig"
                :mode="mode"
                @onCreated="handleCreated"
              />
            </div>
          </div>
        </div>

        <div v-if="uploading" class="upload-progress">
          <div class="progress-bar">
            <div class="progress-fill" :style="{ width: uploadProgress + '%' }"></div>
          </div>
          <p class="progress-text">{{ uploadProgress >= 100 ? '处理中...' : `上传中 ${uploadProgress}%` }}</p>
        </div>
      </main>

      <footer class="edit-footer">
        <button class="btn cancel-btn" @click="handleCancel">取消</button>
        <button
          class="btn submit-btn"
          :class="{ disabled: !formState.title || !formState.coverUrl || !formState.expireTime }"
          @click="handleSubmit"
          :disabled="!formState.title || !formState.coverUrl || !formState.expireTime"
        >
          <span v-if="!submitting">{{ isEdit ? '保存' : '发布' }}</span>
          <i class="fas fa-spinner fa-spin" v-if="submitting"></i>
        </button>
      </footer>
    </div>

    <!-- 日期选择器弹窗 - 仅日期选择 -->
    <div v-if="showDatePicker" class="modal-overlay" @click="showDatePicker = false">
      <div class="modal date-modal" @click.stop>
        <div class="modal-header">
          <h3>选择过期日期</h3>
          <button class="modal-close" @click="showDatePicker = false">
            <i class="fas fa-times"></i>
          </button>
        </div>
        <div class="modal-body">
          <input
            type="date"
            class="datetime-input"
            :min="minDate"
            v-model="formState.expireDate"
            @change="handleDateChange"
          >
        </div>
        <div class="modal-footer">
          <button class="modal-btn cancel" @click="showDatePicker = false">取消</button>
          <button class="modal-btn confirm" @click="confirmDate">确认</button>
        </div>
      </div>
    </div>

    <div v-if="showTipModal" class="modal-overlay" @click="showTipModal = false">
      <div class="modal tip-modal" @click.stop>
        <div class="modal-header">
          <h3>发布提示</h3>
          <button class="modal-close" @click="showTipModal = false">
            <i class="fas fa-times"></i>
          </button>
        </div>
        <div class="modal-body">
          <ul class="tip-list">
            <li><i class="fas fa-check-circle"></i> 标题不超过100字</li>
            <li><i class="fas fa-check-circle"></i> 内容至少10个字符</li>
            <li><i class="fas fa-check-circle"></i> 支持拖拽上传图片</li>
            <li><i class="fas fa-check-circle"></i> 过期日期为必填项</li>
          </ul>
        </div>
      </div>
    </div>

    <div v-if="showConfirmModal" class="modal-overlay" @click="showConfirmModal = false">
      <div class="modal confirm-modal" @click.stop>
        <div class="modal-body confirm-body">
          <i class="fas fa-exclamation-triangle"></i>
          <h3>确定要取消吗？</h3>
          <p>已编辑的内容将不会保存</p>
        </div>
        <div class="modal-footer">
          <button class="modal-btn cancel" @click="showConfirmModal = false">取消</button>
          <button class="modal-btn confirm" @click="confirmCancel">确定</button>
        </div>
      </div>
    </div>

    <div class="toast" :class="{ show: showToast }">
      <i class="fas" :class="toastType === 'success' ? 'fa-check-circle' : 'fa-exclamation-circle'"></i>
      <span>{{ toastMessage }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, shallowRef, onBeforeUnmount, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import '@wangeditor/editor/dist/css/style.css'
import dayjs from 'dayjs'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { uploadPostImageUsingPost } from '@/api/pictureController'
import { addActivityUsingPost, getActivityByIdUsingGet } from '@/api/activityController'

interface FormState {
  id?: string;
  title: string;
  coverUrl: string;
  expireTime: dayjs.Dayjs | null;
  expireTimeText: string;
  expireDate: string; // 改为仅存储日期字符串
  content: string;
  spaceId?: number;
}

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()
const editorRef = shallowRef()
const mode = 'default'
const submitting = ref(false)
const uploading = ref(false)
const uploadProgress = ref(0)
const coverFileInput = ref<HTMLInputElement>()
const showDatePicker = ref(false)
const showTipModal = ref(false)
const showConfirmModal = ref(false)
const showToast = ref(false)
const toastMessage = ref('')
const toastType = ref<'success' | 'error'>('success')

const isEdit = computed(() => !!route.params.id)

// 最小日期改为仅日期格式
const minDate = computed(() => {
  return dayjs().format('YYYY-MM-DD')
})

const formState = ref<FormState>({
  title: '',
  content: '',
  coverUrl: '',
  expireTime: null,
  expireTimeText: '',
  expireDate: '', // 初始化日期字段
})

const triggerCoverUpload = () => {
  coverFileInput.value?.click()
}

const handleCoverUpload = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  if (!file.type.startsWith('image/')) {
    showToastMessage('只能上传图片文件', 'error')
    return
  }

  if (file.size / 1024 / 1024 > 2) {
    showToastMessage('图片必须小于2MB', 'error')
    return
  }

  try {
    uploading.value = true
    uploadProgress.value = 0

    const compressedFile = await compressImage(file)
    uploadProgress.value = 30

    const res = await uploadPostImageUsingPost({}, {}, compressedFile)
    uploadProgress.value = 90

    if (res.data.code === 0 && res.data.data) {
      formState.value.coverUrl = res.data.data.url
      showToastMessage('封面上传成功', 'success')
    } else {
      throw new Error('上传失败')
    }
  } catch (error: any) {
    showToastMessage('封面上传失败: ' + error.message, 'error')
  } finally {
    uploading.value = false
    uploadProgress.value = 0
    if (target) target.value = ''
  }
}

// 修改日期变更处理逻辑 - 仅处理日期
const handleDateChange = () => {
  if (formState.value.expireDate) {
    // 解析日期，默认时间为当天23:59:59
    formState.value.expireTime = dayjs(formState.value.expireDate).endOf('day')
    formState.value.expireTimeText = formState.value.expireTime.format('YYYY-MM-DD')
  }
}

const confirmDate = () => {
  if (formState.value.expireTime) {
    showDatePicker.value = false
  } else {
    showToastMessage('请选择有效的日期', 'error')
  }
}

const getActivityDetail = async () => {
  const id = route.params.id as string
  if (!id) return

  try {
    const res = await getActivityByIdUsingGet({ id })
    if (res.data?.data) {
      const activity = res.data.data
      formState.value = {
        id: activity.id,
        title: activity.title,
        content: activity.content,
        coverUrl: activity.coverUrl,
        expireTime: dayjs(activity.expireTime),
        expireTimeText: dayjs(activity.expireTime).format('YYYY-MM-DD'),
        expireDate: dayjs(activity.expireTime).format('YYYY-MM-DD'), // 提取日期部分
      }
    }
  } catch (error) {
    showToastMessage('获取活动详情失败', 'error')
    router.back()
  }
}

const compressImage = (file: File): Promise<File> => {
  return new Promise((resolve, reject) => {
    if (file.size / 1024 / 1024 < 1) {
      const reader = new FileReader()
      reader.readAsDataURL(file)
      reader.onload = (e) => {
        const img = new Image()
        img.src = e.target?.result as string
        img.onload = () => {
          const canvas = document.createElement('canvas')
          canvas.width = img.width
          canvas.height = img.height
          const ctx = canvas.getContext('2d')
          ctx?.drawImage(img, 0, 0)
          canvas.toBlob(
            (blob) => {
              if (!blob) {
                reject(new Error('图片转换失败'))
                return
              }
              const webpFile = new File([blob], file.name.replace(/\.[^/.]+$/, '.webp'), {
                type: 'image/webp',
                lastModified: Date.now(),
              })
              resolve(webpFile)
            },
            'image/webp',
            1.0
          )
        }
      }
      return
    }

    const maxWidth = 1280
    const maxHeight = 720
    const quality = 0.75

    const reader = new FileReader()
    reader.readAsDataURL(file)
    reader.onload = (e) => {
      const img = new Image()
      img.src = e.target?.result as string
      img.onload = () => {
        let width = img.width
        let height = img.height

        if (width > maxWidth || height > maxHeight) {
          const ratio = Math.min(maxWidth / width, maxHeight / height)
          width *= ratio
          height *= ratio
        }

        const canvas = document.createElement('canvas')
        canvas.width = width
        canvas.height = height
        const ctx = canvas.getContext('2d')
        ctx?.drawImage(img, 0, 0, width, height)

        canvas.toBlob(
          (blob) => {
            if (!blob) {
              reject(new Error('图片压缩失败'))
              return
            }
            const webpFile = new File([blob], file.name.replace(/\.[^/.]+$/, '.webp'), {
              type: 'image/webp',
              lastModified: Date.now(),
            })
            resolve(webpFile)
          },
          'image/webp',
          quality
        )
      }
    }
  })
}

const editorConfig = {
  placeholder: '请输入活动内容...',
  html: true,
  autoFocus: true,
  readOnly: false,
  scroll: true,
  maxLength: 50000,
  MENU_CONF: {
    uploadImage: {
      async customUpload(file: File, insertFn: any) {
        try {
          uploading.value = true
          uploadProgress.value = 0

          const compressedFile = await compressImage(file)
          uploadProgress.value = 30

          const res = await uploadPostImageUsingPost({}, {}, compressedFile)
          uploadProgress.value = 90

          if (res.data.code === 0 && res.data.data) {
            if (editorRef.value) {
              insertFn(res.data.data.url, file.name, res.data.data.url)
              setTimeout(() => {
                const editor = editorRef.value
                if (editor) {
                  editor.blur()
                  editor.focus()
                }
              }, 50)
            }
          } else {
            showToastMessage('图片上传失败', 'error')
          }
        } catch (error: any) {
          showToastMessage('图片上传失败: ' + error.message, 'error')
        } finally {
          uploading.value = false
          uploadProgress.value = 0
        }
      },
    }
  },
  EXTEND_CONF: {
    image: {
      draggable: true,
      resizable: true,
      customConfig: {
        allowDrag: true
      }
    }
  },
  hoverbarKeys: {
    image: {
      menuKeys: ['deleteImage', 'imageWidth', 'imageHeight']
    }
  }
}

const toolbarConfig = {
  excludeKeys: [
    'group-video',
    'insertTable',
    'imageSize'
  ]
}

const handleCreated = (editor: any) => {
  editorRef.value = editor
  setTimeout(() => {
    editor.focus()
  }, 100)
  if (isEdit.value) {
    getActivityDetail()
  }
}

const handleSubmit = async () => {
  try {
    if (!formState.value.title) {
      showToastMessage('请输入活动标题', 'error')
      return
    }
    if (!formState.value.coverUrl) {
      showToastMessage('请上传封面图', 'error')
      return
    }
    if (!formState.value.expireTime) {
      showToastMessage('请选择过期日期', 'error')
      return
    }
    if (!formState.value.content.trim()) {
      showToastMessage('请输入活动内容', 'error')
      return
    }
    if (formState.value.content.length < 10) {
      showToastMessage('内容至少10个字符', 'error')
      return
    }

    submitting.value = true

    let htmlContent = editorRef.value?.getHtml() || ''
    const requestBody = {
      title: formState.value.title,
      content: htmlContent,
      coverUrl: formState.value.coverUrl,
      // 提交时格式化为包含时间的完整格式（当天23:59:59）
      expireTime: formState.value.expireTime.format('YYYY-MM-DDTHH:mm:ss.SSSZ'),
      spaceId: route.query.spaceId ? String(route.query.spaceId) : undefined
    }

    if (isEdit.value) {
      showToastMessage('编辑功能暂未开放', 'error')
      submitting.value = false
      return
    }

    const res = await addActivityUsingPost(requestBody)

    if (res.data?.data) {
      showToastMessage('发布成功', 'success')
      setTimeout(() => {
        router.push(`/activity/detail/${res.data.data}`)
      }, 1500)
    }
  } catch (error: any) {
    showToastMessage('发布失败: ' + error.message, 'error')
  } finally {
    submitting.value = false
  }
}

const handleCancel = () => {
  if (formState.value.title || formState.value.content || formState.value.coverUrl || formState.value.expireTime) {
    showConfirmModal.value = true
  } else {
    router.back()
  }
}

const confirmCancel = () => {
  showConfirmModal.value = false
  router.back()
}

const showToastMessage = (message: string, type: 'success' | 'error') => {
  toastMessage.value = message
  toastType.value = type
  showToast.value = true
  setTimeout(() => {
    showToast.value = false
  }, 3000)
}

watch(() => route.params.id, (newVal) => {
  if (newVal && isEdit.value) {
    getActivityDetail()
  }
})

onMounted(() => {
  if (isEdit.value) {
    getActivityDetail()
  }
})

onBeforeUnmount(() => {
  const editor = editorRef.value
  if (editor) editor.destroy()
})
</script>

<style scoped>
.box {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: var(--background);
  z-index: 100;
  overflow: hidden;
}

.bg-mask {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0,0,0,0.02);
  z-index: -1;
}

.activity-edit-container {
  width: 100%;
  max-width: 1200px;
  height: 92vh;
  background-color: var(--card-background);
  border-radius: 12px;
  box-shadow: 0 0 30px var(--shadow-color);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.edit-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background-color: var(--header-background);
  border-bottom: 1px solid var(--border-color);
  height: 50px;
  box-sizing: border-box;
}

.back-btn, .tip-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background-color: var(--hover-background);
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-primary);
  cursor: pointer;
  transition: var(--theme-transition);
}

.edit-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--header-text);
  margin: 0;
}

.edit-form {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
}

.form-item {
  margin-bottom: 12px;
}

.form-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.input-wrap {
  position: relative;
}

.form-input {
  width: 100%;
  height: 40px;
  padding: 0 12px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background-color: var(--card-background);
  color: var(--text-primary);
  font-size: 14px;
  transition: var(--theme-transition);
  box-sizing: border-box;
}

.form-input:focus {
  outline: none;
  border-color: var(--nav-item-active-text);
  box-shadow: 0 0 0 2px var(--nav-item-active);
}

.form-input::placeholder {
  color: var(--text-secondary);
}

.date-input {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='14' height='14' fill='%23666666' viewBox='0 0 16 16'%3E%3Cpath d='M8 3.5a.5.5 0 0 0-1 0V9a.5.5 0 0 0 .252.434l3.5 2a.5.5 0 0 0 .496-.868L8 8.71V3.5z'/%3E%3Cpath d='M8 16A8 8 0 1 0 8 0a8 8 0 0 0 0 16zm7-8A7 7 0 1 1 1 8a7 7 0 0 1 14 0z'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 12px center;
  background-size: 14px;
  cursor: pointer;
}

.char-count {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 12px;
  color: var(--text-secondary);
}

.cover-upload {
  width: 100%;
}

.upload-area {
  width: 100%;
  height: 120px;
  border: 1px dashed var(--border-color);
  border-radius: 8px;
  background-color: var(--hover-background);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: var(--theme-transition);
  overflow: hidden;
  position: relative;
}

.upload-mask {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(255, 255, 255, 0.8);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  z-index: 10;
  color: #1677ff;
  gap: 8px;
}

.upload-mask i {
  font-size: 24px;
}

.upload-area.loading {
  cursor: not-allowed;
  border-color: #1677ff;
}

.upload-area:hover {
  background-color: var(--nav-item-hover);
}

.cover-preview {
  width: 100%;
  height: 100%;
  position: relative;
}

.cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-mask {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-other);
  opacity: 0;
  transition: var(--theme-transition);
}

.cover-preview:hover .cover-mask {
  opacity: 1;
}

.cover-mask i {
  font-size: 20px;
}

.upload-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-secondary);
}

.upload-placeholder i {
  font-size: 24px;
}

.file-input {
  display: none;
}

.editor-item {
  margin-bottom: 8px;
}

.editor-wrapper {
  width: 100%;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  overflow: hidden;
  background-color: var(--card-background);
}

.editor-toolbar {
  border-bottom: 1px solid var(--border-color);
}

.editor-content {
  min-height: 150px;
}

:deep(.w-e-toolbar) {
  background-color: var(--card-background) !important;
  border-bottom: 1px solid var(--border-color) !important;
  padding: 4px !important;
}

:deep(.w-e-menu) {
  color: var(--text-primary) !important;
  background-color: var(--card-background) !important;
  font-size: 12px !important;
}

:deep(.w-e-menu:hover) {
  background-color: var(--hover-background) !important;
}

:deep(.w-e-text-container) {
  background-color: var(--card-background) !important;
  color: var(--text-primary) !important;
  min-height: 150px !important;
}

:deep(.w-e-text-container [data-slate-editor]) {
  padding: 12px !important;
  font-size: 14px;
  line-height: 1.5;
}

.upload-progress {
  padding: 8px 12px;
  border-radius: 8px;
  background-color: var(--hover-background);
  margin-bottom: 8px;
}

.progress-bar {
  width: 100%;
  height: 4px;
  background-color: var(--border-color);
  border-radius: 2px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background-color: var(--nav-item-active-text);
  border-radius: 2px;
  transition: width 0.3s ease;
}

.progress-text {
  margin: 4px 0 0 0;
  font-size: 12px;
  color: var(--text-secondary);
  text-align: center;
}

.edit-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-top: 1px solid var(--border-color);
  background-color: var(--header-background);
  height: 60px;
  box-sizing: border-box;
}

.btn {
  padding: 0 20px;
  height: 36px;
  border-radius: 18px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: var(--theme-transition);
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cancel-btn {
  background-color: var(--hover-background);
  color: var(--text-primary);
}

.cancel-btn:hover {
  background-color: var(--nav-item-hover);
}

.submit-btn {
  background-color: var(--nav-item-active-text);
  color: var(--text-other);
}

.submit-btn.disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.submit-btn:not(.disabled):hover {
  opacity: 0.9;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background-color: var(--comment-drawer-backdrop);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1001;
}

.modal {
  width: 90%;
  max-width: 360px;
  background-color: var(--card-background);
  border-radius: 12px;
  box-shadow: 0 0 20px var(--shadow-color);
  overflow: hidden;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-color);
}

.modal-header h3 {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.modal-close {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background-color: var(--hover-background);
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-secondary);
  cursor: pointer;
  transition: var(--theme-transition);
}

.modal-body {
  padding: 16px;
}

.datetime-input {
  width: 100%;
  height: 36px;
  padding: 0 12px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background-color: var(--card-background);
  color: var(--text-primary);
  font-size: 14px;
}

.modal-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  padding: 8px 16px;
  border-top: 1px solid var(--border-color);
}

.modal-btn {
  padding: 0 16px;
  height: 32px;
  border-radius: 16px;
  font-size: 13px;
  cursor: pointer;
  transition: var(--theme-transition);
  border: none;
}

.modal-btn.cancel {
  background-color: var(--hover-background);
  color: var(--text-primary);
}

.modal-btn.confirm {
  background-color: var(--nav-item-active-text);
  color: var(--text-other);
}

.tip-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.tip-list li {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  font-size: 13px;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.tip-list li:last-child {
  margin-bottom: 0;
}

.tip-list li i {
  color: var(--nav-item-active-text);
  margin-top: 2px;
  font-size: 12px;
}

.confirm-body {
  text-align: center;
  padding: 20px 16px;
}

.confirm-body i {
  font-size: 36px;
  color: #faad14;
  margin-bottom: 10px;
  display: block;
}

.confirm-body h3 {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 6px 0;
}

.confirm-body p {
  font-size: 13px;
  color: var(--text-secondary);
  margin: 0;
}

.toast {
  position: fixed;
  top: 24px;
  left: 50%;
  transform: translateX(-50%) translateY(-100px);
  padding: 10px 16px;
  background-color: var(--card-background);
  border-radius: 8px;
  box-shadow: 0 4px 12px var(--shadow-color);
  display: flex;
  align-items: center;
  gap: 6px;
  z-index: 1002;
  opacity: 0;
  transition: var(--theme-transition);
}

.toast.show {
  transform: translateX(-50%) translateY(0);
  opacity: 1;
}

.toast i {
  font-size: 14px;
}

.toast i.fa-check-circle {
  color: #52c41a;
}

.toast i.fa-exclamation-circle {
  color: #ff4d4f;
}

.toast span {
  font-size: 13px;
  color: var(--text-primary);
}

@media screen and (max-width: 768px) {
  .activity-edit-container {
    max-width: 100%;
    height: 100vh;
    max-height: none;
    border-radius: 0;
  }

  .edit-form {
    padding: 12px;
  }

  .upload-area {
    height: 100px;
  }

  .editor-content {
    min-height: 120px;
  }

  :deep(.w-e-text-container) {
    min-height: 120px !important;
  }

  /* 移动端日期选择器适配 */
  .date-modal {
    max-width: 95%;
    width: 95%;
  }

  .datetime-input {
    font-size: 16px; /* 避免移动端缩放 */
  }
}
</style>
