import request from '@/utils/request'

/**
 * 调用 SKS.AUTOSCRIPT.OBJECTS 脚本（对象结构 MAXINTOBJECT 管理）
 * 契约:
 *   URL 查询参数: _type=integrationobjects, _action=list|detail|export|import,
 *                 _langcode, ignoreDefVal
 *   body:
 *     list/detail/export: 纯查询条件 {where?|id?, pageNum?, pageSize?, apiType?}
 *     import: 导出格式纯数据 {"integrationobjects":[...]} (可带 syncFlag:true)
 *   导出结果可直接复制作为导入请求体
 */
export function callIntObjectManage(action, body, queryParams) {
  const params = Object.assign(
    { _langcode: 'ZH', _type: 'integrationobjects', _action: action },
    queryParams || {}
  )
  return request({
    url: '/api/script/SKS.AUTOSCRIPT.OBJECTS',
    method: 'post',
    params: params,
    data: body || {}
  })
}

// 便捷方法
export function queryIntObjects(params) {
  return callIntObjectManage('list', Object.assign({ apiType: 'manage' }, params || {}))
}

export function exportIntObjects(params) {
  // ignoreDefVal 走 URL, 不进 body
  const ignoreDefVal = params && params.ignoreDefVal
  const queryParams = {}
  if (ignoreDefVal === true || ignoreDefVal === 'true') {
    queryParams.ignoreDefVal = 'true'
  } else if (ignoreDefVal === false || ignoreDefVal === 'false') {
    // 显式声明完整导出(与服务端默认值一致, 便于日志/抓包区分意图)
    queryParams.ignoreDefVal = 'false'
  }
  const body = Object.assign({}, params || {})
  delete body.ignoreDefVal
  return callIntObjectManage('export', body, queryParams)
}

// data: 导出格式纯数据 {"integrationobjects":[...]} 或 {integrationobjects:[...], syncFlag:true}
export function deployIntObjects(data) {
  return callIntObjectManage('import', data)
}
