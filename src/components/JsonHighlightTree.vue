<template>
  <div ref="treeContainer" class="json-htree">
    <div
      v-for="(item, idx) in flatNodes"
      :key="idx"
      :ref="item.path === highlightPath ? 'hlNode' : undefined"
      class="htree-row"
      :class="{
        'htree-row-highlight': item.path === highlightPath,
        'htree-row-parent': item.nodeType === 'object' || item.nodeType === 'array'
      }"
      :style="{ paddingLeft: (item.depth * 16 + 4) + 'px' }"
      @click="onRowClick(item)"
    >
      <!-- toggle icon -->
      <span v-if="item.hasChildren" class="htree-toggle" @click.stop="toggleNode(item.path)">
        <i :class="item.expanded ? 'el-icon-caret-bottom' : 'el-icon-caret-right'" />
      </span>
      <span v-else class="htree-toggle htree-toggle-placeholder" />

      <!-- type badge -->
      <span class="htree-badge" :class="'htree-badge-' + item.nodeType">{{ item.nodeType === 'array' ? '[]' : item.nodeType === 'object' ? '{}' : item.nodeType === 'null' ? '∅' : '' }}</span>

      <!-- key -->
      <span v-if="item.key !== null" class="htree-key">{{ item.key }}<span class="htree-colon">: </span></span>

      <!-- value preview for primitive / empty -->
      <span v-if="item.nodeType === 'string'" class="htree-val htree-val-string">"{{ item.preview }}"</span>
      <span v-else-if="item.nodeType === 'number'" class="htree-val htree-val-number">{{ item.preview }}</span>
      <span v-else-if="item.nodeType === 'boolean'" class="htree-val htree-val-bool">{{ item.preview }}</span>
      <span v-else-if="item.nodeType === 'null'" class="htree-val htree-val-null">null</span>
      <span v-else-if="item.nodeType === 'object' && !item.hasChildren" class="htree-val">&#123;&#125;</span>
      <span v-else-if="item.nodeType === 'array' && !item.hasChildren" class="htree-val">[]</span>

      <!-- length badge for object/array -->
      <span v-if="item.hasChildren" class="htree-length">{{ item.nodeType === 'array' ? '[' + item.childCount + ']' : '{' + item.childCount + '}' }}</span>
    </div>
    <div v-if="flatNodes.length === 0" class="htree-empty">(empty)</div>
  </div>
</template>

