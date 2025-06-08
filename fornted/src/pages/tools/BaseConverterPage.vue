<template>
  <div class="base-converter-page">
    <div class="converter-container">
      <div class="input-section">
        <div class="number-input">
          <div class="input-header">
            <h3>输入数值</h3>
            <a-select v-model:value="inputBase" class="base-select" size="large">
              <a-select-option :value="2">二进制 (2)</a-select-option>
              <a-select-option :value="8">八进制 (8)</a-select-option>
              <a-select-option :value="10">十进制 (10)</a-select-option>
              <a-select-option :value="16">十六进制 (16)</a-select-option>
            </a-select>
          </div>
          <a-input
            v-model:value="inputValue"
            :placeholder="getPlaceholder(inputBase)"
            @input="handleInput"
            class="base-input"
            :status="inputError ? 'error' : ''"
            size="large"
            allowClear
          />
          <div class="input-error" v-if="inputError">{{ inputError }}</div>
        </div>
      </div>

      <div class="results-section">
        <div class="result-card" v-for="base in outputBases" :key="base">
          <div class="result-header">
            <span class="base-label">{{ getBaseLabel(base) }}</span>
            <a-tooltip title="复制结果">
              <a-button
                type="text"
                @click="copyResult(base)"
                :disabled="!getResult(base) || getResult(base) === '-'"
                class="copy-btn"
              >
                <template #icon><CopyOutlined /></template>
              </a-button>
            </a-tooltip>
          </div>
          <div class="result-value" :class="{ 'long-value': getResult(base).length > 12 }">
            {{ getResult(base) }}
          </div>
        </div>
      </div>

      <div class="help-section">
        <a-button type="link" @click="showReferenceTable = true" class="help-btn">
          <TableOutlined />
          <span>查看常用值对照表</span>
        </a-button>
      </div>
    </div>

    <!-- 对照表弹框 -->
    <a-modal
      v-model:visible="showReferenceTable"
      title="常用值对照表"
      :footer="null"
      :width="isMobile ? '90%' : '500px'"
      :bodyStyle="{ padding: '12px' }"
      class="reference-modal"
      :maskClosable="true"
      :destroyOnClose="true"
    >
      <div class="reference-table-wrapper">
        <div class="reference-table">
          <div class="table-row header">
            <span>十进制</span>
            <span>二进制</span>
            <span>八进制</span>
            <span>十六进制</span>
          </div>
          <div class="table-row" v-for="n in 16" :key="n-1">
            <span>{{ n-1 }}</span>
            <span>{{ (n-1).toString(2).padStart(4, '0') }}</span>
            <span>{{ (n-1).toString(8) }}</span>
            <span>{{ (n-1).toString(16).toUpperCase() }}</span>
          </div>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { CopyOutlined, TableOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'

// 状态
const inputBase = ref(10)
const inputValue = ref('')
const inputError = ref('')
const showReferenceTable = ref(false)

// 检测是否为移动端
const isMobile = computed(() => {
  return window.innerWidth <= 768
})

// 输出进制列表
const outputBases = [2, 8, 10, 16]

// 获取进制标签
const getBaseLabel = (base: number) => {
  const labels: Record<number, string> = {
    2: '二进制',
    8: '八进制',
    10: '十进制',
    16: '十六进制'
  }
  return `${labels[base]} (${base})`
}

// 获取输入框提示
const getPlaceholder = (base: number) => {
  const examples: Record<number, string> = {
    2: '例如: 1010',
    8: '例如: 12',
    10: '例如: 10',
    16: '例如: A5'
  }
  return examples[base]
}

// 验证输入
const validateInput = (value: string, base: number) => {
  if (!value) return true

  // 移除前后空格
  value = value.trim()

  // 检查是否为空
  if (value === '') return true

  // 根据进制验证输入
  switch (base) {
    case 2:
      return /^[01]+$/.test(value)
    case 8:
      return /^[0-7]+$/.test(value)
    case 10:
      return /^[0-9]+$/.test(value)
    case 16:
      return /^[0-9A-Fa-f]+$/.test(value)
    default:
      return false
  }
}

// 处理输入
const handleInput = (value: string) => {
  if (!value || value.trim() === '') {
    inputError.value = ''
    return
  }

  if (!validateInput(value, inputBase.value)) {
    inputError.value = `请输入有效的${getBaseLabel(inputBase.value)}数值`
    return
  }

  // 检查数值范围
  try {
    const decimal = parseInt(value, inputBase.value)
    if (isNaN(decimal)) {
      inputError.value = '无效的数值'
      return
    }
    if (decimal < 0) {
      inputError.value = '请输入非负数'
      return
    }
    if (decimal > Number.MAX_SAFE_INTEGER) {
      inputError.value = '数值超出范围'
      return
    }
    inputError.value = ''
  } catch (e) {
    inputError.value = '无效的数值'
  }
}

// 进制转换
const convertBase = (value: string, fromBase: number, toBase: number): string => {
  if (!value || value.trim() === '' || inputError.value) return '-'
  try {
    const decimal = parseInt(value.trim(), fromBase)
    if (isNaN(decimal) || decimal < 0) return '-'
    return decimal.toString(toBase).toUpperCase()
  } catch {
    return '-'
  }
}

// 获取转换结果
const getResult = (base: number) => {
  return convertBase(inputValue.value, inputBase.value, base)
}

// 复制结果
const copyResult = async (base: number) => {
  const result = getResult(base)
  if (result === '-') return

  try {
    await navigator.clipboard.writeText(result)
    message.success('已复制到剪贴板')
  } catch {
    message.error('复制失败')
  }
}
</script>

<style scoped>
.base-converter-page {
  min-height: calc(94vh - 60px);
  margin: -20px;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 16px;
}

.converter-container {
  width: 100%;
  max-width: 800px;
  margin: 0 auto;
  padding: 24px;
  background: rgba(255, 255, 255, 0.98);
  border-radius: 16px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.input-section {
  background: white;
  padding: 16px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.03);
}

.number-input {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.input-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.input-header h3 {
  margin: 0;
  color: #1f2937;
  font-size: 18px;
  font-weight: 600;
}

.base-select {
  width: 120px !important;
}

.base-input {
  font-size: 16px;
  font-family: monospace;
}

.input-error {
  color: #dc2626;
  font-size: 14px;
  padding: 2px 0;
}

.results-section {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.result-card {
  background: white;
  padding: 16px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.03);
  transition: all 0.2s ease;
}

.result-card:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.base-label {
  color: #4b5563;
  font-size: 15px;
  font-weight: 500;
}

.copy-btn {
  padding: 4px 8px;
  height: auto;
}

.copy-btn:not(:disabled):hover {
  color: #1890ff;
  background: rgba(24, 144, 255, 0.1);
}

.result-value {
  font-size: 18px;
  font-weight: 500;
  color: #1f2937;
  font-family: monospace;
  word-break: break-all;
  min-height: 27px;
  padding: 2px 0;
}

.result-value.long-value {
  font-size: 15px;
}

.help-section {
  display: flex;
  justify-content: center;
  padding: 4px 0;
}

.help-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  height: auto;
  padding: 6px 12px;
  font-size: 14px;
}

.help-btn:hover {
  background: rgba(24, 144, 255, 0.1);
}

.reference-table-wrapper {
  background: white;
  border-radius: 8px;
  overflow: hidden;
}

.reference-table {
  width: 100%;
}

.table-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  padding: 10px;
  font-size: 14px;
  align-items: center;
  border-bottom: 1px solid #f0f0f0;
}

