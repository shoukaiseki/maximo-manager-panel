import {mergeFromObject} from "./sks";


/**
 *
 * @param a  自定义的一些基础信息
 * @return {{}}
 */
export function defaultTableEntity(options={}){
    const tmp={
        queryParams:{
            start: 0,
            limit: 100,
            pageSize: 10,
            pageNum: 1,
        },
        list:[],
        total: 0,
        loading: true,

        single : false,
        multiple : false,
        //点击的行
        currentRow: {},
        //勾选的行ID集合
        ids:[],
        // 勾选的行
        selection: [],
    }
    const tableEntity=mergeFromObject({},tmp,options)
    mergeFromObject(tableEntity.queryParams,tmp.queryParams,options.queryParams)
    return tableEntity
}

export function  defaultCurrentDialogEntity(options={}){

    const tmp={
        title: undefined,
        width: undefined,
    }
    const baseEntity=mergeFromObject({},tmp,options)
    return baseEntity

}
