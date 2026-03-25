<template>
  <!-- From Uiverse.io by ajxcj -->
  <div class="solar">
    <i class="mercury"></i>
    <i class="venus"></i>
    <i class="earth"></i>
    <i class="mars"></i>
    <i class="belt"></i>
    <i class="jupiter"></i>
    <i class="saturn"></i>
    <i class="uranus"></i>
    <i class="neptune"></i>
  </div>

</template>

<script setup lang="ts">
// 组件逻辑
</script>

<style scoped>
/* From Uiverse.io by ajxcj */
.solar {
  margin: 0 auto;
  height: 40px;
  width: 40px;
  background: orange;
  border-radius: 20px;
  position: relative;
  -webkit-animation: glow 1.5s linear infinite;
  -moz-animation: glow 1.5s linear infinite;
  transform: scale(0.7);
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .solar {
    transform: scale(0.45);
  }
}

.solar i {
  border-radius: 250px;
  display: block;
  position: absolute;
  border: 1px solid rgba(255, 255, 255, 0.15);
  box-sizing: border-box;
  -moz-box-sizing: border-box;
  -webkit-transform-origin: 50% 50%;
  -moz-transform-origin: 50% 50%;
}
.solar i:before {
  content: "";
  border-radius: 25px;
  background: black;
  display: block;
  position: absolute;
  top: -5px;
  right: 45%;
}
.solar i.mercury {
  width: 80px;
  height: 80px;
  margin-left: -40px;
  left: 50%;
  top: 50%;
  margin-top: -40px;
  -webkit-animation: orbit 0.5s linear infinite;
  -moz-animation: orbit 0.5s linear infinite;
}
.solar i.mercury:before {
  background: #6f5f5f;
  height: 7px;
  width: 7px;
}
.solar i.venus {
  width: 120px;
  height: 120px;
  margin-left: -60px;
  left: 50%;
  top: 50%;
  margin-top: -60px;
  -webkit-animation: orbit 1s linear infinite;
  -moz-animation: orbit 1s linear infinite;
}
.solar i.venus:before {
  background: #e7a71f;
  height: 10px;
  width: 10px;
}
.solar i.earth {
  width: 160px;
  height: 160px;
  margin-left: -80px;
  left: 50%;
  top: 50%;
  margin-top: -80px;
  -webkit-animation: orbit 1.5s linear infinite;
  -moz-animation: orbit 1.5s linear infinite;
}
.solar i.earth:before {
  background: #63bee2;
  height: 10px;
  width: 10px;
}
.solar i.mars {
  width: 200px;
  height: 200px;
  margin-left: -100px;
  left: 50%;
  top: 50%;
  margin-top: -100px;
  -webkit-animation: orbit 2s linear infinite;
  -moz-animation: orbit 2s linear infinite;
}
.solar i.mars:before {
  background: red;
  height: 10px;
  width: 10px;
}
.solar i.belt {
  box-sizing: border-box;
  -moz-box-sizing: border-box;
  border-width: 20px;
  width: 240px;
  height: 240px;
  margin-left: -120px;
  border-color: rgba(36, 35, 35, 0.21);
  left: 50%;
  top: 50%;
  margin-top: -120px;
}
.solar i.jupiter {
  width: 280px;
  height: 280px;
  margin-left: -140px;
  left: 50%;
  top: 50%;
  margin-top: -140px;
  -webkit-animation: orbit 2.5s linear infinite;
  -moz-animation: orbit 2.5s linear infinite;
}
.solar i.jupiter:before {
  background: #cf9b2b;
  top: -15px;
  height: 30px;
  width: 30px;
}
.solar i.saturn {
  width: 320px;
  height: 320px;
  margin-left: -160px;
  left: 50%;
  top: 50%;
  margin-top: -160px;
  -webkit-animation: orbit 3s linear infinite;
  -moz-animation: orbit 3s linear infinite;
}
.solar i.saturn:before {
  background: #cf7a2b;
  top: -10px;
  height: 20px;
  width: 20px;
}
.solar i.saturn:after {
  background: #fff;
  width: 30px;
  height: 1px;
  content: "";
  position: absolute;
  display: block;
  background: #fff;
  width: 30px;
  height: 1px;
  content: "";
  right: 43.5%;
  -webkit-transform: rotate(20deg);
  -moz-transform: rotate(20deg);
}
.solar i.uranus {
  width: 360px;
  height: 360px;
  margin-left: -180px;
  left: 50%;
  top: 50%;
  margin-top: -180px;
  -webkit-animation: orbit 3.5s linear infinite;
  -moz-animation: orbit 3.5s linear infinite;
}
.solar i.uranus:before {
  background: #10c593;
  top: -8px;
  height: 15px;
  width: 15px;
}
.solar i.neptune {
  width: 400px;
  height: 400px;
  margin-left: -200px;
  left: 50%;
  top: 50%;
  margin-top: -200px;
  -webkit-animation: orbit 4s linear infinite;
  -moz-animation: orbit 4s linear infinite;
}
.solar i.neptune:before {
  background: #1470e4;
  top: -8px;
  height: 15px;
  width: 15px;
}
@-webkit-keyframes orbit {
  0% {
    -webkit-transform: rotate(0deg);
  }
  50% {
    -webkit-transform: rotate(-180deg);
  }
  100% {
    -webkit-transform: rotate(-360deg);
  }
}
@-webkit-keyframes glow {
  0% {
    box-shadow: none;
  }
  50% {
    background: #ffeb00;
    box-shadow: 0 0 20px orange;
  }
  100% {
    box-shadow: none;
  }
}
@-moz-keyframes orbit {
  0% {
    -moz-transform: rotate(0deg);
  }
  50% {
    -moz-transform: rotate(-180deg);
  }
  100% {
    -moz-transform: rotate(-360deg);
  }
}
@-moz-keyframes glow {
  0% {
    box-shadow: none;
  }
  50% {
    background: #ffeb00;
    box-shadow: 0 0 20px orange;
  }
  100% {
    box-shadow: none;
  }
}

</style>
