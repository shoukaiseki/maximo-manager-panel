import solonRequest from './solonapi'

/**
 * 查询 IFACE 日志列表
 */
export function getIfaceLogList(params) {
  return solonRequest({
    url: '/ifacelog/list',
    method: 'get',
    params
  })
}

/**
 * 获取 IFACE 日志详情（含 REQBODY/RESPBODY）
 */
export function getIfaceLogDetail(logId) {
  return solonRequest({
    url: '/ifacelog/detail',
    method: 'get',
    params: { logId }
  })
}
