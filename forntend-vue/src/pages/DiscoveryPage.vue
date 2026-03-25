<template>
  <div class="discovery-page">


    <div class="scroll-container">
      <div class="container">
        <!-- 社区与互动 - 采用大卡片展示 -->
        <div class="module">
          <div class="module-header">
            <h2 class="module-title">社区与互动</h2>
            <span class="module-subtitle">连接你我，分享快乐</span>
          </div>
          <div class="card-list featured-grid">
            <div class="card featured-card" @click="goToPage('/friend-links')">
              <div class="card-background bg-gradient-blue"></div>
              <div class="card-content">
                <div class="icon-box">
                  <LinkOutlined />
                </div>
                <div class="text-info">
                  <h3 class="card-name">友情链接</h3>
                  <p class="card-desc">探索更多伙伴站点</p>
                </div>
              </div>
            </div>
            <div class="card featured-card" @click="goToPage('/loveboard/list')">
              <div class="card-background bg-gradient-pink"></div>
              <div class="card-content">
                <div class="icon-box">
                  <HeartOutlined />
                </div>
                <div class="text-info">
                  <h3 class="card-name">恋爱画板</h3>
                  <p class="card-desc">专属色彩，铭刻浪漫</p>
                </div>
              </div>
            </div>
          </div>

          <div class="card-list secondary-row">
            <div class="card small-card" @click="goToPage('/barrage')">
              <div class="card-content">
                <div class="icon-box bg-green-soft">
                  <MessageOutlined />
                </div>
                <div class="text-info">
                  <h3 class="card-name">树洞</h3>
                  <p class="card-desc">诉说那些不为人知的心事</p>
                </div>
                <div class="arrow-icon">
                  <RightOutlined />
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 休闲游戏 - 采用 App Store 风格网格 -->
        <div class="module">
          <div class="module-header">
            <h2 class="module-title">精品游戏</h2>
            <span class="module-subtitle">指尖上的极致娱乐</span>
          </div>
          <div class="card-grid game-grid">
            <div class="card app-card" v-for="game in games" :key="game.path" @click="goToPage(game.path)">
              <div class="app-icon-wrap" :class="game.iconClass">
                <component :is="game.icon" />
              </div>
              <div class="app-info">
                <h3 class="app-name">{{ game.name }}</h3>
                <p class="app-category">{{ game.desc }}</p>
              </div>
              <button class="app-get-btn">开始</button>
            </div>
          </div>
        </div>

        <!-- 实用工具 - 简洁图标列表 -->
        <div class="module">
          <div class="module-header">
            <h2 class="module-title">实用工具</h2>
            <span class="module-subtitle">事半功倍，得心应手</span>
          </div>
          <div class="card-grid tools-grid">
            <div class="card tool-item" v-for="tool in tools" :key="tool.path" @click="goToPage(tool.path)">
              <div class="tool-icon" :class="tool.bgClass">
                <component :is="tool.icon" />
              </div>
              <h3 class="tool-name">{{ tool.name }}</h3>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { markRaw } from 'vue';
import { useRouter } from 'vue-router';
import {
  LinkOutlined, HeartOutlined, MessageOutlined, BookOutlined,
  BgColorsOutlined, CoffeeOutlined, CalculatorOutlined, ClockCircleOutlined,
  FileTextOutlined, NumberOutlined, RetweetOutlined, RightOutlined,
  CrownOutlined, AppstoreOutlined, BuildOutlined, ThunderboltOutlined,
  BlockOutlined, PartitionOutlined, RadiusUprightOutlined, ExperimentOutlined,
  AimOutlined, FireOutlined, ControlOutlined, SelectOutlined
} from '@ant-design/icons-vue';

const router = useRouter();
const goToPage = (path: string) => {
  router.push(path);
};

// 游戏列表数据配置
const games = [
  {
    name: '贪吃蛇',
    desc: '经典像素挑战',
    path: '/games/snake',
    iconClass: 'icon-snake',
    icon: markRaw(RadiusUprightOutlined)
  },
  {
    name: '2048',
    desc: '智慧数字拼图',
    path: '/games/2048',
    iconClass: 'icon-2048',
    icon: markRaw(AppstoreOutlined)
  },
  {
    name: '连连看',
    desc: '趣味宠物消除',
    path: '/games/link-link',
    iconClass: 'icon-link',
    icon: markRaw(PartitionOutlined)
  },
  {
    name: '俄罗斯方块',
    desc: '堆叠无限可能',
    path: '/games/tetris',
    iconClass: 'icon-tetris',
    icon: markRaw(BlockOutlined)
  },
  {
    name: '八皇后',
    desc: '国际象棋难题',
    path: '/games/queens',
    iconClass: 'icon-queens',
    icon: markRaw(CrownOutlined)
  },
  {
    name: '数字华容道',
    desc: '脑力极限拼图',
    path: '/games/sliding-puzzle',
    iconClass: 'icon-puzzle',
    icon: markRaw(NumberOutlined)
  },
  {
    name: '打砖块',
    desc: '热血街机弹球',
    path: '/games/brick',
    iconClass: 'icon-brick',
    icon: markRaw(BuildOutlined)
  },
  {
    name: '恐龙快跑',
    desc: '沙漠无尽奔跑',
    path: '/games/dino',
    iconClass: 'icon-dino',
    icon: markRaw(ThunderboltOutlined)
  },
  {
    name: '记忆翻牌',
    desc: '翻转卡片挑战',
    path: '/games/memory-card',
    iconClass: 'icon-memory',
    icon: markRaw(SelectOutlined)
  },
  {
    name: '扫雷',
    desc: '逻辑雷区拆除',
    path: '/games/minesweeper',
    iconClass: 'icon-mines',
    icon: markRaw(AimOutlined)
  }
];

