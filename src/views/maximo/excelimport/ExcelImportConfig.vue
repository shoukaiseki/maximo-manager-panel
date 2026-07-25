<template>
  <section class="excel-import-page">
    <el-card class="page-card">
      <!-- 页面标题 -->
      <div slot="header" class="page-header">
        <span>Excel 导入配置</span>
        <div class="header-actions">
          <el-button size="mini" icon="el-icon-question" circle @click="showHelp = !showHelp" />
        </div>
      </div>

      <!-- 帮助提示 -->
      <el-alert v-if="showHelp" title="使用说明" type="info" show-icon :closable="false" class="help-alert">
        <template slot="default">
          <ol style="margin: 4px 0; padding-left: 20px; line-height: 1.8;">
            <li>上传 Excel 文件（.xls 或 .xlsx 格式）</li>
            <li>在下方配置区域添加 <b>Section</b>（固定区域）和 <b>Loop</b>（循环区域）</li>
            <li>在每个区域中添加字段映射，指定 Excel 单元格和对应的属性名</li>
            <li>点击 <b>预览 JSON</b> 跳转到预览页面查看解析结果</li>
            <li>确认数据正确后返回点击 <b>执行导入</b></li>
          </ol>
        </template>
      </el-alert>

      <!-- 方案管理栏 -->
      <div class="scheme-bar">
        <label class="scheme-label">配置方案:</label>
        <el-select
          v-model="selectedSchemeId"
          placeholder="选择已保存的方案"
          size="small"
          clearable
          style="width: 300px"
          @change="handleSchemeSelect"
        >
          <el-option
            v-for="s in schemeList"
            :key="s.id"
            :label="s.schemeName + (s.description ? ' (' + s.description + ')' : '')"
            :value="s.id"
          />
        </el-select>
        <el-button size="small" type="primary" icon="el-icon-document-add" @click="openSaveDialog">保存方案</el-button>
        <el-button
          v-if="selectedSchemeId"
          size="small"
          type="danger"
          icon="el-icon-delete"
          plain
          @click="handleDeleteScheme(selectedSchemeId, getSchemeName(selectedSchemeId))">删除方案</el-button>
      </div>

      <!-- 步骤 1: 文件上传 -->
      <div class="section-block">
        <h3 class="section-title">1. 上传 Excel 文件</h3>
        <el-upload
          ref="upload"
          drag
          :auto-upload="false"
          :on-change="handleFileChange"
          :on-remove="handleFileRemove"
          :file-list="fileList"
          accept=".xls,.xlsx"
          action=""
          :limit="1"
        >
          <i class="el-icon-upload" />
          <div class="el-upload__text">将 Excel 文件拖到此处，或<em>点击选择</em></div>
          <div slot="tip" class="el-upload__tip">仅支持 .xls 和 .xlsx 格式</div>
        </el-upload>
      </div>

      <!-- 步骤 2: 配置区 -->
      <div class="section-block">
        <h3 class="section-title">2. 配置导入映射</h3>

        <!-- 工作表选择 -->
        <div class="config-row">
          <label class="config-label">工作表:</label>
          <el-input v-model="config.sheetName" placeholder="Sheet1" style="width: 200px" size="small" />
          <span style="margin: 0 8px; color: #909399;">或</span>
          <label class="config-label">索引:</label>
          <el-input-number v-model="config.sheetIndex" :min="-1" :max="99" size="small" style="width: 120px" />
          <span style="margin-left: 8px; color: #909399; font-size: 12px;">(-1 表示按名称)</span>
        </div>

        <!-- 配置操作按钮 -->
        <div class="config-actions">
          <el-button type="primary" size="small" icon="el-icon-plus" @click="addSection">添加 Section</el-button>
          <el-button type="success" size="small" icon="el-icon-plus" @click="addLoop">添加 Loop</el-button>
          <el-button size="small" icon="el-icon-upload2" @click="triggerXmlImport">导入配置</el-button>
          <el-button size="small" icon="el-icon-download" @click="exportXmlConfig">导出配置</el-button>
        </div>

        <!-- XML 拖拽导入区 -->
        <div
          class="xml-drop-zone"
          @dragover.prevent="onDragOver"
          @dragleave.prevent="onDragLeave"
          @drop.prevent="onXmlDrop"
          :class="{ 'xml-drop-zone--active': isDraggingXml }"
        >
          <input ref="xmlFileInput" type="file" accept=".xml" style="display: none" @change="handleXmlFileImport" />
          <i class="el-icon-upload2"></i>
          <span v-if="!isDraggingXml">拖拽 XML 配置文件到此处，或 <em @click.stop="triggerXmlImport">点击选择</em></span>
          <span v-else>松开鼠标导入配置</span>
        </div>

        <!-- 配置列表 -->
        <div class="config-list" v-if="configList.length > 0">
          <div v-for="(item, index) in configList" :key="index" class="config-item-card">
            <!-- 配置项头部 -->
            <div class="config-item-header" :class="item.type === 'loop' ? 'loop-header' : 'section-header'">
              <span class="config-type-badge" :class="item.type">
                {{ item.type === 'loop' ? 'LOOP' : 'SECTION' }}
              </span>
              <span class="config-item-title">
                {{ item.type === 'loop' ? `循环区域 [行 ${item.startRow + 1} - ${item.endRow + 1}]` : `固定区域 [行 ${item.startRow + 1} - ${item.endRow + 1}]` }}
              </span>
              <el-button type="text" size="mini" icon="el-icon-delete" class="delete-btn" @click="removeConfigItem(index)" />
            </div>

            <!-- 配置项内容 -->
            <div class="config-item-body">
              <!-- 行范围 -->
              <div class="param-row">
                <label>起始行:</label>
                <el-input-number v-model="item.startRow" :min="0" :max="9999" size="mini" style="width: 100px" @change="() => {}" />
                <label style="margin-left: 12px;">结束行:</label>
                <el-input-number v-model="item.endRow" :min="0" :max="9999" size="mini" style="width: 100px" />
              </div>

              <!-- Loop 专属参数 -->
              <template v-if="item.type === 'loop'">
                <div class="param-row">
                  <label>items 表达式:</label>
                  <el-input v-model="item.items" placeholder="rows" size="mini" style="width: 150px" />
                  <label style="margin-left: 12px;">变量名:</label>
                  <el-input v-model="item.varName" placeholder="row" size="mini" style="width: 100px" />
                  <label style="margin-left: 12px;">变量类型:</label>
                  <el-input v-model="item.varType" placeholder="java.util.HashMap" size="mini" style="width: 200px" />
                </div>
              </template>

              <!-- 字段映射列表 -->
              <div class="mappings-section">
                <div class="mappings-header">
                  <span>字段映射列表 ({{ item.mappings.length }})</span>
                  <el-button type="text" size="mini" icon="el-icon-plus" @click="addMapping(item)" />
                </div>
                <el-table :data="item.mappings" border stripe size="mini" style="width: 100%" max-height="200">
                  <el-table-column label="Excel 单元格" width="140">
                    <template slot-scope="scope">
                      <el-input v-model="scope.row.cell" placeholder="如 A3, B4" size="mini" />
                    </template>
                  </el-table-column>
                  <el-table-column label="映射属性名" width="200">
                    <template slot-scope="scope">
                      <el-input v-model="scope.row.property" :placeholder="(item.varName || 'row') + '.fieldName'" size="mini" />
                    </template>
                  </el-table-column>
                  <el-table-column label="字段名" width="140">
                    <template slot-scope="scope">
                      <el-input v-model="scope.row.fieldName" placeholder="fieldName" size="mini" />
                    </template>
                  </el-table-column>
                  <el-table-column label="类型" width="100">
                    <template slot-scope="scope">
                      <el-input v-model="scope.row.type" placeholder="auto" size="mini" />
                    </template>
                  </el-table-column>
                  <el-table-column label="操作" width="60">
                    <template slot-scope="scope">
                      <el-button type="text" size="mini" icon="el-icon-delete" @click="removeMapping(item, scope.$index)" />
                    </template>
                  </el-table-column>
                </el-table>
              </div>

              <!-- Loop 终止条件 -->
              <template v-if="item.type === 'loop'">
                <div class="break-condition-section">
                  <el-checkbox v-model="item.useDefaultBreak" :true-label="true" :false-label="false">
                    空行自动终止
                  </el-checkbox>
                  <template v-if="!item.useDefaultBreak">
                    <div v-for="(bc, bcIdx) in item.breakConditions" :key="bcIdx" class="break-condition-row">
                      <label>偏移行:</label>
                      <el-input-number v-model="bc.offset" :min="-10" :max="10" size="mini" style="width: 80px" />
                      <el-button type="text" size="mini" icon="el-icon-delete" @click="removeBreakCondition(item, bcIdx)" />
                    </div>
                    <el-button type="text" size="mini" icon="el-icon-plus" @click="addBreakCondition(item)">添加终止条件</el-button>
                  </template>
                </div>
              </template>
            </div>
          </div>
        </div>

        <!-- 空配置提示 -->
        <el-empty v-else description="暂无配置，请添加 Section 或 Loop" :image-size="80" />
      </div>

      <!-- 步骤 3: 操作 -->
      <div class="section-block">
        <h3 class="section-title">3. 执行操作</h3>
        <div class="action-bar">
          <el-button type="primary" icon="el-icon-view" :loading="previewLoading" :disabled="!uploadedFile" @click="handlePreview">
            预览
          </el-button>
          <el-button icon="el-icon-s-data" @click="$router.push('/tools/jsonpreview')">
            JSON 预览工具
          </el-button>
          <el-button type="success" icon="el-icon-upload" :loading="executeLoading" :disabled="!uploadedFile" @click="handleExecute">
            执行导入
          </el-button>
          <el-button icon="el-icon-document" @click="generateAndShowXml">查看 XML</el-button>
        </div>
      </div>
    </el-card>

    <!-- 保存方案对话框 -->
    <el-dialog title="保存配置方案" :visible.sync="showSaveDialog" width="500px">
      <el-form :model="saveForm" label-width="80px" size="small">
        <el-form-item label="方案名称" required>
          <el-input v-model="saveForm.schemeName" placeholder="输入方案名称" />
        </el-form-item>
        <el-form-item label="方案描述">
          <el-input v-model="saveForm.description" placeholder="简要描述此方案的用途" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="工作表">
          <el-input v-model="saveForm.sheetName" placeholder="Sheet1" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button size="small" @click="showSaveDialog = false">取消</el-button>
        <el-button size="small" type="primary" @click="confirmSaveScheme">保存</el-button>
      </div>
    </el-dialog>

    <!-- 预览结果对话框 -->
    <el-dialog title="预览结果" :visible.sync="previewDialogVisible" width="800px" top="5vh">
      <el-input
        type="textarea"
        :rows="20"
        :value="previewResultText"
        readonly
        style="font-family: 'Courier New', monospace; font-size: 12px;"
      />
      <div slot="footer">
        <el-button @click="copyPreviewResult">复制 JSON</el-button>
        <el-button icon="el-icon-s-data" @click="gotoJsonPreview">在 JSON 预览中查看</el-button>
        <el-button type="primary" @click="previewDialogVisible = false">关闭</el-button>
      </div>
    </el-dialog>

    <!-- XML 查看对话框 -->
    <el-dialog title="生成的 jxls XML 配置" :visible.sync="xmlDialogVisible" width="800px" top="5vh">
      <el-input
        type="textarea"
        :rows="20"
        :value="generatedXml"
        readonly
        style="font-family: 'Courier New', monospace; font-size: 12px;"
      />
      <div slot="footer">
        <el-button @click="copyXml">复制到剪贴板</el-button>
        <el-button type="primary" @click="xmlDialogVisible = false">关闭</el-button>
      </div>
    </el-dialog>
  </section>
