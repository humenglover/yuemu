<template>
  <div class="container">
    <div id="spaceDetailPage">
      <!-- 顶部搜索栏（仅一行） -->
      <div class="search-header">
        <div class="search-form-container">
          <PictureSearchForm
            ref="searchFormRef"
            :search-params="searchParams"
            @search="onSearch"
            @colorChange="onColorChange"
          />
        </div>

        <!-- 右侧更多按钮 -->
        <button v-if="!isStatusLoading" class="more-btn" @click="toggleMoreModal">
          <i class="fas fa-ellipsis-v more-icon"></i>
          <span style="margin-left: 6px">更多</span>
        </button>
      </div>

      <!-- 顶部轮播图展示，仅在团队空间且有活动时显示 -->
      <div class="top-carousel-section" v-if="!isStatusLoading && space.spaceType !== SPACE_TYPE_ENUM.PRIVATE && activities && activities.length > 0">
        <div class="carousel-wrapper">
          <CustomPcCarousel
            :activities="activities"
            @activityClick="handleActivityClick"
          />
        </div>
      </div>

      <!-- 申请加入按钮（仅当用户不是空间成员时显示在主界面） -->
      <div v-if="!isStatusLoading && !userSpaceStatus.isMember" class="apply-section-main">
        <button class="apply-btn-main" @click="applyToJoinSpace">
          <i class="fas fa-user-plus apply-icon-main"></i>
          <span>{{ userSpaceStatus.hasPending ? '审核中...' : '申请加入空间' }}</span>
        </button>
      </div>


      <!-- 图片列表区域 -->
      <div class="content-wrap" v-if="!isStatusLoading">
        <van-pull-refresh v-model="loading" @refresh="onRefresh" :distance="80" :head-height="40">
          <GridPictureList
            v-if="userSpaceStatus.isMember"
            :dataList="dataList"
            :loading="loading"
            :canEdit="canEditPicture"
            :canDelete="canDeletePicture"
            :showOp="true"
            :onReload="fetchData"
            :hasMore="hasMore"
            :onLoadMore="loadMore"
          />
          <div v-else class="private-space-info">
            <div class="private-space-message">
              <i class="fas fa-lock" style="margin-bottom: 12px; font-size: 24px; color: #bfbfbf;"></i>
              <div>您没有权限查看此空间的内容</div>
            </div>
          </div>
        </van-pull-refresh>
      </div>
      <!-- 右下角上传按钮（悬浮固定） -->
      <button
        v-if="!isStatusLoading && canUploadPicture"
        class="upload-btn"
        @click="handleUploadClick"
      >
        <i class="fas fa-upload upload-icon"></i>
      </button>

      <!-- 自定义空间详情弹窗（div实现，不影响底层滚动） -->
      <div v-if="moreModalVisible" class="modal-overlay" @click="toggleMoreModal">
        <div class="modal-content" @click.stop>
          <!-- 弹窗头部 -->
          <div class="modal-header">
            <h3 class="space-name">{{ space.spaceName }}</h3>
            <button class="close-btn" @click="toggleMoreModal">
              <i class="fas fa-times close-icon"></i>
            </button>
          </div>

          <!-- 空间详情内容 -->
          <div class="space-detail-content">
            <!-- 空间基本信息 -->
            <div class="space-basic-info">
              <div class="creator-info">
                <span class="label">创建者：</span>
                <span class="value">{{ space.user?.userName || '未知' }}</span>
              </div>
              <div class="space-type-info">
                <span class="label">空间类型：</span>
                <span class="value">{{ space.spaceType === 0 ? '私有空间' : '团队空间' }}</span>
              </div>
              <div class="create-time">
                <span class="label">创建时间：</span>
                <span class="value">{{ space.createTime ? formatTime(space.createTime) : '未知' }}</span>
              </div>
              <div class="space-desc-info">
                <span class="label">空间简介：</span>
                <span class="value">{{ space.spaceDesc || '暂无简介' }}</span>
              </div>
            </div>

            <!-- 空间容量信息 -->
            <div class="capacity-info">
              <div class="capacity-label">空间容量</div>
              <div class="capacity-stats-row">
                <div class="capacity-ring-chart">
                  <VChart
                    :option="gaugeOption"
                    :style="{ width: '60px', height: '60px' }"
                    autoresize
                  />
                </div>
                <div class="capacity-and-count">
                  <div class="capacity-text">
                    <span class="capacity-used">{{ formatSize(space.totalSize) }}</span>
                    <span class="capacity-slash">/</span>
                    <span class="capacity-max">{{ formatSize(space.maxSize) }}</span>
                  </div>
                  <div class="count-info">
                    <span class="count-number">{{ space.totalCount || 0 }}</span>
                    <span class="count-label">/{{ space.maxCount || 0 }}张</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 申请加入按钮（仅当用户不是空间成员且空间类型不是私有空间时显示） -->
            <div v-if="!isStatusLoading && !userSpaceStatus.isMember && space.spaceType !== SPACE_TYPE_ENUM.PRIVATE" class="apply-section">
              <button class="op-btn" @click="applyToJoinSpace">
                <i class="fas fa-user-plus op-icon"></i>
                <span>{{ userSpaceStatus.hasPending ? '审核中...' : '申请加入' }}</span>
              </button>
            </div>

            <!-- 操作按钮列表（仅当用户是空间成员时显示） -->
            <div v-else-if="!isStatusLoading && userSpaceStatus.isMember" class="grid-operation-list">
              <!-- 成员管理 -->
              <button
                v-if="canManageSpaceUser && space.spaceType === SPACE_TYPE_ENUM.TEAM"
                class="grid-op-btn"
                @click="handleNavigate(`/spaceUserManage/${id}`)"
              >
                <i class="fas fa-users-cog grid-op-icon member-icon"></i>
                <span>成员管理</span>
                <span v-if="pendingApplicationsCount > 0" class="badge">{{ pendingApplicationsCount }}</span>
              </button>

              <!-- 团队聊天 -->
              <button
                v-if="space.spaceType === SPACE_TYPE_ENUM.TEAM"
                class="grid-op-btn"
                @click="handleNavigate(`/space_chat/${id}`)"
              >
                <i class="fas fa-comments grid-op-icon chat-icon"></i>
                <span>团队聊天</span>
              </button>

              <!-- 空间分析 -->
              <button
                v-if="canManageSpaceUser"
                class="grid-op-btn"
                @click="handleNavigate(`/space_analyze?spaceId=${id}`)"
              >
                <i class="fas fa-chart-pie grid-op-icon analyze-icon"></i>
                <span>空间分析</span>
              </button>

              <!-- 批量编辑 -->
              <button
                v-if="canEditPicture"
                class="grid-op-btn"
                @click="doBatchEdit"
              >
                <i class="fas fa-edit grid-op-icon edit-icon"></i>
                <span>批量编辑</span>
              </button>

              <!-- 退出空间 -->
              <button
                v-if="canQuitSpace"
                class="grid-op-btn"
                @click="handleQuitSpace"
              >
                <i class="fas fa-sign-out-alt grid-op-icon exit-icon"></i>
                <span>退出空间</span>
              </button>

              <!-- 空间活动 -->
              <button
                v-if="canManageSpaceUser && space.spaceType !== SPACE_TYPE_ENUM.PRIVATE"
                class="grid-op-btn"
                @click="handleNavigate(`/space/${String(id)}/activityManage`)"
              >
                <i class="fas fa-calendar-alt grid-op-icon activity-icon"></i>
                <span>空间活动</span>
              </button>

              <!-- 编辑空间 - 参与网格布局 -->
              <button v-if="canManageSpaceUser && userSpaceStatus.isMember" class="grid-op-btn" @click="openEditSpaceModal">
                <i class="fas fa-edit grid-op-icon edit-space-icon"></i>
                <span>编辑空间</span>
              </button>
            </div>



          </div>
        </div>
      </div>

      <!-- 编辑空间模态框 -->
      <div v-if="showEditSpaceModal" class="modal-overlay" @click="cancelEditSpace">
        <div class="modal-content" @click.stop style="max-width: 500px;">
          <div class="modal-header">
            <h3 class="space-name">编辑空间</h3>
            <button class="close-btn" @click="cancelEditSpace">
              <i class="fas fa-times close-icon"></i>
            </button>
          </div>
          <div class="space-detail-content" style="padding: 24px;">
            <div class="form-item" style="margin-bottom: 20px;">
              <label class="form-label" style="display: block; margin-bottom: 8px; font-weight: 500;">空间名称</label>
              <input
                v-model="editSpaceForm.spaceName"
                type="text"
                placeholder="请输入空间名称"
                class="form-input"
                style="width: 100%; padding: 10px; border: 1px solid #d9d9d9; border-radius: 6px;"
              />
            </div>

            <div class="form-item" style="margin-bottom: 20px;">
              <label class="form-label" style="display: block; margin-bottom: 8px; font-weight: 500;">空间简介</label>
              <textarea
                v-model="editSpaceForm.spaceDesc"
                placeholder="请输入空间简介"
                class="form-input"
                style="width: 100%; padding: 10px; border: 1px solid #d9d9d9; border-radius: 6px; height: 80px; resize: vertical;"
              />
            </div>

            <div class="form-item" style="margin-bottom: 20px;">
              <label class="form-label" style="display: block; margin-bottom: 8px; font-weight: 500;">封面图</label>
              <div style="display: flex; align-items: center; gap: 12px;">
                <div
                  class="image-preview"
                  @click="!isCoverUploading && triggerFileSelect()"
                  style="width: 120px; height: 80px; border: 1px dashed #d9d9d9; border-radius: 6px; display: flex; align-items: center; justify-content: center; cursor: pointer; overflow: hidden; position: relative;"
                >
                  <!-- 加载状态遮罩 -->
                  <div v-if="isCoverUploading" class="upload-loading-mask" style="position: absolute; top:0; left:0; right:0; bottom:0; background: rgba(255,255,255,0.7); display: flex; flex-direction: column; align-items: center; justify-content: center; z-index: 5;">
                    <van-loading size="20px" vertical>上传中...</van-loading>
                  </div>

                  <img
                    v-if="editSpaceForm.spaceCover"
                    :src="editSpaceForm.spaceCover"
                    alt="封面图"
                    style="width: 100%; height: 100%; object-fit: cover;"
                  >
                  <div v-else-if="!isCoverUploading" style="text-align: center; color: #999;">
                    <i class="fas fa-cloud-upload upload-preview-icon"></i>
                    <div>点击上传</div>
                  </div>
                </div>
              </div>
            </div>

            <div style="display: flex; justify-content: flex-end; gap: 12px; margin-top: 20px;">
              <button
                @click="cancelEditSpace"
                class="btn-cancel"
                style="padding: 8px 16px; border: 1px solid #d9d9d9; border-radius: 6px; background: #fff; cursor: pointer;"
              >
                取消
              </button>
              <button
                @click="confirmEditSpace"
                class="btn-confirm"
                style="padding: 8px 16px; background: #1677ff; color: #fff; border: none; border-radius: 6px; cursor: pointer;"
              >
                确认
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 隐藏的文件输入框 -->
      <input
        ref="fileInputRef"
        type="file"
        accept="image/*"
        @change="handleFileChange"
        style="display: none;"
      />

      <!-- 批量编辑模态框 -->
      <BatchEditPictureModal
        ref="batchEditPictureModalRef"
        :spaceId="id"
        :pictureList="dataList"
        :onSuccess="onBatchEditPictureSuccess"
      />

      <!-- 退出空间确认对话框 -->
      <div v-if="showQuitConfirmDialog" class="modal-overlay" @click="cancelQuit">
        <div class="modal-content" @click.stop style="max-width: 400px;">
          <div class="modal-header">
            <h3 class="space-name">确认退出</h3>
            <button class="close-btn" @click="cancelQuit">
              <i class="fas fa-times close-icon"></i>
            </button>
          </div>
          <div class="space-detail-content" style="padding: 24px; text-align: center;">
            <p style="margin-bottom: 24px; color: #666;">您确定要退出该空间吗？退出后将无法访问空间内容。</p>
            <div style="display: flex; justify-content: flex-end; gap: 12px;">
              <button
                @click="cancelQuit"
                class="btn-cancel"
                style="padding: 8px 16px; border: 1px solid #d9d9d9; border-radius: 6px; background: #fff; cursor: pointer;">
                取消
              </button>
              <button
                @click="confirmQuit"
                class="btn-confirm"
                style="padding: 8px 16px; background: #ff4d4f; color: #fff; border: none; border-radius: 6px; cursor: pointer;">
                确认退出
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 申请加入空间对话框 -->
      <div v-if="showJoinSpaceDialog" class="modal-overlay" @click="cancelJoin">
        <div class="modal-content" @click.stop style="max-width: 400px;">
          <div class="modal-header">
            <h3 class="space-name">申请加入空间</h3>
            <button class="close-btn" @click="cancelJoin">
              <i class="fas fa-times close-icon"></i>
            </button>
          </div>
          <div class="space-detail-content" style="padding: 24px; text-align: center;">
            <p style="margin-bottom: 24px; color: #666;">您没有权限查看此空间，是否申请加入？</p>
            <div style="display: flex; justify-content: flex-end; gap: 12px;">
              <button
                @click="cancelJoin"
                class="btn-cancel"
                style="padding: 8px 16px; border: 1px solid #d9d9d9; border-radius: 6px; background: #fff; cursor: pointer;">
                取消
              </button>
              <button
                @click="joinSpace"
                class="btn-confirm"
                style="padding: 8px 16px; background: #1677ff; color: #fff; border: none; border-radius: 6px; cursor: pointer;">
                申请加入
              </button>
            </div>
          </div>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup lang="ts">
