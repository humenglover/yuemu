<template>
  <div class="box">
    <div class="publish-container">
      <header class="publish-header">
        <button class="back-btn" @click="handleBack">
          <i class="fa-solid fa-arrow-left"></i>
        </button>
        <h1 class="header-title">发布图片</h1>
        <button class="publish-btn" @click="handleSubmit" :disabled="!picture || submitting">
          {{ submitting ? '发布中...' : '发布' }}
        </button>
      </header>

      <div v-if="spaceId && spaceInfo" class="space-card">
        <img :src="spaceInfo.spaceCover|| 'src/assets/default-av.png'" alt="空间头像" class="space-avatar" />
        <div class="space-meta">
          <h3 class="space-name">{{ spaceInfo.spaceName }}</h3>
          <p class="space-desc">{{ spaceInfo.spaceDescription || '暂无描述' }}</p>
        </div>
        <div class="space-stats" v-if="spaceInfo">
          <span class="stat-item">
            {{ Math.round((spaceInfo.totalSize || 0) / 1024 / 1024) }}/{{ Math.round((spaceInfo.maxSize || 0) / 1024 / 1024) }}MB
          </span>
          <span class="stat-item">
            {{ spaceInfo.totalCount || 0 }}/{{ spaceInfo.maxCount || 0 }}张
          </span>
        </div>
      </div>

      <main class="publish-main">
        <div class="pc-layout" v-if="!isMobile && picture && !uploading">
          <div class="pc-left">
            <div class="upload-wrapper pc-panel">
              <div class="preview-wrapper">
                <img :src="picture.url" alt="预览图" class="preview-img" @click="showImagePreview" />

                <div class="preview-actions">
                  <button class="action-btn" @click="doEditPicture">
                    <i class="fa-solid fa-pen-to-square"></i>
                    编辑
                  </button>
                  <button class="action-btn" @click="doImagePainting">
                    <i class="fa-solid fa-palette"></i>
                    扩图
                  </button>
                  <button class="action-btn" @click="handleReupload">
                    <i class="fa-solid fa-rotate-right"></i>
                    重传
                  </button>
                </div>
              </div>

            </div>
          </div>

          <div class="pc-right">
            <div class="form-wrapper pc-panel">
              <div class="form-grid">
                <div class="form-item">
                  <input
                    v-model="pictureForm.name"
                    placeholder="给图片起个好听的名字吧~"
                    class="input-name"
                  />
                </div>

                <div class="form-item">
                  <textarea
                    v-model="pictureForm.introduction"
                    placeholder="分享一下这张图片的故事吧..."
                    class="textarea-desc"
                    rows="6"
                  ></textarea>
                </div>

                <div class="form-item select-item">
                  <div class="select-label">分类</div>
                  <div class="select-value" @click="showCategoryModal = true">
                    {{ pictureForm.category || '选择分类' }}
                    <i class="fa-solid fa-chevron-down"></i>
                  </div>
                </div>

                <div class="form-item select-item">
                  <div class="select-label">标签</div>
                  <div class="select-value" @click="showTagModal = true">
                    {{ pictureForm.tags.length > 0 ? pictureForm.tags.join('、') : '添加标签' }}
                    <i class="fa-solid fa-chevron-down"></i>
                  </div>
                </div>

                <div class="form-item switch-item">
                  <div class="switch-label">允许下载</div>
                  <div class="switch-btn" :class="{ active: pictureForm.isDownload === 1 }" @click="pictureForm.isDownload = pictureForm.isDownload === 1 ? 0 : 1">
                    <div class="switch-dot"></div>
                  </div>
                </div>
              </div>

              <div class="form-actions">
                <button class="btn draft-btn" @click="saveAsDraft" v-if="!spaceId">
                  <i class="fa-solid fa-file-draft"></i>
                  存为草稿
                </button>
                <button class="btn cancel-btn" @click="handleCancel" v-if="!spaceId">
                  <i class="fa-solid fa-times"></i>
                  取消
                </button>
              </div>

              <!-- PC AI 检测结果展示区 (仅公共空间图片才显示) -->
              <div v-if="picture?.url && !spaceId" class="ai-analysis-panel pc-ai-panel">
                <div class="ai-header">
                  <i class="fa-solid fa-wand-magic-sparkles"></i>
                  <span>AI 智能识别结果</span>
                </div>
                <div class="ai-content">
                  <div v-if="aiTagLoading" class="ai-loading-mini">
                    <div class="mini-spinner"></div>
                    <span>AI 正在分析中...</span>
                  </div>
                  <div v-else-if="picture?.aiLabels && picture.aiLabels.length > 0" class="ai-tags">
                    <span v-for="tag in translateLabels(picture.aiLabels)" :key="tag" class="ai-tag-chip">
                      {{ tag }}
                    </span>
                  </div>
                  <div v-if="!aiTagLoading && picture?.aiLabels && hasSensitiveLabel(picture.aiLabels)" class="ai-warning">
                    <i class="fa-solid fa-circle-exclamation"></i>
                    <p>{{ getSensitiveWarning() }}</p>
                  </div>
                  <button class="ai-more-btn" @click="viewYoloDetail" :disabled="aiTagLoading">
                    <i class="fa-solid fa-expand"></i>
                    {{ aiTagLoading ? '识别中...' : (picture?.aiLabels?.length ? '查看识别详情' : 'AI 智能识别') }}
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="mobile-layout" v-if="isMobile || !picture || uploading">
          <div class="upload-wrapper">
            <div v-if="!picture && !uploading" class="upload-area">
              <div class="upload-tabs">
                <button class="tab-item" :class="{ active: uploadType === 'file' }" @click="uploadType = 'file'">
                  上传图片
                </button>
                <button class="tab-item" :class="{ active: uploadType === 'url' }" @click="uploadType = 'url'">
                  链接上传
                </button>
              </div>

              <div class="upload-content">
                <PictureUpload
                  v-if="uploadType === 'file'"
                  :spaceId="spaceId"
                  :onSuccess="onUploadSuccess"
                  :onUploadStart="onUploadStart"
                  :onUploadProgress="onUploadProgress"
                />
                <UrlPictureUpload
                  v-else
                  :spaceId="spaceId"
                  :onSuccess="onUploadSuccess"
                  :onUploadStart="onUploadStart"
                  :onUploadProgress="onUploadProgress"
                />
              </div>
            </div>

            <div v-if="uploading" class="upload-progress">
              <div class="progress-bar">
                <div class="progress-fill" :style="{ width: uploadProgress + '%' }"></div>
              </div>
              <p class="progress-text">{{ uploadProgress >= 100 ? '处理中...' : `上传中 ${uploadProgress}%` }}</p>
            </div>

            <div v-if="picture && !uploading && isMobile" class="preview-wrapper">
              <img :src="picture.url" alt="预览图" class="preview-img" @click="showImagePreview" />

              <div class="preview-actions">
                <button class="action-btn" @click="doEditPicture">
                  <i class="fa-solid fa-pen-to-square"></i>
                  编辑
                </button>
                <button class="action-btn" @click="doImagePainting">
                  <i class="fa-solid fa-palette"></i>
                  扩图
                </button>
                <button class="action-btn" @click="handleReupload">
                  <i class="fa-solid fa-rotate-right"></i>
                  重传
                </button>
              </div>
            </div>
          </div>

          <div v-if="picture && !uploading && isMobile" class="form-wrapper">
            <div class="form-grid">
              <!-- Mobile AI 检测结果 (仅公共空间图片才显示) -->
              <div v-if="picture?.url && !spaceId" class="ai-analysis-panel mobile-ai-panel">
                <div class="ai-header">
                  <i class="fa-solid fa-wand-magic-sparkles"></i>
                  <span>AI 智能识别结果</span>
                </div>
                <div class="ai-content">
                  <div v-if="aiTagLoading" class="ai-loading-mini">
                    <div class="mini-spinner"></div>
                    <span>AI 正在分析中...</span>
                  </div>
                  <div v-else-if="picture?.aiLabels && picture.aiLabels.length > 0" class="ai-tags">
                    <span v-for="tag in translateLabels(picture.aiLabels)" :key="tag" class="ai-tag-chip">
                      {{ tag }}
                    </span>
                  </div>
                  <div v-if="!aiTagLoading && picture?.aiLabels && hasSensitiveLabel(picture.aiLabels)" class="ai-warning">
                    <i class="fa-solid fa-circle-exclamation"></i>
                    <p>{{ getSensitiveWarning() }}</p>
                  </div>
                  <button class="ai-more-btn" @click="viewYoloDetail" :disabled="aiTagLoading">
                    <i class="fa-solid fa-expand"></i>
                    {{ aiTagLoading ? '识别中...' : (picture?.aiLabels?.length ? '查看识别详情' : 'AI 智能识别') }}
                  </button>
                </div>
              </div>

              <div class="form-item">
                <input
                  v-model="pictureForm.name"
                  placeholder="给图片起个好听的名字吧~"
                  class="input-name"
                />
              </div>

              <div class="form-item">
                <textarea
                  v-model="pictureForm.introduction"
                  placeholder="分享一下这张图片的故事吧..."
                  class="textarea-desc"
                  rows="4"
                ></textarea>
              </div>

              <div class="form-item select-item">
                <div class="select-label">分类</div>
                <div class="select-value" @click="showCategoryModal = true">
                  {{ pictureForm.category || '选择分类' }}
                  <i class="fa-solid fa-chevron-down"></i>
                </div>
              </div>

              <div class="form-item select-item">
                <div class="select-label">标签</div>
                <div class="select-value" @click="showTagModal = true">
                  {{ pictureForm.tags.length > 0 ? pictureForm.tags.join('、') : '添加标签' }}
                  <i class="fa-solid fa-chevron-down"></i>
                </div>
              </div>

              <div class="form-item switch-item">
                <div class="switch-label">允许下载</div>
                <div class="switch-btn" :class="{ active: pictureForm.isDownload === 1 }" @click="pictureForm.isDownload = pictureForm.isDownload === 1 ? 0 : 1">
                  <div class="switch-dot"></div>
                </div>
              </div>
            </div>

            <div class="form-actions">
              <button class="btn draft-btn" @click="saveAsDraft" v-if="!spaceId">
                <i class="fa-solid fa-file-draft"></i>
                存为草稿
              </button>
              <button class="btn cancel-btn" @click="handleCancel" v-if="!spaceId">
                <i class="fa-solid fa-times"></i>
                取消
              </button>
            </div>
          </div>
        </div>
      </main>

      <!-- 分类弹窗 - 增加搜索框 -->
      <div v-if="showCategoryModal" class="modal-mask" @click="showCategoryModal = false">
        <div class="modal-content category-modal" @click.stop>
          <div class="modal-header">
            <h3 class="modal-title">选择分类</h3>
            <button class="modal-close" @click="showCategoryModal = false">
              <i class="fa-solid fa-xmark"></i>
            </button>
          </div>
          <!-- 分类搜索框 -->
          <div class="modal-search">
            <div class="search-input-wrapper">
              <i class="fa-solid fa-search search-icon"></i>
              <input
                v-model="categorySearchText"
                placeholder="搜索分类..."
                class="modal-search-input"
                @input="filterCategories"
              />
              <button
                v-if="categorySearchText"
                class="search-clear-btn"
                @click="clearCategorySearch"
              >
                <i class="fa-solid fa-xmark"></i>
              </button>
            </div>
          </div>
          <!-- 添加滚动容器 -->
          <div class="modal-body category-scroll-container">
            <!-- 无结果提示 -->
            <div v-if="filteredCategoryOptions.length === 0" class="no-result">
              <i class="fa-solid fa-magnifying-glass"></i>
              <p>未找到 "{{ categorySearchText }}" 相关分类</p>
            </div>
            <div
              class="category-item"
              v-for="category in filteredCategoryOptions"
              :key="category.value"
              @click="selectCategory(category.value)"
              :class="{ active: pictureForm.category === category.value }"
            >
              {{ category.label }}
            </div>
          </div>
        </div>
      </div>

      <div v-if="showTagModal" class="modal-mask" @click="showTagModal = false">
        <div class="modal-content tag-modal" @click.stop>
          <div class="modal-header">
            <h3 class="modal-title">添加标签</h3>
            <button class="modal-close" @click="showTagModal = false">
              <i class="fa-solid fa-xmark"></i>
            </button>
          </div>
          <!-- 标签搜索框 -->
          <div class="modal-search tag-search-wrapper">
            <div class="search-input-wrapper">
              <i class="fa-solid fa-search search-icon"></i>
              <input
                v-model="tagSearchText"
                placeholder="搜索标签..."
                class="modal-search-input"
                @input="filterTags"
              />
              <button
                v-if="tagSearchText"
                class="search-clear-btn"
                @click="clearTagSearch"
              >
                <i class="fa-solid fa-xmark"></i>
              </button>
            </div>
          </div>
          <!-- 添加标签滚动容器 -->
          <div class="tag-scroll-container">
            <!-- 自定义标签输入 -->
            <input
              v-model="tagInput"
              placeholder="输入标签后按回车添加"
              class="tag-input"
              @keyup.enter="addCustomTag"
            />

            <!-- 标签列表（带过滤） -->
            <div class="tag-list">
              <!-- 无结果提示 -->
              <div v-if="filteredTagOptions.length === 0 && tagSearchText" class="no-result tag-no-result">
                <i class="fa-solid fa-magnifying-glass"></i>
                <p>未找到 "{{ tagSearchText }}" 相关标签</p>
              </div>
              <div
                class="tag-item"
                v-for="tag in filteredTagOptions"
                :key="tag.value"
                @click="toggleTag(tag.value)"
                :class="{ active: pictureForm.tags.includes(tag.value) }"
              >
                {{ tag.label }}
              </div>
            </div>

            <div class="selected-tags" v-if="pictureForm.tags.length > 0">
              <span class="selected-tag" v-for="tag in pictureForm.tags" :key="tag">
                {{ tag }}
                <button class="tag-close" @click="removeTag(tag)">
                  <i class="fa-solid fa-xmark"></i>
                </button>
              </span>
            </div>
          </div>
        </div>
      </div>

      <div v-if="showPreview" class="preview-mask" @click="closeImagePreview">
        <img :src="picture?.url" alt="大图预览" class="preview-big-img" @click.stop />
        <button class="preview-close" @click="closeImagePreview">
          <i class="fa-solid fa-xmark"></i>
        </button>
      </div>

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

      <!-- AI 识别详情弹窗 -->
      <div v-if="showYoloModal" class="modal-mask" @click.self="showYoloModal = false">
        <div class="modal-content yolo-modal">
          <div class="modal-header">
            <h3 class="modal-title">AI 智能识别详情</h3>
            <button class="modal-close" @click="showYoloModal = false">
              <i class="fa-solid fa-times"></i>
            </button>
          </div>
          <div class="modal-body yolo-modal-body">
            <div v-if="yoloLoading" class="yolo-loading">
              <div class="loading-spinner"></div>
              <p>正在分析智能识别结果...</p>
            </div>
            <div v-else-if="yoloResult" class="yolo-result-container">
              <img
                :src="'data:image/jpeg;base64,' + yoloResult.annotatedImageBase64"
                alt="AI 标注图"
                class="yolo-annotated-img"
              />
              <div class="yolo-info-panel">
                <div class="yolo-stat-item">
                  <span class="label">识别总数:</span>
                  <span class="value">{{ yoloResult.detections?.length || 0 }}</span>
                </div>
                <div class="yolo-detection-list">
                  <div v-for="(det, index) in yoloResult.detections" :key="index" class="yolo-det-item">
                    <span class="det-label">{{ translateLabels([det.label])[0] }}</span>
                    <span class="det-conf">{{ (det.confidence * 100).toFixed(1) }}%</span>
                  </div>
                </div>
              </div>
            </div>
            <div v-else class="yolo-error">
              <i class="fa-solid fa-triangle-exclamation"></i>
              <p>未获取到有效识别数据</p>
            </div>
          </div>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup lang="ts">
