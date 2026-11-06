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
