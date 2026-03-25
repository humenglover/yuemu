<template>
  <div class="bug-report-manage-page">
    <van-nav-bar title="漏洞报告管理" fixed safe-area-inset-top />

    <div class="page-content">
      <van-search
        v-model="searchKeyword"
        placeholder="搜索标题或描述..."
        @search="handleSearch"
        @clear="handleSearch"
        class="search-bar"
      />

      <van-dropdown-menu class="filter-bar">
        <van-dropdown-item
          v-model="filterStatus"
          :options="statusOptions"
          placeholder="筛选状态"
          @change="handleFilterChange"
        />
        <van-button
          type="primary"
          size="small"
          @click="refreshList"
          class="refresh-btn"
        >
          刷新
        </van-button>
      </van-dropdown-menu>

      <van-grid column-num="2" border class="stats-grid">
        <van-grid-item icon="warning-o" :text="`总报告数 ${stats.totalReports || 0}`" />
        <van-grid-item icon="clock-o" :text="`待处理 ${stats.pendingReports || 0}`" color="#faad14" />
        <van-grid-item icon="success-o" :text="`已解决 ${stats.solvedReports || 0}`" color="#52c41a" />
        <van-grid-item icon="info-o" :text="`已忽略 ${stats.ignoredReports || 0}`" color="#176bf2" />
      </van-grid>

      <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
        <van-list
          v-model:loading="loading"
          :finished="finished"
          finished-text="没有更多了"
          @load="loadMore"
          class="report-list"
        >
          <van-cell-group inset>
            <van-cell
              v-for="item in reportList"
              :key="item.id"
              :title="item.title"
              :label="formatDate(item.createTime)"
              is-link
              @click="viewDetail(item)"
              class="report-item"
            >
              <template #right-icon>
                <van-tag :color="getStatusColor(item.status)" size="small">
                  {{ getStatusText(item.status) }}
                </van-tag>
              </template>
              <template #value>
                <div class="report-item-content">
                  <van-tag :color="getPriorityColor(item.priority)" size="mini">
                    {{ getPriorityText(item.priority) }}
                  </van-tag>
                  <p class="report-desc">{{ item.description?.length > 30 ? item.description.substring(0, 30) + '...' : item.description }}</p>
                </div>
              </template>
              <template #footer>
                <div class="report-actions">
                  <van-button
                    size="mini"
                    type="primary"
                    @click.stop="solveReport(item)"
                    :disabled="item.status === 1"
                  >
                    解决
                  </van-button>
                  <van-button
                    size="mini"
                    type="default"
                    @click.stop="ignoreReport(item)"
                    :disabled="item.status === 2"
                  >
                    忽略
                  </van-button>
                  <van-button
                    size="mini"
                    type="danger"
                    @click.stop="deleteReport(item)"
                  >
                    删除
                  </van-button>
                </div>
              </template>
            </van-cell>
          </van-cell-group>
        </van-list>
      </van-pull-refresh>
    </div>

    <van-popup
      v-model:show="detailVisible"
      position="bottom"
      :style="{ height: '80%' }"
      safe-area-inset-bottom
    >
      <van-nav-bar
        title="漏洞报告详情"
        fixed
        slot="title"
        @click-left="detailVisible = false"
      />
      <div class="detail-content" v-if="currentReport">
        <van-cell-group>
          <van-cell title="标题" :value="currentReport.title" />
          <van-cell title="状态">
            <template #value>
              <van-tag :color="getStatusColor(currentReport.status)">
                {{ getStatusText(currentReport.status) }}
              </van-tag>
            </template>
          </van-cell>
          <van-cell title="优先级">
            <template #value>
              <van-tag :color="getPriorityColor(currentReport.priority)">
                {{ getPriorityText(currentReport.priority) }}
              </van-tag>
            </template>
          </van-cell>
          <van-cell title="提交时间" :value="formatDate(currentReport.createTime)" />
          <van-cell v-if="currentReport.solveTime" title="解决时间" :value="formatDate(currentReport.solveTime)" />
          <van-cell title="描述" :value="currentReport.description" />
          <van-cell title="重现步骤">
            <template #value>
              <pre class="step-content">{{ currentReport.reproduceSteps }}</pre>
            </template>
          </van-cell>
          <van-cell title="预期结果" :value="currentReport.expectedResult" />
          <van-cell title="实际结果" :value="currentReport.actualResult" />
          <van-cell title="设备信息" :value="currentReport.deviceInfo" />
          <van-cell title="浏览器信息" :value="currentReport.browserInfo" />
        </van-cell-group>
      </div>
    </van-popup>

    <van-dialog
      v-model:show="confirmDialog"
      :title="dialogTitle"
      :message="dialogMessage"
      show-cancel-button
      @confirm="handleConfirm"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive, computed } from 'vue';
