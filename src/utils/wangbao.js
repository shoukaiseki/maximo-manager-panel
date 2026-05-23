import { parseTime } from './ruoyi'

/**
 * 通用js方法封装处理
 * Copyright (c) 2019 ruoyi
 */

//用于生成uuid
export function generateUUIDS4() {
  return (((1+Math.random())*0x10000)|0).toString(16).substring(1);
}

export function generateUUID() {
  return (generateUUIDS4()+generateUUIDS4()+""+generateUUIDS4()+""+generateUUIDS4()+""+generateUUIDS4()+""+generateUUIDS4()+generateUUIDS4()+generateUUIDS4());
}


export function canEditProcessNode(editableNode){
  return editableNode.processStatus<0||editableNode.processStatus>10
}


/**
 * 如果将对象所有属性值清空,设置为undefined
 * @param obj
 * @returns  元对象
 */
export function cleanProperties(obj){
  if (obj==undefined || typeof obj !== 'object' ) {
    return obj
  }
  for (const key in obj) {
    if (Object.prototype.hasOwnProperty.call(obj, key)) {
      obj[key] = undefined
    }
  }
  return obj
}

/**
 * 复制属性
 * @param targetIn    目标
 * @param source      源
 * @returns {any|{}}
 */
export function copyProperties(targetIn,source) {
  const length = arguments.length
  let target = arguments[0] || {}
  if (typeof target !== 'object' && typeof target !== 'function') {
    target = {}
  }
  let i = 1
  if (length === 1) {
    target = this
    i--
  }
  for (i; i < length; i++) {
    const source = arguments[i]
    for (const key in source) {
      // 使用for in会遍历数组所有的可枚举属性，包括原型。
      if (Object.prototype.hasOwnProperty.call(source, key)) {
        // console.log("key",key,"value=",source[key])
        // if(source[key] != undefined){
        target[key] = source[key]
        // }
        // console.log("target[key]",target[key],"source[key]=",source[key])
      }
    }
  }
  return target
}

/**
 * 为空或为 null 返回true
 * @param val
 * @return {boolean}
 */
export function isNullOrUndefined(val){
    return val === null||val === undefined
}

/**
 * 不为空且不为 null
 * @param val
 * @return {boolean}
 */
export function notNullAndNotUndefined(val){
    return !isNullOrUndefined(val)
}

export function cloneObject(obj){
    let o
    // 如果  他是对象object的话  , 因为null,object,array  也是'object';
    if (typeof obj === 'object') {

        // 如果  他是空的话
        if (obj === null) {
            o = null;
        } else {

            // 如果  他是数组arr的话
            if (obj instanceof Array) {
                o = [];
                for (let i = 0, len = obj.length; i < len; i++) {
                    o.push(cloneObject(obj[ i ]));
                }
            }
            // 如果  他是对象object的话
            else {
                o = {};
                for (let j in obj) {
                    o[ j ] = cloneObject(obj[ j ]);
                }
            }

        }
    }else {
        o = obj;
    }
    return o;
};

/**
 * 扩展对象
 * @returns {{}}
 */
export function extendObject() {
    return  mergeFromObject({},...arguments);
  // const length = arguments.length
  // const target = {}
  // for (let i=0; i < length; i++) {
  //   const source = arguments[i]
  //   for (const key in source) {
  //     // 使用for in会遍历数组所有的可枚举属性，包括原型。
  //     if (Object.prototype.hasOwnProperty.call(source, key)) {
  //       // console.log("key",key,"value=",source[key])
  //       if(source[key] != undefined){
  //         target[key] = source[key]
  //       }
  //       // console.log("target[key]",target[key],"source[key]=",source[key])
  //     }
  //   }
  // }
  // return target
}



/**
 * 深拷贝
 * 合并对象,与extendObject相比,只要属性存在,未定义的也会覆盖掉最前面的值,是最基础的对象,返回target本身
 * @returns {{}}
 */
