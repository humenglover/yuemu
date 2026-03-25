<template>
  <div class="friend-link-manage">
    <!-- PC端展示 -->
    <template v-if="device === DEVICE_TYPE_ENUM.PC">
      <div class="pc-container">
        <div class="pc-header-premium">
          <div class="header-left">
            <h1 class="premium-title">友链管理</h1>
            <p class="premium-subtitle">管理、维护全站合作伙伴与友链申请</p>
          </div>
          <div class="header-right-actions">
            <a-button type="primary" class="btn-add-premium" @click="showAddModal">
              <PlusSquareOutlined /> 添加友链
            </a-button>
            <a-button class="btn-refresh-premium" @click="handleRefreshCache">
              <CloudSyncOutlined /> 刷新内存
            </a-button>
          </div>
        </div>

        <!-- 搜索和筛选区域 -->
        <div class="pc-search-filter-minimal">
          <div class="filter-item search-input">
            <a-input
              v-model:value="searchParams.siteName"
              placeholder="搜索网站名称..."
              allowClear
              @change="handleSearch"
            >
              <template #prefix><SearchOutlined /></template>
            </a-input>
          </div>
          <div class="filter-item">
            <a-select
              v-model:value="searchParams.siteType"
              placeholder="网站类型"
              class="minimal-select"
              allowClear
              @change="handleSearch"
            >
              <a-select-option v-for="type in siteTypes" :key="type.value" :value="type.value">
                {{ type.name }}
              </a-select-option>
            </a-select>
          </div>
          <div class="filter-item">
            <a-select
              v-model:value="searchParams.status"
              placeholder="审核状态"
              class="minimal-select"
              allowClear
              @change="handleSearch"
            >
              <a-select-option :value="1">已审核</a-select-option>
              <a-select-option :value="0">待审核</a-select-option>
              <a-select-option :value="2">已拒绝</a-select-option>
            </a-select>
          </div>
        </div>

        <!-- 友链列表 -->
        <div class="friend-link-list">
          <a-table
            :dataSource="friendLinks"
            :columns="columns"
            :loading="loading"
            :pagination="pagination"
            @change="handleTableChange"
            :scroll="{ x: 1000 }"
          >
            <!-- 表格列插槽 -->
            <template #siteLogo="{ text }">
              <a-avatar :src="text" shape="square" :size="40" />
            </template>
            <template #siteUrl="{ text }">
              <a :href="text" target="_blank" rel="noopener noreferrer">{{ text }}</a>
            </template>
            <template #status="{ text }">
              <a-tag :color="text === 1 ? 'success' : 'warning'">
                {{ text === 1 ? '已审核' : text === 2 ? '已拒绝' : '待审核' }}
              </a-tag>
            </template>
            <template #action="{ record }">
              <div class="action-buttons">
                <a-button type="link" @click="showEditModal(record)">
                  <EditOutlined /> 编辑
                </a-button>
                <template v-if="record.status === 0">
                  <a-button type="link" @click="handleReview(record)">
                    <CheckOutlined /> 审核
                  </a-button>
                </template>
                <template v-else-if="record.status === 1">
                  <a-button type="link" danger @click="handleReview(record)">
                    <CloseOutlined /> 不通过
                  </a-button>
                </template>
                <template v-else>
                  <a-button type="link" @click="handleReview(record)">
                    <CheckOutlined /> 通过
                  </a-button>
                </template>
                <a-button type="link" @click="showWeightModal(record)">
                  <ColumnHeightOutlined /> 权重
                </a-button>
                <a-popconfirm
                  title="确定要删除这个友链吗？"
                  @confirm="handleDelete(record)"
                  okText="确定"
                  cancelText="取消"
                >
                  <a-button type="link" danger>
                    <DeleteOutlined /> 删除
                  </a-button>
                </a-popconfirm>
              </div>
            </template>
          </a-table>
        </div>
      </div>

    </template>

    <!-- 移动端展示 -->
    <template v-else>
      <div class="mobile-container-premium">
        <!-- 移动端头部 -->
        <div class="mobile-header-bar">
          <h2 class="mobile-title">伙伴管理</h2>
          <div class="mobile-header-actions">
            <a-button type="primary" size="small" shape="circle" class="btn-pulse" @click="showAddModal">
              <PlusOutlined />
            </a-button>
            <a-button size="small" shape="circle" class="btn-ghost" @click="handleRefreshCache">
              <CloudSyncOutlined />
            </a-button>
          </div>
        </div>

        <!-- 移动端搜索区域 -->
        <div class="mobile-search-filter">
          <div class="search-bar-inner">
            <SearchOutlined class="search-icon" />
            <input
              v-model="searchParams.siteName"
              placeholder="搜索网站名称..."
              @keyup.enter="handleSearch"
              class="minimal-input"
            />
          </div>
          <!-- 筛选下拉菜单 -->
          <div class="mobile-dropdown-row">
            <van-dropdown-menu class="clean-dropdown">
              <van-dropdown-item
                v-model="searchParams.siteType"
                :options="mobileTypeOptions"
                @change="handleSearch"
              />
              <van-dropdown-item
                v-model="searchParams.status"
                :options="mobileStatusOptions"
                @change="handleSearch"
              />
            </van-dropdown-menu>
          </div>
        </div>

        <!-- 移动端列表 -->

        <van-list
          v-model:loading="loading"
          :finished="finished"
          finished-text="没有更多了"
          @load="onLoadMore"
        >
          <div class="friend-cards-list">
            <div v-for="item in friendLinks" :key="item.id" class="friend-card-premium">
              <div class="card-glow"></div>
              <div class="card-main">
                <div class="card-header">
                  <van-image
                    :src="item.siteLogo"
                    width="44"
                    height="44"
                    radius="12"
                    fit="cover"
                    class="site-logo-shadow"
                  >
                    <template #error><van-icon name="photo-o" /></template>
                  </van-image>
                  <div class="name-desc">
                    <div class="site-name-top">{{ item.siteName }}</div>
                    <div class="site-type-tag">{{ item.siteType }}</div>
                  </div>
                  <van-tag
                    :color="getStatusColor(item.status)"
                    class="status-badge"
                  >
                    {{ getStatusText(item.status) }}
                  </van-tag>
                </div>

                <div class="card-body" @click="openUrl(item.siteUrl)">
                  <div class="site-url-box">
                    <LinkOutlined /> <span>{{ item.siteUrl }}</span>
                  </div>
                  <div class="site-description">{{ item.siteDesc || '暂无描述' }}</div>

                  <div class="owner-contacts">
                    <div class="contact-item">
                      <UserOutlined /> {{ item.ownerName }}
                    </div>
                    <div class="contact-item">
                      <MessageOutlined /> {{ item.ownerContact }}
                    </div>
                  </div>
                </div>

                <div class="card-footer-actions">
                  <div class="weight-info">
                    权重 <span>{{ item.weight }}</span>
                  </div>
                  <div class="actions-group">
                    <template v-if="item.status === 0">
                      <a-button type="primary" size="small" class="btn-action-sm btn-approve" @click="handleReview(item)">审核</a-button>
                    </template>
                    <template v-else>
                      <a-button
                        size="small"
                        :danger="item.status === 1"
                        class="btn-action-sm"
                        @click="handleReview(item)"
                      >
                        {{ item.status === 1 ? '弃用' : '激活' }}
                      </a-button>
                    </template>
                    <a-dropdown :trigger="['click']">
                      <a-button size="small" class="btn-more-sm">更多</a-button>
                      <template #overlay>
                        <a-menu>
                          <a-menu-item @click="showEditModal(item)"><EditOutlined /> 编辑信息</a-menu-item>
                          <a-menu-item @click="showWeightModal(item)"><ColumnHeightOutlined /> 权重调整</a-menu-item>
                          <a-menu-item danger @click="showDeleteConfirm(item)"><DeleteOutlined /> 彻底删除</a-menu-item>
                        </a-menu>
                      </template>
                    </a-dropdown>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </van-list>

        <!-- 移动端高级分页器 -->
        <div class="mobile-pagination-box">
          <div class="pagination-info">
            第 {{ pagination.current }} / {{ Math.ceil(total / pagination.pageSize) }} 页
          </div>
          <div class="pagination-nav">
            <button
              class="nav-circle-btn"
              :disabled="pagination.current <= 1"
              @click="handlePrevPage"
            >
              <LeftOutlined />
            </button>
            <button
              class="nav-circle-btn"
              :disabled="pagination.current >= Math.ceil(total / pagination.pageSize)"
              @click="handleNextPage"
            >
              <RightOutlined />
            </button>
          </div>
        </div>
      </div>
    </template>

    <!-- 通用模态框组件 -->
    <template v-if="device === DEVICE_TYPE_ENUM.PC">
      <a-modal
        v-model:visible="modalVisible"
        :title="modalMode === 'add' ? '添加友链' : '编辑友链'"
        @ok="handleModalOk"
        @cancel="handleModalCancel"
        :confirmLoading="modalLoading"
      >
        <a-form :model="formData" :rules="rules" ref="formRef">
          <a-form-item label="网站名称" name="siteName">
            <a-input v-model:value="formData.siteName" placeholder="请输入网站名称" />
          </a-form-item>
          <a-form-item label="网站链接" name="siteUrl">
            <a-input v-model:value="formData.siteUrl" placeholder="请输入网站链接" />
          </a-form-item>
          <a-form-item label="网站Logo" name="siteLogo">
            <a-input v-model:value="formData.siteLogo" placeholder="请输入Logo链接" />
          </a-form-item>
          <a-form-item label="网站描述" name="siteDesc">
            <a-textarea v-model:value="formData.siteDesc" placeholder="请输入网站描述" />
          </a-form-item>
          <a-form-item label="站长名称" name="ownerName">
            <a-input v-model:value="formData.ownerName" placeholder="请输入站长名称" />
          </a-form-item>
          <a-form-item label="联系方式" name="ownerContact">
            <a-input v-model:value="formData.ownerContact" placeholder="请输入联系方式" />
          </a-form-item>
          <a-form-item label="网站类型" name="siteType">
            <a-select v-model:value="formData.siteType" placeholder="请选择网站类型">
              <a-select-option v-for="type in siteTypes" :key="type.value" :value="type.value">
                {{ type.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- PC端权重设置模态框 -->
      <a-modal
        v-model:visible="weightModalVisible"
        title="设置权重"
        @ok="handleWeightModalOk"
        @cancel="handleWeightModalCancel"
        :confirmLoading="weightModalLoading"
      >
        <a-form :model="weightForm">
          <a-form-item label="权重值">
            <a-input-number
              v-model:value="weightForm.weight"
              :min="0"
              :max="100"
              style="width: 100%"
            />
          </a-form-item>
        </a-form>
      </a-modal>
    </template>

    <!-- 移动端弹窗组件 -->
    <template v-else>
      <!-- 移动端添加/编辑表单 -->
      <van-dialog
        v-model:show="modalVisible"
        :title="modalMode === 'add' ? '添加友链' : '编辑友链'"
        show-cancel-button
        :before-close="handleMobileDialogClose"
        :loading="modalLoading"
      >
        <van-form ref="mobileFormRef">
          <van-cell-group inset>
            <van-field
              v-model="formData.siteName"
              label="网站名称"
              placeholder="请输入网站名称"
              :rules="[{ required: true, message: '请输入网站名称' }]"
            />
            <van-field
              v-model="formData.siteUrl"
              label="网站链接"
              placeholder="请输入网站链接"
              :rules="[{ required: true, message: '请输入网站链接' }]"
            />
            <van-field
              v-model="formData.siteLogo"
              label="网站Logo"
              placeholder="请输入Logo链接"
            >
              <template #right-icon>
                <van-image
                  v-if="formData.siteLogo"
                  :src="formData.siteLogo"
                  width="24"
                  height="24"
                  fit="cover"
                />
              </template>
            </van-field>
            <van-field
              v-model="formData.siteDesc"
              label="网站描述"
              type="textarea"
              placeholder="请输入网站描述"
              rows="3"
              autosize
            />
            <van-field
              v-model="formData.ownerName"
              label="站长名称"
              placeholder="请输入站长名称"
            />
            <van-field
              v-model="formData.ownerContact"
              label="联系方式"
              placeholder="请输入联系方式"
            />
            <van-field
              v-model="formData.siteType"
              is-link
              readonly
              label="网站类型"
              placeholder="请选择网站类型"
              @click="showSiteTypePicker = true"
              :rules="[{ required: true, message: '请选择网站类型' }]"
            />
          </van-cell-group>
        </van-form>
      </van-dialog>

      <!-- 移动端网站类型选择器 -->
      <van-popup
        v-model:show="showSiteTypePicker"
        round
        position="bottom"
      >
        <van-picker
          :columns="siteTypes.map(type => ({ text: type.name, value: type.value }))"
          @confirm="onSiteTypeConfirm"
          @cancel="showSiteTypePicker = false"
          show-toolbar
          title="选择网站类型"
        />
      </van-popup>

      <!-- 移动端权重设置 -->
      <van-dialog
        v-model:show="weightModalVisible"
        title="设置权重"
        show-cancel-button
        :before-close="handleMobileWeightDialogClose"
        :loading="weightModalLoading"
      >
        <van-cell-group inset>
          <van-field
            v-model="weightForm.weight"
            label="权重值"
            type="number"
            :rules="[
              { required: true, message: '请输入权重值' },
              { validator: validateWeight, message: '权重值必须在0-100之间' }
            ]"
          >
            <template #input>
              <van-stepper
                v-model="weightForm.weight"
                :min="0"
                :max="100"
                integer
              />
            </template>
          </van-field>
        </van-cell-group>
      </van-dialog>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive, computed, h, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { Modal, message } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import {
  ReloadOutlined,
  ColumnHeightOutlined,
  CloseOutlined,
  PlusSquareOutlined,
  CloudSyncOutlined,
  LinkOutlined,
  UserOutlined,
  MessageOutlined,
  LeftOutlined,
  RightOutlined,
} from '@ant-design/icons-vue'
import type { TablePaginationConfig } from 'ant-design-vue'
import { getDeviceType } from '@/utils/device'
import { DEVICE_TYPE_ENUM } from '@/constants/device'
import type { FriendLink, SiteType } from '@/api/types'
import {
  listFriendLinksByPageUsingGet,
  addFriendLinkUsingPost,
  updateFriendLinkUsingPost,
  deleteFriendLinkUsingPost,
  reviewFriendLinkUsingPost,
  updateWeightUsingPost,
  refreshCacheUsingPost,
  listSiteTypesUsingGet
} from '@/api/friendLinkController'
import { formatTime } from '@/utils/dateUtils'

const router = useRouter()
const device = ref<string>('')

// 状态转换工具
const getStatusColor = (status: number | undefined) => {
  switch (status) {
    case 1: return '#10b981' // 已审核 (Emerald 500)
    case 2: return '#ef4444' // 已拒绝 (Red 500)
    default: return '#f59e0b' // 待审核 (Amber 500)
  }
}

const getStatusText = (status: number | undefined) => {
  switch (status) {
    case 1: return '已审核'
    case 2: return '已拒绝'
    default: return '待审核'
  }
}

// 外部链接跳转
const openUrl = (url: string) => {
  if (url) window.open(url, '_blank')
}

// 获取设备类型
onMounted(async () => {
  device.value = await getDeviceType()
})

// 表格列定义
const columns = [
  {
    title: 'Logo',
    dataIndex: 'siteLogo',
    key: 'siteLogo',
    slots: { customRender: 'siteLogo' },
    width: 80
  },
  {
    title: '网站名称',
    dataIndex: 'siteName',
    key: 'siteName',
    width: 160,
    ellipsis: true
  },
  {
    title: '网站链接',
    dataIndex: 'siteUrl',
    key: 'siteUrl',
    slots: { customRender: 'siteUrl' },
    width: 200,
    ellipsis: true
  },
  {
    title: '网站类型',
    dataIndex: 'siteType',
    key: 'siteType',
    width: 120,
    ellipsis: true
  },
  {
    title: '权重',
    dataIndex: 'weight',
    key: 'weight',
    sorter: true,
    width: 80
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    key: 'createTime',
    width: 160,
    customRender: ({ text }) => formatTime(text, 'full')
  },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    slots: { customRender: 'status' },
    width: 100
  },
  {
    title: '操作',
    key: 'action',
    fixed: 'right',
    width: 240,
    slots: { customRender: 'action' }
  }
]

