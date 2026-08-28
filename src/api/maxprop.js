import request from '@/utils/request'

/**
 * 调用 SKS_MAXPROP_MANAGE 脚本（MAXPROP/MAXPROPVALUE 属性管理）
 * params: _action=list(列表查询) / export(完整导出) / deploy(导入), _langcode, apiType, ignoreDefVal, pageNum, pageSize
 * data:   { where: "SQL条件" } 或 { propname, description, servername }(字段搜索) 或 导入数据数组
 */
export function callMaxPropManage(params, data) {
  return request({
    url: '/api/script/SKS_MAXPROP_MANAGE',
    method: 'post',
    params: params,
    data: data
  })
}

// 便捷方法
export function queryMaxProps(params, data) {
  return callMaxPropManage({ ...params, _action: 'list' }, data)
}

export function exportMaxProps(params, data) {
  return callMaxPropManage({ ...params, _action: 'export' }, data)
}

export function deployMaxProps(params, data) {
  return callMaxPropManage({ ...params, _action: 'deploy' }, data)
}