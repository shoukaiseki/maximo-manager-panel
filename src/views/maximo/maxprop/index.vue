<template>
  <section class="query-page">
    <el-card>
      <div class="page-header-row">
        <div>
          <h2>属性查询</h2>
          <p class="page-summary">查询 MAXPROP/MAXPROPVALUE 属性定义。以 "=" 开头精确匹配，支持 % 通配符模糊搜索。</p>
        </div>
        <div class="page-actions">
          <saved-query-panel ref="savedQuery" appname="MAXPROP" :default-where="buildWhere()" @whereChange="handleWhereChange" />
          <el-button type="success" icon="el-icon-upload2" size="mini" style="margin-left: 8px;" @click="openImportDialog">导入</el-button>
          <el-button type="warning" icon="el-icon-download" size="mini" style="margin-left: 8px;" :loading="exportLoading" @click="handleExport">导出</el-button>
        </div>
      </div>

      <el-form :model="formData" ref="queryForm" :inline="true" label-width="70px" @submit.native.prevent>
        <el-form-item label="属性名称">
          <el-input v-model="formData.propname" placeholder="如 =mx.sysprop 或 %sysprop%" clearable style="width: 200px;" @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="formData.description" placeholder="输入描述关键词..." clearable style="width: 200px;" @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item label="值">
          <el-input v-model="formData.propvalue" placeholder="MAXPROPVALUE.PROPVALUE" clearable style="width: 200px;" @keyup.enter.native="handleQuery" />
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
    <el-dialog title="导出属性定义" :visible.sync="exportDialogVisible" width="1000px" top="3vh" :close-on-click-modal="true" @opened="onExportDialogOpened">
      <div class="json-toolbar">
        <span style="float:left;color:#606266;line-height:32px;">共导出 {{ exportTotal }} 个属性</span>
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
    <el-dialog title="导入属性定义" :visible.sync="importDialog.visible" width="800px" top="3vh" :close-on-click-modal="true">
      <p style="margin:0 0 8px;color:#909399;font-size:12px;">粘贴 JSON（数组 或 {"maxprops": [...]}），支持精简模式导出结果直接导入；子记录复制：属性会自动更新或创建，MAXPROPVALUE 按 服务器+服务器主机 匹配更新。</p>
      <el-input v-model="importDialog.text" type="textarea" :rows="12" placeholder='[{"propname":"mx.sysprop","description":"系统属性","maxpropvalue":[{"servername":"SERVER1","propvalue":"1"}]}]' />
      <p style="margin:8px 0 0;color:#f56c6c;font-size:12px" v-if="importDialog.error">{{ importDialog.error }}</p>
      <div v-if="importDialog.summary" class="import-summary">
        <p>导入完成：共 {{ importDialog.summary.total }} 条，成功 {{ importDialog.summary.success }} 条，失败 {{ importDialog.summary.failed }} 条</p>
        <el-table :data="importDialog.result" border stripe size="mini" max-height="260" style="width: 100%">
          <el-table-column prop="propname" label="属性名称" min-width="200" show-overflow-tooltip />
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

    <!-- 属性详情弹窗 -->
    <el-dialog :title="'属性详情 - ' + (currentRow ? currentRow.propname : '')" :visible.sync="dialogVisible" width="1200px" top="3vh" :close-on-click-modal="true">
      <el-descriptions :column="4" border v-if="currentRow" class="detail-desc" size="small">
        <el-descriptions-item label="属性名称">
          <el-input :value="currentRow.propname || '-'" readonly size="small" />
        </el-descriptions-item>
        <el-descriptions-item label="描述">
          <el-input :value="currentRow.description || '-'" readonly size="small" />
        </el-descriptions-item>
        <el-descriptions-item label="数据类型">
          <el-input :value="currentRow.maxtype || '-'" readonly size="small" />
        </el-descriptions-item>
        <el-descriptions-item label="Maximo 缺省值">
          <el-input :value="currentRow.maximodefault || '-'" readonly size="small" />
        </el-descriptions-item>
        <el-descriptions-item label="域">
          <el-input :value="currentRow.domainid || '-'" readonly size="small" />
        </el-descriptions-item>
        <el-descriptions-item label="值规则">
          <el-input :value="currentRow.valuerules || '-'" readonly size="small" />
        </el-descriptions-item>
        <el-descriptions-item label="安全性级别">
          <el-input :value="currentRow.securelevel || '-'" readonly size="small" />
        </el-descriptions-item>
        <el-descriptions-item label="访问类型">
          <el-input :value="currentRow.accesstype !== undefined && currentRow.accesstype !== null ? currentRow.accesstype : '-'" readonly size="small" />
        </el-descriptions-item>
        <el-descriptions-item label="仅全局">
          <el-input :value="currentRow.globalonly !== undefined && currentRow.globalonly !== null ? currentRow.globalonly : '-'" readonly size="small" />
        </el-descriptions-item>
        <el-descriptions-item label="仅实例">
          <el-input :value="currentRow.instanceonly !== undefined && currentRow.instanceonly !== null ? currentRow.instanceonly : '-'" readonly size="small" />
        </el-descriptions-item>
        <el-descriptions-item label="实时刷新">
          <el-input :value="currentRow.liverefresh !== undefined && currentRow.liverefresh !== null ? currentRow.liverefresh : '-'" readonly size="small" />
        </el-descriptions-item>
        <el-descriptions-item label="已加密">
          <el-input :value="currentRow.encrypted !== undefined && currentRow.encrypted !== null ? currentRow.encrypted : '-'" readonly size="small" />
        </el-descriptions-item>
        <el-descriptions-item label="允许空值">
          <el-input :value="currentRow.nullsallowed !== undefined && currentRow.nullsallowed !== null ? currentRow.nullsallowed : '-'" readonly size="small" />
        </el-descriptions-item>
        <el-descriptions-item label="用户定义">
          <el-input :value="currentRow.userdefined !== undefined && currentRow.userdefined !== null ? currentRow.userdefined : '-'" readonly size="small" />
        </el-descriptions-item>
        <el-descriptions-item label="允许联机变更">
          <el-input :value="currentRow.onlinechanges !== undefined && currentRow.onlinechanges !== null ? currentRow.onlinechanges : '-'" readonly size="small" />
        </el-descriptions-item>
        <el-descriptions-item label="被屏蔽">
          <el-input :value="currentRow.masked !== undefined && currentRow.masked !== null ? currentRow.masked : '-'" readonly size="small" />
        </el-descriptions-item>
        <el-descriptions-item label="变更者">
          <el-input :value="currentRow.changeby || '-'" readonly size="small" />
        </el-descriptions-item>
        <el-descriptions-item label="变更日期">
          <el-input :value="currentRow.changedate || '-'" readonly size="small" />
        </el-descriptions-item>
      </el-descriptions>

      <div class="subtable-header">
        <span class="subtable-tip">MAXPROPVALUE 属性值列表</span>
      </div>
      <el-table :data="detailValues" border stripe size="small" v-loading="detailLoading" max-height="420" style="width: 100%">
        <el-table-column prop="servername" label="服务器" min-width="140" show-overflow-tooltip />
        <el-table-column prop="serverhost" label="服务器主机" min-width="140" show-overflow-tooltip />
        <el-table-column prop="propvalue" label="值" min-width="160" show-overflow-tooltip />
        <el-table-column prop="encryptedvalue" label="已加密的值" min-width="160" show-overflow-tooltip />
        <el-table-column prop="accesstype" label="访问类型" width="90" />
        <el-table-column prop="changeby" label="变更者" min-width="120" show-overflow-tooltip />
        <el-table-column prop="changedate" label="变更日期" min-width="150" show-overflow-tooltip />
      </el-table>
      <el-empty v-if="!detailLoading && detailValues.length === 0" description="无属性值数据" />

      <span slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">关 闭</el-button>
      </span>
    </el-dialog>
  </section>
