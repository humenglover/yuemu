<template>
  <div id="postEditPage">
    <a-card class="edit-card" :bordered="false">
      <template #title>
        <div class="page-header">
          <h1>{{ isEdit ? '编辑帖子' : '发布帖子' }}</h1>
          <a-tooltip placement="right">
            <template #title>
              <div class="tip-content">
                <div class="tip-title">发帖小贴士</div>
                <div class="tip-text">添加图片可以提高帖子通过率哦 ~</div>
              </div>
            </template>
            <ExclamationCircleOutlined class="tip-icon" />
          </a-tooltip>
        </div>
      </template>

      <a-form :model="postForm" layout="vertical">
        <!-- 标题 -->
        <a-form-item label="标题" required>
          <a-input v-model:value="postForm.title" placeholder="请输入标题" />
        </a-form-item>

        <!-- 分类 -->
        <a-form-item label="分类" required>
          <a-select v-model:value="postForm.category" placeholder="请选择分类">
            <a-select-option
              v-for="category in categories"
              :key="category"
              :value="category"
            >
              {{ category }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <!-- 内容编辑区 -->
        <a-form-item label="内容" required>
          <div class="editor-wrapper">
            <!-- wangEditor 编辑器 -->
            <div class="editor-container">
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
        </a-form-item>

        <!-- 上传进度提示 -->
        <div v-if="uploading" class="upload-progress">
          <a-progress
            :percent="uploadProgress"
            :status="uploadProgress >= 100 ? 'success' : 'active'"
            :stroke-color="{ from: '#ff8e53', to: '#ff6b6b' }"
          />
          <div class="progress-text">
            {{ uploadProgress >= 100 ? '处理中...' : '上传中...' }}
          </div>
        </div>

        <!-- 提交按钮 -->
        <div class="form-actions">
          <a-space>
            <a-button class="cancel-button" @click="router.back()">取消</a-button>
            <a-button
              class="submit-button"
              type="primary"
              @click="handleSubmit"
              :loading="submitting"
            >
              {{ isEdit ? '保存' : '发布' }}
            </a-button>
          </a-space>
        </div>
      </a-form>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, shallowRef, onBeforeUnmount, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { DeleteOutlined, ExclamationCircleOutlined } from '@ant-design/icons-vue'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import '@wangeditor/editor/dist/css/style.css'
import { addPostUsingPost, updatePostUsingPost, getPostByIdUsingGet } from '@/api/postController'
import { uploadPostImageUsingPost } from '@/api/pictureController'
import { listCategoryByTypeUsingGet } from '@/api/categoryController.ts'

const route = useRoute()
const router = useRouter()
const editorRef = shallowRef()
const submitting = ref(false)
const mode = 'default'
const uploading = ref(false)
const uploadProgress = ref(0)

// 图片压缩函数
const compressImage = (file: File): Promise<File> => {
  return new Promise((resolve, reject) => {
    // 如果文件小于1MB，直接返回但转换为webp
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
    const quality = 0.75; // webp可以用稍高的质量因为其压缩效率更好

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

        // 转换为WebP
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

// 编辑器配置
const editorConfig = {
  placeholder: '请输入内容...',
  html: true,
  autoFocus: true, // 禁用自动获取焦点
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
            const imageCount = postForm.value.attachments.length;
            const marker = `{img-${imageCount + 1}}`;

            // 添加到附件列表
            postForm.value.attachments.push({
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
      // 配置表情菜单
      emoticon: {
        // 选择表情后的回调
        onSelect: () => {
          // 确保光标在表情后面
          setTimeout(() => {
            const editor = editorRef.value;
            if (editor) {
              editor.blur();
              editor.focus();
            }
          }, 100);
        }
      },
      // 监听图片删除事件
      onDeleted: (node: any) => {
        if (node.type === 'image') {
          const marker = node.alt;
          const index = parseInt(marker.replace('{img-', '').replace('}', '')) - 1;

          // 从附件列表中移除
          if (index >= 0 && index < postForm.value.attachments.length) {
            // 获取当前HTML内容
            let htmlContent = editorRef.value?.getHtml() || '';

            // 删除对应的附件
            postForm.value.attachments.splice(index, 1);

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
              if (oldIndex >= 0 && oldIndex < postForm.value.attachments.length) {
                postForm.value.attachments[oldIndex].sort = i + 1;
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
  // 禁用图片拖拽和大小调整
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
  },
}

// 工具栏配置
const toolbarConfig = {
  excludeKeys: [
    'group-video',  // 禁用视频
    'insertTable',  // 禁用表格
    'imageWidth',   // 禁用图片宽度设置
    'imageHeight',  // 禁用图片高度设置
    'imageSize'     // 禁用图片大小设置
  ]
}

// 从路由状态获取帖子数据
const initPostData = () => {
  const postData = route.query.post
  if (postData) {
    const post = JSON.parse(postData as string)
    postForm.value = {
      title: post.title || '',
      category: post.category,
      attachments: [], // 清空附件列表
      content: ''     // 清空内容
    }
    // 设置编辑器内容
    if (editorRef.value) {
      // 清空编辑器内容
      editorRef.value.setHtml('')
    }
  } else if (isEdit.value) {
    message.error('获取帖子数据失败')
    router.back()
  }
}

// 编辑器创建完成时的回调
const handleCreated = (editor: any) => {
  editorRef.value = editor

  // 移动端优化
  if (/mobile|android|iphone|ipad|phone/i.test(navigator.userAgent)) {
    const editorDom = editor.getEditableContainer();
    if (editorDom) {
      // 只设置必要的样式
      editorDom.style.setProperty('-webkit-user-select', 'text');
      editorDom.style.setProperty('user-select', 'text');

      // 监听编辑器区域的触摸事件
      editorDom.addEventListener('touchstart', () => {
        setTimeout(() => {
          editor.focus();
        }, 0);
      });
    }
  }

  // 如果是编辑模式，加载帖子数据
  if (isEdit.value) {
    initPostData()
  }
}
const categories = ref([])
// 获取帖子分类列表
const fetchCategories = async () => {
  try {
    const res = await listCategoryByTypeUsingGet({ type: 1 }) // 1 表示帖子分类
    if (res.data?.code === 0 && res.data.data) {
      categories.value = res.data.data
    }
  } catch (error: any) {
    console.error('获取分类列表失败:', error)
    message.error('获取分类列表失败')
  }
}

onMounted(async () => {
  await fetchCategories()
})

// 组件销毁时，也及时销毁编辑器
onBeforeUnmount(() => {
  const editor = editorRef.value
  if (editor == null) return
  editor.destroy()
})

const isEdit = computed(() => !!route.params.id)

const postForm = ref({
  title: '',
  content: '',
  category: undefined,
  attachments: [] as API.PostAttachment[]
})

// 计算预览内容
const previewContent = computed(() => {
  let content = postForm.value.content

  // 替换图片标记为 Markdown 格式
  postForm.value.attachments.forEach((attach, index) => {
    const marker = `{img-${index + 1}}`
    content = content.replace(marker, `![${attach.name}](${attach.url})`)
  })

  return content
})

// 移除图片
const removeImage = (index: number) => {
  const marker = `{img-${index + 1}}`
  // 移除图片元素
  const imgRegex = new RegExp(`<img[^>]*alt="${marker}"[^>]*>`, 'g')
  const html = editorRef.value?.getHtml() || ''
  editorRef.value?.setHtml(html.replace(imgRegex, ''))

  postForm.value.attachments.splice(index, 1)

  // 重新排序剩余图片的标记
  postForm.value.attachments.forEach((attach, i) => {
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
  if (!postForm.value.title?.trim()) {
    message.warning('请输入标题')
    return
  }
  if (!postForm.value.category) {
    message.warning('请选择分类')
    return
  }

  // 获取HTML内容
  let htmlContent = editorRef.value?.getHtml() || '';
  let editorContent = htmlContent;

  // 清理并重新排序附件列表
  const tempAttachments = [...postForm.value.attachments];
  postForm.value.attachments = [];

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
    postForm.value.attachments.push({
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

  if (!editorContent?.trim()) {
    message.warning('请输入内容')
    return
  }

  submitting.value = true
  try {
    const submitData = {
      ...postForm.value,
      content: editorContent
    }
    if (isEdit.value) {
      await updatePostUsingPost({
        ...submitData,
        id: route.params.id as string
      })
      message.success('更新成功')
    } else {
      await addPostUsingPost(submitData)
      message.success('发布成功，等待审核')
    }
    router.push('/forum')
  } catch (error: any) {
    message.error((isEdit.value ? '更新' : '发布') + '失败：' + error.message)
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
#postEditPage {
  width: 100%;
  margin: 0;
  padding: 0;
}

.edit-card {
  border-radius: 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);

  :deep(.ant-card-body) {
    padding: 16px;
  }

  @media screen and (max-width: 768px) {
    margin: 0 -20px;
  }
}

.page-header h1 {
  font-size: 24px;
  font-weight: 600;
  margin: 0;
}

.editor-wrapper {
  position: relative;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  padding: 8px;
}

.image-preview {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin: 16px 0;
}

.image-item {
  position: relative;
  border-radius: 8px;
  overflow: hidden;
  aspect-ratio: 1;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  .image-actions {
    position: absolute;
    top: 8px;
    right: 8px;
    background: rgba(0, 0, 0, 0.5);
    color: white;
    padding: 4px;
    border-radius: 4px;
    cursor: pointer;
    opacity: 0;
    transition: opacity 0.3s;
  }

  &:hover .image-actions {
    opacity: 1;
  }
}

.form-actions {
  margin-top: 24px;
  text-align: right;

  .cancel-button,
  .submit-button {
    height: 40px;
    padding: 0 24px;
    border-radius: 20px;
    font-size: 14px;
    font-weight: 500;
    transition: all 0.3s ease;
  }

  .cancel-button {
    border-color: #e2e8f0;

    &:hover {
      border-color: #ff8e53;
      color: #ff8e53;
    }
  }

  .submit-button {
    background: linear-gradient(135deg, #ff8e53 0%, #ff6b6b 100%);
    border: none;
    box-shadow: 0 4px 12px rgba(255, 107, 107, 0.2);

    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 6px 16px rgba(255, 107, 107, 0.3);
    }

    &:active {
      transform: translateY(1px);
    }
  }
}

/* 响应式调整 */
@media screen and (max-width: 768px) {
  #postEditPage {
    padding: 0;
    margin: 0;
  }

  .edit-card {
    border-radius: 0;
  }

  .editor-wrapper {
    flex-direction: column;
    padding: 4px;
  }

  .editor-content {
    height: 300px;
  }

  .image-preview {
    grid-template-columns: repeat(3, 1fr);
    gap: 8px;
  }

  .upload-progress {
    margin: 12px 0;
    padding: 12px;
    border-radius: 8px;
  }

  .progress-text {
    font-size: 13px;
  }

  .form-actions {
    .cancel-button,
    .submit-button {
      height: 36px;
      padding: 0 20px;
      font-size: 13px;
    }
  }
}

/* wangEditor 相关样式 */
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
    -webkit-user-select: text !important;
    user-select: text !important;
    -webkit-tap-highlight-color: rgba(0, 0, 0, 0.1) !important;
    cursor: text !important;
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

  .progress-text {
    margin-top: 8px;
    text-align: center;
    color: #666;
  }
}

.page-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.tip-icon {
  font-size: 18px;
  color: #ff8e53;
  cursor: help;
  transition: all 0.3s ease;
}

.tip-icon:hover {
  transform: scale(1.1);
}

.tip-content {
  padding: 4px;
}

.tip-title {
  font-size: 14px;
  font-weight: 500;
  color: #fff;
  margin-bottom: 4px;
}

.tip-text {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.85);
  line-height: 1.5;
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .tip-icon {
    font-size: 16px;
  }
}
</style>