import PictureUpload from '@/components/PictureUpload.vue'
import UrlPictureUpload from '@/components/UrlPictureUpload.vue'
import ImageCropper from '@/components/ImageCropper.vue'
import ImageOutPainting from '@/components/ImageOutPainting.vue'
import { ref, reactive, computed, onMounted } from 'vue'
import { translateLabels, hasSensitiveLabel, getSensitiveWarning } from '@/utils/yoloUtils'
import { message, Modal } from 'ant-design-vue'
import { useRoute, useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { detectObjectsByUrlUsingPost, detectObjectsUsingPost } from '@/api/yoloController'
import { getDeviceType } from '@/utils/device'
import { DEVICE_TYPE_ENUM } from '@/constants/device'
import { SPACE_TYPE_ENUM } from '@/constants/space'
import {
  editPictureUsingPost,
  getPictureVoByIdUsingGet,
  listPictureTagCategoryUsingGet,
  deletePictureUsingPost,
  updatePictureDraftStatusUsingPost,
  getLatestDraftUsingGet,
  aiTagUsingPost
} from '@/api/pictureController'
import { getSpaceVoByIdUsingGet } from '@/api/spaceController'

const picture = ref<any>()
const uploading = ref(false)
const aiTagLoading = ref(false)
const uploadProgress = ref(0)
const uploadType = ref<'file' | 'url'>('file')
const submitting = ref(false)
const isMobile = ref(false)

const pictureForm = reactive<API.PictureEditRequest>({
  name: '',
  introduction: '',
  category: '',
  tags: [],
  isDownload: 1,
  aiLabels: ''
})

const showCategoryModal = ref(false)
const showTagModal = ref(false)
const showPreview = ref(false)
const tagInput = ref('')

// YOLO 详情相关
const showYoloModal = ref(false)
const yoloLoading = ref(false)
const yoloResult = ref<{ annotatedImageBase64: string; detections: any[] } | null>(null)


// 搜索相关状态
const categorySearchText = ref('')
const tagSearchText = ref('')
const filteredCategoryOptions = ref<{ label: string; value: string }[]>([])
const filteredTagOptions = ref<{ label: string; value: string }[]>([])

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

const spaceId = computed(() => route.query?.spaceId || null)
const spaceInfo = ref<API.SpaceVO>()

const categoryOptions = ref<{ label: string; value: string }[]>([])
const tagOptions = ref<{ label: string; value: string }[]>([])

const isCreated = ref(false)
const isDeleted = ref(false)

const imageCropperRef = ref()
const imageOutPaintingRef = ref()


onMounted(async () => {
  isMobile.value = (await getDeviceType()) === DEVICE_TYPE_ENUM.MOBILE
  await Promise.all([
    getSpaceInfo(),
    getTagCategoryOptions(),
    getOldPicture()
  ])
  if (!spaceId.value) getLatestDraft()

  // 初始化过滤列表
  filteredCategoryOptions.value = [...categoryOptions.value]
  filteredTagOptions.value = [...tagOptions.value]
})

const onUploadStart = () => {
  uploading.value = true
  uploadProgress.value = 0
  picture.value = undefined
}

const onUploadProgress = (progress: number) => {
  uploadProgress.value = Math.min(progress, 99)
}

const onSuccess = (newPicture: API.PictureVO) => {
  uploading.value = false
  uploadProgress.value = 100
  picture.value = newPicture
  pictureForm.name = newPicture.name
  pictureForm.isDownload = newPicture.isDownload ?? 1
  if (newPicture.aiLabels) {
    // 确保 aiLabels 存为 JSON 字符串（后端字段是 String）
    pictureForm.aiLabels = Array.isArray(newPicture.aiLabels)
      ? JSON.stringify(newPicture.aiLabels)
      : newPicture.aiLabels
  }
}

// 图片上传组件成功的回调 (携带文件对象)
const onUploadSuccess = async (newPicture: API.PictureVO, file: File) => {
  onSuccess(newPicture)
  // 如果是公共空间（或没有标签），异步触发后端 AI 标签接口
  if (!spaceId.value) {
    doAiTag(file, String(newPicture.id))
  }
}

const getSpaceInfo = async () => {
  if (spaceId.value) {
    const res = await getSpaceVoByIdUsingGet({ id: spaceId.value })
    if (res.data.code === 0) spaceInfo.value = res.data.data
  }
}

const getTagCategoryOptions = async () => {
  const res = await listPictureTagCategoryUsingGet()
  if (res.data.code === 0 && res.data.data) {
    tagOptions.value = (res.data.data.tagList ?? []).map((item: string) => ({
      value: item,
      label: item
    }))
    categoryOptions.value = (res.data.data.categoryList ?? []).map((item: string) => ({
      value: item,
      label: item
    }))

    // 初始化过滤列表
    filteredCategoryOptions.value = [...categoryOptions.value]
    filteredTagOptions.value = [...tagOptions.value]
  }
}

const getOldPicture = async () => {
  const id = route.query?.id
  if (id) {
    const res = await getPictureVoByIdUsingGet({ id })
    if (res.data.code === 0 && res.data.data) {
      const data = res.data.data
      picture.value = data
      Object.assign(pictureForm, {
        name: data.name,
        introduction: data.introduction,
        category: data.category,
        tags: data.tags,
        isDownload: data.isDownload ?? 1,
        aiLabels: data.aiLabels || ''
      })
    }
  }
}

const getLatestDraft = async () => {
  try {
    const res = await getLatestDraftUsingGet()
    if (res.data.code === 0 && res.data.data && !picture.value) {
      const data = res.data.data
      picture.value = data
      Object.assign(pictureForm, {
        name: data.name || '',
        introduction: data.introduction || '',
        category: data.category || '',
        tags: data.tags || [],
        isDownload: data.isDownload ?? 1,
        aiLabels: data.aiLabels || ''
      })
    }
  } catch (e) { console.log('获取草稿失败:', e) }
}

// 分类搜索过滤
const filterCategories = () => {
  const searchText = categorySearchText.value.trim().toLowerCase()
  if (!searchText) {
    filteredCategoryOptions.value = [...categoryOptions.value]
    return
  }

  filteredCategoryOptions.value = categoryOptions.value.filter((item: any) =>
    item.label.toLowerCase().includes(searchText) ||
    item.value.toLowerCase().includes(searchText)
  )
}

// 清空分类搜索
const clearCategorySearch = () => {
  categorySearchText.value = ''
  filterCategories()
}

// 标签搜索过滤
const filterTags = () => {
  const searchText = tagSearchText.value.trim().toLowerCase()
  if (!searchText) {
    filteredTagOptions.value = [...tagOptions.value]
    return
  }

  filteredTagOptions.value = tagOptions.value.filter((item: any) =>
    item.label.toLowerCase().includes(searchText) ||
    item.value.toLowerCase().includes(searchText)
  )
}

// 查看 YOLO 详情
const viewYoloDetail = async () => {
  if (!picture.value?.url) return

  showYoloModal.value = true
  yoloLoading.value = true
  yoloResult.value = null

  try {
    // 方案：前端先下载图片 blob，再调用普通的 multipart 接口，避免后端访问外网
    const response = await fetch(picture.value.url)
    const blob = await response.blob()
    const file = new File([blob], 'image.jpg', { type: blob.type })

    const res = await detectObjectsUsingPost({}, file)
    if (res.data.code === 0) {
      yoloResult.value = res.data.data
    } else {
      message.error('获取识别详情失败：' + res.data.message)
    }
  } catch (error) {
    console.error('YOLO detail error:', error)
    message.error('由于网络限制或图片跨域，无法获取识别详情')
  } finally {
    yoloLoading.value = false
  }
}

const doAiTag = async (file?: File, pictureId?: string) => {
  const pId = pictureId || (picture.value?.id ? String(picture.value.id) : null)
  if (!pId) return

  aiTagLoading.value = true
  try {
    let targetFile = file

    if (!targetFile && picture.value?.url) {
      try {
        const response = await fetch(picture.value.url)
        const blob = await response.blob()
        const extension = picture.value.url.split('.').pop() || 'jpg'
        targetFile = new File([blob], `image.${extension}`, { type: blob.type })
      } catch (e) {
        console.error('Fetch image as blob failed:', e)
        message.error('由于跨域或网络限制，AI 自动识别失败，请尝试重新上传或手动点击识别')
        return
      }
    }

    if (!targetFile) return

    const res = await aiTagUsingPost(
      { pictureId: pId },
      {},
      targetFile
    )
    console.log('AI Tagging result:', res.data.data)

    if (res.data.data) {
      const labels = res.data.data
      if (picture.value) {

        const labelsStr = JSON.stringify(labels)
        picture.value = {
          ...picture.value,
          aiLabels: labelsStr
        }
        pictureForm.aiLabels = labelsStr
        message.success('AI 智能识别成功')
      }
    } else {
      message.error('AI 识别失败：' + res.data.message)
    }
  } catch (error) {
    console.error('AI Tagging error:', error)
    message.error('AI 识别异常，请检查网络或重试')
  } finally {
    aiTagLoading.value = false
  }
}

const selectCategory = (value: string) => {
  pictureForm.category = value
  showCategoryModal.value = false
}

const toggleTag = (value: string) => {
  const index = pictureForm.tags.indexOf(value)
  if (index > -1) {
    pictureForm.tags.splice(index, 1)
  } else {
    pictureForm.tags.push(value)
  }
}

const addCustomTag = () => {
  const val = tagInput.value.trim()
  if (val && !pictureForm.tags.includes(val)) {
    pictureForm.tags.push(val)
    tagInput.value = ''
  }
}


const removeTag = (tag: string) => {
  const index = pictureForm.tags.indexOf(tag)
  if (index > -1) pictureForm.tags.splice(index, 1)
}

const showImagePreview = () => {
  if (picture.value?.url) showPreview.value = true
}

const closeImagePreview = () => {
  showPreview.value = false
}

const doEditPicture = () => {
  imageCropperRef.value?.openModal()
}

const doImagePainting = () => {
  imageOutPaintingRef.value?.openModal()
}

const onCropSuccess = (newPicture: API.PictureVO) => {
  picture.value = newPicture
}

const onImageOutPaintingSuccess = (newPicture: API.PictureVO) => {
  picture.value = newPicture
}

const handleReupload = () => {
  picture.value = undefined
  Object.assign(pictureForm, {
    name: '',
    introduction: '',
    category: '',
    tags: [],
    isDownload: 1
  })
  uploading.value = false
  uploadProgress.value = 0
  uploadType.value = 'file'
}

const handleSubmit = async () => {
  if (!picture.value?.id || submitting.value) return

  submitting.value = true
  try {
    // 确保 aiLabels 是字符串格式（后端 PictureEditRequest.aiLabels 是 String）
    const aiLabelsStr = Array.isArray(pictureForm.aiLabels)
      ? JSON.stringify(pictureForm.aiLabels)
      : (pictureForm.aiLabels || '')
    const res = await editPictureUsingPost({
      id: picture.value.id,
      spaceId: spaceId.value,
      ...pictureForm,
      aiLabels: aiLabelsStr
    })

    if (res.data.code === 0 && res.data.data) {
      const draftRes = await updatePictureDraftStatusUsingPost({
        pictureId: picture.value.id,
        isDraft: 0
      })

      if (draftRes.data.code === 0) {
        isCreated.value = true
        if (!spaceId.value) {
          // 检查是否已显示过审核提示
          const hasShownApprovalNotice = localStorage.getItem('hasShownApprovalNotice')

          if (!hasShownApprovalNotice) {
            Modal.confirm({
              title: '上传成功',
              content: '您的图片已成功上传到公共图库，审核通过后将展示在公共图库中',
              okText: '知道了',
              onOk: () => {
                // 记录用户已了解审核提示
                localStorage.setItem('hasShownApprovalNotice', 'true')
                router.push(`/picture-redirect/${picture.value.id}`)
              },
              onCancel: () => {
                router.push(`/picture-redirect/${picture.value.id}`)
              }
            })
          } else {
            // 如果已显示过提示，直接跳转
            router.push(`/picture-redirect/${picture.value.id}`)
          }
        } else {
          message.success('上传成功')
          router.push(`/space/${spaceId.value}`)
        }
      } else {
        message.error('发布失败：' + draftRes.data.message)
      }
    } else {
      message.error('发布失败：' + res.data.message)
    }
  } catch (error) {
    message.error('发布失败，请重试')
  } finally {
    submitting.value = false
  }
}

const saveAsDraft = async () => {
  if (!picture.value?.id) return message.error('请先上传图片')

  try {
    // 确保 aiLabels 是字符串格式（后端 PictureEditRequest.aiLabels 是 String）
    const aiLabelsStr = Array.isArray(pictureForm.aiLabels)
      ? JSON.stringify(pictureForm.aiLabels)
      : (pictureForm.aiLabels || '')
    const res = await editPictureUsingPost({
      id: picture.value.id,
      spaceId: spaceId.value,
      ...pictureForm,
      aiLabels: aiLabelsStr
    })

    if (res.data.code === 0) {
      const draftRes = await updatePictureDraftStatusUsingPost({
        pictureId: picture.value.id,
        isDraft: 1
      })
      if (draftRes.data.code === 0) {
        message.success('已存为草稿')
        router.push('/')
      } else {
        message.error('保存草稿失败：' + draftRes.data.message)
      }
    } else {
      message.error('保存草稿失败：' + res.data.message)
    }
  } catch (error) {
    message.error('保存草稿失败，请重试')
  }
}

const handleCancel = async () => {
  if (picture.value?.id && !isDeleted.value) {
    try {
      await deletePictureUsingPost({ id: picture.value.id })
      isDeleted.value = true
      message.success('已取消上传')
      picture.value = undefined
      Object.assign(pictureForm, {
        name: '',
        introduction: '',
        category: '',
        tags: [],
        isDownload: 1
      })
      uploading.value = false
      uploadProgress.value = 0
    } catch (error) {
      message.error('取消上传失败')
    }
  } else {
    picture.value = undefined
    Object.assign(pictureForm, {
      name: '',
      introduction: '',
      category: '',
      tags: [],
      isDownload: 1
    })
    uploading.value = false
    uploadProgress.value = 0
  }
}

const handleBack = () => {
  if (picture.value && !isCreated.value) {
    Modal.confirm({
      title: '确认离开',
      content: '当前图片尚未发布，离开后将不会保存，是否继续？',
      onOk: () => {
        router.back()
      },
    })
  } else {
    router.back()
  }
}
</script>

<style scoped>
.box {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: var(--background);
  color: var(--text-primary);
  z-index: 100;
  overflow-y: auto;
  transition: var(--theme-transition);
}


.publish-container {
  width: 100%;
  max-width: 1400px;
  height: 100%;
  overflow-y: auto;
  padding: 0 6px;
  height: 100vh;
}

.publish-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0; /* 12px -> 8px */
  border-bottom: 1px solid var(--border-color);
  position: sticky;
  top: 0;
  background-color: var(--header-background);
  backdrop-filter: blur(8px);
  z-index: 100;
}

