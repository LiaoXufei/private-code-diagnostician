import axios from 'axios'
import { message } from 'ant-design-vue'

// 创建axios实例
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: import.meta.env.VITE_REQUEST_TIMEOUT || 30000,
  headers: {
    'Content-Type': 'application/json'
  },
  // 允许跨域携带cookie
  withCredentials: false
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    // 在发送请求之前做些什么
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    // 添加请求ID用于追踪
    config.headers['X-Request-ID'] = generateRequestId()

    // 开发环境打印请求信息
    if (import.meta.env.DEV) {
      console.log(`[Request] ${config.method?.toUpperCase()} ${config.url}`, {
        params: config.params,
        data: config.data
      })
    }

    return config
  },
  error => {
    // 对请求错误做些什么
    console.error('Request interceptor error:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    // 对响应数据做点什么
    const { data, config } = response

    // 开发环境打印响应信息
    if (import.meta.env.DEV) {
      console.log(`[Response] ${config.method?.toUpperCase()} ${config.url}`, {
        status: response.status,
        data: data
      })
    }

    // 处理文件下载
    if (response.config.responseType === 'blob') {
      return response
    }

    // 统一处理响应数据结构
    if (data && typeof data === 'object') {
      // 如果响应包含code字段，进行状态码处理
      if (data.code !== undefined) {
        if (data.code === 200 || data.code === 0) {
          return {
            ...response,
            data: data.data || data
          }
        } else {
          // 业务错误处理
          const errorMsg = data.message || data.msg || '请求失败'
          message.error(errorMsg)
          return Promise.reject(new Error(errorMsg))
        }
      }

      // 如果响应结构已经是正确格式，直接返回
      return response
    }

    return response
  },
  error => {
    // 对响应错误做点什么
    const { response, message: errorMessage } = error

    if (response) {
      const { status, data } = response

      // 根据状态码处理错误
      switch (status) {
        case 400:
          message.error(data?.message || '请求参数错误')
          break
        case 401:
          message.error('未授权，请重新登录')
          // 清除token并跳转到登录页
          localStorage.removeItem('token')
          // 这里可以添加路由跳转逻辑
          break
        case 403:
          message.error('拒绝访问')
          break
        case 404:
          message.error(data?.message || '请求的资源不存在')
          break
        case 408:
          message.error('请求超时')
          break
        case 500:
          message.error(data?.message || '服务器内部错误')
          break
        case 502:
          message.error('网关错误')
          break
        case 503:
          message.error('服务不可用')
          break
        case 504:
          message.error('网关超时')
          break
        default:
          message.error(data?.message || `请求失败 (${status})`)
      }
    } else if (errorMessage) {
      // 网络错误或其他错误
      if (errorMessage.includes('timeout')) {
        message.error('请求超时，请重试')
      } else if (errorMessage.includes('Network Error')) {
        message.error('网络连接失败，请检查网络')
      } else {
        message.error(errorMessage)
      }
    }

    return Promise.reject(error)
  }
)

// 生成请求ID
export function generateRequestId() {
  return Date.now().toString(36) + Math.random().toString(36).substr(2)
}

/** 构建与 axios 拦截器一致的鉴权请求头，供 fetch/SSE 复用 */
export function buildAuthHeaders(extra = {}) {
  const headers = {
    'Content-Type': 'application/json',
    ...extra
  }
  const token = localStorage.getItem('token')
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }
  headers['X-Request-ID'] = generateRequestId()
  return headers
}

// 请求方法封装
const http = {
  get(url, params = {}, config = {}) {
    return request.get(url, { params, ...config })
  },

  post(url, data = {}, config = {}) {
    return request.post(url, data, config)
  },

  put(url, data = {}, config = {}) {
    return request.put(url, data, config)
  },

  delete(url, config = {}) {
    return request.delete(url, config)
  },

  patch(url, data = {}, config = {}) {
    return request.patch(url, data, config)
  },

  // 文件上传
  upload(url, formData, onProgress, config = {}) {
    return request.post(url, formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      onUploadProgress: (progressEvent) => {
        if (onProgress) {
          const progress = Math.round(
            (progressEvent.loaded * 100) / progressEvent.total
          )
          onProgress(progress)
        }
      },
      ...config
    })
  },

  // 文件下载
  download(url, params = {}, config = {}) {
    return request.get(url, {
      params,
      responseType: 'blob',
      ...config
    })
  }
}

export default request
export { http }