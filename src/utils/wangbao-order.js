/**
 * 订单管理通用方法
 */
import CHANGE_BIT_FLAG from '/src/utils/wangbao-constant'
import { calcNotTaxCost, calcTaxCost, divideNullIsZero, multiplyNullIsZero } from '/src/utils/wangbao-number-math'

/**
 * 更新 bom 的公司Id
 * @param num
 * @return {string|null}
 */
export function updateInventoryBomListCompany(bomList,companyId,companyName) {
    for (let bom of bomList) {
        // console.log("companyId,bom.companyId=",companyId,bom.companyId)
        // console.log("companyName=",companyName)
        if (companyId !== bom.companyId) {
            bom.companyId=companyId
            bom.companyName=companyName
            if(bom.virtualChangeBitFlag==null||bom.virtualChangeBitFlag==0){
                //如果该子表无变更类型则设变更类型为修改
               bom.virtualChangeBitFlag=CHANGE_BIT_FLAG.modify
            }
        }
    }
}

/**
 *  bom 根据含税单位成本和数量计算的含税行成本
 * @param bom
 */
export function calcBomTaxLineCostByTaxUnitCost(bom){
    let lineCost=bom.lineCost
    let unitCost=bom.unitCost
    let taxUnitCost=bom.taxUnitCost
    let taxLineCost=bom.taxLineCost
    let quantity=bom.quantity
    let taxRate=bom.taxRate
    bom.taxLineCost=multiplyNullIsZero(taxUnitCost,quantity)
}

/**
 *  bom 根据不含税行成本计算的含税行成本
 * @param bom
 */
export function calcBomTaxLineCostByLineCost(bom){
    let lineCost=bom.lineCost
    let unitCost=bom.unitCost
    let taxUnitCost=bom.taxUnitCost
    let taxLineCost=bom.taxLineCost
    let quantity=bom.quantity
    let taxRate=bom.taxRate
    bom.taxLineCost=calcTaxCost(lineCost,taxRate)
}

/**
 *  bom 根据含税行成本计算的含税单位成本
 * @param bom
 */
export function calcBomTaxUnitCostByTaxLineCost(bom){
    let lineCost=bom.lineCost
    let unitCost=bom.unitCost
    let taxUnitCost=bom.taxUnitCost
    let taxLineCost=bom.taxLineCost
    let quantity=bom.quantity
    let taxRate=bom.taxRate
    bom.taxUnitCost=divideNullIsZero(taxLineCost,quantity)
}

/**
 *  bom 根据数量单价计算的含税行成本
 * @param bom
 */
export function calcBomLineCostByQuantityAndUnitCost(bom){
    let lineCost=bom.lineCost
    let unitCost=bom.unitCost
    let taxUnitCost=bom.taxUnitCost
    let taxLineCost=bom.taxLineCost
    let quantity=bom.quantity
    let taxRate=bom.taxRate
    lineCost=multiplyNullIsZero(unitCost,quantity)
    // console.log(lineCost,"bom",bom)
    bom.lineCost=lineCost
}


/**
 *  bom 根据含税行成本计算不含税行成本
 * @param bom
 */
export function calcBomLineCostByTaxLineCost(bom){
    let lineCost=bom.lineCost
    let unitCost=bom.unitCost
    let taxUnitCost=bom.taxUnitCost
    let taxLineCost=bom.taxLineCost
    let quantity=bom.quantity
    let taxRate=bom.taxRate
    lineCost=calcNotTaxCost(taxLineCost,taxRate)
    console.log("calcBomLineCostByTaxLineCost.lineCost",lineCost)
    bom.lineCost=lineCost
}


/**
 *  bom 根据不含税行成本计算不含税单位成本
 * @param bom
 */
export function calcBomUnitCostByLineCost(bom){
    let lineCost=bom.lineCost
    let unitCost=bom.unitCost
    let taxUnitCost=bom.taxUnitCost
    let taxLineCost=bom.taxLineCost
    let quantity=bom.quantity
    let taxRate=bom.taxRate
    bom.unitCost=divideNullIsZero(lineCost,quantity)
}

