<template>
  <div id="knowledgeFileManagePage">
    <!-- PC端展示 -->
    <template v-if="device === 'pc'">
      <div class="pc-container">
        <!-- 搜索表单和操作按钮 -->
        <div class="search-and-button-container">
          <a-form layout="inline" :model="searchParams" @finish="doSearch">
            <a-form-item label="文件名">
              <a-input
                v-model:value="searchParams.originalName"
                placeholder="请输入原始文件名"
                allow-clear
                class="compact-input"
              />
            </a-form-item>
            <a-form-item label="文件类型">
              <a-select
                v-model:value="searchParams.fileType"
                class="compact-select"
                placeholder="请选择文件类型"
                allow-clear
              >
                <a-select-option value="pdf">PDF</a-select-option>
                <a-select-option value="txt">TXT</a-select-option>
                <a-select-option value="docx">DOCX</a-select-option>
                <a-select-option value="md">MD</a-select-option>
              </a-select>
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
            <a-upload
              name="file"
              :multiple="true"
              :action="uploadUrl"
              :headers="uploadHeaders"
              :showUploadList="false"
              @change="handleUploadChange"
            >
              <a-button type="primary" class="action-button create-button">
                <UploadOutlined />
                上传知识库文件
              </a-button>
            </a-upload>
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
                  <a-menu-item key="delete" @click="handleBatchDelete">
                    <DeleteOutlined />
                    批量删除
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
              <template v-if="column.dataIndex === 'originalName'">
                <a-tooltip :title="record.originalName">
                  <span>{{ truncateText(record.originalName, 30) }}</span>
                </a-tooltip>
              </template>
              <template v-if="column.dataIndex === 'fileUrl'">
                <a :href="record.fileUrl" target="_blank" rel="noopener noreferrer">
                  <PaperClipOutlined />
                  查看文件
                </a>
              </template>
              <template v-if="column.dataIndex === 'fileSizeDisplay'">
                {{ record.fileSizeDisplay }}
              </template>
              <template v-if="column.dataIndex === 'fileType'">
                <a-tag :color="getFileTypeColor(record.fileType)">
                  {{ record.fileType.toUpperCase() }}
                </a-tag>
              </template>
              <template v-if="column.dataIndex === 'vectorCount'">
                <a-tag color="blue">
                  {{ record.vectorCount || 0 }} 个向量
                </a-tag>
              </template>
              <template v-if="column.dataIndex === 'userName'">
                <a-tag color="green">
                  {{ record.userId|| '未知用户' }}
                </a-tag>
              </template>
              <template v-if="column.dataIndex === 'uploadTime'">
                {{ dayjs(record.uploadTime).format('YYYY-MM-DD HH:mm:ss') }}
              </template>
              <template v-if="column.dataIndex === 'createTime'">
                {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}
              </template>
              <template v-if="column.key === 'action'">
                <a-space wrap>
                  <a-button
                    type="primary"
                    class="table-button view-button"
                    @click="handleView(record)"
                  >
                    <EyeOutlined />
                    查看
                  </a-button>
                  <a-button
                    type="primary"
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
              v-model="searchParams.originalName"
              placeholder="搜索文件名"
              @search="doSearch"
            />
          </div>

          <!-- 操作按钮区 -->
          <div class="action-bar">
            <a-upload
              name="file"
              :multiple="true"
              :action="uploadUrl"
              :headers="uploadHeaders"
              :showUploadList="false"
              @change="handleUploadChange"
            >
              <a-button type="primary" class="action-button create-button">
                <UploadOutlined />
                上传知识库文件
              </a-button>
            </a-upload>
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

          <!-- 文件列表 -->
          <div class="file-list">
            <van-checkbox-group v-model="state.selectedRowKeys">
              <van-cell-group inset v-for="file in dataList" :key="file.id">
                <van-card class="file-card">
                  <template #thumb>
                    <div class="file-icon" :class="getFileIconClass(file.fileType)">
                      <FileTextOutlined v-if="['txt', 'md'].includes(file.fileType)" />
                      <FilePdfOutlined v-else-if="file.fileType === 'pdf'" />
                      <FileWordOutlined v-else-if="file.fileType === 'docx'" />
                      <FileOutlined v-else />
                    </div>
                  </template>

                  <template #title>
                    <div class="card-title">{{ truncateText(file.originalName, 20) }}</div>
                  </template>

                  <template #desc>
                    <div class="card-desc">
                      <div class="desc-item">
                        <span class="label">类型:</span> {{ file.fileType.toUpperCase() }}
                      </div>
                      <div class="desc-item">
                        <span class="label">大小:</span> {{ file.fileSizeDisplay }}
                      </div>
                      <div class="desc-item">
                        <span class="label">向量数:</span> {{ file.vectorCount || 0 }}
                      </div>
                      <div class="desc-item" v-if="file.uploadTime">
                        <span class="label">上传时间:</span>
                        {{ dayjs(file.uploadTime).format('MM-DD HH:mm') }}
                      </div>
                    </div>
                  </template>

                  <template #tags>
                    <div class="tag-container">
                      <van-tag type="primary" round size="small">
                        {{ file.userId|| '未知用户' }}
                      </van-tag>
                    </div>
                  </template>

                  <template #footer>
                    <div class="card-footer">
                      <van-checkbox :name="file.id" class="card-checkbox" />
                      <div class="button-group">
                        <van-button
                          size="mini"
                          type="primary"
                          @click="handleView(file)"
                        >查看</van-button>
                        <van-button
                          type="danger"
                          size="mini"
                          @click="showDeleteConfirm(file)"
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
        <h3 class="confirm-title">确认删除该知识库文件？</h3>
        <p class="confirm-desc">
          文件名：{{ selectedFile?.originalName }}<br>
          文件大小：{{ selectedFile?.fileSizeDisplay }}
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

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { Uploader as VanUploader } from 'vant';
import { DEVICE_TYPE_ENUM } from '@/constants/device';
import {
  listKnowledgeFileVoByPageUsingPost,
  batchDeleteKnowledgeFilesUsingPost
} from '@/api/knowledgeFileController';
import { message } from 'ant-design-vue';
import {
  SearchOutlined,
  UploadOutlined,
  BoldOutlined,
  EyeOutlined,
  DeleteOutlined,
  ExclamationCircleFilled,
  PaperClipOutlined,
  FileTextOutlined,
  FilePdfOutlined,
  FileWordOutlined,
  FileOutlined
} from '@ant-design/icons-vue';
import dayjs from 'dayjs';
import { getDeviceType } from '@/utils/device';

