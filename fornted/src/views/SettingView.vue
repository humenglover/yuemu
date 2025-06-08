<template>
  <div id="SettingView">
    <div class="setting-container">
      <!-- 流星背景 -->
      <div class="meteor-background">
        <div class="meteor meteor-1"></div>
        <div class="meteor meteor-2"></div>
        <div class="meteor meteor-3"></div>
        <div class="meteor meteor-4"></div>
        <div class="meteor meteor-5"></div>
      </div>
      <div class="main-content">
        <SeasonalBanner />
        <div class="content-layout">

          <!-- 左侧区域：用户信息和成长足迹 -->
          <div class="left-section">
            <!-- 用户信息区域 -->
            <div class="user-info-container">
              <div class="avatar-and-text">
                <div class="avatar-container">
                  <a-avatar
                    class="user-avatar"
                    :src="loginUserStore.loginUser.userAvatar || getDefaultAvatar(loginUserStore.loginUser.userName)"
                    :size="80"
                  />
                </div>
                <div class="text-info-container">
                  <div class="user-name">{{ loginUserStore.loginUser.userName }}</div>
                  <div class="user-id">ID: {{ loginUserStore.loginUser.id }}</div>
                  <div class="user-stats" >
                    <div class="stat-item" @click.stop="handleFollowClick">
                      {{ followAndFans.followCount || 0 }} 关注
                    </div>
                    <div class="stat-divider">·</div>
                    <div class="stat-item" @click.stop="handleFansClick">
                      {{ followAndFans.fansCount || 0 }} 粉丝
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 签到日历 -->
            <div class="sign-in-calendar">
              <div class="calendar-header">
                <h3>成长足迹</h3>
                <a-select
                  v-model:value="selectedYear"
                  :options="yearOptions"
                  class="year-selector"
                  @change="fetchSignInData"
                />
              </div>
              <v-chart :option="calendarOption" autoresize class="calendar-chart" />
            </div>
          </div>

          <!-- 右侧区域：操作按钮 -->
          <div class="right-section">
            <!-- 操作按钮区域 -->
            <div class="button-container">
              <div class="grid-layout">
                <a-button class="grid-button" @click="openModal">
                  <div class="button-inner">
                    <div class="icon-wrapper edit-bg">
                      <EditOutlined class="button-icon" />
                    </div>
                    <span class="button-text">编辑资料</span>
                  </div>
                </a-button>

                <a-button class="grid-button" @click="modifyPasswordopenModal">
                  <div class="button-inner">
                    <div class="icon-wrapper password-bg">
                      <LockOutlined class="button-icon" />
                    </div>
                    <span class="button-text">修改密码</span>
                  </div>
                </a-button>

                <a-button class="grid-button" @click="changeEmailOpenModal">
                  <div class="button-inner">
                    <div class="icon-wrapper email-bg">
                      <MailOutlined class="button-icon" />
                    </div>
                    <span class="button-text">修改邮箱</span>
                  </div>
                </a-button>

                <a-button class="grid-button" @click="aboutUsopenModal">
                  <div class="button-inner">
                    <div class="icon-wrapper about-bg">
                      <InfoCircleOutlined class="button-icon" />
                    </div>
                    <span class="button-text">关于悦木</span>
                  </div>
                </a-button>

                <a-button class="grid-button" @click="handleLoveBoard">
                  <div class="button-inner">
                    <div class="icon-wrapper love-bg">
                      <HeartOutlined class="button-icon" />
                    </div>
                    <span class="button-text">恋爱画板</span>
                  </div>
                </a-button>

                <a-button class="grid-button" @click="showLogoutConfirm">
                  <div class="button-inner">
                    <div class="icon-wrapper destroy-bg">
                      <LogoutOutlined class="button-icon" />
                    </div>
                    <span class="button-text">注销账号</span>
                  </div>
                </a-button>
              </div>
            </div>
            <!-- PC端卡通插画区域 -->
            <div class="illustration-container">
              <div class="illustration-content">
                <div ref="animationContainer" class="animation-container"></div>
                <p class="illustration-text">今天也要开开心心哦 ฅ՞•ﻌ•՞ฅ</p>
              </div>
            </div>
          </div>
        </div>

        <!-- 编辑资料模态框 -->
        <div v-if="open" class="custom-modal edit-profile-modal">
          <div class="modal-overlay" @click="open = false"></div>
          <div class="modal-container">
            <div class="modal-header">
              <h3>编辑资料</h3>
              <div class="close-btn" @click="open = false">×</div>
            </div>

            <div class="modal-body">
              <!-- 头像上传区域 -->
              <div class="avatar-upload-container">
                <div class="avatar-wrapper" @click="showFileUploadDialog">
                  <img
                    :src="myMessage.userAvatar || getDefaultAvatar(myMessage.userName)"
                    class="profile-avatar"
                    alt="用户头像"
                  />
                  <div class="upload-icon">
                    <PlusOutlined />
                  </div>
                </div>
                <input
                  type="file"
                  ref="fileInput"
                  style="display: none"
                  accept="image/*"
                  @change="handleAvatarUpload"
                />
              </div>

              <!-- 头像裁剪组件 -->
              <AvatarCropper
                ref="avatarCropperRef"
                :imageUrl="tempImageUrl"
                @success="handleCroppedAvatar"
              />

              <!-- 表单区域 -->
              <div class="form-container">
                <div class="form-group">
                  <label>昵称</label>
                  <input type="text" :value="myMessage.userName" @input="e => myMessage.userName = e.target.value" class="custom-input" />
                </div>
                <div class="form-group">
                  <label>简介</label>
                  <input type="text" :value="myMessage.userProfile" @input="e => myMessage.userProfile = e.target.value" class="custom-input" />
                </div>
                <div class="form-group">
                  <label>编号</label>
                  <input type="text" :value="myMessage.id" class="custom-input" disabled />
                </div>
                <div class="form-group">
                  <label>邮箱</label>
                  <input type="text" :value="myMessage.email" class="custom-input" disabled />
                </div>
                <div class="form-group">
                  <label>账号</label>
                  <input type="text" :value="myMessage.userAccount" class="custom-input" disabled />
                </div>
                <div class="form-group">
                  <label>角色</label>
                  <input type="text" :value="roleText" class="custom-input" disabled />
                </div>
              </div>

              <div class="modal-footer">
                <button class="submit-button" @click="editProfile">完成</button>
              </div>
            </div>
          </div>
        </div>

        <!-- 修改密码模态框 -->
        <a-modal v-model:open="modifyPasswordOpen" title="修改密码" class="password-modal">
          <div class="password-form">
            <a-form layout="vertical">
              <a-form-item label="旧密码" required>
                <a-input
                  v-model:value="modifyPasswordFormData.oldPassword"
                  type="password"
                  placeholder="请输入旧密码"
                />
              </a-form-item>
              <a-form-item label="新密码" required>
                <a-input
                  v-model:value="modifyPasswordFormData.newPassword"
                  type="password"
                  placeholder="请输入新密码"
                />
              </a-form-item>
              <a-form-item label="确认新密码" required>
                <a-input
                  v-model:value="modifyPasswordFormData.checkPassword"
                  type="password"
                  placeholder="请再次输入新密码"
                />
              </a-form-item>
            </a-form>
          </div>
          <div class="forgot-password-link">
            忘记密码？
            <a @click="handleForgotPassword">点击这里重置</a>
          </div>
          <template #footer>
            <div class="modal-footer">
              <a-button @click="submitPasswordForm" class="submit-button">确认修改</a-button>
            </div>
          </template>
        </a-modal>

        <!-- 修改邮箱模态框 -->
        <a-modal v-model:open="changeEmailOpen" title="修改邮箱">
          <div class="form-container">
            <a-form layout="vertical">
              <a-form-item label="新邮箱">
                <a-input v-model:value="changeEmailForm.newEmail" />
              </a-form-item>
              <a-form-item label="验证码">
                <div class="verify-code-container">
                  <a-input
                    v-model:value="changeEmailForm.code"
                    placeholder="请输入验证码"
                    maxlength="6"
                  />
                  <a-button
                    class="send-code-btn"
                    :disabled="!!countdown || !changeEmailForm.newEmail"
                    @click="sendEmailCode"
                  >
                    {{ countdown ? `${countdown}s后重试` : '获取验证码' }}
                  </a-button>
                </div>
              </a-form-item>
            </a-form>
          </div>
          <template #footer>
            <div style="text-align: center">
              <a-button @click="handleChangeEmail" class="submit-button">完成</a-button>
            </div>
          </template>
        </a-modal>

        <!-- 关于悦木模态框 -->
        <div v-if="aboutUsOpen" class="custom-modal about-modal">
          <div class="modal-overlay" @click="aboutUsOpen = false"></div>
          <div class="modal-container">
            <div class="modal-header">
              <h3>关于悦木</h3>
              <div class="close-btn" @click="aboutUsOpen = false">×</div>
            </div>

            <div class="modal-body">
              <div class="about-content">
                <div class="brand-section">
                  <h2 class="app-name">悦木图片分享</h2>
                  <p class="version">Version 1.0.0</p>
                  <div class="brand-divider"></div>
                </div>

                <div class="info-section">
                  <p class="info-text">本网站部分内容源自网络，仅供学习与参考之用。</p>
                  <p class="info-text">本网站所呈现的全部内容，并不代表本站立场，亦不意味着本站赞同相关观点或对其真实性负责。</p>
                  <p class="info-text">若无意中侵犯了任何企业或个人的知识产权，请通过电子邮箱 <a href="mailto:109484028@qq.com" class="email-link">109484028@qq.com</a> 及时告知我们，一经核实，本网站将立即删除相关内容。</p>
                </div>

                <div class="footer-section">
                  <p class="copyright">© {{ currentYear }} 鹿梦. All rights reserved.</p>
                  <a href="https://beian.miit.gov.cn/" target="_blank" class="icp-link">
                    陇ICP备2024012699号
                  </a>
                </div>

                <div class="contact-button-wrapper">
                  <button class="contact-button" @click="handleClick">
                    <div class="contact-button-content">
                      <LinkOutlined class="contact-icon" />
                      <span>联系我们</span>
                    </div>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 注销确认模态框 -->
        <a-modal
          v-model:open="logoutConfirmOpen"
          :footer="null"
          :width="400"
          class="logout-modal"
          @cancel="logoutConfirmOpen = false"
        >
          <div class="logout-modal-content">
            <div class="warning-icon">
              <ExclamationCircleFilled />
            </div>
            <h3 class="modal-title">确认注销账号？</h3>
            <p class="modal-desc">注销后将无法恢复，您的所有数据将被清除。请谨慎操作！</p>
            <div class="modal-actions">
              <a-button class="cancel-button" @click="logoutConfirmOpen = false"> 取消 </a-button>
              <a-button class="confirm-button" @click="confirmLogout"> 确认注销 </a-button>
            </div>
          </div>
        </a-modal>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import { ref, onMounted, computed, reactive, onBeforeUnmount } from 'vue'
