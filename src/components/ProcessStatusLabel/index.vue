<template>
        <span class="processStatus"
              :class="formatProcessStatusClass(value)"
        >
<!--          {{value}}-->
          {{formatProcessStatusText(value)}}</span>
</template>
<script>


import { findRowInListFromAttrName } from '@/utils/wangbao'

export default {
  name: "ProcessStatusLabel",
  components: {
  },
	props: {
		value: [String, Number],
		noFinish:{
			type:String,
			default:  '进行中'
		},
		finish:{
			type:String,
			default:  '已完成'
		},
		notStarted:{
			type:String,
			default:  '未开始'
		},
		// reworkNoFinish:{
		// 	type:String,
		// 	default:  '[返工]进行中'
		// },
		// reworkFinish:{
		// 	type:String,
		// 	default:  '[返工]已完成'
		// },
		processStatusClassFomrat: Function,
		processStatusTextFomrat: Function,

		shareData: {
			type: Object,
			default: function(){
				return {processStatusOptions:[],
					lock: false,
				}
			}
		},
	},
  data() {
    return {
      // 状态类型字典
      processStatusOptions: [],
      // defaultProcessStatusLabel:{
      //   noFinish: '未完成',
      //   finish: '已完成',
      //   notStarted: '未开始',
      // }
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
      },
      immediate: true,
    },
  },
  methods: {
    initComponents(){
      // console.log("initComponents")
      if(!this.shareData.lock&&(this.shareData.processStatusOptions == undefined||this.shareData.processStatusOptions.length==0)){
        this.shareData.lock=true;
        this.getDicts("process_status_001").then(response => {
          this.shareData.processStatusOptions = response.data;
          this.shareData.lock=false;
        });
      }
    },
    formatProcessStatusClass(val) {
      if(this.processStatusClassFomrat){
        let obj = this.processStatusClassFomrat(val)
        // console.log(obj)
        if(obj!=undefined){
          return obj+'';
        }
      }
      if (val == -1) {
        return 'badge-notStarted'
      }
	    if (val & 2) {
		    return 'badge-rework-noFinish'
	    }
	    if (val & 1) {
	    	if(val&4){
	    		return 'badge-rework-finish'
		    }
		    return 'badge-finish'
	    }
	    if(val&4){
		    return 'badge-rework-noFinish'
	    }
	    return 'badge-noFinish'
    },
    formatProcessStatusText(val) {
      if(this.processStatusTextFomrat){
        let obj = this.processStatusTextFomrat(val)
        // console.log(obj)
        if(obj!=undefined){
          return obj+'';
        }
      }
      // console.log("formatProcessStatusText.val=",val)
      var row=findRowInListFromAttrName(this.shareData.processStatusOptions,'dictValue',val+'')
      // console.log("formatProcessStatusText.row=",row)
      if(row!=undefined){
        return row.dictLabel;
      }
      let status=''
        if(val&8){
            status='[待运送]'
        }
      if (val == undefined) {
        return  status+this.noFinish
      }

      if (val == 0) {
        return status+this.notStarted
      }
        if (val & 2) {
            return status+this.noFinish
        }
        if (val & 1) {
            return status+this.finish
        }
      return  status+this.noFinish
    },
  }

}
</script>


<style lange='less' scoped>
.processStatus {
	margin: 5px;
	font-size: 10px;
//color: red;
	color: #FFFFFF;
}


.badge-primary {
	background-color: #1ab394;
}

.badge-success {
	background-color: #1c84c6;
}

.badge-warning {
	background-color: #f8ac59;
}

.badge-warning-light {
	background-color: #f8ac59;
}

.badge-danger {
	background-color: #ed5565;
}

.badge-info {
	background-color: #23c6c8;
}

.badge-inverse {
	background-color: #262626;
}

.badge-white {
	background-color: #FFFFFF;
}

/*未接单*/
.badge-notStarted {
	background-color: #ed5565;
}
/*进行中*/
.badge-noFinish {
	background-color: #8B4513;
}
/*已完成*/
.badge-finish {
	background-color: #1ab394;
}
/*返工-未完成*/
.badge-rework-noFinish {
	background-color: #f8ac59;
}
/*返工-完成*/
.badge-rework-finish {
	background-color: #ffa500;
}
</style>

<!--

        import ProcessStatusLabel  from '/src/components/ProcessStatusLabel/index'

        ProcessStatusLabel,
-->
