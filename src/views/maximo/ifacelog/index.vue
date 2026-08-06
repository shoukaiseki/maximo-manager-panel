<template>
  <section class="query-page">
    <el-card>
      <div class="page-header-row">
        <div>
          <h2>IFACE 日志查询</h2>
          <p class="page-summary">查询 IBM_IFACELOG 接口日志。支持 APP、所属表、状态等搜索。</p>
        </div>
      </div>

      <el-form :model="formData" ref="queryForm" :inline="true" label-width="90px" @submit.native.prevent>
        <el-form-item label="APP">
          <el-input v-model="formData.app" placeholder="=精确/%模糊" clearable style="width: 170px;" @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item label="所属表">
          <el-input v-model="formData.ownerTable" placeholder="=精确/%模糊" clearable style="width: 170px;" @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item label="状态">
          <el-input v-model="formData.status" placeholder="=精确/%模糊" clearable style="width: 150px;" @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="formData.description" placeholder="模糊搜索" clearable style="width: 150px;" @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item label="外部系统">
          <el-input v-model="formData.extSystem" placeholder="模糊搜索" clearable style="width: 150px;" @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item label="接口状态">
          <el-input v-model="formData.ifaceStatus" placeholder="模糊搜索" clearable style="width: 130px;" @keyup.enter.native="handleQuery" />
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

    <!-- 详情弹窗 -->
    <el-dialog :title="'日志详情 - IBM_IFACELOGID: ' + (detailRow ? detailRow.IBM_IFACELOGID : '')" :visible.sync="detailDialog.visible" width="90%" top="3vh" :close-on-click-modal="true" @opened="onDetailDialogOpened">
      <div v-loading="detailLoading">
        <template v-if="detailRow">
          <el-descriptions :column="4" border>
            <el-descriptions-item label="ID">
              <el-input :value="detailRow.IBM_IFACELOGID" readonly size="small" />
            </el-descriptions-item>
            <el-descriptions-item label="APP">
              <el-input :value="detailRow.APP || '-'" readonly size="small" />
            </el-descriptions-item>
            <el-descriptions-item label="描述">
              <el-input :value="detailRow.DESCRIPTION || '-'" readonly size="small" />
            </el-descriptions-item>
            <el-descriptions-item label="接口状态">
              <el-input :value="detailRow.IFACESTATUS || '-'" readonly size="small" />
            </el-descriptions-item>
            <el-descriptions-item label="所属表">
              <el-input :value="detailRow.OWNERTABLE || '-'" readonly size="small" />
            </el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-input :value="detailRow.STATUS || '-'" readonly size="small" />
            </el-descriptions-item>
            <el-descriptions-item label="修改人">
              <el-input :value="detailRow.CHANGEBY || '-'" readonly size="small" />
            </el-descriptions-item>
            <el-descriptions-item label="修改日期">
              <el-input :value="detailRow.CHANGEDATE ? formatDate(detailRow.CHANGEDATE) : '-'" readonly size="small" />
            </el-descriptions-item>
            <el-descriptions-item label="地点">
              <el-input :value="detailRow.SITEID || '-'" readonly size="small" />
            </el-descriptions-item>
            <el-descriptions-item label="组织">
              <el-input :value="detailRow.ORGID || '-'" readonly size="small" />
            </el-descriptions-item>
            <el-descriptions-item label="外部系统">
              <el-input :value="detailRow.EXTSYSTEM || '-'" readonly size="small" />
            </el-descriptions-item>
            <el-descriptions-item label="OWNERID">
              <el-input :value="detailRow.OWNERID || '-'" readonly size="small" />
            </el-descriptions-item>
          </el-descriptions>
          <el-descriptions :column="1" border style="margin-top:8px;">
            <el-descriptions-item label="意见">
              <el-input :value="detailRow.MEMO || '-'" readonly type="textarea" :rows="2" />
            </el-descriptions-item>
          </el-descriptions>

          <el-tabs v-model="detailActiveTab" type="border-card" class="json-tabs" style="margin-top:16px;">
            <el-tab-pane label="请求内容 (REQBODY)" name="reqBody">
              <div ref="reqBodyMonacoRef" class="monaco-container"></div>
            </el-tab-pane>
            <el-tab-pane label="响应内容 (RESPBODY)" name="respBody">
              <div ref="respBodyMonacoRef" class="monaco-container"></div>
            </el-tab-pane>
          </el-tabs>
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
import { getIfaceLogList, getIfaceLogDetail } from '@/api/ifacelog'
import { sksPageMixin } from "sks-plugin-el-erp/lib/sks-page";

