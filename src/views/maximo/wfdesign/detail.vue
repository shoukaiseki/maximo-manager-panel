<template>
  <section class="wf-detail-page" v-loading="loading">
    <div v-loading.fullscreen.lock="saving" element-loading-text="拼命处理中，请耐心等待" element-loading-background="rgba(0, 0, 0, 0.8)">
    <el-card>
      <!-- 头部: 返回 + 主信息 + 编辑/保存/取消 -->
      <div class="detail-header">
        <div class="header-left">
          <el-button icon="el-icon-arrow-left" size="mini" @click="goBack">返回</el-button>
          <div class="header-title">
            <h2>{{ workflow.processName }} <span class="rev">v{{ workflow.processRev }}</span></h2>
            <p v-if="!editMode" class="page-summary">{{ workflow.description || '无描述' }}</p>
            <el-input
              v-else
              v-model="workflow.description"
              size="mini"
              placeholder="流程描述"
              style="max-width: 460px;"
              @input="dirty = true" />
          </div>
        </div>
        <div class="header-actions">
          <template v-if="!editMode">
            <el-button type="primary" size="mini" icon="el-icon-edit" @click="handleEdit">编 辑</el-button>
            <el-dropdown
              class="state-dropdown"
              size="mini"
              trigger="click"
              @command="handleStateCommand">
              <el-button size="mini">
                选择操作<i class="el-icon-arrow-down el-icon--right"></i>
              </el-button>
              <el-dropdown-menu slot="dropdown">
                <el-dropdown-item command="deactivate" :disabled="workflow.active !== true">取消激活过程</el-dropdown-item>
                <el-dropdown-item command="disable" :disabled="workflow.enabled !== true">禁用过程</el-dropdown-item>
              </el-dropdown-menu>
            </el-dropdown>
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
        title="编辑模式：拖拽节点调整坐标（自动吸附网格），双击节点或右键「编辑节点」打开节点对话框，可修改标题/描述并维护出线操作、任务分配、分配组、通知，完成后点击右上角「保存」。"
        type="info"
        :closable="false"
        show-icon />

      <el-tabs v-model="activeTab" class="detail-tabs">
        <!-- ============ 画布 Tab ============ -->
        <el-tab-pane label="流程图" name="canvas">
          <div class="canvas-toolbar">
            <div class="canvas-tools">
              <el-button-group>
                <el-button size="mini" icon="el-icon-zoom-out" @click="zoomOut" title="缩小"></el-button>
                <el-button size="mini" @click="resetZoom">{{ Math.round(zoom * 100) }}%</el-button>
                <el-button size="mini" icon="el-icon-zoom-in" @click="zoomIn" title="放大"></el-button>
                <el-button size="mini" icon="el-icon-full-screen" @click="fitZoom" title="适应宽度"></el-button>
              </el-button-group>
              <!-- 节点间距: 拉开网格, 避免节点标题过长时左右压字 -->
              <span class="canvas-spacing">
                <span class="spacing-label">节点间距</span>
                <el-slider v-model="spacing" :min="1" :max="2.5" :step="0.1" class="spacing-slider" />
                <span class="spacing-value">{{ Math.round(spacing * 100) }}%</span>
              </span>
            </div>
            <span class="canvas-legend">
              <i class="legend-item"><span class="line-pos"></span>正向连线</i>
              <i class="legend-item"><span class="line-neg"></span>负向连线</i>
              <span class="canvas-tip">点击节点查看节点详情，右键节点可选择「查看节点」{{ editMode ? '；编辑模式：拖拽节点调整位置，双击/右键节点打开节点对话框' : '' }}</span>
            </span>
          </div>
          <div class="canvas-scroll" ref="canvasScroll" @scroll="closeContextMenu" @contextmenu.prevent>
            <!-- viewBox + width/height 缩放, 元素本身用 flex margin:auto 居中 -->
            <svg
              :width="canvasSize.width * zoom"
              :height="canvasSize.height * zoom"
              :viewBox="'0 0 ' + canvasSize.width + ' ' + canvasSize.height"
              class="wf-svg">
              <defs>
                <marker id="arrow-pos" markerWidth="10" markerHeight="10" refX="8" refY="3" orient="auto" markerUnits="strokeWidth">
                  <path d="M0,0 L8,3 L0,6 z" fill="#5a5a5a" />
                </marker>
                <marker id="arrow-neg" markerWidth="10" markerHeight="10" refX="8" refY="3" orient="auto" markerUnits="strokeWidth">
                  <path d="M0,0 L8,3 L0,6 z" fill="#f00" />
                </marker>
                <!-- 网格: 次网格 = 主网格/5, 主网格 = 节点间距(默认 80px = GRID), 与节点坐标同一坐标系, 随缩放一起变化 -->
                <pattern id="wf-grid-minor" :width="gridPx / 5" :height="gridPx / 5" patternUnits="userSpaceOnUse">
                  <path :d="'M ' + (gridPx / 5) + ' 0 L 0 0 0 ' + (gridPx / 5)" fill="none" stroke="#eceff3" stroke-width="1" vector-effect="non-scaling-stroke" />
                </pattern>
                <pattern id="wf-grid" :width="gridPx" :height="gridPx" patternUnits="userSpaceOnUse">
                  <rect :width="gridPx" :height="gridPx" fill="url(#wf-grid-minor)" />
                  <path :d="'M ' + gridPx + ' 0 L 0 0 0 ' + gridPx" fill="none" stroke="#dfe3e8" stroke-width="1" vector-effect="non-scaling-stroke" />
                </pattern>
              </defs>

              <rect x="0" y="0" :width="canvasSize.width" :height="canvasSize.height" fill="url(#wf-grid)" />

              <!-- 内容整体平移到画布左上留白处, 画布尺寸按内容包围盒计算, 再由外层居中 -->
              <g :transform="contentTransform">
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
                  :transform="'translate(' + (node.x * gridPx) + ',' + (node.y * gridPx) + ')'"
                  class="wf-node" :class="{ selected: selectedNodeId === node.nodeId, editable: editMode }"
                  @mousedown="onNodeMouseDown($event, node)"
                  @click="selectNode(node)"
                  @dblclick="openNodeDetail(node)"
                  @contextmenu.prevent="onNodeContextMenu($event, node)">
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
              </g>
            </svg>

            <!-- 节点右键菜单(相对画布容器定位) -->
            <div
              v-if="contextMenu.visible"
              class="node-context-menu"
              :style="{ left: contextMenu.x + 'px', top: contextMenu.y + 'px' }">
              <div class="menu-item" @click="contextViewNode">查看节点</div>
              <div v-if="editMode" class="menu-item" @click="contextEditNode">编辑节点</div>
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
              <el-button type="primary" size="mini" icon="el-icon-view" @click="openNodeDetail(selectedNode)">
                {{ editMode ? '编辑节点' : '查看节点' }}
              </el-button>
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

            <el-row :gutter="12" v-if="activeRows(selectedNode.wfactions).length || activeRows(selectedNode.wfassignment).length">
              <el-col :span="14" v-if="activeRows(selectedNode.wfactions).length">
                <p class="sub-title">出线操作（{{ activeRows(selectedNode.wfactions).length }}）</p>
                <el-table :data="activeRows(selectedNode.wfactions)" border stripe size="mini" max-height="220">
                  <el-table-column prop="actionId" label="操作ID" width="70" />
                  <el-table-column label="正向" width="60">
                    <template slot-scope="s">{{ s.row.isPositive === false ? '否' : '是' }}</template>
                  </el-table-column>
                  <el-table-column prop="memberNodeId" label="目标节点" width="75" />
                  <el-table-column prop="action" label="操作" min-width="120" show-overflow-tooltip />
                  <el-table-column prop="instruction" label="说明" min-width="140" show-overflow-tooltip />
                </el-table>
              </el-col>
              <el-col :span="10" v-if="activeRows(selectedNode.wfassignment).length">
                <p class="sub-title">任务分配（{{ activeRows(selectedNode.wfassignment).length }}）</p>
                <el-table :data="activeRows(selectedNode.wfassignment)" border stripe size="mini" max-height="220">
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
                <el-table-column label="操作" width="90" fixed="right">
                  <template slot-scope="s">
                    <el-button type="text" size="mini" @click="openNodeDetail(s.row)">编辑属性</el-button>
                  </template>
                </el-table-column>
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
                <el-table-column label="操作" width="90" fixed="right">
                  <template slot-scope="s">
                    <el-button type="text" size="mini" @click="openActionDetail(s.row.ownerNodeId, s.row._rowIndex)">编辑属性</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>

            <!-- 任务分配 -->
            <el-tab-pane :label="'任务分配（' + assignmentRows.length + '）'" name="assignments">
              <el-table :data="assignmentRows" border stripe size="mini">
                <el-table-column type="expand">
                  <template slot-scope="s">
                    <div class="expand-box">
                      <p class="sub-title">详细信息</p>
                      <el-descriptions v-if="detailItems(s.row, 'wfassignment').length" :column="3" border size="mini">
                        <el-descriptions-item v-for="d in detailItems(s.row, 'wfassignment')" :key="d.label" :label="d.label">
                          {{ d.value }}
                        </el-descriptions-item>
                      </el-descriptions>
                      <p v-else class="no-prop">该记录无更多详细信息</p>
                    </div>
                  </template>
                </el-table-column>
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
                <el-table-column type="expand">
                  <template slot-scope="s">
                    <div class="expand-box">
                      <p class="sub-title">详细信息</p>
                      <el-descriptions v-if="detailItems(s.row, 'wfnotifications').length" :column="3" border size="mini">
                        <el-descriptions-item v-for="d in detailItems(s.row, 'wfnotifications')" :key="d.label" :label="d.label">
                          {{ d.value }}
                        </el-descriptions-item>
                      </el-descriptions>
                      <p v-else class="no-prop">该记录无更多详细信息</p>
                    </div>
                  </template>
                </el-table-column>
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

    <!-- 节点对话框: 基础信息 + 该节点类型自己的属性(字段/子表按 Maximo 同名 dialog 取, 编辑态可改, 查看态只读) -->
    <el-dialog
      :title="nodeDialogTitle"
      :visible.sync="nodeDialog.visible"
      width="1080px"
      top="5vh"
      append-to-body
      :close-on-click-modal="false">
      <div v-if="dialogNode" class="node-dialog-body">
        <!-- 基本信息 -->
        <el-card shadow="never" class="prop-card">
          <div slot="header" class="prop-card-head">基本信息</div>
          <el-form label-width="60px" size="mini" class="node-form">
            <el-row :gutter="12">
              <el-col :span="6">
                <el-form-item label="节点ID">{{ dialogNode.nodeId }}</el-form-item>
              </el-col>
              <el-col :span="6">
                <el-form-item label="类型">
                  <el-tag size="mini" effect="plain">{{ typeLabel(dialogNode.nodeType) }}</el-tag>
                </el-form-item>
              </el-col>
              <el-col :span="6">
                <el-form-item label="坐标">({{ dialogNode.x }}, {{ dialogNode.y }})</el-form-item>
              </el-col>
              <el-col :span="6">
                <el-form-item label="图片">{{ dialogNode.imageFile || '-' }}</el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="标题">
                  <el-input v-if="editMode" v-model="dialogNode.title" size="mini" maxlength="100" @input="markDirty" />
                  <span v-else>{{ dialogNode.title || '-' }}</span>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="描述">
                  <el-input v-if="editMode" v-model="dialogNode.description" size="mini" @input="markDirty" />
                  <span v-else>{{ dialogNode.description || '-' }}</span>
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </el-card>

        <!-- 类型属性: 不同节点类型弹出各自的对话框(字段对照 Maximo wfdesign.xml 中同名 dialog) -->
        <el-card shadow="never" class="prop-card">
          <div slot="header" class="prop-card-head">{{ nodeDialogDef.label }}</div>
          <p v-if="!nodeDialogDef.fields.length" class="no-prop">节点类型无可编辑属性。</p>
          <el-form v-else label-width="96px" size="mini" class="node-form">
            <el-row :gutter="12">
              <el-col v-for="f in nodeDialogDef.fields" :key="f.prop" :span="f.span || 12">
                <el-form-item :label="f.label">
                  <template v-if="editMode && !f.readonly">
                    <el-switch v-if="f.type === 'bool'" v-model="dialogDetail[f.prop]" @change="markDirty" />
                    <el-radio-group v-else-if="f.type === 'radio'" v-model="dialogDetail[f.prop]" @change="markDirty">
                      <el-radio v-for="opt in f.options" :key="String(opt.value)" :label="opt.value">{{ opt.label }}</el-radio>
                    </el-radio-group>
                    <el-input v-else-if="f.type === 'textarea'" v-model="dialogDetail[f.prop]" type="textarea" :rows="3" @input="markDirty" />
                    <el-input v-else v-model="dialogDetail[f.prop]" size="mini" @input="markDirty" />
                  </template>
                  <span v-else>{{ propText(f) }}</span>
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </el-card>

        <!-- 该对话框关联的子表(详细信息对应 Maximo 的 tabledetails, 用展开行展示) -->
        <el-card v-for="name in nodeDialogDef.tables" :key="name" shadow="never" class="prop-card">
          <div slot="header" class="prop-card-head">
            <span>{{ subTableDef(name).label }}（{{ activeRows(dialogNode[name]).length }}）</span>
            <el-button
              v-if="editMode"
              type="primary" plain size="mini" icon="el-icon-plus"
              @click="addSubRow(name)">新增</el-button>
          </div>
          <el-table
            :data="dialogNode[name] || []"
            border stripe size="mini" max-height="260"
            :row-style="subRowStyle">
            <el-table-column v-if="subTableDef(name).details.length" type="expand">
              <template slot-scope="s">
                <div class="expand-box">
                  <p class="sub-title">详细信息</p>
                  <!-- 编辑态: 字段定义 editableDetails 的子表, 详细信息直接就地编辑 -->
                  <el-form
                    v-if="editMode && subTableDef(name).editableDetails && s.row._delete !== true"
                    label-width="96px" size="mini" class="node-form">
                    <el-row :gutter="12">
                      <el-col v-for="d in subTableDef(name).details" :key="d.prop" :span="d.span || 8">
                        <el-form-item :label="d.label">
                          <el-input v-if="d.readonly" :value="s.row[d.prop]" size="mini" disabled />
                          <el-switch v-else-if="d.type === 'bool'" v-model="s.row[d.prop]" @change="markDirty" />
                          <el-input
                            v-else
                            v-model="s.row[d.prop]"
                            :type="d.type === 'textarea' ? 'textarea' : 'text'"
                            :rows="d.type === 'textarea' ? 2 : undefined"
                            size="mini"
                            @input="markDirty" />
                        </el-form-item>
                      </el-col>
                    </el-row>
                  </el-form>
                  <template v-else>
                    <el-descriptions v-if="detailItems(s.row, name).length" :column="2" border size="mini">
                      <el-descriptions-item v-for="d in detailItems(s.row, name)" :key="d.label" :label="d.label">
                        {{ d.value }}
                      </el-descriptions-item>
                    </el-descriptions>
                    <p v-else class="no-prop">该记录无更多详细信息</p>
                  </template>
                </div>
              </template>
            </el-table-column>
            <el-table-column
              v-for="col in subTableDef(name).columns"
              :key="col.prop"
              :label="col.label"
              :width="col.width"
              :min-width="col.minWidth">
              <template slot-scope="s">
                <template v-if="editMode && !subTableDef(name).readonly && !col.readonly && s.row._delete !== true">
                  <el-switch v-if="col.type === 'bool'" v-model="s.row[col.prop]" @change="markDirty" />
                  <el-select
                    v-else-if="col.type === 'node'"
                    v-model="s.row[col.prop]" size="mini" filterable clearable
                    placeholder="选择目标节点" @change="markDirty">
                    <el-option
                      v-for="n in (workflow.wfnodes || [])"
                      :key="n.nodeId"
                      :label="'#' + n.nodeId + ' ' + (n.title || typeLabel(n.nodeType))"
                      :value="n.nodeId" />
                  </el-select>
                  <el-input v-else v-model="s.row[col.prop]" size="mini" @input="markDirty">
                    <i
                      v-if="col.lookup"
                      slot="suffix"
                      class="el-input__icon el-icon-search lookup-icon"
                      :title="'选择' + col.label"
                      @click="openLookup(s.row, col)" />
                  </el-input>
                </template>
                <span v-else>{{ cellText(s.row, col) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" :width="subTableDef(name).readonly ? 130 : 80" fixed="right">
              <template slot-scope="s">
                <el-button
                  v-if="name === 'wfactions'"
                  type="text" size="mini"
                  @click="openActionDetail(dialogNode.nodeId, s.$index)">编辑属性</el-button>
                <template v-if="editMode">
                  <el-button v-if="s.row._delete === true" type="text" size="mini" @click="undoSubRow(s.row)">撤销</el-button>
                  <el-button v-else type="text" size="mini" class="danger-text" @click="deleteSubRow(dialogNode[name], s.$index)">删除</el-button>
                </template>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <p v-if="editMode" class="node-dialog-tip">子表与节点属性改动会随页面右上角「保 存」一并提交；标记删除的行保存后才会真正删除。</p>
      </div>
      <div slot="footer">
        <el-button size="mini" @click="nodeDialog.visible = false">关 闭</el-button>
      </div>
    </el-dialog>

    <!-- 操作属性对话框(出线操作=Maximo「操作属性」, 手工输入节点的操作=Maximo「输入操作属性」) -->
    <el-dialog
      :title="actionDialogTitle"
      :visible.sync="actionDialog.visible"
      width="900px"
      top="6vh"
      append-to-body
      :close-on-click-modal="false">
      <div v-if="dialogAction" class="node-dialog-body">
        <!-- 基本信息 -->
        <el-card shadow="never" class="prop-card">
          <div slot="header" class="prop-card-head">基本信息</div>
          <el-form label-width="96px" size="mini" class="node-form">
            <el-row :gutter="12">
              <el-col :span="8">
                <el-form-item label="操作ID">
                  <span v-if="dialogAction.actionId === null || dialogAction.actionId === undefined">新建</span>
                  <span v-else>{{ dialogAction.actionId }}</span>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="方向">
                  <el-tag :type="dialogAction.isPositive === false ? 'danger' : 'success'" size="mini">
                    {{ dialogAction.isPositive === false ? '负向' : '正向' }}
                  </el-tag>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="目标节点">
                  <el-select
                    v-if="editMode"
                    v-model="dialogAction.memberNodeId" size="mini" filterable clearable
                    placeholder="选择目标节点" @change="markDirty">
                    <el-option
                      v-for="n in (workflow.wfnodes || [])"
                      :key="n.nodeId"
                      :label="'#' + n.nodeId + ' ' + (n.title || typeLabel(n.nodeType))"
                      :value="n.nodeId" />
                  </el-select>
                  <span v-else>{{ dialogAction.memberNodeId }}</span>
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </el-card>

        <!-- 操作本身的属性(字段对照 Maximo 同名 dialog) -->
        <el-card shadow="never" class="prop-card">
          <div slot="header" class="prop-card-head">{{ actionDialogDef.label }}</div>
          <el-form label-width="96px" size="mini" class="node-form">
            <el-row :gutter="12">
              <el-col v-for="f in actionDialogDef.fields" :key="f.prop" :span="f.span || 12">
                <el-form-item :label="f.label">
                  <template v-if="editMode && !f.readonly">
                    <el-switch v-if="f.type === 'bool'" v-model="dialogAction[f.prop]" @change="markDirty" />
                    <el-input v-else-if="f.type === 'textarea'" v-model="dialogAction[f.prop]" type="textarea" :rows="2" @input="markDirty" />
                    <el-input v-else v-model="dialogAction[f.prop]" size="mini" @input="markDirty">
                      <i
                        v-if="f.lookup"
                        slot="suffix"
                        class="el-input__icon el-icon-search lookup-icon"
                        :title="'选择' + f.label"
                        @click="openLookup(dialogAction, f)" />
                    </el-input>
                  </template>
                  <span v-else>{{ propText(f, dialogAction) }}</span>
                </el-form-item>
              </el-col>
              <el-col :span="16">
                <el-form-item label="顺序">
                  <el-input v-if="editMode" v-model="dialogAction.sequence" size="mini" @input="markDirty" />
                  <span v-else>{{ dialogAction.sequence }}</span>
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </el-card>

        <!-- 关联子表(详细信息对应 Maximo 的 tabledetails, 用展开行展示) -->
        <el-card v-for="name in actionDialogDef.tables" :key="name" shadow="never" class="prop-card">
          <div slot="header" class="prop-card-head">
            <span>{{ subTableDef(name).label }}（{{ activeRows(dialogAction[name]).length }}）</span>
            <el-button v-if="editMode" type="primary" plain size="mini" icon="el-icon-plus" @click="addActionNotification">新增</el-button>
          </div>
          <el-table :data="dialogAction[name] || []" border stripe size="mini" :row-style="subRowStyle">
            <el-table-column v-if="subTableDef(name).details.length" type="expand">
              <template slot-scope="s">
                <div class="expand-box">
                  <p class="sub-title">详细信息</p>
                  <!-- 编辑态: 字段定义 editableDetails 的子表, 详细信息直接就地编辑 -->
                  <el-form
                    v-if="editMode && subTableDef(name).editableDetails && s.row._delete !== true"
                    label-width="96px" size="mini" class="node-form">
                    <el-row :gutter="12">
                      <el-col v-for="d in subTableDef(name).details" :key="d.prop" :span="d.span || 8">
                        <el-form-item :label="d.label">
                          <el-input v-if="d.readonly" :value="s.row[d.prop]" size="mini" disabled />
                          <el-switch v-else-if="d.type === 'bool'" v-model="s.row[d.prop]" @change="markDirty" />
                          <el-input
                            v-else
                            v-model="s.row[d.prop]"
                            :type="d.type === 'textarea' ? 'textarea' : 'text'"
                            :rows="d.type === 'textarea' ? 2 : undefined"
                            size="mini"
                            @input="markDirty" />
                        </el-form-item>
                      </el-col>
                    </el-row>
                  </el-form>
                  <template v-else>
                    <el-descriptions v-if="detailItems(s.row, name).length" :column="2" border size="mini">
                      <el-descriptions-item v-for="d in detailItems(s.row, name)" :key="d.label" :label="d.label">
                        {{ d.value }}
                      </el-descriptions-item>
                    </el-descriptions>
                    <p v-else class="no-prop">该记录无更多详细信息</p>
                  </template>
                </div>
              </template>
            </el-table-column>
            <el-table-column
              v-for="col in subTableDef(name).columns"
              :key="col.prop"
              :label="col.label"
              :width="col.width"
              :min-width="col.minWidth">
              <template slot-scope="s">
                <template v-if="editMode && !col.readonly && s.row._delete !== true">
                  <el-switch v-if="col.type === 'bool'" v-model="s.row[col.prop]" @change="markDirty" />
                  <el-input v-else v-model="s.row[col.prop]" size="mini" @input="markDirty">
                    <i
                      v-if="col.lookup"
                      slot="suffix"
                      class="el-input__icon el-icon-search lookup-icon"
                      :title="'选择' + col.label"
                      @click="openLookup(s.row, col)" />
                  </el-input>
                </template>
                <span v-else>{{ cellText(s.row, col) }}</span>
              </template>
            </el-table-column>
            <el-table-column v-if="editMode" label="操作" width="80" fixed="right">
              <template slot-scope="s">
                <el-button v-if="s.row._delete === true" type="text" size="mini" @click="undoSubRow(s.row)">撤销</el-button>
                <el-button v-else type="text" size="mini" class="danger-text" @click="deleteSubRow(dialogAction[name], s.$index)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <p v-if="editMode" class="node-dialog-tip">改动会随页面右上角「保 存」一并提交。</p>
      </div>
      <div slot="footer">
        <el-button size="mini" @click="actionDialog.visible = false">关 闭</el-button>
      </div>
    </el-dialog>

    <!-- 保存确认对话框 -->
    <el-dialog title="保存工作流" :visible.sync="saveDialog.visible" width="460px" append-to-body>
      <p class="save-tip">
        将按迁移模式整包保存流程 {{ workflow.processName }} (v{{ workflow.processRev }})，共
        {{ (workflow.wfnodes || []).length }} 个节点；已存在的节点/操作/分配等记录将被更新，不会删除 JSON 之外的数据。
      </p>
      <el-checkbox v-model="saveDialog.enable">保存后启用并激活</el-checkbox>
      <div slot="footer">
        <el-button size="mini" @click="saveDialog.visible = false">取 消</el-button>
        <el-button type="primary" size="mini" :loading="saving" @click="submitSave">确认保存</el-button>
      </div>
    </el-dialog>

    <!-- 通用 lookup 选择对话框(角色/操作等字段点右侧搜索图标打开) -->
    <sks-lookup-dialog
      ref="lookupDialog"
      :lookup="lookup.name"
      :title="lookup.title"
      :mbo-data="lookup.row"
      :src-keys="lookup.srcKeys"
      :target-keys="lookup.targetKeys"
      :relation-object="lookup.relationObject"
      @selectrecord="markDirty" />
    </div>
  </section>
</template>

<script>
import { workflowDetail, workflowImport, workflowDeactivate, workflowDisable } from '@/api/wfdesign'
import SKsLookupDialog from '@/views/components/skslookup/SKsLookupDialog.vue'
import { getLookupKeyColumns } from '@/views/components/skslookup/sksLookup'

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
// 子表列/详细信息定义(columns=表格列, details=Maximo 各 table 的 tabledetails 字段)
// 键字段 readonly: 由框架分配, 不参与编辑
var SUB_TABLE_DEFS = {
  wfactions: {
    label: '出线操作',
    readonly: true, // 字段通过「编辑属性」对话框维护, 与 Maximo 一致
    columns: [
      { prop: 'actionId', label: '操作ID', width: 80, readonly: true },
      { prop: 'sequence', label: '顺序', width: 70 },
      { prop: 'action', label: '操作(ACTION)', minWidth: 130 },
      { prop: 'instruction', label: '操作说明', minWidth: 150 },
      { prop: 'memberNodeId', label: '目标节点', width: 160, type: 'node' },
      { prop: 'isPositive', label: '正向', width: 70, type: 'bool' }
    ],
    details: [] // 出线操作无 tabledetails, 详细信息即「操作属性」对话框
  },
  wfassignment: {
    label: '任务分配',
    // 编辑态下展开行的「详细信息」可直接编辑(这些字段脚本 buildWfAssignment/saveOrUpdateWfAssignments 都会读写)
    editableDetails: true,
    columns: [
      // 角色: Maximo lookup "role"(对象 MAXROLE)
      { prop: 'roleId', label: '角色', minWidth: 120, lookup: 'role', lookupObject: 'MAXROLE' },
      { prop: 'description', label: '任务描述', minWidth: 160 },
      { prop: 'emailNotification', label: '邮件通知', width: 85, type: 'bool' },
      { prop: 'timelimit', label: '时限', width: 85 }
    ],
    // assignments_table_details
    details: [
      { prop: 'roleId', label: '角色' },
      { prop: 'relationship', label: '关系' },
      { prop: 'app', label: '应用' },
      { prop: 'description', label: '任务描述' },
      { prop: 'escRole', label: '升级角色' },
      { prop: 'templateId', label: '通讯模板' },
      { prop: 'condition', label: '条件(USERSQL)', type: 'textarea', span: 24 },
      { prop: 'timelimit', label: '时限' },
      { prop: 'priority', label: '优先级' },
      { prop: 'emailNotification', label: '邮件通知', type: 'bool' },
      { prop: 'calendarBased', label: '基于日历', type: 'bool' },
      { prop: 'conditionClass', label: '自定义类' },
      { prop: 'keepOrigAssgn', label: '保留原分配', type: 'bool' },
      { prop: 'assignCode', label: '人员' },
      // 分配状态: 由框架维护, 设计器中恒为 DEFAULT, 只读展示
      { prop: 'assignStatus', label: '分配状态', readonly: true },
      { prop: 'groupNum', label: '组号' }
    ]
  },
  wfnotifications: {
    label: '通知',
    columns: [
      { prop: 'uniqueId', label: '通知ID', width: 85, readonly: true },
      { prop: 'templateId', label: '通讯模板', minWidth: 180 },
      // 接收人: Maximo lookup "role"(对象 MAXROLE)
      { prop: 'sendTo', label: '接收人', minWidth: 140, lookup: 'role', lookupObject: 'MAXROLE' }
    ],
    // notifications_table_details / waitnotify_table_details
    details: [
      { prop: 'templateId', label: '通讯模板' },
      { prop: 'sendTo', label: '接收人' },
      { prop: 'subject', label: '主题' },
      { prop: 'message', label: '消息' }
    ]
  }
}

// 各节点类型对应的属性对话框(字段与关联子表严格对照 Maximo wfdesign.xml 中同名的 dialog)
var NODE_DIALOGS = {
  // wfstartproperties / wfstopproperties: 只有「节点类型无可编辑属性。」
  WFSTART: { label: '开始节点属性', fields: [], tables: [] },
  WFSTOP: { label: '停止节点属性', fields: [], tables: [] },
  // wftaskproperties: 任务分配 + 通知
  WFTASK: {
    label: '任务节点属性',
    fields: [
      { prop: 'app', label: '应用', span: 12 },
      { prop: 'timelimit', label: '时限', span: 6 },
      { prop: 'displayOne', label: '显示一个', type: 'bool', span: 6 },
      { prop: 'taskType', label: '任务类型', span: 12 },
      {
        prop: 'firstComplete',
        label: '执行接受操作',
        type: 'radio',
        span: 24,
        options: [
          { value: true, label: '当任何任务分配被接受时' },
          { value: false, label: '当所有任务分配都被接受时' }
        ]
      }
    ],
    tables: ['wfassignment', 'wfnotifications']
  },
  // wfconditionproperties: 条件 + 自定义类, 无子表
  WFCONDITION: {
    label: '条件节点属性',
    fields: [
      { prop: 'condition', label: '条件', type: 'textarea', span: 24 },
      { prop: 'customClass', label: '自定义类', span: 24 }
    ],
    tables: []
  },
  // wfinputproperties: 操作表(操作的通知在「输入操作属性」里维护)
  WFINPUT: {
    label: '手工输入节点属性',
    fields: [
      { prop: 'displayOne', label: '显示一个', type: 'bool', span: 12 }
    ],
    tables: ['wfactions']
  },
  // wfinteractionproperties: 目标标题/目标主体, 无子表
  WFINTERACTION: {
    label: '交互节点属性',
    fields: [
      { prop: 'app', label: '应用程序', span: 12 },
      { prop: 'stayCurrentApp', label: '保持当前应用', type: 'bool', span: 12 },
      { prop: 'tabName', label: '选项卡名', span: 12 },
      { prop: 'action', label: '操作', span: 12 },
      { prop: 'relation', label: '关系', span: 12 },
      { prop: 'launchProcess', label: '启动过程', span: 12 },
      { prop: 'directions', label: '目标标题', span: 12 },
      { prop: 'directionsLongDescription', label: '目标主体', type: 'textarea', span: 24 }
    ],
    tables: []
  },
  // wfsubprocessproperties: 子过程名, 无子表
  WFSUBPROCESS: {
    label: '子过程节点属性',
    fields: [
      { prop: 'subProcessName', label: '子过程名', span: 24 }
    ],
    tables: []
  },
  // wfwaitproperties: 等待列表(单条: 事件名) + 通知
  WFWAIT: {
    label: '等待节点属性',
    fields: [
      { prop: 'eventName', label: '等待事件', span: 12 }
    ],
    tables: ['wfnotifications']
  }
}

// 操作属性对话框: 出线操作(wfactionproperties) / 手工输入节点的操作(wfinputactionproperties)
var ACTION_DIALOGS = {
  wfaction: {
    label: '操作属性',
    fields: [
      // 操作: Maximo lookup "action"(对象 ACTION)
      { prop: 'action', label: '操作(ACTION)', span: 12, lookup: 'action' },
      { prop: 'isPositive', label: '正向', type: 'bool', span: 12, readonly: true },
      { prop: 'instruction', label: '说明', span: 24 },
      { prop: 'condition', label: '条件(USERSQL)', type: 'textarea', span: 24 },
      { prop: 'conditionClass', label: '自定义类', span: 24 }
    ],
    tables: ['wfnotifications']
  },
  wfinputaction: {
    label: '输入操作属性',
    fields: [
      { prop: 'instruction', label: '说明', span: 24 },
      { prop: 'action', label: '操作(ACTION)', span: 12, lookup: 'action' },
      { prop: 'isPositive', label: '正向', type: 'bool', span: 12, readonly: true },
      { prop: 'condition', label: '条件(USERSQL)', type: 'textarea', span: 24 },
      { prop: 'conditionClass', label: '自定义类', span: 24 }
    ],
    tables: ['wfnotifications']
  }
}

export default {
  name: 'WfDesignDetail',
  components: { 'sks-lookup-dialog': SKsLookupDialog },
  data() {
    return {
      GRID: 80, // Maximo 工作流设计器网格像素(系统属性 mxe.webclient.wfdesigner.pixelsPerNode, 默认80)
      loading: false,
      saving: false,
      editMode: false,
      dirty: false,
      editSnapshot: null, // 进入编辑模式时的整包快照, 取消时恢复
      activeTab: 'canvas',
      workflow: {},
      selectedNodeId: null,
      zoom: 1,
      spacing: 1.5, // 节点间距倍率(1 = Maximo 默认 80px/格), 只影响画布显示, 不改节点的网格坐标
      suppressClick: false, // 拖拽结束后的第一次 click 不改选中
      dragState: null, // {node,startX,startY,origX,origY,moved}
      contextMenu: { visible: false, x: 0, y: 0, nodeId: null }, // 节点右键菜单
      nodeDialog: { visible: false, nodeId: null }, // 节点对话框(查看/编辑)
      actionDialog: { visible: false, nodeId: null, index: -1 }, // 出线操作属性对话框
      // SksLookup 选择: 由字段定义(col.lookup)决定 lookup 名, 选择行的关键字段写入 row 的 targetKeys
      lookup: { name: '', title: '', relationObject: '', row: null, srcKeys: [], targetKeys: [] },
      subTableDefs: SUB_TABLE_DEFS,
      saveDialog: { visible: false, enable: true }
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
          if (a._delete === true) {
            return
          }
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
        ;(n.wfactions || []).forEach(function (a, i) {
          if (a._delete === true) {
            return
          }
          // _rowIndex: 指向所在节点 wfactions 数组的下标, 供"编辑属性"对话框定位原始对象
          rows.push(Object.assign({ ownerNodeId: n.nodeId, _rowIndex: i }, a))
        })
      })
      return rows
    },
    assignmentRows() {
      var rows = []
      ;(this.workflow.wfnodes || []).forEach(function (n) {
        ;(n.wfassignment || []).forEach(function (a) {
          if (a._delete === true) {
            return
          }
          rows.push(Object.assign({ ownerNodeId: n.nodeId }, a))
        })
      })
      return rows
    },
    notificationRows() {
      var rows = []
      var wf = this.workflow
      ;(wf.wfnotifications || []).forEach(function (x) {
        if (x._delete === true) {
          return
        }
        rows.push({ scope: '流程级', uniqueId: x.uniqueId, templateId: x.templateId })
      })
      ;(wf.wfnodes || []).forEach(function (n) {
        ;(n.wfnotifications || []).forEach(function (x) {
          if (x._delete === true) {
            return
          }
          rows.push({ scope: '节点 #' + n.nodeId, uniqueId: x.uniqueId, templateId: x.templateId })
        })
        ;(n.wfactions || []).forEach(function (a) {
          if (a._delete === true) {
            return
          }
          ;(a.wfnotifications || []).forEach(function (x) {
            if (x._delete === true) {
              return
            }
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
          if (g._delete === true) {
            return
          }
          rows.push(Object.assign({ ownerNodeId: n.nodeId }, g))
        })
      })
      return rows
    },
    /** 网格像素 = Maximo 每格像素(GRID) × 节点间距倍率; 画布渲染与拖拽换算是同一个坐标系 */
    gridPx() {
      return Math.round(this.GRID * this.spacing)
    },
    /** 画布上所有节点的内容包围盒(像素) */
    canvasBounds() {
      var minX = null
      var minY = null
      var maxX = 0
      var maxY = 0
      var grid = this.gridPx
      ;(this.workflow.wfnodes || []).forEach(function (n) {
        minX = minX === null ? n.x * grid : Math.min(minX, n.x * grid)
        minY = minY === null ? n.y * grid : Math.min(minY, n.y * grid)
        maxX = Math.max(maxX, n.x * grid + 80)
        maxY = Math.max(maxY, n.y * grid + 70)
      })
      if (minX === null) {
        minX = 0
        minY = 0
        maxX = 160
        maxY = 120
      }
      return { minX: minX, minY: minY, maxX: maxX, maxY: maxY }
    },
    /** 内容四周留白: 取网格整数倍, 保证内容与网格对齐 */
    canvasPadding() {
      return this.gridPx
    },
    /** 画布尺寸 = 内容包围盒 + 四周留白 */
    canvasSize() {
      var b = this.canvasBounds
      var pad = this.canvasPadding
      return { width: b.maxX - b.minX + pad * 2, height: b.maxY - b.minY + pad * 2 }
    },
    /** 内容整体平移到画布留白处, 使图形不贴左上角 */
    contentTransform() {
      var b = this.canvasBounds
      var pad = this.canvasPadding
      return 'translate(' + (pad - b.minX) + ',' + (pad - b.minY) + ')'
    },
    /** 节点对话框对应的节点(直接引用 workflow.wfnodes 中的对象, 编辑即改工作流数据) */
    dialogNode() {
      if (this.nodeDialog.nodeId === null) {
        return null
      }
      return this.nodeMap[this.nodeDialog.nodeId] || null
    },
    /** 节点类型子表对象(如 wftask), 编辑态由 ensureNodeDetail 预建 */
    dialogDetail() {
      var n = this.dialogNode
      var key = n ? TYPE_DETAIL_KEYS[n.nodeType] : null
      if (!n || !key) {
        return null
      }
      return n[key] || null
    },
    /** 当前节点类型对应的属性对话框定义(不同节点类型弹出各自对话框) */
    nodeDialogDef() {
      var n = this.dialogNode
      return n ? (NODE_DIALOGS[n.nodeType] || { label: '节点属性', fields: [], tables: [] }) : { label: '节点属性', fields: [], tables: [] }
    },
    nodeDialogTitle() {
      var n = this.dialogNode
      if (!n) {
        return '节点属性'
      }
      var base = NODE_DIALOGS[n.nodeType] ? NODE_DIALOGS[n.nodeType].label : (this.typeLabel(n.nodeType) + '节点属性')
      return base + ' #' + n.nodeId + (this.editMode ? '（编辑中）' : '')
    },
    /** 出线操作对话框对应的操作(直接引用 workflow.wfnodes[].wfactions[] 中的对象) */
    dialogAction() {
      if (this.actionDialog.nodeId === null || this.actionDialog.index < 0) {
        return null
      }
      var node = this.nodeMap[this.actionDialog.nodeId]
      var list = node ? (node.wfactions || []) : []
      return list[this.actionDialog.index] || null
    },
    /** 操作所在的节点(手工输入节点的操作用「输入操作属性」对话框) */
    dialogActionOwnerNode() {
      if (this.actionDialog.nodeId === null) {
        return null
      }
      return this.nodeMap[this.actionDialog.nodeId] || null
    },
    /** 操作属性对话框定义: 出线操作 vs 手工输入节点的操作 */
    actionDialogDef() {
      var owner = this.dialogActionOwnerNode
      var key = owner && owner.nodeType === 'WFINPUT' ? 'wfinputaction' : 'wfaction'
      return ACTION_DIALOGS[key]
    },
    actionDialogTitle() {
      var a = this.dialogAction
      if (!a) {
        return '操作属性'
      }
      var id = (a.actionId === null || a.actionId === undefined) ? '新建' : a.actionId
      return this.actionDialogDef.label + ' #' + id + (this.editMode ? '（编辑中）' : '')
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
      return { x: node.x * this.gridPx, y: node.y * this.gridPx }
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
      // 拖拽结束后的第一次 click 不改变选中
      if (this.suppressClick) {
        this.suppressClick = false
        return
      }
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
      // 画布不超出容器时保持 100%(由外层居中), 仅在超出时缩小
      if (this.canvasSize.width <= w) {
        this.zoom = 1
        return
      }
      this.zoom = Math.max(0.2, Math.round((w / this.canvasSize.width) * 100) / 100)
    },
    // === 编辑模式 ===
    handleEdit() {
      // 激活状态的修订被 Maximo 框架禁止修改, 直接拦截
      if (this.workflow.active === true) {
        this.$message.error('激活状态的流程修订不可修改，请先通过右上角「状态操作 - 取消激活过程」取消激活后再编辑')
        return
      }
      this.editSnapshot = JSON.parse(JSON.stringify(this.workflow))
      this.dirty = false
      this.editMode = true
      this.$message.success('已进入编辑模式：拖拽节点调整位置，双击节点或右键「编辑节点」打开节点对话框')
    },
    cancelEdit() {
      if (!this.dirty) {
        this.exitEdit()
        return
      }
      this.$confirm('放弃当前全部修改并恢复到打开编辑时的状态?', '确认放弃', { type: 'warning' })
        .then(() => {
          this.workflow = JSON.parse(JSON.stringify(this.editSnapshot))
          this.exitEdit()
        }).catch(() => {})
    },
    exitEdit() {
      this.editMode = false
      this.dirty = false
      this.editSnapshot = null
      this.selectedNodeId = null
      this.dragState = null
      this.saveDialog.visible = false
      this.closeContextMenu()
      this.nodeDialog.visible = false
      this.nodeDialog.nodeId = null
      this.actionDialog.visible = false
      this.actionDialog.nodeId = null
      this.actionDialog.index = -1
    },
    // === 画布拖拽 ===
    onNodeMouseDown(event, node) {
      // 仅编辑模式左键可拖拽; 右键留给上下文菜单(mousedown 的 preventDefault 会吞掉部分浏览器的 contextmenu)
      if (!this.editMode || event.button !== 0) {
        return
      }
      event.preventDefault()
      this.dragState = { node: node, startX: event.clientX, startY: event.clientY, origX: node.x, origY: node.y, moved: false }
      document.addEventListener('mousemove', this.onDragMove)
      document.addEventListener('mouseup', this.onDragEnd)
    },
    onDragMove(event) {
      var d = this.dragState
      if (!d) {
        return
      }
      if (Math.abs(event.clientX - d.startX) > 3 || Math.abs(event.clientY - d.startY) > 3) {
        d.moved = true
      }
      // 客户端像素差 -> 网格坐标(除以缩放与网格像素), 四舍五入吸附网格, 不允许负坐标
      var nx = Math.max(0, Math.round(d.origX + (event.clientX - d.startX) / (this.gridPx * this.zoom)))
      var ny = Math.max(0, Math.round(d.origY + (event.clientY - d.startY) / (this.gridPx * this.zoom)))
      if (nx !== d.node.x || ny !== d.node.y) {
        d.node.x = nx
        d.node.y = ny
        this.dirty = true
      }
    },
    onDragEnd() {
      var d = this.dragState
      document.removeEventListener('mousemove', this.onDragMove)
      document.removeEventListener('mouseup', this.onDragEnd)
      this.dragState = null
      if (d && d.moved) {
        this.suppressClick = true
      }
    },
    // === 节点右键菜单 ===
    onNodeContextMenu(event, node) {
      var el = this.$refs.canvasScroll
      if (!el || !node) {
        return
      }
      var rect = el.getBoundingClientRect()
      this.selectedNodeId = node.nodeId
      this.contextMenu.nodeId = node.nodeId
      this.contextMenu.x = event.clientX - rect.left + el.scrollLeft
      this.contextMenu.y = event.clientY - rect.top + el.scrollTop
      this.contextMenu.visible = true
      document.addEventListener('click', this.closeContextMenu)
    },
    closeContextMenu() {
      document.removeEventListener('click', this.closeContextMenu)
      this.contextMenu.visible = false
    },
    contextViewNode() {
      var node = this.nodeMap[this.contextMenu.nodeId]
      this.closeContextMenu()
      this.openNodeDetail(node)
    },
    contextEditNode() {
      this.contextViewNode()
    },
    // === 节点对话框 ===
    openNodeDetail(node) {
      if (!node) {
        return
      }
      this.selectedNodeId = node.nodeId
      this.closeContextMenu()
      if (this.editMode) {
        this.ensureNodeDetail(node)
      }
      this.nodeDialog.nodeId = node.nodeId
      this.nodeDialog.visible = true
    },
    /** 编辑态下为缺类型子表的节点预建对象(字段预置, 保证 v-model 响应式) */
    ensureNodeDetail(node) {
      var key = TYPE_DETAIL_KEYS[node.nodeType]
      var def = NODE_DIALOGS[node.nodeType]
      var fields = def ? def.fields : []
      if (!key || fields.length === 0 || node[key]) {
        return
      }
      var detail = {}
      fields.forEach(function (f) {
        detail[f.prop] = f.type === 'bool' ? false : (f.type === 'radio' ? f.options[0].value : '')
      })
      this.$set(node, key, detail)
    },
    /** 属性字段的只读展示值(默认取节点类型子表, 可传入操作等其他对象) */
    propText(field, obj) {
      var detail = obj || this.dialogDetail
      var v = detail ? detail[field.prop] : null
      if (field.type === 'bool') {
        return this.boolText(v === true)
      }
      if (field.type === 'radio' && field.options) {
        for (var i = 0; i < field.options.length; i++) {
          if (field.options[i].value === v) {
            return field.options[i].label
          }
        }
      }
      if (v === null || v === undefined || v === '') {
        return '-'
      }
      return String(v)
    },
    // === 出线操作属性对话框 ===
    openActionDetail(nodeId, index) {
      var node = this.nodeMap[nodeId]
      var list = node ? (node.wfactions || []) : []
      if (!list[index]) {
        return
      }
      this.actionDialog.nodeId = nodeId
      this.actionDialog.index = index
      this.actionDialog.visible = true
    },
    addActionNotification() {
      var action = this.dialogAction
      if (!action) {
        return
      }
      if (!action.wfnotifications) {
        this.$set(action, 'wfnotifications', [])
      }
      action.wfnotifications.push({ templateId: '', _new: true })
      this.markDirty()
    },
    markDirty() {
      this.dirty = true
    },
    /** 打开 SksLookup 选择对话框: 字段定义中的 lookup 决定 lookup 名, 选择行关键字段写回该字段 */
    openLookup(row, field) {
      if (!row || !field || !field.lookup) {
        return
      }
      const self = this
      this.lookup = {
        name: field.lookup,
        title: '选择' + (field.label || ''),
        relationObject: field.lookupObject || '',
        row: row,
        srcKeys: getLookupKeyColumns(field.lookup),
        targetKeys: [field.prop]
      }
      this.$nextTick(function () {
        if (self.$refs.lookupDialog) {
          self.$refs.lookupDialog.open()
        }
      })
    },
    /** 过滤掉已标记删除的行(提交时仍会带上 _delete 标记) */
    activeRows(list) {
      return (list || []).filter(function (r) {
        return r._delete !== true
      })
    },
    cellText(row, col) {
      var v = row[col.prop]
      if (col.type === 'bool') {
        return this.boolText(v)
      }
      if (v === null || v === undefined || v === '') {
        // 键字段由框架分配, 新增行保存前显示"新建"
        return col.readonly ? '新建' : ''
      }
      return String(v)
    },
    subRowStyle(obj) {
      if (obj && obj.row && obj.row._delete === true) {
        return { color: '#c0c4cc', textDecoration: 'line-through' }
      }
      return {}
    },
    /** 子表定义(列/详细信息/是否只读行内编辑) */
    subTableDef(name) {
      return SUB_TABLE_DEFS[name] || { label: name, columns: [], details: [] }
    },
    /** Maximo tabledetails: 展开行展示的详细信息(空值不显示, 布尔显示是/否) */
    detailItems(row, name) {
      var def = SUB_TABLE_DEFS[name]
      var fields = def ? (def.details || []) : []
      var items = []
      fields.forEach(function (f) {
        var v = row[f.prop]
        if (f.type === 'bool') {
          items.push({ label: f.label, value: v === true ? '是' : '否' })
          return
        }
        if (v === null || v === undefined || v === '') {
          return
        }
        items.push({ label: f.label, value: String(v) })
      })
      return items
    },
    /** 子表拍平(用于生成新行的业务键) */
    flattenSubTable(name) {
      var rows = []
      ;(this.workflow.wfnodes || []).forEach(function (n) {
        ;(n[name] || []).forEach(function (r) {
          rows.push(r)
        })
      })
      return rows
    },
    maxOf(list, prop) {
      var max = 0
      ;(list || []).forEach(function (r) {
        var v = parseInt(r[prop], 10)
        if (!isNaN(v) && v > max) {
          max = v
        }
      })
      return max
    },
    /** 新增子表行: actionId/uniqueId 由框架分配(留空), assignId/groupNum 为必填键需先生成 */
    addSubRow(name) {
      var node = this.dialogNode
      if (!node) {
        return
      }
      var row = null
      if (name === 'wfactions') {
        row = { isPositive: true, memberNodeId: null, action: '', condition: '', conditionClass: '', instruction: '', _new: true }
      } else if (name === 'wfassignment') {
        if (node.nodeType !== 'WFTASK') {
          this.$message.warning('仅任务节点(WFTASK)可维护定义分配')
          return
        }
        row = {
          assignId: this.maxOf(this.flattenSubTable('wfassignment'), 'assignId') + 1,
          roleId: '', relationship: '', assignCode: '', app: '', description: '',
          timelimit: '', priority: 0, groupNum: 0, assignStatus: 'DEFAULT', escRole: '',
          templateId: '', condition: '', conditionClass: '',
          emailNotification: false, calendarBased: false, keepOrigAssgn: false, _new: true
        }
      } else if (name === 'wfnotifications') {
        row = { templateId: '', _new: true }
      }
      if (!row) {
        return
      }
      if (!node[name]) {
        this.$set(node, name, [])
      }
      node[name].push(row)
      this.dirty = true
    },
    deleteSubRow(list, index) {
      var row = list[index]
      if (!row) {
        return
      }
      if (row._new === true) {
        list.splice(index, 1)
      } else {
        this.$set(row, '_delete', true)
      }
      this.dirty = true
    },
    undoSubRow(row) {
      this.$set(row, '_delete', false)
      this.dirty = true
    },
    /** 保存前校验: 通知必须有通讯模板(后端缺 templateId 会整包回滚) */
    validateNotifications() {
      var problems = []
      function check(list, scope) {
        (list || []).forEach(function (n) {
          if (n._delete !== true && !n.templateId) {
            problems.push(scope + ' 的通知缺少通讯模板')
          }
        })
      }
      var wf = this.workflow
      check(wf.wfnotifications, '流程级')
      ;(wf.wfnodes || []).forEach(function (node) {
        check(node.wfnotifications, '节点 #' + node.nodeId)
        ;(node.wfactions || []).forEach(function (a) {
          if (a._delete === true) {
            return
          }
          check(a.wfnotifications, '节点 #' + node.nodeId + ' / 操作 ' + a.actionId)
        })
      })
      return problems
    },
    // === 保存 ===
    handleSave() {
      var problems = this.validateNotifications()
      if (problems.length > 0) {
        this.$message.error('无法保存: ' + problems.join('；'))
        return
      }
      this.saveDialog.enable = this.workflow.enabled === true
      this.saveDialog.visible = true
    },
    submitSave() {
      var payload = JSON.parse(JSON.stringify(this.workflow))
      // 导入脚本仅在 data.enabled === true 且 _enable != false 时才走启用激活流程
      if (this.saveDialog.enable) {
        payload.enabled = true
      }
      this.saving = true
      workflowImport({ workflows: [payload] }, { _impMode: 'migration', _enable: this.saveDialog.enable ? 'true' : 'false' })
        .then(res => {
          this.handleSaveResult(res.data || res)
        })
        .catch(err => {
          this.$message.error('保存失败: ' + (err.message || String(err)))
        })
        .finally(() => {
          this.saving = false
        })
    },
    handleSaveResult(data) {
      if (data.status === 'error') {
        this.$message.error(data.message || '保存失败')
        return
      }
      var result = (data.result || [])[0] || {}
      if (result.status !== 'SUCCESS') {
        this.$message.error('保存失败: ' + (result.message || '未知错误'))
        return
      }
      var warnings = result.warnings || []
      if (warnings.length > 0) {
        this.$alert(warnings.join('<br/>'), '保存成功，启用/激活过程有警告', { dangerouslyUseHTMLString: true, type: 'warning' })
      } else {
        this.$message.success(result.message || '保存成功')
      }
      this.exitEdit()
      this.fetchDetail()
    },
    // === 状态操作(取消激活 / 禁用) ===
    handleStateCommand(command) {
      var label = command === 'deactivate' ? '取消激活过程' : '禁用过程'
      var tip = command === 'deactivate'
        ? '取消激活后该流程修订变回草稿状态，可在本页「编 辑」修改；已在流程中的记录不受影响。是否继续?'
        : '禁用后新记录不再进入该流程，已在流程中的记录不受影响。是否继续?'
      this.$confirm(tip, label, { type: 'warning' })
        .then(() => {
          this.submitStateChange(command, label)
        })
        .catch(() => {})
    },
    submitStateChange(command, label) {
      var api = command === 'deactivate' ? workflowDeactivate : workflowDisable
      this.saving = true
      api({
        processName: this.workflow.processName,
        processRev: this.workflow.processRev
      }).then(res => {
        this.handleStateResult(res.data || res, label)
      }).catch(err => {
        this.$message.error(label + '失败: ' + (err.message || String(err)))
      }).finally(() => {
        this.saving = false
      })
    },
    handleStateResult(data, label) {
      if (data.status === 'error') {
        this.$message.error(data.message || (label + '失败'))
        return
      }
      var result = ((data.workflows || {}).result || [])[0] || {}
      if (result.status !== 'SUCCESS') {
        this.$message.error(label + '失败: ' + (result.message || '未知错误'))
        return
      }
      var warnings = result.warnings || []
      if (warnings.length > 0) {
        this.$message.warning(warnings.join('；'))
      } else {
        this.$message.success(result.message || (label + '成功'))
      }
      this.fetchDetail()
    },
    goBack() {
      this.$router.push({ name: 'WfDesign' })
    },
    /** 顶部标签页标题: 在路由标题后追加流程描述, 便于区分多个已打开的工作流 */
    updateTagTitle() {
      var base = (this.$route.meta && this.$route.meta.title) || '工作流详情'
      var description = String(this.workflow.description || '').trim()
      this.$store.dispatch('tagsView/updateVisitedView', {
        path: this.$route.path,
        title: description ? base + '：' + description : base
      })
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
        this.updateTagTitle()
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
  },
  beforeDestroy() {
    document.removeEventListener('mousemove', this.onDragMove)
    document.removeEventListener('mouseup', this.onDragEnd)
    document.removeEventListener('click', this.closeContextMenu)
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
.header-actions .state-dropdown {
  margin-left: 10px;
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
.canvas-tools {
  display: flex;
  align-items: center;
}
.canvas-spacing {
  display: inline-flex;
  align-items: center;
  margin-left: 14px;
}
.canvas-spacing .spacing-label {
  font-size: 12px;
  color: #606266;
  margin-right: 6px;
}
.canvas-spacing .spacing-slider {
  width: 110px;
  margin-right: 4px;
}
.canvas-spacing .spacing-value {
  font-size: 12px;
  color: #909399;
  width: 36px;
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
  position: relative;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  height: 520px;
  overflow: auto;
  background: #fff;
  /* 内容由 svg 的网格底图绘制, 画布小于容器时整体居中 */
  display: flex;
}
.wf-svg {
  display: block;
  /* margin:auto 居中; 内容超出容器时仍可完整滚动 */
  margin: auto;
  flex: none;
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
/* 节点右键菜单 */
.node-context-menu {
  position: absolute;
  z-index: 2000;
  min-width: 110px;
  padding: 4px 0;
  background: #fff;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.15);
  font-size: 13px;
  color: #303133;
}
.node-context-menu .menu-item {
  padding: 7px 16px;
  cursor: pointer;
  line-height: 1.4;
}
.node-context-menu .menu-item:hover {
  background: #ecf5ff;
  color: #409eff;
}
.node-dialog-body {
  max-height: 70vh;
  overflow: auto;
  padding-right: 4px;
}
.node-form {
  margin-bottom: 4px;
}
.node-form .el-form-item {
  margin-bottom: 8px;
}
.lookup-icon {
  cursor: pointer;

  &:hover {
    color: #409eff;
  }
}
.prop-card {
  margin-bottom: 12px;
  border-radius: 4px;
}
.prop-card ::v-deep .el-card__header {
  padding: 8px 12px;
  background: #f5f7fa;
}
.prop-card ::v-deep .el-card__body {
  padding: 12px 12px 4px 12px;
}
.prop-card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}
.node-dialog-tip {
  margin: 14px 0 0 0;
  font-size: 12px;
  color: #909399;
}
.danger-text {
  color: #f56c6c;
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
  display: flex;
  justify-content: space-between;
  align-items: center;
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
.save-tip {
  margin: 0 0 12px 0;
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
}
</style>
