<template>
  <div id="addPicturePage">
    <!-- 主要内容区域 -->
    <div class="main-content">
      <!-- 空间信息 -->
      <div v-if="spaceId" class="space-info">
        保存至空间：<a :href="`/space/${spaceId}`">{{ spaceId }}</a>
      </div>

      <!-- 根据是否有图片使用不同的布局 -->
      <div :class="['content-layout', { 'has-picture': picture }]">
        <!-- 上传区域 -->
        <div class="upload-section">
          <div class="upload-tabs">
            <a-tabs v-model:activeKey="uploadType">
              <a-tab-pane key="file" tab="文件上传">
                <PictureUpload
                  :picture="picture"
                  :spaceId="spaceId"
                  :onSuccess="onSuccess"
                  :onUploadStart="onUploadStart"
                  :onUploadProgress="onUploadProgress"
                />
              </a-tab-pane>
              <a-tab-pane key="url" tab="URL 上传" force-render>
                <UrlPictureUpload
                  :picture="picture"
                  :spaceId="spaceId"
                  :onSuccess="onSuccess"
                  :onUploadStart="onUploadStart"
                  :onUploadProgress="onUploadProgress"
                />
              </a-tab-pane>
            </a-tabs>
          </div>
          <!-- 上传进度提示 -->
          <div v-if="uploading" class="upload-progress">
            <a-progress
              :percent="uploadProgress"
              :status="uploadProgress >= 100 ? 'success' : 'active'"
              :stroke-color="{ from: '#ff8e53', to: '#ff6b6b' }"
            />
            <div class="progress-text">
              {{ uploadProgress >= 100 ? '处理中...' : '上传中...' }}
            </div>
          </div>
        </div>

        <!-- 表单区域 -->
        <div v-if="picture && !uploading" class="form-section">
          <!-- 图片编辑工具栏移到这里 -->
          <div class="edit-bar">
            <a-space size="middle">
              <a-button class="edit-button" @click="doEditPicture">
                <template #icon><EditOutlined /></template>
                编辑图片
              </a-button>
              <a-button type="primary" class="ai-button" @click="doImagePainting">
                <template #icon><FullscreenOutlined /></template>
                AI 扩图
              </a-button>
            </a-space>
          </div>

          <div class="form-container">
            <!-- 图片信息表单 -->
            <a-form
              name="pictureForm"
              layout="vertical"
              :model="pictureForm"
              @finish="handleSubmit"
            >
              <a-form-item name="name" label="名称">
                <a-input v-model:value="pictureForm.name" placeholder="请输入名称" allow-clear />
              </a-form-item>

              <a-form-item name="introduction" label="简介">
                <a-textarea
                  v-model:value="pictureForm.introduction"
                  placeholder="请输入简介"
                  :auto-size="{ minRows: 2, maxRows: 5 }"
                  allow-clear
                />
              </a-form-item>

              <a-form-item name="category" label="分类">
                <a-auto-complete
                  v-model:value="pictureForm.category"
                  placeholder="请输入分类"
                  :options="categoryOptions"
                  allow-clear
                />
              </a-form-item>

              <a-form-item name="tags" label="标签">
                <a-select
                  v-model:value="pictureForm.tags"
                  mode="tags"
                  placeholder="请输入标签"
                  :options="tagOptions"
                  allow-clear
                />
              </a-form-item>

              <a-form-item name="isDownload" label="下载权限">
                <a-switch
                  v-model:checked="pictureForm.isDownload"
                  :checkedValue="1"
                  :unCheckedValue="0"
                >
                  <template #checkedChildren>允许下载</template>
                  <template #unCheckedChildren>禁止下载</template>
                </a-switch>
              </a-form-item>

              <a-form-item>
                <a-space>
                  <a-button type="primary" html-type="submit" class="submit-button">创建</a-button>
                  <a-button class="cancel-button" @click="handleCancel">取消</a-button>
                </a-space>
              </a-form-item>
            </a-form>
          </div>
        </div>
      </div>

      <!-- 编辑器组件 -->
      <ImageCropper
        ref="imageCropperRef"
        :imageUrl="picture?.url"
        :picture="picture"
        :spaceId="spaceId"
        :space="spaceInfo"
        :onSuccess="onCropSuccess"
      />
      <ImageOutPainting
        ref="imageOutPaintingRef"
        :picture="picture"
        :spaceId="spaceId"
        :onSuccess="onImageOutPaintingSuccess"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import PictureUpload from '@/components/PictureUpload.vue'