.back-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: none;
  background-color: var(--hover-background);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #165dff;
  transition: var(--theme-transition);
}

.back-btn:hover {
  background-color: var(--nav-item-hover);
}

.header-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.publish-btn {
  padding: 12px 32px;
  border-radius: 30px;
  border: none;
  background: linear-gradient(135deg, #2ecc71 0%, #27ae60 100%);
  color: #ffffff;
  font-size: 18px;
  font-weight: 600;
  cursor: pointer;
  transition: var(--theme-transition);
  box-shadow: 0 4px 15px rgba(46, 204, 113, 0.3);
  letter-spacing: 0.5px;
}

.publish-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #27ae60 0%, #2ecc71 100%);
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(46, 204, 113, 0.4);
}

.publish-btn:active:not(:disabled) {
  transform: translateY(0);
  box-shadow: 0 4px 15px rgba(46, 204, 113, 0.3);
}

.publish-btn:disabled {
  background: #cccccc !important;
  cursor: not-allowed;
  transform: none !important;
  box-shadow: none !important;
}

.space-card {
  margin: 8px 0; /* 16px -> 8px */
  padding: 12px 16px; /* 略微压缩上下外边距和内边距 */
  background-color: var(--card-background);
  border-radius: 12px;
  box-shadow: 0 1px 3px var(--shadow-color);
  display: flex;
  align-items: center;
  gap: 12px;
  transition: var(--theme-transition);
}

