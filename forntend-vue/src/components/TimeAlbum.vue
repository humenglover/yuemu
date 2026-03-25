<template>
  <div class="time-album-container">
    <!-- 顶部操作栏 -->
    <div class="album-header">
      <h2 class="album-title">珍藏回忆</h2>
      <button v-if="props.isOwner" class="create-album-btn" @click="showCreateModal = true">
        <span class="btn-icon">✨</span>
        <span class="btn-text">创建相册</span>
      </button>
    </div>

    <!-- 相册列表 -->
    <div class="album-grid">
      <!-- 空状态展示 -->
      <div v-if="albums.length === 0" class="empty-state">
        <div class="empty-icon">🖼️</div>
        <h3>还没有时光相册</h3>
        <p v-if="props.isOwner">点击右上角的按钮创建你的第一个时光相册吧！</p>
        <p v-else>主人还没有创建时光相册哦~</p>
      </div>
      <!-- 相册列表 -->
      <div v-else v-for="album in albums" :key="album.id" class="album-card" :data-is-owner="props.isOwner" @click="handleAlbumClick(album)">
        <div class="album-cover">
          <div class="image-skeleton" v-if="!album.imageLoaded"></div>
          <img :src="album.coverUrl || '/default-album-cover.png'" :alt="album.albumName" @load="handleImageLoad(album)" :class="{ 'image-loaded': album.imageLoaded }">
          <div v-if="album.isPublic === 0" class="private-badge">
            <span class="private-icon">🔒</span>
          </div>
          <button v-if="props.isOwner"
                  class="delete-btn"
                  @click.stop="showDeleteConfirm(album)">
            <span class="delete-icon">✖</span>
          </button>
        </div>
        <div class="album-info">
          <h3>
            {{ album.albumName }}
            <div class="album-actions" v-if="props.isOwner">
              <button class="edit-btn" @click.stop="handleEditClick(album)">
                <span class="edit-icon">✏️</span>
              </button>
              <button class="password-manage" @click.stop="handlePasswordManageClick(album)">
                <span>{{ album.isPublic === 1 ? '设置密码' : '管理密码' }}</span>
                <span>{{ album.isPublic === 1 ? '🔒' : '🔑' }}</span>
              </button>
            </div>
          </h3>
          <p>{{ album.description }}</p>
          <div class="album-footer">
            <span class="album-date">{{ formatDate(album.createTime) }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 使用 Ant Design Vue Modal 组件替换创建相册模态框 -->
    <a-modal
      v-model:open="showCreateModal"
      title="✨ 创建新相册"
      :footer="null"
      :closable="true"
      @cancel="showCreateModal = false"
      width="520px"
      centered>
      <form class="modal-form" @submit.prevent="handleCreateAlbum">
        <div class="form-item">
          <label>相册名称</label>
          <input type="text" v-model="newAlbum.albumName" placeholder="给相册起个名字吧" required>
        </div>
        <div class="form-item">
          <label>相册描述</label>
          <textarea v-model="newAlbum.description" rows="3" placeholder="写下对这个相册的描述"></textarea>
        </div>
        <div class="form-item">
          <label>访问设置</label>
          <div class="switch-wrapper">
            <input
              type="checkbox"
              :checked="newAlbum.isPublic === 1"
              @change="(e) => newAlbum.isPublic = e.target.checked ? 1 : 0"
            >
            <span class="switch-label">{{ newAlbum.isPublic === 1 ? '公开' : '私密' }}</span>
          </div>
        </div>
        <div class="form-item" v-if="newAlbum.isPublic === 0">
          <label>访问密码</label>
          <input
            type="password"
            v-model="newAlbum.password"
            placeholder="设置访问密码"
            :required="newAlbum.isPublic === 0"
          >
        </div>
        <div class="form-item">
          <label>封面图片</label>
          <div class="upload-wrapper" @click="handleUploadCover">
            <div class="image-preview">
              <img v-if="newAlbum.coverUrl" :src="newAlbum.coverUrl" alt="封面预览">
              <div v-else-if="isUploading" class="upload-loading">
                <div class="loading-spinner"></div>
                <span>上传中...</span>
              </div>
              <div v-else class="upload-placeholder">
                <span class="upload-icon">🎞️</span>
                <div>点击上传封面</div>
              </div>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn-cancel" @click="showCreateModal = false">取消</button>
          <button type="submit" class="btn-confirm">创建相册</button>
        </div>
      </form>
    </a-modal>

    <!-- 使用 Ant Design Vue Modal 组件替换删除确认对话框 -->
    <a-modal
      v-model:open="showDeleteModal"
      title="确认删除"
      :footer="null"
      :closable="true"
      @cancel="showDeleteModal = false"
      width="320px"
      centered>
      <div class="confirm-content">
        <div class="confirm-icon">
          <span>(｡•́︿•̀｡)</span>
        </div>
        <div class="confirm-title">确定要删除相册 "{{selectedAlbum?.albumName}}" 吗？</div>
        <div class="confirm-buttons">
          <button class="cancel-btn" @click="showDeleteModal = false">取消</button>
          <button class="confirm-btn" @click="confirmDelete">删除</button>
        </div>
      </div>
    </a-modal>

    <!-- 使用 Ant Design Vue Modal 组件替换密码验证模态框 -->
    <a-modal
      v-model:open="showPasswordModal"
      title="🔒 输入访问密码"
      :footer="null"
      :closable="true"
      @cancel="showPasswordModal = false"
      width="400px"
      centered>
      <form class="modal-form" @submit.prevent="handlePasswordSubmit">
        <div class="form-item">
          <label>访问密码</label>
          <input
            type="password"
            v-model="passwordInput"
            placeholder="请输入相册访问密码"
            required
            ref="passwordInputRef"
          >
        </div>
        <div class="modal-footer">
          <button type="button" class="btn-cancel" @click="showPasswordModal = false">取消</button>
          <button type="submit" class="btn-confirm">确认</button>
        </div>
      </form>
    </a-modal>

    <!-- 使用 Ant Design Vue Modal 组件替换密码管理模态框 -->
    <a-modal
      v-model:open="showPasswordManageModal"
      title="🔒 密码管理"
      :footer="null"
      :closable="true"
      @cancel="showPasswordManageModal = false"
      width="400px"
      centered>
      <div class="modal-body">
        <div v-if="selectedAlbum?.isPublic === 1">
          <h3>设置访问密码</h3>
          <p>设置密码后，相册将变为私密相册</p>
          <div class="form-item">
            <label>新密码</label>
            <input
              type="password"
              v-model="passwordForm.newPassword"
              placeholder="请输入新密码"
              required
            >
          </div>
        </div>
        <div v-else>
          <div class="password-actions">
            <button class="action-btn" @click="currentAction = 'update'">
              <span class="action-icon">🔄</span>
              修改密码
            </button>
            <button class="action-btn" @click="currentAction = 'remove'">
              <span class="action-icon">🗑️</span>
              取消密码
            </button>
          </div>

          <div v-if="currentAction === 'update'" class="form-item">
            <label>原密码</label>
            <input
              type="password"
              v-model="passwordForm.oldPassword"
              placeholder="请输入原密码"
              required
            >
            <label>新密码</label>
            <input
              type="password"
              v-model="passwordForm.newPassword"
              placeholder="请输入新密码"
              required
            >
          </div>

          <div v-if="currentAction === 'remove'" class="form-item">
            <label>原密码</label>
            <input
              type="password"
              v-model="passwordForm.oldPassword"
              placeholder="请输入原密码"
              required
            >
            <p class="warning-text">⚠️ 取消密码后，相册将变为公开相册</p>
          </div>
        </div>
      </div>
      <div class="modal-footer">
        <button class="btn-cancel" @click="showPasswordManageModal = false">取消</button>
        <button class="btn-confirm" @click="handlePasswordManage">确认</button>
      </div>
    </a-modal>

    <!-- 使用 Ant Design Vue Modal 组件替换编辑相册模态框 -->
    <a-modal
      v-model:open="showEditModal"
      title="✏️ 编辑相册"
      :footer="null"
      :closable="true"
      @cancel="showEditModal = false"
      width="500px"
      centered>
      <form class="modal-form" @submit.prevent="handleEditAlbum">
        <div class="form-item">
          <label>相册名称</label>
          <input type="text" v-model="editAlbum.albumName" placeholder="给相册起个名字吧" required>
        </div>
        <div class="form-item">
          <label>相册描述</label>
          <textarea v-model="editAlbum.description" rows="3" placeholder="写下对这个相册的描述"></textarea>
        </div>
        <div class="form-item">
          <label>封面图片</label>
          <div class="upload-wrapper" @click="handleEditCoverUpload">
            <div class="image-preview">
              <img v-if="editAlbum.coverUrl" :src="editAlbum.coverUrl" alt="封面预览">
              <div v-else-if="isUploading" class="upload-loading">
                <div class="loading-spinner"></div>
                <span>上传中...</span>
              </div>
              <div v-else class="upload-placeholder">
                <span class="upload-icon">🎞️</span>
                <div>点击更换封面</div>
              </div>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn-cancel" @click="showEditModal = false">取消</button>
          <button type="submit" class="btn-confirm">保存修改</button>
        </div>
      </form>
    </a-modal>

    <!-- 隐藏的文件输入 -->
    <input
      type="file"
      ref="fileInput"
      style="display: none"
      accept="image/*"
      @change="handleFileChange"
    >
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, defineProps, nextTick, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import dayjs from 'dayjs'
import {
  addTimeAlbumUsingPost,
  listTimeAlbumUsingGet,
  deleteTimeAlbumUsingPost,
  getTimeAlbumByIdUsingGet,
  setAlbumPasswordUsingPost,
  updateAlbumPasswordUsingPost,
  removeAlbumPasswordUsingPost,
  updateTimeAlbumUsingPost
} from '@/api/timeAlbumController'
import { uploadPostImageUsingPost } from '@/api/pictureController'
import { message } from 'ant-design-vue'

interface TimeAlbum {
  id: number;
  albumName: string;
  description?: string;
  coverUrl?: string;
  createTime: string;
  isPublic: number;
  imageLoaded?: boolean;
}

const router = useRouter()
const route = useRoute()
const props = defineProps<{
  loveBoardId: string,
  isOwner?: boolean
}>()
const albums = ref<TimeAlbum[]>([])
const showCreateModal = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)
const showDeleteModal = ref(false)
const selectedAlbum = ref<TimeAlbum | null>(null)
const showPasswordModal = ref(false)
const passwordInput = ref('')
const passwordInputRef = ref<HTMLInputElement | null>(null)
const selectedPrivateAlbum = ref<TimeAlbum | null>(null)
const showPasswordManageModal = ref(false)
const passwordForm = ref({
  oldPassword: '',
  newPassword: ''
})
const currentAction = ref('')
const showEditModal = ref(false)
const editAlbum = ref<TimeAlbum>({
  id: 0,
  albumName: '',
  description: '',
  coverUrl: '',
  isPublic: 1,
  password: ''
})

