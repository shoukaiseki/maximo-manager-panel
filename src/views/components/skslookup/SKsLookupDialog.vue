<template>
  <el-dialog
    :visible.sync="visible"
    :title="dialogTitle"
    width="900px"
    append-to-body
    :close-on-click-modal="false"
    @open="handleDialogOpen"
  >
    <div class="sks-lookup-toolbar">
      <el-input
        v-model="keyword"
        size="mini"
        clearable
        :placeholder="keywordPlaceholder"
        style="width: 220px"
        @keyup.enter.native="handleSearch"
        @clear="handleSearch"
      />
      <el-button size="mini" type="primary" icon="el-icon-search" @click="handleSearch">查询</el-button>
      <el-button size="mini" icon="el-icon-refresh" @click="loadData">刷新</el-button>
      <span class="sks-lookup-info">
        <span v-if="objectname">{{ objectname }}</span>
        <span v-if="total"> · 共 {{ total }} 条</span>
      </span>
    </div>

    <el-table
      v-loading="loading"
      :data="rows"
      size="mini"
      border
      height="380"
      highlight-current-row
      @row-click="handleRowClick"
      @sort-change="handleSortChange"
    >
      <el-table-column
        v-for="col in tableColumns"
        :key="col.dataattribute"
        :prop="col.dataattribute"
        :label="col.label"
        :width="col.width || undefined"
        :sortable="col.sortable ? 'custom' : false"
        show-overflow-tooltip
      >
        <template slot-scope="scope">{{ formatCell(scope.row, col.dataattribute) }}</template>
      </el-table-column>
      <template slot="empty">
        <span>{{ errorMessage || '暂无数据' }}</span>
      </template>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      :page.sync="pageNum"
      :limit.sync="pageSizeValue"
      @pagination="loadData"
    />

    <div slot="footer">
      <el-button size="mini" @click="close">取消</el-button>
    </div>
  </el-dialog>
</template>

<script>
/**
 * 通用 Lookup 选择对话框
 *
 * 用法(页面中):
 *   import SKsLookupDialog from '@/views/components/skslookup/SKsLookupDialog.vue'
 *   // 必须用 kebab 键注册: 标签 <sks-lookup-dialog> 只会被解析成 SksLookupDialog, 匹配不到 SKsLookupDialog
 *   components: { 'sks-lookup-dialog': SKsLookupDialog }
 *   <sks-lookup-dialog ref="assetLookup" lookup="asset" :mbo-data="formData"
 *                      :src-keys="['assetnum','description']" :target-keys="['assetnum','description']"
 *                      :list-where="buildLookupWhere" @selectrecord="handleAssetSelected" />
 *   this.$refs.assetLookup.open()
 *
 * 说明:
 *   1. lookup 对应同目录 sksLookup.js 中的配置, 配置提供列/默认排序/默认 where/对象名;
 *   2. listWhere 为函数时先执行, 用返回值作为 where(listWhere 返回空串时退回配置的 whereclause);
 *   3. 点击行调用 selectrecord 回调, 未传该回调时执行默认方法:
 *      把行数据中 srcKeys 对应的字段依次写入 mboData 的 targetKeys 字段(未传 targetKeys 时与 srcKeys 相同);
 *   4. 服务端接口为 Maximo 自动化脚本 SKS_LOOKUP_API。
 */
import {
  getLookupConfig,
  getLookupColumns,
  getLookupKeyColumns,
  getLookupObjectName,
  normalizeLookupColumns
} from './sksLookup'
import { queryLookupData } from '@/api/sksLookup'

