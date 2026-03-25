<template>
  <div class="html-content" :class="{ 'mobile-view': isMobile }">
    <div class="content-wrapper">
      <div
        v-for="(item, index) in contentItems"
        :key="index"
        :class="item.type === 'text' ? 'text-block' : 'image-block'"
      >
        <div v-if="item.type === 'text'" v-html="item.content"></div>
        <div v-else class="image-wrapper">
          <div class="image-grid" :class="getGridClass(item.images.length)">
            <div
              v-for="(img, imgIdx) in item.images"
              :key="`img-${index}-${imgIdx}`"
              class="grid-item"
              @click="handleImageClick(index, imgIdx)"
            >
              <div class="image-loader">
                <div class="image-placeholder"></div>
                <img
                  :src="img.src"
                  :alt="img.alt"
                  class="grid-img"
                  loading="lazy"
                  @load="setImageLoaded(index, imgIdx, true)"
                  :class="{ 'img-loaded': getImageLoaded(index, imgIdx) }"
                />
              </div>
              <div v-if="!isMobile" class="pc-image-overlay">
                <div class="image-count">
                  <span>{{ imgIdx + 1 }}/{{ item.images.length }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <teleport to="body">
      <van-image-preview
        v-model:show="previewVisible"
        :images="previewImages"
        :start-position="previewStartIndex"
        loop
        show-index
        :show-indicators="true"
        @close="onPreviewClose"
        class="mobile-fullscreen-preview"
        v-if="isMobile"
      ></van-image-preview>

      <a-image
        v-else
        :src="previewImages[previewStartIndex]"
        :preview="{
          visible: previewVisible,
          onVisibleChange: handlePreviewChange,
          maskClassName: 'preview-mask'
        }"
        alt=""
        style="display: none;"
      />
    </teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, onUnmounted } from 'vue'
import { ImagePreview } from 'vant'
import { Image as AImage } from 'ant-design-vue'
import 'vant/es/image-preview/style'

const getDeviceType = () => {
  return new Promise<string>((resolve) => {
    resolve(window.innerWidth < 768 ? 'mobile' : 'pc')
  })
}

const props = defineProps<{
  content: string
}>()

const isMobile = ref(false)
const contentItems = ref<Array<{
  type: 'text' | 'image'
  content?: string
  images?: Array<{ src: string; alt: string }>
}>>([])
const allImagesWithAlt = ref<Array<{ src: string; alt: string }>>([])
const imageLoadedStates = ref<Record<string, boolean>>({})

const previewVisible = ref(false)
const previewImages = ref<string[]>([])
const previewStartIndex = ref(0)

onMounted(async () => {
  const deviceType = await getDeviceType()
  isMobile.value = deviceType === 'mobile'
  parseHtmlContent(props.content)

  if (!isMobile.value) {
    document.addEventListener('keydown', handleKeyDown)
  }
})

const handleKeyDown = (e: KeyboardEvent) => {
  if (!previewVisible.value || isMobile.value) return
  e.preventDefault()
  if (e.key === 'ArrowLeft') showPreviousImage()
  else if (e.key === 'ArrowRight') showNextImage()
  else if (e.key === 'Escape') onPreviewClose()
}

const handlePreviewChange = (visible: boolean) => {
  previewVisible.value = visible
  if (!visible) {
    document.body.style.overflow = ''
    document.removeEventListener('keydown', handleKeyDown)
  }
}

const showPreviousImage = () => {
  previewStartIndex.value = previewStartIndex.value > 0 ? previewStartIndex.value - 1 : previewImages.value.length - 1
}

const showNextImage = () => {
  previewStartIndex.value = previewStartIndex.value < previewImages.value.length - 1 ? previewStartIndex.value + 1 : 0
}

