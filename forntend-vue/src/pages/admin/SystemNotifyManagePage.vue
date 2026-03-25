<template>
  <div id="systemNotifyManagePage">
    <!-- PC端展示 -->
    <template v-if="device === DEVICE_TYPE_ENUM.PC">
      <div class="pc-container">
        <!-- 搜索表单 -->
        <div class="search-and-button-container">
          <a-form layout="inline" :model="searchParams" @finish="doSearch">
            <a-form-item label="通知类型">
              <a-select
                v-model:value="searchParams.notifyType"
                placeholder="请选择通知类型"
                allow-clear
                style="width: 150px"
              >
                <a-select-option value="ADMIN_ANNOUNCE">管理员公告</a-select-option>
                <a-select-option value="POST_SELECTED">帖子精选</a-select-option>
                <a-select-option value="POST_DELETED">帖子删除</a-select-option>
                <a-select-option value="POST_UPDATED">帖子修改</a-select-option>
                <a-select-option value="ACCOUNT_CHANGED">账号变更</a-select-option>
                <a-select-option value="SYSTEM_ALERT">系统告警</a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="接收者类型">
              <a-select
                v-model:value="searchParams.receiverType"
                placeholder="请选择接收者类型"
                allow-clear
                style="width: 150px"
              >
                <a-select-option value="ALL_USER">全体用户</a-select-option>
                <a-select-option value="SPECIFIC_USER">指定用户</a-select-option>
                <a-select-option value="ROLE">按角色</a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="状态">
              <a-select
                v-model:value="searchParams.isEnabled"
                placeholder="请选择状态"
                allow-clear
                style="width: 100px"
              >
                <a-select-option :value="1">有效</a-select-option>
                <a-select-option :value="0">无效</a-select-option>
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
            <a-button
              style="margin-left: 20px"
              type="primary"
              class="action-button primary-button"
              @click="openModal"
            >
              <PlusOutlined />
              <span class="button-text">添加通知</span>
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
                <template v-if="column.dataIndex === 'notifyType'">
                  <a-tag v-if="record.notifyType === 'ADMIN_ANNOUNCE'" color="blue">管理员公告</a-tag>
                  <a-tag v-else-if="record.notifyType === 'POST_SELECTED'" color="green">帖子精选</a-tag>
                  <a-tag v-else-if="record.notifyType === 'POST_DELETED'" color="red">帖子删除</a-tag>
                  <a-tag v-else-if="record.notifyType === 'POST_UPDATED'" color="orange">帖子修改</a-tag>
                  <a-tag v-else-if="record.notifyType === 'ACCOUNT_CHANGED'" color="purple">账号变更</a-tag>
                  <a-tag v-else-if="record.notifyType === 'SYSTEM_ALERT'" color="volcano">系统告警</a-tag>
                  <span v-else>{{ record.notifyType }}</span>
                </template>
                <template v-else-if="column.dataIndex === 'receiverType'">
                  <span v-if="record.receiverType === 'ALL_USER'">全体用户</span>
                  <span v-else-if="record.receiverType === 'SPECIFIC_USER'">指定用户</span>
                  <span v-else-if="record.receiverType === 'ROLE'">按角色</span>
                  <span v-else>{{ record.receiverType }}</span>
                </template>
                <template v-else-if="column.dataIndex === 'isGlobal'">
                  <a-tag v-if="record.isGlobal === 1" color="green">是</a-tag>
                  <a-tag v-else color="red">否</a-tag>
                </template>
                <template v-else-if="column.dataIndex === 'isEnabled'">
                  <a-tag v-if="record.isEnabled === 1" color="green">有效</a-tag>
                  <a-tag v-else color="red">无效</a-tag>
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
                <template v-else-if="column.dataIndex === 'title'">
                  <a-tooltip :title="record.title">
                    <div class="ellipsis-text">
                      {{ record.title }}
                    </div>
                  </a-tooltip>
                </template>
                <template v-else-if="column.key === 'action'">
                  <div class="action-buttons">
                    <!-- 删除按钮（移除了编辑按钮） -->
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

      <!-- 添加通知模态框（移除了编辑功能） -->
      <a-modal
        v-model:open="modalVisible"
        title="添加通知"
        @ok="handleModalOk"
        @cancel="handleModalCancel"
        :confirm-loading="modalConfirmLoading"
      >
        <a-form
          :model="modalForm"
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 18 }"
          ref="modalFormRef"
        >
          <a-form-item
            label="通知类型"
            name="notifyType"
            :rules="[{ required: true, message: '请选择通知类型' }]"
          >
            <a-select v-model:value="modalForm.notifyType" placeholder="请选择通知类型">
              <a-select-option value="ADMIN_ANNOUNCE">管理员公告</a-select-option>
              <a-select-option value="POST_SELECTED">帖子精选</a-select-option>
              <a-select-option value="POST_DELETED">帖子删除</a-select-option>
              <a-select-option value="POST_UPDATED">帖子修改</a-select-option>
              <a-select-option value="ACCOUNT_CHANGED">账号变更</a-select-option>
              <a-select-option value="SYSTEM_ALERT">系统告警</a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item
            label="接收者类型"
            name="receiverType"
            :rules="[{ required: true, message: '请选择接收者类型' }]"
          >
            <a-select v-model:value="modalForm.receiverType" placeholder="请选择接收者类型">
              <a-select-option value="ALL_USER">全体用户</a-select-option>
              <a-select-option value="SPECIFIC_USER">指定用户</a-select-option>
              <a-select-option value="ROLE">按角色</a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item
            v-if="modalForm.receiverType === 'SPECIFIC_USER'"
            label="接收者ID"
            name="receiverId"
            :rules="[{ required: modalForm.receiverType === 'SPECIFIC_USER', message: '请输入接收者ID' }]"
          >
            <a-input v-model:value="modalForm.receiverId" placeholder="请输入接收者ID" />
          </a-form-item>

          <a-form-item
            v-if="modalForm.receiverType === 'ROLE'"
            label="角色编码"
            name="receiverId"
            :rules="[{ required: modalForm.receiverType === 'ROLE', message: '请输入角色编码' }]"
          >
            <a-input v-model:value="modalForm.receiverId" placeholder="请输入角色编码（如USER/VIP/ADMIN）" />
          </a-form-item>

          <a-form-item
            label="标题"
            name="title"
            :rules="[{ required: true, message: '请输入标题' }]"
          >
            <a-input v-model:value="modalForm.title" placeholder="请输入标题" />
          </a-form-item>

          <a-form-item
            label="内容"
            name="content"
            :rules="[{ required: true, message: '请输入内容' }]"
          >
            <a-textarea
              v-model:value="modalForm.content"
              placeholder="请输入内容"
              :rows="4"
            />
          </a-form-item>

          <a-form-item label="是否全局">
            <a-switch
              v-model:checked="modalForm.isGlobal"
              checked-children="是"
              un-checked-children="否"
            />
          </a-form-item>

          <a-form-item label="是否有效">
            <a-switch
              v-model:checked="modalForm.isEnabled"
              checked-children="有效"
              un-checked-children="无效"
              :default-checked="true"
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
            <van-dropdown-menu>
              <van-dropdown-item
                v-model="mobileSearchParams.notifyType"
                :options="notifyTypeOptions"
                @change="onMobileSearch"
              />
              <van-dropdown-item
                v-model="mobileSearchParams.receiverType"
                :options="receiverTypeOptions"
                @change="onMobileSearch"
              />
              <van-dropdown-item
                v-model="mobileSearchParams.isEnabled"
                :options="statusOptions"
                @change="onMobileSearch"
              />
            </van-dropdown-menu>
          </div>

          <!-- 操作按钮区 -->
          <div class="action-bar">
            <van-button
              type="primary"
              size="small"
              icon="plus"
              @click="openModal"
              class="add-notify-button"
            >添加通知</van-button>
          </div>

          <!-- 通知列表 -->
          <div class="notify-list">
            <van-cell-group inset v-for="notify in dataList" :key="notify.id">
              <van-card
                :title="notify.title"
                :desc="getNotifyTypeName(notify.notifyType)"
                class="notify-card"
              >
                <template #tags>
                  <van-tag
                    :type="getReceiverTypeTagType(notify.receiverType)"
                    round
                    size="small"
                    style="margin-right: 4px"
                  >
                    {{ getReceiverTypeName(notify.receiverType) }}
                  </van-tag>
                  <van-tag
                    :type="notify.isGlobal === 1 ? 'primary' : 'default'"
                    round
                    size="small"
                    style="margin-right: 4px"
                  >
                    {{ notify.isGlobal === 1 ? '全局' : '非全局' }}
                  </van-tag>
                  <van-tag
                    :type="notify.isEnabled === 1 ? 'success' : 'danger'"
                    round
                    size="small"
                  >
                    {{ notify.isEnabled === 1 ? '有效' : '无效' }}
                  </van-tag>
                </template>

                <template #price>
                  <span class="create-time">
                    {{ dayjs(notify.createTime).format('YYYY-MM-DD HH:mm') }}
                  </span>
                </template>

                <template #footer>
                  <div class="card-footer">
                    <van-button
                      size="mini"
                      type="danger"
                      @click="showDeleteConfirm(notify)"
                      class="delete-button mobile-delete-button"
                    >删除</van-button>
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

          <!-- 每页条数选择器抽屉 -->
          <van-action-sheet
            v-model:show="showPageSizeSheet"
            :actions="mobilePageSizeOptions"
            cancel-text="取消"
            close-on-click-action
            @select="handlePageSizeChange"
          />
        </div>
      </div>
    </template>

    <!-- 添加通知模态框（移除了编辑功能） -->
    <a-modal
      v-model:open="modalVisible"
      title="添加通知"
      @ok="handleModalOk"
      @cancel="handleModalCancel"
      :confirm-loading="modalConfirmLoading"
    >
      <a-form
        :model="modalForm"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 18 }"
        ref="modalFormRef"
      >
        <a-form-item
          label="通知类型"
          name="notifyType"
          :rules="[{ required: true, message: '请选择通知类型' }]"
        >
          <a-select v-model:value="modalForm.notifyType" placeholder="请选择通知类型">
            <a-select-option value="ADMIN_ANNOUNCE">管理员公告</a-select-option>
            <a-select-option value="POST_SELECTED">帖子精选</a-select-option>
            <a-select-option value="POST_DELETED">帖子删除</a-select-option>
            <a-select-option value="POST_UPDATED">帖子修改</a-select-option>
            <a-select-option value="ACCOUNT_CHANGED">账号变更</a-select-option>
            <a-select-option value="SYSTEM_ALERT">系统告警</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item
          label="接收者类型"
          name="receiverType"
          :rules="[{ required: true, message: '请选择接收者类型' }]"
        >
          <a-select v-model:value="modalForm.receiverType" placeholder="请选择接收者类型">
            <a-select-option value="ALL_USER">全体用户</a-select-option>
            <a-select-option value="SPECIFIC_USER">指定用户</a-select-option>
            <a-select-option value="ROLE">按角色</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item
          v-if="modalForm.receiverType === 'SPECIFIC_USER'"
          label="接收者ID"
          name="receiverId"
          :rules="[{ required: modalForm.receiverType === 'SPECIFIC_USER', message: '请输入接收者ID' }]"
        >
          <a-input v-model:value="modalForm.receiverId" placeholder="请输入接收者ID" />
        </a-form-item>

        <a-form-item
          v-if="modalForm.receiverType === 'ROLE'"
          label="角色编码"
          name="receiverId"
          :rules="[{ required: modalForm.receiverType === 'ROLE', message: '请输入角色编码' }]"
        >
          <a-input v-model:value="modalForm.receiverId" placeholder="请输入角色编码（如USER/VIP/ADMIN）" />
        </a-form-item>

        <a-form-item
          label="标题"
          name="title"
          :rules="[{ required: true, message: '请输入标题' }]"
        >
          <a-input v-model:value="modalForm.title" placeholder="请输入标题" />
        </a-form-item>

        <a-form-item
          label="内容"
          name="content"
          :rules="[{ required: true, message: '请输入内容' }]"
        >
          <a-textarea
            v-model:value="modalForm.content"
            placeholder="请输入内容"
            :rows="4"
          />
        </a-form-item>

        <a-form-item label="是否全局">
          <a-switch
            v-model:checked="modalForm.isGlobal"
            checked-children="是"
            un-checked-children="否"
          />
        </a-form-item>

        <a-form-item label="是否有效">
          <a-switch
            v-model:checked="modalForm.isEnabled"
            checked-children="有效"
            un-checked-children="无效"
            :default-checked="true"
          />
        </a-form-item>
      </a-form>
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
        <h3 class="confirm-title">确认删除该通知？</h3>
        <p class="confirm-desc">
          标题：{{ selectedNotify?.title }}<br>
          类型：{{ getNotifyTypeName(selectedNotify?.notifyType) }}<br>
          创建时间：{{ dayjs(selectedNotify?.createTime).format('YYYY-MM-DD HH:mm:ss') }}
        </p>
        <div class="confirm-actions">
          <a-button class="cancel-button" @click="deleteConfirmVisible = false">取消</a-button>
          <a-button class="confirm-button" danger @click="confirmDelete">确认删除</a-button>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, watch } from 'vue';
