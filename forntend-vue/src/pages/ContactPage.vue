<template>
  <div class="contact-page">
    <!-- 时光地图背景 -->
    <div class="time-map-background">
      <div class="map-grid">
        <div v-for="i in 100" :key="i" class="map-cell"></div>
      </div>
    </div>

    <div class="hero-section">
      <h1 class="hero-title">联系我</h1>
      <p class="hero-subtitle">让我们开始一段美好的对话</p>
    </div>

    <div class="contact-container">
      <div class="contact-grid">
        <div class="contact-card" @click="handleContact('mail')">
          <div class="card-overlay theme-mail"></div>
          <div class="card-content">
            <div class="icon-wrapper">
              <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <rect width="20" height="16" x="2" y="4" rx="2"/>
                <path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7"/>
              </svg>
            </div>
            <div class="card-info">
              <h3>QQ邮箱</h3>
              <p class="description">随时恭候您的来信，我会尽快回复</p>
              <div class="value">109484028@qq.com</div>
            </div>
          </div>
        </div>

        <div class="contact-card" @click="handleContact('github')">
          <div class="card-overlay theme-github"></div>
          <div class="card-content">
            <div class="icon-wrapper">
              <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M15 22v-4a4.8 4.8 0 0 0-1-3.5c3 0 6-2 6-5.5.08-1.25-.27-2.48-1-3.5.28-1.15.28-2.35 0-3.5 0 0-1 0-3 1.5-2.64-.5-5.36-.5-8 0C6 2 5 2 5 2c-.3 1.15-.3 2.35 0 3.5A5.403 5.403 0 0 0 4 9c0 3.5 3 5.5 6 5.5-.39.49-.68 1.05-.85 1.65-.17.6-.22 1.23-.15 1.85v4"/>
                <path d="M9 18c-4.51 2-5-2-7-2"/>
              </svg>
            </div>
            <div class="card-info">
              <h3>GitHub</h3>
              <p class="description">欢迎查看我的开源项目</p>
              <div class="value">humenglover</div>
            </div>
          </div>
        </div>

        <div class="contact-card" @click="handleContact('bilibili')">
          <div class="card-overlay theme-bilibili"></div>
          <div class="card-content">
            <div class="icon-wrapper">
              <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
                <path d="M17.813 4.653h.854c1.51.054 2.769.578 3.773 1.574 1.004.995 1.524 2.249 1.56 3.76v7.36c-.036 1.51-.556 2.769-1.56 3.773s-2.262 1.524-3.773 1.56H5.333c-1.51-.036-2.769-.556-3.773-1.56S.036 18.858 0 17.347v-7.36c.036-1.511.556-2.765 1.56-3.76 1.004-.996 2.262-1.52 3.773-1.574h.774l-1.174-1.12a1.234 1.234 0 0 1-.373-.906c0-.356.124-.658.373-.907l.027-.027c.267-.249.573-.373.92-.373.347 0 .653.124.92.373L9.653 4.44c.071.071.134.142.187.213h4.267a.836.836 0 0 1 .16-.213l2.853-2.747c.267-.249.573-.373.92-.373.347 0 .662.151.929.4.267.249.391.551.391.907 0 .355-.124.657-.373.906zM5.333 7.24c-.746.018-1.373.276-1.88.773-.506.498-.769 1.13-.786 1.894v7.52c.017.764.28 1.395.786 1.893.507.498 1.134.756 1.88.773h13.334c.746-.017 1.373-.275 1.88-.773.506-.498.769-1.129.786-1.893v-7.52c-.017-.765-.28-1.396-.786-1.894-.507-.497-1.134-.755-1.88-.773zM8 11.107c.373 0 .684.124.933.373.25.249.383.569.4.96v1.173c-.017.391-.15.711-.4.96-.249.25-.56.374-.933.374s-.684-.125-.933-.374c-.25-.249-.383-.569-.4-.96V12.44c0-.373.129-.689.386-.947.258-.257.574-.386.947-.386zm8 0c.373 0 .684.124.933.373.25.249.383.569.4.96v1.173c-.017.391-.15.711-.4.96-.249.25-.56.374-.933.374s-.684-.125-.933-.374c-.25-.249-.383-.569-.4-.96V12.44c.017-.391.15-.711.4-.96.249-.249.56-.373.933-.373z"/>
              </svg>
            </div>
            <div class="card-info">
              <h3>哔哩哔哩</h3>
              <p class="description">分享技术与生活的点点滴滴</p>
              <div class="value">bili_45532304845</div>
            </div>
          </div>
        </div>

        <div class="contact-card" @click="handleContact('tiktok')">
          <div class="card-overlay theme-tiktok"></div>
          <div class="card-content">
            <div class="icon-wrapper">
              <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
                <path d="M16.6 5.82s.51.5 0 0A4.278 4.278 0 0 1 15.54 3h-3.09v12.4a2.592 2.592 0 0 1-2.59 2.5c-1.42 0-2.6-1.16-2.6-2.6 0-1.72 1.66-3.01 3.37-2.48V9.66c-3.45-.46-6.47 2.22-6.47 5.64 0 3.33 2.76 5.7 5.69 5.7 3.14 0 5.69-2.55 5.69-5.7V9.01a7.35 7.35 0 0 0 4.3 1.38V7.3s-1.88.09-3.24-1.48z"/>
              </svg>
            </div>
            <div class="card-info">
              <h3>抖音</h3>
              <p class="description">记录精彩瞬间</p>
              <div class="value">68316901625</div>
            </div>
          </div>
        </div>
      </div>

      <div class="quotes-section">
        <div class="section-title">
          <span class="title-text">启发我的话语</span>
        </div>
        <div class="quotes-container">
          <div class="gradient-overlay left"></div>
          <div class="gradient-overlay right"></div>

          <div class="quotes-track" :style="{ transform: 'translateX(' + trackPosition + 'px)' }">
            <div v-for="(quote, index) in firstRowQuotes" :key="index" class="quote-card">
              <div class="quote-header">
                <div class="avatar-wrapper">
                  <img :src="'https://avatar.vercel.sh/' + quote.username.toLowerCase()" :alt="quote.name">
                </div>
                <div class="author-info">
                  <h4>{{ quote.name }}</h4>
                  <p>{{ quote.title }}</p>
                </div>
              </div>
              <blockquote>{{ quote.quote }}</blockquote>
            </div>
          </div>

          <div class="quotes-track reverse" :style="{ transform: 'translateX(' + -trackPosition + 'px)' }">
            <div v-for="(quote, index) in secondRowQuotes" :key="index" class="quote-card">
              <div class="quote-header">
                <div class="avatar-wrapper">
                  <img :src="'https://avatar.vercel.sh/' + quote.username.toLowerCase()" :alt="quote.name">
                </div>
                <div class="author-info">
                  <h4>{{ quote.name }}</h4>
                  <p>{{ quote.title }}</p>
                </div>
              </div>
              <blockquote>{{ quote.quote }}</blockquote>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

