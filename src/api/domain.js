import request from '@/utils/request'
import solonRequest from '@/api/solonapi'

/**
 * 调用 SKS.AUTOSCRIPT.OBJECTS 脚本(域列表/导出)
 * URL 查询参数: _type=domains, _action=list(列表) / export(完整导出), _langcode, ignoreDefVal(精简导出)
 * body: { where: "SQL条件", apiType: "manage", pageNum?, pageSize? }
 * 注意: apiType / pageNum / pageSize 由脚本从请求体读取，不要放到 URL 参数中
 */
export function exportDomains(params, data) {
  return request({
    url: '/api/script/SKS.AUTOSCRIPT.OBJECTS',
    method: 'post',
    params: Object.assign({ _type: 'domains' }, params),
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
 * 导入域定义(走 SKS.AUTOSCRIPT.OBJECTS, _type=domains&_action=import)
 * @param {Object|Array} data 域数组 / {"domains":[...]} / 单个域定义对象
 * @param {Object} [params] 额外 URL 参数, 如 { syncFlag: true } 表示同步模式
 * 响应: {status,message,summary:{total,success,failed},result:[{domainid,status,message}]}
 */
export function importDomains(data, params) {
  return request({
    url: '/api/script/SKS.AUTOSCRIPT.OBJECTS',
    method: 'post',
    params: Object.assign({ _langcode: 'ZH', _type: 'domains', _action: 'import' }, params),
    data: data
  })
}
