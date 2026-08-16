<template>
  <div class="audit-panel" :class="{ 'is-embedded': embedded }">
    <el-form :model="formData" ref="queryForm" :inline="true" label-width="80px" @submit.native.prevent>
      <el-form-item label="脚本名称">
        <el-input v-model="formData.autoscript" placeholder="AUTOSCRIPT" clearable @keyup.enter.native="handleQuery" style="width:220px" />
      </el-form-item>
      <el-form-item label="脚本内容">
        <el-input v-model="formData.source" placeholder="SOURCE 模糊搜索" clearable @keyup.enter.native="handleQuery" style="width:220px" />
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
          <el-table-column label="操作" width="80" fixed="right">
            <template slot-scope="scope">
              <el-button type="text" size="small" @click.stop="showDetail(scope.row)">详情</el-button>
            </template>
          </el-table-column>
        </template>
        <template slot="none-EAUDITTYPE" slot-scope="scope">
          <el-tag :type="auditTypeTag(scope.row.EAUDITTYPE)" size="mini">{{ auditTypeLabel(scope.row.EAUDITTYPE) }}</el-tag>
        </template>
        <template slot="none-EAUDITTYPE_RAW" slot-scope="scope">
          <span class="type-raw">{{ scope.row.EAUDITTYPE }}</span>
        </template>
        <template slot="none-EAUDITTIMESTAMP" slot-scope="scope">
          {{ formatTime(scope.row.EAUDITTIMESTAMP) }}
        </template>
        <template slot="none-EAUDITTRANSID" slot-scope="scope">
          <el-link type="primary" :underline="false" @click.stop="showDetail(scope.row)">{{ scope.row.EAUDITTRANSID }}</el-link>
        </template>
        <template slot="default">
        </template>
      </SksTable>
      <el-empty v-if="!loading && total === 0 && hasSearched" description="暂无查询结果" />
      <el-empty v-if="!loading && total === 0 && !hasSearched" description="请输入关键词后点击搜索" />
    </div>

    <!-- 详情弹窗 -->
    <el-dialog :title="'审计详情: ' + detailDialog.name" :visible.sync="detailDialog.visible" width="85%" top="2vh" :close-on-click-modal="false" append-to-body @closed="onDetailClosed">
      <div v-loading="detailDialog.loading" style="max-height:82vh;overflow-y:auto">
        <div v-if="Object.keys(detailInfo).length > 0">
          <p class="section-title">脚本信息</p>
          <el-form label-width="140px" size="small">
            <el-row :gutter="16" type="flex" class="detail-row">
              <el-col :span="8" v-for="(val, key) in detailInfo" :key="key">
                <el-form-item :label="getFieldLabel(key)">
                  <span v-if="key === 'EAUDITTYPE'" class="detail-form-value">
                    <template v-if="val !== null && val !== undefined && val !== ''">
                      <el-tag :type="auditTypeTag(val)" size="mini">{{ auditTypeLabel(val) }}</el-tag>
                      <span class="type-raw detail-type-raw">{{ val }}</span>
                    </template>
                    <span v-else>-</span>
                  </span>
                  <span v-else-if="key === 'EAUDITTIMESTAMP'" class="detail-form-value">{{ formatTime(val) || '-' }}</span>
                  <span v-else class="detail-form-value">{{ formatVal(val) || '-' }}</span>
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>

          <!-- SOURCE 显示在最下面，Monaco Editor -->
          <p class="section-title">脚本源码 (SOURCE)</p>
          <div ref="monacoContainer" style="height:60vh;border:1px solid #dcdfe6"></div>
        </div>
        <el-empty v-else-if="!detailDialog.loading" description="暂无审计记录" />
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getAutoScriptAuditList, getAutoScriptAuditDetail } from '@/api/autoscript'
import { sksPageMixin } from "sks-plugin-el-erp/lib/sks-page";
import { parseTime } from '@/utils/ruoyi'
import { stringTrim } from '@/utils/sks'

// SCRIPTLANGUAGE 到 Monaco 语言映射
const LANG_MAP = {
  'javascript': 'javascript', 'js': 'javascript', 'JS': 'javascript',
  'JavaScript': 'javascript', 'nashorn': 'javascript', 'Nashorn': 'javascript',
  'ecmascript': 'javascript', 'ECMAScript': 'javascript',
  'python': 'python', 'jython': 'python', 'py': 'python', 'MBR': 'python'
}

// 审计类型
const AUDIT_TYPE_MAP = {
  'I': { label: '新增', tag: 'success' },
  'U': { label: '修改', tag: 'warning' },
  'D': { label: '删除', tag: 'danger' }
}

