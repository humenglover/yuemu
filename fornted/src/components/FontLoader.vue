<template>
  <div style="display: none;"></div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'

// 检查字体是否已加载
const loadCustomFonts = () => {
  // 如果已经加载过字体，直接返回
  if (document.body.classList.contains('fonts-loaded')) {
    return
  }

  // 使用 Promise.all 同时加载多个字体
  Promise.all([
    // 加载普通字重
    new FontFace(
      'TsangerYuYangT',
      `url(${new URL('@/assets/fonts/freeFonts.ttf', import.meta.url).href})`,
      {
        weight: 'normal',
        display: 'optional',
        unicodeRange: 'U+4E00-9FFF, U+3400-4DBF'
      }
    ).load()
  ])
    .then(fonts => {
      // 将字体添加到 FontFaceSet
      fonts.forEach(font => document.fonts.add(font))
      // 等待所有字体加载完成
      return document.fonts.ready
    })
    .then(() => {
      // 添加标记类，触发字体切换
      document.body.classList.add('fonts-loaded')
      console.log('Custom fonts loaded successfully')
    })
    .catch(err => {
      console.warn('Failed to load custom fonts, falling back to system fonts:', err)
    })
}

onMounted(() => {
  // 使用 requestIdleCallback 在浏览器空闲时加载字体
  if ('requestIdleCallback' in window) {
    (window as any).requestIdleCallback(() => {
      loadCustomFonts()
    }, { timeout: 3000 })
  } else {
    // 如果浏览器不支持 requestIdleCallback，延迟加载字体
    setTimeout(loadCustomFonts, 1000)
  }
})
</script>
