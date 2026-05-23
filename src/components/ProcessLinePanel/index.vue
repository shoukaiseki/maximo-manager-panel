<template>
  <div>
    <div v-for="(processNodeTreeInfoPanel,index1) in value" :key="index1" >
      <div class="processPanelDiv" :class="getBadgeClass(processNodeTreeInfoPanel)">
        <div style="min-width:80px;text-align:center;">{{processNodeTreeInfoPanel.nodeLabel}}</div>
		<!--
        <div >{{processNodeTreeInfoPanel.processStatus}}</div>
		-->
<!--        <div >{{getBadgeClass(processNodeTreeInfoPanel)}}</div>-->
        <ProcessLinePanel :badge-class-panel="getBadgeClass(processNodeTreeInfoPanel)" v-if="processNodeTreeInfoPanel.virtualChildrenList" v-model="processNodeTreeInfoPanel.virtualChildrenList">
        </ProcessLinePanel>
      </div>
    </div>
  </div>
</template>
<script>
export default {
  name: "ProcessLinePanel",
  components: {
  },
  props: {
    /* 测试使用 */
    value:{
      type: Array,
      default: () => [],
    }
  },
  data() {
    return {
    }
  },
  computed: {
    size() {
      return 0
    }
  },
  watch: {
    value: {
      handler(val) {
        if (val !== this.currentValue) {
          this.currentValue = val === null ? [] : val;
        }
      },
      immediate: true,
    }
  },
  methods: {
    getBadgeClass: function(processNodeTreeInfoPanel){
      if(processNodeTreeInfoPanel.processStatus==undefined){
        //属于流程设置
        return "badge-info"
      }
      if(processNodeTreeInfoPanel.processStatus>=0&&processNodeTreeInfoPanel.processStatus<=10){
        return "badge-info"
      }
      if(processNodeTreeInfoPanel.processStatus>10){
        return "badge-danger"
      }
      if(processNodeTreeInfoPanel.processStatus==-1){
        return "badge-default"
      }
      if(processNodeTreeInfoPanel.processStatus==-2){
        return "badge-warning"
      }
      return "badge-default"
    }
  }

}
</script>

<style scoped>
div{
  margin:5px;
  float: left;
  text-align:center;
}
.processPanelDiv {
  border:1px solid #ccc;
  /* box-shadow: 0 0 5px #ddd; */
  border-radius: 20px;
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
  background-color: #ABD9DB;
}

.badge-info {
  background-color: #97C4CC;
}

.badge-inverse {
  background-color: #262626;
}

.badge-white {
  background-color: #FFFFFF;
}

.badge-default {
  background-color: #E2F2ED;
}

</style>
