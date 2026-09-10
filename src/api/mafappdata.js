import solonRequest from './solonapi'

/**
 * 查询 MAFAPPDATA 列表（不含 APP BLOB，含 APPLEN）
 * @param {Object} params
 * @param {string} params.appid 应用名（支持 =精确 / 模糊）
 * @param {string} params.status 状态（ACTIVE/...）
 * @param {string} params.ismobile 是否移动端（0/1）
 * @param {number} params.pageNum
 * @param {number} params.pageSize
 */
export function getMafAppDataList(params) {
  return solonRequest({
    url: '/mafappdata/list',
    method: 'get',
    params
  })
}

/**
 * 查询 MAFAPPDATA 单条详情（不含 APP BLOB，含 APPLEN）
 * @param {number} id MAFAPPDATAID
 */
export function getMafAppDetail(id) {
  return solonRequest({
    url: '/mafappdata/detail',
    method: 'get',
    params: { id }
  })
}

/**
 * 分析上传的应用包：校验 zip 合法性并读取 build.json 提取 appid/version
 * @param {FormData} formData 含 file 字段
 * @returns {Promise} { valid, appid, version, size, fileName }
 */
export function analyzeMafApp(formData) {
  return solonRequest({
    url: '/mafappdata/analyze',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 300000
  })
}

/**
 * 导入应用包（.apkg 完整包 或 原始 graphite zip + 表单元数据）
 * @param {FormData} formData 含 file, appid, version, appmode, status, ismobile, revision, deployby
 */
export function importMafApp(formData) {
  return solonRequest({
    url: '/mafappdata/import',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 300000
  })
}

/**
 * 导出应用包（返回二进制 Blob）
 * 单条 → 生产 zip：{APPID}__ver-{VERSION}.zip（如 masuserprofile__ver-9.1.893.0.zip）；
 * 多条 → .zip（内含多个生产 zip）
 * @param {string} ids 逗号分隔的 MAFAPPDATAID，如 "58" 或 "58,57,56"
 * @returns {Promise<Blob>}
 */
export function exportMafApp(ids) {
  return solonRequest({
    url: '/mafappdata/export',
    method: 'get',
    params: { ids },
    responseType: 'blob',
    timeout: 300000
  })
}
