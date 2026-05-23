<template>
    <el-checkbox
        v-model="checkboxValue"
        :disabled="disabled"
        trueLabel="jjkds"
        @change="handleChangeValue()"
    >{{label}}</el-checkbox>
    <!--  :true-value="activeValue"-->
    <!--  :false-value="inactiveValue"-->
</template>
<script>

export default {
    name: "SksCheckbox",
    components: {
    },
    props: {
        value: [String,Number],
        disabled:{
            type:Boolean,
            default:false
        },
        //显示的标签
        label:[String,Number],
        //没勾选的值
        inactiveValue:[String,Number],
        //勾选的值
        activeValue:[String,Number],
    },
    data() {
        return {
            //类型
            currentValue:this.defaultInactiveValue,
            checkboxValue: false,
            //没勾选的值
            defaultInactiveValue:'0',
            //勾选的值
            defaultActiveValue:'1',
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
                // console.log("value=",this.value)
                // console.log("val=",val)
                if (val !== this.currentValue) {
                    if(val === null){
                        this.convertCurrentValue()
                        this.handleChangeValue(this.currentValue);
                    }else{
                        if(this.inactiveValue===undefined){
                            if(val.constructor === Number){
                                this.defaultActiveValue=1
                                this.defaultInactiveValue=0
                            }
                        }
                        this.currentValue= val;
                    }
                    this.convertCheckboxValue();
                }
            },
            immediate: true,
        },
    },
    methods: {
        convertCurrentValue(){
            if(this.inactiveValue===undefined){
                this.currentValue=this.checkboxValue?this.defaultActiveValue:this.defaultInactiveValue
            }else{
                this.currentValue=this.checkboxValue?this.activeValue:this.inactiveValue
            }
        },
        convertCheckboxValue(){
            if(this.inactiveValue===undefined){
                console.log("convertCheckboxValue.inactiveValue= undefined")
                this.checkboxValue=this.currentValue===this.defaultActiveValue
            }else{
                this.checkboxValue=this.currentValue===this.activeValue
            }
        },
        initComponents(){
            // console.log("initComponents")
        },
        handleChangeValue(val){
            this.convertCurrentValue()
            // console.log("val=",val)
            this.$emit("input", this.currentValue);
            this.$emit("change",this.currentValue);
        },
    }

}
</script>
<!--

    <SksCheckbox label="" v-model="form.lockTotalCost">
    </SksCheckbox>


    import SksCheckbox from '@/components/SksCheckbox/SksCheckbox'

    SksCheckbox,

-->