const trackPosition = ref(0)
let animationFrame: number

const animate = () => {
  trackPosition.value -= 0.5
  animationFrame = requestAnimationFrame(animate)
}

onMounted(() => {
  animationFrame = requestAnimationFrame(animate)
})

onUnmounted(() => {
  cancelAnimationFrame(animationFrame)
})

const handleContact = (type: string) => {
  switch (type) {
    case 'mail':
      window.location.href = 'mailto:109484028@qq.com'
      break
    case 'github':
      window.open('https://github.com/humenglover', '_blank')
      break
    case 'bilibili':
      window.open('https://b23.tv/pgoOOmz', '_blank')
      break
    case 'tiktok':
      window.open('https://www.douyin.com/user/68316901625', '_blank')
      break
  }
}

const quotes = [
  {
    name: "爱因斯坦",
    username: "einstein",
    title: "物理学家",
    quote: "想象力比知识更重要，因为知识是有限的，而想象力概括着世界的一切，推动着进步，并且是知识进化的源泉。"
  },
  {
    name: "马云",
    username: "jackma",
    title: "阿里巴巴创始人",
    quote: "今天很残酷，明天更残酷，后天很美好，但是大多数人死在明天晚上。"
  },
  {
    name: "乔布斯",
    username: "jobs",
    title: "Apple 创始人",
    quote: "你的时间有限，所以不要为别人而活。不要被教条所限，不要活在别人的观念里。"
  },
  {
    name: "孔子",
    username: "confucius",
    title: "思想家",
    quote: "三人行，必有我师焉。择其善者而从之，其不善者而改之。"
  },
  {
    name: "马斯克",
    username: "musk",
    title: "Tesla & SpaceX CEO",
    quote: "当某件事很重要时，即使形势不利，你也要去做。"
  },
  {
    name: "李小龙",
    username: "brucelee",
    title: "武术家",
    quote: "不要祈求一帆风顺，要准备直面风浪。"
  },
  {
    name: "老子",
    username: "laozi",
    title: "哲学家",
    quote: "千里之行，始于足下。合抱之木，生于毫末。"
  },
  {
    name: "尼采",
    username: "nietzsche",
    title: "哲学家",
    quote: "那些杀不死我的，使我更强大。"
  },
  {
    name: "曾国藩",
    username: "zengguofan",
    title: "政治家",
    quote: "天下无难事，只怕有心人。做事要循序渐进，不可急于求成。"
  },
  {
    name: "王阳明",
    username: "wangyang",
    title: "思想家",
    quote: "故立志者，为学之心也；为学者，立志之事也。"
  },
  {
    name: "苏轼",
    username: "sushi",
    title: "文学家",
    quote: "古之立大事者，不惟有超世之才，亦必有坚忍不拔之志。"
  },
  {
    name: "庄子",
    username: "zhuangzi",
    title: "哲学家",
    quote: "人生天地之间，若白驹过隙，忽然而已。"
  },
  {
    name: "卡耐基",
    username: "carnegie",
    title: "作家",
    quote: "要让别人喜欢你，首先你要学会喜欢别人。"
  },
  {
    name: "鲁迅",
    username: "luxun",
    title: "作家",
    quote: "其实地上本没有路，走的人多了，也便成了路。"
  },
  {
    name: "梁启超",
    username: "liangqichao",
    title: "思想家",
    quote: "少年智则国智，少年富则国富，少年强则国强。"
  },
  {
    name: "柏拉图",
    username: "plato",
    title: "哲学家",
    quote: "认识你自己。"
  }
]

