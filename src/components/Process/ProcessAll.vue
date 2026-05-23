<template>
    <div>
        <el-row>
            <el-col :span="24">
                <el-collapse v-model="collapseNames" >
                    <el-collapse-item title="流程操作" name="processAction">
                        <el-row>
                            <el-col :span="24">
                                <ProcessSubmit
                                    @actionProcess="actionProcess"
                                    ref="processSubmitRef"
                                    :ownerName="ownerName"
                                    :appName="appName"
                                    :ownerId="ownerId"
                                ></ProcessSubmit>
                            </el-col>
                        </el-row>

                        <el-row v-if="(bitHide&1)==0">
                            <el-col :span="24">
                                <template v-for="(process,index5) in processNodeTreeInfoList">
                                    <el-button type="danger"  size="mini" @click="deleteProcessProgress(process.processProgressId)">删除流程{{process.processProgressId}}</el-button>
                                </template>
                            </el-col>
                        </el-row>
                    </el-collapse-item>
                    <el-collapse-item title="当前流程图" name="processHistory">
                        <ProcessHistoryList
                            ref="processHistoryListRef"
                            :ownerName="ownerName"
                            :appName="appName"
                            :ownerId="ownerId"
                        >

                        </ProcessHistoryList>
                    </el-collapse-item>
                    <el-collapse-item title="当前流程图" name="processImg">
                        <el-row>
                            <el-col :span="24">
<!--                                <ProcessImg-->
<!--                                    ref="processImgRef"-->
<!--                                    :ownerName="ownerName"-->
<!--                                    :appName="appName"-->
<!--                                    :ownerId="ownerId"-->
<!--                                ></ProcessImg>-->
                                <ProcessLine v-model="processNodeTreeInfoList">
                                </ProcessLine>
                            </el-col>
                        </el-row>
                    </el-collapse-item>
                    <el-collapse-item title="详细流程图" name="processConfImg">
                        <el-row>
                            <el-col :span="24">
<!--                                <ProcessConfImg-->
<!--                                    ref="processConfImgRef"-->
<!--                                    :ownerName="ownerName"-->
<!--                                    :appName="appName"-->
<!--                                    :ownerId="ownerId"-->
<!--                                ></ProcessConfImg>-->
                                <ProcessLine v-model="processNodeConfTreeList">
                                </ProcessLine>
                            </el-col>
                        </el-row>
                    </el-collapse-item>
                </el-collapse>
            </el-col>
        </el-row>

    </div>

</template>

<script>
import ProcessLine from '/src/components/ProcessLine';
import draggable from 'vuedraggable'
import { mainProcessDeleteProcessProgress, mainProcessTreeList, processNodeConfTreeList } from './ProcessSubmitApi'

import ProcessSubmit from '/src/components/Process/ProcessSubmit'
import ProcessImg from '/src/components/Process/ProcessImg'
import ProcessConfImg from '/src/components/Process/ProcessConfImg'
import ProcessHistoryList from '/src/components/Process/ProcessHistoryList'

export default {
    name: 'ProcessAll',
    components: {
        ProcessLine,
        ProcessSubmit,
        ProcessImg,
        ProcessConfImg,
        ProcessHistoryList,
        draggable,
    },
    props:{
        ownerName:[String],
        appName:[String],
        ownerId:[Number],
        /**隐藏相应组建
         * & 1 隐藏删除流程按钮
         */
        bitHide:{
            type: Number,
            default: 0,
        },

    },
    data() {
        return{
            collapseNames: ["processAction","processHistory"],
            // collapseNames: ["processAction",'processImg','processConfImg'],
            processNodeTreeInfoList:[],
            processNodeConfTreeList:[],

        }
    },
    methods:{
        initProcess(){
            this.$nextTick(()=>{
                this.$refs.processSubmitRef.initProcess();
                this.$refs.processHistoryListRef.initProcess();

                // this.$refs.processImgRef.initProcess();
                // this.$refs.processConfImgRef.initProcess();
            })
            this.processNodeConfTreeList=[]
            this.processNodeTreeInfoList=[]
            if(!(this.ownerName&&this.ownerId&&this.appName)){
               return
            }
            const params= {
                    ownerName: this.ownerName,
                    appName: this.appName ,
                    ownerId: this.ownerId ,
                    showSelectedNode:true,
            }

            mainProcessTreeList(params).then(response => {
                // console.log("treeList");
                // console.log("treeList",response);
                this.processNodeTreeInfoList = response.data;
                if (this.processNodeTreeInfoList.length > 0) {
                    var processNodeTreeInfo=this.processNodeTreeInfoList[0]
                    // console.log("treeList.processNodeTreeInfo=",processNodeTreeInfo);
                    processNodeConfTreeList(processNodeTreeInfo.processConfId).then(response2 => {
                        // console.log("processNodeConfTreeList.response=",response2);
                        this.processNodeConfTreeList=response2.data;
                    })
                }
                // this.msgSuccess("流程加载完成");
            });
        },
        deleteProcessProgress(processProgressId){
            this.$confirm('是否确认删除?', '警告', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
            }).then(()=> {
                mainProcessDeleteProcessProgress(processProgressId).then(response=>{
                    this.$emit("deleteProcess", processProgressId);
                    this.initProcess();

                    this.msgSuccess("操作成功");
                })
                return
            }).then(() => {
                return
            })
        },
        actionProcess(actionInfo,ownerInfo){
            console.log("actionProcess")
            this.initProcess();
            this.$emit("actionProcess",actionInfo,params);
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
                @deleteProcess=""

            <ProcessAll
                :ownerName="ownerName"
                :appName="appName"
                :ownerId="form.saleOrderId"
            ></ProcessAll>

            import ProcessAll from '/src/components/Process/ProcessAll'
            ProcessAll,
-->
