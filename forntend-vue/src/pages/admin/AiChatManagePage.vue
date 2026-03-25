<template>
  <div id="aiChatManagePage">
    <!-- PC端展示 -->
    <template v-if="device === DEVICE_TYPE_ENUM.PC">
      <div class="pc-container">
        <!-- 搜索表单 -->
        <div class="search-and-button-container">
          <a-form layout="inline" :model="searchParams" @finish="doSearch">
            <a-form-item label="用户ID">
              <a-input
                v-model:value="searchParams.userId"
                placeholder="输入用户ID"
                allow-clear
              />
            </a-form-item>
            <a-form-item label="角色">
              <a-select
                v-model:value="searchParams.role"
                placeholder="选择角色"
                allow-clear
                style="width: 120px"
              >
                <a-select-option value="user">用户</a-select-option>
                <a-select-option value="assistant">助手</a-select-option>
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
            <a-button
              style="margin-left: 20px"
              type="primary"
              danger
              :disabled="!hasSelected"
              @click="batchDeleteSelectedChats"
              class="action-button"
            >
              <DeleteOutlined />批量删除
            </a-button>
          </div>
        </div>

        <div class="table-section">
          <a-spin tip="正在加载中..." :spinning="loading">
            <a-table
              :row-selection="rowSelection"
              rowKey="id"
              :columns="columns"
              :data-source="dataList"
              :pagination="false"
              @change="doTableChange"
              class="custom-table"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'content'">
                  <a-tooltip :title="record.content">
                    <div class="ellipsis-text">
                      {{ record.content }}
                    </div>
                  </a-tooltip>
                </template>
                <template v-if="column.dataIndex === 'createTime'">
                  <div class="ellipsis-text">
                    {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}
                  </div>
                </template>
                <template v-else-if="column.key === 'action'">
                  <div class="action-buttons">
                    <a-button
                      danger
                      class="delete-button"
                      @click="showDeleteConfirm(record)"
                    >
                      <DeleteOutlined />
                      删除
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
    </template>

    <!-- 移动端展示 -->
    <template v-else>
      <div class="mobile-container">
        <div class="mobile-content">
          <!-- 搜索区域 -->
          <div class="search-section">
            <van-search
              v-model="searchParams.userId"
              placeholder="搜索用户ID"
              @search="doSearch"
            />
            <van-dropdown-menu>
              <van-dropdown-item v-model="searchParams.role" :options="roleOptions" />
            </van-dropdown-menu>
          </div>

          <!-- 操作按钮区 -->
          <div class="action-bar">
            <a-button type="dashed" class="sort-button" @click="toggleSortOrder">
              <span class="button-content">
                <span class="button-text">{{ sortOrder === 'ascend' ? '升序' : '降序' }}</span>
              </span>
            </a-button>
            <van-button
              type="danger"
              size="small"
              icon="delete-o"
              :disabled="!hasSelected"
              @click="batchDeleteSelectedChats"
            >批量删除</van-button>
          </div>

          <!-- 聊天记录列表 -->
          <div class="chat-list">
            <van-checkbox-group v-model="state.selectedRowKeys">
              <van-cell-group inset v-for="chat in dataList" :key="chat.id">
                <van-card class="chat-card">
                  <template #title>
                    <div class="chat-info">
                      <div class="chat-content">{{ chat.content }}</div>
                      <div class="chat-meta">
                        <div class="meta-item">
                          <span class="meta-label">ID：</span>
                          <span class="meta-value">{{ chat.id }}</span>
                        </div>
                        <div class="meta-item">
                          <span class="meta-label">用户ID：</span>
                          <span class="meta-value">{{ chat.userId }}</span>
                        </div>
                        <div class="meta-item">
                          <span class="meta-label">角色：</span>
                          <span class="meta-value">{{ chat.role }}</span>
                        </div>
                        <div class="meta-item">
                          <span class="meta-label">创建时间：</span>
                          <span class="meta-value">{{ dayjs(chat.createTime).format('YYYY-MM-DD HH:mm:ss') }}</span>
                        </div>
                      </div>
                    </div>
                  </template>

                  <template #footer>
                    <div class="card-footer">
                      <van-checkbox
                        :name="chat.id"
                        class="card-checkbox"
                      />
                      <div class="card-actions">
                        <van-button
                          size="small"
                          type="danger"
                          @click="showDeleteConfirm(chat)"
                          class="action-btn delete-btn"
                        >删除</van-button>
                      </div>
                    </div>
                  </template>
                </van-card>
              </van-cell-group>
            </van-checkbox-group>
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
            <van-pagination
              v-model="searchParams.current"
              :total-items="total"
              :items-per-page="searchParams.pageSize"
              :show-page-size="3"
              @change="onPageChange"
            />
          </div>

          <!-- 每页条数选择器 -->
          <van-action-sheet
            v-model:show="showPageSizeSheet"
            :actions="pageSizeOptions"
            @select="onSelectPageSize"
            cancel-text="取消"
            close-on-click-action
          />
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed,onMounted, onUnmounted  } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  SearchOutlined,
  DeleteOutlined,
  SortAscendingOutlined,
  SortDescendingOutlined
} from '@ant-design/icons-vue'
import { getDeviceType } from '@/utils/device'
import { DEVICE_TYPE_ENUM } from '@/constants/device'
import dayjs from 'dayjs'
import { listChatByPageAdminUsingGet, batchDeleteChatUsingDelete } from '@/api/aiChatController'

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
    width: '80px'
  },
  {
    title: '用户ID',
    dataIndex: 'userId',
    width: '100px'
  },
  {
    title: '角色',
    dataIndex: 'role',
    width: '100px'
  },
  {
    title: '内容',
    dataIndex: 'content',
    ellipsis: true
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: '180px',
    sorter: true
  },
  {
    title: '操作',
    key: 'action',
    width: '120px',
    fixed: 'right'
  }
]

