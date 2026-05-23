<template>
    <div>
        <el-dialog :close-on-click-modal="canClose"  :show-close="canClose"
                   title="上传文件" :visible.sync="showDialog" width="500px" append-to-body>
            <!--      :limit="1"-->
            <!--        action 任意字符覆盖该属性覆盖默认action方法-->
            <el-upload
                v-if="!fileUrl"
                action="fakeAction"
                drag
                :multiple="false"
                :auto-upload="true"
                :show-file-list="false"
                :http-request="handleRequest"
                :on-progress="uploadFileProcess"
                :before-upload="beforeUpload">
                <!--        :on-change="handleChange"-->
                <i class="el-icon-upload" style="color:#409EFF"></i>
                <div class="el-upload__text text">
                    将图片拖到此处，或
                    <em>点击上传</em>
                </div>
                <!--        <el-button slot="trigger" size="small" type="primary">选取文件</el-button>-->
                <!--        <i className="el-icon-plus avatar-uploader-icon"></i>-->
                <div slot="tip" class="el-upload__tip">上传文件大小不超过{{maxFileSize}}MB</div>
            </el-upload>
            <!--      开始上传之后图片已经无法访问-->
            <!--      <img v-if="fileUrl" :src="fileUrl" class="avatar" />-->
            <span v-if="fileSize">{{ this.formatterText() }}</span>
            <el-progress :stroke-width="16" :percentage="progressPercent"></el-progress>
        </el-dialog>
    </div>
</template>
<script>
import { getStsInfoUnDocument,getStsInfoHasDocument } from '@/api/base/base-ali-oss'
import { formatFileNameDeletePath } from '@/utils/sks'
import { formatFileSize } from '@/utils/sks'

import OSS from "ali-oss";
import { uploadFileToAlmighty } from '../../api/base/base-ali-oss'

