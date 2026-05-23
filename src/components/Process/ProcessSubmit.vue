<template>
    <div>
        <el-row class="mb8" :gutter="10" >
            <el-col :span="24" v-for="(actionInfo,index1) in actionList" :key="index1">
                <el-tag v-if="actionInfo.processActionType!='startProcess'"  size="medium">
                    {{actionInfo.processActionLabel}}
                </el-tag>
                <el-button v-else :type="processStatusButtonType(actionInfo)"  size="mini" @click="actionProcess(index1)">{{actionInfo.processActionLabel}}</el-button>
                <el-button
                    v-for="(selectAction,index2) in actionInfo.selectActionList" :key="index2"
                    type="success"  size="mini" @click="actionProcessConfirm(index1,index2)">{{selectAction.processActionLabel}}</el-button>
            </el-col>
        </el-row>
        <PromptDialog
            title="确定进行操作吗?"
            confirmButtonText="确定"
            itemLabel="备注"
            value-type="3"
            @submit="handleSubmit"
            ref="actionProcessConfirmDialogRef"
        ></PromptDialog>

    </div>

</template>

<script>
import ProcessLine from '/src/components/ProcessLine';
import draggable from 'vuedraggable'
import {
    mainProcessActionProcess, mainProcessDeleteProcessProgress,
    mainProcessListProcess,
    mainProcessTreeList,
    processNodeConfTreeList
} from './ProcessSubmitApi'
import { listProcess } from '../../views/testProcess'
import PromptDialog from '/src/components/PromptDialog'

export default {
    name: 'ProcessSubmit',
    components: {
        ProcessLine,
        draggable,
        PromptDialog,
    },
    props:{
        ownerName:[String],
        appName:[String],
        ownerId:[Number],
        //隐藏相应组建
        // 暂未使用
        bitHide:{
            type: Number,
            default: 0,
        },
    },
    data() {
        return{
            actionList: [],
            collapseNames:['processImg'],

        }
    },
    methods:{
        initProcess(){
            this.actionList=[]
            if(!(this.ownerName&&this.ownerId&&this.appName)){
                return
            }
            const params= {
                ownerName: this.ownerName,
                appName: this.appName ,
                ownerId: this.ownerId ,
            }

            mainProcessListProcess(params).then(response => {
                // console.log("listProcess");
                // console.log("listProcess",response);
                this.actionList = response.actionList;

            });
        },
        handleSubmit(row){
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
        actionProcessConfirm(index1,index2){
            this.$nextTick(()=>{
                let tmp=""
                let actionInfo=this.actionList[index1];
                if(index2!=undefined){
                    actionInfo=actionInfo.selectActionList[index2]
                }
                tmp=actionInfo.processActionLabel
                console.log("tmp",tmp,actionInfo)
                this.$refs.actionProcessConfirmDialogRef.show({
                    ownerId:tmp,
                    success:(remark)=>{
                        this.actionProcess(index1,index2,remark)
                    },
                });
            })
        },
        actionProcess(index1,index2,remark){
            this.allloading=true;
            let actionInfo=this.actionList[index1];
            const params= {
                ownerName: this.ownerName ,
                appName: this.appName ,
                ownerId: this.ownerId ,
                remark: remark||null,
            }
            if(index2==undefined){
                // console.log("actionInfo=",actionInfo)
                if(actionInfo.processActionType!='startProcess'){
                    this.msgError("不能点击");
                    return;
                }else{
                    actionInfo.remark=remark
                    mainProcessActionProcess(actionInfo,params).then(response => {
                        // console.log("actionProcess",response);
                        this.$emit("actionProcess",actionInfo,params);
                        this.initProcess();
                        this.msgSuccess("操作成功");
                        this.allloading=false;
                    }).catch(error =>{
                        this.allloading=false;
                    });
                    return;
                }
            }
            actionInfo.selectActionList[index2].remark=remark
            mainProcessActionProcess(actionInfo.selectActionList[index2],params).then(response => {
                // console.log("actionProcess",response);
                this.$emit("actionProcess",actionInfo.selectActionList[index2],params);
                this.initProcess();
                this.msgSuccess("操作成功");
                this.allloading=false;
            }).catch(error =>{
                this.allloading=false;
            });

            // console.log("index1=",index1);
            // console.log("index2=",index2);
        },
    },
    watch:{
        appName: {
            handler(val) {
                this.initProcess()
            },
            immediate: true,
        },
        ownerName: {
            handler(val) {
                this.initProcess()
            },
            immediate: true,
        },
        ownerId: {
            handler(val) {
                this.initProcess()
            },
            immediate: true,
        },
    },
}
</script>

<style scoped>

</style>

<!--
            @actionProcess="actionProcess"
            <ProcessSubmit
                :ownerName="ownerName"
                :appName="appName"
                :ownerId="ownerId"
            ></ProcessSubmit>

            import ProcessSubmit from '/src/components/Process/ProcessSubmit'
            ProcessSubmit,


        //节点信息,owner信息
        actionProcess(actionInfo,ownerInfo){
            this.initProcess();
        },
-->
