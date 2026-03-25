<template>
  <div class="markdown-content" :class="{ 'mobile-view': isMobile }">
    <div class="content-wrapper">
      <div v-for="(block, index) in blocks" :key="index">
        <div v-if="block.type === 'text'" class="text-block" v-html="block.content"></div>
        <div v-else-if="block.type === 'image-group'" class="image-wrapper">
          <div class="image-grid" :class="getGridClass(block.images?.length || 0)">
            <!-- PC端单张图片特殊处理 -->
            <div
              v-if="!isMobile && block.images?.length === 1"
              class="single-image-container"
              @click="handleImageClick(index, 0)"
            >
              <img
                :src="block.images[0].src"
                class="single-image"
                :alt="block.images[0].alt"
                @load="handleSingleImageLoad(block.images[0].src)"
              />
              <!-- PC端单张图片悬停效果 -->
              <div class="pc-image-overlay single-overlay">
                <div class="image-count">
                  <span>1/1</span>
                </div>
              </div>
            </div>

            <!-- 多张图片/移动端单张图片 -->
            <div
              v-else
              v-for="(img, imgIndex) in block.images"
              :key="imgIndex"
              :class="['grid-item', { 'use-img': isMobile && (block.images?.length || 0) === 1 }]"
              :style="(isMobile && (block.images?.length || 0) === 1) ? undefined : { backgroundImage: `url(${img.src})` }"
              @click="handleImageClick(index, imgIndex)"
            >
              <img
                v-if="isMobile && (block.images?.length || 0) === 1"
                :src="img.src"
                class="grid-img"
                @click.stop="handleImageClick(index, imgIndex)"
              />
              <!-- PC端图片悬停效果 -->
              <div v-if="!isMobile" class="pc-image-overlay">
                <div class="image-count">
                  <span>{{ imgIndex + 1 }}/{{ block.images?.length || 0 }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <template v-if="!isMobile">
      <AImage
        v-show="previewVisible && !isMobile"
        :src="previewImageUrl"
        :preview="{
          visible: previewVisible,
          onVisibleChange: handlePreviewChange,
          maskClassName: 'preview-mask'
        }"
        alt=""
        style="display: none;"
      />
      <!-- PC端预览导航按钮 -->
      <Teleport to="body">
        <div v-if="previewVisible && !isMobile" class="pc-preview-container">
          <div class="pc-preview-info">
            <div class="pc-preview-content">
              <h3>{{ currentImageAlt }}</h3>
              <p>{{ currentImageIndex + 1 }} / {{ allImages.length }}</p>
            </div>
          </div>
          <!-- 上一张/下一张按钮 -->
          <button
            v-if="currentImageIndex > 0"
            class="pc-nav-button prev"
            @click="showPreviousImage"
          >
            <i class="fas fa-chevron-left"></i>
          </button>
          <button
            v-if="currentImageIndex < allImages.length - 1"
            class="pc-nav-button next"
            @click="showNextImage"
          >
            <i class="fas fa-chevron-right"></i>
          </button>
        </div>
      </Teleport>
    </template>

    <van-image-preview
      v-model:show="previewVisible"
      :images="allImages"
      :start-position="currentImageIndex"
      @change="onPreviewChange"
      :closeable="true"
      :show-index="true"
      v-if="isMobile"
    >
    </van-image-preview>
  </div>
</template>

<script setup lang="ts">
// @ts-nocheck
import { ref, onMounted, watch, onUnmounted, nextTick } from 'vue'
import { Image as AImage } from 'ant-design-vue'
import { ImagePreview as VanImagePreview } from 'vant'
import 'vant/lib/image-preview/style'
import { getDeviceType } from '@/utils/device'
import { DEVICE_TYPE_ENUM } from '@/constants/device'

const props = defineProps<{
  content: string
}>()

const isMobile = ref(false)
// 存储单张图片的尺寸信息
const singleImageRatio = ref<number>(1) // 宽高比

onMounted(async () => {
  const deviceType = await getDeviceType()
  isMobile.value = deviceType === DEVICE_TYPE_ENUM.MOBILE
})

// 处理单张图片加载完成，计算宽高比
const handleSingleImageLoad = (src: string) => {
  const img = new Image()
  img.onload = () => {
    singleImageRatio.value = img.width / img.height
  }
  img.src = src
}

interface ImageItem {
  src: string
  alt: string
}

interface ContentBlock {
  type: 'text' | 'image-group'
  content?: string
  images?: ImageItem[]
}