.space-avatar {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  object-fit: cover;
}

.space-meta {
  flex: 1;
}

.space-name {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 4px 0;
  color: var(--text-primary);
}

.space-desc {
  font-size: 13px;
  color: var(--text-secondary);
  margin: 0;
}

.space-stats {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: var(--text-secondary);
}

.publish-main {
  padding: 0 0 24px;
  height: calc(100% - 120px);
}

.pc-layout {
  display: flex;
  gap: 32px;
  height: clamp(600px, 80vh, 1000px); /* 使用 clamp 确保最小和最大高度 */
  width: 100%;
}

.pc-left {
  flex: 0.6;
  height: 100%;
}

.pc-right {
  flex: 0.4;
  height: 100%;
}

.pc-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
  border-radius: 12px;
  overflow-y: auto; /* 允许垂直滚动 */
  overflow-x: hidden;
  padding-right: 4px; /* 为滚动条预留一点空间 */
  transition: var(--theme-transition);
}

/* PC 面板滚动条美化 */
.pc-panel::-webkit-scrollbar {
  width: 6px;
}

.pc-panel::-webkit-scrollbar-track {
  background: transparent;
}

.pc-panel::-webkit-scrollbar-thumb {
  background: var(--border-color);
  border-radius: 3px;
}

.pc-panel::-webkit-scrollbar-thumb:hover {
  background: #165dff;
}

