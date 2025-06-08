<template>
  <div id="pictureManagePage">
    <!-- PC端展示 -->
    <template v-if="device === DEVICE_TYPE_ENUM.PC">
      <div class="pc-container">
        <!-- 搜索表单和操作按钮 -->
        <div class="search-and-button-container">
          <a-form layout="inline" :model="searchParams" @finish="doSearch">
            <a-form-item label="关键词">
              <a-input
                v-model:value="searchParams.searchText"
                placeholder="从名称和简介搜索"
                allow-clear
                class="compact-input"
              />
            </a-form-item>
            <a-form-item label="类型">
              <a-input
                v-model:value="searchParams.category"
                placeholder="请输入类型"
                allow-clear
                class="compact-input"
              />
            </a-form-item>
            <a-form-item label="标签">
              <a-input
                v-model:value="searchParams.tags"
                placeholder="请输入标签"
                allow-clear
                class="compact-input"
              />
            </a-form-item>
            <a-form-item name="reviewStatus" label="审核状态">
              <a-select
                v-model:value="searchParams.reviewStatus"
                class="compact-select"
                placeholder="请选择审核状态"
                :options="PIC_REVIEW_STATUS_OPTIONS"
                allow-clear
              />
            </a-form-item>
            <a-form-item>
              <a-button type="primary" html-type="submit" class="action-button search-button">
                <SearchOutlined />
                搜索
              </a-button>
            </a-form-item>
          </a-form>
          <!-- 操作按钮区 -->
          <div class="button-group">
            <a-button type="primary" href="/add_picture" class="action-button create-button">
              <PlusOutlined />
              创建图片
            </a-button>
            <a-button
              type="primary"
              href="/add_picture/batch"
              class="action-button batch-button"
            >
              <UploadOutlined />
              批量创建图片
            </a-button>
            <a-dropdown>
              <a-button
                type="primary"
                danger
                :disabled="!hasSelected"
                class="action-button danger-button"
              >
                <BoldOutlined />批量操作
              </a-button>
              <template #overlay>
                <a-menu>
                  <a-menu-item
                    v-for="option in OPERATION_OPTIONS"
                    :key="option.value"
                    @click="handleBatchOperation(option.value)"
                  >
                    {{ option.label }}
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </div>
        </div>

        <div class="table-section">
          <a-spin tip="正在加载中..." v-if="loading" class="loading-spin" />
          <!-- 表格 -->
          <a-table
            :row-selection="{
              selectedRowKeys: state.selectedRowKeys,
              onChange: onSelectChange,
            }"
            rowKey="id"
            :columns="columns"
            :data-source="dataList"
            :pagination="pagination"
            @change="doTableChange"
            class="custom-table"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'id'">
                {{ record.id }}
              </template>
              <template v-if="column.dataIndex === 'url'">
                <a-image :src="record.url" :width="120" />
              </template>
              <template v-if="column.dataIndex === 'tags'">
                <a-space wrap>
                  <a-tag
                    class="tag-item success-tag"
                    v-for="tag in JSON.parse(record.tags || '[]')"
                    :key="tag"
                  >
                    {{ tag }}
                  </a-tag>
                </a-space>
              </template>
              <template v-if="column.dataIndex === 'category'">
                <a-tag class="tag-item primary-tag">
                  {{ record.category }}
                </a-tag>
              </template>
              <template v-if="column.dataIndex === 'picInfo'">
                <div>格式：{{ record.picFormat }}</div>
                <div>宽度：{{ record.picWidth }}</div>
                <div>高度：{{ record.picHeight }}</div>
                <div>宽高比：{{ record.picScale }}</div>
                <div>大小：{{ (record.picSize / 1024).toFixed(2) }}KB</div>
              </template>
              <template v-if="column.dataIndex === 'reviewMessage'">
                <div>
                  审核状态：<span :class="getStatusColorClass(record.reviewStatus)">{{
                    PIC_REVIEW_STATUS_MAP[record.reviewStatus]
                  }}</span>
                </div>
                <div>审核信息：{{ record.reviewMessage }}</div>
                <div>审核人：{{ record.reviewerId }}</div>
                <div v-if="record.reviewTime">
                  审核时间：{{ dayjs(record.reviewTime).format('YYYY-MM-DD HH:mm:ss') }}
                </div>
              </template>
              <template v-if="column.dataIndex === 'createTime'">
                {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}
              </template>
              <template v-if="column.dataIndex === 'editTime'">
                {{ dayjs(record.editTime).format('YYYY-MM-DD HH:mm:ss') }}
              </template>
              <template v-if="column.key === 'action'">
                <a-space wrap>
                  <a-button
                    v-if="record.reviewStatus !== PIC_REVIEW_STATUS_ENUM.PASS"
                    type="primary"
                    class="table-button approve-button"
                    @click="handleReview(record, PIC_REVIEW_STATUS_ENUM.PASS)"
                  >
                    <CheckOutlined />
                    通过
                  </a-button>
                  <a-button
                    v-if="record.reviewStatus !== PIC_REVIEW_STATUS_ENUM.REJECT"
                    type="primary"
                    danger
                    class="table-button reject-button"
                    @click="showRejectModal(record)"
                  >
                    <CloseOutlined />
                    拒绝
                  </a-button>
                  <a-button
                    type="primary"
                    class="table-button edit-button"
                    :href="`/add_picture?id=${record.id}`"
                  >
                    <EditOutlined />
                    编辑
                  </a-button>
                  <a-button
                    type="primary"
                    class="table-button feature-button"
                    @click="handleFeature(record)"
                    :ghost="!record.isFeature"
                  >
                    <StarOutlined v-if="!record.isFeature" />
                    <StarFilled v-if="record.isFeature" />
                    {{ record.isFeature ? '精选' : '精选' }}
                  </a-button>
                  <a-button
                    danger
                    class="table-button delete-button"
                    @click="showDeleteConfirm(record)"
                  >
                    <DeleteOutlined />
                    删除
                  </a-button>
                </a-space>
              </template>
            </template>
          </a-table>
        </div>
      </div>
    </template>

    <!-- 移动端展示 -->
    <template v-else>
      <div class="mobile-container">
        <div class="mobile-content">
          <!-- 搜索区域 -->
          <div class="search-section">
            <van-search
              v-model="searchParams.searchText"
              placeholder="搜索图片名称和简介"
              @search="doSearch"
            />
          </div>

          <!-- 操作按钮区 -->
          <div class="action-bar">
            <van-button type="primary" size="small" @click="() => router.push('/add_picture')">
              <template #icon><PlusOutlined /></template>
              创建图片
            </van-button>
            <van-button
              type="primary"
              size="small"
              @click="() => router.push('/add_picture/batch')"
              plain
            >
              <template #icon><UploadOutlined /></template>
              批量创建
            </van-button>
            <van-button
              type="primary"
              size="small"
              danger
              :disabled="!hasSelected"
              @click="showActionSheet = true"
            >
              <template #icon><BoldOutlined /></template>
              批量操作
            </van-button>
          </div>

          <!-- 批量操作动作面板 -->
          <van-action-sheet
            v-model:show="showActionSheet"
            :actions="mobileOperationOptions"
            cancel-text="取消"
            close-on-click-action
            @select="handleBatchOperation"
          />

          <!-- 图片列表 -->
          <div class="picture-list">
            <van-checkbox-group v-model="state.selectedRowKeys">
              <van-cell-group inset v-for="picture in dataList" :key="picture.id">
                <van-card class="picture-card">
                  <template #thumb>
                    <van-image
                      :src="picture.url"
                      fit="cover"
                      width="100%"
                      height="120"
                      radius="8"
                      @click="showImagePreview(picture.url)"
                    />
                  </template>

                  <template #title>
                    <div class="card-title">{{ picture.name || '未命名' }}</div>
                  </template>

                  <template #desc>
                    <div class="card-desc">
                      <div class="desc-item">{{ picture.introduction || '暂无简介' }}</div>
                      <div class="desc-item">
                        <span class="label">用户ID:</span> {{ picture.userId }}
                      </div>
                      <div class="desc-item">
                        <span class="label">审核信息:</span> {{ picture.reviewMessage || '无' }}
                      </div>
                      <div class="desc-item" v-if="picture.reviewTime">
                        <span class="label">审核时间:</span>
                        {{ dayjs(picture.reviewTime).format('YYYY-MM-DD HH:mm:ss') }}
                      </div>
                      <div class="desc-item">
                        <span class="label">审核人:</span> {{ picture.reviewerId || '无' }}
                      </div>
                    </div>
                  </template>

                  <template #tags>
                    <div class="tag-container">
                      <van-tag
                        :type="getReviewStatusType(picture.reviewStatus)"
                        round
                        size="small"
                        style="margin-right: 4px"
                      >
                        {{ PIC_REVIEW_STATUS_MAP[picture.reviewStatus] }}
                      </van-tag>
                      <van-tag type="primary" round size="small" v-if="picture.category">
                        {{ picture.category }}
                      </van-tag>
                      <template v-if="picture.tags">
                        <van-tag
                          v-for="tag in JSON.parse(picture.tags)"
                          :key="tag"
                          type="success"
                          round
                          size="small"
                          style="margin-left: 4px"
                        >
                          {{ tag }}
                        </van-tag>
                      </template>
                    </div>
                  </template>

                  <template #footer>
                    <div class="card-footer">
                      <van-checkbox :name="picture.id" class="card-checkbox" />
                      <div class="button-group">
                        <van-button
                          v-if="picture.reviewStatus !== PIC_REVIEW_STATUS_ENUM.PASS"
                          size="mini"
                          type="primary"
                          plain
                          @click="handleReview(picture, PIC_REVIEW_STATUS_ENUM.PASS)"
                        >通过</van-button
                        >
                        <van-button
                          v-if="picture.reviewStatus !== PIC_REVIEW_STATUS_ENUM.REJECT"
                          size="mini"
                          type="danger"
                          plain
                          @click="showRejectModal(picture)"
                        >拒绝</van-button
                        >
                        <van-button
                          size="mini"
                          type="primary"
                          @click="() => router.push(`/add_picture?id=${picture.id}`)"
                        >编辑</van-button
                        >
                        <van-button
                          type="danger"
                          size="mini"
                          @click="showDeleteConfirm(picture)"
                        >删除</van-button
                        >
                        <a-button
                          type="primary"
                          size="small"
                          class="table-button feature-button"
                          @click="handleFeature(picture)"
                          :ghost="!picture.isFeature"
                        >
                          <StarOutlined v-if="!picture.isFeature" />
                          <StarFilled v-if="picture.isFeature" />
                          {{ picture.isFeature ? '精选' : '精选' }}
                        </a-button>
                      </div>
                    </div>
                  </template>

                  <template #price>
                    <div class="info-text">
                      <div>
                        {{ picture.picFormat }} | {{ (picture.picSize / 1024).toFixed(2) }}KB
                      </div>
                      <div>{{ picture.picWidth }}x{{ picture.picHeight }}</div>
                    </div>
                  </template>

                  <template #num>
                    <span class="create-time">
                      {{ dayjs(picture.createTime).format('YYYY-MM-DD HH:mm') }}
                    </span>
                  </template>
                </van-card>
              </van-cell-group>
            </van-checkbox-group>
          </div>

          <!-- 图片预览组件 -->
          <van-image-preview
            v-model:show="showPreview"
            :images="[previewImage]"
            :show-index="false"
            :closeable="true"
            @close="closePreview"
          />

          <!-- 移动端分页 -->
          <div class="mobile-pagination">
            <div class="pagination-info">
              <span>共 {{ total }} 条</span>
              <div class="page-size-selector" @click="showPageSizeSheet = true">
                <span>{{ searchParams.pageSize }}条/页</span>
                <van-icon name="arrow-down" />
              </div>
            </div>
            <div class="pagination-wrapper">
              <van-pagination
                v-model="searchParams.current"
                :total-items="total"
                :items-per-page="searchParams.pageSize"
                @change="onMobilePageChange"
                prev-text="<"
                next-text=">"
                :show-page-size="3"
                class="custom-pagination"
                force-ellipses
              />
              <div class="jump-page">
                <span>跳至</span>
                <van-field
                  v-model="jumpPage"
                  type="number"
                  :placeholder="searchParams.current.toString()"
                  input-align="center"
                  @keyup.enter="handleJumpPage"
                />
                <span>页</span>
              </div>
            </div>
          </div>

          <!-- 每页条数选择器抽屉 -->
          <van-action-sheet
            v-model:show="showPageSizeSheet"
            :actions="pageSizeOptions"
            cancel-text="取消"
            close-on-click-action
            @select="handlePageSizeChange"
          />
        </div>
      </div>
    </template>

    <!-- 拒绝弹框 -->
    <a-modal
      v-model:open="rejectModalVisible"
      title="拒绝原因"
      @ok="handleRejectConfirm"
      @cancel="handleRejectCancel"
      :confirmLoading="rejectLoading"
    >
      <div class="reject-form">
        <a-form layout="vertical">
          <a-form-item label="选择拒绝原因">
            <a-select
              v-model:value="selectedRejectReason"
              style="width: 100%"
              placeholder="请选择拒绝原因"
              @change="handleRejectReasonChange"
            >
              <a-select-option
                v-for="option in REJECT_REASON_OPTIONS"
                :key="option.value"
                :value="option.value"
              >
                {{ option.label }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="拒绝说明">
            <a-textarea
              v-model:value="rejectMessage"
              :rows="4"
              placeholder="请输入拒绝原因说明"
              :maxLength="200"
              show-count
            />
          </a-form-item>
        </a-form>
      </div>
    </a-modal>

    <!-- 删除确认弹框 -->
    <a-modal
      v-model:open="deleteConfirmVisible"
      :title="null"
      :footer="null"
      :width="400"
      class="delete-confirm-modal"
    >
      <div class="delete-confirm-content">
        <div class="warning-icon">
          <ExclamationCircleFilled style="color: #ff6b6b;" />
        </div>
        <h3 class="confirm-title">确认删除该图片？</h3>
        <p class="confirm-desc">
          图片名称：{{ selectedPicture?.name }}<br>
          图片大小：{{ (selectedPicture?.picSize ? (selectedPicture.picSize / 1024).toFixed(2) : 0) }}KB
        </p>
        <div class="confirm-actions">
          <a-button class="cancel-button" @click="deleteConfirmVisible = false">取消</a-button>
          <a-button class="confirm-button" danger @click="confirmDelete">
            确认删除
          </a-button>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  batchOperationPictureUsingPost,
  deletePictureUsingPost,
  doPictureReviewUsingPost,
  listPictureByPageUsingPost,
  setPictureFeatureUsingPost,
} from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'
import {
  BoldOutlined,
  PlusOutlined,
  UploadOutlined,
  SearchOutlined,
  CheckOutlined,
  CloseOutlined,
  EditOutlined,
  DeleteOutlined,
  ExclamationCircleFilled,
  StarOutlined,
  StarFilled,
} from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import {
  PIC_REVIEW_STATUS_ENUM,
  PIC_REVIEW_STATUS_MAP,
  PIC_REVIEW_STATUS_OPTIONS,
} from '../../constants/picture.ts'
import { getDeviceType } from '@/utils/device.ts'
import { DEVICE_TYPE_ENUM } from '@/constants/device.ts'
import { OPERATION_OPTIONS } from '@/constants/operation.ts'
import { REJECT_REASON_OPTIONS, REJECT_REASON_MAP } from '@/constants/review'
import { useRouter } from 'vue-router'
// 定义用于存储设备类型的响应式变量
// 页面加载时获取设备类型并获取数据
onMounted(async () => {
  device.value = await getDeviceType()
})
const router = useRouter()

