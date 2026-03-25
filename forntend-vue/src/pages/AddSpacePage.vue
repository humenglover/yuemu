<template>
  <div class="space-page">
    <!-- 顶部标题 -->
    <div class="page-header">
      <h2 class="page-title">{{ route.query?.id ? '修改' : '创建' }} {{ SPACE_TYPE_MAP[spaceType] }}</h2>
    </div>

    <!-- 表单主体 -->
    <form class="space-form" @submit.prevent="handleSubmit">
      <!-- 空间名称 -->
      <div class="form-group">
        <label class="form-label">空间名称 <span class="required">*</span></label>
        <div class="input-wrap">
          <input
            v-model="spaceForm.spaceName"
            type="text"
            placeholder="请输入空间名称"
            class="form-input"
            required
          >
          <i
            v-if="spaceForm.spaceName"
            class="fas fa-times clear-icon"
            @click="spaceForm.spaceName = ''"
          ></i>
        </div>
      </div>

      <!-- 空间简介 -->
      <div class="form-group">
        <label class="form-label">空间简介</label>
        <textarea
          v-model="spaceForm.spaceDesc"
          placeholder="请输入空间简介（200字以内）"
          class="form-textarea"
          rows="3"
          maxlength="200"
        ></textarea>
        <span class="word-count">{{ spaceForm.spaceDesc?.length || 0 }}/200</span>
      </div>

      <!-- 封面上传 -->
      <div class="form-group">
        <label class="form-label">空间封面 <span class="required">*</span></label>
        <div class="upload-box">
          <div
            class="upload-area"
            @click="triggerFileSelect"
            :class="{ filled: spaceForm.spaceCover }"
          >
            <img v-if="spaceForm.spaceCover" :src="spaceForm.spaceCover" class="cover-img" alt="封面">
            <div v-else class="upload-placeholder">
              <i class="fas fa-image upload-icon"></i>
              <p class="upload-tip">点击上传封面图</p>
              <p class="upload-hint">建议 400x300 | JPG/PNG</p>
            </div>
          </div>
          <button
            v-if="spaceForm.spaceCover"
            type="button"
            class="remove-btn"
            @click="spaceForm.spaceCover = ''"
          >
            <i class="fas fa-trash-alt"></i> 移除
          </button>
        </div>
      </div>

      <!-- 空间级别 -->
      <div class="form-group">
        <label class="form-label">空间级别 <span class="required">*</span></label>
        <div class="select-wrap">
          <div class="select-display" @click.stop="toggleSelect">
            {{ spaceForm.spaceLevel !== undefined ? getLevelText(spaceForm.spaceLevel) : '请选择空间级别' }}
            <i class="fas fa-chevron-down select-icon" :class="{ rotate: selectOpen }"></i>
          </div>
          <ul class="select-options" v-show="selectOpen">
            <li
              v-for="option in filteredSpaceLevelOptions"
              :key="option.value"
              class="option-item"
              @click.stop="selectLevel(option.value)"
            >
              {{ option.label }}
            </li>
          </ul>
        </div>
      </div>

      <!-- 提交按钮 -->
      <div class="form-submit">
        <button type="submit" class="submit-btn" :disabled="loading || !validateForm()">
          <i v-if="loading" class="fas fa-spinner fa-spin"></i>
          <i v-else class="fas fa-check"></i>
          {{ route.query?.id ? '更新空间' : '创建空间' }}
        </button>
      </div>
    </form>

    <!-- 级别说明卡片 -->
    <div class="level-card">
      <div class="card-header">
        <i class="fas fa-info-circle"></i>
        <span>空间级别说明</span>
      </div>
      <div class="card-body">
        <p class="card-desc">
          目前仅支持开通普通版，如需升级请联系
          <a href="http://my.lumenglover.com" target="_blank" class="link">鹿梦</a>
        </p>
        <div class="level-list">
          <div
            v-for="item in spaceLevelList"
            :key="item.value"
            class="level-item"
          >
            {{ item.text }}：{{ formatSize(item.maxSize) }} | {{ item.maxCount }} 张
          </div>
        </div>
      </div>
    </div>

    <!-- 隐藏文件输入 -->
    <input
      ref="fileInputRef"
      type="file"
      accept="image/*"
      @change="handleFileChange"
      class="hidden-input"
    >
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, reactive, ref, computed } from 'vue'
import { message } from 'ant-design-vue'
import {
  addSpaceUsingPost,
  getSpaceVoByIdUsingGet,
  listSpaceLevelUsingGet,
  updateSpaceUsingPost,
} from '@/api/spaceController'
import { useRoute, useRouter } from 'vue-router'
import { SPACE_LEVEL_OPTIONS, SPACE_LEVEL_ENUM, SPACE_TYPE_ENUM, SPACE_TYPE_MAP } from '@/constants/space'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { formatSize } from '../utils'
import { uploadPostImageUsingPost } from '@/api/pictureController'