import { h, onMounted, reactive, ref, computed, onBeforeUnmount } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  editPictureUsingPost,
  getPictureVoByIdUsingGet,
  listPictureTagCategoryUsingGet,
  deletePictureUsingPost,
} from '@/api/pictureController.ts'
import { useRoute, useRouter } from 'vue-router'
import UrlPictureUpload from '@/components/UrlPictureUpload.vue'
import { EditOutlined, FullscreenOutlined } from '@ant-design/icons-vue'
import ImageCropper from '@/components/ImageCropper.vue'
import ImageOutPainting from '@/components/ImageOutPainting.vue'
import { SPACE_LEVEL_ENUM } from '@/constants/space'
import { getSpaceVoByIdUsingGet } from '@/api/spaceController'

const picture = ref<API.PictureVO>()
const pictureForm = reactive<API.PictureEditRequest>({
  name: '',
  introduction: '',
  category: '',
  tags: [],
  isDownload: 1, // 默认允许下载
})
const uploadType = ref<'file' | 'url'>('file')

// 上传状态
const uploading = ref(false)
const uploadProgress = ref(0)

// 开始上传
const onUploadStart = () => {
  uploading.value = true
  uploadProgress.value = 0
  picture.value = undefined // 清空之前的图片数据
}

// 上传进度
const onUploadProgress = (progress: number) => {
  uploadProgress.value = Math.min(progress, 99) // 保留最后1%给处理时间
}

/**
 * 图片上传成功
 * @param newPicture
 */
const onSuccess = (newPicture: API.PictureVO) => {
  uploading.value = false
  uploadProgress.value = 100
  picture.value = newPicture
  pictureForm.name = newPicture.name
}

const router = useRouter()

// 空间 id
const spaceId = computed(() => {
  return route.query?.spaceId
})

// 获取空间信息
const spaceInfo = ref<API.SpaceVO>()
const getSpaceInfo = async () => {
  if (!spaceId.value) return
  const res = await getSpaceVoByIdUsingGet({ id: spaceId.value })
  if (res.data.code === 0 && res.data.data) {
    spaceInfo.value = res.data.data
  }
}

onMounted(() => {
  getSpaceInfo()
})

// 添加标记是否已创建的变量
const isCreated = ref(false)
// 添加标记是否已删除的变量
const isDeleted = ref(false)

/**
 * 提交表单
 * @param values
 */
const handleSubmit = async (values: any) => {
  const pictureId = picture.value.id
  const newspaceId = spaceId.value
  if (!pictureId) {
    return
  }
  const res = await editPictureUsingPost({
    id: pictureId,
    spaceId: spaceId.value,
    ...values,
  })
  // 操作成功
  if (res.data.code === 0 && res.data.data) {
    isCreated.value = true  // 标记图片已创建
    // 判断是否为公共空间
    if (spaceInfo.value?.spaceLevel === SPACE_LEVEL_ENUM.PUBLIC) {
      Modal.success({
        title: '上传成功',
        content: h('div', {}, [
          h('p', '您的图片已成功上传到公共图库！'),
          h('p', '由于这是公共图库，您的图片需要经过审核后才能展示。'),
          h('p', '审核通过后，您的图片将出现在公共图库中。'),
        ]),
        maskClosable: true,
        centered: true,
        okText: '知道了',
        onOk: () => {
          router.push({
            path: `/picture/${pictureId}`,
          })
        }
      })
    } else {
      Modal.success({
        title: '上传成功',
        content: h('div', {}, [
          h('p', '您的图片已成功上传到相应空间！'),
          h('p', '您可以在对应的空间中查看和管理您的图片。'),
        ]),
        maskClosable: true,
        centered: true,
        okText: '知道了',
        onOk: () => {
          router.push({
            path: `/space/${newspaceId}`,
          })
        }
      })
    }
  } else {
    message.error('创建失败，' + res.data.message)
  }
}

const categoryOptions = ref<string[]>([])
const tagOptions = ref<string[]>([])

/**
 * 获取标签和分类选项
 * @param values
 */
const getTagCategoryOptions = async () => {
  const res = await listPictureTagCategoryUsingGet()
  if (res.data.code === 0 && res.data.data) {
    tagOptions.value = (res.data.data.tagList ?? []).map((data: string) => {
      return {
        value: data,
        label: data,
      }
    })
    categoryOptions.value = (res.data.data.categoryList ?? []).map((data: string) => {
      return {
        value: data,
        label: data,
      }
    })
  } else {
    message.error('获取标签分类列表失败，' + res.data.message)
  }
}

