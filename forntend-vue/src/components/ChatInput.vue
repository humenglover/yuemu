<template>
  <div class="chat-input">
    <!-- 显示当前回复的消息 -->
    <div v-if="replyTo" class="reply-preview">
      <span>回复 <span v-if="type !== 'private'">{{ replyTo.sender?.userName }}</span>: "{{ replyTo.content }}"</span>
      <button class="cancel-reply-button" @click="cancelReply">取消回复</button>
    </div>

    <div class="input-area">
      <!-- 表情按钮 -->
      <button
        class="emoji-trigger"
        :class="{ active: showEmojiPicker }"
        @click="toggleEmojiPicker"
      >
        😊
      </button>

      <!-- 媒体按钮 -->
      <div class="media-buttons">
        <a-upload
          :show-upload-list="false"
          :before-upload="handleImageUpload"
          accept="image/*"
        >
          <button class="media-button" title="发送图片">
            <PictureOutlined />
          </button>
        </a-upload>

        <button
          class="media-button"
          @click="toggleAudioRecording"
          :class="{ recording: isRecording }"
          title="录制音频"
        >
          <AudioOutlined />
          <span v-if="isRecording" class="recording-time">{{ recordingTime }}s</span>
        </button>

        <button
          class="media-button"
          @click="showVideoUrlInput"
          title="添加视频"
        >
          <VideoCameraOutlined />
        </button>
      </div>

      <div class="input-group">
        <input
          v-model="inputMessage"
          type="text"
          class="message-input"
          :placeholder="isRecording ? '正在录音...' : '输入消息...'"
          @keyup.enter.prevent="sendMessage"
          @input="handleInput"
          :disabled="!connected || isRecording"
          ref="messageInput"
          @blur="handleInputBlur"
        />
        <button
          class="send-button"
          @click.prevent="sendMessage"
          :disabled="!connected || (isRecording && recordingTime < 1)"
        >
          <span v-if="isRecording">完成</span>
          <span v-else-if="loadingHistory" class="loading-dots">
            <span class="dot"></span>
            <span class="dot"></span>
            <span class="dot"></span>
          </span>
          <span v-else>发送</span>
        </button>
      </div>
    </div>

    <!-- 表情选择器 -->
    <div v-if="showEmojiPicker" class="emoji-picker-container">
      <EmojiPicker @select="onEmojiSelect" />
    </div>

    <!-- @提及弹出框 -->
    <div v-if="showMentionBox && String(spaceId) === '-2'" class="mention-box" :style="mentionBoxStyle">
      <div class="mention-item" @click="selectMention">
        <div class="mention-avatar">
          <img src="https://static.yuemutuku.com/space/1877028360151445505/2025-07-02_k9wqCp9awJ9iPEaC_thumbnail.webp" alt="悦木小助手" />
        </div>
        <div class="mention-info">
          <div class="mention-name">悦木小助手</div>
          <div class="mention-desc">AI助手</div>
        </div>
      </div>
    </div>

    <!-- 视频URL输入弹窗 -->
    <a-modal
      v-model:visible="videoUrlModalVisible"
      title="添加视频"
      @ok="handleVideoUrlConfirm"
      @cancel="videoUrlModalVisible = false"
      :okText="'确认'"
      :cancelText="'取消'"
    >
      <a-input
        v-model:value="videoUrl"
        placeholder="请输入视频URL地址"
        @keyup.enter="handleVideoUrlConfirm"
      />
      <div class="video-url-tips">
        <p>支持的视频格式：MP4, WebM</p>
        <p>请确保视频URL可以直接访问</p>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onUnmounted } from 'vue'
import { message } from 'ant-design-vue'
import { PictureOutlined, AudioOutlined, VideoCameraOutlined } from '@ant-design/icons-vue'
import EmojiPicker from './EmojiPicker.vue'
import { uploadPostImageUsingPost } from '@/api/pictureController'
import Recorder from 'recorderx'
import type { ChatMessage } from '@/utils/chatWebSocket'

const props = defineProps<{
  connected: boolean
  loadingHistory?: boolean
  type?: 'chat' | 'space' | 'private'
  spaceId?: number | string
  replyTo?: ChatMessage | null
}>()

const emit = defineEmits<{
  (e: 'send', message: {
    type: string
    content: string
    messageType: string
    messageUrl?: string
    messageSize?: number
    replyId?: string | number
    rootId?: string | number
    isAtAI?: boolean
  }): void
  (e: 'cancel-reply'): void
}>()

// 基本状态
const inputMessage = ref('')
const messageInput = ref<HTMLInputElement | null>(null)
const showEmojiPicker = ref(false)

