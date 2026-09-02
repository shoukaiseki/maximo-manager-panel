<template>
  <section class="query-page">
    <el-card>
      <div class="page-header-row">
        <div>
          <h2>条件表达式管理器</h2>
          <p class="page-summary">查询 CONDITION 条件定义（TYPE: CLASS=条件类 / EXPRESSION=表达式）。以 "=" 开头精确匹配，支持 % 通配符模糊搜索。</p>
        </div>
        <div class="page-actions">
          <saved-query-panel ref="savedQuery" appname="CONDITION" :default-where="buildWhere()" @whereChange="handleWhereChange" />
          <el-button type="success" icon="el-icon-upload2" size="mini" style="margin-left: 8px;" @click="openImportDialog">导入</el-button>
          <el-button type="warning" icon="el-icon-download" size="mini" style="margin-left: 8px;" :loading="exportLoading" @click="handleExport">导出</el-button>
        </div>
      </div>

      <el-form :model="formData" ref="queryForm" :inline="true" label-width="70px" @submit.native.prevent>
        <el-form-item label="条件名称">
          <el-input v-model="formData.conditionnum" placeholder="如 =COND1 或 %COND%" clearable style="width: 200px;" @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="formData.description" placeholder="输入描述关键词..." clearable style="width: 200px;" @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="formData.type" placeholder="全部" clearable style="width: 160px;">
            <el-option label="CLASS(条件类)" value="CLASS" />
            <el-option label="EXPRESSION(表达式)" value="EXPRESSION" />
          </el-select>
        </el-form-item>
        <el-form-item label="表达式">
          <el-input v-model="formData.expression" placeholder="输入表达式关键词..." clearable style="width: 200px;" @keyup.enter.native="handleQuery" />
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
    <el-dialog title="导出条件定义" :visible.sync="exportDialogVisible" width="1000px" top="3vh" :close-on-click-modal="true" @opened="onExportDialogOpened">
      <div class="json-toolbar">
        <span style="float:left;color:#606266;line-height:32px;">共导出 {{ exportTotal }} 个条件</span>
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
    <el-dialog title="导入条件定义" :visible.sync="importDialog.visible" width="800px" top="3vh" :close-on-click-modal="true">
      <p style="margin:0 0 8px;color:#909399;font-size:12px;">粘贴 JSON（数组 或 {"conditions": [...]}），支持精简模式导出结果直接导入；按 conditionnum 匹配更新或创建；描述支持 en_description / zh_description 多语言。</p>
      <el-input v-model="importDialog.text" type="textarea" :rows="12" placeholder='[{"conditionnum":"COND1","type":"EXPRESSION","expression":"status = '"'"'APPR'"'"'","description":"示例条件"}]' />
      <p style="margin:8px 0 0;color:#f56c6c;font-size:12px" v-if="importDialog.error">{{ importDialog.error }}</p>
      <div v-if="importDialog.summary" class="import-summary">
        <p>导入完成：共 {{ importDialog.summary.total }} 条，成功 {{ importDialog.summary.success }} 条，失败 {{ importDialog.summary.failed }} 条</p>
        <el-table :data="importDialog.result" border stripe size="mini" max-height="260" style="width: 100%">
          <el-table-column prop="conditionnum" label="条件名称" min-width="140" show-overflow-tooltip />
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

    <!-- 条件详情弹窗 -->
    <el-dialog :title="'条件详情 - ' + (currentRow ? currentRow.conditionnum : '')" :visible.sync="dialogVisible" width="700px" top="3vh" :close-on-click-modal="true">
      <el-descriptions :column="2" border v-if="currentRow" class="detail-desc" size="small">
        <el-descriptions-item label="条件名称">
          <el-input :value="currentRow.conditionnum || '-'" readonly size="small" />
        </el-descriptions-item>
        <el-descriptions-item label="类型">
          <el-input :value="currentRow.type || '-'" readonly size="small" />
        </el-descriptions-item>
        <el-descriptions-item label="描述">
          <el-input :value="currentRow.description || '-'" readonly size="small" />
        </el-descriptions-item>
        <el-descriptions-item label="中文描述">
          <el-input :value="currentRow.zh_description || '-'" readonly size="small" />
        </el-descriptions-item>
        <el-descriptions-item label="条件类" :span="2">
          <el-input :value="currentRow.classname || '-'" readonly size="small" />
        </el-descriptions-item>
        <el-descriptions-item label="总是判定">
          <el-input :value="currentRow.nocaching !== undefined && currentRow.nocaching !== null ? currentRow.nocaching : '-'" readonly size="small" />
        </el-descriptions-item>
        <el-descriptions-item label="唯一标识">
          <el-input :value="currentRow.conditionid || '-'" readonly size="small" />
        </el-descriptions-item>
        <el-descriptions-item label="表达式" :span="2">
          <el-input type="textarea" :value="currentRow.expression || '-'" readonly :autosize="{ minRows: 3, maxRows: 10 }" />
        </el-descriptions-item>
      </el-descriptions>

      <span slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">关 闭</el-button>
      </span>
    </el-dialog>
  </section>
