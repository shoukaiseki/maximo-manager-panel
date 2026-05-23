<template>
    <div class="app-container home">

        <div v-if="isAdmin">
            <el-button type="cyan" @click="jump()">组件DEMO首页</el-button>

            <el-button type="cyan" @click="test001()">test001</el-button>

        </div>
        <el-row>
            <el-button type="cyan" @click="initPage()">刷新首页</el-button>
        </el-row>
        <el-row>
            <el-col :span="6">
            </el-col>
            <el-col :span="6">
            </el-col>
            <el-col :span="6">
            </el-col>
            <el-col :span="6">
            </el-col>
        </el-row>

    </div>
</template>

<script>

import request from '/src/utils/request'
import { isNullOrUndefined } from '../utils/sks'


export default {
    name: "sksComponentIndex",
    components: {
    },
    data() {
        return {
            scanForm:{
                qrCode: undefined,
            },
            taskDomain:{
                unAllocatedRequestTotal: 0,
            },
            defaultTaskDomain:{
                unAllocatedRequestTotal: 0,
            },
            //是否为超管
            isAdmin: true,
            // 版本号
            version: "3.2.1",
            testSendToWebSocketDomain:"测试后台发送消息给前端的方式,现在后台默认设置发送给登录人",
        };
    },
    created() {
        console.log("index.created")
        this.taskDomain=this.mergeFromObject({},this.defaultTaskDomain)
        this.initPage();
    },
    activated() {
        this.initPage();
    },
    destroyed() {

    },
    methods: {
        initPage(){
            // const queryObjectList=[
            //     {
            //         resultName: "mainList",
            //         qoName: 'inventoryReserveUnAllocatedRequestQO',
            //         qoJson:{
            //             pageNum: -1,
            //             pageSize: 0,
            //         },
            //         qoChilds:[
            //         ],
            //     }
            // ]
            //
            // this.aqoeous(queryObjectList).then(response=>{
            //     this.taskDomain.unAllocatedRequestTotal=response.data.mainList.total
            // })

        },

        goTarget(href) {
            window.open(href, "_blank");
        },
        jump(){
            this.$router.push({
                path: "/wb-components/index"
            })

        },
        test001(){
            let a={
                id: -1,
                a: false,
                b: null,
            }
            let aa=undefined;
             a=aa;
            console.log("aa",a?.id)
            if(a?.id){
                console.log("test001 true")
            }else{
                console.log("test001 false")
            }
            if(a?.a){
                console.log("test001 a true")
            }else{
                console.log("test001 a false")
            }
            if(a?.b){
                console.log(`test001 b true`)
            }else{
                console.log(`test001 b false`)
            }
        },
        testIf(val,aliasName){

        },




    },
};
</script>

<style scoped lang="scss">
.home {



}
</style>

