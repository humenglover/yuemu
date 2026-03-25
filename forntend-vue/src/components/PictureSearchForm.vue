<template>
  <div class="picture-search-form">
    <div class="search-container">
      <div class="search-box">
        <i class="fas fa-search search-icon"></i>
        <input
          type="text"
          v-model="searchParams.searchText"
          placeholder="搜索图片..."
          class="search-input"
          @keyup.enter="emit('search', searchParams)"
        >
        <div class="search-actions">
          <button class="filter-btn" @click="showFilterModal = !showFilterModal">
            <i class="fas fa-filter filter-icon"></i>
          </button>
          <button
            class="search-btn"
            @click="emit('search', searchParams)"
            v-if="searchParams.searchText"
          >
            搜索
          </button>
        </div>
      </div>
    </div>

    <div class="modal-overlay" v-if="showColorModal" @click="showColorModal = false">
      <div class="modal-content color-modal" @click.stop>
        <div class="modal-header">
          <h3>颜色搜索</h3>
          <button class="close-btn" @click="showColorModal = false">
            <i class="fas fa-times close-icon"></i>
          </button>
        </div>
        <div class="modal-body">
          <div class="color-picker-wrapper">
            <color-picker
              format="hex"
              @pureColorChange="handleColorChange"
              :disable-alpha="true"
              :disable-history="true"
              shape="square"
            />
          </div>
        </div>
      </div>
    </div>

    <div class="modal-overlay" v-if="showAdvancedFilterModal" @click="showAdvancedFilterModal = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>高级筛选</h3>
          <button class="close-btn" @click="showAdvancedFilterModal = false">
            <i class="fas fa-times close-icon"></i>
          </button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>分类</label>
            <div class="selector-wrapper" @click="openCategoryModal">
              <div class="selected-value">
                {{ tempSearchParams.category || '请选择分类' }}
              </div>
              <i class="fas fa-chevron-right arrow-icon"></i>
            </div>
          </div>

          <div class="form-group">
            <label>标签</label>
            <div class="selector-wrapper" @click="openTagModal">
              <div class="selected-tags">
                <span v-if="tempSearchParams.tags.length === 0" class="placeholder">请选择标签</span>
                <span v-for="(tag, index) in tempSearchParams.tags" :key="index" class="tag-item">
                  {{ tag }}
                </span>
              </div>
              <i class="fas fa-chevron-right arrow-icon"></i>
            </div>
          </div>

          <div class="form-group">
            <label>日期范围</label>
            <div class="date-range">
              <input
                type="date"
                v-model="startDate"
                class="date-input"
              >
              <span class="date-separator">至</span>
              <input
                type="date"
                v-model="endDate"
                class="date-input"
              >
            </div>
          </div>
        </div>

        <div class="modal-footer">
          <button class="clear-btn" @click="doClear">
            <i class="fas fa-trash clear-icon"></i> 清空
          </button>
          <button class="cancel-btn" @click="handleCancelFilter">
            <i class="fas fa-times cancel-icon"></i> 取消
          </button>
          <button class="apply-btn" @click="handleApplyFilter">
            <i class="fas fa-check apply-icon"></i> 应用筛选
          </button>
        </div>
      </div>
    </div>

    <!-- 分类选择弹框 -->
    <div class="modal-overlay" v-if="categoryModalVisible" @click="categoryModalVisible = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>选择分类</h3>
          <button class="close-btn" @click="categoryModalVisible = false">
            <i class="fas fa-times close-icon"></i>
          </button>
        </div>
        <div class="modal-body">
          <div class="search-container">
            <i class="fas fa-search search-icon-small"></i>
            <input
              type="text"
              v-model="categorySearchText"
              placeholder="搜索分类..."
              class="search-input"
            >
          </div>

          <div class="options-list">
            <div
              v-for="category in filteredCategories"
              :key="category.value"
              class="option-item"
              :class="{ selected: tempSearchParams.category === category.value }"
              @click="selectCategory(category.value)"
            >
              <i class="fas fa-folder category-icon"></i>
              {{ category.label }}
            </div>
            <div v-if="filteredCategories.length === 0" class="empty-tip">
              <i class="fas fa-inbox empty-icon"></i> 暂无匹配的分类
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 标签选择弹框 -->
    <div class="modal-overlay" v-if="tagModalVisible" @click="tagModalVisible = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>选择标签</h3>
          <button class="close-btn" @click="tagModalVisible = false">
            <i class="fas fa-times close-icon"></i>
          </button>
        </div>
        <div class="modal-body">
          <div class="search-container">
            <i class="fas fa-search search-icon-small"></i>
            <input
              type="text"
              v-model="tagSearchText"
              placeholder="搜索标签..."
              class="search-input"
            >
          </div>

          <div class="tags-grid">
            <div
              v-for="tag in filteredTags"
              :key="tag.value"
              class="tag-cell"
              :class="{ selected: tempSearchParams.tags.includes(tag.value) }"
              @click="toggleTag(tag.value)"
            >
              <i class="fas fa-tag tag-icon"></i>
              {{ tag.label }}
            </div>
            <div v-if="filteredTags.length === 0" class="empty-tip">
              <i class="fas fa-tags empty-icon"></i> 暂无匹配的标签
            </div>
          </div>

          <!-- 已选标签 -->
          <div class="selected-tags-footer" v-if="tempSearchParams.tags.length > 0">
            <span class="selected-count">
              <i class="fas fa-check-circle count-icon"></i> 已选择 {{ tempSearchParams.tags.length }} 个标签
            </span>
            <button class="clear-tags-btn" @click="clearAllTags">
              <i class="fas fa-trash clear-tags-icon"></i> 清空
            </button>
          </div>
        </div>
      </div>
    </div>

    <div class="dropdown-menu" v-if="showFilterModal" @click.stop>
      <div class="dropdown-item" @click="handleColorSearch">
        <i class="fas fa-palette color-icon"></i>
        <span class="item-text">颜色搜索</span>
      </div>
      <div class="dropdown-item" @click="handleAdvancedFilter">
        <i class="fas fa-sliders-h filter-icon"></i>
        <span class="item-text">高级筛选</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch, onMounted, onUnmounted, defineEmits, defineExpose, computed } from 'vue'
