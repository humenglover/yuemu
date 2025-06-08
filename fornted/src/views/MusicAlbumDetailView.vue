<template>
  <div class="music-album-detail">
    <!-- Loading state -->
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>

    <!-- Error state -->
    <div v-else-if="error" class="error-state">
      <p>{{ error }}</p>
      <button @click="retryLoading">重试</button>
    </div>

    <!-- Content -->
    <div v-else class="music-album-detail">
      <div class="album-header"
           :class="{ 'expanded': isHeaderExpanded }"
           @click="toggleHeader">
        <div class="drag-handle">
          <div class="handle-line"></div>
        </div>
        <div class="album-background">
          <img :src="album?.coverUrl || getDefaultAvatar('album')" :alt="album?.albumName">
        </div>
        <div class="album-content">
          <div class="album-info">
            <div class="album-cover">
              <img :src="album?.coverUrl || getDefaultAvatar('album')" :alt="album?.albumName">
            </div>
            <div class="album-meta">
              <h1>{{ album?.albumName }}</h1>
              <p class="description">{{ album?.description }}</p>
              <div class="stats">
                <span>
                  📅 创建于 {{ formatDate(album?.createTime) }}
                </span>
                <span>
                  🎵 {{ audioFiles.length }} 首音乐
                </span>
              </div>
            </div>
          </div>
          <!-- 只有主人才能看到上传按钮 -->
          <div class="album-actions" v-if="isOwner">
            <button class="upload-btn" @click="openUploadModal">
              <span class="btn-icon">⬆️</span>
            </button>
          </div>
        </div>
      </div>

      <!-- Music list -->
      <div class="music-grid" v-if="audioFiles.length > 0">
        <div v-for="(audio, index) in audioFiles"
             :key="audio.id"
             class="music-card"
             :class="{ 'playing': currentPlaying?.id === audio.id }">
          <div class="music-card-cover"
               @click="playAudio(audio)"
               :class="{ 'paused': currentPlaying?.id === audio.id && !isPlaying }">
            <img :src="getAudioCover(audio)" :alt="audio.title">
            <div class="play-overlay">
              <div class="play-button">
                <span v-if="currentPlaying?.id === audio.id && isPlaying">⏸️</span>
                <span v-else>▶️</span>
              </div>
            </div>
          </div>
          <div class="music-card-info">
            <p class="music-title">{{ audio.title }}</p>
            <p class="music-artist">{{ audio.artist || '未知艺术家' }}</p>
          </div>
          <!-- 只有主人才能看到删除按钮 -->
          <div class="music-card-actions" v-if="isOwner">
            <button class="action-btn delete" @click.stop="showDeleteConfirm(audio)">
              🗑️
            </button>
          </div>
        </div>
      </div>

      <!-- Empty state -->
      <div v-else class="empty-state">
        <div class="empty-icon">🎵</div>
        <p>{{ isOwner ? '还没有音乐，快来上传吧~' : '还没有音乐~' }}</p>
      </div>

      <!-- Upload modal -->
      <div v-if="showUploadModal" class="modal-overlay">
        <div class="modal-content upload-modal">
          <div class="modal-header">
            <h2>🎵 上传音乐</h2>
            <button class="modal-close" @click="closeUploadModal">
              ❌
            </button>
          </div>

          <!-- Upload Area -->
          <div class="upload-container">
            <!-- Left Panel: File Selection -->
            <div class="upload-left-panel">
              <div class="upload-zone"
                   @click="triggerFileInput"
                   @drop.prevent="handleFileDrop"
                   @dragover.prevent
                   @dragenter.prevent>
                <input
                  type="file"
                  ref="fileInput"
                  style="display: none"
                  accept=".mp3,.wav,.ogg,.m4a"
                  @change="handleFileSelect"
                  multiple
                >
                <div v-if="!uploadQueue.length" class="upload-placeholder">
                  <div class="upload-icon-wrapper">
                    ⬆️
                    <div class="upload-pulse"></div>
                  </div>
                  <h3>点击或拖拽音频文件到此处</h3>
                  <div class="upload-formats">
                    <span class="format-tag">MP3</span>
                    <span class="format-tag">WAV</span>
                    <span class="format-tag">OGG</span>
                    <span class="format-tag">M4A</span>
                  </div>
                  <div class="upload-hint">
                    ℹ️ 支持多文件上传
                  </div>
                </div>
              </div>

              <!-- Selected Files List -->
              <div class="selected-files" v-if="uploadQueue.length > 0">
                <div class="files-header">
                  <h4>🎵 已选择的音乐</h4>
                  <span class="file-count">{{ uploadQueue.length }}个文件</span>
                </div>
                <div v-for="(file, index) in uploadQueue"
                     :key="index"
                     class="file-item"
                     :class="{ 'active': currentEditIndex === index }"
                     @click="currentEditIndex = index">
                  <div class="file-icon">
                    <img v-if="file.coverPreview"
                         :src="file.coverPreview"
                         class="file-cover"
                         alt="音乐封面" />
                    <span v-else>🎵</span>
                  </div>
                  <div class="file-info">
                    <div class="file-name">{{ file.title || file.name }}</div>
                    <div class="file-meta">{{ file.artist || '未知艺术家' }}</div>
                  </div>
                </div>
              </div>
            </div>

            <!-- Right Panel: Metadata Form -->
            <div class="upload-right-panel" v-if="uploadQueue.length > 0">
              <div class="metadata-form">
                <div class="form-header">
                  <h3>ℹ️ 音乐信息</h3>
                  <p class="current-file">
                    ✏️ 正在编辑: {{ uploadQueue[currentEditIndex]?.name }}
                  </p>
                </div>

                <!-- Cover Upload -->
                <div class="cover-upload-section">
                  <div class="cover-preview"
                       :class="{ empty: !uploadQueue[currentEditIndex]?.coverPreview }"
                       @click="() => handleCoverClick(currentEditIndex)">
                    <img v-if="uploadQueue[currentEditIndex]?.coverPreview"
                         :src="uploadQueue[currentEditIndex].coverPreview"
                         alt="封面预览" />
                    <div v-else class="cover-placeholder">
                      🖼️
                      <span>点击设置封面</span>
                    </div>
                  </div>
                  <input
                    type="file"
                    :ref="el => coverInputs[currentEditIndex] = el"
                    style="display: none"
                    accept="image/*"
                    @change="(event) => handleCoverSelect(event, currentEditIndex)"
                  >
                </div>

                <!-- Form Fields -->
                <div class="form-fields">
                  <div class="form-group">
                    <label>
                      🎵 歌曲名称 <span class="required">*</span>
                    </label>
                    <input
                      v-model="uploadQueue[currentEditIndex].title"
                      type="text"
                      placeholder="请输入歌曲名称"
                      :disabled="isUploading"
                      required
                    >
                  </div>

                  <div class="form-group">
                    <label>
                      👤 艺术家 <span class="required">*</span>
                    </label>
                    <input
                      v-model="uploadQueue[currentEditIndex].artist"
                      type="text"
                      placeholder="请输入艺术家名称"
                      :disabled="isUploading"
                      required
                    >
                  </div>

                  <div class="form-group">
                    <label>
                      💿 专辑
                    </label>
                    <input
                      v-model="uploadQueue[currentEditIndex].album"
                      type="text"
                      placeholder="请输入专辑名称"
                      :disabled="isUploading"
                    >
                  </div>

                  <div class="form-group">
                    <label>
                      🎸 流派
                    </label>
                    <div class="genre-input-group">
                      <select
                        v-if="!isCustomGenre"
                        v-model="uploadQueue[currentEditIndex].genre"
                        :disabled="isUploading"
                        class="genre-select"
                      >
                        <option value="">请选择流派</option>
                        <option v-for="category in categoryList" :key="category" :value="category">
                          {{ category }}
                        </option>
                      </select>
                      <input
                        v-else
                        v-model="uploadQueue[currentEditIndex].genre"
                        type="text"
                        placeholder="请输入自定义流派"
                        :disabled="isUploading"
                      >
                      <button
                        type="button"
                        class="toggle-genre-btn"
                        @click="isCustomGenre = !isCustomGenre"
                        :disabled="isUploading"
                      >
                        {{ isCustomGenre ? '选择预设' : '自定义' }}
                      </button>
                    </div>
                  </div>

                  <div class="form-group full-width">
                    <label>
                      📝 描述
                    </label>
                    <textarea
                      v-model="uploadQueue[currentEditIndex].description"
                      placeholder="请输入音乐描述"
                      :disabled="isUploading"
                    ></textarea>
                  </div>
                </div>
              </div>
            </div>
            <div class="upload-right-panel empty-right-panel" v-else>
              <div class="empty-upload-state">
                🎵
                <h3>准备上传音乐</h3>
                <p>从左侧选择或拖拽音频文件到上传区域</p>
              </div>
            </div>
          </div>

          <!-- Footer -->
          <div class="modal-footer">
            <button class="btn-cancel" @click="closeUploadModal" :disabled="isUploading">
              ❌ 取消
            </button>
            <button
              class="btn-upload"
              @click="startUpload"
              :disabled="isUploading || !uploadQueue.length || !isUploadFormValid"
            >
              ⬆️ {{ isUploading ? '上传中...' : '开始上传' }}
            </button>
          </div>
        </div>
      </div>

      <!-- Delete confirmation modal -->
      <div v-if="showDeleteModal" class="modal-overlay">
        <div class="modal-content delete-modal">
          <div class="delete-modal-body">
            <div class="delete-preview">
              <img :src="getAudioCover(selectedDeleteAudio)" :alt="selectedDeleteAudio?.title">
            </div>
            <div class="delete-confirm-content">
              <div class="delete-icon">
                <span>(｡•́︿•̀｡)</span>
              </div>
              <div class="delete-message">
                <h3>确定要删除这首音乐吗？</h3>
                <p>删除后将无法恢复</p>
              </div>
              <div class="delete-actions">
                <button class="btn-cancel" @click="showDeleteModal = false">取消</button>
                <button class="btn-delete" @click="confirmDelete">删除</button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 添加音频播放器 -->
      <div class="player"
           v-if="currentPlaying"
           :class="{
             'mobile-player': isMobileView,
             'collapsed': isPlayerCollapsed,
             'pc-player': !isMobileView,
             'pc-collapsed': !isMobileView && isPlayerCollapsed
           }"
           @click="!isMobileView && isPlayerCollapsed && togglePlayerCollapse()">
        <div class="player-header"
             v-if="isMobileView || (!isMobileView && !isPlayerCollapsed)"
             @click.stop="togglePlayerCollapse">
          <div class="drag-handle">
            <div class="handle-line"></div>
          </div>
        </div>
        <div class="player-content">
          <!-- Left side: Album cover and controls -->
          <div class="player-left">
            <div class="music-imgs" :class="{ 'rotating': isPlaying }">
              <img :src="getAudioCover(currentPlaying)" :alt="currentPlaying.title">
              <div class="buffer-box" v-if="isBuffering">缓冲中...</div>
            </div>
            <div class="player-controls">
              <button class="btn prev" @click.stop="playPrevious" :disabled="audioFiles.length <= 1">
                ⏮️
              </button>
              <button class="btn play-pause" @click.stop="togglePlay">
                <span v-if="isPlaying">⏸️</span>
                <span v-else>▶️</span>
              </button>
              <button class="btn next" @click.stop="playNext" :disabled="audioFiles.length <= 1">
                ⏭️
              </button>
            </div>
          </div>

          <div class="progress-container">
            <div class="time">
              <span class="current-time">{{ formatTime(currentTime) }}</span>
              <span class="total-time">{{ formatTime(duration) }}</span>
            </div>
            <div class="seek-area"
                 @click="seek"
                 @mousemove="updateSeekHover"
                 @mouseleave="hideSeekHover">
              <div class="seek-hover" :style="{ left: hoverPosition + 'px' }" v-if="showSeekHover">
                {{ formatTime(hoverTime) }}
              </div>
              <div class="seek-bar" :style="{ width: progress + '%' }"></div>
            </div>
          </div>

          <!-- Right side: Song info and progress -->
          <div class="player-right">
            <div class="music-info">
              <div class="music-name">{{ currentPlaying.title || currentPlaying.fileName }}</div>
              <div class="artist-name">{{ currentPlaying.artist || '未知艺术家' }}</div>
            </div>

            <div class="music-details">
              <div class="detail-item">
                <div class="detail-label">流派</div>
                <div class="detail-value">{{ currentPlaying.genre || '未知流派' }}</div>
              </div>
              <div class="detail-item">
                <div class="detail-label">专辑</div>
                <div class="detail-value">{{ currentPlaying.album || '未知专辑' }}</div>
              </div>
            </div>

            <div class="music-description">{{ currentPlaying.description || '暂无描述' }}</div>


          </div>
        </div>

        <audio
          ref="audioPlayer"
          :src="currentPlaying.fileUrl"
          @timeupdate="onTimeUpdate"
          @loadedmetadata="onAudioLoaded"
          @ended="onAudioEnded"
          @waiting="isBuffering = true"
          @canplay="isBuffering = false"
        ></audio>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch, nextTick, onUnmounted } from 'vue'