import { use } from 'echarts/core';
import { CanvasRenderer } from 'echarts/renderers';
import { PieChart } from 'echarts/charts';
import { TitleComponent, TooltipComponent, LegendComponent } from 'echarts/components';

use([CanvasRenderer, PieChart, TitleComponent, TooltipComponent, LegendComponent]);
import { h, onMounted, onUnmounted, reactive, computed, watch, ref, createVNode } from 'vue'
import { getSpaceVoByIdUsingGet } from '@/api/spaceController.ts'
import { message, Modal, Badge } from 'ant-design-vue'
import {
  listPictureVoByPageUsingPost,
  searchPictureByColorUsingPost,
} from '@/api/pictureController.ts'
import { formatSize } from '@/utils'
import VChart from 'vue-echarts'
import * as echarts from 'echarts'
import PictureList from '@/components/PictureList.vue'
import { EditOutlined, PlusOutlined, BarChartOutlined, TeamOutlined, LeftOutlined, RightOutlined, MessageOutlined, InfoCircleOutlined } from '@ant-design/icons-vue'
import { getDeviceType } from '@/utils/device.ts'
import { DEVICE_TYPE_ENUM } from '@/constants/device.ts'
import { ColorPicker } from 'vue3-colorpicker'
import 'vue3-colorpicker/style.css'
import PictureSearchForm from '@/components/PictureSearchForm.vue'
import BatchEditPictureModal from '@/components/BatchEditPictureModal.vue'
import MobilePictureList from '@/components/MobilePictureList.vue'
import CustomPcCarousel from '@/components/CustomPcCarousel.vue'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { listSpaceVoByPageUsingPost } from '@/api/spaceController'
import { useRouter } from 'vue-router'
import { SPACE_PERMISSION_ENUM, SPACE_TYPE_ENUM } from '@/constants/space.ts'
import { prevRoute } from '@/router'
import {
  Pagination as VanPagination,
  Field as VanField,
  Button as VanButton,
  DropdownMenu as VanDropdownMenu,
  DropdownItem as VanDropdownItem,
  Loading as VanLoading,
  Dialog as VanDialog,
  PullRefresh as VanPullRefresh
} from 'vant'
import 'vant/es/pagination/style'
import 'vant/es/field/style'
import 'vant/es/button/style'
import 'vant/es/dropdown-menu/style'
import 'vant/es/dropdown-item/style'
import 'vant/es/loading/style'
import 'vant/es/dialog/style'
import 'vant/es/pull-refresh/style'

