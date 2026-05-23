import request from '/src/utils/request'
import { VNode } from 'vue'
import { AqoeousOptions } from './aqoeous'

// 查找地点列表
/**
 *
 * @param queryObject       AqoeousOptions 集合
 * @param aliasIn               后端没什么所用,方便前端显示时方便在network查看时更直观的看到属于哪个接口的连接
 * @return {Object}
 */
export function aqoeous(queryObject,aliasIn) {
    let queryObjectList
    if(queryObject.constructor === Array){
        queryObjectList=queryObject
    }else if(queryObject.constructor === Object){
        queryObjectList=[queryObject]
    }else{
        throw new Error("aqoeous 调用的第一个参数有问题");
    }
    let alias=aliasIn||queryObjectList[0].qoName
    return request({
        // url: '/aqoeous',
        url: '/aqoeousDateFormat/'+alias,
        method: 'post',
        data: queryObjectList
    })
}


/**
 * 关联查询文件作为子表
 * @param  ownerIdPropName             主表物资主键属性名
 * @param  ownerName                   所属业务名称
 * @param resultName                   并入主表的属性
 * @return {AqoeousOptions}
 */
//未测试
export function documentQueryListChild(ownerIdPropName,ownerName,resultName="virtualDocumentList"){
    if (ownerIdPropName == undefined||ownerName == undefined) {
       throw new Error("ownerIdPropName,ownerName 不能为空,请使用当前业务主键名称")
    }
    const obj={
        resultName: resultName,
        sourceData: ownerIdPropName,
    }
    const queryObject =
        {
            qoName: 'documentQO',
            mergeParent : true,
            listFirstMergeParent : true,
            listMergeParent : false,
            resultName:obj.resultName,
            qoJson: {
                pageNum: 1,
                pageSize:  10,
                ownerName: ownerName,
            },
            relationships: [
                {
                    paramName: "exactQueryUniqueIdList",
                    sourceDatas: [
                        obj.sourceData
                    ]
                }
            ]
        }
    return queryObject;
}


//如果选项不存在则进行查询
export function aqoeousNoToQueryFunction(value,qoName){
    const queryObjectList = [
        {
            qoName: qoName,
            mergeParent : true,
            listFirstMergeParent : true,
            listMergeParent : false,
            resultName: "mainList",
            qoJson: {
                exactQueryUniqueIdList: [value]
            },
        }
    ]
    return new Promise((resolve, reject) => {
        aqoeous(queryObjectList).then(response => {
            let data=response.data.mainList
            resolve(data)
        }).catch(err => {
            // console.log(err)
            reject(err)
        })
    })
}
