<template>
  <div class="love-board-container">
    <!-- 全局浪漫背景（飘心+柔光效果） -->
    <div class="global-romantic-bg" v-if="!noPermission">
      <div class="floating-hearts"></div>
      <div class="soft-light"></div>
    </div>

    <!-- 封面区域（强化层次感+动态效果） -->
    <div class="bg-wrap my-animation-slide-top" v-if="!noPermission">
      <!-- 背景图片（加柔光遮罩） -->
      <div class="bg-image-container">
        <img
          class="love-image my-el-image"
          :src="loveBoard?.bgCover || defaultBgCover"
          alt="背景图片"
          :class="{ 'loaded': !manAvatarLoading && !womanAvatarLoading }"
        >
        <div class="bg-overlay"></div>
      </div>

      <!-- 情侣信息（居中+心跳动画） -->
      <div class="love-wrap transformCenter">
        <!-- 男方信息 -->
        <div class="person-card">
          <div class="avatar-container">
            <div class="avatar-shine" :class="{ 'active': !manAvatarLoading }"></div>
            <img
              class="love-avatar"
              :class="{ loading: manAvatarLoading, 'scale-in': !manAvatarLoading }"
              :src="loveBoard?.manCover || defaultManAvatar"
              @load="handleAvatarLoad('man')"
              alt="他的头像"
            >
          </div>
          <div class="love-title">
            <app-color-animate-text
              :texts="[loveBoard?.manName || '他']"
              :infinite="true"
              class="name-text"
            ></app-color-animate-text>
          </div>
        </div>

        <!-- 中间爱心（心跳+呼吸动画） -->
        <div class="couple-heart">
          <img
            class="love-img heartbeat"
            src="@/assets/loveLike.svg"
            alt="心心"
          >
          <div class="view-count">
            <span class="view-number pulse">{{ loveBoard?.viewCount || '0' }}</span>
            <div class="charging-bar"></div>
          </div>
        </div>

        <!-- 女方信息 -->
        <div class="person-card">
          <div class="avatar-container">
            <div class="avatar-shine" :class="{ 'active': !womanAvatarLoading }"></div>
            <img
              class="love-avatar"
              :class="{ loading: womanAvatarLoading, 'scale-in': !womanAvatarLoading }"
              :src="loveBoard?.womanCover || defaultWomanAvatar"
              @load="handleAvatarLoad('woman')"
              alt="她的头像"
            >
          </div>
          <div class="love-title">
            <app-color-animate-text
              :texts="[loveBoard?.womanName || '她']"
              :infinite="true"
              class="name-text"
            ></app-color-animate-text>
          </div>
        </div>
      </div>

      <!-- 功能按钮（悬浮效果） -->
      <button
        v-if="isOwner"
        class="edit-button float-btn"
        @click="showEditModal"
      >
        <span class="edit-icon">✏️</span>
        <span class="btn-tooltip">编辑画板</span>
      </button>

      <button
        v-if="loveBoard?.status === 1"
        class="share-button float-btn"
        @click="handleShare"
      >
        💌
        <span class="btn-tooltip">分享甜蜜</span>
      </button>

      <!-- 波浪动画（强化底部过渡） -->
      <div class="wave-container">
        <svg class="waves" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" viewBox="0 24 150 40" preserveAspectRatio="none" shape-rendering="auto">
          <defs>
            <path id="gentle-wave" d="M-160 44c30 0 58-18 88-18s 58 18 88 18 58-18 88-18 58 18 88 18 v44h-352z"></path>
          </defs>
          <g class="moving-waves">
            <use xlink:href="#gentle-wave" x="48" y="-1" fill="rgba(255,255,255,0.6)"></use>
            <use xlink:href="#gentle-wave" x="48" y="3" fill="rgba(255,255,255,0.5)"></use>
            <use xlink:href="#gentle-wave" x="48" y="5" fill="rgba(255,255,255,0.4)"></use>
            <use xlink:href="#gentle-wave" x="48" y="8" fill="rgba(255,255,255,0.3)"></use>
            <use xlink:href="#gentle-wave" x="48" y="13" fill="rgba(255,255,255,0.2)"></use>
          </g>
        </svg>
      </div>
    </div>

    <!-- 内容区域（柔和卡片+渐变） -->
    <div class="love-container" v-if="!noPermission">
      <!-- 时间统计（浪漫文案+动态数字） -->
      <div class="time-section myCenter love-content">
        <div class="time-card">
          <div class="love-time-title1 text-glow">
            这是我们一起走过的
          </div>
          <div class="love-time1">
            第
            <span class="love-time1-item pulse">{{ timing.year }}</span>
            年
            <span class="love-time1-item pulse">{{ timing.month }}</span>
            月
            <span class="love-time1-item pulse">{{ timing.day }}</span>
            日
            <span class="love-time1-item pulse">{{ timing.hour }}</span>
            时
            <span class="love-time1-item pulse">{{ timing.minute }}</span>
            分
            <span class="love-time1-item pulse">{{ timing.second }}</span>
            秒
          </div>
        </div>

        <!-- 倒计时（爱心前缀） -->
        <div
          class="countdown-card"
          v-if="loveBoard?.countdownTitle || loveBoard?.countdownTime"
        >
          <span class="heart-prefix">💖</span>
          <span class="countdown-text">
            {{ loveBoard?.countdownTitle }}:
            <span class="countdown-value">{{ countdownChange }}</span>
          </span>
        </div>
      </div>

      <!-- 功能卡片（玻璃拟态+hover动效） -->
      <div class="card-wrap">
        <div
          class="card-content shadow-box-mini glass-card"
          :class="{ active: currentView === 'daily' }"
          data-type="daily"
          @click="handleDailyClick"
        >
          <div class="card-icon bg-pink-gradient">📝</div>
          <div class="card-right">
            <h3 class="card-title">{{ isMobile ? '点滴' : '点点滴滴' }}</h3>
            <div class="card-desc">💝 记录甜蜜瞬间</div>
          </div>
          <div class="card-arrow">→</div>
        </div>

        <div
          class="card-content shadow-box-mini glass-card"
          :class="{ active: currentView === 'album' }"
          data-type="album"
          @click="handleTimeAlbumClick"
        >
          <div class="card-icon bg-purple-gradient">📸</div>
          <div class="card-right">
            <h3 class="card-title">{{ isMobile ? '相册' : '时光相册' }}</h3>
            <div class="card-desc">✨ 定格美好回忆</div>
          </div>
          <div class="card-arrow">→</div>
        </div>

        <div
          class="card-content shadow-box-mini glass-card"
          :class="{ active: currentView === 'music' }"
          data-type="music"
          @click="handleMusicClick"
        >
          <div class="card-icon bg-blue-gradient">🎵</div>
          <div class="card-right">
            <h3 class="card-title">{{ isMobile ? '音乐' : '音乐专栏' }}</h3>
            <div class="card-desc">🎼 记录专属音乐</div>
          </div>
          <div class="card-arrow">→</div>
        </div>

        <div
          class="card-content shadow-box-mini glass-card"
          :class="{ active: currentView === 'wishes' }"
          data-type="wishes"
          @click="handleWishesClick"
        >
          <div class="card-icon bg-yellow-gradient">🎁</div>
          <div class="card-right">
            <h3 class="card-title">{{ isMobile ? '祝福' : '祝福板' }}</h3>
            <div class="card-desc">🌟 收集世界祝愿</div>
          </div>
          <div class="card-arrow">→</div>
        </div>
      </div>

      <!-- 内容区域（淡入动画） -->
      <div
        v-if="currentView === 'daily' && loveBoard?.id"
        class="content-panel daily-moments-wrap"
        :class="{ 'show': currentView === 'daily', 'collapsed': !currentView && prevView === 'daily' }"
      >
        <DailyMoments
          :loveBoardId="String(loveBoard.id)"
          :isOwner="isOwner"
        />
      </div>

      <div
        v-if="currentView === 'album'"
        class="content-panel time-album-wrap"
        :class="{ 'show': currentView === 'album', 'collapsed': !currentView && prevView === 'album' }"
      >
        <TimeAlbum
          ref="timeAlbumRef"
          :loveBoardId="String(loveBoard?.id || '')"
          :isOwner="isOwner"
        />
      </div>

      <div
        v-if="currentView === 'music'"
        class="content-panel music-album-wrap"
        :class="{ 'show': currentView === 'music', 'collapsed': !currentView && prevView === 'music' }"
      >
        <MusicAlbum
          ref="musicAlbumRef"
          :loveBoardId="String(loveBoard?.id || '')"
          :isOwner="isOwner"
        />
      </div>

      <div
        v-if="currentView === 'wishes'"
        class="content-panel wishes-wrap"
        :class="{ 'show': currentView === 'wishes', 'collapsed': !currentView && prevView === 'wishes' }"
      >
        <WishesBoard
          :ownerId="loveBoard?.id || 0"
          :isOwner="isOwner"
        />
      </div>
    </div>

    <!-- 模态框（浪漫玻璃风格） -->
    <div v-if="modalVisible" class="modal-overlay">
      <div class="modal-content glass-modal">
        <div class="modal-header">
          <h2 class="modal-title text-glow">{{ loveBoard ? '编辑恋爱画板' : '创建恋爱画板' }}</h2>
          <button class="modal-close" @click="handleModalCancel">×</button>
        </div>
        <form class="modal-form" @submit.prevent="handleModalOk">
          <div class="form-item">
            <label class="form-label">背景图片</label>
            <div class="upload-wrapper">
              <div class="image-preview bg-pink-light" @click="showFileUploadDialog('bg')">
                <img v-if="formState.bgCover" :src="formState.bgCover" alt="背景图片" />
                <div v-else class="upload-placeholder">
                  <span class="plus-icon gradient-text">+</span>
                  <div class="upload-tip">点击上传背景图片</div>
                </div>
              </div>
            </div>
          </div>

          <div class="couple-info-form">
            <div class="person-info">
              <div class="form-item">
                <label class="form-label">他的头像</label>
                <div class="upload-wrapper">
                  <div class="image-preview avatar-preview bg-purple-light" @click="showFileUploadDialog('man')">
                    <img v-if="formState.manCover" :src="formState.manCover" alt="他的头像" />
                    <div v-else class="upload-placeholder">
                      <span class="plus-icon gradient-text">+</span>
                      <div class="upload-tip">点击上传头像</div>
                    </div>
                  </div>
                </div>
              </div>
              <div class="form-item">
                <label class="form-label">他的名字</label>
                <input
                  type="text"
                  v-model="formState.manName"
                  placeholder="请输入他的名字"
                  required
                  class="form-input"
                >
              </div>
            </div>

            <div class="heart-divider">💖</div>

            <div class="person-info">
              <div class="form-item">
                <label class="form-label">她的头像</label>
                <div class="upload-wrapper">
                  <div class="image-preview avatar-preview bg-pink-light" @click="showFileUploadDialog('woman')">
                    <img v-if="formState.womanCover" :src="formState.womanCover" alt="她的头像" />
                    <div v-else class="upload-placeholder">
                      <span class="plus-icon gradient-text">+</span>
                      <div class="upload-tip">点击上传头像</div>
                    </div>
                  </div>
                </div>
              </div>
              <div class="form-item">
                <label class="form-label">她的名字</label>
                <input
                  type="text"
                  v-model="formState.womanName"
                  placeholder="请输入她的名字"
                  required
                  class="form-input"
                >
              </div>
            </div>
          </div>

          <div class="form-item">
            <label class="form-label">在一起的时间</label>
            <input
              type="date"
              v-model="formState.timing"
              required
              class="form-input"
            >
          </div>

          <div class="form-item">
            <label class="form-label">倒计时标题</label>
            <input
              type="text"
              v-model="formState.countdownTitle"
              placeholder="例如：结婚纪念日"
              class="form-input"
            >
          </div>

          <div class="form-item" v-if="formState.countdownTitle">
            <label class="form-label">倒计时时间</label>
            <input
              type="date"
              v-model="formState.countdownTime"
              class="form-input"
            >
          </div>

          <div class="form-item">
            <label class="form-label">是否公开</label>
            <div class="switch-wrapper">
              <input
                type="checkbox"
                :checked="formState.status === 1"
                @change="(e) => formState.status = e.target.checked ? 1 : 0"
                class="custom-switch"
              >
              <span class="switch-label">{{ formState.status === 1 ? '公开，可以分享给他人' : '私密，仅自己可见' }}</span>
            </div>
          </div>

          <div class="modal-footer">
            <button type="button" class="btn-cancel" @click="handleModalCancel">取消</button>
            <button type="submit" class="btn-confirm bg-pink-gradient">确定</button>
          </div>
        </form>
      </div>
    </div>

    <!-- 其他原有组件（保留功能，优化样式） -->
    <AvatarCropper
      ref="cropperRef"
      :imageUrl="tempImageUrl"
      @success="handleCroppedImage"
    />

    <input
      type="file"
      ref="fileInput"
      style="display: none"
      accept="image/*"
      @change="handleFileChange"
    />

    <!-- 消息提示（浪漫风格） -->
    <div
      v-if="showMessageText"
      :class="['message', `message-${showMessageType}`, 'romantic-message']"
    >
      <span class="message-icon">{{ showMessageType === 'success' ? '💖' : '💔' }}</span>
      {{ showMessageText }}
    </div>

    <!-- 无权限提示（优化氛围） -->
    <div v-if="noPermission" class="no-permission-container">
      <div class="no-permission-content glass-card">
        <div class="lock-icon pulse">🔒</div>
        <h2 class="text-glow">这是一个私密的恋爱画板</h2>
        <p>主人设置了访问权限，暂时无法查看哦~</p>
        <button class="back-button bg-pink-gradient" @click="$router.push('/home')">返回主页</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive, computed, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { prevRoute } from '@/router'
