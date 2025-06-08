<template>
  <div id="spaceDetailPage">
    <!-- 空间信息 -->

    <!-- 背景动画 -->
    <div class="background-animation" v-if="device !== DEVICE_TYPE_ENUM.PC">
      <div class="circle circle-1"></div>
      <div class="circle circle-2"></div>
      <div class="circle circle-3"></div>
    </div>

    <a-flex
      justify="space-between"
      style="
        display: flex;
        height: 40px;
        align-items: center;
        width: 100%;
        margin-top: -20px;
        margin-bottom: -4px;
        background-color: #fdfaf9;
        border-radius: 10px;
      "
    >
      <a-tooltip
        v-if="device !== DEVICE_TYPE_ENUM.PC"
        :title="`${formatSize(space.totalSize)} / ${formatSize(space.maxSize)}`"
      >
        <a-progress
          style="margin-left: 10px"
          type="circle"
          :size="device !== DEVICE_TYPE_ENUM.PC ? 30 : 42"
          :percent="((space.totalSize * 100) / space.maxSize).toFixed(1)"
        />
      </a-tooltip>
      <div v-if="device === DEVICE_TYPE_ENUM.PC ">
        <a-button
          type="link"
          size="large"
          ghost
          @click="showSpaceDetail"
          class="space-name-btn"
        >
          <span>{{ space.spaceName }}</span>
          <InfoCircleOutlined class="info-icon" />
        </a-button>
      </div>
      <div>
        <a-tooltip
          v-if="device === DEVICE_TYPE_ENUM.PC"
          :style="device === DEVICE_TYPE_ENUM.PC ? { marginLeft: '10px' } : { marginLeft: '0px' }"
          :title="`${formatSize(space.totalSize)} / ${formatSize(space.maxSize)}`"
        >
          <a-progress
            style="margin-right: 10px"
            type="circle"
            :size="device !== DEVICE_TYPE_ENUM.PC ? 28 : 36"
            :percent="((space.totalSize * 100) / space.maxSize).toFixed(1)"
          />
        </a-tooltip>
        <a-button
          v-if="device !== DEVICE_TYPE_ENUM.PC"
          :icon="h(InfoCircleOutlined)"
          style="margin-right: 10px; background-color: #ffb071"
          type="primary"
          @click="showSpaceDetail"
        />
        <a-button
          v-if="canUploadPicture"
          :icon="h(PlusOutlined)"
          style="margin-right: 10px; background-color: #ffb071"
          type="primary"
          :href="`/add_picture?spaceId=${id}`"
        >
          <span v-if="device === DEVICE_TYPE_ENUM.PC"> 添加图片 </span>
        </a-button>
        <a-button
          style="margin-right: 10px; background-color: #35cb6a"
          v-if="canManageSpaceUser && space.spaceType === SPACE_TYPE_ENUM.TEAM"
          ghost
          :icon="h(TeamOutlined)"
          :href="`/spaceUserManage/${id}`"
        >
          <span v-if="device === DEVICE_TYPE_ENUM.PC"> 成员管理 </span>
        </a-button>
<!--        <a-button-->
<!--          v-if="space.spaceType === SPACE_TYPE_ENUM.TEAM"-->
<!--          style="margin-right: 10px; background-color: #4096ff"-->
<!--          :icon="h(MessageOutlined)"-->
<!--          :href="`/space_chat/${id}`"-->
<!--        >-->
<!--          <span v-if="device === DEVICE_TYPE_ENUM.PC"> 团队聊天 </span>-->
<!--        </a-button>-->
        <a-button
          v-if="canManageSpaceUser"
          style="margin-right: 10px; background-color: #e787cf"
          type="primary"
          :icon="h(BarChartOutlined)"
          :href="`/space_analyze?spaceId=${id}`"
        >
          <span v-if="device === DEVICE_TYPE_ENUM.PC"> 空间分析 </span>
        </a-button>
        <a-button
          v-if="canEditPicture"
          :icon="h(EditOutlined)"
          style="margin-right: 10px; background-color: #c1edf5"
          @click="doBatchEdit"
        >
          <span v-if="device === DEVICE_TYPE_ENUM.PC"> 批量编辑 </span>
        </a-button>
        <span>
          <color-picker format="hex" @pureColorChange="onColorChange" />
        </span>
      </div>
    </a-flex>
    <div style="margin-bottom: 2px"></div>
    <!-- 搜索表单 -->
    <div class="search-form-container">
      <PictureSearchForm
        ref="searchFormRef"
        :search-params="searchParams"
        @search="onSearch"
      />
    </div>

    <div class="content-spacing"></div>

    <!-- 图片列表 -->
    <div>
      <van-pull-refresh v-model="loading" @refresh="onRefresh" :distance="80" :head-height="60">
        <!-- 内部组件 -->
        <GridPictureList
          :dataList="dataList"
          :loading="loading"
          :canEdit="canEditPicture"
          :canDelete="canDeletePicture"
          :showOp="true"
          :onReload="fetchData"
          :hasMore="hasMore"
          :onLoadMore="loadMore"
        />
      </van-pull-refresh>
    </div>
    <BatchEditPictureModal
      ref="batchEditPictureModalRef"
      :spaceId="id"
      :pictureList="dataList"
      :onSuccess="onBatchEditPictureSuccess"
    />
    <!-- 添加空间详情模态框 -->
    <SpaceDetailModal
      v-model="detailVisible"
      :space-detail="spaceDetail"
    />
  </div>