// 状态管理
const state = reactive({
  selectedRowKeys: [],
  selectedRows: []
})

// 搜索参数
const searchParams = reactive({
  current: 1,
  pageSize: 10,
  userId: undefined,
  role: undefined,
  sortField: undefined,
  sortOrder: undefined
})

// 数据加载和分页
const loading = ref(false)
const dataList = ref([])
const total = ref(0)
const pcPageSizeOptions = ['10', '20', '50', '100']
const showPageSizeSheet = ref(false)
const pageSizeOptions = pcPageSizeOptions.map(size => ({
  name: `${size}条/页`,
  value: parseInt(size)
}))

// 排序状态
const sortOrder = ref('descend')
const toggleSortOrder = () => {
  sortOrder.value = sortOrder.value === 'ascend' ? 'descend' : 'ascend'
  searchParams.sortOrder = sortOrder.value
  searchParams.sortField = 'createTime'
  doSearch()
}

// 角色选项
const roleOptions = [
  { text: '全部', value: undefined },
  { text: '用户', value: 'user' },
  { text: '助手', value: 'assistant' }
]

// 表格选择配置
const rowSelection = {
  onChange: (selectedRowKeys: any[], selectedRows: any[]) => {
    state.selectedRowKeys = selectedRowKeys
    state.selectedRows = selectedRows
  }
}

// 是否有选中项
const hasSelected = computed(() => state.selectedRowKeys.length > 0)

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const res = await listChatByPageAdminUsingGet(searchParams)
    if (res.data.code === 0) {
      dataList.value = res.data.data.records
      total.value = res.data.data.total
    } else {
      message.error('获取数据失败：' + res.data.message)
    }
  } catch (error) {
    message.error('获取数据失败：' + error.message)
  } finally {
    loading.value = false
  }
}

// 搜索
const doSearch = () => {
  searchParams.current = 1
  loadData()
}

// 表格变化处理
const doTableChange = (pagination: any, filters: any, sorter: any) => {
  if (sorter) {
    searchParams.sortField = sorter.field
    searchParams.sortOrder = sorter.order
  }
  loadData()
}

