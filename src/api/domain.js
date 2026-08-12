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