// 字段中文标签
const FIELD_LABELS = {
  EAUDITTRANSID: '交易ID', EAUDITUSERNAME: '审计用户', EAUDITTIMESTAMP: '审计时间',
  EAUDITTYPE: '操作类型', ESIGTRANSID: '电子签名交易ID',
  AUTOSCRIPT: '脚本', AUTOSCRIPTID: '唯一标识', VERSION: '版本', DESCRIPTION: '描述',
  SCRIPTLANGUAGE: '脚本语言', STATUS: '状态', SCHEDULEDSTATUS: '已调度状态',
  ACTIVE: '活动', USERDEFINED: '用户定义', LOGLEVEL: '日志级别',
  OWNERID: '所有者', OWNERNAME: '姓名', OWNERPHONE: '电话', OWNEREMAIL: '电子邮件', OWNER: '所有者人员',
  CREATEDBYID: '创建者', CREATEDBYNAME: '创建者姓名', CREATEDBYPHONE: '创建者电话',
  CREATEDBYEMAIL: '创建者邮箱', CREATEDBY: '创建人', CREATEDDATE: '创建日期',
  CHANGEBY: '变更人', CHANGEDATE: '变更日期', STATUSDATE: '状态日期',
  COMMENTS: '注释', CATEGORY: '类型', INTERFACE: '是接口', HASLD: '具有详细描述',
  LANGCODE: '语言代码', ORGID: '组织', SITEID: '地点', ACTION: '操作',
  IBM_PACKAGEPATH: '包路径', ROWSTAMP: '行时间戳'
}

