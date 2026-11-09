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
          <el-button type="primary" icon="el-icon-s-operation" size="mini" @click="openPermissionExport">自定义导出</el-button>
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
            <!-- 应用授权: 左侧应用 / 右侧权限 -->
            <div v-if="rel.key === 'APPLICATIONAUTH'" class="appauth-layout">
              <div class="appauth-left">
                <div class="subtable-filter-bar">
                  <el-input v-model="appAuthKeyword" placeholder="应用/描述过滤" size="mini" clearable style="width:180px;" />
                  <el-button size="mini" icon="el-icon-refresh-left" @click="clearAppAuthSelection">清除选中</el-button>
                  <span class="filter-count">{{ filteredAppAuthApps.length }} 个应用</span>
                </div>
                <el-table
                  ref="appAuthLeftTable"
                  :data="filteredAppAuthApps"
                  border
                  stripe
                  size="mini"
                  max-height="380"
                  highlight-current-row
                  row-key="APP"
                  @row-click="handleAppAuthAppClick"
                >
                  <el-table-column prop="APP" label="应用" min-width="140" show-overflow-tooltip />
                  <el-table-column prop="description" label="描述" min-width="150" show-overflow-tooltip />
                  <el-table-column prop="count" label="权限数" width="76" align="center" />
                </el-table>
              </div>
              <div class="appauth-right">
                <div class="subtable-filter-bar">
                  <el-input v-model="authPermKeyword" placeholder="权限过滤（全字段）" size="mini" clearable style="width:200px;" />
                  <span class="filter-count">
                    {{ selectedAppAuth ? '应用 ' + selectedAppAuth : '全部应用' }}：
                    显示 {{ appAuthPermRows.length }} / {{ subTables.APPLICATIONAUTH.rows.length }} 条，点击行查看权限详情
                  </span>
                  <el-button
                    v-if="selectedAppAuth"
                    type="primary"
                    size="mini"
                    icon="el-icon-document-add"
                    :loading="appAuthJsonLoading"
                    @click="generateAppAuthJson"
                  >生成 JSON</el-button>
                  <span class="flex-spacer" />
                  <el-tooltip :content="showAllAuthColumns ? '隐藏多余列' : '显示所有列'" placement="top">
                    <el-button size="mini" circle :type="showAllAuthColumns ? 'success' : 'info'" icon="el-icon-menu" @click="showAllAuthColumns = !showAllAuthColumns" />
                  </el-tooltip>
                  <el-tooltip :content="showAuthPropName ? '隐藏属性名' : '显示属性名'" placement="top">
                    <el-button size="mini" circle :type="showAuthPropName ? 'success' : 'info'" icon="el-icon-s-flag" @click="showAuthPropName = !showAuthPropName" />
                  </el-tooltip>
                </div>
                <el-table :data="appAuthPermRows" border stripe size="mini" max-height="380" highlight-current-row @row-click="openAuthDetail">
                  <el-table-column
                    v-for="col in appAuthVisibleColumns"
                    :key="col + '-' + showAuthPropName"
                    :prop="col"
                    min-width="140"
                    show-overflow-tooltip
                  >
                    <template slot="header">
                      <span>{{ authColLabel(col) }}</span>
                      <template v-if="showAuthPropName">
                        <el-tag size="mini" class="prop-name-tag" @click.native="copyToClipboard(col, '属性名 ' + col)">{{ col }}</el-tag>
                        <el-tooltip content="显示更多配置" placement="top">
                          <el-button size="mini" circle icon="el-icon-search" class="prop-search-btn" @click.stop="showAuthColInfo(col)" />
                        </el-tooltip>
                      </template>
                    </template>
                    <template slot-scope="scope">
                      <span :class="{'ml-empty': isEmptyCell(scope.row[col])}">{{ cellText(scope.row[col]) }}</span>
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </div>
            <template v-else>
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
            </template>
          </el-tab-pane>
        </el-tabs>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">关 闭</el-button>
      </span>
    </el-dialog>

    <!-- 应用授权 - 权限详情弹窗 -->
    <el-dialog
      :title="authDetailTitle"
      :visible.sync="authDetailVisible"
      width="920px"
      top="8vh"
      append-to-body
      :close-on-click-modal="true"
    >
      <template v-if="authDetailRow">
        <el-descriptions :column="2" border size="small">
          <template v-for="col in appAuthPermColumns">
            <el-descriptions-item v-if="col !== 'CONDITIONNUM'" :key="col" :label="authColLabel(col)">
              <span :class="{'ml-empty': isEmptyCell(authDetailRow[col])}">{{ cellText(authDetailRow[col]) }}</span>
            </el-descriptions-item>
          </template>
          <el-descriptions-item v-if="!isEmptyCell(authDetailRow.CONDITIONNUM)" label="条件编号 (CONDITIONNUM)">
            <span>{{ authDetailRow.CONDITIONNUM }}</span>
            <el-tooltip content="查看条件详情" placement="top">
              <el-button type="text" size="small" class="condition-eye-btn" @click="openConditionDetail">
                <svg-icon icon-class="eye-open" />
              </el-button>
            </el-tooltip>
          </el-descriptions-item>
          <el-descriptions-item v-if="!isEmptyCell(authDetailRow.CONDITIONNUM)" label="条件描述">
            <span :class="{'ml-empty': !authConditionDescription}">{{ authConditionDescription || '-' }}</span>
          </el-descriptions-item>
          <el-descriptions-item v-if="!isEmptyCell(authDetailRow.CONDITIONNUM)" label="条件表达式 (EXPRESSION)">
            <span class="condition-expression">{{ authConditionExpression || '-' }}</span>
          </el-descriptions-item>
        </el-descriptions>
        <el-tabs v-model="authDetailTab" type="border-card" class="json-tabs">
          <el-tab-pane label="原始JSON" name="json" lazy>
            <div class="json-toolbar">
              <el-button type="primary" size="mini" icon="el-icon-document-copy" @click="copyToClipboard(authDetailJson, '权限JSON')">复制 JSON</el-button>
            </div>
            <monaco-json-editor :value="authDetailJson" read-only height="300px" />
          </el-tab-pane>
        </el-tabs>
      </template>
      <span slot="footer" class="dialog-footer">
        <el-button @click="authDetailVisible = false">关 闭</el-button>
      </span>
    </el-dialog>

    <!-- 应用授权 - 列配置信息弹窗 -->
    <el-dialog title="列配置信息" :visible.sync="authColInfoDialog.visible" width="700px" top="10vh" append-to-body :close-on-click-modal="true">
      <el-table :data="authColInfoRows" border stripe size="small">
        <el-table-column prop="key" label="配置项" width="180" />
        <el-table-column prop="value" label="值" min-width="300" show-overflow-tooltip />
      </el-table>
      <span slot="footer" class="dialog-footer">
        <el-button @click="authColInfoDialog.visible = false">关 闭</el-button>
      </span>
    </el-dialog>

    <!-- 应用授权 - 条件详情弹窗 -->
    <el-dialog :title="conditionDetailTitle" :visible.sync="conditionDetailVisible" width="820px" top="10vh" append-to-body :close-on-click-modal="true">
      <template v-if="conditionDetailRow">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item v-for="col in conditionDetailColumns" :key="col" :label="col">
            <span :class="{'ml-empty': isEmptyCell(conditionDetailRow[col]), 'condition-expression': col === 'EXPRESSION'}">{{ cellText(conditionDetailRow[col]) }}</span>
          </el-descriptions-item>
        </el-descriptions>
        <div class="json-toolbar" style="margin-top: 12px;">
          <el-button type="primary" size="mini" icon="el-icon-document-copy" @click="copyToClipboard(conditionDetailJson, '条件JSON')">复制 JSON</el-button>
        </div>
        <monaco-json-editor :value="conditionDetailJson" read-only height="260px" />
      </template>
      <span slot="footer" class="dialog-footer">
        <el-button @click="conditionDetailVisible = false">关 闭</el-button>
      </span>
    </el-dialog>

    <!-- 应用授权 - 按应用生成导出 JSON -->
    <el-dialog
      :title="'生成单应用权限 JSON - ' + (currentRow ? currentRow.GROUPNAME : '') + ' / ' + selectedAppAuth"
      :visible.sync="appAuthJsonVisible"
      width="900px"
      top="6vh"
      append-to-body
      :close-on-click-modal="true"
      @open="loadCopyGroupOptions"
    >
      <div class="json-toolbar" style="display:flex; justify-content:space-between; align-items:center; gap:8px;">
        <div class="toolbar-left">
          <span style="font-size:12px;color:#606266;">权限复制到安全组（选中后点「刷新」，按安全组名复制多份）：</span>
          <el-select
            v-model="copyToGroups"
            multiple
            filterable
            size="mini"
            placeholder="不复制，仅显示当前安全组"
            style="width:340px;"
          >
            <el-option
              v-for="g in copyGroupOptions"
              :key="g.GROUPNAME"
              :label="g.GROUPNAME + (g.DESCRIPTION ? ' - ' + g.DESCRIPTION : '')"
              :value="g.GROUPNAME"
            />
          </el-select>
          <el-button size="mini" icon="el-icon-refresh" @click="buildAppAuthJson">刷 新</el-button>
        </div>
        <div class="toolbar-right">
          <span style="font-size:12px;color:#606266;">源数据：{{ appAuthSourceGroups.length }} 个安全组</span>
          <el-button type="primary" size="mini" icon="el-icon-document-copy" @click="copyToClipboard(appAuthJson, '应用授权JSON')">复制 JSON</el-button>
        </div>
      </div>
      <monaco-json-editor :value="appAuthJson" read-only height="60vh" />
      <span slot="footer" class="dialog-footer">
        <el-button @click="appAuthJsonVisible = false">关 闭</el-button>
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
          <monaco-json-editor :value="batchJson" read-only height="40vh" />
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
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="启用迁移模式">
              <el-checkbox v-model="importDialog.dmDeploySet">dmDeploySet（MboSet 调用 setDMDeploySet(true)）</el-checkbox>
            </el-form-item>
          </el-col>
          <el-col :span="12">
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
          </el-col>
          <el-col :span="12">
            <div class="param-block">
              <p class="ignore-hint">选中的根节点不参与导入</p>
              <el-form-item label="忽略导入">
                <el-select
                  v-model="importDialog.ignoreList"
                  multiple
                  size="small"
                  placeholder="不忽略"
                  style="width:100%;"
                >
                  <el-option
                    v-for="opt in importIgnoreOptions"
                    :key="opt.value"
                    :label="opt.label"
                    :value="opt.value"
                  />
                </el-select>
              </el-form-item>
            </div>
          </el-col>
          <el-col :span="12">
            <div class="param-block">
              <p class="ignore-hint">按 APP 同步：目标中存在但导入 JSON 中不存在的权限将被删除</p>
              <el-form-item label="不存在则删除">
                <el-select
                  v-model="importDialog.deleteNotExistsList"
                  multiple
                  size="small"
                  placeholder="不删除"
                  style="width:100%;"
                >
                  <el-option
                    v-for="opt in deleteNotExistsOptions"
                    :key="opt.value"
                    :label="opt.label"
                    :value="opt.value"
                  />
                </el-select>
              </el-form-item>
            </div>
          </el-col>
        </el-row>
      </el-form>
      <monaco-json-editor
        v-model="importDialog.text"
        height="280px"
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
            <monaco-json-editor :value="importDialog.result" read-only height="46vh" />
          </el-tab-pane>
        </el-tabs>
      </template>
      <monaco-json-editor v-else :value="importDialog.result" read-only height="46vh" />
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
import MonacoJsonEditor from '../../../components/MonacoJsonEditor.vue'

