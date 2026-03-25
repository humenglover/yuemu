<template>
  <div v-if="visible" class="share-modal-container">
    <div class="share-modal-overlay" @click="closeModal"></div>

    <div ref="modalContentRef" class="share-card">
      <div class="logo-area">
        <img src="/src/assets/logo.png" alt="悦木图库" class="logo-img">
        <span class="logo-text">悦木图库</span>
      </div>

      <div class="preview-area">
        <div class="preview-img-container">
          <img
            :src="displayImageUrl"
            class="preview-img"
            alt="分享预览"
            @error="handleShowImageError"
          />
        </div>
        <div class="qrcode-wrapper">
          <a-qrcode
            :value="link"
            :size="80"
            :color="'#333333'"
            :bgColor="'#ffffff'"
            class="qrcode-img"
          />
        </div>
        <div class="preview-text">
          <div class="preview-tag">{{ user ? user.userName : '分享内容' }}</div>
          <div class="preview-title">{{ title || '分享内容标题' }}</div>
        </div>
      </div>

      <div class="footer-info">
        <div v-if="loginUser">
          <span class="share-from">由 </span>
          <span class="sharer-name">{{ loginUser.userName }}</span>
          <span class="share-action"> 分享</span>
          <span class="create-time"> {{ formatTime(props.createTime || new Date().toString(), 'full') }}</span>
        </div>
        <div v-else>
          {{ formatTime(props.createTime || new Date().toString(), 'full') }}
        </div>
      </div>

      <div class="button-group">
        <button class="action-btn copy-btn" @click="copyLink">
          <span>复制链接</span>
        </button>
        <button class="action-btn save-btn" @click="saveImage">
          <span>保存图片</span>
        </button>
        <button class="action-btn close-btn" @click="closeModal">
          <span>关闭</span>
        </button>
      </div>
    </div>

    <!-- 恢复旧版可靠的截图容器逻辑：隐藏+临时显示 -->
    <div ref="screenshotRef" class="screenshot-container" style="display: none;">
      <div class="share-card">
        <div class="logo-area">
          <img src="/src/assets/logo.png" alt="悦木图库" class="logo-img">
          <span class="logo-text">悦木图库</span>
        </div>

        <div class="preview-area">
          <!-- 截图容器使用用户真实图片 + 跨域属性 -->
          <div class="preview-img-container">
            <img
              :src="displayImageUrl"
              class="preview-img-screenshot"
              alt="分享预览"
              crossorigin="anonymous"
              @error="(e) => { e.target.src = fallbackImageUrl }"
            />
          </div>
          <div class="qrcode-wrapper">
            <a-qrcode
              :value="link"
              :size="80"
              :color="'#333333'"
              :bgColor="'#ffffff'"
              class="qrcode-img"
            />
          </div>
          <div class="preview-text">
            <div class="preview-tag">{{ user ? user.userName : '分享内容' }}</div>
            <div class="preview-title">{{ title || '分享内容标题' }}</div>
          </div>
        </div>

        <!-- 核心修改：截图容器的时间固定为当前时间 -->
        <div class="footer-info">
          <div>生成时间: {{ formatTime(new Date().toString(), 'full') }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import html2canvas from 'html2canvas'
import { formatTime } from '@/utils/dateUtils'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { getTextCover } from '@/utils/textCoverGenerator'

interface Props {
  title?: string
  link: string
  imageUrl: string
  user?: {
    userName: string
    userAvatar?: string
  }
  createTime?: string
}

const props = withDefaults(defineProps<Props>(), {
  title: '',
  link: '',
  imageUrl: '',
  user: undefined,
  createTime: ''
})

const loginUserStore = useLoginUserStore()
const loginUser = computed(() => loginUserStore.loginUser)

const visible = ref(false)
const emit = defineEmits(['close'])
const modalContentRef = ref<HTMLElement | null>(null)
const screenshotRef = ref<HTMLElement | null>(null)
const fallbackImageUrl = 'https://pic.imgdb.cn/item/65ced2939f345e8d03633db1.jpg?x-oss-process=style/small'
const generatedCoverUrl = ref<string | null>(null)  // 存储动态生成的封面图URL

const openModal = async (dynamicImageUrl = '') => {
  visible.value = true
  document.body.style.overflow = 'hidden'

  // 如果传入了动态图片URL，则使用它
  if (dynamicImageUrl) {
    generatedCoverUrl.value = dynamicImageUrl
  } else if (!props.imageUrl && props.title) {
    // 如果没有图片URL但有标题，则生成封面图
    try {
      generatedCoverUrl.value = await getTextCover(props.title, 300, 400)
    } catch (error) {
      console.error('生成封面图失败:', error)
      generatedCoverUrl.value = fallbackImageUrl
    }
  } else {
    // 重置生成的封面图URL
    generatedCoverUrl.value = null
  }
}

const closeModal = () => {
  visible.value = false
  document.body.style.overflow = ''
  emit('close')
}

const copyLink = async () => {
  try {
    await navigator.clipboard.writeText(props.link)
    message.success('链接已复制')
  } catch {
    message.error('复制失败，请手动复制')
  }
}

// 计算最终要显示的图片URL
const displayImageUrl = computed(() => {
  // 优先使用动态生成的封面图
  if (generatedCoverUrl.value) {
    return generatedCoverUrl.value
  }
  // 其次使用传入的图片URL
  if (props.imageUrl) {
    return props.imageUrl
  }
  // 最后使用默认图片
  return fallbackImageUrl
})

const handleShowImageError = (e: Event) => {
  const img = e.target as HTMLImageElement
  img.src = fallbackImageUrl
}

// 恢复旧版可靠的截图逻辑
const saveImage = async () => {
  try {
    await nextTick()
    if (!screenshotRef.value) throw new Error('未找到截图容器')

    // 旧版核心：临时显示截图容器确保渲染
    const screenshotContainer = screenshotRef.value
    screenshotContainer.style.display = 'block'
    screenshotContainer.style.position = 'fixed'
    screenshotContainer.style.left = '-9999px'
    screenshotContainer.style.top = '0'
    screenshotContainer.style.zIndex = '-1'
    screenshotContainer.style.width = '420px'

    // 等待图片和二维码完全加载
    await new Promise(resolve => setTimeout(resolve, 500))

    // 恢复旧版稳定的截图配置
    const canvas = await html2canvas(screenshotContainer, {
      backgroundColor: '#ffffff',
      useCORS: true,
      allowTaint: true,
      scale: 6,
      width: 396,
      height: screenshotContainer.offsetHeight,
      logging: false,
      imageTimeout: 10000,
      removeContainer: false // 关键：保留容器避免DOM异常
    })

    // 截图后隐藏容器
    screenshotContainer.style.display = 'none'

    // 下载图片（恢复旧版正确的配置）
    const link = document.createElement('a')
    link.download = `share-${Date.now()}.png`
    link.href = canvas.toDataURL('image/png', 3.0)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    message.success('分享图片已保存')
  } catch (error) {
    console.error('保存图片失败:', error)
    // 失败兜底：隐藏容器+提示
    if (screenshotRef.value) {
      screenshotRef.value.style.display = 'none'
    }
    message.error('保存图片失败，请重试')
  }
}

defineExpose({
  openModal,
})
</script>

<style scoped>
.share-modal-container {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  font-family: "HarmonyOS Sans", "PingFang SC", "Microsoft YaHei", sans-serif;
  background-color: rgba(0, 0, 0, 0.15);
  backdrop-filter: blur(12px);
}

.share-modal-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
}

