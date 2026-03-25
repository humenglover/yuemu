<template>
  <div class="empty-state-wrapper">
    <div class="empty-state-container">
      <div class="loading">
        <!-- 【--x是自定义属性，在后面样式中会用到】 -->
        <div style="--x:0"></div>
        <div style="--x:1"></div>
        <div style="--x:2"></div>
        <div style="--x:3"></div>
        <div style="--x:4"></div>
        <div style="--x:5"></div>
      </div>
      <h3 v-if="title" class="empty-title">{{ title }}</h3>
      <p v-if="description" class="empty-description">{{ description }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps({
  title: {
    type: String,
    default: '暂无数据'
  },
  description: {
    type: String,
    default: ''
  }
})
</script>

<style lang="scss" scoped>
*{
  /* 初始化 取消页面内外边距 */
  margin: 0;
  padding: 0;
  /* 这个是告诉浏览器：你想要设置的边框喝内边距的值是包含在总宽高内的 */
  box-sizing: border-box;
}
body{
  /* 100%窗口高度 */
  height: 100vh;
  /* 弹性布局 水平、垂直居中 */
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #eef1f5;
}
.loading{
  display: flex;
  /* 水平显示 */
  flex-direction: row;
}
.loading div{
  position: relative;
  width: 40px;
  height: 200px;
  /* 渐变背景 */
  background: linear-gradient(to bottom,rgba(0,0,0,0.05),#eef1f5);
  margin: 20px;
  border-radius: 20px;
  border: 2px solid #eef1f5;
  /* 阴影 */
  box-shadow: 15px 15px 20px rgba(0,0,0,0.1),
  -15px -15px 20px #fff,
  inset -5px -5px 5px rgba(255,255,255,0.5),
  inset 5px 5px 5px rgba(0,0,0,0.05);
  /* 溢出隐藏 */
  overflow: hidden;
}
.loading div::before{
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  box-shadow: inset -5px -5px 5px rgba(0,0,0,0.1),
  0 420px 0 400px lightskyblue;
  /* 执行动画：动画名称 时长 加速后减速 无限次播放 */
  animation: animate 2s ease-in-out infinite;
  /* 动画延迟 通过var函数获取自定义属性--x，再由calc函数计算得出每个元素的动画延迟时间 */
  animation-delay: calc(var(--x) * -0.3s);
  /* 初始化时先向上移动160px */
  transform: translateY(160px);
}

/* 定义动画 */
@keyframes animate{
  0%{
    transform: translateY(160px);
    /* hue-rotate是颜色滤镜，可以设置不同的度数来改变颜色 */
    filter: hue-rotate(0deg);
  }
  50%{
    transform: translateY(0px);
    filter: hue-rotate(180deg);
  }
  100%{
    transform: translateY(160px);
    filter: hue-rotate(360deg);
  }
}
</style>