.pc-layout .preview-wrapper {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.pc-layout .preview-img {
  width: 100%;
  height: calc(100% - 100px);
  object-fit: contain;
  cursor: pointer;
  border-radius: 8px;
  background-color: var(--hover-background);
  transition: var(--theme-transition);
}

.pc-layout .preview-actions {
  height: 80px; /* 100px -> 80px */
  display: flex;
  justify-content: space-around;
  align-items: center;
  border-top: 1px solid var(--border-color);
  margin-top: 12px;
  transition: var(--theme-transition);
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  border-radius: 20px;
  border: none;
  background-color: var(--hover-background);
  color: var(--text-primary);
  font-size: 14px;
  cursor: pointer;
  transition: var(--theme-transition);
}

.action-btn:hover {
  background-color: var(--nav-item-hover);
  color: #165dff;
}

.pc-layout .form-wrapper {
  padding: 16px 0; /* 32px -> 16px */
  flex: 1;
  display: flex;
  flex-direction: column;
}

.form-grid {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px; /* 24px -> 12px */
  margin-bottom: 12px; /* 24px -> 12px */
}

.form-grid .form-item {
  margin-bottom: 0;
}

.input-name {
  width: 100%;
  padding: 12px 20px; /* 16px -> 12px */
  border-radius: 8px;
  border: 1px solid var(--border-color);
  font-size: 15px;
  color: var(--text-primary);
  background-color: var(--card-background);
  outline: none;
  transition: var(--theme-transition);
}

.input-name:focus {
  border-color: #165dff;
}

.input-name::placeholder {
  color: var(--text-secondary);
}

.textarea-desc {
  width: 100%;
  padding: 12px 20px; /* 16px -> 12px */
  border-radius: 8px;
  border: 1px solid var(--border-color);
  font-size: 15px;
  color: var(--text-primary);
  background-color: var(--card-background);
  resize: none;
  outline: none;
  transition: var(--theme-transition);
  min-height: 140px; /* 180px -> 140px */
}

.textarea-desc:focus {
  border-color: #165dff;
}

.textarea-desc::placeholder {
  color: var(--text-secondary);
}

.select-item {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.select-label {
  font-size: 15px;
  color: var(--text-secondary);
  font-weight: 500;
}

.select-value {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px; /* 16px -> 12px */
  border-radius: 8px;
  border: 1px solid var(--border-color);
  font-size: 15px;
  color: var(--text-primary);
  background-color: var(--card-background);
  cursor: pointer;
  transition: var(--theme-transition);
}

.select-value:hover {
  border-color: #165dff;
}

.select-value i {
  font-size: 14px;
  color: var(--text-secondary);
}

.switch-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
}

.switch-label {
  font-size: 15px;
  color: var(--text-secondary);
  font-weight: 500;
}

.switch-btn {
  width: 50px;
  height: 28px;
  border-radius: 14px;
  background-color: var(--border-color);
  position: relative;
  cursor: pointer;
  transition: var(--theme-transition);
}

.switch-btn.active {
  background-color: #165dff;
}

.switch-dot {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background-color: var(--text-other);
  position: absolute;
  top: 2px;
  left: 2px;
  transition: left 0.2s, var(--theme-transition);
}

.switch-btn.active .switch-dot {
  left: 24px;
}

.form-actions {
  display: flex;
  gap: 16px;
  margin-top: auto;
}

.btn {
  flex: 1;
  padding: 12px; /* 16px -> 12px */
  border-radius: 8px;
  border: none;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: var(--theme-transition);
}

.draft-btn {
  background-color: var(--hover-background);
  color: #165dff;
}

.draft-btn:hover {
  background-color: var(--nav-item-hover);
}

.cancel-btn {
  background-color: var(--comment-drawer-background);
  color: var(--comment-delete-hover-color);
}

.cancel-btn:hover {
  background-color: var(--comment-item-hover);
}

.mobile-layout {
  width: 100%;
}

.upload-wrapper {
  margin-bottom: 24px;
}

.upload-area {
  background-color: var(--card-background);
  border-radius: 12px;
  box-shadow: 0 1px 3px var(--shadow-color);
  overflow: hidden;
  transition: var(--theme-transition);
}

.upload-tabs {
  display: flex;
  border-bottom: 1px solid var(--border-color);
  transition: var(--theme-transition);
}

.tab-item {
  flex: 1;
  padding: 12px;
  text-align: center;
  border: none;
  background-color: transparent;
  font-size: 14px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: var(--theme-transition);
}

.tab-item.active {
  color: #165dff;
  font-weight: 500;
  position: relative;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 30px;
  height: 2px;
  background-color: #165dff;
  border-radius: 2px;
}

.upload-content {
  padding: 24px;
}

.upload-progress {
  background-color: var(--card-background);
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 1px 3px var(--shadow-color);
  transition: var(--theme-transition);
}

.progress-bar {
  height: 6px;
  background-color: var(--hover-background);
  border-radius: 3px;
  overflow: hidden;
  margin-bottom: 12px;
  transition: var(--theme-transition);
}

.progress-fill {
  height: 100%;
  background-color: #165dff;
  transition: width 0.3s ease;
}

.progress-text {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0;
  text-align: center;
}

.mobile-layout .preview-wrapper {
  background-color: var(--card-background);
  border-radius: 12px;
  box-shadow: 0 1px 3px var(--shadow-color);
  overflow: hidden;
  transition: var(--theme-transition);
}

.mobile-layout .preview-img {
  width: 100%;
  height: auto;
  object-fit: cover;
  cursor: pointer;
}

.mobile-layout .preview-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding: 16px;
  border-top: 1px solid var(--border-color);
  transition: var(--theme-transition);
}

