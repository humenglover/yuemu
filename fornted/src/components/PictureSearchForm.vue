<template>
  <div class="picture-search-form">
    <!-- 搜索容器 -->
    <div class="search-container">
      <div class="search-box">
        <input
          type="text"
          v-model="searchParams.searchText"
          placeholder="搜索图片..."
          class="search-input"
        >
        <button class="filter-btn" @click="showFilterModal = true">
          <span class="filter-icon"></span>
          <span class="filter-text">筛选</span>
        </button>
      </div>
    </div>

    <!-- 高级筛选弹窗 -->
    <div class="modal-overlay" v-if="showFilterModal" @click="handleCancelFilter">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>高级筛选</h3>
          <button class="close-btn" @click="handleCancelFilter">×</button>
        </div>

        <div class="modal-body">
          <!-- 分类选择 -->
          <div class="form-group">
            <label>分类</label>
            <div class="custom-select">
              <input
                type="text"
                v-model="tempSearchParams.category"
                placeholder="请输入分类"
                list="categoryList"
              >
              <datalist id="categoryList">
                <option v-for="option in categoryOptions" :key="option.value" :value="option.value">
                  {{ option.label }}
                </option>
              </datalist>
            </div>
          </div>

          <!-- 标签选择 -->
          <div class="form-group">
            <label>标签</label>
            <div class="tags-container">
              <div class="tags-input-container">
                <input
                  type="text"
                  placeholder="输入标签按回车添加"
                  @keyup.enter="addTag"
                  v-model="tagInput"
                >
              </div>
              <div class="selected-tags">
                <span
                  v-for="(tag, index) in tempSearchParams.tags"
                  :key="index"
                  class="tag-item"
                >
                  {{ tag }}
                  <span class="remove-tag" @click="removeTag(index)">×</span>
                </span>
              </div>
            </div>
          </div>

          <!-- 日期范围 -->
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

          <!-- 图片格式 -->
          <div class="form-group">
            <label>图片格式</label>
            <div class="format-options">
              <label
                v-for="format in picFormatOptions"
                :key="format.value"
                class="format-option"
              >
                <input
                  type="checkbox"
                  :value="format.value"
                  v-model="tempSearchParams.format"
                >
                <span class="format-label">{{ format.label }}</span>
              </label>
            </div>
          </div>
        </div>

        <div class="modal-footer">
          <button class="clear-btn" @click="doClear">清空</button>
          <button class="cancel-btn" @click="handleCancelFilter">取消</button>
          <button class="apply-btn" @click="handleApplyFilter">应用筛选</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.picture-search-form {
  padding: 20px;
}

.search-container {
  max-width: 800px;
  margin: 0 auto;
}

.search-box {
  display: flex;
  gap: 12px;
  background: white;
  padding: 8px;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;

  &:hover {
    box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
    transform: translateY(-1px);
  }
}

.search-input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 16px;
  padding: 12px 20px;
  border-radius: 12px;
  background: #f8fafc;
  color: #334155;
  transition: all 0.3s ease;

  &:focus {
    background: white;
    box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.05);
  }

  &::placeholder {
    color: #94a3b8;
  }
}

.filter-btn {
  border: none;
  padding: 0 24px;
  border-radius: 12px;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: white;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.3s ease;

  &:hover {
    background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
    transform: translateY(-1px);
  }

  &:active {
    transform: translateY(1px);
  }
}

.filter-icon {
  width: 16px;
  height: 16px;
  background: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 24 24' stroke='white'%3E%3Cpath stroke-linecap='round' stroke-linejoin='round' stroke-width='2' d='M3 4a1 1 0 011-1h16a1 1 0 011 1v2.586a1 1 0 01-.293.707l-6.414 6.414a1 1 0 00-.293.707V17l-4 4v-6.586a1 1 0 00-.293-.707L3.293 7.293A1 1 0 013 6.586V4z'/%3E%3C/svg%3E") no-repeat center;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.3s ease;
}

.modal-content {
  width: 90%;
  max-width: 500px;
  background: white;
  border-radius: 20px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.2);
  animation: slideUp 0.3s ease;
}

.modal-header {
  padding: 20px 24px;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  justify-content: space-between;
  align-items: center;

  h3 {
    margin: 0;
    color: #1e293b;
    font-size: 18px;
    font-weight: 600;
  }
}

