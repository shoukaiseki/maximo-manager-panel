<!-- 属性列表子组件 -->
<template>
    <div>
        <SksTable width="100%" :showTableColumnButton="true" :showRefreshButton="false" :mainTable="mainTable" :visibleTop="true" :highlight-current-row="true" @rowClickAfter="handleRowClick">
            <template slot="default">
            </template>
        </SksTable>

        <!-- 字段详情弹窗 -->
        <el-dialog :title="'字段详情 - ' + (currentRow ? currentRow.ATTRIBUTENAME : '')" :visible.sync="dialogVisible" width="1500px" :close-on-click-modal="false">
            <el-descriptions :column="3" border v-if="currentRow">
                <el-descriptions-item label="属性名">{{ currentRow.ATTRIBUTENAME }}</el-descriptions-item>
                <el-descriptions-item label="标题">{{ currentRow.L_TITLE }}</el-descriptions-item>
                <el-descriptions-item label="英文标题">{{ currentRow.TITLE }}</el-descriptions-item>
                <el-descriptions-item label="别名">{{ currentRow.ALIAS }}</el-descriptions-item>
                <el-descriptions-item label="数据类型">{{ currentRow.MAXTYPE }}</el-descriptions-item>
                <el-descriptions-item label="长度">{{ currentRow.LENGTH }}</el-descriptions-item>
                <el-descriptions-item label="小数位数">{{ currentRow.SCALE }}</el-descriptions-item>
                <el-descriptions-item label="持久性">{{ currentRow.PERSISTENT }}</el-descriptions-item>
                <el-descriptions-item label="自动编号">{{ currentRow.CANAUTONUM }}</el-descriptions-item>
                <el-descriptions-item label="Java 类" :span="2">{{ currentRow.CLASSNAME }}</el-descriptions-item>
                <el-descriptions-item label="列名">{{ currentRow.COLUMNNAME }}</el-descriptions-item>
                <el-descriptions-item label="属性号">{{ currentRow.ATTRIBUTENO }}</el-descriptions-item>
                <el-descriptions-item label="域">{{ currentRow.DOMAINID }}</el-descriptions-item>
                <el-descriptions-item label="默认值">{{ currentRow.DEFAULTVALUE }}</el-descriptions-item>
                <el-descriptions-item label="必需">{{ currentRow.REQUIRED }}</el-descriptions-item>
                <el-descriptions-item label="主键列序列">{{ currentRow.PRIMARYKEYCOLSEQ }}</el-descriptions-item>
                <el-descriptions-item label="搜索类型">{{ currentRow.SEARCHTYPE }}</el-descriptions-item>
                <el-descriptions-item label="长文本所有者">{{ currentRow.ISLDOWNER }}</el-descriptions-item>
                <el-descriptions-item label="必须">{{ currentRow.MUSTBE }}</el-descriptions-item>
                <el-descriptions-item label="正向">{{ currentRow.ISPOSITIVE }}</el-descriptions-item>
                <el-descriptions-item label="限制">{{ currentRow.RESTRICTED }}</el-descriptions-item>
                <el-descriptions-item label="可本地化">{{ currentRow.LOCALIZABLE }}</el-descriptions-item>
                <el-descriptions-item label="用户定义">{{ currentRow.USERDEFINED }}</el-descriptions-item>
                <el-descriptions-item label="多语言支持">{{ currentRow.MLSUPPORTED }}</el-descriptions-item>
                <el-descriptions-item label="多语言使用中">{{ currentRow.MLINUSE }}</el-descriptions-item>
                <el-descriptions-item label="审计启用">{{ currentRow.EAUDITENABLED }}</el-descriptions-item>
                <el-descriptions-item label="电子签名启用">{{ currentRow.ESIGENABLED }}</el-descriptions-item>
                <el-descriptions-item label="实体">{{ currentRow.ENTITYNAME }}</el-descriptions-item>
                <el-descriptions-item label="对象名">{{ currentRow.OBJECTNAME }}</el-descriptions-item>
                <el-descriptions-item label="等同属性">{{ currentRow.SAMEASATTRIBUTE }}</el-descriptions-item>
                <el-descriptions-item label="等同对象">{{ currentRow.SAMEASOBJECT }}</el-descriptions-item>
                <el-descriptions-item label="自动键名">{{ currentRow.AUTOKEYNAME }}</el-descriptions-item>
                <el-descriptions-item label="属性 ID">{{ currentRow.MAXATTRIBUTEID }}</el-descriptions-item>
                <el-descriptions-item label="行戳">{{ currentRow.ROWSTAMP }}</el-descriptions-item>
                <el-descriptions-item label="备注" :span="2">
                    <div style="white-space: pre-wrap; word-break: break-word;">{{ currentRow.L_REMARKS }}</div>
                </el-descriptions-item>
                <el-descriptions-item label="英文备注" :span="2">
                    <div style="white-space: pre-wrap; word-break: break-word;">{{ currentRow.REMARKS }}</div>
                </el-descriptions-item>
            </el-descriptions>

            <!-- 域信息 -->
            <el-divider v-if="currentDomain" content-position="left">域信息</el-divider>
            <el-card v-if="currentDomain" shadow="never" class="domain-info-card">
                <div slot="header" class="domain-card-header">
                    <div class="domain-title">
                        <el-tag size="small" type="primary" effect="plain">{{ currentDomain.DOMAINID }}</el-tag>
                        <el-tag size="mini" :type="domainTypeTag(currentDomain.DOMAINTYPE)" style="margin-left: 6px;">
                            {{ currentDomain.DOMAINTYPE }}
                        </el-tag>
                    </div>
                    <div class="domain-meta">
                        <span v-if="currentDomain.L_DESCRIPTION" class="meta-item">{{ currentDomain.L_DESCRIPTION }}</span>
                        <span v-if="currentDomain.DESCRIPTION" class="meta-item desc-en">{{ currentDomain.DESCRIPTION }}</span>
                    </div>
                    <div class="domain-type-info">
                        <span class="type-label">{{ currentDomain.MAXTYPE || '-' }}</span>
                        <template v-if="currentDomain.LENGTH">
                            <span class="type-sep">|</span>
                            <span class="type-label">长度 {{ currentDomain.LENGTH }}</span>
                        </template>
                        <template v-if="currentDomain.SCALE">
                            <span class="type-sep">|</span>
                            <span class="type-label">小数 {{ currentDomain.SCALE }}</span>
                        </template>
                        <span class="type-sep">|</span>
                        <span class="type-label">共 {{ currentDomain._VALUES_COUNT || 0 }} 个值</span>
                    </div>
                </div>
                <div v-if="currentDomain._VALUES && currentDomain._VALUES.length > 0">
                    <el-table
                        :data="currentDomain._VALUES"
                        border
                        stripe
                        size="small"
                        style="width: 100%; max-height: 300px; overflow-y: auto;"
                        empty-text="暂无域值">
                        <el-table-column type="index" label="#" width="45" align="center"></el-table-column>
                        <el-table-column v-if="currentDomain.DOMAINTYPE === 'SYNONYM'" prop="MAXVALUE" label="内部值" min-width="110" show-overflow-tooltip>
                            <template slot-scope="{row}">
                                <el-tag size="small" type="warning">{{ row.MAXVALUE }}</el-tag>
                            </template>
                        </el-table-column>
                        <el-table-column prop="VALUE" label="域值" min-width="130" show-overflow-tooltip>
                            <template slot-scope="{row}">
                                <el-tag size="small" type="info">{{ row.VALUE }}</el-tag>
                            </template>
                        </el-table-column>
                        <el-table-column prop="L_DESCRIPTION" label="中文描述" min-width="140" show-overflow-tooltip>
                            <template slot-scope="{row}">
                                <span>{{ row.L_DESCRIPTION || '-' }}</span>
                            </template>
                        </el-table-column>
                        <el-table-column prop="DESCRIPTION" label="英文描述" min-width="140" show-overflow-tooltip>
                            <template slot-scope="{row}">
                                <span>{{ row.DESCRIPTION || '-' }}</span>
                            </template>
                        </el-table-column>
                        <el-table-column v-if="currentDomain.DOMAINTYPE === 'ALN' || currentDomain.DOMAINTYPE === 'SYNONYM'" prop="SITEID" label="站点" width="75" align="center">
                            <template slot-scope="{row}">
                                <span>{{ row.SITEID || '-' }}</span>
                            </template>
                        </el-table-column>
                        <el-table-column v-if="currentDomain.DOMAINTYPE === 'ALN' || currentDomain.DOMAINTYPE === 'SYNONYM'" prop="ORGID" label="组织" width="75" align="center">
                            <template slot-scope="{row}">
                                <span>{{ row.ORGID || '-' }}</span>
                            </template>
                        </el-table-column>
                        <el-table-column v-if="currentDomain.DOMAINTYPE === 'SYNONYM'" prop="DEFAULTS" label="默认" width="65" align="center">
                            <template slot-scope="{row}">
                                <el-tag size="mini" :type="Number(row.DEFAULTS) === 1 ? 'success' : 'info'">
                                    {{ Number(row.DEFAULTS) === 1 ? '是' : '否' }}
                                </el-tag>
                            </template>
                        </el-table-column>
                    </el-table>
                </div>
                <div v-else style="text-align: center; padding: 16px 0; color: #999;">
                    <span>暂无域值</span>
                </div>
            </el-card>

            <!-- 关联关系 -->
            <el-divider v-if="relatedRelationships.length > 0" content-position="left">关联关系 ({{ relatedRelationships.length }})</el-divider>
            <el-table
                v-if="relatedRelationships.length > 0"
                :data="relatedRelationships"
                border
                stripe
                size="small"
                style="width: 100%;"
                empty-text="暂无关联关系">
                <el-table-column type="index" label="#" width="45" align="center"></el-table-column>
                <el-table-column prop="NAME" label="关系名" min-width="150" show-overflow-tooltip></el-table-column>
                <el-table-column prop="CHILD" label="子对象" min-width="120" show-overflow-tooltip></el-table-column>
                <el-table-column prop="WHERECLAUSE" label="条件" min-width="300" show-overflow-tooltip></el-table-column>
                <el-table-column prop="REMARKS" label="备注" min-width="200" show-overflow-tooltip></el-table-column>
                <el-table-column prop="CARDINALITY" label="基数" width="80" align="center"></el-table-column>
                <el-table-column prop="ISDEFAULT" label="默认" width="70" align="center"></el-table-column>
            </el-table>

            <span slot="footer" class="dialog-footer">
                <el-button @click="dialogVisible = false">关 闭</el-button>
            </span>
        </el-dialog>
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
        },
        domains: {
            type: Array,
            default: () => []
        },
        relationships: {
            type: Array,
            default: () => []
        }
    },
    computed: {
        currentDomain() {
            if (!this.currentRow || !this.currentRow.DOMAINID) return null
            return this.domains.find(d => d.DOMAINID === this.currentRow.DOMAINID) || null
        },
        relatedRelationships() {
            if (!this.currentRow || !this.currentRow.ATTRIBUTENAME) return []
            const attrName = this.currentRow.ATTRIBUTENAME
            return this.relationships.filter(r => {
                const where = r.WHERECLAUSE || ''
                const pattern = new RegExp(`:${attrName}\\b`, 'i')
                return pattern.test(where)
            })
        }
    },
    data() {
        return {
            dialogVisible: false,
            currentRow: null
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
                        { prop: 'ATTRIBUTENO', label: '属性号', width: 80 ,visible:false},
                        { prop: 'CANAUTONUM', label: '自动编号', width: 80 },
                        { prop: 'CLASSNAME', label: 'Java 类', minWidth: 200,visible:false },
                        { prop: 'COLUMNNAME', label: '列名', minWidth: 130 ,visible:false},
                        { prop: 'EAUDITENABLED', label: '审计启用', width: 80 ,visible:false},
                        { prop: 'ENTITYNAME', label: '实体', minWidth: 130 ,visible:false},
                        { prop: 'ESIGENABLED', label: '电子签名启用', width: 100 ,visible:false},
                        { prop: 'ISLDOWNER', label: '长文本所有者', width: 100 ,visible:false},
                        { prop: 'ISPOSITIVE', label: '正向', width: 60 ,visible:false},
                        { prop: 'MUSTBE', label: '必须', width: 60 ,visible:false},
                        { prop: 'REQUIRED', label: '必需', width: 60 },
                        { prop: 'PRIMARYKEYCOLSEQ', label: '主键列序列', width: 100 ,visible:false},
                        { prop: 'DOMAINID', label: '域', minWidth: 100 },
                        { prop: 'DEFAULTVALUE', label: '默认值', minWidth: 120 },
                        { prop: 'AUTOKEYNAME', label: '自动键名', minWidth: 120 },
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
                        { prop: 'L_REMARKS', label: '备注', minWidth: 500 ,className:'wrap-cell'},
                        { prop: 'REMARKS', label: '英文备注', minWidth: 500 ,className:'wrap-cell'},
                    ]),
                queryParamsColumnListEnable: false,
                queryParamsColumnList: [],
            }
        },
        setTableData(val) {
            if (!val || !this.mainTable) return
            this.mainTable.list = val
            this.mainTable.loading = false
        },
        handleRowClick(row) {
            this.currentRow = row
            this.dialogVisible = true
        },
        domainTypeTag(type) {
            if (!type) return 'info'
            const map = { 'ALN': 'success', 'NUMERIC': 'warning', 'SYNONYM': 'danger', 'CROSSOVER': '', 'TABLE': 'info' }
            return map[type.toUpperCase()] || 'info'
        }
    },
};
</script>

<style lang="scss">
/*//可点击的行*/
.el-table__body tr {
    cursor: pointer;
}

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
.wrap-cell {
    white-space: normal;
    word-break: break-word;
}

.wrap-cell .cell {
    white-space: normal;
    word-break: break-word;
}

.el-table .cell.el-tooltip {
    white-space: normal !important;
    word-break: break-word !important;
}

.domain-info-card {
    margin-bottom: 16px;
}

.domain-info-card >>> .el-card__header {
    padding: 10px 16px;
    border-bottom: 1px solid #ebeef5;
    background: #fafafa;
}

.domain-card-header {
    display: flex;
    flex-direction: row;
    align-items: center;
    flex-wrap: wrap;
    gap: 8px 16px;
}

.domain-title {
    display: flex;
    align-items: center;
}

.domain-meta {
    display: flex;
    flex-wrap: wrap;
    gap: 4px 12px;
    font-size: 13px;
    color: #606266;
}

.domain-meta .meta-item.desc-en {
    color: #909399;
}

.domain-type-info {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 2px;
    font-size: 12px;
    color: #909399;
}

.type-sep {
    margin: 0 4px;
    color: #dcdfe6;
}
</style>
