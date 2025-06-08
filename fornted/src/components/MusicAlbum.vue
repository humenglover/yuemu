<template>
  <div class="music-album-container">
    <!-- 顶部操作栏 -->
    <div class="album-header">
      <h2 class="album-title" style="color: #3a3a3c">音乐专栏</h2>
      <button v-if="props.isOwner" class="create-album-btn" @click="showCreateModal = true">
        <span class="btn-icon">🎵</span>
        <span class="btn-text">创建专辑</span>
      </button>
    </div>

    <!-- 专辑列表 -->
    <div class="album-grid">
      <!-- 空状态展示 -->
      <div v-if="albums.length === 0" class="empty-state">
        <div class="empty-icon">🎵</div>
        <h3>还没有音乐专辑</h3>
        <p v-if="props.isOwner">点击右上角的按钮创建你的第一个音乐专辑吧！</p>
        <p v-else>主人还没有创建音乐专辑哦~</p>
      </div>
      <!-- 专辑列表 -->
      <div v-else v-for="album in albums" :key="album.id" class="album-card" :data-is-owner="props.isOwner" @click="handleAlbumClick(album)">
        <div class="album-cover">
          <div class="image-skeleton" v-if="!album.imageLoaded"></div>
          <img :src="album.coverUrl || '/default-album-cover.png'" :alt="album.albumName" @load="handleImageLoad(album)" :class="{ 'image-loaded': album.imageLoaded }">
          <div class="album-info-overlay">
            <h3 class="album-title">{{ album.albumName }}</h3>
            <span class="album-date">{{ formatDate(album.createTime) }}</span>
          </div>
          <div v-if="album.isPublic === 0" class="private-badge">
            <span class="private-icon">🔒</span>
          </div>
          <button v-if="props.isOwner"
                  class="delete-btn"
                  @click.stop="showDeleteConfirm(album)">
            <span class="delete-icon">✖</span>
          </button>
          <div v-if="props.isOwner" class="album-actions">
            <button class="edit-btn" @click.stop="handleEditClick(album)">
              <span class="edit-icon">✏️</span>
            </button>
            <button class="password-manage" @click.stop="handlePasswordManageClick(album)">
              <span class="password-icon">{{ album.isPublic === 1 ? '🔒' : '🔑' }}</span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 创建专辑模态框 -->
    <div v-if="showCreateModal" class="modal-overlay">
      <div class="modal-content create-modal">
        <div class="modal-header">
          <h2>🎵 创建新专辑</h2>
          <button class="modal-close" @click="showCreateModal = false">×</button>
        </div>
        <form class="modal-form" @submit.prevent="handleCreateAlbum">
          <div class="form-item">
            <label>专辑名称</label>
            <input type="text" v-model="newAlbum.albumName" placeholder="给专辑起个名字吧" required>
          </div>
          <div class="form-item">
            <label>专辑描述</label>
            <textarea v-model="newAlbum.description" rows="3" placeholder="写下对这个专辑的描述"></textarea>
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
                  <span class="upload-icon">🎵</span>
                  <div>点击上传封面</div>
                </div>
              </div>
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn-cancel" @click="showCreateModal = false">取消</button>
            <button type="submit" class="btn-confirm">创建专辑</button>
          </div>
        </form>
      </div>
    </div>

    <!-- 删除确认对话框 -->
    <div v-if="showDeleteModal" class="confirm-modal">
      <div class="confirm-content">
        <div class="confirm-icon">
          <span>(｡•́︿•̀｡)</span>
        </div>
        <div class="confirm-title">确定要删除专辑 "{{selectedAlbum?.albumName}}" 吗？</div>
        <div class="confirm-buttons">
          <button class="cancel-btn" @click="showDeleteModal = false">取消</button>
          <button class="confirm-btn" @click="confirmDelete">删除</button>
        </div>
      </div>
    </div>

    <!-- 密码验证模态框 -->
    <div v-if="showPasswordModal" class="modal-overlay">
      <div class="modal-content password-modal">
        <div class="modal-header">
          <h2>🔒 输入访问密码</h2>
          <button class="modal-close" @click="showPasswordModal = false">×</button>
        </div>
        <form class="modal-form" @submit.prevent="handlePasswordSubmit">
          <div class="form-item">
            <label>访问密码</label>
            <input
              type="password"
              v-model="passwordInput"
              placeholder="请输入专辑访问密码"
              required
              ref="passwordInputRef"
            >
          </div>
          <div class="modal-footer">
            <button type="button" class="btn-cancel" @click="showPasswordModal = false">取消</button>
            <button type="submit" class="btn-confirm">确认</button>
          </div>
        </form>
      </div>
    </div>

    <!-- 密码管理模态框 -->
    <div v-if="showPasswordManageModal" class="modal-overlay">
      <div class="modal-content password-manage-modal">
        <div class="modal-header">
          <h2>🔒 密码管理</h2>
          <button class="modal-close" @click="showPasswordManageModal = false">×</button>
        </div>
        <div class="modal-body">
          <div v-if="selectedAlbum?.isPublic === 1">
            <h3>设置访问密码</h3>
            <p>设置密码后，专辑将变为私密专辑</p>
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
              <p class="warning-text">⚠️ 取消密码后，专辑将变为公开专辑</p>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="showPasswordManageModal = false">取消</button>
          <button class="btn-confirm" @click="handlePasswordManage">确认</button>
        </div>
      </div>
    </div>

    <!-- 编辑专辑模态框 -->
    <div v-if="showEditModal" class="modal-overlay">
      <div class="modal-content edit-modal">
        <div class="modal-header">
          <h2>✏️ 编辑专辑</h2>
          <button class="modal-close" @click="showEditModal = false">×</button>
        </div>
        <form class="modal-form" @submit.prevent="handleEditAlbum">
          <div class="form-item">
            <label>专辑名称</label>
            <input type="text" v-model="editAlbum.albumName" placeholder="给专辑起个名字吧" required>
          </div>
          <div class="form-item">
            <label>专辑描述</label>
            <textarea v-model="editAlbum.description" rows="3" placeholder="写下对这个专辑的描述"></textarea>
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
                  <span class="upload-icon">🎵</span>
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
      </div>
    </div>

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
  createMusicAlbumUsingPost,
  fetchMusicAlbumsUsingGet,
  removeMusicAlbumUsingPost,
  fetchMusicAlbumByIdUsingGet,
  setMusicAlbumPasswordUsingPost,
  modifyMusicAlbumPasswordUsingPost,
  removeMusicAlbumPasswordUsingPost,
  modifyMusicAlbumUsingPost
} from '@/api/loveBoardMusicAlbumController'
import { uploadPostImageUsingPost } from '@/api/pictureController'
import { message } from 'ant-design-vue'