// 工具列表数据配置
const tools = [
  { name: '记事本', path: '/reminder', icon: markRaw(BookOutlined), bgClass: 'tool-blue' },
  { name: '取色器', path: '/tools/color-picker', icon: markRaw(BgColorsOutlined), bgClass: 'tool-yellow' },
  { name: '吃什么', path: '/tools/food-wheel', icon: markRaw(CoffeeOutlined), bgClass: 'tool-green' },
  { name: '计算器', path: '/tools/calculator', icon: markRaw(CalculatorOutlined), bgClass: 'tool-purple' },
  { name: '计时器', path: '/tools/timer', icon: markRaw(ClockCircleOutlined), bgClass: 'tool-red' },
  { name: '便签墙', path: '/tools/sticky-wall', icon: markRaw(FileTextOutlined), bgClass: 'tool-cyan' },
  { name: '随机数', path: '/tools/random', icon: markRaw(NumberOutlined), bgClass: 'tool-red' },
  { name: '进制转换', path: '/tools/base-converter', icon: markRaw(RetweetOutlined), bgClass: 'tool-yellow' },
];
</script>

<style lang="scss" scoped>
.discovery-page {
  display: flex;
  flex-direction: column;
  background: var(--background);
  color: var(--text-primary);
  overflow: hidden;

  // 玻璃拟态轴头部
  .page-header {
    flex-shrink: 0;
    padding: 12px 24px;
    height: 64px;
    background: var(--ios-bg-blur);
    backdrop-filter: blur(20px);
    border-bottom: 0.5px solid var(--ios-header-border);
    display: flex;
    align-items: center;
    position: sticky;
    top: 0;
    z-index: 100;

    .page-title {
      font-size: 28px;
      font-weight: 700;
      color: var(--text-primary);
      margin: 0;
      letter-spacing: -0.6px;
    }
  }

  .scroll-container {
    flex: 1;
    overflow-y: auto;
    padding: 0;

    &::-webkit-scrollbar { width: 0; } // Hide scrollbar for a cleaner look
  }

  .container {
    max-width: 1400px;
    margin: 0 auto;
    padding: 24px 20px 100px;
  }

  // 模块样式
  .module {
    margin-bottom: 40px;

    .module-header {
      margin-bottom: 16px;
      padding-left: 4px;

      .module-title {
        font-size: 22px;
        font-weight: 700;
        color: var(--text-primary);
        margin: 0;
        letter-spacing: -0.4px;
      }

      .module-subtitle {
        font-size: 14px;
        color: var(--text-secondary);
        opacity: 0.8;
      }
    }
  }

  // 通用卡片样式
  .card {
    background: var(--card-background);
    border-radius: 16px;
    cursor: pointer;
    overflow: hidden;
    position: relative;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    border: 0.5px solid var(--ios-header-border);

    &:hover {
      transform: translateY(-4px) scale(1.01);
      box-shadow: 0 12px 30px var(--shadow-color);
    }

    &:active {
      transform: scale(0.98);
    }
  }

  // 精选大卡片网格
  .featured-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
    gap: 20px;
    margin-bottom: 20px;

    .featured-card {
      height: 180px;

      .card-background {
        position: absolute;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        z-index: 1;
        opacity: 0.9;
        transition: opacity 0.3s;
      }

      &:hover .card-background { opacity: 1; }

      .card-content {
        position: relative;
        z-index: 2;
        padding: 24px;
        height: 100%;
        display: flex;
        flex-direction: column;
        justify-content: flex-end;
        color: #fff;

        .icon-box {
          position: absolute;
          top: 24px;
          left: 24px;
          width: 44px;
          height: 44px;
          background: rgba(255, 255, 255, 0.2);
          backdrop-filter: blur(10px);
          border-radius: 12px;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 22px;
          border: 0.5px solid rgba(255, 255, 255, 0.3);
        }

        .text-info {
          .card-name {
            font-size: 20px;
            font-weight: 700;
            margin: 0;
            color: #fff;
          }
          .card-desc {
            font-size: 14px;
            opacity: 0.9;
            margin: 4px 0 0;
            color: #fff;
          }
        }
      }
    }
  }

  // 渐变色背景
  .bg-gradient-blue { background: linear-gradient(135deg, #007aff, #00c6ff); }
  .bg-gradient-pink { background: linear-gradient(135deg, #ff2d55, #ff5e3a); }

  // 小型卡片行
  .secondary-row {
    .small-card {
      padding: 16px 20px;

      .card-content {
        display: flex;
        align-items: center;
        gap: 16px;

        .icon-box {
          width: 48px;
          height: 48px;
          border-radius: 12px;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 24px;
          flex-shrink: 0;

          &.bg-green-soft {
            background: rgba(52, 199, 89, 0.1);
            color: #34c759;
          }
        }

        .text-info {
          flex: 1;
          .card-name { font-size: 16px; font-weight: 600; margin: 0; color: var(--text-primary); }
          .card-desc { font-size: 13px; color: var(--text-secondary); margin: 2px 0 0; }
        }

        .arrow-icon { color: var(--ios-tab-text); font-size: 14px; }
      }
    }
  }

  // 游戏网格 - App Store 风格
  .game-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 16px;

    .app-card {
      display: flex;
      align-items: center;
      padding: 12px;
      gap: 12px;
      background: var(--card-background);

      .app-icon-wrap {
        width: 64px;
        height: 64px;
        border-radius: 14px;
        flex-shrink: 0;
        display: flex;
        align-items: center;
        justify-content: center;
        overflow: hidden;
        position: relative;
        box-shadow: 0 4px 12px var(--shadow-color);
        font-size: 32px;
        color: #fff;

        &.icon-snake { background: linear-gradient(135deg, #34c759, #b4ec51); }
        &.icon-2048 { background: linear-gradient(135deg, #ff9500, #ffcc00); }
        &.icon-link { background: linear-gradient(135deg, #ff2d55, #ff375f); }
        &.icon-tetris { background: linear-gradient(135deg, #007aff, #5856d6); }
        &.icon-queens { background: linear-gradient(135deg, #af52de, #bf5af2); }
        &.icon-puzzle { background: linear-gradient(135deg, #5ac8fa, #66d3fa); }
        &.icon-brick { background: linear-gradient(135deg, #ff3b30, #ff9500); }
        &.icon-dino { background: linear-gradient(135deg, #8e8e93, #c7c7cc); }
        &.icon-memory { background: linear-gradient(135deg, #5856d6, #007aff); }
        &.icon-mines { background: linear-gradient(135deg, #1d1d1f, #48484a); }
      }

      .app-info {
        flex: 1;
        min-width: 0;
        .app-name { font-size: 16px; font-weight: 600; margin: 0; color: var(--text-primary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
        .app-category { font-size: 12px; color: var(--text-secondary); margin: 2px 0 0; }
      }

      .app-get-btn {
        padding: 4px 16px;
        background: var(--ios-tab-bg);
        border: none;
        border-radius: 14px;
        color: #007aff;
        font-size: 13px;
        font-weight: 700;
        cursor: pointer;
        transition: background 0.2s;

        &:hover { background: var(--ios-search-focus-bg); }
      }
    }
  }

  // 工具网格
  .tools-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(80px, 1fr));
    gap: 20px;
    padding: 4px;

    .tool-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 8px;
      padding: 0;
      background: transparent;
      border: none;
      box-shadow: none !important;

      .tool-icon {
        width: 60px;
        height: 60px;
        border-radius: 14px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 28px;
        color: #fff;
        transition: all 0.3s ease;
        box-shadow: 0 4px 10px var(--shadow-color);

        &.tool-blue { background: linear-gradient(135deg, #007aff, #00c6ff); }
        &.tool-yellow { background: linear-gradient(135deg, #ff9500, #ffcc00); }
        &.tool-green { background: linear-gradient(135deg, #34c759, #b4ec51); }
        &.tool-purple { background: linear-gradient(135deg, #af52de, #bf5af2); }
        &.tool-red { background: linear-gradient(135deg, #ff3b30, #ff5e3a); }
        &.tool-cyan { background: linear-gradient(135deg, #5ac8fa, #66d3fa); }
      }

      .tool-name {
        font-size: 12px;
        font-weight: 500;
        color: var(--text-primary);
        text-align: center;
      }

      &:hover .tool-icon {
        transform: translateY(-4px) rotate(4deg);
      }
    }
  }

  @media screen and (max-width: 768px) {
    .container { padding: 16px 16px 100px; }
    .page-header { padding: 8px 16px; height: 56px; .page-title { font-size: 24px; } }
    .featured-grid { grid-template-columns: 1fr; }
    .game-grid { grid-template-columns: 1fr; }
    .tools-grid { grid-template-columns: repeat(4, 1fr); }
  }
}
</style>