const blocks = ref<ContentBlock[]>([])
const previewVisible = ref(false)
const previewImageUrl = ref('')
const currentImageAlt = ref('')
const allImages = ref<string[]>([])
const currentImageIndex = ref(0)

const parseContent = (content: string) => {
  const result: ContentBlock[] = []
  const lines = content.split('\n')
  let currentText = ''
  let currentImages: ImageItem[] = []

  lines.forEach(line => {
    const imgMatch = line.match(/!\[(.*?)\]\((.*?)\)/)
    if (imgMatch) {
      if (currentText) {
        result.push({ type: 'text', content: currentText.trim() })
        currentText = ''
      }
      currentImages.push({
        alt: imgMatch[1],
        src: imgMatch[2]
      })
    } else {
      if (line.trim()) {
        if (currentImages.length > 0) {
          result.push({
            type: 'image-group',
            images: [...currentImages]
          })
          currentImages = []
        }
        currentText += line + '\n'
      }
    }
  })

  if (currentImages.length > 0) {
    result.push({
      type: 'image-group',
      images: currentImages
    })
  }
  if (currentText.trim()) {
    result.push({ type: 'text', content: currentText.trim() })
  }

  const mergedResult: ContentBlock[] = []
  let currentGroup: ImageItem[] = []

  result.forEach((block, index) => {
    if (block.type === 'image-group') {
      currentGroup.push(...(block.images || []))
    } else {
      if (currentGroup.length > 0) {
        mergedResult.push({
          type: 'image-group',
          images: currentGroup
        })
        currentGroup = []
      }
      mergedResult.push(block)
    }
  })

  if (currentGroup.length > 0) {
    mergedResult.push({
      type: 'image-group',
      images: currentGroup
    })
  }

  blocks.value = mergedResult
}

const getGridClass = (count: number) => {
  const baseClass = count === 1 ? 'single' : ''
  const countClass = {
    1: 'one',
    2: 'two',
    3: 'three',
    4: 'four',
    6: 'six',
    9: 'nine'
  }[count] || 'default'

  return `${baseClass} ${countClass}`
}

const getAllImages = () => {
  const images: string[] = []
  blocks.value.forEach(block => {
    if (block.type === 'image-group' && block.images) {
      block.images.forEach(img => {
        images.push(img.src.replace('_thumbnail', ''))
      })
    }
  })
  return images
}

const getAllImagesWithAlt = () => {
  const imagesWithAlt: ImageItem[] = []
  blocks.value.forEach(block => {
    if (block.type === 'image-group' && block.images) {
      block.images.forEach(img => {
        imagesWithAlt.push({
          src: img.src.replace('_thumbnail', ''),
          alt: img.alt || ''
        })
      })
    }
  })
  return imagesWithAlt
}

const handleImageClick = async (blockIndex: number, imgIndex: number) => {
  const allImagesWithAlt = getAllImagesWithAlt()
  allImages.value = allImagesWithAlt.map(img => img.src)

  let globalIndex = 0
  for (let i = 0; i < blockIndex; i++) {
    if (blocks.value[i].type === 'image-group' && blocks.value[i].images) {
      globalIndex += blocks.value[i].images!.length
    }
  }
  globalIndex += imgIndex

  currentImageIndex.value = globalIndex
  previewImageUrl.value = allImages.value[globalIndex]
  currentImageAlt.value = allImagesWithAlt[globalIndex]?.alt || ''

  previewVisible.value = true

  if (!isMobile.value) {
    document.addEventListener('keydown', handleKeyDown)
    document.body.style.overflow = 'hidden'
  }
}

const handleKeyDown = (e: KeyboardEvent) => {
  if (!previewVisible.value || isMobile.value) return

  if (e.key === 'ArrowLeft') {
    e.preventDefault()
    showPreviousImage()
  } else if (e.key === 'ArrowRight') {
    e.preventDefault()
    showNextImage()
  } else if (e.key === 'Escape') {
    e.preventDefault()
    closePreview()
  }
}

const handlePreviewChange = (visible: boolean) => {
  previewVisible.value = visible
  if (!visible) {
    document.body.style.overflow = ''
    document.removeEventListener('keydown', handleKeyDown)
  }
}

const closePreview = () => {
  previewVisible.value = false
  document.body.style.overflow = ''
  document.removeEventListener('keydown', handleKeyDown)
}

const showPreviousImage = async (e?: Event) => {
  if (e) {
    e.stopPropagation()
  }
  if (currentImageIndex.value > 0) {
    currentImageIndex.value--
    previewImageUrl.value = allImages.value[currentImageIndex.value]
    updateCurrentImageAlt()
  }
}

