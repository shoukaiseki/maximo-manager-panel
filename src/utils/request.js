import axios from 'axios'
import { Notification, MessageBox, Message } from 'element-ui'
import store from '/src/store'
import { getToken } from '/src/utils/auth'
import errorCode from '/src/utils/errorCode'
import { showApiErrorDialog } from './mmp/api-error'

// 从响应体中提取脚本错误(含 errorMessageTrace/debugMsg 时才认为需要对话框展示)
function extractScriptError(data) {
  let payload = data
  if (typeof payload === 'string') {
    const text = payload.trim()
    if (!text.startsWith('{')) return null
    try { payload = JSON.parse(text) } catch (e) { return null }
  }
  if (payload && payload.status === 'error' && (payload.errorMessageTrace || payload.debugMsg)) {
    return payload
  }
  return null
}

axios.defaults.headers['Content-Type'] = 'application/json;charset=utf-8'
// 创建axios实例
const service = axios.create({
  // 超时
  timeout: 5000000
})

// request拦截器
service.interceptors.request.use(config => {
  
  // 从 localStorage 获取认证信息
  const saved = localStorage.getItem('maximo-env-settings')
  console.log("request interceptors saved=",saved)
  if (saved) {
    try {
      // 是否需要设置 token
      const isToken = (config.headers || {}).isToken === false
      if (getToken() && !isToken) {
        const settings = JSON.parse(saved)
        if (settings.useApiKey && settings.apiKey) {
          config.headers['apikey'] = settings.apiKey
          console.log("set apikey")
        } else if (settings.maxauth) {
          config.headers['maxauth'] = settings.maxauth
        }
      }
    } catch (e) {
      console.error('解析配置失败', e)
    }
  }
  
  // 修改后
  let urlPath = config.url
  console.log('urlPath', urlPath)
  // 如果第一个字符是 /，则删掉
  if (urlPath.startsWith('/')) {
    urlPath = urlPath.slice(1)
  }
  let url = '/maximo/' + urlPath;
  console.log('url', url)
  // get请求映射params参数
  if (config.method === 'get' && config.params) {
    url += '?'
    for (const propName of Object.keys(config.params)) {
      const value = config.params[propName];
      var part = encodeURIComponent(propName) + "=";
      if (value !== null && typeof(value) !== "undefined") {
        let paramFormatType=0;
        if (value.constructor === Array) {
          paramFormatType=0;
          for (const key of Object.keys(value)) {
            if (key.constructor === Object||key.constructor === Array) {
              paramFormatType=1;
            }
            continue;
          }
        }else if (value.constructor === Object) {
          paramFormatType=1;
        }else{
          paramFormatType=0;
        }
        if (paramFormatType === 1) {
          for (const key of Object.keys(value)) {
            let params = propName + '[' + key + ']';
            var subPart = encodeURIComponent(params) + "=";
            url += subPart + encodeURIComponent(value[key]) + "&";
          }
        } else {
          url += part + encodeURIComponent(value) + "&";
        }
      }
    }
    console.log('url', url)
    url = url.slice(0, -1);
    console.log('url', url)
    config.params = {};
  }
  config.url = url;
  return config
}, error => {
  console.log(error)
  Promise.reject(error)
})

// 响应拦截器
service.interceptors.response.use(res => {
  // 脚本接口返回 status:'error' 且含 errorMessageTrace/debugMsg 时, 统一弹出错误对话框
  const scriptError = extractScriptError(res.data)
  if (scriptError) {
    showApiErrorDialog(scriptError)
  }
  return res
}, error => {
  console.log('err' + error)
  // HTTP 错误响应体中含 errorMessageTrace/debugMsg 时, 同样用对话框提示, 不再弹通用 toast
  const scriptError = error.response ? extractScriptError(error.response.data) : null
  if (scriptError) {
    showApiErrorDialog(scriptError)
    return Promise.reject(error)
  }
  // 统一格式化错误信息并提示（参考 jyy001 request.js）
  let { message } = error;
  if (message == "Network Error") {
    message = "后端接口连接异常";
  } else if (message.includes("timeout")) {
    message = "系统接口请求超时";
  } else if (message.includes("Request failed with status code")) {
    let code = message.substr(message.length - 3);
    if (code == '502') {
      message = "系统正在维护,请稍后再试";
    } else {
      message = "系统接口" + code + "异常";
    }
  }
  Message({
    message: message,
    type: 'error',
    duration: 5 * 1000
  })
  // 不要静默吞掉错误,让 Promise 进入 catch 分支
  return Promise.reject(error)
})

export default service