import solonRequest from '@/api/solonapi'

/**
 * 查询自动化脚本列表
 */
export function getAutoScriptList(params) {
  return solonRequest({
    url: '/autoscript/list',
    method: 'get',
    params: params
  })
}

/**
 * 查询脚本详情（主表 + 启动点 + 变量）
 */
export function getAutoScriptDetail(name) {
  return solonRequest({
    url: '/autoscript/fullDetail',
    method: 'get',
    params: { name: name }
  })
}

/**
 * 查询脚本源码
 */
export function getAutoScriptSource(name) {
  return solonRequest({
    url: '/autoscript/source',
    method: 'get',
    params: { name: name }
  })
}

/**
 * 查询脚本启动点列表
 */
export function getAutoScriptLaunchPoints(name) {
  return solonRequest({
    url: '/autoscript/launchpoints',
    method: 'get',
    params: { name: name }
  })
}

/**
 * 查询脚本历史记录列表
 */
export function getAutoScriptHistory(name) {
  return solonRequest({
    url: '/autoscript/history',
    method: 'get',
    params: { name: name }
  })
}

/**
 * 查询脚本历史记录详情（含 SOURCE）
 */
export function getAutoScriptHistoryDetail(id) {
  return solonRequest({
    url: '/autoscript/historyDetail',
    method: 'get',
    params: { id: id }
  })
}
