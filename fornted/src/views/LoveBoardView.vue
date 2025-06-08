<template>
  <div class="love-board-container">
    <!-- 封面 -->
    <div class="bg-wrap my-animation-slide-top">
      <!-- 背景图片 -->
      <img class="love-image my-el-image"
           :src="loveBoard?.bgCover"
           alt="背景图片">

      <!-- 对象 -->
      <div class="love-wrap transformCenter">
        <div>
          <img class="love-avatar"
               :class="{ loading: manAvatarLoading }"
               :src="loveBoard?.manCover || defaultManAvatar"
               @load="handleAvatarLoad('man')"
               alt="他的头像">
          <div class="love-title">
            <app-color-animate-text :texts="[loveBoard?.manName || '他']" :infinite="false"></app-color-animate-text>
          </div>
        </div>
        <div>
          <img class="love-img" src="@/assets/loveLike.svg" alt="心心">
        </div>
        <div>
          <img class="love-avatar"
               :class="{ loading: womanAvatarLoading }"
               :src="loveBoard?.womanCover || defaultWomanAvatar"
               @load="handleAvatarLoad('woman')"
               alt="她的头像">
          <div class="love-title">
            <app-color-animate-text :texts="[loveBoard?.womanName || '她']" :infinite="false"></app-color-animate-text>
          </div>
        </div>
      </div>

      <!-- 编辑按钮 -->
      <button v-if="isOwner" class="edit-button" @click="showEditModal">
        <span class="edit-icon">✏️</span>
      </button>

      <!-- 分享按钮 -->
      <button v-if="loveBoard?.status === 1" class="share-button" @click="handleShare">
        💌
      </button>

      <!-- 波浪动画 -->
      <div class="wave-container">
        <svg class="waves" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" viewBox="0 24 150 40" preserveAspectRatio="none" shape-rendering="auto">
          <defs>
            <path id="gentle-wave" d="M-160 44c30 0 58-18 88-18s 58 18 88 18 58-18 88-18 58 18 88 18 v44h-352z"></path>
          </defs>
          <g class="moving-waves">
            <use xlink:href="#gentle-wave" x="48" y="-1" fill="rgba(255,255,255,0.40)"></use>
            <use xlink:href="#gentle-wave" x="48" y="3" fill="rgba(255,255,255,0.35)"></use>
            <use xlink:href="#gentle-wave" x="48" y="5" fill="rgba(255,255,255,0.25)"></use>
            <use xlink:href="#gentle-wave" x="48" y="8" fill="rgba(255,255,255,0.20)"></use>
            <use xlink:href="#gentle-wave" x="48" y="13" fill="rgba(255,255,255,0.15)"></use>
            <use xlink:href="#gentle-wave" x="48" y="16" fill="rgba(255,255,255,0.45)"></use>
          </g>
        </svg>
      </div>
    </div>

    <!-- 内容 -->
    <div class="love-container">
      <div class="myCenter love-content">
        <!-- 时间 -->
        <div>
          <!-- 计时 -->
          <div>

            <div class="love-time-title1">
              这是我们一起走过的
            </div>
            <div class="love-time1">
              第
              <span class="love-time1-item">{{timing.year}}</span>
              年
              <span class="love-time1-item">{{timing.month}}</span>
              月
              <span class="love-time1-item">{{timing.day}}</span>
              日
              <span class="love-time1-item">{{timing.hour}}</span>
              时
              <span class="love-time1-item">{{timing.minute}}</span>
              分
              <span class="love-time1-item">{{timing.second}}</span>
              秒
            </div>
          </div>
          <!-- 倒计时 -->
          <div class="love-time-title2"
               v-if="loveBoard?.countdownTitle || loveBoard?.countdownTime">
            {{loveBoard?.countdownTitle}}: {{countdownChange}}
          </div>
        </div>
      </div>

      <!-- 卡片 -->
      <div class="card-wrap">
        <div class="card-content shadow-box-mini"
             :class="{ active: currentView === 'daily' }"
             data-type="daily"
             @click="handleDailyClick">
          <div class="card-icon">
            📝
          </div>
          <div class="card-right">
            <h3 class="card-title">{{ isMobile ? '点滴' : '点点滴滴' }}</h3>
            <div class="card-desc">💝 记录甜蜜瞬间</div>
          </div>
        </div>

        <div class="card-content shadow-box-mini"
             :class="{ active: currentView === 'album' }"
             data-type="album"
             @click="handleTimeAlbumClick">
          <div class="card-icon">
            📸
          </div>
          <div class="card-right">
            <h3 class="card-title">{{ isMobile ? '相册' : '时光相册' }}</h3>
            <div class="card-desc">✨ 定格美好回忆</div>
          </div>
        </div>

        <div class="card-content shadow-box-mini"
             :class="{ active: currentView === 'music' }"
             data-type="music"
             @click="handleMusicClick">
          <div class="card-icon">
            🎵
          </div>
          <div class="card-right">
            <h3 class="card-title">{{ isMobile ? '音乐' : '音乐专栏' }}</h3>
            <div class="card-desc">🎼 记录专属音乐</div>
          </div>
        </div>

        <div class="card-content shadow-box-mini"
             :class="{ active: currentView === 'wishes' }"
             data-type="wishes"
             @click="handleWishesClick">
          <div class="card-icon">
            🎁
          </div>
          <div class="card-right">
            <h3 class="card-title">{{ isMobile ? '祝福' : '祝福板' }}</h3>
            <div class="card-desc">🌟 收集世界祝愿</div>
          </div>
        </div>
      </div>

      <!-- 内容区域 -->
      <div v-if="currentView === 'daily' && loveBoard?.id" class="daily-moments-wrap">
        <DailyMoments
          :loveBoardId="String(loveBoard.id)"
          :isOwner="isOwner"
        />
      </div>

      <div v-if="currentView === 'album'" class="time-album-wrap">
        <TimeAlbum
          ref="timeAlbumRef"
          :loveBoardId="String(loveBoard?.id || '')"
          :isOwner="isOwner"
        />
      </div>

      <div v-if="currentView === 'music'" class="music-album-wrap">
        <MusicAlbum
          ref="musicAlbumRef"
          :loveBoardId="String(loveBoard?.id || '')"
          :isOwner="isOwner"
        />
      </div>

      <div v-if="currentView === 'wishes'" class="wishes-wrap">
        <WishesBoard
          :ownerId="loveBoard?.id || 0"
          :isOwner="isOwner"
        />
      </div>

    </div>

    <!-- 创建/编辑模态框 -->
    <div v-if="modalVisible" class="modal-overlay">
      <div class="modal-content">
        <div class="modal-header">
          <h2>{{loveBoard ? '编辑恋爱画板' : '创建恋爱画板'}}</h2>
          <button class="modal-close" @click="handleModalCancel">×</button>
        </div>
        <form class="modal-form" @submit.prevent="handleModalOk">
          <div class="form-item">
            <label>背景图片</label>
            <div class="upload-wrapper">
              <div class="image-preview" @click="showFileUploadDialog('bg')">
                <img v-if="formState.bgCover" :src="formState.bgCover" alt="背景图片" />
                <div v-else class="upload-placeholder">
                  <span class="plus-icon">+</span>
                  <div>点击上传背景图片</div>
                </div>
              </div>
            </div>
          </div>

          <div class="couple-info">
            <div class="person-info">
              <div class="form-item">
                <label>他的头像</label>
                <div class="upload-wrapper">
                  <div class="image-preview avatar-preview" @click="showFileUploadDialog('man')">
                    <img v-if="formState.manCover" :src="formState.manCover" alt="他的头像" />
                    <div v-else class="upload-placeholder">
                      <span class="plus-icon">+</span>
                      <div>点击上传头像</div>
                    </div>
                  </div>
                </div>
              </div>
              <div class="form-item">
                <label>他的名字</label>
                <input type="text" v-model="formState.manName" placeholder="请输入他的名字" required>
              </div>
            </div>

            <div class="person-info">
              <div class="form-item">
                <label>她的头像</label>
                <div class="upload-wrapper">
                  <div class="image-preview avatar-preview" @click="showFileUploadDialog('woman')">
                    <img v-if="formState.womanCover" :src="formState.womanCover" alt="她的头像" />
                    <div v-else class="upload-placeholder">
                      <span class="plus-icon">+</span>
                      <div>点击上传头像</div>
                    </div>
                  </div>
                </div>
              </div>
              <div class="form-item">
                <label>她的名字</label>
                <input type="text" v-model="formState.womanName" placeholder="请输入她的名字" required>
              </div>
            </div>
          </div>

          <div class="form-item">
            <label>在一起的时间</label>
            <input type="date" v-model="formState.timing" required>
          </div>

          <div class="form-item">
            <label>倒计时标题</label>
            <input type="text" v-model="formState.countdownTitle" placeholder="例如：结婚纪念日">
          </div>

          <div class="form-item" v-if="formState.countdownTitle">
            <label>倒计时时间</label>
            <input type="date" v-model="formState.countdownTime">
          </div>

          <div class="form-item">
            <label>是否公开</label>
            <div class="switch-wrapper">
              <input type="checkbox"
                     :checked="formState.status === 1"
                     @change="(e) => formState.status = e.target.checked ? 1 : 0">
              <span class="switch-label">{{formState.status === 1 ? '公开，可以分享给他人' : '私密，仅自己可见'}}</span>
            </div>
          </div>

          <div class="modal-footer">
            <button type="button" class="btn-cancel" @click="handleModalCancel">取消</button>
            <button type="submit" class="btn-confirm">确定</button>
          </div>
        </form>
      </div>
    </div>

    <!-- 图片裁剪组件 -->
    <AvatarCropper
      ref="cropperRef"
      :imageUrl="tempImageUrl"
      @success="handleCroppedImage"
    />

    <!-- 隐藏的文件输入框 -->
    <input
      type="file"
      ref="fileInput"
      style="display: none"
      accept="image/*"
      @change="handleFileChange"
    />

    <!-- 消息提示 -->
    <div v-if="showMessageText" :class="['message', `message-${showMessageType}`]">
      {{ showMessageText }}
    </div>

    <!-- 无权限提示界面 -->
    <div v-if="noPermission" class="no-permission-container">
      <div class="no-permission-content">
        <div class="lock-icon">🔒</div>
        <h2>这是一个私密的恋爱画板</h2>
        <p>主人设置了访问权限，暂时无法查看哦~</p>
        <button class="back-button" @click="$router.push('/home')">返回主页</button>
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
import { SPACE_LEVEL_ENUM } from '@/constants/space'
import { getSpaceVoByIdUsingGet } from '@/api/spaceController'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { getDefaultAvatar } from '@/utils/userUtils'

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

