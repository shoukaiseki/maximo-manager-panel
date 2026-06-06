<!-- 属性列表子组件 -->
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
    name: "MaxObjectDetailColumn",
    mixins: [
        sksPageMixin,
    ],
    props: {
        data: {
            type: Array,
            default: () => []
        }
    },
    data() {
        return {
        };
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
                ownerName: `maxattribute`,
                uniqueId: 'attribute',
                sksAppName: 'page51',
                tableColumnListEnable: true,
                showPagination: false,
                showTable: true,
                showAllColumnButton: true,
                showTablePropName: false,
                tableColumnList:
                    this.sksUtils.newTableColumnList([
                        { prop: 'OBJECTNAME', label: '对象名', minWidth: 130 ,visible:false},
                        { prop: 'ATTRIBUTENAME', label: '属性名', minWidth: 230 },
                        { prop: 'L_TITLE', label: '标题', minWidth: 120 },
                        { prop: 'TITLE', label: '英文标题', minWidth: 120 },
                        { prop: 'ALIAS', label: '别名', minWidth: 100 ,visible:false},
                        { prop: 'MAXTYPE', label: '数据类型', width: 80 },
                        { prop: 'LENGTH', label: '长度', width: 60 },
                        { prop: 'SCALE', label: '小数位数', width: 80 },
                        { prop: 'PERSISTENT', label: '持久性', width: 80 },
                        { prop: 'ATTRIBUTENO', label: '属性号', width: 80 },
                        { prop: 'CANAUTONUM', label: '自动编号', width: 80 },
                        { prop: 'CLASSNAME', label: 'Java 类', minWidth: 200,visible:false },
                        { prop: 'COLUMNNAME', label: '列名', minWidth: 130 ,visible:false},
                        { prop: 'EAUDITENABLED', label: '审计启用', width: 80 },
                        { prop: 'ENTITYNAME', label: '实体', minWidth: 130 ,visible:false},
                        { prop: 'ESIGENABLED', label: '电子签名启用', width: 100 ,visible:false},
                        { prop: 'ISLDOWNER', label: '长文本所有者', width: 100 ,visible:false},
                        { prop: 'ISPOSITIVE', label: '正向', width: 60 },
                        { prop: 'MUSTBE', label: '必须', width: 60 ,visible:false},
                        { prop: 'REQUIRED', label: '必需', width: 60 },
                        { prop: 'PRIMARYKEYCOLSEQ', label: '主键列序列', width: 100 ,visible:false},
                        { prop: 'DOMAINID', label: '域', minWidth: 100 },
                        { prop: 'SAMEASATTRIBUTE', label: '等同属性', minWidth: 120 },
                        { prop: 'SAMEASOBJECT', label: '等同对象', minWidth: 120 },
                        { prop: 'USERDEFINED', label: '用户定义', width: 80 ,visible:false},
                        { prop: 'SEARCHTYPE', label: '搜索类型', width: 80 },
                        { prop: 'MLSUPPORTED', label: '多语言支持', width: 90 ,visible:false},
                        { prop: 'MLINUSE', label: '多语言使用中', width: 100 ,visible:false},
                        { prop: 'MAXATTRIBUTEID', label: '属性 ID', width: 100 ,visible:false},
                        { prop: 'RESTRICTED', label: '限制', width: 60 ,visible:false},
                        { prop: 'LOCALIZABLE', label: '可本地化', width: 80 ,visible:false},
                        { prop: 'ROWSTAMP', label: '行戳', width: 100 ,visible:false},
                        { prop: 'L_REMARKS', label: '备注', minWidth: 300 },
                        { prop: 'REMARKS', label: '英文备注', minWidth: 300 },
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
    //background: rgb(77, 195, 255, 0.5) !important;
    background-color: #DCDCDC !important;
    color: #000000;
}

/*//鼠标滑过时高亮行*/
.el-table__body tr:hover>td {
    //background: rgb(77, 195, 255, 0.2);
    background-color: #d7dcdc !important;
    //background: none;
}

.el-table__header-wrapper>table {
    border-collapse: collapse;
    /* margin: 1rem 0; */
    margin: 0px;
    display: block;
    overflow-x: auto;
}

.el-table__body-wrapper>table {
    border-collapse: collapse;
    /* margin: 1rem 0; */
    margin: 0px;
    display: block;
    overflow-x: auto;
}
</style>