import {
  EditOutlined,
  LockOutlined,
  InfoCircleOutlined,
  LogoutOutlined,
  RightOutlined,
  PlusOutlined,
  ExclamationCircleFilled,
  MailOutlined,
  HeartOutlined,
  LinkOutlined,
} from '@ant-design/icons-vue'

import { addUserSignInUsingPost } from '@/api/userController'
import { Form, message } from 'ant-design-vue'
import {
  changePasswordUsingPost,
  updateUserUsingPost,
  userDestroyUsingPost,
  updateUserAvatarUsingPost,
  getUserSignInRecordUsingGet,
  getEmailCodeUsingPost,
  changeEmailUsingPost,
} from '@/api/userController.ts'
import router from '@/router'
import UserModifyPassWord = API.UserModifyPassWord
import { DEVICE_TYPE_ENUM } from '@/constants/device.ts'
import VChart from 'vue-echarts'
import * as echarts from 'echarts'
import lottie from 'lottie-web'
import { getDeviceType } from '@/utils/device.ts'
import { getFollowAndFansCountUsingPost } from '@/api/userFollowsController.ts'
import AvatarCropper from '@/components/AvatarCropper.vue'
import { getCurrentYear } from '@/utils/date'
import BilibiliSpringBanner from '@/banner/inst/bilibili-spring-1-banner.component.vue'
import SeasonalBanner from '@/banner/SeasonalBanner.vue'

const loginUserStore = useLoginUserStore()
const open = ref<boolean>(false)
const myMessage = ref({
  userName: loginUserStore.loginUser.userName,
  id: loginUserStore.loginUser.id,
  userAccount: loginUserStore.loginUser.userAccount,
  userProfile: loginUserStore.loginUser.userProfile,
  userRole: loginUserStore.loginUser.userRole,
  userAvatar: loginUserStore.loginUser.userAvatar,
  email: loginUserStore.loginUser.email,
})
// 关注和粉丝数据
const followAndFans = ref({
  followCount: 0,
  fansCount: 0
})

