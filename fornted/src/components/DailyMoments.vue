<template>
  <div class="tree-hole-container">
    <div class="tree-hole-list" v-if="moments.length > 0">
      <div v-for="(moment, index) in moments" :key="moment.id"
           class="tree-hole-content"
           :class="{ leftTreeHole: index % 2 === 0, rightTreeHole: index % 2 !== 0 }">
        <!-- 时间节点 -->
        <div class="time-node"></div>
        <!-- 头像 -->
        <img class="avatar-img"
             :src="moment.userAvatar || getDefaultAvatar()"
             @click="handleUserClick(moment.userId)" />
        <!-- 内容卡片 -->
        <div class="tree-hole-box">
          <div class="my-content">{{ moment.content }}</div>
          <div class="tree-hole-footer">
            <div>😃 {{ formatTime(moment.createTime) }}</div>
            <div class="tree-hole-actions">
              <span class="like-count" @click="handleLike(moment)">
                <i class="like-icon" :class="{'liked': moment.isLiked}">❤️</i>
                {{ moment.likeCount }}
              </span>
              <span v-if="canDelete(moment)"
                    @click="handleDelete(moment.id)"
                    class="tree-hole-delete">
                <svg viewBox="0 0 1024 1024" width="16" height="16">
                  <path d="M921.1392 155.392h-270.592v-48.2816c0-22.7328-18.432-41.1648-41.1648-41.1648H426.3424a41.1648 41.1648 0 0 0-41.1648 41.1648v48.2816H110.6432c-14.1312 0-25.6 11.4688-25.6 25.6s11.4688 25.6 25.6 25.6h810.496c14.1312 0 25.6-11.4688 25.6-25.6s-11.4688-25.6-25.6-25.6zM170.8032 260.0448v592.8448c0 50.8928 41.2672 92.16 92.16 92.16h500.6848c50.8928 0 92.16-41.2672 92.16-92.16V260.0448H170.8032z m249.1392 462.7968c0 14.1312-11.4688 25.6-25.6 25.6s-25.6-11.4688-25.6-25.6V443.0848c0-14.1312 11.4688-25.6 25.6-25.6s25.6 11.4688 25.6 25.6v279.7568z m243.1488 0c0 14.1312-11.4688 25.6-25.6 25.6s-25.6-11.4688-25.6-25.6V443.0848c0-14.1312 11.4688-25.6 25.6-25.6s25.6 11.4688 25.6 25.6v279.7568z" fill="#FF623E"></path>
                </svg>
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div v-else class="empty-state">
      还没有任何记录，快来记录生活的美好时刻吧~
    </div>

    <!-- 发布按钮 -->
    <div class="tree-hole-go">
      <i class="fa fa-paper-plane" @click="showPublishModal"></i>
    </div>

    <!-- 发布弹窗 -->
    <div class="modal" v-if="showModal" @click.self="closeModal">
      <div class="modal-content" :class="{ 'quote-modal': !loginUserStore.loginUser?.id || !isOwner }">
        <div class="modal-header">
          <h3>{{ !loginUserStore.loginUser?.id || !isOwner ? '今日寄语' : '记录美好时刻' }}</h3>
          <span class="close-btn" @click="closeModal">&times;</span>
        </div>
        <div class="modal-body">
          <!-- 非主人或未登录时显示正能量语句 -->
          <div v-if="!loginUserStore.loginUser?.id || !isOwner" class="quote-container">
            <div class="quote-text">{{ currentQuote }}</div>
          </div>
          <!-- 主人登录时显示发布表单 -->
          <div v-else class="input-container">
            <textarea
              v-model="newMoment.content"
              placeholder="写下此刻的心情..."
              maxlength="1024"
              rows="4"
              class="moment-textarea"
            ></textarea>
            <div class="modal-footer">
              <label class="visibility-toggle">
                <input
                  type="checkbox"
                  v-model="newMoment.isPublic"
                >
                <span>公开</span>
              </label>
              <button class="publish-btn"
                      @click="handlePublish"
                      :disabled="!newMoment.content.trim() || publishing">
                {{ publishing ? '发布中...' : '发布' }}
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
        <div class="confirm-title">确定要删除这条记录吗？</div>
        <div class="confirm-buttons">
          <button class="cancel-btn" @click="cancelDelete">取消</button>
          <button class="confirm-btn" @click="confirmDelete">删除</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, createVNode } from 'vue'
import { useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { message, Modal } from 'ant-design-vue'
import {
  HeartOutlined,
  HeartFilled,
  DeleteOutlined,
  PlusOutlined,
  ExclamationCircleOutlined
} from '@ant-design/icons-vue'
import {
  addWeiYanUsingPost,
  deleteWeiYanUsingPost,
  listWeiYanUsingGet,
  likeWeiYanUsingPost
} from '@/api/weiYanController'
import { formatTime } from '@/utils/dateUtils'
import { getDefaultAvatar } from '@/utils/userUtils'

interface WeiYan {
  id: number
  userId: number
  loveBoardId: number
  content: string
  createTime: string
  isPublic: number
  likeCount: number
  isLiked?: boolean
  userAvatar?: string
}

const router = useRouter()
const loginUserStore = useLoginUserStore()
const moments = ref<WeiYan[]>([])
const showModal = ref(false)
const publishing = ref(false)
const newMoment = ref({
  content: '',
  isPublic: true,
  loveBoardId: 0 // 将从父组件传入
})

// 从父组件接收属性
const props = defineProps<{
  loveBoardId: string
  isOwner: boolean
}>()

// 卡片背景色数组
const colors = ref([
  'var(--themeBackground)',
  'var(--pink)',
  'var(--purple)',
  'var(--cyan)',
  'var(--green)'
])

// 添加随机正能量语句数组
const positiveQuotes = [
  "每一个微笑都是对生活的感恩 ❤️",
  "爱是生命中最美的风景 🌈",
  "珍惜当下，感恩遇见 🌟",
  "愿你被这个世界温柔以待 🌸",
  "生活虽不完美，但依然值得期待 ✨",
  "带着希望前进，怀着爱继续 💫",
  "每个人都是自己人生的主角 🌠",
  "相信美好，美好就会不期而遇 🍀",
  "保持热爱，奔赴山海 ⭐",
  "愿你眼中有光，心中有爱 💖"
]

// 当前显示的正能量语句
const currentQuote = ref(positiveQuotes[Math.floor(Math.random() * positiveQuotes.length)])

const handleUserClick = (userId: number) => {
  if (userId) {
    router.push(`/user/${userId}`)
  }
}

const handleLike = async (moment: WeiYan) => {

  // 检查是否已经点赞过
  if (moment.isLiked) {
    message.warning('您已经点赞过啦~')
    return
  }

  try {
    const response = await likeWeiYanUsingPost({ id: moment.id })
    if (response.data.code === 0) {
      // 更新点赞状态和数量
      moment.likeCount = Number(moment.likeCount || 0) + 1
      moment.isLiked = true
    } else if (response.data.code === 40100) {
      message.warning('您已经点赞过啦~')
    } else {
      message.error('点赞失败：' + response.data.message)
    }
  } catch (error) {
    console.error('点赞失败:', error)
    message.error('点赞失败，请稍后再试')
  }
}

const canDelete = (moment: WeiYan): boolean => {
  return loginUserStore.loginUser?.id === moment.userId
}

const handleDelete = async (id: number) => {
  if (!loginUserStore.loginUser?.id) {
    message.warning('请先登录')
    return
  }

  // 使用自定义的确认弹框
  showDeleteConfirm.value = true
  momentToDelete.value = id
}

// 添加删除相关的状态
const showDeleteConfirm = ref(false)
const momentToDelete = ref<number | null>(null)

// 确认删除
const confirmDelete = async () => {
  if (!momentToDelete.value) return

  try {
    const response = await deleteWeiYanUsingPost({
      id: momentToDelete.value,
      loveBoardId: props.loveBoardId
    })
    if (response.data.code === 0) {
      message.success('删除成功')
      moments.value = moments.value.filter(item => item.id !== momentToDelete.value)
      showDeleteConfirm.value = false
    } else {
      message.error('删除失败：' + response.data.message)
    }
  } catch (error) {
    console.error('删除失败:', error)
    message.error('删除失败')
  }
}

// 取消删除
const cancelDelete = () => {
  showDeleteConfirm.value = false
  momentToDelete.value = null
}

const showPublishModal = () => {
  if (!loginUserStore.loginUser?.id || !props.isOwner) {
    // 如果用户未登录或不是主人，显示随机正能量语句
    currentQuote.value = positiveQuotes[Math.floor(Math.random() * positiveQuotes.length)]
    showModal.value = true
    return
  }
  // 用户已登录且是主人，显示发布界面
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
  newMoment.value = {
    content: '',
    isPublic: true,
    loveBoardId: props.loveBoardId
  }
}

const handlePublish = async () => {
  if (!loginUserStore.loginUser?.id) {
    message.warning('请先登录后再发布内容')
    return
  }

  if (!newMoment.value.content.trim()) {
    message.warning('请输入内容')
    return
  }

  publishing.value = true
  try {
    const response = await addWeiYanUsingPost({
      content: newMoment.value.content,
      isPublic: newMoment.value.isPublic ? 1 : 0,
      loveBoardId: props.loveBoardId,
      userId: String(loginUserStore.loginUser.id),
      type: 0  // 添加type字段，默认值为0
    })

    if (response.data.code === 0) {
      message.success('发布成功')
      await fetchMoments()
      closeModal()
    } else {
      message.error('发布失败：' + response.data.message)
    }
  } catch (error) {
    console.error('发布失败:', error)
    message.error('发布失败')
  } finally {
    publishing.value = false
  }
}

const fetchMoments = async () => {
  try {
    // 验证loveBoardId
    if (!props.loveBoardId) {
      return
    }

    const params = {
      loveBoardId: props.loveBoardId,
      userId: loginUserStore.loginUser?.id ? String(loginUserStore.loginUser.id) : undefined,
      current: 1,
      pageSize: 20
    }

    const response = await listWeiYanUsingGet(params)

    if (response.data.code === 0) {
      moments.value = (response.data.data?.records || []).map(moment => ({
        ...moment,
        likeCount: Number(moment.likeCount || 0)
      }))
      moments.value.sort((a: WeiYan, b: WeiYan) =>
        new Date(b.createTime).getTime() - new Date(a.createTime).getTime()
      )
    } else {
      message.error(response.data.message || '获取点点滴滴列表失败')
    }
  } catch (error: any) {
    console.error('获取点点滴滴列表失败:', error)
    // 显示更详细的错误信息
    if (error.response) {
      message.error(`获取失败: ${error.response.data?.message || '未知错误'}`)
    } else if (error.request) {
      message.error('网络请求失败，请检查网络连接')
    } else {
      message.error(`获取失败: ${error.message || '未知错误'}`)
    }
  }
}

onMounted(() => {
  fetchMoments()
})
</script>

<style scoped>
.tree-hole-container {
  width: 800px;
  margin: 0 auto;
  padding: 40px 20px 20px;  /* 增加顶部内边距 */
  position: relative;
}

/* 时间轴线 */
.tree-hole-list {
  position: relative;
  padding: 40px 0 20px;  /* 增加顶部内边距 */
}

.tree-hole-list::before {
  content: '';
  position: absolute;
  left: 50%;
  top: 0;
  bottom: 0;
  width: 3px;
  background: linear-gradient(180deg,
  rgba(110, 69, 225, 0.8),
  rgba(136, 211, 206, 0.8),
  rgba(255, 105, 180, 0.8),
  rgba(110, 69, 225, 0.8)
  );
  transform: translateX(-50%);
  box-shadow: 0 0 10px rgba(110, 69, 225, 0.3);
}

/* 时间轴顶部装饰 */
.tree-hole-list::after {
  content: '✨';
  position: absolute;
  left: 50%;
  top: -25px;  /* 调整位置 */
  transform: translateX(-50%);
  font-size: 24px;
  animation: starTwinkle 1.5s ease-in-out infinite;
}

/* 内容卡片容器 */
.tree-hole-content {
  display: flex;
  margin-bottom: 30px;
  position: relative;
}

/* 左侧内容 */
.leftTreeHole {
  justify-content: flex-end;
  padding-right: calc(50% + 30px);
}

/* 右侧内容 */
.rightTreeHole {
  justify-content: flex-start;
  padding-left: calc(50% + 30px);
}

/* 移除头像样式，改用彩色圆圈 */
.avatar-img {
  display: none;  /* 隐藏头像 */
}

/* 新增彩色圆圈样式 */
.time-node {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: linear-gradient(45deg, #6e45e1, #ff69b4);
  box-shadow: 0 0 10px rgba(110, 69, 225, 0.4);
  animation: nodeColorChange 4s linear infinite;
  z-index: 2;
}

/* 每个圆圈使用不同的渐变色 */
.tree-hole-content:nth-child(3n) .time-node {
  background: linear-gradient(45deg, #00bcd4, #4caf50);
}

.tree-hole-content:nth-child(3n+1) .time-node {
  background: linear-gradient(45deg, #ff69b4, #9c27b0);
}

.tree-hole-content:nth-child(3n+2) .time-node {
  background: linear-gradient(45deg, #ff9800, #f44336);
}

/* 修改动画效果 */
@keyframes nodeColorChange {
  0% {
    transform: translateX(-50%) scale(1);
    filter: brightness(1) hue-rotate(0deg);
  }
  50% {
    transform: translateX(-50%) scale(1.2);
    filter: brightness(1.2) hue-rotate(180deg);
  }
  100% {
    transform: translateX(-50%) scale(1);
    filter: brightness(1) hue-rotate(360deg);
  }
}

/* 内容卡片 */
.tree-hole-box {
  max-width: 360px;
  padding: 12px 16px;
  border-radius: 8px;
  position: relative;
  background-color: #e3f2fd;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

/* 左侧卡片箭头 */
.leftTreeHole .tree-hole-box::after {
  content: '';
  position: absolute;
  right: -8px;
  top: 15px;
  border-style: solid;
  border-width: 8px 0 8px 8px;
  border-color: transparent transparent transparent #e3f2fd;
}

/* 右侧卡片箭头 */
.rightTreeHole .tree-hole-box::after {
  content: '';
  position: absolute;
  left: -8px;
  top: 15px;
  border-style: solid;
  border-width: 8px 8px 8px 0;
  border-color: transparent #e3f2fd transparent transparent;
}

/* 内容文本 */
.my-content {
  margin-bottom: 10px;
  line-height: 1.5;
  word-break: break-all;
  white-space: pre-wrap;
}

/* 底部信息栏 */
.tree-hole-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 8px;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
  font-size: 14px;
  color: #666;
}

/* 操作按钮 */
.tree-hole-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.like-count {
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
}

.like-icon {
  opacity: 0.6;
  transition: opacity 0.2s;
}

.like-icon.liked {
  opacity: 1;
  color: #ff4757;
}

.tree-hole-delete {
  cursor: pointer;
  opacity: 0.6;
  transition: opacity 0.2s;
}

.tree-hole-delete:hover {
  opacity: 1;
}

/* 发布按钮 */
.tree-hole-go {
  position: relative;
  margin: 20px auto;
  text-align: center;
  color: var(--themeBackground);
  font-weight: 700;
  font-size: 25px;
}

.tree-hole-go i {
  cursor: pointer;
  transition: transform 0.3s ease;
}

.tree-hole-go i:hover {
  transform: scale(1.1);
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 40px 0;
  color: #666;
}

/* 发布弹窗 */
.modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: modalFadeIn 0.3s ease;
}

.modal-content {
  background: #fff;
  border-radius: 24px;
  width: 90%;
  max-width: 500px;
  padding: 24px;
  box-shadow: 0 10px 30px rgba(110, 69, 225, 0.2);
  transform-origin: top;
  animation: modalSlideIn 0.3s ease;
  border: 2px solid rgba(110, 69, 225, 0.1);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 2px dashed rgba(110, 69, 225, 0.1);
}

.modal-header h3 {
  margin: 0;
  font-size: 20px;
  color: #6e45e1;
  display: flex;
  align-items: center;
  gap: 8px;
}

.modal-header h3::before {
  content: '✨';
  font-size: 24px;
}

.close-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: rgba(110, 69, 225, 0.1);
  color: #6e45e1;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 20px;
}

.close-btn:hover {
  background: rgba(110, 69, 225, 0.2);
  transform: rotate(90deg);
}

.moment-textarea {
  width: 100%;
  min-height: 120px;
  padding: 16px;
  border: 2px solid rgba(110, 69, 225, 0.1);
  border-radius: 16px;
  resize: vertical;
  margin-bottom: 16px;
  font-size: 15px;
  transition: all 0.3s ease;
  background: rgba(110, 69, 225, 0.02);
}

.moment-textarea:focus {
  outline: none;
  border-color: #6e45e1;
  background: white;
  box-shadow: 0 0 0 4px rgba(110, 69, 225, 0.1);
}

.moment-textarea::placeholder {
  color: #999;
}

.modal-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 20px;
}

.visibility-toggle {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #666;
  cursor: pointer;
  user-select: none;
}

.visibility-toggle input[type="checkbox"] {
  width: 40px;
  height: 20px;
  position: relative;
  appearance: none;
  background: #eee;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.visibility-toggle input[type="checkbox"]:checked {
  background: #6e45e1;
}

.visibility-toggle input[type="checkbox"]::before {
  content: '';
  position: absolute;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: white;
  top: 2px;
  left: 2px;
  transition: all 0.3s ease;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.visibility-toggle input[type="checkbox"]:checked::before {
  left: 22px;
}

.publish-btn {
  background: linear-gradient(45deg, #6e45e1, #ff69b4);
  color: white;
  border: none;
  padding: 10px 28px;
  border-radius: 20px;
  cursor: pointer;
  font-size: 15px;
  font-weight: 600;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(110, 69, 225, 0.2);
}

.publish-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(110, 69, 225, 0.3);
}

.publish-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
  box-shadow: none;
}

@keyframes modalFadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes modalSlideIn {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 发布按钮 */
.tree-hole-go {
  position: relative;
  margin: 20px auto;
  text-align: center;
}

.tree-hole-go i {
  width: 50px;
  height: 50px;
  background: linear-gradient(45deg, #6e45e1, #ff69b4);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(110, 69, 225, 0.2);
  margin: 0 auto;
}

.tree-hole-go i:hover {
  transform: translateY(-3px) rotate(15deg);
  box-shadow: 0 6px 20px rgba(110, 69, 225, 0.3);
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .tree-hole-container {
    width: 100%;
    padding: 30px 10px 10px;
    box-sizing: border-box;
  }

  .tree-hole-list {
    padding: 30px 0 10px;
    width: 100%;
  }

  /* 时间轴线调整到左侧 */
  .tree-hole-list::before {
    left: 20px;  /* 时间轴线位置调整到左侧 */
    transform: none;
  }

  /* 顶部装饰星星调整到左侧 */
  .tree-hole-list::after {
    left: 20px;
    transform: none;
  }

  /* 时间节点圆点调整到左侧 */
  .time-node {
    left: 20px;
    transform: none;
  }

  .leftTreeHole,
  .rightTreeHole {
    padding-left: 40px;
    padding-right: 10px;
    justify-content: flex-start;
    width: 100%;
    box-sizing: border-box;
  }

  .tree-hole-box {
    width: calc(100% - 30px);
    max-width: none;
    box-sizing: border-box;
  }

  /* 所有卡片箭头都朝右 */
  .leftTreeHole .tree-hole-box::after,
  .rightTreeHole .tree-hole-box::after {
    left: -8px;
    right: auto;
    border-width: 8px 8px 8px 0;
    border-color: transparent #e3f2fd transparent transparent;
  }
}

/* 超小屏幕适配 */
@media screen and (max-width: 480px) {
  .tree-hole-container {
    padding: 5px;
  }

  .leftTreeHole,
  .rightTreeHole {
    padding-left: 35px;
    padding-right: 5px;
  }

  .tree-hole-box {
    padding: 10px;
    width: calc(100% - 25px);
  }

  .modal-content {
    width: 95%;
    padding: 16px;
  }
}

/* 添加正能量语句样式 */
.quote-modal {
  max-width: 400px !important;
}

.quote-container {
  padding: 30px 20px;
  text-align: center;
}

.quote-text {
  font-size: 20px;
  color: #6e45e1;
  line-height: 1.6;
  font-weight: 500;
  text-shadow: 0 2px 4px rgba(110, 69, 225, 0.1);
  animation: quoteFloat 2s ease-in-out infinite;
}

@keyframes quoteFloat {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-5px);
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
  z-index: 1000;
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
</style>
