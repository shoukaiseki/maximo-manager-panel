import request from '/src/utils/request'

// 组建的 created 中调用以下方法获取系统配置
// this.globalConfig.getAllSystemConfig()

let configMap={}
/**
 * 限制配置
 * @type {{}}
 */
export async function getAllSystemConfig(successCallBack){
    configMap={}
    await request({
        url: '/system/config/list',
        params:{pageNum:1,pageSize:999},
        method: 'get'
    }).then(response => {
        for (let row of response.rows) {
            configMap[row.configKey]=row.configValue
        }
        successCallBack&&successCallBack()
    });
}

//使用方法获取,有利于维护配置名称
function getSysConfigValue(key){
    return configMap[key]
}

//使用方法获取,有利于维护配置名称
function getSysConfigValueToNumber(key){
    let tmp=Number(configMap[key])
    if(isNaN(tmp)){
       throw new Error("无效的系统配置,配置值不为数字",key)
    }
    return tmp
}


/**
 * 限制配置
 * @return {Promise<*>}
 */
function proPlanSourceBomTotalLimit(){
    return configMap["proPlan.source.bom.total.limit"]
}

function proPlanTargetBomTotalLimit(){
    return configMap["proPlan.target.bom.total.limit"]
}

function proPlanDefaultProTemplateId(){
    return configMap["proPlan.default.proTemplateId"]
}


//
function proPlanCreateProTaskOrderDefaultValidFlag(){
    return getSysConfigValueToNumber("proPlan.createProTaskOrder.defaultValidFlag")
}

//任务工单列表中是否显示模板
function proTaskListShowProcedureTemplate(){
    return getSysConfigValueToNumber("proTask.list.showProcedureTemplate")
}

//导出到 computed 的
export const computed={
    proPlanSourceBomTotalLimit: proPlanSourceBomTotalLimit,
    proPlanTargetBomTotalLimit: proPlanTargetBomTotalLimit,
    proPlanDefaultProTemplateId: proPlanDefaultProTemplateId,
    proPlanCreateProTaskOrderDefaultValidFlag: proPlanCreateProTaskOrderDefaultValidFlag,
    proTaskListShowProcedureTemplate: proTaskListShowProcedureTemplate,
}