const newAlbum = ref({
  albumName: '',
  description: '',
  coverUrl: '',
  isPublic: 1,  // 默认公开
  password: ''  // 私密相册密码
})

// 添加上传状态变量
const isUploading = ref(false)

// 格式化日期
const formatDate = (date: string) => {
  return dayjs(date).format('YYYY-MM-DD')
}

// 获取相册列表
const fetchAlbums = async () => {
  try {
    const res = await listTimeAlbumUsingGet({
      current: 1,
      pageSize: 20,
      loveBoardId: props.loveBoardId
    })
    if (res.data.code === 0) {
      // 保存当前相册的加载状态
      const loadedStates = new Map(
        albums.value.map(album => [album.id, album.imageLoaded])
      )
      // 为新相册设置加载状态，如果是已存在的相册则保留原来的状态
      albums.value = res.data.data.records.map(album => ({
        ...album,
        imageLoaded: loadedStates.get(album.id) || false
      }))
    }
  } catch (error) {
    console.error('获取相册列表失败:', error)
  }
}

// 处理相册点击
const handleAlbumClick = (album: TimeAlbum) => {
  if (album.isPublic === 0) {
    // 如果是私密相册，显示密码输入框
    selectedPrivateAlbum.value = album
    showPasswordModal.value = true
    // 等待DOM更新后聚焦密码输入框
    nextTick(() => {
      passwordInputRef.value?.focus()
    })
  } else {
    // 公开相册直接跳转，添加isOwner参数
    router.push(`/time-album/${album.id}?isOwner=${props.isOwner}`)
  }
}

