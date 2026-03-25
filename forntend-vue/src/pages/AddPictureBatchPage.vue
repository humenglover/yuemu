<template>
  <div id="addPictureBatchPage">
    <h2 class="page-title">批量创建</h2>
    <!-- 图片信息表单 -->
    <a-form name="formData" layout="vertical" :model="formData" @finish="handleSubmit" class="batch-form">
      <a-form-item name="searchText" label="关键词">
        <div class="custom-input-wrapper">
          <a-input
            v-model:value="formData.searchText"
            placeholder="请输入关键词"
            class="custom-input"
            :suffix-icon="null"
          />
          <i
            v-if="formData.searchText"
            class="fas fa-times clear-icon"
            @click="formData.searchText = ''"
          ></i>
        </div>
      </a-form-item>
      <a-form-item name="count" label="抓取数量">
        <div class="custom-input-wrapper">
          <a-input-number
            v-model:value="formData.count"
            placeholder="请输入数量"
            class="custom-input-number"
            :min="1"
            :max="30"
            :controls="false"
          />
          <i
            v-if="formData.count"
            class="fas fa-times clear-icon"
            @click="formData.count = undefined"
          ></i>
        </div>
      </a-form-item>
      <a-form-item name="namePrefix" label="名称前缀">
        <div class="custom-input-wrapper">
          <a-input
            v-model:value="formData.namePrefix"
            placeholder="请输入名称前缀，会自动补充序号"
            class="custom-input"
            :suffix-icon="null"
          />
          <i
            v-if="formData.namePrefix"
            class="fas fa-times clear-icon"
            @click="formData.namePrefix = ''"
          ></i>
        </div>
      </a-form-item>
      <a-form-item name="category" label="分类">
        <div class="custom-input-wrapper">
          <a-auto-complete
            v-model:value="formData.category"
            placeholder="请输入分类"
            :options="categoryOptions"
            class="custom-input"
            :suffix-icon="null"
          >
            <template #suffixIcon>
              <i class="fas fa-chevron-down dropdown-icon"></i>
            </template>
          </a-auto-complete>
          <i
            v-if="formData.category"
            class="fas fa-times clear-icon"
            @click="formData.category = ''"
          ></i>
        </div>
      </a-form-item>
      <a-form-item name="tags" label="标签">
        <div class="custom-input-wrapper">
          <a-select
            v-model:value="formData.tags"
            mode="tags"
            placeholder="请输入标签"
            :options="tagOptions"
            class="custom-select"
            :suffix-icon="null"
          >
            <template #suffixIcon>
              <i class="fas fa-chevron-down dropdown-icon"></i>
            </template>
          </a-select>
          <i
            v-if="formData.tags && formData.tags.length"
            class="fas fa-times clear-icon"
            @click="formData.tags = []"
          ></i>
        </div>
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit" class="submit-button" :loading="false">
          <i v-if="loading" class="fas fa-spinner fa-spin loading-icon"></i>
          {{ loading ? '执行中...' : '执行任务' }}
        </a-button>
      </a-form-item>
    </a-form>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import {
  listPictureTagCategoryUsingGet,
  uploadPictureByBatchUsingPost
} from '@/api/pictureController.ts'
import { useRoute, useRouter } from 'vue-router'

const formData = reactive<API.PictureUploadByBatchRequest>({
  count: 10,
  category: '',
  tags: []
})
// 提交任务状态
const loading = ref(false)

const router = useRouter()

const categoryOptions = ref<string[]>([])
const tagOptions = ref<{ value: string; label: string }[]>([])

/**
 * 获取标签和分类选项
 * @param values
 */
const getTagCategoryOptions = async () => {
  const res = await listPictureTagCategoryUsingGet()
  if (res.data.code === 0 && res.data.data) {
    tagOptions.value = (res.data.data.tagList?? []).map((data: string) => {
      return {
        value: data,
        label: data,
      }
    })
    categoryOptions.value = (res.data.data.categoryList?? []).map((data: string) => {
      return {
        value: data,
        label: data,
      }
    })
  } else {
    // message.error('获取标签分类列表失败，' + res.data.message)
  }
}