onMounted(() => {
  getTagCategoryOptions()
})

const route = useRoute()

// 获取老数据
const getOldPicture = async () => {
  // 获取到 id
  const id = route.query?.id
  if (id) {
    const res = await getPictureVoByIdUsingGet({
      id,
    })
    if (res.data.code === 0 && res.data.data) {
      const data = res.data.data
      picture.value = data
      pictureForm.name = data.name
      pictureForm.introduction = data.introduction
      pictureForm.category = data.category
      pictureForm.tags = data.tags
    }
  }
}

onMounted(() => {
  getOldPicture()
})

const imageCropperRef = ref()

// 编辑图片
const doEditPicture = async () => {
  imageCropperRef.value?.openModal()
}

// 编辑成功事件
const onCropSuccess = (newPicture: API.PictureVO) => {
  picture.value = newPicture
}

// ----- AI 扩图引用 -----
const imageOutPaintingRef = ref()

// 打开 AI 扩图弹窗
const doImagePainting = async () => {
  imageOutPaintingRef.value?.openModal()
}

// AI 扩图保存事件
const onImageOutPaintingSuccess = (newPicture: API.PictureVO) => {
  picture.value = newPicture
}

// 添加取消按钮的处理函数
const handleCancel = async () => {
  if (picture.value?.id && !isDeleted.value) {
    try {
      await deletePictureUsingPost({ id: picture.value.id })
      isDeleted.value = true // 标记图片已删除
      message.success('已取消上传')
      // 清空上传的内容
      picture.value = undefined
      pictureForm.name = ''
      pictureForm.introduction = ''
      pictureForm.category = ''
      pictureForm.tags = []
      pictureForm.isDownload = 1
      // 重置上传状态
      uploading.value = false
      uploadProgress.value = 0
    } catch (error) {
      message.error('取消上传失败')
    }
  } else {
    // 没有上传图片时也清空表单
    picture.value = undefined
    pictureForm.name = ''
    pictureForm.introduction = ''
    pictureForm.category = ''
    pictureForm.tags = []
    pictureForm.isDownload = 1
    // 重置上传状态
    uploading.value = false
    uploadProgress.value = 0
  }
}

// 页面卸载前的处理
onBeforeUnmount(async () => {
  // 只有在图片存在且未创建且未删除的情况下才删除
  if (picture.value?.id && !isCreated.value && !isDeleted.value) {
    try {
      await deletePictureUsingPost({ id: picture.value.id })
    } catch (error) {
      console.error('Failed to delete uncreated picture:', error)
    }
  }
})
</script>

<style scoped>
#addPicturePage {
  min-height: 100vh;
  margin: -30px -28px !important;
  padding: 0;
  position: relative;
  overflow: hidden;
  color: #333;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  background: linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%);
  border-radius: 2px !important;
  z-index: 1;
}

/* 添加背景动画效果 */
#addPicturePage::before {
  content: '';
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background:
    radial-gradient(circle at 20% 20%, rgba(255, 142, 83, 0.15) 0%, transparent 40%),
    radial-gradient(circle at 80% 80%, rgba(255, 107, 107, 0.15) 0%, transparent 40%);
  filter: blur(80px);
  transform: translateZ(0);

  animation: gradientMove 20s ease-in-out infinite;
}

@keyframes gradientMove {
  0% {
    background-position: 0% 0%;
  }
  50% {
    background-position: 100% 100%;
  }
  100% {
    background-position: 0% 0%;
  }
}

.main-content {
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
  padding: 28px 24px;
  position: relative;
  z-index: 2;
  min-height: calc(100vh - 60px);
  display: flex;
  flex-direction: column;
}

.space-info {
  color: rgba(51, 51, 51, 0.9);
  font-size: 15px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  padding: 16px 24px;
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
}

.space-info a {
  color: #ff8e53;
  text-decoration: none;
  transition: color 0.3s ease;
  font-weight: 500;
}

.space-info a:hover {
  color: #ff6b6b;
}

.upload-tabs {
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
  margin-bottom: 16px;
  border: 1px solid rgba(255, 255, 255, 0.4);
  transition: all 0.3s ease;
}

.upload-tabs:hover {
  background: rgba(255, 255, 255, 0.9);
  transform: translateY(-2px);
  box-shadow: 0 12px 36px rgba(0, 0, 0, 0.1);
}

