<template>
  <section class="excel-paste-test">
    <el-card>
      <div class="page-header">
        <h2>Excel 粘贴测试</h2>
        <p class="page-desc">从 Excel 中复制数据，粘贴到下方表格中</p>
        <div class="cursor-info" v-if="currentCell">
          当前位置: <span class="cell-pos">{{ currentCell.rowNum }}{{ currentCell.col }}</span>
        </div>
      </div>

      <div class="paste-area" @paste="handlePaste">
        <div class="paste-hint">
          <el-icon name="el-icon-copy-document" class="hint-icon" />
          <span>在此区域粘贴数据（或直接粘贴到表格）</span>
        </div>
        <textarea
          ref="pasteInput"
          class="paste-input"
          placeholder="在此粘贴从 Excel 复制的数据..."
          @paste="handlePaste"
        ></textarea>
      </div>

      <div class="table-area">
        <div class="table-toolbar">
          <span class="record-count">共 {{ tableData.length }} 行</span>
          <el-button type="text" icon="el-icon-delete" @click="clearTable">清空表格</el-button>
        </div>
        <el-table :data="tableData" border stripe style="width: 100%" @paste.native="handleTablePaste">
          <el-table-column prop="rowNum" label="序号" width="60" align="center" />
          <el-table-column
            v-for="col in columns"
            :key="col.key"
            :prop="col.key"
            :label="col.label"
            width="120"
          >
            <template slot-scope="scope">
              <el-input
                v-model="scope.row[col.key]"
                size="mini"
                @focus="handleCellFocus(scope.$index, col.key)"
                @click="handleCellClick(scope.$index, col.key)"
              />
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>
  </section>
</template>

<script>
const COLUMNS = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K']

export default {
  name: 'TestExcelPaste',
  data() {
    return {
      columns: COLUMNS.map(c => ({ key: c, label: c })),
      tableData: [],
      currentRowIndex: 0,
      currentColIndex: 0
    }
  },
  computed: {
    currentCell() {
      return {
        rowNum: this.currentRowIndex + 1,
        col: COLUMNS[this.currentColIndex]
      }
    }
  },
  created() {
    this.initTable()
  },
  methods: {
    initTable() {
      const rows = []
      for (let i = 1; i <= 50; i++) {
        const row = { rowNum: i }
        COLUMNS.forEach(col => {
          row[col] = ''
        })
        rows.push(row)
      }
      this.tableData = rows
    },
    handleCellFocus(rowIndex, colKey) {
      this.currentRowIndex = rowIndex
      this.currentColIndex = COLUMNS.indexOf(colKey)
    },
    handleCellClick(rowIndex, colKey) {
      this.currentRowIndex = rowIndex
      this.currentColIndex = COLUMNS.indexOf(colKey)
    },
    handlePaste(e) {
      e.preventDefault()
      const clipboardData = e.clipboardData || window.clipboardData
      const text = clipboardData.getData('text')
      if (text) {
        this.parseAndPasteData(text)
      }
    },
    handleTablePaste(e) {
      e.preventDefault()
      const clipboardData = e.clipboardData || window.clipboardData
      const text = clipboardData.getData('text')
      if (text) {
        this.parseAndPasteData(text)
      }
    },
    parseAndPasteData(text) {
      const rows = text.split('\n').filter(line => line.trim())
      const data = rows.map(row => row.split('\t'))
      
      data.forEach((rowData, dataRowIndex) => {
        const targetRowIndex = this.currentRowIndex + dataRowIndex
        if (targetRowIndex >= this.tableData.length) return
        
        rowData.forEach((cell, dataColIndex) => {
          const targetColIndex = this.currentColIndex + dataColIndex
          if (targetColIndex >= COLUMNS.length) return
          
          this.tableData[targetRowIndex][COLUMNS[targetColIndex]] = cell.trim()
        })
      })
      
      this.$message.success(`成功粘贴 ${data.length} 行数据，起始位置: ${this.currentCell.rowNum}${this.currentCell.col}`)
    },
    handleCellChange(row) {},
    clearTable() {
      this.initTable()
      this.currentRowIndex = 0
      this.currentColIndex = 0
      this.$message.info('已清空表格')
    }
  }
}
</script>

<style lang="scss" scoped>
.excel-paste-test {
  padding: 16px;
}
.page-header {
  margin-bottom: 16px;
}
.page-header h2 {
  margin: 0 0 4px 0;
  font-size: 18px;
  font-weight: 600;
}
.page-desc {
  margin: 0;
  color: #909399;
  font-size: 13px;
}
.cursor-info {
  margin-top: 8px;
  font-size: 13px;
  color: #606266;
}
.cell-pos {
  font-weight: 600;
  color: #409eff;
}
.paste-area {
  margin-bottom: 16px;
}
.paste-hint {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  color: #606266;
  font-size: 13px;
}
.hint-icon {
  font-size: 16px;
  color: #409eff;
}
.paste-input {
  width: 100%;
  height: 100px;
  padding: 12px;
  border: 2px dashed #dcdfe6;
  border-radius: 6px;
  font-size: 13px;
  line-height: 1.6;
  resize: none;
  transition: border-color 0.3s;
}
.paste-input:focus {
  outline: none;
  border-color: #409eff;
}
.table-area {
  margin-top: 16px;
}
.table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.record-count {
  color: #606266;
  font-size: 13px;
}
</style>