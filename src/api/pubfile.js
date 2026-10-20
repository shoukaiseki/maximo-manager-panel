import solonRequest from './solonapi'

/**
 * 公开文件管理 API（Maximo Management Panel 后端）
 *
 * 契约：
 *   POST /api/pubfiles/upload          multipart 字段名 file，需认证（X-API-Key，solonapi 拦截器自动附带）
 *   GET  /api/anon/pubfiles/list       文件列表，无需认证（免认证接口统一用 /api/anon/ 前缀）
 *
 * 公开访问 URL 由后端生成并随文件记录一起返回，前端直接使用（相对路径补代理前缀后即可）；
 * 上传成功返回 { code, message, data }，列表返回 { code, message, data: [...] }。
 */

/** 前端代理前缀（vue.config.js 中 /solonapi → 后端服务，并去掉该前缀） */
export const PUB_FILE_PROXY = '/solonapi'
/** 需认证的后端 @Mapping 前缀 */
export const PUB_FILE_API = '/api/pubfiles'
/** 免认证（/api/anon/ 前缀）的后端 @Mapping 前缀 */
export const PUB_FILE_ANON_API = '/api/anon/pubfiles'

/**
 * 上传文件（单文件，FormData 字段名 file）
 * @param {FormData} formData 含 file 字段
 * @returns {Promise} { success, message, data }
 */
export function uploadPublicFile(formData) {
  return solonRequest({
    url: PUB_FILE_API + '/upload',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 300000
  })
}

/**
 * 查询文件列表（无需认证）
 * @returns {Promise} { code, message, data: [] }
 */
export function listPublicFiles() {
  return solonRequest({
    url: PUB_FILE_ANON_API + '/list',
    method: 'get'
  })
}

/**
 * 取文件记录的公开访问 URL（后端生成，主页图片链接直接用）
 * 后端 url-prefix 是唯一拼前缀的地方，这里优先原样信任后端返回值：
 *  - 绝对地址（http/https）原样返回；
 *  - 相对地址补上根路径 '/'（避免按当前页面路径解析）后原样返回；
 *  - 仅当后端未配 url-prefix（返回默认的 /api/anon/pubfiles/xxx）时才补前端代理前缀，方便开发环境直连后端。
 * @param {Object} file 文件记录
 * @returns {string}
 */
export function buildPublicFileUrl(file) {
  if (!file) return ''
  let text = String(file.url || file.publicUrl || file.downloadUrl || '')
  if (!text) return ''
  if (/^https?:\/\//i.test(text)) return text
  if (text.charAt(0) !== '/') {
    text = '/' + text
  }
  return text.indexOf('/api/') === 0 ? PUB_FILE_PROXY + text : text
}
