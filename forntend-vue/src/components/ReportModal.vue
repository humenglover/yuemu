<template>
  <div class="report-mask" v-if="visible" @click.self="handleCancel">
    <div class="report-container">
      <div class="report-header">
        <h2 class="report-title">
          <i class="fas fa-shield-alt title-icon"></i> 违规举报
        </h2>
        <button class="report-close" @click="handleCancel">
          <i class="fas fa-times"></i>
        </button>
      </div>

      <div class="report-body">
        <!-- 如果是从外部组件带入的目标信息，显示为智能 Badge -->
        <div class="report-item" v-if="props.targetType && props.targetId">
          <div class="report-badge-container">
            <div class="target-badge">
              <span class="target-type-tag">{{ getTargetTypeName(props.targetType) }}</span>
              <span class="target-id-text">ID: {{ props.targetId }}</span>
            </div>
          </div>
        </div>

        <!-- 否则（通常是在全部内容列表等需要手动输入的场景），显示手填输入框 -->
        <template v-else>
          <!-- 目标类型 -->
          <div class="report-item">
            <div class="report-label">目标类型 <span class="required">*</span></div>
            <div class="report-select-wrap">
              <select v-model="formData.targetType" class="report-select" required>
                <option value="" disabled>请选择目标类型</option>
                <option value="1">图片</option>
                <option value="2">帖子</option>
                <option value="3">评论</option>
                <option value="4">用户</option>
                <option value="5">其他</option>
              </select>
              <i class="fas fa-chevron-down select-arrow"></i>
            </div>
            <div class="report-error" v-if="errors.targetType">{{ errors.targetType }}</div>
          </div>

          <!-- 目标ID -->
          <div class="report-item">
            <div class="report-label">目标内容 ID <span class="required">*</span></div>
            <input
              type="text"
              v-model="formData.targetId"
              class="report-input disabled-placeholder"
              placeholder="请输入相关内容的ID或链接"
            >
            <div class="report-error" v-if="errors.targetId">{{ errors.targetId }}</div>
          </div>
        </template>

        <!-- 举报类型 (Tags 单选) -->
        <div class="report-item">
          <div class="report-label">举报原因 <span class="required">*</span></div>
          <div class="report-type-grid">
            <div
              class="report-type-chip"
              v-for="type in reportTypes"
              :key="type.value"
              :class="{ 'active': formData.reportType === type.value }"
              @click="formData.reportType = type.value"
            >
              {{ type.label }}
            </div>
          </div>
          <div class="report-error" v-if="errors.reportType">{{ errors.reportType }}</div>
        </div>

        <!-- 详细说明 -->
        <div class="report-item">
          <div class="report-label">补充说明 <span class="required">*</span></div>
          <div class="textarea-wrapper">
            <textarea
              v-model="formData.reason"
              class="report-textarea"
              placeholder="请详细描述具体的违规情况，这有助于我们更快处理..."
              maxlength="500"
            ></textarea>
            <div class="report-word-count" :class="{ 'near-limit': formData.reason.length > 450 }">
              {{ formData.reason.length }}/500
            </div>
          </div>
          <div class="report-error" v-if="errors.description">{{ errors.description }}</div>
        </div>

        <!-- 证据截图 -->
        <div class="report-item">
          <div class="report-label">证据截图 <span class="optional">(选填，最多6张)</span></div>
          <div class="report-upload-wrap">
            <div class="report-upload-list">
              <div
                class="report-upload-preview"
                v-for="(file, index) in fileList"
                :key="file.uid"
              >
                <img :src="file.preview" class="report-upload-img">
                <div class="upload-mask" v-if="file.status === 'uploading'">
                  <i class="fas fa-spinner fa-spin"></i>
                </div>
                <button class="report-upload-remove" @click="removeFile(index)">
                  <i class="fas fa-times"></i>
                </button>
              </div>
              <div
                class="report-upload-btn"
                v-if="fileList.length < 6"
                @click="triggerFileInput"
              >
                <i class="fas fa-camera upload-icon"></i>
              </div>
            </div>
            <input
              type="file"
              class="report-file-input"
              accept="image/*"
              multiple
              @change="handleFileChange"
            >
          </div>
        </div>
      </div>

      <!-- 提交按钮 -->
      <div class="report-footer">
        <button
          class="report-submit-btn"
          :class="{ 'loading': submitting }"
          @click="handleSubmit"
          :disabled="submitting || !isFormValid"
        >
          <span v-if="!submitting">提交举报</span>
          <span v-else>提交中...</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue';
import { addReportUsingPost } from '@/api/reportController';
import { uploadPostImageUsingPost } from '@/api/pictureController';

// Props定义
interface Props {
  open?: boolean;
  targetId?: string;
  targetType?: string;
}
const props = withDefaults(defineProps<Props>(), {
  open: false,
  targetId: undefined,
  targetType: ''
});