import MobileGridPictureList from '@/components/GridPictureList.vue'
import GridPictureList from '@/components/GridPictureList.vue'
import { quitSpaceUsingPost, checkPendingApplicationUsingPost, joinSpaceUsingPost } from '@/api/spaceUserController'
import { LogoutOutlined } from '@ant-design/icons-vue'
import { ExclamationCircleOutlined } from '@ant-design/icons-vue'
import { listSpaceUserUsingPost } from '@/api/spaceUserController'
import { editSpaceUsingPost } from '@/api/spaceController'
import { uploadPostImageUsingPost } from '@/api/pictureController'
import { InboxOutlined } from '@ant-design/icons-vue'
import { listActivityBySpaceIdUsingPost } from '@/api/activityController'

// 定义API类型
declare namespace API {
  interface SpaceVO {
    id?: string | number
    spaceName?: string
    spaceDesc?: string
    spaceCover?: string
    userId?: string | number
    user?: {
      userName?: string
    }
    spaceType?: number
    createTime?: string | number | Date
    totalSize?: number
    maxSize?: number
    totalCount?: number
    maxCount?: number
    permissionList?: string[]
  }

  interface PictureQueryRequest {
    current?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    orderBy?: string[]
    spaceId?: string | number
    [key: string]: any
  }

  interface PictureVO {
    id?: string | number
    [key: string]: any
  }

  interface SpaceEditRequest {
    id?: string | number
    spaceName?: string
    spaceDesc?: string
    spaceCover?: string
  }
}

interface Props {
  id: string | number,
}

const loginUserStore = useLoginUserStore()
const device = ref<string>('')
const moreModalVisible = ref(false)
const isStatusLoading = ref(true)
const toggleMoreModal = () => {
  moreModalVisible.value = !moreModalVisible.value
  document.body.style.overflow = moreModalVisible.value ? 'hidden' : ''
}

onMounted(async () => {
  device.value = await getDeviceType()

  // 首先获取空间详情
  await fetchSpaceDetail()

  // 私有空间没有活动，只在团队空间时获取活动
  if (userSpaceStatus.value.isMember && space.value.spaceType !== SPACE_TYPE_ENUM.PRIVATE) {
    await fetchActivities()
  }

  if (device.value !== DEVICE_TYPE_ENUM.PC) {
    window.addEventListener('scroll', checkScrollBottom)
  }
})



onUnmounted(() => {
  if (device.value !== DEVICE_TYPE_ENUM.PC) {
    window.removeEventListener('scroll', checkScrollBottom)
  }
  document.body.style.overflow = ''
})

const router = useRouter()
const props = defineProps<Props>()
const space = ref<API.SpaceVO>({})

// 活动相关
const activities = ref([])

const handleNavigate = (path: string) => {
  toggleMoreModal()
  router.push(path)
}

// 获取空间活动列表
const fetchActivities = async () => {
  try {
    const res = await listActivityBySpaceIdUsingPost({
      spaceId: props.id,
      current: 1,
      pageSize: 10,
    })

    if (res.data.code === 0) {
      // 只显示审核通过且未过期的活动
      const filteredActivities = (res.data.data?.records || []).filter(activity =>
        activity.status === 1 && activity.isExpired === 0
      )
      // 为CustomPcCarousel组件格式化数据
      const formattedActivities = filteredActivities.map(activity => ({
        id: activity.id,
        coverUrl: activity.coverUrl,
        title: activity.title,
        isExpired: activity.isExpired,
        expireTime: activity.expireTime
      }))
      activities.value = formattedActivities
    } else {
      console.error('获取活动列表失败:', res.data.message)
    }
  } catch (error) {
    console.error('获取活动列表失败:', error)
  }
}

// 处理活动点击
const handleActivityClick = (activityId: string | number) => {
  router.push(`/activity/detail/${activityId}`)
}



// 格式化日期
const formatDate = (dateString: string) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-CN')
}

const handleUploadClick = () => {
  router.push(`/add_picture?spaceId=${props.id}`)
}

function createPermissionChecker(permission: string) {
  return computed(() => {
    return (space.value.permissionList ?? []).includes(permission)
  })
}

const showUserButton = computed(() => {
  console.log('prevRoute.name', prevRoute)
  return prevRoute.name !== '空间成员管理'
})

const canManageSpaceUser = createPermissionChecker(SPACE_PERMISSION_ENUM.SPACE_USER_MANAGE)
const canUploadPicture = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_UPLOAD)
const canEditPicture = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_EDIT)
const canDeletePicture = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_DELETE)

const canQuitSpace = computed(() => {
  return space.value.spaceType === SPACE_TYPE_ENUM.TEAM &&
    space.value.userId !== loginUserStore.loginUser.id
})

const showEditSpaceModal = ref(false)

const editSpaceForm = reactive<API.SpaceEditRequest>({
  id: props.id,
  spaceName: '',
  spaceDesc: '',
  spaceCover: ''
})

