<template>
  <div class="bankers-algorithm-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1>银行家算法演示</h1>
      <p class="subtitle">理解死锁避免的经典算法</p>
    </div>

    <!-- 算法原理介绍部分 -->
    <div class="theory-section">
      <a-card title="算法核心原理" class="theory-card">
        <div class="principle">
          <h3>一句话讲透</h3>
          <p class="highlight-text">
            银行家算法是通过预先判断 <strong>"资源分配后系统是否仍处于安全状态"</strong>，来决定是否分配资源，
            从而避免死锁的经典死锁避免算法
          </p>
          <div class="analogy">
            <span class="analogy-icon">💡</span>
            类比银行放贷：确保放贷后银行仍有能力满足其他客户需求，不会"资金链断裂"
          </div>
        </div>
      </a-card>

      <a-card title="关键概念定义" class="theory-card">
        <div class="concepts">
          <div class="concept-item">
            <h4>🔒 安全状态</h4>
            <p>存在一种"资源分配序列"，能让所有进程按顺序完成</p>
            <span class="example">例如：进程A→B→C，每个进程都能拿到所需资源并释放，系统不会卡住</span>
          </div>
          <div class="concept-item">
            <h4>📦 资源类型</h4>
            <p>系统中可分配的资源</p>
            <span class="example">例如：R1=10台打印机，R2=5块内存，R3=7个CPU</span>
          </div>
        </div>
      </a-card>

      <a-card title="核心数据结构" class="theory-card">
        <div class="data-structures">
          <a-row :gutter="16">
            <a-col :span="12">
              <div class="structure-item">
                <h4>🎯 总资源向量 (Total)</h4>
                <p>每种资源的总数量</p>
                <div class="example-data">Total = [10, 5, 7]</div>
              </div>
            </a-col>
            <a-col :span="12">
              <div class="structure-item">
                <h4>✅ 已分配矩阵 (Allocated)</h4>
                <p>每个进程已拿到的资源</p>
                <div class="example-data">Allocated[A] = [2, 0, 0]</div>
              </div>
            </a-col>
            <a-col :span="12">
              <div class="structure-item">
                <h4>📋 需求矩阵 (Need)</h4>
                <p>每个进程还需要的资源</p>
                <div class="example-data">Need[A] = [3, 2, 2]</div>
              </div>
            </a-col>
            <a-col :span="12">
              <div class="structure-item">
                <h4>💰 可用资源向量 (Available)</h4>
                <p>当前系统剩余的资源</p>
                <div class="example-data">Available = [3, 3, 2]</div>
              </div>
            </a-col>
          </a-row>
        </div>
      </a-card>

      <a-card title="核心步骤" class="theory-card">
        <div class="steps">
          <div class="step-item">
            <div class="step-number">1</div>
            <div class="step-content">
              <h4>判断"请求是否合理"</h4>
              <p>若 Request[P] ≤ Need[P]（请求不超过进程总需求），进入第二步</p>
              <p class="warning">否则拒绝（进程"贪心"，请求超出需要）</p>
            </div>
          </div>
          <div class="step-item">
            <div class="step-number">2</div>
            <div class="step-content">
              <h4>判断"系统是否有能力分配"</h4>
              <p>若 Request[P] ≤ Available（剩余资源够分配），则尝试分配</p>
              <p class="highlight">检查分配后系统是否"安全"；若安全则正式分配，否则拒绝</p>
            </div>
          </div>
        </div>
      </a-card>
    </div>

    <!-- 交互模拟部分 -->
    <div class="simulation-section">
      <a-card title="交互式模拟演示" class="simulation-card">
        <!-- 控制面板 -->
        <div class="control-panel">
          <a-row :gutter="16">
            <a-col :span="6">
              <a-select v-model:value="selectedExample" placeholder="选择预设案例" style="width: 100%">
                <a-select-option value="safe">✅ 安全分配案例</a-select-option>
                <a-select-option value="unsafe">❌ 不安全案例</a-select-option>
                <a-select-option value="insufficient">⚠️ 资源不足案例</a-select-option>
                <a-select-option value="invalid">🚫 请求不合理案例</a-select-option>
              </a-select>
            </a-col>
            <a-col :span="6">
              <a-button type="primary" @click="loadSelectedExample" :loading="loading" :disabled="!selectedExample">
                加载案例
              </a-button>
            </a-col>
            <a-col :span="6">
              <a-button @click="resetSimulation" :disabled="loading">
                重置模拟
              </a-button>
            </a-col>
            <a-col :span="6">
              <a-button type="dashed" @click="showCustomInput = !showCustomInput">
                {{ showCustomInput ? '隐藏' : '显示' }}自定义输入
              </a-button>
            </a-col>
          </a-row>
        </div>

        <!-- 操作指南 -->
        <div class="guide-section">
          <a-alert
            message="操作指南"
            description="1. 选择并加载预设案例或自定义数据 → 2. 在模拟区域选择进程和请求资源 → 3. 点击'开始模拟'按钮 → 4. 按步骤执行检查观察过程"
            type="info"
            show-icon
            closable
          />
        </div>

        <!-- 自定义输入区域 -->
        <div v-if="showCustomInput" class="custom-input">
          <a-divider>自定义数据输入</a-divider>

          <!-- 结构设置 -->
          <div class="structure-config">
            <h4>🔧 结构配置</h4>
            <a-row :gutter="16">
              <a-col :span="8">
                <a-form-item label="进程数量">
                  <a-input-number
                    v-model:value="processCount"
                    :min="2"
                    :max="8"
                    placeholder="2-8个进程"
                    style="width: 100%"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="资源类型数">
                  <a-input-number
                    v-model:value="resourceCount"
                    :min="2"
                    :max="5"
                    placeholder="2-5种资源"
                    style="width: 100%"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label=" ">
                  <a-button type="primary" @click="updateDataStructure">更新结构</a-button>
                </a-form-item>
              </a-col>
            </a-row>
          </div>

          <!-- 数据输入表单 -->
          <div v-if="systemData.total.length > 0" class="data-input-forms">
            <!-- 总资源设置 -->
            <div class="input-section">
              <h4>🎯 总资源向量 (Total)</h4>
              <a-row :gutter="8">
                <a-col v-for="(resource, index) in systemData.total" :key="index" :span="4">
                  <a-form-item :label="`R${index + 1}`">
                    <a-input-number
                      v-model:value="systemData.total[index]"
                      :min="1"
                      :max="20"
                      style="width: 100%"
                      @change="validateTotalResource(index)"
                    />
                  </a-form-item>
                </a-col>
              </a-row>
            </div>

            <!-- 可用资源设置 -->
            <div class="input-section">
              <h4>💰 可用资源向量 (Available)</h4>
              <a-row :gutter="8">
                <a-col v-for="(resource, index) in systemData.available" :key="index" :span="4">
                  <a-form-item :label="`R${index + 1}`">
                    <a-input-number
                      v-model:value="systemData.available[index]"
                      :min="0"
                      :max="systemData.total[index] || 20"
                      style="width: 100%"
                      @change="validateAvailableResource(index)"
                    />
                  </a-form-item>
                </a-col>
              </a-row>
            </div>

            <!-- 进程数据设置 -->
            <div class="input-section">
              <h4>📊 进程资源矩阵</h4>
              <a-tabs v-model:activeKey="activeProcessTab">
                <a-tab-pane v-for="(_, pIndex) in systemData.allocated" :key="pIndex" :tab="`进程 P${pIndex}`">
                  <a-row :gutter="16">
                    <a-col :span="12">
                      <h5>✅ 已分配资源 (Allocated)</h5>
                      <a-row :gutter="8">
                        <a-col v-for="(_, rIndex) in systemData.total" :key="rIndex" :span="6">
                          <a-form-item :label="`R${rIndex + 1}`">
                            <a-input-number
                              v-model:value="systemData.allocated[pIndex][rIndex]"
                              :min="0"
                              :max="systemData.total[rIndex] || 20"
                              style="width: 100%"
                              @change="validateAllocatedResource(pIndex, rIndex)"
                            />
                          </a-form-item>
                        </a-col>
                      </a-row>
                    </a-col>
                    <a-col :span="12">
                      <h5>📋 需求资源 (Need)</h5>
                      <a-row :gutter="8">
                        <a-col v-for="(_, rIndex) in systemData.total" :key="rIndex" :span="6">
                          <a-form-item :label="`R${rIndex + 1}`">
                            <a-input-number
                              v-model:value="systemData.need[pIndex][rIndex]"
                              :min="0"
                              :max="systemData.total[rIndex] || 20"
                              style="width: 100%"
                              @change="validateNeedResource(pIndex, rIndex)"
                            />
                          </a-form-item>
                        </a-col>
                      </a-row>
                    </a-col>
                  </a-row>
                </a-tab-pane>
              </a-tabs>
            </div>

            <!-- 数据验证和应用 -->
            <div class="validation-section">
              <a-space>
                <a-button type="primary" @click="validateAllData" :loading="validating">
                  验证数据
                </a-button>
                <a-button @click="applyCustomData" :disabled="!dataValid">
                  应用数据
                </a-button>
                <a-text v-if="validationMessage" :type="dataValid ? 'success' : 'danger'">
                  {{ validationMessage }}
                </a-text>
              </a-space>
            </div>
          </div>
        </div>

        <!-- 数据展示区域 -->
        <div v-if="systemData.total.length > 0" class="data-display">
          <a-divider>当前系统状态</a-divider>

          <!-- 总资源和可用资源 -->
          <a-row :gutter="16" class="resource-info">
            <a-col :span="12">
              <div class="resource-card">
                <h4>🎯 总资源 (Total)</h4>
                <div class="resource-vector">
                  <span v-for="(resource, index) in systemData.total" :key="index" class="resource-item">
                    R{{ index + 1 }}: {{ resource }}
                  </span>
                </div>
              </div>
            </a-col>
            <a-col :span="12">
              <div class="resource-card available">
                <h4>💰 可用资源 (Available)</h4>
                <div class="resource-vector">
                  <span v-for="(resource, index) in systemData.available" :key="index"
                        class="resource-item"
                        :class="{ 'insufficient': simulationStep === 2 && requestData.request[index] > resource }">
                    R{{ index + 1 }}: {{ resource }}
                  </span>
                </div>
              </div>
            </a-col>
          </a-row>

          <!-- 进程状态可视化 -->
          <div class="process-visualization">
            <h4>📊 进程状态可视化</h4>

            <!-- 进程卡片展示 -->
            <div class="process-cards">
              <div
                v-for="(process, index) in processTableData"
                :key="index"
                class="process-card"
                :class="{
                  'requesting': requestData.processIndex === index,
                  'completed': completedProcesses.includes(index),
                  'current': currentProcessIndex === index
                }"
              >
                <div class="process-header">
                  <span class="process-name">P{{ index }}</span>
                  <span v-if="requestData.processIndex === index" class="requesting-badge">请求中</span>
                  <span v-if="completedProcesses.includes(index)" class="completed-badge">已完成</span>
                </div>
                <div class="process-resources">
                  <div class="resource-row">
                    <span class="resource-label">已分配:</span>
                    <div class="resource-values allocated">
                      <span v-for="(val, rIndex) in process.allocated" :key="rIndex" class="resource-value">
                        R{{ rIndex + 1 }}: {{ val }}
                      </span>
                    </div>
                  </div>
                  <div class="resource-row">
                    <span class="resource-label">还需要:</span>
                    <div class="resource-values need">
                      <span v-for="(val, rIndex) in process.need" :key="rIndex" class="resource-value">
                        R{{ rIndex + 1 }}: {{ val }}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 资源池可视化 -->
            <div class="resource-pool">
              <h5>🏦 系统资源池</h5>
              <div class="resource-pool-container">
                <div v-for="(resource, index) in systemData.available" :key="index" class="resource-pool-item">
                  <div class="resource-icon">R{{ index + 1 }}</div>
                  <div class="resource-amount" :class="{ 'insufficient': simulationStep === 2 && requestData.request[index] > resource }">
                    {{ resource }}
                  </div>
                  <div class="resource-total">/ {{ systemData.total[index] }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 模拟请求区域 -->
        <div v-if="systemData.total.length > 0" class="request-section">
          <a-divider>资源请求模拟</a-divider>

          <a-row :gutter="16" class="request-controls">
            <a-col :span="6">
              <h4>选择请求进程</h4>
              <a-select v-model:value="requestData.processIndex" style="width: 100%" placeholder="选择进程">
                <a-select-option v-for="(_, index) in systemData.allocated" :key="index" :value="index">
                  进程 P{{ index }}
                </a-select-option>
              </a-select>
            </a-col>
            <a-col :span="10">
              <h4>请求资源数量</h4>
              <a-space>
                <a-input-number
                  v-for="(_, index) in systemData.total"
                  :key="index"
                  v-model:value="requestData.request[index]"
                  :min="0"
                  :placeholder="`R${index + 1}`"
                  style="width: 80px"
                />
              </a-space>
            </a-col>
            <a-col :span="8">
              <h4>执行控制</h4>
              <a-space>
                <a-button
                  type="primary"
                  @click="startSimulation"
                  :disabled="requestData.processIndex === null || isExecuting"
                  :loading="isExecuting"
                >
                  {{ isExecuting ? '自动执行中...' : '开始模拟' }}
                </a-button>
                <a-button
                  v-if="isExecuting"
                  @click="pauseExecution"
                  type="default"
                >
                  暂停
                </a-button>
                <a-button
                  v-if="!isExecuting && simulationStarted && !simulationComplete"
                  @click="resumeExecution"
                  type="default"
                >
                  继续
                </a-button>
              </a-space>
            </a-col>
          </a-row>

          <!-- 执行设置 -->
          <div class="execution-settings">
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item label="执行模式">
                  <a-radio-group v-model:value="autoExecution">
                    <a-radio :value="true">🤖 自动执行</a-radio>
                    <a-radio :value="false">👆 手动执行</a-radio>
                  </a-radio-group>
                </a-form-item>
              </a-col>
              <a-col :span="12" v-if="autoExecution">
                <a-form-item label="执行间隔">
                  <a-slider
                    v-model:value="executionSpeed"
                    :min="500"
                    :max="5000"
                    :step="500"
                    :marks="{ 500: '快速', 2000: '正常', 5000: '慢速' }"
                  />
                </a-form-item>
              </a-col>
            </a-row>
          </div>
        </div>

        <!-- 模拟步骤展示 -->
        <div v-if="simulationStarted" class="simulation-steps">
          <a-divider>模拟过程</a-divider>

          <!-- 步骤1：检查请求合理性 -->
          <div class="step-card" :class="{ 'active': simulationStep >= 1, 'success': step1Result === true, 'error': step1Result === false }">
            <div class="step-header">
              <span class="step-badge">1</span>
              <h4>检查请求合理性</h4>
              <div class="step-status">
                <span v-if="step1Result === true" class="status-icon success">✅</span>
                <span v-else-if="step1Result === false" class="status-icon error">❌</span>
                <span v-else-if="simulationStep >= 1" class="status-icon loading">⏳</span>
              </div>
            </div>
            <div v-if="simulationStep >= 1" class="step-content">
              <p>检查条件：Request[P{{ requestData.processIndex }}] ≤ Need[P{{ requestData.processIndex }}]</p>
              <div class="comparison animated-comparison">
                <div class="comparison-item" :class="{ 'highlight': step1Result !== null }">
                  <span class="comparison-label">请求:</span>
                  <span class="comparison-value">[{{ requestData.request.join(', ') }}]</span>
                </div>
                <div class="comparison-operator" :class="{ 'pulse': step1Result !== null }">
                  ≤
                </div>
                <div class="comparison-item" :class="{ 'highlight': step1Result !== null }">
                  <span class="comparison-label">需求:</span>
                  <span class="comparison-value">[{{ systemData.need[requestData.processIndex].join(', ') }}]</span>
                </div>
              </div>

              <!-- 详细检查过程 -->
              <div v-if="step1Details.length > 0" class="check-details">
                <h5>🔍 逐项检查过程：</h5>
                <div v-for="(detail, index) in step1Details" :key="index" class="check-detail-item" :class="detail.type">
                  {{ detail.message }}
                </div>
              </div>

              <!-- 矩阵运算展示 -->
              <div v-if="step1MatrixCalculation" class="matrix-calculation">
                <h5>📊 矩阵运算过程：</h5>
                <div class="matrix-display">
                  <div class="matrix-row">
                    <span class="matrix-label">Request[P{{ requestData.processIndex }}]:</span>
                    <div class="matrix-vector">[{{ requestData.request.join(', ') }}]</div>
                  </div>
                  <div class="matrix-row">
                    <span class="matrix-label">Need[P{{ requestData.processIndex }}]:</span>
                    <div class="matrix-vector">[{{ systemData.need[requestData.processIndex].join(', ') }}]</div>
                  </div>
                  <div class="matrix-result">
                    <span class="matrix-label">比较结果：</span>
                    <div class="comparison-results">
                      <span v-for="(result, index) in step1MatrixCalculation.comparisons" :key="index"
                            class="comparison-result" :class="result.valid ? 'valid' : 'invalid'">
                        {{ requestData.request[index] }} {{ result.valid ? '≤' : '>' }} {{ systemData.need[requestData.processIndex][index] }}
                      </span>
                    </div>
                    <div class="final-result" :class="step1MatrixCalculation.allValid ? 'success' : 'error'">
                      最终结果: {{ step1MatrixCalculation.allValid ? '✅ 所有条件满足' : '❌ 存在不满足条件' }}
                    </div>
                  </div>
                </div>
              </div>

              <p v-if="step1Result !== null" class="result-text" :class="{ 'success': step1Result, 'error': !step1Result }">
                {{ step1Message }}
              </p>
              <a-button
                v-if="step1Result === null && !autoExecution"
                @click="checkStep1"
                type="primary"
                size="small"
                :loading="step1Checking"
              >
                {{ step1Checking ? '检查中...' : '🔍 执行检查' }}
              </a-button>
            </div>
          </div>

          <!-- 步骤2：检查资源可用性 -->
          <div v-if="step1Result === true" class="step-card" :class="{ 'active': simulationStep >= 2, 'success': step2Result === true, 'error': step2Result === false }">
            <div class="step-header">
              <span class="step-badge">2</span>
              <h4>检查资源可用性</h4>
              <div class="step-status">
                <span v-if="step2Result === true" class="status-icon success">✅</span>
                <span v-else-if="step2Result === false" class="status-icon error">❌</span>
                <span v-else-if="simulationStep >= 2" class="status-icon loading">⏳</span>
              </div>
            </div>
            <div v-if="simulationStep >= 2" class="step-content">
              <p>检查条件：Request[P{{ requestData.processIndex }}] ≤ Available</p>
              <div class="comparison animated-comparison">
                <div class="comparison-item" :class="{ 'highlight': step2Result !== null }">
                  <span class="comparison-label">请求:</span>
                  <span class="comparison-value">[{{ requestData.request.join(', ') }}]</span>
                </div>
                <div class="comparison-operator" :class="{ 'pulse': step2Result !== null }">
                  ≤
                </div>
                <div class="comparison-item" :class="{ 'highlight': step2Result !== null }">
                  <span class="comparison-label">可用:</span>
                  <span class="comparison-value">[{{ systemData.available.join(', ') }}]</span>
                </div>
              </div>

              <!-- 资源可用性检查详情 -->
              <div v-if="step2Details.length > 0" class="check-details">
                <h5>🔍 逐项检查过程：</h5>
                <div v-for="(detail, index) in step2Details" :key="index" class="check-detail-item" :class="detail.type">
                  {{ detail.message }}
                </div>
              </div>

              <!-- 矩阵运算展示 -->
              <div v-if="step2MatrixCalculation" class="matrix-calculation">
                <h5>📊 矩阵运算过程：</h5>
                <div class="matrix-display">
                  <div class="matrix-row">
                    <span class="matrix-label">Request[P{{ requestData.processIndex }}]:</span>
                    <div class="matrix-vector">[{{ requestData.request.join(', ') }}]</div>
                  </div>

                  <div class="matrix-row">
                    <span class="matrix-label">Available:</span>
                    <div class="matrix-vector">[{{ systemData.available.join(', ') }}]</div>
                  </div>
                  <div class="matrix-result">
                    <span class="matrix-label">比较结果：</span>
                    <div class="comparison-results">
                      <span v-for="(result, index) in step2MatrixCalculation.comparisons" :key="index"
                            class="comparison-result" :class="result.sufficient ? 'valid' : 'invalid'">
                        {{ requestData.request[index] }} {{ result.sufficient ? '≤' : '>' }} {{ systemData.available[index] }}
                      </span>
                    </div>
                    <div class="final-result" :class="step2MatrixCalculation.allSufficient ? 'success' : 'error'">
                      最终结果: {{ step2MatrixCalculation.allSufficient ? '✅ 资源充足' : '❌ 资源不足' }}
                    </div>
                  </div>
                </div>
              </div>

              <p v-if="step2Result !== null" class="result-text" :class="{ 'success': step2Result, 'error': !step2Result }">
                {{ step2Message }}
              </p>
              <a-button
                v-if="step2Result === null && !autoExecution"
                @click="checkStep2"
                type="primary"
                size="small"
                :loading="step2Checking"
              >
                {{ step2Checking ? '检查中...' : '🔍 执行检查' }}
              </a-button>
            </div>
          </div>

          <!-- 步骤3：安全性检查 -->
          <div v-if="step2Result === true" class="step-card" :class="{ 'active': simulationStep >= 3, 'success': step3Result === true, 'error': step3Result === false }">
            <div class="step-header">
              <span class="step-badge">3</span>
              <h4>安全性检查</h4>
              <div class="step-status">
                <span v-if="step3Result === true" class="status-icon success">✅</span>
                <span v-else-if="step3Result === false" class="status-icon error">❌</span>
                <span v-else-if="simulationStep >= 3" class="status-icon loading">⏳</span>
              </div>
            </div>
            <div v-if="simulationStep >= 3" class="step-content">
              <p>模拟分配后，检查是否存在安全序列</p>

              <!-- 临时状态展示 -->
              <div v-if="tempState" class="temp-state">
                <h5>🔄 分配后的临时状态：</h5>
                <div class="temp-matrices-grid">
                  <div class="matrix-calculation-step3">
                    <h6>矩阵运算过程：</h6>

                    <!-- Available' = Available - Request -->
                    <div class="calculation-step">
                      <div class="step-title">① 更新可用资源：</div>
                      <div class="matrix-equation">
                        <span class="matrix-term">
                          <span class="matrix-label">Available'</span>
                          <div class="matrix-vector">[{{ tempState.available.join(', ') }}]</div>
                        </span>
                        <span class="matrix-operator">=</span>
                        <span class="matrix-term">
                          <span class="matrix-label">Available</span>
                          <div class="matrix-vector">[{{ systemData.available.join(', ') }}]</div>
                        </span>
                        <span class="matrix-operator">-</span>
                        <span class="matrix-term">
                          <span class="matrix-label">Request</span>
                          <div class="matrix-vector">[{{ requestData.request.join(', ') }}]</div>
                        </span>
                      </div>
                      <div class="calculation-details">
                        <span v-for="(val, index) in tempState.available" :key="index" class="calc-detail">
                          R{{ index + 1 }}: {{ systemData.available[index] }} - {{ requestData.request[index] }} = {{ val }}
                        </span>
                      </div>
                    </div>
                  </div>

                  <div class="matrix-calculation-step3">
                    <!-- Allocated[i] = Allocated[i] + Request -->
                    <div class="calculation-step">
                      <div class="step-title">② 更新进程P{{ requestData.processIndex }}已分配资源：</div>
                      <div class="matrix-equation">
                        <span class="matrix-term">
                          <span class="matrix-label">Allocated'[P{{ requestData.processIndex }}]</span>
                          <div class="matrix-vector">[{{ tempState.allocated[requestData.processIndex].join(', ') }}]</div>
                        </span>
                        <span class="matrix-operator">=</span>
                        <span class="matrix-term">
                          <span class="matrix-label">Allocated[P{{ requestData.processIndex }}]</span>
                          <div class="matrix-vector">[{{ systemData.allocated[requestData.processIndex].join(', ') }}]</div>
                        </span>
                        <span class="matrix-operator">+</span>
                        <span class="matrix-term">
                          <span class="matrix-label">Request</span>
                          <div class="matrix-vector">[{{ requestData.request.join(', ') }}]</div>
                        </span>
                      </div>
                      <div class="calculation-details">
                        <span v-for="(val, index) in tempState.allocated[requestData.processIndex]" :key="index" class="calc-detail">
                          R{{ index + 1 }}: {{ systemData.allocated[requestData.processIndex][index] }} + {{ requestData.request[index] }} = {{ val }}
                        </span>
                      </div>
                    </div>

                    <!-- Need[i] = Need[i] - Request -->
                    <div class="calculation-step">
                      <div class="step-title">③ 更新进程P{{ requestData.processIndex }}需求资源：</div>
                      <div class="matrix-equation">
                        <span class="matrix-term">
                          <span class="matrix-label">Need'[P{{ requestData.processIndex }}]</span>
                          <div class="matrix-vector">[{{ tempState.need[requestData.processIndex].join(', ') }}]</div>
                        </span>
                        <span class="matrix-operator">=</span>
                        <span class="matrix-term">
                          <span class="matrix-label">Need[P{{ requestData.processIndex }}]</span>
                          <div class="matrix-vector">[{{ systemData.need[requestData.processIndex].join(', ') }}]</div>
                        </span>
                        <span class="matrix-operator">-</span>
                        <span class="matrix-term">
                          <span class="matrix-label">Request</span>
                          <div class="matrix-vector">[{{ requestData.request.join(', ') }}]</div>
                        </span>
                      </div>
                      <div class="calculation-details">
                        <span v-for="(val, index) in tempState.need[requestData.processIndex]" :key="index" class="calc-detail">
                          R{{ index + 1 }}: {{ systemData.need[requestData.processIndex][index] }} - {{ requestData.request[index] }} = {{ val }}
                        </span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 安全序列查找过程 -->
              <div v-if="safetyCheckSteps.length > 0" class="safety-check">
                <div class="safety-check-header">
                  <h5>🔍 安全序列查找过程：</h5>
                  <a-button type="primary" @click="showSafetyModal = true" size="small">
                    🔍 查看详细过程
                  </a-button>
                </div>
                <div class="safety-summary">
                  <p>已执行 {{ safetyCheckSteps.length }} 个检查步骤，点击上方按钮查看详细过程</p>
                </div>
              </div>

              <!-- 安全检查详情弹窗 -->
              <a-modal
                v-model:open="showSafetyModal"
                title="🔍 安全序列查找过程"
                width="900px"
                :footer="null"
                @cancel="showSafetyModal = false"
              >
                <div class="safety-steps-modal">
                  <div v-for="(step, index) in safetyCheckSteps" :key="index"  :class="step.type">
                    <div class="safety-step-header">
                      <span class="safety-step-index">{{ index + 1 }}.</span>
                      <span class="safety-step-text">{{ step.message }}</span>
                    </div>


                    <!-- 矩阵运算过程 -->
                    <div v-if="step.matrixCalculation" class="safety-matrix-calc">
                      <div class="safety-calc-title">📊 矩阵运算：</div>
                      <span v-if="step.available" class="safety-step-available">可用资源：[{{ step.available.join(', ') }}]</span>
                      <div class="matrix-calc-grid">
                        <!-- Need[i] <= Work 检查 -->
                        <div v-if="step.matrixCalculation.needCheck" class="need-check-calculation">
                          <div class="calc-step">
                            <span class="calc-label">Need[P{{ step.matrixCalculation.processIndex }}] ≤ Work 检查：</span>
                            <div class="calc-comparison">
                              <span class="calc-vector">[{{ step.matrixCalculation.needCheck.need.join(', ') }}]</span>
                              <span class="calc-op">≤</span>
                              <span class="calc-vector">[{{ step.matrixCalculation.needCheck.work.join(', ') }}]</span>
                            </div>
                            <div class="calc-details">
                              <span v-for="(result, rIndex) in step.matrixCalculation.needCheck.results" :key="rIndex"
                                    class="calc-detail-item" :class="result.satisfied ? 'satisfied' : 'not-satisfied'">
                                R{{ rIndex + 1 }}: {{ result.need }} {{ result.satisfied ? '≤' : '>' }} {{ result.work }}
                              </span>
                            </div>
                          </div>
                        </div>

                        <!-- Work = Work + Allocated[i] 计算 -->
                        <div v-if="step.matrixCalculation.workUpdate" class="work-update-calculation">
                          <div class="calc-step">
                            <span class="calc-label">Work 更新计算：</span>
                            <div class="calc-equation">
                              <span class="calc-term">
                                <span class="calc-label">Work'</span>
                                <span class="calc-vector">[{{ step.matrixCalculation.workUpdate.newWork.join(', ') }}]</span>
                              </span>
                              <span class="calc-op">=</span>
                              <span class="calc-term">
                                <span class="calc-label">Work</span>
                                <span class="calc-vector">[{{ step.matrixCalculation.workUpdate.oldWork.join(', ') }}]</span>
                              </span>
                              <span class="calc-op">+</span>
                              <span class="calc-term">
                                <span class="calc-label">Allocated[P{{ step.matrixCalculation.processIndex }}]</span>
                                <span class="calc-vector">[{{ step.matrixCalculation.workUpdate.allocated.join(', ') }}]</span>
                              </span>
                            </div>
                            <div class="calc-details">
                              <span v-for="(calc, rIndex) in step.matrixCalculation.workUpdate.calculations" :key="rIndex" class="calc-detail-item">
                                R{{ rIndex + 1 }}: {{ calc.oldWork }} + {{ calc.allocated }} = {{ calc.newWork }}
                              </span>
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </a-modal>

              <!-- 最终结果 -->
              <div v-if="safeSequence.length > 0" class="safe-sequence">
                <h5>找到安全序列：</h5>
                <div class="sequence">
                  <span v-for="(process, index) in safeSequence" :key="index" class="sequence-item">
                    P{{ process }}
                    <span v-if="index < safeSequence.length - 1" class="sequence-arrow">→</span>
                  </span>
                </div>
              </div>

              <p v-if="step3Result !== null" class="result-text" :class="{ 'success': step3Result, 'error': !step3Result }">
                {{ step3Message }}
              </p>

              <a-button
                v-if="step3Result === null && !autoExecution"
                @click="checkStep3"
                type="primary"
                size="small"
                :loading="step3Checking"
              >
                {{ step3Checking ? '安全检查中...' : '🛡️ 执行安全检查' }}
              </a-button>

              <!-- 安全检查进度条 -->
              <div v-if="step3Checking" class="safety-check-progress">
                <a-progress :percent="safetyCheckProgress" :status="safetyCheckProgress === 100 ? 'success' : 'active'" />
                <p class="progress-text">{{ safetyCheckProgressText }}</p>
              </div>
            </div>
          </div>

          <!-- 最终结果 -->
          <div v-if="simulationComplete" class="final-result">
            <a-divider>🎯 模拟结果</a-divider>
            <div class="result-card animated-result" :class="{ 'success': finalResult, 'error': !finalResult }">
              <div class="result-icon">
                {{ finalResult ? '🎉' : '🚫' }}
              </div>
              <div class="result-content">
                <h3>{{ finalResult ? '✅ 分配成功!' : '❌ 分配失败!' }}</h3>
                <p>{{ finalMessage }}</p>

                <!-- 操作历史记录 -->
                <div v-if="operationHistory.length > 0" class="operation-history">
                  <h5>📋 操作历史</h5>
                  <div class="history-timeline">
                    <div v-for="(op, index) in operationHistory" :key="index" class="history-item" :class="op.type">
                      <div class="history-time">{{ op.time }}</div>
                      <div class="history-action">{{ op.action }}</div>
                      <div class="history-result">{{ op.result }}</div>
                    </div>
                  </div>
                </div>

                <div class="result-actions">
                  <a-space>
                    <a-button v-if="finalResult" @click="applyAllocation" type="primary">
                      ✅ 应用此次分配
                    </a-button>
                    <a-button @click="resetSimulation">
                      🔄 重新模拟
                    </a-button>
                    <a-button @click="saveToHistory" type="dashed">
                      💾 保存到历史
                    </a-button>
                  </a-space>
                </div>
              </div>
            </div>
          </div>

          <!-- 模拟控制面板 -->
          <div v-if="simulationStarted" class="simulation-controls">
            <!-- 此处可以添加暂停、继续、回退等控制按钮 -->
          </div>
        </div>
      </a-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'

// 响应式数据
const loading = ref(false)
const showCustomInput = ref(false)
const processCount = ref(5)
const resourceCount = ref(3)
const selectedExample = ref('')
const activeProcessTab = ref(0)
const validating = ref(false)
const dataValid = ref(false)
const validationMessage = ref('')

// 模拟控制状态
const step1Checking = ref(false)
const step2Checking = ref(false)
const step3Checking = ref(false)
const safetyCheckProgress = ref(0)
const safetyCheckProgressText = ref('')

// 弹窗控制
const showSafetyModal = ref(false)

// 动画和可视化相关
const completedProcesses = ref<number[]>([])
const currentProcessIndex = ref<number | null>(null)
const step1Details = ref<{type: string, message: string}[]>([])
const step2Details = ref<{type: string, message: string}[]>([])

// 定义矩阵运算接口
interface Step1MatrixCalculation {
  processIndex: number
  request: number[]
  need: number[]
  comparisons: Array<{
    index: number
    request: number
    need: number
    valid: boolean
  }>
  allValid: boolean
}

interface Step2MatrixCalculation {
  processIndex: number
  request: number[]
  available: number[]
  comparisons: Array<{
    index: number
    request: number
    available: number
    sufficient: boolean
  }>
  allSufficient: boolean
}

// 矩阵运算相关
const step1MatrixCalculation = ref<Step1MatrixCalculation | null>(null)
const step2MatrixCalculation = ref<Step2MatrixCalculation | null>(null)

// 历史记录
const operationHistory = ref<{time: string, action: string, result: string, type: string}[]>([])

// 系统数据
const systemData = reactive({
  total: [] as number[],
  available: [] as number[],
  allocated: [] as number[][],
  need: [] as number[][]
})

// 请求数据
const requestData = reactive({
  processIndex: null as number | null,
  request: [] as number[]
})

// 模拟状态
const simulationStarted = ref(false)
const simulationStep = ref(0)
const simulationComplete = ref(false)

// 步骤结果
const step1Result = ref<boolean | null>(null)
const step1Message = ref('')
const step2Result = ref<boolean | null>(null)
const step2Message = ref('')
const step3Result = ref<boolean | null>(null)
const step3Message = ref('')

// 定义类型接口
interface SystemState {
  available: number[]
  allocated: number[][]
  need: number[][]
}

interface SafetyStep {
  type: 'info' | 'success' | 'error'
  message: string
  available?: number[]
  matrixCalculation?: {
    processIndex: number
    needCheck?: {
      need: number[]
      work: number[]
      results: Array<{
        resourceIndex: number
        need: number
        work: number
        satisfied: boolean
      }>
    }
    workUpdate?: {
      oldWork: number[]
      allocated: number[]
      newWork: number[]
      calculations: Array<{
        resourceIndex: number
        oldWork: number
        allocated: number
        newWork: number
      }>
    }
  }
}

// 临时状态和安全检查
const tempState = ref<SystemState | null>(null)
const safetyCheckSteps = ref<SafetyStep[]>([])
const safeSequence = ref<number[]>([])

// 最终结果
const finalResult = ref(false)
const finalMessage = ref('')

// 计算属性 - 表格数据
const processTableData = computed(() => {
  return systemData.allocated.map((allocated, index) => ({
    key: index,
    process: `P${index}`,
    allocated: allocated,
    need: systemData.need[index] || []
  }))
})

// 预设案例数据
const examples = {
  safe: {
    name: '安全分配案例',
    total: [10, 5, 7],
    available: [3, 3, 2],
    allocated: [
      [0, 1, 0],
      [2, 0, 0],
      [3, 0, 2],
      [2, 1, 1],
      [0, 0, 2]
    ],
    need: [
      [7, 4, 3],
      [1, 2, 2],
      [6, 0, 0],
      [0, 1, 1],
      [4, 3, 1]
    ],
    requestProcess: 1,
    requestResource: [1, 0, 2]
  },
  unsafe: {
    name: '不安全案例',
    total: [6, 4, 4],
    available: [1, 1, 0],
    allocated: [
      [3, 1, 2],
      [1, 2, 1],
      [1, 0, 1]
    ],
    need: [
      [2, 2, 1],
      [3, 1, 2],
      [4, 3, 2]
    ],
    requestProcess: 0,
    requestResource: [2, 1, 1]
  },
  insufficient: {
    name: '资源不足案例',
    total: [8, 5, 6],
    available: [1, 1, 1],
    allocated: [
      [2, 1, 2],
      [3, 2, 1],
      [2, 1, 2]
    ],
    need: [
      [4, 2, 3],
      [2, 1, 2],
      [3, 2, 1]
    ],
    requestProcess: 0,
    requestResource: [3, 2, 2]
  },
  invalid: {
    name: '请求不合理案例',
    total: [7, 4, 5],
    available: [2, 2, 2],
    allocated: [
      [1, 1, 1],
      [2, 1, 2],
      [1, 1, 0]
    ],
    need: [
      [3, 2, 2],
      [2, 1, 1],
      [4, 2, 3]
    ],
    requestProcess: 0,
    requestResource: [5, 3, 3] // 超出需求
  }
}

// 加载选中的示例
const loadSelectedExample = () => {
  console.log('开始加载示例:', selectedExample.value)

  if (!selectedExample.value) {
    message.error('请选择一个预设案例')
    return
  }

  loading.value = true
  console.log('加载状态设置为true')

  try {
    const example = examples[selectedExample.value as keyof typeof examples]
    console.log('找到示例:', example)

    if (!example) {
      message.error('找不到指定的示例')
      loading.value = false
      return
    }

    // 立即加载数据，去掉不必要的延迟
    systemData.total = [...example.total]
    systemData.available = [...example.available]
    systemData.allocated = example.allocated.map(row => [...row])
    systemData.need = example.need.map(row => [...row])

    processCount.value = example.allocated.length
    resourceCount.value = example.total.length
    requestData.request = [...example.requestResource]
    requestData.processIndex = example.requestProcess

    resetSimulation()
    loading.value = false
    console.log('加载完成，加载状态设置为false')

    // 添加到历史记录
    operationHistory.value.push({
      time: new Date().toLocaleTimeString(),
      action: `加载${example.name}`,
      result: '成功',
      type: 'info'
    })

    message.success(`${example.name}加载完成！`)
  } catch (error) {
    console.error('加载示例失败:', error)
    message.error('加载示例失败，请重试')
    loading.value = false
  }
}

// 更新数据结构
const updateDataStructure = () => {
  // 重置所有数据
  systemData.total = new Array(resourceCount.value).fill(0)
  systemData.available = new Array(resourceCount.value).fill(0)
  systemData.allocated = Array.from({ length: processCount.value }, () =>
    new Array(resourceCount.value).fill(0)
  )
  systemData.need = Array.from({ length: processCount.value }, () =>
    new Array(resourceCount.value).fill(0)
  )
  requestData.request = new Array(resourceCount.value).fill(0)
  requestData.processIndex = null

  resetSimulation()
  message.info('数据结构已更新，请手动填入数据或加载示例')
}

// 重置模拟
const resetSimulation = () => {
  simulationStarted.value = false
  simulationStep.value = 0
  simulationComplete.value = false

  step1Result.value = null
  step1Message.value = ''
  step2Result.value = null
  step2Message.value = ''
  step3Result.value = null
  step3Message.value = ''

  // 重置动画和检查状态
  step1Checking.value = false
  step2Checking.value = false
  step3Checking.value = false
  step1Details.value = []
  step2Details.value = []
  step1MatrixCalculation.value = null
  step2MatrixCalculation.value = null
  safetyCheckProgress.value = 0
  safetyCheckProgressText.value = ''
  completedProcesses.value = []
  currentProcessIndex.value = null

  tempState.value = null
  safetyCheckSteps.value = []
  safeSequence.value = []

  finalResult.value = false
  finalMessage.value = ''
}

// 自动执行模拟配置
const autoExecution = ref(true)
const executionSpeed = ref(2000) // 默认2秒间隔
const isExecuting = ref(false)

// 开始模拟
const startSimulation = () => {
  if (requestData.processIndex === null) {
    message.error('请选择一个进程')
    return
  }

  resetSimulation()
  simulationStarted.value = true
  simulationStep.value = 1
  isExecuting.value = true
  message.info('开始模拟银行家算法...')

  // 自动执行或手动执行
  if (autoExecution.value) {
    autoExecuteSimulation()
  }
}

// 自动执行模拟流程
const autoExecuteSimulation = async () => {
  try {
    // 步骤1：检查请求合理性
    await new Promise(resolve => setTimeout(resolve, 500))
    await checkStep1Auto()

    if (step1Result.value === true) {
      await new Promise(resolve => setTimeout(resolve, executionSpeed.value))
      await checkStep2Auto()

      if (step2Result.value === true) {
        await new Promise(resolve => setTimeout(resolve, executionSpeed.value))
        await checkStep3Auto()
      }
    }
  } catch (error) {
    console.error('自动执行失败:', error)
    message.error('自动执行过程中出现错误')
  } finally {
    isExecuting.value = false
  }
}

// 暂停/恢复自动执行
const pauseExecution = () => {
  isExecuting.value = false
}

const resumeExecution = () => {
  if (simulationStarted.value && !simulationComplete.value) {
    isExecuting.value = true
    autoExecuteSimulation()
  }
}

// 检查步骤1：请求合理性
const checkStep1 = () => {
  step1Checking.value = true
  step1Details.value = []

  setTimeout(() => {
    const processIndex = requestData.processIndex!
    const request = requestData.request
    const need = systemData.need[processIndex]

    // 准备矩阵运算数据
    const comparisons = []
    let allValid = true

    // 逐个检查资源并记录矩阵运算
    for (let i = 0; i < request.length; i++) {
      const valid = request[i] <= need[i]
      comparisons.push({
        index: i,
        request: request[i],
        need: need[i],
        valid: valid
      })

      const detail = {
        type: valid ? 'success' : 'error',
        message: `R${i + 1}: 请求${request[i]} ${valid ? '≤' : '>'} 需求${need[i]} - ${valid ? '✅ 合理' : '❌ 不合理'}`
      }
      step1Details.value.push(detail)

      if (!valid) {
        allValid = false
      }
    }

    // 设置矩阵运算展示数据
    step1MatrixCalculation.value = {
      processIndex: processIndex,
      request: [...request],
      need: [...need],
      comparisons: comparisons,
      allValid: allValid
    }

    step1Result.value = allValid
    if (allValid) {
      step1Message.value = '✅ 请求合理：所有资源请求都不超过进程需求，可以继续下一步检查'
      simulationStep.value = 2

      // 添加历史记录
      operationHistory.value.push({
        time: new Date().toLocaleTimeString(),
        action: `P${processIndex}请求合理性检查`,
        result: '通过',
        type: 'success'
      })
    } else {
      step1Message.value = '❌ 请求不合理：存在资源请求超出进程总需求，拒绝分配'
      simulationComplete.value = true
      finalResult.value = false
      finalMessage.value = '分配失败：进程请求的资源超出了它的总需求量（贪心请求）'

      // 添加历史记录
      operationHistory.value.push({
        time: new Date().toLocaleTimeString(),
        action: `P${processIndex}请求合理性检查`,
        result: '失败 - 请求超出需求',
        type: 'error'
      })
    }

    step1Checking.value = false
  }, 1000)
}

// 检查步骤2：资源可用性
const checkStep2 = () => {
  step2Checking.value = true
  step2Details.value = []

  setTimeout(() => {
    const request = requestData.request
    const available = systemData.available

    // 准备矩阵运算数据
    const comparisons = []
    let allSufficient = true

    // 逐个检查资源可用性并记录矩阵运算
    for (let i = 0; i < request.length; i++) {
      const sufficient = request[i] <= available[i]
      comparisons.push({
        index: i,
        request: request[i],
        available: available[i],
        sufficient: sufficient
      })

      const detail = {
        type: sufficient ? 'success' : 'error',
        message: `R${i + 1}: 请求${request[i]} ${sufficient ? '≤' : '>'} 可用${available[i]} - ${sufficient ? '✅ 足够' : '❌ 不足'}`
      }
      step2Details.value.push(detail)

      if (!sufficient) {
        allSufficient = false
      }
    }

    // 设置矩阵运算展示数据
    step2MatrixCalculation.value = {
      processIndex: requestData.processIndex!,
      request: [...request],
      available: [...available],
      comparisons: comparisons,
      allSufficient: allSufficient
    }

    step2Result.value = allSufficient
    if (allSufficient) {
      step2Message.value = '✅ 资源充足：系统具有足够的可用资源，可以尝试分配'
      simulationStep.value = 3

      // 添加历史记录
      operationHistory.value.push({
        time: new Date().toLocaleTimeString(),
        action: `资源可用性检查`,
        result: '通过',
        type: 'success'
      })
    } else {
      step2Message.value = '❌ 资源不足：系统没有足够的可用资源，拒绝分配'
      simulationComplete.value = true
      finalResult.value = false
      finalMessage.value = '分配失败：系统没有足够的可用资源满足请求'

      // 添加历史记录
      operationHistory.value.push({
        time: new Date().toLocaleTimeString(),
        action: `资源可用性检查`,
        result: '失败 - 资源不足',
        type: 'error'
      })
    }

    step2Checking.value = false
  }, 1000)
}

// 数据验证函数
const validateTotalResource = (index: number) => {
  if (systemData.total[index] < 1) {
    systemData.total[index] = 1
    message.warning(`资源R${index + 1}的总量不能小于1`)
  }
  // 更新可用资源的最大值
  if (systemData.available[index] > systemData.total[index]) {
    systemData.available[index] = systemData.total[index]
  }
}

const validateAvailableResource = (index: number) => {
  if (systemData.available[index] > systemData.total[index]) {
    systemData.available[index] = systemData.total[index]
    message.warning(`可用资源R${index + 1}不能超过总资源数量`)
  }
}

const validateAllocatedResource = (pIndex: number, rIndex: number) => {
  if (systemData.allocated[pIndex][rIndex] > systemData.total[rIndex]) {
    systemData.allocated[pIndex][rIndex] = systemData.total[rIndex]
    message.warning(`进程P${pIndex}的R${rIndex + 1}已分配资源不能超过总资源`)
  }
}

const validateNeedResource = (pIndex: number, rIndex: number) => {
  if (systemData.need[pIndex][rIndex] > systemData.total[rIndex]) {
    systemData.need[pIndex][rIndex] = systemData.total[rIndex]
    message.warning(`进程P${pIndex}的R${rIndex + 1}需求资源不能超过总资源`)
  }
}

const validateAllData = () => {
  validating.value = true
  validationMessage.value = ''

  setTimeout(() => {
    const errors: string[] = []

    // 检查数据一致性
    for (let p = 0; p < systemData.allocated.length; p++) {
      for (let r = 0; r < systemData.total.length; r++) {
        const allocated = systemData.allocated[p][r]
        const need = systemData.need[p][r]
        const total = systemData.total[r]

        if (allocated + need > total) {
          errors.push(`进程P${p}的R${r + 1}: 已分配(${allocated}) + 需求(${need}) > 总资源(${total})`)
        }
      }
    }

    // 检查可用资源合理性
    for (let r = 0; r < systemData.total.length; r++) {
      const totalAllocated = systemData.allocated.reduce((sum, process) => sum + process[r], 0)
      const available = systemData.available[r]
      const total = systemData.total[r]

      if (totalAllocated + available !== total) {
        errors.push(`R${r + 1}: 已分配总量(${totalAllocated}) + 可用(${available}) ≠ 总资源(${total})`)
      }
    }

    if (errors.length === 0) {
      dataValid.value = true
      validationMessage.value = '✅ 数据验证通过，可以应用'
    } else {
      dataValid.value = false
      validationMessage.value = `❌ 数据验证失败: ${errors.join('; ')}`
    }

    validating.value = false
  }, 1000)
}

const applyCustomData = () => {
  if (!dataValid.value) {
    message.error('请先验证数据')
    return
  }

  resetSimulation()
  message.success('自定义数据应用成功！')

  operationHistory.value.push({
    time: new Date().toLocaleTimeString(),
    action: '应用自定义数据',
    result: '成功',
    type: 'info'
  })
}

const saveToHistory = () => {
  const historyData = {
    timestamp: new Date().toLocaleString(),
    systemData: JSON.parse(JSON.stringify(systemData)),
    request: [...requestData.request],
    processIndex: requestData.processIndex,
    result: finalResult.value,
    safeSequence: [...safeSequence.value]
  }

  localStorage.setItem(`bankers_history_${Date.now()}`, JSON.stringify(historyData))
  message.success('模拟结果已保存到本地历史')
}

// 检查步骤1：请求合理性（自动版本）
const checkStep1Auto = () => {
  return new Promise<void>((resolve) => {
    step1Checking.value = true
    step1Details.value = []

    setTimeout(() => {
      const processIndex = requestData.processIndex!
      const request = requestData.request
      const need = systemData.need[processIndex]

      // 准备矩阵运算数据
      const comparisons = []
      let allValid = true

      // 逐个检查资源并记录矩阵运算
      for (let i = 0; i < request.length; i++) {
        const valid = request[i] <= need[i]
        comparisons.push({
          index: i,
          request: request[i],
          need: need[i],
          valid: valid
        })

        const detail = {
          type: valid ? 'success' : 'error',
          message: `R${i + 1}: 请求${request[i]} ${valid ? '≤' : '>'} 需求${need[i]} - ${valid ? '✅ 合理' : '❌ 不合理'}`
        }
        step1Details.value.push(detail)

        if (!valid) {
          allValid = false
        }
      }

      // 设置矩阵运算展示数据
      step1MatrixCalculation.value = {
        processIndex: processIndex,
        request: [...request],
        need: [...need],
        comparisons: comparisons,
        allValid: allValid
      }

      step1Result.value = allValid
      if (allValid) {
        step1Message.value = '✅ 请求合理：所有资源请求都不超过进程需求，可以继续下一步检查'
        simulationStep.value = 2

        // 添加历史记录
        operationHistory.value.push({
          time: new Date().toLocaleTimeString(),
          action: `P${processIndex}请求合理性检查`,
          result: '通过',
          type: 'success'
        })
      } else {
        step1Message.value = '❌ 请求不合理：存在资源请求超出进程总需求，拒绝分配'
        simulationComplete.value = true
        finalResult.value = false
        finalMessage.value = '分配失败：进程请求的资源超出了它的总需求量（贪心请求）'

        // 添加历史记录
        operationHistory.value.push({
          time: new Date().toLocaleTimeString(),
          action: `P${processIndex}请求合理性检查`,
          result: '失败 - 请求超出需求',
          type: 'error'
        })
      }

      step1Checking.value = false
      resolve()
    }, 1000)
  })
}

// 检查步骤2：资源可用性（自动版本）
const checkStep2Auto = () => {
  return new Promise<void>((resolve) => {
    step2Checking.value = true
    step2Details.value = []

    setTimeout(() => {
      const request = requestData.request
      const available = systemData.available

      // 准备矩阵运算数据
      const comparisons = []
      let allSufficient = true

      // 逐个检查资源可用性并记录矩阵运算
      for (let i = 0; i < request.length; i++) {
        const sufficient = request[i] <= available[i]
        comparisons.push({
          index: i,
          request: request[i],
          available: available[i],
          sufficient: sufficient
        })

        const detail = {
          type: sufficient ? 'success' : 'error',
          message: `R${i + 1}: 请求${request[i]} ${sufficient ? '≤' : '>'} 可用${available[i]} - ${sufficient ? '✅ 足够' : '❌ 不足'}`
        }
        step2Details.value.push(detail)

        if (!sufficient) {
          allSufficient = false
        }
      }

      // 设置矩阵运算展示数据
      step2MatrixCalculation.value = {
        processIndex: requestData.processIndex!,
        request: [...request],
        available: [...available],
        comparisons: comparisons,
        allSufficient: allSufficient
      }

      step2Result.value = allSufficient
      if (allSufficient) {
        step2Message.value = '✅ 资源充足：系统具有足够的可用资源，可以尝试分配'
        simulationStep.value = 3

        // 添加历史记录
        operationHistory.value.push({
          time: new Date().toLocaleTimeString(),
          action: `资源可用性检查`,
          result: '通过',
          type: 'success'
        })
      } else {
        step2Message.value = '❌ 资源不足：系统没有足够的可用资源，拒绝分配'
        simulationComplete.value = true
        finalResult.value = false
        finalMessage.value = '分配失败：系统没有足够的可用资源满足请求'

        // 添加历史记录
        operationHistory.value.push({
          time: new Date().toLocaleTimeString(),
          action: `资源可用性检查`,
          result: '失败 - 资源不足',
          type: 'error'
        })
      }

      step2Checking.value = false
      resolve()
    }, 1000)
  })
}

// 检查步骤3：安全性检查（自动版本）
const checkStep3Auto = () => {
  return new Promise<void>((resolve) => {
    step3Checking.value = true
    safetyCheckProgress.value = 0
    safetyCheckProgressText.value = '准备安全检查...'

    setTimeout(() => {
      const processIndex = requestData.processIndex!
      const request = requestData.request

      safetyCheckProgress.value = 20
      safetyCheckProgressText.value = '创建临时状态...'

      // 创建临时状态（模拟分配后的状态）
      tempState.value = {
        available: [...systemData.available],
        allocated: systemData.allocated.map(row => [...row]),
        need: systemData.need.map(row => [...row])
      }

      // 模拟分配
      for (let i = 0; i < request.length; i++) {
        tempState.value.available[i] -= request[i]
        tempState.value.allocated[processIndex][i] += request[i]
        tempState.value.need[processIndex][i] -= request[i]
      }

      safetyCheckProgress.value = 40
      safetyCheckProgressText.value = '执行安全性检查...'

      setTimeout(() => {
        // 执行安全性检查
        const safetyResult = checkSafety(tempState.value!)

        safetyCheckProgress.value = 100
        safetyCheckProgressText.value = '安全检查完成'

        step3Result.value = safetyResult.isSafe
        if (safetyResult.isSafe) {
          step3Message.value = `✅ 系统安全：找到安全序列 [${safetyResult.sequence.map(p => 'P' + p).join(' → ')}]`
          safeSequence.value = safetyResult.sequence
          finalResult.value = true
          finalMessage.value = '分配成功！系统保持在安全状态，所有进程都能按序完成'

          // 添加历史记录
          operationHistory.value.push({
            time: new Date().toLocaleTimeString(),
            action: `安全性检查`,
            result: `成功 - 安全序列: [${safetyResult.sequence.map(p => 'P' + p).join(', ')}]`,
            type: 'success'
          })
        } else {
          step3Message.value = '❌ 系统不安全：无法找到安全序列，可能导致死锁'
          finalResult.value = false
          finalMessage.value = '分配失败：分配后系统将处于不安全状态，部分进程可能永久等待资源（死锁）'

          // 添加历史记录
          operationHistory.value.push({
            time: new Date().toLocaleTimeString(),
            action: `安全性检查`,
            result: '失败 - 无安全序列',
            type: 'error'
          })
        }

        simulationComplete.value = true
        step3Checking.value = false
        resolve()
      }, 1500)
    }, 1000)
  })
}
// 检查步骤3：安全性检查
const checkStep3 = () => {
  step3Checking.value = true
  safetyCheckProgress.value = 0
  safetyCheckProgressText.value = '准备安全检查...'

  setTimeout(() => {
    const processIndex = requestData.processIndex!
    const request = requestData.request

    safetyCheckProgress.value = 20
    safetyCheckProgressText.value = '创建临时状态...'

    // 创建临时状态（模拟分配后的状态）
    tempState.value = {
      available: [...systemData.available],
      allocated: systemData.allocated.map(row => [...row]),
      need: systemData.need.map(row => [...row])
    }

    // 模拟分配
    for (let i = 0; i < request.length; i++) {
      tempState.value.available[i] -= request[i]
      tempState.value.allocated[processIndex][i] += request[i]
      tempState.value.need[processIndex][i] -= request[i]
    }

    safetyCheckProgress.value = 40
    safetyCheckProgressText.value = '执行安全性检查...'

    setTimeout(() => {
      // 执行安全性检查
      const safetyResult = checkSafety(tempState.value!)

      safetyCheckProgress.value = 100
      safetyCheckProgressText.value = '安全检查完成'

      step3Result.value = safetyResult.isSafe
      if (safetyResult.isSafe) {
        step3Message.value = `✅ 系统安全：找到安全序列 [${safetyResult.sequence.map(p => 'P' + p).join(' → ')}]`
        safeSequence.value = safetyResult.sequence
        finalResult.value = true
        finalMessage.value = '分配成功！系统保持在安全状态，所有进程都能按序完成'

        // 添加历史记录
        operationHistory.value.push({
          time: new Date().toLocaleTimeString(),
          action: `安全性检查`,
          result: `成功 - 安全序列: [${safetyResult.sequence.map(p => 'P' + p).join(', ')}]`,
          type: 'success'
        })
      } else {
        step3Message.value = '❌ 系统不安全：无法找到安全序列，可能导致死锁'
        finalResult.value = false
        finalMessage.value = '分配失败：分配后系统将处于不安全状态，部分进程可能永久等待资源（死锁）'

        // 添加历史记录
        operationHistory.value.push({
          time: new Date().toLocaleTimeString(),
          action: `安全性检查`,
          result: '失败 - 无安全序列',
          type: 'error'
        })
      }

      simulationComplete.value = true
      step3Checking.value = false
    }, 1500)
  }, 1000)
}

// 安全性检查算法
const checkSafety = (state: SystemState) => {
  const work = [...state.available]
  const finish = new Array(state.allocated.length).fill(false)
  const sequence: number[] = []

  safetyCheckSteps.value = []
  safetyCheckSteps.value.push({
    type: 'info',
    message: `🔍 开始安全检查，初始可用资源：[${work.join(', ')}]`
  })

  let foundProcess = true
  let round = 1

  while (foundProcess && sequence.length < state.allocated.length) {
    foundProcess = false

    safetyCheckSteps.value.push({
      type: 'info',
      message: `🔄 第${round}轮搜索，当前可用资源: [${work.join(', ')}]`
    })

    for (let i = 0; i < state.allocated.length; i++) {
      if (!finish[i]) {
        // 检查进程i是否可以完成 - 详细矩阵运算
        let canFinish = true
        const needCheckResults = []

        for (let j = 0; j < state.need[i].length; j++) {
          const canSatisfy = state.need[i][j] <= work[j]
          needCheckResults.push({
            resourceIndex: j,
            need: state.need[i][j],
            work: work[j],
            satisfied: canSatisfy
          })
          if (!canSatisfy) canFinish = false
        }

        // 创建矩阵运算数据
        const matrixCalculation = {
          processIndex: i,
          needCheck: {
            need: [...state.need[i]],
            work: [...work],
            results: needCheckResults
          },
          workUpdate: null as {
            oldWork: number[]
            allocated: number[]
            newWork: number[]
            calculations: Array<{
              resourceIndex: number
              oldWork: number
              allocated: number
              newWork: number
            }>
          } | null
        }

        const needCheck = needCheckResults.map(result =>
          `R${result.resourceIndex + 1}:需求${result.need}${result.satisfied ? '≤' : '>'}Work${result.work}`
        ).join(', ')

        if (canFinish) {
          // 进程i可以完成
          finish[i] = true
          sequence.push(i)
          completedProcesses.value.push(i)
          currentProcessIndex.value = i

          // 计算Work更新的详细过程
          const beforeWork = [...work]
          const calculations = []
          for (let j = 0; j < state.allocated[i].length; j++) {
            const oldWork = work[j]
            const allocated = state.allocated[i][j]
            work[j] += allocated
            calculations.push({
              resourceIndex: j,
              oldWork: oldWork,
              allocated: allocated,
              newWork: work[j]
            })
          }

          // 完善矩阵运算数据
          matrixCalculation.workUpdate = {
            oldWork: [...beforeWork],
            allocated: [...state.allocated[i]],
            newWork: [...work],
            calculations: calculations
          }

          safetyCheckSteps.value.push({
            type: 'success',
            message: `✅ 进程P${i}可以完成! ${needCheck}`,
            available: [...work],
            matrixCalculation: matrixCalculation
          })

          safetyCheckSteps.value.push({
            type: 'success',
            message: `🔄 P${i}释放资源: [${state.allocated[i].join(', ')}], Work更新: [${beforeWork.join(', ')}] → [${work.join(', ')}]`,
            available: [...work]
          })

          foundProcess = true
          break
        } else {
          safetyCheckSteps.value.push({
            type: 'info',
            message: `⚠️ 进程P${i}暂时无法完成: ${needCheck}`,
            matrixCalculation: matrixCalculation
          })
        }
      }
    }

    round++

    if (!foundProcess && sequence.length < state.allocated.length) {
      const unfinishedProcesses = state.allocated
        .map((_, index) => !finish[index] ? `P${index}` : null)
        .filter(p => p !== null)
        .join(', ')

      safetyCheckSteps.value.push({
        type: 'error',
        message: `❌ 没有进程可以完成，系统处于不安全状态! 未完成进程: ${unfinishedProcesses}`
      })
    }
  }

  if (sequence.length === state.allocated.length) {
    safetyCheckSteps.value.push({
      type: 'success',
      message: `🎉 找到安全序列: [${sequence.map(p => 'P' + p).join(' → ')}]`
    })
  }

  return {
    isSafe: sequence.length === state.allocated.length,
    sequence: sequence
  }
}

// 应用分配
const applyAllocation = () => {
  if (tempState.value) {
    systemData.available = [...tempState.value.available]
    systemData.allocated = tempState.value.allocated.map((row: number[]) => [...row])
    systemData.need = tempState.value.need.map((row: number[]) => [...row])

    message.success('资源分配已应用到系统！')
    resetSimulation()
  }
}

// 组件挂载时初始化
onMounted(() => {
  // 可以在这里添加初始化逻辑
})
</script>

<style scoped>
/* 页面整体样式 */
.bankers-algorithm-page {
  padding: 24px;
  background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 50%, #f1f5f9 100%);
  min-height: 100vh;
  position: relative;
}

.bankers-algorithm-page::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background:
    radial-gradient(circle at 20% 30%, rgba(59, 130, 246, 0.04) 0%, transparent 50%),
    radial-gradient(circle at 80% 70%, rgba(16, 185, 129, 0.03) 0%, transparent 50%),
    radial-gradient(circle at 40% 80%, rgba(139, 92, 246, 0.03) 0%, transparent 50%);
  pointer-events: none;
}