export function mergeFromObject() {
    const length = arguments.length
    if(length<1){
        return {}
    }
    const target = arguments[0]
    for (let i=0; i < length; i++) {
        let source = arguments[i]
        if(source===undefined||source===null||source.constructor !== Object){
            continue;
        }
        for (const key in source) {
            // 使用for in会遍历数组所有的可枚举属性，包括原型。
            if (Object.prototype.hasOwnProperty.call(source, key)) {
                // console.log("mergeFormObject,key",key,"value=",source[key])
                //深拷贝
                target[key] = cloneObject(source[key])
                // console.log("target[key]",target[key],"source[key]=",source[key])
            }
        }
    }
    return target
    // const length = arguments.length
    // for (const key in source) {
    //     // 使用for in会遍历数组所有的可枚举属性，包括原型。
    //     if (Object.prototype.hasOwnProperty.call(source, key)) {
    //         // console.log("key",key,"value=",source[key])
    //         target[key] = source[key]
    //         // console.log("target[key]",target[key],"source[key]=",source[key])
    //     }
    // }
    // return target
}




/**
 * 流程节点重新排序
 */
export function resetProcessNodeListSort(sortList){
  let previousNode=undefined;
  for(var i=0;i<sortList.length;i++){
    const currentNode=sortList[i];
    currentNode.nextNodeId=0;
    currentNode.previousNodeId=0;
    currentNode.sort=i+1;
    if(previousNode!=undefined){
      currentNode.previousNodeId=previousNode.processNodeId;
      previousNode.nextNodeId=currentNode.processNodeId;
    }
    previousNode=currentNode;
  }
  return sortList;
}

/**
 * 流程节点配置重新排序
 */
export function resetProcessNodeConfListSort(sortList){
  let previousNode=undefined;
  for(var i=0;i<sortList.length;i++){
    const currentNode=sortList[i];
    currentNode.nextNodeId=0;
    currentNode.previousNodeId=0;
    currentNode.sort=i+1;
    if(previousNode!=undefined){
      currentNode.previousNodeId=previousNode.processConfNodeId;
      previousNode.nextNodeId=currentNode.processConfNodeId;
    }
    previousNode=currentNode;
  }
  return sortList;
}

/**
 * 多个空格替换为1个空格
 * @returns {*}
 */
export function resetBlank(str){
  if(!str)return str;
  const regEx = /\s+/g;
  return str.replace(regEx, ' ');
}

export function bitFlagToArray(bigFlag){
  // console.log("bitFlagToArray.in=",bigFlag)
  let tmp=1;
  const arr=[];
  while (bigFlag>=tmp){

    if(tmp&bigFlag){
      arr.push(tmp);
    }
    //左移一位
    tmp=tmp<<1
  }
  // console.log("bitFlagToArray.out=",arr)
  return arr;
}

export function bitFlagToStringArray(bigFlag){
  // console.log("bitFlagToStringArray.in=",bigFlag)
  let tmp=1;
  const arr=[];
  while (bigFlag>=tmp){

    if(tmp&bigFlag){
      arr.push(tmp+'');
    }
    //左移一位
    tmp=tmp<<1
  }
  // console.log("bitFlagToStringArray.out=",arr)
  return arr;
}

export function arrayToBitFlag(arr){
  // console.log("arrayToBitFlag.in=",arr)
  let tmp=0;
  arr.forEach((obj)=>{
    tmp=tmp|parseInt(obj);
  })
  // console.log("arrayToBitFlag.out=",tmp)
  return tmp;
}

/**
 * bitFlagAndNumber(8,1) return false;
 * 与运算
 * @param bitFlag
 * @returns {boolean}
 */
export function bitFlagAndNumber(bitFlag,num){
  if(bitFlag&&num){
    return true;
  }
  return false;
}
/**
 * 添加日期范围,
 * @param params
 * @param dateRange    时间范围
 * @param propName     属性名,最后会以 begin+属性名为 params名称
 * @returns {*}
 */
