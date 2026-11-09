<template>
  <div v-loading="loading" class="monaco-json-editor" element-loading-text="编辑器加载中..." :style="{ height: height }" />
</template>

<script>
export default {
  name: 'MonacoJsonEditor',
  props: {
    value: {
      type: String,
      default: ''
    },
    readOnly: {
      type: Boolean,
      default: false
    },
    height: {
      type: String,
      default: '320px'
    },
    placeholder: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      loading: true,
      editor: null,
      monacoApi: null,
      settingModel: false
    }
  },
  watch: {
    value(val) {
      if (!this.editor || this.settingModel) {
        return
      }
      const current = this.editor.getValue()
      if (current !== (val || '')) {
        this.settingModel = true
        this.editor.setValue(val || '')
        this.settingModel = false
      }
    },
    readOnly(val) {
      if (this.editor) {
        this.editor.updateOptions({ readOnly: val })
      }
    }
  },
  mounted() {
    const element = this.$el
    import(/* webpackChunkName: "monaco" */ 'monaco-editor').then(monaco => {
      this.monacoApi = monaco
      this.editor = monaco.editor.create(element, {
        value: this.value || '',
        language: 'json',
        readOnly: this.readOnly,
        automaticLayout: true,
        minimap: { enabled: false },
        scrollBeyondLastLine: false,
        fontSize: 12,
        tabSize: 2,
        wordWrap: 'off',
        placeholder: this.placeholder || undefined
      })
      this.editor.onDidChangeModelContent(() => {
        if (this.settingModel) {
          return
        }
        this.$emit('input', this.editor.getValue())
        this.$emit('change', this.editor.getValue())
      })
      this.loading = false
    }).catch(err => {
      this.loading = false
      console.error('Monaco Editor 加载失败:', err)
    })
  },
  beforeDestroy() {
    if (this.editor) {
      this.editor.dispose()
      this.editor = null
    }
  }
}
</script>

<style scoped>
.monaco-json-editor {
  width: 100%;
  border: 1px solid #dcdfe6;
}
</style>
