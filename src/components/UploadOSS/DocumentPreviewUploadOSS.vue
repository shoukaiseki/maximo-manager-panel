<template>
    <div>
        <div class="imgDiv">
            <div class="imgList" v-for="(item,index) in virtualDocumentList">
                <el-image  :zIndex="99999999"  style="width: 100px; height: 100px;" :src="item.url"  fit="contain" :preview-src-list="virtualDocumentList | filterImgUrl"></el-image>
                <div class="imgDelDiv"
                     v-if="!readonly"
                >
                    <i class="el-icon-delete" @click="delDocument(item,index)"></i>
                </div>
                <div v-if="formatType" style="text-align:center;">{{defaultFormatName(item)}}</div>
            </div>
            <el-button
                v-if="!readonly"
                icon="el-icon-plus"  style="font-size: 80px;height: 100px" @click="()=>{this.msgSuccess('aaa');this.$refs.documentUploadOSSRef.show();}"></el-button>
            <!--            font-weight: bold;-->
            <!--            <i class="el-icon-plus avatar-uploader-icon"></i>-->

        </div>
        <DocumentUploadOSS
            :ownerName="ownerName"
            :ownerId="ownerId"
            :docType="docType"
            @on-success="handleSuccess"
            :maxFileSize="maxFileSize"
            ref="documentUploadOSSRef">

        </DocumentUploadOSS>
    </div>

</template>

<script>
import DocumentUploadOSS from '@/components/UploadOSS/DocumentUploadOSS'
import { deleteDocument } from '@/api/base/base-ali-oss'
export default {
    name: 'DocumentPreviewUploadOSS',
    components:{
        DocumentUploadOSS,
    },
    props:{
        readonly:[Boolean],
        /**
         * 只有新增时候才会使用,上传/删除图片后会更新 documentIdList 主键数组,用于后台更新ownerId的值
         */
        value:[Array],
        //文件上传允许最大值
        maxFileSize:{
            type:Number,
            default: 30,
        },
        ownerId: {
            type: Number,
            default: 0,
        },
        ownerName: [String],
        docType: {
            type: String,
            default: 'default'
        },
        //1: 显示 name
        //2: 格式化显示 type
        formatType:{
            type: Number,
            default: 0,
        },
        formatTypeOptions:{
            type: Array,
            default: ()=>{
                return []
            }
        },
        formatName:{
            type: Function,
            default: undefined,
        },
    },
    watch:{
        ownerId: {
            handler(val) {
                this.getDocumentList();
            }

        }
    },
    data(){
        return {
            virtualDocumentList: [],
        }

    },
    created() {
        this.isAdmin=this.checkRole(['admin']);
        this.isSystem=this.checkRole(['system'])||this.isAdmin;
        this.initComponent();
    },
    activated() {
        this.initComponent();
    },
    mounted() {
        this.initComponent();
    },
    filters:{
        filterImgUrl(e){
            return e.map(item=>item.url)
        },
        filterDocumentId(e){
            return e.map(item=>item.wbDocumentId)
        },
        numchange(e){
            return (e*100).toFixed(2)
        }
    },
    methods: {
        show() {
            this.$nextTick(() => {
                this.$refs.uploadOSSRef.show();
            })
        },
        initComponent() {
            this.getDocumentList();
        },
        getDocumentList(){
            this.virtualDocumentList=[];
            //console.log("ownerId=",this.ownerId)
            if(!this.ownerId){
                return;
            }
            const queryObjectList =
                [
                    {
                        qoName: 'documentQO',
                        mergeParent : true,
                        resultName: 'virtualDocumentList',
                        qoJson: {
                            pageNum: 1,
                            pageSize:  10,
                            ownerName: this.ownerName,
                            ownerId: this.ownerId,
                        },
                    }
                ]
            this.aqoeous(queryObjectList).then(response=>{
                //console.log("item.response",response)
                this.virtualDocumentList=  response.data.virtualDocumentList.list;
                //console.log("virtualDocumentList",this.virtualDocumentList)
            })
        },
        delDocument(item, index) {
            //console.log(item);
            //console.log(index);
            this.$confirm("是否删除此图片?", "提示", {
                confirmButtonText: "确定",
                cancelButtonText: "取消",
                type: "warning"
            })
                .then(() => {
                    deleteDocument(item.wbDocumentId).then(res => {
                        this.virtualDocumentList.splice(index, 1);
                        this.updateDocumentIdList()
                        this.msgSuccess("删除成功");
                    });
                })
                .catch(() => {
                    // this.$message({
                    //   type: 'info',
                    //   message: '已取消删除'
                    // });
                });
        },
        handleSuccess(doc){
            this.virtualDocumentList.push(doc)
            this.updateDocumentIdList()
        },
        updateDocumentIdList(){
            let documentIdList = this.$options.filters['filterDocumentId'](this.virtualDocumentList)
            // console.log("documentIdList",documentIdList)
            this.$emit("input", documentIdList);
            // console.log("virtualDocumentList",this.virtualDocumentList)
        },
        getDocumentIdList(){
            let documentIdList = this.$options.filters['filterDocumentId'](this.virtualDocumentList)
            this.$emit("input", documentIdList);
            return this.documentIdList
        },
        defaultFormatName(item){
            if(this.formatName){
                return this.formatName(item)
            }
            if (this.formatType == 1) {
                return item.name
            }
            if (this.formatType == 2) {
                for (let row of this.formatTypeOptions) {
                    if (row.dictValue === item.type) {
                        return row.dictLabel
                    }
                }
            }
            return ""
        },

    },

}
</script>

<!--<style lang="scss" >-->
<style lang="scss" scoped>
.imgDiv {
    width: 100%;
    display: flex;
    justify-content: start;
    flex-wrap: wrap;
    .imgList {
        width: 100px;
        height: 150px;
        margin: 5px;
        position: relative;
        .imgDelDiv{
            text-align: center;
            margin: 0px 0 0;
            font-size: 18px;
            cursor:pointer;
        }
    }
}
</style>

<!--
                <el-form-item label="文件">
                    <DocumentPreviewUploadOSS
                        v-model="form.documentIdList"
                        :ownerName="ownerName"
                        :ownerId="form.demoSelectSourceId"
                    >
                    </DocumentPreviewUploadOSS>
                </el-form-item>

        import DocumentPreviewUploadOSS from '@/components/UploadOSS/DocumentPreviewUploadOSS'

        DocumentPreviewUploadOSS,

        ownerName: 'demo_select_source',
-->
