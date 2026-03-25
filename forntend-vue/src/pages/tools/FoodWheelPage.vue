<template>
  <div class="food-wheel-app">
    <!-- 背景装饰 -->
    <div class="bg-decoration">
      <div class="circle circle-1"></div>
      <div class="circle circle-2"></div>
      <div class="circle circle-3"></div>
    </div>

    <!-- 顶部导航 -->
    <header class="app-header">
      <h1 class="app-title">
        <span>🍽️</span> 美食转盘
        <span class="title-sub">纠结吃什么？交给运气！</span>
      </h1>
    </header>

    <!-- 主内容区 -->
    <main class="app-main">
      <!-- 左侧转盘区 -->
      <section class="wheel-panel">
        <div class="panel-card">
          <!-- 转盘容器 -->
          <div class="wheel-container">
            <div ref="chartRef" class="wheel-chart"></div>
            <!-- 修复箭头方向：改为向下指向转盘（原向上反了） -->
            <div class="wheel-pointer"></div>
            <!-- 旋转中提示 -->
            <div v-if="isSpinning" class="spinning-indicator">
              <span class="spinning-text">转动中...</span>
              <div class="spinner"></div>
            </div>
            <!-- 实时指向提示 -->
            <div v-if="isSpinning && currentFood" class="current-food-tooltip">
              {{ currentFood.name }}
            </div>
          </div>

          <!-- 转盘控制 -->
          <div class="wheel-controls">
            <button
              class="spin-btn"
              @click="spinWheel"
              :disabled="isSpinning || displayFoods.length < 2"
            >
              {{ isSpinning ? '转动中...' : '开始转盘' }}
            </button>
            <p v-if="displayFoods.length < 2" class="empty-tip">
              {{ displayFoods.length === 0 ? '暂无食物，请先添加' : '至少需要2种食物才能转盘' }}
            </p>
          </div>

          <!-- 结果展示 -->
          <div v-if="selectedFood" class="result-card">
            <div class="result-header">
              <h3>🎉 本次推荐</h3>
              <button class="close-result" @click="selectedFood = null">✕</button>
            </div>
            <div class="food-detail">
              <span class="food-category">{{ selectedFood.category }}</span>
              <h2 class="food-name">{{ selectedFood.name }}</h2>
              <p class="food-desc">{{ selectedFood.description || '暂无描述' }}</p>
            </div>
          </div>
        </div>
      </section>

      <!-- 右侧管理区 -->
      <section class="manage-panel">
        <!-- 食物管理卡片 -->
        <div class="panel-card manage-card">
          <div class="card-header">
            <h2 class="card-title">🥢 食物管理</h2>
            <button class="add-btn" @click="showAddForm = true">+ 添加食物</button>
          </div>

          <!-- 分类筛选 -->
          <div class="category-filter">
            <label class="filter-label">分类筛选：</label>
            <select v-model="selectedCategory" class="category-select">
              <option value="">全部分类</option>
              <option v-for="category in categories" :key="category" :value="category">
                {{ category }}
              </option>
            </select>
            <button class="reset-filter" @click="selectedCategory = ''" v-if="selectedCategory">
              重置
            </button>
          </div>

          <!-- 食物列表 -->
          <div class="food-list-container">
            <div v-if="filteredFoods.length === 0" class="empty-list">
              <p>暂无{{ selectedCategory || '食物' }}数据</p>
              <button class="add-empty-btn" @click="showAddForm = true">添加第一个食物</button>
            </div>

            <div class="food-list">
              <div
                v-for="food in filteredFoods"
                :key="food.id"
                class="food-item"
                :class="{ editing: editingFood?.id === food.id }"
              >
                <!-- 查看模式 -->
                <div class="food-view" v-if="editingFood?.id !== food.id">
                  <div class="food-info">
                    <span class="food-tag">{{ food.category }}</span>
                    <h3 class="item-food-name">{{ food.name }}</h3>
                    <p class="item-food-desc">{{ food.description || '暂无描述' }}</p>
                  </div>
                  <div class="food-actions">
                    <button class="edit-btn" @click="startEdit(food)">✏️ 编辑</button>
                    <button class="delete-btn" @click="deleteFood(food.id)">🗑️ 删除</button>
                  </div>
                </div>

                <!-- 编辑模式 -->
                <div class="food-edit" v-else>
                  <input
                    v-model="editingFood.name"
                    placeholder="食物名称"
                    class="edit-input"
                    required
                  >
                  <select
                    v-model="editingFood.category"
                    class="edit-select"
                    required
                  >
                    <option value="">选择分类</option>
                    <option v-for="category in categories" :key="category" :value="category">
                      {{ category }}
                    </option>
                  </select>
                  <input
                    v-model="editingFood.description"
                    placeholder="食物描述（选填）"
                    class="edit-input desc-input"
                  >
                  <div class="edit-actions">
                    <button class="save-edit" @click="saveEdit">✅ 保存</button>
                    <button class="cancel-edit" @click="cancelEdit">❌ 取消</button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 历史记录卡片 -->
        <div class="panel-card history-card">
          <div class="card-header">
            <h2 class="card-title">📜 历史记录</h2>
            <button
              class="clear-history"
              @click="clearHistory"
              v-if="history.length > 0"
            >
              清空
            </button>
          </div>

          <div class="history-list">
            <div v-if="history.length === 0" class="empty-history">
              <p>暂无历史记录</p>
            </div>
            <div
              v-for="(record, index) in history"
              :key="index"
              class="history-item"
            >
              <div class="history-food">
                <span class="history-category">{{ record.food.category }}</span>
                <span class="history-name">{{ record.food.name }}</span>
              </div>
              <span class="history-time">{{ formatTime(record.timestamp) }}</span>
            </div>
          </div>
        </div>
      </section>
    </main>

    <!-- 添加食物弹窗 -->
    <div class="modal-overlay" v-if="showAddForm" @click="showAddForm = false">
      <div class="add-modal" @click.stop>
        <div class="modal-header">
          <h3 class="modal-title">添加新食物</h3>
          <button class="close-modal" @click="showAddForm = false">✕</button>
        </div>
        <form class="add-form" @submit.prevent="addFood">
          <div class="form-group">
            <label class="form-label">食物名称 *</label>
            <input
              v-model="newFood.name"
              type="text"
              class="form-input"
              placeholder="例如：红烧肉"
              required
            >
          </div>
          <div class="form-group">
            <label class="form-label">分类 *</label>
            <select
              v-model="newFood.category"
              class="form-select"
              required
            >
              <option value="">请选择分类</option>
              <option v-for="category in categories" :key="category" :value="category">
                {{ category }}
              </option>
              <option value="自定义分类">+ 自定义分类</option>
            </select>
            <input
              v-if="newFood.category === '自定义分类'"
              v-model="customCategory"
              type="text"
              class="form-input custom-category"
              placeholder="输入自定义分类名称"
            >
          </div>
          <div class="form-group">
            <label class="form-label">描述（选填）</label>
            <input
              v-model="newFood.description"
              type="text"
              class="form-input"
              placeholder="例如：经典红烧肉，香而不腻"
            >
          </div>
          <div class="form-actions">
            <button type="button" class="cancel-btn" @click="showAddForm = false">取消</button>
            <button type="submit" class="confirm-btn" :disabled="!validateAddForm()">添加</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import * as echarts from 'echarts'

