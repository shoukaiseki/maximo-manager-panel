import solonRequest from './solonapi'

/**
 * 保存的查询管理（MySQL，按 app 区分应用）
 */

// 查询某应用的保存查询列表
export function getSavedQueryList(app) {
  return solonRequest({
    url: '/savedquery/list',
    method: 'get',
    params: { app }
  })
}

// 获取单个保存查询
export function getSavedQueryDetail(id) {
  return solonRequest({
    url: '/savedquery/detail',
    method: 'get',
    params: { id }
  })
}

// 保存查询（存在 app+queryname 则更新）
export function saveSavedQuery(data) {
  return solonRequest({
    url: '/savedquery/save',
    method: 'post',
    data
  })
}

// 删除保存的查询
export function deleteSavedQuery(id) {
  return solonRequest({
    url: '/savedquery/delete',
    method: 'post',
    params: { id }
  })
}