import { message, Modal, Form } from 'ant-design-vue';
import dayjs from 'dayjs';
import {
  listSystemNotifiesUsingGet,
  addSystemNotifyUsingPost,
  deleteSystemNotifyUsingDelete
} from '@/api/systemNotifyController';
import { DEVICE_TYPE_ENUM } from '@/constants/device';
import { SearchOutlined, PlusOutlined, DeleteOutlined, ExclamationCircleFilled } from '@ant-design/icons-vue';
import { useLoginUserStore } from '@/stores/useLoginUserStore';
import { getDeviceType } from '@/utils/device';

// 添加 Vant UI 组件导入
import { DropdownMenu, DropdownItem, ActionSheet } from 'vant';
import 'vant/lib/dropdown-menu/style';
import 'vant/lib/dropdown-item/style';
import 'vant/lib/action-sheet/style';

const device = ref<string>(DEVICE_TYPE_ENUM.PC);

// 页面加载时获取设备类型
onMounted(async () => {
  device.value = await getDeviceType();
  // 延迟一小段时间确保设备类型设置完成后再加载数据
  setTimeout(() => {
    loadData();
  }, 100);
});

// 搜索参数
const searchParams = reactive({
  current: 1,
  pageSize: 10,
  sortField: 'createTime',
  sortOrder: 'descend',
  notifyType: undefined,
  receiverType: undefined,
  isEnabled: undefined,
});