// 获取关注和粉丝数量
const getFollowAndFansCount = async () => {
  if (!loginUserStore.loginUser.id) return
  try {
    const res = await getFollowAndFansCountUsingPost({
      id: loginUserStore.loginUser.id
    })
    if (res.data.code === 0) {
      followAndFans.value = res.data.data || { followCount: 0, fansCount: 0 }
    }
  } catch (error) {
    // console.error('获取关注粉丝数失败:', error)
  }
}

// 处理关注列表点击
const handleFollowClick = () => {
  router.push({
    path: '/follow-list',
    query: { tab: 'follow' }
  })
}

// 处理粉丝列表点击
const handleFansClick = () => {
  router.push({
    path: '/follow-list',
    query: { tab: 'fans' }
  })
}

// 页面加载时获取设备类型
onMounted(async () => {
  getFollowAndFansCount()
})
// 根据用户角色计算出对应的显示文本
const roleText = ref<string>(myMessage.value.userRole === 'admin' ? '管理员' : '普通用户')
const openModal = () => {
  open.value = true
}
const editProfile = async () => {
  const res = await updateUserUsingPost(myMessage.value)
  if (res.data.code === 0 && res.data.data) {
    // message.success('修改成功')
    // 获取useLoginUserStore实例
    const loginUserStore = useLoginUserStore()
    // 调用fetchLoginUser方法重新获取用户信息以更新仓库数据
    await loginUserStore.fetchLoginUser()
  } else {
    message.error('修改失败，' + res.data.message)
  }
  open.value = false
}

const modifyPasswordOpen = ref<boolean>(false)
const modifyPasswordopenModal = () => {
  modifyPasswordOpen.value = true
}

// 用于存储表单数据的响应式对象
const modifyPasswordFormData = ref({
  oldPassword: '',
  newPassword: '',
  checkPassword: '',
})

// 获取表单实例的引用
const modifyPasswordForm = ref<Form | null>(null)

const submitPasswordForm = async () => {
  try {
    const userModifyPassword: UserModifyPassWord = {
      id: loginUserStore.loginUser.id,
      oldPassword: modifyPasswordFormData.value.oldPassword,
      newPassword: modifyPasswordFormData.value.newPassword,
      checkPassword: modifyPasswordFormData.value.checkPassword,
    }
    // console.log(userModifyPassword)
    const res = await changePasswordUsingPost(userModifyPassword)
    if (res.data.code === 0) {
      // message.success('修改成功')
      modifyPasswordOpen.value = false
    } else {
      message.error('修改失败，' + res.data.message)
    }
  } catch (error) {
    // console.log(error)
  }
}

const aboutUsOpen = ref<boolean>(false)
const aboutUsopenModal = () => {
  aboutUsOpen.value = true
}

// 控制确认注销模态框的显示与隐藏
const logoutConfirmOpen = ref<boolean>(false)
// 显示确认注销模态框的方法
const showLogoutConfirm = () => {
  logoutConfirmOpen.value = true
}
// 确认注销的方法，在模态框点击确定按钮后执行真正的注销逻辑
const confirmLogout = async () => {
  const id = loginUserStore.loginUser.id
  const res = await userDestroyUsingPost({ id })
  if (res.data.code === 0) {
    message.success('注销成功')
    // 退出登录，清除登录态
    loginUserStore.logout()
    // 重定向到登录页面
    router.push({
      path: '/user/login',
      replace: true,
    })
  } else {
    message.error('注销失败，' + res.data.message)
  }
  logoutConfirmOpen.value = false
}

// 修改文件输入框的引用名称
const fileInput = ref<HTMLInputElement | null>(null)

// 修改显示文件选择对话框的方法
const showFileUploadDialog = () => {
  // 确保DOM元素已经挂载
  if (fileInput.value) {
    fileInput.value.click()
  } else {
    console.error('文件输入框未找到')
  }
}

// 图片裁剪组件引用
const avatarCropperRef = ref()
// 临时图片 URL
const tempImageUrl = ref('')

// 修改头像上传处理方法
const handleAvatarUpload = (e: Event) => {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (file) {
    // 创建临时 URL 并打开裁剪器
    tempImageUrl.value = URL.createObjectURL(file)
    if (avatarCropperRef.value) {
      avatarCropperRef.value.openModal()
    }
    // 清空 input 值，允许重复选择同一文件
    (e.target as HTMLInputElement).value = ''
  }
}

// 处理裁剪后的头像
const handleCroppedAvatar = async (file: File) => {
  try {
    const params = {
      id: loginUserStore.loginUser.id
    }
    const res = await updateUserAvatarUsingPost(params, {}, file, {})
    if (res.data.code === 0) {
      message.success('头像上传成功')
      // 更新头像显示
      myMessage.value.userAvatar = res.data.data
      // 更新全局用户信息
      await loginUserStore.fetchLoginUser()
      // 上传成功后关闭裁剪框
      if (avatarCropperRef.value) {
        avatarCropperRef.value.closeModal()
      }
    }
  } catch (error) {
    console.error('头像上传失败:', error)
    message.error('头像上传失败')
  }
}

// 添加新的响应式变量
const selectedYear = ref(new Date().getFullYear())
const signInData = ref<number[]>([])

// 生成年份选项
const yearOptions = computed(() => {
  const currentYear = new Date().getFullYear()
  return Array.from({ length: 3 }, (_, i) => ({
    label: `${currentYear - i}年`,
    value: currentYear - i
  }))
})

// 获取签到数据
const fetchSignInData = async () => {
  try {
    const res = await getUserSignInRecordUsingGet({
      year: selectedYear.value || new Date().getFullYear()
    })
    if (res.data.code === 0) {
      signInData.value = res.data.data || []
    }
  } catch (error) {
    console.error('获取签到记录失败:', error)
  }
}

// 日历图配置
const calendarOption = computed(() => {
  // 根据屏幕宽度判断是否为移动端
  const isMobile = window.innerWidth <= 768

  return {
    tooltip: {
      formatter: (params: any) => {
        return `${params.value[0]}<br/>${params.value[1]? '✨ 浅浅地踩了一下' : '没有留下足迹'}`
      },
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e2e8f0',
      textStyle: {
        color: '#64748b',
        fontSize: 12
      },
      padding: [8, 12],
      borderRadius: 8
    },
    visualMap: {
      min: 0,
      max: 2,
      calculable: false,
      orient: 'horizontal',
      left: 'center',
      bottom: 0,
      showLabel: false,
      show: false,
      inRange: {
        color: ['#f1f5f9', '#ffd5dc', '#ff8e53']
      }
    },
    calendar: {
      top: 20,
      left: 30,
      right: 30,
      cellSize: ['auto', 20],
      range: selectedYear.value,
      itemStyle: {
        borderWidth: 2,
        borderColor: '#fff',
        borderRadius: 2
      },
      yearLabel: { show: false },
      dayLabel: {
        firstDay: 1,
        nameMap: ['日', '一', '二', '三', '四', '五', '六'],
        color: '#94a3b8',
        fontSize: 12
      },
      monthLabel: {
        nameMap: isMobile
          ? ['1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12']
          : ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月'],
        color: '#64748b',
        fontSize: 12,
        align: 'left'
      }
    },
    series: {
      type: 'heatmap',
      coordinateSystem: 'calendar',
      data: signInData.value.map(day => {
        const date = new Date(selectedYear.value, 0, day)
        return [echarts.format.formatTime('yyyy-MM-dd', date), 1]
      })
    }
  }
})

