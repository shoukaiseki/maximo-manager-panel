<template>
    <div>
        <!-- 提示对话框 -->
        <el-dialog :close-on-click-modal="false" :title="title" :visible.sync="showDialog" :width="width" append-to-body>
            <el-form ref="formRef" :model="form" :rules="rules" :label-width="labelWidth">
                <slot></slot>
                <el-form-item v-if="!customed" :label="itemLabel" prop="currentValue">
                    <el-input  v-if="valueType=='1'" @input="(val)=>inputRestrictedDecimal({value:val,rowData:form,attrName:'currentValue',precision:2,min:0})"  v-model="form.currentValue" >
                        <template slot="prepend" >{{form.itemPrepend}}</template>
                    </el-input>
                    <el-input  type="textarea" v-if="valueType=='3'"  v-model="form.currentValue" >
                    </el-input>
                    <el-select
                        v-if="valueType=='4'"
                        v-model="form.currentValue"
                        clearable
                        size="small"
                        style="width: 240px"
                    >
                        <el-option v-for="item in selectOptions" :key="item.dictValue" :disabled="item.disabled" :label="item.dictLabel" :value="item.dictValue" />
                    </el-select>
                </el-form-item>
            </el-form>
            <div slot="footer" class="dialog-footer">
                <el-button type="primary" @click="submitForm">{{confirmButtonText}}</el-button>
                <el-button @click="cancel">{{cancelButtonText}}</el-button>
            </div>
        </el-dialog>
    </div>

</template>

<script>
//提示对话框
export default {
    name: "PromptDialog",
    props:{
        labelWidth:{
            type: String,
            default: '60px',
        },
        width:{
          type: String,
          default: '400px',
        },
        customed:{
            type:Boolean,
            default: false
        },
        closeOnClickModal:{
            type:Boolean,
            default: false
        },
        confirmButtonText:{
            type: String,
            default: "提交"
        },
        cancelButtonText:{
            type: String,
            default: '取消',
        },
        itemLabel:{
            type:String,
            default: "",
        },
        selectOptions:[Array],
        //输入框类型: 1: 正数数字,2位小数,3:多行文本框,4:下拉选择框
        valueType:{
            type:String,
            default: "1",
        },
        rules:{
            type:Object,
            default:function () {
                return {
                    currentValue: [
                        { required: true, message: "不能为空", trigger: "blur" }
                    ],
                }
            }
        },
        title:{
            type:String,
            default: "请输入"
        }
    },
    data(){
        return{
            showDialog: false,
            form:{
                currentValue: undefined,
                itemPrepend: undefined,
            },
            success: undefined,
        }
    },
    created() {
    },
    methods:{
        reset(){

        },
        /**
         * @param currentValue
         * @param itemPrepend
         */
        show({currentValue,itemPrepend,success}){
            this.form.itemPrepend=itemPrepend
            this.success=success
            this.showDialog=true;
            this.form.currentValue=currentValue

        },
        submitForm(){
            this.$refs.formRef.validate((valid)=>{
                if (valid) {
                    if (this.success&&this.success.constructor === Function) {
                        this.success(this.form.currentValue)
                    }else{
                        this.$emit("submit",this.form.currentValue)
                    }

                    this.showDialog=false;
                }
            })
        },
        cancel(){
            this.showDialog=false;
        },
    }

}
</script>

<style scoped>

</style>

<!--
//该方式已不推荐
            @submit="handlePromptDialogSubmit"
        <PromptDialog
            title="确定进行操作吗?"
            confirmButtonText="确定"
            itemLabel="选择"
            value-type="4"
            :selectOptions="promptDialogSelectOptions"
            ref="promptDialogRef"
        ></PromptDialog>

    import PromptDialog from '/src/components/PromptDialog'
    PromptDialog,

-->
