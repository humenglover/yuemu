<template>
  <div class="wishes-board">
    <!-- 顶部标题栏 -->
    <div class="board-header" @click="showPublishModal">
      <div class="header-content">
        <i class="fa fa-pencil"></i>
        <span>书写下你的留言祝福～</span>
      </div>
    </div>

    <!-- 统计信息 -->
    <div class="stats-info">
      <div class="stats-content">
        祝福留言共 {{ wishes.length }} 条 当前显示 {{ wishes.length }} 条
      </div>
    </div>

    <!-- 留言列表 -->
    <div class="messages-grid">
      <!-- 空状态展示 -->
      <div v-if="wishes.length === 0" class="empty-state">
        <div class="empty-icon">💌</div>
        <h3>还没有祝福留言</h3>
        <p>点击顶部按钮，写下你的第一条祝福吧！</p>
      </div>
      <!-- 留言列表 -->
      <div v-else v-for="wish in wishes" :key="wish.id" class="message-item">
        <div class="message-header">
          <div class="user-info">
            <img :src="'https://q1.qlogo.cn/g?b=qq&nk=' + wish.qq + '&s=100'"
                 class="avatar"
                 :class="{ 'avatar-loading': !isAvatarLoaded[wish.id] }"
                 @load="handleAvatarLoad(wish.id)"
                 @error="handleAvatarLoad(wish.id)"
                 alt="用户头像" />
            <div class="user-meta">
              <span class="nickname">{{ wish.nickname }}</span>
              <span class="location" v-if="wish.location">
                <i class="fa fa-map-marker location-icon"></i>
                {{ wish.location }}
              </span>
            </div>
          </div>
          <div class="message-meta">
            <span class="time">{{ formatTime(wish.createTime) }}</span>
          </div>
        </div>
        <div class="message-content">{{ wish.content }}</div>
        <div class="message-footer">
          <div class="system-info">
            <span class="system-detail">
              <i class="fa fa-chrome browser-icon"></i>
              Chrome
            </span>
            <span class="system-detail">
              <i class="fa fa-windows os-icon"></i>
              Windows
            </span>
          </div>
          <div class="message-actions">
            <button
              class="action-btn like-btn"
              :class="{ 'liked': wish.isLiked }"
              @click="handleLike(wish)"
            >
              <span>{{ wish.likeCount || 0 }}</span>
            </button>
            <button
              v-if="isOwner"
              class="action-btn delete-btn"
              @click="handleDelete(wish)"
            ></button>
          </div>
        </div>
      </div>
    </div>

    <!-- 发布弹窗 -->
    <div class="modal" v-if="showModal" @click.self="closeModal">
      <div class="modal-content">
        <div class="modal-header">
          <div class="modal-title">
            <span>发布留言</span>
          </div>
          <span class="close-btn" @click="closeModal">&times;</span>
        </div>
        <div class="modal-body">
          <div class="user-info-section">
            <img :src="'https://q1.qlogo.cn/g?b=qq&nk=' + (newWish.qq || '10000') + '&s=100'" class="user-avatar">
            <div class="system-info">
              <div class="location-info">
                <i class="fa fa-map-marker location-icon"></i>
                {{ location || '中国' }}
              </div>
              <div class="browser-info">
                <i class="fa fa-windows os-icon"></i>
                Windows
                <i class="fa fa-chrome browser-icon"></i>
                Chrome
              </div>
            </div>
          </div>
          <div class="form-section">
            <div class="input-group">
              <input
                v-model="newWish.qq"
                type="text"
                class="form-input"
                placeholder="请输入QQ号码"
                @blur="fetchQQInfo"
              >
            </div>
            <div class="input-group">
              <input
                v-model="newWish.nickname"
                type="text"
                class="form-input"
                placeholder="昵称"
              >
            </div>
            <div class="input-group">
              <textarea
                v-model="newWish.content"
                class="form-textarea"
                placeholder="请输入留言内容..."
                rows="4"
              ></textarea>
              <div class="random-text">
                <button type="button" class="random-btn" @click="getRandomText">
                  <i class="fa fa-magic"></i>
                  随机一言
                </button>
              </div>
            </div>
            <div class="submit-section">
              <button
                class="submit-btn"
                :class="{ 'submitting': publishing }"
                @click="handlePublish"
                :disabled="!newWish.content.trim() || publishing"
              >
                {{ publishing ? '提交留言中...' : '提交留言' }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 删除确认弹框 -->
    <div class="confirm-modal" v-if="showDeleteConfirm" @click.self="cancelDelete">
      <div class="confirm-content">
        <div class="confirm-icon">
          <span>(｡•́︿•̀｡)</span>
        </div>
        <div class="confirm-title">确定要删除这条留言吗？</div>
        <div class="confirm-buttons">
          <button class="cancel-btn" @click="cancelDelete">取消</button>
          <button class="confirm-btn" @click="confirmDelete">确定</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  addMessageUsingPost,
  listMessagesByPageUsingGet,
  deleteMessageUsingDelete,
  likeMessageUsingPost,
} from '@/api/messageBoardController'
import { formatTime } from '@/utils/dateUtils'
import MessageBoard = API.MessageBoard

