<template>
  <!-- PC端界面 -->
  <div v-if="device === DEVICE_TYPE_ENUM.PC" class="love-board-manage">
    <div class="pc-header-banner">
      <div class="banner-content">
        <div class="title-group">
          <h2 class="page-title">恋爱板管理系统</h2>
          <p class="page-subtitle">Premium Love Board Content Management System</p>
        </div>
        <div class="header-stats">
          <div class="stat-item">
            <span class="stat-value">{{ pagination.total }}</span>
            <span class="stat-label">总恋爱板</span>
          </div>
        </div>
      </div>
    </div>

    <div class="pc-search-filter-minimal">
      <div class="filter-item search-input">
        <a-input
          v-model:value="searchParams.id"
          placeholder="搜索 ID..."
          @keyup.enter="loadData(false)"
          class="minimal-input"
        >
          <template #prefix><SearchOutlined /></template>
        </a-input>
      </div>
      <div class="filter-item search-input">
        <a-input
          v-model:value="searchParams.userId"
          placeholder="搜索用户 ID..."
          @keyup.enter="loadData(false)"
          class="minimal-input"
        >
          <template #prefix><UserOutlined /></template>
        </a-input>
      </div>
      <div class="filter-item">
        <a-select
          v-model:value="searchParams.status"
          placeholder="状态筛选"
          allow-clear
          class="minimal-select"
          @change="loadData(false)"
        >
          <a-select-option :value="1">已启用</a-select-option>
          <a-select-option :value="0">已禁用</a-select-option>
        </a-select>
      </div>
      <div class="filter-item actions">
        <a-button type="primary" @click="loadData(false)" class="glow-button">
          <template #icon><SearchOutlined /></template>
          查询
        </a-button>
        <a-button @click="onRefresh" class="refresh-button">
          <template #icon><SyncOutlined /></template>
        </a-button>
      </div>
    </div>

    <div class="pc-table-container">
      <a-table
        :columns="columns"
        :data-source="dataSource"
        :pagination="pagination"
        :loading="loading"
        @change="handleTableChange"
        class="premium-table"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'images'">
            <div class="pc-images-stack">
              <a-image :src="record.bgCover" class="stack-img bg" />
              <div class="couple-avatars">
                <a-image :src="record.manCover" class="stack-img circular" />
                <a-image :src="record.womanCover" class="stack-img circular" />
              </div>
            </div>
          </template>
          <template v-if="column.key === 'status'">
            <a-badge
              :status="record.status === 1 ? 'success' : 'error'"
              :text="record.status === 1 ? '运行中' : '已停止'"
            />
          </template>
          <template v-if="column.key === 'operation'">
            <div class="pc-actions">
              <a-button type="link" class="action-btn edit" @click="handleEdit(record)">
                <EditOutlined /> 编辑
              </a-button>
              <a-divider type="vertical" />
              <a-popconfirm title="确定删除此恋爱板？" @confirm="handleDelete(record)">
                <a-button type="link" danger class="action-btn delete">
                  <DeleteOutlined /> 删除
                </a-button>
              </a-popconfirm>
            </div>
          </template>
        </template>
      </a-table>
    </div>
  </div>

  <!-- 移动端界面 -->
  <div v-else class="love-board-manage-mobile">
    <div class="mobile-header-premium">
      <div class="header-blur-bg"></div>
      <div class="header-content">
        <h2 class="mobile-title">恋爱板管理</h2>
        <div class="mobile-header-actions">
          <SyncOutlined @click="onRefresh" class="refresh-icon" />
        </div>
      </div>
    </div>

    <!-- 移动端搜索区域 -->
    <div class="mobile-search-filter">
      <div class="search-bar-inner">
        <SearchOutlined class="search-icon" />
        <input
          v-model="searchParams.id"
          placeholder="搜索 ID..."
          @keyup.enter="loadData(false)"
          class="minimal-input"
        />
      </div>
      <!-- 筛选下拉菜单 -->
      <div class="mobile-dropdown-row">
        <van-dropdown-menu class="clean-dropdown">
          <van-dropdown-item v-model="searchParams.status" :options="statusOptions" @change="loadData(false)" />
        </van-dropdown-menu>
      </div>
    </div>

    <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
      <van-list
        v-model:loading="loading"
        :finished="finished"
        finished-text="已加载全部恋爱板"
        @load="onLoad"
        class="love-cards-list"
      >
        <div v-for="item in dataSource" :key="item.id" class="love-card-premium">
          <div class="card-bg-wrap">
            <van-image :src="item.bgCover" fit="cover" class="card-bg" />
            <div class="card-overlay"></div>
            <div class="status-pill" :class="{ 'active': item.status === 1 }">
              {{ item.status === 1 ? '运行中' : '停用' }}
            </div>
          </div>

          <div class="card-body">
            <div class="couple-row">
              <div class="person-box">
                <van-image :src="item.manCover" class="person-img" round />
                <span class="person-name">{{ item.manName || '未设置' }}</span>
              </div>
              <div class="heart-pulse-icon">❤️</div>
              <div class="person-box text-right">
                <span class="person-name">{{ item.womanName || '未设置' }}</span>
                <van-image :src="item.womanCover" class="person-img" round />
              </div>
            </div>

            <div class="timing-info">
              <CalendarOutlined class="info-icon" />
              <span>计时: {{ item.timing || '未开始' }}</span>
            </div>

            <div class="id-meta">
              <span class="id-tag">ID: {{ item.id }}</span>
              <span class="user-id">Owner: {{ item.userId }}</span>
            </div>

            <div class="card-actions-row">
              <van-button class="action-btn edit" @click="handleEdit(item)">
                <EditOutlined /> 编辑设置
              </van-button>
              <van-button class="action-btn delete" @click="handleDelete(item)">
                <DeleteOutlined /> 删除记录
              </van-button>
            </div>
          </div>
        </div>
      </van-list>

      <div class="mobile-pagination-box" v-if="pagination.total > 0">
        <div class="pagination-info">
          第 {{ searchParams.current }} / {{ Math.ceil(pagination.total / (searchParams.pageSize || 10)) }} 页
        </div>
        <div class="pagination-nav">
          <button
            class="nav-circle-btn"
            :disabled="searchParams.current === 1"
            @click="() => goPage(searchParams.current - 1)"
          >
            <LeftOutlined />
          </button>
          <button
            class="nav-circle-btn"
            :disabled="searchParams.current >= Math.ceil(pagination.total / (searchParams.pageSize || 10))"
            @click="() => goPage(searchParams.current + 1)"
          >
            <RightOutlined />
          </button>
        </div>
      </div>
    </van-pull-refresh>
  </div>

  <!-- 编辑弹窗 -->
  <a-modal
    v-model:visible="editModalVisible"
    title="编辑恋爱板"
    @ok="handleEditSubmit"
    :confirmLoading="editLoading"
    class="edit-modal"
  >
    <a-form :model="editForm" layout="vertical" class="edit-modal-form">
      <a-form-item label="状态">
        <a-switch v-model:checked="editForm.status" checked-children="启用" un-checked-children="禁用" />
      </a-form-item>
      <a-form-item label="男生昵称">
        <a-input v-model:value="editForm.manName" placeholder="请输入男生昵称" />
      </a-form-item>
      <a-form-item label="女生昵称">
        <a-input v-model:value="editForm.womanName" placeholder="请输入女生昵称" />
      </a-form-item>
      <a-form-item label="计时">
        <a-input v-model:value="editForm.timing" placeholder="请输入计时信息" />
      </a-form-item>
      <a-form-item label="倒计时标题">
        <a-input v-model:value="editForm.countdownTitle" placeholder="请输入倒计时标题" />
      </a-form-item>
      <a-form-item label="倒计时时间">
        <a-input v-model:value="editForm.countdownTime" placeholder="请输入倒计时时间" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive, computed } from 'vue'
