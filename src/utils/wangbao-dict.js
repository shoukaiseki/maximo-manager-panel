


//字典
export function getProTypeOptions(){
    let dictData = [
        {
            dictLabel: "生产计划",
            dictValue: "sell",
        },
        {
            dictLabel: "厂内订单",
            dictValue: "inside",
        },
        {
            dictLabel: "子计划",
            dictValue: "child",
        },
        {
            dictLabel: "售后订单",
            dictValue: "afterSale",
        },
        {
            dictLabel: "外加工订单",
            dictValue: "outside",
        },
        {
            dictLabel: "其他",
            dictValue: "other",
        },
        // disabled: true,
    ];
    return dictData;
}


/**
 * 0:完成,1:未完成
 * @returns {string}
 */
export function formatStatusColumn(value,type){
    if(!type||value === undefined){
     return ""
    }
    if(type===1){
        if(value==0) return "未归档"
        if(value==1) return "已归档"
        return ""
    }

    if(type===2){
        if(value==0) return "启用"
        if(value==1) return "禁用"
        return ""
    }

    if(type===3){
        if(value==0) return "已生效"
        if(value==1) return "未生效"
        return ""
    }

    if(type===4){
        if(value==0) return "已提交"
        if(value==1) return "未提交"
        return ""
    }


    if(type===5){
        if(value==0) return "已审核"
        if(value==1) return "待审核"
        return ""
    }
    return "type 未定义";
}

/**
 * 1:是,0:否
 * @returns {{dictValue: number, dictLabel: string}[]}
 */
export function yesOneNoZeroNumber(){
    const data= [
        {
            dictValue: 0 ,
            dictLabel: '否',
        },
        {
            dictValue: 1 ,
            dictLabel: '是',
        },
    ]
    return data;
}

/**
 * 0:完成,1:未完成
 * @returns {[{dictValue: number, dictLabel: string}, {dictValue: number, dictLabel: string}]}
 */
export function finishZeroNoFinishOneNumber(){
    const data= [
        {
            dictValue: 0 ,
            dictLabel: '完成',
        },
        {
            dictValue: 1 ,
            dictLabel: '未完成',
        },
    ]

    return data;
}



/**
 * 第一个元素位置插入字典  全部
 * @param dictList
 * @returns {*}
 */
export function addShowAllToFirst(dictList){
    dictList.unshift({dictValue:"",dictLabel:"全部"})
    return dictList
}

/**
 * 字典的值类型变更为整数类型
 * @param dictList
 * @return {*}
 */
export function dictValueTypeToInteger(dictList){
    for (let row of dictList) {
        row.dictValue=Number(row.dictValue)
    }
    return dictList;
}

/**
 *
 * @param dictList
 * @return {*}
 */
export function dictValueToSelectOptions(dictList,valToInteger=false){
    for (let row of dictList) {
        if(valToInteger){
            row.dictValue=Number(row.dictValue)
            row.value=Number(row.dictValue)
            row.label=row.dictLabel
        }else{
            row.value=row.dictValue
            row.label=row.dictLabel
        }
    }
    return dictList;
}


// export function  addCompanyBitType1Options(list){
//     let tmp= {
//         dictValue: 32,
//         dictLabel: "内销",
//     }
//     list.push(tmp)
//     tmp= {
//         dictValue: 64,
//         dictLabel: "外销",
//     }
//     list.push(tmp)
// }

export function  getCompanyType2Options(type=1){
    const list=[]
    let tmp
    if(type&1){
        tmp= {
            dictValue: 1,
            dictLabel: "供应商",
        }
        list.push(tmp)
        tmp= {
            dictValue: 2,
            dictLabel: "客户",
        }
        list.push(tmp)
        tmp= {
            dictValue: 4,
            dictLabel: "外加工",
        }
        list.push(tmp)
    }
    if(type&32){
        tmp= {
            dictValue: 32,
            dictLabel: "内销",
        }
        list.push(tmp)
        tmp= {
            dictValue: 64,
            dictLabel: "外销",
        }
        list.push(tmp)
    }
    if(type&128){
        tmp= {
            dictValue: 128,
            dictLabel: "自制",
        }
        list.push(tmp)
        tmp= {
            dictValue: 256,
            dictLabel: "采购",
        }
        list.push(tmp)
    }
    return list
}

export function formatInventoryBomType(value){
    let list = getInventoryBomTypeOptions()
    return this.selectDictLabel(list, value);
}

export function  getInventoryBomTypeOptions(type=1){
    let dictData = [
        {
            dictValue:"default",
            dictLabel:"默认"
        },{
            dictValue:"poCheck",
            dictLabel:"采购验收入库"
        },{
            dictValue:"proCheck",
            dictLabel:"生产验收入库"
        },{
            dictValue:"source",
            dictLabel:"生产计划物料清单"
        },{
            dictValue:"target",
            dictLabel:"生产计划目标产品"
        },{
            dictValue:"sellReceive",
            dictLabel:"成品入库"
        },{
            dictValue:"proHeadScrap",
            dictLabel:"机头废料"
        },{
            dictValue:"proTailScrap",
            dictLabel:"机尾废料"
        },{
            dictValue:"proRawScrap",
            dictLabel:"原纸废料"
        }
    ];
    return dictData;
}
