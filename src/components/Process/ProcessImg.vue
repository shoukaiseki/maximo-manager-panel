<template>
    <div>
        <ProcessLine v-model="processNodeTreeInfoList">
        </ProcessLine>
    </div>

</template>

<script>
import ProcessLine from '/src/components/ProcessLine';
import draggable from 'vuedraggable'
import { mainProcessTreeList, processNodeConfTreeList } from './ProcessSubmitApi'

export default {
    name: 'ProcessImg',
    components: {
        ProcessLine,
        draggable
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
            processNodeTreeInfoList:[],

            processNodeConfTreeList:[],
            collapseNames:['processImg'],

        }
    },
    methods:{
        initProcess(){
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
                //console.log("treeList");
                //console.log("treeList",response);
                this.processNodeTreeInfoList = response.data;
                if (this.processNodeTreeInfoList.length > 0) {
                    let processNodeTreeInfo=this.processNodeTreeInfoList[0]
                    //console.log("treeList.processNodeTreeInfo=",processNodeTreeInfo);
                    processNodeConfTreeList(processNodeTreeInfo.processConfId).then(response2 => {
                        //console.log("processNodeConfTreeList.response=",response2);
                        this.processNodeConfTreeList=response2.data;
                    })
                }
                // this.msgSuccess("流程加载完成");
            });
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
            <ProcessImg
                :ownerName="ownerName"
                :appName="appName"
                :ownerId="ownerId"
            ></ProcessImg>

            import ProcessImg from '/src/components/Process/ProcessImg'
            ProcessImg,
-->
