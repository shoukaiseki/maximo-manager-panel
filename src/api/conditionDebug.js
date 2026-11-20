import request from '@/utils/request'
import solonRequest from './solonapi'

/**
 * 调试条件表达式（调用 SKS_COND_DEBUG_API 脚本）
 * data: {
 *   mboName:    "WORKORDER",               // 必填: MBO 对象名称
 *   mboWhere:   "wonum = '1000'",          // 必填: MBO 记录过滤条件
 *   expression: "status = 'APPR'",         // 必填: 条件表达式内容
 *   condType:   "WORKFLOW" | "SIGOPTION",  // 可选: 工作流 / 签名
 *   parserType: "SYSTEM" | "CUSTOM",       // 可选: 系统解析器 / 自定义SQL解析器
 *   fields:     "wonum,status",            // 可选: 附加输出字段
 *   maxRows:    10                         // 可选: 多条匹配返回的候选条数
 * }
 */
export function debugCondition(data, params) {
  return request({
    url: '/api/script/SKS_COND_DEBUG_API',
    method: 'post',
    params: Object.assign({ _action: 'debug', _langcode: 'ZH' }, params),
    data: data
  })
}

// ======== 调试记录（MySQL，Solon 后端） ========

// 查询调试记录列表
export function listCondDebugRecords(keyword) {
  return solonRequest({
    url: '/conddebug/list',
    method: 'get',
    params: { keyword }
  })
}

// 获取单条调试记录
export function getCondDebugRecord(id) {
  return solonRequest({
    url: '/conddebug/detail',
    method: 'get',
    params: { id }
  })
}

// 保存调试记录（按 recordName 匹配，存在则更新）
export function saveCondDebugRecord(data) {
  return solonRequest({
    url: '/conddebug/save',
    method: 'post',
    data
  })
}

// 删除调试记录
export function deleteCondDebugRecord(id) {
  return solonRequest({
    url: '/conddebug/delete',
    method: 'post',
    params: { id }
  })
}
