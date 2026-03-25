<template>
  <div class="video-compressor-container">
    <!-- 首屏：强视觉符号 + 核心价值 -->
    <div class="hero-section">
      <div class="hero-content">
        <h1 class="tool-title">
          <span class="gradient-text">智能压缩</span>
          <div class="tool-subtitle">
            <div class="metric-badge success">
              <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <rect x="4" y="4" width="16" height="16" rx="2" ry="2"></rect>
                <line x1="9" y1="4" x2="9" y2="20"></line>
                <line x1="15" y1="4" x2="15" y2="20"></line>
              </svg>
              体积最高减少70%
            </div>
            <div class="metric-badge primary">
              <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect>
                <circle cx="12" cy="12" r="3"></circle>
                <path d="M8.5 8.5L15.5 15.5"></path>
              </svg>
              画质保留度95%+
            </div>
          </div>
        </h1>
        <div class="tool-description">
          <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="gradient-icon">
            <path d="M12 2L2 7l10 5 10-5-10-5z"></path>
            <path d="M2 17l10 5 10-5"></path>
            <path d="M2 12l10 5 10-5"></path>
          </svg>
          智能视频压缩引擎，让您的视频更轻盈，画质依旧出众
        </div>
      </div>
    </div>

    <div class="main-content" :class="{ 'has-file': selectedFile }">
      <!-- 左侧：上传区域 -->
      <div class="upload-section">
        <div class="process-card">
          <div class="process-step active">
            <div class="step-icon">
              <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                <polyline points="17 8 12 3 7 8"></polyline>
                <line x1="12" y1="3" x2="12" y2="15"></line>
              </svg>
            </div>
            <div class="step-content">
              <h3>上传视频</h3>
              <p>支持多种格式</p>
            </div>
          </div>
          <div class="process-step" :class="{ active: selectedFile }">
            <div class="step-icon">
              <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="11" cy="11" r="8"></circle>
                <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
              </svg>
            </div>
            <div class="step-content">
              <h3>智能分析</h3>
              <p>视频参数优化</p>
            </div>
          </div>
          <div class="process-step" :class="{ active: isCompressing || selectedFile }">
            <div class="step-icon">
              <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <rect x="4" y="4" width="16" height="16" rx="2" ry="2"></rect>
                <line x1="9" y1="4" x2="9" y2="20"></line>
                <line x1="15" y1="4" x2="15" y2="20"></line>
              </svg>
            </div>
            <div class="step-content">
              <h3>高效压缩</h3>
              <p>智能编码处理</p>
            </div>
          </div>
          <div class="process-step" :class="{ active: compressedFile }">
            <div class="step-icon">
              <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                <polyline points="7 10 12 15 17 10"></polyline>
                <line x1="12" y1="15" x2="12" y2="3"></line>
              </svg>
            </div>
            <div class="step-content">
              <h3>完成导出</h3>
              <p>即刻下载使用</p>
            </div>
          </div>
        </div>

        <div
          class="upload-zone"
          :class="{ 'has-file': selectedFile }"
          @drop.prevent="handleDrop"
          @dragover.prevent
          @click="triggerFileInput"
        >
          <input
            type="file"
            ref="fileInput"
            accept="video/*"
            @change="handleFileSelect"
            style="display: none"
          >
          <div v-if="!selectedFile" class="upload-placeholder">
            <div class="upload-icon">
              <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                <polyline points="17 8 12 3 7 8"></polyline>
                <line x1="12" y1="3" x2="12" y2="15"></line>
              </svg>
            </div>
            <div class="upload-text">
              点击或拖拽视频文件到此处
            </div>
            <div class="supported-formats">
              <span class="format-tag">MP4</span>
              <span class="format-tag">WebM</span>
              <span class="format-tag">AVI</span>
              <span class="format-tag">MOV</span>
            </div>
          </div>
          <div v-else class="video-preview">
            <video ref="videoPreview" controls></video>
            <div class="video-info-card">
              <div class="info-header">
                <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm-1-13h2v6h-2zm0 8h2v2h-2z"></path>
                </svg>
                <span>视频信息</span>
              </div>
              <div class="info-grid">
                <div class="info-item">
                  <div class="info-label">文件名</div>
                  <div class="info-value">{{ selectedFile.name }}</div>
                </div>
                <div class="info-item">
                  <div class="info-label">大小</div>
                  <div class="info-value">{{ formatFileSize(selectedFile.size) }}</div>
                </div>
                <div class="info-item">
                  <div class="info-label">时长</div>
                  <div class="info-value">{{ duration }}</div>
                </div>
                <div class="info-item">
                  <div class="info-label">分辨率</div>
                  <div class="info-value">{{ resolution }}</div>
                </div>
                <div class="info-item">
                  <div class="info-label">帧率</div>
                  <div class="info-value">{{ frameRate }}</div>
                </div>
                <div class="info-item">
                  <div class="info-label">比特率</div>
                  <div class="info-value">{{ bitrate }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧：控制面板 -->
      <div v-if="selectedFile" class="control-section">
        <div class="compression-options">
          <div class="options-header">
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="4" y1="21" x2="4" y2="14"></line>
              <line x1="4" y1="10" x2="4" y2="3"></line>
              <line x1="12" y1="21" x2="12" y2="12"></line>
              <line x1="12" y1="8" x2="12" y2="3"></line>
              <line x1="20" y1="21" x2="20" y2="16"></line>
              <line x1="20" y1="12" x2="20" y2="3"></line>
              <line x1="1" y1="14" x2="7" y2="14"></line>
              <line x1="9" y1="8" x2="15" y2="8"></line>
              <line x1="17" y1="16" x2="23" y2="16"></line>
            </svg>
            <span>压缩设置</span>
          </div>

          <div class="option-group">
            <label>
              <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"></polygon>
              </svg>
              压缩质量
            </label>
            <div class="quality-selector">
              <button
                v-for="quality in ['high', 'medium', 'low']"
                :key="quality"
                :class="['quality-btn', { active: compressionQuality === quality }]"
                @click="compressionQuality = quality"
              >
                {{ quality === 'high' ? '高质量' : quality === 'medium' ? '平衡' : '高压缩' }}
                <span class="quality-detail">
                  {{ quality === 'high' ? '2000kbps' : quality === 'medium' ? '1000kbps' : '500kbps' }}
                </span>
              </button>
            </div>
          </div>

          <div class="option-group">
            <label>
              <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                <polyline points="14 2 14 8 20 8"></polyline>
                <line x1="12" y1="18" x2="12" y2="12"></line>
                <line x1="9" y1="15" x2="15" y2="15"></line>
              </svg>
              输出格式
            </label>
            <div class="format-selector">
              <button
                v-for="format in ['mp4', 'webm']"
                :key="format"
                :class="['format-btn', { active: outputFormat === format }]"
                @click="outputFormat = format"
              >
                {{ format.toUpperCase() }}
              </button>
            </div>
          </div>

          <div class="compression-actions">
            <button
              class="compress-btn"
              :disabled="isCompressing"
              @click="startCompression"
            >
              <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <rect x="4" y="4" width="16" height="16" rx="2" ry="2"></rect>
                <line x1="9" y1="4" x2="9" y2="20"></line>
                <line x1="15" y1="4" x2="15" y2="20"></line>
              </svg>
              {{ isCompressing ? '压缩中...' : '开始压缩' }}
            </button>
            <button class="reset-btn" @click="resetFile">
              <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M3 2v6h6"></path>
                <path d="M21 12A9 9 0 0 0 6 5.3L3 8"></path>
                <path d="M21 22v-6h-6"></path>
                <path d="M3 12a9 9 0 0 0 15 6.7l3-2.7"></path>
              </svg>
              重新选择
            </button>
          </div>
        </div>

        <div v-if="isCompressing" class="progress-section">
          <div class="progress-card">
            <div class="progress-header">
              <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="fa-spin">
                <circle cx="12" cy="12" r="10"></circle>
                <path d="M12 6v6l4 2"></path>
              </svg>
              <span>压缩进度</span>
            </div>
            <div class="progress-ring-container">
              <svg class="progress-ring" width="120" height="120">
                <defs>
                  <linearGradient id="gradient" x1="0%" y1="0%" x2="100%" y2="0%">
                    <stop offset="0%" stop-color="#165DFF" />
                    <stop offset="100%" stop-color="#722ED1" />
                  </linearGradient>
                </defs>
                <circle
                  class="progress-ring-circle-bg"
                  stroke-width="8"
                  fill="transparent"
                  r="52"
                  cx="60"
                  cy="60"
                />
                <circle
                  class="progress-ring-circle"
                  stroke-width="8"
                  fill="transparent"
                  r="52"
                  cx="60"
                  cy="60"
                  :style="{
                    strokeDasharray: `${2 * Math.PI * 52}`,
                    strokeDashoffset: `${2 * Math.PI * 52 * (1 - compressionProgress / 100)}`
                  }"
                />
              </svg>
              <div class="progress-text">
                <span class="progress-number">{{ Math.round(compressionProgress) }}</span>
                <span class="progress-unit">%</span>
              </div>
            </div>
            <div class="progress-status">{{ progressText }}</div>

            <!-- 添加时间线容器 -->
            <div class="compression-timeline">
              <div class="timeline-stages">
                <div v-for="(stage, index) in compressionStages"
                     :key="index"
                     class="stage-container"
                >
                  <div class="stage-item"
                       :class="{ 'active': compressionProgress >= stage.progress }"
                       @click="showStageDetails(stage)"
                  >
                    <div class="stage-circle">
                      <span class="stage-number">{{ stage.progress }}%</span>
                    </div>
                    <div class="stage-text">{{ stage.text }}</div>
                  </div>

                  <svg v-if="index < compressionStages.length - 1"
                       class="stage-arrow"
                       :class="{ 'active': compressionProgress >= compressionStages[index + 1].progress }"
                       width="120"
                       height="20"
                       viewBox="0 0 120 20"
                  >
                    <defs>
                      <linearGradient :id="'arrowGradient' + index" x1="0%" y1="0%" x2="100%" y2="0%">
                        <stop offset="0%" style="stop-color:#165DFF;stop-opacity:1" />
                        <stop offset="100%" style="stop-color:#722ED1;stop-opacity:1" />
                      </linearGradient>
                    </defs>
                    <path d="M5,10 H95 L85,2 L95,10 L85,18 L95,10 H115"
                          fill="none"
                          :stroke="compressionProgress >= compressionStages[index + 1].progress ? `url(#arrowGradient${index})` : 'rgba(22, 93, 255, 0.1)'"
                          stroke-width="2"
                          stroke-linecap="round"
                          stroke-linejoin="round"
                    />
                  </svg>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div v-if="compressedFile" class="compression-result">
          <h3 class="result-title">
            <i class="el-icon-check"></i>
            压缩完成
          </h3>
          <div class="result-table">
            <div class="table-row header-row">
              <div class="table-header">对比项</div>
              <div class="table-header">原始文件</div>
              <div class="table-header">压缩后</div>
              <div class="table-header">优化效果</div>
            </div>
            <div class="table-row">
              <div class="table-label">
                <i class="el-icon-document"></i>
                文件大小
              </div>
              <div class="table-cell">{{ formatFileSize(selectedFile.size) }}</div>
              <div class="table-cell">{{ formatFileSize(compressedFile.size) }}</div>
              <div class="table-cell reduction">
                <span class="reduction-value">-{{ calculateReduction(selectedFile.size, compressedFile.size) }}%</span>
              </div>
            </div>
            <div class="table-row">
              <div class="table-label">
                <i class="el-icon-picture"></i>
                分辨率
              </div>
              <div class="table-cell">{{ resolution }}</div>
              <div class="table-cell">{{ compressedResolution }}</div>
              <div class="table-cell">
                <span class="status-tag" :class="resolution === compressedResolution ? 'unchanged' : 'optimized'">
                  {{ resolution === compressedResolution ? '保持不变' : '已优化' }}
                </span>
              </div>
            </div>
            <div class="table-row">
              <div class="table-label">
                <i class="el-icon-video-play"></i>
                帧率
              </div>
              <div class="table-cell">{{ frameRate }}</div>
              <div class="table-cell">{{ compressedFrameRate }}</div>
              <div class="table-cell">
                <span class="status-tag" :class="frameRate === compressedFrameRate ? 'unchanged' : 'optimized'">
                  {{ frameRate === compressedFrameRate ? '保持不变' : '已优化' }}
                </span>
              </div>
            </div>
            <div class="table-row">
              <div class="table-label">
                <i class="el-icon-data-line"></i>
                比特率
              </div>
              <div class="table-cell">{{ bitrate }}</div>
              <div class="table-cell">{{ compressedBitrate }}</div>
              <div class="table-cell reduction">
                <span class="reduction-value" v-if="calculateBitrateReduction">-{{ calculateBitrateReduction }}%</span>
                <span v-else>-</span>
              </div>
            </div>
            <div class="table-row">
              <div class="table-label">
                <i class="el-icon-timer"></i>
                处理用时
              </div>
              <div class="table-cell time-cell" colspan="3">{{ processingTime }} 秒</div>
            </div>
          </div>
          <div class="result-actions">
            <div type="success" @click="downloadCompressedFile" class="action-btn download-btn">
              <i class="el-icon-download"></i>
              下载压缩文件
            </div>
            <div
              type="primary"
              @click="showPreviewDialog"
              class="action-btn preview-btn"
            >
              <i class="el-icon-video-play"></i>
              查看压缩效果
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 添加阶段详情弹框 -->
    <div v-if="showStageDialog && selectedStage" class="stage-dialog-overlay" @click="showStageDialog = false">
      <div class="stage-dialog" @click.stop>
        <div class="dialog-header">
          <h3>{{ selectedStage.title }}</h3>
          <button class="close-btn" @click="showStageDialog = false">
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="18" y1="6" x2="6" y2="18"></line>
              <line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
          </button>
        </div>
        <div class="dialog-content">
          <p class="stage-description">{{ selectedStage.description }}</p>
          <div class="stage-details">
            <div v-for="(detail, index) in selectedStage.details"
                 :key="index"
                 class="detail-item"
            >
              {{ detail }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 替换预览弹窗部分 -->
    <div class="modal-overlay" v-if="previewDialogVisible" @click="closePreview">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>压缩效果预览</h3>
          <button class="close-btn" @click="closePreview">×</button>
        </div>

        <div class="modal-body">
          <div class="preview-container" v-if="compressedFile">
            <div class="preview-main">
              <div class="video-container">
                <video
                  ref="previewVideo"
                  controls
                  class="preview-video"
                  :src="compressedVideoUrl"
                  @loadedmetadata="onPreviewVideoLoad"
                ></video>
              </div>

              <!-- 图表区域 -->
              <div class="chart-container">
                <div class="chart-item">
                  <h4>压缩效果分析</h4>
                  <div ref="compressionPieChart" class="pie-chart"></div>
                </div>
                <div class="chart-item">
                  <h4>参数对比分析</h4>
                  <div ref="paramsBarChart" class="bar-chart"></div>
                </div>
              </div>
            </div>

            <div class="preview-sidebar">
              <div class="info-section">
                <h4>视频信息</h4>
                <div class="info-grid">
                  <div class="highlight-card">
                    <div class="card-header">文件大小</div>
                    <div class="card-content">
                      <div class="original">
                        <span class="label">原始</span>
                        <span class="value">{{ formatFileSize(selectedFile?.size || 0) }}</span>
                      </div>
                      <div class="compressed">
                        <span class="label">压缩后</span>
                        <span class="value success">{{ formatFileSize(compressedFile?.size || 0) }}</span>
                      </div>
                      <div class="reduction">
                        <span class="value success">-{{ calculateReduction(selectedFile?.size || 0, compressedFile?.size || 0) }}%</span>
                      </div>
                    </div>
                  </div>

                  <div class="info-group">
                    <h5>分辨率</h5>
                    <div class="compare-item">
                      <span class="label">原始</span>
                      <span class="value">{{ resolution || '-' }}</span>
                    </div>
                    <div class="compare-item">
                      <span class="label">压缩后</span>
                      <span class="value">{{ compressedResolution || '-' }}</span>
                    </div>
                  </div>

                  <div class="info-group">
                    <h5>帧率</h5>
                    <div class="compare-item">
                      <span class="label">原始</span>
                      <span class="value">{{ frameRate || '-' }}</span>
                    </div>
                    <div class="compare-item">
                      <span class="label">压缩后</span>
                      <span class="value">{{ compressedFrameRate || '-' }}</span>
                    </div>
                  </div>

                  <div class="info-group">
                    <h5>比特率</h5>
                    <div class="compare-item">
                      <span class="label">原始</span>
                      <span class="value">{{ bitrate || '-' }}</span>
                    </div>
                    <div class="compare-item">
                      <span class="label">压缩后</span>
                      <span class="value">{{ compressedBitrate || '-' }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="modal-footer">
          <button class="modal-btn cancel" @click="closePreview">关闭预览</button>
          <button class="modal-btn download" @click="downloadCompressedFile">
            <i class="el-icon-download"></i>
            下载压缩文件
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, nextTick } from 'vue'
import { createFFmpeg, fetchFile } from '@ffmpeg/ffmpeg'
import * as echarts from 'echarts'

// 状态变量
const fileInput = ref<HTMLInputElement | null>(null)
const videoPreview = ref<HTMLVideoElement | null>(null)
const selectedFile = ref<File | null>(null)
const compressedFile = ref<File | null>(null)
const isCompressing = ref(false)
const compressionProgress = ref(0)
const progressText = ref('')
const duration = ref('')
const resolution = ref('')
const compressedResolution = ref('')
const compressionQuality = ref('medium')
const outputFormat = ref('mp4')
const frameRate = ref('')
const bitrate = ref('')
const compressedFrameRate = ref('')
const compressedBitrate = ref('')
const processingTime = ref(0)
const startTime = ref(0)

const compressionPieChart = ref<HTMLElement | null>(null)
const paramsBarChart = ref<HTMLElement | null>(null)
let pieChart: echarts.ECharts | null = null
let barChart: echarts.ECharts | null = null

// FFmpeg 实例
const ffmpeg = createFFmpeg({
  corePath: '/ffmpeg/ffmpeg-core.js',
  log: true,
  progress: ({ ratio }) => {
    if (ratio >= 0 && isCompressing.value) {
      // 将进度限制在0-95%之间，最后5%留给文件处理
      const progress = Math.min(Math.round(ratio * 95), 95)
      compressionProgress.value = progress
      updateProgressStage(progress)
    }
  }
})

// 确保在使用前加载 FFmpeg
onMounted(async () => {
  try {
    await ffmpeg.load()
    console.log('FFmpeg 加载成功')
  } catch (error) {
    console.error('FFmpeg 加载失败:', error)
  }
})

// 解析FFmpeg时间格式为秒
const parseTimeToSeconds = (timeStr: string): number => {
  const parts = timeStr.split(':')
  if (parts.length === 3) {
    const [hours, minutes, seconds] = parts
    return (
      parseFloat(hours) * 3600 +
      parseFloat(minutes) * 60 +
      parseFloat(seconds)
    )
  }
  return 0
}

// 文件选择处理
const triggerFileInput = () => {
  fileInput.value?.click()
}

const handleFileSelect = (event: Event) => {
  const input = event.target as HTMLInputElement
  if (input.files && input.files[0]) {
    processSelectedFile(input.files[0])
  }
}

const handleDrop = (event: DragEvent) => {
  const file = event.dataTransfer?.files[0]
  if (file && file.type.startsWith('video/')) {
    processSelectedFile(file)
  }
}

// 定义阶段类型
interface CompressionStage {
  progress: number
  text: string
  title: string
  description: string
  details: string[]
}

// 处理选中的文件
const processSelectedFile = (file: File) => {
  selectedFile.value = file
  compressedFile.value = null
  compressionProgress.value = 0
  processingTime.value = 0

  const videoURL = URL.createObjectURL(file)
  const video = document.createElement('video')
  video.preload = 'metadata'

  video.onloadedmetadata = () => {
    duration.value = formatDuration(video.duration)
    resolution.value = `${video.videoWidth}x${video.videoHeight}`

    // 获取视频的帧率和比特率
    try {
      // 使用类型断言来处理 captureStream API
      interface VideoWithCapture extends HTMLVideoElement {
        captureStream?: () => MediaStream;
        mozCaptureStream?: () => MediaStream;
      }

      const videoWithCapture = video as VideoWithCapture
      const stream = videoWithCapture.captureStream?.() || videoWithCapture.mozCaptureStream?.()
      if (stream) {
        const videoTrack = stream.getVideoTracks()[0]
        const settings = videoTrack.getSettings()
        frameRate.value = settings.frameRate ? `${settings.frameRate} fps` : '未知'

        // 计算比特率
        const fileSize = file.size
        const videoDuration = video.duration
        const bitrateMbps = (fileSize * 8) / (videoDuration * 1000000)
        bitrate.value = `${bitrateMbps.toFixed(2)} Mbps`
      } else {
        frameRate.value = '未知'
        bitrate.value = '未知'
      }
    } catch (error) {
      console.error('获取视频信息失败:', error)
      frameRate.value = '未知'
      bitrate.value = '未知'
    }

    // 更新预览视频
    if (videoPreview.value) {
      videoPreview.value.src = videoURL
    }
  }

  video.onerror = () => {
    console.error('视频加载失败')
    duration.value = '未知'
    resolution.value = '未知'
    frameRate.value = '未知'
    bitrate.value = '未知'
  }

  video.src = videoURL
}

// 添加内存清理函数
const cleanupFFmpeg = async () => {
  try {
    if (ffmpeg.isLoaded()) {
      // 只清理已知的文件
      try {
        await ffmpeg.FS('unlink', 'input.mp4')
      } catch {
        // 忽略文件不存在的错误
      }
      try {
        await ffmpeg.FS('unlink', 'input.webm')
      } catch {
        // 忽略文件不存在的错误
      }
      try {
        await ffmpeg.FS('unlink', 'output.mp4')
      } catch {
        // 忽略文件不存在的错误
      }
      try {
        await ffmpeg.FS('unlink', 'output.webm')
      } catch {
        // 忽略文件不存在的错误
      }
    }
  } catch (error) {
    console.error('清理FFmpeg失败:', error)
  }
}

// 压缩阶段管理
const compressionStages = [
  {
    progress: 0,
    text: '准备开始',
    title: '初始化压缩',
    description: '系统正在初始化压缩引擎，准备开始处理您的视频文件。',
    details: [
      '• 验证文件格式和完整性',
      '• 初始化压缩引擎',
      '• 准备临时存储空间'
    ]
  },
  {
    progress: 20,
    text: '加载编码器',
    title: '编码器配置',
    description: '正在加载并配置视频编码器，为视频压缩做准备。',
    details: [
      '• 加载FFmpeg编码器',
      '• 配置编码参数',
      '• 优化编码设置'
    ]
  },
  {
    progress: 40,
    text: '分析视频',
    title: '视频分析',
    description: '正在分析视频特征，确定最佳压缩策略。',
    details: [
      '• 分析视频码率',
      '• 检测视频分辨率',
      '• 评估视频质量'
    ]
  },
  {
    progress: 60,
    text: '视频编码中',
    title: '视频编码',
    description: '正在使用优化后的参数对视频进行编码压缩。',
    details: [
      '• 执行视频编码',
      '• 优化比特率',
      '• 保持画面质量'
    ]
  },
  {
    progress: 80,
    text: '优化处理中',
    title: '最终优化',
    description: '正在对压缩后的视频进行最终优化处理。',
    details: [
      '• 优化文件封装',
      '• 调整元数据',
      '• 确保兼容性'
    ]
  },
  {
    progress: 100,
    text: '压缩完成',
    title: '处理完成',
    description: '视频压缩已完成，可以预览和下载。',
    details: [
      '• 生成最终文件',
      '• 验证输出质量',
      '• 准备下载'
    ]
  }
]

// 弹框状态管理
const showStageDialog = ref(false)
const selectedStage = ref<CompressionStage | null>(null)

// 显示阶段详情
const showStageDetails = (stage: CompressionStage) => {
  selectedStage.value = stage
  showStageDialog.value = true
}

// 压缩阶段管理
const updateProgressStage = (progress: number) => {
  if (!isCompressing.value) return

  for (let i = compressionStages.length - 1; i >= 0; i--) {
    if (progress >= compressionStages[i].progress) {
      progressText.value = compressionStages[i].text
      break
    }
  }
}

// 开始压缩
const startCompression = async () => {
  if (!selectedFile.value || isCompressing.value) return

  try {
    isCompressing.value = true
    compressionProgress.value = 0
    progressText.value = '准备开始'
    updateProgressStage(0)
    startTime.value = Date.now()

    // 加载 FFmpeg
    if (!ffmpeg.isLoaded()) {
      progressText.value = '正在加载编码器...'
      await ffmpeg.load()
    }

    // 重置进度
    compressionProgress.value = 0
    progressText.value = '开始处理...'

    const inputFileName = 'input.' + selectedFile.value.name.split('.').pop()
    const outputFileName = `output.${outputFormat.value}`

    // 写入输入文件
    updateProgressStage(20)
    await ffmpeg.FS('writeFile', inputFileName, await fetchFile(selectedFile.value))

    // 设置压缩参数
    const bitrateMap = {
      high: '2000k',
      medium: '1000k',
      low: '500k'
    }

    // 构建 FFmpeg 命令
    const args = [
      '-i', inputFileName,
      '-c:v', outputFormat.value === 'webm' ? 'libvpx-vp9' : 'libx264',
      '-b:v', bitrateMap[compressionQuality.value as keyof typeof bitrateMap],
      '-preset', 'medium',
      '-movflags', 'faststart',
      '-c:a', outputFormat.value === 'webm' ? 'libvorbis' : 'aac',
      '-strict', '-2',
      outputFileName
    ]

    // 执行压缩
    await ffmpeg.run(...args)

    // 读取输出文件
    const data = ffmpeg.FS('readFile', outputFileName)
    const uint8Array = new Uint8Array(data.buffer)
    const blob = new Blob([uint8Array], {
      type: `video/${outputFormat.value}`
    })
    compressedFile.value = new File([blob],
      `compressed_${selectedFile.value.name.replace(/\.[^/.]+$/, '')}.${outputFormat.value}`,
      { type: `video/${outputFormat.value}` }
    )

    // 清理临时文件
    try {
      ffmpeg.FS('unlink', inputFileName)
    } catch {
      // 忽略文件不存在的错误
    }
    try {
      ffmpeg.FS('unlink', outputFileName)
    } catch {
      // 忽略文件不存在的错误
    }

    // 获取压缩后的视频信息
    const compressedVideoURL = URL.createObjectURL(compressedFile.value)
    const tempVideo = document.createElement('video')
    tempVideo.preload = 'metadata'

    await new Promise((resolve) => {
      tempVideo.onloadedmetadata = () => {
        compressedResolution.value = `${tempVideo.videoWidth}x${tempVideo.videoHeight}`

        try {
          // @ts-expect-error captureStream is not in TypeScript's DOM types
          const stream = tempVideo.captureStream()
          const videoTrack = stream.getVideoTracks()[0]
          const settings = videoTrack.getSettings()
          compressedFrameRate.value = settings.frameRate ? `${settings.frameRate} fps` : '未知'

          // 计算压缩后的比特率
          const fileSize = compressedFile.value!.size
          const duration = tempVideo.duration
          const bitrateMbps = (fileSize * 8) / (duration * 1000000)
          compressedBitrate.value = `${bitrateMbps.toFixed(2)} Mbps`
        } catch (error) {
          console.error('获取压缩后视频信息失败:', error)
          compressedFrameRate.value = '未知'
          compressedBitrate.value = '未知'
        }

        URL.revokeObjectURL(compressedVideoURL)
        resolve(null)
      }

      tempVideo.onerror = () => {
        console.error('压缩后视频加载失败')
        compressedResolution.value = '未知'
        compressedFrameRate.value = '未知'
        compressedBitrate.value = '未知'
        URL.revokeObjectURL(compressedVideoURL)
        resolve(null)
      }

      tempVideo.src = compressedVideoURL
    })

    // 最后才更新到100%完成状态
    compressionProgress.value = 100
    updateProgressStage(100)

    // 计算处理用时
    processingTime.value = Math.round((Date.now() - startTime.value) / 1000)

  } catch (error) {
    console.error('压缩失败:', error)
    progressText.value = `压缩失败: ${error}`
    compressionProgress.value = 0
    processingTime.value = 0
  } finally {
    isCompressing.value = false
  }
}

// 下载压缩后的文件
const downloadCompressedFile = () => {
  if (!compressedFile.value) return

  const url = URL.createObjectURL(compressedFile.value)
  const a = document.createElement('a')
  a.href = url
  a.download = compressedFile.value.name
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

// 重置文件选择
const resetFile = () => {
  selectedFile.value = null
  compressedFile.value = null
  if (videoPreview.value) {
    videoPreview.value.src = ''
  }
  duration.value = ''
  resolution.value = ''
  compressionProgress.value = 0
  progressText.value = ''
  processingTime.value = 0 // 重置处理用时
}

// 工具函数
const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const formatDuration = (seconds: number): string => {
  if (!seconds || isNaN(seconds)) return '未知'

  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const remainingSeconds = Math.floor(seconds % 60)

  if (hours > 0) {
    return `${hours}:${minutes.toString().padStart(2, '0')}:${remainingSeconds.toString().padStart(2, '0')}`
  }
  return `${minutes}:${remainingSeconds.toString().padStart(2, '0')}`
}

// 组件卸载时清理
onUnmounted(async () => {
  if (ffmpeg.isLoaded()) {
    try {
      await cleanupFFmpeg()
    } catch (error) {
      console.error('FFmpeg 清理失败:', error)
    }
  }
})

// 计算压缩率
const calculateReduction = (originalSize: number, compressedSize: number): string => {
  return ((originalSize - compressedSize) / originalSize * 100).toFixed(1)
}

// 计算比特率减少百分比
const calculateBitrateReduction = computed<string | null>(() => {
  if (!bitrate.value || !compressedBitrate.value) return null
  const originalBitrate = parseFloat(bitrate.value)
  const compressedBitrateValue = parseFloat(compressedBitrate.value)
  if (isNaN(originalBitrate) || isNaN(compressedBitrateValue)) return null
  return ((originalBitrate - compressedBitrateValue) / originalBitrate * 100).toFixed(1)
})

// 预览弹窗相关
const previewDialogVisible = ref(false)
const previewVideo = ref<HTMLVideoElement | null>(null)
const compressedVideoUrl = computed(() => {
  if (!compressedFile.value) return ''
  return URL.createObjectURL(compressedFile.value)
})

// 初始化饼图
const initPieChart = () => {
  if (!compressionPieChart.value) return

  pieChart = echarts.init(compressionPieChart.value)
  const reduction = calculateReduction(selectedFile.value?.size || 0, compressedFile.value?.size || 0)

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c}%'
    },
    legend: {
      orient: 'vertical',
      right: 10,
      top: 'center',
      textStyle: {
        color: '#e2e8f0'
      }
    },
    series: [
      {
        name: '压缩分析',
        type: 'pie',
        radius: ['60%', '80%'],
        avoidLabelOverlap: false,
        label: {
          show: false
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 20,
            fontWeight: 'bold'
          }
        },
        data: [
          {
            value: reduction,
            name: '节省空间',
            itemStyle: { color: '#67c23a' }
          },
          {
            value: 100 - reduction,
            name: '压缩后大小',
            itemStyle: { color: '#409EFF' }
          }
        ]
      }
    ]
  }

  pieChart.setOption(option)
}