import dayjs from 'dayjs'
import AppColorAnimateText from "@/components/color-animate-text/color-animate-text.component.vue"
import {
  getMyLoveBoardUsingGet,
  addLoveBoardUsingPost,
  updateLoveBoardUsingPost,
  getLoveBoardByIdUsingGet,
} from '@/api/loveBoardController'
import { uploadPostImageUsingPost } from '@/api/pictureController'
import AvatarCropper from '@/components/AvatarCropper.vue'
import DailyMoments from '@/components/DailyMoments.vue'
import TimeAlbum from '@/components/TimeAlbum.vue'
import MusicAlbum from '@/components/MusicAlbum.vue'
import WishesBoard from '@/components/WishesBoard.vue'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { getDefaultAvatar } from '@/utils/userUtils'

// 新增默认背景图（强化恋爱氛围）
import defaultBgCover from '@/assets/default.png'

interface LoveBoard {
  id?: number;
  bgCover?: string;
  manCover?: string;
  womanCover?: string;
  manName?: string;
  womanName?: string;
  timing?: string;
  countdownTitle?: string;
  countdownTime?: string;
  status?: number;
  userId?: number;
  viewCount?: number;
}

// 消息提示相关
const showMessageText = ref('')
const showMessageType = ref('info')
const messageTimer = ref<number | null>(null)

