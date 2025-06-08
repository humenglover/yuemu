<template>
  <div class="food-wheel-page">
    <div class="food-wheel-container">
      <!-- 转盘区域 -->
      <div class="wheel-section">
        <div class="wheel-wrapper">
          <div ref="chartRef" class="wheel-chart"></div>
          <div v-if="isSpinning && currentFood" class="current-food-indicator">
            {{ currentFood.name }}
          </div>
        </div>
        <div class="wheel-controls">
          <button class="spin-button" @click="spinWheel" :disabled="isSpinning">
            {{ isSpinning ? '转动中...' : '开始' }}
          </button>
        </div>
      </div>

      <!-- 结果展示 -->
      <div class="result-section" v-if="selectedFood">
        <div class="result-card">
          <h3>推荐美食</h3>
          <div class="food-info">
            <h4>{{ selectedFood.name }}</h4>
            <p class="food-category">分类：{{ selectedFood.category }}</p>
            <p class="food-description">{{ selectedFood.description }}</p>
          </div>
        </div>
      </div>

      <!-- 食物管理 -->
      <div class="food-management">
        <div class="management-header">
          <h3>食物管理</h3>
          <button class="add-button" @click="showAddForm = true">添加食物</button>
        </div>

        <!-- 分类筛选 -->
        <div class="category-filter">
          <select v-model="selectedCategory">
            <option value="">全部分类</option>
            <option v-for="category in categories"
                    :key="category"
                    :value="category">
              {{ category }}
            </option>
          </select>
        </div>

        <!-- 食物列表 -->
        <div class="food-list">
          <div v-for="food in filteredFoods"
               :key="food.id"
               class="food-item">
            <div class="food-content" v-if="editingFood?.id !== food.id">
              <div class="food-details">
                <h4>{{ food.name }}</h4>
                <p class="category-tag">{{ food.category }}</p>
                <p class="description">{{ food.description }}</p>
              </div>
              <div class="food-actions">
                <button class="edit-btn" @click="startEdit(food)">编辑</button>
                <button class="delete-btn" @click="deleteFood(food.id)">删除</button>
              </div>
            </div>
            <div class="edit-form" v-else>
              <input v-model="editingFood.name" placeholder="食物名称">
              <select v-model="editingFood.category">
                <option v-for="category in categories"
                        :key="category"
                        :value="category">
                  {{ category }}
                </option>
              </select>
              <input v-model="editingFood.description" placeholder="描述">
              <div class="edit-actions">
                <button class="save-btn" @click="saveEdit">保存</button>
                <button class="cancel-btn" @click="cancelEdit">取消</button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 添加食物表单 -->
      <div class="add-food-modal" v-if="showAddForm">
        <div class="modal-content">
          <h3>添加新食物</h3>
          <input v-model="newFood.name" placeholder="食物名称" required>
          <select v-model="newFood.category" required>
            <option value="">选择分类</option>
            <option v-for="category in categories"
                    :key="category"
                    :value="category">
              {{ category }}
            </option>
          </select>
          <input v-model="newFood.description" placeholder="描述">
          <div class="modal-actions">
            <button class="save-btn" @click="addFood">保存</button>
            <button class="cancel-btn" @click="showAddForm = false">取消</button>
          </div>
        </div>
      </div>

      <!-- 历史记录 -->
      <div class="history-section">
        <h3>历史记录</h3>
        <div class="history-list">
          <div v-for="(record, index) in history"
               :key="index"
               class="history-item">
            <span class="history-food">{{ record.food.name }}</span>
            <span class="history-time">{{ formatTime(record.timestamp) }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import * as echarts from 'echarts'

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
  { id: 4, name: '宫保鸡丁', category: '中餐', description: '经典川菜，口感丰富' },
  { id: 5, name: '水煮牛肉', category: '中餐', description: '麻辣鲜香的水煮牛肉' },
  { id: 6, name: '糖醋排骨', category: '中餐', description: '酸甜可口的糖醋排骨' },
  { id: 7, name: '回锅肉', category: '中餐', description: '香辣可口的回锅肉' },
  { id: 8, name: '鱼香肉丝', category: '中餐', description: '开胃下饭的鱼香肉丝' },
  { id: 9, name: '酸菜鱼', category: '中餐', description: '酸辣开胃的酸菜鱼' },
  { id: 10, name: '辣子鸡', category: '中餐', description: '香辣可口的辣子鸡' },
  { id: 11, name: '东坡肉', category: '中餐', description: '入口即化的东坡肉' },
  { id: 12, name: '蒜蓉粉丝蒸虾', category: '中餐', description: '鲜美可口的蒜蓉粉丝蒸虾' },
  { id: 13, name: '小龙虾', category: '中餐', description: '麻辣鲜香的小龙虾' },
  { id: 14, name: '烤鸭', category: '中餐', description: '香酥可口的烤鸭' },
  { id: 15, name: '酱牛肉', category: '中餐', description: '香浓可口的酱牛肉' },
  { id: 16, name: '意大利面', category: '西餐', description: '美味的意大利面' },
  { id: 17, name: '披萨', category: '西餐', description: '香脆可口的披萨' },
  { id: 18, name: '牛排', category: '西餐', description: '鲜嫩多汁的牛排' },
  { id: 19, name: '烤鸡翅', category: '西餐', description: '香嫩可口的烤鸡翅' },
  { id: 20, name: '三明治', category: '西餐', description: '营养美味的三明治' },
  { id: 21, name: '沙拉', category: '西餐', description: '健康清爽的沙拉' },
  { id: 22, name: '汉堡', category: '快餐', description: '美味多汁的汉堡' },
  { id: 23, name: '炸鸡', category: '快餐', description: '外酥内嫩的炸鸡' },
  { id: 24, name: '薯条', category: '快餐', description: '金黄酥脆的薯条' },
  { id: 25, name: '寿司', category: '日料', description: '新鲜美味的寿司' },
  { id: 26, name: '拉面', category: '日料', description: '热气腾腾的拉面' },
  { id: 27, name: '天妇罗', category: '日料', description: '酥脆可口的天妇罗' },
  { id: 28, name: '刺身', category: '日料', description: '新鲜可口的刺身' },
  { id: 29, name: '烤肉', category: '韩餐', description: '香嫩多汁的烤肉' },
  { id: 30, name: '石锅拌饭', category: '韩餐', description: '美味可口的石锅拌饭' },
  { id: 31, name: '泡菜汤', category: '韩餐', description: '开胃可口的泡菜汤' },
  { id: 32, name: '炒年糕', category: '韩餐', description: '韩式辣炒年糕' },
  { id: 33, name: '火锅', category: '中餐', description: '暖心美味的火锅' },
  { id: 34, name: '烧烤', category: '中餐', description: '香辣可口的烧烤' },
  { id: 35, name: '粥', category: '中餐', description: '暖胃养生的粥' },
  { id: 36, name: '饺子', category: '中餐', description: '美味可口的饺子' },
  { id: 37, name: '包子', category: '中餐', description: '香软可口的包子' },
  { id: 38, name: '炒面', category: '中餐', description: '香喷喷的炒面' },
  { id: 39, name: '炒饭', category: '中餐', description: '美味可口的炒饭' },
  { id: 40, name: '煲仔饭', category: '中餐', description: '香喷喷的煲仔饭' },
  { id: 41, name: '烤冷面', category: '韩餐', description: '美味的烤冷面' },
  { id: 42, name: '章鱼烧', category: '日料', description: '外酥内软的章鱼烧' },
  { id: 43, name: '咖喱饭', category: '日料', description: '香浓可口的咖喱饭' },
  { id: 44, name: '三文鱼', category: '西餐', description: '新鲜美味的三文鱼' },
  { id: 45, name: '春卷', category: '中餐', description: '酥脆可口的春卷' },
  { id: 46, name: '炸酱面', category: '中餐', description: '美味可口的炸酱面' },
  { id: 47, name: '酱香饼', category: '中餐', description: '香酥可口的酱香饼' },
  { id: 48, name: '肉夹馍', category: '中餐', description: '美味可口的肉夹馍' },
  { id: 49, name: '凉皮', category: '中餐', description: '开胃爽口的凉皮' },
  { id: 50, name: '麻辣烫', category: '中餐', description: '麻辣鲜香的麻辣烫' }
]

