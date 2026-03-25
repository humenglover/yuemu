<template>
  <div class="global-loading" v-if="show">
    <p class="loading-title">资源加载中...</p>
    <div class="loader"></div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  show: boolean
}>()
</script>

<style scoped>
.global-loading {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: transparent;
  z-index: 9999;
  min-height: 100vh;
  box-sizing: border-box;
  animation: fadeIn 0.15s ease-out forwards;
}

.loading-title {
  font-size: 20px;
  color: #bfdbfe;
  margin-bottom: 20px;
  font-weight: 600;
  letter-spacing: 0.5px;
  opacity: 0;
  animation: titleFade 0.15s ease-out 0.05s forwards;
}

.loader {
  width: 64px;
  height: 64px;
  position: relative;
  background: #eff6ff;
  border-radius: 4px;
  overflow: hidden;
  opacity: 0;
  transform: scale(0.9);
  animation: loaderShow 0.15s ease-out 0.05s forwards;
  box-shadow: 0 4px 16px rgba(37, 99, 235, 0.2);
}

.loader:before {
  content: "";
  position: absolute;
  left: 0;
  bottom: 0;
  width: 40px;
  height: 40px;
  transform: rotate(45deg) translate(30%, 40%);
  background: #93c5fd;
  box-shadow: 32px -34px 0 5px #60a5fa;
  animation: slide 2s infinite ease-in-out alternate;
}

.loader:after {
  content: "";
  position: absolute;
  left: 10px;
  top: 10px;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: #3b82f6;
  transform: rotate(0deg);
  transform-origin: 35px 145px;
  animation: rotate 2s infinite ease-in-out;
}

@keyframes slide {
  0%, 100% { bottom: -35px }
  25%, 75% { bottom: -2px }
  20%, 80% { bottom: 2px }
}

@keyframes rotate {
  0% { transform: rotate(-15deg) }
  25%, 75% { transform: rotate(0deg) }
  100% { transform: rotate(25deg) }
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes titleFade {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes loaderShow {
  from { opacity: 0; transform: scale(0.9); }
  to { opacity: 1; transform: scale(1); }
}

@media (max-width: 768px) {
  .loading-title {
    font-size: 18px;
    margin-bottom: 16px;
  }

  .loader {
    width: 56px;
    height: 56px;
    box-shadow: 0 3px 12px rgba(37, 99, 235, 0.2);
  }
}

@media (prefers-reduced-motion: reduce) {
  .global-loading, .loading-title, .loader {
    animation: none;
    opacity: 1;
    transform: none;
  }
}
</style>
