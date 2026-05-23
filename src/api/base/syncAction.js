import request from '/src/utils/request'

export function syncAction(syncActionList,aliasIn) {
    let alias=aliasIn
    if(!alias){
        if (Object.prototype.hasOwnProperty.call(syncActionList[0], alias)) {
            alias=syncActionList[0].alias
        }
        if(!alias){
            alias=syncActionList[0].ownerName+"_"+syncActionList[0].beanName
        }
    }
    return request({
        // url: '/aqoeous',
        url: '/business/syncAction/'+alias,
        method: 'post',
        data: syncActionList
    })
}