const firstRowQuotes = quotes.slice(0, Math.ceil(quotes.length / 2))
const secondRowQuotes = quotes.slice(Math.ceil(quotes.length / 2))
</script>

<style scoped>
.contact-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #f8f9fa 0%, #ffffff 100%);
  position: relative;
  overflow: hidden;
}

/* 时光地图背景 */
.time-map-background {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 0;
  opacity: 0.05;
  pointer-events: none;
  background-color: var(--map-bg-color, #4a5568);
}

.map-grid {
  display: grid;
  grid-template-columns: repeat(10, 1fr);
  grid-template-rows: repeat(10, 1fr);
  height: 100%;
  gap: 1px;
}

.map-cell {
  background: currentColor;
  opacity: 0.1;
  animation: pulse 3s infinite;
  border-radius: 4px;
}

.map-cell:nth-child(3n) {
  animation-delay: 1s;
}

.map-cell:nth-child(3n + 1) {
  animation-delay: 2s;
}

@keyframes pulse {
  0% { opacity: 0.1; }
  50% { opacity: 0.3; }
  100% { opacity: 0.1; }
}

.hero-section {
  padding: 6rem 0 4rem;
  text-align: center;
  position: relative;
  z-index: 1;
}

.hero-section::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 100%;
  background: linear-gradient(180deg, rgba(255,255,255,0.8) 0%, #ffffff 100%);
  z-index: -1;
}

.contact-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 2rem;
  position: relative;
  z-index: 1;
}