// 内容显示控制
const currentView = ref<'daily' | 'album' | 'music' | 'wishes' | null>('daily')

const router = useRouter()
const route = useRoute()

// 根据路由来源设置初始视图
onMounted(() => {
  // 如果是从相册详情页返回，显示相册界面
  if (prevRoute?.name === 'TimeAlbumDetail') {
    currentView.value = 'album'
  } else if (prevRoute?.name === 'MusicAlbumDetail') {
    // 如果是从音乐详情页返回，显示音乐专栏
    currentView.value = 'music'
  } else {
    currentView.value = 'daily'
  }

  fetchLoveBoard()
  // 启动计时器
  setInterval(() => {
    updateTiming()
    updateCountdown()
  }, 1000)

  checkMobile()
  window.addEventListener('resize', checkMobile)
})

const loginUserStore = useLoginUserStore()

// 添加isOwner计算属性
const isOwner = computed(() => {
  return loginUserStore.loginUser?.id === loveBoard.value?.userId
})

// 添加头像加载状态
const manAvatarLoading = ref(true)
const womanAvatarLoading = ref(true)

// 获取默认头像的计算属性
const defaultManAvatar = computed(() => getDefaultAvatar(loveBoard.value?.manName || '他'))
const defaultWomanAvatar = computed(() => getDefaultAvatar(loveBoard.value?.womanName || '她'))

