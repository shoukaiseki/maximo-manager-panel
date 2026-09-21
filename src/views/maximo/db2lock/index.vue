<template>
  <section class="query-page">
    <el-card>
      <div class="page-header-row">
        <div>
          <h2>DB2 锁表查询</h2>
          <p class="page-summary">查询 DB2 中被锁住的表，支持按表名过滤。选中行后可生成 force 进程 SQL。</p>
        </div>
      </div>

      <el-form :model="formData" ref="queryForm" :inline="true" label-width="90px" @submit.native.prevent>
        <el-form-item>
          <el-button icon="el-icon-question" size="mini" circle title="LOCK_MODE 锁说明" @click="helpDialogVisible = true"></el-button>
        </el-form-item>
        <el-form-item label="表名">
          <el-input v-model="formData.tabName" placeholder="=精确/%模糊" clearable style="width: 220px;" @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item>
          <el-button type="cyan" icon="el-icon-search" size="mini" :loading="loading" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" size="mini" @click="resetForm">重置</el-button>
          <el-button type="danger" icon="el-icon-close" size="mini" :disabled="tableSelection.length === 0" @click="showForceSql">生成force进程SQL</el-button>
          <span v-if="tableSelection.length > 0" style="margin-left: 8px; color: #606266; font-size: 13px;">已选 {{ tableSelection.length }} 行</span>
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
          @refresh="fetchList"
          @selectionChangeAfter="handleTableSelectionChange">
          <template slot="tableColumnList-before">
            <!-- 不加 reserve-selection: 刷新/搜索后勾选自动清除(参考插件示例 Page40) -->
            <el-table-column type="selection" width="45" align="center" />
          </template>
          <template slot="default">
          </template>
        </SksTable>
        <el-empty v-if="!loading && total === 0 && hasSearched" description="暂无查询结果" />
        <el-empty v-if="!loading && total === 0 && !hasSearched" description="请输入关键词后点击搜索" />
      </div>
    </el-card>

    <!-- 锁详情弹窗 -->
    <el-dialog :title="'锁详情 - AGENT_ID: ' + (detailRow ? detailRow.AGENT_ID : '')" :visible.sync="detailDialog.visible" width="1100px" :close-on-click-modal="true">
      <el-descriptions :column="3" border v-if="detailRow">
        <el-descriptions-item label="目标进程">{{ detailRow.AGENT_ID }}</el-descriptions-item>
        <el-descriptions-item label="事务开始时间">{{ formatDate(detailRow.UOW_START_TIME) }}</el-descriptions-item>
        <el-descriptions-item label="应用名">{{ detailRow.APPL_NAME || '-' }}</el-descriptions-item>
        <el-descriptions-item label="认证ID">{{ detailRow.AUTHID || '-' }}</el-descriptions-item>
        <el-descriptions-item label="表模式">{{ detailRow.TABSCHEMA || '-' }}</el-descriptions-item>
        <el-descriptions-item label="表名">{{ detailRow.TABNAME || '-' }}</el-descriptions-item>
        <el-descriptions-item label="锁对象类型">{{ detailRow.LOCK_OBJECT_TYPE || '-' }}</el-descriptions-item>
        <el-descriptions-item label="锁模式">{{ detailRow.LOCK_MODE || '-' }}</el-descriptions-item>
        <el-descriptions-item label="锁状态">{{ detailRow.LOCK_STATUS || '-' }}</el-descriptions-item>
        <el-descriptions-item label="表空间">{{ detailRow.TBSP_NAME || '-' }}</el-descriptions-item>
        <el-descriptions-item label="锁名称">{{ detailRow.LOCK_NAME || '-' }}</el-descriptions-item>
        <el-descriptions-item label="工作单元状态">{{ detailRow.UOW_STATE || '-' }}</el-descriptions-item>
        <el-descriptions-item label="提交次数">{{ detailRow.TOTAL_APP_COMMITS }}</el-descriptions-item>
        <el-descriptions-item label="回滚次数">{{ detailRow.TOTAL_APP_ROLLBACKS }}</el-descriptions-item>
        <el-descriptions-item label="应用状态">{{ detailRow.APPL_STATUS || '-' }}</el-descriptions-item>
        <el-descriptions-item label="客户端IP">{{ detailRow.CLIENT_IP || '-' }}</el-descriptions-item>
        <el-descriptions-item label="客户端产品ID">{{ detailRow.CLIENT_PRDID || '-' }}</el-descriptions-item>
        <el-descriptions-item label="快照时间">{{ formatDate(detailRow.SNAPSHOT_TIMESTAMP) }}</el-descriptions-item>
      </el-descriptions>
      <div style="margin-top: 16px;" v-if="detailRow">
        <el-button type="primary" icon="el-icon-view" size="mini" :loading="diagnoseLoading" @click="runDiagnose">诊断</el-button>
        <el-button type="danger" icon="el-icon-close" size="mini" @click="generateSingleForceSql">生成force进程SQL</el-button>
        <el-button v-if="diagnoseText" icon="el-icon-document-copy" size="mini" @click="copyDiagnoseReport">复制诊断报告</el-button>
        <span v-if="diagnoseText" style="margin-left: 8px; color: #909399; font-size: 12px;">
          诊断报告（只读，支持滚动与 Ctrl+F 查找）
        </span>
      </div>

      <!-- 诊断报告 Monaco 显示区 -->
      <div v-if="diagnoseLoading || diagnoseText" style="margin-top: 10px;">
        <div ref="diagnoseMonacoRef" class="diagnose-monaco-container"></div>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="detailDialog.visible = false">关 闭</el-button>
      </span>
    </el-dialog>

    <!-- 生成的 force 进程 SQL 弹窗 -->
    <el-dialog title="force进程SQL" :visible.sync="sqlDialog.visible" width="800px" :close-on-click-modal="true" @opened="onSqlDialogOpened">
      <div class="json-toolbar" style="margin-bottom: 8px;">
        <span style="float:left;color:#606266;line-height:32px;">共 {{ sqlDialog.count }} 条语句</span>
        <el-button type="primary" size="mini" icon="el-icon-document-copy" @click="copyForceSql">复制SQL</el-button>
      </div>
      <div ref="sqlMonacoRef" class="monaco-container"></div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="sqlDialog.visible = false">关 闭</el-button>
      </span>
    </el-dialog>

    <!-- LOCK_MODE 说明弹窗 -->
    <el-dialog title="LOCK_MODE 锁说明" :visible.sync="helpDialogVisible" width="1100px" :close-on-click-modal="true">
      <el-tabs v-model="helpActiveTab" type="border-card">
        <el-tab-pane label="LOCK_MODE 锁说明" name="lockmode">
          <div class="md-content" v-html="helpMarkdownHtml"></div>
        </el-tab-pane>
        <el-tab-pane label="Maximo优化说明" name="maximo">
          <div class="md-content" v-html="maximoHelpMarkdownHtml"></div>
        </el-tab-pane>
      </el-tabs>
      <span slot="footer" class="dialog-footer">
        <el-button @click="helpDialogVisible = false">关 闭</el-button>
      </span>
    </el-dialog>
  </section>