// 录音相关
const isRecording = ref(false)
const recorder = ref<Recorder | null>(null)
const recordingTime = ref(0)
const recordTimer = ref<number | null>(null)

// 视频URL相关
const videoUrlModalVisible = ref(false)
const videoUrl = ref('')

// @提及相关
const showMentionBox = ref(false)
const mentionBoxStyle = ref({})
const mentionStartIndex = ref(-1)

// 表情选择器
const toggleEmojiPicker = () => {
  showEmojiPicker.value = !showEmojiPicker.value
}

const onEmojiSelect = (emoji: string) => {
  inputMessage.value += emoji
  showEmojiPicker.value = false
}

// 处理图片上传
const handleImageUpload = async (file: File) => {
  try {
    const res = await uploadPostImageUsingPost({}, {}, file)
    if (res.data.code === 0) {
      // 发送图片消息
      emit('send', {
        type: 'message',
        content: '发送了一张图片',
        messageType: 'image',
        messageUrl: res.data.data.url,
        messageSize: file.size,
        ...(props.replyTo ? {
          replyId: props.replyTo.id,
          rootId: props.replyTo.rootId || props.replyTo.id
        } : {})
      })
    }
  } catch (error) {
    console.error('图片上传失败:', error)
    message.error('图片上传失败，请重试')
  }
  return false
}

// 处理音频录制
const toggleAudioRecording = async () => {
  if (isRecording.value) {
    // 停止录音
    if (recorder.value) {
      const audioBlob = await recorder.value.stop()
      isRecording.value = false
      if (recordTimer.value) {
        clearInterval(recordTimer.value)
        recordTimer.value = null
      }

      // 上传音频文件
      try {
        const formData = new FormData()
        formData.append('file', audioBlob, 'audio.wav')
        const res = await uploadPostImageUsingPost({}, {}, audioBlob)
        if (res.data.code === 0) {
          // 发送音频消息
          emit('send', {
            type: 'message',
            content: '发送了一段语音',
            messageType: 'audio',
            messageUrl: res.data.data.url,
            messageSize: audioBlob.size,
            ...(props.replyTo ? {
              replyId: props.replyTo.id,
              rootId: props.replyTo.rootId || props.replyTo.id
            } : {})
          })
        }
      } catch (error) {
        console.error('音频上传失败:', error)
        message.error('音频上传失败，请重试')
      }
    }
  } else {
    // 开始录音
    try {
      recorder.value = new Recorder({
        sampleBits: 16,
        sampleRate: 16000,
        numChannels: 1
      })
      await recorder.value.start()
      isRecording.value = true
      recordingTime.value = 0
      recordTimer.value = window.setInterval(() => {
        recordingTime.value++
        // 限制最大录音时间为60秒
        if (recordingTime.value >= 60) {
          toggleAudioRecording()
        }
      }, 1000)
    } catch (error) {
      console.error('录音失败:', error)
      message.error('录音失败，请检查麦克风权限')
    }
  }
}

// 处理视频URL
const showVideoUrlInput = () => {
  videoUrlModalVisible.value = true
}

const handleVideoUrlConfirm = () => {
  if (!videoUrl.value) {
    message.warning('请输入视频URL')
    return
  }

  // 验证URL格式
  try {
    new URL(videoUrl.value)
  } catch {
    message.error('请输入有效的URL地址')
    return
  }

  // 发送视频消息
  emit('send', {
    type: 'message',
    content: '发送了一个视频',
    messageType: 'video',
    messageUrl: videoUrl.value,
    ...(props.replyTo ? {
      replyId: props.replyTo.id,
      rootId: props.replyTo.rootId || props.replyTo.id
    } : {})
  })

  videoUrl.value = ''
  videoUrlModalVisible.value = false
}

// 发送文本消息
const sendMessage = () => {
  if (isRecording.value) {
    toggleAudioRecording()
    return
  }

  if (!inputMessage.value.trim() || !props.connected) return

  const isAtAI = inputMessage.value.includes('@悦木小助手')
  const messageContent = inputMessage.value.trim()

  emit('send', {
    type: 'message',
    content: messageContent,
    messageType: 'text',
    ...(props.replyTo ? {
      replyId: props.replyTo.id,
      rootId: props.replyTo.rootId || props.replyTo.id
    } : {}),
    isAtAI
  })

  inputMessage.value = ''
}

