<template>
  <section class="query-page">
    <el-card>
      <div class="page-header-row">
        <div>
          <h2>MaxObject 查询</h2>
          <p class="page-summary">对象名支持精确匹配(=开头)和通配符(%模糊)，关键词搜索描述。点击行跳转详情。</p>
        </div>
        <saved-query-panel ref="savedQuery" appname="MAXOBJECT" :default-where="savedWhere" @whereChange="handleQuery" />
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
  </section>
</template>

<script>
import { getMaxObjectList } from '@/api/maxobject'
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
      // 后端返回的本次执行 where 条件，用于保存查询预填
      savedWhere: ''
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
    }
  },
  watch: {
    'todoSqlDialog.visible'(val) {
      if (!val && this.todoSqlDialog.editor) {
        this.todoSqlDialog.editor.dispose()
        this.todoSqlDialog.editor = null
      }
    }
  },
  beforeDestroy() {
    if (this.todoSqlDialog.editor) {
      this.todoSqlDialog.editor.dispose()
      this.todoSqlDialog.editor = null
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