</template>

<script>
import { getDb2LockList, diagnoseDb2Lock } from '@/api/db2lock'
import { sksPageMixin } from "sks-plugin-el-erp/lib/sks-page";

// LOCK_MODE 锁说明 markdown 内容
const LOCK_MODE_HELP_MD = [
  '# LOCK_MODE锁说明',
  'DB2 的 `LOCK_MODE` 表示锁的模式（强度），决定了持锁者能做什么、以及是否阻塞其他事务。下面是完整含义。',
  '',
  '## DB2 锁模式（LOCK_MODE）完整含义',
  '',
  '| 缩写 | 全称 | 中文含义 | 适用对象 | 说明 |',
  '|------|------|---------|---------|------|',
  '| **IN** | Intent None | 意向无锁 | 表/表空间 | 只读且不申请行锁，兼容性最强，几乎不阻塞任何人 |',
  '| **IS** | Intent Share | 意向共享 | 表/表空间 | 声明"我要在表的部分行上加 S 锁"，读操作常见。**你查到的就是这个** |',
  '| **IX** | Intent Exclusive | 意向排他 | 表/表空间 | 声明"我要在部分行上加 X 锁"，即将修改部分行 |',
  '| **S** | Share | 共享锁 | 行/表 | 读锁，多个事务可同时持有 S 锁，但阻塞写 |',
  '| **SIX** | Share with Intent Exclusive | 共享+意向排他 | 表 | 对整表加 S 锁，同时打算对个别行加 X 锁 |',
  '| **U** | Update | 更新锁 | 行/表 | 读时预备更新，防止死锁（S→X 升级前的过渡锁），只允许一个 U 锁 |',
  '| **NS** | Next Key Share | 下一键共享 | 行 | RS/CS 隔离级下的读锁，比 S 锁并发性更好 |',
  '| **NW** | Next Key Weak Exclusive | 下一键弱排他 | 行 | 插入时对"下一键"加锁，防止幻读 |',
  '| **X** | Exclusive | 排他锁 | 行/表 | 写锁，独占，阻塞其他所有事务的读写 |',
  '| **Z** | Super Exclusive | 超级排他 | 表 | 最强锁，DDL（如 ALTER/DROP TABLE、REORG）时使用，阻塞一切 |',
  '| **W** | Weak Exclusive | 弱排他 | 行 | 单行插入时使用 |',
  '',
  '## 按锁强度排序（从弱到强）',
  '',
  '```',
  'IN  <  IS  <  NS  <  S / IX  <  U  <  NW / W  <  SIX  <  X  <  Z',
  '```',
  '',
  '## 锁兼容性（谁能与谁共存）',
  '',
  '| | IS | IX | S | SIX | U | X | Z |',
  '|---|---|---|---|---|---|---|',
  '| **IS** | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ |',
  '| **IX** | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |',
  '| **S** | ✅ | ❌ | ✅ | ❌ | ✅ | ❌ | ❌ |',
  '| **SIX** | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |',
  '| **U** | ✅ | ❌ | ✅ | ❌ | ❌ | ❌ | ❌ |',
  '| **X** | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |',
  '| **Z** | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |',
  '',
  '表变更（DDL，如 `ALTER TABLE`、`DROP TABLE`、`CREATE INDEX`、`REORG`）需要在表上获取 **Z 锁（Super Exclusive，超级排他锁）**。Z 锁与**所有其他锁都不兼容**——所以任何在表上持有锁的事务都会阻塞你的表变更。',
  '',
  '## 结论：所有表锁都会影响表变更',
  '',
  '因为 DDL 要拿 Z 锁，而 Z 锁和任何锁（连最弱的 IN/IS 都不行）都冲突。只要有别人在这张表上持锁且未释放，你的 `ALTER/DROP` 就会一直等待（直到超时报 `SQL0911N` 锁超时或死锁）。',
  '',
  '### 按阻塞严重程度分类',
  '',
  '| 持有的锁 | 是否阻塞表变更 | 说明 |',
  '|---------|:---:|------|',
  '| **Z** (Super Exclusive) | ⛔ | 别人也在做 DDL，直接冲突 |',
  '| **X** (Exclusive) | ⛔ | 有事务正独占修改表 |',
  '| **SIX** (Share+Intent Excl) | ⛔ | 整表 S + 部分行 X |',
  '| **U** (Update) | ⛔ | 更新预备锁 |',
  '| **S** (Share) | ⛔ | 整表读锁 |',
  '| **IX** (Intent Exclusive) | ⛔ | 有事务在改部分行 |',
  '| **IS** (Intent Share) | ⛔ | 有事务在读部分行 ← **你当前这个** |',
  '| **IN** (Intent None) | ⛔ | 即使最弱的意向锁也挡 DDL |',
  '',
  '> 关键点：**不是"某几种锁"影响表变更，而是"只要表上有任何锁"就影响。** 因为 Z 锁的兼容性表整行全是 ❌。',
  ''
].join('\n')

