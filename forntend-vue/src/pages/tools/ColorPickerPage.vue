<template>
  <div class="color-picker-page">
    <div class="color-picker-container">
      <!-- 顶部模式切换 -->
      <div class="mode-switcher">
        <div class="mode-tabs">
          <button
            v-for="mode in modes"
            :key="mode.key"
            :class="['mode-tab', { active: currentMode === mode.key }]"
            @click="currentMode = mode.key"
          >
            {{ mode.label }}
          </button>
        </div>
      </div>

      <!-- 主要内容区域 -->
      <div class="main-content">
        <!-- 颜色预览区域 -->
        <div class="preview-section">
          <div
            class="color-preview"
            :style="{
              backgroundColor: currentColorWithAlpha,
              color: isDarkColor ? '#fff' : '#000'
            }"
          >
            <div class="color-values">
              <div class="format-switcher">
                <button
                  v-for="format in colorFormats"
                  :key="format"
                  :class="['format-btn', { active: currentFormat === format }]"
                  @click="currentFormat = format"
                >
                  {{ format }}
                </button>
              </div>
              <div class="color-value" @click="copyCurrentColor">
                {{ currentColorString }}
              </div>
              <div class="alpha-info" v-if="alpha < 100">
                透明度: {{ alpha }}%
              </div>
            </div>
          </div>

          <!-- 颜色对比区域 -->
          <div class="color-contrast" v-if="showContrast">
            <div class="contrast-colors">
              <div class="color-item current" :style="{ backgroundColor: currentColorWithAlpha }">
                <span>当前</span>
              </div>
              <div class="color-item reference" :style="{ backgroundColor: referenceColor }">
                <span>参考</span>
              </div>
            </div>
            <button class="select-reference-btn" @click="selectReference">
              选择参考色
            </button>
          </div>
        </div>

        <!-- 控制面板 -->
        <div class="controls-section">
          <!-- 基础模式 -->
          <div v-if="currentMode === 'basic'" class="control-group basic-mode">
            <h3>精确输入</h3>
            <div class="input-group">
              <!-- HEX输入 -->
              <div class="input-item">
                <label>HEX颜色值</label>
                <input
                  type="text"
                  v-model="hexInput"
                  @input="updateFromHex"
                  class="color-input hex-input"
                  placeholder="#FFFFFF"
                />
                <div class="input-tips">
                  <span class="tip-text">支持 #RGB 和 #RRGGBB 格式</span>
                  <button @click="generateRandomHex" class="tip-btn">随机</button>
                </div>
              </div>

              <!-- RGB输入 -->
              <div class="input-item">
                <label>RGB颜色值 (0-255)</label>
                <div class="rgb-inputs">
                  <div class="rgb-input-wrapper">
                    <label class="rgb-label">R</label>
                    <input type="number" v-model="rgb.r" min="0" max="255" class="rgb-input" />
                  </div>
                  <div class="rgb-input-wrapper">
                    <label class="rgb-label">G</label>
                    <input type="number" v-model="rgb.g" min="0" max="255" class="rgb-input" />
                  </div>
                  <div class="rgb-input-wrapper">
                    <label class="rgb-label">B</label>
                    <input type="number" v-model="rgb.b" min="0" max="255" class="rgb-input" />
                  </div>
                </div>
                <div class="color-info-display">
                  <span class="info-label">RGB:</span>
                  <span class="info-value">rgb({{ rgb.r }}, {{ rgb.g }}, {{ rgb.b }})</span>
                  <button @click="copyRgbValue" class="copy-btn">复制</button>
                </div>
              </div>

              <!-- HSL输入 -->
              <div class="input-item">
                <label>HSL颜色值</label>
                <div class="hsl-inputs">
                  <div class="hsl-input-wrapper">
                    <label class="hsl-label">H (0-360°)</label>
                    <input type="number" v-model="hsl.h" min="0" max="360" class="hsl-input" />
                  </div>
                  <div class="hsl-input-wrapper">
                    <label class="hsl-label">S (0-100%)</label>
                    <input type="number" v-model="hsl.s" min="0" max="100" class="hsl-input" />
                  </div>
                  <div class="hsl-input-wrapper">
                    <label class="hsl-label">L (0-100%)</label>
                    <input type="number" v-model="hsl.l" min="0" max="100" class="hsl-input" />
                  </div>
                </div>
                <div class="color-info-display">
                  <span class="info-label">HSL:</span>
                  <span class="info-value">hsl({{ hsl.h }}, {{ hsl.s }}%, {{ hsl.l }}%)</span>
                  <button @click="copyHslValue" class="copy-btn">复制</button>
                </div>
              </div>

              <!-- 颜色分析信息 -->

            </div>
          </div>

          <!-- 滑块模式 -->
          <div v-if="currentMode === 'slider'" class="control-group slider-mode">
            <div class="slider-tabs">
              <button
                :class="['slider-tab', { active: sliderMode === 'rgb' }]"
                @click="sliderMode = 'rgb'"
              >
                RGB模式
              </button>
              <button
                :class="['slider-tab', { active: sliderMode === 'hsl' }]"
                @click="sliderMode = 'hsl'"
              >
                HSL模式
              </button>
              <button
                :class="['slider-tab', { active: sliderMode === 'advanced' }]"
                @click="sliderMode = 'advanced'"
              >
                高级调节
              </button>
            </div>

            <!-- RGB 滑块 -->
            <div v-if="sliderMode === 'rgb'" class="slider-group">
              <h4>RGB颜色调节</h4>
              <div class="slider-item">
                <span class="label red-label">R</span>
                <input type="range" v-model="rgb.r" min="0" max="255" class="color-slider red-slider" />
                <input type="number" v-model="rgb.r" min="0" max="255" class="number-input" />
                <div class="slider-info">
                  <span class="percentage">{{ Math.round((rgb.r / 255) * 100) }}%</span>
                </div>
              </div>
              <div class="slider-item">
                <span class="label green-label">G</span>
                <input type="range" v-model="rgb.g" min="0" max="255" class="color-slider green-slider" />
                <input type="number" v-model="rgb.g" min="0" max="255" class="number-input" />
                <div class="slider-info">
                  <span class="percentage">{{ Math.round((rgb.g / 255) * 100) }}%</span>
                </div>
              </div>
              <div class="slider-item">
                <span class="label blue-label">B</span>
                <input type="range" v-model="rgb.b" min="0" max="255" class="color-slider blue-slider" />
                <input type="number" v-model="rgb.b" min="0" max="255" class="number-input" />
                <div class="slider-info">
                  <span class="percentage">{{ Math.round((rgb.b / 255) * 100) }}%</span>
                </div>
              </div>

              <!-- RGB预设值 -->
              <div class="preset-section">
                <h5>RGB预设值</h5>
                <div class="preset-buttons">
                  <button @click="setRgbPreset(255, 0, 0)" class="preset-btn red">纯红</button>
                  <button @click="setRgbPreset(0, 255, 0)" class="preset-btn green">纯绿</button>
                  <button @click="setRgbPreset(0, 0, 255)" class="preset-btn blue">纯蓝</button>
                  <button @click="setRgbPreset(255, 255, 0)" class="preset-btn yellow">黄色</button>
                  <button @click="setRgbPreset(255, 0, 255)" class="preset-btn magenta">品红</button>
                  <button @click="setRgbPreset(0, 255, 255)" class="preset-btn cyan">青色</button>
                </div>
              </div>
            </div>

            <!-- HSL 滑块 -->
            <div v-if="sliderMode === 'hsl'" class="slider-group">
              <h4>HSL颜色调节</h4>
              <div class="slider-item">
                <span class="label hue-label">H</span>
                <input type="range" v-model="hsl.h" min="0" max="360" class="color-slider hue-slider" />
                <input type="number" v-model="hsl.h" min="0" max="360" class="number-input" />
                <div class="slider-info">
                  <span class="hue-name">{{ getHueName(hsl.h) }}</span>
                </div>
              </div>
              <div class="slider-item">
                <span class="label saturation-label">S</span>
                <input type="range" v-model="hsl.s" min="0" max="100" class="color-slider saturation-slider" />
                <input type="number" v-model="hsl.s" min="0" max="100" class="number-input" />
                <div class="slider-info">
                  <span class="saturation-desc">{{ getSaturationDesc(hsl.s) }}</span>
                </div>
              </div>
              <div class="slider-item">
                <span class="label lightness-label">L</span>
                <input type="range" v-model="hsl.l" min="0" max="100" class="color-slider lightness-slider" />
                <input type="number" v-model="hsl.l" min="0" max="100" class="number-input" />
                <div class="slider-info">
                  <span class="lightness-desc">{{ getLightnessDesc(hsl.l) }}</span>
                </div>
              </div>

              <!-- HSL快速调节 -->
              <div class="quick-hsl-section">
                <h5>快速HSL调节</h5>
                <div class="hsl-quick-controls">
                  <div class="quick-control-group">
                    <label>色相偏移</label>
                    <div class="quick-buttons">
                      <button @click="adjustHue(-90)" class="quick-btn">-90°</button>
                      <button @click="adjustHue(-30)" class="quick-btn">-30°</button>
                      <button @click="adjustHue(30)" class="quick-btn">+30°</button>
                      <button @click="adjustHue(90)" class="quick-btn">+90°</button>
                    </div>
                  </div>
                  <div class="quick-control-group">
                    <label>饱和度调节</label>
                    <div class="quick-buttons">
                      <button @click="adjustSaturation(-25)" class="quick-btn">-25%</button>
                      <button @click="adjustSaturation(-10)" class="quick-btn">-10%</button>
                      <button @click="adjustSaturation(10)" class="quick-btn">+10%</button>
                      <button @click="adjustSaturation(25)" class="quick-btn">+25%</button>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 高级调节 -->
            <div v-if="sliderMode === 'advanced'" class="slider-group">
              <h4>高级颜色调节</h4>

              <!-- 透明度调节 -->
              <div class="slider-item">
                <span class="label alpha-label">透明度</span>
                <input type="range" v-model="alpha" min="0" max="100" class="color-slider alpha-slider" />
                <input type="number" v-model="alpha" min="0" max="100" class="number-input" />
                <div class="slider-info">
                  <span class="alpha-desc">{{ getAlphaDesc(alpha) }}</span>
                </div>
              </div>

              <!-- 色温调节 -->
              <div class="slider-item">
                <span class="label temp-label">色温</span>
                <input type="range" v-model="colorTemperature" min="2000" max="10000" step="100" class="color-slider temp-slider" />
                <input type="number" v-model="colorTemperature" min="2000" max="10000" class="number-input" />
                <div class="slider-info">
                  <span class="temp-desc">{{ getTempDesc(colorTemperature) }}</span>
                </div>
              </div>

              <!-- 对比度调节 -->
              <div class="slider-item">
                <span class="label contrast-label">对比度</span>
                <input type="range" v-model="contrastLevel" min="0" max="200" class="color-slider contrast-slider" />
                <input type="number" v-model="contrastLevel" min="0" max="200" class="number-input" />
                <div class="slider-info">
                  <span class="contrast-desc">{{ getContrastDesc(contrastLevel) }}</span>
                </div>
              </div>

              <!-- 颜色滤镜 -->
              <div class="filter-section">
                <h5>颜色滤镜</h5>
                <div class="filter-buttons">
                  <button @click="applyFilter('sepia')" class="filter-btn sepia">复古</button>
                  <button @click="applyFilter('grayscale')" class="filter-btn grayscale">灰度</button>
                  <button @click="applyFilter('invert')" class="filter-btn invert">反色</button>
                  <button @click="applyFilter('warm')" class="filter-btn warm">暖色</button>
                  <button @click="applyFilter('cool')" class="filter-btn cool">冷色</button>
                  <button @click="applyFilter('vivid')" class="filter-btn vivid">鲜艳</button>
                </div>
              </div>

              <!-- 颜色和谐 -->
              <div class="harmony-section">
                <h5>颜色和谐</h5>
                <div class="harmony-buttons">
                  <button @click="generateHarmony('complement')" class="harmony-btn">互补色</button>
                  <button @click="generateHarmony('triadic')" class="harmony-btn">三元色</button>
                  <button @click="generateHarmony('tetradic')" class="harmony-btn">四元色</button>
                  <button @click="generateHarmony('analogous')" class="harmony-btn">类似色</button>
                </div>
                <div v-if="harmonyColors.length > 0" class="harmony-preview">
                  <div
                    v-for="(color, index) in harmonyColors"
                    :key="index"
                    class="harmony-color"
                    :style="{ backgroundColor: color }"
                    @click="loadHarmonyColor(color)"
                    :title="color"
                  ></div>
                </div>
              </div>
            </div>
          </div>

          <!-- 拾色器模式 -->
          <div v-if="currentMode === 'picker'" class="control-group picker-mode">
            <div class="color-wheel-container">
              <canvas
                ref="colorWheel"
                class="color-wheel"
                width="200"
                height="200"
                @mousedown="startWheelDrag"
                @mousemove="onWheelDrag"
                @mouseup="endWheelDrag"
                @touchstart="startWheelDrag"
                @touchmove="onWheelDrag"
                @touchend="endWheelDrag"
              ></canvas>
            </div>
            <div class="saturation-lightness-container">
              <canvas
                ref="saturationLightness"
                class="saturation-lightness"
                width="200"
                height="200"
                @mousedown="startSLDrag"
                @mousemove="onSLDrag"
                @mouseup="endSLDrag"
                @touchstart="startSLDrag"
                @touchmove="onSLDrag"
                @touchend="endSLDrag"
              ></canvas>
            </div>
          </div>
        </div>
        <div class="color-analysis">
          <h4>颜色分析</h4>
          <div class="analysis-grid">
            <div class="analysis-item">
              <span class="analysis-label">亮度</span>
              <span class="analysis-value">{{ colorBrightness }}</span>
            </div>
            <div class="analysis-item">
              <span class="analysis-label">对比度</span>
              <span class="analysis-value">{{ contrastRatio }}</span>
            </div>
            <div class="analysis-item">
              <span class="analysis-label">色温</span>
              <span class="analysis-value">{{ colorTemperature }}K</span>
            </div>
            <div class="analysis-item">
              <span class="analysis-label">类型</span>
              <span class="analysis-value">{{ colorType }}</span>
            </div>
          </div>
        </div>

        <!-- 快速颜色调整 -->
        <div class="quick-adjustments">
          <h4>快速调整</h4>
          <div class="adjustment-buttons">
            <button @click="adjustBrightness(10)" class="adjust-btn bright">提亮</button>
            <button @click="adjustBrightness(-10)" class="adjust-btn dark">变暗</button>
            <button @click="adjustSaturation(15)" class="adjust-btn saturate">增强</button>
            <button @click="adjustSaturation(-15)" class="adjust-btn desaturate">淡化</button>
            <button @click="adjustHue(30)" class="adjust-btn hue-shift">暖色</button>
            <button @click="adjustHue(-30)" class="adjust-btn hue-shift">冷色</button>
          </div>
        </div>

      </div>

      <!-- 颜色操作控制台 - PC端单独占一行 -->
      <div class="control-panel-section">
        <div class="color-control-panel">
          <h4>颜色控制台</h4>

         <div>
           <div>
             <div >
               <div
                 class="color-control"
                 :style="{
              backgroundColor: currentColorWithAlpha,
              color: isDarkColor ? '#fff' : '#000'
            }"
               >
                 <div class="color-values">
                   <div class="format-switcher">
                     <button
                       v-for="format in colorFormats"
                       :key="format"
                       :class="['format-btn', { active: currentFormat === format }]"
                       @click="currentFormat = format"
                     >
                       {{ format }}
                     </button>
                   </div>
                   <div class="color-value" @click="copyCurrentColor">
                     {{ currentColorString }}
                   </div>
                   <div class="alpha-info" v-if="alpha < 100">
                     透明度: {{ alpha }}%
                   </div>
                 </div>
               </div>

               <!-- 颜色对比区域 -->
               <div class="color-contrast" v-if="showContrast">
                 <div class="contrast-colors">
                   <div class="color-item current" :style="{ backgroundColor: currentColorWithAlpha }">
                     <span>当前</span>
                   </div>
                   <div class="color-item reference" :style="{ backgroundColor: referenceColor }">
                     <span>参考</span>
                   </div>
                 </div>
                 <button class="select-reference-btn" @click="selectReference">
                   选择参考色
                 </button>
               </div>
             </div>
           </div>
           <div>
             <!-- 亮度控制 -->
             <div class="control-section">
               <div class="control-label">亮度调节</div>
               <div class="control-buttons">
                 <button @click="adjustBrightness(-20)" class="control-btn decrease">
                   <span class="icon">--</span>
                   <span class="text">-20%</span>
                 </button>
                 <button @click="adjustBrightness(-10)" class="control-btn decrease">
                   <span class="icon">-</span>
                   <span class="text">-10%</span>
                 </button>
                 <div class="control-display">
                   <span class="value">{{ hsl.l }}%</span>
                   <span class="label">亮度</span>
                 </div>
                 <button @click="adjustBrightness(10)" class="control-btn increase">
                   <span class="icon">+</span>
                   <span class="text">+10%</span>
                 </button>
                 <button @click="adjustBrightness(20)" class="control-btn increase">
                   <span class="icon">++</span>
                   <span class="text">+20%</span>
                 </button>
               </div>
             </div>

             <!-- 饱和度控制 -->
             <div class="control-section">
               <div class="control-label">饱和度调节</div>
               <div class="control-buttons">
                 <button @click="adjustSaturation(-20)" class="control-btn decrease">
                   <span class="icon">--</span>
                   <span class="text">-20%</span>
                 </button>
                 <button @click="adjustSaturation(-10)" class="control-btn decrease">
                   <span class="icon">-</span>
                   <span class="text">-10%</span>
                 </button>
                 <div class="control-display">
                   <span class="value">{{ hsl.s }}%</span>
                   <span class="label">饱和度</span>
                 </div>
                 <button @click="adjustSaturation(10)" class="control-btn increase">
                   <span class="icon">+</span>
                   <span class="text">+10%</span>
                 </button>
                 <button @click="adjustSaturation(20)" class="control-btn increase">
                   <span class="icon">++</span>
                   <span class="text">+20%</span>
                 </button>
               </div>
             </div>

             <!-- 色相控制 -->
             <div class="control-section">
               <div class="control-label">色相调节</div>
               <div class="control-buttons">
                 <button @click="adjustHue(-30)" class="control-btn decrease">
                   <span class="icon">&laquo;</span>
                   <span class="text">-30°</span>
                 </button>
                 <button @click="adjustHue(-15)" class="control-btn decrease">
                   <span class="icon">&lsaquo;</span>
                   <span class="text">-15°</span>
                 </button>
                 <div class="control-display">
                   <span class="value">{{ hsl.h }}°</span>
                   <span class="label">色相</span>
                 </div>
                 <button @click="adjustHue(15)" class="control-btn increase">
                   <span class="icon">&rsaquo;</span>
                   <span class="text">+15°</span>
                 </button>
                 <button @click="adjustHue(30)" class="control-btn increase">
                   <span class="icon">&raquo;</span>
                   <span class="text">+30°</span>
                 </button>
               </div>
             </div>
           </div>
         </div>

          <!-- 特殊操作 -->
          <div class="control-section special-ops">
            <div class="control-label">特殊操作</div>
            <div class="special-buttons">
              <button @click="invertColor" class="special-btn invert">
                <span class="icon">⟲</span>
                <span class="text">反转</span>
              </button>
              <button @click="complementaryColor" class="special-btn complement">
                <span class="icon">♾</span>
                <span class="text">互补色</span>
              </button>
              <button @click="randomColor" class="special-btn random">
                <span class="icon">🎲</span>
                <span class="text">随机</span>
              </button>
              <button @click="resetColor" class="special-btn reset">
                <span class="icon">↻</span>
                <span class="text">复位</span>
              </button>
            </div>
          </div>

          <!-- 透明度控制 -->
          <div class="control-section alpha-section">
            <div class="control-label">透明度: {{ alpha }}%</div>
            <div class="alpha-slider-container">
              <input
                type="range"
                v-model="alpha"
                min="0"
                max="100"
                class="alpha-slider-control"
              />
              <div class="alpha-buttons">
                <button @click="setAlpha(0)" class="alpha-btn">透明</button>
                <button @click="setAlpha(50)" class="alpha-btn">50%</button>
                <button @click="setAlpha(100)" class="alpha-btn">不透明</button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部面板 - 历史记录和预设面板左右并列 -->
      <div class="bottom-panel">
        <!-- 历史记录 -->
        <div class="history-section">
          <h4>历史记录 ({{ colorHistory.length }}/10)</h4>
          <div class="history-colors">
            <div
              v-for="(color, index) in colorHistory"
              :key="index"
              class="history-color"
              :style="{ backgroundColor: color }"
              @click="loadFromHistory(color)"
              :title="`点击应用: ${color}`"
            >
              <div class="color-info">
                <span class="color-text">{{ color.toUpperCase() }}</span>
              </div>
            </div>
            <div v-for="n in Math.max(0, 10 - colorHistory.length)" :key="'empty-' + n" class="history-color empty" title="空位">
              <span class="empty-text">+</span>
            </div>
          </div>
        </div>

        <!-- 预设调色板 -->
        <div class="palette-section">
          <h4>预设调色板</h4>
          <div class="palette-tabs">
            <button
              v-for="palette in colorPalettes"
              :key="palette.name"
              :class="['palette-tab', { active: currentPalette === palette.name }]"
              @click="currentPalette = palette.name"
            >
              {{ palette.name }}
            </button>
          </div>
          <div class="palette-colors">
            <div
              v-for="(color, index) in currentPaletteColors"
              :key="index"
              class="palette-color"
              :style="{ backgroundColor: color }"
              @click="loadFromPalette(color)"
              :title="`点击应用: ${color.toUpperCase()}`"
            >
              <div class="color-info">
                <span class="color-text">{{ color.toUpperCase() }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import { message } from 'ant-design-vue'

// 接口定义
interface RGB {
  r: number
  g: number
  b: number
}

interface HSL {
  h: number
  s: number
  l: number
}

interface ColorPalette {
  name: string
  colors: string[]
}

// 模式配置
const modes = [
  { key: 'basic', label: '基础模式' },
  { key: 'slider', label: '滑块模式' },
  { key: 'picker', label: '拾色器' }
]

const colorFormats = ['HEX', 'RGB', 'HSL']

// 响应式状态
const currentMode = ref<string>('basic')
const sliderMode = ref<string>('rgb')
const currentFormat = ref<string>('HEX')
const alpha = ref<number>(100)
const currentPalette = ref<string>('Material Design')
const showContrast = ref<boolean>(false)
const referenceColor = ref<string>('#ffffff')
const colorTemperature = ref<number>(6500)
const contrastLevel = ref<number>(100)
const convertedValue = ref<string>('')
const harmonyColors = ref<string[]>([])

// 颜色状态
const rgb = ref<RGB>({ r: 52, g: 152, b: 219 })
const hsl = ref<HSL>({ h: 204, s: 71, l: 53 })
const hexInput = ref<string>('#3498db')

// Canvas 相关
const colorWheel = ref<HTMLCanvasElement | null>(null)
const saturationLightness = ref<HTMLCanvasElement | null>(null)

// 历史记录
const colorHistory = ref<string[]>([])

// 预设调色板
const colorPalettes: ColorPalette[] = [
  {
    name: 'Material Design',
    colors: [
      '#f44336', '#e91e63', '#9c27b0', '#673ab7',
      '#3f51b5', '#2196f3', '#03a9f4', '#00bcd4',
      '#009688', '#4caf50', '#8bc34a', '#cddc39'
    ]
  },
  {
    name: '扁平化',
    colors: [
      '#1abc9c', '#2ecc71', '#3498db', '#9b59b6',
      '#34495e', '#16a085', '#27ae60', '#2980b9',
      '#8e44ad', '#2c3e50', '#f1c40f', '#e67e22'
    ]
  },
  {
    name: '复古风',
    colors: [
      '#d4a574', '#c89666', '#bc8758', '#b0794a',
      '#a46a3c', '#985c2e', '#8c4e20', '#804012',
      '#74320f', '#68240c', '#5c1609', '#500806'
    ]
  }
]

// 计算属性
const currentColorWithAlpha = computed(() => {
  const a = alpha.value / 100
  return `rgba(${rgb.value.r}, ${rgb.value.g}, ${rgb.value.b}, ${a})`
})

const hexColor = computed(() => {
  const r = rgb.value.r.toString(16).padStart(2, '0')
  const g = rgb.value.g.toString(16).padStart(2, '0')
  const b = rgb.value.b.toString(16).padStart(2, '0')
  return `#${r}${g}${b}`
})

const rgbColor = computed(() => {
  return `rgb(${rgb.value.r}, ${rgb.value.g}, ${rgb.value.b})`
})

const hslColor = computed(() => {
  return `hsl(${hsl.value.h}, ${hsl.value.s}%, ${hsl.value.l}%)`
})

const currentColorString = computed(() => {
  switch (currentFormat.value) {
    case 'HEX':
      return hexColor.value
    case 'RGB':
      return alpha.value < 100
        ? `rgba(${rgb.value.r}, ${rgb.value.g}, ${rgb.value.b}, ${alpha.value / 100})`
        : rgbColor.value
    case 'HSL':
      return alpha.value < 100
        ? `hsla(${hsl.value.h}, ${hsl.value.s}%, ${hsl.value.l}%, ${alpha.value / 100})`
        : hslColor.value
    default:
      return hexColor.value
  }
})

const isDarkColor = computed(() => {
  const brightness = (rgb.value.r * 299 + rgb.value.g * 587 + rgb.value.b * 114) / 1000
  return brightness < 128
})

const currentPaletteColors = computed(() => {
  const palette = colorPalettes.find(p => p.name === currentPalette.value)
  return palette ? palette.colors : []
})

// 新增计算属性
const colorBrightness = computed(() => {
  const brightness = (rgb.value.r * 299 + rgb.value.g * 587 + rgb.value.b * 114) / 1000
  return Math.round(brightness)
})

const contrastRatio = computed(() => {
  const brightness = colorBrightness.value
  const ratio = brightness > 128 ? ((brightness + 5) / 5) : (5 / (brightness + 5))
  return ratio.toFixed(2)
})

const colorType = computed(() => {
  if (hsl.value.s < 10) return '灰色'
  if (hsl.value.l < 20) return '深色'
  if (hsl.value.l > 80) return '浅色'
  if (hsl.value.s > 80) return '鲜艳'
  return '中性'
})

// 颜色转换函数
const rgbToHsl = (r: number, g: number, b: number): HSL => {
  r /= 255
  g /= 255
  b /= 255

  const max = Math.max(r, g, b)
  const min = Math.min(r, g, b)
  let h = 0, s = 0
  const l = (max + min) / 2

  if (max !== min) {
    const d = max - min
    s = l > 0.5 ? d / (2 - max - min) : d / (max + min)

    switch (max) {
      case r: h = (g - b) / d + (g < b ? 6 : 0); break
      case g: h = (b - r) / d + 2; break
      case b: h = (r - g) / d + 4; break
    }
    h /= 6
  }

  return {
    h: Math.round(h * 360),
    s: Math.round(s * 100),
    l: Math.round(l * 100)
  }
}

const hslToRgb = (h: number, s: number, l: number): RGB => {
  h /= 360
  s /= 100
  l /= 100

  const hue2rgb = (p: number, q: number, t: number): number => {
    if (t < 0) t += 1
    if (t > 1) t -= 1
    if (t < 1/6) return p + (q - p) * 6 * t
    if (t < 1/2) return q
    if (t < 2/3) return p + (q - p) * (2/3 - t) * 6
    return p
  }

  let r, g, b

  if (s === 0) {
    r = g = b = l
  } else {
    const q = l < 0.5 ? l * (1 + s) : l + s - l * s
    const p = 2 * l - q
    r = hue2rgb(p, q, h + 1/3)
    g = hue2rgb(p, q, h)
    b = hue2rgb(p, q, h - 1/3)
  }

  return {
    r: Math.round(r * 255),
    g: Math.round(g * 255),
    b: Math.round(b * 255)
  }
}

const hexToRgb = (hex: string): RGB | null => {
  const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex)
  return result ? {
    r: parseInt(result[1], 16),
    g: parseInt(result[2], 16),
    b: parseInt(result[3], 16)
  } : null
}