const columns = [
  {
    title: '图片ID',
    dataIndex: 'id',
    width: 80,
  },
  {
    title: '图片',
    dataIndex: 'url',
  },
  {
    title: '名称',
    dataIndex: 'name',
  },
  {
    title: '简介',
    dataIndex: 'introduction',
    ellipsis: true,
  },
  {
    title: '类型',
    dataIndex: 'category',
  },
  {
    title: '标签',
    dataIndex: 'tags',
  },
  {
    title: '图片信息',
    dataIndex: 'picInfo',
  },
  {
    title: '审核信息',
    dataIndex: 'reviewMessage',
  },
  {
    title: '用户ID',
    dataIndex: 'userId',
    width: 80,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
  },
  {
    title: '编辑时间',
    dataIndex: 'editTime',
  },
  {
    title: '操作',
    key: 'action',
  },
]

// 定义数据
const dataList = ref<API.Picture[]>([])
const total = ref(0)

// 搜索条件
const searchParams = reactive<API.PictureQueryRequest>({
  current: 1,
  pageSize: 10,
  sortField: 'createTime',
  sortOrder: 'descend',
})

// 用于存储表格行选择状态的响应式对象
const state = reactive({
  selectedRowKeys: [] as number[],
  loading: false,
})

// 计算属性，判断是否有选中的行，用于控制批量删除按钮是否可用
const hasSelected = computed(() => state.selectedRowKeys.length > 0)

