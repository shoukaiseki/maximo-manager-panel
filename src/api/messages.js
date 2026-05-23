import request from '@/utils/request'

export function queryMessages(params) {
  return request({
    url: '/api/messages/query',
    method: 'get',
    params: params
  })
}