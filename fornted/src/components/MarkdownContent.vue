<template>
  <div class="markdown-content" :class="{ 'mobile-view': isMobile }">
    <div class="content-wrapper">
      <div v-for="(block, index) in blocks" :key="index">
        <!-- 文本块 -->
        <div v-if="block.type === 'text'" class="text-block" v-html="block.content"></div>
        <!-- 图片块 -->
        <div v-else-if="block.type === 'image-group'" class="image-wrapper">
          <div class="image-grid" :class="getGridClass(block.images?.length || 0)">
            <div
              v-for="(img, imgIndex) in block.images"
              :key="imgIndex"
              class="grid-item"
            >
              <img
                :src="img.src"
                :alt="img.alt"
                class="grid-image"
                @click="handleImageClick(img.src)"
              />
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 图片预览 -->
    <Teleport to="body">
      <div v-if="previewVisible" class="custom-preview-overlay" @click="handlePreviewClose">
        <div class="custom-preview-container" @click.stop>
          <div
            class="custom-preview-content"
            @touchstart="handleTouchStart"
            @touchmove="handleTouchMove"
            @touchend="handleTouchEnd"
          >
            <button class="close-button" @click="handlePreviewClose">
              <svg viewBox="0 0 24 24">
                <path fill="currentColor" d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/>
              </svg>
            </button>

            <button class="nav-button prev" @click="showPreviousImage" v-if="currentImageIndex > 0">
              <svg viewBox="0 0 24 24">
                <path fill="currentColor" d="M15.41 7.41L14 6l-6 6 6 6 1.41-1.41L10.83 12z"/>
              </svg>
            </button>

            <img
              :src="previewImageUrl"
              class="preview-image"
              :style="{ transform: `translateX(${swipeOffset}px)` }"
            />

            <button class="nav-button next" @click="showNextImage" v-if="currentImageIndex < allImages.length - 1">
              <svg viewBox="0 0 24 24">
                <path fill="currentColor" d="M10 6L8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z"/>
              </svg>
            </button>

            <div class="image-counter">{{ currentImageIndex + 1 }} / {{ allImages.length }}</div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, computed, h, onUnmounted } from 'vue'
import { Modal } from 'ant-design-vue'
import { getDeviceType } from '@/utils/device'
import { DEVICE_TYPE_ENUM } from '@/constants/device'

const props = defineProps<{
  content: string
}>()

// 判断是否为移动端
const isMobile = computed(() => {
  return getDeviceType() === DEVICE_TYPE_ENUM.MOBILE
})

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

// 控制预览模态框显示
const previewVisible = ref(false)
const previewImageUrl = ref('')

// 存储所有图片
const allImages = ref<string[]>([])
const currentImageIndex = ref(0)

// 触摸滑动相关状态
const touchStartX = ref(0)
const touchEndX = ref(0)
const swipeOffset = ref(0)
const isSwiping = ref(false)

