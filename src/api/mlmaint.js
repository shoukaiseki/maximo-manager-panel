import request from '@/utils/request'

const TIMEOUT = 1200000

function invoke(script, params, data) {
  return request({
    url: '/api/script/' + script,
    method: 'post',
    params: params,
    data: data || {},
    timeout: TIMEOUT,
    headers: { 'Content-Type': 'application/json' }
  })
}

/**
 * 多语言导入统一入口: SKS_MIGRATEAPI_CONFIGUR + _onlyFunctions 指定功能
 * @param {string} functionCode mlmaxapps / mlmaxmodules / mlmaxmenu / mlsigoption / mlmaxobjectcfg / mlmaxattributecfg
 */
function importMl(functionCode, params, data) {
  return invoke('SKS_MIGRATEAPI_CONFIGUR',
    Object.assign({ _action: 'import', _onlyFunctions: functionCode }, params), data)
}

// ==================== MAXAPPS ====================

export function queryMaxApps(params) {
  return invoke('SKS_MIGRATEAPI_MAXAPPS', Object.assign({ _action: 'query' }, params))
}

export function getMaxAppsDetail(params) {
  return invoke('SKS_MIGRATEAPI_MAXAPPS', Object.assign({ _action: 'detail' }, params))
}

export function exportMaxApps(params) {
  return invoke('SKS_MIGRATEAPI_MAXAPPS', Object.assign({ _action: 'export' }, params))
}

export function importMaxApps(params, data) {
  return importMl('mlmaxapps', params, data)
}

// ==================== MAXMODULES ====================

export function queryMaxModules(params) {
  return invoke('SKS_MIGRATEAPI_MAXMODULES', Object.assign({ _action: 'query' }, params))
}

export function getMaxModuleDetail(params) {
  return invoke('SKS_MIGRATEAPI_MAXMODULES', Object.assign({ _action: 'detail' }, params))
}

export function exportMaxModules(params) {
  return invoke('SKS_MIGRATEAPI_MAXMODULES', Object.assign({ _action: 'export' }, params))
}

export function importMaxModules(params, data) {
  return importMl('mlmaxmodules', params, data)
}

// ==================== MAXMENU ====================

export function queryMaxMenu(params) {
  return invoke('SKS_MIGRATEAPI_MAXMENU', Object.assign({ _action: 'query' }, params))
}

export function getMaxMenuDetail(params) {
  return invoke('SKS_MIGRATEAPI_MAXMENU', Object.assign({ _action: 'detail' }, params))
}

export function exportMaxMenu(params) {
  return invoke('SKS_MIGRATEAPI_MAXMENU', Object.assign({ _action: 'export' }, params))
}

export function importMaxMenu(params, data) {
  return importMl('mlmaxmenu', params, data)
}

// ==================== SIGOPTION ====================

export function querySigOption(params) {
  return invoke('SKS_MIGRATEAPI_SIGOPTION', Object.assign({ _action: 'query' }, params))
}

export function getSigOptionDetail(params) {
  return invoke('SKS_MIGRATEAPI_SIGOPTION', Object.assign({ _action: 'detail' }, params))
}

export function exportSigOption(params) {
  return invoke('SKS_MIGRATEAPI_SIGOPTION', Object.assign({ _action: 'export' }, params))
}

export function importSigOption(params, data) {
  return importMl('mlsigoption', params, data)
}

// ==================== MAXOBJECTCFG ====================

export function queryMaxObjectCfg(params) {
  return invoke('SKS_MIGRATEAPI_MAXOBJECTCFG', Object.assign({ _action: 'query' }, params))
}

export function getMaxObjectCfgDetail(params) {
  return invoke('SKS_MIGRATEAPI_MAXOBJECTCFG', Object.assign({ _action: 'detail' }, params))
}

export function exportMaxObjectCfg(params) {
  return invoke('SKS_MIGRATEAPI_MAXOBJECTCFG', Object.assign({ _action: 'export' }, params))
}

export function importMaxObjectCfg(params, data) {
  // 多语言维护页导入: 只执行 MAXOBJECTCFG 多语言更新
  return importMl('mlmaxobjectcfg', params, data)
}

// ==================== MAXATTRIBUTECFG ====================

export function queryMaxAttributeCfg(params) {
  return invoke('SKS_MIGRATEAPI_MAXATTRIBUTECFG', Object.assign({ _action: 'query' }, params))
}

export function getMaxAttributeCfgDetail(params) {
  return invoke('SKS_MIGRATEAPI_MAXATTRIBUTECFG', Object.assign({ _action: 'detail' }, params))
}

export function exportMaxAttributeCfg(params) {
  return invoke('SKS_MIGRATEAPI_MAXATTRIBUTECFG', Object.assign({ _action: 'export' }, params))
}

/**
 * 仅多语言导出(maxObjects 格式): { maxObjects:[{object, sks:langdata, attributes:[{attribute, sks:langdata}]}] }
 * @param {Object} data { objectNames, attributeFilters }
 * @param {Object} params 其它参数(_langcode 等)
 */
export function exportConfigurMl(data, params) {
  return invoke('SKS_MIGRATEAPI_CONFIGUR',
    Object.assign({ _action: 'export', _onlyInclude: 'ml', _allLangcode: 'true' }, params), data)
}

export function importMaxAttributeCfg(params, data) {
  // 多语言维护页导入: 只执行 MAXATTRIBUTECFG 多语言更新
  return importMl('mlmaxattributecfg', params, data)
}