const loginUserStore = useLoginUserStore()
const isAdmin = computed(() => loginUserStore.loginUser?.userRole === 'admin')

const filteredSpaceLevelOptions = computed(() => {
  return isAdmin.value ? SPACE_LEVEL_OPTIONS : SPACE_LEVEL_OPTIONS.filter(opt => opt.value === SPACE_LEVEL_ENUM.COMMON)
})

declare namespace API {
  interface SpaceAddRequest {
    spaceName?: string;
    spaceType?: number;
    spaceLevel?: number;
    spaceDesc?: string;
    spaceCover?: string;
  }
  interface SpaceEditRequest {
    id?: string | number;
    spaceName?: string;
    spaceType?: number;
    spaceLevel?: number;
    spaceDesc?: string;
    spaceCover?: string;
  }
  interface SpaceLevel {
    value?: number;
    text?: string;
    maxSize?: number;
    maxCount?: number;
  }
  interface SpaceVO {
    id?: number;
    spaceName?: string;
    spaceLevel?: number;
    spaceDesc?: string;
    spaceCover?: string;
  }
}

const fileInputRef = ref<HTMLInputElement | null>(null)
const selectOpen = ref(false)
const loading = ref(false)
const space = ref<API.SpaceVO>()
const spaceLevelList = ref<API.SpaceLevel[]>([])

const spaceForm = reactive<API.SpaceAddRequest | API.SpaceEditRequest>({
  spaceName: '',
  spaceDesc: '',
  spaceCover: '',
  spaceLevel: undefined
})

const spaceType = computed(() => {
  return route.query?.type ? Number(route.query.type) : SPACE_TYPE_ENUM.PRIVATE
})

const route = useRoute()
const router = useRouter()

const triggerFileSelect = () => fileInputRef.value?.click()
const toggleSelect = () => selectOpen.value = !selectOpen.value

const getLevelText = (value: number) => {
  return SPACE_LEVEL_OPTIONS.find(opt => opt.value === value)?.label || ''
}

const selectLevel = (value: number) => {
  spaceForm.spaceLevel = value
  selectOpen.value = false
}

const validateForm = () => {
  if (!spaceForm.spaceName) return false
  if (!spaceForm.spaceCover) return false
  if (spaceForm.spaceLevel === undefined) return false
  return true
}

const uploadCoverImage = async (file: File) => {
  try {
    const res = await uploadPostImageUsingPost({}, {}, file)
    if (res.data.code === 0) {
      spaceForm.spaceCover = res.data.data.url
      message.success('封面上传成功')
    } else {
      message.error(`上传失败：${res.data.message}`)
    }
  } catch (err) {
    console.error('封面上传异常：', err)
    message.error('上传失败，请重试')
  }
}

const handleFileChange = async (e: Event) => {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/')) {
    message.error('请选择图片文件')
    return
  }
  await uploadCoverImage(file)
  target.value = ''
}

const fetchSpaceLevelList = async () => {
  const res = await listSpaceLevelUsingGet()
  if (res.data.code === 0 && res.data.data) {
    spaceLevelList.value = res.data.data
  } else {
    message.error(`获取级别失败：${res.data.message}`)
  }
}

