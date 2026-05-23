<template>
	<el-switch
		v-model="currentStatus"
		:disabled="disabled"
		:active-value="activeValue"
		:inactive-value="inactiveValue"
		@change="handleChangeStatus()"
	></el-switch>
</template>
<script>

export default {
	name: "UrgentedSwitch",
	components: {
	},
	props: {
		value: {
		  type: Number,
      default:0,
    },
		disabled:{
			type:Boolean,
			default:false
		},
	},
	data() {
		return {
			defaultStatus:0,
			//数字类型
			currentStatus:this.defaultStatus,
			//开关关闭时候的值
			inactiveValue:0,
			//开关开启时候的值
			activeValue:1,
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
            this.currentStatus=this.defaultStatus
              this.handleChangeStatus(this.currentStatus);
          }else{
            this.currentStatus= Number(val);
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
		handleChangeStatus(val){
			// console.log("val=",val)
			this.$emit("input", this.currentStatus);
			this.$emit("change",this.currentStatus);
		},
	}

}
</script>
<!--

      <UrgentedSwitch
        v-model="scope.row.status"
        :disabled="true"
        @change="handleStatusChange(scope.row)" >
      </UrgentedSwitch>


    import UrgentedSwitch from '@/components/DemoDataRowStatus/UrgentedSwitch'


    StatusSwitch,

		handleChangeStatus(val){
			// console.log("val=",val)
		},

-->
