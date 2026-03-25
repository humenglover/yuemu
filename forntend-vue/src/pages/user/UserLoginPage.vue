<template>
  <div class="box" :class="{ 'is-loaded': bgLoaded }">
    <div class="content">
      <div class="login-wrapper">
        <!-- 猫头鹰交互 -->
        <div class="owl-container">
          <div class="owl" :class="{ password: isPasswordFocused }">
            <div class="hand"></div>
            <div class="hand hand-r"></div>
            <div class="arms">
              <div class="arm"></div>
              <div class="arm arm-r"></div>
            </div>
          </div>
        </div>

        <!-- Apple Segmented Control -->
        <div class="seg-control">
          <div
            class="seg-thumb"
            :style="{ transform: loginType === 'wechat' ? 'translateX(100%)' : 'translateX(0)' }"
          ></div>
          <button
            class="seg-btn"
            :class="{ active: loginType === 'account' }"
            @click="loginType = 'account'"
          >账号登录</button>
          <button
            class="seg-btn"
            :class="{ active: loginType === 'wechat' }"
            @click="loginType = 'wechat'"
          >微信登录</button>
        </div>

        <!-- 账号登录面板 -->
        <Transition name="tab-fade" mode="out-in">
          <div v-if="loginType === 'account'" key="account" class="tab-panel">
            <a-form
              :model="formState"
              name="basic"
              autocomplete="off"
              @finish="handleSubmit"
              class="auth-form"
            >
              <a-form-item
                name="accountOrEmail"
                :rules="[{ required: true, message: '请输入邮箱或悦木号' }]"
              >
                <a-input
                  v-model:value="formState.accountOrEmail"
                  placeholder="邮箱 / 悦木号"
                  size="large"
                  :prefix="h(UserOutlined)"
                  class="premium-input"
                />
              </a-form-item>

              <a-form-item
                name="userPassword"
                :rules="[
                  { required: true, message: '请输入密码' },
                  { min: 8, message: '密码长度不能小于 8 位' },
                ]"
              >
                <a-input-password
                  v-model:value="formState.userPassword"
                  placeholder="请输入密码"
                  size="large"
                  :prefix="h(LockOutlined)"
                  class="premium-input"
                  @focus="handlePasswordFocus"
                  @blur="handlePasswordBlur"
                />
              </a-form-item>

              <!-- 紧凑型验证码 -->
              <a-form-item
                name="verifyCode"
                :rules="[{ required: true, message: '请输入4位验证码' }]"
              >
                <div class="compact-verify-container">
                  <div class="digit-inputs">
                    <input
                      v-for="(item, index) in 4"
                      :key="index"
                      v-model="codeList[index]"
                      type="text"
                      maxlength="1"
                      class="digit-input"
                      placeholder="•"
                      @input="handleCodeInput($event, index)"
                      @keydown.backspace="handleCodeBackspace(index)"
                      ref="codeInputRefs"
                    />
                  </div>
                  <div class="code-captcha" @click="getVerifyCode">
                    <img :src="verifyCodeImg" alt="验证码" v-if="verifyCodeImg" />
                    <div class="captcha-placeholder" v-else>
                      <a-spin size="small" />
                    </div>
                  </div>
                </div>
              </a-form-item>

              <div class="auth-links">
                <RouterLink to="/user/register" class="link-item">立即注册</RouterLink>
                <span class="divider-dot"></span>
                <RouterLink to="/user/reset-password" class="link-item">忘记密码</RouterLink>
              </div>

              <a-form-item>
                <a-button type="primary" html-type="submit" class="auth-submit-btn" size="large">
                  开启探索
                </a-button>
              </a-form-item>
            </a-form>
          </div>

          <!-- 微信登录面板 -->
          <div v-else key="wechat" class="tab-panel">
            <div class="wechat-auth-pane">
              <div class="qr-mask">
                <div class="qr-frame">
                  <img src="@/assets/wx.png" alt="QR" class="qr-image" />
                </div>
              </div>

              <div class="wechat-mini-guide">
                <p class="qr-guide">微信扫码关注，并发送下方验证码</p>

                <div class="code-display" @click="fetchWxReqCode">
                  <span class="code-text" :class="{ 'highlight': wxReqCode !== '获取中...' && wxReqCode !== '已过期' }">
                    {{ wxReqCode }}
                  </span>
                </div>

                <p class="refresh-tip" v-if="wxReqCode === '已过期'" @click="fetchWxReqCode">点击重新获取验证码</p>

                <div class="combined-tip" style="margin-top: 16px;">
                  <span class="reg-badge">极速体验</span>
                  <span class="safety-text">无需操作，发送后网页自动跳转</span>
                </div>
              </div>
            </div>
          </div>
        </Transition>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { reactive, ref, onMounted, onUnmounted, h, watch } from 'vue'