// 初始化柱状图
const initBarChart = () => {
  if (!paramsBarChart.value) return

  barChart = echarts.init(paramsBarChart.value)

  const originalBitrate = parseFloat(bitrate.value?.replace('Mbps', '') || '0')
  const compressedBitrateValue = parseFloat(compressedBitrate.value?.replace('Mbps', '') || '0')
  const originalFrameRate = parseFloat(frameRate.value?.replace('fps', '') || '0')
  const compressedFrameRateValue = parseFloat(compressedFrameRate.value?.replace('fps', '') || '0')

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    legend: {
      data: ['原始', '压缩后'],
      textStyle: {
        color: '#e2e8f0'
      },
      top: 0
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'value',
      axisLabel: {
        color: '#94a3b8'
      }
    },
    yAxis: {
      type: 'category',
      data: ['比特率 (Mbps)', '帧率 (fps)'],
      axisLabel: {
        color: '#94a3b8'
      }
    },
    series: [
      {
        name: '原始',
        type: 'bar',
        data: [originalBitrate, originalFrameRate],
        itemStyle: {
          color: '#409EFF'
        }
      },
      {
        name: '压缩后',
        type: 'bar',
        data: [compressedBitrateValue, compressedFrameRateValue],
        itemStyle: {
          color: '#67c23a'
        }
      }
    ]
  }

  barChart.setOption(option)
}

