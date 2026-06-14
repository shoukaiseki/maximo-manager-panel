import solonRequest from '@/api/solonapi'

/**
 * 查询所有模块列表
 */
export function getMaxMenuModules() {
  return solonRequest({
    url: '/maxmenu/modules',
    method: 'get'
  })
}

/**
 * 查询完整菜单树
 */
export function getMaxMenuFullTree(sortBy) {
  return solonRequest({
    url: '/maxmenu/fullTree',
    method: 'get',
    params: { sortBy: sortBy || 'zh' }
  })
}

/**
 * 查询指定模块的菜单树
 */
export function getMaxModuleTree(module) {
  return solonRequest({
    url: '/maxmenu/moduleTree',
    method: 'get',
    params: { module: module }
  })
}

/**
 * 查询应用菜单（APPMENU）
 */
export function getAppMenu(app) {
  return solonRequest({
    url: '/maxmenu/appMenu',
    method: 'get',
    params: { app: app }
  })
}

/**
 * 查询应用工具栏（APPTOOL）
 */
export function getAppTool(app) {
  return solonRequest({
    url: '/maxmenu/appTool',
    method: 'get',
    params: { app: app }
  })
}

/**
 * 搜索菜单
 */
export function searchMaxMenu(keyword, menuType, elementType) {
  return solonRequest({
    url: '/maxmenu/search',
    method: 'get',
    params: { keyword, menuType, elementType }
  })
}
