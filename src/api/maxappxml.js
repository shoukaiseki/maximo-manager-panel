import solonRequest from './solonapi'
import request from '@/utils/request'

/**
 * 查询 MAXPRESENTATION 列表
 * @param {Object} params 查询参数
 * @param {string} params.app APP名称
 * @param {string} params.description 描述
 * @param {string} params.content 内容（PRESENTATION模糊搜索）
 * @param {boolean} params.contentCaseSensitive 内容区分大小写
 * @param {number} params.pageNum 页码
 * @param {number} params.pageSize 每页条数
 */
export function getMaxAppXmlList(params) {
  return solonRequest({
    url: '/maxappxml/list',
    method: 'get',
    params
  })
}

/**
 * 获取 PRESENTATION 源码
 * @param {string} app APP名称
 */
export function getMaxAppXmlSource(app) {
  return solonRequest({
    url: '/maxappxml/source',
    method: 'get',
    params: { app }
  })
}

/**
 * 获取 MAXPRESENTATION 详情（含源码）
 * @param {Object} params
 * @param {number} params.maxPresentationId
 * @param {string} params.app
 */
export function getMaxAppXmlDetail(params) {
  return solonRequest({
    url: '/maxappxml/detail',
    method: 'get',
    params
  })
}
