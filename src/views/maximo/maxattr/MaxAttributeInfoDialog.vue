<template>
  <el-dialog :title="'字段详情 - ' + (currentRow ? currentRow.ATTRIBUTENAME : '')" :visible.sync="innerVisible" width="1500px" :close-on-click-modal="true"
    append-to-body
    v-loading.fullscreen.lock="allLoading"
    element-loading-text="拼命处理中,请耐心等待"
    element-loading-spinner="el-icon-loading"
    element-loading-background="rgba(0, 0, 0, 0.8)"
    @opened="onDialogOpened" @close="handleClose">
    <el-descriptions :column="3" border v-if="currentRow">
      <el-descriptions-item label="对象名">
        <el-input :value="currentRow.OBJECTNAME || '-'" readonly size="small" />
      </el-descriptions-item>
      <el-descriptions-item label="属性名">
        <el-input :value="currentRow.ATTRIBUTENAME || '-'" readonly size="small" />
      </el-descriptions-item>
      <el-descriptions-item label="标题">
        <el-input :value="currentRow.L_TITLE || '-'" readonly size="small" />
      </el-descriptions-item>
      <el-descriptions-item label="英文标题">
        <el-input :value="currentRow.TITLE || '-'" readonly size="small" />
      </el-descriptions-item>
      <el-descriptions-item label="别名">
        <el-input :value="currentRow.ALIAS || '-'" readonly size="small" />
      </el-descriptions-item>
      <el-descriptions-item label="数据类型">
        <el-input :value="currentRow.MAXTYPE || '-'" readonly size="small" />
      </el-descriptions-item>
      <el-descriptions-item label="长度">
        <el-input :value="val(currentRow.LENGTH)" readonly size="small" />
      </el-descriptions-item>
      <el-descriptions-item label="小数位数">
        <el-input :value="val(currentRow.SCALE)" readonly size="small" />
      </el-descriptions-item>
      <el-descriptions-item label="持久性">
        <el-input :value="val(currentRow.PERSISTENT)" readonly size="small" />
      </el-descriptions-item>
      <el-descriptions-item label="自动编号">
        <el-input :value="val(currentRow.CANAUTONUM)" readonly size="small" />
      </el-descriptions-item>
      <el-descriptions-item label="Java 类" :span="2">
        <el-input :value="currentRow.CLASSNAME || '-'" readonly size="small" />
      </el-descriptions-item>
      <el-descriptions-item label="列名">
        <el-input :value="currentRow.COLUMNNAME || '-'" readonly size="small" />
      </el-descriptions-item>
      <el-descriptions-item label="属性号">
        <el-input :value="val(currentRow.ATTRIBUTENO)" readonly size="small" />
      </el-descriptions-item>
      <el-descriptions-item label="域">
        <div style="display:flex;align-items:center">
          <el-input :value="currentRow.DOMAINID || '-'" readonly size="small" />
          <i v-if="currentRow.DOMAINID" class="el-icon-view" title="查看域详情" style="margin-left:6px;cursor:pointer;color:#409eff;font-size:18px" @click="showDomainDialog" />
        </div>
      </el-descriptions-item>
      <el-descriptions-item label="默认值">
        <el-input :value="currentRow.DEFAULTVALUE || '-'" readonly size="small" />
      </el-descriptions-item>
      <el-descriptions-item label="必需">
        <el-input :value="val(currentRow.REQUIRED)" readonly size="small" />
      </el-descriptions-item>
      <el-descriptions-item label="主键列序列">
        <el-input :value="val(currentRow.PRIMARYKEYCOLSEQ)" readonly size="small" />
      </el-descriptions-item>
      <el-descriptions-item label="搜索类型">
        <el-input :value="currentRow.SEARCHTYPE || '-'" readonly size="small" />
      </el-descriptions-item>
      <el-descriptions-item label="长文本所有者">
        <el-input :value="val(currentRow.ISLDOWNER)" readonly size="small" />
      </el-descriptions-item>
      <el-descriptions-item label="必须">
        <el-input :value="val(currentRow.MUSTBE)" readonly size="small" />
      </el-descriptions-item>
      <el-descriptions-item label="正向">
        <el-input :value="val(currentRow.ISPOSITIVE)" readonly size="small" />
      </el-descriptions-item>
      <el-descriptions-item label="限制">
        <el-input :value="val(currentRow.RESTRICTED)" readonly size="small" />
      </el-descriptions-item>
      <el-descriptions-item label="可本地化">
        <el-input :value="val(currentRow.LOCALIZABLE)" readonly size="small" />
      </el-descriptions-item>
      <el-descriptions-item label="用户定义">
        <el-input :value="val(currentRow.USERDEFINED)" readonly size="small" />
      </el-descriptions-item>
      <el-descriptions-item label="多语言支持">
        <el-input :value="val(currentRow.MLSUPPORTED)" readonly size="small" />
      </el-descriptions-item>
      <el-descriptions-item label="多语言使用中">
        <el-input :value="val(currentRow.MLINUSE)" readonly size="small" />
      </el-descriptions-item>
      <el-descriptions-item label="审计启用">
        <el-input :value="val(currentRow.EAUDITENABLED)" readonly size="small" />
      </el-descriptions-item>
      <el-descriptions-item label="电子签名启用">
        <el-input :value="val(currentRow.ESIGENABLED)" readonly size="small" />
      </el-descriptions-item>
      <el-descriptions-item label="实体">
        <el-input :value="currentRow.ENTITYNAME || '-'" readonly size="small" />
      </el-descriptions-item>
      <el-descriptions-item label="等同属性">
        <el-input :value="currentRow.SAMEASATTRIBUTE || '-'" readonly size="small" />
      </el-descriptions-item>
      <el-descriptions-item label="等同对象">
        <el-input :value="currentRow.SAMEASOBJECT || '-'" readonly size="small" />
      </el-descriptions-item>
      <el-descriptions-item label="自动键名">
        <el-input :value="currentRow.AUTOKEYNAME || '-'" readonly size="small" />
      </el-descriptions-item>
      <el-descriptions-item label="属性 ID">
        <el-input :value="val(currentRow.MAXATTRIBUTEID)" readonly size="small" />
      </el-descriptions-item>
      <el-descriptions-item label="行戳">
        <el-input :value="val(currentRow.ROWSTAMP)" readonly size="small" />
      </el-descriptions-item>
      <el-descriptions-item label="备注" :span="2">
        <el-input :value="currentRow.L_REMARKS || '-'" readonly type="textarea" :rows="3" />
      </el-descriptions-item>
      <el-descriptions-item label="英文备注" :span="2">
        <el-input :value="currentRow.REMARKS || '-'" readonly type="textarea" :rows="3" />
      </el-descriptions-item>
    </el-descriptions>

    <el-tabs v-model="activeTab" type="border-card" class="json-tabs">
      <el-tab-pane label="字段精简" name="fieldSimple">
        <div class="json-toolbar">
          <el-button type="primary" size="mini" icon="el-icon-document-copy" @click="copyFieldSimpleJson">复制字段精简JSON</el-button>
        </div>
        <div v-loading="detailLoading" element-loading-text="加载中..." class="monaco-wrapper">
          <div ref="fieldSimpleMonacoRef" class="monaco-container"></div>
        </div>
      </el-tab-pane>
      <el-tab-pane label="字段完整" name="fieldFull">
        <div class="json-toolbar">
          <el-button type="primary" size="mini" icon="el-icon-document" @click="copyFieldFullJson">复制字段完整JSON</el-button>
        </div>
        <div v-loading="detailLoading" element-loading-text="加载中..." class="monaco-wrapper">
          <div ref="fieldFullMonacoRef" class="monaco-container"></div>
        </div>
      </el-tab-pane>
      <el-tab-pane label="对象精简" name="objSimple">
        <div class="json-toolbar">
          <el-button type="primary" size="mini" icon="el-icon-document-copy" @click="copyObjSimpleJson">复制对象精简JSON</el-button>
        </div>
        <div v-loading="detailLoading" element-loading-text="加载中..." class="monaco-wrapper">
          <div ref="objSimpleMonacoRef" class="monaco-container"></div>
        </div>
      </el-tab-pane>
      <el-tab-pane label="对象完整" name="objFull">
        <div class="json-toolbar">
          <el-button type="primary" size="mini" icon="el-icon-document" @click="copyObjFullJson">复制对象完整JSON</el-button>
        </div>
        <div v-loading="detailLoading" element-loading-text="加载中..." class="monaco-wrapper">
          <div ref="objFullMonacoRef" class="monaco-container"></div>
        </div>
      </el-tab-pane>
      <el-tab-pane label="对象不更改主信息" name="objNoChangeMain">
        <div class="json-toolbar">
          <el-button type="primary" size="mini" icon="el-icon-document" @click="copyObjNoChangeMainJson">复制对象不更改主信息JSON</el-button>
        </div>
        <div class="monaco-wrapper">
          <div ref="objNoChangeMainMonacoRef" class="monaco-container"></div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <span slot="footer" class="dialog-footer">
      <el-button @click="innerVisible = false">关 闭</el-button>
    </span>

    <!-- 域详情弹窗 -->
    <max-domain-info-dialog :visible.sync="domainDialogVisible" :domainid="domainDialogId" />
  </el-dialog>