import { ColorPicker } from 'vue3-colorpicker'
import 'vue3-colorpicker/style.css'
import { listPictureTagCategoryUsingGet } from '@/api/pictureController'

interface SearchParams {
  searchText: string
  category: string
  tags: string[]
  startEditTime: Date | null
  endEditTime: Date | null
  format: string[]
}

const emit = defineEmits<{
  (event: 'search', params: SearchParams): void
  (event: 'filter', params: SearchParams): void
  (event: 'colorChange', color: string): void
}>()

const searchParams = reactive<SearchParams>({
  searchText: '',
  category: '',
  tags: [],
  startEditTime: null,
  endEditTime: null,
  format: []
})

const tempSearchParams = reactive({
  category: '',
  tags: []
})

const showFilterModal = ref(false)
const showColorModal = ref(false)
const showAdvancedFilterModal = ref(false)

// 点击文档空白处关闭下拉菜单
const handleClickOutside = (event: Event) => {
  const dropdown = document.querySelector('.dropdown-menu')
  const filterBtn = document.querySelector('.filter-btn')

  if (showFilterModal.value && dropdown && filterBtn &&
    !dropdown.contains(event.target as Node) &&
    !filterBtn.contains(event.target as Node)) {
    showFilterModal.value = false
  }
}

onMounted(() => {
  getTagCategoryOptions()
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})

const tagSearchText = ref('')
const tagModalVisible = ref(false)
const startDate = ref('')
const endDate = ref('')

