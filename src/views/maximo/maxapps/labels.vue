<template>
  <section class="query-page">
    <el-card>
      <div class="page-header-row">
        <div>
          <h2>应用XML多语言维护</h2>
          <p class="page-summary">维护 MAXLABELS 应用标签（label / title / mxevent_desc 等）的多语言译文，译文存放在 L_MAXLABELS。支持按应用查询、逐条维护各语言、导出 / 导入 JSON。</p>
        </div>
        <div class="header-actions">
          <saved-query-panel ref="savedQuery" appname="MAXLABELS" :default-where="buildWhere()" @whereChange="handleWhereChange" @ready="handleSavedQueryReady" />
          <el-button type="primary" icon="el-icon-plus" size="mini" @click="openAddDialog">新增标签</el-button>
          <el-button type="primary" icon="el-icon-upload2" size="mini" @click="openImportDialog">导入</el-button>
          <el-button type="primary" icon="el-icon-download" size="mini" @click="openExportDialog">导出</el-button>
        </div>
      </div>

      <el-form ref="queryForm" :model="formData" :inline="true" label-width="72px" @submit.native.prevent>
        <el-form-item label="APP">
          <el-input v-model="formData.app" placeholder="=精确 / 模糊" clearable style="width: 200px;" @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item label="标签ID">
          <el-input v-model="formData.id" placeholder="=精确 / 模糊" clearable style="width: 200px;" @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item label="属性">
          <el-input v-model="formData.property" placeholder="label / title ..." clearable style="width: 170px;" @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item label="值">
          <el-input v-model="formData.value" placeholder="按搜索语言的值模糊搜索" clearable style="width: 200px;" @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item label="搜索语言">
          <el-select v-model="queryLang" style="width: 150px;">
            <el-option v-for="l in baseLangOptions" :key="l.code" :label="languageLabel(l)" :value="l.code" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="cyan" icon="el-icon-search" size="mini" :loading="loading" @click="handleQuery">搜索</el-button>
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
          :data="rows"
          stripe
          size="mini"
          style="width: 100%"
          highlight-current-row
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="46" />
          <el-table-column
            v-for="col in visibleColumns"
            :key="col.prop + '-' + showPropName"
            :prop="col.prop"
            :width="col.width"
            :min-width="col.minWidth"
            show-overflow-tooltip
          >
            <template slot="header">
              <span>{{ col.label }}</span>
              <template v-if="showPropName">
                <el-tag size="mini" class="prop-name-tag" @click.native="copyText(col.prop, '已复制属性名 ' + col.prop)">{{ col.prop }}</el-tag>
                <el-tooltip content="显示字段详细信息" placement="top">
                  <el-button size="mini" circle icon="el-icon-search" class="prop-search-btn" @click.stop="showColInfo(col)" />
                </el-tooltip>
              </template>
            </template>
            <template slot-scope="scope">
              <template v-if="col.prop === 'PROPERTY'">
                <span>{{ scope.row.PROPERTY }}</span>
                <span class="prop-cn">{{ propertyLabel(scope.row.PROPERTY) }}</span>
              </template>
              <span v-else>{{ formatCell(scope.row[col.prop]) }}</span>
            </template>
          </el-table-column>
          <!-- 动态语言列: 后端 sks:langdata.VALUE({语言:值}), 前端扫描语言生成列 -->
          <el-table-column
            v-for="col in valueLangColumns"
            :key="col.prop + '-' + showPropName"
            :label="col.label"
            min-width="200"
            show-overflow-tooltip
          >
            <template slot="header">
              <span>{{ col.label }}</span>
              <template v-if="showPropName">
                <el-tag size="mini" class="prop-name-tag" @click.native="copyText(col.prop, '已复制属性名 ' + col.prop)">{{ col.prop }}</el-tag>
                <el-tooltip content="显示字段详细信息" placement="top">
                  <el-button size="mini" circle icon="el-icon-search" class="prop-search-btn" @click.stop="showColInfo(col)" />
                </el-tooltip>
              </template>
            </template>
            <template slot-scope="scope">
              <span :class="{ 'ml-empty': !getLangValue(scope.row, col.code) }">{{ getLangValue(scope.row, col.code) || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="170" fixed="right">
            <template slot-scope="scope">
              <el-button type="text" size="mini" @click.stop="openEditDialog(scope.row)">维护多语言</el-button>
              <el-button type="text" size="mini" @click.stop="exportRow(scope.row)">导出</el-button>
            </template>
          </el-table-column>
        </el-table>
        <pagination
          v-show="tablePatam.total > 0"
          :total="tablePatam.total"
          :page.sync="tablePatam.pageNum"
          :limit.sync="tablePatam.pageSize"
          @pagination="getList"
        />
        <el-empty v-if="sqReady && !loading && tablePatam.total === 0" description="暂无查询结果，请输入 APP 等条件后搜索" />
      </div>
    </el-card>

    <!-- 多语言维护弹窗 -->
    <el-dialog :title="editor.isNew ? '新增标签' : '维护标签多语言'" :visible.sync="editor.visible" width="760px" top="6vh" :close-on-click-modal="false">
      <el-form :model="editor" label-width="90px" size="small">
        <el-form-item label="APP">
          <el-input v-model="editor.app" :disabled="!editor.isNew" placeholder="应用名称，如 IBM_VENDORAPPLY" />
        </el-form-item>
        <el-form-item label="标签ID">
          <el-input v-model="editor.id" :disabled="!editor.isNew" placeholder="XML 元素的 id 属性值" />
        </el-form-item>
        <el-form-item label="属性">
          <el-select v-model="editor.property" :disabled="!editor.isNew" filterable placeholder="请选择属性" style="width: 100%;">
            <el-option v-for="opt in propertyOptions" :key="opt.value" :label="opt.value + '（' + opt.cn + '）'" :value="opt.value" />
          </el-select>
          <div v-if="propertyLabel(editor.property)" class="prop-hint">中文：{{ propertyLabel(editor.property) }}</div>
        </el-form-item>
      </el-form>
      <el-alert type="info" :closable="false" show-icon title="留空表示不修改该语言；基础语言(EN)的值写入 MAXLABELS.VALUE，其它语言写入 L_MAXLABELS。" style="margin-bottom:12px;" />
      <el-tabs v-model="editor.activeLang">
        <el-tab-pane v-for="lang in editor.languages" :key="lang.code" :label="lang.label" :name="lang.code">
          <el-input v-model="lang.value" type="textarea" :rows="5" :placeholder="lang.code + ' 译文'" />
        </el-tab-pane>
      </el-tabs>
      <span slot="footer" class="dialog-footer">
        <el-button @click="editor.visible = false">取 消</el-button>
        <el-button type="primary" :loading="editor.saving" @click="saveEditor">保 存</el-button>
      </span>
    </el-dialog>

    <!-- 导入弹窗 -->
    <el-dialog title="导入标签多语言" :visible.sync="importDialog.visible" width="85%" top="4vh" :close-on-click-modal="false" @opened="onImportDialogOpened" @closed="disposeImportEditor">
      <el-alert type="info" :closable="false" show-icon title="粘贴导出的 JSON 或选择/拖入 .json 文件（支持 {maxlabelsList:[...]} / 数组 / 单个对象）；按 APP + ID + PROPERTY 匹配新增或更新，_delete=true 删除，sks: 开头属性为注解会被忽略" />
      <div class="import-drop-wrap" @dragover.prevent="importDragOver = true" @dragleave.prevent="importDragOver = false" @drop.prevent="onImportDrop">
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
        <el-form label-width="150px" style="margin-top:12px;">
          <el-form-item label="导入多语言(sks:langdata)">
            <el-checkbox v-model="importDialog.allLangcode">按 sks:langdata 写入各语言译文（_allLangcode=true）</el-checkbox>
          </el-form-item>
          <el-form-item label="启用迁移模式">
            <el-checkbox v-model="importDialog.dmDeploySet">dmDeploySet（MboSet 调用 setDMDeploySet(true)）</el-checkbox>
          </el-form-item>
        </el-form>
        <div ref="importMonacoRef" class="monaco-container import-monaco" />
        <div v-if="importDialog.result" class="import-result-bar">
          <el-tag :type="importDialog.result.failed > 0 ? 'danger' : 'success'" size="small">{{ importDialog.result.failed > 0 ? '存在失败记录' : '导入成功' }}</el-tag>
          <span class="import-result-summary">{{ importSummaryText }}</span>
          <el-button type="text" size="mini" @click="openImportDetailDialog">查看详细返回信息</el-button>
        </div>
        <div v-if="importDragOver" class="import-drop-mask">松开鼠标即可读取 JSON 文件</div>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="importDialog.visible = false">关 闭</el-button>
        <el-button type="primary" :loading="importDialog.loading" @click="handleImport">开始导入</el-button>
      </span>
    </el-dialog>

    <!-- 导入详细返回信息弹窗 -->
    <el-dialog title="导入详细返回信息" :visible.sync="importDetailDialog.visible" width="85%" top="5vh" :close-on-click-modal="true">
      <el-table :data="importDetailDialog.rows" border stripe size="mini" max-height="520" style="width:100%">
        <el-table-column prop="ref" label="记录(APP/ID/PROPERTY)" min-width="300" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="90">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.status" :type="statusTagType(scope.row.status)" size="mini">{{ scope.row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operation" label="操作" width="90" />
        <el-table-column prop="message" label="消息" min-width="260" show-overflow-tooltip />
      </el-table>
      <span slot="footer" class="dialog-footer">
        <el-button @click="importDetailDialog.visible = false">关 闭</el-button>
      </span>
    </el-dialog>

    <!-- 导出弹窗 -->
    <el-dialog title="导出标签多语言" :visible.sync="exportDialog.visible" width="85%" top="4vh" :close-on-click-modal="true" @opened="onExportDialogOpened" @closed="disposeExportEditor">
      <el-alert
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom:12px;"
        title="导出范围为「选中记录」（按 MAXLABELSID 数组过滤）或「当前搜索结果」；打开对话框不会自动查询，设置好选项后点击「刷新」生成 JSON。勾选「生成UUID」会为每行写入 sks:uuid，导入结果会原样带回，便于对账；勾选「导出所有语言」时多语言值放在 sks:langdata 中。"
      />
      <div class="json-toolbar">
        <el-radio-group v-model="exportDialog.scope" size="mini">
          <el-radio-button label="selected">选中记录</el-radio-button>
          <el-radio-button label="current">当前搜索结果</el-radio-button>
        </el-radio-group>
        <el-checkbox v-model="exportDialog.allLangcode" style="margin-left:12px;">导出所有语言(sks:langdata)</el-checkbox>
        <el-checkbox v-model="exportDialog.withUuid">生成UUID</el-checkbox>
        <el-button size="mini" icon="el-icon-refresh" :loading="exportDialog.loading" @click="loadExportData">刷 新</el-button>
        <span class="export-total">共 {{ exportDialog.total }} 条</span>
      </div>
      <div v-if="!exportDialog.json && !exportDialog.loading" class="export-empty-tip">点击「刷新」生成导出 JSON</div>
      <div ref="exportMonacoRef" class="monaco-container" />
      <span slot="footer" class="dialog-footer">
        <el-button type="primary" size="mini" icon="el-icon-document-copy" @click="copyText(exportDialog.json, '导出 JSON 已复制')">复制 JSON</el-button>
        <el-button type="primary" size="mini" icon="el-icon-download" @click="saveExportJson">保存 JSON</el-button>
        <el-button @click="exportDialog.visible = false">关 闭</el-button>
      </span>
    </el-dialog>

    <!-- 单行导出弹窗 -->
    <el-dialog :title="rowExportDialog.title" :visible.sync="rowExportDialog.visible" width="760px" top="6vh" :close-on-click-modal="true" @opened="onRowExportOpened" @closed="disposeRowExportEditors">
      <el-alert
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom:12px;"
        title="「记录片段」可直接粘贴到已有 JSON 的 maxlabelsList 数组中；「可直接导入」是包含根节点的完整 JSON，可直接用于导入。"
      />
      <div class="json-toolbar" style="margin-bottom:8px;">
        <el-checkbox v-model="rowExportDialog.withUuid" @change="rebuildRowExport">生成UUID</el-checkbox>
      </div>
      <el-tabs v-model="rowExportDialog.activeTab" @tab-click="layoutRowExportEditors">
        <el-tab-pane label="记录片段（粘贴到已有 JSON）" name="snippet">
          <div ref="rowExportSnippetRef" class="monaco-container" style="height:280px;" />
        </el-tab-pane>
        <el-tab-pane label="可直接导入（完整 JSON）" name="full">
          <div ref="rowExportFullRef" class="monaco-container" style="height:280px;" />
        </el-tab-pane>
      </el-tabs>
      <span slot="footer" class="dialog-footer">
        <el-button type="primary" size="mini" icon="el-icon-document-copy" @click="copyRowExport">复制当前 JSON</el-button>
        <el-button type="primary" size="mini" icon="el-icon-download" @click="saveRowExport">保存 JSON</el-button>
        <el-button @click="rowExportDialog.visible = false">关 闭</el-button>
      </span>
    </el-dialog>

    <!-- 字段详细信息弹窗（列头放大镜） -->
    <el-dialog title="字段详细信息" :visible.sync="colInfoDialog.visible" width="700px" top="10vh" :close-on-click-modal="true" append-to-body>
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
import { queryMaxLabels, getMaxLabelDetail, exportMaxLabels, importMaxLabels } from '@/api/maxlabels'
import SavedQueryPanel from '@/views/components/SavedQueryPanel.vue'
import { getDefaultLangcode, getLanguageConfig, getLanguageOptions, loadLanguageConfig } from '@/utils/mmp/mmp-config'
import { generateUUID } from '@/utils/sks'
import * as monaco from 'monaco-editor'

// 导入分批提交, 避免单次请求过大超时
const IMPORT_BATCH_SIZE = 500

export default {
  name: 'MaxAppLabels',
  components: {
    SavedQueryPanel
  },
  data() {
    return {
      loading: false,
      sqReady: false,
      showAllColumns: false,
      showPropName: false,
      rows: [],
      selectedRows: [],
      tableColumns: [
        { prop: 'APP', label: 'APP', minWidth: 170 },
        { prop: 'ID', label: '标签ID', minWidth: 180 },
        { prop: 'PROPERTY', label: '属性', width: 190 },
        { prop: 'MAXLABELSID', label: '标签主键', width: 110, advanced: true },
        { prop: 'sks:ref', label: '记录键(APP/ID/PROPERTY)', minWidth: 260, advanced: true }
      ],
      // 属性下拉选项(MAXLABELS.PROPERTY)
      propertyOptions: [
        { value: 'boundlabel', cn: '绑定标签' },
        { value: 'buttonlabel', cn: '按钮标签' },
        { value: 'cancellabel', cn: '取消按钮标签' },
        { value: 'collapsedemptylabel', cn: '折叠空数据提示' },
        { value: 'collapsedlabel', cn: '折叠标签' },
        { value: 'description', cn: '描述' },
        { value: 'donelabel', cn: '完成按钮标签' },
        { value: 'fieldlabel', cn: '字段标签' },
        { value: 'innerhtml', cn: '内嵌HTML' },
        { value: 'label', cn: '标签' },
        { value: 'loadmorelabel', cn: '加载更多标签' },
        { value: 'mxevent_desc', cn: '事件描述' },
        { value: 'norowmsg', cn: '无数据提示' },
        { value: 'oklabel', cn: '确定按钮标签' },
        { value: 'parentemptylabel', cn: '父级空数据提示' },
        { value: 'savechartlabel', cn: '保存图表标签' },
        { value: 'savechartonlylabel', cn: '仅保存图表标签' },
        { value: 'selectchartlabel', cn: '选择图表标签' },
        { value: 'supportedcharttypeslabel', cn: '支持的图表类型标签' },
        { value: 'title', cn: '标题' }
      ],
      formData: {
        app: '',
        id: '',
        property: '',
        value: ''
      },
      queryLang: getDefaultLangcode(),
      // 支持语言统一来自 /solonapi/system/languages(全局缓存), 不再硬编码
      baseLangOptions: [],
      tablePatam: {
        pageNum: 1,
        pageSize: 20,
        total: 0
      },
      // 多语言维护弹窗
      editor: {
        visible: false,
        isNew: false,
        saving: false,
        app: '',
        id: '',
        property: '',
        activeLang: 'ZH',
        languages: []
      },
      // 导入
      importDialog: {
        visible: false,
        loading: false,
        allLangcode: true,
        dmDeploySet: false,
        fileList: [],
        text: '',
        result: null
      },
      importMonaco: null,
      // 导入弹窗正在拖入文件
      importDragOver: false,
      importDetailDialog: {
        visible: false,
        rows: []
      },
      // 导出
      exportDialog: {
        visible: false,
        loading: false,
        scope: 'current',
        withUuid: true,
        allLangcode: true,
        total: 0,
        json: ''
      },
      exportMonaco: null,
      // 单行导出
      rowExportDialog: {
        visible: false,
        title: '导出标签',
        activeTab: 'snippet',
        withUuid: true,
        fileName: 'MAXLABELS',
        item: null,
        snippet: '',
        fullJson: ''
      },
      rowExportSnippetMonaco: null,
      rowExportFullMonaco: null,
      colInfoDialog: {
        visible: false,
        col: null
      }
    }
  },
  computed: {
    visibleColumns() {
      return this.showAllColumns ? this.tableColumns : this.tableColumns.filter(c => !c.advanced)
    },
    // 动态语言列: 扫描当前页数据的 sks:langdata.VALUE 生成语言列, 默认语言排最前
    valueLangColumns() {
      const seen = {}
      const codes = []
      this.rows.forEach(row => {
        const langValues = row && row['sks:langdata'] && row['sks:langdata'].VALUE
        if (!langValues) return
        Object.keys(langValues).forEach(code => {
          if (seen[code]) return
          seen[code] = true
          codes.push(code)
        })
      })
      const preferred = String(this.queryLang || '').toUpperCase()
      return codes.sort((a, b) => {
        if (a === preferred) return -1
        if (b === preferred) return 1
        return a.localeCompare(b)
      }).map(code => ({
        code,
        prop: 'sks:langdata.VALUE.' + code,
        label: '值(' + code + ')'
      }))
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
        { key: '列类型', value: col.code ? '多语言译文列（' + col.code + '）' : (col.advanced ? '高级列（仅「显示所有列」时展示）' : '普通列') }
      ]
      let sample = '-'
      for (let i = 0; i < this.rows.length; i++) {
        const value = col.code ? this.getLangValue(this.rows[i], col.code) : this.rows[i][col.prop]
        if (value !== null && typeof value !== 'undefined' && value !== '') {
          sample = String(value)
          break
        }
      }
      rows.push({ key: '示例值（当前页）', value: sample })
      return rows
    },
    importSummaryText() {
      const r = this.importDialog.result
      if (!r) return ''
      return ['新增 ' + (r.added || 0), '更新 ' + (r.updated || 0), '删除 ' + (r.deleted || 0),
        '跳过 ' + (r.skipped || 0), '失败 ' + (r.failed || 0), '共 ' + (r.total || 0) + ' 条'].join(' / ')
    }
  },
  watch: {
    queryLang() {
      if (!this.sqReady) {
        return
      }
      this.tablePatam.pageNum = 1
      this.getList()
    }
  },
  created() {
    this.initLanguageOptions()
  },
  beforeDestroy() {
    this.disposeImportEditor()
    this.disposeExportEditor()
  },
  methods: {
    // ==================== 支持语言(全局缓存) ====================
    initLanguageOptions() {
      const apply = () => {
        const options = getLanguageOptions()
        if (options.length === 0) {
          return
        }
        this.baseLangOptions = options
        const codes = options.map(opt => opt.code)
        if (codes.indexOf(String(this.queryLang || '').toUpperCase()) === -1) {
          const configured = getLanguageConfig() || {}
          this.queryLang = configured.mmpDefaultLang || configured.baseLanguage || codes[0]
        }
      }
      apply()
      loadLanguageConfig().then(apply).catch(() => {})
    },
    languageLabel(lang) {
      if (!lang) return ''
      if (lang.name && lang.name !== lang.code) {
        return lang.name + '（' + lang.code + '）'
      }
      return lang.code
    },
    languageLabelByCode(code) {
      const opt = this.baseLangOptions.filter(item => item.code === code)[0]
      return opt ? this.languageLabel(opt) : code
    },
    getLangValue(row, code) {
      const langValues = row && row['sks:langdata'] && row['sks:langdata'].VALUE
      if (!langValues) return ''
      return langValues[code] || ''
    },
    propertyLabel(code) {
      const key = String(code || '').toLowerCase()
      const opt = this.propertyOptions.filter(o => o.value === key)[0]
      return opt ? opt.cn : ''
    },
    showColInfo(col) {
      this.colInfoDialog.col = col
      this.colInfoDialog.visible = true
    },
    formatCell(cellValue) {
      if (cellValue === null || typeof cellValue === 'undefined' || cellValue === '') {
        return '-'
      }
      return String(cellValue)
    },
    // ==================== 查询 ====================
    likeCond(field, input) {
      const val = String(input || '').trim()
      if (!val) return ''
      const esc = s => String(s).replace(/'/g, "''")
      if (val.charAt(0) === '=') {
        return 'upper(' + field + ") = '" + esc(val.substring(1).toUpperCase()) + "'"
      }
      const like = val.indexOf('%') >= 0 ? val : '%' + val + '%'
      return 'upper(' + field + ") like '" + esc(like.toUpperCase()) + "'"
    },
    // 表单条件生成 SQL where（供保存查询 default-where 预填与合并自定义 where）
    buildWhere() {
      const f = this.formData
      // value 支持按当前语言译文搜索, 由脚本单独处理, 不拼进 where
      return [
        this.likeCond('app', f.app),
        this.likeCond('id', f.id),
        this.likeCond('property', f.property)
      ].filter(Boolean).join(' AND ')
    },
    getCustomWhere() {
      return this.$refs.savedQuery ? this.$refs.savedQuery.getWhere() : ''
    },
    mergedWhere() {
      const customWhere = this.getCustomWhere()
      const formWhere = this.buildWhere()
      if (customWhere && formWhere) {
        return '(' + formWhere + ') AND (' + customWhere + ')'
      }
      return customWhere || formWhere || ''
    },
    handleWhereChange() {
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
    getList() {
      this.loading = true
      const data = {
        pageNum: this.tablePatam.pageNum,
        pageSize: this.tablePatam.pageSize
      }
      const where = this.mergedWhere()
      if (where) {
        data.where = where
      }
      if (this.formData.value) {
        data.value = this.formData.value
      }
      queryMaxLabels(data, { langcode: this.queryLang, allLangcode: true }).then(res => {
        const result = this.parseScriptResponse(res)
        if (!result) {
          this.rows = []
          this.tablePatam.total = 0
          return
        }
        this.rows = result.maxlabelsList || []
        this.tablePatam.total = result.total || 0
      }).catch(() => {
        this.rows = []
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
      this.formData = { app: '', id: '', property: '', value: '' }
      if (this.$refs.savedQuery) {
        this.$refs.savedQuery.setWhere('')
      }
      this.tablePatam.pageNum = 1
      this.getList()
    },
    handleSelectionChange(rows) {
      this.selectedRows = rows
    },
    // ==================== 多语言维护 ====================
    buildLanguageTabs(row) {
      const current = (row && row['sks:langdata'] && row['sks:langdata'].VALUE) || {}
      const codes = []
      const addCode = code => {
        const key = String(code || '').toUpperCase()
        if (key && codes.indexOf(key) === -1) {
          codes.push(key)
        }
      }
      // 基础语言优先, 其余按后台支持语言顺序, 最后补数据中出现的语言
      addCode(this.defaultBaseLang())
      this.baseLangOptions.forEach(opt => addCode(opt.code))
      if (codes.length === 0) {
        addCode('EN')
      }
      Object.keys(current).forEach(addCode)
      return codes.map(code => ({
        code,
        label: this.languageLabelByCode(code),
        value: current[code] !== undefined && current[code] !== null ? String(current[code]) : ''
      }))
    },
    defaultBaseLang() {
      const base = this.baseLangOptions.filter(opt => opt.base)[0]
      if (base) {
        return base.code
      }
      const configured = getLanguageConfig()
      return (configured && (configured.mmpDefaultLang || configured.baseLanguage)) || (this.baseLangOptions[0] || {}).code || 'EN'
    },
    openAddDialog() {
      this.editor = {
        visible: true,
        isNew: true,
        saving: false,
        app: this.formData.app.replace(/^=/, ''),
        id: '',
        property: 'label',
        activeLang: this.defaultBaseLang(),
        languages: this.buildLanguageTabs(null)
      }
    },
    openEditDialog(row) {
      const tabs = this.buildLanguageTabs(null)
      const preferred = String(this.queryLang || '').toUpperCase()
      this.editor = {
        visible: true,
        isNew: false,
        saving: false,
        app: row.APP,
        id: row.ID,
        property: row.PROPERTY,
        activeLang: tabs.some(tab => tab.code === preferred) ? preferred : (tabs[0] ? tabs[0].code : 'EN'),
        languages: tabs
      }
      getMaxLabelDetail({
        app: row.APP,
        id: row.ID,
        property: row.PROPERTY
      }, { langcode: this.queryLang, allLangcode: true }).then(res => {
        const result = this.parseScriptResponse(res)
        const detail = result && result.maxlabelsList && result.maxlabelsList[0] ? result.maxlabelsList[0] : null
        if (detail) {
          this.editor.languages = this.buildLanguageTabs(detail)
        }
      }).catch(() => {})
    },
    saveEditor() {
      const e = this.editor
      const app = String(e.app || '').trim()
      const id = String(e.id || '').trim()
      const property = String(e.property || '').trim()
      if (!app || !id || !property) {
        this.$message.warning('APP / 标签ID / 属性 不能为空')
        return
      }
      const values = {}
      e.languages.forEach(lang => {
        const v = String(lang.value === null || lang.value === undefined ? '' : lang.value)
        if (v !== '') {
          values[lang.code] = v
        }
      })
      if (Object.keys(values).length === 0) {
        this.$message.warning('至少填写一种语言的值')
        return
      }
      const payload = {
        APP: app,
        ID: id,
        PROPERTY: property,
        VALUE: values.EN || values[Object.keys(values)[0]],
        'sks:langdata': { VALUE: values }
      }
      e.saving = true
      importMaxLabels({ maxlabelsList: [payload] }, { allLangcode: true }).then(res => {
        const body = res.data || {}
        const data = body.data || {}
        if (body.status === 'error' || data.failed > 0) {
          const failed = (data.results || []).filter(r => r._status === 'failed')[0]
          this.$message.error('保存失败: ' + ((failed && failed._message) || body.message || '未知错误'))
          return
        }
        this.$message.success('保存成功')
        e.visible = false
        this.getList()
      }).catch(err => {
        this.$message.error('保存失败: ' + (err.message || String(err)))
      }).finally(() => {
        e.saving = false
      })
    },
    statusTagType(status) {
      if (status === 'success') return 'success'
      if (status === 'skipped') return 'info'
      return 'danger'
    },
    // ==================== 导入 ====================
    openImportDialog() {
      this.importDialog.visible = true
      this.importDialog.allLangcode = true
      this.importDialog.dmDeploySet = false
      this.importDialog.fileList = []
      this.importDragOver = false
      this.importDialog.text = ''
      this.importDialog.result = null
    },
    onImportDialogOpened() {
      this.$nextTick(() => {
        if (!this.$refs.importMonacoRef) {
          return
        }
        if (!this.importMonaco) {
          this.importMonaco = this.createJsonEditor(this.$refs.importMonacoRef, false, this.importDialog.text)
          this.importMonaco.onDidChangeModelContent(() => {
            this.importDialog.text = this.importMonaco.getValue()
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
    onImportFileChange(file, fileList) {
      const raw = file.raw
      if (!raw) {
        return
      }
      this.importDialog.fileList = fileList.slice(-1)
      this.readImportJsonFile(raw)
    },
    // 拖拽文件到弹窗时读取(与点击选择共用)
    onImportDrop(event) {
      this.importDragOver = false
      const files = event.dataTransfer ? event.dataTransfer.files : null
      const file = files && files.length ? files[0] : null
      if (!file) {
        return
      }
      if (!/\.json$/i.test(file.name)) {
        this.$message.warning('请拖入 .json 文件')
        return
      }
      this.importDialog.fileList = []
      this.readImportJsonFile(file)
    },
    readImportJsonFile(raw) {
      const reader = new FileReader()
      reader.onload = e => {
        const text = String(e.target.result || '')
        try {
          this.importDialog.text = JSON.stringify(JSON.parse(text), null, 2)
          this.$message.success('已读取文件: ' + raw.name)
        } catch (err) {
          this.$message.error('JSON 解析失败：' + (err.message || err))
        }
        if (this.importMonaco) {
          this.importMonaco.setValue(this.importDialog.text)
        }
      }
      reader.onerror = () => {
        this.$message.error('读取文件失败')
      }
      reader.readAsText(raw)
    },
    onImportFileRemove() {
      this.importDialog.fileList = []
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
        return { maxlabelsList: parsed }
      }
      if (parsed && Array.isArray(parsed.maxlabelsList)) {
        return parsed
      }
      if (parsed && Array.isArray(parsed.labels)) {
        return { maxlabelsList: parsed.labels }
      }
      if (parsed && typeof parsed === 'object' && (parsed.APP || parsed.app) && (parsed.ID || parsed.id)) {
        return { maxlabelsList: [parsed] }
      }
      this.$message.error('无法识别的导入数据，需要 {maxlabelsList:[...]}、数组或单个标签对象')
      return null
    },
    async handleImport() {
      if (this.importDialog.loading) {
        return
      }
      const payload = this.normalizeImportPayload(this.importDialog.text)
      if (!payload) {
        return
      }
      const items = payload.maxlabelsList || []
      if (!items.length) {
        this.$message.error('maxlabelsList 为空，没有可导入的数据')
        return
      }
      this.importDialog.loading = true
      this.importDialog.result = null
      const merged = { total: items.length, added: 0, updated: 0, deleted: 0, skipped: 0, failed: 0, results: [] }
      const opts = { allLangcode: this.importDialog.allLangcode, dmDeploySet: this.importDialog.dmDeploySet }
      try {
        for (let i = 0; i < items.length; i += IMPORT_BATCH_SIZE) {
          const batch = items.slice(i, i + IMPORT_BATCH_SIZE)
          const res = await importMaxLabels({ maxlabelsList: batch }, opts)
          const body = res.data || {}
          const data = body.data || {}
          if (body.status === 'error') {
            merged.failed += batch.length
            merged.results.push({ 'sks:ref': 'batch-' + (i / IMPORT_BATCH_SIZE + 1), _status: 'failed', _operation: 'none', _message: body.message || '批次调用失败' })
            break
          }
          merged.added += Number(data.added || 0)
          merged.updated += Number(data.updated || 0)
          merged.deleted += Number(data.deleted || 0)
          merged.skipped += Number(data.skipped || 0)
          merged.failed += Number(data.failed || 0)
          merged.results = merged.results.concat(data.results || [])
        }
        this.importDialog.result = merged
        if (merged.failed > 0) {
          this.$message.warning('导入完成，存在失败记录，可点击「查看详细返回信息」')
        } else {
          this.$message.success('导入成功')
        }
        this.getList()
      } catch (err) {
        this.$message.error('导入失败: ' + (err.message || String(err)))
      } finally {
        this.importDialog.loading = false
      }
    },
    openImportDetailDialog() {
      const r = this.importDialog.result
      if (!r || !r.results || !r.results.length) {
        this.$message.info('没有明细结果')
        return
      }
      this.importDetailDialog.rows = r.results.map(row => ({
        ref: row['sks:ref'] || '',
        status: row._status || '',
        operation: row._operation || '',
        message: row._message || ''
      }))
      this.importDetailDialog.visible = true
    },
    // ==================== 导出 ====================
    openExportDialog() {
      // 有勾选记录时默认导出勾选项, 否则按当前搜索结果
      this.exportDialog.scope = this.selectedRows.length > 0 ? 'selected' : 'current'
      this.exportDialog.allLangcode = true
      this.exportDialog.withUuid = true
      this.exportDialog.json = ''
      this.exportDialog.total = 0
      this.exportDialog.visible = true
    },
    onExportDialogOpened() {
      // 不自动查询, 由用户点击「刷新」获取导出内容
      this.$nextTick(() => {
        if (!this.exportMonaco && this.$refs.exportMonacoRef) {
          this.exportMonaco = this.createJsonEditor(this.$refs.exportMonacoRef, true, '')
        }
      })
    },
    disposeExportEditor() {
      if (this.exportMonaco) {
        this.exportMonaco.dispose()
        this.exportMonaco = null
      }
    },
    loadExportData() {
      if (this.exportDialog.scope === 'selected' && this.selectedRows.length === 0) {
        this.exportDialog.scope = 'current'
        this.$message.info('未勾选记录，已切换为按当前过滤条件导出')
      }
      const params = {}
      if (this.exportDialog.scope === 'selected' && this.selectedRows.length > 0) {
        // 选中记录直接按主键数组过滤, 避免拼接超长 where
        params.maxlabelsIds = this.selectedRows
          .map(row => row.MAXLABELSID)
          .filter(value => value !== null && value !== undefined && String(value) !== '')
        if (params.maxlabelsIds.length === 0) {
          this.$message.warning('选中记录缺少 MAXLABELSID，无法导出')
          return
        }
      } else {
        const where = this.mergedWhere()
        if (where) {
          params.where = where
        }
        if (this.formData.value) {
          params.value = this.formData.value
        }
      }
      this.exportDialog.loading = true
      exportMaxLabels(params, { langcode: this.queryLang, allLangcode: this.exportDialog.allLangcode }).then(res => {
        const result = this.parseScriptResponse(res)
        const list = (result && result.maxlabelsList) || []
        if (this.exportDialog.withUuid) {
          list.forEach(row => {
            if (!row['sks:uuid']) {
              row['sks:uuid'] = generateUUID()
            }
          })
        } else {
          list.forEach(row => {
            delete row['sks:uuid']
          })
        }
        this.exportDialog.total = list.length
        this.exportDialog.json = JSON.stringify({ maxlabelsList: list }, null, 2)
        if (this.exportMonaco) {
          this.exportMonaco.setValue(this.exportDialog.json)
        }
        if (!list.length) {
          this.$message.warning('没有可导出的数据')
        }
      }).catch(() => {
        this.exportDialog.json = ''
        this.exportDialog.total = 0
        if (this.exportMonaco) {
          this.exportMonaco.setValue('')
        }
      }).finally(() => {
        this.exportDialog.loading = false
      })
    },
    saveExportJson() {
      if (!this.exportDialog.json) {
        this.$message.warning('没有可保存的内容')
        return
      }
      const blob = new Blob([this.exportDialog.json], { type: 'application/json' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = 'MAXLABELS_export_' + new Date().toISOString().slice(0, 10).replace(/-/g, '') + '.json'
      a.click()
      URL.revokeObjectURL(url)
    },
    // ==================== 单行导出 ====================
    buildRowExportItem(row) {
      const item = {}
      ;['APP', 'ID', 'PROPERTY', 'VALUE'].forEach(key => {
        if (row[key] !== undefined) {
          item[key] = row[key]
        }
      })
      const langdata = row['sks:langdata']
      if (langdata && langdata.VALUE) {
        item['sks:langdata'] = { VALUE: Object.assign({}, langdata.VALUE) }
      }
      return item
    },
    exportRow(row) {
      const name = [row.APP, row.ID, row.PROPERTY].filter(v => v !== null && v !== undefined && v !== '').join('_')
      this.rowExportDialog.title = '导出标签 - ' + [row.APP, row.ID, row.PROPERTY].join(' / ')
      this.rowExportDialog.fileName = 'MAXLABELS_' + (name || 'row')
      this.rowExportDialog.item = this.buildRowExportItem(row)
      this.rowExportDialog.withUuid = true
      this.rowExportDialog.activeTab = 'snippet'
      this.rebuildRowExport()
      this.rowExportDialog.visible = true
    },
    // 按 UUID 勾选状态重建两个页签的 JSON
    rebuildRowExport() {
      if (!this.rowExportDialog.item) {
        return
      }
      const item = Object.assign({}, this.rowExportDialog.item)
      if (this.rowExportDialog.withUuid) {
        if (!item['sks:uuid']) {
          item['sks:uuid'] = generateUUID()
        }
      } else {
        delete item['sks:uuid']
      }
      this.rowExportDialog.snippet = JSON.stringify(item, null, 2)
      this.rowExportDialog.fullJson = JSON.stringify({ maxlabelsList: [item] }, null, 2)
      if (this.rowExportSnippetMonaco) {
        this.rowExportSnippetMonaco.setValue(this.rowExportDialog.snippet)
      }
      if (this.rowExportFullMonaco) {
        this.rowExportFullMonaco.setValue(this.rowExportDialog.fullJson)
      }
    },
    onRowExportOpened() {
      this.$nextTick(() => {
        if (!this.rowExportSnippetMonaco && this.$refs.rowExportSnippetRef) {
          this.rowExportSnippetMonaco = this.createJsonEditor(this.$refs.rowExportSnippetRef, true, this.rowExportDialog.snippet)
        }
        if (!this.rowExportFullMonaco && this.$refs.rowExportFullRef) {
          this.rowExportFullMonaco = this.createJsonEditor(this.$refs.rowExportFullRef, true, this.rowExportDialog.fullJson)
        }
        this.layoutRowExportEditors()
      })
    },
    layoutRowExportEditors() {
      this.$nextTick(() => {
        if (this.rowExportSnippetMonaco) {
          this.rowExportSnippetMonaco.layout()
        }
        if (this.rowExportFullMonaco) {
          this.rowExportFullMonaco.layout()
        }
      })
    },
    disposeRowExportEditors() {
      if (this.rowExportSnippetMonaco) {
        this.rowExportSnippetMonaco.dispose()
        this.rowExportSnippetMonaco = null
      }
      if (this.rowExportFullMonaco) {
        this.rowExportFullMonaco.dispose()
        this.rowExportFullMonaco = null
      }
    },
    copyRowExport() {
      if (this.rowExportDialog.activeTab === 'full') {
        this.copyText(this.rowExportDialog.fullJson, '可直接导入的 JSON 已复制')
      } else {
        this.copyText(this.rowExportDialog.snippet, '记录片段已复制')
      }
    },
    saveRowExport() {
      const text = this.rowExportDialog.fullJson
      if (!text) {
        this.$message.warning('没有可保存的内容')
        return
      }
      const blob = new Blob([text], { type: 'application/json' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = this.rowExportDialog.fileName + '.json'
      a.click()
      URL.revokeObjectURL(url)
    },

    // ==================== 工具 ====================
    parseScriptResponse(res) {
      const body = (res && res.data) || {}
      if (body.status === 'error') {
        this.$message.error(body.message || '接口调用失败')
        return null
      }
      return body.data || {}
    },
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
    copyText(text, okMsg) {
      if (!text) {
        this.$message.warning('没有可复制的内容')
        return
      }
      const helper = (window.clipboardData && window.clipboardData.setData)
        ? Promise.resolve().then(() => window.clipboardData.setData('Text', text))
        : navigator.clipboard.writeText(text)
      helper.then(() => {
        this.$message.success(okMsg || '已复制')
      }).catch(() => {
        this.$message.warning('复制失败')
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
  flex-wrap: wrap;
}
.result-panel {
  margin-top: 12px;
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
.prop-cn {
  margin-left: 6px;
  color: #909399;
  font-size: 12px;
}
.ml-empty {
  color: #c0c4cc;
  font-style: italic;
}
.prop-hint {
  margin-top: 4px;
  color: #909399;
  font-size: 12px;
  line-height: 1.4;
}
.monaco-container {
  height: 320px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}
.import-drop-wrap {
  position: relative;
}
.import-drop-mask {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(64, 158, 255, 0.08);
  border: 1px dashed #409eff;
  border-radius: 6px;
  color: #409eff;
  font-size: 13px;
  pointer-events: none;
}
.import-monaco {
  height: 42vh;
  margin-top: 12px;
}
.import-result-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
}
.json-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.export-empty-tip {
  margin-bottom: 6px;
  color: #909399;
  font-size: 12px;
}
.export-total {
  color: #909399;
  font-size: 12px;
}
</style>