.page-header {
  text-align: center;
  margin-bottom: 32px;
  position: relative;
  z-index: 1;
}

.page-header h1 {
  color: #334155;
  font-size: 3em;
  margin-bottom: 8px;
  text-shadow: 1px 1px 3px rgba(0, 0, 0, 0.08);
  font-weight: 700;
  background: linear-gradient(45deg, #1e293b, #3b82f6);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.subtitle {
  color: #64748b;
  font-size: 1.3em;
  text-shadow: 0.5px 0.5px 1px rgba(0, 0, 0, 0.05);
}

/* 理论部分样式 */
.theory-section {
  margin-bottom: 48px;
  position: relative;
  z-index: 1;
}

.theory-card {
  margin-bottom: 24px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
  border-radius: 16px;
  overflow: hidden;
  backdrop-filter: blur(10px);
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid rgba(255, 255, 255, 0.2);
  transition: all 0.3s ease;
}

.theory-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.2);
}

.theory-card .ant-card-head {
  background: linear-gradient(135deg, #e0e7ff, #c7d2fe);
  border-bottom: none;
}

.theory-card .ant-card-head-title {
  color: #4338ca;
  font-weight: 600;
  text-shadow: 0.5px 0.5px 1px rgba(0, 0, 0, 0.05);
}

.simulation-card {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
  border-radius: 16px;
  overflow: hidden;
  backdrop-filter: blur(10px);
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid rgba(255, 255, 255, 0.2);
  position: relative;
  z-index: 1;
}

.simulation-card .ant-card-head {
  background: linear-gradient(135deg, #dbeafe, #bfdbfe);
  border-bottom: none;
}

.simulation-card .ant-card-head-title {
  color: #1d4ed8;
  font-weight: 600;
  text-shadow: 0.5px 0.5px 1px rgba(0, 0, 0, 0.05);
}

.principle {
  text-align: center;
}

.highlight-text {
  font-size: 1.1em;
  line-height: 1.6;
  color: #333;
  margin-bottom: 16px;
}

.analogy {
  background: #e6f7ff;
  padding: 16px;
  border-radius: 8px;
  border-left: 4px solid #1890ff;
}

.analogy-icon {
  font-size: 1.5em;
  margin-right: 8px;
}

.concepts {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

.concept-item {
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
  border-left: 4px solid #52c41a;
}

.concept-item h4 {
  color: #52c41a;
  margin-bottom: 8px;
}

.example {
  font-size: 0.9em;
  color: #666;
  font-style: italic;
}

.data-structures .structure-item {
  text-align: center;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #d9d9d9;
  margin-bottom: 16px;
}

.example-data {
  font-family: 'Courier New', monospace;
  background: #f0f0f0;
  padding: 8px;
  border-radius: 4px;
  margin-top: 8px;
  color: #1890ff;
  font-weight: bold;
}

.steps {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.step-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.step-number {
  width: 40px;
  height: 40px;
  background: #1890ff;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  flex-shrink: 0;
}

.step-content h4 {
  color: #1890ff;
  margin-bottom: 8px;
}

.warning {
  color: #ff4d4f;
}

.highlight {
  color: #52c41a;
  font-weight: bold;
}

/* 操作指南样式 */
.guide-section {
  margin-bottom: 24px;
  margin-top: 12px;
}

/* 自定义输入增强样式 */
.structure-config {
  background: #f8f9fa;
  padding: 16px;
  border-radius: 8px;
  margin-bottom: 20px;
  border-left: 4px solid #1890ff;
}

.data-input-forms {
  margin-top: 20px;
}

.input-section {
  background: #fff;
  padding: 16px;
  border-radius: 8px;
  margin-bottom: 16px;
  border: 1px solid #e8e8e8;
}

.input-section h4 {
  color: #1890ff;
  margin-bottom: 12px;
  font-weight: 600;
}

.input-section h5 {
  color: #52c41a;
  margin-bottom: 8px;
}

.validation-section {
  text-align: center;
  padding: 16px;
  background: #f6f6f6;
  border-radius: 8px;
  margin-top: 16px;
}

/* 进程可视化样式 */
.process-visualization {
  margin: 24px 0;
}

.process-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.process-card {
  background: #fff;
  border: 2px solid #e8e8e8;
  border-radius: 12px;
  padding: 16px;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.process-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: #e8e8e8;
  transition: all 0.3s ease;
}

.process-card.requesting {
  border-color: #1890ff;
  background: #e6f7ff;
  animation: requestingPulse 2s infinite;
}

.process-card.requesting::before {
  background: #1890ff;
}

.process-card.completed {
  border-color: #52c41a;
  background: #f6ffed;
}

.process-card.completed::before {
  background: #52c41a;
}

.process-card.current {
  border-color: #fa8c16;
  background: #fff7e6;
  transform: scale(1.02);
  box-shadow: 0 4px 12px rgba(250, 140, 22, 0.2);
}

.process-card.current::before {
  background: #fa8c16;
}

@keyframes requestingPulse {
  0%, 100% {
    box-shadow: 0 0 0 0 rgba(24, 144, 255, 0.4);
  }
  50% {
    box-shadow: 0 0 0 8px rgba(24, 144, 255, 0);
  }
}

.process-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.process-name {
  font-weight: bold;
  font-size: 18px;
  color: #1890ff;
}

.requesting-badge {
  background: #1890ff;
  color: white;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
  animation: badgePulse 1s infinite;
}

.completed-badge {
  background: #52c41a;
  color: white;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
}

@keyframes badgePulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.1); }
}

.process-resources {
  font-size: 14px;
}

.resource-row {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.resource-label {
  font-weight: 600;
  min-width: 70px;
  color: #666;
}

.resource-values {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.resource-values.allocated .resource-value {
  background: #f6ffed;
  color: #52c41a;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
}

.resource-values.need .resource-value {
  background: #fff2e8;
  color: #fa8c16;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
}

/* 资源池样式 */
.resource-pool {
  background: #f8f9fa;
  padding: 20px;
  border-radius: 12px;
  border: 2px dashed #d9d9d9;
}

.resource-pool h5 {
  text-align: center;
  color: #1890ff;
  margin-bottom: 16px;
  font-size: 16px;
  font-weight: 600;
}

.resource-pool-container {
  display: flex;
  justify-content: center;
  gap: 20px;
  flex-wrap: wrap;
}

.resource-pool-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  background: #fff;
  padding: 16px;
  border-radius: 8px;
  border: 1px solid #e8e8e8;
  min-width: 80px;
  transition: all 0.3s ease;
}

.resource-pool-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0,0,0,0.1);
}

