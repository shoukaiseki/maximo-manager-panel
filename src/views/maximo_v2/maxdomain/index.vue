<template>
  <section class="query-page">
    <el-card>
      <div class="page-header-row">
        <div>
          <h2>域查询(新模式)</h2>
          <p class="page-summary">支持域ID / 描述 / 最大类型模糊查询，点击行查看域定义详情，可导出 / 导入域定义。</p>
        </div>
        <div class="header-actions">
          <el-button type="primary" icon="el-icon-upload2" size="mini" @click="openImportDialog">导入</el-button>
          <el-button type="primary" icon="el-icon-download" size="mini" @click="openExportDialog">导出</el-button>
        </div>
      </div>
      <el-form ref="queryForm" :model="formData" :inline="true" label-width="72px" @submit.native.prevent>
        <el-form-item label="域ID">
          <el-input v-model="formData.domainid" placeholder="模糊搜索域ID" clearable style="width: 200px;" @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="formData.description" placeholder="模糊搜索描述" clearable style="width: 200px;" @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item label="域类型">
          <el-select v-model="formData.domaintype" placeholder="全部" clearable style="width: 160px;" @change="handleQuery">
            <el-option label="全部" value="" />
            <el-option label="ALN" value="ALN" />
            <el-option label="SYNONYM" value="SYNONYM" />
            <el-option label="NUMERIC" value="NUMERIC" />
            <el-option label="NUMRANGE" value="NUMRANGE" />
            <el-option label="TABLE" value="TABLE" />
            <el-option label="CROSSOVER" value="CROSSOVER" />
          </el-select>
        </el-form-item>
        <el-form-item label="最大类型">
          <el-input v-model="formData.maxtype" placeholder="模糊搜索最大类型" clearable style="width: 180px;" @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item>
          <el-button type="cyan" icon="el-icon-search" size="mini" :loading="loading" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" size="mini" @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="result-panel">
        <el-table
          v-loading="loading"
          :data="domains"
          stripe
          class="result-table"
          highlight-current-row
          @selection-change="handleSelectionChange"
          @row-click="handleRowClick"
        >
          <el-table-column type="selection" width="46" />
          <el-table-column prop="domainid" label="域ID" min-width="200" show-overflow-tooltip />
          <el-table-column prop="domaintype" label="域类型" width="120">
            <template slot-scope="scope">
              <el-tag :type="domainTypeTagType(scope.row.domaintype)" size="mini">{{ scope.row.domaintype || '-' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="描述" min-width="220" show-overflow-tooltip>
            <template slot-scope="scope">
              <span :class="{'ml-empty': isEmptyCell(scope.row.description)}">{{ scope.row.description || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="maxtype" label="最大类型" width="110" show-overflow-tooltip />
          <el-table-column prop="length" label="长度" width="80" />
          <el-table-column prop="scale" label="小数位" width="80" />
          <el-table-column prop="internal" label="内部" width="80">
            <template slot-scope="scope">
              <el-tag :type="isTruthy(scope.row.internal) ? 'success' : 'info'" size="mini">{{ boolText(scope.row.internal) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="nevercache" label="不缓存" width="90">
            <template slot-scope="scope">
              <el-tag :type="isTruthy(scope.row.nevercache) ? 'warning' : 'info'" size="mini">{{ boolText(scope.row.nevercache) }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!loading && !domains.length" :description="hasSearched ? '暂无查询结果' : '请输入查询条件后点击搜索'" />
        <pagination
          v-show="tableParam.total > 0"
          :total="tableParam.total"
          :page.sync="tableParam.pageNum"
          :limit.sync="tableParam.pageSize"
          @pagination="getList"
        />
      </div>
    </el-card>

    <!-- 域详情弹窗 -->
    <el-dialog :title="detailTitle" :visible.sync="dialogVisible" width="1300px" top="5vh" :close-on-click-modal="true">
      <div v-loading="detailLoading" element-loading-text="加载中..." class="detail-body">
        <template v-if="detailRow">
          <el-descriptions :column="4" border size="small">
            <el-descriptions-item v-for="field in mainFields" :key="field.key" :label="field.label">
              <el-input :value="formatMainValue(field.key)" readonly size="small" />
            </el-descriptions-item>
          </el-descriptions>
          <el-tabs v-model="activeChildTab" type="border-card" class="json-tabs">
            <el-tab-pane
              v-for="tab in childTabs"
              :key="tab.key"
              :label="tab.label + '(' + tab.rows.length + ')'"
              :name="tab.key"
              lazy
            >
              <el-table v-if="tab.rows.length" :data="tab.rows" border stripe size="mini" max-height="420" style="width: 100%;">
                <el-table-column v-if="tab.key === 'tabledomain'" type="expand" width="40">
                  <template slot-scope="scope">
                    <div class="crossover-panel">
                      <div class="crossover-title">交叉域</div>
                      <el-table v-if="crossoverRows(scope.row).length" :data="crossoverRows(scope.row)" border size="mini" style="width: 100%;">
                        <el-table-column
                          v-for="col in crossoverColumns"
                          :key="col.prop"
                          :prop="col.prop"
                          :label="col.label"
                          :min-width="col.width"
                          show-overflow-tooltip
                        >
                          <template slot-scope="inner">
                            <span :class="{'ml-empty': isEmptyCell(inner.row[col.prop])}">{{ cellText(col, inner.row) }}</span>
                          </template>
                        </el-table-column>
                      </el-table>
                      <span v-else class="ml-empty">无交叉域数据</span>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column
                  v-for="col in tab.columns"
                  :key="col.prop"
                  :prop="col.prop"
                  :label="col.label"
                  :min-width="col.width"
                  show-overflow-tooltip
                >
                  <template slot-scope="scope">
                    <span :class="{'ml-empty': isEmptyCell(scope.row[col.prop])}">{{ cellText(col, scope.row) }}</span>
                  </template>
                </el-table-column>
              </el-table>
              <el-empty v-else description="暂无子表数据" />
            </el-tab-pane>
            <el-tab-pane label="域精简JSON" name="jsonSimple" lazy>
              <div class="json-toolbar detail-json-toolbar">
                <el-checkbox v-model="detailJsonOptions.simple._allLangcode">显示所有语言</el-checkbox>
                <el-checkbox v-model="detailJsonOptions.simple.withUuid">生成UUID</el-checkbox>
                <el-button size="mini" icon="el-icon-refresh" :loading="detailSimpleLoading" @click="loadDetailJson('simple')">刷 新</el-button>
                <el-button type="primary" size="mini" icon="el-icon-document-copy" :disabled="!detailSimpleJson" @click="copyDetailJson('simple')">复制域精简JSON</el-button>
              </div>
              <monaco-json-editor v-if="detailSimpleJson" :value="detailSimpleJson" read-only height="400px" />
              <el-empty v-else description="点击「刷新」获取域精简 JSON" />
            </el-tab-pane>
            <el-tab-pane label="域完整JSON" name="jsonFull" lazy>
              <div class="json-toolbar detail-json-toolbar">
                <el-checkbox v-model="detailJsonOptions.full._allLangcode">显示所有语言</el-checkbox>
                <el-checkbox v-model="detailJsonOptions.full.withUuid">生成UUID</el-checkbox>
                <el-button size="mini" icon="el-icon-refresh" :loading="detailFullLoading" @click="loadDetailJson('full')">刷 新</el-button>
                <el-button type="primary" size="mini" icon="el-icon-document" :disabled="!detailFullJson" @click="copyDetailJson('full')">复制域完整JSON</el-button>
              </div>
              <monaco-json-editor v-if="detailFullJson" :value="detailFullJson" read-only height="400px" />
              <el-empty v-else description="点击「刷新」获取域完整 JSON" />
            </el-tab-pane>
          </el-tabs>
        </template>
        <el-empty v-else-if="!detailLoading" description="未获取到域详情" />
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">关 闭</el-button>
      </span>
    </el-dialog>

    <!-- 导出域定义弹窗 -->
    <el-dialog title="导出域定义" :visible.sync="exportDialogVisible" width="1000px" top="5vh" :close-on-click-modal="true">
      <div class="export-options">
        <el-checkbox v-model="exportOptions._allLangcode">显示所有语言(sks:langdata)</el-checkbox>
        <el-checkbox v-model="exportOptions.withUuid">生成UUID</el-checkbox>
        <el-checkbox v-model="exportOptions.ignoreDefVal">精简默认值(ignoreDefVal)</el-checkbox>
        <el-button size="mini" icon="el-icon-refresh" :loading="exportLoading" @click="generateExport">刷 新</el-button>
        <span class="export-count">{{ exportScopeText }}</span>
      </div>
      <div class="json-toolbar">
        <el-button type="primary" size="mini" icon="el-icon-document-copy" :disabled="!exportJson" @click="copyExportJson">复制 JSON</el-button>
        <el-button type="primary" size="mini" icon="el-icon-download" :disabled="!exportJson" @click="downloadExportJson">下载 JSON</el-button>
      </div>
      <monaco-json-editor v-if="exportJson" :value="exportJson" read-only height="45vh" />
      <el-empty v-else description="设置选项后点击「刷新」获取导出 JSON" />
      <span slot="footer" class="dialog-footer">
        <el-button @click="exportDialogVisible = false">关 闭</el-button>
      </span>
    </el-dialog>

    <!-- 导入域定义弹窗 -->
    <el-dialog title="导入域定义" :visible.sync="importDialog.visible" width="85%" top="4vh" custom-class="import-dialog" :close-on-click-modal="false">
      <el-alert type="info" :closable="false" show-icon title="粘贴导出的域 JSON 或选择 .json 文件（支持 {domainList:[...]} / 数组 / 单个域对象）；导入以编辑框内容为准" />
      <el-upload
        ref="importUpload"
        class="import-upload"
        :file-list="importDialog.fileList"
        :auto-upload="false"
        :limit="1"
        accept=".json"
        :on-change="onImportFileChange"
        :on-remove="onImportFileRemove"
      >
        <el-button size="small" icon="el-icon-folder-opened">选择 JSON 文件</el-button>
      </el-upload>
      <el-form label-width="120px" class="import-options">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="_allLangcode">
              <el-checkbox v-model="importDialog.allLangcode">导入多语言数据(sks:langdata)</el-checkbox>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="dmDeploySet">
              <el-checkbox v-model="importDialog.dmDeploySet">启用迁移模式</el-checkbox>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="_errorAssert">
              <el-checkbox v-model="importDialog.errorAssert">遇到错误时中断导入</el-checkbox>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="_deleteNotExists">
              <el-select v-model="importDialog.deleteNotExistsList" multiple size="small" placeholder="不删除" style="width: 100%;">
                <el-option v-for="opt in deleteNotExistsOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <monaco-json-editor v-model="importDialog.text" height="260px" placeholder="在此粘贴域定义 JSON（{domainList:[...]}）" />
      <div v-if="importDialog.summary" class="import-summary">
        <el-alert :title="importDialog.summary" :type="importDialog.resultSuccess ? 'success' : 'warning'" :closable="false" show-icon />
        <div class="import-stats">
          <el-tag size="mini" type="info">total: {{ importStat('total') }}</el-tag>
          <el-tag size="mini" type="success">added: {{ importStat('added') }}</el-tag>
          <el-tag size="mini" type="success">updated: {{ importStat('updated') }}</el-tag>
          <el-tag size="mini" type="warning">deleted: {{ importStat('deleted') }}</el-tag>
          <el-tag size="mini" type="danger">failed: {{ importStat('failed') }}</el-tag>
          <el-tag size="mini" type="success">成功: {{ importStat('sks:successSumTotal') }}</el-tag>
          <el-tag size="mini" type="danger">失败: {{ importStat('sks:failSumTotal') }}</el-tag>
          <el-tag size="mini" type="info">忽略: {{ importStat('sks:ignoreSumTotal') }}</el-tag>
        </div>
      </div>
      <el-table
        v-if="importResultRows.length"
        :data="importResultRows"
        border
        stripe
        size="mini"
        max-height="240"
        class="import-result-table"
        :row-class-name="importRowClass"
      >
        <el-table-column prop="objectName" label="对象" min-width="160" show-overflow-tooltip />
        <el-table-column prop="ref" label="引用" min-width="200" show-overflow-tooltip />
        <el-table-column prop="operation" label="操作" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template slot-scope="scope">
            <el-tag :type="statusTagType(scope.row.status)" size="mini">{{ scope.row.status || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="message" label="消息" min-width="260" show-overflow-tooltip />
      </el-table>
      <el-empty v-else-if="importDialog.resultData" description="接口未返回明细数据" />
      <span slot="footer" class="dialog-footer">
        <el-button @click="importDialog.visible = false">关 闭</el-button>
        <el-button type="primary" :loading="importDialog.loading" @click="doImport">导 入</el-button>
      </span>
    </el-dialog>
  </section>
</template>

<script>
import { queryDomain, migrateDomain } from '../../../api/domain_v2'
import { getDefaultLangcode, loadLanguageConfig } from '../../../utils/mmp/mmp-config'
import { handleApiError } from '../../../utils/mmp/api-error'
import MonacoJsonEditor from '../../../components/MonacoJsonEditor.vue'
import { generateUUID } from '../../../utils/sks'

export default {
  name: 'DomainV2Query',
  components: { MonacoJsonEditor },
  data() {
    return {
      domains: [],
      loading: false,
      hasSearched: false,
      selectedRows: [],
      tableParam: {
        total: 0,
        pageNum: 1,
        pageSize: 20
      },
      formData: {
        domainid: '',
        description: '',
        domaintype: '',
        maxtype: ''
      },
      mainFields: [
        { key: 'domainid', label: '域ID' },
        { key: 'domaintype', label: '域类型' },
        { key: 'description', label: '描述' },
        { key: 'maxtype', label: '最大类型' },
        { key: 'length', label: '长度' },
        { key: 'scale', label: '小数位' },
        { key: 'internal', label: '内部' },
        { key: 'nevercache', label: '永不缓存' }
      ],
      boolMainKeys: ['internal', 'nevercache'],
      childConfigs: [
        {
          key: 'alndomain',
          label: 'ALN值',
          columns: [
            { prop: 'value', label: '值', width: 160 },
            { prop: 'description', label: '描述', width: 220 },
            { prop: 'orgid', label: '组织', width: 120 },
            { prop: 'siteid', label: '站点', width: 120 }
          ]
        },
        {
          key: 'synonymdomain',
          label: '同义词',
          columns: [
            { prop: 'maxvalue', label: '最大值', width: 160 },
            { prop: 'value', label: '值', width: 160 },
            { prop: 'description', label: '描述', width: 220 },
            { prop: 'defaults', label: '默认', width: 100, bool: true },
            { prop: 'orgid', label: '组织', width: 120 },
            { prop: 'siteid', label: '站点', width: 120 }
          ]
        },
        {
          key: 'numericdomain',
          label: '数值',
          columns: [
            { prop: 'value', label: '值', width: 160 },
            { prop: 'description', label: '描述', width: 220 },
            { prop: 'orgid', label: '组织', width: 120 },
            { prop: 'siteid', label: '站点', width: 120 }
          ]
        },
        {
          key: 'numrangedomain',
          label: '数值范围',
          columns: [
            { prop: 'rangesegment', label: '范围段', width: 140 },
            { prop: 'rangeminimum', label: '范围最小值', width: 160 },
            { prop: 'rangemaximum', label: '范围最大值', width: 160 },
            { prop: 'rangeinterval', label: '范围间隔', width: 160 },
            { prop: 'orgid', label: '组织', width: 120 },
            { prop: 'siteid', label: '站点', width: 120 }
          ]
        },
        {
          key: 'tabledomain',
          label: '表域',
          columns: [
            { prop: 'objectname', label: '对象名', width: 180 },
            { prop: 'validtnwhereclause', label: '验证条件', width: 240 },
            { prop: 'listwhereclause', label: '列表条件', width: 240 },
            { prop: 'errorresourcbundle', label: '错误资源包', width: 180 },
            { prop: 'erroraccesskey', label: '错误访问键', width: 180 },
            { prop: 'orgid', label: '组织', width: 120 },
            { prop: 'siteid', label: '站点', width: 120 }
          ]
        }
      ],
      crossoverColumns: [
        { prop: 'sourcefield', label: '源字段', width: 160 },
        { prop: 'destfield', label: '目标字段', width: 160 },
        { prop: 'sourcecondition', label: '源条件', width: 220 },
        { prop: 'destcondition', label: '目标条件', width: 220 },
        { prop: 'copyevenifsrcnull', label: '源为空也复制', width: 140, bool: true },
        { prop: 'copyonlyifdestnull', label: '仅目标为空时复制', width: 160, bool: true },
        { prop: 'sequence', label: '序号', width: 80 },
        { prop: 'orgid', label: '组织', width: 120 },
        { prop: 'siteid', label: '站点', width: 120 }
      ],
      dialogVisible: false,
      detailLoading: false,
      currentRow: null,
      detailRow: null,
      activeChildTab: '',
      detailSimpleJson: '',
      detailFullJson: '',
      detailSimpleLoading: false,
      detailFullLoading: false,
      detailJsonOptions: {
        simple: { _allLangcode: true, withUuid: false },
        full: { _allLangcode: true, withUuid: false }
      },
      exportDialogVisible: false,
      exportLoading: false,
      exportJson: '',
      exportOptions: {
        _allLangcode: true,
        withUuid: false,
        ignoreDefVal: false
      },
      deleteNotExistsOptions: [
        { label: 'ALN值', value: 'alndomain' },
        { label: '同义词', value: 'synonymdomain' },
        { label: '数值', value: 'numericdomain' },
        { label: '数值范围', value: 'numrangedomain' },
        { label: '表域', value: 'tabledomain' }
      ],
      importDialog: {
        visible: false,
        loading: false,
        fileList: [],
        text: '',
        allLangcode: false,
        dmDeploySet: false,
        errorAssert: false,
        deleteNotExistsList: [],
        resultData: null,
        summary: '',
        resultSuccess: false
      }
    }
  },
  computed: {
    detailTitle() {
      const row = this.currentRow
      return '域详情' + (row && row.domainid ? ' - ' + row.domainid : '')
    },
    childTabs() {
      const row = this.detailRow || {}
      const tabs = []
      this.childConfigs.forEach(cfg => {
        const rows = row['_skschild_' + cfg.key]
        if (Array.isArray(rows)) {
          tabs.push({
            key: cfg.key,
            label: cfg.label,
            rows,
            columns: cfg.columns
          })
        }
      })
      return tabs
    },
    exportScopeText() {
      if (this.selectedRows.length) {
        return '按勾选导出 ' + this.selectedRows.length + ' 个域'
      }
      return '按当前筛选条件导出'
    },
    importResultRows() {
      const payload = this.importDialog.resultData
      if (!payload || !Array.isArray(payload.results)) {
        return []
      }
      return payload.results.map(row => {
        const item = row || {}
        return {
          objectName: item['sks:objectName'] || item.domainid || '',
          ref: item['sks:ref'] || item.domainid || '',
          operation: item._operation || '',
          status: item._status || '',
          message: item._message || ''
        }
      })
    }
  },
  created() {
    this.safeLoadLanguageConfig()
  },
  methods: {
    async safeLoadLanguageConfig() {
      try {
        await loadLanguageConfig()
      } catch (e) {
        console.warn('加载语言配置失败，使用默认语言', e)
      }
    },
    isTruthy(value) {
      if (value === true || value === 1) return true
      if (value === false || value === 0 || value === null || value === undefined || value === '') return false
      return ['1', 'Y', 'YES', 'TRUE'].indexOf(String(value).toUpperCase()) >= 0
    },
    boolText(value) {
      return this.isTruthy(value) ? '是' : '否'
    },
    isEmptyCell(value) {
      return value === null || value === undefined || value === ''
    },
    formatMainValue(key) {
      const row = this.detailRow
      if (!row) return '-'
      const value = row[key]
      if (this.isEmptyCell(value)) return '-'
      if (this.boolMainKeys.indexOf(key) >= 0) return this.boolText(value)
      return String(value)
    },
    cellText(col, row) {
      const value = row ? row[col.prop] : null
      if (this.isEmptyCell(value)) return '-'
      if (col.bool) return this.boolText(value)
      return String(value)
    },
    domainTypeTagType(type) {
      const map = {
        ALN: '',
        SYNONYM: 'success',
        NUMERIC: 'warning',
        NUMRANGE: 'warning',
        TABLE: 'danger',
        CROSSOVER: 'info'
      }
      return map[type] === undefined ? 'info' : map[type]
    },
    statusTagType(status) {
      if (status === 'success') return 'success'
      if (status === 'failed') return 'danger'
      return 'info'
    },
    crossoverRows(row) {
      const rows = row && row._skschild_crossoverdomain
      return Array.isArray(rows) ? rows : []
    },
    importRowClass({ row }) {
      return row && row.status === 'failed' ? 'row-failed' : ''
    },
    importStat(key) {
      const data = this.importDialog.resultData
      if (!data) return 0
      return Number(data[key] || 0)
    },
    escapeSql(value) {
      return String(value || '').replace(/'/g, "''")
    },
    likeCond(field, input) {
      const val = this.escapeSql(String(input || '').trim())
      if (val.startsWith('=')) {
        return field + " = '" + val.slice(1) + "'"
      }
      const like = val.indexOf('%') >= 0 ? val : '%' + val + '%'
      return 'UPPER(' + field + ') LIKE UPPER(\'' + like + '\')'
    },
    buildWhere() {
      const conds = []
      if (this.formData.domainid) {
        conds.push(this.likeCond('DOMAINID', this.formData.domainid))
      }
      if (this.formData.description) {
        conds.push(this.likeCond('DESCRIPTION', this.formData.description))
      }
      if (this.formData.domaintype) {
        conds.push("DOMAINTYPE = '" + this.escapeSql(this.formData.domaintype) + "'")
      }
      if (this.formData.maxtype) {
        conds.push(this.likeCond('MAXTYPE', this.formData.maxtype))
      }
      return conds.length ? conds.join(' AND ') : '1=1'
    },
    handleSelectionChange(rows) {
      this.selectedRows = rows
    },
    handleQuery() {
      this.tableParam.pageNum = 1
      this.getList()
    },
    async getList() {
      this.loading = true
      this.selectedRows = []
      await this.safeLoadLanguageConfig()
      const params = {
        _action: 'query',
        _langcode: getDefaultLangcode()
      }
      if (this.formData.domainid) {
        params.domainid = this.formData.domainid.trim()
      }
      if (this.formData.description) {
        params.description = this.formData.description.trim()
      }
      if (this.formData.domaintype) {
        params.domaintype = this.formData.domaintype
      }
      if (this.formData.maxtype) {
        params.maxtype = this.formData.maxtype.trim()
      }
      params.pageNum = this.tableParam.pageNum
      params.pageSize = this.tableParam.pageSize
      try {
        const res = await queryDomain(params)
        const data = res.data || res
        if (data && data.status === 'error') {
          handleApiError(data, data.message || '查询失败')
          return
        }
        const payload = (data && data.data) || data || {}
        const list = payload.domainList || []
        this.domains = list
        this.tableParam.total = Number(payload.total || list.length)
        this.hasSearched = true
      } catch (err) {
        console.error('查询域失败', err)
        const respData = err.response && err.response.data
        handleApiError(respData && typeof respData === 'object' ? respData : null, '查询域失败：' + (err.message || String(err)))
      } finally {
        this.loading = false
      }
    },
    resetForm() {
      this.formData = {
        domainid: '',
        description: '',
        domaintype: '',
        maxtype: ''
      }
      this.domains = []
      this.selectedRows = []
      this.tableParam.total = 0
      this.tableParam.pageNum = 1
      this.hasSearched = false
    },
    async handleRowClick(row, column) {
      if (column && column.type === 'selection') return
      this.currentRow = row
      this.detailRow = null
      this.activeChildTab = ''
      this.detailSimpleJson = ''
      this.detailFullJson = ''
      this.dialogVisible = true
      this.detailLoading = true
      await this.safeLoadLanguageConfig()
      try {
        // 完整导出同时作为详情数据源(含 _skschild_* 子表与 sks:langdata)
        const [fullList] = await Promise.all([
          this.loadDetailJson('full'),
          this.loadDetailJson('simple')
        ])
        const detail = fullList.length ? fullList[0] : null
        this.detailRow = detail
        if (detail) {
          const tab = this.childConfigs.find(cfg => Array.isArray(detail['_skschild_' + cfg.key]))
          this.activeChildTab = tab ? tab.key : 'jsonSimple'
        } else {
          this.$message.warning('未找到域详情数据')
        }
      } catch (err) {
        console.warn('获取域详情失败:', err)
        const respData = err.response && err.response.data
        handleApiError(respData && typeof respData === 'object' ? respData : null, '获取域详情失败：' + (err.message || String(err)))
      } finally {
        this.detailLoading = false
      }
    },
    // kind: simple=精简(ignoreDefVal) / full=完整；返回 domainList 供详情结构化展示
    async loadDetailJson(kind) {
      const domainid = this.currentRow && this.currentRow.domainid
      if (!domainid) return []
      const isSimple = kind === 'simple'
      const opt = this.detailJsonOptions[kind]
      if (isSimple) {
        this.detailSimpleLoading = true
      } else {
        this.detailFullLoading = true
      }
      try {
        const res = await migrateDomain({
          _action: 'export',
          _langcode: getDefaultLangcode(),
          _allLangcode: opt._allLangcode ? 'true' : 'false',
          ignoreDefVal: isSimple ? 'true' : 'false'
        }, { domainids: [domainid] })
        const data = res.data || res
        if (data && data.status === 'error') {
          handleApiError(data, data.message || '获取域JSON失败')
          return []
        }
        const payload = (data && data.data) || data || {}
        const list = payload.domainList || []
        const rows = opt.withUuid ? list.map(item => this.stampRowUuid(item)) : list
        const json = rows.length ? JSON.stringify({ domainList: rows }, null, 2) : ''
        if (isSimple) {
          this.detailSimpleJson = json
        } else {
          this.detailFullJson = json
        }
        return list
      } catch (err) {
        console.warn('获取域JSON失败:', err)
        const respData = err.response && err.response.data
        handleApiError(respData && typeof respData === 'object' ? respData : null, '获取域JSON失败：' + (err.message || String(err)))
        return []
      } finally {
        if (isSimple) {
          this.detailSimpleLoading = false
        } else {
          this.detailFullLoading = false
        }
      }
    },
    copyDetailJson(kind) {
      const json = kind === 'simple' ? this.detailSimpleJson : this.detailFullJson
      if (!json) return
      this.copyToClipboard(json, kind === 'simple' ? '域精简JSON' : '域完整JSON')
    },
    openExportDialog() {
      this.exportJson = ''
      this.exportDialogVisible = true
    },
    async generateExport() {
      if (!this.selectedRows.length && !this.hasSearched) {
        this.$message.warning('暂无数据可导出，请先查询域')
        return
      }
      this.exportLoading = true
      try {
        await this.safeLoadLanguageConfig()
        const params = {
          _action: 'export',
          _langcode: getDefaultLangcode(),
          _allLangcode: this.exportOptions._allLangcode ? 'true' : 'false',
          ignoreDefVal: this.exportOptions.ignoreDefVal ? 'true' : 'false'
        }
        const body = this.selectedRows.length
          ? { domainids: this.selectedRows.map(row => row.domainid).filter(Boolean) }
          : { where: this.buildWhere() }
        const res = await migrateDomain(params, body)
        const data = res.data || res
        if (data && data.status === 'error') {
          handleApiError(data, data.message || '导出失败')
          return
        }
        const payload = (data && data.data) || data || {}
        const list = payload.domainList || []
        if (!list.length) {
          this.exportJson = ''
          this.$message.warning('未查询到可导出的域数据')
          return
        }
        const rows = this.exportOptions.withUuid ? list.map(item => this.stampRowUuid(item)) : list
        this.exportJson = JSON.stringify({ domainList: rows }, null, 2)
        this.$message.success('已生成导出 JSON，共 ' + rows.length + ' 个域')
      } catch (err) {
        console.error('导出域失败:', err)
        const respData = err.response && err.response.data
        handleApiError(respData && typeof respData === 'object' ? respData : null, '导出失败：' + (err.message || String(err)))
      } finally {
        this.exportLoading = false
      }
    },
    // 递归给每行(含 _skschild_* 子数组)补 sks:uuid，导入时原样带回用于对上导出行
    stampRowUuid(row) {
      const copy = Object.assign({}, row, { 'sks:uuid': generateUUID() })
      Object.keys(copy).forEach(key => {
        if (key.indexOf('_skschild_') === 0 && Array.isArray(copy[key])) {
          copy[key] = copy[key].map(item => this.stampRowUuid(item))
        }
      })
      return copy
    },
    copyExportJson() {
      if (!this.exportJson) return
      this.copyToClipboard(this.exportJson, '导出JSON')
    },
    buildExportFileName() {
      const d = new Date()
      const pad = n => (n < 10 ? '0' + n : String(n))
      const stamp = '' + d.getFullYear() + pad(d.getMonth() + 1) + pad(d.getDate()) +
        pad(d.getHours()) + pad(d.getMinutes()) + pad(d.getSeconds())
      return 'domain-export-' + stamp + '.json'
    },
    downloadExportJson() {
      if (!this.exportJson) return
      const blob = new Blob([this.exportJson], { type: 'application/json;charset=utf-8' })
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = this.buildExportFileName()
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      URL.revokeObjectURL(url)
      this.$message.success('JSON 已保存')
    },
    copyToClipboard(text, label) {
      if (navigator.clipboard && window.isSecureContext) {
        navigator.clipboard.writeText(text).then(() => {
          this.$message.success(label + ' 已复制到剪贴板')
        }).catch(() => {
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
    },
    openImportDialog() {
      this.importDialog.visible = true
      this.importDialog.loading = false
      this.importDialog.fileList = []
      this.importDialog.text = ''
      this.importDialog.allLangcode = false
      this.importDialog.dmDeploySet = false
      this.importDialog.errorAssert = false
      this.importDialog.deleteNotExistsList = []
      this.importDialog.resultData = null
      this.importDialog.summary = ''
      this.importDialog.resultSuccess = false
      if (this.$refs.importUpload) {
        this.$refs.importUpload.clearFiles()
      }
    },
    parseImportPayload(text) {
      let parsed
      try {
        parsed = JSON.parse(text)
      } catch (e) {
        throw new Error('JSON 解析失败: ' + e.message)
      }
      let list
      if (Array.isArray(parsed)) {
        list = parsed
      } else if (parsed && Array.isArray(parsed.domainList)) {
        list = parsed.domainList
      } else if (parsed && parsed.domainid) {
        list = [parsed]
      }
      if (!list || !list.length) {
        throw new Error('未找到可导入的域数据（需要 domainList 数组）')
      }
      return list
    },
    onImportFileChange(file) {
      const raw = file && file.raw
      if (!raw) return
      const reader = new FileReader()
      reader.onload = () => {
        const text = String(reader.result || '')
        this.importDialog.text = text
        try {
          const list = this.parseImportPayload(text)
          this.$message.success('已读取 ' + list.length + ' 个域，确认后点击「导 入」')
        } catch (e) {
          this.$message.error(e.message || String(e))
        }
      }
      reader.onerror = () => {
        this.$message.error('读取文件失败')
      }
      reader.readAsText(raw, 'utf-8')
    },
    onImportFileRemove() {
      this.importDialog.fileList = []
      this.importDialog.text = ''
    },
    async doImport() {
      let list
      try {
        list = this.parseImportPayload(this.importDialog.text)
      } catch (e) {
        this.$message.error(e.message || String(e))
        return
      }
      try {
        await this.$confirm('确认导入 ' + list.length + ' 个域定义？', '提示', { type: 'warning' })
      } catch (e) {
        return
      }
      this.importDialog.loading = true
      this.importDialog.resultData = null
      this.importDialog.summary = ''
      this.importDialog.resultSuccess = false
      try {
        await this.safeLoadLanguageConfig()
        const params = {
          _action: 'import',
          _langcode: getDefaultLangcode(),
          _allLangcode: this.importDialog.allLangcode ? 'true' : 'false'
        }
        if (this.importDialog.dmDeploySet) {
          params.dmDeploySet = 'true'
        }
        if (this.importDialog.errorAssert) {
          params._errorAssert = 'true'
        }
        const keys = this.importDialog.deleteNotExistsList || []
        if (keys.length) {
          params._deleteNotExists = keys.map(key => 'domainList:_skschild_' + key + ':' + key).join(',')
        }
        const res = await migrateDomain(params, { domainList: list })
        const data = res.data || res
        if (data && data.status === 'error') {
          this.importDialog.resultSuccess = false
          this.importDialog.summary = data.message || '导入失败'
          handleApiError(data, data.message || '导入失败')
          return
        }
        const payload = (data && data.data) || data || {}
        const results = Array.isArray(payload.results) ? payload.results : []
        const failed = payload.failed !== undefined
          ? Number(payload.failed)
          : results.filter(item => item && item._status === 'failed').length
        this.importDialog.resultData = payload
        this.importDialog.resultSuccess = failed === 0
        this.importDialog.summary = '共 ' + Number(payload.total || list.length) + ' 个域：新增 ' +
          Number(payload.added || 0) + '，更新 ' + Number(payload.updated || 0) +
          '，删除 ' + Number(payload.deleted || 0) + '，失败 ' + failed +
          '；成功 ' + Number(payload['sks:successSumTotal'] || 0) +
          '，失败 ' + Number(payload['sks:failSumTotal'] || 0) +
          '，忽略 ' + Number(payload['sks:ignoreSumTotal'] || 0)
        if (this.importDialog.resultSuccess) {
          this.$message.success(this.importDialog.summary)
        } else {
          this.$message.warning(this.importDialog.summary)
        }
        this.hasSearched = true
        this.getList()
      } catch (err) {
        console.error('导入域失败:', err)
        const respData = err.response && err.response.data
        handleApiError(respData && typeof respData === 'object' ? respData : null, '导入失败：' + (err.message || String(err)))
      } finally {
        this.importDialog.loading = false
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
.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.result-panel {
  margin-top: 20px;
}
.result-table {
  margin-top: 16px;
}
.result-table ::v-deep .el-table__body tr {
  cursor: pointer;
}
.detail-body {
  min-height: 120px;
}
.json-tabs {
  margin-top: 16px;
}
.json-toolbar {
  margin-bottom: 8px;
  text-align: right;
}
.detail-json-toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  text-align: left;
}
.export-options {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 12px;
}
.export-count {
  color: #909399;
  font-size: 12px;
}
.import-upload {
  margin-top: 12px;
}
.import-options {
  margin-top: 12px;
}
.import-summary {
  margin-top: 12px;
}
.import-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}
.import-result-table {
  margin-top: 12px;
}
.import-result-table ::v-deep .row-failed td {
  background-color: #fef0f0 !important;
}
.crossover-panel {
  padding: 8px 16px;
}
.crossover-title {
  margin-bottom: 6px;
  color: #606266;
  font-size: 12px;
}
.ml-empty {
  color: #c0c4cc;
  font-style: italic;
}
</style>
