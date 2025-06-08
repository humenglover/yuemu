<template>
  <div class="color-picker-page">
    <div class="color-picker-container">
      <!-- 颜色预览区域 -->
      <div class="preview-section">
        <div
          class="color-preview"
          :style="{
            backgroundColor: `rgb(${rgb.r}, ${rgb.g}, ${rgb.b})`,
            color: isDarkColor ? '#fff' : '#000'
          }"
        >
          <div class="color-values">
            <div class="value-item" @click="copyToClipboard(`rgb(${rgb.r}, ${rgb.g}, ${rgb.b})`)">
              RGB: {{rgb.r}}, {{rgb.g}}, {{rgb.b}}
            </div>
            <div class="value-item" @click="copyToClipboard(hexColor)">
              HEX: {{hexColor}}
            </div>
          </div>
        </div>
      </div>

      <!-- 控制面板 -->
      <div class="controls-section">
        <div class="control-group">
          <h3>RGB 调节</h3>
          <div class="slider-group">
            <div class="slider-item">
              <span class="label">R</span>
              <input
                type="range"
                v-model="rgb.r"
                min="0"
                max="255"
                class="color-slider red-slider"
              />
              <input
                type="number"
                v-model="rgb.r"
                min="0"
                max="255"
                class="number-input"
              />
            </div>
            <div class="slider-item">
              <span class="label">G</span>
              <input
                type="range"
                v-model="rgb.g"
                min="0"
                max="255"
                class="color-slider green-slider"
              />
              <input
                type="number"
                v-model="rgb.g"
                min="0"
                max="255"
                class="number-input"
              />
            </div>
            <div class="slider-item">
              <span class="label">B</span>
              <input
                type="range"
                v-model="rgb.b"
                min="0"
                max="255"
                class="color-slider blue-slider"
              />
              <input
                type="number"
                v-model="rgb.b"
                min="0"
                max="255"
                class="number-input"
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { message } from 'ant-design-vue'

// RGB 颜色状态
const rgb = ref({
  r: 52,
  g: 152,
  b: 219
})

// 计算 HEX 颜色值
const hexColor = computed(() => {
  const r = rgb.value.r.toString(16).padStart(2, '0')
  const g = rgb.value.g.toString(16).padStart(2, '0')
  const b = rgb.value.b.toString(16).padStart(2, '0')
  return `#${r}${g}${b}`
})

// 判断颜色是否为深色
const isDarkColor = computed(() => {
  const brightness = (rgb.value.r * 299 + rgb.value.g * 587 + rgb.value.b * 114) / 1000
  return brightness < 128
})

// 复制颜色值到剪贴板
const copyToClipboard = (text: string) => {
  navigator.clipboard.writeText(text).then(() => {
    message.success('颜色值已复制到剪贴板')
  }).catch(() => {
    message.error('复制失败')
  })
}
</script>

<style scoped>
.color-picker-page {
  height: 89vh;
  margin: -20px;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.color-picker-container {
  width: 100%;
  max-width: 800px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 24px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  padding: 32px;
  display: grid;
  gap: 32px;
  grid-template-columns: 1fr 1fr;
}

.preview-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.color-preview {
  aspect-ratio: 1;
  border-radius: 20px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.color-values {
  text-align: center;
  font-size: 16px;
  font-weight: 500;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.color-preview:hover .color-values {
  opacity: 1;
}

.value-item {
  margin: 8px 0;
  padding: 8px 16px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(4px);
  cursor: pointer;
  transition: all 0.3s ease;
}

.value-item:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: translateY(-2px);
}

.controls-section {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.control-group {
  background: rgba(255, 255, 255, 0.8);
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.control-group h3 {
  margin: 0 0 16px;
  font-size: 18px;
  color: #374151;
}

.slider-group {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.slider-item {
  display: grid;
  grid-template-columns: 30px 1fr 80px;
  align-items: center;
  gap: 12px;
}

.label {
  font-weight: 500;
  color: #374151;
  text-align: center;
}

.color-slider {
  -webkit-appearance: none;
  width: 100%;
  height: 8px;
  border-radius: 4px;
  outline: none;
  opacity: 0.9;
  transition: opacity 0.2s;
}

.color-slider:hover {
  opacity: 1;
}

.color-slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: white;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
  cursor: pointer;
  border: none;
  transition: all 0.2s;
}

.color-slider::-moz-range-thumb {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: white;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
  cursor: pointer;
  border: none;
  transition: all 0.2s;
}

.color-slider::-webkit-slider-thumb:hover {
  transform: scale(1.1);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.25);
}

.color-slider::-moz-range-thumb:hover {
  transform: scale(1.1);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.25);
}

.red-slider {
  background: linear-gradient(to right, #ff000000, #ff0000);
}

.green-slider {
  background: linear-gradient(to right, #00ff0000, #00ff00);
}

.blue-slider {
  background: linear-gradient(to right, #0000ff00, #0000ff);
}

.number-input {
  width: 60px;
  padding: 4px 8px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  font-size: 14px;
  text-align: center;
  outline: none;
  transition: all 0.3s;
}

.number-input:focus {
  border-color: #1890ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}

.number-input::-webkit-inner-spin-button {
  opacity: 1;
  height: 24px;
}

@media (max-width: 768px) {
  .color-picker-page {
    padding: 0;
    height: 94vh;
    margin: 0;
    background: #f5f7fa;
  }

  .color-picker-container {
    height: 100%;
    grid-template-columns: 1fr;
    padding: 16px;
    gap: 16px;
    border-radius: 0;
    box-shadow: none;
    border: none;
    display: flex;
    flex-direction: column;
  }

  .preview-section {
    flex: 0 0 auto;
  }

  .color-preview {
    aspect-ratio: 2;
    border-radius: 16px;
    margin-bottom: 16px;
  }

  .color-values {
    opacity: 1;
    font-size: 13px;
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  .value-item {
    padding: 4px 8px;
    margin: 0;
    font-size: 12px;
    border-radius: 6px;
  }

  .controls-section {
    flex: 1;
    overflow-y: auto;
    padding: 0 0 16px 0;
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .control-group {
    padding: 12px;
    border-radius: 12px;
    background: white;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  }

  .control-group h3 {
    font-size: 14px;
    margin-bottom: 12px;
    color: #333;
  }

  .slider-group {
    gap: 12px;
  }

  .slider-item {
    grid-template-columns: 20px 1fr 60px;
    gap: 8px;
    align-items: center;
  }

  .label {
    font-size: 13px;
    font-weight: 500;
    color: #666;
  }

  .number-input {
    width: 50px;
    padding: 2px 4px;
    font-size: 13px;
  }

  .color-slider::-webkit-slider-thumb {
    width: 24px;
    height: 24px;
  }

  .color-slider::-moz-range-thumb {
    width: 24px;
    height: 24px;
  }
}
</style>
