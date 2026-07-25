<template>
  <div ref="scrollContainer" class="json-table-grid" @scroll="onScroll">
    <div
      ref="innerLayer"
      class="json-table-inner"
      :style="{ width: gridWidth + 'px', height: gridHeight + 'px' }"
    >
      <div
        v-for="(cell, idx) in visibleCells"
        :key="idx"
        class="json-tcell"
        :class="[
          `cell-${cell.type}`,
          cell.id ? 'cell-clickable' : '',
          highlightPath === cell.id ? 'cell-highlight' : ''
        ]"
        :style="{
          left: cell.x + 'px',
          top: cell.y + 'px',
          width: cell.w + 'px',
          height: cell.h + 'px',
          lineHeight: cell.h + 'px'
        }"
        :title="cell.id || cell.text"
        @click="onCellClick(cell)"
      >{{ cell.text }}</div>
    </div>
  </div>
</template>

<script>
import { buildTableGrid } from '@/views/maximo/excelimport/tableGridBuilder'

export default {
  name: 'JsonTableGrid',
  props: {
    data: { type: [Object, Array, String, Number, Boolean], default: null },
    highlightPath: { type: String, default: '' }
  },
  data() {
    return {
      cells: [],
      gridWidth: 0,
      gridHeight: 0,
      cellMap: {},
      visibleCells: [],
      scrollTop: 0,
      scrollLeft: 0
    }
  },
  watch: {
    data: {
      deep: true,
      handler() {
        this.$nextTick(() => this.buildGrid())
      }
    },
    highlightPath(path) {
      if (path) {
        this.scrollToCell(path)
      }
    }
  },
  mounted() {
    this.buildGrid()
  },
  methods: {
    buildGrid() {
      if (!this.data) {
        this.cells = []
        this.gridWidth = 0
        this.gridHeight = 0
        this.cellMap = {}
        this.visibleCells = []
        return
      }
      const grid = buildTableGrid(this.data)
      this.cells = grid.cells
      this.gridWidth = grid.width
      this.gridHeight = grid.height
      this.cellMap = grid.cellMap
      this.updateVisibleCells()
    },

    getViewport() {
      const el = this.$refs.scrollContainer
      if (!el) return { top: 0, bottom: 0, left: 0, right: 0 }
      return {
        top: el.scrollTop - 100,
        bottom: el.scrollTop + el.clientHeight + 100,
        left: el.scrollLeft - 100,
        right: el.scrollLeft + el.clientWidth + 100
      }
    },

    updateVisibleCells() {
      const vp = this.getViewport()
      this.visibleCells = this.cells.filter(c =>
        c.y + c.h > vp.top && c.y < vp.bottom &&
        c.x + c.w > vp.left && c.x < vp.right
      )
    },

    onScroll() {
      this.updateVisibleCells()
    },

    onCellClick(cell) {
      if (cell.id) {
        this.$emit('cell-click', cell.id)
      }
    },

    /** 滚动到指定路径的单元格并高亮 */
    scrollToCell(path) {
      const cell = this.cellMap[path]
      if (!cell) return
      const el = this.$refs.scrollContainer
      if (!el) return

      const targetLeft = cell.x + cell.w / 2 - el.clientWidth / 2
      const targetTop = cell.y + cell.h / 2 - el.clientHeight / 2
      el.scrollLeft = Math.max(0, targetLeft)
      el.scrollTop = Math.max(0, targetTop)

      this.$emit('update:highlightPath', path)
    }
  }
}
</script>

<style scoped>
.json-table-grid {
  height: 100%;
  overflow: auto;
  position: relative;
  background: #fff;
  font-family: Consolas, 'Courier New', monospace;
  font-size: 12px;
}

.json-table-inner {
  position: relative;
}

.json-tcell {
  position: absolute;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  padding: 0 8px;
  box-sizing: border-box;
  border-bottom: 1px solid #ebeef5;
  border-right: 1px solid #ebeef5;
  font-size: 12px;
  transition: background 0.15s;
}

/* 类型颜色 */
.cell-key {
  background: #f5f7fa;
  color: #409eff;
  font-weight: 600;
}

.cell-string {
  color: #67c23a;
}

.cell-number {
  color: #e6a23c;
}

.cell-bool {
  color: #9b59b6;
}

.cell-null {
  color: #c0c4cc;
  font-style: italic;
}

.cell-header {
  background: #ebeef5;
  font-weight: 700;
  color: #303133;
  text-align: center;
}

.cell-index {
  color: #909399;
  text-align: center;
  background: #fafafa;
}

.cell-value {
  color: #606266;
}

/* 交互 */
.cell-clickable {
  cursor: pointer;
}

.cell-clickable:hover {
  background: #ecf5ff !important;
}

.cell-highlight {
  background: #fef0e0 !important;
  box-shadow: inset 0 0 0 2px #e6a23c;
}
</style>
