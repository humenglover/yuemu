<template>
  <div class="album-detail-container">
    <!-- 动态背景 -->
    <div class="dynamic-bg">
      <div id="nm-container"></div>
    </div>

    <!-- 浮动的上传按钮 -->
    <button v-if="isOwner" class="floating-upload-btn" @click="showUploadModal = true">
      <span class="btn-icon">➕</span>
    </button>

    <!-- 爱心墙照片展示 -->
    <div class="heart-wall">
      <div v-if="pictures.length === 0" class="empty-state">
        <div class="empty-icon">🌟</div>
        <p>{{isOwner ? '还没有照片哦，快来上传吧~' : '还没有照片哦~'}}</p>
      </div>
      <div v-else class="picture-grid">
        <div v-for="picture in pictures" :key="picture.id" class="picture-item" @click="openPreviewModal(picture)">
          <img :src="picture.thumbnailUrl" :alt="picture.name" loading="lazy">
          <div v-if="isOwner" class="picture-overlay">
            <button class="delete-btn" @click.stop="handleDeletePicture(picture)">
              <span class="delete-icon">✖</span>
            </button>
          </div>
          <div class="picture-info">
            <span class="picture-size">{{formatFileSize(picture.picSize)}}</span>
            <span class="picture-dimensions">{{picture.picWidth}} × {{picture.picHeight}}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 图片预览模态框 -->
    <Teleport to="body">
      <div v-if="previewModalVisible" class="custom-preview-overlay" @click="handlePreviewClose">
        <div class="custom-preview-container" @click.stop>
          <div
            class="custom-preview-content"
            @touchstart="handleTouchStart"
            @touchmove="handleTouchMove"
            @touchend="handleTouchEnd"
          >
            <button class="close-button" @click="handlePreviewClose">
              <svg viewBox="0 0 24 24">
                <path fill="currentColor" d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/>
              </svg>
            </button>

            <button class="nav-button prev" @click="showPreviousImage" v-if="currentImageIndex > 0">
              <svg viewBox="0 0 24 24">
                <path fill="currentColor" d="M15.41 7.41L14 6l-6 6 6 6 1.41-1.41L10.83 12z"/>
              </svg>
            </button>

            <img
              :src="selectedPicture?.url"
              class="preview-image"
              :style="{ transform: `translateX(${swipeOffset}px)` }"
              loading="eager"
            />

            <button class="nav-button next" @click="showNextImage" v-if="currentImageIndex < pictures.length - 1">
              <svg viewBox="0 0 24 24">
                <path fill="currentColor" d="M10 6L8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z"/>
              </svg>
            </button>

            <div class="image-info">
              <div class="image-counter">{{ currentImageIndex + 1 }} / {{ pictures.length }}</div>
              <div class="image-details" v-if="selectedPicture">
                <span>{{formatFileSize(selectedPicture.picSize)}}</span>
                <span>{{selectedPicture.picWidth}} × {{selectedPicture.picHeight}}</span>
                <span>{{selectedPicture.picFormat.toUpperCase()}}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 上传照片模态框 -->
    <div v-if="showUploadModal" class="modal-overlay">
      <div class="modal-content">
        <div class="modal-header">
          <h2>✨ 添加新照片</h2>
          <button class="modal-close" @click="showUploadModal = false">×</button>
        </div>
        <div class="modal-body">
          <div class="upload-area" @click="handleUploadClick" @drop.prevent="handleFileDrop" @dragover.prevent>
            <div v-if="!selectedFiles.length" class="upload-placeholder">
              <span class="upload-icon">🎞️</span>
              <p>点击或拖拽照片到此处上传</p>
              <p class="upload-tip">
                一次最多上传9张照片~
                <br>
                <span :class="{ 'limit-warning': pictures.length >= MAX_PHOTOS }">
                  ({{ pictures.length }}/{{ MAX_PHOTOS }})
                </span>
              </p>
            </div>
            <div v-else class="upload-preview">
              <div v-for="(file, index) in previewFiles" :key="index" class="preview-item">
                <img :src="file.preview" :alt="file.name">
                <button class="remove-preview" @click.stop="removeFile(index)">×</button>
                <div v-if="file.uploading" class="upload-progress">
                  <div class="progress-bar" :style="{ width: file.progress + '%' }"></div>
                  <span class="progress-text">{{ file.progress }}%</span>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="showUploadModal = false">取消</button>
          <button class="btn-confirm" @click="handleUpload" :disabled="selectedFiles.length === 0 || isUploading">
            {{ isUploading ? '上传中...' : '开始上传' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 隐藏的文件输入 -->
    <input
      type="file"
      ref="fileInput"
      style="display: none"
      accept="image/*"
      multiple
      @change="handleFileChange"
    >

    <!-- 删除照片确认模态框 -->
    <div v-if="showDeleteModal" class="modal-overlay">
      <div class="modal-content delete-modal">
        <div class="delete-modal-body">
          <div class="delete-preview">
            <img :src="selectedDeletePicture?.url" alt="要删除的照片">
          </div>
          <div class="delete-confirm-content">
            <div class="delete-icon">
              <span>(｡•́︿•̀｡)</span>
            </div>
            <div class="delete-message">
              <h3>确定要删除这张照片吗？</h3>
              <p>删除后将无法恢复</p>
            </div>
            <div class="delete-actions">
              <button class="btn-cancel" @click="showDeleteModal = false">取消</button>
              <button class="btn-delete" @click="confirmDeletePicture">删除</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import $ from 'jquery'
import {
  getTimeAlbumByIdUsingGet,
  uploadHeartWallPicturesUsingPost,
  getHeartWallPicturesUsingGet,
  deleteHeartWallPictureUsingPost,
} from '@/api/timeAlbumController'
import { message } from 'ant-design-vue'

const route = useRoute()
const router = useRouter()
const albumId = route.params.id as string

// 从 URL 参数中获取 isOwner
const isOwner = computed(() => {
  return route.query.isOwner === 'true'
})

// 定义变量类型
interface Album {
  albumName: string
  description: string
  id: number
}

interface Picture {
  id: number
  url: string
  thumbnailUrl: string
  name: string
  createTime: string
  picWidth: number
  picHeight: number
  picSize: string
  picFormat: string
}

interface PreviewFile extends File {
  preview: string
  progress: number
  uploading: boolean
}

// 定义响应式变量
const album = ref<Album | null>(null)
const pictures = ref<Picture[]>([])
const showUploadModal = ref(false)
const selectedFiles = ref<File[]>([])
const previewFiles = ref<PreviewFile[]>([])
const fileInput = ref<HTMLInputElement | null>(null)
const previewModalVisible = ref(false)
const selectedPicture = ref<Picture | null>(null)
const currentImageIndex = ref(0)
const swipeOffset = ref(0)
const touchStartX = ref(0)
const touchEndX = ref(0)
const isSwiping = ref(false)
const isUploading = ref(false)

// 修改常量
const MAX_PHOTOS = 100
const MAX_UPLOAD_PER_TIME = 9
const MAX_FILE_SIZE = 1 * 1024 * 1024 // 修改为1MB
const MAX_WIDTH = 1920
const MAX_HEIGHT = 1080
const QUALITY = 0.85 // 保持较好的压缩质量

// 添加监听器
watch(pictures, (newVal: Picture[]) => {
  console.log('pictures数组发生变化:', newVal.length)
}, { deep: true })

// 定义类型
interface Renderer {
  SNOW_COUNT: {
    INIT: number
    DELTA: number
  }
  BACKGROUND_COLOR: string
  INIT_HUE: number
  DELTA_HUE: number
  $container: JQuery
  width: number
  height: number
  center: { x: number; y: number }
  countRate: number
  canvas: HTMLCanvasElement
  context: CanvasRenderingContext2D
  radius: number
  hue: number
  snows: Snow[]
  init: () => void
  setParameters: () => void
  reconstructMethod: () => void
  createSnow: (count: number, toRandomize: boolean) => void
  render: () => void
}

interface Snow {
  width: number
  height: number
  center: { x: number; y: number }
  canvas?: HTMLCanvasElement
  context?: CanvasRenderingContext2D
  radius: number
  length: number
  topRadius: number
  x: number
  y: number
  vx: number
  vy: number
  deltaRotate: number
  scale: number
  deltaScale: number
  rotate: number
  init: (toRandomize: boolean) => void
  setParameters: (toRandomize: boolean) => void
  getRandomValue: (range: { MIN: number; MAX: number }) => number
  createSnow: () => void
  render: (context: CanvasRenderingContext2D) => boolean
}

// 获取相册信息
const fetchAlbumInfo = async () => {
  try {
    // 从 sessionStorage 中获取密码
    const password = sessionStorage.getItem(`album_${albumId}_password`)
    const res = await getTimeAlbumByIdUsingGet({
      id: albumId,
      password
    })
    if (res.data.code === 0) {
      album.value = res.data.data
      // 获取相册成功后再获取照片
      await fetchPictures()
    }
  } catch (error: any) {
    console.error('获取相册信息失败:', error)
    const errorMsg = error?.response?.data?.message || '获取相册信息失败'
    message.error(errorMsg)
    // 如果是密码错误，跳回列表页
    if (error?.response?.data?.code === 40101) {
      // 清除密码
      sessionStorage.removeItem(`album_${albumId}_password`)
      router.replace('/time-album')
    }
  }
}

// 获取相册照片
const fetchPictures = async () => {
  try {
    console.log('开始获取照片列表...')
    // 从 sessionStorage 中获取密码
    const password = sessionStorage.getItem(`album_${albumId}_password`)
    const res = await getHeartWallPicturesUsingGet({
      albumId: albumId,
      password
    })
    console.log('API响应:', res)
    if (res.data.code === 0) {
      const fetchedPictures = res.data.data || []
      pictures.value = fetchedPictures
      console.log('成功获取照片，数量:', pictures.value.length)
    }
  } catch (error: any) {
    console.error('获取照片列表失败:', error)
    message.error(error?.response?.data?.message || '获取照片列表失败')
  }
}

// 处理文件选择点击
const handleUploadClick = () => {
  fileInput.value?.click()
}

// 处理文件选择
const handleFileChange = (e: Event) => {
  const files = (e.target as HTMLInputElement).files
  if (files) {
    addFiles(Array.from(files))
  }
}

// 处理文件拖放
const handleFileDrop = (e: DragEvent) => {
  const files = e.dataTransfer?.files
  if (files) {
    addFiles(Array.from(files))
  }
}

// 修改压缩图片函数，优化压缩参数
const compressImage = (file: File): Promise<File> => {
  return new Promise((resolve, reject) => {
    // 如果文件小于1MB，直接返回
    if (file.size <= MAX_FILE_SIZE) {
      resolve(file);
      return;
    }

    const reader = new FileReader();
    reader.readAsDataURL(file);
    reader.onload = (e) => {
      const img = new Image();
      img.src = e.target?.result as string;

      img.onload = () => {
        const canvas = document.createElement('canvas');
        let width = img.width;
        let height = img.height;

        // 优化缩放比例计算
        const aspectRatio = width / height;
        if (width > MAX_WIDTH || height > MAX_HEIGHT) {
          if (aspectRatio > 1) {
            width = MAX_WIDTH;
            height = Math.round(width / aspectRatio);
          } else {
            height = MAX_HEIGHT;
            width = Math.round(height * aspectRatio);
          }
        }

        canvas.width = width;
        canvas.height = height;

        const ctx = canvas.getContext('2d', { alpha: false }); // 禁用alpha通道以提高性能
        if (!ctx) {
          reject(new Error('无法创建canvas上下文'));
          return;
        }

        // 使用更快的图像渲染设置
        ctx.imageSmoothingEnabled = true;
        ctx.imageSmoothingQuality = 'medium';
        ctx.drawImage(img, 0, 0, width, height);

        // 检查浏览器是否支持 WebP
        const isWebPSupported = () => {
          const canvas = document.createElement('canvas');
          if (canvas.toDataURL) {
            return canvas.toDataURL('image/webp').indexOf('data:image/webp') === 0;
          }
          return false;
        };

        const format = isWebPSupported() ? 'image/webp' : 'image/jpeg';
        let quality = 0.85; // 初始压缩质量

        // 递归压缩函数，直到文件大小合适或达到最低质量
        const compressWithQuality = (currentQuality: number): Promise<Blob> => {
          return new Promise((resolveBlob, rejectBlob) => {
            canvas.toBlob(
              (blob) => {
                if (!blob) {
                  rejectBlob(new Error('图片压缩失败'));
                  return;
                }

                // 如果文件大小合适或已经达到最低质量，返回结果
                if (blob.size <= MAX_FILE_SIZE || currentQuality <= 0.5) {
                  resolveBlob(blob);
                  return;
                }

                // 否则继续降低质量压缩
                compressWithQuality(currentQuality - 0.1)
                  .then(resolveBlob)
                  .catch(rejectBlob);
              },
              format,
              currentQuality
            );
          });
        };

        // 开始压缩过程
        compressWithQuality(quality)
          .then((finalBlob) => {
            const newFileName = file.name.replace(/\.[^/.]+$/, '') +
              (format === 'image/webp' ? '.webp' : '.jpg');
            const compressedFile = new File([finalBlob], newFileName, {
              type: format,
              lastModified: Date.now(),
            });
            resolve(compressedFile);
          })
          .catch((error) => {
            reject(error);
          });
      };

      img.onerror = () => reject(new Error('图片加载失败'));
    };

    reader.onerror = () => reject(new Error('文件读取失败'));
  });
};

// 修改上传处理方法，实现并行上传
const handleUpload = async () => {
  if (isUploading.value) return;
  isUploading.value = true;

  try {
    const totalFiles = selectedFiles.value.length;
    let completedFiles = 0;
    let failedFiles = 0;

    // 创建上传任务数组
    const uploadTasks = selectedFiles.value.map(async (file, index) => {
      try {
        const formData = new FormData();
        formData.append('files', file);

        // 更新上传进度
        previewFiles.value[index].uploading = true;
        const updateProgress = () => {
          if (previewFiles.value[index]) {
            previewFiles.value[index].progress += Math.random() * 30;
            if (previewFiles.value[index].progress < 100) {
              setTimeout(updateProgress, 200);
            }
          }
        };
        updateProgress();

        // 执行上传
        const res = await uploadHeartWallPicturesUsingPost(
          { albumId: albumId },
          {
            headers: { 'Content-Type': 'multipart/form-data' },
            requestType: 'form',
            data: formData
          }
        );

        if (res.data.code === 0) {
          completedFiles++;
          previewFiles.value[index].progress = 100;
          previewFiles.value[index].uploading = false;
        } else {
          throw new Error(res.data.message || '上传失败');
        }
      } catch (error) {
        failedFiles++;
        console.error(`文件 ${index + 1} 上传失败:`, error);
        message.error(`第 ${index + 1} 张图片上传失败`);
        previewFiles.value[index].uploading = false;
      }
    });

    // 并行执行所有上传任务
    await Promise.all(uploadTasks);

    // 检查上传结果
    if (failedFiles === 0) {
      message.success('所有图片上传成功');
    } else if (completedFiles > 0) {
      message.warning(`${completedFiles}张图片上传成功，${failedFiles}张上传失败`);
    } else {
      message.error('所有图片上传失败');
    }

    // 刷新图片列表
    if (completedFiles > 0) {
      setTimeout(() => {
        showUploadModal.value = false;
        selectedFiles.value = [];
        previewFiles.value = [];
        fetchPictures();
      }, 500);
    }
  } catch (error: any) {
    console.error('上传照片失败:', error);
    message.error(error?.response?.data?.message || '上传失败，请重试');
  } finally {
    isUploading.value = false;
  }
};

// 修改添加文件的方法
const addFiles = async (files: File[]) => {
  const currentCount = pictures.value.length;
  const remainingSlots = MAX_PHOTOS - currentCount;

  if (currentCount >= MAX_PHOTOS) {
    message.warning(`每个相册最多只能上传${MAX_PHOTOS}张照片哦~`);
    return;
  }

  // 检查单次上传数量限制
  if (files.length > MAX_UPLOAD_PER_TIME) {
    message.warning(`一次最多只能上传${MAX_UPLOAD_PER_TIME}张照片哦~`);
    files = files.slice(0, MAX_UPLOAD_PER_TIME);
  }

  // 如果新增数量超过剩余槽位，只取剩余槽位数量的照片
  const filesToAdd = files.slice(0, Math.min(remainingSlots, MAX_UPLOAD_PER_TIME));

  if (files.length > remainingSlots) {
    message.warning(`当前相册还能上传${remainingSlots}张照片，已自动选择前${remainingSlots}张~`);
  }

  try {
    // 并行处理所有文件
    const processedFiles = await Promise.all(
      filesToAdd.map(async (file) => {
        if (file.type.startsWith('image/')) {
          const compressedFile = await compressImage(file);
          const reader = new FileReader();
          return new Promise<void>((resolve) => {
            reader.onload = (e) => {
              const previewFile = {
                ...compressedFile,
                preview: e.target?.result as string,
                progress: 0,
                uploading: false
              };
              previewFiles.value.push(previewFile);
              selectedFiles.value.push(compressedFile);
              resolve();
            };
            reader.readAsDataURL(compressedFile);
          });
        }
      })
    );
  } catch (error) {
    console.error('图片处理失败:', error);
    message.error('图片处理失败，请重试');
  }
};

// 移除文件
const removeFile = (index: number) => {
  selectedFiles.value.splice(index, 1)
  previewFiles.value.splice(index, 1)
}

// 显示图片预览模态框
const openPreviewModal = (picture: Picture) => {
  selectedPicture.value = picture
  currentImageIndex.value = pictures.value.findIndex(p => p.id === picture.id)
  previewModalVisible.value = true
}

// 显示上一张图片
const showPreviousImage = () => {
  if (currentImageIndex.value > 0) {
    currentImageIndex.value--
    selectedPicture.value = pictures.value[currentImageIndex.value]
  }
}

// 显示下一张图片
const showNextImage = () => {
  if (currentImageIndex.value < pictures.value.length - 1) {
    currentImageIndex.value++
    selectedPicture.value = pictures.value[currentImageIndex.value]
  }
}

// 处理触摸开始
const handleTouchStart = (e: TouchEvent) => {
  touchStartX.value = e.touches[0].clientX
  isSwiping.value = true
}

// 处理触摸移动
const handleTouchMove = (e: TouchEvent) => {
  if (!isSwiping.value) return

  const currentX = e.touches[0].clientX
  const diff = currentX - touchStartX.value
  const previewImage = document.querySelector('.preview-image') as HTMLElement

  // 移除过渡效果，使滑动更加跟手
  if (previewImage) {
    previewImage.style.transition = 'none'
  }

  // 计算滑动距离和阻尼效果
  const dampingFactor = 0.95
  const maxDiff = window.innerWidth * 0.4

  let finalDiff = diff * dampingFactor
  if (Math.abs(finalDiff) > maxDiff) {
    finalDiff = Math.sign(finalDiff) * maxDiff
  }

  // 根据滑动距离计算透明度
  const opacity = Math.max(0.7, 1 - Math.abs(finalDiff) / (window.innerWidth * 1.5))

  if ((currentImageIndex.value === 0 && diff > 0) ||
    (currentImageIndex.value === pictures.value.length - 1 && diff < 0)) {
    swipeOffset.value = finalDiff * 0.4
  } else {
    swipeOffset.value = finalDiff
  }

  // 应用透明度
  if (previewImage) {
    previewImage.style.opacity = opacity.toString()
  }
}

// 处理触摸结束
const handleTouchEnd = () => {
  const minSwipeDistance = 40
  const previewImage = document.querySelector('.preview-image') as HTMLElement

  if (previewImage) {
    previewImage.style.transition = 'all 0.4s cubic-bezier(0.4, 0, 0.2, 1)'
    previewImage.style.opacity = '1'
  }

  if (Math.abs(swipeOffset.value) > minSwipeDistance) {
    const targetOffset = Math.sign(swipeOffset.value) * window.innerWidth

    if (swipeOffset.value > 0 && currentImageIndex.value > 0) {
      // 向右滑动到上一张
      swipeOffset.value = targetOffset
      if (previewImage) {
        previewImage.classList.add('transitioning')
        setTimeout(() => {
          showPreviousImage()
          requestAnimationFrame(() => {
            swipeOffset.value = 0
            if (previewImage) {
              previewImage.classList.remove('transitioning')
            }
          })
        }, 50)
      }
    } else if (swipeOffset.value < 0 && currentImageIndex.value < pictures.value.length - 1) {
      // 向左滑动到下一张
      swipeOffset.value = targetOffset
      if (previewImage) {
        previewImage.classList.add('transitioning')
        setTimeout(() => {
          showNextImage()
          requestAnimationFrame(() => {
            swipeOffset.value = 0
            if (previewImage) {
              previewImage.classList.remove('transitioning')
            }
          })
        }, 50)
      }
    } else {
      // 回弹动画
      swipeOffset.value = 0
    }
  } else {
    // 回弹动画
    swipeOffset.value = 0
  }

  isSwiping.value = false
}

// 关闭预览
const handlePreviewClose = () => {
  previewModalVisible.value = false
  swipeOffset.value = 0
}

// 确保在组件挂载时先获取相册信息
onMounted(() => {
  console.log('组件挂载，开始获取相册信息...')
  fetchAlbumInfo()
  initDynamicBackground()
})

// 初始化动态背景
const initDynamicBackground = () => {
  const RENDERER: Renderer = {
    SNOW_COUNT: {
      INIT: 50,
      DELTA: 0.2
    },
    BACKGROUND_COLOR: 'rgb(255, 240, 245)',
    INIT_HUE: 120,
    DELTA_HUE: 0.1,
    $container: $(''),
    width: 0,
    height: 0,
    center: { x: 0, y: 0 },
    countRate: 0,
    canvas: document.createElement('canvas'),
    context: document.createElement('canvas').getContext('2d') as CanvasRenderingContext2D,
    radius: 0,
    hue: 0,
    snows: [],

    init: function() {
      this.setParameters();
      this.reconstructMethod();
      this.createSnow(this.SNOW_COUNT.INIT * this.countRate, true);
      this.render();
    },
    setParameters: function() {
      const container = document.getElementById('nm-container');
      if (!container) return;

      this.$container = $(container);
      this.width = container.clientWidth;
      this.height = container.clientHeight;
      this.center = {
        x: this.width / 2,
        y: this.height / 2
      };
      this.countRate = this.width * this.height / 500 / 500;
      this.canvas = $('<canvas />').attr({
        width: this.width,
        height: this.height
      }).appendTo(this.$container).get(0);

      const ctx = this.canvas.getContext('2d');
      if (!ctx) return;
      this.context = ctx;

      this.radius = Math.sqrt(this.center.x * this.center.x + this.center.y * this.center.y);
      this.hue = this.INIT_HUE;
      this.snows = [];
    },
    reconstructMethod: function() {
      this.render = this.render.bind(this);
    },
    createSnow: function(count, toRandomize) {
      for (var i = 0; i < count; i++) {
        this.snows.push(new SNOW(this.width, this.height, this.center, toRandomize));
      }
    },
    render: function() {
      requestAnimationFrame(this.render);

      var gradient = this.context.createRadialGradient(this.center.x, this.center.y, 0, this.center.x, this.center.y, this.radius),
        backgroundColor = this.BACKGROUND_COLOR.replace('%h', this.hue.toString());

      gradient.addColorStop(0, backgroundColor.replace('%l', '30'));
      gradient.addColorStop(0.2, backgroundColor.replace('%l', '20'));
      gradient.addColorStop(1, backgroundColor.replace('%l', '5'));

      this.context.fillStyle = gradient;
      this.context.fillRect(0, 0, this.width, this.height);

      for (var i = this.snows.length - 1; i >= 0; i--) {
        if (!this.snows[i].render(this.context)) {
          this.snows.splice(i, 1);
        }
      }
      this.hue += this.DELTA_HUE;
      this.hue %= 360;

      this.createSnow(this.SNOW_COUNT.DELTA, false);
    }
  };

  const SNOW = function(this: Snow, width: number, height: number, center: { x: number; y: number }, toRandomize: boolean) {
    this.width = width;
    this.height = height;
    this.center = center;
    this.init(toRandomize);
  } as unknown as { new (width: number, height: number, center: { x: number; y: number }, toRandomize: boolean): Snow };

  SNOW.prototype = {
    RADIUS: 30,
    OFFSET: 4,
    INIT_POSITION_MARGIN: 200,
    COLOR: 'rgba(255, 192, 203, 0.9)',
    TOP_RADIUS: {
      MIN: 1,
      MAX: 3
    },
    SCALE: {
      INIT: 0.4,
      DELTA: 0.01,
      MAX: 4
    },
    DELTA_ROTATE: {
      MIN: -Math.PI / 360,
      MAX: Math.PI / 360
    },
    THRESHOLD_TRANSPARENCY: 0.8,
    VELOCITY: {
      MIN: -1,
      MAX: 1
    },
    LINE_WIDTH: 2,
    BLUR: 8,

    init: function(toRandomize) {
      this.setParameters(toRandomize);
      this.createSnow();
    },
    setParameters: function(toRandomize) {
      if (!this.canvas) {
        this.radius = this.RADIUS + this.TOP_RADIUS.MAX * 2 + this.LINE_WIDTH;
        this.length = this.radius * 2;
        this.canvas = $('<canvas />').attr({
          width: this.length,
          height: this.length
        }).get(0);
        this.context = this.canvas.getContext('2d');
      }
      this.topRadius = this.getRandomValue(this.TOP_RADIUS);

      this.x = Math.random() * this.width;
      this.y = Math.random() * this.height;
      this.vx = this.getRandomValue(this.VELOCITY);
      this.vy = this.getRandomValue(this.VELOCITY);

      this.deltaRotate = this.getRandomValue(this.DELTA_ROTATE);
      this.scale = this.SCALE.INIT;
      this.deltaScale = 1 + this.SCALE.DELTA * 500 / Math.max(this.width, this.height);
      this.rotate = 0;

      if (toRandomize) {
        for (var i = 0, count = Math.random() * 1000; i < count; i++) {
          this.x += this.vx;
          this.y += this.vy;
          this.scale *= this.deltaScale;
          this.rotate += this.deltaRotate;
        }
      }
    },
    getRandomValue: function(range) {
      return range.MIN + (range.MAX - range.MIN) * Math.random();
    },
    createSnow: function() {
      this.context.clearRect(0, 0, this.length, this.length);

      this.context.save();
      this.context.beginPath();
      this.context.translate(this.radius, this.radius);
      this.context.shadowColor = this.COLOR;
      this.context.shadowBlur = this.BLUR;

      this.context.fillText('❤️', 0, 0);
      this.context.stroke();
      this.context.restore();
    },
    render: function(context) {
      context.save();

      if (this.scale > this.THRESHOLD_TRANSPARENCY) {
        context.globalAlpha = Math.max(0, (this.SCALE.MAX - this.scale) / (this.SCALE.MAX - this.THRESHOLD_TRANSPARENCY));

        if (this.scale > this.SCALE.MAX || this.x < -this.radius || this.x > this.width + this.radius || this.y < -this.radius || this.y > this.height + this.radius) {
          context.restore();
          return false;
        }
      }
      context.translate(this.x, this.y);
      context.rotate(this.rotate);
      context.scale(this.scale, this.scale);
      context.drawImage(this.canvas, -this.radius, -this.radius);
      context.restore();

      this.x += this.vx;
      this.y += this.vy;
      this.scale *= this.deltaScale;
      this.rotate += this.deltaRotate;
      return true;
    }
  };

  // 初始化渲染器
  RENDERER.init();
}

// 在组件卸载时清理动态背景
onUnmounted(() => {
  const container = document.getElementById('nm-container');
  if (container) {
    container.innerHTML = '';
  }
});

// 添加删除相关的状态
const showDeleteModal = ref(false)
const selectedDeletePicture = ref<Picture | null>(null)

// 修改删除照片的方法
const handleDeletePicture = (picture: Picture) => {
  selectedDeletePicture.value = picture
  showDeleteModal.value = true
}

// 确认删除照片
const confirmDeletePicture = async () => {
  if (!selectedDeletePicture.value) return

  try {
    const res = await deleteHeartWallPictureUsingPost({
      albumId: albumId,
      pictureId: String(selectedDeletePicture.value.id)
    })

    if (res.data.code === 0) {
      message.success('删除成功')
      showDeleteModal.value = false
      selectedDeletePicture.value = null
      // 重新获取照片列表
      await fetchPictures()
    } else {
      message.error(res.data.message || '删除失败')
    }
  } catch (error: any) {
    console.error('删除照片失败:', error)
    message.error(error?.response?.data?.message || '删除失败')
  }
}

// 添加文件大小格式化函数
const formatFileSize = (size: string) => {
  const numSize = parseInt(size)
  if (numSize < 1024) {
    return numSize + ' B'
  } else if (numSize < 1024 * 1024) {
    return (numSize / 1024).toFixed(1) + ' KB'
  } else {
    return (numSize / (1024 * 1024)).toFixed(1) + ' MB'
  }
}

</script>

<style scoped>
.album-detail-container {
  position: relative;
  min-height: 88vh;
  padding: 24px;
  overflow: hidden;
  background: transparent;
}

.dynamic-bg {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: transparent;
}

#nm-container {
  width: 100%;
  height: 100%;
  margin: 0;
  padding: 0;
  overflow: hidden;
  background: rgb(255, 240, 245);  /* 浅粉色背景 */
}

.album-header {
  position: relative;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
  padding: 24px;
  background: rgba(255, 255, 255, 0.1);  /* 降低透明度 */
  backdrop-filter: blur(10px);
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.album-info h1 {
  color: #ffffff;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
  background: none;
  -webkit-background-clip: unset;
  font-size: 28px;
  margin: 0 0 8px;
}

.album-info p {
  color: rgba(255, 255, 255, 0.8);
  margin: 0;
}

.upload-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  background: linear-gradient(45deg, #6e45e1, #88d3ce);
  color: white;
  border: none;
  border-radius: 12px;
  cursor: pointer;
  font-size: 16px;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(110, 69, 225, 0.3);
}

.upload-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(110, 69, 225, 0.4);
}

.btn-icon {
  font-size: 20px;
}

.btn-text {
  font-weight: 500;
  letter-spacing: 0.5px;
}

.heart-wall {
  position: relative;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  margin-top: 0;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.empty-state {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 400px;
  color: rgba(0, 0, 0, 0.8);
  font-size: 16px;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.picture-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 24px;
  width: 100%;
  margin: 0 auto;
  padding: 24px;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  will-change: transform, opacity;
}

.picture-item {
  position: relative;
  aspect-ratio: 1;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
  animation: gentleWave 4s ease-in-out infinite;
  cursor: pointer;
  background: #fff;

  &:active .picture-overlay {
    opacity: 1;
  }
}

.picture-item::before {
  display: none;
}

.picture-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: all 0.6s cubic-bezier(0.4, 0, 0.2, 1);
  opacity: 0;
  animation: imageLoad 0.6s ease forwards;
}

@keyframes imageLoad {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

/* 更柔和的波动动画 */
@keyframes gentleWave {
  0% { transform: translateY(0); }
  25% { transform: translateY(-4px); }
  50% { transform: translateY(0); }
  75% { transform: translateY(4px); }
  100% { transform: translateY(0); }
}

/* 错开动画时间 */
.picture-item:nth-child(3n + 1) {
  animation-delay: -0.8s;
}

.picture-item:nth-child(3n + 2) {
  animation-delay: -1.6s;
}

.picture-item:nth-child(3n + 3) {
  animation-delay: -2.4s;
}

.picture-item:hover {
  transform: scale(1.02);
}

.picture-item:hover .picture-overlay {
  opacity: 1;
}

.picture-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.picture-item:hover img {
  transform: scale(1.1);
}

.picture-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.3);
  display: flex;
  justify-content: center;
  align-items: center;
  opacity: 0;
  transition: all 0.3s ease;
}

