<template>
  <div class="json-graph-view">
    <div v-if="!data" class="json-graph-empty">无数据</div>
    <json-graph-node
      v-else
      :value="data"
      :name="rootName || 'root'"
      :depth="0"
      :is-root="true"
    />
  </div>
</template>

<script>
import JsonGraphNode from './JsonGraphNode.vue'

export default {
  name: 'JsonGraphView',
  components: { JsonGraphNode },
  props: {
    data: { type: [Object, Array, String, Number, Boolean], default: null },
    rootName: { type: String, default: '' }
  }
}
</script>

<style scoped>
.json-graph-view {
  font-family: 'Courier New', Courier, monospace;
  font-size: 13px;
  line-height: 1.6;
  padding: 4px 0;
}
.json-graph-empty {
  color: #999;
  text-align: center;
  padding: 20px;
}
</style>
