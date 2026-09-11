<template>
  <el-dialog :title="'域详情 - ' + (currentRow ? currentRow.domainid : '')" :visible.sync="innerVisible" width="1500px" top="3vh" :close-on-click-modal="true"
    append-to-body
    v-loading.fullscreen.lock="allLoading"
    element-loading-text="拼命处理中,请耐心等待"
    element-loading-spinner="el-icon-loading"
    element-loading-background="rgba(0, 0, 0, 0.8)"
    @opened="onDialogOpened" @close="handleClose">
    <el-descriptions :column="4" border v-if="currentRow">
      <el-descriptions-item label="域ID">
        <el-input :value="currentRow.domainid || '-'" readonly size="small" />
      </el-descriptions-item>
      <el-descriptions-item label="域类型">
        <el-input :value="currentRow.domaintype || '-'" readonly size="small" />
      </el-descriptions-item>
      <el-descriptions-item label="中文描述">
        <el-input :value="currentRow.description_zh || currentRow.description || '-'" readonly size="small" />
      </el-descriptions-item>
      <el-descriptions-item label="英文描述">
        <el-input :value="currentRow.description_en || '-'" readonly size="small" />
      </el-descriptions-item>
      <el-descriptions-item label="数据类型">
        <el-input :value="currentRow.maxtype || '-'" readonly size="small" />
      </el-descriptions-item>
      <el-descriptions-item label="长度">
        <el-input :value="currentRow.length !== undefined && currentRow.length !== null ? currentRow.length : '-'" readonly size="small" />
      </el-descriptions-item>
      <el-descriptions-item label="小数位">
        <el-input :value="currentRow.scale !== undefined && currentRow.scale !== null ? currentRow.scale : '-'" readonly size="small" />
      </el-descriptions-item>
      <el-descriptions-item label="内置">
        <el-input :value="currentRow.internal !== undefined && currentRow.internal !== null ? currentRow.internal : '-'" readonly size="small" />
      </el-descriptions-item>
      <el-descriptions-item label="不缓存">
        <el-input :value="currentRow.nevercache !== undefined && currentRow.nevercache !== null ? currentRow.nevercache : '-'" readonly size="small" />
      </el-descriptions-item>
    </el-descriptions>

    <el-tabs v-model="activeTab" type="border-card" class="json-tabs">
      <el-tab-pane label="域值" name="domainValues">
        <div class="subtable-header">
          <span class="subtable-tip" v-if="valueTable">子表: {{ valueTable }}</span>
          <div class="subtable-actions">
            <el-tooltip :content="valueShowAllColumn ? '隐藏多余列' : '显示所有列'" placement="top">
              <el-button size="mini" circle :type="valueShowAllColumn ? 'success' : 'info'" icon="el-icon-menu" @click="toggleValueShowAllColumn" />
            </el-tooltip>
            <el-tooltip :content="valueShowPropName ? '隐藏属性名' : '显示属性名'" placement="top">
              <el-button size="mini" circle :type="valueShowPropName ? 'success' : 'info'" icon="el-icon-s-flag" @click="valueShowPropName = !valueShowPropName" />
            </el-tooltip>
          </div>
        </div>
        <el-table :data="domainValues" border stripe size="small" v-loading="subtableLoading" max-height="420" style="width: 100%">
          <el-table-column v-for="col in valueDisplayColumns" :key="col" :prop="col === '_TRANSLATIONS' ? '' : col" :label="valueShowPropName ? col : valueColumnLabel(col)" :show-overflow-tooltip="col !== '_TRANSLATIONS'" :min-width="col === '_TRANSLATIONS' ? 320 : 130">
            <template slot-scope="scope">
              <div v-if="col === '_TRANSLATIONS'" class="trans-cell">
                <template v-if="scope.row._TRANSLATIONS && scope.row._TRANSLATIONS.length">
                  <div v-for="(t, ti) in scope.row._TRANSLATIONS" :key="ti" class="trans-cell-row">
                    <span class="trans-cell-lang">{{ t.LANGCODE }}</span>
                    <span class="trans-cell-desc">{{ t.DESCRIPTION }}</span>
                  </div>
                </template>
                <span v-else class="cell-trans-empty">-</span>
              </div>
              <span v-else>{{ scope.row[col] }}</span>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!subtableLoading && domainValues.length === 0" description="无域值数据" />
      </el-tab-pane>
      <el-tab-pane label="域精简json" name="domainSimple">
        <div class="json-toolbar">
          <el-button type="primary" size="mini" icon="el-icon-document-copy" @click="copyDomainSimpleJson">复制域精简JSON</el-button>
        </div>
        <div v-loading="detailLoading" element-loading-text="加载中..." class="monaco-wrapper">
          <div ref="domainSimpleMonacoRef" class="monaco-container"></div>
        </div>
      </el-tab-pane>
      <el-tab-pane label="域完整json" name="domainFull">
        <div class="json-toolbar">
          <el-button type="primary" size="mini" icon="el-icon-document" @click="copyDomainFullJson">复制域完整JSON</el-button>
        </div>
        <div v-loading="detailLoading" element-loading-text="加载中..." class="monaco-wrapper">
          <div ref="domainFullMonacoRef" class="monaco-container"></div>
        </div>
      </el-tab-pane>
      <el-tab-pane label="本地化" name="domainTrans">
        <el-table :data="domainTranslations" border stripe size="small" v-loading="subtableLoading" max-height="420" style="width: 100%">
          <el-table-column prop="LANGCODE" label="语言" width="120" />
          <el-table-column prop="DESCRIPTION" label="本地化描述" show-overflow-tooltip />
        </el-table>
        <el-empty v-if="!subtableLoading && domainTranslations.length === 0" description="无本地化数据" />
      </el-tab-pane>
    </el-tabs>

    <span slot="footer" class="dialog-footer">
      <el-button @click="innerVisible = false">关 闭</el-button>
    </span>
  </el-dialog>