const loading = ref(false)

// 获取设备类型
const device = ref('')

// 获取审核状态对应的类型
const getReviewStatusType = (status) => {
  switch (status) {
    case PIC_REVIEW_STATUS_ENUM.REVIEWING:
      return 'warning'
    case PIC_REVIEW_STATUS_ENUM.PASS:
      return 'success'
    case PIC_REVIEW_STATUS_ENUM.REJECT:
      return 'danger'
    default:
      return 'default'
  }
}

// 添加拒绝相关的状态
const rejectModalVisible = ref(false)
const rejectLoading = ref(false)
const selectedRejectReason = ref('')
const rejectMessage = ref('')
const currentRejectRecord = ref<any>(null)

// 添加拒绝相关的方法
const showRejectModal = (record: any) => {
  currentRejectRecord.value = record
  selectedRejectReason.value = ''
  rejectMessage.value = ''
  rejectModalVisible.value = true
}

const handleRejectConfirm = async () => {
  if (!rejectMessage.value.trim()) {
    message.error('请输入拒绝原因')
    return
  }

  rejectLoading.value = true
  try {
    const res = await doPictureReviewUsingPost({
      id: currentRejectRecord.value.id,
      reviewStatus: PIC_REVIEW_STATUS_ENUM.REJECT,
      reviewMessage: rejectMessage.value,
    })

    if (res.data.code === 0) {
      message.success('操作成功')
      rejectModalVisible.value = false
      fetchData() // 刷新列表
    } else {
      message.error(res.data.message || '操作失败')
    }
  } catch (error) {
    message.error('操作失败')
  } finally {
    rejectLoading.value = false
  }
}

