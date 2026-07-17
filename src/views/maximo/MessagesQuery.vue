<template>
  <section class="query-page">
    <el-card>
      <div class="page-header-row">
        <div>
          <h2>消息查询</h2>
          <p class="page-summary">支持 msgid / msggroup + msgkey 查询，环境认证信息由配置自动加载。</p>
        </div>
      </div>
     <el-form :model="formData" ref="queryForm" :inline="true" label-width="68px" @submit.native.prevent>
            <el-form-item label="消息 ID">
              <el-input v-model="formData.msgid" placeholder="BMXAA6378I"  @keyup.enter.native="handleQuery" />
            </el-form-item>
            <el-form-item label="消息组">
              <el-input v-model="formData.msggroup" placeholder="system"  @keyup.enter.native="handleQuery" />
            </el-form-item>
            <el-form-item label="消息键">
              <el-input v-model="formData.msgkey" placeholder="crontaskmanager25"  @keyup.enter.native="handleQuery" />
            </el-form-item>
            <el-form-item label="消息内容">
              <el-input v-model="formData.value" placeholder="模糊搜索 value 内容"  @keyup.enter.native="handleQuery" />
            </el-form-item>
            <el-form-item>
                <el-button type="cyan" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
                <el-button icon="el-icon-refresh" size="mini" @click="resetForm">重置</el-button>
            </el-form-item>
        </el-form>

      <div class="result-panel">
        <div style="margin-bottom: 12px; text-align: right;">
          <el-button type="primary" icon="el-icon-view" size="mini" :loading="batchLoading" @click="handleBatchView">批量查看</el-button>
        </div>
        <el-table :loading="loading" :data="messages" stripe style="width: 100%" class="result-table"
          :highlight-current-row="true" @row-click="handleRowClick">
          <el-table-column prop="msgid" label="msgid" />
          <el-table-column prop="msggroup" label="msggroup" />
          <el-table-column prop="msgkey" label="msgkey" />
          <el-table-column prop="value" label="value" />
          <el-table-column prop="displaymethod_description" label="displaymethod" />
        </el-table>
       <pagination v-show="tablePatam.total > 0"
            :total="tablePatam.total"
            :page.sync="tablePatam.pageNum"
            :limit.sync="tablePatam.pageSize"
            @pagination="getList"
        />
      </div>
    </el-card>

    <el-dialog :title="'消息详情 - ' + (currentRow ? currentRow.msgid : '')" :visible.sync="dialogVisible" width="1500px" :close-on-click-modal="true" @opened="onDialogOpened">
      <el-descriptions :column="3" border v-if="currentRow">
        <el-descriptions-item label="消息ID">
          <el-input :value="currentRow.msgid || '-'" readonly size="small" />
        </el-descriptions-item>
        <el-descriptions-item label="消息组">
          <el-input :value="currentRow.msggroup || '-'" readonly size="small" />
        </el-descriptions-item>
        <el-descriptions-item label="消息键">
          <el-input :value="currentRow.msgkey || '-'" readonly size="small" />
        </el-descriptions-item>
        <el-descriptions-item label="显示方法">
          <el-input :value="currentRow.displaymethod_description || currentRow.displaymethod || '-'" readonly size="small" />
        </el-descriptions-item>
        <el-descriptions-item label="选项" :span="2">
          <el-input :value="formatOptions(currentRow.options)" readonly size="small" />
        </el-descriptions-item>
        <el-descriptions-item label="消息内容" :span="3">
          <el-input :value="currentRow.value || '-'" readonly type="textarea" :rows="3" />
        </el-descriptions-item>
        <el-descriptions-item label="标题">
          <el-input :value="currentRow.title || '-'" readonly type="textarea" :rows="2" />
        </el-descriptions-item>
        <el-descriptions-item label="按钮文本">
          <el-input :value="currentRow.buttonText || '-'" readonly size="small" />
        </el-descriptions-item>
        <el-descriptions-item label="解释">
          <el-input :value="currentRow.explanation || '-'" readonly type="textarea" :rows="2" />
        </el-descriptions-item>
        <el-descriptions-item label="管理员响应">
          <el-input :value="currentRow.adminResponse || '-'" readonly type="textarea" :rows="2" />
        </el-descriptions-item>
        <el-descriptions-item label="操作员响应">
          <el-input :value="currentRow.operatorResponse || '-'" readonly type="textarea" :rows="2" />
        </el-descriptions-item>
        <el-descriptions-item label="系统操作">
          <el-input :value="currentRow.systemAction || '-'" readonly type="textarea" :rows="2" />
        </el-descriptions-item>
        <el-descriptions-item label="响应">
          <el-input :value="currentRow.response || '-'" readonly type="textarea" :rows="2" />
        </el-descriptions-item>
        <el-descriptions-item label="前缀">
          <el-input :value="currentRow.prefix || '-'" readonly size="small" />
        </el-descriptions-item>
        <el-descriptions-item label="按钮" :span="2">
          <el-tag v-if="currentRow.ok" type="success" size="small">OK</el-tag>
          <el-tag v-if="currentRow.yes" type="success" size="small">YES</el-tag>
          <el-tag v-if="currentRow.no" type="danger" size="small">NO</el-tag>
          <el-tag v-if="currentRow.cancel" type="warning" size="small">CANCEL</el-tag>
          <el-tag v-if="currentRow.close" type="info" size="small">CLOSE</el-tag>
          <span v-if="!currentRow.ok && !currentRow.yes && !currentRow.no && !currentRow.cancel && !currentRow.close">-</span>
        </el-descriptions-item>
        <el-descriptions-item label="图标" :span="2">
          <el-tag v-if="currentRow.stop" type="danger" size="small">STOP</el-tag>
          <el-tag v-if="currentRow.warning" type="warning" size="small">WARNING</el-tag>
          <el-tag v-if="currentRow.exclamation" type="info" size="small">EXCLAMATION</el-tag>
          <span v-if="!currentRow.stop && !currentRow.warning && !currentRow.exclamation">-</span>
        </el-descriptions-item>
      </el-descriptions>

      <el-tabs v-model="activeTab" type="border-card" class="json-tabs">
        <el-tab-pane label="消息精简" name="simple">
          <div class="json-toolbar">
            <el-button type="primary" size="mini" icon="el-icon-document-copy" @click="copySimpleJson">复制消息精简JSON</el-button>
          </div>
          <div v-loading="detailLoading" element-loading-text="加载中..." class="monaco-wrapper">
            <div ref="simpleMonacoRef" class="monaco-container"></div>
          </div>
        </el-tab-pane>
        <el-tab-pane label="消息完整" name="full">
          <div class="json-toolbar">
            <el-button type="primary" size="mini" icon="el-icon-document" @click="copyFullJson">复制消息完整JSON</el-button>
          </div>
          <div v-loading="detailLoading" element-loading-text="加载中..." class="monaco-wrapper">
            <div ref="fullMonacoRef" class="monaco-container"></div>
          </div>
        </el-tab-pane>
      </el-tabs>

      <span slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">关 闭</el-button>
      </span>
    </el-dialog>

    <el-dialog title="批量消息查看" :visible.sync="batchDialogVisible" width="1500px" :close-on-click-modal="true" @opened="onBatchDialogOpened">
      <el-tabs v-model="batchActiveTab" type="border-card" class="json-tabs">
        <el-tab-pane label="消息精简" name="batch-simple">
          <div class="json-toolbar">
            <el-button type="primary" size="mini" icon="el-icon-document-copy" @click="copyBatchSimpleJson">复制消息精简JSON</el-button>
            <span style="margin-left: 10px;" class="el-tag el-tag--info">共 {{ batchTotal }} 条记录</span>
          </div>
          <div class="monaco-wrapper">
            <div ref="batchSimpleMonacoRef" class="monaco-container"></div>
          </div>
        </el-tab-pane>
        <el-tab-pane label="消息完整" name="batch-full">
          <div class="json-toolbar">
            <el-button type="primary" size="mini" icon="el-icon-document" @click="copyBatchFullJson">复制消息完整JSON</el-button>
            <span style="margin-left: 10px;" class="el-tag el-tag--info">共 {{ batchTotal }} 条记录</span>
          </div>
          <div class="monaco-wrapper">
            <div ref="batchFullMonacoRef" class="monaco-container"></div>
          </div>
        </el-tab-pane>
      </el-tabs>

      <span slot="footer" class="dialog-footer">
        <el-button @click="batchDialogVisible = false">关 闭</el-button>
      </span>
    </el-dialog>
  </section>
