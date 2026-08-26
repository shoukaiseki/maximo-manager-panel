import solonRequest from './solonapi'

/**
 * 工单库存余量分析
 * @param {Object} params { wonum, itemnum, pageNum, pageSize }
 */
export function getWpMaterialStockList(params) {
  return solonRequest({
    url: '/wpmaterialstock/list',
    method: 'get',
    params
  })
}

/**
 * 工单库存余量详情(三类明细)
 * @param {Object} params { siteid, location, itemnum, wonum }
 */
export function getWpMaterialStockDetail(params) {
  return solonRequest({
    url: '/wpmaterialstock/detail',
    method: 'get',
    params
  })
}