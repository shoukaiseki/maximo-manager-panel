<template>
	<el-switch
		v-model="currentStatus"
		:disabled="disabled"
		:active-value="activeValue"
		:inactive-value="inactiveValue"
        :inactive-text="inactiveText"
        :active-text="activeText"
		@change="handleChangeStatus()"
	></el-switch>
</template>
<script>

export default {
	name: "StatusSwitch",
	components: {
	},
	props: {
		value: {
			type: [Number,String],
			default:0,
		},
		disabled:{
			type:Boolean,
			default:false
		},
        showText:{
            type:Boolean,
            default:false
        },
		/**
		 * 状态开关类型
		 * 1: 归档开关
		 * 2: 禁用开关
		 */
		type:{
			type:Number,
			default: 1
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
            //开关关闭时候的值
            inactiveText: undefined,
            //开关开启时候的值
            activeText: undefined,
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
			    this.initComponents()
			},
			immediate: true,

		},
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
            if (this.type == 1) {
                this.activeValue=1;
                this.inactiveValue=0;
            }else{
                this.activeValue=0;
                this.inactiveValue=1;
            }
            if(this.showText){
                if (this.type == 1) {
                    this.activeText = '归档'
                    this.inactiveText = ''
                }else if (this.type == 2) {
                        this.activeText='启用'
                        this.inactiveText='禁用'
                }else{
                    this.activeText=''
                    this.inactiveText=''
                }
            }else{
                this.activeText=''
                this.inactiveText=''
            }
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

      <StatusSwitch
        v-model="scope.row.status"
        :disabled="true"
        :type="1"
        @change="handleStatusChange(scope.row)" >
      </StatusSwitch>


    import StatusSwitch from '@/components/DataRowStatus/StatusSwitch'


    StatusSwitch,

		handleChangeStatus(val){
			// console.log("val=",val)
		},

-->