import { useRoute, useRouter, type RouteLocationNormalizedLoaded } from 'vue-router'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { getDefaultAvatar } from '@/utils/userUtils'

import {
  fetchMusicAlbumByIdUsingGet,
  fetchAlbumAudiosUsingGet,
  uploadMusicToAlbumUsingPost,
  deleteAudioFromAlbumUsingPost
} from '@/api/loveBoardMusicAlbumController'

import { commonUploadPictureUsingPost } from '@/api/fileUploadController'
import { listCategoryByTypeUsingGet } from '@/api/categoryController'

// Types
interface AudioFile {
  id: string
  title: string
  artist: string
  album?: string
  genre?: string
  description?: string
  fileName: string
  fileUrl: string
  coverUrl?: string
  duration: number
  createTime: string
}

interface Album {
  id: string
  albumName: string
  description?: string
  coverUrl?: string
  createTime: string
  isPublic: string
  musicCount: string
}

interface UploadQueueItem {
  file: File
  title: string
  artist: string
  album?: string
  genre?: string
  description?: string
  coverFile?: File
  coverPreview?: string
  name: string
}

// 图片压缩相关常量
const MAX_IMAGE_SIZE = 1 * 1024 * 1024 // 1MB
const MAX_IMAGE_WIDTH = 1920
const MAX_IMAGE_HEIGHT = 1080
const IMAGE_QUALITY = 0.85

// 音频压缩相关常量
const MAX_AUDIO_SIZE = 10 * 1024 * 1024 // 10MB
const AUDIO_QUALITY = 0.8

// State
const route: RouteLocationNormalizedLoaded = useRoute()
const router = useRouter()
const loading = ref<boolean>(true)
const error = ref<string | null>(null)
const album = ref<Album | null>(null)
const audioFiles = ref<AudioFile[]>([])
const currentPlaying = ref<AudioFile | null>(null)
const isPlaying = ref<boolean>(false)
const currentTime = ref<number>(0)
const duration = ref<number>(0)
const audioPlayer = ref<HTMLAudioElement | null>(null)
const progress = computed(() => {
  if (!duration.value) return 0
  return (currentTime.value / duration.value) * 100
})

// Upload state
const uploadQueue = ref<UploadQueueItem[]>([])
const isUploading = ref<boolean>(false)
const fileInput = ref<HTMLInputElement | null>(null)
const currentEditIndex = ref<number>(0)
const coverInputs = ref<{[key: number]: HTMLInputElement}>({})

// Add new state for seek hover
const showSeekHover = ref<boolean>(false)
const hoverPosition = ref<number>(0)
const hoverTime = ref<number>(0)
const isBuffering = ref<boolean>(false)
const showUploadModal = ref<boolean>(false)
const showDeleteModal = ref<boolean>(false)
const selectedDeleteAudio = ref<AudioFile | null>(null)

// Add new state for mobile view
const isMobileView = ref(window.innerWidth <= 768)
const isPlayerCollapsed = ref(true)

// Add new state for header expansion
const isHeaderExpanded = ref(false)

// 添加分类相关的状态
const categoryList = ref<string[]>([])
const isCustomGenre = ref<boolean>(false)

// Computed
const isOwner = computed(() => {
  return route.query.isOwner === 'true'
})

// Format utilities
const formatDate = (date?: string) => {
  if (!date) return ''
  return dayjs(date).format('YYYY-MM-DD')
}

const formatDuration = (seconds?: number) => {
  if (!seconds) return '00:00'
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${mins}:${secs.toString().padStart(2, '0')}`
}

// Format time for audio player
const formatTime = (seconds: number) => {
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${mins}:${secs.toString().padStart(2, '0')}`
}

