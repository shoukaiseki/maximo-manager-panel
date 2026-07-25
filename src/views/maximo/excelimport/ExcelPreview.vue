<template>
  <section class="json-preview-page">
    <!-- 顶部工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-button size="small" icon="el-icon-edit" type="primary" @click="inputDialogVisible = true">输入 JSON</el-button>
        <span class="separator"></span>
        <el-radio-group v-model="viewerMode" size="small">
          <el-radio-button label="pretty">树形</el-radio-button>
          <el-radio-button label="viewer">源码</el-radio-button>
          <el-radio-button label="graph">可视化</el-radio-button>
          <el-radio-button label="table">表格</el-radio-button>
        </el-radio-group>
        <span class="result-type" v-if="resultData">{{ resultType }}</span>
      </div>
      <div class="toolbar-right">
        <el-button v-if="resultData" size="small" icon="el-icon-document-copy" @click="copyResultJson">复制 JSON</el-button>
        <el-button v-if="resultData" size="small" icon="el-icon-delete" @click="clearData">清空</el-button>
      </div>
    </div>

    <!-- 预览主体 - 100% 填充 -->
    <div class="preview-container" :class="{ 'is-empty': !resultData, 'is-graph': viewerMode === 'graph', 'is-table': viewerMode === 'table' }">
      <template v-if="!resultData">
        <el-empty description="点击「输入 JSON」粘贴数据" :image-size="100" />
      </template>
      <div v-else-if="viewerMode === 'graph'" class="graph-wrapper">
        <json-g6-graph ref="g6Graph" :data="resultData" @node-click="onGraphNodeClick" />
      </div>
      <div v-else-if="viewerMode === 'table'" class="table-wrapper">
        <json-table-grid
          ref="jsonTable"
          :data="resultData"
          :highlight-path.sync="tableHighlightPath"
          @cell-click="onTableCellClick"
        />
      </div>
      <div v-else class="preview-scroll">
        <vue-json-pretty
          v-if="viewerMode === 'pretty'"
          :data="resultData"
          :deep="3"
          :showLength="true"
          :showLineNumber="true"
          showIcon
          highlightMouseoverNode
        />
        <json-viewer
          v-if="viewerMode === 'viewer'"
          :value="resultData"
          :expand-depth="3"
          copyable
          boxed
          sort
        />
      </div>
    </div>

    <!-- JSON 输入对话框 -->
    <el-dialog
      title="输入 JSON 数据"
      :visible.sync="inputDialogVisible"
      width="700px"
      top="3vh"
      :close-on-click-modal="false"
    >
      <el-input
        ref="jsonInput"
        v-model="inputJson"
        type="textarea"
        :rows="20"
        placeholder="在此粘贴或输入 JSON 数据..."
        style="font-family: 'Courier New', monospace; font-size: 13px;"
        @input="debounceParse"
      />
      <div slot="footer" style="display: flex; justify-content: space-between;">
        <div>
          <el-button size="small" icon="el-icon-document-copy" @click="copyInputJson">复制</el-button>
          <el-button size="small" icon="el-icon-delete" @click="inputJson = ''">清空</el-button>
        </div>
        <div>
          <el-button size="small" type="primary" @click="inputDialogVisible = false">确定</el-button>
        </div>
      </div>
    </el-dialog>
  </section>
</template>

<script>
import JsonTableGrid from '@/components/JsonTableGrid.vue'