const parseHtmlContent = (htmlContent: string) => {
  if (!htmlContent) {
    contentItems.value = []
    allImagesWithAlt.value = []
    previewImages.value = []
    return
  }

  // 核心修复：将后端敏感词过滤产生的非法标签还原为标准段落标签
  // 这样既能恢复换行，又能通过标准 DOMParser 正确解析
  let repairedHtml = htmlContent
    .replace(/<(\*+)> /g, '<p>')     // 处理带空格的 <***>
    .replace(/<(\*+)>/g, '<p>')      // 处理标准 <***>
    .replace(/<\/(\*+)>/g, '</p>')   // 处理闭合 </***>
    .replace(/\n/g, '<br/>')         // 将原始换行符转为 HTML 换行

  const tempDiv = document.createElement('div')
  tempDiv.innerHTML = repairedHtml

  const result: Array<{ type: 'text' | 'image'; content?: string; images?: Array<{ src: string; alt: string }> }> = []
  const flatImages: Array<{ src: string; alt: string }> = []
  const imageSrcSet = new Set<string>()

  let currentTextBuffer = ''
  const flushTextBuffer = () => {
    // 移除前导和尾随的 <br> 标签，防止与块级 Margins 叠加产生过多空隙
    const cleaned = currentTextBuffer.replace(/^(<br\s*\/?>)+|(<br\s*\/?>)+$/gi, '')
    if (cleaned.trim().length > 0) {
      result.push({ type: 'text', content: cleaned })
    }
    currentTextBuffer = ''
  }

  // 深度优先遍历所有节点，按顺序提取文本和图片
  const walk = (node: Node) => {
    if (node.nodeType === Node.TEXT_NODE) {
      // 保留原始空白，不使用 trim()
      currentTextBuffer += node.textContent || ''
    } else if (node.nodeType === Node.ELEMENT_NODE) {
      const element = node as HTMLElement
      const tagName = element.tagName.toLowerCase()

      // 如果是图片标签
      if (tagName === 'img') {
        flushTextBuffer() // 遇到图片前，先刷新文本缓冲区
        let src = element.getAttribute('src') || ''
        if (src) {
          src = src.replace('_thumbnail', '')
          if (!imageSrcSet.has(src)) {
            imageSrcSet.add(src)
            const alt = element.getAttribute('alt') || `图片${flatImages.length + 1}`
            const imgObj = { src, alt }

            const lastItem = result[result.length - 1]
            if (lastItem && lastItem.type === 'image' && lastItem.images) {
              lastItem.images.push(imgObj)
            } else {
              result.push({ type: 'image', images: [imgObj] })
            }
            flatImages.push(imgObj)
          }
        }
      }
      // 行内辅助标签累加到文本缓冲区
      else if (['br', 'span', 'a', 'strong', 'em', 'u', 'b', 'i', 'font'].indexOf(tagName) !== -1) {
        currentTextBuffer += element.outerHTML
      }
      // 容器节点处理
      else {
        const hasImg = element.querySelector('img')
        if (!hasImg) {
          // 纯文本容器且不包含图片，作为一个整体处理
          flushTextBuffer()
          const content = element.outerHTML
          if (content.trim()) {
            result.push({ type: 'text', content: content })
          }
        } else {
          // 混合容器（包含图片），先刷新缓冲区并递归处理子项
          flushTextBuffer()
          Array.from(element.childNodes).forEach(walk)
          flushTextBuffer() // 结束后再次确保内容存入
        }
      }
    }
  }

  Array.from(tempDiv.childNodes).forEach(walk)
  flushTextBuffer() // 处理最后的剩余文本

  // 更新内容
  contentItems.value = result
  allImagesWithAlt.value = flatImages
  previewImages.value = flatImages.map(img => img.src)
}

const getGridClass = (count: number) => {
  const effectiveCount = count > 9 ? 9 : count
  const classMap: Record<number, string> = {
    1: 'grid-1',
    2: 'grid-2',
    3: 'grid-3',
    4: 'grid-4',
    6: 'grid-6',
    9: 'grid-9'
  }
  return classMap[effectiveCount] || 'grid-3'
}

const setImageLoaded = (itemIndex: number, imgIdx: number, loaded: boolean) => {
  imageLoadedStates.value[`${itemIndex}-${imgIdx}`] = loaded
}

const getImageLoaded = (itemIndex: number, imgIdx: number) => {
  return imageLoadedStates.value[`${itemIndex}-${imgIdx}`] || false
}

const handleImageClick = (itemIndex: number, imgIdx: number) => {
  let globalIndex = 0
  for (let i = 0; i < itemIndex; i++) {
    const item = contentItems.value[i]
    if (item.type === 'image' && item.images) globalIndex += item.images.length
  }
  globalIndex += imgIdx
  previewStartIndex.value = globalIndex
  previewVisible.value = true
  document.documentElement.style.overflow = 'hidden'
  document.body.style.overflow = 'hidden'
  document.body.style.position = 'fixed'
  document.body.style.width = '100%'
}

const onPreviewClose = () => {
  previewVisible.value = false
  document.documentElement.style.overflow = ''
  document.body.style.overflow = ''
  document.body.style.position = ''
  document.body.style.width = ''
}

watch(() => props.content, (newContent) => {
  if (newContent) {
    parseHtmlContent(newContent)
    imageLoadedStates.value = {}
  }
}, { immediate: true })

onUnmounted(() => {
  previewVisible.value = false
  document.documentElement.style.overflow = ''
  document.body.style.overflow = ''
  document.body.style.position = ''
  document.body.style.width = ''
  if (!isMobile.value) document.removeEventListener('keydown', handleKeyDown)
})
</script>

<style scoped>
.html-content {
  width: 100%;
  box-sizing: border-box;
  color: var(--text-primary);
  font-family: var(--font-family-base);
  transition: var(--theme-transition);
}

.content-wrapper {
  max-width: 880px;
  margin: 0 auto;
}

.text-block {
  line-height: 1.85;
  font-size: 18px;
  margin-bottom: 24px;
  color: var(--post-text-color);
  word-break: break-word;
}

.image-block {
  margin-bottom: 24px;
  width: 100%;
}

.text-block:last-child,
.image-block:last-child {
  margin-bottom: 0;
}

.image-wrapper {
  width: 100%;
}

.image-grid {
  display: grid;
  gap: 16px;
  width: 100%;
}

