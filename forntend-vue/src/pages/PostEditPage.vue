<template>
  <div id="postEditPage" class="box">
    <div class="editor-container">
      <div class="mobile-step-nav" v-if="isMobile">
        <div class="step-item" :class="{ active: currentStep === 1 }" @click="goToStep(1)">
          <span class="step-num">
            <i class="fas fa-check" v-if="currentStep === 1"></i>
            <span v-else>1</span>
          </span>
          <span class="step-text">内容编辑</span>
        </div>
        <div class="step-divider">|</div>
        <div class="step-item" :class="{ active: currentStep === 2, disabled: currentStep !== 2 && (!postForm.title.trim() || !postForm.content.trim()) }" @click="goToStep(2)">
          <span class="step-num">
            <i class="fas fa-check" v-if="currentStep === 2"></i>
            <span v-else>2</span>
          </span>
          <span class="step-text">发布配置</span>
        </div>
        <!-- 移动端编辑器图片上传进度指示器 - 显示在步骤导航右侧 -->
        <div v-if="uploading && currentStep === 1" class="editor-upload-progress">
          <svg class="progress-svg" viewBox="0 0 24 24" width="24" height="24">
            <circle class="progress-circle-bg" cx="12" cy="12" r="10" stroke="#eee" stroke-width="2" fill="none" />
            <circle
              class="progress-circle-fill"
              cx="12" cy="12" r="10" stroke="#007bff" stroke-width="2" fill="none"
              :stroke-dasharray="`${(uploadProgress / 100) * 62.8}, 62.8`"
              stroke-linecap="round"
              transform="rotate(-90 12 12)"
            />
            <text x="12" y="16" text-anchor="middle" font-size="8" fill="#333">{{ uploadProgress }}%</text>
          </svg>
        </div>
      </div>

      <div class="page-header" v-if="!isMobile">
        <h1>{{ isEdit ? '编辑帖子' : '发布帖子' }}</h1>
        <div class="tooltip-container">
          <div class="tooltip-icon">
            <i class="fas fa-exclamation-circle"></i>
          </div>
          <div class="tooltip-content">发帖提示：添加图片可提高通过率</div>
        </div>
      </div>

      <div class="pc-layout" v-if="!isMobile">
        <div class="pc-left">
          <div class="form-item required">
            <div class="input-wrapper">
              <input
                v-model="postForm.title"
                placeholder="输入标题"
                maxlength="100"
                class="title-input"
              >
              <span class="char-count">{{ postForm.title.length }}/100</span>
            </div>
          </div>

          <div class="form-item required editor-form-item">
            <div class="editor-wrapper">
              <Toolbar
                class="editor-toolbar"
                :editor="editorRef"
                :defaultConfig="toolbarConfig"
                :mode="mode"
              />
              <Editor
                class="editor-content"
                v-model="postForm.content"
                :defaultConfig="editorConfig"
                :mode="mode"
                @onCreated="handleCreated"
              />
            </div>
          </div>
        </div>

        <div class="pc-right">
          <div class="config-panel">
            <div class="form-item required">
              <label class="form-label"><i class="fas fa-folder-open"></i> 所属分类</label>
              <div class="select-btn" @click="openCategoryModal = true">
                <span class="select-display">{{ postForm.category || '选择分类' }}</span>
                <i class="fas fa-chevron-right select-icon"></i>
              </div>
            </div>

            <div class="form-item">
              <label class="form-label"><i class="fas fa-tags"></i> 帖子标签</label>
              <div class="tags-wrapper">
                <div class="tags-selected">
                  <span
                    class="tag-item"
                    v-for="(tag, index) in postForm.tags"
                    :key="index"
                  >
                    {{ tag }}
                    <i class="fas fa-times" @click.stop="removeTag(index)"></i>
                  </span>
                </div>
                <div class="select-btn" @click="openTagModal = true">
                  <span class="select-display">{{ postForm.tags.length > 0 ? `${postForm.tags.length}个标签已选择` : '选择标签(可选)' }}</span>
                  <i class="fas fa-chevron-right select-icon"></i>
                </div>
              </div>
            </div>

            <div class="form-item">
              <label class="form-label"><i class="fas fa-image"></i> 封面图</label>
              <div class="cover-image-section">
                <div class="cover-upload-area" @click="triggerCoverUpload">
                  <div v-if="postForm.coverUrl" class="cover-preview">
                    <img :src="postForm.coverUrl" alt="封面预览" class="cover-image" />
                    <div class="cover-overlay">
                      <span>点击更换封面</span>
                    </div>
                  </div>
                  <div v-else class="cover-placeholder">
                    <i class="fas fa-plus placeholder-icon"></i>
                    <div class="placeholder-text">上传封面图</div>
                  </div>
                  <!-- 封面上传图标 - 始终显示在封面图右上角 -->
                  <div v-if="uploading" class="upload-progress-icon">
                    <i class="fas fa-sync fa-spin" :class="{'upload-complete': uploadProgress >= 100}"></i>
                  </div>
                </div>
                <input
                  type="file"
                  ref="coverFileInput"
                  accept="image/*"
                  @change="handleCoverUpload"
                  class="file-input"
                />
                <div class="cover-action-buttons">
                  <button
                    class="action-btn secondary-btn full-width"
                    @click="selectCoverFromContent"
                    v-if="postForm.content"
                  >
                    <i class="fas fa-magic"></i> 从内容中选择封面图
                  </button>
                  <button
                    class="clear-cover-btn"
                    @click="clearCoverImage"
                    v-if="postForm.coverUrl"
                  >
                    <i class="fas fa-trash"></i> 清除封面
                  </button>
                </div>
              </div>
            </div>

            <div class="form-actions">
              <button class="action-btn cancel-btn" @click="handleCancel">
                <i class="fas fa-arrow-left"></i> 取消
              </button>
              <button class="action-btn clear-btn" @click="clearDraftContent" v-if="!isEdit || (isEdit && route.query.draft)" :class="{ loading: clearingContent }">
                <span v-if="!clearingContent"><i class="fas fa-eraser"></i> 清空内容</span>
                <span v-else><i class="fas fa-spinner fa-spin"></i> 清空中...</span>
              </button>
              <button class="action-btn draft-btn" @click="saveDraft" v-if="!isEdit || (isEdit && route.query.draft)" :class="{ loading: submitting }">
                <span v-if="!submitting"><i class="fas fa-save"></i> 存为草稿</span>
                <span v-else><i class="fas fa-spinner fa-spin"></i> 保存中...</span>
              </button>
              <button class="action-btn submit-btn" @click="handleSubmit" :class="{ loading: submitting }">
                <span v-if="!submitting"><i class="fas fa-paper-plane"></i> {{ isEdit ? '保存' : '发布' }}</span>
                <span v-else><i class="fas fa-spinner fa-spin"></i> {{ isEdit ? '保存中...' : '发布中...' }}</span>
              </button>
            </div>

            <div v-if="uploading" class="upload-progress">
              <div class="progress-bar">
                <div class="progress-fill" :style="{ width: uploadProgress + '%' }"></div>
              </div>
              <div class="progress-text">{{ uploadProgress >= 100 ? '处理中...' : '上传中...' }}</div>
            </div>
          </div>
        </div>
      </div>

      <div class="mobile-layout" v-if="isMobile">
        <div class="mobile-step-content" v-if="currentStep === 1">
          <div class="form-item required">
            <div class="input-wrapper">
              <input
                v-model="postForm.title"
                placeholder="输入标题"
                maxlength="100"
                class="title-input"
              >
              <span class="char-count">{{ postForm.title.length }}/100</span>
            </div>
          </div>

          <div class="form-item required editor-form-item">
            <div class="editor-wrapper">
              <Toolbar
                class="editor-toolbar"
                :editor="editorRef"
                :defaultConfig="toolbarConfig"
                :mode="mode"
              />
              <Editor
                class="editor-content"
                v-model="postForm.content"
                :defaultConfig="editorConfig"
                :mode="mode"
                @onCreated="handleCreated"
              />
            </div>
          </div>

          <div class="mobile-step-actions">
            <button class="action-btn cancel-btn" @click="handleCancel">
              <i class="fas fa-arrow-left"></i> 取消
            </button>
            <button
              class="action-btn next-btn"
              @click="goToNextStep()"
            >
              下一步
              <i class="fas fa-arrow-right"></i>
            </button>
          </div>
        </div>

        <div class="mobile-step-content" v-if="currentStep === 2">
          <div class="form-item required">
            <label class="form-label"><i class="fas fa-folder-open"></i> 所属分类</label>
            <div class="select-btn" @click="openCategoryModal = true">
              <span class="select-display">{{ postForm.category || '选择分类' }}</span>
              <i class="fas fa-chevron-right select-icon"></i>
            </div>
          </div>

          <div class="form-item">
            <label class="form-label"><i class="fas fa-tags"></i> 帖子标签</label>
            <div class="tags-wrapper">
              <div class="tags-selected">
                <span
                  class="tag-item"
                  v-for="(tag, index) in postForm.tags"
                  :key="index"
                >
                  {{ tag }}
                  <i class="fas fa-times" @click.stop="removeTag(index)"></i>
                </span>
              </div>
              <div class="select-btn" @click="openTagModal = true">
                <span class="select-display">{{ postForm.tags.length > 0 ? `${postForm.tags.length}个标签已选择` : '选择标签(可选)' }}</span>
                <i class="fas fa-chevron-right select-icon"></i>
              </div>
            </div>
          </div>

          <div class="form-item">
            <label class="form-label"><i class="fas fa-image"></i> 封面图</label>
            <div class="cover-image-section">
              <div class="cover-upload-area" @click="triggerCoverUpload">
                <div v-if="postForm.coverUrl" class="cover-preview">
                  <img :src="postForm.coverUrl" alt="封面预览" class="cover-image" />
                  <div class="cover-overlay">
                    <span>点击更换封面</span>
                  </div>
                </div>
                <div v-else class="cover-placeholder">
                  <i class="fas fa-plus placeholder-icon"></i>
                  <div class="placeholder-text">上传封面图</div>
                </div>
                <!-- 移动端封面上传图标 - 始终显示在封面图右上角 -->
                <div v-if="uploading" class="upload-progress-icon">
                  <i class="fas fa-sync fa-spin" :class="{'upload-complete': uploadProgress >= 100}"></i>
                </div>
              </div>
              <input
                type="file"
                ref="coverFileInput"
                accept="image/*"
                @change="handleCoverUpload"
                class="file-input"
              />
              <div class="cover-action-buttons">
                <button
                  class="action-btn secondary-btn full-width"
                  @click="selectCoverFromContent"
                  v-if="postForm.content"
                >
                  <i class="fas fa-magic"></i> 从内容中选择封面图
                </button>
                <button
                  class="clear-cover-btn"
                  @click="clearCoverImage"
                  v-if="postForm.coverUrl"
                >
                  <i class="fas fa-trash"></i> 清除封面
                </button>
              </div>
            </div>
          </div>

          <div class="form-actions">
            <div class="action-row mobile-two-column">
              <button class="action-btn prev-btn" @click="goToStep(1)">
                <i class="fas fa-arrow-left"></i> 上一步
              </button>
              <button class="action-btn clear-btn" @click="clearDraftContent" v-if="!isEdit || (isEdit && route.query.draft)" :class="{ loading: clearingContent }">
                <span v-if="!clearingContent"><i class="fas fa-eraser"></i> 清空内容</span>
                <span v-else><i class="fas fa-spinner fa-spin"></i> 清空中...</span>
              </button>
              <button class="action-btn draft-btn" @click="saveDraft" v-if="!isEdit || (isEdit && route.query.draft)" :class="{ loading: submitting }">
                <span v-if="!submitting"><i class="fas fa-save"></i> 存为草稿</span>
                <span v-else><i class="fas fa-spinner fa-spin"></i> 保存中...</span>
              </button>
              <button class="action-btn submit-btn" @click="handleSubmit" :class="{ loading: submitting }">
                <span v-if="!submitting"><i class="fas fa-paper-plane"></i> {{ isEdit ? '保存' : '发布' }}</span>
                <span v-else><i class="fas fa-spinner fa-spin"></i> {{ isEdit ? '保存中...' : '发布中...' }}</span>
              </button>
            </div>
          </div>

          <div v-if="uploading" class="upload-progress">
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: uploadProgress + '%' }"></div>
            </div>
            <div class="progress-text">{{ uploadProgress >= 100 ? '处理中...' : '上传中...' }}</div>
          </div>
        </div>
      </div>
    </div>



    <div class="modal-overlay" v-if="showConfirmModal">
      <div class="modal-content confirm-modal">
        <div class="modal-header">
          <h3><i class="fas fa-question-circle"></i> {{ confirmConfig.title }}</h3>
        </div>
        <div class="modal-body">
          <p>{{ confirmConfig.content }}</p>
        </div>
        <div class="modal-footer">
          <button class="modal-btn cancel" @click="confirmConfig.onCancel()">
            <i class="fas fa-ban"></i> 取消
          </button>
          <button class="modal-btn confirm" @click="confirmConfig.onConfirm()">
            <i class="fas fa-check"></i> 确定
          </button>
        </div>
      </div>
    </div>

    <!-- 草稿加载确认弹框 -->
    <div class="modal-overlay" v-if="showDraftConfirmModal">
      <div class="modal-content confirm-modal draft-confirm-modal">
        <div class="modal-header">
          <h3><i class="fas fa-file-alt"></i> 发现草稿</h3>
        </div>
        <div class="modal-body">
          <p style="margin: 0 12px;">检测到您有一份未完成的草稿，是否加载草稿内容？</p>
        </div>
        <div class="modal-footer">
          <button class="modal-btn cancel" @click="cancelDraftLoad()">
            <i class="fas fa-ban"></i> 不，我要新建
          </button>
          <button class="modal-btn confirm" @click="confirmDraftLoad()" :class="{ loading: submitting }">
            <span v-if="!submitting"><i class="fas fa-download"></i> 加载草稿</span>
            <span v-else><i class="fas fa-spinner fa-spin"></i> 加载中...</span>
          </button>
        </div>
      </div>
    </div>

    <!-- 清除内容确认弹框 -->
    <div class="modal-overlay" v-if="showClearConfirmModal">
      <div class="modal-content confirm-modal clear-confirm-modal">
        <div class="modal-header">
          <h3><i class="fas fa-eraser"></i> 确认清空内容</h3>
        </div>
        <div class="modal-body">
          <p style="margin: 0 12px;">您确定要清空当前所有内容吗？此操作不可恢复。</p>
        </div>
        <div class="modal-footer">
          <button class="modal-btn cancel" @click="cancelClearContent()">
            <i class="fas fa-times"></i> 取消
          </button>
          <button class="modal-btn confirm danger" @click="confirmClearContent()" :class="{ loading: clearingContent }">
            <span v-if="!clearingContent"><i class="fas fa-check"></i> 确认清空</span>
            <span v-else><i class="fas fa-spinner fa-spin"></i> 清空中...</span>
          </button>
        </div>
      </div>
    </div>

    <!-- 离开页面确认弹框 -->
    <div class="modal-overlay" v-if="showLeaveConfirmModal">
      <div class="modal-content confirm-modal leave-confirm-modal">
        <div class="modal-header">
          <h3><i class="fas fa-exclamation-triangle"></i> 内容尚未保存</h3>
        </div>
        <div class="modal-body">
          <p style="margin: 0 12px;">您有未保存的内容，确定要离开吗？</p>
        </div>
        <div class="modal-footer">
          <button class="modal-btn cancel" @click="cancelLeave()">
            <i class="fas fa-pen"></i> 继续编辑
          </button>
          <button class="modal-btn confirm" @click="saveDraftAndLeave" :class="{ loading: submitting }">
            <span v-if="!submitting"><i class="fas fa-save"></i> 保存草稿并离开</span>
            <span v-else><i class="fas fa-spinner fa-spin"></i> 保存中...</span>
          </button>
          <button class="modal-btn danger" @click="confirmLeaveWithoutSave()">
            <i class="fas fa-times"></i> 不保存离开
          </button>
        </div>
      </div>
    </div>

    <!-- 从内容中选择封面图弹窗 -->
    <div class="modal-overlay" v-if="showSelectCoverFromContent">
      <div class="modal-content select-cover-modal">
        <div class="modal-header">
          <h3><i class="fas fa-magic"></i> 从内容中选择封面图</h3>
          <button class="modal-close" @click="showSelectCoverFromContent = false">
            <i class="fas fa-times"></i>
          </button>
        </div>
        <div class="modal-body">
          <div class="cover-images-grid">
            <div
              class="cover-image-item"
              v-for="(image, index) in extractImagesFromContent()"
              :key="index"
              @click="useImageAsCover(image)"
            >
              <img :src="image" :alt="`内容图片 ${index + 1}`" class="cover-preview-img" />
              <div class="cover-overlay">
                <span>设为封面</span>
              </div>
            </div>
          </div>
          <div v-if="extractImagesFromContent().length === 0" class="no-images-message">
            <i class="fas fa-info-circle"></i>
            <p>内容中未找到可用的图片</p>
          </div>
        </div>
        <div class="modal-footer">
          <button class="modal-btn cancel" @click="showSelectCoverFromContent = false">
            <i class="fas fa-times"></i> 取消
          </button>
        </div>
      </div>
    </div>

    <!-- 清除封面图确认弹窗 -->
    <div class="modal-overlay" v-if="showClearCoverConfirm">
      <div class="modal-content confirm-modal clear-cover-confirm-modal">
        <div class="modal-header">
          <h3><i class="fas fa-trash"></i> 确认清空封面图</h3>
        </div>
        <div class="modal-body">
          <p style="margin: 0 12px;">您确定要清除当前封面图吗？此操作不可恢复。</p>
        </div>
        <div class="modal-footer">
          <button class="modal-btn cancel" @click="cancelClearCover()">
            <i class="fas fa-times"></i> 取消
          </button>
          <button class="modal-btn confirm danger" @click="confirmClearCover()">
            <i class="fas fa-check"></i> 确认清空
          </button>
        </div>
      </div>
    </div>

    <div class="modal-overlay" v-if="openCategoryModal">
      <div class="modal-content select-modal">
        <div class="modal-header">
          <h3><i class="fas fa-folder-open"></i> 选择分类</h3>
          <button class="modal-close" @click="openCategoryModal = false">
            <i class="fas fa-times"></i>
          </button>
        </div>
        <div class="modal-body">
          <div class="search-input-wrapper" v-if="categories.length > 5">
            <i class="fas fa-search search-icon"></i>
            <input
              v-model="categorySearchText"
              placeholder="搜索分类..."
              class="modal-search-input"
              @input="filterCategories"
            />
            <button
              v-if="categorySearchText"
              class="search-clear-btn"
              @click.stop="clearCategorySearch"
            >
              <i class="fas fa-times"></i>
            </button>
          </div>
          <div class="select-options-container">
            <div
              class="select-option"
              v-for="category in filteredCategories"
              :key="category"
              @click="handleCategorySelect(category)"
            >
              {{ category }}
              <i class="fas fa-check" v-if="postForm.category === category"></i>
            </div>
            <div v-if="filteredCategories.length === 0 && categorySearchText" class="no-result">
              <i class="fas fa-search"></i>
              <span>未找到 "{{ categorySearchText }}" 相关分类</span>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="modal-btn cancel" @click="openCategoryModal = false">
            <i class="fas fa-times"></i> 取消
          </button>
          <button class="modal-btn confirm" @click="openCategoryModal = false" :disabled="!postForm.category">
            <i class="fas fa-check"></i> 确认选择
          </button>
        </div>
      </div>
    </div>

    <div class="modal-overlay" v-if="openTagModal">
      <div class="modal-content select-modal">
        <div class="modal-header">
          <h3><i class="fas fa-tags"></i> 选择标签</h3>
          <button class="modal-close" @click="openTagModal = false">
            <i class="fas fa-times"></i>
          </button>
        </div>
        <div class="modal-body">
          <div class="search-input-wrapper" v-if="tags.length > 5">
            <i class="fas fa-search search-icon"></i>
            <input
              v-model="tagSearchText"
              placeholder="搜索标签..."
              class="modal-search-input"
              @input="filterTags"
            />
            <button
              v-if="tagSearchText"
              class="search-clear-btn"
              @click.stop="clearTagSearch"
            >
              <i class="fas fa-times"></i>
            </button>
          </div>
          <div class="select-options-container tags-list">
            <div
              class="select-option"
              v-for="tag in filteredTags"
              :key="tag"
              :class="{ selected: postForm.tags.includes(tag) }"
              @click="toggleTag(tag)"
            >
              {{ tag }}
              <i class="fas fa-check" v-if="postForm.tags.includes(tag)"></i>
            </div>
            <div v-if="filteredTags.length === 0 && tagSearchText" class="no-result">
              <i class="fas fa-search"></i>
              <span>未找到 "{{ tagSearchText }}" 相关标签</span>
            </div>
          </div>
          <div class="custom-tag-input" v-if="tags.length <= 5">
            <input
              v-model="customTagInput"
              placeholder="输入自定义标签后按回车添加"
              class="tag-input"
              @keyup.enter="addCustomTag"
            />
          </div>
          <div class="selected-tags-hint" v-if="postForm.tags.length > 0">
            <span class="hint-text">已选择 {{ postForm.tags.length }}/3 个标签</span>
            <div class="selected-tags-preview">
              <span
                class="tag-item mini"
                v-for="(tag, index) in postForm.tags"
                :key="index"
              >
                {{ tag }}
                <i class="fas fa-times" @click.stop="removeTag(index)"></i>
              </span>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="modal-btn cancel" @click="openTagModal = false">
            <i class="fas fa-times"></i> 取消
          </button>
          <button class="modal-btn confirm" @click="openTagModal = false">
            <i class="fas fa-check"></i> 确认选择
          </button>
        </div>
      </div>
    </div>

    <div class="toast" :class="{ show: showToast }">
      <i class="fas" :class="toastType === 'success' ? 'fa-check-circle' : 'fa-exclamation-circle'"></i>
      <span>{{ toastMessage }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, shallowRef, onBeforeUnmount, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import '@wangeditor/editor/dist/css/style.css'
import { addPostUsingPost, updatePostUsingPost, listPostTagCategoryUsingGet, getPostLatestDraftUsingGet, savePostDraftUsingPost } from '@/api/postController'
import { uploadPostImageUsingPost } from '@/api/pictureController'

const route = useRoute()
const router = useRouter()
const editorRef = shallowRef()
const submitting = ref(false)
const clearingContent = ref(false)
const mode = 'default'
const uploading = ref(false)
const uploadProgress = ref(0)
const coverFileInput = ref<HTMLInputElement>()
const isMobile = ref(false)
const currentStep = ref(1)
const isUploading = ref(false)
const uploadQueue: { file: File; insertFn: any }[] = []

const selectOpen = ref({
  category: false,
  tags: false
})

const categorySearchText = ref('')
const tagSearchText = ref('')
const customTagInput = ref('')
const filteredCategories = ref([])
const filteredTags = ref([])

const showConfirmModal = ref(false)
const showDraftConfirmModal = ref(false)
const showClearConfirmModal = ref(false)
const draftLoadCallback = ref(() => {})
const confirmConfig = ref({
  title: '',
  content: '',
  onConfirm: () => {},
  onCancel: () => {}
})

const showToast = ref(false)
const toastMessage = ref('')
const toastType = ref<'success' | 'error' | 'warning'>('success')
const openCategoryModal = ref(false)
const openTagModal = ref(false)

const checkDevice = () => {
  isMobile.value = window.innerWidth < 768
}

watch(() => window.innerWidth, () => {
  checkDevice()
})

const compressImage = (file: File): Promise<File> => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.readAsDataURL(file);
    reader.onload = (e) => {
      const img = new Image();
      img.src = e.target?.result as string;
      img.onload = () => {
        let width = img.width;
        let height = img.height;

        if (width > 1920 || height > 1080) {
          const ratio = Math.min(1920 / width, 1080 / height);
          width = Math.floor(width * ratio);
          height = Math.floor(height * ratio);
        }

        const maxFileSize = 2 * 1024 * 1024;
        let quality = 0.8;

        const compressAndCheck = () => {
          const canvas = document.createElement('canvas');
          canvas.width = width;
          canvas.height = height;
          const ctx = canvas.getContext('2d');
          ctx?.drawImage(img, 0, 0, width, height);

          canvas.toBlob((blob) => {
            if (!blob) { reject(new Error('压缩失败')); return; }

            if (blob.size <= maxFileSize || quality <= 0.1) {
              const compressedFile = new File([blob], file.name, { type: blob.type, lastModified: Date.now() });
              resolve(compressedFile);
            } else {
              quality -= 0.1;
              compressAndCheck();
            }
          }, 'image/jpeg', quality);
        };

        compressAndCheck();
      };
      img.onerror = () => reject(new Error('图片加载失败'));
    };
    reader.onerror = () => reject(new Error('读取失败'));
  });
}

