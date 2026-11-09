<template>
  <section class="query-page">
    <el-card>
      <div class="page-header-row">
        <div>
          <h2>安全组应用权限导出</h2>
          <p class="page-summary">选择安全组与应用后生成可直接导入的 JSON，仅导出所选应用的应用授权 / 安全限制 / 控制组；不选安全组表示全部安全组，不选应用表示安全组的全部应用权限。</p>
        </div>
        <div class="header-actions">
          <el-button size="mini" icon="el-icon-back" @click="goBack">返回安全组查询</el-button>
        </div>
      </div>

      <div class="picker-grid">
        <!-- 安全组 -->
        <div class="picker-panel">
          <div class="picker-title">安全组（不选则导出全部）</div>
          <div class="picker-search">
            <el-input v-model="groupKeyword" size="mini" placeholder="组名模糊查询" clearable @keyup.enter.native="loadGroups" />
            <el-button size="mini" type="cyan" icon="el-icon-search" @click="loadGroups">查询</el-button>
            <el-button size="mini" icon="el-icon-plus" :disabled="!checkedGroups.length" @click="addGroups">添加到导出缓存</el-button>
          </div>
          <el-table
            v-loading="groupLoading"
            :data="groupPageList"
            size="mini"
            border
            stripe
            height="640"
            @selection-change="rows => { checkedGroups = rows }"
          >
            <el-table-column type="selection" width="46" />
            <el-table-column prop="GROUPNAME" label="组名" min-width="140" show-overflow-tooltip />
            <el-table-column prop="DESCRIPTION" label="描述" min-width="180" show-overflow-tooltip />
            <el-table-column label="操作" width="120" align="center">
              <template slot-scope="scope">
                <el-button
                  v-if="isGroupCached(scope.row.GROUPNAME)"
                  size="mini"
                  type="danger"
                  plain
                  icon="el-icon-delete"
                  @click="removeGroup(scope.row.GROUPNAME)"
                >删除缓存</el-button>
                <el-button
                  v-else
                  size="mini"
                  type="primary"
                  plain
                  icon="el-icon-plus"
                  @click="addGroupName(scope.row.GROUPNAME)"
                >添加到缓存</el-button>
              </template>
            </el-table-column>
          </el-table>
          <pagination
            v-show="groupPage.total > 0"
            :total="groupPage.total"
            :page.sync="groupPage.pageNum"
            :limit.sync="groupPage.pageSize"
          />
          <div class="cache-box">
            <span class="cache-label">将要导出的安全组（{{ groupNames.length }}）：</span>
            <el-tag
              v-for="name in groupNames"
              :key="name"
              size="small"
              closable
              class="cache-tag"
              @close="removeGroup(name)"
            >{{ name }}</el-tag>
            <span v-if="!groupNames.length" class="cache-empty">未选择，将导出全部安全组</span>
            <el-button v-if="groupNames.length" type="text" size="mini" @click="groupNames = []">清空</el-button>
          </div>
        </div>

        <!-- 应用 -->
        <div class="picker-panel">
          <div class="picker-title">应用（不选则导出安全组下全部应用）</div>
          <div class="picker-search">
            <el-input v-model="appKeyword" size="mini" placeholder="应用名模糊查询，如 SKS" clearable @keyup.enter.native="loadApps" />
            <el-button size="mini" type="cyan" icon="el-icon-search" @click="loadApps">查询</el-button>
            <el-button size="mini" icon="el-icon-plus" :disabled="!checkedApps.length" @click="addApps">添加到导出缓存</el-button>
          </div>
          <el-table
            v-loading="appLoading"
            :data="appPageList"
            size="mini"
            border
            stripe
            height="640"
            @selection-change="rows => { checkedApps = rows }"
          >
            <el-table-column type="selection" width="46" />
            <el-table-column prop="APP" label="应用" min-width="150" show-overflow-tooltip />
            <el-table-column prop="DESCRIPTION" label="描述" min-width="180" show-overflow-tooltip />
            <el-table-column label="操作" width="120" align="center">
              <template slot-scope="scope">
                <el-button
                  v-if="isAppCached(scope.row.APP)"
                  size="mini"
                  type="danger"
                  plain
                  icon="el-icon-delete"
                  @click="removeApp(scope.row.APP)"
                >删除缓存</el-button>
                <el-button
                  v-else
                  size="mini"
                  type="primary"
                  plain
                  icon="el-icon-plus"
                  @click="addAppName(scope.row.APP)"
                >添加到缓存</el-button>
              </template>
            </el-table-column>
          </el-table>
          <pagination
            v-show="appPage.total > 0"
            :total="appPage.total"
            :page.sync="appPage.pageNum"
            :limit.sync="appPage.pageSize"
          />
          <div class="cache-box">
            <span class="cache-label">将要导出的应用（{{ appNames.length }}）：</span>
            <el-tag
              v-for="name in appNames"
              :key="name"
              size="small"
              closable
              class="cache-tag"
              @close="removeApp(name)"
            >{{ name }}</el-tag>
            <span v-if="!appNames.length" class="cache-empty">未选择，将导出安全组的全部应用权限</span>
            <el-button v-if="appNames.length" type="text" size="mini" @click="appNames = []">清空</el-button>
          </div>
        </div>
      </div>

      <div class="generate-bar">
        <span class="generate-summary">
          {{ groupNames.length ? groupNames.length + ' 个安全组' : '全部安全组' }}
          ×
          {{ appNames.length ? appNames.length + ' 个应用' : '全部应用' }}
        </span>
        <el-button type="primary" size="mini" icon="el-icon-document-add" :loading="generating" @click="generateJson">生成 JSON</el-button>
      </div>
    </el-card>

    <!-- 生成结果 -->
    <el-dialog :title="'导出 JSON' + (exportGroups.length ? '（' + exportGroups.length + ' 个安全组）' : '')" :visible.sync="resultVisible" width="85%" top="5vh" :close-on-click-modal="false">
      <div class="json-toolbar">
        <div class="toolbar-left">
          <el-checkbox v-model="ignoreMain">忽略安全组主信息（导出加 _ignoreMain，修改后点「刷新」生效）</el-checkbox>
          <span class="generate-summary">忽略根节点（_ignoreList）：</span>
          <el-select
            v-model="ignoreList"
            multiple
            size="mini"
            placeholder="不忽略"
            style="width:220px;"
            @change="buildExportJson"
          >
            <el-option
              v-for="opt in ignoreListOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </div>
        <div class="toolbar-right">
          <span class="generate-summary">应用授权行：{{ authRowCount }}</span>
          <el-checkbox v-model="withUuid" @change="buildExportJson">每行设置UUID</el-checkbox>
          <el-button size="mini" icon="el-icon-refresh" :loading="generating" @click="generateJson">刷 新</el-button>
          <el-button type="primary" size="mini" icon="el-icon-document-copy" @click="copyJson">复制 JSON</el-button>
          <el-button type="primary" size="mini" icon="el-icon-download" @click="saveJson">保存 JSON</el-button>
        </div>
      </div>
      <monaco-json-editor :value="exportJson" read-only height="60vh" />
      <span slot="footer" class="dialog-footer">
        <el-button @click="resultVisible = false">关 闭</el-button>
      </span>
    </el-dialog>
  </section>
