<template>
  <div class="games-page">
    <!-- 添加像素风格装饰元素 -->
    <div class="pixel-decoration">
      <div class="pixel-star" style="top: 10%; left: 10%;"></div>
      <div class="pixel-star" style="top: 20%; left: 80%;"></div>
      <div class="pixel-star" style="top: 70%; left: 20%;"></div>
      <div class="pixel-star" style="top: 80%; left: 90%;"></div>
      <div class="pixel-cloud" style="top: 30%; left: 30%;"></div>
      <div class="pixel-cloud" style="top: 50%; left: 70%;"></div>
    </div>

    <div class="page-header">
      <h2 class="page-title">休闲游戏</h2>
      <p class="page-subtitle">放松一下,玩个小游戏吧</p>
    </div>

    <!-- PC端游戏列表 -->
    <div class="games-grid">
      <!-- 贪吃蛇游戏卡片 -->
      <div class="game-card" @click="router.push('/games/snake')">
        <div class="game-icon">
          <div class="snake-icon">
            <div class="snake-body snake-head"></div>
            <div class="snake-body snake-body-1"></div>
            <div class="snake-body snake-body-2"></div>
            <div class="snake-body snake-body-3"></div>
            <div class="snake-food"></div>
          </div>
        </div>
        <div class="game-info">
          <h3>贪吃蛇</h3>
          <p>经典的贪吃蛇游戏</p>
          <div class="game-features">
            <span class="feature-tag">键盘控制</span>
            <span class="feature-tag">休闲</span>
          </div>
        </div>
      </div>

      <!-- 2048游戏卡片 -->
      <div class="game-card" @click="router.push('/games/2048')">
        <div class="game-icon">
          <div class="game-2048-icon">
            <div class="tile-preview">2048</div>
          </div>
        </div>
        <div class="game-info">
          <h3>2048</h3>
          <p>数字方块益智游戏</p>
          <div class="game-features">
            <span class="feature-tag">滑动</span>
            <span class="feature-tag">益智</span>
          </div>
        </div>
      </div>

      <!-- 连连看游戏卡片 -->
      <div class="game-card" @click="router.push('/games/link-link')">
        <div class="game-icon">
          <div class="link-link-icon">
            <div class="link-preview">
              <div class="link-item">🐱</div>
              <div class="link-path"></div>
              <div class="link-item">🐱</div>
            </div>
          </div>
        </div>
        <div class="game-info">
          <h3>连连看</h3>
          <p>经典的连连看消除游戏</p>
          <div class="game-features">
            <span class="feature-tag">益智</span>
            <span class="feature-tag">休闲</span>
          </div>
        </div>
      </div>

      <!-- 俄罗斯方块游戏卡片 -->
      <div class="game-card" @click="router.push('/games/tetris')">
        <div class="game-icon">
          <div class="tetris-icon">
            <div class="tetris-preview">
              <div class="tetris-block"></div>
              <div class="tetris-block"></div>
              <div class="tetris-block"></div>
              <div class="tetris-block"></div>
            </div>
          </div>
        </div>
        <div class="game-info">
          <h3>俄罗斯方块</h3>
          <p>经典俄罗斯方块游戏</p>
          <div class="game-features">
            <span class="feature-tag">键盘控制</span>
            <span class="feature-tag">挑战</span>
          </div>
        </div>
      </div>

      <!-- 八皇后游戏卡片 -->
      <div class="game-card" @click="router.push('/games/queens')">
        <div class="game-icon">
          <div class="queens-icon">
            <div class="queen-preview">♕</div>
          </div>
        </div>
        <div class="game-info">
          <h3>八皇后</h3>
          <p>经典的八皇后益智游戏</p>
          <div class="game-features">
            <span class="feature-tag">益智</span>
            <span class="feature-tag">策略</span>
          </div>
        </div>
      </div>

      <!-- 数字华容道游戏卡片 -->
      <div class="game-card" @click="router.push('/games/sliding-puzzle')">
        <div class="game-icon">
          <div class="sliding-puzzle-icon">
            <div class="puzzle-preview">
              <div class="puzzle-number">15</div>
              <div class="puzzle-empty"></div>
              <div class="puzzle-number">14</div>
              <div class="puzzle-number">13</div>
            </div>
          </div>
        </div>
        <div class="game-info">
          <h3>数字华容道</h3>
          <p>经典的数字拼图游戏</p>
          <div class="game-features">
            <span class="feature-tag">益智</span>
            <span class="feature-tag">挑战</span>
          </div>
        </div>
      </div>

      <!-- 打砖块游戏卡片 -->
      <div class="game-card" @click="router.push('/games/brick')">
        <div class="game-icon">
          <div class="brick-game-icon">
            <div class="brick-preview">
              <div class="brick"></div>
              <div class="brick"></div>
              <div class="paddle"></div>
              <div class="ball"></div>
            </div>
          </div>
        </div>
        <div class="game-info">
          <h3>打砖块</h3>
          <p>经典的打砖块游戏</p>
          <div class="game-features">
            <span class="feature-tag">休闲</span>
            <span class="feature-tag">挑战</span>
          </div>
        </div>
      </div>

      <!-- 恐龙快跑游戏卡片 -->
      <div class="game-card" @click="router.push('/games/dino')">
        <div class="game-icon">
          <div class="dino-game-icon">
            <div class="dino-preview">
              <div class="dino"></div>
              <div class="cactus"></div>
              <div class="ground"></div>
            </div>
          </div>
        </div>
        <div class="game-info">
          <h3>恐龙快跑</h3>
          <p>经典的跳跃闯关游戏</p>
          <div class="game-features">
            <span class="feature-tag">跳跃</span>
            <span class="feature-tag">闯关</span>
          </div>
        </div>
      </div>

      <!-- 记忆翻牌游戏卡片 -->