// 事件派发
const emit = defineEmits<{
  'update:open': [open: boolean];
  success: [];
}>();

// 举报类型选项
interface ReportTypeOption {
  label: string;
  value: string;
}
const reportTypes: ReportTypeOption[] = [
  { label: '垃圾广告', value: '1' },
  { label: '违规内容', value: '2' },
  { label: '有害信息', value: '3' },
  { label: '人身攻击', value: '4' },
  { label: '侵犯隐私', value: '5' },
  { label: '版权问题', value: '6' },
  { label: '其他', value: '7' }
];

interface UploadFileItem {
  uid: number;
  file?: File;
  preview: string;
  status: 'uploading' | 'done' | 'error';
  url?: string;
}

// 响应式数据
const visible = ref(false);
const submitting = ref(false);
const fileList = ref<UploadFileItem[]>([]);
const errors = ref<Record<string, string>>({});

// 表单数据
const formData = ref({
  reportType: '',
  targetType: props.targetType || '',
  targetId: props.targetId || '',
  reason: '',
  screenshotUrls: [] as string[]
});

// 获取目标类型中文名
const getTargetTypeName = (type: string) => {
  const map: Record<string, string> = {
    '1': '图片',
    '2': '帖子',
    '3': '评论',
    '4': '用户',
    '5': '其他'
  };
  return map[type] || '未知对象';
};

// 预判表单是否填写完整（用于按钮置灰逻辑交互优化）
const isFormValid = computed(() => {
  return formData.value.reportType !== '' &&
    formData.value.targetType !== '' &&
    formData.value.targetId !== '' &&
    formData.value.reason.trim().length > 0;
});

// 监听弹窗显示
watch(() => props.open, (newVal) => {
  if (newVal) {
    visible.value = true;
    resetForm();
    document.body.style.overflow = 'hidden';
  } else {
    visible.value = false;
    document.body.style.overflow = '';
  }
});

// 监听props变化，动态更新表单数据
watch(() => [props.targetType, props.targetId], ([newTargetType, newTargetId]) => {
  if (visible.value) { // 只在模态框可见时更新
    formData.value.targetType = newTargetType || '';
    formData.value.targetId = newTargetId || '';
  }
});

// 重置表单
const resetForm = () => {
  formData.value = {
    reportType: '',
    targetType: props.targetType || '',
    targetId: props.targetId || '',
    reason: '',
    screenshotUrls: []
  };
  fileList.value = [];
  errors.value = {};
};

// 更新表单数据（用于外部动态更新）
const updateFormData = (newTargetType: string, newTargetId: string) => {
  formData.value.targetType = newTargetType;
  formData.value.targetId = newTargetId;
};

// 打开/关闭弹窗
const openModal = () => {
  visible.value = true;
  resetForm();
  document.body.style.overflow = 'hidden';
};
const closeModal = () => {
  visible.value = false;
  emit('update:open', false);
  document.body.style.overflow = '';
};
const handleCancel = () => closeModal();

// 触发文件选择
const triggerFileInput = () => {
  document.querySelector('.report-file-input')?.click();
};

// 处理文件选择
const handleFileChange = (e: Event) => {
  const target = e.target as HTMLInputElement;
  const files = target.files;
  if (!files) return;

  Array.from(files).forEach(file => {
    if (fileList.value.length >= 6) {
      showToast('最多上传6张图片');
      return;
    }
    if (!file.type.startsWith('image/')) {
      showToast('仅支持图片格式');
      return;
    }
    if (file.size / 1024 / 1024 >= 5) {
      showToast('图片大小不超过5MB');
      return;
    }

    const reader = new FileReader();
    reader.onload = (e) => {
      fileList.value.push({
        uid: Date.now() + Math.random(),
        file,
        preview: e.target?.result as string,
        status: 'uploading'
      });
      uploadFile(file, fileList.value.length - 1);
    };
    reader.readAsDataURL(file);
  });
  target.value = '';
};

// 上传文件
const uploadFile = async (file: File, index: number) => {
  try {
    const res = await uploadPostImageUsingPost({}, {}, file);
    if (res.data.code === 0 && res.data.data) {
      fileList.value[index] = {
        ...fileList.value[index],
        url: res.data.data.url,
        status: 'done'
      };
      formData.value.screenshotUrls = fileList.value
        .filter(f => f.status === 'done')
        .map(f => f.url);
    } else {
      throw new Error(res.data.message || '上传失败');
    }
  } catch (error: any) {
    fileList.value[index].status = 'error';
    showToast(`上传失败: ${error.message}`);
  }
};

// 删除文件
const removeFile = (index: number) => {
  fileList.value.splice(index, 1);
  formData.value.screenshotUrls = fileList.value
    .filter(f => f.status === 'done')
    .map(f => f.url);
};

