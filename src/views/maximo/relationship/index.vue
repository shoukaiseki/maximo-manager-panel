<template>
  <section class="query-page">
    <el-card>
      <div class="page-header-row">
        <div>
          <h2>关系查询</h2>
          <p class="page-summary">支持关系名、父对象、子对象搜索。以 "=" 开头精确匹配，支持 % 通配符。</p>
          <p class="page-tip"><i class="el-icon-info"></i> 在输入框中按 Alt+F1 可查看字段详细信息</p>
        </div>
        <el-button type="success" icon="el-icon-download" size="small" :loading="genFullLoading" @click="generateFullJson">生成完整关系JSON</el-button>
      </div>

      <el-form :model="formData" ref="queryForm" :inline="true" label-width="90px" @submit.native.prevent>
        <el-form-item label="关键字">
          <el-input ref="keywordInput" v-model="formData.keyword" placeholder="通用搜索" clearable style="width: 220px;" @keyup.enter.native="handleQuery" @keydown.native="handleKeyDown($event, {prop: 'KEYWORD', label: '关键字'})" />
        </el-form-item>
        <el-form-item label="关系名">
          <el-input ref="nameInput" v-model="formData.name" placeholder="=精确/%模糊" clearable style="width: 220px;" @keyup.enter.native="handleQuery" @keydown.native="handleKeyDown($event, {prop: 'NAME', label: '关系名'})" />
        </el-form-item>
        <el-form-item label="父对象">
          <el-input ref="parentInput" v-model="formData.parent" placeholder="=精确/%模糊" clearable style="width: 220px;" @keyup.enter.native="handleQuery" @keydown.native="handleKeyDown($event, {prop: 'PARENT', label: '父对象'})" />
        </el-form-item>
        <el-form-item label="子对象">
          <el-input ref="childInput" v-model="formData.child" placeholder="=精确/%模糊" clearable style="width: 220px;" @keyup.enter.native="handleQuery" @keydown.native="handleKeyDown($event, {prop: 'CHILD', label: '子对象'})" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" size="mini" :loading="loading" @click="handleQuery">搜索</el-button>
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
          <template slot="tableColumnList-before">
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

    <el-dialog :title="'关系详情 - ' + (currentRow ? currentRow.NAME : '')" :visible.sync="dialogVisible" width="1500px" :close-on-click-modal="true" @opened="onDialogOpened">
      <el-descriptions :column="3" border v-if="currentRow">
        <el-descriptions-item label="关系名">
          <el-input :value="currentRow.NAME" readonly size="small" @keydown.native="handleKeyDown($event, {prop: 'NAME', label: '关系名'})" />
        </el-descriptions-item>
        <el-descriptions-item label="父对象">
          <el-input :value="currentRow.PARENT" readonly size="small" @keydown.native="handleKeyDown($event, {prop: 'PARENT', label: '父对象'})" />
        </el-descriptions-item>
        <el-descriptions-item label="子对象">
          <el-input :value="currentRow.CHILD" readonly size="small" @keydown.native="handleKeyDown($event, {prop: 'CHILD', label: '子对象'})" />
        </el-descriptions-item>
        <el-descriptions-item label="基数">
          <el-input :value="currentRow.CARDINALITY || '-'" readonly size="small" @keydown.native="handleKeyDown($event, {prop: 'CARDINALITY', label: '基数'})" />
        </el-descriptions-item>
        <el-descriptions-item label="是否默认">
          <el-input :value="currentRow.ISDEFAULT === '1' ? '是' : '否'" readonly size="small" @keydown.native="handleKeyDown($event, {prop: 'ISDEFAULT', label: '是否默认'})" />
        </el-descriptions-item>
        <el-descriptions-item label="必需数据库连接">
          <el-input :value="currentRow.DBJOINREQUIRED === '1' ? '是' : '否'" readonly size="small" @keydown.native="handleKeyDown($event, {prop: 'DBJOINREQUIRED', label: '必需数据库连接'})" />
        </el-descriptions-item>
        <el-descriptions-item label="ID">
          <el-input :value="currentRow.MAXRELATIONSHIPID || '-'" readonly size="small" @keydown.native="handleKeyDown($event, {prop: 'MAXRELATIONSHIPID', label: 'ID'})" />
        </el-descriptions-item>
        <el-descriptions-item label="Where 子句" :span="2">
          <el-input :value="currentRow.WHERECLAUSE || '-'" readonly type="textarea" :rows="3" @keydown.native="handleKeyDown($event, {prop: 'WHERECLAUSE', label: 'Where 子句'})" />
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">
          <el-input :value="currentRow.REMARKS || '-'" readonly type="textarea" :rows="3" @keydown.native="handleKeyDown($event, {prop: 'REMARKS', label: '备注'})" />
        </el-descriptions-item>
      </el-descriptions>

      <el-tabs v-model="activeTab" type="border-card" class="json-tabs">
        <el-tab-pane label="关系精简" name="simple">
          <div class="json-toolbar">
            <el-button type="primary" size="mini" icon="el-icon-document-copy" @click="copySimpleJson">复制关系精简JSON</el-button>
          </div>
          <div v-loading="detailLoading" element-loading-text="加载中..." class="monaco-wrapper">
            <div ref="simpleMonacoRef" class="monaco-container"></div>
          </div>
        </el-tab-pane>
        <el-tab-pane label="关系完整" name="full">
          <div class="json-toolbar">
            <el-button type="primary" size="mini" icon="el-icon-document" @click="copyFullJson">复制关系完整JSON</el-button>
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

    <el-dialog title="完整关系JSON" :visible.sync="fullJsonDialogVisible" width="900px" :close-on-click-modal="false">
      <div class="json-toolbar">
        <el-button type="primary" size="mini" icon="el-icon-document-copy" @click="copyFullJsonData">复制完整关系JSON</el-button>
      </div>
      <div class="monaco-wrapper">
        <div ref="fullJsonMonacoRef" class="monaco-container"></div>
      </div>
    </el-dialog>
  </section>