.delete-btn {
  background: rgba(255, 255, 255, 0.9);
  border: none;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;
  color: #ff4d4f;
  font-size: 18px;
}

.delete-btn:hover {
  background: #ff4d4f;
  color: white;
  transform: scale(1.1);
}

/* 模态框样式 */
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
}

.modal-content {
  background: white;
  border-radius: 20px;
  padding: 32px;
  width: 90%;
  max-width: 500px;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.modal-header h2 {
  font-size: 24px;
  color: #333;
  display: flex;
  align-items: center;
  gap: 8px;
}

.modal-close {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #666;
  transition: all 0.3s ease;
}

.modal-close:hover {
  color: #ff4d4f;
  transform: rotate(90deg);
}

.modal-body {
  flex: 1;
  overflow-y: auto;
  padding-right: 8px;
}

/* 自定义滚动条样式 */
.modal-body::-webkit-scrollbar {
  width: 6px;
}

.modal-body::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.modal-body::-webkit-scrollbar-thumb {
  background: #888;
  border-radius: 3px;
}

.modal-body::-webkit-scrollbar-thumb:hover {
  background: #555;
}

.upload-area {
  border: 3px dashed #e0e0e0;
  border-radius: 16px;
  padding: 40px;
  cursor: pointer;
  transition: all 0.3s ease;
  background: #f8f8f8;
  position: relative;
  min-height: 200px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.upload-preview {
  margin-top: 20px;
  width: 100%;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  gap: 10px;
  padding: 10px;
}

.preview-item {
  position: relative;
  aspect-ratio: 1;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  background: #fff;
}

.preview-item::before {
  display: none;
}

.preview-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  animation: previewItemLoad 0.4s ease forwards;
}

@keyframes previewItemLoad {
  from {
    opacity: 0;
    transform: scale(1.1);
    filter: blur(5px);
  }
  to {
    opacity: 1;
    transform: scale(1);
    filter: blur(0);
  }
}

.upload-progress {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 4px;
  background: rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.progress-bar {
  height: 100%;
  background: linear-gradient(90deg, #6e45e1, #88d3ce);
  transition: width 0.3s ease;
}

.progress-text {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: white;
  font-size: 12px;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
  z-index: 1;
}

.remove-preview {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.9);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 12px;
  color: #ff4d4f;
  border: none;
  padding: 0;
}

.remove-preview:hover {
  background: #ff4d4f;
  color: white;
}

.selected-files {
  margin-top: 20px;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: #f8f8f8;
  border-radius: 8px;
  margin-bottom: 8px;
}

.file-icon {
  font-size: 20px;
}

.file-name {
  flex: 1;
  font-size: 14px;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.remove-btn {
  background: none;
  border: none;
  color: #ff4d4f;
  cursor: pointer;
  font-size: 18px;
  padding: 4px;
  border-radius: 4px;
  transition: all 0.3s ease;
}

.remove-btn:hover {
  background: rgba(255, 77, 79, 0.1);
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 32px;
}

.btn-cancel,
.btn-confirm {
  padding: 12px 24px;
  border-radius: 12px;
  cursor: pointer;
  font-size: 15px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.btn-cancel {
  background: #f5f5f5;
  color: #666;
  border: none;
}

.btn-confirm {
  background: linear-gradient(45deg, #6e45e1, #88d3ce);
  color: white;
  border: none;
  box-shadow: 0 4px 15px rgba(110, 69, 225, 0.3);
}

.btn-cancel:hover {
  background: #ebebeb;
}

.btn-confirm:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(110, 69, 225, 0.4);
}

.btn-confirm:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

/* 移动端布局优化 */
@media (max-width: 768px) {
  .album-detail-container {
    padding: 4px 2px;
    overflow: visible;
    min-height: 100vh;
    display: flex;
    flex-direction: column;
  }

  .heart-wall {
    padding: 2px;
    border-radius: 12px;
    margin: -2px;
    width: calc(100% + 4px);
    overflow: visible;
    flex: 1;
    display: flex;
    flex-direction: column;
  }

  .picture-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 2px;
    padding: 0;
    margin: auto 0; /* 这会使网格在垂直方向上居中 */
    width: 100%;
    height: auto;
    overflow: visible;
  }

  .picture-item {
    position: relative;
    aspect-ratio: 1;
    border-radius: 6px;
    overflow: hidden;
  }

  .picture-item img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  .picture-overlay {
    display: flex;
    opacity: 0;
    background: linear-gradient(to bottom, transparent, rgba(0, 0, 0, 0.5));
    pointer-events: none;
    transition: opacity 0.2s ease;
  }

  .picture-item:active .picture-overlay {
    opacity: 1;
  }

  .delete-btn {
    position: absolute;
    bottom: 6px;
    right: 6px;
    width: 24px;
    height: 24px;
    background: rgba(255, 255, 255, 0.95);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    pointer-events: auto;
    color: #ff4d4f;
    font-size: 14px;
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.2);
    opacity: 0.9;
    transform: none;
    transition: all 0.2s ease;
  }

  .delete-btn:active {
    transform: scale(0.9);
    background: #ff4d4f;
    color: white;
  }

  /* 调整动画幅度和时间 */
  @keyframes gentleWave {
    0% { transform: translateY(0); }
    25% { transform: translateY(-2px); }
    50% { transform: translateY(0); }
    75% { transform: translateY(2px); }
    100% { transform: translateY(0); }
  }
}

/* 小屏幕进一步优化 */
@media (max-width: 480px) {
  .album-detail-container {
    padding: 2px 1px;
  }

  .heart-wall {
    padding: 1px;
    margin: -1px;
    width: calc(100% + 2px);
  }

  .picture-grid {
    gap: 1px;
  }

  .picture-item {
    border-radius: 4px;
  }

  .delete-btn {
    width: 22px;
    height: 22px;
    font-size: 12px;
    bottom: 4px;
    right: 4px;
  }
}

.floating-upload-btn {
  position: fixed;
  top: 80px;
  right: 40px;
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.2);
  transition: all 0.3s ease;
  z-index: 1000;
  font-size: 36px;
  color: #333;
}

.floating-upload-btn:hover {
  transform: translateY(-2px);
  background: rgba(255, 255, 255, 0.3);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.3);
}