const rgbToHex = (r: number, g: number, b: number): string => {
  const toHex = (n: number) => {
    const hex = Math.round(Math.max(0, Math.min(255, n))).toString(16)
    return hex.length === 1 ? '0' + hex : hex
  }
  return `#${toHex(r)}${toHex(g)}${toHex(b)}`
}

// 操作函数
const addToHistory = (color: string) => {
  const index = colorHistory.value.indexOf(color)
  if (index > -1) {
    colorHistory.value.splice(index, 1)
  }
  colorHistory.value.unshift(color)
  if (colorHistory.value.length > 10) {
    colorHistory.value = colorHistory.value.slice(0, 10)
  }
  localStorage.setItem('colorPickerHistory', JSON.stringify(colorHistory.value))
}

const copyCurrentColor = () => {
  navigator.clipboard.writeText(currentColorString.value).then(() => {
    message.success('颜色值已复制到剪贴板')
    addToHistory(currentColorString.value)
  }).catch(() => {
    message.error('复制失败')
  })
}

const updateFromHex = () => {
  try {
    const rgbResult = hexToRgb(hexInput.value)
    if (rgbResult) {
      // 直接设置 RGB 值，会触发 watch 更新
      rgb.value.r = rgbResult.r
      rgb.value.g = rgbResult.g
      rgb.value.b = rgbResult.b
    } else {
      message.error('无效的HEX颜色格式')
    }
  } catch {
    message.error('颜色格式解析失败')
  }
}

