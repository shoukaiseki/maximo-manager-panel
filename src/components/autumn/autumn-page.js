//暴露mixins对象
import { initListVueVirtualUUID } from '../../utils/sks-table'

export const autumnPageMixin = {
    data(){
        return{
            allLoading: false,

            mainTable: {
                //主键,用于选择框
                uniqueId: 'id',

                //表过滤标签页
                tabsFilterValue: undefined,
                //表过滤标签页选项
                tabsFilterOptionList: undefined,
                //变更标签页前要设置的默认值
                tabsQueryParamsDefaultSetting: {},

                dataType: 1,

                queryParams:{
                    start: 0,
                    limit: 100,
                    pageSize: 10,
                    pageNum: 1,
                },
                list:[],
                total: 0,
                loading: true,

                //当前存在单选项,true 禁用
                single : true,
                //当前存在多选项,true 禁用
                multiple : true,
                //点击的行
                currentRow: {},
                //勾选的行ID集合
                ids:[],
                // 勾选的行
                selection: [],

                showTable: true,

                showAllColumnButton: true,
                showAllColumn: false,

                showSearchButton: false,
                showSearch: true,
            },
        }
    },
    beforeCreate(){
        console.log("混入的beforeCreated");

    },
    created() {
        console.log("autumn-page.created")
        this.autumnInit();
    },
    methods: {
        autumnInit(){
            this.mergeFromObject(this.mainTable,this.initMainTableParam())
            this.getMainList()
        },
        /**
         * 初始化 mainTable 的一些值
         * @return {}  例如: {showSearchButton: true} 显示工具栏的搜索放大镜
         */
        initMainTableParam(){
            return {}
        },
        getMainListRemote(){
            return new Promise((resolve, reject) => {
                resolve({
                    data:{
                        list:[],
                        total: 0,
                    }
                })
            });
        },
        handleQuery(){
          this.mainTable.queryParams.pageNum=1
            this.getMainList()
        },
        getMainList(){
            this.mainTable.list=[]
            this.mainTable.currentRow=undefined
            this.mainTable.loading=true
            this.getMainListRemote(this.mainTable.queryParams).then(response=>{
                const res=this.handleSuccessList(response)
                this.mainTable.loading=false
                let list = res.data.list
                initListVueVirtualUUID(list)
                this.mainTable.list= list
                this.mainTable.total=res.data.total
           }).catch(error=>{
           })
        },
        //处理成功的数据
        handleSuccessList(res){
            return res
        },


        convertAqoeousList(mainPage){
          return {
              data: {
                  list: mainPage.list,
                  total: mainPage.total,
              }
          }
        },
    },
}
