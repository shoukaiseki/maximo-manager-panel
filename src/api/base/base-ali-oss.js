import request from '/src/utils/request'


/**
 * 不关联 document 的 stsToken 获取
 * @param params    fileName:  文件名称,需要根据文件名称获取后缀名
 * @return {*}
 */
export function getStsInfoUnDocument(params) {
  return request({
    url: 'wb/document/stsInfoUnDocument',
    method: 'post',
    params: {
      fileName: params.fileName
    }
  })
}

/**
 * 关联 document 的 stsToken 获取
 * @param params    ownerId:   业务所属主键
 *                  ownerName: 业务所属名称
 *                  fileName:  文件名称,需要根据文件名称获取后缀名
 * @return {*}
 */
export function getStsInfoHasDocument(params) {
  return request({
    url: 'wb/document/stsInfo',
    method: 'post',
    params: params
  })
}

// 变更
export function changeOSSUpdateStatus(data) {
  return request({
    url: 'wb/document/changeOSSUpdateStatus',
    method: 'post',
    params: data
  })
}


//暂未使用
export function getAppVersionStsToken(params) {
  return request({
    url: 'wb/AppVersion/stsInfo',
    method: 'post',
    params: params
  })
}

export function deleteDocument(wbProductId) {
    return request({
        url: '/wb/document/'+wbProductId,
        method: 'delete',
    })
}

export function uploadFileToAlmighty(data,params,onUploadProgress) {
    return request({
        url: 'wb/document/uploadFile',
        method: 'post',
        params: params,
        data: data,
        onUploadProgress: onUploadProgress
    })
}
