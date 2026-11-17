<template>
  <section class="query-page">
    <el-card>
      <div class="page-header-row">
        <div>
          <h2>{{ config.title }}</h2>
          <p class="page-summary">
            维护 {{ config.objectName }} 的 {{ mlFieldLabel }} 多语言译文，支持按行维护及 JSON 导入/导出。
          </p>
        </div>
        <div class="page-actions">
          <saved-query-panel
            ref="savedQuery"
            :appname="config.savedQueryApp"
            :default-where="buildWhere()"
            @whereChange="handleWhereChange"
            @ready="handleSavedQueryReady" />
          <el-button type="primary" size="mini" icon="el-icon-upload2" @click="openImport">导入</el-button>
          <el-button size="mini" icon="el-icon-download" @click="openExport">导出</el-button>
        </div>
      </div>

      <el-form :model="formData" :inline="true" size="mini" @submit.native.prevent>
        <el-form-item v-for="field in config.searchFields" :key="field.param" :label="field.label">
          <el-select
            v-if="field.type === 'select'"
            v-model="formData[field.param]"
            clearable
            placeholder="全部"
            :style="{ width: (field.width || 170) + 'px' }"
            @change="handleQuery">
            <el-option v-for="opt in field.options" :key="optionValue(opt)" :label="optionLabel(opt)" :value="optionValue(opt)" />
          </el-select>
          <el-input
            v-else
            v-model="formData[field.param]"
            :placeholder="field.param === 'value' ? '=精确/%模糊' : field.label"
            clearable
            :style="{ width: (field.width || 170) + 'px' }"
            @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item label="语言">
          <el-select v-model="queryLang" style="width: 150px;" @change="handleQuery">
            <el-option v-for="lang in langOptions" :key="lang.code" :label="languageLabel(lang)" :value="lang.code" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="mini" icon="el-icon-search" :loading="loading" @click="handleQuery">搜索</el-button>
          <el-button size="mini" icon="el-icon-refresh" @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 过滤标签页: 仅用标签头作为查询条件(如 所有/应用XML/MAFAPP) -->
      <el-tabs v-if="config.tabs && config.tabs.length" v-model="listTab" class="filter-tabs" @tab-click="handleTabChange">
        <el-tab-pane v-for="tab in config.tabs" :key="tab.name" :label="tab.label" :name="tab.name" />
      </el-tabs>

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
        :data="list"
        border
        stripe
        size="mini"
        @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="42" />
        <el-table-column
          v-for="col in visibleColumns"
          :key="col.prop + '-' + showPropName"
          :prop="col.prop"
          :width="col.width"
          :min-width="col.minWidth || 120"
          show-overflow-tooltip>
          <template slot="header">
            <span>{{ col.label }}</span>
            <template v-if="showPropName">
              <el-tag size="mini" class="prop-name-tag" @click.native="copyText(col.prop, '已复制属性名 ' + col.prop)">{{ col.prop }}</el-tag>
              <el-tooltip content="显示字段详细信息" placement="top">
                <el-button size="mini" circle icon="el-icon-search" class="prop-search-btn" @click.stop="showColInfo(col)" />
              </el-tooltip>
            </template>
          </template>
          <template slot-scope="scope">{{ formatCell(scope.row[col.prop]) }}</template>
        </el-table-column>
        <el-table-column
          v-for="col in valueLangColumns"
          :key="col.prop + '-' + showPropName"
          :label="col.label"
          min-width="200"
          show-overflow-tooltip>
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
            <span v-if="langValue(scope.row, col.mlField, col.code)" :class="{ 'ml-current': col.code === queryLang }">{{ langValue(scope.row, col.mlField, col.code) }}</span>
            <span v-else class="ml-empty">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template slot-scope="scope">
            <el-button type="text" size="mini" @click.stop="openEditor(scope.row)">维护多语言</el-button>
          </template>
        </el-table-column>
        <template slot="empty">
          <el-empty :description="hasSearched ? '暂无查询结果' : '点击搜索加载数据'" :image-size="80" />
        </template>
      </el-table>

      <el-pagination
        class="page-pagination"
        background
        layout="total, sizes, prev, pager, next, jumper"
        :current-page="pageNum"
        :page-size="pageSize"
        :page-sizes="[20, 50, 100, 200]"
        :total="total"
        @current-change="handlePageChange"
        @size-change="handleSizeChange" />
    </el-card>

    <!-- 字段详细信息 -->
    <el-dialog title="字段详细信息" :visible.sync="colInfoDialog.visible" width="700px" top="10vh" :close-on-click-modal="true" append-to-body>
      <el-table :data="colInfoRows" border stripe size="small">
        <el-table-column prop="key" label="配置项" width="180" />
        <el-table-column prop="value" label="值" min-width="300" show-overflow-tooltip />
      </el-table>
      <span slot="footer" class="dialog-footer">
        <el-button @click="colInfoDialog.visible = false">关 闭</el-button>
      </span>
    </el-dialog>

    <!-- 多语言维护 -->
    <el-dialog :title="'维护多语言 - ' + editorTitle" :visible.sync="editor.visible" width="720px" top="6vh" :close-on-click-modal="false">
      <div class="editor-meta">
        <span v-for="key in config.keyFields" :key="key" class="editor-meta-item">{{ key }}: {{ editor.row[key] }}</span>
      </div>
      <el-tabs v-model="editor.activeLang">
        <el-tab-pane v-for="tab in editor.languages" :key="tab.code" :label="tab.label" :name="tab.code">
          <div v-for="def in mlFieldDefs" :key="def.field" class="editor-field">
            <div class="editor-field-label">{{ def.label || def.field }}</div>
            <el-input
              v-model="tab.values[def.field]"
              type="textarea"
              :rows="mlFieldDefs.length > 1 ? 3 : 4"
              :maxlength="def.maxlength || 4000"
              show-word-limit
              :placeholder="def.field + ' 的 ' + tab.code + ' 译文'" />
          </div>
        </el-tab-pane>
      </el-tabs>
      <!-- JSON 预览/编辑: 默认收起, 首次展开生成当前表单 JSON -->
      <el-collapse v-model="editor.collapse" class="editor-json-collapse" @change="handleEditorCollapse">
        <el-collapse-item name="json">
          <template slot="title">
            <span class="editor-json-title">JSON 预览/编辑</span>
          </template>
          <div class="editor-json-toolbar">
            <el-tooltip content="根据对话框内填写的内容更新 JSON" placement="top">
              <el-button size="mini" type="primary" plain icon="el-icon-caret-bottom" @click="syncJsonFromForm">更新JSON</el-button>
            </el-tooltip>
            <el-tooltip content="根据 JSON 内容更新对话框内的输入框和其他属性" placement="top">
              <el-button size="mini" type="success" plain icon="el-icon-caret-top" @click="applyJsonToForm">应用到表单</el-button>
            </el-tooltip>
            <el-button size="mini" icon="el-icon-copy-document" @click="copyEditorJson">复制当前JSON</el-button>
          </div>
          <el-tabs v-model="editor.jsonTab">
            <el-tab-pane label="单对象JSON" name="single">
              <el-input v-model="editor.jsonSingle" type="textarea" :rows="12" placeholder="当前行对象的多语言 JSON" />
            </el-tab-pane>
            <el-tab-pane label="可导入JSON" name="importable">
              <el-input v-model="editor.jsonImportable" type="textarea" :rows="12" placeholder="外层包导入根节点的 JSON，可直接粘贴到导入对话框" />
            </el-tab-pane>
          </el-tabs>
        </el-collapse-item>
      </el-collapse>
      <span slot="footer" class="dialog-footer">
        <el-button @click="editor.visible = false">取 消</el-button>
        <el-button type="primary" :loading="editor.saving" @click="saveEditor">保 存</el-button>
      </span>
    </el-dialog>

    <!-- 导入 -->
    <el-dialog title="导入多语言 JSON" :visible.sync="importDialog.visible" width="80%" top="5vh" :close-on-click-modal="false" @opened="onImportDialogOpened" @closed="disposeImportEditor">
      <el-alert
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom:12px;"
        :title="config.importMaxObjects
          ? '支持 maxObjects 格式（表结构+多语言 / 仅多语言 / 仅结构，自动识别）或 ' + config.listKey + ' 平铺格式，按 ' + config.keyFields.join(' + ') + ' 匹配更新。'
          : '根节点为 ' + config.listKey + '，按 ' + config.keyFields.join(' + ') + ' 匹配更新；勾选「启用迁移模式」时 MboSet 使用 setDMDeploySet(true)。'" />
      <div class="import-drop-wrap" @dragover.prevent="importDragOver = true" @dragleave.prevent="importDragOver = false" @drop.prevent="onImportDrop">
        <div class="dialog-toolbar">
          <el-upload
            ref="importUpload"
            :auto-upload="false"
            :show-file-list="false"
            accept=".json"
            :on-change="onImportFile">
            <el-button size="mini" icon="el-icon-folder-opened">选择 JSON 文件</el-button>
          </el-upload>
          <el-checkbox v-model="importDialog.dmDeploySet">启用迁移模式(dmDeploySet)</el-checkbox>
          <span class="import-drop-tip">也可将 .json 文件直接拖入本对话框</span>
        </div>
        <div ref="importMonacoRef" class="monaco-container import-monaco" />
        <div v-if="importDialog.result" class="import-result-bar">
          <el-tag :type="importDialog.resultSuccess ? 'success' : 'danger'" size="small">{{ importDialog.resultSuccess ? '导入完成' : '导入存在失败' }}</el-tag>
          <span class="import-result-summary">{{ importDialog.result }}</span>
        </div>
        <div v-if="importDragOver" class="import-drop-mask">松开鼠标即可读取 JSON 文件</div>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="importDialog.visible = false">关 闭</el-button>
        <el-button type="primary" :loading="importDialog.loading" @click="doImport">导 入</el-button>
      </span>
    </el-dialog>

    <!-- 导出 -->
    <el-dialog title="导出多语言 JSON" :visible.sync="exportDialog.visible" width="80%" top="5vh" :close-on-click-modal="false">
      <el-alert
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom:12px;"
        :title="'导出范围为「选中记录」（按 ' + config.primaryKey + ' 数组过滤）或「当前搜索结果」；打开对话框不会自动查询，点击「刷新」生成 JSON。勾选「生成UUID」会为每行写入 sks:uuid。'" />
      <div class="dialog-toolbar">
        <el-radio-group v-model="exportDialog.scope" size="mini">
          <el-radio-button label="selected">选中记录</el-radio-button>
          <el-radio-button label="current">当前搜索结果</el-radio-button>
        </el-radio-group>
        <el-checkbox v-model="exportDialog.allLangcode">导出所有语言(sks:langdata)</el-checkbox>
        <el-checkbox v-model="exportDialog.withUuid">生成UUID</el-checkbox>
        <el-button size="mini" icon="el-icon-refresh" :loading="exportDialog.loading" @click="loadExportData">刷 新</el-button>
        <span class="export-total">共 {{ exportDialog.total }} 条</span>
      </div>
      <div v-if="!exportDialog.json && !exportDialog.loading" class="export-empty-tip">点击「刷新」生成导出 JSON</div>
      <el-input :value="exportDialog.json" type="textarea" :rows="16" readonly placeholder="" />
      <span slot="footer" class="dialog-footer">
        <el-button size="mini" icon="el-icon-copy-document" :disabled="!exportDialog.json" @click="copyExport">复制 JSON</el-button>
        <el-button size="mini" icon="el-icon-download" :disabled="!exportDialog.json" @click="downloadExport">下载 JSON</el-button>
        <el-button @click="exportDialog.visible = false">关 闭</el-button>
      </span>
    </el-dialog>
  </section>
