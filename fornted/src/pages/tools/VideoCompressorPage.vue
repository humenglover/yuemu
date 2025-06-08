<template>
  <div class="video-compressor-container">
    <h1 class="tool-title">智压视界</h1>
    <div class="tool-description">
      专业的视频压缩工具，采用先进的压缩算法，在保持高画质的同时显著减小文件体积
    </div>

    <div class="main-content">
      <div class="upload-section">
        <div
          class="upload-zone"
          @drop.prevent="handleDrop"
          @dragover.prevent
          @click="triggerFileInput"
        >
          <input
            type="file"
            ref="fileInput"
            accept="video/*"
            @change="handleFileSelect"
            style="display: none"
          >
          <div v-if="!selectedFile" class="upload-placeholder">
            <div class="upload-icon">
              <i class="fas fa-cloud-upload-alt"></i>
            </div>
            <div class="upload-text">
              点击或拖拽视频文件到此处
            </div>
            <div class="upload-hint">
              支持 MP4, WebM, AVI 等格式
            </div>
          </div>
          <div v-else class="video-preview">
            <video ref="videoPreview" controls></video>
            <div class="video-info">
              <p>文件名：{{ selectedFile.name }}</p>
              <p>大小：{{ formatFileSize(selectedFile.size) }}</p>
              <p>时长：{{ duration }}</p>
              <p>分辨率：{{ resolution }}</p>
              <p>帧率：{{ frameRate }}</p>
              <p>比特率：{{ bitrate }}</p>
            </div>
          </div>
        </div>
      </div>

      <div class="control-section">
        <div v-if="selectedFile" class="compression-options">
          <h3>压缩设置</h3>
          <div class="option-group">
            <label>压缩质量</label>
            <select v-model="compressionQuality">
              <option value="high">高质量 (2000kbps)</option>
              <option value="medium">中等质量 (1000kbps)</option>
              <option value="low">低质量 (500kbps)</option>
            </select>
          </div>

          <div class="option-group">
            <label>输出格式</label>
            <select v-model="outputFormat">
              <option value="mp4">MP4</option>
              <option value="webm">WebM</option>
            </select>
          </div>

          <div class="compression-actions">
            <button
              class="compress-btn"
              :disabled="isCompressing"
              @click="startCompression"
            >
              {{ isCompressing ? '压缩中...' : '开始压缩' }}
            </button>
            <button class="reset-btn" @click="resetFile">重新选择</button>
          </div>
        </div>

        <div v-if="isCompressing" class="progress-section">
          <div class="progress-bar">
            <div
              class="progress-fill"
              :style="{ width: `${compressionProgress}%` }"
            ></div>
          </div>
          <div class="progress-text">{{ progressText }}</div>
        </div>

        <div v-if="compressedFile" class="result-section">
          <div class="comparison">
            <div class="comparison-item">
              <h3>原始文件</h3>
              <p>大小：{{ formatFileSize(selectedFile?.size || 0) }}</p>
              <p>分辨率：{{ resolution }}</p>
              <p>帧率：{{ frameRate }}</p>
              <p>比特率：{{ bitrate }}</p>
            </div>
            <div class="comparison-arrow">→</div>
            <div class="comparison-item">
              <h3>压缩后</h3>
              <p>大小：{{ formatFileSize(compressedFile.size) }}</p>
              <p>分辨率：{{ compressedResolution }}</p>
              <p>帧率：{{ compressedFrameRate }}</p>
              <p>比特率：{{ compressedBitrate }}</p>
            </div>
          </div>
          <div class="compression-ratio">
            压缩率：{{ ((1 - (compressedFile.size / (selectedFile?.size || 1))) * 100).toFixed(1) }}%
          </div>
          <button class="download-btn" @click="downloadCompressedFile">
            下载压缩后的文件
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { createFFmpeg, fetchFile } from '@ffmpeg/ffmpeg'

