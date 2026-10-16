import request from '@/utils/request'

/**
 * 缓存管理（直连 Maximo 脚本 SKS_CACHE_MANAGE，经 /maximo 代理）
 * 通过 _action 区分动作：list / detail / reload / delete
 */
const SCRIPT_API = 'api/script/SKS_CACHE_MANAGE'

/**
 * 查询所有 Maximo 缓存（缓存名 + 记录条数 + 缓存类名）
 * 后端通过 MXServer.getMaximoCacheNames() 取缓存名，再逐个 getFromMaximoCache() 取缓存对象
 * @returns {Promise} 响应体 { status, data: { total, list: [{ name, count, className, mtEnabled }] } }
 */
export function listCaches() {
  return request({
    url: SCRIPT_API,
    method: 'post',
    params: {
      _action: 'list',
      _langcode: 'zh'
    }
  })
}

/**
 * 重载缓存（服务器间同步更新）
 * @param {string[]} cacheNames 缓存名列表
 */
export function reloadCaches(cacheNames) {
  return request({
    url: SCRIPT_API,
    method: 'post',
    params: {
      _action: 'reload',
      _cacheNames: (cacheNames || []).join(','),
      _langcode: 'zh'
    }
  })
}

/**
 * 删除(卸载)缓存。MXServer 仅支持租户级缓存卸载，非租户级缓存返回 skipped。
 * @param {string[]} cacheNames 缓存名列表
 */
export function deleteCaches(cacheNames) {
  return request({
    url: SCRIPT_API,
    method: 'post',
    params: {
      _action: 'delete',
      _cacheNames: (cacheNames || []).join(','),
      _langcode: 'zh'
    }
  })
}
