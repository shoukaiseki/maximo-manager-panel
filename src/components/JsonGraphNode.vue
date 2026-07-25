<template>
  <div class="json-graph-node">
    <!-- 当前节点行 -->
    <div
      class="node-row"
      :class="[`type-${valueType}`, { 'is-expandable': isExpandable, 'is-expanded': localExpanded, 'is-root': isRoot }]"
      @click="handleToggle"
    >
      <!-- 展开/折叠图标 -->
      <span v-if="isExpandable" class="toggle-icon" :class="{ expanded: localExpanded }">
        <svg width="10" height="10" viewBox="0 0 10 10">
          <polygon points="2,2 8,5 2,8" fill="currentColor" />
        </svg>
      </span>
      <span v-else class="toggle-icon spacer"></span>

      <!-- 名称 (key) -->
      <span v-if="name !== undefined && name !== null" class="node-key" :title="String(name)">
        <span v-if="isArrayItem" class="array-index">{{ name }}</span>
        <span v-else class="object-key">{{ name }}</span>
        <span class="key-colon">:</span>
      </span>

      <!-- 值 -->
      <span v-if="!isExpandable" class="node-value" :class="`value-${valueType}`">
        <template v-if="valueType === 'string'">
          <span class="quote">&quot;</span>{{ displayValue }}<span class="quote">&quot;</span>
        </template>
        <template v-else-if="valueType === 'null'">
          <span class="null-keyword">null</span>
        </template>
        <template v-else-if="valueType === 'undefined'">
          <span class="null-keyword">undefined</span>
        </template>
        <template v-else>
          {{ displayValue }}
        </template>
      </span>

      <!-- 展开后的摘要信息 -->
      <span v-if="isExpandable && !localExpanded" class="node-summary">
        {{ valueType === 'object' ? '{ ' + childKeys + ' }' : '[ ' + childKeys + ' ]' }}
      </span>

      <!-- 长度/条目数标签 -->
      <span v-if="isExpandable" class="node-length-badge" :class="`badge-${valueType}`">
        {{ childCount }}
      </span>
    </div>

    <!-- 子节点列表 (展开时) -->
    <div v-if="isExpandable && localExpanded" class="children-container">
      <div
        v-for="(child, index) in childEntries"
        :key="index"
        class="child-wrapper"
      >
        <!-- 垂直连接线 -->
        <div class="tree-line" :class="{ 'has-next': index < childEntries.length - 1 }"></div>
        <json-graph-node
          :value="child.value"
          :name="child.key"
          :depth="depth + 1"
          :is-array-item="valueType === 'array'"
          :is-root="false"
        />
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'JsonGraphNode',
  props: {
    value: { type: [Object, Array, String, Number, Boolean], default: null },
    name: { type: [String, Number], default: undefined },
    depth: { type: Number, default: 0 },
    isRoot: { type: Boolean, default: false },
    isArrayItem: { type: Boolean, default: false },
    expandedDepth: { type: Number, default: 2 }
  },
  data() {
    return {
      localExpanded: this.depth < this.expandedDepth
    }
  },
  computed: {
    valueType() {
      if (this.value === null) return 'null'
      if (this.value === undefined) return 'undefined'
      if (Array.isArray(this.value)) return 'array'
      return typeof this.value
    },
    isExpandable() {
      return this.valueType === 'object' || this.valueType === 'array'
    },
    childEntries() {
      if (this.valueType === 'object') {
        return Object.keys(this.value).map(k => ({ key: k, value: this.value[k] }))
      }
      if (this.valueType === 'array') {
        return this.value.map((v, i) => ({ key: i, value: v }))
      }
      return []
    },
    childCount() {
      if (this.valueType === 'object') return Object.keys(this.value).length
      if (this.valueType === 'array') return this.value.length
      return 0
    },
    childKeys() {
      const keys = this.childEntries.map(e => {
        const v = e.value
        if (v === null) return 'null'
        if (v === undefined) return 'undefined'
        if (Array.isArray(v)) return '[...]'
        if (typeof v === 'object') return '{...}'
        if (typeof v === 'string') return '"' + String(v).substring(0, 20) + '"'
        return String(v)
      })
      if (keys.length <= 3) return keys.join(', ')
      return keys.slice(0, 3).join(', ') + ', ...'
    },
    displayValue() {
      if (this.valueType === 'string') {
        const s = String(this.value)
        return s.length > 200 ? s.substring(0, 200) + '…' : s
      }
      if (this.valueType === 'number') return String(this.value)
      if (this.valueType === 'boolean') return String(this.value)
      return ''
    }
  },
  methods: {
    handleToggle() {
      if (this.isExpandable) {
        this.localExpanded = !this.localExpanded
      }
    }
  }
}
</script>

<style scoped>
.json-graph-node {
  font-family: 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 13px;
  line-height: 1.5;
  user-select: none;
}

/* ── 节点行 ── */
.node-row {
  display: flex;
  align-items: center;
  padding: 3px 6px;
  border-radius: 4px;
  cursor: default;
  transition: background 0.15s;
  gap: 2px;
  position: relative;
}
.node-row:hover {
  background: rgba(0, 0, 0, 0.04);
}
.node-row.is-root {
  padding: 4px 8px;
  background: #f8f9fb;
  border-radius: 6px;
  margin-bottom: 2px;
}

/* ── 展开/折叠图标 ── */
.toggle-icon {
  flex-shrink: 0;
  width: 16px;
  height: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
  transition: transform 0.2s;
  cursor: pointer;
}
.toggle-icon.expanded {
  transform: rotate(90deg);
}
.toggle-icon.spacer {
  visibility: hidden;
}

/* ── 名称/键 ── */
.node-key {
  flex-shrink: 0;
  margin-right: 1px;
}
.array-index {
  color: #7c4dff;
  font-weight: 500;
  font-size: 12px;
}
.object-key {
  color: #881391;
  font-weight: 500;
}
.key-colon {
  color: #999;
  margin-left: 1px;
}

/* ── 值 ── */
.node-value {
  flex-shrink: 0;
  max-width: 600px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.value-string {
  color: #0b8235;
}
.value-number {
  color: #1750eb;
}
.value-boolean {
  color: #c41a16;
}
.value-null, .value-undefined {
  font-style: italic;
}
.null-keyword {
  color: #999;
}
.quote {
  color: #0b8235;
  opacity: 0.6;
}

/* ── 摘要 (收起时) ── */
.node-summary {
  color: #999;
  font-size: 12px;
  margin-left: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  min-width: 0;
}

/* ── 条目数标签 ── */
.node-length-badge {
  flex-shrink: 0;
  margin-left: auto;
  padding: 0 6px;
  font-size: 11px;
  font-weight: 600;
  border-radius: 8px;
  line-height: 18px;
  min-width: 18px;
  text-align: center;
}
.badge-object {
  background: #f3e5f5;
  color: #7b1fa2;
}
.badge-array {
  background: #e8eaf6;
  color: #283593;
}

/* ── 子节点容器 ── */
.children-container {
  position: relative;
  padding-left: 20px;
}

/* ── 子节点包裹器 ── */
.child-wrapper {
  position: relative;
}

/* ── 树形连接线 ── */
.tree-line {
  position: absolute;
  left: -12px;
  top: 0;
  width: 1px;
  height: 100%;
  border-left: 1px solid #d0d5dd;
  opacity: 0.5;
}
.tree-line.has-next {
  opacity: 1;
}
</style>