</template>

<script>
import { getDefaultLangcode, getLanguageConfig, getLanguageOptions, loadLanguageConfig } from '@/utils/mmp/mmp-config'
import { exportConfigurMl } from '@/api/mlmaint'
import { generateUUID } from '@/utils/sks'
import SavedQueryPanel from '@/views/components/SavedQueryPanel.vue'
import * as monaco from 'monaco-editor'

const IMPORT_BATCH_SIZE = 200

export default {
  name: 'MlMaintain',
  components: {
    SavedQueryPanel
  },
  props: {
    config: {
      type: Object,
      required: true
    }
  },
  data() {
    const formData = {}
    this.config.searchFields.forEach(field => {
      formData[field.param] = ''
    })
    return {
      formData,
      queryLang: getDefaultLangcode(),
      // 过滤标签页: config.tabs 的第一个 name(如 all), 未配置为空
      listTab: (this.config.tabs && this.config.tabs[0] && this.config.tabs[0].name) || '',
      langOptions: [],
      list: [],
      total: 0,
      pageNum: 1,
      pageSize: 20,
      loading: false,
      hasSearched: false,
      // 保存查询初始化完成前不查主表
      sqReady: false,
      showAllColumns: false,
      showPropName: false,
      colInfoDialog: {
        visible: false,
        col: null
      },
      selectedRows: [],
      editor: {
        visible: false,
        saving: false,
        row: {},
        activeLang: 'EN',
        languages: [],
        // JSON 折叠面板: 默认收起, 数据按需生成
        collapse: [],
        jsonTab: 'single',
        jsonSingle: '',
        jsonImportable: '',
        jsonReady: false
      },
      importDialog: {
        visible: false,
        loading: false,
        dmDeploySet: true,
        text: '',
        result: '',
        resultSuccess: false,
        // 非空表示当前 JSON 为 CONFIGUR 的 maxObjects 格式
        maxObjects: null
      },
      // 导入对话框正在拖入文件
      importDragOver: false,
      // 导入 JSON 的 Monaco 编辑器实例
      importMonaco: null,
      exportDialog: {
        visible: false,
        loading: false,
        scope: 'current',
        allLangcode: true,
        withUuid: true,
        json: '',
        total: 0
      }
    }
  },
  computed: {
    editorTitle() {
      return this.config.keyFields.map(key => this.editor.row[key]).filter(v => v !== null && v !== undefined && v !== '').join(' / ')
    },
    // 多语言字段定义: config.mlFields 支持字符串或 { field, label, maxlength }; 未配置时回退 config.mlField
    mlFieldDefs() {
      const list = this.config.mlFields && this.config.mlFields.length ? this.config.mlFields : [this.config.mlField]
      return list.map(item => {
        const def = typeof item === 'string' ? { field: item } : Object.assign({}, item)
        if (!def.label) {
          def.label = def.field
        }
        return def
      })
    },
    mlFieldLabel() {
      return this.mlFieldDefs.map(def => def.label || def.field).join(' / ')
    },
    visibleColumns() {
      return this.showAllColumns
        ? this.config.columns
        : this.config.columns.filter(col => !col.advanced)
    },
    // 动态语言列: 每个多语言字段 × 后端 sks:langdata 中出现的语言
    valueLangColumns() {
      const current = String(this.queryLang || '').toUpperCase()
      const columns = []
      this.mlFieldDefs.forEach(def => {
        const codes = []
        const add = code => {
          const key = String(code || '').toUpperCase()
          if (key && codes.indexOf(key) === -1) {
            codes.push(key)
          }
        }
        this.list.forEach(row => {
          Object.keys(this.langData(row, def.field)).forEach(add)
        })
        if (current && codes.indexOf(current) >= 0) {
          codes.splice(codes.indexOf(current), 1)
          codes.unshift(current)
        }
        codes.forEach(code => {
          columns.push({
            code,
            mlField: def.field,
            prop: 'sks:langdata.' + def.field + '.' + code,
            label: this.mlFieldDefs.length > 1 ? (def.label || def.field) + '(' + code + ')' : '译文(' + code + ')'
          })
        })
      })
      return columns
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
        { key: '列类型', value: col.code ? '多语言译文列（' + col.mlField + ' / ' + col.code + '）' : (col.advanced ? '高级列（仅「显示所有列」时展示）' : '普通列') }
      ]
      let sample = '-'
      for (let i = 0; i < this.list.length; i++) {
        const value = col.code ? this.langValue(this.list[i], col.mlField, col.code) : this.list[i][col.prop]
        if (value !== null && typeof value !== 'undefined' && value !== '') {
          sample = String(value)
          break
        }
      }
      rows.push({ key: '示例值（当前页）', value: sample })
      return rows
    }
  },
  created() {
    this.initLanguages()
    // 等保存查询 ready 后再首次查询, 保证首查带默认查询条件
  },
  methods: {
    // ==================== 语言 ====================
    initLanguages() {
      const apply = () => {
        const options = getLanguageOptions()
        if (options.length === 0) {
          return
        }
        this.langOptions = options
        const codes = options.map(opt => opt.code)
        if (codes.indexOf(String(this.queryLang || '').toUpperCase()) === -1) {
          const configured = getLanguageConfig() || {}
          this.queryLang = configured.mmpDefaultLang || configured.baseLanguage || codes[0]
        }
      }
      apply()
      loadLanguageConfig().then(apply).catch(() => {})
    },
    optionValue(opt) {
      return opt && typeof opt === 'object' ? opt.value : opt
    },
    optionLabel(opt) {
      return opt && typeof opt === 'object' ? opt.label : opt
    },
    languageLabel(lang) {
      if (!lang) return ''
      if (lang.name && lang.name !== lang.code) {
        return lang.name + '（' + lang.code + '）'
      }
      return lang.code
    },
    languageLabelByCode(code) {
      const opt = this.langOptions.filter(item => item.code === code)[0]
      return opt ? this.languageLabel(opt) : code
    },
    defaultBaseLang() {
      const base = this.langOptions.filter(opt => opt.base)[0]
      if (base) {
        return base.code
      }
      const configured = getLanguageConfig()
      return (configured && (configured.mmpDefaultLang || configured.baseLanguage)) || (this.langOptions[0] || {}).code || 'EN'
    },

    // ==================== 保存查询 ====================
    getCustomWhere() {
      const panel = this.$refs.savedQuery
      return panel && panel.getWhere ? String(panel.getWhere() || '').trim() : ''
    },
    // 当前表单条件生成的 SQL, 用于「保存查询」弹窗预填
    buildWhere() {
      const conds = []
      this.config.searchFields.forEach(field => {
        if (!field.col) {
          return
        }
        const raw = this.formData[field.param]
        const value = raw === undefined || raw === null ? '' : String(raw).trim()
        if (!value) {
          return
        }
        const exact = field.type === 'select' || value.charAt(0) === '='
        const text = (field.type === 'select' ? value : (exact ? value.substring(1) : value)).replace(/'/g, "''")
        const pattern = exact ? text : (text.indexOf('%') >= 0 ? text : '%' + text + '%')
        conds.push('upper(' + field.col + ') ' + (exact ? '=' : 'like') + " '" + pattern.toUpperCase() + "'")
      })
      return conds.join(' and ')
    },
    handleWhereChange() {
      // 初始化阶段默认查询触发的 whereChange 忽略, 由 ready 统一查询
      if (!this.sqReady) {
        return
      }
      this.pageNum = 1
      this.getList()
    },
    handleSavedQueryReady() {
      this.sqReady = true
      this.pageNum = 1
      this.getList()
    },

    // ==================== 查询 ====================
    async getList() {
      this.loading = true
      try {
        const params = {
          pageNum: this.pageNum,
          pageSize: this.pageSize,
          _langcode: this.queryLang,
          _allLangcode: 'true'
        }
        this.config.searchFields.forEach(field => {
          const value = this.formData[field.param]
          if (value !== undefined && value !== null && String(value).trim() !== '') {
            // 下拉固定值按精确匹配(=前缀), 输入框保持原有 =精确/%模糊 语义
            params[field.param] = field.type === 'select' ? '=' + String(value).trim() : String(value).trim()
          }
        })
        const customWhere = this.getCustomWhere()
        if (customWhere) {
          params.where = customWhere
        }
        if (this.listTab) {
          params.tab = this.listTab
        }
        const res = await this.config.api.query(params)
        const data = this.parseScriptResponse(res)
        this.list = data[this.config.listKey] || []
        this.total = Number(data.total || 0)
        this.hasSearched = true
      } catch (e) {
        this.$message.error('查询失败: ' + (e.message || String(e)))
      } finally {
        this.loading = false
      }
    },
    handleQuery() {
      this.pageNum = 1
      this.getList()
    },
    // 标签页作为过滤条件(所有/应用XML/MAFAPP), 切换后重新查询
    handleTabChange() {
      this.pageNum = 1
      this.getList()
    },
    resetForm() {
      this.config.searchFields.forEach(field => {
        this.formData[field.param] = ''
      })
      if (this.config.tabs && this.config.tabs.length) {
        this.listTab = this.config.tabs[0].name
      }
      if (this.$refs.savedQuery) {
        this.$refs.savedQuery.setWhere('')
      }
      this.handleQuery()
    },
    handlePageChange(page) {
      this.pageNum = page
      this.getList()
    },
    handleSizeChange(size) {
      this.pageSize = size
      this.pageNum = 1
      this.getList()
    },
    handleSelectionChange(rows) {
      this.selectedRows = rows || []
    },
    parseScriptResponse(res) {
      const body = (res && res.data) || {}
      if (body.status === 'error') {
        this.$message.error(body.message || '接口返回错误')
        return {}
      }
      return body.data || {}
    },
    langData(row, field) {
      const all = (row && row['sks:langdata']) || {}
      return all[field] || {}
    },
    langValue(row, field, code) {
      return this.langData(row, field)[code] || ''
    },
    formatCell(value) {
      if (value === null || value === undefined || value === '') {
        return '-'
      }
      return value
    },
    showColInfo(col) {
      this.colInfoDialog.col = col
      this.colInfoDialog.visible = true
    },
    copyText(text, message) {
      const content = String(text === null || text === undefined ? '' : text)
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(content).then(() => {
          this.$message.success(message || '已复制')
        }).catch(() => {
          this.copyTextFallback(content, message)
        })
        return
      }
      this.copyTextFallback(content, message)
    },
    copyTextFallback(content, message) {
      const textarea = document.createElement('textarea')
      textarea.value = content
      document.body.appendChild(textarea)
      textarea.select()
      try {
        document.execCommand('copy')
        this.$message.success(message || '已复制')
      } catch (e) {
        this.$message.error('复制失败')
      }
      document.body.removeChild(textarea)
    },

    // ==================== 维护多语言 ====================
    pickKeys(row) {
      const keys = {}
      this.config.keyFields.forEach(key => {
        keys[key] = row[key]
      })
      return keys
    },
    // 某语言下各多语言字段的编辑值
    fieldValues(langdata, code) {
      const values = {}
      this.mlFieldDefs.forEach(def => {
        const fieldData = (langdata && langdata[def.field]) || {}
        const v = fieldData[code]
        values[def.field] = v === undefined || v === null ? '' : String(v)
      })
      return values
    },
    buildTabs(langdata) {
      const codes = []
      const add = code => {
        const key = String(code || '').toUpperCase()
        if (key && codes.indexOf(key) === -1) {
          codes.push(key)
        }
      }
      add(this.defaultBaseLang())
      this.langOptions.forEach(opt => add(opt.code))
      if (codes.length === 0) {
        add('EN')
      }
      this.mlFieldDefs.forEach(def => {
        Object.keys((langdata && langdata[def.field]) || {}).forEach(add)
      })
      return codes.map(code => ({
        code,
        label: this.languageLabelByCode(code),
        values: this.fieldValues(langdata, code)
      }))
    },
    async openEditor(row) {
      const keys = this.pickKeys(row)
      const tabs = this.buildTabs(row['sks:langdata'])
      const preferred = String(this.queryLang || '').toUpperCase()
      this.editor = {
        visible: true,
        saving: false,
        row: keys,
        activeLang: tabs.some(tab => tab.code === preferred) ? preferred : (tabs[0] ? tabs[0].code : 'EN'),
        languages: tabs,
        collapse: [],
        jsonTab: 'single',
        jsonSingle: '',
        jsonImportable: '',
        jsonReady: false
      }
      try {
        const params = Object.assign({ _langcode: this.queryLang, _allLangcode: 'true' }, keys)
        const res = await this.config.api.detail(params)
        const data = this.parseScriptResponse(res)
        const detail = (data[this.config.listKey] || [])[0]
        if (detail) {
          this.editor.row = this.pickKeys(detail)
          this.editor.languages = this.buildTabs(detail['sks:langdata'])
          // 详情内容变化, JSON 需重新生成
          this.editor.jsonSingle = ''
          this.editor.jsonImportable = ''
          this.editor.jsonReady = false
          if (!this.editor.languages.some(tab => tab.code === this.editor.activeLang)) {
            this.editor.activeLang = this.editor.languages[0] ? this.editor.languages[0].code : 'EN'
          }
        }
      } catch (e) {
        this.$message.warning('加载详情失败，将使用列表中的译文: ' + (e.message || String(e)))
      }
    },
    async saveEditor() {
      const langdata = {}
      this.mlFieldDefs.forEach(def => {
        const values = {}
        this.editor.languages.forEach(tab => {
          const raw = tab.values[def.field]
          const text = String(raw === undefined || raw === null ? '' : raw)
          if (text.trim() !== '') {
            values[tab.code] = text
          }
        })
        if (Object.keys(values).length > 0) {
          langdata[def.field] = values
        }
      })
      if (Object.keys(langdata).length === 0) {
        this.$message.warning('请至少填写一种语言的译文')
        return
      }
      const row = Object.assign({}, this.editor.row)
      row['sks:langdata'] = langdata
      this.editor.saving = true
      try {
        const res = await this.config.api.importData(
          { _langcode: getDefaultLangcode(), _allLangcode: 'true' },
          { [this.config.listKey]: [row] }
        )
        const data = this.parseScriptResponse(res)
        const failed = Number(data.failed || 0)
        if (failed > 0) {
          const first = (data.results || []).filter(item => item && item._status === 'failed')[0]
          this.$message.error('保存失败: ' + ((first && first._message) || data.message || '未知错误'))
          return
        }
        this.$message.success('保存成功')
        this.editor.visible = false
        this.getList()
      } catch (e) {
        this.$message.error('保存失败: ' + (e.message || String(e)))
      } finally {
        this.editor.saving = false
      }
    },

    // ==================== 维护弹窗 JSON 面板 ====================
    // 折叠面板首次展开时生成 JSON
    handleEditorCollapse(names) {
      const opened = Array.isArray(names) ? names.indexOf('json') >= 0 : names === 'json'
      if (opened && !this.editor.jsonReady) {
        this.syncJsonFromForm(true)
      }
    },
    // 表单当前内容 => 行对象(含 sks:langdata)
    buildEditorRow() {
      const row = Object.assign({}, this.editor.row)
      const langdata = {}
      this.mlFieldDefs.forEach(def => {
        const values = {}
        this.editor.languages.forEach(tab => {
          const raw = tab.values[def.field]
          const text = String(raw === undefined || raw === null ? '' : raw)
          if (text.trim() !== '') {
            values[tab.code] = text
          }
        })
        if (Object.keys(values).length > 0) {
          langdata[def.field] = values
        }
      })
      if (Object.keys(langdata).length > 0) {
        row['sks:langdata'] = langdata
      }
      return row
    },
    // 对话框内容 -> JSON(el-icon-caret-bottom)
    syncJsonFromForm(silent) {
      const row = this.buildEditorRow()
      this.editor.jsonSingle = JSON.stringify(row, null, 2)
      this.editor.jsonImportable = JSON.stringify({ [this.config.listKey]: [row] }, null, 2)
      this.editor.jsonReady = true
      if (!silent) {
        this.$message.success('已根据表单内容更新 JSON')
      }
    },
    // JSON -> 对话框内容(el-icon-caret-top)
    applyJsonToForm() {
      const text = this.editor.jsonTab === 'importable' ? this.editor.jsonImportable : this.editor.jsonSingle
      if (!text || !text.trim()) {
        this.$message.warning('JSON 内容为空，请先填写或点击「更新JSON」')
        return
      }
      let parsed
      try {
        parsed = JSON.parse(text)
      } catch (e) {
        this.$message.error('JSON 解析失败: ' + (e.message || String(e)))
        return
      }
      // 兼容可导入格式: 根节点数组 / maxObjects
      if (parsed && Array.isArray(parsed[this.config.listKey])) {
        parsed = parsed[this.config.listKey][0]
      } else if (parsed && Array.isArray(parsed.maxObjects)) {
        parsed = parsed.maxObjects[0]
      }
      if (!parsed || typeof parsed !== 'object') {
        this.$message.error('JSON 结构不正确，需为单个行对象')
        return
      }
      // 更新行字段(忽略 sks: 注解与 _ 控制字段)
      const row = Object.assign({}, this.editor.row)
      Object.keys(parsed).forEach(key => {
        if (key === 'sks:langdata' || key.indexOf('sks:') === 0 || key.charAt(0) === '_') return
        row[key] = parsed[key]
      })
      this.editor.row = row
      // 更新各语言输入框, 未知语言自动补页签
      const tabs = this.editor.languages.map(tab => ({
        code: tab.code,
        label: tab.label,
        values: Object.assign({}, tab.values)
      }))
      const langdata = parsed['sks:langdata'] || {}
      const fieldNames = this.mlFieldDefs.map(def => def.field)
      Object.keys(langdata).forEach(field => {
        if (fieldNames.indexOf(field) === -1) return
        const values = langdata[field] || {}
        Object.keys(values).forEach(lang => {
          const code = String(lang).toUpperCase()
          let tab = tabs.filter(item => item.code === code)[0]
          if (!tab) {
            tab = {
              code,
              label: this.languageLabelByCode(code),
              values: {}
            }
            tabs.push(tab)
          }
          tab.values[field] = values[lang]
        })
      })
      this.editor.languages = tabs
      this.$message.success('已按 JSON 更新对话框内容')
    },
    copyEditorJson() {
      const text = this.editor.jsonTab === 'importable' ? this.editor.jsonImportable : this.editor.jsonSingle
      if (!text) {
        this.$message.warning('JSON 内容为空')
        return
      }
      this.copyText(text, 'JSON 已复制')
    },

    // ==================== 导入 ====================
    openImport() {
      this.importDialog.text = ''
      this.importDialog.result = ''
      this.importDialog.resultSuccess = false
      this.importDialog.dmDeploySet = true
      this.importDialog.maxObjects = null
      this.importDragOver = false
      this.importDialog.visible = true
    },
    onImportFile(file) {
      if (!file || !file.raw) return
      this.readImportFile(file.raw)
    },
    // 拖拽文件到对话框时读取(与点击选择共用)
    onImportDrop(event) {
      this.importDragOver = false
      const files = event.dataTransfer ? event.dataTransfer.files : null
      const file = files && files.length ? files[0] : null
      if (!file) return
      if (!/\.json$/i.test(file.name)) {
        this.$message.warning('请拖入 .json 文件')
        return
      }
      this.readImportFile(file)
    },
    // 导入对话框打开后初始化 Monaco
    onImportDialogOpened() {
      this.$nextTick(() => {
        if (!this.$refs.importMonacoRef) {
          return
        }
        if (!this.importMonaco) {
          this.importMonaco = monaco.editor.create(this.$refs.importMonacoRef, {
            value: this.importDialog.text || '',
            language: 'json',
            theme: 'vs',
            minimap: { enabled: false },
            automaticLayout: true,
            scrollBeyondLastLine: false,
            folding: true,
            wordWrap: 'on',
            fontSize: 12
          })
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
    // 文件内容写入 Monaco(编辑器未创建时先放 text, 打开后同步)
    setImportText(text) {
      this.importDialog.text = text
      if (this.importMonaco) {
        this.importMonaco.setValue(text || '')
      }
    },
    readImportFile(file) {
      const reader = new FileReader()
      reader.onload = e => {
        this.setImportText(String(e.target.result || ''))
        this.importDialog.result = ''
        this.importDialog.resultSuccess = false
        this.$message.success('已读取文件: ' + file.name)
      }
      reader.onerror = () => {
        this.$message.error('读取文件失败: ' + file.name)
      }
      reader.readAsText(file)
    },
    parseImportRows(text) {
      const parsed = JSON.parse(text)
      this.importDialog.maxObjects = null
      if (Array.isArray(parsed)) {
        return parsed
      }
      // SKS_MIGRATEAPI_CONFIGUR 的 maxObjects 格式(表结构/仅多语言)
      if (this.config.importMaxObjects && parsed && Array.isArray(parsed.maxObjects)) {
        this.importDialog.maxObjects = parsed.maxObjects
        return parsed.maxObjects
      }
      if (parsed && Array.isArray(parsed[this.config.listKey])) {
        return parsed[this.config.listKey]
      }
      if (parsed && parsed.data && Array.isArray(parsed.data[this.config.listKey])) {
        return parsed.data[this.config.listKey]
      }
      throw new Error('未找到根节点 ' + (this.config.importMaxObjects ? 'maxObjects 或 ' : '') + this.config.listKey)
    },
    async doImport() {
      if (!this.importDialog.text || this.importDialog.text.trim() === '') {
        this.$message.warning('请粘贴或选择要导入的 JSON')
        return
      }
      let rows
      try {
        rows = this.parseImportRows(this.importDialog.text)
      } catch (e) {
        this.$message.error('JSON 解析失败: ' + (e.message || String(e)))
        return
      }
      if (rows.length === 0) {
        this.$message.warning('导入数据为空')
        return
      }
      try {
        await this.$confirm('确认导入 ' + rows.length + ' 条记录？', '提示', { type: 'warning' })
      } catch (e) {
        return
      }
      this.importDialog.loading = true
      this.importDialog.result = ''
      const merged = { total: 0, added: 0, updated: 0, deleted: 0, failed: 0, skipped: 0, firstError: '' }
      try {
        for (let offset = 0; offset < rows.length; offset += IMPORT_BATCH_SIZE) {
          const batch = rows.slice(offset, offset + IMPORT_BATCH_SIZE)
          const res = await this.config.api.importData(
            {
              _langcode: getDefaultLangcode(),
              _allLangcode: 'true',
              dmDeploySet: this.importDialog.dmDeploySet ? 'true' : 'false'
            },
            this.importDialog.maxObjects ? { maxObjects: batch } : { [this.config.listKey]: batch }
          )
          const data = this.parseScriptResponse(res)
          merged.total += Number(data.total || batch.length)
          merged.added += Number(data.added || 0)
          merged.updated += Number(data.updated || 0)
          merged.deleted += Number(data.deleted || 0)
          merged.failed += Number(data.failed || 0)
          merged.skipped += Number(data.skipped || 0)
          if (!merged.firstError && Number(data.failed || 0) > 0) {
            const first = (data.results || []).filter(item => item && item._status === 'failed')[0]
            merged.firstError = (first && first._message) || ''
          }
          this.importDialog.result = '已处理 ' + Math.min(offset + batch.length, rows.length) + '/' + rows.length
        }
        this.importDialog.resultSuccess = merged.failed === 0
        this.importDialog.result = '总数 ' + merged.total + '，新增 ' + merged.added + '，更新 ' + merged.updated +
          '，删除 ' + merged.deleted + '，跳过 ' + merged.skipped + '，失败 ' + merged.failed +
          (merged.firstError ? '（' + merged.firstError + '）' : '')
        if (merged.failed === 0) {
          this.$message.success('导入完成')
          this.getList()
        } else {
          this.$message.error('导入存在 ' + merged.failed + ' 条失败')
        }
      } catch (e) {
        this.importDialog.result = '导入失败: ' + (e.message || String(e))
        this.importDialog.resultSuccess = false
      } finally {
        this.importDialog.loading = false
      }
    },

    // ==================== 导出 ====================
    openExport() {
      this.exportDialog.scope = this.selectedRows.length > 0 ? 'selected' : 'current'
      this.exportDialog.allLangcode = true
      this.exportDialog.withUuid = true
      this.exportDialog.json = ''
      this.exportDialog.total = 0
      this.exportDialog.visible = true
    },
    async loadExportData() {
      const params = {
        _langcode: this.queryLang,
        _allLangcode: this.exportDialog.allLangcode ? 'true' : 'false'
      }
      if (this.listTab) {
        params.tab = this.listTab
      }
      this.exportDialog.loading = true
      try {
        if (this.exportDialog.scope === 'selected') {
          // 仅多语言导出(maxObjects 格式): 直接用选中行, 不再按 id 参数查询
          if (this.config.exportMl) {
            await this.loadMlExport(this.selectedRows.slice())
            return
          }
          const ids = this.selectedRows
            .map(row => row[this.config.primaryKey])
            .filter(value => value !== null && value !== undefined && String(value) !== '')
          if (ids.length === 0) {
            this.$message.warning('选中记录缺少 ' + this.config.primaryKey + '，无法导出')
            return
          }
          params[this.config.idsParam] = ids
        } else {
          this.config.searchFields.forEach(field => {
            const value = this.formData[field.param]
            if (value !== undefined && value !== null && String(value).trim() !== '') {
              params[field.param] = field.type === 'select' ? '=' + String(value).trim() : String(value).trim()
            }
          })
          const customWhere = this.getCustomWhere()
          if (customWhere) {
            params.where = customWhere
          }
        }
        const res = await this.config.api.exportData(params)
        const data = this.parseScriptResponse(res)
        const rows = data[this.config.listKey] || []
        if (this.config.exportMl) {
          await this.loadMlExport(rows)
          return
        }
        rows.forEach(row => {
          if (this.exportDialog.withUuid) {
            if (!row['sks:uuid']) {
              row['sks:uuid'] = generateUUID()
            }
          } else {
            delete row['sks:uuid']
          }
        })
        this.exportDialog.total = rows.length
        this.exportDialog.json = JSON.stringify({ [this.config.listKey]: rows }, null, 2)
      } catch (e) {
        this.$message.error('导出失败: ' + (e.message || String(e)))
      } finally {
        this.exportDialog.loading = false
      }
    },
    // 仅多语言导出: 组装 maxObjects 格式(对象名 + ATTRIBUTENAME + sks:langdata)
    async loadMlExport(rows) {
      const objectNames = []
      const attributeFilters = []
      rows.forEach(row => {
        const objectName = row.OBJECTNAME
        if (objectName && objectNames.indexOf(objectName) === -1) {
          objectNames.push(objectName)
        }
        if (this.config.exportAttributes && objectName && row.ATTRIBUTENAME) {
          attributeFilters.push({ object: objectName, attribute: row.ATTRIBUTENAME })
        }
      })
      if (objectNames.length === 0) {
        this.exportDialog.total = 0
        this.exportDialog.json = ''
        return
      }
      const res = await exportConfigurMl({ objectNames, attributeFilters }, { _langcode: this.queryLang })
      const data = this.parseScriptResponse(res)
      const maxObjects = data.maxObjects || []
      this.exportDialog.total = maxObjects.length
      this.exportDialog.json = JSON.stringify({ maxObjects }, null, 2)
    },
    copyExport() {
      const text = this.exportDialog.json
      if (!text) return
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(text).then(() => {
          this.$message.success('已复制')
        }).catch(() => {
          this.$message.error('复制失败')
        })
      } else {
        const textarea = document.createElement('textarea')
        textarea.value = text
        document.body.appendChild(textarea)
        textarea.select()
        document.execCommand('copy')
        document.body.removeChild(textarea)
        this.$message.success('已复制')
      }
    },
    downloadExport() {
      const text = this.exportDialog.json
      if (!text) return
      const blob = new Blob([text], { type: 'application/json;charset=utf-8' })
      const link = document.createElement('a')
      link.href = URL.createObjectURL(blob)
      link.download = this.config.objectName.toLowerCase() + '_' + this.queryLang + '.json'
      link.click()
      URL.revokeObjectURL(link.href)
    }
  }
}
</script>

<style lang="scss" scoped>
.query-page {
  .page-header-row {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    margin-bottom: 12px;

    h2 {
      margin: 0;
      font-size: 18px;
      font-weight: 600;
    }

    .page-summary {
      margin: 4px 0 0;
      color: #909399;
      font-size: 12px;
    }
  }

  .page-actions {
    display: flex;
    gap: 8px;
  }

  .page-pagination {
    margin-top: 12px;
    text-align: right;
  }

  .table-toolbar {
    text-align: right;
    margin-bottom: 8px;
  }

  /* 过滤标签页: 仅标签头作为过滤条件, 不显示空的标签内容区 */
  .filter-tabs ::v-deep .el-tabs__content {
    display: none;
  }

  .filter-tabs ::v-deep .el-tabs__header {
    margin-bottom: 8px;
  }

  .prop-name-tag {
    margin-left: 6px;
    cursor: pointer;
  }

  .prop-search-btn {
    margin-left: 4px;
  }

  .ml-current {
    color: #409eff;
    font-weight: 600;
  }

  .ml-empty {
    color: #c0c4cc;
    font-style: italic;
  }

  .editor-meta {
    margin-bottom: 8px;
    color: #606266;
    font-size: 12px;
  }

  .editor-meta-item {
    margin-right: 16px;
  }

  .import-drop-wrap {
    position: relative;
  }

  .import-drop-tip {
    font-size: 12px;
    color: #909399;
  }

  .monaco-container {
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    overflow: hidden;
  }

  .import-monaco {
    height: 320px;
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

  .editor-json-collapse {
    margin-top: 10px;

    .editor-json-title {
      font-size: 13px;
      color: #606266;
    }

    .editor-json-toolbar {
      margin-bottom: 8px;
    }

    .el-collapse-item__content {
      padding-bottom: 0;
    }
  }

  .editor-field {
    margin-bottom: 10px;
  }

  .editor-field-label {
    margin-bottom: 4px;
    color: #606266;
    font-size: 12px;
  }

  .dialog-toolbar {
    display: flex;
    align-items: center;
    gap: 14px;
    margin-bottom: 10px;
  }

  .import-result-bar {
    margin-top: 10px;
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .import-result-summary {
    color: #606266;
    font-size: 12px;
  }

  .export-total {
    color: #909399;
    font-size: 12px;
  }

  .export-empty-tip {
    margin-bottom: 6px;
    color: #909399;
    font-size: 12px;
  }
}
</style>