// 处理头像加载完成
const handleAvatarLoad = (type: 'man' | 'woman') => {
  if (type === 'man') {
    manAvatarLoading.value = false
  } else {
    womanAvatarLoading.value = false
  }
}

// 显示消息的方法
const showMessage = (text: string, type: 'error' | 'success' | 'info' = 'error') => {
  showMessageText.value = text
  showMessageType.value = type

  // 清除之前的定时器
  if (messageTimer.value) {
    clearTimeout(messageTimer.value)
  }

  // 3秒后自动关闭
  messageTimer.value = window.setTimeout(() => {
    showMessageText.value = ''
  }, 3000)
}

const noPermission = ref(false)

// 获取恋爱画板数据
const fetchLoveBoard = async () => {
  try {
    // 从路由参数中获取恋爱板ID
    const loveBoardId = route.params.id
    let res

    if (loveBoardId) {
      // 如果有ID参数，使用getLoveBoardById接口
      res = await getLoveBoardByIdUsingGet({ id: String(loveBoardId) })

      if (res.data.code === 40101) {
        // 无权限访问
        noPermission.value = true
        return
      }
    } else {
      // 如果没有ID参数，使用getMyLoveBoard接口
      res = await getMyLoveBoardUsingGet()
    }

    if (res.data.code === 0) {
      loveBoard.value = res.data.data
      if (!loveBoard.value && !route.params.id) {
        // 只有在查看自己的恋爱板且不存在时，才显示创建弹窗
        modalVisible.value = true
      }
    }
  } catch (error) {
    console.error('获取恋爱画板失败:', error)
    showMessage('获取恋爱画板失败')
  }
}

// 显示编辑模态框
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

// 处理模态框确认
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

// 处理模态框取消
const handleModalCancel = () => {
  modalVisible.value = false
}

