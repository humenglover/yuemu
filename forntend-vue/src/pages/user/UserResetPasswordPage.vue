<template>
  <div class="box">
    <div class="content">
      <div class="login-wrapper">
        <div class="login-header">
          <h2 class="title">重置密码</h2>
          <p class="subtitle">请输入您的注册邮箱</p>
        </div>

        <a-form
          :model="formState"
          name="basic"
          autocomplete="off"
          @finish="handleSubmit"
          class="login-form"
        >
          <!-- 邮箱输入 -->
          <a-form-item
            name="email"
            :rules="[
              { required: true, message: '请输入邮箱' },
              { type: 'email', message: '请输入正确的邮箱格式' }
            ]"
          >
            <a-input
              v-model:value="formState.email"
              placeholder="请输入邮箱"
              size="large"
              :prefix="h(MailOutlined)"
              class="custom-input"
            />
          </a-form-item>

          <!-- 邮箱验证码 -->
          <a-form-item
            name="code"
            :rules="[
              { required: true, message: '请输入验证码' },
              { len: 6, message: '验证码长度为 6 位' },
              { pattern: /^\d+$/, message: '验证码必须是数字' }
            ]"
          >
            <div class="verify-code-container">
              <a-input
                v-model:value="formState.code"
                placeholder="请输入邮箱验证码"
                size="large"
                :prefix="h(SafetyCertificateOutlined)"
                class="custom-input verify-input"
                maxlength="6"
              />
              <a-button
                class="send-code-btn"
                :disabled="!!countdown || !formState.email"
                @click="sendEmailCode"
                size="large"
              >
                {{ countdown ? `${countdown}s后重试` : '获取验证码' }}
              </a-button>
            </div>
          </a-form-item>

          <!-- 新密码输入 -->
          <a-form-item
            name="newPassword"
            :rules="[
              { required: true, message: '请输入新密码' },
              { min: 8, message: '密码长度不能小于 8 位' },
            ]"
          >
            <a-input-password
              v-model:value="formState.newPassword"
              placeholder="请输入新密码"
              size="large"
              :prefix="h(LockOutlined)"
              class="custom-input"
            />
          </a-form-item>

          <!-- 确认密码 -->
          <a-form-item
            name="checkPassword"
            :rules="[{ required: true, message: '请确认密码' }, { validator: validatePassword }]"
          >
            <a-input-password
              v-model:value="formState.checkPassword"
              placeholder="请确认密码"
              size="large"
              :prefix="h(CheckOutlined)"
              class="custom-input"
            />
          </a-form-item>

          <!-- 返回登录 -->
          <div class="register-link">
            记起密码了？
            <RouterLink to="/user/login" class="link-text">返回登录</RouterLink>
          </div>

          <!-- 提交按钮 -->
          <a-form-item>
            <a-button type="primary" html-type="submit" class="submit-button" size="large">
              重置密码
            </a-button>
          </a-form-item>
        </a-form>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { reactive, ref, h, onBeforeUnmount } from 'vue'
import { resetPasswordUsingPost, getEmailCodeUsingPost } from '@/api/userController'
import { message } from 'ant-design-vue'
import router from '@/router'
import {
  MailOutlined,
  LockOutlined,
  CheckOutlined,
  SafetyCertificateOutlined
} from '@ant-design/icons-vue'

// 表单数据
const formState = reactive<API.UserResetPasswordRequest>({
  email: '',
  newPassword: '',
  checkPassword: '',
  code: '',
})

// 倒计时
const countdown = ref<number>(0)
let timer: NodeJS.Timeout | null = null

// 发送邮箱验证码
const sendEmailCode = async () => {
  try {
    const res = await getEmailCodeUsingPost({
      email: formState.email,
      type: 'resetPassword'
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
    if (error.response?.data) {
      message.error(error.response.data.message || '发送失败')
    } else {
      message.error('网络错误，请稍后重试')
    }
  }
}

// 验证两次密码是否一致
const validatePassword = async (_rule: any, value: string) => {
  if (value !== formState.newPassword) {
    return Promise.reject('两次输入的密码不一致')
  }
  return Promise.resolve()
}

// 提交表单
const handleSubmit = async (values: any) => {
  try {
    const res = await resetPasswordUsingPost(values)
    if (res.data.code === 0) {
      message.success('密码重置成功')
      await router.push('/user/login')
    } else {
      message.error(res.data.message || '重置失败')
    }
  } catch (error: any) {
    if (error.response?.data) {
      message.error(error.response.data.message || '重置失败')
    } else {
      message.error('网络错误，请稍后重试')
    }
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
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #fff;
  z-index: 100;
}

.content {
  width: 85vw;
  max-width: 1400px;
  min-height: 80vh;
  background: url("@/assets/images/login_two.jpg") no-repeat;
  background-size: 70% 100%;
  background-position: left;
  border-radius: 20px;
  background-color: #fff;
  position: relative;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
}

.login-wrapper {
  width: 30vw;
  margin-right: 10%;
  position: relative;
}

.login-header {
  text-align: center;
  margin-bottom: 40px;
}

.title {
  font-size: 45px;
  font-weight: 600;
  color: rgb(81, 100, 115);
  margin-bottom: 8px;
}

.subtitle {
  font-size: 16px;
  color: #64748b;
}

.login-form {
  :deep(.ant-form-item) {
    margin-bottom: 24px;
  }
}


.verify-code-container {
  display: flex;
  gap: 12px;
  align-items: center;
}

.verify-input {
  flex: 1;
}

.send-code-btn {
  min-width: 120px;
  border-radius: 40px;
  background-color: rgb(59, 72, 89);
  border: none;
  color: white;
  font-size: 14px;
  font-weight: 100;
  transition: all 0.3s ease;
}

.send-code-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(59, 72, 89, 0.2);
}

.send-code-btn:active:not(:disabled) {
  transform: translateY(1px);
}

.send-code-btn:disabled {
  background: #e2e8f0;
  color: #94a3b8;
  cursor: not-allowed;
}

.register-link {
  text-align: center;
  margin-bottom: 24px;
  color: #64748b;
  font-size: 14px;
}

.register-link .link-text {
  color: #93c5fd;
  font-weight: 500;
  margin-left: 4px;
  transition: color 0.3s ease;
}

.register-link .link-text:hover {
  color: #93c5fd;
}

.submit-button {
  width: 100%;
  height: 50px;
  border-radius: 40px;
  background-color: rgb(59, 72, 89);
  border: none;
  font-size: 20px;
  font-weight: 100;
  box-shadow: 0 4px 12px rgba(59, 72, 89, 0.2);
  transition: all 0.3s ease;
}

.submit-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(59, 72, 89, 0.3);
}

.submit-button:active {
  transform: translateY(1px);
}

@media screen and (max-width: 768px) {
  .box {
    margin: 0;
    height: 100vh;
  }

  .content {
    width: 100vw;
    height: 100vh;
    background: url('@/assets/images/login_bg_phone.png') no-repeat;
    background-size: cover;
    background-position: center;
    border-radius: 0;
    display: flex;
    justify-content: center;
    align-items: center;
  }

  .login-wrapper {
    width: 94vw;
    margin: 0 auto;
    background-color: rgba(255, 255, 255, 0.9);
    padding: 20px;
    border-radius: 20px;
  }

  .title {
    font-size: 32px;
    color: #333;
  }


  .submit-button {
    height: 48px;
    font-size: 16px;
  }
}
</style>
