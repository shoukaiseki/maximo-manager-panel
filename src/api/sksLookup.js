import request from '@/utils/request'

/**
 * 调用 Maximo 自动化脚本 SKS_LOOKUP_API 查询 lookup 列表数据
 *
 * 契约:
 *   URL 查询参数: _langcode(ZH/EN), _debug(可选, true 时返回 debugMsg)
 *   body: {
 *     lookup,                               // lookup 名称(对应 src/components/sks/_lookup.js)
 *     objectname,                           // 查询对象名(为空时服务端按 lookup 解析)
 *     relationship,                         // 关系名(可选, 配合 parentObject/parentKeys 使用)
 *     parentObject, parentKeys, parentWhere,// 关系查询时定位父记录(可选)
 *     where, orderby,                       // 过滤条件 / 排序
 *     columns: [String|Object],             // 需要返回的列, 元素可为字段名或 {dataattribute,label,width}
 *     keyword, keywordColumns,              // 关键字模糊搜索(默认搜索所有可过滤列)
 *     pageNum, pageSize, maxRows            // 分页(服务端内存分页)
 *   }
 *   响应: {
 *     status: 'success' | 'error',
 *     message,
 *     data: { lookup, objectname, columns, rows, total, pageNum, pageSize }
 *   }
 *
 * @param {Object} data 请求体
 * @param {Object} params URL 查询参数(可选)
 */
export function queryLookupData(data, params) {
  return request({
    url: '/api/script/SKS_LOOKUP_API',
    method: 'post',
    params: Object.assign({ _langcode: 'ZH' }, params || {}),
    data: data || {}
  })
}

/**
 * 按 lookup 名称查询列表数据(便捷方法)
 * @param {string} lookup lookup 名称
 * @param {Object} body 其余请求参数
 */
export function queryLookupByName(lookup, body) {
  return queryLookupData(Object.assign({ lookup: lookup }, body || {}))
}
