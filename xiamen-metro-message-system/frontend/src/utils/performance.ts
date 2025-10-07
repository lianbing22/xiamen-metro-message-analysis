/**
 * 前端性能优化工具
 */

// 资源预加载
export function preloadResources(resources: string[]) {
  resources.forEach(url => {
    const link = document.createElement('link')
    link.rel = 'preload'
    link.href = url
    link.as = url.includes('.css') ? 'style' : 'script'
    document.head.appendChild(link)
  })
}

// 图片懒加载
export function lazyLoadImages() {
  if ('IntersectionObserver' in window) {
    const imageObserver = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          const img = entry.target as HTMLImageElement
          if (img.dataset.src) {
            img.src = img.dataset.src
            img.removeAttribute('data-src')
            imageObserver.unobserve(img)
          }
        }
      })
    })

    document.querySelectorAll('img[data-src]').forEach(img => {
      imageObserver.observe(img)
    })
  }
}

// 虚拟滚动
export class VirtualScroll {
  private container: HTMLElement
  private itemHeight: number
  private visibleCount: number
  private scrollTop = 0
  private totalItems: number
  private renderItem: (index: number) => HTMLElement
  private onScroll?: () => void

  constructor(options: {
    container: HTMLElement
    itemHeight: number
    totalItems: number
    renderItem: (index: number) => HTMLElement
    onScroll?: () => void
  }) {
    this.container = options.container
    this.itemHeight = options.itemHeight
    this.totalItems = options.totalItems
    this.renderItem = options.renderItem
    this.onScroll = options.onScroll
    this.visibleCount = Math.ceil(this.container.clientHeight / this.itemHeight) + 2
    this.init()
  }

  private init() {
    this.container.style.height = `${this.totalItems * this.itemHeight}px`
    this.container.style.overflow = 'auto'
    this.container.addEventListener('scroll', this.handleScroll.bind(this))
    this.render()
  }

  private handleScroll() {
    this.scrollTop = this.container.scrollTop
    this.render()
    this.onScroll?.()
  }

  private render() {
    const startIndex = Math.floor(this.scrollTop / this.itemHeight)
    const endIndex = Math.min(startIndex + this.visibleCount, this.totalItems)

    // 清除现有内容
    this.container.innerHTML = ''

    // 渲染可见项
    for (let i = startIndex; i < endIndex; i++) {
      const item = this.renderItem(i)
      item.style.position = 'absolute'
      item.style.top = `${i * this.itemHeight}px`
      item.style.width = '100%'
      this.container.appendChild(item)
    }
  }

  updateTotalItems(totalItems: number) {
    this.totalItems = totalItems
    this.container.style.height = `${this.totalItems * this.itemHeight}px`
    this.render()
  }

  destroy() {
    this.container.removeEventListener('scroll', this.handleScroll.bind(this))
  }
}

// 防抖函数
export function debounce<T extends (...args: any[]) => any>(
  func: T,
  wait: number
): (...args: Parameters<T>) => void {
  let timeout: NodeJS.Timeout
  return function executedFunction(...args: Parameters<T>) {
    const later = () => {
      clearTimeout(timeout)
      func(...args)
    }
    clearTimeout(timeout)
    timeout = setTimeout(later, wait)
  }
}

// 节流函数
export function throttle<T extends (...args: any[]) => any>(
  func: T,
  limit: number
): (...args: Parameters<T>) => void {
  let inThrottle: boolean
  return function executedFunction(...args: Parameters<T>) {
    if (!inThrottle) {
      func.apply(this, args)
      inThrottle = true
      setTimeout(() => inThrottle = false, limit)
    }
  }
}

// 性能监控
export class PerformanceMonitor {
  private static instance: PerformanceMonitor
  private metrics: Map<string, number[]> = new Map()

  static getInstance(): PerformanceMonitor {
    if (!PerformanceMonitor.instance) {
      PerformanceMonitor.instance = new PerformanceMonitor()
    }
    return PerformanceMonitor.instance
  }

  // 记录性能指标
  recordMetric(name: string, value: number) {
    if (!this.metrics.has(name)) {
      this.metrics.set(name, [])
    }
    this.metrics.get(name)!.push(value)

    // 保持最近100个数据点
    const values = this.metrics.get(name)!
    if (values.length > 100) {
      values.shift()
    }
  }

  // 获取性能指标统计
  getMetricStats(name: string) {
    const values = this.metrics.get(name)
    if (!values || values.length === 0) {
      return null
    }

    const sorted = [...values].sort((a, b) => a - b)
    return {
      count: values.length,
      min: sorted[0],
      max: sorted[sorted.length - 1],
      avg: values.reduce((sum, val) => sum + val, 0) / values.length,
      p50: sorted[Math.floor(sorted.length * 0.5)],
      p90: sorted[Math.floor(sorted.length * 0.9)],
      p95: sorted[Math.floor(sorted.length * 0.95)],
      p99: sorted[Math.floor(sorted.length * 0.99)]
    }
  }