// 添加自动签到函数
const autoSignIn = async () => {
  if (!loginUserStore.loginUser.id) {
    return
  }

  const res = await addUserSignInUsingPost()
  if (res.data.code === 0) {
    // 签到成功后更新签到数据
    await fetchSignInData()

  }
}

// 在 onMounted 钩子中调用自动签到
onMounted(async () => {
  // 确保用户已登录
  if (loginUserStore.loginUser.id) {
    await autoSignIn()
    await fetchSignInData()// 刷新签到数据显示
  }
})

// 获取默认头像
const getDefaultAvatar = (userName: string) => {
  const defaultName = userName || 'Guest'
  return `https://api.dicebear.com/7.x/adventurer/svg?seed=${encodeURIComponent(defaultName)}&backgroundColor=ffd5dc,ffdfbf,ffd5dc`
}

const animationContainer = ref<HTMLElement | null>(null)

// 初始化 Lottie 动画
onMounted(() => {
  if (animationContainer.value) {
    lottie.loadAnimation({
      container: animationContainer.value,
      renderer: 'svg',
      loop: true,
      autoplay: true,
      path: 'https://assets9.lottiefiles.com/packages/lf20_w51pcehl.json'
    })
  }
})

// 修改邮箱相关
const changeEmailOpen = ref(false)
const changeEmailForm = reactive({
  newEmail: '',
  code: '',
})
const countdown = ref<number>(0)
let timer: NodeJS.Timeout | null = null

// 打开修改邮箱模态框
const changeEmailOpenModal = () => {
  changeEmailOpen.value = true
  changeEmailForm.newEmail = ''
  changeEmailForm.code = ''
}

// 发送邮箱验证码
const sendEmailCode = async () => {
  try {
    const res = await getEmailCodeUsingPost({
      email: changeEmailForm.newEmail,
      type: 'changeEmail'
    })
    if (res.data.code === 0) {
      message.success('验证码已发送')
      countdown.value = 60
      timer = setInterval(() => {
        countdown.value--
        if (countdown.value <= 0) {
          clearInterval(timer!)
          timer = null
        }
      }, 1000)
    } else {
      message.error(res.data.message || '发送失败')
    }
  } catch (error: any) {
    message.error(error.response?.data?.message || '验证码发送失败')
  }
}

// 处理修改邮箱
const handleChangeEmail = async () => {
  try {
    const res = await changeEmailUsingPost(changeEmailForm)
    if (res.data.code === 0) {
      message.success('邮箱修改成功')
      changeEmailOpen.value = false
    } else {
      message.error(res.data.message || '修改失败')
    }
  } catch (error: any) {
    message.error(error.response?.data?.message || '修改失败')
  }
}

// 处理忘记密码
const handleForgotPassword = () => {
  modifyPasswordOpen.value = false
  router.push('/user/reset-password')
}

// 组件卸载时清理定时器
onBeforeUnmount(() => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
})

const currentYear = computed(() => getCurrentYear())

const handleLoveBoard = () => {
  router.push('/loveboard')
}

const handleClick = () => {
  window.location.href = 'http://my.lumenglover.com/contact'
}

</script>

<style scoped>
#SettingView {
  min-height: calc(100vh - 120px);
  margin-left: -20px !important;
  margin-right: -20px !important;
  margin-top: -20px;
  background: #f8fafc;
  position: relative;
  overflow: hidden;
  display: flex;
  justify-content: center;
}

/* 设置界面容器 */
.setting-container {
  max-width: 80%;
  width: 80%;
  margin: 0 auto;
  padding: 24px 20px;
  height: 100%;
  position: relative;
}

/* 响应式调整 */
@media screen and (max-width: 1200px) {
  .setting-container {
    max-width: 100%;
    width: 100%;
    padding: 12px 4px;
  }
}

@media screen and (max-width: 768px) {
  .setting-container {
    padding: 12px 4px;
    width: 100%;
  }
}

/* 流星背景样式 */
.meteor-background {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
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

.meteor-1 {
  top: -10%;
  left: 20%;
  animation-duration: 6s;
  animation-delay: 0s;
}

.meteor-2 {
  top: -10%;
  left: 40%;
  animation-duration: 8s;
  animation-delay: 2s;
}

.meteor-3 {
  top: -10%;
  left: 60%;
  animation-duration: 7s;
  animation-delay: 4s;
}

.meteor-4 {
  top: -10%;
  left: 80%;
  animation-duration: 9s;
  animation-delay: 1s;
}

.meteor-5 {
  top: -10%;
  left: 30%;
  animation-duration: 5s;
  animation-delay: 3s;
}

@keyframes meteor-fall {
  0% {
    transform: translate(0, 0) rotate(45deg);
    opacity: 1;
  }
  20% {
    opacity: 1;
  }
  60% {
    opacity: 0;
  }
  100% {
    transform: translate(500px, 500px) rotate(45deg);
    opacity: 0;
  }
}

.main-content {
  position: relative;
  margin-bottom: 48px;
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

/* 响应式调整 */
@media screen and (max-width: 1200px) {
  .content-layout {
    flex-direction: column;
    padding-top: 12px;
    gap: 12px;
  }

  .left-section,
  .right-section {
    width: 100%;
    max-width: none;
    gap: 12px;
  }

  .right-section {
    flex-direction: row;
    flex-wrap: wrap;
  }

  .button-container {
    flex: 1;
    min-width: 300px;
  }

  .illustration-container {
    flex: 1;
    min-width: 300px;
  }
}

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
    gap: 12px;
  }

  .right-section {
    flex-direction: column;
  }

  #SettingView {
    min-height: 97vh;
    margin: -20px -20px -60px -20px !important;
    padding-bottom: 60px;
  }

  .setting-container {
    padding: 12px 4px;
    width: 100%;
  }

  .main-content {
    padding: 0;
  }

  .content-layout {
    padding-top: 12px;
  }

  .user-info-container,
  .button-container {
    border-radius: 16px;
    margin: 4px;
    box-shadow: none;
  }

  .user-info-container {
    margin: 4px;
    border-radius: 16px;
  }

  .button-container {
    padding: 12px;
  }

  /* 在移动端隐藏流星效果 */
  .meteor-background {
    display: none;
  }
}

