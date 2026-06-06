import Vue from 'vue'

import { systemConfig } from '/src/systemConfig'

Vue.prototype.systemConfig= systemConfig

import Cookies from 'js-cookie'

import 'normalize.css/normalize.css' // a modern alternative to CSS resets

import Element from 'element-ui'
import './assets/styles/element-variables.scss'
import '@/assets/styles/index.scss' // global css
import '@/assets/styles/ruoyi.scss' // ruoyi css
import App from './App'
import store from './store'
import router from './router'

import '/src/components/AutumnFilterParam'

// autumn
import '/src/components/autumn/index.js'

import './assets/icons' // icon
import './permission' // permission control
import { parseTime, resetForm, addDateRange, selectDictLabel, selectDictLabels, download, handleTree } from "/src/utils/ruoyi";
import Pagination from "/src/components/Pagination";
//自定义表格工具扩展
import RightToolbar from "/src/components/RightToolbar"




// 全局方法挂载,每个组建原型拥有该属性方法
//在组建中使用 this.checkRole(['admin']); 调用


// 全局方法挂载
Vue.prototype.parseTime = parseTime
Vue.prototype.resetForm = resetForm
Vue.prototype.addDateRange = addDateRange
Vue.prototype.selectDictLabel = selectDictLabel
Vue.prototype.selectDictLabels = selectDictLabels
Vue.prototype.download = download
Vue.prototype.handleTree = handleTree

/**
 * @param msg                         提示内容
 * @param dangerouslyUseHTMLString    true:<br>换行
 */
Vue.prototype.msgSuccess = function (msg,dangerouslyUseHTMLString=false) {
    this.$message({ dangerouslyUseHTMLString: dangerouslyUseHTMLString,showClose: true, message: msg||'操作成功', type: "success" });
}

Vue.prototype.msgError = function (msg,dangerouslyUseHTMLString=false) {
    this.$message({ dangerouslyUseHTMLString: dangerouslyUseHTMLString,showClose: true, message: msg, type: "error" });
}

Vue.prototype.msgInfo = function (msg) {
    this.$message.info(msg);
}

// 全局组件挂载
Vue.component('Pagination', Pagination)
Vue.component('RightToolbar', RightToolbar)


/**
 * If you don't want to use mock-server
 * you want to use MockJs for mock api
 * you can execute: mockXHR()
 *
 * Currently MockJs will be used in the production environment,
 * please remove it before going online! ! !
 */

Vue.use(Element, {
    size: Cookies.get('size') || 'mini' // set element-ui default size
})


Vue.config.productionTip = false

new Vue({
    el: '#app',
    router,
    store,
    render: h => h(App)
})




//el-dialog 拖拽
import '@/directive/component/dialogDrag'
import 'xe-utils'
import VXETable from 'vxe-table'
import 'vxe-table/lib/style.css'
Vue.use(VXETable)
VXETable.setup({
    size:Cookies.get('size') || 'medium'
})

import VXETablePluginElement from 'vxe-table-plugin-element'
import 'vxe-table-plugin-element/dist/style.css'
VXETable.use(VXETablePluginElement)

import WbElSwitch from '/src/components/Switch/WbElSwitch'
Vue.component('WbElSwitch', WbElSwitch)

//全局注册自定义组件
import CheckboxMultipleToBitValue from '/src/components/SelectMultipleToBitValue/CheckboxMultipleToBitValue'
Vue.component('CheckboxMultipleToBitValue', CheckboxMultipleToBitValue)

import SelectMultipleToBitValue from '/src/components/SelectMultipleToBitValue/SelectMultipleToBitValue'
Vue.component('SelectMultipleToBitValue', SelectMultipleToBitValue)

import SelectMultipleLookupTable from '/src/components/SelectMultipleLookupTable'
Vue.component('SelectMultipleLookupTable', SelectMultipleLookupTable)

import StatusSwitch from '/src/components/DataRowStatus/StatusSwitch'
Vue.component('StatusSwitch', StatusSwitch)

import UrgentedSwitch from '/src/components/DataRowStatus/UrgentedSwitch'
Vue.component('UrgentedSwitch', UrgentedSwitch)

import StatusSelect from '/src/components/DataRowStatus/StatusSelect'
Vue.component('StatusSelect', StatusSelect)

