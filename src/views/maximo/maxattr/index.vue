<template>
  <section class="query-page">
    <el-card>
      <div class="page-header-row">
        <div>
          <h2>MaxAttribute 字段查询</h2>
          <p class="page-summary">支持对象名、字段名、字段描述搜索。以 "=" 开头精确匹配，支持 % 通配符。</p>
        </div>
      </div>

      <el-form :model="formData" ref="queryForm" :inline="true" label-width="90px" @submit.native.prevent>
        <el-form-item label="对象名">
          <el-input v-model="formData.objectname" placeholder="如 =ASSET 或 %ITEM%" clearable style="width: 220px;" @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item label="字段名">
          <el-input v-model="formData.attributename" placeholder="如 =ASSETNUM 或 %NUM%" clearable style="width: 220px;" @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item label="字段描述">
          <el-input v-model="formData.description" placeholder="输入描述关键词..." clearable style="width: 220px;" @keyup.enter.native="handleQuery" />
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
          @rowClickAfter="handleRowClick">
          <template slot="tableColumnBefore">
            <el-table-column label="操作" width="160" fixed="left" align="center">
              <template slot-scope="{row}">
                <el-tooltip content="复制精简JSON" placement="top">
                  <el-button type="text" size="small" icon="el-icon-document-copy" @click.stop="copySimpleJson(row)">精简</el-button>
                </el-tooltip>
                <el-tooltip content="复制完整JSON" placement="top">
                  <el-button type="text" size="small" icon="el-icon-document" @click.stop="copyFullJson(row)">完整</el-button>
                </el-tooltip>
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

    <!-- 字段详情弹窗 -->
    <el-dialog :title="'字段详情 - ' + (currentRow ? currentRow.ATTRIBUTENAME : '')" :visible.sync="dialogVisible" width="1500px" :close-on-click-modal="true" @opened="onDialogOpened">
      <el-descriptions :column="3" border v-if="currentRow">
        <el-descriptions-item label="对象名">{{ currentRow.OBJECTNAME }}</el-descriptions-item>
        <el-descriptions-item label="属性名">{{ currentRow.ATTRIBUTENAME }}</el-descriptions-item>
        <el-descriptions-item label="标题">{{ currentRow.L_TITLE }}</el-descriptions-item>
        <el-descriptions-item label="英文标题">{{ currentRow.TITLE }}</el-descriptions-item>
        <el-descriptions-item label="别名">{{ currentRow.ALIAS }}</el-descriptions-item>
        <el-descriptions-item label="数据类型">{{ currentRow.MAXTYPE }}</el-descriptions-item>
        <el-descriptions-item label="长度">{{ currentRow.LENGTH }}</el-descriptions-item>
        <el-descriptions-item label="小数位数">{{ currentRow.SCALE }}</el-descriptions-item>
        <el-descriptions-item label="持久性">{{ currentRow.PERSISTENT }}</el-descriptions-item>
        <el-descriptions-item label="自动编号">{{ currentRow.CANAUTONUM }}</el-descriptions-item>
        <el-descriptions-item label="Java 类" :span="2">{{ currentRow.CLASSNAME }}</el-descriptions-item>
        <el-descriptions-item label="列名">{{ currentRow.COLUMNNAME }}</el-descriptions-item>
        <el-descriptions-item label="属性号">{{ currentRow.ATTRIBUTENO }}</el-descriptions-item>
        <el-descriptions-item label="域">{{ currentRow.DOMAINID }}</el-descriptions-item>
        <el-descriptions-item label="默认值">{{ currentRow.DEFAULTVALUE }}</el-descriptions-item>
        <el-descriptions-item label="必需">{{ currentRow.REQUIRED }}</el-descriptions-item>
        <el-descriptions-item label="主键列序列">{{ currentRow.PRIMARYKEYCOLSEQ }}</el-descriptions-item>
        <el-descriptions-item label="搜索类型">{{ currentRow.SEARCHTYPE }}</el-descriptions-item>
        <el-descriptions-item label="长文本所有者">{{ currentRow.ISLDOWNER }}</el-descriptions-item>
        <el-descriptions-item label="必须">{{ currentRow.MUSTBE }}</el-descriptions-item>
        <el-descriptions-item label="正向">{{ currentRow.ISPOSITIVE }}</el-descriptions-item>
        <el-descriptions-item label="限制">{{ currentRow.RESTRICTED }}</el-descriptions-item>
        <el-descriptions-item label="可本地化">{{ currentRow.LOCALIZABLE }}</el-descriptions-item>
        <el-descriptions-item label="用户定义">{{ currentRow.USERDEFINED }}</el-descriptions-item>
        <el-descriptions-item label="多语言支持">{{ currentRow.MLSUPPORTED }}</el-descriptions-item>
        <el-descriptions-item label="多语言使用中">{{ currentRow.MLINUSE }}</el-descriptions-item>
        <el-descriptions-item label="审计启用">{{ currentRow.EAUDITENABLED }}</el-descriptions-item>
        <el-descriptions-item label="电子签名启用">{{ currentRow.ESIGENABLED }}</el-descriptions-item>
        <el-descriptions-item label="实体">{{ currentRow.ENTITYNAME }}</el-descriptions-item>
        <el-descriptions-item label="等同属性">{{ currentRow.SAMEASATTRIBUTE }}</el-descriptions-item>
        <el-descriptions-item label="等同对象">{{ currentRow.SAMEASOBJECT }}</el-descriptions-item>
        <el-descriptions-item label="自动键名">{{ currentRow.AUTOKEYNAME }}</el-descriptions-item>
        <el-descriptions-item label="属性 ID">{{ currentRow.MAXATTRIBUTEID }}</el-descriptions-item>
        <el-descriptions-item label="行戳">{{ currentRow.ROWSTAMP }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">
          <div style="white-space: pre-wrap; word-break: break-word;">{{ currentRow.L_REMARKS }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="英文备注" :span="2">
          <div style="white-space: pre-wrap; word-break: break-word;">{{ currentRow.REMARKS }}</div>
        </el-descriptions-item>
      </el-descriptions>

      <el-tabs v-model="activeTab" type="border-card" class="json-tabs">
        <el-tab-pane label="字段精简" name="fieldSimple">
          <div class="json-toolbar">
            <el-button type="primary" size="mini" icon="el-icon-document-copy" @click="copyFieldSimpleJson">复制字段精简JSON</el-button>
          </div>
          <div v-loading="detailLoading" element-loading-text="加载中..." class="monaco-wrapper">
            <div ref="fieldSimpleMonacoRef" class="monaco-container"></div>
          </div>
        </el-tab-pane>
        <el-tab-pane label="字段完整" name="fieldFull">
          <div class="json-toolbar">
            <el-button type="primary" size="mini" icon="el-icon-document" @click="copyFieldFullJson">复制字段完整JSON</el-button>
          </div>
          <div v-loading="detailLoading" element-loading-text="加载中..." class="monaco-wrapper">
            <div ref="fieldFullMonacoRef" class="monaco-container"></div>
          </div>
        </el-tab-pane>
        <el-tab-pane label="对象精简" name="objSimple">
          <div class="json-toolbar">
            <el-button type="primary" size="mini" icon="el-icon-document-copy" @click="copyObjSimpleJson">复制对象精简JSON</el-button>
          </div>
          <div v-loading="detailLoading" element-loading-text="加载中..." class="monaco-wrapper">
            <div ref="objSimpleMonacoRef" class="monaco-container"></div>
          </div>
        </el-tab-pane>
        <el-tab-pane label="对象完整" name="objFull">
          <div class="json-toolbar">
            <el-button type="primary" size="mini" icon="el-icon-document" @click="copyObjFullJson">复制对象完整JSON</el-button>
          </div>
          <div v-loading="detailLoading" element-loading-text="加载中..." class="monaco-wrapper">
            <div ref="objFullMonacoRef" class="monaco-container"></div>
          </div>
        </el-tab-pane>
      </el-tabs>

      <span slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">关 闭</el-button>
      </span>
    </el-dialog>
  </section>
</template>

<script>
import {sksPageMixin} from "sks-plugin-el-erp/lib/sks-page";
import { getMaxAttributeList, exportDbConfig } from '@/api/maxobject'

export default {
  name: 'MaxAttrList',
  mixins: [
    sksPageMixin,
  ],
  data() {
    return {
      loading: false,
      hasSearched: false,
      total: 0,
      dialogVisible: false,
      currentRow: null,
      simpleAttrData: null,     // 精简模式的字段数据
      simpleObjectData: null,   // 精简模式的对象数据
      fullAttrData: null,       // 完整模式的字段数据
      fullObjectData: null,     // 完整模式的对象数据
      detailLoading: false,     // 详情加载状态
      activeTab: 'fieldSimple',
      fieldSimpleEditor: null,
      fieldFullEditor: null,
      objSimpleEditor: null,
      objFullEditor: null,
      monacoLoaded: false,
      formData: {
        objectname: '',
        attributename: '',
        description: ''
      }
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
    }
  },
  methods: {
    initMainTableParam() {
      return {
        ownerName: `maxattribute`,
        uniqueId: 'maxattr-list',
        sksAppName: 'page51',
        tableColumnListEnable: true,
        showPagination: true,
        showTable: true,
        showAllColumnButton: true,
        showTablePropName: false,
        serverPagination: true,
        total: 0,
        pageSize: 20,
        currentPage: 1,
        tableColumnList:
          this.sksUtils.newTableColumnList([
            { prop: 'OBJECTNAME', label: '对象名', minWidth: 150 },
            { prop: 'ATTRIBUTENAME', label: '属性名', minWidth: 200 },
            { prop: 'L_TITLE', label: '标题', minWidth: 150 },
            { prop: 'TITLE', label: '英文标题', minWidth: 150 },
            { prop: 'MAXTYPE', label: '类型', width: 90 },
            { prop: 'LENGTH', label: '长度', width: 70 },
            { prop: 'SCALE', label: '小数', width: 70 },
            { prop: 'DOMAINID', label: '域', minWidth: 120 },
            { prop: 'REQUIRED', label: '必需', width: 60 },
            { prop: 'PERSISTENT', label: '持久', width: 60 },
            { prop: 'DEFAULTVALUE', label: '默认值', minWidth: 120 },
            { prop: 'ALIAS', label: '别名', minWidth: 100, visible: false },
            { prop: 'ATTRIBUTENO', label: '属性号', width: 80, visible: false },
            { prop: 'CLASSNAME', label: 'Java 类', minWidth: 200, visible: false },
            { prop: 'COLUMNNAME', label: '列名', minWidth: 130, visible: false },
            { prop: 'EAUDITENABLED', label: '审计启用', width: 80, visible: false },
            { prop: 'ENTITYNAME', label: '实体', minWidth: 130, visible: false },
            { prop: 'ESIGENABLED', label: '电子签名启用', width: 100, visible: false },
            { prop: 'ISLDOWNER', label: '长文本所有者', width: 100, visible: false },
            { prop: 'ISPOSITIVE', label: '正向', width: 60, visible: false },
            { prop: 'MUSTBE', label: '必须', width: 60, visible: false },
            { prop: 'PRIMARYKEYCOLSEQ', label: '主键列序列', width: 100, visible: false },
            { prop: 'AUTOKEYNAME', label: '自动键名', minWidth: 120, visible: false },
            { prop: 'SAMEASATTRIBUTE', label: '等同属性', minWidth: 120, visible: false },
            { prop: 'SAMEASOBJECT', label: '等同对象', minWidth: 120, visible: false },
            { prop: 'USERDEFINED', label: '用户定义', width: 80, visible: false },
            { prop: 'SEARCHTYPE', label: '搜索类型', width: 80, visible: false },
            { prop: 'MLSUPPORTED', label: '多语言支持', width: 90, visible: false },
            { prop: 'MLINUSE', label: '多语言使用中', width: 100, visible: false },
            { prop: 'MAXATTRIBUTEID', label: '属性 ID', width: 100, visible: false },
            { prop: 'RESTRICTED', label: '限制', width: 60, visible: false },
            { prop: 'LOCALIZABLE', label: '可本地化', width: 80, visible: false },
            { prop: 'ROWSTAMP', label: '行戳', width: 100, visible: false },
            { prop: 'L_REMARKS', label: '备注', minWidth: 250, className: 'wrap-cell' },
            { prop: 'REMARKS', label: '英文备注', minWidth: 250, className: 'wrap-cell' },
          ]),
        queryParamsColumnListEnable: false,
        queryParamsColumnList: [],
      }
    },
    handleQuery() {
      this.hasSearched = true
      this.mainTable.currentPage = 1
      this.fetchList()
    },
    fetchList() {
      this.loading = true
      const params = {
        objectname: this.formData.objectname,
        attributename: this.formData.attributename,
        description: this.formData.description,
        pageNum: this.mainTable.currentPage,
        pageSize: this.mainTable.pageSize
      }
      getMaxAttributeList(params)
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
      this.mainTable.currentPage = page
      this.mainTable.pageSize = limit
      this.fetchList()
    },
    resetForm() {
      this.formData = {
        objectname: '',
        attributename: '',
        description: ''
      }
      this.hasSearched = false
      this.mainTable.list = []
      this.mainTable.total = 0
      this.total = 0
      this.mainTable.currentPage = 1
    },
    handleRowClick(row) {
      this.currentRow = row
      this.simpleAttrData = null
      this.simpleObjectData = null
      this.fullAttrData = null
      this.fullObjectData = null
      this.activeTab = 'fieldSimple'
      this.dialogVisible = true
    },
    onDialogOpened() {
      if (this.currentRow && this.currentRow.OBJECTNAME) {
        const objectName = this.currentRow.OBJECTNAME
        const attrName = this.currentRow.ATTRIBUTENAME ? this.currentRow.ATTRIBUTENAME.toUpperCase() : ''
        this.detailLoading = true
        // 并行调用两个接口
        Promise.all([
          exportDbConfig(objectName, true),   // 精简模式（ignoreDefVal=true）
          exportDbConfig(objectName, false)   // 完整模式（ignoreDefVal=false）
        ]).then(([simpleRes, fullRes]) => {
          // 处理精简数据
          if (simpleRes.status === 200 && simpleRes.data) {
            this.simpleObjectData = simpleRes.data
            if (simpleRes.data.maxObjects) {
              const maxObject = simpleRes.data.maxObjects.find(obj => obj.object === objectName)
              if (maxObject) {
                const attrData = (maxObject.attributes || []).find(
                  attr => (attr.attribute || '').toUpperCase() === attrName
                )
                this.simpleAttrData = attrData || null
              }
            }
          }
          // 处理完整数据
          if (fullRes.status === 200 && fullRes.data) {
            this.fullObjectData = fullRes.data
            if (fullRes.data.maxObjects) {
              const maxObject = fullRes.data.maxObjects.find(obj => obj.object === objectName)
              if (maxObject) {
                const attrData = (maxObject.attributes || []).find(
                  attr => (attr.attribute || '').toUpperCase() === attrName
                )
                this.fullAttrData = attrData || null
              }
            }
          }
          // 数据返回后初始化编辑器
          this.$nextTick(() => {
            setTimeout(() => {
              this.initMonacoEditors()
              this.detailLoading = false
            }, 100)
          })
        }).catch(err => {
          console.error('获取字段详情失败:', err)
          this.detailLoading = false
        })
      }
    },
    initMonacoEditors() {
      if (!this.currentRow) return
      const fieldSimpleJson = this.simpleAttrData
        ? JSON.stringify(this.simpleAttrData, null, 2)
        : '{}'
      const fieldFullJson = this.fullAttrData
        ? JSON.stringify(this.fullAttrData, null, 2)
        : '{}'
      const objSimpleJson = this.simpleObjectData
        ? JSON.stringify(this.simpleObjectData, null, 2)
        : '{}'
      const objFullJson = this.fullObjectData
        ? JSON.stringify(this.fullObjectData, null, 2)
        : '{}'
      if (!this.monacoLoaded) {
        import(/* webpackChunkName: "monaco" */ 'monaco-editor').then(monaco => {
          this.monacoLoaded = true
          this._monaco = monaco
          this.createEditors(fieldSimpleJson, fieldFullJson, objSimpleJson, objFullJson)
        }).catch(err => {
          console.error('Monaco Editor 加载失败:', err)
        })
      } else {
        this.createEditors(fieldSimpleJson, fieldFullJson, objSimpleJson, objFullJson)
      }
    },
    createEditors(fieldSimpleJson, fieldFullJson, objSimpleJson, objFullJson) {
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
      if (this.$refs.fieldSimpleMonacoRef && !this.fieldSimpleEditor) {
        this.fieldSimpleEditor = monaco.editor.create(this.$refs.fieldSimpleMonacoRef, {
          value: fieldSimpleJson,
          ...options
        })
      } else if (this.fieldSimpleEditor) {
        this.fieldSimpleEditor.setValue(fieldSimpleJson)
      }
      if (this.$refs.fieldFullMonacoRef && !this.fieldFullEditor) {
        this.fieldFullEditor = monaco.editor.create(this.$refs.fieldFullMonacoRef, {
          value: fieldFullJson,
          ...options
        })
      } else if (this.fieldFullEditor) {
        this.fieldFullEditor.setValue(fieldFullJson)
      }
      if (this.$refs.objSimpleMonacoRef && !this.objSimpleEditor) {
        this.objSimpleEditor = monaco.editor.create(this.$refs.objSimpleMonacoRef, {
          value: objSimpleJson,
          ...options
        })
      } else if (this.objSimpleEditor) {
        this.objSimpleEditor.setValue(objSimpleJson)
      }
      if (this.$refs.objFullMonacoRef && !this.objFullEditor) {
        this.objFullEditor = monaco.editor.create(this.$refs.objFullMonacoRef, {
          value: objFullJson,
          ...options
        })
      } else if (this.objFullEditor) {
        this.objFullEditor.setValue(objFullJson)
      }
      setTimeout(() => {
        this.layoutEditors()
      }, 100)
    },
    layoutEditors() {
      if (this.fieldSimpleEditor) this.fieldSimpleEditor.layout()
      if (this.fieldFullEditor) this.fieldFullEditor.layout()
      if (this.objSimpleEditor) this.objSimpleEditor.layout()
      if (this.objFullEditor) this.objFullEditor.layout()
    },
    disposeEditors() {
      if (this.fieldSimpleEditor) {
        this.fieldSimpleEditor.dispose()
        this.fieldSimpleEditor = null
      }
      if (this.fieldFullEditor) {
        this.fieldFullEditor.dispose()
        this.fieldFullEditor = null
      }
      if (this.objSimpleEditor) {
        this.objSimpleEditor.dispose()
        this.objSimpleEditor = null
      }
      if (this.objFullEditor) {
        this.objFullEditor.dispose()
        this.objFullEditor = null
      }
      this.monacoLoaded = false
      this._monaco = null
    },
    copyFieldSimpleJson() {
      const json = this.simpleAttrData
        ? JSON.stringify(this.simpleAttrData, null, 2)
        : '{}'
      this.copyToClipboard(json, '字段精简JSON')
    },
    copyFieldFullJson() {
      const json = this.fullAttrData
        ? JSON.stringify(this.fullAttrData, null, 2)
        : '{}'
      this.copyToClipboard(json, '字段完整JSON')
    },
    copyObjSimpleJson() {
      const json = this.simpleObjectData
        ? JSON.stringify(this.simpleObjectData, null, 2)
        : '{}'
      this.copyToClipboard(json, '对象精简JSON')
    },
    copyObjFullJson() {
      const json = this.fullObjectData
        ? JSON.stringify(this.fullObjectData, null, 2)
        : '{}'
      this.copyToClipboard(json, '对象完整JSON')
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

.wrap-cell {
  white-space: normal;
  word-break: break-word;
}

.wrap-cell .cell {
  white-space: normal;
  word-break: break-word;
}

.el-table .cell.el-tooltip {
  white-space: normal !important;
  word-break: break-word !important;
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
