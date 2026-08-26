import solonRequest from './solonapi'

/**
 * 表数据统计列表
 * @param {Object} params { sortField: 'OBJECTNAME'|'COUNT', sortOrder: 'asc'|'desc' }
 */
export function getTableStatsList(params) {
  return solonRequest({
    url: '/tablestats/list',
    method: 'get',
    params
  })
}