// 显示删除确认框
const showDeleteConfirm = (album: TimeAlbum) => {
  selectedAlbum.value = album
  showDeleteModal.value = true
}

// 确认删除
const confirmDelete = async () => {
  if (!selectedAlbum.value) return

  try {
    const res = await deleteTimeAlbumUsingPost({
      id: selectedAlbum.value.id,
      loveBoardId: props.loveBoardId
    })
    if (res.data.code === 0) {
      albums.value = albums.value.filter(item => item.id !== selectedAlbum.value?.id)
      showMessage('删除成功', 'success')
    }
  } catch (error) {
    console.error('删除相册失败:', error)
    showMessage('删除相册失败', 'error')
  }
  showDeleteModal.value = false
  selectedAlbum.value = null
}

// 消息提示
const showMessage = (text: string, type: 'success' | 'error' = 'error') => {
  message[type](text)
}

// 修改文件上传处理函数
const handleFileChange = async (event: Event) => {
  const target = event.target as HTMLInputElement
  if (target.files && target.files[0]) {
    const file = target.files[0]
    try {
      isUploading.value = true
      const res = await uploadPostImageUsingPost({}, {}, file)
      if (res.data.code === 0) {
        // 根据当前打开的模态框决定更新哪个对象的封面
        if (showEditModal.value) {
          editAlbum.value.coverUrl = res.data.data.url
        } else {
          newAlbum.value.coverUrl = res.data.data.url
        }
      }
    } catch (error: any) {
      console.error('上传图片失败:', error)
      showMessage(error?.response?.data?.message || '上传图片失败')
    } finally {
      isUploading.value = false
    }
    // 清空 input 值，允许重复选择同一文件
    if (fileInput.value) {
      fileInput.value.value = ''
    }
  }
}