export function addDateRangeToParams(params, dateRange, propName) {
  const search = params;
  search.params = {};
  if (null != dateRange && '' != dateRange) {
    let startTime=dateRange[0]
    let endTime=dateRange[1]
    if(startTime!=null){
      if (startTime.length == 10) {
        startTime=startTime+" 00:00:00"
      }
    }
    if(endTime!=null){
      if (endTime.length == 10) {
        endTime=endTime+" 23:59:59"
      }
    }
    if (typeof(propName) === "undefined") {
      search.params["beginTime"] = startTime;
      search.params["endTime"] = endTime;
    } else {
      search.params["begin"+propName] = startTime;
      search.params["end"+propName] = endTime;
    }
  }
  return search;
}

/**
 * @param num
 * @returns  true:num为空或为0,falsenum不为空且不为0:
 */
export function numberIsZeroOrNull(num){
  if(num === undefined || num === null || num === 0){
      // console.log("numberIsZeroOrNull true",num)
    return true
  }
  if(num){
      // console.log("numberIsZeroOrNull false",num)
      return false;
  }
  return true;
  // return num?true:false;
}

/**
 *
 * @param num
 * @returns  true:num不为空且不为0,false:num为空或为0
 */
export function numberIsNotZeroOrNull(num){
  return !numberIsZeroOrNull(num);
}

/**
 * 查找list中对应字段相同的row
 * @param list
 * @param attrName      list中row的字段名
 * @param value         要查找的value值
 * @param typeEqual     类型是否一致,false:== , true:===
 * @returns {undefined|*}
 */
export function findRowInListFromAttrName(list,attrName,value,typeEqual=false){
  if (value == null) {
    return undefined;
  }
  for (let i = 0; i < list.length; i++) {
    if(typeEqual){
      if (list[i][attrName] === value) {
        return list[i]
      }
    }else{
      if (list[i][attrName] == value) {
        return list[i]
      }
    }
  }
  return undefined;
}

/**
 * 根据时间范围计算间隔秒数 并转化为N小时N天N秒
 * @param startTime 如果为空则返回空字符
 * @param endTime  如果为空则用当前时间代替
 */

export function formatRangeToTimeConsuming(startTimeIn,endTimeIn){
  if (startTimeIn == undefined) {
    return ''
  }
  const startTime = parseInt(new Date(startTimeIn).getTime()/1000)
  let endTime
  if(endTimeIn==undefined){
    endTime=parseInt(new Date().getTime()/1000);
  }else{
    endTime= parseInt(new Date(endTimeIn).getTime()/1000)
  }
  const tmp=endTime-startTime;
  return formatSecondToTimeConsuming(tmp)
}

/**
 * 秒数转化为N小时N天N秒
 * @param timestamp 如果为空则返回空字符
 * @returns {string}
 */
export function formatSecondToTimeConsuming(timestamp){
  if (timestamp == undefined) {
    return ''
  }
  let tmp=timestamp;
  let secStr='';
  if( (tmp % 60)>0){
    secStr=(tmp % 60)+ '秒';
  }
  //取整
  tmp=parseInt(tmp/60)
  if (tmp == 0) {
    return secStr;
  }

  let minStr ='';
  if((tmp % 60)>0){
    minStr =(tmp % 60)+ '分';
  }
  //取整
  tmp=parseInt(tmp/60)
  if (tmp == 0) {
    return minStr+secStr;
  }

  let hourStr ='';
  if((tmp % 24)>0){
    hourStr =(tmp % 24)+ '时';
  }

  //取整
  tmp=parseInt(tmp/24)
  if (tmp == 0) {
    return hourStr+(minStr||secStr);
  }


  let dayStr =tmp+'天';


  return dayStr+(hourStr||minStr||secStr)

}

/**
 * 关系解析,  比如 obj={a:{b:{c:'sks'}}}
 * 只需要 relationshipParserToString(obj,"a.b.c") 就可以获得字符串 sks
 * @param obj
 * @param propName
 */
export function relationshipParserToString(obj,relationshipName){
  if(obj===undefined||obj.constructor !== Object){
    return null;
  }
  let split = relationshipName.split(".")

  let item = obj
  for (let i in split) {
    const propName=split[i];
    if(isNullOrUndefined(item[propName])){
      return undefined;
    }
    item=item[propName];
  }
  return item;

}