const compressImages = async (files: File[]): Promise<File[]> => {
  const compressedFiles: File[] = [];
  for (const file of files) {
    if (!file.type.startsWith('image/')) continue;
    const compressedFile = await compressImage(file);
    compressedFiles.push(compressedFile);
  }
  return compressedFiles;
}

const processUploadQueue = async () => {
  while (uploadQueue.length > 0) {
    const { file, insertFn } = uploadQueue.shift()!;

    try {
      uploading.value = true;
      uploadProgress.value = 0;

      if (file.size > 10 * 1024 * 1024) throw new Error('图片不能超过10MB');
      if (!file.type.startsWith('image/')) throw new Error('只能上传图片');

      uploadProgress.value = 10;
      const [compressedFile] = await compressImages([file]);
      uploadProgress.value = 40;

      const formData = new FormData();
      formData.append('file', compressedFile);

      const res = await uploadPostImageUsingPost(
        {},
        { headers: { 'Content-Type': 'multipart/form-data' } },
        compressedFile
      );

      uploadProgress.value = 90;

      if (res.data.code === 0 && res.data.data) {


        insertFn(res.data.data.url);
        uploadProgress.value = 100;
        showToastMessage('图片上传成功', 'success');
      } else {
        throw new Error(res.data.message || '上传失败');
      }
    } catch (error: any) {
      showToastMessage('上传失败: ' + (error.message || '未知错误'), 'error');
    } finally {
      await new Promise(resolve => setTimeout(resolve, 300));
    }
  }

  uploading.value = false;
  uploadProgress.value = 0;
  isUploading.value = false;
}

