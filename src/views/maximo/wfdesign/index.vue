<template>
  <section class="query-page">
    <el-card>
      <div class="page-header-row">
        <div>
          <h2>工作流查询</h2>
          <p class="page-summary">查询 WFPROCESS 工作流定义。以 "=" 开头精确匹配，支持 % 通配符模糊搜索；点击行查看流程图与明细。</p>
        </div>
        <div class="page-actions">
          <saved-query-panel ref="savedQuery" appname="WFPROCESS" :default-where="buildWhere()" @whereChange="handleWhereChange" />
          <el-button type="success" icon="el-icon-upload2" size="mini" style="margin-left: 8px;" @click="openImportDialog">导入</el-button>
          <el-button type="warning" icon="el-icon-download" size="mini" style="margin-left: 8px;" :loading="exportDialog.loading" @click="handleExport">导出</el-button>
        </div>
      </div>

      <el-form :model="formData" ref="queryForm" :inline="true" label-width="80px" @submit.native.prevent>
        <el-form-item label="流程名称">
          <el-input v-model="formData.processName" placeholder="=PRCHG 或 %WF%" clearable style="width: 200px;" @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item label="主对象">
          <el-input v-model="formData.objectName" placeholder="=WORKORDER 或 %WO%" clearable style="width: 180px;" @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="formData.description" placeholder="输入描述关键词..." clearable style="width: 200px;" @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item label="启用">
          <el-select v-model="formData.enabled" placeholder="全部" clearable style="width: 100px;" @change="handleQuery">
            <el-option label="已启用" value="1" />
            <el-option label="未启用" value="0" />
          </el-select>
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
          <template slot="none-enabled" slot-scope="{ row }">
            <el-tag :type="row.enabled === true ? 'success' : 'info'" size="mini">
              {{ row.enabled === true ? '已启用' : '未启用' }}
            </el-tag>
          </template>
          <template slot="none-active" slot-scope="{ row }">
            <el-tag :type="row.active === true ? '' : 'info'" size="mini">
              {{ row.active === true ? '已激活' : '未激活' }}
            </el-tag>
          </template>
          <template slot="tableColumnList-after">
            <el-table-column label="操作" width="80" align="center" fixed="right">
              <template slot-scope="scope">
                <el-button type="text" size="mini" icon="el-icon-download" @click.stop="handleRowExport(scope.row)">导出</el-button>
              </template>
            </el-table-column>
          </template>
        </SksTable>
        <el-empty v-if="!loading && total === 0 && hasSearched" description="暂无查询结果" />
        <el-empty v-if="!loading && total === 0 && !hasSearched" description="请输入关键词后点击搜索" />
      </div>
    </el-card>

    <!-- 导出结果弹窗 -->
    <el-dialog title="导出工作流" :visible.sync="exportDialog.visible" width="1000px" top="3vh" :close-on-click-modal="true" @opened="onExportDialogOpened">
      <p style="margin:0 0 8px;color:#909399;font-size:12px;" v-if="exportDialog.scopeLabel">导出范围：{{ exportDialog.scopeLabel }}</p>
      <el-tabs v-model="exportDialog.tab" @tab-click="onExportTabClick">
        <el-tab-pane v-for="t in exportDialog.tabs" :key="t.name" :label="t.label" :name="t.name"></el-tab-pane>
      </el-tabs>
      <div class="json-toolbar">
        <span style="float:left;color:#606266;line-height:32px;">{{ currentExportTab.label }}：共导出 {{ currentExportTab.total }} 个工作流</span>
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
    <el-dialog title="导入工作流" :visible.sync="importDialog.visible" width="840px" top="3vh" :close-on-click-modal="true" @opened="onImportDialogOpened">
      <p style="margin:0 0 8px;color:#909399;font-size:12px;">粘贴 JSON（数组 或 {"workflows": [...]}，含 actions/maxroles 的迁移包也可直接导入）；按 processName+processRev 匹配更新或创建。</p>
      <div class="import-opts">
        <span class="import-opts-label">导入模式</span>
        <el-select v-model="importDialog.impMode" size="mini" style="width: 300px;">
          <el-option label="新增模式（已存在的不修改）" value="add" />
          <el-option label="迁移模式（存在则修改，缺省）" value="migration" />
        </el-select>
      </div>
      <div class="import-opts">
        <el-checkbox v-model="importDialog.enable">导入后启用并激活（校验不通过则保持草稿）</el-checkbox>
        <el-checkbox v-model="importDialog.syncFlag">全量同步（删除 JSON 中不存在的子记录，仅迁移模式生效）</el-checkbox>
      </div>
      <div v-loading="importDialog.loading" element-loading-text="导入中..." class="monaco-wrapper">
        <div ref="importMonacoRef" class="monaco-container import-monaco"></div>
      </div>
      <p style="margin:8px 0 0;color:#f56c6c;font-size:12px" v-if="importDialog.error">{{ importDialog.error }}</p>
      <div v-if="importDialog.summary" class="import-summary">
        <p>导入完成：共 {{ importDialog.summary.total }} 条，成功 {{ importDialog.summary.success }} 条，失败 {{ importDialog.summary.failed }} 条</p>
        <el-table :data="importDialog.result" border stripe size="mini" max-height="260" style="width: 100%">
          <el-table-column prop="processName" label="流程名称" min-width="180" show-overflow-tooltip />
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
import { workflowList, workflowExport, workflowImport } from '@/api/wfdesign'