.close-btn {
  border: none;
  background: none;
  font-size: 24px;
  color: #64748b;
  cursor: pointer;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;

  &:hover {
    background: #f1f5f9;
    color: #334155;
  }
}

.modal-body {
  padding: 24px;
}

.form-group {
  margin-bottom: 24px;

  label {
    display: block;
    margin-bottom: 8px;
    color: #334155;
    font-weight: 500;
  }
}

.custom-select {
  position: relative;

  input {
    width: 100%;
    padding: 12px 16px;
    border: 1px solid #e2e8f0;
    border-radius: 12px;
    font-size: 14px;
    color: #334155;
    background: white;
    transition: all 0.2s ease;

    &:focus {
      border-color: #3b82f6;
      box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
      outline: none;
    }
  }
}

.tags-container {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 8px;
  background: white;
}

.tags-input-container {
  margin-bottom: 8px;

  input {
    width: 100%;
    padding: 8px 12px;
    border: 1px solid #e2e8f0;
    border-radius: 8px;
    font-size: 14px;

    &:focus {
      border-color: #3b82f6;
      outline: none;
    }
  }
}

.selected-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: #f1f5f9;
  border-radius: 20px;
  font-size: 13px;
  color: #334155;
  animation: fadeIn 0.2s ease;

  .remove-tag {
    cursor: pointer;
    opacity: 0.5;
    transition: opacity 0.2s ease;

    &:hover {
      opacity: 1;
    }
  }
}

.date-range {
  display: flex;
  align-items: center;
  gap: 12px;
}

.date-input {
  flex: 1;
  padding: 12px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  font-size: 14px;
  color: #334155;

  &:focus {
    border-color: #3b82f6;
    outline: none;
  }
}

.date-separator {
  color: #64748b;
}

