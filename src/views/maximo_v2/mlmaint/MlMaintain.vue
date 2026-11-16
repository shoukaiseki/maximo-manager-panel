<template>
  <section class="query-page">
    <el-card>
      <div class="page-header-row">
        <div>
          <h2>{{ config.title }}</h2>
          <p class="page-summary">
            维护 {{ config.objectName }}.{{ config.mlField }} 的多语言译文，支持按行维护及 JSON 导入/导出。
          </p>
        </div>
        <div class="page-actions">
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
            <span v-if="langValue(scope.row, col.code)" :class="{ 'ml-current': col.code === queryLang }">{{ langValue(scope.row, col.code) }}</span>
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
          <el-input
            v-model="tab.value"
            type="textarea"
            :rows="4"
            maxlength="4000"
            show-word-limit
            :placeholder="config.mlField + ' 的 ' + tab.code + ' 译文'" />
        </el-tab-pane>
      </el-tabs>
      <span slot="footer" class="dialog-footer">
        <el-button @click="editor.visible = false">取 消</el-button>
        <el-button type="primary" :loading="editor.saving" @click="saveEditor">保 存</el-button>
      </span>
    </el-dialog>

    <!-- 导入 -->
    <el-dialog title="导入多语言 JSON" :visible.sync="importDialog.visible" width="80%" top="5vh" :close-on-click-modal="false">
      <el-alert
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom:12px;"
        :title="'根节点为 ' + config.listKey + '，按 ' + config.keyFields.join(' + ') + ' 匹配更新；勾选「启用迁移模式」时 MboSet 使用 setDMDeploySet(true)。'" />
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
      </div>
      <el-input v-model="importDialog.text" type="textarea" :rows="16" placeholder="粘贴导出 JSON 或选择文件" />
      <div v-if="importDialog.result" class="import-result-bar">
        <el-tag :type="importDialog.resultSuccess ? 'success' : 'danger'" size="small">{{ importDialog.resultSuccess ? '导入完成' : '导入存在失败' }}</el-tag>
        <span class="import-result-summary">{{ importDialog.result }}</span>
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
import { generateUUID } from '@/utils/sks'

const IMPORT_BATCH_SIZE = 200

export default {
  name: 'MlMaintain',
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
      langOptions: [],
      list: [],
      total: 0,
      pageNum: 1,
      pageSize: 20,
      loading: false,
      hasSearched: false,
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
        languages: []
      },
      importDialog: {
        visible: false,
        loading: false,
        dmDeploySet: true,
        text: '',
        result: '',
        resultSuccess: false
      },
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
    visibleColumns() {
      return this.showAllColumns
        ? this.config.columns
        : this.config.columns.filter(col => !col.advanced)
    },
    // 动态语言列: 后端 sks:langdata.<mlField>({语言:值}), 前端扫描语言生成列
    valueLangColumns() {
      const codes = []
      const add = code => {
        const key = String(code || '').toUpperCase()
        if (key && codes.indexOf(key) === -1) {
          codes.push(key)
        }
      }
      this.list.forEach(row => {
        Object.keys(this.langData(row)).forEach(add)
      })
      const current = String(this.queryLang || '').toUpperCase()
      if (current && codes.indexOf(current) >= 0) {
        codes.splice(codes.indexOf(current), 1)
        codes.unshift(current)
      }
      return codes.map(code => ({
        code,
        prop: 'sks:langdata.' + this.config.mlField + '.' + code,
        label: '译文(' + code + ')'
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
      for (let i = 0; i < this.list.length; i++) {
        const value = col.code ? this.langValue(this.list[i], col.code) : this.list[i][col.prop]
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
    this.getList()
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
    resetForm() {
      this.config.searchFields.forEach(field => {
        this.formData[field.param] = ''
      })
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
    langData(row) {
      return (row && row['sks:langdata'] && row['sks:langdata'][this.config.mlField]) || {}
    },
    langValue(row, code) {
      return this.langData(row)[code] || ''
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
    buildTabs(values) {
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
      Object.keys(values || {}).forEach(add)
      return codes.map(code => ({
        code,
        label: this.languageLabelByCode(code),
        value: values && values[code] !== undefined && values[code] !== null ? String(values[code]) : ''
      }))
    },
    async openEditor(row) {
      const keys = this.pickKeys(row)
      const tabs = this.buildTabs(this.langData(row))
      const preferred = String(this.queryLang || '').toUpperCase()
      this.editor = {
        visible: true,
        saving: false,
        row: keys,
        activeLang: tabs.some(tab => tab.code === preferred) ? preferred : (tabs[0] ? tabs[0].code : 'EN'),
        languages: tabs
      }
      try {
        const params = Object.assign({ _langcode: this.queryLang, _allLangcode: 'true' }, keys)
        const res = await this.config.api.detail(params)
        const data = this.parseScriptResponse(res)
        const detail = (data[this.config.listKey] || [])[0]
        if (detail) {
          this.editor.row = this.pickKeys(detail)
          this.editor.languages = this.buildTabs(this.langData(detail))
          if (!this.editor.languages.some(tab => tab.code === this.editor.activeLang)) {
            this.editor.activeLang = this.editor.languages[0] ? this.editor.languages[0].code : 'EN'
          }
        }
      } catch (e) {
        this.$message.warning('加载详情失败，将使用列表中的译文: ' + (e.message || String(e)))
      }
    },
    async saveEditor() {
      const values = {}
      this.editor.languages.forEach(tab => {
        const text = String(tab.value === undefined || tab.value === null ? '' : tab.value)
        if (text.trim() !== '') {
          values[tab.code] = text
        }
      })
      if (Object.keys(values).length === 0) {
        this.$message.warning('请至少填写一种语言的译文')
        return
      }
      const row = Object.assign({}, this.editor.row)
      row['sks:langdata'] = { [this.config.mlField]: values }
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

    // ==================== 导入 ====================
    openImport() {
      this.importDialog.text = ''
      this.importDialog.result = ''
      this.importDialog.resultSuccess = false
      this.importDialog.dmDeploySet = true
      this.importDialog.visible = true
    },
    onImportFile(file) {
      const reader = new FileReader()
      reader.onload = e => {
        this.importDialog.text = String(e.target.result || '')
      }
      reader.readAsText(file.raw)
    },
    parseImportRows(text) {
      const parsed = JSON.parse(text)
      if (Array.isArray(parsed)) {
        return parsed
      }
      if (parsed && Array.isArray(parsed[this.config.listKey])) {
        return parsed[this.config.listKey]
      }
      if (parsed && parsed.data && Array.isArray(parsed.data[this.config.listKey])) {
        return parsed.data[this.config.listKey]
      }
      throw new Error('未找到根节点 ' + this.config.listKey)
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
            { [this.config.listKey]: batch }
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
      if (this.exportDialog.scope === 'selected') {
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
      }
      this.exportDialog.loading = true
      try {
        const res = await this.config.api.exportData(params)
        const data = this.parseScriptResponse(res)
        const rows = data[this.config.listKey] || []
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