</template>

<script setup lang="ts">
import { h, onMounted, onUnmounted, reactive, computed, watch, ref } from 'vue'
import { getSpaceVoByIdUsingGet } from '@/api/spaceController.ts'
import { message } from 'ant-design-vue'
import {
  listPictureVoByPageUsingPost,
  searchPictureByColorUsingPost,
} from '@/api/pictureController.ts'
import { formatSize } from '@/utils'
import PictureList from '@/components/PictureList.vue'
import { EditOutlined, PlusOutlined, BarChartOutlined, TeamOutlined, LeftOutlined, RightOutlined, MessageOutlined, InfoCircleOutlined } from '@ant-design/icons-vue'
import { getDeviceType } from '@/utils/device.ts'
import { DEVICE_TYPE_ENUM } from '@/constants/device.ts'
import { ColorPicker } from 'vue3-colorpicker'
import 'vue3-colorpicker/style.css'
import PictureSearchForm from '@/components/PictureSearchForm.vue'
import BatchEditPictureModal from '@/components/BatchEditPictureModal.vue'
import MobilePictureList from '@/components/MobilePictureList.vue'
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
  Loading as VanLoading
} from 'vant'
import 'vant/es/pagination/style'
import 'vant/es/field/style'
import 'vant/es/button/style'
import 'vant/es/dropdown-menu/style'
import 'vant/es/dropdown-item/style'
import 'vant/es/loading/style'
import SpaceDetailModal from '@/components/SpaceDetailModal.vue'
import MobileGridPictureList from '@/components/GridPictureList.vue'
import GridPictureList from '@/components/GridPictureList.vue'

interface Props {
  id: string | number,
}
const loginUserStore = useLoginUserStore()
// 定义用于存储设备类型的响应式变量
const device = ref<string>('')
// 页面加载时获取设备类型并获取数据
onMounted(async () => {
  device.value = await getDeviceType()
  await fetchSpaceDetail()
})
const router = useRouter()
const props = defineProps<Props>()
const space = ref<API.SpaceVO>({})
// 通用权限检查函数
function createPermissionChecker(permission: string) {
  return computed(() => {
    return (space.value.permissionList ?? []).includes(permission)
  })
}

//是否显示成员管理按钮
const showUserButton = computed(() => {
  // 仅登录用户可分享
  console.log('prevRoute.name', prevRoute)
  return prevRoute.name !== '空间成员管理'
})
// 定义权限检查
const canManageSpaceUser = createPermissionChecker(SPACE_PERMISSION_ENUM.SPACE_USER_MANAGE)
const canUploadPicture = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_UPLOAD)
const canEditPicture = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_EDIT)
const canDeletePicture = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_DELETE)
// -------- 获取空间详情 --------
// 获取空间详情
const fetchSpaceDetail = async () => {
  loading.value = true
  try {
    // 如果没有传入空间id，则获取用户的第一个空间
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
          // 更新路由到第一个空间
          router.replace(`/space/${firstSpace.id}`)
          space.value = firstSpace
        } else {
          // 如果用户没有空间，跳转到创建空间页面
          // message.info('您还没有空间，即将为您创建')
          router.replace('/add_space')
          return
        }
      } else {
        // message.error('获取空间列表失败：' + res.data.message)
        return
      }
    } else {
      // 有空间 ID 时直接获取空间详情
      const res = await getSpaceVoByIdUsingGet({
        id: props.id,
      })

      if (res.data.code === 0 && res.data.data) {
        space.value = res.data.data
      } else {
        // message.error('获取空间详情失败：' + res.data.message)
      }
    }

    // 获取空间详情后加载图片列表
    await fetchData()
  } catch (e: any) {
    // message.error('获取空间信息失败：' + e.message)
  } finally {
    loading.value = false
  }
}