// 状态管理
const foods = ref<Food[]>([])
const selectedCategory = ref('')
const showAddForm = ref(false)
const editingFood = ref<Food | null>(null)
const selectedFood = ref<Food | null>(null)
const history = ref<HistoryRecord[]>([])
const isSpinning = ref(false)
const chartRef = ref<HTMLElement | null>(null)
let chart: echarts.ECharts | null = null

// 新食物表单
const newFood = ref({
  name: '',
  category: '',
  description: ''
})

// 添加一个新的响应式变量用于实时显示
const currentFood = ref<Food | null>(null)

// 计算属性
const categories = computed(() => {
  const uniqueCategories = new Set(foods.value.map(food => food.category))
  return Array.from(uniqueCategories)
})

const filteredFoods = computed(() => {
  return selectedCategory.value
    ? foods.value.filter(food => food.category === selectedCategory.value)
    : foods.value
})

const displayFoods = computed(() => {
  // 不再限制数量，显示所有不重复的食物
  const uniqueFoods = Array.from(new Map(filteredFoods.value.map(food => [food.name, food])).values())
  return uniqueFoods
})

// 方法
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

const addFood = () => {
  if (!newFood.value.name || !newFood.value.category) return

  const food: Food = {
    id: Date.now(),
    name: newFood.value.name,
    category: newFood.value.category,
    description: newFood.value.description
  }

  foods.value.push(food)
  saveFoods()
  showAddForm.value = false
  newFood.value = { name: '', category: '', description: '' }
}

