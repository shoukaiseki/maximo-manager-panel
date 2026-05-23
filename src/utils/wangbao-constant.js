//常量
//工序状态

export const LOCATION_CONSTANTS={
    INVENTORY_LOCATION_SELL: "sellLocation",
    INVENTORY_CONTAINER_DEFAULT: "defaultContainer",
}

export const PRO_PROCESS_STATUS={
    //0,"未开始",10,""),
    DEFAULT: 0,
    // 1,"已完成",20,""),
    FINISH:1,
    //2, "进行中",30,""),
    NO_FINISH:2,
    // 4,"返工",40,""),
    REWORK:4,
    // 8,"待运送",50,""),
    TRANSPORT:8,
}

//业务名称呢个默认后缀
export const BUSINESS_NAME_SUFFIX={
    //销售单
    soName: "销售单",
    //采购单
    poName: "采购单",
    //生产计划
    proNameSell: "生产计划",
    proNameWO: "任务工单",
    proNameReworkOrder: "返工单",
    inside2: 'inside2',
}

//物资特征
export const INVENTORY_ITEM_ITEM_FEATURE={
    sell: 'sell',
    use: 'use',
    raw: 'raw',
}
export const INVENTORY_ORDER_PRINT_BIT_TYPE={
    proInside: 1,
}

/**
 * 变更标志,
 * @type {{toBeDelete: 1, toBeAdd: 2, toBeModify: 3}}
 */
export const CHANGE_BIT_FLAG={
    //无变更
    none: 0,
    //   &1:  新增 toBeAdd()    判断 true:需新增
    add: 1,
    //   &2:  修改 toBeModify() 判断 true:需更新
    modify: 2,
    //   &4:  修改 toBeDelete() 判断 true:需删除
    delete: 4
}

export const INVENTORY_BOM_BOM_TYPE={
    //销售发货单
    soDelivery: 'soDelivery',
    so: 'so',
    idr: 'idr',
    idd: 'idd',
}

export const SALE_ORDER_BOM_TYPE={
    //销售发货单
    soDelivery: 'soDelivery',
    so: 'so',
}

export const PRODUCTION_ORDER_BOM_TYPE={
    source: 'source',
    target: 'target',
}

export const BUSINESS_NAME= {
    invoice: 'invoice',
    company: 'company',
    saleOrderDelivery: 'sale_order_delivery',

}

export const BUSINESS_TYPE= {
    proPlan: "proPlan",
    procedure: "procedure",
    //销售发票
    invoiceType: 'invoiceSell',
    proTaskOrderInside: 'inside',
    proTaskOrderInside2: 'inside2',
    proProcedureTask: 'procedureTask',
    proReworkOrder: 'reworkOrder',
}

export const BUSINESS_CONSTANTS={
    /**
     * 采购单
     */
    OWNER_TYPE_PURCHASE_ORDER : "PO",
    /**
     * 采购单行
     */
    OWNER_TYPE_PURCHASE_ORDER_LINE : "POLine",
    /**
     * 到货通知检查验收
     */
    OWNER_TYPE_PURCHASE_ORDER_DELIVERY : "PODelivery",
    /**
     * 到货通知检查验收单行
     */
    OWNER_TYPE_PURCHASE_ORDER_DELIVERY_CHECK : "PODeliveryCheck",
    /**
     * 到货通知检查验收单行
     */
    OWNER_TYPE_PURCHASE_ORDER_DELIVERY_CHECK_BOM : "PODeliveryCheckBom",
    /**
     * 到货通知检查验收单行出入库历史记录
     */
    OWNER_TYPE_PURCHASE_ORDER_DELIVERY_CHECK_BOM_HISTORY : "PODeliveryCheckBomHistory",

    /**
     *  生产计划
     */
    OWNER_TYPE_PRODUCTION_ORDER : "PRO",


    /**
     *  生产计划 sourceBom
     */
    OWNER_TYPE_PRODUCTION_ORDER_SOURCE_BOM : "PROSourceBom",


    /**
     *  生产计划 sourceBom 出入库历史记录
     */
    OWNER_TYPE_PRODUCTION_ORDER_SOURCE_BOM_HISTORY : "PROSourceBomHistory",

    //默认bom
    OWNER_TYPE_DEFAULT_INVENTORY_BOM:"inventory_bom",//"defaultInventoryBom",

    //检查验收单行
    OWNER_TYPE_DEFAULT_INVENTORY_BOM_CHECK:"inventory_bom_check",//"defaultInventoryBomCheck",

    //默认库存历史记录
    OWNER_TYPE_DEFAULT_HISTORY:"inventory_history",//"defaultInventoryHistory",

    //默认库房
    OWNER_TYPE_DEFAULT_INVENTORY_BALANCE:"inventory_balance",//"defaultInventoryBalance",

}