// 自定义Toast提示
const showToast = (msg: string) => {
  const toast = document.createElement('div');
  toast.className = 'report-toast';
  toast.textContent = msg;
  document.body.appendChild(toast);
  setTimeout(() => {
    toast.classList.add('show');
  }, 10);
  setTimeout(() => {
    toast.classList.remove('show');
    setTimeout(() => {
      document.body.removeChild(toast);
    }, 300);
  }, 2000);
};

// 验证表单
const validateForm = (): boolean => {
  const newErrors: Record<string, string> = {};
  if (!formData.value.reportType) newErrors.reportType = '请选择具体的举报原因';
  if (!formData.value.targetType) newErrors.targetType = '缺少拦截目标类型';
  if (!formData.value.targetId) newErrors.targetId = '缺少拦截目标凭证';
  if (!formData.value.reason.trim()) newErrors.description = '详细说明不能为空';
  if (formData.value.reason.length > 500) newErrors.description = '详细说明不能超过500字符';
  errors.value = newErrors;
  return Object.keys(newErrors).length === 0;
};

// 提交表单
const handleSubmit = async () => {
  if (!validateForm()) return;

  const uploadingFiles = fileList.value.filter(file => file.status === 'uploading');
  if (uploadingFiles.length > 0) {
    showToast('照片正在上传中，请稍候');
    return;
  }

  submitting.value = true;
  try {
    const reportData = {
      reportType: parseInt(formData.value.reportType),
      targetType: parseInt(formData.value.targetType),
      targetId: parseInt(formData.value.targetId),
      reason: formData.value.reason,
      screenshotUrls: formData.value.screenshotUrls
    };
    const response = await addReportUsingPost(reportData);

    if (response.data.code === 0) {
      showToast('举报提交成功，我们会尽快处理！');
      resetForm();
      closeModal();
      emit('success');
    } else {
      showToast(response.data.message || '提交失败，请重试');
    }
  } catch (error: any) {
    showToast(error.message || '网络或系统异常');
  } finally {
    submitting.value = false;
  }
};

// 暴露方法
defineExpose({ openModal, closeModal, updateFormData });
</script>

<style scoped>
/* 终极毛玻璃遮罩 */
.report-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999999;
  padding: 20px;
}