// 数据状态
const loading = ref<boolean>(false)
const finished = ref<boolean>(false)
const friendLinks = ref<FriendLink[]>([])
const siteTypes = ref<SiteType[]>([])
const total = ref<number>(0)

// 移动端选项
interface MobileOption {
  text: string;
  value: string | number | undefined;
}

const mobileTypeOptions = computed<MobileOption[]>(() => [
  { text: '全部类型', value: '' },
  ...siteTypes.value.map(type => ({
    text: type.name || '',
    value: type.value || ''
  }))
])

const mobileStatusOptions = [
  { text: '全部状态', value: '' },
  { text: '已审核', value: 1 },
  { text: '待审核', value: 0 },
  { text: '已拒绝', value: 2 }
]

// 分页配置
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true
} as {
  current: number;
  pageSize: number;
  total: number;
  showSizeChanger: boolean;
  showQuickJumper: boolean;
})

// 搜索参数
const searchParams = reactive({
  siteName: '',
  siteType: '', // 匹配 mobileTypeOptions 的 '全部类型'
  status: '',   // 匹配 mobileStatusOptions 的 '全部状态'
  orderBy: undefined as string | undefined
})

// 表单相关
const modalVisible = ref(false)
const modalMode = ref<'add' | 'edit'>('add')
const modalLoading = ref(false)
const formRef = ref<FormInstance>()

