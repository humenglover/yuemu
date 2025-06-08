<template>
  <div id="userManagePage">
    <!-- PC端展示 -->
    <template v-if="device === DEVICE_TYPE_ENUM.PC">
      <div class="pc-container">
        <!-- 搜索表单 -->
        <div class="search-and-button-container">
          <a-form layout="inline" :model="searchParams" @finish="doSearch">
            <a-form-item label="账号">
              <a-input
                v-model:value="searchParams.userAccount"
                placeholder="输入账号"
                allow-clear
              />
            </a-form-item>
            <a-form-item label="用户名">
              <a-input v-model:value="searchParams.userName" placeholder="输入用户名" allow-clear />
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
              @click="batchDeleteSelectedPictures"
              class="action-button"
            >
              <DeleteOutlined />批量删除
            </a-button>
            <a-button
              type="primary"
              class="action-button primary-button"
              @click="openModal"
              style="margin-left: 20px"
            >
              <PlusOutlined />
              <span class="button-text">添加用户</span>
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
                <template v-if="column.dataIndex === 'userAvatar'">
                  <a-image
                    :src="record.userAvatar"
                    :width="50"
                    :style="{ maxHeight: '50px', objectFit: 'contain' }"
                  />
                </template>
                <template v-else-if="column.dataIndex === 'userRole'">
                  <div v-if="record.userRole === 'admin'">
                    <a-tag color="green">管理员</a-tag>
                  </div>
                  <div v-if="record.userRole === 'user'">
                    <a-tag color="blue">普通用户</a-tag>
                  </div>
                  <div v-if="record.userRole === 'ban'">
                    <a-tag color="red">封禁用户</a-tag>
                  </div>
                </template>
                <template v-if="column.dataIndex === 'createTime'">
                  <div
                    :style="{
                      maxWidth: '250px',
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                      whiteSpace: 'nowrap',
                    }"
                  >
                    {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}
                  </div>
                </template>
                <template v-else-if="column.dataIndex === 'userProfile'">
                  <a-tooltip :title="record.userProfile">
                    <div class="ellipsis-text">
                      {{ record.userProfile }}
                    </div>
                  </a-tooltip>
                </template>
                <template v-else-if="column.dataIndex === 'id'">
                  <div
                    :style="{
                      maxWidth: '150px',
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                      whiteSpace: 'nowrap',
                    }"
                  >
                    {{ record.id }}
                  </div>
                </template>
                <template v-else-if="column.dataIndex === 'email'">
                  <a-tooltip :title="record.email || '未绑定邮箱'">
                    <div class="ellipsis-text">
                      {{ record.email || '未绑定邮箱' }}
                    </div>
                  </a-tooltip>
                </template>
                <template v-else-if="column.key === 'action'">
                  <div class="action-buttons">
                    <!-- 查看详情按钮 -->
                    <a-button
                      type="link"
                      class="table-button view-button"
                      @click="goToUserDetail(record.id)"
                    >
                      <EyeOutlined />
                      查看详情
                    </a-button>
                    <!-- 封禁按钮 -->
                    <a-button
                      v-if="record.userRole !== 'admin' && record.id !== loginUserStore.loginUser.id && record.userRole !== 'ban'"
                      type="danger"
                      class="ban-button"
                      @click="handleBanOrUnban(record)"
                    >
                      封禁
                    </a-button>
                    <!-- 解禁按钮 -->
                    <a-button
                      v-if="record.userRole === 'ban' && record.id !== loginUserStore.loginUser.id"
                      type="primary"
                      class="unban-button"
                      @click="handleBanOrUnban(record)"
                    >
                      解禁
                    </a-button>
                    <a-button
                      v-if="record.userRole !== 'admin' && record.id !== loginUserStore.loginUser.id"
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

      <!-- 添加用户模态框 -->
      <a-modal v-model:open="open" title="添加用户" @click="openModal">
        <form @submit.prevent="handleAddUser">
          <a-form-item>
            <div style="display: flex; flex-direction: column">
              <span class="form-label"><span STYLE="color: red">*</span>账号</span>
              <a-input
                v-model:value="addUserForm.userAccount"
                placeholder="请输入账号"
                allow-clear
              />
            </div>
          </a-form-item>
          <a-form-item>
            <div style="display: flex; flex-direction: column">
              <span class="form-label"><span STYLE="color: red">*</span>用户角色</span>
              <a-select v-model:value="addUserForm.userRole" placeholder="请选择用户角色">
                <a-select-option value="admin">管理员</a-select-option>
                <a-select-option value="user">普通用户</a-select-option>
              </a-select>
            </div>
          </a-form-item>
          <a-form-item>
            <div style="display: flex; flex-direction: column">
              <span class="form-label">用户名</span>
              <a-input
                v-model:value="addUserForm.userName"
                placeholder="请输入用户名"
                allow-clear
              />
            </div>
          </a-form-item>
          <a-form-item>
            <div style="display: flex; flex-direction: column">
              <span class="form-label">简介</span>
              <a-input
                v-model:value="addUserForm.userProfile"
                placeholder="请输入用户简介"
                allow-clear
              />
            </div>
          </a-form-item>
        </form>
        <template #footer>
          <div style="text-align: center; margin-top: 15px">
            <a-button type="primary" html-type="submit" @click="handleAddUser">提交</a-button>
            <a-button type="default" @click="cancelAddUser">取消</a-button>
          </div>
        </template>
      </a-modal>
    </template>

    <!-- 移动端展示 -->
    <template v-else>
      <div class="mobile-container">
        <div class="mobile-content">
          <!-- 搜索区域 -->
          <div class="search-section">
            <van-search
              v-model="searchParams.userAccount"
              placeholder="搜索账号"
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
            <van-button type="primary" size="small" icon="plus" @click="openModal">
              添加用户
            </van-button>
            <van-button
              type="danger"
              size="small"
              icon="delete-o"
              :disabled="!hasSelected"
              @click="batchDeleteSelectedPictures"
            >批量删除</van-button
            >
          </div>

          <!-- 用户列表 -->
          <div class="user-list">
            <van-checkbox-group v-model="state.selectedRowKeys">
              <van-cell-group inset v-for="user in dataList" :key="user.id">
                <van-card

                  :title="user.userName || '未设置昵称'"
                  :desc="user.userProfile || '这个人很懒，什么都没写~'"
                  :thumb="user.userAvatar"
                  class="user-card"
                  :thumb-style="{ width: '45px', height: '45px', borderRadius: '4px' }"
                >
                  <template #tags>
                    <van-tag
                      :type="user.userRole === 'admin' ? 'success' : 'primary'"
                      round
                      size="small"
                      style="margin-right: 4px; padding: 0 8px"
                    >
                      {{ user.userRole === 'admin' ? '管理员': user.userRole === 'user' ? '普通用户': '封禁用户' }}
                    </van-tag>
                    <van-tag type="warning" round size="small" style="padding: 0 8px">
                      ID: {{ user.id }}
                    </van-tag>
                    <div class="user-email-tag">
                      <MailOutlined class="email-icon" />
                      {{ user.email || '未绑定邮箱' }}
                    </div>
                  </template>

                  <template #price>
                    <span class="account-text">账号: {{ user.userAccount }}</span>
                  </template>

                  <template #num>
                    <span class="create-time">
                      {{ dayjs(user.createTime).format('YYYY-MM-DD HH:mm') }}
                    </span>
                  </template>

                  <template #footer>
                    <div class="card-footer">
                      <van-checkbox
                        :name="user.id"
                        v-if="user.userRole !== 'admin' && user.id !== loginUserStore.loginUser.id"
                        class="card-checkbox"
                      />
                      <van-button
                        size="mini"
                        type="primary"
                        plain
                        @click="goToUserDetail(user.id)"
                        class="view-button"
                        style="margin-right: 8px"
                      >
                        <template #icon>
                          <EyeOutlined />
                        </template>
                        查看
                      </van-button>
                      <!-- 封禁按钮 -->
                      <van-button
                        v-if="user.userRole !== 'admin' && user.id !== loginUserStore.loginUser.id && user.userRole !== 'ban'"
                        size="mini"
                        type="danger"
                        @click="handleBanOrUnban(user)"
                        class="ban-button"
                      >
                        封禁
                      </van-button>
                      <!-- 解禁按钮 -->
                      <van-button
                        v-if="user.userRole === 'ban' && user.id !== loginUserStore.loginUser.id"
                        size="mini"
                        type="primary"
                        @click="handleBanOrUnban(user)"
                        class="unban-button"
                      >
                        解禁
                      </van-button>
                      <van-button
                        v-if="user.userRole !== 'admin' && user.id !== loginUserStore.loginUser.id"
                        size="mini"
                        type="danger"
                        @click="showDeleteConfirm(user)"
                        class="delete-button"
                      >删除</van-button>
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
            :actions="mobilePageSizeOptions"
            cancel-text="取消"
            close-on-click-action
            @select="handlePageSizeChange"
          />
        </div>

        <!-- 移动端添加用户弹窗 -->
        <van-popup v-model:show="open" position="bottom" round :style="{ height: '70%' }">
          <div class="popup-content">
            <div class="popup-header">
              <span class="popup-title">添加用户</span>
              <van-icon name="cross" @click="cancelAddUser" />
            </div>
            <van-form @submit="handleAddUser">
              <van-cell-group inset>
                <van-field
                  v-model="addUserForm.userAccount"
                  name="userAccount"
                  label="账号"
                  placeholder="请输入账号"
                  :rules="[{ required: true, message: '请填写账号' }]"
                />
                <van-field
                  v-model="addUserForm.userName"
                  name="userName"
                  label="用户名"
                  placeholder="请输入用户名"
                />
                <van-field
                  v-model="addUserForm.userProfile"
                  name="userProfile"
                  label="简介"
                  placeholder="请输入用户简介"
                  type="textarea"
                  rows="2"
                />
                <van-field name="userRole" label="用户角色">
                  <template #input>
                    <van-radio-group v-model="addUserForm.userRole" direction="horizontal">
                      <van-radio name="user">普通用户</van-radio>
                      <van-radio name="admin">管理员</van-radio>
                    </van-radio-group>
                  </template>
                </van-field>
              </van-cell-group>
              <div style="margin: 16px">
                <van-button round block type="primary" native-type="submit">提交</van-button>
              </div>
            </van-form>
          </div>
        </van-popup>
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
        <h3 class="confirm-title">确认删除该用户？</h3>
        <p class="confirm-desc">
          用户账号：{{ selectedUser?.userAccount }}<br>
          用户名称：{{ selectedUser?.userName || '未设置' }}
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
import { onMounted, reactive, ref, computed, watch } from 'vue'
import {
  deleteUserUsingPost,
  listUserVoByPageUsingPost,
  addUserUsingPost,
  batchDeleteUserUsingPost,
  banOrUnbanUserUsingPost,
} from '@/api/userController.ts'
import { Form, message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { getDeviceType } from '@/utils/device.ts'
import { DEVICE_TYPE_ENUM } from '@/constants/device.ts'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import {
  PlusOutlined,
  DeleteOutlined,
  SortAscendingOutlined,
  SortDescendingOutlined,
  SearchOutlined,
  ExclamationCircleFilled,
  MailOutlined,
  EyeOutlined,
} from '@ant-design/icons-vue'
import { useRouter } from 'vue-router'

const loginUserStore = useLoginUserStore()
const router = useRouter()
// 定义用于存储设备类型的响应式变量
const device = ref<string>('')
// 页面加载时获取设备类型并获取数据
onMounted(async () => {
  device.value = await getDeviceType()
})
// 表格列定义，虽然移动端使用了不同的展示方式，但这里保留原定义，可根据实际情况调整或简化
const columns = [
  {
    title: '头像',
    dataIndex: 'userAvatar',
    width: 80,
  },
  {
    title: 'ID',
    dataIndex: 'id',
    width: 80,
  },
  {
    title: '用户名',
    dataIndex: 'userName',
    width: 120,
  },
  {
    title: '邮箱',
    dataIndex: 'email',
    width: 180,
    ellipsis: true,
  },
  {
    title: '账号',
    dataIndex: 'userAccount',
    width: 120,
  },
  {
    title: '用户角色',
    dataIndex: 'userRole',
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
  },
  {
    title: '操作',
    key: 'action',
  },
]

// 定义数据
const dataList = ref<API.UserVO[]>([])
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

// 搜索条件，将sortOrder提取出来，方便后续操作
const searchParams = reactive<API.UserQueryRequest>({
  current: 1,
  pageSize: 8,
  sortField: 'createTime',
})
const sortOrder = ref<'ascend' | 'descend'>('ascend')

// 获取数据
const fetchData = async () => {
  if (device.value === DEVICE_TYPE_ENUM.PC) {
    loading.value = true
  }

  try {
    const res = await listUserVoByPageUsingPost({
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
      console.log('获取到的数据:', dataList.value) // 添加日志查看数据
    }
  } catch (error) {
    console.error('获取数据失败:', error)
    message.error('获取数据失败')
  } finally {
    loading.value = false
  }
}


// 处理分页尺寸改变时的逻辑（每页显示条数改变）
const onShowSizeChange = (current: number, pageSize: number) => {
  searchParams.current = 1 // 切换每页条数时，默认回到第一页，可根据需求调整
  searchParams.pageSize = pageSize
  fetchData()
}

// 处理页码改变时的逻辑
const onPageChange = (page: number, pageSize: number) => {
  searchParams.current = page
  searchParams.pageSize = pageSize
  fetchData()
}

// 表格变化之后，重新获取数据（处理排序等变化情况）
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

    const res = await listUserVoByPageUsingPost({
      ...searchParams,
      sortField: 'createTime',
      sortOrder: sortOrder.value,
    })

    if (res.data?.code === 0) {
      dataList.value = res.data.data.records
      total.value = parseInt(res.data.data.total)
      console.log('搜索结果:', dataList.value) // 添加日志查看数据
    } else {
      message.error('获取用户列表失败，请稍后重试')
    }
  } catch (error) {
    console.error('获取用户列表异常', error)
    message.error('获取用户列表失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 监听搜索参数变化
watch(
  () => searchParams,
  async () => {
    try {
      await doSearch()
    } catch (error) {
      console.error('监听搜索参数变化时出错：', error)
    }
  },
  { deep: true },
)

// 组件挂载时获取数据
onMounted(async () => {
  try {
    await doSearch()
  } catch (error) {
    console.error('初始化加载数据失败：', error)
  }
})

// 删除确认相关的状态
const deleteConfirmVisible = ref(false)
const selectedUser = ref<API.UserVO | null>(null)

// 显示删除确认框
const showDeleteConfirm = (user: API.UserVO) => {
  selectedUser.value = user
  deleteConfirmVisible.value = true
}

// 确认删除
const confirmDelete = async () => {
  if (!selectedUser.value?.id) return

  try {
    const res = await deleteUserUsingPost({ id: selectedUser.value.id })
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

const open = ref(false)
const openModal = () => {
  open.value = true
}

// 新增：用于存储添加用户表单数据的响应式对象
const addUserForm = reactive({
  userAccount: '',
  userName: '',
  userProfile: '',
  userRole: '',
})

// 新增：处理添加用户表单提交的方法
const handleAddUser = async () => {
  const userData: API.UserAddRequest = {
    userAccount: addUserForm.userAccount,
    userName: addUserForm.userName,
    userProfile: addUserForm.userProfile,
    userRole: addUserForm.userRole,
  }
  try {
    const res = await addUserUsingPost(userData)
    if (res.data.code === 0) {
      message.success('用户添加成功')
      // 关闭模态框
      open.value = false
      // 刷新数据列表
      fetchData()
    } else {
      message.error('用户添加失败，' + res.data.message)
    }
  } catch (error) {
    console.error('用户添加出现异常', error)
    message.error('用户添加出现异常，请稍后再试')
  }
}

// 新增：取消添加用户的方法，用于关闭模态框并重置表单数据（可根据需求选择是否重置表单数据）
const cancelAddUser = () => {
  open.value = false
  // 可选：重置表单数据，例如
  addUserForm.userAccount = ''
  addUserForm.userName = ''
  addUserForm.userProfile = ''
  addUserForm.userRole = ''
}

// 新增：处理排序按钮点击事件，切换排序顺序并刷新数据
const toggleSortOrder = () => {
  sortOrder.value = sortOrder.value === 'ascend' ? 'descend' : 'ascend'
  searchParams.sortOrder = sortOrder.value
  fetchData()
}

// 用于存储表格行选择状态的响应式对象
const state = reactive({
  selectedRowKeys: [] as number[],
  loading: false,
})
const onSelectChange = (selectedRowKeys: number[]) => {
  state.selectedRowKeys = selectedKeys
}
// 计算属性，判断是否有选中的行，用于控制批量删除按钮是否可用
const hasSelected = computed(() => state.selectedRowKeys.length > 0)
// 新增：处理批量删除选中图片的方法
const batchDeleteSelectedPictures = async () => {
  if (state.selectedRowKeys.length === 0) {
    message.warning('请先选择要删除的图片')
    return
  }
  const selectedPictureIds = state.selectedRowKeys
  try {
    const res = await batchDeleteUserUsingPost(selectedPictureIds)
    if (res.data.code === 0) {
      message.success('批量删除图片成功')
      state.selectedRowKeys = []
      fetchData()
    } else {
      message.error('批量删除图片失败')
    }
  } catch (error) {
    message.error('批量删除图片出现异常，请稍后再试')
  }
}
const rowSelection = computed(() => {
  return {
    selectedRowKeys: state.selectedRowKeys,
    onChange: onSelectChange,
    renderCell: (checked, record, index, originNode) => {
      if (record.userRole === 'admin') {
        // 如果用户角色是管理员，禁用复选框
        return null
      }
      return originNode
    },
  }
})

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

// 处理封禁/解禁
const handleBanOrUnban = async (user: API.UserVO) => {
  try {
    const res = await banOrUnbanUserUsingPost({
      userId: user.id,
      isUnban: user.userRole === 'ban'
    })

    if (res.data?.code === 0) {
      message.success(user.userRole === 'ban' ? '解禁成功' : '封禁成功')
      fetchData()
    } else {
      message.error(res.data?.message || '操作失败')
    }
  } catch (error: any) {
    message.error(error.response?.data?.message || '操作失败')
  }
}

// 跳转到用户详情页
const goToUserDetail = (userId: string) => {
  router.push(`/user/${userId}`)
}
</script>

<style scoped>
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

.user-card {
  background: white;
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
  color: #666;
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
  background: white;
  padding: 10px 0;
}

:deep(.van-card__title) {
  font-size: 15px;
  font-weight: 500;
  margin-bottom: 4px;
}

:deep(.van-card__desc) {
  margin-top: 4px;
  color: #666;
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
  background: white;
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
  background: white;
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
  background: white;
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.search-section {
  position: sticky;
  top: -12px; /* 调整搜索栏的粘性定位位置 */
  z-index: 100;
  background: #f7f8fa;
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
  background: white;
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
  background: #f7f8fa;
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

/* 封禁按钮样式 */
.ban-button {
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
  background: #ff4d4f;
  border-color: #ff4d4f;
  color: white;

  &:hover {
    background: #ff7875;
    border-color: #ff7875;
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(255, 77, 79, 0.2);
  }

  &:active {
    background: #d9363e;
    border-color: #d9363e;
    transform: translateY(1px);
    box-shadow: none;
  }
}

/* 解禁按钮样式 */
.unban-button {
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
  background: #10b981;
  border-color: #10b981;
  color: white;

  &:hover {
    background: #34d399;
    border-color: #34d399;
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(16, 185, 129, 0.2);
  }

  &:active {
    background: #059669;
    border-color: #059669;
    transform: translateY(1px);
    box-shadow: none;
  }
}

/* 移动端按钮样式 */
.card-footer {
  .ban-button,
  .unban-button {
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

/* 添加移动端邮箱样式 */
.user-email {
  font-size: 13px;
  color: #666;
  margin-top: 4px;
  display: flex;
  align-items: center;
  gap: 6px;

  .email-icon {
    font-size: 14px;
    color: #999;
  }
}

.user-basic-info {
  .user-name {
    font-size: 16px;
    font-weight: 500;
    color: #1a1a1a;
  }

  .user-account {
    font-size: 13px;
    color: #666;
    margin-top: 2px;
  }
}

/* 移动端邮箱标签样式 */
.user-email-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  margin-top: 8px;
  background: #f5f5f5;
  border-radius: 12px;
  font-size: 12px;
  color: #666;

  .email-icon {
    font-size: 12px;
    color: #999;
  }
}
</style>