interface MusicAlbum {
  id: number;
  albumName: string;
  description?: string;
  coverUrl?: string;
  createTime: string;
  isPublic: number;
  musicCount?: number;
  imageLoaded?: boolean;
}

const router = useRouter()
const route = useRoute()
const props = defineProps<{
  loveBoardId: string,
  isOwner?: boolean
}>()

const albums = ref<MusicAlbum[]>([])
const showCreateModal = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)
const showDeleteModal = ref(false)
const selectedAlbum = ref<MusicAlbum | null>(null)
const showPasswordModal = ref(false)
const passwordInput = ref('')
const passwordInputRef = ref<HTMLInputElement | null>(null)
const selectedPrivateAlbum = ref<MusicAlbum | null>(null)
const showPasswordManageModal = ref(false)
const passwordForm = ref({
  oldPassword: '',
  newPassword: ''
})
const currentAction = ref('')
const showEditModal = ref(false)
const editAlbum = ref<MusicAlbum>({
  id: 0,
  albumName: '',
  description: '',
  coverUrl: '',
  isPublic: 1,
  createTime: ''
})

const newAlbum = ref({
  albumName: '',
  description: '',
  coverUrl: '',
  isPublic: 1,
  password: ''
})

const isUploading = ref(false)

// 格式化日期
const formatDate = (date: string) => {
  return dayjs(date).format('YYYY-MM-DD')
}