const invertColor = () => {
  // 直接修改RGB值，会触发watch更新
  rgb.value.r = 255 - rgb.value.r
  rgb.value.g = 255 - rgb.value.g
  rgb.value.b = 255 - rgb.value.b
  addToHistory(rgbToHex(rgb.value.r, rgb.value.g, rgb.value.b))
  message.success('颜色已反转')
}

const complementaryColor = () => {
  hsl.value.h = (hsl.value.h + 180) % 360
  addToHistory(rgbToHex(rgb.value.r, rgb.value.g, rgb.value.b))
  message.success('已切换到互补色')
}

const randomColor = () => {
  hsl.value.h = Math.floor(Math.random() * 360)
  hsl.value.s = Math.floor(Math.random() * 60) + 40 // 40-100%
  hsl.value.l = Math.floor(Math.random() * 40) + 30 // 30-70%
  addToHistory(rgbToHex(rgb.value.r, rgb.value.g, rgb.value.b))
  message.success('随机颜色生成')
}

const resetColor = () => {
  rgb.value = { r: 52, g: 152, b: 219 }
  alpha.value = 100
  message.success('颜色已复位')
}

const setAlpha = (value: number) => {
  alpha.value = value
  message.success(`透明度设置为 ${value}%`)
}

const selectReference = () => {
  referenceColor.value = currentColorString.value
  message.success('已设置为参考色')
}

