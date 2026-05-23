import Vue from 'vue'

export function jumpToInventoryReserveHandle(){
    const url='/inventory/InventoryReserveHandle'
    this.$router.push( url)
}

//跳转到生产计划

export function  jumpProductionOrderSell(uniqueId,{newTab=false}={}){
    const url='/proOrder/editProductionOrder/'+uniqueId
    // const url='/proOrder/editOutSideProductionOrder/'+uniqueId

    if(newTab){
        let routeUrl = this.$router.resolve({
            path: url,
        })
        window.open(routeUrl.href, '_blank')
        return
    }

    this.$router.push( url)
}

export function  jumpProductionOrderTemplate(uniqueId,{newTab=false}={}){
    const url='/proOrder/editTemplateProductionOrder/'+uniqueId

    if(newTab){
        let routeUrl = this.$router.resolve({
            path: url,
        })
        window.open(routeUrl.href, '_blank')
        return
    }

    this.$router.push( url)
}

export function  jumpProductionOrderOutSide(uniqueId,{newTab=false}={}){
    const url='/proOrder/editOutSideProductionOrder/'+uniqueId

    if(newTab){
        let routeUrl = this.$router.resolve({
            path: url,
        })
        window.open(routeUrl.href, '_blank')
        return
    }

    this.$router.push( url)
}



export function  jumpSaleOrder(uniqueId,{newTab=false}){
    const url='/order/SaleOrder'
    this.$router.push(
        {
            path:url,
            query:{
                saleOrderId: uniqueId,
            }
        }
    )
}
// ,{newTab=false}
export function  jumpSaleOrderDelivery(uniqueId){
    const url='/order/SaleOrderDelivery'
    this.$router.push(
        {
            path:url,
            query:{
                saleOrderDeliveryId: uniqueId,
            }
        }
    )
}

export function  jumpPurchaseOrder(uniqueId,{newTab=false}){
    const url='/purchaseOrder/editPurchaseOrder/'+uniqueId
    if(newTab){
        let routeUrl = this.$router.resolve({
            path: url,
        })
        window.open(routeUrl.href, '_blank')
        return
    }

    this.$router.push( url)
}