// 定义列配置
const columns = [
  {
    title: '文件ID',
    dataIndex: 'id',
    width: 100,
  },
  {
    title: '原始文件名',
    dataIndex: 'originalName',
    width: 200,
    ellipsis: true,
  },
  {
    title: '文件访问链接',
    dataIndex: 'fileUrl',
  },
  {
    title: '文件大小',
    dataIndex: 'fileSizeDisplay',
    width: 120,
  },
  {
    title: '文件类型',
    dataIndex: 'fileType',
    width: 100,
  },
  {
    title: '向量数量',
    dataIndex: 'vectorCount',
    width: 120,
  },
  {
    title: '上传用户',
    dataIndex: 'userName',
    width: 120,
  },
  {
    title: '上传时间',
    dataIndex: 'uploadTime',
    width: 180,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 180,
  },
  {
    title: '操作',
    key: 'action',
    width: 150,
  },
];

// 定义数据
const dataList = ref([]);
const total = ref(0);

// 搜索条件
const searchParams = reactive({
  current: 1,
  pageSize: 10,
  sortField: 'createTime',
  sortOrder: 'descend',
});

// 用于存储表格行选择状态的响应式对象
const state = reactive({
  selectedRowKeys: [],
  loading: false,
});

// 计算属性，判断是否有选中的行，用于控制批量删除按钮是否可用
const hasSelected = computed(() => state.selectedRowKeys.length > 0);