import { getCodeUsingGet, userLoginUsingPost, reqWxLoginCodeUsingGet, checkWxLoginStatusUsingGet } from '@/api/userController'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { message } from 'ant-design-vue'
import { useRoute } from 'vue-router'
import router from '@/router'
import { UserOutlined, LockOutlined, SafetyCertificateOutlined } from '@ant-design/icons-vue'
import bgPc from '@/assets/images/login_two.jpg'
import bgMobile from '@/assets/images/login_bg_phone.png'

const bgLoaded = ref(false)

const route = useRoute()
const loginType = ref<'account' | 'wechat'>('account')

// 初始化时检查路由参数
onMounted(() => {
  // 图片预加载逻辑
  const isMobile = window.innerWidth <= 768
  const imgUrl = isMobile ? bgMobile : bgPc
  const img = new Image()
  img.src = imgUrl
  img.onload = () => { bgLoaded.value = true }
  img.onerror = () => { bgLoaded.value = true }

  if (route.query.type === 'wechat') {
    loginType.value = 'wechat'
    fetchWxReqCode()
  }
  getVerifyCode()
})

onUnmounted(() => {
  stopCheckStatusTimer()
})

const formState = reactive<API.UserLoginRequest>({
  accountOrEmail: '',
  userPassword: '',
  serververifycode: '',
  verifyCode: '',
})

const verifyCodeImg = ref<string>('')
const loginUserStore = useLoginUserStore()
const isPasswordFocused = ref(false)

// 账号登录验证码方格相关
const codeList = ref(['', '', '', ''])
const codeInputRefs = ref<HTMLInputElement[]>([])

// 微信登录相关状态
const wxReqCode = ref<string>('获取中...')
const wxSceneId = ref<string>('')
let checkStatusTimer: any = null

// 监听codeList变化
watch(codeList, (newVal) => {
  formState.verifyCode = newVal.join('')
}, { deep: true })

// 切换到微信登录时获取验证码
watch(loginType, (val) => {
  if (val === 'wechat') {
    fetchWxReqCode()
  } else {
    stopCheckStatusTimer()
  }
})

const handlePasswordFocus = () => {
  isPasswordFocused.value = true
}

const handlePasswordBlur = () => {
  isPasswordFocused.value = false
}

const getVerifyCode = async () => {
  const res = await getCodeUsingGet()
  if (res.data.code === 0 && res.data.data) {
    verifyCodeImg.value = 'data:image/jpeg;base64,' + res.data.data.base64Captcha
    formState.serververifycode = res.data.data.encryptedCaptcha
    codeList.value = ['', '', '', '']
  }
}

const handleCodeInput = (e: Event, index: number) => {
  const input = e.target as HTMLInputElement
  const value = input.value.replace(/\D/g, '')
  codeList.value[index] = value
  input.value = value

  if (value && index < 3) {
    codeInputRefs.value[index + 1]?.focus()
  }
}

const handleCodeBackspace = (index: number) => {
  if (!codeList.value[index] && index > 0) {
    codeList.value[index - 1] = ''
    codeInputRefs.value[index - 1]?.focus()
  }
}

