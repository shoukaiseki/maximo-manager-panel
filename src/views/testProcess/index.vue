<template>

  <div
    class="app-container"
    v-loading.fullscreen.lock="allloading"
    element-loading-text="拼命加载中"
    element-loading-spinner="el-icon-loading"
    element-loading-background="rgba(0, 0, 0, 0.8)"
  >
    <div class="divLine">
      <template>
        <!--    <el-button type="warning"  size="mini" @click="initProcess()">刷新</el-button>-->
        <el-button type="primary"  size="mini" @click="initProcess()">刷新</el-button>
      </template>
    </div>
    <div class="divLine">
      <template>
        <div v-for="(actionInfo,index1) in actionList" :key="index1">
          <el-button :type="processStatusButtonType(actionInfo)"  size="mini" @click="actionProcess(index1)">{{actionInfo.processActionLabel}}</el-button>
          <div v-for="(selectAction,index2) in actionInfo.selectActionList" :key="index2">
            <el-button type="success"  size="mini" @click="actionProcess(index1,index2)">{{selectAction.processActionLabel}}</el-button>
          </div>
        </div>
      </template>
    </div>
<!--    <div class="divLine">-->
<!--      <template>-->
<!--        <div>sale_order详情</div>-->
<!--        <div v-if="formProcess">-->
<!--          <div> finishStatus: {{formProcess.finishStatus}}</div>-->
<!--          <div> remark: {{formProcess.remark}}</div>-->
<!--        </div>-->
<!--      </template>-->
<!--    </div>-->
    <div class="divLine">
      <template>
        <div>当前流程进度图</div>
        <div v-for="(process,index5) in processNodeTreeInfoList">
          <el-button type="danger"  size="mini" @click="deleteProcessProgress(process.processProgressId)">删除流程{{process.processProgressId}}</el-button>
        </div>
        <ProcessLine v-model="processNodeTreeInfoList">
        </ProcessLine>
      </template>
    </div>
    <div class="divLine">
      <template>
        <div>流程配置</div>
        <ProcessLine v-model="processNodeConfTreeList">
        </ProcessLine>
      </template>
    </div>
    <div class="divLine">
      <template>
        <div>可编辑任务分配节点</div>

        <!--	  <div>新增,删除,修改接口参考src/views/wb/ProcessDistribution调用的接口,processName,processNodeId,processProgressId,processConfId,processConfNodeId,ownerName,appName,ownerId,processName的值根据editableDistributionNode的值设置.-->
        <!--	  userId为选择的用户ID,userName为用户昵称nickName</div>-->
        <!--      <div v-for="(editableDistributionNode,index3) in editableDistributionNodeList" >-->
        <!--        <div class="processDivMain processDiv">-->
        <!--          <div >{{editableDistributionNode.nodeLabel}}</div>-->
        <!--          &lt;!&ndash;            <div>{{editableNode}}</div>&ndash;&gt;-->
        <!--          <div v-for="(virtualProcessDistribution,index4) in editableDistributionNode.virtualProcessDistributionList" >-->
        <!--            <div class="processPanelDiv processDiv">-->
        <!--              {{virtualProcessDistribution.userName}}-->
        <!--            </div>-->
        <!--          </div>-->
        <!--		  &lt;!&ndash;-->
        <!--		  &ndash;&gt;-->
        <!--        </div>-->
        <!--      </div>-->
        <!-- <div>新增,删除,修改接口参考src/views/wb/ProcessDistribution调用的接口,processName,processNodeId,processProgressId,processConfId,processConfNodeId,ownerName,appName,ownerId,processName的值根据editableDistributionNode的值设置.
      userId为选择的用户ID,userName为用户昵称nickName</div> -->
        <div v-for="(editableDistributionNode,index3) in editableDistributionNodeList" >
          <div class="processDivMain processDiv baseradio">

            <div >{{editableDistributionNode.nodeLabel}}</div>
            <!--            <div>{{editableNode}}</div>-->
            <el-button
              type="success"
              size="mini"
              @click="sjopenfun(editableDistributionNode,index3)"
            >增加设计人员</el-button>
            <div v-for="(virtualProcessDistribution,index4) in editableDistributionNode.virtualProcessDistributionList" class="baseradiolist">
              <div class="processPanelDiv processDiv baseradiosun">
                {{virtualProcessDistribution.userName}}
              </div>
              <el-button type="danger" icon="el-icon-delete" size="mini" @click="delsjuserid(editableDistributionNode,virtualProcessDistribution,index4)" >删除</el-button>
              <!--                          <div class="deldiv" style="cursor:pointer;font-size:12px;" @click="delsjuserid(editableDistributionNode,virtualProcessDistribution,index4)">-->
              <!--                            删除-->
              <!--                          </div>-->
            </div>
          </div>
        </div>
      </template>
    </div>
    <div class="divLine">
      <template>
        <div>可编辑节点</div>
        <div v-for="(editableNode,index3) in editableNodeList" :key="editableNode.processConfNodeId">
          <div class="processDivMain processDiv">
            <div >{{editableNode.nodeLabel}}</div>
            <!--
                <el-button type="success" v-if="editableNode.processStatus<0||editableNode.processStatus>10"  size="mini" @click="addDistributionTask(editableNode,editableNode.virtualProcessNodeChildList)">增加任务分配</el-button>
            -->
            <div style="">
              <el-button
                type="success"
                v-if="canEditProcessNode(editableNode)"
                size="mini"
                @click="adddialog(editableNode,editableNode.virtualProcessNodeChildList)"
              >增加任务分配</el-button>
              <el-button
                type="success"
                v-if="canEditProcessNode(editableNode)"
                size="mini"
                @click="pzbtnaddlist(editableNode,editableNode.virtualProcessNodeChildList)"
              >配置</el-button>
              <!-- <div class="deldiv" style="cursor:pointer;font-size:12px;margin-left:10px;" @click="pzbtnaddlist(editableNode,editableNode.virtualProcessNodeChildList)">
                配置
              </div> -->
            </div>
            <!--            <div>{{editableNode}}</div>-->
            <!--
                <div v-for="(editableNodeChild,index4) in editableNode.virtualProcessNodeChildList" :key="editableNodeChild.processNodeId">
                  <div class="processPanelDiv processDiv" :class="getBadgeClass(editableNodeChild)">
                    {{editableNodeChild.nodeLabel}}
                  </div>
                </div>
            -->
            <draggable
              v-model="editableNode.virtualProcessNodeChildList"
              @end="draponEnd($event,editableNode.virtualProcessNodeChildList)"
            >
              <transition-group>
                <div
                  class="dragdiv"
                  style="display: flex;align-items: center;margin:5px 0;"
                  v-for="(editableNodeChild,index4) in editableNode.virtualProcessNodeChildList"
                  :key="editableNodeChild.processNodeId"
                >
                  <div
                    class="processPanelDiv processDiv"
                    :class="getBadgeClass(editableNodeChild)"
                    style="float: none;cursor:move;min-width:80px;"
                  >{{editableNodeChild.nodeLabel}}</div>
                  <div v-if="canEditProcessNode(editableNode)" class="deldiv" style="cursor:pointer;font-size:12px;" @click="deluserid(editableNodeChild,index4,editableNode.virtualProcessNodeChildList)">
                    删除
                  </div>
                  <!-- <div style="color:#666666">
                      {{index+1}}.
                      <span>
                        {{element.processConfNodeName}}
                      </span>
                    </div>
                    <span @click="deluserid(element,index)">
                      删除
                  </span>-->
                </div>
              </transition-group>
            </draggable>
          </div>
        </div>
      </template>
    </div>
    <div class="divLine">
      <template>
        <div>当前流程任务分配的节点</div>
        <div v-for="(distributionTaskProcessNode,index3) in distributionTaskProcessNodeList" :key="distributionTaskProcessNode.processNodeId">
          <div class="processDivMain processDiv">
            <div class="processPanelDiv processDiv" :class="getBadgeClass(distributionTaskProcessNode)">
              {{distributionTaskProcessNode.nodeLabel}}
            </div>
          </div>
        </div>
      </template>
    </div>


    <!-- 用户组列表弹窗 -->
    <el-dialog title="用户组列表" :visible.sync="userlistdialog" width="300px" append-to-body>
      <div class="userlistdiv">
        <div class="userlist_list" v-for="item,index in userlist">
          <span>{{item.name}}</span>
          <span @click="addtofrom(item,index)">添加</span>
        </div>
      </div>
    </el-dialog>
    <!-- 設計師列表弹窗 -->
    <el-dialog title="设计列表" :visible.sync="sjuserligopen" width="300px" append-to-body>
      <div class="userlistdiv">
        <div class="userlist_list" v-for="item,index in sjuserlist">
          <span>{{item.nickName}}</span>
          <span @click="addsjtofrom(item,index)">添加</span>
        </div>
      </div>
    </el-dialog>

    <el-dialog title="工序模板列表" :visible.sync="gxlistdialog" width="500px" append-to-body>
      <el-table  :data="gxlistdata">
        <el-table-column label="ID" align="center" prop="processConfId" />
        <el-table-column label="流程名称" align="center" prop="processName" />
        <el-table-column label="工序" align="center" width="120px">
          <template slot-scope="scope">
            <div class="gxusergropediv">
              <div class="gxusergropediv_list" v-for="item,index in scope.row.virtualProcessNodeConfList">
                <div>
                  {{item.nodeLabel}}
                </div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
          <template slot-scope="scope">
            <el-button size="mini" type="text" icon="el-icon-edit" @click="addpztofrom(scope.row,scope.$index)">添加</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

  </div>