const editorConfig = {
  placeholder: '开始创作...',
  html: true,
  autoFocus: true,
  maxLength: 5000,
  onMaxLength: () => {
    showToastMessage('内容最多不能超过5000字', 'warning');
  },
  // 性能优化配置，处理大量内容粘贴
  scroll: true,
  pasteFilter: true, // 开启粘贴过滤
  pasteIgnoreImg: false, // 不忽略粘贴的图片
  pasteTextHandle: (content: string) => {
    // 处理粘贴的文本内容，限制长度
    if (content.length > 5000) {
      const currentLength = postForm.value.content.length;
      const remainingLength = 5000 - currentLength;
      if (remainingLength > 0) {
        showToastMessage(`粘贴的内容过长，已截取前${remainingLength}个字符`, 'warning');
        return content.substring(0, remainingLength);
      } else {
        showToastMessage('内容已达到最大长度限制(5000字符)，无法继续粘贴', 'warning');
        return '';
      }
    }
    return content;
  },
  MENU_CONF: {
    uploadImage: {
      async customUpload(file: File, insertFn: any) {
        uploadQueue.push({ file, insertFn });
        if (!isUploading.value) {
          isUploading.value = true;
          await processUploadQueue();
        }
      }
    }
  },
  EXTEND_CONF: {
    image: {
      draggable: true,
      resizable: false,
      customConfig: {
        allowDrag: true,
        showMenu: true
      }
    }
  },
  hoverbarKeys: { image: { menuKeys: ['deleteImage', 'viewImage', 'linkImage'] } },
  readOnly: false,
  focus: true,
}