export default {
  name: 'IfaceLogList',
  mixins: [sksPageMixin],
  data() {
    return {
      loading: false,
      hasSearched: false,
      total: 0,
      formData: {
        app: '',
        ownerTable: '',
        status: '',
        description: '',
        extSystem: '',
        ifaceStatus: ''
      },
      detailDialog: {
        visible: false
      },
      detailLoading: false,
      detailRow: null,
      detailActiveTab: 'reqBody',
      monacoLoaded: false,
      reqBodyEditor: null,
      respBodyEditor: null
    }
  },
  watch: {
    'detailDialog.visible'(val) {
      if (!val) this.disposeEditors()
    },
    detailActiveTab() {
      this.$nextTick(() => {
        setTimeout(() => {
          this.layoutActiveEditor()
        }, 200)
      })
    }
  },
  methods: {
    initMainTableParam() {
      return {
        ownerName: 'ifacelog',
        uniqueId: 'ifacelog-list',
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
            { prop: 'IBM_IFACELOGID', label: 'ID', width: 120 },
            { prop: 'APP', label: 'APP', minWidth: 120 },
            { prop: 'DESCRIPTION', label: '描述', minWidth: 180 },
            { prop: 'OWNERTABLE', label: '所属表', minWidth: 120 },
            { prop: 'STATUS', label: '状态', minWidth: 100 },
            { prop: 'IFACESTATUS', label: '接口状态', minWidth: 100 },
            { prop: 'EXTSYSTEM', label: '外部系统', minWidth: 120 },
            { prop: 'SITEID', label: '地点', width: 100 },
            { prop: 'CHANGEBY', label: '修改人', width: 100 },
            { prop: 'CHANGEDATE', label: '修改日期', minWidth: 160 }
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
        ownerTable: this.formData.ownerTable,
        status: this.formData.status,
        description: this.formData.description,
        extSystem: this.formData.extSystem,
        ifaceStatus: this.formData.ifaceStatus,
        pageNum: this.mainTable.queryParams.pageNum,
        pageSize: this.mainTable.queryParams.pageSize
      }
      getIfaceLogList(params)
        .then(res => {
          if (res.code === 200 && res.data) {
            this.mainTable.list = (res.data.rows || []).map(row => {
              return Object.assign({}, row, {
                CHANGEDATE: this.formatDate(row.CHANGEDATE)
              })
            })
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
        ownerTable: '',
        status: '',
        description: '',
        extSystem: '',
        ifaceStatus: ''
      }
      this.hasSearched = false
      this.mainTable.list = []
      this.mainTable.total = 0
      this.total = 0
      this.mainTable.currentPage = 1
    },
    handleRowClick(row) {
      this.detailLoading = true
      this.detailRow = null
      this.detailActiveTab = 'reqBody'
      this.detailDialog.visible = true
      getIfaceLogDetail(row.IBM_IFACELOGID)
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
          this.$nextTick(() => {
            setTimeout(() => {
              this.initEditors()
            }, 300)
          })
        })
    },
    onDetailDialogOpened() {
      this.$nextTick(() => {
        setTimeout(() => {
          this.initEditors()
        }, 300)
      })
    },
    formatDate(date) {
      if (!date) return '-'
      const d = new Date(date)
      if (isNaN(d.getTime())) return date
      const pad = n => String(n).padStart(2, '0')
      return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()) +
        ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes()) + ':' + pad(d.getSeconds())
    },
    // === Monaco Editor ===
    initEditors() {
      if (!this.detailRow) return
      if (!this.monacoLoaded) {
        import(/* webpackChunkName: "monaco" */ 'monaco-editor').then(monaco => {
          this.monacoLoaded = true
          this._monaco = monaco
          this.createEditors(monaco)
        }).catch(err => {
          console.error('Monaco Editor 加载失败:', err)
        })
      } else {
        this.createEditors(this._monaco)
      }
    },
    createEditors(monaco) {
      const lang = this.detectLanguage()
      this.createReqBodyEditor(monaco, lang)
      this.createRespBodyEditor(monaco, lang)
    },
    detectLanguage() {
      const req = this.detailRow.REQBODY || ''
      const resp = this.detailRow.RESPBODY || ''
      const combined = req + resp
      if (combined.startsWith('<') || combined.startsWith('<?xml')) return 'xml'
      if (combined.startsWith('{') || combined.startsWith('[')) return 'json'
      return 'plaintext'
    },
    createReqBodyEditor(monaco, lang) {
      const el = this.$refs.reqBodyMonacoRef
      if (!el) return
      if (this.reqBodyEditor) {
        this.reqBodyEditor.setValue(this.detailRow.REQBODY || '')
        return
      }
      this.reqBodyEditor = monaco.editor.create(el, {
        value: this.detailRow.REQBODY || '',
        language: lang,
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
    createRespBodyEditor(monaco, lang) {
      const el = this.$refs.respBodyMonacoRef
      if (!el) return
      if (this.respBodyEditor) {
        this.respBodyEditor.setValue(this.detailRow.RESPBODY || '')
        return
      }
      this.respBodyEditor = monaco.editor.create(el, {
        value: this.detailRow.RESPBODY || '',
        language: lang,
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
    layoutActiveEditor() {
      if (this.detailActiveTab === 'reqBody' && this.reqBodyEditor) {
        this.reqBodyEditor.layout()
      } else if (this.detailActiveTab === 'respBody' && this.respBodyEditor) {
        this.respBodyEditor.layout()
      }
    },
    disposeEditors() {
      if (this.reqBodyEditor) {
        this.reqBodyEditor.dispose()
        this.reqBodyEditor = null
      }
      if (this.respBodyEditor) {
        this.respBodyEditor.dispose()
        this.respBodyEditor = null
      }
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
.monaco-container {
  height: 500px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}
</style>
