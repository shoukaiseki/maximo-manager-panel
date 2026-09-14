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
      <el-tab-pane label="结构化" name="structured">
        <div v-if="!structDomain" v-loading="detailLoading" style="min-height: 200px;"></div>
        <template v-else>
          <!-- ALN / SYNONYM / NUMERIC 域值 + 条件编号 -->
          <el-table
            v-if="isStructValueType"
            :data="structValues"
            border
            stripe
            size="small"
            v-loading="detailLoading"
            max-height="520"
            style="width: 100%">
            <el-table-column v-if="structDomain.domaintype === 'SYNONYM'" prop="maxvalue" label="内部值" width="130" show-overflow-tooltip />
            <el-table-column prop="value" label="值" width="150" show-overflow-tooltip />
            <el-table-column label="描述" min-width="200" show-overflow-tooltip>
              <template slot-scope="scope">
                <span>{{ scope.row.description_zh || scope.row.description || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column v-if="structDomain.domaintype === 'SYNONYM'" prop="defaults" label="默认" width="70" align="center" />
            <el-table-column prop="orgid" label="组织" width="100" show-overflow-tooltip>
              <template slot-scope="scope">{{ scope.row.orgid || '-' }}</template>
            </el-table-column>
            <el-table-column prop="siteid" label="站点" width="100" show-overflow-tooltip>
              <template slot-scope="scope">{{ scope.row.siteid || '-' }}</template>
            </el-table-column>
            <el-table-column label="条件编号" min-width="220">
              <template slot-scope="scope">
                <template v-if="scope.row.maxdomvalcond && scope.row.maxdomvalcond.length">
                  <el-tag
                    v-for="(c, ci) in scope.row.maxdomvalcond"
                    :key="ci"
                    size="mini"
                    type="warning"
                    class="cond-tag">
                    {{ c.conditionnum }}<span v-if="c.objectname" class="cond-obj"> / {{ c.objectname }}</span>
                  </el-tag>
                </template>
                <span v-else class="cell-trans-empty">-</span>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-else-if="!isStructNumRange && !isStructTableLike" :description="'暂不支持 ' + structDomain.domaintype + ' 类型的结构化展示'" />

          <!-- NUMRANGE 数值范围 -->
          <template v-if="isStructNumRange">
            <el-table :data="structDomain.numrangedomain || []" border stripe size="small" max-height="520" style="width: 100%">
              <el-table-column prop="rangesegment" label="段" width="80" align="center" />
              <el-table-column prop="rangeminimum" label="最小值" width="140" />
              <el-table-column prop="rangemaximum" label="最大值" width="140" />
              <el-table-column prop="rangeinterval" label="间隔" width="140" />
              <el-table-column label="组织" width="120">
                <template slot-scope="scope">{{ scope.row.orgid || '-' }}</template>
              </el-table-column>
              <el-table-column label="站点" width="120">
                <template slot-scope="scope">{{ scope.row.siteid || '-' }}</template>
              </el-table-column>
            </el-table>
            <el-empty v-if="!(structDomain.numrangedomain || []).length" description="无数值范围数据" />
          </template>

          <!-- TABLE / CROSSOVER: 对象、where 子句、错误消息键 -->
          <template v-if="isStructTableLike">
            <div v-for="(tb, tIdx) in (structDomain.tabledomain || [])" :key="tIdx" class="struct-block">
              <div class="struct-sub-title">表域定义 #{{ tIdx + 1 }}</div>
              <el-descriptions :column="2" border size="small">
                <el-descriptions-item label="对象名">{{ tb.objectname || '-' }}</el-descriptions-item>
                <el-descriptions-item label="组织 / 站点">{{ [tb.orgid, tb.siteid].filter(function(x) { return !!x }).join(' / ') || '-' }}</el-descriptions-item>
                <el-descriptions-item label="验证 Where 子句" :span="2">
                  <pre v-if="tb.validtnwhereclause" class="where-pre">{{ tb.validtnwhereclause }}</pre>
                  <span v-else class="cell-trans-empty">-</span>
                </el-descriptions-item>
                <el-descriptions-item label="列表 Where 子句" :span="2">
                  <pre v-if="tb.listwhereclause" class="where-pre">{{ tb.listwhereclause }}</pre>
                  <span v-else class="cell-trans-empty">-</span>
                </el-descriptions-item>
                <el-descriptions-item label="错误消息资源包">{{ tb.errorresourcbundle || '-' }}</el-descriptions-item>
                <el-descriptions-item label="错误消息键">{{ tb.erroraccesskey || '-' }}</el-descriptions-item>
              </el-descriptions>

              <!-- CROSSOVER 字段映射表 -->
              <template v-if="structDomain.domaintype === 'CROSSOVER'">
                <div class="struct-sub-title">字段映射（{{ (tb.crossoverdomain || []).length }}）</div>
                <el-table :data="tb.crossoverdomain || []" border stripe size="small" max-height="400" style="width: 100%">
                  <el-table-column prop="sourcefield" label="源字段" min-width="160" show-overflow-tooltip />
                  <el-table-column label="" width="40" align="center">
                    <template><i class="el-icon-right" /></template>
                  </el-table-column>
                  <el-table-column prop="destfield" label="目标字段" min-width="160" show-overflow-tooltip />
                  <el-table-column prop="sourcecondition" label="源条件" min-width="160" show-overflow-tooltip>
                    <template slot-scope="scope">{{ scope.row.sourcecondition || '-' }}</template>
                  </el-table-column>
                  <el-table-column prop="destcondition" label="目标条件" min-width="160" show-overflow-tooltip>
                    <template slot-scope="scope">{{ scope.row.destcondition || '-' }}</template>
                  </el-table-column>
                  <el-table-column prop="copyevenifsrcnull" label="源空也复制" width="100" align="center">
                    <template slot-scope="scope">
                      <el-tag size="mini" :type="isTruthy(scope.row.copyevenifsrcnull) ? 'success' : 'info'">{{ isTruthy(scope.row.copyevenifsrcnull) ? '是' : '否' }}</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="copyonlyifdestnull" label="仅目标空复制" width="110" align="center">
                    <template slot-scope="scope">
                      <el-tag size="mini" :type="isTruthy(scope.row.copyonlyifdestnull) ? 'success' : 'info'">{{ isTruthy(scope.row.copyonlyifdestnull) ? '是' : '否' }}</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="sequence" label="序号" width="70" align="center" />
                </el-table>
                <el-empty v-if="!(tb.crossoverdomain || []).length" description="无字段映射" :image-size="60" />
              </template>
            </div>
            <el-empty v-if="!(structDomain.tabledomain || []).length" description="无表域定义数据" />
          </template>
        </template>
      </el-tab-pane>
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
import { exportDomains, exportDomainObjects, getDomainSubtables } from '@/api/domain'

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
      // 结构化详情(来自 SKS.AUTOSCRIPT.OBJECTS)
      structDomain: null,
      activeTab: 'structured',
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
  computed: {
    structType() {
      return this.structDomain ? this.structDomain.domaintype : ''
    },
    // ALN / SYNONYM / NUMERIC 值域值
    isStructValueType() {
      return ['ALN', 'SYNONYM', 'NUMERIC'].indexOf(this.structType) >= 0
    },
    isStructNumRange() {
      return this.structType === 'NUMRANGE'
    },
    isStructTableLike() {
      return this.structType === 'TABLE' || this.structType === 'CROSSOVER'
    },
    structValues() {
      if (!this.structDomain) return []
      if (this.structType === 'ALN') return this.structDomain.alndomain || []
      if (this.structType === 'SYNONYM') return this.structDomain.synonymdomain || []
      if (this.structType === 'NUMERIC') return this.structDomain.numericdomain || []
      return []
    }
  },
  methods: {
    // YORN 字段兼容 1/0、"1"/"0"、"Y"/"N"、true/false
    isTruthy(v) {
      return v === true || v === 1 || v === '1' || v === 'Y'
    },
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
      this.structDomain = null
      this.domainValues = []
      this.domainTranslations = []
      this.allValueColumns = []
      this.valueDisplayColumns = []
      this.valueShowAllColumn = false
      this.valueShowPropName = false
      this.activeTab = 'structured'

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
        }),
        exportDomainObjects({
          _langcode: 'ZH'
        }, {
          where: whereClause
        })
      ]).then(([simpleRes, fullRes, structRes]) => {
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

        const structData = structRes.data || structRes
        if (structData && structData.status === 'error') {
          this.$message.error('获取结构化域详情失败: ' + (structData.message || '未知错误'))
        } else if (structData && structData.domains && structData.domains.length > 0) {
          this.structDomain = structData.domains[0]
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
.struct-block {
  margin-bottom: 18px;
}
.struct-sub-title {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  margin: 10px 0 6px 0;
  padding-left: 8px;
  border-left: 3px solid #409eff;
}
.where-pre {
  margin: 0;
  padding: 6px 8px;
  background: #f5f7fa;
  border: 1px solid #ebeef5;
  border-radius: 3px;
  font-family: Consolas, Menlo, Monaco, monospace;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
  color: #303133;
}
.cond-tag {
  margin: 2px 4px 2px 0;
}
.cond-obj {
  color: #e6a23c;
  font-weight: 600;
}
</style>