const handleSubmit = async (values: any) => {
  values.serververifycode = formState.serververifycode
  const res = await userLoginUsingPost(values)
  if (res.data.code === 0 && res.data.data) {
    await loginUserStore.fetchLoginUser()
    message.success('欢迎回来')
    router.push({
      path: '/home',
      replace: true,
    })
  } else {
    message.error('登录失败：' + res.data.message)
    getVerifyCode()
  }
}

const fetchWxReqCode = async () => {
  wxReqCode.value = '获取中...'
  const res = await reqWxLoginCodeUsingGet()
  if (res.data.code === 0 && res.data.data) {
    wxReqCode.value = res.data.data.code as string
    wxSceneId.value = res.data.data.sceneId as string
    startCheckStatusTimer()
  } else {
    message.error('获取验证码失败: ' + res.data.message)
    wxReqCode.value = '已过期'
  }
}

const startCheckStatusTimer = () => {
  stopCheckStatusTimer()
  checkStatusTimer = setInterval(async () => {
    if (!wxSceneId.value) return
    const res = await checkWxLoginStatusUsingGet({ sceneId: wxSceneId.value })
    if (res.data.code === 0) {
      if (res.data.data) {
        // 返回了 LoginUserVO 说明成功
        stopCheckStatusTimer()
        await loginUserStore.fetchLoginUser()
        message.success('登录成功')
        router.push({ path: '/home', replace: true })
      }
    } else {
      stopCheckStatusTimer()
      message.error(res.data.message || '验证码已过期，请刷新重试')
      wxReqCode.value = '已过期'
    }
  }, 2000)
}

const stopCheckStatusTimer = () => {
  if (checkStatusTimer) {
    clearInterval(checkStatusTimer)
    checkStatusTimer = null
  }
}
</script>

<style scoped>
.box {
  position: fixed;
  inset: 0;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f0f2f5;
  z-index: 1000;
}

.content {
  width: 85vw;
  max-width: 1400px;
  min-height: 80vh;
  border-radius: 24px;
  background-color: #fff;
  position: relative;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

/* 呼吸态骨架背景 */
.content::before {
  content: '';
  position: absolute;
  inset: 0;
  background-color: #f1f5f9;
  animation: bgPulse 1.5s ease-in-out infinite;
  z-index: 0;
  transition: opacity 0.8s ease-in-out;
}

@keyframes bgPulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}

/* 真实图片背景 */
.content::after {
  content: '';
  position: absolute;
  inset: 0;
  background: #fff url("@/assets/images/login_two.jpg") no-repeat;
  background-size: 80% 100%;
  background-position: left;
  z-index: 0;
  opacity: 0;
  transition: opacity 0.8s ease-out;
}

/* 加载完毕控制 */
.box.is-loaded .content::before {
  opacity: 0;
  animation: none;
}

.box.is-loaded .content::after {
  opacity: 1;
}

.login-wrapper {
  position: relative;
  z-index: 1;
  width: 32vw;
  margin-right: 8%;
  padding: 40px;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  animation: fadeInRight 0.6s cubic-bezier(0.23, 1, 0.32, 1);
}

@keyframes fadeInRight {
  from { opacity: 0; transform: translateX(30px); }
  to { opacity: 1; transform: translateX(0); }
}

