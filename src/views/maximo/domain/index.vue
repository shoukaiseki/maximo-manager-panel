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
    <max-domain-info-dialog :visible.sync="dialogVisible" :domainid="currentRow && currentRow.domainid" :domaintype="currentRow && currentRow.domaintype" />
  </section>
</template>

<script>
import { sksPageMixin } from "sks-plugin-el-erp/lib/sks-page";
import { exportDomains } from '@/api/domain'
import SavedQueryPanel from '@/views/components/SavedQueryPanel.vue'
import MaxDomainInfoDialog from '@/views/maximo/domain/MaxDomainInfoDialog.vue'

export default {
  name: 'DomainQuery',
  mixins: [sksPageMixin],
  components: {
    SavedQueryPanel,
    MaxDomainInfoDialog
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
      this.dialogVisible = true
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
.trans-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.trans-cell-row {
  display: flex;
  align-items: center;
  line-height: 20px;
}
.trans-cell-lang {
  flex: 0 0 60px;
  color: #909399;
  font-size: 12px;
  padding-right: 8px;
}
.trans-cell-desc {
  flex: 1;
  color: #303133;
  word-break: break-all;
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
