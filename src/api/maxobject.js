import solonRequest from '@/api/solonapi'
import request from '@/utils/request'

/**
 * 查询 MAXOBJECT 列表
 * @param {string} objectname 对象名搜索（=开头精确，%模糊）
 * @param {string} keyword 关键词搜索（描述模糊）
 * @param {number} pageNum 页码
 * @param {number} pageSize 每页条数
 * @param {string} where 自定义 where 条件（可选）
 */
export function getMaxObjectList(objectname, keyword, pageNum = 1, pageSize = 20, where) {
  return solonRequest({
    url: '/maxobject/list',
    method: 'get',
    params: { objectname, keyword, pageNum, pageSize, where }
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
 * 查询 MAXOBJECT 的描述信息（英文描述 + 中文描述）
 * @param {string} objectname 对象名
 */
export function getMaxObjectDescription(objectname) {
  return solonRequest({
    url: `/maxobject/${objectname}/description`,
    method: 'get'
  })
}

/**
 * 查询对象被关联的关系（该表作为子表）
 * @param {string} objectname 对象名
 */
export function getMaxObjectChildRelationships(objectname) {
  return solonRequest({
    url: `/maxobject/${objectname}/child-relationships`,
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
 * 导入对象配置（走 SKS.AUTOSCRIPT.LIBRARY 脚本，需 develop=true）
 * @param {Object} data 纯数据包 {"maxObjects":[...]}，每项为对象定义（object/description/attributes/indexes...）
 *                     或 {object:'XXX', delete:true}
 * 响应: {status:'success'|'error', message, stackTrace?}
 */
export function importMaxObjects(data) {
  return request({
    url: 'api/script/SKS.AUTOSCRIPT.LIBRARY',
    method: 'post',
    params: {
      develop: 'true',
      _langcode: 'zh'
    },
    data: data
  })
}

/**
 * 导入对象配置（新模式，走 SKS_MIGRATEAPI_CONFIGUR 脚本）
 * @param {Object} data 纯数据包 {"maxObjects":[...]}
 * @param {Object} params 额外 query 参数 { dmDeploySet: 'true'|'false', ignoreAttributes, ignoreRelationships }
 * 响应: {status:'success', message, data:{total,added,updated,skipped,failed,results}} 或 {status:'error', message, errorMessageTrace}
 */
export function importMaxObjectsConfigur(data, params) {
  return request({
    url: 'api/script/SKS_MIGRATEAPI_CONFIGUR',
    method: 'post',
    params: Object.assign({ _action: 'import', _langcode: 'zh' }, params || {}),
    data: data,
    timeout: 1200000
  })
}

/**
 * 新模式导出对象配置（SKS_MIGRATEAPI_CONFIGUR _action=export）
 * @param {Array<string>} objectNames 对象名列表
 * @param {Object} params { ignoreDefVal: true|false, _allLangcode: 'true'|'false', _langcode }
 * 响应: {status:'success', data:{maxObjects:[...]}, message} 或 {status:'error', message, errorMessageTrace}
 */
export function exportMaxObjectsConfigur(objectNames, params) {
  return request({
    url: 'api/script/SKS_MIGRATEAPI_CONFIGUR',
    method: 'post',
    params: Object.assign({ _action: 'export', _langcode: 'zh' }, params || {}),
    data: { objectNames: objectNames },
    timeout: 600000
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
