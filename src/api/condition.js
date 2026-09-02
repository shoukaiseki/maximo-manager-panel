import request from '@/utils/request'

/**
 * 调用 SKS_CONDITION_MANAGE 脚本（CONDITION 条件表达式管理）
 * params: _action=list(列表查询) / export(完整导出) / deploy(导入), _langcode, apiType, ignoreDefVal, pageNum, pageSize
 * data:   { where: "SQL条件" } 或 { conditionnum, description, type, expression }(字段搜索) 或 导入数据数组
 */
export function callConditionManage(params, data) {
  return request({
    url: '/api/script/SKS_CONDITION_MANAGE',
    method: 'post',
    params: params,
    data: data
  })
}

// 便捷方法
export function queryConditions(params, data) {
  return callConditionManage({ ...params, _action: 'list' }, data)
}

export function exportConditions(params, data) {
  return callConditionManage({ ...params, _action: 'export' }, data)
}

export function deployConditions(params, data) {
  return callConditionManage({ ...params, _action: 'deploy' }, data)
}