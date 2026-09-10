<template>
  <section class="mafappdata-page">
    <el-card>
      <div class="page-header-row">
        <div>
          <h2>MAFAPPDATA 应用包管理</h2>
          <p class="page-summary">查询 MAXIMO.MAFAPPDATA 表（graphite 移动端应用包），支持应用名 APPID 搜索、应用包上传导入与导出下载。直接操作 DB2，覆盖 ACTIVE 应用会立即影响石墨路由，请谨慎操作。</p>
        </div>
      </div>

      <el-form :model="formData" ref="queryForm" :inline="true" label-width="80px" @submit.native.prevent>
        <el-form-item label="应用名称">
          <el-input v-model="formData.appid" placeholder="APPID 模糊搜索，= 前缀精确" clearable @keyup.enter.native="handleQuery" style="width:220px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="formData.status" placeholder="全部" clearable style="width:140px">
            <el-option label="ACTIVE" value="ACTIVE" />
            <el-option label="INACTIVE" value="INACTIVE" />
          </el-select>
        </el-form-item>
        <el-form-item label="移动端">
          <el-select v-model="formData.ismobile" placeholder="全部" clearable style="width:120px">
            <el-option label="是" value="1" />
            <el-option label="否" value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="cyan" icon="el-icon-search" size="mini" :loading="loading" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" size="mini" @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="result-panel">
        <div class="toolbar">
          <el-button type="primary" icon="el-icon-upload2" size="mini" @click="openUploadDialog">上传导入</el-button>
          <el-button type="success" icon="el-icon-download" size="mini" :disabled="!selection.length" @click="handleBatchExport">批量导出({{ selection.length }})</el-button>
          <el-button icon="el-icon-refresh" size="mini" @click="fetchList">刷新</el-button>
        </div>

        <el-table :data="list" border style="width:100%" v-loading="loading" @selection-change="onSelectionChange">
          <el-table-column type="selection" width="45" />
          <el-table-column prop="APPID" label="应用名称" min-width="130">
            <template slot-scope="scope">
              <el-link type="primary" :underline="false" title="点击查看详情" @click="showDetail(scope.row)">{{ scope.row.APPID }}</el-link>
            </template>
          </el-table-column>
          <el-table-column prop="VERSION" label="版本" width="110" />
          <el-table-column prop="REVISION" label="修订" width="60" />
          <el-table-column prop="STATUS" label="状态" width="90">
            <template slot-scope="scope">
              <el-tag :type="scope.row.STATUS === 'ACTIVE' ? 'success' : 'info'" size="mini">{{ scope.row.STATUS }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="APPMODE" label="模式" width="90" />
          <el-table-column prop="ISMOBILE" label="移动端" width="70">
            <template slot-scope="scope">{{ scope.row.ISMOBILE === 1 ? '是' : '否' }}</template>
          </el-table-column>
          <el-table-column label="包大小" width="90">
            <template slot-scope="scope">{{ formatSize(scope.row.APPLEN) }}</template>
          </el-table-column>
          <el-table-column label="CHECKSUM" width="110">
            <template slot-scope="scope">
              <el-tooltip :content="scope.row.CHECKSUM" placement="top">
                <span>{{ (scope.row.CHECKSUM || '').substring(0, 8) }}</span>
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column prop="DEPLOYBY" label="部署人" width="110" />
          <el-table-column prop="BUILDTIMESTAMP" label="构建时间" min-width="160" show-overflow-tooltip />
          <el-table-column prop="MAFAPPDATAID" label="ID" width="80" />
          <el-table-column label="操作" width="90" fixed="right">
            <template slot-scope="scope">
              <el-button type="text" size="mini" @click="handleExportRow(scope.row)">导出</el-button>
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
      </div>
    </el-card>

    <!-- 上传导入弹窗 -->
    <el-dialog :title="uploadDialog.updateOnly ? '上传应用包（更新 MAFAPPDATA）' : '上传应用包（导入 MAFAPPDATA）'" :visible.sync="uploadDialog.visible" width="640px" :close-on-click-modal="false" @closed="onUploadClosed">
      <el-alert type="warning" :closable="false" show-icon style="margin-bottom:12px"
        title="直接写入 DB2 MAFAPPDATA 表。覆盖 ACTIVE 应用将立即影响石墨路由，建议先导出原包备份。同一 APPID+构建时间戳 已存在时执行覆盖更新。" />

      <el-upload
        ref="uploader"
        drag
        action=""
        :auto-upload="false"
        :limit="1"
        accept=".zip,.apkg"
        :on-change="onFileChange"
        :on-remove="onFileRemove"
        :file-list="uploadDialog.fileList">
        <i class="el-icon-upload"></i>
        <div class="el-upload__text">将 <b>.zip / .apkg</b> 拖到此处，或<em>点击选择</em></div>
        <div class="el-upload__tip" slot="tip">.apkg 为完整包（含元数据，自动识别）；原始 graphite zip 需在下方补全 APPID/版本</div>
      </el-upload>

      <div v-loading="uploadDialog.analyzing" style="margin-top:12px">
        <el-alert v-if="uploadDialog.updateOnly" type="warning" :closable="false" show-icon style="margin-bottom:10px"
          :title="'更新模式：本次导入仅更新记录 [' + uploadDialog.form.appid + ']（MAFAPPDATAID=' + uploadDialog.targetId + '），不会新增记录。请上传与该应用一致的新构建包。'" />
        <el-alert v-if="uploadDialog.analyzeMsg" :type="uploadDialog.analyzeValid ? 'success' : 'error'" :title="uploadDialog.analyzeMsg" :closable="false" show-icon style="margin-bottom:10px" />

        <el-form label-width="90px" size="small" :disabled="uploadDialog.isApkg">
          <el-form-item label="应用 APPID" required>
            <el-input v-model="uploadDialog.form.appid" placeholder="如 MASUSER（<=40字符）" :disabled="uploadDialog.updateOnly" />
          </el-form-item>
          <el-form-item label="版本 VERSION">
            <el-input v-model="uploadDialog.form.version" placeholder="如 9.1.77.0（<=15字符）" />
          </el-form-item>
          <el-form-item label="模式 APPMODE">
            <el-input v-model="uploadDialog.form.appmode" placeholder="DEBUG" />
          </el-form-item>
          <el-form-item label="状态 STATUS">
            <el-select v-model="uploadDialog.form.status" style="width:100%">
              <el-option label="ACTIVE" value="ACTIVE" />
              <el-option label="INACTIVE" value="INACTIVE" />
            </el-select>
          </el-form-item>
          <el-form-item label="移动端">
            <el-switch v-model="uploadDialog.form.ismobileBool" />
          </el-form-item>
          <el-form-item label="修订 REVISION">
            <el-input-number v-model="uploadDialog.form.revision" :min="0" controls-position="right" style="width:100%" />
          </el-form-item>
          <el-form-item label="部署人">
            <el-input v-model="uploadDialog.form.deployby" placeholder="MANAGE-PANEL" />
          </el-form-item>
        </el-form>
        <el-alert v-if="uploadDialog.isApkg" type="info" :closable="false" show-icon
          title="检测到 .apkg 完整包，元数据从包内 appdata.json 自动还原（上表仅展示，不可编辑）。" />
      </div>

      <div slot="footer">
        <el-button size="mini" @click="uploadDialog.visible = false">取消</el-button>
        <el-button type="primary" size="mini" :loading="uploadDialog.importing" :disabled="!uploadDialog.file" @click="submitImport">确定导入</el-button>
      </div>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog title="MAFAPPDATA 详情" :visible.sync="detailDialog.visible" width="640px" :close-on-click-modal="false">
      <div v-loading="detailDialog.loading">
        <el-descriptions v-if="detailDialog.row" :column="2" border size="small">
          <el-descriptions-item label="MAFAPPDATAID">{{ detailDialog.row.MAFAPPDATAID }}</el-descriptions-item>
          <el-descriptions-item label="应用名称 APPID">{{ detailDialog.row.APPID }}</el-descriptions-item>
          <el-descriptions-item label="版本 VERSION">{{ detailDialog.row.VERSION }}</el-descriptions-item>
          <el-descriptions-item label="修订 REVISION">{{ detailDialog.row.REVISION }}</el-descriptions-item>
          <el-descriptions-item label="状态 STATUS">
            <el-tag :type="detailDialog.row.STATUS === 'ACTIVE' ? 'success' : 'info'" size="mini">{{ detailDialog.row.STATUS }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="模式 APPMODE">{{ detailDialog.row.APPMODE || '-' }}</el-descriptions-item>
          <el-descriptions-item label="移动端">{{ detailDialog.row.ISMOBILE === 1 ? '是' : '否' }}</el-descriptions-item>
          <el-descriptions-item label="包大小">{{ formatSize(detailDialog.row.APPLEN) }}</el-descriptions-item>
          <el-descriptions-item label="CHECKSUM" :span="2">{{ detailDialog.row.CHECKSUM || '-' }}</el-descriptions-item>
          <el-descriptions-item label="部署人 DEPLOYBY">{{ detailDialog.row.DEPLOYBY || '-' }}</el-descriptions-item>
          <el-descriptions-item label="部署时间">{{ detailDialog.row.DEPLOYDATETIME || '-' }}</el-descriptions-item>
          <el-descriptions-item label="构建时间">{{ detailDialog.row.BUILDTIMESTAMP || '-' }}</el-descriptions-item>
          <el-descriptions-item label="ROWSTAMP">{{ detailDialog.row.ROWSTAMP }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <div slot="footer">
        <el-button type="primary" size="mini" icon="el-icon-upload2" :disabled="!detailDialog.row" @click="importFromDetail">导入更新</el-button>
        <el-button size="mini" @click="detailDialog.visible = false">关闭</el-button>
      </div>
    </el-dialog>
  </section>
</template>

<script>
import { getMafAppDataList, getMafAppDetail, analyzeMafApp, importMafApp, exportMafApp } from '@/api/mafappdata'

export default {
  name: 'MafAppData',
  data() {
    return {
      loading: false,
      hasSearched: false,
      list: [],
      total: 0,
      pageNum: 1,
      pageSize: 20,
      selection: [],
      formData: { appid: '', status: '', ismobile: '' },
      detailDialog: {
        visible: false,
        loading: false,
        row: null
      },
      uploadDialog: {
        visible: false,
        file: null,
        fileList: [],
        analyzing: false,
        importing: false,
        analyzeValid: false,
        analyzeMsg: '',
        isApkg: false,
        // 更新模式（从详情"导入更新"进入）：仅更新指定记录，不新增
        updateOnly: false,
        targetId: null,
        form: this.defaultUploadForm()
      }
    }
  },
  created() {
    this.fetchList()
  },
  methods: {
    defaultUploadForm() {
      return {
        appid: '',
        version: '',
        appmode: 'DEBUG',
        status: 'ACTIVE',
        ismobileBool: false,
        revision: 0,
        deployby: 'MANAGE-PANEL'
      }
    },
    handleQuery() {
      this.hasSearched = true
      this.pageNum = 1
      this.fetchList()
    },
    resetForm() {
      this.formData = { appid: '', status: '', ismobile: '' }
      this.hasSearched = false
      this.pageNum = 1
      this.fetchList()
    },
    onSelectionChange(rows) {
      this.selection = rows
    },
    fetchList() {
      this.loading = true
      const params = {
        appid: this.formData.appid || undefined,
        status: this.formData.status || undefined,
        ismobile: this.formData.ismobile === '' ? undefined : this.formData.ismobile,
        pageNum: this.pageNum,
        pageSize: this.pageSize
      }
      getMafAppDataList(params).then(res => {
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
    formatSize(bytes) {
      if (!bytes && bytes !== 0) return '-'
      if (bytes < 1024) return bytes + ' B'
      if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
      return (bytes / 1024 / 1024).toFixed(2) + ' MB'
    },

    // ============ 详情 ============
    showDetail(row) {
      this.detailDialog.visible = true
      this.detailDialog.loading = true
      this.detailDialog.row = null
      getMafAppDetail(row.MAFAPPDATAID).then(res => {
        if (res.code === 200 && res.data) {
          this.detailDialog.row = res.data
        } else {
          this.$message.error(res.message || '查询详情失败')
        }
      }).catch(err => {
        this.$message.error('查询详情失败: ' + (err.message || String(err)))
      }).finally(() => {
        this.detailDialog.loading = false
      })
    },
    importFromDetail() {
      const row = this.detailDialog.row
      if (!row) return
      this.detailDialog.visible = false
      // 更新模式：锁定目标记录，仅做更新不新增
      this.openUploadDialog(row.APPID, row.VERSION, row.MAFAPPDATAID, true)
    },

    // ============ 导出 / 下载 ============
    handleExportRow(row) {
      this.doExport([row])
    },
    handleBatchExport() {
      if (!this.selection.length) {
        this.$message.warning('请先选择要导出的记录')
        return
      }
      this.doExport(this.selection)
    },
    buildFileName(rows) {
      if (rows.length === 1) {
        const r = rows[0]
        return r.APPID + '__ver-' + (r.VERSION || '0.0.0.0') + '.zip'
      }
      return 'mafappdata-export.zip'
    },
    doExport(rows) {
      const ids = rows.map(r => r.MAFAPPDATAID).join(',')
      const fileName = this.buildFileName(rows)
      const loading = this.$loading({ lock: true, text: '正在导出 ' + fileName + ' ...' })
      exportMafApp(ids).then(res => {
        if (res instanceof Blob) {
          // 后端异常时可能返回 JSON（content-type 为 application/json）
          if (res.type && res.type.indexOf('application/json') >= 0) {
            res.text().then(t => {
              try { const j = JSON.parse(t); this.$message.error(j.message || '导出失败') } catch (e) { this.$message.error('导出失败') }
            })
            return
          }
          const url = URL.createObjectURL(res)
          const a = document.createElement('a')
          a.href = url
          a.download = fileName
          document.body.appendChild(a)
          a.click()
          document.body.removeChild(a)
          URL.revokeObjectURL(url)
          this.$message.success('已开始下载 ' + fileName)
        } else {
          this.$message.error('导出失败：响应格式异常')
        }
      }).catch(err => {
        this.$message.error('导出失败: ' + (err.message || String(err)))
      }).finally(() => {
        loading.close()
      })
    },

    // ============ 上传导入 ============
    openUploadDialog(prefillAppid, prefillVersion, targetId, updateOnly) {
      this.uploadDialog.visible = true
      this.uploadDialog.file = null
      this.uploadDialog.fileList = []
      this.uploadDialog.analyzeValid = false
      this.uploadDialog.analyzeMsg = ''
      this.uploadDialog.isApkg = false
      // 更新模式标记（从详情"导入更新"进入）
      this.uploadDialog.updateOnly = !!updateOnly
      this.uploadDialog.targetId = targetId || null
      const form = this.defaultUploadForm()
      // 从详情"导入更新"进入时预填 APPID/版本
      if (prefillAppid) form.appid = prefillAppid
      if (prefillVersion) form.version = prefillVersion
      this.uploadDialog.form = form
    },
    onUploadClosed() {
      this.uploadDialog.file = null
      this.uploadDialog.fileList = []
      this.uploadDialog.analyzeValid = false
      this.uploadDialog.analyzeMsg = ''
      if (this.$refs.uploader) this.$refs.uploader.clearFiles()
    },
    onFileChange(file) {
      if (!file || !file.raw) return
      this.uploadDialog.file = file.raw
      this.uploadDialog.fileList = [file]
      this.uploadDialog.analyzing = true
      this.uploadDialog.analyzeMsg = ''
      this.uploadDialog.analyzeValid = false
      this.uploadDialog.isApkg = false
      // 保留从详情"导入更新"进入时预填的 APPID/版本，其余字段重置为默认
      const prefillAppid = this.uploadDialog.form.appid
      const prefillVersion = this.uploadDialog.form.version
      const form = this.defaultUploadForm()
      if (prefillAppid) form.appid = prefillAppid
      if (prefillVersion) form.version = prefillVersion
      this.uploadDialog.form = form

      const fd = new FormData()
      fd.append('file', file.raw)
      analyzeMafApp(fd).then(res => {
        if (res.code === 200 && res.data) {
          const d = res.data
          if (d.valid) {
            this.uploadDialog.analyzeValid = true
            const sizeTxt = this.formatSize(d.size)
            // .apkg 完整包识别（服务端 import 时自动判断；此处按文件后缀提示）
            const name = (d.fileName || '').toLowerCase()
            this.uploadDialog.isApkg = name.endsWith('.apkg')
            if (this.uploadDialog.isApkg) {
              this.uploadDialog.analyzeMsg = '检测到 .apkg 完整包（大小 ' + sizeTxt + '），元数据将在导入时从包内 appdata.json 自动还原'
            } else if (d.appid) {
              this.uploadDialog.form.appid = d.appid
              this.uploadDialog.analyzeMsg = 'ZIP 校验通过，已从 build.json 识别 APPID=' + d.appid + (d.version ? ('，版本=' + d.version) : '') + '（大小 ' + sizeTxt + '）'
              if (d.version) this.uploadDialog.form.version = d.version
            } else {
              this.uploadDialog.analyzeMsg = 'ZIP 校验通过（大小 ' + sizeTxt + '），包内未找到 build.json，请手动填写 APPID/版本'
            }
          } else {
            this.uploadDialog.analyzeValid = false
            this.uploadDialog.analyzeMsg = d.message || '不是有效的 ZIP 包'
          }
        } else {
          this.uploadDialog.analyzeMsg = res.message || '分析失败'
        }
      }).catch(err => {
        this.uploadDialog.analyzeMsg = '分析失败: ' + (err.message || String(err))
      }).finally(() => {
        this.uploadDialog.analyzing = false
      })
    },
    onFileRemove() {
      this.uploadDialog.file = null
      this.uploadDialog.fileList = []
      this.uploadDialog.analyzeValid = false
      this.uploadDialog.analyzeMsg = ''
      this.uploadDialog.isApkg = false
    },
    submitImport() {
      if (!this.uploadDialog.file) {
        this.$message.warning('请先选择应用包文件')
        return
      }
      // 非 apkg 必须有 APPID
      if (!this.uploadDialog.isApkg && !this.uploadDialog.form.appid) {
        this.$message.warning('请填写应用 APPID（或上传含 build.json 的包）')
        return
      }
      const doImport = () => {
        const fd = new FormData()
        fd.append('file', this.uploadDialog.file)
        const f = this.uploadDialog.form
        // 更新模式：携带目标记录 ID，后端仅做 UPDATE 不新增
        if (this.uploadDialog.updateOnly) {
          fd.append('updateOnly', '1')
          if (this.uploadDialog.targetId) fd.append('id', String(this.uploadDialog.targetId))
        }
        fd.append('appid', f.appid || '')
        fd.append('version', f.version || '')
        fd.append('appmode', f.appmode || 'DEBUG')
        fd.append('status', f.status || 'ACTIVE')
        fd.append('ismobile', f.ismobileBool ? '1' : '0')
        fd.append('revision', f.revision != null ? String(f.revision) : '0')
        fd.append('deployby', f.deployby || 'MANAGE-PANEL')

        this.uploadDialog.importing = true
        importMafApp(fd).then(res => {
          if (res.code === 200 && res.data) {
            const rows = res.data.rows || []
            const failed = rows.filter(r => !r.success)
            if (failed.length) {
              this.$message.error('导入完成但有失败：' + failed.map(r => r.appid + '(' + r.error + ')').join('；'))
            } else {
              const ok = rows[0]
              const verb = this.uploadDialog.updateOnly ? '更新' : '导入'
              this.$message.success(verb + '成功：[' + ok.appid + '] ' + ok.action + '，checksum=' + (ok.checksum || '').substring(0, 8))
            }
            this.uploadDialog.visible = false
            this.fetchList()
          } else {
            this.$message.error(res.message || '导入失败')
          }
        }).catch(err => {
          this.$message.error('导入失败: ' + (err.message || String(err)))
        }).finally(() => {
          this.uploadDialog.importing = false
        })
      }

      // 覆盖 ACTIVE 二次确认
      const f = this.uploadDialog.form
      if (f.status === 'ACTIVE') {
        this.$confirm('将以 STATUS=ACTIVE 导入，若应用已存在将立即覆盖在线石墨路由版本。确认继续？', '覆盖确认', {
          confirmButtonText: '确认导入',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(doImport).catch(() => {})
      } else {
        doImport()
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.mafappdata-page {
  padding: 16px;
}
.page-header-row {
  margin-bottom: 16px;
  h2 {
    margin: 0 0 6px 0;
  }
}
.page-summary {
  color: #606266;
  margin: 0;
}
.result-panel {
  margin-top: 16px;
}
.toolbar {
  margin-bottom: 10px;
}
</style>