// --------- 获取图片列表 --------
// 定义数据
const dataList = ref<API.PictureVO[]>([])
const total = ref(0)
const loading = ref(true)

// 搜索条件
const searchParams = reactive<API.PictureQueryRequest>({
  current: 1,
  pageSize: 12,
  sortField: 'createTime',
  sortOrder: 'descend',
  orderBy: ['createTime DESC', 'id DESC'],
})

// 移动端加载更多相关变量
const loadingMore = ref(false)
const hasMore = ref(true)

// 检查是否触底
const checkScrollBottom = () => {
  if (device.value === DEVICE_TYPE_ENUM.PC) return

  const scrollHeight = document.documentElement.scrollHeight
  const scrollTop = document.documentElement.scrollTop || document.body.scrollTop
  const clientHeight = document.documentElement.clientHeight

  // 距离底部100px时开始加载
  if (scrollHeight - scrollTop - clientHeight < 100 && !loadingMore.value && hasMore.value && !loading.value) {
    loadMore()
  }
}

// 加载更多数据
const loadMore = async () => {
  if (loadingMore.value || !hasMore.value || loading.value) return

  loadingMore.value = true
  try {
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
        // 保持现有数据，将新数据追加到末尾
        dataList.value = [...dataList.value, ...newRecords]
      }
      hasMore.value = newRecords.length === searchParams.pageSize
    }
  } catch (error) {
    console.error('加载更多失败:', error)
  } finally {
    loadingMore.value = false
  }
}

