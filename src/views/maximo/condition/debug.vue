<template>
  <section class="query-page">
    <el-card>
      <div class="page-header-row">
        <div>
          <h2>条件表达式调试</h2>
          <p class="page-summary">按 MBO 对象名 + 过滤条件定位唯一记录，使用 Maximo PARSER 服务（与工作流/签名条件一致）校验并执行条件表达式。</p>
        </div>
        <div class="page-actions">
          <el-button icon="el-icon-question" size="mini" circle title="TreeNode 类型说明" @click="treeNodeHelpVisible = true" />
          <el-button type="primary" icon="el-icon-document-add" size="mini" @click="openSaveDialog">保存记录</el-button>
          <el-button icon="el-icon-document-copy" size="mini" @click="openSaveAsDialog">另存为</el-button>
          <el-button icon="el-icon-folder-opened" size="mini" @click="openRecordsDialog">调试记录</el-button>
        </div>
      </div>

      <el-form ref="form" :model="form" :rules="rules" label-width="140px" size="small" @submit.native.prevent>
        <el-form-item label="MBO对象名称" prop="mboName">
          <el-input v-model="form.mboName" placeholder="如 WORKORDER、ASSET" clearable style="width: 320px;" @keyup.enter.native="handleDebug" />
        </el-form-item>
        <el-form-item label="MBO记录过滤条件" prop="mboWhere">
          <el-input v-model="form.mboWhere" type="textarea" :autosize="{ minRows: 2, maxRows: 6 }" placeholder="MboSet.setWhere 条件，如 wonum = '1000' and siteid = 'BEDFORD'" />
        </el-form-item>
        <el-form-item label="条件表达式类型" prop="condType">
          <el-radio-group v-model="form.condType">
            <el-radio label="WORKFLOW">工作流</el-radio>
            <el-radio label="SIGOPTION">签名</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="解析方式">
          <el-radio-group v-model="form.parserType">
            <el-radio label="SYSTEM">系统解析器</el-radio>
            <el-radio label="CUSTOM">自定义解析器(可查看sql)</el-radio>
          </el-radio-group>
          <div class="field-tip">
            系统解析器为 Maximo PARSER 内存求值，无法输出 SQL；自定义解析器按 Maximo 语法自行解析求值（支持 :字段、子查询），并返回执行日志。
          </div>
        </el-form-item>
        <el-form-item label="条件表达式内容" prop="expression">
          <MonacoJsonEditor v-model="form.expression" language="sql" height="140px" word-wrap="on" :font-size="14" />
        </el-form-item>
        <el-form-item label="附加输出字段">
          <el-input v-model="form.fields" placeholder="可选，逗号分隔，如 wonum,status,siteid" clearable style="width: 320px;" />
        </el-form-item>
        <el-form-item label="多条匹配候选数">
          <el-input-number v-model="form.maxRows" :min="1" :max="100" controls-position="right" style="width: 140px;" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-caret-right" size="mini" :loading="loading" @click="handleDebug">调 试</el-button>
          <el-button icon="el-icon-refresh" size="mini" @click="resetForm">重 置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-if="resp" class="result-card">
      <div slot="header" class="result-header">
        <span>执行结果</span>
        <span class="result-tag">
          <template v-if="body.status === 'success'">
            <el-tag v-if="resultData.result === true" type="success" effect="dark">TRUE</el-tag>
            <el-tag v-else-if="resultData.result === false" type="danger" effect="dark">FALSE</el-tag>
            <el-tag v-else type="info">无结果</el-tag>
          </template>
          <el-tag v-else type="danger">执行失败</el-tag>
        </span>
      </div>

      <!-- 错误提示 -->
      <el-alert v-if="body.status === 'error'" :title="body.message || '执行失败'" type="error" :closable="false" show-icon class="error-alert" />

      <!-- 输出信息 -->
      <el-descriptions :column="2" border size="small" class="out-desc">
        <el-descriptions-item label="MBO对象名称">{{ resultData.mboName || form.mboName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="条件表达式类型">{{ condTypeLabel(resultData.condType) }}</el-descriptions-item>
        <el-descriptions-item label="MBO记录过滤条件" :span="2">{{ resultData.mboWhere || form.mboWhere || '-' }}</el-descriptions-item>
        <el-descriptions-item label="条件表达式内容" :span="2">
          <span class="mono">{{ resultData.expression || form.expression || '-' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="匹配记录数">{{ resultData.matchCount === undefined ? '-' : resultData.matchCount }}</el-descriptions-item>
        <el-descriptions-item label="解析方式">{{ parserTypeLabel(resultData.parserType) }}</el-descriptions-item>
        <el-descriptions-item label="求值方式" :span="2">{{ (resultData.detail && (resultData.detail.resultSourceText || resultData.detail.resultSource)) || '-' }}</el-descriptions-item>
        <el-descriptions-item label="记录主键" :span="2">{{ resultData.recordKeyText || '-' }}</el-descriptions-item>
        <el-descriptions-item label="耗时" :span="2">
          <template v-if="resultData.timing">
            匹配 {{ resultData.timing.countMs }}ms / 解析 {{ resultData.timing.parseMs }}ms / 执行 {{ resultData.timing.evalMs }}ms / 合计 {{ resultData.timing.totalMs }}ms
          </template>
          <span v-else>-</span>
        </el-descriptions-item>
      </el-descriptions>

      <!-- 唯一记录字段 -->
      <div v-if="recordRows.length > 0" class="record-block">
        <div class="block-title">匹配记录</div>
        <el-table :data="recordRows" border stripe size="mini" max-height="300">
          <el-table-column prop="field" label="字段" width="200" />
          <el-table-column prop="value" label="值" show-overflow-tooltip />
        </el-table>
      </div>

      <!-- 多条匹配候选 -->
      <div v-if="candidateColumns.length > 0" class="record-block">
        <div class="block-title">多条匹配候选（共 {{ resultData.matchCount }} 条，显示前 {{ resultData.candidates.length }} 条）</div>
        <el-table :data="resultData.candidates" border stripe size="mini" max-height="360">
          <el-table-column v-for="col in candidateColumns" :key="col" :prop="col" :label="col" min-width="120" show-overflow-tooltip />
        </el-table>
      </div>

      <!-- 自定义解析器生成的 SQL -->
      <div v-if="resultData.sql" class="record-block">
        <div class="block-title">执行 SQL（自定义解析器）</div>
        <pre class="raw-json sql-text">{{ resultData.sql }}</pre>
      </div>

      <!-- 执行日志 -->
      <div v-if="evalLogLines.length" class="record-block">
        <div class="block-title">执行日志</div>
        <pre class="raw-json log-text">{{ evalLogLines.join('\n') }}</pre>
      </div>

      <!-- 调试详细信息 -->
      <el-collapse v-model="activeDetail" class="detail-collapse">
        <el-collapse-item name="detail" title="调试输出详细信息">
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="解析器">{{ (resultData.detail && resultData.detail.parser) || '-' }}</el-descriptions-item>
            <el-descriptions-item label="校验对象">{{ (resultData.detail && resultData.detail.zombieObject) || '-' }}</el-descriptions-item>
            <el-descriptions-item label="校验模式">{{ (resultData.detail && resultData.detail.validateMode) || '-' }}</el-descriptions-item>
            <el-descriptions-item label="求值方式">{{ (resultData.detail && (resultData.detail.resultSourceText || resultData.detail.resultSource)) || '-' }}</el-descriptions-item>
            <el-descriptions-item label="参考实现">{{ (resultData.detail && resultData.detail.expressionBuilderRef) || '-' }}</el-descriptions-item>
            <el-descriptions-item label="完整 where" :span="2"><span class="mono">{{ resultData.completeWhere || '-' }}</span></el-descriptions-item>
            <el-descriptions-item label="语法校验">
              <el-tag v-if="resultData.detail" :type="resultData.detail.parseOk ? 'success' : 'danger'" size="mini">{{ resultData.detail.parseOk ? '通过' : '失败' }}</el-tag>
              <span v-else>-</span>
            </el-descriptions-item>
            <el-descriptions-item label="校验消息">{{ (resultData.detail && resultData.detail.parseMessage) || '-' }}</el-descriptions-item>
            <el-descriptions-item label="解析类型">
              <span v-if="resultData.nodeDataType !== undefined && resultData.nodeDataType !== null">{{ resultData.nodeDataTypeName }} ({{ resultData.nodeDataType }})</span>
              <span v-else>-</span>
            </el-descriptions-item>
            <el-descriptions-item label="类型消息" :span="2">{{ (resultData.detail && resultData.detail.nodeDataTypeError) || '-' }}</el-descriptions-item>
            <el-descriptions-item label="求值结果">{{ resultData.detail && resultData.detail.memResult !== undefined && resultData.detail.memResult !== null ? resultData.detail.memResult : '-' }}</el-descriptions-item>
            <el-descriptions-item label="执行消息" :span="2">{{ (resultData.detail && resultData.detail.evalMessage) || '-' }}</el-descriptions-item>
          </el-descriptions>
          <div class="block-title block-title-row">
            <span>原始返回</span>
            <el-button type="text" size="mini" icon="el-icon-copy-document" @click="copyRaw">复制</el-button>
          </div>
          <pre class="raw-json">{{ rawText }}</pre>
        </el-collapse-item>
        <el-collapse-item v-if="body.errorMessageTrace" name="trace" title="错误详细提示">
          <pre class="raw-json">{{ body.errorMessageTrace }}</pre>
        </el-collapse-item>
      </el-collapse>
    </el-card>

    <!-- 保存调试记录 -->
    <el-dialog title="保存调试记录" :visible.sync="saveDialog.visible" width="560px" :close-on-click-modal="false">
      <el-form :model="saveDialog" label-width="90px" size="small">
        <el-form-item label="记录名称" required>
          <el-input v-model="saveDialog.recordName" placeholder="如 工作单审批条件" maxlength="200" show-word-limit @keyup.enter.native="submitSave" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="saveDialog.description" type="textarea" :autosize="{ minRows: 2, maxRows: 4 }" placeholder="可选" />
        </el-form-item>
      </el-form>
      <div class="save-preview">
        <div>对象：{{ form.mboName || '-' }}</div>
        <div>类型：{{ condTypeLabel(form.condType) }}</div>
        <div>解析方式：{{ parserTypeLabel(form.parserType) }}</div>
        <div>过滤条件：{{ form.mboWhere || '-' }}</div>
        <div>表达式：{{ form.expression || '-' }}</div>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="saveDialog.visible = false">取 消</el-button>
        <el-button type="primary" :loading="saveDialog.loading" @click="submitSave">保 存</el-button>
      </span>
    </el-dialog>

    <!-- 调试记录列表 -->
    <el-dialog title="调试记录" :visible.sync="recordDialog.visible" width="960px" top="5vh">
      <div class="record-toolbar">
        <el-input v-model="recordDialog.keyword" placeholder="按名称 / 对象 / 表达式搜索" clearable size="mini" style="width: 260px;" @keyup.enter.native="fetchRecords" />
        <el-button size="mini" icon="el-icon-search" @click="fetchRecords">搜索</el-button>
      </div>
      <el-table v-loading="recordDialog.loading" :data="recordDialog.list" border stripe size="mini" max-height="440">
        <el-table-column prop="recordName" label="记录名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="mboName" label="MBO对象" width="140" show-overflow-tooltip />
        <el-table-column label="类型" width="80">
          <template slot-scope="scope">{{ scope.row.condType === 'SIGOPTION' ? '签名' : '工作流' }}</template>
        </el-table-column>
        <el-table-column label="解析器" width="90">
          <template slot-scope="scope">{{ scope.row.parserType === 'CUSTOM' ? '自定义SQL' : '系统' }}</template>
        </el-table-column>
        <el-table-column prop="mboWhere" label="过滤条件" min-width="200" show-overflow-tooltip />
        <el-table-column prop="expression" label="条件表达式" min-width="220" show-overflow-tooltip />
        <el-table-column prop="updatedAt" label="更新时间" width="160" />
        <el-table-column label="操作" width="120" fixed="right">
          <template slot-scope="scope">
            <el-button type="text" size="mini" @click="loadRecord(scope.row)">加载</el-button>
            <el-button type="text" size="mini" class="btn-danger" @click="removeRecord(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!recordDialog.loading && recordDialog.list.length === 0" description="暂无调试记录" />
    </el-dialog>

    <el-dialog title="TreeNode 类型说明" :visible.sync="treeNodeHelpVisible" width="900px" append-to-body>
      <div class="md-content condition-tree-help" v-html="treeNodeHelpHtml"></div>
      <span slot="footer">
        <el-button size="small" @click="treeNodeHelpVisible = false">关闭</el-button>
      </span>
    </el-dialog>
  </section>
</template>

<script>
import { debugCondition, listCondDebugRecords, getCondDebugRecord, saveCondDebugRecord, deleteCondDebugRecord } from '@/api/conditionDebug'
import MonacoJsonEditor from '@/components/MonacoJsonEditor'

// TreeNode 类型说明（弹窗内转为 HTML 展示）
const TREE_NODE_HELP_MD = [
  '# TreeNode 类型说明',
  '',
  'Maximo 条件表达式经 `ParseExpression` 解析后得到 `psdi.common.parse.TreeNode` 语法树，条件求值就是对这棵树调用 `getBoolean(mbo)`。',
  '',
  '## 结果类型(TreeNodeDataType)',
  '',
  '| 常量 | 值 | 含义 |',
  '| --- | --- | --- |',
  '| UnknownNode | -1 | 未知 |',
  '| BooleanNode | 0 | 布尔 |',
  '| StringNode | 1 | 字符串 |',
  '| NumberNode | 2 | 数字 |',
  '| DateTimeNode | 3 | 日期时间 |',
  '',
  '调试页“解析类型”中显示的数字就是这里的值。',
  '',
  '## 逻辑与比较节点',
  '',
  '| 类 | 说明 | 表达式示例 |',
  '| --- | --- | --- |',
  '| AndNode | 与 | `a and b` |',
  '| OrNode | 或 | `a or b` |',
  '| NotNode | 非 | `not a` |',
  '| ComparisonNode | 比较运算 | `a = b`、`a != b`、`a > b`、`a <= b` |',
  "| LikeNode | 模糊匹配 | `wonum like 'PL%'` |",
  '| InNode | 集合匹配 | `wopriority in (1, 2, 3)` |',
  '| BetweenNode | 区间 | `istask between 0 and 1` |',
  '| IsNullNode | 空值判断 | `reportdate is null` |',
  '| Exists | 存在性 | `exists (关系或子查询)` |',
  '| SQLNode | SQL 片段/子查询节点 | `(select count(1) ...)` |',
  '| YesNode / NoNode | 真 / 假常量 | `true` / `false` |',
  '',
  '## 取值节点',
  '',
  '| 类 | 说明 | 示例 |',
  '| --- | --- | --- |',
  '| AttributeNode | 字段引用 | `status`、`:wonum` |',
  '| LiteralNode | 字面量 | `123`、`ABBR` |',
  '| DateNode | 日期字面量 | `2026-11-13` |',
  '| InternalValue | 内部值/同义词 | `:字段` 绑定值 |',
  '| StringReservedKeyNode | 保留字符串键值 | - |',
  '',
  '## 函数节点',
  '',
  '| 类 | 说明 | 示例 |',
  '| --- | --- | --- |',
  '| AvgNode / MaxNode / MinNode / SumNode / StdDevNode | 聚合函数 | `max(x)`、`sum(y)` |',
  '| ExpNode / PowNode / SqrtNode / RoundNode | 数学函数 | `round(x, 2)` |',
  '| LengthNode / LowerNode / UpperNode | 字符串函数 | `length(x)`、`upper(x)` |',
  '| NegativeNode / ProductNode | 负数、乘法 | `-x`、`a * b` |',
  '| PersonNode / UserNode | 人员、用户相关取值 | - |',
  '| CustomNode | 自定义扩展节点 | - |',
  '',
  '## 辅助类(不是 TreeNode)',
  '',
  '| 类 | 说明 |',
  '| --- | --- |',
  '| Matches / SingleMatch / AnyMatch / MatchEnd | 匹配断言的内部辅助类 |',
  '| ParseExpression / ParseExpressionTokenManager | JavaCC 词法/语法分析器 |',
  '| Token / ParseException | 词法单元、解析异常 |',
  '| ParserService / ParserServiceRemote | 对外解析服务 |',
  '',
  '## 语法树示例',
  '',
  '```',
  'COMPARISON operand = operand',
  '    ATTRIBUTE status',
  "    STRING LITERAL 'ITEMISSUED'",
  '```',
  '',
  '## 说明',
  '',
  '- 条件表达式最终必须是布尔节点(BooleanNode/0)。',
  '- 含子查询等 `ParseExpression` 不支持的语法时，Maximo 会回退到 `SqlFormat` 生成 SQL，再按 `MboSet.isEmpty()` 判定，执行日志中会显示“SqlFormat SQL 回退”。',
  '- 回退路径由数据库语义决定，例如本库空串等于 NULL，`field != \'\'` 不会成立。'
].join('\n')

export default {
  name: 'ConditionDebug',
  components: { MonacoJsonEditor },
  data() {
    return {
      loading: false,
      activeDetail: [],
      resp: null,
      treeNodeHelpVisible: false,
      treeNodeHelpHtml: '',
      currentRecordName: '',
      saveDialog: { visible: false, loading: false, recordName: '', description: '' },
      recordDialog: { visible: false, loading: false, keyword: '', list: [] },
      form: {
        mboName: 'DUMMY_TABLE',
        mboWhere: '1=1',
        condType: 'WORKFLOW',
        parserType: 'SYSTEM',
        expression: '',
        fields: '',
        maxRows: 10
      },
      rules: {
        mboName: [{ required: true, message: '请输入 MBO 对象名称', trigger: 'blur' }],
        mboWhere: [{ required: true, message: '请输入 MBO 记录过滤条件', trigger: 'blur' }],
        expression: [{ required: true, message: '请输入条件表达式内容', trigger: 'blur' }]
      }
    }
  },
  computed: {
    body() {
      return this.resp || {}
    },
    resultData() {
      return this.body.data || {}
    },
    evalLogLines() {
      return this.resultData.evalLog || this.resultData.sqlLog || []
    },
    recordRows() {
      const record = this.resultData.record
      if (!record || typeof record !== 'object') {
        return []
      }
      return Object.keys(record).map(field => ({ field, value: record[field] === null || record[field] === undefined ? '' : String(record[field]) }))
    },
    candidateColumns() {
      const list = this.resultData.candidates
      if (!Array.isArray(list) || list.length === 0) {
        return []
      }
      const cols = []
      const seen = {}
      list.forEach(row => {
        Object.keys(row || {}).forEach(key => {
          if (!seen[key]) {
            seen[key] = true
            cols.push(key)
          }
        })
      })
      return cols
    },
    rawText() {
      return this.resp ? JSON.stringify(this.resp, null, 2) : ''
    }
  },
  mounted() {
    this.treeNodeHelpHtml = this.renderMarkdown(TREE_NODE_HELP_MD)
  },
  methods: {
    renderMarkdown(md) {
      if (!md) return ''
      let html = md
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
      const codeBlocks = []
      html = html.replace(/```([\s\S]*?)```/g, (match, code) => {
        const idx = codeBlocks.length
        codeBlocks.push('<pre><code>' + code.trim() + '</code></pre>')
        return '\n@@CODEBLOCK' + idx + '@@\n'
      })
      const lines = html.split('\n')
      const result = []
      let inTable = false
      let tableRows = []
      const flushTable = () => {
        if (!inTable) return
        inTable = false
        if (tableRows.length === 0) return
        let table = '<table>'
        tableRows.forEach((row, i) => {
          const cells = row.split('|').slice(1, -1).map(c => c.trim())
          if (i === 0) {
            table += '<thead><tr>' + cells.map(c => '<th>' + c + '</th>').join('') + '</tr></thead><tbody>'
          } else if (!/^[\s\-:|]+$/.test(row)) {
            table += '<tr>' + cells.map(c => '<td>' + c + '</td>').join('') + '</tr>'
          }
        })
        table += '</tbody></table>'
        result.push(table)
        tableRows = []
      }
      lines.forEach(line => {
        const trimmed = line.trim()
        if (/^@@CODEBLOCK\d+@@$/.test(trimmed)) {
          flushTable()
          result.push(trimmed)
          return
        }
        if (/^\|.*\|$/.test(trimmed)) {
          inTable = true
          tableRows.push(trimmed)
          return
        }
        flushTable()
        if (/^###\s+/.test(trimmed)) {
          result.push('<h3>' + trimmed.replace(/^###\s+/, '') + '</h3>')
        } else if (/^##\s+/.test(trimmed)) {
          result.push('<h2>' + trimmed.replace(/^##\s+/, '') + '</h2>')
        } else if (/^#\s+/.test(trimmed)) {
          result.push('<h1>' + trimmed.replace(/^#\s+/, '') + '</h1>')
        } else if (/^-\s+/.test(trimmed)) {
          result.push('<p class="md-li">• ' + trimmed.replace(/^-\s+/, '') + '</p>')
        } else if (trimmed !== '') {
          result.push('<p>' + trimmed + '</p>')
        }
      })
      flushTable()
      let out = result.join('\n')
      out = out.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
      out = out.replace(/`([^`]+)`/g, '<code>$1</code>')
      out = out.replace(/@@CODEBLOCK(\d+)@@/g, (match, idx) => codeBlocks[Number(idx)])
      return out
    },
    condTypeLabel(type) {
      if (type === 'WORKFLOW') return '工作流 (WORKFLOW)'
      if (type === 'SIGOPTION') return '签名 (SIGOPTION)'
      return type || '-'
    },
    parserTypeLabel(type) {
      if (type === 'CUSTOM') return '自定义 (TreeNode)'
      if (type === 'SYSTEM') return '系统解析器'
      return type || '-'
    },
    handleDebug() {
      this.$refs.form.validate(valid => {
        if (!valid) {
          return
        }
        this.loading = true
        this.resp = null
        debugCondition({
          mboName: (this.form.mboName || '').trim(),
          mboWhere: (this.form.mboWhere || '').trim(),
          expression: (this.form.expression || '').trim(),
          condType: this.form.condType,
          parserType: this.form.parserType,
          fields: (this.form.fields || '').trim(),
          maxRows: this.form.maxRows
        }).then(res => {
          const body = res.data || res
          this.resp = body
          this.activeDetail = []
          if (body.status === 'error') {
            this.$message.error(body.message || '调试失败')
          } else {
            this.$message.success('执行完成，结果: ' + (body.data && body.data.result === true ? 'TRUE' : 'FALSE'))
          }
        }).catch(err => {
          this.$message.error('调试失败: ' + (err.message || String(err)))
        }).finally(() => {
          this.loading = false
        })
      })
    },
    resetForm() {
      this.resp = null
      this.activeDetail = []
      this.currentRecordName = ''
      this.$refs.form.resetFields()
      this.form.condType = 'WORKFLOW'
      this.form.parserType = 'SYSTEM'
      this.form.maxRows = 10
    },
    // === 调试记录（MySQL） ===
    openSaveDialog() {
      this.saveDialog.recordName = this.currentRecordName || ''
      this.saveDialog.description = ''
      this.saveDialog.visible = true
    },
    openSaveAsDialog() {
      this.saveDialog.recordName = this.currentRecordName ? this.currentRecordName + ' 副本' : ''
      this.saveDialog.description = ''
      this.saveDialog.visible = true
    },
    submitSave() {
      const name = (this.saveDialog.recordName || '').trim()
      if (!name) {
        this.$message.warning('请输入记录名称')
        return
      }
      if (!(this.form.mboName || '').trim() || !(this.form.expression || '').trim()) {
        this.$message.warning('请先填写 MBO 对象名称与条件表达式')
        return
      }
      this.saveDialog.loading = true
      saveCondDebugRecord({
        recordName: name,
        description: (this.saveDialog.description || '').trim(),
        mboName: (this.form.mboName || '').trim(),
        mboWhere: (this.form.mboWhere || '').trim(),
        condType: this.form.condType,
        parserType: this.form.parserType,
        expression: (this.form.expression || '').trim(),
        fields: (this.form.fields || '').trim(),
        maxRows: this.form.maxRows
      }).then(res => {
        if (!res || res.code !== 200) {
          this.$message.error((res && res.message) || '保存失败')
          return
        }
        this.currentRecordName = name
        this.saveDialog.visible = false
        this.$message.success('调试记录已保存')
        if (this.recordDialog.visible) {
          this.fetchRecords()
        }
      }).catch(err => {
        this.$message.error('保存失败: ' + (err.message || String(err)))
      }).finally(() => {
        this.saveDialog.loading = false
      })
    },
    openRecordsDialog() {
      this.recordDialog.visible = true
      this.fetchRecords()
    },
    fetchRecords() {
      this.recordDialog.loading = true
      listCondDebugRecords((this.recordDialog.keyword || '').trim()).then(res => {
        if (!res || res.code !== 200) {
          this.recordDialog.list = []
          this.$message.error((res && res.message) || '查询调试记录失败')
          return
        }
        this.recordDialog.list = res.data || []
      }).catch(err => {
        this.$message.error('查询调试记录失败: ' + (err.message || String(err)))
      }).finally(() => {
        this.recordDialog.loading = false
      })
    },
    loadRecord(row) {
      getCondDebugRecord(row.id).then(res => {
        if (!res || res.code !== 200) {
          this.$message.error((res && res.message) || '加载调试记录失败')
          return
        }
        const r = res.data || {}
        this.form.mboName = r.mboName || ''
        this.form.mboWhere = r.mboWhere || ''
        this.form.condType = r.condType || 'WORKFLOW'
        this.form.parserType = r.parserType || 'SYSTEM'
        this.form.expression = r.expression || ''
        this.form.fields = r.fields || ''
        this.form.maxRows = r.maxRows || 10
        this.currentRecordName = r.recordName || ''
        this.resp = null
        this.activeDetail = []
        this.recordDialog.visible = false
        this.$message.success('已加载调试记录: ' + (r.recordName || ''))
      }).catch(err => {
        this.$message.error('加载调试记录失败: ' + (err.message || String(err)))
      })
    },
    copyRaw() {
      const text = this.rawText || ''
      if (!text) {
        return
      }
      const done = () => this.$message.success('已复制到剪贴板')
      const fail = () => this.$message.error('复制失败，请手动选择复制')
      if (navigator.clipboard && window.isSecureContext) {
        navigator.clipboard.writeText(text).then(done).catch(fail)
        return
      }
      const textarea = document.createElement('textarea')
      textarea.value = text
      textarea.style.position = 'fixed'
      textarea.style.opacity = '0'
      document.body.appendChild(textarea)
      textarea.select()
      try {
        document.execCommand('copy')
        done()
      } catch (e) {
        fail()
      }
      document.body.removeChild(textarea)
    },
    removeRecord(row) {
      this.$confirm('确定删除调试记录「' + row.recordName + '」?', '提示', { type: 'warning' }).then(() => {
        return deleteCondDebugRecord(row.id).then(res => {
          if (!res || res.code !== 200) {
            this.$message.error((res && res.message) || '删除失败')
            return
          }
          if (this.currentRecordName === row.recordName) {
            this.currentRecordName = ''
          }
          this.$message.success('已删除')
          this.fetchRecords()
        })
      }).catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.query-page {
  padding: 16px;
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
.page-actions {
  display: flex;
  align-items: center;
}
.save-preview {
  padding: 10px 12px;
  background: #f5f7fa;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  font-size: 12px;
  color: #606266;
  line-height: 20px;
  white-space: pre-wrap;
  word-break: break-all;
}
.record-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}
.btn-danger {
  color: #f56c6c;
}
.result-card {
  margin-top: 16px;
}
.result-header {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  font-weight: 600;
}
.result-tag {
  margin-left: 10px;
}
.error-alert {
  margin-bottom: 12px;
}
.out-desc {
  margin-bottom: 8px;
}
.mono {
  font-family: Consolas, Monaco, monospace;
}
.field-tip {
  color: #909399;
  font-size: 12px;
  line-height: 18px;
  margin-top: 4px;
}
.record-block {
  margin-top: 16px;
}
.block-title {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  margin: 12px 0 8px 0;
}
.block-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.detail-collapse {
  margin-top: 16px;
}
.raw-json {
  margin: 8px 0 0 0;
  padding: 10px;
  background: #f5f7fa;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  font-family: Consolas, Monaco, monospace;
  font-size: 12px;
  max-height: 420px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-all;
}
.sql-text {
  color: #1f6feb;
}
.log-text {
  color: #303133;
  line-height: 18px;
}
</style>

<style lang="scss">
.condition-tree-help {
  font-size: 14px;
  line-height: 1.7;
  color: #303133;
  overflow: auto;
  max-height: 70vh;
}
.condition-tree-help h1 {
  font-size: 22px;
  border-bottom: 1px solid #e4e7ed;
  padding-bottom: 8px;
  margin: 16px 0 12px;
}
.condition-tree-help h2 {
  font-size: 18px;
  margin: 18px 0 10px;
  border-left: 4px solid #409eff;
  padding-left: 10px;
}
.condition-tree-help h3 {
  font-size: 16px;
  margin: 14px 0 8px;
}
.condition-tree-help table {
  border-collapse: collapse;
  width: 100%;
  margin: 10px 0;
  font-size: 13px;
}
.condition-tree-help th,
.condition-tree-help td {
  border: 1px solid #dcdfe6;
  padding: 6px 10px;
  text-align: left;
}
.condition-tree-help th {
  background-color: #f5f7fa;
  font-weight: 600;
  white-space: nowrap;
}
.condition-tree-help pre {
  background-color: #f5f7fa;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 10px 14px;
  overflow: auto;
  font-size: 13px;
  line-height: 1.5;
}
.condition-tree-help code {
  background-color: #f5f7fa;
  color: #c7254e;
  padding: 2px 5px;
  border-radius: 3px;
  font-size: 13px;
}
.condition-tree-help pre code {
  background-color: transparent;
  color: #303133;
  padding: 0;
}
.condition-tree-help p {
  margin: 8px 0;
}
.condition-tree-help .md-li {
  padding-left: 10px;
}
</style>