// Basic loading retry function
const retryLoading = async () => {
  error.value = null
  loading.value = true
  try {
    await Promise.all([fetchAlbumData(), fetchAudioFiles()])
  } catch (err) {
    error.value = '加载失败，请重试'
  } finally {
    loading.value = false
  }
}

// Fetch album info
const fetchAlbumData = async () => {
  try {
    const albumId = route.params.id as string
    const password = sessionStorage.getItem(`album_${albumId}_password`)
    const res = await fetchMusicAlbumByIdUsingGet({
      id: albumId,
      password
    })
    if (res.data.code === 0) {
      album.value = res.data.data
    } else {
      throw new Error(res.data.message || '获取专辑信息失败')
    }
  } catch (err) {
    error.value = '加载专辑信息失败'
    console.error(err)
  }
}

// Fetch audio list
const fetchAudioFiles = async () => {
  try {
    const albumId = route.params.id as string
    const password = sessionStorage.getItem(`album_${albumId}_password`)
    const res = await fetchAlbumAudiosUsingGet({
      albumId,
      password
    })
    if (res.data.code === 0) {
      audioFiles.value = res.data.data
    } else {
      throw new Error(res.data.message || '获取音频列表失败')
    }
  } catch (err) {
    error.value = '加载音乐列表失败'
    console.error(err)
  }
}

// Initialize
onMounted(async () => {
  try {
    await Promise.all([fetchAlbumData(), fetchAudioFiles()])
  } catch (err: any) {
    error.value = err.message || '加载失败，请重试'
  } finally {
    loading.value = false
  }
})

// Watch route changes
watch(() => route.params.id, async (newId: string) => {
  if (newId) {
    loading.value = true
    error.value = null
    try {
      await Promise.all([fetchAlbumData(), fetchAudioFiles()])
    } catch (err: any) {
      error.value = err.message || '加载失败，请重试'
    } finally {
      loading.value = false
    }
  }
})

// Audio control functions
const playAudio = (audio: AudioFile) => {
  if (currentPlaying.value?.id === audio.id) {
    togglePlay()
  } else {
    currentPlaying.value = audio
    isPlaying.value = true
    nextTick(() => {
      if (audioPlayer.value) {
        audioPlayer.value.play().catch(err => {
          console.error('播放失败:', err)
          isPlaying.value = false
        })
      }
    })
  }
}

const togglePlay = () => {
  if (!audioPlayer.value) return
  if (isPlaying.value) {
    audioPlayer.value.pause()
  } else {
    audioPlayer.value.play().catch(err => {
      console.error('播放失败:', err)
    })
  }
  isPlaying.value = !isPlaying.value
}

// Delete functions
const showDeleteConfirm = (audio: AudioFile) => {
  selectedDeleteAudio.value = audio
  showDeleteModal.value = true
}

const confirmDelete = async () => {
  if (!selectedDeleteAudio.value) return

  try {
    const res = await deleteAudioFromAlbumUsingPost({
      albumId: route.params.id as string,
      audioId: selectedDeleteAudio.value.id
    })

    if (res.data.code === 0) {
      audioFiles.value = audioFiles.value.filter(
        (audio: AudioFile) => audio.id !== selectedDeleteAudio.value?.id
      )
      if (currentPlaying.value?.id === selectedDeleteAudio.value.id) {
        currentPlaying.value = null
        isPlaying.value = false
      }
      message.success('删除成功')
    } else {
      throw new Error(res.data.message || '删除失败')
    }
  } catch (err) {
    message.error('删除失败')
    console.error(err)
  } finally {
    showDeleteModal.value = false
    selectedDeleteAudio.value = null
  }
}

// Audio player controls
const onTimeUpdate = () => {
  if (!audioPlayer.value) return
  currentTime.value = audioPlayer.value.currentTime
}

const onAudioLoaded = () => {
  if (!audioPlayer.value) return
  duration.value = audioPlayer.value.duration
}

const onAudioEnded = () => {
  playNext()
}

const seek = (event: MouseEvent) => {
  if (!audioPlayer.value) return
  const progressBar = event.currentTarget as HTMLElement
  const rect = progressBar.getBoundingClientRect()
  const percent = (event.clientX - rect.left) / rect.width
  const time = percent * duration.value
  audioPlayer.value.currentTime = time
}

const playNext = () => {
  if (!currentPlaying.value || audioFiles.value.length <= 1) return
  const currentIndex = audioFiles.value.findIndex((audio: AudioFile) => audio.id === currentPlaying.value?.id)
  const nextIndex = currentIndex < audioFiles.value.length - 1 ? currentIndex + 1 : 0
  playAudio(audioFiles.value[nextIndex])
}

const playPrevious = () => {
  if (!currentPlaying.value || audioFiles.value.length <= 1) return
  const currentIndex = audioFiles.value.findIndex((audio: AudioFile) => audio.id === currentPlaying.value?.id)
  const prevIndex = currentIndex > 0 ? currentIndex - 1 : audioFiles.value.length - 1
  playAudio(audioFiles.value[prevIndex])
}

// Upload functions
const triggerFileInput = () => {
  fileInput.value?.click()
}

const handleFileSelect = (event: Event) => {
  const target = event.target as HTMLInputElement
  if (target.files) {
    handleFiles(Array.from(target.files))
  }
}

const handleFileDrop = (event: DragEvent) => {
  const files = event.dataTransfer?.files
  if (files) {
    handleFiles(Array.from(files))
  }
}

// 音频压缩函数
const compressAudio = (file: File): Promise<File> => {
  return new Promise((resolve, reject) => {
    // 如果文件小于限制大小，直接返回
    if (file.size <= MAX_AUDIO_SIZE) {
      resolve(file);
      return;
    }

    const reader = new FileReader();
    reader.onload = async (e) => {
      try {
        const audioContext = new (window.AudioContext || window.webkitAudioContext)();
        const audioBuffer = await audioContext.decodeAudioData(e.target?.result as ArrayBuffer);

        // 创建离线音频上下文
        const offlineCtx = new OfflineAudioContext(
          audioBuffer.numberOfChannels,
          audioBuffer.length,
          audioBuffer.sampleRate
        );

        // 创建音频源
        const source = offlineCtx.createBufferSource();
        source.buffer = audioBuffer;

        // 创建压缩器
        const compressor = offlineCtx.createDynamicsCompressor();
        compressor.threshold.value = -50;
        compressor.knee.value = 40;
        compressor.ratio.value = 12;
        compressor.attack.value = 0;
        compressor.release.value = 0.25;

        // 连接节点
        source.connect(compressor);
        compressor.connect(offlineCtx.destination);

        // 开始渲染
        source.start(0);
        const renderedBuffer = await offlineCtx.startRendering();

        // 将 AudioBuffer 转换为 WAV 格式
        const wavBlob = await audioBufferToWav(renderedBuffer);

        // 创建新的文件对象
        const compressedFile = new File([wavBlob], file.name.replace(/\.[^/.]+$/, '.wav'), {
          type: 'audio/wav',
          lastModified: Date.now()
        });

        resolve(compressedFile);
      } catch (error) {
        reject(error);
      }
    };

    reader.onerror = () => reject(new Error('文件读取失败'));
    reader.readAsArrayBuffer(file);
  });
};

// 将 AudioBuffer 转换为 WAV 格式
const audioBufferToWav = (buffer: AudioBuffer): Promise<Blob> => {
  const numOfChan = buffer.numberOfChannels;
  const length = buffer.length * numOfChan * 2;
  const buffer32 = new Float32Array(buffer.length * numOfChan);
  const view = new DataView(new ArrayBuffer(44 + length));
  const channels = [];
  let offset = 0;
  let pos = 0;

  // 提取音频通道数据
  for (let i = 0; i < buffer.numberOfChannels; i++) {
    channels.push(buffer.getChannelData(i));
  }

  // 交错通道数据
  while (pos < buffer.length) {
    for (let i = 0; i < numOfChan; i++) {
      buffer32[offset] = channels[i][pos];
      offset++;
    }
    pos++;
  }

  // 写入WAV文件头
  const writeString = (view: DataView, offset: number, string: string) => {
    for (let i = 0; i < string.length; i++) {
      view.setUint8(offset + i, string.charCodeAt(i));
    }
  };

  writeString(view, 0, 'RIFF');
  view.setUint32(4, 36 + length, true);
  writeString(view, 8, 'WAVE');
  writeString(view, 12, 'fmt ');
  view.setUint32(16, 16, true);
  view.setUint16(20, 1, true);
  view.setUint16(22, numOfChan, true);
  view.setUint32(24, buffer.sampleRate, true);
  view.setUint32(28, buffer.sampleRate * 2 * numOfChan, true);
  view.setUint16(32, numOfChan * 2, true);
  view.setUint16(34, 16, true);
  writeString(view, 36, 'data');
  view.setUint32(40, length, true);

  // 写入音频数据
  const length32 = buffer32.length;
  let index = 44;
  const volume = 1;
  for (let i = 0; i < length32; i++) {
    view.setInt16(index, buffer32[i] * (0x7FFF * volume), true);
    index += 2;
  }

  return Promise.resolve(new Blob([view], { type: 'audio/wav' }));
};