.floating-upload-btn .btn-icon {
  line-height: 1;
  margin-top: -2px;
}

/* 图片预览模态框样式 */
.custom-preview-overlay {
  position: fixed;
  inset: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(0, 0, 0, 0.9);
  backdrop-filter: blur(10px);
  z-index: 9999;
  display: flex;
  justify-content: center;
  align-items: center;
  animation: fadeIn 0.2s ease;
}

.custom-preview-container {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
}

.custom-preview-content {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
}

.preview-image {
  max-width: 90%;
  max-height: 90vh;
  object-fit: contain;
  user-select: none;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  will-change: transform, opacity;
  opacity: 0;
  transform: scale(0.95);
  filter: blur(10px);
  animation: previewLoad 0.4s ease forwards;
}

@keyframes previewLoad {
  from {
    opacity: 0;
    transform: scale(0.95);
    filter: blur(10px);
  }
  to {
    opacity: 1;
    transform: scale(1);
    filter: blur(0);
  }
}

.close-button {
  position: fixed;
  top: 20px;
  right: 20px;
  width: 40px;
  height: 40px;
  border: none;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 50%;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;
  z-index: 10000;
}

.close-button:hover {
  background: rgba(255, 255, 255, 0.2);
  transform: scale(1.1);
}

.nav-button {
  position: fixed;
  top: 50%;
  transform: translateY(-50%);
  width: 44px;
  height: 44px;
  border: none;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 50%;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;
  z-index: 10000;
}

