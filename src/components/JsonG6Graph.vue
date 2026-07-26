<template>
  <div ref="g6Container" class="json-g6-graph" @click="onContainerClick"></div>
</template>

<script>
import G6 from '@antv/g6'

// 将 JSON 数据转换为 G6 树结构，使用路径 ID
function toTreeData(data, path) {
  const node = { id: path || 'root', keyName: '', entries: {}, children: [] }

  // 从路径中提取最后一个 key 作为 keyName
  if (path && path !== 'root') {
    const parts = path.split(/[\.\[\]]+/).filter(Boolean)
    const last = parts[parts.length - 1]
    // 判断是否为数字索引（即数组索引）
    node.keyName = /^\d+$/.test(last) ? last : last
  }

  if (data === null || data === undefined) {
    node.entries = { '': data === null ? 'null' : 'undefined' }
    return node
  }
  if (typeof data !== 'object') {
    node.entries = { '': data }
    return node
  }

  // 处理数组
  if (Array.isArray(data)) {
    if (data.length === 0) {
      node.entries = { '': '[]' }
      return node
    }
    data.forEach((item, idx) => {
      const childPath = `${path}[${idx}]`
      if (item !== null && typeof item === 'object') {
        node.children.push(toTreeData(item, childPath))
      } else {
        node.entries[String(idx)] = item
      }
    })
    return node
  }

  // 处理对象
  for (const k in data) {
    const v = data[k]
    const childPath = path ? `${path}.${k}` : k
    if (v !== null && typeof v === 'object') {
      node.children.push(toTreeData(v, childPath))
    } else {
      node.entries[k] = v
    }
  }
  return node
}

// 格式化键值对为显示文本
function formatEntries(entries) {
  return Object.entries(entries).map(([k, v]) => {
    if (typeof v === 'string') return `${k}: "${v}"`
    if (v === null) return `${k}: null`
    if (v === undefined) return `${k}: undefined`
    return `${k}: ${v}`
  })
}

// 计算节点尺寸
function computeSize(entries, keyName) {
  const lineHeight = 18
  const maxWidth = 380
  let width = 40

  const arr = formatEntries(entries)

  // 构建显示文本：keyName + entries
  const textLines = []
  if (keyName) {
    const prefix = /^\d+$/.test(keyName) ? `[${keyName}]` : `${keyName}:`
    textLines.push(prefix)
  }
  arr.forEach(s => textLines.push(`  ${s}`))

  if (textLines.length === 0) return [60, lineHeight, '']

  const height = textLines.length * lineHeight + 8
  let maxW = 0
  textLines.forEach(s => {
    const w = s.length * 7.5
    if (w > maxW) maxW = w
  })
  width = Math.min(maxW + 24, maxWidth)
  return [width, height, textLines.join('\n')]
}