// 音频元数据解析函数
const parseAudioMetadata = (file: File): Promise<{
  title?: string
  artist?: string
  album?: string
  genre?: string
}> => {
  return new Promise((resolve) => {
    const reader = new FileReader();
    reader.onload = async (e) => {
      try {
        const audioContext = new AudioContext();
        const audioBuffer = await audioContext.decodeAudioData(e.target?.result as ArrayBuffer);

        // 从文件名中提取信息（去掉扩展名和特殊字符）
        const fileName = file.name.replace(/\.[^/.]+$/, '').replace(/[_-]/g, ' ');

        // 尝试从文件名中分离艺术家和标题（如果文件名符合 "艺术家 - 标题" 格式）
        const [artist, title] = fileName.split(' - ').map(s => s.trim());

        resolve({
          title: title || fileName,  // 如果无法分离，则使用完整文件名作为标题
          artist: title ? artist : '',  // 如果无法分离，则艺术家留空
          // 音频时长（秒）
          duration: audioBuffer.duration
        });
      } catch (error) {
        // 如果解析失败，至少返回文件名作为标题
        resolve({
          title: file.name.replace(/\.[^/.]+$/, ''),
        });
      }
    };
    reader.onerror = () => {
      resolve({
        title: file.name.replace(/\.[^/.]+$/, ''),
      });
    };
    reader.readAsArrayBuffer(file);
  });
};

// 修改文件处理函数
const handleFiles = async (files: File[]) => {
  const validFiles = files.filter(file => {
    const ext = file.name.split('.').pop()?.toLowerCase()
    return ['mp3', 'wav', 'ogg', 'm4a'].includes(ext || '')
  })

  if (validFiles.length === 0) {
    message.error('请选择有效的音频文件(MP3, WAV, OGG, M4A)')
    return
  }

  try {
    // 处理每个文件
    const processedFiles = await Promise.all(
      validFiles.map(async (file) => {
        // 压缩音频文件
        const compressedFile = await compressAudio(file);

        // 解析音频元数据
        const metadata = await parseAudioMetadata(file);

        return {
          file: compressedFile,
          name: file.name,
          title: metadata.title || '',
          artist: metadata.artist || '',
          album: metadata.album || '',
          genre: metadata.genre || '',
          description: '',
          coverPreview: '',
          coverUrl: ''
        };
      })
    );

    uploadQueue.value = processedFiles;
  } catch (error) {
    console.error('文件处理失败:', error);
    message.error('文件处理失败，请重试');
  }

  // 重置 coverInputs 对象
  coverInputs.value = {};
};

const isUploadFormValid = computed(() => {
  return uploadQueue.value.every((item: UploadQueueItem) => item.title && item.artist)
})

// 修改封面点击处理函数
const handleCoverClick = (index: number) => {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = 'image/*'
  input.style.display = 'none'
  input.onchange = (event) => handleCoverSelect(event as Event, index)
  document.body.appendChild(input)
  coverInputs.value[index] = input
  input.click()
}

// 修改封面选择处理函数
const handleCoverSelect = async (event: Event, index: number) => {
  const target = event.target as HTMLInputElement
  if (!target.files || !target.files[0]) return

  const file = target.files[0]
  if (!file.type.startsWith('image/')) {
    message.error('请选择图片文件')
    return
  }

  try {
    // 压缩图片
    const compressedFile = await compressImage(file);
    const formData = new FormData()
    formData.append('file', compressedFile)

    const res = await commonUploadPictureUsingPost({}, {}, compressedFile)

    if (res.data.code === 0) {
      // 更新预览和保存URL
      if (uploadQueue.value[index]) {
        const objectUrl = URL.createObjectURL(compressedFile)
        uploadQueue.value[index] = {
          ...uploadQueue.value[index],
          coverPreview: objectUrl,
          coverUrl: res.data.data.url
        }
      }
      message.success('封面上传成功')
    } else {
      throw new Error(res.data.message || '封面上传失败')
    }
  } catch (err: any) {
    console.error('封面上传失败:', err)
    message.error(err.message || '封面上传失败')
  } finally {
    // 清理input元素
    if (coverInputs.value[index]) {
      document.body.removeChild(coverInputs.value[index])
      delete coverInputs.value[index]
    }
  }
}

const startUpload = async () => {
  if (!isUploadFormValid.value) {
    message.error('请填写必填项')
    return
  }

  isUploading.value = true
  const albumId = route.params.id as string

  for (let i = 0; i < uploadQueue.value.length; i++) {
    const item = uploadQueue.value[i]
    currentEditIndex.value = i

    try {
      const res = await uploadMusicToAlbumUsingPost(
        {
          albumId,
          title: item.title,
          artist: item.artist,
          album: item.album,
          genre: item.genre,
          description: item.description,
          coverUrl: item.coverUrl // 直接使用已上传的封面URL
        },
        {},
        item.file
      )

      if (res.data.code === 0) {
        uploadQueue.value[i].progress = 100
        audioFiles.value.push(res.data.data)
        message.success(`音乐 "${item.title}" 上传成功`)
      } else {
        throw new Error(res.data.message || '上传失败')
      }
    } catch (err: any) {
      console.error('上传失败:', err)
      uploadQueue.value[i].error = err.message || '上传失败'
      message.error(`音乐 "${item.title}" 上传失败`)
    }
  }

  isUploading.value = false
  await fetchAudioFiles() // Refresh the list
  closeUploadModal()
}

const closeUploadModal = () => {
  if (isUploading.value) {
    message.warning('正在上传中，请等待上传完成')
    return
  }
  showUploadModal.value = false
  uploadQueue.value = []
  currentEditIndex.value = 0
}

const getAudioCover = (audio: AudioFile) => {
  return audio.coverUrl || getDefaultAvatar(audio.title || audio.fileName)
}

// Add new method for seek hover
const updateSeekHover = (event: MouseEvent) => {
  const seekArea = event.currentTarget as HTMLElement
  const rect = seekArea.getBoundingClientRect()
  const percent = (event.clientX - rect.left) / rect.width
  hoverPosition.value = event.clientX - rect.left
  hoverTime.value = percent * duration.value
  showSeekHover.value = true
}

const hideSeekHover = () => {
  showSeekHover.value = false
}

// 监听窗口大小变化
onMounted(() => {
  window.addEventListener('resize', () => {
    isMobileView.value = window.innerWidth <= 768
  })
})

onUnmounted(() => {
  window.removeEventListener('resize', () => {
    isMobileView.value = window.innerWidth <= 768
  })
})

// 切换播放器展开/折叠状态
const togglePlayerCollapse = () => {
  isPlayerCollapsed.value = !isPlayerCollapsed.value
}

// Toggle header expansion
const toggleHeader = () => {
  if (window.innerWidth <= 768) {
    isHeaderExpanded.value = !isHeaderExpanded.value
  }
}