// 重写获取数据方法
const fetchData = async () => {
  loading.value = true
  try {
    if(!props.id){
      await router.push('/')
      return
    }

    // 重置状态
    hasMore.value = true
    searchParams.current = 1

    // 确保每次请求都带上完整的排序参数
    const requestParams = {
      ...searchParams,
      spaceId: props.id,
      orderBy: ['createTime DESC', 'id DESC'],
    }

    const res = await listPictureVoByPageUsingPost(requestParams)

    if (res.data.code === 0) {
      // 首次加载或刷新时，替换整个列表
      dataList.value = res.data.data?.records ?? []
      hasMore.value = (res.data.data?.records?.length ?? 0) === searchParams.pageSize
    }
  } catch (error) {
    console.error('获取图片列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 按照颜色搜索
const onColorChange = async (color: string) => {
  loading.value = true
  const res = await searchPictureByColorUsingPost({
    picColor: color,
    spaceId: props.id,
  })
  if (res.data.code === 0 && res.data.data) {
    dataList.value = res.data.data ?? []
    total.value = dataList.value.length
  } else {
    // message.error('获取数据失败，' + res.data.message)
  }
  loading.value = false
}

// 搜索
const onSearch = (newSearchParams: API.PictureQueryRequest) => {
  if (!newSearchParams) return

  console.log('新的搜索参数:', newSearchParams)
  // 更新所有搜索参数
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

// ---- 批量编辑图片 -----
const batchEditPictureModalRef = ref()

// 批量编辑图片成功
const onBatchEditPictureSuccess = () => {
  fetchData()
}

// 打开批量编辑图片弹窗
const doBatchEdit = () => {
  if (batchEditPictureModalRef.value) {
    batchEditPictureModalRef.value.openModal()
  }
}

// 新增用于判断是否正在刷新的变量，避免重复触发刷新操作
const isRefreshing = ref(false)

// 重写刷新方法
const onRefresh = async () => {
  if (isRefreshing.value) return
  isRefreshing.value = true

  try {
    // 清空搜索组件的状态
    searchFormRef.value?.handleRefresh()

    // 重置搜索参数
    searchParams.current = 1
    // 清空现有数据
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

// 空间 id 改变时，必须重新获取数据
watch(
  () => props.id,
  (newSpaceId) => {
    fetchSpaceDetail()
    fetchData()
  },
)

// 计算总页数
const totalPages = computed(() => {
  const pages = Math.ceil(total.value / searchParams.pageSize)
  return isNaN(pages) ? 1 : pages
})

// 跳转页码
const jumpPage = ref('')

// 处理页码跳转
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

// 每页条数选项
const pageSizeOptions = [
  { text: '每页 6 条', value: 6 },
  { text: '每页 12 条', value: 12 },
  { text: '每页 18 条', value: 18 },
  { text: '每页 24 条', value: 24 },
  { text: '每页 30 条', value: 30 },
]

// 处理每页条数变化
const onPageSizeChange = (value: number) => {
  searchParams.current = 1
  searchParams.pageSize = value
  fetchData()
}

const showPageSizePopup = ref(false)

// 添加搜索表单的ref
const searchFormRef = ref()

const detailVisible = ref(false)
const spaceDetail = ref(null)

// 显示空间详情
const showSpaceDetail = async () => {
  try {
    const res = await getSpaceVoByIdUsingGet({ id: props.id })
    if (res.data?.code === 0) {
      spaceDetail.value = res.data.data
      detailVisible.value = true
    }
  } catch (error) {
    console.error('获取空间详情失败:', error)
    // message.error('获取空间详情失败')
  }
}

// 计算要显示的页码
const displayedPages = computed(() => {
  const totalPages = Math.ceil(total.value / searchParams.pageSize);
  const current = searchParams.current;
  const pages = [];

  if (totalPages <= 7) {
    // 如果总页数小于等于7，显示所有页码
    for (let i = 1; i <= totalPages; i++) {
      pages.push(i);
    }
  } else {
    // 总是显示第一页
    pages.push(1);

    if (current <= 4) {
      // 当前页靠近开始
      for (let i = 2; i <= 5; i++) {
        pages.push(i);
      }
      pages.push('...');
      pages.push(totalPages);
    } else if (current >= totalPages - 3) {
      // 当前页靠近结束
      pages.push('...');
      for (let i = totalPages - 4; i <= totalPages; i++) {
        pages.push(i);
      }
    } else {
      // 当前页在中间
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

// 添加和移除滚动监听
onMounted(() => {
  if (device.value !== DEVICE_TYPE_ENUM.PC) {
    window.addEventListener('scroll', checkScrollBottom)
  }
})

onUnmounted(() => {
  if (device.value !== DEVICE_TYPE_ENUM.PC) {
    window.removeEventListener('scroll', checkScrollBottom)
  }
})
</script>

<style scoped>
#spaceDetailPage {
  margin: 12px -20px 16px;
}

.search-spacing {
  margin-bottom: 16px;
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
    padding: 0 16px;
    margin: -8px 0 0;
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

/* 移除移动端分页器相关样式 */
.mobile-pagination-wrapper,
.pagination-info,
.separator,
.total-count,
.page-actions,
.page-button,
.page-size-trigger,
.page-size-popup,
.popup-title,
.page-size-list,
.page-size-item {
  display: none;
}

/* PC端分页器样式 */
.custom-pagination {
  margin-top: 8px;
  padding: 16px 24px;
  background: linear-gradient(to right, rgba(255, 255, 255, 0.95), rgba(255, 255, 255, 0.98));
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
  display: flex;
  justify-content: space-between;
  align-items: center;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.8);
}

.pagination-info {
  color: #64748b;
  font-size: 14px;
  font-weight: 500;
  background: linear-gradient(135deg, #64748b, #94a3b8);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-btn {
  padding: 8px 16px;
  border: none;
  background: linear-gradient(135deg, #ff8e53 0%, #ff6b6b 100%);
  color: white;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  font-size: 14px;
  font-weight: 500;
  box-shadow: 0 2px 10px rgba(255, 142, 83, 0.2);
  position: relative;
  overflow: hidden;
}

.page-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(
    120deg,
    transparent,
    rgba(255, 255, 255, 0.3),
    transparent
  );
  transition: 0.5s;
}

.page-btn:hover::before {
  left: 100%;
}

.page-btn:hover:not(.disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(255, 142, 83, 0.3);
}

.page-btn:active:not(.disabled) {
  transform: translateY(1px);
  box-shadow: 0 2px 8px rgba(255, 142, 83, 0.2);
}

.page-btn.disabled {
  background: linear-gradient(135deg, #e2e8f0 0%, #f1f5f9 100%);
  color: #94a3b8;
  cursor: not-allowed;
  box-shadow: none;
}

.page-numbers {
  display: flex;
  gap: 6px;
}

.page-number {
  width: 36px;
  height: 36px;
  border: none;
  background: white;
  color: #64748b;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 500;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  position: relative;
  overflow: hidden;
}

.page-number::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(
    120deg,
    transparent,
    rgba(255, 142, 83, 0.1),
    transparent
  );
  transition: 0.5s;
}

.page-number:hover::before {
  left: 100%;
}

.page-number:hover:not(.active) {
  color: #ff8e53;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(255, 142, 83, 0.15);
}

.page-number.active {
  background: linear-gradient(135deg, #ff8e53 0%, #ff6b6b 100%);
  color: white;
  box-shadow: 0 4px 15px rgba(255, 142, 83, 0.3);
  transform: scale(1.05);
}

.page-size-selector select {
  padding: 8px 12px;
  border: none;
  border-radius: 10px;
  color: #64748b;
  background: white;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='24' height='24' viewBox='0 0 24 24' fill='none' stroke='%2364748b' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolyline points='6 9 12 15 18 9'%3E%3C/polyline%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 8px center;
  background-size: 16px;
  padding-right: 32px;
}

.page-size-selector select:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(255, 142, 83, 0.15);
  color: #ff8e53;
}

.page-size-selector select:focus {
  outline: none;
  border-color: #ff8e53;
  box-shadow: 0 0 0 3px rgba(255, 142, 83, 0.1);
}

.page-jump {
  display: flex;
  gap: 8px;
  align-items: center;
}

.page-jump input {
  width: 60px;
  height: 36px;
  border: none;
  border-radius: 10px;
  text-align: center;
  color: #64748b;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  background: white;
}

.page-jump input:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(255, 142, 83, 0.15);
}

.page-jump input:focus {
  outline: none;
  box-shadow: 0 0 0 3px rgba(255, 142, 83, 0.1);
}

.jump-btn {
  padding: 8px 16px;
  border: none;
  background: linear-gradient(135deg, #ff8e53 0%, #ff6b6b 100%);
  color: white;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  font-size: 14px;
  font-weight: 500;
  box-shadow: 0 2px 10px rgba(255, 142, 83, 0.2);
  position: relative;
  overflow: hidden;
}

.jump-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(
    120deg,
    transparent,
    rgba(255, 255, 255, 0.3),
    transparent
  );
  transition: 0.5s;
}

.jump-btn:hover::before {
  left: 100%;
}

.jump-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(255, 142, 83, 0.3);
}

.jump-btn:active {
  transform: translateY(1px);
  box-shadow: 0 2px 8px rgba(255, 142, 83, 0.2);
}

.space-name-btn {
  color: #345750;
  font-size: 18px;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.3s ease;

  &:hover {
    color: #ff8e53;
    transform: translateY(-1px);
  }

  .info-icon {
    font-size: 16px;
    opacity: 0.7;
  }
}

.mobile-space-info {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  padding: 4px 12px;
  border-radius: 8px;
  transition: all 0.3s ease;

  &:hover {
    background: rgba(255, 142, 83, 0.1);
  }
}

.mobile-space-name {
  font-size: 16px;
  color: #345750;
  font-weight: 500;
}

/* 背景动画 */
.background-animation {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  overflow: hidden;
  z-index: 0;
  opacity: 0.6;
}

.circle {
  position: absolute;
  background: linear-gradient(135deg, rgba(255, 142, 83, 0.1), rgba(255, 107, 107, 0.08));
  border-radius: 50%;
  animation: float 20s infinite;
}

@keyframes float {
  0% {
    transform: translate(0, 0) rotate(0deg);
  }
  33% {
    transform: translate(2%, 2%) rotate(120deg);
  }
  66% {
    transform: translate(-2%, -1%) rotate(240deg);
  }
  100% {
    transform: translate(0, 0) rotate(360deg);
  }
}

.circle-1 {
  width: 300px;
  height: 300px;
  top: -150px;
  left: -150px;
  animation-delay: -5s;
}

.circle-2 {
  width: 400px;
  height: 400px;
  top: 40%;
  right: -200px;
  animation-delay: -10s;
}

.circle-3 {
  width: 200px;
  height: 200px;
  bottom: -100px;
  left: 30%;
  animation-delay: -15s;
}

</style>
