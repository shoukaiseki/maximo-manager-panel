<template>
  <g>
    <!-- 开始 -->
    <template v-if="nodeType === 'WFSTART'">
      <circle cx="16" cy="16" r="15" fill="#fff" stroke="#5a5a5a" stroke-width="1.5" />
      <polygon points="11,9 23,16 11,23" fill="#5aa700" />
    </template>
    <!-- 停止 -->
    <template v-else-if="nodeType === 'WFSTOP'">
      <circle cx="16" cy="16" r="15" fill="#fff" stroke="#5a5a5a" stroke-width="1.5" />
      <rect x="10" y="10" width="12" height="12" fill="#cc6666" />
    </template>
    <!-- 任务 -->
    <template v-else-if="nodeType === 'WFTASK'">
      <rect width="54" height="32" fill="#76ade5" stroke="#5a5a5a" stroke-width="1" />
    </template>
    <!-- 子流程 -->
    <template v-else-if="nodeType === 'WFSUBPROCESS'">
      <rect width="54" height="32" fill="#ab8fcc" stroke="#5a5a5a" stroke-width="1" />
      <rect x="7" y="1" width="2" height="30" fill="#fff" />
      <rect x="45" y="1" width="2" height="30" fill="#fff" />
    </template>
    <!-- 等待 -->
    <template v-else-if="nodeType === 'WFWAIT'">
      <path d="M0,0 H37 Q54,0 54,16 Q54,32 37,32 H0 Z" fill="#c4b08c" stroke="#777677" stroke-width="1" />
    </template>
    <!-- 交互 -->
    <template v-else-if="nodeType === 'WFINTERACTION'">
      <polygon points="10,0 54,0 44,32 0,32" fill="#7accbc" stroke="#5a5a5a" stroke-width="1" />
    </template>
    <!-- 条件 -->
    <template v-else-if="nodeType === 'WFCONDITION'">
      <polygon points="27,0 54,16 27,32 0,16" fill="#eacf60" stroke="#777677" stroke-width="1" />
    </template>
    <!-- 输入 -->
    <template v-else-if="nodeType === 'WFINPUT'">
      <polygon points="15,0 54,0 39,32 0,32" fill="#f0f2f4" stroke="#5a5a5a" stroke-width="1" />
    </template>
    <!-- 兜底矩形 -->
    <template v-else>
      <rect width="54" height="32" fill="#dcdfe6" stroke="#5a5a5a" stroke-width="1" />
    </template>
  </g>
</template>

<script>
/**
 * 工作流节点形状(尺寸与配色对应 Maximo 设计器 miniapps/wfdesigner/WFBoxes.xml)
 * 画布节点与「拖拽新增节点」图标面板共用, 避免形状定义维护两份
 * 形状画在局部坐标 (0,0)-(54,32) 内, 由调用方用父级 <g transform> 定位/缩放
 */
export default {
  name: 'WfNodeShape',
  props: {
    nodeType: {
      type: String,
      default: ''
    }
  }
}
</script>