const loveBoard = ref<LoveBoard>()
const modalVisible = ref(false)
const timing = reactive({
  year: 0,
  month: 0,
  day: 0,
  hour: 0,
  minute: 0,
  second: 0
})

const countdownChange = ref('')
const tempImageUrl = ref('')
const cropperRef = ref()
const fileInput = ref<HTMLInputElement | null>(null)
const prevView = ref<string | null>(null) // 记录上一个视图，优化收起动画

const formState = reactive({
  bgCover: '',
  manCover: '',
  womanCover: '',
  manName: '',
  womanName: '',
  timing: '',
  countdownTitle: '',
  countdownTime: '',
  status: 1
})

const currentUploadType = ref<'bg' | 'man' | 'woman'>('bg')
const currentView = ref<'daily' | 'album' | 'music' | 'wishes' | null>('daily')

const router = useRouter()
const route = useRoute()

// 新增默认背景图变量
const defaultManAvatar = computed(() => getDefaultAvatar(loveBoard.value?.manName || '他'))
const defaultWomanAvatar = computed(() => getDefaultAvatar(loveBoard.value?.womanName || '她'))

onMounted(() => {
  if (prevRoute?.name === 'TimeAlbumDetail') {
    currentView.value = 'album'
    prevView.value = 'album'
  } else if (prevRoute?.name === 'MusicAlbumDetail') {
    currentView.value = 'music'
    prevView.value = 'music'
  } else {
    currentView.value = 'daily'
    prevView.value = 'daily'
  }

  fetchLoveBoard()
  setInterval(() => {
    updateTiming()
    updateCountdown()
  }, 1000)

  checkMobile()
  window.addEventListener('resize', checkMobile)

  // 初始化飘心效果
  initFloatingHearts()
})