/**
 * 格式化ossUrl
 * @param url
 * @param spm
 * @param suffix
 * @return {string|*}
 */
export function formatOssUrl({url,spm=true,suffix=''}){
  if(spm===false||suffix===undefined||url===undefined||url==''){
    return url;
  }
  //url中存在?,直接返回
  if (url.indexOf('?') > -1) {
    return url;
  }

  //后缀中存在 ?, 直接拼接后返回
  if (suffix.indexOf('?') > -1) {
    return url+suffix;
  }
  return url+'?'+suffix

}

/**
 * 变量类型获取
 * @param o
 * @return {string}
 */
export function constructorToLabel(o){
  if (o === null) {
    return 'null'
  }
  if (o === undefined) {
    return 'undefined'
  }
  if(o.constructor === Number){
    return 'Number'
  }else if(o.constructor === String){
    return 'String'
  }else if(o.constructor === Array){
    return 'Array'
  }else if(o.constructor === Object){
    return 'Object'
  }else{
    console.log("constructorToLabel",o)
    return '方法暂未识别该类型'
  }
}

/**
 * 删除文件名的路径
 *
 * 有的浏览器会存在 C:\desktop\aaa.jpg 所以需要格式化成 aaa.jpg
 * /tmp/aaa.jpg  格式化成 aaa.jpg
 */
export function formatFileNameDeletePath(name){
  let tmp=name;
  // console.log("tmp1",tmp)
  let i= Math.max(tmp.lastIndexOf("\\"),tmp.lastIndexOf("/"))
  if(i>-1){
    tmp=tmp.substring(i+1);
    return tmp;
  }
  // console.log("tmp2",tmp)
  return tmp;
}

/**
 * 显示 9MB337KB688B
 * @param value
 * @param powerIn
 * @return {string}
 */
export function formatFileSizeDetail(value, powerIn) {
  const UNITS= ['B', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB']
  let power;
  if(powerIn===undefined){
    power=0;
  }else{
    power=powerIn
  }
  if(power>=UNITS.length){
    return ""
  }
  let i=parseInt(value/1024)
  let j=(value%1024)
  if(j==0){
    return this.formatFileSizeDetail(i,power+1);
  }else{
    return this.formatFileSizeDetail(i,power+1)+j+UNITS[power]
  }
}


export function formatFileSize(value, powerIn) {
  const UNITS= ['B', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB']
  let power;
  if(powerIn===undefined){
    power=0;
    let i=value
    while ((i/1024) > 1){
      i=parseInt(i/1024)
      ++power;
    }
  }else{
    if(powerIn.constructor !== Number){
      power=UNITS.indexOf(powerIn.toUpperCase());
      if(power==-1){
        throw new Error("无效的第二参数,请使用以下数组任何一个",UNITS)
      }
    }else{
      power=powerIn
    }
  }
  return (value / Math.pow(1024, power)).toFixed(2) + UNITS[power];
}

function readFileSizeUnit (value) {
  const UNITS= ['B', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB']
  value = parseFloat(value, 10);
  for (let i = 0; i < UNITS.length; i++) {
    if (value < Math.pow(1024, i)) {
      if (UNITS[i - 1]) {
        return this.format(value, i - 1);
      }
      return value + UNITS[i];
    }
  }
  return this.format(value, i - 1);
}

/**
 * searchValueListSearch 根据空格键分割成数组
 * @param searchValueListSearch
 * @return {null|string}
 */
export function searchValueListSearchToSearchValueList(searchValueListSearch) {
  if (!this.searchValueListSearch) {
      return null;
  }
  let tmp=this.resetBlank(searchValueListSearch);
  return tmp.split(" ")
}

/**
 * 获取当前时间
 * @return {string}   yyyy-MM-dd HH:mm:ss 格式
 */
export function getNowDate(){
    return parseTime(new Date())
}