const toolbarConfig = {
  toolbarKeys: [
    'uploadImage',
    'color',
    'bold',
    'underline',
    'through',
    'italic',
    'undo',
    'redo',
  ]
}

const initPostData = () => {
  // 优先处理直接传递的帖子数据
  const postData = route.query.post
  if (postData) {
    const post = JSON.parse(postData as string)
    // 处理标签字段，确保它是数组格式
    let processedTags: string[] = [];
    if (post.tags) {
      if (Array.isArray(post.tags)) {
        processedTags = post.tags;
      } else if (typeof post.tags === 'string') {
        try {
          processedTags = JSON.parse(post.tags) || [];
        } catch (e) {
          console.warn('解析标签失败:', e);
          processedTags = [];
        }
      }
    }

    postForm.value = {
      title: post.title || '',
      category: post.category,
      tags: processedTags,
      coverUrl: post.coverUrl || '',
      content: post.content || ''
    }
    if (editorRef.value) editorRef.value.setHtml(post.content || '')
  } else {
    // 尝试处理草稿数据
    const draftData = route.query.draft;
    if (draftData) {
      const draft = typeof draftData === 'string' ? JSON.parse(draftData) : draftData;
      // 处理标签字段，确保它是数组格式
      let processedTags: string[] = [];
      if (draft.tags) {
        if (Array.isArray(draft.tags)) {
          processedTags = draft.tags;
        } else if (typeof draft.tags === 'string') {
          try {
            processedTags = JSON.parse(draft.tags) || [];
          } catch (e) {
            console.warn('解析标签失败:', e);
            processedTags = [];
          }
        }
      }

      postForm.value = {
        title: draft.title || '',
        category: draft.category,
        tags: processedTags,
        coverUrl: draft.coverUrl || '',
        content: draft.content || ''
      };
      // 如果是草稿，设置草稿ID
      if (draft.id) {
        draftId = String(draft.id);
      }
      if (editorRef.value && draft.content) {
        editorRef.value.setHtml(draft.content);
      }
      showToastMessage('草稿内容已加载', 'success');
    } else if (isEdit.value) {
      showToastMessage('获取数据失败', 'error');
      router.back();
    }
  }
}

const handleCreated = (editor: any) => {
  editorRef.value = editor

  // 获取编辑器容器
  const editorDom = editor.getEditableContainer()

  if (editorDom) {
    // 添加粘贴事件监听器，处理超长内容提示
    editorDom.addEventListener('paste', (e: ClipboardEvent) => {
      // 稍微延迟执行，让编辑器先处理粘贴内容
      setTimeout(() => {
        const content = editor.getText() || '';
        if (content.length > 5000) {
          // 截取前5000个字符
          const truncatedContent = content.substring(0, 5000);
          // 设置截取后的内容
          editor.setHtml(truncatedContent.replace(/\n/g, '<br>'));
          showToastMessage('内容已达到最大长度限制(5000字符)，已自动截取', 'warning');
        }
      }, 10);
    });

    // 监听编辑器内容变化，更新postForm.content
    editor.on('onChange', () => {
      postForm.value.content = editor.getHtml();
    });
  }

  if (/mobile/i.test(navigator.userAgent)) {
    if (editorDom) {
      editorDom.style.setProperty('-webkit-user-select', 'text')
      editorDom.style.setProperty('user-select', 'text')

      // 添加触摸事件监听器，防止默认行为影响图片缩放
      editorDom.addEventListener('touchstart', (e) => {
        // 如果点击的是图片，不阻止默认行为，允许原生缩放
        if ((e.target as HTMLElement).tagName === 'IMG') {
          return
        }
        setTimeout(() => editor.focus(), 0)
      }, { passive: false })

      // 防止编辑器区域的触摸滚动冲突
      editorDom.addEventListener('touchmove', (e) => {
        // 如果触摸目标是图片，不阻止默认行为
        if ((e.target as HTMLElement).tagName === 'IMG') {
          e.stopPropagation()
        }
      }, { passive: false })
    }
  }
  if (isEdit.value) initPostData()
}

const categories = ref([])
const tags = ref([])

const fetchTags = async () => {
  try {
    const res = await listPostTagCategoryUsingGet()
    if (res.data?.code === 0 && res.data.data) {
      tags.value = res.data.data.tagList
      categories.value = res.data.data.categoryList
      filteredCategories.value = [...categories.value]
      filteredTags.value = [...tags.value]
    }
  } catch (error) {
    showToastMessage('获取标签失败', 'error');
  }
}

const filterCategories = () => {
  const searchText = categorySearchText.value.trim().toLowerCase()
  if (!searchText) {
    filteredCategories.value = [...categories.value]
    return
  }

  filteredCategories.value = categories.value.filter(item =>
    item.toLowerCase().includes(searchText)
  )
}

const clearCategorySearch = () => {
  categorySearchText.value = ''
  filteredCategories.value = [...categories.value]
}

const filterTags = () => {
  const searchText = tagSearchText.value.trim().toLowerCase()
  if (!searchText) {
    filteredTags.value = [...tags.value]
    return
  }

  filteredTags.value = tags.value.filter(item =>
    item.toLowerCase().includes(searchText)
  )
}

const clearTagSearch = () => {
  tagSearchText.value = ''
  filteredTags.value = [...tags.value]
}

const addCustomTag = () => {
  const val = customTagInput.value.trim()
  if (val && !postForm.value.tags.includes(val)) {
    postForm.value.tags.push(val)
    customTagInput.value = ''
  }
}

const loadLatestDraft = async () => {
  try {
    const res = await getPostLatestDraftUsingGet()
    if (res.data?.code === 0 && res.data.data) {
      const draft = res.data.data
      showDraftConfirm(() => {
        // 处理标签字段，确保它是数组格式
        let processedTags: string[] = [];
        if (draft.tags) {
          if (Array.isArray(draft.tags)) {
            processedTags = draft.tags;
          } else if (typeof draft.tags === 'string') {
            try {
              processedTags = JSON.parse(draft.tags) || [];
            } catch (e) {
              console.warn('解析标签失败:', e);
              processedTags = [];
            }
          }
        }

        postForm.value = {
          title: draft.title || '',
          category: draft.category,
          tags: processedTags,
          coverUrl: draft.coverUrl || '',
          content: draft.content || ''
        }
        draftId = String(draft.id);
        if (editorRef.value && draft.content) {
          editorRef.value.setHtml(draft.content)
        }
        showToastMessage('草稿内容已加载', 'success');
      })
    }
  } catch (error) {}
}