/* 现代化弹窗容器 (更精致的阴影和圆角) */
.report-container {
  width: 100%;
  max-width: 440px;
  background: var(--surface-color, #ffffff);
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.2);
  display: flex;
  flex-direction: column;
  max-height: 85vh;
  animation: modal-pop 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

@keyframes modal-pop {
  from { opacity: 0; transform: scale(0.95) translateY(10px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}

/* 弹窗头部 */
.report-header {
  padding: 20px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
  position: relative;
}
.report-header::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 24px;
  right: 24px;
  height: 1px;
  background: var(--border-color, #f0f0f0);
}
.report-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-color, #333);
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}
.title-icon {
  color: #ff4d4f;
  font-size: 16px;
}
.report-close {
  background: var(--bg-color, #f5f5f5);
  border: none;
  font-size: 16px;
  color: var(--text-secondary, #999);
  cursor: pointer;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.2s;
}
.report-close:hover {
  background: var(--border-color, #e8e8e8);
  color: var(--text-color, #333);
  transform: rotate(90deg);
}

/* 弹窗主体 */
.report-body {
  padding: 24px;
  overflow-y: auto;
  flex: 1;
}
.report-body::-webkit-scrollbar {
  width: 6px;
}
.report-body::-webkit-scrollbar-thumb {
  background: var(--border-color, #e0e0e0);
  border-radius: 3px;
}

/* 表单项 */
.report-item {
  margin-bottom: 24px;
}
.report-item:last-child {
  margin-bottom: 0;
}
.report-label {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-color, #333);
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  gap: 4px;
}
.required {
  color: #ff4d4f;
}
.optional {
  font-weight: normal;
  font-size: 12px;
  color: var(--text-secondary, #999);
}

/* 智能对象展示 Badge */
.report-badge-container {
  background: var(--bg-color, #f8f9fa);
  padding: 12px 16px;
  border-radius: 12px;
  border: 1px solid var(--border-color, #f0f0f0);
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.badge-label {
  font-size: 13px;
  color: var(--text-secondary, #666);
}
.target-badge {
  display: flex;
  align-items: center;
  gap: 8px;
}
.target-type-tag {
  background: #e6f4ff;
  color: #1677ff;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 6px;
  font-weight: 500;
}
.target-id-text {
  font-size: 13px;
  color: var(--text-color, #333);
  font-family: monospace;
}

/* Chips 标签单选取代原生的 Select */
.report-type-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}
.report-type-chip {
  background: var(--bg-color, #f5f5f5);
  color: var(--text-color, #333);
  border: 1px solid transparent;
  padding: 10px 8px;
  border-radius: 8px;
  text-align: center;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  user-select: none;
}
.report-type-chip:hover {
  background: var(--border-color, #ebebeb);
}
.report-type-chip.active {
  background: #e6f4ff;
  color: #1677ff;
  border-color: #91caff;
  font-weight: 500;
}

/* 输入与选择原生托底样式 */
.report-select-wrap {
  position: relative;
}
.report-select, .report-input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid var(--border-color, #e5e5e5);
  border-radius: 10px;
  font-size: 14px;
  color: var(--text-color, #333);
  background: var(--surface-color, #fff);
  outline: none;
  transition: all 0.2s;
}
.report-select {
  appearance: none;
}
.select-arrow {
  position: absolute;
  right: 16px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--text-secondary, #999);
  pointer-events: none;
  font-size: 12px;
}
.report-select:focus, .report-input:focus {
  border-color: #1677ff;
  box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.1);
}
.disabled-placeholder::placeholder {
  color: var(--text-secondary, #bbb);
}

/* 渐变底色的现代 Textarea */
.textarea-wrapper {
  position: relative;
  border: 1px solid var(--border-color, #e5e5e5);
  border-radius: 12px;
  background: var(--bg-color, #fcfcfc);
  transition: all 0.2s;
}
.textarea-wrapper:focus-within {
  border-color: #1677ff;
  background: var(--surface-color, #fff);
  box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.1);
}
.report-textarea {
  width: 100%;
  padding: 14px 16px;
  min-height: 100px;
  background: transparent;
  border: none;
  font-size: 14px;
  color: var(--text-color, #333);
  resize: none;
  outline: none;
}
.report-word-count {
  position: absolute;
  bottom: 10px;
  right: 14px;
  font-size: 12px;
  color: var(--text-secondary, #999);
}
.report-word-count.near-limit {
  color: #faad14;
}

/* 相机上传方块群 */
.report-upload-wrap {
  margin-top: 8px;
}
.report-upload-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
.report-upload-btn {
  width: 76px;
  height: 76px;
  background: var(--bg-color, #f5f5f5);
  border: 1px dashed var(--border-color, #d9d9d9);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-secondary, #999);
  cursor: pointer;
  transition: all 0.2s;
}
.report-upload-btn:hover {
  border-color: #1677ff;
  color: #1677ff;
}
.upload-icon {
  font-size: 22px;
}
.report-file-input {
  display: none;
}

/* 预览与移除卡片 */
.report-upload-preview {
  position: relative;
  width: 76px;
  height: 76px;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
}
.report-upload-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.upload-mask {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(255,255,255,0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #1677ff;
}
.report-upload-remove {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 20px;
  height: 20px;
  background: rgba(0,0,0,0.5);
  color: #fff;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  backdrop-filter: blur(4px);
  transition: background 0.2s;
}
.report-upload-remove:hover {
  background: #ff4d4f;
}

/* 表单错误提示 */
.report-error {
  font-size: 12px;
  color: #ff4d4f;
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 4px;
}
.report-error::before {
  content: '\f071';
  font-family: "Font Awesome 5 Free";
  font-weight: 900;
}

/* 底部圆润的大胶囊按钮 */
.report-footer {
  padding: 16px 24px 20px;
  flex-shrink: 0;
}
.report-submit-btn {
  width: 100%;
  padding: 14px;
  background: linear-gradient(135deg, #1677ff, #3e8eff);
  color: #fff;
  border: none;
  border-radius: 30px; /* 超圆润 */
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  box-shadow: 0 6px 16px rgba(22, 119, 255, 0.25);
}
.report-submit-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(22, 119, 255, 0.35);
}
.report-submit-btn:active:not(:disabled) {
  transform: translateY(1px);
}
.report-submit-btn:disabled {
  background: #d9d9d9;
  box-shadow: none;
  transform: none;
  cursor: not-allowed;
  color: rgba(0,0,0,0.25);
}

/* 简洁轻量的 Toast */
.report-toast {
  position: fixed;
  top: 30px;
  left: 50%;
  transform: translateX(-50%) translateY(-20px);
  padding: 12px 24px;
  background: rgba(0,0,0,0.8);
  backdrop-filter: blur(10px);
  color: #fff;
  font-size: 14px;
  border-radius: 20px;
  opacity: 0;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  z-index: 1000000;
  box-shadow: 0 10px 30px rgba(0,0,0,0.2);
}
.report-toast.show {
  opacity: 1;
  transform: translateX(-50%) translateY(0);
}

/* 移动端细微调整 */
@media (max-width: 480px) {
  .report-container {
    width: 92%;
    border-radius: 24px;
  }
  .report-header {
    padding: 18px 20px;
  }
  .report-body {
    padding: 20px;
  }
  .report-type-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
