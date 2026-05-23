import request from '@/utils/request'

export function queryMessages(params) {
  return request({
    url: '/api/os/MXAPIMESSAGE' ,
    method: 'get',
    headers: { 'Content-Type': 'application/json' },
    params: params
  })
}