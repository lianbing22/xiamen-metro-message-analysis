import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { User, LoginForm, LoginResponse } from '@/types'
import { authApi } from '@/api/auth'
import { ElMessage } from 'element-plus'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const refreshToken = ref<string>(localStorage.getItem('refreshToken') || '')
  const user = ref<User | null>(null)
  const permissions = ref<string[]>([])

  const isLoggedIn = computed(() => !!token.value)
  const userRole = computed(() => user.value?.role || 'USER')
  const userName = computed(() => user.value?.fullName || user.value?.username || '')

  // 登录
  const login = async (loginForm: LoginForm): Promise<boolean> => {
    try {
      const response = await authApi.login(loginForm)
      const { token: newToken, refreshToken: newRefreshToken, user: userInfo } = response.data

      token.value = newToken
      refreshToken.value = newRefreshToken
      user.value = userInfo

      localStorage.setItem('token', newToken)
      localStorage.setItem('refreshToken', newRefreshToken)
      localStorage.setItem('user', JSON.stringify(userInfo))

      ElMessage.success('登录成功')
      return true
    } catch (error: any) {
      ElMessage.error(error.message || '登录失败')
      return false
    }
  }

  // 登出
  const logout = async () => {
    try {
      await authApi.logout()
    } catch (error) {
      console.error('Logout error:', error)
    } finally {
      token.value = ''
      refreshToken.value = ''
      user.value = null
      permissions.value = []

      localStorage.removeItem('token')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('user')

      ElMessage.success('已退出登录')
    }
  }

  // 获取用户信息
  const getUserInfo = async () => {
    try {
      const response = await authApi.getUserInfo()
      user.value = response.data
      localStorage.setItem('user', JSON.stringify(response.data))
    } catch (error) {
      console.error('Get user info error:', error)
      await logout()
    }
  }

  // 刷新Token
  const refreshAccessToken = async (): Promise<boolean> => {
    try {
      const response = await authApi.refreshToken(refreshToken.value)
      const { token: newToken, refreshToken: newRefreshToken } = response.data

      token.value = newToken
      refreshToken.value = newRefreshToken

      localStorage.setItem('token', newToken)
      localStorage.setItem('refreshToken', newRefreshToken)

      return true
    } catch (error) {
      await logout()
      return false
    }
  }

  // 检查权限
  const hasPermission = (permission: string): boolean => {
    return permissions.value.includes(permission) || userRole.value === 'ADMIN'
  }

  // 检查角色
  const hasRole = (role: string | string[]): boolean => {
    if (userRole.value === 'ADMIN') return true
    if (typeof role === 'string') {
      return userRole.value === role
    }
    return role.includes(userRole.value)
  }

  // 初始化用户状态
  const initUserState = () => {
    const storedUser = localStorage.getItem('user')
    if (storedUser && token.value) {
      try {
        user.value = JSON.parse(storedUser)
      } catch (error) {
        console.error('Parse user error:', error)
        logout()
      }
    }
  }

  return {
    token,
    refreshToken,
    user,
    permissions,
    isLoggedIn,
    userRole,
    userName,
    login,
    logout,
    getUserInfo,
    refreshAccessToken,
    hasPermission,
    hasRole,
    initUserState
  }
})