// 类型定义
interface Food {
  id: number
  name: string
  category: string
  description: string
}

interface HistoryRecord {
  food: Food
  timestamp: number
}

// 默认食物数据
const defaultFoods: Food[] = [
  { id: 1, name: '红烧肉', category: '中餐', description: '经典红烧肉，香而不腻' },
  { id: 2, name: '清蒸鱼', category: '中餐', description: '新鲜美味的清蒸鱼' },
  { id: 3, name: '麻婆豆腐', category: '中餐', description: '麻辣鲜香的麻婆豆腐' },
  { id: 4, name: '披萨', category: '西餐', description: '香脆可口的披萨' },
  { id: 5, name: '牛排', category: '西餐', description: '鲜嫩多汁的牛排' },
  { id: 6, name: '汉堡', category: '快餐', description: '美味多汁的汉堡' },
  { id: 7, name: '寿司', category: '日料', description: '新鲜美味的寿司' },
  { id: 8, name: '烤肉', category: '韩餐', description: '香嫩多汁的烤肉' },
  { id: 9, name: '火锅', category: '中餐', description: '暖心美味的火锅' },
  { id: 10, name: '炸鸡', category: '快餐', description: '外酥内嫩的炸鸡' }
]

// 响应式状态
const foods = ref<Food[]>([])
const selectedCategory = ref('')
const showAddForm = ref(false)
const editingFood = ref<Food | null>(null)
const selectedFood = ref<Food | null>(null)
const history = ref<HistoryRecord[]>([])
const isSpinning = ref(false)
const chartRef = ref<HTMLElement | null>(null)
const currentFood = ref<Food | null>(null)
const customCategory = ref('')

