<template>
  <div id="activityEditPage" v-show="mounted" :class="{ mounted }">
    <a-card class="edit-card">
      <template #title>
        <div class="page-header">
          <h1 class="page-title">{{ isEdit ? '编辑活动' : '创建活动' }}</h1>
          <a-tooltip placement="right">
            <template #title>
              <div class="tip-content">
                <p>1. 标题简洁明了，不超过100字</p>
                <p>2. 内容支持富文本编辑</p>
                <p>3. 图片可以直接拖拽上传</p>
                <p>4. 活动过期时间必填</p>
              </div>
            </template>
            <ExclamationCircleOutlined style="margin-left: 8px; color: #666;" />
          </a-tooltip>
        </div>
      </template>

      <a-form
        ref="formRef"
        :model="formState"
        :rules="rules"
        layout="vertical"
        class="activity-form"
      >
        <!-- 标题 -->
        <a-form-item name="title" label="活动标题">
          <a-input
            v-model:value="formState.title"
            placeholder="请输入活动标题"
            :maxLength="100"
            show-count
          />
        </a-form-item>

        <!-- 封面图 -->
        <a-form-item name="coverUrl" label="封面图">
          <a-upload
            v-model:fileList="fileList"
            list-type="picture-card"
            :before-upload="beforeUpload"
            :customRequest="customUpload"
            :max-count="1"
          >
            <div v-if="fileList.length < 1">
              <PlusOutlined />
              <div style="margin-top: 8px">上传</div>
            </div>
          </a-upload>
        </a-form-item>

        <!-- 过期时间 -->
        <a-form-item name="expireTime" label="过期时间">
          <a-date-picker
            v-model:value="formState.expireTime"
            :show-time="{ format: 'HH:mm' }"
            format="YYYY-MM-DD HH:mm"
            :disabledDate="disabledDate"
            placeholder="请选择过期时间"
            style="width: 100%"
          />
        </a-form-item>

        <!-- 内容编辑区 -->
        <a-form-item name="content" label="活动内容">
          <div class="editor-wrapper">
            <div class="editor-container">
              <Toolbar
                class="editor-toolbar"
                :editor="editorRef"
                :defaultConfig="toolbarConfig"
                :mode="mode"
              />
              <Editor
                class="editor-content"
                v-model="formState.content"
                :defaultConfig="editorConfig"
                :mode="mode"
                @onCreated="handleCreated"
              />
            </div>
          </div>
        </a-form-item>

        <!-- 上传进度提示 -->
        <div v-if="uploading" class="upload-progress">
          <a-progress
            :percent="uploadProgress"
            :status="uploadProgress >= 100 ? 'success' : 'active'"
            :stroke-color="{ from: '#60c3d5', to: '#4c9aff' }"
          />
          <div class="progress-text">
            {{ uploadProgress >= 100 ? '处理中...' : '上传中...' }}
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="form-actions">
          <a-space>
            <a-button @click="handleCancel">取消</a-button>
            <a-button
              type="primary"
              :loading="submitting"
              @click="handleSubmit"
            >
              {{ isEdit ? '保存修改' : '发布活动' }}
            </a-button>
          </a-space>
        </div>
      </a-form>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, shallowRef, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal, Upload } from 'ant-design-vue'
import { PlusOutlined, ExclamationCircleOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import '@wangeditor/editor/dist/css/style.css'
import type { UploadProps } from 'ant-design-vue'
import type { Rule } from 'ant-design-vue/es/form'
import dayjs from 'dayjs'
import MarkdownContent from '@/components/MarkdownContent.vue'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { uploadPostImageUsingPost } from '@/api/pictureController'
import {
  addActivityUsingPost,
  getActivityByIdUsingGet
} from '@/api/activityController'

// Define interfaces for form state
interface FormState {
  id?: string;
  title: string;
  coverUrl: string;
  expireTime: dayjs.Dayjs | null;
  content: string;
  attachments: API.PostAttachment[];
}

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()
const mounted = ref(false)
const formRef = ref()
const submitting = ref(false)
const editorRef = shallowRef()
const mode = 'default'
const uploading = ref(false)
const uploadProgress = ref(0)

// 是否为编辑模式
const isEdit = computed(() => !!route.params.id)

// 表单状态
const formState = ref<FormState>({
  title: '',
  content: '',
  coverUrl: '',
  expireTime: null,
  attachments: []
})

// 文件列表
const fileList = ref<any[]>([])

// 表单验证规则
const rules: Record<string, Rule[]> = {
  title: [
    { required: true, message: '请输入活动标题', trigger: 'blur' },
    { min: 2, max: 100, message: '标题长度应在2-100个字符之间', trigger: 'blur' }
  ],
  coverUrl: [{ required: true, message: '请上传封面图' }],
  expireTime: [{ required: true, message: '请选择过期时间' }],
  content: [
    { required: true, message: '请输入活动内容' },
    { min: 10, message: '内容至少10个字符' }
  ]
}

// 禁用过去的日期
const disabledDate = (current: dayjs.Dayjs) => {
  return current && current < dayjs().startOf('day')
}

// 上传前校验
const beforeUpload: UploadProps['beforeUpload'] = (file) => {
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    message.error('只能上传图片文件!')
  }
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isLt2M) {
    message.error('图片必须小于2MB!')
  }
  return isImage && isLt2M
}