.resource-icon {
  width: 40px;
  height: 40px;
  background: #1890ff;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  margin-bottom: 8px;
}

.resource-amount {
  font-size: 20px;
  font-weight: bold;
  color: #52c41a;
  transition: all 0.3s ease;
}

.resource-amount.insufficient {
  color: #ff4d4f;
  animation: insufficientShake 0.5s ease-in-out infinite;
}

@keyframes insufficientShake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-2px); }
  75% { transform: translateX(2px); }
}

.resource-total {
  font-size: 14px;
  color: #666;
}

/* 执行设置样式 */
.execution-settings {
  background: linear-gradient(135deg, #f5f7ff, #e8f2ff);
  padding: 20px;
  border-radius: 12px;
  margin-top: 16px;
  border: 1px solid rgba(24, 144, 255, 0.1);
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.05);
}

.execution-settings .ant-form-item-label > label {
  font-weight: 600;
  color: #1890ff;
}

.execution-settings .ant-radio-wrapper {
  color: #333;
  font-weight: 500;
}

.execution-settings .ant-slider {
  margin: 8px 0;
}

.execution-settings .ant-slider-track {
  background: linear-gradient(90deg, #52c41a, #1890ff, #f5222d);
}

.execution-settings .ant-slider-handle {
  border-color: #1890ff;
  box-shadow: 0 0 0 3px rgba(24, 144, 255, 0.2);
}

/* 请求控制增强样式 */
.request-controls {
  background: linear-gradient(135deg, #f9f9f9, #ffffff);
  padding: 24px;
  border-radius: 12px;
  margin-bottom: 20px;
  border: 1px solid #e8e8e8;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.request-controls h4 {
  color: #1890ff;
  font-weight: 600;
  margin-bottom: 12px;
  font-size: 16px;
}

.request-controls .ant-select {
  border-radius: 8px;
}

.request-controls .ant-input-number {
  border-radius: 6px;
  border-color: #d9d9d9;
  transition: all 0.3s ease;
}

.request-controls .ant-input-number:hover {
  border-color: #40a9ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.1);
}

.request-controls .ant-btn {
  border-radius: 8px;
  font-weight: 600;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
}

.request-controls .ant-btn-primary {
  background: linear-gradient(135deg, #1890ff, #40a9ff);
  border: none;
}

.request-controls .ant-btn-primary:hover {
  background: linear-gradient(135deg, #40a9ff, #1890ff);
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(24, 144, 255, 0.3);
}

.process-option {
  display: flex;
  align-items: center;
  gap: 8px;
}

.process-status {
  font-size: 12px;
  color: #666;
}

.request-input-item {
  display: flex;
  align-items: center;
  gap: 4px;
  background: #fff;
  padding: 4px 8px;
  border-radius: 6px;
  border: 1px solid #e8e8e8;
}

.request-preview {
  margin-top: 16px;
}

/* 动画增强样式 */
.animated-comparison {
  background: #f8f9fa;
  padding: 16px;
  border-radius: 8px;
  border: 1px solid #e8e8e8;
}

.animated-comparison .comparison-item {
  transition: all 0.3s ease;
  position: relative;
}

.animated-comparison .comparison-item.highlight {
  background: #e6f7ff;
  transform: scale(1.02);
}

.comparison-label {
  font-weight: 600;
  color: #666;
  margin-right: 8px;
}

.comparison-value {
  font-family: 'Courier New', monospace;
  font-weight: bold;
  color: #1890ff;
}

.comparison-operator.pulse {
  animation: operatorPulse 1s ease-in-out;
}

/* 最终结果样式美化 */
.final-result {
  position: relative;
  z-index: 1;
  animation: finalResultSlideIn 0.8s ease;
}

.result-card {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 20px;
  padding: 40px;
  text-align: center;
  backdrop-filter: blur(15px);
  border: 2px solid transparent;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.15);
  position: relative;
  overflow: hidden;
  transition: all 0.4s ease;
}

.result-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, transparent, rgba(255, 255, 255, 0.1));
  pointer-events: none;
}

.result-card.success {
  border-color: rgba(82, 196, 26, 0.3);
  background: linear-gradient(135deg, rgba(246, 255, 237, 0.95), rgba(217, 247, 190, 0.95));
}

.result-card.error {
  border-color: rgba(255, 77, 79, 0.3);
  background: linear-gradient(135deg, rgba(255, 242, 240, 0.95), rgba(255, 204, 199, 0.95));
}

.result-icon {
  font-size: 64px;
  margin-bottom: 20px;
  animation: resultIconPulse 2s infinite;
}

.result-card.success .result-icon {
  animation: successIconBounce 1s ease;
}

.result-card.error .result-icon {
  animation: errorIconShake 1s ease;
}

@keyframes finalResultSlideIn {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes resultIconPulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.05); }
}

