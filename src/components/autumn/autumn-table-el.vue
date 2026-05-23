<template>
    <div>

        <el-row v-if="!isNullOrUndefined(mainTable.tabsFilterOptionList)" >
            <el-col :span="24">
                <el-tabs @tab-click="handleChangeTabsFilterValueChange()" v-model="mainTable.tabsFilterValue">
                    <el-tab-pane v-for="(item, index) in mainTable.tabsFilterOptionList"
                                 :disabled="mainTable.loading"
                                 :label="item.dictLabel"
                                 :name="''+item.dictValue">
                    </el-tab-pane>
                </el-tabs>
            </el-col>
        </el-row>

<!--        :gutter="10"-->
        <el-row v-if="visibleTop" :gutter="10" class="mb8 ml10">
            <slot name="top"></slot>
            <right-toolbar
                :showTable.sync="mainTable.showTable" :showAllColumnButton="mainTable.showAllColumnButton" :showAllColumn.sync="mainTable.showAllColumn"
                :showSearchButton.sync="mainTable.showSearchButton"
                :showSearch.sync="mainTable.showSearch" @queryTable="refreshTable()"></right-toolbar>
        </el-row>
        <el-table v-if="mainTable.showTable" v-loading="mainTable.loading" :data="mainTable.list"   @selection-change="handleSelectionChange"
                  :highlight-current-row="highlightCurrentRow"
                  :row-style="rowStyle"
                  @row-click="rowClickMainTable"
                  :style="tableStyle"
        >
            <slot name="default"></slot>
            </el-table>
            <pagination
                v-if="pagination"
                v-show="mainTable.total>0"
                :total="mainTable.total"
                :page.sync="mainTable.queryParams.pageNum"
                :limit.sync="mainTable.queryParams.pageSize"
                @pagination="refreshTable()"
            />
    </div>
</template>

<script>
import { rowStyleElTable } from '../../utils/wangbao-table'
import { findRowInListFromAttrName } from '../../utils/wangbao'

export default {
    name: 'autumnTableEl',
    props:{
        //显示顶部工具条
        visibleTop: {
            type: Boolean,
            default: true,
        },
        tableStyle: [Object,String],
        mainTable:[Object],
        //是否显示页码
        pagination:{
            type: Boolean,
            default: true,
        },
        highlightCurrentRow: {
            type: Boolean,
            default: true
        },
        rowStyle:{
            type:  [Object, Function],
            default: ()=>{
                return rowStyleElTable
            }
        },
        tabsFilterValueChange:[Function],
    },
    methods:{

        handleChangeTabsFilterValueChange(){
            let tabsFilterValue = this.mainTable.tabsFilterValue
            if(this.tabsFilterValueChange){
                this.tabsFilterValueChange(tabsFilterValue)
                return
            }

            let row = findRowInListFromAttrName(this.mainTable.tabsFilterOptionList,'dictValue',tabsFilterValue)
            this.mergeFromObject(this.mainTable.queryParams,this.mainTable.tabsQueryParamsDefaultSetting,row?.queryParams)
            this.refreshTable()

        },
        // 多选框选中数据
        handleSelectionChange(selection) {
            console.debug("handleSelectionChange")
            this.mainTable.ids = selection.map(item => item[this.mainTable.uniqueId]);
            this.mainTable.single = selection.length != 1;
            this.mainTable.multiple = !selection.length;
            this.$emit('selectionChangeAfter',selection)
        },

        rowClickMainTable(row){
            this.mainTable.currentRow=row
        },
        refreshTable(){
         this.$emit('refresh')
        },
    }
}
</script>

<style scoped>

</style>