// 自定义上传
const customUpload = async ({ file, onSuccess, onError }: any) => {
  try {
    const formData = new FormData()
    formData.append('file', file)
    const res = await uploadPostImageUsingPost({}, {}, file)
    if (res.data?.data) {
      formState.value.coverUrl = res.data.data.url
      onSuccess()
      message.success('上传成功')
    } else {
      onError()
      message.error('上传失败')
    }
  } catch (error) {
    onError()
    message.error('上传失败')
  }
}

// 获取活动详情
const getActivityDetail = async () => {
  const id = route.params.id as string
  if (!id) return

  try {
    const res = await getActivityByIdUsingGet({ id })
    if (res.data?.data) {
      const activity = res.data.data
      formState.value = {
        id: activity.id,
        title: activity.title,
        content: activity.content,
        coverUrl: activity.coverUrl,
        expireTime: dayjs(activity.expireTime),
        attachments: activity.attachments
      }

      if (activity.coverUrl) {
        fileList.value = [
          {
            uid: '-1',
            name: 'cover.png',
            status: 'done',
            url: activity.coverUrl
          }
        ]
      }

      mounted.value = true
    }
  } catch (error) {
    message.error('获取活动详情失败')
    router.back()
  }
}

// 图片压缩函数
const compressImage = (file: File): Promise<File> => {
  return new Promise((resolve, reject) => {
    // 如果文件小于1MB，直接转换为webp
    if (file.size / 1024 / 1024 < 1) {
      const reader = new FileReader();
      reader.readAsDataURL(file);
      reader.onload = (e) => {
        const img = new Image();
        img.src = e.target?.result as string;
        img.onload = () => {
          const canvas = document.createElement('canvas');
          canvas.width = img.width;
          canvas.height = img.height;
          const ctx = canvas.getContext('2d');
          ctx?.drawImage(img, 0, 0);
          canvas.toBlob(
            (blob) => {
              if (!blob) {
                reject(new Error('图片转换失败'));
                return;
              }
              const webpFile = new File([blob], file.name.replace(/\.[^/.]+$/, '.webp'), {
                type: 'image/webp',
                lastModified: Date.now(),
              });
              resolve(webpFile);
            },
            'image/webp',
            1.0
          );
        };
      };
      return;
    }

    const maxWidth = 1280;
    const maxHeight = 720;
    const quality = 0.75;

    const reader = new FileReader();
    reader.readAsDataURL(file);
    reader.onload = (e) => {
      const img = new Image();
      img.src = e.target?.result as string;
      img.onload = () => {
        let width = img.width;
        let height = img.height;

        if (width > maxWidth || height > maxHeight) {
          const ratio = Math.min(maxWidth / width, maxHeight / height);
          width *= ratio;
          height *= ratio;
        }

        const canvas = document.createElement('canvas');
        canvas.width = width;
        canvas.height = height;
        const ctx = canvas.getContext('2d');
        ctx?.drawImage(img, 0, 0, width, height);

        canvas.toBlob(
          (blob) => {
            if (!blob) {
              reject(new Error('图片压缩失败'));
              return;
            }
            const webpFile = new File([blob], file.name.replace(/\.[^/.]+$/, '.webp'), {
              type: 'image/webp',
              lastModified: Date.now(),
            });
            resolve(webpFile);
          },
          'image/webp',
          quality
        );
      };
    };
  });
};

