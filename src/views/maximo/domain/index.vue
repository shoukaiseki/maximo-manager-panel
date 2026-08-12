<template>
  <section class="query-page">
    <el-card>
      <div class="page-header-row">
        <div>
          <h2>域查询</h2>
          <p class="page-summary">查询 MAXDOMAIN 域定义。以 "=" 开头精确匹配，支持 % 通配符模糊搜索。</p>
        </div>
        <div class="page-actions">
          <saved-query-panel ref="savedQuery" appname="DOMAIN" :default-where="buildWhere()" @whereChange="handleWhereChange" />
          <el-button type="warning" icon="el-icon-download" size="mini" style="margin-left: 8px;" :loading="exportLoading" @click="handleExport">导出</el-button>
        </div>
      </div>

      <el-form :model="formData" ref="queryForm" :inline="true" label-width="70px" @submit.native.prevent>
        <el-form-item label="域ID">
          <el-input v-model="formData.domainid" placeholder="如 =STATUS 或 %STAT%" clearable style="width: 200px;" @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item label="域类型">
          <el-select v-model="formData.domaintype" placeholder="全部" clearable style="width: 160px;" @change="handleQuery">
            <el-option label="全部" value="" />
            <el-option label="ALN" value="ALN" />
            <el-option label="SYNONYM" value="SYNONYM" />
            <el-option label="NUMERIC" value="NUMERIC" />
            <el-option label="NUMRANGE" value="NUMRANGE" />
            <el-option label="CROSSOVER" value="CROSSOVER" />
            <el-option label="TABLE" value="TABLE" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="formData.description" placeholder="输入描述关键词..." clearable style="width: 200px;" @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item>
          <el-button type="cyan" icon="el-icon-search" size="mini" :loading="loading" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" size="mini" @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="result-panel">
        <SksTable
          width="100%"
          :showTableColumnButton="true"
          :showRefreshButton="false"
          :mainTable="mainTable"
          :visibleTop="true"
          :highlight-current-row="true"
          @rowClickAfter="handleRowClick"
          @refresh="fetchList">
          <template slot="default">
          </template>
        </SksTable>
        <el-empty v-if="!loading && total === 0 && hasSearched" description="暂无查询结果" />
        <el-empty v-if="!loading && total === 0 && !hasSearched" description="请输入关键词后点击搜索" />
      </div>
    </el-card>

    <!-- 导出结果弹窗 -->
    <el-dialog title="导出域定义" :visible.sync="exportDialogVisible" width="1000px" top="3vh" :close-on-click-modal="true" @opened="onExportDialogOpened">
      <div class="json-toolbar">
        <el-button type="primary" size="mini" icon="el-icon-document-copy" @click="copyExportJson">复制导出JSON</el-button>
        <span style="float:left;color:#606266;line-height:32px;">共导出 {{ exportTotal }} 个域</span>
      </div>
      <div v-loading="exportLoading" element-loading-text="导出中..." class="monaco-wrapper">
        <div ref="exportMonacoRef" class="monaco-container"></div>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="exportDialogVisible = false">关 闭</el-button>
      </span>
    </el-dialog>

    <!-- 域详情弹窗 -->
    <el-dialog :title="'域详情 - ' + (currentRow ? currentRow.domainid : '')" :visible.sync="dialogVisible" width="1500px" top="3vh" :close-on-click-modal="true" @opened="onDialogOpened">
      <el-descriptions :column="4" border v-if="currentRow">
        <el-descriptions-item label="域ID">
          <el-input :value="currentRow.domainid || '-'" readonly size="small" />
        </el-descriptions-item>
        <el-descriptions-item label="域类型">
          <el-input :value="currentRow.domaintype || '-'" readonly size="small" />
        </el-descriptions-item>
        <el-descriptions-item label="中文描述">
          <el-input :value="currentRow.description_zh || currentRow.description || '-'" readonly size="small" />
        </el-descriptions-item>
        <el-descriptions-item label="英文描述">
          <el-input :value="currentRow.description_en || '-'" readonly size="small" />
        </el-descriptions-item>
        <el-descriptions-item label="数据类型">
          <el-input :value="currentRow.maxtype || '-'" readonly size="small" />
        </el-descriptions-item>
        <el-descriptions-item label="长度">
          <el-input :value="currentRow.length !== undefined && currentRow.length !== null ? currentRow.length : '-'" readonly size="small" />
        </el-descriptions-item>
        <el-descriptions-item label="小数位">
          <el-input :value="currentRow.scale !== undefined && currentRow.scale !== null ? currentRow.scale : '-'" readonly size="small" />
        </el-descriptions-item>
        <el-descriptions-item label="内置">
          <el-input :value="currentRow.internal !== undefined && currentRow.internal !== null ? currentRow.internal : '-'" readonly size="small" />
        </el-descriptions-item>
        <el-descriptions-item label="不缓存">
          <el-input :value="currentRow.nevercache !== undefined && currentRow.nevercache !== null ? currentRow.nevercache : '-'" readonly size="small" />
        </el-descriptions-item>
      </el-descriptions>

      <el-tabs v-model="activeTab" type="border-card" class="json-tabs">
        <el-tab-pane label="域值" name="domainValues">
          <div class="subtable-header">
            <span class="subtable-tip" v-if="valueTable">子表: {{ valueTable }}</span>
            <div class="subtable-actions">
              <el-tooltip :content="valueShowAllColumn ? '隐藏多余列' : '显示所有列'" placement="top">
                <el-button size="mini" circle :type="valueShowAllColumn ? 'success' : 'info'" icon="el-icon-menu" @click="toggleValueShowAllColumn" />
              </el-tooltip>
              <el-tooltip :content="valueShowPropName ? '隐藏属性名' : '显示属性名'" placement="top">
                <el-button size="mini" circle :type="valueShowPropName ? 'success' : 'info'" icon="el-icon-s-flag" @click="valueShowPropName = !valueShowPropName" />
              </el-tooltip>
            </div>
          </div>
          <el-table :data="domainValues" border stripe size="small" v-loading="subtableLoading" max-height="420" style="width: 100%">
            <el-table-column v-for="col in valueDisplayColumns" :key="col" :prop="col" :label="valueShowPropName ? col : valueColumnLabel(col)" :show-overflow-tooltip="col !== '_TRANSLATIONS'" :min-width="col === '_TRANSLATIONS' ? 320 : 130">
              <template slot-scope="scope">
                <el-table v-if="col === '_TRANSLATIONS' && scope.row._TRANSLATIONS && scope.row._TRANSLATIONS.length" :data="scope.row._TRANSLATIONS" size="mini" border style="width: 100%" :show-header="false">
                  <el-table-column prop="LANGCODE" label="语言" width="110" />
                  <el-table-column prop="DESCRIPTION" label="描述" show-overflow-tooltip />
                </el-table>
                <span v-else-if="col === '_TRANSLATIONS'" class="cell-trans-empty">-</span>
                <span v-else>{{ scope.row[col] }}</span>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!subtableLoading && domainValues.length === 0" description="无域值数据" />
        </el-tab-pane>
        <el-tab-pane label="域精简json" name="domainSimple">
          <div class="json-toolbar">
            <el-button type="primary" size="mini" icon="el-icon-document-copy" @click="copyDomainSimpleJson">复制域精简JSON</el-button>
          </div>
          <div v-loading="detailLoading" element-loading-text="加载中..." class="monaco-wrapper">
            <div ref="domainSimpleMonacoRef" class="monaco-container"></div>
          </div>
        </el-tab-pane>
        <el-tab-pane label="域完整json" name="domainFull">
          <div class="json-toolbar">
            <el-button type="primary" size="mini" icon="el-icon-document" @click="copyDomainFullJson">复制域完整JSON</el-button>
          </div>
          <div v-loading="detailLoading" element-loading-text="加载中..." class="monaco-wrapper">
            <div ref="domainFullMonacoRef" class="monaco-container"></div>
          </div>
        </el-tab-pane>
        <el-tab-pane label="本地化" name="domainTrans">
          <el-table :data="domainTranslations" border stripe size="small" v-loading="subtableLoading" max-height="420" style="width: 100%">
            <el-table-column prop="LANGCODE" label="语言" width="120" />
            <el-table-column prop="DESCRIPTION" label="本地化描述" show-overflow-tooltip />
          </el-table>
          <el-empty v-if="!subtableLoading && domainTranslations.length === 0" description="无本地化数据" />
        </el-tab-pane>
      </el-tabs>

      <span slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">关 闭</el-button>
      </span>
    </el-dialog>
  </section>