@keyframes successIconBounce {
  0%, 20%, 50%, 80%, 100% { transform: translateY(0); }
  40% { transform: translateY(-10px); }
  60% { transform: translateY(-5px); }
}

@keyframes errorIconShake {
  0%, 100% { transform: translateX(0); }
  10%, 30%, 50%, 70%, 90% { transform: translateX(-5px); }
  20%, 40%, 60%, 80% { transform: translateX(5px); }
}

.result-content h3 {
  font-size: 28px;
  margin-bottom: 16px;
  font-weight: 700;
}

.result-card.success .result-content h3 {
  color: #389e0d;
  text-shadow: 1px 1px 2px rgba(56, 158, 13, 0.1);
}

.result-card.error .result-content h3 {
  color: #cf1322;
  text-shadow: 1px 1px 2px rgba(207, 19, 34, 0.1);
}

.result-content p {
  font-size: 16px;
  line-height: 1.6;
  margin-bottom: 24px;
  color: #666;
}

.result-actions {
  margin-top: 24px;
}

.result-actions .ant-btn {
  border-radius: 10px;
  height: 40px;
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
}

.result-actions .ant-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

/* 响应式适配 */
@media screen and (max-width: 768px) {
  .matrix-equation {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .comparison-results {
    flex-direction: column;
    gap: 8px;
  }

  .calc-comparison, .calc-equation {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }

  .calc-details {
    flex-direction: column;
    gap: 4px;
  }
}

/* 模拟步骤样式美化 */
.simulation-steps {
  position: relative;
  z-index: 1;
}

.step-card {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 16px;
  margin-bottom: 24px;
  overflow: hidden;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  border: 2px solid transparent;
  backdrop-filter: blur(10px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  position: relative;
}

.step-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: #e8e8e8;
  transition: all 0.3s ease;
}

.step-card.active {
  transform: translateX(8px);
  border-color: rgba(24, 144, 255, 0.3);
  box-shadow: 0 8px 24px rgba(24, 144, 255, 0.15);
}

.step-card.active::before {
  background: linear-gradient(90deg, #1890ff, #40a9ff);
}

.step-card.success {
  border-color: rgba(82, 196, 26, 0.3);
  box-shadow: 0 8px 24px rgba(82, 196, 26, 0.15);
}

.step-card.success::before {
  background: linear-gradient(90deg, #52c41a, #73d13d);
}

.step-card.error {
  border-color: rgba(255, 77, 79, 0.3);
  box-shadow: 0 8px 24px rgba(255, 77, 79, 0.15);
}

.step-card.error::before {
  background: linear-gradient(90deg, #ff4d4f, #ff7875);
}

.step-header {
  display: flex;
  align-items: center;
  padding: 20px 24px 12px;
  gap: 16px;
}

.step-badge {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 18px;
  background: linear-gradient(135deg, #1890ff, #40a9ff);
  color: white;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.3);
  transition: all 0.3s ease;
}

.step-card.success .step-badge {
  background: linear-gradient(135deg, #52c41a, #73d13d);
  box-shadow: 0 2px 8px rgba(82, 196, 26, 0.3);
}

.step-card.error .step-badge {
  background: linear-gradient(135deg, #ff4d4f, #ff7875);
  box-shadow: 0 2px 8px rgba(255, 77, 79, 0.3);
}

.step-header h4 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
  flex: 1;
}

.step-status {
  display: flex;
  align-items: center;
}

.status-icon {
  font-size: 24px;
  animation: statusPulse 2s infinite;
}

.status-icon.success {
  color: #52c41a;
  animation: successBounce 0.6s ease;
}

.status-icon.error {
  color: #ff4d4f;
  animation: errorShake 0.6s ease;
}

.status-icon.loading {
  color: #1890ff;
  animation: loadingRotate 1s linear infinite;
}

@keyframes statusPulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}

@keyframes successBounce {
  0%, 20%, 50%, 80%, 100% { transform: translateY(0); }
  40% { transform: translateY(-8px); }
  60% { transform: translateY(-4px); }
}

@keyframes errorShake {
  0%, 100% { transform: translateX(0); }
  10%, 30%, 50%, 70%, 90% { transform: translateX(-4px); }
  20%, 40%, 60%, 80% { transform: translateX(4px); }
}

@keyframes loadingRotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.step-content {
  padding: 0 24px 24px;
}

.result-text {
  font-weight: 600;
  padding: 12px 16px;
  border-radius: 8px;
  margin: 16px 0;
  transition: all 0.3s ease;
}

.result-text.success {
  background: linear-gradient(135deg, #f6ffed, #d9f7be);
  color: #389e0d;
  border-left: 4px solid #52c41a;
}

.result-text.error {
  background: linear-gradient(135deg, #fff2f0, #ffccc7);
  color: #cf1322;
  border-left: 4px solid #ff4d4f;
}

/* 矩阵运算展示样式 */
.matrix-calculation {
  margin: 16px 0;
  background: linear-gradient(135deg, #f0f7ff, #e6f4ff);
  border-radius: 12px;
  padding: 20px;
  border-left: 4px solid #1890ff;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.1);
}

.matrix-calculation h5 {
  color: #1890ff;
  margin-bottom: 16px;
  font-weight: 600;
  text-align: center;
}

.matrix-display {
  font-family: 'Courier New', monospace;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.matrix-row {
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 12px 0;
  padding: 12px 20px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 8px;
  min-width: 100%;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
}

.matrix-label {
  font-weight: 600;
  color: #333;
  min-width: 150px;
  margin-right: 16px;
  text-align: right;
  font-size: 14px;
}

.matrix-vector {
  background: linear-gradient(135deg, #fff, #f8f9fa);
  padding: 8px 16px;
  border-radius: 6px;
  border: 2px solid #e8f4fd;
  font-weight: bold;
  color: #1890ff;
  min-width: 120px;
  text-align: center;
  font-size: 14px;
  box-shadow: 0 1px 3px rgba(24, 144, 255, 0.1);
}

.matrix-operator {
  margin: 0 20px;
  font-size: 20px;
  font-weight: bold;
  color: #52c41a;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  background: rgba(82, 196, 26, 0.1);
  border-radius: 50%;
}

.matrix-result {
  margin-top: 16px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 8px;
  border-top: 2px solid #e8e8e8;
  width: 100%;
}

.comparison-results {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin: 12px 0;
  flex-wrap: wrap;
}

.comparison-result {
  padding: 8px 16px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 600;
  text-align: center;
  min-width: 140px;
  white-space: nowrap;
  overflow: visible;
  display: flex;
  align-items: center;
  justify-content: center;
}

.comparison-result.valid {
  background: linear-gradient(135deg, #f6ffed, #d9f7be);
  color: #389e0d;
  border: 1px solid #b7eb8f;
}

.comparison-result.invalid {
  background: linear-gradient(135deg, #fff2f0, #ffccc7);
  color: #cf1322;
  border: 1px solid #ffb3b3;
}

.final-result {
  margin-top: 16px;
  padding: 12px 20px;
  border-radius: 8px;
  font-weight: 600;
  text-align: center;
  font-size: 15px;
}

.final-result.success {
  background: linear-gradient(135deg, #f6ffed, #d9f7be);
  color: #389e0d;
  border: 2px solid #52c41a;
}

.final-result.error {
  background: linear-gradient(135deg, #fff2f0, #ffccc7);
  color: #cf1322;
  border: 2px solid #ff4d4f;
}

/* 步骤3矩阵运算样式 */
.matrix-calculation-step3 {
  background: linear-gradient(135deg, #f9fff9, #f0f9f0);
  border-radius: 12px;
  padding: 20px;
  margin: 16px 0;
  border-left: 4px solid #52c41a;
  box-shadow: 0 2px 8px rgba(82, 196, 26, 0.1);
}

.matrix-calculation-step3 h6 {
  color: #52c41a;
  margin-bottom: 16px;
  font-weight: 600;
  text-align: center;
}

.calculation-step {
  margin: 20px 0;
  padding: 16px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 10px;
  border: 1px solid #e8f5e8;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
}

.step-title {
  font-weight: 600;
  color: #333;
  margin-bottom: 12px;
  font-size: 15px;
  text-align: center;
  padding: 8px;
  background: linear-gradient(135deg, #f0f9f0, #e8f5e8);
  border-radius: 6px;
}

.matrix-equation {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 16px;
  margin: 16px 0;
  font-family: 'Courier New', monospace;
  padding: 16px;
  background: rgba(248, 250, 252, 0.8);
  border-radius: 8px;
}

.matrix-term {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 8px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  min-width: 140px;
}

.matrix-term .matrix-label {
  font-size: 12px;
  font-weight: 600;
  color: #666;
  text-align: center;
  margin-bottom: 4px;
}

.matrix-term .matrix-vector {
  font-size: 13px;
  padding: 6px 12px;
  min-width: 100px;
}

.calculation-details {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin-top: 12px;
  flex-wrap: wrap;
}

.calc-detail {
  background: linear-gradient(135deg, #e6f7ff, #bae7ff);
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 12px;
  color: #1890ff;
  border: 1px solid #91d5ff;
  font-family: 'Courier New', monospace;
  text-align: center;
  min-width: 100px;
}

/* 安全检查过程中的矩阵运算样式 */
.safety-matrix-calc {
  margin: 16px 0;
  background: linear-gradient(135deg, #f6ffed, #f0f9ff);
  border-radius: 10px;
  padding: 16px;
  border-left: 3px solid #52c41a;
  box-shadow: 0 2px 6px rgba(82, 196, 26, 0.1);
}

.safety-calc-title {
  font-weight: 600;
  color: #52c41a;
  margin-bottom: 12px;
  font-size: 14px;
  text-align: center;
  background: rgba(82, 196, 26, 0.1);
  padding: 8px;
  border-radius: 6px;
}

.need-check-calculation, .work-update-calculation {
  margin: 12px 0;
}

.calc-step {
  background: rgba(255, 255, 255, 0.9);
  padding: 12px;
  border-radius: 8px;
  margin: 8px 0;
  border: 1px solid #f0f0f0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.calc-label {
  font-weight: 600;
  color: #333;
  font-size: 13px;
  margin-bottom: 8px;
  display: block;
  text-align: center;
  background: rgba(248, 250, 252, 0.8);
  padding: 6px 12px;
  border-radius: 4px;
}

.calc-comparison, .calc-equation {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin: 8px 0;
  font-family: 'Courier New', monospace;
  font-size: 12px;
  flex-wrap: wrap;
  padding: 8px;
  background: rgba(250, 251, 252, 0.5);
  border-radius: 6px;
}

.calc-vector {
  background: linear-gradient(135deg, #fff, #f8f9fa);
  padding: 4px 8px;
  border-radius: 4px;
  border: 1px solid #e1e5e9;
  color: #1890ff;
  font-weight: bold;
  min-width: 80px;
  text-align: center;
}

.calc-op {
  color: #52c41a;
  font-weight: bold;
  font-size: 16px;
  margin: 0 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  background: rgba(82, 196, 26, 0.1);
  border-radius: 50%;
}

.calc-term {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 6px;
  background: white;
  border-radius: 6px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  min-width: 100px;
}

.calc-term .calc-label {
  font-size: 10px;
  margin-bottom: 2px;
  padding: 2px 6px;
  background: rgba(240, 242, 245, 0.8);
}

.calc-term .calc-vector {
  font-size: 11px;
  padding: 3px 6px;
  min-width: 70px;
}

.calc-details {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-top: 10px;
  flex-wrap: wrap;
}

.calc-detail-item {
  padding: 6px 12px;
  border-radius: 4px;
  font-size: 12px;
  font-family: 'Courier New', monospace;
  text-align: center;
  min-width: 120px;
  white-space: nowrap;
  overflow: visible;
  display: flex;
  align-items: center;
  justify-content: center;
}

.calc-detail-item.satisfied {
  background: linear-gradient(135deg, #f6ffed, #d9f7be);
  color: #389e0d;
  border: 1px solid #b7eb8f;
}

.calc-detail-item.not-satisfied {
  background: linear-gradient(135deg, #fff2f0, #ffccc7);
  color: #cf1322;
  border: 1px solid #ffb3b3;
}

.calc-detail-item:not(.satisfied):not(.not-satisfied) {
  background: linear-gradient(135deg, #e6f7ff, #bae7ff);
  color: #1890ff;
  border: 1px solid #91d5ff;
}

/* 安全检查进度样式 */
.safety-check-progress {
  margin: 16px 0;
  padding: 16px;
  background: #f0f0f0;
  border-radius: 8px;
}

.progress-text {
  text-align: center;
  margin-top: 8px;
  color: #666;
  font-weight: 500;
}

/* 最终结果增强样式 */
.animated-result {
  animation: resultSlideIn 0.5s ease-out;
}

@keyframes resultSlideIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.operation-history {
  margin-top: 16px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
}

.history-timeline {
  max-height: 200px;
  overflow-y: auto;
}

.history-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px;
  margin: 4px 0;
  border-radius: 6px;
  font-size: 14px;
}

.history-item.info {
  background: #e6f7ff;
  border-left: 3px solid #1890ff;
}

.history-item.success {
  background: #f6ffed;
  border-left: 3px solid #52c41a;
}

.history-item.error {
  background: #fff2f0;
  border-left: 3px solid #ff4d4f;
}

.history-time {
  font-size: 12px;
  color: #666;
  min-width: 80px;
}

.history-action {
  font-weight: 600;
  min-width: 120px;
}

.history-result {
  flex: 1;
  color: #333;
}

.result-actions {
  margin-top: 16px;
}

/* 模拟控制面板 */
.simulation-controls {
  position: fixed;
  bottom: 20px;
  right: 20px;
  background: #fff;
  padding: 16px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  z-index: 1000;
}

/* 响应式设计增强 */
@media (max-width: 1200px) {
  .process-cards {
    grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  }

  .resource-pool-container {
    gap: 15px;
  }
}

@media (max-width: 768px) {
  .process-cards {
    grid-template-columns: 1fr;
  }

  .resource-pool-container {
    flex-direction: column;
    align-items: center;
  }

  .request-controls .ant-row {
    flex-direction: column;
  }

  .request-controls .ant-col {
    width: 100%;
    margin-bottom: 16px;
  }

  .animated-comparison {
    flex-direction: column;
    gap: 8px;
  }

  .comparison-operator {
    text-align: center;
  }

  .simulation-controls {
    position: relative;
    bottom: auto;
    right: auto;
    margin-top: 16px;
  }
}

.resource-info {
  margin-bottom: 24px;
}

.resource-card {
  padding: 16px;
  background: #fff;
  border: 2px solid #d9d9d9;
  border-radius: 8px;
  text-align: center;
}

.resource-card.available {
  border-color: #52c41a;
  background: #f6ffed;
}

.resource-vector {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-top: 8px;
}

.resource-item {
  padding: 8px 12px;
  background: #1890ff;
  color: white;
  border-radius: 4px;
  font-weight: bold;
}

.resource-item.insufficient {
  background: #ff4d4f;
  animation: pulse 1s infinite;
}

@keyframes pulse {
  0% { opacity: 1; }
  50% { opacity: 0.5; }
  100% { opacity: 1; }
}

.process-table {
  margin: 24px 0;
}

.process-name {
  font-weight: bold;
  display: flex;
  align-items: center;
  gap: 8px;
}

.process-name.requesting {
  color: #1890ff;
  animation: highlight 2s infinite;
}

@keyframes highlight {
  0%, 100% { background-color: transparent; }
  50% { background-color: #e6f7ff; }
}

.requesting-icon {
  animation: wave 1s infinite;
}

@keyframes wave {
  0%, 100% { transform: rotate(0deg); }
  25% { transform: rotate(20deg); }
  75% { transform: rotate(-20deg); }
}

.matrix-cell {
  padding: 4px 8px;
  border-radius: 4px;
  font-weight: bold;
}

.matrix-cell.allocated {
  background: #f6ffed;
  color: #52c41a;
}

.matrix-cell.need {
  background: #fff2e8;
  color: #fa8c16;
}

.request-section {
  margin: 24px 0;
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
}

/* 模拟步骤样式 */
.simulation-steps {
  margin-top: 32px;
}

.step-card {
  margin-bottom: 24px;
  padding: 16px;
  border: 2px solid #d9d9d9;
  border-radius: 8px;
  background: #fafafa;
  transition: all 0.3s ease;
}

.step-card.active {
  border-color: #1890ff;
  background: #e6f7ff;
}

.step-card.success {
  border-color: #52c41a;
  background: #f6ffed;
}

.step-card.error {
  border-color: #ff4d4f;
  background: #fff2f0;
}

.step-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.step-badge {
  width: 32px;
  height: 32px;
  background: #1890ff;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
}

.step-header h4 {
  margin: 0;
  flex: 1;
}

.step-status {
  display: flex;
  align-items: center;
}

.status-icon {
  font-size: 1.5em;
}

.status-icon.loading {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.step-content {
  margin-left: 44px;
}

.comparison {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 12px 0;
  font-family: 'Courier New', monospace;
}

.comparison-item {
  padding: 8px 12px;
  background: #f0f0f0;
  border-radius: 4px;
  font-weight: bold;
}

.comparison-operator {
  font-size: 1.2em;
  font-weight: bold;
  color: #1890ff;
}

.result-text {
  margin: 12px 0;
  padding: 8px 12px;
  border-radius: 4px;
  font-weight: bold;
}

.result-text.success {
  background: #f6ffed;
  color: #52c41a;
  border: 1px solid #b7eb8f;
}

.result-text.error {
  background: #fff2f0;
  color: #ff4d4f;
  border: 1px solid #ffccc7;
}

/* 临时状态展示 */
.temp-state {
  margin: 16px 0;
  padding: 16px;
  background: #fff7e6;
  border: 1px solid #ffd591;
  border-radius: 8px;
}

.temp-matrices {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-top: 12px;
}

.temp-available, .temp-process {
  font-family: 'Courier New', monospace;
  font-size: 0.9em;
}

/* 安全检查步骤 */
.safety-check {
  margin: 16px 0;
}

.safety-steps {
  max-height: 300px;
  overflow-y: auto;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  padding: 12px;
  background: white;
}

.safety-step {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 8px;
  padding: 8px;
  border-radius: 4px;
}

.safety-step.success {
  background: #f6ffed;
  border-left: 4px solid #52c41a;
}

.safety-step.error {
  background: #fff2f0;
  border-left: 4px solid #ff4d4f;
}

.safety-step.info {
  background: #e6f7ff;
  border-left: 4px solid #1890ff;
}

.safety-step-index {
  font-weight: bold;
  min-width: 20px;
}

.safety-step-text {
  flex: 1;
}

.safety-step-available {
  font-family: 'Courier New', monospace;
  font-size: 0.9em;
  color: #666;
}

/* 安全序列展示 */
.safe-sequence {
  margin: 16px 0;
  padding: 16px;
  background: #f6ffed;
  border: 1px solid #b7eb8f;
  border-radius: 8px;
}

.sequence {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  flex-wrap: wrap;
}

.sequence-item {
  padding: 8px 12px;
  background: #52c41a;
  color: white;
  border-radius: 4px;
  font-weight: bold;
  display: flex;
  align-items: center;
  gap: 8px;
}

.sequence-arrow {
  color: #52c41a;
  font-size: 1.2em;
  font-weight: bold;
}

/* 最终结果 */
.final-result {
  margin-top: 32px;
}

.result-card {
  padding: 24px;
  border-radius: 12px;
  text-align: center;
  display: flex;
  align-items: center;
  gap: 24px;
}

.result-card.success {
  background: #f6ffed;
  border: 2px solid #52c41a;
}

.result-card.error {
  background: #fff2f0;
  border: 2px solid #ff4d4f;
}

.result-icon {
  font-size: 4em;
}

.result-content {
  flex: 1;
  text-align: left;
}

.result-content h3 {
  margin: 0 0 8px 0;
}

.result-content.success h3 {
  color: #52c41a;
}

.result-content.error h3 {
  color: #ff4d4f;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .concepts {
    grid-template-columns: 1fr;
  }

  .temp-matrices {
    grid-template-columns: 1fr;
  }

  .sequence {
    justify-content: center;
  }

  .result-card {
    flex-direction: column;
    text-align: center;
  }

  .result-content {
    text-align: center;
  }

  /* 矩阵显示移动端优化 */
  .matrix-equation {
    flex-direction: column;
    align-items: center;
    gap: 12px;
  }

  .matrix-row {
    flex-direction: column;
    align-items: center;
    gap: 8px;
    text-align: center;
  }

  .matrix-label {
    min-width: auto;
    text-align: center;
    margin-right: 0;
    margin-bottom: 8px;
  }

  .comparison-results {
    flex-direction: column;
    align-items: center;
    gap: 8px;
  }

  .comparison-result {
    min-width: 200px;
    font-size: 14px;
    padding: 10px 16px;
  }

  .calc-comparison, .calc-equation {
    flex-direction: column;
    align-items: center;
    gap: 8px;
  }

  .calc-details {
    flex-direction: column;
    align-items: center;
    gap: 6px;
  }

  .calc-detail-item {
    min-width: 180px;
    font-size: 13px;
    padding: 8px 12px;
  }

  .matrix-term {
    min-width: 120px;
  }

  .calculation-details {
    flex-direction: column;
    align-items: center;
  }
}

@media (min-width: 769px) {
  /* 桌面端矩阵优化 */
  .matrix-display {
    max-width: 900px;
    margin: 0 auto;
  }

  .matrix-equation {
    justify-content: space-around;
    max-width: 800px;
    margin: 0 auto;
  }

  .comparison-results {
    justify-content: space-around;
    max-width: 600px;
    margin: 0 auto;
  }
}

/* 临时状态网格布局 */
.temp-matrices-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin: 16px 0;
}

/* 安全检查简化展示 */
.safety-check {
  margin: 16px 0;
  padding: 16px;
  background: linear-gradient(135deg, #f6ffed, #f0f9ff);
  border-radius: 12px;
  border-left: 4px solid #52c41a;
}

.safety-check-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.safety-check-header h5 {
  margin: 0;
  color: #52c41a;
  font-weight: 600;
}

.safety-summary {
  color: #666;
  font-size: 14px;
}

/* 安全检查弹窗样式 */
.safety-steps-modal {
  max-height: 600px;
  overflow-y: auto;
  padding: 8px;
}

.safety-step {
  margin: 12px 0;
  padding: 16px;
  border-radius: 8px;
  background: #fff;
  border-left: 4px solid #e8e8e8;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
}

.safety-step.success {
  background: linear-gradient(135deg, #f6ffed, #f0f9ff);
  border-left: 4px solid #52c41a;
}

.safety-step.error {
  background: linear-gradient(135deg, #fff2f0, #fff7f6);
  border-left: 4px solid #ff4d4f;
}

.safety-step.info {
  background: linear-gradient(135deg, #e6f7ff, #f0f9ff);
  border-left: 4px solid #1890ff;
}

.safety-step-header {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 8px;
}

.safety-step-index {
  font-weight: bold;
  min-width: 24px;
  color: #333;
}

.safety-step-text {
  flex: 1;
  line-height: 1.5;
}

.safety-step-available {
  font-family: 'Courier New', monospace;
  font-size: 13px;
  color: #666;
  background: rgba(248, 250, 252, 0.8);
  padding: 4px 8px;
  border-radius: 4px;
  margin-top: 8px;
  display: inline-block;
}

/* 弹窗矩阵运算网格布局 */
.matrix-calc-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-top: 12px;
}

/* 弹窗中的矩阵运算样式优化 */
.safety-steps-modal .safety-matrix-calc {
  margin: 12px 0;
  background: linear-gradient(135deg, #f6ffed, #f0f9ff);
  border-radius: 8px;
  padding: 12px;
  border-left: 3px solid #52c41a;
}

.safety-steps-modal .calc-step {
  background: rgba(255, 255, 255, 0.9);
  padding: 10px;
  border-radius: 6px;
  margin: 6px 0;
  border: 1px solid #f0f0f0;
}

.safety-steps-modal .calc-label {
  font-size: 12px;
  margin-bottom: 6px;
  text-align: center;
}

.safety-steps-modal .calc-comparison,
.safety-steps-modal .calc-equation {
  justify-content: center;
  gap: 8px;
  padding: 6px;
}

.safety-steps-modal .calc-vector {
  padding: 3px 6px;
  font-size: 11px;
  min-width: 60px;
}

.safety-steps-modal .calc-op {
  width: 20px;
  height: 20px;
  font-size: 12px;
}

.safety-steps-modal .calc-term {
  min-width: 80px;
}

.safety-steps-modal .calc-details {
  justify-content: center;
  gap: 6px;
}

.safety-steps-modal .calc-detail-item {
  min-width: 80px;
  font-size: 10px;
  padding: 3px 6px;
}

/* 移动端临时状态优化 */
@media (max-width: 768px) {
  .temp-matrices-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .matrix-calc-grid {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .safety-steps-modal .calc-comparison,
  .safety-steps-modal .calc-equation {
    flex-direction: column;
    align-items: center;
    gap: 6px;
  }

  .safety-steps-modal .calc-details {
    flex-direction: column;
    align-items: center;
  }

  .safety-steps-modal .calc-detail-item {
    min-width: 120px;
    font-size: 11px;
  }
}
</style>
