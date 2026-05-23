<template>
    <div>
        <div  v-if="type==2" >
            {{nickName}}
        </div>
        <el-input :readonly="readonly" :disabled="disabled" v-if="type==1"  v-model="nickName">
            <template slot="prepend" >{{userId}}</template>
<!--            <el-button  readonly type="primary" slot="suffix"   size="mini"  @click="showLookupSysUser('queryParams.userId')">选择</el-button>-->
            <slot name="prefix"></slot>
        </el-input>
    </div>

</template>

<script>
// import { findRowInListFromAttrName } from '/src/utils/sks'

export default {
    name: 'SysUserNickNameInput',
    props:{
        /**
         * 绑定类型,因为 value 绑定的值可能为 null
         * 1: 绑定的为 value
         * 2: 绑定的为 domain
         */
        bindingType:{
            type: String,
            default: "1",
        },
        //如果该字段有值,则根据该字段为基准
        value:[Number,String],
        //value 为null时,使用该配置
        domain:[Object],
        //属性名称,domain绑定时,必须设置该字段名,
        attrName:[String],
        //userId=0或空 时显示的字符
        nullText:{
            type:String,
            default: ''
        },
        readonly:{
            type:Boolean,
            default: true,
        },
        disabled:{
            type:Boolean,
            default: false,
        },
        /**
         * 1  input
         * 2  div  用于列表显示
         */
        type:{
            type:Number,
            default: 1,
        },
    },
    data() {
        return {
            nickName: null,
            userId: undefined,
        }
    },
    created() {
    },
    methods:{
        initStart(){
            // console.log("domain",this.domain)
            // console.log("value",this.value)
            this.nickName=this.nullText
            let value=undefined;
            if(this.bindingType==="1"){
                value=this.value
            }else{
                if (this.domain) {
                    if (!this.attrName) {
                        this.msgError("SysUserNickName 组建的domain不为空时 attrName不能为空")
                    }
                    try{
                        value=this.domain[this.attrName]
                    }catch (error){
                    }
                }
            }

            if(!value){
                this.userId=undefined
                return
            }
            this.userId=isNaN(value)?undefined:Number(value)
            // console.log("initStart",this.userId)
            this.$nextTick(()=>{
                this.formatNickName()
            })

        },
        formatNickName(){
            // let userList = this.$store.state.user.userList
            // let userId = this.$store.state.user.userId
            // console.log("formatNickName.userList",userList,userId)
            // console.log("formatNickName.store.state",this.$store.state)
            // if(userList){
            //     let row = userList[this.userId+""]
            //     console.log("formatNickName.row",row)
            //     if(row){
            //         this.nickName=row.nickName
            //         return
            //     }
            // }

            // console.log("GetSysUser start",this.userId)
            this.$store.dispatch("GetSysUser", this.userId)
                .then((row) => {
                    // console.log("GetSysUser call")
                    this.nickName= row.nickName
                })
                .catch(() => {
                });
        }
    },
    watch:{
        domain:{
            handler(val) {
                this.initStart();
            },
            immediate: true
        },
        attrName:{
            handler(val) {
                this.initStart();
            },
            immediate: true
        },
        value:{
            handler(val) {
                this.initStart();
            },
            immediate: true
        },
        bindingType:{
            handler(val) {
                this.initStart();
            },
            immediate: true
        },
        nullText:{
            handler(val) {
                this.initStart();
            },
            immediate: true
        }
    },

}
</script>

<style scoped>

</style>

<!--

    <SysUserNickNameInput v-model="userId" :type="1">
    </SysUserNickNameInput>
    <SysUserNickNameInput v-model="userId" :type="2">
    </SysUserNickNameInput>
    <SysUserNickNameInput readonly bindingType="2" :domain="inventoryOrderPrintTable.currentRow" attrName="operUserId">
    </SysUserNickNameInput>

    import SysUserNickNameInput from '/src/components/SysUserNickName/SysUserNickNameInput'
    SysUserNickNameInput,
-->
