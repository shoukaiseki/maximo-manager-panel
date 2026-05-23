<template>
    <div>
        <el-table v-loading="lookupDomainTable.loading" :data="lookupDomainTable.list">

            <el-table-column v-if="isAdmin" label="主键" align="center" prop="processHistoryId" />
            <el-table-column label="操作时间" align="center" prop="createTime" width="180">
                <template slot-scope="scope">
                    <span>{{ parseTime(scope.row.createTime) }}</span>
                </template>
            </el-table-column>
            <!--            <el-table-column label="节点类型" align="center" prop="nodeType" />-->
<!--            <el-table-column label="任务类型" align="center" prop="taskType" />-->
            <el-table-column label="节点名称" align="center" prop="nodeName" />
            <el-table-column label="操作人" align="center" prop="userName" />
            <el-table-column label="备注" align="center" prop="remark" />
        </el-table>

        <pagination
            :pageSizes="[4, 10, 20, 50]"
            v-show="lookupDomainTable.total>0"
            :total="lookupDomainTable.total"
            :page.sync="lookupDomainTable.queryParams.pageNum"
            :limit.sync="lookupDomainTable.queryParams.pageSize"
            @pagination="lookupList"
        />
    </div>
</template>

<script>
import { listProcessHistory } from '/src/api/wb/ProcessHistory'

export default {
    name: 'ProcessHistoryList',
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
    data(){
      return {
          lookupDomainTable:{
              queryParams: {
                  pageNum: 1,
                  pageSize: 4,
              },
              total: 0,
              list:[],
              loading:false,

              //未使用
              showDialog:false,
          },
          //是否为超管
          isAdmin: false,
          //是否为管理员
          isSystem: false,
      }
    },
    created() {
        this.isAdmin=this.checkRole(['admin']);
        this.isSystem=this.checkRole(['system'])||this.isAdmin;
    },
    methods:{
        initProcess(){
            this.lookupDomainTable.loading = true;
            this.lookupDomainTable.pageNum=1
            this.lookupDomainTable.list=[]
            if(!(this.ownerName&&this.ownerId&&this.appName)){
                return
            }
            this.lookupList()
        },
        lookupList(){
            this.lookupDomainTable.loading=true
            this.lookupDomainTable.list=[]
            let queryParams = this.lookupDomainTable.queryParams
            queryParams.ownerId=this.ownerId
            queryParams.ownerName=this.ownerName
            queryParams.appName=this.appName
            queryParams.orderByColumn='process_history_id'
            queryParams.isAsc='desc'
            listProcessHistory(this.lookupDomainTable.queryParams).then(res=>{
                // console.log("res=",res)
                this.lookupDomainTable.loading = false;
                this.lookupDomainTable.total=res.total;
                this.lookupDomainTable.list=res.rows;
            })
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
            <ProcessHistoryList
                ref="processHistoryListRef"
                :ownerName="ownerName"
                :appName="appName"
                :ownerId="ownerId"
            >

    import ProcessHistoryList from '/src/components/Process/ProcessHistoryList'

    ProcessHistoryList,
-->