</template>

<script>
import { sksPageMixin } from "sks-plugin-el-erp/lib/sks-page";
import { exportDomains, getDomainSubtables } from '@/api/domain'
import SavedQueryPanel from '@/views/components/SavedQueryPanel.vue'

export default {
  name: 'DomainQuery',
  mixins: [sksPageMixin],
  components: {
    SavedQueryPanel
  },
  data() {
    return {
      loading: false,
      hasSearched: false,
      total: 0,
      formData: {
        domainid: '',
        domaintype: '',
        description: ''
      },
      // 导出
      exportLoading: false,
      exportDialogVisible: false,
      exportJson: '',
      exportTotal: 0,
      exportMonacoLoaded: false,
      exportEditor: null,
      // 详情
      dialogVisible: false,
      detailLoading: false,
      currentRow: null,
      apiSimpleDomains: [],
      apiFullDomains: [],
      activeTab: 'domainValues',
      domainSimpleEditor: null,
      domainFullEditor: null,
      monacoLoaded: false,
      _monaco: null,
      // 子表信息
      subtableLoading: false,
      domainValues: [],
      valueTable: '',
      allValueColumns: [],
      valueDisplayColumns: [],
      valueShowAllColumn: false,
      valueShowPropName: false,
      domainTranslations: []
    }
  },
  watch: {
    dialogVisible(val) {
      if (val && this.currentRow) {
        this.$nextTick(() => {
          this.initDetailEditors()
        })
      } else {
        this.disposeDetailEditors()
      }
    },
    activeTab() {
      this.$nextTick(() => {
        this.layoutDetailEditors()
      })
    },
    exportDialogVisible(val) {
      if (!val) {
        this.disposeExportEditor()
      }
    }
  },
  methods: {
    initMainTableParam() {
      return {
        ownerName: 'maxdomain',
        uniqueId: 'domain-list',
        sksAppName: 'page51',
        tableColumnListEnable: true,
        showPagination: true,
        showTable: true,
        showAllColumnButton: true,
        showTablePropName: false,
        serverPagination: true,
        total: 0,
        queryParams: {
          pageNum: 1,
          pageSize: 20
        },
        tableColumnList:
          this.sksUtils.newTableColumnList([
            { prop: 'domainid', label: '域ID', minWidth: 160 },
            { prop: 'domaintype', label: '域类型', width: 110 },
            { prop: 'description', label: '描述', minWidth: 200 },
            { prop: 'description_zh', label: '中文描述', minWidth: 200 },
            { prop: 'maxtype', label: '数据类型', width: 100 },
            { prop: 'length', label: '长度', width: 80 },
            { prop: 'scale', label: '小数位', width: 80 },
            { prop: 'internal', label: '内置', width: 80 }
          ]),
        queryParamsColumnListEnable: false,
        queryParamsColumnList: []
      }
    },
    // === SQL 条件构建 ===
    escapeSql(v) {
      return String(v || '').replace(/'/g, "''")
    },
    // 域值动态列标题映射（默认显示友好名称，"显示属性名"后显示字段名）
    valueColumnLabel(col) {
      const map = { VALUE: '值', DESCRIPTION: '描述', MAXVALUE: '最大值', DEFAULTS: '默认值', _TRANSLATIONS: '多语言' }
      return map[col] || col
    },
    // 域值次要技术字段默认隐藏
    isHiddenValueColumn(col) {
      if (['SITEID', 'ORGID', 'ROWSTAMP', 'VALUEID', 'MAXVALUE', 'DEFAULTS'].includes(col)) return true
      // 各类型子表主键 ID（ALNDOMAINID 等），DOMAINID/VALUEID 除外
      if (col.endsWith('ID') && col !== 'DOMAINID' && col !== 'VALUEID') return true
      return false
    },
    // 根据显示所有列开关计算当前展示列
    applyValueColumns() {
      if (this.valueShowAllColumn) {
        this.valueDisplayColumns = this.allValueColumns.slice()
      } else {
        this.valueDisplayColumns = this.allValueColumns.filter(c => !this.isHiddenValueColumn(c))
      }
    },
    toggleValueShowAllColumn() {
      this.valueShowAllColumn = !this.valueShowAllColumn
      this.applyValueColumns()
    },
    likeCond(field, input) {
      const val = this.escapeSql(input.trim())
      if (val.startsWith('=')) {
        return field + " = '" + val.slice(1) + "'"
      }
      const like = val.indexOf('%') >= 0 ? val : '%' + val + '%'
      return 'UPPER(' + field + ') LIKE UPPER(\'' + like + '\')'
    },
    buildWhere() {
      const conds = []
      if (this.formData.domainid) {
        conds.push(this.likeCond('DOMAINID', this.formData.domainid))
      }
      if (this.formData.domaintype) {
        conds.push("DOMAINTYPE = '" + this.escapeSql(this.formData.domaintype.trim()) + "'")
      }
      if (this.formData.description) {
        conds.push("UPPER(DESCRIPTION) LIKE UPPER('%" + this.escapeSql(this.formData.description.trim()) + "%')")
      }
      return conds.length > 0 ? conds.join(' AND ') : '1=1'
    },
    // === 列表查询 ===
    handleQuery() {
      this.hasSearched = true
      this.mainTable.queryParams.pageNum = 1
      this.fetchList()
    },
    fetchList() {
      this.loading = true
      exportDomains({
        _langcode: 'ZH',
        apiType: 'manage',
        _action: 'list',
        pageNum: this.mainTable.queryParams.pageNum,
        pageSize: this.mainTable.queryParams.pageSize
      }, {
        where: this.getEffectiveWhere()
      }).then(res => {
        const data = res.data || res
        if (data.status === 'error') {
          this.mainTable.list = []
          this.mainTable.total = 0
          this.total = 0
          this.$message.error(data.message || '查询失败')
        } else {
          this.mainTable.list = data.domains || []
          this.mainTable.total = data.total || 0
          this.total = data.total || 0
        }
      }).catch(err => {
        this.$message.error('查询失败: ' + (err.message || String(err)))
      }).finally(() => {
        this.loading = false
      })
    },
    handlePageChange(page, limit) {
      this.mainTable.queryParams.pageNum = page
      this.mainTable.queryParams.pageSize = limit
      this.fetchList()
    },
    resetForm() {
      this.formData = {
        domainid: '',
        domaintype: '',
        description: ''
      }
      this.hasSearched = false
      this.mainTable.list = []
      this.mainTable.total = 0
      this.total = 0
      this.mainTable.currentPage = 1
    },
    // === 导出 ===
    handleExport() {
      this.exportLoading = true
      this.exportJson = ''
      this.exportTotal = 0
      exportDomains({
        _langcode: 'ZH',
        apiType: 'manage',
        _action: 'export'
      }, {
        where: this.getEffectiveWhere()
      }).then(res => {
        const data = res.data || res
        if (data.status === 'error') {
          this.$message.error(data.message || '导出失败')
          return
        }
        this.exportTotal = (data.domains || []).length
        this.exportJson = JSON.stringify({ domains: data.domains || [] }, null, 2)
        this.exportDialogVisible = true
        this.$nextTick(() => {
          setTimeout(() => {
            this.initExportEditor()
          }, 200)
        })
      }).catch(err => {
        this.$message.error('导出失败: ' + (err.message || String(err)))
      }).finally(() => {
        this.exportLoading = false
      })
    },
    onExportDialogOpened() {
      this.$nextTick(() => {
        setTimeout(() => {
          this.initExportEditor()
        }, 200)
      })
    },
    copyExportJson() {
      if (this.exportJson) {
        this.copyToClipboard(this.exportJson, '导出JSON')
      }
    },
    // 当前生效的 where：表单条件 与 自定义 where 组合
    getEffectiveWhere() {
      const formWhere = this.buildWhere()
      const customWhere = this.$refs.savedQuery ? this.$refs.savedQuery.getWhere() : ''
      const hasForm = formWhere && formWhere !== '1=1'
      if (hasForm && customWhere) {
        return '(' + formWhere + ') AND (' + customWhere + ')'
      }
      return customWhere ? customWhere : formWhere
    },
    // 自定义 where 变化（选择/设置/清除保存的查询时触发）
    handleWhereChange() {
      this.hasSearched = true
      this.mainTable.queryParams.pageNum = 1
      this.fetchList()
    },
    // === 详情 ===
    handleRowClick(row) {
      this.currentRow = row
      this.activeTab = 'domainValues'
      this.dialogVisible = true
      this.detailLoading = true
      this.apiSimpleDomains = []
      this.apiFullDomains = []
      this.domainValues = []
      this.domainTranslations = []
      this.allValueColumns = []
      this.valueDisplayColumns = []
      this.valueShowAllColumn = false
      this.valueShowPropName = false

      const whereClause = "DOMAINID = '" + this.escapeSql(row.domainid) + "'"

      // 加载子表信息（域值 + 本地化）
      this.subtableLoading = true
      getDomainSubtables(row.domainid, row.domaintype).then(res => {
        if (res.code === 200 && res.data) {
          this.domainValues = res.data.values || []
          this.valueTable = res.data.valueTable || ''
          this.domainTranslations = res.data.translations || []
          if (this.domainValues.length > 0) {
            const keys = Object.keys(this.domainValues[0])
            const cols = []
            keys.forEach(k => {
              // 多语言翻译用单元格内嵌子表展示，不作为原始字段列
              if (k === '_TRANSLATIONS') return
              cols.push(k)
              // 多语言列显示在 DESCRIPTION 右边
              if (k === 'DESCRIPTION') cols.push('_TRANSLATIONS')
            })
            this.allValueColumns = cols
            this.applyValueColumns()
          }
        } else {
          this.$message.error('获取子表信息失败: ' + (res.message || '未知错误'))
        }
      }).catch(err => {
        this.$message.error('获取子表信息失败: ' + (err.message || String(err)))
      }).finally(() => {
        this.subtableLoading = false
      })

      Promise.all([
        exportDomains({
          _langcode: 'ZH',
          apiType: 'manage',
          _action: 'export',
          ignoreDefVal: 'true'
        }, {
          where: whereClause
        }),
        exportDomains({
          _langcode: 'ZH',
          apiType: 'manage',
          _action: 'export'
        }, {
          where: whereClause
        })
      ]).then(([simpleRes, fullRes]) => {
        const simpleData = simpleRes.data || simpleRes
        const fullData = fullRes.data || fullRes

        if (simpleData && simpleData.status === 'error') {
          this.$message.error('获取域精简失败: ' + (simpleData.message || '未知错误'))
        } else if (simpleData && simpleData.domains && simpleData.domains.length > 0) {
          this.apiSimpleDomains = simpleData.domains
        }

        if (fullData && fullData.status === 'error') {
          this.$message.error('获取域完整失败: ' + (fullData.message || '未知错误'))
        } else if (fullData && fullData.domains && fullData.domains.length > 0) {
          this.apiFullDomains = fullData.domains
          this.currentRow = Object.assign({}, row, fullData.domains[0])
        }
      }).catch(err => {
        this.$message.error('获取域详情失败: ' + (err.message || String(err)))
      }).finally(() => {
        this.detailLoading = false
        this.$nextTick(() => {
          setTimeout(() => {
            this.initDetailEditors()
          }, 200)
        })
      })
    },
    onDialogOpened() {
      this.$nextTick(() => {
        setTimeout(() => {
          this.initDetailEditors()
        }, 200)
      })
    },
    copyDomainSimpleJson() {
      if (this.apiSimpleDomains && this.apiSimpleDomains.length > 0) {
        this.copyToClipboard(JSON.stringify(this.apiSimpleDomains[0], null, 2), '域精简JSON')
      }
    },
    copyDomainFullJson() {
      if (this.apiFullDomains && this.apiFullDomains.length > 0) {
        this.copyToClipboard(JSON.stringify({ domains: this.apiFullDomains }, null, 2), '域完整JSON')
      }
    },
    // === Monaco Editor ===
    initExportEditor() {
      if (!this.exportJson) return
      if (!this.exportMonacoLoaded) {
        import(/* webpackChunkName: "monaco" */ 'monaco-editor').then(monaco => {
          this.exportMonacoLoaded = true
          this._exportMonaco = monaco
          this.createExportEditor()
        }).catch(err => {
          console.error('Monaco Editor 加载失败:', err)
        })
      } else {
        this.createExportEditor()
      }
    },
    createExportEditor() {
      const monaco = this._exportMonaco
      if (this.$refs.exportMonacoRef && !this.exportEditor) {
        this.exportEditor = monaco.editor.create(this.$refs.exportMonacoRef, {
          value: this.exportJson,
          language: 'json',
          readOnly: true,
          theme: 'vs',
          automaticLayout: true,
          minimap: { enabled: false },
          scrollBeyondLastLine: false,
          fontSize: 13,
          wordWrap: 'on',
          folding: true,
          lineNumbers: 'on',
          renderLineHighlight: 'none'
        })
      } else if (this.exportEditor) {
        this.exportEditor.setValue(this.exportJson)
      }
    },
    disposeExportEditor() {
      if (this.exportEditor) {
        this.exportEditor.dispose()
        this.exportEditor = null
      }
    },
    initDetailEditors() {
      if (!this.currentRow) return
      if (!this.monacoLoaded) {
        import(/* webpackChunkName: "monaco" */ 'monaco-editor').then(monaco => {
          this.monacoLoaded = true
          this._monaco = monaco
          this.createDetailEditors()
        }).catch(err => {
          console.error('Monaco Editor 加载失败:', err)
        })
      } else {
        this.createDetailEditors()
      }
    },
    createDetailEditors() {
      const monaco = this._monaco
      const options = {
        language: 'json',
        readOnly: true,
        theme: 'vs',
        automaticLayout: false,
        minimap: { enabled: false },
        scrollBeyondLastLine: false,
        fontSize: 13,
        wordWrap: 'on',
        folding: true,
        lineNumbers: 'on',
        renderLineHighlight: 'none'
      }
      let simpleJson = ''
      if (this.apiSimpleDomains && this.apiSimpleDomains.length > 0) {
        simpleJson = JSON.stringify(this.apiSimpleDomains[0], null, 2)
      }
      if (this.$refs.domainSimpleMonacoRef && !this.domainSimpleEditor) {
        this.domainSimpleEditor = monaco.editor.create(this.$refs.domainSimpleMonacoRef, {
          value: simpleJson,
          ...options
        })
      } else if (this.domainSimpleEditor) {
        this.domainSimpleEditor.setValue(simpleJson)
      }

      let fullJson = ''
      if (this.apiFullDomains && this.apiFullDomains.length > 0) {
        fullJson = JSON.stringify({ domains: this.apiFullDomains }, null, 2)
      }
      if (this.$refs.domainFullMonacoRef && !this.domainFullEditor) {
        this.domainFullEditor = monaco.editor.create(this.$refs.domainFullMonacoRef, {
          value: fullJson,
          ...options
        })
      } else if (this.domainFullEditor) {
        this.domainFullEditor.setValue(fullJson)
      }

      setTimeout(() => {
        this.layoutDetailEditors()
      }, 100)
    },
    layoutDetailEditors() {
      if (this.domainSimpleEditor) this.domainSimpleEditor.layout()
      if (this.domainFullEditor) this.domainFullEditor.layout()
    },
    disposeDetailEditors() {
      if (this.domainSimpleEditor) {
        this.domainSimpleEditor.dispose()
        this.domainSimpleEditor = null
      }
      if (this.domainFullEditor) {
        this.domainFullEditor.dispose()
        this.domainFullEditor = null
      }
    },
    copyToClipboard(text, label) {
      if (navigator.clipboard && window.isSecureContext) {
        navigator.clipboard.writeText(text)
          .then(() => {
            this.$message.success(label + ' 已复制到剪贴板')
          })
          .catch(() => {
            this.fallbackCopy(text, label)
          })
      } else {
        this.fallbackCopy(text, label)
      }
    },
    fallbackCopy(text, label) {
      const textarea = document.createElement('textarea')
      textarea.value = text
      textarea.style.position = 'fixed'
      textarea.style.top = '-1000px'
      textarea.style.left = '-1000px'
      document.body.appendChild(textarea)
      textarea.select()
      try {
        document.execCommand('copy')
        this.$message.success(label + ' 已复制到剪贴板')
      } catch (e) {
        this.$message.error('复制失败: ' + e.message)
      }
      document.body.removeChild(textarea)
    }
  },
  mounted() {
    if (this.mainTable) {
      this.mainTable.onPageChange = (page, limit) => {
        this.handlePageChange(page, limit)
      }
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
.page-actions {
  display: flex;
  align-items: center;
}
.result-panel {
  margin-top: 20px;
}
.json-tabs {
  margin-top: 16px;
}
.subtable-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.subtable-tip {
  color: #909399;
  font-size: 12px;
  margin: 0;
}
.subtable-actions {
  display: flex;
  gap: 4px;
}
.cell-trans-empty {
  color: #909399;
  font-size: 12px;
}
.json-toolbar {
  margin-bottom: 8px;
  text-align: right;
}
.monaco-wrapper {
  position: relative;
}
.monaco-container {
  height: 400px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}
</style>