// 新食物表单
const newFood = ref({
  name: '',
  category: '',
  description: ''
})

// ECharts实例
let chart: echarts.ECharts | null = null

// 计算属性
const categories = computed(() => {
  const uniqueCategories = new Set(foods.value.map(food => food.category))
  return Array.from(uniqueCategories).sort()
})

const filteredFoods = computed(() => {
  return selectedCategory.value
    ? foods.value.filter(food => food.category === selectedCategory.value)
    : foods.value
})

const displayFoods = computed(() => {
  // 去重并返回
  return Array.from(new Map(foods.value.map(food => [food.name, food])).values())
})

// 方法 - 数据持久化
const loadFoods = () => {
  const savedFoods = localStorage.getItem('foods')
  foods.value = savedFoods ? JSON.parse(savedFoods) : defaultFoods
}

const loadHistory = () => {
  const savedHistory = localStorage.getItem('foodHistory')
  history.value = savedHistory ? JSON.parse(savedHistory) : []
}

const saveFoods = () => {
  localStorage.setItem('foods', JSON.stringify(foods.value))
}

const saveHistory = () => {
  localStorage.setItem('foodHistory', JSON.stringify(history.value))
}

// 方法 - 表单验证
const validateAddForm = () => {
  const category = newFood.value.category === '自定义分类'
    ? customCategory.value
    : newFood.value.category

  return newFood.value.name.trim() && category.trim()
}

// 方法 - 食物管理
const addFood = () => {
  const category = newFood.value.category === '自定义分类'
    ? customCategory.value.trim()
    : newFood.value.category.trim()

  if (!newFood.value.name.trim() || !category) return

  const food: Food = {
    id: Date.now(),
    name: newFood.value.name.trim(),
    category,
    description: newFood.value.description.trim()
  }

  foods.value.push(food)
  saveFoods()

  // 重置表单
  showAddForm.value = false
  newFood.value = { name: '', category: '', description: '' }
  customCategory.value = ''
}

const deleteFood = (id: number) => {
  if (confirm('确定要删除这个食物吗？')) {
    foods.value = foods.value.filter(food => food.id !== id)
    saveFoods()
  }
}

const startEdit = (food: Food) => {
  editingFood.value = { ...food }
}

const saveEdit = () => {
  if (!editingFood.value || !editingFood.value.name.trim()) return

  const index = foods.value.findIndex(food => food.id === editingFood.value?.id)
  if (index !== -1) {
    foods.value[index] = editingFood.value
    saveFoods()
  }
  editingFood.value = null
}

const cancelEdit = () => {
  editingFood.value = null
}

// 方法 - 历史记录
const clearHistory = () => {
  if (confirm('确定要清空历史记录吗？')) {
    history.value = []
    saveHistory()
  }
}

