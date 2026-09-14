import request from '@/utils/request'
import solonRequest from '@/api/solonapi'

/**
 * 调用 SKS_EXPORT_DOMAIN 脚本
 * params: _action=list(列表) / export(完整导出), _langcode, apiType, ignoreDefVal, pageNum, pageSize
 * data:   { where: "SQL条件" }
 */
export function exportDomains(params, data) {
  return request({
    url: '/api/script/SKS_EXPORT_DOMAIN',
    method: 'post',
    params: params,
    data: data
  })
}

/**
 * 查询域的子表信息（域值 + 本地化描述）
 * @param {string} domainid 域ID
 * @param {string} domaintype 域类型（ALN/SYNONYM/NUMERIC/NUMRANGE/CROSSOVER/TABLE）
 */
export function getDomainSubtables(domainid, domaintype) {
  return solonRequest({
    url: '/domain/subtables',
    method: 'get',
    params: { domainid, domaintype }
  })
}

/**
 * 调用 SKS.AUTOSCRIPT.OBJECTS 脚本(域结构化详情)
 * 含各值 maxdomvalcond 条件、TABLE/CROSSOVER 的 where 子句与错误消息键、CROSSOVER 字段映射
 * 契约: URL 固定 _type=domains&_action=export; data 为纯查询条件 { where: "SQL条件" }
 */
export function exportDomainObjects(params, data) {
  return request({
    url: '/api/script/SKS.AUTOSCRIPT.OBJECTS',
    method: 'post',
    params: Object.assign({ _type: 'domains', _action: 'export' }, params),
    data: data
  })
}

/**
 * 导入域定义(走 SKS.AUTOSCRIPT.OBJECTS, _type=domains&_action=import)
 * @param {Object|Array} data 域数组 / {"domains":[...]} / 单个域定义对象, 可携带 syncFlag:true 全量同步
 * 响应: {status,message,summary:{total,success,failed},result:[{domainid,status,message}]}
 */
export function importDomains(data) {
  return request({
    url: '/api/script/SKS.AUTOSCRIPT.OBJECTS',
    method: 'post',
    params: { _langcode: 'ZH', _type: 'domains', _action: 'import' },
    data: data
  })
}
