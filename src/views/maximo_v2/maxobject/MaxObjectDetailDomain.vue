<template>
    <div>
        <el-row :gutter="16">
            <el-col :span="24" v-for="(domain, idx) in data" :key="idx" style="margin-bottom: 16px;">
                <el-card shadow="hover" class="domain-card">
                    <div slot="header" class="domain-card-header">
                        <div class="domain-title">
                            <el-tag size="small" type="primary" effect="plain">{{ domain.DOMAINID }}</el-tag>
                            <el-tag size="mini" :type="domainTypeTag(domain.DOMAINTYPE)" style="margin-left: 6px;">
                                {{ domain.DOMAINTYPE }}
                            </el-tag>
                        </div>
                        <div class="domain-meta">
                            <span v-if="domain.L_DESCRIPTION" class="meta-item">{{ domain.L_DESCRIPTION }}</span>
                            <span v-if="domain.DESCRIPTION" class="meta-item desc-en">{{ domain.DESCRIPTION }}</span>
                        </div>
                        <div class="domain-type-info">
                            <span class="type-label">{{ domain.MAXTYPE || '-' }}</span>
                            <template v-if="domain.LENGTH">
                                <span class="type-sep">|</span>
                                <span class="type-label">长度 {{ domain.LENGTH }}</span>
                            </template>
                            <template v-if="domain.SCALE">
                                <span class="type-sep">|</span>
                                <span class="type-label">小数 {{ domain.SCALE }}</span>
                            </template>
                            <span class="type-sep">|</span>
                            <span class="type-label">共 {{ domain._VALUES_COUNT || 0 }} 个值</span>
                        </div>
                    </div>
                    <div v-if="domain._VALUES && domain._VALUES.length > 0">
                        <el-table
                            :data="domain._VALUES"
                            border
                            stripe
                            size="small"
                            style="width: 100%; min-height: 200px;"
                            empty-text="暂无域值">
                            <el-table-column type="index" label="#" width="45" align="center"></el-table-column>
                            <!-- SYNONYM 特有：内部值 -->
                            <el-table-column v-if="domain.DOMAINTYPE === 'SYNONYM'" prop="MAXVALUE" label="内部值" min-width="110" show-overflow-tooltip>
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
                            <!-- ALN + SYNONYM 特有：站点/组织 -->
                            <el-table-column v-if="domain.DOMAINTYPE === 'ALN' || domain.DOMAINTYPE === 'SYNONYM'" prop="SITEID" label="站点" width="75" align="center">
                                <template slot-scope="{row}">
                                    <span>{{ row.SITEID || '-' }}</span>
                                </template>
                            </el-table-column>
                            <el-table-column v-if="domain.DOMAINTYPE === 'ALN' || domain.DOMAINTYPE === 'SYNONYM'" prop="ORGID" label="组织" width="75" align="center">
                                <template slot-scope="{row}">
                                    <span>{{ row.ORGID || '-' }}</span>
                                </template>
                            </el-table-column>
                            <!-- SYNONYM 特有：默认 -->
                            <el-table-column v-if="domain.DOMAINTYPE === 'SYNONYM'" prop="DEFAULTS" label="默认" width="65" align="center">
                                <template slot-scope="{row}">
                                    <el-tag size="mini" :type="Number(row.DEFAULTS) === 1 ? 'success' : 'info'">
                                        {{ Number(row.DEFAULTS) === 1 ? '是' : '否' }}
                                    </el-tag>
                                </template>
                            </el-table-column>
                        </el-table>
                    </div>
                    <div v-else style="text-align: center; padding: 24px 0; color: #999;">
                        <i class="el-icon-document" style="font-size: 28px;"></i>
                        <p style="margin-top: 6px; font-size: 13px;">暂无域值</p>
                    </div>
                </el-card>
            </el-col>
        </el-row>
    </div>
</template>

<script>
export default {
    name: "MaxObjectDetailDomain",
    props: {
        data: {
            type: Array,
            default: () => []
        }
    },
    methods: {
        domainTypeTag(type) {
            if (!type) return 'info'
            const map = { 'ALN': 'success', 'NUMERIC': 'warning', 'SYNONYM': 'danger', 'CROSSOVER': '', 'TABLE': 'info' }
            return map[type.toUpperCase()] || 'info'
        }
    },
};
</script>

<style scoped>
.domain-card >>> .el-card__header {
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