const formatTime = (timestamp: number) => {
  const date = new Date(timestamp)
  return new Intl.DateTimeFormat('zh-CN', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  }).format(date)
}

// 方法 - 转盘逻辑
const initChart = () => {
  if (!chartRef.value) return

  // 销毁旧实例
  if (chart) chart.dispose()

  chart = echarts.init(chartRef.value)
  updateChartOptions()
}

const updateChartOptions = () => {
  if (!chart || displayFoods.value.length === 0) return

  const foodList = displayFoods.value
  const total = foodList.length
  if (total === 0) return

  // 生成渐变色
  const colors = foodList.map((_, index) => {
    const hue = (360 / total) * index
    return `hsla(${hue}, 80%, 65%, 0.9)`
  })

  // 生成数据项
  const data = foodList.map((food, index) => ({
    value: 1,
    name: food.name,
    itemStyle: {
      color: colors[index],
      borderRadius: [8, 8, 8, 8]
    },
    label: {
      show: true,
      position: 'outside',
      fontSize: 12,
      fontWeight: 500,
      color: '#333',
      formatter: `{b}`
    }
  }))

  const option = {
    backgroundColor: 'transparent',
    series: [{
      type: 'pie',
      radius: ['40%', '80%'],
      center: ['50%', '50%'],
      startAngle: 0, // 重置初始角度为0（关键：统一计算基准）
      clockwise: true, // 改为顺时针旋转（符合直觉）
      avoidLabelOverlap: false,
      itemStyle: {
        borderColor: '#fff',
        borderWidth: 2
      },
      labelLine: {
        show: true,
        length: 15,
        length2: 10,
        lineStyle: {
          width: 1,
          color: '#666'
        }
      },
      animation: true,
      animationDurationUpdate: 50, // 每帧过渡时长
      animationEasingUpdate: 'linear', // 线性过渡
      data
    }]
  }

  chart.setOption(option)
}

// 修复后的转盘旋转函数（核心解决指向不一致问题）
const spinWheel = () => {
  if (isSpinning.value || displayFoods.value.length < 2) return

  isSpinning.value = true
  selectedFood.value = null
  currentFood.value = null

  const foodList = displayFoods.value
  const total = foodList.length
  const rounds = 8 + Math.random() * 4 // 8-12圈
  const duration = 4000 + Math.random() * 1000 // 4-5秒

  // 随机选择最终结果
  const targetIndex = Math.floor(Math.random() * total)
  const targetFood = foodList[targetIndex]

  // 核心修复：角度计算逻辑（适配顺时针+指针在顶部）
  const anglePerItem = 360 / total // 每个扇区的角度
  // 目标角度：让指针正好指向目标扇区的中心（顶部为0°）
  const targetAngle = (targetIndex * anglePerItem) + (anglePerItem / 2)
  // 总旋转角度：多圈 + 最终定位角度（360 - targetAngle 让转盘转到目标位置）
  const finalAngle = (rounds * 360) + (360 - targetAngle)

  // 缓动函数：先快后慢
  const easeOutCubic = (t: number) => 1 - Math.pow(1 - t, 3)

  // 动画帧
  let startTime = 0
  const animate = (timestamp: number) => {
    if (!startTime) startTime = timestamp
    const elapsed = timestamp - startTime
    const progress = Math.min(elapsed / duration, 1)
    const easeProgress = easeOutCubic(progress)
    const currentAngle = finalAngle * easeProgress

    // 更新旋转角度（基于初始0°累加）
    chart?.setOption({
      series: [{
        startAngle: currentAngle // 核心：用startAngle驱动旋转
      }]
    }, false) // 保留现有配置，仅更新角度

    // 修复实时指向计算：匹配最终结果的角度逻辑
    const normalizedAngle = currentAngle % 360
    // 计算当前指向的扇区索引（向下取整）
    const currentIndex = Math.floor((360 - normalizedAngle) % 360 / anglePerItem) % total
    currentFood.value = foodList[currentIndex]

    if (progress < 1) {
      requestAnimationFrame(animate)
    } else {
      // 动画结束：强制校准角度（确保指针精准指向目标）
      chart?.setOption({
        series: [{
          startAngle: finalAngle
        }]
      })

      isSpinning.value = false
      selectedFood.value = targetFood
      currentFood.value = null

      // 添加到历史记录
      history.value.unshift({
        food: targetFood,
        timestamp: Date.now()
      })

      // 限制历史记录数量
      if (history.value.length > 20) {
        history.value = history.value.slice(0, 20)
      }

      saveHistory()
    }
  }

  requestAnimationFrame(animate)
}