const loading = ref(false);

// 获取设备类型
const device = ref('');

// 获取文件类型对应的颜色
const getFileTypeColor = (fileType) => {
  const colorMap = {
    pdf: 'red',
    txt: 'blue',
    docx: 'purple',
    md: 'orange'
  };
  return colorMap[fileType] || 'default';
};

// 获取文件图标类
const getFileIconClass = (fileType) => {
  const iconMap = {
    pdf: 'pdf-icon',
    txt: 'txt-icon',
    docx: 'docx-icon',
    md: 'md-icon'
  };
  return iconMap[fileType] || 'default-icon';
};

// 截断文本
const truncateText = (text, maxLength) => {
  if (!text) return '';
  if (text.length <= maxLength) return text;
  return text.substring(0, maxLength) + '...';
};

// 获取数据
const fetchData = async () => {
  if (device.value === 'pc') {
    loading.value = true;
  }

  try {
    const res = await listKnowledgeFileVoByPageUsingPost({
      ...searchParams,
    });
    if (res.data.code === 0 && res.data.data) {
      dataList.value = res.data.data.records || [];
      total.value = res.data.data.total || 0;
    } else {
      message.error('获取数据失败，' + res.data.message);
    }
  } catch (error) {
    message.error('获取数据失败');
  } finally {
    loading.value = false;
  }
};

// 页面加载时获取数据
onMounted(async () => {
  device.value = await getDeviceType();
  fetchData();
});

// 分页参数
const pagination = computed(() => {
  return {
    current: searchParams.current,
    pageSize: searchParams.pageSize,
    total: total.value,
    showSizeChanger: true,
    showTotal: (total) => `共 ${total} 条`,
  };
});

// 表格变化之后，重新获取数据
const doTableChange = (page) => {
  searchParams.current = page.current;
  searchParams.pageSize = page.pageSize;
  fetchData();
};

// 搜索数据
const doSearch = () => {
  searchParams.current = 1;
  fetchData();
};

// 处理行选择变化的回调函数
const onSelectChange = (selectedRowKeys) => {
  state.selectedRowKeys = selectedRowKeys;
};

// 批量删除
const handleBatchDelete = async () => {
  if (state.selectedRowKeys.length === 0) {
    message.warning('请先选择要删除的文件');
    return;
  }

  try {
    const res = await batchDeleteKnowledgeFilesUsingPost(state.selectedRowKeys);
    if (res.data.code === 0) {
      message.success('批量删除成功');
      state.selectedRowKeys = [];
      fetchData();
    } else {
      message.error('批量删除失败：' + res.data.message);
    }
  } catch (error) {
    message.error('批量删除出现异常，请稍后再试');
  }
};

// 查看文件
const handleView = (record) => {
  if (record.fileUrl) {
    window.open(record.fileUrl, '_blank');
  } else {
    message.warning('文件链接不可用');
  }
};

// 删除确认相关的状态
const deleteConfirmVisible = ref(false);
const selectedFile = ref(null);

// 显示删除确认框
const showDeleteConfirm = (file) => {
  selectedFile.value = file;
  deleteConfirmVisible.value = true;
};

// 确认删除
const confirmDelete = async () => {
  if (!selectedFile.value?.id) return;

  try {
    // 使用批量删除接口删除单个文件
    const res = await batchDeleteKnowledgeFilesUsingPost([selectedFile.value.id]);
    if (res.data.code === 0) {
      message.success('删除成功');
      deleteConfirmVisible.value = false;
      // 刷新数据
      fetchData();
    } else {
      message.error('删除失败：' + res.data.message);
    }
  } catch (error) {
    message.error('删除失败，请稍后重试');
  }
};

// 移动端相关状态
const showActionSheet = ref(false);
const showUploadPanel = ref(false);
const showPageSizeSheet = ref(false);
const pageSizeOptions = [
  { name: '10条/页', value: 10 },
  { name: '20条/页', value: 20 },
  { name: '30条/页', value: 30 },
  { name: '50条/页', value: 50 },
];