</template>

<script>
import request from '/src/utils/request';
import ProcessLine from '/src/components/ProcessLine';
// import {  getProduct} from "/src/api/wb/Product";
import draggable from "vuedraggable";

const ownerName='sale_order';
const appName='saleOrder';

const ownerId=1;

export async function delProcessNode(data) {
  return await request({
    url: "/process/SaleOrderProcess/deleteProcessNode?processNodeIds="+data,
    method: "post"
    // data: data
  });
}

export async function addmubantoform(query) {
  return await request({
    url: "/process/SaleOrderProcess/appendProcessNodeFromTemplate",
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

// 获取可操作的流程
export function listProcess() {
  return request({
    url: '/process/SaleOrderProcess/nextNodeInfo',
    method: 'get',
    params: {
      ownerName: ownerName ,
      appName: appName ,
      ownerId: ownerId ,
    },
  })
};

//首次启动发送流程
export function actionProcess(data) {
  return request({
    url: '/process/SaleOrderProcess/actionProcess',
    method: 'post',
    params: {
        ownerName: ownerName ,
        appName: appName ,
        ownerId: ownerId ,
    },
    data:data
  })
};

//流程树结构信息
export function treeList() {
  return request({
    url: '/process/SaleOrderProcess/treeList',
    method: 'get',
    params: {
        ownerName: ownerName ,
        appName: appName ,
        ownerId: ownerId ,
      'showSelectedNode':true
    },
  })
};

//可编辑节点列表
export function editableDistributionNodeList() {
  return request({
    url: '/process/SaleOrderProcess/editableDistributionNodeList',
    method: 'get',
    params: {
        ownerName: ownerName ,
        appName: appName ,
        ownerId: ownerId ,
      'nodeGroupId': null
    },
  })
};

//可编辑节点列表
export function editableNodeList() {
  return request({
    url: '/process/SaleOrderProcess/editableNodeList',
    method: 'get',
    params: {
        ownerName: ownerName ,
        appName: appName ,
        ownerId: ownerId ,
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
    url: '/process/SaleOrderProcess/distributionTaskProcessNodeList',
    method: 'get',
    params: {
        ownerName: ownerName ,
        appName: appName ,
        ownerId: ownerId ,
    },
  })
};

export async function addProcessNode(data) {
  return await request({
    url: '/process/SaleOrderProcess/addProcessNode',
    method: 'post',
    data: data
    ,
  })
};


export async function batchModifyProcessNodeSort(data) {
  return await request({
    url: '/process/SaleOrderProcess/batchModifyProcessNodeSort',
    method: 'post',
    data: data
    ,
  })
};

export async function deleteProcessProgress(processProgressId) {
  return await request({
    url: '/process/SaleOrderProcess/deleteProcessProgress',
    method: 'post',
    params: {
      processProgressId: processProgressId
    }
    ,
  })
};

// export async function getgxmubanlist(query) {
//   return await request({
//     url: "/wb/ProcessConfTemplate/lookup",
//     method: 'get',
//     params: query
//   });
// }

export default {
  components: {
    ProcessLine,
    draggable
  },
  data() {
    return{
      gxlistdialog:false,
      gxlistdata:[],
      pzaddnode:{},

      userlistdialog: false,
      userlist: [],
      sjuserligopen:false,
      sjuserlist:[],
      sjaddnodemain:{},
      sjaddnodeindex:'',
      items: [{d:"aa",t:"bb"}],
      actionList:[],
      processNodeTreeInfoList:[],
      editableNodeList:[],
      editableDistributionNodeList:[],
      processNodeConfTreeList:[],
      distributionTaskProcessNodeList:[],
      testString: "asus,linux",
      formProcess:{},

      allloading: false,

      // 遮罩层
      loading: true,

      addnode: {},
      addchildlist: {}

    }
  },
  mounted(){
    this.initProcess();
    this.getsjuserlist();
    this.getUserGroupList();
    // this.getgxmubanlist();
  },
  methods: {
    deluserid(e,x,virtualProcessNodeChildList){
      console.log(e,x)
      this.$confirm('是否删除此节点?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        // var
        delProcessNode(e.processNodeId).then(res=>{
          console.log(res)
          virtualProcessNodeChildList.splice(x,1);
          var sortList = [];
          for (var i = 0; i < virtualProcessNodeChildList.length; i++) {
            sortList.push({
              processNodeId: virtualProcessNodeChildList[i].processNodeId
            });
          }
          sortList = this.resetProcessNodeListSort(sortList);
          batchModifyProcessNodeSort(sortList).then(response => {
            // this.msgSuccess("操作成功");
          });
        })
      }).catch(() => {
        // this.$message({
        //   type: 'info',
        //   message: '已取消删除'
        // });
      });
    },
    addpztofrom(e,x){
      console.log(this.pzaddnode.processNodeId)
      console.log(e.processConfId)
      var data={
        processNodeId:this.pzaddnode.processNodeId,
        processConfId:e.processConfId
      }
      addmubantoform(data).then(res=>{
        console.log(res)
        this.initProcess();
        // this.userlistdialog = false;
        this.msgSuccess("新增节点成功");
      })
      this.gxlistdialog=false;
    },
    pzbtnaddlist(e,x){
      // console.log(e)
      this.pzaddnode=e;

      this.gxlistdialog=true;
    },
    // getgxmubanlist(){
    //   getgxmubanlist().then(res=>{
    //     this.gxlistdata=res.rows;
    //     console.log(res.row)
    //   })
    // },
    addsjtofrom(e,x){
      console.log(e,x)
      var sjform={
        processName:this.sjaddnodemain.processName,
        processNodeId:this.sjaddnodemain.processNodeId,
        processProgressId:this.sjaddnodemain.processProgressId,
        processConfId:this.sjaddnodemain.processConfId,
        processConfNodeId:this.sjaddnodemain.processConfNodeId,
        ownerName:this.sjaddnodemain.ownerName,
        appName:this.sjaddnodemain.appName,
        ownerId:this.sjaddnodemain.ownerId,
        userId:e.userId,
        userName:e.nickName
      }
      addProcessDistribution(sjform).then(res=>{
        console.log(res)
        this.initProcess();
        this.msgSuccess("新增节点成功");
        this.sjuserligopen=false;
      })
    },
    getUserGroupList() {
      var params = {
        pageNum: 1,
        pageSize: 999
      };
      getUserGroupList(params).then(res => {
        this.userlist = res.rows;
      });
    },
    delsjuserid(e,x ,s){
      console.log(e,x)
      this.$confirm('是否删除此节点?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        // var
        delProcessDistribution(x.processDistributionId).then(res=>{
          console.log(res)
          editableDistributionNodeList(this.shopwbProductId).then(response => {
            // console.log("editableDistributionNodeList");
            // console.log("editableDistributionNodeList",response);
            this.editableDistributionNodeList = response.data;
            // this.msgSuccess("可编辑节点加载完成");
            this.msgSuccess("删除成功");
          });
        })
      }).catch(() => {
        // this.$message({
        //   type: 'info',
        //   message: '已取消删除'
        // });
      });

    },
    initProcess() {
      this.actionList=[];
      this.processNodeTreeInfoList=[];
      this.editableNodeList=[];
      this.editableDistributionNodeList=[];


      // getProduct(wbProductId).then(response => {
      //   this.formProcess = response.data;
      // });

      console.log("add")
      console.log(this.items)
      listProcess().then(response => {
        console.log("listProcess");
        console.log("listProcess",response);
        this.actionList = response.actionList;
        this.msgSuccess("加载完成");

      });
      treeList().then(response => {
        console.log("treeList");
        console.log("treeList",response);
        this.processNodeTreeInfoList = response.data;
        if (this.processNodeTreeInfoList.length > 0) {
          var processNodeTreeInfo=this.processNodeTreeInfoList[0]
          console.log("treeList.processNodeTreeInfo=",processNodeTreeInfo);
          processNodeConfTreeList(processNodeTreeInfo.processConfId).then(response2 => {
            console.log("processNodeConfTreeList.response=",response2);
            this.processNodeConfTreeList=response2.data;
          })
        }
        this.msgSuccess("流程加载完成");
      });

      editableNodeList().then(response => {
        console.log("editableNodeList");
        console.log("editableNodeList",response);
        this.editableNodeList = response.data;
        this.msgSuccess("可编辑节点加载完成");
      });

      editableDistributionNodeList().then(response => {
        console.log("editableDistributionNodeList");
        console.log("editableDistributionNodeList",response);
        this.editableDistributionNodeList = response.data;
        this.msgSuccess("可编辑节点加载完成");
      });

      distributionTaskProcessNodeList().then(response => {
        console.log("distributionTaskProcessNodeList");
        console.log("distributionTaskProcessNodeList",response);
        this.distributionTaskProcessNodeList = response.data;
        this.msgSuccess("当前流程节点加载完成");
      });


      this.items.push({
        d:'小腿',t:'2019'
      })
    },
    addtofrom(e, x) {
      this.addDistributionTask(this.addnode, this.addchildlist, e);
    },
    actionProcess(index1,index2){
      this.allloading=true;
      var actionInfo=this.actionList[index1];
      if(index2==undefined){
        console.log("actionInfo=",actionInfo)
        if(actionInfo.processActionType!='startProcess'){
          this.msgError("不能点击");
          return;
        }else{
          actionProcess(actionInfo).then(response => {
            console.log("actionProcess",response);
            this.initProcess();
            this.msgSuccess("成功");
            this.allloading=false;
          }).catch(error =>{
            this.allloading=false;
          });
          return;
        }
      }
      actionProcess(actionInfo.selectActionList[index2]).then(response => {
        console.log("actionProcess",response);
        this.initProcess();
        this.msgSuccess("成功");
        this.allloading=false;
      }).catch(error =>{
        this.allloading=false;
      });

      console.log("index1=",index1);
      console.log("index2=",index2);
    },
    processStatusButtonType(actionInfo){
      if(actionInfo.processActionValue==undefined){
        return "default"
      }else if(actionInfo.processActionValue=="-2"){
        return "warning"
      }else if(actionInfo.processActionValue>=0&&actionInfo.processActionValue<=10){
        return "success"

      }

      // return "cyan"
      return "primary"
    },
    adddialog(e, x) {
      console.log(e)
      console.log(x)
      this.addnode = e;
      this.addchildlist = x;
      this.userlistdialog = true;
    },
    addDistributionTask(editableNode,virtualProcessNodeChildList){
      console.log("editableNode=",editableNode)
      console.log("virtualProcessNodeChildList=",virtualProcessNodeChildList)
      var sortList=[]
      for (var i=0;i<virtualProcessNodeChildList.length;i++){
        sortList.push({"processNodeId":virtualProcessNodeChildList[i].processNodeId})
      }
      var data={
        "parentId": editableNode.processNodeId,
        "processConfId": editableNode.processConfId,
        "processProgressId": editableNode.processProgressId,
        "ownerId": editableNode.ownerId,
        "ownerName": editableNode.ownerName,
        "appName": editableNode.appName,
        "nodeLabel": "新增任务2",//用户组id
        "nodeName": "新增任务2",//用户组id
        "nodeType": "task",
        "taskType": "any_user",
        "virtualProcessDistributionAOList":[{
          "typeValue": 1000,//用户组id
          "type": "user_group",
          "sort": "0"
        }]
      }
      addProcessNode(data).then(response => {
        console.log("addProcessNode",response);
        sortList.push({"processNodeId":response.data})
        console.log("sortList1",sortList);
        sortList=this.resetProcessNodeListSort(sortList)
        console.log("sortList2",sortList);
        batchModifyProcessNodeSort(sortList).then(response => {
          this.msgSuccess("排序完成");
        });
        this.initProcess();
        this.msgSuccess("新增节点成功");
      });
    },
    resetProcessNodeListSort(sortList){
      var previousNode=undefined;
      for(var i=0;i<sortList.length;i++){
        var currentNode=sortList[i];
        currentNode.nextNodeId=0;
        currentNode.previousNodeId=0;
        currentNode.sort=i+1;
        if(previousNode!=undefined){
          currentNode.previousNodeId=previousNode.processNodeId;
          previousNode.nextNodeId=currentNode.processNodeId;
        }
        previousNode=currentNode;
      }
      return sortList;
    },
    deleteProcessProgress(processProgressId){
      deleteProcessProgress(processProgressId).then(response=>{
        this.initProcess();
        this.msgSuccess("流程已删除");
      })
    },
    getsjuserlist(){
      getsjuserlist().then(res => {
        this.sjuserlist = res.rows;
      });
    },
    sjopenfun(e,x){
      console.log(e,x)
      this.sjaddnodemain = e;
      this.sjaddnodeindex = x;
      this.sjuserligopen=true;
    },
    getBadgeClass: function(processNodeTreeInfoPanel){
      if(processNodeTreeInfoPanel.processStatus==undefined){
        //属于流程设置
        return "badge-info"
      }
      if(processNodeTreeInfoPanel.processStatus>=0&&processNodeTreeInfoPanel.processStatus<=10){
        return "badge-info"
      }
      if(processNodeTreeInfoPanel.processStatus>10){
        return "badge-danger"
      }
      if(processNodeTreeInfoPanel.processStatus==-1){
        return "badge-default"
      }
      if(processNodeTreeInfoPanel.processStatus==-2){
        return "badge-warning"
      }
      return "badge-default"
    }
  }
}
</script>

<style lang="scss" scoped>
.el-button--default{
  color: #FFFFFF;
  background-color: #8B91A0;
  border-color: #8B91A0;
}

.divLine{
  width: 100%;
  position: relative;
  margin:5px;
  float: left;
  .baseradio{
    border-radius: 8px;
    box-sizing: border-box;
    padding: 6px 6px;
    .baseradiolist{
      width: 100%;
      display: flex;
      align-items: center;
      .baseradiosun{
        min-width: 80px;
        background-color: #E2F2ED;
        border: 1px solid #ddd;
        border-radius: 20px;
      }
    }
  }
}

.userlistdiv {
  width: 60%;
  margin: 0 auto;
  .userlist_list {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 10px;
    box-sizing: border-box;
    padding: 5px;
    border-radius: 4px;
    span:last-child {
      color: #409eff;
      cursor: pointer;
    }
  }
  .userlist_list:hover {
    background: rgba(0, 0, 0, 0.05);
  }
}

.processDivMain{
  border:1px solid #000
}
.processDiv {
  margin:5px;
  float: left;
  text-align:center;
}

.processPanelDiv {
  border:1px solid #000;
  border-radius: 20px;
}

.badge-primary {
  background-color: #1ab394;
}

.badge-success {
  background-color: #1c84c6;
}

.badge-warning {
  background-color: #f8ac59;
}

.badge-warning-light {
  background-color: #f8ac59;
}

.badge-danger {
  background-color: #ed5565;
}

.badge-info {
  background-color: #23c6c8;
}

.badge-inverse {
  background-color: #262626;
}

.badge-white {
  background-color: #FFFFFF;
}

.badge-default {
  background-color: #8B91A0;
}
</style>
