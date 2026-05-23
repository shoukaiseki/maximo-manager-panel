import {
    calcBomLineCostByQuantityAndUnitCost,
    calcBomLineCostByTaxLineCost,
    calcBomTaxLineCostByLineCost, calcBomTaxLineCostByTaxUnitCost, calcBomTaxUnitCostByTaxLineCost,
    calcBomUnitCostByLineCost
} from '/src/utils/wangbao-order'

export function handleChangeBomTaxRateInVxeInput({row}){
    // console.log("handleChangeBomTaxRate",row.taxRate)
    //自动计算不含税价格
    if(row.taxInputMode==1){
        calcBomUnitCostByLineCost(row)
        calcBomLineCostByTaxLineCost(row)
        return
    }
    calcBomTaxLineCostByLineCost(row)
    calcBomTaxUnitCostByTaxLineCost(row)

}
export function handleChangeBomQuantityInVxeInput({row,value,$event,calculationType}){
    // console.log("handleChangeBomQuantity",row.quantity)
    //会触发两次change事件,只取成功的提交
    if($event.isTrusted){
        row.quantity=Number(value)
        // console.log("column",column)

        //自动计算不含税价格
        if(row.taxInputMode==1){

            //自动计算行成本
            if (calculationType == 1) {
                if(!row.taxUnitCost){
                    calcBomTaxUnitCostByTaxLineCost(row)
                }else{
                    calcBomTaxLineCostByTaxUnitCost(row)
                }
            }else{
                if(!row.taxLineCost){
                    calcBomTaxLineCostByTaxUnitCost(row)
                }else{
                    calcBomTaxUnitCostByTaxLineCost(row)
                }
            }
            calcBomLineCostByTaxLineCost(row)
            calcBomUnitCostByLineCost(row)
            return
        }
        if (calculationType == 1) {
            if(!row.unitCost){
                calcBomUnitCostByLineCost(row)
            }else{
                calcBomLineCostByQuantityAndUnitCost(row)
            }
        }else{
            if(!row.lineCost){
                calcBomLineCostByQuantityAndUnitCost(row)
            }else{
                calcBomUnitCostByLineCost(row)
            }
        }
        calcBomTaxLineCostByLineCost(row)
        calcBomTaxUnitCostByTaxLineCost(row)
    }


}
export function handleChangeBomLineCostInVxeInput({row,value,$event}){
    if(row.taxInputMode==1){
        return
    }
    //会触发两次change事件,只取成功的提交
    if($event.isTrusted){
        row.lineCost=Number(value)
        // console.log("column",column)

        //自动计算不含税价格
        if(row.taxInputMode==1){
            return
        }
        //数量不自动计算
        calcBomUnitCostByLineCost(row)
        calcBomTaxLineCostByLineCost(row)
        calcBomTaxUnitCostByTaxLineCost(row)
    }
}
export function  handleChangeBomUnitCostInVxeInput({row,value,$event}){
    if(row.taxInputMode==1){
        return
    }
    //会触发两次change事件,只取成功的提交
    if($event.isTrusted){
        row.unitCost=Number(value)
        // console.log("column",column)
        calcBomLineCostByQuantityAndUnitCost(row)
        calcBomTaxLineCostByLineCost(row )

    }
}
export function handleChangeBomTaxUnitCostInVxeInput({row,value,$event}){
    if(row.taxInputMode==0){
        return
    }
    //会触发两次change事件,只取成功的提交
    if($event.isTrusted){
        row.taxUnitCost=Number(value)
        // console.log("column",column)
        //自动计算不含税价格
        if(row.taxInputMode==1){
            if(row.taxUnitCost) {
                if (row.quantity) {
                    calcBomTaxLineCostByTaxUnitCost(row)
                }
            }
            calcBomLineCostByTaxLineCost(row)
            calcBomUnitCostByLineCost(row)
            return
        }
    }
}
export function handleChangeBomTaxLineCostInVxeInput({row,value,$event}){
    if(row.taxInputMode==0){
        return
    }
    //会触发两次change事件,只取成功的提交
    if($event.isTrusted){
        row.taxLineCost=Number(value)
        // console.log("column",column)
        if(row.taxInputMode==1){
            if(row.taxLineCost){
                if(row.quantity){
                    calcBomTaxUnitCostByTaxLineCost(row)
                }
            }
            calcBomLineCostByTaxLineCost(row)
            calcBomUnitCostByLineCost(row)
            return ;

        }
    }

}