// 分页变化
const onPageChange = (page: number) => {
  searchParams.current = page
  loadData()
}

// 每页条数变化
const onShowSizeChange = (current: number, size: number) => {
  searchParams.current = 1
  searchParams.pageSize = size
  loadData()
}

// 移动端选择每页条数
const onSelectPageSize = (option: any) => {
  searchParams.pageSize = option.value
  searchParams.current = 1
  loadData()
}

// 删除确认
const showDeleteConfirm = (record: any) => {
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除这条聊天记录吗？',
    okText: '确定',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      try {
        const res = await batchDeleteChatUsingDelete([record.id])
        if (res.data.code === 0) {
          message.success('删除成功')
          // 重新加载数据
          loadData()
        } else {
          message.error('删除失败：' + res.data.message)
        }
      } catch (error) {
        message.error('删除失败：' + error.message)
      }
    }
  })
}

// 批量删除
const batchDeleteSelectedChats = () => {
  if (!hasSelected.value) {
    return
  }
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除这${state.selectedRowKeys.length}条聊天记录吗？`,
    okText: '确定',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      try {
        const res = await batchDeleteChatUsingDelete(state.selectedRowKeys)
        if (res.data.code === 0) {
          message.success('删除成功')
          // 清空选择
          state.selectedRowKeys = []
          state.selectedRows = []
          // 重新加载数据
          loadData()
        } else {
          message.error('删除失败：' + res.data.message)
        }
      } catch (error) {
        message.error('删除失败：' + error.message)
      }
    }
  })
}

// 初始加载
loadData()
</script>

<style scoped>
@media screen and (min-width: 768px) {
  #aiChatManagePage {
    background: var(--header-background);
    color: var(--text-primary)!important;
    width: 100vw;
    margin: 0 auto;
    padding: 24px;
  }
}



#aiChatManagePage .pc-container .search-and-button-container {
  display: flex;
  justify-content: space-between;
  margin-bottom: 16px;
}

#aiChatManagePage .pc-container .table-section {
  margin-bottom: 16px;
}

#aiChatManagePage .pc-container .custom-table .ellipsis-text {
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

#aiChatManagePage .pc-container .custom-table .action-buttons {
  display: flex;
  gap: 8px;
}

#aiChatManagePage .pc-container .mz-antd-pagination {
  display: flex;
  justify-content: flex-end;
}

/* 移动端样式 */
#aiChatManagePage .mobile-container .mobile-content .search-section {
  margin-bottom: 16px;
}

#aiChatManagePage .mobile-container .mobile-content .action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding: 0 16px;
}

#aiChatManagePage .mobile-container .mobile-content .chat-list .chat-card {
  margin-bottom: 12px;
}

#aiChatManagePage .mobile-container .mobile-content .chat-list .chat-card .chat-info .chat-content {
  font-size: 14px;
  margin-bottom: 8px;
}

#aiChatManagePage .mobile-container .mobile-content .chat-list .chat-card .chat-info .chat-meta {
  font-size: 12px;
  color: #666;
}

#aiChatManagePage .mobile-container .mobile-content .chat-list .chat-card .chat-info .chat-meta .meta-item {
  margin-bottom: 4px;
}

#aiChatManagePage .mobile-container .mobile-content .chat-list .chat-card .chat-info .chat-meta .meta-item .meta-label {
  color: #999;
}

#aiChatManagePage .mobile-container .mobile-content .chat-list .chat-card .card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}

#aiChatManagePage .mobile-container .mobile-content .chat-list .chat-card .card-footer .card-actions {
  display: flex;
  gap: 8px;
}

#aiChatManagePage .mobile-container .mobile-content .mobile-pagination {
  padding: 16px;
}

#aiChatManagePage .mobile-container .mobile-content .mobile-pagination .pagination-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

#aiChatManagePage .mobile-container .mobile-content .mobile-pagination .pagination-info .page-size-selector {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #666;
}
</style>
