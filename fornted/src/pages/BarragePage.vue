<template>
  <div class="barrage-page">
    <!-- 背景图片 -->
    <div
      class="background-image"
      :style="{
        backgroundImage: `url(${randomBackgroundImage})`,
        backgroundSize: 'cover',
        backgroundPosition: 'center',
        backgroundRepeat: 'no-repeat'
      }"
    ></div>

    <!-- 弹幕区域 -->
    <div class="barrage-wrapper">
      <Barrage
        ref="barrageRef"
        :speed="barrageSpeed"
        :showInput="showInput"
      />
    </div>

    <!-- 控制面板 -->
    <div class="control-panel" :class="{ 'show': showControls }">
      <div class="control-header">
        <h3>弹幕控制台</h3>
        <a-switch
          v-model:checked="showInput"
          checked-children="显示输入"
          un-checked-children="隐藏输入"
        />
      </div>
      <div class="control-content">
        <div class="control-item">
          <span class="label">弹幕速度</span>
          <a-slider
            v-model:value="barrageSpeed"
            :min="8000"
            :max="15000"
            :step="1000"
            :tipFormatter="(value) => `${value/1000}秒`"
          />
        </div>
      </div>
    </div>

    <!-- 控制按钮 -->
    <div class="control-toggle" @click="showControls = !showControls">
      <SettingOutlined :class="{ 'rotate': showControls }" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { SettingOutlined } from '@ant-design/icons-vue'
import Barrage from '@/components/Barrage.vue'
import { BARRAGE_BACKGROUND_URLS, MOBILE_BARRAGE_BACKGROUND_URLS } from '@/constants/background'

function isMobile() {
  return window.innerWidth <= 768
}

const randomBackgroundImage = ref<string>(
  isMobile()
    ? MOBILE_BARRAGE_BACKGROUND_URLS[Math.floor(Math.random() * MOBILE_BARRAGE_BACKGROUND_URLS.length)]
    : BARRAGE_BACKGROUND_URLS[Math.floor(Math.random() * BARRAGE_BACKGROUND_URLS.length)]
)

onMounted(() => {
  window.addEventListener('resize', () => {
    randomBackgroundImage.value = isMobile()
      ? MOBILE_BARRAGE_BACKGROUND_URLS[Math.floor(Math.random() * MOBILE_BARRAGE_BACKGROUND_URLS.length)]
      : BARRAGE_BACKGROUND_URLS[Math.floor(Math.random() * BARRAGE_BACKGROUND_URLS.length)]
  })
})

// 状态
const showControls = ref(false)
const showInput = ref(true)
const barrageSpeed = ref(12000)
const barrageRef = ref()

// 监听设置变化
watch([barrageSpeed, showInput], ([newSpeed, newShowInput]) => {
  if (barrageRef.value) {
    // 更新弹幕速度
    barrageRef.value.updateSpeed(newSpeed)
    // 更新输入框显示状态
    barrageRef.value.updateShowInput(newShowInput)
  }
}, { immediate: true })
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Noto+Sans+SC:wght@300;400;500;700&display=swap');


.barrage-page {
  margin: -28px!important;
  margin-top: -88px!important;
  margin-bottom: -44px!important;
  position: relative;
  width: 100vw;
  height: 99vh;
  overflow: hidden;
  animation: hideToShow 2s;
}

.background-image {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  filter: brightness(0.8);
  z-index: 0;
}

.barrage-wrapper {
  position: relative;
  width: 100%;
  height: 100%;
  z-index: 1;
}

.control-panel {
  position: fixed;
  top: 50%;
  right: 0;
  transform: translate(100%, -50%);
  width: 300px;
  background: var(--gradientBG);
  background-size: 300% 300%;
  animation: gradientBG 10s ease infinite;
  backdrop-filter: blur(12px);
  border-radius: 16px 0 0 16px;
  padding: 24px;
  transition: transform 0.3s ease;
  z-index: 2;
  box-shadow: -4px 0 16px rgba(0, 0, 0, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.control-panel.show {
  transform: translate(0, -50%);
}

.control-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  color: var(--white);
}

.control-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 500;
  user-select: none;
  letter-spacing: 1px;
}