.modal-mask {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background-color: var(--comment-drawer-backdrop);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  transition: var(--theme-transition);
}

.modal-content {
  width: 90%;
  max-width: 400px;
  background-color: var(--card-background);
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 20px var(--shadow-color);
  transition: var(--theme-transition);
}

/* 分类弹窗专属样式 - 核心修改 */
.category-modal {
  max-height: 80vh; /* 限制弹窗最大高度为视口的80% */
  display: flex;
  flex-direction: column;
}

/* 分类弹窗滚动容器 */
.category-scroll-container {
  padding: 16px;
  max-height: calc(80vh - 140px); /* 减去header和search高度 */
  overflow-y: auto; /* 垂直滚动 */
  overflow-x: hidden; /* 隐藏水平滚动 */
  scrollbar-width: thin; /* 细滚动条（Firefox） */
}

/* 美化滚动条（Chrome/Safari） */
.category-scroll-container::-webkit-scrollbar {
  width: 6px;
}

.category-scroll-container::-webkit-scrollbar-track {
  background: var(--hover-background);
  border-radius: 3px;
}

.category-scroll-container::-webkit-scrollbar-thumb {
  background: var(--border-color);
  border-radius: 3px;
}

.category-scroll-container::-webkit-scrollbar-thumb:hover {
  background: #165dff;
}