// 状态变量
const fileInput = ref<HTMLInputElement | null>(null)
const videoPreview = ref<HTMLVideoElement | null>(null)
const selectedFile = ref<File | null>(null)
const compressedFile = ref<File | null>(null)
const isCompressing = ref(false)
const compressionProgress = ref(0)
const progressText = ref('')
const duration = ref('')
const resolution = ref('')
const compressedResolution = ref('')
const compressionQuality = ref('medium')
const outputFormat = ref('mp4')
const frameRate = ref('')
const bitrate = ref('')
const compressedFrameRate = ref('')
const compressedBitrate = ref('')

// FFmpeg 实例
const ffmpeg = createFFmpeg({
  corePath: '/ffmpeg/ffmpeg-core.js',
  log: true
})

// 确保在使用前加载 FFmpeg
onMounted(async () => {
  try {
    await ffmpeg.load()
    console.log('FFmpeg 加载成功')
  } catch (error) {
    console.error('FFmpeg 加载失败:', error)
  }
})

// 解析FFmpeg时间格式为秒
const parseTimeToSeconds = (timeStr: string): number => {
  const parts = timeStr.split(':')
  if (parts.length === 3) {
    const [hours, minutes, seconds] = parts
    return (
      parseFloat(hours) * 3600 +
      parseFloat(minutes) * 60 +
      parseFloat(seconds)
    )
  }
  return 0
}

// 文件选择处理
const triggerFileInput = () => {
  fileInput.value?.click()
}

const handleFileSelect = (event: Event) => {
  const input = event.target as HTMLInputElement
  if (input.files && input.files[0]) {
    processSelectedFile(input.files[0])
  }
}

const handleDrop = (event: DragEvent) => {
  const file = event.dataTransfer?.files[0]
  if (file && file.type.startsWith('video/')) {
    processSelectedFile(file)
  }
}

// 处理选中的文件
const processSelectedFile = (file: File) => {
  selectedFile.value = file
  compressedFile.value = null
  compressionProgress.value = 0

  const videoURL = URL.createObjectURL(file)
  const video = document.createElement('video')
  video.preload = 'metadata'

  video.onloadedmetadata = () => {
    duration.value = formatDuration(video.duration)
    resolution.value = `${video.videoWidth}x${video.videoHeight}`

    // 获取视频的帧率和比特率
    try {
      // @ts-ignore
      const stream = video.captureStream()
      const videoTrack = stream.getVideoTracks()[0]
      const settings = videoTrack.getSettings()
      frameRate.value = settings.frameRate ? `${settings.frameRate} fps` : '未知'

      // 计算比特率
      const fileSize = file.size
      const videoDuration = video.duration
      const bitrateMbps = (fileSize * 8) / (videoDuration * 1000000)
      bitrate.value = `${bitrateMbps.toFixed(2)} Mbps`
    } catch (error) {
      console.error('获取视频信息失败:', error)
      frameRate.value = '未知'
      bitrate.value = '未知'
    }

    // 更新预览视频
    if (videoPreview.value) {
      videoPreview.value.src = videoURL
    }
  }

  video.onerror = () => {
    console.error('视频加载失败')
    duration.value = '未知'
    resolution.value = '未知'
    frameRate.value = '未知'
    bitrate.value = '未知'
  }

  video.src = videoURL
}