const isCoverUploading = ref(false)

const fileInputRef = ref<HTMLInputElement | null>(null)

const openEditSpaceModal = () => {
  editSpaceForm.id = space.value.id
  editSpaceForm.spaceName = space.value.spaceName || ''
  editSpaceForm.spaceDesc = space.value.spaceDesc || ''
  editSpaceForm.spaceCover = space.value.spaceCover || ''
  showEditSpaceModal.value = true
  toggleMoreModal()
}

const uploadCoverImage = async (file: File) => {
  isCoverUploading.value = true
  try {
    const res = await uploadPostImageUsingPost({}, {}, file)
    if (res.data.code === 0) {
      editSpaceForm.spaceCover = res.data.data.url
      message.success('封面图上传成功')
    } else {
      message.error('封面图上传失败：' + res.data.message)
    }
  } catch (error) {
    console.error('上传封面图失败:', error)
    message.error('封面图上传失败')
  } finally {
    isCoverUploading.value = false
  }
}

const handleFileChange = async (e: Event) => {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  if (!file.type.startsWith('image/')) {
    message.error('请选择图片文件')
    return
  }

  // 修复：添加大括号包裹await代码
  await uploadCoverImage(file)
  // 清空输入框，以便选择同一张图片
  target.value = ''
}

const triggerFileSelect = () => {
  fileInputRef.value?.click()
}

const confirmEditSpace = async () => {
  try {
    const params: API.SpaceEditRequest = {
      id: editSpaceForm.id,
      spaceName: editSpaceForm.spaceName,
      spaceDesc: editSpaceForm.spaceDesc,
      spaceCover: editSpaceForm.spaceCover
    }

    const res = await editSpaceUsingPost(params)
    if (res.data.code === 0) {
      message.success('空间更新成功')
      showEditSpaceModal.value = false
      await fetchSpaceDetail()
    } else {
      message.error('空间更新失败：' + res.data.message)
    }
  } catch (error) {
    console.error('更新空间失败:', error)
    message.error('空间更新失败')
  }
}

const cancelEditSpace = () => {
  showEditSpaceModal.value = false
}

const handleQuitSpace = async () => {
  try {
    if (device.value === DEVICE_TYPE_ENUM.PC) {
      Modal.confirm({
        title: '确认退出',
        icon: createVNode(ExclamationCircleOutlined),
        content: '您确定要退出该空间吗？退出后将无法访问空间内容。',
        okText: '确认退出',
        cancelText: '取消',
        okButtonProps: { danger: true },
        async onOk() {
          try {
            const res = await quitSpaceUsingPost({
              id: props.id
            })

            if (res.data.code === 0) {
              message.success('退出成功')
              await router.push('/')
            } else {
              message.error('退出失败：' + res.data.message)
            }
          } catch (error: any) {
            message.error('退出失败：' + error.message)
          }
        }
      })
    } else {
      // 使用原生div实现移动端确认对话框
      showQuitConfirmDialog.value = true
    }
  } catch (error: any) {
    message.error('操作失败：' + error.message)
  }
}

// 退出确认对话框相关变量
const showQuitConfirmDialog = ref(false)

// 处理退出确认
const confirmQuit = async () => {
  try {
    const res = await quitSpaceUsingPost({
      id: props.id
    })

    if (res.data.code === 0) {
      message.success('退出成功')
      showQuitConfirmDialog.value = false
      await router.push('/')
    } else {
      message.error('退出失败：' + res.data.message)
    }
  } catch (error: any) {
    message.error('退出失败：' + error.message)
  }
}

// 取消退出
const cancelQuit = () => {
  showQuitConfirmDialog.value = false
}

const pendingApplicationsCount = ref<number>(0)

const fetchPendingApplications = async () => {
  try {
    const res = await listSpaceUserUsingPost({
      spaceId: props.id.toString(),
      status: 0
    })
    if (res.data?.code === 0) {
      pendingApplicationsCount.value = res.data.data?.length || 0
    }
  } catch (error) {
    console.error('获取待审核申请数量失败:', error)
  }
}

const checkPendingApplication = async () => {
  try {
    // 确保用户已登录
    if (!loginUserStore.loginUser?.id) {
      message.error('请先登录')
      router.push('/user/login')
      return
    }

    const res = await checkPendingApplicationUsingPost({
      userId: loginUserStore.loginUser.id,
      spaceId: props.id
    })

    if (res.data.code === 0) {
      const pendingApplication = res.data.data
      if (pendingApplication) {
        // 用户已有待审核的申请
        message.info('您已提交申请，请等待审核')
        return
      } else {
        // 用户没有待审核的申请，显示申请加入对话框
        showJoinSpaceDialog.value = true
      }
    } else {
      message.error('检查申请状态失败：' + res.data.message)
    }
  } catch (error) {
    console.error('检查申请状态失败:', error)
    message.error('检查申请状态失败')
  }
}

// 申请加入空间对话框相关变量
const showJoinSpaceDialog = ref(false)

// 申请加入空间
const joinSpace = async () => {
  try {
    const res = await joinSpaceUsingPost({
      spaceId: props.id
    })

    if (res.data.code === 0) {
      message.success('申请提交成功，请等待审核')
      showJoinSpaceDialog.value = false
      // 重新检查权限状态
      await updateUserSpaceStatus()
    } else {
      message.error('申请提交失败：' + res.data.message)
    }
  } catch (error) {
    console.error('申请加入空间失败:', error)
    message.error('申请加入空间失败')
  }
}

// 申请加入空间（从更多弹窗触发）
const applyToJoinSpace = async () => {
  try {
    // 确保用户已登录
    if (!loginUserStore.loginUser?.id) {
      message.error('请先登录')
      router.push('/user/login')
      return
    }

    // 检查用户是否已经有申请
    await updateUserSpaceStatus()

    if (userSpaceStatus.value.hasPending) {
      // 如果有待审核申请，提示用户
      message.info('您已提交申请，请等待审核')
      return
    } else if (userSpaceStatus.value.isMember) {
      // 如果已经是成员，直接返回
      message.info('您已经是空间成员')
      return
    } else {
      // 显示申请加入对话框
      showJoinSpaceDialog.value = true
    }
  } catch (error) {
    console.error('申请加入空间失败:', error)
    message.error('申请加入空间失败')
  }
}

// 用户在当前空间的权限状态
const userSpaceStatus = ref({
  isMember: false,      // 是否是空间成员
  hasPending: false,  // 是否有待审核申请
  application: null   // 申请信息
})