const formData = reactive({
  id: undefined as number | undefined,
  siteName: '',
  siteUrl: '',
  siteLogo: '',
  siteDesc: '',
  ownerName: '',
  ownerContact: '',
  siteType: undefined as string | undefined
})

// 权重设置相关
const weightModalVisible = ref(false)
const weightModalLoading = ref(false)
const weightForm = reactive({
  id: undefined,
  weight: 0
})

// 表单验证规则
const rules = {
  siteName: [{ required: true, message: '请输入网站名称', trigger: 'blur' }],
  siteUrl: [{ required: true, message: '请输入网站链接', trigger: 'blur' }],
  siteType: [{ required: true, message: '请选择网站类型', trigger: 'change' }]
}


// 初始化
onMounted(async () => {
  await Promise.all([fetchFriendLinks(), fetchSiteTypes()])
})

// 获取友链列表
const fetchFriendLinks = async () => {
  loading.value = true
  try {
    const res = await listFriendLinksByPageUsingGet({
      current: pagination.current,
      pageSize: pagination.pageSize,
      ...searchParams
    })
    if (res.data?.code === 0 && res.data?.data) {
      // 如果是移动端加载更多，追加数据
      if (device.value !== DEVICE_TYPE_ENUM.PC && pagination.current > 1) {
        friendLinks.value = [...friendLinks.value, ...res.data.data.records]
      } else {
        // PC端或移动端首次加载/刷新，直接替换数据
        friendLinks.value = res.data.data.records || []
      }
      total.value = res.data.data.total || 0
      pagination.total = total.value
      finished.value = !res.data.data.records?.length ||
        (pagination.current * pagination.pageSize >= total.value)
    }
  } catch (err) {
    message.error('获取友链列表失败')
    console.error('获取友链列表失败:', err)
  } finally {
    loading.value = false
  }
}