import {
  listBugReportByPageUsingPost,
  solveBugReportUsingPost,
  deleteBugReportUsingPost,
  getBugReportByIdUsingGet,
  ignoreBugReportUsingPost
} from '@/api/bugReportController';

const loading = ref(false);
const refreshing = ref(false);
const finished = ref(false);
const reportList = ref<API.BugReportVO[]>([]);
const searchKeyword = ref('');
const filterStatus = ref<string | null>(null);

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0
});

const stats = ref({
  totalReports: 0,
  pendingReports: 0,
  solvedReports: 0,
  ignoredReports: 0
});

const detailVisible = ref(false);
const currentReport = ref<API.BugReportVO | null>(null);

const confirmDialog = ref(false);
const dialogTitle = ref('');
const dialogMessage = ref('');
let currentOperateRecord = ref<API.BugReportVO | null>(null);
let currentOperateType = ref('');

const statusOptions = [
  { text: '待处理', value: '0' },
  { text: '已解决', value: '1' },
  { text: '已忽略', value: '2' }
];

const fetchReportList = async (isLoadMore = false) => {
  if (!isLoadMore) {
    loading.value = true;
  }

  try {
    const response = await listBugReportByPageUsingPost({
      current: pagination.current,
      pageSize: pagination.pageSize,
      sortField: 'createTime',
      sortOrder: 'desc',
      title: searchKeyword.value || undefined,
      status: filterStatus.value ? parseInt(filterStatus.value) : undefined
    });

    if (response.code === 0 && response.data) {
      const newData = response.data.records || [];
      if (isLoadMore) {
        reportList.value = [...reportList.value, ...newData];
      } else {
        reportList.value = newData;
      }

      pagination.total = response.data.total || 0;
      finished.value = reportList.value.length >= pagination.total;

      stats.value.totalReports = pagination.total;
      stats.value.pendingReports = reportList.value.filter(r => r.status === 0).length;
      stats.value.solvedReports = reportList.value.filter(r => r.status === 1).length;
      stats.value.ignoredReports = reportList.value.filter(r => r.status === 2).length;
    }
  } catch (error) {
    console.error('获取漏洞报告列表失败:', error);
  } finally {
    loading.value = false;
    refreshing.value = false;
  }
};

const refreshList = () => {
  pagination.current = 1;
  finished.value = false;
  fetchReportList();
};

const onRefresh = () => {
  pagination.current = 1;
  finished.value = false;
  fetchReportList();
};

const loadMore = () => {
  if (reportList.value.length >= pagination.total) {
    finished.value = true;
    loading.value = false;
    return;
  }

  pagination.current++;
  fetchReportList(true);
};

const handleSearch = () => {
  pagination.current = 1;
  finished.value = false;
  fetchReportList();
};

const handleFilterChange = () => {
  pagination.current = 1;
  finished.value = false;
  fetchReportList();
};

const getStatusText = (status: number) => {
  switch (status) {
    case 0: return '待处理';
    case 1: return '已解决';
    case 2: return '已忽略';
    default: return '未知';
  }
};

const getStatusColor = (status: number) => {
  switch (status) {
    case 0: return 'warning';
    case 1: return 'success';
    case 2: return 'default';
    default: return 'default';
  }
};

const getPriorityText = (priority: number) => {
  switch (priority) {
    case 0: return '低';
    case 1: return '中';
    case 2: return '高';
    case 3: return '紧急';
    default: return '未知';
  }
};

const getPriorityColor = (priority: number) => {
  switch (priority) {
    case 0: return 'gray';
    case 1: return 'blue';
    case 2: return 'orange';
    case 3: return 'red';
    default: return 'gray';
  }
};

const formatDate = (dateString: string) => {
  if (!dateString) return '';
  const date = new Date(dateString);
  return date.toLocaleString('zh-CN');
};

const viewDetail = async (record: API.BugReportVO) => {
  try {
    const response = await getBugReportByIdUsingGet({ id: record.id });
    if (response.code === 0 && response.data) {
      currentReport.value = response.data.data;
      detailVisible.value = true;
    }
  } catch (error) {
    console.error('获取报告详情失败:', error);
  }
};