const getOldSpace = async () => {
  const id = route.query?.id
  if (id) {
    const res = await getSpaceVoByIdUsingGet({ id })
    if (res.data.code === 0 && res.data.data) {
      const data = res.data.data
      space.value = data
      spaceForm.spaceName = data.spaceName || ''
      spaceForm.spaceLevel = data.spaceLevel
      spaceForm.spaceDesc = data.spaceDesc || ''
      spaceForm.spaceCover = data.spaceCover || ''
    }
  }
}

const handleSubmit = async () => {
  if (!validateForm()) return
  loading.value = true
  try {
    let res
    if (space.value?.id) {
      res = await updateSpaceUsingPost({ id: space.value.id, ...spaceForm })
    } else {
      res = await addSpaceUsingPost({ ...spaceForm, spaceType: spaceType.value })
    }
    if (res.data.code === 0 && res.data.data) {
      message.success('操作成功')
      router.push({ path: `/space/${res.data.data}` })
    } else {
      message.error(`操作失败：${res.data.message}`)
    }
  } catch (err) {
    console.error('提交异常：', err)
    message.error('操作失败，请重试')
  } finally {
    loading.value = false
  }
}

const handleClickOutside = (event: MouseEvent) => {
  const target = event.target as HTMLElement
  if (!target.closest('.select-wrap')) {
    selectOpen.value = false
  }
}

onMounted(() => {
  fetchSpaceLevelList()
  getOldSpace()
  window.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  window.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
  transition: var(--theme-transition);
}

.space-page {
  width: 100%;
  max-width: 560px;
  margin: 0 auto;
  padding: 16px;
  background-color: var(--background);
}

.page-header {
  text-align: center;
  margin-bottom: 20px;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--nav-item-active-text);
}

.space-form {
  background: var(--card-background);
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 1px 3px var(--shadow-color);
  margin-bottom: 16px;
  border: 1px solid var(--border-color);
}

.form-group {
  margin-bottom: 16px;
}

.form-label {
  display: block;
  font-size: 14px;
  color: var(--text-primary);
  margin-bottom: 6px;
  font-weight: 500;
}

.required {
  color: var(--comment-delete-hover-color);
  font-size: 12px;
  margin-left: 2px;
}

.input-wrap {
  position: relative;
}

.form-input {
  width: 100%;
  height: 40px;
  padding: 0 36px 0 12px;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  font-size: 14px;
  color: var(--text-primary);
  outline: none;
  transition: border-color 0.2s;
  background-color: var(--card-background);
}

.form-input:focus {
  border-color: var(--nav-item-active-text);
  box-shadow: 0 0 0 2px var(--nav-item-active);
}

.form-input::placeholder {
  color: var(--text-secondary);
}

.clear-icon {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 14px;
  color: var(--text-secondary);
  cursor: pointer;
}

.form-textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  font-size: 14px;
  color: var(--text-primary);
  outline: none;
  resize: none;
  transition: border-color 0.2s;
  background-color: var(--card-background);
}

.form-textarea:focus {
  border-color: var(--nav-item-active-text);
  box-shadow: 0 0 0 2px var(--nav-item-active);
}

.form-textarea::placeholder {
  color: var(--text-secondary);
}

.word-count {
  text-align: right;
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 4px;
}

.upload-box {
  width: 100%;
}

.upload-area {
  width: 100%;
  height: 120px;
  border: 1px dashed var(--border-color);
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  overflow: hidden;
  transition: all 0.2s;
  background-color: var(--card-background);
}

.upload-area:hover {
  border-color: var(--nav-item-active-text);
  background-color: var(--hover-background);
}

.upload-area.filled {
  border-style: solid;
  border-color: var(--nav-item-active-text);
}

.cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.upload-placeholder {
  text-align: center;
}

.upload-icon {
  font-size: 24px;
  color: var(--text-secondary);
  margin-bottom: 4px;
}

.upload-tip {
  font-size: 14px;
  color: var(--text-primary);
  margin-bottom: 2px;
}

