<template>
<!--  <FontLoader />-->
  <div id="app">
    <a-config-provider :locale="zhCN">
      <BasicLayout />
    </a-config-provider>
  </div>
</template>

<script setup lang="ts">
import BasicLayout from '@/layouts/BasicLayout.vue'
import zhCN from 'ant-design-vue/es/locale/zh_CN';
import dayjs from 'dayjs';
import 'dayjs/locale/zh-cn';
// import FontLoader from '@/components/FontLoader.vue'

dayjs.locale('zh-cn');
</script>

<style>
#app {
  width: 100%;
  height: 100%;
}
</style>
