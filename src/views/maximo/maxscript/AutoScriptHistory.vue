<template>
  <section class="history-page">
    <el-card>
      <div class="page-header-row">
        <div>
          <h2>脚本历史记录查询</h2>
          <p class="page-summary">查询 IBM_AUTOSCRIPT_HISTORY 脚本历史记录，支持脚本名称、描述、创建人模糊过滤，按历史ID倒序。</p>
        </div>
        <div class="header-actions">
          <saved-query-panel ref="savedQuery" appname="AUTOSCRIPT_HISTORY" :default-where="savedWhere" @whereChange="handleQuery" />
        </div>
      </div>

      <el-form :model="formData" ref="queryForm" :inline="true" label-width="80px" @submit.native.prevent>
        <el-form-item label="脚本名称">
          <el-input v-model="formData.autoscript" placeholder="AUTOSCRIPT 模糊搜索" clearable @keyup.enter.native="handleQuery" style="width:200px" />
        </el-form-item>
        <el-form-item label="脚本描述">
          <el-input v-model="formData.description" placeholder="DESCRIPTION 模糊搜索" clearable @keyup.enter.native="handleQuery" style="width:200px" />
        </el-form-item>
        <el-form-item label="创建人">
          <el-input v-model="formData.createperson" placeholder="CREATEPERSON 模糊搜索" clearable @keyup.enter.native="handleQuery" style="width:200px" />
        </el-form-item>
        <el-form-item label="脚本内容">
          <el-input v-model="formData.source" placeholder="SOURCE 模糊搜索" clearable @keyup.enter.native="handleQuery" style="width:200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="cyan" icon="el-icon-search" size="mini" :loading="loading" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" size="mini" @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="result-panel">
        <el-table :data="list" border style="width:100%" v-loading="loading">
          <el-table-column prop="AUTOSCRIPT" label="脚本名称" min-width="160">
            <template slot-scope="scope">
              <el-link type="primary" :underline="false" @click="showDetail(scope.row)">{{ scope.row.AUTOSCRIPT }}</el-link>
            </template>
          </el-table-column>
          <el-table-column prop="VERSION" label="版本" width="90" />
          <el-table-column prop="DESCRIPTION" label="描述" min-width="180" show-overflow-tooltip />
          <el-table-column prop="ALIASNAME" label="推送人别名" width="120" />
          <el-table-column prop="HOSTNAME" label="主机名" width="130" />
          <el-table-column prop="CREATEPERSON" label="创建人" width="120" />
          <el-table-column prop="CREATETIME" label="创建时间" width="160">
            <template slot-scope="scope">{{ parseTime(scope.row.CREATETIME) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="100" fixed="right">
            <template slot-scope="scope">
              <el-button type="text" size="small" @click="showDetail(scope.row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>

        <pagination v-show="total > 0"
          :total="total"
          :page.sync="pageNum"
          :limit.sync="pageSize"
          @pagination="fetchList"
        />
        <el-empty v-if="!loading && total === 0 && hasSearched" description="暂无查询结果" />
        <el-empty v-if="!loading && total === 0 && !hasSearched" description="请输入关键词后点击搜索" />
      </div>
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog :title="'历史详情: ' + detailDialog.name" :visible.sync="detailDialog.visible" width="85%" top="2vh" :close-on-click-modal="false" @closed="onDetailClosed">
      <div v-loading="detailDialog.loading" style="max-height:82vh;overflow-y:auto">
        <div v-if="Object.keys(detailInfo).length > 0">
          <p class="section-title">历史记录信息</p>
          <el-form label-width="140px" size="small">
            <el-row :gutter="16" type="flex" class="detail-row">
              <el-col :span="8" v-for="(val, key) in detailInfo" :key="key">
                <el-form-item :label="getFieldLabel(key)">
                  <el-input :value="formatVal(val)" readonly size="small" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>

          <!-- SOURCE 显示在最下面，Monaco Editor -->
          <p class="section-title">脚本源码 (SOURCE)</p>
          <div ref="monacoContainer" style="height:60vh;border:1px solid #dcdfe6"></div>
        </div>
        <el-empty v-else-if="!detailDialog.loading" description="暂无历史记录" />
      </div>
    </el-dialog>
  </section>
</template>

<script>
import { getAutoScriptHistory, getAutoScriptHistoryDetail } from '@/api/autoscript'
import { parseTime } from '@/utils/ruoyi'
import { stringTrim } from '@/utils/sks'
import SavedQueryPanel from '@/views/components/SavedQueryPanel.vue'

// SCRIPTLANGUAGE 到 Monaco 语言映射
const LANG_MAP = {
  'javascript': 'javascript', 'js': 'javascript', 'JS': 'javascript',
  'JavaScript': 'javascript', 'nashorn': 'javascript', 'Nashorn': 'javascript',
  'ecmascript': 'javascript', 'ECMAScript': 'javascript',
  'python': 'python', 'jython': 'python', 'py': 'python', 'MBR': 'python'
}

// 字段中文标签
const FIELD_LABELS = {
  IBM_AUTOSCRIPT_HISTORYID: '历史记录ID', AUTOSCRIPT: '脚本', VERSION: '版本',
  DESCRIPTION: '描述', ALIASNAME: '推送人别名', HOSTNAME: '主机名',
  CREATEPERSON: '创建人', CREATETIME: '创建时间', HASLD: '具有详细描述',
  SOURCE: '脚本源码', SCRIPTLANGUAGE: '脚本语言', STATUS: '状态',
  SCHEDULEDSTATUS: '已调度状态', COMMENTS: '注释', OWNERID: '所有者',
  OWNERNAME: '姓名', OWNERPHONE: '电话', OWNEREMAIL: '电子邮件',
  CREATEDBYID: '创建者', ORGID: '组织', SITEID: '地点', ACTION: '操作',
  CREATEDDATE: '创建日期', CATEGORY: '类型', STATUSDATE: '状态日期',
  CHANGEDATE: '变更日期', OWNER: '所有者人员', CREATEDBY: '创建人',
  CHANGEBY: '变更人', AUTOSCRIPTID: '唯一标识', LANGCODE: '语言代码',
  USERDEFINED: '用户定义', LOGLEVEL: '日志级别', INTERFACE: '是接口',
  ACTIVE: '活动', IBM_PACKAGEPATH: '包路径', CREATEDBYPHONE: '创建者电话',
  CREATEDBYNAME: '创建者姓名', CREATEDBYEMAIL: '创建者邮箱',
  L_DESCRIPTION: '中文描述', ROWSTAMP: '行时间戳'
}

export default {
  name: 'AutoScriptHistory',
  components: {
    SavedQueryPanel
  },
  data() {
    return {
      loading: false,
      hasSearched: false,
      list: [],
      total: 0,
      pageNum: 1,
      pageSize: 20,
      formData: { autoscript: '', description: '', createperson: '', source: '' },
      detailDialog: { visible: false, name: '', loading: false },
      detailData: {},
      monacoEditor: null,
      savedWhere: ''
    }
  },
  computed: {
    // 详情信息（排除 SOURCE，SOURCE 在底部 Monaco 中显示）
    detailInfo() {
      const info = {}
      for (const key of Object.keys(this.detailData)) {
        if (key === 'SOURCE') continue
        info[key] = this.detailData[key]
      }
      return info
    }
  },
  created() {
    const autoscript = this.$route.query && this.$route.query.autoscript
    if (autoscript) {
      this.formData.autoscript = String(autoscript)
      this.handleQuery()
    }
  },
  methods: {
    getCustomWhere() {
      return this.$refs.savedQuery ? this.$refs.savedQuery.getWhere() : ''
    },
    handleQuery() {
      this.hasSearched = true
      this.pageNum = 1
      this.fetchList()
    },
    fetchList() {
      this.loading = true
      const trim = v => v ? stringTrim(v) : undefined
      getAutoScriptHistory({
        autoscript: trim(this.formData.autoscript),
        description: trim(this.formData.description),
        createperson: trim(this.formData.createperson),
        source: trim(this.formData.source),
        pageNum: this.pageNum,
        pageSize: this.pageSize,
        where: this.getCustomWhere() || undefined
      }).then(res => {
        if (res.code === 200 && res.data) {
          this.list = res.data.rows || []
          this.total = res.data.total || 0
        } else {
          this.list = []
          this.total = 0
          this.$message.error(res.message || '查询失败')
        }
      }).catch(err => {
        this.$message.error('查询失败: ' + (err.message || String(err)))
      }).finally(() => {
        this.loading = false
      })
    },
    resetForm() {
      this.formData = { autoscript: '', description: '', createperson: '', source: '' }
      this.hasSearched = false
      this.list = []
      this.total = 0
      this.pageNum = 1
      this.savedWhere = ''
      if (this.$refs.savedQuery) this.$refs.savedQuery.clear()
    },
    showDetail(row) {
      this.detailDialog.visible = true
      this.detailDialog.name = row.AUTOSCRIPT + ' (Version: ' + row.VERSION + ')'
      this.detailDialog.loading = true
      this.detailData = {}
      this.disposeMonaco()

      getAutoScriptHistoryDetail(row.IBM_AUTOSCRIPT_HISTORYID).then(res => {
        if (res.code === 200 && res.data) {
          this.detailData = res.data
          const lang = res.data.SCRIPTLANGUAGE || 'javascript'
          const monacoLang = LANG_MAP[lang] || 'plaintext'
          this.detailDialog.loading = false
          this.$nextTick(() => {
            this.initMonaco(res.data.SOURCE || '', monacoLang)
          })
        } else {
          this.detailDialog.loading = false
          this.$message.error(res.message || '获取历史详情失败')
        }
      }).catch(err => {
        this.detailDialog.loading = false
        this.$message.error('获取历史详情失败: ' + (err.message || String(err)))
      })
    },
    onDetailClosed() {
      this.detailData = {}
      this.disposeMonaco()
    },
    initMonaco(code, language) {
      const container = this.$refs.monacoContainer
      if (!container) {
        // 容器可能还未渲染，延迟重试
        setTimeout(() => {
          if (this.$refs.monacoContainer) {
            this.doInitMonaco(this.$refs.monacoContainer, code, language)
          }
        }, 200)
        return
      }
      this.doInitMonaco(container, code, language)
    },
    doInitMonaco(container, code, language) {
      import(/* webpackChunkName: "monaco" */ 'monaco-editor').then(monaco => {
        this.disposeMonaco()
        this.monacoEditor = monaco.editor.create(container, {
          value: code,
          language: language,
          readOnly: true,
          theme: 'vs',
          automaticLayout: true,
          minimap: { enabled: true },
          scrollBeyondLastLine: false,
          fontSize: 13,
          wordWrap: 'on'
        })
      }).catch(err => {
        console.error('Monaco Editor 加载失败:', err)
        container.innerHTML = '<textarea readonly style="width:100%;height:100%;font-family:monospace">' + this.escapeHtml(code) + '</textarea>'
      })
    },
    disposeMonaco() {
      if (this.monacoEditor) {
        this.monacoEditor.dispose()
        this.monacoEditor = null
      }
    },
    escapeHtml(str) {
      return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    },
    getFieldLabel(fieldName) {
      return FIELD_LABELS[fieldName] || fieldName
    },
    formatVal(val) {
      if (val === null || val === undefined) return ''
      const parsed = this.parseTimestampStr(val)
      if (parsed) return parsed
      return String(val)
    },
    // 支持 13 位数字时间戳（number 或字符串形式）
    parseTimestampStr(val) {
      let num
      if (typeof val === 'number') {
        num = val
      } else if (/^\d{13}$/.test(String(val).trim())) {
        num = Number(val)
      } else {
        return null
      }
      if (num > 1000000000000 && num < 10000000000000) {
        return parseTime(num)
      }
      return null
    }
  },
  beforeDestroy() {
    this.disposeMonaco()
  }
}
</script>

<style lang="scss" scoped>
.history-page {
  padding: 16px;
}
.page-header-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
  h2 {
    margin: 0 0 6px 0;
  }
}
.page-summary {
  color: #606266;
  margin: 0;
}
.header-actions {
  display: flex;
  align-items: center;
}
.result-panel {
  margin-top: 16px;
}
.section-title {
  font-weight: 600;
  font-size: 13px;
  color: #303133;
  margin: 12px 0 8px 0;
}
.detail-row {
  flex-wrap: wrap;
}
</style>