:deep(.ant-tabs-nav) {
  margin-bottom: 16px;
  background: transparent;
}

:deep(.ant-tabs-tab) {
  font-size: 15px;
  padding: 8px 16px;
  color: rgba(102, 102, 102, 0.8) !important;
  transition: all 0.3s ease;
}

:deep(.ant-tabs-tab-active) {
  font-weight: 500;
  color: #ff8e53 !important;
  background: rgba(255, 142, 83, 0.1);
  border-radius: 8px;
}

:deep(.ant-tabs-ink-bar) {
  background: #ff8e53 !important;
  height: 3px;
  border-radius: 3px;
}

:deep(.ant-tabs-nav::before) {
  border-bottom: none !important;
}

.edit-bar {
  text-align: center;
  margin-bottom: 16px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  padding: 16px;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.4);
  transition: all 0.3s ease;
}

.edit-bar:hover {
  background: rgba(255, 255, 255, 0.9);
  transform: translateY(-2px);
  box-shadow: 0 12px 36px rgba(0, 0, 0, 0.1);
}

.edit-button,
.ai-button {
  height: 40px;
  padding: 0 24px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.edit-button {
  border: 1px solid rgba(230, 230, 230, 0.8);
  color: #666;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
}

.edit-button:hover {
  border-color: #ff8e53;
  color: #ff8e53;
  transform: translateY(-1px);
  background: rgba(255, 142, 83, 0.1);
}

.ai-button {
  background: linear-gradient(135deg, rgba(255, 142, 83, 0.95), rgba(255, 107, 107, 0.95));
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  border: none;
  box-shadow: 0 4px 12px rgba(255, 107, 107, 0.2);
}

.ai-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(255, 107, 107, 0.3);
  background: linear-gradient(135deg, #ff8e53, #ff6b6b);
}

.form-container {
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 24px;
  padding: 32px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.5);
  transition: all 0.3s ease;
  flex: 1;
  margin-bottom: 24px;
}

.form-container:hover {
  background: rgba(255, 255, 255, 0.9);
  transform: translateY(-2px);
  box-shadow: 0 12px 36px rgba(0, 0, 0, 0.1);
}

:deep(.ant-form-item-label > label) {
  font-size: 14px;
  color: rgba(102, 102, 102, 0.9) !important;
  font-weight: 500;
}

:deep(.ant-input),
:deep(.ant-select-selector),
:deep(.ant-input-affix-wrapper) {
  border-radius: 10px !important;
  border: 1px solid rgba(230, 230, 230, 0.8) !important;
  background: rgba(255, 255, 255, 0.9) !important;
  backdrop-filter: blur(4px) !important;
  -webkit-backdrop-filter: blur(4px) !important;
  color: #333 !important;
  transition: all 0.3s ease;
}

:deep(.ant-input::placeholder),
:deep(.ant-select-selection-placeholder) {
  color: rgba(153, 153, 153, 0.8) !important;
}

:deep(.ant-input:hover),
:deep(.ant-select:hover .ant-select-selector),
:deep(.ant-input-affix-wrapper:hover) {
  border-color: #ff8e53 !important;
  background: rgba(255, 255, 255, 0.95) !important;
}

:deep(.ant-input:focus),
:deep(.ant-select-focused .ant-select-selector),
:deep(.ant-input-affix-wrapper-focused) {
  border-color: #ff8e53 !important;
  box-shadow: 0 0 0 2px rgba(255, 142, 83, 0.2) !important;
  background: #ffffff !important;
}

/* 修改按钮容器样式 */
:deep(.ant-form-item:last-child) {
  margin-bottom: 0;

  .ant-form-item-control-input-content {
    display: flex;
    justify-content: center;
    gap: 16px;
  }
}

/* 修改按钮样式 */
.submit-button,
.cancel-button {
  width: 120px !important;
  height: 48px;
  font-size: 16px;
  border-radius: 24px;
  transition: all 0.3s ease;
}

.submit-button {
  background: linear-gradient(135deg, #ff8e53, #ff6b6b);
  border: none;
  box-shadow: 0 4px 12px rgba(255, 107, 107, 0.2);
}

.submit-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(255, 107, 107, 0.3);
  background: linear-gradient(135deg, #ff9b67, #ff7e7e);
}

.cancel-button {
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(230, 230, 230, 0.8);
  color: rgba(102, 102, 102, 0.9);
}

.cancel-button:hover {
  border-color: #ff6b6b;
  color: #ff6b6b;
  background: rgba(255, 107, 107, 0.05);
  transform: translateY(-2px);
}

/* 上传进度样式 */
.upload-progress {
  margin-top: 16px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.4);
  transition: all 0.3s ease;
}

