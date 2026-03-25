<template>
  <div></div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getDeviceType } from '@/utils/device.ts'
import { DEVICE_TYPE_ENUM } from '@/constants/device.ts'

const router = useRouter()
const route = useRoute()

onMounted(async () => {
  const deviceType = await getDeviceType()
  const pictureId = route.params.id as string

  // 获取传递的图片数据（通过history state传递）
  const pictureData = history.state?.state?.pictureData

  if (deviceType === DEVICE_TYPE_ENUM.MOBILE) {
    // 如果是移动端，跳转到移动端图片详情页，并传递图片数据
    await router.replace({
      path: `/mobile/picture/${pictureId}`,
      state: { pictureData }
    })
  } else {
    // 如果是PC端，跳转到PC端图片详情页，并传递图片数据
    await router.replace({
      path: `/picture/${pictureId}`,
      state: { pictureData }
    })
  }
})
</script>