/* 头部样式 */
.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.title {
  font-size: 42px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
  background: linear-gradient(135deg, #1e293b 0%, #334155 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

/* 猫头鹰交互区 */
.owl-container {
  display: flex;
  justify-content: center;
  height: 60px;
  margin-bottom: 28px;
}

.owl {
  width: 180px;
  height: 90px;
  background: url("@/assets/images/owl/owl-login.png") no-repeat center bottom;
  background-size: contain;
  position: relative;
  transition: all 0.3s ease;
}

.owl .hand {
  width: 30px;
  height: 30px;
  background: #4a332a;
  border-radius: 50%;
  position: absolute;
  left: 14px;
  bottom: -6px;
  transform: scaleY(0.5);
  transition: 0.3s;
}

.owl .hand.hand-r { left: 136px; }

.owl.password .hand { transform: translate(36px, -20px) scale(0.7); }
.owl.password .hand.hand-r { transform: translate(-36px, -20px) scale(0.7); }

.owl .arms {
  position: absolute;
  top: 45px;
  width: 100%;
  height: 35px;
  overflow: hidden;
}

.owl .arm {
  width: 34px;
  height: 55px;
  position: absolute;
  left: 22px;
  top: 35px;
  background: url("@/assets/images/owl/owl-login-arm.png") no-repeat;
  background-size: contain;
  transform: rotate(-25deg);
  transition: 0.3s;
}

.owl .arm.arm-r { left: 124px; transform: rotate(25deg) scaleX(-1); }

.owl.password .arm { transform: translateY(-35px) translateX(35px); }
.owl.password .arm.arm-r { transform: translateY(-35px) translateX(-35px) scaleX(-1); }

/* Apple Segmented Control */
.seg-control {
  display: flex;
  position: relative;
  background: #e9e9eb;
  border-radius: 9px;
  padding: 2px;
  margin-bottom: 28px;
  gap: 0;
}

.seg-thumb {
  position: absolute;
  top: 2px;
  left: 2px;
  width: calc(50% - 2px);
  height: calc(100% - 4px);
  background: #ffffff;
  border-radius: 7px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.15), 0 0 0 0.5px rgba(0, 0, 0, 0.06);
  transition: transform 0.28s cubic-bezier(0.34, 1.2, 0.64, 1);
  pointer-events: none;
}

.seg-btn {
  flex: 1;
  padding: 7px 0;
  background: none;
  border: none;
  border-radius: 7px;
  font-size: 14px;
  font-weight: 500;
  color: #6b7280;
  cursor: pointer;
  position: relative;
  z-index: 1;
  transition: color 0.2s ease;
  letter-spacing: 0.01em;
}

.seg-btn.active {
  color: #111827;
  font-weight: 600;
}

/* Tab 内容 Fade 切换 */
.tab-fade-enter-active,
.tab-fade-leave-active {
  transition: opacity 0.2s ease;
}
.tab-fade-enter-from,
.tab-fade-leave-to {
  opacity: 0;
}

.tab-panel {
  /* 占位，防止切换时高度跳动 */
}

/* 原 Tabs 样式（保留备用，已不再使用）*/

.premium-input {
  :deep(.ant-input), :deep(.ant-input-password) {
    height: 50px;
    border-radius: 40px;
    border-color: #e2e8f0;
    padding-left: 18px;
    transition: all 0.3s;
    background: #fff;

    &:hover { border-color: #94a3b8; }
    &:focus { border-color: #3b82f6; box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1); }
  }

  :deep(.ant-input-prefix) { color: #94a3b8; font-size: 18px; margin-right: 12px; }
}

/* 紧凑型验证码 - 核心修复 */
.compact-verify-container {
  display: flex;
  gap: 12px;
  align-items: center;
}

.digit-inputs {
  flex: 1;
  display: flex;
  gap: 8px;
}

.digit-inputs.six-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
}

.digit-input {
  flex: 1;
  width: 100%;
  height: 50px;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  background: #fff;
  text-align: center;
  font-size: 20px;
  font-weight: 600;
  color: #1e293b;
  outline: none;
  transition: all 0.3s;

  &:focus {
    border-color: #3b82f6;
    box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
  }
}

.code-captcha {
  width: 110px;
  height: 50px;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  img { width: 100%; height: 100%; object-fit: contain; }
}

/* 链接区域 */
.auth-links {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.link-item {
  color: #64748b;
  font-size: 14px;
  transition: color 0.2s;
  &:hover { color: #3b82f6; }
}

.divider-dot {
  width: 3px;
  height: 3px;
  background: #cbd5e1;
  border-radius: 50%;
}

/* 提交按钮 */
.auth-submit-btn {
  width: 100%;
  height: 52px;
  border-radius: 40px;
  background: linear-gradient(135deg, #1e293b 0%, #334155 100%);
  border: none;
  font-size: 18px;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(30, 41, 59, 0.15);
  transition: all 0.3s;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 6px 20px rgba(30, 41, 59, 0.25);
    filter: brightness(1.1);
  }
}

/* 微信面板 */
.wechat-auth-pane {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 10px 0;
}

.qr-mask {
  padding: 12px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 10px 25px rgba(0,0,0,0.05);
  margin-bottom: 20px;
}

.qr-frame { width: 150px; height: 150px; overflow: hidden; }
.qr-image { width: 100%; height: 100%; }

.qr-guide {
  font-size: 14px;
  color: #64748b;
  margin-bottom: 12px;
  .highlight { color: #10b981; font-weight: 700; margin: 0 4px; }
}

/* 微信动态验证码样式 */
.code-display {
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
  border-radius: 12px;
  padding: 12px 24px;
  margin: 10px auto;
  display: inline-block;
  cursor: pointer;
  transition: all 0.3s;
}

.code-display:hover {
  background: #f1f5f9;
  border-color: #94a3b8;
}

.code-text {
  font-size: 32px;
  font-weight: 700;
  letter-spacing: 4px;
  color: #64748b;
  transition: color 0.3s;
}

.code-text.highlight {
  color: #3b82f6;
}

.refresh-tip {
  font-size: 13px;
  color: #3b82f6;
  cursor: pointer;
  margin-top: 8px;
}
.refresh-tip:hover {
  text-decoration: underline;
}

/* 微信面板精简样式 */
.wechat-mini-guide {
  text-align: center;
  margin-bottom: 16px;
  width: 100%;
}

.combined-tip {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background: rgba(59, 130, 246, 0.04);
  padding: 6px 12px;
  border-radius: 8px;
  border: 1px solid rgba(59, 130, 246, 0.1);
}

.reg-badge {
  font-size: 11px;
  background: #10b981;
  color: #fff;
  padding: 1px 6px;
  border-radius: 4px;
  font-weight: 600;
  white-space: nowrap;
}

.safety-text {
  font-size: 12px;
  color: #64748b;
  line-height: 1.2;
}

@keyframes pulse {
  0% { opacity: 0.8; transform: scale(1); }
  50% { opacity: 1; transform: scale(1.02); }
  100% { opacity: 0.8; transform: scale(1); }
}

/* 响应式 */
@media screen and (max-width: 1024px) {
  .login-wrapper { width: 45vw; margin-right: 5%; }
}

@media screen and (max-width: 768px) {
  .content {
    width: 100vw;
    height: 100vh;
    border-radius: 0;
    justify-content: center;
  }

  .content::after {
    background: url('@/assets/images/login_bg_phone.png') no-repeat center;
    background-size: cover;
  }

  .login-wrapper {
    width: 90vw;
    margin: 0;
    background: rgba(255, 255, 255, 0.85);
    padding: 30px 20px;
  }

  .title { font-size: 32px; }
  .owl-container { display: none; }

  /* 移动端压缩间距 */
  .login-wrapper { padding: 25px 20px; padding-bottom: 0}
  .seg-control { margin-bottom: 20px; }
  .wechat-auth-pane { padding: 0; }
  .qr-mask { margin-bottom: 12px; }
  .qr-frame { width: 130px; height: 130px; }
  .qr-guide { margin-bottom: 8px; font-size: 13px; }
  .wechat-mini-guide { margin-bottom: 12px; }
  .combined-tip { padding: 4px 8px; }
  .safety-text { font-size: 11px; }
  .auth-submit-btn { height: 48px; font-size: 16px; }
}
</style>
