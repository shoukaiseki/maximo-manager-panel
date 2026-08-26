<template>
  <section class="query-page">
    <el-card>
      <div class="page-header-row">
        <div>
          <h2>表数据统计</h2>
          <p class="page-summary">统计 Maximo 所有持久化实体表（排除视图）的数据行数。共 {{ total }} 张表。</p>
        </div>
      </div>

      <el-form :model="formData" :inline="true" label-width="90px" @submit.native.prevent>
        <el-form-item label="表名">
          <el-input v-model="formData.keyword" placeholder="输入表名关键词..." clearable style="width: 200px;" @keyup.enter.native="$refs.tableRef && $refs.tableRef.sort('OBJECTNAME')" />
        </el-form-item>
        <el-form-item label="排序方式">
          <el-select v-model="formData.sortField" style="width: 140px;" @change="handleSortChange">
            <el-option label="表名排序" value="OBJECTNAME" />
            <el-option label="数据行数排序" value="COUNT" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序方向">
          <el-select v-model="formData.sortOrder" style="width: 120px;" @change="handleSortChange">
            <el-option label="正序" value="ascending" />
            <el-option label="倒序" value="descending" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="cyan" icon="el-icon-search" size="mini" :loading="loading" @click="handleQuery">统计</el-button>
          <el-button type="primary" icon="el-icon-refresh" size="mini" :loading="loading" @click="handleRefresh">重新请求</el-button>
        </el-form-item>
        <el-form-item>
          <el-button type="success" icon="el-icon-document" size="mini" :disabled="selectedRows.length === 0" @click="generateSql">生成查询SQL</el-button>
        </el-form-item>
      </el-form>

      <div class="result-panel">
        <el-table ref="tableRef" :data="allRows" border stripe size="mini" v-loading="loading" element-loading-text="统计中..." height="calc(100vh - 320px)" @sort-change="handleNativeSort" @selection-change="handleSelectionChange" @row-click="handleRowClick">
          <el-table-column type="selection" width="55" align="center" />
          <el-table-column type="index" label="序号" width="55" align="center" />
          <el-table-column prop="OBJECTNAME" label="表名" min-width="200" show-overflow-tooltip sortable="custom" :sort-orders="['ascending','descending']" />
          <el-table-column prop="LZH_DESCRIPTION" label="中文描述" min-width="220" show-overflow-tooltip />
          <el-table-column prop="DESCRIPTION" label="英文描述" min-width="180" show-overflow-tooltip />
          <el-table-column prop="COUNT" label="数据行数" width="130" align="right" sortable="custom" :sort-orders="['ascending','descending']">
            <template slot-scope="scope">
              <el-tooltip v-if="scope.row.COUNT === null || scope.row.COUNT === undefined" content="统计失败（表不存在或无权限）" placement="top">
                <span class="count-fail">-</span>
              </el-tooltip>
              <span v-else class="count-num">{{ formatCount(scope.row.COUNT) }}</span>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!loading && allRows.length === 0" description="暂无表数据" />
      </div>

      <el-dialog title="表详情" :visible.sync="detailDialogVisible" width="50%" top="10vh" @close="disposeDetailSqlMonaco">
        <el-descriptions :column="2" border size="mini">
          <el-descriptions-item label="表名">{{ detailRow.OBJECTNAME }}</el-descriptions-item>
          <el-descriptions-item label="数据行数">{{ detailRow.COUNT === null || detailRow.COUNT === undefined ? '-' : formatCount(detailRow.COUNT) }}</el-descriptions-item>
          <el-descriptions-item label="中文描述" :span="2">{{ detailRow.LZH_DESCRIPTION }}</el-descriptions-item>
          <el-descriptions-item label="英文描述" :span="2">{{ detailRow.DESCRIPTION }}</el-descriptions-item>
        </el-descriptions>
        <div class="detail-sql-title">查询SQL</div>
        <div class="sql-monaco detail-sql-monaco" ref="detailSqlMonacoRef"></div>
        <div slot="footer">
          <el-button @click="detailDialogVisible = false">关闭</el-button>
          <el-button type="primary" icon="el-icon-document-copy" @click="copyDetailSql">复制SQL</el-button>
        </div>
      </el-dialog>

      <el-dialog title="生成查询SQL" :visible.sync="sqlDialogVisible" width="60%" top="8vh" @opened="initSqlMonaco" @close="disposeSqlMonaco">
        <div class="sql-monaco" ref="sqlMonacoRef"></div>
        <div slot="footer">
          <el-button @click="sqlDialogVisible = false">关闭</el-button>
          <el-button type="primary" icon="el-icon-document-copy" @click="copySql">复制SQL</el-button>
        </div>
      </el-dialog>
    </el-card>
  </section>
