import solonRequest from './solonapi'

/**
 * 查询 DB2 锁表列表
 */
export function getDb2LockList(params) {
  return solonRequest({
    url: '/db2lock/list',
    method: 'get',
    params
  })
}