// 更新编辑器配置中的图片上传部分
const editorConfig = {
  placeholder: '请输入内容...',
  html: true,
  autoFocus: true,
  readOnly: false,
  scroll: true,
  maxLength: 50000,
  MENU_CONF: {
    uploadImage: {
      // 自定义图片上传
      async customUpload(file: File, insertFn: any) {
        try {
          uploading.value = true;
          uploadProgress.value = 0;

          // 压缩图片
          const compressedFile = await compressImage(file);
          uploadProgress.value = 30;

          const res = await uploadPostImageUsingPost(
            {},
            {},
            compressedFile
          );

          uploadProgress.value = 90;

          if (res.data.code === 0 && res.data.data) {
            const imageCount = formState.value.attachments.length;
            const marker = `{img-${imageCount + 1}}`;

            // 添加到附件列表
            formState.value.attachments.push({
              type: 1,
              url: res.data.data.thumbnailUrl,
              name: file.name,
              size: compressedFile.size,
              sort: imageCount + 1
            });

            // 插入图片但保存标记信息
            if (editorRef.value) {
              insertFn(res.data.data.url, marker, res.data.data.url);
              // 减少延时时间
              setTimeout(() => {
                const editor = editorRef.value;
                if (editor) {
                  editor.blur();
                  editor.focus();
                }
              }, 50);
            }
          } else {
            message.error('图片上传失败');
          }
        } catch (error: any) {
          console.error('图片上传失败:', error);
          message.error('图片上传失败: ' + error.message);
        } finally {
          uploading.value = false;
          uploadProgress.value = 0;
        }
      },
      // 监听图片删除事件
      onDeleted: (node: any) => {
        if (node.type === 'image') {
          const marker = node.alt;
          const index = parseInt(marker.replace('{img-', '').replace('}', '')) - 1;

          // 从附件列表中移除
          if (index >= 0 && index < formState.value.attachments.length) {
            // 获取当前HTML内容
            let htmlContent = editorRef.value?.getHtml() || '';

            // 删除对应的附件
            formState.value.attachments.splice(index, 1);

            // 重新扫描编辑器中的图片和标记
            const imgRegex = /<img[^>]*alt="({img-\d+})"[^>]*>/g;
            const markers = [];
            let match;

            // 收集所有现存的图片标记
            while ((match = imgRegex.exec(htmlContent)) !== null) {
              markers.push(match[1]);
            }

            // 按顺序更新附件列表的排序
            markers.forEach((marker, i) => {
              const oldIndex = parseInt(marker.replace('{img-', '').replace('}', '')) - 1;
              if (oldIndex >= 0 && oldIndex < formState.value.attachments.length) {
                formState.value.attachments[oldIndex].sort = i + 1;
              }
            });

            // 更新编辑器中的标记
            markers.forEach((oldMarker, i) => {
              const newMarker = `{img-${i + 1}}`;
              if (oldMarker !== newMarker) {
                const oldImgRegex = new RegExp(`<img[^>]*alt="${oldMarker}"[^>]*>`, 'g');
                htmlContent = htmlContent.replace(oldImgRegex, (match) => {
                  return match.replace(oldMarker, newMarker);
                });
              }
            });

            // 设置更新后的HTML内容
            if (editorRef.value) {
              editorRef.value.setHtml(htmlContent);
            }
          }
        }
      }
    }
  },
  EXTEND_CONF: {
    image: {
      draggable: false,
      resizable: false,
      customConfig: {
        allowDrag: false
      }
    }
  },
  hoverbarKeys: {
    image: {
      menuKeys: ['deleteImage']
    }
  }
};

// 更新工具栏配置
const toolbarConfig = {
  excludeKeys: [
    'group-video',  // 禁用视频
    'insertTable',  // 禁用表格
    'imageWidth',   // 禁用图片宽度设置
    'imageHeight',  // 禁用图片高度设置
    'imageSize'     // 禁用图片大小设置
  ]
};

// 编辑器创建完成时的回调
const handleCreated = (editor: any) => {
  editorRef.value = editor
  setTimeout(() => {
    editor.focus()
  }, 100)
  if (isEdit.value) {
    getActivityDetail()
  }
}

// 组件销毁时，也及时销毁编辑器
onBeforeUnmount(() => {
  const editor = editorRef.value
  if (editor == null) return
  editor.destroy()
})