// 表格数据
const dataList = ref<API.SystemNotify[]>([]);
const total = ref(0);
const loading = ref(false);

// 表格列定义
const columns = [
  {
    title: 'ID',
    dataIndex: 'id',
    key: 'id',
    sorter: true,
  },
  {
    title: '通知类型',
    dataIndex: 'notifyType',
    key: 'notifyType',
    sorter: true,
  },
  {
    title: '标题',
    dataIndex: 'title',
    key: 'title',
    sorter: true,
  },
  {
    title: '接收者类型',
    dataIndex: 'receiverType',
    key: 'receiverType',
  },
  {
    title: '是否全局',
    dataIndex: 'isGlobal',
    key: 'isGlobal',
  },
  {
    title: '状态',
    dataIndex: 'isEnabled',
    key: 'isEnabled',
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    key: 'createTime',
    sorter: true,
  },
  {
    title: '操作',
    key: 'action',
    fixed: 'right',
    width: 150,
  },
];

// 表格选择
const selectedRowKeys = ref<number[]>([]);
const rowSelection = ref({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: number[]) => {
    selectedRowKeys.value = keys;
  },
});

// 分页配置
const pcPageSizeOptions = ref(['10', '20', '50', '100']);

// 模态框相关
const modalVisible = ref(false);
const modalConfirmLoading = ref(false);
const modalFormRef = ref<InstanceType<typeof Form> | null>(null);