import { message, type FormInstance } from 'ant-design-vue'
import {
  SearchOutlined,
  UserOutlined,
  SyncOutlined,
  EditOutlined,
  DeleteOutlined,
  CalendarOutlined,
  LeftOutlined,
  RightOutlined,
  HeartOutlined
} from '@ant-design/icons-vue'
import {
  listLoveBoardsByPageUsingPost,
  updateLoveBoardAdminUsingPost,
  batchOperationLoveBoardsUsingPost
} from '@/api/loveBoardController'
import { DEVICE_TYPE_ENUM } from '@/constants/device'
import { getDeviceType } from '@/utils/device'

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
    key: 'id',
    width: 80,
  },
  {
    title: '用户ID',
    dataIndex: 'userId',
    key: 'userId',
    width: 80,
  },
  {
    title: '图片预览',
    key: 'images',
    width: 200,
  },
  {
    title: '男生昵称',
    dataIndex: 'manName',
    key: 'manName',
    width: 120,
  },
  {
    title: '女生昵称',
    dataIndex: 'womanName',
    key: 'womanName',
    width: 120,
  },
  {
    title: '计时',
    dataIndex: 'timing',
    key: 'timing',
    width: 120,
  },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    width: 80,
    render: (status: number) => (status ? '启用' : '禁用'),
  },
  {
    title: '操作',
    key: 'operation',
    fixed: 'right',
    width: 200,
  },
]

