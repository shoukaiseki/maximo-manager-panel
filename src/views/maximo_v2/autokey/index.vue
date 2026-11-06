<template>
  <section class="query-page">
    <el-card>
      <div class="page-header-row">
        <div>
          <h2>AUTOKEY 自动编号</h2>
          <p class="page-summary">支持自动编号名称 / 前缀模糊查询与级别过滤，点击行查看详情，可导出 / 导入自动编号定义。</p>
        </div>
        <div class="header-actions">
          <saved-query-panel ref="savedQuery" appname="AUTOKEY" :default-where="buildWhere()" @whereChange="handleWhereChange" @ready="handleSavedQueryReady" />
          <el-button type="primary" icon="el-icon-upload2" size="mini" @click="openImportDialog">导入</el-button>
          <el-button type="primary" icon="el-icon-download" size="mini" @click="openExportDialog">导出</el-button>
        </div>
      </div>
      <el-form ref="queryForm" :model="formData" :inline="true" label-width="80px" @submit.native.prevent>
        <el-form-item label="编号名称">
          <el-input v-model="formData.autokeyname" placeholder="模糊搜索自动编号名称" @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item label="前缀">
          <el-input v-model="formData.prefix" placeholder="模糊搜索前缀" @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item label="级别">
          <el-select v-model="formData.level" placeholder="全部级别" clearable style="width: 130px;">
            <el-option label="系统(SYSTEM)" value="SYSTEM" />
            <el-option label="组织(ORG)" value="ORG" />
            <el-option label="站点(SITE)" value="SITE" />
            <el-option label="套集(SET)" value="SET" />
          </el-select>
        </el-form-item>
        <el-form-item label="站点">
          <el-input v-model="formData.siteid" placeholder="SITEID 精确" style="width: 130px;" @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item label="组织">
          <el-input v-model="formData.orgid" placeholder="ORGID 精确" style="width: 130px;" @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item label="套集">
          <el-input v-model="formData.setid" placeholder="SETID 精确" style="width: 130px;" @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item>
          <el-button type="cyan" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" size="mini" @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="result-panel">
        <div class="table-toolbar">
          <el-tooltip :content="showAllColumns ? '隐藏多余列' : '显示所有列'" placement="top">
            <el-button size="mini" circle :type="showAllColumns ? 'success' : 'info'" icon="el-icon-menu" @click="showAllColumns = !showAllColumns" />
          </el-tooltip>
          <el-tooltip :content="showPropName ? '隐藏属性名' : '显示属性名'" placement="top">
            <el-button size="mini" circle :type="showPropName ? 'success' : 'info'" icon="el-icon-s-flag" @click="showPropName = !showPropName" />
          </el-tooltip>
        </div>
        <el-table
          v-loading="loading"
          :data="autokeys"
          stripe
          style="width: 100%"
          class="result-table"
          :highlight-current-row="true"
          @selection-change="handleSelectionChange"
          @row-click="handleRowClick"
        >
          <el-table-column type="selection" width="46" />
          <template v-for="col in visibleColumns">
            <el-table-column
              v-if="col.type === 'level'"
              :key="col.prop + '-' + showPropName"
              :prop="col.prop"
              :label="col.label"
              :width="col.width"
              :min-width="col.minWidth"
            >
              <template slot="header">
                <span>{{ col.label }}</span>
                <template v-if="showPropName">
                  <el-tag size="mini" class="prop-name-tag" @click.native="copyText(col.prop, '已复制属性名 ' + col.prop)">{{ col.prop }}</el-tag>
                  <el-tooltip content="显示更多配置" placement="top">
                    <el-button size="mini" circle icon="el-icon-search" class="prop-search-btn" @click.stop="showColInfo(col)" />
                  </el-tooltip>
                </template>
              </template>
              <template slot-scope="scope">
                <el-tag :type="levelTagType(scope.row[col.prop])" size="mini">{{ scope.row[col.prop] }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column
              v-else
              :key="col.prop + '-' + showPropName"
              :prop="col.prop"
              :label="col.label"
              :width="col.width"
              :min-width="col.minWidth"
              show-overflow-tooltip
              :formatter="cellFormatter"
            >
              <template slot="header">
                <span>{{ col.label }}</span>
                <template v-if="showPropName">
                  <el-tag size="mini" class="prop-name-tag" @click.native="copyText(col.prop, '已复制属性名 ' + col.prop)">{{ col.prop }}</el-tag>
                  <el-tooltip content="显示更多配置" placement="top">
                    <el-button size="mini" circle icon="el-icon-search" class="prop-search-btn" @click.stop="showColInfo(col)" />
                  </el-tooltip>
                </template>
              </template>
            </el-table-column>
          </template>
        </el-table>
        <pagination
          v-show="tablePatam.total > 0"
          :total="tablePatam.total"
          :page.sync="tablePatam.pageNum"
          :limit.sync="tablePatam.pageSize"
          @pagination="getList"
        />
      </div>
    </el-card>

    <!-- 自动编号详情弹窗 -->
    <el-dialog :title="'自动编号详情 - ' + (detailRow ? detailRow.AUTOKEYNAME : '')" :visible.sync="dialogVisible" width="900px" top="8vh" :close-on-click-modal="true" @closed="disposeDetailEditor">
      <div v-loading="detailLoading" element-loading-text="加载中...">
        <el-descriptions v-if="detailRow" :column="2" border size="small">
          <el-descriptions-item v-for="field in mainFields" :key="field.key" :label="field.label">
            <el-input :value="formatMainValue(field.key)" readonly size="small" />
          </el-descriptions-item>
        </el-descriptions>
        <el-tabs v-if="detailRow" v-model="activeDetailTab" type="border-card" class="json-tabs" @tab-click="onDetailTabChange">
          <el-tab-pane label="原始JSON" name="raw">
            <div class="json-toolbar">
              <el-button type="primary" size="mini" icon="el-icon-document-copy" @click="copyDetailJson">复制 JSON</el-button>
            </div>
            <div ref="detailMonacoRef" class="monaco-container" />
          </el-tab-pane>
          <el-tab-pane label="可导入JSON" name="importable">
            <div class="json-toolbar">
              <el-button type="primary" size="mini" icon="el-icon-document-copy" @click="copyDetailImportJson">复制 JSON</el-button>
            </div>
            <div ref="detailImportMonacoRef" class="monaco-container" />
          </el-tab-pane>
        </el-tabs>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">关 闭</el-button>
      </span>
    </el-dialog>

    <!-- 导出自动编号弹窗 -->
    <el-dialog title="导出自动编号(点击刷新之后获得导出的json)" :visible.sync="batchDialogVisible" width="1400px" top="5vh" :close-on-click-modal="true" @opened="onBatchDialogOpened" @closed="disposeBatchEditors">
      <div class="json-toolbar" style="margin-bottom: 12px; text-align: left;">
        <el-radio-group v-model="exportOptions.scope" size="mini">
          <el-radio-button label="selected">选中记录</el-radio-button>
          <el-radio-button label="current">当前搜索结果</el-radio-button>
        </el-radio-group>
        <el-button size="mini" icon="el-icon-refresh" :loading="batchLoading" @click="loadExportData">刷 新</el-button>
        <span style="margin-left: 10px;" class="el-tag el-tag--info">共 {{ batchTotal }} 条</span>
      </div>
      <el-alert type="info" :closable="false" show-icon title="「当前搜索结果」按当前过滤条件导出不分页；选中记录时导出所有同名自动编号的全部级别记录；未勾选记录将自动按「当前搜索结果」导出" style="margin-bottom: 12px;" />
      <el-tabs v-model="batchActiveTab" type="border-card" class="json-tabs" @tab-click="onBatchTabChange">
        <el-tab-pane label="完整导出" name="batch-full">
          <div class="json-toolbar">
            <el-button type="primary" size="mini" icon="el-icon-document-copy" @click="copyBatchJson">复制 JSON</el-button>
            <el-button type="primary" size="mini" icon="el-icon-download" @click="saveBatchJson">保存 JSON</el-button>
          </div>
          <div class="monaco-wrapper">
            <div ref="batchMonacoRef" class="monaco-container" />
          </div>
        </el-tab-pane>
        <el-tab-pane label="原始JSON" name="batch-raw" lazy>
          <div class="json-toolbar">
            <el-button type="primary" size="mini" icon="el-icon-document-copy" @click="copyBatchJson">复制 JSON</el-button>
            <el-button type="primary" size="mini" icon="el-icon-download" @click="saveBatchJson">保存 JSON</el-button>
          </div>
          <div class="monaco-wrapper">
            <div ref="batchRawMonacoRef" class="monaco-container" />
          </div>
        </el-tab-pane>
      </el-tabs>

      <span slot="footer" class="dialog-footer">
        <el-button @click="batchDialogVisible = false">关 闭</el-button>
      </span>
    </el-dialog>

    <!-- 导入自动编号弹窗 -->
    <el-dialog title="导入自动编号" :visible.sync="importDialog.visible" width="85%" top="4vh" custom-class="import-dialog" :close-on-click-modal="false" @opened="onImportDialogOpened" @closed="disposeImportEditor">
      <el-alert type="info" :closable="false" show-icon title="粘贴导出的自动编号 JSON 或选择 .json 文件（支持 {autokeys:[...]} / 数组 / 单个对象）；导入以编辑框内容为准；_seed 与 sks: 开头属性为注解会被忽略" />
      <el-upload
        ref="importUpload"
        style="margin-top:12px;"
        :file-list="importDialog.fileList"
        :auto-upload="false"
        :limit="1"
        accept=".json"
        :on-change="onImportFileChange"
        :on-remove="onImportFileRemove"
      >
        <el-button size="small" icon="el-icon-folder-opened">选择 JSON 文件</el-button>
      </el-upload>
      <el-form label-width="110px" style="margin-top:12px;">
        <el-form-item label="启用迁移模式">
          <el-checkbox v-model="importDialog.dmDeploySet">dmDeploySet（MboSet 调用 setDMDeploySet(true)）</el-checkbox>
        </el-form-item>
        <el-form-item label="分批导入">
          <el-checkbox v-model="importDialog.batchImport">启用分批导入</el-checkbox>
          <span style="margin-left:16px;color:#909399;font-size:12px;">每批提交条数</span>
          <el-input-number
            v-model="importDialog.batchSize"
            :min="1"
            :max="5000"
            :disabled="!importDialog.batchImport"
            size="small"
            style="margin-left:8px;width:130px;"
          />
        </el-form-item>
      </el-form>
      <div ref="importMonacoRef" class="monaco-container import-monaco" />
      <div v-if="importDialog.loading" class="import-result-bar">
        <span class="import-result-summary">{{ importDialog.progressText }}</span>
        <el-progress :percentage="importProgress" :stroke-width="14" style="flex:1; margin-left:12px;" />
      </div>
      <div v-else-if="importDialog.result" class="import-result-bar">
        <el-tag :type="importDialog.result.failed > 0 ? 'danger' : 'success'" size="small">{{ importDialog.result.failed > 0 ? '导入失败' : '导入成功' }}</el-tag>
        <span class="import-result-summary">{{ importSummaryText }}</span>
        <el-button type="text" size="mini" @click="openImportDetailDialog">查看详细返回信息</el-button>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="importDialog.visible = false">关 闭</el-button>
        <el-button type="primary" :loading="importDialog.loading" @click="handleImport">开始导入</el-button>
      </span>
    </el-dialog>

    <!-- 导入详细返回信息弹窗 -->
    <el-dialog title="导入详细返回信息" :visible.sync="importDetailDialog.visible" width="85%" top="4vh" custom-class="import-dialog" @opened="onImportDetailOpened" @closed="disposeImportDetailEditor">
      <div style="margin-bottom:8px;">
        <el-button size="mini" type="primary" plain @click="importDetailDialog.allExpanded = !importDetailDialog.allExpanded; applyImportDetailExpandAll()">
          {{ importDetailDialog.allExpanded ? '收缩所有' : '展开所有' }}
        </el-button>
      </div>
      <el-tabs v-model="importDetailDialog.activeTab" type="border-card" @tab-click="onImportDetailOpened">
        <el-tab-pane :label="'列表(' + importDetailDialog.rows.length + ')'" name="list">
          <el-table
            ref="importDetailTable"
            :data="importDetailDialog.rows"
            border
            stripe
            size="mini"
            max-height="520"
            row-key="rowKey"
            :default-expand-all="importDetailDialog.allExpanded"
            :tree-props="{ children: 'children' }"
          >
            <el-table-column prop="label" label="对象 / 记录" min-width="300" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="90">
              <template slot-scope="scope">
                <el-tag v-if="scope.row.status" :type="scope.row.status === 'success' ? 'success' : 'danger'" size="mini">{{ scope.row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="operation" label="操作" width="90" />
            <el-table-column prop="message" label="消息" min-width="260" show-overflow-tooltip />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="原始JSON" name="raw" lazy>
          <div class="monaco-wrapper">
            <div ref="importRawMonacoRef" class="monaco-container" />
          </div>
        </el-tab-pane>
      </el-tabs>
      <span slot="footer" class="dialog-footer">
        <el-button @click="importDetailDialog.visible = false">关 闭</el-button>
      </span>
    </el-dialog>

    <!-- 列配置信息弹窗（放大镜） -->
    <el-dialog title="列配置信息" :visible.sync="colInfoDialog.visible" width="700px" top="10vh" :close-on-click-modal="true" append-to-body>
      <el-table :data="colInfoRows" border stripe size="small">
        <el-table-column prop="key" label="配置项" width="180" />
        <el-table-column prop="value" label="值" min-width="300" show-overflow-tooltip />
      </el-table>
      <span slot="footer" class="dialog-footer">
        <el-button @click="colInfoDialog.visible = false">关 闭</el-button>
      </span>
    </el-dialog>
  </section>
</template>

<script>
import { queryAutokeyList, getAutokeyDetail, exportAutokey, importAutokey } from '@/api/autokey'
import SavedQueryPanel from '@/views/components/SavedQueryPanel.vue'
import * as monaco from 'monaco-editor'

const DETAIL_COLUMNS = ['AUTOKEYID', 'AUTOKEYNAME', 'SITEID', 'ORGID', 'SETID', 'PREFIX', 'SEED', 'LANGCODE']

const MAIN_FIELD_LABELS = {
  AUTOKEYID: '自动编号ID',
  AUTOKEYNAME: '自动编号名称',
  SITEID: '站点',
  ORGID: '组织',
  SETID: '套集',
  PREFIX: '前缀',
  SEED: '种子值',
  LANGCODE: '语言',
  'sks:level': '级别',
  'sks:ownerdescription': '归属描述'
}

export default {
  name: 'AutokeyIndex',
  components: {
    SavedQueryPanel
  },
  data() {
    return {
      loading: false,
      // 保存查询面板初始化完成标志：进页面不先查主表，待 ready 后再查
      sqReady: false,
      showAllColumns: false,
      showPropName: false,
      // advanced: true 的列仅在「显示所有列」时展示
      tableColumns: [
        { prop: 'AUTOKEYID', label: '自动编号ID', width: 110, advanced: true },
        { prop: 'AUTOKEYNAME', label: '自动编号名称', minWidth: 180 },
        { prop: 'sks:level', label: '级别', width: 90, type: 'level' },
        { prop: 'SITEID', label: '站点', minWidth: 110, advanced: true },
        { prop: 'ORGID', label: '组织', minWidth: 110, advanced: true },
        { prop: 'SETID', label: '套集', minWidth: 120, advanced: true },
        { prop: 'PREFIX', label: '前缀', minWidth: 110 },
        { prop: 'SEED', label: '种子值', width: 100 },
        { prop: 'LANGCODE', label: '语言', width: 70, advanced: true },
        { prop: 'sks:ownerdescription', label: '归属描述', minWidth: 200 }
      ],
      formData: {
        autokeyname: '',
        prefix: '',
        level: '',
        siteid: '',
        orgid: '',
        setid: ''
      },
      autokeys: [],
      selectedRows: [],
      tablePatam: {
        pageNum: 1,
        pageSize: 10,
        total: 0
      },
      dialogVisible: false,
      detailLoading: false,
      // 「显示属性名」后列头放大镜的列配置信息弹窗（参考 maxobject/attr SksTable 设计）
      colInfoDialog: {
        visible: false,
        col: null
      },
      currentRow: null,
      detailRow: null,
      activeDetailTab: 'raw',
      detailJson: '',
      detailMonaco: null,
      detailImportMonaco: null,
      mainFields: DETAIL_COLUMNS.concat(['sks:level', 'sks:ownerdescription']).map(key => ({ key, label: MAIN_FIELD_LABELS[key] || key })),
      // 导出弹窗
      batchDialogVisible: false,
      batchActiveTab: 'batch-full',
      batchLoading: false,
      batchJson: '',
      batchTotal: 0,
      exportOptions: {
        scope: 'selected'
      },
      batchMonaco: null,
      batchRawMonaco: null,
      importMonaco: null,
      importRawMonaco: null,
      // 导入弹窗
      importDialog: {
        visible: false,
        loading: false,
        dmDeploySet: false,
        batchImport: false,
        batchSize: 200,
        fileList: [],
        fileJson: '',
        text: '',
        progressText: '',
        result: null,
        rawResult: ''
      },
      importDetailDialog: {
        visible: false,
        activeTab: 'list',
        rows: [],
        allExpanded: false
      }
    }
  },
  computed: {
    visibleColumns() {
      return this.showAllColumns ? this.tableColumns : this.tableColumns.filter(c => !c.advanced)
    },
    // 放大镜弹窗内容: 列配置 + 当前页首个非空示例值
    colInfoRows() {
      const col = this.colInfoDialog.col
      if (!col) {
        return []
      }
      const rows = [
        { key: '属性名 (prop)', value: col.prop },
        { key: '显示名 (label)', value: col.label },
        { key: '列宽 (width)', value: col.width || '-' },
        { key: '最小列宽 (minWidth)', value: col.minWidth || '-' },
        { key: '渲染类型', value: col.type === 'level' ? '级别标签 (el-tag)' : '文本' },
        { key: '高级列', value: col.advanced ? '是（仅「显示所有列」时展示）' : '否' }
      ]
      let sample = '-'
      for (let i = 0; i < this.autokeys.length; i++) {
        const v = this.autokeys[i][col.prop]
        if (v !== null && typeof v !== 'undefined' && v !== '') {
          sample = String(v)
          break
        }
      }
      rows.push({ key: '示例值（当前页）', value: sample })
      return rows
    },
    // 详情「可导入JSON」: 外层包 {autokeys:[...]}，可直接粘贴到导入弹窗
    detailImportJson() {
      if (!this.detailRow) return ''
      return JSON.stringify({ autokeys: [this.detailRow] }, null, 2)
    },
    importProgress() {
      const r = this.importDialog.result
      if (!r || !r.total) return 0
      return Math.round((r.processed / r.total) * 100)
    },
    importSummaryText() {
      const r = this.importDialog.result
      if (!r) return ''
      const parts = []
      if (r.processed && r.total && r.processed < r.total) {
        parts.push('已处理 ' + r.processed + '/' + r.total + ' 条（已停止）')
      }
      parts.push('新增 ' + (r.added || 0))
      parts.push('更新 ' + (r.updated || 0))
      parts.push('删除 ' + (r.deleted || 0))
      parts.push('失败 ' + (r.failed || 0))
      parts.push('共 ' + (r.total || 0) + ' 条')
      return parts.join(' / ')
    }
  },
  created() {
    // 主表首次查询由 SavedQueryPanel 的 ready 事件触发（handleSavedQueryReady），
    // 保证默认查询条件应用后再查询主表，避免进页面先查一次无条件数据
  },
  watch: {
    // 文件选择/清空等程序化修改时同步到 Monaco 编辑器
    'importDialog.text'(val) {
      if (this.importMonaco && this.importMonaco.getValue() !== (val || '')) {
        this.importMonaco.setValue(val || '')
      }
    },
    batchJson(val) {
      if (this.batchRawMonaco && this.batchRawMonaco.getValue() !== (val || '')) {
        this.batchRawMonaco.setValue(val || '')
      }
    },
    'importDialog.rawResult'(val) {
      if (this.importRawMonaco && this.importRawMonaco.getValue() !== (val || '')) {
        this.importRawMonaco.setValue(val || '')
      }
    }
  },
  beforeDestroy() {
    this.disposeBatchEditors()
    this.disposeDetailEditor()
    this.disposeImportEditor()
    this.disposeImportDetailEditor()
  },
  methods: {
    levelTagType(level) {
      return { SYSTEM: 'info', ORG: 'warning', SITE: 'success', SET: '' }[level] || 'info'
    },
    // ==================== 查询 ====================
    showColInfo(col) {
      this.colInfoDialog.col = col
      this.colInfoDialog.visible = true
    },
    cellFormatter(row, column, cellValue) {
      if (cellValue === null || typeof cellValue === 'undefined' || cellValue === '') {
        return '-'
      }
      return String(cellValue)
    },
    // 表单条件生成 SQL where（供保存查询 default-where 预填与合并自定义 where）
    buildWhere() {
      const f = this.formData
      const esc = s => String(s).replace(/'/g, "''")
      const conds = []
      if (f.autokeyname) conds.push("upper(autokeyname) like '%" + esc(String(f.autokeyname).toUpperCase()) + "%'")
      if (f.prefix) conds.push("upper(prefix) like '%" + esc(String(f.prefix).toUpperCase()) + "%'")
      if (f.siteid) conds.push("upper(siteid) = '" + esc(String(f.siteid).toUpperCase()) + "'")
      if (f.orgid) conds.push("upper(orgid) = '" + esc(String(f.orgid).toUpperCase()) + "'")
      if (f.setid) conds.push("upper(setid) = '" + esc(String(f.setid).toUpperCase()) + "'")
      if (f.level === 'SYSTEM') conds.push('siteid is null and orgid is null and setid is null')
      else if (f.level === 'SITE') conds.push('siteid is not null')
      else if (f.level === 'ORG') conds.push('orgid is not null and siteid is null')
      else if (f.level === 'SET') conds.push('setid is not null')
      return conds.join(' AND ')
    },
    getCustomWhere() {
      return this.$refs.savedQuery ? this.$refs.savedQuery.getWhere() : ''
    },
    handleWhereChange() {
      // 初始化阶段默认查询应用触发的 whereChange 由 handleSavedQueryReady 统一查询，避免重复
      if (!this.sqReady) {
        return
      }
      this.tablePatam.pageNum = 1
      this.getList()
    },
    handleSavedQueryReady() {
      this.sqReady = true
      this.tablePatam.pageNum = 1
      this.getList()
    },
    // 表单 where 与保存查询/自定义 where AND 组合
    mergedWhere() {
      const customWhere = this.getCustomWhere()
      const formWhere = this.buildWhere()
      if (customWhere && formWhere) {
        return '(' + formWhere + ') AND (' + customWhere + ')'
      }
      return customWhere || formWhere || ''
    },
    getList() {
      this.loading = true
      const params = {
        pageNum: this.tablePatam.pageNum,
        pageSize: this.tablePatam.pageSize
      }
      const where = this.mergedWhere()
      if (where) {
        params.where = where
      }
      queryAutokeyList(params).then(res => {
        const data = (res.data && res.data.data) || {}
        this.autokeys = data.autokeys || []
        this.tablePatam.total = data.total || 0
      }).catch(() => {
        this.autokeys = []
        this.tablePatam.total = 0
      }).finally(() => {
        this.loading = false
      })
    },
    handleQuery() {
      this.tablePatam.pageNum = 1
      this.getList()
    },
    resetForm() {
      this.formData = { autokeyname: '', prefix: '', level: '', siteid: '', orgid: '', setid: '' }
      if (this.$refs.savedQuery) {
        this.$refs.savedQuery.setWhere('')
      }
      this.tablePatam.pageNum = 1
      this.getList()
    },
    handleSelectionChange(rows) {
      this.selectedRows = rows
    },
    // ==================== 详情 ====================
    handleRowClick(row) {
      this.currentRow = row
      this.dialogVisible = true
      this.detailLoading = true
      this.detailRow = null
      this.detailJson = ''
      this.activeDetailTab = 'raw'
      getAutokeyDetail({ autokeyid: row.AUTOKEYID }).then(res => {
        const data = (res.data && res.data.data) || {}
        const rows = data.autokeys || []
        this.detailRow = rows[0] || row
        this.detailJson = JSON.stringify(this.detailRow, null, 2)
      }).catch(() => {
        this.detailRow = row
        this.detailJson = JSON.stringify(row, null, 2)
      }).finally(() => {
        this.detailLoading = false
        this.$nextTick(() => {
          this.initDetailMonaco()
        })
      })
    },
    initDetailMonaco() {
      if (!this.detailRow || !this.$refs.detailMonacoRef) {
        return
      }
      if (!this.detailMonaco) {
        this.detailMonaco = monaco.editor.create(this.$refs.detailMonacoRef, {
          value: '',
          language: 'json',
          theme: 'vs',
          readOnly: true,
          minimap: { enabled: false },
          automaticLayout: true,
          scrollBeyondLastLine: false,
          lineNumbers: 'on',
          folding: true,
          wordWrap: 'on',
          fontSize: 12
        })
      }
      this.detailMonaco.setValue(this.detailJson)
    },
    onDetailTabChange() {
      // 切换标签页时确保对应编辑器内容最新
      if (this.activeDetailTab === 'raw') {
        this.$nextTick(() => {
          this.initDetailMonaco()
        })
      } else if (this.activeDetailTab === 'importable') {
        this.$nextTick(() => {
          this.initDetailImportMonaco()
        })
      }
    },
    initDetailImportMonaco() {
      if (!this.detailRow || !this.$refs.detailImportMonacoRef) {
        return
      }
      if (!this.detailImportMonaco) {
        this.detailImportMonaco = monaco.editor.create(this.$refs.detailImportMonacoRef, {
          value: '',
          language: 'json',
          theme: 'vs',
          readOnly: true,
          minimap: { enabled: false },
          automaticLayout: true,
          scrollBeyondLastLine: false,
          lineNumbers: 'on',
          folding: true,
          wordWrap: 'on',
          fontSize: 12
        })
      }
      this.detailImportMonaco.setValue(this.detailImportJson)
    },
    disposeDetailEditor() {
      if (this.detailMonaco) {
        this.detailMonaco.dispose()
        this.detailMonaco = null
      }
      if (this.detailImportMonaco) {
        this.detailImportMonaco.dispose()
        this.detailImportMonaco = null
      }
    },
    copyDetailImportJson() {
      this.copyText(this.detailImportJson, '可导入 JSON 已复制')
    },
    formatMainValue(key) {
      if (!this.detailRow) return ''
      const v = this.detailRow[key]
      if (v === null || typeof v === 'undefined' || v === '') return ''
      return String(v)
    },
    copyDetailJson() {
      this.copyText(this.detailJson, '详情 JSON 已复制')
    },
    // ==================== 导出 ====================
    openExportDialog() {
      // 未勾选记录时不拦截，自动按「当前搜索结果」过滤条件导出全部
      if (this.exportOptions.scope === 'selected' && this.selectedRows.length === 0) {
        this.exportOptions.scope = 'current'
      }
      this.batchJson = ''
      this.batchTotal = 0
      this.batchDialogVisible = true
    },
    onBatchDialogOpened() {
      this.$nextTick(() => {
        this.initBatchMonaco()
        this.onBatchTabChange()
        this.loadExportData()
      })
    },
    initBatchMonaco() {
      if (this.batchMonaco) {
        return
      }
      this.batchMonaco = monaco.editor.create(this.$refs.batchMonacoRef, {
        value: '',
        language: 'json',
        theme: 'vs',
        readOnly: true,
        minimap: { enabled: false },
        automaticLayout: true,
        scrollBeyondLastLine: false,
        lineNumbers: 'on',
        folding: true,
        wordWrap: 'on',
        fontSize: 12
      })
    },
    loadExportData() {
      if (this.exportOptions.scope === 'selected' && this.selectedRows.length === 0) {
        this.exportOptions.scope = 'current'
        this.$message.info('未勾选记录，已切换为按当前过滤条件导出')
      }
      this.batchLoading = true
      const params = this.buildExportParams()
      exportAutokey(params).then(res => {
        const data = (res.data && res.data.data) || {}
        const rows = data.autokeys || []
        this.batchTotal = rows.length
        this.batchJson = JSON.stringify({ autokeys: rows }, null, 2)
        if (this.batchMonaco) {
          this.batchMonaco.setValue(this.batchJson)
        }
        if (!rows.length) {
          this.$message.warning('没有可导出的数据')
        }
      }).catch(() => {
        this.batchJson = ''
        this.batchTotal = 0
        if (this.batchMonaco) {
          this.batchMonaco.setValue('')
        }
      }).finally(() => {
        this.batchLoading = false
      })
    },
    buildExportParams() {
      if (this.exportOptions.scope === 'selected') {
        // 选中记录: 按自动编号名称导出全部级别
        const names = []
        this.selectedRows.forEach(row => {
          if (row.AUTOKEYNAME && names.indexOf(row.AUTOKEYNAME) === -1) {
            names.push(row.AUTOKEYNAME)
          }
        })
        return { autokeynames: names }
      }
      const where = this.mergedWhere()
      return where ? { where } : {}
    },
    copyBatchJson() {
      if (!this.batchJson) {
        this.$message.warning('没有可复制的内容')
        return
      }
      this.copyText(this.batchJson, '导出 JSON 已复制')
    },
    saveBatchJson() {
      if (!this.batchJson) {
        this.$message.warning('没有可保存的内容')
        return
      }
      const blob = new Blob([this.batchJson], { type: 'application/json' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = 'DBAUTOKEY_export_' + new Date().toISOString().slice(0, 10).replace(/-/g, '') + '.json'
      a.click()
      URL.revokeObjectURL(url)
    },
    disposeBatchEditors() {
      if (this.batchMonaco) {
        this.batchMonaco.dispose()
        this.batchMonaco = null
      }
      if (this.batchRawMonaco) {
        this.batchRawMonaco.dispose()
        this.batchRawMonaco = null
      }
    },
    // 通用 Monaco JSON 编辑器创建
    createJsonEditor(el, readOnly, value) {
      return monaco.editor.create(el, {
        value: value || '',
        language: 'json',
        theme: 'vs',
        readOnly: !!readOnly,
        minimap: { enabled: false },
        automaticLayout: true,
        scrollBeyondLastLine: false,
        lineNumbers: 'on',
        folding: true,
        wordWrap: 'on',
        fontSize: 12
      })
    },
    // 导出弹窗「原始JSON」标签页: 懒创建 Monaco
    onBatchTabChange() {
      this.$nextTick(() => {
        if (this.batchActiveTab === 'batch-raw' && !this.batchRawMonaco && this.$refs.batchRawMonacoRef) {
          this.batchRawMonaco = this.createJsonEditor(this.$refs.batchRawMonacoRef, true, this.batchJson)
        }
      })
    },
    // 导入弹窗: 可编辑 Monaco
    onImportDialogOpened() {
      this.$nextTick(() => {
        if (!this.$refs.importMonacoRef) {
          return
        }
        if (!this.importMonaco) {
          this.importMonaco = this.createJsonEditor(this.$refs.importMonacoRef, false, this.importDialog.text)
          this.importMonaco.onDidChangeModelContent(() => {
            const v = this.importMonaco.getValue()
            if (this.importDialog.text !== v) {
              this.importDialog.text = v
            }
          })
        } else {
          this.importMonaco.setValue(this.importDialog.text || '')
        }
      })
    },
    disposeImportEditor() {
      if (this.importMonaco) {
        this.importMonaco.dispose()
        this.importMonaco = null
      }
    },
    // 导入明细弹窗「原始JSON」标签页: Monaco
    onImportDetailOpened() {
      this.$nextTick(() => {
        if (this.$refs.importRawMonacoRef && !this.importRawMonaco) {
          this.importRawMonaco = this.createJsonEditor(this.$refs.importRawMonacoRef, true, this.importDialog.rawResult)
        }
      })
    },
    disposeImportDetailEditor() {
      if (this.importRawMonaco) {
        this.importRawMonaco.dispose()
        this.importRawMonaco = null
      }
    },
    // ==================== 导入 ====================
    openImportDialog() {
      this.importDialog.dmDeploySet = false
      this.importDialog.batchImport = false
      this.importDialog.batchSize = 200
      this.importDialog.fileList = []
      this.importDialog.fileJson = ''
      this.importDialog.text = ''
      this.importDialog.progressText = ''
      this.importDialog.result = null
      this.importDialog.rawResult = ''
      this.importDialog.visible = true
    },
    onImportFileChange(file, fileList) {
      const raw = file.raw
      if (!raw) {
        return
      }
      const reader = new FileReader()
      reader.onload = e => {
        const text = String(e.target.result || '')
        try {
          this.importDialog.fileJson = JSON.stringify(JSON.parse(text))
        } catch (err) {
          this.$message.error('JSON 解析失败：' + (err.message || err))
          this.importDialog.fileJson = ''
        }
        this.importDialog.fileList = fileList.slice(-1)
        this.syncImportText()
      }
      reader.onerror = () => {
        this.$message.error('读取文件失败')
      }
      reader.readAsText(raw)
    },
    onImportFileRemove() {
      this.importDialog.fileJson = ''
      this.importDialog.fileList = []
      this.syncImportText()
    },
    syncImportText() {
      if (this.importDialog.fileJson) {
        this.importDialog.text = this.importDialog.fileJson
      } else if (!this.importDialog.text) {
        this.importDialog.text = ''
      }
    },
    normalizeImportPayload(text) {
      let parsed
      try {
        parsed = JSON.parse(text)
      } catch (err) {
        this.$message.error('JSON 解析失败：' + (err.message || err))
        return null
      }
      if (Array.isArray(parsed)) {
        return { autokeys: parsed }
      }
      if (parsed && Array.isArray(parsed.autokeys)) {
        return parsed
      }
      if (parsed && typeof parsed === 'object' && (parsed.autokeyname || parsed.AUTOKEYNAME)) {
        return { autokeys: [parsed] }
      }
      this.$message.error('无法识别的导入数据，需要 {autokeys:[...]}、数组或单个自动编号对象')
      return null
    },
    handleImport() {
      if (this.importDialog.loading) {
        return
      }
      const payload = this.normalizeImportPayload(this.importDialog.text)
      if (!payload) {
        return
      }
      const items = payload.autokeys || []
      if (!items.length) {
        this.$message.error('autokeys 为空，没有可导入的数据')
        return
      }
      const opts = {
        dmDeploySet: this.importDialog.dmDeploySet,
        errorAssert: false
      }
      this.importDialog.result = null
      this.importDialog.rawResult = ''
      this.importDialog.loading = true
      if (this.importDialog.batchImport) {
        this.runBatchImport(items, opts)
      } else {
        importAutokey({ autokeys: items }, opts).then(res => {
          const merged = this.mergeImportResponse({ total: items.length, processed: 0, added: 0, updated: 0, deleted: 0, failed: 0, results: [] }, res)
          this.importDialog.result = merged
          this.importDialog.rawResult = JSON.stringify(merged, null, 2)
          if (merged.failed > 0) {
            this.$message.warning('导入完成，存在失败记录，可点击「查看详细返回信息」')
          } else {
            this.$message.success('导入成功')
          }
          this.importDialog.loading = false
          this.getList()
        }).catch(() => {
          this.importDialog.loading = false
        })
      }
    },
    runBatchImport(items, opts) {
      const size = Math.max(1, Number(this.importDialog.batchSize) || 200)
      const batches = []
      for (let i = 0; i < items.length; i += size) {
        batches.push(items.slice(i, i + size))
      }
      const merged = {
        total: items.length,
        processed: 0,
        added: 0,
        updated: 0,
        deleted: 0,
        failed: 0,
        results: [],
        stopped: false
      }
      this.importDialog.result = merged
      this.importDialog.progressText = '正在导入 0/' + batches.length + ' 批'
      let chain = Promise.resolve()
      batches.forEach((batch, idx) => {
        chain = chain.then(() => {
          if (merged.stopped) {
            return
          }
          return importAutokey({ autokeys: batch }, opts).then(res => {
            this.mergeImportResponse(merged, res)
            this.importDialog.progressText = '正在导入 ' + (idx + 1) + '/' + batches.length + ' 批'
            if (!res.data || res.data.status !== 'success') {
              merged.stopped = true
              merged.failed++
            }
          }).catch(() => {
            merged.stopped = true
            merged.failed++
          })
        })
      })
      chain.then(() => {
        this.importDialog.loading = false
        this.importDialog.rawResult = JSON.stringify(merged, null, 2)
        if (merged.stopped || merged.failed > 0) {
          this.$message.warning('导入已停止或存在失败记录，可点击「查看详细返回信息」')
        } else {
          this.$message.success('分批导入成功')
        }
        this.getList()
      })
    },
    mergeImportResponse(merged, res) {
      // request 返回完整 axios 响应: res.data = 脚本返回体 {status, data:{total,added,...}, message}
      const body = res.data || {}
      const data = body.data || {}
      const isOk = body.status === 'success' && data.status !== 'error'
      const results = data.results || []
      merged.processed = (merged.processed || 0) + results.length
      merged.added += Number(data.added || 0)
      merged.updated += Number(data.updated || 0)
      merged.deleted += Number(data.deleted || 0)
      if (!isOk) {
        merged.failed += (results.length || 1)
        merged.results = merged.results.concat(results.length ? results : [{
          'sks:ref': 'batch',
          _status: 'failed',
          _operation: 'none',
          _message: (data && data.message) || '批次调用失败'
        }])
      } else {
        merged.failed += Number(data.failed || 0)
        merged.results = merged.results.concat(results)
      }
      return merged
    },
    openImportDetailDialog() {
      const r = this.importDialog.result
      if (!r || !r.results || !r.results.length) {
        this.$message.info('没有明细结果')
        return
      }
      this.importDetailDialog.allExpanded = false
      this.importDetailDialog.rows = r.results.map((row, idx) => ({
        rowKey: 'item-' + idx,
        label: row['sks:ref'] || ('#' + (idx + 1)),
        status: row._status,
        operation: row._operation,
        message: row._message || ''
      }))
      this.importDetailDialog.activeTab = 'list'
      this.importDetailDialog.visible = true
    },
    applyImportDetailExpandAll() {
      this.$nextTick(() => {
        const table = this.$refs.importDetailTable
        if (!table) {
          return
        }
        const expand = this.importDetailDialog.allExpanded
        this.importDetailDialog.rows.forEach(row => {
          table.toggleRowExpansion(row, expand)
        })
      })
    },
    // ==================== 工具 ====================
    copyText(text, okMsg) {
      const helper = (window.clipboardData && window.clipboardData.setData)
        ? Promise.resolve().then(() => window.clipboardData.setData('Text', text))
        : navigator.clipboard.writeText(text)
      helper.then(() => {
        this.$message({ message: okMsg || '已复制', type: 'success' })
      }).catch(() => {
        this.$message({ message: '复制失败', type: 'warning' })
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.query-page {
  padding: 16px;
}
.page-header-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}
.page-header-row h2 {
  margin: 0 0 4px 0;
  font-size: 18px;
  font-weight: 600;
}
.page-summary {
  color: #606266;
  margin: 0;
  font-size: 13px;
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.result-panel {
  margin-top: 20px;
}
.table-toolbar {
  text-align: right;
  margin-bottom: 8px;
}
.prop-name-tag {
  margin-left: 6px;
  cursor: pointer;
}
.prop-search-btn {
  margin-left: 4px;
}
.result-table {
  margin-top: 16px;
}

.el-table__body tr {
  cursor: pointer;
}

.el-table__body tr.current-row>td {
  background-color: #DCDCDC !important;
  color: #000000;
}

.el-table__body tr:hover>td {
  background-color: #d7dcdc !important;
}

.json-tabs {
  margin-top: 16px;
}
.json-toolbar {
  margin-bottom: 8px;
  text-align: right;
}
.monaco-container {
  height: 350px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}
.import-monaco {
  height: 420px;
}
.monaco-wrapper {
  position: relative;
}
.ml-empty {
  color: #c0c4cc;
  font-style: italic;
}
</style>

<style lang="scss">
/* 弹窗被挂载到 body, 需用非 scoped 样式 */
.import-dialog .el-dialog__body {
  max-height: 74vh;
  overflow-y: auto;
}
.import-result-bar {
  margin-top: 12px;
  display: flex;
  align-items: center;
}
.import-result-summary {
  margin: 0 8px;
  color: #606266;
  font-size: 13px;
}
</style>
