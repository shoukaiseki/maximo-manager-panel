import solonRequest from './solonapi'

/**
 * 表数据统计列表
 * @param {Object} params { where: '自定义where条件(可选,过滤表列表)' }
 */
export function getTableStatsList(params) {
  return solonRequest({
    url: '/tablestats/list',
    method: 'get',
    params
  })
}