.nav-button:hover {
  background: rgba(255, 255, 255, 0.2);
  transform: translateY(-50%) scale(1.1);
}

.nav-button.prev {
  left: 20px;
}

.nav-button.next {
  right: 20px;
}

.nav-button svg {
  width: 24px;
  height: 24px;
  fill: currentColor;
}

.image-counter {
  position: fixed;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(255, 255, 255, 0.1);
  padding: 6px 12px;
  border-radius: 20px;
  color: #fff;
  font-size: 14px;
  z-index: 10000;
  user-select: none;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .preview-image {
    max-width: 95%;
    max-height: 80vh;
  }

  .nav-button {
    width: 36px;
    height: 36px;
  }

  .nav-button.prev {
    left: 10px;
  }

  .nav-button.next {
    right: 10px;
  }

  .close-button {
    top: auto;
    right: auto;
    bottom: 36px;
    left: 50%;
    transform: translateX(-50%);
    width: 44px;
    height: 44px;
    background: rgba(255, 255, 255, 0.15);
  }

  .close-button:hover {
    transform: translateX(-50%);
    background: rgba(255, 255, 255, 0.25);
  }

  .image-counter {
    bottom: 90px;
    font-size: 12px;
    padding: 4px 10px;
  }
}

.limit-warning {
  color: #ff4d4f;
  font-weight: bold;
}

