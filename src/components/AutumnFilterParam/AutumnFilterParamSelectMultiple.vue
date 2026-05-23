<template>
    <el-select v-model="currentValueArray" :placeholder="placeholder"
               :filterable="filterable" multiple
               :clearable="clearable"
                @change="handleChangeValue"
               :style="styles"
    >
        <el-option
            v-for="item in optionList"
            :key="item.key==null?item.dictValue:item.key"
            :label="item.dictLabel"
            :value="item.dictValue"
            :disabled="item.disabled"
            class="item-option"
        >
            <!--              这里显示的不能搜索-->
        </el-option>

    </el-select>
</template>

<script>
export default {
    name: "AutumnFilterParamSelectMultiple",
    components: {
    },
    props: {
        value: [String],
        placeholder:{
            type: String,
            default: '请选择'
        },
        //选择的数组,dictValue必须为整数,且为 1<< n位之后的值 1,2,4,8
        optionList:{
            type: Array,
            default: function() {
                return []
            }
        },
        clearable: Boolean,
        filterable:{
            type:Boolean,
            default:true,
        },
        //宽度对于 1000px 无效
        //        100%   有效
        width:[String]

    },
    data() {
        return {
            currentValueArray:[],
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
                        this.currentValueArray=JSON.parse(val);
                        this.currentValue=Number(val);
                    }else{
                        this.currentValueArray=[];
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
            // console.log("val=",val)
            let currentValue=""
            if(this.currentValueArray&&this.currentValueArray.length>0){
                currentValue = JSON.stringify(this.currentValueArray)
            }
            this.currentValue=currentValue
            this.$emit("input", currentValue);
            this.$emit("change",currentValue);
        },
    }

}
</script>

<style scoped>

</style>