export default {
  name: 'JsonG6Graph',
  props: {
    data: { type: [Object, Array, String, Number, Boolean], default: null }
  },
  data() {
    return {
      graph: null,
      resizeObserver: null,
      initZoomed: false
    }
  },
  watch: {
    data: {
      deep: true,
      handler() {
        this.initZoomed = false
        this.$nextTick(() => {
          this.drawGraph()
        })
      }
    }
  },
  mounted() {
    this.$nextTick(() => {
      this.initGraph()
      this.observeResize()
    })
  },
  beforeDestroy() {
    if (this.resizeObserver) {
      this.resizeObserver.disconnect()
    }
    if (this.graph) {
      this.graph.destroy()
      this.graph = null
    }
  },
  methods: {
    observeResize() {
      const container = this.$refs.g6Container
      if (!container || typeof ResizeObserver === 'undefined') return
      this.resizeObserver = new ResizeObserver(() => {
        if (this.graph && container.clientWidth > 0 && container.clientHeight > 0) {
          this.graph.changeSize(container.clientWidth, container.clientHeight)
          this.graph.fitView(20)
        }
      })
      // 观测时立即触发一次，确保首次布局正确
      this.resizeObserver.observe(container)
    },
    getContainerSize() {
      const container = this.$refs.g6Container
      if (!container) return { width: 600, height: 500 }
      return {
        width: container.clientWidth || 600,
        height: container.clientHeight || 500
      }
    },
    initGraph() {
      const container = this.$refs.g6Container
      if (!container) return

      const theme = {
        color: '#8252F5',
        hcolor: '#7c49f3',
        hbcolor: '#fff',
        nodeLabelColor: '#333333',
        nodeHoverColor: '#F4BE50'
      }

      this.registerNodes(theme)

      const { width, height } = this.getContainerSize()

      this.graph = new G6.TreeGraph({
        container,
        width,
        height,
        fitView: true,
        fitViewPadding: [20, 20, 20, 40],
        animate: true,
        defaultNode: { type: 'flow-rect' },
        defaultEdge: {
          type: 'cubic-horizontal',
          style: { stroke: '#ccc', lineWidth: 1.5 }
        },
        modes: {
          default: [
            { type: 'zoom-canvas', enableOptimize: true },
            { type: 'drag-canvas' }
          ]
        },
        layout: {
          type: 'compactBox',
          direction: 'LR',
          getHGap: () => 60,
          getVGap: () => 30,
          getWidth: (d) => {
            const [w] = computeSize(d.entries || {}, d.keyName || '')
            return w + 16
          },
          getHeight: (d) => {
            const [, h] = computeSize(d.entries || {}, d.keyName || '')
            return h + 16
          }
        }
      })

      this.registerBehaviors()

      this.drawGraph()

      // 布局完成后自动适配视图
      const onLayout = () => {
        if (this.graph && !this.graph.destroyed) {
          this.graph.fitView(20)
        }
      }
      if (!this.initZoomed) {
        this.initZoomed = true
        this.graph.on('afterlayout', onLayout)
      }
    },

    registerNodes(theme) {
      const { color, hcolor, hbcolor, nodeLabelColor, nodeHoverColor } = theme

      // 根节点：圆形带 JSON 图标
      G6.registerNode('root-icon', {
        draw(cfg, group) {
          if (!group) return
          // 背景圆（用于布局占位）
          const rootCircle = group.addShape('circle', {
            attrs: { x: 0, y: 0, r: 30 },
            name: 'root-bg'
          })
          // JSON 图标
          group.addShape('text', {
            attrs: {
              x: 0, y: 22,
              fontFamily: 'Arial',
              textAlign: 'center',
              text: '📄',
              fontSize: 40,
              fill: color,
              cursor: 'pointer'
            },
            name: 'root-icon-shape'
          })
          // 根标签
          group.addShape('text', {
            attrs: {
              x: 0, y: -4,
              textAlign: 'center',
              textBaseline: 'middle',
              text: cfg?.id || '',
              fontSize: 13,
              fill: '#fff',
              fontWeight: 'bold'
            },
            name: 'root-label'
          })
          return rootCircle
        },
        setState(name, value, item) {
          const group = item?.getContainer()
          const iconShape = group?.get('children')[1]
          if (!iconShape) return
          if (name === 'hover') {
            iconShape.attr('fill', value ? hcolor : color)
          }
        }
      }, 'circle')

      // 矩形节点：显示键值对
      G6.registerNode('flow-rect', {
        draw(cfg, group) {
          if (!group) return
          const collapsed = cfg.collapsed !== false
          const [width, height, entriesStr] = computeSize(cfg.entries || {}, cfg.keyName || '')

          const rectCfg = {
            width: width + 16,
            height: height + 16,
            lineWidth: 1,
            fill: `${color}18`,
            radius: 6,
            stroke: color,
            opacity: 1
          }

          const ox = -rectCfg.width / 2
          const oy = -rectCfg.height / 2

          // 矩形背景（作为 key shape，用于 bbox 计算）
          const mainRect = group.addShape('rect', {
            attrs: { x: ox, y: oy, ...rectCfg }
          })

          // 文本内容
          group.addShape('text', {
            attrs: {
              textAlign: 'left',
              textBaseline: 'bottom',
              x: ox + 8,
              y: -oy - 10,
              text: entriesStr,
              fontSize: 12,
              lineHeight: 18,
              fill: nodeLabelColor,
              cursor: 'pointer',
              fontFamily: 'Consolas, "Courier New", monospace'
            }
          })

          // 折叠按钮
          const { id, children = [] } = cfg
          if (children && children.length) {
            // 折叠按钮背景
            group.addShape('rect', {
              attrs: {
                x: rectCfg.width / 2 - 7,
                y: -7,
                width: 14,
                height: 14,
                stroke: `${hcolor}80`,
                cursor: 'pointer',
                fill: hbcolor,
                radius: 3
              },
              name: 'collapse-back',
              modelId: id
            })
            // + / - 号
            group.addShape('text', {
              attrs: {
                x: rectCfg.width / 2,
                y: -1,
                textAlign: 'center',
                textBaseline: 'middle',
                text: collapsed ? '+' : '-',
                fontSize: 16,
                cursor: 'pointer',
                fill: `${hcolor}80`
              },
              name: 'collapse-text',
              modelId: id
            })
          }

          return mainRect
        },
        setState(name, value, item) {
          if (name === 'collapse') {
            const group = item.getContainer()
            const t = group.find(e => e.get('name') === 'collapse-text')
            if (t) t.attr({ text: value ? '+' : '-' })
          } else if (name === 'hover') {
            const group = item.getContainer()
            const shape = group.get('children')[0]
            if (shape) {
              shape.attr('stroke', value ? nodeHoverColor : color)
              shape.attr('fill', value ? `${nodeHoverColor}20` : `${color}18`)
            }
          } else if (name === 'selected') {
            const group = item.getContainer()
            const shape = group.get('children')[0]
            if (shape) {
              shape.attr('stroke', value ? '#E6A23C' : color)
              shape.attr('lineWidth', value ? 3 : 1)
              shape.attr('fill', value ? '#FFF7E6' : `${color}18`)
            }
          }
        },
        getAnchorPoints() {
          return [[0, 0.5], [1, 0.5]]
        }
      }, 'rect')
    },

    registerBehaviors() {
      const graph = this.graph
      if (!graph) return

      const handleCollapse = (e) => {
        e.stopPropagation()
        const target = e.target
        const id = target.get('modelId')
        const item = graph.findById(id)
        if (!item) return
        const model = item.getModel()
        model.collapsed = !model.collapsed
        graph.layout()
        graph.setItemState(item, 'collapse', model.collapsed)
      }

      graph.on('collapse-text:click', handleCollapse)
      graph.on('collapse-back:click', handleCollapse)

      graph.on('node:mouseenter', (e) => {
        graph.setItemState(e.item, 'hover', true)
      })
      graph.on('node:mouseleave', (e) => {
        graph.setItemState(e.item, 'hover', false)
      })

      // 节点点击 → 触发联动
      graph.on('node:click', (e) => {
        const model = e.item.getModel()
        if (model.id) {
          this.$emit('node-click', model.id)
        }
      })
    },

    drawGraph() {
      if (!this.graph || !this.data) return

      const isEmpty = typeof this.data === 'object' &&
        !Array.isArray(this.data) &&
        this.data !== null &&
        Object.keys(this.data).length === 0

      const treeData = {
        id: isEmpty ? '{ }' : '_root',
        type: 'root-icon',
        children: []
      }

      if (!isEmpty) {
        const child = toTreeData(this.data, 'root')
        if (child.children.length || Object.keys(child.entries).length) {
          treeData.children.push(child)
        }
      }

      this.graph.changeData(treeData)
    },

    /** 聚焦并高亮指定路径的节点 */
    focusNode(path) {
      if (!this.graph || !path) return
      // 先清除所有 selected 状态
      this.graph.getNodes().forEach((node) => {
        this.graph.setItemState(node, 'selected', false)
      })
      // 查找路径对应的节点
      const item = this.graph.findById(path)
      if (item) {
        this.graph.setItemState(item, 'selected', true)
        this.graph.focusItem(item, true)
      }
    },

    /** 点击空白区清除选中 */
    onContainerClick(e) {
      // 只有点击 canvas 空白区域时才清除
      if (e.target === this.$refs.g6Container) {
        this.graph?.getNodes().forEach((node) => {
          this.graph.setItemState(node, 'selected', false)
        })
      }
    }
  }
}
</script>

<style scoped>
.json-g6-graph {
  flex: 1;
  width: 100%;
  min-height: 300px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  overflow: hidden;
  background: #fafafa;
}
</style>