.upload-tip {
  font-size: 14px;
  color: #999;
  margin-top: 8px;
  text-align: center;
}

.upload-tip span {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 10px;
  background: rgba(0, 0, 0, 0.05);
  margin-top: 4px;
}

.daily-moments-wrap,
.time-album-wrap {
  padding: 40px 20px;
  position: relative;
  opacity: 0;
  transform-origin: top;
  animation: smoothSlideDown 0.4s cubic-bezier(0.4, 0, 0.2, 1) forwards;
  will-change: transform, opacity;
  overflow: hidden;
}

@keyframes smoothSlideDown {
  0% {
    opacity: 0;
    transform: translateY(-20px) scale(0.98);
  }
  100% {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

/* 移动端优化 */
@media screen and (max-width: 768px) {
  .daily-moments-wrap,
  .time-album-wrap {
    padding: 20px 10px;
  }
}

/* 确保内容区域的平滑过渡 */
.heart-wall {
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 优化图片网格的过渡效果 */
.picture-grid {
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  will-change: transform, opacity;
}

/* 优化卡片的过渡效果 */
.card-content {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  will-change: transform, background-color, border-color;
}

.card-content.active {
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  border-color: var(--themeBackground);
  background: rgba(110, 69, 225, 0.05);
}

/* 删除确认框样式 */
.delete-modal {
  max-width: 360px;
  padding: 0;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  animation: modalPop 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

@keyframes modalPop {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.delete-modal-body {
  display: flex;
  flex-direction: column;
}

.delete-preview {
  width: 100%;
  height: 240px;
  overflow: hidden;
  position: relative;
  margin: 0;
  box-shadow: none;
  border-radius: 0;
}

.delete-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.delete-confirm-content {
  padding: 24px;
  text-align: center;
}

.delete-icon {
  font-size: 32px;
  color: #ff4d4f;
  margin-bottom: 16px;
  animation: iconShake 1s ease infinite;
}

@keyframes iconShake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-4px); }
  75% { transform: translateX(4px); }
}

.delete-message h3 {
  font-size: 18px;
  color: #333;
  margin: 0 0 8px;
  font-weight: 600;
}

.delete-message p {
  font-size: 14px;
  color: #666;
  margin: 0;
}

.delete-actions {
  display: flex;
  gap: 12px;
  margin-top: 24px;
  padding: 0 12px;
}

.delete-actions button {
  flex: 1;
  padding: 12px;
  border: none;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.delete-actions .btn-cancel {
  background: #f5f5f5;
  color: #666;
}

.delete-actions .btn-cancel:hover {
  background: #ebebeb;
}

.delete-actions .btn-delete {
  background: #ff4d4f;
  color: white;
  box-shadow: 0 4px 12px rgba(255, 77, 79, 0.2);
}

.delete-actions .btn-delete:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(255, 77, 79, 0.3);
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .delete-modal {
    width: 90%;
    margin: 16px;
  }

  .delete-preview {
    height: 200px;
  }

  .delete-confirm-content {
    padding: 20px;
  }

  .delete-message h3 {
    font-size: 16px;
  }

  .delete-message p {
    font-size: 13px;
  }

  .delete-actions button {
    padding: 10px;
    font-size: 14px;
  }
}

.picture-info {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 8px;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.7), transparent);
  color: white;
  font-size: 12px;
  opacity: 0;
  transition: opacity 0.3s ease;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.picture-item:hover .picture-info {
  opacity: 1;
}

.picture-size, .picture-dimensions {
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
}

.image-info {
  position: fixed;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.7);
  padding: 8px 16px;
  border-radius: 20px;
  color: white;
  font-size: 14px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  z-index: 10000;
  backdrop-filter: blur(10px);
}

.image-details {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.8);
}

.image-details span {
  padding: 2px 8px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 12px;
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .picture-info {
    font-size: 10px;
    padding: 6px;
  }

  .image-info {
    bottom: 16px;
    padding: 6px 12px;
  }

  .image-details {
    font-size: 10px;
    gap: 8px;
  }

  .image-details span {
    padding: 1px 6px;
  }
}
</style>
