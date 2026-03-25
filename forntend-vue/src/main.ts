import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { handleBackButton } from '@/utils/back.ts'
import App from './App.vue'
import router from './router'
import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
import '@/access.ts'
import VueCropper from 'vue-cropper'
import 'vue-cropper/dist/index.css'
import "vue3-emoji-picker/css";
import { addUserSignInUsingPost } from '@/api/userController'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
// 导入全局样式
import '@/styles/theme.css'
import 'vue-virtual-scroller/dist/vue-virtual-scroller.css'
// 1. 先创建 app 实例
const app = createApp(App)
const pinia = createPinia()

// 2. 先 use 所有插件（包括 router），确保路由初始化完成
app.use(pinia)
app.use(router) // 关键：必须在 router.isReady() 之前执行
app.use(Antd)
app.use(VueCropper)

// 初始化安全防护
// createSecurityShield()

// 自动签到函数
const autoSignIn = async () => {
  const loginUserStore = useLoginUserStore()
  // 确保用户已登录
  if (loginUserStore.loginUser?.id) {
    try {
      await addUserSignInUsingPost()
    } catch (error) {
      console.error('自动签到失败:', error)
    }
  }
}


// 3. 等路由准备就绪后，再执行依赖路由的操作
router.isReady().then(() => {
  autoSignIn()


})


// 挂载应用
app.mount('#app')
// 使用nextTick确保在DOM更新后（也就是应用挂载完成后）执行后续逻辑
import { nextTick } from 'vue'
nextTick(() => {
  handleBackButton()
    .then(() => {
      // 这里可以添加一些后续逻辑，比如确认返回键处理逻辑添加成功后的提示等，目前为空
    })
    .catch((error) => {
      console.error('处理返回键时出现错误:', error)
    })
})

