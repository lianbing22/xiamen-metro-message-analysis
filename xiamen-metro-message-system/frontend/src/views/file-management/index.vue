<template>
  <div class="file-management">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1>文件管理</h1>
      <p>上传和管理设备报文文件</p>
    </div>

    <!-- 文件上传区域 -->
    <el-card class="upload-card">
      <template #header>
        <div class="card-header">
          <span>文件上传</span>
          <el-badge :value="uploadStatus.uploading" :hidden="uploadStatus.uploading === 0" type="primary">
            <el-button type="primary" :icon="Upload" :loading="uploading">
              {{ uploading ? '上传中...' : '选择文件' }}
            </el-button>
          </el-badge>
        </div>
      </template>

      <el-upload
        ref="uploadRef"
        :action="uploadUrl"
        :headers="uploadHeaders"
        :auto-upload="false"
        :show-file-list="true"
        :limit="10"
        :multiple="true"
        :accept="'.xlsx,.xls,.csv'"
        :on-change="handleFileChange"
        :on-remove="handleFileRemove"
        :on-exceed="handleExceed"
        :on-success="handleUploadSuccess"
        :on-error="handleUploadError"
        :before-upload="beforeUpload"
        :file-list="fileList"
        drag
      >
        <div class="upload-area">
          <el-icon class="upload-icon"><UploadFilled /></el-icon>
          <div class="upload-text">
            <p>将文件拖拽到此处，或点击选择文件</p>
            <p class="upload-hint">支持 .xlsx、.xls、.csv 格式，单个文件最大 100MB</p>
          </div>
        </div>
      </el-upload>

      <div class="upload-actions" v-if="fileList.length > 0">
        <el-button @click="clearFileList">清空列表</el-button>
        <el-button type="primary" @click="startUpload" :loading="uploading">
          开始上传 ({{ fileList.length }} 个文件)
        </el-button>
      </div>
    </el-card>

    <!-- 文件列表 -->
    <el-card class="file-list-card">
      <template #header>
        <div class="card-header">
          <span>文件列表</span>
          <div class="list-actions">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索文件名"
              prefix-icon="Search"
              style="width: 200px; margin-right: 10px"
              @input="handleSearch"
            />
            <el-select
              v-model="fileTypeFilter"
              placeholder="文件类型"
              style="width: 120px; margin-right: 10px"
              @change="handleFileTypeFilter"
            >
              <el-option label="全部" value="" />
              <el-option label="Excel" value="EXCEL" />
              <el-option label="CSV" value="CSV" />
            </el-select>
            <el-button @click="refreshFileList" :icon="Refresh">刷新</el-button>
          </div>
        </div>
      </template>

      <el-table
        v-loading="tableLoading"
        :data="tableData"
        style="width: 100%"
        stripe
      >
        <el-table-column prop="originalFileName" label="文件名" min-width="200">
          <template #default="{ row }">
            <div class="file-name">
              <el-icon><Document /></el-icon>
              <span>{{ row.originalFileName }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="fileType" label="类型" width="80">
          <template #default="{ row }">
            <el-tag :type="row.fileType === 'EXCEL' ? 'success' : 'info'">
              {{ row.fileType }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="formattedFileSize" label="大小" width="100" />

        <el-table-column prop="uploadStatus" label="上传状态" width="100">
          <template #default="{ row }">
            <el-tag
              :type="getUploadStatusType(row.uploadStatus)"
              size="small"
            >
              {{ getUploadStatusText(row.uploadStatus) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="processStatus" label="处理状态" width="100">
          <template #default="{ row }">
            <el-tag
              :type="getProcessStatusType(row.processStatus)"
              size="small"
            >
              {{ getProcessStatusText(row.processStatus) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="数据统计" width="150">
          <template #default="{ row }">
            <div v-if="row.dataRowCount">
              <div>总行数: {{ row.dataRowCount }}</div>
              <div class="stats-detail">
                有效: {{ row.validMessageCount || 0 }} /
                无效: {{ row.invalidMessageCount || 0 }}
              </div>
            </div>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>

        <el-table-column prop="createdAt" label="上传时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button
              size="small"
              type="primary"
              link
              :icon="Download"
              @click="downloadFile(row)"
              :disabled="row.uploadStatus !== 'COMPLETED'"
            >
              下载
            </el-button>
            <el-button
              size="small"
              type="info"
              link
              :icon="View"
              @click="viewFileDetail(row)"
            >
              详情
            </el-button>
            <el-button
              size="small"
              type="danger"
              link
              :icon="Delete"
              @click="deleteFile(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 文件详情对话框 -->
    <el-dialog
      v-model="detailDialog.visible"
      title="文件详情"
      width="600px"
    >
      <div v-if="detailDialog.data" class="file-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="文件名">{{ detailDialog.data.originalFileName }}</el-descriptions-item>
          <el-descriptions-item label="文件类型">{{ detailDialog.data.fileType }}</el-descriptions-item>
          <el-descriptions-item label="文件大小">{{ detailDialog.data.formattedFileSize }}</el-descriptions-item>
          <el-descriptions-item label="MIME类型">{{ detailDialog.data.mimeType || '-' }}</el-descriptions-item>
          <el-descriptions-item label="上传状态">
            <el-tag :type="getUploadStatusType(detailDialog.data.uploadStatus)">
              {{ getUploadStatusText(detailDialog.data.uploadStatus) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="处理状态">
            <el-tag :type="getProcessStatusType(detailDialog.data.processStatus)">
              {{ getProcessStatusText(detailDialog.data.processStatus) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="数据行数">{{ detailDialog.data.dataRowCount || '-' }}</el-descriptions-item>
          <el-descriptions-item label="有效报文">{{ detailDialog.data.validMessageCount || '-' }}</el-descriptions-item>
          <el-descriptions-item label="无效报文">{{ detailDialog.data.invalidMessageCount || '-' }}</el-descriptions-item>
          <el-descriptions-item label="上传时间">{{ formatDateTime(detailDialog.data.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ formatDateTime(detailDialog.data.updatedAt) }}</el-descriptions-item>
        </el-descriptions>

        <div v-if="detailDialog.data.errorMessage" class="error-message">
          <h4>错误信息:</h4>
          <p>{{ detailDialog.data.errorMessage }}</p>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Upload,
  UploadFilled,
  Document,
  Download,
  View,
  Delete,
  Search,
  Refresh
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { fileApi } from '@/api/file'
import type { FileItem, FileListResponse } from '@/types/file'
import dayjs from 'dayjs'

const userStore = useUserStore()

// 上传相关
const uploadRef = ref()
const uploadUrl = '/api/files/upload'
const uploadHeaders = ref({
  'X-User-Id': userStore.userInfo?.id || '1'
})
const uploading = ref(false)
const fileList = ref<FileItem[]>([])
const uploadStatus = reactive({
  uploading: 0,
  success: 0,
  failed: 0
})

// 文件列表相关
const tableLoading = ref(false)
const tableData = ref<FileItem[]>([])
const searchKeyword = ref('')
const fileTypeFilter = ref('')
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

// 详情对话框
const detailDialog = reactive({
  visible: false,
  data: null as FileItem | null
})

// 文件选择变化
const handleFileChange = (file: FileItem, fileList: FileItem[]) => {
  // 验证文件
  if (!validateFile(file)) {
    // 移除无效文件
    const index = fileList.findIndex(item => item.uid === file.uid)
    if (index > -1) {
      fileList.splice(index, 1)
    }
    return
  }
}

// 文件移除
const handleFileRemove = (file: FileItem, fileList: FileItem[]) => {
  // 更新状态
  updateUploadStatus()
}

// 文件数量超限
const handleExceed = () => {
  ElMessage.warning('最多只能同时上传 10 个文件')
}

// 上传前验证
const beforeUpload = (file: File) => {
  return validateFile(file as any)
}

// 验证文件
const validateFile = (file: FileItem) => {
  // 检查文件类型
  const allowedTypes = ['.xlsx', '.xls', '.csv']
  const fileName = file.name
  const fileExt = fileName.substring(fileName.lastIndexOf('.')).toLowerCase()

  if (!allowedTypes.includes(fileExt)) {
    ElMessage.error(`文件 ${fileName} 格式不支持，请上传 Excel 或 CSV 文件`)
    return false
  }

  // 检查文件大小 (100MB)
  const maxSize = 100 * 1024 * 1024
  if (file.size > maxSize) {
    ElMessage.error(`文件 ${fileName} 大小超过 100MB 限制`)
    return false
  }

  return true
}

// 开始上传
const startUpload = () => {
  if (fileList.value.length === 0) {
    ElMessage.warning('请选择要上传的文件')
    return
  }

  uploading.value = true
  uploadStatus.uploading = fileList.value.length

  // 使用 XMLHttpRequest 进行多文件上传
  const uploadPromises = fileList.value.map(file => {
    return new Promise((resolve, reject) => {
      const formData = new FormData()
      formData.append('file', file.raw)

      const xhr = new XMLHttpRequest()

      xhr.upload.addEventListener('progress', (e) => {
        if (e.lengthComputable) {
          const percent = Math.round((e.loaded / e.total) * 100)
          // 更新进度
          const index = fileList.value.findIndex(item => item.uid === file.uid)
          if (index > -1) {
            fileList.value[index].percentage = percent
          }
        }
      })

      xhr.addEventListener('load', () => {
        if (xhr.status === 200) {
          uploadStatus.success++
          resolve(xhr.response)
        } else {
          uploadStatus.failed++
          reject(new Error('上传失败'))
        }
      })

      xhr.addEventListener('error', () => {
        uploadStatus.failed++
        reject(new Error('上传失败'))
      })

      xhr.open('POST', uploadUrl)
      xhr.setRequestHeader('X-User-Id', uploadHeaders.value['X-User-Id'])
      xhr.send(formData)
    })
  })

  Promise.allSettled(uploadPromises).then(() => {
    uploading.value = false
    uploadStatus.uploading = 0
    ElMessage.success(`文件上传完成，成功: ${uploadStatus.success}，失败: ${uploadStatus.failed}`)
    clearFileList()
    refreshFileList()

    // 重置状态
    uploadStatus.success = 0
    uploadStatus.failed = 0
  })
}

// 清空文件列表
const clearFileList = () => {
  fileList.value = []
  uploadRef.value?.clearFiles()
  updateUploadStatus()
}

// 更新上传状态
const updateUploadStatus = () => {
  uploadStatus.uploading = fileList.value.filter(file =>
    file.status === 'uploading'
  ).length
}

// 上传成功
const handleUploadSuccess = (response: any, file: FileItem) => {
  ElMessage.success(`${file.name} 上传成功`)
}

// 上传失败
const handleUploadError = (error: any, file: FileItem) => {
  ElMessage.error(`${file.name} 上传失败`)
  console.error('Upload error:', error)
}

// 获取文件列表
const getFileList = async () => {
  try {
    tableLoading.value = true
    const params = {
      page: pagination.page,
      size: pagination.size,
      keyword: searchKeyword.value || undefined,
      fileType: fileTypeFilter.value || undefined
    }

    const response = await fileApi.getFileList(params)
    const data = response.data as FileListResponse

    tableData.value = data.content
    pagination.total = data.totalElements

  } catch (error) {
    console.error('获取文件列表失败:', error)
    ElMessage.error('获取文件列表失败')
  } finally {
    tableLoading.value = false
  }
}

// 搜索处理
const handleSearch = () => {
  pagination.page = 1
  getFileList()
}

// 文件类型筛选
const handleFileTypeFilter = () => {
  pagination.page = 1
  getFileList()
}

// 刷新文件列表
const refreshFileList = () => {
  getFileList()
}

// 分页大小变化
const handleSizeChange = (size: number) => {
  pagination.size = size
  pagination.page = 1
  getFileList()
}

// 页码变化
const handleCurrentChange = (page: number) => {
  pagination.page = page
  getFileList()
}

// 下载文件
const downloadFile = async (file: FileItem) => {
  try {
    await fileApi.downloadFile(file.id)
    ElMessage.success('文件下载开始')
  } catch (error) {
    console.error('下载文件失败:', error)
    ElMessage.error('下载文件失败')
  }
}

// 查看文件详情
const viewFileDetail = async (file: FileItem) => {
  try {
    const response = await fileApi.getFileDetail(file.id)
    detailDialog.data = response.data
    detailDialog.visible = true
  } catch (error) {
    console.error('获取文件详情失败:', error)
    ElMessage.error('获取文件详情失败')
  }
}

// 删除文件
const deleteFile = async (file: FileItem) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除文件 "${file.originalFileName}" 吗？`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await fileApi.deleteFile(file.id)
    ElMessage.success('文件删除成功')
    refreshFileList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除文件失败:', error)
      ElMessage.error('删除文件失败')
    }
  }
}

// 获取上传状态类型
const getUploadStatusType = (status: string) => {
  switch (status) {
    case 'COMPLETED': return 'success'
    case 'UPLOADING': return 'warning'
    case 'FAILED': return 'danger'
    default: return 'info'
  }
}

// 获取上传状态文本
const getUploadStatusText = (status: string) => {
  switch (status) {
    case 'PENDING': return '等待中'
    case 'UPLOADING': return '上传中'
    case 'COMPLETED': return '已完成'
    case 'FAILED': return '失败'
    default: return status
  }
}

// 获取处理状态类型
const getProcessStatusType = (status: string) => {
  switch (status) {
    case 'COMPLETED': return 'success'
    case 'PROCESSING': return 'warning'
    case 'FAILED': return 'danger'
    default: return 'info'
  }
}

// 获取处理状态文本
const getProcessStatusText = (status: string) => {
  switch (status) {
    case 'PENDING': return '等待中'
    case 'PROCESSING': return '处理中'
    case 'COMPLETED': return '已完成'
    case 'FAILED': return '失败'
    default: return status
  }
}

// 格式化日期时间
const formatDateTime = (dateTime: string) => {
  return dateTime ? dayjs(dateTime).format('YYYY-MM-DD HH:mm:ss') : '-'
}

// 初始化
onMounted(() => {
  getFileList()
})
</script>

<style scoped>
.file-management {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h1 {
  margin: 0 0 8px 0;
  font-size: 24px;
  color: #303133;
}

.page-header p {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.upload-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.upload-area {
  text-align: center;
  padding: 40px 20px;
}

.upload-icon {
  font-size: 48px;
  color: #409EFF;
  margin-bottom: 16px;
}

.upload-text p {
  margin: 8px 0;
}

.upload-hint {
  color: #909399;
  font-size: 12px;
}

.upload-actions {
  margin-top: 20px;
  text-align: center;
}

.upload-actions .el-button {
  margin: 0 8px;
}

.file-list-card {
  margin-bottom: 20px;
}

.list-actions {
  display: flex;
  align-items: center;
}

.file-name {
  display: flex;
  align-items: center;
  gap: 8px;
}

.stats-detail {
  font-size: 12px;
  color: #909399;
}

.text-muted {
  color: #909399;
}

.pagination-wrapper {
  margin-top: 20px;
  text-align: right;
}

.file-detail {
  margin-bottom: 20px;
}

.error-message {
  margin-top: 20px;
  padding: 16px;
  background-color: #FEF0F0;
  border-radius: 4px;
  color: #F56C6C;
}

.error-message h4 {
  margin: 0 0 8px 0;
}

.error-message p {
  margin: 0;
  word-break: break-all;
}
</style>