<script>
export default {
  name: 'JsonHighlightTree',
  props: {
    data: { type: [Object, Array, String, Number, Boolean], default: null },
    highlightPath: { type: String, default: '' },
    deep: { type: Number, default: 3 }
  },
  data() {
    return {
      /** Set of expanded paths */
      expandedSet: new Set()
    }
  },
  computed: {
    /** Build flat visible node list from JSON data */
    flatNodes() {
      if (this.data === null || this.data === undefined) return []
      const result = []
      const path = 'root'
      this._expandNode(this.data, path, null, 0, result)
      return result
    }
  },
  watch: {
    data: {
      deep: true,
      handler() {
        this.$nextTick(() => {
          if (this.highlightPath) this._revealPath(this.highlightPath)
        })
      }
    },
    highlightPath(path) {
      if (path) {
        this.$nextTick(() => this._revealPath(path))
      }
    }
  },
  mounted() {
    if (this.highlightPath) {
      this.$nextTick(() => this._revealPath(this.highlightPath))
    }
  },
  methods: {
    /**
     * Recursively walk JSON and push visible nodes into flat array.
     * @param {any} val - current value
     * @param {string} path - current JSON path (root.key, root.arr[0], etc.)
     * @param {string|null} key - key name (null for root/array items)
     * @param {number} depth - nesting depth
     * @param {Array} out - output array
     */
    _expandNode(val, path, key, depth, out) {
      const isPrimitive = val === null || val === undefined || typeof val !== 'object'
      const isArray = Array.isArray(val)
      const isObj = !isPrimitive && !isArray

      const isExpanded = this.expandedSet.has(path) || depth < this.deep

      if (isPrimitive) {
        out.push(this._makeItem(path, key, depth, val, false, 0))
        return
      }

      const keys = isArray ? val.map((_, i) => i) : Object.keys(val)
      const childCount = keys.length

      // Push the container node
      out.push(this._makeItem(path, key, depth, null, true, childCount, isObj ? 'object' : 'array', isExpanded))

      if (isExpanded) {
        for (const k of keys) {
          const childPath = isArray ? `${path}[${k}]` : (path ? `${path}.${k}` : k)
          const childVal = isArray ? val[k] : val[k]
          const childKey = isArray ? null : k
          this._expandNode(childVal, childPath, childKey, depth + 1, out)
        }
      }
    },

    _makeItem(path, key, depth, val, hasChildren, childCount, nodeType, expanded) {
      const t = nodeType || (val === null ? 'null' : typeof val)
      let preview = ''
      if (val !== null && typeof val !== 'object') preview = String(val)
      else if (val === null) preview = 'null'
      return {
        path,
        key: key !== undefined ? key : null,
        depth,
        value: val,
        nodeType: t,
        hasChildren,
        childCount,
        expanded: !!expanded,
        preview
      }
    },

    /** Expand all ancestors of a path, then scroll/highlight the target */
    _revealPath(path) {
      if (!path) return
      // Expand all ancestor paths
      const parts = path.split('.')
      let acc = ''
      for (const p of parts) {
        // Handle array indices in path: root.arr[0].name → expand root, root.arr, root.arr[0]
        const bracketIdx = p.indexOf('[')
        if (bracketIdx > 0) {
          const key = p.substring(0, bracketIdx)
          if (acc) {
            this.expandedSet.add(`${acc}.${key}`)
            acc = `${acc}.${key}`
          } else {
            this.expandedSet.add(key)
            acc = key
          }
          // Add the indexed version
          this.expandedSet.add(`${acc}${p.substring(bracketIdx)}`)
          acc = `${acc}${p.substring(bracketIdx)}`
        } else {
          acc = acc ? `${acc}.${p}` : p
          this.expandedSet.add(acc)
        }
      }

      // Force re-compute then scroll
      this.$nextTick(() => {
        const el = this.$refs.hlNode
        if (el) {
          const node = Array.isArray(el) ? el[0] : el
          if (node) {
            const container = this.$refs.treeContainer
            if (container) {
              const top = node.offsetTop - container.offsetTop - 60
              container.scrollTop = Math.max(0, top)
            }
          }
        }
      })
    },

    toggleNode(path) {
      if (this.expandedSet.has(path)) {
        this.expandedSet.delete(path)
      } else {
        this.expandedSet.add(path)
      }
      // Force re-compute
      this.$forceUpdate()
    },

    onRowClick(item) {
      if (item.path) {
        this.$emit('node-click', item.path)
      }
    }
  }
}
</script>

<style scoped>
.json-htree {
  height: 100%;
  overflow: auto;
  font-family: Consolas, 'Courier New', monospace;
  font-size: 13px;
  background: #fafafa;
  user-select: none;
}

.htree-row {
  display: flex;
  align-items: center;
  gap: 3px;
  padding: 2px 8px;
  cursor: default;
  white-space: nowrap;
  min-height: 24px;
  line-height: 24px;
  border-bottom: 1px solid #f0f0f0;
}

.htree-row:hover {
  background: #ecf5ff;
}

.htree-row.htree-row-highlight {
  background: #fef0e0 !important;
  box-shadow: inset 3px 0 0 #e6a23c;
}

.htree-row-parent {
  cursor: pointer;
}

.htree-toggle {
  flex-shrink: 0;
  width: 14px;
  text-align: center;
  font-size: 12px;
  color: #909399;
}

.htree-toggle-placeholder {
  visibility: hidden;
}

.htree-badge {
  flex-shrink: 0;
  font-size: 10px;
  padding: 0 3px;
  border-radius: 2px;
  margin-right: 2px;
  font-weight: bold;
}

.htree-badge-object {
  color: #409eff;
}

.htree-badge-array {
  color: #67c23a;
}

.htree-badge-null {
  color: #909399;
}

.htree-key {
  color: #881391;
  font-weight: 500;
}

.htree-colon {
  color: #333;
}

.htree-val {
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
}

.htree-val-string {
  color: #67c23a;
}

.htree-val-number {
  color: #e6a23c;
}

.htree-val-bool {
  color: #9b59b6;
}

.htree-val-null {
  color: #c0c4cc;
  font-style: italic;
}

.htree-length {
  font-size: 11px;
  color: #c0c4cc;
  margin-left: 4px;
}

.htree-empty {
  padding: 20px;
  text-align: center;
  color: #c0c4cc;
}
</style>