export default {
  name: 'SKsLookupDialog',
  props: {
    // 主对象 js 数据(默认方法把选择行的字段写入该对象)
    mboData: {
      type: Object,
      default: null
    },
    // lookup 名称, 对应同目录 sksLookup.js
    lookup: {
      type: String,
      default: ''
    },
    // 选择行的字段数组(源字段), 未设置时取 lookup 配置中的关键列
    srcKeys: {
      type: [Array, String],
      default: () => []
    },
    // 主对象字段数组(目标字段), 未设置时与 srcKeys 一一对应
    targetKeys: {
      type: [Array, String],
      default: () => []
    },
    // 排序(如 "assetnum desc"), 未设置时取 lookup 配置的 orderby
    listOrder: {
      type: String,
      default: ''
    },
    // 关系对象: 列表查询的对象名(传关系名时由服务端解析)
    relationObject: {
      type: String,
      default: ''
    },
    // where 条件, 为函数时执行后取返回值作为 where(与 Maximo lookup 取值一致)
    listWhere: {
      type: [String, Function],
      default: ''
    },
    // 点击行回调方法(不传则执行默认方法)
    selectrecord: {
      type: Function,
      default: null
    },
    // 对话框标题
    title: {
      type: String,
      default: ''
    },
    // 每页条数
    pageSize: {
      type: Number,
      default: 20
    },
    // 查询失败时是否弹出错误消息
    showError: {
      type: Boolean,
      default: true
    }
  },
  data() {
    return {
      visible: false,
      loading: false,
      keyword: '',
      sortOrder: '',
      errorMessage: '',
      rows: [],
      total: 0,
      pageNum: 1,
      pageSizeValue: 20,
      columns: [],
      objectname: ''
    }
  },
  computed: {
    dialogTitle() {
      if (this.title) {
        return this.title
      }
      return this.lookup ? '选择 - ' + this.lookup : '选择'
    },
    keywordPlaceholder() {
      const names = this.keywordColumns
      return names.length ? '关键字(' + names.join(',') + ')' : '请输入关键字'
    },
    // 列定义: 服务端返回的列(含描述)优先, 否则用 _lookup.js 的配置
    tableColumns() {
      if (this.columns.length) {
        return this.columns
      }
      return normalizeLookupColumns(getLookupColumns(this.lookup))
    },
    // 关键字搜索字段(可过滤且不是关系路径的列)
    keywordColumns() {
      const result = []
      this.tableColumns.forEach(col => {
        if (col.filterable === false) {
          return
        }
        if (col.dataattribute.indexOf('.') > -1) {
          return
        }
        result.push(col.dataattribute)
      })
      return result
    },
    effectiveSrcKeys() {
      const keys = this.normalizeKeys(this.srcKeys)
      if (keys.length) {
        return keys
      }
      const keyColumns = getLookupKeyColumns(this.lookup)
      return keyColumns.length ? keyColumns : this.tableColumns.map(col => col.dataattribute)
    },
    effectiveTargetKeys() {
      const keys = this.normalizeKeys(this.targetKeys)
      return keys.length ? keys : this.effectiveSrcKeys
    },
    // 排序: 表格点击排序 > listOrder 属性 > lookup 配置
    effectiveListOrder() {
      if (this.sortOrder) {
        return this.sortOrder
      }
      if (this.listOrder) {
        return this.listOrder
      }
      const config = getLookupConfig(this.lookup)
      return config && config.orderby ? config.orderby : ''
    }
  },
  watch: {
    pageSize: {
      immediate: true,
      handler(val) {
        if (val) {
          this.pageSizeValue = val
        }
      }
    }
  },
  methods: {
    /** 打开对话框 */
    open() {
      this.keyword = ''
      this.sortOrder = ''
      this.errorMessage = ''
      this.pageNum = 1
      // 列/对象名必须清空: 组件被多个 lookup 复用(如先开「选择角色」再开「选择操作」),
      // 残留上一次服务端返回的列会导致本次表格列与关键字过滤字段全是上一个 lookup 的字段
      this.columns = []
      this.objectname = ''
      this.visible = true
    },
    /** 关闭对话框 */
    close() {
      this.visible = false
    },
    handleDialogOpen() {
      this.loadData()
    },
    handleSearch() {
      this.pageNum = 1
      this.loadData()
    },
    /** 表格排序: 转为 Maximo orderby */
    handleSortChange(param) {
      if (param && param.prop && param.order) {
        this.sortOrder = param.prop + (param.order === 'descending' ? ' desc' : ' asc')
      } else {
        this.sortOrder = ''
      }
      this.pageNum = 1
      this.loadData()
    },
    /** 解析 listWhere: 函数先执行并取返回值 */
    resolveListWhere() {
      const listWhere = this.listWhere
      if (typeof listWhere === 'function') {
        const value = listWhere.call(this, this.mboData, this)
        if (value !== null && value !== undefined) {
          return String(value)
        }
      } else if (listWhere) {
        return String(listWhere)
      }
      const config = getLookupConfig(this.lookup)
      return config && config.whereclause ? config.whereclause : ''
    },
    buildRequestData() {
      const config = getLookupConfig(this.lookup)
      return {
        lookup: this.lookup,
        objectname: getLookupObjectName(this.lookup, this.relationObject),
        relationship: config && config.relationship ? config.relationship : '',
        where: this.resolveListWhere(),
        orderby: this.effectiveListOrder,
        columns: this.tableColumns,
        keyword: this.keyword,
        keywordColumns: this.keywordColumns,
        pageNum: this.pageNum,
        pageSize: this.pageSizeValue
      }
    },
    /** 查询列表数据 */
    loadData() {
      if (!this.lookup && !this.relationObject) {
        this.rows = []
        this.total = 0
        this.errorMessage = '未指定 lookup 或 relationObject'
        return
      }
      this.loading = true
      this.errorMessage = ''
      queryLookupData(this.buildRequestData()).then(res => {
        const body = res && res.data ? res.data : res
        if (body && body.status === 'error') {
          throw new Error(body.message || '查询失败')
        }
        const data = body && body.data ? body.data : (body || {})
        this.rows = data.rows || []
        this.total = Number(data.total || 0)
        this.objectname = data.objectname || data.object || ''
        if (data.columns && data.columns.length) {
          this.columns = normalizeLookupColumns(data.columns)
        }
      }).catch(err => {
        this.rows = []
        this.total = 0
        this.errorMessage = '查询失败: ' + (err && err.message ? err.message : String(err))
        if (this.showError) {
          this.$message.error(this.errorMessage)
        }
      }).finally(() => {
        this.loading = false
      })
    },
    /** 点击行: 有 selectrecord 回调则调用, 否则执行默认方法 */
    handleRowClick(row) {
      const payload = {
        lookup: this.lookup,
        objectname: this.objectname,
        row: row,
        mboData: this.mboData,
        srcKeys: this.effectiveSrcKeys,
        targetKeys: this.effectiveTargetKeys,
        columns: this.tableColumns,
        config: getLookupConfig(this.lookup),
        dialog: this
      }
      if (typeof this.selectrecord === 'function') {
        this.selectrecord(row, payload)
      } else {
        this.defaultSelectRecord(row)
      }
      this.$emit('selectrecord', row, payload)
      this.visible = false
    },
    /** 默认方法: 把选择行的 srcKeys 字段写入 mboData 的 targetKeys 字段 */
    defaultSelectRecord(row) {
      if (!this.mboData) {
        return
      }
      const srcKeys = this.effectiveSrcKeys
      const targetKeys = this.effectiveTargetKeys
      for (let i = 0; i < srcKeys.length; i++) {
        const targetKey = targetKeys[i] || srcKeys[i]
        this.setTargetValue(targetKey, this.getRowValue(row, srcKeys[i]))
      }
    },
    /** 支持 a.b 形式的嵌套字段 */
    setTargetValue(targetKey, value) {
      if (!targetKey) {
        return
      }
      const paths = String(targetKey).split('.')
      let obj = this.mboData
      for (let i = 0; i < paths.length - 1; i++) {
        const path = paths[i]
        if (!obj[path] || typeof obj[path] !== 'object') {
          this.$set(obj, path, {})
        }
        obj = obj[path]
      }
      this.$set(obj, paths[paths.length - 1], value)
    },
    /** 行取值(字段名大小写不敏感) */
    getRowValue(row, key) {
      if (!row || !key) {
        return undefined
      }
      if (row[key] !== undefined) {
        return row[key]
      }
      const lowerKey = String(key).toLowerCase()
      const keys = Object.keys(row)
      for (let i = 0; i < keys.length; i++) {
        if (keys[i].toLowerCase() === lowerKey) {
          return row[keys[i]]
        }
      }
      return undefined
    },
    formatCell(row, key) {
      const value = this.getRowValue(row, key)
      return value === null || value === undefined ? '' : String(value)
    },
    normalizeKeys(keys) {
      if (!keys) {
        return []
      }
      if (Array.isArray(keys)) {
        return keys.filter(key => !!key)
      }
      return String(keys).split(',').map(key => key.trim()).filter(key => !!key)
    }
  }
}
</script>

<style lang="scss" scoped>
.sks-lookup-toolbar {
  display: flex;
  align-items: center;
  margin-bottom: 8px;

  .el-button {
    margin-left: 6px;
  }

  .sks-lookup-info {
    margin-left: auto;
    color: #909399;
    font-size: 12px;
  }
}
</style>
