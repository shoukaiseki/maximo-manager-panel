import request from '@/utils/request'

// 查找实例选择列表
export function lookupTreeDemoSelectTarget(query) {
  return request({
    url: '/test/DemoSelectTarget/lookupTree',
    method: 'post',
    params: query
  })
}

// 查找实例选择列表
export function lookupDemoSelectTarget(query) {
  return request({
    url: '/test/DemoSelectTarget/lookup',
    method: 'post',
    params: query
  })
}

// 查询实例选择列表
export function listDemoSelectTarget(query) {
  return request({
    url: '/test/DemoSelectTarget/list',
    method: 'get',
    params: query
  })
}

// 查询实例选择详细
export function getDemoSelectTarget(demoSelectTargetId) {
  return request({
    url: '/test/DemoSelectTarget/' + demoSelectTargetId,
    method: 'get'
  })
}

// 新增实例选择
export function addDemoSelectTarget(data) {
  return request({
    url: '/test/DemoSelectTarget',
    method: 'post',
    data: data
  })
}

// 修改实例选择
export function updateDemoSelectTarget(data) {
  return request({
    url: '/test/DemoSelectTarget',
    method: 'put',
    data: data
  })
}

// 删除实例选择
export function delDemoSelectTarget(demoSelectTargetId) {
  return request({
    url: '/test/DemoSelectTarget/' + demoSelectTargetId,
    method: 'delete'
  })
}

// 导出实例选择
export function exportDemoSelectTarget(query) {
  return request({
    url: '/test/DemoSelectTarget/export',
    method: 'get',
    params: query
  })
}