// 显示预览弹窗时初始化图表
const showPreviewDialog = async () => {
  if (!compressedFile.value) return
  previewDialogVisible.value = true

  await nextTick()
  initPieChart()
  initBarChart()

  // 设置视频音量
  if (previewVideo.value) {
    previewVideo.value.volume = 0.5
  }
}

// 关闭预览时销毁图表
const closePreview = () => {
  previewDialogVisible.value = false
  if (previewVideo.value) {
    previewVideo.value.pause()
    previewVideo.value.src = ''
  }

  if (pieChart) {
    pieChart.dispose()
    pieChart = null
  }
  if (barChart) {
    barChart.dispose()
    barChart = null
  }
}

// 监听窗口大小变化，重绘图表
const handleResize = () => {
  pieChart?.resize()
  barChart?.resize()
}

onMounted(() => {
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  closePreview()
  window.removeEventListener('resize', handleResize)
  if (compressedVideoUrl.value) {
    URL.revokeObjectURL(compressedVideoUrl.value)
  }
})
</script>

<style scoped>
.video-compressor-container {
  position: relative;
  margin: -24px!important;
  background: linear-gradient(135deg, #3a3a3a 0%, #242b38 100%);
  color: #e6e8f0;
  min-height: 100vh;
}

/* Hero Section */
.hero-section {
  text-align: center;
  padding: 48px 0;
  position: relative;
  overflow: hidden;
}

.hero-section::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background:
    radial-gradient(circle at 20% 20%, rgba(64, 158, 255, 0.08) 0%, transparent 50%),
    radial-gradient(circle at 80% 80%, rgba(114, 46, 209, 0.08) 0%, transparent 50%);
  z-index: 0;
}