// 方法 - 窗口适配
const handleResize = () => {
  chart?.resize()
}

// 生命周期
onMounted(() => {
  loadFoods()
  loadHistory()
  initChart()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (chart) chart.dispose()
})

// 监听数据变化
watch([displayFoods, selectedCategory], () => {
  updateChartOptions()
}, { deep: true })

watch(() => newFood.value.category, (newVal) => {
  if (newVal !== '自定义分类') {
    customCategory.value = ''
  }
})
</script>

<style>
/* 全局样式重置 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

:root {
  --primary-color: #ff6b6b;
  --secondary-color: #4ecdc4;
  --accent-color: #ffe066;
  --dark-color: #2d3436;
  --light-color: #f8f9fa;
  --gray-color: #6c757d;
  --white-color: #ffffff;
  --shadow-sm: 0 2px 8px rgba(0, 0, 0, 0.1);
  --shadow-md: 0 4px 16px rgba(0, 0, 0, 0.15);
  --shadow-lg: 0 8px 32px rgba(0, 0, 0, 0.2);
  --radius-sm: 8px;
  --radius-md: 16px;
  --radius-lg: 24px;
  --transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 主体样式 */
body {
  background: linear-gradient(135deg, #f5f7fa 0%, #e4eaf5 100%);
  color: var(--dark-color);
  overflow-x: hidden;
}

.food-wheel-app {
  position: relative;
  padding: 20px 16px;
}

/* 背景装饰 */
.bg-decoration {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: -1;
  overflow: hidden;
}

.circle {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.2;
  animation: float 15s infinite ease-in-out;
}

.circle-1 {
  width: 600px;
  height: 600px;
  background: var(--primary-color);
  top: -200px;
  left: -200px;
}

.circle-2 {
  width: 700px;
  height: 700px;
  background: var(--secondary-color);
  bottom: -300px;
  right: -200px;
  animation-delay: -5s;
}

.circle-3 {
  width: 500px;
  height: 500px;
  background: var(--accent-color);
  top: 40%;
  left: 70%;
  animation-delay: -10s;
}

@keyframes float {
  0%, 100% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(20px, 20px) scale(1.05); }
}

/* 顶部导航 */
.app-header {
  text-align: center;
  margin-bottom: 24px;
  padding: 16px 0;
}