export default {
  name: 'WfDesign',
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
        processName: '',
        objectName: '',
        description: '',
        enabled: ''
      },
      // 导出: 两个参数组合 tab(仅流程定义 _refs=false / 含引用操作角色 _refs=true)
      exportDialog: {
        visible: false,
        loading: false,
        tab: 'refs',
        scopeLabel: '',
        body: null,
        tabs: [
          { name: 'refs', label: '含引用操作/角色', json: '', total: 0 },
          { name: 'pure', label: '仅流程定义', json: '', total: 0 }
        ]
      },
      exportEditor: null,
      // 导入
      importDialog: { visible: false, text: '', error: '', loading: false, summary: null, result: [], impMode: 'add', enable: true, syncFlag: false },
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
    }
  },
  methods: {
    initMainTableParam() {
      return {
        ownerName: 'wfprocess',
        uniqueId: 'wfprocess-list',
        sksAppName: 'wfdesign',
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
            { prop: 'processName', label: '流程名称', minWidth: 180 },
            { prop: 'processRev', label: '版本', width: 70 },
            { prop: 'objectName', label: '主对象', width: 140 },
            { prop: 'description', label: '描述', minWidth: 220 },
            { prop: 'enabled', label: '启用', width: 90, align: 'center', htmlType: 'none' },
            { prop: 'active', label: '激活', width: 90, align: 'center', htmlType: 'none' }
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
      if (this.formData.processName) {
        conds.push(this.likeCond('PROCESSNAME', this.formData.processName))
      }
      if (this.formData.objectName) {
        conds.push(this.likeCond('OBJECTNAME', this.formData.objectName))
      }
      if (this.formData.description) {
        conds.push("UPPER(DESCRIPTION) LIKE UPPER('%" + this.escapeSql(this.formData.description.trim()) + "%')")
      }
      if (this.formData.enabled === '1') {
        conds.push('ENABLED = 1')
      } else if (this.formData.enabled === '0') {
        conds.push('ENABLED = 0')
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
    // === 列表查询 ===
    handleQuery() {
      this.hasSearched = true
      this.mainTable.queryParams.pageNum = 1
      this.fetchList()
    },
    fetchList() {
      this.loading = true
      workflowList({
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
          const rows = data.workflows || []
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
      this.formData = {
        processName: '',
        objectName: '',
        description: '',
        enabled: ''
      }
      this.hasSearched = false
      this.mainTable.list = []
      this.mainTable.total = 0
      this.total = 0
      this.mainTable.currentPage = 1
    },
    handleRowClick(row) {
      this.$router.push({
        name: 'WfDesignDetail',
        params: { processName: row.processName, processRev: String(row.processRev) }
      })
    },
    // === 导出(两个参数组合: 含引用 _refs=true / 仅流程 _refs=false) ===
    // 页面导出按当前条件, 行导出按 processName+processRev
    handleExport() {
      const where = this.getEffectiveWhere()
      this.openExportDialog({
        where: where && where !== '1=1' ? where : ''
      }, where && where !== '1=1' ? '当前查询条件' : '全部工作流')
    },
    handleRowExport(row) {
      this.openExportDialog({
        processName: row.processName,
        processRev: row.processRev
      }, row.processName + ' (Rev ' + row.processRev + ')')
    },
    openExportDialog(body, scopeLabel) {
      this.exportDialog.visible = true
      this.exportDialog.loading = true
      this.exportDialog.tab = 'refs'
      this.exportDialog.scopeLabel = scopeLabel || ''
      this.exportDialog.body = body
      this.exportDialog.tabs.forEach(t => {
        t.json = ''
        t.total = 0
      })
      Promise.all([
        workflowExport(body, { _refs: true }),
        workflowExport(body, { _refs: false })
      ]).then(([refsRes, pureRes]) => {
        const refsData = refsRes.data || refsRes
        const pureData = pureRes.data || pureRes
        this.fillExportTab('refs', refsData)
        this.fillExportTab('pure', pureData)
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
      tab.total = (data && (data.workflows || []).length) || 0
      tab.json = JSON.stringify(data || { workflows: [] }, null, 2)
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
      this.importDialog = { visible: true, text: '', error: '', loading: false, summary: null, result: [], impMode: 'add', enable: true, syncFlag: false }
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
      // 支持数组 / 导出原样(含 actions/maxroles 的迁移包) / 单对象, 统一归一成纯数据包
      let importData
      if (Array.isArray(parsed)) {
        importData = { workflows: parsed }
      } else if (Array.isArray(parsed.workflows) || Array.isArray(parsed.actions) || Array.isArray(parsed.maxroles)) {
        importData = parsed
      } else {
        importData = { workflows: [parsed] }
      }
      if (d.syncFlag) {
        importData.syncFlag = true
      }
      d.loading = true
      d.error = ''
      d.summary = null
      d.result = []
      workflowImport(importData, { _impMode: d.impMode, _enable: d.enable }).then(res => {
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