// 获取网站类型
const fetchSiteTypes = async () => {
  try {
    const res = await listSiteTypesUsingGet()
    if (res.data?.code === 0 && res.data?.data) {
      // 将网站类型的value转换为大写
      siteTypes.value = res.data.data.map(type => ({
        name: type.name,
        value: type.value.toUpperCase()
      }))
    }
  } catch (error) {
    message.error('获取网站类型失败')
    console.error('获取网站类型失败:', error)
  }
}

// 表格变化处理
const handleTableChange = (pag: TablePaginationConfig, filters: any, sorter: any) => {
  pagination.current = pag.current || 1
  pagination.pageSize = pag.pageSize || 10
  if (sorter.field === 'weight') {
    searchParams.orderBy = `weight ${sorter.order === 'ascend' ? 'asc' : 'desc'}`
  } else {
    searchParams.orderBy = undefined
  }
  fetchFriendLinks()
}

// 搜索处理
const handleSearch = () => {
  pagination.current = 1
  fetchFriendLinks()
}

// 显示添加模态框
const showAddModal = () => {
  modalMode.value = 'add'
  Object.assign(formData, {
    id: undefined,
    siteName: '',
    siteUrl: '',
    siteLogo: '',
    siteDesc: '',
    ownerName: '',
    ownerContact: '',
    siteType: undefined
  })
  modalVisible.value = true
}

