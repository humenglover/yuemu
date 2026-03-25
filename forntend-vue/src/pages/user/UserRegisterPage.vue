<template>
  <div class="box" :class="{ 'is-loaded': bgLoaded }">
    <div class="content">
      <div class="login-wrapper">
        <!-- 核心头部 -->
        <div class="login-header">
          <h1 class="title">加入悦木</h1>
        </div>

        <a-form
          :model="formState"
          class="auth-form"
          @finish="handleSubmit"
        >
          <!-- 邮箱输入 -->
          <a-form-item
            name="email"
            :rules="[
              { required: true, message: '请输入邮箱' },
              { type: 'email', message: '请输入有效的邮箱地址' },
            ]"
          >
            <a-input
              v-model:value="formState.email"
              placeholder="注册邮箱"
              size="large"
              :prefix="h(MailOutlined)"
              class="premium-input"
            />
          </a-form-item>

          <!-- 验证码输入 -->
          <a-form-item
            name="code"
            :rules="[{ required: true, message: '请输入邮箱验证码' }]"
          >
            <div class="verify-action-group">
              <a-input
                v-model:value="formState.code"
                placeholder="验证码"
                size="large"
                :prefix="h(SafetyCertificateOutlined)"
                class="premium-input verify-field"
              />
              <a-button
                class="get-code-btn"
                :disabled="!!countdown || !formState.email"
                @click="sendEmailCode"
              >
                {{ countdown ? `${countdown}s` : '获取验证码' }}
              </a-button>
            </div>
          </a-form-item>

          <!-- 密码输入 -->
          <a-form-item
            name="userPassword"
            :rules="[
              { required: true, message: '请输入密码' },
              { min: 8, message: '密码长度不能小于 8 位' },
            ]"
          >
            <a-input-password
              v-model:value="formState.userPassword"
              placeholder="设置密码 (8位以上)"
              size="large"
              :prefix="h(LockOutlined)"
              class="premium-input"
            />
          </a-form-item>

          <!-- 确认密码 -->
          <a-form-item
            name="checkPassword"
            :rules="[{ required: true, message: '请确认密码' }, { validator: validatePassword }]"
          >
            <a-input-password
              v-model:value="formState.checkPassword"
              placeholder="确认密码"
              size="large"
              :prefix="h(CheckOutlined)"
              class="premium-input"
            />
          </a-form-item>

          <div class="auth-helper-link">
            已有账号？
            <RouterLink to="/user/login" class="jump-link">立即登录</RouterLink>
            <span class="divider-dot"></span>
            <RouterLink to="/user/login?type=wechat" class="jump-link special">微信快速注册</RouterLink>
          </div>

          <!-- 按钮 -->
          <a-form-item>
            <a-button type="primary" html-type="submit" class="auth-submit-btn" size="large">
              创建账号
            </a-button>
          </a-form-item>
        </a-form>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { reactive, ref, h, onBeforeUnmount, onMounted } from 'vue'
import { userRegisterUsingPost, getEmailCodeUsingPost } from '@/api/userController.ts'
import { message } from 'ant-design-vue'
import router from '@/router'
import { MailOutlined, LockOutlined, CheckOutlined, SafetyCertificateOutlined } from '@ant-design/icons-vue'
import bgPc from '@/assets/images/login_two.jpg'
import bgMobile from '@/assets/images/login_bg_phone.png'

const bgLoaded = ref(false)

onMounted(() => {
  const isMobile = window.innerWidth <= 768
  const imgUrl = isMobile ? bgMobile : bgPc
  const img = new Image()
  img.src = imgUrl
  img.onload = () => { bgLoaded.value = true }
  img.onerror = () => { bgLoaded.value = true }
})

// 表单数据
const formState = reactive<API.UserRegisterRequest>({
  email: '',
  userPassword: '',
  checkPassword: '',
  code: '',
})

// 倒计时
const countdown = ref<number>(0)
let timer: NodeJS.Timeout | null = null

// 发送邮箱验证码
const sendEmailCode = async () => {
  try {
    await getEmailCodeUsingPost({
      email: formState.email,
      type: 'register'
    })
    message.success('验证码已发送')
    countdown.value = 60
    timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer!)
        timer = null
      }
    }, 1000)
  } catch (error: any) {
    message.error('验证码发送失败：' + error.message)
  }
}

// 验证两次密码是否一致
const validatePassword = async (_rule: any, value: string) => {
  if (value !== formState.userPassword) {
    return Promise.reject('两次输入的密码不一致')
  }
  return Promise.resolve()
}

// 提交表单
const handleSubmit = async (values: any) => {
  const res = await userRegisterUsingPost(values)
  if (res.data.code === 0 && res.data.data) {
    message.success('注册成功')
    await router.push('/user/login')
  } else {
    message.error('注册失败，' + res.data.message)
  }
}

// 组件卸载时清除定时器
onBeforeUnmount(() => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
})
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
  background-size: 70% 100%;
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
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.auth-form :deep(.ant-form-item) {
  margin-bottom: 24px;
}

/* 统一输入框方案 */
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

/* 验证码特别组 */
.verify-action-group {
  display: flex;
  gap: 12px;
  align-items: center;
}

.verify-field { flex: 1; }

.get-code-btn {
  min-width: 110px;
  height: 48px;
  border-radius: 40px;
  background: #1e293b;
  border: none;
  color: white;
  font-size: 14px;
  font-weight: 600;
  transition: all 0.3s;

  &:hover:not(:disabled) {
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(0,0,0,0.15);
    background: #334155;
  }

  &:disabled {
    background: #e2e8f0;
    color: #94a3b8;
    cursor: not-allowed;
  }
}

/* 辅助跳转 */
.auth-helper-link {
  text-align: center;
  margin-bottom: 24px;
  color: #64748b;
  font-size: 14px;
}

.jump-link {
  color: #3b82f6;
  font-weight: 600;
  margin-left: 4px;
  transition: all 0.2s;
  &:hover { text-decoration: underline; color: #2563eb; }

  &.special {
    color: #10b981;
    &:hover { color: #059669; }
  }
}

.divider-dot {
  display: inline-block;
  width: 3px;
  height: 3px;
  background: #cbd5e1;
  border-radius: 50%;
  margin: 0 8px;
  vertical-align: middle;
}

/* 核心提交按钮 */
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
}
</style>
