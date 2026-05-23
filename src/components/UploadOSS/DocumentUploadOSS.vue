<template>
    <UploadOSS
        ref="uploadOSSRef"
        @on-success="onSuccess"
        :requestStsToken="handleRequestStsToken"
        :maxFileSize="maxFileSize"
        :almightyOwnerInfo="almightyOwnerInfo"
    >

    </UploadOSS>
</template>
<script>
import { changeOSSUpdateStatus,getStsInfoUnDocument,getStsInfoHasDocument } from '/src/api/base/base-ali-oss'
import UploadOSS from '/src/components/UploadOSS/UploadOSS'

export default {
    components:{
        UploadOSS,
    },
    name: "DocumentUploadOSS",
    props:{
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

    },
    data () {
        return {
            documentId: null,
            // ownerId: 0,
            // ownerName: null,
        }
    },
    mounted(){
        this.initComponent();
    },
    methods: {
        show(){
            this.initComponent();
            this.$nextTick(()=>{
                this.$refs.uploadOSSRef.show();
            })
        },
        initComponent(){
            // this.ownerId= 0;
            // this.ownerName= null
        },
        onSuccess(url,document){
            if(document?.wbDocumentId){
                this.documentId=document.wbDocumentId
            }
            // console.log("url",url)
            let params = {wbDocumentId:this.documentId}
            // console.log("params",params)
            changeOSSUpdateStatus(params).then(response=>{
                let tmp = {
                    wbDocumentId: this.documentId,
                    url: url,
                }
                this.$emit("on-success", tmp)
            });

        },
        almightyOwnerInfo({file,fileName,formData}){
            const data={
                ownerId: this.ownerId,
                ownerName: this.ownerName,
                fileName: fileName,
                file: file,
            }

            formData.append("ownerName",this.ownerName)
            formData.append("ownerId",this.ownerId)
            console.log("almightyOwnerInfo.data",data)

            return data;
        },
        /**
         *
         * @param fileName    格式化好的fileName,不包含路径名
         * @param file        选择的文件
         * @return {Promise}  如果返回结构一致,而且不需要做其它处理,则不需要作处理,直接返回即可
         */
        handleRequestStsToken(fileName,file){
            const data={
                ownerId: this.ownerId,
                ownerName: this.ownerName,
                fileName: fileName,
                docType:this.docType,
            }
            // return getStsInfoHasDocument(data)
            return new Promise((resolve, reject) => {
                getStsInfoHasDocument(data).then(response => {
                    // console.log("response",response)
                    this.documentId=response.data.documentId
                    resolve(response)
                }).catch(err => {
                    // console.log(err)
                    reject(err)
                })
            })
        },
    }
}
</script>
