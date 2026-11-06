<template>
  <section class="query-page">
    <el-card>
      <div class="page-header-row">
        <div>
          <h2>安全组查询</h2>
          <p class="page-summary">支持组名 / 描述模糊查询，点击行查看安全组权限详情，可导出 / 导入安全组。</p>
        </div>
        <div class="header-actions">
          <el-button type="primary" icon="el-icon-upload2" size="mini" @click="openImportDialog">导入</el-button>
          <el-button type="primary" icon="el-icon-download" size="mini" @click="openExportDialog">导出</el-button>
        </div>
      </div>
      <el-form ref="queryForm" :model="formData" :inline="true" label-width="68px" @submit.native.prevent>
        <el-form-item label="组名">
          <el-input v-model="formData.groupname" placeholder="模糊搜索组名" @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="formData.description" placeholder="模糊搜索描述" @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item>
          <el-button type="cyan" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" size="mini" @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="result-panel">
        <el-table
          v-loading="loading"
          :data="groups"
          stripe
          style="width: 100%"
          class="result-table"
          :highlight-current-row="true"
          @selection-change="handleSelectionChange"
          @row-click="handleRowClick"
        >
          <el-table-column type="selection" width="46" />
          <el-table-column prop="GROUPNAME" label="组名" min-width="140" show-overflow-tooltip />
          <el-table-column prop="DESCRIPTION" label="描述" min-width="200" show-overflow-tooltip>
            <template slot-scope="scope">
              <span :class="{'ml-empty': !scope.row.DESCRIPTION}">{{ scope.row.DESCRIPTION || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="INDEPENDENT" label="独立" width="90">
            <template slot-scope="scope">
              <el-tag :type="isTruthy(scope.row.INDEPENDENT) ? 'success' : 'info'" size="mini">{{ boolText(scope.row.INDEPENDENT) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="AUTHALLSITES" label="所有站点" width="100">
            <template slot-scope="scope">
              <el-tag :type="isTruthy(scope.row.AUTHALLSITES) ? 'success' : 'info'" size="mini">{{ boolText(scope.row.AUTHALLSITES) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="DFLTAPP" label="默认应用" min-width="120" show-overflow-tooltip />
          <el-table-column prop="LANGCODE" label="语言" width="80" />
          <el-table-column prop="appcount" label="应用数" width="80" />
          <el-table-column prop="sitecount" label="站点数" width="80" />
          <el-table-column prop="usercount" label="用户数" width="80" />
        </el-table>
        <pagination
          v-show="tablePatam.total > 0"
          :total="tablePatam.total"
          :page.sync="tablePatam.pageNum"
          :limit.sync="tablePatam.pageSize"
          @pagination="getList"
        />
      </div>
    </el-card>

    <!-- 安全组详情弹窗 -->
    <el-dialog :title="'安全组详情 - ' + (currentRow ? currentRow.GROUPNAME : '')" :visible.sync="dialogVisible" width="1400px" top="5vh" :close-on-click-modal="true">
      <div v-loading="detailLoading" element-loading-text="加载中...">
        <el-descriptions v-if="detailRow" :column="3" border size="small">
          <el-descriptions-item v-for="field in mainFields" :key="field.key" :label="field.label">
            <el-input :value="formatMainValue(field.key)" readonly size="small" />
          </el-descriptions-item>
        </el-descriptions>

        <el-tabs v-if="detailRow" v-model="activeRelTab" type="border-card" class="json-tabs">
          <el-tab-pane
            v-for="rel in relConfigs"
            :key="rel.key"
            :label="rel.label + '(' + subTables[rel.key].rows.length + ')'"
            :name="rel.key"
            lazy
          >
            <div v-if="rel.filters" class="subtable-filter-bar">
              <template v-for="f in rel.filters">
                <el-select
                  v-if="f.type === 'select'"
                  :key="rel.key + '-' + f.key"
                  v-model="subFilterValues[rel.key][f.key]"
                  :placeholder="f.label"
                  size="mini"
                  filterable
                  clearable
                  style="width:230px;"
                >
                  <el-option
                    v-for="opt in subFilterOptions(rel.key, f.key)"
                    :key="opt"
                    :label="subFilterOptionLabel(rel.key, f.key, opt)"
                    :value="opt"
                  />
                </el-select>
                <el-input
                  v-else
                  :key="rel.key + '-' + f.key"
                  v-model="subFilterValues[rel.key][f.key]"
                  :placeholder="f.label + ' 模糊过滤'"
                  size="mini"
                  clearable
                  style="width:260px;"
                />
              </template>
              <el-button size="mini" icon="el-icon-refresh-left" @click="resetSubFilter(rel.key)">清除过滤</el-button>
              <span class="filter-count">显示 {{ subTables[rel.key].rows.length }} / {{ subTables[rel.key].totalRows }} 条</span>
            </div>
            <el-table :data="subTables[rel.key].rows" border stripe size="mini" max-height="420" style="width:100%">
              <el-table-column
                v-for="col in subTables[rel.key].columns"
                :key="col"
                :prop="col"
                :label="colLabel(col)"
                min-width="140"
                show-overflow-tooltip
              />
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">关 闭</el-button>
      </span>
    </el-dialog>

    <!-- 导出安全组弹窗 -->
    <el-dialog title="导出安全组(点击刷新之后获得导出的json)" :visible.sync="batchDialogVisible" width="1400px" top="5vh" :close-on-click-modal="true" @opened="onBatchDialogOpened" @closed="disposeBatchEditors">
      <div class="json-toolbar" style="margin-bottom: 12px; text-align: left;">
        <el-radio-group v-model="exportOptions.scope" size="mini">
          <el-radio-button label="selected">选中组</el-radio-button>
          <el-radio-button label="current">当前搜索结果</el-radio-button>
        </el-radio-group>
        <el-checkbox v-model="exportOptions.withUuid" style="margin-left: 12px;">每行设置UUID</el-checkbox>
        <el-button size="mini" icon="el-icon-refresh" :loading="batchLoading" @click="loadExportData">刷 新</el-button>
        <span style="margin-left: 10px;" class="el-tag el-tag--info">共 {{ batchTotal }} 组</span>
      </div>
      <el-alert type="info" :closable="false" show-icon title="设置导出选项后点击「刷新」生成 JSON，避免大数据量导出时长时间等待" style="margin-bottom: 12px;" />
      <el-tabs v-model="batchActiveTab" type="border-card" class="json-tabs">
        <el-tab-pane label="完整导出" name="batch-full">
          <div class="json-toolbar">
            <el-button type="primary" size="mini" icon="el-icon-document-copy" @click="copyBatchJson">复制 JSON</el-button>
            <el-button type="primary" size="mini" icon="el-icon-download" @click="saveBatchJson">保存 JSON</el-button>
          </div>
          <div class="monaco-wrapper">
            <div ref="batchMonacoRef" class="monaco-container" />
          </div>
        </el-tab-pane>
        <el-tab-pane label="原始JSON" name="batch-raw" lazy>
          <div class="json-toolbar">
            <el-button type="primary" size="mini" icon="el-icon-document-copy" @click="copyBatchJson">复制 JSON</el-button>
            <el-button type="primary" size="mini" icon="el-icon-download" @click="saveBatchJson">保存 JSON</el-button>
          </div>
          <el-input :value="batchJson" type="textarea" :rows="16" readonly />
        </el-tab-pane>
      </el-tabs>

      <span slot="footer" class="dialog-footer">
        <el-button @click="batchDialogVisible = false">关 闭</el-button>
      </span>
    </el-dialog>

    <!-- 导入安全组弹窗 -->
    <el-dialog title="导入安全组" :visible.sync="importDialog.visible" width="85%" top="4vh" custom-class="import-dialog" :close-on-click-modal="false">
      <el-alert type="info" :closable="false" show-icon title="粘贴导出的安全组 JSON 或选择 .json 文件（支持 {groups:[...]} / 数组 / 单个安全组对象）；导入以编辑框内容为准" />
      <el-upload
        ref="importUpload"
        style="margin-top:12px;"
        :file-list="importDialog.fileList"
        :auto-upload="false"
        :limit="1"
        accept=".json"
        :on-change="onImportFileChange"
        :on-remove="onImportFileRemove"
      >
        <el-button size="small" icon="el-icon-folder-opened">选择 JSON 文件</el-button>
      </el-upload>
      <el-form label-width="110px" style="margin-top:12px;">
        <el-form-item label="启用迁移模式">
          <el-checkbox v-model="importDialog.dmDeploySet">dmDeploySet（MboSet 调用 setDMDeploySet(true)）</el-checkbox>
        </el-form-item>
        <el-form-item label="分批导入">
          <el-checkbox v-model="importDialog.batchImport">启用分批导入</el-checkbox>
          <span style="margin-left:16px;color:#909399;font-size:12px;">每批提交条数</span>
          <el-input-number
            v-model="importDialog.batchSize"
            :min="1"
            :max="5000"
            :disabled="!importDialog.batchImport"
            size="small"
            style="margin-left:8px;width:130px;"
          />
        </el-form-item>
      </el-form>
      <el-input
        v-model="importDialog.text"
        type="textarea"
        :rows="12"
        placeholder="在此粘贴安全组 JSON（{groups:[...]}）"
      />
      <div v-if="importDialog.loading" class="import-result-bar">
        <span class="import-result-summary">{{ importDialog.summary }}</span>
        <el-progress :percentage="importPercent" :stroke-width="8" style="flex:1;" />
      </div>
      <div v-if="importDialog.result" class="import-result-bar">
        <el-tag :type="importDialog.resultSuccess ? 'success' : 'danger'" size="small">{{ importDialog.resultSuccess ? '导入成功' : '导入失败' }}</el-tag>
        <span class="import-result-summary">{{ importDialog.summary }}</span>
        <el-button type="text" size="mini" icon="el-icon-document" @click="openImportResult">查看详细返回信息</el-button>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="importDialog.visible = false">关 闭</el-button>
        <el-button type="primary" :loading="importDialog.loading" @click="doImport">导 入</el-button>
      </span>
    </el-dialog>

    <!-- 导入详细返回信息弹窗 -->
    <el-dialog title="导入详细返回信息" :visible.sync="importResultDialog.visible" width="82%" top="5vh" :close-on-click-modal="false">
      <template v-if="importRows.length">
        <el-tabs v-model="importResultDialog.activeTab">
          <el-tab-pane label="列表" name="list">
            <vxe-table
              :data="importRows"
              border
              stripe
              size="mini"
              height="520"
              :virtual-y-config="{ enabled: true, gt: 50 }"
              style="width:100%"
            >
              <vxe-column field="ref" title="组名(sks:ref)" min-width="240" show-overflow="tooltip" />
              <vxe-column field="status" title="状态" width="90">
                <template #default="{ row }">
                  <el-tag v-if="row.status" :type="statusTagType(row.status)" size="mini">{{ row.status }}</el-tag>
                </template>
              </vxe-column>
              <vxe-column field="operation" title="操作" width="90" />
              <vxe-column field="message" title="消息" min-width="240" show-overflow="tooltip" />
            </vxe-table>
          </el-tab-pane>
          <el-tab-pane label="原始JSON" name="raw" lazy>
            <el-input :value="importDialog.result" type="textarea" :rows="22" readonly />
          </el-tab-pane>
        </el-tabs>
      </template>
      <el-input v-else :value="importDialog.result" type="textarea" :rows="22" readonly />
      <span slot="footer" class="dialog-footer">
        <el-button type="primary" @click="copyImportRawJson">复制原始JSON</el-button>
        <el-button @click="importResultDialog.visible = false">关 闭</el-button>
      </span>
    </el-dialog>
  </section>
</template>

<script>
import { migrateSecurGroup, querySecurGroup } from '../../../api/securgroup'
import { getDefaultLangcode, loadLanguageConfig } from '../../../utils/mmp/mmp-config'
import { handleApiError, createApiError } from '../../../utils/mmp/api-error'
import { generateUUID } from '../../../utils/sks'

export default {
  name: 'SecurGroupQuery',
  data() {
    return {
      tablePatam: {
        total: 0,
        pageNum: 1,
        pageSize: 10
      },
      loading: false,
      batchLoading: false,
      detailLoading: false,
      error: '',
      groups: [],
      selectedGroups: [],
      formData: {
        groupname: '',
        description: ''
      },
      dialogVisible: false,
      currentRow: null,
      detailRow: null,
      activeRelTab: 'APPLICATIONAUTH',
      relConfigs: [
        {
          key: 'APPLICATIONAUTH',
          label: '应用授权',
          filters: [
            { key: 'APP', label: '应用名称', type: 'select' },
            { key: 'OPTIONNAME', label: 'OPTIONNAME', type: 'select' },
            { key: 'sks:sigdescription', label: '签名描述', type: 'text' }
          ]
        },
        { key: 'SITEAUTH', label: '站点授权' },
        { key: 'LOCAUTH', label: '位置授权' },
        { key: 'LABORAUTH', label: '班组授权' },
        { key: 'GLAUTH', label: '总账授权' },
        { key: 'GROUPUSER', label: '组成员' },
        { key: 'GRPREASSIGNAUTH', label: '重新分派授权' },
        { key: 'LIMITTOLERANCE', label: '限额容差' },
        { key: 'SECURITYRESTRICT', label: '安全限制' },
        { key: 'CTRLGROUP', label: '控制组' },
        { key: 'COLLECTIONAUTH', label: '集合授权' },
        { key: 'DASHBOARDAUTH', label: '仪表盘授权' },
        { key: 'REPFACAUTH', label: '报表文件夹授权' }
      ],
      mainFields: [
        { key: 'GROUPNAME', label: '组名' },
        { key: 'DESCRIPTION', label: '描述' },
        { key: 'INDEPENDENT', label: '独立' },
        { key: 'DFLTAPP', label: '默认应用' },
        { key: 'LANGCODE', label: '语言' },
        { key: 'SCTEMPLATEID', label: '安全模板' },
        { key: 'AUTHALLSITES', label: '所有站点' },
        { key: 'AUTHALLSTOREROOMS', label: '所有仓库' },
        { key: 'AUTHALLREPFACS', label: '所有报表' },
        { key: 'AUTHLABORALL', label: '所有班组' },
        { key: 'AUTHPERSONGROUP', label: '人员组授权' },
        { key: 'SIDENAV', label: '侧边导航' },
        { key: 'WORKCENTER', label: '工作台' },
        { key: 'MASISSUER', label: 'MAS颁发者' },
        { key: 'PASSWORDDURATION', label: '密码有效期' },
        { key: 'PASSWORDWARNING', label: '密码警告期' },
        { key: 'MAXSCHEDREPORT', label: '最大计划报表' },
        { key: 'REPORTSTOPLIMIT', label: '报表停止上限' },
        { key: 'NULLREPFAC', label: '空报表文件夹' },
        { key: 'ADHOCCREATELIMIT', label: '临时创建上限' },
        { key: 'AUTHALLGLS', label: '所有总账' },
        { key: 'HASLD', label: 'HASLD' }
      ],
      boolMainKeys: ['INDEPENDENT', 'AUTHALLSITES', 'AUTHALLSTOREROOMS', 'AUTHALLREPFACS', 'AUTHLABORALL', 'AUTHPERSONGROUP', 'SIDENAV', 'NULLREPFAC', 'AUTHALLGLS', 'HASLD'],
      subFilterValues: {},
      colLabelMap: { 'sks:sigdescription': '签名描述' },
      batchDialogVisible: false,
      batchActiveTab: 'batch-full',
      exportOptions: {
        scope: 'current',
        withUuid: false
      },
      batchJson: '',
      batchTotal: 0,
      batchEditor: null,
      monacoLoaded: false,
      monacoInstance: null,
      importDialog: {
        visible: false,
        loading: false,
        dmDeploySet: false,
        batchImport: true,
        batchSize: 50,
        fileList: [],
        text: '',
        result: '',
        resultData: null,
        resultSuccess: false,
        summary: '',
        completed: 0,
        totalCount: 0
      },
      importResultDialog: {
        visible: false,
        activeTab: 'list'
      },
      importRunId: 0
    }
  },
  computed: {
    subTables() {
      const result = {}
      const src = this.detailRow || {}
      for (let i = 0; i < this.relConfigs.length; i++) {
        const rel = this.relConfigs[i]
        const rawRows = Array.isArray(src[rel.key]) ? src[rel.key] : []
        const colSet = {}
        const columns = []
        for (let j = 0; j < rawRows.length; j++) {
          const row = rawRows[j] || {}
          const keys = Object.keys(row)
          for (let k = 0; k < keys.length; k++) {
            const key = keys[k]
            if (key === 'sks:appdescription') continue // 仅用于应用下拉显示, 不进列
            if (!colSet[key]) {
              colSet[key] = true
              columns.push(key)
            }
          }
        }
        let rows = rawRows
        if (rel.filters && this.subFilterValues[rel.key]) {
          const fv = this.subFilterValues[rel.key]
          rows = rawRows.filter(row => rel.filters.every(f => {
            const v = fv[f.key]
            if (!v) return true
            const cell = (row[f.key] === null || row[f.key] === undefined) ? '' : String(row[f.key])
            if (f.type === 'text') {
              return cell.toLowerCase().indexOf(String(v).toLowerCase()) >= 0
            }
            return cell === String(v)
          }))
        }
        result[rel.key] = { rows, columns, totalRows: rawRows.length }
      }
      return result
    },
    importRows() {
      const payload = this.importDialog.resultData
      if (!payload) return []
      const rows = Array.isArray(payload.results) ? payload.results : []
      return rows.map(row => ({
        ref: (row && (row['sks:ref'] || row.GROUPNAME)) || '',
        status: (row && row._status) || '',
        operation: (row && row._operation) || '',
        message: (row && row._message) || ''
      }))
    },
    importPercent() {
      const total = this.importDialog.totalCount
      if (!total) return 0
      return Math.min(100, Math.round(this.importDialog.completed / total * 100))
    }
  },
  watch: {
    batchActiveTab() {
      this.$nextTick(() => {
        if (this.batchEditor) this.batchEditor.layout()
      })
    },
    batchDialogVisible(val) {
      if (!val) {
        this.disposeBatchEditors()
      }
    }
  },
  created() {
    loadLanguageConfig()
  },
  methods: {
    isTruthy(value) {
      if (value === true || value === 1) return true
      if (value === false || value === 0 || value === null || value === undefined || value === '') return false
      return ['1', 'Y', 'YES', 'TRUE'].indexOf(String(value).toUpperCase()) >= 0
    },
    boolText(value) {
      return this.isTruthy(value) ? '是' : '否'
    },
    formatMainValue(key) {
      const row = this.detailRow
      if (!row) return '-'
      const value = row[key]
      if (value === null || value === undefined || value === '') return '-'
      if (this.boolMainKeys.indexOf(key) >= 0) return this.boolText(value)
      return String(value)
    },
    handleSelectionChange(rows) {
      this.selectedGroups = rows
    },
    colLabel(col) {
      return this.colLabelMap[col] || col
    },
    resetAllSubFilters() {
      const m = {}
      this.relConfigs.forEach(rel => {
        if (rel.filters) {
          const f = {}
          rel.filters.forEach(fl => { f[fl.key] = '' })
          m[rel.key] = f
        }
      })
      this.subFilterValues = m
    },
    resetSubFilter(relKey) {
      const fv = this.subFilterValues[relKey]
      if (!fv) return
      Object.keys(fv).forEach(k => { fv[k] = '' })
    },
    subFilterOptionLabel(relKey, colKey, value) {
      if (colKey === 'APP') {
        const src = this.detailRow || {}
        const rows = Array.isArray(src[relKey]) ? src[relKey] : []
        for (let i = 0; i < rows.length; i++) {
          if (rows[i] && String(rows[i].APP) === String(value) && rows[i]['sks:appdescription']) {
            return value + ' - ' + rows[i]['sks:appdescription']
          }
        }
      }
      return value
    },
    subFilterOptions(relKey, colKey) {
      const src = this.detailRow || {}
      const rows = Array.isArray(src[relKey]) ? src[relKey] : []
      const set = {}
      rows.forEach(row => {
        const v = row ? row[colKey] : null
        if (v !== null && v !== undefined && v !== '') set[String(v)] = true
      })
      return Object.keys(set).sort()
    },
    async handleQuery() {
      this.tablePatam.pageNum = 1
      this.getList()
    },
    async getList() {
      this.loading = true
      this.error = ''
      this.groups = []
      await loadLanguageConfig()

      // 直接传字段值, 脚本中处理搜索逻辑
      const params = {
        _action: 'query',
        _langcode: getDefaultLangcode(),
        groupname: this.formData.groupname || undefined,
        description: this.formData.description || undefined,
        pageNum: this.tablePatam.pageNum,
        pageSize: this.tablePatam.pageSize
      }

      querySecurGroup(params).then(res => {
        const data = res.data || res
        if (data && data.status === 'error') {
          handleApiError(data, data.message || '查询失败')
          this.loading = false
          return
        }
        const payload = data.data || data
        const list = payload.groups || []
        this.groups = list
        this.tablePatam.total = payload.total || list.length
        this.loading = false
      }).catch(err => {
        console.error('查询失败', err)
        const respData = err.response && err.response.data
        handleApiError(respData && typeof respData === 'object' ? respData : null, '查询安全组失败：' + (err.message || String(err)))
        this.loading = false
      })
    },
    resetForm() {
      this.formData = {
        groupname: '',
        description: ''
      }
      this.error = ''
      this.tablePatam.pageNum = 1
      this.groups = []
      this.selectedGroups = []
    },
    async handleRowClick(row, column) {
      // 点击选择框列不打开详情
      if (column && column.type === 'selection') return
      this.currentRow = row
      this.detailRow = null
      this.activeRelTab = this.relConfigs[0].key
      this.resetAllSubFilters()
      this.dialogVisible = true
      this.detailLoading = true
      await loadLanguageConfig()

      try {
        const res = await migrateSecurGroup({
          _action: 'detail',
          groupname: row.GROUPNAME,
          _langcode: getDefaultLangcode()
        }, {})
        const data = res.data || res
        if (data && data.status === 'error') {
          handleApiError(data, '获取安全组详情失败: ' + (data.message || '未知错误'))
          return
        }
        const payload = data.data || data
        const list = (payload && payload.groups) || []
        this.detailRow = list.length ? list[0] : null
        if (!this.detailRow) {
          this.$message.warning('未找到安全组详情数据')
        }
      } catch (err) {
        console.warn('获取安全组详情失败:', err)
        const respData = err.response && err.response.data
        handleApiError(respData && typeof respData === 'object' ? respData : null, '获取安全组详情失败: ' + (err.message || String(err)))
      } finally {
        this.detailLoading = false
      }
    },
    openExportDialog() {
      this.exportOptions.scope = this.selectedGroups.length ? 'selected' : 'current'
      this.batchActiveTab = 'batch-full'
      this.batchJson = ''
      this.batchTotal = 0
      this.batchDialogVisible = true
    },
    async loadExportData() {
      if (this.exportOptions.scope === 'selected') {
        if (!this.selectedGroups.length) {
          this.$message.warning('请先勾选要导出的安全组')
          return
        }
      } else if (!this.groups.length) {
        this.$message.warning('没有数据可导出，请先查询安全组')
        return
      }

      this.batchLoading = true
      try {
        await loadLanguageConfig()
        const params = {
          _action: 'export',
          _langcode: getDefaultLangcode()
        }
        let body
        if (this.exportOptions.scope === 'selected') {
          body = { groupnames: this.selectedGroups.map(item => item.GROUPNAME) }
        } else {
          body = {
            groupname: this.formData.groupname || undefined,
            description: this.formData.description || undefined
          }
        }

        const res = await migrateSecurGroup(params, body)
        const data = res.data || res
        if (data && data.status === 'error') {
          handleApiError(data, '导出失败: ' + (data.message || '未知错误'))
          return
        }
        const payload = data.data || data
        const list = (payload && payload.groups) || []
        if (!list.length) {
          this.$message.warning('未查询到安全组数据')
          this.batchJson = ''
          this.batchTotal = 0
          return
        }

        if (this.exportOptions.withUuid) {
          for (let i = 0; i < list.length; i++) {
            list[i]['sks:uuid'] = generateUUID()
          }
        }

        this.batchTotal = list.length
        this.batchJson = JSON.stringify({ groups: list }, null, 2)
        this.$nextTick(() => {
          this.initBatchMonacoEditors()
        })
      } catch (err) {
        console.error('导出失败:', err)
        // 尝试从 AxiosError 中提取服务器返回的错误信息
        var serverMsg = ''
        if (err.response && err.response.data) {
          if (typeof err.response.data === 'string') {
            serverMsg = err.response.data.substring(0, 200)
          } else if (err.response.data.message) {
            serverMsg = err.response.data.message
          } else if (err.response.data['oslc:Error']) {
            serverMsg = err.response.data['oslc:Error']['oslc:message'] || ''
          } else {
            serverMsg = JSON.stringify(err.response.data).substring(0, 200)
          }
        }
        var displayMsg = serverMsg || err.message || String(err)
        handleApiError(err.response && err.response.data, '导出失败: ' + displayMsg)
      } finally {
        this.batchLoading = false
      }
    },
    onBatchDialogOpened() {
      this.$nextTick(() => {
        setTimeout(() => {
          this.initBatchMonacoEditors()
        }, 100)
      })
    },
    initBatchMonacoEditors() {
      if (!this.batchJson) return
      if (!this.monacoLoaded) {
        import(/* webpackChunkName: "monaco" */ 'monaco-editor').then(monaco => {
          this.monacoLoaded = true
          this.monacoInstance = monaco
          this.createBatchEditor()
        }).catch(err => {
          console.error('Monaco Editor 加载失败:', err)
        })
      } else {
        this.createBatchEditor()
      }
    },
    createBatchEditor() {
      if (!this.$refs.batchMonacoRef) return
      const monaco = this.monacoInstance
      if (!this.batchEditor) {
        this.batchEditor = monaco.editor.create(this.$refs.batchMonacoRef, {
          value: this.batchJson,
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
        })
      } else {
        this.batchEditor.setValue(this.batchJson)
      }
      setTimeout(() => {
        if (this.batchEditor) this.batchEditor.layout()
      }, 100)
    },
    disposeBatchEditors() {
      if (this.batchEditor) {
        this.batchEditor.dispose()
        this.batchEditor = null
      }
    },
    copyBatchJson() {
      if (this.batchJson) {
        this.copyToClipboard(this.batchJson, '安全组JSON')
      }
    },
    saveBatchJson() {
      this.saveJsonFile(this.batchJson, '安全组')
    },
    saveJsonFile(text, name) {
      if (!text) {
        this.$message.warning('没有可保存的JSON')
        return
      }
      const blob = new Blob([text], { type: 'application/json;charset=utf-8' })
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = name + '-' + Date.now() + '.json'
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      URL.revokeObjectURL(url)
      this.$message.success('JSON 已保存')
    },
    openImportDialog() {
      // 使上一次仍在执行的导入任务失效, 避免旧进度/结果重新写回
      this.importRunId += 1
      this.importDialog.visible = true
      this.importDialog.loading = false
      this.importDialog.dmDeploySet = false
      this.importDialog.batchImport = true
      this.importDialog.batchSize = 50
      this.importDialog.fileList = []
      this.importDialog.text = ''
      this.importDialog.result = ''
      this.importDialog.resultData = null
      this.importDialog.resultSuccess = false
      this.importDialog.summary = ''
      this.importDialog.completed = 0
      this.importDialog.totalCount = 0
      if (this.$refs.importUpload) {
        this.$refs.importUpload.clearFiles()
      }
    },
    onImportFileChange(file) {
      const raw = file && file.raw
      if (!raw) return
      const reader = new FileReader()
      reader.onload = () => {
        const text = String(reader.result || '')
        this.importDialog.text = text
        try {
          const groups = this.parseImportGroups(text)
          this.$message.success('已读取 ' + groups.length + ' 个安全组，确认后点击“导 入”')
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
      this.importDialog.text = ''
    },
    parseImportGroups(text) {
      let parsed
      try {
        parsed = JSON.parse(text)
      } catch (e) {
        throw new Error('JSON 解析失败: ' + e.message)
      }
      let groups
      if (Array.isArray(parsed)) {
        groups = parsed
      } else if (parsed && Array.isArray(parsed.groups)) {
        groups = parsed.groups
      } else if (parsed && parsed.GROUPNAME) {
        groups = [parsed]
      }
      if (!groups || groups.length === 0) {
        throw new Error('未找到可导入的安全组数据（需要 groups 数组）')
      }
      return groups
    },
    extractImportResponse(res) {
      let data = (res && res.data !== undefined) ? res.data : res
      if (typeof data === 'string') {
        try {
          data = JSON.parse(data)
        } catch (e) {
          // 非 JSON 响应, 原样返回
        }
      }
      if (data && data.status === 'success' && data.data !== undefined) {
        return data.data
      }
      if (data && data.status === 'error') {
        handleApiError(data, data.message || '接口返回错误')
        throw createApiError(String(data.message || '接口返回错误'))
      }
      return data || {}
    },
    async doImport() {
      let groups
      try {
        groups = this.parseImportGroups(this.importDialog.text)
      } catch (e) {
        this.$message.error(e.message || String(e))
        return
      }
      try {
        await this.$confirm('确认导入 ' + groups.length + ' 个安全组？', '提示', { type: 'warning' })
      } catch (e) {
        return
      }
      this.importDialog.loading = true
      this.importDialog.result = ''
      this.importDialog.resultData = null
      this.importDialog.resultSuccess = false
      this.importDialog.summary = ''
      this.importDialog.completed = 0
      this.importDialog.totalCount = groups.length
      const runId = ++this.importRunId
      await loadLanguageConfig()
      if (runId !== this.importRunId) return
      const batchSize = this.importDialog.batchImport
        ? Math.max(1, Number(this.importDialog.batchSize) || 50)
        : Math.max(1, groups.length)
      const params = { _action: 'import', _langcode: getDefaultLangcode() }
      if (this.importDialog.dmDeploySet) {
        params.dmDeploySet = 'true'
      }
      const merged = {
        total: 0,
        added: 0,
        updated: 0,
        deleted: 0,
        failed: 0,
        results: [],
        'sks:successSumTotal': 0,
        'sks:failSumTotal': 0
      }
      let completed = 0
      try {
        for (let offset = 0; offset < groups.length; offset += batchSize) {
          if (runId !== this.importRunId) return
          const batch = groups.slice(offset, offset + batchSize)
          this.importDialog.summary = '正在导入 ' + (offset + batch.length) + '/' + groups.length + ' 组...'
          const res = await migrateSecurGroup(params, { groups: batch })
          if (runId !== this.importRunId) return
          const payload = this.extractImportResponse(res)
          merged.total += Number(payload.total || batch.length)
          merged.added += Number(payload.added || 0)
          merged.updated += Number(payload.updated || 0)
          merged.deleted += Number(payload.deleted || 0)
          merged.failed += Number(payload.failed || 0)
          merged['sks:successSumTotal'] += Number(payload['sks:successSumTotal'] || 0)
          merged['sks:failSumTotal'] += Number(payload['sks:failSumTotal'] || 0)
          if (Array.isArray(payload.results)) {
            merged.results = merged.results.concat(payload.results)
          }
          completed += batch.length
          this.importDialog.completed = completed
        }
        const failed = merged.failed !== undefined
          ? merged.failed
          : merged.results.filter(item => item && item._status !== 'success').length
        this.importDialog.resultData = merged
        this.importDialog.result = JSON.stringify(merged, null, 2)
        this.importDialog.resultSuccess = failed === 0
        this.importDialog.summary = '共 ' + (merged.total !== undefined ? merged.total : groups.length) +
          ' 组：新增 ' + (merged.added || 0) + '，更新 ' + (merged.updated || 0) +
          '，删除 ' + (merged.deleted || 0) + '，失败 ' + failed
        if (this.importDialog.resultSuccess) {
          this.$message.success(this.importDialog.summary)
        } else {
          this.$message.warning(this.importDialog.summary)
        }
        this.handleQuery()
      } catch (err) {
        if (runId !== this.importRunId) return
        const message = (err && err.message) ? err.message : String(err)
        this.importDialog.resultData = merged
        this.importDialog.result = JSON.stringify(Object.assign({}, merged, { error: message }), null, 2)
        this.importDialog.resultSuccess = false
        this.importDialog.summary = '已导入 ' + completed + '/' + groups.length + ' 组，后续失败：' + message
        if (!err || !err.isApiError) {
          this.$message.error('导入失败: ' + message)
        }
      } finally {
        if (runId === this.importRunId) {
          this.importDialog.loading = false
        }
      }
    },
    openImportResult() {
      this.importResultDialog.activeTab = 'list'
      this.importResultDialog.visible = true
    },
    copyImportRawJson() {
      const text = this.importDialog.result || ''
      if (!text) {
        this.$message.warning('没有可复制的原始 JSON')
        return
      }
      if (navigator.clipboard && window.isSecureContext) {
        navigator.clipboard.writeText(text).then(() => {
          this.$message.success('原始 JSON 已复制')
        }).catch(() => {
          this.copyTextByTextarea(text)
        })
      } else {
        this.copyTextByTextarea(text)
      }
    },
    copyTextByTextarea(text) {
      const textarea = document.createElement('textarea')
      textarea.value = text
      textarea.setAttribute('readonly', 'readonly')
      textarea.style.position = 'fixed'
      textarea.style.top = '-9999px'
      document.body.appendChild(textarea)
      textarea.select()
      let copied = false
      try {
        copied = document.execCommand('copy')
      } catch (e) {
        copied = false
      }
      document.body.removeChild(textarea)
      if (copied) {
        this.$message.success('原始 JSON 已复制')
      } else {
        this.$message.error('复制失败，请切换到原始JSON标签手动复制')
      }
    },
    statusTagType(status) {
      if (status === 'success') return 'success'
      if (status === 'failed') return 'danger'
      return 'info'
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
.subtable-filter-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.subtable-filter-bar .filter-count {
  margin-left: 4px;
  color: #909399;
  font-size: 12px;
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
.ml-empty {
  color: #c0c4cc;
  font-style: italic;
}
</style>

<style lang="scss">
/* 弹窗被挂载到 body, 需用非 scoped 样式 */
.import-dialog .el-dialog__body {
  max-height: 74vh;
  overflow-y: auto;
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
</style>