.table-row.header {
  background: #f3f4f6;
  font-weight: 600;
  color: #374151;
  position: sticky;
  top: 0;
  z-index: 1;
}

.table-row:last-child {
  border-bottom: none;
}

@media (max-width: 768px) {

  .base-converter-page {
    min-height: calc(94vh - 2px);
    margin: -16px;
    padding: 0 12px;
  }

  .converter-container {
    padding: 16px;
    gap: 16px;
  }

  .input-section {
    padding: 12px;
  }

  .input-header h3 {
    font-size: 16px;
  }

  .base-select {
    width: 110px !important;
  }

  .results-section {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .result-card {
    padding: 12px;
  }

  .result-value {
    font-size: 16px;
    min-height: 24px;
  }

  .result-value.long-value {
    font-size: 14px;
  }

  .help-btn {
    padding: 4px 10px;
    font-size: 13px;
  }

  .table-row {
    gap: 8px;
    padding: 8px;
    font-size: 13px;
  }
}

:deep(.ant-modal-content) {
  border-radius: 16px;
  overflow: hidden;
}

:deep(.ant-modal-header) {
  border-bottom: 1px solid #f0f0f0;
  padding: 16px 20px;
}

:deep(.ant-modal-body) {
  padding: 16px !important;
}

:deep(.ant-modal-close) {
  top: 14px;
  right: 16px;
}
</style>
