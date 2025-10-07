/**
 * 文件管理相关类型定义
 */

// 文件类型
export enum FileType {
  EXCEL = 'EXCEL',
  CSV = 'CSV'
}

// 上传状态
export enum UploadStatus {
  PENDING = 'PENDING',
  UPLOADING = 'UPLOADING',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED'
}

// 处理状态
export enum ProcessStatus {
  PENDING = 'PENDING',
  PROCESSING = 'PROCESSING',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED'
}

// 文件项接口
export interface FileItem {
  uid: string
  name: string
  url?: string
  status: 'ready' | 'uploading' | 'success' | 'fail'
  raw?: File
  percentage?: number
  response?: any
}

// 文件实体接口
export interface FileEntity {
  id: number
  fileName: string
  originalFileName: string
  fileExtension: string
  fileSize: number
  fileType: FileType
  mimeType?: string
  storagePath?: string
  fileHash?: string
  uploadStatus: UploadStatus
  processStatus: ProcessStatus
  errorMessage?: string
  dataRowCount?: number
  validMessageCount?: number
  invalidMessageCount?: number
  uploadedBy?: number
  createdAt: string
  updatedAt?: string
}

// 文件列表项接口
export interface FileListItem {
  id: number
  fileName: string
  originalFileName: string
  fileType: FileType
  fileSize: number
  formattedFileSize: string
  uploadStatus: UploadStatus
  processStatus: ProcessStatus
  dataRowCount?: number
  validMessageCount?: number
  invalidMessageCount?: number
  errorMessage?: string
  uploadedBy?: string
  createdAt: string
  updatedAt?: string
}

// 分页响应接口
export interface FileListResponse {
  content: FileListItem[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  first: boolean
  last: boolean
  empty: boolean
}

// API响应接口
export interface ApiResponse<T = any> {
  success: boolean
  message: string
  data: T
  timestamp: number
}