export default {
  name: 'AutoScriptAuditPanel',
  mixins: [
    sksPageMixin,
  ],
  props: {
    // 初始/外部指定的脚本名称过滤条件
    autoscript: {
      type: String,
      default: ''
    },
    // 是否内嵌显示（例如放在对话框内），内嵌时去掉外层留白
    embedded: {
      type: Boolean,
      default: false
    },
    // 挂载后是否根据 autoscript 自动查询
    autoQuery: {
      type: Boolean,
      default: true
    }
  },
  data() {
    return {
      loading: false,
      hasSearched: false,
      total: 0,
      formData: { autoscript: '', source: '' },
      detailDialog: { visible: false, name: '', loading: false },
      detailData: {},
      monacoEditor: null
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
  watch: {
    autoscript(val) {
      this.formData.autoscript = val || ''
      if (val) {
        this.handleQuery()
      }
    }
  },
  methods: {
    initMainTableParam() {
      return {
        ownerName: 'A_AUTOSCRIPT',
        uniqueId: 'eaudit-transid',
        sksAppName: 'autoscript-audit',
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
            { prop: 'EAUDITTRANSID', label: '交易ID', minWidth: 100, htmlType: 'none' },
            { prop: 'AUTOSCRIPT', label: '脚本名称', minWidth: 180 },
            { prop: 'VERSION', label: '版本', width: 90 },
            { prop: 'EAUDITTYPE', label: '操作类型', width: 100, htmlType: 'none' },
            { prop: 'EAUDITTYPE_RAW', label: '操作类型原值', width: 100, htmlType: 'none', visible: false },
            { prop: 'EAUDITUSERNAME', label: '审计用户', width: 120 },
            { prop: 'EAUDITTIMESTAMP', label: '审计时间', width: 180, htmlType: 'none' },
            // 以下字段默认隐藏，"显示所有列"后可见
            { prop: 'DESCRIPTION', label: '描述', minWidth: 160, visible: false },
            { prop: 'ESIGTRANSID', label: '电子签名交易ID', minWidth: 120, visible: false },
            { prop: 'SCRIPTLANGUAGE', label: '脚本语言', minWidth: 100, visible: false },
            { prop: 'STATUS', label: '状态', width: 80, visible: false },
            { prop: 'SCHEDULEDSTATUS', label: '已调度状态', width: 90, visible: false },
            { prop: 'ACTIVE', label: '活动', width: 60, visible: false },
            { prop: 'USERDEFINED', label: '用户定义', width: 80, visible: false },
            { prop: 'LOGLEVEL', label: '日志级别', width: 90, visible: false },
            { prop: 'AUTOSCRIPTID', label: '唯一标识', minWidth: 120, visible: false },
            { prop: 'OWNERID', label: '所有者', minWidth: 120, visible: false },
            { prop: 'OWNERNAME', label: '姓名', minWidth: 100, visible: false },
            { prop: 'OWNERPHONE', label: '电话', minWidth: 110, visible: false },
            { prop: 'OWNEREMAIL', label: '电子邮件', minWidth: 150, visible: false },
            { prop: 'OWNER', label: '所有者人员', minWidth: 120, visible: false },
            { prop: 'CREATEDBYID', label: '创建者', minWidth: 120, visible: false },
            { prop: 'CREATEDBYNAME', label: '创建者姓名', minWidth: 100, visible: false },
            { prop: 'CREATEDBYPHONE', label: '创建者电话', minWidth: 110, visible: false },
            { prop: 'CREATEDBYEMAIL', label: '创建者邮箱', minWidth: 150, visible: false },
            { prop: 'CREATEDBY', label: '创建人', minWidth: 100, visible: false },
            { prop: 'CREATEDDATE', label: '创建日期', width: 160, visible: false },
            { prop: 'CHANGEBY', label: '变更人', minWidth: 100, visible: false },
            { prop: 'CHANGEDATE', label: '变更日期', width: 160, visible: false },
            { prop: 'STATUSDATE', label: '状态日期', width: 160, visible: false },
            { prop: 'COMMENTS', label: '注释', minWidth: 150, visible: false },
            { prop: 'CATEGORY', label: '类型', minWidth: 100, visible: false },
            { prop: 'INTERFACE', label: '是接口', width: 80, visible: false },
            { prop: 'HASLD', label: '具有详细描述', width: 100, visible: false },
            { prop: 'LANGCODE', label: '语言代码', width: 90, visible: false },
            { prop: 'ORGID', label: '组织', minWidth: 100, visible: false },
            { prop: 'SITEID', label: '地点', minWidth: 100, visible: false },
            { prop: 'ACTION', label: '操作', width: 80, visible: false },
            { prop: 'IBM_PACKAGEPATH', label: '包路径', minWidth: 140, visible: false },
            { prop: 'ROWSTAMP', label: '行时间戳', width: 110, visible: false }
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
      const trim = v => v ? stringTrim(v) : undefined
      const params = {
        autoscript: trim(this.formData.autoscript),
        source: trim(this.formData.source),
        pageNum: this.mainTable.queryParams.pageNum,
        pageSize: this.mainTable.queryParams.pageSize
      }
      getAutoScriptAuditList(params)
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
          this.$message.error('查询失败: ' + (err.message || String(err)))
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
      this.formData = { autoscript: '', source: '' }
      this.hasSearched = false
      this.mainTable.list = []
      this.mainTable.total = 0
      this.total = 0
      this.mainTable.currentPage = 1
    },
    handleRowClick(row) {
      // 点击行不做特殊处理
    },
    showDetail(row) {
      this.detailDialog.visible = true
      this.detailDialog.name = row.EAUDITTRANSID
      this.detailDialog.loading = true
      this.detailData = {}
      this.disposeMonaco()

      getAutoScriptAuditDetail(row.EAUDITTRANSID).then(res => {
        if (res.code === 200 && res.data) {
          this.detailData = res.data
          const lang = res.data.SCRIPTLANGUAGE || 'javascript'
          const monacoLang = LANG_MAP[lang] || 'plaintext'
          this.$nextTick(() => {
            this.initMonaco(res.data.SOURCE || '', monacoLang)
          })
        } else {
          this.$message.error(res.message || '获取审计详情失败')
        }
      }).catch(err => {
        this.$message.error('获取审计详情失败: ' + (err.message || String(err)))
      }).finally(() => {
        this.detailDialog.loading = false
      })
    },
    onDetailClosed() {
      this.detailData = {}
      this.disposeMonaco()
    },
    initMonaco(code, language) {
      const container = this.$refs.monacoContainer
      if (!container) return
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
    auditTypeLabel(type) {
      return (AUDIT_TYPE_MAP[type] || {}).label || type || '-'
    },
    auditTypeTag(type) {
      return (AUDIT_TYPE_MAP[type] || {}).tag || 'info'
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
    },
    formatTime(val) {
      if (val === null || val === undefined || val === '') return ''
      const parsed = this.parseTimestampStr(val)
      if (parsed) return parsed
      return String(val)
    }
  },
  mounted() {
    if (this.mainTable) {
      this.mainTable.onPageChange = (page, limit) => {
        this.handlePageChange(page, limit)
      }
    }
    if (this.autoscript) {
      this.formData.autoscript = this.autoscript
      if (this.autoQuery) {
        this.handleQuery()
      }
    }
  },
  beforeDestroy() {
    this.disposeMonaco()
  }
}
</script>

<style lang="scss" scoped>
.audit-panel {
  &.is-embedded {
    .result-panel {
      margin-top: 12px;
    }
  }
}
.result-panel {
  margin-top: 20px;
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
.type-raw {
  font-family: monospace;
  font-size: 12px;
  color: #909399;
}
.detail-type-raw {
  margin-left: 6px;
}
.detail-form-value {
  word-break: break-all;
  white-space: normal;
  line-height: 1.6;
}
</style>
