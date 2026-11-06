<!-- 索引列表子组件 -->
<template>
    <div>
        <SksTable width="100%" :showTableColumnButton="true" :showRefreshButton="false" :mainTable="mainTable" :visibleTop="true" :highlight-current-row="false">
            <template slot="default">
            </template>
            <template slot="none-_COLUMNS" slot-scope="{row}">
                <el-tag v-for="(c, i) in row._COLUMNS" :key="i" size="small" style="margin-right: 4px;">{{ c }}</el-tag>
                <span v-if="!row._COLUMNS || row._COLUMNS.length === 0">-</span>
            </template>
        </SksTable>
    </div>
</template>

<script>
import {sksPageMixin} from "sks-plugin-el-erp/lib/sks-page";
export default {
    name: "MaxObjectDetailIdx",
    mixins: [
        sksPageMixin,
    ],
    props: {
        data: {
            type: Array,
            default: () => []
        }
    },
    watch: {
        data: {
            immediate: true,
            handler(val) {
                this.$nextTick(() => {
                    this.setTableData(val)
                })
            }
        }
    },
    methods: {
        initMainTableParam() {
            return {
                ownerName: `maxsysindexes`,
                uniqueId: 'index',
                sksAppName: 'page51',
                tableColumnListEnable: true,
                showPagination: false,
                showTable: true,
                showAllColumnButton: true,
                showTablePropName: false,
                tableColumnList:
                    this.sksUtils.newTableColumnList([
                        { prop: 'NAME', label: '索引名', minWidth: 150 },
                        { prop: 'TBNAME', label: '表名', minWidth: 120, visible: false },
                        { prop: 'UNIQUERULE', label: '唯一规则', width: 90 },
                        { prop: 'CHANGED', label: '已更改', width: 80 },
                        { prop: 'CLUSTERRULE', label: '聚簇规则', width: 90 },
                        { prop: 'STORAGEPARTITION', label: '存储分区', width: 100, visible: false },
                        { prop: 'REQUIRED', label: '必需', width: 70 },
                        { prop: 'TEXTSEARCH', label: '文本搜索', width: 90 },
                        { prop: 'MAXSYSINDEXESID', label: '索引 ID', width: 100, visible: false },
                        { prop: 'ROWSTAMP', label: '行戳', width: 100, visible: false },
                        { prop: '_COLUMNS', label: '索引列', minWidth: 250, htmlType: 'none' },
                    ]),
                queryParamsColumnListEnable: false,
                queryParamsColumnList: [],
            }
        },
        setTableData(val) {
            if (!val || !this.mainTable) return
            this.mainTable.list = val
            this.mainTable.loading = false
        }
    },
};
</script>

<style>
/*//高亮点击的行*/
.el-table__body tr.current-row>td {
    background-color: #DCDCDC !important;
    color: #000000;
}
/*//鼠标滑过时高亮行*/
.el-table__body tr:hover>td {
    background-color: #d7dcdc !important;
}
.el-table__header-wrapper>table {
    border-collapse: collapse;
    margin: 0px;
    display: block;
    overflow-x: auto;
}
.el-table__body-wrapper>table {
    border-collapse: collapse;
    margin: 0px;
    display: block;
    overflow-x: auto;
}
</style>