// 更新用户空间权限状态
const updateUserSpaceStatus = async () => {
  if (!loginUserStore.loginUser?.id) {
    // 用户未登录，设置为非成员状态
    userSpaceStatus.value = {
      isMember: false,
      hasPending: false,
      application: null
    }
    return
  }

  try {
    // 对于私有空间，如果当前用户是空间创建者，则自动视为成员
    if (space.value.spaceType === SPACE_TYPE_ENUM.PRIVATE &&
      space.value.userId === loginUserStore.loginUser.id) {
      userSpaceStatus.value = {
        isMember: true,
        hasPending: false,
        application: null
      }
      return
    }

    const res = await checkPendingApplicationUsingPost({
      userId: loginUserStore.loginUser.id,
      spaceId: props.id
    })

    if (res.data.code === 0) {
      const application = res.data.data
      if (application) {
        // 如果status为1，表示用户已经是空间成员
        // 如果status为0，表示有待审核的申请
        userSpaceStatus.value = {
          isMember: application.status === 1,
          hasPending: application.status === 0,
          application: application
        }
      } else {
        // 没有申请记录，用户不是空间成员
        userSpaceStatus.value = {
          isMember: false,
          hasPending: false,
          application: null
        }
      }
    } else {
      console.error('检查用户空间权限失败:', res.data.message)
      // 出错时默认设置为非成员状态
      userSpaceStatus.value = {
        isMember: false,
        hasPending: false,
        application: null
      }
    }
  } catch (error) {
    console.error('检查用户空间权限失败:', error)
    // 出错时默认设置为非成员状态
    userSpaceStatus.value = {
      isMember: false,
      hasPending: false,
      application: null
    }
  }
}

// 取消申请
const cancelJoin = () => {
  showJoinSpaceDialog.value = false
}

const fetchSpaceDetail = async () => {
  loading.value = true
  try {
    if (!props.id) {
      const res = await listSpaceVoByPageUsingPost({
        userId: loginUserStore.loginUser.id,
        current: 1,
        pageSize: 1,
        spaceType: SPACE_TYPE_ENUM.PRIVATE,
      })

      if (res.data.code === 0) {
        if (res.data.data?.records?.length > 0) {
          const firstSpace = res.data.data.records[0]
          router.replace(`/space/${firstSpace.id}`)
          space.value = firstSpace
        } else {
          router.replace('/add_space')
          return
        }
      } else {
        return
      }
    } else {
      const res = await getSpaceVoByIdUsingGet({
        id: props.id,
      })

      if (res.data.code === 0 && res.data.data) {
        space.value = res.data.data

        // 更新用户权限状态 - 这需要在获取空间信息后立即执行
        await updateUserSpaceStatus()

        if (canManageSpaceUser.value) {
          await fetchPendingApplications()
        }

        // 根据权限状态决定是否获取图片数据
        if (userSpaceStatus.value.isMember) {
          // 如果是空间成员，获取图片数据
          await fetchData()
        }
      } else if (res.data.code === 40101) {
        // 没有权限查看空间，检查是否已申请加入
        await checkPendingApplication()
        // 如果对话框没有显示，直接返回，避免后续操作
        return
      }
    }
  } catch (e: any) {
    console.error('获取空间信息失败:', e)
  } finally {
    loading.value = false
    isStatusLoading.value = false
  }
}

watch(canManageSpaceUser, (newValue) => {
  if (newValue) {
    fetchPendingApplications()
  }
})

const dataList = ref<API.PictureVO[]>([])
const total = ref(0)
const loading = ref(true)

const searchParams = reactive<API.PictureQueryRequest>({
  current: 1,
  pageSize: 12,
  sortField: 'createTime',
  sortOrder: 'descend',
  orderBy: ['createTime DESC', 'id DESC'],
})

const loadingMore = ref(false)
const hasMore = ref(true)

const checkScrollBottom = () => {
  if (device.value === DEVICE_TYPE_ENUM.PC) return

  const scrollHeight = document.documentElement.scrollHeight
  const scrollTop = document.documentElement.scrollTop || document.body.scrollTop
  const clientHeight = document.documentElement.clientHeight

  if (scrollHeight - scrollTop - clientHeight < 100 && !loadingMore.value && hasMore.value && !loading.value) {
    loadMore()
  }
}

const loadMore = async () => {
  if (loadingMore.value || !hasMore.value || loading.value) return

  loadingMore.value = true
  try {
    // 检查用户是否有查看空间的权限
    if (!userSpaceStatus.value.isMember) {
      // 用户没有权限，检查是否已申请加入
      await updateUserSpaceStatus()
      if (!userSpaceStatus.value.isMember && !userSpaceStatus.value.hasPending) {
        // 如果不是空间成员且没有待审核申请，则检查申请状态
        await checkPendingApplication()
      }
      // 检查后再次确认权限，如果仍然没有权限则返回
      if (!userSpaceStatus.value.isMember) {
        return
      }
    }

    const nextPage = Math.floor(dataList.value.length / searchParams.pageSize) + 1
    const requestParams = {
      ...searchParams,
      current: nextPage,
      spaceId: props.id,
      orderBy: ['createTime DESC', 'id DESC'],
    }

    const res = await listPictureVoByPageUsingPost(requestParams)

    if (res.data.code === 0) {
      const newRecords = res.data.data?.records ?? []
      if (newRecords.length > 0) {
        dataList.value = [...dataList.value, ...newRecords]
      }
      hasMore.value = newRecords.length === searchParams.pageSize
    } else {
      // 检查用户是否有查看空间的权限
      if (!userSpaceStatus.value.isMember) {
        // 用户没有权限，检查是否已申请加入
        await updateUserSpaceStatus()
        if (!userSpaceStatus.value.isMember && !userSpaceStatus.value.hasPending) {
          // 如果不是空间成员且没有待审核申请，则检查申请状态
          await checkPendingApplication()
        }
        // 检查后再次确认权限，如果仍然没有权限则返回
        if (!userSpaceStatus.value.isMember) {
          return
        }
        // 如果现在有权限了，重新获取数据
        await fetchData()
      }
    }
  } catch (error) {
    console.error('加载更多失败:', error)
  } finally {
    loadingMore.value = false
  }
}

const fetchData = async () => {
  loading.value = true
  try {
    if(!props.id){
      await router.push('/')
      return
    }

    // 检查用户是否有查看空间的权限
    if (!userSpaceStatus.value.isMember) {
      // 用户没有权限，检查是否已申请加入
      await updateUserSpaceStatus()
      if (!userSpaceStatus.value.isMember && !userSpaceStatus.value.hasPending) {
        // 如果不是空间成员且没有待审核申请，则检查申请状态
        await checkPendingApplication()
      }
      // 检查后再次确认权限，如果仍然没有权限则返回
      if (!userSpaceStatus.value.isMember) {
        return
      }
    }

    hasMore.value = true
    searchParams.current = 1

    const requestParams = {
      ...searchParams,
      spaceId: props.id,
      orderBy: ['createTime DESC', 'id DESC'],
    }

    const res = await listPictureVoByPageUsingPost(requestParams)

    if (res.data.code === 0) {
      dataList.value = res.data.data?.records ?? []
      hasMore.value = (res.data.data?.records?.length ?? 0) === searchParams.pageSize
    } else {
      // 检查用户是否有查看空间的权限
      if (!userSpaceStatus.value.isMember) {
        // 用户没有权限，检查是否已申请加入
        await updateUserSpaceStatus()
        if (!userSpaceStatus.value.isMember && !userSpaceStatus.value.hasPending) {
          // 如果不是空间成员且没有待审核申请，则检查申请状态
          await checkPendingApplication()
        }
        // 检查后再次确认权限，如果仍然没有权限则返回
        if (!userSpaceStatus.value.isMember) {
          return
        }
        // 如果现在有权限了，重新获取数据
        await fetchData()
      }
    }
  } catch (error) {
    console.error('获取图片列表失败:', error)
  } finally {
    loading.value = false
  }
}