  // 测量函数执行时间
  measureAsync<T>(name: string, fn: () => Promise<T>): Promise<T> {
    const startTime = performance.now()
    return fn().then(result => {
      const endTime = performance.now()
      this.recordMetric(name, endTime - startTime)
      return result
    })
  }

  // 测量同步函数执行时间
  measure<T>(name: string, fn: () => T): T {
    const startTime = performance.now()
    const result = fn()
    const endTime = performance.now()
    this.recordMetric(name, endTime - startTime)
    return result
  }
}

// 请求缓存
export class RequestCache {
  private cache = new Map<string, { data: any; timestamp: number; ttl: number }>()

  set(key: string, data: any, ttl = 300000) { // 默认5分钟
    this.cache.set(key, {
      data,
      timestamp: Date.now(),
      ttl
    })
  }

  get(key: string) {
    const item = this.cache.get(key)
    if (!item) return null

    if (Date.now() - item.timestamp > item.ttl) {
      this.cache.delete(key)
      return null
    }

    return item.data
  }

  clear() {
    this.cache.clear()
  }

  delete(key: string) {
    this.cache.delete(key)
  }

  // 清理过期缓存
  cleanup() {
    const now = Date.now()
    for (const [key, item] of this.cache.entries()) {
      if (now - item.timestamp > item.ttl) {
        this.cache.delete(key)
      }
    }
  }
}

// 图片压缩
export function compressImage(file: File, quality = 0.8): Promise<Blob> {
  return new Promise((resolve) => {
    const canvas = document.createElement('canvas')
    const ctx = canvas.getContext('2d')!
    const img = new Image()

    img.onload = () => {
      // 计算压缩尺寸
      const maxWidth = 1920
      const maxHeight = 1080
      let { width, height } = img

      if (width > maxWidth || height > maxHeight) {
        const ratio = Math.min(maxWidth / width, maxHeight / height)
        width *= ratio
        height *= ratio
      }

      canvas.width = width
      canvas.height = height

      // 绘制压缩后的图片
      ctx.drawImage(img, 0, 0, width, height)
      canvas.toBlob(resolve, 'image/jpeg', quality)
    }

    img.src = URL.createObjectURL(file)
  })
}

// 批量请求
export class BatchRequest {
  private queue: Array<{ resolve: Function; reject: Function; params: any }> = []
  private timer: NodeJS.Timeout | null = null
  private requestFn: (params: any[]) => Promise<any[]>

  constructor(requestFn: (params: any[]) => Promise<any[]>, delay = 10) {
    this.requestFn = requestFn

    this.timer = setInterval(() => {
      if (this.queue.length > 0) {
        this.flush()
      }
    }, delay)
  }

  add(params: any): Promise<any> {
    return new Promise((resolve, reject) => {
      this.queue.push({ resolve, reject, params })
    })
  }

  private async flush() {
    if (this.queue.length === 0) return

    const batch = this.queue.splice(0, 50) // 限制批量大小
    const paramsList = batch.map(item => item.params)

    try {
      const results = await this.requestFn(paramsList)
      batch.forEach((item, index) => {
        item.resolve(results[index])
      })
    } catch (error) {
      batch.forEach(item => {
        item.reject(error)
      })
    }
  }

  destroy() {
    if (this.timer) {
      clearInterval(this.timer)
      this.timer = null
    }
    this.queue = []
  }
}

// 页面性能指标
export function getPagePerformanceMetrics() {
  if (!window.performance) return null

  const navigation = performance.getEntriesByType('navigation')[0] as PerformanceNavigationTiming

  return {
    // DNS查询时间
    dnsLookup: navigation.domainLookupEnd - navigation.domainLookupStart,
    // TCP连接时间
    tcpConnect: navigation.connectEnd - navigation.connectStart,
    // 请求响应时间
    request: navigation.responseEnd - navigation.requestStart,
    // DOM解析时间
    domParse: navigation.domContentLoadedEventEnd - navigation.domLoading,
    // 页面加载完成时间
    pageLoad: navigation.loadEventEnd - navigation.loadEventStart,
    // 首次渲染时间
    firstPaint: performance.getEntriesByType('paint')[0]?.startTime,
    // 首次内容绘制时间
    firstContentfulPaint: performance.getEntriesByType('paint')[1]?.startTime,
    // 总加载时间
    totalTime: navigation.loadEventEnd - navigation.startTime
  }
}

// 监控长任务
export function observeLongTasks(callback: (tasks: any[]) => void) {
  if ('PerformanceObserver' in window) {
    const observer = new PerformanceObserver((list) => {
      const entries = list.getEntries()
      callback(entries)
    })

    observer.observe({ entryTypes: ['longtask'] })
    return observer
  }
  return null
}

// 内存使用情况
export function getMemoryUsage() {
  if ('memory' in performance) {
    const memory = (performance as any).memory
    return {
      usedJSHeapSize: memory.usedJSHeapSize,
      totalJSHeapSize: memory.totalJSHeapSize,
      jsHeapSizeLimit: memory.jsHeapSizeLimit,
      usagePercent: (memory.usedJSHeapSize / memory.jsHeapSizeLimit * 100).toFixed(2)
    }
  }
  return null
}