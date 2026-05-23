<template>
    <el-date-picker
        :clearable="clearable"
        v-model="dateRange"
        :size="size"
        :style="{width: width}"
        :valueFormat="valueFormat"
        type="daterange"
        range-separator="-"
        :startPlaceholder="startPlaceholder"
        :endPlaceholder="endPlaceholder"
        @change="handleChangeValue"
    ></el-date-picker>

</template>

<script>
import { Notification, MessageBox, Message } from 'element-ui'

export default {
    name: "AutumnFilterParamDateRange",
    components: {
    },
    props: {
        value: [String],
        startPlaceholder:{
            type: String,
            default: '开始日期'
        },
        endPlaceholder:{
            type: String,
            default: '结束日期'
        },
        valueFormat:{
            type: String,
            default: 'yyyy-MM-dd'
        },
        clearable: Boolean,
        //宽度对于 1000px 无效
        //        100%   有效
        width:[String],
        size: [String],

        /**
         * 1:  yyyy-MM-dd HH:mm:ss
         *     开始时间 00:00:00 - 结束时间 23:59:59
         * 2:  yyyy-MM-dd
         */
        queryFormat: {
          type: [String,Number],
          default: '1',
        },


    },
    data() {
        return {
            dateRange: [],
            currentValue: undefined,
        }
    },
    computed: {
        styles(){
            let style = {};
            if (this.width) {
                style.width = `${this.width}`;
            }
            return style;
        },
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
                if (val != this.currentValue) {
                    if (val != undefined) {
                        this.dateRange=JSON.parse(val);
                        this.currentValue=Number(val);
                    }else{
                        this.dateRange=[];
                        this.currentValue=undefined;
                    }
                }
                // console.log("value=",val)
            },
            immediate: true,
        },
        optionList:{
            handler(val){

            }
        }
    },
    methods: {
        initComponents(force){
        },
        handleChangeValue(val){
            console.log("val=",this.dateRange)
            let currentValue=""
            if(this.dateRange&&this.dateRange.length>0){
                if(this.queryFormat==1){
                    currentValue = JSON.stringify([
                        this.formatToAutumnJson(this.dateRange[0]),
                        this.formatToAutumnJson(this.dateRange[1],' 23:59:59'),
                    ])
                }else if(this.queryFormat==2){
                    currentValue = JSON.stringify([
                        this.dateRange[0],
                        this.dateRange[1],
                    ])
                }else{
                    const msg="AutumnFilterParamDateRange 未定义的 queryFormat"
                    Message({
                        message: msg,
                        type: 'error'
                    })
                    return
                }
            }
            this.currentValue=currentValue
            this.$emit("input", currentValue);
            this.$emit("change",currentValue);
        },

        formatToAutumnJson(tmp,suffix=" 00:00:00"){
            if(tmp!=null){
                if (tmp.length == 10) {
                    return tmp+suffix
                }
            }
            return undefined
        }
    }

}
</script>

<style scoped>

</style>