.contact-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 2rem;
  margin-bottom: 4rem;
}

.contact-card {
  position: relative;
  border-radius: 1.5rem;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -1px rgba(0, 0, 0, 0.03);
}

.card-content {
  position: relative;
  z-index: 2;
  padding: 2rem;
  height: 100%;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
}

.card-overlay {
  position: absolute;
  inset: 0;
  z-index: 1;
  opacity: 0.9;
  transition: opacity 0.3s ease;
}

.contact-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.05), 0 10px 10px -5px rgba(0, 0, 0, 0.02);
}

.contact-card:hover .card-overlay {
  opacity: 1;
}

.theme-mail {
  background: linear-gradient(135deg, #E3F2FD 0%, #90CAF9 100%);
}

.theme-github {
  background: linear-gradient(135deg, #ECEFF1 0%, #B0BEC5 100%);
}

.theme-bilibili {
  background: linear-gradient(135deg, #FCE4EC 0%, #F48FB1 100%);
}

.theme-tiktok {
  background: linear-gradient(135deg, #FAFAFA 0%, #E0E0E0 100%);
}

.icon-wrapper {
  width: 48px;
  height: 48px;
  border-radius: 1rem;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 1.5rem;
  font-size: 1.5rem;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.theme-mail .icon-wrapper {
  color: #1E88E5;
}

.theme-github .icon-wrapper {
  color: #546E7A;
}

.theme-bilibili .icon-wrapper {
  color: #EC407A;
}

.theme-tiktok .icon-wrapper {
  color: #424242;
}

.card-info h3 {
  font-size: 1.5rem;
  font-weight: 600;
  color: #333333;
  margin-bottom: 0.75rem;
}

.description {
  color: #666666;
  font-size: 1rem;
  margin-bottom: 1.5rem;
  line-height: 1.6;
}

.value {
  background: rgba(255, 255, 255, 0.9);
  padding: 0.75rem 1.25rem;
  border-radius: 0.75rem;
  font-family: 'SF Mono', 'Fira Code', monospace;
  font-size: 0.875rem;
  color: #333333;
  display: inline-block;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.quotes-section {
  position: relative;
  padding: 6rem 0;
  background: linear-gradient(180deg, rgba(248,249,250,0.8) 0%, rgba(255,255,255,0.9) 100%);
  overflow: hidden;
  width: 100vw;
  margin-left: calc(-50vw + 50%);
  margin-right: calc(-50vw + 50%);
  border-top: 1px solid rgba(0,0,0,0.05);
  border-bottom: 1px solid rgba(0,0,0,0.05);
}

.section-title {
  text-align: center;
  margin-bottom: 3rem;
  position: relative;
}

.section-title::before {
  content: "";
  position: absolute;
  top: 50%;
  left: 0;
  right: 0;
  height: 1px;
  background: #eeeeee;
  z-index: 1;
}

.title-text {
  position: relative;
  z-index: 2;
  background: white;
  padding: 0 2rem;
  font-size: 1.5rem;
  font-weight: 600;
  color: #333333;
}

.quotes-container {
  position: relative;
  height: 500px;
  width: 100%;
  overflow: hidden;
}

.quotes-track {
  display: flex;
  gap: 3rem;
  position: absolute;
  padding: 2rem;
  min-width: 100%;
  will-change: transform;
}

.quotes-track.reverse {
  top: 250px;
}

.quote-card {
  width: 400px;
  padding: 2rem;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(10px);
  border-radius: 1.5rem;
  box-shadow:
    0 4px 6px -1px rgba(0, 0, 0, 0.05),
    0 2px 4px -1px rgba(0, 0, 0, 0.03),
    inset 0 0 0 1px rgba(255, 255, 255, 0.15);
  flex-shrink: 0;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  margin: 1rem 0;
}

.quote-card:hover {
  transform: translateY(-4px);
  box-shadow:
    0 20px 25px -5px rgba(0, 0, 0, 0.05),
    0 10px 10px -5px rgba(0, 0, 0, 0.02),
    inset 0 0 0 1px rgba(255, 255, 255, 0.2);
  background: rgba(255, 255, 255, 0.8);
}

.quote-header {
  display: flex;
  align-items: center;
  gap: 1.25rem;
  margin-bottom: 1.5rem;
}

.avatar-wrapper {
  width: 56px;
  height: 56px;
  border-radius: 1rem;
  overflow: hidden;
  box-shadow:
    0 4px 6px -1px rgba(0, 0, 0, 0.05),
    inset 0 0 0 1px rgba(255, 255, 255, 0.1);
}

.avatar-wrapper img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  filter: saturate(1.1);
}

.author-info h4 {
  font-size: 1.125rem;
  font-weight: 600;
  color: #2d3748;
  margin-bottom: 0.375rem;
}

.author-info p {
  font-size: 0.875rem;
  color: #718096;
}

blockquote {
  color: #4a5568;
  line-height: 1.8;
  font-size: 1rem;
  font-style: italic;
  position: relative;
  padding-left: 1.5rem;
}

blockquote::before {
  content: '"';
  position: absolute;
  left: 0;
  top: 0;
  font-size: 2rem;
  line-height: 1;
  color: #a0aec0;
  font-family: serif;
}

.gradient-overlay {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 25%;
  pointer-events: none;
  z-index: 10;
}

.gradient-overlay.left {
  left: 0;
  background: linear-gradient(to right, rgba(248,249,250,1) 0%, rgba(248,249,250,0) 100%);
}

.gradient-overlay.right {
  right: 0;
  background: linear-gradient(to left, rgba(248,249,250,1) 0%, rgba(248,249,250,0) 100%);
}

@media (max-width: 1200px) {
  .contact-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .contact-grid {
    grid-template-columns: 1fr;
  }

  .contact-container {
    padding: 0 1rem;
  }

  .quote-card {
    width: 300px;
  }

  .gradient-overlay {
    width: 15%;
  }
}

@media (prefers-color-scheme: dark) {
  .contact-page {
    background: linear-gradient(135deg, #1a202c 0%, #2d3748 100%);
  }

  .time-map-background {
    opacity: 0.03;
    --map-bg-color: #a0aec0;
  }

  .quotes-section {
    background: linear-gradient(180deg, rgba(26,32,44,0.8) 0%, rgba(45,55,72,0.9) 100%);
    border-top: 1px solid rgba(255,255,255,0.05);
    border-bottom: 1px solid rgba(255,255,255,0.05);
  }

  .quote-card {
    background: rgba(45, 55, 72, 0.7);
    box-shadow:
      0 4px 6px -1px rgba(0, 0, 0, 0.2),
      0 2px 4px -1px rgba(0, 0, 0, 0.1),
      inset 0 0 0 1px rgba(255, 255, 255, 0.05);
  }

  .quote-card:hover {
    background: rgba(45, 55, 72, 0.8);
    box-shadow:
      0 20px 25px -5px rgba(0, 0, 0, 0.2),
      0 10px 10px -5px rgba(0, 0, 0, 0.1),
      inset 0 0 0 1px rgba(255, 255, 255, 0.1);
  }

  .author-info h4 {
    color: #e2e8f0;
  }

  .author-info p {
    color: #a0aec0;
  }

  blockquote {
    color: #cbd5e0;
  }

  blockquote::before {
    color: #718096;
  }

  .gradient-overlay.left {
    background: linear-gradient(to right, rgba(26,32,44,1) 0%, rgba(26,32,44,0) 100%);
  }

  .gradient-overlay.right {
    background: linear-gradient(to left, rgba(26,32,44,1) 0%, rgba(26,32,44,0) 100%);
  }

  .avatar-wrapper {
    box-shadow:
      0 4px 6px -1px rgba(0, 0, 0, 0.2),
      inset 0 0 0 1px rgba(255, 255, 255, 0.05);
  }

  .avatar-wrapper img {
    filter: saturate(0.9) brightness(0.9);
  }
}
</style>