// 移除图片
const removeImage = (index: number) => {
  const marker = `{img-${index + 1}}`
  const imgRegex = new RegExp(`<img[^>]*alt="${marker}"[^>]*>`, 'g')
  const html = editorRef.value?.getHtml() || ''
  editorRef.value?.setHtml(html.replace(imgRegex, ''))

  formState.value.attachments.splice(index, 1)

  formState.value.attachments.forEach((attach, i) => {
    const oldMarker = `{img-${attach.sort}}`
    const newMarker = `{img-${i + 1}}`
    const oldImgRegex = new RegExp(`<img[^>]*alt="${oldMarker}"[^>]*>`, 'g')
    const html = editorRef.value?.getHtml() || ''
    editorRef.value?.setHtml(html.replace(oldImgRegex, (match) => {
      return match.replace(oldMarker, newMarker)
    }))
    attach.sort = i + 1
  })
}

// 提交表单
const handleSubmit = async () => {
  try {
    await formRef.value?.validate()
    submitting.value = true

    // 获取HTML内容
    let htmlContent = editorRef.value?.getHtml() || '';
    let editorContent = htmlContent;

    // 清理并重新排序附件列表
    const tempAttachments = [...formState.value.attachments];
    formState.value.attachments = [];

    // 扫描编辑器中的图片标记
    const imgRegex = /<img[^>]*alt="({img-\d+})"[^>]*>/g;
    const foundMarkers = new Set();
    let match;
    let newIndex = 1;

    // 第一遍扫描：收集所有存在的图片标记
    while ((match = imgRegex.exec(htmlContent)) !== null) {
      const marker = match[1];
      const oldIndex = parseInt(marker.replace('{img-', '').replace('}', '')) - 1;
      if (oldIndex >= 0 && oldIndex < tempAttachments.length) {
        foundMarkers.add(oldIndex);
      }
    }

    // 重新构建附件列表和更新标记
    Array.from(foundMarkers).sort((a, b) => a - b).forEach((oldIndex) => {
      const attachment = tempAttachments[oldIndex];
      const oldMarker = `{img-${oldIndex + 1}}`;
      const newMarker = `{img-${newIndex}}`;

      // 更新HTML中的标记
      const markerRegex = new RegExp(oldMarker.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'g');
      editorContent = editorContent.replace(markerRegex, newMarker);

      // 更新附件列表
      formState.value.attachments.push({
        ...attachment,
        sort: newIndex
      });

      newIndex++;
    });

    // 将HTML中的图片标记转换为纯文本标记
    editorContent = editorContent
      .replace(/<img[^>]*alt="({img-\d+})"[^>]*>/g, '\n$1\n')
      .replace(/<p>/g, '')
      .replace(/<\/p>/g, '\n')
      .replace(/\n\n+/g, '\n\n')
      .trim();

    // 验证所有图片标记是否存在
    for (let i = 0; i < formState.value.attachments.length; i++) {
      const marker = `{img-${i + 1}}`;
      if (!editorContent.includes(marker)) {
        message.error(`内容中缺少图片标记 ${marker}`);
        submitting.value = false;
        return;
      }
    }

    const requestBody = {
      ...formState.value,
      content: editorContent,
      expireTime: formState.value.expireTime?.format('YYYY-MM-DDTHH:mm:ss.SSSZ')
    }

    if (isEdit.value) {
      message.warning('编辑功能暂未开放，请等待后续更新')
      return
    }

    const res = await addActivityUsingPost(requestBody)

    if (res.data?.data) {
      message.success('发布成功')
      router.push(`/activity/detail/${res.data.data}`)
    }
  } catch (error: any) {
    if (error?.errorFields) {
      message.error('请检查表单填写是否正确')
    } else {
      message.error('发布失败')
    }
  } finally {
    submitting.value = false
  }
}

// 取消
const handleCancel = () => {
  Modal.confirm({
    title: '确定要取消吗？',
    content: '已编辑的内容将不会保存',
    okText: '确定',
    cancelText: '取消',
    onOk: () => router.back()
  })
}

// 生命周期
onMounted(() => {
  if (isEdit.value) {
    getActivityDetail()
  } else {
    mounted.value = true
  }
})
</script>

<style scoped>
#activityEditPage {
  opacity: 0;
  transition: opacity 0.3s ease;

  &.mounted {
    opacity: 1;
  }
}

