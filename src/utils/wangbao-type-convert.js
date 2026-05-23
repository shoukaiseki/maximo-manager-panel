/**
 * 参数转换为 Array ,如果参数为 string 则分隔成数组
 * @param param
 * @param separator  分隔符:默认','
 * @returns {any}
 */
export function paramToArray(param,separator){
    if(param === undefined){
        return param;
    }
    // console.log("typeof param",typeof param)
    // console.log("param.constructor",param.constructor)
    if(param.constructor === Array){
       return param
    }
    let paramArray=undefined;
    if(param.constructor ===  Number){
        paramArray=[param];
    }

    if(param.constructor ===  String){
        paramArray=param.split((separator||","));
        //分隔之后去掉空值
        paramArray=paramArray.map(el => el.trim()).filter(item => item.trim() != '');
    }

    return paramArray;
}
