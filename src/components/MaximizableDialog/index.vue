<!-- 全局 el-dialog 包装组件: 标题栏默认带"最大化/还原"按钮, 全局注册覆盖 ElDialog 后所有 el-dialog 自动生效
     注意: 使用 render 函数实现插槽透传(Vue 2.6 中 v-slot 动态插槽名不能与 v-for 同用, template 写法会编译报错) -->
<script>
import { Dialog as ElDialogRaw } from 'element-ui'

export default {
  name: 'ElDialog',
  components: { ElDialogRaw },
  data() {
    return {
      isMax: false
    }
  },
  computed: {
    // 页面显式传了 fullscreen 的对话框视为原生全屏, 不再叠加我们的最大化按钮
    isNativeFullscreen() {
      const fs = this.$attrs.fullscreen
      return fs === true || fs === '' || fs === 'true'
    },
    showMaxBtn() {
      return !this.isNativeFullscreen && Boolean(this.$attrs.title || this.$slots.title)
    },
    realFullscreen() {
      return this.isNativeFullscreen || this.isMax
    }
  },
  methods: {
    toggleMax() {
      this.isMax = !this.isMax
    }
  },
  render(h) {
    // 插槽透传: 直接引用原始插槽函数(官方推荐), 避免包装丢失
    // 注意: 旧语法 slot="xxx" 的插槽在 Vue2.6 只进入 $slots, 需从 $slots 兜底补齐
    const scopedSlots = Object.assign({}, this.$scopedSlots)
    Object.keys(this.$slots).forEach(name => {
      if (name !== 'title' && !scopedSlots[name]) {
        scopedSlots[name] = () => this.$slots[name]
      }
    })
    // 关键: 标记 proxy=true, 否则 normalizeScopedSlot 不会把插槽填充到子组件 $slots,
    // 导致 el-dialog-raw 内部 v-if="$slots.footer" 判断失效, footer 不渲染
    Object.keys(scopedSlots).forEach(name => {
      const fn = scopedSlots[name]
      if (typeof fn === 'function') fn.proxy = true
    })
    // title 插槽: 原标题 + 最大化按钮
    if (this.showMaxBtn || this.$attrs.title || this.$slots.title) {
      scopedSlots.title = () => {
        const original = this.$slots.title && this.$slots.title.length
          ? this.$slots.title
          : [h('span', { class: 'el-dialog__title' }, this.$attrs.title || '')]
        if (!this.showMaxBtn) return original
        const btn = h('el-button', {
          class: 'el-dialog-max-btn',
          props: { size: 'mini', circle: true, icon: this.isMax ? 'el-icon-copy-document' : 'el-icon-full-screen', type: 'text' },
          attrs: { title: this.isMax ? '还原' : '最大化' },
          on: { click: e => { e.stopPropagation(); this.toggleMax() } }
        })
        return original.concat([btn])
      }
    }
    // fullscreen 已被显式放入 props, 从 attrs 移除避免覆盖(extractPropsFromVNodeData 会优先取 props)
    const attrs = { ...this.$attrs }
    delete attrs.fullscreen
    return h(ElDialogRaw, {
      ref: 'dialog',
      attrs,
      on: this.$listeners,
      props: { fullscreen: this.realFullscreen },
      scopedSlots
    })
  }
}
</script>

<style>
/* 全局样式: 弹窗可能 append-to-body 挂到 body 下, scoped 样式会失效 */
.el-dialog-max-btn {
  position: absolute;
  top: 14px;
  right: 44px;
  z-index: 10;
}
</style>