export default {
  name: 'SecurGroupQuery',
  components: { MonacoJsonEditor },
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
      appAuthKeyword: '',
      authPermKeyword: '',
      selectedAppAuth: '',
      appAuthJsonVisible: false,
      appAuthJson: '',
      appAuthJsonLoading: false,
      // 源数据缓存(未复制前) 与「权限复制到安全组」
      appAuthSourceGroups: [],
      copyToGroups: [],
      copyGroupOptions: [],
      showAllAuthColumns: false,
      showAuthPropName: false,
      // advanced: true 的列仅在「显示所有列」时展示
      authAdvancedColumns: ['APP', 'APPLICATIONAUTHID', 'GROUPNAME'],
      authColInfoDialog: {
        visible: false,
        col: null
      },
      authDetailVisible: false,
      authDetailRow: null,
      authDetailTab: 'json',
      conditionDetailVisible: false,
      conditionDetailRow: null,
      detailConditions: [],
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
        ignoreList: [],
        deleteNotExistsList: [],
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
        // 子级键统一为 _skschild_ 前缀, 兼容旧的无前缀格式
        const childRows = src['_skschild_' + rel.key]
        const rawRows = Array.isArray(childRows)
          ? childRows
          : (Array.isArray(src[rel.key]) ? src[rel.key] : [])
        // APPLICATIONAUTH 导出为按 APP 聚合格式, 展示时拍平为权限行
        const flatRows = rel.key === 'APPLICATIONAUTH' ? this.flattenAppAuthRows(rawRows) : rawRows
        const colSet = {}
        const columns = []
        for (let j = 0; j < flatRows.length; j++) {
          const row = flatRows[j] || {}
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
        // 聚合拍平后 APP 来自包装行, 保持 APP 列在最前
        if (rel.key === 'APPLICATIONAUTH') {
          const appIdx = columns.indexOf('APP')
          if (appIdx > 0) {
            columns.splice(appIdx, 1)
            columns.unshift('APP')
          }
        }
        let rows = flatRows
        if (rel.filters && this.subFilterValues[rel.key]) {
          const fv = this.subFilterValues[rel.key]
          rows = flatRows.filter(row => rel.filters.every(f => {
            const v = fv[f.key]
            if (!v) return true
            const cell = (row[f.key] === null || row[f.key] === undefined) ? '' : String(row[f.key])
            if (f.type === 'text') {
              return cell.toLowerCase().indexOf(String(v).toLowerCase()) >= 0
            }
            return cell === String(v)
          }))
        }
        result[rel.key] = { rows, columns, totalRows: flatRows.length }
      }
      return result
    },
    // 左侧应用列表: 按 APP 聚合, 取应用描述与权限条数
    appAuthApps() {
      const src = this.detailRow || {}
      const childRows = src['_skschild_APPLICATIONAUTH']
      const rows = Array.isArray(childRows)
        ? childRows
        : (Array.isArray(src.APPLICATIONAUTH) ? src.APPLICATIONAUTH : [])
      const map = {}
      const order = []
      for (let i = 0; i < rows.length; i++) {
        const row = rows[i] || {}
        const app = (row.APP === null || row.APP === undefined) ? '' : String(row.APP)
        if (!app) continue
        // 聚合格式: [ { APP, APPLICATIONAUTH:[权限行] } ]; 旧格式: 每行一个权限
        const nested = row.APPLICATIONAUTH || row.applicationauth
        const count = Array.isArray(nested) ? nested.length : 1
        if (!map[app]) {
          map[app] = {
            APP: app,
            description: row['sks:appdescription'] || '',
            count: 0
          }
          order.push(app)
        } else if (!map[app].description && row['sks:appdescription']) {
          map[app].description = row['sks:appdescription']
        }
        map[app].count += count
      }
      return order.sort().map(app => map[app])
    },
    filteredAppAuthApps() {
      const kw = String(this.appAuthKeyword || '').trim().toLowerCase()
      if (!kw) return this.appAuthApps
      return this.appAuthApps.filter(app => {
        return String(app.APP).toLowerCase().indexOf(kw) >= 0 ||
          String(app.description).toLowerCase().indexOf(kw) >= 0
      })
    },
    appAuthPermColumns() {
      const table = this.subTables.APPLICATIONAUTH
      return table ? table.columns : []
    },
    appAuthPermRows() {
      const table = this.subTables.APPLICATIONAUTH
      const rows = table ? table.rows : []
      const kw = String(this.authPermKeyword || '').trim().toLowerCase()
      return rows.filter(row => {
        if (!row) return false
        if (this.selectedAppAuth && String(row.APP) !== String(this.selectedAppAuth)) {
          return false
        }
        if (!kw) return true
        return Object.keys(row).some(key => {
          const v = row[key]
          return v !== null && v !== undefined && String(v).toLowerCase().indexOf(kw) >= 0
        })
      })
    },
    appAuthVisibleColumns() {
      return this.showAllAuthColumns
        ? this.appAuthPermColumns
        : this.appAuthPermColumns.filter(col => this.authAdvancedColumns.indexOf(col) < 0)
    },
    authDetailTitle() {
      const row = this.authDetailRow
      if (!row) return '权限详情'
      return '权限详情 - ' + (row.APP || '') + ' / ' + (row.OPTIONNAME || '')
    },
    authDetailJson() {
      return this.authDetailRow ? JSON.stringify(this.authDetailRow, null, 2) : ''
    },
    // 权限行 CONDITIONNUM 对应的条件(来自详情返回的 conditionList)
    authCondition() {
      const row = this.authDetailRow
      if (!row || row.CONDITIONNUM === null || row.CONDITIONNUM === undefined || row.CONDITIONNUM === '') {
        return null
      }
      const num = String(row.CONDITIONNUM)
      return (this.detailConditions || []).find(item => item && String(item.CONDITIONNUM) === num) || null
    },
    authConditionDescription() {
      return this.authCondition ? (this.authCondition.DESCRIPTION || '') : ''
    },
    authConditionExpression() {
      return this.authCondition ? (this.authCondition.EXPRESSION || '') : ''
    },
    conditionDetailTitle() {
      const row = this.conditionDetailRow
      return row ? ('条件详情 - ' + (row.CONDITIONNUM || '')) : '条件详情'
    },
    conditionDetailColumns() {
      return ['CONDITIONNUM', 'TYPE', 'CLASSNAME', 'DESCRIPTION', 'EXPRESSION', 'NOCACHING']
    },
    conditionDetailJson() {
      return this.conditionDetailRow ? JSON.stringify(this.conditionDetailRow, null, 2) : ''
    },
    authColInfoRows() {
      const col = this.authColInfoDialog.col
      if (!col) return []
      const src = this.detailRow || {}
      const childRows = src['_skschild_APPLICATIONAUTH']
      const rows = this.flattenAppAuthRows(Array.isArray(childRows)
        ? childRows
        : (Array.isArray(src.APPLICATIONAUTH) ? src.APPLICATIONAUTH : []))
      let sample = ''
      for (let i = 0; i < rows.length; i++) {
        const value = rows[i] ? rows[i][col] : ''
        if (value !== null && value !== undefined && value !== '') {
          sample = String(value)
          break
        }
      }
      return [
        { key: '属性名 (prop)', value: col },
        { key: '显示名 (label)', value: this.authColLabel(col) },
        { key: '渲染类型', value: '文本' },
        { key: '高级列', value: this.authAdvancedColumns.indexOf(col) >= 0 ? '是（仅「显示所有列」时展示）' : '否' },
        { key: '示例值', value: sample || '-' }
      ]
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
    },
    importIgnoreOptions() {
      return [
        { label: 'conditionList（关联条件）', value: 'conditionList' }
      ]
    },
    deleteNotExistsOptions() {
      return [
        {
          label: '应用权限 APPLICATIONAUTH（按 APP 同步删除不存在行）',
          value: 'maxgroupList:_skschild_APPLICATIONAUTH:APPLICATIONAUTH'
        }
      ]
    }
  },
  watch: {
    dialogVisible(val) {
      if (!val) {
        this.authDetailVisible = false
        this.authColInfoDialog.visible = false
        this.conditionDetailVisible = false
        this.conditionDetailRow = null
      }
    },
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
    authColLabel(col) {
      return this.colLabelMap[col] || col
    },
    isEmptyCell(value) {
      return value === null || value === undefined || value === ''
    },
    // APPLICATIONAUTH 聚合格式 [ { APP, APPLICATIONAUTH:[...] } ] 拍平为权限行, 兼容旧扁平格式
    flattenAppAuthRows(list) {
      const flat = []
      const rows = Array.isArray(list) ? list : []
      rows.forEach(item => {
        if (!item || typeof item !== 'object') return
        const nested = item.APPLICATIONAUTH || item.applicationauth
        if (!Array.isArray(nested)) {
          flat.push(item)
          return
        }
        nested.forEach(perm => {
          if (!perm || typeof perm !== 'object') return
          const row = Object.assign({}, perm)
          // 以聚合包装行的 APP 为准
          if (!this.isEmptyCell(item.APP)) {
            row.APP = item.APP
          }
          flat.push(row)
        })
      })
      return flat
    },
    cellText(value) {
      return this.isEmptyCell(value) ? '-' : String(value)
    },
    handleAppAuthAppClick(row) {
      if (!row) return
      this.selectedAppAuth = String(row.APP)
    },
    clearAppAuthSelection() {
      this.selectedAppAuth = ''
      if (this.$refs.appAuthLeftTable) {
        this.$refs.appAuthLeftTable.setCurrentRow()
      }
    },
    // 按当前安全组 + 当前选中应用生成导出 JSON（_apiMode=appauth，忽略主信息与条件）
    async generateAppAuthJson() {
      const groupname = this.currentRow && this.currentRow.GROUPNAME ? String(this.currentRow.GROUPNAME) : ''
      const appname = this.selectedAppAuth ? String(this.selectedAppAuth) : ''
      if (!groupname || !appname) {
        this.$message.warning('请先点击左侧应用行')
        return
      }
      this.appAuthJsonLoading = true
      try {
        await loadLanguageConfig()
        const body = {
          _apiMode: 'appauth',
          groupnames: [groupname],
          appnames: [appname],
          ignoreMain: true,
          _ignoreList: ['conditionList']
        }
        const res = await migrateSecurGroup({
          _action: 'export',
          _langcode: getDefaultLangcode()
        }, body)
        const data = res.data || res
        if (data && data.status === 'error') {
          handleApiError(data, '生成 JSON 失败: ' + (data.message || '未知错误'))
          return
        }
        const payload = data.data || data
        const groups = (payload && (payload.maxgroupList || payload.groups)) || []
        if (!groups.length) {
          this.$message.warning('未查询到应用授权数据')
          return
        }
        // 缓存源数据, 再由源数据 + 复制目标安全组生成显示 JSON
        this.appAuthSourceGroups = groups
        this.copyToGroups = []
        this.buildAppAuthJson()
        this.appAuthJsonVisible = true
      } catch (err) {
        const respData = err.response && err.response.data
        handleApiError(respData && typeof respData === 'object' ? respData : null, '生成 JSON 失败: ' + (err.message || String(err)))
      } finally {
        this.appAuthJsonLoading = false
      }
    },
    cloneJson(value) {
      return JSON.parse(JSON.stringify(value))
    },
    // 源数据 + 「权限复制到安全组」→ 按安全组名替换 GROUPNAME 复制多份后刷新 JSON
    buildAppAuthJson() {
      const source = Array.isArray(this.appAuthSourceGroups) ? this.appAuthSourceGroups : []
      const targets = (Array.isArray(this.copyToGroups) ? this.copyToGroups : []).filter(item => !!item)
      let groups
      if (!targets.length) {
        groups = this.cloneJson(source)
      } else {
        const optionMap = {}
        this.copyGroupOptions.forEach(item => {
          if (item && item.GROUPNAME) {
            optionMap[String(item.GROUPNAME)] = item
          }
        })
        groups = []
        targets.forEach(name => {
          const target = optionMap[String(name)]
          source.forEach(group => {
            const copy = this.cloneJson(group)
            copy.GROUPNAME = String(name)
            // 描述同步为目标安全组的描述
            if (target) {
              copy.DESCRIPTION = target.DESCRIPTION || ''
            }
            groups.push(copy)
          })
        })
      }
      this.appAuthJson = JSON.stringify({ maxgroupList: groups }, null, 2)
    },
    async loadCopyGroupOptions() {
      if (this.copyGroupOptions.length) return
      try {
        const res = await querySecurGroup({
          _action: 'query',
          _langcode: getDefaultLangcode(),
          pageNum: 1,
          pageSize: 1000
        })
        const data = res.data || res
        if (data && data.status === 'error') {
          handleApiError(data, '查询安全组失败: ' + (data.message || '未知错误'))
          return
        }
        const payload = data.data || data
        this.copyGroupOptions = (payload && (payload.maxgroupList || payload.groups)) || []
      } catch (err) {
        console.error('查询安全组失败', err)
      }
    },
    openAuthDetail(row) {
      if (!row) return
      this.authDetailRow = row
      this.authDetailTab = 'json'
      this.authDetailVisible = true
    },
    showAuthColInfo(col) {
      this.authColInfoDialog.col = col
      this.authColInfoDialog.visible = true
    },
    openConditionDetail() {
      const row = this.authDetailRow
      const num = row && !this.isEmptyCell(row.CONDITIONNUM) ? String(row.CONDITIONNUM) : ''
      if (!num) return
      const cond = (this.detailConditions || []).find(item => item && String(item.CONDITIONNUM) === num)
      if (!cond) {
        this.$message.warning('未找到条件 ' + num + ' 的详情')
        return
      }
      this.conditionDetailRow = cond
      this.conditionDetailVisible = true
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
        const list = payload.maxgroupList || payload.groups || []
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
      this.appAuthKeyword = ''
      this.authPermKeyword = ''
      this.selectedAppAuth = ''
      this.detailConditions = []
      this.authDetailVisible = false
      this.authDetailRow = null
      this.authColInfoDialog.visible = false
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
        const list = (payload && (payload.maxgroupList || payload.groups)) || []
        this.detailRow = list.length ? list[0] : null
        this.detailConditions = (payload && Array.isArray(payload.conditionList)) ? payload.conditionList : []
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
    openPermissionExport() {
      this.$router.push({ name: 'SecurGroupPermissionExport' })
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
        const list = (payload && (payload.maxgroupList || payload.groups)) || []
        const conditions = (payload && Array.isArray(payload.conditionList)) ? payload.conditionList : []
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
        this.batchJson = JSON.stringify({ conditionList: conditions, maxgroupList: list }, null, 2)
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
      this.importDialog.batchImport = false
      this.importDialog.batchSize = 50
      this.importDialog.ignoreList = ['conditionList']
      this.importDialog.deleteNotExistsList = []
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
          const payload = this.parseImportPayload(text)
          const ignoreConditions = (this.importDialog.ignoreList || []).indexOf('conditionList') >= 0
          this.$message.success('已读取 ' + payload.groups.length + ' 个安全组' +
            (!ignoreConditions && payload.conditions.length ? '、' + payload.conditions.length + ' 个条件' : '') +
            '，确认后点击“导 入”')
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
    parseImportPayload(text) {
      let parsed
      try {
        parsed = JSON.parse(text)
      } catch (e) {
        throw new Error('JSON 解析失败: ' + e.message)
      }
      let groups
      let conditions = []
      if (Array.isArray(parsed)) {
        groups = parsed
      } else if (parsed && Array.isArray(parsed.maxgroupList)) {
        groups = parsed.maxgroupList
      } else if (parsed && Array.isArray(parsed.groups)) {
        groups = parsed.groups
      } else if (parsed && parsed.GROUPNAME) {
        groups = [parsed]
      }
      if (parsed && !Array.isArray(parsed) && Array.isArray(parsed.conditionList)) {
        conditions = parsed.conditionList
      }
      if (!groups || groups.length === 0) {
        throw new Error('未找到可导入的安全组数据（需要 maxgroupList 数组）')
      }
      return { groups, conditions }
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
      let conditions
      const ignoreList = this.importDialog.ignoreList || []
      try {
        const payload = this.parseImportPayload(this.importDialog.text)
        groups = payload.groups
        conditions = payload.conditions
        // _ignoreList 中的根节点不参与导入
        if (ignoreList.indexOf('conditionList') >= 0) {
          conditions = []
        }
      } catch (e) {
        this.$message.error(e.message || String(e))
        return
      }
      try {
        await this.$confirm('确认导入 ' + groups.length + ' 个安全组' +
          (conditions.length ? '、' + conditions.length + ' 个条件' : '') + '？', '提示', { type: 'warning' })
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
      let conditionStat = null
      let completed = 0
      try {
        for (let offset = 0; offset < groups.length; offset += batchSize) {
          if (runId !== this.importRunId) return
          const batch = groups.slice(offset, offset + batchSize)
          this.importDialog.summary = '正在导入 ' + (offset + batch.length) + '/' + groups.length + ' 组...'
          const body = { maxgroupList: batch }
          if (ignoreList.length) {
            body._ignoreList = ignoreList.slice()
          }
          const deleteNotExistsList = this.importDialog.deleteNotExistsList || []
          if (deleteNotExistsList.length) {
            body._deleteNotExists = deleteNotExistsList.slice()
          }
          // 条件只在首批导入, 避免重复 upsert
          if (offset === 0 && conditions.length) {
            body.conditionList = conditions
          }
          const res = await migrateSecurGroup(params, body)
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
          if (offset === 0 && payload.conditions) {
            conditionStat = payload.conditions
            merged.conditions = conditionStat
          }
          completed += batch.length
          this.importDialog.completed = completed
        }
        const failed = merged.failed !== undefined
          ? merged.failed
          : merged.results.filter(item => item && item._status !== 'success').length
        this.importDialog.resultData = merged
        this.importDialog.result = JSON.stringify(merged, null, 2)
        this.importDialog.resultSuccess = failed === 0 && (!conditionStat || !conditionStat.failed)
        this.importDialog.summary = '共 ' + (merged.total !== undefined ? merged.total : groups.length) +
          ' 组：新增 ' + (merged.added || 0) + '，更新 ' + (merged.updated || 0) +
          '，删除 ' + (merged.deleted || 0) + '，失败 ' + failed +
          (conditionStat && conditionStat.total
            ? '；条件：新增 ' + (conditionStat.added || 0) + '，更新 ' + (conditionStat.updated || 0) +
              '，删除 ' + (conditionStat.deleted || 0) + '，失败 ' + (conditionStat.failed || 0)
            : '')
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
.appauth-layout {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}
.appauth-left {
  width: 360px;
  flex: 0 0 360px;
}
.appauth-right {
  flex: 1;
  min-width: 0;
}
.flex-spacer {
  flex: 1;
}
.prop-name-tag {
  margin-left: 6px;
  cursor: pointer;
}
.prop-search-btn {
  margin-left: 4px;
}
.condition-eye-btn {
  margin-left: 4px;
  padding: 0;
  vertical-align: middle;
}
.condition-expression {
  display: block;
  font-family: Consolas, Monaco, monospace;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-all;
}
.json-toolbar {
  margin-bottom: 8px;
  text-align: right;
}
.param-block {
  margin-bottom: 18px;
}
.param-block .el-form-item {
  margin-bottom: 0;
}
.param-block .ignore-hint {
  margin-left: 110px;
}
.ignore-hint {
  margin: 0 0 2px;
  line-height: 1.3;
  color: #909399;
  font-size: 12px;
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