const loginUserStore = useLoginUserStore()
const isOwner = computed(() => {
  return loginUserStore.loginUser?.id === loveBoard.value?.userId
})

const manAvatarLoading = ref(true)
const womanAvatarLoading = ref(true)

const handleAvatarLoad = (type: 'man' | 'woman') => {
  if (type === 'man') {
    manAvatarLoading.value = false
  } else {
    womanAvatarLoading.value = false
  }
}

const showMessage = (text: string, type: 'error' | 'success' | 'info' = 'error') => {
  showMessageText.value = text
  showMessageType.value = type

  if (messageTimer.value) {
    clearTimeout(messageTimer.value)
  }

  messageTimer.value = window.setTimeout(() => {
    showMessageText.value = ''
  }, 3000)
}

const noPermission = ref(false)

const fetchLoveBoard = async () => {
  try {
    const loveBoardId = route.params.id
    let res

    if (loveBoardId) {
      res = await getLoveBoardByIdUsingGet({ id: String(loveBoardId) })
      if (res.data.code === 40101) {
        noPermission.value = true
        return
      }
    } else {
      res = await getMyLoveBoardUsingGet()
    }

    if (res.data.code === 0) {
      loveBoard.value = res.data.data
      // 初始化表单数据
      if (loveBoard.value) {
        Object.assign(formState, {
          ...loveBoard.value,
          timing: dayjs(loveBoard.value.timing).format('YYYY-MM-DD'),
          countdownTime: loveBoard.value.countdownTime ? dayjs(loveBoard.value.countdownTime).format('YYYY-MM-DD') : null,
        })
      } else if (!route.params.id) {
        modalVisible.value = true
      }
    }
  } catch (error) {
    console.error('获取恋爱画板失败:', error)
    showMessage('获取恋爱画板失败')
  }
}

const showEditModal = () => {
  if (loveBoard.value) {
    Object.assign(formState, {
      ...loveBoard.value,
      timing: dayjs(loveBoard.value.timing).format('YYYY-MM-DD'),
      countdownTime: loveBoard.value.countdownTime ? dayjs(loveBoard.value.countdownTime).format('YYYY-MM-DD') : null,
    })
  }
  modalVisible.value = true
}

const handleModalOk = async () => {
  try {
    if (!formState.bgCover || !formState.manCover || !formState.womanCover ||
      !formState.manName || !formState.womanName || !formState.timing) {
      showMessage('请填写完整信息')
      return
    }

    const data = {
      ...formState,
      timing: formState.timing,
      countdownTime: formState.countdownTime,
      status: formState.status ? 1 : 0,
    }

    const res = loveBoard.value
      ? await updateLoveBoardUsingPost(data)
      : await addLoveBoardUsingPost(data)

    if (res.data.code === 0) {
      showMessage(loveBoard.value ? '更新成功' : '创建成功', 'success')
      modalVisible.value = false
      await fetchLoveBoard()
    } else {
      showMessage(res.data.message || '操作失败')
    }
  } catch (error) {
    console.error('操作失败:', error)
    showMessage('操作失败')
  }
}

const handleModalCancel = () => {
  modalVisible.value = false
}

const showFileUploadDialog = (type: 'bg' | 'man' | 'woman') => {
  currentUploadType.value = type
  fileInput.value?.click()
}

