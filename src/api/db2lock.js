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

/**
 * DB2 锁诊断：对指定进程执行固定诊断SQL集，返回汇总分析报告
 * @param {Object} params - { agentId, tabName }
 */
export function diagnoseDb2Lock(params) {
  return solonRequest({
    url: '/db2lock/diagnose',
    method: 'get',
    params,
    // 诊断会串行执行多条监控视图查询，超时放宽
    timeout: 90000
  })
}