// 显示编辑模态框
const showEditModal = (record: any) => {
  modalMode.value = 'edit'
  Object.assign(formData, record)
  modalVisible.value = true
}

// 模态框确认
const handleModalOk = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
    modalLoading.value = true
    const api = modalMode.value === 'add' ? addFriendLinkUsingPost : updateFriendLinkUsingPost
    const res = await api(formData)
    if (res.data?.code === 0) {
      message.success(`${modalMode.value === 'add' ? '添加' : '编辑'}友链成功`)
      modalVisible.value = false
      await fetchFriendLinks()
    }
  } catch (err) {
    console.error(err)
  } finally {
    modalLoading.value = false
  }
}

// 模态框取消
const handleModalCancel = () => {
  modalVisible.value = false
  formRef.value?.resetFields()
}

// 删除友链
const handleDelete = async (record: FriendLink) => {
  if (!record.id) return
  try {
    const res = await deleteFriendLinkUsingPost({ id: record.id })
    if (res.data?.code === 0) {
      message.success('删除友链成功')
      await fetchFriendLinks()
    }
  } catch (err) {
    message.error('删除友链失败')
    console.error('删除友链失败:', err)
  }
}

// 审核友链
const handleReview = async (item: FriendLink) => {
  // 如果当前是已审核状态，直接拒绝
  if (item.status === 1) {
    try {
      const res = await reviewFriendLinkUsingPost({
        id: item.id,
        status: 2
      })
      if (res.data?.code === 0) {
        message.success('已拒绝')
        await fetchFriendLinks()
      } else {
        message.error(res.data?.message || '操作失败')
      }
    } catch (err) {
      console.error('审核失败:', err)
      message.error('审核失败')
    }
    return
  }

  // 如果是待审核或已拒绝状态，直接通过
  if (item.status === 0 || item.status === 2) {
    try {
      const res = await reviewFriendLinkUsingPost({
        id: item.id,
        status: 1
      })
      if (res.data?.code === 0) {
        message.success('审核通过')
        await fetchFriendLinks()
      } else {
        message.error(res.data?.message || '操作失败')
      }
    } catch (err) {
      console.error('审核失败:', err)
      message.error('审核失败')
    }
    return
  }
}