const handleFileChange = async (e: Event) => {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return

  if (currentUploadType.value === 'bg') {
    try {
      const res = await uploadPostImageUsingPost({}, {}, file)
      if (res.data.code === 0) {
        showMessage('背景图片上传成功', 'success')
        formState.bgCover = res.data.data.url
      }
    } catch (error) {
      console.error('背景图片上传失败:', error)
      showMessage('背景图片上传失败')
    }
  } else {
    tempImageUrl.value = URL.createObjectURL(file)
    cropperRef.value?.openModal()
  }
  (e.target as HTMLInputElement).value = ''
}

const handleCroppedImage = async (file: File) => {
  try {
    const res = await uploadPostImageUsingPost({}, {}, file)
    if (res.data.code === 0) {
      showMessage('头像上传成功', 'success')
      if (currentUploadType.value === 'man') {
        formState.manCover = res.data.data.url
      } else if (currentUploadType.value === 'woman') {
        formState.womanCover = res.data.data.url
      }
      cropperRef.value?.closeModal()
    }
  } catch (error) {
    console.error('头像上传失败:', error)
    showMessage('头像上传失败')
  }
}

const updateTiming = () => {
  if (!loveBoard.value?.timing) return

  const startDate = dayjs(loveBoard.value.timing)
  const now = dayjs()

  timing.year = now.diff(startDate, 'year')
  const afterYears = startDate.add(timing.year, 'year')
  timing.month = now.diff(afterYears, 'month')
  const afterMonths = afterYears.add(timing.month, 'month')
  timing.day = now.diff(afterMonths, 'day')
  const afterDays = afterMonths.add(timing.day, 'day')
  timing.hour = now.diff(afterDays, 'hour')
  const afterHours = afterDays.add(timing.hour, 'hour')
  timing.minute = now.diff(afterHours, 'minute')
  const afterMinutes = afterHours.add(timing.minute, 'minute')
  timing.second = now.diff(afterMinutes, 'second')
}

const updateCountdown = () => {
  if (!loveBoard.value?.countdownTime) return
  const diff = dayjs(loveBoard.value.countdownTime).diff(dayjs(), 'second')
  if (diff <= 0) return
  const d = Math.floor(diff / (24 * 60 * 60))
  const h = Math.floor((diff % (24 * 60 * 60)) / (60 * 60))
  const m = Math.floor((diff % (60 * 60)) / 60)
  const s = diff % 60
  countdownChange.value = `${d}天${h}时${m}分${s}秒`
}

// 优化视图切换逻辑（记录上一个视图）
const handleDailyClick = () => {
  if (currentView.value === 'daily') {
    const container = document.querySelector('.daily-moments-wrap')
    if (container) {
      container.classList.add('collapsed')
      setTimeout(() => {
        prevView.value = 'daily'
        currentView.value = null
      }, 300)
    }
  } else {
    prevView.value = currentView.value
    currentView.value = 'daily'
  }
}

const handleTimeAlbumClick = () => {
  if (currentView.value === 'album') {
    const container = document.querySelector('.time-album-wrap')
    if (container) {
      container.classList.add('collapsed')
      setTimeout(() => {
        prevView.value = 'album'
        currentView.value = null
      }, 300)
    }
  } else {
    prevView.value = currentView.value
    currentView.value = 'album'
  }
}

const handleMusicClick = () => {
  if (currentView.value === 'music') {
    const container = document.querySelector('.music-album-wrap')
    if (container) {
      container.classList.add('collapsed')
      setTimeout(() => {
        prevView.value = 'music'
        currentView.value = null
      }, 300)
    }
  } else {
    prevView.value = currentView.value
    currentView.value = 'music'
  }
}

const handleWishesClick = () => {
  if (currentView.value === 'wishes') {
    const container = document.querySelector('.wishes-wrap')
    if (container) {
      container.classList.add('collapsed')
      setTimeout(() => {
        prevView.value = 'wishes'
        currentView.value = null
      }, 300)
    }
  } else {
    prevView.value = currentView.value
    currentView.value = 'wishes'
  }
}

const handleShare = () => {
  if (!loveBoard.value?.id) return
  const shareUrl = `${window.location.origin}/loveboard/${loveBoard.value.id}`
  navigator.clipboard.writeText(shareUrl)
    .then(() => {
      showMessage('分享链接已复制到剪贴板', 'success')
    })
    .catch(() => {
      showMessage('复制链接失败')
    })
}

// 移动端检测
const isMobile = ref(false)
const checkMobile = () => {
  isMobile.value = window.innerWidth <= 767
}

// 新增：飘心动画初始化
const initFloatingHearts = () => {
  const container = document.querySelector('.floating-hearts')
  if (!container) return

  const createHeart = () => {
    const heart = document.createElement('div')
    heart.className = 'floating-heart'
    heart.textContent = '💖'

    // 随机样式
    const size = Math.random() * 16 + 8
    const left = Math.random() * 100
    const delay = Math.random() * 5
    const duration = Math.random() * 10 + 10

    heart.style.cssText = `
      position: absolute;
      left: ${left}%;
      bottom: -20px;
      font-size: ${size}px;
      opacity: ${Math.random() * 0.5 + 0.3};
      animation: floatUp ${duration}s linear infinite;
      transform: translateY(0) rotate(${Math.random() * 360}deg);
      pointer-events: none;
    `

    container.appendChild(heart)

    // 动画结束后移除
    setTimeout(() => {
      heart.remove()
    }, duration * 1000)
  }

  // 定时创建飘心
  setInterval(createHeart, 500)
}

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
  if (messageTimer.value) {
    clearTimeout(messageTimer.value)
  }
})
</script>

