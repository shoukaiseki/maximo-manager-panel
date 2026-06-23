<!-- 关系列表子组件 -->
<template>
    <div>
        <SksTable width="100%" :showTableColumnButton="true" :showRefreshButton="false" :mainTable="mainTable" :visibleTop="true" :highlight-current-row="false">
            <template slot="default">
            </template>
        </SksTable>
    </div>
</template>

<script>
import {sksPageMixin} from "sks-plugin-el-erp/lib/sks-page";
export default {
    name: "MaxObjectDetailRel",
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
                ownerName: `maxrelationship`,
                uniqueId: 'relationship',
                sksAppName: 'page51',
                tableColumnListEnable: true,
                showPagination: false,
                showTable: true,
                showAllColumnButton: true,
                showTablePropName: false,
                tableColumnList:
                    this.sksUtils.newTableColumnList([
                        { prop: 'NAME', label: '关系名', minWidth: 150 },
                        { prop: 'PARENT', label: '父对象', minWidth: 120, visible: false },
                        { prop: 'CHILD', label: '子对象', minWidth: 120 },
                        { prop: 'WHERECLAUSE', label: '条件', minWidth: 200 },
                        { prop: 'REMARKS', label: '备注', minWidth: 400 ,className:'wrap-cell'},
                        { prop: 'CARDINALITY', label: '基数', width: 80 },
                        { prop: 'DBJOINREQUIRED', label: '需要 DB 连接', width: 110 },
                        { prop: 'ISDEFAULT', label: '默认', width: 70 },
                        { prop: 'MAXRELATIONSHIPID', label: '关系 ID', width: 100, visible: false },
                        { prop: 'ROWSTAMP', label: '行戳', width: 100, visible: false },
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