// 显示权重设置模态框
const showWeightModal = (record: any) => {
  weightForm.id = record.id
  weightForm.weight = Number(record.weight)
  weightModalVisible.value = true
}

// 权重设置确认
const handleWeightModalOk = async () => {
  try {
    weightModalLoading.value = true
    const res = await updateWeightUsingPost({
      id: weightForm.id!,
      weight: weightForm.weight
    })
    if (res.data?.code === 0) {
      message.success('设置权重成功')
      weightModalVisible.value = false
      await fetchFriendLinks()
    }
  } catch (error) {
    message.error('设置权重失败')
  } finally {
    weightModalLoading.value = false
  }
}

// 权重设置取消
const handleWeightModalCancel = () => {
  weightModalVisible.value = false
}

// 刷新缓存
const handleRefreshCache = async () => {
  try {
    const res = await refreshCacheUsingPost()
    if (res.data?.code === 0) {
      message.success('刷新缓存成功')
    }
  } catch (error) {
    message.error('刷新缓存失败')
  }
}

// 移动端加载更多
const onLoadMore = async () => {
  if (loading.value || finished.value) return
  pagination.current += 1
  await fetchFriendLinks()
}


// 删除确认
const showDeleteConfirm = (item: FriendLink) => {
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除该友链吗？',
    async onOk() {
      try {
        const res = await deleteFriendLinkUsingPost({ id: item.id })
        if (res.data?.code === 0) {
          message.success('删除成功')
          await fetchFriendLinks()
        } else {
          message.error(res.data?.message || '删除失败')
        }
      } catch (err) {
        console.error('删除失败:', err)
        message.error('删除失败')
      }
    }
  })
}

// 移动端表单相关
const showSiteTypePicker = ref(false)
const mobileFormRef = ref()

// 移动端表单关闭处理
const handleMobileDialogClose = (action: string) => {
  if (action === 'confirm') {
    handleMobileFormSubmit()
    return false
  }
  return true
}

// 移动端表单提交
const handleMobileFormSubmit = async () => {
  try {
    await mobileFormRef.value?.validate()
    modalLoading.value = true
    const api = modalMode.value === 'add' ? addFriendLinkUsingPost : updateFriendLinkUsingPost
    const res = await api(formData)
    if (res.data?.code === 0) {
      message.success(`${modalMode.value === 'add' ? '添加' : '编辑'}友链成功`)
      modalVisible.value = false
      fetchFriendLinks()
    }
  } catch (err) {
    console.error(err)
    message.error('表单验证失败')
  } finally {
    modalLoading.value = false
  }
}

// 网站类型选择确认
const onSiteTypeConfirm = (value: { text: string; value: string }) => {
  formData.siteType = value.value
  showSiteTypePicker.value = false
}

// 权重验证
const validateWeight = (value: string) => {
  const num = Number(value)
  return !isNaN(num) && num >= 0 && num <= 100
}

// 移动端权重设置关闭处理
const handleMobileWeightDialogClose = async (action: string) => {
  if (action === 'confirm') {
    try {
      const weight = Number(weightForm.weight)
      if (!validateWeight(String(weight))) {
        message.error('权重值必须在0-100之间')
        return false
      }
      weightModalLoading.value = true
      const res = await updateWeightUsingPost({
        id: weightForm.id!,
        weight: weight
      })
      if (res.data?.code === 0) {
        message.success('设置权重成功')
        weightModalVisible.value = false
        pagination.current = 1 // 重置到第一页
        await fetchFriendLinks() // 重新获取数据
        return true
      }
    } catch (err) {
      console.error(err)
      message.error('设置权重失败')
    } finally {
      weightModalLoading.value = false
    }
    return false
  }
  weightModalVisible.value = false
  return true
}

// 处理筛选变化
const handleFilterChange = () => {
  nextTick(() => {
    pagination.current = 1
    finished.value = false
    fetchFriendLinks()
  })
}

// Add these reactive variables and watchers
const localPageSize = ref(pagination.pageSize)

watch(localPageSize, (newVal) => {
  if (newVal !== pagination.pageSize) {
    pagination.pageSize = newVal
    pagination.current = 1
    fetchFriendLinks()
  }
})

// Handle page navigation
const handlePrevPage = () => {
  if (pagination.current > 1) {
    pagination.current--
    fetchFriendLinks()
  }
}

const handleNextPage = () => {
  if (pagination.current < Math.ceil(total.value / pagination.pageSize)) {
    pagination.current++
    fetchFriendLinks()
  }
}
</script>

