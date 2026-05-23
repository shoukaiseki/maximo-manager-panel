<template>
    <div>
        <el-row>
            <el-col :span="20">
                <el-select v-model="currentValue" :placeholder="placeholder"
                           :filterable="filterable" multiple
                           :clearable="clearable"
                           size="small" @change="handleChangeValue"
                           :style="styles"
                >
                    <el-option
                        v-for="item in selectedList"
                        :key="item[optionKey]"
                        :label="defaultFormatLabel(item)"
                        :value="item[optionKey]"
                        class="item-option"
                    >
                        <!--              这里显示的不能搜索-->
                    </el-option>
                </el-select>

            </el-col>
            <el-col :span="4">
                <el-button type="primary"
                           @click="buttonClick()"
                           size="mini" >{{buttonText||'选择'}}</el-button>
            </el-col>
        </el-row>
    </div>
</template>
<script>
//多选数组


import { isInArray } from '/src/utils/wangbao-array'
import { findRowInListFromAttrName } from '/src/utils/wangbao'

export default {
    name: "SelectMultipleLookupTable",
    components: {
    },
    props: {
        value: [Array,String],
        placeholder:{
            type: String,
            default: '请选择'
        },
        /**
         * array
         * commaNumber  用逗号分割的数字数组
         * commaString  用逗号分割的字符数组
         ***/
        type: {
            type: String,
            default: 'array',
        },
        clearable: Boolean,
        filterable:{
            type:Boolean,
            default:true,
        },
        //宽度对于 1000px 无效
        //        100%   有效
        width:[String],
        buttonText:[String],
        /**
         * 集合存放key的字段名
         * 例如: userId   depId
         ***/
        optionKey:[String],
        optionLabel:[String],
        formatOptionLabel:{
            type: Function
        },
        //如果对应的选项不存在则查询
        noToQueryFunction:{
           type: Function
        },
    },
    data() {
        return {
            currentValue:[],
            //选择的数组,dictValue必须为整数,且为 1<< n位之后的值 1,2,4,8
            selectedList: [],

            qoLock: false,
        }
    },
    computed: {
        styles(){
            let style = {};
            if (this.width) {
                style.width = `${this.width}`;
            }else{
                style.width = `100%`;
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
    methods: {
        initStart(){
            if (['commaNumber', 'commaString'].indexOf(this.type) > -1) {
                console.log("initStart comma")
                if (!this.value) {
                    this.currentValue=[]
                }else{
                    let tmp = []
                    for (let item of this.value.split(',')) {
                        if(this.type === 'commaNumber'){
                            let a=Number(item)
                            if(!isNaN(a)){
                               tmp.push(a)
                            }
                            continue
                        }
                        if(this.type === 'commaString'){
                            if(item !== ''){
                                tmp.push(item)
                            }
                        }
                    }
                    console.log("tmp",tmp)
                    this.currentValue=tmp
                }
                this.initComponents()
                return
            }

            this.currentValue=this.value
            this.initComponents()
        },
        initComponents(force){
            this.noToQuery()
        },
        noToQuery(){
            if(this.noToQueryFunction){
                let delay=0;
                if(this.qoLock){
                    delay=6000
                }else{
                    this.qoLock=true;
                }
                setTimeout(()=>{
                    for (let key of this.currentValue) {
                        if(!findRowInListFromAttrName(this.selectedList,this.optionKey,key)){
                            this.noToQueryFunction(key).then(row=>{
                                if(!findRowInListFromAttrName(this.selectedList,this.optionKey,row[this.optionKey])){
                                    this.selectedList.push(row)
                                }
                            })
                        }
                    }
                    this.qoLock=false;
                },delay)
            }
        },
        /**
         * 添加已选项
         * @param item
         */
        addSelectedItem(item){
            if(this.optionKey==null){
                console.error("不存在 optionKey 属性")
                return;
            }
            if(!isInArray(this.currentValue,item[this.optionKey])){
                console.log("currentValue",this.currentValue)
                this.currentValue.push(item[this.optionKey])
                this.selectedList.push(item)
                this.onChange()
            }

        },
        handleChangeValue(val){
            // console.log("val=",val)
        },
        buttonClick(){
            this.$emit("click");
        },
        onChange(){
            let value=undefined
            if(this.type==='array'){
               value=this.currentValue
            }else{
                value=this.currentValue.join(",")
            }
            this.$emit("input", value);
            this.$emit("change",value);
        },
        defaultFormatLabel(item){
            if(this.formatOptionLabel===undefined||this.formatOptionLabel===null||this.formatOptionLabel.constructor !== Function){
                return this.optionLabel===undefined?item[this.optionKey]:item[this.optionLabel]
            }
            return this.formatOptionLabel(item)
        }
    },
    watch: {
        type:{
            handler(val) {
                this.initStart()
            },
            immediate: true,
        },
        value: {
            handler(val) {
                this.initStart()
            },
            immediate: true,
        },
        bitValueOptionList:{
            handler(val){

            }
        }
    },

}
</script>

<style lang="scss" >
.item-option span{
    white-space: pre;
}
</style>



<!--
              <SelectMultipleLookupTable
                :formatOptionLabel="formatSelectMultipleLookupTableOptionLabel"
                optionKey="demoSelectSourceId"
                optionLabel="name"
                ref="queryParamsVirtualDemoSelectSourceIdListRef"
                @click="showLookupDemoSelectSource('queryParams.virtualDemoSelectSourceIdList')"
                v-model="queryParams.virtualDemoSelectSourceIdList">
              </SelectMultipleLookupTable>

import SelectMultipleLookupTable from '@/components/SelectMultipleLookupTable'

    SelectMultipleLookupTable,

    formatSelectMultipleLookupTableOptionLabel(item){
      return `${item.id}\t${item.name}`
    },



    //选择框内添加按钮点击后,不同按钮调用根据调用时设置的uid判断
    handleAddButtonLookupDemoSelectSource(row,uid){
      console.log("handleAddButtonLookupDemoSelectSource.row=",row,"uid",uid);
      if(uid=='queryParams.virtualDemoSelectSourceIdList'){
        this.$refs.queryParamsVirtualDemoSelectSourceIdListRef.addSelectedItem(row)
        // this.$forceUpdate();
        return ;
      }
    },
-->
