import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { ThemeConfig } from '@/types'

export const useAppStore = defineStore('app', () => {
  const theme = ref<ThemeConfig>({
    mode: 'light',
    primaryColor: '#409eff',
    layout: 'vertical',
    sidebarCollapsed: false
  })

  const loading = ref(false)
  const isOnline = ref(navigator.onLine)

  // 初始化应用
  const initializeApp = async () => {
    loading.value = true
    try {
      // 检查网络状态
      window.addEventListener('online', () => {
        isOnline.value = true
      })
      window.addEventListener('offline', () => {
        isOnline.value = false
      })

      // 加载主题配置
      loadThemeConfig()

      // 其他初始化逻辑...
    } catch (error) {
      console.error('Initialize app error:', error)
    } finally {
      loading.value = false
    }
  }

  // 加载主题配置
  const loadThemeConfig = () => {
    const storedTheme = localStorage.getItem('theme')
    if (storedTheme) {
      try {
        theme.value = JSON.parse(storedTheme)
        applyTheme(theme.value)
      } catch (error) {
        console.error('Parse theme error:', error)
      }
    }
  }

  // 切换主题
  const toggleTheme = () => {
    theme.value.mode = theme.value.mode === 'light' ? 'dark' : 'light'
    saveThemeConfig()
    applyTheme(theme.value)
  }

  // 设置主题配置
  const setTheme = (newTheme: Partial<ThemeConfig>) => {
    theme.value = { ...theme.value, ...newTheme }
    saveThemeConfig()
    applyTheme(theme.value)
  }

  // 保存主题配置
  const saveThemeConfig = () => {
    localStorage.setItem('theme', JSON.stringify(theme.value))
  }

  // 应用主题
  const applyTheme = (themeConfig: ThemeConfig) => {
    const root = document.documentElement
    if (themeConfig.mode === 'dark') {
      root.classList.add('dark')
    } else {
      root.classList.remove('dark')
    }
    root.style.setProperty('--el-color-primary', themeConfig.primaryColor)
  }

  // 设置侧边栏折叠状态
  const setSidebarCollapsed = (collapsed: boolean) => {
    theme.value.sidebarCollapsed = collapsed
    saveThemeConfig()
  }

  return {
    theme,
    loading,
    isOnline,
    initializeApp,
    toggleTheme,
    setTheme,
    setSidebarCollapsed
  }
})