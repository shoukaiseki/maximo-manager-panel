import request from '@/utils/request'

const URL = '/api/script/SKS_MIGRATEAPI_DOMAIN'

/** 域查询(分页) / 详情 / 导出: GET */
export function queryDomain(params) {
  return request({
    url: URL,
    method: 'get',
    headers: { 'Content-Type': 'application/json' },
    params: params
  })
}

/** 域导入: POST JSON */
export function migrateDomain(params, data) {
  return request({
    url: URL,
    method: 'post',
    params: params,
    data: data,
    timeout: 1200000,
    headers: { 'Content-Type': 'application/json' }
  })
}
