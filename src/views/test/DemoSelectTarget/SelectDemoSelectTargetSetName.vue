<template>
    <div>
        <el-select v-model="currentValue" :placeholder="placeholder" clearable size="small" @change="handleChangeValue">
            <el-option
                    v-for="item in searchList"
                    :key="item.demoSelectTargetId"
                    :label="item.name"
                    :value="item.demoSelectTargetId"
            />
            <!-- item.name 改成要显示的字段名 -->
        </el-select>
    </div>
</template>
<script>


import { lookupDemoSelectTarget } from "@/api/test/DemoSelectTarget";
import { findRowInListFromAttrName } from '@/utils/wangbao'
import { valueEquals } from 'element-ui/src/utils/util'

export default {
    name: "SelectDemoSelectTargetSetName",
    components: {
    },
    props: {
        value: [String,Number],
        placeholder:{
            type: String,
            default: '请选择'
        },
        DemoSelectTargetList:{
            type: Array,
            default: null
        },
        //父组建选择对象所在行数据
        rowData:[Object],
        //父组建选择对象所在行数据中要设置显示值的字段
        //如果rowData为空则该属性无作用,rowData不为空则必须要设置该属性
        labelName:[String],

        //list过滤信息
        //未使用自定义 list时才有效果
        queryParams:[Object],
    },
    data() {
        return {
            searchList:[],
            currentValue: "",

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

        this.$nextTick(function() {
            this.$on('initComponents', function() {
                // console.log('我是子组件方法');
            });
        });
    },
    watch: {
        value: {
            handler(val) {
                if (val !== this.currentValue) {
                    this.currentValue = val === null ? "" : val;
                }
            },
            immediate: true,
        },
    },
    methods: {
        initComponents(){
            this.searchList=this.DemoSelectTargetList;
            if(this.DemoSelectTargetList==null){
                lookupDemoSelectTarget(this.queryParams).then(res=>{
                    this.searchList = res.rows;
                })
            }
        },
        handleChangeValue(val){
            this.currentValue=val;
            // console.log("labelName=",this.labelName)
            // console.log("rowData=",this.rowData)
            let row = findRowInListFromAttrName(this.searchList,'demoSelectTargetId',this.currentValue)||{};
            if(this.rowData!==undefined && this.rowData.constructor === Object){
                this.rowData[this.labelName] = row.name;
                //row.name 改成要设置到父组建的字段名
                // console.log("rowData2=",this.rowData)
            }
            if (!valueEquals(this.value, this.currentValue)) {
                this.$emit("input", this.currentValue);
                // this.$emit('update', val);
                // this.$emit('change', val);
                this.$emit("change",this.currentValue,row);
            }
        },
    }

}
</script>

<style scoped>

</style>