const loadFromHistory = (color: string) => {
  const rgbResult = hexToRgb(color)
  if (rgbResult) {
    // 直接设置 RGB 值，会触发 watch 更新
    rgb.value.r = rgbResult.r
    rgb.value.g = rgbResult.g
    rgb.value.b = rgbResult.b
    message.success(`已应用历史颜色: ${color}`)
  }
}

const loadFromPalette = (color: string) => {
  const rgbResult = hexToRgb(color)
  if (rgbResult) {
    // 直接设置 RGB 值，会触发 watch 更新
    rgb.value.r = rgbResult.r
    rgb.value.g = rgbResult.g
    rgb.value.b = rgbResult.b
    addToHistory(color)
    message.success(`已应用调色板颜色: ${color}`)
  }
}

// 新增功能函数
const generateRandomHex = () => {
  const randomColor = '#' + Math.floor(Math.random()*16777215).toString(16).padStart(6, '0')
  hexInput.value = randomColor
  updateFromHex()
  message.success('已生成随机颜色')
}

const copyRgbValue = () => {
  const rgbString = `rgb(${rgb.value.r}, ${rgb.value.g}, ${rgb.value.b})`
  navigator.clipboard.writeText(rgbString).then(() => {
    message.success('RGB值已复制')
  })
}

const copyHslValue = () => {
  const hslString = `hsl(${hsl.value.h}, ${hsl.value.s}%, ${hsl.value.l}%)`
  navigator.clipboard.writeText(hslString).then(() => {
    message.success('HSL值已复制')
  })
}

const adjustBrightness = (delta: number) => {
  hsl.value.l = Math.max(0, Math.min(100, hsl.value.l + delta))
  addToHistory(rgbToHex(rgb.value.r, rgb.value.g, rgb.value.b))
  message.success(`亮度调节 ${delta > 0 ? '+' : ''}${delta}%`)
}

const adjustSaturation = (delta: number) => {
  hsl.value.s = Math.max(0, Math.min(100, hsl.value.s + delta))
  addToHistory(rgbToHex(rgb.value.r, rgb.value.g, rgb.value.b))
  message.success(`饱和度调节 ${delta > 0 ? '+' : ''}${delta}%`)
}

const adjustHue = (delta: number) => {
  hsl.value.h = (hsl.value.h + delta + 360) % 360
  addToHistory(rgbToHex(rgb.value.r, rgb.value.g, rgb.value.b))
  message.success(`色相调节 ${delta > 0 ? '+' : ''}${delta}°`)
}

const convertToFormat = (format: string) => {
  const r = rgb.value.r
  const g = rgb.value.g
  const b = rgb.value.b
  const hex = hexColor.value

  switch (format) {
    case 'css':
      convertedValue.value = `--primary-color: ${hex};\n--primary-rgb: ${r}, ${g}, ${b};\n--primary-hsl: ${hsl.value.h}, ${hsl.value.s}%, ${hsl.value.l}%;`
      break
    case 'android':
      convertedValue.value = `<color name="primary_color">${hex}</color>`
      break
    case 'ios':
      convertedValue.value = `UIColor(red: ${(r/255).toFixed(3)}, green: ${(g/255).toFixed(3)}, blue: ${(b/255).toFixed(3)}, alpha: 1.0)`
      break
    case 'photoshop':
      convertedValue.value = `R:${r} G:${g} B:${b}\nH:${hsl.value.h}° S:${hsl.value.s}% L:${hsl.value.l}%`
      break
  }
  message.success(`已转换为${format}格式`)
}

const copyConvertedValue = () => {
  navigator.clipboard.writeText(convertedValue.value).then(() => {
    message.success('转换结果已复制')
  })
}

const setRgbPreset = (r: number, g: number, b: number) => {
  rgb.value = { r, g, b }
  addToHistory(rgbToHex(r, g, b))
  message.success('已应用RGB预设值')
}

const getHueName = (hue: number): string => {
  if (hue >= 0 && hue < 30) return '红色'
  if (hue >= 30 && hue < 60) return '橙色'
  if (hue >= 60 && hue < 90) return '黄色'
  if (hue >= 90 && hue < 150) return '绿色'
  if (hue >= 150 && hue < 210) return '青色'
  if (hue >= 210 && hue < 270) return '蓝色'
  if (hue >= 270 && hue < 330) return '紫色'
  return '红色'
}

const getSaturationDesc = (saturation: number): string => {
  if (saturation < 20) return '很淡'
  if (saturation < 40) return '淡'
  if (saturation < 60) return '中等'
  if (saturation < 80) return '浓'
  return '很浓'
}

const getLightnessDesc = (lightness: number): string => {
  if (lightness < 20) return '很暗'
  if (lightness < 40) return '暗'
  if (lightness < 60) return '中等'
  if (lightness < 80) return '亮'
  return '很亮'
}

const getAlphaDesc = (alpha: number): string => {
  if (alpha < 20) return '几乎透明'
  if (alpha < 40) return '很透明'
  if (alpha < 60) return '半透明'
  if (alpha < 80) return '略透明'
  return '不透明'
}

const getTempDesc = (temp: number): string => {
  if (temp < 3000) return '很暖'
  if (temp < 4000) return '暖'
  if (temp < 6000) return '中性'
  if (temp < 8000) return '冷'
  return '很冷'
}

const getContrastDesc = (contrast: number): string => {
  if (contrast < 50) return '很低'
  if (contrast < 100) return '低'
  if (contrast < 150) return '正常'
  return '高'
}

const applyFilter = (filter: string) => {
  switch (filter) {
    case 'sepia':
      const sepiaR = Math.min(255, Math.round(rgb.value.r * 0.393 + rgb.value.g * 0.769 + rgb.value.b * 0.189))
      const sepiaG = Math.min(255, Math.round(rgb.value.r * 0.349 + rgb.value.g * 0.686 + rgb.value.b * 0.168))
      const sepiaB = Math.min(255, Math.round(rgb.value.r * 0.272 + rgb.value.g * 0.534 + rgb.value.b * 0.131))
      rgb.value = { r: sepiaR, g: sepiaG, b: sepiaB }
      break
    case 'grayscale':
      const gray = Math.round(rgb.value.r * 0.299 + rgb.value.g * 0.587 + rgb.value.b * 0.114)
      rgb.value = { r: gray, g: gray, b: gray }
      break
    case 'invert':
      rgb.value = { r: 255 - rgb.value.r, g: 255 - rgb.value.g, b: 255 - rgb.value.b }
      break
    case 'warm':
      rgb.value.r = Math.min(255, Math.round(rgb.value.r * 1.1))
      rgb.value.g = Math.min(255, Math.round(rgb.value.g * 1.05))
      break
    case 'cool':
      rgb.value.b = Math.min(255, Math.round(rgb.value.b * 1.1))
      rgb.value.g = Math.min(255, Math.round(rgb.value.g * 1.05))
      break
    case 'vivid':
      hsl.value.s = Math.min(100, hsl.value.s + 20)
      break
  }
  addToHistory(rgbToHex(rgb.value.r, rgb.value.g, rgb.value.b))
  message.success(`已应用${filter}滤镜`)
}

const generateHarmony = (type: string) => {
  const baseHue = hsl.value.h
  const baseSat = hsl.value.s
  const baseLig = hsl.value.l

  harmonyColors.value = []

  switch (type) {
    case 'complement':
      harmonyColors.value = [
        hslToHex(baseHue, baseSat, baseLig),
        hslToHex((baseHue + 180) % 360, baseSat, baseLig)
      ]
      break
    case 'triadic':
      harmonyColors.value = [
        hslToHex(baseHue, baseSat, baseLig),
        hslToHex((baseHue + 120) % 360, baseSat, baseLig),
        hslToHex((baseHue + 240) % 360, baseSat, baseLig)
      ]
      break
    case 'tetradic':
      harmonyColors.value = [
        hslToHex(baseHue, baseSat, baseLig),
        hslToHex((baseHue + 90) % 360, baseSat, baseLig),
        hslToHex((baseHue + 180) % 360, baseSat, baseLig),
        hslToHex((baseHue + 270) % 360, baseSat, baseLig)
      ]
      break
    case 'analogous':
      harmonyColors.value = [
        hslToHex((baseHue - 30 + 360) % 360, baseSat, baseLig),
        hslToHex(baseHue, baseSat, baseLig),
        hslToHex((baseHue + 30) % 360, baseSat, baseLig)
      ]
      break
  }
  message.success(`已生成${type}配色方案`)
}

const hslToHex = (h: number, s: number, l: number): string => {
  const rgbResult = hslToRgb(h, s, l)
  return rgbToHex(rgbResult.r, rgbResult.g, rgbResult.b)
}

const loadHarmonyColor = (color: string) => {
  const rgbResult = hexToRgb(color)
  if (rgbResult) {
    rgb.value = rgbResult
    addToHistory(color)
    message.success(`已应用和谐色: ${color}`)
  }
}

// 监听颜色变化
watch(rgb, () => {
  // 避免无限循环更新
  const newHsl = rgbToHsl(rgb.value.r, rgb.value.g, rgb.value.b)
  if (JSON.stringify(newHsl) !== JSON.stringify(hsl.value)) {
    hsl.value = newHsl
  }
  const newHex = rgbToHex(rgb.value.r, rgb.value.g, rgb.value.b)
  if (hexInput.value !== newHex) {
    hexInput.value = newHex
  }
  updateCSSVariables()
}, { deep: true })