// 显示文件选择对话框
const showFileUploadDialog = (type: 'bg' | 'man' | 'woman') => {
  currentUploadType.value = type
  if (fileInput.value) {
    fileInput.value.click()
  }
}

// 处理文件选择
const handleFileChange = async (e: Event) => {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return

  if (currentUploadType.value === 'bg') {
    // 背景图片直接上传
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
    // 头像需要裁剪
    tempImageUrl.value = URL.createObjectURL(file)
    if (cropperRef.value) {
      cropperRef.value.openModal()
    }
  }
  // 清空 input 值，允许重复选择同一文件
  (e.target as HTMLInputElement).value = ''
}

// 处理裁剪后的头像
const handleCroppedImage = async (file: File) => {
  try {
    const res = await uploadPostImageUsingPost({}, {}, file)
    if (res.data.code === 0) {
      showMessage('头像上传成功', 'success')
      // 根据当前上传类型更新对应的图片URL
      if (currentUploadType.value === 'man') {
        formState.manCover = res.data.data.url
      } else if (currentUploadType.value === 'woman') {
        formState.womanCover = res.data.data.url
      }
      // 关闭裁剪框
      if (cropperRef.value) {
        cropperRef.value.closeModal()
      }
    }
  } catch (error) {
    console.error('头像上传失败:', error)
    showMessage('头像上传失败')
  }
}

// 显示开发中消息
const showDevelopingMessage = () => {
  showMessage('功能开发中...', 'info')
}

// 计时器更新函数
const updateTiming = () => {
  if (!loveBoard.value?.timing) return

  const startDate = dayjs(loveBoard.value.timing)
  const now = dayjs()

  // 计算年份差
  timing.year = now.diff(startDate, 'year')

  // 计算月份差 (减去整年的部分)
  const afterYears = startDate.add(timing.year, 'year')
  timing.month = now.diff(afterYears, 'month')

  // 计算天数差 (减去整月的部分)
  const afterMonths = afterYears.add(timing.month, 'month')
  timing.day = now.diff(afterMonths, 'day')

  // 计算小时差 (减去整天的部分)
  const afterDays = afterMonths.add(timing.day, 'day')
  timing.hour = now.diff(afterDays, 'hour')

  // 计算分钟差 (减去整小时的部分)
  const afterHours = afterDays.add(timing.hour, 'hour')
  timing.minute = now.diff(afterHours, 'minute')

  // 计算秒数差 (减去整分钟的部分)
  const afterMinutes = afterHours.add(timing.minute, 'minute')
  timing.second = now.diff(afterMinutes, 'second')
}

// 倒计时更新函数
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

// 处理点点滴滴点击
const handleDailyClick = () => {
  if (currentView.value === 'daily') {
    // 添加收起动画
    const container = document.querySelector('.daily-moments-wrap');
    if (container) {
      container.classList.add('collapsed');
      setTimeout(() => {
        currentView.value = null;
      }, 300);
    }
  } else {
    currentView.value = 'daily';
  }
}

// 处理时光相册点击
const handleTimeAlbumClick = () => {
  if (currentView.value === 'album') {
    // 添加收起动画
    const container = document.querySelector('.time-album-wrap');
    if (container) {
      container.classList.add('collapsed');
      setTimeout(() => {
        currentView.value = null;
      }, 300);
    }
  } else {
    currentView.value = 'album';
  }
}

// 处理音乐专栏点击
const handleMusicClick = () => {
  if (currentView.value === 'music') {
    // 添加收起动画
    const container = document.querySelector('.music-album-wrap');
    if (container) {
      container.classList.add('collapsed');
      setTimeout(() => {
        currentView.value = null;
      }, 300);
    }
  } else {
    currentView.value = 'music';
  }
}

// 处理祝福板点击
const handleWishesClick = () => {
  if (currentView.value === 'wishes') {
    // 添加收起动画
    const container = document.querySelector('.wishes-wrap');
    if (container) {
      container.classList.add('collapsed');
      setTimeout(() => {
        currentView.value = null;
      }, 300);
    }
  } else {
    currentView.value = 'wishes';
  }
}

// 添加分享功能
const handleShare = () => {
  if (!loveBoard.value?.id) return

  // 构造分享链接
  const shareUrl = `${window.location.origin}/loveboard/${loveBoard.value.id}`

  // 复制链接到剪贴板
  navigator.clipboard.writeText(shareUrl)
    .then(() => {
      showMessage('分享链接已复制到剪贴板', 'success')
    })
    .catch(() => {
      showMessage('复制链接失败')
    })
}

// 添加移动端检测
const isMobile = ref(false)

// 检测是否为移动端
const checkMobile = () => {
  isMobile.value = window.innerWidth <= 767
}

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
})
</script>