const clearDraftContent = () => {
  showClearConfirm();
}

onMounted(async () => {
  checkDevice()
  window.addEventListener('resize', checkDevice)
  // 添加 beforeunload 事件监听器
  window.addEventListener('beforeunload', beforeUnloadHandler)
  await fetchTags()
  if (!isEdit.value && !route.query.post) {
    await loadLatestDraft()
  }

  // 在组件挂载后设置离开确认
  unregisterGuard = router.beforeEach((to, from, next) => {
    // 如果弹窗正在显示，允许路由变化以避免冲突
    if (showLeaveConfirmModal.value) {
      next();
      return;
    }

    // 检查是否是从编辑页面跳转到另一个编辑页面
    const isFromEditPage = from.path.startsWith('/post/edit');
    const isToEditPage = to.path.startsWith('/post/edit');

    if (hasUnsavedChanges.value && !isEdit.value && to.path !== from.path && !(isFromEditPage && isToEditPage)) {
      // 如果有未保存的更改，且当前不在编辑模式，且不是在编辑页面间跳转，则显示确认弹窗
      showLeaveConfirmModal.value = true
      next(false) // 阻止导航
    } else {
      next() // 允许导航
    }
  });
});

// 在 mounted 之后添加路由守卫
let unregisterGuard: Function | null = null;

onBeforeUnmount(() => {
  window.removeEventListener('resize', checkDevice)
  const editor = editorRef.value;
  if (editor) editor.destroy()
  // 移除 beforeunload 事件监听器
  window.removeEventListener('beforeunload', beforeUnloadHandler)
  // 移除路由守卫
  if (unregisterGuard) {
    unregisterGuard();
  }
})

let draftId: string | null = null

const isEdit = computed(() => !!route.params.id)
const postForm = ref({
  title: '',
  content: '',
  category: undefined,
  tags: [] as string[],
  coverUrl: ''
})

// 从内容中提取图片的方法
const extractImagesFromContent = (): string[] => {
  if (!postForm.value.content) return [];

  // 解析HTML内容
  const parser = new DOMParser();
  const doc = parser.parseFromString(postForm.value.content, 'text/html');

  // 查找所有img标签
  const imgElements = doc.querySelectorAll('img');
  const imageUrls: string[] = [];

  imgElements.forEach(img => {
    const src = img.getAttribute('src');
    if (src) {
      imageUrls.push(src);
    }
  });

  return imageUrls;
};

// 是否显示从内容选择封面的弹窗
const showSelectCoverFromContent = ref(false);

// 检测表单内容是否发生变化
const initialFormData = ref({
  title: '',
  content: '',
  category: undefined,
  tags: [] as string[],
  coverUrl: ''
})

const hasUnsavedChanges = computed(() => {
  return (
    postForm.value.title !== initialFormData.value.title ||
    postForm.value.content !== initialFormData.value.content ||
    postForm.value.category !== initialFormData.value.category ||
    JSON.stringify(postForm.value.tags) !== JSON.stringify(initialFormData.value.tags) ||
    postForm.value.coverUrl !== initialFormData.value.coverUrl
  )
})

const previewContent = computed(() => {
  return postForm.value.content
})



const triggerCoverUpload = () => {
  if (coverFileInput.value) {
    coverFileInput.value.click()
  }
}

const handleCoverUpload = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  if (!file.type.startsWith('image/')) {
    showToastMessage('请选择图片文件', 'warning');
    return
  }

  try {
    uploading.value = true
    uploadProgress.value = 10

    const [compressedFile] = await compressImages([file])
    uploadProgress.value = 40

    const formData = new FormData()
    formData.append('file', compressedFile)

    const res = await uploadPostImageUsingPost(
      {},
      { headers: { 'Content-Type': 'multipart/form-data' } },
      compressedFile
    )

    uploadProgress.value = 90

    if (res.data.code === 0 && res.data.data) {
      postForm.value.coverUrl = res.data.data.url
      showToastMessage('封面图上传成功', 'success');
      uploadProgress.value = 100
    } else {
      throw new Error(res.data.message || '上传失败');
    }
  } catch (error: any) {
    showToastMessage('封面图上传失败: ' + (error.message || '未知错误'), 'error');
  } finally {
    uploading.value = false
    uploadProgress.value = 0
    if (target) {
      target.value = ''
    }
  }
}

const toggleSelect = (type: string) => {
  Object.keys(selectOpen.value).forEach(key => {
    if (key !== type) {
      selectOpen.value[key as keyof typeof selectOpen.value] = false
    }
  })
  selectOpen.value[type as keyof typeof selectOpen.value] = !selectOpen.value[type as keyof typeof selectOpen.value]
}

const selectCategory = (category: string) => {
  postForm.value.category = category
}

const toggleTag = (tag: string) => {
  const index = postForm.value.tags.indexOf(tag)
  if (index > -1) {
    postForm.value.tags.splice(index, 1)
  } else {
    if (postForm.value.tags.length < 3) {
      postForm.value.tags.push(tag)
    } else {
      showToastMessage('最多只能选择3个标签', 'warning');
    }
  }
}

const removeTag = (index: number) => {
  postForm.value.tags.splice(index, 1)
}

// 是否显示清除封面图确认弹窗
const showClearCoverConfirm = ref(false);

// 清除封面图
const clearCoverImage = () => {
  showClearCoverConfirm.value = true;
};

// 确认清空封面图
const confirmClearCover = () => {
  postForm.value.coverUrl = ''
  showToastMessage('封面图已清除', 'info');
  // 封面图改变后，也需要更新初始数据，避免误判为未保存内容
  updateInitialFormData()
  showClearCoverConfirm.value = false;
};

// 取消清除封面图
const cancelClearCover = () => {
  showClearCoverConfirm.value = false;
};

const showConfirm = (title: string, content: string, onConfirm: Function) => {
  confirmConfig.value = {
    title,
    content,
    onConfirm,
    onCancel: () => {
      showConfirmModal.value = false
    }
  }
  showConfirmModal.value = true
}

const showDraftConfirm = (callback: Function) => {
  draftLoadCallback.value = callback
  showDraftConfirmModal.value = true
}

const confirmDraftLoad = () => {
  draftLoadCallback.value()
  showDraftConfirmModal.value = false
}

const cancelDraftLoad = () => {
  showDraftConfirmModal.value = false
  // 用户选择不加载草稿，可以在这里添加其他逻辑，比如清空当前内容
}

const cancelClearContent = () => {
  showClearConfirmModal.value = false
}

const handleCancel = () => {
  // 更新初始数据以匹配当前表单状态，这样就不会触发离开确认弹窗
  updateInitialFormData()
  // 然后执行返回操作
  router.back()
}

const confirmClearContent = () => {
  clearingContent.value = true;
  try {
    postForm.value = {
      title: '',
      content: '',
      category: undefined,
      tags: [],
      coverUrl: '',
    }
    if (editorRef.value) {
      editorRef.value.setHtml('')
    }
    showToastMessage('内容已清空', 'success');
    showClearConfirmModal.value = false
    // 清空内容后，更新初始数据以避免离开确认弹窗
    updateInitialFormData()
  } finally {
    clearingContent.value = false;
  }
}

const showClearConfirm = () => {
  showClearConfirmModal.value = true
}

// 移动端步骤导航函数
const goToStep = (step: number) => {
  if (step === 1) {
    // 总是可以返回第一页
    currentStep.value = step
  } else if (step === 2) {
    // 检查是否满足进入第二步的条件
    if (!postForm.value.title.trim()) {
      showToastMessage('请先填写标题', 'warning')
      return
    }
    // 检查富文本编辑器的实际内容（去除HTML标签后）
    const contentWithoutHtml = postForm.value.content.replace(/<[^>]*>/g, '').replace(/\s+/g, '').trim();
    if (!contentWithoutHtml) {
      showToastMessage('请先填写内容', 'warning')
      return
    }
    currentStep.value = step
  }
}

// 移动端下一步函数
const goToNextStep = () => {
  if (!postForm.value.title.trim()) {
    showToastMessage('请先填写标题', 'warning')
    return
  }
  // 检查富文本编辑器的实际内容（去除HTML标签后）
  const contentWithoutHtml = postForm.value.content.replace(/<[^>]*>/g, '').replace(/\s+/g, '').trim();
  if (!contentWithoutHtml) {
    showToastMessage('请先填写内容', 'warning')
    return
  }
  currentStep.value = 2
}

// 自动保存草稿功能
const showLeaveConfirmModal = ref(false)

let isLeavingPage = false; // 标记是否正在离开页面