// 图片压缩函数
const compressImage = (file: File): Promise<File> => {
  return new Promise((resolve, reject) => {
    // 如果文件小于1MB，直接返回
    if (file.size <= MAX_IMAGE_SIZE) {
      resolve(file);
      return;
    }

    const reader = new FileReader();
    reader.readAsDataURL(file);
    reader.onload = (e) => {
      const img = new Image();
      img.src = e.target?.result as string;

      img.onload = () => {
        const canvas = document.createElement('canvas');
        let width = img.width;
        let height = img.height;

        // 优化缩放比例计算
        const aspectRatio = width / height;
        if (width > MAX_IMAGE_WIDTH || height > MAX_IMAGE_HEIGHT) {
          if (aspectRatio > 1) {
            width = MAX_IMAGE_WIDTH;
            height = Math.round(width / aspectRatio);
          } else {
            height = MAX_IMAGE_HEIGHT;
            width = Math.round(height * aspectRatio);
          }
        }

        canvas.width = width;
        canvas.height = height;

        const ctx = canvas.getContext('2d', { alpha: false }); // 禁用alpha通道以提高性能
        if (!ctx) {
          reject(new Error('无法创建canvas上下文'));
          return;
        }

        // 使用更快的图像渲染设置
        ctx.imageSmoothingEnabled = true;
        ctx.imageSmoothingQuality = 'medium';
        ctx.drawImage(img, 0, 0, width, height);

        // 检查浏览器是否支持 WebP
        const isWebPSupported = () => {
          const canvas = document.createElement('canvas');
          if (canvas.toDataURL) {
            return canvas.toDataURL('image/webp').indexOf('data:image/webp') === 0;
          }
          return false;
        };

        const format = isWebPSupported() ? 'image/webp' : 'image/jpeg';
        let quality = IMAGE_QUALITY;

        // 递归压缩函数，直到文件大小合适或达到最低质量
        const compressWithQuality = (currentQuality: number): Promise<Blob> => {
          return new Promise((resolveBlob, rejectBlob) => {
            canvas.toBlob(
              (blob) => {
                if (!blob) {
                  rejectBlob(new Error('图片压缩失败'));
                  return;
                }

                // 如果文件大小合适或已经达到最低质量，返回结果
                if (blob.size <= MAX_IMAGE_SIZE || currentQuality <= 0.5) {
                  resolveBlob(blob);
                  return;
                }

                // 否则继续降低质量压缩
                compressWithQuality(currentQuality - 0.1)
                  .then(resolveBlob)
                  .catch(rejectBlob);
              },
              format,
              currentQuality
            );
          });
        };

        // 开始压缩过程
        compressWithQuality(quality)
          .then((finalBlob) => {
            const newFileName = file.name.replace(/\.[^/.]+$/, '') +
              (format === 'image/webp' ? '.webp' : '.jpg');
            const compressedFile = new File([finalBlob], newFileName, {
              type: format,
              lastModified: Date.now(),
            });
            resolve(compressedFile);
          })
          .catch((error) => {
            reject(error);
          });
      };

      img.onerror = () => reject(new Error('图片加载失败'));
    };

    reader.onerror = () => reject(new Error('文件读取失败'));
  });
};

// 获取音频分类列表
const fetchCategories = async () => {
  try {
    const res = await listCategoryByTypeUsingGet({
      type: 2 // 3代表音频分类
    })
    if (res.data.code === 0) {
      categoryList.value = res.data.data || []
    }
  } catch (err) {
    console.error('获取分类列表失败:', err)
  }
}

// 修改上传模态框打开时的处理
const openUploadModal = async () => {
  showUploadModal.value = true
  await fetchCategories()
}

</script>

<style scoped>
.music-album-detail {
  min-height: 99vh;
  width: 100vw;
  margin: -84px -28px;
  background: linear-gradient(135deg, #ff79c6 0%, #bd93f9 100%);
  padding: 84px 28px;
  position: relative;
  overflow-x: hidden;
}

.loading-state,
.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  text-align: center;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #f3f3f3;
  border-top: 3px solid var(--themeBackground);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.error-state {
  color: #ff4d4f;
}

.error-state button {
  margin-top: 16px;
  padding: 8px 24px;
  border: none;
  border-radius: 4px;
  background: var(--themeBackground);
  color: white;
  cursor: pointer;
  transition: opacity 0.2s;
}

.error-state button:hover {
  opacity: 0.9;
}

.album-header {
  position: relative;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(30px);
  border: none;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
  position: relative;
  overflow: hidden;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  min-height: auto;
  border-radius: 12px;
}

.album-background {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 100%;
  overflow: hidden;
}

.album-background img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  filter: blur(30px);
  transform: scale(1.2);
  opacity: 0.6;
}

.album-background::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(
    180deg,
    rgba(0, 0, 0, 0.3) 0%,
    rgba(0, 0, 0, 0.7) 100%
  );
}

.album-content {
  position: relative;

  padding: 32px;
}

.album-info {
  display: flex;
  gap: 32px;
  align-items: center;
}

.album-cover {
  width: 240px;
  height: 240px;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 12px 36px rgba(0, 0, 0, 0.3);
  position: relative;
  transition: transform 0.3s ease;
  flex-shrink: 0;
}

.album-cover:hover {
  transform: translateY(-8px);
}

.album-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.album-cover:hover img {
  transform: scale(1.05);
}

.album-meta {
  flex: 1;
  color: white;
}

.album-meta h1 {
  font-size: 42px;
  font-weight: 700;
  margin: 0 0 16px;
  background: linear-gradient(135deg, #fff 0%, rgba(255, 255, 255, 0.8) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.description {
  font-size: 18px;
  line-height: 1.6;
  color: rgba(255, 255, 255, 0.9);
  margin-bottom: 24px;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
  white-space: pre-wrap;
  word-break: break-all;
  word-wrap: break-word;
  overflow-wrap: break-word;
  max-height: 200px;
  overflow-y: auto;
  padding-right: 10px;
  width: 100%;
  box-sizing: border-box;
}

/* 添加滚动条样式 */
.description::-webkit-scrollbar {
  width: 4px;
}

.description::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 2px;
}

.description::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.3);
  border-radius: 2px;
}

.description::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.5);
}

/* 移动端样式优化 */
@media (max-width: 768px) {
  .music-album-detail {
    min-height: 100vh!important;
  }
  .description {
    font-size: 16px;
    margin-bottom: 20px;
    color: rgba(255, 255, 255, 0.9);
    text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
    padding: 0 20px;
    max-height: 150px;
    width: 100%;
    box-sizing: border-box;
  }

  .album-meta {
    width: 100%;
    box-sizing: border-box;
  }
}

/* 优化空专辑状态的样式 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 24px;
  text-align: center;
  margin: 20px;
  min-height: 300px;
}

.empty-state .empty-icon {
  font-size: 64px;
  margin-bottom: 24px;
  background: linear-gradient(135deg, #ff79c6 0%, #bd93f9 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  animation: float 3s ease-in-out infinite;
}

.empty-state p {
  font-size: 18px;
  color: rgba(255, 255, 255, 0.9);
  margin: 0;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

@keyframes float {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-10px);
  }
}

/* 移动端空状态样式 */
@media (max-width: 768px) {
  .empty-state {
    margin: 12px;
    padding: 36px 20px;
    min-height: 240px;
  }

  .empty-state .empty-icon {
    font-size: 48px;
    margin-bottom: 16px;
  }

  .empty-state p {
    font-size: 16px;
  }
}

.stats {
  display: flex;
  gap: 24px;
  padding-top: 24px;
  border-top: 1px solid rgba(255, 255, 255, 0.2);
  flex-wrap: wrap;
}

.stats span {
  display: flex;
  align-items: center;
  gap: 8px;
  color: rgba(255, 255, 255, 0.8);
  font-size: 16px;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.album-actions {
  position: absolute;
  top: 32px;
  right: 32px;
}

.upload-btn {
  position: fixed;
  bottom: calc(48px + env(safe-area-inset-bottom, 0px));
  right: 16px;

  width: 44px;
  height: 44px;
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.9) 0%, rgba(139, 92, 246, 0.9) 100%);
  color: white;
  border: none;
  border-radius: 50%;
  font-size: 24px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
}

.upload-btn:hover {
  transform: translateY(-4px) scale(1.05);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.95) 0%, rgba(124, 58, 237, 0.95) 100%);
}

.upload-btn:active {
  transform: translateY(-2px) scale(0.98);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.upload-btn i {
  font-size: 20px;
  transition: transform 0.3s ease;
}

.upload-btn:hover i {
  transform: rotate(90deg);
}

.music-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
  padding-bottom:  64px;
  padding-top: 24px;

}

.music-card {
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 20px;
  padding: 24px;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.music-card:hover {
  transform: translateY(-8px);
  background: rgba(255, 255, 255, 0.15);
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.15);
}

.music-card.playing {
  background: linear-gradient(135deg, rgba(255, 121, 198, 0.2) 0%, rgba(189, 147, 249, 0.2) 100%);
  border-color: rgba(255, 255, 255, 0.3);
}

.music-card.playing .music-card-cover {
  animation: rotate 20s linear infinite;
}

.music-card.playing.paused .music-card-cover,
.music-card.playing .music-card-cover.paused {
  animation-play-state: paused;
}

.music-card-cover {
  width: 200px;
  height: 200px;
  border-radius: 50%;
  overflow: hidden;
  margin: 0 auto 24px;
  position: relative;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
  transition: transform 0.5s ease;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.music-card-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s ease;
}