export default {
    name: "UploadOSS",
    props:{
        //会更新 url
        value:[String],
        //文件上传允许最大值
        maxFileSize:{
            type:Number,
            default: 30,
        },
        requestStsToken:[Function],
        almightyOwnerInfo:[Function],
    },
    data () {
        return {


            fileList:[],

            showDialog: false,

            //能否关闭
            canClose: true,
            //上传进度 0-100
            progressPercent: 0,
            fileUrl: null,
            fileSize: null,
            fileUploadedSize: null,
        }
    },
    mounted(){
        this.initComponent();
    },
    methods: {
        show(){
            this.showDialog=true;
            this.initComponent();
        },
        initComponent(){

            this.fileUrl=null;
            this.canClose=true;
            this.fileSize= null;
            this.fileUploadedSize= null;
            this.progressPercent=0;
        },
        uploadFileProcess(event, file, fileList){
            let fileStoreType = this.systemConfig.fileStoreType
            if (fileStoreType === this.systemConfig.fileStoreTypeOptions.localDisk) {
                this.progressPercent = file.percentage.toFixed(0);
                this.fileUploadedSize=Number((this.fileSize*file.percentage).toFixed())
            }

        },
        handleRequest (params) {

            let fileStoreType = this.systemConfig.fileStoreType
            console.log("fileStoreType",fileStoreType)
            if (fileStoreType === this.systemConfig.fileStoreTypeOptions.localDisk) {
                this.uploadFileToAlmighty(params.file)
                return;
            }

            if (fileStoreType === this.systemConfig.fileStoreTypeOptions.aliyunOSS) {
                this.uploadToAliyunOSS(params.file)
                return;
            }
            console.error("无效的存储类型",fileStoreType)
            // console.log("params",params)
            // let formdata = new FormData()
            // formdata.append('file', params.file)
            // const config = {
            //   onUploadProgress: progressEvent => {
            //     // progressEvent.loaded:已上传文件大小
            //     // progressEvent.total:被上传文件的总大小
            //     this.progressPercent = Number((progressEvent.loaded / progressEvent.total * 100).toFixed(2))
            //   }
            // }
            // this.$axios.post(this.actionURL,formdata,config).then(res => {
            //   if (res.params.code===1) {}
            // })
        },
        //上传前对文件大小进行校验
        beforeUpload(file) {

            const isLt2M = file.size / 1024 / 1024 <= this.maxFileSize;
            if (!isLt2M) {
                this.$message.error(`上传文件大小大小不能超过 ${this.maxFileSize}MB!`);
                return isLt2M;
            }
            let localUrl = window.webkitURL.createObjectURL(file)
            this.fileSize=file.size;
            this.fileUrl=localUrl;

            // console.log("file",file)


        },
        formatterText(){
            // 数据小于0.1M的时候按KB显示
            // const size = file.size/1024/1024 > 0.1 ? `(${(file.size/1024/1024).toFixed(1)}M)` : `(${(file.size/1024).toFixed(1)}KB)`
            // file.name.indexOf('M')>-1 || file.name.indexOf('KB')>-1 ? file.name : file.name += size

            return `文件大小:${formatFileSize(this.fileSize)};已上传:${formatFileSize(this.fileUploadedSize)}`
        },
        uploadFileToAlmighty(file){
            this.fileUrl=file.name
            let fileName=formatFileNameDeletePath(file.name);
            let formData = new FormData();
             this.almightyOwnerInfo(
                {
                    file:file,
                    fileName:fileName,
                    formData: formData
                })

            formData.append("file", file);
            formData.append("fileName",fileName)
            uploadFileToAlmighty(formData,{},(progressEvent)=>{
                console.log("event",progressEvent)
                let tmp = progressEvent.loaded / progressEvent.total
                let complete = (tmp * 100 | 0)
                this.progressPercent = complete
                this.fileUploadedSize=Number((this.fileSize*tmp).toFixed())
            }).then(response=>{
                this.canClose=true;
                let ossUrl=response.data.url
                //此处获取的url包含 uploadId,需要处理
                //使用token返回的url
                this.$emit("on-success", ossUrl,response.data)
                this.$emit("input", ossUrl)
            })
            // uploadAvatar(formData).then(response => {
            //     this.open = false;
                //     this.options.img = process.env.VUE_APP_BASE_API + response.imgUrl;
                //     store.commit('SET_AVATAR', this.options.img);
                //     this.msgSuccess("修改成功");
                //     this.visible = false;
                // });



        },
        uploadToAliyunOSS(file){
            // console.log(file)
            this.fileUrl=file.name
            let fileName=formatFileNameDeletePath(file.name);
            const data={
                fileName: fileName,
            }
            const _this=this;
            this.canClose=false;
            let requestStsToken
            if(this.requestStsToken !== undefined){
                requestStsToken = this.requestStsToken(fileName,file)
            }else{
                requestStsToken=getStsInfoUnDocument(data);
            }
            // console.log("requestStsTokenFunction",requestStsToken)
            requestStsToken.then(res=>{
                const ossName = res.data.ossName;
                const ossUrl=res.data.url
                // console.log("stsInfo",res)
                let client = new OSS({

                    //"oss-cn-hangzhou", //res.data.endpoint,
                    region: res.data.endpoint.replace('.aliyuncs.com',''),
                    secure: true, // secure: 配合region使用，如果指定了secure为true，则使用HTTPS访问
                    accessKeyId: res.data.accessKeyId,
                    accessKeySecret: res.data.accessKeySecret,
                    stsToken: res.data.stsToken,
                    bucket: res.data.bucket
                });
                client.multipartUpload(ossName, file,{
                    progress:function (p) { //获取进度条的值
                        // console.log(p)
                        _this.progressPercent = Number((p * 100).toFixed(2))
                        _this.fileUploadedSize=Number((_this.fileSize*p).toFixed())
                    },
                })
                    .then(res1 => {
                        this.canClose=true;
                        //此处获取的url包含 uploadId,需要处理
                        // console.log("res1",res1)
                        // let url=res1.res.requestUrls[0]
                        // if(url.indexOf("?")>-1){
                        //   url=url.substring(0,url.indexOf("?"))
                        // }
                        // console.log("url",url)
                        //使用token返回的url
                        this.$emit("on-success", ossUrl,res1)
                        this.$emit("input", ossUrl)

                    }).catch(error=>{
                    this.msgError("上传阿里云时出错!")
                    this.canClose=true;
                })
            }).catch(error=>{
                this.msgError("获取上传密钥出错,请检查是否阿里云余额不足?")
                this.canClose=true;
            })
        },
    }
}
</script>
<style scoped lang="scss">
.avatar {
    width: 100%;
    height: 100%;
}
</style>

<!--

    <UploadOSS
        ref="uploadOSSRef"
        @on-success="onSuccess"
        :requestStsToken="handleRequestStsToken"
        :maxFileSize="maxFileSize"
    >




    /**
     *
     * @param fileName    格式化好的fileName,不包含路径名
     * @param file        选择的文件
     * @return {Promise}  如果返回结构一直,则不需要作处理,直接返回即可
     */
    handleRequestStsToken(fileName,file){
      const data={
        ownerId: this.ownerId,
        ownerName: this.ownerName,
      }
      return getStsInfoHasDocument(data)
    }


    //数据结构不一致 new 一个Promise出来
    handleRequestStsToken(fileName,file){
      const data={
        ownerId: this.ownerId,
        ownerName: this.ownerName,
        fileName: fileName,
      }
      // return getStsInfoHasDocument(data)
      return new Promise((resolve, reject) => {
        getStsInfoHasDocument(data).then(response => {
          resolve(response)
        }).catch(err => {
          console.log(err)
          reject(err)
        })
      })
    }

-->
