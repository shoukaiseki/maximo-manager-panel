<template>
  <section class="query-page">
    <el-card>
      <div class="page-header-row">
        <div>
          <h2>应用XML查询</h2>
          <p class="page-summary">查询 MAXPRESENTATION 数据，支持 APP、描述、XML内容搜索。</p>
        </div>
        <div class="page-actions">
          <el-button type="primary" size="mini" icon="el-icon-upload2" @click="openImport">导入应用信息</el-button>
        </div>
      </div>

      <el-form :model="formData" ref="queryForm" :inline="true" label-width="80px" @submit.native.prevent>
        <el-form-item label="APP">
          <el-input v-model="formData.app" placeholder="=精确/%模糊" clearable style="width: 220px;" @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="formData.description" placeholder="描述关键词" clearable style="width: 220px;" @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item label="XML内容">
          <el-input v-model="formData.content" placeholder="PRESENTATION 模糊搜索" clearable style="width: 200px;" @keyup.enter.native="handleQuery" />
          <el-radio-group v-model="contentCaseSensitive" size="mini" style="margin-left:8px;vertical-align:middle;">
            <el-radio-button :label="false">不区分大小写(慢)</el-radio-button>
            <el-radio-button :label="true">区分大小写(快)</el-radio-button>
          </el-radio-group>
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
          <template slot="tableColumnList-after">
            <el-table-column label="操作" width="380" fixed="right">
              <template slot-scope="scope">
                <el-button type="text" size="small" @click.stop="showSource(scope.row)">查看源码</el-button>
                <el-button type="text" size="small" @click.stop="showDetail(scope.row)">查看详情</el-button>
                <el-button type="text" size="small" @click.stop="showAudit(scope.row)">审计记录</el-button>
                <el-button type="text" size="small" @click.stop="showAppInfo(scope.row)">导出应用信息</el-button>
                <el-button type="text" size="small" @click.stop="showLangXml(scope.row)">查看当前语言xml</el-button>
              </template>
            </el-table-column>
          </template>
          <template slot="default">
          </template>
        </SksTable>
        <el-empty v-if="!loading && total === 0 && hasSearched" description="暂无查询结果" />
        <el-empty v-if="!loading && total === 0 && !hasSearched" description="请输入关键词后点击搜索" />
      </div>
    </el-card>

    <!-- 源码弹窗 -->
    <el-dialog :title="'源码: ' + sourceDialog.app" :visible.sync="sourceDialog.visible" width="80%" top="5vh" :close-on-click-modal="false" @opened="onSourceDialogOpened">
      <div v-loading="sourceDialog.loading">
        <div v-if="sourceDialog.content" ref="sourceMonacoContainer" style="height:65vh;border:1px solid #dcdfe6"></div>
        <el-empty v-else-if="!sourceDialog.loading" description="暂无源码" />
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="sourceDialog.visible = false">关 闭</el-button>
      </span>
    </el-dialog>

    <!-- 应用信息 / 当前语言XML 弹窗 -->
    <el-dialog :title="infoDialog.title" :visible.sync="infoDialog.visible" width="85%" top="5vh" :close-on-click-modal="false" @opened="onInfoDialogOpened">
      <div v-loading="infoDialog.loading">
        <template v-if="infoDialog.mode === 'json'">
          <div class="info-toolbar">
            <el-checkbox v-model="infoDialog.allLangcode" @change="reloadAppInfo">导出所有语言信息</el-checkbox>
            <el-button size="mini" icon="el-icon-refresh" :loading="infoDialog.loading" @click="reloadAppInfo">重 载</el-button>
            <el-button size="mini" icon="el-icon-copy-document" :disabled="!infoDialog.appInfo" @click="copyJson">复制JSON</el-button>
            <el-button size="mini" icon="el-icon-document" :disabled="!infoDialog.appInfo" @click="saveJsonFile">保存JSON</el-button>
          </div>
          <el-tabs v-model="infoDialog.activeTab">
            <el-tab-pane label="常用" name="common">
              <div ref="infoCommonContainer" class="monaco-pane"></div>
            </el-tab-pane>
            <el-tab-pane label="完整json" name="full">
              <div ref="infoFullContainer" class="monaco-pane"></div>
            </el-tab-pane>
          </el-tabs>
        </template>
        <template v-else>
          <div v-if="infoDialog.content" ref="infoMonacoContainer" style="height:65vh;border:1px solid #dcdfe6"></div>
          <el-empty v-else-if="!infoDialog.loading" description="暂无内容" />
        </template>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button v-if="canExportZip" type="primary" size="mini" icon="el-icon-download" :loading="zipDownloading" @click="downloadZip">导出 MAFAPPDATA zip</el-button>
        <el-button v-if="canExportXml" type="primary" size="mini" icon="el-icon-download" :loading="xmlDownloading" @click="downloadXml">导出应用XML</el-button>
        <el-button @click="infoDialog.visible = false">关 闭</el-button>
      </span>
    </el-dialog>

    <!-- 导入应用信息弹窗 -->
    <el-dialog title="导入应用信息" :visible.sync="importDialog.visible" width="85%" top="4vh" custom-class="import-dialog" :close-on-click-modal="false" @opened="onImportDialogOpened">
      <el-alert type="info" :closable="false" show-icon title="选择导出的应用信息 JSON 文件；内容会填入下方 JSON 编辑框，导入以编辑框内容为准，应用名取自 JSON 中的 app" />
      <el-upload
        ref="importUpload"
        style="margin-top:12px;"
        :file-list="importDialog.fileList"
        :auto-upload="false"
        :limit="1"
        accept=".json"
        :on-change="onImportFileChange"
        :on-remove="onImportFileRemove">
        <el-button size="small" icon="el-icon-folder-opened">选择 JSON 文件</el-button>
      </el-upload>
      <el-form label-width="110px" style="margin-top:12px;">
        <el-form-item label="启用迁移模式">
          <el-checkbox v-model="importDialog.dmDeploySet">dmDeploySet（MboSet 调用 setDMDeploySet(true)）</el-checkbox>
        </el-form-item>
      </el-form>
      <div ref="importJsonContainer" style="height:42vh;border:1px solid #dcdfe6"></div>
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
    <el-dialog title="导入详细返回信息" :visible.sync="importResultDialog.visible" width="82%" top="5vh" :close-on-click-modal="false" @opened="onImportResultOpened">
      <template v-if="importGroups.length">
        <div class="import-result-toolbar">
          <el-button type="text" size="mini" icon="el-icon-circle-plus-outline" @click="expandAllImport">展开所有</el-button>
          <el-button type="text" size="mini" icon="el-icon-remove-outline" @click="collapseAllImport">收缩所有</el-button>
        </div>
        <el-tabs v-model="importResultDialog.activeTab">
          <el-tab-pane label="列表" name="list">
            <el-table ref="importListTable" :data="importGroups" row-key="key" border stripe size="mini" max-height="58vh" :tree-props="{children:'children'}" style="width:100%">
              <el-table-column prop="key" label="对象" width="150" />
              <el-table-column prop="objectName" label="对象名称" width="160" />
              <el-table-column prop="ref" label="引用(sks:ref)" min-width="220" show-overflow-tooltip />
              <el-table-column label="状态" width="90">
                <template slot-scope="scope">
                  <el-tag v-if="scope.row.status" :type="statusTagType(scope.row.status)" size="mini">{{ scope.row.status }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="operation" label="操作" width="90" />
              <el-table-column prop="success" label="成功" width="70" />
              <el-table-column prop="failed" label="失败" width="70" />
              <el-table-column prop="message" label="消息" min-width="220" show-overflow-tooltip />
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="树结构" name="tree">
            <el-tree ref="importTree" :key="'import-tree-' + importResultDialog.treeKey" :data="importTreeData" node-key="id" :default-expand-all="importResultDialog.expandAll" :expand-on-click-node="false" style="max-height:58vh;overflow:auto">
              <span slot-scope="{ node, data }" class="import-tree-node">
                <el-tag v-if="data.statusLabel" :type="statusTagType(data.statusLabel)" size="mini" style="margin-right:6px;">{{ data.statusLabel }}</el-tag>
                <span class="import-tree-label">{{ data.label }}</span>
              </span>
            </el-tree>
          </el-tab-pane>
          <el-tab-pane label="原始JSON" name="raw">
            <el-input :value="importDialog.result" type="textarea" :rows="22" readonly />
          </el-tab-pane>
        </el-tabs>
      </template>
      <el-input v-else :value="importDialog.result" type="textarea" :rows="22" readonly />
      <span slot="footer" class="dialog-footer">
        <el-button @click="importResultDialog.visible = false">关 闭</el-button>
      </span>
    </el-dialog>

    <!-- 审计记录弹窗 -->
    <el-dialog :title="'应用XML审计记录: ' + auditDialog.app" :visible.sync="auditDialog.visible" width="90%" top="3vh" :close-on-click-modal="false" append-to-body destroy-on-close>
      <div style="max-height:82vh;overflow-y:auto">
        <AuditPanel v-if="auditDialog.visible" :app="auditDialog.app" embedded />
      </div>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog :title="'详情: ' + (detailRow ? detailRow.APP : '')" :visible.sync="detailDialog.visible" width="700px" :close-on-click-modal="true">
      <div v-loading="detailLoading">
        <template v-if="detailRow">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="APP">
              <el-input :value="detailRow.APP" readonly size="small" />
            </el-descriptions-item>
            <el-descriptions-item label="MAXPRESENTATIONID">
              <el-input :value="detailRow.MAXPRESENTATIONID" readonly size="small" />
            </el-descriptions-item>
            <el-descriptions-item label="英文描述">
              <el-input :value="detailRow.DESCRIPTION || '-'" readonly size="small" />
            </el-descriptions-item>
            <el-descriptions-item label="中文描述">
              <el-input :value="detailRow.L_DESCRIPTION || '-'" readonly size="small" />
            </el-descriptions-item>
          </el-descriptions>
        </template>
        <el-empty v-else-if="!detailLoading" description="未找到详情" />
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="detailDialog.visible = false">关 闭</el-button>
      </span>
    </el-dialog>
  </section>
</template>

<script>
import { getMaxAppXmlList, getMaxAppXmlSource, getMaxAppXmlDetail, exportAppInfo, getAppXmlByLang, importAppInfo, downloadAppZip } from '@/api/maxappxml'
import AuditPanel from './AuditPanel.vue'
import { sksPageMixin } from "sks-plugin-el-erp/lib/sks-page";

export default {
  name: 'MaxAppXmlList',
  mixins: [sksPageMixin],
  components: { AuditPanel },
  data() {
    return {
      loading: false,
      hasSearched: false,
      total: 0,
      contentCaseSensitive: true,
      formData: {
        app: '',
        description: '',
        content: ''
      },
      // 源码弹窗
      sourceDialog: {
        visible: false,
        loading: false,
        app: '',
        content: ''
      },
      sourceEditor: null,
      // 应用信息 / 当前语言XML 弹窗
      infoDialog: {
        visible: false,
        loading: false,
        mode: 'json',
        title: '',
        app: '',
        content: '',
        appInfo: null,
        allLangcode: true,
        activeTab: 'common'
      },
      infoEditor: null,
      jsonEditors: {},
      importEditor: null,
      zipDownloading: false,
      xmlDownloading: false,
      // 导入应用信息
      importDialog: {
        visible: false,
        loading: false,
        dmDeploySet: false,
        fileList: [],
        text: '',
        result: '',
        resultData: null,
        resultSuccess: false,
        summary: ''
      },
      // 导入详细返回信息弹窗
      importResultDialog: {
        visible: false,
        activeTab: 'list',
        expandAll: true,
        treeKey: 0
      },
      // 详情弹窗
      detailDialog: {
        visible: false
      },
      detailLoading: false,
      detailRow: null,
      // 审计记录弹窗
      auditDialog: {
        visible: false,
        app: ''
      },
      monacoLoaded: false
    }
  },
  watch: {
    'sourceDialog.visible'(val) {
      if (!val) this.disposeSourceEditor()
    },
    'infoDialog.visible'(val) {
      if (!val) this.disposeInfoEditor()
    },
    'importDialog.visible'(val) {
      if (!val) this.disposeImportEditor()
    }
  },
  computed: {
    // 导出信息中只有一个应用时，存在对应记录才显示下载按钮
    canExportZip() {
      return !!(this.infoDialog.appInfo && this.infoDialog.appInfo.mafappdata)
    },
    canExportXml() {
      return !!(this.infoDialog.appInfo && this.infoDialog.appInfo.presentation)
    },
    // 导入结果按对象分组(列表标签页, 父行为对象, 子行为导入记录)
    importGroups() {
      const root = this.importDialog.resultData
      if (!root || !root.data || typeof root.data !== 'object') return []
      return Object.keys(root.data).map(key => {
        const list = Array.isArray(root.data[key]) ? root.data[key] : [root.data[key]]
        const rows = list.filter(row => !!row)
        const first = rows[0] || {}
        const success = rows.filter(row => row._status === 'success').length
        const failed = rows.length - success
        return {
          key: key,
          objectName: first['sks:objectName'] || '',
          success: success,
          failed: failed,
          status: failed === 0 ? 'success' : 'failed',
          children: rows.map((row, idx) => ({
            key: key + '#' + idx,
            ref: row['sks:ref'] || '',
            status: row._status || '',
            operation: row._operation || '',
            message: row._message || (row['sks:currentCacheUseError'] ? JSON.stringify(row['sks:currentCacheUseError']) : ''),
            raw: row
          }))
        }
      })
    },
    // 导入结果树(对象 -> 导入记录 -> 嵌套子记录, 如 sigoption flags)
    importTreeData() {
      const groups = this.importGroups
      const nodes = []
      for (let i = 0; i < groups.length; i++) {
        const group = groups[i]
        nodes.push({
          id: 'obj#' + group.key,
          label: (group.objectName || group.key) + '（' + group.key + '）：成功 ' + group.success + ' / ' + (group.success + group.failed),
          statusLabel: group.status,
          children: this.buildImportTreeRows('obj#' + group.key, groups[i].children)
        })
      }
      return nodes
    }
  },
  methods: {
    initMainTableParam() {
      return {
        ownerName: 'maxappxml',
        uniqueId: 'maxappxml-list',
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
            { prop: 'APP', label: 'APP', minWidth: 200 },
            { prop: 'L_DESCRIPTION', label: '中文描述', minWidth: 200 },
            { prop: 'DESCRIPTION', label: '英文描述', minWidth: 200 },
            { prop: 'MAXPRESENTATIONID', label: 'PRESENTATION ID', width: 140 }
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
        app: this.formData.app,
        description: this.formData.description,
        content: this.formData.content,
        contentCaseSensitive: this.contentCaseSensitive,
        pageNum: this.mainTable.queryParams.pageNum,
        pageSize: this.mainTable.queryParams.pageSize
      }
      getMaxAppXmlList(params)
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
    resetForm() {
      this.formData = {
        app: '',
        description: '',
        content: ''
      }
      this.contentCaseSensitive = true
      this.hasSearched = false
      this.mainTable.list = []
      this.mainTable.total = 0
      this.total = 0
      this.mainTable.currentPage = 1
    },
    handleRowClick(row) {
      // 点击行不做特殊处理
    },
    // === XML 美化 ===
    formatXml(xml) {
      if (!xml) return ''
      let formatted = ''
      let indent = ''
      const tab = '  '
      // 在 >< 之间加换行
      xml = xml.replace(/>\s*</g, '>\n<')
      xml = xml.trim()
      const lines = xml.split('\n')
      for (let i = 0; i < lines.length; i++) {
        const line = lines[i].trim()
        if (!line) continue
        // 结束标签减少缩进
        if (line.match(/^<\//)) {
          indent = indent.substring(tab.length)
        }
        formatted += indent + line + '\n'
        // 开始标签（非自闭合、非结束标签）增加缩进
        if (line.match(/^<[^>]*[^/]>$/) && !line.match(/^<\//) && !line.match(/\/>$/)) {
          indent += tab
        }
        // 注释缩进
        if (line.match(/^<!--/) && !line.match(/-->$/)) {
          indent += tab
        }
        if (!line.match(/^<!--/) && line.match(/-->$/)) {
          indent = indent.substring(tab.length)
        }
        // CDATA 缩进
        if (line.match(/^<!\[CDATA\[/) && !line.match(/\]\]>$/)) {
          indent += tab
        }
        if (!line.match(/^<!\[CDATA\[/) && line.match(/\]\]>$/)) {
          indent = indent.substring(tab.length)
        }
      }
      return formatted.trim()
    },
    // === 查看源码 ===
    showSource(row) {
      this.sourceDialog.app = row.APP
      this.sourceDialog.content = ''
      this.sourceDialog.loading = true
      this.sourceDialog.visible = true
      getMaxAppXmlSource(row.APP)
        .then(res => {
          if (res.code === 200 && res.data) {
            this.sourceDialog.content = this.formatXml(res.data)
          } else {
            this.$message.error('获取源码失败: ' + (res.message || '未知错误'))
          }
        })
        .catch(err => {
          this.$message.error('获取源码失败: ' + (err.message || String(err)))
        })
        .finally(() => {
          this.sourceDialog.loading = false
        })
    },
    onSourceDialogOpened() {
      this.$nextTick(() => {
        setTimeout(() => {
          this.initSourceEditor()
        }, 200)
      })
    },
    initSourceEditor() {
      if (!this.sourceDialog.content) return
      if (!this.monacoLoaded) {
        import(/* webpackChunkName: "monaco" */ 'monaco-editor').then(monaco => {
          this.monacoLoaded = true
          this._monaco = monaco
          this.createSourceEditor(monaco)
        }).catch(err => {
          console.error('Monaco Editor 加载失败:', err)
        })
      } else {
        this.createSourceEditor(this._monaco)
      }
    },
    createSourceEditor(monaco) {
      if (this.$refs.sourceMonacoContainer && !this.sourceEditor) {
        this.sourceEditor = monaco.editor.create(this.$refs.sourceMonacoContainer, {
          value: this.sourceDialog.content,
          language: 'xml',
          readOnly: true,
          theme: 'vs',
          automaticLayout: true,
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
        })
      }
    },
    disposeSourceEditor() {
      if (this.sourceEditor) {
        this.sourceEditor.dispose()
        this.sourceEditor = null
      }
    },
    // === 审计记录 ===
    showAudit(row) {
      this.auditDialog.app = row.APP
      this.auditDialog.visible = true
    },
    // === 导出应用信息（多标签页：常用 / 完整json） ===
    showAppInfo(row) {
      this.infoDialog.mode = 'json'
      this.infoDialog.app = row.APP
      this.infoDialog.title = '应用信息: ' + row.APP
      this.infoDialog.activeTab = 'common'
      this.infoDialog.appInfo = null
      this.infoDialog.content = ''
      this.disposeInfoEditor()
      this.infoDialog.visible = true
      this.loadAppInfo()
    },
    reloadAppInfo() {
      this.loadAppInfo()
    },
    // 获取当前标签页的 JSON 文本(优先编辑器内容, 未就绪时按导出数据生成)
    getActiveJson() {
      const tab = this.infoDialog.activeTab || 'common'
      const editors = this.jsonEditors || {}
      if (editors[tab] && typeof editors[tab].getValue === 'function') {
        return editors[tab].getValue()
      }
      if (!this.infoDialog.appInfo) {
        return ''
      }
      const value = tab === 'full' ? { maxapps: [this.infoDialog.appInfo] } : this.infoDialog.appInfo
      return JSON.stringify(value, null, 2)
    },
    // 复制当前标签页的 JSON(常用/完整json)
    copyJson() {
      const text = this.getActiveJson()
      if (!text) {
        this.$message.warning('没有可复制的内容')
        return
      }
      const fallback = () => {
        const ta = document.createElement('textarea')
        ta.value = text
        document.body.appendChild(ta)
        ta.select()
        document.execCommand('copy')
        document.body.removeChild(ta)
        this.$message.success('JSON 已复制到剪贴板')
      }
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(text).then(() => {
          this.$message.success('JSON 已复制到剪贴板')
        }).catch(fallback)
      } else {
        fallback()
      }
    },
    // 保存当前标签页 JSON 到文件, 默认文件名 MAXAPPINFO_<应用名>.json
    saveJsonFile() {
      const text = this.getActiveJson()
      if (!text) {
        this.$message.warning('没有可保存的内容')
        return
      }
      const defaultName = 'MAXAPPINFO_' + (this.infoDialog.app || 'APP') + '.json'
      this.$prompt('请输入文件名', '保存JSON到文件', {
        inputValue: defaultName,
        confirmButtonText: '保 存',
        cancelButtonText: '取 消'
      }).then(({ value }) => {
        let fileName = (value || '').trim() || defaultName
        if (!/\.json$/i.test(fileName)) {
          fileName += '.json'
        }
        this.saveBlob(new Blob([text], { type: 'application/json;charset=utf-8' }), fileName)
      }).catch(() => { /* 取消 */ })
    },
    loadAppInfo() {
      const app = this.infoDialog.app
      this.infoDialog.loading = true
      exportAppInfo(app, { allLangcode: this.infoDialog.allLangcode })
        .then(res => {
          let data = (res && res.data !== undefined) ? res.data : res
          if (typeof data === 'string') {
            try { data = JSON.parse(data) } catch (e) { /* 非JSON字符串，原样展示 */ }
          }
          if (!data || data.status === 'error') {
            this.$message.error((data && data.message) || '导出应用信息失败')
            this.infoDialog.appInfo = null
            return
          }
          this.infoDialog.appInfo = data
          this.infoDialog.content = JSON.stringify(data, null, 2)
          this.$nextTick(() => {
            if (this.infoDialog.visible) this.initInfoEditor()
          })
        })
        .catch(err => {
          this.$message.error('导出应用信息失败: ' + (err.message || String(err)))
          this.infoDialog.appInfo = null
        })
        .finally(() => {
          this.infoDialog.loading = false
        })
    },
    // 导出 MAFAPPDATA 的 zip 字节码
    downloadZip() {
      if (!this.infoDialog.app) return
      this.zipDownloading = true
      downloadAppZip(this.infoDialog.app)
        .then(res => {
          const blob = new Blob([res.data], { type: 'application/octet-stream' })
          this.saveBlob(blob, this.infoDialog.app + '.zip')
        })
        .catch(err => {
          this.$message.error('导出 zip 失败: ' + (err.message || String(err)))
        })
        .finally(() => {
          this.zipDownloading = false
        })
    },
    // 导出应用 XML（MAXPRESENTATION.PRESENTATION）
    downloadXml() {
      if (!this.infoDialog.app) return
      this.xmlDownloading = true
      exportAppInfo(this.infoDialog.app, { include: 'presentation', allLangcode: this.infoDialog.allLangcode })
        .then(res => {
          let data = (res && res.data !== undefined) ? res.data : res
          if (typeof data === 'string') {
            try { data = JSON.parse(data) } catch (e) { /* ignore */ }
          }
          const xml = (data && data.presentation && data.presentation.PRESENTATION) ? data.presentation.PRESENTATION : ''
          if (!xml) {
            this.$message.error('该应用没有 XML 数据')
            return
          }
          this.saveBlob(new Blob([xml], { type: 'application/xml;charset=utf-8' }), this.infoDialog.app + '.xml')
        })
        .catch(err => {
          this.$message.error('导出应用XML失败: ' + (err.message || String(err)))
        })
        .finally(() => {
          this.xmlDownloading = false
        })
    },
    saveBlob(blob, fileName) {
      if (window.navigator && window.navigator.msSaveOrOpenBlob) {
        window.navigator.msSaveOrOpenBlob(blob, fileName)
        return
      }
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = fileName
      a.click()
      window.URL.revokeObjectURL(url)
    },
    // === 查看当前语言 XML ===
    showLangXml(row) {
      const langcode = 'ZH'
      this.infoDialog.mode = 'xml'
      this.infoDialog.title = '当前语言XML(' + langcode + '): ' + row.APP
      this.infoDialog.appInfo = null
      this.disposeInfoEditor()
      this.infoDialog.content = ''
      this.infoDialog.loading = true
      this.infoDialog.visible = true
      getAppXmlByLang(row.APP, langcode)
        .then(res => {
          let data = (res && res.data !== undefined) ? res.data : res
          if (typeof data === 'string') {
            try { data = JSON.parse(data) } catch (e) { /* 非JSON字符串，原样展示 */ }
          }
          const xml = (data && data.presentation) ? data.presentation : ''
          if (!xml) {
            this.$message.error('获取当前语言XML失败: ' + ((data && data.message) || '无内容'))
            this.infoDialog.visible = false
            return
          }
          this.infoDialog.content = this.formatXml(xml)
          this.$nextTick(() => {
            if (this.infoDialog.visible) this.initInfoEditor()
          })
        })
        .catch(err => {
          this.$message.error('获取当前语言XML失败: ' + (err.message || String(err)))
          this.infoDialog.visible = false
        })
        .finally(() => {
          this.infoDialog.loading = false
        })
    },
    onInfoDialogOpened() {
      this.$nextTick(() => {
        setTimeout(() => {
          this.initInfoEditor()
        }, 200)
      })
    },
    initInfoEditor() {
      if (this.infoDialog.mode === 'json') {
        if (!this.infoDialog.appInfo) return
      } else if (!this.infoDialog.content) {
        return
      }
      this.loadMonaco(monaco => {
        if (this.infoDialog.mode === 'json') {
          this.createJsonEditors(monaco)
        } else {
          this.createInfoEditor(monaco)
        }
      })
    },
    loadMonaco(callback) {
      if (this.monacoLoaded) {
        callback(this._monaco)
        return
      }
      import(/* webpackChunkName: "monaco" */ 'monaco-editor').then(monaco => {
        this.monacoLoaded = true
        this._monaco = monaco
        callback(monaco)
      }).catch(err => {
        console.error('Monaco Editor 加载失败:', err)
      })
    },
    monacoOptions(value, language) {
      return {
        value: value,
        language: language,
        readOnly: true,
        theme: 'vs',
        automaticLayout: true,
        minimap: { enabled: false },
        scrollBeyondLastLine: false,
        fontSize: 13,
        wordWrap: 'on',
        folding: true,
        lineNumbers: 'on',
        scrollbar: {
          verticalScrollbarSize: 10,
          horizontalScrollbarSize: 10
        }
      }
    },
    // 常用标签页: 原始导出 JSON; 完整json标签页: 外层包 {"maxapps":[{单个应用信息}]}
    createJsonEditors(monaco) {
      const appInfo = this.infoDialog.appInfo
      const commonValue = JSON.stringify(appInfo, null, 2)
      const fullValue = JSON.stringify({ maxapps: [appInfo] }, null, 2)
      const editors = this.jsonEditors || (this.jsonEditors = {})
      if (this.$refs.infoCommonContainer) {
        if (editors.common) {
          editors.common.setValue(commonValue)
        } else {
          editors.common = monaco.editor.create(this.$refs.infoCommonContainer, this.monacoOptions(commonValue, 'json'))
        }
      }
      if (this.$refs.infoFullContainer) {
        if (editors.full) {
          editors.full.setValue(fullValue)
        } else {
          editors.full = monaco.editor.create(this.$refs.infoFullContainer, this.monacoOptions(fullValue, 'json'))
        }
      }
    },
    createInfoEditor(monaco) {
      if (this.$refs.infoMonacoContainer && !this.infoEditor) {
        this.infoEditor = monaco.editor.create(this.$refs.infoMonacoContainer, this.monacoOptions(this.infoDialog.content, 'xml'))
      }
    },
    disposeInfoEditor() {
      if (this.infoEditor) {
        this.infoEditor.dispose()
        this.infoEditor = null
      }
      const editors = this.jsonEditors || {}
      Object.keys(editors).forEach(key => {
        if (editors[key]) editors[key].dispose()
      })
      this.jsonEditors = {}
    },
    // === 导入应用信息 ===
    openImport() {
      this.importDialog.visible = true
      this.importDialog.loading = false
      this.importDialog.dmDeploySet = false
      this.importDialog.fileList = []
      this.importDialog.text = ''
      this.importDialog.result = ''
      this.importDialog.resultData = null
      this.importDialog.resultSuccess = false
      this.importDialog.summary = ''
      this.importResultDialog.visible = false
      this.importResultDialog.activeTab = 'list'
      this.$nextTick(() => {
        if (this.$refs.importUpload) this.$refs.importUpload.clearFiles()
      })
    },
    onImportDialogOpened() {
      this.$nextTick(() => {
        setTimeout(() => {
          this.initImportEditor()
        }, 200)
      })
    },
    // JSON 编辑框: 文件内容填入后可手动修改, 导入时以编辑框内容为准
    initImportEditor() {
      if (!this.$refs.importJsonContainer || this.importEditor) return
      this.loadMonaco(monaco => {
        if (this.importEditor) return
        const options = Object.assign({}, this.monacoOptions(this.importDialog.text || '', 'json'), { readOnly: false })
        this.importEditor = monaco.editor.create(this.$refs.importJsonContainer, options)
      })
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
    // 兼容「完整json」标签页格式: {"maxapps":[单个应用信息]}
    parseImportJson(text) {
      const parsed = JSON.parse(text)
      const payload = Array.isArray(parsed.maxapps) ? (parsed.maxapps[0] || null) : parsed
      if (!payload) {
        throw new Error('JSON 中没有应用信息')
      }
      return payload
    },
    onImportFileChange(file) {
      this.importDialog.fileList = [file]
      this.importDialog.result = ''
      this.importDialog.resultData = null
      this.importDialog.resultSuccess = false
      this.importDialog.summary = ''
      const reader = new FileReader()
      reader.onload = e => {
        const text = String(e.target.result || '')
        this.setImportJson(text)
        try {
          const payload = this.parseImportJson(text)
          const app = this.resolveImportApp(payload)
          this.$message.success('已读取文件: ' + file.name + (app ? '（应用: ' + app + '）' : ''))
        } catch (err) {
          this.$message.error('JSON 解析失败: ' + (err.message || String(err)))
        }
      }
      reader.readAsText(file.raw, 'UTF-8')
    },
    onImportFileRemove() {
      this.importDialog.fileList = []
      this.importDialog.result = ''
      this.setImportJson('')
    },
    // 应用名以 JSON 内容为准: app 字段, 兼容 {"maxapps":[...]} 外层包装
    resolveImportApp(payload) {
      if (!payload) return ''
      const raw = payload.app || (payload.maxapps && payload.maxapps.APP ? payload.maxapps.APP : payload.APP)
      return String(raw || '').trim().toUpperCase()
    },
    doImport() {
      const text = this.getImportJson()
      if (!String(text || '').trim()) {
        this.$message.warning('请先选择 JSON 文件或在编辑框中填写内容')
        return
      }
      let importPayload = null
      try {
        importPayload = this.parseImportJson(text)
      } catch (err) {
        this.$message.error('JSON 解析失败: ' + (err.message || String(err)))
        return
      }
      const app = this.resolveImportApp(importPayload)
      if (!app) {
        this.$message.warning('JSON 中缺少应用名(app)')
        return
      }
      const payload = Object.assign({}, importPayload, { app: app })
      this.importDialog.loading = true
      importAppInfo(payload, { dmDeploySet: this.importDialog.dmDeploySet })
        .then(res => {
          const data = this.extractImportResponse(res)
          this.importDialog.resultData = data
          this.importDialog.result = JSON.stringify(data || {}, null, 2)
          if (!this.isImportSuccess(data)) {
            this.importDialog.resultSuccess = false
            this.importDialog.summary = (data && (data.message || data.stackTrace)) || '导入失败'
            this.$message.error(this.importDialog.summary)
            return
          }
          this.importDialog.resultSuccess = true
          this.importDialog.summary = '导入完成: ' + this.summarizeImport(data)
          this.$message.success(this.importDialog.summary)
          this.fetchList()
        })
        .catch(err => {
          this.importDialog.resultSuccess = false
          this.importDialog.summary = '导入失败: ' + (err.message || String(err))
          this.importDialog.resultData = null
          this.importDialog.result = JSON.stringify({ status: 'error', message: this.importDialog.summary }, null, 2)
          this.$message.error(this.importDialog.summary)
        })
        .finally(() => {
          this.importDialog.loading = false
        })
    },
    summarizeImport(data) {
      if (!data) return ''
      // 优先使用接口返回的递归汇总总数(sks:successSumTotal / sks:failSumTotal)
      if (data['sks:successSumTotal'] !== undefined || data['sks:failSumTotal'] !== undefined) {
        return '成功 ' + (data['sks:successSumTotal'] || 0) + ' 条，失败 ' + (data['sks:failSumTotal'] || 0) + ' 条'
      }
      const rowsData = data.data || data
      return Object.keys(rowsData).map(key => {
        const rows = Array.isArray(rowsData[key]) ? rowsData[key] : [rowsData[key]]
        const ok = rows.filter(r => r && r._status === 'success').length
        return key + ' ' + ok + '/' + rows.length
      }).join('，')
    },
    // 兼容 axios 原始响应 / 字符串 / {code,data} 包装
    extractImportResponse(res) {
      let data = (res && res.data !== undefined) ? res.data : res
      if (typeof data === 'string') {
        // 去掉可能存在的 BOM/首尾空白, 避免 JSON.parse 失败被误判为导入失败
        const text = data.replace(/^\uFEFF/, '').trim()
        try { data = JSON.parse(text) } catch (e) { /* 保持原值 */ }
      }
      if (data && typeof data === 'object' && data.status === undefined && data.data && typeof data.data === 'object') {
        data = data.data
      }
      return data
    },
    // 成功判定: status=success; 无 status 时按导入记录是否全部成功判定
    isImportSuccess(data) {
      if (typeof data === 'string') {
        return data.indexOf('"status":"success"') >= 0 || data.indexOf('"status": "success"') >= 0
      }
      if (!data || typeof data !== 'object') return false
      if (data.status !== undefined) {
        return data.status === 'success'
      }
      const rows = this.collectImportRows(data.data)
      return rows.length > 0 && rows.every(row => row._status !== 'failed')
    },
    collectImportRows(group) {
      const rows = []
      if (!group || typeof group !== 'object') return rows
      Object.keys(group).forEach(key => {
        const list = Array.isArray(group[key]) ? group[key] : [group[key]]
        list.forEach(row => {
          if (row && typeof row === 'object') rows.push(row)
        })
      })
      return rows
    },
    openImportResult() {
      this.importResultDialog.activeTab = 'list'
      this.importResultDialog.expandAll = true
      this.importResultDialog.treeKey += 1
      this.importResultDialog.visible = true
    },
    onImportResultOpened() {
      this.applyImportExpand(this.importResultDialog.expandAll)
    },
    // 应用展开/收缩状态(列表树形表格); 树结构通过 treeKey 重建并取 default-expand-all
    applyImportExpand(expand) {
      this.$nextTick(() => {
        const table = this.$refs.importListTable
        if (!table) return
        this.importGroups.forEach(row => table.toggleRowExpansion(row, !!expand))
      })
    },
    // 展开所有(列表树形表格 + 树结构标签页)
    expandAllImport() {
      this.importResultDialog.expandAll = true
      this.importResultDialog.treeKey += 1
      this.applyImportExpand(true)
    },
    // 收缩所有(列表树形表格 + 树结构标签页)
    collapseAllImport() {
      this.importResultDialog.expandAll = false
      this.importResultDialog.treeKey += 1
      this.applyImportExpand(false)
    },
    statusTagType(status) {
      if (status === 'success') return 'success'
      if (status === 'skipped') return 'warning'
      return 'danger'
    },
    // 树结构: 对象 -> 导入记录 -> 行内嵌套数组(如 sigoption flags)
    buildImportTreeRows(prefix, rows) {
      const nodes = []
      for (let i = 0; i < rows.length; i++) {
        const item = rows[i]
        const raw = item.raw || {}
        const node = {
          id: prefix + '/' + i,
          label: this.formatImportRowLabel(raw, i),
          statusLabel: item.status || '',
          children: []
        }
        Object.keys(raw).forEach(k => {
          if (!Array.isArray(raw[k])) return
          raw[k].forEach((child, ci) => {
            if (!child || typeof child !== 'object') return
            node.children.push({
              id: node.id + '/' + k + '#' + ci,
              label: k + '[' + ci + '] ' + (child['sks:ref'] || '') + ' ' + (child._operation || '') + ' ' + (child._message || ''),
              statusLabel: child._status || ''
            })
          })
        })
        nodes.push(node)
      }
      return nodes
    },
    formatImportRowLabel(row, idx) {
      const parts = ['#' + (idx + 1)]
      if (!row) return parts.join(' ')
      if (row['sks:ref'] !== undefined && row['sks:ref'] !== null) parts.push('ref=' + row['sks:ref'])
      if (row['sks:uniqueID'] !== undefined && row['sks:uniqueID'] !== null && row['sks:uniqueID'] !== '') {
        parts.push('uniqueID=' + row['sks:uniqueID'] + '(' + (row['sks:uniqueIDName'] || '') + ')')
      }
      if (row['sks:uuid']) parts.push('uuid=' + row['sks:uuid'])
      if (row._operation) parts.push(row._operation)
      if (row._message) parts.push(row._message)
      return parts.join('  ')
    },
    // === 查看详情 ===
    showDetail(row) {
      this.detailLoading = true
      this.detailRow = null
      this.detailDialog.visible = true
      getMaxAppXmlDetail({ maxPresentationId: row.MAXPRESENTATIONID, app: row.APP })
        .then(res => {
          if (res.code === 200 && res.data) {
            this.detailRow = res.data
          } else {
            this.$message.error('获取详情失败: ' + (res.message || '未知错误'))
          }
        })
        .catch(err => {
          this.$message.error('获取详情失败: ' + (err.message || String(err)))
        })
        .finally(() => {
          this.detailLoading = false
        })
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
.page-actions {
  flex-shrink: 0;
}
.info-toolbar {
  margin-bottom: 8px;
  text-align: right;
}
.monaco-pane {
  height: 60vh;
  border: 1px solid #dcdfe6;
}
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
.import-result-toolbar {
  margin-bottom: 4px;
  text-align: right;
}
.import-tree-node {
  display: flex;
  align-items: center;
  font-size: 13px;
}
.import-tree-label {
  color: #303133;
}
.result-panel {
  margin-top: 20px;
}
.json-tabs {
  margin-top: 16px;
}
.json-toolbar {
  margin-bottom: 8px;
  text-align: right;
}
.monaco-container {
  height: 500px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}
</style>