/* 用户信息区域 */
.user-info-container {
  background: white;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  height: 112px;
  display: flex;
  align-items: center;
  background: linear-gradient(135deg, rgba(255, 142, 83, 0.05) 0%, rgba(255, 107, 107, 0.05) 100%);
}

.avatar-and-text {
  display: flex;
  align-items: center;
  gap: 16px;
}

.avatar-container {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.user-avatar {
  border-radius: 16px;
  border: 2px solid white;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  background-color: #fff6f3;
}

.text-info-container {
  flex: 1;
}

.user-name {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 4px;
}

.user-id {
  font-size: 13px;
  color: #94a3b8;
}

/* 按钮容器新样式 */
.button-container {
  background: white;
  border-radius: 16px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  height: auto;
  min-height: 240px;
  background: linear-gradient(135deg, rgba(255, 142, 83, 0.05) 0%, rgba(255, 107, 107, 0.05) 100%);
}

/* 宫格布局 */
.grid-layout {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  padding: 4px;
}

/* 宫格按钮样式 */
:deep(.grid-button) {
  height: auto;
  padding: 12px;
  border: none;
  border-radius: 12px;
  background: white;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.03);
}

.icon-wrapper {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

:deep(.button-icon) {
  font-size: 20px;
  color: white;
}

:deep(.button-text) {
  font-size: 13px;
  color: #1a1a1a;
  margin: 0;
}

.button-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

/* 图标背景色 */
.edit-bg {
  background: linear-gradient(135deg, #0ea5e9, #38bdf8);
}

.password-bg {
  background: linear-gradient(135deg, #8b5cf6, #a78bfa);
}

.about-bg {
  background: linear-gradient(135deg, #f59e0b, #fbbf24);
}

.destroy-bg {
  background: linear-gradient(135deg, #ef4444, #f87171);
}

.email-bg {
  background: linear-gradient(135deg, #ff8e53, #ff6b6b);
}

.love-bg {
  background: linear-gradient(135deg, #ec4899, #f472b6);
}

/* 响应式调整 */
@media screen and (max-width: 768px) {
  .grid-layout {
    gap: 8px;
  }

  :deep(.grid-button) {
    padding: 8px;
  }

  .icon-wrapper {
    width: 36px;
    height: 36px;
    border-radius: 8px;
  }

  :deep(.button-icon) {
    font-size: 18px;
  }

  :deep(.button-text) {
    font-size: 12px;
  }
}

/* 移除之前的按钮样式 */
:deep(.custom-button) {
  display: none;
}

/* 模态框样式优化 */
:deep(.ant-modal) {
  border-radius: 20px;
  overflow: hidden;
}

:deep(.ant-modal-content) {
  padding: 24px;
}

:deep(.ant-modal-header) {
  border-bottom: none;
  padding: 0 0 20px 0;
}

:deep(.ant-modal-title) {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
}

:deep(.ant-modal-body) {
  padding: 0;
}

/* 头像上传区域优化 */
.avatar-upload-container {
  position: relative;
  text-align: center;
  padding: 32px 0;
  margin-bottom: 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.avatar-wrapper {
  position: relative;
  display: inline-block;
  padding: 4px;
  background: white;
  border-radius: 50%;
  cursor: pointer;
  margin: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: fit-content;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
  background: linear-gradient(135deg, rgba(255, 142, 83, 0.1) 0%, rgba(255, 107, 107, 0.1) 100%);

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 16px rgba(255, 142, 83, 0.15);
  }

  &:active {
    transform: translateY(0);
  }
}

:deep(.ant-avatar) {
  border-radius: 50%;
  border: 2px solid white;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
  width: 80px !important;
  height: 80px !important;
  background-color: #fff6f3;

  &:hover {
    border-color: #ff8e53;
  }
}

.upload-icon {
  position: absolute;
  bottom: -4px;
  right: -4px;
  width: 28px;
  height: 28px;
  background: linear-gradient(135deg, #ff8e53 0%, #ff6b6b 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(255, 107, 107, 0.3);

  border: 2px solid white;
}

/* 确保上传图标可以被点击 */
.upload-icon:hover {
  transform: scale(1.1);
  box-shadow: 0 4px 12px rgba(255, 107, 107, 0.4);
}

/* 响应式调整 */
@media screen and (max-width: 768px) {
  .avatar-upload-container {
    padding: 24px 0;
    margin-bottom: 20px;
    width: 100%;
  }

  .avatar-wrapper {
    padding: 3px;
    margin: 0 auto;
  }

  :deep(.ant-avatar) {
    width: 72px !important;
    height: 72px !important;
  }

  .upload-icon {
    width: 24px;
    height: 24px;
    bottom: -3px;
    right: -3px;
  }
}

/* 添加加载状态的样式 */
.uploading {
  opacity: 0.7;
  pointer-events: none;
}

/* 提交按钮样式 */
:deep(.submit-button) {
  background: linear-gradient(135deg, #ff8e53 0%, #ff6b6b 100%);
  border: none;
  color: white;
  width: 200px;
  height: 44px;
  border-radius: 22px;
  font-size: 15px;
  font-weight: 500;
  box-shadow: 0 4px 12px rgba(255, 107, 107, 0.2);
  transition: all 0.3s ease;
}

:deep(.submit-button:hover) {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(255, 107, 107, 0.3);
}

:deep(.submit-button:active) {
  transform: translateY(1px);
}

/* 表单样式优化 */
.form-container {
  padding: 0;
}

:deep(.ant-form-item) {
  margin-bottom: 20px;
}

:deep(.ant-form-item-label) {
  padding-bottom: 4px;
}

:deep(.ant-form-item-label > label) {
  font-size: 14px;
  color: #64748b;
}

:deep(.ant-input) {
  border-radius: 10px;
  border-color: #e2e8f0;
  padding: 8px 12px;
  transition: all 0.3s ease;
}

:deep(.ant-input:hover) {
  border-color: #ff8e53;
}

:deep(.ant-input:focus) {
  border-color: #ff8e53;
  box-shadow: 0 0 0 2px rgba(255, 142, 83, 0.1);
}

:deep(.ant-input[disabled]) {
  background: #f8fafc;
  color: #94a3b8;
  border-color: #e2e8f0;
}

/* 修改密码表单样式 */
.form-item-wrapper {
  margin-bottom: 16px;
}

.form-label {
  font-size: 14px;
  color: #64748b;
  margin-bottom: 4px;
  display: block;
}

:deep(.form-input) {
  border-radius: 10px;
  border-color: #e2e8f0;
}

/* 模态框按钮样式 */
:deep(.ant-modal-footer) {
  text-align: center;
  border-top: none;
  padding: 24px 0 0 0;
}

:deep(.ant-modal-footer .ant-btn-primary) {
  background: linear-gradient(135deg, #ff8e53 0%, #ff6b6b 100%);
  border: none;
  width: 200px;
  height: 44px;
  border-radius: 22px;
  font-size: 15px;
  font-weight: 500;
  box-shadow: 0 4px 12px rgba(255, 107, 107, 0.2);
  transition: all 0.3s ease;
}

:deep(.ant-modal-footer .ant-btn-primary:hover) {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(255, 107, 107, 0.3);
}

:deep(.ant-modal-footer .ant-btn-primary:active) {
  transform: translateY(1px);
}

/* 关于我们模态框样式 */
:deep(.ant-modal.about-modal .ant-modal-body) {
  text-align: center;
  padding: 32px 0;
}

:deep(.ant-modal.about-modal p) {
  font-size: 15px;
  color: #1a1a1a;
  margin-bottom: 8px;
}

:deep(.ant-modal.about-modal a) {
  color: #94a3b8;
  text-decoration: none;
  font-size: 13px;
  transition: color 0.3s ease;
}

:deep(.ant-modal.about-modal a:hover) {
  color: #ff8e53;
}

/* 确认注销模态框样式 */
:deep(.ant-modal.confirm-modal .ant-modal-body) {
  padding: 32px 24px;
  text-align: center;
  font-size: 15px;
  color: #1a1a1a;
}

/* 响应式调整 */
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
    gap: 12px;
    min-width: unset;
  }

  #SettingView {
    min-height: 97vh;
    margin: -20px -20px -60px -20px !important;
    padding-bottom: 60px;
  }

  .setting-container {
    padding: 12px 4px;
    width: 100%;
  }

  .main-content {
    padding: 0;
  }

  .content-layout {
    padding-top: 12px;
  }

  .user-info-container,
  .button-container {
    border-radius: 16px;
    margin: 4px;
    box-shadow: none;
  }

  .user-info-container {
    margin: 4px;
    border-radius: 16px;
  }

  .button-container {
    padding: 12px;
  }

  /* 在移动端隐藏流星效果 */
  .meteor-background {
    display: none;
  }
}

/* 修改密码模态框样式 */
.password-form {
  padding: 0 12px;
}

:deep(.password-modal .ant-form-item-label > label) {
  font-size: 14px;
  color: #64748b;
}

:deep(.password-modal .ant-form-item-label > label.ant-form-item-required::before) {
  color: #ff6b6b;
}

:deep(.password-modal .ant-input) {
  height: 42px;
  border-radius: 10px;
  border-color: #e2e8f0;
  transition: all 0.3s ease;
}

:deep(.password-modal .ant-input:hover) {
  border-color: #ff8e53;
}

:deep(.password-modal .ant-input:focus) {
  border-color: #ff8e53;
  box-shadow: 0 0 0 2px rgba(255, 142, 83, 0.1);
}

/* 关于悦木模态框样式 */
.about-content {
  padding: 32px 0;
  text-align: center;
}

.app-name {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 8px;
}

.version {
  font-size: 14px;
  color: #64748b;
  margin-bottom: 20px;
}

.divider {
  width: 40px;
  height: 2px;
  background: linear-gradient(90deg, #ff8e53, #ff6b6b);
  margin: 20px auto;
  border-radius: 1px;
}

.copyright {
  font-size: 14px;
  color: #1a1a1a;
  margin-bottom: 8px;
}

.icp-link {
  font-size: 13px;
  color: #94a3b8;
  text-decoration: none;
  transition: color 0.3s ease;
  display: inline-block;
  padding: 4px 8px;
  border-radius: 6px;
}

.icp-link:hover {
  color: #ff8e53;
  background: rgba(255, 142, 83, 0.05);
}

/* 模态框通用样式 */
:deep(.modal-footer) {
  text-align: center;
  padding-top: 24px;
}

:deep(.submit-button) {
  background: linear-gradient(135deg, #ff8e53 0%, #ff6b6b 100%);
  border: none;
  color: white;
  width: 200px;
  height: 44px;
  border-radius: 22px;
  font-size: 15px;
  font-weight: 500;
  box-shadow: 0 4px 12px rgba(255, 107, 107, 0.2);
  transition: all 0.3s ease;
}

:deep(.submit-button:hover) {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(255, 107, 107, 0.3);
}

:deep(.submit-button:active) {
  transform: translateY(1px);
}

/* 响应式调整 */
@media screen and (max-width: 768px) {
  .password-form {
    padding: 0 8px;
  }

  .about-content {
    padding: 20px 0;
  }

  .app-name {
    font-size: 18px;
  }
}

/* 注销模态框样式 */
:deep(.logout-modal) {
  .ant-modal-content {
    padding: 0;
    border-radius: 16px;
    overflow: hidden;
  }

  .ant-modal-close {
    color: #94a3b8;
    transition: all 0.3s ease;

    &:hover {
      color: #64748b;
      background: rgba(0, 0, 0, 0.02);
    }
  }

  .ant-modal-body {
    padding: 0;
  }
}

.logout-modal-content {
  padding: 32px 24px;
  text-align: center;
}

.warning-icon {
  font-size: 48px;
  color: #ff6b6b;
  margin-bottom: 16px;

  .anticon {
    animation: pulse 2s infinite;
  }
}

.modal-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 12px;
}

.modal-desc {
  font-size: 14px;
  color: #64748b;
  margin-bottom: 24px;
  line-height: 1.6;
  padding: 0 16px;
}

.modal-actions {
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

/* 响应式调整 */
@media screen and (max-width: 768px) {
  .logout-modal-content {
    padding: 24px 16px;
  }

  .warning-icon {
    font-size: 40px;
  }

  .modal-title {
    font-size: 16px;
  }

  .modal-desc {
    font-size: 13px;
    padding: 0 8px;
  }

  .modal-actions {
    gap: 8px;
  }

  .cancel-button,
  .confirm-button {
    min-width: 90px;
    height: 36px;
    font-size: 13px;
  }
}

/* 签到日历样式 */
.sign-in-calendar {
  background: white;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  height: calc(100% - 128px);
  flex-direction: column;
  margin-top: 16px;
  width: 100%;
  background: linear-gradient(135deg, rgba(14, 165, 233, 0.05) 0%, rgba(139, 92, 246, 0.05) 100%);
}

.calendar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.calendar-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0;
}

.year-selector {
  width: 120px;
}

.calendar-chart {
  height: 200px;
  width: 100% !important;
  margin-top: 8px;
  flex: 1;
}

/* 下拉选择器样式 */
:deep(.ant-select-selector) {
  border-radius: 8px !important;
  border-color: #e2e8f0 !important;
  height: 32px !important;
  line-height: 32px !important;
}

:deep(.ant-select-selection-item) {
  line-height: 30px !important;
  font-size: 13px;
  color: #64748b;
}

/* 响应式调整 */
@media screen and (max-width: 768px) {
  .sign-in-calendar {
    padding: 16px 12px;
    margin: 12px 0;
    border-radius: 12px;
    min-height: 280px;
    width: calc(100% - 8px);
    margin: 4px auto;
  }

  .calendar-header {
    margin-bottom: 12px;
  }

  .calendar-header h3 {
    font-size: 15px;
    color: #334155;
  }

  .year-selector {
    width: 90px;
  }

  .calendar-chart {
    height: auto;
    margin-top: 4px;
    min-height: 200px;
    width: 100% !important;
  }

  /* 优化日历在移动端的显示 */
  :deep(.calendar-chart) {
    .echarts-tooltip {
      padding: 4px 8px !important;
      border-radius: 6px !important;
      font-size: 12px !important;
    }

    .calendar-heatmap text {
      font-size: 11px !important;
    }
  }
}

/* 插画容器样式 */
.illustration-container {
  background: white;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  text-align: center;
  flex-direction: column;
  justify-content: center;
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.05) 0%, rgba(14, 165, 233, 0.05) 100%);
}

.illustration-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.animation-container {
  width: 160px;
  height: 160px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: center;
}

.illustration-text {
  font-size: 14px;
  color: #64748b;
  margin: 0;
  margin-top: 8px;
}

/* 仅在PC端显示插画 */
@media screen and (min-width: 769px) {
  .illustration-container {
    display: block;
  }

  .button-container {
    flex: 0 0 auto;
  }
}

/* 移动端隐藏插画 */
@media screen and (max-width: 768px) {
  .illustration-container {
    display: none;
  }
}

.user-stats {
  display: flex;
  align-items: center;
  margin-top: 4px;
  gap: 8px;
  font-size: 13px;
  color: #64748b;
}

.stat-item {
  position: relative;
  cursor: pointer;
  transition: color 0.2s ease;
}

.stat-divider {
  color: #cbd5e1;
}

/* 点击效果 */
.stat-item:active {
  color: #1a1a1a;
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .user-info-container {
    padding: 20px 16px;
  }

  .user-meta {
    margin-top: 6px;
  }
}

.forgot-password-link {
  text-align: center;
  margin-top: -12px;
  margin-bottom: 16px;
  font-size: 14px;
  color: #64748b;
}

.forgot-password-link a {
  color: #ff8e53;
  margin-left: 4px;
  cursor: pointer;
  transition: color 0.3s ease;
}

.forgot-password-link a:hover {
  color: #ff7a3d;
}

.verify-code-container {
  display: flex;
  gap: 12px;
}

.verify-code-container .ant-input {
  flex: 1;
}

.send-code-btn {
  min-width: 120px;
  height: 32px;
  border-radius: 6px;
  background: linear-gradient(135deg, #ff8e53 0%, #ff6b6b 100%);
  border: none;
  color: white;
  font-size: 14px;
  transition: all 0.3s ease;
}

.send-code-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(255, 107, 107, 0.2);
}

.send-code-btn:disabled {
  background: #e2e8f0;
  color: #94a3b8;
  cursor: not-allowed;
}

.email-icon {
  color: #ff8e53;
  font-size: 18px;
}

/* 修改邮箱按钮特殊样式 */
.custom-button:has(.email-icon) {
  background: linear-gradient(to right, rgba(255, 142, 83, 0.1), rgba(255, 107, 107, 0.1));
  border-color: transparent;

  &:hover {
    background: linear-gradient(to right, rgba(255, 142, 83, 0.15), rgba(255, 107, 107, 0.15));
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(255, 142, 83, 0.1);
  }

  &:active {
    transform: translateY(1px);
  }
}

:deep(.love-icon) {
  color: #ff69b4;
}

:deep(.custom-button:has(.love-icon)) {
  background: rgba(255, 105, 180, 0.05);
}

:deep(.custom-button:has(.love-icon):hover) {
  background: rgba(255, 105, 180, 0.1);
}

.contact-button-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.contact-button {
  background: linear-gradient(135deg, #ff8e53, #ff6b6b);
  border: none;
  color: white;
  height: 40px;
  border-radius: 20px;
  padding: 0 28px;
  font-size: 14px;
  font-weight: 500;
  box-shadow: 0 4px 12px rgba(255, 142, 83, 0.25);
  transition: all 0.3s ease;
}

.contact-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(255, 142, 83, 0.35);
  background: linear-gradient(135deg, #ff9b69, #ff8282);
}

.contact-button:active {
  transform: translateY(0);
  box-shadow: 0 2px 8px rgba(255, 142, 83, 0.2);
}

.contact-button-content {
  display: flex;
  align-items: center;
  gap: 8px;
}

.contact-icon {
  font-size: 16px;
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .contact-button {
    height: 36px;
    padding: 0 24px;
    font-size: 13px;
  }

  .contact-icon {
    font-size: 14px;
  }
}

/* 自定义模态框基础样式 */
.custom-modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(4px);
}

.modal-container {
  position: relative;
  width: 90%;
  max-width: 560px;
  background: white;
  border-radius: 24px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
  z-index: 1001;
  overflow: hidden;
  animation: modalPop 0.3s ease-out;
}

.modal-header {
  padding: 24px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-header h3 {
  margin: 0;
  font-size: 20px;
  color: #1a1a1a;
  font-weight: 600;
}

.close-btn {
  width: 32px;
  height: 32px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 24px;
  color: #666;
  transition: all 0.3s ease;
  background: transparent;
}

.close-btn:hover {
  background: rgba(0, 0, 0, 0.05);
  color: #ff6b6b;
}

.modal-body {
  padding: 24px;
  max-height: calc(90vh - 180px);
  overflow-y: auto;
}

/* 编辑资料模态框样式 */
.edit-profile-modal .modal-container {
  background: linear-gradient(135deg, #fff 0%, #f8fafc 100%);
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1),
  0 0 0 1px rgba(255, 255, 255, 0.8),
  0 8px 16px -4px rgba(255, 142, 83, 0.1);
}

.edit-profile-modal .modal-header {
  background: linear-gradient(135deg, rgba(255, 142, 83, 0.05), rgba(255, 107, 107, 0.05));
  border-bottom: 1px solid rgba(255, 142, 83, 0.1);
  padding: 24px 32px;
}

.edit-profile-modal .modal-header h3 {
  font-size: 22px;
  background: linear-gradient(135deg, #ff8e53, #ff6b6b);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  font-weight: 600;
}

.edit-profile-modal .modal-body {
  padding: 32px;
}

.edit-profile-modal .avatar-upload-container {
  margin-bottom: 40px;
  position: relative;
}

.edit-profile-modal .avatar-wrapper {
  position: relative;
  width: 120px;
  height: 120px;
  margin: 0 auto;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(255, 142, 83, 0.1), rgba(255, 107, 107, 0.1));
  padding: 4px;
  transition: all 0.3s ease;
  cursor: pointer;
}

.edit-profile-modal .profile-avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
  border: 2px solid #fff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.edit-profile-modal .upload-icon {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, #ff8e53, #ff6b6b);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  border: 3px solid white;
  box-shadow: 0 4px 12px rgba(255, 142, 83, 0.3);
  cursor: pointer;
  transition: all 0.3s ease;
}

.edit-profile-modal .form-group {
  margin-bottom: 24px;
  position: relative;
}

.edit-profile-modal .form-group label {
  display: block;
  margin-bottom: 8px;
  color: #64748b;
  font-size: 14px;
  font-weight: 500;
}

.edit-profile-modal .custom-input {
  width: 100%;
  height: 44px;
  padding: 0 16px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  font-size: 14px;
  color: #1a1a1a;
  transition: all 0.3s ease;
  background: white;
}

.edit-profile-modal .custom-input:hover:not(:disabled) {
  border-color: #ff8e53;
}

.edit-profile-modal .custom-input:focus {
  outline: none;
  border-color: #ff8e53;
  box-shadow: 0 0 0 3px rgba(255, 142, 83, 0.1);
}

.edit-profile-modal .custom-input:disabled {
  background: #f8fafc;
  color: #94a3b8;
  cursor: not-allowed;
  border-color: #e2e8f0;
}

.edit-profile-modal .modal-footer {
  padding: 24px 32px;
  text-align: center;
  border-top: 1px solid rgba(255, 142, 83, 0.1);
  background: linear-gradient(135deg, rgba(255, 142, 83, 0.02), rgba(255, 107, 107, 0.02));
}

.edit-profile-modal .submit-button {
  min-width: 160px;
  height: 44px;
  background: linear-gradient(135deg, #ff8e53, #ff6b6b);
  border: none;
  border-radius: 22px;
  color: white;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(255, 142, 83, 0.25);
}

.edit-profile-modal .submit-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(255, 142, 83, 0.35);
}

.edit-profile-modal .submit-button:active {
  transform: translateY(1px);
  box-shadow: 0 2px 8px rgba(255, 142, 83, 0.2);
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .edit-profile-modal .modal-container {
    width: 92%;
    margin: 16px;
  }

  .edit-profile-modal .modal-header {
    padding: 20px 24px;
  }

  .edit-profile-modal .modal-header h3 {
    font-size: 20px;
  }

  .edit-profile-modal .modal-body {
    padding: 24px;
  }

  .edit-profile-modal .avatar-wrapper {
    width: 100px;
    height: 100px;
  }

  .edit-profile-modal .upload-icon {
    width: 32px;
    height: 32px;
  }

  .edit-profile-modal .form-group {
    margin-bottom: 20px;
  }

  .edit-profile-modal .custom-input {
    height: 40px;
  }

  .edit-profile-modal .modal-footer {
    padding: 20px 24px;
  }

  .edit-profile-modal .submit-button {
    width: 100%;
    max-width: 280px;
  }
}

/* 关于悦木模态框特定样式 */
.about-modal .modal-container {
  max-width: 640px;
}

.about-content {
  padding: 24px;
}

.brand-section {
  text-align: center;
  margin-bottom: 32px;
}

.app-name {
  font-size: 28px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 8px;
}

.version {
  font-size: 14px;
  color: #64748b;
  margin: 0;
}

.brand-divider {
  width: 60px;
  height: 3px;
  background: linear-gradient(90deg, #ff8e53, #ff6b6b);
  margin: 24px auto;
  border-radius: 1.5px;
}

.info-section {
  margin-bottom: 32px;
  padding: 24px;
  background: linear-gradient(135deg, rgba(255, 142, 83, 0.05), rgba(255, 107, 107, 0.05));
  border-radius: 16px;
}

.info-text {
  font-size: 14px;
  line-height: 1.8;
  color: #334155;
  margin: 0 0 16px;
  text-align: justify;
}

.info-text:last-child {
  margin-bottom: 0;
}

.email-link {
  color: #ff8e53;
  text-decoration: none;
  transition: color 0.3s ease;
}

.email-link:hover {
  color: #ff6b6b;
}

.footer-section {
  text-align: center;
  margin-bottom: 24px;
}

.copyright {
  font-size: 14px;
  color: #64748b;
  margin: 0 0 8px;
}

.icp-link {
  color: #94a3b8;
  text-decoration: none;
  font-size: 13px;
  transition: all 0.3s ease;
  display: inline-block;
  padding: 4px 12px;
  border-radius: 6px;
}

.icp-link:hover {
  color: #ff8e53;
  background: rgba(255, 142, 83, 0.05);
}

.contact-button-wrapper {
  text-align: center;
}

.contact-button {
  background: linear-gradient(135deg, #ff8e53, #ff6b6b);
  border: none;
  color: white;
  height: 44px;
  border-radius: 22px;
  padding: 0 32px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(255, 142, 83, 0.25);
  transition: all 0.3s ease;
}

.contact-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(255, 142, 83, 0.35);
}

.contact-button:active {
  transform: translateY(0);
}

.contact-button-content {
  display: flex;
  align-items: center;
  gap: 8px;
}

.contact-icon {
  font-size: 18px;
}

@keyframes modalPop {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .about-modal .modal-container {
    width: 92%;
    margin: 16px;
  }

  .about-modal .modal-header {
    padding: 20px 24px;
  }

  .about-modal .modal-body {
    padding: 20px;
  }

  .about-content {
    padding: 0;
  }

  .brand-section {
    margin-bottom: 24px;
  }

  .app-name {
    font-size: 24px;
  }

  .brand-divider {
    width: 48px;
    margin: 20px auto;
  }

  .info-section {
    padding: 20px;
    margin-bottom: 24px;
  }

  .info-text {
    font-size: 13px;
  }

  .contact-button {
    width: 100%;
    max-width: 280px;
  }
}
</style>