// 模态框表单数据
const modalForm: UnwrapRef<{
  notifyType: string | undefined;
  receiverType: string | undefined;
  receiverId: string | undefined;
  title: string | undefined;
  content: string | undefined;
  isGlobal: boolean;
  isEnabled: boolean;
}> = reactive({
  notifyType: undefined,
  receiverType: undefined,
  receiverId: undefined,
  title: undefined,
  content: undefined,
  isGlobal: false,
  isEnabled: true,
});

// 监听接收者类型变化
watch(() => modalForm.receiverType, (newVal) => {
  if (newVal !== 'SPECIFIC_USER' && newVal !== 'ROLE') {
    modalForm.receiverId = undefined;
  }
});

// 获取数据列表
const loadData = async () => {
  loading.value = true;
  try {
    const res = await listSystemNotifiesUsingGet({
      ...searchParams,
      isEnabled: searchParams.isEnabled !== undefined ? searchParams.isEnabled : undefined,
    });
    if (res.data?.code === 0) {
      dataList.value = res.data.data?.records || [];
      total.value = res.data.data?.total || 0;
    } else {
      message.error(res.data?.message || '获取数据失败');
    }
  } catch (error) {
    console.error('获取系统通知列表失败:', error);
    message.error('获取数据失败');
  } finally {
    loading.value = false;
  }
};

