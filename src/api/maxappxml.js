import solonRequest from './solonapi'
import request from '@/utils/request'

/**
 * 查询 MAXPRESENTATION 列表
 * @param {Object} params 查询参数
 * @param {string} params.app APP名称
 * @param {string} params.description 描述
 * @param {string} params.content 内容（PRESENTATION模糊搜索）
 * @param {boolean} params.contentCaseSensitive 内容区分大小写
 * @param {number} params.pageNum 页码
 * @param {number} params.pageSize 每页条数
 */
export function getMaxAppXmlList(params) {
  return solonRequest({
    url: '/maxappxml/list',
    method: 'get',
    params
  })
}

/**
 * 获取 PRESENTATION 源码
 * @param {string} app APP名称
 */
export function getMaxAppXmlSource(app) {
  return solonRequest({
    url: '/maxappxml/source',
    method: 'get',
    params: { app }
  })
}

/**
 * 获取 MAXPRESENTATION 详情（含源码）
 * @param {Object} params
 * @param {number} params.maxPresentationId
 * @param {string} params.app
 */
export function getMaxAppXmlDetail(params) {
  return solonRequest({
    url: '/maxappxml/detail',
    method: 'get',
    params
  })
}

/**
 * 查询 A_MAXPRESENTATION 审计记录列表
 * @param {Object} params
 * @param {string} params.app APP（支持 =精确 / %模糊）
 * @param {number} params.pageNum
 * @param {number} params.pageSize
 */
export function getMaxAppXmlAuditList(params) {
  return solonRequest({
    url: '/maxappxml/audit/list',
    method: 'get',
    params
  })
}

/**
 * 查询 A_MAXPRESENTATION 审计记录详情（含 PRESENTATION 源码）
 * @param {string} eaudittransid 审计交易ID
 */
export function getMaxAppXmlAuditDetail(eaudittransid) {
  return solonRequest({
    url: '/maxappxml/audit/detail',
    method: 'get',
    params: { eaudittransid }
  })
}

/**
 * 导出应用信息（以 MAXAPPS 为主对象，含 MAXPRESENTATION/MAFAPPDATA 元信息，默认不含 XML 与 zip 字节码）
 * 调用 Maximo 脚本 SKS.AUTOSCRIPT.SCREENS 新方式导出（内部使用 SKS_MIGRATE_* 脚本）
 * @param {string} app 应用名称
 * @param {Object} [opts]
 * @param {boolean} [opts.allLangcode] 导出所有语言信息（URL 参数 _allLangcode=true）
 * @param {string} [opts.include] 大字段参数：presentation / zip
 */
export function exportAppInfo(app, opts) {
  opts = opts || {}
  return request({
    url: '/api/script/SKS.AUTOSCRIPT.SCREENS',
    method: 'get',
    params: {
      _action: 'exportAppInfo',
      app: app,
      _langcode: 'ZH',
      _allLangcode: opts.allLangcode ? 'true' : undefined,
      _include: opts.include
    }
  })
}

/**
 * 导入应用信息（请求体为导出的应用信息 JSON）
 * @param {Object} payload 应用信息 JSON（app 为目标应用）
 * @param {Object} [opts]
 * @param {boolean} [opts.dmDeploySet] 启用迁移模式（URL 参数 dmDeploySet=true）
 */
export function importAppInfo(payload, opts) {
  opts = opts || {}
  return request({
    url: '/api/script/SKS.AUTOSCRIPT.SCREENS',
    method: 'post',
    params: {
      _action: 'importAppInfo',
      _langcode: 'ZH',
      dmDeploySet: opts.dmDeploySet ? 'true' : undefined
    },
    data: payload
  })
}

/**
 * 下载 MAFAPPDATA.APP 的 zip 字节码（二进制流）
 * @param {string} app 应用名称
 */
export function downloadAppZip(app) {
  return request({
    url: '/api/script/SKS.AUTOSCRIPT.SCREENS',
    method: 'get',
    params: { _action: 'exportAppZip', app: app },
    responseType: 'blob',
    timeout: 300000
  })
}

/**
 * 获取指定语言的 XML（Maximo 脚本 SKS.AUTOSCRIPT.SCREENS 按 _langcode 合并 MAXLABELS 译文）
 * @param {string} app 应用名称
 * @param {string} [langcode] 语言代码，默认 ZH
 */
export function getAppXmlByLang(app, langcode) {
  return request({
    url: '/api/script/SKS.AUTOSCRIPT.SCREENS/' + encodeURIComponent(app),
    method: 'get',
    params: { _langcode: (langcode || 'ZH'), ignoreMetadata: 'true' }
  })
}