// import SksCheckbox from '/src/components/SksCheckbox/SksCheckbox'
// Vue.component('SksCheckbox', SksCheckbox)

import SysUserNickNameInput from '/src/components/SysUserNickName/SysUserNickNameInput'
Vue.component('SysUserNickNameInput', SysUserNickNameInput)


// import { isNumberInRuleValidate,isIntegerInRuleValidate,isPositiveIntegerInRuleValidate,isPositiveIntegerAndNotZeroInRuleValidate } from '/src/utils/sks-validate'
import {
    searchValueListSearchToSearchValueList,
    formatOssUrl,
    resetBlank,
    resetProcessNodeListSort,
    resetProcessNodeConfListSort,
    generateUUID,
    canEditProcessNode,
    getNowDate, relationshipParserToString, isNullOrUndefined
} from '/src/utils/sks'
import {numberIsNotZeroOrNull,numberIsZeroOrNull,mergeFromObject,extendObject,arrayToBitFlag,bitFlagToArray,bitFlagToStringArray} from "/src/utils/sks";



import { getCompanyByCompanyId,getCompanyNameByCompanyId } from '/src/utils/sks-lookup-format'

import { inputRestrictedDecimal } from '/src/utils/sks-input-restricted'

import { negateNumber } from '/src/utils/sks-number-utils'
import { rowStyle } from '/src/utils/sks-table'
import { aqoeous, aqoeousNoToQueryFunction } from '/src/api/base/aqoeous.js'
import { syncAction } from '/src/api/base/syncAction.js'
import { changeInventoryTypeFormat, initItemToDomain } from '/src/utils/sks-inventory'
import {
    dictValueToSelectOptions,
    dictValueTypeToInteger, formatInventoryBomType,
    formatStatusColumn
} from '/src/utils/sks-dict'
import { isInArray, isNotInArray } from '/src/utils/sks-array'
import { calcPercentageProgressValue } from './utils/sks-number-math'

Vue.prototype.dictValueToSelectOptions=dictValueToSelectOptions
Vue.prototype.dictValueTypeToInteger=dictValueTypeToInteger
Vue.prototype.isNotInArray=isNotInArray
Vue.prototype.isInArray=isInArray
Vue.prototype.formatStatusColumn=formatStatusColumn
Vue.prototype.changeInventoryTypeFormat=changeInventoryTypeFormat
//查询器
Vue.prototype.aqoeous=aqoeous

//同步触发
Vue.prototype.syncAction=syncAction


Vue.prototype.Constants=Constants

// 全局方法挂载
Vue.prototype.BUSINESS_CONSTANTS=Constants.BUSINESS_CONSTANTS
Vue.prototype.CHANGE_BIT_FLAG=Constants.CHANGE_BIT_FLAG
Vue.prototype.PRODUCTION_ORDER_BOM_TYPE=Constants.PRODUCTION_ORDER_BOM_TYPE

Vue.prototype.formatOssUrl= formatOssUrl;
Vue.prototype.rowStyle= rowStyle;
Vue.prototype.negateNumber= negateNumber;

Vue.prototype.inputRestrictedDecimal= inputRestrictedDecimal;
Vue.prototype.getCompanyByCompanyId= getCompanyByCompanyId;
Vue.prototype.getCompanyNameByCompanyId= getCompanyNameByCompanyId;

Vue.prototype.numberIsNotZeroOrNull= numberIsNotZeroOrNull;
Vue.prototype.numberIsZeroOrNull= numberIsZeroOrNull;
Vue.prototype.mergeFromObject= mergeFromObject;
Vue.prototype.isNullOrUndefined= isNullOrUndefined;


Vue.prototype.extendObject= extendObject;
Vue.prototype.arrayToBitFlag = arrayToBitFlag
Vue.prototype.bitFlagToArray = bitFlagToArray
Vue.prototype.bitFlagToStringArray =bitFlagToStringArray
Vue.prototype.relationshipParserToString =relationshipParserToString
Vue.prototype.calcPercentageProgressValue =calcPercentageProgressValue



Vue.prototype.searchValueListSearchToSearchValueList=searchValueListSearchToSearchValueList
Vue.prototype.resetBlank = resetBlank;