.music-card:hover .music-card-cover img {
  transform: scale(1.1);
}

.play-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: all 0.3s ease;
}

.music-card:hover .play-overlay {
  opacity: 1;
}

.play-button {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  transform: scale(0.8);
  transition: all 0.3s ease;
  font-size: 28px;
  border: 2px solid rgba(255, 255, 255, 0.3);
}

.music-card:hover .play-button {
  transform: scale(1);
}

.music-card-content {
  text-align: center;
  width: 100%;
}

.music-title {
  font-size: 20px;
  font-weight: 600;
  color: white;
  margin: 0 0 8px;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.music-artist {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.8);
  margin: 0 0 16px;
}

.music-duration {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: rgba(255, 255, 255, 0.6);
  font-size: 14px;
}

.music-duration i {
  color: #ff79c6;
}

.music-card-actions {
  position: absolute;
  top: 20px;
  right: 20px;
  opacity: 0;
  transform: translateX(20px);
  transition: all 0.3s ease;
}

.music-card:hover .music-card-actions {
  opacity: 1;
  transform: translateX(0);
}

.action-btn {
  width: 40px;
  height: 40px;
  border: none;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.9);
  color: #ff79c6;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.action-btn:hover {
  transform: scale(1.1);
  background: white;
}

.action-btn.delete:hover {
  background: #ff5555;
  color: white;
}

/* 播放器样式 */
.player {
  position: fixed;
  bottom: 32px;
  right: 32px;
  width: 400px;
  max-height: 80vh;
  height: 80vh;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 20px;
  padding: 0;
  color: white;
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.2);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  cursor: pointer;
}

.player:hover {
  transform: translateY(-8px);
  box-shadow: 0 16px 56px rgba(0, 0, 0, 0.3);
}

.player-content {
  padding: 16px;
  flex-direction: column;
  overflow-y: auto;
  overflow-x: hidden;
  -webkit-overflow-scrolling: touch;
  flex: 1;
  display: flex;
  gap: 24px;
}

.player-left {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.music-imgs {
  width: 240px;
  height: 240px;
  border-radius: 50%;
  overflow: hidden;
  margin-bottom: 24px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
  position: relative;
}


.music-imgs img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%!important;
}

.player-controls {
  width: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 24px;
  margin-top: 24px;
}

.player-right {
  width: 100%;
  padding: 0;
  text-align: center;
}

.music-info {
  margin-bottom: 24px;
  text-align: center;
}

.music-name {
  font-size: 24px;
  font-weight: 600;
  margin-bottom: 8px;
  word-break: break-word;
  overflow-wrap: break-word;
}

.artist-name {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.8);
  margin-bottom: 16px;
  word-break: break-word;
  overflow-wrap: break-word;
}

.music-details {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin-bottom: 24px;
  padding: 0 32px;
}

.detail-value {
  color: rgba(255, 255, 255, 0.9);
  word-break: break-word;
  overflow-wrap: break-word;
}

.music-description {
  margin: 0 32px 24px;
  font-size: 14px;
  line-height: 1.6;
  color: rgba(255, 255, 255, 0.8);
  word-break: break-word;
  overflow-wrap: break-word;
  margin-bottom: 64px;
}

.progress-container {
  width: 100%;
  padding: 0 32px;
  margin-bottom: 24px;
}

.time {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.8);
  margin-bottom: 8px;
}

.seek-area {
  height: 4px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 2px;
  cursor: pointer;
  position: relative;
}

.seek-hover {
  position: absolute;
  top: -24px;
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.8);
  color: white;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  pointer-events: none;
}

.seek-bar {
  height: 100%;
  background: linear-gradient(90deg, #ff79c6 0%, #bd93f9 100%);
  border-radius: 2px;
  position: relative;
}

.seek-bar::after {
  content: '';
  position: absolute;
  right: -4px;
  top: 50%;
  transform: translateY(-50%);
  width: 8px;
  height: 8px;
  background: white;
  border-radius: 50%;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.seek-area:hover .seek-bar::after {
  opacity: 1;
}

/* Upload modal styles */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.75);
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(8px);
  z-index: 1000000;
}

.modal-content {
  background: #fff;
  border-radius: 16px;
  width: 95%;
  max-width: 1200px;
  height: 85vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 12px 36px rgba(0, 0, 0, 0.25);
  margin: 20px;
  position: relative;
}