// 移动端分页变化
const onMobilePageChange = (page) => {
  searchParams.current = page;
  fetchData();
};

// 修改每页条数改变的处理方法
const handlePageSizeChange = (action) => {
  const value = action.value;
  searchParams.current = 1;
  searchParams.pageSize = value;
  fetchData();
};

// 跳转页码
const jumpPage = ref('');

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
  fetchData();
  jumpPage.value = '';
};

// 上传相关
const uploadUrl = ref(`${location.origin}/api/knowledgeFile/upload`);
const uploadHeaders = ref({
  'Authorization': `Bearer ${localStorage.getItem('token')}`
});

// 上传文件处理
const handleUploadChange = (info) => {
  // 检查文件类型
  const fileName = info.file.name;
  const fileExtension = fileName.split('.').pop().toLowerCase();
  const allowedExtensions = ['pdf', 'txt', 'docx', 'md'];

  if (!allowedExtensions.includes(fileExtension)) {
    message.error(`不支持的文件格式: ${fileExtension}，仅支持 ${allowedExtensions.join(', ')} 格式`);
    return;
  }

  if (info.file.status === 'done') {
    message.success(`${info.file.name} 文件上传成功`);
    fetchData(); // 刷新列表
  } else if (info.file.status === 'error') {
    message.error(`${info.file.name} 文件上传失败`);
  }
};
</script>

<style scoped>
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
  padding-bottom: 8px;
}

.mobile-content {
  height: 100%;
  overflow-y: auto;
  padding: 12px;
  -webkit-overflow-scrolling: touch;
}

.action-bar {
  padding: 12px;
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: flex-end;
  background: var(--header-background);
  color: var(--text-primary);
  border-radius: 8px;
  margin-bottom: 12px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.action-bar .van-button {
  flex: 1;
  max-width: 150px;
}

:deep(.van-cell-group--inset) {
  margin: 0 12px 12px;
}

:deep(.van-cell-group--inset:last-child) {
  margin-bottom: 0;
}

.file-card {
  background: var(--header-background);
  color: var(--text-primary);
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
  color: var(--text-primary)!important;
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
  color: var(--text-primary)!important;
}

.create-time {
  font-size: 12px;
  color: #999;
}

:deep(.van-card__content) {
  padding-left: 8px;
}

/* 移动端搜索区域样式 */
.search-section {
  position: sticky;
  top: -12px;
  z-index: 100;
  background: var(--header-background);
  color: var(--text-primary);
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
  color: var(--text-primary)!important;
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
  background: var(--header-background);
  color: var(--text-primary);
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.pagination-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  color: var(--text-primary)!important;
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
  background: var(--header-background);
  color: var(--text-primary);
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

.file-list {
  margin-bottom: 16px;
}

/* 搜索框样式优化 */
:deep(.van-search) {
  padding: 8px 12px;
  background: transparent;
}

:deep(.van-search__content) {
  border-radius: 8px;
  background: var(--header-background);
  color: var(--text-primary);
  margin: 0 12px;
}

/* 每页条数选择器样式 */
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
  color: var(--text-primary)!important;
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
  background: var(--header-background);
  color: var(--text-primary);
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
  background: var(--header-background);
  color: var(--text-primary);
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
    color: var(--text-primary)!important;
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

.view-button {
  background: linear-gradient(135deg, #91d5ff 0%, #1890ff 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.2);
}

.view-button:hover {
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

/* 文件图标样式 */
.file-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  font-size: 20px;
}

.pdf-icon {
  background-color: #f8f8f8;
  color: #e74c3c;
}

.txt-icon {
  background-color: #f8f8f8;
  color: #3498db;
}

.docx-icon {
  background-color: #f8f8f8;
  color: #2980b9;
}

.md-icon {
  background-color: #f8f8f8;
  color: #34495e;
}

.default-icon {
  background-color: #f8f8f8;
  color: #7f8c8d;
}
</style>