Vue.prototype.resetProcessNodeListSort  = resetProcessNodeListSort
Vue.prototype.resetProcessNodeConfListSort = resetProcessNodeConfListSort
// Vue.prototype.isPositiveIntegerAndNotZeroInRuleValidate = isPositiveIntegerAndNotZeroInRuleValidate
// Vue.prototype.isPositiveIntegerInRuleValidate =isPositiveIntegerInRuleValidate
// Vue.prototype.isIntegerInRuleValidate =isIntegerInRuleValidate
// Vue.prototype.isNumberInRuleValidate =isNumberInRuleValidate
Vue.prototype.generateUUID = generateUUID
Vue.prototype.canEditProcessNode = canEditProcessNode



//默认的存在精度问题 https://www.jianshu.com/p/849b0ae36b36
// 1.35.toFixed(1) // 1.4 正确
// 1.335.toFixed(2) // 1.33  错误
// 1.3335.toFixed(3) // 1.333 错误
// 1.33335.toFixed(4) // 1.3334 正确
// 1.333335.toFixed(5)  // 1.33333 错误
// 1.3333335.toFixed(6) // 1.333333 错误
Number.prototype.toFixed=function (d) {
    var s=this+"";
    if(!d)d=0;
    if(s.indexOf(".")==-1)s+=".";
    s+=new Array(d+1).join("0");
    if(new RegExp("^(-|\\+)?(\\d+(\\.\\d{0,"+(d+1)+"})?)\\d*$").test(s)){
        var s="0"+RegExp.$2,pm=RegExp.$1,a=RegExp.$3.length,b=true;
        if(a==d+2){
            a=s.match(/\d/g);
            if(parseInt(a[a.length-1])>4){
                for(var i=a.length-2;i>=0;i--){
                    a[i]=parseInt(a[i])+1;
                    if(a[i]==10){
                        a[i]=0;
                        b=i!=1;
                    }else break;
                }
            }
            s=a.join("").replace(new RegExp("(\\d+)(\\d{"+d+"})\\d$"),"$1.$2");

        }if(b)s=s.substr(1);
        return (pm+s).replace(/\.$/,"");
    }return this+"";

};

import {
    jumpProductionOrderSell,
    jumpProductionOrderOutSide,
    jumpPurchaseOrder,
    jumpSaleOrderDelivery, jumpSaleOrder, jumpToInventoryReserveHandle, jumpProductionOrderTemplate
} from '/src/utils/sks-jump.js'
Vue.prototype.jumpToInventoryReserveHandle=jumpToInventoryReserveHandle
Vue.prototype.jumpPurchaseOrder=jumpPurchaseOrder
Vue.prototype.jumpProductionOrderOutSide=jumpProductionOrderOutSide
Vue.prototype.jumpProductionOrderSell=jumpProductionOrderSell
Vue.prototype.jumpSaleOrderDelivery= jumpSaleOrderDelivery;
Vue.prototype.jumpSaleOrder= jumpSaleOrder;
Vue.prototype.jumpProductionOrderTemplate= jumpProductionOrderTemplate;


Vue.prototype.getNowDate=getNowDate

//全局用户信息缓存
import '/src/utils/sks-cache'

Vue.prototype.formatInventoryBomType=formatInventoryBomType


import 'font-awesome/css/font-awesome.min.css'

import * as Constants from '/src/utils/sks-constant'

Vue.prototype.initItemToDomain=initItemToDomain


import SuperFlow from 'vue-super-flow'
import 'vue-super-flow/lib/index.css'

Vue.use(SuperFlow)

Vue.prototype.aqoeousNoToQueryFunction=aqoeousNoToQueryFunction
import * as globalConfig from '/src/systemConfig/globalConfig'
Vue.prototype.globalConfig=globalConfig





import ProTaskWorkStatusLabel from  '/src/components/WorkStatusLabel/ProTaskWorkStatusLabel'
Vue.component('ProTaskWorkStatusLabel',ProTaskWorkStatusLabel)


// import * as wb780js from './utils/wb780'
//
// mergeFromObject(Vue.prototype,wb780js)




import  SksPluginElErp from 'sks-plugin-el-erp'
import axios from 'axios'
let request=axios.create({
    // axios中请求配置有baseURL选项，表示请求URL公共部分
    baseURL: process.env.VUE_APP_BASE_API,
    // 超时
    timeout: 10000
})

Vue.use(SksPluginElErp,{
    request: request,
})