// 搜索
const doSearch = () => {
  searchParams.current = 1;
  loadData();
};

// 表格分页变化
const onPageChange = (page: number, pageSize: number) => {
  searchParams.current = page;
  searchParams.pageSize = pageSize;
  loadData();
};

// 分页大小变化
const onShowSizeChange = (current: number, size: number) => {
  searchParams.current = 1;
  searchParams.pageSize = size;
  loadData();
};

// 表格排序变化
const doTableChange = (pagination: any, filters: any, sorter: any) => {
  if (sorter.field && sorter.order) {
    searchParams.sortField = sorter.field;
    searchParams.sortOrder = sorter.order === 'ascend' ? 'ascend' : 'descend';
  } else {
    searchParams.sortField = 'createTime';
    searchParams.sortOrder = 'descend';
  }
  loadData();
};

// 打开模态框
const openModal = () => {
  console.log('点击了添加通知按钮');
  // 重置表单
  modalForm.notifyType = undefined;
  modalForm.receiverType = undefined;
  modalForm.receiverId = undefined;
  modalForm.title = undefined;
  modalForm.content = undefined;
  modalForm.isGlobal = false;
  modalForm.isEnabled = true;
  modalVisible.value = true;
};

// 模态框确认
const handleModalOk = async () => {
  try {
    await modalFormRef.value?.validateFields();

    modalConfirmLoading.value = true;

    // 构造请求参数
    const requestData: API.SystemNotifyAddRequest = {
      notifyType: modalForm.notifyType,
      receiverType: modalForm.receiverType,
      receiverId: modalForm.receiverType === 'SPECIFIC_USER' || modalForm.receiverType === 'ROLE' ? modalForm.receiverId : undefined,
      title: modalForm.title,
      content: modalForm.content,
      isGlobal: modalForm.isGlobal ? 1 : 0,
      isEnabled: modalForm.isEnabled ? 1 : 0,
      // 自动填充发送者信息（管理员操作）
      senderType: 'ADMIN',
      senderId: loginUserStore.loginUser?.id?.toString() || 'ADMIN', // 使用实际的管理员ID
    };

    // 新增
    const res = await addSystemNotifyUsingPost(requestData);

    if (res.data?.code === 0) {
      message.success('添加成功');
      modalVisible.value = false;
      await loadData();
    } else {
      message.error(res.data?.message || '添加失败');
    }
  } catch (error: any) {
    if (error.errorFields) {
      // 表单验证失败，不显示错误消息
      return;
    }
    console.error('添加失败:', error);
    message.error('添加失败');
  } finally {
    modalConfirmLoading.value = false;
  }
};

