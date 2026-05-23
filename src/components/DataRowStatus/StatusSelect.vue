<template>
    <el-select
        v-model="currentStatus"
        clearable
        size="small"
        style="width: 240px"
        :disabled="disabled"
        @change="handleChangeStatus"
    >
        <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
    </el-select>
</template>
<script>

export default {
    name: "StatusSelect",
    components: {
    },
    props: {
        value: [String, Number],
        disabled:{
            type:Boolean,
            default:false
        },
        /**
         * 状态开关类型
         * 1: 归档开关
         * 2: 禁用开关
         * 3: 是否生效开关
         * 4: 是否提交  已提交/未提交
         */
        type:{
            type:Number,
            default: 1
        },
    },
    data() {
        return {
            defaultStatus:undefined,
            //字符类型
            currentStatus:this.defaultStatus,
            // 状态类型字典
            statusOptions: [],
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
        type:{
            handler(val) {
                this.initSwitchActiveValue();
            },
            immediate: true,

        },
        value: {
            handler(val) {
                if (val != this.currentValue) {
                    if(val === null||val===undefined){
                        this.currentStatus=val
                    }else{
                        this.currentStatus= (val+'');
                    }

                }
            },
            immediate: true,
        },
    },
    methods: {
        initComponents(){
            // console.log("initComponents")
        },
        initSwitchActiveValue(){
            if (this.type == 1) {
                this.statusOptions = [
                    {
                        value: '',
                        label: '全部'
                    },
                    {
                        value: '0',
                        label: '未归档'
                    },
                    {
                        value: '1',
                        label: '已归档'
                    },
                ]
            }else if (this.type == 2) {
                this.statusOptions=[
                    {
                        value: '',
                        label: '全部'
                    },
                    {
                        value: '0',
                        label: '启用'
                    },
                    {
                        value: '1',
                        label: '禁用'
                    },
                ]
            }else if (this.type == 3) {
                this.statusOptions=[
                    {
                        value: '',
                        label: '全部'
                    },
                    {
                        value: '0',
                        label: '已生效'
                    },
                    {
                        value: '1',
                        label: '未生效'
                    },
                ]
            }else if (this.type == 4) {
                this.statusOptions=[
                    {
                        value: '',
                        label: '全部'
                    },
                    {
                        value: '0',
                        label: '已提交'
                    },
                    {
                        value: '1',
                        label: '未提交'
                    },
                ]
            } else {
                this.statusOptions=[
                    {
                        value: '',
                        label: '全部'
                    },
                    {
                        value: '0',
                        label: '完成'
                    },
                    {
                        value: '1',
                        label: '未完成'
                    },
                ]
            }
        },
        handleChangeStatus(val){
            // let currentStatus = this.currentStatus===''?null:this.currentStatus
            console.log("val=",val)
            let currentStatus = this.currentStatus
            console.log("currentStatus=",currentStatus)
            this.$emit("input", currentStatus);
            this.$emit("change",currentStatus);
        },
    }

}
</script>


<style lange='less' scoped>
</style>

<!--
                <StatusSelect
                  v-if="isSystem"
                  v-model="queryParams.status"
                  :type="1"
                  @keyup.enter.native="handleQuery" >
                </StatusSelect>

            import StatusSelect from '@/components/DataRowStatus/StatusSelect'

            StatusSelect,
-->
