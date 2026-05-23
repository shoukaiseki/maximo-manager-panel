<template>

<!--  <div  class="app-container">-->
  <div  class="app">
    <div v-for="(processNodeTreeInfo,index1) in value" :key="index1" :class="badgeClass">
      <div class="processDiv">
        <div >{{processNodeTreeInfo.nodeName}}</div>
		<!--
        <div >{{processNodeTreeInfo.processProgressId}}</div>
        <div >{{processNodeTreeInfo.processStatus}}</div>
		-->
        <ProcessLinePanel  v-model="processNodeTreeInfo.virtualChildrenList">
        </ProcessLinePanel>
      </div>
    </div>
	</div>
</template>
<script>
import ProcessLinePanel from '@/components/ProcessLinePanel';
export default {
  components: {
    ProcessLinePanel
  },
  props: {
    /* 测试使用 */
    value:{
      type: Array,
      default: () => [],
    },
    badgeClass:{
      type: String,
      default: "badge-white"
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
        console.log("val=",val)
        if (val !== this.currentValue) {
          this.currentValue = val === null ? [] : val;
        }
      },
      immediate: true,
    }
  },
  methods: {
  }

}
</script>
<style scoped>
div {
  margin:5px;
  float: left;
  text-align:center;
}
.processDiv{
  border:1px solid #ccc;
  /* box-shadow: 0 0 5px #ddd; */
  box-sizing: border-box;
  padding: 10px;
  border-radius: 8px;
  background:#ABDBCC;
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