// 状态选项
const statusOptions = [
  { text: '全部状态', value: '' },
  { text: '已启用', value: 1 },
  { text: '已禁用', value: 0 },
]

// 搜索参数
const searchParams = ref({
  id: undefined,
  userId: undefined,
  status: '', // 匹配 statusOptions 的 '全部状态'
  current: 1,
  pageSize: 10,
})

// 表格数据
const dataSource = ref([])
const loading = ref(false)
const pagination = ref({
  current: 1,
  pageSize: 10,
  total: 0,
})

// 加载数据
const loadData = async (append = false) => {
  loading.value = true
  try {
    const res = await listLoveBoardsByPageUsingPost({
      ...searchParams.value,
      sortField: 'createTime',
      sortOrder: 'descend',
    })
    if (res.data.code === 0 && res.data.data) {
      const records = Array.isArray(res.data.data.records) ? res.data.data.records : []
      pagination.value.total = Number(res.data.data.total || 0)
      if (append) {
        dataSource.value = [...dataSource.value, ...records]
      } else {
        dataSource.value = records
      }
      finished.value = dataSource.value.length >= pagination.value.total || records.length < (searchParams.value.pageSize || 10)
    }
  } catch (error) {
    console.error('加载数据失败:', error)
    message.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

// 选择行
const selectedRowKeys = ref([])
const onSelectChange = (keys: string[]) => {
  selectedRowKeys.value = keys
}
const onSelectClear = () => {
  selectedRowKeys.value = []
}

// 表格变化处理
const handleTableChange = (pag: any) => {
  pagination.value.current = pag.current
  searchParams.value.current = pag.current
  loadData()
}

// 编辑相关
const editModalVisible = ref(false)
const editLoading = ref(false)
const editForm = ref({
  id: undefined,
  status: true,
  manName: '',
  womanName: '',
  timing: '',
  countdownTitle: '',
  countdownTime: '',
})

// 编辑处理
const handleEdit = (record: any) => {
  Object.assign(editForm.value, record)
  editModalVisible.value = true
}

// 提交编辑
const handleEditSubmit = async () => {
  editLoading.value = true
  try {
    await updateLoveBoardAdminUsingPost(editForm.value)
    message.success('更新成功')
    editModalVisible.value = false
    loadData()
  } catch (error) {
    console.error('更新失败:', error)
    message.error('更新失败')
  } finally {
    editLoading.value = false
  }
}

// 删除处理
const handleDelete = async (record: any) => {
  try {
    await batchOperationLoveBoardsUsingPost({
      ids: [record.id],
      operation: 'delete',
    })
    message.success('删除成功')
    loadData()
  } catch (error) {
    console.error('删除失败:', error)
    message.error('删除失败')
  }
}

// 移动端相关
const refreshing = ref(false)
const finished = ref(false)

const onRefresh = async () => {
  searchParams.value.current = 1
  finished.value = false
  dataSource.value = []
  await loadData(false)
  refreshing.value = false
}

const onLoad = async () => {
  if (!loading.value && !finished.value) {
    searchParams.value.current++
    await loadData(true)
  }
}

const goPage = async (page: number) => {
  const totalPages = Math.max(1, Math.ceil((pagination.value.total || 0) / (searchParams.value.pageSize || 10)))
  const target = Math.min(Math.max(1, page), totalPages)
  searchParams.value.current = target
  finished.value = false
  dataSource.value = []
  await loadData(false)
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
/* =========================================
   通用变量与基础样式
   ========================================= */
:deep(.ant-input), :deep(.ant-input-number), :deep(.ant-select-selector) {
  border-radius: 10px !important;
  border-color: #eee !important;
  transition: all 0.3s ease;
}

:deep(.ant-input:focus), :deep(.ant-input-number-focused), :deep(.ant-select-selector:focus) {
  border-color: var(--primary-color) !important;
  box-shadow: 0 0 0 2px rgba(var(--primary-rgb), 0.1) !important;
}

/* =========================================
   PC 端样式 (Premium Dashboard)
   ========================================= */
.love-board-manage {
  padding: 24px;
  background: #f8fafc;
  min-height: 100vh;
}

/* 页头横幅 */
.pc-header-banner {
  background: linear-gradient(135deg, #1890ff 0%, #0050b3 100%);
  border-radius: 20px;
  padding: 40px;
  color: white;
  margin-bottom: 24px;
  box-shadow: 0 10px 30px rgba(24, 144, 255, 0.15);
  position: relative;
  overflow: hidden;
}

.pc-header-banner::after {
  content: "";
  position: absolute;
  top: -50%;
  right: -10%;
  width: 300px;
  height: 300px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 50%;
  filter: blur(50px);
}

.banner-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: relative;
  z-index: 1;
}

.page-title {
  font-size: 32px;
  font-weight: 800;
  margin: 0;
  letter-spacing: -0.5px;
  color: white;
}

.page-subtitle {
  font-size: 16px;
  opacity: 0.8;
  margin: 8px 0 0 0;
}

.header-stats {
  display: flex;
  gap: 32px;
}

.stat-item {
  text-align: right;
  background: rgba(255, 255, 255, 0.15);
  padding: 12px 24px;
  border-radius: 16px;
  backdrop-filter: blur(10px);
}

.stat-value {
  display: block;
  font-size: 28px;
  font-weight: 700;
  line-height: 1;
}

.stat-label {
  font-size: 12px;
  opacity: 0.9;
  text-transform: uppercase;
  letter-spacing: 1px;
}

/* 简约筛选区 */
.pc-search-filter-minimal {
  display: flex;
  align-items: center;
  gap: 16px;
  background: white;
  padding: 20px 24px;
  border-radius: 16px;
  margin-bottom: 24px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.03);
}

.filter-item {
  display: flex;
  align-items: center;
}

.filter-item.search-input {
  width: 220px;
}

.minimal-input, .minimal-select {
  width: 100%;
}

.glow-button {
  height: 40px;
  padding: 0 24px;
  border-radius: 12px;
  font-weight: 500;
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.25);
}

.refresh-button {
  width: 40px;
  height: 40px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  background: #f0f2f5;
  border: none;
  color: #666;
}

/* 高端表格 */
.pc-table-container {
  background: white;
  border-radius: 20px;
  padding: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.02);
}