const showNextImage = async (e?: Event) => {
  if (e) {
    e.stopPropagation()
  }
  if (currentImageIndex.value < allImages.value.length - 1) {
    currentImageIndex.value++
    previewImageUrl.value = allImages.value[currentImageIndex.value]
    updateCurrentImageAlt()
  }
}

const updateCurrentImageAlt = () => {
  const allImagesWithAlt = getAllImagesWithAlt()
  currentImageAlt.value = allImagesWithAlt[currentImageIndex.value]?.alt || ''
}

const onPreviewChange = (index: number) => {
  currentImageIndex.value = index
  updateCurrentImageAlt()
}

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeyDown)
})

watch(() => props.content, (newContent) => {
  if (newContent) {
    parseContent(newContent)
  }
}, { immediate: true })
</script>

<style scoped>
.markdown-content, .markdown-content * {
  color: var(--text-primary) !important;
  font-family: var(--font-family-base) !important;
  transition: var(--theme-transition);
}

.content-wrapper {
  max-width: 850px;
  margin: 0 auto;
  padding: 6px 2px;
}

/* 强制正文字体样式 */
.content-wrapper :deep(p),
.content-wrapper :deep(div) {
  font-size: 18px !important;
  line-height: 2.0 !important;
  letter-spacing: 0.5px !important;
  color: var(--text-primary) !important;
}

.content-wrapper :deep(h2) {
  font-size: 42px !important;
  font-weight: 700 !important;
  line-height: 2 !important;
  padding: 0 !important;
  border: none !important;
  background: none !important;
  color: var(--markdown-heading-text) !important;
}

.content-wrapper :deep(h3) {
  font-size: 18px !important;
  font-weight: 700 !important;
  line-height: 2 !important;
  margin: 30px 0 16px !important;
  padding: 0 !important;
  border: none !important;
  background: none !important;
  color: var(--markdown-heading-text) !important;
}

.content-wrapper :deep(h4) {
  font-size: 38px !important;
  font-weight: 700 !important;
  line-height: 2 !important;
  margin: 28px 0 14px !important;
  padding: 0 !important;
  border: none !important;
  background: none !important;
  color: var(--markdown-heading-text) !important;
}

.content-wrapper :deep(ul),
.content-wrapper :deep(ol) {
  font-size: 18px !important;
  line-height: 2.2 !important;
  margin: 24px 0 24px 18px !important;
  padding: 0 !important;
  color: var(--text-primary) !important;
}

.content-wrapper :deep(li) {
  margin: 16px 0 !important;
  color: var(--text-primary) !important;
}

.content-wrapper :deep(a) {
  color: var(--link-color) !important;
  text-decoration: none !important;
  font-size: inherit !important;
}

.content-wrapper :deep(a:hover) {
  text-decoration: underline !important;
  color: var(--link-hover-color) !important;
}

.image-wrapper {
  margin: 32px 0;
}

/* ========== PC端单张图片核心优化 ========== */
.single-image-container {
  position: relative;
  width: 100%;
  max-width: 600px; /* 限制最大宽度，避免过宽 */
  margin: 0 auto;
  cursor: zoom-in;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  /* 完全移除背景和边框 */
  background: transparent !important;
  border: none !important;
}

/* 根据图片宽高比动态调整高度 */
.single-image {
  display: block;
  width: 100%;
  height: auto;
  max-height: 80vh;
  object-fit: contain;
  border-radius: 8px;
}

.single-image-container:hover {
  transform: translateY(-6px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.12);
}

/* PC端单张图片覆盖层优化 */
.single-overlay {
  border-radius: 8px;
}

/* ========== PC端多张图片网格样式 ========== */
.image-grid {
  display: grid;
  gap: 2px;
  width: 100%;
}

/* PC端不同数量图片的网格布局 */
.image-grid.one {
  grid-template-columns: 1fr;
  max-width: 100%; /* 单张图片不再限制宽度 */
  margin: 0 auto;
}

.image-grid.two {
  grid-template-columns: repeat(2, 1fr);
  gap: 4px;
}

.image-grid.three {
  grid-template-columns: repeat(3, 1fr);
}

.image-grid.four {
  grid-template-columns: repeat(2, 1fr);
  grid-template-rows: repeat(2, 1fr);
  gap: 4px;
}

.image-grid.six {
  grid-template-columns: repeat(3, 1fr);
  grid-template-rows: repeat(2, 1fr);
}

