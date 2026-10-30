import solonRequest from './solonapi'

/**
 * 查询 MAXSEQUENCE 序列列表（分页）
 * @param {Object} params { tbname, name, sequencename, pageNum, pageSize, where }
 */
export function getMaxSequenceList(params) {
  return solonRequest({
    url: '/maxsequence/list',
    method: 'get',
    params
  })
}

/**
 * 序列诊断：ids 有值时诊断指定行，否则按过滤条件诊断全部
 * @param {Object} params { ids, tbname, name, sequencename, where }
 * 返回行附加 V_MAXRECORD / V_CURRENTVALUE / V_STATUS
 */
export function diagnoseMaxSequence(params) {
  return solonRequest({
    url: '/maxsequence/diagnose',
    method: 'get',
    params,
    timeout: 180000
  })
}

/**
 * 修复序列：对勾选记录执行 ALTER SEQUENCE ... RESTART WITH 记录最大值+1
 * @param {Object} params { ids: '1,2,3' }
 * 返回 [{ MAXSEQUENCEID, SUCCESS, MESSAGE }]
 */
export function repairMaxSequence(params) {
  return solonRequest({
    url: '/maxsequence/repair',
    method: 'post',
    params,
    timeout: 180000
  })
}
