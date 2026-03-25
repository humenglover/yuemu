<template>
  <div id="reportManagePage">
    <!-- PC端展示 -->
    <template v-if="device === DEVICE_TYPE_ENUM.PC">
      <div class="pc-container">
        <!-- 搜索表单 -->
        <div class="search-and-button-container">
          <a-form layout="inline" :model="searchParams" @finish="doSearch">
            <a-form-item label="举报人ID">
              <a-input
                v-model:value="searchParams.userId"
                placeholder="输入举报人ID"
                allow-clear
              />
            </a-form-item>
            <a-form-item label="举报类型">
              <a-select
                v-model:value="searchParams.reportType"
                placeholder="选择举报类型"
                allow-clear
                style="width: 150px"
              >
                <a-select-option :value="1">垃圾广告</a-select-option>
                <a-select-option :value="2">违规内容</a-select-option>
                <a-select-option :value="3">有害信息</a-select-option>
                <a-select-option :value="4">人身攻击</a-select-option>
                <a-select-option :value="5">侵犯隐私</a-select-option>
                <a-select-option :value="6">版权问题</a-select-option>
                <a-select-option :value="7">其他</a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="处理状态">
              <a-select
                v-model:value="searchParams.status"
                placeholder="选择处理状态"
                allow-clear
                style="width: 120px"
              >
                <a-select-option :value="0">待处理</a-select-option>
                <a-select-option :value="1">已处理</a-select-option>
                <a-select-option :value="2">驳回</a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item>
              <a-button type="primary" html-type="submit" class="action-button">
                <SearchOutlined />
                搜索
              </a-button>
            </a-form-item>
          </a-form>
          <div style="display: flex; align-items: center">
            <a-button type="dashed" class="sort-button" @click="toggleSortOrder">
              <span class="button-content">
                <SortAscendingOutlined v-if="sortOrder === 'ascend'" />
                <SortDescendingOutlined v-else />
                <span class="button-text">{{ sortOrder === 'ascend' ? '升序' : '降序' }}</span>
              </span>
            </a-button>
          </div>
        </div>

        <div class="table-section">
          <a-spin tip="正在加载中..." :spinning="loading">
            <a-table
              rowKey="id"
              :columns="columns"
              :data-source="dataList"
              :pagination="false"
              @change="doTableChange"
              class="custom-table"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'userAvatar'">
                  <a-image
                    :src="record.userAvatar"
                    :width="50"
                    :style="{ maxHeight: '50px', objectFit: 'contain' }"
                  />
                </template>
                <template v-else-if="column.dataIndex === 'reportTypeText'">
                  <a-tag :color="getReportTypeColor(record.reportType)">
                    {{ record.reportTypeText }}
                  </a-tag>
                </template>
                <template v-else-if="column.dataIndex === 'targetTypeText'">
                  <a-tag color="blue">
                    {{ record.targetTypeText }}
                  </a-tag>
                </template>
                <template v-else-if="column.dataIndex === 'statusText'">
                  <a-tag :color="getStatusColor(record.status)">
                    {{ record.statusText }}
                  </a-tag>
                </template>
                <template v-else-if="column.dataIndex === 'screenshotUrls'">
                  <div v-if="record.screenshotUrls && record.screenshotUrls.length > 0">
                    <a-popover v-for="(url, index) in record.screenshotUrls" :key="index">
                      <template #content>
                        <a-image :src="url" :width="200" />
                      </template>
                      <a-image :src="url" :width="30" :height="30" style="margin-right: 4px; cursor: zoom-in;" />
                    </a-popover>
                  </div>
                  <span v-else>无截图</span>
                </template>
                <template v-else-if="column.dataIndex === 'createTime'">
                  <div
                    :style="{
                      maxWidth: '150px',
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                      whiteSpace: 'nowrap',
                    }"
                  >
                    {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}
                  </div>
                </template>
                <template v-else-if="column.dataIndex === 'handleTime'">
                  <div
                    :style="{
                      maxWidth: '150px',
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                      whiteSpace: 'nowrap',
                    }"
                  >
                    {{ record.handleTime ? dayjs(record.handleTime).format('YYYY-MM-DD HH:mm:ss') : '未处理' }}
                  </div>
                </template>
                <template v-else-if="column.key === 'action'">
                  <div class="action-buttons">
                    <a-button
                      v-if="record.status === 0"
                      type="primary"
                      class="process-button"
                      @click="openProcessModal(record)"
                    >
                      <ToolOutlined />
                      处理
                    </a-button>
                    <a-button
                      v-else
                      type="default"
                      class="view-button"
                      disabled
                    >
                      <EyeOutlined />
                      已处理
                    </a-button>
                  </div>
                </template>
              </template>
            </a-table>
          </a-spin>
        </div>

        <!-- 分页组件 -->
        <div class="mz-antd-pagination">
          <a-pagination
            v-model:current="searchParams.current"
            :page-size-options="pcPageSizeOptions"
            :total="total"
            :show-total="(total) => `共 ${total} 条`"
            show-size-changer
            :page-size="searchParams.pageSize"
            @change="onPageChange"
            @showSizeChange="onShowSizeChange"
          >
          </a-pagination>
        </div>
      </div>

      <!-- 处理举报模态框 -->
      <a-modal v-model:open="processModalOpen" title="处理举报" @ok="handleProcessReport">
        <a-form :model="processForm" layout="vertical">
          <a-form-item label="处理状态">
            <a-select v-model:value="processForm.status">
              <a-select-option :value="1">已处理</a-select-option>
              <a-select-option :value="2">驳回</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="处理结果">
            <a-textarea
              v-model:value="processForm.handleResult"
              placeholder="请输入处理结果"
              :rows="4"
            />
          </a-form-item>
        </a-form>
      </a-modal>
    </template>

    <!-- 移动端展示 -->
    <template v-else>
      <div class="mobile-container">
        <div class="mobile-content">
          <!-- 搜索区域 -->
          <div class="search-section">
            <van-search
              v-model="searchParams.userId"
              placeholder="搜索举报人ID"
              @search="doSearch"
            />
          </div>

          <!-- 操作按钮区 -->
          <div class="action-bar">
            <a-button type="dashed" class="sort-button" @click="toggleSortOrder">
              <span class="button-content">
                <span class="button-text">{{ sortOrder === 'ascend' ? '升序' : '降序' }}</span>
              </span>
            </a-button>
          </div>

          <!-- 举报列表 -->
          <div class="report-list">
            <van-cell-group inset v-for="report in dataList" :key="report.id">
              <van-card
                :title="`ID: ${report.id} | 目标ID: ${report.targetId}`"
                :desc="report.reason || '无举报原因'"
                :thumb="report.userAvatar"
                class="report-card"
                :thumb-style="{ width: '45px', height: '45px', borderRadius: '4px' }"
              >
                <template #tags>
                  <div class="report-info">
                    <div class="info-row">
                      <span class="info-label">举报人:</span>
                      <span class="info-value">{{ report.userName || '匿名用户' }}</span>
                    </div>
                    <div class="info-row">
                      <span class="info-label">举报类型:</span>
                      <span class="info-value">{{ report.reportTypeText }}</span>
                    </div>
                    <div class="info-row">
                      <span class="info-label">目标类型:</span>
                      <span class="info-value">{{ report.targetTypeText }}</span>
                    </div>
                    <div class="info-row">
                      <span class="info-label">处理状态:</span>
                      <span class="info-value">{{ report.statusText }}</span>
                    </div>
                    <div v-if="report.handlerName" class="info-row">
                      <span class="info-label">处理人:</span>
                      <span class="info-value">{{ report.handlerName }}</span>
                    </div>
                    <div v-if="report.handleResult" class="info-row">
                      <span class="info-label">处理结果:</span>
                      <span class="info-value">{{ report.handleResult }}</span>
                    </div>
                  </div>
                  <!-- 举报截图展示 -->
                  <div v-if="report.screenshotUrls && report.screenshotUrls.length > 0" class="report-screenshots">
                    <div class="screenshot-label">截图:</div>
                    <div class="screenshot-list">
                      <img
                        v-for="(url, index) in report.screenshotUrls.slice(0, 3)"
                        :key="index"
                        :src="url"
                        class="screenshot-thumb"
                        @click.stop="previewImage(url)"
                      >
                      <span v-if="report.screenshotUrls.length > 3" class="more-screenshots">
                        +{{ report.screenshotUrls.length - 3 }}
                      </span>
                    </div>
                  </div>
                  <div v-else class="no-screenshots">
                    无截图
                  </div>
                </template>

                <template #bottom>
                  <div class="report-time">
                    <div>创建时间: {{ dayjs(report.createTime).format('YYYY-MM-DD HH:mm') }}</div>
                    <div v-if="report.handleTime">处理时间: {{ dayjs(report.handleTime).format('YYYY-MM-DD HH:mm') }}</div>
                  </div>
                </template>

                <template #footer>
                  <div class="card-footer">
                    <van-button
                      v-if="report.status === 0"
                      size="mini"
                      type="primary"
                      plain
                      @click="openMobileProcessModal(report)"
                      class="process-button"
                      style="margin-right: 8px"
                    >
                      <template #icon>
                        <ToolOutlined />
                      </template>
                      处理
                    </van-button>
                    <van-button
                      v-else
                      size="mini"
                      type="default"
                      plain
                      disabled
                      class="view-button"
                      style="margin-right: 8px"
                    >
                      <template #icon>
                        <EyeOutlined />
                      </template>
                      已处理
                    </van-button>
                  </div>
                </template>
              </van-card>
            </van-cell-group>
          </div>

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
        </div>

        <!-- 移动端处理举报弹窗 -->
        <van-popup v-model:show="mobileProcessModalOpen" position="bottom" round :style="{ height: '70%' }">
          <div class="popup-content">
            <div class="popup-header">
              <span class="popup-title">处理举报</span>
              <van-icon name="cross" @click="mobileProcessModalOpen = false" />
            </div>
            <van-form @submit="handleMobileProcessReport">
              <van-cell-group inset>
                <van-field name="status" label="处理状态">
                  <template #input>
                    <van-radio-group v-model="mobileProcessForm.status" direction="horizontal">
                      <van-radio :name="1">已处理</van-radio>
                      <van-radio :name="2">驳回</van-radio>
                    </van-radio-group>
                  </template>
                </van-field>
                <van-field
                  v-model="mobileProcessForm.handleResult"
                  name="handleResult"
                  label="处理结果"
                  type="textarea"
                  placeholder="请输入处理结果"
                  rows="4"
                  :rules="[{ required: true, message: '请填写处理结果' }]"
                />
              </van-cell-group>
              <div style="margin: 16px">
                <van-button round block type="primary" native-type="submit">提交</van-button>
              </div>
            </van-form>
          </div>
        </van-popup>
      </div>
    </template>
  </div>