// 模态框取消
const handleModalCancel = () => {
  modalVisible.value = false;
};

// 删除确认相关
const deleteConfirmVisible = ref(false);
const selectedNotify = ref<API.SystemNotify | null>(null);

// 显示删除确认框
const showDeleteConfirm = (notify: API.SystemNotify) => {
  selectedNotify.value = notify;
  deleteConfirmVisible.value = true;
};

// 确认删除
const confirmDelete = async () => {
  if (!selectedNotify.value?.id) return;

  try {
    const res = await deleteSystemNotifyUsingDelete({ id: selectedNotify.value.id || 0 });
    if (res.data?.code === 0) {
      message.success('删除成功');
      deleteConfirmVisible.value = false;
      await loadData();
    } else {
      message.error(res.data?.message || '删除失败');
    }
  } catch (error) {
    console.error('删除失败:', error);
    message.error('删除失败');
  }
};

// 获取登录用户状态
const loginUserStore = useLoginUserStore();

// 移动端相关数据
const mobileSearchParams = reactive({
  notifyType: '',
  receiverType: '',
  isEnabled: '',
});

const jumpPage = ref('');
const showPageSizeSheet = ref(false);

// 移动端下拉选项
const notifyTypeOptions = [
  { text: '全部类型', value: '' },
  { text: '管理员公告', value: 'ADMIN_ANNOUNCE' },
  { text: '帖子精选', value: 'POST_SELECTED' },
  { text: '帖子删除', value: 'POST_DELETED' },
  { text: '帖子修改', value: 'POST_UPDATED' },
  { text: '账号变更', value: 'ACCOUNT_CHANGED' },
  { text: '系统告警', value: 'SYSTEM_ALERT' },
];

const receiverTypeOptions = [
  { text: '全部类型', value: '' },
  { text: '全体用户', value: 'ALL_USER' },
  { text: '指定用户', value: 'SPECIFIC_USER' },
  { text: '按角色', value: 'ROLE' },
];

const statusOptions = [
  { text: '全部状态', value: '' },
  { text: '有效', value: 1 },
  { text: '无效', value: 0 },
];

// 移动端分页选项
const mobilePageSizeOptions = [
  { name: '10条/页', value: 10 },
  { name: '20条/页', value: 20 },
  { name: '30条/页', value: 30 },
  { name: '50条/页', value: 50 },
];

// 获取通知类型名称
const getNotifyTypeName = (type: string | undefined) => {
  const map: Record<string, string> = {
    'ADMIN_ANNOUNCE': '管理员公告',
    'POST_SELECTED': '帖子精选',
    'POST_DELETED': '帖子删除',
    'POST_UPDATED': '帖子修改',
    'ACCOUNT_CHANGED': '账号变更',
    'SYSTEM_ALERT': '系统告警'
  };
  return type ? map[type] || type : '';
};

// 获取接收者类型名称
const getReceiverTypeName = (type: string | undefined) => {
  const map: Record<string, string> = {
    'ALL_USER': '全体用户',
    'SPECIFIC_USER': '指定用户',
    'ROLE': '按角色'
  };
  return type ? map[type] || type : '';
};

// 获取接收者类型标签类型
const getReceiverTypeTagType = (type: string | undefined) => {
  const map: Record<string, string> = {
    'ALL_USER': 'primary',
    'SPECIFIC_USER': 'success',
    'ROLE': 'warning'
  };
  return type ? map[type] || 'default' : 'default';
};

