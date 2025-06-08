<template>
  <div class="global-loading" v-if="show">
    <div class="loading-content">
      <div class="loading-balls">
        <div class="ball"></div>
        <div class="ball"></div>
        <div class="ball"></div>
        <div class="ball"></div>
        <div class="ball"></div>
      </div>
      <div class="loading-text">
        <h3>加载中</h3>
        <p>请稍候片刻...</p>
      </div>
    </div>
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
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(8px);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: fadeIn 0.3s ease;
}

.loading-content {
  text-align: center;
  padding: 32px 48px;
  border-radius: 20px;
  background: white;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
  border: 1px solid rgba(0, 0, 0, 0.04);
  transform: translateY(0);
  animation: float 6s ease-in-out infinite;
}

.loading-balls {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  height: 40px;
  margin-bottom: 24px;
}

.ball {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  animation: bounce 0.5s alternate infinite;
}

.ball:nth-child(1) {
  background: linear-gradient(120deg, #FF6B6B, #FF8E53);
  animation-delay: 0.1s;
  box-shadow: 0 4px 12px rgba(255, 107, 107, 0.3);
}

.ball:nth-child(2) {
  background: linear-gradient(120deg, #4ECDC4, #45B7D1);
  animation-delay: 0.2s;
  box-shadow: 0 4px 12px rgba(78, 205, 196, 0.3);
}

.ball:nth-child(3) {
  background: linear-gradient(120deg, #96C93D, #7AB929);
  animation-delay: 0.3s;
  box-shadow: 0 4px 12px rgba(150, 201, 61, 0.3);
}

.ball:nth-child(4) {
  background: linear-gradient(120deg, #45B7D1, #4ECDC4);
  animation-delay: 0.4s;
  box-shadow: 0 4px 12px rgba(69, 183, 209, 0.3);
}

.ball:nth-child(5) {
  background: linear-gradient(120deg, #FF8E53, #FF6B6B);
  animation-delay: 0.5s;
  box-shadow: 0 4px 12px rgba(255, 142, 83, 0.3);
}

.loading-text {
  text-align: center;
}

.loading-text h3 {
  color: #1a1a1a;
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 8px;
  background: linear-gradient(120deg, #1a1a1a, #4a4a4a);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.loading-text p {
  color: #94a3b8;
  font-size: 14px;
  margin: 0;
  font-weight: 500;
}

@keyframes bounce {
  from {
    transform: translateY(0) scale(1);
  }
  to {
    transform: translateY(-20px) scale(1.1);
  }
}

@keyframes float {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-8px);
  }
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

@media (max-width: 768px) {
  .loading-content {
    padding: 24px 32px;
  }

  .loading-balls {
    gap: 8px;
  }

  .ball {
    width: 10px;
    height: 10px;
  }

  .loading-text h3 {
    font-size: 16px;
  }

  .loading-text p {
    font-size: 13px;
  }
}
</style>
