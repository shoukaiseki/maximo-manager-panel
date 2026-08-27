<template>
  <section class="query-page">
    <el-card>
      <div class="page-header-row">
        <div>
          <h2>工单库存余量分析</h2>
          <p class="page-summary">统计未完工工单的物料需求数量、当前库存余量与预约数量，支持按工单号、物料号过滤。</p>
        </div>
      </div>

      <el-form :model="formData" ref="queryForm" :inline="true" label-width="90px" @submit.native.prevent>
        <el-form-item label="工单号">
          <el-input v-model="formData.wonum" placeholder="=精确/%模糊" clearable style="width: 220px;" @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item label="物料号">
          <el-input v-model="formData.itemnum" placeholder="=精确/%模糊" clearable style="width: 220px;" @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item>
          <el-button type="cyan" icon="el-icon-search" size="mini" :loading="loading" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" size="mini" @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="result-panel">
        <SksTable
          width="100%"
          :showTableColumnButton="true"
          :showRefreshButton="false"
          :mainTable="mainTable"
          :visibleTop="true"
          @refresh="fetchList"
          @rowClickAfter="handleRowClick">
          <template slot="none-V_STATUS" slot-scope="{ row }">
            <el-tag :type="row.V_STATUS === '充足' ? 'success' : 'danger'" size="small">
              {{ row.V_STATUS }}
            </el-tag>
          </template>
        </SksTable>
        <el-empty v-if="!loading && total === 0 && hasSearched" description="暂无查询结果" />
        <el-empty v-if="!loading && total === 0 && !hasSearched" description="请输入关键词后点击搜索" />
      </div>
    </el-card>

    <el-dialog title="工单库存余量详情" :visible.sync="detailDialogVisible" width="70%" top="8vh" custom-class="detail-dialog" @open="loadDetailData">
        <el-descriptions :column="3" border size="mini">
          <el-descriptions-item label="站点">{{ detailRow.SITEID }}</el-descriptions-item>
          <el-descriptions-item label="工单号">{{ detailRow.WONUM }}</el-descriptions-item>
          <el-descriptions-item label="物料号">{{ detailRow.ITEMNUM }}</el-descriptions-item>
          <el-descriptions-item label="位置">{{ detailRow.LOCATION }}</el-descriptions-item>
          <el-descriptions-item label="物料名称">{{ detailRow.DESCRIPTION }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="detailRow.V_STATUS === '充足' ? 'success' : 'danger'" size="small">{{ detailRow.V_STATUS }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="需求数量">{{ formatNum(detailRow.ITEMQTY) }}</el-descriptions-item>
          <el-descriptions-item label="当前库存余量">{{ formatNum(detailRow.CURBAL) }}</el-descriptions-item>
          <el-descriptions-item label="预约数量">{{ formatNum(detailRow.RESERVEDQTY) }}</el-descriptions-item>
        </el-descriptions>
        <el-tabs v-model="detailTab" style="margin-top: 16px;">
          <el-tab-pane label="当前工单物料" name="wp">
            <div :class="['detail-table-scroll', { 'scroll-all': detailMainTables.wp.showAllColumn, 'show-prop': detailMainTables.wp.showTablePropName }]">
              <SksTable
                width="100%"
                :showRefreshButton="false"
                :mainTable="detailMainTables.wp"
                :visibleTop="true" />
            </div>
            <el-empty v-if="!detailLoading && detailData.wp.length === 0" description="无数据" />
          </el-tab-pane>
          <el-tab-pane label="当前工单预留的数量" name="resCur">
            <div :class="['detail-table-scroll', { 'scroll-all': detailMainTables.resCur.showAllColumn, 'show-prop': detailMainTables.resCur.showTablePropName }]">
              <SksTable
                width="100%"
                :showRefreshButton="false"
                :mainTable="detailMainTables.resCur"
                :visibleTop="true" />
            </div>
            <el-empty v-if="!detailLoading && detailData.resCur.length === 0" description="无数据" />
          </el-tab-pane>
          <el-tab-pane label="其它工单预留的数量" name="resOther">
            <div :class="['detail-table-scroll', { 'scroll-all': detailMainTables.resOther.showAllColumn, 'show-prop': detailMainTables.resOther.showTablePropName }]">
              <SksTable
                width="100%"
                :showRefreshButton="false"
                :mainTable="detailMainTables.resOther"
                :visibleTop="true" />
            </div>
            <el-empty v-if="!detailLoading && detailData.resOther.length === 0" description="无数据" />
          </el-tab-pane>
        </el-tabs>
      </el-dialog>
  </section>
</template>

<script>
import { getWpMaterialStockList, getWpMaterialStockDetail } from '@/api/wpmaterialstock'
import { sksPageMixin } from "sks-plugin-el-erp/lib/sks-page";

export default {
  name: 'WpMaterialStock',
  mixins: [sksPageMixin],
  data() {
    return {
      // 字段中文标题(硬编码, 来源: MAXATTRIBUTE/L_MAXATTRIBUTE ZH)
      wpMaterialTitles: {
        WPITEMID: '工作计划项目标识',
        ITEMNUM: '项目',
        ITEMSETID: '项目集',
        SITEID: '地点',
        ORGID: '组织',
        WONUM: '工单',
        LOCATION: '库房',
        DESCRIPTION: '描述',
        LINETYPE: '行类型',
        ITEMQTY: '数量',
        ORDERUNIT: '订购单位',
        UNITCOST: '单位成本',
        LINECOST: '行成本',
        DIRECTREQ: '直接发放',
        REQUESTNUM: '请求',
        REQUIREDATE: '要求的日期',
        REQUESTBY: '请求者',
        VENDOR: '供应商',
        MANUFACTURER: '制造商',
        MODELNUM: '型号',
        CATALOGCODE: '目录号',
        VENDORUNITCOST: '供应商单位成本',
        VENDORPACKCODE: '供应商包装代码',
        VENDORPACKQUANTITY: '供应商包装数量',
        VENDORWAREHOUSE: '供应商仓库',
        PR: 'PR',
        PRLINENUM: 'PR 行',
        ISSUETO: '发放目标',
        HASLD: '具有详细描述',
        WPM1: '门户记录ID号',
        WPM2: 'Wpm2',
        WPM3: 'Wpm3',
        WPM4: 'Wpm4',
        WPM5: 'Wpm4',
        WPM6: 'Wpm6',
        CONDITIONCODE: '条件代码',
        RESTYPE: '预留类型',
        STORELOCSITE: '库房地点',
        LANGCODE: '语言代码',
        RATE: '费率',
        RATEHASCHANGED: '比率已变更',
        HOURS: '工具时数',
        MKTPLCITEM: '市场项目',
        JOBITEMID: '作业项目标识',
        AMCREW: '工作人员',
        AMCREWTYPE: '班组类型',
        WPLABORREF: '班组引用',
        UNITCOSTHASCHANGED: '单位成本已变更',
        IBM_CRNUM: '客户订单号',
        IBM_NETWEIGHT: '净重',
        IBM_GROSSWEIGHT: '毛重',
        IBM_SIZE: '体积',
        IBM_CONTAINER: '箱号',
        IBM_TRANSLINE: '线路',
        IBM_CONTAINERTYPE: '箱规格',
        IBM_PLATECNT: '托盘数量',
        IBM_REMARK: '备注',
        IBM_RLLINEID: '客户订单行ID',
        IBM_UNITPRICE: '销售单价',
        IBM_TOTALPRICE: '销售总价',
        IBM_LINENUM: '行号',
        IBM_BOXNUMTEMP: '临时箱号',
        IBM_PARTTYPE: '零件类型',
        IBM_CURRENCYCODE: '币种',
        IBM_MASTERPRICE: '零件MASTER单价',
        IBM_PACKAGETYPE: '包装方式',
        FOBUNITCOST: 'FOB单价',
        IBM_BOXQTY: '托盘数',
        IBM_PALLETNO: '托盘号'
      },
      invReserveTitles: {
        REQUESTNUM: '请求',
        ITEMNUM: '项目',
        LOCATION: '库房',
        WONUM: '工单',
        ACTUALQTY: '实际数量',
        RESERVEDQTY: '已预留数量',
        INITFLAG: '初始化标志',
        DIRECTREQ: '直接请求',
        GLACCOUNT: 'GL 科目',
        REQUESTEDBY: '请求者',
        REQUESTEDDATE: '请求日期',
        REQUIREDDATE: '要求的日期',
        PONUM: '采购单',
        POLINENUM: 'PO 行',
        DELLOCATION: '交货地点',
        DESCRIPTION: '描述',
        POLINEID: '标识',
        MRNUM: '申请',
        ASSETNUM: '资产',
        MRLINENUM: '申请行',
        SOURCESYSID: '源系统标识',
        OWNERSYSID: '所有者系统标识',
        EXTERNALREFID: '外部引用标识',
        ISSUETO: '发放目标',
        SENDERSYSID: '发送者系统标识',
        FINCNTRLID: '财务控制标识',
        ORGID: '组织',
        SITEID: '地点',
        ITEMSETID: '项目集',
        CONDITIONCODE: '条件代码',
        INVRESERVEID: '发票保存标识',
        STORELOCSITEID: '库房地点',
        LANGCODE: '语言代码',
        HASLD: '具有详细描述',
        POREVISIONNUM: 'PO 修订',
        PENDINGQTY: '待处理数量',
        SHIPPEDQTY: '已装运数量',
        STAGEDQTY: '已暂存数量',
        OPLOCATION: '位置',
        TOSTORELOC: '目标库房',
        RESTYPE: '预留类型',
        HARDBACKORDER: '硬性延期交货',
        WOGROUP: '工单',
        BINNUM: '缺省货柜',
        PICKEDQTY: '选取的数量'
      },
      // 各标签页默认显示字段(其他字段通过工具栏"显示所有列"展开)
      detailDefaultProps: {
        wp: ['WPITEMID', 'ITEMQTY', 'ORDERUNIT', 'UNITCOST', 'LINECOST'],
        resCur: ['REQUESTNUM', 'RESTYPE', 'RESERVEDQTY'],
        resOther: ['REQUESTNUM', 'RESTYPE', 'RESERVEDQTY']
      },
      // 详情三个标签页的 SksTable mainTable(与主表一致支持"显示所有列/显示属性名")
      // 注意: 初始必须写全字段, showTable/showSearchButton 等缺省时 SksTable 行为异常(搜索按钮默认显示、表格不渲染)
      detailMainTables: this.createDetailMainTables(),
      loading: false,
      hasSearched: false,
      total: 0,
      formData: {
        wonum: '',
        itemnum: ''
      },
      detailDialogVisible: false,
      detailRow: {},
      detailTab: 'wp',
      detailLoading: false,
      detailData: {
        wp: [],
        resCur: [],
        resOther: []
      }
    }
  },
  methods: {
    initMainTableParam() {
      return {
        ownerName: 'wpmaterialstock',
        uniqueId: 'wpmaterialstock-list',
        sksAppName: 'page-wpmaterialstock',
        tableColumnListEnable: true,
        showPagination: true,
        showTable: true,
        showAllColumnButton: true,
        showTablePropName: false,
        serverPagination: true,
        total: 0,
        queryParams: {
          pageNum: 1,
          pageSize: 20
        },
        tableColumnList:
          this.sksUtils.newTableColumnList([
            { prop: 'SITEID', label: '站点', minWidth: 100 },
            { prop: 'WONUM', label: '工单号', minWidth: 160 },
            { prop: 'ITEMNUM', label: '物料号', minWidth: 150 },
            { prop: 'LOCATION', label: '位置', minWidth: 110 },
            { prop: 'DESCRIPTION', label: '物料名称', minWidth: 180 },
            { prop: 'ITEMQTY', label: '需求数量', minWidth: 110, align: 'right' },
            { prop: 'CURBAL', label: '当前库存余量', minWidth: 130, align: 'right' },
            { prop: 'RESERVEDQTY', label: '预约数量', minWidth: 110, align: 'right' },
            { prop: 'THISRESERVEDQTY', label: '当前工单预留数量', minWidth: 140, align: 'right' },
            { prop: 'V_STATUS', label: '状态', width: 100, align: 'center', htmlType: 'none', fixed: 'right' }
          ]),
        queryParamsColumnListEnable: false,
        queryParamsColumnList: []
      }
    },
    handleQuery() {
      this.hasSearched = true
      this.mainTable.queryParams.pageNum = 1
      this.fetchList()
    },
    fetchList() {
      this.loading = true
      const params = {
        wonum: this.formData.wonum,
        itemnum: this.formData.itemnum,
        pageNum: this.mainTable.queryParams.pageNum,
        pageSize: this.mainTable.queryParams.pageSize
      }
      getWpMaterialStockList(params)
        .then(res => {
          if (res.code === 200 && res.data) {
            this.mainTable.list = res.data.rows || []
            this.mainTable.total = res.data.total || 0
            this.total = res.data.total || 0
          } else {
            this.mainTable.list = []
            this.mainTable.total = 0
            this.total = 0
            this.$message.error(res.message || '查询失败')
          }
        })
        .catch(err => {
          this.$message.error('请求失败: ' + (err.message || String(err)))
        })
        .finally(() => {
          this.loading = false
        })
    },
    handlePageChange(page, limit) {
      this.mainTable.queryParams.pageNum = page
      this.mainTable.queryParams.pageSize = limit
      this.fetchList()
    },
    // ==== 行点击详情: 显示三类明细数据 ====
    handleRowClick(row) {
      this.detailRow = row || {}
      this.detailTab = 'wp'
      this.detailDialogVisible = true
    },
    // 详情标签页 SksTable 基础配置(与主表一致: 显示所有列/显示属性名按钮)
    // 注意: showSearchButton/showTable 必须显式设置, SksTable 内部默认为 true, 缺省会显示搜索按钮且表格不渲染
    detailMainTableTemplate() {
      return {
        uniqueId: '',
        ownerName: 'wpmaterialstock-detail',
        sksAppName: 'page-wpmaterialstock',
        tableColumnListEnable: true,
        tableColumnList: [],
        showPagination: false,
        showTable: true,
        showAllColumnButton: true,
        showAllColumn: false,
        showTablePropName: false,
        showSearchButton: false,
        showSearch: true,
        queryParamsColumnListEnable: false,
        queryParamsColumnList: [],
        list: [],
        total: 0,
        loading: false
      }
    },
    // 初始构建三个标签页的 mainTable(字段必须与 detailMainTableTemplate 一致)
    createDetailMainTables() {
      return {
        wp: this.detailMainTableTemplate(),
        resCur: this.detailMainTableTemplate(),
        resOther: this.detailMainTableTemplate()
      }
    },
    // 根据实际返回字段构建详情列(默认字段 visible=true, 其余 false, 工具栏"显示所有列"后可展开)
    // titles: 字段中文标题映射; defaultProps: 默认显示的字段
    buildDetailTable(group, rows, titles, defaultProps) {
      const mt = this.detailMainTables[group]
      Object.assign(mt, this.detailMainTableTemplate(), {
        uniqueId: 'wpmaterialstock-detail-' + group,
        list: rows || [],
        tableColumnList: rows && rows.length
          ? this.sksUtils.newTableColumnList(
              Object.keys(rows[0]).map(key => ({
                prop: key,
                label: titles[key] || key,
                minWidth: key === 'DESCRIPTION' || key === 'IBM_REMARK' ? 180 : 120,
                visible: defaultProps.indexOf(key) !== -1
              }))
            )
          : []
      })
    },
    loadDetailData() {
      const r = this.detailRow
      if (!r.SITEID || !r.LOCATION || !r.ITEMNUM || !r.WONUM) return
      this.detailLoading = true
      this.detailMainTables.wp.loading = true
      this.detailMainTables.resCur.loading = true
      this.detailMainTables.resOther.loading = true
      getWpMaterialStockDetail({
        siteid: r.SITEID,
        location: r.LOCATION,
        itemnum: r.ITEMNUM,
        wonum: r.WONUM
      })
        .then(res => {
          if (res.code === 200 && res.data) {
            this.detailData.wp = res.data.wp || []
            this.detailData.resCur = res.data.resCur || []
            this.detailData.resOther = res.data.resOther || []
            this.buildDetailTable('wp', this.detailData.wp, this.wpMaterialTitles, this.detailDefaultProps.wp)
            this.buildDetailTable('resCur', this.detailData.resCur, this.invReserveTitles, this.detailDefaultProps.resCur)
            this.buildDetailTable('resOther', this.detailData.resOther, this.invReserveTitles, this.detailDefaultProps.resOther)
          } else {
            this.detailData = { wp: [], resCur: [], resOther: [] }
            this.$message.error(res.message || '查询详情失败')
          }
        })
        .catch(err => {
          this.detailData = { wp: [], resCur: [], resOther: [] }
          this.$message.error('请求详情失败: ' + (err.message || String(err)))
        })
        .finally(() => {
          this.detailLoading = false
          this.detailMainTables.wp.loading = false
          this.detailMainTables.resCur.loading = false
          this.detailMainTables.resOther.loading = false
        })
    },
    formatNum(v) {
      if (v === null || v === undefined) return '-'
      return Number(v).toLocaleString()
    },
    resetForm() {
      this.formData = {
        wonum: '',
        itemnum: ''
      }
      this.hasSearched = false
      this.mainTable.list = []
      this.mainTable.total = 0
      this.total = 0
      this.mainTable.currentPage = 1
    }
  },
  mounted() {
    if (this.mainTable) {
      this.mainTable.onPageChange = (page, limit) => {
        this.handlePageChange(page, limit)
      }
    }
  }
}
</script>

<style lang="scss" scoped>
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
.result-panel {
  margin-top: 8px;
}
</style>
<style lang="scss">
/* 详情弹窗: 本体及 body 禁止横向滚动, 表格内部滚动条不受影响 */
.detail-dialog,
.detail-dialog .el-dialog__body {
  overflow-x: hidden;
}
/* 恢复 el-table 标准布局(MaxObjectDetail*.vue 的全局样式把 table 改成 display:block 会撑宽弹窗) */
.detail-dialog .el-table .el-table__header-wrapper table,
.detail-dialog .el-table .el-table__body-wrapper table {
  display: table !important;
  overflow: visible !important;
  width: 100%;
}
/* 表格留 2% 余量, 避免贴边/内边距导致出现横向滚动条 */
.detail-dialog .el-table {
  width: 98%;
}
/* 未点"显示所有列"且未点"显示属性名"时隐藏弹窗内表格横向滚动条(含表头/表体/容器, 空数据也生效), 点击后(scroll-all/show-prop)恢复正常 */
.detail-table-scroll:not(.scroll-all):not(.show-prop) .el-table__body-wrapper,
.detail-table-scroll:not(.scroll-all):not(.show-prop) .el-table__header-wrapper,
.detail-table-scroll:not(.scroll-all):not(.show-prop) {
  overflow-x: hidden !important;
}
/* tab 内容区同样禁止横向溢出 */
.detail-dialog .el-tabs__content,
.detail-dialog .el-tab-pane {
  overflow-x: hidden !important;
}
</style>