<style lang="scss" scoped>

/* 全局变量与基础重置 */
.friend-link-manage {
  --primary-color: #1890ff;
  --success-color: #10b981;
  --warning-color: #f59e0b;
  --danger-color: #ef4444;
  --card-bg: rgba(255, 255, 255, 0.05);
  --border-color: rgba(255, 255, 255, 0.1);
  --text-main: var(--text-primary, #1a1a1a);
  --text-sub: var(--text-secondary, #8c8c8c);

  background: var(--header-background, #f5f7fa);
  min-height: 100vh;
}

/* PC端高级感样式 */
.pc-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 32px 24px;

  .pc-header-premium {
    display: flex;
    justify-content: space-between;
    align-items: flex-end;
    margin-bottom: 32px;

    .premium-title {
      font-size: 28px;
      font-weight: 700;
      color: var(--text-main);
      margin: 0 0 4px 0;
      letter-spacing: -0.5px;
    }

    .premium-subtitle {
      font-size: 14px;
      color: var(--text-sub);
      margin: 0;
    }

    .header-right-actions {
      display: flex;
      gap: 12px;

      .ant-btn {
        height: 40px;
        padding: 0 20px;
        border-radius: 10px;
        font-weight: 500;
        display: flex;
        align-items: center;
        gap: 8px;
        transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

        &.btn-add-premium {
          box-shadow: 0 4px 12px rgba(24, 144, 255, 0.3);
          &:hover {
            transform: translateY(-2px);
            box-shadow: 0 6px 16px rgba(24, 144, 255, 0.4);
          }
        }

        &.btn-refresh-premium {
          background: transparent;
          border-color: var(--border-color);
          color: var(--text-main);
          &:hover {
            background: rgba(0, 0, 0, 0.02);
            border-color: var(--primary-color);
            color: var(--primary-color);
          }
        }
      }
    }
  }

  .pc-search-filter-minimal {
    display: flex;
    gap: 16px;
    margin-bottom: 24px;
    padding: 16px 20px;
    background: #f5f5f5; // 使用实色背景
    border: 1px solid #e8e8e8;
    border-radius: 16px;

    .filter-item {
      &.search-input {
        flex: 1;
      }
      :deep(.ant-input), :deep(.ant-select-selector) {
        border-radius: 10px !important;
        height: 40px !important;
        background: #fff !important; // 强制白色内部提高对比度
        border: 1px solid #d9d9d9 !important;
        display: flex;
        align-items: center;
      }
      :deep(.ant-select-selection-item), :deep(.ant-select-selection-placeholder) {
        line-height: 38px !important;
        color: #262626 !important;
      }
      .minimal-select {
        width: 160px;
      }
    }
  }

  :deep(.ant-table-wrapper) {
    background: #fff;
    border-radius: 20px;
    padding: 12px;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);

    .ant-table {
      background: transparent;
    }

    .ant-table-thead > tr > th {
      background: transparent;
      border-bottom: 1px solid #f0f0f0;
      color: var(--text-sub);
      font-weight: 500;
    }

    .ant-table-tbody > tr > td {
      border-bottom: 1px solid #f9f9f9;
      padding: 16px 8px;
    }

    .ant-table-row:hover > td {
      background: #fafafa !important;
    }
  }
}

/* 移动端简约奢华样式 */
.mobile-container-premium {
  padding: 16px;
  padding-top: 64px;
  background: var(--header-background, #f5f7fa);
}

.mobile-header-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 60px;
  padding: 0 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(20px);
  z-index: 100;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);

  .mobile-title {
    font-size: 18px;
    font-weight: 700;
    margin: 0;
    background: linear-gradient(135deg, #1890ff, #096dd9);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
  }

  .mobile-header-actions {
    display: flex;
    gap: 12px;
  }
}

.mobile-search-filter {
  margin: 16px;
  padding: 18px;
  background: #fff;
  border-radius: 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);

  .search-bar-inner {
    display: flex;
    align-items: center;
    background: #f5f5f5;
    border-radius: 12px;
    padding: 0 14px;
    height: 44px;
    margin-bottom: 12px;
    border: 1px solid #eee;

    .search-icon {
      color: var(--primary-color);
      margin-right: 10px;
      font-size: 16px;
    }

    .minimal-input {
      flex: 1;
      border: none;
      outline: none;
      font-size: 14px;
      background: transparent;
      color: #262626;
      &::placeholder {
        color: #bfbfbf;
      }
    }
  }

  .mobile-dropdown-row {
    display: flex;
    gap: 10px;
  }

  .clean-dropdown {
    flex: 1;
    height: 36px;
    :deep(.van-dropdown-menu__bar) {
      background: transparent;
      box-shadow: none;
      height: 36px;
    }
    :deep(.van-dropdown-menu__title) {
      font-size: 13px;
      color: #262626 !important;
      font-weight: 600;
      background: #f0f0f0;
      padding: 0 12px;
      border-radius: 10px;
      height: 34px;
      line-height: 34px;
      border: 1px solid #e8e8e8;
      width: 100%;
      display: flex;
      justify-content: center;
      &::after {
        border-color: transparent transparent #262626 #262626 !important;
      }
    }
  }
}

