<template>
  <div id="spaceManagePage">
    <!-- PC端展示 -->
    <template v-if="device === DEVICE_TYPE_ENUM.PC">
      <a-flex justify="space-between">
        <h2>空间管理</h2>
        <a-space>
          <a-button type="primary" href="/add_space" class="action-button create-button">
            <PlusOutlined />创建空间
          </a-button>
          <a-button
            type="primary"
            href="/space_analyze?queryPublic=1"
            class="action-button analyze-public-button"
          >
            <BarChartOutlined />分析公共图库
          </a-button>
          <a-button
            type="primary"
            href="/space_analyze?queryAll=1"
            class="action-button analyze-all-button"
          >
            <LineChartOutlined />分析全部空间
          </a-button>
        </a-space>
      </a-flex>
      <div style="margin-bottom: 16px" />
      <!-- 搜索表单 -->
      <a-form layout="inline" :model="searchParams" @finish="doSearch">
        <a-form-item label="空间名称">
          <a-input
            v-model:value="searchParams.spaceName"
            placeholder="请输入空间名称"
            allow-clear
          />
        </a-form-item>
        <a-form-item name="spaceLevel" label="空间级别">
          <a-select
            v-model:value="searchParams.spaceLevel"
            style="min-width: 180px"
            placeholder="请选择空间级别"
            :options="SPACE_LEVEL_OPTIONS"
            allow-clear
          />
        </a-form-item>
        <a-form-item label="空间类别" name="spaceType">
          <a-select
            v-model:value="searchParams.spaceType"
            :options="SPACE_TYPE_OPTIONS"
            placeholder="请输入空间类别"
            style="min-width: 180px"
            allow-clear
          />
        </a-form-item>
        <a-form-item label="用户 id">
          <a-input v-model:value="searchParams.userId" placeholder="请输入用户 id" allow-clear />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit">搜索</a-button>
        </a-form-item>
      </a-form>
      <div style="margin-bottom: 16px" />
      <!-- 表格 -->
      <a-table
        :columns="columns"
        :data-source="dataList"
        :pagination="{
          current: searchParams.current,
          pageSize: searchParams.pageSize,
          total: total,
          showSizeChanger: true,
          showTotal: (total) => `共 ${total} 条`,
        }"
        :loading="loading"
        @change="doTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'spaceLevel'">
            <div>{{ SPACE_LEVEL_MAP[record.spaceLevel] }}</div>
          </template>
          <!-- 空间类别 -->
          <template v-if="column.dataIndex === 'spaceType'">
            <a-tag>{{ SPACE_TYPE_MAP[record.spaceType] }}</a-tag>
          </template>
          <template v-if="column.dataIndex === 'spaceUseInfo'">
            <div>大小：{{ formatSize(record.totalSize) }} / {{ formatSize(record.maxSize) }}</div>
            <div>数量：{{ record.totalCount }} / {{ record.maxCount }}</div>
          </template>
          <template v-if="column.dataIndex === 'createTime'">
            {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}
          </template>
          <template v-if="column.dataIndex === 'editTime'">
            {{ dayjs(record.editTime).format('YYYY-MM-DD HH:mm:ss') }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space wrap>
              <a-button
                type="primary"
                :href="`/space_analyze?spaceId=${record.id}`"
                class="table-button analyze-button"
              >
                <AreaChartOutlined />分析
              </a-button>
              <a-button
                type="primary"
                :href="`/add_space?id=${record.id}`"
                class="table-button edit-button"
              >
                <EditOutlined />编辑
              </a-button>
              <a-button
                :type="record.isRecommended === 1 ? 'default' : 'primary'"
                @click="toggleRecommendStatus(record)"
                class="table-button"
                :class="record.isRecommended === 1 ? 'unrecommend-button' : 'recommend-button'"
                :disabled="record.spaceType !== 1"
                :title="record.spaceType !== 1 ? '只有团队空间可以被推荐' : ''"
              >
                <component :is="record.isRecommended === 1 ? 'StarFilled' : 'StarOutlined'" />
                {{ record.isRecommended === 1 ? '取消推荐' : (record.spaceType === 1 ? '设为推荐' : '仅团队') }}
              </a-button>
              <a-button
                class="table-button delete-button"
                @click="showDeleteConfirm(record)"
              >
                <DeleteOutlined />删除
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </template>

    <!-- 移动端展示 -->
    <template v-else>
      <div class="mobile-container">
        <div class="mobile-content">
          <!-- 搜索区域 -->
          <div class="search-section">
            <van-search
              v-model="searchParams.spaceName"
              placeholder="搜索空间名称"
              @search="onSearch"
            />
          </div>

          <!-- 操作按钮区 -->
          <div class="action-bar">
            <div class="button-row">
              <van-button
                type="primary"
                block
                @click="() => router.push('/add_space')"
                class="mobile-button"
              >
                <template #icon><PlusOutlined /></template>
                创建空间
              </van-button>
              <van-button
                type="primary"
                block
                color="#ff9837"
                @click="() => router.push('/space_analyze?queryPublic=1')"
                class="mobile-button"
              >
                <template #icon><BarChartOutlined /></template>
                分析公共
              </van-button>
              <van-button
                type="primary"
                block
                color="#e380dc"
                @click="() => router.push('/space_analyze?queryAll=1')"
                class="mobile-button"
              >
                <template #icon><LineChartOutlined /></template>
                分析全部
              </van-button>
            </div>
          </div>

          <!-- 空间列表 -->
          <div class="space-list">
            <a-spin :spinning="loading">
              <van-cell-group inset v-for="space in dataList" :key="space.id">
                <van-card class="space-card">
                  <template #title>
                    <div class="card-title">{{ space.spaceName }}</div>
                  </template>

                  <template #desc>
                    <div class="card-info">
                      <div class="info-item">
                        <span class="label">空间级别：</span>
                        <span class="value">{{ SPACE_LEVEL_MAP[space.spaceLevel] }}</span>
                      </div>
                      <div class="info-item">
                        <span class="label">空间类型：</span>
                        <span class="value">{{ SPACE_TYPE_MAP[space.spaceType] }}</span>
                      </div>
                      <div class="info-item">
                        <span class="label">使用情况：</span>
                        <div class="value">
                          <div>
                            大小：{{ formatSize(space.totalSize) }} /
                            {{ formatSize(space.maxSize) }}
                          </div>
                          <div>数量：{{ space.totalCount }} / {{ space.maxCount }}</div>
                        </div>
                      </div>
                      <div class="info-item">
                        <span class="label">用户ID：</span>
                        <span class="value">{{ space.userId }}</span>
                      </div>
                      <div class="info-item">
                        <span class="label">推荐状态：</span>
                        <span class="value">
                          <van-tag :type="space.isRecommended === 1 ? 'warning' : 'default'">
                            {{ space.isRecommended === 1 ? '已推荐' : '未推荐' }}
                          </van-tag>
                        </span>
                      </div>
                      <div class="info-item">
                        <span class="label">创建时间：</span>
                        <span class="value">{{
                            dayjs(space.createTime).format('YYYY-MM-DD HH:mm:ss')
                          }}</span>
                      </div>
                    </div>
                  </template>

                  <template #footer>
                    <div class="card-footer">
                      <van-button
                        type="primary"
                        size="small"
                        color="#4fe2ee"
                        @click="() => router.push(`/space_analyze?spaceId=${space.id}`)"
                      >
                        <template #icon><AreaChartOutlined /></template>
                        分析
                      </van-button>
                      <van-button
                        type="primary"
                        size="small"
                        color="#39cb7d"
                        @click="() => router.push(`/add_space?id=${space.id}`)"
                      >
                        <template #icon><EditOutlined /></template>
                        编辑
                      </van-button>
                      <van-button
                        size="small"
                        :class="space.isRecommended === 1 ? 'van-button--unrecommend' : 'van-button--recommend'"
                        @click="toggleRecommendStatus(space)"
                        :disabled="space.spaceType !== 1"
                      >
                        <template #icon>
                          <StarFilled v-if="space.isRecommended === 1" />
                          <StarOutlined v-else />
                        </template>
                        {{ space.isRecommended === 1 ? '取消推荐' : (space.spaceType === 1 ? '设为推荐' : '仅团队') }}
                      </van-button>
                      <van-button
                        type="danger"
                        size="small"
                        @click="showDeleteConfirm(space)"
                      >
                        <template #icon><DeleteOutlined /></template>
                        删除
                      </van-button>
                    </div>
                  </template>
                </van-card>
              </van-cell-group>
            </a-spin>

            <!-- 分页器 -->
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
                  @change="onPageChange"
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

            <!-- 每页条数选择器 -->
            <van-action-sheet
              v-model:show="showPageSizeSheet"
              :actions="pageSizeOptions"
              @select="handlePageSizeChange"
              cancel-text="取消"
              close-on-click-action
            />
          </div>
        </div>
      </div>
    </template>

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
        <h3 class="confirm-title">确认删除该空间？</h3>
        <p class="confirm-desc">
          空间名称：{{ selectedSpace?.spaceName }}<br>
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
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { deleteSpaceUsingPost, listSpaceByPageUsingPost, setRecommendStatusUsingPost } from '@/api/spaceController.ts'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { SPACE_LEVEL_MAP, SPACE_LEVEL_OPTIONS, SPACE_TYPE_MAP, SPACE_TYPE_OPTIONS } from '@/constants/space.ts'
import { formatSize } from '@/utils'
import { useRouter } from 'vue-router'
import {
  PlusOutlined,
  BarChartOutlined,
  LineChartOutlined,
  AreaChartOutlined,
  EditOutlined,
  DeleteOutlined,
  ExclamationCircleFilled,
  StarOutlined,
  StarFilled,
} from '@ant-design/icons-vue'
import { getDeviceType } from '@/utils/device.ts'
import { DEVICE_TYPE_ENUM } from '@/constants/device.ts'

// 定义用于存储设备类型的响应式变量
const device = ref<string>('')
// 页面加载时获取设备类型并获取数据
onMounted(async () => {
  device.value = await getDeviceType()
  loadData()
})
const router = useRouter()

const showPageSizeSheet = ref(false)

// 移动端分页选项
const pageSizeOptions = [
  { name: '10条/页', value: 10 },
  { name: '20条/页', value: 20 },
  { name: '30条/页', value: 30 },
  { name: '50条/页', value: 50 },
]

const columns = [
  {
    title: 'id',
    dataIndex: 'id',
    width: 80,
  },
  {
    title: '空间名称',
    dataIndex: 'spaceName',
  },
  {
    title: '空间级别',
    dataIndex: 'spaceLevel',
  },
  {
    title: '空间类别',
    dataIndex: 'spaceType',
  },
  {
    title: '使用情况',
    dataIndex: 'spaceUseInfo',
  },
  {
    title: '用户 id',
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

const loading = ref(false)
const dataList = ref<API.SpaceVO[]>([])
const total = ref(0)

const searchParams = reactive({
  current: 1,
  pageSize: 10,
  spaceName: '',
  spaceLevel: undefined,
  spaceType: undefined,
  userId: undefined,
})

// 修改表格变化处理函数
const doTableChange = (pagination: any) => {
  searchParams.current = pagination.current
  searchParams.pageSize = pagination.pageSize
  loadData()
}

// 统一的数据加载函数
const loadData = async () => {
  if (loading.value) return
  loading.value = true
  try {
    const res = await listSpaceByPageUsingPost(searchParams)
    if (res.data.code === 0) {
      dataList.value = res.data.data.records ?? []
      total.value = res.data.data.total ?? 0
    } else {
      message.error('获取数据失败')
    }
  } catch (error) {
    message.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

// 页面卸载时清理
onUnmounted(() => {
  // 重置所有状态
  loading.value = false
  dataList.value = []
  total.value = 0
  searchParams.current = 1
  searchParams.pageSize = 10
})

// 搜索处理函数
const onSearch = () => {
  searchParams.current = 1
  loadData()
}

// 移动端页码变化处理
const onPageChange = (page: number) => {
  searchParams.current = page
  loadData()
}

// 移动端每页条数变化处理
const handlePageSizeChange = (option: any) => {
  searchParams.pageSize = option.value
  searchParams.current = 1
  loadData()
  showPageSizeSheet.value = false
}

// PC端搜索处理
const doSearch = () => {
  searchParams.current = 1
  loadData()
}

// 删除确认相关的状态
const deleteConfirmVisible = ref(false)
const selectedSpace = ref<API.SpaceVO | null>(null)

// 显示删除确认框
const showDeleteConfirm = (space: API.SpaceVO) => {
  selectedSpace.value = space
  deleteConfirmVisible.value = true
}

// 确认删除
const confirmDelete = async () => {
  if (!selectedSpace.value?.id) return

  try {
    const res = await deleteSpaceUsingPost({ id: selectedSpace.value.id })
    if (res.data.code === 0) {
      // message.success('删除成功')
      deleteConfirmVisible.value = false
      // 刷新数据
      loadData()
    } else {
      message.error('删除失败：' + res.data.message)
    }
  } catch (error) {
    message.error('删除失败，请稍后重试')
  }
}

// 切换推荐状态
const toggleRecommendStatus = async (space: API.SpaceVO) => {
  if (!space.id) return

  try {
    const recommendStatus = space.isRecommended === 1 ? 0 : 1
    const res = await setRecommendStatusUsingPost({
      spaceId: space.id,
      recommendStatus: recommendStatus
    })

    if (res.data.code === 0) {
      message.success(space.isRecommended === 1 ? '已取消推荐' : '已设为推荐')
      // 更新本地数据
      space.isRecommended = recommendStatus
      // 刷新数据
      loadData()
    } else {
      message.error('操作失败：' + res.data.message)
    }
  } catch (error) {
    message.error('操作失败，请稍后重试')
  }
}

// 移动端分页处理方法

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
  loadData()
  jumpPage.value = ''
}
</script>

<style scoped>
@media screen and (min-width: 768px) {
  #spaceManagePage {
    background: var(--header-background);
    color: var(--text-primary)!important;
    width: 100vw;
    margin: 0 auto;
    padding: 24px;
  }
}

/* 移动端样式 */
.mobile-container {
  min-height: calc(100vh - 100px);
}

.mobile-content {
  padding: 0;

  border-radius: 12px;
}

.action-bar {
  margin-bottom: 8px;
  padding: 0 12px;
}

.button-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  width: 100%;
}

.mobile-button {
  height: 36px;
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.mobile-button :deep(.van-button__content) {
  padding: 0 4px;
}

.mobile-button :deep(.van-button__icon) {
  font-size: 16px;
}

.space-list {
  margin: 0;
}

:deep(.van-cell-group) {
  margin: 8px 0;
  border-radius: 12px;
}

:deep(.van-cell-group--inset) {
  margin: 8px 0;
}

.space-card {
  background: white;
  border-radius: 12px;
  margin: 0;
}

.card-title {
  font-size: 16px;
  font-weight: 500;
  color: #1a1a1a;
  margin-bottom: 8px;
}

.card-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.info-item {
  margin-bottom: 8px;
}

.info-item .label {
  color: #999;
  margin-right: 8px;
}

.info-item .value {
  color: #323233;
}

.card-footer {
  display: flex;
  gap: 8px;
  margin-top: 12px;
  padding: 0;
}

/* 添加移动端分页样式 */
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

.pagination-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

/* 分页器样式优化 */
:deep(.custom-pagination) {
  .van-pagination__item {
    min-width: 28px;
    height: 28px;
    line-height: 28px;
    border-radius: 16px;
    font-size: 14px;
    border: 1px solid #ebedf0;
    margin: 0 2px;
  }

  .van-pagination__item--active {
    background: #1989fa;
    color: white;
    border-color: #1989fa;
  }

  .van-pagination__prev,
  .van-pagination__next {
    background: var(--header-background);
    border: 1px solid #ebedf0;
    font-weight: bold;
    min-width: 28px !important;
    height: 28px !important;
    line-height: 28px !important;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .van-icon {
    font-size: 14px;
    color: #666;
  }
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


.search-section {
  position: sticky;
  top: 0;
  z-index: 100;
  background: var(--header-background);
  /* 抵消父元素的内边距 */
  border-radius: 8px!important;
  margin-bottom: 10px;
}

:deep(.van-search__content) {
  border-radius: 8px;
  background: white;
}

/* 顶部操作按钮样式 */
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

.create-button {
  background: linear-gradient(135deg, #40c9ff 0%, #1890ff 100%);
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.2);
}

.create-button:hover {
  background: linear-gradient(135deg, #69d4ff 0%, #40a9ff 100%);
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(24, 144, 255, 0.3);
}

.analyze-public-button {
  background: linear-gradient(135deg, #ffa940 0%, #ff9837 100%);
  box-shadow: 0 4px 12px rgba(255, 152, 55, 0.2);
}

.analyze-public-button:hover {
  background: linear-gradient(135deg, #ffbb6b 0%, #ffa940 100%);
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(255, 152, 55, 0.3);
}

.analyze-all-button {
  background: linear-gradient(135deg, #eb7ae2 0%, #e380dc 100%);
  box-shadow: 0 4px 12px rgba(227, 128, 220, 0.2);
}

.analyze-all-button:hover {
  background: linear-gradient(135deg, #f19be9 0%, #eb7ae2 100%);
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(227, 128, 220, 0.3);
}

/* 表格按钮样式 */
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
  color: white;
}

.table-button .anticon {
  font-size: 14px;
}

.analyze-button {
  background: linear-gradient(135deg, #73e8ee 0%, #4fe2ee 100%);
  box-shadow: 0 2px 8px rgba(79, 226, 238, 0.2);
}

.analyze-button:hover {
  background: linear-gradient(135deg, #8eedf2 0%, #73e8ee 100%);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(79, 226, 238, 0.3);
}

.edit-button {
  background: linear-gradient(135deg, #52d990 0%, #39cb7d 100%);
  box-shadow: 0 2px 8px rgba(57, 203, 125, 0.2);
}

.edit-button:hover {
  background: linear-gradient(135deg, #6bdfa3 0%, #52d990 100%);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(57, 203, 125, 0.3);
}

.delete-button {
  background: linear-gradient(135deg, #ff7875 0%, #ff4d4f 100%);
  box-shadow: 0 2px 8px rgba(255, 77, 79, 0.2);
}

.delete-button:hover {
  background: linear-gradient(135deg, #ff9c9a 0%, #ff7875 100%);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(255, 77, 79, 0.3);
}

/* 移动端卡片按钮样式 */
:deep(.van-button) {
  border: none;
  border-radius: 6px;
  font-size: 13px;
  height: 28px;
  padding: 0 12px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

:deep(.van-button__icon) {
  font-size: 14px;
}

/* 按钮激活状态 */
.action-button:active,
.table-button:active,
:deep(.van-button:active) {
  transform: translateY(0);
  opacity: 0.9;
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

/* 推荐按钮样式 */
.recommend-button {
  background: linear-gradient(135deg, #2196f3 0%, #0d8aee 100%) !important;
  box-shadow: 0 2px 8px rgba(33, 150, 243, 0.2) !important;
  color: white !important;
}

.recommend-button:hover {
  background: linear-gradient(135deg, #42a5f5 0%, #2196f3 100%) !important;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(33, 150, 243, 0.3) !important;
}

.unrecommend-button {
  background: linear-gradient(135deg, #ff9800 0%, #f57c00 100%) !important;
  box-shadow: 0 2px 8px rgba(245, 124, 0, 0.2) !important;
  color: white !important;
}

.unrecommend-button:hover {
  background: linear-gradient(135deg, #ffaa33 0%, #ff8a00 100%) !important;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(245, 124, 0, 0.3) !important;
}

/* 移动端推荐按钮样式 */
:deep(.van-button--recommend) {
  background: linear-gradient(135deg, #2196f3 0%, #0d8aee 100%) !important;
  border-color: transparent !important;
  color: white !important;
}

:deep(.van-button--recommend):active {
  background: linear-gradient(135deg, #42a5f5 0%, #2196f3 100%) !important;
}

:deep(.van-button--unrecommend) {
  background: linear-gradient(135deg, #ff9800 0%, #f57c00 100%) !important;
  border-color: transparent !important;
  color: white !important;
}

:deep(.van-button--unrecommend):active {
  background: linear-gradient(135deg, #ffaa33 0%, #ff8a00 100%) !important;
}
</style>