const beforeUnloadHandler = (e: BeforeUnloadEvent) => {
  if (hasUnsavedChanges.value && !isEdit.value && !showLeaveConfirmModal.value && !isLeavingPage) {
    e.preventDefault()
    e.returnValue = '' // 触发浏览器默认的确认对话框
    return ''
  }
}

const confirmLeaveWithoutSave = () => {
  showLeaveConfirmModal.value = false
  // 设置离开页面标记，防止beforeunload事件触发
  isLeavingPage = true
  // 允许离开页面
  window.removeEventListener('beforeunload', beforeUnloadHandler)
  // 注销路由守卫
  if (unregisterGuard) {
    unregisterGuard();
  }
  router.go(-1) // 或者 router.push('/previous-page')
}

const cancelLeave = () => {
  showLeaveConfirmModal.value = false
  // 重置离开页面标记
  isLeavingPage = false
  // 更新初始数据以匹配当前表单状态，这样用户可以继续编辑而不立即再次触发提示
  updateInitialFormData()
}

const saveDraftAndLeave = async () => {
  await saveDraft()
  showLeaveConfirmModal.value = false
  // 设置离开页面标记，防止beforeunload事件触发
  isLeavingPage = true
  // 保存完成后更新初始数据，这样就不会再检测到内容变化
  updateInitialFormData()
  // 保存完成后直接离开，不再检查内容变化
  window.removeEventListener('beforeunload', beforeUnloadHandler)
  if (unregisterGuard) {
    unregisterGuard();
  }
  router.go(-1)
}

// 监听表单变化，更新初始数据
watch(postForm, (newVal) => {
  if (!initialFormData.value.title && !initialFormData.value.content) {
    // 如果初始数据还没有设置，则设置它
    initialFormData.value = {
      title: newVal.title,
      content: newVal.content,
      category: newVal.category,
      tags: [...newVal.tags],
      coverUrl: newVal.coverUrl
    }
  }
}, { deep: true })

// 从内容中选择封面图
const selectCoverFromContent = () => {
  const images = extractImagesFromContent();
  if (images.length > 0) {
    showSelectCoverFromContent.value = true;
  } else {
    showToastMessage('内容中未找到可用图片', 'warning');
  }
};

// 使用选中的图片作为封面
const useImageAsCover = (imageUrl: string) => {
  postForm.value.coverUrl = imageUrl;
  showSelectCoverFromContent.value = false;
  showToastMessage('封面图已更新', 'success');
};



// 更新初始数据，用于在用户决定继续编辑时
const updateInitialFormData = () => {
  initialFormData.value = {
    title: postForm.value.title,
    content: postForm.value.content,
    category: postForm.value.category,
    tags: [...postForm.value.tags],
    coverUrl: postForm.value.coverUrl
  }
}

const showToastMessage = (message: string, type: 'success' | 'error' | 'warning' = 'success') => {
  toastMessage.value = message
  toastType.value = type
  showToast.value = true

  setTimeout(() => {
    showToast.value = false
  }, 3000)
}

const saveDraft = async () => {
  if (!postForm.value.title?.trim()) {
    showToastMessage('标题不能为空', 'warning');
    return
  }
  // 检查富文本编辑器的实际内容（去除HTML标签后）
  const contentWithoutHtml = postForm.value.content.replace(/<[^>]*>/g, '').replace(/\s+/g, '').trim();
  if (!contentWithoutHtml) {
    showToastMessage('内容不能为空', 'warning');
    return
  }

  if (postForm.value.content.length > 5000) {
    showToastMessage('内容不能超过5000字', 'warning');
    return;
  }

  submitting.value = true
  try {
    const submitData = {
      ...postForm.value,
      content: postForm.value.content,
      tags: postForm.value.tags || []
    }
    // 如果存在草稿ID，则添加到提交数据中，以便后端更新现有草稿
    if (draftId) {
      submitData.id = draftId
    }
    await savePostDraftUsingPost(submitData)
    showToastMessage('草稿保存成功', 'success');
    // 保存成功后，更新初始数据以避免离开确认弹窗
    updateInitialFormData()
  } catch (error: any) {
    showToastMessage('草稿保存失败：' + (error.message || '未知错误'), 'error');
  } finally {
    submitting.value = false
  }
}

const handleSubmit = async () => {
  if (!postForm.value.title?.trim()) {
    showToastMessage('请输入标题', 'warning');
    return;
  }
  if (!postForm.value.category) {
    showToastMessage('请选择分类', 'warning');
    return;
  }

  // 检查富文本编辑器的实际内容（去除HTML标签后）
  const contentWithoutHtml = postForm.value.content.replace(/<[^>]*>/g, '').replace(/\s+/g, '').trim();
  if (!contentWithoutHtml) {
    showToastMessage('请输入内容', 'warning');
    return;
  }

  if (postForm.value.content.length > 5000) {
    showToastMessage('内容不能超过5000字', 'warning');
    return;
  }

  submitting.value = true
  try {
    const submitData = {
      ...postForm.value,
      content: postForm.value.content,
      tags: postForm.value.tags || []
    }
    if (isEdit.value) {
      // 即使是编辑模式也使用新增接口，但仍需传递原始ID
      submitData.id = String(route.params.id)
      await addPostUsingPost(submitData)
      showToastMessage('发布成功', 'success');
    } else {
      if (draftId) {
        submitData.id = draftId
      } else if (route.params.id) {
        submitData.id = String(route.params.id)
      }
      await addPostUsingPost(submitData)
      showToastMessage('发布成功，等待审核', 'success');
      // 发布成功后，重置草稿ID，避免后续操作仍针对原草稿
      draftId = null
    }
    // 发布成功后，更新初始数据以避免离开确认弹窗
    updateInitialFormData()
    router.push('/forum')
  } catch (error: any) {
    showToastMessage((isEdit.value ? '更新' : '发布') + '失败：' + (error.message || '未知错误'), 'error');
  } finally {
    submitting.value = false
  }
}

const handlePublishClick = () => {
  if (!postForm.value.title.trim()) {
    showToastMessage('请输入标题', 'warning');
    return;
  }
  // 检查富文本编辑器的实际内容（去除HTML标签后）
  const contentWithoutHtml = postForm.value.content.replace(/<[^>]*>/g, '').replace(/\s+/g, '').trim();
  if (!contentWithoutHtml) {
    showToastMessage('请输入内容', 'warning');
    return;
  }
  if (!postForm.value.category) {
    showToastMessage('请选择分类', 'warning');
    return;
  }

  // 检查内容长度
  if (postForm.value.content.length > 5000) {
    showToastMessage('内容不能超过5000字', 'warning');
    return;
  }

  // 所有条件满足，执行提交
  handleSubmit();
}

const handleCategorySelect = (category: string) => {
  selectCategory(category)
}
</script>

<style scoped>
.select-btn {
  position: relative;
  width: 100%;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--card-background);
  color: var(--text-primary);
  padding: 12px 16px;
  cursor: pointer;
  transition: var(--theme-transition);
  box-sizing: border-box;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.select-btn:hover {
  border-color: var(--nav-item-active-text);
  background: var(--hover-background);
}

.select-display {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.select-icon {
  font-size: 12px;
  color: var(--text-secondary);
  transition: var(--theme-transition);
}

.select-dropdown,
.select-wrapper {
  display: none;
}

.select-modal {
  max-width: 400px;
}


.select-modal .select-options-container {
  overflow-y: auto;
  margin: 8px 0;
}


.select-modal .select-option {
  padding: 12px 16px;
  cursor: pointer;
  transition: var(--theme-transition);
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid var(--border-color);
}

.select-modal .select-option:last-child {
  border-bottom: none;
}

.select-modal .select-option:hover {
  background: var(--hover-background);
}

.select-modal .select-option.selected {
  color: var(--nav-item-active-text);
  background: var(--nav-item-active);
}

.selected-tags-hint {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--border-color);
}

.hint-text {
  font-size: 12px;
  color: var(--text-secondary);
  margin-bottom: 8px;
  display: block;
}

.selected-tags-preview {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.tag-item.mini {
  padding: 2px 6px;
  font-size: 12px;
}

.custom-tag-input {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--border-color);
}

.tag-input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid var(--border-color);
  border-radius: 4px;
  background: var(--card-background);
  color: var(--text-primary);
}

.search-input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  padding: 8px;
  border-bottom: 1px solid var(--border-color);
}

.modal-search-input {
  width: 100%;
  padding: 8px 30px 8px 30px;
  border: 1px solid var(--border-color);
  border-radius: 4px;
  background: var(--card-background);
  color: var(--text-primary);
  font-size: 14px;
}

.search-icon {
  position: absolute;
  left: 16px;
  color: var(--text-secondary);
  font-size: 14px;
  z-index: 1;
}

.search-clear-btn {
  position: absolute;
  right: 10px;
  background: none;
  border: none;
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 14px;
  padding: 4px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1;
}