.edit-card {
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.page-header {
  padding: 8px 0;

  .page-title {
    font-size: 20px;
    font-weight: 600;
    color: #1a1a1a;
    margin: 0;
  }
}

.activity-form {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px 0;

  :deep(.ant-form-item) {
    margin-bottom: 24px;
  }

  :deep(.ant-form-item-label) {
    padding-bottom: 8px;

    label {
      font-size: 14px;
      font-weight: 500;
      color: #1a1a1a;
    }
  }

  :deep(.ant-input) {
    border-radius: 8px;
    padding: 8px 12px;
    font-size: 14px;

    &:hover,
    &:focus {
      border-color: #4c9aff;
    }
  }

  :deep(.ant-upload-list-picture-card-container),
  :deep(.ant-upload.ant-upload-select-picture-card) {
    width: 200px;
    height: 120px;
    border-radius: 8px;
  }
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 32px;

  .ant-btn {
    min-width: 100px;
    height: 40px;
    border-radius: 20px;
    font-size: 14px;
    font-weight: 500;
    transition: all 0.3s ease;

    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    }

    &:active {
      transform: translateY(1px);
    }
  }

  .ant-btn-primary {
    background: linear-gradient(135deg, #60c3d5 0%, #4c9aff 100%);
    border: none;

    &:hover {
      background: linear-gradient(135deg, #4c9aff 0%, #60c3d5 100%);
    }
  }
}

.editor-wrapper {
  position: relative;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  padding: 8px;
}

.editor-container {
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  overflow: hidden;
}

.editor-toolbar {
  border-bottom: 1px solid #d9d9d9;
}

.editor-content {
  min-height: 300px;
}

:deep(.w-e-text-container) {
  background-color: #fff !important;
  height: auto !important;
  min-height: 300px !important;

  [data-slate-editor] {
    padding: 12px;
  }

  /* 图片容器基础样式 */
  .w-e-image-container {
    position: relative !important;
    display: inline-block !important;
    width: 33.33% !important;
    padding: 0 !important;
    margin: 4px auto !important;
    aspect-ratio: 1 !important;
    overflow: hidden !important;
  }

  /* 图片基础样式 */
  .w-e-image-container img {
    position: absolute !important;
    top: 0 !important;
    left: 0 !important;
    width: 100% !important;
    height: 100% !important;
    object-fit: cover !important;
    border-radius: 8px !important;
    box-shadow: 0 2px 8px rgba(0,0,0,0.1) !important;
  }

  /* 三张图片布局 */
  p:has(.w-e-image-container + .w-e-image-container + .w-e-image-container) {
    display: flex !important;
    flex-wrap: nowrap !important;
    justify-content: space-between !important;
    align-items: flex-start !important;
    gap: 4px !important;
    padding: 0 !important;
    margin: 4px 0 !important;
    width: 100% !important;

    .w-e-image-container {
      flex: 0 0 calc((100% - 8px) / 3) !important;
      width: calc((100% - 8px) / 3) !important;
      margin: 0 !important;
    }
  }

  /* 两张图片布局 */
  p:has(.w-e-image-container + .w-e-image-container):not(:has(.w-e-image-container + .w-e-image-container + .w-e-image-container)) {
    display: flex !important;
    flex-wrap: nowrap !important;
    justify-content: flex-start !important;
    align-items: flex-start !important;
    gap: 4px !important;
    padding: 0 !important;
    margin: 4px 0 !important;
    width: 100% !important;

    .w-e-image-container {
      flex: 0 0 calc((100% - 4px) / 3) !important;
      width: calc((100% - 4px) / 3) !important;
      margin: 0 !important;
    }
  }

  /* 单张图片布局 */
  p:has(.w-e-image-container:only-child) {
    text-align: left !important;
    margin: 4px 0 !important;

    .w-e-image-container {
      width: calc((100% - 8px) / 3) !important;
      margin: 0 !important;
    }
  }
}

:deep(.w-e-toolbar) {
  background-color: #fafafa !important;
  border-bottom: 1px solid #f0f0f0 !important;
  flex-wrap: wrap !important;
  padding: 4px !important;

  .w-e-bar-item {
    margin: 4px !important;
  }

  .w-e-bar-divider {
    margin: 4px !important;
  }
}

:deep(.w-e-text-container [data-slate-editor]) {
  padding: 12px !important;
}

:deep(.w-e-text-container [data-slate-editor] p) {
  margin: 8px 0 !important;
}

/* 上传进度样式 */
.upload-progress {
  margin: 16px 0;
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f9fafb;
}

.progress-text {
  margin-top: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #6b7280;
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .activity-form {
    padding: 16px 0;
  }

  .form-actions {
    margin-top: 24px;

    .ant-btn {
      height: 36px;
      min-width: 80px;
      font-size: 13px;
    }
  }

  .editor-wrapper {
    flex-direction: column;
    padding: 4px;
  }

  .editor-content {
    height: 300px;
  }

  .upload-progress {
    margin: 12px 0;
    padding: 12px;
  }

  .progress-text {
    font-size: 13px;
  }
}
</style>