<style scoped lang="scss">
.love-board-container{
  margin-bottom: 64px;

}

@media screen  and (min-width: 767px){
  .love-board-container{
    margin: auto;
    margin-bottom: 64px;
    max-width: 1200px;
  }
}
// 基础变量（浪漫色调）
$pink-light: rgba(255, 192, 203, 0.2);
$pink-medium: rgba(255, 145, 173, 0.3);
$pink-deep: #ff6b9d;
$purple-light: rgba(216, 191, 216, 0.2);
$blue-light: rgba(173, 216, 230, 0.2);
$yellow-light: rgba(255, 215, 0, 0.1);
$gradient-pink: linear-gradient(135deg, #ff9a9e 0%, #fecfef 50%, #fecfef 100%);
$gradient-purple: linear-gradient(135deg, #9370db 0%, #e6e6fa 100%);
$gradient-blue: linear-gradient(135deg, #87ceeb 0%, #e0f7fa 100%);
$gradient-yellow: linear-gradient(135deg, #ffd700 0%, #fff8e1 100%);

// 全局浪漫背景
.global-romantic-bg {
  position: fixed;
  inset: 0;
  z-index: -1;
  background-color: #fff9fb;
  overflow: hidden;

}

// 飘心动画
.floating-hearts {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

@keyframes floatUp {
  0% {
    transform: translateY(0) rotate(0deg);
    opacity: 0.8;
  }
  100% {
    transform: translateY(-100vh) rotate(360deg);
    opacity: 0;
  }
}

// 柔光效果
.soft-light {
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 30% 20%, $pink-light 0%, transparent 60%),
  radial-gradient(circle at 70% 80%, $purple-light 0%, transparent 60%);
  animation: softPulse 15s infinite ease-in-out;
}

@keyframes softPulse {
  0%, 100% { opacity: 0.6; }
  50% { opacity: 1; }
}

// 封面区域
.bg-wrap {
  position: relative;
  width: 100%;
  border-radius: 0 0 32px 32px;
  overflow: hidden;
  margin-bottom: 32px;
}

.bg-image-container {
  position: relative;
  width: 100%;
  height: 380px;
  overflow: hidden;
}

.love-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 1.5s ease, opacity 1s ease;
  opacity: 0.8;
}

.love-image.loaded {
  transform: scale(1.02);
  opacity: 1;
}

.bg-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(to bottom, rgba(0,0,0,0.1), rgba(0,0,0,0.3));
}

// 情侣信息
.love-wrap {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  align-items: center;
  gap: 24px;
  z-index: 10;
}

.person-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.avatar-container {
  position: relative;
}

.love-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  object-fit: cover;
  border: 4px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 4px 16px rgba(255, 107, 157, 0.3);
  transition: all 0.5s ease;
  opacity: 0;
}

.love-avatar.scale-in {
  opacity: 1;
  transform: scale(1);
}

.love-avatar.loading {
  opacity: 0;
  transform: scale(0.8);
}

.avatar-shine {
  position: absolute;
  inset: -2px;
  border-radius: 50%;
  background: linear-gradient(45deg, transparent, #ffd1e6, transparent);
  animation: shine 2s infinite;
  opacity: 0;
  z-index: -1;
}

.avatar-shine.active {
  opacity: 1;
}

@keyframes shine {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.name-text {
  font-size: 20px;
  font-weight: 600;
  color: #fff;
  text-shadow: 0 2px 8px rgba(0,0,0,0.3);
}

.couple-heart {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.love-img {
  width: 48px;
  height: 48px;
  animation: heartbeat 1.5s infinite ease-in-out;
}

@keyframes heartbeat {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.2); }
}

.view-count {
  color: #fff;
  font-size: 14px;
  text-shadow: 0 1px 4px rgba(0,0,0,0.3);
}

.view-number {
  font-weight: 600;
  font-size: 16px;
}

// 功能按钮
.float-btn {
  position: absolute;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.9);
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  box-shadow: 0 4px 12px rgba(255, 107, 157, 0.3);
  cursor: pointer;
  transition: all 0.3s ease;
  z-index: 10;
}

.float-btn:hover {
  transform: scale(1.1);
  box-shadow: 0 6px 16px rgba(255, 107, 157, 0.4);
}

.edit-button {
  top: 24px;
  right: 24px;
}

.share-button {
  bottom: 80px;
  right: 24px;
}

.btn-tooltip {
  position: absolute;
  right: 56px;
  background: rgba(0,0,0,0.7);
  color: #fff;
  padding: 4px 12px;
  border-radius: 16px;
  font-size: 12px;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.3s ease;
}

.float-btn:hover .btn-tooltip {
  opacity: 1;
}

// 波浪动画
.wave-container {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  overflow: hidden;
  line-height: 0;
  transform: rotate(180deg);
}

.waves {
  position: relative;
  display: block;
  width: calc(100% + 1.3px);
  height: 40px;
  transform: rotate(180deg);
}

// 时间统计区域
.time-section {
  margin-bottom: 48px;
  width: 100%;
}

.time-card {
  background: $gradient-pink;
  border-radius: 24px;
  padding: 24px;
  box-shadow: 0 8px 24px rgba(255, 145, 173, 0.2);
  text-align: center;
  margin-bottom: 24px;
}

.love-time-title1 {
  font-size: 18px;
  color: #d63384;
  margin-bottom: 16px;
  font-weight: 500;
}

.love-time1 {
  font-size: 16px;
  color: #5a2d48;
  line-height: 1.6;
}

.love-time1-item {
  display: inline-block;
  padding: 4px 8px;
  margin: 0 4px;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(255, 107, 157, 0.1);
}

.countdown-card {
  background: $pink-light;
  border-radius: 24px;
  padding: 16px 24px;
  box-shadow: 0 4px 16px rgba(255, 145, 173, 0.15);
  display: flex;
  align-items: center;
  gap: 12px;
  justify-content: center;
}

.heart-prefix {
  font-size: 20px;
  animation: heartbeat 1.5s infinite;
}

.countdown-text {
  font-size: 16px;
  color: #d63384;
  font-weight: 500;
}

.countdown-value {
  color: $pink-deep;
  font-weight: 600;
}

// 功能卡片
.card-wrap {
  display: flex;
  flex-direction: row;
  justify-content: center;
  align-items: stretch;
  gap: 20px;
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.card-content {
  flex: 1;
  display: flex;
  align-items: center;
  padding: 25px;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  will-change: transform, border-color, background-color;
  background: var(--background);
  min-width: 0;
  max-width: 350px;
  border: 2px solid transparent;
}

.card-content.active {
  background: $pink-light;
  border-color: $pink-deep;
}

.card-content:hover {
  transform: translateY(-6px);
  box-shadow: 0 12px 24px rgba(255, 107, 157, 0.15);
}

.card-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: #fff;
  box-shadow: 0 4px 8px rgba(0,0,0,0.1);
}

.card-right {
  margin-left: 16px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.card-title {
  font-size: 18px;
  color: #5a2d48;
  margin-bottom: 4px;
  font-weight: 600;
}

.card-desc {
  font-size: 14px;
  color: #a1668f;
}

.card-arrow {
  margin-left: auto;
  color: $pink-deep;
  font-size: 16px;
  opacity: 0.6;
  transition: opacity 0.3s ease;
}

.card-content:hover .card-arrow {
  opacity: 1;
  transform: translateX(4px);
}

// 内容面板动画
.content-panel {
  transition: all 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
  opacity: 0;
  transform: translateY(20px);
  height: 0;
  overflow: hidden;
}

.content-panel.show {
  opacity: 1;
  transform: translateY(0);
  height: auto;
  padding: 0 16px;
}

.content-panel.collapsed {
  opacity: 0;
  transform: translateY(20px);
  height: 0;
}

// 模态框
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
  backdrop-filter: blur(4px);
}

.glass-modal {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 24px;
  width: 90%;
  max-width: 800px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 12px 48px rgba(255, 107, 157, 0.2);
  border: 1px solid rgba(255, 192, 203, 0.3);
}

.modal-header {
  padding: 24px;
  border-bottom: 1px solid $pink-light;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.modal-title {
  font-size: 22px;
  color: #d63384;
  font-weight: 600;
}

.modal-close {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #a1668f;
  transition: all 0.3s ease;
}

.modal-close:hover {
  color: $pink-deep;
  transform: rotate(90deg);
}

.modal-form {
  padding: 24px;
}

.form-item {
  margin-bottom: 24px;
}

.form-label {
  display: block;
  margin-bottom: 8px;
  color: #5a2d48;
  font-weight: 500;
  font-size: 15px;
}

.form-input {
  width: 100%;
  padding: 12px 16px;
  border-radius: 12px;
  border: 1px solid $pink-light;
  background: rgba(255, 255, 255, 0.8);
  transition: all 0.3s ease;
  font-size: 14px;
}

.form-input:focus {
  outline: none;
  border-color: $pink-deep;
  box-shadow: 0 0 0 2px rgba(255, 107, 157, 0.1);
}

.couple-info-form {
  display: flex;
  gap: 24px;
  margin-bottom: 24px;
  align-items: center;
}

.person-info {
  flex: 1;
}

.heart-divider {
  font-size: 24px;
  animation: heartbeat 1.5s infinite;
}

.upload-wrapper {
  position: relative;
}

.image-preview {
  width: 100%;
  height: 180px;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.avatar-preview {
  width: 100%;
  height: 120px;
}

.bg-pink-light {
  background: $pink-light;
}

.bg-purple-light {
  background: $purple-light;
}

.image-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #a1668f;
}

.plus-icon {
  font-size: 24px;
}

.gradient-text {
  background: linear-gradient(90deg, $pink-deep, #9370db);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.upload-tip {
  font-size: 14px;
}

.switch-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
}

.custom-switch {
  width: 48px;
  height: 24px;
  border-radius: 12px;
  background: $pink-light;
  appearance: none;
  position: relative;
  cursor: pointer;
  transition: all 0.3s ease;
}

.custom-switch:checked {
  background: $pink-deep;
}

.custom-switch::before {
  content: '';
  position: absolute;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  top: 2px;
  left: 2px;
  background: #fff;
  transition: all 0.3s ease;
}

.custom-switch:checked::before {
  left: 26px;
}

.switch-label {
  color: #5a2d48;
  font-size: 14px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 16px;
  margin-top: 32px;
}

.btn-cancel {
  padding: 10px 24px;
  border-radius: 12px;
  border: 1px solid $pink-light;
  background: #fff;
  color: #5a2d48;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-cancel:hover {
  background: $pink-light;
}

.btn-confirm {
  padding: 10px 24px;
  border-radius: 12px;
  border: none;
  color: #fff;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-confirm:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(255, 107, 157, 0.3);
}

// 消息提示
.romantic-message {
  position: fixed;
  top: 24px;
  left: 50%;
  transform: translateX(-50%);
  padding: 12px 24px;
  border-radius: 32px;
  color: #fff;
  font-weight: 500;
  z-index: 1000;
  display: flex;
  align-items: center;
  gap: 8px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.15);
  animation: slideDown 0.3s ease-out;
}

.message-success {
  background: linear-gradient(90deg, #ff9a9e, #fecfef);
  border: 1px solid #ffd1e6;
}

.message-error {
  background: linear-gradient(90deg, #ff6b9d, #ffa8cc);
  border: 1px solid #ffb6c1;
}

.message-icon {
  font-size: 18px;
}

@keyframes slideDown {
  0% {
    top: -60px;
    opacity: 0;
  }
  100% {
    top: 24px;
    opacity: 1;
  }
}

// 无权限提示
.no-permission-container {
  position: fixed;
  inset: 0;
  background: #fff9fb;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
}

.no-permission-content {
  padding: 48px;
  border-radius: 24px;
  text-align: center;
  max-width: 400px;
  width: 90%;
  box-shadow: 0 12px 48px rgba(255, 107, 157, 0.15);
}

.lock-icon {
  font-size: 72px;
  margin-bottom: 24px;
  color: $pink-deep;
}

.no-permission-content h2 {
  font-size: 22px;
  color: #5a2d48;
  margin-bottom: 16px;
  font-weight: 600;
}

.no-permission-content p {
  font-size: 16px;
  color: #a1668f;
  margin-bottom: 32px;
}

.back-button {
  padding: 12px 32px;
  border-radius: 12px;
  border: none;
  color: #fff;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.back-button:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 16px rgba(255, 107, 157, 0.3);
}

// 通用动画
.text-glow {
  text-shadow: 0 0 12px rgba(255, 107, 157, 0.4);
}

.pulse {
  animation: pulse 2s infinite ease-in-out;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.8; }
}

// 响应式优化
@media (max-width: 768px) {
  .bg-image-container {
    height: 300px;
  }

  .love-wrap {
    gap: 16px;
  }

  .love-avatar {
    width: 64px;
    height: 64px;
  }

  .name-text {
    font-size: 16px;
  }

  .love-img {
    width: 36px;
    height: 36px;
  }

  .card-wrap {
    grid-template-columns: 1fr 1fr;
    gap: 16px;
  }

  .couple-info-form {
    flex-direction: column;
    gap: 16px;
  }

  .heart-divider {
    transform: rotate(90deg);
  }

  .time-card {
    padding: 16px;
  }

  .love-time1 {
    font-size: 14px;
  }

  .love-time1-item {
    padding: 2px 6px;
    margin: 0 2px;
  }
}

@media (max-width: 480px) {
  .card-wrap {
    grid-template-columns: 1fr;
  }

  .float-btn {
    width: 40px;
    height: 40px;
    font-size: 18px;
  }

  .edit-button {
    top: 16px;
    right: 16px;
  }

  .share-button {
    bottom: 70px;
    right: 16px;
  }

  .modal-title {
    font-size: 18px;
  }

  .modal-form {
    padding: 16px;
  }
}

/* 移动端样式 */
@media screen and (max-width: 767px) {
  .card-wrap {
    flex-direction: row;
    flex-wrap: nowrap;
    gap: 8px;
    padding: 10px;
    padding-top: 20px;
    overflow-x: hidden;
  }

  .card-content {
    flex: 1;
    padding: 12px 10px;
    min-width: 0;
    max-width: none;
    flex-direction: column;
    align-items: center;
    text-align: center;
    border-radius: 12px;
  }

  .card-icon {
    width: 36px;
    height: 36px;
    font-size: 1.1rem;
    margin-bottom: 6px;
    border-radius: 8px;
  }

  .card-right {
    margin-left: 0;
    display: flex;
    flex-direction: column;
    align-items: center;
  }

  .card-title {
    font-size: 0.85rem;
    letter-spacing: 0.03rem;
    line-height: 1.2;
    margin: 0;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    max-width: 100%;
  }

  .card-desc {
    display: none;
  }
}
</style>