// 开始压缩
const startCompression = async () => {
  if (!selectedFile.value || isCompressing.value) return

  try {
    isCompressing.value = true
    progressText.value = '初始化 ...'
    compressionProgress.value = 0

    // 加载 FFmpeg
    if (!ffmpeg.isLoaded()) {
      await ffmpeg.load()
    }

    const inputFileName = 'input.' + selectedFile.value.name.split('.').pop()
    const outputFileName = `output.${outputFormat.value}`

    // 写入输入文件
    progressText.value = '处理输入文件...'
    ffmpeg.FS('writeFile', inputFileName, await fetchFile(selectedFile.value))

    // 设置压缩参数
    const bitrateMap = {
      high: '2000k',
      medium: '1000k',
      low: '500k'
    }

    // 构建 FFmpeg 命令
    const args = [
      '-i', inputFileName,
      '-c:v', outputFormat.value === 'webm' ? 'libvpx-vp9' : 'libx264',
      '-b:v', bitrateMap[compressionQuality.value as keyof typeof bitrateMap],
      '-c:a', outputFormat.value === 'webm' ? 'libvorbis' : 'aac',
      '-strict', '-2',
      outputFileName
    ]

    progressText.value = '压缩中...'
    await ffmpeg.run(...args)

    // 读取输出文件
    progressText.value = '处理输出文件...'
    const data = ffmpeg.FS('readFile', outputFileName)
    const uint8Array = new Uint8Array(data.buffer)
    const blob = new Blob([uint8Array], {
      type: `video/${outputFormat.value}`
    })
    compressedFile.value = new File([blob],
      `compressed_${selectedFile.value.name.replace(/\.[^/.]+$/, '')}.${outputFormat.value}`,
      { type: `video/${outputFormat.value}` }
    )

    // 清理临时文件
    ffmpeg.FS('unlink', inputFileName)
    ffmpeg.FS('unlink', outputFileName)

    // 创建临时视频元素来获取压缩后的视频信息
    const compressedVideoURL = URL.createObjectURL(compressedFile.value)
    const tempVideo = document.createElement('video')
    tempVideo.src = compressedVideoURL

    await new Promise((resolve) => {
      tempVideo.onloadedmetadata = () => {
        compressedResolution.value = `${tempVideo.videoWidth}x${tempVideo.videoHeight}`

        // 获取压缩后视频的帧率和比特率
        // @ts-ignore
        const stream = tempVideo.captureStream()
        const videoTrack = stream.getVideoTracks()[0]
        const settings = videoTrack.getSettings()
        compressedFrameRate.value = settings.frameRate ? `${settings.frameRate} fps` : '未知'

        // 计算压缩后的比特率
        const fileSize = compressedFile.value!.size
        const duration = tempVideo.duration
        const bitrateMbps = (fileSize * 8) / (duration * 1000000)
        compressedBitrate.value = `${bitrateMbps.toFixed(2)} Mbps`

        URL.revokeObjectURL(compressedVideoURL)
        resolve(null)
      }
    })

    progressText.value = '压缩完成！'
    compressionProgress.value = 100

  } catch (error) {
    console.error('压缩失败:', error)
    progressText.value = `压缩失败: ${error}`
  } finally {
    isCompressing.value = false
  }
}

