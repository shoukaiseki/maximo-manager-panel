import request from '@/utils/request'

export function migrateSecurGroup(params, data) {
  return request({
    url: '/api/script/SKS_MIGRATEAPI_SECURGROUP',
    method: 'post',
    params: params,
    data: data,
    timeout: 1200000,
    headers: { 'Content-Type': 'application/json' }
  })
}

export function querySecurGroup(params) {
  return request({
    url: '/api/script/SKS_MIGRATEAPI_SECURGROUP',
    method: 'get',
    headers: { 'Content-Type': 'application/json' },
    params: params
  })
}

/**
 * 查询应用列表(MAXAPPS)
 * @param {Object} params { app: 应用名模糊匹配 }
 */
export function querySecurGroupApps(params) {
  return request({
    url: '/api/script/SKS_MIGRATEAPI_SECURGROUP',
    method: 'get',
    headers: { 'Content-Type': 'application/json' },
    params: Object.assign({ _action: 'apps' }, params)
  })
}