</template>

<script>
import { sksPageMixin } from "sks-plugin-el-erp/lib/sks-page"
import { getMaxRelationshipList, getAllMaxRelationships } from '@/api/relationship'
import { getMaxObjectDescription, getMaxAttributeList } from '@/api/maxobject'

export default {
  name: 'MaxRelationshipQuery',
  mixins: [sksPageMixin],
  data() {
    return {
      loading: false,
      hasSearched: false,
      total: 0,
      dialogVisible: false,
      currentRow: null,
      detailLoading: false,
      activeTab: 'simple',
      simpleEditor: null,
      fullEditor: null,
      monacoLoaded: false,
      formData: {
        keyword: '',
        name: '',
        parent: '',
        child: ''
      },
      objectDescription: null,
      fieldDetailLoading: false,
      genFullLoading: false,
      fullJsonData: null,
      fullJsonDialogVisible: false,
      fullJsonMonacoRef: null
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
    fullJsonDialogVisible(val) {
      if (val) {
        this.$nextTick(() => {
          this.initFullJsonEditor()
        })
      } else {
        this.disposeFullJsonEditor()
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
        ownerName: `maxrelationship`,
        uniqueId: 'relationship-list',
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
            { prop: 'NAME', label: '关系名', minWidth: 180 },
            { prop: 'PARENT', label: '父对象', minWidth: 180 },
            { prop: 'PARENT_DESC_CN', label: '父对象描述(中文)', minWidth: 200 },
            { prop: 'PARENT_DESC', label: '父对象描述(英文)', minWidth: 200, visible: false },
            { prop: 'CHILD', label: '子对象', minWidth: 180 },
            { prop: 'CARDINALITY', label: '基数', width: 100 },
            { prop: 'ISDEFAULT', label: '默认', width: 70 },
            { prop: 'DBJOINREQUIRED', label: '必需连接', width: 90, visible: false },
            { prop: 'MAXRELATIONSHIPID', label: 'ID', width: 100, visible: false },
            { prop: 'WHERECLAUSE', label: 'Where 子句', minWidth: 300, className: 'wrap-cell' },
            { prop: 'REMARKS', label: '备注', minWidth: 250, className: 'wrap-cell' },
          ]),
        queryParamsColumnListEnable: false,
        queryParamsColumnList: [],
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
        keyword: this.formData.keyword,
        name: this.formData.name,
        parent: this.formData.parent,
        child: this.formData.child,
        pageNum: this.mainTable.queryParams.pageNum,
        pageSize: this.mainTable.queryParams.pageSize
      }
      getMaxRelationshipList(params)
        .then(res => {
          if (res.code === 200 && res.data) {
            this.mainTable.list = res.data.list || []
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
        keyword: '',
        name: '',
        parent: '',
        child: ''
      }
      this.hasSearched = false
      this.mainTable.list = []
      this.mainTable.total = 0
      this.total = 0
      this.mainTable.currentPage = 1
    },
    handleKeyDown(event, field) {
      if (event.altKey && event.key === 'F1') {
        event.preventDefault()
        const isKeyword = field.prop === 'KEYWORD'
        if (isKeyword) {
          this.$alert(
            `<div style="font-size:14px;line-height:2">
              <div><b>中文名：</b>${field.label}</div>
              <div><b>属性名：</b><code style="background:#f5f5f5;padding:2px 6px;border-radius:3px">${field.prop}</code></div>
              <div><b>说明：</b>通用搜索，可搜索关系名、父对象、子对象等字段</div>
            </div>`,
            '字段属性',
            { dangerouslyUseHTMLString: true, confirmButtonText: '确定' }
          )
        } else {
          this.fieldDetailLoading = true
          getMaxAttributeList({
            objectname: '=MAXRELATIONSHIP',
            attributename: '=' + field.prop,
            pageNum: 1,
            pageSize: 1
          }).then(res => {
            if (res.code === 200 && res.data && res.data.rows && res.data.rows.length > 0) {
              const attr = res.data.rows[0]
              this.$alert(
                `<div style="font-size:14px;line-height:2">
                  <div><b>中文名：</b>${field.label}</div>
                  <div><b>属性名：</b><code style="background:#f5f5f5;padding:2px 6px;border-radius:3px">${attr.ATTRIBUTENAME}</code></div>
                  <div><b>标题：</b>${attr.L_TITLE || '-'}</div>
                  <div><b>英文标题：</b>${attr.TITLE || '-'}</div>
                  <div><b>数据类型：</b>${attr.MAXTYPE || '-'}</div>
                  <div><b>长度：</b>${attr.LENGTH || '-'}</div>
                  <div><b>小数位数：</b>${attr.SCALE || '-'}</div>
                  <div><b>必需：</b>${attr.REQUIRED || '-'}</div>
                  <div><b>持久性：</b>${attr.PERSISTENT || '-'}</div>
                  <div><b>域：</b>${attr.DOMAINID || '-'}</div>
                  <div><b>默认值：</b>${attr.DEFAULTVALUE || '-'}</div>
                  <div><b>备注：</b>${attr.L_REMARKS || '-'}</div>
                </div>`,
                '字段属性',
                { dangerouslyUseHTMLString: true, confirmButtonText: '确定' }
              )
            } else {
              this.$alert(
                `<div style="font-size:14px;line-height:2">
                  <div><b>中文名：</b>${field.label}</div>
                  <div><b>属性名：</b><code style="background:#f5f5f5;padding:2px 6px;border-radius:3px">${field.prop}</code></div>
                  <div><b>说明：</b>未找到该字段的详细属性信息</div>
                </div>`,
                '字段属性',
                { dangerouslyUseHTMLString: true, confirmButtonText: '确定' }
              )
            }
            this.fieldDetailLoading = false
          }).catch(err => {
            this.$alert(
              `<div style="font-size:14px;line-height:2">
                <div><b>中文名：</b>${field.label}</div>
                <div><b>属性名：</b><code style="background:#f5f5f5;padding:2px 6px;border-radius:3px">${field.prop}</code></div>
                <div><b>错误：</b>查询字段信息失败</div>
              </div>`,
              '字段属性',
              { dangerouslyUseHTMLString: true, confirmButtonText: '确定' }
            )
            this.fieldDetailLoading = false
          })
        }
      }
    },
    handleRowClick(row) {
      this.currentRow = row
      this.objectDescription = null
      this.activeTab = 'simple'
      this.dialogVisible = true
      const hasDesc = row?.PARENT_DESC_CN || row?.PARENT_DESC
      if (!hasDesc && row?.PARENT) {
        getMaxObjectDescription(row.PARENT).then(res => {
          if (res.code === 200 && res.data) {
            this.objectDescription = res.data
            this.$nextTick(() => {
              this.initMonacoEditors()
            })
          }
        }).catch(() => {})
      }
    },
    onDialogOpened() {
      this.detailLoading = true
      this.$nextTick(() => {
        setTimeout(() => {
          this.initMonacoEditors()
          this.detailLoading = false
        }, 100)
      })
    },
    initMonacoEditors() {
      if (!this.currentRow) return
      const simpleJson = JSON.stringify({
        relationship: this.currentRow.NAME,
        child: this.currentRow.CHILD,
        whereClause: this.currentRow.WHERECLAUSE || null,
        remarks: this.currentRow.REMARKS || null,
        cardinality: this.currentRow.CARDINALITY || null,
        isDefault: this.currentRow.ISDEFAULT === '1'
      }, null, 2)
      const desc = this.objectDescription?.descriptionCn || this.objectDescription?.description || this.currentRow?.PARENT_DESC_CN || this.currentRow?.PARENT_DESC || ''
      const fullJson = JSON.stringify({
        object: this.currentRow.PARENT,
        description: desc,
        ignoreObjectMain: true,
        relationships: [{
          relationship: this.currentRow.NAME,
          child: this.currentRow.CHILD,
          whereClause: this.currentRow.WHERECLAUSE || null,
          remarks: this.currentRow.REMARKS || null,
          cardinality: this.currentRow.CARDINALITY || null,
          isDefault: this.currentRow.ISDEFAULT === '1'
        }]
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
    async generateFullJson() {
      this.genFullLoading = true
      try {
        const params = {
          keyword: this.formData.keyword,
          name: this.formData.name,
          parent: this.formData.parent,
          child: this.formData.child,
          pageNum: 1,
          pageSize: 10000
        }
        const res = await getMaxRelationshipList(params)
        if (res.code !== 200 || !res.data || !res.data.list) {
          this.$message.error('获取关系数据失败')
          return
        }

        const relationships = res.data.list
        const parentGroups = {}
        relationships.forEach(rel => {
          const parent = rel.PARENT
          if (!parentGroups[parent]) {
            parentGroups[parent] = []
          }
          parentGroups[parent].push(rel)
        })

        // 收集没有描述的父对象，批量查询
        const missingDescParents = Object.keys(parentGroups).filter(objName => {
          const firstRel = parentGroups[objName][0]
          return !firstRel?.PARENT_DESC_CN && !firstRel?.PARENT_DESC
        })
        const descMap = {}
        if (missingDescParents.length > 0) {
          const results = await Promise.allSettled(
            missingDescParents.map(name => getMaxObjectDescription(name))
          )
          results.forEach((result, i) => {
            if (result.status === 'fulfilled' && result.value.code === 200 && result.value.data) {
              const objName = missingDescParents[i]
              descMap[objName] = result.value.data.descriptionCn || result.value.data.description || ''
            }
          })
        }

        const maxObjects = Object.keys(parentGroups).map(objName => {
          const rels = parentGroups[objName]
          const firstRel = rels[0]
          const desc = firstRel?.PARENT_DESC_CN || firstRel?.PARENT_DESC || descMap[objName] || ''
          return {
            object: objName,
            description: desc,
            ignoreObjectMain: true,
            relationships: rels.map(rel => ({
              relationship: rel.NAME,
              child: rel.CHILD,
              whereClause: rel.WHERECLAUSE || null,
              remarks: rel.REMARKS || null,
              cardinality: rel.CARDINALITY || null,
              isDefault: rel.ISDEFAULT === '1'
            }))
          }
        })

        this.fullJsonData = JSON.stringify({ maxObjects }, null, 2)
        this.fullJsonDialogVisible = true
      } catch (err) {
        this.$message.error('生成JSON失败: ' + (err.message || String(err)))
      } finally {
        this.genFullLoading = false
      }
    },
    escapeHtml(text) {
      const div = document.createElement('div')
      div.textContent = text
      return div.innerHTML
    },
    initFullJsonEditor() {
      if (!this.fullJsonData) return
      if (!this.monacoLoaded) {
        import(/* webpackChunkName: "monaco" */ 'monaco-editor').then(monaco => {
          this.monacoLoaded = true
          this._monaco = monaco
          this.createFullJsonEditor()
        }).catch(err => {
          console.error('Monaco Editor 加载失败:', err)
        })
      } else {
        this.createFullJsonEditor()
      }
    },
    createFullJsonEditor() {
      if (!this._monaco || !this.fullJsonData) return
      const container = this.$refs.fullJsonMonacoRef
      if (!container) return
      if (this.fullJsonEditor) {
        this.fullJsonEditor.dispose()
        this.fullJsonEditor = null
      }
      this.fullJsonEditor = this._monaco.editor.create(container, {
        value: this.fullJsonData,
        language: 'json',
        theme: 'vs',
        readOnly: true,
        minimap: { enabled: false },
        lineNumbers: 'on',
        automaticLayout: true,
        fontSize: 13,
        wordWrap: 'on',
        folding: true,
        scrollbar: {
          verticalScrollbarSize: 10,
          horizontalScrollbarSize: 10
        }
      })
      setTimeout(() => {
        if (this.fullJsonEditor) {
          this.fullJsonEditor.layout()
        }
      }, 100)
    },
    disposeFullJsonEditor() {
      if (this.fullJsonEditor) {
        this.fullJsonEditor.dispose()
        this.fullJsonEditor = null
      }
    },
    copyFullJsonData() {
      if (this.fullJsonData) {
        this.copyToClipboard(this.fullJsonData, '完整关系JSON')
      }
    },
    copySimpleJson(row = this.currentRow) {
      if (!row) return
      const text = this.simpleEditor ? this.simpleEditor.getValue() : JSON.stringify({
        relationship: row.NAME,
        child: row.CHILD,
        whereClause: row.WHERECLAUSE || null,
        remarks: row.REMARKS || null,
        cardinality: row.CARDINALITY || null,
        isDefault: row.ISDEFAULT === '1'
      }, null, 2)
      this.copyToClipboard(text, '关系精简JSON')
    },
    copyFullJson(row = this.currentRow) {
      if (!row) return
      const text = this.fullEditor ? this.fullEditor.getValue() : JSON.stringify({
        object: row.PARENT,
        description: this.objectDescription?.descriptionCn || this.objectDescription?.description || row?.PARENT_DESC_CN || row?.PARENT_DESC || '',
        ignoreObjectMain: true,
        relationships: [{
          relationship: row.NAME,
          child: row.CHILD,
          whereClause: row.WHERECLAUSE || null,
          remarks: row.REMARKS || null,
          cardinality: row.CARDINALITY || null,
          isDefault: row.ISDEFAULT === '1'
        }]
      }, null, 2)
      this.copyToClipboard(text, '关系完整JSON')
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
.page-tip {
  color: #409eff;
  margin: 4px 0 0 0;
  font-size: 12px;
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