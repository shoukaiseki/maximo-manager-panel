<template>
  <section class="wf-detail-page" v-loading="loading">
    <el-card>
      <!-- 头部: 返回 + 主信息 + 编辑/保存/取消 -->
      <div class="detail-header">
        <div class="header-left">
          <el-button icon="el-icon-arrow-left" size="mini" @click="goBack">返回</el-button>
          <div class="header-title">
            <h2>{{ workflow.processName }} <span class="rev">v{{ workflow.processRev }}</span></h2>
            <p class="page-summary">{{ workflow.description || '无描述' }}</p>
          </div>
        </div>
        <div class="header-actions">
          <template v-if="!editMode">
            <el-button type="primary" size="mini" icon="el-icon-edit" @click="handleEdit">编 辑</el-button>
          </template>
          <template v-else>
            <el-button type="success" size="mini" icon="el-icon-check" @click="handleSave">保 存</el-button>
            <el-button size="mini" @click="cancelEdit">取 消</el-button>
          </template>
        </div>
      </div>

      <el-descriptions :column="4" border size="small" class="wf-desc">
        <el-descriptions-item label="主对象">{{ workflow.objectName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="启用">
          <el-tag :type="workflow.enabled ? 'success' : 'info'" size="mini">{{ boolText(workflow.enabled) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="激活">
          <el-tag :type="workflow.active ? 'success' : 'info'" size="mini">{{ boolText(workflow.active) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="自动启动">{{ boolText(workflow.autoInitiate) }}</el-descriptions-item>
      </el-descriptions>

      <el-alert
        v-if="editMode"
        class="edit-tip"
        title="编辑功能正在开发中，当前仍为只读预览，保存暂未开放。"
        type="warning"
        :closable="false"
        show-icon />

      <el-tabs v-model="activeTab" class="detail-tabs">
        <!-- ============ 画布 Tab ============ -->
        <el-tab-pane label="流程图" name="canvas">
          <div class="canvas-toolbar">
            <el-button-group>
              <el-button size="mini" icon="el-icon-zoom-out" @click="zoomOut" title="缩小"></el-button>
              <el-button size="mini" @click="resetZoom">{{ Math.round(zoom * 100) }}%</el-button>
              <el-button size="mini" icon="el-icon-zoom-in" @click="zoomIn" title="放大"></el-button>
              <el-button size="mini" icon="el-icon-full-screen" @click="fitZoom" title="适应宽度"></el-button>
            </el-button-group>
            <span class="canvas-legend">
              <i class="legend-item"><span class="line-pos"></span>正向连线</i>
              <i class="legend-item"><span class="line-neg"></span>负向连线</i>
              <span class="canvas-tip">点击节点查看节点详情{{ editMode ? '（编辑模式：节点可拖拽，暂未开放）' : '' }}</span>
            </span>
          </div>
          <div class="canvas-scroll" ref="canvasScroll">
            <div class="canvas-inner" :style="innerStyle">
              <svg :width="canvasSize.width" :height="canvasSize.height" class="wf-svg">
                <defs>
                  <marker id="arrow-pos" markerWidth="10" markerHeight="10" refX="8" refY="3" orient="auto" markerUnits="strokeWidth">
                    <path d="M0,0 L8,3 L0,6 z" fill="#5a5a5a" />
                  </marker>
                  <marker id="arrow-neg" markerWidth="10" markerHeight="10" refX="8" refY="3" orient="auto" markerUnits="strokeWidth">
                    <path d="M0,0 L8,3 L0,6 z" fill="#f00" />
                  </marker>
                </defs>

                <!-- 连线 -->
                <g class="edges">
                  <template v-for="edge in edges">
                    <line
                      :key="'e-' + edge.from + '-' + edge.actionId"
                      :x1="edge.x1" :y1="edge.y1" :x2="edge.x2" :y2="edge.y2"
                      :stroke="edge.isPositive === false ? '#f00' : '#5a5a5a'"
                      :stroke-width="edge.isPositive === false ? 1.5 : 1.2"
                      :marker-end="edge.isPositive === false ? 'url(#arrow-neg)' : 'url(#arrow-pos)'" />
                    <g v-if="edge.label" :key="'l-' + edge.from + '-' + edge.actionId" :transform="'translate(' + edge.lx + ',' + edge.ly + ')'">
                      <rect :x="-(edge.labelW / 2)" y="-9" :width="edge.labelW + 8" height="16" rx="2" fill="#fff" stroke="#c0c4cc" stroke-width="0.6" />
                      <text x="0" y="3" font-size="10" fill="#606266" text-anchor="middle">{{ edge.label }}</text>
                    </g>
                  </template>
                </g>

                <!-- 节点 -->
                <g
                  v-for="node in workflow.wfnodes"
                  :key="'n-' + node.nodeId"
                  :transform="'translate(' + (node.x * GRID) + ',' + (node.y * GRID) + ')'"
                  class="wf-node" :class="{ selected: selectedNodeId === node.nodeId, editable: editMode }"
                  @click="selectNode(node)">
                  <!-- 选中环 -->
                  <rect v-if="selectedNodeId === node.nodeId" x="-4" y="-4" :width="shapeBox(node).w + 8" :height="shapeBox(node).h + 18"
                        rx="4" fill="none" stroke="#409eff" stroke-width="2" stroke-dasharray="4,2" />

                  <!-- 开始 -->
                  <template v-if="node.nodeType === 'WFSTART'">
                    <circle cx="16" cy="16" r="15" fill="#fff" stroke="#5a5a5a" stroke-width="1.5" />
                    <polygon points="11,9 23,16 11,23" fill="#5aa700" />
                  </template>
                  <!-- 停止 -->
                  <template v-else-if="node.nodeType === 'WFSTOP'">
                    <circle cx="16" cy="16" r="15" fill="#fff" stroke="#5a5a5a" stroke-width="1.5" />
                    <rect x="10" y="10" width="12" height="12" fill="#cc6666" />
                  </template>
                  <!-- 任务 -->
                  <template v-else-if="node.nodeType === 'WFTASK'">
                    <rect width="54" height="32" fill="#76ade5" stroke="#5a5a5a" stroke-width="1" />
                  </template>
                  <!-- 子流程 -->
                  <template v-else-if="node.nodeType === 'WFSUBPROCESS'">
                    <rect width="54" height="32" fill="#ab8fcc" stroke="#5a5a5a" stroke-width="1" />
                    <rect x="7" y="1" width="2" height="30" fill="#fff" />
                    <rect x="45" y="1" width="2" height="30" fill="#fff" />
                  </template>
                  <!-- 等待 -->
                  <template v-else-if="node.nodeType === 'WFWAIT'">
                    <path d="M0,0 H37 Q54,0 54,16 Q54,32 37,32 H0 Z" fill="#c4b08c" stroke="#777677" stroke-width="1" />
                  </template>
                  <!-- 交互 -->
                  <template v-else-if="node.nodeType === 'WFINTERACTION'">
                    <polygon points="10,0 54,0 44,32 0,32" fill="#7accbc" stroke="#5a5a5a" stroke-width="1" />
                  </template>
                  <!-- 条件 -->
                  <template v-else-if="node.nodeType === 'WFCONDITION'">
                    <polygon points="27,0 54,16 27,32 0,16" fill="#eacf60" stroke="#777677" stroke-width="1" />
                  </template>
                  <!-- 输入 -->
                  <template v-else-if="node.nodeType === 'WFINPUT'">
                    <polygon points="15,0 54,0 39,32 0,32" fill="#f0f2f4" stroke="#5a5a5a" stroke-width="1" />
                  </template>
                  <!-- 兜底矩形 -->
                  <template v-else>
                    <rect width="54" height="32" fill="#dcdfe6" stroke="#5a5a5a" stroke-width="1" />
                  </template>

                  <text x="27" y="48" font-size="11" fill="#303133" text-anchor="middle" class="node-label">
                    {{ truncate(node.title || (typeLabel(node.nodeType) + ' ' + node.nodeId), 12) }}
                    <title>{{ node.title || (typeLabel(node.nodeType) + ' ' + node.nodeId) }}</title>
                  </text>
                </g>
              </svg>
            </div>
          </div>

          <!-- 选中节点详情 -->
          <div v-if="selectedNode" class="node-detail">
            <div class="node-detail-header">
              <span class="node-detail-title">
                节点 #{{ selectedNode.nodeId }}
                <el-tag size="mini" effect="plain">{{ typeLabel(selectedNode.nodeType) }}</el-tag>
                <span class="node-name">{{ selectedNode.title }}</span>
              </span>
            </div>
            <el-descriptions :column="4" border size="mini">
              <el-descriptions-item label="节点ID">{{ selectedNode.nodeId }}</el-descriptions-item>
              <el-descriptions-item label="类型">{{ selectedNode.nodeType }}</el-descriptions-item>
              <el-descriptions-item label="坐标">({{ selectedNode.x }}, {{ selectedNode.y }})</el-descriptions-item>
              <el-descriptions-item label="图片">{{ selectedNode.imageFile || '-' }}</el-descriptions-item>
              <el-descriptions-item label="描述" :span="4">{{ selectedNode.description || '-' }}</el-descriptions-item>
              <template v-for="item in typeDetailItems(selectedNode)">
                <el-descriptions-item :key="item.key" :label="item.key">{{ item.value }}</el-descriptions-item>
              </template>
            </el-descriptions>

            <el-row :gutter="12" v-if="(selectedNode.wfactions && selectedNode.wfactions.length) || (selectedNode.wfassignment && selectedNode.wfassignment.length)">
              <el-col :span="14" v-if="selectedNode.wfactions && selectedNode.wfactions.length">
                <p class="sub-title">出线操作（{{ selectedNode.wfactions.length }}）</p>
                <el-table :data="selectedNode.wfactions" border stripe size="mini" max-height="220">
                  <el-table-column prop="actionId" label="操作ID" width="70" />
                  <el-table-column label="正向" width="60">
                    <template slot-scope="s">{{ s.row.isPositive === false ? '否' : '是' }}</template>
                  </el-table-column>
                  <el-table-column prop="memberNodeId" label="目标节点" width="75" />
                  <el-table-column prop="action" label="操作" min-width="120" show-overflow-tooltip />
                  <el-table-column prop="instruction" label="说明" min-width="140" show-overflow-tooltip />
                </el-table>
              </el-col>
              <el-col :span="10" v-if="selectedNode.wfassignment && selectedNode.wfassignment.length">
                <p class="sub-title">任务分配（{{ selectedNode.wfassignment.length }}）</p>
                <el-table :data="selectedNode.wfassignment" border stripe size="mini" max-height="220">
                  <el-table-column prop="assignId" label="分配ID" width="70" />
                  <el-table-column prop="roleId" label="角色" min-width="100" show-overflow-tooltip />
                  <el-table-column prop="assignCode" label="人员" min-width="90" show-overflow-tooltip />
                  <el-table-column prop="description" label="描述" min-width="120" show-overflow-tooltip />
                </el-table>
              </el-col>
            </el-row>
          </div>
        </el-tab-pane>

        <!-- ============ 表格 Tab ============ -->
        <el-tab-pane label="表格明细" name="table">
          <el-tabs type="card" class="sub-tabs">
            <!-- 节点 -->
            <el-tab-pane :label="'节点（' + (workflow.wfnodes || []).length + '）'" name="nodes">
              <el-table :data="workflow.wfnodes || []" border stripe size="mini">
                <el-table-column type="expand">
                  <template slot-scope="s">
                    <div class="expand-box" v-if="typeDetailItems(s.row).length">
                      <p class="sub-title">类型明细（{{ s.row.nodeType }}）</p>
                      <el-descriptions :column="3" border size="mini">
                        <el-descriptions-item v-for="item in typeDetailItems(s.row)" :key="item.key" :label="item.key">
                          {{ item.value }}
                        </el-descriptions-item>
                      </el-descriptions>
                    </div>
                    <el-empty v-else description="该节点无类型子表数据" :image-size="40" />
                  </template>
                </el-table-column>
                <el-table-column prop="nodeId" label="节点ID" width="80" />
                <el-table-column label="类型" width="110">
                  <template slot-scope="s">
                    <el-tag size="mini" effect="plain">{{ typeLabel(s.row.nodeType) }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="title" label="标题" min-width="140" show-overflow-tooltip />
                <el-table-column prop="description" label="描述" minWidth="200" show-overflow-tooltip />
                <el-table-column prop="x" label="X" width="70" />
                <el-table-column prop="y" label="Y" width="70" />
                <el-table-column prop="imageFile" label="图片" min-width="100" show-overflow-tooltip />
              </el-table>
            </el-tab-pane>

            <!-- 出线操作 -->
            <el-tab-pane :label="'出线操作（' + actionRows.length + '）'" name="actions">
              <el-table :data="actionRows" border stripe size="mini">
                <el-table-column prop="ownerNodeId" label="所属节点" width="85" />
                <el-table-column prop="actionId" label="操作ID" width="80" />
                <el-table-column label="正向" width="65">
                  <template slot-scope="s">
                    <el-tag :type="s.row.isPositive === false ? 'danger' : 'success'" size="mini">
                      {{ s.row.isPositive === false ? '负向' : '正向' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="memberNodeId" label="目标节点" width="85" />
                <el-table-column prop="sequence" label="顺序" width="65" />
                <el-table-column prop="action" label="操作(ACTION)" min-width="130" show-overflow-tooltip />
                <el-table-column prop="condition" label="条件" min-width="130" show-overflow-tooltip />
                <el-table-column prop="conditionClass" label="条件类" min-width="150" show-overflow-tooltip />
                <el-table-column prop="instruction" label="操作说明" min-width="160" show-overflow-tooltip />
              </el-table>
            </el-tab-pane>

            <!-- 任务分配 -->
            <el-tab-pane :label="'任务分配（' + assignmentRows.length + '）'" name="assignments">
              <el-table :data="assignmentRows" border stripe size="mini">
                <el-table-column prop="ownerNodeId" label="所属节点" width="85" />
                <el-table-column prop="assignId" label="分配ID" width="80" />
                <el-table-column prop="roleId" label="角色" min-width="100" show-overflow-tooltip />
                <el-table-column prop="assignCode" label="人员" min-width="90" show-overflow-tooltip />
                <el-table-column prop="relationship" label="关系" min-width="110" show-overflow-tooltip />
                <el-table-column prop="description" label="描述" min-width="140" show-overflow-tooltip />
                <el-table-column prop="app" label="应用" width="100" show-overflow-tooltip />
                <el-table-column prop="priority" label="优先级" width="70" />
                <el-table-column prop="groupNum" label="组号" width="65" />
                <el-table-column prop="timelimit" label="时限" width="80" />
                <el-table-column prop="assignStatus" label="状态" width="90" />
                <el-table-column prop="escRole" label="升级角色" min-width="100" show-overflow-tooltip />
              </el-table>
            </el-tab-pane>

            <!-- 通知 -->
            <el-tab-pane :label="'通知（' + notificationRows.length + '）'" name="notifications">
              <el-table :data="notificationRows" border stripe size="mini">
                <el-table-column prop="scope" label="归属" width="160" show-overflow-tooltip />
                <el-table-column prop="uniqueId" label="通知ID" width="90" />
                <el-table-column prop="templateId" label="通讯模板" min-width="180" show-overflow-tooltip />
              </el-table>
            </el-tab-pane>

            <!-- 分配组 -->
            <el-tab-pane :label="'分配组（' + groupRows.length + '）'" name="groups">
              <el-table :data="groupRows" border stripe size="mini">
                <el-table-column prop="ownerNodeId" label="所属节点" width="100" />
                <el-table-column prop="groupNum" label="组号" width="80" />
                <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
                <el-table-column label="先完成者通过" width="120">
                  <template slot-scope="s">{{ boolText(s.row.firstComplete) }}</template>
                </el-table-column>
              </el-table>
            </el-tab-pane>
          </el-tabs>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </section>
</template>

<script>
import { workflowDetail } from '@/api/wfdesign'

// 节点类型中文标签
var TYPE_LABELS = {
  WFSTART: '开始',
  WFSTOP: '停止',
  WFTASK: '任务',
  WFCONDITION: '条件',
  WFINPUT: '输入',
  WFINTERACTION: '交互',
  WFSUBPROCESS: '子流程',
  WFWAIT: '等待'
}
// 节点类型 -> 导出 JSON 中类型子表键(与脚本 NODE_TYPE_DEF 一致)
var TYPE_DETAIL_KEYS = {
  WFSTART: 'wfstart',
  WFSTOP: 'wfstop',
  WFTASK: 'wftask',
  WFCONDITION: 'wfcondition',
  WFINPUT: 'wfinput',
  WFINTERACTION: 'wfinteraction',
  WFSUBPROCESS: 'wfsubprocess',
  WFWAIT: 'wfwaitlist'
}

export default {
  name: 'WfDesignDetail',
  data() {
    return {
      GRID: 80, // Maximo 工作流设计器网格像素(系统属性 mxe.webclient.wfdesigner.pixelsPerNode, 默认80)
      loading: false,
      editMode: false,
      activeTab: 'canvas',
      workflow: {},
      selectedNodeId: null,
      zoom: 1
    }
  },
  computed: {
    nodeMap() {
      var m = {}
      ;(this.workflow.wfnodes || []).forEach(function (n) { m[n.nodeId] = n })
      return m
    },
    /** 连线: 节点 wfactions 拍平, 并算出起止坐标 */
    edges() {
      var self = this
      var list = []
      ;(this.workflow.wfnodes || []).forEach(function (node) {
        ;(node.wfactions || []).forEach(function (a) {
          var target = self.nodeMap[a.memberNodeId]
          var seg = target ? self.borderSegment(node, target) : null
          var rawLabel = a.action || a.instruction || ''
          var label = rawLabel ? self.truncateByWidth(rawLabel, 120) : ''
          var item = {
            from: node.nodeId,
            actionId: a.actionId,
            isPositive: a.isPositive,
            label: label,
            labelW: self.textWidth(label)
          }
          if (seg) {
            item.x1 = seg.x1
            item.y1 = seg.y1
            item.x2 = seg.x2
            item.y2 = seg.y2
            item.lx = (seg.x1 + seg.x2) / 2
            item.ly = (seg.y1 + seg.y2) / 2
          }
          list.push(item)
        })
      })
      return list
    },
    selectedNode() {
      return this.selectedNodeId === null ? null : (this.nodeMap[this.selectedNodeId] || null)
    },
    actionRows() {
      var rows = []
      ;(this.workflow.wfnodes || []).forEach(function (n) {
        ;(n.wfactions || []).forEach(function (a) {
          rows.push(Object.assign({ ownerNodeId: n.nodeId }, a))
        })
      })
      return rows
    },
    assignmentRows() {
      var rows = []
      ;(this.workflow.wfnodes || []).forEach(function (n) {
        ;(n.wfassignment || []).forEach(function (a) {
          rows.push(Object.assign({ ownerNodeId: n.nodeId }, a))
        })
      })
      return rows
    },
    notificationRows() {
      var rows = []
      var wf = this.workflow
      ;(wf.wfnotifications || []).forEach(function (x) {
        rows.push({ scope: '流程级', uniqueId: x.uniqueId, templateId: x.templateId })
      })
      ;(wf.wfnodes || []).forEach(function (n) {
        ;(n.wfnotifications || []).forEach(function (x) {
          rows.push({ scope: '节点 #' + n.nodeId, uniqueId: x.uniqueId, templateId: x.templateId })
        })
        ;(n.wfactions || []).forEach(function (a) {
          ;(a.wfnotifications || []).forEach(function (x) {
            rows.push({ scope: '节点 #' + n.nodeId + ' / 操作 ' + a.actionId, uniqueId: x.uniqueId, templateId: x.templateId })
          })
        })
      })
      return rows
    },
    groupRows() {
      var rows = []
      ;(this.workflow.wfnodes || []).forEach(function (n) {
        ;(n.wfasgngroup || []).forEach(function (g) {
          rows.push(Object.assign({ ownerNodeId: n.nodeId }, g))
        })
      })
      return rows
    },
    canvasSize() {
      var maxX = 120
      var maxY = 120
      ;(this.workflow.wfnodes || []).forEach(n => {
        maxX = Math.max(maxX, n.x * this.GRID + 80)
        maxY = Math.max(maxY, n.y * this.GRID + 70)
      })
      return { width: maxX, height: maxY }
    },
    innerStyle() {
      return {
        width: this.canvasSize.width + 'px',
        height: this.canvasSize.height + 'px',
        transform: 'scale(' + this.zoom + ')',
        transformOrigin: '0 0'
      }
    }
  },
  methods: {
    boolText: function (v) {
      return v === true ? '是' : '否'
    },
    typeLabel: function (t) {
      return TYPE_LABELS[t] || t
    },
    truncate: function (s, n) {
      s = s == null ? '' : String(s)
      return s.length > n ? s.slice(0, n) + '…' : s
    },
    /** 文本近似像素宽(10px 字体: 全角 11px, 半角 6px) */
    textWidth: function (s) {
      s = s == null ? '' : String(s)
      var w = 0
      for (var i = 0; i < s.length; i++) {
        w += s.charCodeAt(i) > 255 ? 11 : 6
      }
      return w
    },
    /** 按最大像素宽截断 */
    truncateByWidth: function (s, maxW) {
      s = s == null ? '' : String(s)
      if (this.textWidth(s) <= maxW) {
        return s
      }
      var out = ''
      var w = 0
      for (var i = 0; i < s.length; i++) {
        var cw = s.charCodeAt(i) > 255 ? 11 : 6
        if (w + cw + 11 > maxW) {
          break
        }
        out += s[i]
        w += cw
      }
      return out + '…'
    },
    /** 节点形状外接框(连线起止用), 宽高近似为统一矩形 */
    shapeBox: function (node) {
      if (node.nodeType === 'WFSTART' || node.nodeType === 'WFSTOP') {
        return { w: 32, h: 32 }
      }
      return { w: 54, h: 32 }
    },
    /** 节点左上角画布坐标 */
    nodeOrigin: function (node) {
      return { x: node.x * this.GRID, y: node.y * this.GRID }
    },
    /** 两节点中心 -> 与各自矩形边框交点(椭圆近似) */
    borderSegment: function (from, to) {
      var b1 = this.shapeBox(from)
      var b2 = this.shapeBox(to)
      var p1 = this.nodeOrigin(from)
      var p2 = this.nodeOrigin(to)
      var c1x = p1.x + b1.w / 2
      var c1y = p1.y + b1.h / 2
      var c2x = p2.x + b2.w / 2
      var c2y = p2.y + b2.h / 2
      var dx = c2x - c1x
      var dy = c2y - c1y
      var t1 = 1 / Math.sqrt(Math.pow(dx / (b1.w / 2), 2) + Math.pow(dy / (b1.h / 2), 2))
      var t2 = 1 / Math.sqrt(Math.pow(dx / (b2.w / 2), 2) + Math.pow(dy / (b2.h / 2), 2))
      return {
        x1: c1x + dx * (isFinite(t1) ? t1 : 0),
        y1: c1y + dy * (isFinite(t1) ? t1 : 0),
        x2: c2x - dx * (isFinite(t2) ? t2 : 0),
        y2: c2y - dy * (isFinite(t2) ? t2 : 0)
      }
    },
    /** 节点类型子表拍平为 key/value(布尔转是/否, null 跳过) */
    typeDetailItems: function (node) {
      var key = TYPE_DETAIL_KEYS[node.nodeType]
      var detail = key ? node[key] : null
      if (!detail || typeof detail !== 'object') {
        return []
      }
      var items = []
      Object.keys(detail).forEach(k => {
        var v = detail[k]
        if (v === null || v === undefined || v === '') {
          return
        }
        if (typeof v === 'boolean') {
          v = v ? '是' : '否'
        }
        items.push({ key: k, value: String(v) })
      })
      return items
    },
    selectNode: function (node) {
      this.selectedNodeId = node.nodeId
    },
    // === 缩放 ===
    zoomIn() {
      this.zoom = Math.min(2, Math.round((this.zoom + 0.1) * 100) / 100)
    },
    zoomOut() {
      this.zoom = Math.max(0.2, Math.round((this.zoom - 0.1) * 100) / 100)
    },
    resetZoom() {
      this.zoom = 1
    },
    fitZoom() {
      var el = this.$refs.canvasScroll
      if (!el) return
      var w = el.clientWidth - 20
      if (this.canvasSize.width > 0) {
        this.zoom = Math.min(1, Math.max(0.2, Math.round((w / this.canvasSize.width) * 100) / 100))
      }
    },
    // === 编辑(功能占位, 暂未实现) ===
    handleEdit() {
      this.editMode = true
      this.$message.info('编辑功能正在开发中，当前仅可预览')
    },
    cancelEdit() {
      this.editMode = false
    },
    handleSave() {
      // TODO: 编辑保存待实现(画布拖拽改 x/y、表格内联编辑后调用工作流导入接口)
      this.$message.info('编辑保存功能正在开发中，暂未开放')
    },
    goBack() {
      this.$router.push({ name: 'WfDesign' })
    },
    fetchDetail() {
      this.loading = true
      workflowDetail({
        processName: this.$route.params.processName,
        processRev: parseInt(this.$route.params.processRev, 10)
      }).then(res => {
        const data = res.data || res
        if (data.status === 'error') {
          this.$message.error(data.message || '加载失败')
          this.workflow = {}
          return
        }
        var list = data.workflows || []
        this.workflow = list[0] || {}
        this.$nextTick(() => {
          this.fitZoom()
        })
      }).catch(err => {
        this.$message.error('加载失败: ' + (err.message || String(err)))
      }).finally(() => {
        this.loading = false
      })
    }
  },
  mounted() {
    this.fetchDetail()
  }
}
</script>

<style lang="scss" scoped>
.wf-detail-page {
  padding: 16px;
}
.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}
.header-left {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}
.header-title h2 {
  margin: 0 0 2px 0;
  font-size: 18px;
  font-weight: 600;
}
.header-title .rev {
  font-size: 13px;
  color: #909399;
  font-weight: normal;
}
.page-summary {
  color: #606266;
  margin: 0;
  font-size: 13px;
}
.wf-desc {
  margin-bottom: 8px;
}
.edit-tip {
  margin-bottom: 10px;
}
.detail-tabs {
  margin-top: 6px;
}
.canvas-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.canvas-legend {
  font-size: 12px;
  color: #606266;
  display: flex;
  align-items: center;
  gap: 14px;
}
.legend-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-style: normal;
}
.legend-item span {
  display: inline-block;
  width: 26px;
  height: 0;
  border-top-width: 2px;
  border-top-style: solid;
}
.line-pos { border-top-color: #5a5a5a; }
.line-neg { border-top-color: #f00; }
.canvas-tip {
  color: #909399;
}
.canvas-scroll {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  height: 520px;
  overflow: auto;
  background: #fafafa;
  /* 主网格 80px(=GRID), 次网格 16px(5 等分) */
  background-image:
    linear-gradient(to right, rgba(200,200,200,.35) 1px, transparent 1px),
    linear-gradient(to bottom, rgba(200,200,200,.35) 1px, transparent 1px),
    linear-gradient(to right, #e4e7ed 1px, transparent 1px),
    linear-gradient(to bottom, #e4e7ed 1px, transparent 1px);
  background-size: 16px 16px, 16px 16px, 80px 80px, 80px 80px;
  background-position: -1px -1px;
}
.canvas-inner {
  position: relative;
}
.wf-svg {
  display: block;
}
.wf-node {
  cursor: pointer;
}
.wf-node.editable {
  cursor: move;
}
.node-label {
  pointer-events: none;
}
.node-detail {
  margin-top: 12px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 10px;
  background: #fff;
}
.node-detail-header {
  margin-bottom: 8px;
}
.node-detail-title {
  font-size: 14px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
.node-name {
  font-weight: normal;
  color: #606266;
}
.sub-title {
  margin: 8px 0 6px 0;
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}
.expand-box {
  padding: 8px 12px;
  background: #fafafa;
}
.sub-tabs {
  margin-top: 4px;
}
</style>