<!--      <div class="game-card" @click="router.push('/games/memory-card')">-->
<!--        <div class="game-icon">-->
<!--          <div class="memory-card-icon">-->
<!--            <div class="card-preview">-->
<!--              <div class="preview-card"></div>-->
<!--              <div class="preview-card flipped"></div>-->
<!--            </div>-->
<!--          </div>-->
<!--        </div>-->
<!--        <div class="game-info">-->
<!--          <h3>记忆翻牌</h3>-->
<!--          <p>经典的记忆配对游戏</p>-->
<!--          <div class="game-features">-->
<!--            <span class="feature-tag">记忆</span>-->
<!--            <span class="feature-tag">益智</span>-->
<!--          </div>-->
<!--        </div>-->
<!--      </div>-->

<!--      &lt;!&ndash; 扫雷游戏卡片 &ndash;&gt;-->
<!--      <div class="game-card" @click="router.push('/games/minesweeper')">-->
<!--        <div class="game-icon">-->
<!--          <div class="minesweeper-icon">-->
<!--            <div class="mine-preview">-->
<!--              <div class="mine-grid">-->
<!--                <div class="mine-cell">1</div>-->
<!--                <div class="mine-cell flag">🚩</div>-->
<!--                <div class="mine-cell">2</div>-->
<!--                <div class="mine-cell mine">💣</div>-->
<!--              </div>-->
<!--            </div>-->
<!--          </div>-->
<!--        </div>-->
<!--        <div class="game-info">-->
<!--          <h3>扫雷</h3>-->
<!--          <p>经典的扫雷益智游戏</p>-->
<!--          <div class="game-features">-->
<!--            <span class="feature-tag">策略</span>-->
<!--            <span class="feature-tag">益智</span>-->
<!--          </div>-->
<!--        </div>-->
<!--      </div>-->



      <!-- 敬请期待卡片 -->
      <div class="game-card coming-soon">
        <div class="game-icon">
          <QuestionOutlined />
        </div>
        <div class="game-info">
          <h3>敬请期待</h3>
          <p>更多有趣的游戏正在开发中...</p>
          <div class="game-features">
            <span class="feature-tag">开发中</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 移动端九宫格布局 -->
    <div class="mobile-games-grid">
      <!-- 贪吃蛇 -->
      <div class="mobile-game-card" @click="router.push('/games/snake')">
        <div class="mobile-game-icon">
          <div class="snake-icon">
            <div class="snake-body snake-head"></div>
            <div class="snake-body snake-body-1"></div>
            <div class="snake-body snake-body-2"></div>
            <div class="snake-body snake-body-3"></div>
            <div class="snake-food"></div>
          </div>
        </div>
        <div class="mobile-game-name">贪吃蛇</div>
      </div>

      <!-- 2048 -->
      <div class="mobile-game-card" @click="router.push('/games/2048')">
        <div class="mobile-game-icon">
          <div class="game-2048-icon">
            <div class="tile-preview">2048</div>
          </div>
        </div>
        <div class="mobile-game-name">2048</div>
      </div>

      <!-- 连连看 -->
      <div class="mobile-game-card" @click="router.push('/games/link-link')">
        <div class="mobile-game-icon">
          <div class="link-link-icon">
            <div class="link-preview">
              <div class="link-item">🐱</div>
              <div class="link-path"></div>
              <div class="link-item">🐱</div>
            </div>
          </div>
        </div>
        <div class="mobile-game-name">连连看</div>
      </div>

      <!-- 俄罗斯方块 -->
      <div class="mobile-game-card" @click="router.push('/games/tetris')">
        <div class="mobile-game-icon">
          <div class="tetris-icon">
            <div class="tetris-preview">
              <div class="tetris-block"></div>
              <div class="tetris-block"></div>
              <div class="tetris-block"></div>
              <div class="tetris-block"></div>
            </div>
          </div>
        </div>
        <div class="mobile-game-name">俄罗斯方块</div>
      </div>

      <!-- 八皇后 -->
      <div class="mobile-game-card" @click="router.push('/games/queens')">
        <div class="mobile-game-icon">
          <div class="queens-icon">
            <div class="queen-preview">♕</div>
          </div>
        </div>
        <div class="mobile-game-name">八皇后</div>
      </div>

      <!-- 数字华容道 -->
      <div class="mobile-game-card" @click="router.push('/games/sliding-puzzle')">
        <div class="mobile-game-icon">
          <div class="sliding-puzzle-icon">
            <div class="puzzle-preview">
              <div class="puzzle-number">15</div>
              <div class="puzzle-empty"></div>
              <div class="puzzle-number">14</div>
              <div class="puzzle-number">13</div>
            </div>
          </div>
        </div>
        <div class="mobile-game-name">数字华容道</div>
      </div>

