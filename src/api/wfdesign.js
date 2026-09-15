import request from '@/utils/request'

/**
 * SKS.AUTOSCRIPT.WORKFLOW 工作流/操作/角色 查询接口
 * URL 参数: _type=workflows(缺省)|actions|maxroles  _action=list|detail|export|import
 * 请求体: {where:"SQL"} / {processName,processRev} / {id} 等, list 可带 pageNum/pageSize
 */

// ==================== 工作流(WFPROCESS) ====================

/** 工作流列表(精简主记录) */
export function workflowList(data) {
  return request({
    url: '/api/script/SKS.AUTOSCRIPT.WORKFLOW',
    method: 'post',
    params: { _langcode: 'ZH', _type: 'workflows', _action: 'list' },
    data: data
  })
}

/** 单个工作流完整详情(含节点/出线/分配/通知等全部子记录) */
export function workflowDetail(data) {
  return request({
    url: '/api/script/SKS.AUTOSCRIPT.WORKFLOW',
    method: 'post',
    params: { _langcode: 'ZH', _type: 'workflows', _action: 'detail' },
    data: data
  })
}

/**
 * 工作流批量导出
 * params 可带: { _refs: true|false }  true(缺省,migration)=连同流程引用到的操作/角色一并导出
 * body: {"where":"..."} / {"processName":"PRCHG","processRev":1}
 */
export function workflowExport(data, params) {
  return request({
    url: '/api/script/SKS.AUTOSCRIPT.WORKFLOW',
    method: 'post',
    params: Object.assign({ _langcode: 'ZH', _type: 'workflows', _action: 'export' }, params || {}),
    data: data
  })
}

/**
 * 工作流导入
 * params 可带: { _impMode: 'migration'|'add', _enable: true|false }
 * body: 导出原样 {"workflows":[...]} / 裸数组 / 单对象, 可带 syncFlag:true(全量同步, 仅 migration 生效)
 */
export function workflowImport(data, params) {
  return request({
    url: '/api/script/SKS.AUTOSCRIPT.WORKFLOW',
    method: 'post',
    params: Object.assign({ _langcode: 'ZH', _type: 'workflows', _action: 'import' }, params || {}),
    data: data
  })
}

// ==================== 操作(ACTION) ====================

/** 操作列表 */
export function actionList(data) {
  return request({
    url: '/api/script/SKS.AUTOSCRIPT.WORKFLOW',
    method: 'post',
    params: { _langcode: 'ZH', _type: 'actions', _action: 'list' },
    data: data
  })
}

/** 单个操作完整详情 */
export function actionDetail(data) {
  return request({
    url: '/api/script/SKS.AUTOSCRIPT.WORKFLOW',
    method: 'post',
    params: { _langcode: 'ZH', _type: 'actions', _action: 'detail' },
    data: data
  })
}

/**
 * 操作批量导出
 * body: {"where":"..."} / {"action":"ACT_XXX"} / 空=全部操作
 */
export function actionExport(data) {
  return request({
    url: '/api/script/SKS.AUTOSCRIPT.WORKFLOW',
    method: 'post',
    params: { _langcode: 'ZH', _type: 'actions', _action: 'export' },
    data: data
  })
}

/**
 * 操作导入
 * params 可带: { _impMode: 'migration'|'add' }
 * body: 导出原样 {"actions":[...]} / 裸数组 / 单对象
 */
export function actionImport(data, params) {
  return request({
    url: '/api/script/SKS.AUTOSCRIPT.WORKFLOW',
    method: 'post',
    params: Object.assign({ _langcode: 'ZH', _type: 'actions', _action: 'import' }, params || {}),
    data: data
  })
}

// ==================== 角色(MAXROLE) ====================

/** 角色列表 */
export function maxRoleList(data) {
  return request({
    url: '/api/script/SKS.AUTOSCRIPT.WORKFLOW',
    method: 'post',
    params: { _langcode: 'ZH', _type: 'maxroles', _action: 'list' },
    data: data
  })
}

/** 单个角色完整详情 */
export function maxRoleDetail(data) {
  return request({
    url: '/api/script/SKS.AUTOSCRIPT.WORKFLOW',
    method: 'post',
    params: { _langcode: 'ZH', _type: 'maxroles', _action: 'detail' },
    data: data
  })
}

/**
 * 角色批量导出
 * body: {"where":"..."} / {"maxrole":"ROLE1"} / 空=全部角色
 */
export function maxRoleExport(data) {
  return request({
    url: '/api/script/SKS.AUTOSCRIPT.WORKFLOW',
    method: 'post',
    params: { _langcode: 'ZH', _type: 'maxroles', _action: 'export' },
    data: data
  })
}

/**
 * 角色导入
 * params 可带: { _impMode: 'migration'|'add' }
 * body: 导出原样 {"maxroles":[...]} / 裸数组 / 单对象
 */
export function maxRoleImport(data, params) {
  return request({
    url: '/api/script/SKS.AUTOSCRIPT.WORKFLOW',
    method: 'post',
    params: Object.assign({ _langcode: 'ZH', _type: 'maxroles', _action: 'import' }, params || {}),
    data: data
  })
}
