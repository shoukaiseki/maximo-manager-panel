import axios from 'axios'

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
  return Promise.reject(error)
})

export default solonRequest