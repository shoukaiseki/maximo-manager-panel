import request from '@/utils/request'

// 应用XML标签(MAXLABELS)多语言迁移 API 封装
// 后端脚本: SKS_MIGRATEAPI_MAXLABELS (查询/详情/导出/导入)
// 说明: 统一用 POST, 动作走 URL 查询参数 _action, 过滤条件放 JSON 请求体

const SCRIPT = 'SKS_MIGRATEAPI_MAXLABELS'

function post(action, query, data) {
  return request({
    url: `/api/script/${SCRIPT}`,
    method: 'post',
    params: Object.assign({ _action: action }, query),
    data: data
  })
}

// 分页查询: { app, id, property, value, where, pageNum, pageSize }
// → { maxlabelsList:[ 行 ], total, pageNum, pageSize }
export function queryMaxLabels(data, opts) {
  const query = { _langcode: (opts && opts.langcode) || 'ZH' }
  if (opts && opts.allLangcode) query._allLangcode = 'true'
  return post('query', query, data)
}

// 详情: { app, id, property } → { maxlabelsList:[ 行(含 sks:langdata) ] }
export function getMaxLabelDetail(params, opts) {
  const query = { _langcode: (opts && opts.langcode) || 'ZH' }
  if (!opts || opts.allLangcode !== false) query._allLangcode = 'true'
  return post('detail', query, params || {})
}

// 导出(不分页): { app } 或 { where } → { maxlabelsList:[ 行 ] }
export function exportMaxLabels(data, opts) {
  const query = { _langcode: (opts && opts.langcode) || 'ZH' }
  if (opts && opts.allLangcode) query._allLangcode = 'true'
  return post('export', query, data || {})
}

// 导入: { maxlabelsList:[ 标签对象... ] } → { total, added, updated, deleted, skipped, failed, results, sks:*SumTotal }
// 导入统一走 SKS_MIGRATEAPI_CONFIGUR, 用 _onlyFunctions=mlmaxlabels 指定仅执行 MAXLABELS 多语言
export function importMaxLabels(data, opts) {
  const query = { _action: 'import', _onlyFunctions: 'mlmaxlabels' }
  if (opts && opts.dmDeploySet) query.dmDeploySet = 'true'
  if (opts && opts.allLangcode) query._allLangcode = 'true'
  if (opts && opts.errorAssert) query._errorAssert = 'true'
  return request({
    url: '/api/script/SKS_MIGRATEAPI_CONFIGUR',
    method: 'post',
    params: query,
    data: data
  })
}