</template>

<script>
import { mapGetters } from 'vuex'
import { queryMessages, exportMessages } from '../../api/messages'

export default {
  name: 'MessagesQuery',
  data() {
    return {
      tablePatam:{
        total: 0,
        pageNum: 1,
        pageSize: 10
      },
      loading: false,
      batchLoading: false,
      detailLoading: false,
      error: '',
      messages: [],
      formData: {
        msgid: '',
        msggroup: '',
        msgkey: '',
        value: ''
      },
      dialogVisible: false,
      currentRow: null,
      activeTab: 'simple',
      batchActiveTab: 'batch-simple',
      simpleEditor: null,
      fullEditor: null,
      monacoLoaded: false,
      _monaco: null,
      batchDialogVisible: false,
      batchSimpleJson: '',
      batchFullJson: '',
      batchTotal: 0,
      batchSimpleEditor: null,
      batchFullEditor: null
    }
  },
  computed: {
    ...mapGetters([
      'selectedEnv'
    ])
  },
  watch: {
    dialogVisible(val) {
      if (val && this.currentRow) {
        this.$nextTick(() => {
          this.initMonacoEditors()
        })
      } else {
        this.disposeEditors()
      }
    },
    activeTab() {
      this.$nextTick(() => {
        this.layoutEditors()
      })
    },
    batchDialogVisible(val) {
      if (!val) {
        this.disposeBatchEditors()
      }
    },
    batchActiveTab() {
      this.$nextTick(() => {
        if (this.batchSimpleEditor) this.batchSimpleEditor.layout()
        if (this.batchFullEditor) this.batchFullEditor.layout()
      })
    }
  },
  methods: {
    buildWhere(params) {
      let clauses = []
      if (params.msgid) {
        clauses.push(`msgid="${params.msgid}"`)
      }
      if (params.msggroup) {
        clauses.push(`msggroup="${params.msggroup}"`)
      }
      if (params.msgkey) {
        clauses.push(`msgkey="${params.msgkey}"`)
      }
      if (params.value) {
        clauses.push(`value="${params.value}"`)
      }
      return clauses.join(' and ')
    },
    buildSqlWhere(params) {
      let clauses = []
      if (params.msgid) {
        const op = params.msgid.includes('%') || params.msgid.includes('_') ? 'LIKE' : '='
        clauses.push(`UPPER(MSGID) ${op} UPPER('${params.msgid}')`)
      }
      if (params.msggroup) {
        const op = params.msggroup.includes('%') || params.msggroup.includes('_') ? 'LIKE' : '='
        clauses.push(`UPPER(MSGGROUP) ${op} UPPER('${params.msggroup}')`)
      }
      if (params.msgkey) {
        const op = params.msgkey.includes('%') || params.msgkey.includes('_') ? 'LIKE' : '='
        clauses.push(`UPPER(MSGKEY) ${op} UPPER('${params.msgkey}')`)
      }
      if (params.value) {
        clauses.push(`UPPER(VALUE) LIKE UPPER('%${params.value}%')`)
      }
      return clauses.join(' and ')
    },
    formatOptions(options) {
      if (!options) return '-'
      if (Array.isArray(options)) {
        return options.join(', ')
      }
      if (typeof options === 'string') {
        return options
      }
      if (typeof options === 'object') {
        return JSON.stringify(options)
      }
      return String(options)
    },
    async handleQuery() {
      this.tablePatam.pageNum=1
      this.getList()
    },
    getList() {
      this.loading = true
      this.error = ''
      this.messages = []

      try {
        const env = this.selectedEnv
        const params = {
          msgid:  this.formData.msgid ,
          msggroup:this.formData.msggroup ,
          msgkey:  this.formData.msgkey ,
          value:   this.formData.value 
        }

        const whereClause = this.buildWhere(params)
        console.log("whereClause=", whereClause)

        const queryParams={
          lean: '1',
          'collectioncount': 1,
          'oslc.select': '*',
          'oslc.where': whereClause&&whereClause!==''?whereClause: undefined,
          'oslc.paging': 'true',
          'oslc.pageSize': this.tablePatam.pageSize,
          'pageno': this.tablePatam.pageNum
        }

        queryMessages(queryParams).then(res => {
          console.log("查询结果", res)
          const data = res.data || res
          const members = data.member || data.Member || []
          this.messages = members.map(item => ({
            msgid: item.msgid || item.msgId || item.MSGID || '-',
            msggroup: item.msggroup || item.msgGroup || item.MSGGROUP || '-',
            msgkey: item.msgkey || item.msgKey || item.MSGKEY || '-',
            value: item.value || item.VALUE || '-',
            displaymethod: item.displaymethod || item.displayMethod || item.DISPLAYMETHOD || '-',
            displaymethod_description: item.displaymethod_description || item.displayMethod_description || item.DISPLAYMETHOD_DESCRIPTION || '-',
            ...item
          }))
          this.tablePatam.total = data.responseInfo && data.responseInfo.totalCount || this.messages.length
          this.loading = false
        }).catch(err => {
          console.error("查询失败", err)
          this.$message.error("查询消息失败：" + (err.message || String(err)))
          this.loading = false
        })
      } catch (err) {
        this.error = err instanceof Error ? err.message : String(err)
      } finally {
        this.loading = false
      }
    },
    resetForm() {
      this.formData = {
        msgid: '',
        msggroup: '',
        msgkey: '',
        value: ''
      }
      this.searchMode = 'msgid'
      this.error = ''
      this.tablePatam.pageNum=1
      this.messages = []
    },
    async handleRowClick(row) {
      this.currentRow = row
      this.activeTab = 'simple'
      this.dialogVisible = true
      this.detailLoading = true

      this.$nextTick(() => {
        this.initMonacoEditors()
      })

      try {
        const whereClause = this.buildSqlWhere({
          msgid: row.msgid,
          msggroup: row.msggroup,
          msgkey: row.msgkey
        })

        if (!whereClause) return

        const res = await exportMessages({
          filterMode: 'where',
          where: whereClause
        })

        const data = res.data || res
        if (data && data.status === 'error') {
          this.$message.error('获取消息详情失败: ' + (data.message || '未知错误'))
          return
        }
        if (data && data.messages && data.messages.length > 0) {
          this.currentRow = { ...row, ...data.messages[0] }
          this.$nextTick(() => {
            this.initMonacoEditors()
          })
        }
      } catch (err) {
        console.warn('获取消息详情失败:', err)
        this.$message.error('获取消息详情失败: ' + (err.message || String(err)))
      } finally {
        this.detailLoading = false
      }
    },
    async handleBatchView() {
      if (!this.messages || this.messages.length === 0) {
        this.$message.warning('没有数据可批量查看')
        return
      }

      this.batchLoading = true

      try {
        const whereClause = this.buildSqlWhere(this.formData)
        if (!whereClause) {
          this.$message.warning('请先设置查询条件')
          return
        }

        const res = await exportMessages({
          filterMode: 'where',
          where: whereClause
        })

        const data = res.data || res
        if (data && data.status === 'error') {
          this.$message.error('批量查看失败: ' + (data.message || '未知错误'))
          return
        }
        if (data && data.messages && data.messages.length > 0) {
          this.batchTotal = data.messages.length
          const batchData = data.messages.map(msg => ({
            msgGroup: msg.msggroup || msg.msgGroup || msg.MSGGROUP || null,
            msgKey: msg.msgkey || msg.msgKey || msg.MSGKEY || null,
            value: msg.value || msg.VALUE || null,
            displayMethod: msg.displaymethod || msg.displayMethod || msg.DISPLAYMETHOD || null,
            options: msg.options || null,
            title: msg.title || null,
            buttonText: msg.buttonText || null,
            explanation: msg.explanation || null,
            adminResponse: msg.adminResponse || null,
            operatorResponse: msg.operatorResponse || null,
            systemAction: msg.systemAction || null,
            response: msg.response || null,
            msgId: msg.msgid || msg.msgId || msg.MSGID || null,
            ok: msg.ok || null,
            yes: msg.yes || null,
            no: msg.no || null,
            cancel: msg.cancel || null,
            close: msg.close || null,
            stop: msg.stop || null,
            warning: msg.warning || null,
            exclamation: msg.exclamation || null,
            prefix: msg.prefix || null
          }))

          const simpleJson = JSON.stringify({
            messages: batchData.map(msg => ({
              msgGroup: msg.msgGroup,
              msgKey: msg.msgKey,
              value: msg.value,
              displayMethod: msg.displayMethod
            }))
          }, null, 2)

          const fullJson = JSON.stringify({
            messages: batchData
          }, null, 2)

          this.batchSimpleJson = simpleJson
          this.batchFullJson = fullJson
          this.batchDialogVisible = true
        } else {
          this.$message.warning('未查询到消息数据')
        }
      } catch (err) {
        console.error('批量查看失败:', err)
        this.$message.error('批量查看失败: ' + (err.message || String(err)))
      } finally {
        this.batchLoading = false
      }
    },
    onDialogOpened() {
      this.$nextTick(() => {
        setTimeout(() => {
          this.initMonacoEditors()
        }, 100)
      })
    },
    onBatchDialogOpened() {
      this.$nextTick(() => {
        setTimeout(() => {
          this.initBatchMonacoEditors()
        }, 100)
      })
    },
    initBatchMonacoEditors() {
      if (!this.batchSimpleJson) return
      if (!this.monacoLoaded) {
        import(/* webpackChunkName: "monaco" */ 'monaco-editor').then(monaco => {
          this.monacoLoaded = true
          this._monaco = monaco
          this.createBatchEditors()
        }).catch(err => {
          console.error('Monaco Editor 加载失败:', err)
        })
      } else {
        this.createBatchEditors()
      }
    },
    createBatchEditors() {
      const monaco = this._monaco
      const options = {
        language: 'json',
        readOnly: true,
        theme: 'vs',
        automaticLayout: false,
        minimap: { enabled: false },
        scrollBeyondLastLine: false,
        fontSize: 13,
        wordWrap: 'on',
        folding: true,
        lineNumbers: 'on',
        renderLineHighlight: 'none',
        scrollbar: {
          verticalScrollbarSize: 10,
          horizontalScrollbarSize: 10
        }
      }
      if (this.$refs.batchSimpleMonacoRef && !this.batchSimpleEditor) {
        this.batchSimpleEditor = monaco.editor.create(this.$refs.batchSimpleMonacoRef, {
          value: this.batchSimpleJson,
          ...options
        })
      } else if (this.batchSimpleEditor) {
        this.batchSimpleEditor.setValue(this.batchSimpleJson)
      }
      if (this.$refs.batchFullMonacoRef && !this.batchFullEditor) {
        this.batchFullEditor = monaco.editor.create(this.$refs.batchFullMonacoRef, {
          value: this.batchFullJson,
          ...options
        })
      } else if (this.batchFullEditor) {
        this.batchFullEditor.setValue(this.batchFullJson)
      }
      setTimeout(() => {
        if (this.batchSimpleEditor) this.batchSimpleEditor.layout()
        if (this.batchFullEditor) this.batchFullEditor.layout()
      }, 100)
    },
    copyBatchSimpleJson() {
      if (this.batchSimpleJson) {
        this.copyToClipboard(this.batchSimpleJson, '消息精简JSON')
      }
    },
    copyBatchFullJson() {
      if (this.batchFullJson) {
        this.copyToClipboard(this.batchFullJson, '消息完整JSON')
      }
    },
    disposeBatchEditors() {
      if (this.batchSimpleEditor) {
        this.batchSimpleEditor.dispose()
        this.batchSimpleEditor = null
      }
      if (this.batchFullEditor) {
        this.batchFullEditor.dispose()
        this.batchFullEditor = null
      }
    },
    initMonacoEditors() {
      if (!this.currentRow) return
      const simpleJson = JSON.stringify({
        msgGroup: this.currentRow.msggroup || null,
        msgKey: this.currentRow.msgkey || null,
        value: this.currentRow.value || null,
        displayMethod: this.currentRow.displaymethod || null
      }, null, 2)
      const fullJson = JSON.stringify({
        msgGroup: this.currentRow.msggroup || null,
        msgKey: this.currentRow.msgkey || null,
        value: this.currentRow.value || null,
        displayMethod: this.currentRow.displaymethod || null,
        options: this.currentRow.options || null,
        title: this.currentRow.title || null,
        buttonText: this.currentRow.buttonText || null,
        explanation: this.currentRow.explanation || null,
        adminResponse: this.currentRow.adminResponse || null,
        operatorResponse: this.currentRow.operatorResponse || null,
        systemAction: this.currentRow.systemAction || null,
        response: this.currentRow.response || null,
        msgId: this.currentRow.msgid || null,
        ok: this.currentRow.ok || null,
        yes: this.currentRow.yes || null,
        no: this.currentRow.no || null,
        cancel: this.currentRow.cancel || null,
        close: this.currentRow.close || null,
        stop: this.currentRow.stop || null,
        warning: this.currentRow.warning || null,
        exclamation: this.currentRow.exclamation || null,
        prefix: this.currentRow.prefix || null,
        msgIdPrefix: this.currentRow.msgIdPrefix || null,
        msgIdSuffix: this.currentRow.msgIdSuffix || null
      }, null, 2)
      if (!this.monacoLoaded) {
        import(/* webpackChunkName: "monaco" */ 'monaco-editor').then(monaco => {
          this.monacoLoaded = true
          this._monaco = monaco
          this.createEditors(simpleJson, fullJson)
        }).catch(err => {
          console.error('Monaco Editor 加载失败:', err)
        })
      } else {
        this.createEditors(simpleJson, fullJson)
      }
    },
    createEditors(simpleJson, fullJson) {
      const monaco = this._monaco
      const options = {
        language: 'json',
        readOnly: true,
        theme: 'vs',
        automaticLayout: false,
        minimap: { enabled: false },
        scrollBeyondLastLine: false,
        fontSize: 13,
        wordWrap: 'on',
        folding: true,
        lineNumbers: 'on',
        renderLineHighlight: 'none',
        scrollbar: {
          verticalScrollbarSize: 10,
          horizontalScrollbarSize: 10
        }
      }
      if (this.$refs.simpleMonacoRef && !this.simpleEditor) {
        this.simpleEditor = monaco.editor.create(this.$refs.simpleMonacoRef, {
          value: simpleJson,
          ...options
        })
      } else if (this.simpleEditor) {
        this.simpleEditor.setValue(simpleJson)
      }
      if (this.$refs.fullMonacoRef && !this.fullEditor) {
        this.fullEditor = monaco.editor.create(this.$refs.fullMonacoRef, {
          value: fullJson,
          ...options
        })
      } else if (this.fullEditor) {
        this.fullEditor.setValue(fullJson)
      }
      setTimeout(() => {
        this.layoutEditors()
      }, 100)
    },
    layoutEditors() {
      if (this.simpleEditor) this.simpleEditor.layout()
      if (this.fullEditor) this.fullEditor.layout()
    },
    disposeEditors() {
      if (this.simpleEditor) {
        this.simpleEditor.dispose()
        this.simpleEditor = null
      }
      if (this.fullEditor) {
        this.fullEditor.dispose()
        this.fullEditor = null
      }
    },
    copySimpleJson(row = this.currentRow) {
      if (!row) return
      const json = JSON.stringify({
        msgGroup: row.msggroup || null,
        msgKey: row.msgkey || null,
        value: row.value || null,
        displayMethod: row.displaymethod || null
      }, null, 2)
      this.copyToClipboard(json, '消息精简JSON')
    },
    copyFullJson(row = this.currentRow) {
      if (!row) return
      const json = JSON.stringify({
        msgGroup: row.msggroup || null,
        msgKey: row.msgkey || null,
        value: row.value || null,
        displayMethod: row.displaymethod || null,
        options: row.options || null,
        title: row.title || null,
        buttonText: row.buttonText || null,
        explanation: row.explanation || null,
        adminResponse: row.adminResponse || null,
        operatorResponse: row.operatorResponse || null,
        systemAction: row.systemAction || null,
        response: row.response || null,
        msgId: row.msgid || null,
        ok: row.ok || null,
        yes: row.yes || null,
        no: row.no || null,
        cancel: row.cancel || null,
        close: row.close || null,
        stop: row.stop || null,
        warning: row.warning || null,
        exclamation: row.exclamation || null,
        prefix: row.prefix || null,
        msgIdPrefix: row.msgIdPrefix || null,
        msgIdSuffix: row.msgIdSuffix || null
      }, null, 2)
      this.copyToClipboard(json, '消息完整JSON')
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
.button-column {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-top: 30px;
}
.result-panel {
  margin-top: 20px;
}
.empty-card {
  margin-top: 16px;
  padding: 20px;
  color: #909399;
}
.result-table {
  margin-top: 16px;
}

.el-table__body tr {
  cursor: pointer;
}

.el-table__body tr.current-row>td {
  background-color: #DCDCDC !important;
  color: #000000;
}

.el-table__body tr:hover>td {
  background-color: #d7dcdc !important;
}

.json-tabs {
  margin-top: 16px;
}
.json-toolbar {
  margin-bottom: 8px;
  text-align: right;
}
.monaco-container {
  height: 350px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}
.monaco-wrapper {
  position: relative;
}
</style>