.upload-hint {
  font-size: 12px;
  color: var(--text-secondary);
}

.remove-btn {
  margin-top: 8px;
  background: transparent;
  border: none;
  color: var(--comment-delete-hover-color);
  font-size: 13px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
}

.remove-btn:hover {
  color: var(--like-button-active-color);
}

.select-wrap {
  position: relative;
  width: 100%;
}

.select-display {
  height: 40px;
  padding: 0 36px 0 12px;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  font-size: 14px;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
  transition: border-color 0.2s;
  background-color: var(--card-background);
}

.select-display:hover {
  border-color: var(--nav-item-active-text);
  background-color: var(--hover-background);
}

.select-icon {
  font-size: 12px;
  color: var(--text-secondary);
  transition: transform 0.2s;
}

.select-icon.rotate {
  transform: rotate(180deg);
}

.select-options {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  right: 0;
  background: var(--card-background);
  border: 1px solid var(--border-color);
  border-radius: 6px;
  box-shadow: 0 4px 6px var(--shadow-color);
  list-style: none;
  z-index: 10;
  max-height: 160px;
  overflow-y: auto;
}

.option-item {
  padding: 10px 12px;
  font-size: 14px;
  color: var(--text-primary);
  cursor: pointer;
  transition: background-color 0.2s;
}

.option-item:hover {
  background-color: var(--hover-background);
  color: var(--nav-item-active-text);
}

.form-submit {
  margin-top: 8px;
}

.submit-btn {
  width: 100%;
  height: 44px;
  background-color: var(--nav-item-active-text);
  color: var(--text-other);
  border: none;
  border-radius: 6px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: background-color 0.2s;
}

.submit-btn:hover {
  background-color: var(--link-hover-color);
}

.submit-btn:disabled {
  background-color: var(--text-secondary);
  cursor: not-allowed;
}

.level-card {
  background: var(--card-background);
  padding: 16px;
  border-radius: 8px;
  box-shadow: 0 1px 3px var(--shadow-color);
  border: 1px solid var(--border-color);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 500;
  color: var(--nav-item-active-text);
  margin-bottom: 12px;
}

.card-body {
  font-size: 13px;
  color: var(--text-secondary);
}

.card-desc {
  line-height: 1.5;
  margin-bottom: 10px;
}

.link {
  color: var(--link-color);
  text-decoration: none;
}

.link:hover {
  color: var(--link-hover-color);
  text-decoration: underline;
}

.level-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.level-item {
  line-height: 1.4;
}

.hidden-input {
  display: none;
}

/* 响应式适配 */
@media (max-width: 480px) {
  .space-page {
    padding: 12px;
  }
  .space-form {
    padding: 16px;
  }
  .form-group {
    margin-bottom: 14px;
  }
  .submit-btn {
    height: 40px;
    font-size: 14px;
  }
  .upload-area {
    height: 100px;
  }
}

/* 暗色模式适配（通过全局 dark-theme 类控制，无需媒体查询） */
.dark-theme {
  .space-page {
    background-color: var(--background);
  }

  .space-form, .level-card {
    background-color: var(--card-background);
    box-shadow: 0 1px 3px var(--shadow-color);
    border-color: var(--border-color);
  }

  .form-label, .form-input, .form-textarea, .select-display, .option-item {
    color: var(--text-primary);
  }

  .form-input, .form-textarea, .select-display, .select-options {
    border-color: var(--border-color);
    background-color: var(--card-background);
  }

  .form-input:focus, .form-textarea:focus, .select-display:hover, .upload-area:hover, .upload-area.filled {
    border-color: var(--nav-item-active-text);
  }

  .upload-area {
    background-color: var(--card-background);
  }

  .upload-area:hover {
    background-color: var(--hover-background);
  }

  .option-item:hover {
    background-color: var(--hover-background);
  }

  .card-header {
    color: var(--nav-item-active-text);
  }

  .card-body {
    color: var(--text-secondary);
  }

  .upload-tip {
    color: var(--text-primary);
  }

  .upload-hint {
    color: var(--text-secondary);
  }
}
</style>