const deleteFood = (id: number) => {
  foods.value = foods.value.filter(food => food.id !== id)
  saveFoods()
}

const startEdit = (food: Food) => {
  editingFood.value = { ...food }
}

const saveEdit = () => {
  if (!editingFood.value) return

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

const formatTime = (timestamp: number) => {
  const date = new Date(timestamp)
  return date.toLocaleString()
}

// 初始化图表
const initChart = () => {
  if (!chartRef.value) return
  chart = echarts.init(chartRef.value)
  updateChartOptions()
}

// 更新图表配置
const updateChartOptions = () => {
  if (!chart) return

  const foods = displayFoods.value
  const total = foods.length
  const interval = 360 / total

  // 生成颜色数组
  const colors = foods.map((_, index) => {
    const hue = (360 / total) * index
    return `hsla(${hue}, 70%, 85%, 0.8)`
  })

  const option = {
    series: [{
      type: 'gauge',
      startAngle: 90,
      endAngle: -270,
      min: 0,
      max: 360,
      splitNumber: total,
      axisLine: {
        lineStyle: {
          width: 40,
          color: colors.map((color, index) => [
            (index + 1) / total,
            color
          ])
        }
      },
      pointer: {
        icon: 'triangle',
        length: '60%',
        width: 8,
        itemStyle: {
          color: '#345750'
        }
      },
      axisTick: {
        show: false // 隐藏刻度
      },
      splitLine: {
        show: false // 隐藏分隔线
      },
      axisLabel: {
        show: false // 隐藏标签文字
      },
      detail: {
        show: false
      },
      data: [{
        value: 0
      }],
      animation: true,
      animationDurationUpdate: 1500
    }]
  }

  chart.setOption(option)
}

// 更新转盘旋转逻辑
const spinWheel = () => {
  if (isSpinning.value || !chart) return

  isSpinning.value = true
  selectedFood.value = null // 清空之前的结果

  // 设置多圈旋转
  const rounds = 5 // 增加旋转圈数
  const duration = 3000 // 增加动画时长

  // 用于实时更新显示的食物
  const updateCurrentFood = (currentAngle: number) => {
    const normalizedAngle = currentAngle % 360
    const currentIndex = Math.floor((normalizedAngle / 360) * displayFoods.value.length)
    currentFood.value = displayFoods.value[currentIndex]
  }

  // 设置动画
  let startAngle = 0
  const startTime = Date.now()

  const animate = () => {
    const now = Date.now()
    const elapsed = now - startTime
    const progress = Math.min(elapsed / duration, 1)

    // 使用缓动函数使动画更自然
    const easeOut = (t: number) => 1 - Math.pow(1 - t, 3)
    const currentRotation = (rounds * 360) * easeOut(progress)

    // 更新图表
    chart?.setOption({
      series: [{
        data: [{
          value: currentRotation % 360
        }]
      }]
    })

    // 更新当前指向的食物
    updateCurrentFood(currentRotation)

    if (progress < 1) {
      requestAnimationFrame(animate)
    } else {
      // 动画结束后才选择食物
      const selectedIndex = Math.floor(Math.random() * displayFoods.value.length)
      const targetFood = displayFoods.value[selectedIndex]

      // 计算最终角度
      const total = displayFoods.value.length
      const sectorAngle = 360 / total
      const targetAngle = selectedIndex * sectorAngle + (sectorAngle / 2)

      // 设置最终位置
      chart?.setOption({
        series: [{
          data: [{
            value: targetAngle
          }]
        }]
      })

      // 动画结束
      isSpinning.value = false
      selectedFood.value = targetFood
      currentFood.value = null

      // 添加到历史记录
      history.value.unshift({
        food: targetFood,
        timestamp: Date.now()
      })
      if (history.value.length > 10) history.value.pop()
      saveHistory()
    }
  }

  // 开始动画
  animate()
}

// 监听窗口大小变化
const handleResize = () => {
  chart?.resize()
}

onMounted(() => {
  loadFoods()
  loadHistory()
  initChart()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chart?.dispose()
})

// 监听食物列表变化，更新图表
watch(displayFoods, () => {
  updateChartOptions()
}, { deep: true })
</script>

<style scoped>
.food-wheel-page {
  margin: -36px -28px 0;
  min-height: 90vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  padding: 24px;
  display: flex;
  justify-content: center;
}

.food-wheel-container {
  max-width: 1200px;
  width: 100%;
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: 24px;
}

.wheel-section {
  background: white;
  border-radius: 24px;
  padding: 32px 24px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  align-items: center;
}

.wheel-wrapper {
  width: 400px;
  height: 400px;
  margin: 0 auto;
  position: relative;
}

.wheel-chart {
  width: 100%;
  height: 100%;
}

.wheel-controls {
  margin-top: 24px;
}

.spin-button {
  padding: 12px 32px;
  font-size: 18px;
  border: none;
  border-radius: 12px;
  background: linear-gradient(135deg, #345750 0%, #3a8fb7 100%);
  color: white;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.spin-button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
  background: #6c757d;
}

.spin-button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(52, 87, 80, 0.2);
}

.spin-button:active:not(:disabled) {
  transform: translateY(0);
  box-shadow: none;
}

.result-section {
  margin-top: 24px;
}

.result-card {
  background: white;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.food-info h4 {
  color: #345750;
  margin: 0 0 8px;
  font-size: 24px;
}

.food-category {
  color: #6c757d;
  font-size: 14px;
  margin: 8px 0;
}

.food-description {
  color: #495057;
  margin: 8px 0;
}

.food-management {
  background: white;
  border-radius: 24px;
  padding: 24px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.management-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.add-button {
  padding: 8px 16px;
  border: none;
  border-radius: 8px;
  background: #345750;
  color: white;
  cursor: pointer;
  transition: all 0.3s ease;
}

.category-filter {
  margin-bottom: 16px;
}

.category-filter select {
  width: 200px;
  padding: 8px;
  border: 1px solid #ced4da;
  border-radius: 8px;
  font-size: 14px;
}

.food-list {
  display: grid;
  gap: 16px;
  max-height: 500px;
  overflow-y: auto;
  padding-right: 8px;
}

.food-item {
  background: #f8f9fa;
  border-radius: 12px;
  padding: 16px;
  transition: all 0.3s ease;
}

.food-item:hover {
  transform: translateX(4px);
  background: #fff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.food-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.food-details h4 {
  margin: 0 0 8px;
  color: #345750;
}

.category-tag {
  display: inline-block;
  padding: 4px 8px;
  background: rgba(52, 87, 80, 0.1);
  color: #345750;
  border-radius: 4px;
  font-size: 12px;
  margin: 4px 0;
}

.food-actions {
  display: flex;
  gap: 8px;
}

.edit-btn,
.delete-btn {
  padding: 6px 12px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 14px;
}

.edit-btn {
  background: rgba(52, 87, 80, 0.1);
  color: #345750;
}

.delete-btn {
  background: rgba(220, 53, 69, 0.1);
  color: #dc3545;
}

.edit-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.edit-form input,
.edit-form select {
  padding: 8px;
  border: 1px solid #ced4da;
  border-radius: 8px;
  font-size: 14px;
}

.edit-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  margin-top: 12px;
}

.save-btn,
.cancel-btn {
  padding: 6px 12px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 14px;
}

.save-btn {
  background: #345750;
  color: white;
}

.cancel-btn {
  background: #6c757d;
  color: white;
}

.add-food-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 16px;
  padding: 24px;
  width: 90%;
  max-width: 400px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.modal-content h3 {
  margin: 0;
  color: #345750;
}

.modal-content input,
.modal-content select {
  padding: 8px;
  border: 1px solid #ced4da;
  border-radius: 8px;
  font-size: 14px;
}

.modal-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 8px;
}

.history-section {
  background: white;
  border-radius: 24px;
  padding: 24px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  margin-top: 24px;
}

.history-list {
  max-height: 300px;
  overflow-y: auto;
  padding-right: 8px;
}

.history-item {
  display: flex;
  justify-content: space-between;
  padding: 12px;
  border-bottom: 1px solid #e9ecef;
}

.history-food {
  color: #345750;
  font-weight: 500;
}

.history-time {
  color: #6c757d;
  font-size: 14px;
}

@media (max-width: 768px) {
  .food-wheel-page {
    min-height: 94vh;
  }
  .food-wheel-container {
    grid-template-columns: 1fr;
    gap: 8px;
    margin: -4px;
    justify-items: center;
    width: 100%;
    max-width: 100%;
    box-sizing: border-box;
  }

  .wheel-section,
  .food-management,
  .result-section,
  .history-section {
    width: 100%;
    max-width: 100%;
    margin-left: 4px;
    margin-right: 4px;
    border-radius: 8px;
    box-sizing: border-box;
    padding-left: 0;
    padding-right: 0;
  }

  .wheel-section {
    padding: 8px 0 8px 0;
    align-items: center;
  }

  .wheel-wrapper {
    width: 95vw;
    max-width: 320px;
    height: 95vw;
    max-height: 320px;
    margin: 0 auto;
    display: flex;
    justify-content: center;
    align-items: center;
  }

  .food-management {
    padding: 12px;
    border-radius: 14px;
  }

  .management-header {
    flex-direction: column;
    gap: 8px;
    align-items: stretch;
  }

  .add-button {
    width: 100%;
    font-size: 15px;
    padding: 8px 0;
    border-radius: 8px;
    margin-bottom: 4px;
  }

  .category-filter {
    margin-bottom: 10px;
  }
  .category-filter select {
    width: 100%;
    font-size: 14px;
    padding: 7px 8px;
    border-radius: 7px;
  }

  .food-list {
    max-height: 300px;
    gap: 10px;
    padding-right: 4px;
  }

  .food-item {
    padding: 10px;
    border-radius: 8px;
    font-size: 14px;
  }

  .food-details h4 {
    font-size: 16px;
    margin-bottom: 4px;
  }
  .category-tag {
    font-size: 11px;
    padding: 2px 6px;
    border-radius: 3px;
    margin: 2px 0;
  }
  .description {
    font-size: 13px;
    margin: 2px 0;
  }

  .food-actions {
    flex-direction: column;
    gap: 4px;
    margin-left: 8px;
  }
  .edit-btn, .delete-btn {
    font-size: 13px;
    padding: 4px 0;
    border-radius: 5px;
    min-width: 48px;
  }

  .edit-form {
    gap: 8px;
  }
  .edit-form input, .edit-form select {
    font-size: 13px;
    padding: 6px;
    border-radius: 6px;
  }
  .edit-actions {
    gap: 4px;
    margin-top: 6px;
  }
  .save-btn, .cancel-btn {
    font-size: 13px;
    padding: 4px 0;
    border-radius: 5px;
    min-width: 48px;
  }

  .result-section {
    margin-top: 16px;
  }

  .history-section {
    margin-top: 16px;
    padding: 16px;
  }

  .modal-content {
    width: 95%;
    padding: 16px;
  }

  .spin-button {
    padding: 10px 24px;
    font-size: 16px;
  }
}

@media (max-width: 480px) {
  .wheel-wrapper {
    width: 200px;
    height: 200px;
  }
  .current-food-indicator {
    font-size: 13px;
    padding: 5px 10px;
    top: -24px;
  }
  .modal-content {
    width: 99%;
    padding: 8px;
    border-radius: 10px;
  }
}

/* 自定义滚动条样式 */
.food-list::-webkit-scrollbar,
.history-list::-webkit-scrollbar {
  width: 6px;
}

.food-list::-webkit-scrollbar-track,
.history-list::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.05);
  border-radius: 3px;
}

.food-list::-webkit-scrollbar-thumb,
.history-list::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.1);
  border-radius: 3px;
}

.food-list::-webkit-scrollbar-thumb:hover,
.history-list::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.2);
}

.current-food-indicator {
  position: absolute;
  top: -40px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(52, 87, 80, 0.9);
  color: white;
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 16px;
  font-weight: 500;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  animation: fadeInOut 0.3s ease;
  white-space: nowrap;
}

@keyframes fadeInOut {
  from {
    opacity: 0;
    transform: translate(-50%, 10px);
  }
  to {
    opacity: 1;
    transform: translate(-50%, 0);
  }
}
</style>