</template>

<script>
import { exportDbConfig } from '@/api/maxobject'
import MaxDomainInfoDialog from '@/views/maximo/domain/MaxDomainInfoDialog.vue'

export default {
  name: 'MaxAttributeInfoDialog',
  components: {
    MaxDomainInfoDialog
  },
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    row: {
      type: Object,
      default: () => null
    }
  },
  data() {
    return {
      innerVisible: false,
      allLoading: false,
      currentRow: null,
      simpleAttrData: null,
      simpleObjectData: null,
      fullAttrData: null,
      fullObjectData: null,
      detailLoading: false,
      activeTab: 'fieldSimple',
      fieldSimpleEditor: null,
      fieldFullEditor: null,
      objSimpleEditor: null,
      objFullEditor: null,
      objNoChangeMainEditor: null,
      monacoLoaded: false,
      _monaco: null,
      // 域详情
      domainDialogVisible: false,
      domainDialogId: ''
    }
  },
  watch: {
    visible: {
      immediate: true,
      handler(val) {
        this.innerVisible = val
        if (val && this.row) {
          this.loadDetail()
        }
      }
    },
    innerVisible(val) {
      if (!val) {
        this.$emit('update:visible', false)
      }
    },
    activeTab() {
      this.$nextTick(() => {
        this.layoutEditors()
      })
    }
  },
  methods: {
    val(v) {
      return (v === undefined || v === null || v === '') ? '-' : String(v)
    },
    handleClose() {
      this.disposeEditors()
    },
    loadDetail() {
      this.currentRow = this.row
      this.simpleAttrData = null
      this.simpleObjectData = null
      this.fullAttrData = null
      this.fullObjectData = null
      this.activeTab = 'fieldSimple'

      if (this.currentRow && this.currentRow.OBJECTNAME) {
        const objectName = this.currentRow.OBJECTNAME
        const attrName = this.currentRow.ATTRIBUTENAME ? this.currentRow.ATTRIBUTENAME.toUpperCase() : ''
        this.detailLoading = true
        this.allLoading = true
        Promise.all([
          exportDbConfig(objectName, true),
          exportDbConfig(objectName, false)
        ]).then(([simpleRes, fullRes]) => {
          if (simpleRes.status === 200 && simpleRes.data) {
            this.simpleObjectData = simpleRes.data
            if (simpleRes.data.maxObjects) {
              const maxObject = simpleRes.data.maxObjects.find(obj => obj.object === objectName)
              if (maxObject) {
                const attrData = (maxObject.attributes || []).find(
                  attr => (attr.attribute || '').toUpperCase() === attrName
                )
                this.simpleAttrData = attrData || null
              }
            }
          }
          if (fullRes.status === 200 && fullRes.data) {
            this.fullObjectData = fullRes.data
            if (fullRes.data.maxObjects) {
              const maxObject = fullRes.data.maxObjects.find(obj => obj.object === objectName)
              if (maxObject) {
                const attrData = (maxObject.attributes || []).find(
                  attr => (attr.attribute || '').toUpperCase() === attrName
                )
                this.fullAttrData = attrData || null
              }
            }
          }
          this.$nextTick(() => {
            setTimeout(() => {
              // 先结束 loading，Monaco 初始化推迟到 mask 淡出(300ms)结束后：
              // Monaco 首载解析会阻塞主线程，若在淡出期间执行会冻结 mask 半透明帧（灰色残留，点击才消失）
              this.detailLoading = false
              setTimeout(() => {
                this.initMonacoEditors()
              }, 500)
            }, 100)
          })
        }).catch(err => {
          console.error('获取字段详情失败:', err)
          this.detailLoading = false
        }).finally(() => {
          this.allLoading = false
        })
      }
    },
    onDialogOpened() {
      // loadDetail 已经处理
    },
    showDomainDialog() {
      if (this.currentRow && this.currentRow.DOMAINID) {
        this.domainDialogId = this.currentRow.DOMAINID
        this.domainDialogVisible = true
      }
    },
    initMonacoEditors() {
      if (!this.currentRow) return
      const fieldSimpleJson = this.simpleAttrData ? JSON.stringify(this.simpleAttrData, null, 2) : '{}'
      const fieldFullJson = this.fullAttrData ? JSON.stringify(this.fullAttrData, null, 2) : '{}'
      const objSimpleJson = this.simpleObjectData ? JSON.stringify(this.simpleObjectData, null, 2) : '{}'
      const objFullJson = this.fullObjectData ? JSON.stringify(this.fullObjectData, null, 2) : '{}'
      if (!this.monacoLoaded) {
        import(/* webpackChunkName: "monaco" */ 'monaco-editor').then(monaco => {
          this.monacoLoaded = true
          this._monaco = monaco
          this.createEditors(fieldSimpleJson, fieldFullJson, objSimpleJson, objFullJson)
        }).catch(err => {
          console.error('Monaco Editor 加载失败:', err)
        })
      } else {
        this.createEditors(fieldSimpleJson, fieldFullJson, objSimpleJson, objFullJson)
      }
    },
    createEditors(fieldSimpleJson, fieldFullJson, objSimpleJson, objFullJson) {
      const monaco = this._monaco
      const options = {
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
      }
      if (this.$refs.fieldSimpleMonacoRef && !this.fieldSimpleEditor) {
        this.fieldSimpleEditor = monaco.editor.create(this.$refs.fieldSimpleMonacoRef, { value: fieldSimpleJson, ...options })
      } else if (this.fieldSimpleEditor) {
        this.fieldSimpleEditor.setValue(fieldSimpleJson)
      }
      if (this.$refs.fieldFullMonacoRef && !this.fieldFullEditor) {
        this.fieldFullEditor = monaco.editor.create(this.$refs.fieldFullMonacoRef, { value: fieldFullJson, ...options })
      } else if (this.fieldFullEditor) {
        this.fieldFullEditor.setValue(fieldFullJson)
      }
      if (this.$refs.objSimpleMonacoRef && !this.objSimpleEditor) {
        this.objSimpleEditor = monaco.editor.create(this.$refs.objSimpleMonacoRef, { value: objSimpleJson, ...options })
      } else if (this.objSimpleEditor) {
        this.objSimpleEditor.setValue(objSimpleJson)
      }
      if (this.$refs.objFullMonacoRef && !this.objFullEditor) {
        this.objFullEditor = monaco.editor.create(this.$refs.objFullMonacoRef, { value: objFullJson, ...options })
      } else if (this.objFullEditor) {
        this.objFullEditor.setValue(objFullJson)
      }
      this.initObjNoChangeMainEditor(monaco, options)
      setTimeout(() => {
        this.layoutEditors()
      }, 100)
    },
    layoutEditors() {
      if (this.fieldSimpleEditor) this.fieldSimpleEditor.layout()
      if (this.fieldFullEditor) this.fieldFullEditor.layout()
      if (this.objSimpleEditor) this.objSimpleEditor.layout()
      if (this.objFullEditor) this.objFullEditor.layout()
      if (this.objNoChangeMainEditor) this.objNoChangeMainEditor.layout()
    },
    disposeEditors() {
      ;['fieldSimpleEditor', 'fieldFullEditor', 'objSimpleEditor', 'objFullEditor', 'objNoChangeMainEditor'].forEach(k => {
        if (this[k]) {
          this[k].dispose()
          this[k] = null
        }
      })
      this.monacoLoaded = false
      this._monaco = null
    },
    copyFieldSimpleJson() {
      this.copyToClipboard(this.simpleAttrData ? JSON.stringify(this.simpleAttrData, null, 2) : '{}', '字段精简JSON')
    },
    copyFieldFullJson() {
      this.copyToClipboard(this.fullAttrData ? JSON.stringify(this.fullAttrData, null, 2) : '{}', '字段完整JSON')
    },
    copyObjSimpleJson() {
      this.copyToClipboard(this.simpleObjectData ? JSON.stringify(this.simpleObjectData, null, 2) : '{}', '对象精简JSON')
    },
    copyObjFullJson() {
      this.copyToClipboard(this.fullObjectData ? JSON.stringify(this.fullObjectData, null, 2) : '{}', '对象完整JSON')
    },
    getObjNoChangeMainJson() {
      const objName = this.currentRow.OBJECTNAME || ''
      let description = ''
      const objectData = this.simpleObjectData || this.fullObjectData
      if (objectData && objectData.maxObjects) {
        const found = objectData.maxObjects.find(o => o.object === objName)
        if (found && found.description) description = found.description
      }
      return JSON.stringify({
        maxObjects: [
          {
            object: objName,
            description: description,
            ignoreObjectMain: true,
            attributes: [],
            relationships: []
          }
        ]
      }, null, 2)
    },
    initObjNoChangeMainEditor(monaco, options) {
      const json = this.getObjNoChangeMainJson()
      if (this.$refs.objNoChangeMainMonacoRef && !this.objNoChangeMainEditor) {
        this.objNoChangeMainEditor = monaco.editor.create(this.$refs.objNoChangeMainMonacoRef, { value: json, ...options })
      } else if (this.objNoChangeMainEditor) {
        this.objNoChangeMainEditor.setValue(json)
      }
    },
    copyObjNoChangeMainJson() {
      this.copyToClipboard(this.getObjNoChangeMainJson(), '对象不更改主信息JSON')
    },
    copyToClipboard(text, label) {
      if (navigator.clipboard && window.isSecureContext) {
        navigator.clipboard.writeText(text)
          .then(() => { this.$message.success(label + ' 已复制到剪贴板') })
          .catch(() => { this.fallbackCopy(text, label) })
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
.json-tabs {
  margin-top: 16px;
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
</style>
