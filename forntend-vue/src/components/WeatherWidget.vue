<template>
  <div class="weather-widget" v-if="weatherStore.weatherData">
    <span class="location">{{ weatherStore.weatherData.province }}</span>
    <span class="weather-emoji">{{ getWeatherEmoji(weatherStore.weatherData.weather) }}</span>
    <span class="temperature">{{ weatherStore.weatherData.temperature }}°</span>
  </div>
</template>

<script setup lang="ts">
import { onMounted, watch } from 'vue';
import { useWeatherStore } from '../stores/useWeatherStore';

const weatherStore = useWeatherStore();

// 天气类型到emoji的映射
const weatherEmojis: Record<string, string> = {
  '晴': '☀️',
  '多云': '⛅',
  '阴': '☁️',
  '小雨': '🌧️',
  '中雨': '🌧️',
  '大雨': '🌧️',
  '暴雨': '⛈️',
  '雷阵雨': '⛈️',
  '小雪': '🌨️',
  '中雪': '🌨️',
  '大雪': '🌨️',
  '雾': '🌫️',
  '霾': '🌫️',
  '扬沙': '🌪️',
  '沙尘暴': '🌪️'
};

// 获取天气对应的emoji
const getWeatherEmoji = (weather: string) => {
  return weatherEmojis[weather] || '🌞';
};

// 监听天气数据变化
watch(() => weatherStore.weatherData, (newData) => {
  if (newData) {
    console.log('天气数据更新:', newData);
  }
}, { immediate: true });

onMounted(() => {
  console.log('组件挂载，当前天气数据:', weatherStore.weatherData);
  if (!weatherStore.weatherData) {
    console.log('开始获取天气数据');
    weatherStore.fetchWeather();
  }
});
</script>

<style scoped>
.weather-widget {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  color: var(--text-primary);
  background: transparent;
  padding: 4px 8px;
  border-radius: 8px;
  white-space: nowrap;
}

.weather-emoji {
  font-size: 18px;
  line-height: 1;
}

.temperature {
  line-height: 1;
  font-weight: 500;
}

@media (max-width: 768px) {
  .weather-widget {
    font-size: 13px;
  }

  .weather-emoji {
    font-size: 16px;
  }
}
</style>