<!--      &lt;!&ndash; 恐龙快跑 &ndash;&gt;-->
<!--      <div class="mobile-game-card" @click="router.push('/games/dino')">-->
<!--        <div class="mobile-game-icon">-->
<!--          <div class="dino-game-icon">-->
<!--            <div class="dino-preview">-->
<!--              <div class="dino"></div>-->
<!--              <div class="cactus"></div>-->
<!--              <div class="ground"></div>-->
<!--            </div>-->
<!--          </div>-->
<!--        </div>-->
<!--        <div class="mobile-game-name">恐龙快跑</div>-->
<!--      </div>-->

<!--      &lt;!&ndash; 记忆翻牌 &ndash;&gt;-->
<!--      <div class="mobile-game-card" @click="router.push('/games/memory-card')">-->
<!--        <div class="mobile-game-icon">-->
<!--          <div class="memory-card-icon">-->
<!--            <div class="card-preview">-->
<!--              <div class="preview-card"></div>-->
<!--              <div class="preview-card flipped"></div>-->
<!--            </div>-->
<!--          </div>-->
<!--        </div>-->
<!--        <div class="mobile-game-name">记忆翻牌</div>-->
<!--      </div>-->

<!--      &lt;!&ndash; 扫雷 &ndash;&gt;-->
<!--      <div class="mobile-game-card" @click="router.push('/games/minesweeper')">-->
<!--        <div class="mobile-game-icon">-->
<!--          <div class="minesweeper-icon">-->
<!--            <div class="mine-preview">-->
<!--              <div class="mine-grid">-->
<!--                <div class="mine-cell">1</div>-->
<!--                <div class="mine-cell flag">🚩</div>-->
<!--                <div class="mine-cell">2</div>-->
<!--                <div class="mine-cell mine">💣</div>-->
<!--              </div>-->
<!--            </div>-->
<!--          </div>-->
<!--        </div>-->
<!--        <div class="mobile-game-name">扫雷</div>-->
<!--      </div>-->



      <!-- 敬请期待 -->
      <div class="mobile-game-card coming-soon">
        <div class="mobile-game-icon">
          <QuestionOutlined />
        </div>
        <div class="mobile-game-name">敬请期待</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { QuestionOutlined } from '@ant-design/icons-vue'

const router = useRouter()
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Press+Start+2P&display=swap');

.games-page {
  min-height: 80vh;
  color: var(--text-primary);
  position: relative;
  overflow: hidden;
  margin: 4px;
  border-radius: 24px;
  padding: 40px 80px;
}

.background-animation {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.pixel-decoration {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: -1;
}

.pixel-star {
  position: absolute;
  width: 20px;
  height: 20px;
  background-color: #FFD700;
  clip-path: polygon(50% 0%, 61% 35%, 98% 35%, 68% 57%, 79% 91%, 50% 70%, 21% 91%, 32% 57%, 2% 35%, 39% 35%);
  animation: pixelTwinkle 2s infinite alternate;
}

.pixel-cloud {
  position: absolute;
  width: 60px;
  height: 30px;
  background: #fff;
  border: 3px solid #333;
  border-radius: 0;
}

.pixel-cloud::before,
.pixel-cloud::after {
  content: '';
  position: absolute;
  background: #fff;
  border: 3px solid #333;
  border-radius: 50%;
}

.pixel-cloud::before {
  width: 25px;
  height: 25px;
  top: -15px;
  left: 5px;
}

.pixel-cloud::after {
  width: 35px;
  height: 35px;
  top: -20px;
  right: 10px;
}

@keyframes pixelTwinkle {
  0% { opacity: 0.3; transform: scale(0.8); }
  100% { opacity: 1; transform: scale(1.2); }
}

.gradient-bg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, rgba(255, 142, 83, 0.1), rgba(255, 107, 107, 0.05));
  animation: gradientMove 10s ease infinite;
}

