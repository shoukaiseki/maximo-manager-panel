import request from '@/utils/request'

export function queryMessages(params) {
  return request({
    url: '/api/os/MXAPIMESSAGE' ,
    method: 'get',
    headers: { 'Content-Type': 'application/json' },
    params: params
  })
}

export function exportMessages(params) {
  return request({
    url: '/api/script/SKS_EXPORT_MESSAGES',
    method: 'post',
    data: params
  })
}