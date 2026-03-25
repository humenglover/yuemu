import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useLayoutStore = defineStore('layout', () => {
    // 侧边栏位置：'left' | 'right'
    const siderSide = ref<'left' | 'right'>((localStorage.getItem('sider-side') as 'left' | 'right') || 'left')

    const setSiderSide = (side: 'left' | 'right') => {
        siderSide.value = side
        localStorage.setItem('sider-side', side)
    }

    return {
        siderSide,
        setSiderSide,
    }
})
