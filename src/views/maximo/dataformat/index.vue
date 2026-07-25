<template>
  <section class="query-page">
    <el-card>
      <div class="page-header-row">
        <div>
          <h2>数据处理</h2>
          <p class="page-summary">多行数据处理与格式化</p>
        </div>
      </div>

      <div style="display:flex; gap:16px;">
        <div style="flex:1;">
          <div class="section-title">输入</div>
          <div style="margin-bottom:8px; display:flex; gap:8px; flex-wrap:wrap;">
            <el-button type="primary" size="mini" @click="handleExactMatch">maximo精确查找多个值</el-button>
            <el-button type="success" size="mini" @click="handleSqlInString">SQL IN 字符串</el-button>
            <el-button type="warning" size="mini" @click="handleSqlInInt">SQL IN 整数</el-button>
          </div>
          <el-input
            v-model="inputText"
            type="textarea"
            :rows="20"
            placeholder="请输入数据，支持逗号和换行分隔..."
            resize="vertical"
            style="width:100%;"
          />
        </div>
        <div style="flex:1;">
          <div class="section-title">
            输出
            <el-button type="primary" size="mini" icon="el-icon-document-copy" style="margin-left:8px;" @click="copyOutput">复制输出</el-button>
          </div>
          <div ref="monacoContainer" class="monaco-container"></div>
        </div>
      </div>
    </el-card>
  </section>
</template>

<script>
export default {
  name: 'DataFormat',
  data() {
    return {
      inputText: '',
      monacoEditor: null,
      monacoLoaded: false
    }
  },
  watch: {
    inputText() {
      // 不自动处理，按按钮触发
    }
  },
  methods: {
    handleExactMatch() {
      if (!this.inputText.trim()) {
        this.$message.warning('请输入数据')
        return
      }
      // 按逗号和换行分割，去空格，过滤空值
      const items = this.inputText
        .split(/[,，\n]/)
        .map(s => s.trim())
        .filter(s => s.length > 0)
      if (items.length === 0) {
        this.$message.warning('未解析到有效数据')
        return
      }
      const result = items.map(s => '=' + s).join(',')
      this.setMonacoValue(result)
      this.$message.success('处理完成，共 ' + items.length + ' 个值')
    },
    handleSqlInString() {
      this.formatSqlIn('\'')
    },
    handleSqlInInt() {
      this.formatSqlIn('')
    },
    formatSqlIn(quote) {
      if (!this.inputText.trim()) {
        this.$message.warning('请输入数据')
        return
      }
      const items = this.inputText
        .split(/[,，\n]/)
        .map(s => s.trim())
        .filter(s => s.length > 0)
      if (items.length === 0) {
        this.$message.warning('未解析到有效数据')
        return
      }
      const result = 'in (' + items.map(s => quote + s + quote).join(',') + ')'
      this.setMonacoValue(result)
      this.$message.success('处理完成，共 ' + items.length + ' 个值')
    },
    setMonacoValue(val) {
      if (this.monacoEditor) {
        this.monacoEditor.setValue(val)
      } else {
        this.initMonaco(val)
      }
    },
    copyOutput() {
      const text = this.monacoEditor ? this.monacoEditor.getValue() : ''
      if (!text) {
        this.$message.warning('输出内容为空')
        return
      }
      if (navigator.clipboard && window.isSecureContext) {
        navigator.clipboard.writeText(text)
          .then(() => this.$message.success('输出已复制到剪贴板'))
          .catch(() => this.fallbackCopy(text))
      } else {
        this.fallbackCopy(text)
      }
    },
    fallbackCopy(text) {
      const textarea = document.createElement('textarea')
      textarea.value = text
      textarea.style.position = 'fixed'
      textarea.style.top = '-1000px'
      textarea.style.left = '-1000px'
      document.body.appendChild(textarea)
      textarea.select()
      try {
        document.execCommand('copy')
        this.$message.success('输出已复制到剪贴板')
      } catch (e) {
        this.$message.error('复制失败: ' + e.message)
      }
      document.body.removeChild(textarea)
    },
    initMonaco(value) {
      if (!this.monacoLoaded) {
        import(/* webpackChunkName: "monaco" */ 'monaco-editor').then(monaco => {
          this.monacoLoaded = true
          this._monaco = monaco
          this.createEditor(monaco, value)
        }).catch(err => {
          console.error('Monaco Editor 加载失败:', err)
        })
      } else {
        this.createEditor(this._monaco, value)
      }
    },
    createEditor(monaco, value) {
      if (!this.$refs.monacoContainer) return
      if (this.monacoEditor) {
        this.monacoEditor.setValue(value || '')
        return
      }
      this.monacoEditor = monaco.editor.create(this.$refs.monacoContainer, {
        value: value || '',
        language: 'plaintext',
        readOnly: false,
        theme: 'vs',
        automaticLayout: true,
        minimap: { enabled: false },
        scrollBeyondLastLine: false,
        fontSize: 13,
        wordWrap: 'on',
        folding: true,
        lineNumbers: 'on',
        renderLineHighlight: 'none'
      })
    }
  },
  mounted() {
    this.$nextTick(() => {
      this.initMonaco('')
    })
  },
  beforeDestroy() {
    if (this.monacoEditor) {
      this.monacoEditor.dispose()
      this.monacoEditor = null
    }
  }
}
</script>

<style lang="scss">
.query-page {
  padding: 16px;
}
.page-header-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}
.page-summary {
  color: #606266;
  margin: 0;
  font-size: 13px;
}
.section-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 8px;
  color: #303133;
}
.monaco-container {
  height: 532px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}
</style>