</template>

<script lang="ts" setup>
import { onMounted, reactive, ref } from 'vue'
import { listReportByPageUsingPost, updateReportUsingPost } from '@/api/reportController.ts'
import { Form, message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { getDeviceType } from '@/utils/device.ts'
import { DEVICE_TYPE_ENUM } from '@/constants/device.ts'
import {
  PlusOutlined,
  DeleteOutlined,
  SortAscendingOutlined,
  SortDescendingOutlined,
  SearchOutlined,
  ExclamationCircleFilled,
  MailOutlined,
  EyeOutlined,
  ToolOutlined,
} from '@ant-design/icons-vue'

// 定义用于存储设备类型的响应式变量
const device = ref<string>('')

// 页面加载时获取设备类型并获取数据
onMounted(async () => {
  device.value = await getDeviceType()
})

// 表格列定义
const columns = [
  {
    title: 'ID',
    dataIndex: 'id',
    width: 80,
    ellipsis: true,
  },
  {
    title: '举报人',
    dataIndex: 'userAvatar',
    width: 120,
    customRender: ({ record }) => {
      return `${record.userName || '匿名用户'}`
    },
  },
  {
    title: '举报类型',
    dataIndex: 'reportTypeText',
    width: 100,
  },
  {
    title: '目标类型',
    dataIndex: 'targetTypeText',
    width: 100,
  },
  {
    title: '目标ID',
    dataIndex: 'targetId',
    width: 150,
    ellipsis: true,
  },
  {
    title: '举报原因',
    dataIndex: 'reason',
    width: 180,
    ellipsis: true,
  },
  {
    title: '举报截图',
    dataIndex: 'screenshotUrls',
    width: 120,
  },
  {
    title: '处理状态',
    dataIndex: 'statusText',
    width: 100,
  },
  {
    title: '处理人',
    dataIndex: 'handlerName',
    width: 120,
    ellipsis: true,
  },
  {
    title: '处理结果',
    dataIndex: 'handleResult',
    width: 150,
    ellipsis: true,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 160,
  },
  {
    title: '处理时间',
    dataIndex: 'handleTime',
    width: 160,
  },
  {
    title: '操作',
    key: 'action',
    width: 120,
  },
]

// 定义数据
const dataList = ref<any[]>([])
const total = ref(0)
const loading = ref(false)
const showPageSizeSheet = ref(false)
const jumpPage = ref('')

// PC端分页选项
const pcPageSizeOptions = ['5', '8', '10', '20', '50']

// 移动端分页选项
const mobilePageSizeOptions = [
  { name: '10条/页', value: 10 },
  { name: '20条/页', value: 20 },
  { name: '30条/页', value: 30 },
  { name: '50条/页', value: 50 },
]

// 搜索条件
const searchParams = reactive<any>({
  current: 1,
  pageSize: 8,
  sortField: 'createTime',
  userId: undefined,
})

const sortOrder = ref<'ascend' | 'descend'>('ascend')

// 处理举报相关的状态
const processModalOpen = ref(false)
const processForm = reactive({
  id: 0,
  status: 1,
  handleResult: '',
})

const mobileProcessModalOpen = ref(false)
const mobileProcessForm = reactive({
  id: 0,
  status: 1,
  handleResult: '',
})

// 获取数据
const fetchData = async () => {
  if (device.value === DEVICE_TYPE_ENUM.PC) {
    loading.value = true
  }

  try {
    const res = await listReportByPageUsingPost({
      ...searchParams,
      sortOrder: sortOrder.value,
    })
    if (res.data?.code === 0) {
      // 如果是移动端加载更多，追加数据
      if (device.value !== DEVICE_TYPE_ENUM.PC && searchParams.current > 1) {
        dataList.value = [...dataList.value, ...res.data.data.records]
      } else {
        // PC端或移动端首次加载/刷新，直接替换数据
        dataList.value = res.data.data.records
      }
      total.value = parseInt(res.data.data.total)
      console.log('获取到的举报数据:', dataList.value)
    }
  } catch (error) {
    console.error('获取举报数据失败:', error)
    message.error('获取举报数据失败')
  } finally {
    loading.value = false
  }
}

// 处理分页尺寸改变时的逻辑
const onShowSizeChange = (current: number, pageSize: number) => {
  searchParams.current = 1
  searchParams.pageSize = pageSize
  fetchData()
}

// 处理页码改变时的逻辑
const onPageChange = (page: number, pageSize: number) => {
  searchParams.current = page
  searchParams.pageSize = pageSize
  fetchData()
}

// 表格变化之后，重新获取数据
const doTableChange = (page: any) => {
  searchParams.current = page.current
  searchParams.pageSize = page.pageSize
  if (page.sortField && page.sortOrder) {
    searchParams.sortField = page.sortField
    searchParams.sortOrder = page.sortOrder === 'ascend' ? 'ascend' : 'descend'
  }
  fetchData()
}

// 搜索数据
const doSearch = async () => {
  try {
    if (device.value === DEVICE_TYPE_ENUM.PC) {
      loading.value = true
    }

    const res = await listReportByPageUsingPost({
      ...searchParams,
      sortField: 'createTime',
      sortOrder: sortOrder.value,
    })

    if (res.data?.code === 0) {
      dataList.value = res.data.data.records
      total.value = parseInt(res.data.data.total)
      console.log('搜索结果:', dataList.value)
    } else {
      message.error('获取举报列表失败，请稍后重试')
    }
  } catch (error) {
    console.error('获取举报列表异常', error)
    message.error('获取举报列表失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 组件挂载时获取数据
onMounted(async () => {
  try {
    await doSearch()
  } catch (error) {
    console.error('初始化加载数据失败：', error)
  }
})

// 新增：处理排序按钮点击事件，切换排序顺序并刷新数据
const toggleSortOrder = () => {
  sortOrder.value = sortOrder.value === 'ascend' ? 'descend' : 'ascend'
  searchParams.sortOrder = sortOrder.value
  fetchData()
}

// 移动端分页处理方法
const onMobilePageChange = async (page: number) => {
  searchParams.current = page
  await doSearch()
}

// 处理每页条数改变
const handlePageSizeChange = async (action: { value: number }) => {
  searchParams.current = 1
  searchParams.pageSize = action.value
  await doSearch()
}

// 处理页码跳转
const handleJumpPage = async () => {
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
  await doSearch()
  jumpPage.value = ''
}

// 获取举报类型颜色
const getReportTypeColor = (type: number) => {
  const colors = [
    'default',
    'red',
    'orange',
    'gold',
    'lime',
    'cyan',
    'blue',
    'purple',
  ]
  return colors[type] || 'default'
}

// 获取状态颜色
const getStatusColor = (status: number) => {
  if (status === 0) return 'orange'
  if (status === 1) return 'green'
  if (status === 2) return 'red'
  return 'default'
}

// 打开处理举报模态框
const openProcessModal = (report: API.ReportVO) => {
  processForm.id = report.id
  processForm.status = 1
  processForm.handleResult = ''
  processModalOpen.value = true
}

// 处理举报
const handleProcessReport = async () => {
  try {
    const res = await updateReportUsingPost({
      id: processForm.id,
      status: processForm.status,
      handleResult: processForm.handleResult,
    })

    if (res.data.code === 0) {
      message.success('处理成功')
      processModalOpen.value = false
      fetchData()
    } else {
      message.error('处理失败：' + res.data.message)
    }
  } catch (error) {
    console.error('处理举报失败', error)
    message.error('处理失败，请稍后重试')
  }
}

// 打开移动端处理举报模态框
const openMobileProcessModal = (report: API.ReportVO) => {
  mobileProcessForm.id = report.id
  mobileProcessForm.status = 1
  mobileProcessForm.handleResult = ''
  mobileProcessModalOpen.value = true
}

// 预览图片
const previewImage = (url: string) => {
  // 使用vant的图片预览组件
  if (window.Vant && window.Vant.ImagePreview) {
    window.Vant.ImagePreview([url]);
  } else {
    // 如果没有引入ImagePreview，新开窗口查看图片
    window.open(url, '_blank');
  }
}

// 处理移动端举报
const handleMobileProcessReport = async () => {
  try {
    const res = await updateReportUsingPost({
      id: mobileProcessForm.id,
      status: mobileProcessForm.status,
      handleResult: mobileProcessForm.handleResult,
    })

    if (res.data.code === 0) {
      message.success('处理成功')
      mobileProcessModalOpen.value = false
      fetchData()
    } else {
      message.error('处理失败：' + res.data.message)
    }
  } catch (error) {
    console.error('处理举报失败', error)
    message.error('处理失败，请稍后重试')
  }
}
</script>

<style scoped>
#reportManagePage {
  background: var(--header-background);
  color: var(--text-primary) !important;
}

.mz-antd-pagination {
  text-align: right;
  margin-top: 20px;
}

/* 按钮内容部分样式（图标和文字） */
.button-content {
  display: flex;
  align-items: center;
}

.search-and-button-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* 模态框内表单样式调整 */
.a-modal form {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.loading-spin {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}

/* 移动端样式 */
.mobile-container {
  background: var(--header-background);
  color: var(--text-primary);
  min-height: 100vh;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  overflow: hidden;
}

.mobile-content {
  height: 100%;
  overflow-y: auto;
  padding: 12px;
  -webkit-overflow-scrolling: touch;
}

.action-bar {
  padding: 0 12px 12px;
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: flex-end;
}

/* 调整按钮顺序 */
.action-bar > * {
  order: 1;
}

.action-bar .sort-button {
  order: 2;
  height: 32px;
  padding: 0 12px;
}

.report-card {
  background: var(--header-background);
  color: var(--text-primary);
  border-radius: 8px;
  overflow: hidden;
  padding: 12px;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 4px;
}

.account-text {
  font-size: 12px;
  color: var(--text-primary) !important;
  display: inline-block;
  margin-top: 4px;
}

.create-time {
  font-size: 12px;
  color: #999;
}

.card-checkbox {
  transform: scale(0.9);
  margin-right: auto;
}

.delete-button {
  padding: 0 8px;
  height: 24px;
  line-height: 22px;
  font-size: 12px;
}

.popup-content {
  padding: 16px;
}

.popup-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 16px 16px;
  border-bottom: 1px solid #eee;
}

.popup-title {
  font-size: 18px;
  font-weight: 600;
  color: #323233;
}

:deep(.van-card) {
  background: var(--header-background);
  color: var(--text-primary);
  padding: 10px 0;
}

:deep(.van-card__title) {
  font-size: 15px;
  font-weight: 500;
  margin-bottom: 4px;
}

:deep(.van-card__desc) {
  margin-top: 4px;
  color: var(--text-primary) !important;
  font-size: 13px;
  line-height: 1.4;
}

:deep(.van-tag) {
  margin-top: 6px;
  font-size: 11px;
}

:deep(.van-card__content) {
  padding-left: 8px;
}

:deep(.van-cell-group--inset) {
  margin: 0 12px 12px;
}

:deep(.van-cell-group--inset:last-child) {
  margin-bottom: 0;
}

.sort-button {
  height: 32px;
  padding: 0 12px;
}

:deep(.van-list) {
  min-height: 100px;
}

/* PC端按钮样式 */
.action-button {
  height: 32px;
  border-radius: 6px;
  padding: 0 16px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition: all 0.3s;
}

.primary-button {
  background: #1890ff;
  border: none;
  box-shadow: 0 2px 0 rgba(24, 144, 255, 0.1);
}

.primary-button:hover {
  background: #40a9ff;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.2);
}

.primary-button:active {
  background: #096dd9;
  transform: translateY(0);
}

.delete-button {
  height: 28px;
  border-radius: 6px;
  padding: 0 12px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
}

.sort-button {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.sort-button .anticon {
  font-size: 14px;
}

/* 文本省略样式 */
.ellipsis-text {
  max-width: 150px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
}

/* PC端样式 */
.pc-container {
  background: var(--header-background);
  color: var(--text-primary);
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.06);
}

.search-and-button-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

/* 按钮统一样式 */
.action-button {
  height: 32px;
  border-radius: 8px;
  padding: 0 16px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition: all 0.3s ease;
  font-weight: 500;
}

.primary-button {
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

.sort-button {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  transition: all 0.3s ease;

  &:hover {
    color: #ff8e53;
    border-color: #ff8e53;
    background: #fff6f3;
  }
}

.delete-button {
  height: 28px;
  border-radius: 8px;
  padding: 0 12px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  font-weight: 500;
}

/* 表格样式 */
.table-section {
  background: var(--header-background);
  color: var(--text-primary);
  border-radius: 8px;
}

:deep(.custom-table) {
  .ant-table-thead > tr > th {
    background: #fafafa;
    font-weight: 500;
    color: #1f2937;
  }

  .ant-table-tbody > tr:hover > td {
    background: #fff6f3;
  }
}

/* 分页样式 */
.mz-antd-pagination {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

/* 搜索表单样式 */
:deep(.ant-form-inline) {
  .ant-form-item {
    margin-right: 16px;
  }

  .ant-input {
    border-radius: 8px;
  }
}

/* 移动端分页样式优化 */
.mobile-pagination {
  margin-top: 16px;
  padding: 12px;
  background: var(--header-background);
  color: var(--text-primary);
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.search-section {
  position: sticky;
  top: -12px;
  z-index: 100;
  background: var(--header-background);
  color: var(--text-primary);
  /* 抵消父元素的内边距 */
  margin: 0 -12px 12px;
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
  background: var(--header-background);
  color: var(--text-primary);
  cursor: pointer;
}

.page-size-selector .van-icon {
  font-size: 12px;
  color: #999;
}

.pagination-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

:deep(.van-search) {
  background: var(--header-background);
}

/* 分页器样式 */
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
  background: var(--header-background);
  color: var(--text-primary);
  border: 1px solid #ebedf0;
  font-weight: bold;
  min-width: 28px !important;
  height: 28px !important;
  line-height: 28px !important;
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

.action-buttons {
  display: flex;
  gap: 8px;
}

/* 处理按钮样式 */
.process-button {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 32px;
  min-width: 88px;
  border-radius: 6px;
  padding: 0 16px;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 2px 0 rgba(0, 0, 0, 0.02);
  background: #52c41a;
  border-color: #52c41a;
  color: white;

  &:hover {
    background: #73d13d;
    border-color: #73d13d;
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(82, 196, 26, 0.2);
  }

  &:active {
    background: #389e0d;
    border-color: #389e0d;
    transform: translateY(1px);
    box-shadow: none;
  }
}

/* 移动端处理按钮样式 */
.card-footer {
  .process-button {
    height: 28px;
    line-height: 26px;
    font-size: 13px;
    padding: 0 12px;
    border-radius: 4px;
    min-width: 72px;
  }

  .view-button {
    height: 28px;
    line-height: 26px;
    font-size: 13px;
    padding: 0 12px;
    border-radius: 4px;
    color: #1890ff;
    border-color: #1890ff;

    :deep(.anticon) {
      font-size: 14px;
      margin-right: 4px;
      vertical-align: -0.125em;
    }

    &:active {
      opacity: 0.8;
    }
  }
}

/* 移动端举报相关信息样式 */
.report-target {
  font-size: 13px;
  color: #666;
  margin-top: 4px;
  display: flex;
  align-items: center;
  gap: 6px;
}

/* 移动端详细信息展示 */
.report-info {
  margin-bottom: 8px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  margin-bottom: 4px;
  padding: 2px 0;
}

.info-label {
  color: #999;
  font-weight: normal;
  min-width: 60px;
  margin-right: 8px;
}

.info-value {
  color: #333;
  flex: 1;
  text-align: right;
  word-break: break-all;
}

/* 移动端举报截图样式 */
.report-screenshots {
  margin-top: 8px;
  display: flex;
  align-items: flex-start;
  gap: 6px;
}

.screenshot-label {
  font-size: 12px;
  color: #666;
  flex-shrink: 0;
}

.screenshot-list {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.screenshot-thumb {
  width: 40px;
  height: 40px;
  object-fit: cover;
  border-radius: 4px;
  border: 1px solid #eee;
  cursor: pointer;
}

.more-screenshots {
  font-size: 12px;
  color: #999;
  align-self: center;
}

.no-screenshots {
  margin-top: 8px;
  font-size: 12px;
  color: #999;
}

.report-basic-info {
  .report-name {
    font-size: 16px;
    font-weight: 500;
    color: #1a1a1a;
  }

  .report-account {
    font-size: 13px;
    color: #666;
    margin-top: 2px;
  }
}

/* 移动端举报标签样式 */
.report-target-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  margin-top: 8px;
  background: #f5f5f5;
  border-radius: 12px;
  font-size: 12px;
  color: #666;
}

.report-time {
  font-size: 12px;
  color: #999;
}
</style>