// 解析内容
const parseContent = (content: string) => {
  const result: ContentBlock[] = []
  const lines = content.split('\n')
  let currentText = ''
  let currentImages: ImageItem[] = []

  lines.forEach(line => {
    const imgMatch = line.match(/!\[(.*?)\]\((.*?)\)/)
    if (imgMatch) {
      // 如果之前有文本，先添加文本块
      if (currentText) {
        result.push({ type: 'text', content: currentText.trim() })
        currentText = ''
      }
      // 收集图片
      currentImages.push({
        alt: imgMatch[1],
        src: imgMatch[2]
      })
    } else {
      // 只有当当前行不为空时，才处理图片组
      if (line.trim()) {
        // 如果有收集的图片，先添加图片组
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

  // 处理最后的块
  if (currentImages.length > 0) {
    result.push({
      type: 'image-group',
      images: currentImages
    })
  }
  if (currentText.trim()) {
    result.push({ type: 'text', content: currentText.trim() })
  }

  // 合并连续的图片组
  const mergedResult: ContentBlock[] = []
  let currentGroup: ImageItem[] = []

  result.forEach((block, index) => {
    if (block.type === 'image-group') {
      currentGroup.push(...(block.images || []))
    } else {
      // 如果有收集的图片组，先添加
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

  // 处理最后可能剩余的图片组
  if (currentGroup.length > 0) {
    mergedResult.push({
      type: 'image-group',
      images: currentGroup
    })
  }

  blocks.value = mergedResult
}

// 获取网格类名
const getGridClass = (count: number) => {
  const baseClass = count === 1 ? 'single' : ''
  const countClass = {
    1: 'one',
    2: 'two',
    3: 'three',
    4: 'four',
    6: 'six'
  }[count] || 'default'

  return `${baseClass} ${countClass}`
}

// 处理图片点击
const handleImageClick = (src: string) => {
  // 收集所有图片URL
  allImages.value = blocks.value
    .filter(block => block.type === 'image-group')
    .flatMap(block => block.images || [])
    .map(img => img.src.replace('_thumbnail', ''))

  // 设置当前图片索引
  currentImageIndex.value = allImages.value.findIndex(url => url === src.replace('_thumbnail', ''))
  previewImageUrl.value = src.replace('_thumbnail', '')
  previewVisible.value = true
}

// 显示上一张图片
const showPreviousImage = () => {
  if (currentImageIndex.value > 0) {
    currentImageIndex.value--
    previewImageUrl.value = allImages.value[currentImageIndex.value]
  }
}

// 显示下一张图片
const showNextImage = () => {
  if (currentImageIndex.value < allImages.value.length - 1) {
    currentImageIndex.value++
    previewImageUrl.value = allImages.value[currentImageIndex.value]
  }
}

// 处理触摸开始
const handleTouchStart = (e: TouchEvent) => {
  touchStartX.value = e.touches[0].clientX
  isSwiping.value = true
}

// 处理触摸移动
const handleTouchMove = (e: TouchEvent) => {
  if (!isSwiping.value) return

  const currentX = e.touches[0].clientX
  const diff = currentX - touchStartX.value
  const previewImage = document.querySelector('.preview-image') as HTMLElement

  // 移除过渡效果，使滑动更加跟手
  if (previewImage) {
    previewImage.style.transition = 'none'
  }

  // 计算滑动距离和阻尼效果
  const dampingFactor = 0.95 // 增加阻尼系数，使滑动更加灵敏
  const maxDiff = window.innerWidth * 0.4 // 减小最大滑动距离限制

  let finalDiff = diff * dampingFactor
  if (Math.abs(finalDiff) > maxDiff) {
    finalDiff = Math.sign(finalDiff) * maxDiff
  }

  // 根据滑动距离计算透明度
  const opacity = Math.max(0.7, 1 - Math.abs(finalDiff) / (window.innerWidth * 1.5))

  if ((currentImageIndex.value === 0 && diff > 0) ||
    (currentImageIndex.value === allImages.value.length - 1 && diff < 0)) {
    swipeOffset.value = finalDiff * 0.4 // 增加边界滑动的阻尼
  } else {
    swipeOffset.value = finalDiff
  }

  // 应用透明度
  if (previewImage) {
    previewImage.style.opacity = opacity.toString()
  }
}

// 处理触摸结束
const handleTouchEnd = () => {
  const minSwipeDistance = 40
  const previewImage = document.querySelector('.preview-image') as HTMLElement

  if (previewImage) {
    previewImage.style.transition = 'all 0.4s cubic-bezier(0.4, 0, 0.2, 1)'
    previewImage.style.opacity = '1'
  }

  if (Math.abs(swipeOffset.value) > minSwipeDistance) {
    const targetOffset = Math.sign(swipeOffset.value) * window.innerWidth

    if (swipeOffset.value > 0 && currentImageIndex.value > 0) {
      // 向右滑动到上一张
      swipeOffset.value = targetOffset
      if (previewImage) {
        previewImage.classList.add('transitioning')
        setTimeout(() => {
          showPreviousImage()
          requestAnimationFrame(() => {
            swipeOffset.value = 0
            if (previewImage) {
              previewImage.classList.remove('transitioning')
            }
          })
        }, 50)
      }
    } else if (swipeOffset.value < 0 && currentImageIndex.value < allImages.value.length - 1) {
      // 向左滑动到下一张
      swipeOffset.value = targetOffset
      if (previewImage) {
        previewImage.classList.add('transitioning')
        setTimeout(() => {
          showNextImage()
          requestAnimationFrame(() => {
            swipeOffset.value = 0
            if (previewImage) {
              previewImage.classList.remove('transitioning')
            }
          })
        }, 50)
      }
    } else {
      // 回弹动画
      swipeOffset.value = 0
    }
  } else {
    // 回弹动画
    swipeOffset.value = 0
  }

  isSwiping.value = false
}

// 关闭预览
const handlePreviewClose = () => {
  previewVisible.value = false
}

// 监听内容变化
watch(() => props.content, (newContent) => {
  if (newContent) {
    parseContent(newContent)
  }
}, { immediate: true })
</script>

<style scoped>
.markdown-content {
  font-size: 1rem;
  line-height: 1.8;
  color: #ffffff !important;
  width: 100%;
}

.content-wrapper {
  padding: 1rem;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  width: 100%;
  transition: all 0.3s ease;
  color: #ffffff !important;
}

/* 文本样式优化 */
.text-block {
  margin: 1.5rem 0;
  letter-spacing: 0.02em;
  width: 100%;
  display: block;
  color: #ffffff !important;
}

.text-block:first-child {
  margin-top: 0;
}

.text-block:last-child {
  margin-bottom: 0;
}

.text-block :deep(*) {
  color: #ffffff !important;
}

.text-block :deep(h1),
.text-block :deep(h2),
.text-block :deep(h3),
.text-block :deep(h4),
.text-block :deep(h5),
.text-block :deep(h6) {
  color: #ffffff !important;
  font-weight: 600;
  margin: 1.5em 0 0.8em;
  line-height: 1.4;
  padding: 0.6em 1em;
  border-radius: 8px;
  transition: all 0.3s ease;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.text-block :deep(h1) {
  font-size: 2em;
  background: linear-gradient(135deg, rgba(72, 187, 120, 0.3) 0%, rgba(72, 187, 120, 0.1) 100%);
  border-left: 4px solid rgba(72, 187, 120, 0.8);
  border-radius: 12px;
  margin-bottom: 1.2em;
  box-shadow: 0 4px 6px rgba(72, 187, 120, 0.1);
}

.text-block :deep(h2) {
  font-size: 1.5em;
  background: linear-gradient(135deg, rgba(237, 100, 166, 0.25) 0%, rgba(237, 100, 166, 0.1) 100%);
  border-left: 3px solid rgba(237, 100, 166, 0.7);
  border-radius: 10px;
  margin-bottom: 1em;
  box-shadow: 0 4px 6px rgba(237, 100, 166, 0.1);
}

.text-block :deep(h3) {
  font-size: 1.3em;
  background: linear-gradient(135deg, rgba(234, 179, 8, 0.25) 0%, rgba(234, 179, 8, 0.1) 100%);
  border-left: 3px solid rgba(234, 179, 8, 0.7);
  border-radius: 8px;
  box-shadow: 0 4px 6px rgba(234, 179, 8, 0.1);
}

.text-block :deep(h4) {
  font-size: 1.2em;
  background: linear-gradient(135deg, rgba(72, 187, 120, 0.2) 0%, rgba(72, 187, 120, 0.05) 100%);
  border-left: 2px solid rgba(72, 187, 120, 0.6);
  border-radius: 6px;
}

.text-block :deep(h5) {
  font-size: 1.1em;
  background: linear-gradient(135deg, rgba(237, 100, 166, 0.15) 0%, rgba(237, 100, 166, 0.05) 100%);
  border-left: 2px solid rgba(237, 100, 166, 0.5);
  border-radius: 4px;
}

.text-block :deep(h6) {
  font-size: 1em;
  background: linear-gradient(135deg, rgba(234, 179, 8, 0.15) 0%, rgba(234, 179, 8, 0.05) 100%);
  border-left: 2px solid rgba(234, 179, 8, 0.5);
  border-radius: 4px;
}

/* 标题悬停效果 */
.text-block :deep(h1:hover),
.text-block :deep(h2:hover),
.text-block :deep(h3:hover),
.text-block :deep(h4:hover),
.text-block :deep(h5:hover),
.text-block :deep(h6:hover) {
  transform: translateX(4px);
  filter: brightness(1.1);
}

.text-block :deep(h1:hover) {
  box-shadow: 0 6px 12px rgba(72, 187, 120, 0.15);
}

.text-block :deep(h2:hover) {
  box-shadow: 0 6px 12px rgba(237, 100, 166, 0.15);
}

.text-block :deep(h3:hover) {
  box-shadow: 0 6px 12px rgba(234, 179, 8, 0.15);
}

.text-block :deep(p) {
  margin: 1em 0;
  line-height: 1.8;
  color: #ffffff !important;
}

.text-block :deep(strong) {
  color: #ffffff !important;
  font-weight: 600;
}

.text-block :deep(em) {
  color: #ffffff !important;
}

.text-block :deep(blockquote) {
  margin: 1em 0;
  padding: 0.5em 1em;
  border-left: 4px solid rgba(76, 111, 255, 0.5);
  background: rgba(76, 111, 255, 0.1);
  border-radius: 4px;
  color: #ffffff !important;
}

.text-block :deep(code) {
  background: rgba(255, 255, 255, 0.1);
  padding: 0.2em 0.4em;
  border-radius: 4px;
  font-family: 'Fira Code', monospace;
  font-size: 0.9em;
  color: #7dd3fc !important;
}

.text-block :deep(pre) {
  background: rgba(0, 0, 0, 0.3);
  padding: 1em;
  border-radius: 8px;
  overflow-x: auto;
  margin: 1em 0;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.text-block :deep(pre code) {
  background: transparent;
  padding: 0;
  color: #ffffff !important;
}

.text-block :deep(a) {
  color: #7dd3fc !important;
  text-decoration: none;
  transition: all 0.3s ease;
  border-bottom: 1px solid transparent;
}

.text-block :deep(a:hover) {
  border-bottom-color: #7dd3fc;
  opacity: 0.8;
}

.text-block :deep(ul),
.text-block :deep(ol) {
  margin: 1em 0;
  padding-left: 1.5em;
  color: #ffffff !important;
}

.text-block :deep(li) {
  margin: 0.5em 0;
  color: #ffffff !important;
}

.text-block :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 1em 0;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  overflow: hidden;
}

.text-block :deep(th),
.text-block :deep(td) {
  padding: 0.75em 1em;
  border: 1px solid rgba(255, 255, 255, 0.1);
  text-align: left;
  color: #ffffff !important;
}

.text-block :deep(th) {
  background: rgba(255, 255, 255, 0.1);
  color: #ffffff !important;
  font-weight: 600;
}

.text-block :deep(hr) {
  border: none;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  margin: 2em 0;
}

.image-wrapper {
  width: 100%;
  margin: 1.5rem 0;
}

.image-grid {
  display: grid;
  gap: 8px;
  width: 100%;
  background: rgba(0, 0, 0, 0.2);
  border-radius: 12px;
  padding: 8px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.image-grid.single {
  grid-template-columns: 1fr;
}

.image-grid.two {
  grid-template-columns: repeat(2, 1fr);
}

.image-grid.three {
  grid-template-columns: repeat(3, 1fr);
}

.image-grid.four {
  grid-template-columns: repeat(2, 1fr);
  grid-template-rows: repeat(2, 1fr);
}

.image-grid.six {
  grid-template-columns: repeat(3, 1fr);
  grid-template-rows: repeat(2, 1fr);
}

.image-grid.default {
  grid-template-columns: repeat(3, 1fr);
  grid-auto-rows: 1fr;
}

.grid-item {
  position: relative;
  width: 100%;
  padding-top: 100%;
  overflow: hidden;
  border-radius: 8px;
}

.grid-image {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  min-width: 100%;
  min-height: 100%;
  object-fit: cover;
  cursor: pointer;
}

/* 单图保持原比例 */
.image-grid.single .grid-item {
  padding-top: 0;
}

.image-grid.single .grid-image {
  position: static;
  transform: none;
  min-height: auto;
  width: 100%;
  height: auto;
}

.grid-item:hover .grid-image {
  transform: translate(-50%, -50%) scale(1.05);
}

.image-grid.single .grid-item:hover .grid-image {
  transform: scale(1.05);
}

/* 移动端适配 */
@media screen and (max-width: 767px) {
  .mobile-view .content-wrapper {
    border-radius: 0;
    padding: 1rem;
    min-width: 100%;
    margin: 0;
  }

  .text-block {
    font-size: 0.95rem;
    line-height: 1.6;
  }

  .image-grid {
    gap: 4px;
    padding: 4px;
    border-radius: 8px;
  }

  .grid-item {
    border-radius: 4px;
  }

  .grid-image {
    border-radius: 4px;
  }
}

/* 自定义图片预览 */
.custom-preview-overlay {
  position: fixed;
  inset: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(10px);
  z-index: 9999;
  display: flex;
  justify-content: center;
  align-items: center;
  animation: fadeIn 0.2s ease;
}

.custom-preview-container {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
}

.custom-preview-content {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
}

.preview-image {
  max-width: 90%;
  max-height: 90vh;
  object-fit: contain;
  user-select: none;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  will-change: transform, opacity;
  opacity: 1;
  transform-origin: center center;
}

.preview-image.transitioning {
  opacity: 0.8;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.close-button {
  position: fixed;
  top: 20px;
  right: 20px;
  width: 40px;
  height: 40px;
  border: none;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 50%;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;
  z-index: 10000;
}

.close-button:hover {
  background: rgba(255, 255, 255, 0.2);
  transform: scale(1.1);
}

.nav-button {
  position: fixed;
  top: 50%;
  transform: translateY(-50%);
  width: 44px;
  height: 44px;
  border: none;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 50%;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;
  z-index: 10000;
}

.nav-button:hover {
  background: rgba(255, 255, 255, 0.2);
  transform: translateY(-50%) scale(1.1);
}

.nav-button.prev {
  left: 20px;
}

.nav-button.next {
  right: 20px;
}

.nav-button svg {
  width: 24px;
  height: 24px;
  fill: currentColor;
}

.image-counter {
  position: fixed;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(255, 255, 255, 0.1);
  padding: 6px 12px;
  border-radius: 20px;
  color: #fff;
  font-size: 14px;
  z-index: 10000;
  user-select: none;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .preview-image {
    max-width: 95%;
    max-height: 80vh;
  }

  .nav-button {
    width: 36px;
    height: 36px;
  }

  .nav-button.prev {
    left: 10px;
  }

  .nav-button.next {
    right: 10px;
  }

  .close-button {
    top: auto;
    right: auto;
    bottom: 36px;
    left: 50%;
    transform: translateX(-50%);
    width: 44px;
    height: 44px;
    background: rgba(255, 255, 255, 0.15);
  }

  .close-button:hover {
    transform: translateX(-50%);
    background: rgba(255, 255, 255, 0.25);
  }

  .image-counter {
    bottom: 90px;
    font-size: 12px;
    padding: 4px 10px;
  }

  .image-grid {
    gap: 4px;
    padding: 4px;
  }

  .text-block :deep(h1),
  .text-block :deep(h2),
  .text-block :deep(h3),
  .text-block :deep(h4),
  .text-block :deep(h5),
  .text-block :deep(h6) {
    padding: 0.5em 0.8em;
    margin: 1.2em 0 0.6em;
  }

  .text-block :deep(h1) {
    font-size: 1.8em;
    border-radius: 10px;
  }

  .text-block :deep(h2) {
    font-size: 1.4em;
    border-radius: 8px;
  }

  .text-block :deep(h3) {
    font-size: 1.2em;
    border-radius: 6px;
  }

  .text-block :deep(h4) {
    font-size: 1.1em;
    border-radius: 4px;
  }

  .text-block :deep(h5),
  .text-block :deep(h6) {
    font-size: 1em;
    border-radius: 4px;
  }
}
</style>