const handleColorChange = (color: string) => {
  showColorModal.value = false
  emit('colorChange', color)
}

const handleColorSearch = () => {
  showFilterModal.value = false
  showColorModal.value = true
}

const categorySearchText = ref('')
const categoryModalVisible = ref(false)

const handleAdvancedFilter = () => {
  showFilterModal.value = false
  showAdvancedFilterModal.value = true
}

// 打开分类选择弹框
const openCategoryModal = () => {
  categoryModalVisible.value = true
  categorySearchText.value = ''
}

// 打开标签选择弹框
const openTagModal = () => {
  tagModalVisible.value = true
  tagSearchText.value = ''
}

// 选择分类
const selectCategory = (categoryValue: string) => {
  tempSearchParams.category = categoryValue
  categoryModalVisible.value = false
}

// 切换标签选择状态
const toggleTag = (tagValue: string) => {
  const index = tempSearchParams.tags.indexOf(tagValue)
  if (index > -1) {
    // 移除标签
    tempSearchParams.tags.splice(index, 1)
  } else {
    // 添加标签
    tempSearchParams.tags.push(tagValue)
  }
}

// 清空所有标签
const clearAllTags = () => {
  tempSearchParams.tags = []
}

// 过滤后的分类
const filteredCategories = computed(() => {
  if (!categorySearchText.value) {
    return categoryOptions.value
  }
  const searchText = categorySearchText.value.toLowerCase()
  return categoryOptions.value.filter(item =>
    item.label.toLowerCase().includes(searchText) ||
    item.value.toLowerCase().includes(searchText)
  )
})

// 过滤后的标签
const filteredTags = computed(() => {
  if (!tagSearchText.value) {
    return tagOptions.value
  }
  const searchText = tagSearchText.value.toLowerCase()
  return tagOptions.value.filter(item =>
    item.label.toLowerCase().includes(searchText) ||
    item.value.toLowerCase().includes(searchText)
  )
})

const handleApplyFilter = () => {
  searchParams.searchText = ''
  Object.assign(searchParams, tempSearchParams)
  if (startDate.value) searchParams.startEditTime = new Date(startDate.value)
  if (endDate.value) searchParams.endEditTime = new Date(endDate.value)
  showFilterModal.value = false
  emit('search', searchParams)
}

const handleCancelFilter = () => {
  showFilterModal.value = false
  Object.assign(tempSearchParams, searchParams)
}

const doClear = () => {
  Object.keys(searchParams).forEach(key => {
    searchParams[key] = Array.isArray(searchParams[key]) ? [] : ''
  })
  startDate.value = ''
  endDate.value = ''
  emit('search', searchParams)
}

const categoryOptions = ref<Array<{ value: string; label: string }>>([])
const tagOptions = ref<Array<{ value: string; label: string }>>([])

const getTagCategoryOptions = async () => {
  try {
    const res = await listPictureTagCategoryUsingGet()
    if (res.data.code === 0 && res.data.data) {
      categoryOptions.value = (res.data.data.categoryList || []).map(item => ({
        value: item,
        label: item
      }))

      // 更新标签选项
      tagOptions.value = (res.data.data.tagList || []).map(item => ({
        value: item,
        label: item
      }))
    }
  } catch (error) {
    console.error('获取分类和标签列表失败:', error)
  }
}

watch(() => searchParams.searchText, () => emit('search', searchParams))

onMounted(() => getTagCategoryOptions())

const handleRefresh = () => {
  Object.keys(searchParams).forEach(key => {
    searchParams[key] = Array.isArray(searchParams[key]) ? [] : ''
  })
  startDate.value = ''
  endDate.value = ''
  showFilterModal.value = false
  Object.assign(tempSearchParams, { category: '', tags: [] })
  emit('search', searchParams)
}

defineExpose({ handleRefresh })
</script>

<style lang="scss" scoped>
.picture-search-form {
  padding: 12px 16px;
}

