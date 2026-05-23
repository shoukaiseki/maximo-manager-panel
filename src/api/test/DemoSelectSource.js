import request from '@/utils/request'

// 查找实例选择源列表
export function lookupTreeDemoSelectSource(query) {
  return request({
    url: '/test/DemoSelectSource/lookupTree',
    method: 'post',
    params: query
  })
}

// 查找实例选择源列表
export function lookupDemoSelectSource(query) {
  return request({
    url: '/test/DemoSelectSource/lookup',
    method: 'post',
    params: query
  })
}

// 查询实例选择源列表
export function listDemoSelectSource(query) {
  return request({
    url: '/test/DemoSelectSource/list',
    method: 'get',
    params: query
  })
}

// 查询实例选择源详细
export function getDemoSelectSource(demoSelectSourceId) {
  return request({
    url: '/test/DemoSelectSource/' + demoSelectSourceId,
    method: 'get'
  })
}

// 新增实例选择源
export function addDemoSelectSource(data) {
  return request({
    url: '/test/DemoSelectSource',
    method: 'post',
    data: data
  })
}

// 修改实例选择源
export function updateDemoSelectSource(data) {
  return request({
    url: '/test/DemoSelectSource',
    method: 'put',
    data: data
  })
}

// 删除实例选择源
export function delDemoSelectSource(demoSelectSourceId) {
  return request({
    url: '/test/DemoSelectSource/' + demoSelectSourceId,
    method: 'delete'
  })
}

// 导出实例选择源
export function exportDemoSelectSource(query) {
  return request({
    url: '/test/DemoSelectSource/export',
    method: 'get',
    params: query
  })
}