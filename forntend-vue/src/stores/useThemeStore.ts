import { defineStore } from 'pinia'
import { ref, nextTick } from 'vue'

export const useThemeStore = defineStore('theme', () => {
  const isDarkTheme = ref(false)
  const isAnimating = ref(false)

  // 切换主题
  const toggleTheme = () => {
    if (isAnimating.value) return
    isAnimating.value = true

    const isDark = !isDarkTheme.value

    // 将底层 DOM 实际的模式变掉的复用函数
    const performThemeChange = () => {
      isDarkTheme.value = isDark
      document.documentElement.classList.toggle('dark-theme', isDark)
      document.body.classList.remove('dark-theme', 'light-theme')
      localStorage.setItem('theme', isDark ? 'dark' : 'light')
      window.dispatchEvent(new Event('themeChange'))
    }

    // 万无一失的替代版 视觉补间方案：
    // 创建一个临时的 DOM 元素遮挡屏幕以防锁定，视觉效果上与 ViewTransition 并无二致
    const overlay = document.createElement('div')
    overlay.style.cssText = `
      position: fixed;
      top: 0; left: 0; right: 0; bottom: 0;
      z-index: 2147483647;
      pointer-events: none;
      background-color: ${isDark ? '#1a1a1a' : '#ffffff'};
      clip-path: circle(0px at top right);
      transition: clip-path 0.5s ease-in-out;
    `
    document.body.appendChild(overlay)

    // 让浏览器读取一下强制回流触发初始 0px 的判定
    // eslint-disable-next-line no-void
    void overlay.offsetWidth
    overlay.style.clipPath = 'circle(150vmax at top right)'

    // 当前遮罩动画完全铺满全屏幕时（0.5s后差不多满屏）
    setTimeout(() => {
      // 底层真界面静默切换掉主题颜色
      performThemeChange()

      // 然后将这个临时遮罩层平滑淡出，露出已经更新好颜色的真实界面
      overlay.style.transition = 'opacity 0.3s ease'
      overlay.style.opacity = '0'

      setTimeout(() => {
        if (overlay.parentNode) overlay.parentNode.removeChild(overlay)
        isAnimating.value = false
      }, 300)
    }, 450)
  }

  // 初始化主题
  const initTheme = () => {
    const savedTheme = localStorage.getItem('theme')
    if (savedTheme === 'dark') {
      isDarkTheme.value = true
      document.documentElement.classList.add('dark-theme')
    }
    // 初始清理 body 污染
    document.body.classList.remove('dark-theme', 'light-theme')
  }

  return {
    isDarkTheme,
    isAnimating,
    toggleTheme,
    initTheme
  }
})