.search-container {
  max-width: 800px;
  margin: 0 auto;
}

/* 核心优化：压缩搜索框高度，整体更紧凑 */
.search-box {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 6px 12px; /* 大幅降低内边距 */
  height: 40px; /* 固定高度，移动端更协调 */
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.02);

  &:focus-within {
    border-color: #2563eb;
    box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
    outline: none;
  }
}

/* 搜索图标缩小 */
.search-icon {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 24 24' stroke='%239ca3af'%3E%3Cpath stroke-linecap='round' stroke-linejoin='round' stroke-width='2' d='M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z'/%3E%3C/svg%3E");
  background-size: contain;
}

/* 输入框高度适配 */
.search-input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 14px;
  color: #111827;
  background: transparent;
  padding: 0; /* 清空内边距，靠父容器padding撑高度 */
  height: 100%;
  line-height: 1; /* 行高重置 */

  &::placeholder {
    color: #9ca3af;
    font-size: 13px;
  }
}

/* 操作区高度适配 */
.search-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  height: 100%;
}

/* 筛选按钮缩小 */
.filter-btn {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  border: none;
  background: #f9fafb;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;

  &:hover {
    background: #f3f4f6;
  }

  .filter-icon {
    width: 14px;
    height: 14px;
    background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 24 24' stroke='%236b7280'%3E%3Cpath stroke-linecap='round' stroke-linejoin='round' stroke-width='2' d='M3 4a1 1 0 011-1h16a1 1 0 011 1v2.586a1 1 0 01-.293.707l-6.414 6.414a1 1 0 00-.293.707V17l-4 4v-6.586a1 1 0 00-.293.707L3.293 7.293A1 1 0 013 6.586V4z'/%3E%3C/svg%3E");
    background-size: contain;
  }
}

/* 搜索按钮高度压缩 */
.search-btn {
  padding: 4px 10px;
  border-radius: 6px;
  border: none;
  background: #2563eb;
  color: white;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  height: 28px;
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;

  &:hover {
    background: #1d4ed8;
  }
}

/* 下拉菜单位置适配 */
.dropdown-menu {
  position: absolute;
  top: calc(100% + 6px);
  right: 16px;
  min-width: 140px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  z-index: 1000;
  overflow: hidden;

  .dropdown-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 10px 14px;
    cursor: pointer;
    transition: background 0.2s;
    font-size: 13px;
    color: #374151;

    &:hover {
      background: #f9fafb;
    }

    .item-icon {
      width: 16px;
      height: 16px;
      background-size: contain;
    }

    .color-icon {
      background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 24 24' stroke='%236b7280'%3E%3Cpath stroke-linecap='round' stroke-linejoin='round' stroke-width='2' d='M7 21a4 4 0 01-4-4V5a2 2 0 012-2h4a2 2 0 012 2v12a4 4 0 01-4 4zm0 0h12a2 2 0 002-2v-4a2 2 0 00-2-2h-2.343M11 7.343l3.657 3.657M7 7.343l3.657 3.657'/%3E%3C/svg%3E");
    }

    .filter-icon {
      background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 24 24' stroke='%236b7280'%3E%3Cpath stroke-linecap='round' stroke-linejoin='round' stroke-width='2' d='M3 4a1 1 0 011-1h16a1 1 0 011 1v2.586a1 1 0 01-.293.707l-6.414 6.414a1 1 0 00-.293.707V17l-4 4v-6.586a1 1 0 00-.293.707L3.293 7.293A1 1 0 013 6.586V4z'/%3E%3C/svg%3E");
    }
  }
}