.hero-content {
  position: relative;
  z-index: 1;
}

.tool-title {
  font-size: 48px;
  font-weight: 800;
  background: linear-gradient(135deg, #409eff 0%, #a855f7 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  margin-bottom: 24px;
}

.tool-subtitle {
  display: flex;
  gap: 16px;
  justify-content: center;
  margin-top: 16px;
}

.metric-badge {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
}

.metric-badge.success {
  background: rgba(103, 194, 58, 0.1);
  color: #67c23a;
}

.metric-badge.primary {
  background: rgba(64, 158, 255, 0.1);
  color: #409EFF;
}

.metric-badge i {
  font-size: 16px;
}

.tool-description {
  font-size: 18px;
  color: #94a3b8;
}

.gradient-icon {
  font-size: 24px;
  background: linear-gradient(135deg, #165DFF 0%, #722ED1 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

/* Process Steps */
.process-card {
  display: flex;
  justify-content: space-between;
  margin-bottom: 32px;
  padding: 24px;
  background: rgba(30, 41, 59, 0.7);
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
}

.process-step {
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  flex: 1;
}

.process-step:not(:last-child)::after {
  content: '';
  position: absolute;
  top: 24px;
  right: -50%;
  width: 100%;
  height: 2px;
  background: rgba(255, 255, 255, 0.1);
  z-index: 0;
}

.process-step.active:not(:last-child)::after {
  background: linear-gradient(90deg, #165DFF, #722ED1);
}

.step-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: rgba(30, 41, 59, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
  position: relative;
  z-index: 1;
  transition: all 0.3s ease;
}

.process-step.active .step-icon {
  background: linear-gradient(135deg, #165DFF 0%, #722ED1 100%);
  box-shadow: 0 8px 16px rgba(22, 93, 255, 0.2);
}

.process-step.active .step-icon i {
  color: white;
}

.step-icon i {
  font-size: 20px;
  color: #94a3b8;
  transition: all 0.3s ease;
}

.step-content {
  text-align: center;
}

.step-content h3 {
  font-size: 16px;
  font-weight: 600;
  color: #e6e8f0;
  margin-bottom: 4px;
}

.step-content p {
  font-size: 14px;
  color: #94a3b8;
}

/* Upload Zone */
.upload-zone {
  border: 2px dashed rgba(255, 255, 255, 0.2);
  border-radius: 16px;
  padding: 32px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
}

.upload-zone:hover {
  border-color: #409eff;
  transform: translateY(-2px);
  box-shadow: 0 12px 32px rgba(64, 158, 255, 0.1);
}

.upload-zone.has-file {
  border-style: solid;
  border-color: #409eff;
  margin-bottom: 72px;
}

.upload-placeholder {
  padding: 40px 20px;
}

.upload-icon {
  width: 80px;
  height: 80px;
  margin: 0 auto 24px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.1) 0%, rgba(168, 85, 247, 0.1) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.upload-icon i {
  font-size: 32px;
  background: linear-gradient(135deg, #409eff 0%, #a855f7 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.upload-text {
  font-size: 20px;
  color: #e6e8f0;
  margin-bottom: 12px;
  font-weight: 500;
}

.supported-formats {
  display: flex;
  gap: 8px;
  justify-content: center;
  margin-top: 16px;
}

.format-tag {
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 14px;
  color: #94a3b8;
  background: rgba(30, 41, 59, 0.5);
  transition: all 0.3s ease;
}

.format-tag:hover {
  background: rgba(64, 158, 255, 0.1);
  color: #409eff;
}

/* Video Preview */
.video-preview {
  margin-top: 24px;
}

.video-preview video {
  width: 100%;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  background: #000;
  margin-bottom: 24px;
}

.video-info-card {
  background: rgba(30, 41, 59, 0.7);
  border-radius: 16px;
  padding: 24px;
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
}

.info-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.info-header i {
  font-size: 24px;
  color: #409eff;
}

.info-header span {
  font-size: 18px;
  font-weight: 600;
  color: #e6e8f0;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 24px;
}

.info-item {
  background: rgba(30, 41, 59, 0.5);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  padding: 16px;
  transition: all 0.3s ease;
}

.info-item:hover {
  transform: translateY(-2px);
  background: rgba(30, 41, 59, 0.7);
}

.info-label {
  color: #94a3b8;
  font-size: 14px;
}

.info-value {
  color: #e6e8f0;
  font-size: 16px;
  font-weight: 500;
}

.stat-value.highlight {
  color: #409eff;
  font-weight: 600;
}

/* Compression Options */
.compression-options {
  background: rgba(30, 41, 59, 0.7);
  border-radius: 16px;
  padding: 24px;
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
}

.options-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.options-header i {
  font-size: 24px;
  color: #409eff;
}

.options-header span {
  font-size: 18px;
  font-weight: 600;
  color: #e6e8f0;
}

.option-group {
  margin-bottom: 24px;
}

.option-group:last-child {
  margin-bottom: 0;
}

.option-group label {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  font-size: 16px;
  color: #e6e8f0;
}

.option-group label i {
  color: #409eff;
}

.quality-selector,
.format-selector {
  display: flex;
  gap: 12px;
}

.quality-btn,
.format-btn {
  flex: 1;
  padding: 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  background: rgba(30, 41, 59, 0.5);
  color: #94a3b8;
}

.quality-btn:hover,
.format-btn:hover {
  background: rgba(64, 158, 255, 0.1);
  color: #409eff;
}

.quality-btn.active,
.format-btn.active {
  background: linear-gradient(135deg, #409eff 0%, #a855f7 100%);
  color: white;
  border: none;
  box-shadow: 0 8px 16px rgba(64, 158, 255, 0.2);
}

.quality-detail {
  display: block;
  font-size: 12px;
  margin-top: 4px;
  opacity: 0.7;
}

/* Progress Section */
.progress-card {
  background: rgba(30, 41, 59, 0.7);
  border-radius: 16px;
  padding: 32px;
  text-align: center;
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
}

/* 压缩时间线样式 */
.compression-timeline {
  overflow-x: auto;
  margin-top: 40px;

  padding: 32px;
  background: rgba(30, 41, 59, 0.7);
  border-radius: 16px;
  position: relative;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  width: 100%;
}

/* 自定义滚动条样式 */
.compression-timeline::-webkit-scrollbar {
  height: 6px;
  background: transparent;
}

.compression-timeline::-webkit-scrollbar-thumb {
  background: rgba(64, 158, 255, 0.3);
  border-radius: 3px;
  transition: background 0.3s ease;
}

.compression-timeline::-webkit-scrollbar-thumb:hover {
  background: rgba(64, 158, 255, 0.5);
}

.compression-timeline::-webkit-scrollbar-track {
  background: rgba(30, 41, 59, 0.3);
  border-radius: 3px;
}

/* Firefox 滚动条样式 */
.compression-timeline {
  scrollbar-width: thin;
  scrollbar-color: rgba(64, 158, 255, 0.3) rgba(30, 41, 59, 0.3);
}

.timeline-stages {
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: relative;
  padding: 0 12px;
  width: 100%;
  box-sizing: border-box;
  margin-left: 20px;
  margin-right: 100px;
}

.stage-container {
  display: flex;
  align-items: center;
  flex: 1;
}

.stage-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  opacity: 0.5;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.stage-item.active {
  opacity: 1;
}

.stage-circle {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: rgba(30, 41, 59, 0.5);
  border: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.stage-item.active .stage-circle {
  background: linear-gradient(135deg, #409eff 0%, #a855f7 100%);
  border: none;
  box-shadow: 0 8px 16px rgba(64, 158, 255, 0.2);
}

.stage-number {
  font-size: 15px;
  font-weight: 600;
  color: #94a3b8;
  font-family: 'DIN Alternate', -apple-system, BlinkMacSystemFont, sans-serif;
  transition: color 0.3s ease;
}

.stage-item.active .stage-number {
  color: white;
}

.stage-text {
  font-size: 13px;
  color: #94a3b8;
  text-align: center;
  max-width: 80px;
  line-height: 1.4;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  transition: all 0.3s ease;
}

.stage-item.active .stage-text {
  color: #409eff;
  font-weight: 600;
  transform: scale(1.05);
}

.stage-arrow {
  margin-bottom: 22px;

  transition: all 0.3s ease;
}

.stage-arrow path {
  transition: all 0.5s ease;
}

.stage-arrow.active path {
  animation: arrowFlow 2s ease-in-out infinite;
}

@keyframes arrowFlow {
  0% {
    stroke-dasharray: 0, 140;
    stroke-dashoffset: 0;
  }
  50% {
    stroke-dasharray: 140, 0;
    stroke-dashoffset: -140;
  }
  100% {
    stroke-dasharray: 140, 0;
    stroke-dashoffset: -280;
  }
}

/* 进度环样式优化 */
.progress-ring-container {
  padding-top: 20px;
  position: relative;
  width: 160px;
  height: 160px;
  margin: 0 auto 24px;
}

.progress-ring {
  transform: rotate(-90deg);
  filter: drop-shadow(0 4px 12px rgba(64, 158, 255, 0.2));
}

.progress-ring-circle {
  transition: stroke-dashoffset 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  stroke-linecap: round;
  stroke: url(#progress-gradient);
  filter: drop-shadow(0 0 8px rgba(64, 158, 255, 0.3));
}

.progress-ring-circle-bg {
  stroke: rgba(255, 255, 255, 0.1);
}

.progress-text {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  align-items: baseline;
  justify-content: center;
  font-family: 'DIN Alternate', -apple-system, BlinkMacSystemFont, sans-serif;
}

.progress-number {
  font-size: 48px;
  font-weight: 700;
  background: linear-gradient(135deg, #409eff 0%, #a855f7 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.progress-unit {
  font-size: 20px;
  margin-left: 2px;
  background: linear-gradient(135deg, #409eff 0%, #a855f7 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  opacity: 0.8;
}

.progress-status {
  font-size: 16px;
  color: #94a3b8;
  margin-top: 16px;
  font-weight: 500;
  text-align: center;
}

.timeline-item.active .timeline-progress-number {
  color: white;
  font-weight: 700;
}

@keyframes progressPulse {
  0% {
    opacity: 0.9;
    transform: scale(0.98);
  }
  50% {
    opacity: 1;
    transform: scale(1);
  }
  100% {
    opacity: 0.9;
    transform: scale(0.98);
  }
}

.progress-number {
  animation: progressPulse 2s ease-in-out infinite;
}

/* Result Section */
.result-card {
  background: rgba(30, 41, 59, 0.7);
  border-radius: 16px;
  padding: 32px;
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
}

.result-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 32px;
}

.result-header i {
  font-size: 24px;
  color: #67c23a;
}

.result-header span {
  font-size: 20px;
  font-weight: 600;
  color: #e6e8f0;
}

.comparison-container {
  display: flex;
  align-items: stretch;
  gap: 24px;
  margin-bottom: 32px;
}

.comparison-item {
  flex: 1;
  padding: 24px;
  border-radius: 12px;
  text-align: center;
  transition: all 0.3s ease;
}

.comparison-item:hover {
  transform: translateY(-2px);
}

.comparison-icon {
  width: 48px;
  height: 48px;
  margin: 0 auto 16px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.comparison-item.original .comparison-icon {
  background: rgba(103, 194, 58, 0.1);
  color: #67c23a;
}

.comparison-item.compressed .comparison-icon {
  background: rgba(64, 158, 255, 0.1);
  color: #409eff;
}

.comparison-item h4 {
  font-size: 18px;
  font-weight: 600;
  color: #e6e8f0;
  margin-bottom: 24px;
}

.comparison-stats {
  display: grid;
  gap: 16px;
}

.stat-item {
  padding: 12px;
  border-radius: 8px;
  background: rgba(30, 41, 59, 0.5);
}

.stat-label {
  font-size: 14px;
  color: #94a3b8;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 16px;
  font-weight: 500;
  color: #e6e8f0;
}

.comparison-arrow {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #409eff;
  font-size: 24px;
}

.compression-metrics {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24px;
  margin-bottom: 32px;
}

.metric-card {
  padding: 24px;
  border-radius: 12px;
  text-align: center;
  transition: all 0.3s ease;
}

.metric-card:hover {
  transform: translateY(-2px);
}

.metric-icon {
  width: 48px;
  height: 48px;
  margin: 0 auto 16px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.metric-value {
  font-size: 24px;
  font-weight: 600;
  color: #e6e8f0;
  margin-bottom: 8px;
}

.metric-label {
  font-size: 14px;
  color: #94a3b8;
}

/* Buttons */
.compression-actions {
  display: flex;
  gap: 16px;
}

.compress-btn,
.reset-btn,
.download-btn {
  padding: 12px 24px;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #fff;
}

.compress-btn {
  flex: 2;
  background: linear-gradient(135deg, #409eff 0%, #a855f7 100%);
  box-shadow: 0 8px 16px rgba(64, 158, 255, 0.2);
}

.compress-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 24px rgba(64, 158, 255, 0.3);
}

.compress-btn:disabled {
  background: linear-gradient(135deg, #c0ccda 0%, #909399 100%);
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.reset-btn {
  flex: 1;
  background: linear-gradient(135deg, #f56c6c 0%, #f56c6c 100%);
  box-shadow: 0 8px 16px rgba(245, 108, 108, 0.2);
}

.reset-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 24px rgba(245, 108, 108, 0.3);
}

.download-btn {
  width: 100%;
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
  box-shadow: 0 8px 16px rgba(103, 194, 58, 0.2);
  border: none;
}

.download-btn:hover {
  background: linear-gradient(135deg, #85ce61 0%, #95d475 100%);
  box-shadow: 0 8px 20px rgba(103, 194, 58, 0.3);
  border: none;
}

.preview-btn {
  width: 100%;
  background: linear-gradient(135deg, #409EFF 0%, #53a8ff 100%);
  box-shadow: 0 8px 16px rgba(64, 158, 255, 0.2);
  border: none;
}

.preview-btn:hover {
  background: linear-gradient(135deg, #53a8ff 0%, #66b1ff 100%);
  box-shadow: 0 8px 20px rgba(64, 158, 255, 0.3);
  border: none;
}

.preview-btn[disabled] {
  background: rgba(148, 163, 184, 0.1) !important;
  box-shadow: none !important;
  border: 1px solid rgba(148, 163, 184, 0.05) !important;
  color: rgba(148, 163, 184, 0.5) !important;
  cursor: not-allowed;
}

/* Main Content */
.main-content {
  max-width: 800px;
  margin: 0 auto;
  transition: all 0.3s ease;

}

/* 上传文件后的两列布局 */
.main-content.has-file {
  max-width: none;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  align-items: start;
}

.upload-section {
  @extend .card-base;
  width: 100%;
  min-width: 0;
}

.control-section {
  @extend .card-base;
  position: sticky;
  top: 32px;
  width: 100%;
  min-width: 0;
}

/* 响应式布局 */
@media (max-width: 1200px) {
  .main-content.has-file {
    grid-template-columns: 1fr;
    max-width: 800px;
    margin: 0 auto;

    gap: 24px;
  }

  .control-section {
    position: static;
  }
}

@media (max-width: 768px) {
  .video-compressor-container {
    padding: 16px;
  }

  .hero-section {
    padding: 32px 0;
  }

  .tool-title {
    font-size: 32px;
  }

  .upload-section,
  .control-section {
    padding: 16px;
  }

  .process-card {
    flex-direction: column;
    gap: 32px;
  }

  .process-step:not(:last-child)::after {
    top: auto;
    bottom: -16px;
    right: auto;
    left: 24px;
    width: 2px;
    height: 32px;
  }

  .upload-zone {
    padding: 16px;
  }

  .info-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .quality-selector,
  .format-selector {
    flex-direction: column;
  }

  .compression-actions {
    flex-direction: column;
  }

  .comparison-container {
    flex-direction: column;
  }

  .comparison-arrow {
    transform: rotate(90deg);
  }

  .compression-metrics {
    grid-template-columns: 1fr;
  }

  .compression-timeline {
    padding: 20px;
  }

  .timeline-stages {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .stage-container {
    width: 100%;
  }

  .stage-arrow {
    transform: rotate(90deg);
    margin: 8px 0;
  }

  .stage-circle {
    width: 40px;
    height: 40px;
  }

  .stage-number {
    font-size: 13px;
  }

  .stage-text {
    font-size: 12px;
    max-width: 100%;
  }
}

/* 添加弹框样式 */
.stage-dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.stage-dialog {
  background: rgba(30, 41, 59, 0.95);
  border-radius: 16px;
  padding: 24px;
  width: 90%;
  max-width: 480px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
}

.dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.dialog-header h3 {
  font-size: 20px;
  font-weight: 600;
  color: #e6e8f0;
  margin: 0;
}

.close-btn {
  background: none;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  padding: 4px;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.close-btn:hover {
  color: #e6e8f0;
  transform: scale(1.1);
}

.stage-description {
  color: #94a3b8;
  font-size: 16px;
  line-height: 1.6;
  margin-bottom: 20px;
}

.stage-details {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.detail-item {
  color: #e6e8f0;
  font-size: 14px;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  transition: all 0.3s ease;
}

.detail-item:hover {
  background: rgba(255, 255, 255, 0.1);
  transform: translateX(4px);
}

/* 增强时间线点击效果 */
.stage-item {
  cursor: pointer;
}

.stage-circle {
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.stage-item:hover .stage-circle {
  transform: scale(1.1);
  box-shadow: 0 0 20px rgba(64, 158, 255, 0.3);
}

.stage-item.active:hover .stage-circle {
  box-shadow: 0 0 20px rgba(64, 158, 255, 0.5);
}

@media (max-width: 768px) {
  .stage-dialog {
    width: calc(100% - 32px);
    margin: 16px;
    padding: 20px;
  }

  .dialog-header h3 {
    font-size: 18px;
  }

  .stage-description {
    font-size: 14px;
  }

  .detail-item {
    font-size: 13px;
    padding: 10px 14px;
  }
}

/* 压缩结果样式 */
.compression-result {
  margin-top: 32px;
  padding: 32px;
  background: rgba(30, 41, 59, 0.7);
  border-radius: 20px;
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.result-title {
  font-size: 24px;
  font-weight: 600;
  color: #e2e8f0;
  margin-bottom: 32px;
  padding-bottom: 20px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.1);
  display: flex;
  align-items: center;
  gap: 12px;
}

.result-title i {
  color: #10b981;
  font-size: 24px;
}

.result-table {
  width: 100%;
  border-radius: 16px;
  overflow: hidden;
  background: rgba(15, 23, 42, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.table-row {
  display: grid;
  grid-template-columns: 1.5fr 1fr 1fr 1fr;
  align-items: center;
  transition: background-color 0.3s ease;
}

.table-row:not(.header-row):hover {
  background: rgba(255, 255, 255, 0.03);
}

.header-row {
  background: rgba(15, 23, 42, 0.5);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.table-header {
  padding: 20px;
  font-weight: 600;
  color: #94a3b8;
  font-size: 14px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.table-label {
  padding: 20px;
  color: #94a3b8;
  font-weight: 500;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.table-label i {
  font-size: 16px;
  opacity: 0.7;
}

.table-cell {
  padding: 20px;
  color: #e2e8f0;
  font-size: 14px;
  font-family: 'DIN Alternate', -apple-system, BlinkMacSystemFont, sans-serif;
}

.time-cell {
  color: #10b981;
  font-weight: 500;
}

.reduction {
  color: #10b981;
}

.reduction-value {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  background: rgba(16, 185, 129, 0.1);
  border: 1px solid rgba(16, 185, 129, 0.2);
  border-radius: 6px;
  font-weight: 600;
  font-size: 13px;
}

.status-tag {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 6px;
  font-weight: 500;
  font-size: 13px;
}

.status-tag.unchanged {
  background: rgba(148, 163, 184, 0.1);
  border: 1px solid rgba(148, 163, 184, 0.2);
  color: #94a3b8;
}

.status-tag.optimized {
  background: rgba(16, 185, 129, 0.1);
  border: 1px solid rgba(16, 185, 129, 0.2);
  color: #10b981;
}

.result-actions {
  margin-top: 32px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.action-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.download-btn {
  width: 100%;
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
  box-shadow: 0 8px 16px rgba(103, 194, 58, 0.2);
  border: none;
}

.download-btn:hover {
  background: linear-gradient(135deg, #85ce61 0%, #95d475 100%);
  box-shadow: 0 8px 20px rgba(103, 194, 58, 0.3);
  border: none;
}

.preview-btn {
  width: 100%;
  background: linear-gradient(135deg, #409EFF 0%, #53a8ff 100%);
  box-shadow: 0 8px 16px rgba(64, 158, 255, 0.2);
  border: none;
}

.preview-btn:hover {
  background: linear-gradient(135deg, #53a8ff 0%, #66b1ff 100%);
  box-shadow: 0 8px 20px rgba(64, 158, 255, 0.3);
  border: none;
}

.preview-btn[disabled] {
  background: rgba(148, 163, 184, 0.1) !important;
  box-shadow: none !important;
  border: 1px solid rgba(148, 163, 184, 0.05) !important;
  color: rgba(148, 163, 184, 0.5) !important;
  cursor: not-allowed;
}

/* 预览弹窗样式优化 */
:deep(.preview-dialog) {
  background: rgba(30, 41, 59, 0.95);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  max-width: 1200px;
  margin: 0 auto;
}

:deep(.preview-dialog .el-dialog__header) {
  padding: 20px 24px;
  margin: 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(15, 23, 42, 0.5);
  border-radius: 16px 16px 0 0;
}

:deep(.preview-dialog .el-dialog__title) {
  color: #e2e8f0;
  font-size: 18px;
  font-weight: 600;
  line-height: 1;
}

:deep(.preview-dialog .el-dialog__headerbtn) {
  top: 20px;
}

:deep(.preview-dialog .el-dialog__headerbtn .el-dialog__close) {
  color: #94a3b8;
  font-size: 18px;
}

:deep(.preview-dialog .el-dialog__body) {
  padding: 24px;
  margin: 0;
  background: rgba(15, 23, 42, 0.3);
}

:deep(.preview-dialog .el-dialog__footer) {
  padding: 16px 24px;
  margin: 0;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(15, 23, 42, 0.5);
  border-radius: 0 0 16px 16px;
}

.preview-container {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 24px;
  min-height: 400px;
}

.preview-main {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.chart-container {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.chart-item {
  background: rgba(15, 23, 42, 0.5);
  border-radius: 12px;
  padding: 16px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.chart-item h4 {
  margin: 0 0 16px;
  color: #e2e8f0;
  font-size: 16px;
  text-align: center;
}

.pie-chart, .bar-chart {
  height: 200px;
  width: 100%;
}

.preview-sidebar {
  background: rgba(15, 23, 42, 0.5);
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  padding: 20px;
}

.highlight-card {
  background: rgba(15, 23, 42, 0.3);
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 20px;
}

.card-header {
  color: #94a3b8;
  font-size: 14px;
  margin-bottom: 12px;
}

.card-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.original, .compressed {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.reduction {
  text-align: right;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.info-group {
  background: rgba(15, 23, 42, 0.3);
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;
}

.info-group h5 {
  margin: 0 0 8px;
  color: #94a3b8;
  font-size: 14px;
}

.compare-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 0;
}

.label {
  color: #94a3b8;
}

.value {
  color: #e2e8f0;
  font-weight: 500;
}

.value.success {
  color: #67c23a;
}

/* 移动端适配 */
@media (max-width: 768px) {
  :deep(.preview-dialog) {
    width: 95% !important;
    margin: 0 auto;
  }

  .preview-container {
    grid-template-columns: 1fr;
    gap: 16px;
    max-height: none;
  }

  .video-container {
    aspect-ratio: 16/9;
  }

  .video-info {
    padding: 16px;
  }

  .info-grid {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .dialog-footer {
    flex-direction: column-reverse;
    gap: 8px;
  }

  .dialog-footer .el-button {
    width: 100%;
  }
}

/* 弹窗样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.75);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  width: 80%;
  max-width: 1200px;
  max-height: 90vh;
  background: rgba(30, 41, 59, 0.95);
  border-radius: 16px;
  box-shadow: 0 24px 48px rgba(0, 0, 0, 0.2);
  display: flex;
  flex-direction: column;
  animation: modalFadeIn 0.3s ease;
}

@keyframes modalFadeIn {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.modal-header {
  padding: 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-header h3 {
  margin: 0;
  color: #e2e8f0;
  font-size: 18px;
  font-weight: 600;
}

.close-btn {
  background: none;
  border: none;
  color: #94a3b8;
  font-size: 24px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: all 0.3s ease;
}

.close-btn:hover {
  color: #e2e8f0;
  background: rgba(255, 255, 255, 0.1);
}

.modal-body {
  padding: 24px;
  overflow-y: auto;
}

.modal-footer {
  padding: 16px 24px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.modal-btn {
  padding: 8px 16px;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.3s ease;
}

.modal-btn.cancel {
  background: rgba(255, 255, 255, 0.1);
  color: #e2e8f0;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.modal-btn.cancel:hover {
  background: rgba(255, 255, 255, 0.15);
}

.modal-btn.download {
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
  color: white;
  border: none;
  box-shadow: 0 4px 12px rgba(103, 194, 58, 0.2);
}

.modal-btn.download:hover {
  background: linear-gradient(135deg, #85ce61 0%, #95d475 100%);
  box-shadow: 0 4px 16px rgba(103, 194, 58, 0.3);
}

/* 保持原有的视频容器和信息样式 */
.preview-container {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 24px;
  min-height: 400px;
}

.video-container {
  position: relative;
  width: 100%;
  background: #000;
  border-radius: 12px;
  overflow: hidden;
  aspect-ratio: 16/9;
}

.preview-video {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

</style>

