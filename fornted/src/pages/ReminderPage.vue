<template>
  <div id="reminderPage">
    <!-- 流星背景 -->
    <div class="meteor-background">
      <div class="meteor meteor-1"></div>
      <div class="meteor meteor-2"></div>
      <div class="meteor meteor-3"></div>
      <div class="meteor meteor-4"></div>
      <div class="meteor meteor-5"></div>
    </div>

    <div class="reminderPage-container"  v-if="device !== DEVICE_TYPE_ENUM.MOBILE">
      <div class="main-content">
        <div class="content-layout">
          <!-- 左侧区域：时间日期和提醒事项 -->
          <div class="left-section">
            <!-- 时间日期区域 -->
            <div class="time-container">
              <div class="time-and-date">
                <div class="time">
                  <ClockCircleOutlined />
                  <span>{{ currentTime }}</span>
                </div>
                <div class="date">
                  <CalendarOutlined />
                  <span>{{ currentDate }}</span>
                </div>
              </div>
            </div>

            <!-- 提醒事项区域 -->
            <div class="reminders">
              <div class="section-header">
                <div class="header-left">
                  <h3>
                    <UnorderedListOutlined />
                    今日提醒
                  </h3>
                  <a-date-picker
                    v-model:value="selectedDate"
                    :disabledDate="disabledDate"
                    :format="dateFormat"
                    @change="onDateChange"
                    :bordered="false"
                    :allowClear="false"
                    :inputReadOnly="true"
                    :popupStyle="{ marginTop: '8px' }"
                  >
                    <template #suffixIcon>
                      <CalendarOutlined style="color: #ff8e53" />
                    </template>
                  </a-date-picker>
                </div>
                <a-button class="add-button" @click="addReminder">
                  <PlusOutlined />
                  添加提醒
                </a-button>
              </div>

              <!-- 提醒列表 -->
              <div class="reminder-list-wrapper">
                <a-list :data-source="reminders" class="reminder-list">
                  <template #renderItem="{ item }">
                    <a-list-item>
                      <div class="reminder-item">
                        <CheckOutlined v-if="item.completed" class="check-icon" />
                        <ClockCircleOutlined v-else class="clock-icon" />
                        <span class="time">{{ item.time }}</span>
                        <span class="content" @click="showDetail(item)">
                          {{ item.content.length > 20 ? item.content.slice(0, 20) + '...' : item.content }}
                        </span>
                        <div class="actions">
                          <a-button
                            type="text"
                            class="action-icon"
                            :class="{ active: item.starred }"
                            @click="toggleStarred(item)"
                          >
                            <StarFilled v-if="item.starred" />
                            <StarOutlined v-else />
                          </a-button>
                          <a-button
                            type="text"
                            class="action-icon"
                            :class="{ active: item.important }"
                            @click="toggleImportant(item)"
                          >
                            <ExclamationCircleFilled v-if="item.important" />
                            <ExclamationCircleOutlined v-else />
                          </a-button>
                          <a-button type="text" @click="toggleReminder(item)">
                            {{ item.completed ? '撤销' : '完成' }}
                          </a-button>
                          <a-button type="text" danger @click="deleteReminder(item)">
                            删除
                          </a-button>
                        </div>
                      </div>
                    </a-list-item>
                  </template>
                </a-list>

                <!-- 分页器 -->
                <div class="pagination-wrapper">
                  <a-pagination
                    v-model:current="pcCurrentPage"
                    :total="pcTotal"
                    :pageSize="pcPageSize"
                    :pageSizeOptions="['5', '10', '15', '20']"
                    show-size-changer
                    show-quick-jumper
                    :show-total="total => `共 ${total} 条`"
                    @change="onPageChange"
                  />
                </div>
              </div>
            </div>
          </div>

          <!-- 右侧区域：插画 -->
          <div class="right-section">
            <div class="illustration-container">
              <div class="illustration-content">
                <div ref="animationContainer" class="animation-container"></div>
                <p class="illustration-text">今天也要开开心心哦 ฅ՞•ﻌ•՞ฅ</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 移动端展示 -->
    <template v-if="device === DEVICE_TYPE_ENUM.MOBILE">
      <div class="mobile-container">
        <!-- 流星背景 -->
        <div class="meteor-background">
          <div class="meteor meteor-1"></div>
          <div class="meteor meteor-2"></div>
          <div class="meteor meteor-3"></div>
          <div class="meteor meteor-4"></div>
          <div class="meteor meteor-5"></div>
        </div>

        <div class="main-content">
          <!-- 时间日期区域 -->
          <div class="time-container">
            <div class="time-and-text">
              <div class="time">{{ currentTime }}</div>
              <div class="date">{{ currentDate }}</div>
            </div>
            <div class="pet-container">
              <lottie-player
                src="https://assets5.lottiefiles.com/packages/lf20_syqnfe7c.json"
                background="transparent"
                speed="1"
                style="width: 80px; height: 80px;"
                loop
                autoplay
              ></lottie-player>
            </div>
          </div>

          <!-- 提醒事项列表 -->
          <div class="reminder-list">
            <div class="list-header">
              <div class="header-top">
                <div class="header-left">
                  <h3>今日提醒</h3>
                  <div class="date-picker" @click="showCalendar = true">
                    <CalendarOutlined />
                    <span>{{ formatSelectedDate }}</span>
                  </div>
                </div>
                <div class="header-actions">
                  <van-button
                    plain
                    size="small"
                    @click="toggleShowCompleted"
                    class="toggle-button"
                  >
                    <CheckCircleOutlined v-if="showCompleted" />
                    <CheckCircleOutlined v-else style="opacity: 0.5" />
                  </van-button>
                  <van-button
                    type="primary"
                    size="small"
                    @click="addReminder"
                    class="add-button"
                  >
                    <PlusOutlined />
                  </van-button>
                </div>
              </div>
            </div>

            <!-- 空状态 -->
            <div v-if="!loading && reminders.length === 0" class="empty-state">
              <div class="empty-text">
                <h3>{{ getEmptyStateText }}</h3>
                <p>{{ getEmptyStateSubText }}</p>
              </div>
            </div>

            <!-- 列表内容 -->
            <van-list
              v-else
              v-model="loading"
              :finished="finished"
              :immediate-check="false"
              :offset="50"
              :direction="'down'"
              finished-text="没有更多了"
              @load="onLoadMore"
              class="reminder-list-content"
            >
              <van-cell-group class="reminder-group">
                <van-swipe-cell
                  v-for="item in filteredReminders"
                  :key="item.time"
                  class="reminder-cell"
                >
                  <van-cell :border="false" class="reminder-content">
                    <template #icon>
                      <CheckOutlined v-if="item.completed" class="check-icon" />
                      <ClockCircleOutlined v-else class="clock-icon" />
                    </template>
                    <template #title>
                      <div class="reminder-title">
                        <div class="content-wrapper">
                          <span class="content" @click="showDetail(item)">
                            {{ truncateContent(item.content) }}
                          </span>
                          <div class="badges">
                            <StarFilled v-if="item.starred" class="star-icon" @click="toggleStarred(item)" />
                            <StarOutlined v-else class="star-icon" @click="toggleStarred(item)" />
                            <ExclamationCircleFilled v-if="item.important" class="important-icon" @click="toggleImportant(item)" />
                            <ExclamationCircleOutlined v-else class="important-icon" @click="toggleImportant(item)" />
                          </div>
                        </div>
                        <div class="status-info">
                          <span class="time">{{ item.time }}</span>
                          <van-tag
                            :type="item.completed ? 'success' : 'primary'"
                            round
                            size="mini"
                          >
                            {{ item.completed ? '已完成' : '待完成' }}
                          </van-tag>
                        </div>
                      </div>
                    </template>
                  </van-cell>
                  <template #right>
                    <van-button
                      square
                      type="primary"
                      class="action-button"
                      @click="toggleReminder(item)"
                    >
                      <CheckOutlined v-if="!item.completed" />
                      <UndoOutlined v-else />
                    </van-button>
                    <van-button
                      square
                      type="danger"
                      class="action-button"
                      @click="deleteReminder(item)"
                    >
                      <DeleteOutlined />
                    </van-button>
                  </template>
                </van-swipe-cell>
              </van-cell-group>
            </van-list>
          </div>
        </div>
      </div>
    </template>

    <!-- 添加提醒弹框 -->
    <div v-if="showAddModal" class="modal-overlay" @click="closeAddModal">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>添加提醒</h3>
          <CloseOutlined class="close-icon" @click="closeAddModal" />
        </div>

        <div class="modal-body">
          <textarea
            v-model="reminderContent"
            class="reminder-input"
            placeholder="请输入提醒内容【最多2048个汉字】"
            rows="4"
            maxlength="2048"
          ></textarea>

          <div class="reminder-options">
            <div
              class="option-item"
              :class="{ active: isStarred }"
              @click="isStarred = !isStarred"
            >
              <StarOutlined v-if="!isStarred" class="option-icon" />
              <StarFilled v-else class="option-icon" />
              <span>收藏</span>
            </div>
            <div
              class="option-item"
              :class="{ active: isImportant }"
              @click="isImportant = !isImportant"
            >
              <ExclamationCircleOutlined v-if="!isImportant" class="option-icon" />
              <ExclamationCircleFilled v-else class="option-icon" />
              <span>重要</span>
            </div>
          </div>
        </div>

        <div class="modal-footer">
          <button class="cancel-btn" @click="closeAddModal">取消</button>
          <button class="confirm-btn" @click="handleAddConfirm">确认</button>
        </div>
      </div>
    </div>

    <!-- 日历弹出层 -->
    <van-calendar
      v-model:show="showCalendar"
      :min-date="minDate"
      :max-date="maxDate"
      :show-confirm="false"
      :poppable="true"
      :show-title="false"
      color="#ff8e53"
      @select="onSelectDate"
      :style="{ '--calendar-height': '100%' }"
    >
    </van-calendar>

    <!-- 内容详情弹框 -->
    <div v-if="showContentModal" class="modal-overlay" @click="closeContentModal">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>提醒详情</h3>
          <CloseOutlined class="close-icon" @click="closeContentModal" />
        </div>
        <div class="modal-body">
          <div class="reminder-info">
            <div class="time-badge">
              <ClockCircleOutlined />
              <span>{{ currentReminder?.time }}</span>
            </div>
            <div class="reminder-options">
              <div
                class="option-item"
                :class="{ active: currentReminder?.starred }"
                @click="toggleStarred(currentReminder)"
              >
                <StarOutlined v-if="!currentReminder?.starred" class="option-icon" />
                <StarFilled v-else class="option-icon" />
                <span>收藏</span>
              </div>
              <div
                class="option-item"
                :class="{ active: currentReminder?.important }"
                @click="toggleImportant(currentReminder)"
              >
                <ExclamationCircleOutlined v-if="!currentReminder?.important" class="option-icon" />
                <ExclamationCircleFilled v-else class="option-icon" />
                <span>重要</span>
              </div>
            </div>
          </div>
          <div class="content-box">
            {{ currentReminder?.content }}
          </div>
        </div>
        <div class="modal-footer">
          <button class="cancel-btn" @click="closeContentModal">关闭</button>
          <button
            class="confirm-btn"
            @click="toggleReminder(currentReminder)"
          >
            {{ currentReminder?.completed ? '撤销完成' : '标记完成' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 添加详情弹框 -->
    <a-modal
      v-model:open="detailVisible"
      :title="null"
      :footer="null"
      :width="520"
      class="detail-modal"
    >
      <div class="detail-content">
        <div class="detail-header">
          <div class="time-badge">
            <ClockCircleOutlined />
            <span>{{ selectedItem?.time }}</span>
          </div>
        </div>
        <div class="detail-body">
          <div class="content-box">{{ selectedItem?.content }}</div>
        </div>
        <div class="detail-footer">
          <div class="detail-actions">
            <div class="action-group">
              <a-button
                type="text"
                class="action-icon"
                :class="{ active: selectedItem?.starred }"
                @click="toggleStarred(selectedItem)"
              >
                <StarFilled v-if="selectedItem?.starred" />
                <StarOutlined v-else />
                <span class="action-text">{{ selectedItem?.starred ? '取消标星' : '标记星标' }}</span>
              </a-button>
              <a-button
                type="text"
                class="action-icon"
                :class="{ active: selectedItem?.important }"
                @click="toggleImportant(selectedItem)"
              >
                <ExclamationCircleFilled v-if="selectedItem?.important" />
                <ExclamationCircleOutlined v-else />
                <span class="action-text">{{ selectedItem?.important ? '取消重要' : '标记重要' }}</span>
              </a-button>
            </div>
            <div class="action-group">
              <a-button
                type="text"
                class="action-btn complete-btn"
                :class="{ completed: selectedItem?.completed }"
                @click="toggleReminder(selectedItem)"
              >
                {{ selectedItem?.completed ? '撤销完成' : '标记完成' }}
              </a-button>
              <a-button type="primary" danger class="action-btn delete-btn" @click="handleDetailDelete">
                删除
              </a-button>
            </div>
          </div>
        </div>
      </div>
    </a-modal>

  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, h } from 'vue'
import {
  ClockCircleOutlined,
  CalendarOutlined,
  UnorderedListOutlined,
  CheckOutlined,
  PlusOutlined,
  StarOutlined,
  StarFilled,
  ExclamationCircleOutlined,
  ExclamationCircleFilled,
  CloseOutlined,
  UndoOutlined,
  DeleteOutlined,
  CheckCircleOutlined
} from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
import { message, Modal } from 'ant-design-vue'
import lottie from 'lottie-web'
import { DEVICE_TYPE_ENUM } from '@/constants/device.ts'
import { getDeviceType } from '@/utils/device.ts'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import {
  getTodayRemindersUsingPost,
  addReminderUsingPost,
  toggleReminderUsingPost,
  deleteReminderUsingPost,
  toggleStarredUsingPost,
  toggleImportantUsingPost,
  listReminderByPageUsingPost
} from '@/api/reminderController'
import { DatePicker } from 'ant-design-vue'

// 设置中文语言
dayjs.locale('zh-cn')

const loginUserStore = useLoginUserStore()

// 当前时间
const currentTime = ref(dayjs().format('HH:mm:ss'))
const currentDate = ref(dayjs().format('YYYY年MM月DD日 dddd'))

// 定时器
let timer: number

// 更新时间
const updateTime = () => {
  currentTime.value = dayjs().format('HH:mm:ss')
  currentDate.value = dayjs().format('YYYY年MM月DD日 dddd')
}

// 添加设备类型检测
const device = ref<string>('')

// 提醒事项列表
const reminders = ref<API.ReminderVO[]>([])
const loading = ref(false)
const isLoadingMore = ref(false)

// 分页相关
const currentPage = ref(1)
const pageSize = ref(15)
const sortField = ref<string>('isImportant')
const sortOrder = ref<string>('descend')
const total = ref(0)
const finished = ref(false)

// 日期选择相关
const selectedDate = ref<dayjs.Dayjs>(dayjs())
const showCalendar = ref(false)
const minDate = computed(() => dayjs().subtract(7, 'day').toDate())
const maxDate = computed(() => dayjs().add(30, 'day').toDate())

// 格式化选中日期显示
const formatSelectedDate = computed(() => {
  const date = dayjs(selectedDate.value)
  const today = dayjs()
  const tomorrow = dayjs().add(1, 'day')

  if (date.isSame(today, 'day')) {
    return '今天'
  } else if (date.isSame(tomorrow, 'day')) {
    return '明天'
  } else {
    return date.format('M月D日')
  }
})

// 获取今日提醒列表
const fetchTodayReminders = async (isLoadMore = false) => {
  if (loading.value || (isLoadMore && finished.value)) return

  try {
    loading.value = true
    isLoadingMore.value = isLoadMore

    const isToday = dayjs(selectedDate.value).isSame(dayjs(), 'day')
    const res = isToday
      ? await getTodayRemindersUsingPost({
        current: currentPage.value,
        pageSize: pageSize.value,
        completed: showCompleted.value ? undefined : false,
        sortField: 'isImportant',
        sortOrder: sortOrder.value
      })
      : await listReminderByPageUsingPost({
        current: currentPage.value,
        pageSize: pageSize.value,
        date: selectedDate.value.format('YYYY-MM-DD'),
        completed: showCompleted.value ? undefined : false,
        sortField: 'isImportant',
        sortOrder: sortOrder.value
      })

    if (res.data?.code === 0) {
      const newData = res.data.data?.records || []
      total.value = res.data.data?.total || 0

      if (isLoadMore) {
        reminders.value = [...reminders.value, ...newData]
      } else {
        reminders.value = newData
      }

      finished.value = reminders.value.length >= total.value
    } else {
      message.error(res.data?.message || '获取提醒列表失败')
      finished.value = true
    }
  } catch (error: any) {
    message.error(error.message || '获取提醒列表失败')
    finished.value = true
  } finally {
    loading.value = false
    isLoadingMore.value = false
  }
}

// 移动端加载更多
const onLoadMore = async () => {
  if (device.value === DEVICE_TYPE_ENUM.MOBILE && !isLoadingMore.value && !finished.value) {
    currentPage.value++
    await fetchTodayReminders(true)
  }
}

// PC端分页相关
const pcCurrentPage = ref(1)
const pcPageSize = ref(15)
const pcTotal = ref(0)
// PC端分页变化
const onPageChange = (page: number, pageSize: number) => {
  pcCurrentPage.value = page
  pcPageSize.value = pageSize
  fetchPCReminders()
}
// 获取提醒列表 - PC端
const fetchPCReminders = async () => {
  try {
    const isToday = dayjs(selectedDate.value).isSame(dayjs(), 'day')
    const res = isToday
      ? await getTodayRemindersUsingPost({
        current: pcCurrentPage.value,
        pageSize: pcPageSize.value,
        completed: showCompleted.value ? undefined : false,
        sortField: sortField.value,
        sortOrder: sortOrder.value
      })
      : await listReminderByPageUsingPost({
        current: pcCurrentPage.value,
        pageSize: pcPageSize.value,
        date: selectedDate.value.format('YYYY-MM-DD'),
        completed: showCompleted.value ? undefined : false,
        sortField: sortField.value,
        sortOrder: sortOrder.value
      })

    if (res.data?.code === 0) {
      reminders.value = res.data.data?.records || []
      pcTotal.value = res.data.data?.total || 0
    } else {
      message.error(res.data?.message || '获取提醒列表失败')
    }
  } catch (error: any) {
    message.error(error.message || '获取提醒列表失败')
  }
}

// 重置分页
const resetPagination = () => {
  currentPage.value = 1
  finished.value = false
  reminders.value = []
}

// 修改添加/删除/切换状态后的刷新逻辑
const refreshList = () => {
  resetPagination()
  fetchTodayReminders()
}

// 截断内容
const truncateContent = (content: string) => {
  if (!content) return ''
  return content.length > 10 ? content.slice(0, 10) + '...' : content
}

// 添加新的状态
const showAddModal = ref(false)
const reminderContent = ref('')
const isStarred = ref(false)
const isImportant = ref(false)

// 修改添加提醒的方法
const addReminder = () => {
  if (!loginUserStore.loginUser.id) {
    message.warning('请先登录')
    return
  }
  showAddModal.value = true
}

// 关闭弹框
const closeAddModal = () => {
  showAddModal.value = false
  reminderContent.value = ''
  isStarred.value = false
  isImportant.value = false
}

// 确认添加
const handleAddConfirm = async () => {
  const content = reminderContent.value.trim()
  if (!content) {
    message.warning('提醒内容不能为空')
    return
  }

  try {
    const res = await addReminderUsingPost({
      content,
      starred: isStarred.value,
      important: isImportant.value
    })

    if (res.data?.code === 0) {
      message.success('添加成功')
      closeAddModal()
      refreshList()
    } else {
      message.error(res.data?.message || '添加失败')
    }
  } catch (error: any) {
    message.error(error.message || '添加失败')
  }
}

// 切换提醒状态
const toggleReminder = async (item: API.ReminderVO) => {
  if (!item.id) return
  try {
    // 先更新前端状态
    const index = reminders.value.findIndex(r => r.id === item.id)
    if (index !== -1) {
      reminders.value[index] = { ...reminders.value[index], completed: !item.completed }
    }

    // 后端请求
    const res = await toggleReminderUsingPost({ id: item.id })
    if (res.data?.code !== 0) {
      // 如果失败，恢复原状态
      if (index !== -1) {
        reminders.value[index] = { ...reminders.value[index], completed: item.completed }
      }
      message.error(res.data?.message || '操作失败')
    } else {
      message.success(item.completed ? '已撤销' : '已完成')
    }
  } catch (error: any) {
    // 发生错误，恢复原状态
    const index = reminders.value.findIndex(r => r.id === item.id)
    if (index !== -1) {
      reminders.value[index] = { ...reminders.value[index], completed: item.completed }
    }
    message.error(error.message || '操作失败')
  }
}

// 删除提醒
const deleteReminder = async (item: API.ReminderVO) => {
  if (!item.id) return
  try {
    const res = await deleteReminderUsingPost({
      id: item.id
    })
    if (res.data?.code === 0) {
      message.success('删除成功')
      refreshList()
    } else {
      message.error(res.data?.message || '删除失败')
    }
  } catch (error: any) {
    message.error(error.message || '删除失败')
  }
}

// Lottie 动画
const animationContainer = ref<HTMLElement>()

// 添加显示已完成的状态
const showCompleted = ref(true)

// 过滤已完成/未完成的提醒
const filteredReminders = computed(() => {
  if (showCompleted.value) {
    return reminders.value
  }
  return reminders.value.filter(item => !item.completed)
})

// 切换显示已完成
const toggleShowCompleted = () => {
  showCompleted.value = !showCompleted.value
  resetPagination()
  fetchTodayReminders()
}

// 切换收藏状态
const toggleStarred = async (item: API.ReminderVO) => {
  if (!item.id) return
  try {
    // 先更新前端状态
    const index = reminders.value.findIndex(r => r.id === item.id)
    if (index !== -1) {
      reminders.value[index] = { ...reminders.value[index], starred: !item.starred }
    }

    // 后端请求
    const res = await toggleStarredUsingPost({ id: item.id })
    if (res.data?.code !== 0) {
      // 如果失败，恢复原状态
      if (index !== -1) {
        reminders.value[index] = { ...reminders.value[index], starred: item.starred }
      }
      message.error(res.data?.message || '操作失败')
    } else {
      message.success(item.starred ? '已取消收藏' : '已收藏')
    }
  } catch (error: any) {
    // 发生错误，恢复原状态
    const index = reminders.value.findIndex(r => r.id === item.id)
    if (index !== -1) {
      reminders.value[index] = { ...reminders.value[index], starred: item.starred }
    }
    message.error(error.message || '操作失败')
  }
}

// 切换重要状态
const toggleImportant = async (item: API.ReminderVO) => {
  if (!item.id) return
  try {
    // 先更新前端状态
    const index = reminders.value.findIndex(r => r.id === item.id)
    if (index !== -1) {
      reminders.value[index] = { ...reminders.value[index], important: !item.important }
    }

    // 后端请求
    const res = await toggleImportantUsingPost({ id: item.id })
    if (res.data?.code !== 0) {
      // 如果失败，恢复原状态
      if (index !== -1) {
        reminders.value[index] = { ...reminders.value[index], important: item.important }
      }
      message.error(res.data?.message || '操作失败')
    } else {
      message.success(item.important ? '已取消重要标记' : '已标记为重要')
    }
  } catch (error: any) {
    // 发生错误，恢复原状态
    const index = reminders.value.findIndex(r => r.id === item.id)
    if (index !== -1) {
      reminders.value[index] = { ...reminders.value[index], important: item.important }
    }
    message.error(error.message || '操作失败')
  }
}

// 空状态文案
const getEmptyStateText = computed(() => {
  if (loading.value) return '加载中...'
  const date = dayjs(selectedDate.value)
  const today = dayjs()

  if (date.isSame(today, 'day')) {
    return '今天还没有提醒事项'
  } else if (date.isBefore(today, 'day')) {
    return '这一天没有提醒事项'
  } else {
    return `${date.format('M月D日')}还没有安排`
  }
})

const getEmptyStateSubText = computed(() => {
  if (loading.value) return ''
  const date = dayjs(selectedDate.value)
  const today = dayjs()

  if (date.isSame(today, 'day')) {
    return '点击右上角添加一个新的提醒吧'
  } else if (date.isBefore(today, 'day')) {
    return '看看其他日期的安排吧'
  } else {
    return '提前规划一下这一天？'
  }
})

// 修改排序方法
const handleSort = (field: string) => {
  if (sortField.value === field) {
    // 切换排序顺序
    sortOrder.value = sortOrder.value === 'ascend' ? 'descend' : 'ascend'
  } else {
    // 切换排序字段
    sortField.value = field
    sortOrder.value = 'descend'
  }
  resetPagination()
  fetchTodayReminders()
}

// PC端日期变化处理
const onDateChange = (date: dayjs.Dayjs | null) => {
  if (date) {
    selectedDate.value = date
    resetPagination()
    fetchTodayReminders()
  }
}

// 移动端日期选择处理
const onSelectDate = (date: Date) => {
  selectedDate.value = dayjs(date)
  showCalendar.value = false
  resetPagination()
  fetchTodayReminders()
}

// 日期选择相关
const dateFormat = 'YYYY-MM-DD'
const disabledDate = (current: dayjs.Dayjs) => {
  return current && (current < minDate.value || current > maxDate.value)
}

// 内容弹框相关
const showContentModal = ref(false)
const currentReminder = ref<API.ReminderVO | null>(null)

const showFullContent = (reminder: API.ReminderVO) => {
  currentReminder.value = reminder
  showContentModal.value = true
}

const closeContentModal = () => {
  showContentModal.value = false
  currentReminder.value = null
}

// 添加详情弹框相关变量和方法
const detailVisible = ref(false)
const selectedItem = ref(null)

const showDetail = (item) => {
  selectedItem.value = item
  detailVisible.value = true
}

// 在详情弹框中删除提醒
const handleDetailDelete = () => {
  if (selectedItem.value) {
    deleteReminder(selectedItem.value)
    detailVisible.value = false
  }
}

onMounted(async () => {
  device.value = await getDeviceType()
  updateTime()
  timer = setInterval(updateTime, 1000)

  // 初始化动画
  if (animationContainer.value) {
    lottie.loadAnimation({
      container: animationContainer.value,
      renderer: 'svg',
      loop: true,
      autoplay: true,
      path: 'https://assets9.lottiefiles.com/packages/lf20_w51pcehl.json'
    })
  }

  // 获取今日提醒列表
  if (loginUserStore.loginUser.id) {
    await fetchTodayReminders()
    await fetchPCReminders()
  }
})

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
  }
})
</script>

