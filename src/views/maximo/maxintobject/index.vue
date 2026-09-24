<template>
  <section class="query-page">
    <el-card>
      <div class="page-header-row">
        <div>
          <h2>对象结构管理器</h2>
          <p class="page-summary">查询 MAXINTOBJECT 对象结构定义（含 MAXINTOBJDETAIL 明细、字段与别名）。以 "=" 开头精确匹配，支持 % 通配符模糊搜索。</p>
        </div>
        <div class="page-actions">
          <saved-query-panel ref="savedQuery" appname="MAXINTOBJECT" :default-where="buildWhere()" @whereChange="handleWhereChange" />
          <el-button type="success" icon="el-icon-upload2" size="mini" style="margin-left: 8px;" @click="openImportDialog">导入</el-button>
          <el-button type="warning" icon="el-icon-download" size="mini" style="margin-left: 8px;" :loading="exportLoading" @click="handleExport">导出</el-button>
        </div>
      </div>

      <el-form :model="formData" ref="queryForm" :inline="true" label-width="80px" @submit.native.prevent>
        <el-form-item label="结构名称">
          <el-input v-model="formData.intobjectname" placeholder="=精确/%模糊" clearable style="width: 200px;" @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="formData.description" placeholder="=精确/%模糊" clearable style="width: 200px;" @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item label="支持">
          <el-input v-model="formData.usewith" placeholder="如 INTEGRATION,OSLC" clearable style="width: 200px;" @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item label="仅查询">
          <el-select v-model="formData.queryonly" placeholder="全部" clearable style="width: 100px;">
            <el-option label="是" value="1" />
            <el-option label="否" value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="平铺支持">
          <el-select v-model="formData.flatsupported" placeholder="全部" clearable style="width: 100px;">
            <el-option label="是" value="1" />
            <el-option label="否" value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="cyan" icon="el-icon-search" size="mini" :loading="mainTable.loading" @click="handleQuery">搜索</el-button>
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
          @refresh="getMainList">
          <template slot="default">
          </template>
        </SksTable>
        <el-empty v-if="!mainTable.loading && mainTable.total === 0" description="暂无查询结果" />
      </div>
    </el-card>

    <!-- 导出结果弹窗 -->
    <el-dialog title="导出对象结构" :visible.sync="exportDialogVisible" width="1000px" top="3vh" :close-on-click-modal="true" @opened="onExportDialogOpened">
      <div class="json-toolbar">
        <span style="float:left;color:#606266;line-height:32px;">共导出 {{ exportTotal }} 个对象结构</span>
        <el-checkbox v-model="exportIgnoreDefVal" style="margin-right:12px;">精简模式(省略默认值)</el-checkbox>
        <el-button type="primary" size="mini" icon="el-icon-document-copy" @click="copyExportJson">复制导出JSON</el-button>
      </div>
      <div v-loading="exportLoading" element-loading-text="导出中..." class="monaco-wrapper">
        <div ref="exportMonacoRef" class="monaco-container"></div>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="exportDialogVisible = false">关 闭</el-button>
      </span>
    </el-dialog>

    <!-- 导入 JSON 弹窗 -->
    <el-dialog title="导入对象结构" :visible.sync="importDialog.visible" width="800px" top="3vh" :close-on-click-modal="false" @opened="onImportDialogOpened">
      <p style="margin:0 0 8px;color:#909399;font-size:12px;">粘贴 JSON（数组 或 {"integrationobjects": [...]}），支持导出结果直接导入；按 intObjectName 匹配更新或创建。</p>
      <div v-loading="importDialog.loading" element-loading-text="导入中..." class="monaco-wrapper">
        <div ref="importMonacoRef" class="monaco-container import-monaco"></div>
      </div>
      <p style="margin:8px 0 0;color:#f56c6c;font-size:12px" v-if="importDialog.error">{{ importDialog.error }}</p>
      <div v-if="importDialog.summary" class="import-summary">
        <p>导入完成：共 {{ importDialog.summary.total }} 条，成功 {{ importDialog.summary.success }} 条，失败 {{ importDialog.summary.failed }} 条</p>
        <el-table :data="importDialog.result" border stripe size="mini" max-height="260" style="width: 100%">
          <el-table-column prop="intobjectname" label="结构名称" min-width="160" show-overflow-tooltip />
          <el-table-column prop="status" label="状态" width="100">
            <template slot-scope="scope">
              <el-tag :type="scope.row.status === 'SUCCESS' ? 'success' : 'danger'" size="mini">{{ scope.row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="message" label="消息" min-width="240" show-overflow-tooltip />
        </el-table>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="importDialog.visible = false">关 闭</el-button>
        <el-button type="primary" :loading="importDialog.loading" @click="submitImport">导 入</el-button>
      </span>
    </el-dialog>

    <!-- 对象结构详情弹窗 -->
    <el-dialog :title="'对象结构详情 - ' + (currentRow ? currentRow.intobjectname : '')" :visible.sync="dialogVisible" width="1000px" top="3vh" :close-on-click-modal="true">
      <el-descriptions :column="2" border v-if="currentRow" class="detail-desc" size="small">
        <el-descriptions-item label="结构名称">
          <el-input :value="currentRow.intobjectname || '-'" readonly size="small" />
        </el-descriptions-item>
        <el-descriptions-item label="支持">
          <el-input :value="currentRow.usewith || '-'" readonly size="small" />
        </el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">
          <el-input :value="currentRow.description || '-'" readonly size="small" />
        </el-descriptions-item>
        <el-descriptions-item label="仅查询">
          <el-input :value="currentRow.queryonly !== undefined && currentRow.queryonly !== null && currentRow.queryonly !== '' ? currentRow.queryonly : '-'" readonly size="small" />
        </el-descriptions-item>
        <el-descriptions-item label="平铺支持">
          <el-input :value="currentRow.flatsupported !== undefined && currentRow.flatsupported !== null && currentRow.flatsupported !== '' ? currentRow.flatsupported : '-'" readonly size="small" />
        </el-descriptions-item>
        <el-descriptions-item label="应用程序">
          <el-input :value="currentRow.authapp || '-'" readonly size="small" />
        </el-descriptions-item>
        <el-descriptions-item label="模块">
          <el-input :value="currentRow.module || '-'" readonly size="small" />
        </el-descriptions-item>
        <el-descriptions-item label="别名冲突">
          <el-input :value="currentRow.aliasConflict !== undefined && currentRow.aliasConflict !== null && currentRow.aliasConflict !== '' ? currentRow.aliasConflict : '-'" readonly size="small" />
        </el-descriptions-item>
      </el-descriptions>

      <div class="detail-toolbar" v-if="detailJson">
        <el-radio-group v-model="detailViewMode" size="mini" @change="onDetailViewModeChange">
          <el-radio-button label="simple">精简 JSON</el-radio-button>
          <el-radio-button label="full">完整 JSON</el-radio-button>
        </el-radio-group>
        <el-button type="primary" size="mini" icon="el-icon-document-copy" @click="copyDetailJson">{{ detailViewMode === 'simple' ? '复制精简 JSON' : '复制完整 JSON' }}</el-button>
      </div>
      <div v-loading="detailLoading" element-loading-text="加载中..." class="monaco-wrapper">
        <div ref="detailMonacoRef" v-show="detailJson" class="monaco-container detail-monaco"></div>
      </div>
      <el-empty v-if="!detailLoading && !detailJson" description="无数据" />

      <span slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">关 闭</el-button>
      </span>
    </el-dialog>
  </section>
</template>

<script>
import { sksPageMixin } from 'sks-plugin-el-erp/lib/sks-page'
import { queryIntObjects, exportIntObjects, deployIntObjects } from '@/api/maxintobject'
import SavedQueryPanel from '@/views/components/SavedQueryPanel.vue'

export default {
  name: 'MaxIntObjectQuery',
  mixins: [sksPageMixin],
  components: {
    SavedQueryPanel
  },
  data() {
    return {
      formData: {
        intobjectname: '',
        description: '',
        usewith: '',
        queryonly: '',
        flatsupported: ''
      },
      // 导出
      exportLoading: false,
      exportDialogVisible: false,
      exportJson: '',
      exportTotal: 0,
      exportIgnoreDefVal: false,
      exportMonacoLoaded: false,
      exportEditor: null,
      _exportMonaco: null,
      // 导入
      importDialog: { visible: false, text: '', error: '', loading: false, summary: null, result: [] },
      importEditor: null,
      // 详情
      dialogVisible: false,
      currentRow: null,
      // detailViewMode: simple=ignoreDefVal:true 精简; full=ignoreDefVal:false 完整; 均为可直接导入的 {"integrationobjects":[...]}
      detailViewMode: 'simple',
      detailObjectSimple: null,
      detailObjectFull: null,
      detailJson: '',
      detailLoading: false,
      detailEditor: null
    }
  },
  watch: {
    exportDialogVisible(val) {
      if (!val) {
        this.disposeExportEditor()
      }
    },
    'importDialog.visible'(val) {
      if (!val) {
        this.disposeImportEditor()
      }
    },
    dialogVisible(val) {
      if (!val) {
        this.disposeDetailEditor()
      }
    }
  },
  methods: {
    initMainTableParam() {
      return {
        ownerName: 'maxintobject',
        uniqueId: 'maxintobject-list',
        sksAppName: 'page52',
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
            { prop: 'intobjectname', label: '结构名称', minWidth: 160 },
            { prop: 'description', label: '描述', minWidth: 220 },
            { prop: 'usewith', label: '支持', minWidth: 140 },
            { prop: 'queryonly', label: '仅查询', width: 90 },
            { prop: 'flatsupported', label: '平铺支持', width: 90 },
            { prop: 'loadqueryfromapp', label: '从应用加载查询', width: 120 },
            { prop: 'authapp', label: '应用程序', width: 110 },
            { prop: 'module', label: '模块', minWidth: 120 }
          ]),
        queryParamsColumnListEnable: false,
        queryParamsColumnList: []
      }
    },
    // === SQL 条件构建 ===
    escapeSql(v) {
      return String(v || '').replace(/'/g, "''")
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
      if (this.formData.intobjectname) {
        conds.push(this.likeCond('INTOBJECTNAME', this.formData.intobjectname))
      }
      if (this.formData.description) {
        conds.push(this.likeCond('DESCRIPTION', this.formData.description))
      }
      if (this.formData.usewith) {
        conds.push(this.likeCond('USEWITH', this.formData.usewith))
      }
      if (this.formData.queryonly !== '') {
        conds.push("QUERYONLY = '" + this.escapeSql(this.formData.queryonly) + "'")
      }
      if (this.formData.flatsupported !== '') {
        conds.push("FLATSUPPORTED = '" + this.escapeSql(this.formData.flatsupported) + "'")
      }
      return conds.length > 0 ? conds.join(' AND ') : '1=1'
    },
    getEffectiveWhere() {
      const formWhere = this.buildWhere()
      const customWhere = this.$refs.savedQuery ? this.$refs.savedQuery.getWhere() : ''
      const hasForm = formWhere && formWhere !== '1=1'
      if (hasForm && customWhere) {
        return '(' + formWhere + ') AND (' + customWhere + ')'
      }
      return customWhere || formWhere
    },
    handleWhereChange() {
      this.handleQuery()
    },
    // === 列表查询(sksPageMixin 契约, created/mounted 时自动调用) ===
    // 必须 resolve {data:{list,total}}, 异常也 resolve 空集, 避免 mixin 的 loading 不复位
    getMainListRemote(queryParams) {
      return queryIntObjects({
        where: this.getEffectiveWhere(),
        pageNum: queryParams.pageNum,
        pageSize: queryParams.pageSize
      }).then(res => {
        const data = res.data || res
        if (data.status === 'error') {
          this.$message.error(data.message || '查询失败')
          return {
            data: { list: [], total: 0 }
          }
        }
        // 服务端分页: {integrationobjects:[...], total, pageNum, pageSize}
        return {
          data: {
            list: data.integrationobjects || [],
            total: data.total || 0
          }
        }
      }).catch(err => {
        this.$message.error('查询失败: ' + (err.message || String(err)))
        return {
          data: { list: [], total: 0 }
        }
      })
    },
    handlePageChange(page, limit) {
      this.mainTable.queryParams.pageNum = page
      this.mainTable.queryParams.pageSize = limit
      this.getMainList()
    },
    resetForm() {
      this.formData = {
        intobjectname: '',
        description: '',
        usewith: '',
        queryonly: '',
        flatsupported: ''
      }
      this.handleQuery()
    },
    // === 导出 ===
    handleExport() {
      this.exportLoading = true
      this.exportJson = ''
      this.exportTotal = 0
      exportIntObjects({
        where: this.getEffectiveWhere(),
        ignoreDefVal: this.exportIgnoreDefVal
      }).then(res => {
        const data = res.data || res
        if (data.status === 'error') {
          this.$message.error(data.message || '导出失败')
          return
        }
        // 导出响应本身即为纯数据 {integrationobjects:[...]}, 可直接复制回导
        this.exportTotal = (data.integrationobjects || []).length
        this.exportJson = JSON.stringify(data, null, 2)
        this.exportDialogVisible = true
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
    // === 导入 ===
    openImportDialog() {
      this.importDialog = { visible: true, text: '', error: '', loading: false, summary: null, result: [] }
    },
    onImportDialogOpened() {
      this.$nextTick(() => {
        setTimeout(() => {
          this.initImportEditor()
        }, 200)
      })
    },
    initImportEditor() {
      if (!this.exportMonacoLoaded) {
        import(/* webpackChunkName: "monaco" */ 'monaco-editor').then(monaco => {
          this.exportMonacoLoaded = true
          this._exportMonaco = monaco
          this.createImportEditor()
        }).catch(err => {
          console.error('Monaco Editor 加载失败:', err)
        })
      } else {
        this.createImportEditor()
      }
    },
    createImportEditor() {
      const monaco = this._exportMonaco
      if (this.$refs.importMonacoRef && !this.importEditor) {
        this.importEditor = monaco.editor.create(this.$refs.importMonacoRef, {
          value: this.importDialog.text || '',
          language: 'json',
          readOnly: false,
          theme: 'vs',
          automaticLayout: true,
          minimap: { enabled: false },
          scrollBeyondLastLine: false,
          fontSize: 13,
          wordWrap: 'on',
          folding: true,
          lineNumbers: 'on',
          tabSize: 2,
          renderLineHighlight: 'none'
        })
      } else if (this.importEditor) {
        this.importEditor.setValue(this.importDialog.text || '')
      }
    },
    disposeImportEditor() {
      if (this.importEditor) {
        this.importEditor.dispose()
        this.importEditor = null
      }
    },
    submitImport() {
      const d = this.importDialog
      const text = (this.importEditor ? this.importEditor.getValue() : d.text || '').trim()
      if (!text) {
        d.error = '请粘贴 JSON 内容'
        return
      }
      let parsed
      try {
        parsed = JSON.parse(text)
      } catch (err) {
        d.error = 'JSON 解析失败: ' + err.message
        return
      }
      // 支持数组 / 导出格式 {integrationobjects:[...]} / 单个对象, 统一归一成纯数据包 {integrationobjects:[...]}
      let importData
      if (Array.isArray(parsed)) {
        importData = { integrationobjects: parsed }
      } else if (Array.isArray(parsed.integrationobjects)) {
        // 导出结果原样回导(允许携带 syncFlag 等顶层参数)
        importData = parsed
      } else {
        importData = { integrationobjects: [parsed] }
      }
      d.loading = true
      d.error = ''
      d.summary = null
      d.result = []
      deployIntObjects(importData).then(res => {
        const data = res.data || res
        if (data.status === 'error') {
          this.$message.error(data.message || '导入失败')
        } else {
          this.$message.success((data.message || '导入完成') + '：共 ' + data.summary.total + ' 条，成功 ' + data.summary.success + ' 条，失败 ' + data.summary.failed + ' 条')
          d.summary = data.summary
          d.result = data.result || []
          this.getMainList()
        }
      }).catch(err => {
        this.$message.error('导入失败: ' + (err.message || String(err)))
      }).finally(() => {
        d.loading = false
      })
    },
    // === 详情 ===
    handleRowClick(row) {
      this.currentRow = row
      this.detailObjectSimple = null
      this.detailObjectFull = null
      this.detailViewMode = 'simple'
      this.detailJson = ''
      this.detailLoading = true
      this.dialogVisible = true
      const whereClause = "INTOBJECTNAME = '" + this.escapeSql(row.intobjectname) + "'"
      // 并行获取: 精简(ignoreDefVal=true) + 完整(ignoreDefVal=false)
      Promise.all([
        exportIntObjects({ where: whereClause, ignoreDefVal: true }),
        exportIntObjects({ where: whereClause, ignoreDefVal: false })
      ]).then(([simpleRes, fullRes]) => {
        const simpleData = simpleRes.data || simpleRes
        const fullData = fullRes.data || fullRes
        if (simpleData && simpleData.status === 'error') {
          this.$message.error('获取精简详情失败: ' + (simpleData.message || '未知错误'))
        } else if (simpleData && (simpleData.integrationobjects || []).length > 0) {
          this.detailObjectSimple = simpleData.integrationobjects[0]
        }
        if (fullData && fullData.status === 'error') {
          this.$message.error('获取完整详情失败: ' + (fullData.message || '未知错误'))
        } else if (fullData && (fullData.integrationobjects || []).length > 0) {
          this.detailObjectFull = fullData.integrationobjects[0]
          // 完整数据字段最全, 头部描述以它为准
          this.currentRow = Object.assign({}, row, this.detailObjectFull)
        }
        this.detailJson = this.buildDetailJson()
        if (this.detailJson) {
          this.$nextTick(() => this.initDetailEditor())
        }
      }).catch(err => {
        this.$message.error('获取详情失败: ' + (err.message || String(err)))
      }).finally(() => {
        this.detailLoading = false
      })
    },
    // 按当前模式构建可直接导入的 {"integrationobjects":[...]} JSON
    buildDetailJson() {
    if(this.detailViewMode === 'simple'){
 return JSON.stringify(this.detailObjectSimple, null, 2) 
    }else{
      const obj =  this.detailObjectFull
      if (!obj) return ''
      return JSON.stringify({ integrationobjects: [obj] }, null, 2)
      }
    },
    onDetailViewModeChange() {
      this.detailJson = this.buildDetailJson()
      if (this.detailEditor) {
        this.detailEditor.setValue(this.detailJson)
      }
    },
    copyDetailJson() {
      if (this.detailJson) {
        this.copyToClipboard(this.detailJson, this.detailViewMode === 'simple' ? '精简JSON' : '完整JSON')
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
    // === 详情 Monaco Editor ===
    initDetailEditor() {
      if (!this.detailJson) return
      if (!this.exportMonacoLoaded) {
        import(/* webpackChunkName: "monaco" */ 'monaco-editor').then(monaco => {
          this.exportMonacoLoaded = true
          this._exportMonaco = monaco
          this.createDetailEditor()
        }).catch(err => {
          console.error('Monaco Editor 加载失败:', err)
        })
      } else {
        this.createDetailEditor()
      }
    },
    createDetailEditor() {
      const monaco = this._exportMonaco
      if (this.$refs.detailMonacoRef && !this.detailEditor) {
        this.detailEditor = monaco.editor.create(this.$refs.detailMonacoRef, {
          value: this.detailJson,
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
      } else if (this.detailEditor) {
        this.detailEditor.setValue(this.detailJson)
      }
    },
    disposeDetailEditor() {
      if (this.detailEditor) {
        this.detailEditor.dispose()
        this.detailEditor = null
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
    // 列表首次加载由 sksPageMixin 的 created/mounted 钩子自动触发, 这里只需绑定翻页回调
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
.json-toolbar {
  margin-bottom: 8px;
}
.monaco-wrapper {
  position: relative;
}
.monaco-container {
  height: 400px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}
.detail-desc {
  margin-bottom: 8px;
}
.detail-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 16px 0 8px;
}
.detail-monaco {
  height: 320px;
}
.import-monaco {
  height: 320px;
}
.import-summary {
  margin-top: 12px;
}
.import-summary p {
  margin: 0 0 8px 0;
}
</style>