.share-card {
  width: 94%;
  max-width: 420px;
  background: linear-gradient(180deg, #d9e8ff, #f0e8ff);
  border-radius: 4px;
  padding: 24px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  position: relative;
  z-index: 1001;
  animation: modalFadeIn 0.3s ease;
}

.logo-area {
  width: 100%;
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}

.logo-img {
  width: 40px;
  height: 40px;
  margin-right: 8px;
  object-fit: contain;
}

.logo-text {
  font-size: 18px;
  font-weight: 700;
  color: #333;
}

.preview-area {
  width: 100%;
  background: #ffffff;
  border-radius: 12px;
  padding: 0;
  display: flex;
  flex-direction: column;
  margin-bottom: 20px;
  position: relative;
  overflow: hidden;
}

.preview-img-container {
  width: 100%;
  height: 280px;
  overflow: hidden;
  border-radius: 12px 12px 0 0;
}

.preview-img, .preview-img-screenshot {
  width: 100%;
  height: 100%;
  object-fit: cover;
  background-color: #f5f5f5;
}

.qrcode-wrapper {
  position: absolute;
  bottom: 12px;
  right: 12px;
  background: linear-gradient(135deg, #f8f9fa, #e9ecef);
  padding: 6px;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.08);
}

.qrcode-img {
  width: 80px !important;
  height: 80px !important;
  border-radius: 8px;
}

.preview-text {
  padding: 16px;
  position: relative;
  z-index: 2;
}

.preview-tag {
  font-size: 12px;
  color: #999;
  margin-bottom: 4px;
}

.preview-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  line-height: 1.4;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: calc(100% - 90px);
}

.footer-info {
  text-align: left;
  font-size: 12px;
  color: #999;
  line-height: 1.6;
  margin-bottom: 0;
  padding: 0 4px;
}

.sharer-name {
  color: #40a9ff;
  font-weight: 500;
}

.create-time {
  margin-left: 8px;
}

.button-group {
  display: flex;
  gap: 8px;
  width: 100%;
  margin-top: 16px;
}

.action-btn {
  flex: 1;
  height: 40px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  color: white;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 0 12px;
}

.copy-btn {
  background-color: #40a9ff;
}

.save-btn {
  background-color: #52c41a;
}

.close-btn {
  background-color: #f5f5f5;
  color: #666;
}

.action-btn:hover {
  opacity: 0.9;
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.action-btn:active {
  transform: translateY(0);
}

@keyframes modalFadeIn {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

/* 截图容器样式补充 */
.screenshot-container {
  position: fixed;
  left: -9999px;
  top: 0;
  z-index: -1;
}
</style>
