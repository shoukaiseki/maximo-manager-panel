<template>
  <section class="logger-level-page">
    <el-card>
      <div class="page-header-row">
        <div>
          <h2>日志级别管理</h2>
          <p class="page-summary">查询与配置 Maximo 运行时日志器级别。查询实时读取 Maximo；配置按分组持久化，点击“更新到 Maximo”下发当前分组的未忽略条目。</p>
        </div>
      </div>

      <el-tabs v-model="activeTab" class="logger-tabs">
        <!-- ============ Tab1: 日志级别查询 ============ -->
        <el-tab-pane label="日志级别查询" name="query">
          <div class="toolbar">
            <el-button type="primary" icon="el-icon-search" size="mini" :loading="queryLoading" @click="queryAllLoggers">查询所有日志级别</el-button>
            <el-radio-group v-model="matchMode" size="mini" style="margin-left:8px">
              <el-radio-button label="fuzzy">模糊</el-radio-button>
              <el-radio-button label="exact">精确</el-radio-button>
            </el-radio-group>
            <el-input v-model="queryFilter" placeholder="搜索日志器名称（模糊：空格分隔需全部包含）" clearable prefix-icon="el-icon-search" size="mini" style="width:320px;margin-left:8px" />
            <el-select v-model="levelFilter" multiple collapse-tags clearable placeholder="按级别过滤" size="mini" style="width:200px;margin-left:8px">
              <el-option v-for="lv in queryLevelOptions" :key="lv" :label="lv" :value="lv" />
            </el-select>
            <el-button type="warning" icon="el-icon-edit-outline" size="mini" :disabled="!querySelection.length" @click="openQueryBatchLevelDialog">设置级别({{ querySelection.length }})</el-button>
            <el-button type="success" icon="el-icon-folder-add" size="mini" :disabled="!querySelection.length" @click="openAddToGroupDialog(querySelection.map(r => r.loggerName))">加入分组({{ querySelection.length }})</el-button>
            <span class="count-tip" v-if="queryLoggers.length">共 {{ filteredQueryLoggers.length }} / {{ queryLoggers.length }} 条</span>
          </div>

          <el-table :data="filteredQueryLoggers" stripe size="small" style="width:100%" v-loading="queryLoading" max-height="560"
            @selection-change="onQuerySelectionChange"
            empty-text="暂无数据，点击“查询所有日志级别”开始查询">
            <el-table-column type="selection" width="40" />
            <el-table-column label="日志器名称" prop="loggerName" min-width="300" show-overflow-tooltip sortable />
            <el-table-column label="当前级别" width="110" align="center">
              <template slot-scope="scope">
                <el-tag :type="levelTagType(scope.row.level)" size="mini" effect="dark">{{ scope.row.level }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="更改级别" width="140" align="center">
              <template slot-scope="scope">
                <el-select :value="scope.row.level" size="mini" @change="val => changeLoggerLevel(scope.row, val)" :disabled="!isSettableLevel(scope.row.level)">
                  <el-option v-for="lv in levelOptions" :key="lv" :label="lv" :value="lv" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180" align="center">
              <template slot-scope="scope">
                <el-button type="text" size="small" icon="el-icon-refresh" @click="reloadSingleLogger(scope.row)">重新加载</el-button>
                <el-button type="text" size="small" icon="el-icon-folder-add" @click="openAddToGroupDialog([scope.row.loggerName])">加入分组</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- ============ Tab2: 日志级别配置（分组） ============ -->
        <el-tab-pane label="日志级别配置" name="config">
          <el-alert type="warning" :closable="false" show-icon style="margin-bottom:12px"
            title="配置按分组持久化到数据库。“默认配置”为系统默认记录器，不可删除且不可下发；“更新到 Maximo”仅下发当前选中用户分组的未忽略条目；Maximo 重启后运行时级别会恢复。" />

          <!-- 当前分组工具栏 -->
          <div class="toolbar">
            <el-button type="success" icon="el-icon-upload2" size="mini" :loading="pushLoading" :disabled="isDefaultGroup" @click="updateToMaximo">更新到 Maximo</el-button>
            <span v-if="saveLoading" class="count-tip"><i class="el-icon-loading"></i> 自动保存中...</span>
            <!-- 仅默认配置可添加行；其他组引用默认配置 -->
            <el-button v-if="isDefaultGroup" icon="el-icon-plus" size="mini" @click="addRow">添加行</el-button>
            <el-button icon="el-icon-refresh" size="mini" :loading="currentGroupLoading" @click="reloadCurrentGroup">重新加载</el-button>
            <el-button icon="el-icon-share" size="mini" @click="openCrossGroupDialog">跨组添加</el-button>
            <el-button icon="el-icon-sort" size="mini" :disabled="tableSelection.length === 0" @click="openBatchLevelDialog">批量改级别</el-button>
            <!-- 导入 JSON（增量）：默认分组或用户分组均可导入 -->
            <el-button icon="el-icon-upload2" size="mini" @click="openImportConfigDialog">导入JSON(增量)</el-button>
            <!-- 用户分组：重命名 -->
            <el-button v-if="!isDefaultGroup" icon="el-icon-edit" size="mini" @click="openGroupEditDialog(activeGroup)">重命名</el-button>
          </div>

          <!-- 分组标签页 -->
          <el-tabs v-model="activeGroupId" type="card" addable @tab-add="openCreateGroupDialog" @tab-remove="onGroupRemove" @tab-click="onGroupTabClick">
            <!-- 默认分组（不可关闭） -->
            <el-tab-pane :label="'默认配置 (' + defaultItems.length + ')'" name="default" :closable="false">
              <el-table :data="defaultItems" stripe size="small" style="width:100%" v-loading="defaultLoading" max-height="520" empty-text="暂无配置，点击“添加行”新增" @selection-change="onDefaultSelectionChange">
                <el-table-column type="selection" width="42" align="center" />
                <el-table-column label="#" type="index" width="50" align="center" />
                <el-table-column label="日志器名称" min-width="280">
                  <template slot-scope="scope">
                    <el-input v-model="scope.row.loggerName" placeholder="例如: maximo.script" size="mini" @change="onItemEdit(scope.row)" />
                  </template>
                </el-table-column>
                <el-table-column label="日志级别" width="120" align="center">
                  <template slot-scope="scope">
                    <el-select v-model="scope.row.level" size="mini" @change="onItemEdit(scope.row)">
                      <el-option v-for="lv in levelOptions" :key="lv" :label="lv" :value="lv" />
                    </el-select>
                  </template>
                </el-table-column>
                <el-table-column label="忽略" width="60" align="center">
                  <template slot-scope="scope">
                    <el-checkbox v-model="scope.row.ignored" @change="onItemEdit(scope.row)" />
                  </template>
                </el-table-column>
                <el-table-column label="描述" min-width="180">
                  <template slot-scope="scope">
                    <!-- 默认配置：描述可编辑 -->
                    <el-input v-model="scope.row.description" placeholder="可选" size="mini" @change="onItemEdit(scope.row)" />
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="140" align="center">
                  <template slot-scope="scope">
                    <el-button type="text" size="small" icon="el-icon-refresh" @click="reloadRowLevel(scope.row)">读取当前</el-button>
                    <el-button type="text" size="small" icon="el-icon-delete" style="color:#f56c6c" @click="removeRow(scope.$index)">删除</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>
            <!-- 用户分组 -->
            <el-tab-pane v-for="g in userGroups" :key="g.id" :label="g.name + ' (' + g.itemCount + ')'" :name="String(g.id)" closable>
              <el-table :data="g.items" stripe size="small" style="width:100%" v-loading="g.loading" max-height="520" empty-text="暂无配置，点击“添加行”新增" @selection-change="onUserSelectionChange">
                <el-table-column type="selection" width="42" align="center" />
                <el-table-column label="#" type="index" width="50" align="center" />
                <el-table-column label="日志器名称" min-width="280">
                  <template slot-scope="scope">
                    <!-- 用户分组：只读显示引用的默认配置日志器名称 -->
                    <span class="readonly-desc">{{ scope.row.loggerName || '-' }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="日志级别" width="120" align="center">
                  <template slot-scope="scope">
                    <el-select v-model="scope.row.level" size="mini" @change="onItemEdit(scope.row)">
                      <el-option v-for="lv in levelOptions" :key="lv" :label="lv" :value="lv" />
                    </el-select>
                  </template>
                </el-table-column>
                <el-table-column label="忽略" width="60" align="center">
                  <template slot-scope="scope">
                    <el-checkbox v-model="scope.row.ignored" @change="onItemEdit(scope.row)" />
                  </template>
                </el-table-column>
                <el-table-column label="描述" min-width="180">
                  <template slot-scope="scope">
                    <!-- 用户分组：描述只读，引用默认配置中的描述 -->
                    <span class="readonly-desc">{{ defaultItemDesc(scope.row.loggerName) || '-' }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="140" align="center">
                  <template slot-scope="scope">
                    <el-button type="text" size="small" icon="el-icon-refresh" @click="reloadRowLevel(scope.row)">读取当前</el-button>
                    <el-button type="text" size="small" icon="el-icon-delete" style="color:#f56c6c" @click="removeRow(scope.$index)">删除</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>
          </el-tabs>
        </el-tab-pane>

        <!-- ============ Tab3: MXLogger 日志管理 ============ -->
        <el-tab-pane label="MXLogger日志管理" name="mx">
          <el-alert type="info" :closable="false" show-icon style="margin-bottom:12px"
            title="先添加组，再在该组下添加节点。可跨组添加其它组的记录（主已存在则只新增子级）；logkey 由层级自动生成；编辑后先“保存”到本地库，再“更新到 Maximo”调用脚本 SKS_LOGGER_MANAGE 下发。" />
          <!-- 组选择与组管理 -->
          <div class="toolbar">
            <span style="font-weight:600;color:#303133">组:</span>
            <el-select v-model="mxGroupId" placeholder="选择组" size="mini" style="width:220px" :loading="mxGroupsLoading" clearable filterable @change="onMxGroupChange">
              <el-option v-for="g in mxGroups" :key="g.id" :label="g.name" :value="g.id">
                <span>{{ g.name }}</span>
                <span style="float:right;color:#909399;font-size:12px">{{ g.description }}</span>
              </el-option>
            </el-select>
            <el-button type="primary" icon="el-icon-plus" size="mini" @click="openMxGroupDialog('create')">添加组</el-button>
            <el-button type="warning" icon="el-icon-edit" size="mini" :disabled="!mxGroupId" @click="openMxGroupDialog('update')">改名</el-button>
            <el-button type="danger" icon="el-icon-delete" size="mini" :disabled="!mxGroupId" @click="removeMxGroup">删除组</el-button>
            <el-divider direction="vertical" />
            <el-button type="primary" icon="el-icon-search" size="mini" :loading="mxLoading" @click="loadMxTree">加载</el-button>
            <el-button type="success" icon="el-icon-plus" size="mini" :disabled="!mxGroupId" @click="addMxTop">添加根节点</el-button>
            <el-button type="warning" icon="el-icon-folder-checked" size="mini" :loading="mxSaving" :disabled="!mxGroupId || (!mxDirty && mxTree.length === 0)" @click="saveMxTree">保存{{ mxDirty ? '(有改动)' : '' }}</el-button>
            <el-button type="danger" icon="el-icon-s-promotion" size="mini" :loading="mxPushing" :disabled="!mxGroupId || mxTree.length === 0" @click="pushMxToMaximo">更新到 Maximo</el-button>
            <el-button type="info" icon="el-icon-download" size="mini" @click="openMxImportDialog">导入 JSON</el-button>
            <span class="count-tip" v-if="mxTree.length">共 {{ mxTree.length }} 个根节点</span>
          </div>
          <el-table :data="mxTree" row-key="key" border size="small" style="width:100%" v-loading="mxLoading" ref="mxTable"
            class="mx-tree-table"
            :tree-props="{ children: 'children' }" empty-text="暂无配置，点击“加载”或“添加根节点”">
            <el-table-column label="展开" width="80" align="center">
              <template slot-scope="scope">
                <el-button v-if="scope.row.children && scope.row.children.length" type="text" size="small"
                  :icon="isMxExpanded(scope.row) ? 'el-icon-remove-outline' : 'el-icon-circle-plus-outline'"
                  @click="toggleMxExpand(scope.row)">{{ isMxExpanded(scope.row) ? '收起' : '展开' }}</el-button>
                <span v-else style="color:#c0c4cc">-</span>
              </template>
            </el-table-column>
            <el-table-column label="日志器名称" min-width="200">
              <template slot-scope="scope">
                <el-input v-model="scope.row.logger" size="mini" placeholder="例如: sql" @change="onMxEdit" />
              </template>
            </el-table-column>
            <el-table-column label="logkey（自动生成）" min-width="280">
              <template slot-scope="scope">
                <span class="mx-logkey">{{ mxLogkey(scope.row) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="级别" width="110" align="center">
              <template slot-scope="scope">
                <el-select v-model="scope.row.level" size="mini" @change="onMxEdit">
                  <el-option v-for="lv in levelOptions" :key="lv" :label="lv" :value="lv" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="启用" width="70" align="center">
              <template slot-scope="scope">
                <el-checkbox v-model="scope.row.active" @change="onMxEdit" />
              </template>
            </el-table-column>
            <el-table-column label="备注 (sks:remark)" min-width="180">
              <template slot-scope="scope">
                <el-input v-model="scope.row.remark" size="mini" placeholder="可选备注" @change="onMxEdit" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="220" align="center">
              <template slot-scope="scope">
                <el-button v-if="!scope.row.__pid" type="text" size="small" icon="el-icon-plus" @click="addMxChild(scope.row)">添加子级</el-button>
                <el-button type="text" size="small" icon="el-icon-copy-document" @click="openMxCopyDialog(scope.row)">跨组添加</el-button>
                <el-button type="text" size="small" icon="el-icon-delete" style="color:#f56c6c" @click="removeMxNode(scope.row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 更新结果弹窗 -->
    <el-dialog title="更新到 Maximo 结果" :visible.sync="pushResult.visible" width="60%" append-to-body>
      <el-table :data="pushResult.rows" stripe size="small" max-height="60vh">
        <el-table-column label="日志器名称" prop="loggerName" min-width="260" show-overflow-tooltip />
        <el-table-column label="级别" prop="level" width="90" align="center" />
        <el-table-column label="状态" width="100" align="center">
          <template slot-scope="scope">
            <el-tag :type="statusTagType(scope.row.status)" size="mini">{{ scope.row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="说明" prop="reason" min-width="200" show-overflow-tooltip />
      </el-table>
    </el-dialog>

    <!-- 日志级别配置 JSON 导入弹窗 -->
    <el-dialog title="导入 JSON（增量）" :visible.sync="importConfigDialog.visible" width="680px" append-to-body>
      <p style="margin:0 0 8px;color:#606266;font-size:13px">
        粘贴 JSON：<code>{"loggers":[{"loggerName":"maximo.sql","level":"ERROR","ignore":false}]}</code>。
        默认配置按 loggerName 去重，已存在的跳过；<template v-if="!isDefaultGroup">当前分组<b>{{ currentGroupLabel }}</b>以 JSON 中的级别导入（已存在的更新级别），若默认配置中不存在该日志器则先自动补齐默认配置。</template><template v-else>导入到默认配置。</template>
      </p>
      <el-input v-model="importConfigDialog.text" type="textarea" :rows="12" placeholder='{"loggers":[{"loggerName":"maximo.sql","level":"ERROR","ignore":false},{"loggerName":"maximo.script","level":"DEBUG","ignore":false}]}' />
      <p style="margin:8px 0 0;color:#f56c6c;font-size:12px" v-if="importConfigDialog.error">{{ importConfigDialog.error }}</p>
      <div slot="footer">
        <el-button size="mini" @click="importConfigDialog.visible = false">取消</el-button>
        <el-button type="primary" size="mini" :loading="importConfigDialog.loading" @click="submitImportConfigDialog">导入</el-button>
      </div>
    </el-dialog>

    <el-dialog :title="groupEditDialog.mode === 'create' ? '新建分组' : '重命名分组'" :visible.sync="groupEditDialog.visible" width="460px" append-to-body>
      <el-form label-width="80px">
        <el-form-item label="分组名称">
          <el-input v-model="groupEditDialog.name" placeholder="例如: 生产环境" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="groupEditDialog.description" type="textarea" :rows="3" placeholder="可选" maxlength="200" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button size="mini" @click="groupEditDialog.visible = false">取消</el-button>
        <el-button type="primary" size="mini" :loading="groupEditDialog.loading" @click="submitGroupEditDialog">确定</el-button>
      </div>
    </el-dialog>

    <!-- 跨组添加弹窗 -->
    <el-dialog title="跨组添加日志器" :visible.sync="crossGroupDialog.visible" width="900px" append-to-body>
      <p style="margin:0 0 8px;color:#606266;font-size:13px">
        从其它来源的日志器中选择，添加到当前分组“<b>{{ currentGroupLabel }}</b>”（已存在的复选框不可勾选，确认后统一添加）。
      </p>
      <div style="margin-bottom:8px;display:flex;align-items:center;gap:8px">
        <el-select v-model="crossGroupDialog.sourceKey" size="mini" style="width:200px" @change="onCrossSourceChange">
          <el-option v-for="s in crossGroupDialog.sources" :key="s.key" :label="s.label" :value="s.key" />
        </el-select>
        <el-input v-model="crossGroupDialog.filter" size="mini" clearable prefix-icon="el-icon-search" placeholder="过滤日志器名称或默认配置描述" style="width:240px" @input="refreshCrossItems" />
      </div>
      <el-table :data="crossGroupDialog.filteredItems" size="mini" max-height="380" border style="width:100%"
          empty-text="该来源暂无日志器" v-loading="crossGroupDialog.tableLoading" @selection-change="onCrossSelectionChange">
        <el-table-column type="selection" width="42" align="center" :selectable="crossSelectable" />
        <el-table-column label="日志器名称" prop="loggerName" min-width="260" show-overflow-tooltip />
        <el-table-column label="来源级别" prop="level" width="100" align="center" />
        <el-table-column label="添加级别" width="100" align="center">
          <template slot-scope="scope">
            {{ scope.row.addLevel }}
          </template>
        </el-table-column>
        <el-table-column label="默认配置描述" min-width="180" show-overflow-tooltip>
          <template slot-scope="scope">
            {{ defaultItemDesc(scope.row.loggerName) || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="添加到当前组" width="110" align="center">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.inCurrent" type="info" size="mini">已存在</el-tag>
            <el-tag v-else type="success" size="mini">可添加</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <p style="margin:8px 0 0;color:#606266;font-size:13px">
        已选 <b>{{ crossGroupDialog.selected.length }}</b> 条（添加级别取默认配置中的级别，可添加 {{ crossAddableCount }} 条）
      </p>
      <div slot="footer">
        <el-button size="mini" @click="crossGroupDialog.visible = false">取消</el-button>
        <el-button type="primary" size="mini" :loading="crossGroupDialog.loading" @click="submitCrossGroupDialog">确认添加</el-button>
      </div>
    </el-dialog>

    <!-- MXLogger 组新建/改名弹窗 -->
    <el-dialog :title="mxGroupDialog.mode === 'create' ? '添加 MXLogger 组' : '改名 MXLogger 组'" :visible.sync="mxGroupDialog.visible" width="460px" append-to-body>
      <el-form label-width="80px">
        <el-form-item label="组名称">
          <el-input v-model="mxGroupDialog.name" placeholder="例如: 生产库" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="mxGroupDialog.description" type="textarea" :rows="2" placeholder="可选" maxlength="200" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button size="mini" @click="mxGroupDialog.visible = false">取消</el-button>
        <el-button type="primary" size="mini" :loading="mxGroupDialog.loading" @click="submitMxGroupDialog">确定</el-button>
      </div>
    </el-dialog>

    <!-- MXLogger 跨组添加弹窗：从其它组复制节点到当前组 -->
    <el-dialog title="跨组添加节点" :visible.sync="mxCopyDialog.visible" width="880px" append-to-body>
      <p style="margin:0 0 8px;color:#606266;font-size:13px">
        从其它组的记录复制到当前组“<b>{{ currentMxGroupLabel }}</b>”。若主记录已存在，则只把勾选的子级新增到该主记录下。
      </p>
      <div style="margin-bottom:8px;display:flex;align-items:center;gap:8px;flex-wrap:wrap">
        <el-select v-model="mxCopyDialog.srcGroupId" placeholder="选择来源组" size="mini" style="width:200px" @change="onMxCopySrcChange">
          <el-option v-for="g in mxOtherGroups" :key="g.id" :label="g.name" :value="g.id" />
        </el-select>
        <span v-if="mxCopyDialog.source" style="color:#909399;font-size:12px">来源: “{{ mxCopyDialog.source.name }}” 共 {{ mxCopyDialog.source.items.length }} 个根节点</span>
      </div>
      <el-table ref="mxCopyTable" :data="mxCopyDialog.filteredItems" size="mini" max-height="360" border style="width:100%"
        empty-text="请先选择来源组" v-loading="mxCopyDialog.loading" @selection-change="onMxCopySelectionChange">
        <el-table-column type="selection" width="42" align="center" :selectable="mxCopySelectable" />
        <el-table-column label="类型" width="70" align="center">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.isTop" size="mini" :type="scope.row.inCurrent ? 'info' : 'success'">{{ scope.row.inCurrent ? '主已存在' : '主记录' }}</el-tag>
            <el-tag v-else size="mini" type="warning">子级</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="节点名称" prop="logger" min-width="200" show-overflow-tooltip />
        <el-table-column label="级别" prop="level" width="90" align="center" />
        <el-table-column label="启用" width="70" align="center">
          <template slot-scope="scope">
            {{ scope.row.active === false ? '否' : '是' }}
          </template>
        </el-table-column>
        <el-table-column label="备注" prop="remark" min-width="140" show-overflow-tooltip />
      </el-table>
      <p style="margin:8px 0 0;color:#606266;font-size:13px">
        已选 <b>{{ mxCopyDialog.selected.length }}</b> 条（勾选主记录时自动带上其子级）
      </p>
      <div slot="footer">
        <el-button size="mini" @click="mxCopyDialog.visible = false">取消</el-button>
        <el-button type="primary" size="mini" :loading="mxCopyDialog.adding" @click="submitMxCopyDialog">添加到当前组</el-button>
      </div>
    </el-dialog>

    <!-- MXLogger JSON 导入弹窗：粘贴符合 SKS_LOGGER_MANAGE 格式的数组 -->
    <el-dialog title="导入 MXLogger JSON" :visible.sync="mxImportDialog.visible" width="720px" append-to-body>
      <p style="margin:0 0 8px;color:#606266;font-size:13px">
        粘贴 JSON 数组（格式与 SKS_LOGGER_MANAGE 请求体一致：<code>logger / loglevel / logkey / active / children</code>）。解析合并到当前组“<b>{{ currentMxGroupLabel }}</b>”：主记录已存在则合并其子级（避免重复），不存在则整条新增。
      </p>
      <el-input v-model="mxImportDialog.text" type="textarea" :rows="12" placeholder='[{"logger":"sql","loglevel":"ERROR","active":1,"children":[{"logger":"GRAPHITE","loglevel":"ERROR","active":1}]}]' />
      <p style="margin:8px 0 0;color:#f56c6c;font-size:12px" v-if="mxImportDialog.error">{{ mxImportDialog.error }}</p>
      <div slot="footer">
        <el-button size="mini" @click="mxImportDialog.visible = false">取消</el-button>
        <el-button type="primary" size="mini" :loading="mxImportDialog.loading" @click="submitMxImportDialog">解析并合并</el-button>
      </div>
    </el-dialog>

    <!-- 从查询加入分组弹窗 -->
    <el-dialog title="加入分组" :visible.sync="addToGroupDialog.visible" width="460px" append-to-body>
      <p style="margin:0 0 8px;color:#606266;font-size:13px">将选中的 <b>{{ addToGroupDialog.loggerNames.length }}</b> 个日志器加入：（已存在的会跳过，默认级别 INFO）</p>
      <el-select v-model="addToGroupDialog.targetGroupId" placeholder="选择目标分组" style="width:100%">
        <el-option label="默认配置" value="default" />
        <el-option v-for="g in userGroups" :key="g.id" :label="g.name" :value="String(g.id)" />
      </el-select>
      <div slot="footer">
        <el-button size="mini" @click="addToGroupDialog.visible = false">取消</el-button>
        <el-button type="primary" size="mini" :loading="addToGroupDialog.loading" @click="submitAddToGroupDialog">确定</el-button>
      </div>
    </el-dialog>

    <!-- 批量更改日志级别弹窗 -->
    <el-dialog title="批量更改日志级别" :visible.sync="batchLevelDialog.visible" width="480px" append-to-body>
      <p style="margin:0 0 8px;color:#606266;font-size:13px">将选中的 <b>{{ batchLevelDialog.items.length }}</b> 条日志器级别统一设为：</p>
      <el-select v-model="batchLevelDialog.level" placeholder="选择级别" style="width:100%">
        <el-option v-for="lv in levelOptions" :key="lv" :label="lv" :value="lv" />
      </el-select>
      <div slot="footer">
        <el-button size="mini" @click="batchLevelDialog.visible = false">取消</el-button>
        <el-button type="primary" size="mini" :loading="batchLevelDialog.loading" @click="submitBatchLevelDialog">确定</el-button>
      </div>
    </el-dialog>

    <!-- 查询页多选设置级别弹窗（直接调用 Maximo 更新接口） -->
    <el-dialog title="设置日志级别" :visible.sync="queryBatchLevelDialog.visible" width="480px" append-to-body>
      <p style="margin:0 0 8px;color:#606266;font-size:13px">
        将查询结果中勾选的 <b>{{ querySelection.length }}</b> 条日志器（其中可设置级别 <b>{{ querySettableCount }}</b> 条，继承/未设置的将跳过）统一更新为：
      </p>
      <el-select v-model="queryBatchLevelDialog.level" placeholder="选择级别" style="width:100%">
        <el-option v-for="lv in levelOptions" :key="lv" :label="lv" :value="lv" />
      </el-select>
      <div slot="footer">
        <el-button size="mini" @click="queryBatchLevelDialog.visible = false">取消</el-button>
        <el-button type="primary" size="mini" :loading="queryBatchLevelDialog.loading" @click="submitQueryBatchLevelDialog">确定并更新</el-button>
      </div>
    </el-dialog>
  </section>
</template>

<script>
import {
  queryLoggerLevel, updateLoggerLevel,
  listLoggerConfig, saveLoggerConfig, importLoggerConfig,
  listLoggerGroups, createLoggerGroup, updateLoggerGroup, deleteLoggerGroup,
  listLoggerGroupItems, saveLoggerGroupItems, addLoggerToGroup,
  listLoggerMx, saveLoggerMx, pushLoggerMxToMaximo,
  listLoggerMxGroups, createLoggerMxGroup, updateLoggerMxGroup, deleteLoggerMxGroup
} from '@/api/loggerlevel'

const LEVEL_OPTIONS = ['DEBUG', 'INFO', 'WARN', 'ERROR']
// 查询结果中可能出现的所有级别类型（含继承/未设置）
const QUERY_LEVEL_FILTERS = ['DEBUG', 'INFO', 'WARN', 'ERROR', 'FATAL', 'INHERITED']

export default {
  name: 'LoggerLevelManager',
  data() {
    return {
      activeTab: 'query',
      levelOptions: LEVEL_OPTIONS,

      // 查询 tab
      queryLoading: false,
      queryLoggers: [],
      queryFilter: '',
      matchMode: 'fuzzy',
      levelFilter: [],
      querySelection: [],

      // 配置 tab - 分组
      activeGroupId: 'default',
      defaultItems: [],
      defaultLoading: false,
      userGroups: [],
      // 当前分组表格勾选状态
      tableSelection: [],
      // 批量更改日志级别弹窗
      batchLevelDialog: { visible: false, items: [], level: 'INFO', loading: false },
      // 查询页多选设置级别弹窗
      queryBatchLevelDialog: { visible: false, level: 'INFO', loading: false },

      // ===== MXLogger 日志管理 tab =====
      mxLoading: false,
      mxSaving: false,
      mxPushing: false,
      // 组管理
      mxGroups: [],            // [{ id, name, description }]
      mxGroupsLoading: false,
      mxGroupId: null,         // 当前选中组ID；null 表示未选组
      mxGroupDialog: { visible: false, mode: 'create', id: null, name: '', description: '', loading: false },
      // 跨组复制
      mxCopyDialog: { visible: false, srcGroupId: null, source: null, filteredItems: [], selected: [], adding: false, loading: false },
      // JSON 导入
      mxImportDialog: { visible: false, text: '', error: '', loading: false },
      mxTree: [], // 树形数据（顶层节点含 children）
      mxDirty: false, // 是否有未保存改动

      // 通用 loading
      saveLoading: false,
      pushLoading: false,
      saveTimer: null,
      pendingSave: false,

      pushResult: { visible: false, rows: [] },

      // 弹窗
      importConfigDialog: { visible: false, text: '', error: '', loading: false },
      groupEditDialog: { visible: false, mode: 'create', id: null, name: '', description: '', loading: false },
      crossGroupDialog: {
        visible: false,
        sources: [], // [{key, label, items:[{loggerName, level}]}]
        sourceKey: '',
        filter: '',
        allItems: [], // 当前来源的全部条目（未过滤）
        filteredItems: [], // 过滤+标记后的显示行
        selected: [], // 勾选的显示行
        tableLoading: false,
        loading: false
      },
      addToGroupDialog: { visible: false, targetGroupId: 'default', loggerNames: [], loading: false }
    }
  },
  computed: {
    queryLevelOptions() { return QUERY_LEVEL_FILTERS },
    isDefaultGroup() { return this.activeGroupId === 'default' },
    activeGroup() { return this.userGroups.find(g => String(g.id) === this.activeGroupId) },
    currentItems() { return this.isDefaultGroup ? this.defaultItems : (this.activeGroup ? this.activeGroup.items : []) },
    currentGroupLoading() { return this.isDefaultGroup ? this.defaultLoading : (this.activeGroup ? this.activeGroup.loading : false) },
    currentGroupLabel() { return this.isDefaultGroup ? '默认配置' : (this.activeGroup ? this.activeGroup.name : '') },
    // 跨组添加弹窗：当前勾选中可实际添加（不在当前组）的数量
    crossAddableCount() {
      return this.crossGroupDialog.selected.filter(r => !r.inCurrent).length
    },
    // 查询页多选日志器中可设置级别（非继承/未设置）的数量
    querySettableCount() {
      return this.querySelection.filter(r => this.isSettableLevel(r.level)).length
    },
    filteredQueryLoggers() {
      let list = this.queryLoggers
      if (this.levelFilter.length > 0) {
        const allow = this.levelFilter.map(v => v.toUpperCase())
        list = list.filter(l => allow.includes((l.level || '').toUpperCase()))
      }
      const keyword = this.queryFilter.trim()
      if (!keyword) return list
      if (this.matchMode === 'exact') {
        const kw = keyword.toLowerCase()
        return list.filter(l => (l.loggerName || '').toLowerCase() === kw)
      }
      const terms = keyword.toLowerCase().split(/\s+/).filter(t => t.length > 0)
      return list.filter(l => {
        const name = (l.loggerName || '').toLowerCase()
        return terms.every(t => name.includes(t))
      })
    },
    // ===== MXLogger 相关 computed =====
    currentMxGroup() { return this.mxGroups.find(g => g.id === this.mxGroupId) || null },
    currentMxGroupLabel() { return this.currentMxGroup ? this.currentMxGroup.name : '' },
    // 跨组添加来源：其它组
    mxOtherGroups() { return this.mxGroups.filter(g => g.id !== this.mxGroupId) }
  },
  created() {
    this.loadDefaultItems()
    this.loadUserGroups()
    this.loadMxGroups()
  },
  watch: {
    activeTab(val) {
      // 首次进入 MXLogger tab 时加载组列表
      if (val === 'mx') {
        if (!this.mxGroups.length) this.loadMxGroups()
        if (!this.mxGroupId && this.mxGroups.length && !this.mxTree.length && !this.mxLoading) this.loadMxTree()
      }
    }
  },
  methods: {
    // ============ MXLogger 日志管理 tab ============
    // ---- 组管理 ----
    async loadMxGroups() {
      this.mxGroupsLoading = true
      try {
        const res = await listLoggerMxGroups()
        if (res && res.code === 200) {
          this.mxGroups = res.data || []
          // 若当前选中的组不存在则清空（避免残留无效 id）
          if (this.mxGroupId && !this.mxGroups.some(g => g.id === this.mxGroupId)) {
            this.mxGroupId = null
            this.mxTree = []
          }
        } else {
          this.$message.error((res && res.message) || '加载 MXLogger 组失败')
        }
      } catch (err) {
        this.$message.error('加载 MXLogger 组失败: ' + (err.message || String(err)))
      } finally {
        this.mxGroupsLoading = false
      }
    },
    openMxGroupDialog(mode) {
      const g = mode === 'update' ? this.currentMxGroup : null
      this.mxGroupDialog = { visible: true, mode: mode, id: g ? g.id : null, name: g ? g.name : '', description: g ? (g.description || '') : '', loading: false }
    },
    async submitMxGroupDialog() {
      const name = (this.mxGroupDialog.name || '').trim()
      if (!name) { this.$message.warning('请输入组名称'); return }
      this.mxGroupDialog.loading = true
      try {
        if (this.mxGroupDialog.mode === 'create') {
          const res = await createLoggerMxGroup({ name: name, description: this.mxGroupDialog.description })
          if (res && res.code === 200) {
            this.$message.success('添加组成功')
            await this.loadMxGroups()
            // 自动选中新组并加载空树
            this.mxGroupId = res.data.id
            this.mxTree = []
            this.mxDirty = false
            this.mxGroupDialog.visible = false
          } else {
            this.$message.error((res && res.message) || '添加组失败')
          }
        } else {
          const res = await updateLoggerMxGroup(this.mxGroupDialog.id, { name: name, description: this.mxGroupDialog.description })
          if (res && res.code === 200) {
            this.$message.success('改名成功')
            await this.loadMxGroups()
            this.mxGroupDialog.visible = false
          } else {
            this.$message.error((res && res.message) || '改名失败')
          }
        }
      } catch (err) {
        this.$message.error('操作失败: ' + (err.message || String(err)))
      } finally {
        this.mxGroupDialog.loading = false
      }
    },
    removeMxGroup() {
      const g = this.currentMxGroup
      if (!g) return
      this.$confirm('删除组“' + g.name + '”将同时删除该组下的所有配置节点，确定删除？', '提示', {
        confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning'
      }).then(async () => {
        try {
          const res = await deleteLoggerMxGroup(g.id)
          if (res && res.code === 200) {
            this.$message.success('删除组成功')
            if (this.mxGroupId === g.id) { this.mxGroupId = null; this.mxTree = []; this.mxDirty = false }
            await this.loadMxGroups()
          } else {
            this.$message.error((res && res.message) || '删除组失败')
          }
        } catch (err) {
          this.$message.error('删除组失败: ' + (err.message || String(err)))
        }
      }).catch(() => {})
    },
    // 切换组：加载该组树（未保存改动前直接切换将丢弃，先提示）
    onMxGroupChange() {
      this.mxTree = []
      this.mxDirty = false
      if (this.mxGroupId) this.loadMxTree()
    },
    async loadMxTree() {
      if (!this.mxGroupId) {
        this.$message.warning('请先选择组')
        return
      }
      this.mxLoading = true
      try {
        const res = await listLoggerMx(this.mxGroupId)
        if (res && res.code === 200) {
          const rows = (res.data || []).map(n => this.normalizeMxNode(n))
          this.mxTree = rows
          this.mxDirty = false
        } else {
          this.$message.error((res && res.message) || '加载 MXLogger 配置失败')
        }
      } catch (err) {
        this.$message.error('加载 MXLogger 配置失败: ' + (err.message || String(err)))
      } finally {
        this.mxLoading = false
      }
    },
    // 后端行 → 前端节点（确保 key；子节点带 __pid 用于 logkey 生成与删除）
    normalizeMxNode(n, parentKey) {
      const node = {
        key: 'mx-' + (n.id !== undefined ? n.id : Math.random().toString(36).slice(2, 8)),
        id: n.id,
        logger: n.logger || '',
        level: n.level || 'ERROR',
        active: n.active !== false,
        remark: n.remark || '',
        children: []
      }
      if (parentKey) node.__pid = parentKey
      if (n.children && n.children.length) {
        node.children = n.children.map(c => this.normalizeMxNode(c, node.key))
      }
      return node
    },
    // 生成 logkey：顶层 log4j.logger.maximo.<logger>，子级追加 .<子logger>
    mxLogkey(node) {
      const name = (node.logger || '').trim()
      if (!name) return ''
      if (node.__pid != null) {
        const parent = this.mxTree.find(p => p.key === node.__pid)
        const parentName = parent ? (parent.logger || '').trim() : ''
        return parentName ? 'log4j.logger.maximo.' + parentName + '.' + name : 'log4j.logger.maximo.' + name
      }
      return 'log4j.logger.maximo.' + name
    },
    // 标记改动（保存按钮提示）
    onMxEdit() { this.mxDirty = true },
    // 控制树行展开/收起（供"展开"独立列用）
    // 树表格展开状态在 store.states.treeData[key].expanded（响应式，直接读取用于按钮图标和切换）
    isMxExpanded(node) {
      const table = this.$refs.mxTable
      if (!table || !node) return false
      const treeData = table.store && table.store.states.treeData
      const info = treeData && treeData[node.key]
      return !!(info && info.expanded)
    },
    toggleMxExpand(node) {
      const table = this.$refs.mxTable
      if (!table || !node) return
      table.toggleRowExpansion(node, !this.isMxExpanded(node))
    },
    addMxTop() {
      this.mxTree.push({
        key: 'mx-new-' + Math.random().toString(36).slice(2, 8),
        id: null, logger: '', level: 'ERROR', active: true, remark: '', children: []
      })
      this.mxDirty = true
    },
    addMxChild(parent) {
      if (!parent.children) parent.children = []
      parent.children.push({
        key: 'mx-new-' + Math.random().toString(36).slice(2, 8),
        __pid: parent.key,
        id: null, logger: '', level: 'ERROR', active: true, remark: ''
      })
      this.mxDirty = true
      // 展开父节点，确保新子级可见（默认收起）
      this.$nextTick(() => {
        this.toggleMxExpandQuiet(parent)
      })
    },
    // 只展开、不切换（添加子级后自动展开用）
    toggleMxExpandQuiet(node) {
      const table = this.$refs.mxTable
      if (!table || !node) return
      if (!this.isMxExpanded(node)) table.toggleRowExpansion(node, true)
    },
    removeMxNode(node) {
      if (node.__pid != null) {
        const parent = this.mxTree.find(p => p.key === node.__pid)
        if (parent && parent.children) {
          const idx = parent.children.findIndex(c => c.key === node.key)
          if (idx !== -1) parent.children.splice(idx, 1)
        }
      } else {
        const idx = this.mxTree.findIndex(t => t.key === node.key)
        if (idx !== -1) this.mxTree.splice(idx, 1)
      }
      this.mxDirty = true
    },
    // 构建保存数据：树 → 扁平数组（顶层带 children，子级不含 children 属性）
    buildMxSaveList() {
      const list = []
      for (const t of this.mxTree) {
        const logger = (t.logger || '').trim()
        if (!logger) continue
        const top = {
          logger: logger,
          level: (t.level || 'ERROR').toUpperCase(),
          active: t.active !== false,
          remark: t.remark || '',
          sortOrder: list.length,
          children: []
        }
        if (t.children && t.children.length) {
          top.children = t.children
            .filter(c => (c.logger || '').trim())
            .map((c, i) => ({
              logger: (c.logger || '').trim(),
              level: (c.level || 'ERROR').toUpperCase(),
              active: c.active !== false,
              remark: c.remark || '',
              sortOrder: i
            }))
        }
        list.push(top)
      }
      return list
    },
    async saveMxTree() {
      if (!this.mxGroupId) {
        this.$message.warning('请先选择组')
        return
      }
      const list = this.buildMxSaveList()
      this.mxSaving = true
      try {
        const res = await saveLoggerMx(list, this.mxGroupId)
        if (res && res.code === 200) {
          this.mxTree = (res.data || []).map(n => this.normalizeMxNode(n))
          this.mxDirty = false
          this.$message.success('已保存 ' + list.length + ' 个根节点配置')
        } else {
          this.$message.error((res && res.message) || '保存失败')
        }
      } catch (err) {
        this.$message.error('保存失败: ' + (err.message || String(err)))
      } finally {
        this.mxSaving = false
      }
    },
    // 构建推送到 Maximo 的数据（带 logkey 与 loglevel 字段名）
    buildMxPushList() {
      const list = []
      for (const t of this.mxTree) {
        const logger = (t.logger || '').trim()
        if (!logger) continue
        const top = {
          logger: logger,
          loglevel: (t.level || 'ERROR').toUpperCase(),
          logkey: this.mxLogkey(t),
          active: t.active !== false ? 1 : 0,
          children: []
        }
        if (t.remark) top['sks:remark'] = t.remark
        if (t.children && t.children.length) {
          top.children = t.children
            .filter(c => (c.logger || '').trim())
            .map(c => {
              const child = {
                logger: (c.logger || '').trim(),
                loglevel: (c.level || 'ERROR').toUpperCase(),
                logkey: this.mxLogkey(c),
                active: c.active !== false ? 1 : 0
              }
              if (c.remark) child['sks:remark'] = c.remark
              return child
            })
        }
        list.push(top)
      }
      return list
    },
    async pushMxToMaximo() {
      if (this.mxDirty) {
        // 有未保存改动：先保存到本地库，再继续推送
        await this.saveMxTree()
      }
      const list = this.buildMxPushList()
      if (list.length === 0) { this.$message.warning('没有可更新的日志器配置'); return }
      this.mxPushing = true
      try {
        const res = await pushLoggerMxToMaximo(list)
        const data = res.data || res
        // 真实返回: { status: 'success', message, total, results: [{ logger, loglevel, logkey, active, status, message, children: [{ logger, status, message }] }] }
        if (data && data.status === 'success') {
          // 拍平 results 及其子级用于结果弹窗展示（层级以父>子描述）
          const rows = []
          ;(data.results || []).forEach(r => {
            rows.push({
              loggerName: r.logger + (r.loglevel ? ' [当前 ' + r.loglevel + ']' : ''),
              level: r.loglevel || '',
              status: r.status || 'success',
              reason: r.message || ''
            })
            ;(r.children || []).forEach(c => {
              rows.push({
                loggerName: '  └ 子级: ' + c.logger,
                level: '',
                status: c.status || 'success',
                reason: c.message || ''
              })
            })
          })
          this.pushResult.rows = rows
          this.pushResult.visible = true
          this.$message.success((data.message || '更新完成') + '（共 ' + (data.total != null ? data.total : list.length) + ' 个根节点）')
        } else {
          this.$message.error((data && data.message) || '更新失败（接口返回异常）')
        }
      } catch (err) {
        this.$message.error('更新失败: ' + (err.message || String(err)))
      } finally {
        this.mxPushing = false
      }
    },

    // ---- 跨组添加 ----
    openMxCopyDialog() {
      if (!this.mxGroupId) { this.$message.warning('请先选择组'); return }
      const others = this.mxOtherGroups
      if (others.length === 0) { this.$message.info('暂无可跨组添加的来源组'); return }
      const d = this.mxCopyDialog
      d.visible = true
      d.srcGroupId = others[0].id
      d.source = null
      d.filteredItems = []
      d.selected = []
      this.onMxCopySrcChange()
    },
    // 加载来源组树并生成可勾选列表（主记录 + 子级拍平，标记存在关系）
    async onMxCopySrcChange() {
      const d = this.mxCopyDialog
      if (!d.srcGroupId) { d.source = null; d.filteredItems = []; d.selected = []; return }
      d.loading = true
      try {
        const res = await listLoggerMx(d.srcGroupId)
        const rows = (res && res.code === 200 ? (res.data || []) : []).map(n => this.normalizeMxNode(n))
        const g = this.mxGroups.find(x => x.id === d.srcGroupId)
        d.source = { id: d.srcGroupId, name: g ? g.name : '', items: rows }
        // 当前组映射：主名 → 子名集合
        const curTops = {}
        this.mxTree.forEach(t => {
          const name = (t.logger || '').trim()
          if (!name) return
          curTops[name] = new Set((t.children || []).map(c => (c.logger || '').trim()).filter(Boolean))
        })
        const list = []
        rows.forEach(t => {
          const tname = (t.logger || '').trim()
          const parentInCurrent = curTops[tname] !== undefined
          const curKids = curTops[tname] || new Set()
          list.push({
            key: t.key, __pid: null, isTop: true,
            logger: t.logger, level: t.level, active: t.active, remark: t.remark,
            children: t.children || [],
            inCurrent: parentInCurrent
          })
          ;(t.children || []).forEach(c => {
            const cname = (c.logger || '').trim()
            list.push({
              key: c.key, __pid: t.key, isTop: false,
              logger: c.logger, level: c.level, active: c.active, remark: c.remark,
              inCurrent: parentInCurrent && curKids.has(cname)
            })
          })
        })
        d.filteredItems = list
        d.selected = []
        const table = this.$refs.mxCopyTable
        if (table) table.clearSelection()
      } catch (err) {
        this.$message.error('加载来源组失败: ' + (err.message || String(err)))
      } finally {
        d.loading = false
      }
    },
    // 勾选规则：主记录已存在 → 主行不可选（只能选子级）；子级 → 主已存在且子不存在时可单独选
    mxCopySelectable(row) {
      if (row.isTop) return !row.inCurrent
      const parent = this.mxCopyDialog.filteredItems.find(i => i.isTop && i.key === row.__pid)
      return !!(parent && parent.inCurrent && !row.inCurrent)
    },
    // 主/子联动：勾选主记录自动带上其全部子级；取消主记录也去掉其子级
    onMxCopySelectionChange(sel) {
      const d = this.mxCopyDialog
      d.selected = sel.slice()
      const table = this.$refs.mxCopyTable
      if (!table || this._mxCopySyncing) return
      this._mxCopySyncing = true
      try {
        const items = d.filteredItems
        const selKeys = new Set(sel.map(r => r.key))
        let changed = false
        items.forEach(row => {
          if (!row.isTop || row.inCurrent) return
          const kids = items.filter(k => !k.isTop && k.__pid === row.key)
          const want = selKeys.has(row.key)
          kids.forEach(k => {
            const has = selKeys.has(k.key)
            if (want && !has) { table.toggleRowSelection(k, true); changed = true }
            else if (!want && has) { table.toggleRowSelection(k, false); changed = true }
          })
        })
        // toggleRowSelection 触发的 selection-change 已被同步标记屏蔽，此处按表格最新状态收集
        if (changed) {
          const st = table.store && table.store.states
          d.selected = (st && st.selection ? st.selection.slice() : d.selected).slice()
        }
      } finally {
        this._mxCopySyncing = false
      }
    },
    submitMxCopyDialog() {
      const d = this.mxCopyDialog
      const sel = d.selected
      if (sel.length === 0) { this.$message.warning('请先勾选要添加的节点'); return }
      const uid = () => 'mx-copy-' + Math.random().toString(36).slice(2, 8)
      let addedTop = 0, addedChild = 0
      const newTopKeys = []
      sel.forEach(row => {
        if (row.isTop) {
          // 主记录：当前组不存在该主记录 → 整条新增（带上勾选的子级）
          if (row.inCurrent) return
          const node = {
            key: uid(), id: null, logger: row.logger, level: row.level,
            active: row.active, remark: row.remark, children: []
          }
          sel.filter(k => !k.isTop && k.__pid === row.key).forEach(k => {
            node.children.push({ key: uid(), __pid: node.key, id: null, logger: k.logger, level: k.level, active: k.active, remark: k.remark })
            addedChild++
          })
          this.mxTree.push(node)
          newTopKeys.push(node.key)
          addedTop++
        } else {
          // 子级：主记录已在该组 → 只往该主记录下新增子节点（按名称去重）
          const srcParent = d.filteredItems.find(i => i.isTop && i.key === row.__pid)
          if (!srcParent || !srcParent.inCurrent || row.inCurrent) return
          const parentNode = this.mxTree.find(t => (t.logger || '').trim() === (srcParent.logger || '').trim())
          if (!parentNode) return
          if (!parentNode.children) parentNode.children = []
          if (parentNode.children.some(c => (c.logger || '').trim() === (row.logger || '').trim())) return
          parentNode.children.push({ key: uid(), __pid: parentNode.key, id: null, logger: row.logger, level: row.level, active: row.active, remark: row.remark })
          addedChild++
        }
      })
      if (addedTop === 0 && addedChild === 0) {
        this.$message.info('勾选的内容在当前组均已存在，未新增任何记录')
        return
      }
      this.mxDirty = true
      d.visible = false
      this.$message.success('已添加 ' + addedTop + ' 个主记录、' + addedChild + ' 个子级，请点“保存”')
      this.$nextTick(() => {
        newTopKeys.forEach(k => {
          const t = this.mxTree.find(x => x.key === k)
          if (t) this.toggleMxExpandQuiet(t)
        })
      })
    },

    // ---- JSON 导入 ----
    openMxImportDialog() {
      if (!this.mxGroupId) { this.$message.warning('请先选择组'); return }
      this.mxImportDialog = { visible: true, text: '', error: '', loading: false }
    },
    submitMxImportDialog() {
      const d = this.mxImportDialog
      const text = (d.text || '').trim()
      if (!text) { d.error = '请粘贴 JSON 内容'; return }
      let parsed
      try {
        parsed = JSON.parse(text)
      } catch (err) {
        d.error = 'JSON 解析失败: ' + err.message
        return
      }
      if (!Array.isArray(parsed)) { d.error = 'JSON 必须是数组'; return }
      const uid = () => 'mx-imp-' + Math.random().toString(36).slice(2, 8)
      // SKS_LOGGER_MANAGE 请求体项 → 前端节点（logkey 忽略，保存/推送时自动生成）
      const norm = (p, pid) => {
        const upper = String(p.loglevel || '').toUpperCase()
        const node = {
          key: uid(), id: null,
          logger: (p.logger || '').trim(),
          level: LEVEL_OPTIONS.includes(upper) ? upper : 'ERROR',
          active: p.active !== false && String(p.active) !== '0',
          remark: p['sks:remark'] || '',
          children: []
        }
        if (pid) node.__pid = pid
        if (Array.isArray(p.children)) node.children = p.children.map(c => norm(c, node.key))
        return node
      }
      const incoming = parsed.map(p => norm(p, null)).filter(n => n.logger)
      if (incoming.length === 0) { d.error = '没有有效的日志器条目（logger 不能为空）'; return }
      let addedTop = 0, addedChild = 0
      const newTopKeys = []
      incoming.forEach(n => {
        const dup = this.mxTree.find(t => (t.logger || '').trim() === n.logger)
        if (dup) {
          // 主已存在：子级按名称去重合并
          if (!dup.children) dup.children = []
          const exist = new Set(dup.children.map(c => (c.logger || '').trim()).filter(Boolean))
          n.children.forEach(c => {
            if (c.logger && !exist.has(c.logger)) {
              dup.children.push(Object.assign({}, c, { __pid: dup.key }))
              exist.add(c.logger)
              addedChild++
            }
          })
        } else {
          this.mxTree.push(n)
          newTopKeys.push(n.key)
          addedTop++
          addedChild += n.children.length
        }
      })
      this.mxDirty = true
      d.visible = false
      this.$message.success('已导入 ' + addedTop + ' 个主记录（子级 ' + addedChild + ' 条），请点“保存”')
      this.$nextTick(() => {
        newTopKeys.forEach(k => {
          const t = this.mxTree.find(x => x.key === k)
          if (t) this.toggleMxExpandQuiet(t)
        })
      })
    },

    // ============ 查询 tab ============
    queryAllLoggers() {
      this.queryLoading = true
      queryLoggerLevel([]).then(res => {
        const data = res.data || res
        if (data && data.success) {
          this.queryLoggers = (data.result || []).slice().sort((a, b) => (a.loggerName || '').localeCompare(b.loggerName || ''))
          this.$message.success('查询成功，共 ' + this.queryLoggers.length + ' 条')
        } else {
          this.$message.error((data && data.message) || '查询失败')
        }
      }).catch(err => {
        this.$message.error('查询失败: ' + (err.message || String(err)))
      }).finally(() => {
        this.queryLoading = false
      })
    },
    onQuerySelectionChange(rows) { this.querySelection = rows },
    reloadSingleLogger(row) {
      this.queryLoading = true
      queryLoggerLevel([{ loggerName: row.loggerName }]).then(res => {
        const data = res.data || res
        if (data && data.success && data.result && data.result.length) {
          const found = data.result[0]
          const idx = this.queryLoggers.findIndex(l => l.loggerName === found.loggerName)
          if (idx !== -1) this.$set(this.queryLoggers, idx, found)
          this.$message.success('已刷新: ' + found.loggerName + ' = ' + found.level)
        } else {
          this.$message.error((data && data.message) || '刷新失败')
        }
      }).catch(err => {
        this.$message.error('刷新失败: ' + (err.message || String(err)))
      }).finally(() => {
        this.queryLoading = false
      })
    },
    changeLoggerLevel(row, newLevel) {
      if (newLevel === row.level) return
      this.queryLoading = true
      updateLoggerLevel([{ loggerName: row.loggerName, level: newLevel }]).then(res => {
        const data = res.data || res
        if (data && data.success) {
          this.$message.success('已设置 ' + row.loggerName + ' = ' + newLevel)
          return this.reloadSingleLogger(row)
        }
        this.$message.error((data && data.message) || '更新失败')
        this.queryLoading = false
      }).catch(err => {
        this.$message.error('更新失败: ' + (err.message || String(err)))
        this.queryLoading = false
      })
    },
    reloadRowLevel(row) {
      if (!row.loggerName) { this.$message.warning('请先填写日志器名称'); return }
      queryLoggerLevel([{ loggerName: row.loggerName }]).then(res => {
        const data = res.data || res
        if (data && data.success && data.result && data.result.length) {
          const lv = data.result[0].level
          if (this.isSettableLevel(lv)) {
            row.level = lv
            this.$message.success('当前级别: ' + lv)
          } else {
            this.$message.info('当前级别为 ' + lv + '（继承/未设置），未修改配置')
          }
        } else {
          this.$message.error((data && data.message) || '读取失败')
        }
      }).catch(err => {
        this.$message.error('读取失败: ' + (err.message || String(err)))
      })
    },

    // ============ 配置 tab - 默认分组 ============
    loadDefaultItems() {
      this.defaultLoading = true
      listLoggerConfig().then(res => {
        if (res && res.code === 200) {
          this.defaultItems = this.normalizeItems(res.data)
        } else {
          this.$message.error((res && res.message) || '加载默认配置失败')
        }
      }).catch(err => {
        this.$message.error('加载默认配置失败: ' + (err.message || String(err)))
      }).finally(() => {
        this.defaultLoading = false
      })
    },
    normalizeItems(list) {
      return (list || []).map(r => ({
        id: r.id || null,
        loggerName: r.loggerName || '',
        level: r.level || 'INFO',
        ignored: !!r.ignored,
        description: r.description || ''
      }))
    },

    // ============ 配置 tab - 用户分组 ============
    loadUserGroups() {
      listLoggerGroups().then(res => {
        if (res && res.code === 200) {
          this.userGroups = (res.data || []).map(g => ({
            id: g.id,
            name: g.name,
            description: g.description || '',
            itemCount: g.itemCount || 0,
            items: [],
            loading: false,
            loaded: false
          }))
        } else {
          this.$message.error((res && res.message) || '加载分组失败')
        }
      }).catch(err => {
        this.$message.error('加载分组失败: ' + (err.message || String(err)))
      })
    },
    onGroupTabClick(tab) {
      if (tab.name !== 'default') {
        const g = this.userGroups.find(x => String(x.id) === tab.name)
        if (g && !g.loaded) this.loadGroupItems(g)
      }
    },
    loadGroupItems(g) {
      g.loading = true
      return listLoggerGroupItems(g.id).then(res => {
        if (res && res.code === 200) {
          g.items = this.normalizeItems(res.data)
          g.itemCount = g.items.length
          g.loaded = true
        } else {
          this.$message.error((res && res.message) || '加载分组条目失败')
        }
      }).catch(err => {
        this.$message.error('加载分组条目失败: ' + (err.message || String(err)))
      }).finally(() => {
        g.loading = false
      })
    },

    // ============ 行操作（自动保存） ============
    addRow() {
      this.currentItems.push({ id: null, loggerName: '', level: 'INFO', ignored: false, description: '' })
    },
    async removeRow(index) {
      const items = this.currentItems
      const row = items[index]
      if (!row) return
      // 默认配置：删除前检查是否被其他用户分组引用
      if (this.isDefaultGroup && row.id != null && await this.defaultItemReferenced(row.loggerName)) {
        const name = row.loggerName || ''
        this.$confirm('默认配置项 “' + name + '” 被其他分组引用，删除后引用它的分组将失效。确定删除吗？', '提示', {
          confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning'
        }).then(() => {
          items.splice(index, 1)
          this.autoSave()
        }).catch(() => {})
        return
      }
      items.splice(index, 1)
      // 已存在的行删除后立即持久化（全量覆盖保存）
      if (row.id != null) this.autoSave()
    },
    onItemEdit() {
      // 当前组没有任何已命名日志器时（如刚添加的空行），无需触发保存
      if (this.buildSaveList(this.isDefaultGroup, this.activeGroup).filter(i => i.loggerName).length === 0) return
      this.autoSave()
    },
    reloadCurrentGroup() {
      if (this.isDefaultGroup) {
        this.loadDefaultItems()
      } else if (this.activeGroup) {
        this.loadGroupItems(this.activeGroup)
      }
    },

    // ============ 查询页多选设置级别（直接调 Maximo 更新接口） ============
    openQueryBatchLevelDialog() {
      if (!this.querySelection.length) { this.$message.warning('请先选择日志器'); return }
      this.queryBatchLevelDialog = { visible: true, level: 'INFO', loading: false }
    },
    submitQueryBatchLevelDialog() {
      const d = this.queryBatchLevelDialog
      if (!this.querySelection.length) { this.$message.warning('请先选择日志器'); return }
      const level = (d.level || '').toUpperCase()
      if (!this.isSettableLevel(level)) { this.$message.warning('请选择有效的日志级别'); return }
      // 过滤出可设置级别（继承/未设置的不传，服务端会跳过）
      const loggers = this.querySelection
        .filter(r => this.isSettableLevel(r.level) && r.loggerName)
        .map(r => ({ loggerName: r.loggerName, level: level }))
      if (!loggers.length) { this.$message.warning('所选日志器中无可设置级别的条目（继承/未设置的已忽略）'); return }
      d.loading = true
      updateLoggerLevel(loggers).then(res => {
        const data = res.data || res
        if (data && data.success) {
          this.pushResult.rows = data.result || []
          this.pushResult.visible = true
          this.$message.success(data.message || ('已更新 ' + loggers.length + ' 条日志器级别为 ' + level))
          d.visible = false
          // 刷新查询列表以反映最新级别
          this.queryAllLoggers()
        } else {
          this.$message.error((data && data.message) || '更新失败')
        }
      }).catch(err => {
        this.$message.error('更新失败: ' + (err.message || String(err)))
      }).finally(() => {
        d.loading = false
      })
    },

    // ============ 批量更改级别 ============
    onDefaultSelectionChange(rows) { this.tableSelection = rows },
    onUserSelectionChange(rows) { this.tableSelection = rows },
    openBatchLevelDialog() {
      this.batchLevelDialog = { visible: true, items: this.tableSelection.slice(), level: 'INFO', loading: false }
    },
    submitBatchLevelDialog() {
      const items = this.batchLevelDialog.items
      const level = this.batchLevelDialog.level
      if (!items.length || !level) { this.$message.warning('请选择要更改的条目和级别'); return }
      items.forEach(row => { row.level = level })
      this.batchLevelDialog.visible = false
      this.$message.success('已为 ' + items.length + ' 条日志器设置级别 ' + level)
      this.autoSave()
    },

    // ============ 保存 ============
    buildSaveList(targetIsDefault, targetGroup) {
      const srcItems = targetIsDefault ? this.defaultItems : (targetGroup ? targetGroup.items : [])
      const safeItems = srcItems || []
      const items = safeItems.map((c, i) => ({
        loggerName: (c.loggerName || '').trim(),
        level: (c.level || 'INFO').toUpperCase(),
        ignored: !!c.ignored,
        // 用户分组：描述引用默认配置（默认配置修改后此处自动取最新）
        description: targetIsDefault
          ? (c.description || '')
          : (this.defaultItemDesc(c.loggerName) || c.description || ''),
        sortOrder: i
      }))
      const named = items.filter(l => l.loggerName)
      const names = named.map(l => l.loggerName)
      const dup = names.find((n, i) => names.indexOf(n) !== i)
      if (dup) {
        this.$message.error('日志器名称重复: ' + dup)
        return null
      }
      return items
    },
    // ============ 自动保存（防抖，改动即存库） ============
    autoSave() {
      // 已在防抖等待中：跳过（最新改动会随本次保存提交）
      if (this.saveTimer) return
      // 上一请求仍在途：标记待保存，完成后补一次
      if (this.saveLoading) {
        this.pendingSave = true
        return
      }
      // 捕获本次保存目标（防止防抖窗口内切换分组导致保存到错误的组）
      const targetIsDefault = this.isDefaultGroup
      const targetGroup = targetIsDefault ? null : this.activeGroup
      this.saveTimer = setTimeout(() => {
        this.saveTimer = null
        this.saveLoading = true
        this.persistCurrentGroup(targetIsDefault, targetGroup).finally(() => {
          this.saveLoading = false
          if (this.pendingSave) {
            this.pendingSave = false
            this.autoSave()
          }
        })
      }, 400)
    },
    persistCurrentGroup(targetIsDefault, targetGroup) {
      const items = this.buildSaveList(targetIsDefault, targetGroup)
      if (items === null) {
        this.$message.error('自动保存失败：日志器名称重复，请修正')
        return Promise.resolve()
      }
      if (targetIsDefault) {
        return saveLoggerConfig(items).then(res => {
          if (res && res.code === 200) {
            this.mergeSavedIds(this.defaultItems, res.data)
          } else {
            this.$message.error((res && res.message) || '自动保存失败')
          }
        }).catch(err => {
          this.$message.error('自动保存失败: ' + (err.message || String(err)))
        })
      }
      if (!targetGroup) return Promise.resolve()
      return saveLoggerGroupItems(targetGroup.id, items).then(res => {
        if (res && res.code === 200) {
          this.mergeSavedIds(targetGroup.items, res.data)
          targetGroup.itemCount = (res.data || []).length
        } else {
          this.$message.error((res && res.message) || '自动保存失败')
        }
      }).catch(err => {
        this.$message.error('自动保存失败: ' + (err.message || String(err)))
      })
    },
    // 将后端返回行的 id 回填本地行（新插入的行第一次保存后才能拿到 id）
    mergeSavedIds(localItems, savedList) {
      const saved = savedList || []
      localItems.forEach(local => {
        if (local.id != null) return
        const name = (local.loggerName || '').trim()
        if (!name) return
        const hit = saved.find(s => (s.loggerName || '').trim() === name)
        if (hit) local.id = hit.id
      })
    },

    // ============ 更新到 Maximo（仅当前用户分组） ============
    updateToMaximo() {
      if (this.isDefaultGroup) return
      const g = this.activeGroup
      const items = (g ? g.items : []).filter(i => i.loggerName && !i.ignored).map(i => ({ loggerName: i.loggerName, level: i.level }))
      if (items.length === 0) {
        this.$message.warning('当前分组没有可下发的未忽略条目，请先添加（编辑即自动保存）')
        return
      }
      this.pushLoading = true
      updateLoggerLevel(items).then(res => {
        const data = res.data || res
        if (data && data.success) {
          this.pushResult.rows = data.result || []
          this.pushResult.visible = true
          this.$message.success(data.message || '更新完成')
        } else {
          this.$message.error((data && data.message) || '更新到 Maximo 失败')
        }
      }).catch(err => {
        this.$message.error('更新到 Maximo 失败: ' + (err.message || String(err)))
      }).finally(() => {
        this.pushLoading = false
      })
    },

    // ============ 导入 JSON（默认分组，增量） ============
    openImportConfigDialog() {
      this.importConfigDialog = { visible: true, text: '', error: '', loading: false }
    },
    submitImportConfigDialog() {
      const d = this.importConfigDialog
      const text = (d.text || '').trim()
      if (!text) { d.error = '请粘贴 JSON 内容'; return }
      let parsed
      try {
        parsed = JSON.parse(text)
      } catch (err) {
        d.error = 'JSON 解析失败: ' + err.message
        return
      }
      // 支持 {loggers:[...]} 或直接数组
      const list = Array.isArray(parsed) ? parsed : (parsed && Array.isArray(parsed.loggers) ? parsed.loggers : null)
      if (!list) { d.error = 'JSON 格式不正确，应为 {"loggers":[...]} 或数组'; return }
      const loggers = list.map(p => ({
        loggerName: (p.loggerName || '').trim(),
        level: (p.level || 'INFO').toUpperCase(),
        ignored: !!(p.ignore !== undefined ? p.ignore : p.ignored),
        description: p.description || ''
      })).filter(l => l.loggerName)
      if (loggers.length === 0) {
        d.error = '没有有效的日志器（loggerName 不能为空）'
        return
      }
      d.loading = true
      d.error = ''
      const groupId = this.isDefaultGroup ? null : (this.activeGroup ? this.activeGroup.id : null)
      importLoggerConfig(loggers, groupId).then(res => {
        if (res && res.code === 200) {
          const r = res.data || {}
          const parts = []
          if (r.added || r.skipped) parts.push('默认配置 新增 ' + (r.added || 0) + ' 条，跳过 ' + (r.skipped || 0) + ' 条')
          if (!this.isDefaultGroup) {
            parts.push('分组新增 ' + (r.groupAdded || 0) + ' 条，更新 ' + (r.groupUpdated || 0) + ' 条')
          }
          this.$message.success('导入完成：' + (parts.join('；') || '无变化'))
          d.visible = false
          this.loadDefaultItems()
          if (!this.isDefaultGroup && this.activeGroup) {
            this.loadGroupItems(this.activeGroup)
          }
        } else {
          this.$message.error((res && res.message) || '导入失败')
        }
      }).catch(err => {
        this.$message.error('导入失败: ' + (err.message || String(err)))
      }).finally(() => { d.loading = false })
    },

    // ============ 分组新建/重命名 ============
    openCreateGroupDialog() {
      this.groupEditDialog = { visible: true, mode: 'create', id: null, name: '', description: '', loading: false }
    },
    openGroupEditDialog(g) {
      this.groupEditDialog = { visible: true, mode: 'update', id: g.id, name: g.name, description: g.description || '', loading: false }
    },
    submitGroupEditDialog() {
      const d = this.groupEditDialog
      if (!d.name.trim()) { this.$message.warning('分组名称不能为空'); return }
      d.loading = true
      if (d.mode === 'create') {
        createLoggerGroup({ name: d.name.trim(), description: d.description.trim() }).then(res => {
          if (res && res.code === 200) {
            const g = res.data
            this.userGroups.push({ id: g.id, name: g.name, description: g.description || '', itemCount: 0, items: [], loading: false, loaded: false })
            this.activeGroupId = String(g.id)
            this.$message.success('已创建分组: ' + g.name)
            d.visible = false
          } else {
            this.$message.error((res && res.message) || '创建失败')
          }
        }).catch(err => {
          this.$message.error('创建失败: ' + (err.message || String(err)))
        }).finally(() => { d.loading = false })
      } else {
        updateLoggerGroup(d.id, { name: d.name.trim(), description: d.description.trim() }).then(res => {
          if (res && res.code === 200) {
            const g = res.data
            const idx = this.userGroups.findIndex(x => x.id === d.id)
            if (idx !== -1) {
              this.userGroups[idx].name = g.name
              this.userGroups[idx].description = g.description || ''
            }
            this.$message.success('已更新分组')
            d.visible = false
          } else {
            this.$message.error((res && res.message) || '更新失败')
          }
        }).catch(err => {
          this.$message.error('更新失败: ' + (err.message || String(err)))
        }).finally(() => { d.loading = false })
      }
    },
    onGroupRemove(targetName) {
      const id = Number(targetName)
      const g = this.userGroups.find(x => x.id === id)
      if (!g) return
      this.$confirm('删除分组“' + g.name + '”将一并删除其所有条目，是否继续？', '删除分组', {
        type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消'
      }).then(() => {
        deleteLoggerGroup(id).then(res => {
          if (res && res.code === 200) {
            const idx = this.userGroups.findIndex(x => x.id === id)
            if (idx !== -1) this.userGroups.splice(idx, 1)
            this.activeGroupId = 'default'
            this.$message.success('已删除分组')
          } else {
            this.$message.error((res && res.message) || '删除失败')
          }
        }).catch(err => {
          this.$message.error('删除失败: ' + (err.message || String(err)))
        })
      }).catch(() => {})
    },

    // ============ 跨组添加 ============
    currentGroupNameSet() {
      const keys = new Set()
      if (this.isDefaultGroup) {
        this.defaultItems.forEach(i => { if (i.loggerName) keys.add(i.loggerName.trim()) })
      } else {
        const g = this.activeGroup
        if (g) (g.items || []).forEach(i => { if (i.loggerName) keys.add(i.loggerName.trim()) })
      }
      return keys
    },
    openCrossGroupDialog() {
      const sources = []
      if (!this.isDefaultGroup) {
        // 用户组：来源包含默认配置 + 其它用户分组
        sources.push({ key: 'default', label: '默认配置', items: this.defaultItems.map(i => ({ loggerName: i.loggerName, level: i.level })).filter(i => i.loggerName) })
        this.userGroups.filter(g => g.id !== (this.activeGroup ? this.activeGroup.id : null))
          .forEach(g => sources.push({ key: String(g.id), label: g.name, items: (g.items || []).map(i => ({ loggerName: i.loggerName, level: i.level })).filter(i => i.loggerName) }))
      } else {
        // 默认组：来源为所有用户分组
        this.userGroups.forEach(g => sources.push({ key: String(g.id), label: g.name, items: (g.items || []).map(i => ({ loggerName: i.loggerName, level: i.level })).filter(i => i.loggerName) }))
      }
      if (sources.length === 0) {
        this.$message.info('暂无可用的跨组来源')
        return
      }
      const needsLoad = sources.filter(s => s.key !== 'default' && this.userGroups.find(g => String(g.id) === s.key && !g.loaded))
      const d = this.crossGroupDialog
      d.sources = sources
      d.sourceKey = sources[0].key
      d.filter = ''
      d.allItems = []
      d.filteredItems = []
      d.selected = []
      d.visible = true
      this.refreshCrossItems()
      // 若来源分组未加载过，先异步加载其条目，完成后刷新弹窗表格
      if (needsLoad.length > 0) {
        d.tableLoading = true
        Promise.all(needsLoad.map(s => this.loadGroupItems(this.userGroups.find(g => String(g.id) === s.key))))
          .finally(() => {
            d.tableLoading = false
            sources.forEach(s => {
              if (s.items.length === 0) {
                const g = this.userGroups.find(x => String(x.id) === s.key)
                if (g && g.items && g.items.length) {
                  s.items = g.items.map(i => ({ loggerName: i.loggerName, level: i.level })).filter(i => i.loggerName)
                }
              }
            })
            this.refreshCrossItems()
          })
      }
    },
    // 每次切换来源/过滤时重建表格数据（标记 inCurrent 并禁用勾选）
    refreshCrossItems() {
      const d = this.crossGroupDialog
      const src = d.sources.find(s => s.key === d.sourceKey)
      if (!src) {
        d.allItems = []
        d.filteredItems = []
        d.selected = []
        return
      }
      // 来源可能只含名称（未加载时没有 items），尝试补齐
      if (src.items.length === 0) {
        const g = src.key !== 'default' ? this.userGroups.find(x => String(x.id) === src.key) : null
        if (g && g.items && g.items.length) {
          src.items = g.items.map(i => ({ loggerName: i.loggerName, level: i.level })).filter(i => i.loggerName)
        }
      }
      d.allItems = src.items
      const currentNames = this.currentGroupNameSet()
      // 默认配置级别映射：添加时级别取默认配置中的级别，缺省 INFO
      const defaultLevelByName = {}
      this.defaultItems.forEach(i => {
        if (i.loggerName) defaultLevelByName[i.loggerName.trim()] = i.level || 'INFO'
      })
      const kw = d.filter.trim().toLowerCase()
      d.filteredItems = d.allItems
        .filter(i => {
          if (!kw) return true
          // 名称或默认配置描述包含关键字即命中
          const desc = this.defaultItemDesc(i.loggerName) || ''
          return i.loggerName.toLowerCase().includes(kw) || desc.toLowerCase().includes(kw)
        })
        .map(i => ({
          ...i,
          addLevel: defaultLevelByName[i.loggerName.trim()] || 'INFO',
          inCurrent: currentNames.has(i.loggerName.trim())
        }))
    },
    onCrossSourceChange() { this.refreshCrossItems() },
    onCrossSelectionChange(rows) { this.crossGroupDialog.selected = rows },
    crossSelectable(row) { return !row.inCurrent },
    submitCrossGroupDialog() {
      const d = this.crossGroupDialog
      const names = d.selected.filter(r => !r.inCurrent).map(r => r.loggerName)
      if (names.length === 0) { this.$message.warning('请至少勾选一个可添加的日志器'); return }
      d.loading = true
      const finish = (msg, items) => {
        this.$message.success(msg)
        d.visible = false
        if (items) this.refreshCurrentAfterAdd(Array.isArray(items) ? items : d.filteredItems)
      }
      if (this.isDefaultGroup) {
        const loggers = names.map(n => ({ loggerName: n, level: 'INFO', ignored: false }))
        importLoggerConfig(loggers).then(res => {
          if (res && res.code === 200) {
            const r = res.data || {}
            finish('已加入 ' + (r.added || 0) + ' 条，跳过 ' + (r.skipped || 0) + ' 条已存在')
            this.loadDefaultItems()
          } else {
            this.$message.error((res && res.message) || '添加失败')
          }
        }).catch(err => {
          this.$message.error('添加失败: ' + (err.message || String(err)))
        }).finally(() => { d.loading = false })
      } else {
        const g = this.activeGroup
        addLoggerToGroup(g.id, names).then(res => {
          if (res && res.code === 200) {
            const r = res.data || {}
            finish('已加入 ' + (r.added || 0) + ' 条，跳过 ' + (r.skipped || 0) + ' 条已存在', r.items)
          } else {
            this.$message.error((res && res.message) || '添加失败')
          }
        }).catch(err => {
          this.$message.error('添加失败: ' + (err.message || String(err)))
        }).finally(() => { d.loading = false })
      }
    },
    // 添加完成后刷新当前组显示并同步数量
    refreshCurrentAfterAdd(items) {
      const list = Array.isArray(items) ? items : []
      if (list.length === 0) return
      if (this.isDefaultGroup) {
        this.defaultItems = this.mergeById(this.defaultItems, this.normalizeItems(list))
      } else {
        const g = this.activeGroup
        if (g) {
          g.items = this.mergeById(g.items, this.normalizeItems(list))
          g.itemCount = g.items.length
        }
      }
    },
    mergeById(localItems, incoming) {
      const out = localItems.slice()
      incoming.forEach(n => {
        const name = n.loggerName.trim()
        if (!out.some(o => o.loggerName.trim() === name)) out.push(n)
      })
      return out
    },

    // ============ 从查询加入分组 ============
    openAddToGroupDialog(loggerNames) {
      if (!loggerNames || loggerNames.length === 0) { this.$message.warning('请先选择日志器'); return }
      this.addToGroupDialog = { visible: true, targetGroupId: 'default', loggerNames: loggerNames, loading: false }
    },
    submitAddToGroupDialog() {
      const d = this.addToGroupDialog
      if (!d.targetGroupId) { this.$message.warning('请选择目标分组'); return }
      d.loading = true
      const finish = (msg) => { this.$message.success(msg); d.visible = false }
      if (d.targetGroupId === 'default') {
        const loggers = d.loggerNames.map(n => ({ loggerName: n, level: 'INFO', ignored: false }))
        importLoggerConfig(loggers).then(res => {
          if (res && res.code === 200) {
            const r = res.data || {}
            finish('已加入 ' + (r.added || 0) + ' 条，跳过 ' + (r.skipped || 0) + ' 条已存在')
            this.loadDefaultItems()
          } else {
            this.$message.error((res && res.message) || '加入失败')
          }
        }).catch(err => {
          this.$message.error('加入失败: ' + (err.message || String(err)))
        }).finally(() => { d.loading = false })
      } else {
        const gid = Number(d.targetGroupId)
        addLoggerToGroup(gid, d.loggerNames).then(res => {
          if (res && res.code === 200) {
            const r = res.data || {}
            finish('已加入 ' + (r.added || 0) + ' 条，跳过 ' + (r.skipped || 0) + ' 条已存在')
            const g = this.userGroups.find(x => x.id === gid)
            if (g && g.loaded) {
              g.items = this.normalizeItems(r.items)
              g.itemCount = g.items.length
            }
          } else {
            this.$message.error((res && res.message) || '加入失败')
          }
        }).catch(err => {
          this.$message.error('加入失败: ' + (err.message || String(err)))
        }).finally(() => { d.loading = false })
      }
    },

    // ============ 工具 ============
    // 查询默认配置 (defaultItems) 中指定日志器名称的描述（用户分组描述只读展示/跨组添加列表展示）
    defaultItemDesc(loggerName) {
      if (!loggerName) return ''
      const name = String(loggerName).trim()
      const hit = this.defaultItems.find(i => i.loggerName && i.loggerName.trim() === name)
      return hit ? (hit.description || '') : ''
    },
    // 检查某默认配置行是否被其他用户分组引用（按日志器名称）
    // 由于用户分组条目懒加载，先确保所有组加载完再判定（异步）
    async ensureAllGroupItemsLoaded() {
      const waiters = this.userGroups
        .filter(g => !g.loaded && !g.loading)
        .map(g => this.loadGroupItems(g))
      if (waiters.length) await Promise.all(waiters)
    },
    async defaultItemReferenced(loggerName) {
      await this.ensureAllGroupItemsLoaded()
      const name = String(loggerName || '').trim()
      if (!name) return false
      return this.userGroups.some(g =>
        (g.items || []).some(i => i.loggerName && i.loggerName.trim() === name)
      )
    },
    isSettableLevel(level) {
      return LEVEL_OPTIONS.includes((level || '').toUpperCase())
    },
    levelTagType(level) {
      switch ((level || '').toUpperCase()) {
        case 'FATAL':
        case 'ERROR': return 'danger'
        case 'WARN': return 'warning'
        case 'INFO': return 'primary'
        case 'DEBUG': return 'success'
        default: return 'info'
      }
    },
    statusTagType(status) {
      switch ((status || '').toUpperCase()) {
        case 'SUCCESS': return 'success'
        case 'FAILED': return 'danger'
        case 'IGNORED': return 'info'
        default: return 'info'
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.logger-level-page {
  padding: 16px;
}
.page-header-row {
  margin-bottom: 12px;
  h2 { margin: 0 0 6px 0; }
}
.page-summary {
  color: #606266;
  margin: 0;
  font-size: 13px;
}
.toolbar {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
  flex-wrap: wrap;
  gap: 8px 0;
}
.count-tip {
  margin-left: 12px;
  color: #909399;
  font-size: 12px;
}
.readonly-desc {
  color: #606266;
  font-size: 13px;
}
.mx-logkey {
  color: #409eff;
  font-size: 12px;
  font-family: monospace;
}
/* MX树表格：隐藏默认行内展开箭头（展开操作移至独立"展开"列） */
.mx-tree-table ::v-deep .el-table__expand-icon {
  display: none !important;
}
</style>