.premium-table :deep(.ant-table) {
  background: transparent;
}

.premium-table :deep(.ant-table-thead > tr > th) {
  background: #fafafa;
  font-weight: 600;
  border-bottom: 1px solid #f0f0f0;
}

/* 图片堆叠效果 */
.pc-images-stack {
  display: flex;
  align-items: center;
  gap: 15px;
}

.stack-img {
  border: 4px solid #fff;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s ease;
}

.stack-img.bg {
  width: 80px;
  height: 50px;
  border-radius: 8px;
}

.couple-avatars {
  display: flex;
  margin-left: -5px;
}

.stack-img.circular {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  margin-left: -12px;
}

.pc-actions {
  display: flex;
  align-items: center;
}

.action-btn {
  font-size: 14px;
  padding: 4px 8px;
}

/* =========================================
   移动端样式 (Minimalist Card Style)
   ========================================= */
.love-board-manage-mobile {
  padding-bottom: 100px;
  background: #f6f8fa;
  min-height: 100vh;
}

/* 精致头部 */
.mobile-header-premium {
  position: sticky;
  top: 0;
  z-index: 100;
  padding: 16px 20px;
  overflow: hidden;
}

.header-blur-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(20px);
}

.header-content {
  position: relative;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.mobile-title {
  font-size: 20px;
  font-weight: 800;
  margin: 0;
  color: #1a1a1a;
}

.refresh-icon {
  font-size: 18px;
  color: #666;
}

/* 移动端搜索区域 */
.mobile-search-filter {
  margin: 16px;
  padding: 18px;
  background: #fff;
  border-radius: 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
}

.search-bar-inner {
  display: flex;
  align-items: center;
  background: #f5f5f5;
  padding: 10px 14px;
  border-radius: 12px;
  margin-bottom: 12px;
}

.search-icon {
  color: #999;
  margin-right: 10px;
}

.mobile-search-filter .minimal-input {
  border: none;
  background: transparent;
  flex: 1;
  font-size: 14px;
  outline: none;
}

/* 筛选下拉菜单同步样式 */
.mobile-dropdown-row {
  display: flex;
  gap: 10px;
}

.clean-dropdown {
  flex: 1;
  height: 36px;
}

:deep(.van-dropdown-menu__bar) {
  background: transparent;
  box-shadow: none;
  height: 36px;
}

:deep(.van-dropdown-menu__title) {
  background: #f0f0f0;
  padding: 0 16px;
  border-radius: 10px;
  font-size: 13px;
  color: #262626 !important;
  font-weight: 500;
}

:deep(.van-dropdown-item__option) {
  .van-cell__title {
    color: #262626 !important;
    font-weight: 500;
  }
}

/* 列表容器 */
.love-cards-list {
  padding: 0 16px;
}

/* 恋爱板卡片 */
.love-card-premium {
  background: #fff;
  border-radius: 24px;
  overflow: hidden;
  margin-bottom: 20px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.04);
  border: 1px solid #f0f0f0;
}