/* 标签弹窗专属样式 - 核心修改 */
.tag-modal {
  max-height: 80vh; /* 限制弹窗最大高度为视口的80% */
  display: flex;
  flex-direction: column;
}

/* 标签弹窗滚动容器 */
.tag-scroll-container {
  padding: 16px;
  max-height: calc(80vh - 140px); /* 减去header和search高度 */
  overflow-y: auto; /* 垂直滚动 */
  overflow-x: hidden; /* 隐藏水平滚动 */
  scrollbar-width: thin; /* 细滚动条（Firefox） */
}

/* 美化滚动条（Chrome/Safari） */
.tag-scroll-container::-webkit-scrollbar {
  width: 6px;
}

.tag-scroll-container::-webkit-scrollbar-track {
  background: var(--hover-background);
  border-radius: 3px;
}

.tag-scroll-container::-webkit-scrollbar-thumb {
  background: var(--border-color);
  border-radius: 3px;
}

.tag-scroll-container::-webkit-scrollbar-thumb:hover {
  background: #165dff;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-bottom: 1px solid var(--border-color);
  transition: var(--theme-transition);
  flex-shrink: 0; /* 防止header被压缩 */
}

.modal-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.modal-close {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: none;
  background-color: var(--hover-background);
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: var(--theme-transition);
}

.modal-close:hover {
  background-color: var(--nav-item-hover);
  color: #165dff;
}

.modal-body {
  padding: 16px;
}

/* 搜索框样式 */
.modal-search {
  padding: 0 16px 16px;
  border-bottom: 1px solid var(--border-color);
}

.tag-search-wrapper {
  border-bottom: none;
}

.search-input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.search-icon {
  position: absolute;
  left: 12px;
  color: var(--text-secondary);
  font-size: 14px;
}

.modal-search-input {
  width: 100%;
  padding: 10px 12px 10px 36px;
  border-radius: 8px;
  border: 1px solid var(--border-color);
  font-size: 14px;
  color: var(--text-primary);
  background-color: var(--card-background);
  outline: none;
  transition: var(--theme-transition);
}

.modal-search-input:focus {
  border-color: #165dff;
}

.modal-search-input::placeholder {
  color: var(--text-secondary);
}