// 移动端搜索
const onMobileSearch = () => {
  searchParams.notifyType = mobileSearchParams.notifyType || undefined;
  searchParams.receiverType = mobileSearchParams.receiverType || undefined;
  searchParams.isEnabled = mobileSearchParams.isEnabled !== '' ? mobileSearchParams.isEnabled : undefined;
  searchParams.current = 1;
  loadData();
};

// 移动端分页处理
const onMobilePageChange = (page: number) => {
  searchParams.current = page;
  loadData();
};

// 处理每页条数改变
const handlePageSizeChange = (action: { value: number }) => {
  searchParams.current = 1;
  searchParams.pageSize = action.value;
  loadData();
};

// 处理页码跳转
const handleJumpPage = () => {
  const page = parseInt(jumpPage.value);
  if (isNaN(page)) {
    return;
  }

  const maxPage = Math.ceil(total.value / searchParams.pageSize);
  if (page < 1 || page > maxPage) {
    message.warning(`请输入1-${maxPage}之间的页码`);
    return;
  }

  searchParams.current = page;
  loadData();
  jumpPage.value = '';
};

// 初始化加载数据
onMounted(() => {
  loadData();
});
</script>

<style scoped>
#systemNotifyManagePage {
  padding: 20px;
  background-color: var(--background-color);
  min-height: calc(100vh - 64px);
}

.pc-container {
  background: var(--card-background);
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.search-and-button-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 10px;
}

.table-section {
  margin-bottom: 20px;
}

.custom-table :deep(.ant-table-thead > tr > th) {
  background-color: var(--table-header-background);
  color: var(--text-primary);
}

.custom-table :deep(.ant-table-tbody > tr:hover > td) {
  background-color: var(--table-row-hover);
}

.custom-table :deep(.ant-table-tbody > tr.ant-table-row-selected > td) {
  background-color: var(--table-row-selected);
}

.ellipsis-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.action-buttons {
  display: flex;
  gap: 8px;
}

.table-button {
  padding: 4px 8px;
}

.mz-antd-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

@media (max-width: 768px) {
  .search-and-button-container {
    flex-direction: column;
    align-items: stretch;
  }

  #systemNotifyManagePage {
    padding: 10px;
  }

  .pc-container {
    padding: 15px;
  }
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
  padding-bottom: 50px;
}

.mobile-content {
  height: 100%;
  overflow-y: auto;
  padding: 12px;
  -webkit-overflow-scrolling: touch;
  padding-bottom: 80px;
}

.search-section {
  margin-bottom: 12px;
}

.action-bar {
  padding: 0 12px 12px;
  display: flex;
  justify-content: flex-end;
}

.notify-card {
  background: var(--header-background);
  color: var(--text-primary);
  border-radius: 8px;
  overflow: hidden;
  padding: 12px;
  margin-bottom: 12px;
}

.notify-card:last-child {
  margin-bottom: 0;
}

.card-footer {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-top: 4px;
}

.create-time {
  font-size: 12px;
  color: #999;
}

/* 移动端分页样式 */
.mobile-pagination {
  margin-top: 16px;
  padding: 12px;
  background: var(--header-background);
  color: var(--text-primary);
  border-radius: 8px;
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
  cursor: pointer;
}

.pagination-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.jump-page {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #666;
}

.delete-button {
  color: #ff4d4f;
  border-color: #ff4d4f;
}

.mobile-delete-button {
  color: #ff4d4f !important;
  border-color: #ff4d4f !important;
  font-weight: bold;
}

.add-notify-button {
  font-weight: bold;
}

/* 删除确认弹框样式 */
.delete-confirm-modal :deep(.ant-modal-content) {
  padding: 0;
  border-radius: 16px;
  overflow: hidden;
}

.delete-confirm-content {
  padding: 32px 24px;
  text-align: center;
}

.warning-icon {
  font-size: 48px;
  margin-bottom: 16px;
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
}
</style>