watch(hsl, () => {
  // 避免无限循环更新
  const newRgb = hslToRgb(hsl.value.h, hsl.value.s, hsl.value.l)
  if (JSON.stringify(newRgb) !== JSON.stringify(rgb.value)) {
    rgb.value = newRgb
  }
  const newHex = rgbToHex(newRgb.r, newRgb.g, newRgb.b)
  if (hexInput.value !== newHex) {
    hexInput.value = newHex
  }
  updateCSSVariables()
  // 重新绘制饱和度-亮度选择器
  if (currentMode.value === 'picker') {
    nextTick(() => {
      drawSaturationLightness()
    })
  }
}, { deep: true })

// 更新CSS变量
const updateCSSVariables = () => {
  if (typeof document !== 'undefined') {
    document.documentElement.style.setProperty('--current-hue', hsl.value.h.toString())
    document.documentElement.style.setProperty('--current-saturation', `${hsl.value.s}%`)
  }
}

// 监听模式切换
watch(currentMode, (newMode) => {
  if (newMode === 'picker') {
    nextTick(() => {
      drawColorWheel()
      drawSaturationLightness()
    })
  }
})

// 初始化
onMounted(() => {
  const savedHistory = localStorage.getItem('colorPickerHistory')
  if (savedHistory) {
    colorHistory.value = JSON.parse(savedHistory)
  } else {
    // 添加一些初始演示颜色
    colorHistory.value = [
      '#3498db', '#e74c3c', '#2ecc71', '#f39c12', '#9b59b6'
    ]
    localStorage.setItem('colorPickerHistory', JSON.stringify(colorHistory.value))
  }

  // 初始化Canvas
  nextTick(() => {
    drawColorWheel()
    drawSaturationLightness()
    updateCSSVariables()
  })

  // 添加快捷键支持
  const handleKeydown = (event: KeyboardEvent) => {
    if (event.ctrlKey || event.metaKey) {
      switch (event.key) {
        case 'c':
        case 'C':
          event.preventDefault()
          copyCurrentColor()
          break
        case 'i':
        case 'I':
          event.preventDefault()
          invertColor()
          break
      }
    }
  }

  document.addEventListener('keydown', handleKeydown)
})

// Canvas相关函数
const drawColorWheel = () => {
  if (!colorWheel.value) return

  const canvas = colorWheel.value
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  const centerX = canvas.width / 2
  const centerY = canvas.height / 2
  const radius = Math.min(centerX, centerY) - 10

  // 绘制色相环
  for (let angle = 0; angle < 360; angle++) {
    const startAngle = (angle - 1) * Math.PI / 180
    const endAngle = angle * Math.PI / 180

    ctx.beginPath()
    ctx.arc(centerX, centerY, radius, startAngle, endAngle)
    ctx.arc(centerX, centerY, radius * 0.6, endAngle, startAngle, true)
    ctx.closePath()

    const hslColor = `hsl(${angle}, 100%, 50%)`
    ctx.fillStyle = hslColor
    ctx.fill()
  }
}

const drawSaturationLightness = () => {
  if (!saturationLightness.value) return

  const canvas = saturationLightness.value
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  const width = canvas.width
  const height = canvas.height

  // 创建渐变
  const imageData = ctx.createImageData(width, height)
  const data = imageData.data

  for (let x = 0; x < width; x++) {
    for (let y = 0; y < height; y++) {
      const saturation = (x / width) * 100
      const lightness = ((height - y) / height) * 100

      const rgb = hslToRgb(hsl.value.h, saturation, lightness)
      const index = (y * width + x) * 4

      data[index] = rgb.r     // Red
      data[index + 1] = rgb.g // Green
      data[index + 2] = rgb.b // Blue
      data[index + 3] = 255   // Alpha
    }
  }

  ctx.putImageData(imageData, 0, 0)
}

// Canvas交互事件处理
const startWheelDrag = (event: MouseEvent | TouchEvent) => {
  isDraggingWheel.value = true
  onWheelDrag(event)
}

const onWheelDrag = (event: MouseEvent | TouchEvent) => {
  if (!isDraggingWheel.value || !colorWheel.value) return

  event.preventDefault()
  const rect = colorWheel.value.getBoundingClientRect()
  const centerX = rect.width / 2
  const centerY = rect.height / 2

  let clientX, clientY
  if (event instanceof MouseEvent) {
    clientX = event.clientX - rect.left
    clientY = event.clientY - rect.top
  } else {
    clientX = event.touches[0].clientX - rect.left
    clientY = event.touches[0].clientY - rect.top
  }

  const angle = Math.atan2(clientY - centerY, clientX - centerX)
  const hue = (angle * 180 / Math.PI + 360) % 360

  hsl.value.h = Math.round(hue)
  // watch 会自动同步 RGB 和 Hex 值
  drawSaturationLightness()
}

const endWheelDrag = () => {
  isDraggingWheel.value = false
}

const startSLDrag = (event: MouseEvent | TouchEvent) => {
  isDraggingSL.value = true
  onSLDrag(event)
}

const onSLDrag = (event: MouseEvent | TouchEvent) => {
  if (!isDraggingSL.value || !saturationLightness.value) return

  event.preventDefault()
  const rect = saturationLightness.value.getBoundingClientRect()

  let clientX, clientY
  if (event instanceof MouseEvent) {
    clientX = event.clientX - rect.left
    clientY = event.clientY - rect.top
  } else {
    clientX = event.touches[0].clientX - rect.left
    clientY = event.touches[0].clientY - rect.top
  }

  const saturation = Math.max(0, Math.min(100, (clientX / rect.width) * 100))
  const lightness = Math.max(0, Math.min(100, ((rect.height - clientY) / rect.height) * 100))

  hsl.value.s = Math.round(saturation)
  hsl.value.l = Math.round(lightness)
  // watch 会自动同步 RGB 和 Hex 值
}

const endSLDrag = () => {
  isDraggingSL.value = false
}

// 添加缺失的状态变量
const isDraggingWheel = ref<boolean>(false)
const isDraggingSL = ref<boolean>(false)
</script>

<style scoped>
.color-picker-page {
  color: var(--text-primary);
  overflow: auto;
  padding: 20px;
}

.color-picker-container {
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 24px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* 模式切换 */
.mode-switcher {
  display: flex;
  justify-content: center;
}

.mode-tabs {
  display: flex;
  background: rgba(0, 0, 0, 0.05);
  border-radius: 12px;
  padding: 4px;
  gap: 2px;
}

.mode-tab {
  padding: 8px 16px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: #666;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 14px;
  font-weight: 500;
}

.mode-tab.active {
  background: white;
  color: #333;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.mode-tab:hover:not(.active) {
  background: rgba(255, 255, 255, 0.5);
}

/* 主要内容区域 */
.main-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 32px;
  align-items: start;
  margin-bottom: 24px;
}

/* 控制面板区域 - 单独占一行 */
.control-panel-section {
  margin: 24px 0;
}

/* 预览区域 */
.preview-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
  height: auto;
}

.color-preview {
  aspect-ratio: 1;
  min-height: 280px;
  border-radius: 20px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
  cursor: pointer;
  animation: colorPreviewPulse 2s ease-in-out infinite;
}

@keyframes colorPreviewPulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.02); }
}

.color-preview:hover {
  animation: none;
  transform: scale(1.05);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.15);
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

.format-switcher {
  display: flex;
  gap: 4px;
  margin-bottom: 12px;
  justify-content: center;
}

.format-btn {
  padding: 4px 8px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(4px);
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 12px;
  color: inherit;
}

.format-btn.active {
  background: rgba(255, 255, 255, 0.4);
  border-color: rgba(255, 255, 255, 0.5);
}

.format-btn:hover {
  background: rgba(255, 255, 255, 0.3);
}

.color-value {
  padding: 8px 16px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(4px);
  cursor: pointer;
  transition: all 0.3s ease;
  font-family: 'Courier New', monospace;
}

.color-value:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: translateY(-2px);
}

.alpha-info {
  margin-top: 8px;
  font-size: 12px;
  opacity: 0.8;
}

/* 颜色对比 */
.color-contrast {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.contrast-colors {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 12px;
}

.color-item {
  aspect-ratio: 2;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 500;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  color: var(--text-contrast, inherit);
}

.select-reference-btn {
  width: 100%;
  padding: 8px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  background: white;
  cursor: pointer;
  transition: all 0.3s;
  font-size: 12px;
  color: #666;
}

.select-reference-btn:hover {
  background: #f5f5f5;
  border-color: #1890ff;
  color: #1890ff;
}

/* 控制面板 */
.controls-section {
  display: flex;
  flex-direction: column;
  gap: 24px;
  height: auto;
  justify-content: flex-start;
}

.control-group {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(8px);
  transition: all 0.3s ease;
  flex-shrink: 0;
}

.control-group:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}

.control-group h3 {
  margin: 0 0 16px;
  font-size: 18px;
  color: #2c3e50;
  font-weight: 600;
  border-bottom: 2px solid #e8f4fd;
  padding-bottom: 8px;
}

