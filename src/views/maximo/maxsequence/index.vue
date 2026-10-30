<template>
  <section class="query-page">
    <el-card>
      <div class="page-header-row">
        <div>
          <h2>序列查询与诊断</h2>
          <p class="page-summary">查询 MAXSEQUENCE 序列预留定义，并按当前过滤条件诊断记录最大值与数据库序列当前值；记录最大值大于序列当前值或无法查询时判定为异常。以 "=" 开头精确匹配，支持 % 通配符模糊搜索。</p>
        </div>
        <div class="page-actions">
          <saved-query-panel ref="savedQuery" appname="MAXSEQUENCE" :default-where="buildWhere()" @whereChange="handleWhereChange" />
        </div>
      </div>

      <el-form :model="formData" ref="queryForm" :inline="true" label-width="70px" @submit.native.prevent>
        <el-form-item label="表名">
          <el-input v-model="formData.tbname" placeholder="=WORKORDER 或 %WO%" clearable style="width: 190px;" @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item label="列名">
          <el-input v-model="formData.name" placeholder="=WORKORDERID 或 %ID%" clearable style="width: 190px;" @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item label="序列名">
          <el-input v-model="formData.sequencename" placeholder="=WORKORDERSEQ 或 %SEQ%" clearable style="width: 200px;" @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item>
          <el-button type="cyan" icon="el-icon-search" size="mini" :loading="loading" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" size="mini" @click="resetForm">重置</el-button>
          <el-button type="primary" icon="el-icon-view" size="mini" :loading="diagLoading" @click="diagnoseAll">全部诊断</el-button>
          <el-button type="warning" icon="el-icon-view" size="mini" :loading="diagLoading" :disabled="tableSelection.length === 0" @click="diagnoseSelected">诊断</el-button>
          <span v-if="tableSelection.length" class="selected-tip">已选 {{ tableSelection.length }} 行</span>
        </el-form-item>
      </el-form>

      <div class="result-panel">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="主表" name="main">
            <SksTable
              width="100%"
              :showTableColumnButton="true"
              :showRefreshButton="false"
              :mainTable="mainTable"
              :visibleTop="true"
              :highlight-current-row="true"
              @refresh="fetchList"
              @selectionChangeAfter="handleTableSelectionChange">
              <template slot="tableColumnList-before">
                <el-table-column type="selection" width="45" align="center" />
              </template>
              <template slot="default">
              </template>
            </SksTable>
            <el-empty v-if="!loading && total === 0 && hasSearched" description="暂无查询结果" />
            <el-empty v-if="!loading && total === 0 && !hasSearched" description="暂无序列数据" />
          </el-tab-pane>
          <el-tab-pane :label="diagTabLabel" name="diag">
            <div class="diag-toolbar">
              <el-select v-model="diagStatus" placeholder="全部状态" clearable size="mini" style="width: 120px;">
                <el-option label="正常" value="NORMAL" />
                <el-option label="异常" value="ABNORMAL" />
                <el-option label="错误" value="ERROR" />
              </el-select>
              <el-input v-model="diagKeyword" placeholder="缓存过滤：表/列/序列/状态/值" clearable size="mini" style="width: 240px;" />
              <el-checkbox v-model="diagShowAllFields">显示所有字段</el-checkbox>
              <span class="diag-summary">
                共 {{ diagRows.length }} 条，正常 {{ normalCount }} 条，
                <span class="diag-abnormal">异常 {{ abnormalCount }} 条</span>，
                <span class="diag-error">错误 {{ errorCount }} 条</span>
              </span>
              <el-button type="danger" icon="el-icon-refresh-left" size="mini" :loading="repairing" :disabled="diagSelection.length === 0" @click="repairSequences">修复（{{ diagSelection.length }}）</el-button>
            </div>
            <el-table :data="diagFiltered" border stripe size="mini" max-height="560" v-loading="diagLoading" empty-text="请选择记录后点击“诊断”，或点击“全部诊断”" @selection-change="handleDiagSelectionChange">
              <el-table-column type="selection" width="45" align="center" />
              <el-table-column v-for="col in diagColumns" :key="col.prop" :prop="col.prop" :label="col.label" :width="col.width" show-overflow-tooltip />
              <el-table-column label="状态" width="100" align="center">
                <template slot-scope="scope">
                  <el-tag :type="statusTagType(scope.row.V_STATUS)" size="mini">{{ statusText(scope.row.V_STATUS) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="V_ERROR" label="错误信息" min-width="260" show-overflow-tooltip />
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-card>
  </section>
</template>

<script>
import { sksPageMixin } from "sks-plugin-el-erp/lib/sks-page";
import SavedQueryPanel from '@/views/components/SavedQueryPanel.vue'
import { getMaxSequenceList, diagnoseMaxSequence, repairMaxSequence } from '@/api/maxsequence'

export default {
  name: 'MaxSequence',
  mixins: [sksPageMixin],
  components: {
    SavedQueryPanel
  },
  data() {
    return {
      loading: false,
      diagLoading: false,
      hasSearched: false,
      total: 0,
      activeTab: 'main',
      formData: {
        tbname: '',
        name: '',
        sequencename: ''
      },
      tableSelection: [],
      diagRows: [],
      diagKeyword: '',
      diagStatus: '',
      diagSelection: [],
      repairing: false,
      diagShowAllFields: false
    }
  },
  computed: {
    diagTabLabel() {
      return this.diagRows.length ? '诊断（' + this.diagRows.length + '）' : '诊断'
    },
    diagFiltered() {
      let rows = this.diagRows
      if (this.diagStatus) {
        rows = rows.filter(row => row.V_STATUS === this.diagStatus)
      }
      const kw = this.diagKeyword.trim().toUpperCase()
      if (kw) {
        rows = rows.filter(row => {
          return ['MAXSEQUENCEID', 'TBNAME', 'NAME', 'SEQUENCENAME', 'MAXRESERVED', 'MAXVALUE', 'RANGE', 'ROWSTAMP',
            'V_MAXRECORD', 'V_CURRENTVALUE', 'V_STATUS', 'V_ERROR'].some(key => {
            const v = row[key]
            return v !== null && v !== undefined && String(v).toUpperCase().indexOf(kw) >= 0
          })
        })
      }
      return rows
    },
    abnormalCount() {
      return this.diagRows.filter(row => row.V_STATUS === 'ABNORMAL').length
    },
    errorCount() {
      return this.diagRows.filter(row => row.V_STATUS === 'ERROR').length
    },
    normalCount() {
      return this.diagRows.length - this.abnormalCount - this.errorCount
    },
    diagColumns() {
      const columns = [
        { prop: 'MAXSEQUENCEID', label: 'Maxsequence 标识', width: 140 },
        { prop: 'TBNAME', label: '表', width: 200 },
        { prop: 'NAME', label: '列', width: 200 },
        { prop: 'SEQUENCENAME', label: '序号', width: 220 }
      ]
      if (this.diagShowAllFields) {
        columns.push(
          { prop: 'MAXRESERVED', label: '最大预留值', width: 130 },
          { prop: 'MAXVALUE', label: '最大值', width: 130 },
          { prop: 'RANGE', label: '序列范围', width: 130 },
          { prop: 'ROWSTAMP', label: 'ROWSTAMP', width: 120 }
        )
      }
      columns.push(
        { prop: 'V_MAXRECORD', label: '记录最大值', width: 140 },
        { prop: 'V_CURRENTVALUE', label: '序列当前值', width: 140 }
      )
      return columns
    }
  },
  methods: {
    initMainTableParam() {
      return {
        ownerName: 'maxsequence',
        uniqueId: 'maxsequence-list',
        sksAppName: 'maxsequence',
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
            { prop: 'MAXSEQUENCEID', label: 'Maxsequence 标识', width: 140 },
            { prop: 'TBNAME', label: '表', width: 200 },
            { prop: 'NAME', label: '列', width: 200 },
            { prop: 'SEQUENCENAME', label: '序号', width: 220 },
            { prop: 'MAXRESERVED', label: '最大预留值', width: 130, visible: false },
            { prop: 'MAXVALUE', label: '最大值', width: 130, visible: false },
            { prop: 'RANGE', label: '序列范围', width: 130, visible: false },
            { prop: 'ROWSTAMP', label: 'ROWSTAMP', width: 120, visible: false }
          ]),
        queryParamsColumnListEnable: false,
        queryParamsColumnList: []
      }
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
      return "UPPER(" + field + ") LIKE UPPER('" + like + "')"
    },
    buildWhere() {
      const conds = []
      if (this.formData.tbname) {
        conds.push(this.likeCond('TBNAME', this.formData.tbname))
      }
      if (this.formData.name) {
        conds.push(this.likeCond('NAME', this.formData.name))
      }
      if (this.formData.sequencename) {
        conds.push(this.likeCond('SEQUENCENAME', this.formData.sequencename))
      }
      return conds.length > 0 ? conds.join(' AND ') : '1=1'
    },
    getCustomWhere() {
      return this.$refs.savedQuery ? this.$refs.savedQuery.getWhere() : ''
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
      const params = {
        tbname: this.formData.tbname,
        name: this.formData.name,
        sequencename: this.formData.sequencename,
        pageNum: this.mainTable.queryParams.pageNum,
        pageSize: this.mainTable.queryParams.pageSize
      }
      const customWhere = this.getCustomWhere()
      if (customWhere) {
        params.where = customWhere
      }
      getMaxSequenceList(params)
        .then(res => {
          if (res.code === 200 && res.data) {
            this.mainTable.list = res.data.rows || []
            this.mainTable.total = res.data.total || 0
            this.total = res.data.total || 0
          } else {
            this.mainTable.list = []
            this.mainTable.total = 0
            this.total = 0
            this.$message.error(res.message || '查询失败')
          }
        })
        .catch(err => {
          this.$message.error('请求失败: ' + (err.message || String(err)))
        })
        .finally(() => {
          this.loading = false
        })
    },
    handlePageChange(page, limit) {
      this.mainTable.queryParams.pageNum = page
      this.mainTable.queryParams.pageSize = limit
      this.fetchList()
    },
    handleTableSelectionChange(selection) {
      this.tableSelection = selection || []
    },
    handleDiagSelectionChange(selection) {
      this.diagSelection = selection || []
    },
    statusText(status) {
      if (status === 'NORMAL') return '正常'
      if (status === 'ABNORMAL') return '异常'
      if (status === 'ERROR') return '错误'
      return status || '-'
    },
    statusTagType(status) {
      if (status === 'NORMAL') return 'success'
      if (status === 'ABNORMAL') return 'danger'
      return 'warning'
    },
    repairSequences() {
      if (this.diagSelection.length === 0) {
        this.$message.warning('请先勾选需要修复的记录')
        return
      }
      const ids = this.diagSelection.map(row => row.MAXSEQUENCEID)
      this.$confirm('将对选中的 ' + ids.length + ' 条记录执行序列修复（RESTART WITH 记录最大值 + 1），是否继续？', '修复序列', {
        type: 'warning',
        confirmButtonText: '修 复',
        cancelButtonText: '取 消'
      }).then(() => {
        this.repairing = true
        repairMaxSequence({ ids: ids.join(',') })
          .then(res => {
            if (res.code === 200 && res.data) {
              const failed = res.data.filter(item => !item.SUCCESS)
              const success = res.data.length - failed.length
              if (failed.length > 0) {
                const html = failed.map(item => 'MAXSEQUENCEID ' + item.MAXSEQUENCEID + '：' + item.MESSAGE).join('<br/>')
                this.$alert(html, '部分修复失败', { dangerouslyUseHTMLString: true, type: 'error' })
              }
              this.$message.success('修复完成：成功 ' + success + ' 条，失败 ' + failed.length + ' 条')
              this.diagSelection = []
              this.refreshDiagRows(ids)
            } else {
              this.$message.error(res.message || '修复失败')
            }
          })
          .catch(err => {
            this.$message.error('修复失败: ' + (err.message || String(err)))
          })
          .finally(() => {
            this.repairing = false
          })
      }).catch(() => {})
    },
    refreshDiagRows(ids) {
      if (!ids || ids.length === 0) {
        return
      }
      diagnoseMaxSequence({ ids: ids.join(',') })
        .then(res => {
          if (res.code === 200 && res.data) {
            const map = {}
            res.data.forEach(row => {
              map[row.MAXSEQUENCEID] = row
            })
            this.diagRows = this.diagRows.map(row => map[row.MAXSEQUENCEID] || row)
          }
        })
        .catch(() => {})
    },
    diagnoseAll() {
      const params = {
        tbname: this.formData.tbname,
        name: this.formData.name,
        sequencename: this.formData.sequencename
      }
      const customWhere = this.getCustomWhere()
      if (customWhere) {
        params.where = customWhere
      }
      this.runDiagnose(params)
    },
    diagnoseSelected() {
      if (this.tableSelection.length === 0) {
        this.$message.warning('请先勾选需要诊断的记录')
        return
      }
      this.runDiagnose({
        ids: this.tableSelection.map(row => row.MAXSEQUENCEID).join(',')
      })
    },
    runDiagnose(params) {
      this.diagLoading = true
      diagnoseMaxSequence(params)
        .then(res => {
          if (res.code === 200 && res.data) {
            this.diagRows = res.data
            this.diagKeyword = ''
            this.diagStatus = ''
            this.diagSelection = []
            this.activeTab = 'diag'
            const abnormal = this.diagRows.filter(row => row.V_STATUS === 'ABNORMAL').length
            const errors = this.diagRows.filter(row => row.V_STATUS === 'ERROR').length
            if (abnormal > 0 || errors > 0) {
              this.$message.warning('诊断完成：共 ' + this.diagRows.length + ' 条，异常 ' + abnormal + ' 条，错误 ' + errors + ' 条')
            } else {
              this.$message.success('诊断完成：共 ' + this.diagRows.length + ' 条，全部正常')
            }
          } else {
            this.$message.error(res.message || '诊断失败')
          }
        })
        .catch(err => {
          this.$message.error('诊断失败: ' + (err.message || String(err)))
        })
        .finally(() => {
          this.diagLoading = false
        })
    },
    resetForm() {
      this.formData = {
        tbname: '',
        name: '',
        sequencename: ''
      }
      if (this.$refs.savedQuery) {
        this.$refs.savedQuery.setWhere('')
      }
      this.tableSelection = []
      this.diagRows = []
      this.diagKeyword = ''
      this.diagStatus = ''
      this.diagSelection = []
      this.activeTab = 'main'
      this.hasSearched = false
      this.mainTable.queryParams.pageNum = 1
      this.fetchList()
    }
  },
  mounted() {
    if (this.mainTable) {
      this.mainTable.onPageChange = (page, limit) => {
        this.handlePageChange(page, limit)
      }
    }
    this.fetchList()
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
  margin: 0;
  color: #909399;
  font-size: 12px;
  max-width: 760px;
  line-height: 1.5;
}
.page-actions {
  flex-shrink: 0;
}
.result-panel {
  margin-top: 8px;
}
.selected-tip {
  margin-left: 8px;
  color: #e6a23c;
  font-size: 12px;
}
.diag-toolbar {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
  gap: 12px;
}
.diag-summary {
  color: #606266;
  font-size: 12px;
}
.diag-abnormal {
  color: #f56c6c;
}
.diag-error {
  color: #e6a23c;
}
</style>
