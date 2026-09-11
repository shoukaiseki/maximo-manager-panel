import axios from 'axios'
import { Message } from 'element-ui'

/**
 * Solon 后端 API 请求封装
 * 通过 /solonapi 代理访问 Solon 后端服务
 */
const solonRequest = axios.create({
  timeout: 30000
})

// 请求拦截器
solonRequest.interceptors.request.use(config => {
  // 从 localStorage 获取 apiKey
  const saved = localStorage.getItem('maximo-env-settings')
  console.log("request interceptors saved=",saved)
  if (saved) {
    try {
      const settings = JSON.parse(saved)
      if (settings.useApiKey && settings.apiKey) {
        config.headers['X-API-Key'] = settings.apiKey
        console.log("set apikey")
      }
    } catch (e) {
      console.error('解析配置失败', e)
    }
  }

  // 添加 /solonapi 前缀
  let urlPath = config.url || ''
  if (urlPath.startsWith('/')) {
    urlPath = urlPath.slice(1)
  }
  config.url = '/solonapi/' + urlPath

  console.log('[SolonAPI]', config.method, config.url, config.params)
  return config
}, error => {
  return Promise.reject(error)
})

// 响应拦截器
solonRequest.interceptors.response.use(res => {
  return res.data
}, error => {
  console.error('[SolonAPI] 请求失败:', error.message)
  // 统一格式化错误信息并提示（参考 jyy001 request.js）
  let { message } = error
  if (message == 'Network Error') {
    message = '后端接口连接异常'
  } else if (message.includes('timeout')) {
    message = '系统接口请求超时'
  } else if (message.includes('Request failed with status code')) {
    const code = message.substr(message.length - 3)
    if (code == '502') {
      message = '系统正在维护,请稍后再试'
    } else {
      message = '系统接口' + code + '异常'
    }
  }
  Message({
    message: message,
    type: 'error',
    duration: 5 * 1000
  })
  return Promise.reject(error)
})

export default solonRequest

// ======== Excel 导入相关 API ========

/**
 * 预览 Excel 文件（原始数据）
 * @param {FormData} formData - 包含 file 字段的表单数据
 */
export function excelPreview(formData) {
  return solonRequest({
    url: '/excelimport/preview',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/**
 * 使用 jxls XML 配置解析 Excel 并预览
 * @param {FormData} formData - 包含 file 和 xmlConfig 字段的表单数据
 */
export function excelPreviewWithConfig(formData) {
  return solonRequest({
    url: '/excelimport/previewWithConfig',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/**
 * 执行 Excel 导入
 * @param {FormData} formData - 包含 file, xmlConfig, params 字段的表单数据
 */
export function excelExecute(formData) {
  return solonRequest({
    url: '/excelimport/execute',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// ======== 配置方案管理 API ========

/**
 * 获取所有配置方案列表
 */
export function listSchemes() {
  return solonRequest({
    url: '/excelimport/schemes',
    method: 'get'
  })
}

/**
 * 获取单个配置方案
 * @param {number} id 方案 ID
 */
export function getScheme(id) {
  return solonRequest({
    url: '/excelimport/schemes/' + id,
    method: 'get'
  })
}

/**
 * 保存配置方案（新增）
 * @param {Object} data - { schemeName, xmlConfig, description, sheetName }
 */
export function saveScheme(data) {
  return solonRequest({
    url: '/excelimport/schemes',
    method: 'post',
    data: data
  })
}

/**
 * 更新配置方案
 * @param {number} id 方案 ID
 * @param {Object} data
 */
export function updateScheme(id, data) {
  return solonRequest({
    url: '/excelimport/schemes/' + id,
    method: 'put',
    data: data
  })
}

/**
 * 删除配置方案
 * @param {number} id 方案 ID
 */
export function deleteScheme(id) {
  return solonRequest({
    url: '/excelimport/schemes/' + id,
    method: 'delete'
  })
}