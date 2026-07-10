<template>
  <section class="query-page">
    <el-card>
      <div class="page-header-row">
        <div>
          <h2>自动化脚本查询</h2>
          <p class="page-summary">支持脚本名称、描述、对象名、字段名、启动点名称查询，诊断模式可智能匹配相关脚本。</p>
        </div>
        <div class="mode-switch">
          <span class="mode-label">{{ isDiagMode ? '诊断模式' : '查询模式' }}</span>
          <el-switch v-model="isDiagMode" active-text="诊断" inactive-text="查询" />
        </div>
      </div>

      <el-form :model="formData" ref="queryForm" :inline="true" label-width="80px" @submit.native.prevent>
        <el-form-item label="脚本名称">
          <el-input v-model="formData.autoscript" placeholder="AUTOSCRIPT" clearable @keyup.enter.native="handleQuery" style="width:180px" />
        </el-form-item>
        <el-form-item label="脚本描述">
          <el-input v-model="formData.description" placeholder="DESCRIPTION" clearable @keyup.enter.native="handleQuery" style="width:180px" />
        </el-form-item>
        <el-form-item label="对象名">
          <el-input v-model="formData.objectname" placeholder="OBJECTNAME" clearable @keyup.enter.native="handleQuery" style="width:160px" />
        </el-form-item>
        <el-form-item label="字段名">
          <el-input v-model="formData.attributename" placeholder="ATTRIBUTENAME" clearable @keyup.enter.native="handleQuery" style="width:160px" />
        </el-form-item>
        <el-form-item label="启动点">
          <el-input v-model="formData.launchpointname" placeholder="LAUNCHPOINTNAME" clearable @keyup.enter.native="handleQuery" style="width:180px" />
        </el-form-item>
        <el-form-item label="脚本内容" v-if="!isDiagMode">
          <el-input v-model="formData.source" placeholder="SOURCE 模糊搜索" clearable @keyup.enter.native="handleQuery" style="width:200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="cyan" icon="el-icon-search" size="mini" :loading="loading" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" size="mini" @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="result-panel">
        <div class="result-toolbar">
          <el-button size="mini" :icon="allExpanded ? 'el-icon-arrow-up' : 'el-icon-arrow-down'" @click="toggleExpandAll">{{ allExpanded ? '全部收起' : '全部展开' }}</el-button>
        </div>
        <el-table ref="mainTable" :data="tableData" stripe style="width: 100%" class="result-table" v-loading="loading"
          row-key="AUTOSCRIPT" @expand-change="loadRowDetails">
          <el-table-column type="expand">
            <template slot-scope="scope">
              <div v-if="rowDetails[scope.row.AUTOSCRIPT]" class="expand-content">
                <!-- 都为空时显示在一行 -->
                <div v-if="(rowDetails[scope.row.AUTOSCRIPT].launchPoints || []).length === 0 && (rowDetails[scope.row.AUTOSCRIPT].vars || []).length === 0" class="empty-row">
                  <span>启动点: 空</span>
                  <span style="margin:0 16px;color:#dcdfe6">|</span>
                  <span>变量: 空</span>
                </div>
                <template v-else>
                  <!-- 启动点 -->
                  <template v-if="(rowDetails[scope.row.AUTOSCRIPT].launchPoints || []).length > 0">
                    <p class="section-title">启动点 ({{ rowDetails[scope.row.AUTOSCRIPT].launchPoints.length }})</p>
                    <div class="card-grid">
                      <el-card v-for="lp in rowDetails[scope.row.AUTOSCRIPT].launchPoints" :key="lp.LAUNCHPOINTNAME" shadow="hover" class="lp-card">
                        <div slot="header" class="card-header">
                          <span class="card-title">{{ lp.LAUNCHPOINTNAME }}</span>
                          <el-tag :type="lp.ACTIVE === 1 ? 'success' : 'info'" size="mini">{{ lp.ACTIVE === 1 ? '活动' : '非活动' }}</el-tag>
                        </div>
                        <div class="kv-grid">
                          <div class="kv-item"><span class="kv-label">类型</span><span>{{ lp.LAUNCHPOINTTYPE }}</span></div>
                          <div class="kv-item"><span class="kv-label">对象名</span><span>{{ lp.OBJECTNAME }}</span></div>
                          <div class="kv-item" v-if="lp.ATTRIBUTENAME"><span class="kv-label">属性名</span><span>{{ lp.ATTRIBUTENAME }}</span></div>
                          <div class="kv-item" v-if="lp.DESCRIPTION"><span class="kv-label">描述</span><span class="kv-val-overflow">{{ lp.DESCRIPTION }}</span></div>
                          <div class="kv-item kv-full"><span class="kv-label">事件</span><span>{{ parseObjectEvent(lp) }}</span></div>
                          <div class="kv-item" v-if="lp.CONDITION"><span class="kv-label">条件</span><span class="kv-val-overflow">{{ lp.CONDITION }}</span></div>
                        </div>
                        <div v-if="lp._VARS && lp._VARS.length > 0" class="lp-vars">
                          <p class="sub-title">启动点变量</p>
                          <div class="var-tag" v-for="v in lp._VARS" :key="v.VARNAME">
                            <el-tag size="mini" type="info">{{ v.VARNAME }}</el-tag>
                            <span class="var-val">{{ v.VARBINDINGVALUE }}</span>
                          </div>
                        </div>
                      </el-card>
                    </div>
                  </template>
                  <div v-else class="empty-single">启动点: 空</div>

                  <!-- 变量 -->
                  <template v-if="(rowDetails[scope.row.AUTOSCRIPT].vars || []).length > 0">
                    <p class="section-title">变量 ({{ rowDetails[scope.row.AUTOSCRIPT].vars.length }})</p>
                    <div class="card-grid">
                      <el-card v-for="v in rowDetails[scope.row.AUTOSCRIPT].vars" :key="v.VARNAME" shadow="hover" class="var-card">
                        <div slot="header" class="card-header">
                          <span class="card-title">{{ v.VARNAME }}</span>
                          <el-tag size="mini">{{ v.VARTYPE }}</el-tag>
                        </div>
                        <div class="kv-grid">
                          <div class="kv-item"><span class="kv-label">绑定类型</span><span>{{ v.VARBINDINGTYPE }}</span></div>
                          <div class="kv-item kv-full"><span class="kv-label">绑定值</span><span class="kv-val-overflow">{{ v.VARBINDINGVALUE }}</span></div>
                          <div class="kv-item" v-if="v.LITERALDATATYPE"><span class="kv-label">数据类型</span><span>{{ v.LITERALDATATYPE }}</span></div>
                          <div class="kv-item"><span class="kv-label">允许覆盖</span><span>{{ v.ALLOWOVERRIDE === 1 ? '是' : '否' }}</span></div>
                          <div class="kv-item" v-if="v.DESCRIPTION"><span class="kv-label">描述</span><span class="kv-val-overflow">{{ v.DESCRIPTION }}</span></div>
                        </div>
                      </el-card>
                    </div>
                  </template>
                  <div v-else class="empty-single">变量: 空</div>
                </template>
              </div>
              <div v-else style="text-align:center;padding:16px;color:#909399">加载中...</div>
            </template>
          </el-table-column>
          <el-table-column prop="AUTOSCRIPT" label="脚本名称" min-width="50">
            <template slot-scope="scope">
              <el-link type="primary" :underline="false" @click="showDetail(scope.row)">{{ scope.row.AUTOSCRIPT }}</el-link>
            </template>
          </el-table-column>
          <el-table-column prop="L_DESCRIPTION" label="中文描述" min-width="50" show-overflow-tooltip>
            <template slot-scope="scope">{{ scope.row.L_DESCRIPTION || scope.row.DESCRIPTION || '' }}</template>
          </el-table-column>
          <el-table-column prop="SCRIPTLANGUAGE" label="语言" width="100" />
          <el-table-column prop="VERSION" label="版本" width="80" />
          <el-table-column prop="CHANGEDATE" label="修改日期" width="160">
            <template slot-scope="scope">{{ parseTime(scope.row.CHANGEDATE) }}</template>
          </el-table-column>
          <el-table-column label="活动" width="60">
            <template slot-scope="scope">
              <el-tag :type="scope.row.ACTIVE === 1 ? 'success' : 'info'" size="mini">{{ scope.row.ACTIVE === 1 ? '是' : '否' }}</el-tag>
            </template>
          </el-table-column>
          <!-- <el-table-column label="用户定义" width="80">
            <template slot-scope="scope">
              <el-tag :type="scope.row.USERDEFINED === 1 ? 'warning' : 'info'" size="mini">{{ scope.row.USERDEFINED === 1 ? '是' : '否' }}</el-tag>
            </template>
          </el-table-column> -->
         <el-table-column prop="IBM_PACKAGEPATH" label="包路径" width="150" />
          <el-table-column prop="_matchType" label="匹配类型" width="180" v-if="isDiagMode">
            <template slot-scope="scope">
              <el-tag v-for="tag in (scope.row._matchType || '').split(', ')" :key="tag" size="mini" :type="matchTypeTag(tag)" style="margin-right:4px">{{ tag }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right">
            <template slot-scope="scope">
              <el-button type="text" size="small" @click="showDetail(scope.row)">详情</el-button>
              <el-button type="text" size="small" @click="showSource(scope.row)">源码</el-button>
            </template>
          </el-table-column>
        </el-table>

        <pagination v-show="tableParam.total > 0"
          :total="tableParam.total"
          :page.sync="tableParam.pageNum"
          :limit.sync="tableParam.pageSize"
          @pagination="handlePageChange"
        />
        <el-empty v-if="!loading && tableData.length === 0 && hasSearched" description="暂无查询结果" />
      </div>
    </el-card>

    <!-- 源码弹窗 -->
    <el-dialog :visible.sync="sourceDialog.visible" :width="sourceDialog.fullscreen ? '100%' : '75%'" :top="sourceDialog.fullscreen ? '0' : '5vh'" :close-on-click-modal="false" :fullscreen="sourceDialog.fullscreen" custom-class="source-dialog">
      <template #title>
        <div class="source-dialog-title">
          <span>源码: {{ sourceDialog.name }}</span>
          <el-button size="mini" :icon="sourceDialog.fullscreen ? 'el-icon-close' : 'el-icon-full-screen'" @click="toggleSourceFullscreen">{{ sourceDialog.fullscreen ? '退出全屏' : '全屏' }}</el-button>
        </div>
      </template>
      <div v-if="sourceDialog.loading" style="text-align:center;padding:40px">加载中...</div>
      <div v-else ref="monacoContainer" :style="{ height: sourceDialog.fullscreen ? 'calc(100vh - 100px)' : '70vh', border: '1px solid #dcdfe6' }"></div>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog :title="'详情: ' + detailDialog.name" :visible.sync="detailDialog.visible" width="85%" top="2vh" :close-on-click-modal="false">
      <div v-loading="detailDialog.loading" style="max-height:80vh;overflow-y:auto">
        <!-- 脚本基本信息 -->
        <div v-if="Object.keys(detailDialog.mainInfo).length > 0">
          <p class="section-title">脚本信息</p>
          <el-form label-width="140px" size="small">
            <el-row :gutter="16">
              <el-col :span="8" v-for="(val, key) in detailDialog.mainInfo" :key="key">
                <el-form-item :label="getFieldLabel(AUTOSCRIPT_LABELS, key)">
                  <el-input :value="formatVal(val)" readonly size="small" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </div>
        <el-empty v-else-if="!detailDialog.loading" description="暂无脚本信息" />

        <!-- 启动点 -->
        <div style="margin-top:16px">
          <p class="section-title">启动点 ({{ detailDialog.launchPoints.length }})</p>
          <div v-if="detailDialog.launchPoints.length > 0" class="card-grid">
            <el-card v-for="lp in detailDialog.launchPoints" :key="lp.LAUNCHPOINTNAME" shadow="hover" class="lp-card">
              <div slot="header" class="card-header">
                <span class="card-title">{{ lp.LAUNCHPOINTNAME }}</span>
                <el-tag :type="lp.ACTIVE === 1 ? 'success' : 'info'" size="mini">{{ lp.ACTIVE === 1 ? '活动' : '非活动' }}</el-tag>
              </div>
              <el-form label-width="120px" size="mini" class="detail-form">
                <el-form-item v-for="(val, key) in lp" :key="key" :label="getFieldLabel(LP_LABELS, key)" v-if="!String(key).startsWith('_')">
                  <span v-if="key === 'OBJECTEVENT'">{{ parseObjectEvent(lp) }}</span>
                  <span v-else>{{ formatVal(val) }}</span>
                </el-form-item>
              </el-form>
              <div v-if="lp._VARS && lp._VARS.length > 0" class="lp-vars">
                <p class="sub-title">启动点变量 ({{ lp._VARS.length }})</p>
                <el-card v-for="v in lp._VARS" :key="v.VARNAME" shadow="never" class="nested-var-card">
                  <el-form label-width="100px" size="mini" class="detail-form">
                    <el-form-item v-for="(val, key) in v" :key="key" :label="getFieldLabel(LPVAR_LABELS, key)">
                      <span>{{ formatVal(val) }}</span>
                    </el-form-item>
                  </el-form>
                </el-card>
              </div>
              <div v-else-if="!lp._VARS || lp._VARS.length === 0" class="empty-single">启动点变量: 空</div>
            </el-card>
          </div>
          <div v-else class="empty-single">启动点: 空</div>
        </div>

        <!-- 变量 -->
        <div style="margin-top:16px">
          <p class="section-title">变量 ({{ detailDialog.vars.length }})</p>
          <div v-if="detailDialog.vars.length > 0" class="card-grid">
            <el-card v-for="v in detailDialog.vars" :key="v.VARNAME" shadow="hover" class="var-card">
              <div slot="header" class="card-header">
                <span class="card-title">{{ v.VARNAME }}</span>
                <el-tag size="mini">{{ v.VARTYPE }}</el-tag>
              </div>
              <el-form label-width="120px" size="mini" class="detail-form">
                <el-form-item v-for="(val, key) in v" :key="key" :label="getFieldLabel(VAR_LABELS, key)">
                  <span v-if="key === 'ALLOWOVERRIDE'">{{ val === 1 ? '是' : '否' }}</span>
                  <span v-else>{{ formatVal(val) }}</span>
                </el-form-item>
              </el-form>
            </el-card>
          </div>
          <div v-else class="empty-single">变量: 空</div>
        </div>
      </div>
    </el-dialog>
  </section>
</template>

<script>
import { getAutoScriptList, getAutoScriptDetail, getAutoScriptSource } from '@/api/autoscript'
import { parseTime } from '@/utils/ruoyi'

// OBJECTEVENT 位标志 - OBJECT 类型
const OBJECT_EVENT_BITS = [
  { bit: 1,    name: 'onMboInit',               label: '初始化值（Mbo初始化）' },
  { bit: 2,    name: 'onMboAdd',                label: '新增（保存前）' },
  { bit: 4,    name: 'onMboUpdate',             label: '修改（保存前）' },
  { bit: 8,    name: 'onMboDelete',             label: '删除（保存前）' },
  { bit: 16,   name: 'onMboAddPostSave',        label: '新增（保存后）' },
  { bit: 32,   name: 'onMboUpdatePostSave',     label: '修改（保存后）' },
  { bit: 64,   name: 'onMboDeletePostSave',     label: '删除（保存后）' },
  { bit: 128,  name: 'onMboAddPostCommit',      label: '新增（落实后）' },
  { bit: 256,  name: 'onMboUpdatePostCommit',   label: '修改（落实后）' },
  { bit: 512,  name: 'onMboDeletePostCommit',   label: '删除（落实后）' },
  { bit: 1024, name: 'onMboAppValidate',        label: '验证应用程序' },
  { bit: 2048, name: 'onMboCanAdd',             label: '允许创建对象' },
  { bit: 4096, name: 'onMboCanDelete',          label: '允许删除对象' }
]

// OBJECTEVENT 位标志 - ATTRIBUTE 类型
const ATTR_EVENT_BITS = [
  { bit: 0,  name: 'onMVAValidate', label: '验证',          exact: true },
  { bit: 1,  name: 'onMVAAction',   label: '运行操作' },
  { bit: 2,  name: 'onMVAInit',     label: '初始化值' },
  { bit: 8,  name: 'onMVAInitAR',   label: '初始化访问限制' },
  { bit: 64, name: 'onMTDList',     label: '检索列表' }
]

// SCRIPTLANGUAGE 到 Monaco 语言映射
const LANG_MAP = {
  'javascript': 'javascript', 'js': 'javascript', 'JS': 'javascript',
  'JavaScript': 'javascript', 'nashorn': 'javascript', 'Nashorn': 'javascript',
  'ecmascript': 'javascript', 'ECMAScript': 'javascript',
  'python': 'python', 'jython': 'python', 'py': 'python', 'MBR': 'python'
}

// DB2 中文标签映射 - AUTOSCRIPT
const AUTOSCRIPT_LABELS = {
  AUTOSCRIPT: '脚本', STATUS: '状态', SCHEDULEDSTATUS: '已调度状态', COMMENTS: '注释',
  OWNERID: '所有者', OWNERNAME: '姓名', OWNERPHONE: '电话', OWNEREMAIL: '电子邮件',
  CREATEDBYID: '创建者', DESCRIPTION: '描述', ORGID: '组织', SITEID: '地点',
  ACTION: '操作', CREATEDDATE: '创建日期', VERSION: '版本', CATEGORY: '类型',
  STATUSDATE: '状态日期', CHANGEDATE: '变更日期', OWNER: '所有者人员',
  CREATEDBY: '创建人', CHANGEBY: '变更人', AUTOSCRIPTID: '唯一标识',
  HASLD: '具有详细描述', LANGCODE: '语言代码', SCRIPTLANGUAGE: '脚本语言',
  USERDEFINED: '用户定义', LOGLEVEL: '日志级别', INTERFACE: '是接口',
  ACTIVE: '活动', IBM_PACKAGEPATH: '包路径', CREATEDBYPHONE: '创建者电话',
  CREATEDBYNAME: '创建者姓名', CREATEDBYEMAIL: '创建者邮箱', L_DESCRIPTION: '中文描述'
}
// DB2 中文标签映射 - SCRIPTLAUNCHPOINT
const LP_LABELS = {
  LAUNCHPOINTNAME: '启动点', AUTOSCRIPT: '脚本', DESCRIPTION: '描述',
  LAUNCHPOINTTYPE: '启动点类型', OBJECTNAME: '对象', OBJECTEVENT: '事件',
  ATTRIBUTENAME: '属性', CONDITION: '对象事件条件', ACTIVE: '活动',
  SCRIPTLAUNCHPOINTID: '唯一标识', L_DESCRIPTION: '中文描述'
}
// DB2 中文标签映射 - AUTOSCRIPTVARS
const VAR_LABELS = {
  VARNAME: '变量', AUTOSCRIPT: '脚本', VARBINDINGVALUE: '绑定值',
  VARBINDINGTYPE: '绑定类型', VARTYPE: '变量类型', DESCRIPTION: '描述',
  ALLOWOVERRIDE: '覆盖', LITERALDATATYPE: '字面值数据类型', ACCESSFLAG: '属性标志',
  AUTOSCRIPTVARSID: '脚本变量标识', L_DESCRIPTION: '中文描述'
}
// DB2 中文标签映射 - LAUNCHPOINTVARS
const LPVAR_LABELS = {
  LAUNCHPOINTNAME: '启动点', AUTOSCRIPT: '脚本', VARNAME: '变量',
  VARBINDINGVALUE: '绑定值', LAUNCHPOINTVARSID: '唯一标识'
}

export default {
  name: 'AutoScriptQuery',
  data() {
    return {
      isDiagMode: localStorage.getItem('autoscript-isDiagMode') !== 'false',
      AUTOSCRIPT_LABELS,
      LP_LABELS,
      VAR_LABELS,
      LPVAR_LABELS,
      loading: false,
      hasSearched: false,
      tableData: [],
      tableParam: { total: 0, pageNum: 1, pageSize: 20 },
      formData: {
        autoscript: '',
        description: '',
        objectname: '',
        attributename: '',
        launchpointname: '',
        source: ''
      },
      sourceDialog: { visible: false, name: '', loading: false, editor: null, fullscreen: false },
      detailDialog: {
        visible: false, name: '', loading: false,
        mainInfo: {}, launchPoints: [], vars: []
      },
      // 诊断模式去重
      diagResultMap: {},
      allDiagData: [],
      rowDetails: {},
      allExpanded: false
    }
  },
  computed: {
    slicedDiagData() {
      if (this.allDiagData.length === 0) return []
      const start = (this.tableParam.pageNum - 1) * this.tableParam.pageSize
      return this.allDiagData.slice(start, start + this.tableParam.pageSize)
    }
  },
  watch: {
    isDiagMode(val) {
      localStorage.setItem('autoscript-isDiagMode', val)
    }
  },
  methods: {
    handleQuery() {
      this.tableParam.pageNum = 1
      if (this.isDiagMode) {
        // 诊断模式：不清空已有结果，追加新查询结果（去重）
      } else {
        this.tableData = []
        this.diagResultMap = {}
      }
      this.getList()
    },
    getList() {
      this.loading = true
      this.hasSearched = true
      const params = {
        autoscript: this.formData.autoscript || undefined,
        description: this.formData.description || undefined,
        objectname: this.formData.objectname || undefined,
        attributename: this.formData.attributename || undefined,
        launchpointname: this.formData.launchpointname || undefined,
        source: this.isDiagMode ? undefined : (this.formData.source || undefined),
        mode: this.isDiagMode ? 'diag' : 'query',
        pageNum: 1,
        pageSize: this.isDiagMode ? 9999 : this.tableParam.pageSize
      }

      getAutoScriptList(params).then(res => {
        console.log('[AutoScriptQuery] 接口响应:', res)
        if (res.code === 200 && res.data) {
          const rows = res.data.rows || []
          const total = res.data.total || 0
          console.log('[AutoScriptQuery] rows:', rows.length, 'total:', total)

          if (this.isDiagMode) {
            // 诊断模式：合并去重
            for (const row of rows) {
              const key = row.AUTOSCRIPT
              if (!this.diagResultMap[key]) {
                this.$set(this.diagResultMap, key, row)
              } else {
                // 合并匹配类型
                const existTypes = (this.diagResultMap[key]._matchType || '').split(', ').filter(Boolean)
                const newTypes = (row._matchType || '').split(', ').filter(Boolean)
                const merged = [...new Set([...existTypes, ...newTypes])]
                this.$set(this.diagResultMap, key, { ...this.diagResultMap[key], _matchType: merged.join(', ') })
              }
            }
            this.allDiagData = Object.values(this.diagResultMap)
            this.tableParam.total = this.allDiagData.length
            this.tableData = this.slicedDiagData
            console.log('[AutoScriptQuery] 诊断模式: tableData.length=', this.tableData.length)
          } else {
            this.tableData = rows
            this.tableParam.total = total
            console.log('[AutoScriptQuery] 查询模式: tableData.length=', this.tableData.length)
          }
        } else {
          this.$message.error(res.message || '查询失败')
        }
      }).catch(err => {
        console.error('[AutoScriptQuery] 查询失败:', err)
        this.$message.error('查询失败: ' + (err.message || String(err)))
      }).finally(() => {
        this.loading = false
      })
    },
    handlePageChange() {
      if (this.isDiagMode) {
        this.tableData = this.slicedDiagData
      } else {
        this.getList()
      }
    },
    resetForm() {
      this.formData = {
        autoscript: '', description: '', objectname: '',
        attributename: '', launchpointname: '', source: ''
      }
      this.tableData = []
      this.tableParam = { total: 0, pageNum: 1, pageSize: 20 }
      this.diagResultMap = {}
      this.allDiagData = []
      this.rowDetails = {}
      this.hasSearched = false
    },
    toggleExpandAll() {
      if (!this.$refs.mainTable) return
      const table = this.$refs.mainTable
      const shouldExpand = !this.allExpanded
      // 先展开所有行，触发 loadRowDetails
      this.tableData.forEach(row => {
        table.toggleRowExpansion(row, shouldExpand)
      })
      this.allExpanded = shouldExpand
      // 展开时批量加载详情
      if (shouldExpand) {
        this.tableData.forEach(row => {
          this.loadRowDetails(row, this.tableData)
        })
      }
    },
    loadRowDetails(row, expandedRows) {
      const name = row.AUTOSCRIPT
      if (this.rowDetails[name] || !expandedRows || !expandedRows.includes(row)) return
      this.$set(this.rowDetails, name, { loading: true })
      getAutoScriptDetail(name).then(res => {
        if (res.code === 200 && res.data) {
          this.$set(this.rowDetails, name, {
            launchPoints: res.data.launchPoints || [],
            vars: res.data.vars || []
          })
        }
      }).catch(() => {
        this.$set(this.rowDetails, name, { launchPoints: [], vars: [] })
      })
    },
    showSource(row) {
      this.sourceDialog.visible = true
      this.sourceDialog.name = row.AUTOSCRIPT
      this.sourceDialog.loading = true
      this.$nextTick(() => {
        if (this.sourceDialog.editor) {
          this.sourceDialog.editor.dispose()
          this.sourceDialog.editor = null
        }
      })
      getAutoScriptSource(row.AUTOSCRIPT).then(res => {
        this.sourceDialog.loading = false
        if (res.code === 200 && res.data) {
          const lang = res.data.SCRIPTLANGUAGE || 'javascript'
          const monacoLang = LANG_MAP[lang] || 'plaintext'
          this.$nextTick(() => {
            this.initMonaco(res.data.SOURCE || '', monacoLang)
          })
        } else {
          this.$message.error('获取源码失败')
        }
      }).catch(err => {
        this.sourceDialog.loading = false
        this.$message.error('获取源码失败: ' + (err.message || String(err)))
      })
    },
    initMonaco(code, language) {
      if (!this.$refs.monacoContainer) return
      // 动态加载 monaco
      import(/* webpackChunkName: "monaco" */ 'monaco-editor').then(monaco => {
        if (this.sourceDialog.editor) {
          this.sourceDialog.editor.dispose()
        }
        this.sourceDialog.editor = monaco.editor.create(this.$refs.monacoContainer, {
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
        // 回退到 textarea
        this.$refs.monacoContainer.innerHTML = '<textarea readonly style="width:100%;height:100%;font-family:monospace">' + this.escapeHtml(code) + '</textarea>'
      })
    },
    escapeHtml(str) {
      return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    },
    toggleSourceFullscreen() {
      this.sourceDialog.fullscreen = !this.sourceDialog.fullscreen
      // 触发 Monaco Editor 重新布局
      if (this.sourceDialog.editor) {
        setTimeout(() => {
          this.sourceDialog.editor.layout()
        }, 100)
      }
    },
    showDetail(row) {
      this.detailDialog.visible = true
      this.detailDialog.name = row.AUTOSCRIPT
      this.detailDialog.loading = true
      this.detailDialog.mainInfo = {}
      this.detailDialog.launchPoints = []
      this.detailDialog.vars = []

      getAutoScriptDetail(row.AUTOSCRIPT).then(res => {
        if (res.code === 200 && res.data) {
          this.detailDialog.mainInfo = res.data.mainInfo || {}
          this.detailDialog.launchPoints = res.data.launchPoints || []
          this.detailDialog.vars = res.data.vars || []
        } else {
          this.$message.error(res.message || '获取详情失败')
        }
      }).catch(err => {
        this.$message.error('获取详情失败: ' + (err.message || String(err)))
      }).finally(() => {
        this.detailDialog.loading = false
      })
    },
    getFieldLabel(labelMap, fieldName) {
      return labelMap[fieldName] || fieldName
    },
    formatVal(val) {
      if (val === null || val === undefined) return ''
      if (typeof val === 'number' && val > 1000000000000 && val < 10000000000000) {
        return parseTime(val)
      }
      return String(val)
    },
    matchTypeTag(tag) {
      if (tag === '对象匹配') return 'success'
      if (tag === '脚本名匹配') return 'warning'
      if (tag === '源码包含') return 'danger'
      return 'info'
    },
    parseObjectEvent(row) {
      const lpType = (row.LAUNCHPOINTTYPE || '').toUpperCase()
      const event = row.OBJECTEVENT
      if (event === null || event === undefined) return ''

      if (lpType === 'OBJECT') {
        return this.parseObjectEventBits(event, OBJECT_EVENT_BITS)
      } else if (lpType === 'ATTRIBUTE') {
        return this.parseAttrEventBits(event, ATTR_EVENT_BITS)
      }
      return 'OBJECTEVENT=' + event
    },
    parseObjectEventShort(row) {
      const full = this.parseObjectEvent(row)
      if (!full) return ''
      // 只显示事件标签，截断
      const labels = full.split(', ')
      if (labels.length <= 3) return full
      return labels.slice(0, 3).join(', ') + '...'
    },
    parseObjectEventBits(event, bits) {
      const labels = []
      for (const b of bits) {
        if ((event & b.bit) === b.bit) {
          labels.push(b.label)
        }
      }
      return labels.length > 0 ? labels.join(', ') : '无事件'
    },
    parseAttrEventBits(event, bits) {
      // ATTRIBUTE 类型：event=0 是验证（精确匹配）
      if (event === 0) return '验证'
      const labels = []
      for (const b of bits) {
        if (b.exact) continue
        if ((event & b.bit) === b.bit) {
          labels.push(b.label)
        }
      }
      return labels.length > 0 ? labels.join(', ') : '无事件'
    }
  },
  beforeDestroy() {
    if (this.sourceDialog.editor) {
      this.sourceDialog.editor.dispose()
      this.sourceDialog.editor = null
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
.mode-switch {
  display: flex;
  align-items: center;
  gap: 10px;
  .mode-label {
    font-size: 14px;
    color: #606266;
  }
}
.result-panel {
  margin-top: 20px;
}
.result-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 8px;
}
.result-table {
  margin-top: 0;
}
.section-title {
  font-weight: 600;
  font-size: 13px;
  color: #303133;
  margin: 0 0 8px 0;
}
.sub-title {
  font-weight: 600;
  font-size: 12px;
  color: #606266;
  margin: 10px 0 6px 0;
}
.empty-row {
  color: #909399;
  font-size: 13px;
  text-align: center;
  padding: 12px 0;
}
.empty-single {
  color: #909399;
  font-size: 12px;
  padding: 4px 0 8px 0;
}
.expand-content {
  padding: 12px 24px;
}
.card-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 12px;
}
.lp-card, .var-card {
  flex: 1 1 300px;
  min-width: 280px;
  max-width: 100%;
}
::v-deep .lp-card .el-card__header,
::v-deep .var-card .el-card__header {
  padding: 10px 14px;
  background: #f5f7fa;
}
::v-deep .lp-card .el-card__body,
::v-deep .var-card .el-card__body {
  padding: 10px 14px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.card-title {
  font-weight: 600;
  font-size: 13px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.kv-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px 12px;
  font-size: 12px;
}
.kv-item {
  display: flex;
  gap: 6px;
  line-height: 1.8;
}
.kv-full {
  grid-column: 1 / -1;
}
.kv-label {
  color: #909399;
  white-space: nowrap;
  min-width: 48px;
  flex-shrink: 0;
}
.kv-label::after {
  content: ':';
}
.kv-val-overflow {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 300px;
}
.lp-vars {
  border-top: 1px solid #ebeef5;
  margin-top: 8px;
  padding-top: 6px;
}
.var-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin: 2px 6px 2px 0;
  font-size: 12px;
}
.var-val {
  color: #606266;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.sub-table {
  margin: 8px 0;
}
.detail-form {
  .el-form-item {
    margin-bottom: 4px;
  }
}
.nested-var-card {
  margin-bottom: 6px;
  ::v-deep .el-card__body {
    padding: 8px 12px;
  }
}
.source-dialog-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-right: 30px;
}
</style>
