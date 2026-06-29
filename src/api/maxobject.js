import solonRequest from '@/api/solonapi'
import request from '@/utils/request'

/**
 * 查询 MAXOBJECT 列表
 * @param {string} objectname 对象名搜索（=开头精确，%模糊）
 * @param {string} keyword 关键词搜索（描述模糊）
 * @param {number} pageNum 页码
 * @param {number} pageSize 每页条数
 */
export function getMaxObjectList(objectname, keyword, pageNum = 1, pageSize = 20) {
  return solonRequest({
    url: '/maxobject/list',
    method: 'get',
    params: { objectname, keyword, pageNum, pageSize }
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

/**
 * 查询 MAXATTRIBUTE 列表
 * @param {Object} params 查询参数
 * @param {string} params.objectname 对象名
 * @param {string} params.attributename 属性名
 * @param {string} params.description 描述
 * @param {number} params.pageNum 页码
 * @param {number} params.pageSize 每页条数
 */
export function getMaxAttributeList(params) {
  return solonRequest({
    url: '/maxattribute/list',
    method: 'get',
    params
  })
}

/**
 * 导出数据库配置对象信息
 * @param {string} objectName 对象名
 * @param {boolean} ignoreDefVal 是否忽略默认值（精简模式）
 */
export function exportDbConfig(objectName, ignoreDefVal = true) {
  return request({
    url: 'api/script/SKS_EXPORT_DBCONFIG',
    method: 'post',
    params: {
      _langcode: 'zh',
      ignoreDefVal: ignoreDefVal
    },
    data: {
      objectNames: [objectName]
    }
  })
}