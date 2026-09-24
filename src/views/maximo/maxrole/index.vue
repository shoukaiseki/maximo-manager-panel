<template>
  <section class="query-page">
    <el-card>
      <div class="page-header-row">
        <div>
          <h2>角色管理</h2>
          <p class="page-summary">查询 MAXROLE 角色定义（人员/人员组/数据集等类型），供工作流分配引用。以 "=" 开头精确匹配，支持 % 通配符模糊搜索。</p>
        </div>
        <div class="page-actions">
          <saved-query-panel ref="savedQuery" appname="MAXROLE" :default-where="buildWhere()" @whereChange="handleWhereChange" />
          <el-button type="success" icon="el-icon-upload2" size="mini" style="margin-left: 8px;" @click="openImportDialog">导入</el-button>
          <el-button type="warning" icon="el-icon-download" size="mini" style="margin-left: 8px;" @click="handleExport">导出</el-button>
        </div>
      </div>

      <el-form :model="formData" ref="queryForm" :inline="true" label-width="70px" @submit.native.prevent>
        <el-form-item label="角色">
          <el-input v-model="formData.maxrole" placeholder="=1017 或 %SUP%" clearable style="width: 180px;" @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="formData.type" placeholder="全部" clearable filterable style="width: 150px;" @change="handleQuery">
            <el-option v-for="t in typeOptions" :key="t.value" :label="t.label" :value="t.value" />
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

    <!-- 角色详情弹窗 -->
    <el-dialog :title="'角色详情 - ' + (currentRow ? currentRow.maxrole : '')" :visible.sync="dialogVisible" width="860px" top="5vh" :close-on-click-modal="true">
      <el-tabs v-model="detailTab" @tab-click="onDetailTabClick">
        <el-tab-pane label="基本信息" name="basic">
          <div v-loading="detailLoading">
            <el-descriptions v-if="detail" :column="2" border size="small">
              <el-descriptions-item label="角色">{{ detail.maxrole }}</el-descriptions-item>
              <el-descriptions-item label="类型">{{ typeLabel(detail.type) }}</el-descriptions-item>
              <el-descriptions-item label="描述" :span="2">{{ detail.description || '-' }}</el-descriptions-item>
              <el-descriptions-item label="值">{{ detail.value || '-' }}</el-descriptions-item>
              <el-descriptions-item label="对象">{{ detail.objectName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="参数">{{ detail.parameter || '-' }}</el-descriptions-item>
              <el-descriptions-item label="邮件数据集">{{ boolText(detail.emailDataSet) }}</el-descriptions-item>
              <el-descriptions-item label="广播">{{ boolText(detail.broadcast) }}</el-descriptions-item>
              <el-descriptions-item label="发送方系统">{{ detail.senderSysId || '-' }}</el-descriptions-item>
            </el-descriptions>
            <el-empty v-else-if="!detailLoading" description="未加载到详情" :image-size="60" />
          </div>
        </el-tab-pane>
        <el-tab-pane label="导出 JSON" name="json">
          <div v-loading="detailLoading" element-loading-text="加载中...">
            <div class="detail-toolbar" v-if="detailJson">
              <el-radio-group v-model="detailJsonMode" size="mini" @change="onDetailJsonModeChange">
                <el-radio-button label="single">单条导出</el-radio-button>
                <el-radio-button label="cond">按当前条件导出</el-radio-button>
              </el-radio-group>
              <el-button type="primary" size="mini" icon="el-icon-document-copy" @click="copyDetailJson">{{ detailJsonMode === 'single' ? '复制单条JSON' : '复制条件导出JSON' }}</el-button>
            </div>
            <div ref="detailMonacoRef" v-show="detailJson" class="monaco-container detail-monaco"></div>
            <el-empty v-if="!detailLoading && !detailJson" description="无数据" :image-size="60" />
          </div>
        </el-tab-pane>
      </el-tabs>
      <span slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">关 闭</el-button>
      </span>
    </el-dialog>

    <!-- 导出结果弹窗 -->
    <el-dialog title="导出角色" :visible.sync="exportDialog.visible" width="1000px" top="3vh" :close-on-click-modal="true" @opened="onExportDialogOpened">
      <el-tabs v-model="exportDialog.tab" @tab-click="onExportTabClick">
        <el-tab-pane v-for="t in exportDialog.tabs" :key="t.name" :label="t.label" :name="t.name"></el-tab-pane>
      </el-tabs>
      <div class="json-toolbar">
        <span style="float:left;color:#606266;line-height:32px;">{{ currentExportTab.label }}：共导出 {{ currentExportTab.total }} 个角色</span>
        <el-button type="primary" size="mini" icon="el-icon-document-copy" @click="copyExportJson">复制当前导出JSON</el-button>
      </div>
      <div v-loading="exportDialog.loading" element-loading-text="导出中..." class="monaco-wrapper">
        <div ref="exportMonacoRef" class="monaco-container"></div>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="exportDialog.visible = false">关 闭</el-button>
      </span>
    </el-dialog>

    <!-- 导入 JSON 弹窗 -->
    <el-dialog title="导入角色" :visible.sync="importDialog.visible" width="820px" top="3vh" :close-on-click-modal="false" @opened="onImportDialogOpened">
      <p style="margin:0 0 8px;color:#909399;font-size:12px;">粘贴 JSON（数组 或 {"maxroles": [...]}），支持导出结果直接导入；按 maxrole 名称匹配更新或创建。</p>
      <div class="import-opts">
        <span class="import-opts-label">导入模式</span>
        <el-select v-model="importDialog.impMode" size="mini" style="width: 260px;">
          <el-option label="迁移模式（存在则修改，缺省）" value="migration" />
          <el-option label="新增模式（已存在的不修改）" value="add" />
        </el-select>
      </div>
      <div v-loading="importDialog.loading" element-loading-text="导入中..." class="monaco-wrapper">
        <div ref="importMonacoRef" class="monaco-container import-monaco"></div>
      </div>
      <p style="margin:8px 0 0;color:#f56c6c;font-size:12px" v-if="importDialog.error">{{ importDialog.error }}</p>
      <div v-if="importDialog.summary" class="import-summary">
        <p>导入完成：共 {{ importDialog.summary.total }} 条，成功 {{ importDialog.summary.success }} 条，失败 {{ importDialog.summary.failed }} 条</p>
        <el-table :data="importDialog.result" border stripe size="mini" max-height="260" style="width: 100%">
          <el-table-column prop="maxrole" label="角色" min-width="160" show-overflow-tooltip />
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
  </section>
</template>

<script>
import { sksPageMixin } from "sks-plugin-el-erp/lib/sks-page";
import SavedQueryPanel from '@/views/components/SavedQueryPanel.vue'
import { maxRoleList, maxRoleDetail, maxRoleExport, maxRoleImport } from '@/api/wfdesign'

export default {
  name: 'MaxRole',
  mixins: [sksPageMixin],
  components: {
    SavedQueryPanel
  },
  data() {
    return {
      loading: false,
      detailLoading: false,
      hasSearched: false,
      total: 0,
      formData: {
        maxrole: '',
        type: '',
        description: ''
      },
      typeOptions: [
        { value: 'PERSONGROUP', label: '人员组' },
        { value: 'PERSON', label: '人员' },
        { value: 'DATASET', label: '数据集' },
        { value: 'USERDATA', label: '用户数据集' },
        { value: 'EMAILADDRESS', label: '邮件地址' },
        { value: 'CUSTOM', label: '自定义类' }
      ],
      currentRow: null,
      detail: null,
      dialogVisible: false,
      detailTab: 'basic',
      detailJsonMode: 'single',
      detailJson: '',
      detailJsonSingle: '',
      detailJsonCond: '',
      detailEditor: null,
      exportDialog: {
        visible: false,
        loading: false,
        tab: 'cond',
        tabs: [
          { name: 'cond', label: '按当前条件', json: '', total: 0 },
          { name: 'all', label: '全部角色', json: '', total: 0 }
        ]
      },
      exportEditor: null,
      importDialog: { visible: false, text: '', error: '', loading: false, summary: null, result: [], impMode: 'migration' },
      importEditor: null,
      monacoLoaded: false,
      _monaco: null
    }
  },
  computed: {
    currentExportTab() {
      var hit = this.exportDialog.tabs.find(x => x.name === this.exportDialog.tab)
      return hit || this.exportDialog.tabs[0]
    }
  },
  watch: {
    exportDialog: {
      deep: true,
      handler(val) {
        if (!val.visible) {
          this.disposeExportEditor()
        }
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
        this.detailJson = ''
      }
    }
  },
  methods: {
    initMainTableParam() {
      return {
        ownerName: 'maxrole',
        uniqueId: 'maxrole-list',
        sksAppName: 'maxrole',
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
            { prop: 'maxrole', label: '角色', minWidth: 140 },
            { prop: 'typeText', label: '类型', width: 120 },
            { prop: 'value', label: '值', minWidth: 140 },
            { prop: 'objectName', label: '对象', width: 140 },
            { prop: 'description', label: '描述', minWidth: 220 }
          ]),
        queryParamsColumnListEnable: false,
        queryParamsColumnList: []
      }
    },
    typeLabel(t) {
      var hit = this.typeOptions.find(x => x.value === t)
      return hit ? hit.label + ' (' + t + ')' : (t || '-')
    },
    boolText(v) {
      return v === true ? '是' : '否'
    },
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
      if (this.formData.maxrole) {
        conds.push(this.likeCond('MAXROLE', this.formData.maxrole))
      }
      if (this.formData.type) {
        conds.push("TYPE = '" + this.escapeSql(this.formData.type) + "'")
      }
      if (this.formData.description) {
        conds.push("UPPER(DESCRIPTION) LIKE UPPER('%" + this.escapeSql(this.formData.description.trim()) + "%')")
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
    handleQuery() {
      this.hasSearched = true
      this.mainTable.queryParams.pageNum = 1
      this.fetchList()
    },
    fetchList() {
      this.loading = true
      maxRoleList({
        where: this.getEffectiveWhere(),
        pageNum: this.mainTable.queryParams.pageNum,
        pageSize: this.mainTable.queryParams.pageSize
      }).then(res => {
        const data = res.data || res
        if (data.status === 'error') {
          this.mainTable.list = []
          this.mainTable.total = 0
          this.total = 0
          this.$message.error(data.message || '查询失败')
        } else {
          const rows = data.maxroles || []
          rows.forEach(r => {
            r.typeText = this.typeLabel(r.type)
          })
          this.mainTable.list = rows
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
      this.formData = { maxrole: '', type: '', description: '' }
      this.hasSearched = false
      this.mainTable.list = []
      this.mainTable.total = 0
      this.total = 0
      this.mainTable.currentPage = 1
    },
    // === 导出(两个参数组合: 按当前条件 / 全部) ===
    handleExport() {
      this.exportDialog.visible = true
      this.exportDialog.loading = true
      this.exportDialog.tab = 'cond'
      this.exportDialog.tabs.forEach(t => {
        t.json = ''
        t.total = 0
      })
      const where = this.getEffectiveWhere()
      const condBody = where && where !== '1=1' ? { where: where } : {}
      Promise.all([
        maxRoleExport(condBody),
        maxRoleExport({})
      ]).then(([condRes, allRes]) => {
        const condData = condRes.data || condRes
        const allData = allRes.data || allRes
        this.fillExportTab('cond', condData)
        this.fillExportTab('all', allData)
        this.onExportTabClick()
      }).catch(err => {
        this.$message.error('导出失败: ' + (err.message || String(err)))
      }).finally(() => {
        this.exportDialog.loading = false
      })
    },
    fillExportTab(name, data) {
      const tab = this.exportDialog.tabs.find(t => t.name === name)
      if (!tab) return
      if (data && data.status === 'error') {
        this.$message.error('导出失败: ' + (data.message || '未知错误'))
        return
      }
      tab.total = (data && (data.maxroles || []).length) || 0
      tab.json = JSON.stringify(data || { maxroles: [] }, null, 2)
    },
    onExportDialogOpened() {
      this.$nextTick(() => {
        setTimeout(() => {
          this.initExportEditor()
        }, 200)
      })
    },
    onExportTabClick() {
      if (!this.currentExportTab.json) return
      this.$nextTick(() => {
        if (this.exportEditor) {
          this.exportEditor.setValue(this.currentExportTab.json)
        } else {
          this.initExportEditor()
        }
      })
    },
    copyExportJson() {
      if (this.currentExportTab.json) {
        this.copyToClipboard(this.currentExportTab.json, '导出JSON')
      }
    },
    // === 导入 ===
    openImportDialog() {
      this.importDialog = { visible: true, text: '', error: '', loading: false, summary: null, result: [], impMode: 'migration' }
    },
    onImportDialogOpened() {
      this.$nextTick(() => {
        setTimeout(() => {
          this.initImportEditor()
        }, 200)
      })
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
      let importData
      if (Array.isArray(parsed)) {
        importData = { maxroles: parsed }
      } else if (Array.isArray(parsed.maxroles)) {
        importData = parsed
      } else {
        importData = { maxroles: [parsed] }
      }
      d.loading = true
      d.error = ''
      d.summary = null
      d.result = []
      maxRoleImport(importData, { _impMode: d.impMode }).then(res => {
        const data = res.data || res
        if (data.status === 'error') {
          this.$message.error(data.message || '导入失败')
        } else {
          this.$message.success((data.message || '导入完成') + '：共 ' + data.summary.total + ' 条，成功 ' + data.summary.success + ' 条，失败 ' + data.summary.failed + ' 条')
          d.summary = data.summary
          d.result = data.result || []
          this.fetchList()
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
      this.detail = null
      this.detailJson = ''
      this.detailJsonSingle = ''
      this.detailJsonCond = ''
      this.detailTab = 'basic'
      this.detailJsonMode = 'single'
      this.detailLoading = true
      this.dialogVisible = true
      const where = this.getEffectiveWhere()
      const condBody = where && where !== '1=1' ? { where: where } : {}
      Promise.all([
        maxRoleDetail({ id: row.id }),
        maxRoleExport(condBody)
      ]).then(([detailRes, condRes]) => {
        const detailData = detailRes.data || detailRes
        const condData = condRes.data || condRes
        if (detailData && detailData.status === 'error') {
          this.$message.error('获取详情失败: ' + (detailData.message || '未知错误'))
        } else {
          this.detail = (detailData.maxroles || [])[0] || null
          this.detailJsonSingle = JSON.stringify(detailData || { maxroles: [] }, null, 2)
        }
        if (condData && condData.status === 'error') {
          this.$message.error('获取条件导出失败: ' + (condData.message || '未知错误'))
        } else {
          this.detailJsonCond = JSON.stringify(condData || { maxroles: [] }, null, 2)
        }
        this.detailJson = this.detailJsonSingle
        if (this.detailJson) {
          this.$nextTick(() => this.initDetailEditor())
        }
      }).catch(err => {
        this.$message.error('获取详情失败: ' + (err.message || String(err)))
      }).finally(() => {
        this.detailLoading = false
      })
    },
    onDetailTabClick(tab) {
      if (tab && tab.name === 'json') {
        this.$nextTick(() => {
          setTimeout(() => {
            if (this.detailJson) {
              this.initDetailEditor()
            }
          }, 200)
        })
      }
    },
    onDetailJsonModeChange() {
      this.detailJson = this.detailJsonMode === 'single' ? this.detailJsonSingle : this.detailJsonCond
      if (this.detailEditor) {
        this.detailEditor.setValue(this.detailJson)
      }
    },
    copyDetailJson() {
      if (this.detailJson) {
        this.copyToClipboard(this.detailJson, this.detailJsonMode === 'single' ? '单条JSON' : '条件导出JSON')
      }
    },
    // === Monaco Editor(懒加载共享实例) ===
    initExportEditor() {
      if (!this.currentExportTab.json) return
      if (!this.monacoLoaded) {
        import(/* webpackChunkName: "monaco" */ 'monaco-editor').then(monaco => {
          this.monacoLoaded = true
          this._monaco = monaco
          this.createExportEditor()
        }).catch(err => {
          console.error('Monaco Editor 加载失败:', err)
        })
      } else {
        this.createExportEditor()
      }
    },
    createExportEditor() {
      const monaco = this._monaco
      if (this.$refs.exportMonacoRef && !this.exportEditor) {
        this.exportEditor = monaco.editor.create(this.$refs.exportMonacoRef, {
          value: this.currentExportTab.json,
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
        this.exportEditor.setValue(this.currentExportTab.json)
      }
    },
    disposeExportEditor() {
      if (this.exportEditor) {
        this.exportEditor.dispose()
        this.exportEditor = null
      }
    },
    initDetailEditor() {
      if (!this.detailJson) return
      if (!this.monacoLoaded) {
        import(/* webpackChunkName: "monaco" */ 'monaco-editor').then(monaco => {
          this.monacoLoaded = true
          this._monaco = monaco
          this.createDetailEditor()
        }).catch(err => {
          console.error('Monaco Editor 加载失败:', err)
        })
      } else {
        this.createDetailEditor()
      }
    },
    createDetailEditor() {
      const monaco = this._monaco
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
    initImportEditor() {
      if (!this.monacoLoaded) {
        import(/* webpackChunkName: "monaco" */ 'monaco-editor').then(monaco => {
          this.monacoLoaded = true
          this._monaco = monaco
          this.createImportEditor()
        }).catch(err => {
          console.error('Monaco Editor 加载失败:', err)
        })
      } else {
        this.createImportEditor()
      }
    },
    createImportEditor() {
      const monaco = this._monaco
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
.detail-monaco {
  height: 420px;
}
.detail-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 0 0 8px;
}
.import-monaco {
  height: 320px;
}
.import-opts {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}
.import-opts-label {
  color: #606266;
  font-size: 13px;
  margin-right: 8px;
}
.import-summary {
  margin-top: 12px;
}
.import-summary p {
  margin: 0 0 8px 0;
}
</style>
