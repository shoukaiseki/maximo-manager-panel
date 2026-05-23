/**
 * 订单管理通用方法
 */
import CHANGE_BIT_FLAG from '/src/utils/wangbao-constant'
import { findRowInListFromAttrName } from './wangbao'

/**
 * 格式化 历史变更类型
 * @param num
 * @return {string|null}
 */
export function changeInventoryTypeFormat(changeTypeOptions,value) {
    const exactActions = [];
    const likeActions = [];
    Object.keys(changeTypeOptions).some((key) => {
        if ((changeTypeOptions[key].dictValue) == (Number(value)&(~3))) {
            likeActions.push(changeTypeOptions[key].dictLabel);
        }
        if ((changeTypeOptions[key].dictValue) == ('' + value)) {
            exactActions.push(changeTypeOptions[key].dictLabel);
            return true;
        }
    })
    // console.log("exactActions",exactActions)
    // console.log("likeActions",likeActions)
    if(exactActions.length==0){
        if(likeActions.length>0){
            if (value & 2) {
                likeActions.push((value&1?'退回':'退库'))
            }else{
                likeActions.push((value&1?'发放':'入库'))
            }

        }
        return likeActions.join('')
    }

    return exactActions.join('');
}


//如果itemCompanyCost存在则设置bom的默认含税/不含税价格
export function autoSettingBomCost(itemCompanyCost,bom){
    if(!itemCompanyCost){
        return;
    }
    bom.taxRate=itemCompanyCost.taxRate
    if (itemCompanyCost.taxUnitCost) {
        bom.taxUnitCost=itemCompanyCost.taxUnitCost
        bom.taxInputMode=1
        return;
    }
    bom.unitCost=itemCompanyCost.unitCost
    bom.taxInputMode=0
}

export function initItemToDomain(list,itemList,{sourceAttrName: itemIdAttrName = "itemId",virtualName="item"}={}){
    if(!list)return;
    let tmp;
    if (list === Array) {
        tmp=list
    }else{
        tmp=[list]
    }
    for (let row of list) {
        let item={}
        if (Object.prototype.hasOwnProperty.call(row, itemIdAttrName)) {
            if(row[itemIdAttrName]){
                let itemTmp=findRowInListFromAttrName(itemList,"itemId",row.itemId)
                if(itemTmp){
                    item=itemTmp
                }
            }
        }
        row[virtualName]=item
    }
}