</template>

<script>
import { exportDomains, getDomainSubtables } from '@/api/domain'

export default {
  name: 'MaxDomainInfoDialog',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    domainid: {
      type: String,
      default: ''
    },
    domaintype: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      innerVisible: false,
      allLoading: false,
      pendingCount: 0,
      detailLoading: false,
      currentRow: null,
      apiSimpleDomains: [],
      apiFullDomains: [],
      activeTab: 'domainValues',
      domainSimpleEditor: null,
      domainFullEditor: null,
      monacoLoaded: false,
      _monaco: null,
      subtableLoading: false,
      domainValues: [],
      valueTable: '',
      allValueColumns: [],
      valueDisplayColumns: [],
      valueShowAllColumn: false,
      valueShowPropName: false,
      domainTranslations: []
    }
  },
  watch: {
    visible: {
      immediate: true,
      handler(val) {
        this.innerVisible = val
        if (val) {
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
        this.layoutDetailEditors()
      })
    }
  },
  methods: {
    beginLoading() {
      this.pendingCount++
      this.allLoading = true
    },
    endLoading() {
      this.pendingCount--
      if (this.pendingCount <= 0) {
        this.pendingCount = 0
        this.allLoading = false
      }
    },
    handleClose() {
      this.disposeDetailEditors()
    },
    escapeSql(v) {
      return String(v || '').replace(/'/g, "''")
    },
    valueColumnLabel(col) {
      const map = { VALUE: '值', DESCRIPTION: '描述', MAXVALUE: '内部值', DEFAULTS: '默认值', _TRANSLATIONS: '多语言' }
      return map[col] || col
    },
    isHiddenValueColumn(col) {
      if (['SITEID', 'ORGID', 'ROWSTAMP', 'VALUEID', 'DEFAULTS'].includes(col)) return true
      if (col === 'MAXVALUE') {
        return !(this.currentRow && this.currentRow.domaintype === 'SYNONYM')
      }
      if (col.endsWith('ID') && col !== 'DOMAINID' && col !== 'VALUEID') return true
      return false
    },
    applyValueColumns() {
      if (this.valueShowAllColumn) {
        this.valueDisplayColumns = this.allValueColumns.slice()
      } else {
        this.valueDisplayColumns = this.allValueColumns.filter(c => !this.isHiddenValueColumn(c))
      }
    },
    toggleValueShowAllColumn() {
      this.valueShowAllColumn = !this.valueShowAllColumn
      this.applyValueColumns()
    },
    loadDetail() {
      if (!this.domainid) return
      this.detailLoading = true
      this.currentRow = { domainid: this.domainid, domaintype: this.domaintype }
      this.apiSimpleDomains = []
      this.apiFullDomains = []
      this.domainValues = []
      this.domainTranslations = []
      this.allValueColumns = []
      this.valueDisplayColumns = []
      this.valueShowAllColumn = false
      this.valueShowPropName = false
      this.activeTab = 'domainValues'

      const whereClause = "DOMAINID = '" + this.escapeSql(this.domainid) + "'"

      // 域类型已知时直接加载子表；未知时等待完整导出返回后解析 domaintype 再加载
      if (this.domaintype) {
        this.loadSubtables(this.domaintype)
      } else {
        // 尚无子表请求在途，需保证计数不为零（loadSubtables 内部会再 +1）
        this.beginLoading()
      }

      Promise.all([
        exportDomains({
          _langcode: 'ZH',
          apiType: 'manage',
          _action: 'export',
          ignoreDefVal: 'true'
        }, {
          where: whereClause
        }),
        exportDomains({
          _langcode: 'ZH',
          apiType: 'manage',
          _action: 'export'
        }, {
          where: whereClause
        })
      ]).then(([simpleRes, fullRes]) => {
        const simpleData = simpleRes.data || simpleRes
        const fullData = fullRes.data || fullRes

        if (simpleData && simpleData.status === 'error') {
          this.$message.error('获取域精简失败: ' + (simpleData.message || '未知错误'))
        } else if (simpleData && simpleData.domains && simpleData.domains.length > 0) {
          this.apiSimpleDomains = simpleData.domains
        }

        if (fullData && fullData.status === 'error') {
          this.$message.error('获取域完整失败: ' + (fullData.message || '未知错误'))
        } else if (fullData && fullData.domains && fullData.domains.length > 0) {
          this.apiFullDomains = fullData.domains
          this.currentRow = Object.assign({}, this.currentRow, fullData.domains[0])
          // 打开时未传入 domaintype（如从字段详情进入），用解析出的类型加载子表
          if (!this.domaintype && this.currentRow.domaintype) {
            this.loadSubtables(this.currentRow.domaintype)
          }
        }
      }).catch(err => {
        this.$message.error('获取域详情失败: ' + (err.message || String(err)))
      }).finally(() => {
        this.detailLoading = false
        this.endLoading()
        this.$nextTick(() => {
          // 延时须大于 loading mask 淡出动画时长(300ms)：
          // Monaco 首载解析会阻塞主线程，若在淡出期间执行会冻结 mask 半透明帧（灰色残留，点击才消失）
          setTimeout(() => {
            this.initDetailEditors()
          }, 400)
        })
      })
    },
    loadSubtables(domaintype) {
      this.subtableLoading = true
      this.beginLoading()
      getDomainSubtables(this.domainid, domaintype).then(res => {
        if (res.code === 200 && res.data) {
          this.domainValues = res.data.values || []
          this.valueTable = res.data.valueTable || ''
          this.domainTranslations = res.data.translations || []
          if (this.domainValues.length > 0) {
            const orderedKeys = []
            const seen = {}
            let hasTranslations = false
            this.domainValues.forEach(row => {
              Object.keys(row).forEach(k => {
                if (k === '_TRANSLATIONS') {
                  if (Array.isArray(row._TRANSLATIONS) && row._TRANSLATIONS.length) hasTranslations = true
                  return
                }
                if (!seen[k]) {
                  seen[k] = true
                  orderedKeys.push(k)
                }
              })
            })
            if (!seen['DESCRIPTION']) {
              seen['DESCRIPTION'] = true
              const valueIdx = orderedKeys.indexOf('VALUE')
              if (valueIdx >= 0) {
                orderedKeys.splice(valueIdx + 1, 0, 'DESCRIPTION')
              } else {
                orderedKeys.push('DESCRIPTION')
              }
            }
            const cols = []
            orderedKeys.forEach(k => {
              cols.push(k)
              if (k === 'DESCRIPTION' && hasTranslations) cols.push('_TRANSLATIONS')
            })
            if (hasTranslations && cols.indexOf('_TRANSLATIONS') === -1) {
              cols.push('_TRANSLATIONS')
            }
            this.allValueColumns = cols
            this.applyValueColumns()
          }
        } else {
          this.$message.error('获取子表信息失败: ' + (res.message || '未知错误'))
        }
      }).catch(err => {
        this.$message.error('获取子表信息失败: ' + (err.message || String(err)))
      }).finally(() => {
        this.subtableLoading = false
        this.endLoading()
      })
    },
    onDialogOpened() {
      this.$nextTick(() => {
        // 同上：避开 loading mask 淡出窗口期，防止 Monaco 首载阻塞主线程导致 mask 冻结
        setTimeout(() => {
          this.initDetailEditors()
        }, 400)
      })
    },
    copyDomainSimpleJson() {
      if (this.apiSimpleDomains && this.apiSimpleDomains.length > 0) {
        this.copyToClipboard(JSON.stringify(this.apiSimpleDomains[0], null, 2), '域精简JSON')
      }
    },
    copyDomainFullJson() {
      if (this.apiFullDomains && this.apiFullDomains.length > 0) {
        this.copyToClipboard(JSON.stringify({ domains: this.apiFullDomains }, null, 2), '域完整JSON')
      }
    },
    initDetailEditors() {
      if (!this.currentRow) return
      if (!this.monacoLoaded) {
        import(/* webpackChunkName: "monaco" */ 'monaco-editor').then(monaco => {
          this.monacoLoaded = true
          this._monaco = monaco
          this.createDetailEditors()
        }).catch(err => {
          console.error('Monaco Editor 加载失败:', err)
        })
      } else {
        this.createDetailEditors()
      }
    },
    createDetailEditors() {
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
        renderLineHighlight: 'none'
      }
      let simpleJson = ''
      if (this.apiSimpleDomains && this.apiSimpleDomains.length > 0) {
        simpleJson = JSON.stringify(this.apiSimpleDomains[0], null, 2)
      }
      if (this.$refs.domainSimpleMonacoRef && !this.domainSimpleEditor) {
        this.domainSimpleEditor = monaco.editor.create(this.$refs.domainSimpleMonacoRef, {
          value: simpleJson,
          ...options
        })
      } else if (this.domainSimpleEditor) {
        this.domainSimpleEditor.setValue(simpleJson)
      }

      let fullJson = ''
      if (this.apiFullDomains && this.apiFullDomains.length > 0) {
        fullJson = JSON.stringify({ domains: this.apiFullDomains }, null, 2)
      }
      if (this.$refs.domainFullMonacoRef && !this.domainFullEditor) {
        this.domainFullEditor = monaco.editor.create(this.$refs.domainFullMonacoRef, {
          value: fullJson,
          ...options
        })
      } else if (this.domainFullEditor) {
        this.domainFullEditor.setValue(fullJson)
      }

      setTimeout(() => {
        this.layoutDetailEditors()
      }, 100)
    },
    layoutDetailEditors() {
      if (this.domainSimpleEditor) this.domainSimpleEditor.layout()
      if (this.domainFullEditor) this.domainFullEditor.layout()
    },
    disposeDetailEditors() {
      if (this.domainSimpleEditor) {
        this.domainSimpleEditor.dispose()
        this.domainSimpleEditor = null
      }
      if (this.domainFullEditor) {
        this.domainFullEditor.dispose()
        this.domainFullEditor = null
      }
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
.json-tabs {
  margin-top: 16px;
}
.subtable-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.subtable-tip {
  color: #909399;
  font-size: 12px;
  margin: 0;
}
.subtable-actions {
  display: flex;
  gap: 4px;
}
.cell-trans-empty {
  color: #909399;
  font-size: 12px;
}
.trans-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.trans-cell-row {
  display: flex;
  align-items: center;
  line-height: 20px;
}
.trans-cell-lang {
  flex: 0 0 60px;
  color: #909399;
  font-size: 12px;
  padding-right: 8px;
}
.trans-cell-desc {
  flex: 1;
  color: #303133;
  word-break: break-all;
}
.json-toolbar {
  margin-bottom: 8px;
  text-align: right;
}
.monaco-wrapper {
  position: relative;
}
.monaco-container {
  height: 400px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}
</style>
