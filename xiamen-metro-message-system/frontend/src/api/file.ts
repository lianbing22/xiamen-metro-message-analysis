import request from './request'
import type { ApiResponse, FileEntity, FileListResponse } from '@/types/file'

/**
 * 文件管理API接口
 */

// 上传文件
export const uploadFile = (file: File, userId?: string): Promise<ApiResponse<FileEntity>> => {
  const formData = new FormData()
  formData.append('file', file)

  return request.post('/api/files/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
      'X-User-Id': userId || '1'
    }
  })
}

// 获取文件列表
export const getFileList = (params: {
  page?: number
  size?: number
  keyword?: string
  fileType?: string
}): Promise<ApiResponse<FileListResponse>> => {
  return request.get('/api/files', { params })
}

// 获取文件详情
export const getFileDetail = (id: number): Promise<ApiResponse<FileEntity>> => {
  return request.get(`/api/files/${id}`)
}

// 下载文件
export const downloadFile = (id: number): Promise<void> => {
  return request.get(`/api/files/${id}/download`, {
    responseType: 'blob'
  }).then(response => {
    // 创建下载链接
    const blob = new Blob([response.data])
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url

    // 从响应头获取文件名
    const contentDisposition = response.headers['content-disposition']
    let filename = 'download'
    if (contentDisposition) {
      const filenameMatch = contentDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/)
      if (filenameMatch && filenameMatch[1]) {
        filename = decodeURIComponent(filenameMatch[1].replace(/['"]/g, ''))
      }
    }

    link.setAttribute('download', filename)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
  })
}

// 删除文件
export const deleteFile = (id: number): Promise<ApiResponse<null>> => {
  return request.delete(`/api/files/${id}`)
}

// 批量删除文件
export const batchDeleteFiles = (ids: number[]): Promise<ApiResponse<null>> => {
  return request.delete('/api/files/batch', { data: ids })
}