.upload-progress:hover {
  background: rgba(255, 255, 255, 0.9);
  transform: translateY(-2px);
  box-shadow: 0 12px 36px rgba(0, 0, 0, 0.1);
}

.progress-text {
  margin-top: 8px;
  text-align: center;
  color: rgba(102, 102, 102, 0.9);
  font-size: 14px;
  font-weight: 500;
}

:deep(.ant-progress-bg) {
  background: linear-gradient(90deg, rgba(255, 142, 83, 0.95), rgba(255, 107, 107, 0.95)) !important;
  backdrop-filter: blur(4px) !important;
  -webkit-backdrop-filter: blur(4px) !important;
}

:deep(.ant-progress-text) {
  color: rgba(102, 102, 102, 0.9) !important;
}

/* 上传区域样式 */
.upload-area {
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 2px dashed rgba(255, 142, 83, 0.3);
  border-radius: 24px;
  padding: 48px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.upload-area::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(45deg, rgba(255, 142, 83, 0.05), rgba(255, 107, 107, 0.05));
  opacity: 0;
  transition: opacity 0.3s ease;
}

.upload-area:hover {
  border-color: #ff8e53;
  transform: translateY(-2px);
}

.upload-area:hover::before {
  opacity: 1;
}

.upload-icon {
  font-size: 48px;
  color: rgba(255, 142, 83, 0.9);
  margin-bottom: 16px;
  transition: all 0.3s ease;
}

.upload-area:hover .upload-icon {
  transform: scale(1.1);
  color: #ff8e53;
}

.upload-text {
  color: rgba(102, 102, 102, 0.9);
  font-size: 16px;
  margin-bottom: 8px;
  font-weight: 500;
}

.upload-hint {
  color: rgba(153, 153, 153, 0.8);
  font-size: 14px;
}

/* 添加动画效果 */
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.space-info,
.upload-tabs,
.edit-bar,
.form-container {
  animation: fadeIn 0.6s ease-out forwards;
}

/* 修改上传区域和表单的布局 */
.content-layout {
  margin-top: 24px;
  display: grid;
  grid-template-columns: 1fr;
  gap: 24px;
  flex: 1;
  min-height: 0;
}

/* 有图片时的布局 */
.content-layout.has-picture {
  grid-template-columns: 1.2fr 1fr;
}

.upload-section {
  width: 100%;
}

.form-section {
  width: 100%;
}

/* 响应式布局优化 */
@media screen and (max-width: 1200px) {
  .main-content {
    max-width: 900px;
  }
}

@media screen and (max-width: 768px) {
  #addPicturePage {
    margin: -28px -28px !important;
    min-height: calc(100vh + 56px);
    border-radius: 0 !important;
  }

  .main-content {
    padding: 16px;
    min-height: calc(100vh + 56px);
  }

  .content-layout.has-picture {
    grid-template-columns: 1fr;
  }

  .form-container {
    padding: 20px;
    margin-bottom: 16px;
  }
}

/* 表单容器样式优化 */
.form-container {
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 24px;
  padding: 32px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.5);
  transition: all 0.3s ease;
}

.form-container:hover {
  background: rgba(255, 255, 255, 0.9);
  transform: translateY(-2px);
  box-shadow: 0 12px 36px rgba(0, 0, 0, 0.1);
}

/* 按钮样式优化 */
.submit-button,
.cancel-button {
  height: 48px;
  font-size: 16px;
  border-radius: 24px;
  transition: all 0.3s ease;
}

.submit-button {
  background: linear-gradient(135deg, #ff8e53, #ff6b6b);
  border: none;
  box-shadow: 0 4px 12px rgba(255, 107, 107, 0.2);
}

.submit-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(255, 107, 107, 0.3);
  background: linear-gradient(135deg, #ff9b67, #ff7e7e);
}

.cancel-button {
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(230, 230, 230, 0.8);
  color: rgba(102, 102, 102, 0.9);
}

.cancel-button:hover {
  border-color: #ff6b6b;
  color: #ff6b6b;
  background: rgba(255, 107, 107, 0.05);
  transform: translateY(-2px);
}
</style>
