//流程树结构信息
import request from '/src/utils/request'

export function mainProcessTreeList(params) {
    return request({
        url: '/process/MainProcess/treeList',
        method: 'get',
        params: params
    })
}

//首次启动发送流程
export function mainProcessActionProcess(data,params) {
    return request({
        url: '/process/MainProcess/actionProcess',
        method: 'post',
        params: params,
        data:data
    })
};

// 获取可操作的流程
export function mainProcessListProcess(params) {
    return request({
        url: '/process/MainProcess/nextNodeInfo',
        method: 'get',
        params: params,

    })
};


export async function mainProcessDeleteProcessProgress(processProgressId) {
    return await request({
        url: '/process/MainProcess/deleteProcessProgress',
        method: 'post',
        params: {
            processProgressId: processProgressId
        }
        ,
    })
};


//未使用
export async function delProcessNode(data) {
    return await request({
        url: "/process/MainProcess/deleteProcessNode?processNodeIds="+data,
        method: "post"
        // data: data
    });
}

export async function addmubantoform(query) {
    return await request({
        url: "/process/MainProcess/appendProcessNodeFromTemplate",
        method: 'post',
        params: query
    });
}

//用户组列表
export function getUserGroupList(query) {
    return request({
        url: '/wb/ProcessUserGroup/list',
        method: 'get',
        params: query
    })
}

//獲取列表
export function getsjuserlist(query) {
    return request({
        url: '/wb/PersonLookup/userList',
        method: 'post',
        params: query
    })
}


// 新增工序节点分配
export function addProcessDistribution(data) {
    return request({
        url: '/wb/ProcessDistribution',
        method: 'post',
        data: data
    })
}

// 删除工序节点分配
export function delProcessDistribution(processDistributionId) {
    return request({
        url: '/wb/ProcessDistribution/' + processDistributionId,
        method: 'delete'
    })
}




//可编辑节点列表
export function editableDistributionNodeList() {
    return request({
        url: '/process/MainProcess/editableDistributionNodeList',
        method: 'get',
        params: {
            'ownerName':'product' ,
            'appName':'product' ,
            'ownerId':wbProductId ,
            'nodeGroupId': null
        },
    })
};

//可编辑节点列表
export function editableNodeList() {
    return request({
        url: '/process/MainProcess/editableNodeList',
        method: 'get',
        params: {
            'ownerName':'product' ,
            'appName':'product' ,
            'ownerId':wbProductId ,
            'nodeGroupId': null
        },
    })
};

export function processNodeConfTreeList(processConfId) {
    return request({
        url: '/wb/ProcessNodeConf/treeList',
        method: 'get',
        params: {
            'processConfId': processConfId,
            'showSelectedNode': true
        },
    })
};

export function distributionTaskProcessNodeList() {
    return request({
        url: '/process/MainProcess/distributionTaskProcessNodeList',
        method: 'get',
        params: {
            'ownerName':'product' ,
            'appName':'product' ,
            'ownerId':wbProductId,
        },
    })
};

export async function addProcessNode(data) {
    return await request({
        url: '/process/MainProcess/addProcessNode',
        method: 'post',
        data: data
        ,
    })
};


export async function batchModifyProcessNodeSort(data) {
    return await request({
        url: '/process/MainProcess/batchModifyProcessNodeSort',
        method: 'post',
        data: data
        ,
    })
};

