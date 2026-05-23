/**
 * 数组中是否包含某个元素
 * @param arr           数组:推荐使用 字符数组,数字数组 ,如果为对象推荐使用 findRowInListFromAttrName
 * @param value         元素
 * @param typeEqual     类型是否一致,false:== , true:===
 * @returns {boolean}
 */
export function isInArray(arr,value,typeEqual=false){
    if(arr===undefined||value===undefined){
        return false;
    }
    // console.log("typeEqual=",typeEqual)
    if(typeEqual){
        for(let i = 0; i < arr.length; i++){
            if(value === arr[i]){
                return true;
            }
        }
    }else{
        for(let i = 0; i < arr.length; i++){
            // console.log("arr[i]",arr[i])
            // console.log("value",value)
            if(value == arr[i]){
                return true;
            }
        }
    }
    return false;
}
/**
 * 数组中是否包含某个元素
 * @param arr           数组:推荐使用 字符数组,数字数组 ,如果为对象推荐使用 findRowInListFromAttrName
 * @param value         元素
 * @param typeEqual     类型是否一致,false:== , true:===
 * @returns {boolean}
 */
export function isNotInArray(arr,value,typeEqual=false){
    return !isInArray(arr,value,typeEqual);
}

/**
 * 合并数组,只增加不重复的元素
 * @param sourceList        原数组
 * @param multipleArr       多个要合并的数组 [[{id:1,name:'one'}],[{id:2,name:'two'}]]
 * @param arr               要合并的数组 [{id:1,name:'one'}]
 * @param equalsFun         自定义比较方法 参考 equalsFun(obj,item)
 * @param attrName          要比较的属性
 *                              元素为 number,string  该属性与equalsFun 都保持为空,则直接两个对象进行比较是否相等
 * @param typeEqual         是否同时进行类型判断:false == 判断,默认true使用 === 判断
 * @return {*[]}            sourceList
 */
export function mergeArrayToNoExistsList({sourceList=[],multipleArr,arr,equalsFun=defaultMergeArrayToNoExistsListEqualsFun,attrName=undefined,typeEqual=true}){
    if(!multipleArr&&!arr){
        return sourceList;
    }
    let multipleArrTmp=[arr]
    if (multipleArr) {
        multipleArrTmp=multipleArr
    }

        for (let attTmp of multipleArrTmp) {
            for (let item of attTmp) {
                if(sourceList.filter((obj)=>defaultMergeArrayToNoExistsListEqualsFun(obj,item,attrName,typeEqual)).length>0){
                    continue
                }
                sourceList.push(item);
            }
        }
    return sourceList;
}

export function defaultMergeArrayToNoExistsListEqualsFun(obj,item,attrName,typeEqual){
    if(attrName){
    return typeEqual?obj[attrName]===item[attrName]:obj[attrName]==item[attrName];
    }else{
        return typeEqual?obj===item:obj==item;
    }
}