.image-grid.nine {
  grid-template-columns: repeat(3, 1fr);
  grid-template-rows: repeat(3, 1fr);
}

.image-grid.default {
  grid-template-columns: repeat(3, 1fr);
}

/* PC端多张图片项样式 */
.grid-item {
  position: relative;
  width: 100%;
  height: 320px; /* 增高图片容器 */
  background-repeat: no-repeat;
  background-position: center;
  background-size: cover;
  border-radius: 12px;
  cursor: zoom-in;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  background-color: transparent !important; /* 移除背景色 */
  border: 1px solid rgba(0, 0, 0, 0.08);
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

/* PC端图片悬停效果 */
.grid-item:hover {
  transform: translateY(-6px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.12);
  border-color: rgba(0, 0, 0, 0.12);
}

/* PC端图片覆盖层 */
.pc-image-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.6), transparent 40%);
  opacity: 0;
  transition: opacity 0.3s ease;
  display: flex;
  align-items: flex-end;
  justify-content: flex-end;
  padding: 16px;
  pointer-events: none;
}

.grid-item:hover .pc-image-overlay,
.single-image-container:hover .pc-image-overlay {
  opacity: 1;
}

.image-count {
  background: rgba(0, 0, 0, 0.7);
  color: white !important;
  padding: 4px 12px;
  border-radius: 16px;
  font-size: 14px;
  font-weight: 500;
}

/* ========== 移动端样式（保持不变） ========== */
@media (max-width: 768px) {
  .content-wrapper {
    max-width: 100%;
    padding: 4px 4px;
  }

  .content-wrapper :deep(p),
  .content-wrapper :deep(div) {
    font-size: 18px !important;
  }

  .content-wrapper :deep(h2) {
    font-size: 38px !important;
  }

  .content-wrapper :deep(h3) {
    font-size: 18px !important;
  }

  .content-wrapper :deep(h4) {
    font-size: 34px !important;
  }

  .content-wrapper :deep(ul),
  .content-wrapper :deep(ol) {
    font-size: 18px !important;
    margin: 22px 0 22px 18px !important;
  }

  .image-grid.four .grid-item,
  .image-grid.six .grid-item,
  .image-grid.nine .grid-item {
    padding-top: 100%;
    height: 0;
    background-size: cover;
  }

  /* 移动端单张图片容器样式 */
  .single-image-container {
    max-width: 100%;
  }
}

.grid-item.use-img {
  height: auto !important;
  padding-top: 0 !important;
  background: none;
  border: none;
}

.grid-img {
  display: block;
  width: 100%;
  height: auto;
  border-radius: 8px;
  border: 1px solid var(--border-color);
}

.mobile-view .grid-item {
  background-size: cover;
}

.mobile-view .grid-img {
  border-radius: 8px;
  padding: 0;
}

/* ========== PC端预览样式优化 ========== */
.pc-preview-container {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1000;
}

.pc-preview-info {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1000;
}

.pc-preview-content {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 24px;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.9), transparent);
  color: white !important;
  text-align: center;
}

.pc-preview-content h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: white !important;
}

.pc-preview-content p {
  margin: 0;
  font-size: 16px;
  opacity: 0.9;
  color: white !important;
}

/* PC端导航按钮优化 */
.pc-nav-button {
  position: fixed;
  top: 50%;
  transform: translateY(-50%);
  width: 56px;
  height: 56px;
  border: none;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: white !important;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  cursor: pointer;
  transition: all 0.3s ease;
  z-index: 1002;
  font-size: 20px;
}

.pc-nav-button.prev {
  left: 32px;
}

.pc-nav-button.next {
  right: 32px;
}

.pc-nav-button:hover {
  transform: translateY(-50%) scale(1.1);
  background: rgba(0, 0, 0, 0.7);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
}

:deep(.ant-image-preview-mask) {
  z-index: 1000 !important;
  background: rgba(0, 0, 0, 0.9) !important;
}

:deep(.ant-image-preview-wrap) {
  z-index: 1001 !important;
}

@media screen and (min-width: 768px) and (max-width: 1200px) {
  .markdown-content .content-wrapper {
    padding: 22px 20px;
    max-width: 750px;
  }
  .markdown-content .image-wrapper {
    margin-left: 0;
    margin-right: 0;
  }
  .markdown-content .grid-item {
    box-sizing: border-box;
    height: 260px;
  }
  .single-image-container {
    max-width: 500px;
  }
}
</style>
