import request from '@/utils/request'

// 自动键(AUTOKEY)迁移 API 封装
// 后端脚本: SKS_MIGRATEAPI_AUTOKEY (查询/详情/导出/导入)
// 说明: 统一用 POST, 动作走 URL 查询参数 _action, 过滤条件放 JSON 请求体

const SCRIPT = 'SKS_MIGRATEAPI_AUTOKEY'

function post(action, query, data) {
  return request({
    url: `/api/script/${SCRIPT}`,
    method: 'post',
    params: Object.assign({ _action: action }, query),
    data: data
  })
}

// 分页查询: { autokeyname, prefix, siteid, orgid, setid, level, where, pageNum, pageSize }
// → { autokeys:[ 行(含 sks:level / sks:ownerdescription) ], total, pageNum, pageSize }
export function queryAutokeyList(data) {
  return post('query', {}, data)
}

// 详情: { autokeyid } 或 { autokeyname, siteid/orgid/setid } → { autokeys:[ 行 ] }
export function getAutokeyDetail(params) {
  return post('detail', params || {}, {})
}

// 导出(不分页): { autokeynames:[...] } 或过滤条件 → { autokeys:[ 行 ] }
export function exportAutokey(data) {
  return post('export', {}, data || {})
}

// 导入: { autokeys:[ 定义对象... ] } → { total, added, updated, deleted, failed, results }
export function importAutokey(data, opts) {
  const query = {}
  if (opts && opts.dmDeploySet) query.dmDeploySet = 'true'
  if (opts && opts.errorAssert) query._errorAssert = 'true'
  return post('import', query, data)
}