.control-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.control-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}

.control-item:last-child {
  margin-bottom: 0;
}

.control-item .label {
  font-size: 14px;
  color: var(--white);
  user-select: none;
  opacity: 0.9;
}

.control-toggle {
  position: fixed;
  top: 50%;
  right: 0;
  transform: translateY(-50%);
  width: 44px;
  height: 44px;
  background: var(--gradientBG);
  background-size: 300% 300%;
  animation: gradientBG 10s ease infinite;
  border-radius: 8px 0 0 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 3;
  transition: all 0.3s ease;
  box-shadow: -2px 0 12px rgba(0, 0, 0, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.control-toggle:hover {
  width: 48px;
  background-position: 100% 50%;
}

.control-toggle .anticon {
  color: var(--white);
  font-size: 20px;
  transition: transform 0.3s ease;
}

.control-toggle .anticon.rotate {
  transform: rotate(180deg);
}

@keyframes hideToShow {
  0% {
    opacity: 0;
  }
  100% {
    opacity: 1;
  }
}

@keyframes gradientBG {
  0% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0% 50%;
  }
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .barrage-page {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    width: 100vw;
    height: 100vh;
    margin: 0!important;
    padding: 0;
    z-index: 1002;
    background: var(--background);
  }

  .background-image {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    width: 100%;
    height: 100%;
    filter: brightness(0.8);
    z-index: 1002;
  }

  .barrage-wrapper {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    width: 100%;
    height: 100%;
    z-index: 1003;
  }

  .control-panel {
    position: fixed;
    top: auto;
    bottom: 0;
    left: 0;
    right: 0;
    transform: translateY(100%);
    width: 100%;
    border-radius: 20px 20px 0 0;
    padding: 16px;
    background: var(--gradientBG);
    background-size: 300% 300%;
    animation: gradientBG 10s ease infinite;
    backdrop-filter: blur(20px);
    transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    margin: 0;
    z-index: 10001;
  }

  .control-panel.show {
    transform: translateY(0);
  }

  .control-toggle {
    position: fixed;
    top: auto;
    bottom: 250px;
    right: 20px;
    transform: none;
    width: 48px;
    height: 48px;
    border-radius: 24px;
    background: var(--gradientBG);
    background-size: 300% 300%;
    animation: gradientBG 10s ease infinite;
    backdrop-filter: blur(10px);
    border: 1px solid rgba(255, 255, 255, 0.2);
    z-index: 100001;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    transition: all 0.3s ease;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.25);
  }

  .control-toggle:hover {
    width: 48px;
    transform: scale(1.05);
    background-position: 100% 50%;
  }

  .control-toggle .anticon {
    font-size: 24px;
    color: var(--white);
    transition: transform 0.3s ease;
  }

  .control-toggle .anticon.rotate {
    transform: rotate(180deg);
  }

  .control-header {
    margin-bottom: 20px;
    padding: 0 4px;
  }

  .control-header h3 {
    font-size: 18px;
    font-weight: 600;
  }

  .control-content {
    gap: 24px;
    padding: 0 4px;
  }

  .control-item {
    gap: 12px;
    margin-bottom: 0;
  }

  .control-item .label {
    font-size: 15px;
    font-weight: 500;
  }

  /* 优化滑块在移动端的触摸区域 */
  :deep(.ant-slider) {
    margin: 8px 0;
  }

  :deep(.ant-slider-handle) {
    width: 24px;
    height: 24px;
    margin-top: -10px;
  }

  :deep(.ant-slider-rail),
  :deep(.ant-slider-track) {
    height: 6px;
  }

  /* 优化开关在移动端的大小 */
  :deep(.ant-switch) {
    min-width: 44px;
    height: 24px;
  }

  :deep(.ant-switch-handle) {
    width: 20px;
    height: 20px;
  }
}
</style>