</template>

<script>
import { excelPreviewWithConfig, excelExecute, listSchemes, getScheme, saveScheme, updateScheme, deleteScheme } from '/src/api/solonapi'
import { buildXmlConfig, parseXmlConfig, createDefaultLoop, createDefaultSection, EMPTY_XML_TEMPLATE } from '/src/utils/jxlsConfigBuilder'

export default {
  name: 'ExcelImportConfig',
  data() {
    return {
      showHelp: false,
      fileList: [],
      uploadedFile: null,
      config: {
        sheetName: 'Sheet1',
        sheetIndex: -1,
        rootKey: 'data',
        sections: [],
        loops: []
      },
      previewLoading: false,
      previewDialogVisible: false,
      previewResultText: '',
      executeLoading: false,
      xmlDialogVisible: false,
      generatedXml: '',
      // 配置方案管理
      schemeList: [],
      selectedSchemeId: null,
      showSaveDialog: false,
      saveForm: { schemeName: '', description: '', sheetName: 'Sheet1' },
      // XML 拖拽导入
      isDraggingXml: false,
      // JSON 查看器模式（保留备用）
      jsonViewerMode: 'pretty'
    }
  },
  computed: {
    // 合并 sections 和 loops 为统一的配置列表
    configList() {
      const list = []
      this.config.sections.forEach(s => list.push({ ...s, type: 'section' }))
      this.config.loops.forEach(l => list.push({ ...l, type: 'loop' }))
      return list
    }
  },
  created() {
    this.loadSchemeList()
  },
  methods: {
    // ======== 文件操作 ========
    handleFileChange(file) {
      this.uploadedFile = file.raw
      this.fileList = [file]
    },
    handleFileRemove() {
      this.uploadedFile = null
      this.fileList = []
    },

    // ======== 配置操作 ========
    addSection() {
      const section = createDefaultSection(this.config.loops.length > 0 ? this.config.loops[this.config.loops.length - 1].startRow + 1 : 0, null, [])
      this.config.sections.push(section)
    },
    addLoop() {
      const lastRow = this.config.loops.length > 0
        ? Math.max(...this.config.loops.map(l => l.endRow))
        : 2
      const loop = {
        ...createDefaultLoop(lastRow + 1, lastRow + 1, []),
        varName: 'row',
        useDefaultBreak: true,
        breakConditions: [{ offset: 0, cellChecks: [] }]
      }
      this.config.loops.push(loop)
    },
    removeConfigItem(index) {
      // 确定是 sections 还是 loops 中的项
      let sectionsCount = this.config.sections.length
      if (index < sectionsCount) {
        this.config.sections.splice(index, 1)
      } else {
        this.config.loops.splice(index - sectionsCount, 1)
      }
    },
    addMapping(item) {
      const nextCol = item.mappings.length
      const cell = String.fromCharCode(65 + nextCol) + (item.startRow + 1)
      item.mappings.push({
        cell: cell,
        property: (item.varName || 'row') + '.field' + (nextCol + 1),
        fieldName: 'field' + (nextCol + 1),
        type: ''
      })
    },
    removeMapping(item, mappingIndex) {
      item.mappings.splice(mappingIndex, 1)
    },
    addBreakCondition(item) {
      if (!item.breakConditions) {
        item.breakConditions = []
      }
      item.breakConditions.push({ offset: 0, cellChecks: [] })
    },
    removeBreakCondition(item, bcIdx) {
      item.breakConditions.splice(bcIdx, 1)
    },

    // ======== 配置导入/导出 ========
    triggerXmlImport() {
      this.$refs.xmlFileInput.click()
    },
    onDragOver() {
      this.isDraggingXml = true
    },
    onDragLeave() {
      this.isDraggingXml = false
    },
    onXmlDrop(e) {
      this.isDraggingXml = false
      const files = e.dataTransfer.files
      if (files.length > 0) {
        const file = files[0]
        if (!file.name.endsWith('.xml')) {
          this.$message.warning('请导入 .xml 格式的配置文件')
          return
        }
        // 复用文件读取逻辑
        const input = this.$refs.xmlFileInput
        const dt = new DataTransfer()
        dt.items.add(file)
        input.files = dt.files
        this.handleXmlFileImport({ target: input })
      }
    },
    handleXmlFileImport(e) {
      const file = e.target.files[0]
      if (!file) return
      const reader = new FileReader()
      reader.onload = (event) => {
        try {
          const xml = event.target.result
          const parsed = parseXmlConfig(xml)
          this.config.sheetName = parsed.sheetName
          this.config.sheetIndex = parsed.sheetIndex
          this.config.sections = parsed.sections.map(s => ({ ...s, type: 'section' }))
          this.config.loops = parsed.loops.map(l => ({
            ...l,
            type: 'loop',
            varName: l.var,
            useDefaultBreak: l.breakConditions.length === 1 && l.breakConditions[0].cellChecks.length === 0
          }))
          this.$message.success('配置导入成功')
        } catch (err) {
          this.$message.error('配置解析失败: ' + err.message)
        }
      }
      reader.readAsText(file)
      // 重置 input 以便再次选择同一文件
      e.target.value = ''
    },
    exportXmlConfig() {
      const xml = this.buildCurrentXml()
      const blob = new Blob([xml], { type: 'application/xml;charset=utf-8' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = 'excel-import-config.xml'
      a.click()
      URL.revokeObjectURL(url)
      this.$message.success('配置已导出')
    },

    // ======== 构建 XML ========
    buildCurrentXml() {
      const cfg = {
        sheetName: this.config.sheetName,
        sheetIndex: this.config.sheetIndex,
        rootKey: this.config.rootKey,
        sections: this.config.sections.map(s => ({
          startRow: s.startRow,
          endRow: s.endRow !== undefined ? s.endRow : s.startRow,
          mappings: s.mappings
        })),
        loops: this.config.loops.map(l => ({
          startRow: l.startRow,
          endRow: l.endRow !== undefined ? l.endRow : l.startRow,
          items: l.items || 'rows',
          var: l.varName || 'row',
          varType: l.varType || 'java.util.HashMap',
          mappings: l.mappings,
          breakConditions: l.useDefaultBreak
            ? [{ offset: 0, cellChecks: [] }]
            : (l.breakConditions || [{ offset: 0, cellChecks: [] }])
        }))
      }
      return buildXmlConfig(cfg)
    },
    generateAndShowXml() {
      this.generatedXml = this.buildCurrentXml()
      this.xmlDialogVisible = true
    },
    copyXml() {
      navigator.clipboard.writeText(this.generatedXml).then(() => {
        this.$message.success('已复制到剪贴板')
      }).catch(() => {
        // 降级方案
        const textarea = document.createElement('textarea')
        textarea.value = this.generatedXml
        document.body.appendChild(textarea)
        textarea.select()
        document.execCommand('copy')
        document.body.removeChild(textarea)
        this.$message.success('已复制到剪贴板')
      })
    },

    // ======== 方案管理 ========
    loadSchemeList() {
      listSchemes().then(res => {
        if (res.code === 200) {
          this.schemeList = res.data || []
        }
      }).catch(() => {})
    },

    getSchemeName(schemeId) {
      const found = this.schemeList.find(s => s.id === schemeId)
      return found ? found.schemeName : ''
    },

    handleSchemeSelect(schemeId) {
      if (!schemeId) {
        this.selectedSchemeId = null
        return
      }
      getScheme(schemeId).then(res => {
        if (res.code === 200 && res.data) {
          this.loadConfigFromXml(res.data.xmlConfig)
          this.$message.success('已加载方案: ' + res.data.schemeName)
        }
      }).catch(err => {
        this.$message.error('加载方案失败: ' + (err.message || '未知错误'))
      })
    },

    openSaveDialog() {
      this.saveForm = {
        schemeName: '',
        description: '',
        sheetName: this.config.sheetName || 'Sheet1'
      }
      this.showSaveDialog = true
    },

    confirmSaveScheme() {
      if (!this.saveForm.schemeName.trim()) {
        this.$message.warning('请输入方案名称')
        return
      }

      const data = {
        schemeName: this.saveForm.schemeName,
        description: this.saveForm.description,
        sheetName: this.config.sheetName || 'Sheet1',
        xmlConfig: this.buildCurrentXml()
      }

      const savePromise = this.selectedSchemeId
        ? updateScheme(this.selectedSchemeId, data)
        : saveScheme(data)

      savePromise.then(res => {
        if (res.code === 200) {
          this.$message.success('方案保存成功')
          this.showSaveDialog = false
          this.loadSchemeList()
        } else {
          this.$message.error(res.message || '保存失败')
        }
      }).catch(err => {
        this.$message.error('保存失败: ' + (err.message || '未知错误'))
      })
    },

    handleDeleteScheme(schemeId, schemeName) {
      this.$confirm('确认删除方案 "' + schemeName + '"？', '提示', {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        deleteScheme(schemeId).then(res => {
          if (res.code === 200) {
            this.$message.success('方案已删除')
            if (this.selectedSchemeId === schemeId) {
              this.selectedSchemeId = null
            }
            this.loadSchemeList()
          } else {
            this.$message.error(res.message || '删除失败')
          }
        }).catch(err => {
          this.$message.error('删除失败: ' + (err.message || '未知错误'))
        })
      }).catch(() => {})
    },

    loadConfigFromXml(xml) {
      try {
        const parsed = parseXmlConfig(xml)
        this.config.sheetName = parsed.sheetName || 'Sheet1'
        this.config.sheetIndex = parsed.sheetIndex
        this.config.sections = parsed.sections.map(s => ({ ...s, type: 'section' }))
        this.config.loops = parsed.loops.map(l => ({
          ...l,
          type: 'loop',
          varName: l.var,
          useDefaultBreak: l.breakConditions.length === 1 && l.breakConditions[0].cellChecks.length === 0
        }))
      } catch (err) {
        this.$message.error('配置解析失败: ' + err.message)
      }
    },

    // ======== API 调用 ========
    handlePreview() {
      if (!this.uploadedFile) {
        this.$message.warning('请先上传 Excel 文件')
        return
      }

      const formData = new FormData()
      formData.append('file', this.uploadedFile)
      const xmlConfig = this.buildCurrentXml()
      formData.append('xmlConfig', xmlConfig)

      this.previewLoading = true

      excelPreviewWithConfig(formData).then(res => {
        if (res.code === 200) {
          this.previewResultText = JSON.stringify(res.data, null, 2)
          this.previewDialogVisible = true
        } else {
          this.$message.error(res.message || '预览失败')
        }
      }).catch(err => {
        this.$message.error('请求失败: ' + (err.message || '未知错误'))
      }).finally(() => {
        this.previewLoading = false
      })
    },

    copyPreviewResult() {
      if (!this.previewResultText) return
      navigator.clipboard.writeText(this.previewResultText).then(() => {
        this.$message.success('JSON 已复制到剪贴板')
      }).catch(() => {
        const ta = document.createElement('textarea')
        ta.value = this.previewResultText
        document.body.appendChild(ta)
        ta.select()
        document.execCommand('copy')
        document.body.removeChild(ta)
        this.$message.success('JSON 已复制到剪贴板')
      })
    },

    gotoJsonPreview() {
      if (this.previewResultText) {
        sessionStorage.setItem('jsonPreviewData', this.previewResultText)
      }
      this.$router.push('/tools/jsonpreview')
    },

    handleExecute() {
      if (!this.uploadedFile) {
        this.$message.warning('请先上传 Excel 文件')
        return
      }

      this.$confirm('确认执行导入操作？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        const formData = new FormData()
        formData.append('file', this.uploadedFile)
        const xmlConfig = this.buildCurrentXml()
        formData.append('xmlConfig', xmlConfig)

        this.executeLoading = true
        this.resultData = null

        excelExecute(formData).then(res => {
          if (res.code === 200) {
            this.resultData = res.data
            this.resultTitle = '导入结果'
            if (res.data.success) {
              this.$message.success(`导入完成: 成功 ${res.data.successRows} 行`)
            } else {
              this.$message.error('导入失败: ' + (res.data.errorMessage || '未知错误'))
            }
          } else {
            this.$message.error(res.message || '导入失败')
          }
        }).catch(err => {
          this.$message.error('请求失败: ' + (err.message || '未知错误'))
        }).finally(() => {
          this.executeLoading = false
        })
      }).catch(() => {})
    },

  }
}
</script>

<style lang="scss" scoped>
.excel-import-page {
  padding: 16px;

  .page-card {
    margin-bottom: 16px;
  }

  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .help-alert {
    margin-bottom: 16px;
  }

  .section-block {
    margin-bottom: 24px;
    padding-bottom: 24px;
    border-bottom: 1px solid #ebeef5;

    &:last-child {
      border-bottom: none;
      margin-bottom: 0;
      padding-bottom: 0;
    }
  }

  .section-title {
    margin: 0 0 12px 0;
    font-size: 15px;
    font-weight: 600;
    color: #303133;
  }

  .config-row {
    display: flex;
    align-items: center;
    margin-bottom: 12px;
  }

  .config-label {
    font-size: 13px;
    color: #606266;
    margin-right: 8px;
    white-space: nowrap;
  }

  .config-actions {
    margin-bottom: 16px;
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  .config-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .config-item-card {
    border: 1px solid #e4e7ed;
    border-radius: 6px;
    overflow: hidden;
  }

  .config-item-header {
    display: flex;
    align-items: center;
    padding: 8px 12px;
    font-size: 13px;

    &.section-header {
      background: #ecf5ff;
    }

    &.loop-header {
      background: #f0f9eb;
    }
  }

  .config-type-badge {
    display: inline-block;
    padding: 2px 8px;
    border-radius: 4px;
    font-size: 11px;
    font-weight: 600;
    margin-right: 8px;

    &.section {
      background: #b3d8ff;
      color: #096dd9;
    }

    &.loop {
      background: #b7eb8f;
      color: #389e0d;
    }
  }

  .config-item-title {
    flex: 1;
    color: #303133;
  }

  .delete-btn {
    color: #f56c6c;
  }

  .config-item-body {
    padding: 12px;
  }

  .param-row {
    display: flex;
    align-items: center;
    margin-bottom: 8px;
    flex-wrap: wrap;
    gap: 4px;

    label {
      font-size: 12px;
      color: #606266;
      white-space: nowrap;
    }
  }

  .mappings-section {
    margin-top: 12px;
  }

  .mappings-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;
    font-size: 13px;
    color: #606266;
  }

  .break-condition-section {
    margin-top: 12px;
    padding: 8px;
    background: #fafafa;
    border-radius: 4px;
  }

  .break-condition-row {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-top: 4px;

    label {
      font-size: 12px;
      color: #606266;
    }
  }

  .action-bar {
    display: flex;
    gap: 12px;
    align-items: center;
  }

  .scheme-bar {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 20px;
    padding: 12px 16px;
    background: #fafafa;
    border-radius: 6px;
    border: 1px solid #e4e7ed;
  }

  .scheme-label {
    font-size: 13px;
    font-weight: 600;
    color: #303133;
    white-space: nowrap;
  }

  .xml-drop-zone {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    padding: 12px 16px;
    margin-bottom: 16px;
    border: 2px dashed #dcdfe6;
    border-radius: 6px;
    background: #fafafa;
    color: #909399;
    font-size: 13px;
    cursor: pointer;
    transition: all 0.3s;

    i {
      font-size: 20px;
      color: #c0c4cc;
    }

    em {
      color: #409eff;
      font-style: normal;
      text-decoration: underline;
    }

    &:hover {
      border-color: #409eff;
      background: #ecf5ff;

      i { color: #409eff; }
    }

    &--active {
      border-color: #67c23a;
      background: #f0f9eb;
      color: #67c23a;

      i { color: #67c23a; }
    }
  }

}
</style>
