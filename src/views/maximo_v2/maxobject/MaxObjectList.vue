<template>
  <section class="query-page">
    <el-card>
      <div class="page-header-row">
        <div>
          <h2>MaxObject 查询</h2>
          <p class="page-summary">对象名支持精确匹配(=开头)和通配符(%模糊)，关键词搜索描述。点击行跳转详情。</p>
        </div>
        <div class="page-actions">
          <saved-query-panel ref="savedQuery" appname="MAXOBJECT" :default-where="savedWhere" @whereChange="handleQuery" />
          <el-button type="success" icon="el-icon-upload2" size="mini" style="margin-left: 8px;" @click="openImportDialog">导入</el-button>
        </div>
      </div>

      <el-form :model="formData" ref="queryForm" :inline="true" label-width="90px" @submit.native.prevent>
        <el-form-item label="对象名">
          <el-input v-model="formData.objectname" placeholder="如 =ASSET 或 %ITEM%" clearable style="width: 220px;" @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="formData.keyword" placeholder="描述模糊搜索..." clearable style="width: 300px;" @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item>
          <el-button type="cyan" icon="el-icon-search" size="mini" :loading="loading" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" size="mini" @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="result-panel">
        <el-table :data="objectList" stripe style="width: 100%" class="result-table" @row-click="handleRowClick">
          <el-table-column prop="objectName" label="对象名" width="200">
            <template slot-scope="scope">
              <el-link type="primary" :underline="false">{{ scope.row.objectName }}</el-link>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="英文描述" />
          <el-table-column prop="descriptionCn" label="中文描述" />
          <el-table-column label="操作" width="180" fixed="right">
            <template slot-scope="scope">
              <el-button type="text" size="small" @click.stop="goDetail(scope.row.objectName)">详情</el-button>
              <el-button type="text" size="small" @click.stop="showTodoSql(scope.row.objectName)">生成待办SQL</el-button>
            </template>
          </el-table-column>
        </el-table>
        <SksPagination
          v-if="total > 0"
          :total="total"
          :page.sync="pageNum"
          :limit.sync="pageSize"
          @pagination="handlePageChange"
        />
        <el-empty v-if="!loading && objectList.length === 0 && hasSearched" description="暂无查询结果" />
        <el-empty v-if="!loading && objectList.length === 0 && !hasSearched" description="请输入关键词后点击搜索" />
      </div>
    </el-card>

    <el-dialog title="待办SQL" :visible.sync="todoSqlDialog.visible" width="70%" top="10vh" :close-on-click-modal="true">
      <div style="margin-bottom:8px">
        <el-button size="mini" type="primary" @click="copyTodoSql">复制SQL</el-button>
      </div>
      <div ref="todoMonacoContainer" style="height:50vh;border:1px solid #dcdfe6"></div>
    </el-dialog>

    <el-dialog title="导入对象配置" :visible.sync="importDialog.visible" width="840px" top="5vh" :close-on-click-modal="false" @opened="onImportDialogOpened">
      <p style="margin:0 0 8px;color:#909399;font-size:12px;">
        粘贴对象配置 JSON（{"maxObjects": [...]} 或数组、单个对象，格式同「导出数据库配置」）；按 object 名称存在则更新、不存在则新增。
      </p>
      <el-checkbox v-model="importDialog.dmDeploySet" class="dm-deploy-checkbox">
        dmDeploySet（迁移模式：等同属性引用的对象/属性在目标环境还不存在时必须开启，先显式设置 MAXTYPE/LENGTH/SCALE 再设置 sameAs）
      </el-checkbox>
      <div v-loading="importDialog.loading" element-loading-text="导入中...">
        <div ref="importMonacoContainer" style="height:340px;border:1px solid #dcdfe6"></div>
      </div>
      <p style="margin:8px 0 0;color:#f56c6c;font-size:12px;" v-if="importDialog.error">{{ importDialog.error }}</p>
      <div v-if="importDialog.result" class="import-result-bar">
        <el-tag :type="importDialog.result.failed > 0 ? 'danger' : 'success'" size="small">{{ importDialog.result.failed > 0 ? '导入失败' : '导入成功' }}</el-tag>
        <span class="import-result-summary">{{ importSummaryText }}</span>
        <el-button type="text" size="mini" @click="openImportDetailDialog">查看详细返回信息</el-button>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="importDialog.visible = false">关 闭</el-button>
        <el-button type="primary" :loading="importDialog.loading" @click="submitImport">导 入</el-button>
      </span>
    </el-dialog>

    <!-- 导入详细返回信息弹窗 -->
    <el-dialog title="导入详细返回信息" :visible.sync="importDetailDialog.visible" width="85%" top="4vh" append-to-body @opened="onImportDetailOpened" @closed="disposeImportRawEditor">
      <el-tabs v-model="importDetailDialog.activeTab" type="border-card" @tab-click="onImportDetailOpened">
        <el-tab-pane :label="'列表(' + importDetailDialog.rows.length + ')'" name="list">
          <el-table :data="importDetailDialog.rows" border stripe size="mini" max-height="520">
            <el-table-column prop="label" label="对象 / 记录" min-width="300" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="90">
              <template slot-scope="scope">
                <el-tag v-if="scope.row.status" :type="scope.row.status === 'success' ? 'success' : 'danger'" size="mini">{{ scope.row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="operation" label="操作" width="90" />
            <el-table-column prop="message" label="消息" min-width="260" show-overflow-tooltip />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="原始JSON" name="raw" lazy>
          <div ref="importRawMonacoContainer" style="height:52vh;border:1px solid #dcdfe6"></div>
        </el-tab-pane>
      </el-tabs>
      <span slot="footer" class="dialog-footer">
        <el-button @click="importDetailDialog.visible = false">关 闭</el-button>
      </span>
    </el-dialog>
  </section>
</template>

<script>
import { getMaxObjectList, importMaxObjectsConfigur } from '@/api/maxobject'
import SavedQueryPanel from '@/views/components/SavedQueryPanel.vue'

export default {
  name: 'MaxObjectList',
  components: {
    SavedQueryPanel
  },
  data() {
    return {
      loading: false,
      hasSearched: false,
      objectList: [],
      pageNum: 1,
      pageSize: 20,
      total: 0,
      formData: {
        objectname: '',
        keyword: ''
      },
      todoSqlDialog: { visible: false, sql: '', editor: null },
      importDialog: { visible: false, loading: false, error: '', text: '', dmDeploySet: false, result: null, rawResult: '' },
      importDetailDialog: { visible: false, activeTab: 'list', rows: [] },
      importRawEditor: null,
      importEditor: null,
      // 后端返回的本次执行 where 条件，用于保存查询预填
      savedWhere: ''
    }
  },
  computed: {
    importSummaryText() {
      const r = this.importDialog.result
      if (!r) return ''
      const parts = ['新增 ' + (r.added || 0), '更新 ' + (r.updated || 0), '删除 ' + (r.deleted || 0), '失败 ' + (r.failed || 0), '共 ' + (r.total || 0) + ' 条']
      return parts.join(' / ')
    }
  },
  methods: {
    // 当前生效的自定义 where（保存查询选择），未设置返回 ''
    getCustomWhere() {
      return this.$refs.savedQuery ? this.$refs.savedQuery.getWhere() : ''
    },
    handleQuery() {
      this.hasSearched = true
      this.loading = true
      this.pageNum = 1
      getMaxObjectList(this.formData.objectname, this.formData.keyword, this.pageNum, this.pageSize, this.getCustomWhere() || undefined)
        .then(res => {
          if (res.code === 200 && res.data) {
            this.objectList = res.data.rows || []
            this.total = res.data.total || 0
            this.savedWhere = res.data.where || ''
          } else {
            this.objectList = []
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
    handlePageChange({ page, limit }) {
      this.pageNum = page
      this.pageSize = limit
      this.loading = true
      getMaxObjectList(this.formData.objectname, this.formData.keyword, page, limit, this.getCustomWhere() || undefined)
        .then(res => {
          if (res.code === 200 && res.data) {
            this.objectList = res.data.rows || []
            this.total = res.data.total || 0
            this.savedWhere = res.data.where || ''
          } else {
            this.objectList = []
            this.total = 0
          }
        })
        .catch(err => {
          this.$message.error('请求失败: ' + (err.message || String(err)))
        })
        .finally(() => {
          this.loading = false
        })
    },
    resetForm() {
      this.formData.objectname = ''
      this.formData.keyword = ''
      this.hasSearched = false
      this.objectList = []
      this.pageNum = 1
      this.pageSize = 20
      this.total = 0
      this.savedWhere = ''
      if (this.$refs.savedQuery) this.$refs.savedQuery.clear()
    },
    handleRowClick(row) {
      this.goDetail(row.objectName)
    },
    goDetail(objectName) {
      this.$router.push({
        path: `/maxobject-detail/index/${objectName}`
      })
    },
    showTodoSql(objectName) {
      this.todoSqlDialog.sql = `exists(
select 1 from WFASSIGNMENT
 where ownertable = '${objectName}'
   and WFASSIGNMENT.ownerid = ${objectName}.${objectName}ID
   and assignstatus in (select value from synonymdomain where domainid='WFASGNSTATUS' and maxvalue='ACTIVE')
   and ASSIGNCODE = :&PERSONID&)`
      this.todoSqlDialog.visible = true
      this.$nextTick(() => this.initTodoSqlEditor())
    },
    initTodoSqlEditor() {
      const container = this.$refs.todoMonacoContainer
      if (!container) return
      if (this.todoSqlDialog.editor) {
        this.todoSqlDialog.editor.dispose()
        this.todoSqlDialog.editor = null
      }
      import(/* webpackChunkName: "monaco" */ 'monaco-editor').then(monaco => {
        this.todoSqlDialog.editor = monaco.editor.create(container, {
          value: this.todoSqlDialog.sql,
          language: 'sql',
          readOnly: true,
          theme: 'vs',
          automaticLayout: true,
          minimap: { enabled: false },
          scrollBeyondLastLine: false,
          fontSize: 13,
          wordWrap: 'on'
        })
      }).catch(err => {
        console.error('Monaco Editor 加载失败:', err)
      })
    },
    copyTodoSql() {
      if (!this.todoSqlDialog.sql) return
      const el = document.createElement('textarea')
      el.value = this.todoSqlDialog.sql
      document.body.appendChild(el)
      el.select()
      document.execCommand('copy')
      document.body.removeChild(el)
      this.$message.success('已复制到剪贴板')
    },
    // ===== 导入对象配置(SKS.AUTOSCRIPT.LIBRARY) =====
    openImportDialog() {
      this.importDialog = { visible: true, loading: false, error: '', text: '', dmDeploySet: false, result: null, rawResult: '' }
    },
    onImportDialogOpened() {
      this.$nextTick(() => {
        setTimeout(() => this.initImportEditor(), 200)
      })
    },
    initImportEditor() {
      const container = this.$refs.importMonacoContainer
      if (!container) return
      if (this.importEditor) {
        this.importEditor.dispose()
        this.importEditor = null
      }
      import(/* webpackChunkName: "monaco" */ 'monaco-editor').then(monaco => {
        this.importEditor = monaco.editor.create(container, {
          value: this.importDialog.text || '',
          language: 'json',
          readOnly: false,
          theme: 'vs',
          automaticLayout: true,
          minimap: { enabled: false },
          scrollBeyondLastLine: false,
          fontSize: 13,
          wordWrap: 'on',
          tabSize: 2
        })
      }).catch(err => {
        console.error('Monaco Editor 加载失败:', err)
      })
    },
    disposeImportEditor() {
      if (this.importEditor) {
        this.importEditor.dispose()
        this.importEditor = null
      }
    },
    submitImport() {
      const d = this.importDialog
      const text = (this.importEditor ? this.importEditor.getValue() : d.text || '').trim()
      if (!text) {
        d.error = '请粘贴 JSON 内容'
        return
      }
      let parsed
      try {
        parsed = JSON.parse(text)
      } catch (err) {
        d.error = 'JSON 解析失败: ' + err.message
        return
      }
      // 支持数组 / {"maxObjects":[...]} 原样 / 单个对象定义, 统一归一成 {maxObjects:[...]}
      let importData
      if (Array.isArray(parsed)) {
        importData = { maxObjects: parsed }
      } else if (Array.isArray(parsed.maxObjects)) {
        importData = parsed
      } else if (parsed.object) {
        importData = { maxObjects: [parsed] }
      } else {
        d.error = 'JSON 中未找到 maxObjects 数组或 object 名称'
        return
      }
      d.loading = true
      d.error = ''
      d.result = null
      d.rawResult = ''
      const merged = { total: importData.maxObjects.length, processed: 0, added: 0, updated: 0, deleted: 0, failed: 0, results: [] }
      importMaxObjectsConfigur(importData, { dmDeploySet: d.dmDeploySet ? 'true' : 'false' }).then(res => {
        const body = res.data || {}
        if (body.status === 'error') {
          d.error = body.message || '导入失败'
          return
        }
        this.mergeImportResponse(merged, res)
        d.result = merged
        d.rawResult = JSON.stringify(merged, null, 2)
        if (merged.failed > 0) {
          this.$message.warning('导入完成，存在失败记录，可点击「查看详细返回信息」')
        } else {
          this.$message.success(body.message || '导入成功')
        }
        if (this.hasSearched) {
          this.handleQuery()
        }
      }).catch(err => {
        d.error = '导入失败: ' + (err.message || String(err))
      }).finally(() => {
        d.loading = false
      })
    },
    // 按行级统计合并返回结果（res.data = 脚本返回体 {status, data, message}）
    mergeImportResponse(merged, res) {
      const body = res.data || {}
      const data = body.data || {}
      const isOk = body.status === 'success' && data.status !== 'error'
      const results = data.results || []
      merged.processed = (merged.processed || 0) + results.length
      merged.added += Number(data.added || 0)
      merged.updated += Number(data.updated || 0)
      merged.deleted += Number(data.deleted || 0)
      if (!isOk) {
        merged.failed += (results.length || 1)
        merged.results = merged.results.concat(results.length ? results : [{
          'sks:ref': 'batch',
          _status: 'failed',
          _operation: 'none',
          _message: body.message || data.message || '调用失败'
        }])
      } else {
        merged.failed += Number(data.failed || 0)
        merged.results = merged.results.concat(results)
      }
      return merged
    },
    openImportDetailDialog() {
      const r = this.importDialog.result
      if (!r || !r.results || !r.results.length) {
        this.$message.info('没有明细结果')
        return
      }
      this.importDetailDialog.rows = r.results.map((row, idx) => ({
        label: row['sks:ref'] || row.object || ('#' + (idx + 1)),
        status: row._status,
        operation: row._operation,
        message: row._message || ''
      }))
      this.importDetailDialog.activeTab = 'list'
      this.importDetailDialog.visible = true
    },
    onImportDetailOpened() {
      if (this.importDetailDialog.activeTab !== 'raw') return
      this.$nextTick(() => {
        const container = this.$refs.importRawMonacoContainer
        if (!container) return
        if (this.importRawEditor) {
          this.importRawEditor.setValue(this.importDialog.rawResult || '')
          return
        }
        import(/* webpackChunkName: "monaco" */ 'monaco-editor').then(monaco => {
          this.importRawEditor = monaco.editor.create(container, {
            value: this.importDialog.rawResult || '',
            language: 'json',
            readOnly: true,
            theme: 'vs',
            automaticLayout: true,
            minimap: { enabled: false },
            scrollBeyondLastLine: false,
            fontSize: 13,
            wordWrap: 'on'
          })
        }).catch(err => {
          console.error('Monaco Editor 加载失败:', err)
        })
      })
    },
    disposeImportRawEditor() {
      if (this.importRawEditor) {
        this.importRawEditor.dispose()
        this.importRawEditor = null
      }
    }
  },
  watch: {
    'todoSqlDialog.visible'(val) {
      if (!val && this.todoSqlDialog.editor) {
        this.todoSqlDialog.editor.dispose()
        this.todoSqlDialog.editor = null
      }
    },
    'importDialog.visible'(val) {
      if (!val) {
        this.disposeImportEditor()
      }
    }
  },
  beforeDestroy() {
    if (this.todoSqlDialog.editor) {
      this.todoSqlDialog.editor.dispose()
      this.todoSqlDialog.editor = null
    }
    this.disposeImportEditor()
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
.page-actions {
  display: flex;
  align-items: center;
}
.import-stack {
  max-height: 180px;
  overflow: auto;
  margin: 0;
  padding: 8px;
  background: #f5f7fa;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  color: #f56c6c;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
}
.dm-deploy-checkbox {
  display: flex;
  align-items: flex-start;
  margin: 0 0 8px;
}
.dm-deploy-checkbox ::v-deep .el-checkbox__input {
  margin-top: 3px;
}
.dm-deploy-checkbox ::v-deep .el-checkbox__label {
  white-space: normal;
  word-break: break-word;
  line-height: 1.5;
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
.page-summary {
  color: #606266;
  margin: 0;
}
.result-panel {
  margin-top: 20px;
}
.result-table {
  margin-top: 16px;
}
</style>