interface Wish extends MessageBoard {
  isLiked?: boolean;
}

interface NewWish {
  content: string
  nickname: string
  qq: string
}

const props = defineProps<{
  ownerId: number
  isOwner: boolean
}>()

const wishes: Ref<Wish[]> = ref([])
const showModal = ref(false)
const publishing = ref(false)
const newWish: Ref<NewWish> = ref({
  content: '',
  nickname: '',
  qq: ''
})
const location = ref('')
const showDeleteConfirm = ref(false)
const wishToDelete = ref<Wish | null>(null)

// 添加头像加载状态管理
const isAvatarLoaded = ref<Record<number, boolean>>({})

const showPublishModal = () => {
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
  newWish.value = {
    content: '',
    nickname: '',
    qq: ''
  }
}

const fetchQQInfo = async () => {
  const qq = newWish.value.qq
  if (!qq || !/^\d{6,10}$/.test(qq)) {
    message.warning('请输入正确的QQ号码')
    return
  }

  try {
    const response = await fetch(`https://jkapi.com/api/qqinfo?qq=${qq}`)
    const data = await response.json()
    if (data.code === 200) {
      newWish.value.nickname = data.nick
    } else {
    }
  } catch (error) {
  }
}

const handlePublish = async () => {
  if (!newWish.value.qq || !newWish.value.nickname || !newWish.value.content.trim()) {
    return
  }

  if (!/^\d{6,10}$/.test(newWish.value.qq)) {
    message.warning('请输入正确的QQ号码')
    return
  }

  if (newWish.value.content.length < 2) {
    message.warning('留言内容至少需要2个字符')
    return
  }

  publishing.value = true
  try {
    const response = await addMessageUsingPost({
      content: newWish.value.content,
      nickname: newWish.value.nickname,
      qq: newWish.value.qq,
      location: location.value,
      ownerId: props.ownerId
    })

    if (response.data.code === 0) {

      closeModal()
      loadMessages()
    } else {

    }
  } catch (error) {

  } finally {
    publishing.value = false
  }
}

const loadMessages = async () => {
  try {
    const params: listMessagesByPageUsingGETParams = {
      ownerId: props.ownerId,
      current: 1,
      size: 50
    }
    const response = await listMessagesByPageUsingGet(params)
    if (response.data.code === 0) {
      wishes.value = response.data.data.records || []
    }
  } catch (error) {
    message.error('加载留言失败')
  }
}

const getLocation = () => {
  // 默认设置位置为"中国"
  location.value = '中国'

  // 使用fetch API直接获取位置信息
  fetch('https://api.myip.la/cn?json')
    .then(response => response.json())
    .then(data => {
      if (data && data.location) {
        const province = data.location.province || '陕西'
        const city = data.location.city || '西安'
        // 组合省市名称
        location.value = `${province}${city}`
      }
    })
    .catch(() => {
      // 如果获取失败，直接使用默认值
      location.value = '陕西西安'
    })
}

const getRandomText = async () => {
  try {
    const response = await fetch('https://v1.hitokoto.cn')
    const data = await response.json()
    newWish.value.content = data.hitokoto
  } catch (error) {
    message.error('获取随机一言失败')
  }
}

// 处理点赞
const handleLike = async (wish: Wish) => {
  if (!wish.id) return

  try {
    const response = await likeMessageUsingPost({ id: wish.id })
    if (response.data.code === 0) {
      // 更新点赞状态和数量
      wish.isLiked = !wish.isLiked
      wish.likeCount = (wish.likeCount || 0) + (wish.isLiked ? 1 : -1)
    } else {
    }
  } catch (error) {
  }
}

// 处理删除
const handleDelete = (wish: Wish) => {
  showDeleteConfirm.value = true
  wishToDelete.value = wish
}