// 获取专辑列表
const fetchAlbums = async () => {
  try {
    const res = await fetchMusicAlbumsUsingGet({
      current: 1,
      pageSize: 20,
      loveBoardId: props.loveBoardId
    })
    if (res.data.code === 0) {
      const loadedStates = new Map(
        albums.value.map(album => [album.id, album.imageLoaded])
      )
      albums.value = res.data.data.records.map(album => ({
        ...album,
        imageLoaded: loadedStates.get(album.id) || false
      }))
    }
  } catch (error) {
    console.error('获取专辑列表失败:', error)
  }
}

// 处理专辑点击
const handleAlbumClick = (album: MusicAlbum) => {
  if (album.isPublic === 0) {
    selectedPrivateAlbum.value = album
    showPasswordModal.value = true
    nextTick(() => {
      passwordInputRef.value?.focus()
    })
  } else {
    router.push(`/music-album/${album.id}?isOwner=${props.isOwner}`)
  }
}

// 显示删除确认框
const showDeleteConfirm = (album: MusicAlbum) => {
  selectedAlbum.value = album
  showDeleteModal.value = true
}

// 确认删除
const confirmDelete = async () => {
  if (!selectedAlbum.value) return

  try {
    const res = await removeMusicAlbumUsingPost({
      id: selectedAlbum.value.id,
      loveBoardId: props.loveBoardId
    })
    if (res.data.code === 0) {
      albums.value = albums.value.filter(item => item.id !== selectedAlbum.value?.id)
      showMessage('删除成功', 'success')
    }
  } catch (error) {
    console.error('删除专辑失败:', error)
    showMessage('删除专辑失败', 'error')
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
    if (fileInput.value) {
      fileInput.value.value = ''
    }
  }
}

// 处理上传封面
const handleUploadCover = () => {
  fileInput.value?.click()
}

