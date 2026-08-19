import request from '@/utils/request'
import solonRequest from '@/api/solonapi'

// ======== Maximo 脚本直连（经 /maximo 代理） ========

/**
 * 查询日志记录器级别
 * 调用 Maximo 脚本 SKS_LOGGER_LEVEL_QUERY
 * @param {Array} loggers - [{ loggerName }]，为空数组时查询所有日志器
 * 响应: { success, message, result: [{ loggerName, level }] }
 */
export function queryLoggerLevel(loggers) {
  return request({
    url: '/api/script/SKS_LOGGER_LEVEL_QUERY',
    method: 'post',
    data: { loggers: loggers || [] }
  })
}

/**
 * 更新日志记录器级别
 * 调用 Maximo 脚本 SKS_LOGGER_LEVEL_UPDATE
 * @param {Array} loggers - [{ loggerName, level, ignore? }]
 * 响应: { success, message, result: [{ loggerName, level, status, reason? }] }
 */
export function updateLoggerLevel(loggers) {
  return request({
    url: '/api/script/SKS_LOGGER_LEVEL_UPDATE',
    method: 'post',
    data: { loggers: loggers || [] }
  })
}

// ======== 日志级别配置（Solon 后端 + MySQL） ========

/**
 * 查询全部日志级别配置
 */
export function listLoggerConfig() {
  return solonRequest({
    url: '/loggerlevel/list',
    method: 'get'
  })
}

/**
 * 查询未忽略的日志级别配置（用于"更新到 Maximo"）
 */
export function getActiveLoggerConfig() {
  return solonRequest({
    url: '/loggerlevel/active',
    method: 'get'
  })
}

/**
 * 批量保存日志级别配置（全量覆盖）
 * @param {Array} loggers - [{ loggerName, level, ignored, description, sortOrder }]
 */
export function saveLoggerConfig(loggers) {
  return solonRequest({
    url: '/loggerlevel/save',
    method: 'post',
    data: { loggers: loggers || [] }
  })
}

/**
 * 删除单条日志级别配置
 * @param {number} id
 */
export function deleteLoggerConfig(id) {
  return solonRequest({
    url: '/loggerlevel/delete',
    method: 'post',
    params: { id: id }
  })
}

/**
 * 增量导入日志级别配置到默认表（已存在的跳过）
 * @param {Array} loggers - [{ loggerName, level, ignored?, description? }]
 * 返回: { added, skipped, total }
 */
export function importLoggerConfig(loggers) {
  return solonRequest({
    url: '/loggerlevel/import',
    method: 'post',
    data: { loggers: loggers || [] }
  })
}

// ======== 日志级别分组（用户自建分组） ========

/**
 * 查询全部分组
 */
export function listLoggerGroups() {
  return solonRequest({
    url: '/loggerlevel/group/list',
    method: 'get'
  })
}

/**
 * 查询单个分组
 */
export function getLoggerGroup(id) {
  return solonRequest({
    url: '/loggerlevel/group/detail',
    method: 'get',
    params: { id: id }
  })
}

/**
 * 新建分组
 * @param {{name:string, description?:string}} data
 */
export function createLoggerGroup(data) {
  return solonRequest({
    url: '/loggerlevel/group/create',
    method: 'post',
    data: data
  })
}

/**
 * 更新分组（重命名/描述）
 */
export function updateLoggerGroup(id, data) {
  return solonRequest({
    url: '/loggerlevel/group/update',
    method: 'post',
    params: { id: id },
    data: data
  })
}

/**
 * 删除分组（级联删除其条目）
 */
export function deleteLoggerGroup(id) {
  return solonRequest({
    url: '/loggerlevel/group/delete',
    method: 'post',
    params: { id: id }
  })
}

/**
 * 查询分组条目
 */
export function listLoggerGroupItems(groupId) {
  return solonRequest({
    url: '/loggerlevel/group/items',
    method: 'get',
    params: { groupId: groupId }
  })
}

/**
 * 全量保存分组条目（覆盖式）
 * @param {number} groupId
 * @param {Array} items - [{ loggerName, level, ignored, description, sortOrder }]
 */
export function saveLoggerGroupItems(groupId, items) {
  return solonRequest({
    url: '/loggerlevel/group/items/save',
    method: 'post',
    data: { groupId: groupId, items: items || [] }
  })
}

/**
 * 删除单条分组条目
 */
export function deleteLoggerGroupItem(id) {
  return solonRequest({
    url: '/loggerlevel/group/items/delete',
    method: 'post',
    params: { id: id }
  })
}

/**
 * 增量添加日志器到分组（已存在跳过，默认 level=INFO）
 * @param {number} groupId
 * @param {Array} loggerNames - ["maximo.script", ...]
 * 返回: { added, skipped, items }
 */
export function addLoggerToGroup(groupId, loggerNames) {
  return solonRequest({
    url: '/loggerlevel/group/items/add',
    method: 'post',
    data: { groupId: groupId, loggerNames: loggerNames || [] }
  })
}

// ======== MXLogger 日志管理（树结构，最多一层子级，按组分） ========

/**
 * 查询 MXLogger 配置树
 * @param {number|null} groupId 组ID；null 查询所有
 * 响应: { success, data: [{ id, groupId, parentId, logger, level, active, remark, sortOrder, children: [] }] }
 */
export function listLoggerMx(groupId) {
  return solonRequest({
    url: '/loggerlevel/mx/list',
    method: 'get',
    params: groupId != null ? { groupId: groupId } : {}
  })
}

/**
 * 全量保存 MXLogger 配置（清空指定组/旧数据后重建树）
 * @param {Array} loggers - [{ logger, level, active, remark, sortOrder, children: [] }]
 * @param {number|null} groupId 组ID；null 保存旧数据区
 */
export function saveLoggerMx(loggers, groupId) {
  return solonRequest({
    url: '/loggerlevel/mx/save',
    method: 'post',
    data: { groupId: groupId != null ? groupId : null, loggers: loggers || [] }
  })
}

// ======== MXLogger 组管理 ========

/**
 * 查询全部 MXLogger 组
 */
export function listLoggerMxGroups() {
  return solonRequest({
    url: '/loggerlevel/mx/group/list',
    method: 'get'
  })
}

/**
 * 创建 MXLogger 组
 * @param {{name:string, description?:string}} data
 */
export function createLoggerMxGroup(data) {
  return solonRequest({
    url: '/loggerlevel/mx/group/create',
    method: 'post',
    data: data
  })
}

/**
 * 更新 MXLogger 组（重命名/描述）
 */
export function updateLoggerMxGroup(id, data) {
  return solonRequest({
    url: '/loggerlevel/mx/group/update',
    method: 'post',
    params: { id: id },
    data: data
  })
}

/**
 * 删除 MXLogger 组（级联删除其下节点）
 */
export function deleteLoggerMxGroup(id) {
  return solonRequest({
    url: '/loggerlevel/mx/group/delete',
    method: 'post',
    params: { id: id }
  })
}

/**
 * 更新 MXLogger 到 Maximo（调用脚本 SKS_LOGGER_MANAGE）
 * @param {Array} loggers - [{ logger, loglevel, logkey, active, children: [{ logger, loglevel, logkey, active }] }]
 */
export function pushLoggerMxToMaximo(loggers) {
  return request({
    url: '/api/script/SKS_LOGGER_MANAGE',
    method: 'post',
    data: loggers || []
  })
}