// 处理上传封面
const handleUploadCover = () => {
  fileInput.value?.click()
}

// 处理创建相册
const handleCreateAlbum = async () => {
  try {
    const res = await addTimeAlbumUsingPost(
      { loveBoardId: props.loveBoardId },
      newAlbum.value
    )
    if (res.data.code === 0) {
      showCreateModal.value = false
      newAlbum.value = {
        albumName: '',
        description: '',
        coverUrl: '',
        isPublic: 1,
        password: ''
      }
      await fetchAlbums()
    }
  } catch (error) {
    console.error('创建相册失败:', error)
  }
}

// 处理密码提交
const handlePasswordSubmit = async () => {
  if (!selectedPrivateAlbum.value) return

  try {
    // 验证密码
    const res = await getTimeAlbumByIdUsingGet({
      id: selectedPrivateAlbum.value.id,
      password: passwordInput.value
    })

    if (res.data.code === 0) {
      // 密码正确，将密码存储在 sessionStorage 中
      sessionStorage.setItem(`album_${selectedPrivateAlbum.value.id}_password`, passwordInput.value)

      // 跳转到相册详情，不在 URL 中包含密码
      router.push(`/time-album/${selectedPrivateAlbum.value.id}?isOwner=${props.isOwner}`)
      showPasswordModal.value = false
      passwordInput.value = ''
      selectedPrivateAlbum.value = null
    } else {
      message.error('密码错误')
      // 清空密码输入
      passwordInput.value = ''
      // 聚焦密码输入框
      nextTick(() => {
        passwordInputRef.value?.focus()
      })
    }
  } catch (error: any) {
    console.error('验证密码失败:', error)
    message.error(error?.response?.data?.message || '验证失败')
  }
}

// 处理图片加载完成
const handleImageLoad = (album: TimeAlbum) => {
  const targetAlbum = albums.value.find(a => a.id === album.id)
  if (targetAlbum) {
    targetAlbum.imageLoaded = true
  }
}

// 处理密码管理按钮点击
const handlePasswordManageClick = (album: TimeAlbum) => {
  selectedAlbum.value = album
  showPasswordManageModal.value = true
  currentAction.value = album.isPublic === 1 ? 'set' : ''
  passwordForm.value = { oldPassword: '', newPassword: '' }
}

