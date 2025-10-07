import jsPDF from 'jspdf'
import html2canvas from 'html2canvas'
import * as XLSX from 'xlsx'
import { saveAs } from 'file-saver'
import dayjs from 'dayjs'

// 导出PDF
export const exportToPDF = async (
  element: HTMLElement,
  filename: string,
  options: {
    scale?: number
    quality?: number
    backgroundColor?: string
  } = {}
): Promise<void> => {
  try {
    const {
      scale = 2,
      quality = 0.95,
      backgroundColor = '#ffffff'
    } = options

    // 生成canvas
    const canvas = await html2canvas(element, {
      scale,
      useCORS: true,
      allowTaint: true,
      backgroundColor,
      logging: false
    })

    // 获取图片数据
    const imgData = canvas.toDataURL('image/png', quality)

    // 计算PDF尺寸
    const imgWidth = canvas.width
    const imgHeight = canvas.height
    const pdfWidth = imgWidth > imgHeight ? 297 : 210 // A4尺寸，根据图片宽高决定方向
    const pdfHeight = (imgHeight / imgWidth) * pdfWidth

    // 创建PDF
    const pdf = new jsPDF({
      orientation: imgWidth > imgHeight ? 'landscape' : 'portrait',
      unit: 'mm',
      format: 'a4'
    })

    // 添加图片到PDF
    pdf.addImage(imgData, 'PNG', 0, 0, pdfWidth, pdfHeight)

    // 保存PDF
    pdf.save(`${filename}.pdf`)
  } catch (error) {
    console.error('导出PDF失败:', error)
    throw new Error('导出PDF失败')
  }
}

// 导出Excel
export const exportToExcel = (
  data: any[],
  filename: string,
  options: {
    sheetName?: string
    headers?: Record<string, string>
    dateFormat?: string
  } = {}
): void => {
  try {
    const {
      sheetName = 'Sheet1',
      headers = {},
      dateFormat = 'YYYY-MM-DD HH:mm:ss'
    } = options

    // 准备数据
    const processedData = data.map(item => {
      const processedItem: any = {}

      // 处理每个字段
      Object.keys(item).forEach(key => {
        let value = item[key]

        // 处理日期
        if (value && (typeof value === 'string' || typeof value === 'number')) {
          const date = new Date(value)
          if (!isNaN(date.getTime())) {
            value = dayjs(date).format(dateFormat)
          }
        }

        // 处理对象
        if (typeof value === 'object' && value !== null) {
          value = JSON.stringify(value)
        }

        processedItem[headers[key] || key] = value
      })

      return processedItem
    })

    // 创建工作簿
    const wb = XLSX.utils.book_new()
    const ws = XLSX.utils.json_to_sheet(processedData)

    // 设置列宽
    const colWidths = Object.keys(processedData[0] || {}).map(() => ({ wch: 15 }))
    ws['!cols'] = colWidths

    // 添加工作表到工作簿
    XLSX.utils.book_append_sheet(wb, ws, sheetName)

    // 生成Excel文件并保存
    const excelBuffer = XLSX.write(wb, { bookType: 'xlsx', type: 'array' })
    const blob = new Blob([excelBuffer], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    })

    saveAs(blob, `${filename}.xlsx`)
  } catch (error) {
    console.error('导出Excel失败:', error)
    throw new Error('导出Excel失败')
  }
}

// 导出CSV
export const exportToCSV = (
  data: any[],
  filename: string,
  options: {
    headers?: Record<string, string>
    separator?: string
    dateFormat?: string
  } = {}
): void => {
  try {
    const {
      headers = {},
      separator = ',',
      dateFormat = 'YYYY-MM-DD HH:mm:ss'
    } = options

    if (data.length === 0) {
      throw new Error('没有数据可导出')
    }

    // 准备表头
    const firstItem = data[0]
    const csvHeaders = Object.keys(firstItem).map(key => headers[key] || key)

    // 准备数据行
    const csvData = data.map(item => {
      return Object.keys(firstItem).map(key => {
        let value = item[key]

        // 处理日期
        if (value && (typeof value === 'string' || typeof value === 'number')) {
          const date = new Date(value)
          if (!isNaN(date.getTime())) {
            value = dayjs(date).format(dateFormat)
          }
        }

        // 处理包含分隔符或换行符的值
        if (typeof value === 'string' && (value.includes(separator) || value.includes('\n'))) {
          value = `"${value.replace(/"/g, '""')}"`
        }

        return value || ''
      })
    })

    // 构建CSV内容
    const csvContent = [
      csvHeaders.join(separator),
      ...csvData.map(row => row.join(separator))
    ].join('\n')

    // 添加BOM以支持中文
    const BOM = '\uFEFF'
    const blob = new Blob([BOM + csvContent], {
      type: 'text/csv;charset=utf-8'
    })

    saveAs(blob, `${filename}.csv`)
  } catch (error) {
    console.error('导出CSV失败:', error)
    throw new Error('导出CSV失败')
  }
}