.grid-1 {
  grid-template-columns: 1fr;
  max-width: 750px;
  margin: 0 auto;
}

.grid-2 {
  grid-template-columns: repeat(2, 1fr);
}

.grid-3 {
  grid-template-columns: repeat(3, 1fr);
}

.grid-4 {
  grid-template-columns: repeat(2, 1fr);
  grid-template-rows: repeat(2, 1fr);
}

.grid-6 {
  grid-template-columns: repeat(3, 1fr);
  grid-template-rows: repeat(2, 1fr);
}

.grid-9 {
  grid-template-columns: repeat(3, 1fr);
  grid-template-rows: repeat(3, 1fr);
}

.grid-item {
  position: relative;
  aspect-ratio: 1/1;
  border-radius: 16px;
  cursor: zoom-in;
  transition: all 0.4s cubic-bezier(0.25, 0.8, 0.25, 1);
  overflow: hidden;
  pointer-events: auto;
  z-index: 1;
}

.grid-item:hover {
  transform: translateY(-8px);
}

.image-loader {
  position: relative;
  width: 100%;
  height: 100%;
}

.grid-img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0;
  transition: opacity 0.8s ease-in-out;
  z-index: 2;
}

.grid-img.img-loaded {
  opacity: 1;
}

.image-placeholder {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-size: 200% 100%;
  animation: placeholderShimmer 1.5s infinite linear;
  z-index: 1;
}

@keyframes placeholderShimmer {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

.pc-image-overlay {
  position: absolute;
  bottom: 16px;
  right: 16px;
  background: rgba(0, 0, 0, 0.6);
  color: var(--text-other);
  font-size: 14px;
  padding: 6px 10px;
  border-radius: 12px;
  opacity: 0;
  transition: opacity 0.3s ease;
  z-index: 3;
}

.grid-item:hover .pc-image-overlay {
  opacity: 1;
}

.mobile-view .content-wrapper {
  max-width: 100%;
  padding: 8px 4px;
}

.mobile-view .text-block {
  font-size: 17px;
  margin: 16px 0;
}

.mobile-view .image-grid {
  gap: 8px;
}

.mobile-view .grid-item {
  border-radius: 12px;
}

.mobile-view .grid-item:hover {
  transform: none;
}

:deep(.mobile-fullscreen-preview) {
  position: fixed !important;
  top: 0 !important;
  left: 0 !important;
  right: 0 !important;
  bottom: 0 !important;
  width: 100vw !important;
  height: 100vh !important;
  z-index: 999999 !important;
  margin: 0 !important;
  padding: 0 !important;
  max-width: none !important;
  max-height: none !important;
  background: #000 !important;
}

:deep(.mobile-fullscreen-preview .van-image-preview__wrapper) {
  width: 100vw !important;
  height: 100vh !important;
  padding: env(safe-area-inset-top) env(safe-area-inset-right) env(safe-area-inset-bottom) env(safe-area-inset-left) !important;
  box-sizing: border-box !important;
}

:deep(.mobile-fullscreen-preview .van-image-preview__container) {
  width: 100% !important;
  height: 100% !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
}

:deep(.mobile-fullscreen-preview .van-image-preview__img) {
  max-width: 100% !important;
  max-height: 100% !important;
  width: auto !important;
  height: auto !important;
  object-fit: contain !important;
}

:deep(.mobile-fullscreen-preview .van-image-preview__index) {
  position: fixed !important;
  top: env(safe-area-inset-top, 20px) !important;
  left: 50% !important;
  transform: translateX(-50%) !important;
  background: rgba(0,0,0,0.7) !important;
  color: #fff !important;
  border-radius: 16px !important;
  padding: 4px 12px !important;
  z-index: 999999 !important;
}

:deep(.mobile-fullscreen-preview .van-image-preview__close) {
  position: fixed !important;
  top: env(safe-area-inset-top, 20px) !important;
  right: env(safe-area-inset-right, 20px) !important;
  width: 44px !important;
  height: 44px !important;
  z-index: 999999 !important;
  color: #fff !important;
}

:deep(.mobile-fullscreen-preview .van-image-preview__indicator) {
  background: rgba(255,255,255,0.5) !important;
  margin: 0 4px !important;
}

:deep(.mobile-fullscreen-preview .van-image-preview__indicator-active) {
  background: #fff !important;
}

:deep(.ant-image-preview-mask) {
  z-index: 999998 !important;
  background: #000 !important;
}

:deep(.ant-image-preview-wrap) {
  z-index: 999999 !important;
}

:deep(.ant-image-preview-img-wrapper) {
  background-color: #000;
}

:deep(.ant-image-preview-switch-left),
:deep(.ant-image-preview-switch-right) {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.7);
  z-index: 1000000 !important;
  pointer-events: auto;
}

:deep(.ant-image-preview-switch-left:hover),
:deep(.ant-image-preview-switch-right:hover) {
  background: rgba(0, 0, 0, 0.9);
}

:deep(.ant-image-preview-close) {
  z-index: 1000000 !important;
  pointer-events: auto;
}
</style>