.app-title {
  font-size: 2.2rem;
  font-weight: 700;
  color: var(--dark-color);
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.title-sub {
  font-size: 0.9rem;
  color: var(--gray-color);
  font-weight: 400;
  margin-left: 8px;
}

/* 主内容区 */
.app-main {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  max-width: 1400px;
  margin: 0 auto;
}

/* 通用面板样式 */
.panel-card {
  background: var(--white-color);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
  padding: 24px;
  height: 100%;
  transition: var(--transition);
}

.panel-card:hover {
  box-shadow: var(--shadow-lg);
  transform: translateY(-2px);
}

/* 左侧转盘面板 */
.wheel-panel {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.wheel-container {
  position: relative;
  width: 100%;
  height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24px;
}

.wheel-chart {
  width: 100%;
  height: 100%;
}

/* 转盘指针：修复方向（改为向下指向转盘） */
.wheel-pointer {
  position: absolute;
  top: -10px; /* 调整位置到转盘正上方 */
  left: 50%;
  transform: translateX(-50%);
  width: 0;
  height: 0;
  /* 修复箭头方向：上三角改为下三角，指向转盘 */
  border-left: 16px solid transparent;
  border-right: 16px solid transparent;
  border-top: 32px solid var(--primary-color); /* 原border-bottom改为border-top */
  z-index: 10;
  filter: drop-shadow(0 2px 4px rgba(0,0,0,0.2));
}

/* 旋转提示 */
.spinning-indicator {
  position: absolute;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(255,255,255,0.9);
  padding: 8px 16px;
  border-radius: 20px;
  box-shadow: var(--shadow-sm);
  z-index: 10;
}

.spinning-text {
  font-size: 1rem;
  font-weight: 500;
  color: var(--primary-color);
}

.spinner {
  width: 16px;
  height: 16px;
  border: 2px solid var(--primary-color);
  border-top: 2px solid transparent;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 实时指向提示 */
.current-food-tooltip {
  position: absolute;
  top: 40px;
  left: 50%;
  transform: translateX(-50%);
  background: var(--accent-color);
  color: var(--dark-color);
  padding: 8px 16px;
  border-radius: 20px;
  font-weight: 500;
  font-size: 1rem;
  box-shadow: var(--shadow-sm);
  z-index: 10;
  white-space: nowrap;
  animation: pulse 1s infinite alternate;
}

@keyframes pulse {
  from { transform: translateX(-50%) scale(1); }
  to { transform: translateX(-50%) scale(1.05); }
}

/* 转盘控制 */
.wheel-controls {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.spin-btn {
  padding: 14px 48px;
  font-size: 1.2rem;
  font-weight: 600;
  color: white;
  background: linear-gradient(135deg, var(--primary-color), #ff8e8e);
  border: none;
  border-radius: 50px;
  cursor: pointer;
  transition: var(--transition);
  box-shadow: 0 4px 12px rgba(255, 107, 107, 0.3);
}

.spin-btn:hover:not(:disabled) {
  transform: translateY(-3px);
  box-shadow: 0 6px 16px rgba(255, 107, 107, 0.4);
  background: linear-gradient(135deg, #ff5252, var(--primary-color));
}

.spin-btn:disabled {
  background: var(--gray-color);
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
  opacity: 0.7;
}

.empty-tip {
  color: var(--gray-color);
  font-size: 0.9rem;
  text-align: center;
  margin: 0;
}

/* 结果卡片 */
.result-card {
  background: linear-gradient(135deg, #fdf2f8 0%, #fef7fb 100%);
  border-radius: var(--radius-md);
  padding: 20px;
  margin-top: 24px;
  box-shadow: var(--shadow-sm);
  border: 1px solid #fce4ec;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.result-header h3 {
  color: var(--primary-color);
  font-size: 1.2rem;
  margin: 0;
}

.close-result {
  background: transparent;
  border: none;
  font-size: 1.2rem;
  cursor: pointer;
  color: var(--gray-color);
  transition: var(--transition);
}

.close-result:hover {
  color: var(--primary-color);
  transform: scale(1.1);
}

.food-detail {
  text-align: center;
}

.food-category {
  display: inline-block;
  padding: 4px 12px;
  background: rgba(255, 107, 107, 0.1);
  color: var(--primary-color);
  border-radius: 20px;
  font-size: 0.8rem;
  margin-bottom: 8px;
}

.food-name {
  font-size: 2rem;
  font-weight: 700;
  color: var(--dark-color);
  margin-bottom: 12px;
}

.food-desc {
  color: var(--gray-color);
  font-size: 1rem;
  line-height: 1.6;
  max-width: 400px;
  margin: 0 auto;
}

/* 右侧管理面板 */
.manage-panel {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.manage-card {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.card-title {
  font-size: 1.4rem;
  font-weight: 600;
  color: var(--dark-color);
  display: flex;
  align-items: center;
  gap: 8px;
}

.add-btn {
  padding: 8px 16px;
  background: linear-gradient(135deg, var(--secondary-color), #66d9e8);
  color: white;
  border: none;
  border-radius: var(--radius-sm);
  font-weight: 500;
  cursor: pointer;
  transition: var(--transition);
}

.add-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(78, 205, 196, 0.3);
}

/* 分类筛选 */
.category-filter {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  padding: 12px 0;
  border-bottom: 1px solid #eee;
}

.filter-label {
  font-weight: 500;
  color: var(--gray-color);
}

.category-select {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: var(--radius-sm);
  font-size: 0.9rem;
  color: var(--dark-color);
  background: white;
  cursor: pointer;
}

.reset-filter {
  padding: 6px 12px;
  background: #f5f5f5;
  border: 1px solid #ddd;
  border-radius: var(--radius-sm);
  font-size: 0.85rem;
  cursor: pointer;
  transition: var(--transition);
}

.reset-filter:hover {
  background: #eee;
}

/* 食物列表 */
.food-list-container {
  max-height: 400px;
  overflow-y: auto;
  padding-right: 8px;
}

/* 自定义滚动条 */
.food-list-container::-webkit-scrollbar {
  width: 6px;
}

.food-list-container::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.food-list-container::-webkit-scrollbar-thumb {
  background: #ccc;
  border-radius: 3px;
}

.food-list-container::-webkit-scrollbar-thumb:hover {
  background: #bbb;
}

.empty-list {
  text-align: center;
  padding: 40px 20px;
  color: var(--gray-color);
}

.add-empty-btn {
  margin-top: 16px;
  padding: 8px 16px;
  background: var(--primary-color);
  color: white;
  border: none;
  border-radius: var(--radius-sm);
  cursor: pointer;
}

.food-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.food-item {
  background: #f8f9fa;
  border-radius: var(--radius-md);
  padding: 16px;
  transition: var(--transition);
  border-left: 4px solid transparent;
}

.food-item:hover {
  transform: translateX(4px);
  border-left-color: var(--secondary-color);
  box-shadow: var(--shadow-sm);
}

.food-view {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.food-info {
  flex: 1;
}

.food-tag {
  display: inline-block;
  padding: 4px 8px;
  background: rgba(78, 205, 196, 0.1);
  color: var(--secondary-color);
  border-radius: 4px;
  font-size: 0.75rem;
  margin-bottom: 8px;
}

.item-food-name {
  font-size: 1.1rem;
  font-weight: 600;
  margin-bottom: 4px;
  color: var(--dark-color);
}

.item-food-desc {
  font-size: 0.9rem;
  color: var(--gray-color);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.food-actions {
  display: flex;
  gap: 8px;
}

.edit-btn, .delete-btn {
  padding: 6px 12px;
  border: none;
  border-radius: var(--radius-sm);
  font-size: 0.85rem;
  cursor: pointer;
  transition: var(--transition);
  display: flex;
  align-items: center;
  gap: 4px;
}

.edit-btn {
  background: rgba(78, 205, 196, 0.1);
  color: var(--secondary-color);
}

.edit-btn:hover {
  background: rgba(78, 205, 196, 0.2);
}

.delete-btn {
  background: rgba(255, 107, 107, 0.1);
  color: var(--primary-color);
}

.delete-btn:hover {
  background: rgba(255, 107, 107, 0.2);
}

/* 编辑模式 */
.food-edit {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.edit-input, .edit-select {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: var(--radius-sm);
  font-size: 0.9rem;
}

.desc-input {
  min-height: 60px;
}

.edit-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

.save-edit, .cancel-edit {
  padding: 6px 16px;
  border: none;
  border-radius: var(--radius-sm);
  font-size: 0.9rem;
  cursor: pointer;
  transition: var(--transition);
}

.save-edit {
  background: var(--secondary-color);
  color: white;
}

.cancel-edit {
  background: #eee;
  color: var(--gray-color);
}

/* 历史记录卡片 */
.history-card {
  max-height: 400px;
}

.clear-history {
  padding: 6px 12px;
  background: #f5f5f5;
  border: 1px solid #ddd;
  border-radius: var(--radius-sm);
  font-size: 0.85rem;
  cursor: pointer;
  color: var(--gray-color);
}

.clear-history:hover {
  background: #eee;
}

.history-list {
  max-height: 300px;
  overflow-y: auto;
  padding-right: 8px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.empty-history {
  text-align: center;
  padding: 30px 20px;
  color: var(--gray-color);
}

.history-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  background: #f8f9fa;
  border-radius: var(--radius-sm);
}

.history-food {
  display: flex;
  flex-direction: column;
}

.history-category {
  font-size: 0.75rem;
  color: var(--gray-color);
  margin-bottom: 2px;
}

.history-name {
  font-weight: 500;
  color: var(--dark-color);
}

.history-time {
  font-size: 0.8rem;
  color: var(--gray-color);
  white-space: nowrap;
}

/* 添加食物弹窗 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(4px);
}

.add-modal {
  background: white;
  border-radius: var(--radius-lg);
  width: 90%;
  max-width: 500px;
  padding: 24px;
  box-shadow: var(--shadow-lg);
  animation: modalIn 0.3s ease-out;
}

@keyframes modalIn {
  from {
    opacity: 0;
    transform: translateY(20px) scale(0.98);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.modal-title {
  font-size: 1.4rem;
  font-weight: 600;
  color: var(--dark-color);
}

.close-modal {
  background: transparent;
  border: none;
  font-size: 1.2rem;
  cursor: pointer;
  color: var(--gray-color);
  transition: var(--transition);
}

.close-modal:hover {
  color: var(--primary-color);
  transform: scale(1.1);
}

.add-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-label {
  font-weight: 500;
  color: var(--dark-color);
  font-size: 0.9rem;
}

.form-input, .form-select {
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: var(--radius-sm);
  font-size: 0.9rem;
}

.custom-category {
  margin-top: 8px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 8px;
}

.cancel-btn, .confirm-btn {
  padding: 10px 20px;
  border: none;
  border-radius: var(--radius-sm);
  font-size: 0.9rem;
  font-weight: 500;
  cursor: pointer;
  transition: var(--transition);
}

.cancel-btn {
  background: #f5f5f5;
  color: var(--gray-color);
}

.confirm-btn {
  background: var(--primary-color);
  color: white;
}

.confirm-btn:disabled {
  background: var(--gray-color);
  cursor: not-allowed;
  opacity: 0.7;
}

/* 响应式适配 */
@media (max-width: 1024px) {
  .app-main {
    grid-template-columns: 1fr;
  }

  .wheel-container {
    height: 350px;
  }
}

@media (max-width: 768px) {
  .app-title {
    font-size: 1.8rem;
  }

  .panel-card {
    padding: 16px;
  }

  .wheel-container {
    height: 300px;
  }

  .food-name {
    font-size: 1.6rem;
  }

  .card-title {
    font-size: 1.2rem;
  }
}

@media (max-width: 480px) {
  .food-wheel-app {
    padding: 12px 8px;
  }

  .app-header {
    margin-bottom: 16px;
  }

  .app-title {
    font-size: 1.5rem;
    flex-direction: column;
    gap: 4px;
  }

  .title-sub {
    margin-left: 0;
  }

  .wheel-container {
    height: 250px;
  }

  .spin-btn {
    padding: 12px 32px;
    font-size: 1rem;
  }

  .food-view {
    flex-direction: column;
    align-items: flex-start;
  }

  .food-actions {
    margin-top: 12px;
    width: 100%;
    justify-content: flex-end;
  }

  .history-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .history-time {
    align-self: flex-end;
  }
}
</style>
