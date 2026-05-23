import request from '/src/utils/request'


// 新增人员工序
export function addUserProcedure(data) {
  return request({
    url: '/base/UserProcedure',
    method: 'post',
    data: data
  })
}


// 删除人员工序
export function delUserProcedure(data) {
    return request({
        url: '/base/UserProcedure/delete' ,
        method: 'post',
        data:data,
    })
}