.format-options {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.format-option {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;

  input[type="checkbox"] {
    display: none;
  }

  .format-label {
    padding: 8px 16px;
    background: #f1f5f9;
    border-radius: 20px;
    font-size: 14px;
    color: #64748b;
    transition: all 0.2s ease;
  }

  input[type="checkbox"]:checked + .format-label {
    background: #3b82f6;
    color: white;
  }
}

.modal-footer {
  padding: 20px 24px;
  border-top: 1px solid #e2e8f0;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.clear-btn,
.cancel-btn,
.apply-btn {
  padding: 10px 20px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.clear-btn {
  border: 1px solid #e2e8f0;
  background: white;
  color: #64748b;

  &:hover {
    background: #f8fafc;
    border-color: #cbd5e1;
  }
}

.cancel-btn {
  border: 1px solid #e2e8f0;
  background: white;
  color: #334155;

  &:hover {
    background: #f8fafc;
    border-color: #cbd5e1;
  }
}

.apply-btn {
  border: none;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: white;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(37, 99, 235, 0.2);
  }

  &:active {
    transform: translateY(1px);
  }
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .picture-search-form {
    padding: 12px;
  }

  .search-box {
    flex-direction: row;
    padding: 8px;
    gap: 8px;
    background: transparent;
    box-shadow: none;

    &:hover {
      box-shadow: none;
      transform: none;
    }
  }

  .search-input {
    font-size: 14px;
    padding: 8px 12px;
    background: rgba(255, 255, 255, 0.8);
    backdrop-filter: blur(8px);
    border-radius: 20px;
  }

  .filter-btn {
    padding: 8px 12px;
    min-width: auto;
    white-space: nowrap;
  }

  .filter-text {
    display: none;
  }

  .filter-icon {
    margin: 0;
  }

  .modal-content {
    width: 100%;
    margin: 16px;
    max-height: 90vh;
    overflow-y: auto;
  }

  .modal-header {
    padding: 16px;
    position: sticky;
    top: 0;
    background: white;
    z-index: 1;
  }

  .modal-body {
    padding: 16px;
  }

  .form-group {
    margin-bottom: 16px;
  }

  .date-range {
    flex-direction: row;
    gap: 8px;
  }

  .date-input {
    padding: 8px;
    font-size: 13px;
  }

  .format-options {
    gap: 6px;
  }

  .format-option .format-label {
    padding: 6px 10px;
    font-size: 12px;
  }

  .modal-footer {
    flex-direction: row;
    padding: 16px;
    position: sticky;
    bottom: 0;
    background: white;
    z-index: 1;
  }

  .clear-btn,
  .cancel-btn,
  .apply-btn {
    flex: 1;
    padding: 8px;
    font-size: 13px;
  }

  .tags-container {
    padding: 6px;
  }

  .tag-item {
    padding: 2px 8px;
    font-size: 12px;
  }
}
</style>

<script setup lang="ts">
import { ref, reactive, watch, onMounted, defineEmits, defineExpose } from 'vue'
import { formatTime } from '@/utils/dateUtils'
import { listPictureTagCategoryUsingGet } from '@/api/pictureController'

interface SearchParams {
  searchText: string
  category: string
  tags: string[]
  startEditTime: Date | null
  endEditTime: Date | null
  format: string[]
}

interface EmitsType {
  (event: 'search', params: SearchParams): void
  (event: 'filter', params: SearchParams): void
}

const emit = defineEmits<EmitsType>()

// 搜索参数
const searchParams = reactive<SearchParams>({
  searchText: '',
  category: '',
  tags: [],
  startEditTime: null,
  endEditTime: null,
  format: []
})

// 临时搜索参数
const tempSearchParams = reactive({
  category: '',
  tags: [],
  format: []
})

// 控制弹窗显示
const showFilterModal = ref(false)

// 标签输入
const tagInput = ref('')

// 日期选择
const startDate = ref('')
const endDate = ref('')

// 添加标签
const addTag = () => {
  if (tagInput.value && !tempSearchParams.tags.includes(tagInput.value)) {
    tempSearchParams.tags.push(tagInput.value)
    tagInput.value = ''
  }
}

// 移除标签
const removeTag = (index: number) => {
  tempSearchParams.tags.splice(index, 1)
}

// 应用筛选
const handleApplyFilter = () => {
  // 清空搜索文本
  searchParams.searchText = ''

  // 应用其他筛选条件
  Object.assign(searchParams, tempSearchParams)
  if (startDate.value) {
    searchParams.startEditTime = new Date(startDate.value)
  }
  if (endDate.value) {
    searchParams.endEditTime = new Date(endDate.value)
  }
  showFilterModal.value = false
  emit('search', searchParams)
}

// 取消筛选
const handleCancelFilter = () => {
  showFilterModal.value = false
  Object.assign(tempSearchParams, searchParams)
}

// 清空筛选
const doClear = () => {
  Object.keys(searchParams).forEach(key => {
    searchParams[key] = Array.isArray(searchParams[key]) ? [] : ''
  })
  startDate.value = ''
  endDate.value = ''
  emit('search', searchParams)
}

// 图片格式选项
const picFormatOptions = [
  { value: 'jpeg', label: 'JPEG' },
  { value: 'png', label: 'PNG' },
  { value: 'jpg', label: 'JPG' },
  { value: 'webp', label: 'WEBP' },
  { value: 'gif', label: 'GIF' }
]

// 分类选项
const categoryOptions = ref([])

// 获取分类和标签选项
const getTagCategoryOptions = async () => {
  try {
    const res = await listPictureTagCategoryUsingGet()
    if (res.data.code === 0 && res.data.data) {
      categoryOptions.value = (res.data.data.categoryList || []).map(item => ({
        value: item,
        label: item
      }))
    }
  } catch (error) {
    console.error('获取分类列表失败:', error)
  }
}

// 监听搜索文本变化
watch(() => searchParams.searchText, (newVal) => {
  emit('search', searchParams)
})

// 初始化
onMounted(() => {
  getTagCategoryOptions()
})

// 添加下拉刷新处理
const handleRefresh = () => {
  // 清空所有筛选条件
  Object.keys(searchParams).forEach(key => {
    searchParams[key] = Array.isArray(searchParams[key]) ? [] : ''
  })
  startDate.value = ''
  endDate.value = ''
  showFilterModal.value = false

  // 重置临时搜索参数
  Object.assign(tempSearchParams, {
    category: '',
    tags: [],
    format: []
  })

  // 触发搜索事件
  emit('search', searchParams)
}

// 导出方法供父组件调用
defineExpose({
  handleRefresh
})
</script>