@keyframes gradientMove {
  0% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0% 50%;
  }
}

.page-header {
  position: relative;
  text-align: center;
  margin-bottom: 40px;
  padding: 30px;
  background: linear-gradient(135deg, rgba(255, 142, 83, 0.08), rgba(255, 107, 107, 0.08));
  border-radius: 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.03);
  backdrop-filter: blur(10px);
}

.page-title {
  font-size: 28px;
  font-weight: bold;
  color: #333;
  margin-bottom: 12px;
  letter-spacing: 2px;
  text-shadow: 3px 3px 0px #000;
  font-family: 'Press Start 2P', 'Courier New', monospace;
  background: linear-gradient(45deg, #ff6b35, #f7931e, #ffd23f, #06ffa5, #118ab2, #073b4c);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  text-fill-color: transparent;
  filter: drop-shadow(3px 3px 0px #000);
}

.page-subtitle {
  font-size: 14px;
  color: #666;
  font-weight: bold;
  opacity: 0.9;
  text-shadow: 2px 2px 0px #000;
  font-family: 'Press Start 2P', 'Courier New', monospace;
  letter-spacing: 1px;
}

.games-grid {
  position: relative;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

.game-card {
  background: var(--header-background);
  color: var(--text-primary);
  border: 4px solid #333;
  border-radius: 0;
  padding: 0;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  box-shadow: 6px 6px 0px #333;
  transition: all 0.1s ease;
  overflow: hidden;
  position: relative;
}

.game-card:hover {
  transform: translate(2px, 2px);
  box-shadow: 4px 4px 0px #333;
}

.game-icon {
  width: 100%;
  height: 0;
  padding-bottom: 100%;
  position: relative;
  background: #fff;
  border-bottom: 4px solid #333;
  border-radius: 0;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.game-icon > div {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 60%;
  height: 60%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.snake-icon {
  background: linear-gradient(135deg, #FF9D6C, #FF5E62);
  border: 4px solid #333;
  border-radius: 0;
  width: 100% !important;
  height: 100% !important;
  position: relative;
  overflow: hidden;
  box-shadow: inset -4px -4px 0px rgba(0, 0, 0, 0.2);
}

.snake-body {
  position: absolute;
  width: 16px;
  height: 16px;
  background: #fff;
  border: 2px solid #333;
  border-radius: 0;
}

.snake-head {
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 20px;
  height: 20px;
  border: 2px solid #333;
  border-radius: 0;
}

.snake-body-1 {
  top: 50%;
  left: 30%;
  transform: translateY(-50%);
}

.snake-body-2 {
  top: 50%;
  right: 30%;
  transform: translateY(-50%);
}

.snake-food {
  position: absolute;
  top: 30%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 12px;
  height: 12px;
  background: #fff;
  border: 2px solid #333;
  border-radius: 0;
  animation: pixelPulse 0.5s infinite alternate;
}

.game-2048-icon {
  background: linear-gradient(135deg, #edc22e, #f2b179);
  border: 4px solid #333;
  border-radius: 0;
  width: 100% !important;
  height: 100% !important;
  position: relative;
  overflow: hidden;
  box-shadow: inset -4px -4px 0px rgba(0, 0, 0, 0.2);
}

.tile-preview {
  width: 100%;
  height: 100%;
  background: #edc22e;
  border: 4px solid #333;
  border-radius: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: bold;
  color: #f9f6f2;
  font-family: 'Press Start 2P', 'Courier New', monospace;
  text-shadow: 2px 2px 0px #000;
}

.tetris-icon {
  background: linear-gradient(135deg, #FF6B6B, #FF8E53);
  border: 4px solid #333;
  border-radius: 0;
  width: 100% !important;
  height: 100% !important;
  position: relative;
  overflow: hidden;
  box-shadow: inset -4px -4px 0px rgba(0, 0, 0, 0.2);
}

.tetris-preview {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 4px;
}

.tetris-block {
  width: 20px;
  height: 20px;
  background: #fff;
  border: 2px solid #333;
  border-radius: 0;
}

@keyframes tetrisRotate {
  0% { transform: translate(-50%, -50%) rotate(0deg); }
  25% { transform: translate(-50%, -50%) rotate(90deg); }
  50% { transform: translate(-50%, -50%) rotate(180deg); }
  75% { transform: translate(-50%, -50%) rotate(270deg); }
  100% { transform: translate(-50%, -50%) rotate(360deg); }
}

@keyframes pixelPulse {
  0% { transform: translate(-50%, -50%) scale(1); }
  100% { transform: translate(-50%, -50%) scale(1.1); }
}

.queens-icon {
  background: linear-gradient(135deg, #f0d9b5, #b58863);
  border: 4px solid #333;
  border-radius: 0;
  width: 100% !important;
  height: 100% !important;
  box-shadow: inset -4px -4px 0px rgba(0, 0, 0, 0.2);
}

.sliding-puzzle-icon {
  background: linear-gradient(135deg, #00c6fb, #005bea);
  border: 4px solid #333;
  border-radius: 0;
  width: 100% !important;
  height: 100% !important;
  box-shadow: inset -4px -4px 0px rgba(0, 0, 0, 0.2);
}

.brick-game-icon {
  background: linear-gradient(135deg, #FF6B6B, #FF8E53);
  border: 4px solid #333;
  border-radius: 0;
  width: 100% !important;
  height: 100% !important;
  box-shadow: inset -4px -4px 0px rgba(0, 0, 0, 0.2);
}

.dino-game-icon {
  background: linear-gradient(135deg, #4CAF50, #8BC34A);
  border: 4px solid #333;
  border-radius: 0;
  width: 100% !important;
  height: 100% !important;
  box-shadow: inset -4px -4px 0px rgba(0, 0, 0, 0.2);
}

.memory-card-icon {
  background: linear-gradient(135deg, #00c6fb, #005bea);
  border: 4px solid #333;
  border-radius: 0;
  width: 100% !important;
  height: 100% !important;
  box-shadow: inset -4px -4px 0px rgba(0, 0, 0, 0.2);
}

.minesweeper-icon {
  background: linear-gradient(135deg, #4CAF50, #8BC34A);
  border: 4px solid #333;
  border-radius: 0;
  width: 100% !important;
  height: 100% !important;
  box-shadow: inset -4px -4px 0px rgba(0, 0, 0, 0.2);
}

.game-info {
  padding: 16px;
  background: #fff;
  border-top: 4px solid #333;
}

.game-info h3 {
  font-size: 16px;
  font-weight: bold;
  color: #333;
  margin: 0 0 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-family: 'Press Start 2P', 'Courier New', monospace;
  text-shadow: 1px 1px 0px #000;
}

.game-info p {
  font-size: 12px;
  color: #666;
  margin: 0 0 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.4;
  height: 40px;
  font-family: 'Press Start 2P', 'Courier New', monospace;
}

.game-features {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.feature-tag {
  padding: 4px 8px;
  border: 2px solid #333;
  border-radius: 0;
  font-size: 10px;
  color: #333;
  background: #f5f5f7;
  font-weight: bold;
  font-family: 'Press Start 2P', 'Courier New', monospace;
  transition: all 0.1s ease;
  box-shadow: 2px 2px 0px #333;
}

.feature-tag:hover {
  background: #eee;
  transform: translate(1px, 1px);
  box-shadow: 1px 1px 0px #333;
}

/* 响应式布局 */
@media screen and (max-width: 1400px) {
  .games-grid {
    grid-template-columns: repeat(3, 1fr);
  }

  .games-page {
    padding: 40px 60px;
  }
}

@media screen and (max-width: 1200px) {
  .games-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .games-page {
    padding: 40px;
  }
}

@media screen and (max-width: 768px) {
  .games-page {
    padding: 0;
    margin: 0;
    border-radius: 0;
  }

  .page-header {
    text-align: center;
    margin: 20px 0;
    padding: 0;
  }

  .page-title {
    font-size: 24px;
    margin-bottom: 8px;
  }

  .games-grid {
    display: none;
  }
}

/* 移动端九宫格样式 */
.mobile-games-grid {
  display: none;
  padding: 20px;
  gap: 16px;
  grid-template-columns: repeat(3, 1fr);
  animation: fadeInUp 0.6s ease;
}

.mobile-game-card {
  aspect-ratio: 1;
  background: rgba(255, 255, 255, 0.9);
  border: 4px solid #333;
  border-radius: 0;
  padding: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  transition: all 0.1s ease;
  box-shadow: 6px 6px 0px #333;
  position: relative;
  overflow: hidden;
}

.mobile-game-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(255, 142, 83, 0.1), rgba(255, 107, 107, 0.1));
  opacity: 0;
  transition: opacity 0.1s ease;
}

.mobile-game-card:active {
  transform: translate(2px, 2px);
  box-shadow: 4px 4px 0px #333;
  background: rgba(255, 255, 255, 0.95);
}

.mobile-game-card:active::before {
  opacity: 1;
}

.mobile-game-icon {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(255, 142, 83, 0.1), rgba(255, 107, 107, 0.1));
  border: 2px solid #333;
  border-radius: 0;
  margin-bottom: 4px;
}

.mobile-game-name {
  font-size: 12px;
  color: #1a1a1a;
  text-align: center;
  font-weight: bold;
  line-height: 1.3;
  font-family: 'Press Start 2P', 'Courier New', monospace;
  text-shadow: 1px 1px 0px #000;
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .games-page {
    padding: 0;
    margin: 0;
    border-radius: 0;
  }

  .page-header {
    position: relative;
    text-align: center;

    padding: 30px;
    background: linear-gradient(135deg, rgba(255, 142, 83, 0.08), rgba(255, 107, 107, 0.08));
    border: 4px solid #333;
    border-radius: 0;
    box-shadow: 6px 6px 0px #333;
    backdrop-filter: blur(10px);
  }

  .page-title {
    font-size: 24px;
    font-weight: bold;
    color: #333;
    margin-bottom: 12px;
    letter-spacing: 1px;
    text-shadow: 2px 2px 0px #000;
  }

  .page-subtitle {
    font-size: 12px;
    color: #666;
    font-weight: bold;
    opacity: 0.9;
    text-shadow: 1px 1px 0px #000;
  }

  .games-grid {
    display: none;
  }

  .mobile-games-grid {
    display: grid;
    width: 100%;
    height: 100%;
    margin: 0;
    padding: 16px;
    align-content: flex-start;
  }

  .mobile-game-card {
    padding: 10px;
    border: 4px solid #333;
    border-radius: 0;
    box-shadow: 4px 4px 0px #333;
  }

  .mobile-game-card:active {
    transform: translate(2px, 2px);
    box-shadow: 2px 2px 0px #333;
  }

  .mobile-game-icon {
    width: 40px;
    height: 40px;
    border: 2px solid #333;
    border-radius: 0;
  }

  .snake-icon,
  .game-2048-icon,
  .tetris-icon {
    width: 32px;
    height: 32px;
    border: 2px solid #333;
  }

  .snake-body {
    width: 8px;
    height: 8px;
    border: 1px solid #333;
  }

  .snake-head {
    width: 10px;
    height: 10px;
    border: 1px solid #333;
  }

  .snake-food {
    width: 8px;
    height: 8px;
    border: 1px solid #333;
  }

  .tetris-block {
    width: 8px;
    height: 8px;
    border: 1px solid #333;
  }

  .tile-preview {
    font-size: 10px;
    padding: 2px;
    border: 2px solid #333;
    text-shadow: 1px 1px 0px #000;
  }

  .mobile-game-name {
    font-size: 10px;
    margin-top: 2px;
    text-shadow: 1px 1px 0px #000;
  }

  .coming-soon {
    opacity: 0.6;
  }

  .brick-game-icon {
    width: 48px;
    height: 48px;
    padding: 5px;
    border: 2px solid #333;
  }

  .brick {
    width: 16px;
    height: 6px;
    border: 1px solid #333;
  }

  .paddle {
    width: 20px;
    height: 3px;
    bottom: 10px;
    border: 1px solid #333;
  }

  .ball {
    width: 5px;
    height: 5px;
    bottom: 16px;
    border: 1px solid #333;
  }

  .dino-game-icon {
    width: 48px;
    height: 48px;
    padding: 5px;
    border: 2px solid #333;
  }

  .dino {
    width: 16px;
    height: 16px;
    bottom: 10px;
    border: 1px solid #333;
  }

  .dino::before {
    width: 10px;
    height: 6px;
    top: -4px;
    border: 1px solid #333;
  }

  .cactus {
    width: 6px;
    height: 12px;
    bottom: 10px;
    border: 1px solid #333;
  }

  .cactus::before,
  .cactus::after {
    width: 4px;
    height: 6px;
    border: 1px solid #333;
  }

  .ground {
    bottom: 6px;
    border: 1px solid #333;
  }

  .memory-card-icon {
    width: 40px;
    height: 40px;
    padding: 4px;
    border: 2px solid #333;
  }

  .preview-card {
    width: 14px;
    height: 20px;
    border: 1px solid #333;
  }

  .preview-card:first-child {
    left: 6px;
  }

  .preview-card.flipped {
    right: 6px;
  }
}

/* 适配超小屏幕 */
@media screen and (max-width: 360px) {
  .mobile-games-grid {
    padding: 12px;
    gap: 12px;
  }

  .mobile-game-card {
    padding: 8px;
    border: 4px solid #333;
    border-radius: 0;
    box-shadow: 4px 4px 0px #333;
  }

  .mobile-game-card:active {
    transform: translate(2px, 2px);
    box-shadow: 2px 2px 0px #333;
  }

  .mobile-game-icon {
    width: 36px;
    height: 36px;
    border: 2px solid #333;
    border-radius: 0;
  }

  .snake-icon,
  .game-2048-icon,
  .tetris-icon {
    width: 28px;
    height: 28px;
    border: 2px solid #333;
  }

  .snake-body {
    width: 6px;
    height: 6px;
    border: 1px solid #333;
  }

  .snake-head {
    width: 8px;
    height: 8px;
    border: 1px solid #333;
  }

  .snake-food {
    width: 6px;
    height: 6px;
    border: 1px solid #333;
  }

  .tetris-block {
    width: 6px;
    height: 6px;
    border: 1px solid #333;
  }

  .tile-preview {
    font-size: 8px;
    padding: 1px;
    border: 2px solid #333;
    text-shadow: 1px 1px 0px #000;
  }

  .mobile-game-name {
    font-size: 9px;
    text-shadow: 1px 1px 0px #000;
  }

  .brick-game-icon {
    width: 40px;
    height: 40px;
    padding: 4px;
    border: 2px solid #333;
  }

  .brick {
    width: 14px;
    height: 5px;
    border: 1px solid #333;
  }

  .paddle {
    width: 16px;
    height: 3px;
    bottom: 8px;
    border: 1px solid #333;
  }

  .ball {
    width: 4px;
    height: 4px;
    bottom: 14px;
    border: 1px solid #333;
  }

  .dino-game-icon {
    width: 40px;
    height: 40px;
    padding: 4px;
    border: 2px solid #333;
  }

  .dino {
    width: 14px;
    height: 14px;
    bottom: 8px;
    border: 1px solid #333;
  }

  .dino::before {
    width: 8px;
    height: 5px;
    top: -3px;
    border: 1px solid #333;
  }

  .cactus {
    width: 5px;
    height: 10px;
    bottom: 8px;
    border: 1px solid #333;
  }

  .cactus::before,
  .cactus::after {
    width: 3px;
    height: 5px;
    border: 1px solid #333;
  }

  .ground {
    bottom: 5px;
    border: 1px solid #333;
  }

  .memory-card-icon {
    width: 36px;
    height: 36px;
    padding: 3px;
    border: 2px solid #333;
  }

  .preview-card {
    width: 12px;
    height: 18px;
    border: 1px solid #333;
  }

  .preview-card:first-child {
    left: 5px;
  }

  .preview-card.flipped {
    right: 5px;
  }
}

.queen-preview {
  font-size: 36px;
  color: #1a1a1a;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
  animation: queenFloat 3s ease-in-out infinite;
}

@keyframes queenFloat {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-4px);
  }
}

.puzzle-preview {
  width: 100%;
  height: 100%;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 2px;
  padding: 2px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 4px;
}

.puzzle-number {
  background: var(--header-background);
  color: var(--text-primary);
  border-radius: 2px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  color: #005bea;
  aspect-ratio: 1;
}

.puzzle-empty {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 2px;
  aspect-ratio: 1;
}

@media screen and (max-width: 360px) {
  .sliding-puzzle-icon {
    width: 40px;
    height: 40px;
    padding: 4px;
  }

  .puzzle-number {
    font-size: 10px;
  }
}

.brick-preview {
  width: 100%;
  height: 100%;
  position: relative;
}

.brick {
  position: absolute;
  width: 20px;
  height: 8px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 2px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.brick:nth-child(1) {
  top: 4px;
  left: 10px;
}

.brick:nth-child(2) {
  top: 4px;
  right: 10px;
}

.paddle {
  position: absolute;
  bottom: 12px;
  left: 50%;
  transform: translateX(-50%);
  width: 24px;
  height: 4px;
  background: var(--header-background);
  color: var(--text-primary);
  border-radius: 2px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.ball {
  position: absolute;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  width: 6px;
  height: 6px;
  background: var(--header-background);
  color: var(--text-primary);
  border-radius: 50%;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  animation: bounce 2s infinite ease-in-out;
}

@keyframes bounce {
  0%, 100% {
    transform: translate(-50%, 0);
  }
  50% {
    transform: translate(-50%, -16px);
  }
}

.dino-preview {
  width: 100%;
  height: 100%;
  position: relative;
}

.dino {
  position: absolute;
  bottom: 12px;
  left: 10px;
  width: 20px;
  height: 20px;
  background: var(--header-background);
  color: var(--text-primary);
  border-radius: 4px;
  transform: skew(-10deg);
  animation: dinoIdle 1s infinite ease-in-out;
}

.dino::before {
  content: '';
  position: absolute;
  top: -6px;
  left: 2px;
  width: 12px;
  height: 8px;
  background: var(--header-background);
  color: var(--text-primary);
  border-radius: 4px;
  transform: skew(10deg);
}

.cactus {
  position: absolute;
  bottom: 12px;
  right: 10px;
  width: 8px;
  height: 16px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 1px;
}

.cactus::before,
.cactus::after {
  content: '';
  position: absolute;
  width: 6px;
  height: 8px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 1px;
}

.cactus::before {
  top: 4px;
  left: -4px;
  transform: rotate(-30deg);
}

.cactus::after {
  top: 2px;
  right: -4px;
  transform: rotate(30deg);
}

.ground {
  position: absolute;
  bottom: 8px;
  left: 0;
  width: 100%;
  height: 2px;
  background: rgba(255, 255, 255, 0.7);
}

@keyframes dinoIdle {
  0%, 100% {
    transform: skew(-10deg) translateY(0);
  }
  50% {
    transform: skew(-10deg) translateY(-2px);
  }
}

.card-preview {
  width: 100%;
  height: 100%;
  position: relative;
  perspective: 1000px;
}

.preview-card {
  position: absolute;
  width: 20px;
  height: 28px;
  background: var(--header-background);
  color: var(--text-primary);
  border-radius: 4px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  transition: transform 0.6s;
  transform-style: preserve-3d;
}

.preview-card:first-child {
  left: 8px;
  top: 50%;
  transform: translateY(-50%) rotateY(0);
}

.preview-card.flipped {
  right: 8px;
  top: 50%;
  transform: translateY(-50%) rotateY(180deg);
  background: #f0f0f0;
}

.mine-preview {
  width: 100%;
  height: 100%;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.mine-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 2px;
  padding: 2px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 4px;
}

.mine-cell {
  width: 20px;
  height: 20px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 2px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: bold;
  color: #2196F3;
}

.mine-cell.flag {
  font-size: 10px;
}

.mine-cell.mine {
  font-size: 10px;
  background: rgba(244, 67, 54, 0.9);
}

.link-link-icon {
  background: linear-gradient(135deg, #FF6B6B, #FF8E53);
  border: 4px solid #333;
  border-radius: 0;
  width: 100% !important;
  height: 100% !important;
  position: relative;
  overflow: hidden;
  box-shadow: inset -4px -4px 0px rgba(0, 0, 0, 0.2);
}

.link-preview {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.link-item {
  font-size: 24px;
  animation: linkBounce 0.5s infinite ease-in-out alternate;
}

.link-path {
  width: 24px;
  height: 2px;
  background: #fff;
  position: relative;
  border: 1px solid #333;
}

.link-path::before,
.link-path::after {
  content: '';
  position: absolute;
  width: 8px;
  height: 2px;
  background: #fff;
  border: 1px solid #333;
}

.link-path::before {
  left: 0;
  transform: rotate(45deg);
  transform-origin: left center;
}

.link-path::after {
  right: 0;
  transform: rotate(-45deg);
  transform-origin: right center;
}

@keyframes linkBounce {
  from {
    transform: scale(1);
  }
  to {
    transform: scale(1.1);
  }
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .link-item {
    font-size: 18px;
  }

  .link-path {
    width: 16px;
  }

  .link-link-icon {
    border: 2px solid #333;
  }

  .link-item {
    text-shadow: 1px 1px 0px #000;
  }

  .link-path {
    border: 1px solid #333;
  }

  .link-path::before,
  .link-path::after {
    border: 1px solid #333;
  }
}

@media screen and (max-width: 360px) {
  .link-item {
    font-size: 14px;
  }

  .link-path {
    width: 12px;
  }

  .link-link-icon {
    border: 2px solid #333;
  }

  .link-item {
    text-shadow: 1px 1px 0px #000;
  }

  .link-path {
    border: 1px solid #333;
  }

  .link-path::before,
  .link-path::after {
    border: 1px solid #333;
  }
}
</style>
