<template>
  <div class="batch-edit-picture-modal">
    <a-modal
      v-model:visible="visible"
      title="批量编辑图片"
      :footer="false"
      @cancel="closeModal"
      class="custom-modal"
      :width="500"
    >
      <div class="modal-content">
        <a-typography-paragraph class="tip-text"> * 只对当前页面的图片生效 </a-typography-paragraph>

        <!-- 批量创建表单 -->
        <a-form
          name="formData"
          layout="vertical"
          :model="formData"
          @finish="handleSubmit"
          class="edit-form"
        >
          <a-form-item name="category" label="分类">
            <a-auto-complete
              v-model:value="formData.category"
              placeholder="请输入分类"
              :options="categoryOptions"
              allow-clear
              class="custom-input"
            />
          </a-form-item>

          <a-form-item name="tags" label="标签">
            <a-select
              v-model:value="formData.tags"
              mode="tags"
              placeholder="请输入标签"
              :options="tagOptions"
              allow-clear
              class="custom-select"
            />
          </a-form-item>

          <a-form-item name="nameRule" label="命名规则">
            <a-input
              v-model:value="formData.nameRule"
              placeholder="请输入命名规则，输入 {序号} 可动态生成"
              allow-clear
              class="custom-input"
            />
          </a-form-item>

          <a-form-item>
            <a-button type="primary" html-type="submit" class="submit-button"> 确认修改 </a-button>
          </a-form-item>
        </a-form>
      </div>
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
import { onMounted, reactive, ref } from 'vue'
import {
  editPictureByBatchUsingPost,
  listPictureTagCategoryUsingGet,
} from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'

interface Props {
  pictureList: API.PictureVO[]
  spaceId: number
  onSuccess: () => void
}

const props = withDefaults(defineProps<Props>(), {})

// 是否可见
const visible = ref(false)

// 打开弹窗
const openModal = () => {
  visible.value = true
}

// 关闭弹窗
const closeModal = () => {
  visible.value = false
}

// 暴露函数给父组件
defineExpose({
  openModal,
})

const formData = reactive<API.PictureEditByBatchRequest>({
  category: '',
  tags: [],
  nameRule: '',
})

/**
 * 提交表单
 * @param values
 */
const handleSubmit = async (values: any) => {
  if (!props.pictureList) {
    return
  }
  const res = await editPictureByBatchUsingPost({
    pictureIdList: props.pictureList.map((picture) => picture.id),
    spaceId: props.spaceId,
    ...values,
  })
  // 操作成功
  if (res.data.code === 0 && res.data.data) {
    // message.success('操作成功')
    closeModal()
    props.onSuccess?.()
  } else {
    message.error('操作失败，' + res.data.message)
  }
}

const categoryOptions = ref<string[]>([])
const tagOptions = ref<string[]>([])

/**
 * 获取标签和分类选项
 * @param values
 */
const getTagCategoryOptions = async () => {
  const res = await listPictureTagCategoryUsingGet()
  if (res.data.code === 0 && res.data.data) {
    tagOptions.value = (res.data.data.tagList ?? []).map((data: string) => {
      return {
        value: data,
        label: data,
      }
    })
    categoryOptions.value = (res.data.data.categoryList ?? []).map((data: string) => {
      return {
        value: data,
        label: data,
      }
    })
  } else {
    message.error('获取标签分类列表失败，' + res.data.message)
  }
}

onMounted(() => {
  getTagCategoryOptions()
})
</script>

<style scoped>
.custom-modal {
  :deep(.ant-modal-content) {
    padding: 0;
    border-radius: 16px;
    overflow: hidden;
  }

  :deep(.ant-modal-header) {
    padding: 16px 24px;
    margin: 0;
    border-bottom: 1px solid #f0f0f0;
  }

  :deep(.ant-modal-body) {
    padding: 24px;
  }

  :deep(.ant-modal-close) {
    top: 16px;
  }
}

.modal-content {
  .tip-text {
    color: #94a3b8;
    font-size: 13px;
    margin-bottom: 20px;
  }
}

.edit-form {
  :deep(.ant-form-item-label) {
    padding-bottom: 8px;

    label {
      color: #64748b;
      font-size: 14px;
      font-weight: 500;
    }
  }

  .custom-input {
    :deep(.ant-input),
    :deep(.ant-input-affix-wrapper) {
      border-radius: 8px;
      border-color: #e2e8f0;
      padding: 8px 12px;
      transition: all 0.3s ease;

      &:hover,
      &:focus {
        border-color: #ff8e53;
        box-shadow: 0 0 0 2px rgba(255, 142, 83, 0.1);
      }
    }
  }

  .custom-select {
    :deep(.ant-select-selector) {
      border-radius: 8px !important;
      border-color: #e2e8f0 !important;
      padding: 4px 12px !important;
      min-height: 40px;
    }

    :deep(.ant-select:not(.ant-select-disabled)):hover .ant-select-selector {
      border-color: #ff8e53 !important;
    }

    :deep(
      .ant-select-focused:not(.ant-select-disabled).ant-select:not(.ant-select-customize-input)
        .ant-select-selector
    ) {
      border-color: #ff8e53 !important;
      box-shadow: 0 0 0 2px rgba(255, 142, 83, 0.1) !important;
    }

    :deep(.ant-select-selection-item) {
      background: #fff6f3;
      border-color: #ffb071;
      color: #ff8e53;
    }
  }

  .submit-button {
    width: 100%;
    height: 40px;
    border-radius: 8px;
    background: linear-gradient(135deg, #ff8e53 0%, #ff6b6b 100%);
    border: none;
    font-weight: 500;
    box-shadow: 0 2px 6px rgba(255, 107, 107, 0.2);
    transition: all 0.3s ease;
    margin-top: 8px;

    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba(255, 107, 107, 0.3);
    }

    &:active {
      transform: translateY(1px);
    }
  }
}

/* 响应式调整 */
@media screen and (max-width: 768px) {
  .edit-form {
    :deep(.ant-form-item-label) label {
      font-size: 13px;
    }

    .submit-button {
      height: 36px;
    }
  }
}
</style>
