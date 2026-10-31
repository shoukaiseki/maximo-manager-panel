import solonRequest from './solonapi'

/**
 * 查询 MAXAPPS 应用列表
 * @param {Object} params 查询参数
 * @param {string} params.app APP名称
 * @param {string} params.description 描述
 * @param {string} params.content 内容（PRESENTATION模糊搜索）
 * @param {boolean} params.contentCaseSensitive 内容区分大小写
 * @param {string} params.tab 标签页过滤: all=所有, xml=有XML的应用, mafapp=MAFAPP(APPTYPE in ('WC'))
 * @param {number} params.pageNum 页码
 * @param {number} params.pageSize 每页条数
 */
export function getMaxAppsList(params) {
  return solonRequest({
    url: '/maxapps/list',
    method: 'get',
    params
  })
}

/**
 * 查询单个应用详情（MAXAPPS 完整字段）
 * @param {Object} params
 * @param {string} params.app 应用名
 */
export function getMaxAppsDetail(params) {
  return solonRequest({
    url: '/maxapps/detail',
    method: 'get',
    params
  })
}