</template>

<script>
import { sksPageMixin } from "sks-plugin-el-erp/lib/sks-page";
import { queryConditions, exportConditions, deployConditions } from '@/api/condition'
import SavedQueryPanel from '@/views/components/SavedQueryPanel.vue'

export default {
  name: 'ConditionQuery',
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
        conditionnum: '',
        description: '',
        type: '',
        expression: ''
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
      // 详情
      dialogVisible: false,
      currentRow: null
    }
  },
  watch: {
    exportDialogVisible(val) {
      if (!val) {
        this.disposeExportEditor()
      }
    }
  },
  methods: {
    initMainTableParam() {
      return {
        ownerName: 'condition',
        uniqueId: 'condition-list',
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
            { prop: 'conditionnum', label: '条件名称', minWidth: 140 },
            { prop: 'description', label: '描述', minWidth: 220 },
            { prop: 'type', label: '类型', width: 110 },
            { prop: 'expression', label: '表达式', minWidth: 260, showOverflowTooltip: true },
            { prop: 'classname', label: '条件类', minWidth: 160 },
            { prop: 'nocaching', label: '总是判定', width: 90 }
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
      if (this.formData.conditionnum) {
        conds.push(this.likeCond('c.CONDITIONNUM', this.formData.conditionnum))
      }
      if (this.formData.description) {
        conds.push(this.likeCond('c.DESCRIPTION', this.formData.description))
      }
      if (this.formData.type) {
        conds.push("c.TYPE = '" + this.escapeSql(this.formData.type.trim()) + "'")
      }
      if (this.formData.expression) {
        conds.push(this.likeCond('c.EXPRESSION', this.formData.expression))
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
      return customWhere ? customWhere : formWhere
    },
    handleWhereChange() {
      this.hasSearched = true
      this.mainTable.queryParams.pageNum = 1
      this.fetchList()
    },
    // === 列表查询 ===
    handleQuery() {
      this.hasSearched = true
      this.mainTable.queryParams.pageNum = 1
      this.fetchList()
    },
    fetchList() {
      this.loading = true
      queryConditions({
        _langcode: 'ZH',
        apiType: 'manage',
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
          this.mainTable.list = data.conditions || []
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
        conditionnum: '',
        description: '',
        type: '',
        expression: ''
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
      exportConditions({
        _langcode: 'ZH',
        apiType: 'manage',
        ignoreDefVal: this.exportIgnoreDefVal ? 'true' : 'false'
      }, {
        where: this.getEffectiveWhere()
      }).then(res => {
        const data = res.data || res
        if (data.status === 'error') {
          this.$message.error(data.message || '导出失败')
          return
        }
        this.exportTotal = (data.conditions || []).length
        this.exportJson = JSON.stringify({ conditions: data.conditions || [] }, null, 2)
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
    submitImport() {
      const d = this.importDialog
      const text = (d.text || '').trim()
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
      d.loading = true
      d.error = ''
      d.summary = null
      d.result = []
      deployConditions({
        _langcode: 'ZH',
        apiType: 'manage'
      }, parsed).then(res => {
        const data = res.data || res
        if (data.status === 'error') {
          this.$message.error(data.message || '导入失败')
        } else {
          this.$message.success((data.message || '导入完成') + '：共 ' + data.summary.total + ' 条，成功 ' + data.summary.success + ' 条，失败 ' + data.summary.failed + ' 条')
          d.summary = data.summary
          d.result = data.result || []
          this.hasSearched = true
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
      this.currentRow = null
      this.dialogVisible = true
      const whereClause = "c.CONDITIONNUM = '" + this.escapeSql(row.conditionnum) + "'"
      exportConditions({
        _langcode: 'ZH',
        apiType: 'manage',
        ignoreDefVal: 'false'
      }, {
        where: whereClause
      }).then(res => {
        const data = res.data || res
        if (data.status === 'error') {
          this.$message.error(data.message || '获取详情失败')
          return
        }
        const list = data.conditions || []
        if (list.length > 0) {
          this.currentRow = Object.assign({}, row, list[0])
        } else {
          this.currentRow = row
        }
      }).catch(err => {
        this.$message.error('获取详情失败: ' + (err.message || String(err)))
      })
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
.detail-desc {
  margin-bottom: 16px;
}
.import-summary {
  margin-top: 12px;
}
.import-summary p {
  margin: 0 0 8px 0;
}
</style>