const onColorChange = async (color: string) => {
  loading.value = true

  // 检查用户是否有查看空间的权限
  if (!userSpaceStatus.value.isMember) {
    // 用户没有权限，检查是否已申请加入
    await updateUserSpaceStatus()
    if (!userSpaceStatus.value.isMember && !userSpaceStatus.value.hasPending) {
      // 如果不是空间成员且没有待审核申请，则检查申请状态
      await checkPendingApplication()
    }
    // 检查后再次确认权限，如果仍然没有权限则返回
    if (!userSpaceStatus.value.isMember) {
      loading.value = false
      return
    }
  }

  const res = await searchPictureByColorUsingPost({
    picColor: color,
    spaceId: props.id,
  })
  if (res.data.code === 0 && res.data.data) {
    dataList.value = res.data.data ?? []
    total.value = dataList.value.length
  } else {
    // 检查用户是否有查看空间的权限
    if (!userSpaceStatus.value.isMember) {
      // 用户没有权限，检查是否已申请加入
      await updateUserSpaceStatus()
      if (!userSpaceStatus.value.isMember && !userSpaceStatus.value.hasPending) {
        // 如果不是空间成员且没有待审核申请，则检查申请状态
        await checkPendingApplication()
      }
      // 检查后再次确认权限，如果仍然没有权限则返回
      if (!userSpaceStatus.value.isMember) {
        loading.value = false
        return
      }
      // 如果现在有权限了，重新获取数据
      await fetchData()
    }
  }
  loading.value = false
}

const onSearch = (newSearchParams: API.PictureQueryRequest) => {
  if (!newSearchParams) return

  console.log('新的搜索参数:', newSearchParams)
  Object.assign(searchParams, {
    ...newSearchParams,
    current: 1,
    pageSize: searchParams.pageSize,
    sortField: 'createTime',
    sortOrder: 'descend'
  })
  console.log('更新后的搜索参数:', searchParams)
  fetchData()
}

const batchEditPictureModalRef = ref()

const onBatchEditPictureSuccess = () => {
  fetchData()
}

const doBatchEdit = () => {
  if (batchEditPictureModalRef.value) {
    batchEditPictureModalRef.value.openModal()
  }
}

const isRefreshing = ref(false)

const onRefresh = async () => {
  if (isRefreshing.value) return
  isRefreshing.value = true

  try {
    searchFormRef.value?.handleRefresh()

    searchParams.current = 1
    dataList.value = []
    hasMore.value = true
    await fetchData()
  } catch (error) {
    console.error('刷新失败:', error)
  } finally {
    loading.value = false
    isRefreshing.value = false
  }
}

watch(
  () => props.id,
  async (newSpaceId) => {
    // 更新用户在空间中的权限状态
    await updateUserSpaceStatus()

    // 根据权限状态决定获取哪些数据
    if (userSpaceStatus.value.isMember) {
      // 如果是空间成员，获取空间详情和图片数据
      await fetchSpaceDetail()
      await fetchData()
    } else {
      // 如果不是空间成员，只获取空间基本信息
      await fetchSpaceDetail()
      // 不获取图片数据，因为用户没有权限
    }
  },
)

const totalPages = computed(() => {
  const pages = Math.ceil(total.value / searchParams.pageSize)
  return isNaN(pages) ? 1 : pages
})

const jumpPage = ref('')

const handleJumpPage = () => {
  const page = parseInt(jumpPage.value)
  if (isNaN(page) || page < 1 || page > Math.ceil(total.value / searchParams.pageSize)) {
    message.error(`请输入1-${Math.ceil(total.value / searchParams.pageSize)}之间的页码`)
    return
  }
  searchParams.current = page
  fetchData()
  jumpPage.value = ''
}

const pageSizeOptions = [
  { text: '每页 6 条', value: 6 },
  { text: '每页 12 条', value: 12 },
  { text: '每页 18 条', value: 18 },
  { text: '每页 24 条', value: 24 },
  { text: '每页 30 条', value: 30 },
]

const onPageSizeChange = (value: number) => {
  searchParams.current = 1
  searchParams.pageSize = value
  fetchData()
}

const showPageSizePopup = ref(false)

const searchFormRef = ref()

const spaceDetail = ref(null)

const formatTime = (time: string | number | Date) => {
  if (!time) return '未知'
  const date = new Date(time)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

const showSpaceDetail = () => {
  console.log('空间详情已直接在弹窗中展示')
}

// 计算空间容量百分比
const capacityPercentage = computed(() => {
  if (!space.value.totalSize || !space.value.maxSize) return 0
  return Math.round((space.value.totalSize / space.value.maxSize) * 100)
})

// 环形图相关计算
const radius = 45 // 圆环半径
const circumference = 2 * Math.PI * radius

const strokeDashoffset = computed(() => {
  if (!space.value.totalSize || !space.value.maxSize) return circumference
  const progress = (space.value.totalSize / space.value.maxSize)
  return circumference * (1 - progress)
})

// ECharts 环形图选项
const gaugeOption = computed(() => {
  const percentage = capacityPercentage.value
  const remaining = 100 - percentage

  return {
    series: [
      {
        type: 'pie',
        radius: ['70%', '90%'], // 创建环形图效果
        center: ['50%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 4,
          borderColor: '#fff',
          borderWidth: 1
        },
        label: {
          show: true,
          position: 'center',
          formatter: `{d|${percentage}%}`,
          rich: {
            d: {
              fontSize: 16,
              fontWeight: 'bold',
              color: '#333',
              lineHeight: 24
            }
          }
        },
        emphasis: {
          label: {
            show: true
          }
        },
        labelLine: {
          show: false
        },
        data: [
          {
            value: percentage,
            name: '已使用',
            itemStyle: {
              color: '#1677ff'
            }
          },
          {
            value: remaining,
            name: '剩余',
            itemStyle: {
              color: '#f0f0f0'
            }
          }
        ]
      }
    ]
  }
})

const displayedPages = computed(() => {
  const totalPages = Math.ceil(total.value / searchParams.pageSize);
  const current = searchParams.current;
  const pages = [];

  if (totalPages <= 7) {
    for (let i = 1; i <= totalPages; i++) {
      pages.push(i);
    }
  } else {
    pages.push(1);

    if (current <= 4) {
      for (let i = 2; i <= 5; i++) {
        pages.push(i);
      }
      pages.push('...');
      pages.push(totalPages);
    } else if (current >= totalPages - 3) {
      pages.push('...');
      for (let i = totalPages - 4; i <= totalPages; i++) {
        pages.push(i);
      }
    } else {
      pages.push('...');
      for (let i = current - 1; i <= current + 1; i++) {
        pages.push(i);
      }
      pages.push('...');
      pages.push(totalPages);
    }
  }

  return pages;
});

</script>

<style scoped>
#spaceDetailPage {
  margin: 8px 0;
  color: var(--text-primary);
  position: relative;
}

