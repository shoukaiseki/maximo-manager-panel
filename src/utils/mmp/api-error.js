import Vue from 'vue'

// 注入一次样式
let styleInjected = false
function injectStyle() {
  if (styleInjected) return
  styleInjected = true
  const style = document.createElement('style')
  style.textContent = `
.api-error-message {
  color: #f56c6c;
  font-weight: 600;
  margin-bottom: 12px;
  word-break: break-word;
  line-height: 1.6;
}
.api-error-pre {
  margin: 0;
  padding: 12px;
  background: #f5f7fa;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  max-height: 300px;
  overflow: auto;
  font-family: 'Courier New', monospace;
  font-size: 12px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-all;
}
`
  document.head.appendChild(style)
}

const ScriptErrorDialog = {
  name: 'ScriptErrorDialog',
  data() {
    return {
      visible: false,
      payload: null,
      activeNames: []
    }
  },
  computed: {
    title() {
      return '接口返回错误'
    },
    message() {
      if (!this.payload) return '未知错误'
      return this.payload.message ? String(this.payload.message) : '未知错误'
    },
    hasTrace() {
      return this.payload && this.payload.errorMessageTrace !== undefined &&
        this.payload.errorMessageTrace !== null && this.payload.errorMessageTrace !== ''
    },
    traceText() {
      return this.hasTrace ? String(this.payload.errorMessageTrace) : ''
    },
    hasDebug() {
      return this.payload && this.payload.debugMsg !== undefined &&
        this.payload.debugMsg !== null && this.payload.debugMsg !== ''
    },
    debugText() {
      return this.hasDebug ? String(this.payload.debugMsg) : ''
    }
  },
  methods: {
    copyRaw() {
      this.copyText(JSON.stringify(this.payload, null, 2))
    },
    copyField(text) {
      this.copyText(String(text))
    },
    copyText(text) {
      if (navigator.clipboard && window.isSecureContext) {
        navigator.clipboard.writeText(text).then(() => {
          this.$message.success('已复制')
        }).catch(() => {
          this.copyByTextarea(text)
        })
      } else {
        this.copyByTextarea(text)
      }
    },
    copyByTextarea(text) {
      const textarea = document.createElement('textarea')
      textarea.value = text
      textarea.setAttribute('readonly', 'readonly')
      textarea.style.position = 'fixed'
      textarea.style.top = '-9999px'
      document.body.appendChild(textarea)
      textarea.select()
      let copied = false
      try {
        copied = document.execCommand('copy')
      } catch (e) {
        copied = false
      }
      document.body.removeChild(textarea)
      if (copied) {
        this.$message.success('已复制')
      } else {
        this.$message.error('复制失败')
      }
    },
    onClosed() {
      if (activeInstance === this) activeInstance = null
      this.$destroy()
      if (this.$el && this.$el.parentNode) {
        this.$el.parentNode.removeChild(this.$el)
      }
    }
  },
  render(h) {
    const children = [
      h('div', { class: 'api-error-message' }, this.message)
    ]
    if (this.hasTrace || this.hasDebug) {
      const collapseItems = []
      const buildItem = (label, name, text) => h('el-collapse-item', { props: { name }}, [
        h('span', { slot: 'title' }, [
          h('span', label),
          h('el-button', {
            style: { marginLeft: '10px' },
            props: { type: 'text', size: 'mini', icon: 'el-icon-copy-document' },
            attrs: { title: '复制 ' + label },
            on: {
              click: (e) => {
                e.stopPropagation() // 避免触发折叠面板展开/收起
                this.copyField(text)
              }
            }
          })
        ]),
        h('pre', { class: 'api-error-pre' }, text)
      ])
      if (this.hasTrace) {
        collapseItems.push(buildItem('errorMessageTrace', 'trace', this.traceText))
      }
      if (this.hasDebug) {
        collapseItems.push(buildItem('debugMsg', 'debug', this.debugText))
      }
      children.push(
        h('el-collapse', {
          props: { value: this.activeNames },
          on: { input: (v) => { this.activeNames = v } }
        }, collapseItems)
      )
    }
    children.push(
      h('span', { slot: 'footer' }, [
        h('el-button', { props: { type: 'primary' }, on: { click: this.copyRaw }}, '复制返回JSON'),
        h('el-button', { on: { click: () => { this.visible = false } }}, '关闭')
      ])
    )
    // 注意: 项目全局用 MaximizableDialog 覆盖了 el-dialog(未声明 props),
    // 必须经 attrs 传参, 用 props 会被 Vue 静默丢弃导致对话框不显示
    return h('el-dialog', {
      attrs: {
        title: this.title,
        visible: this.visible,
        width: '900px',
        'append-to-body': true,
        'close-on-click-modal': false
      },
      on: {
        'update:visible': (v) => { this.visible = v },
        closed: this.onClosed
      }
    }, children)
  }
}

// 单例：同一时间只显示一个错误对话框，重复调用只刷新内容
let activeInstance = null

function fillInstance(instance, payload) {
  instance.payload = payload
  instance.activeNames = [] // 折叠面板默认不展开
  instance.visible = true
}

/**
 * 显示脚本错误对话框
 * @param {Object} payload - 接口返回的错误对象 { status:'error', message, errorMessageTrace, debugMsg }
 */
export function showApiErrorDialog(payload) {
  if (!payload) return
  injectStyle()
  if (activeInstance) {
    fillInstance(activeInstance, payload)
    return
  }
  const Ctor = Vue.extend(ScriptErrorDialog)
  const instance = new Ctor().$mount()
  document.body.appendChild(instance.$el)
  activeInstance = instance
  fillInstance(instance, payload)
}

/**
 * 处理接口错误：如果有 trace/debug 则显示对话框，否则显示 toast
 * @param {Object} payload - 接口返回对象
 * @param {string} fallbackToast - 无 trace 时的 toast 消息
 * @returns {boolean} true=已显示对话框, false=未显示对话框
 */
export function handleApiError(payload, fallbackToast) {
  if (payload && (payload.errorMessageTrace || payload.debugMsg)) {
    showApiErrorDialog(payload)
    return true
  }
  if (fallbackToast) {
    Vue.prototype.$message({
      message: fallbackToast,
      type: 'error',
      duration: 5000
    })
  }
  return false
}

/**
 * 创建一个带 isApiError 标记的错误对象
 * @param {string} message
 * @returns {Error}
 */
export function createApiError(message) {
  const err = new Error(message)
  err.isApiError = true
  return err
}
