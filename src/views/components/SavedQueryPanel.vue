<template>
  <div class="saved-query-panel">
    <div class="sqp-actions">
      <el-select v-model="selectedSavedQuery" placeholder="选择保存的查询" clearable filterable size="mini" style="width: 180px; margin-right: 8px;" :class="{ 'sqp-select-active': !!selectedSavedQuery }" @change="applySavedQuery">
        <el-option v-for="q in savedQueryList" :key="q.id" :label="q.queryname" :value="q.id" />
      </el-select>
      <el-dropdown trigger="click" size="mini" @command="handleMenuCommand">
        <el-button size="mini" style="width: 31px; padding: 7px 0;">
          <img :src="menuArrowImg" style="width: 14px; height: 14px; vertical-align: middle;" />
        </el-button>
        <el-dropdown-menu slot="dropdown">
          <el-dropdown-item command="where">where子句</el-dropdown-item>
          <el-dropdown-item command="save">保存查询</el-dropdown-item>
          <el-dropdown-item command="manage" divided>查看/管理查询</el-dropdown-item>
        </el-dropdown-menu>
      </el-dropdown>
    </div>

    <!-- where 子句弹窗 -->
    <el-dialog title="自定义 where 子句" :visible.sync="whereDialog.visible" width="800px" :close-on-click-modal="false" append-to-body>
      <p class="sqp-tip">输入 SQL where 条件（不含 WHERE 关键字），将直接作为查询条件，例如：<code>DOMAINTYPE = 'ALN' AND DESCRIPTION LIKE '%状态%'</code></p>
      <el-input v-model="whereDialog.where" type="textarea" :rows="6" placeholder="如 DOMAINID = 'STATUS' AND DESCRIPTION LIKE '%状态%'" />
      <span slot="footer" class="dialog-footer">
        <el-button @click="whereDialog.visible = false">取 消</el-button>
        <el-button type="primary" @click="confirmWhere">确 定</el-button>
      </span>
    </el-dialog>

    <!-- 保存查询弹窗 -->
    <el-dialog title="保存查询" :visible.sync="saveDialog.visible" width="700px" :close-on-click-modal="false" append-to-body>
      <el-form :model="saveDialog" label-width="90px" size="small">
        <el-form-item label="查询名称" required>
          <el-input v-model="saveDialog.queryname" placeholder="查询名称，如：按类型查询" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="saveDialog.description" placeholder="可选" />
        </el-form-item>
        <el-form-item label="WHERE子句" required>
          <el-input v-model="saveDialog.whereclause" type="textarea" :rows="5" />
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="saveDialog.visible = false">取 消</el-button>
        <el-button type="primary" @click="confirmSave">保 存</el-button>
      </span>
    </el-dialog>

    <!-- 查看/管理查询弹窗 -->
    <el-dialog title="查看/管理保存的查询" :visible.sync="manageDialog.visible" width="1000px" top="5vh" :close-on-click-modal="true" append-to-body>
      <el-table :data="manageDialog.list" border stripe size="small" v-loading="manageDialog.loading">
        <el-table-column prop="queryname" label="查询名称" width="180" />
        <el-table-column prop="description" label="描述" width="180" />
        <el-table-column prop="whereclause" label="WHERE子句" min-width="300" show-overflow-tooltip />
        <el-table-column prop="updatedAt" label="更新时间" width="180" />
        <el-table-column label="操作" width="160" fixed="right">
          <template slot-scope="{row}">
            <el-button type="text" size="mini" @click="editManageQuery(row)">编辑</el-button>
            <el-button type="text" size="mini" @click="applyManageQuery(row)">应用</el-button>
            <el-button type="text" size="mini" class="danger-text" @click="deleteManageQuery(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <span slot="footer" class="dialog-footer">
        <el-button @click="manageDialog.visible = false">关 闭</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { getSavedQueryList, getSavedQueryDetail, saveSavedQuery, deleteSavedQuery } from '@/api/savedquery'
import menuArrowImg from '@/assets/mas/images/nav_btn_menuArrow_Search.gif'

/**
 * 通用保存查询组件
 * 用法:
 *   <saved-query-panel ref="savedQuery" appname="DOMAIN" @whereChange="onWhereChange" />
 * 方法:
 *   getWhere()  - 获取当前生效的自定义 where（未设置返回 ''）
 *   clear()     - 清除自定义 where
 * 事件:
 *   whereChange(where) - 自定义 where 变化时触发（清除时为 ''）
 */
