<template>
  <section class="message-import-page">
    <el-card>
      <div class="page-header-row">
        <div>
          <h2>多语言消息 Excel 导入</h2>
          <p class="page-summary">
            读取 Excel 第一个工作表（消息页），按 MSGGROUP + MSGKEY 匹配 MAXMESSAGES；
            基准语言更新 MAXMESSAGES.VALUE，其它语言更新 L_MAXMESSAGES.VALUE（翻译记录不存在时自动新增）。上传文件仅缓存在服务端内存中。
          </p>
        </div>
        <div class="page-actions">
          <el-button type="primary" icon="el-icon-upload2" size="mini" @click="openUploadDialog">上传 Excel</el-button>
        </div>
      </div>

      <div class="language-bar">
        <span class="language-label">更新语言（勾选后才更新对应语言的 value）</span>
        <el-checkbox-group v-model="selectedLanguages" class="language-checkboxes">
          <el-checkbox
            v-for="lang in languages"
            :key="lang.code"
            :label="lang.code"
          >
            {{ lang.name }}（{{ lang.code }}）<span v-if="lang.base" class="base-mark">· 基准</span>
          </el-checkbox>
        </el-checkbox-group>
        <span v-if="!loadingLanguages && !languages.length" class="empty-tip">未查询到已启用语言</span>
      </div>

      <div class="preview-toolbar">
        <div class="toolbar-left">
          <el-checkbox v-model="trimAll" class="trim-checkbox">读取excel时清除前后空字符</el-checkbox>
          <el-button
            type="primary"
            icon="el-icon-refresh"
            size="mini"
            :disabled="!canExecute"
            :loading="executing"
            @click="handleExecute"
          >确定更新</el-button>
          <span v-if="sessionId" class="file-info">
            {{ fileName }} · 共 {{ totalRows }} 行 · 当前预览前 {{ previewRows.length }} 行
          </span>
        </div>
        <div v-if="toolbarReason" class="toolbar-right">
          <span class="disabled-reason"><i class="el-icon-warning-outline" /> {{ toolbarReason }}</span>
        </div>
      </div>

      <el-alert
        v-if="sessionId && missingLanguages.length"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 12px"
        :title="'Excel 缺少以下已启用语言列：' + missingLanguages.join('、') + '，执行时将不会更新这些语言'"
      />

      <el-table
        v-if="sessionId"
        :data="previewRows"
        border
        size="small"
        max-height="420"
        empty-text="Excel 中没有可预览的数据行"
      >
        <el-table-column
          v-for="col in previewColumns"
          :key="col.prop"
          :prop="col.prop"
          :label="col.label"
          :min-width="columnMinWidth(col)"
          show-overflow-tooltip
        />
      </el-table>
      <el-empty v-else description="请上传 Excel 文件，上传后显示前 10 行数据" :image-size="80" />
    </el-card>

    <el-card v-if="result" class="result-card">
      <div slot="header" class="result-header">
        <span>执行结果</span>
        <span class="result-file">{{ result.fileName }}</span>
      </div>

      <div class="result-summary">
        <el-tag type="success" size="small">成功 {{ summary.success }}</el-tag>
        <el-tag type="warning" size="small">异常 {{ summary.abnormal }}</el-tag>
        <el-tag type="danger" size="small">错误 {{ summary.error }}</el-tag>
        <el-tag type="info" size="small">跳过 {{ summary.skipped }}</el-tag>
        <span class="summary-total">总行数 {{ result.totalRows || 0 }}</span>
        <span v-if="result.selectedLanguages && result.selectedLanguages.length" class="summary-total">
          更新语言 {{ result.selectedLanguages.join('、') }}
        </span>
      </div>

      <el-table
        :data="resultRows"
        border
        size="small"
        row-key="sks:uuid"
        max-height="520"
        empty-text="没有执行结果"
      >
        <el-table-column type="expand">
          <template slot-scope="scope">
            <div class="lang-detail-wrap">
              <el-table
                v-if="scope.row['sks:languages'] && scope.row['sks:languages'].length"
                :data="scope.row['sks:languages']"
                border
                size="mini"
              >
                <el-table-column prop="LANGCODE" label="语言" width="80" />
                <el-table-column prop="TABLE" label="表" width="160" />
                <el-table-column prop="sks:uniqueID" label="记录ID" width="140" />
                <el-table-column label="状态" width="90">
                  <template slot-scope="langScope">
                    <el-tag :type="languageTagType(langScope.row)" size="mini">{{ langScope.row['sks:statusLabel'] || langScope.row._status }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="_message" label="说明" min-width="220" show-overflow-tooltip />
              </el-table>
              <span v-else class="empty-tip">无语言更新明细</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="sks:excelRow" label="Excel行" width="80" />
        <el-table-column prop="MSGGROUP" label="消息组" min-width="140" show-overflow-tooltip />
        <el-table-column prop="MSGKEY" label="消息键" min-width="170" show-overflow-tooltip />
        <el-table-column label="MAXMESSAGESID" width="130">
          <template slot-scope="scope">
            {{ scope.row['sks:uniqueID'] || scope.row.EXCEL_MAXMESSAGESID || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="MSGID" label="MSGID" width="130" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template slot-scope="scope">
            <el-tag :type="rowTagType(scope.row)" size="mini">{{ scope.row['sks:statusLabel'] || scope.row._status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="_message" label="说明" min-width="300" show-overflow-tooltip />
      </el-table>
    </el-card>

    <el-dialog
      title="上传多语言消息 Excel"
      :visible.sync="uploadDialog.visible"
      width="600px"
      :close-on-click-modal="false"
      @closed="onUploadClosed"
    >
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="仅读取第一个工作表（消息页）。表头需包含 MSGGROUP、MSGKEY、MAXMESSAGESID 及各语言 _VALUE 列；文件仅缓存在服务端内存中，30 分钟后失效。"
      />
      <el-upload
        ref="uploader"
        drag
        action=""
        :auto-upload="false"
        :limit="1"
        accept=".xls,.xlsx"
        :on-change="onFileChange"
        :on-remove="onFileRemove"
        :file-list="uploadDialog.fileList"
        style="margin-top: 12px"
      >
        <i class="el-icon-upload" />
        <div class="el-upload__text">将 <b>.xls / .xlsx</b> 拖到此处，或<em>点击选择</em></div>
        <div slot="tip" class="el-upload__tip">上传后返回前 10 行预览，并生成文件缓存会话标识</div>
      </el-upload>
      <el-alert
        v-if="uploadDialog.errorMsg"
        type="error"
        :title="uploadDialog.errorMsg"
        :closable="false"
        show-icon
        style="margin-top: 12px"
      />
      <div slot="footer">
        <el-button size="mini" @click="uploadDialog.visible = false">取消</el-button>
        <el-button
          type="primary"
          size="mini"
          :loading="uploadDialog.uploading"
          :disabled="!uploadDialog.file"
          @click="submitUpload"
        >确认上传</el-button>
      </div>
    </el-dialog>
  </section>
</template>

<script>
import { getLanguages } from '@/api/system'
import { previewMessageExcel, executeMessageImport } from '@/api/messageimport'

export default {
  name: 'MessageImport',
  data() {
    return {
      loadingLanguages: false,
      languages: [],
      selectedLanguages: [],
      baseLanguage: '',
      trimAll: false,

      uploadDialog: {
        visible: false,
        file: null,
        fileList: [],
        uploading: false,
        errorMsg: ''
      },

      sessionId: '',
      fileName: '',
      totalRows: 0,
      previewColumns: [],
      previewRows: [],
      invalidLanguages: [],
      missingLanguages: [],
      canUpdate: false,
      disabledReason: '请先上传 Excel 文件',

      executing: false,
      result: null,
      resultRows: [],
      summary: {
        success: 0,
        abnormal: 0,
        error: 0,
        skipped: 0
      }
    }
  },
  computed: {
    canExecute() {
      return !!this.sessionId && this.canUpdate && this.selectedLanguages.length > 0 && !this.executing
    },
    toolbarReason() {
      if (!this.canUpdate) return this.disabledReason
      if (!this.selectedLanguages.length) return '请至少勾选一种要更新的语言'
      return ''
    }
  },
  created() {
    this.fetchLanguages()
  },
  methods: {
    fetchLanguages() {
      this.loadingLanguages = true
      getLanguages().then(res => {
        if (res.code === 200 && res.data) {
          this.languages = res.data.languages || []
          this.baseLanguage = res.data.baseLanguage || ''
        } else {
          this.$message.error(res.message || '查询支持的语言失败')
        }
      }).catch(err => {
        this.$message.error('查询支持的语言失败: ' + (err.message || String(err)))
      }).finally(() => {
        this.loadingLanguages = false
      })
    },
    columnMinWidth(col) {
      if (col.prop === 'MAXMESSAGESID') return 130
      if (col.langcode) return 260
      return 140
    },
    syncLanguageSelection(languages) {
      this.languages = languages || []
      const codes = this.languages.map(lang => lang.code)
      this.selectedLanguages = this.selectedLanguages.filter(code => codes.indexOf(code) >= 0)
    },
    openUploadDialog() {
      this.uploadDialog.visible = true
      this.uploadDialog.file = null
      this.uploadDialog.fileList = []
      this.uploadDialog.errorMsg = ''
    },
    onUploadClosed() {
      this.uploadDialog.file = null
      this.uploadDialog.fileList = []
      this.uploadDialog.errorMsg = ''
      if (this.$refs.uploader) this.$refs.uploader.clearFiles()
    },
    onFileChange(file) {
      if (!file || !file.raw) return
      this.uploadDialog.file = file.raw
      this.uploadDialog.fileList = [file]
      this.uploadDialog.errorMsg = ''
    },
    onFileRemove() {
      this.uploadDialog.file = null
      this.uploadDialog.fileList = []
    },
    submitUpload() {
      if (!this.uploadDialog.file) {
        this.$message.warning('请先选择 Excel 文件')
        return
      }
      const fd = new FormData()
      fd.append('file', this.uploadDialog.file)
      fd.append('trim', this.trimAll ? 'true' : 'false')
      this.uploadDialog.uploading = true
      this.uploadDialog.errorMsg = ''
      previewMessageExcel(fd).then(res => {
        if (res.code !== 200 || !res.data) {
          this.uploadDialog.errorMsg = res.message || '上传失败'
          return
        }
        const data = res.data
        this.sessionId = data.sessionId || ''
        this.fileName = data.fileName || ''
        this.totalRows = data.totalRows || 0
        this.previewColumns = data.columns || []
        this.previewRows = data.rows || []
        this.invalidLanguages = data.invalidLanguages || []
        this.missingLanguages = data.missingLanguages || []
        this.canUpdate = !!data.canUpdate
        this.disabledReason = data.canUpdate ? '' : (data.disabledReason || '无法执行更新')
        this.syncLanguageSelection(data.languages || this.languages)
        this.baseLanguage = data.baseLanguage || this.baseLanguage
        this.result = null
        this.resultRows = []
        this.summary = { success: 0, abnormal: 0, error: 0, skipped: 0 }
        this.uploadDialog.visible = false
        this.$message.success('读取成功，共 ' + this.totalRows + ' 行，预览前 ' + this.previewRows.length + ' 行')
      }).catch(err => {
        this.uploadDialog.errorMsg = '上传失败: ' + (err.message || String(err))
      }).finally(() => {
        this.uploadDialog.uploading = false
      })
    },
    handleExecute() {
      if (!this.sessionId) {
        this.$message.warning('请先上传 Excel 文件')
        return
      }
      if (!this.canUpdate) {
        this.$message.warning(this.disabledReason || '当前文件无法执行更新')
        return
      }
      if (!this.selectedLanguages.length) {
        this.$message.warning('请至少勾选一种要更新的语言')
        return
      }
      const langText = this.selectedLanguages.join('、')
      this.$confirm(
        '将按 Excel 内容直接更新 DB2 中 MAXMESSAGES / L_MAXMESSAGES 的 VALUE 字段，更新语言：' +
        langText + '，共 ' + this.totalRows + ' 行。确认执行？',
        '确认更新',
        {
          confirmButtonText: '确认更新',
          cancelButtonText: '取消',
          type: 'warning'
        }
      ).then(() => this.doExecute()).catch(() => {})
    },
    doExecute() {
      this.executing = true
      executeMessageImport({
        sessionId: this.sessionId,
        trim: this.trimAll ? 'true' : 'false',
        langcodes: this.selectedLanguages.join(',')
      }).then(res => {
        if (res.code !== 200 || !res.data) {
          this.$message.error(res.message || '执行更新失败')
          return
        }
        const data = res.data
        this.result = data
        this.resultRows = (data.data && data.data.maxmessages) || []
        this.summary = {
          success: data['sks:successSumTotal'] || 0,
          abnormal: data['sks:abnormalSumTotal'] || 0,
          error: data['sks:errorSumTotal'] || 0,
          skipped: data['sks:skippedSumTotal'] || 0
        }
        const failed = this.summary.abnormal + this.summary.error
        if (failed > 0) {
          this.$message.warning('更新完成：成功 ' + this.summary.success + ' 行，异常 ' +
            this.summary.abnormal + ' 行，错误 ' + this.summary.error + ' 行，跳过 ' + this.summary.skipped + ' 行')
        } else {
          this.$message.success('更新完成：成功 ' + this.summary.success + ' 行，跳过 ' + this.summary.skipped + ' 行')
        }
      }).catch(err => {
        this.$message.error('执行更新失败: ' + (err.message || String(err)))
      }).finally(() => {
        this.executing = false
      })
    },
    rowTagType(row) {
      if (row._status === 'success') return 'success'
      if (row._status === 'skipped') return 'info'
      if (row['sks:failType'] === 'NOT_FOUND') return 'warning'
      return 'danger'
    },
    languageTagType(detail) {
      if (detail._status === 'success') return 'success'
      if (detail._status === 'skipped') return 'info'
      if (detail['sks:failType'] === 'NOT_FOUND') return 'warning'
      return 'danger'
    }
  }
}
</script>

<style lang="scss" scoped>
.message-import-page {
  padding: 16px;
}
.page-header-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  h2 {
    margin: 0 0 6px 0;
  }
}
.page-summary {
  color: #606266;
  margin: 0;
  max-width: 980px;
  line-height: 1.6;
}
.page-actions {
  flex-shrink: 0;
  margin-left: 16px;
}
.language-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin: 16px 0;
  padding: 10px 12px;
  background: #f7f9fc;
  border-radius: 4px;
  .language-label {
    color: #606266;
    font-size: 13px;
    margin-right: 4px;
  }
  .language-checkboxes {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
  }
  .language-checkboxes .el-checkbox {
    margin-right: 16px;
    margin-left: 0;
  }
  .base-mark {
    color: #67c23a;
  }
}
.preview-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  gap: 16px;
}
.toolbar-left {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
}
.trim-checkbox {
  margin-right: 14px;
}
.file-info {
  margin-left: 14px;
  color: #909399;
  font-size: 12px;
}
.toolbar-right {
  flex-shrink: 0;
  text-align: right;
}
.disabled-reason {
  color: #f56c6c;
  font-size: 12px;
  line-height: 1.5;
}
.empty-tip {
  color: #909399;
  font-size: 12px;
}
.result-card {
  margin-top: 16px;
}
.result-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.result-file {
  color: #909399;
  font-size: 12px;
}
.result-summary {
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  .el-tag {
    margin-right: 4px;
  }
  .summary-total {
    margin-left: 8px;
    color: #606266;
    font-size: 12px;
  }
}
.lang-detail-wrap {
  padding: 10px 16px;
  background: #fafafa;
}
</style>