/* 基础模式 */
.basic-mode {
  min-height: 280px;
  height: auto;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(20px);
  border-radius: 20px;
  padding: 24px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.input-group {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: auto;
}

.input-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.input-item label {
  font-size: 14px;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 4px;
}

.color-input {
  width: 100%;
  padding: 12px 16px;
  border: 2px solid rgba(52, 152, 219, 0.2);
  border-radius: 12px;
  font-size: 14px;
  font-family: 'Courier New', monospace;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(4px);
  transition: all 0.3s ease;
  color: #2c3e50;
}

.color-input:focus {
  outline: none;
  border-color: #3498db;
  box-shadow: 0 0 0 3px rgba(52, 152, 219, 0.1);
  background: white;
}

.rgb-inputs, .hsl-inputs {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 12px;
}

.rgb-input, .hsl-input {
  padding: 12px 8px;
  border: 2px solid rgba(52, 152, 219, 0.2);
  border-radius: 10px;
  font-size: 14px;
  text-align: center;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(4px);
  transition: all 0.3s ease;
  color: #2c3e50;
  font-weight: 500;
}

.rgb-input:focus, .hsl-input:focus {
  outline: none;
  border-color: #3498db;
  box-shadow: 0 0 0 3px rgba(52, 152, 219, 0.1);
  background: white;
}

/* 滑块模式 */
.slider-mode {
  min-height: 280px;
  height: auto;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(20px);
  border-radius: 20px;
  padding: 24px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.slider-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
  background: rgba(0, 0, 0, 0.05);
  border-radius: 12px;
  padding: 4px;
}

.slider-tab {
  flex: 1;
  padding: 8px 16px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: #666;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 14px;
  font-weight: 500;
}

.slider-tab.active {
  background: white;
  color: #333;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.slider-tab:hover:not(.active) {
  background: rgba(255, 255, 255, 0.5);
}

.slider-group {
  display: flex;
  flex-direction: column;
  gap: 18px;
  height: auto;
}

.slider-item {
  display: grid;
  grid-template-columns: 40px 1fr 80px 80px;
  gap: 16px;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid rgba(52, 152, 219, 0.1);
}

.slider-item:last-child {
  border-bottom: none;
}

.slider-item .label {
  font-size: 16px;
  font-weight: 600;
  color: #2c3e50;
  text-align: center;
  background: rgba(52, 152, 219, 0.1);
  padding: 8px 4px;
  border-radius: 8px;
  min-width: 32px;
}

.slider-item .red-label {
  background: rgba(231, 76, 60, 0.1);
  color: #e74c3c;
}

.slider-item .green-label {
  background: rgba(39, 174, 96, 0.1);
  color: #27ae60;
}

.slider-item .blue-label {
  background: rgba(52, 152, 219, 0.1);
  color: #3498db;
}

.slider-item .hue-label {
  background: linear-gradient(45deg, #ff0000, #ffff00, #00ff00, #00ffff, #0000ff, #ff00ff, #ff0000);
  color: white;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
}

.slider-item .saturation-label {
  background: rgba(155, 89, 182, 0.1);
  color: #9b59b6;
}

.slider-item .lightness-label {
  background: rgba(52, 73, 94, 0.1);
  color: #34495e;
}

.slider-item .alpha-label {
  background: rgba(149, 165, 166, 0.1);
  color: #95a5a6;
}

.slider-item .temp-label {
  background: linear-gradient(45deg, #3498db, #e67e22);
  color: white;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
}

.slider-item .contrast-label {
  background: rgba(44, 62, 80, 0.1);
  color: #2c3e50;
}

.slider-info {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 70px;
  padding: 6px 8px;
  background: rgba(236, 240, 241, 0.8);
  border-radius: 6px;
  font-size: 11px;
  color: #34495e;
  font-weight: 500;
}

.percentage {
  color: #7f8c8d;
}

.hue-name {
  color: #e67e22;
  font-weight: 600;
}

.saturation-desc {
  color: #9b59b6;
}

.lightness-desc {
  color: #34495e;
}

.alpha-desc {
  color: #95a5a6;
}

.temp-desc {
  color: #3498db;
}

.contrast-desc {
  color: #2c3e50;
}

.preset-section, .quick-hsl-section, .filter-section, .harmony-section {
  margin-top: 20px;
  padding: 16px;
  background: rgba(236, 240, 241, 0.3);
  border-radius: 12px;
  border: 1px solid rgba(189, 195, 199, 0.3);
}

.preset-section h5, .quick-hsl-section h5, .filter-section h5, .harmony-section h5 {
  margin: 0 0 12px;
  font-size: 13px;
  color: #2c3e50;
  font-weight: 600;
}

.preset-buttons, .filter-buttons, .harmony-buttons {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.preset-btn, .filter-btn, .harmony-btn {
  padding: 8px 12px;
  border: none;
  border-radius: 6px;
  font-size: 10px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  color: white;
  text-shadow: 0 1px 1px rgba(0, 0, 0, 0.2);
}

.preset-btn.red { background: linear-gradient(45deg, #e74c3c, #c0392b); }
.preset-btn.green { background: linear-gradient(45deg, #27ae60, #229954); }
.preset-btn.blue { background: linear-gradient(45deg, #3498db, #2980b9); }
.preset-btn.yellow { background: linear-gradient(45deg, #f1c40f, #f39c12); }
.preset-btn.magenta { background: linear-gradient(45deg, #e91e63, #ad1457); }
.preset-btn.cyan { background: linear-gradient(45deg, #1abc9c, #16a085); }

.filter-btn.sepia { background: linear-gradient(45deg, #d2b48c, #bc9a6a); }
.filter-btn.grayscale { background: linear-gradient(45deg, #95a5a6, #7f8c8d); }
.filter-btn.invert { background: linear-gradient(45deg, #34495e, #2c3e50); }
.filter-btn.warm { background: linear-gradient(45deg, #e67e22, #d35400); }
.filter-btn.cool { background: linear-gradient(45deg, #3498db, #2980b9); }
.filter-btn.vivid { background: linear-gradient(45deg, #e74c3c, #c0392b); }

.harmony-btn {
  background: linear-gradient(45deg, #9b59b6, #8e44ad);
}

.preset-btn:hover, .filter-btn:hover, .harmony-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 3px 6px rgba(0, 0, 0, 0.2);
}

.quick-control-group {
  margin-bottom: 12px;
}

.quick-control-group:last-child {
  margin-bottom: 0;
}

.quick-control-group label {
  display: block;
  font-size: 12px;
  color: #34495e;
  font-weight: 500;
  margin-bottom: 6px;
}

.quick-buttons {
  display: flex;
  gap: 6px;
}

.quick-btn {
  flex: 1;
  padding: 6px 8px;
  background: linear-gradient(45deg, #ecf0f1, #bdc3c7);
  border: 1px solid #bdc3c7;
  border-radius: 4px;
  font-size: 10px;
  color: #2c3e50;
  cursor: pointer;
  transition: all 0.2s ease;
}

.quick-btn:hover {
  background: linear-gradient(45deg, #3498db, #2980b9);
  color: white;
  border-color: #3498db;
  transform: scale(1.02);
}

.harmony-preview {
  display: flex;
  gap: 8px;
  margin-top: 12px;
  justify-content: center;
}

.harmony-color {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  border: 2px solid white;
}

.harmony-color:hover {
  transform: scale(1.1);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);
  border-color: #3498db;
}

.color-slider {
  -webkit-appearance: none;
  width: 100%;
  height: 12px;
  border-radius: 6px;
  outline: none;
  background: linear-gradient(to right, #000, #fff);
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.1);
}

.color-slider:hover {
  transform: scaleY(1.1);
}

.color-slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: white;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.3);
  cursor: pointer;
  border: 3px solid #3498db;
  transition: all 0.2s ease;
}

.color-slider::-webkit-slider-thumb:hover {
  transform: scale(1.1);
  box-shadow: 0 3px 8px rgba(0, 0, 0, 0.4);
}

.color-slider::-moz-range-thumb {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: white;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.3);
  cursor: pointer;
  border: 3px solid #3498db;
  transition: all 0.2s ease;
}

/* 滑块颜色渐变 */
.red-slider {
  background: linear-gradient(to right, #000, #ff0000) !important;
}

.green-slider {
  background: linear-gradient(to right, #000, #00ff00) !important;
}

.blue-slider {
  background: linear-gradient(to right, #000, #0000ff) !important;
}

.hue-slider {
  background: linear-gradient(to right,
  #ff0000 0%, #ffff00 16.66%, #00ff00 33.33%,
  #00ffff 50%, #0000ff 66.66%, #ff00ff 83.33%, #ff0000 100%) !important;
}

.saturation-slider {
  background: linear-gradient(to right,
  hsl(var(--current-hue, 0), 0%, 50%),
  hsl(var(--current-hue, 0), 100%, 50%)) !important;
}

.lightness-slider {
  background: linear-gradient(to right,
  hsl(var(--current-hue, 0), var(--current-saturation, 50%), 0%),
  hsl(var(--current-hue, 0), var(--current-saturation, 50%), 50%),
  hsl(var(--current-hue, 0), var(--current-saturation, 50%), 100%)) !important;
}

.number-input {
  width: 70px;
  padding: 8px 12px;
  border: 2px solid rgba(52, 152, 219, 0.2);
  border-radius: 8px;
  font-size: 14px;
  text-align: center;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(4px);
  transition: all 0.3s ease;
  color: #2c3e50;
  font-weight: 500;
}

.number-input:focus {
  outline: none;
  border-color: #3498db;
  box-shadow: 0 0 0 3px rgba(52, 152, 219, 0.1);
  background: white;
}

/* 新增基础模式样式 */
.input-tips {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}

.tip-text {
  font-size: 12px;
  color: #666;
  font-style: italic;
}

.tip-btn {
  padding: 4px 8px;
  background: linear-gradient(45deg, #3498db, #2980b9);
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 11px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.tip-btn:hover {
  transform: scale(1.05);
  box-shadow: 0 2px 4px rgba(52, 152, 219, 0.3);
}

.rgb-input-wrapper, .hsl-input-wrapper {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.rgb-label, .hsl-label {
  font-size: 11px;
  color: #666;
  font-weight: 500;
  text-align: center;
}

.color-info-display {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  padding: 8px 12px;
  background: rgba(52, 152, 219, 0.05);
  border-radius: 8px;
  border: 1px solid rgba(52, 152, 219, 0.1);
}

.info-label {
  font-size: 12px;
  font-weight: 600;
  color: #2c3e50;
  min-width: 35px;
}

.info-value {
  flex: 1;
  font-family: 'Courier New', monospace;
  font-size: 12px;
  color: #34495e;
  background: rgba(255, 255, 255, 0.8);
  padding: 4px 8px;
  border-radius: 4px;
}

.copy-btn {
  padding: 4px 8px;
  background: #27ae60;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.copy-btn:hover {
  background: #229954;
  transform: scale(1.05);
}

.color-analysis {
  margin-top: 20px;
  padding: 16px;
  background: rgba(155, 89, 182, 0.05);
  border-radius: 12px;
  border: 1px solid rgba(155, 89, 182, 0.1);
}

.color-analysis h4 {
  margin: 0 0 12px;
  font-size: 14px;
  color: #8e44ad;
  font-weight: 600;
}

.analysis-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.analysis-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.analysis-label {
  font-size: 12px;
  color: #666;
  font-weight: 500;
}

.analysis-value {
  font-size: 12px;
  color: #2c3e50;
  font-weight: 600;
}

.quick-adjustments {
  margin-top: 20px;
  padding: 16px;
  background: rgba(230, 126, 34, 0.05);
  border-radius: 12px;
  border: 1px solid rgba(230, 126, 34, 0.1);
}

.quick-adjustments h4 {
  margin: 0 0 12px;
  font-size: 14px;
  color: #e67e22;
  font-weight: 600;
}

.adjustment-buttons {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 8px;
}

.adjust-btn {
  padding: 8px 12px;
  border: none;
  border-radius: 8px;
  font-size: 11px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  color: white;
}

.adjust-btn.bright {
  background: linear-gradient(45deg, #f39c12, #e67e22);
}

.adjust-btn.dark {
  background: linear-gradient(45deg, #34495e, #2c3e50);
}

.adjust-btn.saturate {
  background: linear-gradient(45deg, #e74c3c, #c0392b);
}

.adjust-btn.desaturate {
  background: linear-gradient(45deg, #95a5a6, #7f8c8d);
}

.adjust-btn.hue-shift {
  background: linear-gradient(45deg, #9b59b6, #8e44ad);
}

.adjust-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 3px 6px rgba(0, 0, 0, 0.2);
}

.color-converter {
  margin-top: 20px;
  padding: 16px;
  background: rgba(52, 152, 219, 0.05);
  border-radius: 12px;
  border: 1px solid rgba(52, 152, 219, 0.1);
}

.color-converter h4 {
  margin: 0 0 12px;
  font-size: 14px;
  color: #3498db;
  font-weight: 600;
}

.converter-options {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-bottom: 12px;
}

.convert-btn {
  padding: 8px 12px;
  background: linear-gradient(45deg, #3498db, #2980b9);
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 11px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.convert-btn:hover {
  transform: scale(1.02);
  box-shadow: 0 2px 4px rgba(52, 152, 219, 0.3);
}

.converted-result {
  display: flex;
  gap: 8px;
  align-items: flex-start;
}

.converted-text {
  flex: 1;
  min-height: 60px;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-family: 'Courier New', monospace;
  font-size: 11px;
  resize: vertical;
  background: rgba(255, 255, 255, 0.9);
}

/* 拾色器模式 */
.picker-mode {
  display: flex;
  flex-direction: column;
  gap: 20px;
  align-items: center;
  min-height: 280px;
  height: auto;
  justify-content: center;
}

.color-wheel-container {
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
}

.color-wheel {
  cursor: crosshair;
  border-radius: 50%;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
  transition: all 0.3s ease;
}

.color-wheel:hover {
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.2);
  transform: scale(1.02);
}

.saturation-lightness-container {
  display: flex;
  justify-content: center;
  padding: 20px;
}

.saturation-lightness {
  cursor: crosshair;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
  transition: all 0.3s ease;
}

.saturation-lightness:hover {
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.2);
  transform: scale(1.02);
}

/* 颜色控制台样式 */
.color-control-panel {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.3);
  backdrop-filter: blur(12px);
}

.color-control-panel h4 {
  margin: 0 0 20px;
  font-size: 18px;
  color: #2c3e50;
  font-weight: 600;
  text-align: center;
  border-bottom: 2px solid #e8f4fd;
  padding-bottom: 8px;
}

/* 左右布局样式 */
.control-panel-layout {
  display: grid;
  grid-template-columns: 250px 1fr 250px;
  gap: 24px;
  align-items: start;
}

/* 左侧预览区域 */
.control-panel-left {
  display: flex;
  flex-direction: column;
  gap: 16px;
  position: sticky;
  top: 20px;
}

/* 控制台颜色预览 */
.control-panel-preview {
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

.control-preview-color {
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;
  background-image:
    linear-gradient(45deg, rgba(255,255,255,0.1) 25%, transparent 25%),
    linear-gradient(-45deg, rgba(255,255,255,0.1) 25%, transparent 25%),
    linear-gradient(45deg, transparent 75%, rgba(255,255,255,0.1) 75%),
    linear-gradient(-45deg, transparent 75%, rgba(255,255,255,0.1) 75%);
  background-size: 20px 20px;
  background-position: 0 0, 0 10px, 10px -10px, -10px 0px;
}

.control-preview-color:hover {
  transform: scale(1.02);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.15);
}

.preview-info {
  text-align: center;
  background: rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(8px);
  padding: 8px 16px;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.current-format {
  font-family: 'Courier New', monospace;
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 2px;
}

.alpha-display {
  font-size: 11px;
  opacity: 0.8;
}

/* 颜色信息面板 */
.color-info-panel {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

.info-item:last-child {
  border-bottom: none;
}

.info-label {
  font-size: 12px;
  font-weight: 600;
  color: #666;
  min-width: 40px;
}

.info-value {
  font-family: 'Courier New', monospace;
  font-size: 12px;
  color: #2c3e50;
  cursor: pointer;
  padding: 2px 8px;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.info-value:hover {
  background: rgba(52, 152, 219, 0.1);
  color: #3498db;
}

/* 中间控制区域 */
.control-panel-center {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 右侧辅助功能区域 */
.control-panel-right {
  display: flex;
  flex-direction: column;
  gap: 16px;
  position: sticky;
  top: 20px;
}

/* 快速历史 */
.quick-history {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.quick-history h5 {
  margin: 0 0 12px;
  font-size: 14px;
  color: #2c3e50;
  font-weight: 600;
}

.quick-history-colors {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.quick-history-color {
  aspect-ratio: 1;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  border: 2px solid transparent;
}

.quick-history-color:hover {
  transform: scale(1.1);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
  border-color: #3498db;
}

/* 分析面板 */
.analysis-panel {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.analysis-panel h5 {
  margin: 0 0 12px;
  font-size: 14px;
  color: #2c3e50;
  font-weight: 600;
}

.analysis-items {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.analysis-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 8px;
  background: rgba(52, 152, 219, 0.05);
  border-radius: 6px;
}

.analysis-label {
  font-size: 12px;
  color: #666;
  font-weight: 500;
}

.analysis-value {
  font-size: 12px;
  color: #2c3e50;
  font-weight: 600;
}

.control-section {
  margin-bottom: 20px;
  padding: 16px;
  background: rgba(248, 251, 255, 0.8);
  border-radius: 12px;
  border: 1px solid rgba(52, 152, 219, 0.1);
}

.control-label {
  font-size: 14px;
  font-weight: 600;
  color: #34495e;
  margin-bottom: 12px;
  text-align: center;
}

.control-buttons {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.control-btn {
  background: linear-gradient(145deg, #f8fafc, #e2e8f0);
  border: 2px solid #cbd5e0;
  border-radius: 10px;
  padding: 8px 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 11px;
  font-weight: 500;
  color: #4a5568;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  min-width: 50px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.control-btn:hover {
  background: linear-gradient(145deg, #e2e8f0, #cbd5e0);
  border-color: #3498db;
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(52, 152, 219, 0.2);
}

.control-btn:active {
  transform: translateY(0);
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.1);
}

.control-btn.decrease {
  border-color: #e74c3c;
  color: #c0392b;
}

.control-btn.decrease:hover {
  background: linear-gradient(145deg, #fdf2f2, #fed7d7);
  border-color: #e74c3c;
  box-shadow: 0 4px 8px rgba(231, 76, 60, 0.2);
}

.control-btn.increase {
  border-color: #27ae60;
  color: #229954;
}

.control-btn.increase:hover {
  background: linear-gradient(145deg, #f0fff4, #d4edda);
  border-color: #27ae60;
  box-shadow: 0 4px 8px rgba(39, 174, 96, 0.2);
}

.control-btn .icon {
  font-size: 14px;
  font-weight: bold;
}

.control-btn .text {
  font-size: 10px;
  opacity: 0.8;
}

.control-display {
  background: linear-gradient(145deg, #2c3e50, #34495e);
  color: white;
  border-radius: 12px;
  padding: 12px 16px;
  text-align: center;
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.2);
  min-width: 80px;
}

.control-display .value {
  display: block;
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 2px;
}

.control-display .label {
  display: block;
  font-size: 11px;
  opacity: 0.8;
}

/* 特殊操作按钮 */
.special-ops {
  background: rgba(52, 152, 219, 0.05);
  border-color: rgba(52, 152, 219, 0.2);
}

.special-buttons {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.special-btn {
  background: linear-gradient(145deg, #3498db, #2980b9);
  border: none;
  border-radius: 12px;
  padding: 12px 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  color: white;
  font-weight: 500;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  box-shadow: 0 4px 8px rgba(52, 152, 219, 0.3);
}

.special-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 12px rgba(52, 152, 219, 0.4);
}

.special-btn:active {
  transform: translateY(0);
}

.special-btn.invert {
  background: linear-gradient(145deg, #9b59b6, #8e44ad);
  box-shadow: 0 4px 8px rgba(155, 89, 182, 0.3);
}

.special-btn.complement {
  background: linear-gradient(145deg, #e67e22, #d35400);
  box-shadow: 0 4px 8px rgba(230, 126, 34, 0.3);
}

.special-btn.random {
  background: linear-gradient(145deg, #1abc9c, #16a085);
  box-shadow: 0 4px 8px rgba(26, 188, 156, 0.3);
}

.special-btn.reset {
  background: linear-gradient(145deg, #95a5a6, #7f8c8d);
  box-shadow: 0 4px 8px rgba(149, 165, 166, 0.3);
}

.special-btn .icon {
  font-size: 18px;
}

.special-btn .text {
  font-size: 12px;
}

/* 透明度控制 */
.alpha-section {
  background: rgba(106, 90, 205, 0.05);
  border-color: rgba(106, 90, 205, 0.2);
}

.alpha-slider-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.alpha-slider-control {
  -webkit-appearance: none;
  width: 100%;
  height: 8px;
  border-radius: 6px;
  background: linear-gradient(to right,
  rgba(0, 0, 0, 0) 0%,
  rgba(0, 0, 0, 1) 100%);
  outline: none;
  box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.1);
}

.alpha-slider-control::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: white;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.3);
  cursor: pointer;
  border: 2px solid #6a5acd;
}

.alpha-buttons {
  display: flex;
  gap: 8px;
  justify-content: space-between;
}

.alpha-btn {
  background: linear-gradient(145deg, #6a5acd, #5a4fcf);
  border: none;
  border-radius: 8px;
  padding: 6px 12px;
  color: white;
  font-size: 11px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  flex: 1;
}

/* 底部面板 - 历史记录和预设面板左右并列 */
.bottom-panel {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  margin-top: 16px;
}

.history-section, .palette-section {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(8px);
  transition: all 0.3s ease;
}

.history-section:hover, .palette-section:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}

.history-section h4, .palette-section h4 {
  margin: 0 0 16px;
  font-size: 16px;
  color: #2c3e50;
  font-weight: 600;
  border-bottom: 2px solid #e8f4fd;
  padding-bottom: 8px;
}

.op-btn {
  padding: 8px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  background: white;
  cursor: pointer;
  transition: all 0.3s;
  font-size: 12px;
  color: #666;
  font-weight: 500;
  position: relative;
  overflow: hidden;
}

.op-btn:hover {
  background: #f0f8ff;
  border-color: #1890ff;
  color: #1890ff;
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(24, 144, 255, 0.15);
}

.color-control{
  border-radius: 15px;
}

.op-btn:active {
  transform: translateY(0);
  box-shadow: 0 2px 4px rgba(24, 144, 255, 0.15);
}

.alpha-control {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.alpha-control label {
  font-size: 13px;
  color: #666;
  font-weight: 500;
}

.alpha-slider {
  -webkit-appearance: none;
  width: 100%;
  height: 6px;
  border-radius: 3px;
  background: linear-gradient(to right, transparent, #000);
  outline: none;
}

.alpha-slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
  cursor: pointer;
  border: 2px solid #ddd;
}

.history-colors, .palette-colors {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 8px;
}

.history-color, .palette-color {
  aspect-ratio: 1;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  border: 2px solid transparent;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.history-color:hover, .palette-color:hover {
  transform: scale(1.1);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
  border-color: #1890ff;
  z-index: 10;
}

.color-info {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.7);
  opacity: 0;
  transition: opacity 0.3s ease;
  padding: 2px;
}

.history-color:hover .color-info,
.palette-color:hover .color-info {
  opacity: 1;
}

.color-text {
  color: white;
  font-size: 10px;
  font-weight: 500;
  text-align: center;
  line-height: 1.1;
  font-family: 'Courier New', monospace;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
}

.history-color.empty {
  background: #f5f5f5;
  border: 2px dashed #ddd;
  cursor: default;
  display: flex;
  align-items: center;
  justify-content: center;
}

.history-color.empty:hover {
  transform: none;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  border-color: #ddd;
  z-index: auto;
}

.empty-text {
  color: #999;
  font-size: 18px;
  font-weight: 300;
}

.palette-tabs {
  display: flex;
  gap: 4px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.palette-tab {
  padding: 4px 8px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  background: white;
  cursor: pointer;
  transition: all 0.3s;
  font-size: 11px;
  color: #666;
}

.palette-tab.active {
  background: #1890ff;
  color: white;
  border-color: #1890ff;
}

.palette-tab:hover:not(.active) {
  background: #f5f5f5;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .color-picker-page {
    padding: 8px;
    height: 100vh;
    margin: 0;
    background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  }

  .color-picker-container {
    height: 100%;
    padding: 12px;
    border-radius: 16px;
    gap: 16px;
    max-width: 100%;
    overflow-y: auto;
  }

  .mode-switcher {
    margin-bottom: 8px;
  }

  .mode-tabs {
    width: 100%;
    padding: 2px;
  }

  .mode-tab {
    padding: 8px 12px;
    font-size: 12px;
    flex: 1;
    min-width: 0;
  }

  .main-content {
    grid-template-columns: 1fr;
    gap: 16px;
    margin-bottom: 16px;
  }

  .control-panel-section {
    margin: 16px 0;
  }

  .bottom-panel {
    grid-template-columns: 1fr;
    gap: 16px;
    margin-top: 16px;
  }

  .preview-section {
    gap: 16px;
    height: auto;
  }

  .color-preview {
    min-height: 200px;
    aspect-ratio: 1.8;
    border-radius: 16px;
    flex: none;
  }

  .color-values {
    opacity: 1;
    font-size: 12px;
  }

  .format-switcher {
    flex-wrap: wrap;
    gap: 4px;
    margin-bottom: 8px;
  }

  .format-btn {
    font-size: 10px;
    padding: 4px 8px;
    flex: 1;
    min-width: 0;
  }

  .controls-section {
    gap: 16px;
    height: auto;
  }

  .control-group {
    padding: 16px;
    border-radius: 12px;
    min-height: auto;
  }

  .control-group h3 {
    font-size: 16px;
    margin-bottom: 12px;
  }

  .basic-mode, .slider-mode, .picker-mode {
    min-height: auto;
  }

  .input-group {
    gap: 16px;
  }

  .color-input {
    padding: 10px 12px;
    font-size: 13px;
  }

  .rgb-inputs, .hsl-inputs {
    gap: 8px;
  }

  .rgb-input, .hsl-input {
    padding: 10px 8px;
    font-size: 12px;
  }

  .slider-mode {
    min-height: auto;
  }

  .slider-tabs {
    margin-bottom: 16px;
    gap: 8px;
  }

  .slider-tab {
    padding: 8px 12px;
    font-size: 12px;
  }

  .slider-group {
    gap: 16px;
  }

  .slider-item {
    grid-template-columns: 35px 1fr 65px;
    gap: 12px;
    padding: 8px 0;
  }

  .label {
    font-size: 13px;
    padding: 6px 4px;
  }

  .color-slider {
    height: 8px;
  }

  .color-slider::-webkit-slider-thumb {
    width: 20px;
    height: 20px;
  }

  .color-slider::-moz-range-thumb {
    width: 20px;
    height: 20px;
  }

  .number-input {
    width: 55px;
    padding: 6px 8px;
    font-size: 12px;
  }

  .color-wheel, .saturation-lightness {
    width: 160px !important;
    height: 160px !important;
  }

  .picker-mode {
    gap: 16px;
    min-height: auto;
  }

  .bottom-panel {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .color-control-panel {
    padding: 16px;
    border-radius: 16px;
  }

  .control-section {
    margin-bottom: 16px;
    padding: 12px;
  }

  .control-buttons {
    gap: 6px;
  }

  .control-btn {
    padding: 6px 8px;
    min-width: 40px;
    font-size: 10px;
  }

  .control-btn .icon {
    font-size: 12px;
  }

  .control-btn .text {
    font-size: 9px;
  }

  .control-display {
    padding: 8px 12px;
    min-width: 60px;
  }

  .control-display .value {
    font-size: 14px;
  }

  .control-display .label {
    font-size: 10px;
  }

  .special-buttons {
    grid-template-columns: 1fr 1fr;
    gap: 8px;
  }

  .special-btn {
    padding: 10px 12px;
  }

  .special-btn .icon {
    font-size: 16px;
  }

  .special-btn .text {
    font-size: 11px;
  }

  .alpha-buttons {
    gap: 6px;
  }

  .alpha-btn {
    padding: 5px 8px;
    font-size: 10px;
  }

  .history-colors, .palette-colors {
    grid-template-columns: repeat(5, 1fr);
    gap: 8px;
  }

  .palette-tabs {
    gap: 4px;
    flex-wrap: wrap;
  }

  .palette-tab {
    padding: 6px 8px;
    font-size: 10px;
    flex: 1;
    min-width: 0;
  }

  .color-info {
    padding: 2px;
  }

  .color-text {
    font-size: 8px;
    line-height: 1;
  }

  .alpha-control {
    gap: 8px;
  }

  .alpha-control label {
    font-size: 12px;
  }

  .alpha-slider {
    height: 8px;
  }

  .color-contrast {
    padding: 12px;
    border-radius: 10px;
    margin-top: 12px;
  }

  .contrast-colors {
    gap: 8px;
    margin-bottom: 8px;
  }

  .color-item {
    aspect-ratio: 2.5;
    border-radius: 6px;
    font-size: 11px;
  }

  .select-reference-btn {
    padding: 8px;
    font-size: 11px;
  }
}

@media (max-width: 480px) {
  .color-picker-page {
    padding: 4px;
  }

  .color-picker-container {
    padding: 8px;
    border-radius: 12px;
    gap: 12px;
  }

  .mode-tab {
    padding: 6px 8px;
    font-size: 11px;
  }

  .main-content {
    gap: 12px;
  }

  .preview-section {
    gap: 12px;
  }

  .color-preview {
    min-height: 160px;
    border-radius: 12px;
  }

  .control-group {
    padding: 12px;
    border-radius: 10px;
  }

  .control-group h3 {
    font-size: 14px;
    margin-bottom: 10px;
  }

  .color-control-panel {
    padding: 12px;
    border-radius: 12px;
  }

  .color-control-panel h4 {
    font-size: 16px;
    margin-bottom: 12px;
  }

  .control-section {
    margin-bottom: 12px;
    padding: 10px;
  }

  .control-label {
    font-size: 12px;
    margin-bottom: 8px;
  }

  .control-btn {
    padding: 5px 6px;
    min-width: 35px;
  }

  .control-btn .icon {
    font-size: 11px;
  }

  .control-btn .text {
    font-size: 8px;
  }

  .control-display {
    padding: 6px 8px;
    min-width: 50px;
  }

  .control-display .value {
    font-size: 12px;
  }

  .control-display .label {
    font-size: 9px;
  }

  .special-buttons {
    gap: 6px;
  }

  .special-btn {
    padding: 8px 10px;
  }

  .special-btn .icon {
    font-size: 14px;
  }

  .special-btn .text {
    font-size: 10px;
  }

  .alpha-btn {
    padding: 4px 6px;
    font-size: 9px;
  }

  .input-group {
    gap: 12px;
  }

  .color-input {
    padding: 8px 10px;
    font-size: 12px;
  }

  .rgb-inputs, .hsl-inputs {
    gap: 6px;
  }

  .rgb-input, .hsl-input {
    padding: 8px 6px;
    font-size: 11px;
  }

  .slider-group {
    gap: 12px;
  }

  .slider-item {
    grid-template-columns: 30px 1fr 50px;
    gap: 8px;
  }

  .label {
    font-size: 12px;
    padding: 4px 2px;
  }

  .number-input {
    width: 45px;
    padding: 4px 6px;
    font-size: 11px;
  }

  .color-wheel, .saturation-lightness {
    width: 140px !important;
    height: 140px !important;
  }

  .bottom-panel {
    gap: 12px;
  }

  .history-section, .palette-section {
    padding: 12px;
  }

  .history-colors, .palette-colors {
    grid-template-columns: repeat(4, 1fr);
    gap: 6px;
  }

  .palette-tab {
    padding: 4px 6px;
    font-size: 9px;
  }

  .color-text {
    font-size: 7px;
  }
}
</style>