// 处理密码管理
const handlePasswordManage = async () => {
  if (!selectedAlbum.value) return

  try {
    let res
    if (selectedAlbum.value.isPublic === 1) {
      // 设置密码
      res = await setAlbumPasswordUsingPost({
        albumId: selectedAlbum.value.id,
        newPassword: passwordForm.value.newPassword
      })
    } else if (currentAction.value === 'update') {
      // 更新密码
      res = await updateAlbumPasswordUsingPost({
        albumId: selectedAlbum.value.id,
        oldPassword: passwordForm.value.oldPassword,
        newPassword: passwordForm.value.newPassword
      })
    } else if (currentAction.value === 'remove') {
      // 取消密码
      res = await removeAlbumPasswordUsingPost({
        albumId: selectedAlbum.value.id,
        oldPassword: passwordForm.value.oldPassword
      })
    }

    if (res?.data.code === 0) {
      message.success('操作成功')
      // 刷新相册列表
      await fetchAlbums()
      showPasswordManageModal.value = false
      passwordForm.value = { oldPassword: '', newPassword: '' }
      selectedAlbum.value = null
      currentAction.value = ''
    } else {
      message.error(res?.data?.message || '操作失败')
    }
  } catch (error: any) {
    console.error('密码管理失败:', error)
    message.error(error?.response?.data?.message || '操作失败')
  }
}

// 处理编辑按钮点击
const handleEditClick = (album: TimeAlbum) => {
  editAlbum.value = { ...album }
  showEditModal.value = true
}

// 处理编辑封面上传
const handleEditCoverUpload = () => {
  fileInput.value?.click()
}

// 处理编辑相册提交
const handleEditAlbum = async () => {
  try {
    const res = await updateTimeAlbumUsingPost(
      { loveBoardId: props.loveBoardId },
      {
        id: editAlbum.value.id,
        albumName: editAlbum.value.albumName,
        description: editAlbum.value.description,
        coverUrl: editAlbum.value.coverUrl,
        isPublic: editAlbum.value.isPublic,
        password: editAlbum.value.password
      }
    )
    if (res.data.code === 0) {
      showMessage('更新成功', 'success')
      showEditModal.value = false
      await fetchAlbums()
    }
  } catch (error: any) {
    console.error('更新相册失败:', error)
    showMessage(error?.response?.data?.message || '更新失败')
  }
}

// 监听loveBoardId的变化
watch(() => props.loveBoardId, (newVal) => {
  if (newVal) {
    fetchAlbums()
  }
}, { immediate: true })

onMounted(() => {
  if (props.loveBoardId) {
    fetchAlbums()
  }
})

// 暴露方法给父组件
defineExpose({
  showCreateModal,
  fetchAlbums
})
</script>

<style scoped>
.time-album-container {
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  padding: 10px;
}

.album-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 0 10px;
  flex-wrap: wrap;
  gap: 16px;
}

.album-title {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  flex: 1;
}

.album-title::before {
  content: "🖼️";
  font-size: 28px;
}

.album-title::after {
  font-size: 28px;
}

.album-title {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  flex: 1;
}

.album-title::before {
  content: "🖼️";
  font-size: 28px;
}

.album-title::after {
  font-size: 28px;
}

.create-album-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  border-radius: 20px;
  background: #fff;
  border: 2px solid var(--themeBackground);
  color: var(--themeBackground);
  cursor: pointer;
  transition: all 0.3s ease;
  font-weight: 500;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  white-space: nowrap;
}

.create-album-btn:hover {
  transform: translateY(-2px);
  background: var(--themeBackground);
  color: white;
  box-shadow: 0 4px 15px rgba(110, 69, 226, 0.2);
}

.btn-icon {
  font-size: 20px;
}

.btn-text {
  font-size: 15px;
  letter-spacing: 0.5px;
  color: currentColor;
}

.album-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
  padding: 10px;
}

.album-card {
  background: var(--header-background);
  color: var(--text-primary);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  height: 100%;
  display: flex;
  flex-direction: column;
  cursor: pointer;
  position: relative;
  transform: translateY(0);
}

.album-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 16px 32px rgba(0, 0, 0, 0.12);
}

.album-card:hover .album-cover img.image-loaded {
  transform: scale(1.08);
}

.album-cover {
  width: 100%;
  padding-top: 75%; /* 4:3 aspect ratio */
  position: relative;
  overflow: hidden;
  background: #f8f9fa;
  border-radius: 8px 8px 0 0;
}

/* 优化骨架屏样式 */
.image-skeleton {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(
    110deg,
    #eff1f3 30%,
    #f5f7f9 40%,
    #f5f7f9 50%,
    #eff1f3 60%
  );
  background-size: 200% 100%;
  animation: wave 2s linear infinite;
  border-radius: 8px 8px 0 0;
}

