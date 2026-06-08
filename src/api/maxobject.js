import solonRequest from '@/api/solonapi'

/**
 * 查询 MAXOBJECT 列表
 * @param {string} keyword 搜索关键词
 * @param {number} pageNum 页码
 * @param {number} pageSize 每页条数
 */
export function getMaxObjectList(keyword, pageNum = 1, pageSize = 20) {
  return solonRequest({
    url: '/maxobject/list',
    method: 'get',
    params: { keyword, pageNum, pageSize }
  })
}

/**
 * 查询 MAXOBJECT 详情
 * @param {string} objectname 对象名
 */
export function getMaxObjectDetail(objectname) {
  return solonRequest({
    url: `/maxobject/${objectname}/detail`,
    method: 'get'
  })
}

/**
 * 查询 MAXOBJECT 域信息
 * @param {string} objectname 对象名
 */
export function getMaxObjectDomains(objectname) {
  return solonRequest({
    url: `/maxobject/${objectname}/domains`,
    method: 'get'
  })
}