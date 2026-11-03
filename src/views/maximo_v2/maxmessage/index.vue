<template>
  <section class="query-page">
    <el-card>
      <div class="page-header-row">
        <div>
          <h2>消息查询</h2>
          <p class="page-summary">支持 msgid / msggroup + msgkey 查询，消息内容同时匹配 ZH / EN / JA 译文。</p>
        </div>
        <div class="header-actions">
          <el-button type="primary" icon="el-icon-upload2" size="mini" @click="openImportDialog">导入</el-button>
        </div>
      </div>
      <el-form ref="queryForm" :model="formData" :inline="true" label-width="68px" @submit.native.prevent>
        <el-form-item label="消息 ID">
          <el-input v-model="formData.msgid" placeholder="BMXAA6378I" @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item label="消息组">
          <el-input v-model="formData.msggroup" placeholder="system" @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item label="消息键">
          <el-input v-model="formData.msgkey" placeholder="crontaskmanager25" @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item label="消息内容">
          <el-input v-model="formData.value" placeholder="模糊搜索 ZH/EN/JA 译文" @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item>
          <el-button type="cyan" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" size="mini" @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="result-panel">
        <div style="margin-bottom: 12px; text-align: right;">
          <el-button type="primary" icon="el-icon-download" size="mini" @click="openExportDialog">导 出</el-button>
        </div>
        <el-table
          :loading="loading"
          :data="messages"
          stripe
          style="width: 100%"
          class="result-table"
          :highlight-current-row="true"
          @row-click="handleRowClick"
        >
          <el-table-column prop="msgid" label="msgid" />
          <el-table-column prop="msggroup" label="msggroup" />
          <el-table-column prop="msgkey" label="msgkey" />
          <el-table-column prop="value_zh" label="value(ZH)" min-width="180" show-overflow-tooltip>
            <template slot-scope="scope">
              <span :class="{'ml-empty': !scope.row.value_zh}">{{ scope.row.value_zh || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="value_en" label="value(EN)" min-width="180" show-overflow-tooltip>
            <template slot-scope="scope">
              <span :class="{'ml-empty': !scope.row.value_en}">{{ scope.row.value_en || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="value_ja" label="value(JA)" min-width="180" show-overflow-tooltip>
            <template slot-scope="scope">
              <span :class="{'ml-empty': !scope.row.value_ja}">{{ scope.row.value_ja || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="displaymethod" label="displaymethod" />
        </el-table>
        <pagination
          v-show="tablePatam.total > 0"
          :total="tablePatam.total"
          :page.sync="tablePatam.pageNum"
          :limit.sync="tablePatam.pageSize"
          @pagination="getList"
        />
      </div>
    </el-card>

    <el-dialog :title="'消息详情 - ' + (currentRow ? currentRow.msgid : '')" :visible.sync="dialogVisible" width="1500px" :close-on-click-modal="true" @opened="onDialogOpened">
      <el-descriptions v-if="currentRow" :column="3" border>
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
        <el-descriptions-item label="消息内容(ZH)" :span="3">
          <el-input :value="currentRow.value_zh || '-'" readonly type="textarea" :rows="2" />
        </el-descriptions-item>
        <el-descriptions-item label="消息内容(EN)" :span="3">
          <el-input :value="currentRow.value_en || '-'" readonly type="textarea" :rows="2" />
        </el-descriptions-item>
        <el-descriptions-item label="消息内容(JA)" :span="3">
          <el-input :value="currentRow.value_ja || '-'" readonly type="textarea" :rows="2" />
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
            <div ref="simpleMonacoRef" class="monaco-container" />
          </div>
        </el-tab-pane>
        <el-tab-pane label="消息完整" name="full">
          <div class="json-toolbar">
            <el-button type="primary" size="mini" icon="el-icon-document" @click="copyFullJson">复制消息完整JSON</el-button>
          </div>
          <div v-loading="detailLoading" element-loading-text="加载中..." class="monaco-wrapper">
            <div ref="fullMonacoRef" class="monaco-container" />
          </div>
        </el-tab-pane>
        <el-tab-pane label="消息精简(可直接导入)" name="import-simple">
          <div class="json-toolbar">
            <el-button type="primary" size="mini" icon="el-icon-document-copy" @click="copyImportSimpleJson">复制可导入精简JSON</el-button>
          </div>
          <div v-loading="detailLoading" element-loading-text="加载中..." class="monaco-wrapper">
            <div ref="importSimpleMonacoRef" class="monaco-container" />
          </div>
        </el-tab-pane>
        <el-tab-pane label="消息完整(可直接导入)" name="import-full">
          <div class="json-toolbar">
            <el-button type="primary" size="mini" icon="el-icon-document" @click="copyImportFullJson">复制可导入完整JSON</el-button>
          </div>
          <div v-loading="detailLoading" element-loading-text="加载中..." class="monaco-wrapper">
            <div ref="importFullMonacoRef" class="monaco-container" />
          </div>
        </el-tab-pane>
      </el-tabs>

      <span slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">关 闭</el-button>
      </span>
    </el-dialog>

    <el-dialog title="导出消息(点击刷新之后获得导出的json)" :visible.sync="batchDialogVisible" width="1500px" :close-on-click-modal="true" @opened="onBatchDialogOpened" @closed="disposeBatchEditors">
      <div class="json-toolbar" style="margin-bottom: 12px;">
        <el-checkbox v-model="exportOptions.allLangCode">导出所有语言</el-checkbox>
        <el-checkbox v-model="exportOptions.withUuid" style="margin-left: 12px;">每行设置UUID</el-checkbox>
        <el-button size="mini" icon="el-icon-refresh" :loading="batchLoading" @click="loadExportData">刷 新</el-button>
        <span style="margin-left: 10px;" class="el-tag el-tag--info">共 {{ batchTotal }} 条记录</span>
      </div>
      <el-tabs v-model="batchActiveTab" type="border-card" class="json-tabs">
        <el-tab-pane label="消息精简" name="batch-simple">
          <div class="json-toolbar">
            <el-button type="primary" size="mini" icon="el-icon-document-copy" @click="copyBatchSimpleJson">复制消息精简JSON</el-button>
            <el-button type="primary" size="mini" icon="el-icon-download" @click="saveBatchSimpleJson">保存JSON</el-button>
          </div>
          <div class="monaco-wrapper">
            <div ref="batchSimpleMonacoRef" class="monaco-container" />
          </div>
        </el-tab-pane>
        <el-tab-pane label="消息完整" name="batch-full">
          <div class="json-toolbar">
            <el-button type="primary" size="mini" icon="el-icon-document" @click="copyBatchFullJson">复制消息完整JSON</el-button>
            <el-button type="primary" size="mini" icon="el-icon-download" @click="saveBatchFullJson">保存JSON</el-button>
          </div>
          <div class="monaco-wrapper">
            <div ref="batchFullMonacoRef" class="monaco-container" />
          </div>
        </el-tab-pane>
      </el-tabs>

      <span slot="footer" class="dialog-footer">
        <el-button @click="batchDialogVisible = false">关 闭</el-button>
      </span>
    </el-dialog>

    <!-- 导入消息弹窗: 选文件填入编辑框, 以编辑框内容为准 -->
    <el-dialog title="导入消息" :visible.sync="importDialog.visible" width="85%" top="4vh" custom-class="import-dialog" :close-on-click-modal="false" @opened="onImportDialogOpened" @closed="disposeImportEditor">
      <el-alert type="info" :closable="false" show-icon title="选择导出的消息 JSON 文件（支持 {messages:[...]} / 数组 / 单条消息对象）；内容会填入下方 JSON 编辑框，导入以编辑框内容为准" />
      <el-upload
        ref="importUpload"
        style="margin-top:12px;"
        :file-list="importDialog.fileList"
        :auto-upload="false"
        :limit="1"
        accept=".json"
        :on-change="onImportFileChange"
        :on-remove="onImportFileRemove"
      >
        <el-button size="small" icon="el-icon-folder-opened">选择 JSON 文件</el-button>
      </el-upload>
      <el-form label-width="110px" style="margin-top:12px;">
        <el-form-item label="启用迁移模式">
          <el-checkbox v-model="importDialog.dmDeploySet">dmDeploySet（MboSet 调用 setDMDeploySet(true)）</el-checkbox>
        </el-form-item>
        <el-form-item label="分批导入">
          <el-checkbox v-model="importDialog.batchImport">启用分批导入</el-checkbox>
          <span style="margin-left:16px;color:#909399;font-size:12px;">每批提交条数</span>
          <el-input-number
            v-model="importDialog.batchSize"
            :min="1"
            :max="5000"
            :disabled="!importDialog.batchImport"
            size="small"
            style="margin-left:8px;width:130px;"
          />
        </el-form-item>
      </el-form>
      <div ref="importJsonContainer" style="height:42vh;border:1px solid #dcdfe6" />
      <div v-if="importDialog.loading" class="import-result-bar">
        <span class="import-result-summary">{{ importDialog.summary }}</span>
      </div>
      <div v-if="importDialog.result" class="import-result-bar">
        <el-tag :type="importDialog.resultSuccess ? 'success' : 'danger'" size="small">{{ importDialog.resultSuccess ? '导入成功' : '导入失败' }}</el-tag>
        <span class="import-result-summary">{{ importDialog.summary }}</span>
        <el-button type="text" size="mini" icon="el-icon-document" @click="openImportResult">查看详细返回信息</el-button>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="importDialog.visible = false">关 闭</el-button>
        <el-button type="primary" :loading="importDialog.loading" @click="doImport">导 入</el-button>
      </span>
    </el-dialog>

    <!-- 导入详细返回信息弹窗 -->
    <el-dialog title="导入详细返回信息" :visible.sync="importResultDialog.visible" width="82%" top="5vh" :close-on-click-modal="false">
      <template v-if="importRows.length">
        <el-tabs v-model="importResultDialog.activeTab">
          <el-tab-pane label="列表" name="list">
            <vxe-table
              :data="importRows"
              border
              stripe
              size="mini"
              height="520"
              :virtual-y-config="{ enabled: true, gt: 50 }"
              style="width:100%"
            >
              <vxe-column field="ref" title="引用(sks:ref)" min-width="240" show-overflow="tooltip" />
              <vxe-column field="status" title="状态" width="90">
                <template #default="{ row }">
                  <el-tag v-if="row.status" :type="statusTagType(row.status)" size="mini">{{ row.status }}</el-tag>
                </template>
              </vxe-column>
              <vxe-column field="operation" title="操作" width="90" />
              <vxe-column field="message" title="消息" min-width="240" show-overflow="tooltip" />
            </vxe-table>
          </el-tab-pane>
          <el-tab-pane label="原始JSON" name="raw" lazy>
            <el-input :value="importDialog.result" type="textarea" :rows="22" readonly />
          </el-tab-pane>
        </el-tabs>
      </template>
      <el-input v-else :value="importDialog.result" type="textarea" :rows="22" readonly />
      <span slot="footer" class="dialog-footer">
        <el-button type="primary" @click="copyImportRawJson">复制原始JSON</el-button>
        <el-button @click="importResultDialog.visible = false">关 闭</el-button>
      </span>
    </el-dialog>
  </section>
</template>

<script>
import { migrateMessages } from '../../../api/messages'
import { getDefaultLangcode, loadLanguageConfig } from '../../../utils/mmp/mmp-config'
import { generateUUID } from '../../../utils/sks'

export default {
  name: 'MessagesQuery',
  data() {
    return {
      tablePatam: {
        total: 0,
        pageNum: 1,
        pageSize: 10
      },
      loading: false,
      batchLoading: false,
      detailLoading: false,
      importDialog: {
        visible: false,
        loading: false,
        dmDeploySet: false,
        batchImport: true,
        batchSize: 2000,
        fileList: [],
        text: '',
        result: '',
        resultData: null,
        resultSuccess: false,
        summary: ''
      },
      importResultDialog: {
        visible: false,
        activeTab: 'list'
      },
      importEditor: null,
      importRunId: 0,
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
      apiSimpleMessages: [],
      apiFullMessages: [],
      activeTab: 'simple',
      batchActiveTab: 'batch-simple',
      simpleEditor: null,
      fullEditor: null,
      importSimpleEditor: null,
      importFullEditor: null,
      monacoLoaded: false,
      _monaco: null,
      batchDialogVisible: false,
      exportOptions: {
        allLangCode: true,
        withUuid: false
      },
      batchSimpleJson: '',
      batchFullJson: '',
      batchTotal: 0,
      batchSimpleEditor: null,
      batchFullEditor: null
    }
  },
  computed: {
    importRows() {
      const payload = this.importDialog.resultData
      if (!payload) return []
      const rows = Array.isArray(payload.results) ? payload.results : []
      return rows.map(row => ({
        ref: (row && (row['sks:ref'] || ((row.msgGroup || '') + '/' + (row.msgKey || '')))) || '',
        status: (row && row._status) || '',
        operation: (row && row._operation) || '',
        message: (row && row._message) || ''
      }))
    }
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
  created() {
    loadLanguageConfig()
  },
  methods: {
    openImportDialog() {
      // 使上一次仍在执行的导入任务失效, 避免旧进度/结果重新写回
      this.importRunId += 1
      this.importDialog.visible = true
      this.importDialog.loading = false
      this.importDialog.dmDeploySet = false
      this.importDialog.fileList = []
      this.importDialog.text = ''
      this.importDialog.result = ''
      this.importDialog.resultData = null
      this.importDialog.resultSuccess = false
      this.importDialog.summary = ''
      if (this.$refs.importUpload) {
        this.$refs.importUpload.clearFiles()
      }
    },
    onImportDialogOpened() {
      if (!this.importEditor) {
        const create = monaco => {
          if (this.importEditor) return
          this.importEditor = monaco.editor.create(this.$refs.importJsonContainer, {
            value: this.importDialog.text || '',
            language: 'json',
            theme: 'vs',
            automaticLayout: true,
            minimap: { enabled: false },
            scrollBeyondLastLine: false,
            fontSize: 13,
            wordWrap: 'on',
            folding: true,
            lineNumbers: 'on'
          })
        }
        if (this.monacoLoaded && this._monaco) {
          create(this._monaco)
        } else {
          import(/* webpackChunkName: "monaco" */ 'monaco-editor').then(monaco => {
            this.monacoLoaded = true
            this._monaco = monaco
            create(monaco)
          }).catch(err => {
            console.error('Monaco Editor 加载失败:', err)
          })
        }
      } else {
        this.importEditor.layout()
      }
    },
    disposeImportEditor() {
      if (this.importEditor) {
        this.importEditor.dispose()
        this.importEditor = null
      }
    },
    getImportJson() {
      if (this.importEditor && typeof this.importEditor.getValue === 'function') {
        return this.importEditor.getValue()
      }
      return this.importDialog.text || ''
    },
    setImportJson(text) {
      this.importDialog.text = text
      if (this.importEditor) {
        this.importEditor.setValue(text)
      }
    },
    onImportFileChange(file) {
      const raw = file && file.raw
      if (!raw) return
      const reader = new FileReader()
      reader.onload = () => {
        const text = String(reader.result || '')
        this.setImportJson(text)
        try {
          const messages = this.parseImportMessages(text)
          this.$message.success('已读取 ' + messages.length + ' 条消息，确认后点击“导 入”')
        } catch (e) {
          this.$message.error(e.message || String(e))
        }
      }
      reader.onerror = () => {
        this.$message.error('读取文件失败')
      }
      reader.readAsText(raw, 'utf-8')
    },
    onImportFileRemove() {
      this.setImportJson('')
    },
    parseImportMessages(text) {
      let parsed
      try {
        parsed = JSON.parse(text)
      } catch (e) {
        throw new Error('JSON 解析失败: ' + e.message)
      }
      let messages
      if (Array.isArray(parsed)) {
        messages = parsed
      } else if (parsed && Array.isArray(parsed.messages)) {
        messages = parsed.messages
      } else if (parsed && Array.isArray(parsed.maxmessages)) {
        messages = parsed.maxmessages
      } else if (parsed && parsed.msgGroup && parsed.msgKey) {
        messages = [parsed]
      }
      if (!messages || messages.length === 0) {
        throw new Error('未找到可导入的消息数据（需要 messages 数组）')
      }
      return messages
    },
    extractImportResponse(res) {
      let data = (res && res.data !== undefined) ? res.data : res
      if (typeof data === 'string') {
        try {
          data = JSON.parse(data)
        } catch (e) {
          // 非 JSON 响应, 原样返回
        }
      }
      if (data && data.status === 'success' && data.data !== undefined) {
        return data.data
      }
      return data || {}
    },
    async doImport() {
      let messages
      try {
        messages = this.parseImportMessages(this.getImportJson())
      } catch (e) {
        this.$message.error(e.message || String(e))
        return
      }
      this.importDialog.loading = true
      this.importDialog.result = ''
      this.importDialog.resultData = null
      this.importDialog.resultSuccess = false
      this.importDialog.summary = ''
      const runId = ++this.importRunId
      await loadLanguageConfig()
      if (runId !== this.importRunId) return
      const batchSize = this.importDialog.batchImport
        ? Math.max(1, Number(this.importDialog.batchSize) || 500)
        : Math.max(1, messages.length)
      const params = { _action: 'import', _langcode: getDefaultLangcode() }
      if (this.importDialog.dmDeploySet) {
        params.dmDeploySet = 'true'
      }
      const merged = {
        total: 0,
        added: 0,
        updated: 0,
        deleted: 0,
        failed: 0,
        results: [],
        'sks:successSumTotal': 0,
        'sks:failSumTotal': 0
      }
      let completed = 0
      try {
        for (let offset = 0; offset < messages.length; offset += batchSize) {
          if (runId !== this.importRunId) return
          const batch = messages.slice(offset, offset + batchSize)
          this.importDialog.summary = '正在导入 ' + (offset + batch.length) + '/' + messages.length + ' 条...'
          const res = await migrateMessages(params, { messages: batch })
          if (runId !== this.importRunId) return
          const payload = this.extractImportResponse(res)
          merged.total += Number(payload.total || batch.length)
          merged.added += Number(payload.added || 0)
          merged.updated += Number(payload.updated || 0)
          merged.deleted += Number(payload.deleted || 0)
          merged.failed += Number(payload.failed || 0)
          merged['sks:successSumTotal'] += Number(payload['sks:successSumTotal'] || 0)
          merged['sks:failSumTotal'] += Number(payload['sks:failSumTotal'] || 0)
          if (Array.isArray(payload.results)) {
            merged.results = merged.results.concat(payload.results)
          }
          completed += batch.length
        }
        const failed = merged.failed !== undefined
          ? merged.failed
          : merged.results.filter(item => item && item._status !== 'success').length
        this.importDialog.resultData = merged
        this.importDialog.result = JSON.stringify(merged, null, 2)
        this.importDialog.resultSuccess = failed === 0
        this.importDialog.summary = '共 ' + (merged.total !== undefined ? merged.total : messages.length) +
          ' 条：新增 ' + (merged.added || 0) + '，更新 ' + (merged.updated || 0) +
          '，删除 ' + (merged.deleted || 0) + '，失败 ' + failed
        if (this.importDialog.resultSuccess) {
          this.$message.success(this.importDialog.summary)
        } else {
          this.$message.warning(this.importDialog.summary)
        }
        this.handleQuery()
      } catch (err) {
        if (runId !== this.importRunId) return
        const message = (err && err.message) ? err.message : String(err)
        this.importDialog.resultData = merged
        this.importDialog.result = JSON.stringify({ ...merged, error: message }, null, 2)
        this.importDialog.resultSuccess = false
        this.importDialog.summary = '已导入 ' + completed + '/' + messages.length + ' 条，后续失败：' + message
        this.$message.error('导入失败: ' + message)
      } finally {
        if (runId === this.importRunId) {
          this.importDialog.loading = false
        }
      }
    },
    openImportResult() {
      this.importResultDialog.activeTab = 'list'
      this.importResultDialog.visible = true
    },
    copyImportRawJson() {
      const text = this.importDialog.result || ''
      if (!text) {
        this.$message.warning('没有可复制的原始 JSON')
        return
      }
      if (navigator.clipboard && window.isSecureContext) {
        navigator.clipboard.writeText(text).then(() => {
          this.$message.success('原始 JSON 已复制')
        }).catch(() => {
          this.copyTextByTextarea(text)
        })
      } else {
        this.copyTextByTextarea(text)
      }
    },
    copyTextByTextarea(text) {
      const textarea = document.createElement('textarea')
      textarea.value = text
      textarea.setAttribute('readonly', 'readonly')
      textarea.style.position = 'fixed'
      textarea.style.top = '-9999px'
      document.body.appendChild(textarea)
      textarea.select()
      let copied = false
      try {
        copied = document.execCommand('copy')
      } catch (e) {
        copied = false
      }
      document.body.removeChild(textarea)
      if (copied) {
        this.$message.success('原始 JSON 已复制')
      } else {
        this.$message.error('复制失败，请切换到原始JSON标签手动复制')
      }
    },
    statusTagType(status) {
      if (status === 'success') return 'success'
      if (status === 'failed') return 'danger'
      return 'info'
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
    normalizeMessage(item) {
      const mask = parseInt(String(item['sks:options'] === undefined || item['sks:options'] === null ? 0 : item['sks:options']), 10) || 0
      return {
        ...item,
        msgid: item.msgId || item.msgid || '-',
        msggroup: item.msgGroup || item.msggroup || '-',
        msgkey: item.msgKey || item.msgkey || '-',
        value: item.value || '',
        value_zh: item.value_zh || null,
        value_en: item.value_en || item.value || null,
        value_ja: item.value_ja || null,
        displaymethod: item.displayMethod || item.displaymethod || '-',
        displaymethod_description: item.displayMethod || item.displaymethod || '-',
        ok: !!(mask & 2),
        yes: !!(mask & 8),
        no: !!(mask & 16),
        cancel: !!(mask & 4),
        close: !!(mask & 1),
        stop: !!(mask & 64),
        warning: !!(mask & 32),
        exclamation: !!(mask & 128)
      }
    },
    async handleQuery() {
      this.tablePatam.pageNum = 1
      this.getList()
    },
    async getList() {
      this.loading = true
      this.error = ''
      this.messages = []
      await loadLanguageConfig()

      // 直接传字段值, 脚本中处理搜索逻辑
      const body = {
        msgid: this.formData.msgid || undefined,
        msggroup: this.formData.msggroup || undefined,
        msgkey: this.formData.msgkey || undefined,
        value: this.formData.value || undefined
      }

      migrateMessages({
        _action: 'query',
        _langcode: getDefaultLangcode(),
        _includeExtra: 'true',
        pageNum: this.tablePatam.pageNum,
        pageSize: this.tablePatam.pageSize
      }, body).then(res => {
        const data = res.data || res
        if (data.status === 'error') {
          this.$message.error(data.message || '查询失败')
          this.loading = false
          return
        }
        const payload = data.data || data
        const list = payload.messages || []
        this.messages = list.map(item => this.normalizeMessage(item))
        this.tablePatam.total = payload.total || list.length
        this.loading = false
      }).catch(err => {
        console.error('查询失败', err)
        this.$message.error('查询消息失败：' + (err.message || String(err)))
        this.loading = false
      })
    },
    resetForm() {
      this.formData = {
        msgid: '',
        msggroup: '',
        msgkey: '',
        value: ''
      }
      this.error = ''
      this.tablePatam.pageNum = 1
      this.messages = []
    },
    async handleRowClick(row) {
      this.currentRow = row
      this.activeTab = 'simple'
      this.dialogVisible = true
      this.detailLoading = true
      this.apiSimpleMessages = []
      this.apiFullMessages = []
      await loadLanguageConfig()

      try {
        const detailParams = {
          _action: 'detail',
          _langcode: getDefaultLangcode(),
          _allLangcode: 'true',
          maxmessagesid: row.MAXMESSAGESID
        }

        const [simpleRes, fullRes] = await Promise.all([
          migrateMessages(detailParams, {}),
          migrateMessages({ ...detailParams, _includeExtra: 'true' }, {})
        ])

        const simpleData = simpleRes.data || simpleRes
        const fullData = fullRes.data || fullRes

        if (simpleData && simpleData.status === 'error') {
          this.$message.error('获取精简消息失败: ' + (simpleData.message || '未知错误'))
        } else if (simpleData && simpleData.data) {
          this.apiSimpleMessages = [simpleData.data]
        }

        if (fullData && fullData.status === 'error') {
          this.$message.error('获取完整消息失败: ' + (fullData.message || '未知错误'))
        } else if (fullData && fullData.data) {
          this.apiFullMessages = [fullData.data]
          this.currentRow = this.normalizeMessage({ ...row, ...fullData.data })
        }
      } catch (err) {
        console.warn('获取消息详情失败:', err)
        this.$message.error('获取消息详情失败: ' + (err.message || String(err)))
      } finally {
        this.detailLoading = false
        this.$nextTick(() => {
          setTimeout(() => {
            this.initMonacoEditors()
          }, 200)
        })
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
    saveBatchSimpleJson() {
      this.saveJsonFile(this.batchSimpleJson, '消息精简')
    },
    saveBatchFullJson() {
      this.saveJsonFile(this.batchFullJson, '消息完整')
    },
    saveJsonFile(text, name) {
      if (!text) {
        this.$message.warning('没有可保存的JSON')
        return
      }
      const blob = new Blob([text], { type: 'application/json;charset=utf-8' })
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = name + '-' + Date.now() + '.json'
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      URL.revokeObjectURL(url)
      this.$message.success('JSON 已保存')
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
    openExportDialog() {
      this.batchActiveTab = 'batch-simple'
      this.batchSimpleJson = ''
      this.batchFullJson = ''
      this.batchTotal = 0
      this.batchDialogVisible = true
    },
    async loadExportData() {
      if (!this.messages || this.messages.length === 0) {
        this.$message.warning('没有数据可导出，请先查询消息')
        return
      }

      this.batchLoading = true

      try {
        // 直接传字段值, 脚本中处理搜索逻辑
        const body = {
          msgid: this.formData.msgid || undefined,
          msggroup: this.formData.msggroup || undefined,
          msgkey: this.formData.msgkey || undefined,
          value: this.formData.value || undefined
        }

        const params = {
          _action: 'export',
          _langcode: getDefaultLangcode(),
          _allLangcode: this.exportOptions.allLangCode ? 'true' : 'false'
        }
        const [simpleRes, fullRes] = await Promise.all([
          migrateMessages(params, body),
          migrateMessages({ ...params, _includeExtra: 'true' }, body)
        ])

        const simpleData = simpleRes.data || simpleRes
        const fullData = fullRes.data || fullRes

        if (simpleData && simpleData.status === 'error') {
          this.$message.error('获取精简消息失败: ' + (simpleData.message || '未知错误'))
        }
        if (fullData && fullData.status === 'error') {
          this.$message.error('获取完整消息失败: ' + (fullData.message || '未知错误'))
        }

        const simpleMessages = simpleData && simpleData.data && simpleData.data.messages ? simpleData.data.messages : []
        const fullMessages = fullData && fullData.data && fullData.data.messages ? fullData.data.messages : []

        if (simpleMessages.length === 0 && fullMessages.length === 0) {
          this.$message.warning('未查询到消息数据')
          return
        }

        if (this.exportOptions.withUuid) {
          const uuidList = simpleMessages.map(() => generateUUID())
          simpleMessages.forEach((item, index) => {
            item['sks:uuid'] = uuidList[index]
          })
          fullMessages.forEach((item, index) => {
            item['sks:uuid'] = uuidList[index] || generateUUID()
          })
        }

        this.batchTotal = Math.max(simpleMessages.length, fullMessages.length)

        this.batchSimpleJson = JSON.stringify({ messages: simpleMessages }, null, 2)
        this.batchFullJson = JSON.stringify({ messages: fullMessages }, null, 2)
        this.$nextTick(() => {
          this.initBatchMonacoEditors()
        })
      } catch (err) {
        console.error('导出失败:', err)
        // 尝试从 AxiosError 中提取服务器返回的错误信息
        var serverMsg = ''
        if (err.response && err.response.data) {
          if (typeof err.response.data === 'string') {
            serverMsg = err.response.data.substring(0, 200)
          } else if (err.response.data.message) {
            serverMsg = err.response.data.message
          } else if (err.response.data['oslc:Error']) {
            serverMsg = err.response.data['oslc:Error']['oslc:message'] || ''
          } else {
            serverMsg = JSON.stringify(err.response.data).substring(0, 200)
          }
        }
        var displayMsg = serverMsg || err.message || String(err)
        this.$message.error('导出失败: ' + displayMsg)
      } finally {
        this.batchLoading = false
      }
    },
    initMonacoEditors() {
      let simpleJson = ''
      let fullJson = ''

      if (this.apiSimpleMessages && this.apiSimpleMessages.length > 0) {
        simpleJson = JSON.stringify(this.apiSimpleMessages[0], null, 2)
      } else if (this.currentRow) {
        simpleJson = JSON.stringify({
          msgGroup: this.currentRow.msggroup || null,
          msgKey: this.currentRow.msgkey || null,
          value: this.currentRow.value || null,
          value_zh: this.currentRow.value_zh || null,
          displayMethod: this.currentRow.displaymethod || null
        }, null, 2)
      }

      if (this.apiFullMessages && this.apiFullMessages.length > 0) {
        fullJson = JSON.stringify(this.apiFullMessages[0], null, 2)
      } else if (this.currentRow) {
        fullJson = JSON.stringify({
          msgGroup: this.currentRow.msggroup || null,
          msgKey: this.currentRow.msgkey || null,
          value: this.currentRow.value || null,
          value_zh: this.currentRow.value_zh || null,
          value_en: this.currentRow.value_en || null,
          value_ja: this.currentRow.value_ja || null,
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
      }

      if (!simpleJson && !fullJson) return

      // 可直接导入: 去掉查询专用字段, 统一包装成 { messages: [...] }
      const importSimpleJson = simpleJson ? this.buildImportJson(JSON.parse(simpleJson)) : ''
      const importFullJson = fullJson ? this.buildImportJson(JSON.parse(fullJson)) : ''

      this.createEditorsWithJson(simpleJson, fullJson, importSimpleJson, importFullJson)
    },
    buildImportJson(source) {
      if (!source) return ''
      const item = { ...source }
      // 跨环境导入不需要主键, 平铺的 EN/JA 也不参与导入(多语言走 sks:langdata)
      delete item.MAXMESSAGESID
      delete item.value_en
      delete item.value_ja
      delete item._status
      delete item['sks:ref']
      return JSON.stringify({ messages: [item] }, null, 2)
    },
    createEditorsWithJson(simpleJson, fullJson, importSimpleJson, importFullJson) {
      if (!this.monacoLoaded) {
        import(/* webpackChunkName: "monaco" */ 'monaco-editor').then(monaco => {
          this.monacoLoaded = true
          this._monaco = monaco
          this.createEditors(simpleJson, fullJson, importSimpleJson, importFullJson)
        }).catch(err => {
          console.error('Monaco Editor 加载失败:', err)
        })
      } else {
        this.createEditors(simpleJson, fullJson, importSimpleJson, importFullJson)
      }
    },
    createEditors(simpleJson, fullJson, importSimpleJson, importFullJson) {
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
      if (this.$refs.importSimpleMonacoRef && !this.importSimpleEditor) {
        this.importSimpleEditor = monaco.editor.create(this.$refs.importSimpleMonacoRef, {
          value: importSimpleJson,
          ...options
        })
      } else if (this.importSimpleEditor) {
        this.importSimpleEditor.setValue(importSimpleJson)
      }
      if (this.$refs.importFullMonacoRef && !this.importFullEditor) {
        this.importFullEditor = monaco.editor.create(this.$refs.importFullMonacoRef, {
          value: importFullJson,
          ...options
        })
      } else if (this.importFullEditor) {
        this.importFullEditor.setValue(importFullJson)
      }
      setTimeout(() => {
        this.layoutEditors()
      }, 100)
    },
    layoutEditors() {
      if (this.simpleEditor) this.simpleEditor.layout()
      if (this.fullEditor) this.fullEditor.layout()
      if (this.importSimpleEditor) this.importSimpleEditor.layout()
      if (this.importFullEditor) this.importFullEditor.layout()
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
      if (this.importSimpleEditor) {
        this.importSimpleEditor.dispose()
        this.importSimpleEditor = null
      }
      if (this.importFullEditor) {
        this.importFullEditor.dispose()
        this.importFullEditor = null
      }
    },
    copyImportSimpleJson() {
      if (!this.importSimpleEditor) return
      this.copyToClipboard(this.importSimpleEditor.getValue(), '消息精简(可直接导入)JSON')
    },
    copyImportFullJson() {
      if (!this.importFullEditor) return
      this.copyToClipboard(this.importFullEditor.getValue(), '消息完整(可直接导入)JSON')
    },
    copySimpleJson(row = this.currentRow) {
      if (!row) return
      if (this.apiSimpleMessages && this.apiSimpleMessages.length > 0) {
        const json = JSON.stringify(this.apiSimpleMessages[0], null, 2)
        this.copyToClipboard(json, '消息精简JSON')
        return
      }
      const json = JSON.stringify({
        msgGroup: row.msggroup || null,
        msgKey: row.msgkey || null,
        value: row.value || null,
        value_zh: row.value_zh || null,
        displayMethod: row.displaymethod || null
      }, null, 2)
      this.copyToClipboard(json, '消息精简JSON')
    },
    copyFullJson(row = this.currentRow) {
      if (!row) return
      if (this.apiFullMessages && this.apiFullMessages.length > 0) {
        const json = JSON.stringify(this.apiFullMessages[0], null, 2)
        this.copyToClipboard(json, '消息完整JSON')
        return
      }
      const json = JSON.stringify({
        msgGroup: row.msggroup || null,
        msgKey: row.msgkey || null,
        value: row.value || null,
        value_zh: row.value_zh || null,
        value_en: row.value_en || null,
        value_ja: row.value_ja || null,
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
.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
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
.ml-empty {
  color: #c0c4cc;
  font-style: italic;
}
</style>

<style lang="scss">
/* 弹窗被挂载到 body, 需用非 scoped 样式 */
.import-dialog .el-dialog__body {
  max-height: 74vh;
  overflow-y: auto;
}
.import-result-bar {
  margin-top: 12px;
  display: flex;
  align-items: center;
}
.import-result-summary {
  margin: 0 8px;
  color: #606266;
  font-size: 13px;
}
</style>