const cancelDelete = () => {
  showDeleteConfirm.value = false
  wishToDelete.value = null
}

const confirmDelete = async () => {
  if (!wishToDelete.value?.id) return

  try {
    const response = await deleteMessageUsingDelete({
      id: wishToDelete.value.id,
      ownerId: props.ownerId
    })

    if (response.data.code === 0) {
      wishes.value = wishes.value.filter(item => item.id !== wishToDelete.value?.id)
      message.success('删除成功 (｡◕‿◕｡)')
    } else {
      message.error('删除失败，请重试 (╥﹏╥)')
    }
  } catch (error) {
    message.error('删除失败，请重试 (╥﹏╥)')
  } finally {
    showDeleteConfirm.value = false
    wishToDelete.value = null
  }
}

// 处理头像加载完成
const handleAvatarLoad = (wishId: number) => {
  isAvatarLoaded.value[wishId] = true
}

onMounted(() => {
  loadMessages()
  getLocation()
})
</script>

<style scoped>
.wishes-board {
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  min-height: 100vh;
}

/* 顶部标题栏 */
.board-header {
  background: #4a4a4a;
  border-radius: 12px;
  padding: 16px 24px;
  margin-bottom: 24px;
  cursor: pointer;
  color: #ffffff;
  transition: transform 0.2s ease;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.board-header:hover {
  transform: translateY(-2px);
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: center;

  font-size: 16px;
  gap: 8px;
}

.header-content i {
  font-size: 18px;
  color: #666;
}

/* 统计信息 */
.stats-info {
  margin: 20px 0;
  display: flex;
  justify-content: center;
}

.stats-content {
  background: linear-gradient(90deg, #7BB9FF 0%, #B6A4FF 100%);
  color: white;
  padding: 8px 24px;
  border-radius: 20px;
  font-size: 14px;
}

/* 留言列表 */
.messages-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 12px;
  margin-top: 20px;
}

.message-item {
  background: transparent;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.message-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  opacity: 0.15;
  transition: opacity 0.3s ease;
  z-index: 0;
}

.message-item:hover::before {
  opacity: 0.2;
}

/* 为每个卡片添加不同的渐变背景 */
.message-item:nth-child(6n+1)::before {
  background: linear-gradient(135deg, #FF9A9E, #FAD0C4);
}

.message-item:nth-child(6n+2)::before {
  background: linear-gradient(135deg, #A1C4FD, #C2E9FB);
}

.message-item:nth-child(6n+3)::before {
  background: linear-gradient(135deg, #D4FC79, #96E6A1);
}

.message-item:nth-child(6n+4)::before {
  background: linear-gradient(135deg, #FFC3A0, #FFAFBD);
}

.message-item:nth-child(6n+5)::before {
  background: linear-gradient(135deg, #81FBB8, #28C76F);
}

.message-item:nth-child(6n+6)::before {
  background: linear-gradient(135deg, #FF9A9E, #F6D365);
}

/* 统一所有内部元素的背景样式 */
.message-header, .message-content, .message-footer, .user-info, .action-btn {
  background: transparent !important;
  backdrop-filter: none !important;
}

.message-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid rgba(0, 0, 0, 0.1);
  position: relative;

}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 4px 12px 4px 4px;
  border-radius: 25px;
}

.avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid rgba(255, 255, 255, 0.9);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
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

.avatar-loading {
  position: relative;
}

.avatar-loading::after {
  content: '';
  position: absolute;
  top: -2px;
  left: -2px;
  right: -2px;
  bottom: -2px;
  border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.1);
  border-top-color: rgba(255, 255, 255, 0.8);
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border: none;
  border-radius: 20px;
  color: #666;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 14px;
  background: transparent;
}

.action-btn:hover {
  transform: translateY(-1px);
  color: #333;
}

.like-btn::before {
  content: '♡';
  font-size: 16px;
}

.like-btn.liked::before {
  content: '♥';
  color: #ff4757;
}

.delete-btn::before {
  content: '(×﹏×)';
  font-size: 14px;
}

.delete-btn:hover {
  color: #ff4757;
}

.system-info {
  display: flex;
  gap: 16px;
  align-items: center;
  opacity: 0.8;
  font-size: 14px;
}

.system-detail {
  display: flex;
  align-items: center;
  gap: 4px;
}

.browser-icon::before {
  content: '🌐';
}

.os-icon::before {
  content: '💻';
}

.message-actions {
  display: flex;
  gap: 16px;
  align-items: center;
}

.message-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  position: relative;
}

.message-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

/* 美化用户信息区域 */
.user-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.location-icon {
  color: #ff6b81;
}

/* Modal styles */
.modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-content {
  background: white;
  border-radius: 16px;
  width: 90%;
  max-width: 500px;
  overflow: hidden;
}

.modal-header {
  padding: 16px 20px;
  border-bottom: 1px solid #eee;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.modal-title {
  font-size: 16px;
  color: #333;
}

.close-btn {
  font-size: 20px;
  color: #999;
  cursor: pointer;
  transition: color 0.2s;
}

.close-btn:hover {
  color: #666;
}

.modal-body {
  padding: 20px;
}

.user-info-section {
  background: #f8f9fa;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  border: 2px solid white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.system-info {
  flex: 1;
  display: flex;
  gap: 8px;
}

.location-info, .browser-info {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #666;
  font-size: 14px;
}

.location-icon, .os-icon, .browser-icon {
  color: #999;
}

.form-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.input-group {
  position: relative;
}

.form-input {
  width: 100%;
  padding: 12px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.3s;
  background: #f8f9fa;
}

.form-input:focus {
  border-color: #7BB9FF;
  background: white;
  outline: none;
  box-shadow: 0 0 0 3px rgba(123, 185, 255, 0.1);
}

.form-textarea {
  width: 100%;
  padding: 12px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  resize: vertical;
  min-height: 120px;
  transition: all 0.3s;
  background: #f8f9fa;
}

.form-textarea:focus {
  border-color: #7BB9FF;
  background: white;
  outline: none;
  box-shadow: 0 0 0 3px rgba(123, 185, 255, 0.1);
}

.random-text {
  position: absolute;
  right: 8px;
  bottom: 8px;
}

.random-btn {
  background: none;
  border: none;
  color: #7BB9FF;
  font-size: 14px;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.random-btn:hover {
  background: rgba(123, 185, 255, 0.1);
}

.submit-btn {
  width: 100%;
  padding: 12px;
  background: linear-gradient(90deg, #7BB9FF 0%, #B6A4FF 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;

}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(123, 185, 255, 0.3);
}

.submit-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

/* Responsive Design */
@media (max-width: 768px) {
  .messages-grid {
    grid-template-columns: 1fr;
  }


  .modal-content {
    width: 95%;
    margin: 20px;
  }
}

/* 删除确认弹框样式 */
.confirm-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(5px);
  display: flex;
  align-items: center;
  justify-content: center;
  animation: fadeIn 0.2s ease;
}

.confirm-content {
  background: white;
  border-radius: 20px;
  padding: 24px;
  width: 90%;
  max-width: 320px;
  text-align: center;
  animation: scaleIn 0.2s ease;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.2);
}

.confirm-icon {
  font-size: 32px;
  margin-bottom: 16px;
  color: #ff6b81;
}

.confirm-title {
  font-size: 18px;
  color: #333;
  margin-bottom: 24px;
  font-weight: 500;
}

.confirm-buttons {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.confirm-btn, .cancel-btn {
  padding: 10px 24px;
  border-radius: 20px;
  border: none;
  font-size: 15px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.cancel-btn {
  background: #f1f1f1;
  color: #666;
}

.cancel-btn:hover {
  background: #e5e5e5;
}

.confirm-btn {
  background: #ff6b81;
  color: white;
}

.confirm-btn:hover {
  background: #ff5268;
  transform: translateY(-1px);
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes scaleIn {
  from {
    transform: scale(0.9);
    opacity: 0;
  }
  to {
    transform: scale(1);
    opacity: 1;
  }
}

/* 添加空状态样式 */
.empty-state {
  grid-column: 1 / -1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.2);
  margin: 20px 0;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 20px;
  animation: float 3s ease-in-out infinite;
}

.empty-state h3 {
  font-size: 24px;
  color: #333;
  margin-bottom: 12px;
  font-weight: 600;
}

.empty-state p {
  font-size: 16px;
  color: #666;
  max-width: 300px;
  line-height: 1.5;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-10px);
  }
}

/* 移动端适配空状态 */
@media screen and (max-width: 768px) {
  .empty-state {
    padding: 40px 20px;
    margin: 10px 0;
  }

  .empty-icon {
    font-size: 48px;
    margin-bottom: 16px;
  }

  .empty-state h3 {
    font-size: 20px;
    margin-bottom: 8px;
  }

  .empty-state p {
    font-size: 14px;
    padding: 0 20px;
  }
}
</style>