const handleRejectCancel = () => {
  rejectModalVisible.value = false
}
const handleRejectReasonChange = (value: string) => {
  if (value !== 'other') {
    rejectMessage.value = REJECT_REASON_MAP[value]
  } else {
    rejectMessage.value = ''
  }
}

// 修改获取数据的方法
const fetchData = async () => {
  if (device.value === DEVICE_TYPE_ENUM.PC) {
    loading.value = true
  }

  try {
    const res = await listPictureByPageUsingPost({
      ...searchParams,
      nullSpaceId: true,
    })
    if (res.data.code === 0 && res.data.data) {
      dataList.value = res.data.data.records ?? []
      total.value = res.data.data.total ?? 0
    } else {
      message.error('获取数据失败，' + res.data.message)
    }
  } catch (error) {
    message.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

// 页面加载时获取数据，请求一次
onMounted(() => {
  fetchData()
})

// 分页参数
const pagination = computed(() => {
  return {
    current: searchParams.current,
    pageSize: searchParams.pageSize,
    total: total.value,
    showSizeChanger: true,
    showTotal: (total) => `共 ${total} 条`,
  }
})

// 表格变化之后，重新获取数据
const doTableChange = (page: any) => {
  searchParams.current = page.current
  searchParams.pageSize = page.pageSize
  fetchData()
}

// 搜索数据
const doSearch = () => {
  searchParams.current = 1
  fetchData()
}

// 删除数据
const doDelete = async (id: string) => {
  if (!id) {
    return
  }
  const res = await deletePictureUsingPost({ id })
  if (res.data.code === 0) {
    message.success('删除成功，数据更新可能需要一段时间')
    // 刷新数据
    fetchData()
  } else {
    message.error('删除失败')
  }
}

// 处理行选择变化的回调函数
const onSelectChange = (selectedRowKeys: number[]) => {
  state.selectedRowKeys = selectedRowKeys
}

// 移动端批量操作选项
const mobileOperationOptions = [
  { name: OPERATION_OPTIONS[0].label, value: OPERATION_OPTIONS[0].value, color: '#ee0a24' },
  { name: OPERATION_OPTIONS[1].label, value: OPERATION_OPTIONS[1].value },
  { name: OPERATION_OPTIONS[2].label, value: OPERATION_OPTIONS[2].value, color: '#ee0a24' },
]

// 修改批量操作处理方法
const handleBatchOperation = async (action: any) => {
  if (state.selectedRowKeys.length === 0) {
    message.warning('请先选择要操作的图片')
    return
  }

  const selectedPictureIds = state.selectedRowKeys
  try {
    const body = {
      ids: selectedPictureIds,
      operationType: device.value === DEVICE_TYPE_ENUM.PC ? action : action.value,
    }

    const res = await batchOperationPictureUsingPost(body)
    if (res.data.code === 0) {
      const operationLabel =
        device.value === DEVICE_TYPE_ENUM.PC
          ? OPERATION_OPTIONS.find((opt) => opt.value === action)?.label
          : action.name
      message.success(`${operationLabel}成功`)
      state.selectedRowKeys = []
      fetchData()
      if (device.value !== DEVICE_TYPE_ENUM.PC) {
        showActionSheet.value = false
      }
    } else {
      message.error('批量操作失败')
    }
  } catch (error) {
    message.error('批量操作出现异常，请稍后再试')
  }
}

// 审核图片
const handleReview = async (record: API.Picture, reviewStatus: number) => {
  const reviewMessage =
    reviewStatus === PIC_REVIEW_STATUS_ENUM.PASS ? '管理员操作通过' : '管理员操作拒绝'
  const res = await doPictureReviewUsingPost({
    id: record.id,
    reviewStatus,
    reviewMessage,
  })
  if (res.data.code === 0) {
    message.success('审核操作成功')
    // 重新获取列表数据
    fetchData()
  } else {
    message.error('审核操作失败，' + res.data.message)
  }
}
// 新增：根据审核状态返回对应的颜色类名的方法
const getStatusColorClass = (status) => {
  switch (status) {
    case PIC_REVIEW_STATUS_ENUM.REVIEWING:
      return 'reviewing-color'
    case PIC_REVIEW_STATUS_ENUM.PASS:
      return 'pass-color'
    case PIC_REVIEW_STATUS_ENUM.REJECT:
      return 'reject-color'
    default:
      return ''
  }
}

// 移动端分页变化
const onMobilePageChange = (page: number) => {
  searchParams.current = page
  fetchData()
}

// 添加动作面板显示状态
const showActionSheet = ref(false)

// 图片预览相关的状态
const showPreview = ref(false)
const previewImage = ref('')

// 显示图片预览
const showImagePreview = (url: string) => {
  previewImage.value = url
  showPreview.value = true
}

// 关闭图片预览
const closePreview = () => {
  showPreview.value = false
}

// 每页条数选择器的状态和选项
const showPageSizeSheet = ref(false)
const pageSizeOptions = [
  { name: '10条/页', value: 10 },
  { name: '20条/页', value: 20 },
  { name: '30条/页', value: 30 },
  { name: '50条/页', value: 50 },
]

// 修改每页条数改变的处理方法
const handlePageSizeChange = (action: { value: number }) => {
  const value = action.value
  searchParams.current = 1
  searchParams.pageSize = value
  fetchData()
}

// 跳转页码
const jumpPage = ref('')

// 处理页码跳转
const handleJumpPage = () => {
  const page = parseInt(jumpPage.value)
  if (isNaN(page)) {
    return
  }

  const maxPage = Math.ceil(total.value / searchParams.pageSize)
  if (page < 1 || page > maxPage) {
    message.warning(`请输入1-${maxPage}之间的页码`)
    return
  }

  searchParams.current = page
  fetchData()
  jumpPage.value = ''
}

// 删除确认相关的状态
const deleteConfirmVisible = ref(false)
const selectedPicture = ref<API.PictureVO | null>(null)

// 显示删除确认框
const showDeleteConfirm = (picture: API.PictureVO) => {
  selectedPicture.value = picture
  deleteConfirmVisible.value = true
}

// 确认删除
const confirmDelete = async () => {
  if (!selectedPicture.value?.id) return

  try {
    const res = await deletePictureUsingPost({ id: selectedPicture.value.id })
    if (res.data.code === 0) {
      message.success('删除成功')
      deleteConfirmVisible.value = false
      // 刷新数据
      fetchData()
    } else {
      message.error('删除失败：' + res.data.message)
    }
  } catch (error) {
    message.error('删除失败，请稍后重试')
  }
}

// 添加精选处理函数
const handleFeature = async (record: API.PictureVO) => {
  try {
    await setPictureFeatureUsingPost({
      id: record.id,
      isFeature: record.isFeature ? 0 : 1
    })
    // message.success(record.isFeature ? '已取消精选' : '已设为精选')
    await fetchData()
  } catch (error) {
    message.error('操作失败')
  }
}
</script>

<style scoped>
/* 移动端样式 */
.mobile-container {
  background: #f7f8fa;
  min-height: 100vh;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  overflow: hidden;
  padding-bottom: 50px;
}

.mobile-content {
  height: 100%;
  overflow-y: auto;
  padding: 12px;
  -webkit-overflow-scrolling: touch;
  padding-bottom: 80px;
}

.action-bar {
  padding: 12px;
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: flex-end;
  background: white;
  border-radius: 8px;
  margin-bottom: 12px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.action-bar .van-button {
  flex: 1;
  max-width: 120px;
}

:deep(.van-cell-group--inset) {
  margin: 0 12px 12px;
}

:deep(.van-cell-group--inset:last-child) {
  margin-bottom: 0;
}

.picture-card {
  background: white;
  border-radius: 8px;
  overflow: hidden;
  padding: 12px;
}

.card-title {
  font-size: 15px;
  font-weight: 500;
  margin-bottom: 4px;
}

.card-desc {
  font-size: 13px;
  color: #666;
  margin: 4px 0;
  line-height: 1.4;
}

.tag-container {
  margin: 8px 0;
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}

.button-group {
  display: flex;
  gap: 8px;
}

.info-text {
  font-size: 12px;
  color: #666;
}

.create-time {
  font-size: 12px;
  color: #999;
}

:deep(.van-card__content) {
  padding-left: 8px;
}

.reviewing-color {
  color: #ff943f;
}

.pass-color {
  color: green;
}

.reject-color {
  color: red;
}

/* 移动端搜索区域样式 */
.search-section {
  position: sticky;
  top: -12px;
  z-index: 100;
  background: #f7f8fa;
  /* 抵消父元素的内边距 */
  margin: 0 -12px 12px;
}

/* 卡片描述样式 */
.card-desc {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.desc-item {
  font-size: 13px;
  color: #666;
  line-height: 1.4;
}

.label {
  color: #999;
  margin-right: 4px;
}

/* 下拉菜单样式 */
:deep(.van-dropdown-menu) {
  box-shadow: none;
  background: transparent;
}

:deep(.van-dropdown-menu__item) {
  justify-content: flex-start;
}

/* 移动端分页样式优化 */
.mobile-pagination {
  margin-top: 16px;
  padding: 12px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.pagination-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  color: #666;
  font-size: 13px;
}

:deep(.van-dropdown-menu) {
  height: 24px;
  border: 1px solid #ebedf0;
  border-radius: 4px;
  min-width: 90px;
}

:deep(.van-dropdown-menu__title) {
  padding: 0 8px;
  font-size: 12px;
  line-height: 22px;
}

:deep(.van-pagination) {
  width: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 4px;
}

:deep(.van-pagination__item) {
  min-width: 28px;
  height: 28px;
  line-height: 28px;
  border-radius: 16px;
  font-size: 14px;
  border: 1px solid #ebedf0;
  margin: 0 2px;
}

:deep(.van-pagination__item--active) {
  background: #1989fa;
  color: white;
  border-color: #1989fa;
}

:deep(.van-pagination__prev),
:deep(.van-pagination__next) {
  background: #f7f8fa;
  border: 1px solid #ebedf0;
  font-weight: bold;
  min-width: 28px !important;
  height: 28px !important;
  line-height: 28px !important;
}

:deep(.van-pagination__item--disabled) {
  background: transparent;
  border: none;
  color: #999;
}

/* 下拉菜单样式优化 */
:deep(.van-dropdown-menu) {
  height: 24px;
  border: 1px solid #ebedf0;
  border-radius: 4px;
  min-width: 90px;
}

:deep(.van-dropdown-menu__title) {
  padding: 0 8px;
  font-size: 12px;
  line-height: 22px;
}

:deep(.van-dropdown-menu__bar) {
  height: 24px;
}

:deep(.van-dropdown-item__option) {
  padding: 8px 12px;
  font-size: 13px;
}

:deep(.van-dropdown-item__option--active) {
  color: #1989fa;
}

.picture-list {
  margin-bottom: 16px;
}

/* 搜索框样式优化 */
:deep(.van-search) {
  padding: 8px 12px;
  background: transparent;
}

:deep(.van-search__content) {
  border-radius: 8px;
  background: white;
  margin: 0 12px;
}

/* 图片预览样式优化 */
:deep(.van-image) {
  cursor: pointer;
}

:deep(.van-image-preview__image) {
  background-color: #000;
}

:deep(.van-image-preview__close-icon) {
  color: #fff;
}

/* 每页条数选择器样式 */
.page-size-selector {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  border: 1px solid #ebedf0;
  border-radius: 4px;
  background: white;
  cursor: pointer;
}

.page-size-selector .van-icon {
  font-size: 12px;
  color: #999;
}

/* 分页器包装器 */
.pagination-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

/* 跳转页码样式 */
.jump-page {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #666;
}

.jump-page :deep(.van-field) {
  width: 48px;
  padding: 0;
}

.jump-page :deep(.van-field__control) {
  height: 28px;
  padding: 0 4px;
  text-align: center;
  border: 1px solid #ebedf0;
  border-radius: 4px;
  font-size: 13px;
}

/* 隐藏输入框的上下箭头 */
.jump-page :deep(.van-field__control::-webkit-inner-spin-button),
.jump-page :deep(.van-field__control::-webkit-outer-spin-button) {
  -webkit-appearance: none;
  margin: 0;
}

.jump-page :deep(.van-field__control) {
  -moz-appearance: textfield;
}

/* PC端容器样式 */
.pc-container {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.06);
}

/* 搜索和按钮区域样式 */
.search-and-button-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.button-group {
  display: flex;
  gap: 12px;
  align-items: center;
}

/* 按钮样式优化 */
.create-button {
  background: linear-gradient(135deg, #40c9ff 0%, #1890ff 100%);
  border: none;
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.2);
}

.create-button:hover {
  background: linear-gradient(135deg, #69d4ff 0%, #40a9ff 100%);
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(24, 144, 255, 0.3);
}

.create-button:active {
  background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
  transform: translateY(0);
}

.batch-button {
  background: linear-gradient(135deg, #722ed1 0%, #531dab 100%);
  border: none;
  box-shadow: 0 4px 12px rgba(114, 46, 209, 0.2);
  color: white;
}

.batch-button:hover {
  background: linear-gradient(135deg, #8c51e0 0%, #722ed1 100%);
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(114, 46, 209, 0.3);
}

.batch-button:active {
  background: linear-gradient(135deg, #531dab 0%, #391085 100%);
  transform: translateY(0);
}

.danger-button {
  background: linear-gradient(135deg, #ff7875 0%, #ff4d4f 100%);
  border: none;
  box-shadow: 0 4px 12px rgba(255, 77, 79, 0.2);
}

.danger-button:hover {
  background: linear-gradient(135deg, #ff4d4f 0%, #f5222d 100%);
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(255, 77, 79, 0.3);
}

.danger-button:active {
  background: linear-gradient(135deg, #cf1322 0%, #a8071a 100%);
  transform: translateY(0);
}

.danger-button:disabled {
  background: #f5f5f5;
  color: rgba(0, 0, 0, 0.25);
  box-shadow: none;
  transform: none;
}

/* 操作按钮样式 */
.action-button {
  height: 32px;
  border-radius: 8px;
  padding: 0 16px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition: all 0.3s ease;
  font-weight: 500;
  color: white;
  border: none;
}

.action-button:hover {
  color: white;
}

.action-button .anticon {
  font-size: 16px;
}

/* 表格区域样式 */
.table-section {
  background: white;
  border-radius: 8px;
}

/* 表格样式优化 */
:deep(.custom-table) {
  .ant-table-thead > tr > th {
    background: #fafafa;
    font-weight: 500;
    color: #1f2937;
    padding: 12px 16px;
  }

  .ant-table-tbody > tr > td {
    padding: 12px 16px;
  }

  .ant-table-tbody > tr:hover > td {
    background: #f0f7ff;
  }

  .ant-table-row-selected > td {
    background: #e6f4ff !important;
  }
}

/* 搜索表单样式优化 */
:deep(.ant-form-inline) {
  .ant-form-item {
    margin-right: 12px;
    margin-bottom: 0;
  }

  .ant-form-item-label {
    padding-right: 6px;
    font-size: 13px;
    color: #666;
  }
}

/* 紧凑型输入框样式 */
.compact-input {
  width: 140px !important;
  height: 32px;
  border-radius: 6px;
}

/* 紧凑型下拉框样式 */
.compact-select {
  width: 120px !important;
}

/* 搜索按钮样式 */
.search-button {
  background: linear-gradient(135deg, #36cfc9 0%, #13c2c2 100%);
  border: none;
  box-shadow: 0 4px 12px rgba(19, 194, 194, 0.2);
}

.search-button:hover {
  background: linear-gradient(135deg, #40d9d4 0%, #36cfc9 100%);
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(19, 194, 194, 0.3);
}

.search-button:active {
  background: linear-gradient(135deg, #13c2c2 0%, #08979c 100%);
  transform: translateY(0);
}

/* 表格中的标签样式 */
.tag-item {
  border: none;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.success-tag {
  background: linear-gradient(135deg, #95de64 0%, #52c41a 100%);
  color: white;
}

.primary-tag {
  background: linear-gradient(135deg, #69c0ff 0%, #1890ff 100%);
  color: white;
}

/* 表格中的按钮样式 */
.table-button {
  height: 28px;
  border-radius: 6px;
  padding: 0 12px;
  font-size: 13px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: none;
  transition: all 0.3s ease;
}

.table-button .anticon {
  font-size: 14px;
}

.approve-button {
  background: linear-gradient(135deg, #b7eb8f 0%, #52c41a 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(82, 196, 26, 0.2);
}

.approve-button:hover {
  background: linear-gradient(135deg, #95de64 0%, #52c41a 100%);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(82, 196, 26, 0.3);
}

.reject-button {
  background: linear-gradient(135deg, #ffa39e 0%, #ff4d4f 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(255, 77, 79, 0.2);
}

.reject-button:hover {
  background: linear-gradient(135deg, #ff7875 0%, #ff4d4f 100%);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(255, 77, 79, 0.3);
}

.edit-button {
  background: linear-gradient(135deg, #91d5ff 0%, #1890ff 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.2);
}

.edit-button:hover {
  background: linear-gradient(135deg, #69c0ff 0%, #1890ff 100%);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.3);
}

.delete-button {
  background: linear-gradient(135deg, #ffbb96 0%, #fa541c 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(250, 84, 28, 0.2);
}

.delete-button:hover {
  background: linear-gradient(135deg, #ff9c6e 0%, #fa541c 100%);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(250, 84, 28, 0.3);
}

/* 按钮激活状态 */
.table-button:active {
  transform: translateY(0);
}

/* 删除确认弹框样式 */
:deep(.delete-confirm-modal) {
  .ant-modal-content {
    padding: 0;
    border-radius: 16px;
    overflow: hidden;
  }

  .ant-modal-body {
    padding: 0;
  }
}

.delete-confirm-content {
  padding: 32px 24px;
  text-align: center;
}

.warning-icon {
  font-size: 48px;
  margin-bottom: 16px;

  .anticon {
    animation: pulse 2s infinite;
  }
}

.confirm-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 12px;
}

.confirm-desc {
  font-size: 14px;
  color: #64748b;
  margin-bottom: 24px;
  line-height: 1.6;
}

.confirm-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.cancel-button {
  min-width: 100px;
  height: 38px;
  border-radius: 19px;
  border: 1px solid #e2e8f0;
  color: #64748b;
  font-size: 14px;
  transition: all 0.3s ease;

  &:hover {
    color: #1a1a1a;
    border-color: #94a3b8;
    background: #f8fafc;
  }
}

.confirm-button {
  min-width: 100px;
  height: 38px;
  border-radius: 19px;
  background: #ff6b6b;
  border: none;
  color: white;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;

  &:hover {
    background: #ff5252;
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(255, 107, 107, 0.2);
  }

  &:active {
    transform: translateY(1px);
  }
}

@keyframes pulse {
  0% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.1);
    opacity: 0.8;
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .delete-confirm-content {
    padding: 24px 16px;
  }

  .warning-icon {
    font-size: 40px;
  }

  .confirm-title {
    font-size: 16px;
  }

  .confirm-desc {
    font-size: 13px;
  }

  .confirm-actions {
    gap: 8px;
  }

  .cancel-button,
  .confirm-button {
    min-width: 90px;
    height: 36px;
    font-size: 13px;
  }
}

.feature-button {
  &.ant-btn-primary:not(.ant-btn-dangerous) {
    &:not(:disabled) {
      color: #ff8e53;
      border-color: #ff8e53;

      &:not(.ant-btn-ghost) {
        background: #ff8e53;
        color: white;
      }

      &:hover {
        color: #ff7a3c;
        border-color: #ff7a3c;

        &:not(.ant-btn-ghost) {
          background: #ff7a3c;
          color: white;
        }
      }
    }
  }
}
</style>