onMounted(() => {
  getTagCategoryOptions()
})

/**
 * 提交表单
 * @param values
 */
const handleSubmit = async (values: any) => {
  loading.value = true
  const { category, tags,...tagOptions } = formData;
  // console.log(tags)
  const requestData: API.PictureUploadByBatchRequest = {
    categoryName: category,
    tagName: tags, // 将标签格式转换为后端要求的列表格式
    ...tagOptions
  };
  // console.log(requestData)
  const res = await uploadPictureByBatchUsingPost(requestData)
  // 操作成功
  if (res.data.code === 0 && res.data.data) {
    message.success(`创建成功，共 ${res.data.data} 条`)
    // 跳转到主页
    router.push({
      path: `/`,
    })
  } else {
    // message.error('创建失败，' + res.data.message)
  }
  loading.value = false
}
</script>

<style scoped>
#addPictureBatchPage {
  max-width: 720px;
  margin: 0 auto;
  padding: 24px 16px;
  background: var(--header-background);
  color: var(--text-primary);
  border-radius: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 24px;
  text-align: center;
}

.batch-form {
  :deep(.ant-form-item-label) {
    padding-bottom: 8px;
  }

  :deep(.ant-form-item-label > label) {
    color: #4b5563;
    font-size: 14px;
    font-weight: 500;
  }

  :deep(.ant-form-item) {
    margin-bottom: 24px;
  }
}

.custom-input {
  :deep(.ant-input) {
    height: 40px;
    border-radius: 8px;
    border-color: #e2e8f0;
    transition: all 0.3s ease;

    &:hover {
      border-color: #ff8e53;
    }

    &:focus {
      border-color: #ff8e53;
      box-shadow: 0 0 0 2px rgba(255, 142, 83, 0.1);
    }
  }
}

.custom-input-number {
  width: 100%;

  :deep(.ant-input-number) {
    width: 100%;
    height: 40px;
    border-radius: 8px;
    border-color: #e2e8f0;

    &:hover {
      border-color: #ff8e53;
    }

    &:focus {
      border-color: #ff8e53;
      box-shadow: 0 0 0 2px rgba(255, 142, 83, 0.1);
    }
  }

  :deep(.ant-input-number-input) {
    height: 38px;
  }
}

.custom-select {
  :deep(.ant-select-selector) {
    min-height: 40px !important;
    border-radius: 8px !important;
    border-color: #e2e8f0 !important;
    padding: 4px 11px !important;

    &:hover {
      border-color: #ff8e53 !important;
    }
  }

  :deep(.ant-select-focused .ant-select-selector) {
    border-color: #ff8e53 !important;
    box-shadow: 0 0 0 2px rgba(255, 142, 83, 0.1) !important;
  }

  :deep(.ant-select-selection-placeholder) {
    line-height: 32px;
  }
}

.submit-button {
  width: 100%;
  height: 40px;
  border-radius: 20px;
  font-size: 15px;
  font-weight: 500;
  background: linear-gradient(135deg, #ff8e53 0%, #ff6b6b 100%);
  border: none;
  box-shadow: 0 4px 12px rgba(255, 107, 107, 0.2);
  transition: all 0.3s ease;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 6px 16px rgba(255, 107, 107, 0.3);
  }

  &:active {
    transform: translateY(1px);
  }
}

/* 响应式调整 */
@media screen and (max-width: 768px) {
  #addPictureBatchPage {
    padding: 20px 12px;
    border-radius: 12px;
  }

  .page-title {
    font-size: 20px;
    margin-bottom: 20px;
  }

  .batch-form :deep(.ant-form-item) {
    margin-bottom: 20px;
  }
}
</style>