</template>

<script>
import { sksPageMixin } from "sks-plugin-el-erp/lib/sks-page";
import { queryMaxProps, exportMaxProps, deployMaxProps } from '@/api/maxprop'
import SavedQueryPanel from '@/views/components/SavedQueryPanel.vue'

export default {
  name: 'MaxPropQuery',
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
        propname: '',
        description: '',
        propvalue: ''
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
      detailLoading: false,
      currentRow: null,
      detailValues: []
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
        ownerName: 'maxprop',
        uniqueId: 'maxprop-list',
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
            { prop: 'propname', label: '属性名称', minWidth: 180 },
            { prop: 'description', label: '描述', minWidth: 200 },
            { prop: 'pv_propvalue', label: '值', minWidth: 140 },
            { prop: 'maxtype', label: '数据类型', width: 100 },
            { prop: 'maximodefault', label: 'Maximo 缺省值', minWidth: 140 },
            { prop: 'domainid', label: '域', width: 110 },
            { prop: 'securelevel', label: '安全性级别', width: 110 },
            { prop: 'pv_servername', label: '服务器', minWidth: 140, visible: false },
            { prop: 'pv_serverhost', label: '服务器主机', minWidth: 140, visible: false }
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
      if (this.formData.propname) {
        conds.push(this.likeCond('MAXPROP.PROPNAME', this.formData.propname))
      }
      if (this.formData.description) {
        conds.push(this.likeCond('MAXPROP.DESCRIPTION', this.formData.description))
      }
      if (this.formData.propvalue) {
        conds.push(this.likeCond('v.PROPVALUE', this.formData.propvalue))
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
      queryMaxProps({
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
          this.mainTable.list = data.props || []
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
        propname: '',
        description: '',
        propvalue: ''
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
      exportMaxProps({
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
        this.exportTotal = (data.maxprops || []).length
        this.exportJson = JSON.stringify({ maxprops: data.maxprops || [] }, null, 2)
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
      deployMaxProps({
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
      this.currentRow = row
      this.detailValues = []
      this.detailLoading = true
      this.dialogVisible = true
      const whereClause = "MAXPROP.PROPNAME = '" + this.escapeSql(row.propname) + "'"
      exportMaxProps({
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
        const list = data.maxprops || []
        if (list.length > 0) {
          this.currentRow = Object.assign({}, row, list[0])
          this.detailValues = list[0].maxpropvalue || []
        }
      }).catch(err => {
        this.$message.error('获取详情失败: ' + (err.message || String(err)))
      }).finally(() => {
        this.detailLoading = false
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
.import-summary {
  margin-top: 12px;
}
.import-summary p {
  margin: 0 0 8px 0;
}
</style>