@keyframes wave {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

.album-cover img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: all 0.6s cubic-bezier(0.4, 0, 0.2, 1);
  opacity: 0;
  transform: scale(1.05);
  filter: blur(5px);
}

.album-cover img.image-loaded {
  opacity: 1;
  transform: scale(1);
  filter: blur(0);
}

.album-info {
  padding: 16px;
  flex: 1;
  display: flex;
  flex-direction: column;
  position: relative;
}

.album-info h3 {
  margin: 0 0 8px;
  font-size: 16px;
  color: #333;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.album-info h3 .password-manage {
  font-size: 14px;
  background: none;
  border: none;
  color: var(--themeBackground);
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 4px;
}

.album-info h3 .password-manage:hover {
  background: rgba(110, 69, 226, 0.1);
}

.album-info p {
  margin: 0;
  font-size: 14px;
  color: #666;
  flex: 1;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.album-footer {
  margin-top: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.album-date {
  font-size: 13px;
  color: #999;
}

.delete-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  background: rgba(255, 255, 255, 0.9);
  border: none;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  opacity: 0;
  transform: translateY(-10px);
  transition: all 0.3s ease;
  backdrop-filter: blur(4px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  z-index: 10;
}

/* 为所有者显示删除按钮 */
.album-card[data-is-owner="true"]:hover .delete-btn {
  opacity: 1;
  transform: translateY(0);
}

.delete-btn:hover {
  background: #ff4d4f;
  color: white;
}

.delete-icon {
  font-size: 16px;
}

/* 删除确认对话框样式 */
/*
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
  background: var(--header-background);
  color: var(--text-primary);
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
*/

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

/* Modal styles */
/*
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
  z-index: 100;
}

.modal-content {
  background: var(--header-background);
  color: var(--text-primary);
  border-radius: 16px;
  padding: 24px;
  width: 90%;
  max-width: 500px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.modal-header h2 {
  font-size: 20px;
  color: #333;
  margin: 0;
  font-weight: 600;
}

.modal-close {
  background: none;
  border: none;
  font-size: 24px;
  color: #999;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: all 0.3s ease;
}

.modal-close:hover {
  background: #f5f5f5;
  color: #666;
}
*/

.form-item {
  margin-bottom: 20px;
}

.form-item label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.form-item input,
.form-item textarea {
  width: 100%;
  padding: 12px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.3s ease;
}

.form-item input:focus,
.form-item textarea:focus {
  border-color: var(--themeBackground);
  box-shadow: 0 0 0 3px rgba(110, 69, 226, 0.1);
  outline: none;
}

.upload-wrapper {
  border: 2px dashed #e0e0e0;
  border-radius: 12px;
  padding: 16px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
}

.upload-wrapper:hover {
  border-color: var(--themeBackground);
  background: rgba(110, 69, 226, 0.02);
}

.image-preview {
  position: relative;
  width: 100%;
  padding-top: 56.25%; /* 16:9 aspect ratio */
  background: #f8f9fa;
  border-radius: 8px;
  overflow: hidden;
  transition: all 0.3s ease;
}

.image-preview img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.upload-placeholder {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #f8f9fa;
  color: #666;
  gap: 8px;
  transition: all 0.3s ease;
}

.upload-wrapper:hover .upload-placeholder {
  background: #f0f2f5;
  color: var(--themeBackground);
}

.upload-icon {
  font-size: 32px;
  margin-bottom: 8px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.btn-cancel,
.btn-confirm {
  padding: 10px 24px;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
  color: #3a3a3c!important;
}

.btn-cancel {
  background: #f5f5f5;
  border: none;
  color: #666;
}

.btn-confirm {
  background: var(--themeBackground);
  border: none;
  color: white;
}

.btn-cancel:hover {
  background: #ebebeb;
}

.btn-confirm:hover {
  opacity: 0.9;
}

/* 创建相册模态框样式 */
/*
.create-modal {
  background: linear-gradient(to bottom right, #ffffff, #f8f9fa);
  border-radius: 20px;
  padding: 32px;
  width: 90%;
  max-width: 520px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.15);
  animation: slideUp 0.3s ease;
  border: 1px solid rgba(255, 255, 255, 0.8);
}

.create-modal .modal-header {
  border: none;
  padding: 0;
  margin-bottom: 32px;
  position: relative;
}

.create-modal .modal-header h2 {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  display: flex;
  align-items: center;
  gap: 12px;
}

.create-modal .modal-close {
  position: absolute;
  top: -10px;
  right: -10px;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: none;
  background: #f5f5f5;
  color: #666;
  font-size: 20px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.create-modal .modal-close:hover {
  background: #e0e0e0;
  transform: rotate(90deg);
}

.create-modal .form-item {
  margin-bottom: 24px;
  position: relative;
}

.create-modal .form-item label {
  display: block;
  margin-bottom: 10px;
  font-size: 15px;
  font-weight: 500;
  color: #333;
  transition: color 0.3s ease;
}

.create-modal .form-item:hover label {
  color: var(--themeBackground);
}

.create-modal .form-item input[type="text"],
.create-modal .form-item input[type="password"],
.create-modal .form-item textarea {
  width: 100%;
  padding: 12px 16px;
  border: 2px solid #e0e0e0;
  border-radius: 12px;
  font-size: 15px;
  transition: all 0.3s ease;
  background: #f8f9fa;
}

.create-modal .form-item input:focus,
.create-modal .form-item textarea:focus {
  border-color: var(--themeBackground);
  background: var(--header-background);
  color: var(--text-primary);
  box-shadow: 0 0 0 4px rgba(110, 69, 226, 0.1);
  outline: none;
}

.create-modal .modal-footer {
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  justify-content: flex-end;
  gap: 16px;
}

.create-modal .btn-cancel,
.create-modal .btn-confirm {
  padding: 12px 24px;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.create-modal .btn-confirm {
  background: var(--themeBackground);
  color: white;
  border: none;
}

.create-modal .btn-confirm:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(110, 69, 226, 0.2);
}
*/

/* 访问设置开关样式 */
.switch-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: #f8f9fa;
  border-radius: 12px;
  border: 2px solid #e0e0e0;
  transition: all 0.3s ease;
}

.switch-wrapper:hover {
  border-color: var(--themeBackground);
  background: rgba(110, 69, 226, 0.05);
}

.switch-wrapper input[type="checkbox"] {
  position: relative;
  width: 44px;
  height: 24px;
  appearance: none;
  background: #e0e0e0;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.switch-wrapper input[type="checkbox"]:checked {
  background: var(--themeBackground);
}

.switch-wrapper input[type="checkbox"]::before {
  content: "";
  position: absolute;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: var(--header-background);
  color: var(--text-primary);
  top: 2px;
  left: 2px;
  transition: all 0.3s ease;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.switch-wrapper input[type="checkbox"]:checked::before {
  left: 22px;
}

.switch-label {
  font-size: 14px;
  color: #666;
  font-weight: 500;
  transition: color 0.3s ease;
}

.switch-wrapper:hover .switch-label {
  color: var(--themeBackground);
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .time-album-container {
    padding: 12px;
  }

  .album-header {
    display: flex;
    flex-wrap: nowrap;
    align-items: center;
    gap: 12px;
    margin-bottom: 16px;
    padding: 0 5px;
  }

  .album-title {
    font-size: 20px;
    margin: 0;
    white-space: nowrap;
  }

  .create-album-btn {
    padding: 8px 16px;
    white-space: nowrap;
    margin-left: auto;
  }

  .album-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
    padding: 0;
  }

  .album-card {
    border-radius: 16px;
    background: rgba(255, 255, 255, 0.8);
    backdrop-filter: blur(10px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
  }

  .album-info {
    padding: 12px;
  }

  .album-info h3 {
    font-size: 14px;
    margin-bottom: 4px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .album-info p {
    font-size: 12px;
    margin-bottom: 8px;
    -webkit-line-clamp: 2;
    max-height: 36px;
  }

  .album-footer {
    font-size: 11px;
  }

  .album-actions {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    background: linear-gradient(to top, rgba(0, 0, 0, 0.8), transparent);
    padding: 24px 12px 12px;
    opacity: 0;
    transition: opacity 0.3s ease;
  }

  .album-card:hover .album-actions,
  .album-card:active .album-actions {
    opacity: 1;
  }

  .edit-btn, .password-manage {
    padding: 6px 12px;
    font-size: 12px;
    background: rgba(255, 255, 255, 0.9);
    border-radius: 12px;
    color: #333;
  }

  .album-card {
    transform: none;
    transition: transform 0.3s ease, box-shadow 0.3s ease;
  }

  .album-card:active {
    transform: scale(0.95);
  }

  .album-cover {
    padding-top: 70%;
  }

  /* 在移动端隐藏密码管理按钮的文字 */
  .password-manage span:first-child {
    display: none;
  }

  .password-manage {
    width: 32px;
    height: 32px;
    padding: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
  }
}

.private-badge {
  position: absolute;
  top: 8px;
  left: 8px;
  background: rgba(0, 0, 0, 0.6);
  padding: 4px 8px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
  color: white;
  font-size: 14px;
}

.private-icon {
  font-size: 16px;
}

.private-label {
  font-size: 12px;
  background: rgba(255, 77, 79, 0.1);
  color: #ff4d4f;
  padding: 2px 6px;
  border-radius: 4px;
  margin-left: 8px;
  font-weight: normal;
}

.password-modal .modal-content {
  max-width: 400px;
}

.password-modal input[type="password"] {
  font-size: 16px;
  letter-spacing: 2px;
}

/* 添加图片加载进度效果 */
.image-skeleton::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 2px;
  background: linear-gradient(90deg, var(--themeBackground), #ff69b4);
  transform-origin: left;
  animation: progress 2s ease-in-out infinite;
}

@keyframes progress {
  0% {
    transform: scaleX(0);
    opacity: 0.8;
  }
  50% {
    transform: scaleX(1);
    opacity: 1;
  }
  100% {
    transform: scaleX(0);
    opacity: 0.8;
    transform-origin: right;
  }
}

.password-manage-modal .modal-content {
  max-width: 400px;
}

.password-actions {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.action-btn {
  flex: 1;
  padding: 12px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  background: var(--header-background);
  color: var(--text-primary);
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.action-btn:hover {
  border-color: var(--themeBackground);
  background: rgba(110, 69, 226, 0.05);
}

.action-icon {
  font-size: 24px;
}

.warning-text {
  color: #ff4d4f;
  font-size: 14px;
  margin-top: 8px;
}

/* 美化密码输入界面 */
.password-modal {
  max-width: 400px;
}

.password-modal .modal-header {
  text-align: center;
  border-bottom: none;
  padding-bottom: 0;
}

.password-modal .modal-header h2 {
  font-size: 24px;
  color: var(--themeBackground);
  margin-bottom: 16px;
}

.password-modal .form-item {
  margin: 24px 0;
}

.password-modal input[type="password"] {
  font-size: 18px;
  letter-spacing: 4px;
  text-align: center;
  padding: 16px;
  border: 2px solid #e0e0e0;
  border-radius: 12px;
  transition: all 0.3s ease;
  background: #f8f9fa;
}

.password-modal input[type="password"]:focus {
  border-color: var(--themeBackground);
  box-shadow: 0 0 0 4px rgba(110, 69, 226, 0.1);
  background: var(--header-background);
  color: var(--text-primary);
}

/* 相册操作按钮样式 */
.album-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.edit-btn, .password-manage {
  background: none;
  border: none;
  color: var(--themeBackground);
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 4px;
}

.edit-btn:hover, .password-manage:hover {
  background: rgba(110, 69, 226, 0.1);
}

/* 编辑模态框样式 */
.edit-modal {
  max-width: 500px;
}

.edit-modal .modal-header {
  border-bottom-color: #f0f0f0;
}

.edit-modal .form-item {
  margin-bottom: 20px;
}

.edit-modal input[type="text"],
.edit-modal textarea {
  width: 100%;
  padding: 12px;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.3s ease;
  background: #f8f9fa;
}

.edit-modal input[type="text"]:focus,
.edit-modal textarea:focus {
  border-color: var(--themeBackground);
  box-shadow: 0 0 0 3px rgba(110, 69, 226, 0.1);
  background: var(--header-background);
  color: var(--text-primary);
  outline: none;
}

/* 上传加载状态样式 */
.upload-loading {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.9);
  color: var(--themeBackground);
  gap: 12px;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid rgba(110, 69, 226, 0.2);
  border-top-color: var(--themeBackground);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.upload-loading span {
  font-size: 14px;
  font-weight: 500;
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
