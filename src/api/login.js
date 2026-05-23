import request from '@/utils/request'

const userInfo=`{"msg":"操作成功","code":200,"permissions":["*:*:*"],"roles":["admin"],"user":{"virtualReturnType":null,"virtualOrderByFilterList":null,"lookupBitFilter":null,"showVirtualBitFilter":null,"virtualChangeBitFlag":0,"documentIdList":null,"searchValueList":null,"searchValue":null,"deptId":100,"createBy":"admin","createUserId":null,"updateUserId":null,"createTime":"2020-12-09 14:33:15","updateBy":null,"updateTime":null,"remark":"管理员","params":{},"pageNum":null,"pageSize":null,"checkRowstamp":false,"companyId":0,"userId":1,"userName":"admin","nickName":"旺宝科技","email":"jiang28555@qq.com","phonenumber":"13106013865","sex":"1","avatar":"/profile/avatar/2021/06/09/7d19917c-a592-4f22-accd-7775e964856a.jpeg","salt":null,"status":"0","delFlag":"0","loginIp":"127.0.0.1","loginDate":"2020-12-09T14:33:15.000+0800","dept":{"virtualReturnType":null,"virtualOrderByFilterList":null,"lookupBitFilter":null,"showVirtualBitFilter":null,"virtualChangeBitFlag":0,"documentIdList":null,"searchValueList":null,"searchValue":null,"deptId":100,"createBy":null,"createUserId":null,"updateUserId":null,"createTime":null,"updateBy":null,"updateTime":null,"remark":null,"params":{},"pageNum":null,"pageSize":null,"checkRowstamp":false,"parentId":0,"ancestors":null,"deptName":"公司","orderNum":"0","leader":"超级super","phone":null,"email":null,"status":"0","delFlag":null,"parentName":null,"children":[]},"roles":[{"virtualReturnType":null,"virtualOrderByFilterList":null,"lookupBitFilter":null,"showVirtualBitFilter":null,"virtualChangeBitFlag":0,"documentIdList":null,"searchValueList":null,"searchValue":null,"deptId":null,"createBy":null,"createUserId":null,"updateUserId":null,"createTime":null,"updateBy":null,"updateTime":null,"remark":null,"params":{},"pageNum":null,"pageSize":null,"checkRowstamp":false,"roleId":1,"roleName":"超级管理员","roleKey":"admin","roleSort":"1","dataScope":"1","menuCheckStrictly":false,"deptCheckStrictly":false,"status":"0","delFlag":null,"flag":false,"menuIds":null,"deptIds":null,"admin":true}],"roleIds":null,"postIds":null,"admin":true}}`

// 登录方法
export function login(username, password, code, uuid) {
  const data = {
    username,
    password,
    code,
    uuid
  }
  return request({
    url: '/login',
    method: 'post',
    data: data
  })
}

// 获取用户详细信息
export function getInfo() {
    return new Promise((resolve, reject) => {
        let res = JSON.parse(userInfo);
        console.log("res",res)
        // throw new Error("")
        resolve( res )
    })
}

// 退出方法
export function logout() {
  return request({
    url: '/logout',
    method: 'post'
  })
}

// 获取验证码
export function getCodeImg() {
  return request({
    url: '/captchaImage',
    method: 'get'
  })
}