.card-bg-wrap {
  position: relative;
  height: 120px;
}

.card-bg {
  width: 100%;
  height: 100%;
}

.card-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(to bottom, transparent, rgba(0,0,0,0.4));
}

.status-pill {
  position: absolute;
  top: 15px;
  right: 15px;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 700;
  background: rgba(0, 0, 0, 0.4);
  color: #fff;
  backdrop-filter: blur(5px);
}

.status-pill.active {
  background: rgba(82, 196, 26, 0.85);
}

/* 卡片主体 */
.card-body {
  padding: 20px;
}

.couple-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
}

.person-box {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
}

.person-box.text-right {
  justify-content: flex-end;
}

.person-img {
  width: 44px;
  height: 44px;
  border: 3px solid #fff;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1);
}

.person-name {
  font-size: 14px;
  font-weight: 700;
  color: #1a1a1a;
  max-width: 70px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.heart-pulse-icon {
  font-size: 20px;
  animation: heartBeat 1.5s infinite;
}

@keyframes heartBeat {
  0% { transform: scale(1); }
  15% { transform: scale(1.2); }
  30% { transform: scale(1); }
  45% { transform: scale(1.15); }
  60% { transform: scale(1); }
}

.timing-info {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #f8fafc;
  padding: 10px 14px;
  border-radius: 12px;
  color: #64748b;
  font-size: 13px;
  margin-bottom: 14px;
}

.info-icon {
  color: #94a3b8;
}

.id-meta {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: #94a3b8;
  margin-bottom: 18px;
  padding: 0 4px;
}

.card-actions-row {
  display: flex;
  gap: 10px;
}

.card-actions-row .action-btn {
  flex: 1;
  height: 38px;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 600;
  border: none;
}

.action-btn.edit {
  background: #f1f5f9;
  color: #475569;
}

.action-btn.delete {
  background: #fff1f0;
  color: #f5222d;
}

/* 移动端分页 */
.mobile-pagination-box {
  margin: 30px 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.pagination-info {
  font-size: 13px;
  color: #94a3b8;
  margin-bottom: 16px;
  font-weight: 500;
}

.pagination-nav {
  display: flex;
  gap: 24px;
}

.nav-circle-btn {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  border: 1px solid #e8e8e8;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  color: #1a1a1a;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.05);
  transition: all 0.2s ease;
}

.nav-circle-btn:active {
  transform: scale(0.9);
  background: #fafafa;
}

.nav-circle-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

/* 编辑弹窗优化 */
:deep(.ant-modal-content) {
  border-radius: 24px !important;
  overflow: hidden;
}

:deep(.ant-modal-header) {
  padding: 24px 24px 16px !important;
  border-bottom: none !important;
}

:deep(.ant-modal-body) {
  padding: 16px 24px 32px !important;
}

/* 响应式微调 */
@media (max-width: 640px) {
  .pc-header-banner {
    padding: 24px;
  }
}
</style>