// 处理创建专辑
const handleCreateAlbum = async () => {
  try {
    const res = await createMusicAlbumUsingPost(
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
    console.error('创建专辑失败:', error)
  }
}

// 处理密码提交
const handlePasswordSubmit = async () => {
  if (!selectedPrivateAlbum.value) return

  try {
    const res = await fetchMusicAlbumByIdUsingGet({
      id: selectedPrivateAlbum.value.id,
      password: passwordInput.value
    })

    if (res.data.code === 0) {
      sessionStorage.setItem(`album_${selectedPrivateAlbum.value.id}_password`, passwordInput.value)
      router.push(`/music-album/${selectedPrivateAlbum.value.id}?isOwner=${props.isOwner}`)
      showPasswordModal.value = false
      passwordInput.value = ''
      selectedPrivateAlbum.value = null
    } else {
      message.error('密码错误')
      passwordInput.value = ''
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
const handleImageLoad = (album: MusicAlbum) => {
  const targetAlbum = albums.value.find(a => a.id === album.id)
  if (targetAlbum) {
    targetAlbum.imageLoaded = true
  }
}

// 处理密码管理按钮点击
const handlePasswordManageClick = (album: MusicAlbum) => {
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
      res = await setMusicAlbumPasswordUsingPost({
        albumId: selectedAlbum.value.id,
        newPassword: passwordForm.value.newPassword
      })
    } else if (currentAction.value === 'update') {
      res = await modifyMusicAlbumPasswordUsingPost({
        albumId: selectedAlbum.value.id,
        oldPassword: passwordForm.value.oldPassword,
        newPassword: passwordForm.value.newPassword
      })
    } else if (currentAction.value === 'remove') {
      res = await removeMusicAlbumPasswordUsingPost({
        albumId: selectedAlbum.value.id,
        oldPassword: passwordForm.value.oldPassword
      })
    }

    if (res?.data.code === 0) {
      message.success('操作成功')
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
const handleEditClick = (album: MusicAlbum) => {
  editAlbum.value = { ...album }
  showEditModal.value = true
}

// 处理编辑封面上传
const handleEditCoverUpload = () => {
  fileInput.value?.click()
}

// 处理编辑专辑提交
const handleEditAlbum = async () => {
  try {
    const res = await modifyMusicAlbumUsingPost(
      { loveBoardId: props.loveBoardId },
      {
        id: editAlbum.value.id,
        albumName: editAlbum.value.albumName,
        description: editAlbum.value.description,
        coverUrl: editAlbum.value.coverUrl,
        isPublic: editAlbum.value.isPublic
      }
    )
    if (res.data.code === 0) {
      showMessage('更新成功', 'success')
      showEditModal.value = false
      await fetchAlbums()
    }
  } catch (error: any) {
    console.error('更新专辑失败:', error)
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
.music-album-container {
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
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

  box-shadow: 0 4px 15px rgba(110, 69, 226, 0.2);
}

/* 专辑网格布局 */
.album-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
  padding: 10px;
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .music-album-container {
    padding: 12px;
  }

  .album-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
    padding: 8px;
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

  .create-album-btn {
    padding: 8px 16px;
    font-size: 14px;
  }

  .album-title {
    font-size: 20px;
  }

  .album-title::before {
    font-size: 24px;
  }

  .album-title::after {
    font-size: 24px;
  }

  .private-badge {
    width: 28px;
    height: 28px;
    top: 8px;
    right: 8px;
  }

  .private-icon {
    font-size: 14px;
  }

  .delete-btn {
    top: 8px;
    right: 8px;
    width: 28px;
    height: 28px;
    opacity: 1;
    transform: none;
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
}

/* 美化专辑卡片 */
.album-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.album-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.12);
}

.album-cover {
  position: relative;
  width: 100%;
  padding-top: 100%;
  overflow: hidden;
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

.album-info-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 48px 16px 16px;
  background: linear-gradient(to top,
  rgba(0, 0, 0, 0.85) 0%,
  rgba(0, 0, 0, 0.6) 50%,
  rgba(0, 0, 0, 0) 100%
  );
  color: #ffffff;
  transition: all 0.3s ease;
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
}

.album-title {
  font-size: 1.25rem;
  font-weight: 600;
  margin: 0 0 8px;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.4;
  color: #ffffff;
}

.album-date {
  font-size: 0.875rem;
  opacity: 0.95;
  display: flex;
  align-items: center;
  gap: 4px;
  color: #ffffff;
}

.album-date::before {
  content: "📅";
  font-size: 0.9em;
}

.private-badge {
  position: absolute;
  top: 12px;
  right: 52px;
  width: 32px;
  height: 32px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  z-index: 2;
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
}

.delete-btn {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 32px;
  height: 32px;
  background: rgba(255, 255, 255, 0.95);
  border: none;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transform: translateY(-10px);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 3;
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.album-card:hover .delete-btn {
  opacity: 1;
  transform: translateY(0);
}

.delete-btn:hover {
  background: #ff4d4f;
  color: white;
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .album-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
    padding: 8px;
  }

  .album-card {
    border-radius: 8px;
  }

  .album-info-overlay {
    padding: 12px 12px 12px;
  }

  .album-title {
    font-size: 1rem;
    -webkit-line-clamp: 1;
  }

  .album-date {
    font-size: 0.75rem;
  }

  .private-badge{
    width: 28px;
    height: 28px;
  }


  .private-icon,
  .delete-icon {
    font-size: 14px;
  }
}

/* 模态框样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 12px;
  width: 90%;
  max-width: 500px;
  max-height: 90vh;
  overflow-y: auto;
  padding: 24px;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.modal-header h2 {
  margin: 0;
  font-size: 20px;
  color: #333;
}

.modal-close {
  background: none;
  border: none;
  font-size: 24px;
  color: #999;
  cursor: pointer;
  padding: 4px;
}

.modal-close:hover {
  color: #666;
}

.modal-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-item label {
  font-size: 14px;
  color: #666;
}

.form-item input[type="text"],
.form-item input[type="password"],
.form-item textarea {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  transition: all 0.3s ease;
}

.form-item input[type="text"]:focus,
.form-item input[type="password"]:focus,
.form-item textarea:focus {
  border-color: var(--themeBackground);
  outline: none;
  box-shadow: 0 0 0 2px rgba(110, 69, 226, 0.1);
}

.switch-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
}

.switch-wrapper input[type="checkbox"] {
  width: 40px;
  height: 20px;
  appearance: none;
  background: #ddd;
  border-radius: 10px;
  position: relative;
  cursor: pointer;
  transition: all 0.3s ease;
}

.switch-wrapper input[type="checkbox"]:checked {
  background: var(--themeBackground);
}

.switch-wrapper input[type="checkbox"]::before {
  content: "";
  position: absolute;
  width: 16px;
  height: 16px;
  background: white;
  border-radius: 50%;
  top: 2px;
  left: 2px;
  transition: all 0.3s ease;
}

.switch-wrapper input[type="checkbox"]:checked::before {
  left: 22px;
}

.upload-wrapper {
  border: 2px dashed #ddd;
  border-radius: 8px;
  padding: 16px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
}

.upload-wrapper:hover {
  border-color: var(--themeBackground);
  background: rgba(110, 69, 226, 0.05);
}

.image-preview {
  width: 100%;
  height: 200px;
  position: relative;
  overflow: hidden;
  border-radius: 4px;
}

.image-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.upload-placeholder {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #999;
}

.upload-icon {
  font-size: 32px;
}

.upload-loading {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #666;
}

.loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid #f3f3f3;
  border-top: 3px solid var(--themeBackground);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
}

.btn-cancel, .btn-confirm {
  padding: 8px 20px;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
  color: #666;
}

.btn-cancel {
  background: #f5f5f5;
  border: none;
  color: #666;
}

.btn-cancel:hover {
  background: #eee;
}

.btn-confirm {
  background: var(--themeBackground);
  border: none;
  color: #666;
}

.btn-confirm:hover {
  opacity: 0.9;
}

/* 确认对话框样式 */
.confirm-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.confirm-content {
  background: white;
  border-radius: 12px;
  padding: 24px;
  width: 90%;
  max-width: 400px;
  text-align: center;
}

.confirm-icon {
  font-size: 48px;
  margin-bottom: 16px;
  color: #ff4d4f;
}

.confirm-title {
  font-size: 16px;
  color: #333;
  margin-bottom: 24px;
}

.confirm-buttons {
  display: flex;
  justify-content: center;
  gap: 12px;
}

.confirm-buttons button {
  padding: 8px 24px;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.cancel-btn {
  background: #f5f5f5;
  border: none;
  color: #666;
}

.cancel-btn:hover {
  background: #eee;
}

.confirm-btn {
  background: #ff4d4f;
  border: none;
  color: white;
}

.confirm-btn:hover {
  opacity: 0.9;
}

/* 密码管理模态框特定样式 */
.password-actions {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.action-btn {
  flex: 1;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  background: white;
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
  font-size: 12px;
  margin-top: 8px;
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

.album-actions {
  position: absolute;
  bottom: 12px;
  right: 12px;
  display: flex;
  gap: 8px;
  opacity: 0;
  transform: translateY(10px);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 3;
}

.album-card:hover .album-actions {
  opacity: 1;
  transform: translateY(0);
}

.edit-btn,
.password-manage {
  width: 32px;
  height: 32px;
  background: rgba(255, 255, 255, 0.95);
  border: none;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  transition: all 0.3s ease;
}

.edit-btn:hover,
.password-manage:hover {
  background: var(--themeBackground);
  color: white;
  transform: translateY(-2px);
}

.edit-icon,
.password-icon {
  font-size: 16px;
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .album-actions {
    bottom: 0;
    left: 0;
    right: 0;
    padding: 24px 12px 12px;
    background: linear-gradient(to top, rgba(0, 0, 0, 0.8), transparent);
    justify-content: center;
  }

  .edit-btn,
  .password-manage {
    width: 28px;
    height: 28px;
    background: rgba(255, 255, 255, 0.9);
  }

  .edit-icon,
  .password-icon {
    font-size: 14px;
  }
}
</style>
