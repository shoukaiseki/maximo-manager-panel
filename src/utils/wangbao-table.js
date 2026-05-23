import { generateUUID } from '@/utils/wangbao'
import { addNullIsZero, subtractNullIsZero,divideNullIsZero } from '@/utils/wangbao-number-math'
import { updateRole } from '../api/system/role'
import { CHANGE_BIT_FLAG } from '@/utils/wangbao-constant'

export function initListVueVirtualUUID(list){
  if(list == undefined){
    return [];
  }
  list.forEach((item,i)=>{
    item.vueVirtualUUID=generateUUID();
    item.isSet=false;
  })
  return list;
}


/**
 * 删除list中的 vueVirtualUUID 的行
 * 用于新增之后主键无值,row的key不能用list中的索引为,避免多个table时报错
 * 每个row数据时默认设置 vueVirtualUUID 的值为 生成的UUID
 * @param list
 * @param row
 */
export function deleteFromVueVirtualUUID(list,vueVirtualUUID) {
  // 1.根据ID来找到要删除的这一项的索引
  // 2. 找到索引后，调用数组的splice方法
  // 方法一
  list.some((item,i)=>{
    if(item.vueVirtualUUID==vueVirtualUUID){
      list.splice(i,1);
      // 在数组的some方法中，如果return true，就会立即终止这个数组的后续循环,所以相比较foreach，如果想要终止循环，那么建议使用some
      return true;
    }
  })
}


/**
 * 获取变更的行集合,原list不受影响,会生成新的集合
 * @param listIn
 * @returns  Array
 */
export function getNewChangeRowList(listIn){
  if(listIn==undefined||listIn.constructor != Array||listIn.length==0){
    if(listIn.constructor != Array){
      console.error("listIn=",listIn)
      throw new Error("listIn不是Array类型")
    }
    return [];
  }

  let list=listIn.filter ( item =>{
    //   &1:  新增 toBeAdd()    判断 true:需新增
    //   &2:  修改 toBeModify() 判断 true:需更新
    //   &4:  修改 toBeDelete() 判断 true:需删除
    if (item.virtualChangeBitFlag === undefined || item.virtualChangeBitFlag == 0) {
      return false;
    }
    return true;
  })
  return list;
}

/**
 * 获取变更的行集合,原list不受影响,会生成新的集合
 * @param xTable      vxe中的 table ref
 * @returns  Array
 */
export function getVxeChangeRowList(xTable){
  let list=[]
  for (let updateRecord of xTable.getUpdateRecords()) {
    updateRecord.virtualChangeBitFlag=CHANGE_BIT_FLAG.modify
    list.push(updateRecord)
  }
  for (let insertRecord of xTable.getInsertRecords()) {
    insertRecord.virtualChangeBitFlag=CHANGE_BIT_FLAG.add
    list.push(insertRecord)
  }
  for (let removeRecord of xTable.getRemoveRecords()) {
    removeRecord.virtualChangeBitFlag=CHANGE_BIT_FLAG.delete
    list.push(removeRecord)
  }
  return list;
}

/**
 *  数组中的某个属性累加,如果该属性为 null 则当作0处理
 * @param list
 * @param propName
 * @returns {number}
 */
export function sumArrayProperty(list,propName){
  if(list == undefined||list.length==0){
    return 0.0;
  }
  let val=0.0;
  for (let ind in list) {
    const item=list[ind]
    if (item[propName] == undefined) {
      continue;
    }
    let tmp=item[propName]
    if (typeof tmp !== 'number') {
      throw new Error(`[${propName}]不是数字类型`)
    }
    val=val+tmp
  }
  return val;
}

/**
 * 获取主键集合
 * @param list        数组,元素为 Object(map)
 * @param propName    元素中要获取的属性名
 * @returns {[]|*[]}   空数组或者包含 元素属性的数组
 */
export function getNumberListFromTableList(list,propName){
  if(list==null){
    return []
  }
  const idList=[]
  for (let item of list) {
    if (item !== undefined && item[propName]!==undefined) {
      idList.push(item[propName]);
    }
  }

  return idList;

}

/**
 * 获取某个属性的集合
 * @param list        数组,元素为 Object(map)
 * @param propName    元素中要获取的属性名
 * @param unique      true:不重复
 * @returns {[]|*[]}   空数组或者包含 元素属性的数组
 */
export function getPropListFromTableList(list,propName,unique=true){
  if(list==null){
    return []
  }
  const idList=[]
  for (let item of list) {
    if (item !== undefined && item[propName]!==undefined) {
        if(unique){
          if(item[propName].indexOf(idList,0)==-1){
            idList.push(item[propName]);
          }
        }else{
          idList.push(item[propName]);
        }
    }
  }

  return idList;

}

/**
 * 平均分配
 * @param list              要平分的列表数据
 * @param propName          要平分的属性名
 * @param totalValue        要平分的总计值
 */
export function equalDistributionToList({list,propName,totalValue}){
  if(totalValue===undefined||list===undefined||list.length===0){
    return;
  }

  let lineCost = parseFloat(totalValue)
  const unitCost=divideNullIsZero(lineCost,list.length,2)
  let totalLineCost=0;
  for (let i = 0; i < list.length; i++) {
    const item=list[i]
    let itemLineCost=unitCost;

    if(addNullIsZero(itemLineCost,totalLineCost)>lineCost){
      itemLineCost=subtractNullIsZero(lineCost,totalLineCost)
    }
    if(i+1==list.length){
      itemLineCost=subtractNullIsZero(lineCost,totalLineCost)
    }
    item[propName]=itemLineCost;
    totalLineCost=addNullIsZero(totalLineCost,itemLineCost)
  }

}


export function rowStyleElTable(params){
    const {row,rowIndex}=params
    return rowStyle(params)
}

/**
 * table中根据新增/修改/删除 变色
 * @param row
 * @param rowIndex
 * @return {{}}
 */
export function rowStyle({ row, rowIndex }) {
    let styleJson = {}
    //优先判断删除
    if (row.virtualChangeBitFlag & 4) {
        styleJson = {
            'text-decoration': 'line-through',
            'margin-top': '61px',
            'background-color': '#FFE4B5',
        }
        return styleJson
    }

    if (row.virtualChangeBitFlag & 1) {
        styleJson = {
            'background-color': '#D1EEEE',
        }
    } else if (row.virtualChangeBitFlag & 2) {
        styleJson = {
            'background-color': '#C5C1AA',
        }
    } else {
        styleJson = {}
    }
    // console.log("row=",row)
    // console.log("virtualChangeBitFlag=",row.virtualChangeBitFlag)
    // console.log("styleJson=",styleJson)
    // 返回对象
    return styleJson
}

/**
 * tableList设置 virtual属性,
 * @param tableList
 * @param tablePropName
 * @param RelationshipListPropName
 */
export function tableListSetVirtualDomainByRelationshipList({tableList,tablePropName,RelationshipListPropName}){

}
