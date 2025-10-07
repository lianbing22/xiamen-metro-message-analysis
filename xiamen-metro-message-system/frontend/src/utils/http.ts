import axios from 'axios'
import type { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'

// 创建axios实例
const http: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
http.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()

    // 添加认证token
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }

    // 添加请求时间戳
    config.headers['X-Request-Time'] = Date.now().toString()

    return config
  },
  (error) => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
http.interceptors.response.use(
  (response: AxiosResponse) => {
    const { data, code, message } = response.data

    // 请求成功
    if (code === 200 || response.status === 200) {
      return {
        ...response,
        data: data || response.data
      }
    }

    // 业务错误
    ElMessage.error(message || '请求失败')
    return Promise.reject(new Error(message || '请求失败'))
  },
  async (error) => {
    const { response } = error

    if (response) {
      const { status, data } = response

      switch (status) {
        case 401:
          // Token过期或无效
          const userStore = useUserStore()
          await ElMessageBox.confirm('登录状态已过期，请重新登录', '提示', {
            confirmButtonText: '重新登录',
            cancelButtonText: '取消',
            type: 'warning'
          })
          userStore.logout()
          window.location.href = '/login'
          break

        case 403:
          ElMessage.error('没有权限访问该资源')
          break

        case 404:
          ElMessage.error('请求的资源不存在')
          break

        case 500:
          ElMessage.error('服务器内部错误')
          break

        default:
          ElMessage.error(data?.message || `请求失败 (${status})`)
      }
    } else if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请检查网络连接')
    } else {
      ElMessage.error('网络错误，请检查网络连接')
    }

    return Promise.reject(error)
  }
)

// 封装常用请求方法
export const request = {
  get: <T = any>(url: string, params?: any, config?: AxiosRequestConfig) => {
    return http.get<T>(url, { ...config, params })
  },

  post: <T = any>(url: string, data?: any, config?: AxiosRequestConfig) => {
    return http.post<T>(url, data, config)
  },

  put: <T = any>(url: string, data?: any, config?: AxiosRequestConfig) => {
    return http.put<T>(url, data, config)
  },

  delete: <T = any>(url: string, config?: AxiosRequestConfig) => {
    return http.delete<T>(url, config)
  },

  patch: <T = any>(url: string, data?: any, config?: AxiosRequestConfig) => {
    return http.patch<T>(url, data, config)
  }
}

export { http }
export default http