.search-clear-btn {
  position: absolute;
  right: 12px;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 12px;
  padding: 2px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.search-clear-btn:hover {
  color: #165dff;
}

/* 无结果提示 */
.no-result {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32px 16px;
  color: var(--text-secondary);
  text-align: center;
}

.no-result i {
  font-size: 24px;
  margin-bottom: 8px;
  opacity: 0.5;
}

.no-result p {
  font-size: 14px;
  margin: 0;
}

.tag-no-result {
  padding: 16px;
}

.category-item {
  padding: 12px 16px;
  font-size: 14px;
  color: var(--text-primary);
  border-radius: 8px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: var(--theme-transition);
}

.category-item:hover {
  background-color: var(--hover-background);
}

.category-item.active {
  background-color: var(--markdown-heading-blue-bg);
  color: #165dff;
  font-weight: 500;
}

.tag-input {
  width: 100%;
  padding: 12px 16px;
  border-radius: 8px;
  border: 1px solid var(--border-color);
  font-size: 14px;
  margin-bottom: 16px;
  outline: none;
  background-color: var(--card-background);
  color: var(--text-primary);
  transition: var(--theme-transition);
}

.tag-input:focus {
  border-color: #165dff;
}

.tag-input::placeholder {
  color: var(--text-secondary);
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.tag-item {
  padding: 6px 12px;
  border-radius: 16px;
  background-color: var(--hover-background);
  color: var(--text-primary);
  font-size: 13px;
  cursor: pointer;
  transition: var(--theme-transition);
}

.tag-item:hover {
  background-color: var(--nav-item-hover);
}

.tag-item.active {
  background-color: #165dff;
  color: var(--text-other);
}

.selected-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.selected-tag {
  padding: 6px 12px;
  border-radius: 16px;
  background-color: var(--markdown-heading-blue-bg);
  color: #165dff;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: var(--theme-transition);
}

.tag-close {
  border: none;
  background-color: transparent;
  color: #165dff;
  cursor: pointer;
  font-size: 12px;
  padding: 0;
  line-height: 1;
}

.preview-mask {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background-color: var(--comment-drawer-backdrop);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  transition: var(--theme-transition);
}

.preview-big-img {
  max-width: 90%;
  max-height: 90vh;
  object-fit: contain;
}

.preview-close {
  position: absolute;
  top: 20px;
  right: 20px;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: none;
  background-color: rgba(255, 255, 255, 0.2);
  color: var(--text-other);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: var(--theme-transition);
}

.preview-close:hover {
  background-color: rgba(255, 255, 255, 0.3);
}

/* 暗色模式下预览关闭按钮适配 */
.dark-theme .preview-close {
  background-color: rgba(0, 0, 0, 0.4);
}

.dark-theme .preview-close:hover {
  background-color: rgba(0, 0, 0, 0.6);
}

@media (max-width: 768px) {
  .form-wrapper {
    margin: 0 4px;
    padding-bottom: 12px;
  }
  .form-grid {
    grid-template-columns: 1fr;
  }

  .form-grid .form-item:nth-child(1),
  .form-grid .form-item:nth-child(2) {
    grid-column: 1;
  }

  .pc-left, .pc-right {
    flex: 1;
  }

  /* 移动端分类弹窗优化 */
  .category-modal {
    max-height: 70vh;
    width: 95%;
  }

  .category-scroll-container {
    max-height: calc(70vh - 140px);
  }
}

/* AI 检测面板样式 - Premium Design */

.ai-analysis-panel {
  background: rgba(22, 93, 255, 0.05);
  border: 1px solid rgba(22, 93, 255, 0.15);
  border-radius: 12px;
  padding: 12px; /* 16px -> 12px */
  margin-bottom: 8px;
  backdrop-filter: blur(10px);
}

.ai-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px; /* 12px -> 8px */
  color: #165dff;
  font-weight: 600;
  font-size: 14px;
}

.ai-header i {
  font-size: 16px;
  animation: sparkle 2s infinite;
}

@keyframes sparkle {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.7; transform: scale(1.1); }
}

.ai-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.ai-tag-chip {
  background: var(--card-background);
  color: #165dff;
  border: 1px solid rgba(22, 93, 255, 0.2);
  padding: 4px 12px;
  border-radius: 16px;
  font-size: 12px;
  font-weight: 500;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.02);
  transition: all 0.3s ease;
}

.ai-tag-chip:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(22, 93, 255, 0.1);
  border-color: #165dff;
}

.ai-warning {
  margin-top: 12px;
  display: flex;
  gap: 8px;
  padding: 10px;
  background: rgba(255, 77, 79, 0.05);
  border-radius: 8px;
  border: 1px solid rgba(255, 77, 79, 0.1);
}

.ai-warning i {
  color: #ff4d4f;
  font-size: 14px;
  margin-top: 2px;
}

.ai-warning p {
  margin: 0;
  font-size: 12px;
  color: #ff4d4f;
  line-height: 1.5;
  text-align: justify;
}

.mobile-ai-panel {
  margin: 0;
  border-radius: 12px;
}

.pc-ai-panel {
  margin-top: 16px;
  background: rgba(22, 93, 255, 0.03);
}

.ai-more-btn {
  margin-top: 12px;
  width: 100%;
  padding: 8px;
  border: 1px dashed rgba(22, 93, 255, 0.3);
  background: rgba(22, 93, 255, 0.02);
  color: #165dff;
  border-radius: 8px;
  font-size: 13px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  transition: all 0.3s ease;
}

.ai-more-btn:hover {
  background: rgba(22, 93, 255, 0.08);
  border-style: solid;
}

/* YOLO Modal Styles */
.yolo-modal {
  max-width: 800px;
  width: 90%;
}

.yolo-modal-body {
  padding: 0;
  background: #000;
  min-height: 300px;
  display: flex;
  flex-direction: column;
}

.yolo-loading {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #fff;
  gap: 16px;
  padding: 40px;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid rgba(255, 255, 255, 0.1);
  border-top-color: #165dff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.yolo-result-container {
  display: flex;
  flex-direction: column;
}

.yolo-annotated-img {
  width: 100%;
  max-height: 60vh;
  object-fit: contain;
}

.yolo-info-panel {
  background: var(--card-background);
  padding: 16px;
  border-top: 1px solid var(--border-color);
}

.yolo-stat-item {
  margin-bottom: 12px;
  font-weight: 600;
  color: var(--text-primary);
}

.yolo-detection-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.yolo-det-item {
  background: var(--hover-background);
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
  display: flex;
  gap: 6px;
}

.det-label {
  color: var(--text-primary);
  font-weight: 500;
}

.det-conf {
  color: #27ae60;
}
</style>