.search-spacing {
  margin-bottom: 16px;
}

/* 顶部搜索栏 */
.search-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-radius: 10px;
  height: 60px;
}

.search-form-container {
  flex: 1;
  margin-right: 12px;

}

.search-form-container :deep(.picture-search-form) {
  padding: 0;
}

/* 更多按钮 */
.more-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 80px;
  height: 36px;
  background: linear-gradient(135deg, #4096ff, #1677ff);
  border: none;
  border-radius: 6px;
  color: white;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(64, 150, 255, 0.3);
}

.more-btn:hover {
  opacity: 0.9;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(64, 150, 255, 0.4);
}

.more-btn svg {
  margin-right: 4px;
}

/* 内容区域 */
.content-wrap {
  padding: 0 4px;
  margin-bottom: 24px;
}

/* 上传按钮 */
.upload-btn {
  position: fixed;
  bottom: 180px;
  right: 32px;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: linear-gradient(135deg, #4096ff, #1677ff);
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(64, 150, 255, 0.4);
  transition: all 0.3s ease;
  z-index: 100;
}

.upload-btn:hover {
  transform: scale(1.05);
  box-shadow: 0 6px 20px rgba(64, 150, 255, 0.5);
}

.upload-btn:active {
  transform: scale(0.95);
}

/* 空间详情弹窗 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(4px);
}

.modal-content {
  width: 90%;
  max-width: 500px;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
  overflow: hidden;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
}

/* 弹窗头部 */
.modal-header {
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.space-name {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.close-btn {
  border: none;
  background: none;
  font-size: 24px;
  color: #999;
  cursor: pointer;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.close-btn:hover {
  background-color: #f5f5f5;
  color: #666;
}

/* 空间详情内容 */
.space-detail-content {
  padding: 20px;
  overflow-y: auto;
  flex: 1;
}

/* 空间基本信息 */
.space-basic-info {
  margin-bottom: 20px;
}

.creator-info,
.space-type-info,
.create-time {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.creator-info:last-child,
.space-type-info:last-child,
.create-time:last-child {
  border-bottom: none;
}

.label {
  font-size: 14px;
  color: #666;
}

.value {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

/* 空间容量信息 */
.capacity-info {
  padding: 16px;
  background: #f9f9f9;
  border-radius: 8px;
  margin-bottom: 20px;
}

.capacity-label {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}

.capacity-stats-row {
  display: flex;
  align-items: center;
  gap: 16px;
}

.capacity-ring-chart {
  flex-shrink: 0;
  width: 60px;
  height: 60px;
  position: relative;
}

.ring-chart {
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
}

.ring-bg {
  stroke: #f0f0f0;
}

.ring-progress {
  stroke: #1677ff;
  transition: stroke-dashoffset 0.5s ease;
  transform-origin: center;
  fill: transparent;
}

.ring-text {
  font-size: 14px;
  font-weight: 600;
  fill: #333;
}

.capacity-and-count {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.capacity-text {
  display: flex;
  align-items: baseline;
  gap: 4px;
  font-size: 14px;
  color: #333;
  margin-bottom: 4px;
}

.capacity-used {
  font-weight: 600;
  color: #1677ff;
}

.capacity-slash {
  color: #999;
}

.capacity-max {
  color: #666;
}

.count-info {
  display: flex;
  align-items: center;
  gap: 6px;
}

.count-number {
  font-size: 16px;
  font-weight: 600;
  color: #52c41a;
}

.count-label {
  font-size: 12px;
  color: #999;
}

/* 空间简介信息 */
.space-desc-info {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.space-desc-info:last-child {
  border-bottom: none;
}



/* 宫格操作按钮列表 */
.grid-operation-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  padding: 8px 0;
}

/* 当按钮总数为奇数时，最后一个按钮独占一行 */
.grid-operation-list .grid-op-btn:nth-last-child(1):nth-child(odd) {
  grid-column: 1 / -1; /* 独占整行 */
  width: 100%;
}

.grid-op-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding:  8px;
  border: 1px solid #f0f0f0;
  background: #fafafa;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  min-height: 66px;
  text-align: center;
}

.grid-op-btn:hover {
  background: #f5f5f5;
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0,0,0,0.1);
}

.grid-op-icon {
  font-size: 20px;
  margin-bottom: 8px;
  display: block;
}

/* 不同功能的图标颜色 */
.member-icon {
  color: #1890ff; /* 成员管理 - 蓝色 */
}

.chat-icon {
  color: #52c41a; /* 团队聊天 - 绿色 */
}

.analyze-icon {
  color: #722ed1; /* 空间分析 - 紫色 */
}

.edit-icon {
  color: #fa8c16; /* 批量编辑 - 橙色 */
}

.exit-icon {
  color: #ff4d4f; /* 退出空间 - 红色 */
}

.activity-icon {
  color: #13c2c2; /* 空间活动 - 青色 */
}

.grid-op-btn span {
  font-size: 13px;
  color: #333;
}

.full-width-grid-op-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 8px;
  width: 100%;
  height: 80px;
  border: 1px solid #f0f0f0;
  background: #fafafa;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  min-height: 80px;
  text-align: center;
  margin-top: 8px;
}

.full-width-grid-op-btn:hover {
  background: #f5f5f5;
  border-color: #d9d9d9;
}

.full-width-grid-op-btn i {
  font-size: 16px;
  margin-right: 0;
}

.full-width-grid-op-btn span {
  font-size: 14px;
  color: #333;
}

.edit-space-icon {
  color: #fa8c16; /* 橙色，与批量编辑保持一致 */
}

/* 编辑空间按钮独占一行 */
.grid-operation-list .edit-space-btn {
  grid-column: 1 / -1; /* 跨越所有列，独占一行 */
  width: 100%;
}

.single-grid-container {
  display: grid;
  grid-template-columns: 1fr;
  gap: 8px;
  margin-top: 8px;
}

/* 操作按钮列表 (旧样式) */
.operation-list {
  padding-top: 8px;
}

.op-btn {
  display: flex;
  align-items: center;
  width: 100%;
  padding: 14px 16px;
  border: none;
  background: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  margin-bottom: 8px;
}

.op-btn:hover {
  background: #f5f5f5;
}

.op-btn:last-child {
  margin-bottom: 0;
}

.op-icon {
  margin-right: 12px;
  flex-shrink: 0;
}

.op-btn span {
  font-size: 15px;
  color: #333;
  flex: 1;
  text-align: left;
}

.badge {
  display: inline-block;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  background: #ff4d4f;
  color: white;
  font-size: 12px;
  line-height: 18px;
  text-align: center;
  margin-left: 8px;
}

.danger-btn {
  color: #ff4d4f;
}

.danger-btn:hover {
  background: #fff2f0;
}

.search-form-container {
  position: relative;
  width: 100%;
  margin: 0 auto;
  max-width: 800px;
}

.search-form-container :deep(.picture-search-form) {
  width: 100%;
  max-width: 800px;
}

.search-form-container :deep(.ant-input) {
  border-radius: 8px 0 0 8px;
  border: 1px solid #e2e8f0;
  transition: all 0.3s ease;
}

.search-form-container :deep(.ant-input):hover {
  border-color: #ff8e53;
}

.search-form-container :deep(.ant-input):focus {
  border-color: #ff8e53;
  box-shadow: 0 0 0 2px rgba(255, 142, 83, 0.1);
}

.search-form-container :deep(.ant-input-group) {
  display: flex;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  border-radius: 8px;
  overflow: hidden;
  height: 36px;
}

.search-form-container :deep(.ant-btn) {
  background: linear-gradient(135deg, #ff8e53 0%, #ff6b6b 100%);
  border: none;
  transition: all 0.3s ease;
}

.search-form-container :deep(.ant-btn:hover) {
  opacity: 0.9;
  box-shadow: 0 2px 8px rgba(255, 142, 83, 0.3);
}

.content-spacing {
  margin-bottom: 16px;
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .search-spacing {
    margin-bottom: 8px;
  }

  .search-form-container {
    width: 100%;
    margin: 0;
    padding: 0 -18px 8px;
    max-width: none;
    left: 0;
    right: 0;
  }

  .search-form-container :deep(.search-container) {
    width: 100%;
    padding-right: 6px;
  }

  .search-form-container :deep(.ant-form) {
    width: 100%;
    margin: 0;
  }

  .search-form-container :deep(.ant-form-item) {
    width: 100%;
    margin: 0;
  }

  .search-form-container :deep(.ant-input-group) {
    width: 100%;
    display: flex !important;
    height: 36px;
  }

  .search-form-container :deep(.ant-input) {
    flex: 1;
    line-height: 36px !important;
  }

  .search-form-container :deep(.ant-btn) {
    padding: 0 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    min-width: 48px;
    flex-shrink: 0;
    height: 36px !important;
  }

  .search-form-container :deep(.ant-btn > span:not(.anticon)) {
    display: none;
  }

  .search-form-container :deep(.ant-btn .anticon) {
    font-size: 16px;
    margin: 0;
    display: flex !important;
  }

  .content-spacing {
    margin-bottom: 0;
  }

  /* 移动端适配 */
  .search-header {
    padding: 8px 12px;
    height: 52px;
  }

  .more-btn {
    width: 70px;
    height: 32px;
    font-size: 13px;
  }


  .upload-btn {
    width: 50px;
    height: 50px;
    bottom: 180px;
    right: 48px;
  }

  /* 编辑空间模态框移动端样式 */
  .modal-content {
    width: 95% !important;
  }

  .space-detail-content {
    padding: 16px !important;
  }

  .form-item {
    margin-bottom: 16px !important;
  }

  .form-input {
    padding: 8px !important;
  }

  .image-preview {
    width: 100px !important;
    height: 70px !important;
  }

  .btn-cancel,
  .btn-confirm {
    padding: 6px 12px !important;
    font-size: 14px;
  }

  .modal-content {
    width: 95%;
    max-height: 95vh;
  }

  .modal-header {
    padding: 12px 16px;
  }

  .space-name {
    font-size: 16px;
  }

  .space-detail-content {
    padding: 16px;
  }

  .creator-info,
  .space-type-info,
  .create-time {
    padding: 6px 0;
  }

  .label,
  .value {
    font-size: 13px;
  }

  .capacity-info {
    padding: 12px;
  }

  .capacity-label,
  .capacity-text {
    font-size: 13px;
  }

  .stats-info {
    padding: 12px;
  }

  .stat-value {
    font-size: 18px;
  }

  .stat-label {
    font-size: 12px;
  }

  .op-btn {
    padding: 12px 14px;
  }

  .op-btn span {
    font-size: 14px;
  }
}

#spaceDetailPage :deep(.vc-color-wrap) {
  width: 30px;
  height: 30px;
  border-radius: 5px;
  margin-top: -4px;
  margin-right: 8px;
}

/* 加载更多样式 */
.load-more {
  text-align: center;
  padding: 16px 0;
}

.no-more {
  color: #94a3b8;
  font-size: 14px;
}

/* 添加徽标样式 */
:deep(.ant-badge-count) {
  background: #ff4d4f;
  box-shadow: 0 0 0 1px #fff;
}

:deep(.ant-badge) {
  line-height: 1;
}

/* 主容器样式 */
.container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0;
}

/* 轮播图容器样式 */
.carousel-wrapper {
  margin: 0;
  padding: 4px;
}

.upload-icon{
  color: #fff0f0;
}

.carousel-wrapper :deep(.luxury-carousel) {
  height: 420px;
  max-height: 420px;
  min-height: 360px;
  aspect-ratio: 16/9;
}

/* 移动端轮播图样式 */
@media (max-width: 768px) {
  .carousel-wrapper :deep(.luxury-carousel) {
    height: 250px !important;
    max-height: 250px !important;
    min-height: 250px;
  }
}

/* 顶部轮播图区域 */
.top-carousel-section {
  margin-bottom: 24px;
  padding: 0 16px;

  @media (max-width: 768px) {
    margin-bottom: 16px;
    padding: 0;
  }
}

.top-carousel-section .carousel-wrapper {
  width: 100%;
  padding: 0;
}


/* 私有空间信息样式 */
.private-space-info {
  margin-bottom: 16px;
  padding: 0 16px;
}

.private-space-message {
  padding: 20px;
  text-align: center;
  color: #666;
  font-size: 16px;
  background: #f9f9f9;
  border-radius: 8px;
  border: 1px solid #e8e8e8;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .pc-content {
    flex-direction: column;
  }

  .space-info-box {
    width: 100%;
    margin-top: 16px;
  }
}

/* 申请按钮样式 */
.apply-section {
  margin-top: 16px;
}

.apply-section .op-btn {
  width: 100%;
  padding: 14px 16px;
  border: none;
  background: linear-gradient(135deg, #4096ff, #1677ff);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  color: white;
}

.apply-section .op-btn:hover {
  opacity: 0.9;
  transform: translateY(-1px);
}

.apply-section .op-icon {
  margin-right: 12px;
  flex-shrink: 0;
}

.apply-section .op-btn span {
  font-size: 15px;
  color: white;
  flex: 1;
  text-align: left;
}

/* 主界面申请按钮样式 */
.apply-section-main {
  padding: 0 16px;
  margin-bottom: 16px;
  display: flex;
  justify-content: center;
}

.apply-btn-main {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 12px 24px;
  background: linear-gradient(135deg, #4096ff, #1677ff);
  border: none;
  border-radius: 6px;
  color: white;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(64, 150, 255, 0.3);
  min-width: 120px;
}

.apply-btn-main:hover {
  opacity: 0.9;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(64, 150, 255, 0.4);
}

.apply-icon-main {
  margin-right: 8px;
}
</style>