<style scoped>
#reminderPage {
  margin-left: -20px !important;
  margin-right: -20px !important;
  margin-top: -20px;
  background: #f8fafc;
  position: relative;
  overflow: hidden;
  display: flex;
  justify-content: center;
}

/* 主容器样式 */
.reminderPage-container {
  max-width: 1400px;
  margin: 0 auto;
  height: 100%;
  position: relative;
  width: 100%;
}

/* 流星背景样式 */
.meteor-background {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;

}

.meteor {
  position: absolute;
  width: 2px;
  height: 2px;
  background: linear-gradient(45deg, #ff8e53, #ff6b6b);
  border-radius: 50%;
  animation: meteor-fall linear infinite;
  opacity: 0;
}

.meteor::before {
  content: '';
  position: absolute;
  width: 100px;
  height: 1px;
  transform: translateX(-100%);
  background: linear-gradient(90deg, #ff8e53, transparent);
}

.meteor-1 { top: -10%; left: 20%; animation-duration: 6s; animation-delay: 0s; }
.meteor-2 { top: -10%; left: 40%; animation-duration: 8s; animation-delay: 2s; }
.meteor-3 { top: -10%; left: 60%; animation-duration: 7s; animation-delay: 4s; }
.meteor-4 { top: -10%; left: 80%; animation-duration: 9s; animation-delay: 1s; }
.meteor-5 { top: -10%; left: 30%; animation-duration: 5s; animation-delay: 3s; }

@keyframes meteor-fall {
  0% { transform: translate(0, 0) rotate(45deg); opacity: 1; }
  20% { opacity: 1; }
  60% { opacity: 0; }
  100% { transform: translate(500px, 500px) rotate(45deg); opacity: 0; }
}

.main-content {
  position: relative;
  width: 100%;
}

/* 左右布局容器 */
.content-layout {
  display: flex;
  gap: 20px;
  padding-top: 24px;
  width: 100%;
  height: 100%;
}

/* 左侧区域 */
.left-section {
  flex: 1;
  min-width: 800px;
  width: calc(100% - 360px);
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 右侧区域 */
.right-section {
  width: 320px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  flex-shrink: 0;
}

/* 时间容器样式 */
.time-container {
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  background: linear-gradient(135deg, rgba(255, 142, 83, 0.05) 0%, rgba(255, 107, 107, 0.05) 100%);
}

.time-and-date {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.time, .date {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 24px;
  color: #1f2937;
}

/* 提醒事项区域 */
.reminders {
  height: calc(100vh - 280px); /* 减去头部、底部和其他元素的高度 */
  display: flex;
  flex-direction: column;
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-header h3 {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.add-button {
  background: linear-gradient(135deg, #ff8e53 0%, #ff6b6b 100%);
  border: none;
  color: white;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 16px;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.add-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(255, 107, 107, 0.2);
}

.reminder-list-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;

}

.reminder-list {
  flex: 1;
  overflow-y: auto;
}

.reminder-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.reminder-item:hover {
  background: rgba(255, 255, 255, 0.8);
}

.reminder-item .time {
  color: #64748b;
  font-size: 14px;
  min-width: 60px;
}

.reminder-item .content {
  flex: 1;
  color: #1f2937;
}

.reminder-item .actions {
  display: flex;
  gap: 8px;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.reminder-item:hover .actions {
  opacity: 1;
}

.check-icon {
  color: #10b981;
}

.clock-icon {
  color: #6366f1;
}

/* 插画容器样式 */
.illustration-container {
  background: white;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  height: 100%;
  text-align: center;
  display: flex;
  flex-direction: column;
  justify-content: center;
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.05) 0%, rgba(14, 165, 233, 0.05) 100%);
}

.animation-container {
  width: 200px;
  height: 200px;
  margin: 0 auto;
}

.illustration-text {
  font-size: 14px;
  color: #64748b;
  margin-top: 16px;
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .content-layout {
    flex-direction: column;
    padding-top: 12px;
    gap: 12px;
  }

  .left-section,
  .right-section {
    width: 100%;
    max-width: none;
    min-width: unset;
  }

  #reminderPage {
    padding: 12px 0;
    min-height: calc(100vh - 160px);
  }

  .reminderPage-container {
    padding: 12px;
  }

  .time-container,
  .reminders {
    border-radius: 12px;
    padding: 16px;
  }

  .time, .date {
    font-size: 20px;
  }

  .reminder-item .actions {
    opacity: 1;
  }

  .right-section {
    display: none;
  }

  .meteor-background {
    opacity: 0.5;
  }

  .main-content {

  }
}

/* 移动端专属样式 */
.mobile-container {
  height: 92vh;  /* 减去头部和底部的高度 */
  background: #f7f8fa;
  width: 100%;
  position: relative;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.mobile-container .main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 时间容器 */
.mobile-container .time-container {
  padding: 20px;
  background: linear-gradient(135deg, #ff8e53 0%, #ff6b6b 100%);
  margin-bottom: 12px;
  border-radius: 20px;
  position: relative;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(255, 107, 107, 0.15);
}

.mobile-container .time-and-text {
  color: white;

  position: relative;
}

.mobile-container .time {
  font-size: 32px;
  font-weight: 600;
  margin-bottom: 4px;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.mobile-container .date {
  font-size: 16px;
  opacity: 0.9;
}

.mobile-container .pet-container {
  position: absolute;
  right: -10px;
  bottom: -10px;
  opacity: 0.9;
  transform: scale(0.8);
  pointer-events: none;
}

/* 提醒事项列表 */
.mobile-container .reminder-list {
  height: calc(100vh - 200px); /* 减去头部和其他元素的高度 */
  overflow-y: auto;
  padding: 12px;
}

/* 移动端列表头部固定 */
.mobile-container .list-header {
  position: sticky;
  top: 0;

  background: #f7f8fa;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

.mobile-container .header-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.mobile-container .list-header h3 {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

.mobile-container .header-actions {
  display: flex;
  gap: 8px;
}

/* 移动端列表内容区域 */
.mobile-container .reminder-list {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 180px); /* 调整高度 */
  overflow: hidden;
}

.mobile-container .reminder-list-content {
  flex: 1;
  overflow-y: auto;

}

.mobile-container .reminder-group {
  padding: 4px 0;
}

/* 移动端按钮样式 */
.mobile-container .toggle-button {
  background: linear-gradient(135deg, #ff8e53 0%, #ff6b6b 100%);
  border: none;
  color: white;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 16px;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.mobile-container .add-button {
  background: linear-gradient(135deg, #ff8e53 0%, #ff6b6b 100%);
  border: none;
}

/* 加载更多样式 */
:deep(.van-list__loading),
:deep(.van-list__finished-text) {
  padding: 12px 0;
  color: #ff8e53;
}

/* 添加提醒弹窗样式 */
:deep(.reminder-add-modal) {
  .ant-modal-content {
    background: linear-gradient(135deg, #fff5f0 0%, #fff 100%);
    border-radius: 20px;
    overflow: hidden;
  }

  .ant-modal-header {
    background: transparent;
    border: none;
    padding: 24px 24px 0;
  }

  .ant-modal-title {
    color: #ff8e53;
    font-size: 18px;
    font-weight: 600;
  }

  .ant-modal-body {
    padding: 20px 24px;
    max-height: 60vh;
    overflow-y: auto;
  }

  .ant-modal-footer {
    border-top: none;
    padding: 0 24px 24px;
  }
}

/* 添加提醒内容样式 */
.add-reminder-content {
  .reminder-input {
    width: 100%;
    padding: 12px;
    border: 1px solid #ffe4d6;
    border-radius: 12px;
    background: rgba(255, 255, 255, 0.8);
    font-size: 14px;
    line-height: 1.6;
    resize: none;
    transition: all 0.3s ease;

    &:focus {
      outline: none;
      border-color: #ff8e53;
      box-shadow: 0 0 0 2px rgba(255, 142, 83, 0.1);
    }
  }

  .reminder-options {
    margin-top: 16px;
    display: flex;
    gap: 16px;
  }

  .option-item {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 6px 12px;
    border-radius: 8px;
    background: rgba(255, 142, 83, 0.1);
    color: #ff8e53;
    cursor: pointer;
    transition: all 0.3s ease;

    &:hover {
      background: rgba(255, 142, 83, 0.2);
    }

    .option-icon {
      font-size: 16px;
    }
  }
}

/* 弹框按钮样式 */
:deep(.reminder-add-modal) {
  .ant-modal-footer {
    .ant-btn {
      height: 36px;
      padding: 0 24px;
      border-radius: 18px;
      font-size: 14px;
      transition: all 0.3s ease;
    }

    .ant-btn-default {
      border-color: #e5e7eb;
      color: #64748b;

      &:hover {
        color: #1a1a1a;
        border-color: #94a3b8;
        background: #f8fafc;
      }
    }

    .ant-btn-primary {
      background: linear-gradient(135deg, #ff8e53 0%, #ff6b6b 100%);
      border: none;
      color: white;

      &:hover {
        transform: translateY(-1px);
        box-shadow: 0 4px 12px rgba(255, 107, 107, 0.2);
      }

      &:active {
        transform: translateY(1px);
      }
    }
  }
}

/* 提醒详情弹窗样式 */
:deep(.reminder-detail-modal) {
  .ant-modal-content {
    background: linear-gradient(135deg, #fff5f0 0%, #fff 100%);
    border-radius: 20px;
    overflow: hidden;
  }

  .ant-modal-header {
    background: transparent;
    border: none;
    padding: 24px 24px 0;

    .ant-modal-title {
      color: #ff8e53;
      font-size: 18px;
      font-weight: 600;
    }
  }

  .ant-modal-body {
    padding: 20px 24px;
    max-height: 60vh;
    overflow: hidden;
  }

  .reminder-detail-wrapper {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .reminder-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
  }

  .reminder-time {
    display: flex;
    align-items: center;
    gap: 6px;
    color: #ff8e53;
    font-size: 14px;
  }

  .reminder-badges {
    display: flex;
    gap: 12px;
  }

  .badge-icon {
    font-size: 20px;
    cursor: pointer;
    transition: all 0.3s ease;

    &.active {
      transform: scale(1.1);
    }
  }

  .reminder-content {
    padding: 16px;
    background: rgba(255, 255, 255, 0.8);
    border-radius: 12px;
    font-size: 16px;
    line-height: 1.6;
    color: #1f2937;
    white-space: pre-wrap;
    word-break: break-all;
  }
}

/* 修改卡片样式 */
.content-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
}

.badges {
  display: flex;
  gap: 4px;
}

.star-icon,
.important-icon {
  font-size: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.star-icon {
  color: #facc15;
  opacity: 0.5;

  &:hover {
    opacity: 0.8;
  }

  &.anticon-star-filled {
    opacity: 1;
  }
}

.important-icon {
  color: #ef4444;
  opacity: 0.5;

  &:hover {
    opacity: 0.8;
  }

  &.anticon-exclamation-circle-filled {
    opacity: 1;
  }
}

/* 移动端加载更多 */
.load-more {
  text-align: center;
  padding: 16px 0;
  color: #ff8e53;
}

/* PC端分页器样式 */
.pagination-wrapper {
  padding: 16px 0 0;
  display: flex;
  justify-content: flex-end;
  background: transparent;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
}

/* 移动端列表样式 */
.mobile-container .reminder-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 12px;
}

:deep(.van-list) {
  min-height: 100px;
}

:deep(.van-list__loading) {
  padding: 16px 0;
  background: transparent;
}

:deep(.van-list__finished-text) {
  color: #ff8e53;
  padding: 16px 0;
  background: transparent;
}

/* 修改 ant-pagination 样式 */
:deep(.ant-pagination) {
  .ant-pagination-item-active {
    border-color: #ff8e53;

    a {
      color: #ff8e53;
    }
  }

  .ant-pagination-item:hover {
    border-color: #ff8e53;

    a {
      color: #ff8e53;
    }
  }

  .ant-pagination-prev:hover .ant-pagination-item-link,
  .ant-pagination-next:hover .ant-pagination-item-link {
    border-color: #ff8e53;
    color: #ff8e53;
  }
}

/* PC端样式优化 */
.reminders {
  height: calc(100vh - 280px); /* 减去头部、底部和其他元素的高度 */
  display: flex;
  flex-direction: column;
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.reminder-list-wrapper {
  flex: 1;
  min-height: 0; /* 重要：防止flex子元素溢出 */
  display: flex;
  flex-direction: column;
}

.reminder-list {
  flex: 1;
  overflow-y: auto;
  min-height: 0; /* 重要：防止flex子元素溢出 */
  padding: 8px;
  margin-bottom: 16px;
}

/* 移动端样式恢复和优化 */
.mobile-container .reminder-cell {
  margin-bottom: 8px;
}

.mobile-container .reminder-content {
  padding: 12px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.95) 0%, rgba(255, 255, 255, 0.9) 100%);
  margin: 0 12px;
  border-radius: 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.8);
}

.mobile-container .reminder-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  min-height: 24px;
  padding-right: 14px;
}

.mobile-container .reminder-title .content {
  font-size: 14px;
  color: #1f2937;
  line-height: 1.3;
  cursor: pointer;
  transition: color 0.3s ease;
}

.mobile-container .status-info {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.mobile-container .status-info .time {
  font-size: 12px;
  color: #64748b;
}

/* 移动端列表容器 */
.mobile-container .reminder-list {
  height: calc(100vh - 240px); /* 减去头部和其他元素的高度 */
  overflow-y: auto;
  padding: 12px;
}

.mobile-container .reminder-group {
  padding: 4px 0;
}

/* 移动端滑动按钮 */
.mobile-container :deep(.van-swipe-cell__right) {
  height: 100%;
  display: flex;
  margin-right: 12px;
}

.mobile-container .action-button {
  width: 64px;
  height: 100%;
  font-size: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #ff8e53 0%, #ff6b6b 100%);

  &:first-child {
    background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  }
}

/* 移动端标签样式 */
.mobile-container :deep(.van-tag) {
  padding: 0 6px;
  font-size: 10px;
  line-height: 14px;
  height: 16px;
}

/* 根据完成状态设置不同的背景色 */
.mobile-container .reminder-cell:has(.check-icon) .reminder-content {
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.03) 0%, rgba(5, 150, 105, 0.03) 100%);
  border-color: rgba(16, 185, 129, 0.08);
}

.mobile-container .reminder-cell:has(.clock-icon) .reminder-content {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.03) 0%, rgba(37, 99, 235, 0.03) 100%);
  border-color: rgba(59, 130, 246, 0.08);
}

/* 弹框样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;

  backdrop-filter: blur(4px);
}

.modal-content {
  width: 90%;
  max-width: 520px;
  background: linear-gradient(135deg, #fff5f0 0%, #fff 100%);
  border-radius: 20px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
  animation: modal-in 0.3s ease;
}

.modal-header {
  padding: 20px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid rgba(255, 142, 83, 0.1);
}

.modal-header h3 {
  margin: 0;
  color: #ff8e53;
  font-size: 18px;
  font-weight: 600;
}

.close-icon {
  font-size: 18px;
  color: #64748b;
  cursor: pointer;
  transition: all 0.3s ease;

  &:hover {
    color: #ff8e53;
    transform: rotate(90deg);
  }
}

.modal-body {
  padding: 24px;
}

.reminder-input {
  width: 100%;
  padding: 12px;
  border: 1px solid #ffe4d6;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.8);
  font-size: 14px;
  line-height: 1.6;
  resize: none;
  transition: all 0.3s ease;

  &:focus {
    outline: none;
    border-color: #ff8e53;
    box-shadow: 0 0 0 2px rgba(255, 142, 83, 0.1);
  }
}

.reminder-options {
  margin-top: 16px;
  display: flex;
  gap: 16px;
}

.option-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 8px;
  background: rgba(255, 142, 83, 0.1);
  color: #ff8e53;
  cursor: pointer;
  transition: all 0.3s ease;

  &:hover {
    background: rgba(255, 142, 83, 0.2);
  }

  &.active {
    background: #ff8e53;
    color: white;
  }

  .option-icon {
    font-size: 16px;
  }
}

.modal-footer {
  padding: 16px 24px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  border-top: 1px solid rgba(255, 142, 83, 0.1);
}

.cancel-btn,
.confirm-btn {
  height: 36px;
  padding: 0 24px;
  border-radius: 18px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.cancel-btn {
  border: 1px solid #e5e7eb;
  background: transparent;
  color: #64748b;

  &:hover {
    color: #1a1a1a;
    border-color: #94a3b8;
    background: #f8fafc;
  }
}

.confirm-btn {
  background: linear-gradient(135deg, #ff8e53 0%, #ff6b6b 100%);
  border: none;
  color: white;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(255, 107, 107, 0.2);
  }

  &:active {
    transform: translateY(1px);
  }
}

@keyframes modal-in {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 列表头部样式 */
.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.date-picker {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  border-radius: 6px;
  background: rgba(255, 142, 83, 0.1);
  color: #ff8e53;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;

  &:active {
    background: rgba(255, 142, 83, 0.2);
  }
}

/* 空状态样式优化 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  height: 100%;
  background: linear-gradient(to bottom, #fff9f6, #fff);
  border-radius: 20px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.08);
  position: relative;
  overflow: visible;
}

.empty-pet {
  position: absolute;
  top: -75px;
  transform: scale(0.8);
  animation: dogBounce 2s ease-in-out infinite;
  pointer-events: none;
}

.empty-text {
  text-align: center;
  margin-top: 60px;
  animation: fadeIn 0.5s ease-out;

  h3 {
    font-size: 20px;
    font-weight: 600;
    margin-bottom: 12px;
    background: linear-gradient(135deg, #ff8e53 0%, #ff6b6b 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
  }

  p {
    color: #64748b;
    font-size: 15px;
    line-height: 1.6;
    margin: 0;
  }
}

@keyframes dogBounce {
  0%, 100% {
    transform: translateY(0) rotate(3deg) scale(0.8);
  }
  50% {
    transform: translateY(-8px) rotate(-3deg) scale(0.8);
  }
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .empty-pet {
    top: -65px;
    transform: scale(0.7);
  }

  .empty-text {
    margin-top: 40px;

    h3 {
      font-size: 18px;
    }

    p {
      font-size: 14px;
    }
  }
}

/* PC端日历样式 */
:deep(.ant-picker) {
  background: rgba(255, 142, 83, 0.1);
  border: none;
  border-radius: 8px;
  padding: 6px 16px;
  margin-left: 16px;
  cursor: pointer;
  transition: all 0.3s ease;

  &:hover {
    background: rgba(255, 142, 83, 0.2);
  }

  .ant-picker-input {
    input {
      color: #ff8e53;
      font-size: 14px;
      cursor: pointer;

      &::placeholder {
        color: #ff8e53;
      }
    }
  }
}

:deep(.ant-picker-dropdown) {
  .ant-picker-cell-in-view.ant-picker-cell-selected .ant-picker-cell-inner {
    background: linear-gradient(135deg, #ff8e53 0%, #ff6b6b 100%);
  }

  .ant-picker-cell-in-view.ant-picker-cell-today .ant-picker-cell-inner::before {
    border-color: #ff8e53;
  }

  .ant-picker-header-view {
    color: #ff8e53;
  }

  .ant-picker-cell:hover:not(.ant-picker-cell-selected):not(.ant-picker-cell-disabled) .ant-picker-cell-inner {
    background: rgba(255, 142, 83, 0.1);
  }
}

/* 移动端日历样式 */
:deep(.van-calendar) {
  --van-calendar-height: 100%;
  --van-calendar-background: linear-gradient(to bottom, #fff9f6, #fff);

  .calendar-title {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px;
    font-size: 18px;
    font-weight: 600;
    color: #1f2937;

    .close-icon {
      font-size: 20px;
      color: #64748b;
      cursor: pointer;

      &:active {
        opacity: 0.8;
      }
    }
  }

  .van-calendar__header {
    box-shadow: none;
    background: transparent;
  }

  .van-calendar__month-title {
    font-size: 16px;
    font-weight: 600;
    color: #1f2937;
  }

  .van-calendar__day {
    height: 64px;

    &:not(.van-calendar__day--disabled) {
      &:hover {
        background: rgba(255, 142, 83, 0.1);
      }
    }
  }

  .van-calendar__selected-day {
    background: linear-gradient(135deg, #ff8e53 0%, #ff6b6b 100%);
    border-radius: 8px;
  }
}

/* 内容弹框样式优化 */
.reminder-info {
  margin-bottom: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.time-badge {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  background: rgba(255, 142, 83, 0.1);
  border-radius: 8px;
  color: #ff8e53;
  font-size: 14px;
}

.content-box {
  padding: 16px;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 12px;
  border: 1px solid #ffe4d6;
  font-size: 16px;
  line-height: 1.8;
  color: #1f2937;
  white-space: pre-wrap;
  word-break: break-all;
}

/* PC端操作按钮样式 */
.actions .action-icon {
  color: #64748b;
  opacity: 0.6;
  transition: all 0.3s ease;
  padding: 4px 8px;

  &:hover {
    opacity: 0.8;
    background: transparent;
  }

  &.active {
    color: #ff8e53;
    opacity: 1;
  }
}

.reminder-item {
  .actions {
    display: flex;
    align-items: center;
    gap: 4px;
  }
}

/* 详情弹框样式 */
.detail-modal {
  :deep(.ant-modal-content) {
    border-radius: 16px;
    overflow: hidden;
  }

  :deep(.ant-modal-body) {
    padding: 24px;
    max-height: 80vh;
    overflow: hidden;
  }
}

.detail-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.detail-body {
  max-height: calc(80vh - 120px);
  overflow-y: auto;
}

.status-badge {
  padding: 4px 12px;
  border-radius: 16px;
  font-size: 13px;
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;

  &.completed {
    background: rgba(16, 185, 129, 0.1);
    color: #10b981;
  }
}

/* 自定义滚动条样式 */
.detail-body::-webkit-scrollbar {
  width: 6px;
}

.detail-body::-webkit-scrollbar-track {
  background: transparent;
}

.detail-body::-webkit-scrollbar-thumb {
  background: rgba(203, 213, 225, 0.8);
  border-radius: 3px;
}

.detail-body::-webkit-scrollbar-thumb:hover {
  background: rgba(148, 163, 184, 0.8);
}

.detail-footer {
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  padding-top: 16px;
  margin-top: 8px;
}

.detail-actions {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.action-group {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  width: 100%;
  gap: 12px;
}

.detail-actions .action-icon {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 16px;
  border-radius: 6px;
  transition: all 0.2s ease;
  color: #64748b;
  font-weight: 500;
  flex: 1;
  min-width: 120px;
  justify-content: center;

  &:hover {
    background: rgba(0, 0, 0, 0.04);
    transform: translateY(-1px);
  }

  &:active {
    transform: translateY(1px);
  }

  &.active {
    color: #ff8e53;
    background: rgba(255, 142, 83, 0.1);
  }

  .action-text {
    font-size: 14px;
  }
}

.detail-actions .action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 16px;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s ease;
  flex: 1;
  min-width: 120px;
  justify-content: center;

  &:hover {
    background: rgba(0, 0, 0, 0.04);
    transform: translateY(-1px);
  }

  &:active {
    transform: translateY(1px);
  }
}

.complete-btn {
  &.completed {
    color: #10b981;
    background: rgba(16, 185, 129, 0.1);
  }

  &:hover {
    color: #059669;
    background: rgba(16, 185, 129, 0.15);
  }
}

.delete-btn:hover {
  color: #ef4444;
  background: rgba(239, 68, 68, 0.1);
}
</style>