.search-clear-btn:hover {
  background: var(--hover-background);
  color: var(--text-primary);
}

.select-options-container {
  overflow-y: auto;
}

.no-result {
  padding: 12px 16px;
  text-align: center;
  color: var(--text-secondary);
  font-size: 14px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.no-result i {
  font-size: 16px;
}

.box {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: var(--background);
  z-index: 100;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: #888 var(--card-background);

  &::-webkit-scrollbar {
    width: 6px;
    height: 6px;
  }

  &::-webkit-scrollbar-track {
    background: var(--card-background);
    border-radius: 3px;
  }

  &::-webkit-scrollbar-thumb {
    background-color: #b0b0b0;
    border-radius: 3px;
    transition: background-color 0.2s ease;
  }

  &::-webkit-scrollbar-thumb:hover {
    background-color: #888;
  }

  &::-webkit-scrollbar-thumb:active {
    background-color: #666;
  }
}

.editor-container {
  width: 100%;
  max-width: 1440px;
  margin: 0 auto;
  padding: 16px 6px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-color);
}

.page-header h1 {
  font-size: 22px;
  font-weight: 600;
  margin: 0;
  color: var(--text-primary);
}

.tooltip-container {
  position: relative;
  cursor: help;
}

.tooltip-icon {
  font-size: 18px;
  color: var(--text-secondary);
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.tooltip-content {
  position: absolute;
  right: 0;
  top: calc(100% + 8px);
  background: var(--card-background);
  color: var(--text-primary);
  padding: 8px 12px;
  border-radius: 8px;
  border: 1px solid var(--border-color);
  font-size: 14px;
  white-space: nowrap;
  z-index: 100;
  opacity: 0;
  visibility: hidden;
  transition: var(--theme-transition);
}

.tooltip-container:hover .tooltip-content {
  opacity: 1;
  visibility: visible;
}

.pc-layout {
  display: flex;
  gap: 24px;
  flex: 1;
  height: calc(100% - 60px);
  overflow: hidden;
}

.pc-left {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
  overflow: hidden;
}

.pc-right {
  flex-shrink: 0;
  width: 360px;
  overflow-y: auto;
  max-height: 100%;
}

.config-panel {
  background: var(--card-background);
  border-radius: 8px;
  padding: 20px;
  border: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.mobile-layout {
  display: flex;
  flex-direction: column;
  flex: 1;
  overflow: hidden;
  position: relative;
  height: 100%;
}

.mobile-step-nav {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-color);
}

.step-item {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 16px;
  border-radius: 16px;
  transition: var(--theme-transition);
}

.step-item.active {
  background: var(--nav-item-active);
  color: var(--nav-item-active-text);
}

.step-num {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--hover-background);
  color: var(--text-primary);
  font-size: 14px;
}

.step-item.active .step-num {
  background: var(--nav-item-active-text);
  color: var(--text-other);
}

.step-text {
  font-size: 15px;
  color: var(--text-primary);
}

.step-divider {
  color: var(--text-secondary);
  margin: 0 8px;
}

.mobile-step-content {
  display: flex;
  flex-direction: column;
  flex: 1;
  overflow-y: auto;
  padding-bottom: 44px;
}

.form-item {
  width: 100%;
}

.form-item.required .form-label::after,
.form-item.required .input-wrapper::before {
  content: '*';
  color: var(--nav-item-active-text);
}

.form-item.required .input-wrapper::before {
  position: absolute;
  left: 8px;
  top: 50%;
  transform: translateY(-50%);
}

.form-label {
  display: block;
  margin: 12px 0;
  font-size: 15px;
  color: var(--text-primary);
  font-weight: 500;
}

.input-wrapper {
  position: relative;
  width: 100%;
}

.title-input {
  width: 100%;
  font-size: 16px;
  padding: 12px;
  border-radius: 8px;
  border: 1px solid var(--border-color);
  background: var(--card-background);
  color: var(--text-primary);
  box-sizing: border-box;
  transition: var(--theme-transition);
  outline: none;
}

.title-input:focus {
  border-color: var(--nav-item-active-text);
}

.title-input::placeholder {
  color: var(--text-secondary);
}

.char-count {
  position: absolute;
  right: 12px;
  bottom: 8px;
  font-size: 12px;
  color: var(--text-secondary);
}

.tags-wrapper {
  width: 100%;
}

.tags-selected {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}

.tag-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 8px;
  background: var(--nav-item-active);
  color: var(--nav-item-active-text);
  border-radius: 4px;
  font-size: 14px;
}

.tag-item i {
  font-size: 12px;
  cursor: pointer;
}

.editor-form-item {
  flex: 1;
  margin-bottom: 0;
  display: flex;
  flex-direction: column;
}

.editor-wrapper {
  width: 100%;
  border-radius: 8px;
  border: 1px solid var(--border-color);
  overflow: hidden;
  background: var(--card-background);
  flex: 1;
  display: flex;
  flex-direction: column;
  margin-top: 6px;
}

:deep(.w-e-toolbar) {
  border-bottom: 1px solid var(--border-color);
  background: var(--card-background);
  padding: 8px 4px;
  transition: var(--theme-transition);
}

:deep(.w-e-menu) {
  color: var(--text-primary);
  transition: var(--theme-transition);
}

:deep(.w-e-menu:hover) {
  background: var(--hover-background);
}

:deep(.w-e-text-container) {
  background: var(--card-background);
  flex: 1;
  min-height: 300px;
  color: var(--text-primary);
  padding: 4px;
  margin: 0;
  height: calc(100vh - 250px) !important;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: #888 var(--card-background);

  &::-webkit-scrollbar {
    width: 6px;
    height: 6px;
  }

  &::-webkit-scrollbar-track {
    background: var(--card-background);
    border-radius: 3px;
  }

  &::-webkit-scrollbar-thumb {
    background-color: #b0b0b0;
    border-radius: 3px;
    transition: background-color 0.2s ease;
  }

  &::-webkit-scrollbar-thumb:hover {
    background-color: #888;
  }

  &::-webkit-scrollbar-thumb:active {
    background-color: #666;
  }

  /* 图片容器基础样式 - 修复光标问题 */
  .w-e-image-container {
    display: inline-block !important;
    width: 33.33% !important;
    height: auto !important;
    padding: 0 !important;
    margin: 4px 4px !important;
    aspect-ratio: 1 !important;
    overflow: hidden !important;
    vertical-align: top !important;
    line-height: normal !important;
  }

  /* 图片基础样式 */
  .w-e-image-container img {
    width: 100% !important;
    height: 100% !important;
    object-fit: cover !important;
    border-radius: 8px !important;
    box-shadow: 0 2px 8px rgba(0,0,0,0.1) !important;
    vertical-align: top !important;
  }

  /* 确保图片后能正常插入光标 */
  .w-e-text-container [data-slate-node="element"] {
    position: relative;
  }
}

.cover-image-section {
  padding-top: 8px;
  width: 100%;
}

.cover-upload-area {
  border: 2px dashed var(--border-color);
  border-radius: 12px;
  padding: 32px 16px;
  text-align: center;
  cursor: pointer;
  transition: var(--theme-transition);
  background: var(--hover-background);
}

.cover-upload-area:hover {
  border-color: var(--nav-item-active-text);
  background: var(--card-background);
}

.cover-preview {
  width: 100%;
  border-radius: 8px;
  overflow: hidden;
  position: relative;
}

.cover-image {
  width: 100%;
  height: 200px;
  object-fit: cover;
}

.cover-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-other);
  font-size: 14px;
  opacity: 0;
  transition: var(--theme-transition);
}

.upload-progress-icon {
  position: absolute;
  top: 8px;
  right: 8px;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  background: var(--card-background);
  border-radius: 50%;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.upload-progress-icon i {
  font-size: 12px;
  color: var(--text-primary);
}

.upload-progress-icon i.upload-complete {
  color: #28a745;
}

.cover-preview:hover .cover-overlay {
  opacity: 1;
}

.cover-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.placeholder-icon {
  font-size: 32px;
  color: var(--text-secondary);
}

.placeholder-text {
  font-size: 15px;
  color: var(--text-secondary);
}

.clear-cover-btn {
  height: 40px;
  background: #dc2626;
  border: none;
  color: white;
  cursor: pointer;
  font-size: 14px;
  padding: 6px 12px;
  border-radius: 6px;
  transition: var(--theme-transition);
}

.clear-cover-btn:hover {
  background: #ef4444;
}

.cover-action-buttons {
  display: flex;
  gap: 8px;
  margin-top: 8px;
  flex-wrap: wrap;
}

.action-btn.secondary-btn {
  flex: 1;
  height: 40px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  border: 1px solid var(--border-color);
  cursor: pointer;
  transition: var(--theme-transition);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-width: 120px;
  background: var(--card-background);
  color: var(--text-primary);
}

.action-btn.secondary-btn.full-width {
  width: 100%;
  margin-bottom: 8px;
}

.action-btn.secondary-btn:hover {
  background: var(--hover-background);
}

.select-cover-modal {
  max-width: 900px;
  width: 90%;
  max-height: 80vh;
  height: 70vh;
}

.cover-images-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 12px;
  overflow-y: auto;
  padding: 8px;
  padding-top: 16px;
}

