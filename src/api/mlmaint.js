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
  return invoke('SKS_MIGRATEAPI_MAXMODULES', Object.assign({ _action: 'import' }, params), data)
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
  return invoke('SKS_MIGRATEAPI_MAXMENU', Object.assign({ _action: 'import' }, params), data)
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
  return invoke('SKS_MIGRATEAPI_SIGOPTION', Object.assign({ _action: 'import' }, params), data)
}