/* 弹窗样式微调 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  backdrop-filter: blur(4px);
  animation: fadeIn 0.2s ease;
}

.modal-content {
  width: 90%;
  max-width: 500px;
  background: #ffffff;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  animation: slideUp 0.2s ease;
  overflow: hidden;

  &.color-modal {
    max-width: 320px;
    .modal-body {
      padding: 20px;
      display: flex;
      justify-content: center;
      .color-picker-wrapper {
        :deep(.vc-color-wrap) {
          width: 100px;
          height: 100px;
          border-radius: 6px;
          border: 1px solid #e8e8e8;
          transition: all 0.2s ease;
          &:hover {
            border-color: #2563eb;
            transform: scale(1.02);
          }
        }
      }
    }
  }
}

.modal-header {
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  h3 {
    margin: 0;
    font-size: 15px;
    font-weight: 600;
    color: #333333;
  }
}

.close-btn {
  border: none;
  background: none;
  font-size: 18px;
  color: #999999;
  cursor: pointer;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  &:hover {
    background-color: #f5f5f5;
    color: #666666;
  }
}

.modal-body {
  padding: 16px;
}

.form-group {
  margin-bottom: 16px;
  label {
    display: block;
    margin-bottom: 6px;
    font-size: 13px;
    font-weight: 500;
    color: #333333;
  }
}

.custom-select input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  font-size: 13px;
  color: #333333;
  transition: all 0.2s ease;
  &:focus {
    border-color: #2563eb;
    outline: none;
    box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.1);
  }
}

.tags-container {
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  padding: 8px;
  .tags-input-container {
    margin-bottom: 6px;
    input {
      width: 100%;
      padding: 6px 10px;
      border: 1px solid #e8e8e8;
      border-radius: 4px;
      font-size: 13px;
      &:focus {
        border-color: #2563eb;
        outline: none;
      }
    }
  }
  .selected-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
  }
}

.tag-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  background: #f5f5f5;
  border-radius: 12px;
  font-size: 12px;
  color: #333333;
  .remove-tag {
    cursor: pointer;
    color: #999999;
    font-size: 12px;
    transition: color 0.2s ease;
    &:hover {
      color: #666666;
    }
  }
}

.date-range {
  display: flex;
  align-items: center;
  gap: 8px;
  .date-input {
    flex: 1;
    padding: 8px 12px;
    border: 1px solid #e8e8e8;
    border-radius: 6px;
    font-size: 13px;
    color: #333333;
    transition: all 0.2s ease;
    &:focus {
      border-color: #2563eb;
      outline: none;
      box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.1);
    }
  }
  .date-separator {
    color: #999999;
    font-size: 12px;
  }
}

.modal-footer {
  padding: 12px 16px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  background: #fafafa;
}

.clear-btn,
.cancel-btn,
.apply-btn {
  padding: 6px 12px;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid transparent;
}

.clear-btn {
  border-color: #e8e8e8;
  background: #ffffff;
  color: #666666;
  &:hover {
    background: #f5f5f5;
  }
}

.cancel-btn {
  border-color: #e8e8e8;
  background: #ffffff;
  color: #333333;
  &:hover {
    background: #f5f5f5;
  }
}

.apply-btn {
  background: #2563eb;
  color: #ffffff;
  border-color: #2563eb;
  &:hover {
    background: #1d4ed8;
  }
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 移动端极致适配 */
@media screen and (max-width: 768px) {
  .picture-search-form {
    padding: 8px 12px;
  }
  .search-box {
    padding: 4px 8px;
    height: 36px; /* 移动端进一步降低高度 */
    gap: 6px;
  }
  .search-icon {
    width: 14px;
    height: 14px;
  }
  .search-input {
    font-size: 13px;
    min-width: 0; /* 允许flex项目收缩到内容大小以下 */
    flex: 1; /* 确保输入框尽可能占据剩余空间 */
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap; /* 防止文本换行 */
  }
  .search-input::placeholder {
    font-size: 12px;
  }
  .filter-btn {
    width: 24px;
    height: 24px;
  }
  .filter-icon {
    width: 12px;
    height: 12px;
  }
  .search-btn {
    padding: 2px 8px;
    font-size: 12px;
    height: 24px;
  }
  .dropdown-menu {
    right: 12px;
    min-width: 120px;
    .dropdown-item {
      padding: 8px 12px;
      font-size: 12px;
    }
  }
  .modal-content {
    width: calc(100% - 24px);
    max-height: 90vh;
    overflow-y: auto;
  }
  .modal-header,
  .modal-body,
  .modal-footer {
    padding: 12px;
  }
  .date-range {
    flex-direction: column;
    gap: 6px;
    .date-input {
      width: 100%;
      padding: 6px 10px;
    }
  }
  .modal-footer {
    flex-wrap: wrap;
    gap: 6px;
  }
  .clear-btn,
  .cancel-btn,
  .apply-btn {
    flex: 1;
    text-align: center;
    padding: 6px 0;
    font-size: 12px;
  }
}