// 处理@提及
const handleInput = (e: Event) => {
  const target = e.target as HTMLInputElement
  const value = target.value
  const cursorPosition = target.selectionStart || 0

  if (value[cursorPosition - 1] === '@') {
    mentionStartIndex.value = cursorPosition - 1
    showMentionBox.value = true

    // 计算提及框的位置
    const inputRect = target.getBoundingClientRect()
    const isMobile = window.innerWidth <= 768

    if (isMobile) {
      mentionBoxStyle.value = {
        position: 'fixed',
        left: '16px',
        right: '16px',
        bottom: '80px',
        width: 'auto',
        top: 'auto',
        zIndex: 1000
      }
    } else {
      mentionBoxStyle.value = {
        position: 'absolute',
        bottom: '100%',
        left: '0',
        marginBottom: '8px',
        width: '240px',
        zIndex: 1000
      }
    }
  } else if (showMentionBox.value) {
    const textFromMentionStart = value.slice(mentionStartIndex.value, cursorPosition)
    if (!textFromMentionStart.startsWith('@') || textFromMentionStart.includes(' ')) {
      showMentionBox.value = false
    }
  }
}

const handleInputBlur = () => {
  setTimeout(() => {
    showMentionBox.value = false
  }, 200)
}

const selectMention = () => {
  if (mentionStartIndex.value >= 0) {
    const beforeMention = inputMessage.value.slice(0, mentionStartIndex.value)
    const afterMention = inputMessage.value.slice(mentionStartIndex.value + 1)
    inputMessage.value = beforeMention + '@悦木小助手 ' + afterMention
  }
  showMentionBox.value = false
  mentionStartIndex.value = -1
  messageInput.value?.focus()
}

// 取消回复
const cancelReply = () => {
  emit('cancel-reply')
}

// 组件卸载时清理
onUnmounted(() => {
  if (recordTimer.value) {
    clearInterval(recordTimer.value)
  }
  if (recorder.value && isRecording.value) {
    recorder.value.stop()
  }
})
</script>

<style scoped>
.chat-input {
  padding: 16px;
  position: relative;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 16px;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.05);
  margin-top: 0;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
  border: 1px solid rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(8px);
}

.input-area {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
}

.media-buttons {
  display: flex;
  gap: 8px;
}

.media-button {
  background: none;
  border: none;
  font-size: 20px;
  padding: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  border-radius: 50%;
  color: #666;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.media-button:hover {
  background: rgba(0, 0, 0, 0.05);
  color: #1890ff;
}

.media-button.recording {
  color: #ff4d4f;
  animation: pulse 1s infinite;
}

.recording-time {
  position: absolute;
  top: -8px;
  right: -8px;
  background: #ff4d4f;
  color: white;
  font-size: 12px;
  padding: 2px 4px;
  border-radius: 8px;
  min-width: 20px;
  text-align: center;
}

@keyframes pulse {
  0% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.1);
  }
  100% {
    transform: scale(1);
  }
}

.emoji-trigger {
  background: none;
  border: none;
  font-size: 20px;
  padding: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  border-radius: 50%;
  color: #666;
}

.emoji-trigger:hover,
.emoji-trigger.active {
  background: rgba(0, 0, 0, 0.05);
  transform: scale(1.1);
  color: #1890ff;
}

.input-group {
  display: flex;
  flex: 1;
  background: rgba(0, 0, 0, 0.05);
  padding: 4px;
  border-radius: 16px;
  border: 1px solid rgba(0, 0, 0, 0.1);
}

.message-input {
  flex: 1;
  padding: 12px;
  border: none;
  background: transparent;
  font-size: 14px;
  color: #333;
  outline: none;
}

.message-input::placeholder {
  color: #999;
}

.send-button {
  padding: 8px 24px;
  border-radius: 12px;
  border: none;
  background: #1890ff;
  color: white;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  min-width: 80px;
}

.send-button:hover {
  background: #40a9ff;
}

.send-button:disabled {
  background: #d9d9d9;
  cursor: not-allowed;
}

.reply-preview {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  padding: 8px 16px;
  border-radius: 12px;
  background: rgba(24, 144, 255, 0.1);
  border-left: 3px solid #1890ff;
  font-size: 13px;
  color: #1890ff;
  width: 100%;
}

.cancel-reply-button {
  padding: 4px 12px;
  border-radius: 12px;
  border: none;
  background: rgba(0, 0, 0, 0.1);
  color: #666;
  font-size: 12px;
  cursor: pointer;
}

.video-url-tips {
  margin-top: 12px;
  font-size: 12px;
  color: #999;
}

.video-url-tips p {
  margin: 4px 0;
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .chat-input {
    padding: 12px;
  }

  .input-area {
    gap: 8px;
  }

  .media-button,
  .emoji-trigger {
    padding: 6px;
    font-size: 18px;
  }

  .send-button {
    padding: 8px 16px;
    min-width: 60px;
  }
}
</style>