</template>

<script>
import { getTableStatsList } from '@/api/tablestats'

export default {
  name: 'TableStats',
  data() {
    return {
      loading: false,
      total: 0,
      rows: [],
      selectedRows: [],
      sqlDialogVisible: false,
      sqlText: '',
      sqlMonacoLoaded: false,
      _monaco: null,
      sqlEditor: null,
      detailDialogVisible: false,
      detailRow: {},
      detailSQLEditor: null,
      formData: {
        keyword: '',
        sortField: 'OBJECTNAME',
        sortOrder: 'ascending'
      }
    }
  },
  computed: {
    allRows() {
      // 本地排序（切换排序方式/方向不重新请求后端）
      const rows = this.rows.slice()
      const field = this.formData.sortField
      const dir = this.formData.sortOrder === 'descending' ? -1 : 1
      rows.sort((a, b) => {
        let va = a[field]
        let vb = b[field]
        if (va === null || va === undefined) va = ''
        if (vb === null || vb === undefined) vb = ''
        let cmp = 0
        if (va === '' || vb === '') {
          cmp = (va === '' ? 1 : 0) - (vb === '' ? 1 : 0)
        } else if (typeof va === 'string' || typeof vb === 'string') {
          cmp = String(va).localeCompare(String(vb))
        } else {
          cmp = va - vb
        }
        return cmp * dir
      })
      const kw = (this.formData.keyword || '').trim().toUpperCase()
      if (!kw) {
        return rows
      }
      return rows.filter(r => {
        return String(r.OBJECTNAME || '').toUpperCase().indexOf(kw) >= 0 ||
          String(r.LZH_DESCRIPTION || '').toUpperCase().indexOf(kw) >= 0 ||
          String(r.DESCRIPTION || '').toUpperCase().indexOf(kw) >= 0
      })
    }
  },
  methods: {
    handleQuery() {
      if (!this.rows.length) {
        this.fetchList()
      } else if (this.$refs.tableRef) {
        // 已有统计结果：本地重排即可
        this.$refs.tableRef.sort(this.formData.sortField, this.formData.sortOrder)
      }
    },
    handleSelectionChange(selectedRows) {
      this.selectedRows = selectedRows
    },
    handleRowClick(row, column, event) {
      // 点击勾选框不触发详情
      if (event && event.target && event.target.closest && event.target.closest('.el-checkbox')) return
      if (row === this.detailRow && this.detailDialogVisible) return
      this.detailRow = row
      if (this.detailSQLEditor) {
        this.detailSQLEditor.dispose()
        this.detailSQLEditor = null
      }
      this.detailDialogVisible = true
      this.$nextTick(() => this.initDetailSqlMonaco())
    },
    generateSql() {
      if (!this.selectedRows.length) {
        this.$message.warning('请先勾选要生成查询SQL的表')
        return
      }
      this.sqlText = this.selectedRows
        .map(r => `select * from ${r.OBJECTNAME};`)
        .join('\n')
      this.sqlDialogVisible = true
    },
    copySql() {
      const textarea = document.createElement('textarea')
      textarea.value = this.sqlText
      document.body.appendChild(textarea)
      textarea.select()
      try {
        document.execCommand('copy')
        this.$message.success('SQL 已复制到剪贴板')
      } catch (e) {
        this.$message.error('复制失败，请手动复制')
      } finally {
        document.body.removeChild(textarea)
      }
    },
    copyDetailSql() {
      const sql = `select * from ${this.detailRow.OBJECTNAME};`
      const textarea = document.createElement('textarea')
      textarea.value = sql
      document.body.appendChild(textarea)
      textarea.select()
      try {
        document.execCommand('copy')
        this.$message.success('SQL 已复制到剪贴板')
      } catch (e) {
        this.$message.error('复制失败，请手动复制')
      } finally {
        document.body.removeChild(textarea)
      }
    },
    detailSql() {
      return `select * from ${this.detailRow.OBJECTNAME};`
    },
    // === Monaco Editor ===
    initDetailSqlMonaco() {
      if (!this.$refs.detailSqlMonacoRef) return
      if (!this.sqlMonacoLoaded) {
        import(/* webpackChunkName: "monaco" */ 'monaco-editor').then(monaco => {
          this.sqlMonacoLoaded = true
          this._monaco = monaco
          this.createDetailSqlEditor()
        }).catch(err => {
          console.error('Monaco Editor 加载失败:', err)
        })
      } else if (!this.detailSQLEditor) {
        this.createDetailSqlEditor()
      } else {
        this.detailSQLEditor.setValue(this.detailSql())
      }
    },
    createDetailSqlEditor() {
      if (!this.$refs.detailSqlMonacoRef || this.detailSQLEditor) return
      this.detailSQLEditor = this._monaco.editor.create(this.$refs.detailSqlMonacoRef, {
        value: this.detailSql(),
        language: 'sql',
        readOnly: true,
        theme: 'vs',
        automaticLayout: true,
        minimap: { enabled: false },
        scrollBeyondLastLine: false,
        fontSize: 13,
        wordWrap: 'on',
        folding: true,
        lineNumbers: 'on',
        renderLineHighlight: 'line'
      })
    },
    // === Monaco Editor ===
    initSqlMonaco() {
      if (!this.$refs.sqlMonacoRef) return
      if (!this.sqlMonacoLoaded) {
        import(/* webpackChunkName: "monaco" */ 'monaco-editor').then(monaco => {
          this.sqlMonacoLoaded = true
          this._monaco = monaco
          this.createSqlEditor()
        }).catch(err => {
          console.error('Monaco Editor 加载失败:', err)
        })
      } else if (!this.sqlEditor) {
        this.createSqlEditor()
      } else {
        this.sqlEditor.setValue(this.sqlText)
      }
    },
    createSqlEditor() {
      if (!this.$refs.sqlMonacoRef || this.sqlEditor) return
      this.sqlEditor = this._monaco.editor.create(this.$refs.sqlMonacoRef, {
        value: this.sqlText,
        language: 'sql',
        readOnly: true,
        theme: 'vs',
        automaticLayout: true,
        minimap: { enabled: false },
        scrollBeyondLastLine: false,
        fontSize: 13,
        wordWrap: 'on',
        folding: true,
        lineNumbers: 'on',
        renderLineHighlight: 'line'
      })
    },
    disposeSqlMonaco() {
      if (this.sqlEditor) {
        this.sqlEditor.dispose()
        this.sqlEditor = null
      }
    },
    disposeDetailSqlMonaco() {
      if (this.detailSQLEditor) {
        this.detailSQLEditor.dispose()
        this.detailSQLEditor = null
      }
    },
    handleRefresh() {
      // 强制重新请求后端，获取最新统计信息（忽略本地缓存）
      this.rows = []
      this.fetchList()
    },
    handleSortChange() {
      // 排序方式/方向变化：仅本地重排（数据已含 COUNT），无需重新请求
      if (this.$refs.tableRef) {
        const column = this.formData.sortField === 'COUNT' ? 'COUNT' : 'OBJECTNAME'
        this.$refs.tableRef.sort(column, this.formData.sortOrder)
      }
    },
    handleNativeSort({ prop, order }) {
      // 点击表头排序：同步到下拉框（保持界面一致）
      if (prop === 'OBJECTNAME' || prop === 'COUNT') {
        this.formData.sortField = prop
      }
      if (order === 'ascending' || order === 'descending') {
        this.formData.sortOrder = order
      }
    },
    fetchList() {
      if (this.rows.length > 0) {
        // 已有统计结果则直接使用（本地排序），不重复请求
        return
      }
      this.loading = true
      getTableStatsList()
        .then(res => {
          if (res.code === 200 && Array.isArray(res.data)) {
            this.rows = res.data
            this.total = res.data.length
          } else {
            this.rows = []
            this.total = 0
            this.$message.error(res.message || '查询失败')
          }
        })
        .catch(err => {
          this.rows = []
          this.total = 0
          this.$message.error('请求失败: ' + (err.message || String(err)))
        })
        .finally(() => {
          this.loading = false
        })
    },
    formatCount(count) {
      return Number(count).toLocaleString()
    }
  },
  mounted() {
    this.fetchList()
  }
}
</script>

<style lang="scss">
.query-page {
  padding: 16px;
}
.page-header-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}
.page-summary {
  color: #606266;
  margin: 0;
  font-size: 13px;
}
.result-panel {
  margin-top: 20px;
}
.count-num {
  font-weight: 600;
  color: #409eff;
}
.count-fail {
  color: #f56c6c;
}
.sql-monaco {
  width: 100%;
  height: 50vh;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}
.detail-sql-title {
  margin-top: 16px;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}
.detail-sql-monaco {
  height: 30vh;
}
</style>