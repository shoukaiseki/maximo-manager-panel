import solonRequest from '@/api/solonapi'
import request from '@/utils/request'

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

/**
 * 查询菜单列表（按 MENUTYPE 分组）
 */
export function getMaxMenuList(moduleApp, menuType) {
  return solonRequest({
    url: '/maxmenu/list',
    method: 'get',
    params: { moduleApp, menuType }
  })
}

/**
 * 查询签名选项（SIGOPTION）
 */
export function getSigOption(app) {
  return solonRequest({
    url: '/maxmenu/sigoption',
    method: 'get',
    params: { app }
  })
}

/**
 * 查询签名条件属性（CTRLGROUP → CTRLCONDITION → CTRLCONDPROP）
 */
export function getCtrlGroup(app) {
  return solonRequest({
    url: '/maxmenu/ctrlgroup',
    method: 'get',
    params: { app }
  })
}

/**
 * 导出 MAXAPPINFO (SIGOPTION + SIGOPTFLAG + MAXMENU) 为 JSON
 * 调用 Maximo 自动化脚本 SKS_EXP_MAXAPPINFO
 * @param {string} app 应用名称
 * @param {boolean} ignoreDefVal 精简模式(省略空值/默认值)
 */
export function exportMaxAppInfo(app, ignoreDefVal) {
  return request({
    url: '/api/script/SKS_EXP_MAXAPPINFO',
    method: 'post',
    params: { app: app, ignoreDefVal: ignoreDefVal ? 'true' : 'false' },
    data: {}
  })
}