// 导出图表为图片
export const exportChartToImage = (
  chartElement: HTMLElement,
  filename: string,
  format: 'png' | 'jpeg' = 'png',
  options: {
    scale?: number
    quality?: number
    backgroundColor?: string
  } = {}
): Promise<void> => {
  return new Promise(async (resolve, reject) => {
    try {
      const {
        scale = 2,
        quality = 0.95,
        backgroundColor = '#ffffff'
      } = options

      const canvas = await html2canvas(chartElement, {
        scale,
        useCORS: true,
        allowTaint: true,
        backgroundColor,
        logging: false
      })

      canvas.toBlob((blob) => {
        if (blob) {
          saveAs(blob, `${filename}.${format}`)
          resolve()
        } else {
          reject(new Error('生成图片失败'))
        }
      }, `image/${format}`, quality)
    } catch (error) {
      reject(error)
    }
  })
}

// 导出仪表板完整报告
export const exportDashboardReport = async (
  dashboardElement: HTMLElement,
  data: {
    title: string
    stats: any
    charts: any[]
    tables: any[]
  },
  filename: string
): Promise<void> => {
  try {
    // 创建临时容器
    const tempContainer = document.createElement('div')
    tempContainer.style.cssText = `
      position: fixed;
      top: -9999px;
      left: -9999px;
      width: 1200px;
      background: white;
      padding: 40px;
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
    `

    // 构建报告HTML
    const reportHTML = `
      <div style="margin-bottom: 40px; text-align: center;">
        <h1 style="color: #333; margin-bottom: 10px;">${data.title}</h1>
        <p style="color: #666; margin: 0;">生成时间: ${dayjs().format('YYYY-MM-DD HH:mm:ss')}</p>
      </div>

      ${data.stats ? `
      <div style="margin-bottom: 40px;">
        <h2 style="color: #333; border-bottom: 2px solid #1890ff; padding-bottom: 10px;">统计数据</h2>
        <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; margin-top: 20px;">
          ${Object.entries(data.stats).map(([key, value]) => `
            <div style="background: #f5f5f5; padding: 15px; border-radius: 8px;">
              <div style="color: #666; font-size: 14px; margin-bottom: 5px;">${key}</div>
              <div style="color: #333; font-size: 24px; font-weight: bold;">${value}</div>
            </div>
          `).join('')}
        </div>
      </div>
      ` : ''}

      ${data.charts && data.charts.length > 0 ? `
      <div style="margin-bottom: 40px;">
        <h2 style="color: #333; border-bottom: 2px solid #1890ff; padding-bottom: 10px;">图表分析</h2>
        <div style="margin-top: 20px;">
          ${data.charts.map(chart => `
            <div style="margin-bottom: 30px;">
              <h3 style="color: #333; margin-bottom: 15px;">${chart.title}</h3>
              <div style="background: #fafafa; padding: 20px; border-radius: 8px; text-align: center;">
                ${chart.description || ''}
              </div>
            </div>
          `).join('')}
        </div>
      </div>
      ` : ''}

      ${data.tables && data.tables.length > 0 ? `
      <div>
        <h2 style="color: #333; border-bottom: 2px solid #1890ff; padding-bottom: 10px;">详细数据</h2>
        <div style="margin-top: 20px;">
          ${data.tables.map(table => `
            <div style="margin-bottom: 30px;">
              <h3 style="color: #333; margin-bottom: 15px;">${table.title}</h3>
              <div style="color: #666; font-size: 14px; line-height: 1.6;">
                ${table.description || ''}
              </div>
            </div>
          `).join('')}
        </div>
      </div>
      ` : ''}
    `

    tempContainer.innerHTML = reportHTML
    document.body.appendChild(tempContainer)

    // 导出为PDF
    await exportToPDF(tempContainer, filename)

    // 清理临时容器
    document.body.removeChild(tempContainer)
  } catch (error) {
    console.error('导出仪表板报告失败:', error)
    throw new Error('导出仪表板报告失败')
  }
}