.cover-image-item {
  position: relative;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  aspect-ratio: 1/1;
  border: 2px solid transparent;
  transition: var(--theme-transition);
}

.cover-image-item:hover {
  border-color: var(--nav-item-active-text);
}

.cover-preview-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-image-item .cover-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 14px;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.cover-image-item:hover .cover-overlay {
  opacity: 1;
}

.no-images-message {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: var(--text-secondary);
  text-align: center;
}

.no-images-message i {
  font-size: 24px;
  margin-bottom: 8px;
}

.clear-cover-btn:hover {
  color: var(--nav-item-active-text);
}

.file-input {
  display: none;
}

.form-actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
  width: 100%;
  flex-wrap: wrap;
}

.action-btn {
  flex: 1;
  height: 44px;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 500;
  border: 1px solid var(--border-color);
  cursor: pointer;
  transition: var(--theme-transition);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-width: 80px;
}

.action-row {
  display: flex;
  gap: 8px;
  width: 100%;
  margin-bottom: 8px;
}

.action-row .action-btn {
  flex: 1 1 calc(50% - 4px);
  min-width: calc(50% - 4px);
  max-width: calc(50% - 4px);
}

/* 移动端专属：允许 Step 2 的四个按钮换行为 2x2 布局 */
.action-row.mobile-two-column {
  flex-wrap: wrap;
}

.action-row.mobile-two-column .action-btn {
  flex: 1 1 calc(50% - 4px);
  min-width: calc(50% - 4px);
  max-width: calc(50% - 4px);
}

.action-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.cancel-btn, .prev-btn, .clear-btn, .draft-btn {
  background: var(--card-background);
  color: var(--text-primary);
}

.cancel-btn:hover, .prev-btn:hover, .clear-btn:hover, .draft-btn:hover {
  background: var(--hover-background);
}

.next-btn, .submit-btn {
  background: var(--nav-item-active-text);
  color: var(--text-other);
  border-color: var(--nav-item-active-text);
}

.next-btn:hover, .submit-btn:hover {
  opacity: 0.9;
}

.action-btn.loading {
  pointer-events: none;
  opacity: 0.8;
}

.action-btn.loading i {
  animation: spin 1s linear infinite;
}

.action-btn.loading span {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.mobile-step-actions {
  display: flex !important;
  gap: 8px !important;
  width: 100% !important;
  position: fixed !important;
  bottom: 0 !important;
  left: 0 !important;
  background: var(--background) !important;
  padding: 8px 16px !important;
  z-index: 999 !important;
  border-top: 1px solid var(--border-color) !important;
  box-sizing: border-box !important;
  margin: 0 !important;
}

.mobile-step-content:nth-child(2) .form-actions {
  position: fixed !important;
  bottom: 0 !important;
  left: 0 !important;
  width: 100% !important;
  background: var(--background) !important;
  padding: 8px 16px !important;
  border-top: 1px solid var(--border-color) !important;
  box-sizing: border-box !important;
  z-index: 999 !important;
  margin: 0 !important;
  display: flex !important;
  flex-wrap: wrap !important;
  gap: 8px !important;
}

.mobile-step-content:nth-child(2) .action-btn {
  flex: 1 1 calc(50% - 4px) !important;
  min-width: calc(50% - 4px) !important;
  max-width: calc(50% - 4px) !important;
}



.upload-progress {
  margin-top: 12px;
  margin-bottom: 70px;
}

.progress-bar {
  width: 100%;
  height: 6px;
  background: var(--border-color);
  border-radius: 3px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: var(--nav-item-active-text);
  transition: width 0.3s ease;
}

.progress-text {
  text-align: center;
  font-size: 13px;
  color: var(--text-secondary);
  margin-top: 4px;
}

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
  background: var(--card-background);
  border-radius: 8px;
  border: 1px solid var(--border-color);
  width: 90%;
  height: 80vh;
  display: flex;
  flex-direction: column;
}

.draft-confirm-modal {
  height: auto !important;
  max-height: 300px;
  min-height: 200px;
  width: 80% !important;
  max-width: 400px;
}

.clear-confirm-modal {
  height: auto !important;
  max-height: 300px;
  min-height: 200px;
  width: 80% !important;
  max-width: 400px;
}

.clear-cover-confirm-modal {
  height: auto !important;
  max-height: 300px;
  min-height: 200px;
  width: 80% !important;
  max-width: 400px;
}

.leave-confirm-modal {
  height: auto !important;
  max-height: 400px;
  min-height: 250px;
  width: 85% !important;
  max-width: 450px;
}

.leave-confirm-modal .modal-footer {
  display: flex !important;
  flex-direction: column !important;
  gap: 8px !important;
}

.leave-confirm-modal .modal-footer .modal-btn {
  margin: 0 !important;
  width: 100% !important;
}

.modal-header {
  padding: 16px;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
  color: var(--text-primary);
}

.modal-close {
  background: none;
  border: none;
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 16px;
}

.modal-body {
  overflow-y: auto;
  flex: 1;
}

.modal-footer {
  padding: 4px;
  border-top: 1px solid var(--border-color);
  display: flex;
  justify-content: flex-end;
  gap: 32px;
  margin: auto;
}

.modal-btn {
  padding: 8px 16px;
  border-radius: 4px;
  border: 1px solid var(--border-color);
  background: var(--card-background);
  color: var(--text-primary);
  cursor: pointer;
  transition: var(--theme-transition);
}

.modal-btn.loading {
  pointer-events: none;
  opacity: 0.8;
}

.modal-btn.loading i {
  animation: spin 1s linear infinite;
}

.modal-btn.loading span {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.modal-btn.confirm {
  background: var(--nav-item-active-text);
  color: var(--text-other);
  border-color: var(--nav-item-active-text);
}

.modal-btn.danger {
  background: #ef4444;
  color: var(--text-other);
  border-color: #ef4444;
}

.modal-btn.danger:hover {
  background: #dc2626;
  border-color: #dc2626;
}

.modal-btn:hover {
  opacity: 0.9;
}

.attachment-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  gap: 8px;
}

.attachment-item {
  position: relative;
  border-radius: 4px;
  border: 1px solid var(--border-color);
  overflow: hidden;
  cursor: pointer;
}

.attachment-image {
  width: 100%;
  height: 100px;
  object-fit: cover;
}

.attachment-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  opacity: 0;
  transition: var(--theme-transition);
}

.attachment-item:hover .attachment-overlay {
  opacity: 1;
}

.toast {
  position: fixed;
  top: 20px;
  left: 50%;
  transform: translateX(-50%) translateY(-100px);
  background: var(--card-background);
  color: var(--text-primary);
  padding: 12px 20px;
  border-radius: 8px;
  border: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  gap: 8px;
  z-index: 1000;
  opacity: 0;
  transition: var(--theme-transition);
}

.toast.show {
  transform: translateX(-50%) translateY(0);
  opacity: 1;
}

.toast i {
  font-size: 16px;
}

.toast i.fa-check-circle {
  color: #22c55e;
}

.toast i.fa-exclamation-circle {
  color: var(--nav-item-active-text);
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.mobile-step-nav .step-item.disabled {
  opacity: 0.5;
  cursor: not-allowed;
  pointer-events: none;
}

.mobile-step-nav {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-color);
  position: relative;
}

.editor-upload-progress {
  position: absolute;
  top: 0;
  right: 0;
  width: 24px;
  height: 24px;
  z-index: 10;
  background: var(--card-background);
  border-radius: 50%;
  padding: 2px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.editor-upload-progress .progress-svg {
  width: 100%;
  height: 100%;
}

.editor-upload-progress .progress-circle-bg {
  stroke: #eee;
}

.editor-upload-progress .progress-circle-fill {
  stroke-linecap: round;
  transition: stroke-dasharray 0.3s ease;
}

@media screen and (max-width: 768px) {
  .page-header h1 {
    font-size: 20px;
  }

  .action-btn {
    height: 40px;
    font-size: 14px;
  }

  .mobile-step-actions,
  .mobile-step-content:nth-child(2) .form-actions {
    padding: 8px 12px !important;
  }
}

:deep(.w-e-bar-item button) {
  margin: 0 4px !important;
  padding: 0 4px!important;
}
</style>
