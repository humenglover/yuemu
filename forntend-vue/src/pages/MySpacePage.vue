<template>
  <div id="mySpacePage">
    <div class="loading-container">
      <div class="loading-content">
        <a-spin :indicator="indicator" class="custom-spin" />
        <div class="loading-text">
          <h2>正在为您跳转</h2>
          <p>请稍候片刻...</p>
        </div>
      </div>
      <div class="loading-tips">
        <InfoCircleOutlined class="tips-icon" />
        <span>首次访问需要创建个人空间</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import { listSpaceVoByPageUsingPost } from '@/api/spaceController.ts'
import { message } from 'ant-design-vue'
import { h, onMounted } from 'vue'
import { LoadingOutlined, InfoCircleOutlined } from '@ant-design/icons-vue'
import { SPACE_TYPE_ENUM } from '@/constants/space.ts'

const router = useRouter()
const loginUserStore = useLoginUserStore()

// 自定义加载图标
const indicator = h(LoadingOutlined, {
  style: {
    fontSize: '32px',
    color: '#ff8e53',
  },
})

// 检查用户是否有个人空间
const checkUserSpace = async () => {
  const loginUser = loginUserStore.loginUser
  if (!loginUser?.id) {
    router.replace('/user/login')
    return
  }

  try {
    const res = await listSpaceVoByPageUsingPost({
      userId: loginUser.id,
      current: 1,
      pageSize: 1,
      spaceType: SPACE_TYPE_ENUM.PRIVATE,
    })

    if (res.data.code === 0) {
      if (res.data.data?.records?.length > 0) {
        const space = res.data.data.records[0]
        router.replace(`/space/${space.id}`)
      } else {
        router.replace('/add_space')
        message.info('即将为您创建个人空间')
      }
    } else {
      message.error('加载我的空间失败，' + res.data.message)
    }
  } catch (error) {
    message.error('网络请求失败，请稍后重试')
  }
}

onMounted(() => {
  checkUserSpace()
})
</script>

<style scoped>
#mySpacePage {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: calc(100vh - 200px);
  background: var(--header-background);
  color: var(--text-primary);
}

.loading-container {
  text-align: center;
  padding: 40px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
}

.loading-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24px;
}

.loading-text {
  h2 {
    color: #1a1a1a;
    font-size: 20px;
    margin: 0 0 8px;
  }

  p {
    color: #64748b;
    font-size: 14px;
    margin: 0;
  }
}

.loading-tips {
  margin-top: 32px;
  padding-top: 16px;
  border-top: 1px dashed #e2e8f0;
  color: #94a3b8;
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.tips-icon {
  color: #ff8e53;
}

/* 响应式调整 */
@media screen and (max-width: 768px) {
  .loading-container {
    padding: 32px 24px;
    margin: 0 16px;
  }

  .loading-text {
    h2 {
      font-size: 18px;
    }
  }
}
</style>