</template>

<script>
import { migrateSecurGroup, querySecurGroup, querySecurGroupApps } from '../../../api/securgroup'
import { getDefaultLangcode, loadLanguageConfig } from '../../../utils/mmp/mmp-config'
import { handleApiError } from '../../../utils/mmp/api-error'
import MonacoJsonEditor from '../../../components/MonacoJsonEditor.vue'

export default {
  name: 'SecurGroupPermissionExport',
  components: { MonacoJsonEditor },
  data() {
    return {
      groupKeyword: '',
      appKeyword: '',
      groupLoading: false,
      appLoading: false,
      generating: false,
      groupList: [],
      appList: [],
      groupPage: { pageNum: 1, pageSize: 20, total: 0 },
      appPage: { pageNum: 1, pageSize: 20, total: 0 },
      checkedGroups: [],
      checkedApps: [],
      // 导出缓存(可删除的 tag)
      groupNames: [],
      appNames: [],
      ignoreMain: true,
      resultVisible: false,
      withUuid: false,
      ignoreList: [],
      exportJson: '',
      exportGroups: [],
      exportConditions: [],
      authRowCount: 0
    }
  },
  computed: {
    groupPageList() {
      return this.slicePage(this.groupList, this.groupPage)
    },
    appPageList() {
      return this.slicePage(this.appList, this.appPage)
    },
    ignoreListOptions() {
      return [
        { label: 'conditionList（关联条件）', value: 'conditionList' }
      ]
    }
  },
  created() {
    loadLanguageConfig()
    // 顺序加载：两个接口都会用当前用户的同一个数据库连接，并发会触发 "Connection is closed"
    this.loadGroups().finally(() => {
      this.loadApps()
    })
  },
  methods: {
    slicePage(list, page) {
      const start = (page.pageNum - 1) * page.pageSize
      return list.slice(start, start + page.pageSize)
    },
    goBack() {
      this.$router.push({ name: 'SecurGroupQuery' })
    },
    loadGroups() {
      this.groupLoading = true
      return querySecurGroup({
        _action: 'query',
        _langcode: getDefaultLangcode(),
        groupname: this.groupKeyword || undefined,
        pageNum: 1,
        pageSize: 500
      }).then(res => {
        const data = res.data || res
        if (data && data.status === 'error') {
          handleApiError(data, '查询安全组失败: ' + (data.message || '未知错误'))
          return
        }
        const payload = data.data || data
        this.groupList = payload.maxgroupList || payload.groups || []
        this.groupPage.total = this.groupList.length
        this.groupPage.pageNum = 1
      }).catch(err => {
        const respData = err.response && err.response.data
        handleApiError(respData && typeof respData === 'object' ? respData : null, '查询安全组失败: ' + (err.message || String(err)))
      }).finally(() => {
        this.groupLoading = false
      })
    },
    loadApps() {
      this.appLoading = true
      return querySecurGroupApps({
        _langcode: getDefaultLangcode(),
        app: this.appKeyword || undefined
      }).then(res => {
        const data = res.data || res
        if (data && data.status === 'error') {
          handleApiError(data, '查询应用失败: ' + (data.message || '未知错误'))
          return
        }
        const payload = data.data || data
        this.appList = payload.apps || []
        this.appPage.total = this.appList.length
        this.appPage.pageNum = 1
      }).catch(err => {
        const respData = err.response && err.response.data
        handleApiError(respData && typeof respData === 'object' ? respData : null, '查询应用失败: ' + (err.message || String(err)))
      }).finally(() => {
        this.appLoading = false
      })
    },
    addGroups() {
      const selected = this.checkedGroups.map(row => row.GROUPNAME).filter(Boolean)
      this.groupNames = Array.from(new Set(this.groupNames.concat(selected)))
    },
    addApps() {
      const selected = this.checkedApps.map(row => row.APP).filter(Boolean)
      this.appNames = Array.from(new Set(this.appNames.concat(selected)))
    },
    isGroupCached(name) {
      return !!name && this.groupNames.indexOf(name) >= 0
    },
    addGroupName(name) {
      if (name && this.groupNames.indexOf(name) < 0) {
        this.groupNames.push(name)
      }
    },
    isAppCached(name) {
      return !!name && this.appNames.indexOf(name) >= 0
    },
    addAppName(name) {
      if (name && this.appNames.indexOf(name) < 0) {
        this.appNames.push(name)
      }
    },
    removeGroup(name) {
      this.groupNames = this.groupNames.filter(item => item !== name)
    },
    removeApp(name) {
      this.appNames = this.appNames.filter(item => item !== name)
    },
    async generateJson() {
      this.generating = true
      try {
        await loadLanguageConfig()
        const body = { _apiMode: 'appauth' }
        if (this.groupNames.length) {
          body.groupnames = this.groupNames.slice()
        }
        if (this.appNames.length) {
          body.appnames = this.appNames.slice()
        }
        if (this.ignoreMain) {
          body.ignoreMain = true
        }
        if (this.ignoreList.length) {
          body._ignoreList = this.ignoreList.slice()
        }
        const res = await migrateSecurGroup({
          _action: 'export',
          _langcode: getDefaultLangcode()
        }, body)
        const data = res.data || res
        if (data && data.status === 'error') {
          handleApiError(data, '导出失败: ' + (data.message || '未知错误'))
          return
        }
        const payload = data.data || data
        const groups = (payload && (payload.maxgroupList || payload.groups)) || []
        if (!groups.length) {
          this.$message.warning('未查询到可导出的安全组数据')
          return
        }
        this.exportGroups = groups
        this.exportConditions = (payload && Array.isArray(payload.conditionList)) ? payload.conditionList : []
        this.authRowCount = groups.reduce((sum, g) => {
          const rows = g['_skschild_APPLICATIONAUTH'] || g.APPLICATIONAUTH || []
          if (!Array.isArray(rows)) return sum
          return sum + rows.reduce((count, item) => {
            const nested = item && (item.APPLICATIONAUTH || item.applicationauth)
            return count + (Array.isArray(nested) ? nested.length : 1)
          }, 0)
        }, 0)
        this.buildExportJson()
        this.resultVisible = true
      } catch (err) {
        const respData = err.response && err.response.data
        handleApiError(respData && typeof respData === 'object' ? respData : null, '导出失败: ' + (err.message || String(err)))
      } finally {
        this.generating = false
      }
    },
    buildExportJson() {
      const groups = this.withUuid ? this.withRowUuid(this.exportGroups) : this.exportGroups
      const payload = { conditionList: this.exportConditions, maxgroupList: groups }
      // _ignoreList 中的根节点不写入导出 JSON
      const ignoreList = this.ignoreList || []
      ignoreList.forEach(key => { delete payload[key] })
      this.exportJson = JSON.stringify(payload, null, 2)
    },
    withRowUuid(groups) {
      return groups.map(group => this.stampRowUuid(group))
    },
    // 递归给每行(含 APPLICATIONAUTH 聚合子数组)补 sks:uuid
    stampRowUuid(row) {
      const copy = Object.assign({}, row, { 'sks:uuid': this.genUuid() })
      Object.keys(copy).forEach(key => {
        if (Array.isArray(copy[key])) {
          copy[key] = copy[key].map(item => this.stampRowUuid(item))
        }
      })
      return copy
    },
    genUuid() {
      return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
        const r = (Math.random() * 16) | 0
        const v = c === 'x' ? r : (r & 0x3) | 0x8
        return v.toString(16)
      })
    },
    copyJson() {
      if (!this.exportJson) {
        this.$message.warning('没有可复制的 JSON')
        return
      }
      this.copyToClipboard(this.exportJson, '导出JSON')
    },
    saveJson() {
      if (!this.exportJson) {
        this.$message.warning('没有可保存的 JSON')
        return
      }
      const blob = new Blob([this.exportJson], { type: 'application/json;charset=utf-8' })
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = 'SecurGroupPermission-' + Date.now() + '.json'
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
.picker-grid {
  display: flex;
  gap: 16px;
}
.picker-panel {
  flex: 1;
  min-width: 0;
}
.picker-title {
  font-weight: 600;
  font-size: 14px;
  margin-bottom: 8px;
}
.picker-search {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.cache-box {
  margin-top: 10px;
  min-height: 40px;
  padding: 8px;
  background: #fafafa;
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
}
.cache-label {
  color: #606266;
  font-size: 13px;
  margin-right: 6px;
}
.cache-tag {
  margin: 0 6px 6px 0;
}
.cache-empty {
  color: #909399;
  font-size: 12px;
}
.generate-bar {
  margin-top: 16px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
}
.generate-summary {
  color: #606266;
  font-size: 13px;
}
.json-toolbar {
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}
.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
</style>
