import solonRequest from './solonapi'

/**
 * 上传多语言消息 Excel，返回前 10 行预览与文件缓存会话标识
 * @param {FormData} formData 含 file、trim
 * @returns {Promise} { sessionId, fileName, languages, columns, rows, totalRows, invalidLanguages, missingLanguages, canUpdate, disabledReason }
 */
export function previewMessageExcel(formData) {
  return solonRequest({
    url: '/messageimport/preview',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 300000
  })
}

/**
 * 确认更新（按 sessionId 读取服务端缓存文件并写入 MAXMESSAGES / L_MAXMESSAGES）
 * @param {Object} params { sessionId, trim }
 * @returns {Promise} importAppInfo 风格的执行结果
 */
export function executeMessageImport(params) {
  return solonRequest({
    url: '/messageimport/execute',
    method: 'post',
    params,
    timeout: 300000
  })
}