/* 针对更小屏幕的适配 */
@media screen and (max-width: 480px) {
  .search-actions {
    gap: 4px; /* 减少操作按钮之间的间距 */
  }
  .search-btn {
    padding: 2px 6px; /* 减少搜索按钮的水平内边距 */
    font-size: 11px;
    min-width: 40px; /* 设置最小宽度 */
  }
  .search-input {
    font-size: 12px; /* 进一步减小字体 */
  }
  .search-icon {
    width: 12px;
    height: 12px;
  }
}

/* 优化的选择器样式 */
.selector-wrapper {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-radius: 8px;
  border: 1px solid #e8e8e8;
  background: #fafafa;
  cursor: pointer;
  transition: all 0.2s ease;
}

.selector-wrapper:hover {
  border-color: #ffb3b3;
  background: #fff;
}

.selected-value {
  font-size: 14px;
  color: #333;
}

.selected-value:empty::before {
  content: '请选择分类';
  color: #999;
}

.arrow-icon {
  font-size: 12px;
  color: #999;
}

/* 已选标签展示 */
.selected-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  width: 100%;
}

.tag-item {
  padding: 4px 10px;
  background: #f0f8ff;
  color: #1890ff;
  border-radius: 12px;
  font-size: 12px;
}

.placeholder {
  color: #999;
  font-size: 14px;
}

/* 选择器弹框样式 */
.search-container {
  padding: 8px 0;

}

.search-input {
  border-radius: 8px;
  padding: 8px 12px;
  font-size: 14px;
}

.options-list {
  flex: 1;
  overflow-y: auto;
}

.option-item {
  padding: 12px 16px;
  font-size: 14px;
  cursor: pointer;
  transition: background-color 0.2s ease;
  border-bottom: 1px solid #f5f5f5;
}

.option-item:hover {
  background-color: #f8f8f8;
}

.option-item.selected {
  background-color: #fff0f0;
  color: #ff2442;
  font-weight: 500;
}

/* 标签网格布局 */
.tags-grid {
  flex: 1;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  padding: 8px 0;
  overflow-y: auto;
}

.tag-cell {
  padding: 8px 16px;
  background: #f5f5f5;
  border-radius: 20px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.tag-cell:hover {
  background: #e8e8e8;
}

.tag-cell.selected {
  background: #ff2442;
  color: #fff;
}

/* 空提示 */
.empty-tip {
  padding: 24px;
  text-align: center;
  color: #999;
  font-size: 14px;
}

/* 已选标签底部栏 */
.selected-tags-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  margin-top: 8px;
  border-top: 1px solid #f5f5f5;
}

.selected-count {
  font-size: 13px;
  color: #666;
}

.clear-tags-btn {
  border: none;
  background: none;
  color: #ff2442;
  cursor: pointer;
  font-size: 13px;
  padding: 4px 8px;
  border-radius: 4px;
  transition: background-color 0.2s ease;
}

.clear-tags-btn:hover {
  background-color: #ffeef0;
}
</style>
