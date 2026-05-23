<template>
    <div>
        <el-select
            ref="selectDictOrCreateRef"
            v-model="currentValue"
            clearable
            filterable
            size="small"
            :style="{width: width}"
            :disabled="disabled"
            :allow-create="true"
            @change="handleChangeValue"
        >
            <el-option :disabled="disabled" v-for="item in dictOptions" :key="item.dictValue" :label="item.dictLabel" :value="item.dictValue" />
        </el-select>
    </div>
</template>
<script>

import request from "../../utils/request";
import {findRowInListFromAttrName} from "../../utils/wangbao";

export async function addData(data) {
    return await request({
        url: '/system/dict/data',
        method: 'post',
        data: data
    })
}

export default {
    name: "SelectDictOrCreate",
    components: {
    },
    props: {
        value: [String],
        dictType:[String],
        disabled:{
            type:Boolean,
            default:false
        },
        width:{
            type: String,
            default: '100%'
        }
    },
    data() {
        return {
            //字符类型
            currentValue: undefined,
            // 状态类型字典
            dictOptions: [],
            //初始化完成标志, dictOptions 初始化完成置位
            initFlag: false,
        }
    },
    computed: {
        size() {
            return 0
        }
    },
    created() {
        this.initComponents();
    },
    mounted() {
        this.initComponents();
    },
    watch: {
        value: {
            handler(val) {
                if(val !== this.currentValue){
                    this.currentValue = val === null ? "" : val;
                    this.$nextTick(()=>{
                        this.handleChangeValue(this.currentValue)
                    })
                }
            },
            immediate: true,
        },
        dictType: {
            handler(val) {
                this.getDictList()
            },
            immediate: true,
        },
    },
    methods: {
        initComponents(){
            // console.log("initComponents")
        },
        async getDictList(){
            this.dictOptions=[];
            if(this.dictType){
             await this.getDicts(this.dictType).then(response => {
                    this.dictOptions = response.data;
                    this.initFlag=true
                });
            }
        },
        handleChangeValue(val){
            console.log("handleChangeValue",val)

            if(this.initFlag&&this.currentValue){
                let row = findRowInListFromAttrName(this.dictOptions,'dictValue',this.currentValue);
                if (!row) {
                    if(this.dictType){
                        const data={
                            dictValue: this.currentValue,
                            dictLabel: this.currentValue,
                            dictType: this.dictType,
                        }
                        addData(data).then(response=>{
                            this.msgSuccess("新增成功")
                            this.dictOptions.push(data);
                        }).catch(error=>{
                            this.msgError("新增失败")
                            this.currentValue=""
                        })
                    }else{
                        this.msgError("新增失败,组件中未定义 dictType")
                        this.currentValue=""
                    }
                }
            }
            // let currentStatus = this.currentStatus===''?null:this.currentStatus
            console.log("val=",val)
            let currentValue = this.currentValue
            console.log("currentValue=",currentValue)
            // this.$emit("input", currentValue);
            // this.$emit("change",currentValue);
        },
    }

}
</script>


<style lange='less' scoped>
</style>

<!--
            <SelectDictOrCreate dictType="inventory_item_unit"
            v-model="form.itemUnit" ></SelectDictOrCreate>

            import SelectDictOrCreate from '@/components/SelectDictOrCreate/SelectDictOrCreate'

            SelectDictOrCreate,
-->