.modal-header {
  padding: 20px 24px;
  background: linear-gradient(135deg, #2C88D9 0%, #1890FF 100%);
  color: white;
  border-radius: 16px 16px 0 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.modal-header h2 {
  font-size: 20px;
  font-weight: 600;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.modal-close {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: none;
  background: rgba(255, 255, 255, 0.2);
  color: white;
  font-size: 20px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.modal-close:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: rotate(90deg);
}

.upload-container {
  display: flex;
  flex: 1;
  min-height: 0;
  background: #F0F2F5;
}

.upload-left-panel {
  width: 300px;
  border-right: 1px solid #E8E8E8;
  display: flex;
  flex-direction: column;
  background: white;
}

.upload-zone {
  padding: 24px;
  border-bottom: 1px solid #E8E8E8;
}

.upload-icon-wrapper {
  position: relative;
  width: 80px;
  height: 80px;
  margin: 0 auto 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.upload-icon-wrapper i {
  font-size: 40px;
  color: #1890FF;

}

.upload-pulse {
  position: absolute;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: rgba(24, 144, 255, 0.1);
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% {
    transform: scale(0.95);
    opacity: 0.5;
  }
  50% {
    transform: scale(1.05);
    opacity: 0.8;
  }
  100% {
    transform: scale(0.95);
    opacity: 0.5;
  }
}

.upload-formats {
  display: flex;
  gap: 8px;
  justify-content: center;
  margin: 16px 0;
}

.format-tag {
  padding: 4px 12px;
  background: rgba(24, 144, 255, 0.1);
  color: #1890FF;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.upload-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #666;
  font-size: 13px;
}

.upload-hint i {
  color: #1890FF;
}

.selected-files {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.files-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid #E8E8E8;
}

.files-header h4 {
  margin: 0;
  font-size: 14px;
  color: #333;
  display: flex;
  align-items: center;
  gap: 8px;
}

.file-count {
  font-size: 12px;
  color: #666;
  background: rgba(24, 144, 255, 0.1);
  padding: 2px 8px;
  border-radius: 10px;
}

.file-item {
  display: flex;
  align-items: center;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-bottom: 8px;
  background: #F8F9FA;
}

.file-item.active {
  background: #E6F7FF;
  border-left: 3px solid #1890FF;
}

.file-item:hover {
  background: #E6F7FF;
}

.file-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
  border-radius: 4px;
  background: rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

.file-cover {
  width: 40px;
  height: 40px;
  border-radius: 4px;
  object-fit: cover;
}

.file-info {
  flex: 1;
  min-width: 0;
}

.file-name {
  font-size: 14px;
  color: #333;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.file-meta {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.6);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.form-group label i {
  margin-right: 6px;
  color: #1890FF;
}

.upload-right-panel {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  background: #F8F9FA;
}

.metadata-form {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.form-header {
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #F0F0F0;
}

.form-header h3 {
  font-size: 18px;
  color: #333;
  margin: 0 0 8px;
}

.current-file {
  color: #666;
  font-size: 14px;
  margin: 0;
}

.cover-upload-section {
  margin-bottom: 24px;
}

.cover-preview {
  width: 200px;
  height: 200px;
  border-radius: 12px;
  overflow: hidden;
  background: #F8F9FA;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 2px dashed #D9D9D9;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cover-preview:hover {
  border-color: #1890FF;
  background: #E6F7FF;
}

/* PC 端播放器样式 */
.pc-player {
  position: fixed;
  bottom: 64px;
  right: 88px;
  width: 450px;
  height: 80vh;
  max-height: 80vh;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 20px;
  padding: 0;
  color: white;
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.2);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  cursor: pointer;
}

.pc-player.pc-collapsed {
  height: 100px;
}

.pc-player.pc-collapsed .player-content {
  flex-direction: row;
  align-items: center;
  padding: 8px 16px;
  gap: 16px;
  height: 100px;
  overflow: hidden;
}

.pc-player.pc-collapsed .music-imgs {
  width: 64px !important;
  height: 64px !important;
  margin-bottom: 0;
  border-radius: 50% !important;
  overflow: hidden !important;
  aspect-ratio: 1/1 !important;
  flex-shrink: 0 !important;
}

.pc-player.pc-collapsed .music-imgs img {
  width: 64px !important;
  height: 64px !important;
  object-fit: cover !important;
  border-radius: 50% !important;
  aspect-ratio: 1/1 !important;
}

.pc-player.pc-collapsed .player-left {
  width: auto;
  flex-direction: row;
  align-items: center;
  gap: 16px;
}

.pc-player.pc-collapsed .player-controls {
  margin-top: 0;
}

.pc-player.pc-collapsed .player-right {
  flex: 1;
  min-width: 0;
  margin-top: 0;
}

.pc-player.pc-collapsed .music-info {
  text-align: left;
  margin-bottom: 0;
}

.pc-player.pc-collapsed .music-name {
  font-size: 16px;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.pc-player.pc-collapsed .artist-name {
  font-size: 14px;
  margin-bottom: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.pc-player.pc-collapsed .music-details,
.pc-player.pc-collapsed .music-description,
.pc-player.pc-collapsed .progress-container {
  display: none;
}

.pc-player:not(.pc-collapsed):hover {
  transform: translateY(-8px);
  box-shadow: 0 16px 56px rgba(0, 0, 0, 0.3);
}

.cover-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.cover-preview:hover img {
  transform: scale(1.05);
}

.cover-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.cover-placeholder i {
  font-size: 40px;
  color: #1890FF;
}

.cover-placeholder small {
  color: #999;
  font-size: 12px;
}

.form-fields {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-group.full-width {
  grid-column: 1 / -1;
}

.form-group label {
  font-size: 14px;
  color: #333;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 4px;
}

.required {
  color: #FF4D4F;
}

.form-group input,
.form-group textarea {
  padding: 12px;
  border: 1px solid #D9D9D9;
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.3s ease;
  background: white;
}

.form-group input:hover,
.form-group textarea:hover {
  border-color: #1890FF;
}

.form-group input:focus,
.form-group textarea:focus {
  border-color: #1890FF;
  outline: none;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.1);
}

.form-group textarea {
  resize: vertical;
  min-height: 120px;
}

.modal-footer {
  padding: 16px 24px;
  border-top: 1px solid #F0F0F0;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  background: white;
  border-radius: 0 0 16px 16px;
}

.btn-cancel,
.btn-upload {
  padding: 12px 24px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 8px;
}

.btn-cancel {
  background: #F5F5F5;
  color: #666;
  border: 1px solid #D9D9D9;
}

.btn-cancel:hover {
  background: #E8E8E8;
  color: #333;
}

.btn-upload {
  background: linear-gradient(135deg, #1890FF 0%, #40A9FF 100%);
  padding: 12px 28px;
  border-radius: 8px;
  font-weight: 600;
  letter-spacing: 0.5px;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.btn-upload:hover {
  background: linear-gradient(135deg, #40A9FF 0%, #69C0FF 100%);
  transform: translateY(-1px);
}

.btn-upload i {
  font-size: 16px;
}

.btn-upload:disabled {
  background: #D9D9D9;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

/* 移动端播放器样式 */
.mobile-player {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  width: 100%;
  max-height: 80vh;
  height: 80vh;
  border-radius: 20px 20px 0 0;
  padding: 0;
  transform: translateY(0);
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 -4px 24px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.mobile-player.collapsed {
  transform: translateY(calc(100% - 80px));
  height: 80px;
  background: linear-gradient(135deg, rgba(255, 121, 198, 0.95) 0%, rgba(189, 147, 249, 0.95) 100%);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: none;
  box-shadow: 0 -4px 24px rgba(0, 0, 0, 0.15);
}

.mobile-player.collapsed .player-content {
  flex-direction: row;
  align-items: center;
  padding: 12px 28px;
  gap: 12px;
  height: 80px;
  overflow: hidden;
}

.mobile-player.collapsed .player-left {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 12px;
  width: auto;
  flex-shrink: 0;
}

.mobile-player.collapsed .music-imgs {
  width: 56px;
  height: 56px;
  margin: 0;
  flex-shrink: 0;
  border: 2px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.mobile-player.collapsed .player-controls {
  position: absolute;
  right: 28px;
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
}

.mobile-player.collapsed .btn {
  width: 36px;
  height: 36px;
  font-size: 18px;
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.3);
}

.mobile-player.collapsed .play-pause {
  width: 42px;
  height: 42px;
  font-size: 22px;
  background: rgba(255, 255, 255, 0.25);
  border: 2px solid rgba(255, 255, 255, 0.4);
}

.mobile-player.collapsed .player-right {
  display: none;
}

.mobile-player.collapsed .progress-container {
  display: none;
}

/* 添加按钮悬停效果 */
.mobile-player.collapsed .btn:hover,
.mobile-player.collapsed .play-pause:hover {
  transform: scale(1.1);
  background: rgba(255, 255, 255, 0.3);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.mobile-player.collapsed .music-imgs {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

.mobile-player .player-content {
  padding: 16px;
  flex-direction: column;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  flex: 1;
}

.mobile-player.collapsed .player-content {
  flex-direction: row;
  align-items: center;
  padding: 8px 16px;
  gap: 16px;
  height: 80px;
  overflow: hidden;
}

.mobile-player .player-left {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  flex-shrink: 0;
}

.mobile-player .music-imgs {
  width: 240px;
  height: 240px;
  margin-bottom: 24px;
}

.mobile-player.collapsed .music-imgs {
  width: 56px;
  height: 56px;
  margin-bottom: 0;
  margin-right: 12px;
  flex-shrink: 0;
}

.mobile-player .player-controls {
  width: 100%;
  justify-content: center;
  gap: 24px;
  margin-top: 24px;
}

.mobile-player.collapsed .player-controls {
  display: flex;
  margin-top: 0;
  gap: 8px;
  flex-shrink: 0;
}

.mobile-player .player-right {
  width: 100%;
  margin-top: 24px;
}

.mobile-player.collapsed .player-right {
  flex: 1;
  min-width: 0;
  margin-top: 0;
}

.mobile-player .music-info {
  text-align: center;
  margin-bottom: 16px;
}

.mobile-player.collapsed .music-info {
  text-align: left;
  margin-bottom: 0;
}

.mobile-player .music-name {
  font-size: 20px;
  margin-bottom: 8px;
}

.mobile-player.collapsed .music-name {
  font-size: 14px;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.mobile-player .artist-name {
  font-size: 14px;
  margin-bottom: 12px;
}

.mobile-player.collapsed .music-details {
  grid-template-columns: 1fr;
  gap: 12px;
  margin-bottom: 16px;
}

.mobile-player.collapsed .music-description {
  display: none;
  margin-bottom: 68px;
}

/* 确保不遮挡底部导航栏 */
@media (max-width: 768px) {


  .album-header {
    margin: -28px -26px 26px;
    padding: 12px 24px 24px;
    background: rgba(0, 0, 0, 0.4);
    backdrop-filter: blur(30px);
    border: none;
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
    position: relative;
    overflow: hidden;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    min-height: auto;
    border-radius: 12px;
  }

  .album-header.expanded {
    padding: 32px 24px 48px;
    border-radius: 0 0 32px 32px;
    background: rgba(0, 0, 0, 0.6);
  }

  .album-info {
    display: flex;
    flex-direction: column;
    align-items: center;
    text-align: center;
    gap: 24px;
  }

  .album-cover {
    display: none;
    width: 200px;
    height: 200px;
    border-radius: 20px;
    overflow: hidden;
    margin: 0 auto;
    box-shadow: 0 12px 32px rgba(0, 0, 0, 0.3);
    border: 2px solid rgba(255, 255, 255, 0.2);
  }

  .album-header.expanded .album-cover {
    display: block;
  }

  .album-meta {
    width: 100%;
    text-align: center;
  }

  .album-meta h1 {
    font-size: 16px;
    font-weight: 600;
    margin: 0;
    color: white;
    text-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  }

  .album-header.expanded .album-meta h1 {
    font-size: 28px;
    margin-bottom: 16px;
  }

  .description {
    display: none;
  }

  .album-header.expanded .description {
    display: block;
    font-size: 16px;
    line-height: 1.7;
    margin: 0 auto 24px;
    color: rgba(255, 255, 255, 0.95);
    text-shadow: 0 1px 4px rgba(0, 0, 0, 0.2);
    padding: 0;
    max-width: 540px;
    font-weight: 400;
  }

  .stats {
    display: flex;
    justify-content: center;
    gap: 8px;
    margin-top: 6px;
  }

  .stats span {
    font-size: 12px;
    color: rgba(255, 255, 255, 0.9);
    background: rgba(255, 255, 255, 0.15);
    padding: 4px 8px;
    border-radius: 12px;
    backdrop-filter: blur(10px);
    border: 1px solid rgba(255, 255, 255, 0.2);
  }

  .album-header.expanded .stats {
    margin-top: 24px;
    padding-top: 24px;
    border-top: 1px solid rgba(255, 255, 255, 0.1);
  }

  .album-header.expanded .stats span {
    padding: 8px 16px;
    font-size: 14px;
  }

  .album-header .drag-handle {
    position: absolute;
    bottom: 8px;
    left: 50%;
    transform: translateX(-50%);
    width: 40px;
    height: 4px;
    display: flex;
    justify-content: center;
    align-items: center;
    opacity: 0.7;
    transition: all 0.3s ease;
  }

  .album-header .handle-line {
    width: 100%;
    height: 4px;
    background: rgba(255, 255, 255, 0.3);
    border-radius: 2px;
  }

  .album-header.expanded .drag-handle {
    bottom: 16px;
    opacity: 1;
  }

  .album-header.expanded .handle-line {
    background: rgba(255, 255, 255, 0.5);
  }
}

@keyframes slideUp {
  from {
    transform: translateY(100%);
  }
  to {
    transform: translateY(0);
  }
}

/* 修复播放器头部样式 */
.player-header {
  height: 24px;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 8px 0;
  cursor: pointer;
}

.drag-handle {
  width: 40px;
  height: 4px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.handle-line {
  width: 100%;
  height: 4px;
  background: rgba(255, 255, 255, 0.3);
  border-radius: 2px;
}

/* 小屏幕下的音乐卡片样式调整 */
@media (max-width: 480px) {
  .music-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
    padding-top: 0;
    margin-left: -16px;
    margin-right: -16px;
    padding-bottom:  64px;
  }

  .music-card {
    padding: 12px;
  }

  .music-card-cover {
    width: 100px;
    height: 100px;
  }

  .music-title {
    font-size: 14px;
  }

  .music-artist {
    font-size: 12px;
  }

  .music-duration {
    font-size: 12px;
  }

  .action-btn {
    width: 28px;
    height: 28px;
    font-size: 12px;
  }

  .music-card-actions {
    top: 8px;
    right: 8px;
  }
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.music-imgs.rotating {
  animation: rotate 20s linear infinite;
}

.buffer-box {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  color: white;
}

.btn {
  width: 48px;
  height: 48px;
  border: none;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.15);
  color: white;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.btn:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.25);
  transform: scale(1.1);
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.play-pause {
  width: 56px;
  height: 56px;
  font-size: 28px;
  background: rgba(255, 255, 255, 0.2);
  border: 2px solid rgba(255, 255, 255, 0.3);
}

.play-pause:hover {
  background: rgba(255, 255, 255, 0.3);
  box-shadow: 0 4px 12px rgba(255, 255, 255, 0.2);
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.detail-label {
  font-weight: 500;
  color: rgba(255, 255, 255, 0.6);
}

.detail-value {
  color: rgba(255, 255, 255, 0.9);
}

/* 删除确认框样式 */
.delete-modal {
  max-width: 360px;
  padding: 0;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  animation: modalPop 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  height: 400px;
}

@keyframes modalPop {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.delete-modal-body {
  display: flex;
  flex-direction: column;
}

.delete-preview {
  width: 100%;
  height: 200px;
  overflow: hidden;
  position: relative;
  margin: 0;
  box-shadow: none;
  border-radius: 0;
}

.delete-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.delete-confirm-content {
  height: 200px;
  padding: 24px;
  text-align: center;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.delete-icon {
  font-size: 32px;
  color: #ff4d4f;
  margin-bottom: 16px;
  animation: iconShake 1s ease infinite;
}

@keyframes iconShake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-4px); }
  75% { transform: translateX(4px); }
}

.delete-message h3 {
  font-size: 18px;
  color: #333;
  margin: 0 0 8px;
  font-weight: 600;
}

.delete-message p {
  font-size: 14px;
  color: #666;
  margin: 0;
}

.delete-actions {
  display: flex;
  gap: 12px;
  margin-top: 24px;
  padding: 0 12px;
}

.delete-actions button {
  flex: 1;
  padding: 12px;
  border: none;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.delete-actions .btn-cancel {
  background: #f5f5f5;
  color: #666;
}

.delete-actions .btn-cancel:hover {
  background: #ebebeb;
}

.delete-actions .btn-delete {
  background: #ff4d4f;
  color: white;
  box-shadow: 0 4px 12px rgba(255, 77, 79, 0.2);
}

.delete-actions .btn-delete:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(255, 77, 79, 0.3);
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .delete-modal {
    width: 90%;
    margin: 16px;
  }

  .delete-preview {
    height: 200px;
  }

  .delete-confirm-content {
    padding: 20px;
  }

  .delete-message h3 {
    font-size: 16px;
  }

  .delete-message p {
    font-size: 13px;
  }

  .delete-actions button {
    padding: 10px;
    font-size: 14px;
  }
}

/* Upload modal styles */
@media (max-width: 768px) {

  .modal-content {
    width: 90%;
    height: 88vh;
    margin: 20px;
    border-radius: 20px;
    display: flex;
    flex-direction: column;
    max-width: 500px;
    background: rgba(255, 255, 255, 0.98);
    backdrop-filter: blur(20px);
    -webkit-backdrop-filter: blur(20px);
    box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
    animation: modalPop 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  }

  .modal-header {
    padding: 16px 20px;
    border-radius: 20px 20px 0 0;
  }

  .upload-container {
    flex-direction: column;
    height: 100%;
    overflow: hidden;
    background: transparent;
  }

  .upload-left-panel {
    width: 100%;
    height: auto;
    min-height: 180px;
    max-height: 35vh;
    overflow-y: auto;
    border-right: none;
    border-bottom: 1px solid #E8E8E8;
  }

  .upload-zone {
    padding: 16px;
  }

  .upload-right-panel {
    width: 100%;
    flex: 1;
    overflow-y: auto;
    padding: 16px;
    background: transparent;
  }

  .metadata-form {
    height: 100%;
    overflow-y: auto;
    padding: 16px;
    box-shadow: none;
    border: 1px solid #E8E8E8;
  }

  .form-fields {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .cover-preview {
    width: 140px;
    height: 140px;
    margin: 0 auto 16px;
  }

  .form-group input,
  .form-group textarea {
    padding: 10px;
    font-size: 14px;
  }

  .modal-footer {
    padding: 12px 16px;
    background: transparent;
    border-top: 1px solid #E8E8E8;
  }

  .btn-cancel,
  .btn-upload {
    padding: 10px 20px;
    font-size: 14px;
  }

  .upload-formats {
    margin: 12px 0;
  }

  .format-tag {
    padding: 3px 10px;
    font-size: 11px;
  }

  .upload-hint {
    font-size: 12px;
  }

  .files-header {
    padding: 8px 12px;
  }

  .file-item {
    padding: 8px;
  }

  .file-icon {
    width: 32px;
    height: 32px;
  }

  .file-name {
    font-size: 13px;
  }

  .file-meta {
    font-size: 11px;
  }
}

/* 添加弹出动画 */
@keyframes modalPop {
  from {
    opacity: 0;
    transform: scale(0.95) translateY(20px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

/* 小屏幕进一步优化 */
@media (max-width: 375px) {
  .modal-content {
    width: 95%;
    margin: 10px;
  }

  .cover-preview {
    width: 120px;
    height: 120px;
  }

  .modal-header h2 {
    font-size: 18px;
  }

  .btn-cancel,
  .btn-upload {
    padding: 8px 16px;
    font-size: 13px;
  }
}

.genre-input-group {
  display: flex;
  gap: 8px;
}

.genre-select {
  flex: 1;
  padding: 12px;
  border: 1px solid #D9D9D9;
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.3s ease;
  background: white;
}

.genre-select:hover {
  border-color: #1890FF;
}

.genre-select:focus {
  border-color: #1890FF;
  outline: none;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.1);
}

.toggle-genre-btn {
  padding: 0 12px;
  background: #F5F5F5;
  border: 1px solid #D9D9D9;
  border-radius: 8px;
  color: #666;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.toggle-genre-btn:hover {
  background: #E8E8E8;
  color: #333;
}

.toggle-genre-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>

