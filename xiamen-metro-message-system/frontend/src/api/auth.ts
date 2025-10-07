import { request } from '@/utils/http'
import type { LoginForm, LoginResponse, User } from '@/types'

export const authApi = {
  // 登录
  login: (data: LoginForm) => {
    return request.post<LoginResponse>('/auth/login', data)
  },

  // 登出
  logout: () => {
    return request.post('/auth/logout')
  },

  // 获取用户信息
  getUserInfo: () => {
    return request.get<User>('/auth/user')
  },

  // 刷新Token
  refreshToken: (refreshToken: string) => {
    return request.post<{ token: string; refreshToken: string }>('/auth/refresh', {
      refreshToken
    })
  },

  // 修改密码
  changePassword: (data: { oldPassword: string; newPassword: string }) => {
    return request.post('/auth/change-password', data)
  },

  // 获取验证码
  getCaptcha: () => {
    return request.get<{ captchaId: string; captchaImage: string }>('/auth/captcha')
  }
}