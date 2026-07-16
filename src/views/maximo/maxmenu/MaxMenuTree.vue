<template>
  <section class="menu-page">
    <el-card>
      <div class="page-header-row">
        <div>
          <h2>菜单管理</h2>
          <p class="page-summary">Maximo 菜单树形结构查看：模块 → 分组 → 应用/操作</p>
        </div>
        <div>
          <el-button type="primary" icon="el-icon-refresh" size="mini" :loading="loading" @click="loadFullTree">刷新</el-button>
          <el-button size="mini" @click="toggleExpandAll">{{ expandAll ? '全部折叠' : '全部展开' }}</el-button>
        </div>
      </div>

      <!-- 过滤条件 -->
      <el-form :inline="true" size="mini" class="filter-form">
        <el-form-item label="排序">
          <el-select v-model="sortBy" style="width:140px" @change="applySort">
            <el-option label="中文描述" value="zh" />
            <el-option label="英文描述" value="en" />
            <el-option label="模块标识" value="module" />
          </el-select>
        </el-form-item>
        <el-form-item label="搜索">
          <el-input v-model="filterText" placeholder="标识/标题/模块" clearable style="width:200px" @keyup.enter.native="doFilter" />
        </el-form-item>
        <el-form-item label="菜单类型">
          <el-select v-model="filterMenuType" clearable placeholder="全部" style="width:130px" @change="doFilter">
            <el-option label="MODULE" value="MODULE" />
            <el-option label="APPMENU" value="APPMENU" />
            <el-option label="APPTOOL" value="APPTOOL" />
            <el-option label="SEARCHMENU" value="SEARCHMENU" />
          </el-select>
        </el-form-item>
        <el-form-item label="元素类型">
          <el-select v-model="filterElementType" clearable placeholder="全部" style="width:130px" @change="doFilter">
            <el-option label="MODULE" value="MODULE" />
            <el-option label="HEADER" value="HEADER" />
            <el-option label="APP" value="APP" />
            <el-option label="OPTION" value="OPTION" />
            <el-option label="SEP" value="SEP" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="doFilter">搜索</el-button>
          <el-button icon="el-icon-refresh-left" @click="resetFilter">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="table-container" v-loading="loading">
        <el-table
          v-if="refreshTable"
          ref="menuTable"
          :data="tableData"
          row-key="nodeId"
          :tree-props="{ children: 'children' }"
          :default-expand-all="expandAll || isFiltering"
          border
          size="mini"
          highlight-current-row
          @row-click="handleRowClick"
          style="width: 100%"
        >
          <el-table-column label="标识" min-width="200">
            <template slot-scope="scope">
              <span class="row-label" :class="getRowClass(scope.row)">
                <i :class="getRowIcon(scope.row)" style="margin-right:4px"></i>
                {{ getRowLabel(scope.row) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="中文描述" min-width="180" show-overflow-tooltip>
            <template slot-scope="scope">
              <span style="color:#409eff">{{ scope.row._descZh || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="英文描述" min-width="180" show-overflow-tooltip>
            <template slot-scope="scope">
              <span style="color:#909399;font-style:italic">{{ scope.row._descEn || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="元素类型" width="100" align="center">
            <template slot-scope="scope">
              <el-tag v-if="scope.row.ELEMENTTYPE" size="mini" :type="elementTypeTag(scope.row.ELEMENTTYPE)">{{ scope.row.ELEMENTTYPE }}</el-tag>
              <el-tag v-else-if="scope.row._isModule" size="mini" type="primary">MODULE</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="位置" width="80" align="center">
            <template slot-scope="scope">
              <span v-if="getPos(scope.row) !== undefined" class="pos-badge">{{ getPos(scope.row) }}.{{ getSubPos(scope.row) || 0 }}</span>
            </template>
          </el-table-column>
          <el-table-column label="可见" width="60" align="center">
            <template slot-scope="scope">
              <el-tag v-if="getVisible(scope.row) !== undefined" size="mini" :type="getVisible(scope.row) === 1 ? 'success' : 'info'">
                {{ getVisible(scope.row) === 1 ? '是' : '否' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="固定" width="60" align="center">
            <template slot-scope="scope">
              <el-tag v-if="getPinned(scope.row) === 1" size="mini" type="warning">固定</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="模块/应用" width="120" show-overflow-tooltip>
            <template slot-scope="scope">
              {{ scope.row.MODULEAPP || scope.row.MODULE || '' }}
            </template>
          </el-table-column>
        </el-table>

        <el-empty v-if="!loading && tableData.length === 0" description="暂无菜单数据，请点击刷新" />
      </div>
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog title="菜单项详情" :visible.sync="detailVisible" width="90%" :close-on-click-modal="true">
      <div v-if="selectedNode">
        <el-tabs v-model="activeTab" type="card" @tab-click="handleTabClick">
          <el-tab-pane label="详细信息" name="detail">
            <!-- 非模块节点: 显示 MAXMENU 字段 -->
            <el-descriptions title="MAXMENU" :column="2" border size="small" v-if="!selectedNode._isModule">
              <el-descriptions-item v-for="f in menuFields" :key="f.attr" :label="f.title" :span="f.span || 1">
                <template v-if="f.maxtype === 'YORN'">
                  <el-tag size="mini" :type="selectedNode[f.attr] === 1 ? 'success' : 'info'">{{ selectedNode[f.attr] === 1 ? f.yes || '是' : f.no || '否' }}</el-tag>
                </template>
                <template v-else-if="f.attr === 'L_HEADERDESCRIPTION'">
                  <span style="color:#409eff">{{ selectedNode[f.attr] || '-' }}</span>
                </template>
                <template v-else-if="f.attr === 'HEADERDESCRIPTION'">
                  <span style="color:#909399;font-style:italic">{{ selectedNode[f.attr] || '-' }}</span>
                </template>
                <template v-else-if="f.domain">
                  <el-tag size="mini">{{ selectedNode[f.attr] || '-' }}</el-tag>
                </template>
                <template v-else>
                  {{ selectedNode[f.attr] != null ? selectedNode[f.attr] : '-' }}
                </template>
              </el-descriptions-item>
            </el-descriptions>
            <!-- 模块节点: 同时显示 MAXMODULES + MAXMENU(MODULE) -->
            <template v-if="selectedNode._isModule">
              <el-descriptions title="MAXMODULES" :column="2" border size="small">
                <el-descriptions-item v-for="f in moduleFields" :key="f.attr" :label="f.title" :span="f.span || 1">
                  <template v-if="f.maxtype === 'YORN'">
                    <el-tag size="mini" :type="selectedNode[f.attr] === 1 ? 'success' : 'info'">{{ selectedNode[f.attr] === 1 ? f.yes || '是' : f.no || '否' }}</el-tag>
                  </template>
                  <template v-else-if="f.attr === 'L_DESCRIPTION'">
                    <span style="color:#409eff">{{ selectedNode[f.attr] || '-' }}</span>
                  </template>
                  <template v-else-if="f.attr === 'DESCRIPTION'">
                    <span style="color:#909399;font-style:italic">{{ selectedNode[f.attr] || '-' }}</span>
                  </template>
                  <template v-else>
                    {{ selectedNode[f.attr] != null ? selectedNode[f.attr] : '-' }}
                  </template>
                </el-descriptions-item>
                <el-descriptions-item label="菜单项数量" :span="2">{{ selectedNode._menuCount || 0 }}</el-descriptions-item>
              </el-descriptions>
              <el-descriptions title="MAXMENU (MODULE)" :column="2" border size="small" style="margin-top: 12px">
                <el-descriptions-item v-for="f in moduleMenuFields" :key="f.attr" :label="f.title" :span="f.span || 1">
                  <template v-if="f.maxtype === 'YORN'">
                    <el-tag size="mini" :type="selectedNode[f.source] === 1 ? 'success' : 'info'">{{ selectedNode[f.source] === 1 ? f.yes || '是' : f.no || '否' }}</el-tag>
                  </template>
                  <template v-else-if="f.attr === 'L_HEADERDESCRIPTION'">
                    <span style="color:#409eff">{{ selectedNode[f.source] || '-' }}</span>
                  </template>
                  <template v-else-if="f.attr === 'HEADERDESCRIPTION'">
                    <span style="color:#909399;font-style:italic">{{ selectedNode[f.source] || '-' }}</span>
                  </template>
                  <template v-else>
                    {{ selectedNode[f.source] != null ? selectedNode[f.source] : '-' }}
                  </template>
                </el-descriptions-item>
              </el-descriptions>
            </template>
          </el-tab-pane>
          <el-tab-pane label="APPMENU" name="appmenu">
            <div class="menu-list-tab">
              <el-input v-model="menuFilterText" placeholder="搜索" clearable style="width:250px; margin-bottom: 10px" @keyup.enter.native="filterMenuList" />
              <el-table :data="filteredAppMenu" border size="small" v-loading="menuLoading" highlight-current-row @row-click="handleMenuRowClick">
                <el-table-column label="图标" width="60" align="center">
                  <template slot-scope="scope">
                    <img v-if="scope.row.IMAGE" :src="require('@/assets/mas/images/' + scope.row.IMAGE)" alt="" style="width:24px;height:24px;" />
                  </template>
                </el-table-column>
                <el-table-column prop="KEYVALUE" label="键值" min-width="150" show-overflow-tooltip />
                <el-table-column prop="DESCRIPTIONZH" label="中文描述" min-width="180" show-overflow-tooltip />
                <el-table-column prop="DESCRIPTIONEN" label="英文描述" min-width="180" show-overflow-tooltip />
                <el-table-column prop="MENUTYPE" label="菜单类型" width="100" align="center" />
                <el-table-column prop="POSITION" label="位置" width="80" align="center" />
                <el-table-column prop="SUBPOSITION" label="子位置" width="80" align="center" />
                <el-table-column prop="ELEMENTTYPE" label="元素类型" width="100" align="center" />
              </el-table>
              <el-empty v-if="!menuLoading && appMenuList.length === 0" description="暂无数据" />
            </div>
          </el-tab-pane>
          <el-tab-pane label="SEARCHMENU" name="searchmenu">
            <div class="menu-list-tab">
              <el-input v-model="menuFilterText" placeholder="搜索" clearable style="width:250px; margin-bottom: 10px" @keyup.enter.native="filterMenuList" />
              <el-table :data="filteredSearchMenu" border size="small" v-loading="menuLoading" highlight-current-row @row-click="handleMenuRowClick">
                <el-table-column label="图标" width="60" align="center">
                  <template slot-scope="scope">
                    <img v-if="scope.row.IMAGE" :src="require('@/assets/mas/images/' + scope.row.IMAGE)" alt="" style="width:24px;height:24px;" />
                  </template>
                </el-table-column>
                <el-table-column prop="DESCRIPTIONEN" label="英文描述" min-width="180" show-overflow-tooltip />
                <el-table-column prop="DESCRIPTIONZH" label="中文描述" min-width="180" show-overflow-tooltip />
                <el-table-column prop="MENUTYPE" label="菜单类型" width="100" align="center" />
                <el-table-column prop="POSITION" label="位置" width="80" align="center" />
                <el-table-column prop="SUBPOSITION" label="子位置" width="80" align="center" />
                <el-table-column prop="ELEMENTTYPE" label="元素类型" width="100" align="center" />
                <el-table-column prop="KEYVALUE" label="键值" min-width="150" show-overflow-tooltip />
              </el-table>
              <el-empty v-if="!menuLoading && searchMenuList.length === 0" description="暂无数据" />
            </div>
          </el-tab-pane>
          <el-tab-pane label="APPTOOL" name="apptool">
            <div class="menu-list-tab">
              <el-input v-model="menuFilterText" placeholder="搜索" clearable style="width:250px; margin-bottom: 10px" @keyup.enter.native="filterMenuList" />
              <el-table :data="filteredAppTool" border size="small" v-loading="menuLoading" highlight-current-row @row-click="handleMenuRowClick">
                <el-table-column label="图标" width="60" align="center">
                  <template slot-scope="scope">
                    <img v-if="scope.row.IMAGE" :src="require('@/assets/mas/images/' + scope.row.IMAGE)" alt="" style="width:24px;height:24px;" />
                  </template>
                </el-table-column>
                <el-table-column prop="DESCRIPTIONEN" label="英文描述" min-width="180" show-overflow-tooltip />
                <el-table-column prop="DESCRIPTIONZH" label="中文描述" min-width="180" show-overflow-tooltip />
                <el-table-column prop="MENUTYPE" label="菜单类型" width="100" align="center" />
                <el-table-column prop="POSITION" label="位置" width="80" align="center" />
                <el-table-column prop="SUBPOSITION" label="子位置" width="80" align="center" />
                <el-table-column prop="ELEMENTTYPE" label="元素类型" width="100" align="center" />
                <el-table-column prop="KEYVALUE" label="键值" min-width="150" show-overflow-tooltip />
              </el-table>
              <el-empty v-if="!menuLoading && appToolList.length === 0" description="暂无数据" />
            </div>
          </el-tab-pane>
          <el-tab-pane label="签名" name="sigoption">
            <div class="menu-list-tab">
              <el-input v-model="menuFilterText" placeholder="搜索" clearable style="width:250px; margin-bottom: 10px" @keyup.enter.native="filterMenuList" />
              <el-table :data="filteredSigOption" border size="small" v-loading="menuLoading" highlight-current-row @row-click="handleSigOptionRowClick">
                <el-table-column prop="DESCRIPTION" label="中文描述" min-width="200" show-overflow-tooltip />
                <el-table-column prop="EN_DESCRIPTION" label="英文描述" min-width="200" show-overflow-tooltip />
                <el-table-column prop="OPTIONNAME" label="选项名称" min-width="150" show-overflow-tooltip />
                <el-table-column prop="APP" label="应用" width="80" align="center" />
                <el-table-column prop="ESIGENABLED" label="电子签名" width="100" align="center">
                  <template slot-scope="scope">{{ scope.row.ESIGENABLED === 1 ? '是' : '否' }}</template>
                </el-table-column>
                <el-table-column prop="VISIBLE" label="可见" width="80" align="center">
                  <template slot-scope="scope">{{ scope.row.VISIBLE === 1 ? '是' : '否' }}</template>
                </el-table-column>
                <el-table-column prop="ALSOGRANTS" label="同时授予" min-width="180" show-overflow-tooltip />
              </el-table>
              <el-empty v-if="!menuLoading && sigOptionList.length === 0" description="暂无数据" />
            </div>
          </el-tab-pane>
          <el-tab-pane label="签名条件属性" name="ctrlgroup">
            <div class="menu-list-tab">
              <el-input v-model="menuFilterText" placeholder="搜索" clearable style="width:250px; margin-bottom: 10px" @keyup.enter.native="filterMenuList" />
              <div v-loading="menuLoading">
                <el-collapse v-model="ctrlGroupExpanded">
                  <el-collapse-item v-for="group in filteredCtrlGroup" :key="group.CTRLGROUPID" :name="group.CTRLGROUPID" :title="group.GROUPNAME + ' (选项: ' + group.OPTIONNAME + ')'">
                    <el-descriptions :column="3" border size="small" style="margin-bottom: 12px">
                      <el-descriptions-item label="组ID">{{ group.CTRLGROUPID }}</el-descriptions-item>
                      <el-descriptions-item label="组名称">{{ group.GROUPNAME }}</el-descriptions-item>
                      <el-descriptions-item label="选项名称">{{ group.OPTIONNAME }}</el-descriptions-item>
                      <el-descriptions-item label="应用">{{ group.APP }}</el-descriptions-item>
                      <el-descriptions-item label="组顺序">{{ group.GROUPSEQ }}</el-descriptions-item>
                      <el-descriptions-item label="ROWSTAMP">{{ group.ROWSTAMP }}</el-descriptions-item>
                    </el-descriptions>
                    <el-collapse v-model="ctrlConditionExpanded">
                      <el-collapse-item v-for="condition in group.conditions" :key="condition.CTRLCONDITIONID" :name="condition.CTRLCONDITIONID" :title="'条件 ' + condition.CONDITIONNUM">
                        <el-descriptions :column="3" border size="small" style="margin-bottom: 12px">
                          <el-descriptions-item label="条件ID">{{ condition.CTRLCONDITIONID }}</el-descriptions-item>
                          <el-descriptions-item label="条件号">{{ condition.CONDITIONNUM }}</el-descriptions-item>
                          <el-descriptions-item label="条件序列">{{ condition.CONDITIONSEQ }}</el-descriptions-item>
                          <el-descriptions-item label="重新求值">
                            <el-tag :type="condition.REEVALUATE === 1 ? 'success' : 'danger'">
                              {{ condition.REEVALUATE === 1 ? '是' : '否' }}
                            </el-tag>
                          </el-descriptions-item>
                        </el-descriptions>
                        <el-table v-if="condition.properties && condition.properties.length > 0" :data="condition.properties" border size="small" highlight-current-row @row-click="handleCtrlPropRowClick">
                          <el-table-column prop="CONDITIONRESULT" label="条件结果" width="100" align="center">
                    <template slot-scope="scope">
                      <el-tag :type="scope.row.CONDITIONRESULT === 1 ? 'success' : 'danger'">
                        {{ scope.row.CONDITIONRESULT === 1 ? '是' : '否' }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="PROPERTY" label="属性" min-width="150" show-overflow-tooltip />
                  <el-table-column prop="PROPERTYVALUE" label="属性值(英文)" min-width="180" show-overflow-tooltip />
                  <el-table-column prop="L_PROPERTYVALUE" label="属性值(中文)" min-width="180" show-overflow-tooltip />
                        </el-table>
                        <el-empty v-else description="无属性配置" />
                      </el-collapse-item>
                    </el-collapse>
                    <el-empty v-if="!group.conditions || group.conditions.length === 0" description="无条件配置" />
                  </el-collapse-item>
                </el-collapse>
                <el-empty v-if="!menuLoading && ctrlGroupList.length === 0" description="暂无数据" />
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-dialog>
  </section>
</template>

<script>
import { getMaxMenuFullTree, getMaxMenuList, getSigOption, getCtrlGroup } from '@/api/maxmenu'

export default {
  name: 'MaxMenuTree',
  data() {
    return {
      loading: false,
      treeData: [],
      tableData: [],
      isFiltering: false,
      filterText: '',
      filterMenuType: '',
      filterElementType: '',
      detailVisible: false,
      selectedNode: null,
      expandAll: false,
      refreshTable: true,
      sortBy: 'zh',
      activeTab: 'detail',
      menuLoading: false,
      menuFilterText: '',
      appMenuList: [],
      searchMenuList: [],
      appToolList: [],
      sigOptionList: [],
      ctrlGroupList: [],
      ctrlGroupExpanded: [],
      ctrlConditionExpanded: [],
      // MAXMENU 全字段定义（来自 MAXATTRIBUTE）
      menuFields: [
        { attr: 'MENUTYPE', title: '菜单类型', maxtype: 'UPPER' },
        { attr: 'MODULEAPP', title: '模块或应用程序', maxtype: 'UPPER' },
        { attr: 'POSITION', title: '职位', maxtype: 'INTEGER' },
        { attr: 'SUBPOSITION', title: '下级职位', maxtype: 'INTEGER' },
        { attr: 'ELEMENTTYPE', title: '元素类型', maxtype: 'UPPER', domain: 'ELEMENTTYPE' },
        { attr: 'KEYVALUE', title: '键值', maxtype: 'UPPER' },
        { attr: 'L_HEADERDESCRIPTION', title: '标题描述(中文)', maxtype: 'ALN', span: 2 },
        { attr: 'HEADERDESCRIPTION', title: '标题描述(英文)', maxtype: 'ALN', span: 2 },
        { attr: 'URL', title: 'URL', maxtype: 'ALN', span: 2 },
        { attr: 'VISIBLE', title: '可见', maxtype: 'YORN', yes: '可见', no: '隐藏' },
        { attr: 'IMAGE', title: '图像', maxtype: 'ALN' },
        { attr: 'ACCESSKEY', title: '访问键', maxtype: 'ALN' },
        { attr: 'TABDISPLAY', title: '选项卡', maxtype: 'UPPER', domain: 'TABDISPLAY' },
        { attr: 'MAXMENUID', title: 'MAXMENUID', maxtype: 'BIGINT' },
        { attr: 'PINNED', title: '固定应用', maxtype: 'INTEGER' },
        { attr: 'ROWSTAMP', title: 'ROWSTAMP', maxtype: 'BIGINT' }
      ],
      // MAXMODULES 全字段定义（来自 MAXATTRIBUTE）
      moduleFields: [
        { attr: 'MODULE', title: '模块', maxtype: 'UPPER' },
        { attr: 'L_DESCRIPTION', title: '描述(中文)', maxtype: 'ALN', span: 2 },
        { attr: 'DESCRIPTION', title: '描述(英文)', maxtype: 'ALN', span: 2 },
        { attr: 'MAXMODULESID', title: 'MAXMODULESID', maxtype: 'BIGINT' },
        { attr: 'SKIPNAVIGATION', title: '跳过导航', maxtype: 'YORN', yes: '是', no: '否' },
        { attr: 'SKIPENTITLEMENT', title: '跳过计算权利', maxtype: 'YORN', yes: '是', no: '否' },
        { attr: 'MAXPRODID', title: '产品标识', maxtype: 'ALN' },
        { attr: 'ROWSTAMP', title: 'ROWSTAMP', maxtype: 'BIGINT' }
      ],
      // MAXMENU MODULE 类型字段（后端合并为 MENU_ 前缀）
      moduleMenuFields: [
        { attr: 'MENUTYPE', title: '菜单类型', source: 'MENU_MENUTYPE', maxtype: 'UPPER' },
        { attr: 'MODULEAPP', title: '模块或应用程序', source: 'MENU_MODULEAPP', maxtype: 'UPPER' },
        { attr: 'POSITION', title: '职位', source: 'MENU_POSITION', maxtype: 'INTEGER' },
        { attr: 'SUBPOSITION', title: '下级职位', source: 'MENU_SUBPOSITION', maxtype: 'INTEGER' },
        { attr: 'ELEMENTTYPE', title: '元素类型', source: 'MENU_ELEMENTTYPE', maxtype: 'UPPER' },
        { attr: 'KEYVALUE', title: '键值', source: 'MENU_KEYVALUE', maxtype: 'UPPER' },
        { attr: 'L_HEADERDESCRIPTION', title: '标题描述(中文)', source: 'MENU_L_HEADERDESCRIPTION', maxtype: 'ALN', span: 2 },
        { attr: 'HEADERDESCRIPTION', title: '标题描述(英文)', source: 'MENU_HEADERDESCRIPTION', maxtype: 'ALN', span: 2 },
        { attr: 'URL', title: 'URL', source: 'MENU_URL', maxtype: 'ALN', span: 2 },
        { attr: 'VISIBLE', title: '可见', source: 'MENU_VISIBLE', maxtype: 'YORN', yes: '可见', no: '隐藏' },
        { attr: 'IMAGE', title: '图像', source: 'MENU_IMAGE', maxtype: 'ALN' },
        { attr: 'ACCESSKEY', title: '访问键', source: 'MENU_ACCESSKEY', maxtype: 'ALN' },
        { attr: 'TABDISPLAY', title: '选项卡', source: 'MENU_TABDISPLAY', maxtype: 'UPPER' },
        { attr: 'MAXMENUID', title: 'MAXMENUID', source: 'MENU_MAXMENUID', maxtype: 'BIGINT' },
        { attr: 'PINNED', title: '固定应用', source: 'MENU_PINNED', maxtype: 'INTEGER' },
        { attr: 'ROWSTAMP', title: 'ROWSTAMP', source: 'MENU_ROWSTAMP', maxtype: 'BIGINT' }
      ]
    }
  },
  computed: {
    filteredAppMenu() {
      return this.filterMenuListByText(this.appMenuList)
    },
    filteredSearchMenu() {
      return this.filterMenuListByText(this.searchMenuList)
    },
    filteredAppTool() {
      return this.filterMenuListByText(this.appToolList)
    },
    filteredSigOption() {
      return this.filterSigOptionByText(this.sigOptionList)
    },
    filteredCtrlGroup() {
      if (!this.menuFilterText.trim()) {
        return this.ctrlGroupList
      }
      const text = this.menuFilterText.toLowerCase()
      return this.ctrlGroupList.filter(group => {
        const matchGroup = (group.GROUPNAME && group.GROUPNAME.toLowerCase().includes(text)) ||
                          (group.OPTIONNAME && group.OPTIONNAME.toLowerCase().includes(text)) ||
                          (group.APP && group.APP.toLowerCase().includes(text))
        if (matchGroup) return true
        if (group.conditions) {
          return group.conditions.some(condition => {
            const matchCondition = (condition.CONDITIONNUM && condition.CONDITIONNUM.toLowerCase().includes(text))
            if (matchCondition) return true
            if (condition.properties) {
              return condition.properties.some(prop => {
                return (prop.PROPERTY && prop.PROPERTY.toLowerCase().includes(text)) ||
                       (prop.PROPERTYVALUE && prop.PROPERTYVALUE.toLowerCase().includes(text)) ||
                       (prop.L_PROPERTYVALUE && prop.L_PROPERTYVALUE.toLowerCase().includes(text))
              })
            }
            return false
          })
        }
        return false
      })
    }
  },
  watch: {
    detailVisible(val) {
      console.log('detailVisible changed:', val)
    },
    selectedNode(val) {
      console.log('selectedNode changed:', val ? val.MODULEAPP : null)
    },
    activeTab(val) {
      console.log('activeTab changed:', val)
      if (!this.selectedNode) return
      if (val === 'appmenu' && this.appMenuList.length === 0) {
        this.loadMenuList('APPMENU')
      } else if (val === 'searchmenu' && this.searchMenuList.length === 0) {
        this.loadMenuList('SEARCHMENU')
      } else if (val === 'apptool' && this.appToolList.length === 0) {
        this.loadMenuList('APPTOOL')
      } else if (val === 'sigoption' && this.sigOptionList.length === 0) {
        this.loadSigOption()
      } else if (val === 'ctrlgroup' && this.ctrlGroupList.length === 0) {
        this.loadCtrlGroup()
      }
    }
  },
  created() {
    this.loadFullTree()
  },
  methods: {
    loadFullTree() {
      this.loading = true
      getMaxMenuFullTree(this.sortBy).then(res => {
        if (res.code === 200 && res.data) {
          this.treeData = this.processTreeData(res.data)
          this.isFiltering = false
          if (this.isFiltering) {
            this.tableData = this.filterTree(this.treeData)
          } else {
            this.tableData = this.treeData
          }
          this.refreshTable = false
          this.$nextTick(() => { this.refreshTable = true })
        } else {
          this.$message.error(res.message || '加载失败')
        }
      }).catch(err => {
        this.$message.error('加载菜单树失败: ' + (err.message || String(err)))
      }).finally(() => {
        this.loading = false
      })
    },

    applySort() {
      // SQL 排序，重新请求后端
      this.loadFullTree()
    },

    processTreeData(modules) {
      this._nodeIdSeq = 0
      return modules.map(mod => {
        const moduleNode = {
          ...mod,
          nodeId: 'mod_' + (this._nodeIdSeq++),
          _isModule: true,
          _descZh: mod.L_DESCRIPTION || '',
          _descEn: mod.DESCRIPTION || ''
        }
        if (mod.children) {
          moduleNode.children = this.processMenuItems(mod.children)
        }
        return moduleNode
      })
    },

    processMenuItems(items) {
      return items.map(item => {
        const type = item.ELEMENTTYPE || ''
        const node = {
          ...item,
          nodeId: 'item_' + (this._nodeIdSeq++),
          _descZh: item.L_HEADERDESCRIPTION || '',
          _descEn: item.HEADERDESCRIPTION || ''
        }
        if (type === 'SEP') {
          node._descZh = '─── 分隔线 ───'
          node._descEn = ''
        }
        if (item.children && item.children.length > 0) {
          node.children = this.processMenuItems(item.children)
        }
        return node
      })
    },

    countNodes(nodes) {
      let count = nodes.length
      for (const n of nodes) {
        if (n.children) count += this.countNodes(n.children)
      }
      return count
    },

    filterTree(nodes) {
      const result = []
      for (const node of nodes) {
        const matchSelf = this.matchNode(node)
        const filteredChildren = node.children ? this.filterTree(node.children) : []
        if (matchSelf || filteredChildren.length > 0) {
          result.push({ ...node, children: filteredChildren.length > 0 ? filteredChildren : node.children })
        }
      }
      return result
    },

    matchNode(node) {
      const text = this.filterText.toLowerCase()
      const menuType = this.filterMenuType
      const elementType = this.filterElementType

      if (menuType && node.MENUTYPE && node.MENUTYPE !== menuType && !node._isModule) return false
      if (menuType && node._isModule) { /* module 级别不过滤 menuType */ }
      if (elementType && node.ELEMENTTYPE && node.ELEMENTTYPE !== elementType) return false
      if (elementType && node._isModule && elementType !== 'MODULE') return false

      if (!text) return true
      const fields = [
        node.MODULE, node.MODULEAPP, node.KEYVALUE,
        node.HEADERDESCRIPTION, node.L_HEADERDESCRIPTION,
        node.DESCRIPTION, node.L_DESCRIPTION, node._label
      ].map(f => (f || '').toLowerCase())
      return fields.some(f => f.includes(text))
    },

    doFilter() {
      const hasFilter = !!(this.filterText || this.filterMenuType || this.filterElementType)
      if (!hasFilter) {
        this.isFiltering = false
        this.tableData = this.treeData
      } else {
        this.isFiltering = true
        this.tableData = this.filterTree(this.treeData)
      }
      // 强制重建表格以更新展开状态
      this.refreshTable = false
      this.$nextTick(() => {
        this.refreshTable = true
      })
    },

    handleSearch() {
      this.doFilter()
    },

    buildFlatTree(items) {
      const groups = {}
      let counter = 0
      for (const item of items) {
        const key = item.MENUTYPE + '|' + item.MODULEAPP
        if (!groups[key]) {
          groups[key] = {
            nodeId: 'grp_' + (counter++),
            _isGroup: true,
            MENUTYPE: item.MENUTYPE,
            MODULEAPP: item.MODULEAPP,
            _descZh: item.MENUTYPE + ' - ' + item.MODULEAPP,
            _descEn: '',
            children: []
          }
        }
        groups[key].children.push({
          ...item,
          nodeId: 'item_' + (counter++),
          _descZh: item.L_HEADERDESCRIPTION || '',
          _descEn: item.HEADERDESCRIPTION || ''
        })
      }
      return Object.values(groups)
    },

    resetFilter() {
      this.filterText = ''
      this.filterMenuType = ''
      this.filterElementType = ''
      this.isFiltering = false
      this.tableData = this.treeData
      this.refreshTable = false
      this.$nextTick(() => {
        this.refreshTable = true
      })
    },

    toggleExpandAll() {
      this.expandAll = !this.expandAll
      this.refreshTable = false
      this.$nextTick(() => {
        this.refreshTable = true
      })
    },

    getRowLabel(row) {
      if (row._isModule) return row.MODULE
      if (row._isGroup) return row.MENUTYPE + ' - ' + row.MODULEAPP
      const type = row.ELEMENTTYPE || ''
      if (type === 'SEP') return '─── 分隔线 ───'
      return row.KEYVALUE || row.L_HEADERDESCRIPTION || row.HEADERDESCRIPTION || ''
    },

    getRowIcon(row) {
      if (row._isModule) return 'el-icon-folder'
      if (row._isGroup) return 'el-icon-s-fold'
      const type = row.ELEMENTTYPE || ''
      const iconMap = { HEADER: 'el-icon-s-operation', APP: 'el-icon-document', OPTION: 'el-icon-setting', SEP: 'el-icon-minus', MODULE: 'el-icon-folder-opened' }
      return iconMap[type] || 'el-icon-document'
    },

    getRowClass(row) {
      if (row._isModule) return 'label-module'
      const type = row.ELEMENTTYPE || ''
      const classMap = { HEADER: 'label-header', APP: 'label-app', OPTION: 'label-option', SEP: 'label-sep' }
      return classMap[type] || ''
    },

    elementTypeTag(type) {
      const map = { MODULE: 'primary', HEADER: 'warning', APP: 'success', OPTION: 'info', SEP: 'danger' }
      return map[type] || 'info'
    },
    // 模块节点使用 MENU_ 前缀的合并字段，其他节点使用原始字段
    getPos(row) {
      return row._isModule ? row.MENU_POSITION : row.POSITION
    },
    getSubPos(row) {
      return row._isModule ? undefined : row.SUBPOSITION
    },
    getVisible(row) {
      return row._isModule ? row.MENU_VISIBLE : row.VISIBLE
    },
    getPinned(row) {
      return row._isModule ? row.MENU_PINNED : row.PINNED
    },

    handleRowClick(row) {
      console.log('handleRowClick:', row,row._isModule, row.ELEMENTTYPE, row.MODULEAPP, row.MODULE)
      if (row._isModule || row.ELEMENTTYPE) {
        this.selectedNode = row
        this.detailVisible = true
        this.activeTab = 'detail'
        this.menuFilterText = ''
        this.appMenuList = []
      this.searchMenuList = []
      this.appToolList = []
      this.sigOptionList = []
      this.ctrlGroupList = []
        console.log('selectedNode set:', this.selectedNode.MODULEAPP, this.selectedNode.ELEMENTTYPE)
      }
    },

    handleTabClick(tab) {
      console.log('handleTabClick:', tab.name)
      this.activeTab = tab.name
    },

    loadMenuList(menuType) {
      const moduleApp = this.selectedNode._isModule ? this.selectedNode.MODULE : this.selectedNode.KEYVALUE
      console.log('loadMenuList:', menuType, 'moduleApp:', moduleApp, '_isModule:', this.selectedNode._isModule)
      if (!moduleApp) {
        this.$message.warning('无法获取模块或应用信息')
        return
      }
      this.menuLoading = true
      getMaxMenuList(moduleApp, menuType).then(res => {
        console.log('getMaxMenuList response:', res)
        if (res.code === 200 && res.data) {
          if (menuType === 'APPMENU') {
            this.appMenuList = res.data
          } else if (menuType === 'SEARCHMENU') {
            this.searchMenuList = res.data
          } else if (menuType === 'APPTOOL') {
            this.appToolList = res.data
          }
          console.log('loaded ' + menuType + ': ', res.data.length, 'items')
        } else {
          this.$message.error(res.message || '加载失败')
        }
      }).catch(err => {
        console.error('loadMenuList error:', err)
        this.$message.error('加载菜单列表失败: ' + (err.message || String(err)))
      }).finally(() => {
        this.menuLoading = false
      })
    },

    loadSigOption() {
      const moduleApp = this.selectedNode._isModule ? this.selectedNode.MODULE : this.selectedNode.KEYVALUE
      if (!moduleApp) {
        this.$message.warning('无法获取模块或应用信息')
        return
      }
      this.menuLoading = true
      getSigOption(moduleApp).then(res => {
        if (res.code === 200 && res.data) {
          this.sigOptionList = res.data
        } else {
          this.$message.error(res.message || '加载失败')
        }
      }).catch(err => {
        this.$message.error('加载签名列表失败: ' + (err.message || String(err)))
      }).finally(() => {
        this.menuLoading = false
      })
    },

    loadCtrlGroup() {
      const moduleApp = this.selectedNode._isModule ? this.selectedNode.MODULE : this.selectedNode.KEYVALUE
      if (!moduleApp) {
        this.$message.warning('无法获取模块或应用信息')
        return
      }
      this.menuLoading = true
      getCtrlGroup(moduleApp).then(res => {
        if (res.code === 200 && res.data) {
          this.ctrlGroupList = res.data
          this.ctrlGroupExpanded = res.data.map(g => g.CTRLGROUPID)
          const conditionIds = []
          res.data.forEach(g => {
            if (g.conditions) {
              g.conditions.forEach(c => conditionIds.push(c.CTRLCONDITIONID))
            }
          })
          this.ctrlConditionExpanded = conditionIds
        } else {
          this.$message.error(res.message || '加载失败')
        }
      }).catch(err => {
        this.$message.error('加载签名条件属性失败: ' + (err.message || String(err)))
      }).finally(() => {
        this.menuLoading = false
      })
    },

    handleMenuRowClick(row) {
      const fields = Object.keys(row)
      let html = '<div style="max-height:400px;overflow-y:auto;padding:10px;">'
      html += '<table style="width:100%;border-collapse:collapse;font-size:12px;">'
      fields.forEach(field => {
        const value = row[field] != null ? String(row[field]) : '-'
        html += `<tr><td style="border:1px solid #ebeef5;padding:6px 10px;width:30%;background:#fafafa;font-weight:bold;">${field}</td><td style="border:1px solid #ebeef5;padding:6px 10px;word-break:break-all;">${value}</td></tr>`
      })
      html += '</table></div>'
      this.$alert(html, '行详情', {
        dangerouslyUseHTMLString: true,
        width: '700px'
      })
    },

    handleSigOptionRowClick(row) {
      const fields = Object.keys(row)
      let html = '<div style="max-height:400px;overflow-y:auto;padding:10px;">'
      html += '<table style="width:100%;border-collapse:collapse;font-size:12px;">'
      fields.forEach(field => {
        const value = row[field] != null ? String(row[field]) : '-'
        html += `<tr><td style="border:1px solid #ebeef5;padding:6px 10px;width:30%;background:#fafafa;font-weight:bold;">${field}</td><td style="border:1px solid #ebeef5;padding:6px 10px;word-break:break-all;">${value}</td></tr>`
      })
      html += '</table></div>'
      this.$alert(html, '签名详情', {
        dangerouslyUseHTMLString: true,
        width: '700px'
      })
    },

    handleCtrlPropRowClick(row) {
      const fields = Object.keys(row)
      let html = '<div style="max-height:400px;overflow-y:auto;padding:10px;">'
      html += '<table style="width:100%;border-collapse:collapse;font-size:12px;">'
      fields.forEach(field => {
        const value = row[field] != null ? String(row[field]) : '-'
        html += `<tr><td style="border:1px solid #ebeef5;padding:6px 10px;width:30%;background:#fafafa;font-weight:bold;">${field}</td><td style="border:1px solid #ebeef5;padding:6px 10px;word-break:break-all;">${value}</td></tr>`
      })
      html += '</table></div>'
      this.$alert(html, '条件属性详情', {
        dangerouslyUseHTMLString: true,
        width: '700px'
      })
    },

    filterMenuListByText(list) {
      if (!this.menuFilterText.trim()) {
        return list
      }
      const text = this.menuFilterText.toLowerCase()
      return list.filter(item => {
        return (item.DESCRIPTIONEN && item.DESCRIPTIONEN.toLowerCase().includes(text)) ||
               (item.DESCRIPTIONZH && item.DESCRIPTIONZH.toLowerCase().includes(text)) ||
               (item.KEYVALUE && item.KEYVALUE.toLowerCase().includes(text)) ||
               (item.ELEMENTTYPE && item.ELEMENTTYPE.toLowerCase().includes(text))
      })
    },

    filterSigOptionByText(list) {
      if (!this.menuFilterText.trim()) {
        return list
      }
      const text = this.menuFilterText.toLowerCase()
      return list.filter(item => {
        return (item.DESCRIPTION && item.DESCRIPTION.toLowerCase().includes(text)) ||
               (item.OPTIONNAME && item.OPTIONNAME.toLowerCase().includes(text)) ||
               (item.VALUE && item.VALUE.toLowerCase().includes(text)) ||
               (item.APP && item.APP.toLowerCase().includes(text))
      })
    },

    filterMenuList() {
    }
  }
}
</script>

<style lang="scss" scoped>
.menu-page {
  padding: 0;
}
.page-header-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
  h2 { margin: 0 0 4px 0; font-size: 18px; }
  .page-summary { margin: 0; color: #909399; font-size: 13px; }
}
.filter-form {
  margin-bottom: 12px;
  ::v-deep .el-form-item { margin-bottom: 8px; }
}
.table-container {
  min-height: 400px;
  max-height: calc(100vh - 280px);
  overflow-y: auto;
}
.row-label {
  font-size: 13px;
  white-space: nowrap;
}
.label-module { font-weight: bold; color: #303133; }
.label-header { font-weight: 600; color: #e6a23c; }
.label-app { color: #67c23a; }
.label-option { color: #409eff; }
.label-sep { color: #c0c4cc; font-style: italic; }
.pos-badge {
  background: #f0f0f0;
  color: #606266;
  padding: 0 5px;
  border-radius: 3px;
  font-size: 11px;
  font-family: monospace;
}
.menu-list-tab {
  ::v-deep .el-table {
    font-size: 12px;
  }
}
</style>
