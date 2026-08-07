import request from '@/utils/request'

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
