<template>
  <div class="redirect-container">
    <div class="loading-spinner">
      <a-spin size="large" />
    </div>
    <p class="loading-text">正在为您匹配最佳聊天界面...</p>
  </div>
</template>oo

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue' // 引入 message 用于错误提示
import { getDeviceType } from '@/utils/device'
import { DEVICE_TYPE_ENUM } from '@/constants/device'

const router = useRouter()
const route = useRoute()

onMounted(async () => {
  try {
    // 尝试获取设备类型
    const device = await getDeviceType()

    // 根据设备类型进行重定向
    if (device === DEVICE_TYPE_ENUM.PC) {
      console.log('检测到PC设备，跳转到PC聊天界面...')
      await router.replace('/pc-chat')
    } else {
      console.log('检测到移动设备，跳转到移动聊天列表...')
      await router.replace({
        path: `/chat-list`,
        query: route.query // 保留原始查询参数
      })
    }
  } catch (error) {
    // 如果发生错误（例如 getDeviceType 失败）
    console.error('设备检测或路由跳转失败:', error)
    message.error('页面跳转失败，请刷新重试或检查网络连接。')

    // 提供一个备用方案，例如跳转到移动版或首页
    setTimeout(() => {
      router.replace('/chat-list')
    }, 3000)
  }
})
</script>

<style scoped>
.redirect-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.loading-spinner {

  width: 40px;
  height: 40px;
}

.loading-text {
  color: #666;
  font-size: 14px;
  margin-top: 20px;
  text-align: center;
}
</style>