// Maximo 优化说明 markdown 内容
const MAXIMO_HELP_MD = [
  '# Maximo 优化说明',
  '',
  '## 长连接不释放',
  '',
  '开启 Maximo 长连接自动回收（7.5.0.3+）：',
  '',
  '```',
  'mxe.db.closelongrunconn=true（默认true,启用长连接自动关闭）',
  'mxe.db.longruntimelimit=80（分钟,默认180，可按实际调小，比如 30–60）',
  'mxe.db.detectlongrunconninterval=30（检查频率，最小30）[检查长时间运行的连接的间隔（分钟）。默认值=0。（0，30]中的值被视为30。]',
  '```',
  ''
].join('\n')

export default {
  name: 'Db2LockList',
  mixins: [sksPageMixin],
  data() {
    return {
      loading: false,
      hasSearched: false,
      total: 0,
      formData: {
        tabName: ''
      },
      tableSelection: [],
      detailDialog: {
        visible: false
      },
      detailRow: null,
      sqlDialog: {
        visible: false,
        count: 0
      },
      sqlText: '',
      monacoLoaded: false,
      sqlEditor: null,
      // 锁诊断
      diagnoseLoading: false,
      diagnoseText: '',
      diagnoseEditor: null,
      helpDialogVisible: false,
      helpActiveTab: 'lockmode',
      helpMarkdownHtml: '',
      maximoHelpMarkdownHtml: ''
    }
  },
  watch: {
    'sqlDialog.visible'(val) {
      if (!val && this.sqlEditor) {
        this.sqlEditor.dispose()
        this.sqlEditor = null
      }
    },
    'detailDialog.visible'(val) {
      // 关闭锁详情弹窗时销毁诊断编辑器并清空报告
      if (!val) {
        this.disposeDiagnoseEditor()
        this.diagnoseText = ''
        this.diagnoseLoading = false
      }
    }
  },
  methods: {
    initMainTableParam() {
      return {
        ownerName: 'db2lock',
        uniqueId: 'db2lock-list',
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
            { prop: 'AGENT_ID', label: '目标进程', width: 110 },
            { prop: 'UOW_START_TIME', label: '事务开始时间', minWidth: 170 },
            { prop: 'TABNAME', label: '表名', minWidth: 140 },
            { prop: 'TABSCHEMA', label: '表模式', minWidth: 100 },
            { prop: 'LOCK_OBJECT_TYPE', label: '锁对象类型', minWidth: 120 },
            { prop: 'LOCK_MODE', label: '锁模式', minWidth: 100 },
            { prop: 'LOCK_STATUS', label: '锁状态', minWidth: 100 },
            { prop: 'TBSP_NAME', label: '表空间', minWidth: 110 },
            { prop: 'LOCK_NAME', label: '锁名称', minWidth: 130 },
            { prop: 'APPL_NAME', label: '应用名', minWidth: 140 },
            { prop: 'AUTHID', label: '认证ID', width: 100 },
            { prop: 'UOW_STATE', label: '工作单元状态', minWidth: 130 },
            { prop: 'TOTAL_APP_COMMITS', label: '提交次数', width: 100 },
            { prop: 'TOTAL_APP_ROLLBACKS', label: '回滚次数', width: 100 },
            { prop: 'APPL_STATUS', label: '应用状态', minWidth: 110 },
            { prop: 'CLIENT_IP', label: '客户端IP', minWidth: 120 },
            { prop: 'CLIENT_PRDID', label: '客户端产品ID', minWidth: 120 },
            { prop: 'SNAPSHOT_TIMESTAMP', label: '快照时间', minWidth: 170 }
          ]),
        queryParamsColumnListEnable: false,
        queryParamsColumnList: []
      }
    },
    handleQuery() {
      this.hasSearched = true
      this.mainTable.queryParams.pageNum = 1
      this.fetchList()
    },
    fetchList() {
      this.loading = true
      const params = {
        tabName: this.formData.tabName,
        pageNum: this.mainTable.queryParams.pageNum,
        pageSize: this.mainTable.queryParams.pageSize
      }
      getDb2LockList(params)
        .then(res => {
          if (res.code === 200 && res.data) {
            const rows = (res.data.rows || []).map(row => {
              return Object.assign({}, row, {
                UOW_START_TIME: this.formatDate(row.UOW_START_TIME),
                SNAPSHOT_TIMESTAMP: this.formatDate(row.SNAPSHOT_TIMESTAMP)
              })
            })
            this.mainTable.list = rows
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
    resetForm() {
      this.formData = {
        tabName: ''
      }
      this.hasSearched = false
      this.mainTable.list = []
      this.mainTable.total = 0
      this.total = 0
      this.mainTable.currentPage = 1
      this.tableSelection = []
    },
    handleTableSelectionChange(selection) {
      this.tableSelection = selection || []
    },
    handleRowClick(row) {
      // 切换行时重置上一次的诊断结果
      this.disposeDiagnoseEditor()
      this.diagnoseText = ''
      this.diagnoseLoading = false
      this.detailRow = row
      this.detailDialog.visible = true
    },
    generateSingleForceSql() {
      if (!this.detailRow) return
      this.showSqlDialog([this.detailRow])
    },
    // ==================== 锁诊断 ====================
    runDiagnose() {
      if (!this.detailRow) return
      this.disposeDiagnoseEditor()
      this.diagnoseLoading = true
      this.diagnoseText = '正在执行诊断SQL，请稍候...'
      // 按钮 loading 后诊断区才会渲染出来，等 DOM 就绪再初始化编辑器
      this.$nextTick(() => {
        setTimeout(() => {
          this.initDiagnoseEditor()
        }, 200)
      })

      const params = {
        agentId: this.detailRow.AGENT_ID,
        tabName: this.detailRow.TABNAME
      }
      diagnoseDb2Lock(params)
        .then(res => {
          if (res && res.code === 200 && res.data) {
            this.diagnoseText = res.data.reportText || '诊断未返回内容'
          } else {
            this.diagnoseText = '诊断失败：' + ((res && res.message) || '未知错误')
            this.$message.error((res && res.message) || '诊断失败')
          }
        })
        .catch(err => {
          this.diagnoseText = '诊断请求失败：' + (err.message || String(err))
        })
        .finally(() => {
          this.diagnoseLoading = false
          this.$nextTick(() => {
            setTimeout(() => {
              if (this.diagnoseEditor) {
                this.diagnoseEditor.setValue(this.diagnoseText)
              } else {
                this.initDiagnoseEditor()
              }
            }, 100)
          })
        })
    },
    initDiagnoseEditor() {
      const el = this.$refs.diagnoseMonacoRef
      if (!el) return
      const create = (monaco) => {
        if (this.diagnoseEditor) {
          this.diagnoseEditor.setValue(this.diagnoseText)
          return
        }
        this.diagnoseEditor = monaco.editor.create(el, {
          value: this.diagnoseText,
          language: 'sql',
          readOnly: true,
          theme: 'vs',
          automaticLayout: true,
          minimap: { enabled: false },
          scrollBeyondLastLine: false,
          fontSize: 12,
          wordWrap: 'on',
          folding: false,
          lineNumbers: 'on',
          renderLineHighlight: 'none'
        })
      }
      if (!this.monacoLoaded) {
        import(/* webpackChunkName: "monaco" */ 'monaco-editor').then(monaco => {
          this.monacoLoaded = true
          this._monaco = monaco
          create(monaco)
        }).catch(err => {
          console.error('Monaco Editor 加载失败:', err)
          this.diagnoseText = (this.diagnoseText || '') + '\n\n[Monaco Editor 加载失败: ' + (err.message || err) + ']'
        })
      } else {
        create(this._monaco)
      }
    },
    disposeDiagnoseEditor() {
      if (this.diagnoseEditor) {
        this.diagnoseEditor.dispose()
        this.diagnoseEditor = null
      }
    },
    copyDiagnoseReport() {
      if (!this.diagnoseText) {
        this.$message.warning('诊断报告为空')
        return
      }
      if (navigator.clipboard && window.isSecureContext) {
        navigator.clipboard.writeText(this.diagnoseText)
          .then(() => this.$message.success('诊断报告已复制到剪贴板'))
          .catch(() => this.fallbackCopy(this.diagnoseText, '诊断报告已复制到剪贴板'))
      } else {
        this.fallbackCopy(this.diagnoseText, '诊断报告已复制到剪贴板')
      }
    },
    showForceSql() {
      if (this.tableSelection.length === 0) {
        this.$message.warning('请先勾选需要生成SQL的行')
        return
      }
      this.showSqlDialog(this.tableSelection)
    },
    showSqlDialog(rows) {
      const sqlLines = rows.map(row => {
        return "CALL SYSPROC.ADMIN_CMD('force application (" + row.AGENT_ID + ")')" + ';'
      })
      this.sqlText = sqlLines.join('\n')
      this.sqlDialog.count = rows.length
      this.sqlDialog.visible = true
      this.$nextTick(() => {
        setTimeout(() => {
          this.initSqlEditor()
        }, 300)
      })
    },
    onSqlDialogOpened() {
      this.$nextTick(() => {
        setTimeout(() => {
          this.initSqlEditor()
        }, 300)
      })
    },
    initSqlEditor() {
      if (!this.sqlText) return
      if (!this.monacoLoaded) {
        import(/* webpackChunkName: "monaco" */ 'monaco-editor').then(monaco => {
          this.monacoLoaded = true
          this._monaco = monaco
          this.createSqlEditor(monaco)
        }).catch(err => {
          console.error('Monaco Editor 加载失败:', err)
        })
      } else {
        this.createSqlEditor(this._monaco)
      }
    },
    createSqlEditor(monaco) {
      const el = this.$refs.sqlMonacoRef
      if (!el) return
      if (this.sqlEditor) {
        this.sqlEditor.setValue(this.sqlText)
        return
      }
      this.sqlEditor = monaco.editor.create(el, {
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
        renderLineHighlight: 'none'
      })
    },
    copyForceSql() {
      if (!this.sqlText) {
        this.$message.warning('SQL内容为空')
        return
      }
      if (navigator.clipboard && window.isSecureContext) {
        navigator.clipboard.writeText(this.sqlText)
          .then(() => this.$message.success('SQL已复制到剪贴板'))
          .catch(() => this.fallbackCopy(this.sqlText, 'SQL已复制到剪贴板'))
      } else {
        this.fallbackCopy(this.sqlText, 'SQL已复制到剪贴板')
      }
    },
    fallbackCopy(text, successMsg) {
      const textarea = document.createElement('textarea')
      textarea.value = text
      textarea.style.position = 'fixed'
      textarea.style.top = '-1000px'
      textarea.style.left = '-1000px'
      document.body.appendChild(textarea)
      textarea.select()
      try {
        document.execCommand('copy')
        this.$message.success(successMsg || '已复制到剪贴板')
      } catch (e) {
        this.$message.error('复制失败: ' + e.message)
      }
      document.body.removeChild(textarea)
    },
    formatDate(date) {
      if (!date) return '-'
      const d = new Date(date)
      if (isNaN(d.getTime())) return date
      const pad = n => String(n).padStart(2, '0')
      return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()) +
        ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes()) + ':' + pad(d.getSeconds())
    },
    // 轻量 markdown 渲染（标题/表格/代码块/引用/加粗/行内代码）
    renderMarkdown(md) {
      if (!md) return ''
      const escapeHtml = (s) => s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      const inline = (s) => escapeHtml(s)
        .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
        .replace(/`([^`]+)`/g, '<code>$1</code>')
      let html = ''
      let inTable = false
      let inCode = false
      const lines = md.split('\n')
      for (let i = 0; i < lines.length; i++) {
        const line = lines[i]
        // 代码块
        if (/^\s*```/.test(line)) {
          if (inCode) {
            html += '</code></pre>\n'
            inCode = false
          } else {
            html += '<pre><code>'
            inCode = true
          }
          continue
        }
        if (inCode) {
          html += escapeHtml(line) + '\n'
          continue
        }
        // 表格
        if (/^\s*\|/.test(line)) {
          // 表头分隔行（|------| 或 |:---:|）
          if (/^\s*\|[\s|:=-]*\|?\s*$/.test(line) && line.indexOf('---') >= 0) {
            continue
          }
          const cells = line.replace(/^\s*\||\|\s*$/g, '').split('|').map(c => c.trim())
          if (!inTable) {
            html += '<table><thead><tr>' + cells.map(c => '<th>' + inline(c) + '</th>').join('') + '</tr></thead><tbody>'
            inTable = true
          } else {
            html += '<tr>' + cells.map(c => '<td>' + inline(c) + '</td>').join('') + '</tr>'
          }
          continue
        }
        if (inTable) {
          html += '</tbody></table>\n'
          inTable = false
        }
        // 标题
        const h = line.match(/^(#{1,6})\s+(.*)$/)
        if (h) {
          html += '<h' + h[1].length + '>' + inline(h[2]) + '</h' + h[1].length + '>\n'
          continue
        }
        // 引用
        if (/^\s*>\s?/.test(line)) {
          html += '<blockquote>' + inline(line.replace(/^\s*>\s?/, '')) + '</blockquote>\n'
          continue
        }
        // 分割线
        if (/^\s*[-*_]{3,}\s*$/.test(line)) {
          html += '<hr/>\n'
          continue
        }
        // 空行
        if (line.trim() === '') {
          html += '\n'
          continue
        }
        html += '<p>' + inline(line) + '</p>\n'
      }
      if (inCode) html += '</code></pre>'
      if (inTable) html += '</tbody></table>'
      return html
    }
  },
  mounted() {
    if (this.mainTable) {
      this.mainTable.onPageChange = (page, limit) => {
        this.handlePageChange(page, limit)
      }
    }
    this.helpMarkdownHtml = this.renderMarkdown(LOCK_MODE_HELP_MD)
    this.maximoHelpMarkdownHtml = this.renderMarkdown(MAXIMO_HELP_MD)
  },
  beforeDestroy() {
    // 组件销毁时释放 Monaco 实例
    this.disposeDiagnoseEditor()
    if (this.sqlEditor) {
      this.sqlEditor.dispose()
      this.sqlEditor = null
    }
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
.monaco-container {
  height: 400px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}
.diagnose-monaco-container {
  height: 460px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background-color: #fff;
}
.md-content {
  font-size: 14px;
  line-height: 1.7;
  color: #303133;
  overflow: auto;
  max-height: 70vh;
}
.md-content h1 {
  font-size: 22px;
  border-bottom: 1px solid #e4e7ed;
  padding-bottom: 8px;
  margin: 16px 0 12px;
}
.md-content h2 {
  font-size: 18px;
  margin: 18px 0 10px;
  border-left: 4px solid #409eff;
  padding-left: 10px;
}
.md-content h3 {
  font-size: 16px;
  margin: 14px 0 8px;
}
.md-content table {
  border-collapse: collapse;
  width: 100%;
  margin: 10px 0;
  font-size: 13px;
}
.md-content th,
.md-content td {
  border: 1px solid #dcdfe6;
  padding: 6px 10px;
  text-align: left;
}
.md-content th {
  background-color: #f5f7fa;
  font-weight: 600;
  white-space: nowrap;
}
.md-content pre {
  background-color: #f5f7fa;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 10px 14px;
  overflow: auto;
  font-size: 13px;
  line-height: 1.5;
}
.md-content code {
  background-color: #f5f7fa;
  color: #c7254e;
  padding: 2px 5px;
  border-radius: 3px;
  font-size: 13px;
}
.md-content pre code {
  background-color: transparent;
  color: #303133;
  padding: 0;
}
.md-content blockquote {
  border-left: 4px solid #e6a23c;
  background-color: #fdf6ec;
  padding: 8px 12px;
  margin: 10px 0;
  border-radius: 0 4px 4px 0;
  color: #b06f0c;
}
.md-content p {
  margin: 8px 0;
}
.md-content hr {
  border: none;
  border-top: 1px solid #e4e7ed;
  margin: 14px 0;
}
</style>