const showConfirmDialog = (type: string, record: API.BugReportVO) => {
  currentOperateRecord.value = record;
  currentOperateType.value = type;

  switch (type) {
    case 'solve':
      dialogTitle.value = '确认解决';
      dialogMessage.value = `确定要将报告"${record.title}"标记为已解决吗？`;
      break;
    case 'ignore':
      dialogTitle.value = '确认忽略';
      dialogMessage.value = `确定要将报告"${record.title}"标记为已忽略吗？`;
      break;
    case 'delete':
      dialogTitle.value = '确认删除';
      dialogMessage.value = `确定要删除报告"${record.title}"吗？此操作不可撤销！`;
      break;
  }

  confirmDialog.value = true;
};

const handleConfirm = async () => {
  if (!currentOperateRecord.value) return;

  try {
    let response;
    const record = currentOperateRecord.value;

    switch (currentOperateType.value) {
      case 'solve':
        response = await solveBugReportUsingPost({ id: record.id });
        if (response.code === 0) {
          record.status = 1;
          record.solveTime = new Date().toISOString();
          stats.value.pendingReports--;
          stats.value.solvedReports++;
        }
        break;
      case 'ignore':
        response = await ignoreBugReportUsingPost({ id: record.id });
        if (response.code === 0) {
          record.status = 2;
          stats.value.pendingReports--;
          stats.value.ignoredReports++;
        }
        break;
      case 'delete':
        response = await deleteBugReportUsingPost({ id: record.id });
        if (response.code === 0) {
          const index = reportList.value.findIndex(r => r.id === record.id);
          if (index !== -1) {
            reportList.value.splice(index, 1);
            pagination.total--;

            if (record.status === 0) stats.value.pendingReports--;
            else if (record.status === 1) stats.value.solvedReports--;
            else if (record.status === 2) stats.value.ignoredReports--;

            stats.value.totalReports--;
          }
        }
        break;
    }

    if (response?.code === 0) {
      console.log(`报告${currentOperateType.value}成功`);
    } else {
      console.error(`报告${currentOperateType.value}失败:`, response?.message);
    }
  } catch (error) {
    console.error(`操作失败:`, error);
  } finally {
    confirmDialog.value = false;
    currentOperateRecord.value = null;
    currentOperateType.value = '';
  }
};

const solveReport = (record: API.BugReportVO) => {
  if (record.status === 1) return;
  showConfirmDialog('solve', record);
};

const ignoreReport = (record: API.BugReportVO) => {
  if (record.status === 2) return;
  showConfirmDialog('ignore', record);
};

const deleteReport = (record: API.BugReportVO) => {
  showConfirmDialog('delete', record);
};

const toggleStatus = (record: API.BugReportVO) => {
  console.log('切换状态:', record);
};

onMounted(() => {
  fetchReportList();
});
</script>

<style scoped>
.bug-report-manage-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.page-content {
  padding-top: 46px;
  padding-bottom: 20px;
}

.search-bar {
  margin: 10px;
  --van-search-background: #ffffff;
}

.filter-bar {
  padding: 0 10px 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.refresh-btn {
  flex-shrink: 0;
  margin-left: 10px;
}

.stats-grid {
  margin: 0 10px 10px;
  --van-grid-item-content-padding: 10px;
}

.report-list {
  padding: 0 10px;
}

.report-item {
  margin-bottom: 10px;
  --van-cell-background: #ffffff;
  --van-cell-border-radius: 8px;
}

.report-item-content {
  width: 100%;
  padding-top: 5px;
}

.report-desc {
  font-size: 12px;
  color: #999;
  margin: 5px 0 0 0;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.report-actions {
  display: flex;
  gap: 5px;
  margin-top: 10px;
  justify-content: flex-end;
}

.detail-content {
  height: calc(100% - 46px);
  overflow-y: auto;
  padding-top: 46px;
}

.step-content {
  background: #f5f5f5;
  padding: 10px;
  border-radius: 4px;
  margin: 5px 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 12px;
  line-height: 1.5;
}

@media (max-width: 375px) {
  .report-actions {
    flex-wrap: wrap;
  }

  .report-actions .van-button {
    flex: 1;
    min-width: 70px;
  }
}

@media (max-width: 320px) {
  .report-actions {
    flex-direction: column;
  }

  .report-actions .van-button {
    width: 100%;
  }
}
</style>