export default {
  name: 'SavedQueryPanel',
  props: {
    appname: {
      type: String,
      required: true
    },
    // 父组件表单构建的默认 where（无自定义 where 时作为 where子句/保存查询的默认值）
    defaultWhere: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      menuArrowImg,
      selectedSavedQuery: null,
      savedQueryList: [],
      customWhere: '',
      whereDialog: {
        visible: false,
        where: ''
      },
      saveDialog: {
        visible: false,
        queryname: '',
        description: '',
        whereclause: ''
      },
      manageDialog: {
        visible: false,
        loading: false,
        list: []
      }
    }
  },
  methods: {
    // === 对外方法 ===
    getWhere() {
      return this.customWhere
    },
    setWhere(where) {
      const next = where ? String(where).trim() : ''
      this.customWhere = next
      if (!next) {
        this.selectedSavedQuery = null
      }
    },
    clear() {
      this.setWhere('')
      this.$emit('whereChange', '')
    },
    // === 内部 ===
    loadList() {
      getSavedQueryList(this.appname).then(res => {
        if (res.code === 200) {
          this.savedQueryList = res.data || []
        }
      }).catch(err => {
        console.warn('加载保存的查询失败:', err)
      })
    },
    handleMenuCommand(command) {
      if (command === 'where') {
        this.whereDialog.where = this.customWhere || this.defaultWhere
        this.whereDialog.visible = true
      } else if (command === 'save') {
        this.saveDialog.queryname = ''
        this.saveDialog.description = ''
        this.saveDialog.whereclause = this.customWhere || this.defaultWhere
        this.saveDialog.visible = true
      } else if (command === 'manage') {
        this.loadManageList()
        this.manageDialog.visible = true
      }
    },
    // where 子句
    confirmWhere() {
      const where = (this.whereDialog.where || '').trim()
      if (!where) {
        this.$message.warning('请输入 where 子句')
        return
      }
      this.setWhere(where)
      this.whereDialog.visible = false
      this.$emit('whereChange', this.customWhere)
    },
    // 应用保存的查询（下拉框选择）
    applySavedQuery(id) {
      if (!id) {
        this.setWhere('')
        this.$emit('whereChange', '')
        return
      }
      getSavedQueryDetail(id).then(res => {
        if (res.code === 200 && res.data) {
          this.setWhere(res.data.whereclause || '')
          this.$emit('whereChange', this.customWhere)
        } else {
          this.$message.error('获取保存的查询失败: ' + (res.message || '未知错误'))
        }
      }).catch(err => {
        this.$message.error('获取保存的查询失败: ' + (err.message || String(err)))
      })
    },
    // 保存查询
    confirmSave() {
      const queryname = (this.saveDialog.queryname || '').trim()
      const whereclause = (this.saveDialog.whereclause || '').trim()
      if (!queryname) {
        this.$message.warning('请输入查询名称')
        return
      }
      if (!whereclause) {
        this.$message.warning('请输入 WHERE 子句')
        return
      }
      saveSavedQuery({
        app: this.appname,
        queryname,
        whereclause,
        description: this.saveDialog.description || ''
      }).then(res => {
        if (res.code === 200) {
          this.$message.success('保存查询成功')
          this.saveDialog.visible = false
          this.loadList()
        } else {
          this.$message.error('保存查询失败: ' + (res.message || '未知错误'))
        }
      }).catch(err => {
        this.$message.error('保存查询失败: ' + (err.message || String(err)))
      })
    },
    // 查看/管理查询
    loadManageList() {
      this.manageDialog.loading = true
      getSavedQueryList(this.appname).then(res => {
        if (res.code === 200) {
          this.manageDialog.list = res.data || []
        } else {
          this.$message.error('加载查询列表失败: ' + (res.message || '未知错误'))
        }
      }).catch(err => {
        this.$message.error('加载查询列表失败: ' + (err.message || String(err)))
      }).finally(() => {
        this.manageDialog.loading = false
      })
    },
    editManageQuery(row) {
      this.manageDialog.visible = false
      this.saveDialog.queryname = row.queryname
      this.saveDialog.description = row.description || ''
      this.saveDialog.whereclause = row.whereclause || ''
      this.saveDialog.visible = true
    },
    applyManageQuery(row) {
      this.setWhere(row.whereclause || '')
      this.selectedSavedQuery = row.id
      this.manageDialog.visible = false
      this.$emit('whereChange', this.customWhere)
    },
    deleteManageQuery(row) {
      this.$confirm('确定删除查询「' + row.queryname + '」吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        deleteSavedQuery(row.id).then(res => {
          if (res.code === 200) {
            this.$message.success('删除成功')
            this.loadManageList()
            this.loadList()
            if (this.selectedSavedQuery === row.id) {
              this.selectedSavedQuery = null
              this.setWhere('')
              this.$emit('whereChange', '')
            }
          } else {
            this.$message.error('删除失败: ' + (res.message || '未知错误'))
          }
        }).catch(err => {
          this.$message.error('删除失败: ' + (err.message || String(err)))
        })
      }).catch(() => {})
    }
  },
  mounted() {
    this.loadList()
  }
}
</script>

<style lang="scss" scoped>
.saved-query-panel {
  display: inline-block;
}
.sqp-actions {
  display: flex;
  align-items: center;
}
// 保存查询选中时高亮
.sqp-select-active ::v-deep .el-input__inner {
  border-color: #e6a23c;
  background: #fdf6ec;
  color: #e6a23c;
  font-weight: bold;
}
.sqp-select-active ::v-deep .el-input__inner::placeholder {
  color: #e6a23c;
}
.sqp-tip {
  color: #909399;
  font-size: 12px;
  margin: 0 0 8px 0;
  line-height: 1.6;
}
.sqp-tip code {
  background: #f4f4f5;
  padding: 2px 6px;
  border-radius: 3px;
  color: #409eff;
}
.danger-text {
  color: #f56c6c;
}
</style>