<style scoped>
.love-board-container{
  margin-top: -84px;
  margin-left: -28px;
  margin-right: -28px;
}
.bg-wrap {
  position: relative;
  top: 0;
  left: 0;
  width: 100%;
  height: 55vh;
  overflow: hidden;
  background: linear-gradient(45deg, #6e45e1, #88d3ce);
}

.love-image {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  filter: brightness(0.85);
  transform: scale(1.1);
}

.love-wrap {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -60%);
  width: 90%;
  max-width: 950px;
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(10px);
  border-radius: 3em;
  display: flex;
  align-items: center;
  justify-content: space-around;
  padding: 50px 70px 30px 70px;

}

.love-avatar {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  border: 4px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 0 20px rgba(0, 0, 0, 0.2);
  object-fit: cover;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  opacity: 0;
  transform: scale(0.8);
  animation: avatarFadeIn 0.6s ease forwards;
}

@keyframes avatarFadeIn {
  from {
    opacity: 0;
    transform: scale(0.8);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.love-avatar.loading {
  position: relative;
}

.love-avatar.loading::after {
  content: '';
  position: absolute;
  top: -4px;
  left: -4px;
  right: -4px;
  bottom: -4px;
  border-radius: 50%;
  border: 4px solid rgba(255, 255, 255, 0.1);
  border-top-color: rgba(255, 255, 255, 0.8);
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.love-title {
  text-align: center;
  color: #fff;
  font-size: 1.5rem;
  margin-top: 1rem;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
  font-weight: 600;
}

.love-img {
  width: 80px;
  height: 80px;
  animation: heartbeat 1.5s ease-in-out infinite;
  filter: drop-shadow(0 0 10px rgba(255, 255, 255, 0.6));
}

@keyframes heartbeat {
  0% {
    transform: scale(1);
  }
  25% {
    transform: scale(1.1);
  }
  40% {
    transform: scale(1);
  }
  60% {
    transform: scale(1.1);
  }
  100% {
    transform: scale(1);
  }
}

.wave-container {
  position: absolute;
  bottom: -2px;
  left: 0;
  width: 100%;
  overflow: hidden;
  line-height: 0;
}

.waves {
  position: relative;
  width: 100%;
  height: 16vh;
  margin-bottom: -7px;
  min-height: 100px;
  max-height: 150px;
}

.moving-waves > use {
  animation: move-forever 40s cubic-bezier(0.55, 0.5, 0.45, 0.5) infinite;
}

.moving-waves > use:first-child {
  animation-delay: -2s;
  animation-duration: 11s;
}

.moving-waves > use:nth-child(2) {
  animation-delay: -4s;
  animation-duration: 13s;
}

.moving-waves > use:nth-child(3) {
  animation-delay: -3s;
  animation-duration: 15s;
}

.moving-waves > use:nth-child(4) {
  animation-delay: -4s;
  animation-duration: 20s;
}

.moving-waves > use:nth-child(5) {
  animation-delay: -4s;
  animation-duration: 25s;
}

.moving-waves > use:nth-child(6) {
  animation-delay: -3s;
  animation-duration: 30s;
}

@keyframes move-forever {
  0% {
    transform: translate3d(85px, 0, 0);
  }
  100% {
    transform: translate3d(-90px, 0, 0);
  }
}

@media screen and (max-width: 768px) {
  .waves {
    height: 80px;
    min-height: 80px;
    transform: scaleY(1.2);
  }

  .moving-waves > use {
    animation-duration: 20s !important;
    opacity: 0.8;
  }
}

.love-container {
  position: relative;
  width: 100%;
  min-height: 45vh;
  background-image: linear-gradient(90deg, rgba(0, 0, 0, 0.06) 2px, transparent 2px);
  background-size: 3rem 2rem;
  padding: 2rem 0;
  padding-bottom: 80px;
}

.love-content {
  max-width: 1200px;
  overflow: hidden;
  margin: 20px auto 0;
  user-select: none;
}

.love-time-title1 {
  font-size: clamp(1.5rem, 4vw, 2.2rem);
  font-weight: 600;
  letter-spacing: 0.1rem;
  line-height: 1.5;
  text-align: center;
  background: linear-gradient(45deg, #ff6b95, #ff8e53, #ffd700, #90ee90, #00ffff, #1e90ff, #9370db);
  background-size: 300% 300%;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  animation: gradientText 8s ease infinite;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  position: relative;
  padding: 0 20px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
}

@keyframes gradientText {
  0% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0% 50%;
  }
}

.love-time-title1::before,
.love-time-title1::after {
  content: '❤️';
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  font-size: 1.8rem;
  -webkit-text-fill-color: initial;
  animation: heartbeat 1.5s ease-in-out infinite;
}

.love-time-title1::before {
  left: -10px;
}

.love-time-title1::after {
  right: -10px;
}

@keyframes heartbeat {
  0% {
    transform: translateY(-50%) scale(1);
  }
  25% {
    transform: translateY(-50%) scale(1.1);
  }
  50% {
    transform: translateY(-50%) scale(1);
  }
  75% {
    transform: translateY(-50%) scale(1.1);
  }
  100% {
    transform: translateY(-50%) scale(1);
  }
}

.love-time1 {
  text-align: center;
  font-size: clamp(1rem, 2.5vw, 2rem);
  font-weight: 700;
  margin: 20px 0;
  padding: 0 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: nowrap;
  gap: 4px;
  white-space: nowrap;
  overflow: hidden;
}

.love-time1-item {
  font-size: clamp(1.5rem, 3.5vw, 3rem);
  font-weight: 700;
  color: var(--themeBackground);
  text-shadow: 0 0 15px rgba(110, 69, 225, 0.3);
  padding: 0.3rem 0.4rem;
  min-width: 2.5rem;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  backdrop-filter: none;
  border: none;
  transition: all 0.3s ease;
}

.love-time1-item:hover {
  transform: translateY(-2px);
  text-shadow: 0 0 20px rgba(110, 69, 225, 0.5);
}

.love-time-title2 {
  text-align: center;
  font-size: clamp(1rem, 2.5vw, 1.5rem);
  line-height: 1.5;
  font-weight: 600;
  letter-spacing: 1px;
  color: var(--themeBackground);
  margin-top: 20px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  padding: 0 20px;
}

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

.card-content:hover {
  transform: translateY(-8px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15);
}

.card-content:hover .card-icon {
  transform: scale(1.1);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.15);
}

.card-content.active {
  border-color: var(--themeBackground);
  background: rgba(110, 69, 225, 0.05);
  transform: translateY(-2px);
}

.card-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 2rem;
  transition: all 0.3s ease;
  flex-shrink: 0;
}

.card-right {
  margin-left: 1rem;
  min-width: 0;
}

.card-title {
  font-size: 1.2rem;
  letter-spacing: 0.1rem;
  line-height: 2;
  font-weight: 600;
  color: var(--themeBackground);
  white-space: nowrap;
}

.card-desc {
  font-size: 1rem;
  letter-spacing: 0.1rem;
  color: #777777;
}

/* 移动端样式 */
@media screen and (max-width: 767px) {
  .card-wrap {
    gap: 10px;
    padding: 10px;
    padding-top: 40px;
  }

  .card-content {
    padding: 15px;
  }

  .card-icon {
    width: 40px;
    height: 40px;
    font-size: 1.5rem;
  }

  .card-right {
    margin-left: 0.5rem;
  }

  .card-title {
    font-size: 1rem;
    letter-spacing: 0.05rem;
    line-height: 1.5;
  }

  .card-desc {
    display: none;
  }

  /* 移动端标题文本修改 */
  .card-content[data-type="daily"] .card-title {
    content: "点滴";
  }

  .card-content[data-type="album"] .card-title {
    content: "相册";
  }

  .card-content[data-type="music"] .card-title {
    content: "音乐";
  }

  .card-content[data-type="wishes"] .card-title {
    content: "祝福";
  }
}

.edit-button {
  position: absolute;
  top: 88%;
  right: 20px;
  transform: translateY(-50%);
  background: rgba(255, 255, 255, 0.25);
  backdrop-filter: blur(10px);
  padding: 12px;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  color: white;
  border: 2px solid rgba(255, 255, 255, 0.3);
  width: 50px;
  height: 50px;
  font-size: 24px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  z-index: 999;
}

.edit-button:hover {
  background: rgba(255, 255, 255, 0.3);
}

@media screen and (max-width: 800px) {
  .love-wrap {
    border-radius: 1.5em;
    padding: 40px 30px 10px 30px;
  }

  .love-avatar {
    width: 120px;
    height: 120px;
  }

  .love-img {
    width: 100px;
    height: 100px;
  }

  .love-time1 {
    font-size: 1.4rem;
  }

  .love-time1-item {
    font-size: 3rem;
    padding: 0.4rem 0.8rem;
  }
}

@media screen and (max-width: 600px) {
  .love-wrap {
    padding: 30px 20px 10px 20px;
  }

  .love-avatar {
    width: 100px;
    height: 100px;
  }

  .love-img {
    width: 80px;
    height: 80px;
  }

  .love-time1 {
    font-size: 0.9rem;
    margin: 10px 0;
    padding: 0 5px;
  }

  .love-time1-item {
    font-size: 1.2rem;
    padding: 0.2rem 0.4rem;
    min-width: 1.8rem;
    margin: 0 1px;
  }

  .love-time-title2 {
    font-size: 1.2rem;
  }

  .card-wrap {
    gap: 10px;
    padding: 0 10px;
    padding-top: 24px;
  }

  .card-content {
    flex-direction: column;
    align-items: center;
    text-align: center;
    padding: 15px;
  }

  .card-icon {
    width: 40px;
    height: 40px;
    font-size: 1.5rem;
    margin-bottom: 8px;
  }

  .card-right {
    margin-left: 0;
    margin-top: 8px;
  }

  .card-title {
    font-size: 0.9rem;
    line-height: 1.2;
    letter-spacing: 0.05rem;
  }

  .card-desc {
    font-size: 0.8rem;
  }

  .share-button {
    right: auto;
    left: 20px;
    width: 40px;
    height: 40px;
    font-size: 20px;
    top: 50%;
  }

  .share-button:hover {
    transform: translateY(-50%) scale(1.1);
  }

  .share-button:active {
    transform: translateY(-50%) scale(0.95);
  }
}

@media screen and (min-width: 601px) {
  .card-wrap {
    display: flex;
    padding: 0 20px;
  }

  .card-content {
    width: 100%;
    margin: 15px auto;
    display: flex;
    flex-direction: row;
  }
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(6px);
  display: flex;
  justify-content: center;
  align-items: center;
  animation: fadeIn 0.3s ease;
}

.modal-content {
  background: linear-gradient(145deg, #ffffff, #f5f5f5);
  border-radius: 20px;
  padding: 30px;
  width: 84%;
  max-width: 600px;
  max-height: 84vh;
  overflow-y: auto;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.2);
  animation: slideUp 0.3s ease;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  padding-bottom: 15px;
  border-bottom: 2px solid rgba(110, 69, 225, 0.1);
}

.modal-header h2 {
  font-size: 24px;
  color: #6e45e1;
  font-weight: 600;
  margin: 0;
}

.modal-close {
  background: none;
  border: none;
  font-size: 28px;
  cursor: pointer;
  color: #666;
  transition: all 0.3s ease;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-close:hover {
  background: rgba(110, 69, 225, 0.1);
  color: #6e45e1;
}

.modal-form {
  display: flex;
  flex-direction: column;
  gap: 25px;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.form-item label {
  font-weight: 600;
  color: #333;
  font-size: 15px;
}

.form-item input[type="text"],
.form-item input[type="date"] {
  padding: 12px 16px;
  border: 2px solid rgba(110, 69, 225, 0.1);
  border-radius: 12px;
  font-size: 15px;
  transition: all 0.3s ease;
  background: white;
}

.form-item input[type="text"]:focus,
.form-item input[type="date"]:focus {
  border-color: #6e45e1;
  outline: none;
  box-shadow: 0 0 0 3px rgba(110, 69, 225, 0.1);
}

.upload-wrapper {
  border: 2px dashed rgba(110, 69, 225, 0.2);
  border-radius: 16px;
  padding: 15px;
  cursor: pointer;
  transition: all 0.3s ease;
  background: white;
}

.upload-wrapper:hover {
  border-color: #6e45e1;
  background: rgba(110, 69, 225, 0.02);
}

.image-preview {
  width: 100%;
  height: 200px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background: rgba(110, 69, 225, 0.02);
  border-radius: 12px;
  overflow: hidden;
}

.image-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-preview {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  margin: 0 auto;
}

.upload-placeholder {
  text-align: center;
  color: #666;
}

.plus-icon {
  font-size: 32px;
  color: #6e45e1;
  margin-bottom: 10px;
  display: block;
}

.switch-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
}

.switch-wrapper input[type="checkbox"] {
  appearance: none;
  width: 50px;
  height: 26px;
  background: #eee;
  border-radius: 13px;
  position: relative;
  cursor: pointer;
  transition: all 0.3s ease;
}

.switch-wrapper input[type="checkbox"]:checked {
  background: #6e45e1;
}

.switch-wrapper input[type="checkbox"]::before {
  content: '';
  position: absolute;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: white;
  top: 2px;
  left: 2px;
  transition: all 0.3s ease;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.switch-wrapper input[type="checkbox"]:checked::before {
  left: 26px;
}

.switch-label {
  color: #333;
  font-size: 15px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 15px;
  margin-top: 30px;
  padding-top: 20px;
  border-top: 2px solid rgba(110, 69, 225, 0.1);
}

.btn-cancel,
.btn-confirm {
  padding: 12px 24px;
  border-radius: 12px;
  cursor: pointer;
  font-size: 15px;
  font-weight: 600;
  border: none;
  transition: all 0.3s ease;
}

.btn-cancel {
  background: rgba(110, 69, 225, 0.1);
  color: #6e45e1;
}

.btn-confirm {
  background: #6e45e1;
  color: white;
}

.btn-cancel:hover {
  background: rgba(110, 69, 225, 0.2);
}

.btn-confirm:hover {
  background: #5835c9;
}

.couple-info {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes slideUp {
  from {
    transform: translateY(20px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

@media screen and (max-width: 600px) {
  .modal-content {
    padding: 20px;
  }

  .couple-info {
    grid-template-columns: 1fr;
  }

  .btn-cancel,
  .btn-confirm {
    padding: 10px 20px;
  }
}

/* 消息提示样式优化 */
.message {
  position: fixed;
  top: 20px;
  left: 50%;
  transform: translateX(-50%);
  padding: 12px 24px;
  border-radius: 8px;
  color: white;
  font-size: 14px;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  animation: messageSlideIn 0.3s ease;
}

.message-error {
  background-color: #ff4d4f;
}

.message-success {
  background-color: #52c41a;
}

.message-info {
  background-color: #1890ff;
}

@keyframes messageSlideIn {
  from {
    opacity: 0;
    transform: translate(-50%, -20px);
  }
  to {
    opacity: 1;
    transform: translate(-50%, 0);
  }
}

/* 点点滴滴弹窗样式优化 */
.daily-moments-modal,
.daily-moments-content,
.daily-moments-header {
  display: none;
}

.daily-moments-wrap,
.time-album-wrap {
  position: relative;
  overflow: hidden;
  opacity: 1;
  max-height: 2000px;
  padding-top: 40px;
  transition: all 0.5s cubic-bezier(0.4, 0, 0.2, 1);
  will-change: max-height, opacity;
}

.daily-moments-wrap.collapsed,
.time-album-wrap.collapsed {
  max-height: 0;
  opacity: 0;
  margin: 0;
  padding: 0;
}

/* 内容区域容器 */
.content-container {
  position: relative;
  overflow: hidden;
  transition: height 0.5s cubic-bezier(0.4, 0, 0.2, 1);
  will-change: height;
}

.wishes-wrap {
  position: relative;
  overflow: hidden;
  opacity: 1;
  max-height: 2000px;
  transition: all 0.5s cubic-bezier(0.4, 0, 0.2, 1);
  will-change: max-height, opacity;
}

.wishes-wrap.collapsed {
  max-height: 0;
  opacity: 0;
  margin: 0;
}

.share-button {
  position: fixed;
  top: 66%;
  left: 20px;
  transform: translateY(-50%);
  background: rgba(255, 255, 255, 0.25);
  backdrop-filter: blur(10px);
  padding: 12px;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  color: white;
  border: 2px solid rgba(255, 255, 255, 0.3);
  width: 40px;
  height: 40px;
  font-size: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  z-index: 100;
}

.share-button:hover {
  transform: translateY(-50%) scale(1.1);
  background: rgba(255, 255, 255, 0.35);
  border-color: rgba(255, 255, 255, 0.5);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.15);
}

.share-button:active {
  transform: translateY(-50%) scale(0.95);
}

.no-permission-container {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f6d3d7 0%, #e6b4c3 100%);
}

.no-permission-content {
  background: rgba(255, 255, 255, 0.95);
  padding: 40px;
  border-radius: 20px;
  text-align: center;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
  animation: fadeInUp 0.6s ease;
  max-width: 90%;
  width: 400px;
}

.lock-icon {
  font-size: 48px;
  margin-bottom: 20px;
  animation: bounce 2s infinite;
}

.no-permission-content h2 {
  color: #ff6b95;
  margin-bottom: 16px;
  font-size: 24px;
}

.no-permission-content p {
  color: #666;
  margin-bottom: 30px;
  font-size: 16px;
}

.back-button {
  background: #ff6b95;
  color: white;
  border: none;
  padding: 12px 30px;
  border-radius: 25px;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.back-button:hover {
  background: #ff4f7b;
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(255, 107, 149, 0.3);
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes bounce {
  0%, 20%, 50%, 80%, 100% {
    transform: translateY(0);
  }
  40% {
    transform: translateY(-20px);
  }
  60% {
    transform: translateY(-10px);
  }
}

.music-album-wrap {
  position: relative;
  overflow: hidden;
  opacity: 1;
  max-height: 2000px;
  padding-top: 40px;
  transition: all 0.5s cubic-bezier(0.4, 0, 0.2, 1);
  will-change: max-height, opacity;
}

.music-album-wrap.collapsed {
  max-height: 0;
  opacity: 0;
  margin: 0;
  padding: 0;
}
</style>
