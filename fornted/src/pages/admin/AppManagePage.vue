<template>
  <div id="appManagePage">
    <div class="pc-container">
      <!-- 当前版本信息卡片 -->
      <a-card title="当前版本" class="version-card">
        <template #extra>
          <a-button type="primary" @click="showUploadModal">
            <UploadOutlined />
            上传新版本
          </a-button>
        </template>
        <a-descriptions :column="2">
          <a-descriptions-item label="版本号">
            {{ currentVersion.version || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="版本码">
            {{ currentVersion.versionCode || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="更新时间">
            {{ currentVersion.updateTime ? dayjs(currentVersion.updateTime).format('YYYY-MM-DD HH:mm:ss') : '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="APK大小">
            {{ formatFileSize(currentVersion.apkSize) }}
          </a-descriptions-item>
          <a-descriptions-item label="强制更新">
            {{ currentVersion.forceUpdate ? '是' : '否' }}
          </a-descriptions-item>
          <a-descriptions-item label="下载链接">
            <a :href="currentVersion.downloadUrl" target="_blank">点击下载</a>
          </a-descriptions-item>
          <a-descriptions-item label="版本说明" :span="2">
            {{ currentVersion.description || '-' }}
          </a-descriptions-item>
        </a-descriptions>
      </a-card>

      <!-- 上传新版本模态框 -->
      <a-modal
        v-model:open="uploadModalVisible"
        title="上传新版本"
        @ok="handleUpload"
        @cancel="handleCancel"
        :confirmLoading="uploading"
      >
        <a-form :model="uploadForm" :rules="rules" ref="uploadFormRef">
          <a-form-item label="版本号" name="version">
            <a-input v-model:value="uploadForm.version" placeholder="如: 1.0.0" />
          </a-form-item>
          <a-form-item label="版本码" name="versionCode">
            <a-input-number
              v-model:value="uploadForm.versionCode"
              :min="1"
              style="width: 100%"
              placeholder="请输入版本码"
            />
          </a-form-item>
          <a-form-item label="版本说明" name="description">
            <a-textarea
              v-model:value="uploadForm.description"
              :rows="4"
              placeholder="请输入版本更新说明"
            />
          </a-form-item>
          <a-form-item label="强制更新" name="isForce">
            <a-switch v-model:checked="uploadForm.isForce" />
          </a-form-item>
          <a-form-item label="APK文件" name="file">
            <a-upload-dragger
              v-model:fileList="fileList"
              name="file"
              :maxCount="1"
              :beforeUpload="beforeUpload"
              @change="handleFileChange"
            >
              <p class="ant-upload-drag-icon">
                <InboxOutlined />
              </p>
              <p class="ant-upload-text">点击或拖拽文件到此处上传</p>
              <p class="ant-upload-hint">
                只支持上传 .apk 文件
              </p>
            </a-upload-dragger>
          </a-form-item>
        </a-form>
      </a-modal>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { UploadOutlined, InboxOutlined } from '@ant-design/icons-vue'
import type { UploadProps } from 'ant-design-vue'
import dayjs from 'dayjs'
import { uploadNewVersionUsingPost, getLatestVersionUsingGet } from '@/api/appController'

// 当前版本信息
const currentVersion = ref<any>({})

// 获取最新版本信息
const fetchLatestVersion = async () => {
  try {
    const res = await getLatestVersionUsingGet()
    if (res.data?.code === 0) {
      currentVersion.value = res.data.data
    }
  } catch (error) {
    message.error('获取版本信息失败')
  }
}

// 格式化文件大小
const formatFileSize = (bytes?: number) => {
  if (!bytes) return '-'
  const units = ['B', 'KB', 'MB', 'GB']
  let size = bytes
  let unitIndex = 0
  while (size >= 1024 && unitIndex < units.length - 1) {
    size /= 1024
    unitIndex++
  }
  return `${size.toFixed(2)} ${units[unitIndex]}`
}

// 上传相关
const uploadModalVisible = ref(false)
const uploading = ref(false)
const fileList = ref<any[]>([])
const uploadFormRef = ref()

const uploadForm = ref({
  version: '',
  versionCode: undefined,
  description: '',
  isForce: false
})

const rules = {
  version: [{ required: true, message: '请输入版本号' }],
  versionCode: [{ required: true, message: '请输入版本码' }],
  description: [{ required: true, message: '请输入版本说明' }],
}

// 处理文件变化
const handleFileChange = (info: any) => {
  fileList.value = info.fileList.slice(-1)
}

// 上传前校验
const beforeUpload = (file: File) => {
  // 阻止自动上传
  return false
}

// 显示上传模态框
const showUploadModal = () => {
  uploadModalVisible.value = true
}

// 处理取消
const handleCancel = () => {
  uploadModalVisible.value = false
  uploadForm.value = {
    version: '',
    versionCode: undefined,
    description: '',
    isForce: false
  }
  fileList.value = []
}

// 处理上传
const handleUpload = async () => {
  try {
    // 表单验证
    await uploadFormRef.value.validate()

    // 检查是否选择了文件
    if (fileList.value.length === 0) {
      message.error('请选择要上传的APK文件')
      return
    }

    uploading.value = true
    const formData = new FormData()
    const file = fileList.value[0]?.originFileObj
    if (!file) {
      message.error('文件不存在')
      return
    }

    // 验证文件类型
    if (!file.name.endsWith('.apk')) {
      message.error('只能上传APK文件')
      return
    }

    // 验证版本号格式
    const versionPattern = /^\d+\.\d+\.\d+$/
    if (!versionPattern.test(uploadForm.value.version)) {
      message.error('版本号格式不正确，请使用 x.y.z 格式')
      return
    }

    // 验证版本码
    if (!uploadForm.value.versionCode || uploadForm.value.versionCode < 1) {
      message.error('请输入有效的版本码')
      return
    }

    const appVersionData = {
      version: uploadForm.value.version,
      versionCode: uploadForm.value.versionCode,
      description: uploadForm.value.description,
      isForce: uploadForm.value.isForce ? 1 : 0,
      status: 1
    }

    // 添加文件和版本信息到 FormData
    formData.append('file', file)
    formData.append('appVersion', JSON.stringify(appVersionData))

    // 发送请求
    const res = await fetch('/api/app/upload', {
      method: 'POST',
      body: formData,
      headers: {
        // 不要设置 Content-Type，让浏览器自动设置
        'Authorization': localStorage.getItem('token') || ''
      }
    }).then(response => response.json())

    if (res.code === 0) {
      message.success('上传成功')
      handleCancel()
      fetchLatestVersion()
    } else {
      throw new Error(res.message || '上传失败')
    }
  } catch (error: any) {
    console.error('上传失败:', error)
    message.error(error.message || '上传失败')
  } finally {
    uploading.value = false
  }
}

onMounted(() => {
  fetchLatestVersion()
})
</script>

<style scoped>
#appManagePage {
  background: #f0f2f5;
  min-height: 88vh;
  padding: 4px;
  margin-left: -20px !important;
  margin-right: -20px !important;
}

.pc-container {
  max-width: 1200px;
  margin: 0 auto;
}

.version-card {
  margin-bottom: 24px;
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

:deep(.ant-card-head) {
  border-bottom: 1px solid #f0f0f0;
  padding: 0 24px;
}

:deep(.ant-card-head-title) {
  font-size: 16px;
  font-weight: 500;
}

:deep(.ant-descriptions-item-label) {
  color: #666;
}

:deep(.ant-upload-drag) {
  border-radius: 8px;
}
</style>
