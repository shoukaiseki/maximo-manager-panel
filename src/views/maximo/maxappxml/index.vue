<template>
  <section class="query-page">
    <el-card>
      <div class="page-header-row">
        <div>
          <h2>应用XML查询</h2>
          <p class="page-summary">查询 MAXPRESENTATION 数据，支持 APP、描述、XML内容搜索。</p>
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
            <el-table-column label="操作" width="180" fixed="right">
              <template slot-scope="scope">
                <el-button type="text" size="small" @click.stop="showSource(scope.row)">查看源码</el-button>
                <el-button type="text" size="small" @click.stop="showDetail(scope.row)">查看详情</el-button>
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
import { getMaxAppXmlList, getMaxAppXmlSource, getMaxAppXmlDetail } from '@/api/maxappxml'
import { sksPageMixin } from "sks-plugin-el-erp/lib/sks-page";

export default {
  name: 'MaxAppXmlList',
  mixins: [sksPageMixin],
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
      // 详情弹窗
      detailDialog: {
        visible: false
      },
      detailLoading: false,
      detailRow: null,
      monacoLoaded: false
    }
  },
  watch: {
    'sourceDialog.visible'(val) {
      if (!val) this.disposeSourceEditor()
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