// 下载压缩后的文件
const downloadCompressedFile = () => {
  if (!compressedFile.value) return

  const url = URL.createObjectURL(compressedFile.value)
  const a = document.createElement('a')
  a.href = url
  a.download = compressedFile.value.name
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

// 重置文件选择
const resetFile = () => {
  selectedFile.value = null
  compressedFile.value = null
  if (videoPreview.value) {
    videoPreview.value.src = ''
  }
  duration.value = ''
  resolution.value = ''
  compressionProgress.value = 0
  progressText.value = ''
}

// 工具函数
const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const formatDuration = (seconds: number): string => {
  if (!seconds || isNaN(seconds)) return '未知'

  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const remainingSeconds = Math.floor(seconds % 60)

  if (hours > 0) {
    return `${hours}:${minutes.toString().padStart(2, '0')}:${remainingSeconds.toString().padStart(2, '0')}`
  }
  return `${minutes}:${remainingSeconds.toString().padStart(2, '0')}`
}

// 组件卸载时清理
onUnmounted(() => {
  if (ffmpeg.isLoaded()) {
    try {
      ffmpeg.terminate()
    } catch (error) {
      console.error('FFmpeg 清理失败:', error)
    }
  }
})
</script>

<style scoped>
/* 添加全屏背景 */
.video-compressor-container {
  position: relative;
  max-width: 1200px;
  margin: 0 auto;
  padding: 32px;
  min-height: calc(100vh - 74px);
  z-index: 1;
}

/* 创建动态背景层 */
.video-compressor-container::before {
  content: '';
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background:
    linear-gradient(120deg, rgba(64, 158, 255, 0.1), rgba(103, 194, 58, 0.1)),
    radial-gradient(circle at 50% 0%, rgba(64, 158, 255, 0.15), transparent 40%),
    radial-gradient(circle at 100% 0%, rgba(103, 194, 58, 0.15), transparent 40%),
    radial-gradient(circle at 100% 100%, rgba(245, 34, 45, 0.15), transparent 40%),
    radial-gradient(circle at 0% 100%, rgba(64, 158, 255, 0.15), transparent 40%);
  background-color: #f5f7fa;
  z-index: -1;
  animation: gradientAnimation 15s ease infinite;
}

/* 添加动态背景动画 */
@keyframes gradientAnimation {
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

/* 添加装饰性图形 */
.video-compressor-container::after {
  content: '';
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image:
    radial-gradient(circle at 20% 20%, rgba(64, 158, 255, 0.05) 0%, transparent 8%),
    radial-gradient(circle at 80% 80%, rgba(103, 194, 58, 0.05) 0%, transparent 8%),
    radial-gradient(circle at 50% 50%, rgba(245, 34, 45, 0.05) 0%, transparent 8%),
    radial-gradient(circle at 80% 20%, rgba(64, 158, 255, 0.05) 0%, transparent 8%),
    radial-gradient(circle at 20% 80%, rgba(103, 194, 58, 0.05) 0%, transparent 8%);
  z-index: -1;
  pointer-events: none;
}

/* 内容区域样式优化 */
.main-content {
  position: relative;
  display: flex;
  gap: 32px;
  margin-bottom: 74px;
  z-index: 2;
}

/* 更新卡片背景，增加玻璃态效果 */
.upload-zone,
.compression-options,
.progress-section,
.result-section {
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

/* 增强卡片悬停效果 */
.upload-zone:hover,
.compression-options:hover,
.progress-section:hover,
.result-section:hover {
  background: rgba(255, 255, 255, 0.9);
  transform: translateY(-2px);
  box-shadow: 0 12px 48px rgba(64, 158, 255, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.5);
}

.tool-title {
  font-size: 32px;
  font-weight: bold;
  color: #1a1a1a;
  text-align: center;
  margin-bottom: 16px;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.tool-description {
  text-align: center;
  color: #666;
  margin-bottom: 40px;
  font-size: 16px;
  max-width: 600px;
  margin-left: auto;
  margin-right: auto;
  line-height: 1.6;
}

.upload-section, .control-section {
  flex: 1;
  min-width: 0;
}

.upload-zone {
  border: 2px dashed #c0ccda;
  border-radius: 16px;
  padding: 32px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
}

.upload-zone:hover {
  border-color: #409EFF;
  transform: translateY(-2px);
  box-shadow: 0 12px 32px rgba(64, 158, 255, 0.1);
}

.upload-placeholder {
  padding: 40px 20px;
}

.upload-icon {
  font-size: 56px;
  color: #409EFF;
  margin-bottom: 24px;
  opacity: 0.9;
}

.upload-text {
  font-size: 20px;
  margin-bottom: 12px;
  color: #2c3e50;
  font-weight: 500;
}

.upload-hint {
  font-size: 14px;
  color: #909399;
}

.video-preview {
  max-width: 100%;
}

.video-preview video {
  width: 100%;
  border-radius: 12px;
  margin-bottom: 20px;
  background: #000;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

.video-info {
  text-align: left;
  padding: 24px;
  border-radius: 12px;
}

.video-info p {
  margin: 12px 0;
  color: #5a6a7f;
  font-size: 14px;
  display: flex;
  justify-content: space-between;
}

.compression-options {
  padding: 28px;
}

.compression-options h3 {
  margin: 0 0 24px;
  color: #2c3e50;
  font-size: 20px;
  font-weight: 600;
}

.option-group {
  margin-bottom: 24px;
}

.option-group label {
  display: block;
  margin-bottom: 10px;
  color: #5a6a7f;
  font-size: 14px;
  font-weight: 500;
}

.option-group select {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #fff;
  font-size: 14px;
  color: #2c3e50;
  transition: all 0.3s ease;
  cursor: pointer;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='16' height='16' viewBox='0 0 24 24' fill='none' stroke='%23606266' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M6 9l6 6 6-6'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 12px center;
  padding-right: 40px;
}

.option-group select:hover {
  border-color: #409EFF;
}

.option-group select:focus {
  outline: none;
  border-color: #409EFF;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

.compression-actions {
  display: flex;
  gap: 16px;
  margin-top: 32px;
}

button {
  padding: 14px 28px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 15px;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.compress-btn {
  background: linear-gradient(135deg, #409EFF 0%, #3182f6 100%);
  color: white;
  flex: 2;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

.compress-btn:hover {
  background: linear-gradient(135deg, #66b1ff 0%, #4e94f8 100%);
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(64, 158, 255, 0.4);
}

.compress-btn:disabled {
  background: linear-gradient(135deg, #a0cfff 0%, #8ab4e8 100%);
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.reset-btn {
  background: linear-gradient(135deg, #ff4d4f 0%, #f5222d 100%);
  color: white;
  flex: 1;
  box-shadow: 0 4px 12px rgba(245, 34, 45, 0.3);
}

.reset-btn:hover {
  background: linear-gradient(135deg, #ff7875 0%, #ff4d4f 100%);
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(245, 34, 45, 0.4);
}

.progress-section {
  padding: 24px;
}

.progress-bar {
  height: 8px;
  background: #e4e7ed;
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 12px;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #409EFF 0%, #3182f6 100%);
  transition: width 0.3s ease;
}

.progress-text {
  text-align: center;
  color: #5a6a7f;
  font-size: 14px;
  margin-top: 12px;
}

.result-section {
  padding: 28px;
}

.comparison {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  gap: 24px;
}

.comparison-item {
  flex: 1;
  text-align: center;
  padding: 20px;
  border-radius: 12px;
  transition: all 0.3s ease;
}

.comparison-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.05);
}

.comparison-item h3 {
  margin-bottom: 16px;
  color: #2c3e50;
  font-size: 18px;
  font-weight: 600;
}

.comparison-item p {
  margin: 10px 0;
  color: #5a6a7f;
  font-size: 14px;
}

.comparison-arrow {
  color: #409EFF;
  font-size: 28px;
  opacity: 0.8;
}

.compression-ratio {
  text-align: center;
  margin: 20px 0;
  padding: 16px;
  background: linear-gradient(135deg, #f0f9eb 0%, #e7f6df 100%);
  color: #67c23a;
  border-radius: 8px;
  font-weight: 600;
  font-size: 16px;
  box-shadow: 0 2px 8px rgba(103, 194, 58, 0.1);
}

.download-btn {
  width: 100%;
  background: linear-gradient(135deg, #67c23a 0%, #55a532 100%);
  color: white;
  margin-top: 20px;
  box-shadow: 0 4px 12px rgba(103, 194, 58, 0.3);
}

.download-btn:hover {
  background: linear-gradient(135deg, #85ce61 0%, #67c23a 100%);
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(103, 194, 58, 0.4);
}

/* 移动端响应式布局 */
@media (max-width: 768px) {
  .video-compressor-container {
    padding: 20px;
  }

  .main-content {
    flex-direction: column;
    gap: 24px;
  }

  .tool-title {
    font-size: 24px;
  }

  .tool-description {
    font-size: 14px;
    margin-bottom: 32px;
  }

  .upload-zone {
    padding: 20px;
  }

  .upload-placeholder {
    padding: 32px 16px;
  }

  .upload-icon {
    font-size: 48px;
  }

  .upload-text {
    font-size: 18px;
  }

  .compression-options,
  .progress-section,
  .result-section {
    padding: 20px;
  }

  .comparison {
    flex-direction: column;
    gap: 20px;
  }

  .comparison-arrow {
    transform: rotate(90deg);
  }

  .compression-actions {
    flex-direction: column;
    gap: 12px;
  }

  .compress-btn,
  .reset-btn {
    width: 100%;
    padding: 12px 20px;
  }

  button {
    font-size: 14px;
  }
}
</style>