export default {
  name: 'JsonPreview',
  components: { JsonTableGrid },
  data() {
    return {
      inputJson: '',
      resultData: null,
      resultType: '',
      viewerMode: 'pretty',
      inputDialogVisible: false,
      parseTimer: null,
      tableHighlightPath: ''
    }
  },
  created() {
    // 从 sessionStorage 读取从配置页传来的数据
    try {
      const shared = sessionStorage.getItem('jsonPreviewData')
      if (shared) {
        this.inputJson = shared
        sessionStorage.removeItem('jsonPreviewData')
        this.$nextTick(() => this.parseInput())
      }
    } catch (e) { /* ignore */ }
  },
  methods: {
    /** 点击表格单元格 → 跳转到可视化并高亮 */
    onTableCellClick(path) {
      this.viewerMode = 'graph'
      this.$nextTick(() => {
        if (this.$refs.g6Graph) {
          this.$refs.g6Graph.focusNode(path)
        }
      })
    },
    /** 点击可视化节点 → 跳转到表格并高亮 */
    onGraphNodeClick(path) {
      this.viewerMode = 'table'
      this.tableHighlightPath = path
      this.$nextTick(() => {
        if (this.$refs.jsonTable) {
          this.$refs.jsonTable.scrollToCell(path)
        }
      })
    },
    debounceParse() {
      if (this.parseTimer) clearTimeout(this.parseTimer)
      this.parseTimer = setTimeout(() => this.parseInput(), 400)
    },
    parseInput() {
      const text = this.inputJson.trim()
      if (!text) {
        this.resultData = null
        this.resultType = ''
        return
      }
      try {
        const parsed = JSON.parse(text)
        this.resultData = parsed
        const t = typeof parsed
        this.resultType = Array.isArray(parsed)
          ? `Array[${parsed.length}]`
          : parsed === null
            ? 'null'
            : t === 'object'
              ? `Object{${Object.keys(parsed).length}}`
              : t
      } catch (e) {
        this.resultData = null
        this.resultType = 'JSON 格式错误'
      }
    },
    clearData() {
      this.inputJson = ''
      this.resultData = null
      this.resultType = ''
      this.tableHighlightPath = ''
    },
    copyInputJson() {
      if (!this.inputJson) { this.$message.warning('没有可复制的内容'); return }
      navigator.clipboard.writeText(this.inputJson).then(() => {
        this.$message.success('已复制')
      }).catch(() => {
        const ta = document.createElement('textarea')
        ta.value = this.inputJson
        document.body.appendChild(ta)
        ta.select()
        document.execCommand('copy')
        document.body.removeChild(ta)
        this.$message.success('已复制')
      })
    },
    copyResultJson() {
      try {
        const str = typeof this.resultData === 'object'
          ? JSON.stringify(this.resultData, null, 2)
          : String(this.resultData)
        navigator.clipboard.writeText(str).then(() => {
          this.$message.success('JSON 已复制到剪贴板')
        }).catch(() => {
          const ta = document.createElement('textarea')
          ta.value = str
          document.body.appendChild(ta)
          ta.select()
          document.execCommand('copy')
          document.body.removeChild(ta)
          this.$message.success('JSON 已复制到剪贴板')
        })
      } catch (e) {
        this.$message.error('复制失败: ' + e.message)
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.json-preview-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 100px);
  padding: 0;
  overflow: hidden;

  .toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 10px 16px;
    background: #fff;
    border-bottom: 1px solid #e4e7ed;
    flex-shrink: 0;

    .toolbar-left {
      display: flex;
      align-items: center;
      gap: 8px;
    }
    .toolbar-right {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .separator {
      width: 1px;
      height: 20px;
      background: #dcdfe6;
      margin: 0 4px;
    }

    .result-type {
      font-size: 12px;
      color: #909399;
      background: #f0f2f5;
      padding: 2px 8px;
      border-radius: 4px;
      margin-left: 4px;
    }
  }

  .preview-container {
    flex: 1;
    overflow: hidden;
    padding: 0;

    &.is-empty {
      display: flex;
      align-items: center;
      justify-content: center;
    }

    &.is-graph {
      display: flex;
      flex-direction: column;
      padding: 0;
    }

    .preview-scroll {
      height: 100%;
      overflow: auto;
      padding: 12px 16px;
      background: #fafafa;
    }

    .graph-wrapper {
      flex: 1;
      display: flex;
      min-height: 0;
    }

    .table-wrapper {
      height: 100%;
      overflow: hidden;
      padding: 0;
      background: #fff;
    }
  }
}
</style>
