<template>
  <div>
    <el-select v-model="lookupBitValueArray" :placeholder="placeholder"
               :filterable="filterable" multiple
               :clearable="clearable"
               size="small" @change="handleChangeValue"
               :style="styles"
    >
      <el-option
        v-for="item in bitValueOptionList"
        :key="item.key==null?item.dictValue:item.key"
        :label="item.dictLabel"
        :value="item.dictValue"
        :disabled="item.disabled"
        class="item-option"
      >
        <!--              这里显示的不能搜索-->
      </el-option>

    </el-select>
  </div>
</template>
<script>
//多选数组


import { arrayToBitFlag,bitFlagToArray } from '@/utils/wangbao'

export default {
  name: "SelectMultipleToBitValue",
  components: {
  },
  props: {
    value: [Number,String],
    placeholder:{
      type: String,
      default: '请选择'
    },
    //选择的数组,dictValue必须为整数,且为 1<< n位之后的值 1,2,4,8
    bitValueOptionList:{
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
      lookupBitValueArray:[],
      currentValue:0,
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
            this.lookupBitValueArray=bitFlagToArray(val);
            this.currentValue=Number(val);
          }else{
            this.lookupBitValueArray=[];
            this.currentValue=0;
          }
        }
        // console.log("value=",val)
      },
      immediate: true,
    },
    bitValueOptionList:{
      handler(val){

      }
    }
  },
  methods: {
    initComponents(force){
    },
    handleChangeValue(val){
      // console.log("val=",val)
      const bitValue = arrayToBitFlag(this.lookupBitValueArray)
      this.$emit("input", bitValue);
      this.$emit("change",bitValue);
    },
  }

}
</script>

<style lang="scss" >
.item-option span{
  white-space: pre;
}
</style>


<!--
@change方法，保留默认参数再获取到自定义参数

      <el-form-item label="显示筛选" prop="lookupBitFilter">
        <SelectMultipleToBitValue :bitValueOptionList="lookupBitFilterOptionList"
          v-model="queryParams.lookupBitFilter"
          ></SelectMultipleToBitValue>
      </el-form-item>


    import SelectMultipleToBitValue from '@/components/SelectMultipleToBitValue/SelectMultipleToBitValue'

    SelectMultipleToBitValue,

      lookupBitFilterOptionList:[
        {
          dictValue: 1,
          dictLabel:"余量大于0",
        },
        {
          dictValue: 2,
          dictLabel:"选项2",
        },
      ],

    //值的方式
  initLookupBitFilterOptionList(){
    this.lookupBitFilterOptionList=[]
    var dictValueTmp=1;
    for (let i = 1; i <= 30; i++) {
      let tmp={
        dictValue: dictValueTmp,
        dictLabel: `选项${i}[${dictValueTmp}]`
      }
      dictValueTmp=dictValueTmp<<1
      this.lookupBitFilterOptionList.push(tmp)
    }
    console.log(this.lookupBitFilterOptionList)
  },
-->