.friend-cards-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding-bottom: 80px;
}

.friend-card-premium {
  position: relative;
  background: #fff;
  border-radius: 20px;
  padding: 20px;
  overflow: hidden;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.03);
  border: 1px solid rgba(0, 0, 0, 0.02);

  .card-main {
    position: relative;
    z-index: 1;
  }

  .card-header {
    display: flex;
    align-items: center;
    gap: 14px;
    margin-bottom: 16px;

    .site-logo-shadow {
      box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1);
      border: 2px solid #fff;
    }

    .name-desc {
      flex: 1;
      .site-name-top {
        font-size: 17px;
        font-weight: 700;
        color: #262626;
        margin-bottom: 2px;
      }
      .site-type-tag {
        font-size: 11px;
        color: var(--primary-color);
        background: rgba(24, 144, 255, 0.08);
        padding: 1px 8px;
        border-radius: 6px;
        display: inline-block;
      }
    }

    .status-badge {
      font-size: 11px;
      padding: 2px 8px;
      border-radius: 6px;
    }
  }

  .card-body {
    .site-url-box {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 13px;
      color: var(--primary-color);
      background: #f0f7ff;
      padding: 8px 12px;
      border-radius: 10px;
      margin-bottom: 12px;
      span {
        flex: 1;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }

    .site-description {
      font-size: 14px;
      color: #595959;
      line-height: 1.6;
      margin-bottom: 16px;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }

    .owner-contacts {
      display: flex;
      flex-direction: column;
      gap: 6px;
      margin-bottom: 16px;
      .contact-item {
        font-size: 12px;
        color: #595959; // 增加对比度
        display: flex;
        align-items: center;
        gap: 6px;
      }
    }
  }

  .card-footer-actions {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-top: 16px;
    border-top: 1px solid #f5f5f5;

    .weight-info {
      font-size: 12px;
      color: #8c8c8c; // 增加对比度
      span {
        color: var(--primary-color);
        font-weight: 600;
        font-size: 14px;
      }
    }

    .actions-group {
      display: flex;
      gap: 10px;

      .btn-action-sm {
        height: 32px;
        border-radius: 8px;
        font-size: 13px;
        padding: 0 16px;
      }
      .btn-approve {
        box-shadow: 0 4px 10px rgba(16, 185, 129, 0.2);
      }
      .btn-more-sm {
        height: 32px;
        border-radius: 8px;
        background: #f5f5f5;
        border: none;
        color: #262626;
      }
    }
  }
}

.mobile-pagination-box {
  margin: 16px;
  background: #fff; // 强制纯白背景
  border-radius: 20px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  border: 1px solid #f0f0f0;
  box-shadow: 0 -4px 15px rgba(0, 0, 0, 0.03); // 向上阴影

  .pagination-info {
    font-size: 14px;
    color: #262626 !important; // 强制深色
    font-weight: 600;
  }

  .pagination-nav {
    display: flex;
    gap: 20px;
    .nav-circle-btn {
      width: 44px;
      height: 44px;
      border-radius: 22px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: #fff;
      box-shadow: 0 4px 10px rgba(0, 0, 0, 0.05);
      border: 1px solid #e8e8e8;
      color: var(--primary-color);
      font-size: 18px;
      &:disabled {
        opacity: 0.2;
      }
      &:active {
        background: #f5f5f5;
      }
    }
  }
}

// 解决 Vant 下拉菜单选项看不清的问题
:deep(.van-dropdown-item__option) {
  .van-cell__title {
    color: #262626 !important;
    font-weight: 500;
  }
}

.btn-pulse {
  animation: pulse-glow 2s infinite;
}

@keyframes pulse-glow {
  0% { box-shadow: 0 0 0 0 rgba(24, 144, 255, 0.4); }
  70% { box-shadow: 0 0 0 8px rgba(24, 144, 255, 0); }
  100% { box-shadow: 0 0 0 0 rgba(24, 144, 255, 0); }
}

.btn-ghost {
  background: rgba(0, 0, 0, 0.04);
  border: none;
  color: #595959;
}
</style>

